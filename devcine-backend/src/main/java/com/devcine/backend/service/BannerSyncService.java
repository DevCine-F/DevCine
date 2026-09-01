package com.devcine.backend.service;

import com.devcine.backend.entity.Banner;
import com.devcine.backend.repository.BannerRepository;
import com.devcine.backend.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Đồng bộ 2 chiều giữa cờ {@code Movie.showOnBanner} và banner mode = MOVIE ở trang chủ.
 *
 * <p>Bất biến: {@code showOnBanner == true} ⟺ tồn tại banner mode = MOVIE cho phim đó.
 * <ul>
 *   <li>Bật cờ bên phim → tạo banner theo phim (nếu chưa có).</li>
 *   <li>Tắt cờ bên phim → xoá banner theo phim của phim đó.</li>
 *   <li>Thêm/sửa/xoá banner mode = MOVIE bên trang quản lý banner → cập nhật lại cờ phim.</li>
 * </ul>
 * Nhờ bất biến này, phim bật cờ sẽ tự có banner trong bảng {@code banners} và hiển thị ở trang chủ
 * (trang chủ vốn render các banner đang bật), nên không cần logic hiển thị riêng.
 */
@Service
public class BannerSyncService {

    private static final String MOVIE_MODE = "MOVIE";

    private final BannerRepository bannerRepository;
    private final MovieRepository movieRepository;

    public BannerSyncService(BannerRepository bannerRepository, MovieRepository movieRepository) {
        this.bannerRepository = bannerRepository;
        this.movieRepository = movieRepository;
    }

    /**
     * Chiều PHIM → BANNER: đảm bảo trạng thái của banner theo phim khớp với cờ {@code showOnBanner}.
     * Bật mà chưa có → tạo mới (isActive = true); bật mà đã có → kích hoạt lại (isActive = true);
     * tắt mà đang có → vô hiệu hoá (isActive = false).
     */
    @Transactional
    public void applyMovieFlag(Integer movieId, boolean show, String movieTitle) {
        if (movieId == null) return;
        List<Banner> existing = bannerRepository.findByModeAndMovieId(MOVIE_MODE, movieId);
        if (show) {
            if (existing.isEmpty()) {
                Integer maxOrder = bannerRepository.findMaxDisplayOrder();
                int nextOrder = (maxOrder != null && maxOrder > 0) ? maxOrder + 1 : 1;
                bannerRepository.save(Banner.builder()
                        .title(movieTitle)
                        .mode(MOVIE_MODE)
                        .movieId(movieId)
                        .placement("HOME")
                        .isActive(true)
                        .displayOrder(nextOrder)
                        .startDate(LocalDateTime.now())
                        .endDate(null) // mở vô thời hạn: hiển thị đến khi tắt cờ / xoá banner
                        .build());
                normalizeOrders();
            } else {
                for (Banner b : existing) {
                    if (!Boolean.TRUE.equals(b.getIsActive())) {
                        b.setIsActive(true);
                        bannerRepository.save(b);
                    }
                }
            }
        } else if (!existing.isEmpty()) {
            for (Banner b : existing) {
                if (Boolean.TRUE.equals(b.getIsActive())) {
                    b.setIsActive(false);
                    bannerRepository.save(b);
                }
            }
        }
    }

    /**
     * Chuẩn hoá thứ tự hiển thị của toàn bộ banner trong CSDL thành chuỗi số nguyên liên tục 1..N
     * theo đúng thứ tự ưu tiên (displayOrder ASC, id DESC), loại bỏ hoàn toàn các giá trị 0, âm, trùng lặp hoặc nhảy cóc.
     */
    @Transactional
    public void normalizeOrders() {
        List<Banner> all = bannerRepository.findAllOrderByDisplayOrder();
        int cur = 1;
        boolean changed = false;
        for (Banner b : all) {
            if (b.getDisplayOrder() == null || b.getDisplayOrder() != cur) {
                b.setDisplayOrder(cur);
                changed = true;
            }
            cur++;
        }
        if (changed) {
            bannerRepository.saveAll(all);
        }
    }

    /**
     * Chiều BANNER → PHIM: đồng bộ cờ {@code showOnBanner} theo việc còn tồn tại banner theo phim ĐANG BẬT hay không.
     * Chỉ ghi khi giá trị thực sự đổi để tránh update thừa.
     */
    @Transactional
    public void syncMovieFlag(Integer movieId) {
        if (movieId == null) return;
        boolean hasActive = bannerRepository.existsByModeAndMovieIdAndIsActiveTrue(MOVIE_MODE, movieId);
        movieRepository.findById(movieId).ifPresent(m -> {
            if (!Boolean.valueOf(hasActive).equals(m.getShowOnBanner())) {
                m.setShowOnBanner(hasActive);
                movieRepository.save(m);
            }
        });
    }
}
