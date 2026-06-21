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
     * Phụ thu định dạng cho NGÀY THƯỜNG (T2–T5). Vd 2D +0, 3D +20k.
     * Khi isFixedPrice=true: định dạng/phòng đặc biệt (IMAX/4DX/Gold) — giá lấy từ SpecialSeatPrice
     * (theo từng loại ghế), bỏ qua công thức cộng dồn; surcharge dùng làm giá mặc định nếu thiếu cấu hình.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal surcharge;

    /** Phụ thu định dạng cho CUỐI TUẦN & NGÀY LỄ (T6,7,CN + lễ). Vd 3D +30k. Null → dùng surcharge thường. */
    @Column(name = "weekend_surcharge", precision = 15, scale = 2)
    private BigDecimal weekendSurcharge;

    @Column(name = "is_fixed_price", columnDefinition = "boolean not null default false")
    private Boolean isFixedPrice;
}
