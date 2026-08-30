package com.devcine.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Kết quả quét QR & in vé theo ĐƠN HÀNG (không theo từng ghế).
 * Dùng để hiển thị chi tiết đơn tại quầy và dựng bản in vé giấy nhiệt K80.
 */
public record BookingPrintResponse(
        String bookingCode,
        String movieTitle,
        String cinemaName,
        String cinemaAddress,
        String roomName,
        String roomType,
        String format,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String paymentMethod,
        BigDecimal totalPrice,
        BigDecimal finalPrice,
        BigDecimal discount,
        String memberName,
        int seatCount,
        List<SeatLine> seats,
        List<FnbLine> fnbs,
        LocalDateTime printedAt,
        String cashierName,
        boolean requiresStudentVerification
) {
    /** Một ghế trong đơn: nhãn ghế, loại đối tượng, giá đã chốt và QR vé hiện hành. */
    public record SeatLine(String seatLabel, String ticketType, BigDecimal price, String qrCode) {}

    /** Một dòng combo/đồ ăn kèm: tên, số lượng, đơn giá đã chốt, tổng phụ thu và các lựa chọn đi kèm. */
    public record FnbLine(
            String name,
            Integer quantity,
            BigDecimal price,
            BigDecimal surchargePrice,
            List<FnbOptionLine> options
    ) {
        public record FnbOptionLine(String slotLabel, String optionName, BigDecimal surcharge) {}
    }
}

