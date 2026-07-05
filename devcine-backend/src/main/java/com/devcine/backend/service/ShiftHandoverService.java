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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShiftHandoverService {

    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_RECEIVED = "RECEIVED";
    private static final String STATUS_PENDING_LEGACY = "PENDING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String SCHEDULE_APPROVED = "APPROVED";
    private static final int HANDOVER_EARLY_MINUTES = 30;

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
        var handovers = shiftHandoverRepository.findAllWithDetails().stream();
        // ADMIN xem toàn hệ thống; MANAGER chỉ xem bàn giao của cơ sở mình
        if (!SecurityUtils.isAdmin()) {
            Integer myCinemaId = SecurityUtils.getCurrentUserCinemaId();
            handovers = handovers.filter(h -> {
                StaffSchedule sched = h.getStaffSchedule();
                Integer cinemaId = sched != null && sched.getCinema() != null ? sched.getCinema().getId() : null;
                return myCinemaId != null && myCinemaId.equals(cinemaId);
            });
        }
        return handovers
                .map(ShiftHandoverResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShiftHandoverResponse> myList() {
        if (!SecurityUtils.hasRole("STAFF") || SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("Chi nhan vien moi co the xem bien ban ban giao cua minh.");
        }
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new AccessDeniedException("Ban can dang nhap de xem bien ban ban giao.");
        }
        return shiftHandoverRepository.findMineWithDetails(currentUserId).stream()
                .map(ShiftHandoverResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> receiverCandidates(Integer staffScheduleId) {
        StaffSchedule schedule = loadSchedule(staffScheduleId);
        verifyScheduleAccess(schedule);
        Integer cinemaId = schedule.getCinema() != null ? schedule.getCinema().getId() : null;
        if (cinemaId == null) {
            throw new IllegalArgumentException("Ca lam viec chua co co so de chon nguoi nhan ban giao.");
        }
        Integer senderId = schedule.getStaff() != null ? schedule.getStaff().getUserId() : null;
        return staffRepository.findByCinemaIdWithDetails(cinemaId).stream()
                .filter(staff -> staff.getUser() != null && Boolean.TRUE.equals(staff.getUser().getIsActive()))
                .filter(staff -> senderId == null || !senderId.equals(staff.getUserId()))
                .map(staff -> {
                    var user = staff.getUser();
                    return Map.<String, Object>of(
                            "userId", staff.getUserId(),
                            "staffCode", staff.getStaffCode() != null ? staff.getStaffCode() : "",
                            "fullName", user != null ? user.getFullName() : "Nhan vien",
                            "cinemaId", cinemaId
                    );
                })
                .toList();
    }

    @Transactional
    public ShiftHandoverResponse submit(ShiftHandoverRequest request) {
        StaffSchedule schedule = request.getStaffScheduleId() != null
                ? loadSchedule(request.getStaffScheduleId())
                : shiftAccessService.requireCurrentStaffSchedule();
        verifyScheduleAccess(schedule);
        validateScheduleReadyForHandover(schedule);
        if (shiftHandoverRepository.existsByStaffScheduleIdAndStatus(schedule.getId(), STATUS_SUBMITTED)
                || shiftHandoverRepository.existsByStaffScheduleIdAndStatus(schedule.getId(), STATUS_RECEIVED)
                || shiftHandoverRepository.existsByStaffScheduleIdAndStatus(schedule.getId(), STATUS_PENDING_LEGACY)) {
            throw new IllegalArgumentException("Ca này đã có biên bản bàn giao đang chờ xác nhận.");
        }

        if (shiftHandoverRepository.existsByStaffScheduleIdAndStatus(schedule.getId(), STATUS_CONFIRMED)) {
            throw new IllegalArgumentException("Ca nay da duoc xac nhan ban giao, khong the tao lai.");
        }

        Staff receiver = loadReceiver(request.getReceiverStaffId(), schedule);

        ShiftHandoverSummaryResponse summary = buildSummary(schedule);
        BigDecimal declaredCash = money(request.getDeclaredCash());
        BigDecimal systemCash = money(summary.getSystemCash());

        ShiftHandover handover = ShiftHandover.builder()
                .staffSchedule(schedule)
                .receivedByStaff(receiver)
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
                .status(STATUS_SUBMITTED)
                .submittedAt(LocalDateTime.now())
                .note(cleanNote(request.getNote()))
                .build();
        return ShiftHandoverResponse.fromEntity(shiftHandoverRepository.save(handover));
    }

    @Transactional
    public ShiftHandoverResponse receive(Integer id, ShiftHandoverDecisionRequest request) {
        ShiftHandover handover = loadHandover(id);
        ensureSubmitted(handover);
        verifyReceiverAccess(handover);
        handover.setStatus(STATUS_RECEIVED);
        handover.setReceivedAt(LocalDateTime.now());
        handover.setReceiverNote(cleanNote(request != null ? request.getNote() : null));
        return ShiftHandoverResponse.fromEntity(shiftHandoverRepository.save(handover));
    }

    @Transactional
    public ShiftHandoverResponse confirm(Integer id, ShiftHandoverDecisionRequest request) {
        ShiftHandover handover = loadHandover(id);
        ensureReceived(handover);
        handover.setStatus(STATUS_CONFIRMED);
        handover.setConfirmedAt(LocalDateTime.now());
        handover.setApprovedByManager(currentStaffOrNull());
        mergeNote(handover, request);
        return ShiftHandoverResponse.fromEntity(shiftHandoverRepository.save(handover));
    }

    @Transactional
    public ShiftHandoverResponse reject(Integer id, ShiftHandoverDecisionRequest request) {
        ShiftHandover handover = loadHandover(id);
        ensureReviewable(handover);
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

    private Staff loadReceiver(Integer receiverStaffId, StaffSchedule schedule) {
        if (receiverStaffId == null) {
            throw new IllegalArgumentException("Vui long chon nhan vien nhan ban giao.");
        }
        Staff receiver = staffRepository.findByIdWithDetails(receiverStaffId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nhan vien nhan ban giao."));
        if (receiver.getUser() == null || !Boolean.TRUE.equals(receiver.getUser().getIsActive())) {
            throw new IllegalArgumentException("Nhan vien nhan ban giao khong hoat dong.");
        }
        Integer senderId = schedule.getStaff() != null ? schedule.getStaff().getUserId() : null;
        if (senderId != null && senderId.equals(receiver.getUserId())) {
            throw new IllegalArgumentException("Nguoi nhan ban giao phai khac nhan vien ket ca.");
        }
        Integer scheduleCinemaId = schedule.getCinema() != null ? schedule.getCinema().getId() : null;
        Integer receiverCinemaId = receiver.getCinema() != null ? receiver.getCinema().getId() : null;
        if (scheduleCinemaId == null || !scheduleCinemaId.equals(receiverCinemaId)) {
            throw new IllegalArgumentException("Nguoi nhan ban giao phai thuoc cung co so.");
        }
        return receiver;
    }

    private ShiftHandover loadHandover(Integer id) {
        return shiftHandoverRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biên bản bàn giao ca."));
    }

    private void verifyScheduleAccess(StaffSchedule schedule) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        // MANAGER: được xử lý bàn giao của mọi ca trong cơ sở mình
        if (SecurityUtils.isManager()) {
            Integer myCinemaId = SecurityUtils.getCurrentUserCinemaId();
            Integer scheduleCinemaId = schedule.getCinema() != null ? schedule.getCinema().getId() : null;
            if (myCinemaId == null || !myCinemaId.equals(scheduleCinemaId)) {
                throw new AccessDeniedException("Bạn chỉ quản lý bàn giao ca của cơ sở mình.");
            }
            return;
        }
        // STAFF: chỉ được bàn giao ca của chính mình
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        Integer scheduleStaffId = schedule.getStaff() != null ? schedule.getStaff().getUserId() : null;
        if (scheduleStaffId == null || !scheduleStaffId.equals(currentUserId)) {
            throw new AccessDeniedException("Bạn chỉ được bàn giao ca của chính mình.");
        }
    }

    private void validateScheduleReadyForHandover(StaffSchedule schedule) {
        if (!SCHEDULE_APPROVED.equalsIgnoreCase(schedule.getStatus())) {
            throw new IllegalArgumentException("Chi ca da duyet moi duoc ban giao.");
        }
        var shift = schedule.getShift();
        if (shift == null || shift.getEndTime() == null) {
            throw new IllegalArgumentException("Ca lam viec chua co gio ket thuc hop le.");
        }
        LocalDateTime handoverOpenAt = shift.getEndTime().minusMinutes(HANDOVER_EARLY_MINUTES);
        if (LocalDateTime.now().isBefore(handoverOpenAt)) {
            throw new IllegalArgumentException("Chua den gio ban giao. Ban co the ban giao tu " + handoverOpenAt + ".");
        }
    }

    private void ensureSubmitted(ShiftHandover handover) {
        String status = normalizeStatus(handover.getStatus());
        if (!STATUS_SUBMITTED.equals(status)) {
            throw new IllegalArgumentException("Chi bien ban da gui moi duoc nhan ban giao.");
        }
    }

    private void ensureReceived(ShiftHandover handover) {
        if (!STATUS_RECEIVED.equals(normalizeStatus(handover.getStatus()))) {
            throw new IllegalArgumentException("Chi bien ban da nhan moi duoc chot doi soat.");
        }
    }

    private void ensureReviewable(ShiftHandover handover) {
        String status = normalizeStatus(handover.getStatus());
        if (STATUS_CONFIRMED.equals(status) || STATUS_REJECTED.equals(status)) {
            throw new IllegalArgumentException("Bien ban ban giao nay da duoc xu ly.");
        }
    }

    private void verifyReceiverAccess(ShiftHandover handover) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        Integer receiverId = handover.getReceivedByStaff() != null ? handover.getReceivedByStaff().getUserId() : null;
        if (currentUserId == null || receiverId == null || !currentUserId.equals(receiverId)) {
            throw new AccessDeniedException("Ban khong phai nhan vien duoc chi dinh nhan ban giao.");
        }
    }

    private String normalizeStatus(String status) {
        if (STATUS_PENDING_LEGACY.equalsIgnoreCase(status)) {
            return STATUS_SUBMITTED;
        }
        return status != null ? status.toUpperCase() : STATUS_SUBMITTED;
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
