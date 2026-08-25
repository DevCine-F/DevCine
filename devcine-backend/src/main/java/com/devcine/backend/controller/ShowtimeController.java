package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.response.CinemaShowtimeDTO;
import com.devcine.backend.dto.response.MovieCardDTO;
import com.devcine.backend.dto.response.PublicShowtimeDTO;
import com.devcine.backend.dto.response.ShowtimeDTO;
import com.devcine.backend.dto.response.SneakPreviewDTO;
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
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @GetMapping("/sneak-previews")
    public ResponseEntity<ApiResponse<List<SneakPreviewDTO>>> getSneakPreviews() {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getSneakPreviews()));
    }

    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<String>>> getAllCities() {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getAllCities()));
    }


    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<PublicShowtimeDTO>>> getAllUpcomingShowtimes() {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getAllUpcomingShowtimes()));
    }

    @GetMapping("/cinemas-with-showtimes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCinemasWithUpcomingShowtimes() {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getCinemasWithUpcomingShowtimes()));
    }

    // ===== Trang Lịch chiếu: lọc phía server + phân trang =====

    @GetMapping("/cinemas")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCinemasByCity(@RequestParam(required = false) String city) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getCinemasByCity(city)));
    }

    @GetMapping("/movies")
    public ResponseEntity<ApiResponse<Page<MovieCardDTO>>> getMoviesWithShowtimes(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getMoviesWithShowtimes(city, date, q, page, size)));
    }

    @GetMapping("/by-movie")
    public ResponseEntity<ApiResponse<List<PublicShowtimeDTO>>> getByMovie(
            @RequestParam Integer movieId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getShowtimesByMovieAndDate(movieId, date, city)));
    }

    @GetMapping("/by-cinema")
    public ResponseEntity<ApiResponse<List<PublicShowtimeDTO>>> getByCinema(
            @RequestParam Integer cinemaId,
            @RequestParam(required = false) String date) {
        if (date != null && !date.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(showtimeService.getShowtimesByCinemaAndDate(cinemaId, date)));
        }
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getUpcomingShowtimesByCinema(cinemaId)));
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<List<CinemaShowtimeDTO>>> getShowtimesForMovie(
            @PathVariable Integer movieId,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getShowtimesForMovie(movieId, city)));
    }

    @GetMapping("/cinema/{cinemaId}")
    public ResponseEntity<ApiResponse<List<ShowtimeDTO>>> getShowtimesByCinema(@PathVariable Integer cinemaId) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getShowtimesByCinemaId(cinemaId)));
    }

    @PostMapping
    @PreAuthorize("@perm.can('schedules', 'add')")
    public ResponseEntity<?> createShowtime(@Valid @RequestBody ShowtimeRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(showtimeService.createShowtime(request)));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/batch")
    @PreAuthorize("@perm.can('schedules', 'add')")
    public ResponseEntity<?> createBatchShowtimes(
            @Valid @RequestBody com.devcine.backend.dto.request.BatchShowtimeRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(showtimeService.createBatchShowtimes(request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** Backfill snapshot sơ đồ cho suất cũ (layout_data null). Chạy 1 lần, chỉ ADMIN. */
    @PostMapping("/backfill-layout")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> backfillLayout() {
        int count = showtimeService.backfillLayoutSnapshots();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("updated", count)));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<?> getShowtimeDetail(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(showtimeService.getShowtimeDetail(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@perm.can('schedules', 'edit')")
    public ResponseEntity<?> updateShowtime(@PathVariable Integer id, @RequestBody java.util.Map<String, Object> updates) {
        try {
            showtimeService.updateShowtime(id, updates);
            return ResponseEntity.ok(ApiResponse.success("Đã cập nhật suất chiếu."));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('schedules', 'edit')")
    public ResponseEntity<?> deleteShowtime(@PathVariable Integer id) {
        try {
            showtimeService.deleteShowtime(id);
            return ResponseEntity.ok(ApiResponse.success("Đã xoá suất chiếu."));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
