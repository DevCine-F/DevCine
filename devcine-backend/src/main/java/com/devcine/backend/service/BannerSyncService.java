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
     * Chiều PHIM → BANNER: đảm bảo sự tồn tại của banner theo phim khớp với cờ {@code showOnBanner}.
     * Bật mà chưa có → tạo mới; tắt mà đang có → xoá hẳn (theo lựa chọn "xoá khi tắt").
     */
    @Transactional
    public void applyMovieFlag(Integer movieId, boolean show, String movieTitle) {
        if (movieId == null) return;
        List<Banner> existing = bannerRepository.findByModeAndMovieId(MOVIE_MODE, movieId);
        if (show) {
            if (existing.isEmpty()) {
                bannerRepository.save(Banner.builder()
                        .title(movieTitle)
                        .mode(MOVIE_MODE)
                        .movieId(movieId)
                        .placement("HOME")
                        .isActive(true)
                        .displayOrder(0)
                        .startDate(LocalDateTime.now())
                        .endDate(null) // mở vô thời hạn: hiển thị đến khi tắt cờ / xoá banner
                        .build());
            }
        } else if (!existing.isEmpty()) {
            bannerRepository.deleteAll(existing);
        }
    }

    /**
     * Chiều BANNER → PHIM: đồng bộ cờ {@code showOnBanner} theo việc còn tồn tại banner theo phim hay không.
     * Chỉ ghi khi giá trị thực sự đổi để tránh update thừa.
     */
    @Transactional
    public void syncMovieFlag(Integer movieId) {
        if (movieId == null) return;
        boolean has = bannerRepository.existsByModeAndMovieId(MOVIE_MODE, movieId);
        movieRepository.findById(movieId).ifPresent(m -> {
            if (!Boolean.valueOf(has).equals(m.getShowOnBanner())) {
                m.setShowOnBanner(has);
                movieRepository.save(m);
            }
        });
    }
}
