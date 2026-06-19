package com.devcine.backend.controller;

import com.devcine.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getNotifications(@PathVariable Integer customerId) {
        return ResponseEntity.ok(notificationService.getForCustomer(customerId));
    }

    @GetMapping("/customer/{customerId}/unread-count")
    public ResponseEntity<?> getUnreadCount(@PathVariable Integer customerId) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(customerId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/customer/{customerId}/read-all")
    public ResponseEntity<?> markAllAsRead(@PathVariable Integer customerId) {
        notificationService.markAllAsRead(customerId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
