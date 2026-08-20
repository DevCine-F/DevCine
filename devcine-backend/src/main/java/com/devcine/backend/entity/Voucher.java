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

    // ═══ SNAPSHOT đóng băng thông số giảm giá tại thời điểm voucher được phát ═══
    // Khi Admin sửa Promotion sau đó, các voucher đã phát KHÔNG bị ảnh hưởng.
    // Null = voucher cũ (trước khi có snapshot) → fallback về Promotion LIVE.

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

    /** Copy thông số giảm giá từ Promotion vào snapshot — gọi khi tạo voucher. */
    public void snapshotFrom(Promotion promo) {
        this.discountTypeSnapshot = promo.getDiscountType();
        this.discountValueSnapshot = promo.getDiscountValue();
        this.maxDiscountAmountSnapshot = promo.getMaxDiscountAmount();
        this.maxTicketQuantitySnapshot = promo.getMaxTicketQuantity();
        this.minOrderValueSnapshot = promo.getMinOrderValue();
    }

    // ═══ Getter an toàn: ưu tiên snapshot, fallback Promotion LIVE ═══

    public String effectiveDiscountType() {
        return discountTypeSnapshot != null ? discountTypeSnapshot : promotion.getDiscountType();
    }

    public BigDecimal effectiveDiscountValue() {
        return discountValueSnapshot != null ? discountValueSnapshot : promotion.getDiscountValue();
    }

    public BigDecimal effectiveMaxDiscountAmount() {
        return maxDiscountAmountSnapshot != null ? maxDiscountAmountSnapshot : promotion.getMaxDiscountAmount();
    }

    public Integer effectiveMaxTicketQuantity() {
        return maxTicketQuantitySnapshot != null ? maxTicketQuantitySnapshot : promotion.getMaxTicketQuantity();
    }

    public BigDecimal effectiveMinOrderValue() {
        return minOrderValueSnapshot != null ? minOrderValueSnapshot : promotion.getMinOrderValue();
    }
}
