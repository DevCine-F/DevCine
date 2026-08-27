package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate dob;

    @Column(name = "id_card", length = 20)
    private String idCard;

    @Column(name = "membership_tier", length = 50)
    private String membershipTier;

    /** Điểm khả dụng (ví tiêu được) — tăng khi mua, giảm khi đổi điểm/void. */
    @Column(name = "loyalty_points")
    @Builder.Default
    private Integer loyaltyPoints = 0;

    /** Điểm tích lũy trọn đời — CHỈ dùng để xét hạng thành viên; KHÔNG giảm khi khách đổi điểm. */
    @Column(name = "lifetime_points")
    @Builder.Default
    private Integer lifetimePoints = 0;

    @Column(name = "lock_reason", columnDefinition = "TEXT")
    private String lockReason;

    @Column(name = "locked_at")
    private java.time.LocalDateTime lockedAt;
}
