package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import com.devcine.backend.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final BookingRepository bookingRepository;
    private final ConcessionSaleRepository concessionSaleRepository;
    private final VoucherRepository voucherRepository;
    private final PasswordResetService passwordResetService;
    private final com.devcine.backend.service.PosHoldService posHoldService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    // ===== Security helpers =====

    /**
     * Lấy cinemaId của người đang đăng nhập từ JWT (đã được JwtFilter inject vào Details).
     * ADMIN không mang cinemaId → trả null.
     */
    private Integer currentCinemaId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (auth.getDetails() instanceof Map<?, ?> details) {
            Object cid = details.get("cinemaId");
            return cid instanceof Integer i ? i : null;
        }
        return null;
    }

    private Integer currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof Integer i) return i;
        if (auth.getPrincipal() instanceof String s) {
            try { return Integer.parseInt(s); } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean isSelf(Integer customerId) {
        if (customerId == null) return false;
        Integer currentId = currentUserId();
        return currentId != null && currentId.equals(customerId);
    }

    /**
     * Kiểm tra người đang đăng nhập có role ADMIN hệ thống hay không.
     * ADMIN thấy toàn bộ dữ liệu, không bị scope theo cinema.
     */
    private boolean isSystemAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
    }

    /**
     * Kiểm tra quyền truy cập vào profile của 1 khách hàng cụ thể.
     * - Chính khách hàng (self-access): luôn được phép xem/sửa thông tin của mình.
     * - ADMIN: luôn được phép.
     * - Manager/Staff: chỉ được xem khách đã từng giao dịch tại cinema của mình
     *   (Booking CONFIRMED hoặc ConcessionSale COMPLETED).
     *
     * @return true nếu có quyền truy cập; false nếu bị cấm.
     */
    private boolean hasAccessToCustomer(Integer customerId) {
        if (isSelf(customerId)) return true;
        if (isSystemAdmin()) return true;
        Integer cinemaId = currentCinemaId();
        if (cinemaId == null) return false; // không xác định cinema → từ chối an toàn
        return customerRepository.existsBookingByCinemaAndCustomer(cinemaId, customerId)
                || concessionSaleRepository.existsCompletedByCinemaAndCustomer(cinemaId, customerId);
    }

    // ===== Endpoints =====

    /**
     * Danh sách khách hàng cho khu vực quản trị.
     * - ADMIN: toàn bộ hệ thống (kể cả khách mới đăng ký chưa mua vé).
     * - Manager/Staff cụm rạp: CHỈ khách đã từng giao dịch tại cinema của mình.
     * Tối ưu O(1) batch query tính tổng chi tiêu (totalSpent) và tổng số đơn (orderCount).
     */
    @GetMapping
    @PreAuthorize("@perm.can('customers', 'view')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listCustomers(@RequestParam(required = false) String q) {
        try {
            String cleanQ = q != null ? q.trim() : "";
            if (cleanQ.toUpperCase().startsWith("#DC-")) cleanQ = cleanQ.substring(4).trim();
            else if (cleanQ.toUpperCase().startsWith("DC-")) cleanQ = cleanQ.substring(3).trim();
            else if (cleanQ.startsWith("#")) cleanQ = cleanQ.substring(1).trim();

            Integer cinemaId = currentCinemaId();
            boolean isAdmin = isSystemAdmin();

            List<Customer> customers;
            if (isAdmin || cinemaId == null) {
                // ADMIN: toàn hệ thống, kể cả khách mới đăng ký chưa mua vé
                customers = !cleanQ.isBlank()
                        ? customerRepository.searchWithUser(cleanQ)
                        : customerRepository.findAllWithUser();
            } else {
                // Manager / Staff: chỉ khách đã giao dịch tại cinema của mình
                customers = !cleanQ.isBlank()
                        ? customerRepository.searchByCinemaScope(cleanQ, cinemaId)
                        : customerRepository.findByCinemaScope(cinemaId);
            }

            List<Integer> customerIds = customers.stream().map(Customer::getUserId).collect(Collectors.toList());

            Map<Integer, BigDecimal> spentMap = new HashMap<>();
            Map<Integer, Long> countMap = new HashMap<>();

            if (!customerIds.isEmpty()) {
                // 1. Batch aggregate từ Booking (CONFIRMED)
                for (Object[] row : bookingRepository.aggregateSpentAndOrderCountByCustomerIds(customerIds)) {
                    Integer cid = (Integer) row[0];
                    BigDecimal spent = (BigDecimal) row[1];
                    Long cnt = ((Number) row[2]).longValue();
                    spentMap.put(cid, spent != null ? spent : BigDecimal.ZERO);
                    countMap.put(cid, cnt != null ? cnt : 0L);
                }
                // 2. Batch aggregate từ ConcessionSale (COMPLETED)
                for (Object[] row : concessionSaleRepository.aggregateConcessionSpentAndCountByCustomerIds(customerIds)) {
                    Integer cid = (Integer) row[0];
                    BigDecimal spent = (BigDecimal) row[1];
                    Long cnt = ((Number) row[2]).longValue();
                    spentMap.merge(cid, spent != null ? spent : BigDecimal.ZERO, BigDecimal::add);
                    countMap.merge(cid, cnt != null ? cnt : 0L, Long::sum);
                }
            }

            List<Map<String, Object>> result = customers.stream()
                    .map(c -> buildProfileResponse(c, spentMap.getOrDefault(c.getUserId(), BigDecimal.ZERO), countMap.getOrDefault(c.getUserId(), 0L)))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách khách hàng: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.fail("Không thể tải danh sách khách hàng: " + e.getMessage()));
        }
    }

    /** Chi tiết khách hàng kèm tổng chi tiêu & đơn hàng. */
    @GetMapping("/{id}")
    @PreAuthorize("@perm.can('customers', 'view') or @perm.isSelf(#id)")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerProfile(@PathVariable Integer id) {
        try {
            Customer customer = customerRepository.findById(id).orElse(null);
            if (customer == null) {
                return ResponseEntity.notFound().build();
            }

            // Guard: Manager/Staff chỉ được xem khách thuộc cinema mình
            if (!hasAccessToCustomer(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Khách hàng này không thuộc phạm vi quản lý của cụm rạp bạn."));
            }

            BigDecimal totalSpent = BigDecimal.ZERO;
            long orderCount = 0;

            List<Object[]> bookingAgg = bookingRepository.aggregateSpentAndOrderCountByCustomerIds(List.of(id));
            if (!bookingAgg.isEmpty()) {
                BigDecimal sp = (BigDecimal) bookingAgg.get(0)[1];
                Long cnt = ((Number) bookingAgg.get(0)[2]).longValue();
                if (sp != null) totalSpent = totalSpent.add(sp);
                if (cnt != null) orderCount += cnt;
            }

            List<Object[]> concessionAgg = concessionSaleRepository.aggregateConcessionSpentAndCountByCustomerIds(List.of(id));
            if (!concessionAgg.isEmpty()) {
                BigDecimal sp = (BigDecimal) concessionAgg.get(0)[1];
                Long cnt = ((Number) concessionAgg.get(0)[2]).longValue();
                if (sp != null) totalSpent = totalSpent.add(sp);
                if (cnt != null) orderCount += cnt;
            }

            return ResponseEntity.ok(ApiResponse.ok(buildProfileResponse(customer, totalSpent, orderCount)));
        } catch (Exception e) {
            log.error("Lỗi khi tải chi tiết khách hàng ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.fail("Không thể tải chi tiết khách hàng: " + e.getMessage()));
        }
    }

    /**
     * Lịch sử đặt vé & bắp nước của khách hàng (Tab 2).
     * Kết hợp cả đơn Booking (vé/combo) và ConcessionSale (bán nhanh tại quầy).
     * Hiển thị toàn bộ đơn toàn chuỗi (không scope theo cinema) — staff thấy đầy đủ
     * context khi hỗ trợ khách, cột cinemaName phân biệt nguồn gốc từng đơn.
     */
    @GetMapping("/{id}/orders")
    @PreAuthorize("@perm.can('customers', 'view') or @perm.isSelf(#id)")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerOrders(@PathVariable Integer id) {
        try {
            // Guard: Manager/Staff chỉ được xem khách thuộc cinema mình
            if (!hasAccessToCustomer(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Khách hàng này không thuộc phạm vi quản lý của cụm rạp bạn."));
            }

            List<Map<String, Object>> orders = new ArrayList<>();

            // 1. Đơn vé (Bookings)
            List<Booking> bookings = bookingRepository.findByCustomerIdWithDetails(id);
            if (bookings != null && !bookings.isEmpty()) {
                List<Integer> bookingIds = bookings.stream().map(Booking::getId).collect(Collectors.toList());
                List<BookingSeat> allSeats = bookingRepository.findAllSeatsByBookingIds(bookingIds);
                Map<Integer, List<String>> seatsByBooking = new HashMap<>();
                if (allSeats != null) {
                    for (BookingSeat bs : allSeats) {
                        if (bs.getBooking() != null && bs.getSeat() != null) {
                            seatsByBooking.computeIfAbsent(bs.getBooking().getId(), k -> new ArrayList<>())
                                    .add(bs.getSeat().displayLabel());
                        }
                    }
                }

                Integer currentCinemaId = currentCinemaId();
                for (Booking b : bookings) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", b.getId());
                    m.put("orderCode", b.getBookingCode() != null ? b.getBookingCode() : "#BK-" + b.getId());
                    m.put("orderType", "TICKET");
                    m.put("title", b.getShowtime() != null && b.getShowtime().getMovie() != null ? b.getShowtime().getMovie().getTitle() : "Vé xem phim");
                    m.put("showtimeStart", b.getShowtime() != null && b.getShowtime().getStartTime() != null ? b.getShowtime().getStartTime().toString() : null);
                    m.put("roomName", b.getShowtime() != null && b.getShowtime().getRoom() != null ? b.getShowtime().getRoom().getName() : "");
                    Integer bookingCinemaId = (b.getShowtime() != null && b.getShowtime().getRoom() != null
                            && b.getShowtime().getRoom().getCinema() != null)
                            ? b.getShowtime().getRoom().getCinema().getId() : null;
                    m.put("cinemaName", b.getShowtime() != null && b.getShowtime().getRoom() != null && b.getShowtime().getRoom().getCinema() != null
                            ? b.getShowtime().getRoom().getCinema().getName() : "DevCine");
                    // Đánh dấu đơn thuộc cinema khác — FE có thể dùng để render read-only
                    m.put("isOtherCinema", currentCinemaId != null && bookingCinemaId != null && !currentCinemaId.equals(bookingCinemaId));
                    List<String> seatList = seatsByBooking.getOrDefault(b.getId(), List.of());
                    m.put("seatCount", seatList.size());
                    m.put("seats", seatList.isEmpty() ? "—" : String.join(", ", seatList));
                    m.put("totalPrice", b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO);
                    m.put("discountAmount", b.getDiscountAmount() != null ? b.getDiscountAmount() : BigDecimal.ZERO);
                    m.put("finalPrice", b.getFinalPrice() != null ? b.getFinalPrice() : BigDecimal.ZERO);
                    m.put("paymentMethod", b.getPaymentMethod() != null ? b.getPaymentMethod() : "CASH");
                    m.put("status", b.getStatus() != null ? b.getStatus() : "CONFIRMED");
                    m.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
                    m.put("createdAtRaw", b.getCreatedAt());
                    orders.add(m);
                }
            }

            // 2. Đơn bắp nước tại quầy (ConcessionSale)
            List<ConcessionSale> concessions = concessionSaleRepository.findByCustomerIdOrderByCreatedAtDesc(id);
            if (concessions != null) {
                Integer currentCinemaId = currentCinemaId();
                for (ConcessionSale s : concessions) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", s.getId());
                    m.put("orderCode", s.getSaleCode() != null ? s.getSaleCode() : "#CS-" + s.getId());
                    m.put("orderType", "CONCESSION");
                    m.put("title", "Bán nhanh bắp nước (F&B)");
                    m.put("showtimeStart", null);
                    m.put("roomName", "Quầy Concession");
                    Integer saleCinemaId = s.getCinema() != null ? s.getCinema().getId() : null;
                    m.put("cinemaName", s.getCinema() != null ? s.getCinema().getName() : "DevCine");
                    // Đánh dấu đơn thuộc cinema khác — FE có thể dùng để render read-only
                    m.put("isOtherCinema", currentCinemaId != null && saleCinemaId != null && !currentCinemaId.equals(saleCinemaId));
                    m.put("seatCount", 0);
                    m.put("seats", "—");
                    m.put("totalPrice", s.getTotalPrice() != null ? s.getTotalPrice() : BigDecimal.ZERO);
                    m.put("discountAmount", BigDecimal.ZERO);
                    m.put("finalPrice", s.getTotalPrice() != null ? s.getTotalPrice() : BigDecimal.ZERO);
                    m.put("paymentMethod", s.getPaymentMethod() != null ? s.getPaymentMethod() : "CASH");
                    m.put("status", s.getStatus() != null ? s.getStatus() : "COMPLETED");
                    m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
                    m.put("createdAtRaw", s.getCreatedAt());
                    orders.add(m);
                }
            }

            // Sắp xếp đơn hàng mới nhất trước
            orders.sort((a, b) -> {
                LocalDateTime ta = (LocalDateTime) a.get("createdAtRaw");
                LocalDateTime tb = (LocalDateTime) b.get("createdAtRaw");
                if (ta == null && tb == null) return 0;
                if (ta == null) return 1;
                if (tb == null) return -1;
                return tb.compareTo(ta);
            });

            return ResponseEntity.ok(ApiResponse.ok(orders));
        } catch (Exception e) {
            log.error("Lỗi khi tải đơn hàng của khách hàng ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.fail("Không thể tải lịch sử đơn hàng: " + e.getMessage()));
        }
    }

    /** Danh sách Voucher của khách hàng (Tab 3). */
    @GetMapping("/{id}/vouchers")
    @PreAuthorize("@perm.can('customers', 'view') or @perm.isSelf(#id)")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerVouchers(@PathVariable Integer id) {
        try {
            // Guard: Manager/Staff chỉ được xem khách thuộc cinema mình
            if (!hasAccessToCustomer(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Khách hàng này không thuộc phạm vi quản lý của cụm rạp bạn."));
            }

            LocalDateTime now = LocalDateTime.now();
            List<Voucher> vouchers = voucherRepository.findAllByCustomerIdWithPromotion(id);

            List<Map<String, Object>> result = new ArrayList<>();
            if (vouchers != null) {
                for (Voucher v : vouchers) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", v.getId());
                    m.put("code", v.getPromotion() != null && v.getPromotion().getCode() != null ? v.getPromotion().getCode() : "VOUCHER");
                    m.put("title", v.getPromotion() != null && v.getPromotion().getName() != null ? v.getPromotion().getName() : "Ưu đãi thành viên");
                    try {
                        m.put("discountType", v.effectiveDiscountType());
                        m.put("discountValue", v.effectiveDiscountValue());
                        m.put("maxDiscountAmount", v.effectiveMaxDiscountAmount());
                        m.put("minOrderValue", v.effectiveMinOrderValue());
                    } catch (Exception ignored) {
                        m.put("discountType", "PERCENTAGE");
                        m.put("discountValue", BigDecimal.ZERO);
                    }
                    m.put("validUntil", v.getValidUntil() != null ? v.getValidUntil().toString() : null);
                    m.put("isUsed", Boolean.TRUE.equals(v.getIsUsed()));
                    m.put("usedAt", v.getUsedAt() != null ? v.getUsedAt().toString() : null);

                    String status = "ACTIVE";
                    if (Boolean.TRUE.equals(v.getIsUsed())) {
                        status = "USED";
                    } else if (v.getValidUntil() != null && v.getValidUntil().isBefore(now)) {
                        status = "EXPIRED";
                    }
                    m.put("status", status);
                    result.add(m);
                }
            }

            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            log.error("Lỗi khi tải voucher của khách hàng ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.fail("Không thể tải danh sách voucher: " + e.getMessage()));
        }
    }

    /** Lịch sử biến động điểm của khách hàng (Tab 3). */
    @GetMapping("/{id}/point-history")
    @PreAuthorize("@perm.can('customers', 'view') or @perm.isSelf(#id)")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPointHistory(@PathVariable Integer id) {
        try {
            // Guard: Manager/Staff chỉ được xem khách thuộc cinema mình
            if (!hasAccessToCustomer(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Khách hàng này không thuộc phạm vi quản lý của cụm rạp bạn."));
            }

            List<PointTransaction> txs = pointTransactionRepository.findByCustomer_UserIdOrderByCreatedAtDescIdDesc(id);
            List<Map<String, Object>> result = new ArrayList<>();
            if (txs != null) {
                for (PointTransaction t : txs) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("points", t.getPoints());
                    m.put("type", t.getType());
                    m.put("source", t.getSource());
                    m.put("refCode", t.getRefCode());
                    m.put("balanceAfter", t.getBalanceAfter());
                    m.put("note", t.getNote());
                    m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
                    result.add(m);
                }
            }
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            log.error("Lỗi khi tải lịch sử điểm của khách hàng ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.fail("Không thể tải lịch sử điểm: " + e.getMessage()));
        }
    }

    /**
     * Chỉnh sửa thông tin cơ bản: Chỉ cho phép sửa Họ và tên + Ngày sinh.
     * Các trường Email, SĐT, Điểm, Hạng được bảo vệ chỉ đọc.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('customers', 'edit') or @perm.isSelf(#id)")
    @Transactional
    public ResponseEntity<?> updateCustomerProfile(@PathVariable Integer id,
                                                   @RequestBody Map<String, Object> body) {
        try {
            Customer customer = customerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

            // Guard: Manager/Staff chỉ được sửa khách thuộc cinema mình
            if (!hasAccessToCustomer(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Khách hàng này không thuộc phạm vi quản lý của cụm rạp bạn."));
            }

            if (customer.getUser() != null) {
                var user = customer.getUser();
                if (body.containsKey("fullName")) {
                    Object nameVal = body.get("fullName");
                    if (nameVal == null || nameVal.toString().trim().isBlank()) {
                        throw new RuntimeException("Vui lòng nhập họ và tên thành viên.");
                    }
                    String name = nameVal.toString().trim().replaceAll("\\s+", " ");
                    if (name.length() < 2 || name.length() > 50) {
                        throw new RuntimeException("Họ và tên phải từ 2 đến 50 ký tự.");
                    }
                    if (!name.matches("^[\\p{L} ]+$")) {
                        throw new RuntimeException("Họ và tên chỉ được chứa chữ cái và khoảng trắng.");
                    }
                    // Chuẩn hóa Title Case
                    String[] words = name.toLowerCase().split(" ");
                    StringBuilder formattedName = new StringBuilder();
                    for (String w : words) {
                        if (!w.isEmpty()) {
                            formattedName.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
                        }
                    }
                    user.setFullName(formattedName.toString().trim());
                }
                if (body.containsKey("email") && body.get("email") != null) {
                    String email = body.get("email").toString().trim();
                    if (!email.isBlank() && !email.equalsIgnoreCase(user.getEmail())) {
                        if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
                            throw new RuntimeException("Email " + email + " đã được sử dụng bởi một tài khoản khác.");
                        }
                        user.setEmail(email);
                    }
                }
                if (body.containsKey("phone")) {
                    Object phoneVal = body.get("phone");
                    String phone = phoneVal != null ? phoneVal.toString().trim() : null;
                    if (phone != null && !phone.isBlank()) {
                        String cleanPhone = com.devcine.backend.util.PhoneUtils.validateAndSanitize(phone, false);
                        if (cleanPhone != null && !cleanPhone.equals(user.getPhone())) {
                            if (userRepository.existsByPhoneAndIdNot(cleanPhone, user.getId())) {
                                throw new RuntimeException("Số điện thoại " + cleanPhone + " đã được sử dụng bởi một tài khoản khác.");
                            }
                            user.setPhone(cleanPhone);
                        }
                    } else {
                        user.setPhone(null);
                    }
                }
                if (body.containsKey("avatarUrl")) {
                    Object avatarVal = body.get("avatarUrl");
                    String avatar = avatarVal != null ? avatarVal.toString().trim() : null;
                    user.setAvatarUrl(avatar != null && !avatar.isBlank() ? avatar : null);
                }
                userRepository.save(user);
            }

            if (body.containsKey("dob")) {
                Object dobVal = body.get("dob");
                if (dobVal != null && !dobVal.toString().isBlank()) {
                    LocalDate parsedDob;
                    try {
                        parsedDob = LocalDate.parse(dobVal.toString().trim());
                    } catch (Exception ex) {
                        throw new RuntimeException("Định dạng ngày sinh không hợp lệ (chuẩn YYYY-MM-DD).");
                    }
                    LocalDate now = LocalDate.now();
                    if (parsedDob.isAfter(now)) {
                        throw new RuntimeException("Ngày sinh không thể lớn hơn ngày hiện tại.");
                    }
                    if (parsedDob.isAfter(now.minusYears(6))) {
                        throw new RuntimeException("Thành viên phải từ đủ 6 tuổi trở lên.");
                    }
                    if (parsedDob.isBefore(now.minusYears(125))) {
                        throw new RuntimeException("Năm sinh không hợp lệ. Vui lòng kiểm tra lại.");
                    }
                    customer.setDob(parsedDob);
                } else {
                    customer.setDob(null);
                }
                customerRepository.save(customer);
            }

            BigDecimal totalSpent = BigDecimal.ZERO;
            long orderCount = 0;

            List<Object[]> bookingAgg = bookingRepository.aggregateSpentAndOrderCountByCustomerIds(List.of(id));
            if (!bookingAgg.isEmpty()) {
                BigDecimal sp = (BigDecimal) bookingAgg.get(0)[1];
                Long cnt = ((Number) bookingAgg.get(0)[2]).longValue();
                if (sp != null) totalSpent = totalSpent.add(sp);
                if (cnt != null) orderCount += cnt;
            }

            List<Object[]> concessionAgg = concessionSaleRepository.aggregateConcessionSpentAndCountByCustomerIds(List.of(id));
            if (!concessionAgg.isEmpty()) {
                BigDecimal sp = (BigDecimal) concessionAgg.get(0)[1];
                Long cnt = ((Number) concessionAgg.get(0)[2]).longValue();
                if (sp != null) totalSpent = totalSpent.add(sp);
                if (cnt != null) orderCount += cnt;
            }

            return ResponseEntity.ok(ApiResponse.ok(buildProfileResponse(customer, totalSpent, orderCount)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * Khóa hoặc Mở khóa tài khoản khách hàng.
     * Khi khóa: bắt buộc kèm lý do (lockReason) và thời điểm (lockedAt).
     */
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("@perm.can('customers', 'edit')")
    @Transactional
    public ResponseEntity<?> toggleCustomerStatus(@PathVariable Integer id,
                                                  @RequestBody(required = false) Map<String, Object> body) {
        try {
            Customer customer = customerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

            // Guard: Manager/Staff chỉ được thao tác khách thuộc cinema mình
            if (!hasAccessToCustomer(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Khách hàng này không thuộc phạm vi quản lý của cụm rạp bạn."));
            }

            User user = customer.getUser();
            if (user == null) {
                throw new RuntimeException("Không tìm thấy thông tin tài khoản người dùng");
            }

            boolean newActive;
            if (body != null && body.containsKey("isActive")) {
                newActive = Boolean.parseBoolean(body.get("isActive").toString());
            } else {
                newActive = !Boolean.TRUE.equals(user.getIsActive());
            }

            if (!newActive) {
                // Khóa tài khoản
                String reason = (body != null && body.get("reason") != null && !body.get("reason").toString().isBlank())
                        ? body.get("reason").toString().trim()
                        : "Tài khoản bị tạm khóa bởi Quản trị viên.";
                customer.setLockReason(reason);
                customer.setLockedAt(LocalDateTime.now());

                // 1. Tự động giải phóng các đơn giữ chỗ (HOLD) đang diễn ra của khách
                try {
                    List<Booking> activeHolds = bookingRepository.findActiveHoldsByCustomerId(id);
                    if (activeHolds != null) {
                        for (Booking hold : activeHolds) {
                            posHoldService.releaseHold(hold.getId(), true);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Lỗi khi tự động giải phóng đơn giữ chỗ của khách hàng bị khóa ID {}: {}", id, ex.getMessage());
                }

                // 2. Phát sự kiện WebSocket Kick tức thì đến kênh riêng của user
                try {
                    String dest = "/topic/customer/" + id + "/status";
                    Object payload = Map.of(
                            "type", "ACCOUNT_LOCKED",
                            "userId", id,
                            "reason", reason,
                            "timestamp", LocalDateTime.now().toString()
                    );
                    messagingTemplate.convertAndSend(dest, payload);
                } catch (Exception ex) {
                    log.warn("Lỗi khi gửi WebSocket kick khách hàng bị khóa ID {}: {}", id, ex.getMessage());
                }
            } else {
                // Mở khóa tài khoản
                customer.setLockReason(null);
                customer.setLockedAt(null);
            }

            user.setIsActive(newActive);
            userRepository.save(user);
            customerRepository.save(customer);

            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "success", true,
                    "isActive", newActive,
                    "lockReason", customer.getLockReason() != null ? customer.getLockReason() : "",
                    "message", newActive ? "Đã mở khóa tài khoản thành công" : "Đã khóa tài khoản thành công"
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * Gửi liên kết / mã đặt lại mật khẩu tới email khách hàng.
     * Chặn tài khoản đang bị khóa.
     */
    @PostMapping("/{id}/send-reset-password")
    @PreAuthorize("@perm.can('customers', 'edit')")
    public ResponseEntity<?> sendResetPassword(@PathVariable Integer id) {
        try {
            Customer customer = customerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

            // Guard: Manager/Staff chỉ được gửi reset password cho khách thuộc cinema mình
            if (!hasAccessToCustomer(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Khách hàng này không thuộc phạm vi quản lý của cụm rạp bạn."));
            }

            User user = customer.getUser();
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                throw new RuntimeException("Khách hàng này chưa cập nhật email để nhận liên kết đặt lại mật khẩu.");
            }

            String email = user.getEmail().trim().toLowerCase();

            if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new RuntimeException("Tài khoản đang trong trạng thái bị khóa. Vui lòng mở khóa tài khoản trước khi gửi yêu cầu.");
            }

            passwordResetService.requestReset(email);

            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "success", true,
                    "message", "Đã gửi mã xác minh đặt lại mật khẩu tới email " + email
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    private Map<String, Object> buildProfileResponse(Customer customer, BigDecimal totalSpent, long orderCount) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", customer.getUserId());
        m.put("fullName", customer.getUser() != null ? customer.getUser().getFullName() : "Khách hàng");
        
        String email = customer.getUser() != null && customer.getUser().getEmail() != null ? customer.getUser().getEmail() : "";
        m.put("email", email);
        m.put("phone", customer.getUser() != null && customer.getUser().getPhone() != null ? customer.getUser().getPhone() : "");
        m.put("avatarUrl", customer.getUser() != null && customer.getUser().getAvatarUrl() != null ? customer.getUser().getAvatarUrl() : "");
        m.put("membershipTier", customer.getMembershipTier() != null ? customer.getMembershipTier() : "BRONZE");
        m.put("loyaltyPoints", customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0);
        m.put("lifetimePoints", customer.getLifetimePoints() != null ? customer.getLifetimePoints() : 0);
        m.put("dob", customer.getDob() != null ? customer.getDob().toString() : "");
        m.put("createdAt", customer.getUser() != null && customer.getUser().getCreatedAt() != null ? customer.getUser().getCreatedAt().toString() : "");
        
        // Trạng thái tài khoản & Khóa (mặc định active = true nếu null)
        boolean active = customer.getUser() != null && !Boolean.FALSE.equals(customer.getUser().getIsActive());
        m.put("isActive", active);
        m.put("lockReason", customer.getLockReason() != null ? customer.getLockReason() : "");
        m.put("lockedAt", customer.getLockedAt() != null ? customer.getLockedAt().toString() : null);

        // Thống kê tài chính
        m.put("totalSpent", totalSpent != null ? totalSpent : BigDecimal.ZERO);
        m.put("orderCount", orderCount);

        return m;
    }
}
