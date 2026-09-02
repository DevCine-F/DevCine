package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seat_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "color_code", length = 10)
    private String colorCode;

    /** Phụ thu loại ghế ngày thường (T2–T5). Mặc định 0.00. */
    @Column(columnDefinition = "decimal(15,2) not null default 0")
    @Builder.Default
    private java.math.BigDecimal surcharge = java.math.BigDecimal.ZERO;

    /** Phụ thu loại ghế cuối tuần (T6–CN & Lễ). Null = dùng surcharge. */
    @Column(name = "weekend_surcharge", precision = 15, scale = 2)
    private java.math.BigDecimal weekendSurcharge;
}

