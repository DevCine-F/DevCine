package com.devcine.backend.dto.response;

import com.devcine.backend.entity.ShiftHandover;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftHandoverResponse {
    private Integer id;
    private Integer staffScheduleId;
    private Integer staffId;
    private String staffName;
    private String workPosition;
    private Integer cinemaId;
    private String cinemaName;
    private LocalDate workDate;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime actualCheckInAt;
    private LocalDateTime actualCheckOutAt;
    private BigDecimal declaredCash;
    private BigDecimal systemCash;
    private BigDecimal openingFloat;
    private BigDecimal cashSales;
    private BigDecimal cardSales;
    private BigDecimal transferSales;
    private BigDecimal ticketRevenue;
    private BigDecimal concessionRevenue;
    private Long ticketCount;
    private Long concessionOrderCount;
    private BigDecimal difference;
    private String status;
    private LocalDateTime submittedAt;
    private String note;

    public static ShiftHandoverResponse fromEntity(ShiftHandover handover) {
        var schedule = handover.getStaffSchedule();
        var staff = schedule != null ? schedule.getStaff() : null;
        var user = staff != null ? staff.getUser() : null;
        var shift = schedule != null ? schedule.getShift() : null;
        var cinema = schedule != null ? schedule.getCinema() : null;

        return ShiftHandoverResponse.builder()
                .id(handover.getId())
                .staffScheduleId(schedule != null ? schedule.getId() : null)
                .staffId(staff != null ? staff.getUserId() : null)
                .staffName(user != null ? user.getFullName() : "Nhân viên")
                .workPosition(schedule != null ? schedule.getWorkPosition() : null)
                .cinemaId(cinema != null ? cinema.getId() : null)
                .cinemaName(cinema != null ? cinema.getName() : null)
                .workDate(schedule != null ? schedule.getWorkDate() : null)
                .startAt(shift != null ? shift.getStartTime() : null)
                .endAt(shift != null ? shift.getEndTime() : null)
                .actualCheckInAt(schedule != null ? schedule.getActualCheckInAt() : null)
                .actualCheckOutAt(schedule != null ? schedule.getActualCheckOutAt() : null)
                .declaredCash(defaultMoney(handover.getDeclaredCash()))
                .systemCash(defaultMoney(handover.getSystemCash()))
                .openingFloat(defaultMoney(handover.getOpeningFloat()))
                .cashSales(defaultMoney(handover.getCashSales()))
                .cardSales(defaultMoney(handover.getCardSales()))
                .transferSales(defaultMoney(handover.getTransferSales()))
                .ticketRevenue(defaultMoney(handover.getTicketRevenue()))
                .concessionRevenue(defaultMoney(handover.getConcessionRevenue()))
                .ticketCount(handover.getTicketCount() != null ? handover.getTicketCount() : 0L)
                .concessionOrderCount(handover.getConcessionOrderCount() != null ? handover.getConcessionOrderCount() : 0L)
                .difference(defaultMoney(handover.getDifference()))
                .status(normalizeStatus(handover.getStatus()))
                .submittedAt(handover.getSubmittedAt())
                .note(handover.getNote())
                .build();
    }

    private static BigDecimal defaultMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String normalizeStatus(String value) {
        if (value == null || value.equalsIgnoreCase("PENDING")) {
            return "SUBMITTED";
        }
        return value;
    }
}
