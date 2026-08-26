package com.devcine.backend.config;

import com.devcine.backend.entity.Promotion;
import com.devcine.backend.entity.SystemSetting;
import com.devcine.backend.repository.PromotionRepository;
import com.devcine.backend.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeder một-lần cho các template đền bù sự cố phòng chiếu.
 *
 * <p>Tất cả template có {@code code} bắt đầu bằng {@code COMP_} và
 * sẽ được tự động load lên dropdown đền bù ở màn Xử lý sự cố
 * ({@code SeatIncidentService.listCompensationTemplates()}).
 *
 * <p>Cờ seed: {@code COMP_TEMPLATES_SEEDED_V1}.
 */
@Component
@Order(101) // Chạy sau FnbDataSeeder (Order 100)
@RequiredArgsConstructor
public class CompensationTemplateSeeder implements CommandLineRunner {

    private static final String SEED_FLAG = "COMP_TEMPLATES_SEEDED_V1";

    private final PromotionRepository promotionRepository;
    private final SystemSettingRepository systemSettingRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (systemSettingRepository.findById(SEED_FLAG).isPresent()) {
            return;
        }

        System.out.println("====== BẮT ĐẦU SEED TEMPLATE ĐỀN BÙ SỰ CỐ ======");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusYears(10);

        List<Promotion> templates = List.of(
                // ── GIFT_FNB: tặng combo F&B (giá trị quà = 0, hiển thị "Tặng combo F&B") ──
                Promotion.builder()
                        .code("COMP_FNB_COMBO")
                        .name("Tặng Combo F&B")
                        .description("Tặng 01 combo bắp + nước lớn cho khách bị ảnh hưởng sự cố.")
                        .discountType("GIFT_FNB")
                        .discountValue(BigDecimal.ZERO)
                        .isActive(false)   // Bật lên khi cần sử dụng
                        .isStackable(false)
                        .allowPointRedemption(false)
                        .minOrderValue(BigDecimal.ZERO)
                        .pointsRequired(0)
                        .usageLimit(0)
                        .usedCount(0)
                        .maxTicketQuantity(0)
                        .maxDiscountAmount(BigDecimal.ZERO)
                        .startDate(now)
                        .endDate(endDate)
                        .build(),

                // ── DISCOUNT: voucher giảm 50.000đ ──
                Promotion.builder()
                        .code("COMP_50K")
                        .name("Voucher giảm 50.000đ")
                        .description("Voucher trừ 50.000đ cho đơn F&B hoặc vé lần sau.")
                        .discountType("DISCOUNT")
                        .discountValue(new BigDecimal("50000"))
                        .isActive(false)
                        .isStackable(false)
                        .allowPointRedemption(false)
                        .minOrderValue(BigDecimal.ZERO)
                        .pointsRequired(0)
                        .usageLimit(0)
                        .usedCount(0)
                        .maxTicketQuantity(0)
                        .maxDiscountAmount(BigDecimal.ZERO)
                        .startDate(now)
                        .endDate(endDate)
                        .build(),

                // ── DISCOUNT: voucher giảm 100.000đ ──
                Promotion.builder()
                        .code("COMP_100K")
                        .name("Voucher giảm 100.000đ")
                        .description("Voucher trừ 100.000đ cho đơn F&B hoặc vé lần sau.")
                        .discountType("DISCOUNT")
                        .discountValue(new BigDecimal("100000"))
                        .isActive(false)
                        .isStackable(false)
                        .allowPointRedemption(false)
                        .minOrderValue(BigDecimal.ZERO)
                        .pointsRequired(0)
                        .usageLimit(0)
                        .usedCount(0)
                        .maxTicketQuantity(0)
                        .maxDiscountAmount(BigDecimal.ZERO)
                        .startDate(now)
                        .endDate(endDate)
                        .build(),

                // ── GIFT_TICKET: đền vé nguyên giá (chỉ hiển thị ở luồng HỦY CHO) ──
                Promotion.builder()
                        .code("COMP_TICKET_FULL")
                        .name("Đền vé nguyên giá")
                        .description("Voucher giá trị tương đương giá vé gốc — chỉ dùng khi HỦY CHO ghế hỏng.")
                        .discountType("GIFT_TICKET")
                        .discountValue(BigDecimal.ZERO)
                        .isActive(false)
                        .isStackable(false)
                        .allowPointRedemption(false)
                        .minOrderValue(BigDecimal.ZERO)
                        .pointsRequired(0)
                        .usageLimit(0)
                        .usedCount(0)
                        .maxTicketQuantity(0)
                        .maxDiscountAmount(BigDecimal.ZERO)
                        .startDate(now)
                        .endDate(endDate)
                        .build()
        );

        for (Promotion t : templates) {
            if (!promotionRepository.existsByCodeIgnoreCase(t.getCode())) {
                promotionRepository.save(t);
                System.out.println("  Đã tạo template: " + t.getCode() + " — " + t.getName());
            } else {
                System.out.println("  Bỏ qua (đã tồn tại): " + t.getCode());
            }
        }

        systemSettingRepository.save(SystemSetting.builder()
                .settingKey(SEED_FLAG).settingValue("true").build());
        System.out.println("====== SEED TEMPLATE ĐỀN BÙ XONG ======");
    }
}
