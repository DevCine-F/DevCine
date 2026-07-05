package com.devcine.backend.service;

import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import com.devcine.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;
    private final StaffRepository staffRepository;

    @Transactional
    public Map<String, Object> register(String email, String password, String fullName, String phone) {
        // Tất cả các trường bắt buộc
        if (fullName == null || fullName.isBlank()
                || email == null || email.isBlank()
                || phone == null || phone.isBlank()
                || password == null || password.isBlank()) {
            throw new RuntimeException("Vui lòng nhập đầy đủ họ tên, email, số điện thoại và mật khẩu");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email này đã được sử dụng");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new RuntimeException("Số điện thoại này đã được sử dụng");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("CUSTOMER").build()));

        // Username là cột bắt buộc & duy nhất -> tự sinh từ số điện thoại (định danh đăng nhập mới)
        String username = phone;
        if (userRepository.existsByUsername(username)) {
            username = phone + "_" + (System.currentTimeMillis() % 100000);
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .phone(phone)
                .role(customerRole)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.saveAndFlush(user);

        Customer customer = Customer.builder()
                .user(user)
                .membershipTier("BRONZE")
                .loyaltyPoints(0)
                .build();
        customerRepository.save(customer);

        log.info("Registered new customer by phone: {}", phone);
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", customerRole.getName()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> login(String identifier, String password, String ipAddress) {
        if (identifier == null || identifier.isBlank()) {
            throw new RuntimeException("Vui lòng nhập số điện thoại hoặc email");
        }
        // Có thể có nhiều ứng viên (dữ liệu lịch sử trùng SĐT) -> chọn user khớp mật khẩu
        User user = userRepository.findByLoginIdentifier(identifier.trim()).stream()
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Số điện thoại/email hoặc mật khẩu không đúng"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }

        String role = user.getRole().getName();
        Integer cinemaId = null;
        // STAFF và MANAGER đều gắn với một cơ sở (bản ghi Staff) -> nạp cinemaId để scoping theo cơ sở
        if ("STAFF".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) {
            Staff staff = staffRepository.findById(user.getId()).orElse(null);
            if (staff != null && staff.getCinema() != null) {
                cinemaId = staff.getCinema().getId();
            }
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role, cinemaId);

        // Ghi nhật ký đăng nhập cho tài khoản quản trị / quản lý / nhân viên
        if ("ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role)) {
            auditLogService.record(user.getId(), "LOGIN", "auth", ipAddress);
        }

        log.info("User logged in: {}", user.getUsername());
        return Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName(),
                        "role", role
                )
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        return profileMap(user);
    }

    @Transactional
    public Map<String, Object> updateProfile(Integer userId, String fullName, String email, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (email != null && !email.isBlank() && !email.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("Email đã được sử dụng");
            }
            user.setEmail(email);
        }
        if (fullName != null && !fullName.isBlank()) user.setFullName(fullName);
        if (phone != null) user.setPhone(phone);

        userRepository.save(user);
        log.info("Profile updated for user: {}", user.getUsername());
        return profileMap(user);
    }

    private Map<String, Object> profileMap(User user) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("email", user.getEmail());
        m.put("fullName", user.getFullName());
        m.put("phone", user.getPhone() != null ? user.getPhone() : "");
        m.put("role", user.getRole() != null ? user.getRole().getName() : "");
        m.put("isActive", Boolean.TRUE.equals(user.getIsActive()));
        m.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        return m;
    }

    @Transactional
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }
}
