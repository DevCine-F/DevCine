package com.devcine.backend.controller;

import com.devcine.backend.entity.Role;
import com.devcine.backend.repository.RoleRepository;
import com.devcine.backend.service.PermissionService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Quản lý cấu hình phân quyền chi tiết theo vai trò (ma trận feature -> action),
 * lưu xuống cột Role.permissionsMatrix dưới dạng JSON. ADMIN quản lý role, STAFF chỉ đọc quyền của chính mình.
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

    @GetMapping("/me/permissions")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> getMyPermissions(Authentication authentication) {
        String roleName = currentRole(authentication);
        if (roleName == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không xác định được vai trò"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("role", roleName);
        response.put("admin", "ADMIN".equalsIgnoreCase(roleName));
        response.put("permissions", roleRepository.findByName(roleName.toUpperCase())
                .map(role -> parseMatrix(role.getPermissionsMatrix()))
                .orElse(Map.of()));
        return ResponseEntity.ok(response);
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

    private String currentRole(Authentication auth) {
        if (auth == null) return null;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMatrix(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
