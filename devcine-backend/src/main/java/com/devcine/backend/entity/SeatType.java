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
    // Flat pricing: KHÔNG còn phụ thu theo loại ghế. Loại ghế chỉ mang ý nghĩa hiển thị (tên + màu).
}
