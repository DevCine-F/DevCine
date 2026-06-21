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
    
    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m JOIN FETCH s.format f " +
           "WHERE s.movie.id = :movieId AND s.startTime >= :now " +
           "ORDER BY s.startTime ASC")
    List<Showtime> findUpcomingShowtimesByMovieId(@Param("movieId") Integer movieId, @Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m JOIN FETCH s.format f " +
           "WHERE s.movie.id = :movieId AND c.city = :city AND s.startTime >= :now " +
           "ORDER BY s.startTime ASC")
    List<Showtime> findUpcomingShowtimesByMovieIdAndCity(@Param("movieId") Integer movieId, @Param("city") String city, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m LEFT JOIN FETCH m.genres JOIN FETCH s.format f " +
           "WHERE s.startTime >= :now ORDER BY s.startTime ASC")
    List<Showtime> findUpcomingShowtimes(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH s.movie m JOIN FETCH s.format f " +
           "WHERE r.cinema.id = :cinemaId " +
           "ORDER BY s.startTime ASC")
    List<Showtime> findByCinemaId(@Param("cinemaId") Integer cinemaId);

    @Query("SELECT COUNT(s) > 0 FROM Showtime s WHERE s.room.id = :roomId " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    boolean hasConflict(@Param("roomId") Integer roomId, 
                        @Param("startTime") LocalDateTime startTime, 
                        @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COALESCE(SUM(r.matrixRow * r.matrixCol), 0) FROM Showtime s JOIN s.room r WHERE s.startTime >= :startDate AND s.startTime <= :endDate")
    long countTotalSeatsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    boolean existsByFormat_Id(Integer formatId);
}
