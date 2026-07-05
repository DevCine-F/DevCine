package com.devcine.backend.controller;

import com.devcine.backend.entity.PromoArticle;
import com.devcine.backend.repository.PromoArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Quản lý "Tin khuyến mãi" (nội dung biên tập hiển thị cho khách).
 * GET công khai (đã permitAll trong SecurityConfig); ghi được bảo vệ bởi @PreAuthorize.
 */
@RestController
@RequestMapping("/api/promo-articles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class PromoArticleController {

    private final PromoArticleRepository repository;

    /** Khách: danh sách tin đang bật và còn trong thời gian hiển thị. */
    @GetMapping
    public ResponseEntity<?> getActive() {
        LocalDate today = LocalDate.now();
        List<PromoArticle> result = repository.findByIsActiveTrueOrderByDisplayOrderAscIdDesc().stream()
                .filter(a -> (a.getStartDate() == null || !a.getStartDate().isAfter(today))
                        && (a.getEndDate() == null || !a.getEndDate().isBefore(today)))
                .toList();
        return ResponseEntity.ok(result);
    }

    /** Chi tiết một tin (công khai). */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Integer id) {
        return repository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Admin: tất cả tin (kể cả đang ẩn). */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(repository.findAllByOrderByDisplayOrderAscIdDesc());
    }

    @PostMapping
    @PreAuthorize("@perm.can('promotions','add')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            String title = str(body.get("title"));
            if (title == null || title.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng nhập tiêu đề tin khuyến mãi."));
            }
            PromoArticle article = PromoArticle.builder()
                    .title(title.trim())
                    .description(str(body.get("description")))
                    .imageUrl(str(body.get("imageUrl")))
                    .content(str(body.get("content")))
                    .startDate(parseDate(body.get("startDate")))
                    .endDate(parseDate(body.get("endDate")))
                    .isActive(body.get("isActive") == null || Boolean.parseBoolean(body.get("isActive").toString()))
                    .displayOrder(body.get("displayOrder") != null ? Integer.parseInt(body.get("displayOrder").toString()) : 0)
                    .createdAt(LocalDateTime.now())
                    .build();
            repository.save(article);
            return ResponseEntity.status(201).body(Map.of("success", true, "data", article));
        } catch (Exception e) {
            log.error("Lỗi tạo tin khuyến mãi", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không thể tạo tin. Vui lòng kiểm tra lại dữ liệu nhập."));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('promotions','edit')")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            PromoArticle a = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tin khuyến mãi"));
            if (body.containsKey("title")) {
                String title = str(body.get("title"));
                if (title == null || title.isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng nhập tiêu đề tin khuyến mãi."));
                }
                a.setTitle(title.trim());
            }
            if (body.containsKey("description")) a.setDescription(str(body.get("description")));
            if (body.containsKey("imageUrl")) a.setImageUrl(str(body.get("imageUrl")));
            if (body.containsKey("content")) a.setContent(str(body.get("content")));
            if (body.containsKey("startDate")) a.setStartDate(parseDate(body.get("startDate")));
            if (body.containsKey("endDate")) a.setEndDate(parseDate(body.get("endDate")));
            if (body.containsKey("isActive")) a.setIsActive(Boolean.parseBoolean(body.get("isActive").toString()));
            if (body.containsKey("displayOrder")) a.setDisplayOrder(body.get("displayOrder") != null ? Integer.parseInt(body.get("displayOrder").toString()) : 0);
            repository.save(a);
            return ResponseEntity.ok(Map.of("success", true, "data", a));
        } catch (Exception e) {
            log.error("Lỗi cập nhật tin khuyến mãi {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không thể cập nhật tin. Vui lòng kiểm tra lại dữ liệu nhập."));
        }
    }

    /** Bật/tắt nhanh trạng thái hiển thị. */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("@perm.can('promotions','edit')")
    public ResponseEntity<?> toggle(@PathVariable Integer id) {
        try {
            PromoArticle a = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tin khuyến mãi"));
            a.setIsActive(!Boolean.TRUE.equals(a.getIsActive()));
            repository.save(a);
            return ResponseEntity.ok(Map.of("success", true, "isActive", a.getIsActive()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('promotions','delete')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        try {
            repository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }

    /** Parse "yyyy-MM-dd" → LocalDate; chuỗi rỗng/null → null. */
    private static LocalDate parseDate(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        if (s.isBlank()) return null;
        return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
    }
}
