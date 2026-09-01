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
    private final com.devcine.backend.repository.MovieRepository movieRepository;

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


    /**
     * Tra cứu tiêu đề phim an toàn theo ID (tránh NPE).
     */
    public String getMovieTitleById(Integer movieId) {
        if (movieId == null) return null;
        return movieRepository.findById(movieId).map(com.devcine.backend.entity.Movie::getTitle).orElse(null);
    }

    /**
     * Tra cứu danh sách tên các phim áp dụng từ Promotion.
     */
    public String getMovieTitles(Promotion promo) {
        if (promo == null) return null;
        List<Integer> ids = promo.getApplicableMovieIdList();
        if (ids.isEmpty()) return null;
        List<String> titles = new ArrayList<>();
        for (Integer id : ids) {
            String t = getMovieTitleById(id);
            if (t != null && !t.isBlank()) {
                titles.add(t);
            }
        }
        return titles.isEmpty() ? null : String.join(", ", titles);
    }

    /** Kết quả chấm một voucher theo giỏ hàng. reason = null khi đủ điều kiện. discountAmount = số giảm THÔ. */
    public record VoucherEval(boolean applicable, String reason, BigDecimal discountAmount) {}

    /**
     * Lý do KHÔNG đủ điều kiện theo ĐỐI TƯỢNG áp dụng (null nếu đủ). Dùng chung cho apply/claim,
     * preview và (gián tiếp) đặt vé: NEW_CUSTOMER (chưa từng mua vé) & TIER_* (hạng tối thiểu).
     */
    public String eligibilityReason(Integer customerId, Customer customer, String elig) {
        if (elig == null || elig.equalsIgnoreCase("ALL")) return null;
        if ("NEW_CUSTOMER".equalsIgnoreCase(elig)) {
            if (customerId != null && bookingRepository.countConfirmedByCustomer(customerId) > 0) {
                return "Chỉ dành cho khách hàng mới";
            }
        } else if (elig.startsWith("TIER_")) {
            String requiredTier = elig.substring(5); // SILVER | GOLD | PLATINUM
            int lifetime = customer != null && customer.getLifetimePoints() != null ? customer.getLifetimePoints() : 0;
            if (loyaltyService.tierRank(loyaltyService.tierFor(lifetime)) < loyaltyService.tierRank(requiredTier)) {
                return "Chỉ dành cho thành viên " + loyaltyService.tierLabelVi(requiredTier) + " trở lên";
            }
        }
        return null;
    }

    public String eligibilityReason(Integer customerId, Customer customer, Promotion promo) {
        return eligibilityReason(customerId, customer, promo != null ? promo.getCustomerEligibility() : "ALL");
    }

    /** Chặn sớm theo đối tượng áp dụng ngay ở bước áp/lưu mã (ném lỗi nếu không đủ). */
    private void assertEligibility(Integer customerId, Customer customer, Promotion promo) {
        String reason = eligibilityReason(customerId, customer, promo);
        if (reason != null) throw new RuntimeException(reason);
    }

    /**
     * Chấm một voucher theo ngữ cảnh giỏ hàng — NGUỒN SỰ THẬT DUY NHẤT dùng chung với
     * {@code BookingService} để preview khớp với lúc đặt vé.
     *
     * <p><b>Thứ tự kiểm tra (Pipeline Priority chuẩn Lotte Cinema / CGV):</b>
     * 1. Điều kiện CỐ ĐỊNH (Hard checks): Active → Ngày áp dụng/Hạn sử dụng → Hết lượt toàn sàn → Đối tượng → Phim.
     * 2. Điều kiện ĐỘNG (Soft check): Giá trị đơn tối thiểu (khách có thể mua thêm vé/combo để đạt).
     * </p>
     *
     * @param voucher    voucher cần chấm (chứa snapshot thông số giảm)
     * @param orderTotal tổng tiền đơn (ghế + bắp nước)
     * @param movieId    phim của suất đang đặt (null = bỏ qua kiểm theo phim)
     * @param seatPrices giá từng ghế — để tính base khi mã giới hạn số vé
     * @return số giảm THÔ (chưa kẹp về 0/tổng đơn); caller tự kẹp finalPrice.
     */
    public VoucherEval evaluate(Integer customerId, Customer customer, Voucher voucher,
                                BigDecimal orderTotal, Integer movieId, List<BigDecimal> seatPrices) {
        Promotion promo = voucher.getPromotion();
        if (promo == null) {
            return new VoucherEval(false, "Không tìm thấy chương trình ưu đãi.", BigDecimal.ZERO);
        }

        // 1. Thời hạn áp dụng của Voucher (Snapshot)
        // Hạn sử dụng: ưu tiên validUntil của Voucher (snapshot tại thời điểm cấp phát).
        // Chỉ fallback về promo.endDate nếu voucher chưa có validUntil (dữ liệu cũ / guest).
        // Khi Admin tạm dừng/sửa chiến dịch sau đó, các voucher ĐÃ trong ví khách vẫn có hiệu lực đến validUntil.
        LocalDateTime effectiveExpiry = voucher.getValidUntil() != null
                ? voucher.getValidUntil()
                : promo.getEndDate();
        LocalDateTime now = LocalDateTime.now();
        if (effectiveExpiry != null && effectiveExpiry.isBefore(now)) {
            return new VoucherEval(false, "Mã ưu đãi đã hết hạn sử dụng.", BigDecimal.ZERO);
        }

        // 2. Trạng thái hoạt động của chiến dịch
        // Với voucher ĐÃ TRONG VÍ khách hàng (voucher.getId() != null hoặc voucher.getCustomer() != null),
        // khách đã sở hữu voucher có hạn dùng validUntil riêng → vẫn được áp dụng theo snapshot.
        // Chỉ chặn khi chưa cấp phát (guest hoặc claim mã mới khi promo đang tạm dừng).
        if (Boolean.FALSE.equals(promo.getIsActive()) && (voucher.getId() == null || voucher.getCustomer() == null)) {
            return new VoucherEval(false, "Mã ưu đãi đang tạm dừng áp dụng.", BigDecimal.ZERO);
        }

        // 3. Lượt sử dụng toàn hệ thống (Hết lượt)
        if (promo.getUsageLimit() != null && promo.getUsageLimit() > 0
                && promo.getUsedCount() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            return new VoucherEval(false, "Mã ưu đãi đã hết lượt sử dụng.", BigDecimal.ZERO);
        }

        // 4. Đối tượng áp dụng (Hard check - đọc từ SNAPSHOT của voucher)
        String eligReason = eligibilityReason(customerId, customer, voucher.effectiveCustomerEligibility());
        if (eligReason != null) return new VoucherEval(false, eligReason, BigDecimal.ZERO);

        // 5. Phim áp dụng (Hard check - đọc từ SNAPSHOT của voucher, hỗ trợ đa phim)
        if (movieId != null && !voucher.isMovieApplicable(movieId)) {
            String titles = voucher.effectiveApplicableMovieTitle();
            if (titles == null || titles.isBlank()) {
                List<Integer> ids = voucher.effectiveApplicableMovieIdList();
                List<String> titleList = new ArrayList<>();
                for (Integer id : ids) {
                    String t = getMovieTitleById(id);
                    if (t != null && !t.isBlank()) titleList.add(t);
                }
                titles = String.join(", ", titleList);
            }
            String reason = (titles != null && !titles.isBlank())
                    ? "Chỉ áp dụng cho phim: " + titles
                    : "Mã chỉ áp dụng cho phim khác, không dùng được cho suất này.";
            return new VoucherEval(false, reason, BigDecimal.ZERO);
        }

        // 6. Giá trị đơn tối thiểu (Soft check: kiểm tra sau cùng để báo chính xác số tiền còn thiếu)
        BigDecimal minOrder = voucher.effectiveMinOrderValue();
        if (minOrder != null && minOrder.compareTo(BigDecimal.ZERO) > 0 && orderTotal.compareTo(minOrder) < 0) {
            BigDecimal missing = minOrder.subtract(orderTotal);
            return new VoucherEval(false,
                    "Chưa đạt đơn tối thiểu " + minOrder.toBigInteger() + "đ (còn thiếu " + missing.toBigInteger() + "đ)", BigDecimal.ZERO);
        }

        // ═══ THÔNG SỐ GIẢM GIÁ: đọc từ SNAPSHOT (đóng băng lúc phát voucher) ═══
        // Voucher cũ (trước fix) chưa có snapshot → fallback Promotion LIVE qua effective*().
        String discountType = voucher.effectiveDiscountType();
        BigDecimal discountValue = voucher.effectiveDiscountValue();
        BigDecimal maxDiscAmt = voucher.effectiveMaxDiscountAmount();
        Integer maxTk = voucher.effectiveMaxTicketQuantity();

        // Base tính giảm: mặc định cả đơn; nếu giới hạn số vé → chỉ X vé ĐẮT NHẤT
        BigDecimal base = orderTotal;
        if (maxTk != null && maxTk > 0 && seatPrices != null && !seatPrices.isEmpty()) {
            base = seatPrices.stream().filter(Objects::nonNull)
                    .sorted(Comparator.reverseOrder()).limit(maxTk)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        BigDecimal discount = BigDecimal.ZERO;
        if ("PERCENTAGE".equalsIgnoreCase(discountType)) {
            discount = base.multiply(discountValue).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else if ("FIXED_AMOUNT".equalsIgnoreCase(discountType)) {
            discount = discountValue.min(base);
        }
        if (maxDiscAmt != null && maxDiscAmt.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(maxDiscAmt) > 0) {
            discount = maxDiscAmt;
        }
        return new VoucherEval(true, null, discount);
    }

    /**
     * Đảm bảo voucher đã có snapshot thông số giảm giá. Nếu snapshot rỗng (voucher cũ
     * tạo trước khi có cơ chế snapshot), tự đóng băng từ Promotion hiện tại.
     * Lazy migration — chỉ chạy 1 lần duy nhất cho mỗi voucher cũ.
     * Public để BookingService cũng có thể gọi trước evaluate().
     */
    public void ensureSnapshotPublic(Voucher v) {
        if (v.getDiscountValueSnapshot() == null || v.getMinOrderValueSnapshot() == null || v.getTitleSnapshot() == null) {
            Promotion promo = v.getPromotion();
            if (promo != null) {
                String movieTitles = getMovieTitles(promo);
                v.snapshotFrom(promo, movieTitles, promo.getApplicableMovieIds());
                voucherRepository.save(v);
            }
        }
    }

    private void ensureSnapshot(Voucher v) { ensureSnapshotPublic(v); }

    /**
     * Phân loại lý do không hợp lệ:
     * Giữ lại phương thức để tương thích ngược nếu cần.
     */
    public boolean shouldHideFromUI(String reason) {
        return false;
    }

    /**
     * Preview toàn bộ voucher đang hiệu lực của khách theo giỏ hàng hiện tại — phục vụ bước
     * "Ưu đãi" khi đặt vé. Trả về đầy đủ thông tin để FE phân chia thành 2 khu vực:
     * "Voucher khả dụng" và "Ưu đãi chưa đủ điều kiện" kèm gợi ý thông minh.
     */
    @Transactional
    public List<Map<String, Object>> previewActiveVouchers(VoucherPreviewRequest req) {
        Integer customerId = req.getCustomerId();
        if (customerId == null) return List.of();
        Customer customer = customerRepository.findById(customerId).orElse(null);

        BigDecimal seatSum = req.getSeatPrices() == null ? BigDecimal.ZERO
                : req.getSeatPrices().stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal orderTotal = seatSum.add(req.getFnbTotal() != null ? req.getFnbTotal() : BigDecimal.ZERO);

        List<Map<String, Object>> out = new ArrayList<>();
        for (Voucher v : voucherRepository.findActiveVouchersByCustomerId(customerId, LocalDateTime.now())) {
            ensureSnapshot(v); // Lazy migration: đóng băng snapshot cho voucher cũ
            Promotion p = v.getPromotion();
            VoucherEval eval = evaluate(customerId, customer, v, orderTotal, req.getMovieId(), req.getSeatPrices());
            BigDecimal shown = eval.discountAmount().min(orderTotal); // số giảm thực (không vượt tổng đơn)

            Integer applicableMovieId = v.effectiveApplicableMovieId();
            String movieTitle = v.effectiveApplicableMovieTitle();
            if (movieTitle == null) {
                if (applicableMovieId != null) {
                    movieTitle = getMovieTitleById(applicableMovieId);
                } else if (p != null) {
                    movieTitle = getMovieTitles(p);
                }
            }

            Map<String, Object> m = new HashMap<>();
            m.put("voucherId", v.getId());
            m.put("code", p != null && p.getCode() != null ? p.getCode() : "");
            m.put("title", v.effectiveTitle() != null ? v.effectiveTitle() : "");
            m.put("description", v.effectiveDescription() != null ? v.effectiveDescription() : "");
            m.put("applicable", eval.applicable());
            m.put("reason", eval.reason() != null ? eval.reason() : "");
            m.put("discountAmount", eval.applicable() ? shown : BigDecimal.ZERO);
            m.put("discountType", v.effectiveDiscountType() != null ? v.effectiveDiscountType() : "");
            m.put("discountValue", v.effectiveDiscountValue() != null ? v.effectiveDiscountValue() : BigDecimal.ZERO);
            m.put("minOrderValue", v.effectiveMinOrderValue() != null ? v.effectiveMinOrderValue() : BigDecimal.ZERO);
            m.put("maxDiscountAmount", v.effectiveMaxDiscountAmount() != null ? v.effectiveMaxDiscountAmount() : BigDecimal.ZERO);
            m.put("maxTicketQuantity", v.effectiveMaxTicketQuantity() != null ? v.effectiveMaxTicketQuantity() : 0);
            m.put("validUntil", v.getValidUntil() != null ? v.getValidUntil().toString() : "");
            m.put("applicableMovieId", applicableMovieId);
            m.put("applicableMovieTitle", movieTitle);
            m.put("applicableMovieIds", v.effectiveApplicableMovieIds());
            m.put("hideFromUI", false);
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

        if (Boolean.FALSE.equals(promo.getIsActive())) {
            throw new RuntimeException("Ưu đãi này đang tạm dừng áp dụng.");
        }
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

        String movieTitles = getMovieTitles(promo);
        Voucher voucher = Voucher.builder()
                .promotion(promo)
                .customer(customer)
                .isUsed(false)
                .validUntil(promo.getEndDate() != null ? promo.getEndDate() : now.plusMonths(1))
                .build();
        voucher.snapshotFrom(promo, movieTitles, promo.getApplicableMovieIds()); // Đóng băng thông số giảm giá tại thời điểm đổi
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

        if (Boolean.FALSE.equals(promo.getIsActive())) {
            throw new RuntimeException("Mã ưu đãi đang tạm dừng áp dụng.");
        }

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

        // Chặn sớm khi mã đã hết lượt toàn hệ thống
        if (promo.getUsageLimit() != null && promo.getUsageLimit() > 0
                && promo.getUsedCount() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            throw new RuntimeException("Mã ưu đãi đã hết lượt sử dụng.");
        }

        // Chặn sớm theo đối tượng áp dụng (khách mới / hạng thành viên) trước khi lưu mã
        assertEligibility(customerId, customer, promo);

        if (voucherRepository.findActiveVoucherByCustomerAndCode(customerId, promo.getCode(), now).isPresent()) {
            throw new RuntimeException("Bạn đã lưu mã này rồi.");
        }

        String movieTitles = getMovieTitles(promo);
        Voucher voucher = Voucher.builder()
                .promotion(promo)
                .customer(customer)
                .isUsed(false)
                .validUntil(promo.getEndDate() != null ? promo.getEndDate() : now.plusMonths(1))
                .build();
        voucher.snapshotFrom(promo, movieTitles, promo.getApplicableMovieIds()); // Đóng băng thông số giảm giá tại thời điểm lưu mã
        voucherRepository.save(voucher);

        log.info("Khách #{} lưu voucher bằng mã '{}' (promotion #{})", customerId, promo.getCode(), promo.getId());
        return voucher;
    }

    /**
     * Admin phát voucher cho khách hàng.
     */
    @Transactional
    public Voucher issueVoucher(Promotion promo, Customer customer, LocalDateTime validUntil) {
        String movieTitles = getMovieTitles(promo);
        Voucher voucher = Voucher.builder()
                .promotion(promo)
                .customer(customer)
                .isUsed(false)
                .validUntil(validUntil != null ? validUntil : (promo.getEndDate() != null ? promo.getEndDate() : LocalDateTime.now().plusMonths(1)))
                .build();
        voucher.snapshotFrom(promo, movieTitles, promo.getApplicableMovieIds());
        return voucherRepository.save(voucher);
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

        Voucher existing = voucherRepository.findActiveVoucherByCustomerAndCode(customerId, promo.getCode(), LocalDateTime.now())
                .orElse(null);
        if (existing != null) {
            ensureSnapshot(existing); // Lazy migration: đóng băng snapshot cho voucher cũ
            if (Boolean.FALSE.equals(promo.getIsActive())) {
                throw new RuntimeException("Mã ưu đãi đang tạm dừng áp dụng.");
            }
            if (promo.getUsageLimit() != null && promo.getUsageLimit() > 0
                    && promo.getUsedCount() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
                throw new RuntimeException("Mã ưu đãi đã hết lượt sử dụng.");
            }
            return existing;
        }

        // Khách chưa sở hữu -> chỉ cho claim mới khi promotion đang active
        if (Boolean.FALSE.equals(promo.getIsActive())) {
            throw new RuntimeException("Mã ưu đãi đang tạm dừng áp dụng.");
        }
        return claimByCode(customerId, promo.getCode());
    }
}
