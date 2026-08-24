package com.devcine.backend.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ngữ cảnh vé để xử lý sự cố (dùng chung cho tra theo Mã vé / SĐT / chọn ghế trên sơ đồ).
 */
@Builder
public record IncidentBookingContext(
        Integer bookingId,
        String bookingCode,
        String channel,            // ONLINE | POS
        boolean hasCustomer,       // false = khách vãng lai → không phát voucher điện tử (Edge #4)
        Integer customerId,
        String customerName,
        String customerPhone,
        ShowtimeBrief showtime,
        List<IncidentSeatLine> seats
) {

    @Builder
    public record ShowtimeBrief(
            Integer showtimeId,
            String movieTitle,
            String roomName,
            String formatName,
            LocalDateTime startTime,
            Integer cinemaId,
            String cinemaName,
            boolean started,        // suất đã bắt đầu → chặn đổi ghế, chỉ cho hủy/goodwill (Edge #5)
            boolean expired         // suất kết thúc > 2h → chặn TOÀN BỘ thao tác xử lý sự cố
    ) {}

    @Builder
    public record IncidentSeatLine(
            Integer bookingSeatId,
            Integer seatId,
            String seatLabel,
            String seatType,       // SWEETBOX | VIP | NORMAL (chỉ hiển thị — flat pricing)
            String ticketType,     // ADULT | U22 | CHILD | SENIOR
            BigDecimal priceSnapshot,
            String status          // SOLD | RELOCATED | CANCELLED
    ) {}
}
