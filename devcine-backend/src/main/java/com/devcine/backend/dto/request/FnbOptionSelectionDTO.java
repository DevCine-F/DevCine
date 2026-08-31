package com.devcine.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Một lựa chọn tùy chọn do client gửi lên khi chốt đơn.
 *
 * <p>{@code slotId} cho biết lựa chọn thuộc Ô chọn (Slot) nào của món — bắt buộc
 * theo mô hình Combo Slot mới để backend xác thực ràng buộc min/max/required và
 * kiểm tra option có thuộc đúng pool của slot hay không. {@code optionGroupId} chỉ
 * còn mang tính tham chiếu (suy ra được từ slot) và có thể null.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FnbOptionSelectionDTO {
    private Integer slotId;
    private Integer optionGroupId;
    private Integer optionItemId;
    /** Phụ thu lock tại thời điểm khách/thu ngân chọn (Price Lock at Selection). */
    private BigDecimal clientSurcharge;
}
