package com.devcine.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Yêu cầu đổi ghế đền bù. Hỗ trợ đổi NHIỀU ghế cùng lúc (khách nhóm — Edge #2).
 * Tất cả swap phải thuộc cùng một đơn ({@code bookingId}) và cùng suất của đơn đó.
 */
public record RelocateRequest(
        @NotNull(message = "Thiếu mã đơn.")
        Integer bookingId,

        @NotEmpty(message = "Chọn ít nhất một ghế cần đổi.")
        @Valid
        List<SeatSwap> swaps,

        @Valid
        CompensationRequest compensation,

        @Size(max = 255, message = "Lý do tối đa 255 ký tự.")
        String reason,

        /** Cho phép bỏ qua luật chống ghế mồ côi (mirror POS "allow single seat"). */
        boolean allowOrphan,

        /** Cờ tự động khóa bảo trì ghế cũ (mặc định true từ UI khi đổi do ghế hỏng). */
        Boolean lockOldSeatsAsMaintenance
) {
    public boolean shouldLockOldSeats() {
        return lockOldSeatsAsMaintenance == null || lockOldSeatsAsMaintenance;
    }
    /** Một cặp đổi: ghế nguồn (đang thuộc đơn) → ghế đích (đang trống). */
    public record SeatSwap(
            @NotNull(message = "Thiếu ghế nguồn.")
            Integer oldSeatId,
            @NotNull(message = "Thiếu ghế đích.")
            Integer newSeatId
    ) {}
}
