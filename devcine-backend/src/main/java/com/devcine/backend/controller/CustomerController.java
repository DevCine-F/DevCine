package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.Customer;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.PointTransactionRepository;
import com.devcine.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;

    /** Danh sách khách hàng cho khu vực quản trị (xem hạng, điểm). Hỗ trợ tìm kiếm theo q. */
    @GetMapping
    @PreAuthorize("@perm.can('customers', 'view')")
    public ResponseEntity<?> listCustomers(@RequestParam(required = false) String q) {
        boolean hasKeyword = q != null && !q.isBlank();
        List<Customer> customers = hasKeyword
                ? customerRepository.searchWithUser(q.trim())
                : customerRepository.findAllWithUser();
        List<Map<String, Object>> result = customers.stream()
                .map(this::buildProfileResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerProfile(@PathVariable Integer id) {
        Customer customer = customerRepository.findById(id).orElse(null);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok(buildProfileResponse(customer)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomerProfile(@PathVariable Integer id,
                                                    @RequestBody Map<String, String> body) {
        try {
            Customer customer = customerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

            if (customer.getUser() != null) {
                var user = customer.getUser();
                if (body.containsKey("fullName") && !body.get("fullName").isBlank()) {
                    user.setFullName(body.get("fullName"));
                }
                // Cho phép đổi email — chặn trùng (email là unique) và validate định dạng cơ bản
                if (body.containsKey("email") && body.get("email") != null && !body.get("email").isBlank()) {
                    String newEmail = body.get("email").trim();
                    if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                        if (!newEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                            throw new RuntimeException("Email không hợp lệ");
                        }
                        if (userRepository.existsByEmail(newEmail)) {
                            throw new RuntimeException("Email đã được sử dụng");
                        }
                        user.setEmail(newEmail);
                    }
                }
                if (body.containsKey("phone")) {
                    String cleanPhone = com.devcine.backend.util.PhoneUtils.validateAndSanitize(body.get("phone"), false);
                    if (cleanPhone != null && !cleanPhone.equals(user.getPhone())) {
                        if (userRepository.existsByPhoneAndIdNot(cleanPhone, user.getId())) {
                            throw new RuntimeException("Số điện thoại " + cleanPhone + " đã được sử dụng bởi một tài khoản khác.");
                        }
                    }
                    user.setPhone(cleanPhone);
                }
                if (body.containsKey("avatarUrl")) {
                    user.setAvatarUrl(body.get("avatarUrl"));
                }
                userRepository.save(user);
            }

            if (body.containsKey("dob") && !body.get("dob").isBlank()) {
                customer.setDob(LocalDate.parse(body.get("dob")));
            }
            if (body.containsKey("idCard")) {
                customer.setIdCard(body.get("idCard"));
            }
            customerRepository.save(customer);

            return ResponseEntity.ok(ApiResponse.ok(buildProfileResponse(customer)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** Lịch sử biến động điểm của khách (mới nhất trước) — cho màn "Lịch sử điểm". */
    @GetMapping("/{id}/point-history")
    public ResponseEntity<?> getPointHistory(@PathVariable Integer id) {
        List<Map<String, Object>> result = pointTransactionRepository
                .findByCustomer_UserIdOrderByCreatedAtDescIdDesc(id).stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("points", t.getPoints());
                    m.put("type", t.getType());
                    m.put("source", t.getSource());
                    m.put("refCode", t.getRefCode());
                    m.put("balanceAfter", t.getBalanceAfter());
                    m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
                    return m;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private Map<String, Object> buildProfileResponse(Customer customer) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", customer.getUserId());
        m.put("fullName", customer.getUser() != null ? customer.getUser().getFullName() : "Khách hàng");
        m.put("email", customer.getUser() != null ? customer.getUser().getEmail() : "");
        m.put("phone", customer.getUser() != null && customer.getUser().getPhone() != null ? customer.getUser().getPhone() : "");
        m.put("avatarUrl", customer.getUser() != null && customer.getUser().getAvatarUrl() != null ? customer.getUser().getAvatarUrl() : "");
        m.put("membershipTier", customer.getMembershipTier() != null ? customer.getMembershipTier() : "BRONZE");
        m.put("loyaltyPoints", customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0);
        m.put("lifetimePoints", customer.getLifetimePoints() != null ? customer.getLifetimePoints() : 0);
        m.put("dob", customer.getDob() != null ? customer.getDob().toString() : "");
        m.put("idCard", customer.getIdCard() != null ? customer.getIdCard() : "");
        m.put("createdAt", customer.getUser() != null && customer.getUser().getCreatedAt() != null ? customer.getUser().getCreatedAt().toString() : "");
        return m;
    }
}
