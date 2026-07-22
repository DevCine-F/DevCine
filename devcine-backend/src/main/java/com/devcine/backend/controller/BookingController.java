package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.request.BookingRequestDTO;
import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.BookingSeat;
import com.devcine.backend.entity.Ticket;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.repository.TicketRepository;
import com.devcine.backend.service.BookingService;
import com.devcine.backend.service.PosHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final PosHoldService posHoldService;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final TicketRepository ticketRepository;

    @PostMapping("/hold")
    public ResponseEntity<?> holdSeats(@Valid @RequestBody BookingRequestDTO request) {
        try {
            Booking booking = bookingService.holdSeats(request);
            return ResponseEntity.ok(ApiResponse.ok(booking));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        }
    }

    @PostMapping("/{bookingId}/payment/complete")
    public ResponseEntity<?> completePayment(@PathVariable Integer bookingId,
                                              @RequestParam String paymentMethod) {
        try {
            bookingService.completePayment(bookingId, paymentMethod);
            return ResponseEntity.ok(ApiResponse.success("Payment completed and booking confirmed"));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        }
    }

    /**
     * Nhả một đơn đang giữ ghế (PENDING) khi khách hết giờ giữ chỗ ở bước đặt vé online →
     * ghế mở lại cho người khác mua ngay. Đơn đã CONFIRMED sẽ KHÔNG bị nhả.
     */
    @PostMapping("/{bookingId}/release")
    public ResponseEntity<?> releaseHold(@PathVariable Integer bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("result", posHoldService.releaseHold(bookingId))));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getBookingHistory(@RequestParam Integer customerId) {
        try {
            List<Booking> bookings = bookingRepository.findByCustomerIdWithDetails(customerId);
            List<Map<String, Object>> result = bookings.stream().map(b -> {
                List<BookingSeat> seats = bookingSeatRepository.findAllByBookingId(b.getId());
                // Lấy vé theo booking trong 1 truy vấn (tránh N+1 gọi findByBookingSeatId trong vòng lặp)
                List<Ticket> tickets = ticketRepository.findAllByBookingId(b.getId());

                String seatLabels = seats.stream()
                        .filter(bs -> bs.getSeat() != null)
                        .map(bs -> bs.getSeat().getRowChar() + bs.getSeat().getColNum())
                        .collect(Collectors.joining(", "));

                return Map.of(
                        "bookingId", b.getId(),
                        "bookingCode", b.getBookingCode() != null ? b.getBookingCode() : "",
                        "status", b.getStatus() != null ? b.getStatus() : "",
                        "totalPrice", b.getTotalPrice(),
                        "finalPrice", b.getFinalPrice(),
                        "paymentMethod", b.getPaymentMethod() != null ? b.getPaymentMethod() : "",
                        "createdAt", b.getCreatedAt().toString(),
                        "showtime", Map.of(
                                "id", b.getShowtime().getId(),
                                "startTime", b.getShowtime().getStartTime().toString(),
                                "movieTitle", b.getShowtime().getMovie().getTitle(),
                                "moviePosterUrl", b.getShowtime().getMovie().getPosterUrl() != null ? b.getShowtime().getMovie().getPosterUrl() : ""
                        ),
                        "seats", seatLabels,
                        "qrCodes", tickets.stream().map(Ticket::getQrCode).collect(Collectors.toList())
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        }
    }
}
