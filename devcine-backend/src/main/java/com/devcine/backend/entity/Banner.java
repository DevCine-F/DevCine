package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "banners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 255)
    private String title;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Chế độ hiển thị: IMAGE = ảnh nguyên bản admin upload; MOVIE = dựng theo thông tin phim
    @Column(name = "mode", length = 20)
    @Builder.Default
    private String mode = "IMAGE";

    // ID phim được chọn khi mode = MOVIE
    @Column(name = "movie_id")
    private Integer movieId;

    @Column(length = 50)
    private String placement;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "link", length = 500)
    private String link;
}
