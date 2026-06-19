package com.devcine.backend.service;

import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import com.devcine.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final WalletRepository walletRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    @Transactional
    public Map<String, Object> register(String username, String email, String password,
                                         String fullName, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("CUSTOMER").build()));

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

        Wallet wallet = Wallet.builder()
                .customer(customer)
                .balance(BigDecimal.ZERO)
                .status("ACTIVE")
                .build();
        walletRepository.save(wallet);

        log.info("Registered new customer: {}", username);
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", customerRole.getName()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> login(String username, String password, String ipAddress) {
        User user = userRepository.findByUsernameWithRole(username)
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập hoặc mật khẩu không đúng"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        String role = user.getRole().getName();
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role);

        // Ghi nhật ký đăng nhập cho tài khoản quản trị / nhân viên
        if ("ADMIN".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role)) {
            auditLogService.record(user.getId(), "LOGIN", "auth", ipAddress);
        }

        log.info("User logged in: {}", username);
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
