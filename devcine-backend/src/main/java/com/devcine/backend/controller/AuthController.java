package com.devcine.backend.controller;

import com.devcine.backend.service.AuthService;
import com.devcine.backend.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            var result = authService.register(
                    body.get("email"),
                    body.get("password"),
                    body.getOrDefault("fullName", body.get("full_name")),
                    body.get("phone")
            );
            return ResponseEntity.status(201).body(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            // Nhận 'identifier' (SĐT/email); fallback 'username' để tương thích trang admin & client cũ
            String identifier = body.getOrDefault("identifier", body.get("username"));
            var result = authService.login(identifier, body.get("password"), extractIp(request));
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable Integer userId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", authService.getProfile(userId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body) {
        try {
            Integer userId = Integer.valueOf(body.get("userId"));
            var data = authService.updateProfile(userId, body.get("fullName"), body.get("email"), body.get("phone"));
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        try {
            Integer userId = Integer.valueOf(body.get("userId"));
            authService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
            return ResponseEntity.ok(Map.of("success", true, "message", "Đổi mật khẩu thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ===== Quên mật khẩu (khách hàng) — gửi OTP về email, xác minh rồi đặt mật khẩu mới =====

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            // Nhận 'identifier' (SĐT/email đã nhập ở ô đăng nhập); gửi OTP về email đã đăng ký của tài khoản
            String identifier = body.getOrDefault("identifier", body.get("email"));
            String maskedEmail = passwordResetService.requestReset(identifier);
            return ResponseEntity.ok(Map.of("success", true, "maskedEmail", maskedEmail,
                    "message", "Đã gửi mã xác minh tới email của tài khoản."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        try {
            String identifier = body.getOrDefault("identifier", body.get("email"));
            passwordResetService.verifyOtp(identifier, body.get("otp"));
            return ResponseEntity.ok(Map.of("success", true, "message", "Mã hợp lệ"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String identifier = body.getOrDefault("identifier", body.get("email"));
            passwordResetService.resetPassword(identifier, body.get("otp"), body.get("newPassword"));
            return ResponseEntity.ok(Map.of("success", true, "message", "Đặt lại mật khẩu thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
