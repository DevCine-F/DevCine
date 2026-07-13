package com.devcine.backend.dto.response;

import com.devcine.backend.entity.StaffSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffShiftResponse {

    private Integer id;
    private LocalDate workDate;
    private String status;
    private Integer staffId;
    private String staffCode;
    private String staffName;
    private String staffRole;
    private Boolean staffActive;
    private Integer cinemaId;
    private String cinemaName;
    private String workPosition;
    private String location;
    private String note;
    private Integer shiftId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String startTime;
    private String endTime;
    private LocalDateTime actualCheckInAt;
    private LocalDateTime actualCheckOutAt;

    public static StaffShiftResponse fromEntity(StaffSchedule schedule) {
        var staff = schedule.getStaff();
        var user = staff != null ? staff.getUser() : null;
        var role = user != null ? user.getRole() : null;
        var cinema = schedule.getCinema() != null ? schedule.getCinema() : (staff != null ? staff.getCinema() : null);
        var shift = schedule.getShift();
        String status = schedule.getStatus();
        if (status == null || status.equalsIgnoreCase("PENDING")) {
            status = "SCHEDULED";
        }

        return StaffShiftResponse.builder()
                .id(schedule.getId())
                .workDate(schedule.getWorkDate())
                .status(status)
                .staffId(staff != null ? staff.getUserId() : null)
                .staffCode(staff != null ? staff.getStaffCode() : null)
                .staffName(user != null ? user.getFullName() : "Nhân viên")
                .staffRole(role != null ? role.getName() : "STAFF")
                .staffActive(user != null && Boolean.TRUE.equals(user.getIsActive()))
                .cinemaId(cinema != null ? cinema.getId() : null)
                .cinemaName(cinema != null ? cinema.getName() : null)
                .workPosition(schedule.getWorkPosition())
                .location(schedule.getLocation())
                .note(schedule.getNote())
                .shiftId(shift != null ? shift.getId() : null)
                .startAt(shift != null ? shift.getStartTime() : null)
                .endAt(shift != null ? shift.getEndTime() : null)
                .startTime(shift != null && shift.getStartTime() != null ? shift.getStartTime().toLocalTime().toString() : null)
                .endTime(shift != null && shift.getEndTime() != null ? shift.getEndTime().toLocalTime().toString() : null)
                .actualCheckInAt(schedule.getActualCheckInAt())
                .actualCheckOutAt(schedule.getActualCheckOutAt())
                .build();
    }
}
