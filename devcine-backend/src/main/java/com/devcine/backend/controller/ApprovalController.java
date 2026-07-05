package com.devcine.backend.controller;

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
 * Luồng phê duyệt "sửa sai" nội bộ của Trưởng ca (Shift Leader):
 * nhân viên quầy tạo yêu cầu → Trưởng ca/Manager/Admin duyệt hoặc từ chối.
 * Gate chi tiết theo Position (SHIFT_LEAD) nằm trong {@link ApprovalService}.
 */
@RestController
@RequestMapping("/api/staff/approvals")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping("/fnb-void")
    public ResponseEntity<ApprovalResponse> requestFnbVoid(@RequestBody Map<String, Object> body) {
        Integer saleId = asInt(body.get("saleId"));
        String reason = asString(body.get("reason"));
        return ResponseEntity.ok(ApprovalResponse.from(approvalService.requestFnbVoid(saleId, reason)));
    }

    @PostMapping("/seat-move")
    public ResponseEntity<ApprovalResponse> requestSeatMove(@RequestBody Map<String, Object> body) {
        Integer bookingSeatId = asInt(body.get("bookingSeatId"));
        Integer toSeatId = asInt(body.get("toSeatId"));
        String reason = asString(body.get("reason"));
        return ResponseEntity.ok(ApprovalResponse.from(approvalService.requestSeatMove(bookingSeatId, toSeatId, reason)));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalResponse>> listPending() {
        return ResponseEntity.ok(approvalService.listPending().stream().map(ApprovalResponse::from).toList());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ApprovalResponse>> listMine() {
        return ResponseEntity.ok(approvalService.listMine().stream().map(ApprovalResponse::from).toList());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApprovalResponse> approve(@PathVariable Integer id) {
        return ResponseEntity.ok(ApprovalResponse.from(approvalService.approve(id)));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApprovalResponse> reject(@PathVariable Integer id, @RequestBody(required = false) Map<String, Object> body) {
        String note = body != null ? asString(body.get("note")) : null;
        return ResponseEntity.ok(ApprovalResponse.from(approvalService.reject(id, note)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", ex.getMessage() != null ? ex.getMessage() : "Yêu cầu không hợp lệ."));
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
