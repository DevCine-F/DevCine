package com.devcine.backend.controller;

import com.devcine.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** userId của người đang đăng nhập (principal do JwtFilter set). */
    private Integer currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Integer) auth.getPrincipal();
    }

    /** Chặn IDOR: chỉ cho thao tác trên thông báo của chính mình. */
    private void assertOwner(Integer customerId) {
        if (!currentUserId().equals(customerId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập thông báo của người khác.");
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getNotifications(@PathVariable Integer customerId) {
        assertOwner(customerId);
        return ResponseEntity.ok(notificationService.getForCustomer(customerId));
    }

    @GetMapping("/customer/{customerId}/unread-count")
    public ResponseEntity<?> getUnreadCount(@PathVariable Integer customerId) {
        assertOwner(customerId);
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(customerId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id, currentUserId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/customer/{customerId}/read-all")
    public ResponseEntity<?> markAllAsRead(@PathVariable Integer customerId) {
        assertOwner(customerId);
        notificationService.markAllAsRead(customerId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
