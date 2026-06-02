package com.devcine.backend.dto;

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
}
