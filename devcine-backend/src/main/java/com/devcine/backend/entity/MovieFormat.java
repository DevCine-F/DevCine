package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "movie_formats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    /**
     * Phụ thu CÔNG NGHỆ định dạng cho NGÀY THƯỜNG (T2–T5). Vd 2D +0, 3D +30k.
     * Cộng vào giá nền (theo loại ngày × loại phòng × đối tượng) — KHÔNG liên quan hạng phòng.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal surcharge;

    /** Phụ thu định dạng cho CUỐI TUẦN & NGÀY LỄ (T6,7,CN + lễ). Null → dùng surcharge thường. */
    @Column(name = "weekend_surcharge", precision = 15, scale = 2)
    private BigDecimal weekendSurcharge;
}
