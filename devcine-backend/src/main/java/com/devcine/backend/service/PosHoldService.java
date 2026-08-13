package com.devcine.backend.service;

import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.BookingSeat;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Hỗ trợ "Hoá đơn chờ" tại POS: giải phóng (release) một booking đang giữ ghế (HOLD)
 * khi nhân viên huỷ đơn chờ hoặc đơn hết giờ giữ.
 *
 * <p>Tách khỏi {@code BookingService} (file bảo vệ) — chỉ thao tác repository có sẵn,
 * không sửa nghiệp vụ đặt/thanh toán hiện hành. Việc TẠO hold tái dùng
 * {@code BookingService.holdSeats} nên không cần lặp lại ở đây.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosHoldService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Nhả ghế của một đơn chờ về AVAILABLE (đặt bookingSeat HOLD → EXPIRED, booking → CANCELLED).
     *
     * @return trạng thái xử lý: {@code NOT_FOUND} | {@code CONFIRMED} (đã thanh toán, không nhả)
     *         | {@code RELEASED}
     */
    @Transactional
    public String releaseHold(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return "NOT_FOUND";
        }
        // Đơn đã thanh toán xong → tuyệt đối không nhả ghế (chống bán trùng / mất vé đã bán)
        if ("CONFIRMED".equals(booking.getStatus())) {
            return "CONFIRMED";
        }
        // Đã nhả trước đó (Idempotent) -> trả về thành công luôn, tránh loop/bắn WebSocket
        if ("EXPIRED".equals(booking.getStatus()) || "CANCELLED".equals(booking.getStatus())) {
            return "RELEASED";
        }
        // Nhả cả đơn giữ POS (PENDING_PAYMENT/PAYING) chứ không chỉ "HOLD". Trước đây guard chỉ chấp
        // nhận "HOLD" khiến Huỷ đơn POS thành no-op im lặng: ghế kẹt + vẫn bị đếm vào hạn mức 3 đơn chờ.
        String st = booking.getStatus();
        if (!"HOLD".equals(st) && !"PENDING_PAYMENT".equals(st) && !"PAYING".equals(st)) {
            return st; // Trạng thái khác (lạ) → không xử lý nhả ghế
        }

        List<BookingSeat> seats = bookingSeatRepository.findAllByBookingIdWithSeat(bookingId);
        for (BookingSeat bs : seats) {
            if ("HOLD".equals(bs.getStatus())) {
                bs.setStatus("EXPIRED");
            }
        }
        bookingSeatRepository.saveAll(seats);

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        log.info("Đã nhả {} ghế của đơn chờ {} (booking #{}).", seats.size(), booking.getBookingCode(), bookingId);
        
        try {
            List<Integer> seatIds = seats.stream().map(bs -> bs.getSeat().getId()).collect(Collectors.toList());
            Object payload = Map.of("type", "SEAT_RELEASED", "seatIds", seatIds, "by", "");
            messagingTemplate.convertAndSend("/topic/showtime/" + booking.getShowtime().getId(), payload);
        } catch (Exception e) {
            log.warn("Mạng WebSocket ngắt kết nối đột ngột khi releaseHold booking #{}: {}", bookingId, e.getMessage());
        }

        return "RELEASED";
    }
}
