package com.devcine.backend.service;

import com.devcine.backend.dto.request.StaffShiftRequest;
import com.devcine.backend.dto.response.StaffShiftResponse;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Shift;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.entity.StaffSchedule;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.ShiftRepository;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.repository.StaffScheduleRepository;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaffScheduleService {

    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final StaffRepository staffRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final ShiftRepository shiftRepository;
    private final CinemaRepository cinemaRepository;

    @Transactional(readOnly = true)
    public List<StaffShiftResponse> getShifts(LocalDate workDate, Integer cinemaId, String status) {
        Integer effectiveCinemaId = resolveReadableCinemaId(cinemaId);
        String normalizedStatus = normalizeStatus(status);
        return staffScheduleRepository.findByWorkDateWithDetails(workDate, effectiveCinemaId, normalizedStatus)
                .stream()
                .map(StaffShiftResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffShiftResponse> getMyShifts(LocalDate fromDate, LocalDate toDate) {
        if (!SecurityUtils.hasRole("STAFF") || SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("Chỉ nhân viên mới có thể xem ca làm việc của mình.");
        }
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new AccessDeniedException("Bạn cần đăng nhập để xem ca làm việc.");
        }

        LocalDate today = LocalDate.now();
        LocalDate effectiveFrom = fromDate != null ? fromDate : today.minusDays(7);
        LocalDate effectiveTo = toDate != null ? toDate : today.plusDays(30);
        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Khoảng ngày xem ca không hợp lệ.");
        }

        return staffScheduleRepository.findMyShiftsWithDetails(currentUserId, effectiveFrom, effectiveTo)
                .stream()
                .map(StaffShiftResponse::fromEntity)
                .toList();
    }

    @Transactional
    public StaffShiftResponse assignShift(StaffShiftRequest request) {
        Staff staff = staffRepository.findByIdWithDetails(request.getStaffId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên"));
        if (staff.getUser() == null || !Boolean.TRUE.equals(staff.getUser().getIsActive())) {
            throw new IllegalArgumentException("Nhân viên đang bị khoá hoặc không hợp lệ");
        }

        Cinema cinema = resolveAssignableCinema(request.getCinemaId());
        if (staff.getCinema() == null || !staff.getCinema().getId().equals(cinema.getId())) {
            throw new IllegalArgumentException("Nhân viên không thuộc cơ sở đã chọn");
        }

        LocalDateTime startAt = request.getWorkDate().atTime(request.getStartTime());
        LocalDateTime endAt = request.getWorkDate().atTime(request.getEndTime());
        if (!endAt.isAfter(startAt)) {
            endAt = endAt.plusDays(1);
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        boolean overlapped = staffScheduleRepository.existsOverlappingSchedule(
                staff.getUserId(),
                request.getWorkDate(),
                startAt,
                endAt);
        if (overlapped) {
            throw new IllegalArgumentException("Nhân viên đã có ca làm việc trùng thời gian");
        }

        Shift shift = Shift.builder()
                .startTime(startAt)
                .endTime(endAt)
                .build();
        shiftRepository.save(shift);

        StaffSchedule schedule = StaffSchedule.builder()
                .staff(staff)
                .shift(shift)
                .cinema(cinema)
                .workDate(request.getWorkDate())
                .workPosition(clean(request.getWorkPosition()))
                .location(cleanNullable(request.getLocation()))
                .note(cleanNullable(request.getNote()))
                .status(STATUS_SCHEDULED)
                .build();
        staffScheduleRepository.save(schedule);
        return StaffShiftResponse.fromEntity(schedule);
    }

    @Transactional
    public StaffShiftResponse approveShift(Integer id) {
        StaffSchedule schedule = getOwnedSchedule(id);
        schedule.setStatus(STATUS_APPROVED);
        return StaffShiftResponse.fromEntity(staffScheduleRepository.save(schedule));
    }

    @Transactional
    public StaffShiftResponse rejectShift(Integer id) {
        StaffSchedule schedule = getOwnedSchedule(id);
        schedule.setStatus(STATUS_REJECTED);
        return StaffShiftResponse.fromEntity(staffScheduleRepository.save(schedule));
    }

    /**
     * Vào ca (Check-in): nhân viên tự kích hoạt ca ĐÃ DUYỆT của mình để mở quyền theo Position.
     * APPROVED → IN_PROGRESS. Chỉ nhân viên (STAFF) và chỉ ca của chính mình.
     */
    @Transactional
    public StaffShiftResponse checkIn(Integer id) {
        if (!SecurityUtils.hasRole("STAFF") || SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("Chỉ nhân viên mới có thể vào ca.");
        }
        StaffSchedule schedule = requireOwnSchedule(id);
        if (!STATUS_APPROVED.equalsIgnoreCase(schedule.getStatus())) {
            throw new IllegalArgumentException("Chỉ ca đã được duyệt mới có thể vào ca.");
        }
        Shift shift = schedule.getShift();
        if (shift == null || shift.getEndTime() == null) {
            throw new IllegalArgumentException("Ca làm việc chưa có giờ hợp lệ.");
        }
        if (LocalDateTime.now().isAfter(shift.getEndTime())) {
            throw new IllegalArgumentException("Ca làm việc đã kết thúc, không thể vào ca.");
        }
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (!staffScheduleRepository.findActiveSchedulesForUser(currentUserId).isEmpty()) {
            throw new IllegalArgumentException("Bạn đang trong một ca làm việc khác. Hãy kết ca trước khi vào ca mới.");
        }
        schedule.setStatus(STATUS_IN_PROGRESS);
        schedule.setActualCheckInAt(LocalDateTime.now()); // ghi mốc giờ vào ca thực tế
        return StaffShiftResponse.fromEntity(staffScheduleRepository.save(schedule));
    }

    /**
     * Kết ca (Check-out): đóng ca đang chạy, thu hồi quyền theo Position.
     * IN_PROGRESS → COMPLETED. Chỉ ca của chính nhân viên.
     */
    @Transactional
    public StaffShiftResponse checkOut(Integer id) {
        if (!SecurityUtils.hasRole("STAFF") || SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("Chỉ nhân viên mới có thể kết ca.");
        }
        StaffSchedule schedule = requireOwnSchedule(id);
        if (!STATUS_IN_PROGRESS.equalsIgnoreCase(schedule.getStatus())) {
            throw new IllegalArgumentException("Chỉ ca đang làm việc mới có thể kết ca.");
        }
        schedule.setStatus(STATUS_COMPLETED);
        schedule.setActualCheckOutAt(LocalDateTime.now()); // ghi mốc giờ kết ca thực tế
        return StaffShiftResponse.fromEntity(staffScheduleRepository.save(schedule));
    }

    @Transactional(readOnly = true)
    public Optional<StaffShiftResponse> getCurrentShift() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Optional.empty();
        }
        return staffScheduleRepository.findActiveSchedulesForUser(currentUserId).stream()
                .findFirst()
                .map(StaffShiftResponse::fromEntity);
    }

    private StaffSchedule getOwnedSchedule(Integer id) {
        StaffSchedule schedule = staffScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch làm việc"));
        Integer staffCinemaId = SecurityUtils.getCurrentUserCinemaId();
        if (!SecurityUtils.isAdmin() && staffCinemaId != null) {
            Integer scheduleCinemaId = schedule.getCinema() != null ? schedule.getCinema().getId() : null;
            if (!staffCinemaId.equals(scheduleCinemaId)) {
                throw new IllegalArgumentException("Bạn chỉ có thể xử lý ca của cơ sở mình");
            }
        }
        return schedule;
    }

    /** Nạp ca và bắt buộc ca thuộc về chính nhân viên đang đăng nhập (dùng cho Check-in/Check-out). */
    private StaffSchedule requireOwnSchedule(Integer id) {
        StaffSchedule schedule = staffScheduleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch làm việc"));
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        Integer scheduleStaffId = schedule.getStaff() != null ? schedule.getStaff().getUserId() : null;
        if (currentUserId == null || !currentUserId.equals(scheduleStaffId)) {
            throw new AccessDeniedException("Bạn chỉ có thể thao tác trên ca của chính mình.");
        }
        return schedule;
    }

    private Integer resolveReadableCinemaId(Integer requestedCinemaId) {
        // Non-admin (STAFF + MANAGER) bị ép về cơ sở của mình; ADMIN xem theo bộ lọc yêu cầu
        if (!SecurityUtils.isAdmin()) {
            Integer staffCinemaId = SecurityUtils.getCurrentUserCinemaId();
            if (staffCinemaId == null) {
                throw new IllegalArgumentException("Bạn chưa được gán cơ sở");
            }
            if (requestedCinemaId != null && !staffCinemaId.equals(requestedCinemaId)) {
                throw new IllegalArgumentException("Bạn chỉ có thể xem ca của cơ sở mình");
            }
            return staffCinemaId;
        }
        return requestedCinemaId;
    }

    private Cinema resolveAssignableCinema(Integer cinemaId) {
        Integer effectiveCinemaId = resolveReadableCinemaId(cinemaId);
        if (effectiveCinemaId == null) {
            throw new IllegalArgumentException("Vui lòng chọn cơ sở");
        }
        return cinemaRepository.findById(effectiveCinemaId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cơ sở"));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return null;
        }
        String value = status.trim().toUpperCase();
        if (!List.of(STATUS_SCHEDULED, STATUS_APPROVED, STATUS_IN_PROGRESS, STATUS_COMPLETED, STATUS_REJECTED).contains(value)) {
            throw new IllegalArgumentException("Trạng thái ca không hợp lệ");
        }
        return value;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanNullable(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned;
    }
}
