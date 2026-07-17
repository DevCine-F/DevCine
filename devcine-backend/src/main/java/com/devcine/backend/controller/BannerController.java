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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
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
            if (!"IMAGE".equalsIgnoreCase(mode) && !"MOVIE".equalsIgnoreCase(mode)) {
                return badRequest("Vui lòng chọn chế độ hiển thị.");
            }
            Integer movieId = parseMovieId(body.get("movieId"));

            // Tiêu đề: 5–100 ký tự, đã strip HTML/script (Edge case 4).
            String title = sanitizeTitle((String) body.get("title"));
            String titleErr = validateTitle(title);
            if (titleErr != null) return badRequest(titleErr);

            String link = trimToNull((String) body.get("link"));
            String imageUrl = trimToNull((String) body.get("imageUrl"));
            if ("IMAGE".equalsIgnoreCase(mode)) {
                // Chế độ ảnh: bắt buộc có ảnh; link điều hướng (nếu nhập) phải hợp lệ.
                if (imageUrl == null) return badRequest("Vui lòng tải lên file ảnh banner hợp lệ (định dạng JPG/PNG/WEBP, tối đa 5MB).");
                if (link != null && !isValidRedirect(link)) return badRequest("Đường dẫn điều hướng không hợp lệ.");
            } else {
                // Edge case 1: banner theo phim -> phim phải tồn tại & đang hoạt động (không bị xoá/ngừng chiếu).
                String movieErr = checkMovieAvailable(movieId);
                if (movieErr != null) return badRequest(movieErr);
                link = null; // banner theo phim tự gắn link, không dùng link nhập tay
            }

            // Edge case 3: thứ tự ưu tiên phải là số nguyên 1–99.
            Integer order = parsePriority(body.get("order"));
            if (order == null) return badRequest("Thứ tự ưu tiên phải là số nguyên dương từ 1 đến 99.");

            // Edge case 2: chuẩn hoá mốc giờ (bắt đầu -> 00:00:00, kết thúc -> 23:59:59) + ràng buộc ngày.
            LocalDateTime startDate = parseDate(body.get("startDate"), false);
            LocalDateTime endDate = parseDate(body.get("endDate"), true);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            if (startDate != null && startDate.isBefore(todayStart)) return badRequest("Ngày bắt đầu không được ở trong quá khứ.");
            if (endDate != null && endDate.isBefore(now)) return badRequest("Ngày kết thúc phải sau thời điểm hiện tại.");
            if (startDate == null) startDate = now; // để trống = bắt đầu ngay
            // endDate để trống = treo banner vô thời hạn (hiển thị đến khi tự tắt/xoá).
            if (endDate != null && !endDate.isAfter(startDate)) return badRequest("Ngày kết thúc phải lớn hơn ngày bắt đầu.");

            Banner banner = Banner.builder()
                    .title(title)
                    .imageUrl("IMAGE".equalsIgnoreCase(mode) ? imageUrl : null)
                    .link(link)
                    .mode(mode)
                    .movieId(movieId)
                    .placement((String) body.getOrDefault("placement", "HOME"))
                    .isActive(body.get("isActive") == null || Boolean.TRUE.equals(body.get("isActive")))
                    .displayOrder(order)
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
            LocalDateTime oldStart = banner.getStartDate(); // ngày bắt đầu đang lưu (để bỏ qua chặn quá khứ nếu không đổi)

            // Edge case 1: nếu cập nhật liên quan phim (đổi phim hoặc đổi sang chế độ MOVIE) -> phim phải còn khả dụng.
            String effMode = body.containsKey("mode") ? (String) body.get("mode") : banner.getMode();
            Integer effMovieId = body.containsKey("movieId") ? parseMovieId(body.get("movieId")) : banner.getMovieId();
            if ("MOVIE".equalsIgnoreCase(effMode) && (body.containsKey("movieId") || body.containsKey("mode"))) {
                String movieErr = checkMovieAvailable(effMovieId);
                if (movieErr != null) return badRequest(movieErr);
            }

            if (body.containsKey("title")) { // Edge case 4 + độ dài 5–100
                String title = sanitizeTitle((String) body.get("title"));
                String titleErr = validateTitle(title);
                if (titleErr != null) return badRequest(titleErr);
                banner.setTitle(title);
            }
            if (body.containsKey("imageUrl")) banner.setImageUrl((String) body.get("imageUrl"));
            if (body.containsKey("link")) {
                String link = trimToNull((String) body.get("link"));
                if (link != null && !isValidRedirect(link)) return badRequest("Đường dẫn điều hướng không hợp lệ.");
                banner.setLink(link);
            }
            if (body.containsKey("mode")) banner.setMode((String) body.get("mode"));
            if (body.containsKey("movieId")) banner.setMovieId(effMovieId);
            if (body.containsKey("placement")) banner.setPlacement((String) body.get("placement"));
            if (body.containsKey("isActive")) banner.setIsActive(Boolean.TRUE.equals(body.get("isActive")));
            if (body.containsKey("order")) { // Edge case 3
                Integer order = parsePriority(body.get("order"));
                if (order == null) return badRequest("Thứ tự ưu tiên phải là số nguyên dương từ 1 đến 99.");
                banner.setDisplayOrder(order);
            }
            // Edge case 2: chuẩn hoá mốc giờ (bắt đầu -> 00:00:00, kết thúc -> 23:59:59) + ràng buộc ngày.
            if (body.get("startDate") != null) {
                LocalDateTime newStart = parseDate(body.get("startDate"), false);
                // Edge case Update: chỉ chặn quá khứ khi ngày bắt đầu THỰC SỰ thay đổi so với giá trị đang lưu.
                if (newStart != null && !newStart.equals(oldStart) && newStart.isBefore(LocalDate.now().atStartOfDay())) {
                    return badRequest("Ngày bắt đầu không được ở trong quá khứ.");
                }
                banner.setStartDate(newStart);
            }
            if (body.get("endDate") != null) banner.setEndDate(parseDate(body.get("endDate"), true));
            if (banner.getStartDate() != null && banner.getEndDate() != null
                    && !banner.getEndDate().isAfter(banner.getStartDate())) {
                return badRequest("Ngày kết thúc phải lớn hơn ngày bắt đầu.");
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

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Tiêu đề bắt buộc 5–100 ký tự (sau khi đã strip HTML/khoảng trắng). */
    private String validateTitle(String title) {
        if (title == null || title.length() < 5 || title.length() > 100) {
            return "Tiêu đề banner phải từ 5 - 100 ký tự và không chứa mã độc.";
        }
        return null;
    }

    /** Link điều hướng hợp lệ: URL http(s) hoặc đường dẫn nội bộ bắt đầu bằng '/'. */
    private boolean isValidRedirect(String link) {
        return link.startsWith("/") || link.startsWith("http://") || link.startsWith("https://");
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

    /** Bắt buộc thứ tự ưu tiên là số nguyên 1–99. null = không truyền. */
    private Integer parsePriority(Object raw) {
        if (raw == null) return null;
        if (!(raw instanceof Number num)) {
            throw new IllegalArgumentException("Thứ tự ưu tiên phải là số nguyên dương từ 1 đến 99.");
        }
        double d = num.doubleValue();
        if (d != Math.rint(d) || Double.isNaN(d) || Double.isInfinite(d)) {
            throw new IllegalArgumentException("Thứ tự ưu tiên phải là số nguyên dương từ 1 đến 99.");
        }
        int value = (int) d;
        if (value < 1 || value > 99) {
            throw new IllegalArgumentException("Thứ tự ưu tiên phải là số nguyên dương từ 1 đến 99.");
        }
        return value;
    }

    /**
     * Parse ngày; nếu chỉ nhận được ngày (yyyy-MM-dd, không có giờ) thì tự gán mốc giờ chuẩn:
     * ngày bắt đầu -> 00:00:00, ngày kết thúc -> 23:59:59 ({@code endOfDay = true}).
     * Sai định dạng (ví dụ 31/02) -> ném lỗi 400 với thông báo chuẩn.
     */
    private LocalDateTime parseDate(Object raw, boolean endOfDay) {
        if (raw == null) return null;
        String value = ((String) raw).trim();
        if (value.isEmpty()) return null;
        try {
            if (value.length() == 10) { // chỉ có ngày yyyy-MM-dd -> gán mốc giờ chuẩn ở 2 đầu
                LocalDate date = LocalDate.parse(value);
                return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
            }
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng ngày tháng không hợp lệ.");
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
