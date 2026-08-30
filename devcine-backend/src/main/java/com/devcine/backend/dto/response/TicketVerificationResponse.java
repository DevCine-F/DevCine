package com.devcine.backend.dto.response;

import java.time.LocalDateTime;

/** Dữ liệu an toàn trả về sau khi soát một QR vé lẻ. */
public record TicketVerificationResponse(
        Integer ticketId,
        String bookingCode,
        String seatLabel,
        String movieTitle,
        String roomName,
        LocalDateTime startTime,
        LocalDateTime checkInTime
) {}
