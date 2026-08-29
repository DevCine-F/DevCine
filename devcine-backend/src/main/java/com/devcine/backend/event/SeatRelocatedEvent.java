package com.devcine.backend.event;

import com.devcine.backend.dto.IncidentRelocateEmailData;
import com.devcine.backend.dto.response.IncidentResultResponse;

import java.util.List;

/**
 * Domain Event bắn ra SAU KHI transaction đổi ghế đền bù hoàn tất thành công.
 * Dùng để tách các tác vụ I/O (gửi Email, STOMP) ra khỏi Transaction & Redis distributed lock.
 */
public record SeatRelocatedEvent(
        Integer bookingId,
        String reason,
        List<IncidentRelocateEmailData.SeatSwapLine> swaps,
        IncidentResultResponse.CompensationResult comp,
        String voucherLabel
) {}
