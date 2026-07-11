package com.devcine.backend.service;

import com.devcine.backend.entity.ApprovalRequest;
import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.BookingSeat;
import com.devcine.backend.entity.ConcessionSale;
import com.devcine.backend.entity.ConcessionSaleItem;
import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.Seat;
import com.devcine.backend.entity.StaffSchedule;
import com.devcine.backend.enums.WorkPosition;
import com.devcine.backend.repository.ApprovalRequestRepository;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.repository.ConcessionSaleItemRepository;
import com.devcine.backend.repository.ConcessionSaleRepository;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.SeatRepository;
import com.devcine.backend.repository.UserRepository;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Luồng phê duyệt "sửa sai" nội bộ của Trưởng ca (Shift Leader):
 *  - FNB_VOID: hủy hóa đơn bắp nước bấm nhầm (hoàn kho + thu hồi điểm).
 *  - SEAT_MOVE: đổi ghế cho khách do sự cố vật lý (giữ nguyên giá/booking gốc).
 *
 * Nhân viên quầy KHÔNG tự thực hiện — chỉ tạo yêu cầu; Trưởng ca (hoặc Manager/Admin) duyệt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    public static final String TYPE_FNB_VOID = "FNB_VOID";
    public static final String TYPE_SEAT_MOVE = "SEAT_MOVE";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final ApprovalRequestRepository approvalRepository;
    private final ConcessionSaleRepository concessionSaleRepository;
    private final ConcessionSaleItemRepository concessionSaleItemRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final ShiftAccessService shiftAccessService;
    private final LoyaltyService loyaltyService;
    private final ObjectMapper objectMapper;

    // ----------------------------------------------------------------------------------
    // TẠO YÊU CẦU (nhân viên quầy)
    // ----------------------------------------------------------------------------------

    /** Nhân viên F&B (hoặc Trưởng ca) yêu cầu hủy một hóa đơn bắp nước bấm nhầm. */
    @Transactional
    public ApprovalRequest requestFnbVoid(Integer saleId, String reason) {
        StaffSchedule schedule = shiftAccessService
                .requireCurrentShiftForStaff(WorkPosition.withLeader(WorkPosition.FNB), "yeu cau huy hoa don F&B");

        ConcessionSale sale = concessionSaleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn F&B."));
        if (!"CONFIRMED".equalsIgnoreCase(sale.getStatus())) {
            throw new IllegalArgumentException("Hóa đơn không ở trạng thái hợp lệ để hủy.");
        }
        if (approvalRepository.existsByTypeAndRefIdAndStatus(TYPE_FNB_VOID, saleId, STATUS_PENDING)) {
            throw new IllegalArgumentException("Hóa đơn này đã có yêu cầu hủy đang chờ duyệt.");
        }

        ApprovalRequest request = baseRequest(schedule);
        request.setType(TYPE_FNB_VOID);
        request.setRefId(saleId);
        request.setRefCode(sale.getSaleCode());
        request.setSummary("Hủy hóa đơn " + sale.getSaleCode()
                + " (" + sale.getTotalPrice().toBigInteger() + "đ)");
        request.setReason(reason);
        return approvalRepository.save(request);
    }

    /** Nhân viên quầy vé (hoặc Trưởng ca) yêu cầu đổi ghế cho khách do sự cố vật lý. */
    @Transactional
    public ApprovalRequest requestSeatMove(Integer bookingSeatId, Integer toSeatId, String reason) {
        StaffSchedule schedule = shiftAccessService.requireCurrentShiftForStaff(
                WorkPosition.withLeader(WorkPosition.POS_TICKETING), "yeu cau doi ghe");

        BookingSeat bookingSeat = bookingSeatRepository.findById(bookingSeatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế trong booking."));
        Seat target = seatRepository.findById(toSeatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế đích."));
        validateSeatMove(bookingSeat, target);

        Booking booking = bookingSeat.getBooking();
        Seat from = bookingSeat.getSeat();
        String fromLabel = seatLabel(from);
        String toLabel = seatLabel(target);

        ApprovalRequest request = baseRequest(schedule);
        request.setType(TYPE_SEAT_MOVE);
        request.setRefId(booking.getId());
        request.setRefCode(booking.getBookingCode());
        request.setSummary("Đổi ghế " + fromLabel + " → " + toLabel + " (" + booking.getBookingCode() + ")");
        request.setReason(reason);
        request.setPayload(writeJson(Map.of(
                "bookingSeatId", bookingSeatId,
                "fromSeatId", from.getId(),
                "toSeatId", toSeatId,
                "fromLabel", fromLabel,
                "toLabel", toLabel
        )));
        return approvalRepository.save(request);
    }

    // ----------------------------------------------------------------------------------
    // DUYỆT / TỪ CHỐI (Trưởng ca hoặc Manager/Admin)
    // ----------------------------------------------------------------------------------

    @Transactional
    public ApprovalRequest approve(Integer requestId) {
        // Gate: chỉ Trưởng ca đang trong ca (hoặc Manager/Admin theo tier) mới được duyệt.
        StaffSchedule approverSchedule =
                shiftAccessService.requireCurrentShiftForStaff(WorkPosition.APPROVERS, "phe duyet sua sai");

        ApprovalRequest request = loadPending(requestId);
        switch (request.getType()) {
            case TYPE_FNB_VOID -> executeFnbVoid(request, approverSchedule);
            case TYPE_SEAT_MOVE -> executeSeatMove(request);
            default -> throw new IllegalArgumentException("Loại yêu cầu không hỗ trợ: " + request.getType());
        }
        stampDecision(request, STATUS_APPROVED, null);
        return approvalRepository.save(request);
    }

    @Transactional
    public ApprovalRequest reject(Integer requestId, String note) {
        shiftAccessService.requireCurrentShiftForStaff(WorkPosition.APPROVERS, "phe duyet sua sai");
        ApprovalRequest request = loadPending(requestId);
        stampDecision(request, STATUS_REJECTED, note);
        return approvalRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> listPending() {
        // Trưởng ca/Manager chỉ thấy yêu cầu của cơ sở mình; Admin thấy tất cả.
        Integer cinemaId = SecurityUtils.getCurrentUserCinemaId();
        if (cinemaId != null && !SecurityUtils.isAdmin()) {
            return approvalRepository.findByCinemaIdAndStatusOrderByCreatedAtDesc(cinemaId, STATUS_PENDING);
        }
        return approvalRepository.findByStatusOrderByCreatedAtDesc(STATUS_PENDING);
    }

    /** Danh sách ghế ĐÃ BÁN (kèm bookingSeatId) của một suất chiếu — dùng để chọn ghế hỏng khi đổi ghế. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> seatMoveOptions(Integer showtimeId) {
        return bookingSeatRepository.findReservedSeatsByShowtime(showtimeId).stream()
                .filter(bs -> "SOLD".equalsIgnoreCase(bs.getStatus()))
                .map(bs -> {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("bookingSeatId", bs.getId());
                    m.put("seatId", bs.getSeat().getId());
                    m.put("label", seatLabel(bs.getSeat()));
                    m.put("seatType", bs.getSeat().getSeatType() != null ? bs.getSeat().getSeatType().getName() : null);
                    m.put("bookingCode", bs.getBooking().getBookingCode());
                    return m;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> listMine() {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return List.of();
        return approvalRepository.findByRequestedByUserIdOrderByCreatedAtDesc(userId);
    }

    // ----------------------------------------------------------------------------------
    // Thực thi hành động khi duyệt
    // ----------------------------------------------------------------------------------

    private void executeFnbVoid(ApprovalRequest request, StaffSchedule approverSchedule) {
        ConcessionSale sale = concessionSaleRepository.findById(request.getRefId())
                .orElseThrow(() -> new IllegalArgumentException("Hóa đơn F&B không còn tồn tại."));
        if (!"CONFIRMED".equalsIgnoreCase(sale.getStatus())) {
            throw new IllegalArgumentException("Hóa đơn đã được xử lý trước đó.");
        }

        // Thu hồi điểm thưởng đã cộng cho thành viên (đảo cả ví lẫn tích lũy trọn đời, ghi sổ điểm).
        loyaltyService.reclaim(sale.getCustomer(), sale.getTotalPrice(), "VOID_FNB", sale.getSaleCode());

        sale.setStatus("VOIDED");
        concessionSaleRepository.save(sale);
        log.info("Đã hủy (void) hóa đơn F&B {} theo yêu cầu duyệt #{}", sale.getSaleCode(), request.getId());
    }

    private void executeSeatMove(ApprovalRequest request) {
        Map<String, Object> data = readJson(request.getPayload());
        Integer bookingSeatId = asInt(data.get("bookingSeatId"));
        Integer toSeatId = asInt(data.get("toSeatId"));

        BookingSeat bookingSeat = bookingSeatRepository.findById(bookingSeatId)
                .orElseThrow(() -> new IllegalArgumentException("Ghế trong booking không còn tồn tại."));
        Seat target = seatRepository.findById(toSeatId)
                .orElseThrow(() -> new IllegalArgumentException("Ghế đích không còn tồn tại."));

        // Kiểm tra lại tại thời điểm duyệt (chống tranh chấp: ghế đích có thể vừa bị bán).
        validateSeatMove(bookingSeat, target);

        Seat old = bookingSeat.getSeat();
        bookingSeat.setSeat(target);
        bookingSeatRepository.save(bookingSeat);
        log.info("Đã đổi ghế {} → {} cho booking {} theo yêu cầu duyệt #{}",
                seatLabel(old), seatLabel(target), request.getRefCode(), request.getId());
    }

    /** Ghế đích phải: active, cùng phòng suất chiếu, cùng loại ghế (tương đương giá), đang trống. */
    private void validateSeatMove(BookingSeat bookingSeat, Seat target) {
        Seat current = bookingSeat.getSeat();
        if (target.getId().equals(current.getId())) {
            throw new IllegalArgumentException("Ghế đích trùng với ghế hiện tại.");
        }
        if (!Boolean.TRUE.equals(target.getIsActive())) {
            throw new IllegalArgumentException("Ghế đích không khả dụng.");
        }
        Booking booking = bookingSeat.getBooking();
        Integer roomId = booking.getShowtime().getRoom().getId();
        if (target.getRoom() == null || !roomId.equals(target.getRoom().getId())) {
            throw new IllegalArgumentException("Ghế đích không thuộc phòng chiếu của vé.");
        }
        Integer currentTypeId = current.getSeatType() != null ? current.getSeatType().getId() : null;
        Integer targetTypeId = target.getSeatType() != null ? target.getSeatType().getId() : null;
        if (currentTypeId == null || !currentTypeId.equals(targetTypeId)) {
            throw new IllegalArgumentException("Ghế đích phải cùng loại (giá trị tương đương) với ghế cũ.");
        }

        Integer showtimeId = booking.getShowtime().getId();
        boolean taken = bookingSeatRepository.findReservedSeatsByShowtime(showtimeId).stream()
                .anyMatch(bs -> !bs.getId().equals(bookingSeat.getId())
                        && bs.getSeat().getId().equals(target.getId()));
        if (taken) {
            throw new IllegalArgumentException("Ghế đích đã có người giữ/mua.");
        }
    }

    // ----------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------

    private ApprovalRequest baseRequest(StaffSchedule schedule) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Integer cinemaId = schedule != null && schedule.getCinema() != null
                ? schedule.getCinema().getId()
                : SecurityUtils.getCurrentUserCinemaId();
        return ApprovalRequest.builder()
                .status(STATUS_PENDING)
                .cinemaId(cinemaId)
                .requestedByUserId(userId)
                .requestedByName(resolveUserName(schedule, userId))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ApprovalRequest loadPending(Integer requestId) {
        ApprovalRequest request = approvalRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu phê duyệt."));
        if (!STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalArgumentException("Yêu cầu đã được xử lý.");
        }
        return request;
    }

    private void stampDecision(ApprovalRequest request, String status, String note) {
        request.setStatus(status);
        request.setDecisionNote(note);
        request.setApprovedByUserId(SecurityUtils.getCurrentUserId());
        request.setApprovedByName(resolveUserName(null, SecurityUtils.getCurrentUserId()));
        request.setDecidedAt(LocalDateTime.now());
    }

    private String resolveUserName(StaffSchedule schedule, Integer userId) {
        if (schedule != null && schedule.getStaff() != null && schedule.getStaff().getUser() != null) {
            return schedule.getStaff().getUser().getFullName();
        }
        if (userId == null) return null;
        return userRepository.findById(userId).map(u -> u.getFullName()).orElse(null);
    }

    private String seatLabel(Seat seat) {
        return seat.getRowChar() + seat.getColNum();
    }

    private String writeJson(Map<String, Object> map) {
        return objectMapper.writeValueAsString(map);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        return objectMapper.readValue(json, Map.class);
    }

    private Integer asInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
