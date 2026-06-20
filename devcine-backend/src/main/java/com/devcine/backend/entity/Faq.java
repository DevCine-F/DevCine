package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/** Câu hỏi thường gặp (FAQ) hiển thị ở trang Hỗ trợ — quản trị viên CRUD được. */
@Entity
@Table(name = "faqs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Nhóm danh mục, vd "Đặt vé & Thanh toán". */
    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active", columnDefinition = "boolean not null default true")
    private Boolean isActive;
}
