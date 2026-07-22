package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import com.devcine.backend.service.BookingService;
import com.devcine.backend.service.ConcessionService;
import com.devcine.backend.service.PosHoldService;
import com.devcine.backend.service.ShiftAccessService;
import com.devcine.backend.service.VoucherService;
import com.devcine.backend.dto.request.SeatSelectionDTO;
import com.devcine.backend.util.SecurityUtils;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final VoucherService voucherService;
    private final StaffRepository staffRepository;

    /**
     * Nhân viên đang đăng nhập — nguồn quy kết doanh thu POS.
     * Thay cho việc suy ra từ ca làm việc: ADMIN/MANAGER bán quầy không có ca nhưng vẫn
     * phải được ghi nhận là người bán.
     */
    private Staff currentStaff() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return userId != null ? staffRepository.findById(userId).orElse(null) : null;
    }

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
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Lấy danh sách F&B combo cho POS
    @GetMapping("/combos")
    @PreAuthorize("@perm.can('pos_ticketing', 'view')")
    public ResponseEntity<?> getFnbCombos() {
        shiftAccessService.requireCurrentShiftForStaff(List.of("FNB", "SHIFT_LEAD"), "quay F&B");
        List<FnbItem> items = fnbItemRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    // Tra cứu khách hàng theo SỐ ĐIỆN THOẠI để tích điểm tại quầy
    @GetMapping("/member-card/{phone}")
    @PreAuthorize("@perm.can('pos_ticketing', 'view')")
    public ResponseEntity<?> lookupMemberCard(@PathVariable String phone) {
        shiftAccessService.requireCurrentShiftForStaff(List.of("POS_TICKETING", "FNB", "SHIFT_LEAD"), "nghiep vu tai quay");
        try {
            String p = phone == null ? "" : phone.trim().replaceAll("\\s+", "").replaceFirst("^\\+84", "0");
            if (p.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Vui lòng nhập số điện thoại"));
            }
            Customer customer = customerRepository.findByUserPhone(p).stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với số điện thoại này"));
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "customerId", customer.getUserId(),
                    "fullName", customer.getUser() != null ? customer.getUser().getFullName() : "Khách hàng",
                    "phone", customer.getUser() != null && customer.getUser().getPhone() != null ? customer.getUser().getPhone() : "",
                    "membershipTier", customer.getMembershipTier() != null ? customer.getMembershipTier() : "BRONZE",
                    "loyaltyPoints", customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0
            )));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    // Thanh toán tại quầy (POS checkout)
    // Không gắn @Transactional ở controller: holdSeats/completePayment đã tự @Transactional.
    // Nếu bọc tx ở đây, lỗi nghiệp vụ trong service (vd "Mỗi lần đặt tối đa N vé") bị catch rồi
    // commit tx rollback-only → UnexpectedRollbackException đè message thật thành "Lỗi hệ thống nội bộ".
    @PostMapping("/pay")
    @PreAuthorize("@perm.can('pos_ticketing', 'add')")
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

            // Loại vé/đối tượng theo từng ghế (ưu tiên hơn seatIds; BookingService fallback ADULT nếu thiếu)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> selRaw = (List<Map<String, Object>>) body.get("seatSelections");
            List<SeatSelectionDTO> seatSelections = selRaw == null ? null : selRaw.stream()
                    .map(m -> SeatSelectionDTO.builder()
                            .seatId(Integer.parseInt(m.get("seatId").toString()))
                            .ticketType(m.get("ticketType") != null ? m.get("ticketType").toString() : "ADULT")
                            .build())
                    .collect(Collectors.toList());

            // F&B / combo kèm theo (nếu có)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fnbsRaw = (List<Map<String, Object>>) body.get("fnbs");
            List<FnbSelectionDTO> fnbs = fnbsRaw == null ? List.of() : fnbsRaw.stream()
                    .map(m -> FnbSelectionDTO.builder()
                            .fnbItemId(Integer.parseInt(m.get("fnbItemId").toString()))
                            .quantity(Integer.parseInt(m.get("quantity").toString()))
                            .build())
                    .collect(Collectors.toList());

            // Voucher: nhận voucherId trực tiếp, hoặc voucherCode -> tự lưu/áp cho khách (cần customerId)
            Integer voucherId = body.get("voucherId") != null
                    ? Integer.parseInt(body.get("voucherId").toString()) : null;
            String voucherCode = body.get("voucherCode") != null ? body.get("voucherCode").toString().trim() : null;
            if (voucherId == null && voucherCode != null && !voucherCode.isBlank() && customerId != null) {
                voucherId = voucherService.getOrClaimForCheckout(customerId, voucherCode).getId();
            }

            com.devcine.backend.dto.request.BookingRequestDTO req =
                    com.devcine.backend.dto.request.BookingRequestDTO.builder()
                            .showtimeId(showtimeId)
                            .seatIds(seatIds)
                            .seatSelections(seatSelections)
                            .fnbs(fnbs)
                            .customerId(customerId)
                            .voucherId(voucherId)
                            .paymentMethod(paymentMethod)
                            .build();

            Booking booking = bookingService.holdSeatsForStaffSchedule(req, schedule, currentStaff());

            // Số liệu tiền để POS hiển thị đúng giảm giá (voucher đánh dấu USED trong completePayment)
            BigDecimal totalAmount = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal preRoundFinal = booking.getFinalPrice() != null ? booking.getFinalPrice() : totalAmount;
            BigDecimal discountAmount = totalAmount.subtract(preRoundFinal);
            if (discountAmount.signum() < 0) discountAmount = BigDecimal.ZERO;

            // Cash Rounding: khách vãng lai (KHÔNG có thẻ thành viên) trả TIỀN MẶT -> làm tròn gần nhất
            // 1.000đ và ghi đúng số đã thu vào finalPrice để đối soát bàn giao ca khớp tiền thật trong két.
            BigDecimal roundingAmount = BigDecimal.ZERO;
            if ("CASH".equalsIgnoreCase(paymentMethod) && customerId == null) {
                BigDecimal rounded = roundToNearestThousand(preRoundFinal);
                roundingAmount = rounded.subtract(preRoundFinal);
                if (roundingAmount.signum() != 0) {
                    booking.setFinalPrice(rounded);
                    // Lưu ngay giá đã làm tròn (controller không còn bọc tx) để completePayment nạp lại đúng.
                    bookingRepository.save(booking);
                }
            }
            BigDecimal finalAmount = booking.getFinalPrice() != null ? booking.getFinalPrice() : preRoundFinal;

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

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("bookingId", booking.getId());
            result.put("bookingCode", booking.getBookingCode());
            result.put("message", "Thanh toán thành công");
            result.put("tickets", tickets);
            result.put("totalAmount", totalAmount);
            result.put("discountAmount", discountAmount);
            result.put("roundingAmount", roundingAmount);
            result.put("finalAmount", finalAmount);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** Làm tròn số tiền về bội số 1.000đ gần nhất (Cash Rounding, HALF_UP). */
    private static BigDecimal roundToNearestThousand(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.divide(new BigDecimal("1000"), 0, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("1000"));
    }

    // Bán nhanh bắp nước độc lập tại quầy (Concession Only) — không suất chiếu / không ghế.
    // Không bọc @Transactional ở controller (createSale đã tự @Transactional) — tránh bug rollback-only che message.
    @PostMapping("/concession")
    @PreAuthorize("@perm.can('pos_ticketing', 'add')")
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

            ConcessionSale sale = concessionService.createSale(fnbs, customerId, paymentMethod, schedule, currentStaff());

            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "saleId", sale.getId(),
                    "saleCode", sale.getSaleCode(),
                    "message", "Thanh toán thành công"
            )));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
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

            Booking booking = bookingService.holdSeatsForStaffSchedule(req, schedule, currentStaff());

            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "bookingId", booking.getId(),
                    "bookingCode", booking.getBookingCode()
            )));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    // Nhả ghế của đơn chờ (huỷ đơn / hết giờ) → ghế về AVAILABLE ngay theo thời gian thực
    @PostMapping("/hold/{bookingId}/release")
    @PreAuthorize("@perm.can('pos_ticketing', 'add')")
    public ResponseEntity<?> releaseHold(@PathVariable Integer bookingId) {
        try {
            shiftAccessService.requireCurrentShiftForStaff(List.of("POS_TICKETING", "SHIFT_LEAD"), "ban ve POS");
            String status = posHoldService.releaseHold(bookingId);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("status", status)));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
