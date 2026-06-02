package com.devcine.backend.repository;

import com.devcine.backend.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Integer> {
    
    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c " +
           "WHERE s.movie.id = :movieId AND s.startTime >= :now " +
           "ORDER BY s.startTime ASC")
    List<Showtime> findUpcomingShowtimesByMovieId(@Param("movieId") Integer movieId, @Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c " +
           "WHERE s.movie.id = :movieId AND c.city = :city AND s.startTime >= :now " +
           "ORDER BY s.startTime ASC")
    List<Showtime> findUpcomingShowtimesByMovieIdAndCity(@Param("movieId") Integer movieId, @Param("city") String city, @Param("now") LocalDateTime now);
}
