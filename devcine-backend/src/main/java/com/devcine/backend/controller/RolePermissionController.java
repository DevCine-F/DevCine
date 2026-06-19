package com.devcine.backend.controller;

import com.devcine.backend.entity.Role;
import com.devcine.backend.repository.RoleRepository;
import com.devcine.backend.service.PermissionService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Quản lý cấu hình phân quyền chi tiết theo vai trò (ma trận feature -> action),
 * lưu xuống cột Role.permissionsMatrix dưới dạng JSON. Chỉ ADMIN được truy cập.
 */
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RolePermissionController {

    private final RoleRepository roleRepository;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRoles() {
        List<Map<String, Object>> result = roleRepository.findAll().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("permissions", parseMatrix(r.getPermissionsMatrix()));
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePermissions(@PathVariable Integer id,
                                               @RequestBody Map<String, Object> matrix) {
        try {
            Role role = roleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò"));
            role.setPermissionsMatrix(objectMapper.writeValueAsString(matrix));
            roleRepository.save(role);
            permissionService.invalidate();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private Object parseMatrix(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
