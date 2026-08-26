package com.devcine.backend;

import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.Role;
import com.devcine.backend.entity.User;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.RoleRepository;
import com.devcine.backend.repository.UserRepository;
import com.devcine.backend.service.AuthService;
import com.devcine.backend.util.PhoneUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PhoneValidationAndUniqueTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuthService authService;

    private String randomTestPhone() {
        return "093" + (1000000 + new Random().nextInt(8999999));
    }

    @Test
    @DisplayName("TC-03: Format Sanitization (+84, dấu chấm, khoảng trắng, gạch ngang)")
    void testFormatSanitization() {
        assertEquals("0901234567", PhoneUtils.validateAndSanitize("+84 901 234 567", true));
        assertEquals("0901234567", PhoneUtils.validateAndSanitize("0901.234.567", true));
        assertEquals("0901234567", PhoneUtils.validateAndSanitize("  0901234567  ", true));
        assertEquals("0901234567", PhoneUtils.validateAndSanitize("+84901234567", true));
        assertEquals("0901234567", PhoneUtils.validateAndSanitize("84901234567", true));
        assertEquals("0901234567", PhoneUtils.validateAndSanitize("090-123-4567", true));

        assertNull(PhoneUtils.validateAndSanitize("   ", false));
        assertNull(PhoneUtils.validateAndSanitize(null, false));

        // Sai định dạng / không đủ 10 số / sai đầu số
        assertThrows(IllegalArgumentException.class, () -> PhoneUtils.validateAndSanitize("123456", true));
        assertThrows(IllegalArgumentException.class, () -> PhoneUtils.validateAndSanitize("0123456789", true)); // đầu 01 không thuộc mạng VN
        assertThrows(IllegalArgumentException.class, () -> PhoneUtils.validateAndSanitize("", true));
    }

    @Test
    @DisplayName("TC-02: Cho phép nhiều bản ghi có phone = NULL trong database")
    void testMultipleNullPhonesAllowed() {
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_users_phone ON users(phone) WHERE phone IS NOT NULL AND phone <> '';");

        String username1 = "test_null_1_" + System.currentTimeMillis();
        String username2 = "test_null_2_" + System.currentTimeMillis();
        String email1 = username1 + "@devcine.test";
        String email2 = username2 + "@devcine.test";

        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();

        User u1 = userRepository.save(User.builder()
                .username(username1).email(email1).passwordHash("hash").fullName("Null Phone 1")
                .role(customerRole).phone(null).isActive(true).createdAt(LocalDateTime.now()).build());

        User u2 = userRepository.save(User.builder()
                .username(username2).email(email2).passwordHash("hash").fullName("Null Phone 2")
                .role(customerRole).phone(null).isActive(true).createdAt(LocalDateTime.now()).build());

        assertNotNull(u1.getId());
        assertNotNull(u2.getId());

        // Dọn dẹp
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", u1.getId(), u2.getId());
    }

    @Test
    @DisplayName("TC-01: Chèn 2 bản ghi cùng SĐT -> DB Unique Index chặn")
    void testDuplicatePhoneRejectedByDb() {
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_users_phone ON users(phone) WHERE phone IS NOT NULL AND phone <> '';");

        String phoneTest = randomTestPhone();
        String username1 = "dup_user_1_" + System.currentTimeMillis();
        String username2 = "dup_user_2_" + System.currentTimeMillis();

        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();

        User u1 = userRepository.saveAndFlush(User.builder()
                .username(username1).email(username1 + "@devcine.test").passwordHash("hash").fullName("User 1")
                .role(customerRole).phone(phoneTest).isActive(true).createdAt(LocalDateTime.now()).build());

        // Chèn bản ghi thứ 2 cùng phoneTest -> Phải ném DataIntegrityViolationException từ DB
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(User.builder()
                    .username(username2).email(username2 + "@devcine.test").passwordHash("hash").fullName("User 2")
                    .role(customerRole).phone(phoneTest).isActive(true).createdAt(LocalDateTime.now()).build());
        });

        // Dọn dẹp
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", u1.getId());
    }

    @Test
    @DisplayName("TC-04 & TC-05: Self-Update thành công & Duplicate Update bị chặn")
    void testSelfUpdateAndDuplicateUpdate() {
        String phoneA = randomTestPhone();
        String phoneB = randomTestPhone();

        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();

        User userA = userRepository.saveAndFlush(User.builder()
                .username("user_a_" + System.currentTimeMillis()).email("usera_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash").fullName("Nguyen Van A").phone(phoneA).role(customerRole).isActive(true).createdAt(LocalDateTime.now()).build());

        User userB = userRepository.saveAndFlush(User.builder()
                .username("user_b_" + System.currentTimeMillis()).email("userb_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash").fullName("Tran Thi B").phone(phoneB).role(customerRole).isActive(true).createdAt(LocalDateTime.now()).build());

        // TC-04: User A cập nhật giữ nguyên phoneA hoặc đổi tên -> Thành công
        assertDoesNotThrow(() -> {
            authService.updateProfile(userA.getId(), "Nguyen Van A Updated", userA.getEmail(), phoneA);
        });

        // TC-05: User B cố tình đổi sang phoneA của User A -> Bị chặn
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            authService.updateProfile(userB.getId(), "Tran Thi B", userB.getEmail(), phoneA);
        });
        assertTrue(ex.getMessage().contains("đã được sử dụng"));

        // Dọn dẹp
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", userA.getId(), userB.getId());
    }

    @Test
    @DisplayName("TC-06: POS Tra cứu theo SĐT chuẩn hóa (+84, 09x) ra đúng 1 khách hàng")
    void testPosLookupByPhone() {
        String phone = "0901234567";
        String clean = PhoneUtils.sanitize("+84 901 234 567");
        assertEquals("0901234567", clean);

        Optional<Customer> customerOpt = customerRepository.findFirstByUserPhone(clean);
        assertTrue(customerOpt.isPresent(), "Phải tìm thấy khách hàng Demo với số 0901234567");
        assertEquals("Khách hàng Demo", customerOpt.get().getUser().getFullName());
    }
}
