package com.devcine.backend.controller;

import com.devcine.backend.entity.AgeRating;
import com.devcine.backend.entity.Category;
import com.devcine.backend.entity.MovieFormat;
import com.devcine.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Danh mục phim: Thể loại / Định dạng / Kiểm duyệt độ tuổi.
 * GET công khai (phục vụ dropdown ở màn quản lý phim & lọc); ghi yêu cầu quyền 'movies'.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;

    // ===================== THỂ LOẠI =====================

    @GetMapping("/genres")
    public List<Category> getGenres() {
        return categoryService.getGenres();
    }

    @PostMapping("/genres")
    @PreAuthorize("@perm.can('movies','add')")
    public ResponseEntity<?> createGenre(@RequestBody Category body) {
        return ResponseEntity.ok(categoryService.createGenre(body));
    }

    @PutMapping("/genres/{id}")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<?> updateGenre(@PathVariable Integer id, @RequestBody Category body) {
        return ResponseEntity.ok(categoryService.updateGenre(id, body));
    }

    @DeleteMapping("/genres/{id}")
    @PreAuthorize("@perm.can('movies','delete')")
    public ResponseEntity<?> deleteGenre(@PathVariable Integer id) {
        categoryService.deleteGenre(id);
        return ResponseEntity.noContent().build();
    }

    // ===================== ĐỊNH DẠNG =====================

    @GetMapping("/formats")
    public List<MovieFormat> getFormats() {
        return categoryService.getFormats();
    }

    @PostMapping("/formats")
    @PreAuthorize("@perm.can('movies','add')")
    public ResponseEntity<?> createFormat(@RequestBody MovieFormat body) {
        return ResponseEntity.ok(categoryService.createFormat(body));
    }

    @PutMapping("/formats/{id}")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<?> updateFormat(@PathVariable Integer id, @RequestBody MovieFormat body) {
        return ResponseEntity.ok(categoryService.updateFormat(id, body));
    }

    @DeleteMapping("/formats/{id}")
    @PreAuthorize("@perm.can('movies','delete')")
    public ResponseEntity<?> deleteFormat(@PathVariable Integer id) {
        categoryService.deleteFormat(id);
        return ResponseEntity.noContent().build();
    }

    // ===================== KIỂM DUYỆT =====================

    @GetMapping("/age-ratings")
    public List<AgeRating> getAgeRatings() {
        return categoryService.getAgeRatings();
    }

    @PostMapping("/age-ratings")
    @PreAuthorize("@perm.can('movies','add')")
    public ResponseEntity<?> createAgeRating(@RequestBody AgeRating body) {
        return ResponseEntity.ok(categoryService.createAgeRating(body));
    }

    @PutMapping("/age-ratings/{id}")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<?> updateAgeRating(@PathVariable Integer id, @RequestBody AgeRating body) {
        return ResponseEntity.ok(categoryService.updateAgeRating(id, body));
    }

    @DeleteMapping("/age-ratings/{id}")
    @PreAuthorize("@perm.can('movies','delete')")
    public ResponseEntity<?> deleteAgeRating(@PathVariable Integer id) {
        categoryService.deleteAgeRating(id);
        return ResponseEntity.noContent().build();
    }

    // ===================== XỬ LÝ LỖI CỤC BỘ =====================

    /** Dữ liệu sai / trùng / không tìm thấy → 400 kèm thông điệp thân thiện. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    /** Vi phạm ràng buộc nghiệp vụ (vd thể loại đang được dùng) → 409. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }
}
