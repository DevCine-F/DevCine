package com.devcine.backend.dto.response;

import lombok.Builder;

/** Kết quả khóa/mở ghế vật lý + id dòng ghi vết seat_incidents (nếu có). */
@Builder
public record SeatPhysicalStatusResponse(
        Integer seatId,
        String seatLabel,
        String status,        // AVAILABLE | MAINTENANCE | LOCKED
        Integer incidentId
) {}
