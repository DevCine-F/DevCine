package com.devcine.backend.controller;

import com.devcine.backend.dto.CinemaShowtimeDTO;
import com.devcine.backend.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // For development
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getAllCities() {
        return ResponseEntity.ok(showtimeService.getAllCities());
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<CinemaShowtimeDTO>> getShowtimesForMovie(
            @PathVariable Integer movieId,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(showtimeService.getShowtimesForMovie(movieId, city));
    }
}
