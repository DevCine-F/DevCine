package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.Role;
import com.devcine.backend.entity.User;
import com.devcine.backend.entity.UserPermissionOverride;
import com.devcine.backend.repository.RoleRepository;
import com.devcine.backend.repository.UserPermissionOverrideRepository;
import com.devcine.backend.repository.UserRepository;
import com.devcine.backend.service.PermissionService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quản lý cấu hình phân quyền chi tiết theo vai trò (ma trận feature -> action),
 * lưu xuống cột Role.permissionsMatrix dưới dạng JSON. ADMIN quản lý role, STAFF chỉ đọc quyền của chính mình.
 */
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserPermissionOverrideRepository userPermissionOverrideRepository;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @perm.can('roles', 'manage')")
    public ResponseEntity<?> getRoles() {
        List<Map<String, Object>> result = roleRepository.findAll().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("permissions", parseMatrix(r.getPermissionsMatrix()));
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/staff-users")
    @PreAuthorize("hasRole('ADMIN') or @perm.can('roles', 'manage')")
    public ResponseEntity<?> getStaffUsers() {
        List<Map<String, Object>> result = userRepository.findAllByRoleName("STAFF").stream()
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(user -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", user.getId());
                    m.put("username", user.getUsername());
                    m.put("fullName", user.getFullName());
                    m.put("email", user.getEmail());
                    m.put("isActive", Boolean.TRUE.equals(user.getIsActive()));
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/users/{userId}/permission-overrides")
    @PreAuthorize("hasRole('ADMIN') or @perm.can('roles', 'manage')")
    public ResponseEntity<?> getUserPermissionOverrides(@PathVariable Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        String roleName = user.getRole() != null ? user.getRole().getName() : "";
        Map<String, List<String>> allow = new HashMap<>();
        Map<String, List<String>> deny = new HashMap<>();
        for (UserPermissionOverride override : userPermissionOverrideRepository.findByUserId(userId)) {
            Map<String, List<String>> target = "DENY".equalsIgnoreCase(override.getEffect()) ? deny : allow;
            target.computeIfAbsent(override.getFeature(), key -> new ArrayList<>()).add(override.getAction());
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "userId", userId,
                "role", roleName,
                "basePermissions", roleRepository.findByName(roleName.toUpperCase())
                        .map(role -> parseMatrix(role.getPermissionsMatrix()))
                        .orElse(Map.of()),
                "allow", allow,
                "deny", deny,
                "effectivePermissions", toListMatrix(permissionService.effectivePermissions(userId, roleName))
        )));
    }

    @GetMapping("/me/permissions")
    public ResponseEntity<?> getMyPermissions(Authentication authentication) {
        String roleName = currentRole(authentication);
        if (roleName == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Chưa xác thực"));
        }
        if (!"ADMIN".equalsIgnoreCase(roleName) && !"MANAGER".equalsIgnoreCase(roleName)
                && !"STAFF".equalsIgnoreCase(roleName)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail("Tài khoản không có quyền truy cập khu nội bộ"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("role", roleName);
        response.put("admin", "ADMIN".equalsIgnoreCase(roleName));
        response.put("permissions", toListMatrix(permissionService.effectivePermissions(currentUserId(authentication), roleName)));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN') or @perm.can('roles', 'manage')")
    public ResponseEntity<?> updatePermissions(@PathVariable Integer id,
                                               @RequestBody Map<String, Object> matrix) {
        try {
            Role role = roleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò"));
            role.setPermissionsMatrix(objectMapper.writeValueAsString(matrix));
            roleRepository.save(role);
            permissionService.invalidate();
            return ResponseEntity.ok(ApiResponse.success("Đã lưu phân quyền vai trò."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}/permission-overrides")
    @PreAuthorize("hasRole('ADMIN') or @perm.can('roles', 'manage')")
    @Transactional
    public ResponseEntity<?> updateUserPermissionOverrides(@PathVariable Integer userId,
                                                           @RequestBody Map<String, Object> body) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
            String roleName = user.getRole() != null ? user.getRole().getName() : "";
            if (!"STAFF".equalsIgnoreCase(roleName)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Chỉ cấu hình riêng cho nhân viên STAFF"));
            }

            Map<String, List<String>> allow = normalizeMatrix(body.get("allow"));
            Map<String, List<String>> deny = normalizeMatrix(body.get("deny"));
            Map<String, String> effects = new HashMap<>();
            allow.forEach((feature, actions) -> actions.forEach(action -> effects.put(feature + "\u0000" + action, "ALLOW")));
            deny.forEach((feature, actions) -> actions.forEach(action -> effects.put(feature + "\u0000" + action, "DENY")));

            userPermissionOverrideRepository.deleteByUserId(userId);
            List<UserPermissionOverride> overrides = effects.entrySet().stream()
                    .map(entry -> {
                        String[] parts = entry.getKey().split("\u0000", 2);
                        return UserPermissionOverride.builder()
                                .user(user)
                                .feature(parts[0])
                                .action(parts[1])
                                .effect(entry.getValue())
                                .build();
                    })
                    .collect(Collectors.toList());
            userPermissionOverrideRepository.saveAll(overrides);
            permissionService.invalidateUser(userId);
            return ResponseEntity.ok(ApiResponse.success("Đã lưu quyền riêng của nhân viên."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
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

    private Integer currentUserId(Authentication auth) {
        return auth != null && auth.getPrincipal() instanceof Integer id ? id : null;
    }

    private Map<String, List<String>> toListMatrix(Map<String, Set<String>> matrix) {
        Map<String, List<String>> result = new HashMap<>();
        matrix.forEach((feature, actions) -> result.put(feature, actions.stream().sorted().collect(Collectors.toList())));
        return result;
    }

    private Map<String, List<String>> normalizeMatrix(Object raw) {
        if (!(raw instanceof Map<?, ?> rawMap)) return Map.of();
        Map<String, List<String>> result = new HashMap<>();
        rawMap.forEach((feature, actions) -> {
            if (feature == null || !(actions instanceof List<?> list)) return;
            List<String> cleanActions = list.stream()
                    .filter(item -> item != null && !item.toString().isBlank())
                    .map(item -> item.toString().trim())
                    .distinct()
                    .collect(Collectors.toList());
            if (!cleanActions.isEmpty()) {
                result.put(feature.toString(), cleanActions);
            }
        });
        return result;
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
