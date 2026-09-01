package com.devcine.backend.service;

import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.entity.Promotion;
import com.devcine.backend.entity.Voucher;
import com.devcine.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherComprehensiveTest {

    @Mock PromotionRepository promotionRepository;
    @Mock CustomerRepository customerRepository;
    @Mock VoucherRepository voucherRepository;
    @Mock UserRepository userRepository;
    @Mock BookingRepository bookingRepository;
    @Mock LoyaltyService loyaltyService;
    @Mock MailService mailService;
    @Mock PromoEmailLogRepository promoEmailLogRepository;
    @Mock MovieRepository movieRepository;

    @InjectMocks VoucherService voucherService;

    private Customer sampleCustomer;
    private Movie movieA;
    private Movie movieB;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setUserId(1);
        sampleCustomer.setMembershipTier("BRONZE");
        sampleCustomer.setLifetimePoints(0);

        movieA = new Movie();
        movieA.setId(10);
        movieA.setTitle("Conan: Ngôi Sao 5 Cánh 1 Triệu Đô");

        movieB = new Movie();
        movieB.setId(20);
        movieB.setTitle("Doraemon: Bản Giao Hưởng Địa Cầu");
    }

    // ==========================================
    // NHÓM 1: Voucher Riêng Tư (isHidden = true)
    // ==========================================
    @Test
    @DisplayName("TC-PRI: Voucher Riêng tư có thể claim bằng code và đánh giá thành công")
    void testPrivateVoucherClaimAndEvaluate() {
        Promotion promo = new Promotion();
        promo.setId(100);
        promo.setCode("SECRET50");
        promo.setName("Ưu đãi riêng tư 50k");
        promo.setIsHidden(true);
        promo.setIsActive(true);
        promo.setDiscountType("FIXED_AMOUNT");
        promo.setDiscountValue(new BigDecimal("50000"));
        promo.setMinOrderValue(new BigDecimal("100000"));

        when(promotionRepository.findByCodeIgnoreCase("SECRET50")).thenReturn(Optional.of(promo));
        when(customerRepository.findById(1)).thenReturn(Optional.of(sampleCustomer));
        when(voucherRepository.findActiveVoucherByCustomerAndCode(eq(1), eq("SECRET50"), any())).thenReturn(Optional.empty());
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Voucher claimed = voucherService.claimByCode(1, "SECRET50");
        assertNotNull(claimed);
        assertEquals("SECRET50", claimed.getPromotion().getCode());

        // Đánh giá voucher trên đơn 150.000đ
        VoucherService.VoucherEval eval = voucherService.evaluate(1, sampleCustomer, claimed, new BigDecimal("150000"), null, null);
        assertTrue(eval.applicable());
        assertEquals(new BigDecimal("50000"), eval.discountAmount());
    }

    // ==========================================================
    // NHÓM 2: Voucher Đa Phim (applicableMovieIds) & Snapshot
    // ==========================================================
    @Test
    @DisplayName("TC-MOV-01: Áp dụng đúng phim trong danh sách đa phim thành công")
    void testMultiMovieApplicableSuccess() {
        Promotion promo = new Promotion();
        promo.setId(200);
        promo.setCode("COMBO2P");
        promo.setIsActive(true);
        promo.setDiscountType("PERCENTAGE");
        promo.setDiscountValue(new BigDecimal("20"));
        promo.setApplicableMovieIds("10,20");

        Voucher voucher = new Voucher();
        voucher.setPromotion(promo);
        voucher.snapshotFrom(promo, "Conan, Doraemon", "10,20");

        // Đặt vé cho phim ID = 20 (Doraemon)
        VoucherService.VoucherEval eval = voucherService.evaluate(1, sampleCustomer, voucher, new BigDecimal("200000"), 20, null);
        assertTrue(eval.applicable());
        assertEquals(new BigDecimal("40000.00"), eval.discountAmount());
    }

    @Test
    @DisplayName("TC-MOV-02: Áp dụng sai phim bị từ chối kèm danh sách tên phim")
    void testMultiMovieApplicableMismatch() {
        Promotion promo = new Promotion();
        promo.setId(200);
        promo.setCode("COMBO2P");
        promo.setIsActive(true);
        promo.setDiscountType("PERCENTAGE");
        promo.setDiscountValue(new BigDecimal("20"));
        promo.setApplicableMovieIds("10,20");

        Voucher voucher = new Voucher();
        voucher.setPromotion(promo);
        voucher.snapshotFrom(promo, "Conan, Doraemon", "10,20");

        // Đặt vé cho phim ID = 99 (Phim khác)
        VoucherService.VoucherEval eval = voucherService.evaluate(1, sampleCustomer, voucher, new BigDecimal("200000"), 99, null);
        assertFalse(eval.applicable());
        assertTrue(eval.reason().contains("Chỉ áp dụng cho phim: Conan, Doraemon"));
    }

    @Test
    @DisplayName("TC-MOV-04: Snapshot bảo toàn danh sách phim khi Admin sửa đổi Promotion sau đó")
    void testMultiMovieSnapshotPreservedWhenPromotionChanges() {
        Promotion promo = new Promotion();
        promo.setId(300);
        promo.setCode("SNAPTEST");
        promo.setIsActive(true);
        promo.setDiscountType("FIXED_AMOUNT");
        promo.setDiscountValue(new BigDecimal("30000"));
        promo.setApplicableMovieIds("10,20");

        // Khách nhận voucher -> snapshot được tạo cho Phim 10 & 20
        Voucher voucher = new Voucher();
        voucher.setId(1);
        voucher.setCustomer(sampleCustomer);
        voucher.setPromotion(promo);
        voucher.snapshotFrom(promo, "Conan, Doraemon", "10,20");

        // Sau đó Admin sửa Promotion thành chỉ áp dụng cho phim 99
        promo.setApplicableMovieIds("99");

        // Khách đặt vé xem phim 10 -> Voucher vẫn hợp lệ do đọc từ snapshot
        VoucherService.VoucherEval eval = voucherService.evaluate(1, sampleCustomer, voucher, new BigDecimal("100000"), 10, null);
        assertTrue(eval.applicable());
        assertEquals(new BigDecimal("30000"), eval.discountAmount());
    }

    // ==========================================================
    // NHÓM 3: Loại giảm giá & Trần giảm tối đa (maxDiscountAmount)
    // ==========================================================
    @Test
    @DisplayName("TC-VAL: Giảm % bị chặn trần tối đa")
    void testPercentageDiscountWithCap() {
        Promotion promo = new Promotion();
        promo.setIsActive(true);
        promo.setDiscountType("PERCENTAGE");
        promo.setDiscountValue(new BigDecimal("20")); // 20%
        promo.setMaxDiscountAmount(new BigDecimal("50000")); // Trần 50k

        Voucher voucher = new Voucher();
        voucher.setPromotion(promo);

        // Đơn 400.000đ -> 20% = 80.000đ -> Bị chặn trần còn 50.000đ
        VoucherService.VoucherEval eval = voucherService.evaluate(1, sampleCustomer, voucher, new BigDecimal("400000"), null, null);
        assertTrue(eval.applicable());
        assertEquals(new BigDecimal("50000"), eval.discountAmount());
    }

    // ==========================================================
    // NHÓM 4: Giới hạn số vé được giảm (maxTicketQuantity)
    // ==========================================================
    @Test
    @DisplayName("TC-TKT: Giới hạn số vé ưu tiên áp dụng cho các vé đắt nhất")
    void testMaxTicketQuantityTakesMostExpensive() {
        Promotion promo = new Promotion();
        promo.setIsActive(true);
        promo.setDiscountType("PERCENTAGE");
        promo.setDiscountValue(new BigDecimal("50")); // Giảm 50%
        promo.setMaxTicketQuantity(2); // Tối đa 2 vé

        Voucher voucher = new Voucher();
        voucher.setPromotion(promo);

        // Mua 4 vé: 2 vé VIP (100k) + 2 vé thường (80k)
        List<BigDecimal> seatPrices = List.of(
                new BigDecimal("80000"),
                new BigDecimal("100000"),
                new BigDecimal("80000"),
                new BigDecimal("100000")
        );
        BigDecimal orderTotal = new BigDecimal("360000");

        // Base tính giảm = 2 vé đắt nhất = 100k + 100k = 200k -> Giảm 50% = 100k
        VoucherService.VoucherEval eval = voucherService.evaluate(1, sampleCustomer, voucher, orderTotal, null, seatPrices);
        assertTrue(eval.applicable());
        assertEquals(new BigDecimal("100000.00"), eval.discountAmount());
    }

    // ==========================================================
    // NHÓM 5: Điều kiện đơn tối thiểu (minOrderValue)
    // ==========================================================
    @Test
    @DisplayName("TC-MIN: Báo lỗi chính xác số tiền còn thiếu khi chưa đạt đơn tối thiểu")
    void testMinOrderValueCheck() {
        Promotion promo = new Promotion();
        promo.setIsActive(true);
        promo.setDiscountType("FIXED_AMOUNT");
        promo.setDiscountValue(new BigDecimal("20000"));
        promo.setMinOrderValue(new BigDecimal("150000"));

        Voucher voucher = new Voucher();
        voucher.setPromotion(promo);

        // Đơn 120.000đ (thiếu 30.000đ)
        VoucherService.VoucherEval eval = voucherService.evaluate(1, sampleCustomer, voucher, new BigDecimal("120000"), null, null);
        assertFalse(eval.applicable());
        assertTrue(eval.reason().contains("còn thiếu 30000đ"));
    }

    // ==========================================================
    // NHÓM 7: Voucher đổi điểm (allowPointRedemption)
    // ==========================================================
    @Test
    @DisplayName("TC-PNT: Chặn người dùng claim trực tiếp voucher đổi điểm nếu chưa đổi")
    void testAllowPointRedemptionBlockedFromDirectClaim() {
        Promotion promo = new Promotion();
        promo.setId(400);
        promo.setCode("POINT100");
        promo.setIsActive(true);
        promo.setAllowPointRedemption(true);
        promo.setPointsRequired(100);

        when(promotionRepository.findByCodeIgnoreCase("POINT100")).thenReturn(Optional.of(promo));
        when(customerRepository.findById(1)).thenReturn(Optional.of(sampleCustomer));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> voucherService.claimByCode(1, "POINT100"));
        assertTrue(ex.getMessage().contains("chỉ có thể đổi bằng điểm tích luỹ"));
    }

    // ==========================================================
    // NHÓM 8: Giới hạn lượt dùng toàn hệ thống (usageLimit)
    // ==========================================================
    @Test
    @DisplayName("TC-LMT: Voucher hết lượt dùng toàn hệ thống bị từ chối")
    void testUsageLimitReached() {
        Promotion promo = new Promotion();
        promo.setIsActive(true);
        promo.setDiscountType("FIXED_AMOUNT");
        promo.setDiscountValue(new BigDecimal("20000"));
        promo.setUsageLimit(10);
        promo.setUsedCount(10); // Đã dùng hết 10/10

        Voucher voucher = new Voucher();
        voucher.setPromotion(promo);

        VoucherService.VoucherEval eval = voucherService.evaluate(1, sampleCustomer, voucher, new BigDecimal("100000"), null, null);
        assertFalse(eval.applicable());
        assertTrue(eval.reason().contains("hết lượt sử dụng"));
    }

    // ==========================================================
    // NHÓM 6: Phân loại đối tượng khách hàng (customerEligibility)
    // ==========================================================
    @Test
    @DisplayName("TC-CUS: Voucher chỉ dành cho VIP/PLATINUM từ chối khách thường")
    void testCustomerEligibilityFilter() {
        Promotion promo = new Promotion();
        promo.setIsActive(true);
        promo.setDiscountType("FIXED_AMOUNT");
        promo.setDiscountValue(new BigDecimal("20000"));
        promo.setCustomerEligibility("TIER_PLATINUM");

        when(loyaltyService.tierFor(0)).thenReturn("BRONZE");
        when(loyaltyService.tierRank("BRONZE")).thenReturn(1);
        when(loyaltyService.tierRank("PLATINUM")).thenReturn(4);
        when(loyaltyService.tierLabelVi("PLATINUM")).thenReturn("Bạch Kim");

        Voucher voucher = new Voucher();
        voucher.setPromotion(promo);

        // Khách có tier BRONZE
        VoucherService.VoucherEval eval = voucherService.evaluate(1, sampleCustomer, voucher, new BigDecimal("100000"), null, null);
        assertFalse(eval.applicable());
        assertTrue(eval.reason().contains("Chỉ dành cho thành viên Bạch Kim trở lên"));
    }

    @Test
    @DisplayName("TC-CUS-NEW: Voucher khách hàng mới từ chối khách đã có đơn")
    void testNewCustomerEligibilityFilter() {
        Promotion promo = new Promotion();
        promo.setIsActive(true);
        promo.setDiscountType("FIXED_AMOUNT");
        promo.setDiscountValue(new BigDecimal("20000"));
        promo.setCustomerEligibility("NEW_CUSTOMER");

        when(bookingRepository.countConfirmedByCustomer(1)).thenReturn(2L);

        Voucher voucher = new Voucher();
        voucher.setPromotion(promo);

        VoucherService.VoucherEval eval = voucherService.evaluate(1, sampleCustomer, voucher, new BigDecimal("100000"), null, null);
        assertFalse(eval.applicable());
        assertTrue(eval.reason().contains("Chỉ dành cho khách hàng mới"));
    }
}
