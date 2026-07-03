package com.devcine.backend.service;

import com.devcine.backend.entity.Role;
import com.devcine.backend.entity.UserPermissionOverride;
import com.devcine.backend.repository.RoleRepository;
import com.devcine.backend.repository.UserPermissionOverrideRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Đánh giá quyền chi tiết theo ma trận (feature -> [action]) lưu trong Role.permissionsMatrix.
 * Được tham chiếu trong @PreAuthorize qua SpEL, ví dụ: @PreAuthorize("@perm.can('movies','edit')").
 * ADMIN luôn toàn quyền; các vai trò khác bị giới hạn theo ma trận cấu hình.
 */
@Slf4j
@Service("perm")
@RequiredArgsConstructor
public class PermissionService {

    private final RoleRepository roleRepository;
    private final UserPermissionOverrideRepository userPermissionOverrideRepository;
    private final ObjectMapper objectMapper;

    /** Cache: roleName(UPPER) -> (feature -> set actions). Xoá khi cập nhật quyền. */
    private final Map<String, Map<String, Set<String>>> cache = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, Set<String>>> userCache = new ConcurrentHashMap<>();

    public boolean can(String feature, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        String roleName = currentRole(auth);
        if (roleName == null) return false;
        if ("ADMIN".equalsIgnoreCase(roleName)) return true; // ADMIN toàn quyền

        Integer userId = currentUserId(auth);
        Set<String> actions = effectivePermissions(userId, roleName).get(feature);
        return actions != null && actions.contains(action);
    }

    public Map<String, Set<String>> effectivePermissions(Integer userId, String roleName) {
        if (userId == null) return copyMatrix(loadMatrix(roleName));
        return userCache.computeIfAbsent(userId, id -> {
            Map<String, Set<String>> result = copyMatrix(loadMatrix(roleName));
            for (UserPermissionOverride override : userPermissionOverrideRepository.findByUserId(id)) {
                String feature = override.getFeature();
                String action = override.getAction();
                String effect = override.getEffect();
                if (feature == null || action == null || effect == null) continue;

                if ("DENY".equalsIgnoreCase(effect)) {
                    Set<String> actions = result.get(feature);
                    if (actions != null) {
                        actions.remove(action);
                        if (actions.isEmpty()) result.remove(feature);
                    }
                } else if ("ALLOW".equalsIgnoreCase(effect)) {
                    result.computeIfAbsent(feature, key -> new HashSet<>()).add(action);
                }
            }
            return result;
        });
    }

    private String currentRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse(null);
    }

    private Integer currentUserId(Authentication auth) {
        return auth.getPrincipal() instanceof Integer id ? id : null;
    }

    private Map<String, Set<String>> loadMatrix(String roleName) {
        return cache.computeIfAbsent(roleName.toUpperCase(), rn -> {
            Role role = roleRepository.findByName(rn).orElse(null);
            if (role == null || role.getPermissionsMatrix() == null || role.getPermissionsMatrix().isBlank()) {
                return Map.of();
            }
            try {
                Map<String, List<String>> raw = objectMapper.readValue(
                        role.getPermissionsMatrix(), new TypeReference<Map<String, List<String>>>() {});
                Map<String, Set<String>> result = new HashMap<>();
                raw.forEach((k, v) -> result.put(k, new HashSet<>(v)));
                return result;
            } catch (Exception e) {
                log.error("Ma trận quyền của vai trò {} không hợp lệ", rn, e);
                return Map.of();
            }
        });
    }

    /** Gọi sau khi cập nhật ma trận quyền để cache không bị cũ. */
    private Map<String, Set<String>> copyMatrix(Map<String, Set<String>> source) {
        Map<String, Set<String>> copy = new HashMap<>();
        source.forEach((feature, actions) -> copy.put(feature, new HashSet<>(actions)));
        return copy;
    }

    public void invalidate() {
        cache.clear();
        userCache.clear();
    }

    public void invalidateUser(Integer userId) {
        if (userId != null) userCache.remove(userId);
    }
}
