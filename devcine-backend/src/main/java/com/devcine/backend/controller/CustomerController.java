package com.devcine.backend.controller;

import com.devcine.backend.entity.Customer;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    /** Danh sách khách hàng cho khu vực quản trị (xem hạng, điểm). Hỗ trợ tìm kiếm theo q. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> listCustomers(@RequestParam(required = false) String q) {
        boolean hasKeyword = q != null && !q.isBlank();
        List<Customer> customers = hasKeyword
                ? customerRepository.searchWithUser(q.trim())
                : customerRepository.findAllWithUser();
        List<Map<String, Object>> result = customers.stream()
                .map(this::buildProfileResponse).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerProfile(@PathVariable Integer id) {
        Customer customer = customerRepository.findById(id).orElse(null);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(buildProfileResponse(customer));
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
                if (body.containsKey("phone")) {
                    user.setPhone(body.get("phone"));
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

            return ResponseEntity.ok(Map.of("success", true, "data", buildProfileResponse(customer)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private Map<String, Object> buildProfileResponse(Customer customer) {
        return Map.of(
            "userId", customer.getUserId(),
            "fullName", customer.getUser() != null ? customer.getUser().getFullName() : "Khách hàng",
            "email", customer.getUser() != null ? customer.getUser().getEmail() : "",
            "phone", customer.getUser() != null ? (customer.getUser().getPhone() != null ? customer.getUser().getPhone() : "") : "",
            "avatarUrl", customer.getUser() != null && customer.getUser().getAvatarUrl() != null ? customer.getUser().getAvatarUrl() : "",
            "membershipTier", customer.getMembershipTier() != null ? customer.getMembershipTier() : "BRONZE",
            "loyaltyPoints", customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0,
            "dob", customer.getDob() != null ? customer.getDob().toString() : "",
            "idCard", customer.getIdCard() != null ? customer.getIdCard() : "",
            "createdAt", customer.getUser() != null && customer.getUser().getCreatedAt() != null ? customer.getUser().getCreatedAt().toString() : ""
        );
    }
}
