package com.devcine.backend.service;

import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.entity.Review;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.MovieRepository;
import com.devcine.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getMovieReviews(Integer movieId) {
        List<Review> reviews = reviewRepository.findByMovieIdWithCustomer(movieId);

        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        average = Math.round(average * 10.0) / 10.0; // làm tròn 1 chữ số thập phân

        List<Map<String, Object>> items = reviews.stream().map(r -> Map.<String, Object>of(
                "id", r.getId(),
                "rating", r.getRating(),
                "comment", r.getComment() != null ? r.getComment() : "",
                "createdAt", r.getCreatedAt().toString(),
                "customerName", r.getCustomer() != null && r.getCustomer().getUser() != null
                        ? r.getCustomer().getUser().getFullName() : "Khách hàng"
        )).collect(Collectors.toList());

        return Map.of(
                "averageRating", average,
                "totalReviews", reviews.size(),
                "reviews", items
        );
    }

    /** Mỗi khách chỉ có 1 đánh giá cho 1 phim: đã có thì cập nhật, chưa có thì tạo mới. */
    @Transactional
    public Review createOrUpdateReview(Integer movieId, Integer customerId, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("Điểm đánh giá phải từ 1 đến 5 sao");
        }
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        Review review = reviewRepository.findByMovieIdAndCustomerId(movieId, customerId)
                .orElseGet(() -> Review.builder()
                        .movie(movie)
                        .customer(customer)
                        .createdAt(LocalDateTime.now())
                        .build());
        review.setRating(rating);
        review.setComment(comment);
        return reviewRepository.save(review);
    }
}
