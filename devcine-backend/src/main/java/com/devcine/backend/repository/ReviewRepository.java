package com.devcine.backend.repository;

import com.devcine.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // Công khai: chỉ đánh giá KHÔNG bị ẩn (hidden = false / null)
    @Query("SELECT r FROM Review r JOIN FETCH r.customer c JOIN FETCH c.user " +
           "WHERE r.movie.id = :movieId AND (r.hidden = false OR r.hidden IS NULL) " +
           "ORDER BY r.createdAt DESC")
    List<Review> findVisibleByMovieIdWithCustomer(@Param("movieId") Integer movieId);

    @Query("SELECT r FROM Review r WHERE r.movie.id = :movieId AND r.customer.userId = :customerId")
    Optional<Review> findByMovieIdAndCustomerId(@Param("movieId") Integer movieId,
                                                @Param("customerId") Integer customerId);

    // Admin: tất cả đánh giá (kể cả đã ẩn) + JOIN FETCH khách + phim để tránh N+1
    @Query("SELECT r FROM Review r JOIN FETCH r.customer c JOIN FETCH c.user JOIN FETCH r.movie m " +
           "ORDER BY r.createdAt DESC")
    List<Review> findAllForAdmin();
}
