package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.entity.FnbOptionGroup;
import com.devcine.backend.entity.FnbComboSlot;
import com.devcine.backend.repository.FnbItemRepository;
import com.devcine.backend.repository.FnbOptionGroupRepository;
import com.devcine.backend.repository.FnbComboSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Quản trị THỰC ĐƠN F&B (món & combo) — tên, giá, ảnh, ẩn/hiện. Không phải kho:
 * tồn kho là VÔ HẠN, {@link FnbItem} không có trường số lượng và module Kho/BOM đã gỡ.
 * Quyền gác ở đây là {@code fnb_menu} (trước 21/07/2026 mang tên cũ {@code pos_inventory}).
 * Bán F&B tại quầy KHÔNG đi qua đây — xem TicketingController + Position FNB.
 */
@RestController
@RequestMapping("/api/fnbs")
@RequiredArgsConstructor
public class FnbController {

    private final FnbItemRepository fnbItemRepository;
    private final FnbOptionGroupRepository fnbOptionGroupRepository;
    private final FnbComboSlotRepository fnbComboSlotRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FnbItem>>> getActiveFnbs() {
        // Kênh bán: đang bán VÀ chưa xoá.
        return ResponseEntity.ok(ApiResponse.ok(fnbItemRepository.findByIsActiveTrueAndIsDeletedFalseOrderByTypeAscNameAsc()));
    }
    
    @GetMapping("/groups")
    @PreAuthorize("@perm.can('fnb_menu','view')")
    public ResponseEntity<ApiResponse<List<com.devcine.backend.entity.FnbOptionGroup>>> getAllOptionGroups() {
        return ResponseEntity.ok(ApiResponse.ok(fnbOptionGroupRepository.findAll()));
    }

    @PostMapping("/groups")
    @PreAuthorize("@perm.can('fnb_menu','add')")
    public ResponseEntity<?> createOptionGroup(@RequestBody Map<String, Object> body) {
        try {
            String rawName = (String) body.get("name");
            if (rawName == null || rawName.trim().length() < 2 || rawName.trim().length() > 100) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Tên kho phải từ 2 đến 100 ký tự."));
            }
            String name = rawName.trim().replaceAll("\\s+", " ");
            if (name.contains("<") || name.contains(">")) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Tên kho chứa ký tự không hợp lệ."));
            }
            if (fnbOptionGroupRepository.existsByNameIgnoreCase(name)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Tên kho tùy chọn đã tồn tại."));
            }

            com.devcine.backend.entity.FnbOptionGroup group = com.devcine.backend.entity.FnbOptionGroup.builder()
                    .name(name)
                    .build();
            
            if (body.containsKey("items")) {
                List<Map<String, Object>> itemsList = (List<Map<String, Object>>) body.get("items");
                if (itemsList == null || itemsList.isEmpty()) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Cần ít nhất 1 lựa chọn vị con."));
                }
                if (itemsList.size() > 50) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Đã đạt giới hạn tối đa số lượng vị (tối đa 50)."));
                }

                java.util.Set<String> itemNames = new java.util.HashSet<>();
                for (Map<String, Object> itemData : itemsList) {
                    String rawItemName = (String) itemData.get("name");
                    if (rawItemName == null || rawItemName.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(ApiResponse.fail("Vui lòng nhập tên vị."));
                    }
                    String itemName = rawItemName.trim().replaceAll("\\s+", " ");
                    if (itemName.length() > 50) {
                        return ResponseEntity.badRequest().body(ApiResponse.fail("Tên vị không được quá 50 ký tự."));
                    }
                    if (!itemNames.add(itemName.toLowerCase())) {
                        return ResponseEntity.badRequest().body(ApiResponse.fail("Tên vị '" + itemName + "' đã tồn tại trong danh sách."));
                    }

                    BigDecimal price = BigDecimal.ZERO;
                    if (itemData.get("surchargePrice") != null) {
                        price = new BigDecimal(itemData.get("surchargePrice").toString());
                        if (price.compareTo(BigDecimal.ZERO) < 0) {
                            return ResponseEntity.badRequest().body(ApiResponse.fail("Giá phụ thu không được là số âm."));
                        }
                        if (price.compareTo(new BigDecimal("100000000")) > 0) {
                            return ResponseEntity.badRequest().body(ApiResponse.fail("Giá tối đa không vượt quá 100.000.000đ."));
                        }
                    }

                    com.devcine.backend.entity.FnbOptionItem optionItem = com.devcine.backend.entity.FnbOptionItem.builder()
                            .group(group)
                            .name(itemName)
                            .surchargePrice(price)
                            .build();
                    group.getItems().add(optionItem);
                }
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Cần ít nhất 1 lựa chọn vị con."));
            }

            fnbOptionGroupRepository.save(group);
            return ResponseEntity.status(201).body(ApiResponse.ok(group));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/groups/{id}")
    @PreAuthorize("@perm.can('fnb_menu','edit')")
    public ResponseEntity<?> updateOptionGroup(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            com.devcine.backend.entity.FnbOptionGroup group = fnbOptionGroupRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Kho tùy chọn"));
            
            if (body.containsKey("name")) {
                String rawName = (String) body.get("name");
                if (rawName == null || rawName.trim().length() < 2 || rawName.trim().length() > 100) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Tên kho phải từ 2 đến 100 ký tự."));
                }
                String name = rawName.trim().replaceAll("\\s+", " ");
                if (name.contains("<") || name.contains(">")) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Tên kho chứa ký tự không hợp lệ."));
                }
                if (fnbOptionGroupRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Tên kho tùy chọn đã tồn tại."));
                }
                group.setName(name);
            }

            if (body.containsKey("items")) {
                List<Map<String, Object>> itemsList = (List<Map<String, Object>>) body.get("items");
                if (itemsList == null || itemsList.isEmpty()) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Cần ít nhất 1 lựa chọn vị con."));
                }
                if (itemsList.size() > 50) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Đã đạt giới hạn tối đa số lượng vị (tối đa 50)."));
                }

                java.util.Set<String> itemNames = new java.util.HashSet<>();
                for (Map<String, Object> itemData : itemsList) {
                    String rawItemName = (String) itemData.get("name");
                    if (rawItemName == null || rawItemName.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(ApiResponse.fail("Vui lòng nhập tên vị."));
                    }
                    String itemName = rawItemName.trim().replaceAll("\\s+", " ");
                    if (itemName.length() > 50) {
                        return ResponseEntity.badRequest().body(ApiResponse.fail("Tên vị không được quá 50 ký tự."));
                    }
                    if (!itemNames.add(itemName.toLowerCase())) {
                        return ResponseEntity.badRequest().body(ApiResponse.fail("Tên vị '" + itemName + "' đã tồn tại trong danh sách."));
                    }
                }

                List<Integer> incomingIds = itemsList.stream()
                        .filter(i -> i.get("id") != null)
                        .map(i -> (Integer) i.get("id"))
                        .toList();
                group.getItems().removeIf(item -> item.getId() != null && !incomingIds.contains(item.getId()));
                
                for (Map<String, Object> itemData : itemsList) {
                    String itemName = ((String) itemData.get("name")).trim().replaceAll("\\s+", " ");
                    BigDecimal price = BigDecimal.ZERO;
                    if (itemData.get("surchargePrice") != null) {
                        price = new BigDecimal(itemData.get("surchargePrice").toString());
                        if (price.compareTo(BigDecimal.ZERO) < 0) {
                            return ResponseEntity.badRequest().body(ApiResponse.fail("Giá phụ thu không được là số âm."));
                        }
                        if (price.compareTo(new BigDecimal("100000000")) > 0) {
                            return ResponseEntity.badRequest().body(ApiResponse.fail("Giá tối đa không vượt quá 100.000.000đ."));
                        }
                    }

                    if (itemData.get("id") != null) {
                        Integer itemId = (Integer) itemData.get("id");
                        com.devcine.backend.entity.FnbOptionItem existing = group.getItems().stream()
                                .filter(i -> i.getId().equals(itemId)).findFirst().orElse(null);
                        if (existing != null) {
                            existing.setName(itemName);
                            existing.setSurchargePrice(price);
                        }
                    } else {
                        com.devcine.backend.entity.FnbOptionItem newItem = com.devcine.backend.entity.FnbOptionItem.builder()
                                .group(group)
                                .name(itemName)
                                .surchargePrice(price)
                                .build();
                        group.getItems().add(newItem);
                    }
                }
            }
            fnbOptionGroupRepository.save(group);
            return ResponseEntity.ok(ApiResponse.ok(group));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/groups/{id}")
    @PreAuthorize("@perm.can('fnb_menu','delete')")
    public ResponseEntity<?> deleteOptionGroup(@PathVariable Integer id) {
        try {
            com.devcine.backend.entity.FnbOptionGroup group = fnbOptionGroupRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Kho tùy chọn"));

            // Chặn xoá nếu pool đang được một Ô chọn (Slot) của combo nào đó sử dụng
            // (tránh phá cấu hình combo + vi phạm khoá ngoại). Gỡ slot trước rồi mới xoá pool.
            if (fnbComboSlotRepository.existsByOptionGroup_Id(id)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail(
                        "Kho tùy chọn đang được dùng trong Ô chọn của combo. Hãy gỡ khỏi combo trước khi xoá."));
            }

            fnbOptionGroupRepository.delete(group);
            return ResponseEntity.ok(ApiResponse.success("Đã xoá Kho tùy chọn."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** Toàn bộ thực đơn (kể cả tạm ngưng) cho khu vực quản trị — nhưng ẨN món đã xoá. */
    @GetMapping("/all")
    @PreAuthorize("@perm.can('fnb_menu','view')")
    public ResponseEntity<ApiResponse<List<FnbItem>>> getAllFnbs() {
        return ResponseEntity.ok(ApiResponse.ok(fnbItemRepository.findByIsDeletedFalseOrderByTypeAscNameAsc()));
    }

    @PostMapping
    @PreAuthorize("@perm.can('fnb_menu','add')")
    public ResponseEntity<?> createFnb(@RequestBody Map<String, Object> body) {
        try {
            FnbItem item = FnbItem.builder()
                    .name((String) body.get("name"))
                    .type((String) body.getOrDefault("type", "COMBO"))
                    .price(new BigDecimal(body.get("price").toString()))
                    .imageUrl((String) body.get("imageUrl"))
                    .description((String) body.get("description"))
                    .isActive(body.get("isActive") == null || Boolean.parseBoolean(body.get("isActive").toString()))
                    .build();
            if (body.containsKey("slots")) {
                applySlots(item, body.get("slots"));
            }
            fnbItemRepository.save(item);
            return ResponseEntity.status(201).body(ApiResponse.ok(item));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('fnb_menu','edit')")
    public ResponseEntity<?> updateFnb(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            FnbItem item = fnbItemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món F&B"));
            if (body.containsKey("name")) item.setName((String) body.get("name"));
            if (body.containsKey("type")) item.setType((String) body.get("type"));
            if (body.get("price") != null) item.setPrice(new BigDecimal(body.get("price").toString()));
            if (body.containsKey("imageUrl")) item.setImageUrl((String) body.get("imageUrl"));
            if (body.containsKey("description")) item.setDescription((String) body.get("description"));
            if (body.containsKey("isActive")) item.setIsActive(Boolean.parseBoolean(body.get("isActive").toString()));
            if (body.containsKey("slots")) {
                applySlots(item, body.get("slots"));
            }
            fnbItemRepository.save(item);
            return ResponseEntity.ok(ApiResponse.ok(item));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('fnb_menu','delete')")
    public ResponseEntity<?> deleteFnb(@PathVariable Integer id) {
        try {
            FnbItem item = fnbItemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy F&B"));
            // Soft-delete: đánh dấu đã xoá (KHÁC với tạm ngưng isActive). Giữ row cho lịch sử.
            item.setIsDeleted(true);
            fnbItemRepository.save(item);
            return ResponseEntity.ok(ApiResponse.success("Đã xoá món F&B."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * Dựng lại toàn bộ Ô chọn (Slot) của món từ payload admin.
     * Rebuild-from-scratch: orphanRemoval xoá slot cũ, insert slot mới. An toàn cho
     * lịch sử vì snapshot đơn hàng lưu {@code slotLabel} dạng chuỗi, không FK tới slot.
     */
    @SuppressWarnings("unchecked")
    private void applySlots(FnbItem item, Object slotsRaw) {
        List<Map<String, Object>> slotsList = slotsRaw instanceof List
                ? (List<Map<String, Object>>) slotsRaw : List.of();

        List<Integer> poolIds = slotsList.stream()
                .map(s -> toInt(s.get("optionGroupId")))
                .filter(java.util.Objects::nonNull)
                .distinct().toList();
        Map<Integer, FnbOptionGroup> poolMap = new java.util.HashMap<>();
        fnbOptionGroupRepository.findAllById(poolIds).forEach(p -> poolMap.put(p.getId(), p));

        item.getSlots().clear();
        int order = 0;
        for (Map<String, Object> s : slotsList) {
            Integer poolId = toInt(s.get("optionGroupId"));
            FnbOptionGroup pool = poolId != null ? poolMap.get(poolId) : null;
            if (pool == null) {
                throw new RuntimeException("Kho tùy chọn không hợp lệ cho một Ô chọn.");
            }
            int min = s.get("minChoices") != null ? toInt(s.get("minChoices")) : 1;
            int max = s.get("maxChoices") != null ? toInt(s.get("maxChoices")) : 1;
            if (max < 1) max = 1;
            if (min < 0) min = 0;
            if (min > max) min = max;
            boolean required = s.get("isRequired") != null
                    ? Boolean.parseBoolean(s.get("isRequired").toString()) : (min > 0);
            int displayOrder = s.get("displayOrder") != null ? toInt(s.get("displayOrder")) : order;

            // Vị mặc định (tuỳ chọn) — phải là một Vị THUỘC đúng kho của slot này.
            Integer defaultId = toInt(s.get("defaultOptionItemId"));
            var defaultItem = defaultId == null ? null
                    : pool.getItems().stream()
                        .filter(oi -> defaultId.equals(oi.getId()))
                        .findFirst().orElse(null);

            item.getSlots().add(FnbComboSlot.builder()
                    .fnbItem(item)
                    .optionGroup(pool)
                    .defaultOptionItem(defaultItem)
                    .slotLabel(s.get("slotLabel") != null && !s.get("slotLabel").toString().isBlank()
                            ? s.get("slotLabel").toString() : pool.getName())
                    .displayOrder(displayOrder)
                    .minChoices(min)
                    .maxChoices(max)
                    .isRequired(required)
                    .build());
            order++;
        }
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(o.toString());
    }
}
