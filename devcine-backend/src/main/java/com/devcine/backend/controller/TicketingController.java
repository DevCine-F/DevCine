package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import com.devcine.backend.service.BookingService;
import com.devcine.backend.service.ConcessionService;
import com.devcine.backend.service.PosHoldService;
import com.devcine.backend.service.SystemSettingService;
import com.devcine.backend.service.VoucherService;
import com.devcine.backend.util.SecurityUtils;
import com.devcine.backend.dto.request.SeatSelectionDTO;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.devcine.backend.dto.request.FnbSelectionDTO;
import com.devcine.backend.dto.request.PosCheckoutRequestDTO;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private final StaffRepository staffRepository;
    private final VoucherService voucherService;
    private final SystemSettingService systemSettingService;

    /** Nhân viên (Staff) đang đăng nhập, hoặc null nếu là ADMIN không phải nhân sự quầy. */
    private Staff currentStaffOrNull() {
        Integer uid = SecurityUtils.getCurrentUserId();
        return uid == null ? null : staffRepository.findById(uid).orElse(null);
    }

    /** Cơ sở (rạp) của một suất chiếu: Showtime → Room → Cinema. */
    private Integer cinemaIdOfShowtime(Showtime s) {
        return s != null && s.getRoom() != null && s.getRoom().getCinema() != null
                ? s.getRoom().getCinema().getId() : null;
    }

    // Suất chiếu cho POS: chỉ lấy các suất chưa quá 10 phút sau giờ bắt đầu, sắp xếp tăng dần
    @GetMapping("/showtimes")
    @PreAuthorize("@perm.can('pos_ticketing', 'view')")
    public ResponseEntity<?> getTodayShowtimes() {
        // Cách ly cụm rạp: nhân viên/quản lý chỉ thấy suất của cơ sở mình; ADMIN thấy toàn hệ thống.
        Integer myCinemaId = SecurityUtils.getCurrentUserCinemaId();
        boolean isAdmin = SecurityUtils.isAdmin();
        int lateMinutes = systemSettingService.getBookingLateMinutes();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(lateMinutes);
        List<Showtime> showtimes = showtimeRepository.findPOSShowtimesWithDetails(cutoff).stream()
                .filter(s -> isAdmin || (myCinemaId != null && myCinemaId.equals(cinemaIdOfShowtime(s))))
                .filter(s -> s.getStatus() == null || !"Cancelled".equalsIgnoreCase(s.getStatus()))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = showtimes.stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("movieId", s.getMovie() != null ? s.getMovie().getId() : null);
            map.put("movieTitle", s.getMovie() != null && s.getMovie().getTitle() != null ? s.getMovie().getTitle() : "Phim");
            map.put("movieTitleVietnamese", s.getMovie() != null ? s.getMovie().getTitleVietnamese() : null);
            map.put("moviePoster", s.getMovie() != null && s.getMovie().getPosterUrl() != null ? s.getMovie().getPosterUrl() : "");
            map.put("durationMins", s.getMovie() != null && s.getMovie().getDurationMins() != null ? s.getMovie().getDurationMins() : 120);
            map.put("ageRating", s.getMovie() != null && s.getMovie().getAgeRating() != null ? s.getMovie().getAgeRating() : "P");
            map.put("startTime", s.getStartTime() != null ? s.getStartTime().toString() : "");
            map.put("endTime", s.getEndTime() != null ? s.getEndTime().toString() : "");
            map.put("roomId", s.getRoom() != null ? s.getRoom().getId() : null);
            map.put("roomName", s.getRoom() != null && s.getRoom().getName() != null ? s.getRoom().getName() : "");
            map.put("formatId", s.getFormat() != null ? s.getFormat().getId() : null);
            map.put("formatName", s.getFormat() != null && s.getFormat().getName() != null ? s.getFormat().getName() : "2D");
            map.put("status", s.getStatus() != null ? s.getStatus() : "SCHEDULED");
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Lấy danh sách F&B combo cho POS
    @GetMapping("/combos")
    @PreAuthorize("@perm.can('pos_ticketing', 'view')")
    public ResponseEntity<?> getFnbCombos() {
        List<FnbItem> items = fnbItemRepository.findByIsActiveTrueAndIsDeletedFalseOrderByTypeAscNameAsc();
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    // Tra cứu khách hàng theo SỐ ĐIỆN THOẠI để tích điểm tại quầy
    @GetMapping("/member-card/{phone}")
    @PreAuthorize("@perm.can('pos_ticketing', 'view')")
    public ResponseEntity<?> lookupMemberCard(@PathVariable String phone) {
        try {
            String p = com.devcine.backend.util.PhoneUtils.sanitize(phone);
            if (p == null || p.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Vui lòng nhập số điện thoại"));
            }
            Customer customer = customerRepository.findFirstByUserPhone(p)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với số điện thoại này"));
            if (customer.getUser() != null && !Boolean.TRUE.equals(customer.getUser().getIsActive())) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Tài khoản khách hàng này đã bị tạm khóa. Không thể tích hoặc sử dụng điểm tại quầy."));
            }
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
            Staff soldBy = currentStaffOrNull();
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
                            .unitPrice(m.get("unitPrice") != null ? new java.math.BigDecimal(m.get("unitPrice").toString()) : null)
                            .build())
                    .collect(Collectors.toList());

            // F&B / combo kèm theo (nếu có)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fnbsRaw = (List<Map<String, Object>>) body.get("fnbs");
            List<FnbSelectionDTO> fnbs = fnbsRaw == null ? List.of() : fnbsRaw.stream()
                    .map(m -> {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> optionsRaw = (List<Map<String, Object>>) m.get("options");
                        List<com.devcine.backend.dto.request.FnbOptionSelectionDTO> options = optionsRaw == null ? List.of() : optionsRaw.stream()
                                .map(opt -> com.devcine.backend.dto.request.FnbOptionSelectionDTO.builder()
                                        .slotId(opt.get("slotId") != null ? Integer.parseInt(opt.get("slotId").toString()) : null)
                                        .optionGroupId(opt.get("optionGroupId") != null ? Integer.parseInt(opt.get("optionGroupId").toString()) : null)
                                        .optionItemId(opt.get("optionItemId") != null ? Integer.parseInt(opt.get("optionItemId").toString()) : null)
                                        .build())
                                .collect(Collectors.toList());
                        return FnbSelectionDTO.builder()
                                .fnbItemId(Integer.parseInt(m.get("fnbItemId").toString()))
                                .quantity(Integer.parseInt(m.get("quantity").toString()))
                                .options(options)
                                .build();
                    })
                    .collect(Collectors.toList());

            // Voucher: nhận voucherId trực tiếp, hoặc voucherCode -> tự lưu/áp cho khách (cần customerId)
            Integer voucherId = body.get("voucherId") != null
                    ? Integer.parseInt(body.get("voucherId").toString()) : null;
            String voucherCode = body.get("voucherCode") != null ? body.get("voucherCode").toString().trim() : null;
            if (voucherId == null && voucherCode != null && !voucherCode.isBlank() && customerId != null) {
                voucherId = voucherService.getOrClaimForCheckout(customerId, voucherCode).getId();
            }

            Integer heldBookingId = body.get("heldBookingId") != null
                    ? Integer.parseInt(body.get("heldBookingId").toString()) : null;

            com.devcine.backend.dto.request.BookingRequestDTO req =
                    com.devcine.backend.dto.request.BookingRequestDTO.builder()
                            .showtimeId(showtimeId)
                            .seatIds(seatIds)
                            .seatSelections(seatSelections)
                            .fnbs(fnbs)
                            .customerId(customerId)
                            .voucherId(voucherId)
                            .paymentMethod(paymentMethod)
                            .heldBookingId(heldBookingId)
                            // POS override "Cho phép lẻ ghế" (chỉ hiệu lực ở kênh POS đã qua quyền pos_ticketing)
                            .allowOrphan(Boolean.parseBoolean(String.valueOf(body.getOrDefault("allowOrphan", "false"))))
                            .build();

            Booking booking = bookingService.holdSeatsForStaff(req, soldBy);

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
                                "seatLabel", seat.displayLabel(),
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
            if (e.getMessage() != null && e.getMessage().contains("Out-of-Stock")) {
                return ResponseEntity.status(422).body(ApiResponse.fail(e.getMessage()));
            }
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
    public ResponseEntity<?> concessionCheckout(@Valid @RequestBody PosCheckoutRequestDTO body) {
        try {
            // Cách ly cụm rạp: đơn F&B thuần được gán cố định cinema = cơ sở của nhân viên bán.
            Staff soldBy = currentStaffOrNull();
            Cinema cinema = soldBy != null ? soldBy.getCinema() : null;
            SecurityUtils.assertCinemaAccess(cinema != null ? cinema.getId() : null);

            String paymentMethod = body.getPaymentMethod() != null ? body.getPaymentMethod() : "CASH";
            Integer customerId = body.getCustomerId();

            List<FnbSelectionDTO> fnbs = body.getFnbs() == null ? List.of() : body.getFnbs().stream()
                    .map(m -> FnbSelectionDTO.builder()
                            .fnbItemId(m.getItemId())
                            .quantity(m.getQuantity())
                            .options(m.getOptions())
                            .build())
                    .collect(Collectors.toList());

            ConcessionSale sale = concessionService.createSale(fnbs, customerId, paymentMethod, soldBy, cinema);

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
            Staff soldBy = currentStaffOrNull();
            Integer showtimeId = Integer.parseInt(body.get("showtimeId").toString());
            @SuppressWarnings("unchecked")
            List<Integer> seatIds = (List<Integer>) body.get("seatIds");
            String paymentMethod = (String) body.getOrDefault("paymentMethod", "TRANSFER");
            Integer customerId = body.get("customerId") != null
                    ? Integer.parseInt(body.get("customerId").toString()) : null;

            // Loại vé/đối tượng theo từng ghế
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> selRaw = (List<Map<String, Object>>) body.get("seatSelections");
            List<SeatSelectionDTO> seatSelections = selRaw == null ? null : selRaw.stream()
                    .map(m -> SeatSelectionDTO.builder()
                            .seatId(Integer.parseInt(m.get("seatId").toString()))
                            .ticketType(m.get("ticketType") != null ? m.get("ticketType").toString() : "ADULT")
                            .unitPrice(m.get("unitPrice") != null ? new java.math.BigDecimal(m.get("unitPrice").toString()) : null)
                            .build())
                    .collect(Collectors.toList());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fnbsRaw = (List<Map<String, Object>>) body.get("fnbs");
            List<FnbSelectionDTO> fnbs = fnbsRaw == null ? List.of() : fnbsRaw.stream()
                    .map(m -> {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> optionsRaw = (List<Map<String, Object>>) m.get("options");
                        List<com.devcine.backend.dto.request.FnbOptionSelectionDTO> options = optionsRaw == null ? List.of() : optionsRaw.stream()
                                .map(opt -> com.devcine.backend.dto.request.FnbOptionSelectionDTO.builder()
                                        .slotId(opt.get("slotId") != null ? Integer.parseInt(opt.get("slotId").toString()) : null)
                                        .optionGroupId(opt.get("optionGroupId") != null ? Integer.parseInt(opt.get("optionGroupId").toString()) : null)
                                        .optionItemId(opt.get("optionItemId") != null ? Integer.parseInt(opt.get("optionItemId").toString()) : null)
                                        .build())
                                .collect(Collectors.toList());
                        return FnbSelectionDTO.builder()
                                .fnbItemId(Integer.parseInt(m.get("fnbItemId").toString()))
                                .quantity(Integer.parseInt(m.get("quantity").toString()))
                                .options(options)
                                .build();
                    })
                    .collect(Collectors.toList());

            // Voucher: nhận voucherId trực tiếp, hoặc voucherCode -> tự lưu/áp cho khách
            Integer voucherId = body.get("voucherId") != null
                    ? Integer.parseInt(body.get("voucherId").toString()) : null;
            String voucherCode = body.get("voucherCode") != null ? body.get("voucherCode").toString().trim() : null;
            if (voucherId == null && voucherCode != null && !voucherCode.isBlank() && customerId != null) {
                voucherId = voucherService.getOrClaimForCheckout(customerId, voucherCode).getId();
            }

            Integer heldBookingId = body.get("heldBookingId") != null
                    ? Integer.parseInt(body.get("heldBookingId").toString()) : null;

            com.devcine.backend.dto.request.BookingRequestDTO req =
                    com.devcine.backend.dto.request.BookingRequestDTO.builder()
                            .showtimeId(showtimeId)
                            .seatIds(seatIds)
                            .seatSelections(seatSelections)
                            .fnbs(fnbs)
                            .customerId(customerId)
                            .voucherId(voucherId)
                            .paymentMethod(paymentMethod)
                            .heldBookingId(heldBookingId)
                            .allowOrphan(Boolean.parseBoolean(String.valueOf(body.getOrDefault("allowOrphan", "false"))))
                            .build();

            Booking booking = bookingService.holdSeatsForStaff(req, soldBy);

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
            // Cách ly cụm rạp: chỉ được nhả ghế của đơn thuộc cơ sở mình.
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn giữ chỗ."));
            SecurityUtils.assertCinemaAccess(cinemaIdOfShowtime(booking.getShowtime()));
            String status = posHoldService.releaseHold(bookingId);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("status", status)));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
