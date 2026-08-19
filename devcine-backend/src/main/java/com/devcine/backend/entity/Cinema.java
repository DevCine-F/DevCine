package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "cinemas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cinema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(length = 50)
    private String type;

    @Column(length = 20)
    private String hotline;

    @Column
    private Integer rooms;

    // ----- Thông tin mở rộng phục vụ trang khách (cột mới, ddl-auto update) -----

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    /** Danh sách tiện ích, lưu chuỗi ngăn cách bởi dấu phẩy (vd "Superplex,Dolby Atmos,Bãi đỗ xe"). */
    @Column(columnDefinition = "TEXT")
    private String amenities;

    /** Trạng thái hoạt động: ACTIVE / MAINTENANCE / CLOSED. */
    @Column(length = 20)
    private String status;

    /** Giờ mở cửa (mặc định 08:00). Dùng cho ma trận timeline động + kiểm soát ràng buộc suất chiếu. */
    @Column(name = "opening_time")
    private LocalTime openingTime;

    /** Giờ đóng cửa (mặc định 23:30). Nếu <= openingTime ⇒ đóng cửa RẠNG SÁNG hôm sau (suất khuya). */
    @Column(name = "closing_time")
    private LocalTime closingTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Staff manager;
}
