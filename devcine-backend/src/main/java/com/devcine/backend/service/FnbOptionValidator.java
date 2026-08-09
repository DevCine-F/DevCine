package com.devcine.backend.service;

import com.devcine.backend.dto.request.FnbOptionSelectionDTO;
import com.devcine.backend.entity.FnbComboSlot;
import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.entity.FnbOptionItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Xác thực SERVER-SIDE các lựa chọn tùy chọn F&B theo mô hình Combo Slot, dùng
 * chung cho cả luồng Checkout Web ({@code BookingService}) và POS ({@code ConcessionService}).
 *
 * <p>Chống mọi gian lận đến từ client:
 * <ul>
 *   <li><b>Membership:</b> option được chọn phải thuộc đúng pool mà Slot trỏ tới.</li>
 *   <li><b>min / max choices:</b> số lựa chọn mỗi Slot nằm trong ngưỡng cấu hình.</li>
 *   <li><b>required:</b> Slot bắt buộc phải có lựa chọn.</li>
 *   <li><b>Anti-price-tampering:</b> phụ thu LẤY TỪ DB ({@link FnbOptionItem#getSurchargePrice()}),
 *       KHÔNG tin bất kỳ con số nào client gửi lên.</li>
 * </ul>
 * Trả về danh sách {@link ResolvedOption} đã nạp entity thật để service dựng snapshot
 * (tên + phụ thu + nhãn Slot). Ném {@link IllegalArgumentException} (→ 400) khi vi phạm.
 */
@Component
public class FnbOptionValidator {

    /** Một lựa chọn đã được xác thực + nạp entity thật, sẵn sàng để dựng snapshot. */
    public record ResolvedOption(FnbComboSlot slot, FnbOptionItem item) {
        public String slotLabel() { return slot.getSlotLabel(); }
        public String optionName() { return item.getName(); }
        public java.math.BigDecimal surcharge() { return item.getSurchargePrice(); }
    }

    /**
     * Xác thực lựa chọn của MỘT món và trả về danh sách option đã resolve.
     * {@code item} PHẢI được nạp kèm {@code slots.optionGroup.items} (dùng EntityGraph).
     */
    public List<ResolvedOption> validateAndResolve(FnbItem item, List<FnbOptionSelectionDTO> selections) {
        java.util.Collection<FnbComboSlot> slots = item.getSlots();

        // Món không cấu hình Slot → tuyệt đối không nhận option nào (chống tiêm option lạ).
        if (slots == null || slots.isEmpty()) {
            if (selections != null && !selections.isEmpty()) {
                throw new IllegalArgumentException("Món '" + item.getName() + "' không có tùy chọn để lựa.");
            }
            return List.of();
        }

        Map<Integer, FnbComboSlot> slotById = slots.stream()
                .collect(Collectors.toMap(FnbComboSlot::getId, s -> s));

        // Gom lựa chọn theo Slot; đồng thời chặn slotId lạ / thiếu.
        Map<Integer, List<FnbOptionSelectionDTO>> bySlot = new LinkedHashMap<>();
        if (selections != null) {
            for (FnbOptionSelectionDTO sel : selections) {
                if (sel.getSlotId() == null) {
                    throw new IllegalArgumentException("Thiếu Ô chọn (slotId) cho tùy chọn của món '" + item.getName() + "'.");
                }
                if (!slotById.containsKey(sel.getSlotId())) {
                    throw new IllegalArgumentException("Ô chọn không hợp lệ cho món '" + item.getName() + "'.");
                }
                bySlot.computeIfAbsent(sel.getSlotId(), k -> new ArrayList<>()).add(sel);
            }
        }

        List<ResolvedOption> resolved = new ArrayList<>();
        for (FnbComboSlot slot : slots) {
            List<FnbOptionSelectionDTO> chosen = bySlot.getOrDefault(slot.getId(), List.of());
            int count = chosen.size();

            if (Boolean.TRUE.equals(slot.getIsRequired()) && count == 0) {
                throw new IllegalArgumentException(
                        "Vui lòng chọn '" + slot.getSlotLabel() + "' cho món '" + item.getName() + "'.");
            }
            if (count > 0 && count < slot.getMinChoices()) {
                throw new IllegalArgumentException(
                        "'" + slot.getSlotLabel() + "' cần chọn ít nhất " + slot.getMinChoices() + " lựa chọn.");
            }
            if (count > slot.getMaxChoices()) {
                throw new IllegalArgumentException(
                        "'" + slot.getSlotLabel() + "' chỉ được chọn tối đa " + slot.getMaxChoices() + " lựa chọn.");
            }

            // Membership: option phải thuộc pool của Slot.
            Map<Integer, FnbOptionItem> poolItems = slot.getOptionGroup().getItems().stream()
                    .collect(Collectors.toMap(FnbOptionItem::getId, i -> i));
            for (FnbOptionSelectionDTO sel : chosen) {
                FnbOptionItem optItem = poolItems.get(sel.getOptionItemId());
                if (optItem == null) {
                    throw new IllegalArgumentException(
                            "Lựa chọn không thuộc Ô '" + slot.getSlotLabel() + "' của món '" + item.getName() + "'.");
                }
                resolved.add(new ResolvedOption(slot, optItem));
            }
        }
        return resolved;
    }
}
