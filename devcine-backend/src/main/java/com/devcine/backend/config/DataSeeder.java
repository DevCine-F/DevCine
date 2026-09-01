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
                jdbcTemplate.execute("ALTER TABLE vouchers ALTER COLUMN applicable_movie_titles_snapshot TYPE TEXT;");
                jdbcTemplate.execute("ALTER TABLE vouchers ALTER COLUMN applicable_movie_ids_snapshot TYPE TEXT;");
                jdbcTemplate.execute("ALTER TABLE vouchers ALTER COLUMN applicable_movie_title_snapshot TYPE TEXT;");
                jdbcTemplate.execute("ALTER TABLE vouchers ALTER COLUMN description_snapshot TYPE TEXT;");
                jdbcTemplate.execute("ALTER TABLE vouchers ALTER COLUMN title_snapshot TYPE TEXT;");
                jdbcTemplate.execute("ALTER TABLE promotions ALTER COLUMN applicable_movie_ids TYPE TEXT;");
                jdbcTemplate.execute("ALTER TABLE promotions ALTER COLUMN description TYPE TEXT;");
                jdbcTemplate.execute("ALTER TABLE promotions ALTER COLUMN name TYPE TEXT;");
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

            // ===== CHUẨN HÓA ĐỊNH DẠNG CHIẾU (MOVIE FORMATS) THEO TITLE CASE & MÔ TẢ =====
            try {
                jdbcTemplate.execute("UPDATE movie_formats SET name = '2D Phụ Đề', description = 'Hình ảnh 2D tiêu chuẩn, âm thanh gốc kèm phụ đề tiếng Việt' WHERE LOWER(TRIM(name)) IN ('2d phụ đề', '2d phu de');");
                jdbcTemplate.execute("UPDATE movie_formats SET name = '2D Lồng Tiếng', description = 'Hình ảnh 2D tiêu chuẩn, âm thanh lồng tiếng Việt phù hợp gia đình và trẻ em' WHERE LOWER(TRIM(name)) IN ('2d lồng tiếng', '2d long tieng');");
                jdbcTemplate.execute("UPDATE movie_formats SET name = '3D Phụ Đề', description = 'Hiệu ứng không gian 3 chiều sống động qua kính 3D, âm thanh gốc kèm phụ đề tiếng Việt' WHERE LOWER(TRIM(name)) IN ('3d phụ đề', '3d phu de');");
                jdbcTemplate.execute("UPDATE movie_formats SET name = '3D Lồng Tiếng', description = 'Hiệu ứng không gian 3 chiều sống động qua kính 3D, âm thanh lồng tiếng Việt sinh động' WHERE LOWER(TRIM(name)) IN ('3d lồng tiếng', '3d long tieng');");
                jdbcTemplate.execute("UPDATE movie_formats SET name = 'Superplex 2D', description = 'Màn chiếu siêu đại Superplex kích thước khổng lồ, hình ảnh 2D sắc nét vượt trội' WHERE LOWER(TRIM(name)) IN ('superplex 2d');");
                jdbcTemplate.execute("UPDATE movie_formats SET name = 'Superplex 3D', description = 'Màn chiếu siêu đại Superplex kết hợp không gian 3D hoành tráng và âm thanh đỉnh cao' WHERE LOWER(TRIM(name)) IN ('superplex 3d');");

                // MÔ TẢ KIỂM DUYỆT (AGE RATINGS)
                jdbcTemplate.execute("UPDATE age_ratings SET description = 'Phim được phép phổ biến rộng rãi đến mọi lứa tuổi người xem' WHERE code = 'P';");
                jdbcTemplate.execute("UPDATE age_ratings SET description = 'Phim dành cho khán giả dưới 13 tuổi với điều kiện có cha mẹ hoặc người giám hộ đi cùng' WHERE code = 'K';");
                jdbcTemplate.execute("UPDATE age_ratings SET description = 'Phim chỉ dành cho khán giả từ đủ 13 tuổi trở lên (13+)' WHERE code = 'T13';");
                jdbcTemplate.execute("UPDATE age_ratings SET description = 'Phim chỉ dành cho khán giả từ đủ 16 tuổi trở lên (16+)' WHERE code = 'T16';");
                jdbcTemplate.execute("UPDATE age_ratings SET description = 'Phim chỉ dành cho khán giả từ đủ 18 tuổi trở lên (18+)' WHERE code = 'T18';");
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

            // ===== PERMISSION MATRIX V8 =====
            // Đặt lại MỘT LẦN qua cờ PERMISSION_MATRIX_V8.
            // V8:
            // - MANAGER: được phân quyền cinemas (view, edit) để chỉnh sửa thông tin rạp/phòng/sơ đồ ghế thuộc quyền quản lý.
            // - ADMIN: toàn quyền mọi tính năng bao gồm cinemas (view, add, edit, delete).
            // - STAFF: tinh gọn tối đa CHỈ có pos_ticketing (view, add).
            boolean permissionMatrixV8 = systemSettingRepository.findById("PERMISSION_MATRIX_V8").isPresent();
            if (!permissionMatrixV8 || adminRole.getPermissionsMatrix() == null || adminRole.getPermissionsMatrix().isBlank()) {
                adminRole.setPermissionsMatrix("{"
                        + "\"dashboard_stats\":[\"view\",\"export\"],"
                        + "\"movies\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"schedules\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"banners\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"promotions\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"pricing\":[\"view\",\"edit\"],"
                        + "\"cinemas\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"staff_management\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"pos_ticketing\":[\"view\",\"add\"],"
                        + "\"fnb_menu\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"bookings\":[\"view\"],"
                        + "\"customers\":[\"view\",\"edit\"],"
                        + "\"audit_logs\":[\"view\"],"
                        + "\"settings\":[\"view\",\"edit\"]}");
                roleRepository.save(adminRole);
            }
            if (!permissionMatrixV8 || staffRole.getPermissionsMatrix() == null || staffRole.getPermissionsMatrix().isBlank()) {
                staffRole.setPermissionsMatrix("{"
                        + "\"pos_ticketing\":[\"view\",\"add\"]}");
                roleRepository.save(staffRole);
            }
            if (!permissionMatrixV8 || managerRole.getPermissionsMatrix() == null || managerRole.getPermissionsMatrix().isBlank()) {
                managerRole.setPermissionsMatrix("{"
                        + "\"dashboard_stats\":[\"view\"],"
                        + "\"pos_ticketing\":[\"view\",\"add\"],"
                        + "\"bookings\":[\"view\"],"
                        + "\"schedules\":[\"view\",\"add\",\"edit\"],"
                        + "\"customers\":[\"view\",\"edit\"],"
                        + "\"cinemas\":[\"view\",\"edit\"],"
                        + "\"staff_management\":[\"view\",\"add\",\"edit\"]}");
                roleRepository.save(managerRole);
            }
            if (!permissionMatrixV8) {
                systemSettingRepository.save(SystemSetting.builder()
                        .settingKey("PERMISSION_MATRIX_V8").settingValue("true").build());
                System.out.println("[DataSeeder] Đã áp dụng Permission Matrix V8.");
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
