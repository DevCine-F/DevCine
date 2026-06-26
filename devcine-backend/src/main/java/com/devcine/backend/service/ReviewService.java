package com.devcine.backend.service;

import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.entity.Review;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.MovieRepository;
import com.devcine.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    /** Đánh giá công khai của 1 phim: chỉ review đang hiện + điểm trung bình + phân phối sao. */
    @Transactional(readOnly = true)
    public Map<String, Object> getMovieReviews(Integer movieId) {
        List<Review> reviews = reviewRepository.findVisibleByMovieIdWithCustomer(movieId);

        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        average = Math.round(average * 10.0) / 10.0;

        // Phân phối sao 5→1 (giữ thứ tự bằng LinkedHashMap)
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (int s = 5; s >= 1; s--) distribution.put(String.valueOf(s), 0);
        for (Review r : reviews) {
            String key = String.valueOf(r.getRating());
            distribution.merge(key, 1, Integer::sum);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Review r : reviews) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("rating", r.getRating());
            m.put("comment", r.getComment() != null ? r.getComment() : "");
            m.put("createdAt", r.getCreatedAt().toString());
            m.put("customerName", r.getCustomer() != null && r.getCustomer().getUser() != null
                    ? r.getCustomer().getUser().getFullName() : "Khách hàng");
            items.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("averageRating", average);
        result.put("totalReviews", reviews.size());
        result.put("distribution", distribution);
        result.put("reviews", items);
        return result;
    }

    /**
     * Mỗi khách chỉ có 1 đánh giá cho 1 phim (đã có thì cập nhật).
     * Điều kiện: khách phải đã mua vé (đơn CONFIRMED) cho phim này.
     */
    @Transactional
    public Review createOrUpdateReview(Integer movieId, Integer customerId, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5 sao.");
        }
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim."));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng."));

        if (!bookingRepository.hasConfirmedBookingForMovie(customerId, movieId)) {
            throw new IllegalArgumentException("Bạn cần mua vé phim này trước khi đánh giá.");
        }

        Review review = reviewRepository.findByMovieIdAndCustomerId(movieId, customerId)
                .orElseGet(() -> Review.builder()
                        .movie(movie)
                        .customer(customer)
                        .hidden(false)
                        .createdAt(LocalDateTime.now())
                        .build());
        review.setRating(rating);
        review.setComment(comment);
        return reviewRepository.save(review);
    }

    // ===== Quản trị: kiểm duyệt đánh giá =====

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllForAdmin() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Review r : reviewRepository.findAllForAdmin()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("rating", r.getRating());
            m.put("comment", r.getComment() != null ? r.getComment() : "");
            m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            m.put("hidden", Boolean.TRUE.equals(r.getHidden()));
            m.put("movieId", r.getMovie() != null ? r.getMovie().getId() : null);
            m.put("movieTitle", r.getMovie() != null
                    ? (r.getMovie().getTitleVietnamese() != null && !r.getMovie().getTitleVietnamese().isBlank()
                        ? r.getMovie().getTitleVietnamese() : r.getMovie().getTitle())
                    : "—");
            m.put("customerName", r.getCustomer() != null && r.getCustomer().getUser() != null
                    ? r.getCustomer().getUser().getFullName() : "Khách hàng");
            out.add(m);
        }
        return out;
    }

    @Transactional
    public boolean toggleHidden(Integer id) {
        Review r = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá."));
        r.setHidden(!Boolean.TRUE.equals(r.getHidden()));
        reviewRepository.save(r);
        return r.getHidden();
    }

    @Transactional
    public void deleteReview(Integer id) {
        if (!reviewRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy đánh giá.");
        }
        reviewRepository.deleteById(id);
    }
}
