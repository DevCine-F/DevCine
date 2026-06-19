package com.devcine.backend.controller;

import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import com.devcine.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.devcine.backend.dto.request.FnbSelectionDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ticketing")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TicketingController {

    private final ShowtimeRepository showtimeRepository;
    private final FnbItemRepository fnbItemRepository;
    private final CustomerRepository customerRepository;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    // Suất chiếu cho POS: từ đầu ngày hôm nay trở đi (chưa diễn ra hoặc đang trong ngày), sắp xếp tăng dần
    @GetMapping("/showtimes")
    public ResponseEntity<?> getTodayShowtimes() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        List<Showtime> showtimes = showtimeRepository.findAll().stream()
                .filter(s -> s.getStartTime() != null && !s.getStartTime().isBefore(startOfToday))
                .sorted(Comparator.comparing(Showtime::getStartTime))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = showtimes.stream().map(s -> Map.<String, Object>of(
                "id", s.getId(),
                "movieTitle", s.getMovie().getTitle(),
                "moviePoster", s.getMovie().getPosterUrl() != null ? s.getMovie().getPosterUrl() : "",
                "startTime", s.getStartTime().toString(),
                "roomName", s.getRoom().getName(),
                "formatName", s.getFormat().getName(),
                "status", s.getStatus() != null ? s.getStatus() : "SCHEDULED"
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // Lấy danh sách F&B combo cho POS
    @GetMapping("/combos")
    public ResponseEntity<?> getFnbCombos() {
        List<FnbItem> items = fnbItemRepository.findAll();
        return ResponseEntity.ok(items);
    }

    // Tra cứu khách hàng theo thẻ thành viên (userId làm số thẻ)
    @GetMapping("/member-card/{cardNumber}")
    public ResponseEntity<?> lookupMemberCard(@PathVariable String cardNumber) {
        try {
            Integer customerId = Integer.parseInt(cardNumber);
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thẻ thành viên"));
            return ResponseEntity.ok(Map.of(
                    "customerId", customer.getUserId(),
                    "fullName", customer.getUser() != null ? customer.getUser().getFullName() : "Khách hàng",
                    "membershipTier", customer.getMembershipTier() != null ? customer.getMembershipTier() : "BRONZE",
                    "loyaltyPoints", customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Số thẻ không hợp lệ"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Thanh toán tại quầy (POS checkout)
    @PostMapping("/pay")
    @Transactional
    public ResponseEntity<?> posCheckout(@RequestBody Map<String, Object> body) {
        try {
            // POS tạo booking CONFIRMED trực tiếp (không qua hold)
            Integer showtimeId = Integer.parseInt(body.get("showtimeId").toString());
            @SuppressWarnings("unchecked")
            List<Integer> seatIds = (List<Integer>) body.get("seatIds");
            String paymentMethod = (String) body.getOrDefault("paymentMethod", "CASH");
            Integer customerId = body.get("customerId") != null
                    ? Integer.parseInt(body.get("customerId").toString()) : null;

            // F&B / combo kèm theo (nếu có)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fnbsRaw = (List<Map<String, Object>>) body.get("fnbs");
            List<FnbSelectionDTO> fnbs = fnbsRaw == null ? List.of() : fnbsRaw.stream()
                    .map(m -> FnbSelectionDTO.builder()
                            .fnbItemId(Integer.parseInt(m.get("fnbItemId").toString()))
                            .quantity(Integer.parseInt(m.get("quantity").toString()))
                            .build())
                    .collect(Collectors.toList());

            com.devcine.backend.dto.request.BookingRequestDTO req =
                    com.devcine.backend.dto.request.BookingRequestDTO.builder()
                            .showtimeId(showtimeId)
                            .seatIds(seatIds)
                            .fnbs(fnbs)
                            .customerId(customerId)
                            .paymentMethod(paymentMethod)
                            .build();

            Booking booking = bookingService.holdSeats(req);
            bookingService.completePayment(booking.getId(), paymentMethod);

            // Vé đã được sinh trong completePayment — lấy QR + nhãn ghế để in hoá đơn/soát vé tại cổng
            List<Map<String, Object>> tickets = ticketRepository.findAllByBookingIdWithSeat(booking.getId())
                    .stream()
                    .map(t -> {
                        Seat seat = t.getBookingSeat().getSeat();
                        return Map.<String, Object>of(
                                "seatLabel", seat.getRowChar() + seat.getColNum(),
                                "qrCode", t.getQrCode()
                        );
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "bookingId", booking.getId(),
                    "bookingCode", booking.getBookingCode(),
                    "message", "Thanh toán thành công",
                    "tickets", tickets
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
