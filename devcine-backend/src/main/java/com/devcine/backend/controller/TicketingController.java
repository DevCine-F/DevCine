package com.devcine.backend.controller;

import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import com.devcine.backend.service.BookingService;
import com.devcine.backend.service.ConcessionService;
import com.devcine.backend.service.PosHoldService;
import com.devcine.backend.service.ShiftAccessService;
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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/ticketing")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TicketingController {

    private final ShowtimeRepository showtimeRepository;
    private final FnbItemRepository fnbItemRepository;
    private final CustomerRepository customerRepository;
    private final BookingService bookingService;
    private final ConcessionService concessionService;
    private final PosHoldService posHoldService;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final ShiftAccessService shiftAccessService;

    // Suất chiếu cho POS: từ đầu ngày hôm nay trở đi (chưa diễn ra hoặc đang trong ngày), sắp xếp tăng dần
    @GetMapping("/showtimes")
    @PreAuthorize("@perm.can('pos_ticketing', 'view')")
    public ResponseEntity<?> getTodayShowtimes() {
        shiftAccessService.requireCurrentShiftForStaff(List.of("POS_TICKETING", "SHIFT_LEAD"), "ban ve POS");
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
    @PreAuthorize("@perm.can('pos_ticketing', 'view')")
    public ResponseEntity<?> getFnbCombos() {
        shiftAccessService.requireCurrentShiftForStaff(List.of("FNB", "SHIFT_LEAD"), "quay F&B");
        List<FnbItem> items = fnbItemRepository.findAll();
        return ResponseEntity.ok(items);
    }

    // Tra cứu khách hàng theo SỐ ĐIỆN THOẠI để tích điểm tại quầy
    @GetMapping("/member-card/{phone}")
    @PreAuthorize("@perm.can('pos_ticketing', 'view')")
    public ResponseEntity<?> lookupMemberCard(@PathVariable String phone) {
        shiftAccessService.requireCurrentShiftForStaff(List.of("POS_TICKETING", "FNB", "SHIFT_LEAD"), "nghiep vu tai quay");
        try {
            String p = phone == null ? "" : phone.trim().replaceAll("\\s+", "").replaceFirst("^\\+84", "0");
            if (p.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập số điện thoại"));
            }
            Customer customer = customerRepository.findByUserPhone(p).stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với số điện thoại này"));
            return ResponseEntity.ok(Map.of(
                    "customerId", customer.getUserId(),
                    "fullName", customer.getUser() != null ? customer.getUser().getFullName() : "Khách hàng",
                    "phone", customer.getUser() != null && customer.getUser().getPhone() != null ? customer.getUser().getPhone() : "",
                    "membershipTier", customer.getMembershipTier() != null ? customer.getMembershipTier() : "BRONZE",
                    "loyaltyPoints", customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Thanh toán tại quầy (POS checkout)
    @PostMapping("/pay")
    @PreAuthorize("@perm.can('pos_ticketing', 'add')")
    @Transactional
    public ResponseEntity<?> posCheckout(@RequestBody Map<String, Object> body) {
        try {
            StaffSchedule schedule = shiftAccessService.requireCurrentShiftForStaff(List.of("POS_TICKETING", "SHIFT_LEAD"), "ban ve POS");
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

            Booking booking = bookingService.holdSeatsForStaffSchedule(req, schedule);
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
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Bán nhanh bắp nước độc lập tại quầy (Concession Only) — không suất chiếu / không ghế
    @PostMapping("/concession")
    @PreAuthorize("@perm.can('pos_ticketing', 'add')")
    @Transactional
    public ResponseEntity<?> concessionCheckout(@RequestBody Map<String, Object> body) {
        try {
            StaffSchedule schedule = shiftAccessService.requireCurrentShiftForStaff(List.of("FNB", "SHIFT_LEAD"), "quay F&B");
            String paymentMethod = (String) body.getOrDefault("paymentMethod", "CASH");
            Integer customerId = body.get("customerId") != null
                    ? Integer.parseInt(body.get("customerId").toString()) : null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fnbsRaw = (List<Map<String, Object>>) body.get("fnbs");
            List<FnbSelectionDTO> fnbs = fnbsRaw == null ? List.of() : fnbsRaw.stream()
                    .map(m -> FnbSelectionDTO.builder()
                            .fnbItemId(Integer.parseInt(m.get("fnbItemId").toString()))
                            .quantity(Integer.parseInt(m.get("quantity").toString()))
                            .build())
                    .collect(Collectors.toList());

            ConcessionSale sale = concessionService.createSale(fnbs, customerId, paymentMethod, schedule);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "saleId", sale.getId(),
                    "saleCode", sale.getSaleCode(),
                    "message", "Thanh toán thành công"
            ));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ===== Hoá đơn chờ có vé: giữ ghế thật trong DB (khoá toàn hệ thống) =====

    // Giữ đơn vé → tạo booking HOLD (ghế chuyển HOLD, online/quầy khác lập tức không chọn được)
    // Không gắn @Transactional ở controller: holdSeats đã @Transactional; tránh lỗi "403 che 500"
    // (commit rollback-only) làm mất message lỗi "ghế đã bị đặt".
    @PostMapping("/hold")
    @PreAuthorize("@perm.can('pos_ticketing', 'add')")
    public ResponseEntity<?> createHold(@RequestBody Map<String, Object> body) {
        try {
            StaffSchedule schedule = shiftAccessService.requireCurrentShiftForStaff(List.of("POS_TICKETING", "SHIFT_LEAD"), "ban ve POS");
            Integer showtimeId = Integer.parseInt(body.get("showtimeId").toString());
            @SuppressWarnings("unchecked")
            List<Integer> seatIds = (List<Integer>) body.get("seatIds");
            Integer customerId = body.get("customerId") != null
                    ? Integer.parseInt(body.get("customerId").toString()) : null;

            com.devcine.backend.dto.request.BookingRequestDTO req =
                    com.devcine.backend.dto.request.BookingRequestDTO.builder()
                            .showtimeId(showtimeId)
                            .seatIds(seatIds)
                            .customerId(customerId)
                            .paymentMethod("POS_HOLD")
                            .build();

            Booking booking = bookingService.holdSeatsForStaffSchedule(req, schedule);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "bookingId", booking.getId(),
                    "bookingCode", booking.getBookingCode()
            ));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Nhả ghế của đơn chờ (huỷ đơn / hết giờ) → ghế về AVAILABLE ngay theo thời gian thực
    @PostMapping("/hold/{bookingId}/release")
    @PreAuthorize("@perm.can('pos_ticketing', 'add')")
    public ResponseEntity<?> releaseHold(@PathVariable Integer bookingId) {
        try {
            shiftAccessService.requireCurrentShiftForStaff(List.of("POS_TICKETING", "SHIFT_LEAD"), "ban ve POS");
            String status = posHoldService.releaseHold(bookingId);
            return ResponseEntity.ok(Map.of("success", true, "status", status));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
