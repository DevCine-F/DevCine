package com.devcine.backend.service;

import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.Promotion;
import com.devcine.backend.entity.Voucher;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.PromotionRepository;
import com.devcine.backend.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Nghiệp vụ voucher phía khách hàng — hiện phục vụ tính năng "Đổi điểm tích luỹ lấy ưu đãi".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherService {

    private final PromotionRepository promotionRepository;
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;
    private final LoyaltyService loyaltyService;

    /**
     * Khách dùng điểm tích luỹ để đổi lấy một voucher từ chương trình khuyến mãi.
     * Chỉ áp dụng với promotion được admin bật {@code allowPointRedemption} và có {@code pointsRequired > 0}.
     */
    @Transactional
    public Voucher redeemWithPoints(Integer customerId, Integer promoId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        Promotion promo = promotionRepository.findById(promoId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ưu đãi"));

        if (!Boolean.TRUE.equals(promo.getAllowPointRedemption())
                || promo.getPointsRequired() == null || promo.getPointsRequired() <= 0) {
            throw new RuntimeException("Ưu đãi này không cho phép đổi bằng điểm.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (promo.getStartDate() != null && promo.getStartDate().isAfter(now)) {
            throw new RuntimeException("Ưu đãi chưa bắt đầu.");
        }
        if (promo.getEndDate() != null && promo.getEndDate().isBefore(now)) {
            throw new RuntimeException("Ưu đãi đã hết hạn.");
        }

        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        if (currentPoints < promo.getPointsRequired()) {
            throw new RuntimeException("Bạn không đủ điểm để đổi ưu đãi này.");
        }

        // Trừ điểm qua LoyaltyService: chỉ trừ ví tiêu được (GIỮ NGUYÊN tích lũy trọn đời -> hạng
        // không tụt) và ghi sổ điểm.
        loyaltyService.redeem(customer, promo.getPointsRequired(), promo.getCode());

        Voucher voucher = Voucher.builder()
                .promotion(promo)
                .customer(customer)
                .isUsed(false)
                .validUntil(promo.getEndDate() != null ? promo.getEndDate() : now.plusMonths(1))
                .build();
        voucherRepository.save(voucher);

        log.info("Khách #{} đổi {} điểm lấy voucher từ promotion #{}", customerId, promo.getPointsRequired(), promoId);
        return voucher;
    }

    /**
     * Khách nhập MÃ để lưu voucher vào ví. Chỉ áp dụng với promotion KHÔNG bật đổi-bằng-điểm
     * (loại đổi-bằng-điểm phải dùng {@link #redeemWithPoints}).
     */
    @Transactional
    public Voucher claimByCode(Integer customerId, String code) {
        if (code == null || code.isBlank()) {
            throw new RuntimeException("Vui lòng nhập mã ưu đãi.");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        Promotion promo = promotionRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new RuntimeException("Mã ưu đãi không tồn tại."));

        if (Boolean.TRUE.equals(promo.getAllowPointRedemption())) {
            throw new RuntimeException("Mã này chỉ có thể đổi bằng điểm tích luỹ.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (promo.getStartDate() != null && promo.getStartDate().isAfter(now)) {
            throw new RuntimeException("Ưu đãi chưa bắt đầu.");
        }
        if (promo.getEndDate() != null && promo.getEndDate().isBefore(now)) {
            throw new RuntimeException("Ưu đãi đã hết hạn.");
        }

        if (voucherRepository.findActiveVoucherByCustomerAndCode(customerId, promo.getCode(), now).isPresent()) {
            throw new RuntimeException("Bạn đã lưu mã này rồi.");
        }

        Voucher voucher = Voucher.builder()
                .promotion(promo)
                .customer(customer)
                .isUsed(false)
                .validUntil(promo.getEndDate() != null ? promo.getEndDate() : now.plusMonths(1))
                .build();
        voucherRepository.save(voucher);

        log.info("Khách #{} lưu voucher bằng mã '{}' (promotion #{})", customerId, promo.getCode(), promo.getId());
        return voucher;
    }

    /**
     * Dùng cho bước thanh toán: trả về voucher của khách theo mã để áp dụng.
     * Nếu khách đã sở hữu (đã lưu) → trả về voucher đó; nếu chưa nhưng mã hợp lệ (không phải
     * loại đổi-điểm) → tự lưu rồi trả về. Trả về voucher chưa dùng, còn hạn.
     */
    @Transactional
    public Voucher getOrClaimForCheckout(Integer customerId, String code) {
        if (code == null || code.isBlank()) {
            throw new RuntimeException("Vui lòng nhập mã ưu đãi.");
        }
        Promotion promo = promotionRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new RuntimeException("Mã ưu đãi không tồn tại."));

        return voucherRepository.findActiveVoucherByCustomerAndCode(customerId, promo.getCode(), LocalDateTime.now())
                .orElseGet(() -> claimByCode(customerId, promo.getCode()));
    }
}
