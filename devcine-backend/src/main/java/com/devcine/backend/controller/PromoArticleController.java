package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
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
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** Chi tiết một tin (công khai). */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Integer id) {
        return repository.findById(id)
                .<ResponseEntity<?>>map(a -> ResponseEntity.ok(ApiResponse.ok(a)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Admin: tất cả tin (tự động cập nhật tắt hiển thị cho các tin đã hết hạn). */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> getAll() {
        LocalDate today = LocalDate.now();
        List<PromoArticle> list = repository.findAllByOrderByDisplayOrderAscIdDesc();
        for (PromoArticle a : list) {
            if (Boolean.TRUE.equals(a.getIsActive()) && a.getEndDate() != null && a.getEndDate().isBefore(today)) {
                a.setIsActive(false);
                repository.save(a);
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping
    @PreAuthorize("@perm.can('promotions','add')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            String title = str(body.get("title"));
            if (title == null || title.trim().length() < 5 || title.trim().length() > 150) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Tiêu đề tin khuyến mãi phải từ 5 đến 150 ký tự."));
            }
            String imageUrl = str(body.get("imageUrl"));
            if (imageUrl == null || imageUrl.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Vui lòng tải lên ảnh Thumbnail / Banner cho tin khuyến mãi."));
            }
            String description = str(body.get("description"));
            if (description == null || description.trim().length() < 5 || description.trim().length() > 255) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Mô tả ngắn phải từ 5 đến 255 ký tự."));
            }
            LocalDate startDate = parseDate(body.get("startDate"));
            if (startDate == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Vui lòng chọn ngày bắt đầu áp dụng."));
            }
            LocalDate endDate = parseDate(body.get("endDate"));
            if (endDate == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Vui lòng chọn ngày kết thúc áp dụng."));
            }
            if (endDate.isBefore(startDate)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Ngày kết thúc không được trước ngày bắt đầu."));
            }
            String content = str(body.get("content"));
            if (content == null || content.trim().length() < 10) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Nội dung chi tiết phải có tối thiểu 10 ký tự."));
            }

            boolean active = body.get("isActive") == null || Boolean.parseBoolean(body.get("isActive").toString());
            // Nếu ngày kết thúc đã qua, tự động tắt hiển thị
            if (endDate.isBefore(LocalDate.now())) {
                active = false;
            }
            PromoArticle article = PromoArticle.builder()
                    .title(title.trim())
                    .description(description.trim())
                    .imageUrl(imageUrl.trim())
                    .content(content.trim())
                    .startDate(startDate)
                    .endDate(endDate)
                    .isActive(active)
                    .displayOrder(body.get("displayOrder") != null ? Integer.parseInt(body.get("displayOrder").toString()) : 0)
                    .createdAt(LocalDateTime.now())
                    .build();
            repository.save(article);
            return ResponseEntity.status(201).body(ApiResponse.ok(article));
        } catch (Exception e) {
            log.error("Lỗi tạo tin khuyến mãi", e);
            return ResponseEntity.badRequest().body(ApiResponse.fail("Không thể tạo tin. Vui lòng kiểm tra lại dữ liệu nhập."));
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
                if (title == null || title.trim().length() < 5 || title.trim().length() > 150) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Tiêu đề tin khuyến mãi phải từ 5 đến 150 ký tự."));
                }
                a.setTitle(title.trim());
            }
            if (body.containsKey("imageUrl")) {
                String imageUrl = str(body.get("imageUrl"));
                if (imageUrl == null || imageUrl.isBlank()) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Vui lòng tải lên ảnh Thumbnail / Banner cho tin khuyến mãi."));
                }
                a.setImageUrl(imageUrl.trim());
            }
            if (body.containsKey("description")) {
                String description = str(body.get("description"));
                if (description == null || description.trim().length() < 5 || description.trim().length() > 255) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Mô tả ngắn phải từ 5 đến 255 ký tự."));
                }
                a.setDescription(description.trim());
            }
            if (body.containsKey("startDate")) {
                LocalDate startDate = parseDate(body.get("startDate"));
                if (startDate == null) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Vui lòng chọn ngày bắt đầu áp dụng."));
                }
                a.setStartDate(startDate);
            }
            if (body.containsKey("endDate")) {
                LocalDate endDate = parseDate(body.get("endDate"));
                if (endDate == null) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Vui lòng chọn ngày kết thúc áp dụng."));
                }
                a.setEndDate(endDate);
            }
            if (a.getStartDate() != null && a.getEndDate() != null && a.getEndDate().isBefore(a.getStartDate())) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Ngày kết thúc không được trước ngày bắt đầu."));
            }
            if (body.containsKey("content")) {
                String content = str(body.get("content"));
                if (content == null || content.trim().length() < 10) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Nội dung chi tiết phải có tối thiểu 10 ký tự."));
                }
                a.setContent(content.trim());
            }
            if (body.containsKey("isActive")) a.setIsActive(Boolean.parseBoolean(body.get("isActive").toString()));
            if (body.containsKey("displayOrder")) a.setDisplayOrder(body.get("displayOrder") != null ? Integer.parseInt(body.get("displayOrder").toString()) : 0);
            
            // Nếu ngày kết thúc đã qua, tự động tắt hiển thị
            if (a.getEndDate() != null && a.getEndDate().isBefore(LocalDate.now())) {
                a.setIsActive(false);
            }
            repository.save(a);
            return ResponseEntity.ok(ApiResponse.ok(a));
        } catch (Exception e) {
            log.error("Lỗi cập nhật tin khuyến mãi {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.fail("Không thể cập nhật tin. Vui lòng kiểm tra lại dữ liệu nhập."));
        }
    }

    /** Bật/tắt nhanh trạng thái hiển thị. */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("@perm.can('promotions','edit')")
    public ResponseEntity<?> toggle(@PathVariable Integer id) {
        try {
            PromoArticle a = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tin khuyến mãi"));
            // Không cho phép bật hiển thị nếu tin đã hết hạn
            if (!Boolean.TRUE.equals(a.getIsActive()) && a.getEndDate() != null && a.getEndDate().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Tin khuyến mãi đã hết hạn. Vui lòng gia hạn ngày kết thúc trước khi bật hiển thị."));
            }
            a.setIsActive(!Boolean.TRUE.equals(a.getIsActive()));
            repository.save(a);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("isActive", a.getIsActive())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('promotions','delete')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        try {
            repository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Đã xoá tin khuyến mãi."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
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
