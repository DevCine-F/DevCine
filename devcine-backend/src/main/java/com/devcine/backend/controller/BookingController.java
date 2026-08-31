package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.request.BookingRequestDTO;
import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.BookingFnb;
import com.devcine.backend.entity.BookingSeat;
import com.devcine.backend.entity.Ticket;
import com.devcine.backend.repository.BookingFnbRepository;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.repository.TicketRepository;
import com.devcine.backend.service.BookingService;
import com.devcine.backend.service.PosHoldService;
import com.devcine.backend.util.SecurityUtils;
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
    private final BookingFnbRepository bookingFnbRepository;

    @PostMapping("/hold")
    public ResponseEntity<?> holdSeats(@RequestBody BookingRequestDTO request) {
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
    public ResponseEntity<?> getBookingHistory() {
        try {
            Integer customerId = SecurityUtils.getCurrentUserId();
            if (customerId == null) {
                return ResponseEntity.status(401).body(ApiResponse.fail("Unauthorized"));
            }

            List<Booking> bookings = bookingRepository.findByCustomerIdWithDetails(customerId);
            if (bookings.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.ok(List.of()));
            }

            List<Integer> bookingIds = bookings.stream().map(Booking::getId).toList();
            
            // Lấy toàn bộ dữ liệu phụ trợ trong 3 truy vấn (Tránh N+1)
            List<BookingSeat> allSeats = bookingRepository.findAllSeatsByBookingIds(bookingIds);
            List<Ticket> allTickets = bookingRepository.findAllTicketsByBookingIds(bookingIds);
            List<BookingFnb> allFnbs = bookingRepository.findAllFnbsByBookingIds(bookingIds);

            // Nhóm dữ liệu theo bookingId trong memory
            Map<Integer, List<BookingSeat>> seatsMap = allSeats.stream()
                    .collect(Collectors.groupingBy(bs -> bs.getBooking().getId()));
            Map<Integer, List<Ticket>> ticketsMap = allTickets.stream()
                    .collect(Collectors.groupingBy(t -> t.getBookingSeat().getBooking().getId()));
            Map<Integer, List<BookingFnb>> fnbsMap = allFnbs.stream()
                    .collect(Collectors.groupingBy(bf -> bf.getBooking().getId()));

            List<Map<String, Object>> result = bookings.stream().map(b -> {
                List<BookingSeat> seats = seatsMap.getOrDefault(b.getId(), List.of());
                List<Ticket> tickets = ticketsMap.getOrDefault(b.getId(), List.of());
                List<BookingFnb> fnbs = fnbsMap.getOrDefault(b.getId(), List.of());

                String seatLabels = seats.stream()
                        .filter(bs -> bs.getSeat() != null)
                        .map(bs -> bs.getSeat().displayLabel())
                        .collect(Collectors.joining(", "));
                        
                List<Map<String, Object>> fnbList = fnbs.stream().map(f -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    // Lịch sử: ưu tiên snapshot tên món; fallback FK cho đơn cũ.
                    m.put("itemName", f.getItemNameSnapshot() != null ? f.getItemNameSnapshot()
                            : (f.getFnbItem() != null ? f.getFnbItem().getName() : ""));
                    m.put("quantity", f.getQuantity());
                    m.put("priceSnapshot", f.getPriceSnapshot());

                    List<Map<String, Object>> options = f.getOptions() != null ? f.getOptions().stream().map(o -> {
                        Map<String, Object> om = new java.util.HashMap<>();
                        om.put("slotLabel", o.getSlotLabelSnapshot());
                        om.put("optionName", o.getOptionNameSnapshot());
                        om.put("surcharge", o.getSurchargeSnapshot());
                        return om;
                    }).collect(Collectors.toList()) : List.of();
                    m.put("options", options);

                    return m;
                }).collect(Collectors.toList());

                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("bookingId", b.getId());
                map.put("bookingCode", b.getBookingCode() != null ? b.getBookingCode() : "");
                map.put("status", b.getStatus() != null ? b.getStatus() : "");
                
                java.math.BigDecimal originalPrice = b.getTotalPrice() != null ? b.getTotalPrice() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal finalPrice = b.getFinalPrice() != null ? b.getFinalPrice() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal discountAmount = originalPrice.subtract(finalPrice);
                java.math.BigDecimal fnbTotal = fnbs.stream()
                        .map(f -> (f.getPriceSnapshot() != null ? f.getPriceSnapshot() : java.math.BigDecimal.ZERO)
                                .multiply(java.math.BigDecimal.valueOf(f.getQuantity() != null ? f.getQuantity() : 0)))
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                map.put("totalPrice", originalPrice);
                map.put("finalPrice", finalPrice);
                map.put("originalPrice", originalPrice);
                map.put("discountAmount", discountAmount);
                map.put("fnbTotal", fnbTotal);
                map.put("paymentMethod", b.getPaymentMethod() != null ? b.getPaymentMethod() : "");
                
                boolean requiresStudentVerification = seats.stream().anyMatch(bs -> 
                    bs.getSeat() != null && bs.getSeat().getSeatType() != null && "SWEETBOX".equals(bs.getSeat().getSeatType().getName()) &&
                    bs.getTicketType() != null && List.of("U22", "CHILD", "SENIOR").contains(bs.getTicketType().toUpperCase())
                );
                map.put("requiresStudentVerification", requiresStudentVerification);
                map.put("createdAt", b.getCreatedAt().toString());
                
                java.util.Map<String, Object> showtimeMap = new java.util.HashMap<>();
                showtimeMap.put("id", b.getShowtime().getId());
                showtimeMap.put("startTime", b.getShowtime().getStartTime().toString());
                showtimeMap.put("showDate", b.getShowtime().getStartTime().toLocalDate().toString());
                showtimeMap.put("movieTitle", b.getShowtime().getMovie().getTitle());
                showtimeMap.put("moviePosterUrl", b.getShowtime().getMovie().getPosterUrl() != null ? b.getShowtime().getMovie().getPosterUrl() : "");
                showtimeMap.put("format", b.getShowtime().getFormat() != null ? b.getShowtime().getFormat().getName() : "");
                showtimeMap.put("ageRating", b.getShowtime().getMovie().getAgeRating() != null ? b.getShowtime().getMovie().getAgeRating() : "");
                showtimeMap.put("cinemaName", (b.getShowtime().getRoom() != null && b.getShowtime().getRoom().getCinema() != null) ? b.getShowtime().getRoom().getCinema().getName() : "");
                showtimeMap.put("roomName", b.getShowtime().getRoom() != null ? b.getShowtime().getRoom().getName() : "");
                
                map.put("showtime", showtimeMap);
                map.put("seats", seatLabels);
                
                List<Map<String, Object>> seatsDetail = seats.stream().map(bs -> {
                    Map<String, Object> sm = new java.util.HashMap<>();
                    sm.put("seatNumber", bs.getSeat() != null ? bs.getSeat().displayLabel() : "");
                    sm.put("seatType", (bs.getSeat() != null && bs.getSeat().getSeatType() != null) ? bs.getSeat().getSeatType().getName() : "");
                    sm.put("targetType", bs.getTicketType() != null ? bs.getTicketType() : "");
                    return sm;
                }).collect(Collectors.toList());
                map.put("seatsDetail", seatsDetail);
                
                map.put("fnbs", fnbList);
                map.put("qrCodes", tickets.stream().map(Ticket::getQrCode).collect(Collectors.toList()));
                
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        }
    }
}
