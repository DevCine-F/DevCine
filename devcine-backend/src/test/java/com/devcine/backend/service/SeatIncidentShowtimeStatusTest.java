package com.devcine.backend.service;

import com.devcine.backend.dto.response.IncidentBookingContext;
import com.devcine.backend.entity.Showtime;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SeatIncidentShowtimeStatusTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 30, 20, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 30, 22, 0);

    @Test
    void resolvesAllShowtimeStatesAtBoundaries() {
        assertEquals(IncidentBookingContext.ShowtimeStatus.UPCOMING,
                SeatIncidentService.resolveShowtimeStatus(START, END, START.minusNanos(1)));
        assertEquals(IncidentBookingContext.ShowtimeStatus.IN_PROGRESS,
                SeatIncidentService.resolveShowtimeStatus(START, END, START));
        assertEquals(IncidentBookingContext.ShowtimeStatus.IN_PROGRESS,
                SeatIncidentService.resolveShowtimeStatus(START, END, END.minusNanos(1)));
        assertEquals(IncidentBookingContext.ShowtimeStatus.ENDED,
                SeatIncidentService.resolveShowtimeStatus(START, END, END));
        assertEquals(IncidentBookingContext.ShowtimeStatus.ENDED,
                SeatIncidentService.resolveShowtimeStatus(START, END, END.plusHours(2).minusNanos(1)));
        assertEquals(IncidentBookingContext.ShowtimeStatus.EXPIRED,
                SeatIncidentService.resolveShowtimeStatus(START, END, END.plusHours(2)));
    }

    @Test
    void allowsRelocationWhileShowtimeIsInProgress() {
        Showtime showtime = Showtime.builder().startTime(START).endTime(END).build();

        assertDoesNotThrow(() ->
                SeatIncidentService.assertRelocationAllowedAt(showtime, START.plusMinutes(30)));
    }

    @Test
    void blocksRelocationWhenShowtimeHasEnded() {
        Showtime showtime = Showtime.builder().startTime(START).endTime(END).build();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                SeatIncidentService.assertRelocationAllowedAt(showtime, END));

        assertEquals("Suất chiếu đã kết thúc — không thể đổi ghế.", error.getMessage());
    }
}
