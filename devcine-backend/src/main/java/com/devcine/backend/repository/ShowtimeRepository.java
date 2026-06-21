package com.devcine.backend.repository;

import com.devcine.backend.entity.Movie;
import com.devcine.backend.entity.Showtime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // ===== Lịch chiếu có lọc + phân trang (trang /lich-chieu) =====

    // Suất của 1 RẠP trong khoảng [start, end]
    @Query("SELECT DISTINCT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m LEFT JOIN FETCH m.genres JOIN FETCH s.format f " +
           "WHERE c.id = :cinemaId AND s.startTime >= :start AND s.startTime <= :end ORDER BY m.title ASC, s.startTime ASC")
    List<Showtime> findByCinemaAndRange(@Param("cinemaId") Integer cinemaId,
                                        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Suất của 1 PHIM trong khoảng [start, end], lọc theo thành phố (rỗng = tất cả)
    @Query("SELECT DISTINCT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m LEFT JOIN FETCH m.genres JOIN FETCH s.format f " +
           "WHERE m.id = :movieId AND (:city = '' OR LOWER(c.city) = LOWER(:city)) AND s.startTime >= :start AND s.startTime <= :end " +
           "ORDER BY c.name ASC, s.startTime ASC")
    List<Showtime> findByMovieAndRange(@Param("movieId") Integer movieId, @Param("city") String city,
                                       @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Danh sách PHIM có suất trong (thành phố × khoảng ngày), tìm theo tên — phân trang
    @Query(value = "SELECT DISTINCT m FROM Showtime s JOIN s.movie m JOIN s.room r JOIN r.cinema c " +
            "WHERE (:city = '' OR LOWER(c.city) = LOWER(:city)) AND s.startTime >= :start AND s.startTime <= :end " +
            "AND LOWER(m.title) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY m.title ASC",
            countQuery = "SELECT COUNT(DISTINCT m) FROM Showtime s JOIN s.movie m JOIN s.room r JOIN r.cinema c " +
            "WHERE (:city = '' OR LOWER(c.city) = LOWER(:city)) AND s.startTime >= :start AND s.startTime <= :end " +
            "AND LOWER(m.title) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Movie> findMoviesWithShowtimes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                        @Param("city") String city, @Param("q") String q, Pageable pageable);
}
