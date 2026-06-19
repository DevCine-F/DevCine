package com.devcine.backend.controller;

import com.devcine.backend.entity.Shift;
import com.devcine.backend.entity.ShiftHandover;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.entity.StaffSchedule;
import com.devcine.backend.repository.ShiftHandoverRepository;
import com.devcine.backend.repository.ShiftRepository;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.repository.StaffScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @GetMapping("/list")
    public ResponseEntity<?> getAllStaff() {
        List<Staff> staffList = staffRepository.findAll();
        List<Map<String, Object>> result = staffList.stream().map(s -> Map.<String, Object>of(
                "userId", s.getUserId(),
                "staffCode", s.getStaffCode() != null ? s.getStaffCode() : "",
                "fullName", s.getUser() != null ? s.getUser().getFullName() : "Nhân viên",
                "email", s.getUser() != null ? s.getUser().getEmail() : "",
                "phone", s.getUser() != null && s.getUser().getPhone() != null ? s.getUser().getPhone() : ""
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
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
