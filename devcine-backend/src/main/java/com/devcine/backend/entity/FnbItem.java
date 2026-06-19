package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fnb_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FnbItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(length = 30)
    private String type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String description;

    /** Còn bán / hiển thị cho khách ở bước chọn combo hay không. */
    @Column(name = "is_active", columnDefinition = "boolean not null default true")
    @Builder.Default
    private Boolean isActive = true;
}
