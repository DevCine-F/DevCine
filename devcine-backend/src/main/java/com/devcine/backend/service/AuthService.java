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
        String cleanPhone = com.devcine.backend.util.PhoneUtils.validateAndSanitize(phone, true);
        if (userRepository.existsByEmail(email.trim())) {
            throw new RuntimeException("Email này đã được sử dụng");
        }
        if (userRepository.existsByPhone(cleanPhone)) {
            throw new RuntimeException("Số điện thoại " + cleanPhone + " đã được sử dụng bởi một tài khoản khác.");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("CUSTOMER").build()));

        // Username là cột bắt buộc & duy nhất -> tự sinh từ số điện thoại (định danh đăng nhập mới)
        String username = cleanPhone;
        if (userRepository.existsByUsername(username)) {
            username = cleanPhone + "_" + (System.currentTimeMillis() % 100000);
        }

        User user = User.builder()
                .username(username)
                .email(email.trim())
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName.trim())
                .phone(cleanPhone)
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

        log.info("Registered new customer by phone: {}", cleanPhone);
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "role", user.getRole().getName()
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
            Customer customer = customerRepository.findById(user.getId()).orElse(null);
            String reason = (customer != null && customer.getLockReason() != null && !customer.getLockReason().isBlank())
                    ? customer.getLockReason().trim() : null;
            String msg = reason != null
                    ? "Tài khoản của bạn đã bị tạm khóa (Lý do: " + reason + "). Vui lòng liên hệ bộ phận Chăm sóc khách hàng hoặc Hotline để được hỗ trợ."
                    : "Tài khoản của bạn đã bị tạm khóa do vi phạm chính sách hoặc theo yêu cầu. Vui lòng liên hệ bộ phận Chăm sóc khách hàng hoặc Hotline để được hỗ trợ.";
            throw new RuntimeException(msg);
        }

        String role = user.getRole().getName();
        Integer cinemaId = null;
        String cinemaName = null;
        // STAFF và MANAGER đều gắn với một cơ sở (bản ghi Staff) -> nạp cinemaId để scoping theo cơ sở.
        // Tên cơ sở trả kèm cho FE hiển thị trên topbar (ADMIN không thuộc cơ sở nào -> null).
        if ("STAFF".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) {
            Staff staff = staffRepository.findById(user.getId()).orElse(null);
            if (staff != null && staff.getCinema() != null) {
                cinemaId = staff.getCinema().getId();
                cinemaName = staff.getCinema().getName();
            }
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role, cinemaId);

        // Ghi nhật ký đăng nhập cho tài khoản quản trị / quản lý / nhân viên
        if ("ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role)) {
            auditLogService.record(user.getId(), "LOGIN", "auth", ipAddress);
        }

        log.info("User logged in: {}", user.getUsername());
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("fullName", user.getFullName());
        userMap.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        userMap.put("role", role);
        userMap.put("mustChangePassword", Boolean.TRUE.equals(user.getMustChangePassword()));
        userMap.put("cinemaId", cinemaId);
        userMap.put("cinemaName", cinemaName);
        return Map.of("token", token, "user", userMap);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        return profileMap(user);
    }

    @Transactional
    public Map<String, Object> updateProfile(Integer userId, String fullName, String email, String phone) {
        return updateProfile(userId, fullName, email, phone, null);
    }

    @Transactional
    public Map<String, Object> updateProfile(Integer userId, String fullName, String email, String phone, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (email != null && !email.isBlank() && !email.equalsIgnoreCase(user.getEmail())) {
            String newEmail = email.trim();
            if (userRepository.existsByEmail(newEmail)) {
                throw new RuntimeException("Email đã được sử dụng");
            }
            user.setEmail(newEmail);
        }
        if (fullName != null && !fullName.isBlank()) user.setFullName(fullName.trim());
        if (phone != null) {
            String cleanPhone = com.devcine.backend.util.PhoneUtils.validateAndSanitize(phone, false);
            if (cleanPhone != null && !cleanPhone.equals(user.getPhone())) {
                if (userRepository.existsByPhoneAndIdNot(cleanPhone, userId)) {
                    throw new RuntimeException("Số điện thoại " + cleanPhone + " đã được sử dụng bởi một tài khoản khác.");
                }
            }
            user.setPhone(cleanPhone);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl.isBlank() ? null : avatarUrl.trim());
        }

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
        m.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
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

        if (Boolean.TRUE.equals(user.getMustChangePassword())) {
            // Đổi mật khẩu lần đầu: người dùng đã xác thực đăng nhập trước đó, không bắt buộc nhập mật khẩu cũ
            if (oldPassword != null && !oldPassword.isBlank()) {
                if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                    throw new RuntimeException("Mật khẩu hiện tại không đúng");
                }
            }
        } else {
            if (oldPassword == null || oldPassword.isBlank() || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                throw new RuntimeException("Mật khẩu hiện tại không đúng");
            }
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Mật khẩu mới phải có tối thiểu 8 ký tự");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Đổi mật khẩu thành công → giải phóng cờ buộc đổi lần đầu (kích hoạt tài khoản)
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }
}
