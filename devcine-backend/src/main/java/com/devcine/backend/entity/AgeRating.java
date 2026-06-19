package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/** Phân loại kiểm duyệt độ tuổi (P, K, T13, T16, T18...) — danh mục dùng cho màn quản lý phim. */
@Entity
@Table(name = "age_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgeRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;
}
