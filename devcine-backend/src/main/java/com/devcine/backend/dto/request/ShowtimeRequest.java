package com.devcine.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeRequest {
    @NotNull(message = "Movie ID is required")
    private Integer movieId;

    @NotNull(message = "Room ID is required")
    private Integer roomId;

    @NotNull(message = "Format ID is required")
    private Integer formatId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;
    // Thời gian dọn dẹp KHÔNG nhận từ FE nữa — bốc trực tiếp từ Room.turnaroundTimeMins ở service.
}
