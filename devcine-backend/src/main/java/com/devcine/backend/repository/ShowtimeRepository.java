package com.devcine.backend.repository;

import com.devcine.backend.entity.Movie;
import com.devcine.backend.entity.Showtime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Integer> {

    /**
     * Nạp suất chiếu kèm khóa ghi bi quan (SELECT ... FOR UPDATE) — dùng khi giữ ghế ở POS/booking
     * để tuần tự hóa các lệnh giữ ghế cùng một suất, chống bán trùng do race check-then-act.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Showtime s WHERE s.id = :id")
    Optional<Showtime> findByIdForUpdate(@Param("id") Integer id);


    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m JOIN FETCH s.format f " +
           "WHERE s.movie.id = :movieId AND m.status = 'active' AND s.startTime >= :now " +
           "ORDER BY s.startTime ASC")
    List<Showtime> findUpcomingShowtimesByMovieId(@Param("movieId") Integer movieId, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m JOIN FETCH s.format f " +
           "WHERE s.movie.id = :movieId AND m.status = 'active' AND c.city = :city AND s.startTime >= :now " +
           "ORDER BY s.startTime ASC")
    List<Showtime> findUpcomingShowtimesByMovieIdAndCity(@Param("movieId") Integer movieId, @Param("city") String city, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m LEFT JOIN FETCH m.genres JOIN FETCH s.format f " +
           "WHERE m.status = 'active' AND s.startTime >= :now ORDER BY s.startTime ASC")
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

    /**
     * Như {@link #hasConflict} nhưng BỎ QUA chính suất đang sửa (excludeId) — dùng khi kéo-thả/PATCH
     * đổi giờ/phòng: nếu không loại trừ, suất tự chồng lên chính nó và luôn báo trùng.
     */
    @Query("SELECT COUNT(s) > 0 FROM Showtime s WHERE s.room.id = :roomId AND s.id <> :excludeId " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    boolean hasConflictExcluding(@Param("roomId") Integer roomId,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime,
                                 @Param("excludeId") Integer excludeId);

    /**
     * Nạp MỘT lần toàn bộ suất của các phòng có giao với cửa sổ [start, end) — phục vụ
     * tạo lịch hàng loạt, kiểm tra trùng in-memory thay vì gọi hasConflict từng suất (chống N+1).
     */
    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r WHERE r.id IN :roomIds " +
           "AND s.startTime < :end AND s.endTime > :start")
    List<Showtime> findByRoomsAndWindow(@Param("roomIds") List<Integer> roomIds,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    // Phòng có suất chiếu nào không (guard khi sửa kích thước / xoá phòng)
    boolean existsByRoom_Id(Integer roomId);

    // Dashboard: :cinemaId = null -> toàn hệ thống (chỉ ADMIN), khác null -> chỉ cơ sở đó
    @Query("SELECT COALESCE(SUM(r.matrixRow * r.matrixCol), 0) FROM Showtime s JOIN s.room r "
           + "WHERE s.startTime >= :startDate AND s.startTime <= :endDate "
           + "AND (:cinemaId IS NULL OR r.cinema.id = :cinemaId)")
    long countTotalSeatsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
                                    @Param("cinemaId") Integer cinemaId);

    // Suất chiếu trong khoảng [start, end] kèm phòng/rạp/phim (cho dashboard)
    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m " +
           "WHERE s.startTime >= :start AND s.startTime <= :end ORDER BY s.startTime ASC")
    List<Showtime> findByRangeWithDetails(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Như trên nhưng lấy N suất gần hiện tại nhất (khoảng dài như Tháng có thể hàng trăm suất)
    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m " +
           "WHERE s.startTime >= :start AND s.startTime <= :end " +
           "AND (:cinemaId IS NULL OR c.id = :cinemaId) ORDER BY s.startTime DESC")
    List<Showtime> findLatestByRangeWithDetails(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end,
                                                @Param("cinemaId") Integer cinemaId,
                                                Pageable pageable);

    boolean existsByFormat_Id(Integer formatId);

    // ===== Thống kê theo phim (modal chi tiết Quản lý Phim) =====

    /** Tổng số suất chiếu đã lên lịch cho 1 phim. */
    @Query("SELECT COUNT(s) FROM Showtime s WHERE s.movie.id = :movieId")
    long countByMovieId(@Param("movieId") Integer movieId);

    /** Số suất chiếu CÒN HOẠT ĐỘNG (chưa diễn ra) của 1 phim — chặn xoá/ngừng chiếu/ẩn. */
    @Query("SELECT COUNT(s) FROM Showtime s WHERE s.movie.id = :movieId AND s.startTime > :now")
    long countFutureByMovieId(@Param("movieId") Integer movieId, @Param("now") LocalDateTime now);

    /** Sức chứa của các suất ĐÃ diễn ra (startTime <= now) — mẫu số cho tỷ lệ lấp đầy. */
    @Query("SELECT COALESCE(SUM(r.matrixRow * r.matrixCol), 0) FROM Showtime s JOIN s.room r " +
           "WHERE s.movie.id = :movieId AND s.startTime <= :now")
    long sumPastCapacityByMovie(@Param("movieId") Integer movieId, @Param("now") LocalDateTime now);

    // ===== Lịch chiếu có lọc + phân trang (trang /lich-chieu) =====

    // Suất của 1 RẠP trong khoảng [start, end] — chỉ phim đang chiếu (ẩn phim đã lưu trữ)
    @Query("SELECT DISTINCT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m LEFT JOIN FETCH m.genres JOIN FETCH s.format f " +
           "WHERE c.id = :cinemaId AND m.status = 'active' AND s.startTime >= :start AND s.startTime <= :end ORDER BY m.title ASC, s.startTime ASC")
    List<Showtime> findByCinemaAndRange(@Param("cinemaId") Integer cinemaId,
                                        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Suất của 1 PHIM trong khoảng [start, end], lọc theo thành phố (rỗng = tất cả) — chỉ phim đang chiếu
    @Query("SELECT DISTINCT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m LEFT JOIN FETCH m.genres JOIN FETCH s.format f " +
           "WHERE m.id = :movieId AND m.status = 'active' AND (:city = '' OR LOWER(c.city) = LOWER(:city)) AND s.startTime >= :start AND s.startTime <= :end " +
           "ORDER BY c.name ASC, s.startTime ASC")
    List<Showtime> findByMovieAndRange(@Param("movieId") Integer movieId, @Param("city") String city,
                                       @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Danh sách PHIM có suất trong (thành phố × khoảng ngày), tìm theo tên — phân trang — chỉ phim đang chiếu
    @Query(value = "SELECT DISTINCT m FROM Showtime s JOIN s.movie m JOIN s.room r JOIN r.cinema c " +
            "WHERE m.status = 'active' AND (:city = '' OR LOWER(c.city) = LOWER(:city)) AND s.startTime >= :start AND s.startTime <= :end " +
            "AND LOWER(m.title) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY m.title ASC",
            countQuery = "SELECT COUNT(DISTINCT m) FROM Showtime s JOIN s.movie m JOIN s.room r JOIN r.cinema c " +
            "WHERE m.status = 'active' AND (:city = '' OR LOWER(c.city) = LOWER(:city)) AND s.startTime >= :start AND s.startTime <= :end " +
            "AND LOWER(m.title) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Movie> findMoviesWithShowtimes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                        @Param("city") String city, @Param("q") String q, Pageable pageable);
}
