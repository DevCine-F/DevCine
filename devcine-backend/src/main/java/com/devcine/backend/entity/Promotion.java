package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, length = 50)
    private String code;

    /** Tên/tiêu đề hiển thị của voucher (vd "Khuyến mãi hè rực rỡ"). */
    @Column(name = "name", length = 255)
    private String name;

    /** Mô tả ngắn cho voucher, hiển thị ở chi tiết. */
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active", columnDefinition = "boolean not null default true")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_stackable", nullable = false)
    @Builder.Default
    private Boolean isStackable = false;

    @Column(name = "points_required")
    @Builder.Default
    private Integer pointsRequired = 0;

    /** Cho phép khách tự đổi điểm tích luỹ lấy voucher này (true) hay chỉ admin phát thủ công (false). */
    @Column(name = "allow_point_redemption", columnDefinition = "boolean not null default false")
    @Builder.Default
    private Boolean allowPointRedemption = false;

    /** Giá trị đơn tối thiểu để được áp mã (0/null = không yêu cầu). */
    @Column(name = "min_order_value", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    /** Chỉ áp dụng cho phim này (null = áp dụng mọi phim). */
    @Column(name = "applicable_movie_id")
    private Integer applicableMovieId;

    /**
     * Đối tượng áp dụng: ALL (mọi khách) | NEW_CUSTOMER (khách chưa từng mua vé)
     * | TIER_SILVER | TIER_GOLD | TIER_PLATINUM (khách thân thiết: hạng thành viên tối thiểu).
     */
    @Column(name = "customer_eligibility", length = 20, columnDefinition = "varchar(20) not null default 'ALL'")
    @Builder.Default
    private String customerEligibility = "ALL";

    /** Tổng số lượt được dùng (0/null = không giới hạn). */
    @Column(name = "usage_limit")
    @Builder.Default
    private Integer usageLimit = 0;

    /** Số lượt đã dùng (tăng khi voucher được dùng thanh toán thành công). */
    @Column(name = "used_count", columnDefinition = "integer not null default 0")
    @Builder.Default
    private Integer usedCount = 0;

    /**
     * Số vé tối đa trong 1 đơn được áp mã (0/null = không giới hạn).
     * Khi > 0, chỉ những vé ĐẮT NHẤT (tối đa X vé) được tính giảm; vé còn lại giữ giá gốc.
     */
    @Column(name = "max_ticket_quantity")
    @Builder.Default
    private Integer maxTicketQuantity = 0;

    /** Số tiền giảm tối đa cho 1 đơn (0/null = không giới hạn) — trần capping áp sau khi tính giảm. */
    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal maxDiscountAmount = BigDecimal.ZERO;

    /** Thời điểm gửi email chiến dịch gần nhất (null = chưa gửi lần nào). */
    @Column(name = "campaign_sent_at")
    private LocalDateTime campaignSentAt;

    /** Tổng số khách ĐÃ nhận email chiến dịch của mã này (dedup — không đếm trùng người). */
    @Column(name = "campaign_sent_count", columnDefinition = "integer not null default 0")
    @Builder.Default
    private Integer campaignSentCount = 0;
}
