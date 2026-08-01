package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.response.ApprovalResponse;
import com.devcine.backend.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Luồng phê duyệt "sửa sai" nội bộ (chỉ còn hủy hóa đơn F&B):
 * nhân viên quầy tạo yêu cầu → Quản lý/Quản trị viên duyệt hoặc từ chối.
 * Gate theo Vai trò + cách ly cụm rạp nằm trong {@link ApprovalService}.
 */
@RestController
@RequestMapping("/api/staff/approvals")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class ApprovalController {

    private final ApprovalService approvalService;

    // Tạo yêu cầu + xem danh sách = approvals:view (STAFF được tạo/xem của mình).
    @PostMapping("/fnb-void")
    @PreAuthorize("@perm.can('approvals', 'view')")
    public ResponseEntity<ApiResponse<ApprovalResponse>> requestFnbVoid(@RequestBody Map<String, Object> body) {
        Integer saleId = asInt(body.get("saleId"));
        String reason = asString(body.get("reason"));
        return ResponseEntity.ok(ApiResponse.ok(ApprovalResponse.from(approvalService.requestFnbVoid(saleId, reason))));
    }

    @GetMapping("/pending")
    @PreAuthorize("@perm.can('approvals', 'view')")
    public ResponseEntity<ApiResponse<List<ApprovalResponse>>> listPending() {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.listPending().stream().map(ApprovalResponse::from).toList()));
    }

    @GetMapping("/mine")
    @PreAuthorize("@perm.can('approvals', 'view')")
    public ResponseEntity<ApiResponse<List<ApprovalResponse>>> listMine() {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.listMine().stream().map(ApprovalResponse::from).toList()));
    }

    // Duyệt / Từ chối = approvals:edit (chỉ Quản lý/Quản trị viên — service còn chặn thêm ở requireApprover).
    @PutMapping("/{id}/approve")
    @PreAuthorize("@perm.can('approvals', 'edit')")
    public ResponseEntity<ApiResponse<ApprovalResponse>> approve(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(ApprovalResponse.from(approvalService.approve(id))));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("@perm.can('approvals', 'edit')")
    public ResponseEntity<ApiResponse<ApprovalResponse>> reject(@PathVariable Integer id, @RequestBody(required = false) Map<String, Object> body) {
        String note = body != null ? asString(body.get("note")) : null;
        return ResponseEntity.ok(ApiResponse.ok(ApprovalResponse.from(approvalService.reject(id, note))));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ex.getMessage() != null ? ex.getMessage() : "Yêu cầu không hợp lệ."));
    }

    private Integer asInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString().trim() : null;
    }
}
