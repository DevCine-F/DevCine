package com.devcine.backend.controller;

import com.devcine.backend.entity.AuditLog;
import com.devcine.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<?> getLogs(
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<AuditLog> pageResult = (action != null && !action.isBlank())
                ? auditLogRepository.findByActionWithUser(action.toUpperCase(), PageRequest.of(page, size))
                : auditLogRepository.findAllWithUser(PageRequest.of(page, size));

        List<Map<String, Object>> content = pageResult.getContent().stream().map(log -> {
            Map<String, Object> m = new java.util.HashMap<>();
            String act = log.getAction() != null ? log.getAction().toUpperCase() : "";
            m.put("logId", log.getId());
            m.put("action", act);
            m.put("entityType", log.getTargetTable() != null ? log.getTargetTable() : "");
            m.put("createdAt", log.getTimestamp() != null ? log.getTimestamp().toString() : null);
            m.put("performedBy", log.getUser() != null ? log.getUser().getUsername() : "system");
            m.put("userRole", log.getUser() != null && log.getUser().getRole() != null ? log.getUser().getRole().getName() : "SYSTEM");
            m.put("ipAddress", log.getIpAddress() != null ? log.getIpAddress() : "");
            m.put("description", describe(act, log.getTargetTable()));
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "content", content,
                "page", pageResult.getNumber(),
                "size", pageResult.getSize(),
                "totalElements", pageResult.getTotalElements(),
                "totalPages", pageResult.getTotalPages()
        ));
    }

    // Mô tả hành động cho cột "Chi tiết" (AuditLog không lưu mô tả riêng — dựng từ action + bảng đích)
    private String describe(String action, String targetTable) {
        String t = (targetTable != null && !targetTable.isBlank()) ? " · " + targetTable : "";
        switch (action) {
            case "LOGIN": return "Đăng nhập hệ thống";
            case "CREATE": return "Tạo mới bản ghi" + t;
            case "UPDATE": return "Cập nhật bản ghi" + t;
            case "DELETE": return "Xoá bản ghi" + t;
            case "SYSTEM": return "Thao tác hệ thống" + t;
            default: return (action.isBlank() ? "Thao tác" : action) + t;
        }
    }
}
