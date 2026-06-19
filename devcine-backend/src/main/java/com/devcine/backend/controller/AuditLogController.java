package com.devcine.backend.controller;

import com.devcine.backend.entity.AuditLog;
import com.devcine.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        LocalDateTime fromDate = from != null ? LocalDateTime.parse(from) : null;
        LocalDateTime toDate = to != null ? LocalDateTime.parse(to) : null;

        Page<AuditLog> pageResult = auditLogRepository.findWithFilters(
                action, fromDate, toDate, PageRequest.of(page, size));

        List<Map<String, Object>> content = pageResult.getContent().stream().map(log -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("logId", log.getId());
            m.put("action", log.getAction() != null ? log.getAction().toUpperCase() : "");
            m.put("entityType", log.getTargetTable() != null ? log.getTargetTable() : "");
            m.put("createdAt", log.getTimestamp().toString());
            m.put("performedBy", log.getUser() != null ? log.getUser().getUsername() : "system");
            m.put("userRole", log.getUser() != null && log.getUser().getRole() != null ? log.getUser().getRole().getName() : "SYSTEM");
            m.put("ipAddress", log.getIpAddress() != null ? log.getIpAddress() : "");
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
}
