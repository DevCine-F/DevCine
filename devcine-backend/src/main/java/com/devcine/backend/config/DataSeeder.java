package com.devcine.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.devcine.backend.entity.Role;
import com.devcine.backend.entity.SystemSetting;
import com.devcine.backend.entity.User;
import com.devcine.backend.repository.RoleRepository;
import com.devcine.backend.repository.SystemSettingRepository;
import com.devcine.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData(
            RoleRepository roleRepository,
            SystemSettingRepository systemSettingRepository,
            UserRepository userRepository,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return args -> {

            // ===== MIGRATION CỘT TRÊN BẢNG PROMOTIONS & VOUCHERS & UNIQUE PHONE =====
            try {
                jdbcTemplate.execute("ALTER TABLE promotions ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;");
                jdbcTemplate.execute("ALTER TABLE vouchers ADD COLUMN IF NOT EXISTS min_order_value_snapshot NUMERIC(15, 2);");
                // Chuẩn hóa chuỗi rỗng thành NULL và dọn dẹp các SĐT trùng cũ nếu có trước khi tạo Partial Unique Index
                jdbcTemplate.execute("UPDATE users SET phone = NULL WHERE phone IS NOT NULL AND TRIM(phone) = '';");
                jdbcTemplate.execute("WITH ranked_users AS (" +
                        "    SELECT id, phone, ROW_NUMBER() OVER (PARTITION BY phone ORDER BY id ASC) as rn " +
                        "    FROM users WHERE phone IS NOT NULL AND phone <> ''" +
                        ") " +
                        "UPDATE users u SET phone = NULL FROM ranked_users r WHERE u.id = r.id AND r.rn > 1;");
                jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_users_phone ON users(phone) WHERE phone IS NOT NULL AND phone <> '';");
            } catch (Exception ignored) {
            }

            // ===== CHUẨN HÓA ĐỊNH DẠNG CHIẾU (MOVIE FORMATS) THEO TITLE CASE =====
            try {
                jdbcTemplate.execute("UPDATE movie_formats SET name = '2D Phụ Đề' WHERE LOWER(TRIM(name)) IN ('2d phụ đề', '2d phu de');");
                jdbcTemplate.execute("UPDATE movie_formats SET name = '2D Lồng Tiếng' WHERE LOWER(TRIM(name)) IN ('2d lồng tiếng', '2d long tieng');");
                jdbcTemplate.execute("UPDATE movie_formats SET name = '3D Phụ Đề' WHERE LOWER(TRIM(name)) IN ('3d phụ đề', '3d phu de');");
                jdbcTemplate.execute("UPDATE movie_formats SET name = '3D Lồng Tiếng' WHERE LOWER(TRIM(name)) IN ('3d lồng tiếng', '3d long tieng');");
                jdbcTemplate.execute("UPDATE movie_formats SET name = 'Superplex 2D' WHERE LOWER(TRIM(name)) IN ('superplex 2d');");
                jdbcTemplate.execute("UPDATE movie_formats SET name = 'Superplex 3D' WHERE LOWER(TRIM(name)) IN ('superplex 3d');");
            } catch (Exception ignored) {
            }

            // ===== ROLES =====
            Role adminRole = roleRepository.findByName("ADMIN").orElseGet(()
                    -> roleRepository.save(Role.builder().name("ADMIN").build()));
            Role managerRole = roleRepository.findByName("MANAGER").orElseGet(()
                    -> roleRepository.save(Role.builder().name("MANAGER").build()));
            Role staffRole = roleRepository.findByName("STAFF").orElseGet(()
                    -> roleRepository.save(Role.builder().name("STAFF").build()));
            roleRepository.findByName("CUSTOMER").orElseGet(()
                    -> roleRepository.save(Role.builder().name("CUSTOMER").build()));

            // ===== PERMISSION MATRIX V6 =====
            // Đặt lại MỘT LẦN qua cờ PERMISSION_MATRIX_V6.
            // V6: thêm feature incident_handling (view/handle) — Xử lý sự cố phòng chiếu / đổi ghế đền bù.
            boolean permissionMatrixV6 = systemSettingRepository.findById("PERMISSION_MATRIX_V6").isPresent();
            if (!permissionMatrixV6 || adminRole.getPermissionsMatrix() == null || adminRole.getPermissionsMatrix().isBlank()) {
                adminRole.setPermissionsMatrix("{"
                        + "\"incident_handling\":[\"view\",\"handle\"],"
                        + "\"dashboard_stats\":[\"view\",\"export\"],"
                        + "\"movies\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"schedules\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"banners\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"promotions\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"pricing\":[\"view\",\"edit\"],"
                        + "\"cinemas\":[\"view\"],"
                        + "\"staff_management\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"pos_ticketing\":[\"view\",\"add\"],"
                        + "\"fnb_menu\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"bookings\":[\"view\",\"delete\"],"
                        + "\"approvals\":[\"view\",\"edit\"],"
                        + "\"customers\":[\"view\"],"
                        + "\"audit_logs\":[\"view\"],"
                        + "\"support\":[\"view\",\"edit\",\"delete\"],"
                        + "\"settings\":[\"view\",\"edit\"]}");
                roleRepository.save(adminRole);
            }
            if (!permissionMatrixV6 || staffRole.getPermissionsMatrix() == null || staffRole.getPermissionsMatrix().isBlank()) {
                staffRole.setPermissionsMatrix("{"
                        + "\"incident_handling\":[\"view\",\"handle\"],"
                        + "\"movies\":[\"view\"],"
                        + "\"schedules\":[\"view\"],"
                        + "\"pos_ticketing\":[\"view\",\"add\"],"
                        + "\"bookings\":[\"view\"],"
                        + "\"approvals\":[\"view\"],"
                        + "\"customers\":[\"view\"],"
                        + "\"support\":[\"view\"]}");
                roleRepository.save(staffRole);
            }
            if (!permissionMatrixV6 || managerRole.getPermissionsMatrix() == null || managerRole.getPermissionsMatrix().isBlank()) {
                managerRole.setPermissionsMatrix("{"
                        + "\"incident_handling\":[\"view\",\"handle\"],"
                        + "\"dashboard_stats\":[\"view\"],"
                        + "\"movies\":[\"view\"],"
                        + "\"schedules\":[\"view\",\"add\",\"edit\"],"
                        + "\"banners\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"promotions\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"pricing\":[\"view\",\"edit\"],"
                        + "\"cinemas\":[\"view\"],"
                        + "\"staff_management\":[\"view\",\"add\",\"edit\"],"
                        + "\"pos_ticketing\":[\"view\",\"add\"],"
                        + "\"fnb_menu\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"bookings\":[\"view\"],"
                        + "\"approvals\":[\"view\",\"edit\"],"
                        + "\"customers\":[\"view\"],"
                        + "\"support\":[\"view\",\"edit\"],"
                        + "\"settings\":[\"view\"]}");
                roleRepository.save(managerRole);
            }
            if (!permissionMatrixV6) {
                systemSettingRepository.save(SystemSetting.builder()
                        .settingKey("PERMISSION_MATRIX_V6").settingValue("true").build());
                System.out.println("[DataSeeder] Đã áp dụng Permission Matrix V6.");
            }

            // ===== TÀI KHOẢN ADMIN (bắt buộc để đăng nhập lần đầu) =====
            User adminUser = userRepository.findByUsername("admin").orElse(null);
            if (adminUser == null) {
                adminUser = User.builder()
                        .username("admin")
                        .email("admin@devcine.com")
                        .passwordHash(passwordEncoder.encode("123"))
                        .fullName("Quản trị viên")
                        .role(adminRole)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                userRepository.save(adminUser);
                System.out.println("[DataSeeder] Đã tạo tài khoản admin mặc định (admin / 123).");
            } else if (!passwordEncoder.matches("123", adminUser.getPasswordHash())) {
                // Reset mật khẩu nếu bị thay đổi ngoài ý muốn (môi trường dev)
                adminUser.setPasswordHash(passwordEncoder.encode("123"));
                adminUser.setRole(adminRole);
                adminUser.setIsActive(true);
                userRepository.save(adminUser);
                System.out.println("[DataSeeder] Đã đặt lại mật khẩu admin về (admin / 123).");
            }
        };
    }
}
