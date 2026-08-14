package com.devcine.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Yêu cầu hủy chỗ (khi hết ghế thay thế / khách từ chối đổi). Không hoàn tiền — đền bằng
 * voucher trị giá bằng đúng giá vé đã mua (thường dùng template GIFT_TICKET / COMP_TICKET_FULL).
 */
public record CancelSeatRequest(
        @NotNull(message = "Thiếu mã đơn.")
        Integer bookingId,

        @NotEmpty(message = "Chọn ít nhất một ghế cần hủy.")
        List<Integer> bookingSeatIds,

        @Valid
        CompensationRequest compensation,

        @Size(max = 255, message = "Lý do tối đa 255 ký tự.")
        String reason
) {}
