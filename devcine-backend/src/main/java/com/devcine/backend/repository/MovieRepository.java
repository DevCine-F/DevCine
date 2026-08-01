package com.devcine.backend.repository;

import com.devcine.backend.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
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
     * Đếm số phim (trong tập id) CHƯA có suất chiếu nào — 1 query gộp (chống N+1).
     * Dùng để nhắc nhẹ admin "nhớ cấu hình suất chiếu" sau khi bật ĐANG CHIẾU, KHÔNG chặn thao tác.
     */
    @Query("SELECT COUNT(m) FROM Movie m WHERE m.id IN :ids " +
           "AND NOT EXISTS (SELECT 1 FROM Showtime s WHERE s.movie = m)")
    long countWithoutShowtimes(@Param("ids") List<Integer> ids);

    // ─────────────────────────────────────────────────────────────────────────────
    //  AUTO-SYNC TRẠNG THÁI THEO NGÀY — 3 quy tắc, mỗi quy tắc 1 bulk UPDATE (chống N+1).
    //  clearAutomatically/flushAutomatically: đồng bộ persistence context để query đọc
    //  ngay sau đó (getNowShowing/getUpcoming) không thấy dữ liệu cũ.
    //  Mọi quy tắc TÔN TRỌNG 'archived' thủ công (điều kiện status <> 'archived').
    // ─────────────────────────────────────────────────────────────────────────────

    /** QUY TẮC 1 — HẾT HẠN: endDate &lt; today ⇒ 'archived'. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Movie m SET m.status = 'archived' " +
           "WHERE m.endDate IS NOT NULL AND m.endDate < :today " +
           "AND LOWER(m.status) <> 'archived'")
    int syncArchiveExpired(@Param("today") LocalDate today);

    /** QUY TẮC 2 — ĐẾN NGÀY CHIẾU: releaseDate &lt;= today AND (endDate null OR endDate &gt;= today) AND chưa archived ⇒ 'active'. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Movie m SET m.status = 'active' " +
           "WHERE m.releaseDate IS NOT NULL AND m.releaseDate <= :today " +
           "AND (m.endDate IS NULL OR m.endDate >= :today) " +
           "AND LOWER(m.status) <> 'archived' AND LOWER(m.status) <> 'active'")
    int syncActivateReleased(@Param("today") LocalDate today);

    /** QUY TẮC 3 — CHƯA CHIẾU: releaseDate &gt; today AND chưa archived ⇒ 'upcoming'. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Movie m SET m.status = 'upcoming' " +
           "WHERE m.releaseDate IS NOT NULL AND m.releaseDate > :today " +
           "AND LOWER(m.status) <> 'archived' AND LOWER(m.status) <> 'upcoming'")
    int syncUpcomingFuture(@Param("today") LocalDate today);
}
