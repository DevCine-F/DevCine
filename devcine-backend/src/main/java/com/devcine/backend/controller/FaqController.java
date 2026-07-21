package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.request.FaqRequest;
import com.devcine.backend.entity.Faq;
import com.devcine.backend.service.FaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FAQ trang Hỗ trợ. GET công khai; thêm/sửa/xoá yêu cầu quyền ADMIN.
 */
@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    /** Công khai — chỉ FAQ đang bật. */
    @GetMapping
    public ApiResponse<List<Faq>> getPublic() {
        return ApiResponse.ok(faqService.getPublicFaqs());
    }

    /** Quản trị — toàn bộ FAQ (kể cả đang ẩn). */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Faq>> getAll() {
        return ApiResponse.ok(faqService.getAllFaqs());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody FaqRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(faqService.create(body)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody FaqRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(faqService.update(id, body)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        faqService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xoá câu hỏi."));
    }
}
