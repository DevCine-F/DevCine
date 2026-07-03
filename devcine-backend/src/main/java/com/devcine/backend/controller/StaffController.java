package com.devcine.backend.controller;

import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Role;
import com.devcine.backend.entity.Shift;
import com.devcine.backend.entity.ShiftHandover;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.entity.StaffSchedule;
import com.devcine.backend.entity.User;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.RoleRepository;
import com.devcine.backend.repository.ShiftHandoverRepository;
import com.devcine.backend.repository.ShiftRepository;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.repository.StaffScheduleRepository;
import com.devcine.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final PasswordEncoder passwordEncoder;

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
        m.put("joinDate", u != null && u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        m.put("cinemaId", s.getCinema() != null ? s.getCinema().getId() : null);
        m.put("cinemaName", s.getCinema() != null ? s.getCinema().getName() : null);
        return m;
    }

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
        if (com.devcine.backend.util.SecurityUtils.hasRole("STAFF")) {
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

            Role role = roleRepository.findByName("STAFF")
                    .orElseThrow(() -> new IllegalArgumentException("Hệ thống chưa cấu hình vai trò STAFF."));

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
            if (com.devcine.backend.util.SecurityUtils.hasRole("STAFF")) {
                Integer myCinemaId = com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId();
                if (myCinemaId == null) throw new IllegalArgumentException("Bạn chưa được gán cơ sở, không thể tạo nhân viên.");
                if (finalCinemaId != null && !str(finalCinemaId).isBlank() && !str(finalCinemaId).equals(myCinemaId.toString())) {
                    throw new IllegalArgumentException("Bạn chỉ có thể tạo nhân viên cho cơ sở của mình.");
                }
                finalCinemaId = myCinemaId;
            }

            Staff staff = Staff.builder()
                    .user(u)
                    .staffCode(generateStaffCode())
                    .cinema(resolveCinema(finalCinemaId))
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
            userRepository.save(u);

            if (body.containsKey("cinemaId")) {
                Object finalCinemaId = body.get("cinemaId");
                if (com.devcine.backend.util.SecurityUtils.hasRole("STAFF")) {
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
            } else if (com.devcine.backend.util.SecurityUtils.hasRole("STAFF")) {
                Integer myCinemaId = com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId();
                if (staff.getCinema() == null || !staff.getCinema().getId().equals(myCinemaId)) {
                    throw new IllegalArgumentException("Bạn chỉ có thể sửa nhân viên của cơ sở mình.");
                }
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

        if (com.devcine.backend.util.SecurityUtils.hasRole("STAFF")) {
            Integer myCinemaId = com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId();
            if (staff.getCinema() == null || !staff.getCinema().getId().equals(myCinemaId)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Bạn chỉ có thể đổi trạng thái nhân viên của cơ sở mình."));
            }
        }

        User u = staff.getUser();
        u.setIsActive(!Boolean.TRUE.equals(u.getIsActive()));
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("success", true, "isActive", u.getIsActive()));
    }

    private Cinema resolveCinema(Object cinemaId) {
        if (cinemaId == null || str(cinemaId).isBlank()) return null;
        return cinemaRepository.findById(Integer.parseInt(str(cinemaId))).orElse(null);
    }

    @GetMapping("/shifts")
    public ResponseEntity<?> getShifts(@RequestParam(required = false) String date) {
        try {
            LocalDate workDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            List<StaffSchedule> schedules = staffScheduleRepository.findByWorkDateWithDetails(workDate);
            List<Map<String, Object>> result = schedules.stream().map(ss -> Map.<String, Object>of(
                    "id", ss.getId(),
                    "workDate", ss.getWorkDate().toString(),
                    "status", ss.getStatus() != null ? ss.getStatus() : "PENDING",
                    "staffId", ss.getStaff().getUserId(),
                    "staffName", ss.getStaff().getUser() != null ? ss.getStaff().getUser().getFullName() : "Nhân viên",
                    "shiftId", ss.getShift().getId(),
                    "shiftStart", ss.getShift().getStartTime().toString(),
                    "shiftEnd", ss.getShift().getEndTime().toString()
            )).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/shifts")
    @PreAuthorize("@perm.can('staff_management','add')")
    public ResponseEntity<?> assignShift(@RequestBody Map<String, Object> body) {
        try {
            Integer staffId = Integer.parseInt(body.get("staffId").toString());
            Integer shiftId = Integer.parseInt(body.get("shiftId").toString());
            LocalDate workDate = LocalDate.parse((String) body.get("workDate"));

            Staff staff = staffRepository.findById(staffId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
            Shift shift = shiftRepository.findById(shiftId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc"));

            StaffSchedule schedule = StaffSchedule.builder()
                    .staff(staff)
                    .shift(shift)
                    .workDate(workDate)
                    .status("SCHEDULED")
                    .build();
            staffScheduleRepository.save(schedule);
            return ResponseEntity.status(201).body(Map.of("success", true, "id", schedule.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/shifts/{id}/approve")
    @PreAuthorize("@perm.can('staff_management','edit')")
    public ResponseEntity<?> approveShift(@PathVariable Integer id) {
        try {
            StaffSchedule schedule = staffScheduleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch làm việc"));
            schedule.setStatus("APPROVED");
            staffScheduleRepository.save(schedule);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/shifts/{id}/reject")
    @PreAuthorize("@perm.can('staff_management','edit')")
    public ResponseEntity<?> rejectShift(@PathVariable Integer id) {
        try {
            StaffSchedule schedule = staffScheduleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch làm việc"));
            schedule.setStatus("REJECTED");
            staffScheduleRepository.save(schedule);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/shifts/all")
    public ResponseEntity<?> getAllShiftTemplates() {
        List<Shift> shifts = shiftRepository.findAll();
        return ResponseEntity.ok(shifts);
    }

    @PostMapping("/shifts/template")
    @PreAuthorize("@perm.can('staff_management','add')")
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

    @GetMapping("/handovers")
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
                "status", h.getStatus() != null ? h.getStatus() : "PENDING"
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/handovers")
    @PreAuthorize("@perm.can('staff_management','edit')")
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
                    .status("PENDING")
                    .build();
            shiftHandoverRepository.save(handover);
            return ResponseEntity.status(201).body(Map.of("success", true, "id", handover.getId(),
                    "difference", handover.getDifference()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
