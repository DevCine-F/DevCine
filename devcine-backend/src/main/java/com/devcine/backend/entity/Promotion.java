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

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

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
}
