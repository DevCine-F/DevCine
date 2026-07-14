package com.devcine.backend.repository;

import com.devcine.backend.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Integer> {
    List<Banner> findAllByOrderByIdDesc();

    // Banner "theo phim" gắn với một phim cụ thể — dùng cho đồng bộ 2 chiều với Movie.showOnBanner.
    List<Banner> findByModeAndMovieId(String mode, Integer movieId);

    boolean existsByModeAndMovieId(String mode, Integer movieId);

    // Banner đang hiển thị cho khách: đang bật, đúng vị trí, còn trong khoảng ngày,
    // sắp xếp theo thứ tự ưu tiên (displayOrder tăng dần, null coi như 0).
    //
    // Tự động ẩn banner theo phim khi phim không còn khả dụng: với banner mode = MOVIE,
    // chỉ hiển thị nếu vẫn tồn tại phim tương ứng và phim đang chiếu (status = active).
    // -> Phim bị xoá (không còn dòng Movie) hoặc ngừng chiếu (archived) sẽ tự động biến mất
    //    khỏi trang chủ mà không cần ai vào sửa/xoá banner.
    @Query("SELECT b FROM Banner b WHERE b.isActive = true "
            + "AND (b.placement = :placement OR b.placement IS NULL) "
            + "AND (b.startDate IS NULL OR b.startDate <= :now) "
            + "AND (b.endDate IS NULL OR b.endDate >= :now) "
            + "AND (b.mode IS NULL OR b.mode <> 'MOVIE' OR EXISTS ("
            + "    SELECT 1 FROM Movie m WHERE m.id = b.movieId AND lower(m.status) = 'active')) "
            + "ORDER BY COALESCE(b.displayOrder, 0) ASC, b.id DESC")
    List<Banner> findActiveBanners(@Param("placement") String placement, @Param("now") LocalDateTime now);
}
