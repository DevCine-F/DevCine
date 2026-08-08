package com.devcine.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Kết quả quét QR & in vé theo ĐƠN HÀNG (không theo từng ghế).
 * Dùng để hiển thị chi tiết đơn tại quầy và dựng bản in vé giấy.
 */
public record BookingPrintResponse(
        String bookingCode,
        String movieTitle,
        String cinemaName,
        String roomName,
        String format,
        LocalDateTime startTime,
        String paymentMethod,
        BigDecimal totalPrice,
        BigDecimal finalPrice,
        BigDecimal discount,
        String memberName,
        int seatCount,
        List<SeatLine> seats,
        List<FnbLine> fnbs,
        LocalDateTime printedAt,
        boolean requiresStudentVerification
) {
    /** Một ghế trong đơn: nhãn ghế (vd "A5"), loại đối tượng, giá đã chốt. */
    public record SeatLine(String seatLabel, String ticketType, BigDecimal price) {}

    /** Một dòng combo/đồ ăn kèm: tên, số lượng, đơn giá đã chốt. */
    public record FnbLine(String name, Integer quantity, BigDecimal price) {}
}
