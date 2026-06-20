package com.devcine.backend.controller;

import com.devcine.backend.entity.Faq;
import com.devcine.backend.service.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FAQ trang Hỗ trợ. GET công khai; thêm/sửa/xoá yêu cầu quyền ADMIN.
 */
@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
@CrossOrigin("*")
public class FaqController {

    private final FaqService faqService;

    /** Công khai — chỉ FAQ đang bật. */
    @GetMapping
    public List<Faq> getPublic() {
        return faqService.getPublicFaqs();
    }

    /** Quản trị — toàn bộ FAQ (kể cả đang ẩn). */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Faq> getAll() {
        return faqService.getAllFaqs();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Faq body) {
        return ResponseEntity.ok(faqService.create(body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Faq body) {
        return ResponseEntity.ok(faqService.update(id, body));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        faqService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
