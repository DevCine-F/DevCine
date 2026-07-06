package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sổ điểm thưởng — ghi lại MỌI biến động điểm của khách hàng để minh bạch, đối soát,
 * và làm nền tảng cho hết hạn điểm. Mỗi dòng là một giao dịch bất biến (append-only).
 */
@Entity
@Table(name = "point_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** Số điểm biến động: dương = cộng (EARN), âm = trừ (REDEEM/VOID). */
    @Column(nullable = false)
    private Integer points;

    /** EARN | REDEEM | VOID | ADJUST */
    @Column(length = 20, nullable = false)
    private String type;

    /** Nguồn: BOOKING | FNB | PROMO_REDEEM | VOID_FNB | ADMIN */
    @Column(length = 30)
    private String source;

    /** Mã tham chiếu: bookingCode / saleCode / mã khuyến mãi. */
    @Column(name = "ref_code", length = 50)
    private String refCode;

    /** Số dư điểm khả dụng SAU giao dịch (để hiển thị lịch sử). */
    @Column(name = "balance_after")
    private Integer balanceAfter;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
