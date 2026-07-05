package com.devcine.backend.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

public class SecurityUtils {

    /**
     * Lấy cinemaId của người dùng hiện tại từ SecurityContext (nếu có)
     * Trả về null nếu người dùng không có cinemaId (VD: là Khách hàng, hoặc ADMIN không gán cơ sở)
     */
    public static Integer getCurrentUserCinemaId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (auth.getDetails() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) auth.getDetails();
            Object cinemaId = details.get("cinemaId");
            if (cinemaId instanceof Integer) {
                return (Integer) cinemaId;
            } else if (cinemaId != null) {
                try {
                    return Integer.valueOf(cinemaId.toString());
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    /**
     * Kiểm tra người dùng hiện tại có vai trò được chỉ định hay không
     * @param role VD: "ADMIN", "STAFF"
     */
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return false;
        String roleWithPrefix = "ROLE_" + role.toUpperCase();
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (authority.getAuthority().equals(roleWithPrefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Trả về true nếu người dùng hiện tại là ADMIN
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Trả về true nếu người dùng hiện tại là MANAGER (quản lý cơ sở)
     */
    public static boolean isManager() {
        return hasRole("MANAGER");
    }

    /**
     * Lấy ID của người dùng hiện tại
     */
    public static Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        try {
            return Integer.valueOf(auth.getPrincipal().toString());
        } catch (NumberFormatException ignored) {}
        return null;
    }
}
