package com.devcine.backend.service;

import com.devcine.backend.entity.User;
import com.devcine.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quên mật khẩu phía khách: người dùng nhập email; nếu email CÓ trong hệ thống thì gửi mã OTP 6 số
 * về chính email đó, xác minh rồi cho đặt mật khẩu mới.
 *
 * <p>OTP lưu in-memory (đủ cho 1 instance backend, mã ngắn hạn 10') — không cần bảng DB. Có giới hạn
 * tần suất gửi lại (cooldown) và số lần nhập sai để chống dò mã. Khóa map theo email đã chuẩn hoá.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private static final int TTL_MIN = 10;                 // mã hiệu lực 10 phút
    private static final long TTL_MS = TTL_MIN * 60_000L;
    private static final long RESEND_COOLDOWN_MS = 30_000L; // chờ 30s mới được gửi lại
    private static final int MAX_ATTEMPTS = 5;             // tối đa 5 lần nhập sai

    private final Map<String, Otp> otps = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private static final class Otp {
        final String code;
        final long createdAt;
        final long expiresAt;
        int attempts;
        Otp(String code, long now) { this.code = code; this.createdAt = now; this.expiresAt = now + TTL_MS; }
    }

    private String key(String email) { return email == null ? "" : email.trim().toLowerCase(); }

    /** Tra tài khoản theo email — email phải tồn tại trong hệ thống mới gửi mã. */
    private User resolveUser(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Vui lòng nhập email.");
        }
        return userRepository.findByEmailIgnoreCase(key(email)).stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với email này."));
    }

    /** Bước 1: kiểm tra email có trong hệ thống → sinh OTP và gửi về chính email đó. */
    public void requestReset(String email) {
        String k = key(email);
        User user = resolveUser(email);

        long now = System.currentTimeMillis();
        Otp existing = otps.get(k);
        if (existing != null && now - existing.createdAt < RESEND_COOLDOWN_MS) {
            long wait = Math.max(1, (RESEND_COOLDOWN_MS - (now - existing.createdAt)) / 1000);
            throw new RuntimeException("Vui lòng đợi " + wait + " giây trước khi gửi lại mã.");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        // Ghi đè mã OTP cũ thành vô hiệu (thay thế bằng mã mới)
        otps.put(k, new Otp(code, now));
        
        mailService.sendOtpEmail(user.getEmail(), code, TTL_MIN);
        log.info("Đã phát hành OTP đặt lại mật khẩu tới {}", user.getEmail());
    }

    /** Bước 2: xác minh mã (không tiêu thụ) để FE cho sang màn nhập mật khẩu mới. */
    public void verifyOtp(String email, String code) {
        validate(email, code);
    }

    /** Bước 3: xác minh lại + đặt mật khẩu mới rồi tiêu thụ mã. */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 8 ký tự.");
        }
        validate(email, code);
        User user = resolveUser(email);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        otps.remove(key(email));
        log.info("Đặt lại mật khẩu thành công cho {}", user.getUsername());
    }

    /** Kiểm tra OTP: tồn tại, chưa hết hạn, chưa vượt số lần thử, khớp mã. Sai → tăng attempts. */
    private void validate(String email, String code) {
        String k = key(email);
        Otp otp = otps.get(k);
        if (otp == null) {
            throw new RuntimeException("Mã xác minh không tồn tại hoặc đã được dùng. Vui lòng yêu cầu gửi lại.");
        }
        if (System.currentTimeMillis() > otp.expiresAt) {
            otps.remove(k);
            throw new RuntimeException("Mã xác minh đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }
        if (otp.attempts >= MAX_ATTEMPTS) {
            otps.remove(k);
            throw new RuntimeException("Bạn đã nhập sai quá số lần cho phép. Vui lòng yêu cầu mã mới.");
        }
        if (code == null || !otp.code.equals(code.trim())) {
            otp.attempts++;
            throw new RuntimeException("Mã xác minh không đúng.");
        }
    }
}
