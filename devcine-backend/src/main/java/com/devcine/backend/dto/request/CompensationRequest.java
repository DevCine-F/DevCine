package com.devcine.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Khối đền bù dùng chung cho luồng đổi ghế / hủy chỗ.
 *
 * <p>Cây quyết định phía Service (client KHÔNG tự quyết):
 * <ul>
 *   <li>{@code type = NONE} → chỉ ghi vết, không phát gì.</li>
 *   <li>Khách CÓ tài khoản + type ≠ NONE + có {@code promotionTemplateId} → phát Voucher thật.</li>
 *   <li>Khách VÃNG LAI (không Customer) → luôn đền trực tiếp tại quầy: KHÔNG sinh Voucher,
 *       chỉ ghi vết {@code seat_incidents} (Edge #4 phương án a).</li>
 * </ul>
 */
public record CompensationRequest(
        /** NONE | DISCOUNT | GIFT_FNB | GIFT_TICKET. */
        @NotBlank(message = "Vui lòng chọn hình thức đền bù.")
        String type,

        /** ID Promotion-template đền bù đã seed (null khi type=NONE hoặc đền trực tiếp tại quầy). */
        Integer promotionTemplateId,

        /** Ghi chú tay, ví dụ: "In phiếu bắp nước cứng đền tại quầy". */
        @Size(max = 255, message = "Ghi chú tối đa 255 ký tự.")
        String note
) {}
