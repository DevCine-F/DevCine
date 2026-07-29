package com.devcine.backend.repository;

import com.devcine.backend.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface MovieRepository extends JpaRepository<Movie, Integer> {
    
    @Query("SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres ORDER BY m.id DESC")
    List<Movie> findAllWithGenres();

    @Query("SELECT m FROM Movie m LEFT JOIN FETCH m.genres WHERE m.id = :id")
    Optional<Movie> findByIdWithGenres(@Param("id") Integer id);

    Optional<Movie> findBySlug(String slug);

    @Query("SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres WHERE " +
           "LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.titleVietnamese) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.director) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "EXISTS (SELECT 1 FROM m.genres g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY m.id DESC")
    List<Movie> searchMovies(@Param("keyword") String keyword);

    /** Đếm số phim đang gắn 1 thể loại — chặn xoá thể loại đang được sử dụng. */
    long countByGenres_Id(Integer categoryId);

    /** Trùng tên phim khi THÊM mới. */
    boolean existsByTitleIgnoreCase(String title);

    /** Trùng tên phim khi CẬP NHẬT (loại trừ chính nó). */
    boolean existsByTitleIgnoreCaseAndIdNot(String title, Integer id);

    /** Cập nhật trạng thái hàng loạt cho nhiều phim trong 1 query (bulk action). */
    @Modifying
    @Query("UPDATE Movie m SET m.status = :status WHERE m.id IN :ids")
    int bulkUpdateStatus(@Param("ids") List<Integer> ids, @Param("status") String status);

    /**
     * ID các phim "Sắp chiếu" đủ điều kiện tự chuyển sang "Đang chiếu":
     * đã tới ngày khởi chiếu (releaseDate <= hôm nay) VÀ có ≥1 suất chiếu.
     * Gộp 1 query để job không phải đếm suất trong vòng lặp (chống N+1).
     */
    @Query("SELECT m.id FROM Movie m WHERE LOWER(m.status) = 'upcoming' " +
           "AND m.releaseDate IS NOT NULL AND m.releaseDate <= :today " +
           "AND EXISTS (SELECT 1 FROM Showtime s WHERE s.movie = m)")
    List<Integer> findUpcomingIdsToActivate(@Param("today") LocalDate today);

    /**
     * ID các phim "Đang chiếu" cần tự chuyển sang "Ngừng chiếu":
     * <b>(</b> đã quá ngày kết thúc (endDate < hôm nay) HOẶC không còn suất chiếu nào ở tương lai <b>)</b>
     * VÀ không còn vé nào đang GIỮ CHỖ (Booking status = 'HOLD').
     *
     * <p>Guard vé đang giữ đồng bộ với chặn thủ công {@code countActiveHoldsByMovie}: không tự
     * ngừng chiếu khi khách còn giao dịch dở, tránh làm suất chiếu biến mất giữa luồng thanh toán.</p>
     */
    @Query("SELECT m.id FROM Movie m WHERE LOWER(m.status) = 'active' " +
           "AND ( (m.endDate IS NOT NULL AND m.endDate < :today) " +
           "OR NOT EXISTS (SELECT 1 FROM Showtime s WHERE s.movie = m AND s.startTime > :now) ) " +
           "AND NOT EXISTS (SELECT 1 FROM Booking b WHERE b.showtime.movie = m AND b.status = 'HOLD')")
    List<Integer> findActiveIdsToArchive(@Param("today") LocalDate today, @Param("now") LocalDateTime now);
}
