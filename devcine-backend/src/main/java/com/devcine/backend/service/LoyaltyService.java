package com.devcine.backend.service;

import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.PointTransaction;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.PointTransactionRepository;
import com.devcine.backend.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Nguồn chân lý DUY NHẤT cho điểm thưởng — dùng chung cho MỌI kênh (vé online, vé POS, F&B).
 *
 * <p>Mô hình 2 loại điểm:
 * <ul>
 *   <li>{@code loyaltyPoints} — ví tiêu được: +khi mua, -khi đổi điểm/void.</li>
 *   <li>{@code lifetimePoints} — điểm tích lũy trọn đời: CHỈ xét hạng, KHÔNG giảm khi đổi điểm
 *       (tránh lỗi tụt hạng sau khi khách đổi voucher).</li>
 * </ul>
 * Mọi biến động đều ghi vào sổ điểm {@link PointTransaction}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private static final BigDecimal DEFAULT_RATE = BigDecimal.valueOf(1000); // 1.000đ = 1 điểm
    private static final int TIER_SILVER = 2000;
    private static final int TIER_GOLD = 5000;
    private static final int TIER_PLATINUM = 10000;

    public static final String TYPE_EARN = "EARN";
    public static final String TYPE_REDEEM = "REDEEM";
    public static final String TYPE_VOID = "VOID";

    private final CustomerRepository customerRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final SystemSettingRepository systemSettingRepository;

    /** Số điểm nhận được cho một khoản chi (làm tròn xuống theo tỉ lệ quy đổi). */
    public int pointsFor(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return 0;
        return amount.divide(rate(), 0, RoundingMode.DOWN).intValue();
    }

    /** Xét hạng theo điểm tích lũy trọn đời. */
    public String tierFor(int lifetimePoints) {
        if (lifetimePoints >= TIER_PLATINUM) return "PLATINUM";
        if (lifetimePoints >= TIER_GOLD) return "GOLD";
        if (lifetimePoints >= TIER_SILVER) return "SILVER";
        return "BRONZE";
    }

    /** Thứ hạng để so sánh cao/thấp: BRONZE=0 &lt; SILVER=1 &lt; GOLD=2 &lt; PLATINUM=3. */
    public int tierRank(String tier) {
        if (tier == null) return 0;
        return switch (tier.toUpperCase()) {
            case "PLATINUM" -> 3;
            case "GOLD" -> 2;
            case "SILVER" -> 1;
            default -> 0; // BRONZE / không xác định
        };
    }

    /** Nhãn tiếng Việt của hạng thành viên (dùng cho thông báo eligibility). */
    public String tierLabelVi(String tier) {
        return switch (tier == null ? "" : tier.toUpperCase()) {
            case "SILVER" -> "Bạc";
            case "GOLD" -> "Vàng";
            case "PLATINUM" -> "Bạch Kim";
            default -> tier;
        };
    }

    /**
     * Cộng điểm khi có đơn thành công (vé/F&B). Cộng cả ví tiêu được lẫn tích lũy trọn đời,
     * cập nhật hạng theo tích lũy, và ghi sổ điểm. Idempotency do người gọi đảm bảo
     * (completePayment chỉ chạy 1 lần cho mỗi booking).
     *
     * @return số điểm thực nhận (0 nếu khách null hoặc không đủ để tính 1 điểm).
     */
    @Transactional
    public int award(Customer customer, BigDecimal amount, String source, String refCode) {
        if (customer == null) return 0;
        int earned = pointsFor(amount);
        if (earned <= 0) return 0;

        int newBalance = nz(customer.getLoyaltyPoints()) + earned;
        int newLifetime = nz(customer.getLifetimePoints()) + earned;
        customer.setLoyaltyPoints(newBalance);
        customer.setLifetimePoints(newLifetime);
        customer.setMembershipTier(tierFor(newLifetime));
        customerRepository.save(customer);

        record(customer, earned, TYPE_EARN, source, refCode, newBalance, null);
        log.info("Tích {} điểm cho khách #{} từ {} {} (số dư {}, tích lũy {})",
                earned, customer.getUserId(), source, refCode, newBalance, newLifetime);
        return earned;
    }

    /**
     * Thu hồi điểm đã cộng khi hủy/void một đơn (đảo cả ví lẫn tích lũy trọn đời → hạng có thể giảm),
     * và ghi sổ điểm. Số điểm thu hồi = đúng số đã cộng cho khoản tiền của đơn.
     */
    @Transactional
    public void reclaim(Customer customer, BigDecimal amount, String source, String refCode) {
        if (customer == null) return;
        int points = pointsFor(amount);
        if (points <= 0) return;

        int newBalance = Math.max(0, nz(customer.getLoyaltyPoints()) - points);
        int newLifetime = Math.max(0, nz(customer.getLifetimePoints()) - points);
        customer.setLoyaltyPoints(newBalance);
        customer.setLifetimePoints(newLifetime);
        customer.setMembershipTier(tierFor(newLifetime));
        customerRepository.save(customer);

        record(customer, -points, TYPE_VOID, source, refCode, newBalance, null);
        log.info("Thu hồi {} điểm của khách #{} do {} {}", points, customer.getUserId(), source, refCode);
    }

    /**
     * Trừ điểm khi khách ĐỔI điểm lấy ưu đãi — chỉ trừ ví tiêu được, GIỮ NGUYÊN tích lũy trọn đời
     * (hạng không tụt vì đã tiêu điểm). Người gọi phải kiểm tra đủ điểm trước.
     */
    @Transactional
    public void redeem(Customer customer, int points, String refCode) {
        if (customer == null || points <= 0) return;
        int newBalance = Math.max(0, nz(customer.getLoyaltyPoints()) - points);
        customer.setLoyaltyPoints(newBalance);
        customerRepository.save(customer);
        record(customer, -points, TYPE_REDEEM, "PROMO_REDEEM", refCode, newBalance, null);
    }

    private void record(Customer customer, int points, String type, String source,
                        String refCode, int balanceAfter, String note) {
        pointTransactionRepository.save(PointTransaction.builder()
                .customer(customer)
                .points(points)
                .type(type)
                .source(source)
                .refCode(refCode)
                .balanceAfter(balanceAfter)
                .note(note)
                .build());
    }

    private BigDecimal rate() {
        try {
            var setting = systemSettingRepository.findById("LOYALTY_POINT_RATE").orElse(null);
            if (setting != null && setting.getSettingValue() != null) {
                BigDecimal parsed = new BigDecimal(setting.getSettingValue().trim());
                if (parsed.compareTo(BigDecimal.ZERO) > 0) return parsed;
            }
        } catch (Exception ignored) {
            // Cấu hình lỗi → dùng mặc định
        }
        return DEFAULT_RATE;
    }

    private int nz(Integer v) {
        return v != null ? v : 0;
    }
}
