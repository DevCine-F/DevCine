package com.devcine.backend.controller;

import com.devcine.backend.entity.Review;
import com.devcine.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<?> getMovieReviews(@PathVariable Integer movieId) {
        return ResponseEntity.ok(reviewService.getMovieReviews(movieId));
    }

    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody Map<String, Object> body) {
        try {
            Integer movieId = Integer.parseInt(body.get("movieId").toString());
            Integer customerId = Integer.parseInt(body.get("customerId").toString());
            Integer rating = Integer.parseInt(body.get("rating").toString());
            String comment = body.get("comment") != null ? body.get("comment").toString() : "";

            Review review = reviewService.createOrUpdateReview(movieId, customerId, rating, comment);
            return ResponseEntity.status(201).body(Map.of("success", true, "id", review.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
