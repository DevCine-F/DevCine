package com.devcine.backend.repository;

import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.dto.projection.ShowtimePublicProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    /** Suất cũ chưa có snapshot sơ đồ (trước migration) — dùng để backfill 1 lần. JOIN FETCH room tránh N+1. */
    @Query("SELECT s FROM Showtime s JOIN FETCH s.room WHERE s.layoutData IS NULL")
    List<Showtime> findWithoutLayoutSnapshot();

    /** Suất CHƯA KẾT THÚC (đang mở bán) của một phòng — dùng re-sync snapshot khi admin sửa sơ đồ. */
    @Query("SELECT s FROM Showtime s WHERE s.room.id = :roomId AND s.endTime >= :now AND s.status <> 'Cancelled'")
    List<Showtime> findActiveByRoomId(@Param("roomId") Integer roomId, @Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT c FROM Showtime s JOIN s.room r JOIN r.cinema c JOIN s.movie m " +
           "WHERE (m.status = 'active' OR (m.status = 'upcoming' AND s.status = 'Xuất chiếu sớm')) " +
           "AND s.startTime >= :now AND s.status <> 'Cancelled' " +
           "AND (c.status IS NULL OR c.status = 'ACTIVE') " +
           "ORDER BY c.city ASC, c.name ASC")
    List<Cinema> findCinemasWithUpcomingShowtimes(@Param("now") LocalDateTime now);

    @Query("SELECT s.id AS id, s.startTime AS startTime, s.endTime AS endTime, s.status AS status, " +
           "c.id AS cinemaId, c.name AS cinemaName, c.address AS cinemaAddress, c.city AS cinemaCity, " +
           "m.id AS movieId, m.title AS movieTitle, m.titleVietnamese AS movieTitleVietnamese, " +
           "m.durationMins AS movieDurationMins, m.posterUrl AS moviePosterUrl, m.ageRating AS movieAgeRating, " +
           "m.country AS movieCountry, m.releaseDate AS movieReleaseDate, m.description AS movieDescription, " +
           "m.rating AS movieRating, m.ratingCount AS movieRatingCount, m.trailerUrl AS movieTrailerUrl, " +
           "f.id AS formatId, f.name AS formatName, " +
           "r.id AS roomId, r.name AS roomName, r.type AS roomType " +
           "FROM Showtime s JOIN s.room r JOIN r.cinema c JOIN s.movie m JOIN s.format f " +
           "WHERE (m.status = 'active' OR (m.status = 'upcoming' AND s.status = 'Xuất chiếu sớm')) " +
           "AND s.startTime >= :now AND s.status <> 'Cancelled' " +
           "AND (c.status IS NULL OR c.status = 'ACTIVE') " +
           "ORDER BY s.startTime ASC")
    List<ShowtimePublicProjection> findAllUpcomingProjections(@Param("now") LocalDateTime now);

    @Query("SELECT s.id AS id, s.startTime AS startTime, s.endTime AS endTime, s.status AS status, " +
           "c.id AS cinemaId, c.name AS cinemaName, c.address AS cinemaAddress, c.city AS cinemaCity, " +
           "m.id AS movieId, m.title AS movieTitle, m.titleVietnamese AS movieTitleVietnamese, " +
           "m.durationMins AS movieDurationMins, m.posterUrl AS moviePosterUrl, m.ageRating AS movieAgeRating, " +
           "m.country AS movieCountry, m.releaseDate AS movieReleaseDate, m.description AS movieDescription, " +
           "m.rating AS movieRating, m.ratingCount AS movieRatingCount, m.trailerUrl AS movieTrailerUrl, " +
           "f.id AS formatId, f.name AS formatName, " +
           "r.id AS roomId, r.name AS roomName, r.type AS roomType " +
           "FROM Showtime s JOIN s.room r JOIN r.cinema c JOIN s.movie m JOIN s.format f " +
           "WHERE c.id = :cinemaId " +
           "AND (m.status = 'active' OR (m.status = 'upcoming' AND s.status = 'Xuất chiếu sớm')) " +
           "AND s.startTime >= :now AND s.status <> 'Cancelled' " +
           "AND (c.status IS NULL OR c.status = 'ACTIVE') " +
           "ORDER BY s.startTime ASC")
    List<ShowtimePublicProjection> findUpcomingProjectionsByCinemaId(@Param("cinemaId") Integer cinemaId, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m JOIN FETCH s.format f " +
           "WHERE s.movie.id = :movieId " +
           "AND (m.status = 'active' OR (m.status = 'upcoming' AND s.status = 'Xuất chiếu sớm')) " +
           "AND s.startTime >= :now AND s.status <> 'Cancelled' " +
           "AND (c.status IS NULL OR c.status = 'ACTIVE') " +
           "ORDER BY s.startTime ASC")
    List<Showtime> findUpcomingShowtimesByMovieId(@Param("movieId") Integer movieId, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m JOIN FETCH s.format f " +
           "WHERE s.movie.id = :movieId AND c.city = :city " +
           "AND (m.status = 'active' OR (m.status = 'upcoming' AND s.status = 'Xuất chiếu sớm')) " +
           "AND s.startTime >= :now AND s.status <> 'Cancelled' " +
           "AND (c.status IS NULL OR c.status = 'ACTIVE') " +
           "ORDER BY s.startTime ASC")
    List<Showtime> findUpcomingShowtimesByMovieIdAndCity(@Param("movieId") Integer movieId, @Param("city") String city, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m LEFT JOIN FETCH m.genres JOIN FETCH s.format f " +
           "WHERE (m.status = 'active' OR (m.status = 'upcoming' AND s.status = 'Xuất chiếu sớm')) " +
           "AND s.startTime >= :now AND s.status <> 'Cancelled' " +
           "AND (c.status IS NULL OR c.status = 'ACTIVE') " +
           "ORDER BY s.startTime ASC")
    List<Showtime> findUpcomingShowtimes(@Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m LEFT JOIN FETCH m.genres JOIN FETCH s.format f " +
           "WHERE r.cinema.id = :cinemaId " +
           "ORDER BY s.startTime ASC")
    List<Showtime> findByCinemaId(@Param("cinemaId") Integer cinemaId);

    @Query("SELECT s FROM Showtime s JOIN FETCH s.room r " +
           "WHERE r.cinema.id = :cinemaId AND s.endTime >= :now AND (s.status IS NULL OR s.status <> 'Cancelled')")
    List<Showtime> findFutureShowtimesByCinema(@Param("cinemaId") Integer cinemaId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM Showtime s WHERE s.room.cinema.id = :cinemaId AND s.endTime >= :now AND (s.status IS NULL OR s.status <> 'Cancelled')")
    long countFutureShowtimesByCinema(@Param("cinemaId") Integer cinemaId, @Param("now") LocalDateTime now);

    /**
     * Hủy HÀNG LOẠT các suất chiếu TƯƠNG LAI (startTime >= now) của một cụm rạp khi rạp
     * đóng cửa đột xuất. Set status = 'Cancelled' (đúng chữ hoa/thường mà các query loại-trừ
     * hiện có dùng: {@code s.status <> 'Cancelled'}) để suất đã hủy không còn được coi là active.
     *
     * <p>Dùng subquery theo Room để bulk-update JPQL không phải join qua nhiều bậc (an toàn mọi
     * provider). {@code flushAutomatically=true} để đẩy thay đổi entity đang chờ trước khi chạy;
     * {@code clearAutomatically=false} để KHÔNG detach {@code cinema} đang thao tác (tránh
     * LazyInitializationException khi dựng response ngay sau đó).</p>
     *
     * @return số suất đã bị hủy
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Showtime s SET s.status = 'Cancelled' " +
           "WHERE s.startTime >= :now AND s.status <> 'Cancelled' " +
           "AND s.room.id IN (SELECT r.id FROM Room r WHERE r.cinema.id = :cinemaId)")
    int cancelFutureShowtimesByCinema(@Param("cinemaId") Integer cinemaId, @Param("now") LocalDateTime now);

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

    /** Số suất chiếu của 1 phòng chiếu tính từ thời điểm hiện tại trở đi (đang chiếu hoặc sắp chiếu). */
    @Query("SELECT COUNT(s) FROM Showtime s WHERE s.room.id = :roomId AND s.endTime >= :now AND s.status <> 'Cancelled'")
    long countByRoomIdAndEndTimeAfter(@Param("roomId") Integer roomId, @Param("now") LocalDateTime now);

    /** Số suất chiếu CÒN HOẠT ĐỘNG của 1 phòng chiếu (chỉ tính tương lai chưa diễn ra) */
    @Query("SELECT COUNT(s) FROM Showtime s WHERE s.room.id = :roomId AND s.startTime > :now AND s.status <> 'Cancelled'")
    long countFutureByRoomId(@Param("roomId") Integer roomId, @Param("now") LocalDateTime now);

    /** Sức chứa của các suất ĐÃ diễn ra (startTime <= now) — mẫu số cho tỷ lệ lấp đầy. */
    @Query("SELECT COALESCE(SUM(r.matrixRow * r.matrixCol), 0) FROM Showtime s JOIN s.room r " +
           "WHERE s.movie.id = :movieId AND s.startTime <= :now")
    long sumPastCapacityByMovie(@Param("movieId") Integer movieId, @Param("now") LocalDateTime now);

    // ===== Lịch chiếu có lọc + phân trang (trang /lich-chieu) =====

    // Suất của 1 RẠP trong khoảng [start, end] — chỉ phim đang chiếu (ẩn phim đã lưu trữ)
    // cinema đã JOIN FETCH (tránh N+1 khi toPublicDTO gọi getRoom().getCinema())
    @Query("SELECT DISTINCT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m LEFT JOIN FETCH m.genres JOIN FETCH s.format f " +
           "WHERE c.id = :cinemaId " +
           "AND (m.status = 'active' OR (m.status = 'upcoming' AND s.status = 'Xuất chiếu sớm')) " +
           "AND (c.status IS NULL OR c.status = 'ACTIVE') AND s.startTime >= :start AND s.startTime <= :end ORDER BY m.title ASC, s.startTime ASC")
    List<Showtime> findByCinemaAndRange(@Param("cinemaId") Integer cinemaId,
                                        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Suất của 1 PHIM trong khoảng [start, end], lọc theo thành phố (rỗng = tất cả) — chỉ phim đang chiếu
    // cinema đã JOIN FETCH (tránh N+1 khi toPublicDTO gọi getRoom().getCinema())
    @Query("SELECT DISTINCT s FROM Showtime s JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.movie m LEFT JOIN FETCH m.genres JOIN FETCH s.format f " +
           "WHERE m.id = :movieId " +
           "AND (m.status = 'active' OR (m.status = 'upcoming' AND s.status = 'Xuất chiếu sớm')) " +
           "AND (c.status IS NULL OR c.status = 'ACTIVE') AND (:city = '' OR LOWER(c.city) = LOWER(:city)) AND s.startTime >= :start AND s.startTime <= :end " +
           "ORDER BY c.name ASC, s.startTime ASC")
    List<Showtime> findByMovieAndRange(@Param("movieId") Integer movieId, @Param("city") String city,
                                       @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Danh sách PHIM có suất trong (thành phố × khoảng ngày), tìm theo tên — phân trang — phim đang chiếu & phim có xuất chiếu sớm
    @Query(value = "SELECT DISTINCT m FROM Showtime s JOIN s.movie m JOIN s.room r JOIN r.cinema c " +
            "WHERE (m.status = 'active' OR (m.status = 'upcoming' AND s.status = 'Xu\u1ea5t chi\u1ebfu s\u1edbm')) " +
            "AND (c.status IS NULL OR c.status = 'ACTIVE') AND (:city = '' OR LOWER(c.city) = LOWER(:city)) AND s.startTime >= :start AND s.startTime <= :end " +
            "AND LOWER(m.title) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY m.title ASC",
            countQuery = "SELECT COUNT(DISTINCT m) FROM Showtime s JOIN s.movie m JOIN s.room r JOIN r.cinema c " +
            "WHERE (m.status = 'active' OR (m.status = 'upcoming' AND s.status = 'Xu\u1ea5t chi\u1ebfu s\u1edbm')) " +
            "AND (c.status IS NULL OR c.status = 'ACTIVE') AND (:city = '' OR LOWER(c.city) = LOWER(:city)) AND s.startTime >= :start AND s.startTime <= :end " +
            "AND LOWER(m.title) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Movie> findMoviesWithShowtimes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                        @Param("city") String city, @Param("q") String q, Pageable pageable);

    @Query("SELECT DISTINCT s FROM Showtime s LEFT JOIN FETCH s.movie LEFT JOIN FETCH s.room r LEFT JOIN FETCH r.cinema LEFT JOIN FETCH s.format WHERE s.startTime >= :fromTime ORDER BY s.startTime ASC")
    List<Showtime> findPOSShowtimesWithDetails(@Param("fromTime") LocalDateTime fromTime);

    /**
     * Trả tập hợp movieId của các phim CÓ ÍT NHẤT 1 suất chiếu sớm còn mở bán
     * (status = 'Xuất chiếu sớm' và startTime >= now).
     * Dùng bởi {@link com.devcine.backend.service.MovieService} để populate
     * {@code hasEarlyScreening} trong MovieSummaryDTO mà không gây N+1.
     */
    @Query("SELECT DISTINCT s.movie.id FROM Showtime s " +
           "WHERE s.status = 'Xuất chiếu sớm' AND s.startTime >= :now")
    java.util.Set<Integer> findMovieIdsWithEarlyScreening(@Param("now") LocalDateTime now);

    /**
     * Lấy toàn bộ các suất chiếu sớm còn hiệu lực cùng với thông tin phim, rạp và phòng.
     * Dùng cho khung Banner Sneak Preview trang chủ.
     */
    @Query("SELECT DISTINCT s FROM Showtime s JOIN FETCH s.movie m LEFT JOIN FETCH m.genres JOIN FETCH s.room r JOIN FETCH r.cinema c JOIN FETCH s.format f " +
           "WHERE s.status = 'Xuất chiếu sớm' AND s.startTime >= :now AND (c.status IS NULL OR c.status = 'ACTIVE') " +
           "ORDER BY s.startTime ASC")
    List<Showtime> findActiveEarlyShowtimes(@Param("now") LocalDateTime now);
}

