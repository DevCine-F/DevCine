package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tin khuyến mãi (nội dung biên tập hiển thị cho khách) — KHÁC voucher/Promotion.
 * Gồm tiêu đề, ảnh, mô tả ngắn, nội dung chi tiết, khoảng thời gian hiển thị và cờ bật/tắt.
 */
@Entity
@Table(name = "promo_articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String title;

    /** Mô tả ngắn hiển thị ở thẻ danh sách. */
    @Column(length = 500)
    private String description;

    /** Ảnh banner/thumbnail (URL Cloudinary). */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Nội dung chi tiết hiển thị ở modal "Xem chi tiết". */
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active", columnDefinition = "boolean not null default true")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order", columnDefinition = "integer not null default 0")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
