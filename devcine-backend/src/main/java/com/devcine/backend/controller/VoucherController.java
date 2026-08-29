package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.request.VoucherPreviewRequest;
import com.devcine.backend.entity.Promotion;
import com.devcine.backend.entity.Voucher;
import com.devcine.backend.repository.PromotionRepository;
import com.devcine.backend.repository.VoucherRepository;
import com.devcine.backend.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherRepository voucherRepository;
    private final PromotionRepository promotionRepository;
    private final VoucherService voucherService;
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

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getActiveVouchers(@PathVariable Integer customerId) {
        List<Voucher> vouchers = voucherRepository.findActiveVouchersByCustomerId(customerId, java.time.LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.ok(vouchers));
    }

    /**
     * Chấm điều kiện + tính số giảm THỰC của toàn bộ voucher đang hiệu lực theo giỏ hàng hiện tại.
     * FE dùng để làm mờ mã không đủ điều kiện & hiển thị đúng số giảm ngay ở bước áp mã.
     */
    @PostMapping("/preview")
    public ResponseEntity<?> previewVouchers(@RequestBody VoucherPreviewRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(voucherService.previewActiveVouchers(req)));
    }

    /**
     * Toàn bộ voucher của khách (cả đang dùng, đã dùng, đã hết hạn) kèm trạng thái suy ra,
     * phục vụ trang "Ưu đãi của tôi" (tab voucher hiện có + tab lịch sử).
     */
    @GetMapping("/customer/{customerId}/all")
    public ResponseEntity<?> getAllVouchers(@PathVariable Integer customerId) {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = voucherRepository.findAllByCustomerIdWithPromotion(customerId)
                .stream().map(v -> {
                    String status;
                    if (Boolean.TRUE.equals(v.getIsUsed())) {
                        status = "USED";
                    } else if (v.getValidUntil() != null && v.getValidUntil().isBefore(now)) {
                        status = "EXPIRED";
                    } else {
                        // Voucher chưa dùng và còn hạn, nhưng cần kiểm tra thêm:
                        // Nếu promotion đã hết lượt toàn hệ thống → đánh dấu EXHAUSTED.
                        // Khách không thể dùng và không nên thấy ở tab "Ưu đãi hiện tại".
                        var promo = v.getPromotion();
                        boolean exhausted = promo != null
                                && promo.getUsageLimit() != null
                                && promo.getUsageLimit() > 0
                                && promo.getUsedCount() != null
                                && promo.getUsedCount() >= promo.getUsageLimit();
                        status = exhausted ? "EXHAUSTED" : "ACTIVE";
                    }
                    return Map.<String, Object>of(
                            "id", v.getId(),
                            "code", v.getPromotion().getCode() != null ? v.getPromotion().getCode() : "",
                            "discountType", v.effectiveDiscountType() != null ? v.effectiveDiscountType() : "",
                            "discountValue", v.effectiveDiscountValue() != null ? v.effectiveDiscountValue() : 0,
                            "validUntil", v.getValidUntil() != null ? v.getValidUntil().toString() : "",
                            "usedAt", v.getUsedAt() != null ? v.getUsedAt().toString() : "",
                            "status", status
                    );
                }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateVoucher(@RequestParam String code, @RequestParam Integer customerId) {
        Voucher voucher = voucherRepository.findActiveVoucherByCustomerAndCode(customerId, code, java.time.LocalDateTime.now())
                .orElse(null);
        
        if (voucher == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Mã giảm giá không tồn tại, đã sử dụng hoặc hết hạn."));
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "id", voucher.getId(),
            "code", voucher.getPromotion().getCode(),
            "discountType", voucher.effectiveDiscountType(),
            "discountValue", voucher.effectiveDiscountValue(),
            "validUntil", voucher.getValidUntil().toString()
        )));
    }

    /** Tra cứu một mã ưu đãi để xem có thể lưu vào ví hay không (phục vụ ô tìm/nhập mã ở "Ưu đãi của tôi"). */
    @GetMapping("/lookup")
    public ResponseEntity<?> lookupByCode(@RequestParam Integer customerId, @RequestParam String code) {
        if (code == null || code.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("found", false, "reason", "NOT_FOUND")));
        }
        Promotion promo = promotionRepository.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (promo == null) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("found", false, "reason", "NOT_FOUND")));
        }

        LocalDateTime now = LocalDateTime.now();
        boolean expired = (promo.getEndDate() != null && promo.getEndDate().isBefore(now))
                || (promo.getStartDate() != null && promo.getStartDate().isAfter(now));
        boolean pointOnly = Boolean.TRUE.equals(promo.getAllowPointRedemption());
        boolean owned = voucherRepository.findActiveVoucherByCustomerAndCode(customerId, promo.getCode(), now).isPresent();
        boolean claimable = !pointOnly && !expired && !owned;
        String reason = pointOnly ? "POINT_ONLY" : expired ? "EXPIRED" : owned ? "ALREADY_OWNED" : "OK";

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "found", true,
                "claimable", claimable,
                "reason", reason,
                "code", promo.getCode() != null ? promo.getCode() : "",
                "discountType", promo.getDiscountType() != null ? promo.getDiscountType() : "",
                "discountValue", promo.getDiscountValue() != null ? promo.getDiscountValue() : 0,
                "endDate", promo.getEndDate() != null ? promo.getEndDate().toString() : ""
        )));
    }

    /** Bước thanh toán: áp dụng voucher theo mã (tự lưu nếu hợp lệ mà chưa có), trả về để áp dụng. */
    @PostMapping("/apply")
    public ResponseEntity<?> applyByCode(@RequestParam Integer customerId, @RequestParam String code) {
        try {
            Voucher voucher = voucherService.getOrClaimForCheckout(customerId, code);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "id", voucher.getId(),
                "code", voucher.getPromotion().getCode() != null ? voucher.getPromotion().getCode() : "",
                "discountType", voucher.effectiveDiscountType() != null ? voucher.effectiveDiscountType() : "",
                "discountValue", voucher.effectiveDiscountValue() != null ? voucher.effectiveDiscountValue() : 0,
                "maxTicketQuantity", voucher.effectiveMaxTicketQuantity() != null ? voucher.effectiveMaxTicketQuantity() : 0,
                "maxDiscountAmount", voucher.effectiveMaxDiscountAmount() != null ? voucher.effectiveMaxDiscountAmount() : 0,
                "validUntil", voucher.getValidUntil() != null ? voucher.getValidUntil().toString() : ""
            )));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        }
    }

    /** Khách nhập mã để lưu voucher vào ví "Ưu đãi của tôi". */
    @PostMapping("/claim")
    public ResponseEntity<?> claimByCode(@RequestParam Integer customerId, @RequestParam String code) {
        try {
            Voucher voucher = voucherService.claimByCode(customerId, code);
            notifyVoucherUpdate("VOUCHER_CLAIMED");
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "voucherId", voucher.getId(),
                "code", voucher.getPromotion().getCode() != null ? voucher.getPromotion().getCode() : ""
            )));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        }
    }

    /** Khách đổi điểm tích luỹ lấy voucher từ một chương trình khuyến mãi. */
    @PostMapping("/redeem")
    public ResponseEntity<?> redeemWithPoints(@RequestParam Integer customerId, @RequestParam Integer promoId) {
        try {
            Voucher voucher = voucherService.redeemWithPoints(customerId, promoId);
            notifyVoucherUpdate("VOUCHER_REDEEMED");
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "voucherId", voucher.getId(),
                "code", voucher.getPromotion().getCode() != null ? voucher.getPromotion().getCode() : "",
                "remainingPoints", voucher.getCustomer().getLoyaltyPoints() != null ? voucher.getCustomer().getLoyaltyPoints() : 0
            )));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        }
    }
}
