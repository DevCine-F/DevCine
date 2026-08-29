package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeDTO {
    private Integer id;
    private Integer roomId;
    private String roomName;
    private Integer formatId;
    private String formatName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String movie;
    private Integer duration;
    // Tình trạng ghế cho card suất chiếu (client): tổng ghế bán được của phòng & số còn trống
    private Integer totalSeats;      // ghế đang hoạt động, bán được (loại trừ ghế bảo trì/khóa)
    private Integer availableSeats;  // totalSeats − ghế đã bán/đang giữ (SOLD/HOLD)

    /**
     * true nếu suất này là xuất chiếu sớm (startTime.toLocalDate() < movie.releaseDate).
     * FE dùng để hiện badge "Chiếu sớm 🎬" trên từng slot giờ chiếu.
     */
    private boolean earlyScreening;

    /**
     * Constructor phục vụ JPQL Constructor Projection (tránh SELECT toàn bộ entity
     * và các cột nặng như layout_data, poster_base64, banner_base64).
     */
    public ShowtimeDTO(Integer id, Integer roomId, String roomName, Integer formatId, String formatName,
                       LocalDateTime startTime, LocalDateTime endTime, String status, String movie,
                       Integer duration, boolean earlyScreening) {
        this.id = id;
        this.roomId = roomId;
        this.roomName = roomName;
        this.formatId = formatId;
        this.formatName = formatName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.movie = movie;
        this.duration = duration;
        this.earlyScreening = earlyScreening;
    }
}
