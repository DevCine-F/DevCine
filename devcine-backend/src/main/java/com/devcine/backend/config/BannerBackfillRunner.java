package com.devcine.backend.config;

import com.devcine.backend.entity.Movie;
import com.devcine.backend.repository.BannerRepository;
import com.devcine.backend.repository.MovieRepository;
import com.devcine.backend.service.BannerSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Backfill (idempotent) cho dữ liệu phim đã tồn tại TRƯỚC khi có tính năng đồng bộ banner:
 * với mọi phim đang bật {@code showOnBanner} nhưng chưa có banner mode = MOVIE, tạo banner tương ứng.
 *
 * <p>Chạy mỗi lần khởi động nhưng chỉ tạo phần còn thiếu ({@link BannerSyncService#applyMovieFlag}
 * bỏ qua nếu phim đã có banner), nên an toàn khi lặp lại: sau lần đầu sẽ không tạo thêm gì và
 * không "hồi sinh" banner mà người dùng đã chủ động tắt (vì khi tắt, cờ cũng về false).
 */
@Component
@Order(100)
public class BannerBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BannerBackfillRunner.class);

    private final MovieRepository movieRepository;
    private final BannerRepository bannerRepository;
    private final BannerSyncService bannerSyncService;

    public BannerBackfillRunner(MovieRepository movieRepository,
                                BannerRepository bannerRepository,
                                BannerSyncService bannerSyncService) {
        this.movieRepository = movieRepository;
        this.bannerRepository = bannerRepository;
        this.bannerSyncService = bannerSyncService;
    }

    @Override
    public void run(String... args) {
        int created = 0, already = 0;
        for (Movie m : movieRepository.findAll()) {
            if (!Boolean.TRUE.equals(m.getShowOnBanner())) continue;
            boolean had = bannerRepository.existsByModeAndMovieId("MOVIE", m.getId());
            bannerSyncService.applyMovieFlag(m.getId(), true, m.getTitle());
            if (had) already++; else created++;
        }
        if (created > 0 || already > 0) {
            log.info("[BannerBackfill] Phim bật showOnBanner — tạo mới {} banner theo phim, {} đã có sẵn.",
                    created, already);
        }
    }
}
