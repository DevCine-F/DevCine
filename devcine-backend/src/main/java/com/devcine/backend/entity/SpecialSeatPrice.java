package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Bảng giá cố định cho định dạng/phòng đặc biệt (IMAX/4DX/Gold) theo từng loại ghế.
 * Chỉ áp dụng khi MovieFormat.isFixedPrice=true. Giá này ghi đè công thức cộng dồn.
 */
@Entity
@Table(name = "special_seat_prices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"format_id", "seat_type_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialSeatPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "format_id", nullable = false)
    private MovieFormat format;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_type_id", nullable = false)
    private SeatType seatType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;
}
