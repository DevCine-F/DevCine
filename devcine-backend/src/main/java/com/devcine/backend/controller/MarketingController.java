package com.devcine.backend.controller;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/marketing")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class MarketingController {

    private final PromotionRepository promotionRepository;
    private final VoucherRepository voucherRepository;
    private final CustomerRepository customerRepository;

    @GetMapping("/promotions")
    public ResponseEntity<?> getAllPromotions() {
        return ResponseEntity.ok(promotionRepository.findAll());
    }

    /**
     * Danh sách khuyến mãi công khai cho trang Khuyến mãi phía khách — CHỈ gồm các ưu đãi
     * admin cho phép đổi bằng điểm. Ưu đãi không bật đổi-điểm là "mã bí mật", khách phải tự
     * nhập mã để lưu (không hiển thị công khai).
     */
    @GetMapping("/promotions/active")
    public ResponseEntity<?> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        // Trả mọi promotion đang trong thời gian áp dụng (cả mã đổi-điểm lẫn mã lưu-miễn-phí);
        // FE dựa vào allowPointRedemption + pointsRequired để chọn nút "Đổi điểm" hay "Lưu mã".
        List<Map<String, Object>> result = promotionRepository.findAll().stream()
                .filter(p -> p.getCode() != null && !p.getCode().isBlank())
                .filter(p -> (p.getStartDate() == null || !p.getStartDate().isAfter(now))
                        && (p.getEndDate() == null || !p.getEndDate().isBefore(now)))
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "code", p.getCode() != null ? p.getCode() : "",
                        "name", p.getName() != null ? p.getName() : "",
                        "discountType", p.getDiscountType() != null ? p.getDiscountType() : "",
                        "discountValue", p.getDiscountValue() != null ? p.getDiscountValue() : 0,
                        "startDate", p.getStartDate() != null ? p.getStartDate().toString() : "",
                        "endDate", p.getEndDate() != null ? p.getEndDate().toString() : "",
                        "pointsRequired", p.getPointsRequired() != null ? p.getPointsRequired() : 0,
                        "allowPointRedemption", Boolean.TRUE.equals(p.getAllowPointRedemption())
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** Các ưu đãi cho phép khách tự đổi bằng điểm — phục vụ trang "Ưu đãi của tôi". */
    @GetMapping("/promotions/redeemable")
    public ResponseEntity<?> getRedeemablePromotions() {
        LocalDateTime now = LocalDateTime.now();
        // Khách đang đăng nhập (nếu có) -> đánh dấu mã đã đổi để FE vô hiệu nút "Đổi ngay"
        // (mỗi mã chỉ đổi 1 lần/khách). Khách vãng lai: currentUserId null -> redeemed = false.
        Integer currentUserId = com.devcine.backend.util.SecurityUtils.getCurrentUserId();
        List<Map<String, Object>> result = promotionRepository.findAll().stream()
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
        return ResponseEntity.ok(result);
    }

    @PostMapping("/promotions")
    @PreAuthorize("@perm.can('promotions','add')")
    public ResponseEntity<?> createPromotion(@RequestBody Map<String, Object> body) {
        try {
            String code = body.get("code") != null ? ((String) body.get("code")).trim() : "";
            if (code.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng nhập mã code."));
            }
            if (promotionRepository.existsByCodeIgnoreCase(code)) {
                return ResponseEntity.status(409).body(Map.of("success", false, "message", "Mã code '" + code + "' đã tồn tại. Vui lòng chọn mã khác."));
            }
            Promotion promo = Promotion.builder()
                    .code(code)
                    .name((String) body.get("name"))
                    .description((String) body.get("description"))
                    .discountType((String) body.get("discountType"))
                    .discountValue(new BigDecimal(body.get("discountValue").toString()))
                    .startDate(body.get("startDate") != null ? LocalDateTime.parse((String) body.get("startDate")) : null)
                    .endDate(body.get("endDate") != null ? LocalDateTime.parse((String) body.get("endDate")) : null)
                    .isStackable(Boolean.parseBoolean(body.getOrDefault("isStackable", false).toString()))
                    .pointsRequired(body.get("pointsRequired") != null ? Integer.parseInt(body.get("pointsRequired").toString()) : 0)
                    .allowPointRedemption(Boolean.parseBoolean(body.getOrDefault("allowPointRedemption", false).toString()))
                    .minOrderValue(body.get("minOrderValue") != null ? new BigDecimal(body.get("minOrderValue").toString()) : BigDecimal.ZERO)
                    .applicableMovieId(body.get("applicableMovieId") != null && !body.get("applicableMovieId").toString().isBlank()
                            ? Integer.parseInt(body.get("applicableMovieId").toString()) : null)
                    .customerEligibility(body.get("customerEligibility") != null ? body.get("customerEligibility").toString() : "ALL")
                    .usageLimit(body.get("usageLimit") != null ? Integer.parseInt(body.get("usageLimit").toString()) : 0)
                    .maxTicketQuantity(body.get("maxTicketQuantity") != null ? Integer.parseInt(body.get("maxTicketQuantity").toString()) : 0)
                    .maxDiscountAmount(body.get("maxDiscountAmount") != null ? new BigDecimal(body.get("maxDiscountAmount").toString()) : BigDecimal.ZERO)
                    .build();
            promotionRepository.save(promo);
            return ResponseEntity.status(201).body(Map.of("success", true, "data", promo));
        } catch (Exception e) {
            log.error("Lỗi tạo promotion", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không thể tạo voucher. Vui lòng kiểm tra lại dữ liệu nhập."));
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
                String code = body.get("code") != null ? ((String) body.get("code")).trim() : "";
                if (code.isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng nhập mã code."));
                }
                if (promotionRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                    return ResponseEntity.status(409).body(Map.of("success", false, "message", "Mã code '" + code + "' đã tồn tại. Vui lòng chọn mã khác."));
                }
                promo.setCode(code);
            }
            if (body.containsKey("name")) promo.setName((String) body.get("name"));
            if (body.containsKey("description")) promo.setDescription((String) body.get("description"));
            if (body.containsKey("discountType")) promo.setDiscountType((String) body.get("discountType"));
            if (body.containsKey("discountValue")) promo.setDiscountValue(new BigDecimal(body.get("discountValue").toString()));
            if (body.get("startDate") != null) promo.setStartDate(LocalDateTime.parse((String) body.get("startDate")));
            if (body.get("endDate") != null) promo.setEndDate(LocalDateTime.parse((String) body.get("endDate")));
            if (body.containsKey("isStackable")) promo.setIsStackable(Boolean.parseBoolean(body.get("isStackable").toString()));
            if (body.containsKey("pointsRequired")) promo.setPointsRequired(body.get("pointsRequired") != null ? Integer.parseInt(body.get("pointsRequired").toString()) : 0);
            if (body.containsKey("allowPointRedemption")) promo.setAllowPointRedemption(Boolean.parseBoolean(body.get("allowPointRedemption").toString()));
            if (body.containsKey("minOrderValue")) promo.setMinOrderValue(body.get("minOrderValue") != null ? new BigDecimal(body.get("minOrderValue").toString()) : BigDecimal.ZERO);
            if (body.containsKey("applicableMovieId")) promo.setApplicableMovieId(body.get("applicableMovieId") != null && !body.get("applicableMovieId").toString().isBlank()
                    ? Integer.parseInt(body.get("applicableMovieId").toString()) : null);
            if (body.containsKey("customerEligibility")) promo.setCustomerEligibility(body.get("customerEligibility") != null ? body.get("customerEligibility").toString() : "ALL");
            if (body.containsKey("usageLimit")) promo.setUsageLimit(body.get("usageLimit") != null ? Integer.parseInt(body.get("usageLimit").toString()) : 0);
            if (body.containsKey("maxTicketQuantity")) promo.setMaxTicketQuantity(body.get("maxTicketQuantity") != null ? Integer.parseInt(body.get("maxTicketQuantity").toString()) : 0);
            if (body.containsKey("maxDiscountAmount")) promo.setMaxDiscountAmount(body.get("maxDiscountAmount") != null ? new BigDecimal(body.get("maxDiscountAmount").toString()) : BigDecimal.ZERO);
            promotionRepository.save(promo);
            return ResponseEntity.ok(Map.of("success", true, "data", promo));
        } catch (Exception e) {
            log.error("Lỗi cập nhật promotion {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không thể cập nhật voucher. Vui lòng kiểm tra lại dữ liệu nhập."));
        }
    }

    @DeleteMapping("/promotions/{id}")
    @PreAuthorize("@perm.can('promotions','delete')")
    public ResponseEntity<?> deletePromotion(@PathVariable Integer id) {
        try {
            promotionRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/combos")
    public ResponseEntity<?> getCombos() {
        // Trả về danh sách voucher chưa sử dụng để dùng như combo trong POS
        List<Voucher> vouchers = voucherRepository.findAll().stream()
                .filter(v -> !Boolean.TRUE.equals(v.getIsUsed()))
                .toList();
        return ResponseEntity.ok(vouchers);
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

            Voucher voucher = Voucher.builder()
                    .promotion(promo)
                    .customer(customer)
                    .isUsed(false)
                    .validUntil(promo.getEndDate() != null ? promo.getEndDate() : LocalDateTime.now().plusMonths(1))
                    .build();
            voucherRepository.save(voucher);
            return ResponseEntity.status(201).body(Map.of("success", true, "data", voucher));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
