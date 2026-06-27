package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Đơn bán bắp nước/đồ ăn độc lập tại quầy (Concession Only) — KHÔNG gắn suất chiếu/ghế.
 * Tách khỏi bảng {@code bookings} để doanh thu phòng vé không bị sai lệch bởi khách vãng lai.
 */
@Entity
@Table(name = "concession_sales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcessionSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sale_code", length = 50, unique = true)
    private String saleCode;

    /** Thành viên (nếu có) để tích điểm — null với khách vãng lai. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(length = 50)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
