package com.devcine.backend.service;

import com.devcine.backend.dto.request.ShiftHandoverRequest;
import com.devcine.backend.dto.response.ShiftHandoverResponse;
import com.devcine.backend.dto.response.ShiftHandoverSummaryResponse;
import com.devcine.backend.entity.ShiftHandover;
import com.devcine.backend.entity.StaffSchedule;
import com.devcine.backend.repository.BookingFnbRepository;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.ConcessionSaleRepository;
import com.devcine.backend.repository.ShiftHandoverRepository;
import com.devcine.backend.repository.StaffScheduleRepository;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bàn giao ca (đối soát tiền cuối ca) — luồng TỰ ĐỘNG, không có bước duyệt.
 *
 * <p>Nhân viên nhập tiền mặt thực đếm → hệ thống tính chênh lệch so với "tiền két kỳ vọng"
 * (= quỹ đầu ca + doanh thu tiền mặt), lưu biên bản trạng thái {@code COMPLETED} và kết thúc ca
 * ngay lập tức. Quản lý xem lại ở màn Lịch sử bàn giao; dòng chênh lệch ≠ 0 được tô cảnh báo.
 */
@Service
@RequiredArgsConstructor
public class ShiftHandoverService {

    private static final String STATUS_SUBMITTED = "SUBMITTED";     // dữ liệu cũ (luồng có duyệt trước đây)
    private static final String STATUS_PENDING_LEGACY = "PENDING";  // dữ liệu cũ hơn nữa
    private static final String STATUS_COMPLETED = "COMPLETED";     // trạng thái chốt của luồng tự động
    private static final String SCHEDULE_APPROVED = "APPROVED";
    private static final String SCHEDULE_IN_PROGRESS = "IN_PROGRESS";
    private static final String SCHEDULE_COMPLETED = "COMPLETED";
    private static final int HANDOVER_EARLY_MINUTES = 30;

    private final ShiftAccessService shiftAccessService;
    private final StaffScheduleRepository staffScheduleRepository;
    private final ShiftHandoverRepository shiftHandoverRepository;
    private final BookingRepository bookingRepository;
    private final BookingFnbRepository bookingFnbRepository;
    private final ConcessionSaleRepository concessionSaleRepository;
    private final SystemSettingService systemSettingService;

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
            throw new AccessDeniedException("Chỉ nhân viên mới có thể xem biên bản bàn giao của mình.");
        }
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new AccessDeniedException("Bạn cần đăng nhập để xem biên bản bàn giao.");
        }
        return shiftHandoverRepository.findMineWithDetails(currentUserId).stream()
                .map(ShiftHandoverResponse::fromEntity)
                .toList();
    }

    /**
     * Gửi bàn giao — TỰ ĐỘNG chốt: tính chênh lệch, lưu biên bản COMPLETED và kết thúc ca.
     * Không cần người nhận, không cần quản lý duyệt.
     */
    @Transactional
    public ShiftHandoverResponse submit(ShiftHandoverRequest request) {
        StaffSchedule schedule = request.getStaffScheduleId() != null
                ? loadSchedule(request.getStaffScheduleId())
                : shiftAccessService.requireCurrentStaffSchedule();
        verifyScheduleAccess(schedule);
        validateScheduleReadyForHandover(schedule);

        // Mỗi ca chỉ bàn giao một lần.
        if (shiftHandoverRepository.existsByStaffScheduleIdAndStatus(schedule.getId(), STATUS_COMPLETED)
                || shiftHandoverRepository.existsByStaffScheduleIdAndStatus(schedule.getId(), STATUS_SUBMITTED)
                || shiftHandoverRepository.existsByStaffScheduleIdAndStatus(schedule.getId(), STATUS_PENDING_LEGACY)) {
            throw new IllegalArgumentException("Ca này đã được bàn giao, không thể bàn giao lại.");
        }

        ShiftHandoverSummaryResponse summary = buildSummary(schedule);
        BigDecimal declaredCash = money(request.getDeclaredCash());
        BigDecimal cashSales = money(summary.getCashSales());
        BigDecimal openingFloat = systemSettingService.getShiftOpeningFloat();
        // Tiền két kỳ vọng = quỹ đầu ca + doanh thu tiền mặt; chênh lệch = thực đếm − kỳ vọng.
        BigDecimal expectedCash = openingFloat.add(cashSales);

        ShiftHandover handover = ShiftHandover.builder()
                .staffSchedule(schedule)
                .declaredCash(declaredCash)
                .systemCash(cashSales)
                .openingFloat(openingFloat)
                .cashSales(cashSales)
                .cardSales(money(summary.getCardSales()))
                .transferSales(money(summary.getTransferSales()))
                .ticketRevenue(money(summary.getTicketRevenue()))
                .concessionRevenue(money(summary.getConcessionRevenue()))
                .ticketCount(summary.getTicketCount())
                .concessionOrderCount(summary.getConcessionOrderCount())
                .difference(declaredCash.subtract(expectedCash))
                .status(STATUS_COMPLETED)
                .submittedAt(LocalDateTime.now())
                .note(cleanNote(request.getNote()))
                .build();
        ShiftHandover saved = shiftHandoverRepository.save(handover);

        // Kết thúc ca ngay: thu hồi quyền theo Position, khóa POS để không phát sinh thêm doanh thu.
        // Bàn giao đóng vai trò kết ca cho quầy tiền → ghi luôn mốc giờ ra thực tế.
        schedule.setStatus(SCHEDULE_COMPLETED);
        if (schedule.getActualCheckOutAt() == null) {
            schedule.setActualCheckOutAt(LocalDateTime.now());
        }
        staffScheduleRepository.save(schedule);

        return ShiftHandoverResponse.fromEntity(saved);
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

        BigDecimal openingFloat = systemSettingService.getShiftOpeningFloat();

        return ShiftHandoverSummaryResponse.builder()
                .staffScheduleId(schedule.getId())
                .staffId(staff != null ? staff.getUserId() : null)
                .staffName(user != null ? user.getFullName() : "Nhân viên")
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
                .openingFloat(openingFloat)
                .expectedCash(openingFloat.add(cashSales))
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
        String scheduleStatus = schedule.getStatus();
        // Ca đã Check-in (IN_PROGRESS) hoặc đã duyệt (APPROVED, tương thích luồng cũ) mới được bàn giao.
        if (!SCHEDULE_APPROVED.equalsIgnoreCase(scheduleStatus)
                && !SCHEDULE_IN_PROGRESS.equalsIgnoreCase(scheduleStatus)) {
            throw new IllegalArgumentException("Chỉ ca đã duyệt hoặc đang làm việc mới được bàn giao.");
        }
        var shift = schedule.getShift();
        if (shift == null || shift.getEndTime() == null) {
            throw new IllegalArgumentException("Ca làm việc chưa có giờ kết thúc hợp lệ.");
        }
        LocalDateTime handoverOpenAt = shift.getEndTime().minusMinutes(HANDOVER_EARLY_MINUTES);
        if (LocalDateTime.now().isBefore(handoverOpenAt)) {
            throw new IllegalArgumentException("Chưa đến giờ bàn giao. Bạn có thể bàn giao từ " + handoverOpenAt + ".");
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
