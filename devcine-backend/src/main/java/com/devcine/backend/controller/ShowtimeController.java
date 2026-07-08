package com.devcine.backend.controller;

import com.devcine.backend.dto.response.CinemaShowtimeDTO;
import com.devcine.backend.dto.response.MovieCardDTO;
import com.devcine.backend.dto.response.PublicShowtimeDTO;
import com.devcine.backend.dto.response.ShowtimeDTO;
import com.devcine.backend.dto.request.ShowtimeRequest;
import com.devcine.backend.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

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

    @GetMapping("/upcoming")
    public ResponseEntity<List<com.devcine.backend.dto.response.PublicShowtimeDTO>> getAllUpcomingShowtimes() {
        return ResponseEntity.ok(showtimeService.getAllUpcomingShowtimes());
    }

    // ===== Trang Lịch chiếu: lọc phía server + phân trang =====

    @GetMapping("/cinemas")
    public ResponseEntity<List<Map<String, Object>>> getCinemasByCity(@RequestParam(required = false) String city) {
        return ResponseEntity.ok(showtimeService.getCinemasByCity(city));
    }

    @GetMapping("/movies")
    public ResponseEntity<Page<MovieCardDTO>> getMoviesWithShowtimes(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(showtimeService.getMoviesWithShowtimes(city, date, q, page, size));
    }

    @GetMapping("/by-movie")
    public ResponseEntity<List<PublicShowtimeDTO>> getByMovie(
            @RequestParam Integer movieId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(showtimeService.getShowtimesByMovieAndDate(movieId, date, city));
    }

    @GetMapping("/by-cinema")
    public ResponseEntity<List<PublicShowtimeDTO>> getByCinema(
            @RequestParam Integer cinemaId,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(showtimeService.getShowtimesByCinemaAndDate(cinemaId, date));
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
    @PreAuthorize("@perm.can('schedules', 'add')")
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

    @PostMapping("/batch")
    @PreAuthorize("@perm.can('schedules', 'add')")
    public ResponseEntity<?> createBatchShowtimes(
            @Valid @RequestBody com.devcine.backend.dto.request.BatchShowtimeRequest request) {
        try {
            return ResponseEntity.ok(showtimeService.createBatchShowtimes(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@perm.can('schedules', 'edit')")
    public ResponseEntity<?> updateShowtime(@PathVariable Integer id, @RequestBody java.util.Map<String, Object> updates) {
        try {
            showtimeService.updateShowtime(id, updates);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
