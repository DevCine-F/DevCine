package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/** Định dạng chiếu phim (2D, 3D, IMAX...) — danh mục dùng cho màn quản lý phim. */
@Entity
@Table(name = "formats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Format {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;
}
