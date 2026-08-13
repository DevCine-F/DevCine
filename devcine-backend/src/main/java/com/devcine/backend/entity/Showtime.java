package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "showtimes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "format_id", nullable = false)
    private MovieFormat format;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(length = 20)
    private String status;

    /**
     * Ảnh chụp (snapshot) BẤT BIẾN khung sơ đồ ghế (JSON) tại thời điểm TẠO suất.
     * Nguồn CHUNG cho cả hiển thị sơ đồ lẫn luật đặt vé → sửa phòng về sau KHÔNG phá suất cũ,
     * và không còn cảnh "mỗi trang một sơ đồ". Chỉ chứa khung không gian + seatId; trạng thái
     * SOLD/HOLD/MAINTENANCE luôn overlay live theo seatId.
     * NULL với suất cũ (trước migration) → đọc live để tương thích ngược (xem SeatService/BookingService).
     */
    @Column(name = "layout_data", columnDefinition = "text")
    private String layoutData;
}
