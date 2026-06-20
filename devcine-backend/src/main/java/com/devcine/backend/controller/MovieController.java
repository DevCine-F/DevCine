package com.devcine.backend.controller;

import com.devcine.backend.entity.Movie;
import com.devcine.backend.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.devcine.backend.dto.response.MovieSummaryDTO;
@RestController
@RequestMapping("/api/movies")
@CrossOrigin("*") // Cho phép Frontend gọi API
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

    @PostMapping
    @PreAuthorize("@perm.can('movies','add')")
    public Movie createMovie(@RequestBody Movie movie) {
        return movieService.createMovie(movie);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<Movie> updateMovie(@PathVariable Integer id, @RequestBody Movie movie) {
        Movie updatedMovie = movieService.updateMovie(id, movie);
        if (updatedMovie != null) {
            return ResponseEntity.ok(updatedMovie);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('movies','delete')")
    public ResponseEntity<Void> deleteMovie(@PathVariable Integer id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
