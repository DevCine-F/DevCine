package com.devcine.backend.config;

import com.devcine.backend.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Tự động ghi Audit Log cho mọi thao tác thay đổi dữ liệu (POST/PUT/PATCH/DELETE)
 * do tài khoản ADMIN/STAFF thực hiện thành công. Tập trung tại một nơi nên không
 * cần chỉnh sửa từng controller.
 */
@Component
@RequiredArgsConstructor
public class AuditLogInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String method = request.getMethod();
        if (!isMutating(method)) return;

        int status = response.getStatus();
        if (status < 200 || status >= 300) return; // chỉ ghi khi thành công

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return;

        boolean isInternal = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_STAFF"));
        if (!isInternal) return;

        Integer userId = (auth.getPrincipal() instanceof Integer id) ? id : null;

        String action = switch (method) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> method;
        };

        auditLogService.record(userId, action, deriveTarget(request.getRequestURI()), extractIp(request));
    }

    private boolean isMutating(String method) {
        return method.equals("POST") || method.equals("PUT")
                || method.equals("PATCH") || method.equals("DELETE");
    }

    /**
     * Suy ra tên đối tượng bị tác động từ URI.
     * Ví dụ: /api/banners/5 -> "banners"; /api/admin/movies -> "admin/movies".
     */
    private String deriveTarget(String uri) {
        String path = uri;
        int apiIdx = path.indexOf("/api/");
        if (apiIdx >= 0) path = path.substring(apiIdx + 5);
        StringBuilder sb = new StringBuilder();
        for (String part : path.split("/")) {
            if (part.isBlank()) continue;
            if (part.matches("\\d+")) break;       // gặp id dạng số thì dừng
            if (sb.length() > 0) sb.append("/");
            sb.append(part);
            if (!part.equals("admin")) break;      // bỏ qua tiền tố "admin", lấy segment nghiệp vụ kế tiếp
        }
        return sb.length() > 0 ? sb.toString() : path;
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
