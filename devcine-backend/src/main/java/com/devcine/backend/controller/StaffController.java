package com.devcine.backend.controller;

import com.devcine.backend.dto.request.ShiftHandoverDecisionRequest;
import com.devcine.backend.dto.request.ShiftHandoverRequest;
import com.devcine.backend.dto.request.StaffShiftRequest;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Role;
import com.devcine.backend.entity.Shift;
import com.devcine.backend.entity.ShiftHandover;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.entity.StaffSchedule;
import com.devcine.backend.entity.User;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.RoleRepository;
import com.devcine.backend.repository.ShiftHandoverRepository;
import com.devcine.backend.repository.ShiftRepository;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
@CrossOrigin(origins = "*")
public class StaffController {

    private final StaffRepository staffRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final ShiftRepository shiftRepository;
    private final ShiftHandoverRepository shiftHandoverRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CinemaRepository cinemaRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final StaffScheduleService staffScheduleService;
    private final ShiftHandoverService shiftHandoverService;

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
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
        return ResponseEntity.ok(result);
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

        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("@perm.can('staff_management','add')")
    @Transactional
    public ResponseEntity<?> createStaff(@RequestBody Map<String, Object> body) {
        try {
            String username = str(body.get("username"));
            String email = str(body.get("email"));
            String fullName = str(body.get("fullName"));
            String password = str(body.get("password"));
            if (username.isBlank() || email.isBlank() || fullName.isBlank() || password.isBlank())
                throw new IllegalArgumentException("Vui lòng nhập đầy đủ họ tên, tài khoản, email và mật khẩu.");
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

            User u = User.builder()
                    .username(username)
                    .email(email)
                    .fullName(fullName)
                    .phone(str(body.get("phone")).isBlank() ? null : str(body.get("phone")))
                    .passwordHash(passwordEncoder.encode(password))
                    .role(role)
                    .isActive(true)
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
            if ("MANAGER".equals(roleName) && staffCinema == null) {
                throw new IllegalArgumentException("Quản lý phải được gán một cơ sở.");
            }
            Staff staff = Staff.builder()
                    .user(u)
                    .staffCode(resolveStaffCodeForCreate(body.get("staffCode")))
                    .cinema(staffCinema)
                    .defaultPosition(normalizePosition(body.get("defaultPosition")))
                    .build();
            staffRepository.save(staff); // @MapsId: entity mới (userId null) -> persist

            return ResponseEntity.status(201).body(Map.of("success", true, "userId", u.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
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

            if (body.containsKey("fullName") && !str(body.get("fullName")).isBlank())
                u.setFullName(str(body.get("fullName")));
            if (body.containsKey("phone"))
                u.setPhone(str(body.get("phone")).isBlank() ? null : str(body.get("phone")));
            if (body.containsKey("email")) {
                String email = str(body.get("email"));
                if (!email.isBlank() && !email.equalsIgnoreCase(u.getEmail())) {
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

            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
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
        return ResponseEntity.ok(Map.of("success", true, "isActive", u.getIsActive()));
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
        return ResponseEntity.ok(staffScheduleService.getShifts(workDate, cinemaId, status));
    }

    @GetMapping("/shifts/current")
    public ResponseEntity<?> getCurrentShift() {
        return ResponseEntity.ok(staffScheduleService.getCurrentShift().orElse(null));
    }

    @GetMapping("/shifts/my")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<?> getMyShifts(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        LocalDate fromDate = from != null && !from.isBlank() ? LocalDate.parse(from) : null;
        LocalDate toDate = to != null && !to.isBlank() ? LocalDate.parse(to) : null;
        return ResponseEntity.ok(staffScheduleService.getMyShifts(fromDate, toDate));
    }

    @PostMapping("/shifts")
    @PreAuthorize("@perm.can('staff_management','add')")
    public ResponseEntity<?> assignShift(@Valid @RequestBody StaffShiftRequest request) {
        return ResponseEntity.status(201).body(staffScheduleService.assignShift(request));
    }

    @PutMapping("/shifts/{id}/approve")
    @PreAuthorize("@perm.can('staff_management','edit')")
    public ResponseEntity<?> approveShift(@PathVariable Integer id) {
        return ResponseEntity.ok(staffScheduleService.approveShift(id));
    }

    @PutMapping("/shifts/{id}/reject")
    @PreAuthorize("@perm.can('staff_management','edit')")
    public ResponseEntity<?> rejectShift(@PathVariable Integer id) {
        return ResponseEntity.ok(staffScheduleService.rejectShift(id));
    }

    @GetMapping("/shifts/all")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllShiftTemplates() {
        List<Shift> shifts = shiftRepository.findAll();
        return ResponseEntity.ok(shifts);
    }

    @PostMapping("/shifts/template")
    @PreAuthorize("@perm.can('staff_management','add')")
    @Transactional
    public ResponseEntity<?> createShiftTemplate(@RequestBody Map<String, Object> body) {
        try {
            Shift shift = Shift.builder()
                    .startTime(LocalDateTime.parse((String) body.get("startTime")))
                    .endTime(LocalDateTime.parse((String) body.get("endTime")))
                    .status("ACTIVE")
                    .build();
            shiftRepository.save(shift);
            return ResponseEntity.status(201).body(Map.of("success", true, "data", shift));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ===== Bàn giao ca (Shift Handover) =====

    @GetMapping("/handovers/legacy")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getHandovers() {
        List<Map<String, Object>> result = shiftHandoverRepository.findAllWithDetails().stream().map(h -> Map.<String, Object>of(
                "id", h.getId(),
                "staffScheduleId", h.getStaffSchedule().getId(),
                "staffName", h.getStaffSchedule().getStaff() != null && h.getStaffSchedule().getStaff().getUser() != null
                        ? h.getStaffSchedule().getStaff().getUser().getFullName() : "Nhân viên",
                "workDate", h.getStaffSchedule().getWorkDate() != null ? h.getStaffSchedule().getWorkDate().toString() : "",
                "declaredCash", h.getDeclaredCash() != null ? h.getDeclaredCash() : BigDecimal.ZERO,
                "systemCash", h.getSystemCash() != null ? h.getSystemCash() : BigDecimal.ZERO,
                "difference", h.getDifference() != null ? h.getDifference() : BigDecimal.ZERO,
                "status", h.getStatus() != null ? h.getStatus() : "SUBMITTED"
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/handovers/legacy")
    @PreAuthorize("@perm.can('staff_management','edit')")
    @Transactional
    public ResponseEntity<?> createHandover(@RequestBody Map<String, Object> body) {
        try {
            Integer staffScheduleId = Integer.parseInt(body.get("staffScheduleId").toString());
            StaffSchedule schedule = staffScheduleRepository.findById(staffScheduleId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch làm việc"));

            BigDecimal declared = new BigDecimal(body.getOrDefault("declaredCash", "0").toString());
            BigDecimal system = new BigDecimal(body.getOrDefault("systemCash", "0").toString());

            ShiftHandover handover = ShiftHandover.builder()
                    .staffSchedule(schedule)
                    .declaredCash(declared)
                    .systemCash(system)
                    .difference(declared.subtract(system))
                    .status("SUBMITTED")
                    .build();
            shiftHandoverRepository.save(handover);
            return ResponseEntity.status(201).body(Map.of("success", true, "id", handover.getId(),
                    "difference", handover.getDifference()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/handovers")
    @PreAuthorize("@perm.can('staff_management','view')")
    public ResponseEntity<?> getShiftHandovers() {
        return ResponseEntity.ok(shiftHandoverService.list());
    }

    @GetMapping("/handovers/my")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<?> getMyShiftHandovers() {
        return ResponseEntity.ok(shiftHandoverService.myList());
    }

    @GetMapping("/shifts/current/handover-summary")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','MANAGER')")
    public ResponseEntity<?> getCurrentHandoverSummary() {
        return ResponseEntity.ok(shiftHandoverService.currentSummary());
    }

    @GetMapping("/handovers/summary")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','MANAGER')")
    public ResponseEntity<?> getHandoverSummary(@RequestParam Integer staffScheduleId) {
        return ResponseEntity.ok(shiftHandoverService.summary(staffScheduleId));
    }

    @GetMapping("/handovers/receivers")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','MANAGER')")
    public ResponseEntity<?> getHandoverReceivers(@RequestParam Integer staffScheduleId) {
        return ResponseEntity.ok(shiftHandoverService.receiverCandidates(staffScheduleId));
    }

    @PostMapping("/handovers")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','MANAGER')")
    public ResponseEntity<?> submitShiftHandover(@Valid @RequestBody ShiftHandoverRequest request) {
        return ResponseEntity.status(201).body(shiftHandoverService.submit(request));
    }

    @PutMapping("/handovers/{id}/receive")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','MANAGER')")
    public ResponseEntity<?> receiveShiftHandover(@PathVariable Integer id,
                                                  @RequestBody(required = false) ShiftHandoverDecisionRequest request) {
        return ResponseEntity.ok(shiftHandoverService.receive(id, request));
    }

    @PutMapping("/handovers/{id}/confirm")
    @PreAuthorize("@perm.can('staff_management','edit')")
    public ResponseEntity<?> confirmShiftHandover(@PathVariable Integer id,
                                                  @RequestBody(required = false) ShiftHandoverDecisionRequest request) {
        return ResponseEntity.ok(shiftHandoverService.confirm(id, request));
    }

    @PutMapping("/handovers/{id}/reject")
    @PreAuthorize("@perm.can('staff_management','edit')")
    public ResponseEntity<?> rejectShiftHandover(@PathVariable Integer id,
                                                 @RequestBody(required = false) ShiftHandoverDecisionRequest request) {
        return ResponseEntity.ok(shiftHandoverService.reject(id, request));
    }

    @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", message));
    }
}
