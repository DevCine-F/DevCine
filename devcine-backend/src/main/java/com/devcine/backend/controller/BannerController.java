package com.devcine.backend.controller;

import com.devcine.backend.entity.Banner;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.repository.BannerRepository;
import com.devcine.backend.repository.MovieRepository;
import com.devcine.backend.service.BannerSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BannerController {

    private final BannerRepository bannerRepository;
    private final MovieRepository movieRepository;
    private final BannerSyncService bannerSyncService;

    // Loại bỏ mọi thẻ HTML (chống XSS/injection qua tiêu đề banner)
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    @GetMapping
    public ResponseEntity<?> getAllBanners() {
        return ResponseEntity.ok(bannerRepository.findAllByOrderByIdDesc());
    }

    // Công khai: banner đang hiển thị cho khách (đang bật + còn hạn), dùng cho trang chủ.
    @GetMapping("/active")
    public ResponseEntity<?> getActiveBanners(@RequestParam(defaultValue = "HOME") String placement) {
        return ResponseEntity.ok(bannerRepository.findActiveBanners(placement, LocalDateTime.now()));
    }

    @PostMapping
    @PreAuthorize("@perm.can('banners','add')")
    public ResponseEntity<?> createBanner(@RequestBody Map<String, Object> body) {
        try {
            String mode = (String) body.getOrDefault("mode", "IMAGE");
            Integer movieId = parseMovieId(body.get("movieId"));

            // Edge case 1: banner theo phim -> phim phải còn tồn tại & còn khả dụng (không bị xoá/ngừng chiếu).
            if ("MOVIE".equalsIgnoreCase(mode)) {
                String movieErr = checkMovieAvailable(movieId);
                if (movieErr != null) return badRequest(movieErr);
            }

            // Edge case 3: thứ tự ưu tiên phải là số nguyên dương.
            Integer order = parsePriority(body.get("order"));

            // Edge case 2: ngày hợp lệ & ngày kết thúc phải sau ngày bắt đầu.
            LocalDateTime startDate = parseDate(body.get("startDate"), "ngày bắt đầu");
            LocalDateTime endDate = parseDate(body.get("endDate"), "ngày kết thúc");
            if (startDate == null) startDate = LocalDateTime.now();
            if (endDate == null) endDate = LocalDateTime.now().plusMonths(1);
            if (!endDate.isAfter(startDate)) return badRequest("Ngày kết thúc phải sau ngày bắt đầu.");

            Banner banner = Banner.builder()
                    .title(sanitizeTitle((String) body.get("title"))) // Edge case 4: loại bỏ HTML/script khỏi tiêu đề
                    .imageUrl((String) body.get("imageUrl"))
                    .link((String) body.get("link"))
                    .mode(mode)
                    .movieId(movieId)
                    .placement((String) body.getOrDefault("placement", "HOME"))
                    .isActive(body.get("isActive") == null || Boolean.TRUE.equals(body.get("isActive")))
                    .displayOrder(order != null ? order : 0)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
            bannerRepository.save(banner);
            // Banner theo phim -> bật cờ showOnBanner của phim tương ứng.
            bannerSyncService.syncMovieFlag(banner.getMovieId());
            return ResponseEntity.status(201).body(Map.of("success", true, "data", banner));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('banners','edit')")
    public ResponseEntity<?> updateBanner(@PathVariable Integer id,
                                           @RequestBody Map<String, Object> body) {
        try {
            Banner banner = bannerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy banner"));
            Integer oldMovieId = banner.getMovieId(); // phim gắn trước khi sửa (để đồng bộ lại cờ nếu đổi phim/chế độ)

            // Edge case 1: nếu cập nhật liên quan phim (đổi phim hoặc đổi sang chế độ MOVIE) -> phim phải còn khả dụng.
            String effMode = body.containsKey("mode") ? (String) body.get("mode") : banner.getMode();
            Integer effMovieId = body.containsKey("movieId") ? parseMovieId(body.get("movieId")) : banner.getMovieId();
            if ("MOVIE".equalsIgnoreCase(effMode) && (body.containsKey("movieId") || body.containsKey("mode"))) {
                String movieErr = checkMovieAvailable(effMovieId);
                if (movieErr != null) return badRequest(movieErr);
            }

            if (body.containsKey("title")) banner.setTitle(sanitizeTitle((String) body.get("title"))); // Edge case 4
            if (body.containsKey("imageUrl")) banner.setImageUrl((String) body.get("imageUrl"));
            if (body.containsKey("link")) banner.setLink((String) body.get("link"));
            if (body.containsKey("mode")) banner.setMode((String) body.get("mode"));
            if (body.containsKey("movieId")) banner.setMovieId(effMovieId);
            if (body.containsKey("placement")) banner.setPlacement((String) body.get("placement"));
            if (body.containsKey("isActive")) banner.setIsActive(Boolean.TRUE.equals(body.get("isActive")));
            if (body.containsKey("order")) { // Edge case 3
                Integer order = parsePriority(body.get("order"));
                if (order != null) banner.setDisplayOrder(order);
            }
            // Edge case 2: định dạng ngày hợp lệ + ngày kết thúc phải sau ngày bắt đầu.
            if (body.get("startDate") != null) banner.setStartDate(parseDate(body.get("startDate"), "ngày bắt đầu"));
            if (body.get("endDate") != null) banner.setEndDate(parseDate(body.get("endDate"), "ngày kết thúc"));
            if (banner.getStartDate() != null && banner.getEndDate() != null
                    && !banner.getEndDate().isAfter(banner.getStartDate())) {
                return badRequest("Ngày kết thúc phải sau ngày bắt đầu.");
            }
            bannerRepository.save(banner);
            // Đồng bộ cờ cho cả phim cũ (nếu đổi phim/bỏ chế độ MOVIE) lẫn phim mới.
            bannerSyncService.syncMovieFlag(oldMovieId);
            if (banner.getMovieId() != null && !banner.getMovieId().equals(oldMovieId)) {
                bannerSyncService.syncMovieFlag(banner.getMovieId());
            }
            return ResponseEntity.ok(Map.of("success", true, "data", banner));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ===== Helper: validate & làm sạch dữ liệu banner (chặn các trường hợp ngoại lệ) =====

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", message));
    }

    private Integer parseMovieId(Object raw) {
        return raw != null ? ((Number) raw).intValue() : null;
    }

    /** Trả về thông báo lỗi nếu phim không còn khả dụng để gắn banner; null nếu hợp lệ. */
    private String checkMovieAvailable(Integer movieId) {
        if (movieId == null) return "Vui lòng chọn phim để tạo banner theo phim.";
        Movie movie = movieRepository.findById(movieId).orElse(null);
        // Phim bị xoá (không còn trong DB) hoặc đã ngừng chiếu (status = archived) -> không cho tạo banner.
        if (movie == null || "archived".equalsIgnoreCase(movie.getStatus())) {
            return "Phim được chọn hiện không còn khả dụng để tạo banner.";
        }
        return null;
    }

    /** Bắt buộc thứ tự ưu tiên là số nguyên dương (>= 1). null = không truyền (giữ mặc định). */
    private Integer parsePriority(Object raw) {
        if (raw == null) return null;
        if (!(raw instanceof Number num)) {
            throw new IllegalArgumentException("Thứ tự ưu tiên phải là số nguyên dương.");
        }
        double d = num.doubleValue();
        if (d != Math.rint(d) || Double.isNaN(d) || Double.isInfinite(d)) {
            throw new IllegalArgumentException("Thứ tự ưu tiên phải là số nguyên (không chấp nhận số thập phân).");
        }
        int value = (int) d;
        if (value < 1) {
            throw new IllegalArgumentException("Thứ tự ưu tiên phải là số nguyên dương (>= 1).");
        }
        return value;
    }

    /** Parse LocalDateTime với thông báo lỗi thân thiện khi định dạng sai. */
    private LocalDateTime parseDate(Object raw, String fieldLabel) {
        if (raw == null) return null;
        try {
            return LocalDateTime.parse((String) raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng " + fieldLabel + " không hợp lệ.");
        }
    }

    /** Loại bỏ mọi thẻ HTML/script khỏi tiêu đề, tránh lưu mã độc vào DB. */
    private String sanitizeTitle(String raw) {
        if (raw == null) return null;
        String cleaned = HTML_TAG.matcher(raw).replaceAll("").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('banners','delete')")
    public ResponseEntity<?> deleteBanner(@PathVariable Integer id) {
        try {
            // Lấy movieId trước khi xoá để đồng bộ tắt cờ showOnBanner nếu phim không còn banner nào.
            Integer movieId = bannerRepository.findById(id).map(Banner::getMovieId).orElse(null);
            bannerRepository.deleteById(id);
            bannerSyncService.syncMovieFlag(movieId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
