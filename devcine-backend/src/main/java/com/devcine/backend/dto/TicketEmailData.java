package com.devcine.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu phẳng (primitives) phục vụ gửi email vé điện tử.
 * Cố tình KHÔNG mang entity LAZY để luồng @Async gửi mail không bị
 * LazyInitializationException khi chạy ngoài transaction.
 */
public record TicketEmailData(
        String toEmail,
        String customerName,
        String bookingCode,
        String movieTitle,
        String cinemaName,
        String roomName,
        LocalDateTime startTime,
        String paymentMethod,
        BigDecimal finalPrice,
        List<SeatLine> seats,
        List<FnbLine> fnbs
) {
    /** Một vé/ghế: nhãn ghế (vd "A5"), loại đối tượng, mã QR check-in. */
    public record SeatLine(String seatLabel, String ticketType, String qrCode) {}

    /** Một dòng combo/đồ ăn kèm số lượng. */
    public record FnbLine(String name, Integer quantity) {}
}
