package com.devcine.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu phẳng (primitives) phục vụ gửi email thông báo đổi ghế sự cố & phát voucher đền bù.
 * Cố tình KHÔNG mang entity LAZY để luồng @Async gửi mail không bị
 * LazyInitializationException khi chạy ngoài transaction.
 */
public record IncidentRelocateEmailData(
        String toEmail,
        String customerName,
        String bookingCode,
        String movieTitle,
        String cinemaName,
        String roomName,
        LocalDateTime startTime,
        String reason,
        List<SeatSwapLine> swaps,
        boolean voucherIssued,
        String voucherCode,
        String voucherLabel,
        BigDecimal voucherValue,
        String voucherType,
        boolean counterGift,
        List<TicketEmailData.SeatLine> seats,
        List<TicketEmailData.FnbLine> fnbs
) {
    /** Một cặp đổi ghế: nhãn cũ -> nhãn mới (vd: "F9" -> "D7"). */
    public record SeatSwapLine(String oldSeatLabel, String newSeatLabel) {}
}
