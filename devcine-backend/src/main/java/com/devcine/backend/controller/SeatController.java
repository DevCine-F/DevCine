package com.devcine.backend.controller;

import com.devcine.backend.dto.SeatDTO;
import com.devcine.backend.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/showtime/{showtimeId}")
    public ResponseEntity<List<SeatDTO>> getSeatsForShowtime(@PathVariable Integer showtimeId) {
        return ResponseEntity.ok(seatService.getSeatsForShowtime(showtimeId));
    }
}
