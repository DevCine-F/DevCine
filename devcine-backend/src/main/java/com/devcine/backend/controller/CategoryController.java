package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.request.AgeRatingRequest;
import com.devcine.backend.dto.request.CategoryRequest;
import com.devcine.backend.dto.request.MovieFormatRequest;
import com.devcine.backend.entity.AgeRating;
import com.devcine.backend.entity.Category;
import com.devcine.backend.entity.MovieFormat;
import com.devcine.backend.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Danh mục phim: Thể loại / Định dạng / Kiểm duyệt độ tuổi.
 * GET công khai (phục vụ dropdown ở màn quản lý phim & lọc); ghi yêu cầu quyền 'movies'.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;

    // ===================== THỂ LOẠI =====================

    @GetMapping("/genres")
    public ApiResponse<List<Category>> getGenres() {
        return ApiResponse.ok(categoryService.getGenres());
    }

    @PostMapping("/genres")
    @PreAuthorize("@perm.can('movies','add')")
    public ResponseEntity<?> createGenre(@Valid @RequestBody CategoryRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.createGenre(body)));
    }

    @PutMapping("/genres/{id}")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<?> updateGenre(@PathVariable Integer id, @Valid @RequestBody CategoryRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.updateGenre(id, body)));
    }

    @DeleteMapping("/genres/{id}")
    @PreAuthorize("@perm.can('movies','delete')")
    public ResponseEntity<?> deleteGenre(@PathVariable Integer id) {
        categoryService.deleteGenre(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xoá thể loại."));
    }

    // ===================== ĐỊNH DẠNG =====================

    @GetMapping("/formats")
    public ApiResponse<List<MovieFormat>> getFormats() {
        return ApiResponse.ok(categoryService.getFormats());
    }

    @PostMapping("/formats")
    @PreAuthorize("@perm.can('movies','add')")
    public ResponseEntity<?> createFormat(@Valid @RequestBody MovieFormatRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.createFormat(body)));
    }

    @PutMapping("/formats/{id}")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<?> updateFormat(@PathVariable Integer id, @Valid @RequestBody MovieFormatRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.updateFormat(id, body)));
    }

    @DeleteMapping("/formats/{id}")
    @PreAuthorize("@perm.can('movies','delete')")
    public ResponseEntity<?> deleteFormat(@PathVariable Integer id) {
        categoryService.deleteFormat(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xoá định dạng."));
    }

    // ===================== KIỂM DUYỆT =====================

    @GetMapping("/age-ratings")
    public ApiResponse<List<AgeRating>> getAgeRatings() {
        return ApiResponse.ok(categoryService.getAgeRatings());
    }

    @PostMapping("/age-ratings")
    @PreAuthorize("@perm.can('movies','add')")
    public ResponseEntity<?> createAgeRating(@Valid @RequestBody AgeRatingRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.createAgeRating(body)));
    }

    @PutMapping("/age-ratings/{id}")
    @PreAuthorize("@perm.can('movies','edit')")
    public ResponseEntity<?> updateAgeRating(@PathVariable Integer id, @Valid @RequestBody AgeRatingRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.updateAgeRating(id, body)));
    }

    @DeleteMapping("/age-ratings/{id}")
    @PreAuthorize("@perm.can('movies','delete')")
    public ResponseEntity<?> deleteAgeRating(@PathVariable Integer id) {
        categoryService.deleteAgeRating(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xoá nhãn phân loại."));
    }

    // ===================== XỬ LÝ LỖI CỤC BỘ =====================

    /** Dữ liệu sai / trùng / không tìm thấy → 400 kèm thông điệp thân thiện. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    /** Vi phạm ràng buộc nghiệp vụ (vd thể loại đang được dùng) → 409. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ex.getMessage()));
    }
}
