package com.devcine.backend.controller;

import com.devcine.backend.entity.Banner;
import com.devcine.backend.repository.BannerRepository;
import com.devcine.backend.service.BannerSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BannerController {

    private final BannerRepository bannerRepository;
    private final BannerSyncService bannerSyncService;

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
            Banner banner = Banner.builder()
                    .title((String) body.get("title"))
                    .imageUrl((String) body.get("imageUrl"))
                    .link((String) body.get("link"))
                    .mode((String) body.getOrDefault("mode", "IMAGE"))
                    .movieId(body.get("movieId") != null ? ((Number) body.get("movieId")).intValue() : null)
                    .placement((String) body.getOrDefault("placement", "HOME"))
                    .isActive(body.get("isActive") == null || Boolean.TRUE.equals(body.get("isActive")))
                    .displayOrder(body.get("order") != null ? ((Number) body.get("order")).intValue() : 0)
                    .startDate(body.get("startDate") != null ? LocalDateTime.parse((String) body.get("startDate")) : LocalDateTime.now())
                    .endDate(body.get("endDate") != null ? LocalDateTime.parse((String) body.get("endDate")) : LocalDateTime.now().plusMonths(1))
                    .build();
            bannerRepository.save(banner);
            // Banner theo phim -> bật cờ showOnBanner của phim tương ứng.
            bannerSyncService.syncMovieFlag(banner.getMovieId());
            return ResponseEntity.status(201).body(Map.of("success", true, "data", banner));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
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
            if (body.containsKey("title")) banner.setTitle((String) body.get("title"));
            if (body.containsKey("imageUrl")) banner.setImageUrl((String) body.get("imageUrl"));
            if (body.containsKey("link")) banner.setLink((String) body.get("link"));
            if (body.containsKey("mode")) banner.setMode((String) body.get("mode"));
            if (body.containsKey("movieId")) banner.setMovieId(body.get("movieId") != null ? ((Number) body.get("movieId")).intValue() : null);
            if (body.containsKey("placement")) banner.setPlacement((String) body.get("placement"));
            if (body.containsKey("isActive")) banner.setIsActive(Boolean.TRUE.equals(body.get("isActive")));
            if (body.containsKey("order")) banner.setDisplayOrder(((Number) body.get("order")).intValue());
            if (body.get("startDate") != null) banner.setStartDate(LocalDateTime.parse((String) body.get("startDate")));
            if (body.get("endDate") != null) banner.setEndDate(LocalDateTime.parse((String) body.get("endDate")));
            bannerRepository.save(banner);
            // Đồng bộ cờ cho cả phim cũ (nếu đổi phim/bỏ chế độ MOVIE) lẫn phim mới.
            bannerSyncService.syncMovieFlag(oldMovieId);
            if (banner.getMovieId() != null && !banner.getMovieId().equals(oldMovieId)) {
                bannerSyncService.syncMovieFlag(banner.getMovieId());
            }
            return ResponseEntity.ok(Map.of("success", true, "data", banner));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
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
