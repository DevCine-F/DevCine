package com.devcine.backend.controller;

import com.devcine.backend.dto.response.CinemaShowtimeDTO;
import com.devcine.backend.dto.response.ShowtimeDTO;
import com.devcine.backend.dto.request.ShowtimeRequest;
import com.devcine.backend.service.ShowtimeService;
import jakarta.validation.Valid;
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

    @GetMapping("/cinema/{cinemaId}")
    public ResponseEntity<List<ShowtimeDTO>> getShowtimesByCinema(@PathVariable Integer cinemaId) {
        return ResponseEntity.ok(showtimeService.getShowtimesByCinemaId(cinemaId));
    }

    @PostMapping
    public ResponseEntity<?> createShowtime(@Valid @RequestBody ShowtimeRequest request) {
        try {
            ShowtimeDTO dto = showtimeService.createShowtime(request);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
