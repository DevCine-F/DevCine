package com.devcine.backend.controller;

import com.devcine.backend.entity.Review;
import com.devcine.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /** Quyền đánh giá của khách với phim này — FE dùng để render form động (đủ điều kiện / chưa mua vé). */
    @GetMapping("/movie/{movieId}/eligibility")
    public ResponseEntity<?> getEligibility(@PathVariable Integer movieId, @RequestParam Integer customerId) {
        return ResponseEntity.ok(reviewService.getReviewEligibility(movieId, customerId));
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
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Gửi đánh giá thất bại."));
        }
    }

    // ===== Quản trị: kiểm duyệt đánh giá (method security bảo vệ dù URL GET là permitAll) =====

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllForAdmin() {
        return ResponseEntity.ok(reviewService.getAllForAdmin());
    }

    @PutMapping("/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleVisibility(@PathVariable Integer id) {
        try {
            boolean hidden = reviewService.toggleHidden(id);
            return ResponseEntity.ok(Map.of("success", true, "hidden", hidden));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteReview(@PathVariable Integer id) {
        try {
            reviewService.deleteReview(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
