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

    // Banner đang hiển thị cho khách: đang bật, đúng vị trí, còn trong khoảng ngày,
    // sắp xếp theo thứ tự ưu tiên (displayOrder tăng dần, null coi như 0).
    @Query("SELECT b FROM Banner b WHERE b.isActive = true "
            + "AND (b.placement = :placement OR b.placement IS NULL) "
            + "AND (b.startDate IS NULL OR b.startDate <= :now) "
            + "AND (b.endDate IS NULL OR b.endDate >= :now) "
            + "ORDER BY COALESCE(b.displayOrder, 0) ASC, b.id DESC")
    List<Banner> findActiveBanners(@Param("placement") String placement, @Param("now") LocalDateTime now);
}
