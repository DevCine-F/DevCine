package com.devcine.backend.service;

import com.devcine.backend.dto.request.VoucherPreviewRequest;
import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.PromoEmailLog;
import com.devcine.backend.entity.Promotion;
import com.devcine.backend.entity.User;
import com.devcine.backend.entity.Voucher;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.PromoEmailLogRepository;
import com.devcine.backend.repository.PromotionRepository;
import com.devcine.backend.repository.UserRepository;
import com.devcine.backend.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final LoyaltyService loyaltyService;
    private final MailService mailService;
    private final PromoEmailLogRepository promoEmailLogRepository;

    /**
     * Gửi email chiến dịch (chỉ thông báo mã) tới TOÀN BỘ khách thuộc đúng ĐỐI TƯỢNG áp dụng của
     * ưu đãi — dùng chung bộ lọc {@link #eligibilityReason} (khách hợp lệ ⟺ reason == null).
     * Bỏ qua khách không có email; mã đổi-điểm không cho gửi (khách không nhập mã trực tiếp được).
     *
     * @return số khách đã gửi (dispatch async).
     */
    @Transactional
    public int sendCampaignEmails(Integer promoId) {
        Promotion promo = promotionRepository.findById(promoId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ưu đãi."));
        if (Boolean.TRUE.equals(promo.getAllowPointRedemption())) {
            throw new RuntimeException("Mã đổi-điểm không gửi email chiến dịch được (khách không nhập mã trực tiếp).");
        }
        // Dedup: khách ĐÃ nhận mã này rồi thì KHÔNG gửi lại (chống spam trùng người)
        Set<Integer> alreadySent = new HashSet<>(promoEmailLogRepository.findCustomerIdsByPromotionId(promoId));

        // Pre-fetch 1 lần tập customerId đã có đơn CONFIRMED → tránh N query countConfirmedByCustomer()
        // khi điều kiện là NEW_CUSTOMER (eligibilityReason gọi per-customer query → N+1 với nhiều khách).
        String elig = promo.getCustomerEligibility();
        Set<Integer> customerIdsWithBooking = ("NEW_CUSTOMER".equalsIgnoreCase(elig))
                ? bookingRepository.findCustomerIdsWithConfirmedBookings()
                : java.util.Collections.emptySet();

        LocalDateTime now = LocalDateTime.now();
        List<PromoEmailLog> newLogs = new ArrayList<>();
        for (Customer c : customerRepository.findAllWithUser()) {
            User u = c.getUser();
            if (u == null || u.getEmail() == null || u.getEmail().isBlank()) continue;
            if (alreadySent.contains(c.getUserId())) continue; // đã nhận rồi → bỏ qua

            // Kiểm tra đối tượng in-memory nếu là NEW_CUSTOMER, gọi eligibilityReason() cho các trường hợp khác
            if ("NEW_CUSTOMER".equalsIgnoreCase(elig)) {
                if (customerIdsWithBooking.contains(c.getUserId())) continue; // không phải khách mới
            } else if (eligibilityReason(c.getUserId(), c, promo) != null) {
                continue; // không thuộc đối tượng
            }

            mailService.sendPromotionEmail(u.getEmail(), u.getFullName(), promo);
            newLogs.add(PromoEmailLog.builder().promotionId(promoId).customerId(c.getUserId()).sentAt(now).build());
        }
        int sent = newLogs.size();
        if (sent > 0) {
            promoEmailLogRepository.saveAll(newLogs);
            promo.setCampaignSentCount((promo.getCampaignSentCount() != null ? promo.getCampaignSentCount() : 0) + sent);
            promo.setCampaignSentAt(now);
            promotionRepository.save(promo);
        }
        log.info("Chiến dịch email ưu đãi #{} ({}): gửi MỚI tới {} khách (đã có {} khách nhận trước đó).",
                promoId, promo.getCode(), sent, alreadySent.size());
        return sent;
    }


    /** Kết quả chấm một voucher theo giỏ hàng. reason = null khi đủ điều kiện. discountAmount = số giảm THÔ. */
    public record VoucherEval(boolean applicable, String reason, BigDecimal discountAmount) {}

    /**
     * Lý do KHÔNG đủ điều kiện theo ĐỐI TƯỢNG áp dụng (null nếu đủ). Dùng chung cho apply/claim,
     * preview và (gián tiếp) đặt vé: NEW_CUSTOMER (chưa từng mua vé) & TIER_* (hạng tối thiểu).
     */
    private String eligibilityReason(Integer customerId, Customer customer, Promotion promo) {
        String elig = promo.getCustomerEligibility();
        if (elig == null || elig.equalsIgnoreCase("ALL")) return null;
        if ("NEW_CUSTOMER".equalsIgnoreCase(elig)) {
            if (bookingRepository.countConfirmedByCustomer(customerId) > 0) {
                return "Mã chỉ dành cho khách hàng mới (chưa từng mua vé).";
            }
        } else if (elig.startsWith("TIER_")) {
            String requiredTier = elig.substring(5); // SILVER | GOLD | PLATINUM
            int lifetime = customer != null && customer.getLifetimePoints() != null ? customer.getLifetimePoints() : 0;
            if (loyaltyService.tierRank(loyaltyService.tierFor(lifetime)) < loyaltyService.tierRank(requiredTier)) {
                return "Mã chỉ dành cho khách hàng hạng " + loyaltyService.tierLabelVi(requiredTier) + " trở lên.";
            }
        }
        return null;
    }

    /** Chặn sớm theo đối tượng áp dụng ngay ở bước áp/lưu mã (ném lỗi nếu không đủ). */
    private void assertEligibility(Integer customerId, Customer customer, Promotion promo) {
        String reason = eligibilityReason(customerId, customer, promo);
        if (reason != null) throw new RuntimeException(reason);
    }

    /**
     * Chấm một voucher theo ngữ cảnh giỏ hàng — NGUỒN SỰ THẬT DUY NHẤT dùng chung với
     * {@code BookingService} để preview khớp với lúc đặt vé. Giữ đúng THỨ TỰ kiểm tra:
     * đơn tối thiểu → theo phim → đối tượng → lượt dùng; rồi tính base & số giảm (có trần).
     *
     * @param orderTotal tổng tiền đơn (ghế + bắp nước)
     * @param movieId    phim của suất đang đặt (null = bỏ qua kiểm theo phim)
     * @param seatPrices giá từng ghế — để tính base khi mã giới hạn số vé
     * @return số giảm THÔ (chưa kẹp về 0/tổng đơn); caller tự kẹp finalPrice.
     */
    public VoucherEval evaluate(Integer customerId, Customer customer, Promotion promo,
                                BigDecimal orderTotal, Integer movieId, List<BigDecimal> seatPrices) {
        if (promo.getMinOrderValue() != null && orderTotal.compareTo(promo.getMinOrderValue()) < 0) {
            return new VoucherEval(false,
                    "Đơn tối thiểu " + promo.getMinOrderValue().toBigInteger() + "đ để áp dụng mã này.", BigDecimal.ZERO);
        }
        if (promo.getApplicableMovieId() != null && movieId != null
                && !promo.getApplicableMovieId().equals(movieId)) {
            return new VoucherEval(false, "Mã chỉ áp dụng cho phim khác, không dùng được cho suất này.", BigDecimal.ZERO);
        }
        String eligReason = eligibilityReason(customerId, customer, promo);
        if (eligReason != null) return new VoucherEval(false, eligReason, BigDecimal.ZERO);
        if (promo.getUsageLimit() != null && promo.getUsageLimit() > 0
                && promo.getUsedCount() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            return new VoucherEval(false, "Mã đã hết lượt sử dụng.", BigDecimal.ZERO);
        }

        // Base tính giảm: mặc định cả đơn; nếu giới hạn số vé → chỉ X vé ĐẮT NHẤT
        BigDecimal base = orderTotal;
        Integer maxTk = promo.getMaxTicketQuantity();
        if (maxTk != null && maxTk > 0 && seatPrices != null && !seatPrices.isEmpty()) {
            base = seatPrices.stream().filter(Objects::nonNull)
                    .sorted(Comparator.reverseOrder()).limit(maxTk)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        BigDecimal discount = BigDecimal.ZERO;
        if ("PERCENTAGE".equalsIgnoreCase(promo.getDiscountType())) {
            discount = base.multiply(promo.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else if ("FIXED_AMOUNT".equalsIgnoreCase(promo.getDiscountType())) {
            discount = promo.getDiscountValue().min(base);
        }
        BigDecimal maxDisc = promo.getMaxDiscountAmount();
        if (maxDisc != null && maxDisc.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(maxDisc) > 0) {
            discount = maxDisc;
        }
        return new VoucherEval(true, null, discount);
    }

    /**
     * Preview toàn bộ voucher đang hiệu lực của khách theo giỏ hàng hiện tại — phục vụ bước
     * "Ưu đãi" khi đặt vé: FE làm mờ mã không đủ điều kiện và hiển thị số giảm THỰC.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> previewActiveVouchers(VoucherPreviewRequest req) {
        Integer customerId = req.getCustomerId();
        if (customerId == null) return List.of();
        Customer customer = customerRepository.findById(customerId).orElse(null);

        BigDecimal seatSum = req.getSeatPrices() == null ? BigDecimal.ZERO
                : req.getSeatPrices().stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal orderTotal = seatSum.add(req.getFnbTotal() != null ? req.getFnbTotal() : BigDecimal.ZERO);

        List<Map<String, Object>> out = new ArrayList<>();
        for (Voucher v : voucherRepository.findActiveVouchersByCustomerId(customerId, LocalDateTime.now())) {
            Promotion p = v.getPromotion();
            VoucherEval eval = evaluate(customerId, customer, p, orderTotal, req.getMovieId(), req.getSeatPrices());
            BigDecimal shown = eval.discountAmount().min(orderTotal); // số giảm thực (không vượt tổng đơn)
            Map<String, Object> m = new HashMap<>();
            m.put("voucherId", v.getId());
            m.put("code", p.getCode() != null ? p.getCode() : "");
            m.put("applicable", eval.applicable());
            m.put("reason", eval.reason() != null ? eval.reason() : "");
            m.put("discountAmount", eval.applicable() ? shown : BigDecimal.ZERO);
            out.add(m);
        }
        return out;
    }

    /**
     * Lấy hồ sơ khách theo id; nếu user tồn tại nhưng chưa có Customer (vd tài khoản admin/staff
     * hoặc user seed) thì tự tạo hồ sơ BRONZE — giống {@code BookingService}.
     */
    private Customer resolveOrCreateCustomer(Integer customerId) {
        return customerRepository.findById(customerId).orElseGet(() -> {
            User u = userRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
            // @MapsId: chỉ set association, KHÔNG set userId (tránh merge)
            return customerRepository.save(Customer.builder()
                    .user(u)
                    .membershipTier("BRONZE")
                    .loyaltyPoints(0)
                    .build());
        });
    }

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

        // Mỗi mã chỉ được đổi 1 lần / khách — chặn TRƯỚC khi trừ điểm
        if (voucherRepository.existsByCustomerAndPromotion(customerId, promo.getId())) {
            throw new RuntimeException("Bạn đã đổi ưu đãi này rồi. Mỗi mã chỉ được đổi 1 lần.");
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
        Customer customer = resolveOrCreateCustomer(customerId);
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

        // Chặn sớm theo đối tượng áp dụng (khách mới / hạng thành viên) trước khi lưu mã
        assertEligibility(customerId, customer, promo);

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

        // Chặn sớm theo đối tượng áp dụng — chặn cả trường hợp khách đã lưu mã từ trước
        Customer customer = resolveOrCreateCustomer(customerId);
        assertEligibility(customerId, customer, promo);

        return voucherRepository.findActiveVoucherByCustomerAndCode(customerId, promo.getCode(), LocalDateTime.now())
                .orElseGet(() -> claimByCode(customerId, promo.getCode()));
    }
}
