package com.devcine.backend.validator;

import com.devcine.backend.entity.Seat;
import com.devcine.backend.entity.SeatType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrphanSeatValidatorTest {

    private final OrphanSeatValidator validator = new OrphanSeatValidator();

    @Test
    void detectsSingleEmptySeatCreatedAtRowBoundary() {
        List<Seat> seats = List.of(seat(1, 0), seat(2, 1), seat(3, 2));

        assertTrue(validator.hasOrphanSeats(seats, Set.of(1, 2), List.of(1, 2)));
    }

    @Test
    void acceptsTwoAdjacentEmptySeats() {
        List<Seat> seats = List.of(seat(1, 0), seat(2, 1), seat(3, 2));

        assertFalse(validator.hasOrphanSeats(seats, Set.of(1), List.of(1)));
    }

    @Test
    void ignoresOrphanThatWasNotCreatedByCurrentSelection() {
        List<Seat> seats = List.of(seat(1, 0), seat(2, 1), seat(3, 2), seat(4, 3));

        assertFalse(validator.hasOrphanSeats(seats, Set.of(1, 4), List.of(4)));
    }

    private Seat seat(int id, int gridCol) {
        return Seat.builder()
                .id(id)
                .rowChar("A")
                .colNum(gridCol + 1)
                .gridRow(0)
                .gridCol(gridCol)
                .cellKind("SEAT")
                .seatStatus("AVAILABLE")
                .isActive(true)
                .seatType(SeatType.builder().name("NORMAL").build())
                .build();
    }
}
