package com.devcine.backend.service;

import com.devcine.backend.dto.response.SeatDTO;
import com.devcine.backend.entity.BookingSeat;
import com.devcine.backend.entity.Seat;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.repository.SeatRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeRepository showtimeRepository;

    public List<SeatDTO> getSeatsForShowtime(Integer showtimeId) {
        var showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));

        Integer roomId = showtime.getRoom().getId();
        List<Seat> allSeats = seatRepository.findByRoomIdAndIsActiveTrue(roomId);

        List<BookingSeat> reservedBookingSeats = bookingSeatRepository.findReservedSeatsByShowtime(showtimeId);
        
        Set<Integer> soldSeatIds = reservedBookingSeats.stream()
                .filter(bs -> "SOLD".equals(bs.getStatus()))
                .map(bs -> bs.getSeat().getId())
                .collect(Collectors.toSet());
                
        Set<Integer> holdSeatIds = reservedBookingSeats.stream()
                .filter(bs -> "HOLD".equals(bs.getStatus()))
                .map(bs -> bs.getSeat().getId())
                .collect(Collectors.toSet());

        return allSeats.stream().map(seat -> {
            String status = "AVAILABLE";
            if (soldSeatIds.contains(seat.getId())) {
                status = "SOLD";
            } else if (holdSeatIds.contains(seat.getId())) {
                status = "HOLD";
            }
            
            // In a real app, price should be determined by PricingRule or MovieFormat etc.
            // For now, assume a base price for the seat type
            return SeatDTO.builder()
                    .seatId(seat.getId())
                    .rowChar(seat.getRowChar())
                    .colNum(seat.getColNum())
                    .seatType(seat.getSeatType().getName())
                    .price(seat.getSeatType().getPriceModifier())
                    .status(status)
                    .build();
        }).collect(Collectors.toList());
    }
}
