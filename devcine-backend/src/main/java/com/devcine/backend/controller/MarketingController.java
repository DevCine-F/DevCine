package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.Promotion;
import com.devcine.backend.entity.Voucher;
import com.devcine.backend.repository.PromotionRepository;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/marketing")
@RequiredArgsConstructor
@Slf4j
public class MarketingController {

    private final PromotionRepository promotionRepository;
    private final VoucherRepository voucherRepository;
    private final CustomerRepository customerRepository;
    private final com.devcine.backend.repository.BookingRepository bookingRepository;
    private final com.devcine.backend.service.LoyaltyService loyaltyService;
    private final com.devcine.backend.service.VoucherService voucherService;
    private final com.devcine.backend.service.SystemSettingService systemSettingService;
    private final SimpMessagingTemplate messagingTemplate;

    private void notifyVoucherUpdate(String action) {
        try {
            if (messagingTemplate != null) {
                Object payload = Map.of(
                        "action", action,
                        "timestamp", System.currentTimeMillis()
                );
                messagingTemplate.convertAndSend("/topic/voucher-updates", payload);
            }
        } catch (Exception e) {
            // best-effort notification
        }
    }

    @GetMapping("/promotions")
    public ResponseEntity<?> getAllPromotions() {
        return ResponseEntity.ok(ApiResponse.ok(promotionRepository.findAll()));
    }

    /**
     * Danh sách khuyến mãi công khai cho trang Khuyến mãi phía khách kèm đầy đủ thông số
     * snapshot (phim áp dụng, đơn tối thiểu, trần giảm, số vé, đối tượng và lý do không đủ điều kiện).
     */
    @GetMapping("/promotions/active")
    public ResponseEntity<?> getActivePromotions(@RequestParam(required = false) Integer customerId) {
        LocalDateTime now = LocalDateTime.now();
        Integer resolvedCustomerId = customerId != null ? customerId : com.devcine.backend.util.SecurityUtils.getCurrentUserId();
        com.devcine.backend.entity.Customer customer = resolvedCustomerId != null
                ? customerRepository.findById(resolvedCustomerId).orElse(null)
                : null;

        List<Map<String, Object>> result = promotionRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")).stream()
                .filter(p -> p.getCode() != null && !p.getCode().isBlank())
                .filter(p -> !Boolean.FALSE.equals(p.getIsActive()))
                .filter(p -> !Boolean.TRUE.equals(p.getIsHidden())) // Voucher kín: không công khai trên trang khuyến mãi
                .filter(p -> (p.getStartDate() == null || !p.getStartDate().isAfter(now))
                        && (p.getEndDate() == null || !p.getEndDate().isBefore(now)))
                .map(p -> {
                    String movieTitles = voucherService.getMovieTitles(p);
                    Integer movieId = p.getApplicableMovieId();
                    if (movieTitles == null && movieId != null) {
                        movieTitles = voucherService.getMovieTitleById(movieId);
                    }
                    boolean exhausted = p.getUsageLimit() != null && p.getUsageLimit() > 0
                            && p.getUsedCount() != null && p.getUsedCount() >= p.getUsageLimit();

                    String ineligibilityReason = null;
                    if (resolvedCustomerId != null) {
                        ineligibilityReason = voucherService.eligibilityReason(resolvedCustomerId, customer, p.getCustomerEligibility());
                    }

                    boolean saved = resolvedCustomerId != null && voucherRepository.existsByCustomerAndPromotion(resolvedCustomerId, p.getId());

                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", p.getId());
                    m.put("code", p.getCode() != null ? p.getCode() : "");
                    m.put("name", p.getName() != null ? p.getName() : "");
                    m.put("description", p.getDescription() != null ? p.getDescription() : "");
                    m.put("discountType", p.getDiscountType() != null ? p.getDiscountType() : "");
                    m.put("discountValue", p.getDiscountValue() != null ? p.getDiscountValue() : 0);
                    m.put("minOrderValue", p.getMinOrderValue() != null ? p.getMinOrderValue() : BigDecimal.ZERO);
                    m.put("maxDiscountAmount", p.getMaxDiscountAmount() != null ? p.getMaxDiscountAmount() : BigDecimal.ZERO);
                    m.put("maxTicketQuantity", p.getMaxTicketQuantity() != null ? p.getMaxTicketQuantity() : 0);
                    m.put("applicableMovieId", movieId);
                    m.put("applicableMovieIds", p.getApplicableMovieIds());
                    m.put("applicableMovieTitle", movieTitles != null ? movieTitles : "");
                    m.put("customerEligibility", p.getCustomerEligibility() != null ? p.getCustomerEligibility() : "ALL");
                    m.put("usageLimit", p.getUsageLimit() != null ? p.getUsageLimit() : 0);
                    m.put("usedCount", p.getUsedCount() != null ? p.getUsedCount() : 0);
                    m.put("exhausted", exhausted);
                    m.put("saved", saved);
                    m.put("ineligibilityReason", ineligibilityReason);
                    m.put("startDate", p.getStartDate() != null ? p.getStartDate().toString() : "");
                    m.put("endDate", p.getEndDate() != null ? p.getEndDate().toString() : "");
                    m.put("pointsRequired", p.getPointsRequired() != null ? p.getPointsRequired() : 0);
                    m.put("allowPointRedemption", Boolean.TRUE.equals(p.getAllowPointRedemption()));
                    m.put("isHidden", Boolean.TRUE.equals(p.getIsHidden()));
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** Các ưu đãi cho phép khách tự đổi bằng điểm — phục vụ trang "Ưu đãi của tôi". */
    @GetMapping("/promotions/redeemable")
    public ResponseEntity<?> getRedeemablePromotions() {
        LocalDateTime now = LocalDateTime.now();
        Integer currentUserId = com.devcine.backend.util.SecurityUtils.getCurrentUserId();
        List<Map<String, Object>> result = promotionRepository.findAll().stream()
                .filter(p -> !Boolean.FALSE.equals(p.getIsActive()))
                .filter(p -> Boolean.TRUE.equals(p.getAllowPointRedemption())
                        && p.getPointsRequired() != null && p.getPointsRequired() > 0)
                .filter(p -> (p.getStartDate() == null || !p.getStartDate().isAfter(now))
                        && (p.getEndDate() == null || !p.getEndDate().isBefore(now)))
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "code", p.getCode() != null ? p.getCode() : "",
                        "discountType", p.getDiscountType() != null ? p.getDiscountType() : "",
                        "discountValue", p.getDiscountValue() != null ? p.getDiscountValue() : 0,
                        "pointsRequired", p.getPointsRequired(),
                        "endDate", p.getEndDate() != null ? p.getEndDate().toString() : "",
                        "redeemed", currentUserId != null
                                && voucherRepository.existsByCustomerAndPromotion(currentUserId, p.getId())
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private String extractMovieIdsCsv(Object rawIds, Object rawSingleId) {
        if (rawIds instanceof List<?> list) {
            List<String> valid = list.stream().filter(Objects::nonNull)
                    .map(Object::toString).map(String::trim).filter(s -> !s.isEmpty()).toList();
            return valid.isEmpty() ? null : String.join(",", valid);
        }
        if (rawIds != null && !rawIds.toString().isBlank()) {
            return rawIds.toString().trim();
        }
        if (rawSingleId != null && !rawSingleId.toString().isBlank()) {
            return rawSingleId.toString().trim();
        }
        return null;
    }

    private Integer extractPrimaryMovieId(String movieIdsCsv, Object rawSingleId) {
        if (movieIdsCsv != null && !movieIdsCsv.isBlank()) {
            String first = movieIdsCsv.split(",")[0].trim();
            try {
                return Integer.parseInt(first);
            } catch (NumberFormatException ignored) {}
        }
        if (rawSingleId != null && !rawSingleId.toString().isBlank()) {
            try {
                return Integer.parseInt(rawSingleId.toString().trim());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    @PostMapping("/promotions")
    @PreAuthorize("@perm.can('promotions','add')")
    public ResponseEntity<?> createPromotion(@RequestBody Map<String, Object> body) {
        try {
            String code = body.get("code") != null ? ((String) body.get("code")).trim().toUpperCase() : "";
            if (code.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Vui lòng nhập mã code."));
            }
            if (code.length() < 3 || code.length() > 15) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Mã code phải dài từ 3 đến 15 ký tự."));
            }
            if (!code.matches("^[A-Z0-9]+$")) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Mã code chỉ được chứa chữ cái không dấu và số."));
            }
            if (promotionRepository.existsByCodeIgnoreCase(code)) {
                return ResponseEntity.status(409).body(ApiResponse.fail("Mã code '" + code + "' đã tồn tại. Vui lòng chọn mã khác."));
            }
            // Validate ngày: bắt đầu >= hôm nay; hết hạn > bắt đầu (mặc định bắt đầu = hôm nay nếu để trống)
            java.time.LocalDateTime startDt = body.get("startDate") != null ? LocalDateTime.parse((String) body.get("startDate")) : null;
            java.time.LocalDateTime endDt = body.get("endDate") != null ? LocalDateTime.parse((String) body.get("endDate")) : null;
            java.time.LocalDate today = java.time.LocalDate.now();
            if (startDt != null && startDt.toLocalDate().isBefore(today)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Ngày bắt đầu không được ở quá khứ."));
            }
            if (endDt != null && !endDt.toLocalDate().isAfter(startDt != null ? startDt.toLocalDate() : today)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Ngày hết hạn phải sau ngày bắt đầu."));
            }
            String movieIdsCsv = extractMovieIdsCsv(body.get("applicableMovieIds"), body.get("applicableMovieId"));
            Integer primaryMovieId = extractPrimaryMovieId(movieIdsCsv, body.get("applicableMovieId"));

            int maxTicketsLimit = systemSettingService.getMaxTicketsPerBooking();
            int maxTk = body.get("maxTicketQuantity") != null ? Integer.parseInt(body.get("maxTicketQuantity").toString()) : 0;
            if (maxTk < 0) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Số vé áp dụng không được là số âm."));
            }
            if (maxTk > maxTicketsLimit) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Số vé tối đa được giảm trên một đơn không được vượt quá " + maxTicketsLimit + " vé (theo cấu hình rạp)."));
            }

            Promotion promo = Promotion.builder()
                    .code(code)
                    .name((String) body.get("name"))
                    .description((String) body.get("description"))
                    .discountType((String) body.get("discountType"))
                    .discountValue(new BigDecimal(body.get("discountValue").toString()))
                    .startDate(startDt)
                    .endDate(endDt)
                    .isStackable(Boolean.parseBoolean(body.getOrDefault("isStackable", false).toString()))
                    .pointsRequired(body.get("pointsRequired") != null ? Integer.parseInt(body.get("pointsRequired").toString()) : 0)
                    .allowPointRedemption(Boolean.parseBoolean(body.getOrDefault("allowPointRedemption", false).toString()))
                    .minOrderValue(body.get("minOrderValue") != null ? new BigDecimal(body.get("minOrderValue").toString()) : BigDecimal.ZERO)
                    .applicableMovieId(primaryMovieId)
                    .applicableMovieIds(movieIdsCsv)
                    .isHidden(Boolean.parseBoolean(body.getOrDefault("isHidden", false).toString()))
                    .customerEligibility(body.get("customerEligibility") != null ? body.get("customerEligibility").toString() : "ALL")
                    .usageLimit(body.get("usageLimit") != null ? Integer.parseInt(body.get("usageLimit").toString()) : 0)
                    .maxTicketQuantity(maxTk)
                    .maxDiscountAmount(body.get("maxDiscountAmount") != null ? new BigDecimal(body.get("maxDiscountAmount").toString()) : BigDecimal.ZERO)
                    .build();
            promotionRepository.save(promo);
            notifyVoucherUpdate("PROMOTION_CREATED");
            return ResponseEntity.status(201).body(ApiResponse.ok(promo));
        } catch (Exception e) {
            log.error("Lỗi tạo promotion", e);
            return ResponseEntity.badRequest().body(ApiResponse.fail("Không thể tạo voucher. Vui lòng kiểm tra lại dữ liệu nhập."));
        }
    }

    @PutMapping("/promotions/{id}")
    @PreAuthorize("@perm.can('promotions','edit')")
    public ResponseEntity<?> updatePromotion(@PathVariable Integer id,
                                              @RequestBody Map<String, Object> body) {
        try {
            Promotion promo = promotionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));
            if (body.containsKey("code")) {
                String code = body.get("code") != null ? ((String) body.get("code")).trim().toUpperCase() : "";
                if (code.isBlank()) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Vui lòng nhập mã code."));
                }
                if (code.length() < 3 || code.length() > 15) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Mã code phải dài từ 3 đến 15 ký tự."));
                }
                if (!code.matches("^[A-Z0-9]+$")) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Mã code chỉ được chứa chữ cái không dấu và số."));
                }
                if (promotionRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                    return ResponseEntity.status(409).body(ApiResponse.fail("Mã code '" + code + "' đã tồn tại. Vui lòng chọn mã khác."));
                }
                promo.setCode(code);
            }
            if (body.containsKey("name")) promo.setName((String) body.get("name"));
            if (body.containsKey("description")) promo.setDescription((String) body.get("description"));
            if (body.containsKey("discountType")) promo.setDiscountType((String) body.get("discountType"));
            if (body.containsKey("discountValue")) promo.setDiscountValue(new BigDecimal(body.get("discountValue").toString()));
            // Validate ngày + KHÓA ngày bắt đầu khi voucher đang chạy (start <= hôm nay hoặc null)
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDateTime existingStart = promo.getStartDate();
            boolean running = existingStart == null || !existingStart.toLocalDate().isAfter(today);
            java.time.LocalDateTime newStart = body.get("startDate") != null ? LocalDateTime.parse((String) body.get("startDate")) : null;
            if (newStart != null) {
                boolean changed = existingStart == null || !newStart.toLocalDate().isEqual(existingStart.toLocalDate());
                if (running && changed) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Voucher đang chạy, không thể sửa ngày bắt đầu."));
                }
                if (!running && newStart.toLocalDate().isBefore(today)) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Ngày bắt đầu không được ở quá khứ."));
                }
                promo.setStartDate(newStart);
            }
            java.time.LocalDateTime newEnd = body.get("endDate") != null ? LocalDateTime.parse((String) body.get("endDate")) : promo.getEndDate();
            java.time.LocalDateTime effStart = newStart != null ? newStart : existingStart;
            java.time.LocalDate effStartDate = effStart != null ? effStart.toLocalDate() : today;
            if (newEnd != null && !newEnd.toLocalDate().isAfter(effStartDate)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Ngày hết hạn phải sau ngày bắt đầu."));
            }
            if (body.get("endDate") != null) promo.setEndDate(newEnd);
            if (body.containsKey("isStackable")) promo.setIsStackable(Boolean.parseBoolean(body.get("isStackable").toString()));
            if (body.containsKey("pointsRequired")) promo.setPointsRequired(body.get("pointsRequired") != null ? Integer.parseInt(body.get("pointsRequired").toString()) : 0);
            if (body.containsKey("allowPointRedemption")) promo.setAllowPointRedemption(Boolean.parseBoolean(body.get("allowPointRedemption").toString()));
            if (body.containsKey("minOrderValue")) promo.setMinOrderValue(body.get("minOrderValue") != null ? new BigDecimal(body.get("minOrderValue").toString()) : BigDecimal.ZERO);
            
            if (body.containsKey("applicableMovieIds") || body.containsKey("applicableMovieId")) {
                String movieIdsCsv = extractMovieIdsCsv(body.get("applicableMovieIds"), body.get("applicableMovieId"));
                Integer primaryMovieId = extractPrimaryMovieId(movieIdsCsv, body.get("applicableMovieId"));
                promo.setApplicableMovieIds(movieIdsCsv);
                promo.setApplicableMovieId(primaryMovieId);
            }
            if (body.containsKey("isHidden")) {
                promo.setIsHidden(Boolean.parseBoolean(body.get("isHidden").toString()));
            }
            if (body.containsKey("customerEligibility")) promo.setCustomerEligibility(body.get("customerEligibility") != null ? body.get("customerEligibility").toString() : "ALL");
            if (body.containsKey("usageLimit")) promo.setUsageLimit(body.get("usageLimit") != null ? Integer.parseInt(body.get("usageLimit").toString()) : 0);
            if (body.containsKey("maxTicketQuantity")) {
                int maxTicketsLimit = systemSettingService.getMaxTicketsPerBooking();
                int maxTkUpdate = body.get("maxTicketQuantity") != null ? Integer.parseInt(body.get("maxTicketQuantity").toString()) : 0;
                if (maxTkUpdate < 0) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Số vé áp dụng không được là số âm."));
                }
                if (maxTkUpdate > maxTicketsLimit) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Số vé tối đa được giảm trên một đơn không được vượt quá " + maxTicketsLimit + " vé (theo cấu hình rạp)."));
                }
                promo.setMaxTicketQuantity(maxTkUpdate);
            }
            if (body.containsKey("maxDiscountAmount")) promo.setMaxDiscountAmount(body.get("maxDiscountAmount") != null ? new BigDecimal(body.get("maxDiscountAmount").toString()) : BigDecimal.ZERO);
            promotionRepository.save(promo);
            notifyVoucherUpdate("PROMOTION_UPDATED");
            return ResponseEntity.ok(ApiResponse.ok(promo));
        } catch (Exception e) {
            log.error("Lỗi cập nhật promotion {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.fail("Không thể cập nhật voucher. Vui lòng kiểm tra lại dữ liệu nhập."));
        }
    }

    @PatchMapping("/promotions/{id}/toggle")
    @PreAuthorize("@perm.can('promotions','edit')")
    public ResponseEntity<?> togglePromotion(@PathVariable Integer id) {
        try {
            Promotion promo = promotionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher."));
            promo.setIsActive(promo.getIsActive() == null || !promo.getIsActive());
            promotionRepository.save(promo);
            notifyVoucherUpdate("PROMOTION_TOGGLED");
            String msg = (Boolean.TRUE.equals(promo.getIsActive()) ? "Đã kích hoạt" : "Đã tạm dừng") + " voucher '" + promo.getCode() + "'.";
            return ResponseEntity.ok(ApiResponse.ok(promo, msg));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/promotions/{id}")
    @PreAuthorize("@perm.can('promotions','delete')")
    public ResponseEntity<?> deletePromotion(@PathVariable Integer id) {
        try {
            promotionRepository.deleteById(id);
            notifyVoucherUpdate("PROMOTION_DELETED");
            return ResponseEntity.ok(ApiResponse.success("Đã xoá khuyến mãi."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Không thể xoá voucher do đã có khách hàng lưu hoặc sử dụng trong đơn hàng."));
        }
    }

    @GetMapping("/combos")
    public ResponseEntity<?> getCombos() {
        // Trả về danh sách voucher chưa sử dụng để dùng như combo trong POS
        List<Voucher> vouchers = voucherRepository.findAll().stream()
                .filter(v -> !Boolean.TRUE.equals(v.getIsUsed()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(vouchers));
    }

    // Gửi email chiến dịch mã ưu đãi tới toàn bộ khách thuộc đối tượng áp dụng
    @PostMapping("/promotions/{id}/send-campaign")
    @PreAuthorize("@perm.can('promotions','edit')")
    public ResponseEntity<?> sendCampaign(@PathVariable Integer id) {
        try {
            int sent = voucherService.sendCampaignEmails(id);
            String message = sent > 0
                    ? "Đã gửi email chiến dịch tới " + sent + " khách hàng."
                    : "Tất cả khách thuộc đối tượng đã nhận email mã này rồi.";
            return ResponseEntity.ok(ApiResponse.ok(Map.of("sent", sent, "message", message), message));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        }
    }

    // Phát voucher cho khách hàng
    @PostMapping("/promotions/{promoId}/issue-voucher")
    @PreAuthorize("@perm.can('promotions','edit')")
    public ResponseEntity<?> issueVoucher(@PathVariable Integer promoId,
                                           @RequestBody Map<String, Object> body) {
        try {
            Promotion promo = promotionRepository.findById(promoId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

            Integer customerId = Integer.parseInt(body.get("customerId").toString());
            var customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

            Voucher voucher = voucherService.issueVoucher(
                    promo,
                    customer,
                    promo.getEndDate() != null ? promo.getEndDate() : LocalDateTime.now().plusMonths(1)
            );
            notifyVoucherUpdate("VOUCHER_ISSUED");
            return ResponseEntity.status(201).body(ApiResponse.ok(voucher));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
