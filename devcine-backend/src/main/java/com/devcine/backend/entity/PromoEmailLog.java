package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Nhật ký gửi email chiến dịch: mỗi (promotion, customer) chỉ 1 dòng → chống gửi trùng cho cùng
 * một khách khi Admin gửi lại chiến dịch (dedup). Cũng là nguồn tra cứu lịch sử gửi.
 */
@Entity
@Table(name = "promo_email_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_promo_email_customer", columnNames = {"promotion_id", "customer_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoEmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "promotion_id", nullable = false)
    private Integer promotionId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
