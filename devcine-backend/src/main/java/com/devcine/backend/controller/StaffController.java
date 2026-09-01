package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Role;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.entity.User;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.RoleRepository;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CinemaRepository cinemaRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.devcine.backend.service.MailService mailService;

    @org.springframework.beans.factory.annotation.Value("${staff.default-password:DevCine@2026}")
    private String defaultStaffPassword;

    private static final java.util.regex.Pattern USERNAME_RE = java.util.regex.Pattern.compile("^[a-z0-9_]{3,20}$");
    private static final java.util.regex.Pattern EMAIL_RE = java.util.regex.Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final java.util.regex.Pattern PHONE_RE = java.util.regex.Pattern.compile("^(03|05|07|08|09)\\d{8}$");
    private static final java.util.regex.Pattern NAME_RE = java.util.regex.Pattern.compile("^[\\p{L}\\p{M} ]+$");

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String toTitleCase(String str) {
        if (str == null || str.isBlank()) return "";
        String[] words = str.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void validateFullName(String v) {
        String s = str(v);
        if (s.isBlank()) throw new IllegalArgumentException("Vui lòng nhập họ và tên.");
        if (s.length() < 2 || s.length() > 50) throw new IllegalArgumentException("Họ tên phải từ 2 đến 50 ký tự.");
        if (!NAME_RE.matcher(s).matches()) throw new IllegalArgumentException("Họ tên chỉ gồm chữ cái và khoảng trắng.");
    }

    private void validateEmailFormat(String v) {
        if (!EMAIL_RE.matcher(str(v)).matches()) throw new IllegalArgumentException("Email không đúng định dạng.");
    }

    private String validateAndSanitizePhone(Object v, boolean required) {
        return com.devcine.backend.util.PhoneUtils.validateAndSanitize(v, required);
    }

    private void validateUsernameFormat(String v) {
        if (!USERNAME_RE.matcher(str(v)).matches())
            throw new IllegalArgumentException("Tài khoản 3-20 ký tự, chỉ gồm chữ thường, số, gạch dưới, không dấu, không khoảng trắng.");
    }

    private static String formatStaffCode(String prefix, int number) {
        return number <= 999 ? String.format("%s%03d", prefix, number) : prefix + number;
    }

    private synchronized String generateStaffCode() {
        String prefix = "DC";
        int next = staffRepository.findAllStaffCodes().stream()
                .filter(code -> code != null && code.startsWith(prefix) && code.substring(prefix.length()).matches("^\\d+$"))
                .map(code -> Integer.parseInt(code.substring(prefix.length())))
                .max(Integer::compareTo)
                .orElse(0) + 1;

        String code = formatStaffCode(prefix, next);
        while (staffRepository.existsByStaffCode(code)) {
            code = formatStaffCode(prefix, ++next);
        }
        return code;
    }

    // Gói thông tin một nhân viên trả về FE (dùng HashMap vì có field cho phép null)
    private Map<String, Object> toStaffMap(Staff s) {
        User u = s.getUser();
        Map<String, Object> m = new HashMap<>();
        m.put("userId", s.getUserId());
        m.put("staffCode", s.getStaffCode());
        m.put("username", u != null ? u.getUsername() : null);
        m.put("fullName", u != null ? u.getFullName() : "Nhân viên");
        m.put("email", u != null ? u.getEmail() : null);
        m.put("phone", u != null ? u.getPhone() : null);
        m.put("avatarUrl", u != null ? u.getAvatarUrl() : null);
        m.put("role", u != null && u.getRole() != null ? u.getRole().getName() : "STAFF");
        m.put("isActive", u != null && Boolean.TRUE.equals(u.getIsActive()));
        LocalDateTime joinedAt = s.getCreatedAt() != null ? s.getCreatedAt() : (u != null ? u.getCreatedAt() : null);
        m.put("joinDate", joinedAt != null ? joinedAt.toString() : null);
        m.put("createdAt", joinedAt != null ? joinedAt.toString() : null);
        m.put("updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null);
        m.put("cinemaId", s.getCinema() != null ? s.getCinema().getId() : null);
        m.put("cinemaName", s.getCinema() != null ? s.getCinema().getName() : null);
        return m;
    }

    /**
     * Danh sách nhân viên + bộ lọc theo cơ sở / trạng thái / từ khoá.
     * Lọc trong Java (dữ liệu nhỏ) để tránh gotcha lower(null) của Postgres khi truyền param null vào JPQL.
     */
    @GetMapping("/next-code")
    @PreAuthorize("@perm.can('staff_management','add')")
    public ResponseEntity<?> getNextStaffCode() {
        return ResponseEntity.ok(ApiResponse.ok(generateStaffCode()));
    }



    @GetMapping("/list")
    @PreAuthorize("@perm.can('staff_management','view')")
    public ResponseEntity<?> getAllStaff(
            @RequestParam(required = false) Integer cinemaId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
            
        final Integer effectiveCinemaId;
        if (!com.devcine.backend.util.SecurityUtils.isAdmin()) {
            effectiveCinemaId = com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId();
        } else {
            effectiveCinemaId = cinemaId;
        }

        final String kw = q != null ? q.trim().toLowerCase() : "";
        List<Map<String, Object>> result = staffRepository.findAllWithDetails().stream()
                .filter(s -> effectiveCinemaId == null || (s.getCinema() != null && effectiveCinemaId.equals(s.getCinema().getId())))
                .filter(s -> {
                    if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) return true;
                    boolean active = s.getUser() != null && Boolean.TRUE.equals(s.getUser().getIsActive());
                    return status.equalsIgnoreCase("ACTIVE") == active;
                })
                .filter(s -> {
                    if (kw.isEmpty()) return true;
                    User u = s.getUser();
                    String name = u != null && u.getFullName() != null ? u.getFullName().toLowerCase() : "";
                    String email = u != null && u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                    String code = s.getStaffCode() != null ? s.getStaffCode().toLowerCase() : "";
                    return name.contains(kw) || email.contains(kw) || code.contains(kw);
                })
                .map(this::toStaffMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/cinema-roster/{cinemaId}")
    @PreAuthorize("@perm.can('staff_management','view')")
    public ResponseEntity<?> getCinemaRoster(@PathVariable Integer cinemaId) {
        List<Map<String, Object>> result = staffRepository.findByCinemaIdWithDetails(cinemaId).stream()
                .map(staff -> {
                    User u = staff.getUser();
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", staff.getUserId());
                    m.put("name", u != null ? u.getFullName() : "Nhân viên");
                    m.put("role", u != null && u.getRole() != null ? u.getRole().getName() : "STAFF");
                    m.put("isActive", u != null && Boolean.TRUE.equals(u.getIsActive()));
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping
    @PreAuthorize("@perm.can('staff_management','add')")
    @Transactional
    public ResponseEntity<?> createStaff(@RequestBody Map<String, Object> body) {
        try {
            String username = str(body.get("username"));
            String email = str(body.get("email"));
            String fullName = toTitleCase(str(body.get("fullName")));

            // Validate chi tiết (đồng bộ với realtime validate ở FE)
            validateFullName(fullName);
            validateUsernameFormat(username);
            validateEmailFormat(email);
            String cleanPhone = validateAndSanitizePhone(body.get("phone"), true);
            if (userRepository.existsByUsername(username))
                throw new IllegalArgumentException("Tài khoản đăng nhập đã tồn tại.");
            if (userRepository.existsByEmail(email))
                throw new IllegalArgumentException("Email đã được sử dụng.");
            if (userRepository.existsByPhone(cleanPhone))
                throw new IllegalArgumentException("Số điện thoại " + cleanPhone + " đã được sử dụng bởi một tài khoản khác.");

            String desiredRole = str(body.get("role")).toUpperCase();
            if (desiredRole.isBlank()) desiredRole = "STAFF";
            if (!desiredRole.equals("STAFF") && !desiredRole.equals("MANAGER")) {
                throw new IllegalArgumentException("Vai trò không hợp lệ.");
            }
            if (desiredRole.equals("MANAGER") && !com.devcine.backend.util.SecurityUtils.isAdmin()) {
                throw new IllegalArgumentException("Chỉ quản trị viên mới tạo được tài khoản quản lý.");
            }
            final String roleName = desiredRole;
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException("Hệ thống chưa cấu hình vai trò " + roleName + "."));

            // Luôn dùng MẬT KHẨU MẶC ĐỊNH + buộc đổi ở lần đăng nhập đầu tiên
            User u = User.builder()
                    .username(username)
                    .email(email)
                    .fullName(fullName)
                    .phone(cleanPhone)
                    .passwordHash(passwordEncoder.encode(defaultStaffPassword))
                    .role(role)
                    .isActive(true)
                    .mustChangePassword(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(u);

            Object finalCinemaId = body.get("cinemaId");
            if (!com.devcine.backend.util.SecurityUtils.isAdmin()) {
                Integer myCinemaId = com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId();
                if (myCinemaId == null) throw new IllegalArgumentException("Bạn chưa được gán cơ sở, không thể tạo nhân viên.");
                if (finalCinemaId != null && !str(finalCinemaId).isBlank() && !str(finalCinemaId).equals(myCinemaId.toString())) {
                    throw new IllegalArgumentException("Bạn chỉ có thể tạo nhân viên cho cơ sở của mình.");
                }
                finalCinemaId = myCinemaId;
            }

            Cinema staffCinema = resolveCinema(finalCinemaId);
            // Cơ sở làm việc BẮT BUỘC cho mọi tài khoản nội bộ
            if (staffCinema == null) {
                throw new IllegalArgumentException("Vui lòng chọn cơ sở làm việc.");
            }
            Staff staff = Staff.builder()
                    .user(u)
                    .staffCode(generateStaffCode())
                    .cinema(staffCinema)
                    .build();
            staffRepository.save(staff); // @MapsId: entity mới (userId null) -> persist

            // Gửi email cấp tài khoản chạy ngầm (Asynchronous)
            mailService.sendStaffCredentials(email, fullName, username, defaultStaffPassword);
            boolean emailSent = true;

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("userId", u.getId());
            resp.put("username", username);
            resp.put("defaultPassword", defaultStaffPassword);
            resp.put("emailSent", emailSent);
            return ResponseEntity.status(201).body(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Không tạo được nhân viên: " + e.getMessage()));
        }
    }

    private static int getRoleRank(String roleName) {
        if (roleName == null) return 0;
        return switch (roleName.toUpperCase()) {
            case "ADMIN" -> 3;
            case "MANAGER" -> 2;
            case "STAFF" -> 1;
            default -> 0;
        };
    }

    private static String roleLabel(String role) {
        if (role == null) return "Nhân viên";
        return switch (role.toUpperCase()) {
            case "ADMIN" -> "Quản trị viên";
            case "MANAGER" -> "Quản lý cơ sở";
            case "STAFF" -> "Nhân viên";
            default -> role;
        };
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('staff_management','edit')")
    @Transactional
    public ResponseEntity<?> updateStaff(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            Integer currentUserId = com.devcine.backend.util.SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(401).body(ApiResponse.fail("Vui lòng đăng nhập để thực hiện thao tác này."));
            }

            Staff staff = staffRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));
            User u = staff.getUser();
            if (u == null) throw new IllegalArgumentException("Nhân viên không hợp lệ.");

            User caller = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin tài khoản đang thao tác."));
            String callerRoleName = caller.getRole() != null ? caller.getRole().getName() : "";
            int callerRank = getRoleRank(callerRoleName);

            String targetRoleName = u.getRole() != null ? u.getRole().getName() : "";
            int targetRank = getRoleRank(targetRoleName);

            boolean isSelf = id.equals(currentUserId);

            if (isSelf) {
                // 1. Chỉ được sửa thông tin của bản thân, KHÔNG được chuyển trạng thái của bản thân
                if (body.containsKey("isActive")) {
                    Boolean requestedActive = Boolean.TRUE.equals(body.get("isActive"));
                    if (!requestedActive.equals(Boolean.TRUE.equals(u.getIsActive()))) {
                        throw new IllegalArgumentException("Bạn không thể tự thay đổi trạng thái hoạt động của chính mình. Vui lòng nhờ cấp trên thao tác!");
                    }
                }
                // Không được tự đổi vai trò của chính mình
                if (body.containsKey("role") && !str(body.get("role")).isBlank() && !str(body.get("role")).equalsIgnoreCase(targetRoleName)) {
                    throw new IllegalArgumentException("Bạn không thể tự thay đổi vai trò của chính mình.");
                }
                // Không được tự chuyển cơ sở làm việc của chính mình
                if (body.containsKey("cinemaId")) {
                    Object cid = body.get("cinemaId");
                    Integer requestedCinemaId = (cid != null && !str(cid).isBlank()) ? Integer.parseInt(str(cid)) : null;
                    Integer currentCinemaId = staff.getCinema() != null ? staff.getCinema().getId() : null;
                    if (!java.util.Objects.equals(requestedCinemaId, currentCinemaId)) {
                        throw new IllegalArgumentException("Bạn không thể tự chuyển đổi cơ sở làm việc của chính mình.");
                    }
                }
            } else {
                // 2. Thao tác trên tài khoản khác:
                // a. Cấp cao hơn
                if (targetRank > callerRank) {
                    throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa thông tin của tài khoản cấp cao hơn.");
                }
                // b. Cùng cấp (ADMIN vs ADMIN, MANAGER vs MANAGER, STAFF vs STAFF)
                if (targetRank == callerRank) {
                    throw new IllegalArgumentException("Bạn không thể chỉnh sửa thông tin của nhân sự cùng cấp (" + roleLabel(targetRoleName) + ").");
                }
                // c. Cấp thấp hơn (callerRank > targetRank)
                if (!com.devcine.backend.util.SecurityUtils.isAdmin()) {
                    Integer myCinemaId = com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId();
                    if (myCinemaId == null) throw new IllegalArgumentException("Bạn chưa được gán cơ sở.");
                    if (staff.getCinema() == null || !staff.getCinema().getId().equals(myCinemaId)) {
                        throw new IllegalArgumentException("Bạn chỉ có thể sửa nhân viên thuộc cơ sở của mình.");
                    }
                    if (body.containsKey("cinemaId")) {
                        Object finalCinemaId = body.get("cinemaId");
                        if (finalCinemaId != null && !str(finalCinemaId).isBlank() && !str(finalCinemaId).equals(myCinemaId.toString())) {
                            throw new IllegalArgumentException("Bạn chỉ có thể gán nhân viên vào cơ sở của mình.");
                        }
                    }
                }
            }

            // Cập nhật họ tên (cho phép cả self và cấp trên)
            if (body.containsKey("fullName") && !str(body.get("fullName")).isBlank()) {
                String fullName = toTitleCase(str(body.get("fullName")));
                validateFullName(fullName);
                u.setFullName(fullName);
            }
            // Cập nhật SĐT (cho phép cả self và cấp trên)
            if (body.containsKey("phone")) {
                String cleanPhone = validateAndSanitizePhone(body.get("phone"), false);
                if (cleanPhone != null && !cleanPhone.equals(u.getPhone())) {
                    if (userRepository.existsByPhoneAndIdNot(cleanPhone, u.getId())) {
                        throw new IllegalArgumentException("Số điện thoại " + cleanPhone + " đã được sử dụng bởi một tài khoản khác.");
                    }
                }
                u.setPhone(cleanPhone);
            }
            // Cập nhật Email (cho phép cả self và cấp trên)
            if (body.containsKey("email")) {
                String email = str(body.get("email"));
                if (!email.isBlank() && !email.equalsIgnoreCase(u.getEmail())) {
                    validateEmailFormat(email);
                    if (userRepository.existsByEmail(email))
                        throw new IllegalArgumentException("Email đã được sử dụng.");
                    u.setEmail(email);
                }
            }

            // Cập nhật trạng thái (chỉ cấp trên mới được đổi trạng thái cấp dưới)
            if (!isSelf && body.containsKey("isActive")) {
                u.setIsActive(Boolean.TRUE.equals(body.get("isActive")));
            }

            // Cập nhật vai trò (chỉ ADMIN đổi cho cấp dưới)
            if (!isSelf && body.containsKey("role") && com.devcine.backend.util.SecurityUtils.isAdmin()) {
                String newRole = str(body.get("role")).toUpperCase();
                if (!newRole.isBlank()) {
                    if (!newRole.equals("STAFF") && !newRole.equals("MANAGER")) {
                        throw new IllegalArgumentException("Vai trò không hợp lệ.");
                    }
                    Role r = roleRepository.findByName(newRole)
                            .orElseThrow(() -> new IllegalArgumentException("Hệ thống chưa cấu hình vai trò " + newRole + "."));
                    u.setRole(r);
                }
            }

            userRepository.save(u);
            staff.setUpdatedAt(LocalDateTime.now());

            // Cập nhật cơ sở làm việc (chỉ cấp trên đổi cho cấp dưới)
            if (!isSelf && body.containsKey("cinemaId")) {
                Object finalCinemaId = body.get("cinemaId");
                if (!com.devcine.backend.util.SecurityUtils.isAdmin()) {
                    Integer myCinemaId = com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId();
                    finalCinemaId = myCinemaId;
                }
                staff.setCinema(resolveCinema(finalCinemaId));
            }

            if ("MANAGER".equalsIgnoreCase(u.getRole() != null ? u.getRole().getName() : "") && staff.getCinema() == null) {
                throw new IllegalArgumentException("Quản lý phải được gán một cơ sở.");
            }
            staffRepository.save(staff);

            return ResponseEntity.ok(ApiResponse.success("Đã cập nhật nhân viên."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Không cập nhật được nhân viên: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("@perm.can('staff_management','edit')")
    @Transactional
    public ResponseEntity<?> toggleStaff(@PathVariable Integer id) {
        Integer currentUserId = com.devcine.backend.util.SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("Vui lòng đăng nhập để thực hiện thao tác này."));
        }

        if (id.equals(currentUserId)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Bạn không thể tự đổi trạng thái tài khoản của chính mình. Vui lòng nhờ cấp trên thao tác!"));
        }

        Staff staff = staffRepository.findByIdWithDetails(id).orElse(null);
        if (staff == null || staff.getUser() == null)
            return ResponseEntity.badRequest().body(ApiResponse.fail("Không tìm thấy nhân viên."));

        User caller = userRepository.findById(currentUserId).orElse(null);
        String callerRoleName = caller != null && caller.getRole() != null ? caller.getRole().getName() : "";
        int callerRank = getRoleRank(callerRoleName);

        User u = staff.getUser();
        String targetRoleName = u.getRole() != null ? u.getRole().getName() : "";
        int targetRank = getRoleRank(targetRoleName);

        if (targetRank > callerRank) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Bạn không có quyền thay đổi trạng thái tài khoản cấp cao hơn."));
        }
        if (targetRank == callerRank) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Bạn không thể thay đổi trạng thái tài khoản của nhân sự cùng cấp (" + roleLabel(targetRoleName) + ")."));
        }

        if (!com.devcine.backend.util.SecurityUtils.isAdmin()) {
            Integer myCinemaId = com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId();
            if (staff.getCinema() == null || !staff.getCinema().getId().equals(myCinemaId)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Bạn chỉ có thể đổi trạng thái nhân viên thuộc cơ sở của mình."));
            }
        }

        u.setIsActive(!Boolean.TRUE.equals(u.getIsActive()));
        userRepository.save(u);
        staff.setUpdatedAt(LocalDateTime.now());
        staffRepository.save(staff);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "isActive", u.getIsActive())));
    }

    private Cinema resolveCinema(Object cinemaId) {
        if (cinemaId == null || str(cinemaId).isBlank()) return null;
        return cinemaRepository.findById(Integer.parseInt(str(cinemaId))).orElse(null);
    }

    @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}
