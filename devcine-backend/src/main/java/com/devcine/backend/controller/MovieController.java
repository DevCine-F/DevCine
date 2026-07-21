package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.request.MovieBulkRequest;
import com.devcine.backend.dto.request.MovieRequest;
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
    public ApiResponse<List<MovieSummaryDTO>> getAllMovies() {
        return ApiResponse.ok(movieService.getAllMovies());
    }

    @GetMapping("/search")
    public ApiResponse<List<MovieSummaryDTO>> searchMovies(@RequestParam(value = "q", required = false) String q) {
        return ApiResponse.ok(movieService.searchMovies(q));
    }

    @GetMapping("/now-showing")
    public ApiResponse<List<MovieSummaryDTO>> getNowShowing() {
        return ApiResponse.ok(movieService.getNowShowing());
    }

    @GetMapping("/upcoming")
    public ApiResponse<List<MovieSummaryDTO>> getUpcoming() {
        return ApiResponse.ok(movieService.getUpcoming());
    }

    // Ràng buộc chỉ khớp id dạng số để không nuốt các path như /now-showing, /search
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<?> getMovieById(@PathVariable Integer id) {
        Movie movie = movieService.getMovieById(id);
        if (movie != null) {
            return ResponseEntity.ok(ApiResponse.ok(movie));
        }
        return ResponseEntity.status(404).body(ApiResponse.fail("Không tìm thấy dữ liệu."));
    }

    /** Thống kê vận hành thật theo phim (doanh thu, vé bán, lấp đầy, hạng vé) cho modal chi tiết. */
    @GetMapping("/{id:\\d+}/stats")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<?> getMovieStats(@PathVariable Integer id) {
        if (movieService.getMovieById(id) == null) {
            return ResponseEntity.status(404).body(ApiResponse.fail("Không tìm thấy dữ liệu."));
        }
        return ResponseEntity.ok(ApiResponse.ok(movieService.getMovieStats(id)));
    }

    /** Đổi trạng thái hàng loạt (cũng dùng cho đổi nhanh 1 phim) — trả {updated, blocked[]}. */
    @PatchMapping("/bulk-status")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<?> bulkUpdateStatus(@Valid @RequestBody MovieBulkRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.bulkUpdateStatus(request.getIds(), request.getStatus())));
    }

    /** Xoá hàng loạt — trả {deleted, blocked[]} để FE hiện toast thành công một phần. */
    @DeleteMapping("/bulk")
    @PreAuthorize("@perm.can('movies','delete')")
    public ResponseEntity<?> bulkDelete(@Valid @RequestBody MovieBulkRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.bulkDelete(request.getIds())));
    }

    @PostMapping
    @PreAuthorize("@perm.can('movies','add')")
    public ResponseEntity<?> createMovie(@Valid @RequestBody MovieRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(movieService.createMovie(request)));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<?> updateMovie(@PathVariable Integer id, @Valid @RequestBody MovieRequest request) {
        try {
            Movie updatedMovie = movieService.updateMovie(id, request);
            if (updatedMovie != null) {
                return ResponseEntity.ok(ApiResponse.ok(updatedMovie));
            }
            return ResponseEntity.status(404).body(ApiResponse.fail("Không tìm thấy dữ liệu."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('movies','delete')")
    public ResponseEntity<?> deleteMovie(@PathVariable Integer id) {
        try {
            movieService.deleteMovie(id);
            return ResponseEntity.ok(ApiResponse.success("Đã xoá."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
