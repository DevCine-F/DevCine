package com.devcine.backend.controller;

import com.devcine.backend.dto.request.MovieBulkRequest;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.devcine.backend.dto.response.MovieStatsResponse;
import com.devcine.backend.dto.response.MovieSummaryDTO;
@RestController
@RequestMapping("/api/movies")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @GetMapping
    public List<MovieSummaryDTO> getAllMovies() {
        return movieService.getAllMovies();
    }

    @GetMapping("/search")
    public List<MovieSummaryDTO> searchMovies(@RequestParam(value = "q", required = false) String q) {
        return movieService.searchMovies(q);
    }

    @GetMapping("/now-showing")
    public List<MovieSummaryDTO> getNowShowing() {
        return movieService.getNowShowing();
    }

    @GetMapping("/upcoming")
    public List<MovieSummaryDTO> getUpcoming() {
        return movieService.getUpcoming();
    }

    // Ràng buộc chỉ khớp id dạng số để không nuốt các path như /now-showing, /search
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Integer id) {
        Movie movie = movieService.getMovieById(id);
        if (movie != null) {
            return ResponseEntity.ok(movie);
        }
        return ResponseEntity.notFound().build();
    }

    /** Thống kê vận hành thật theo phim (doanh thu, vé bán, lấp đầy, hạng vé) cho modal chi tiết. */
    @GetMapping("/{id:\\d+}/stats")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<MovieStatsResponse> getMovieStats(@PathVariable Integer id) {
        if (movieService.getMovieById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(movieService.getMovieStats(id));
    }

    /** Đổi trạng thái hàng loạt (cũng dùng cho đổi nhanh 1 phim) — trả {updated, blocked[]}. */
    @PatchMapping("/bulk-status")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<?> bulkUpdateStatus(@Valid @RequestBody MovieBulkRequest request) {
        return ResponseEntity.ok(movieService.bulkUpdateStatus(request.getIds(), request.getStatus()));
    }

    /** Xoá hàng loạt — trả {deleted, blocked[]} để FE hiện toast thành công một phần. */
    @DeleteMapping("/bulk")
    @PreAuthorize("@perm.can('movies','delete')")
    public ResponseEntity<?> bulkDelete(@Valid @RequestBody MovieBulkRequest request) {
        return ResponseEntity.ok(movieService.bulkDelete(request.getIds()));
    }

    @PostMapping
    @PreAuthorize("@perm.can('movies','add')")
    public ResponseEntity<?> createMovie(@RequestBody Movie movie) {
        try {
            return ResponseEntity.ok(movieService.createMovie(movie));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<?> updateMovie(@PathVariable Integer id, @RequestBody Movie movie) {
        try {
            Movie updatedMovie = movieService.updateMovie(id, movie);
            if (updatedMovie != null) {
                return ResponseEntity.ok(updatedMovie);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('movies','delete')")
    public ResponseEntity<?> deleteMovie(@PathVariable Integer id) {
        try {
            movieService.deleteMovie(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
