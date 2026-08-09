package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.LinkedHashSet;

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

    /**
     * Các Ô chọn món (Slot) khi cấu hình Combo — thay cho quan hệ ManyToMany cũ.
     * Một món có thể có nhiều slot trỏ tới cùng một pool (VD: "Nước 1", "Nước 2").
     *
     * <p>Dùng {@link Set} (không phải List/bag) để khi EntityGraph fetch kèm
     * {@code slots.optionGroup.items} thì tích Descartes (mỗi slot nhân theo số item
     * của pool) được Set gộp lại — không bị nhân bản slot. {@code @OrderBy} vẫn giữ
     * thứ tự (Hibernate trả LinkedHashSet).
     */
    @OneToMany(mappedBy = "fnbItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private Set<FnbComboSlot> slots = new LinkedHashSet<>();
}
