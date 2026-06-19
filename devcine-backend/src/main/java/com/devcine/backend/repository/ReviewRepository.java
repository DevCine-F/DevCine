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

    @Query("SELECT r FROM Review r JOIN FETCH r.customer c JOIN FETCH c.user " +
           "WHERE r.movie.id = :movieId ORDER BY r.createdAt DESC")
    List<Review> findByMovieIdWithCustomer(@Param("movieId") Integer movieId);

    @Query("SELECT r FROM Review r WHERE r.movie.id = :movieId AND r.customer.userId = :customerId")
    Optional<Review> findByMovieIdAndCustomerId(@Param("movieId") Integer movieId,
                                                @Param("customerId") Integer customerId);
}
