package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    // Thời điểm voucher được sử dụng (ghi khi đặt vé áp dụng voucher). Null nếu chưa dùng
    // hoặc là voucher đã dùng từ trước khi có cột này (dữ liệu cũ).
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    // ═══ SNAPSHOT đóng băng toàn bộ thông số ưu đãi tại thời điểm voucher được phát ═══
    // Khi Admin sửa Promotion sau đó, các voucher đã phát KHÔNG bị ảnh hưởng.
    // Null = voucher cũ (trước khi có snapshot) → fallback về Promotion LIVE.

    /** Tiêu đề / tên khuyến mãi snapshot (vd: "Ưu đãi hè 2026") */
    @Column(name = "title_snapshot", length = 255)
    private String titleSnapshot;

    /** Mô tả chi tiết khuyến mãi snapshot */
    @Column(name = "description_snapshot", length = 500)
    private String descriptionSnapshot;

    /** Loại giảm giá snapshot: PERCENTAGE | FIXED_AMOUNT */
    @Column(name = "discount_type_snapshot", length = 20)
    private String discountTypeSnapshot;

    /** Giá trị giảm snapshot (% hoặc số tiền) */
    @Column(name = "discount_value_snapshot", precision = 15, scale = 2)
    private BigDecimal discountValueSnapshot;

    /** Trần giảm tối đa snapshot (0/null = không giới hạn) */
    @Column(name = "max_discount_amount_snapshot", precision = 15, scale = 2)
    private BigDecimal maxDiscountAmountSnapshot;

    /** Số vé tối đa được giảm snapshot (0/null = toàn đơn) */
    @Column(name = "max_ticket_qty_snapshot")
    private Integer maxTicketQuantitySnapshot;

    /** Giá trị đơn tối thiểu snapshot (0/null = không yêu cầu) */
    @Column(name = "min_order_value_snapshot", precision = 15, scale = 2)
    private BigDecimal minOrderValueSnapshot;

    /** ID phim áp dụng snapshot (null = áp dụng mọi phim) */
    @Column(name = "applicable_movie_id_snapshot")
    private Integer applicableMovieIdSnapshot;

    /** Tên phim áp dụng snapshot (đóng băng tên phim tại thời điểm phát) */
    @Column(name = "applicable_movie_title_snapshot", length = 255)
    private String applicableMovieTitleSnapshot;

    /** Đối tượng áp dụng snapshot: ALL | NEW_CUSTOMER | TIER_SILVER | ... */
    @Column(name = "customer_eligibility_snapshot", length = 20)
    private String customerEligibilitySnapshot;

    /** Copy toàn bộ thông số từ Promotion vào snapshot — gọi khi tạo voucher. */
    public void snapshotFrom(Promotion promo) {
        snapshotFrom(promo, null);
    }

    public void snapshotFrom(Promotion promo, String movieTitle) {
        if (promo == null) return;
        this.titleSnapshot = promo.getName();
        this.descriptionSnapshot = promo.getDescription();
        this.discountTypeSnapshot = promo.getDiscountType();
        this.discountValueSnapshot = promo.getDiscountValue();
        this.maxDiscountAmountSnapshot = promo.getMaxDiscountAmount();
        this.maxTicketQuantitySnapshot = promo.getMaxTicketQuantity();
        this.minOrderValueSnapshot = promo.getMinOrderValue();
        this.applicableMovieIdSnapshot = promo.getApplicableMovieId();
        this.applicableMovieTitleSnapshot = movieTitle;
        this.customerEligibilitySnapshot = promo.getCustomerEligibility();
    }

    // ═══ Getter an toàn: ưu tiên snapshot, fallback Promotion LIVE ═══

    public String effectiveTitle() {
        if (titleSnapshot != null && !titleSnapshot.isBlank()) return titleSnapshot;
        return promotion != null ? promotion.getName() : null;
    }

    public String effectiveDescription() {
        if (descriptionSnapshot != null && !descriptionSnapshot.isBlank()) return descriptionSnapshot;
        return promotion != null ? promotion.getDescription() : null;
    }

    public String effectiveDiscountType() {
        return discountTypeSnapshot != null ? discountTypeSnapshot : (promotion != null ? promotion.getDiscountType() : null);
    }

    public BigDecimal effectiveDiscountValue() {
        return discountValueSnapshot != null ? discountValueSnapshot : (promotion != null ? promotion.getDiscountValue() : BigDecimal.ZERO);
    }

    public BigDecimal effectiveMaxDiscountAmount() {
        return maxDiscountAmountSnapshot != null ? maxDiscountAmountSnapshot : (promotion != null ? promotion.getMaxDiscountAmount() : BigDecimal.ZERO);
    }

    public Integer effectiveMaxTicketQuantity() {
        return maxTicketQuantitySnapshot != null ? maxTicketQuantitySnapshot : (promotion != null ? promotion.getMaxTicketQuantity() : 0);
    }

    public BigDecimal effectiveMinOrderValue() {
        return minOrderValueSnapshot != null ? minOrderValueSnapshot : (promotion != null ? promotion.getMinOrderValue() : BigDecimal.ZERO);
    }

    public Integer effectiveApplicableMovieId() {
        return applicableMovieIdSnapshot != null ? applicableMovieIdSnapshot : (promotion != null ? promotion.getApplicableMovieId() : null);
    }

    public String effectiveApplicableMovieTitle() {
        return (applicableMovieTitleSnapshot != null && !applicableMovieTitleSnapshot.isBlank())
                ? applicableMovieTitleSnapshot
                : null;
    }

    public String effectiveCustomerEligibility() {
        return customerEligibilitySnapshot != null ? customerEligibilitySnapshot : (promotion != null ? promotion.getCustomerEligibility() : "ALL");
    }
}
