package com.devcine.backend.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Đơn vé bị ảnh hưởng ở các suất chiếu tương lai khi một ghế vật lý bị khóa bảo trì (Chain Lock).
 */
@Builder
public record FutureSeatConflictDTO(
        Integer bookingId,
        String bookingCode,
        Integer showtimeId,
        String movieTitle,
        String roomName,
        LocalDateTime startTime,
        String seatLabel,
        String customerName,
        String customerPhone
) {}
