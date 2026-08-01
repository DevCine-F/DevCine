package com.devcine.backend.scheduler;

import com.devcine.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kích hoạt TỰ ĐỘNG ĐỒNG BỘ TRẠNG THÁI PHIM theo NGÀY (3 quy tắc: hết hạn → archived,
 * đến ngày chiếu → active, chưa chiếu → upcoming). Toàn bộ nghiệp vụ nằm ở
 * {@link MovieService#autoSyncMovieStatuses()}; lớp này chỉ kích hoạt + ghi log.
 *
 * <p>Được gọi ở 2 mốc (mốc thứ 3 — lười theo request — nằm ngay trong service):</p>
 * <ul>
 *   <li><b>Khởi động backend</b> ({@code ApplicationReadyEvent}) — bù ngay dữ liệu lệch khi vừa lên.</li>
 *   <li><b>Cron 00:00 mỗi ngày</b> (giờ VN) — lật trạng thái đúng thời điểm sang ngày mới.</li>
 * </ul>
 *
 * <p>Admin vẫn đổi trạng thái thủ công; auto-sync TÔN TRỌNG 'archived' thủ công, chỉ bù các phim
 * lệch theo ngày mà admin quên chuyển.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovieStatusScheduler {

    private final MovieService movieService;

    /** Đồng bộ ngay khi backend vừa sẵn sàng (bù dữ liệu lệch tồn đọng). */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSync("khởi động");
    }

    /** Đồng bộ ngầm định kỳ 00:00 mỗi ngày (giờ VN). */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void syncDaily() {
        runSync("cron 00:00");
    }

    private void runSync(String trigger) {
        try {
            Map<String, Integer> r = movieService.autoSyncMovieStatuses();
            int archived = r.getOrDefault("archived", 0);
            int activated = r.getOrDefault("activated", 0);
            int upcoming = r.getOrDefault("upcoming", 0);
            if (archived > 0 || activated > 0 || upcoming > 0) {
                log.info("Auto-sync trạng thái phim ({}): {} -> Ngừng chiếu, {} -> Đang chiếu, {} -> Sắp chiếu.",
                        trigger, archived, activated, upcoming);
            } else {
                log.debug("Auto-sync trạng thái phim ({}): không có thay đổi.", trigger);
            }
        } catch (Exception e) {
            log.error("Lỗi khi tự đồng bộ trạng thái phim ({}): {}", trigger, e.getMessage(), e);
        }
    }
}
