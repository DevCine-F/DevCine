package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.request.ShiftHandoverRequest;
import com.devcine.backend.dto.request.StaffShiftRequest;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Role;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.entity.StaffSchedule;
import com.devcine.backend.entity.User;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.RoleRepository;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.repository.StaffScheduleRepository;
import com.devcine.backend.repository.UserRepository;
import com.devcine.backend.service.ShiftHandoverService;
import com.devcine.backend.service.StaffScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    private final StaffScheduleRepository staffScheduleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CinemaRepository cinemaRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final StaffScheduleService staffScheduleService;
    private final ShiftHandoverService shiftHandoverService;
    private final com.devcine.backend.service.MailService mailService;

    @org.springframework.beans.factory.annotation.Value("${staff.default-password:DevCine@2026}")
    private String defaultStaffPassword;

    private static final java.util.regex.Pattern USERNAME_RE = java.util.regex.Pattern.compile("^[a-zA-Z0-9._]{3,30}$");
    private static final java.util.regex.Pattern STAFFCODE_RE = java.util.regex.Pattern.compile("^[A-Z0-9]{3,15}$");
    private static final java.util.regex.Pattern EMAIL_RE = java.util.regex.Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final java.util.regex.Pattern PHONE_RE = java.util.regex.Pattern.compile("^(0[35789])[0-9]{8}$");
    private static final java.util.regex.Pattern NAME_RE = java.util.regex.Pattern.compile("^[\\p{L}\\p{M} ]+$");

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
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

    private void validatePhoneFormat(Object v) {
        String s = str(v);
        if (s.isBlank()) return; // không bắt buộc
        if (!PHONE_RE.matcher(s).matches())
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (10 số, bắt đầu 03/05/07/08/09).");
    }

    private void validateUsernameFormat(String v) {
        if (!USERNAME_RE.matcher(str(v)).matches())
            throw new IllegalArgumentException("Tài khoản 3-30 ký tự, chỉ gồm chữ/số/dấu chấm/gạch dưới, không dấu, không khoảng trắng.");
    }

    private static String formatStaffCode(int number) {
        return number <= 999 ? String.format("DC%03d", number) : "DC" + number;
    }

    private synchronized String generateStaffCode() {
        int next = staffRepository.findAllStaffCodes().stream()
                .filter(code -> code != null && code.matches("^DC\\d+$"))
                .map(code -> Integer.parseInt(code.substring(2)))
                .max(Integer::compareTo)
                .orElse(0) + 1;

        String code = formatStaffCode(next);
        while (staffRepository.existsByStaffCode(code)) {
            code = formatStaffCode(++next);
        }
        return code;
    }

    private String resolveStaffCodeForCreate(Object value) {
        String staffCode = str(value);
        if (staffCode.isBlank()) {
            return generateStaffCode();
        }
        staffCode = staffCode.toUpperCase();
        if (!STAFFCODE_RE.matcher(staffCode).matches()) {
            throw new IllegalArgumentException("Mã nhân viên 3-15 ký tự, chỉ gồm chữ IN HOA và chữ số (VD: DC001).");
        }
        if (staffRepository.existsByStaffCodeIgnoreCase(staffCode)) {
            throw new IllegalArgumentException("Mã nhân viên đã tồn tại.");
        }
        return staffCode;
    }

    private void updateStaffCode(Staff staff, Object value) {
        if (value == null) return;
        String staffCode = str(value);
        if (staffCode.isBlank()) {
            throw new IllegalArgumentException("Mã nhân viên không được để trống.");
        }
        staffCode = staffCode.toUpperCase();
        if (!STAFFCODE_RE.matcher(staffCode).matches()) {
            throw new IllegalArgumentException("Mã nhân viên 3-15 ký tự, chỉ gồm chữ IN HOA và chữ số (VD: DC001).");
        }
        if (!staffCode.equalsIgnoreCase(str(staff.getStaffCode()))
                && staffRepository.existsByStaffCodeIgnoreCaseAndUserIdNot(staffCode, staff.getUserId())) {
            throw new IllegalArgumentException("Mã nhân viên đã tồn tại.");
        }
        staff.setStaffCode(staffCode);
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
        m.put("defaultPosition", s.getDefaultPosition());
        return m;
    }

    /** Chuẩn hóa vị trí mặc định: chỉ nhận mã hợp lệ, rỗng -> null (Chưa gán). */
    private String normalizePosition(Object value) {
        String p = str(value).trim().toUpperCase();
        if (p.isBlank()) return null;
        if (!ALLOWED_POSITIONS.contains(p)) {
            throw new IllegalArgumentException("Vị trí mặc định không hợp lệ.");
        }
        return p;
    }

    private static final java.util.Set<String> ALLOWED_POSITIONS = java.util.Set.of(
            com.devcine.backend.enums.WorkPosition.POS_TICKETING,
            com.devcine.backend.enums.WorkPosition.FNB,
            com.devcine.backend.enums.WorkPosition.CHECK_IN,
            com.devcine.backend.enums.WorkPosition.SHIFT_LEAD);

    /**
     * Danh sách nhân viên + bộ lọc theo cơ sở / trạng thái / từ khoá.
     * Lọc trong Java (dữ liệu nhỏ) để tránh gotcha lower(null) của Postgres khi truyền param null vào JPQL.
     */
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
        List<Staff> staffList = staffRepository.findAllWithDetails().stream()
                .filter(s -> s.getCinema() != null && s.getCinema().getId().equals(cinemaId))
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        List<StaffSchedule> todaySchedules = staffScheduleRepository.findByWorkDateWithDetails(today, cinemaId, "APPROVED");

        List<Map<String, Object>> result = staffList.stream().map(staff -> {
            StaffSchedule schedule = todaySchedules.stream()
                    .filter(ss -> ss.getStaff().getUserId().equals(staff.getUserId()))
                    .findFirst()
                    .orElse(null);

            User u = staff.getUser();
            Map<String, Object> m = new HashMap<>();
            m.put("id", staff.getUserId());
            m.put("name", u != null ? u.getFullName() : "Nhân viên");

            String workPosition = schedule != null && schedule.getWorkPosition() != null ? schedule.getWorkPosition() : "Unassigned";
            String uiRole = "Box Office"; // default fallback for UI
            String wpLower = workPosition.toLowerCase();
            if (wpLower.contains("f&b") || wpLower.contains("bắp") || wpLower.contains("nước")) {
                uiRole = "F&B";
            } else if (wpLower.contains("usher") || wpLower.contains("soát") || wpLower.contains("kiểm")) {
                uiRole = "Usher";
            } else if (workPosition.equals("Unassigned")) {
                uiRole = "Unassigned";
            }

            m.put("role", uiRole);
            
            // Format shift name based on start/end time
            String shiftName = "-";
            if (schedule != null && schedule.getShift() != null) {
                shiftName = schedule.getShift().getStartTime().toLocalTime().toString() + " - " + schedule.getShift().getEndTime().toLocalTime().toString();
            }
            m.put("shift", shiftName);
            
            // Determine status based on current time
            String status = "Off Duty";
            if (schedule != null && schedule.getShift() != null) {
                java.time.LocalTime now = java.time.LocalTime.now();
                if (!now.isBefore(schedule.getShift().getStartTime().toLocalTime()) && !now.isAfter(schedule.getShift().getEndTime().toLocalTime())) {
                    status = "On Duty";
                }
            }
            m.put("status", status);
            m.put("sales", schedule != null ? bookingRepository.countTicketsByStaffSchedule(schedule.getId()) : 0);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping
    @PreAuthorize("@perm.can('staff_management','add')")
    @Transactional
    public ResponseEntity<?> createStaff(@RequestBody Map<String, Object> body) {
        try {
            String username = str(body.get("username"));
            String email = str(body.get("email"));
            String fullName = str(body.get("fullName"));

            // Validate chi tiết (đồng bộ với realtime validate ở FE)
            validateFullName(fullName);
            validateUsernameFormat(username);
            validateEmailFormat(email);
            validatePhoneFormat(body.get("phone"));
            if (userRepository.existsByUsername(username))
                throw new IllegalArgumentException("Tài khoản đăng nhập đã tồn tại.");
            if (userRepository.existsByEmail(email))
                throw new IllegalArgumentException("Email đã được sử dụng.");

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

            // Vị trí mặc định BẮT BUỘC khi là STAFF; MANAGER/ADMIN bỏ qua (null)
            String defaultPosition = null;
            if ("STAFF".equals(roleName)) {
                defaultPosition = normalizePosition(body.get("defaultPosition"));
                if (defaultPosition == null) {
                    throw new IllegalArgumentException("Vui lòng chọn vị trí cho nhân viên.");
                }
            }

            // Luôn dùng MẬT KHẨU MẶC ĐỊNH + buộc đổi ở lần đăng nhập đầu tiên
            User u = User.builder()
                    .username(username)
                    .email(email)
                    .fullName(fullName)
                    .phone(str(body.get("phone")).isBlank() ? null : str(body.get("phone")))
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
                    .staffCode(resolveStaffCodeForCreate(body.get("staffCode")))
                    .cinema(staffCinema)
                    .defaultPosition(defaultPosition)
                    .build();
            staffRepository.save(staff); // @MapsId: entity mới (userId null) -> persist

            // Gửi email cấp tài khoản (best-effort) — KHÔNG chặn tạo nếu SMTP lỗi
            boolean emailSent = mailService.sendStaffCredentials(email, fullName, username, defaultStaffPassword);

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
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tạo được nhân viên: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('staff_management','edit')")
    @Transactional
    public ResponseEntity<?> updateStaff(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            Staff staff = staffRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));
            User u = staff.getUser();
            if (u == null) throw new IllegalArgumentException("Nhân viên không hợp lệ.");

            if (body.containsKey("fullName") && !str(body.get("fullName")).isBlank()) {
                validateFullName(str(body.get("fullName")));
                u.setFullName(str(body.get("fullName")));
            }
            if (body.containsKey("phone")) {
                validatePhoneFormat(body.get("phone"));
                u.setPhone(str(body.get("phone")).isBlank() ? null : str(body.get("phone")));
            }
            if (body.containsKey("email")) {
                String email = str(body.get("email"));
                if (!email.isBlank() && !email.equalsIgnoreCase(u.getEmail())) {
                    validateEmailFormat(email);
                    if (userRepository.existsByEmail(email))
                        throw new IllegalArgumentException("Email đã được sử dụng.");
                    u.setEmail(email);
                }
            }
            if (body.containsKey("isActive"))
                u.setIsActive(Boolean.TRUE.equals(body.get("isActive")));
            // Chỉ ADMIN được đổi vai trò, và chỉ giữa STAFF <-> MANAGER
            if (body.containsKey("role") && com.devcine.backend.util.SecurityUtils.isAdmin()) {
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
            updateStaffCode(staff, body.get("staffCode"));
            if (body.containsKey("defaultPosition")) {
                staff.setDefaultPosition(normalizePosition(body.get("defaultPosition")));
            }
            staff.setUpdatedAt(LocalDateTime.now());

            if (body.containsKey("cinemaId")) {
                Object finalCinemaId = body.get("cinemaId");
                if (!com.devcine.backend.util.SecurityUtils.isAdmin()) {
                    Integer myCinemaId = com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId();
                    if (myCinemaId == null) throw new IllegalArgumentException("Bạn chưa được gán cơ sở.");
                    if (staff.getCinema() == null || !staff.getCinema().getId().equals(myCinemaId)) {
                        throw new IllegalArgumentException("Bạn chỉ có thể sửa nhân viên của cơ sở mình.");
                    }
                    if (finalCinemaId != null && !str(finalCinemaId).isBlank() && !str(finalCinemaId).equals(myCinemaId.toString())) {
                        throw new IllegalArgumentException("Bạn chỉ có thể gán nhân viên vào cơ sở của mình.");
                    }
                    finalCinemaId = myCinemaId;
                }
                staff.setCinema(resolveCinema(finalCinemaId));
            } else if (!com.devcine.backend.util.SecurityUtils.isAdmin()) {
                Integer myCinemaId = com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId();
                if (staff.getCinema() == null || !staff.getCinema().getId().equals(myCinemaId)) {
                    throw new IllegalArgumentException("Bạn chỉ có thể sửa nhân viên của cơ sở mình.");
                }
            }
            if ("MANAGER".equalsIgnoreCase(u.getRole() != null ? u.getRole().getName() : "") && staff.getCinema() == null) {
                throw new IllegalArgumentException("Quản lý phải được gán một cơ sở.");
            }
            staffRepository.save(staff);

            return ResponseEntity.ok(ApiResponse.success("Đã cập nhật nhân viên."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không cập nhật được nhân viên: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("@perm.can('staff_management','edit')")
    @Transactional
    public ResponseEntity<?> toggleStaff(@PathVariable Integer id) {
        Staff staff = staffRepository.findById(id).orElse(null);
        if (staff == null || staff.getUser() == null)
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy nhân viên."));

        if (!com.devcine.backend.util.SecurityUtils.isAdmin()) {
            Integer myCinemaId = com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId();
            if (staff.getCinema() == null || !staff.getCinema().getId().equals(myCinemaId)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Bạn chỉ có thể đổi trạng thái nhân viên của cơ sở mình."));
            }
        }

        User u = staff.getUser();
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

    @GetMapping("/shifts")
    @PreAuthorize("@perm.can('staff_management','view')")
    public ResponseEntity<?> getShifts(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer cinemaId,
            @RequestParam(required = false) String status) {
        LocalDate workDate = date != null && !date.isBlank() ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(staffScheduleService.getShifts(workDate, cinemaId, status)));
    }

    @GetMapping("/shifts/current")
    public ResponseEntity<?> getCurrentShift() {
        return ResponseEntity.ok(ApiResponse.ok(staffScheduleService.getCurrentShift().orElse(null)));
    }

    @GetMapping("/shifts/my")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<?> getMyShifts(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        LocalDate fromDate = from != null && !from.isBlank() ? LocalDate.parse(from) : null;
        LocalDate toDate = to != null && !to.isBlank() ? LocalDate.parse(to) : null;
        return ResponseEntity.ok(ApiResponse.ok(staffScheduleService.getMyShifts(fromDate, toDate)));
    }

    @PostMapping("/shifts")
    @PreAuthorize("@perm.can('staff_management','add')")
    public ResponseEntity<?> assignShift(@Valid @RequestBody StaffShiftRequest request) {
        return ResponseEntity.status(201).body(staffScheduleService.assignShift(request));
    }

    @PutMapping("/shifts/{id}/approve")
    @PreAuthorize("@perm.can('staff_management','edit')")
    public ResponseEntity<?> approveShift(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(staffScheduleService.approveShift(id)));
    }

    @PutMapping("/shifts/{id}/reject")
    @PreAuthorize("@perm.can('staff_management','edit')")
    public ResponseEntity<?> rejectShift(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(staffScheduleService.rejectShift(id)));
    }

    // Vào ca / Kết ca (nhân viên tự thao tác trên ca của chính mình để bật/tắt quyền theo Position)
    @PutMapping("/shifts/{id}/check-in")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<?> checkInShift(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(staffScheduleService.checkIn(id)));
    }

    @PutMapping("/shifts/{id}/check-out")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<?> checkOutShift(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(staffScheduleService.checkOut(id)));
    }

    // ===== Bàn giao ca (Shift Handover) =====

    @GetMapping("/handovers")
    @PreAuthorize("@perm.can('staff_management','view')")
    public ResponseEntity<?> getShiftHandovers() {
        return ResponseEntity.ok(ApiResponse.ok(shiftHandoverService.list()));
    }

    @GetMapping("/handovers/my")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<?> getMyShiftHandovers() {
        return ResponseEntity.ok(ApiResponse.ok(shiftHandoverService.myList()));
    }

    @GetMapping("/shifts/current/handover-summary")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','MANAGER')")
    public ResponseEntity<?> getCurrentHandoverSummary() {
        return ResponseEntity.ok(ApiResponse.ok(shiftHandoverService.currentSummary()));
    }

    @GetMapping("/handovers/summary")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','MANAGER')")
    public ResponseEntity<?> getHandoverSummary(@RequestParam Integer staffScheduleId) {
        return ResponseEntity.ok(ApiResponse.ok(shiftHandoverService.summary(staffScheduleId)));
    }

    @PostMapping("/handovers")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','MANAGER')")
    public ResponseEntity<?> submitShiftHandover(@Valid @RequestBody ShiftHandoverRequest request) {
        return ResponseEntity.status(201).body(shiftHandoverService.submit(request));
    }

    @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class})
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}
