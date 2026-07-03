package com.devcine.backend.service;

import com.devcine.backend.dto.request.ShiftHandoverDecisionRequest;
import com.devcine.backend.dto.request.ShiftHandoverRequest;
import com.devcine.backend.dto.response.ShiftHandoverResponse;
import com.devcine.backend.dto.response.ShiftHandoverSummaryResponse;
import com.devcine.backend.entity.ShiftHandover;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.entity.StaffSchedule;
import com.devcine.backend.repository.BookingFnbRepository;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.ConcessionSaleRepository;
import com.devcine.backend.repository.ShiftHandoverRepository;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.repository.StaffScheduleRepository;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftHandoverService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final ShiftAccessService shiftAccessService;
    private final StaffScheduleRepository staffScheduleRepository;
    private final ShiftHandoverRepository shiftHandoverRepository;
    private final BookingRepository bookingRepository;
    private final BookingFnbRepository bookingFnbRepository;
    private final ConcessionSaleRepository concessionSaleRepository;
    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public ShiftHandoverSummaryResponse currentSummary() {
        StaffSchedule schedule = shiftAccessService.requireCurrentStaffSchedule();
        return buildSummary(schedule);
    }

    @Transactional(readOnly = true)
    public ShiftHandoverSummaryResponse summary(Integer staffScheduleId) {
        StaffSchedule schedule = loadSchedule(staffScheduleId);
        verifyScheduleAccess(schedule);
        return buildSummary(schedule);
    }

    @Transactional(readOnly = true)
    public List<ShiftHandoverResponse> list() {
        return shiftHandoverRepository.findAllWithDetails().stream()
                .map(ShiftHandoverResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ShiftHandoverResponse submit(ShiftHandoverRequest request) {
        StaffSchedule schedule = request.getStaffScheduleId() != null
                ? loadSchedule(request.getStaffScheduleId())
                : shiftAccessService.requireCurrentStaffSchedule();
        verifyScheduleAccess(schedule);
        if (shiftHandoverRepository.existsByStaffScheduleIdAndStatus(schedule.getId(), STATUS_PENDING)) {
            throw new IllegalArgumentException("Ca này đã có biên bản bàn giao đang chờ xác nhận.");
        }

        ShiftHandoverSummaryResponse summary = buildSummary(schedule);
        BigDecimal declaredCash = money(request.getDeclaredCash());
        BigDecimal systemCash = money(summary.getSystemCash());

        ShiftHandover handover = ShiftHandover.builder()
                .staffSchedule(schedule)
                .declaredCash(declaredCash)
                .systemCash(systemCash)
                .cashSales(money(summary.getCashSales()))
                .cardSales(money(summary.getCardSales()))
                .transferSales(money(summary.getTransferSales()))
                .ticketRevenue(money(summary.getTicketRevenue()))
                .concessionRevenue(money(summary.getConcessionRevenue()))
                .ticketCount(summary.getTicketCount())
                .concessionOrderCount(summary.getConcessionOrderCount())
                .difference(declaredCash.subtract(systemCash))
                .status(STATUS_PENDING)
                .submittedAt(LocalDateTime.now())
                .note(cleanNote(request.getNote()))
                .build();
        return ShiftHandoverResponse.fromEntity(shiftHandoverRepository.save(handover));
    }

    @Transactional
    public ShiftHandoverResponse confirm(Integer id, ShiftHandoverDecisionRequest request) {
        ShiftHandover handover = loadHandover(id);
        handover.setStatus(STATUS_CONFIRMED);
        handover.setConfirmedAt(LocalDateTime.now());
        handover.setApprovedByManager(currentStaffOrNull());
        mergeNote(handover, request);
        return ShiftHandoverResponse.fromEntity(shiftHandoverRepository.save(handover));
    }

    @Transactional
    public ShiftHandoverResponse reject(Integer id, ShiftHandoverDecisionRequest request) {
        ShiftHandover handover = loadHandover(id);
        handover.setStatus(STATUS_REJECTED);
        handover.setConfirmedAt(LocalDateTime.now());
        handover.setApprovedByManager(currentStaffOrNull());
        mergeNote(handover, request);
        return ShiftHandoverResponse.fromEntity(shiftHandoverRepository.save(handover));
    }

    private ShiftHandoverSummaryResponse buildSummary(StaffSchedule schedule) {
        Integer scheduleId = schedule.getId();
        BigDecimal cashSales = sumBookingAndConcession(scheduleId, "CASH");
        BigDecimal cardSales = sumBookingAndConcession(scheduleId, "CARD");
        BigDecimal transferSales = sumBookingAndConcession(scheduleId, "TRANSFER");
        BigDecimal ticketRevenue = money(bookingRepository.sumTicketRevenueByStaffSchedule(scheduleId));
        BigDecimal concessionRevenue = money(bookingFnbRepository.sumFnbRevenueByStaffSchedule(scheduleId))
                .add(money(concessionSaleRepository.sumRevenueByStaffSchedule(scheduleId)));
        long ticketCount = bookingRepository.countTicketsByStaffSchedule(scheduleId);
        long concessionOrderCount = bookingFnbRepository.countFnbOrdersByStaffSchedule(scheduleId)
                + concessionSaleRepository.countConfirmedByStaffSchedule(scheduleId);

        var staff = schedule.getStaff();
        var user = staff != null ? staff.getUser() : null;
        var cinema = schedule.getCinema();
        var shift = schedule.getShift();

        return ShiftHandoverSummaryResponse.builder()
                .staffScheduleId(schedule.getId())
                .staffId(staff != null ? staff.getUserId() : null)
                .staffName(user != null ? user.getFullName() : "Nhan vien")
                .workPosition(schedule.getWorkPosition())
                .cinemaId(cinema != null ? cinema.getId() : null)
                .cinemaName(cinema != null ? cinema.getName() : null)
                .workDate(schedule.getWorkDate())
                .startAt(shift != null ? shift.getStartTime() : null)
                .endAt(shift != null ? shift.getEndTime() : null)
                .cashSales(cashSales)
                .cardSales(cardSales)
                .transferSales(transferSales)
                .systemCash(cashSales)
                .ticketRevenue(ticketRevenue)
                .concessionRevenue(concessionRevenue)
                .ticketCount(ticketCount)
                .concessionOrderCount(concessionOrderCount)
                .build();
    }

    private BigDecimal sumBookingAndConcession(Integer scheduleId, String paymentMethod) {
        return money(bookingRepository.sumConfirmedRevenueByStaffScheduleAndPaymentMethod(scheduleId, paymentMethod))
                .add(money(concessionSaleRepository.sumConfirmedRevenueByStaffScheduleAndPaymentMethod(scheduleId, paymentMethod)));
    }

    private StaffSchedule loadSchedule(Integer id) {
        return staffScheduleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca làm việc."));
    }

    private ShiftHandover loadHandover(Integer id) {
        return shiftHandoverRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biên bản bàn giao ca."));
    }

    private void verifyScheduleAccess(StaffSchedule schedule) {
        if (!SecurityUtils.hasRole("STAFF") || SecurityUtils.isAdmin()) {
            return;
        }
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        Integer scheduleStaffId = schedule.getStaff() != null ? schedule.getStaff().getUserId() : null;
        if (scheduleStaffId == null || !scheduleStaffId.equals(currentUserId)) {
            throw new AccessDeniedException("Bạn chỉ được bàn giao ca của chính mình.");
        }
    }

    private Staff currentStaffOrNull() {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null || !staffRepository.existsById(userId)) {
            return null;
        }
        return staffRepository.getReferenceById(userId);
    }

    private void mergeNote(ShiftHandover handover, ShiftHandoverDecisionRequest request) {
        String note = request != null ? cleanNote(request.getNote()) : null;
        if (note != null) {
            String current = handover.getNote();
            handover.setNote(current == null || current.isBlank() ? note : current + "\n" + note);
        }
    }

    private String cleanNote(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private BigDecimal money(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
