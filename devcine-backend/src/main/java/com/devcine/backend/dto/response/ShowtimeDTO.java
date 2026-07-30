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
}
