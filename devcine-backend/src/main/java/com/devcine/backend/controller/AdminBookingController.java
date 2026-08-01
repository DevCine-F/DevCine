package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.Seat;
import com.devcine.backend.repository.BookingFnbRepository;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingFnbRepository bookingFnbRepository;
    private final TicketRepository ticketRepository;

    private static final LocalDateTime MIN_DATE = LocalDateTime.of(2000, 1, 1, 0, 0);

    @GetMapping
    @PreAuthorize("@perm.can('bookings', 'view')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> list(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String method,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Integer staffUserId = isAdmin ? null : (Integer) auth.getPrincipal();

        LocalDateTime fromDt = parseStart(from, MIN_DATE);
        LocalDateTime toDt = parseEnd(to, LocalDateTime.now().plusYears(10));

        Page<Booking> result = bookingRepository.searchForAdmin(
                q.trim(), status.trim().toUpperCase(), method.trim().toUpperCase(), staffUserId,
                fromDt, toDt, PageRequest.of(page, size));

        List<Integer> ids = result.getContent().stream().map(Booking::getId).collect(Collectors.toList());
        Map<Integer, Long> seatCounts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : bookingRepository.countSeatsByBookingIds(ids)) {
                seatCounts.put((Integer) row[0], (Long) row[1]);
            }
        }

        List<Map<String, Object>> content = result.getContent().stream().map(b -> {
            boolean hasCustomer = b.getCustomer() != null && b.getCustomer().getUser() != null;
            Map<String, Object> m = new HashMap<>();
            m.put("bookingId", b.getId());
            m.put("bookingCode", nn(b.getBookingCode()));
            m.put("status", nn(b.getStatus()));
            m.put("paymentMethod", nn(b.getPaymentMethod()));
            m.put("totalPrice", b.getTotalPrice());
            m.put("finalPrice", b.getFinalPrice());
            m.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
            m.put("customerName", hasCustomer ? b.getCustomer().getUser().getFullName() : "Khách tại quầy");
            m.put("channel", channelOf(b.getPaymentMethod()));
            m.put("movieTitle", b.getShowtime().getMovie().getTitle());
            m.put("roomName", b.getShowtime().getRoom().getName());
            m.put("showtimeStart", b.getShowtime().getStartTime().toString());
            m.put("seatCount", seatCounts.getOrDefault(b.getId(), 0L));
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "content", content,
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        )));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.can('bookings', 'view')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detail(@PathVariable Integer id) {
        Booking b = bookingRepository.findDetailById(id).orElse(null);
        if (b == null) return ResponseEntity.status(404).body(ApiResponse.fail("Không tìm thấy hoá đơn."));

        List<Map<String, Object>> seats = bookingSeatRepository.findAllByBookingIdWithSeat(id).stream().map(bs -> {
            Seat seat = bs.getSeat();
            Map<String, Object> s = new HashMap<>();
            s.put("label", seat.displayLabel());
            s.put("seatType", seat.getSeatType() != null ? seat.getSeatType().getName() : "");
            s.put("ticketType", nn(bs.getTicketType()));
            s.put("price", bs.getPriceSnapshot());
            return s;
        }).collect(Collectors.toList());

        List<Map<String, Object>> fnbs = bookingFnbRepository.findByBookingIdWithFnb(id).stream().map(bf -> {
            Map<String, Object> f = new HashMap<>();
            f.put("name", bf.getFnbItem().getName());
            f.put("quantity", bf.getQuantity());
            f.put("price", bf.getPriceSnapshot());
            return f;
        }).collect(Collectors.toList());

        List<Map<String, Object>> tickets = ticketRepository.findAllByBookingIdWithSeat(id).stream().map(t -> {
            Seat seat = t.getBookingSeat().getSeat();
            Map<String, Object> tk = new HashMap<>();
            tk.put("seatLabel", seat.displayLabel());
            tk.put("qrCode", t.getQrCode());
            tk.put("isCheckedIn", Boolean.TRUE.equals(t.getIsCheckedIn()));
            return tk;
        }).collect(Collectors.toList());

        boolean hasCustomer = b.getCustomer() != null && b.getCustomer().getUser() != null;
        Map<String, Object> dto = new HashMap<>();
        dto.put("bookingId", b.getId());
        dto.put("bookingCode", nn(b.getBookingCode()));
        dto.put("status", nn(b.getStatus()));
        dto.put("paymentMethod", nn(b.getPaymentMethod()));
        dto.put("channel", channelOf(b.getPaymentMethod()));
        dto.put("totalPrice", b.getTotalPrice());
        dto.put("finalPrice", b.getFinalPrice());
        dto.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
        dto.put("customerName", hasCustomer ? b.getCustomer().getUser().getFullName() : "Khách tại quầy");
        dto.put("customerPhone", hasCustomer ? b.getCustomer().getUser().getPhone() : null);
        dto.put("membershipTier", hasCustomer ? b.getCustomer().getMembershipTier() : null);
        dto.put("movieTitle", b.getShowtime().getMovie().getTitle());
        dto.put("roomName", b.getShowtime().getRoom().getName());
        dto.put("formatName", b.getShowtime().getFormat().getName());
        dto.put("showtimeStart", b.getShowtime().getStartTime().toString());
        dto.put("voucherCode", b.getVoucher() != null && b.getVoucher().getPromotion() != null
                ? b.getVoucher().getPromotion().getCode() : null);
        dto.put("seats", seats);
        dto.put("fnbs", fnbs);
        dto.put("tickets", tickets);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    // ---- helpers ----
    private String channelOf(String method) {
        if (method == null) return "—";
        switch (method.toUpperCase()) {
            case "CASH": case "CARD": case "TRANSFER": return "Quầy (POS)";
            case "VNPAY": return "Online";
            default: return method;
        }
    }

    private String nn(String s) {
        return s != null ? s : "";
    }

    private LocalDateTime parseStart(String s, LocalDateTime def) {
        if (s == null || s.isBlank()) return def;
        try {
            return LocalDate.parse(s).atStartOfDay();
        } catch (Exception e) {
            return def;
        }
    }

    private LocalDateTime parseEnd(String s, LocalDateTime def) {
        if (s == null || s.isBlank()) return def;
        try {
            return LocalDate.parse(s).atTime(LocalTime.MAX);
        } catch (Exception e) {
            return def;
        }
    }
}
