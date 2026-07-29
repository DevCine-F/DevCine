package com.devcine.backend.scheduler;

import com.devcine.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Tự động đồng bộ trạng thái phim theo lịch phát hành mỗi ngày lúc 00:01 (giờ VN).
 *
 * <ul>
 *   <li><b>"Sắp chiếu" → "Đang chiếu":</b> đã tới ngày khởi chiếu (releaseDate) &amp; có ≥1 suất chiếu.</li>
 *   <li><b>"Đang chiếu" → "Ngừng chiếu":</b> đã quá ngày kết thúc (endDate) HOẶC không còn suất chiếu tương lai.</li>
 * </ul>
 *
 * <p>Admin vẫn đổi trạng thái thủ công cho các ca ngoại lệ (mở bán sneak show, ngừng chiếu sớm...);
 * job này chỉ bù những phim admin quên chuyển, KHÔNG ghi đè ý định thủ công.</p>
 *
 * <p>Toàn bộ logic nghiệp vụ nằm ở {@link MovieService#autoSyncStatuses}; scheduler chỉ kích hoạt,
 * cấp mốc thời gian theo múi giờ VN và ghi log kết quả.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovieStatusScheduler {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MovieService movieService;

    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void syncMovieStatuses() {
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        try {
            Map<String, Integer> result = movieService.autoSyncStatuses(today, now);
            int activated = result.getOrDefault("activated", 0);
            int archived = result.getOrDefault("archived", 0);
            if (activated > 0 || archived > 0) {
                log.info("Auto-sync trạng thái phim: {} phim -> Đang chiếu, {} phim -> Ngừng chiếu.",
                        activated, archived);
            } else {
                log.debug("Auto-sync trạng thái phim: không có thay đổi.");
            }
        } catch (Exception e) {
            log.error("Lỗi khi tự đồng bộ trạng thái phim theo lịch: {}", e.getMessage(), e);
        }
    }
}
