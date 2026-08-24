package com.devcine.backend.service;

import com.devcine.backend.dto.request.CancelSeatRequest;
import com.devcine.backend.dto.request.CompensationRequest;
import com.devcine.backend.dto.request.RelocateRequest;
import com.devcine.backend.dto.request.SeatPhysicalStatusRequest;
import com.devcine.backend.dto.response.CompensationOption;
import com.devcine.backend.dto.response.IncidentBookingContext;
import com.devcine.backend.dto.response.IncidentListItem;
import com.devcine.backend.dto.response.IncidentResultResponse;
import com.devcine.backend.dto.response.SeatPhysicalStatusResponse;
import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Xử lý sự cố phòng chiếu tại quầy: đổi ghế đền bù, hủy chỗ, khóa ghế bảo trì, và ghi vết.
 *
 * <p>Chốt kiến trúc (xem CLAUDE.md / memory devcine-permission-architecture):
 * <ul>
 *   <li>Đổi ghế = REPOINT {@code BookingSeat.seat_id} TẠI CHỖ → giữ nguyên Ticket/QR/giá, nhãn ghế
 *       suy live → reprint & email tự đúng. Không sinh Ticket/QR mới.</li>
 *   <li>Chống trùng ghế 100%: Redis Distributed Lock + DB Pessimistic Write Lock (Showtime & Booking).</li>
 *   <li>Realtime sơ đồ ghế: WebSocket STOMP broadcast tới {@code /topic/showtime/{id}}.</li>
 *   <li>Không hoàn tiền — đền bằng Voucher (từ Promotion-template "COMP_*") hoặc đền trực tiếp tại
 *       quầy cho khách vãng lai (không sinh Voucher, chỉ ghi vết).</li>
 *   <li>Mọi ghi đều qua {@link SecurityUtils#assertCinemaAccess} → chặn thao tác chéo cụm rạp (403).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatIncidentService {

    private static final String PREFIX_COMP = "COMP_";
    private static final String REDIS_LOCK_PREFIX = "lock:showtime:";
    private static final Duration REDIS_LOCK_TTL = Duration.ofSeconds(10);

    /** Xếp hạng loại ghế để suy "hạ hạng vật lý" (chỉ gợi ý đền bù, không enforce). */
    private static final Map<String, Integer> SEAT_RANK = Map.of("NORMAL", 0, "VIP", 1, "SWEETBOX", 2);

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final SeatIncidentRepository incidentRepository;
    private final PromotionRepository promotionRepository;
    private final VoucherRepository voucherRepository;
    private final StaffRepository staffRepository;
    private final TicketService ticketService;
    private final SeatLockService seatLockService;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    // ===================== TRA CỨU =====================

    /** Cửa sổ xử lý sự cố sau khi suất kết thúc: 2 giờ (120 phút). */
    private static final int INCIDENT_WINDOW_HOURS = 2;

    /** Tra vé theo Mã đặt vé. Input là booking code (KHÔNG phải SĐT — dùng lookupByPhone() cho SĐT). */
    @Transactional(readOnly = true)
    public IncidentBookingContext lookup(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã vé hoặc số điện thoại.");
        }
        String q = query.trim();
        if (q.matches("\\d{9,11}")) {
            // SĐT → dùng lookupByPhone() thay vì trả 1 đơn tuỳ tiện
            throw new IllegalArgumentException(
                    "Vui lòng dùng tìm kiếm theo SĐT ở bên trên để chọn đúng đơn cần xử lý.");
        }
        Booking booking = bookingRepository.findByBookingCodeForPrint(q)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn với mã: " + q));
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Đơn chưa thanh toán hoặc không hợp lệ để xử lý.");
        }
        // Kiểm tra cửa sổ xử lý sự cố
        Showtime st = booking.getShowtime();
        if (st != null && st.getEndTime() != null) {
            LocalDateTime deadline = st.getEndTime().plusHours(INCIDENT_WINDOW_HOURS);
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new IllegalArgumentException(
                    "Suất chiếu đã kết thúc từ lúc " +
                    st.getEndTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) +
                    " — hết thời gian xử lý sự cố (trong vòng " + INCIDENT_WINDOW_HOURS + " giờ sau khi chiếu).");
            }
        }
        SecurityUtils.assertCinemaAccess(cinemaIdOf(booking));
        return buildContext(booking);
    }

    /**
     * Tra cứu toàn bộ đơn CONFIRMED còn trong cửa sổ xử lý sự cố theo SĐT khách.
     * Trả danh sách để nhân viên chọn đơn đúng (trường hợp khách mua nhiều đơn / mua thêm cho người thân).
     */
    @Transactional(readOnly = true)
    public List<IncidentBookingContext> lookupByPhone(String phone) {
        if (phone == null || !phone.trim().matches("\\d{9,11}")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(INCIDENT_WINDOW_HOURS + 3);
        List<Booking> bookings = bookingRepository.findConfirmedByCustomerPhone(
                phone.trim(), cutoff, now, PageRequest.of(0, 10));
        if (bookings.isEmpty()) {
            throw new IllegalArgumentException(
                "Không tìm thấy đơn hợp lệ cho SĐT " + phone.trim() + ". " +
                "Suất chiếu phải chưa kết thúc quá " + INCIDENT_WINDOW_HOURS + " giờ.");
        }
        // Lọc qua cinema scoping: chỉ trả đơn thuộc rạp hiện tại (STAFF/MANAGER), ADMIN thấy tất cả
        return bookings.stream()
                .filter(b -> {
                    try { SecurityUtils.assertCinemaAccess(cinemaIdOf(b)); return true; }
                    catch (Exception e) { return false; }
                })
                .map(this::buildContext)
                .collect(Collectors.toList());
    }


    /** Chọn theo Phòng→Suất→Ghế: truy ngược đơn đang giữ ghế đã bán đó. */
    @Transactional(readOnly = true)
    public IncidentBookingContext findSeatOccupant(Integer showtimeId, Integer seatId) {
        BookingSeat bs = bookingSeatRepository.findSoldSeatOccupant(showtimeId, seatId)
                .orElseThrow(() -> new IllegalArgumentException("Ghế này chưa bán cho đơn nào ở suất đã chọn."));
        Booking booking = bs.getBooking();
        SecurityUtils.assertCinemaAccess(cinemaIdOf(booking));
        return buildContext(booking);
    }

    private IncidentBookingContext buildContext(Booking booking) {
        Showtime st = booking.getShowtime();
        Room room = st != null ? st.getRoom() : null;
        Cinema cinema = room != null ? room.getCinema() : null;
        boolean hasCustomer = booking.getCustomer() != null && booking.getCustomer().getUser() != null;
        User user = hasCustomer ? booking.getCustomer().getUser() : null;

        List<IncidentBookingContext.IncidentSeatLine> seatLines =
                bookingSeatRepository.findAllByBookingIdWithSeat(booking.getId()).stream()
                        .map(bs -> IncidentBookingContext.IncidentSeatLine.builder()
                                .bookingSeatId(bs.getId())
                                .seatId(bs.getSeat().getId())
                                .seatLabel(bs.getSeat().displayLabel())
                                .seatType(bs.getSeat().getSeatType() != null ? bs.getSeat().getSeatType().getName() : null)
                                .ticketType(bs.getTicketType())
                                .priceSnapshot(bs.getPriceSnapshot())
                                .status(bs.getStatus())
                                .build())
                        .collect(Collectors.toList());

        boolean expired = st != null && st.getEndTime() != null && LocalDateTime.now().isAfter(st.getEndTime());

        IncidentBookingContext.ShowtimeBrief brief = IncidentBookingContext.ShowtimeBrief.builder()
                .showtimeId(st != null ? st.getId() : null)
                .movieTitle(st != null && st.getMovie() != null ? st.getMovie().getTitle() : null)
                .roomName(room != null ? room.getName() : null)
                .formatName(st != null && st.getFormat() != null ? st.getFormat().getName() : null)
                .startTime(st != null ? st.getStartTime() : null)
                .cinemaId(cinema != null ? cinema.getId() : null)
                .cinemaName(cinema != null ? cinema.getName() : null)
                .started(now_after_start)
                .expired(expired)
                .build();

        return IncidentBookingContext.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .channel(booking.getChannel())
                .hasCustomer(hasCustomer)
                .customerId(hasCustomer ? booking.getCustomer().getUserId() : null)
                .customerName(user != null ? user.getFullName() : null)
                .customerPhone(user != null ? user.getPhone() : null)
                .showtime(brief)
                .seats(seatLines)
                .build();
    }

    // ===================== TEMPLATE ĐỀN BÙ =====================

    @Transactional(readOnly = true)
    public List<CompensationOption> listCompensationTemplates() {
        return promotionRepository.findAll().stream()
                .filter(p -> p.getCode() != null && p.getCode().startsWith(PREFIX_COMP))
                .map(p -> CompensationOption.builder()
                        .promotionId(p.getId())
                        .code(p.getCode())
                        .label(p.getName() != null ? p.getName() : p.getCode())
                        .type(compTypeOf(p))
                        .discountValue(p.getDiscountValue() != null ? p.getDiscountValue() : BigDecimal.ZERO)
                        .cancelOnly("GIFT_TICKET".equals(compTypeOf(p)))
                        .build())
                .sorted((a, b) -> a.code().compareTo(b.code()))
                .collect(Collectors.toList());
    }

    // ===================== KHÓA GHẾ VẬT LÝ =====================

    @Transactional
    public SeatPhysicalStatusResponse setSeatPhysicalStatus(Integer seatId, SeatPhysicalStatusRequest req) {
        String status = req.status() != null ? req.status().trim().toUpperCase() : "";
        if (!List.of("AVAILABLE", "MAINTENANCE", "LOCKED").contains(status)) {
            throw new IllegalArgumentException("Trạng thái ghế không hợp lệ.");
        }
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế."));
        Cinema cinema = seat.getRoom() != null ? seat.getRoom().getCinema() : null;
        SecurityUtils.assertCinemaAccess(cinema != null ? cinema.getId() : null);

        seat.setSeatStatus(status);
        seatRepository.save(seat);

        Integer incidentId = null;
        if (!"AVAILABLE".equals(status)) { // chỉ ghi vết khi KHÓA (bảo trì/khóa), không ghi khi mở lại
            SeatIncident si = incidentRepository.save(SeatIncident.builder()
                    .incidentType("SEAT_MAINTENANCE")
                    .oldSeat(seat)
                    .oldSeatLabel(seat.displayLabel())
                    .compensationType("NONE")
                    .reason(req.reason())
                    .handledBy(currentStaffOrNull())
                    .cinema(cinema)
                    .build());
            incidentId = si.getId();
        }

        // REALTIME WEBSOCKET STOMP: Broadcast tới tất cả suất chiếu active của phòng này
        if (seat.getRoom() != null && seat.getRoom().getId() != null) {
            List<Showtime> activeShowtimes = showtimeRepository.findActiveByRoomId(seat.getRoom().getId(), LocalDateTime.now());
            String eventType = "AVAILABLE".equals(status) ? "SEAT_RELEASED" : "SEAT_LOCKED";
            for (Showtime activeSt : activeShowtimes) {
                broadcastSeatEvent(activeSt.getId(), eventType, List.of(seat.getId()));
            }
        }

        return SeatPhysicalStatusResponse.builder()
                .seatId(seat.getId()).seatLabel(seat.displayLabel()).status(status).incidentId(incidentId)
                .build();
    }

    // ===================== ĐỔI GHẾ =====================

    @Transactional
    public IncidentResultResponse relocate(RelocateRequest req) {
        List<Integer> newSeatIds = req.swaps().stream().map(RelocateRequest.SeatSwap::newSeatId).toList();
        List<Integer> oldSeatIds = req.swaps().stream().map(RelocateRequest.SeatSwap::oldSeatId).toList();
        if (newSeatIds.stream().distinct().count() != newSeatIds.size()) {
            throw new IllegalArgumentException("Không thể đổi nhiều ghế về cùng một vị trí đích.");
        }

        Booking booking = loadConfirmedBooking(req.bookingId());
        Cinema cinema = cinemaOf(booking);
        SecurityUtils.assertCinemaAccess(cinema != null ? cinema.getId() : null);
        Showtime st = booking.getShowtime();

        // Guard: suất kết thúc > INCIDENT_WINDOW_HOURS → chặn toàn bộ thao tác
        assertWithinIncidentWindow(st);

        if (st.getStartTime() != null && st.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Suất đã bắt đầu — chỉ có thể hủy chỗ, không đổi ghế.");
        }

        // Lớp bảo vệ 1: REDIS DISTRIBUTED LOCK cho toàn bộ ghế đích (chống race condition giữa các server/instance)
        List<String> lockedKeys = acquireRedisLocks(st.getId(), newSeatIds);
        try {
            // Lớp bảo vệ 2: TRANSACTION PESSIMISTIC WRITE LOCK trên Suất chiếu và Đơn đặt vé
            showtimeRepository.findByIdForUpdate(st.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));
            bookingRepository.findByIdWithPessimisticLock(booking.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt vé."));

            // Check transient lock từ quầy POS / Online đang click giữ
            List<Integer> transientLocks = seatLockService.lockedSeatIds(st.getId());
            for (Integer newId : newSeatIds) {
                if (transientLocks.contains(newId)) {
                    throw new IllegalStateException("Ghế đích đang được giữ tạm bởi giao dịch khác. Vui lòng chọn ghế khác.");
                }
            }

            // Ghế nguồn phải thuộc đơn & đang SOLD
            Map<Integer, BookingSeat> soldBySeatId = bookingSeatRepository.findAllByBookingIdWithSeat(booking.getId())
                    .stream().filter(bs -> "SOLD".equalsIgnoreCase(bs.getStatus()))
                    .collect(Collectors.toMap(bs -> bs.getSeat().getId(), bs -> bs, (a, b) -> a));

            // Race check: ghế đích vừa bị chiếm (quầy khác / khách online) → 409
            List<Integer> conflicts = bookingSeatRepository.findConflictingSeats(st.getId(), newSeatIds, LocalDateTime.now());
            if (!conflicts.isEmpty()) {
                throw new IllegalStateException("Ghế đích vừa bị chiếm bởi giao dịch khác. Vui lòng chọn ghế trống khác.");
            }

            Map<Integer, Seat> newSeats = seatRepository.findByIdInWithSeatType(newSeatIds).stream()
                    .collect(Collectors.toMap(Seat::getId, s -> s, (a, b) -> a));

            // Nạp 1 lần tập ghế đã xử lý sự cố → tránh N query existsActiveForBookingSeat trong vòng lặp
            java.util.Set<Integer> alreadyProcessedSeatIds = incidentRepository.findProcessedSeatIdsByBooking(booking.getId());

            List<IncidentResultResponse.SeatSwapResult> swapResults = new ArrayList<>();
            List<SeatIncident> toSave = new ArrayList<>();
            List<BookingSeat> updatedSeats = new ArrayList<>();
            for (RelocateRequest.SeatSwap swap : req.swaps()) {
                BookingSeat bs = soldBySeatId.get(swap.oldSeatId());
                if (bs == null) {
                    throw new IllegalArgumentException("Ghế nguồn không thuộc đơn hoặc đã được xử lý.");
                }
                if (alreadyProcessedSeatIds.contains(swap.oldSeatId())) {
                    throw new IllegalStateException("Ghế " + bs.getSeat().displayLabel() + " đã được xử lý sự cố trước đó.");
                }
                Seat newSeat = newSeats.get(swap.newSeatId());
                if (newSeat == null || !newSeat.isSeatCell() || !Boolean.TRUE.equals(newSeat.getIsActive())) {
                    throw new IllegalArgumentException("Ghế đích không hợp lệ.");
                }
                if (newSeat.getSeatStatus() != null && !"AVAILABLE".equals(newSeat.getSeatStatus())) {
                    throw new IllegalArgumentException("Ghế đích đang bảo trì/khóa, không thể chuyển tới.");
                }
                if (newSeat.getRoom() == null || !newSeat.getRoom().getId().equals(st.getRoom().getId())) {
                    throw new IllegalArgumentException("Ghế đích không thuộc phòng của suất chiếu.");
                }

                Seat oldSeat = bs.getSeat();
                String oldLabel = oldSeat.displayLabel();
                String newLabel = newSeat.displayLabel();
                boolean downgrade = isDowngrade(oldSeat, newSeat);

                bs.setSeat(newSeat); // REPOINT tại chỗ → giữ QR/Ticket/giá
                updatedSeats.add(bs);

                toSave.add(SeatIncident.builder()
                        .incidentType("RELOCATE")
                        .booking(booking).showtime(st)
                        .oldSeat(oldSeat).newSeat(newSeat)
                        .oldSeatLabel(oldLabel).newSeatLabel(newLabel)
                        .compensationType("NONE")
                        .reason(req.reason())
                        .handledBy(currentStaffOrNull()).cinema(cinema)
                        .build());
                swapResults.add(IncidentResultResponse.SeatSwapResult.builder()
                        .oldLabel(oldLabel).newLabel(newLabel).downgrade(downgrade).build());
            }
            bookingSeatRepository.saveAll(updatedSeats); // gom thành 1 batch thay vì N saves

            // Đền bù ÁP DỤNG MỘT LẦN cho cả lần xử lý → gắn vào dòng ghi vết đầu tiên (tránh cộng trùng trị giá)
            IncidentResultResponse.CompensationResult comp = applyCompensation(booking, req.compensation(), null, false);
            attachCompensation(toSave.get(0), comp);

            List<SeatIncident> saved = incidentRepository.saveAll(toSave);
            List<Integer> incidentIds = saved.stream().map(SeatIncident::getId).toList();

            // REALTIME WEBSOCKET STOMP:
            // 1. Ghế mới -> SEAT_SOLD (đồng thời dọn transient lock)
            seatLockService.markSold(st.getId(), newSeatIds);
            // 2. Ghế cũ -> SEAT_RELEASED
            broadcastSeatEvent(st.getId(), "SEAT_RELEASED", oldSeatIds);

            boolean emailResent = ticketService.resendTicketEmailIfOnline(booking.getId());
            return IncidentResultResponse.builder()
                    .incidentIds(incidentIds).swaps(swapResults).compensation(comp)
                    .reprint(ticketService.buildPrintData(booking.getId()))
                    .emailResent(emailResent)
                    .build();
        } finally {
            releaseRedisLockKeys(lockedKeys);
        }
    }

    // ===================== HỦY CHỖ =====================

    @Transactional
    public IncidentResultResponse cancel(CancelSeatRequest req) {
        Booking booking = loadConfirmedBooking(req.bookingId());
        Cinema cinema = cinemaOf(booking);
        SecurityUtils.assertCinemaAccess(cinema != null ? cinema.getId() : null);
        // Guard: suất kết thúc > INCIDENT_WINDOW_HOURS → chặn toàn bộ thao tác
        assertWithinIncidentWindow(booking.getShowtime());
        // Thao tác quầy tương tác: người xử lý = nhân viên đang đăng nhập; ghi vết loại "CANCEL".
        return performCancel(booking, req.bookingSeatIds(), req.compensation(), req.reason(),
                currentStaffOrNull(), "CANCEL");
    }

    /**
     * LÕI HỦY CHỖ dùng chung — KHÔNG chạm SecurityContext (an toàn khi gọi từ thread @Async không
     * có ngữ cảnh bảo mật). Cả luồng quầy tương tác {@link #cancel} lẫn luồng đóng cửa đột xuất
     * {@link #cancelBookingForEmergency} đều tái sử dụng để tránh nhân đôi logic đền bù/ghi vết.
     *
     * <p>Người gọi chịu trách nhiệm kiểm tra quyền (assertCinemaAccess) TRƯỚC khi vào đây. Với luồng
     * hệ thống, phạm vi cơ sở đã được xác định bởi cinemaId của sự kiện nên không cần kiểm tra lại.</p>
     *
     * @param booking        đơn CONFIRMED đã nạp
     * @param bookingSeatIds các dòng ghế cần hủy (phải đang SOLD)
     * @param comp           khối đền bù (NONE với khách vãng lai)
     * @param reason         lý do ghi vết
     * @param handledBy      nhân sự xử lý (null = thao tác hệ thống)
     * @param incidentType   loại ghi vết: "CANCEL" (quầy) | "EMERGENCY_CLOSURE" (đóng cửa)
     */
    private IncidentResultResponse performCancel(Booking booking, List<Integer> bookingSeatIds,
                                                 CompensationRequest comp, String reason,
                                                 Staff handledBy, String incidentType) {
        Cinema cinema = cinemaOf(booking);
        Showtime st = booking.getShowtime();

        // Transaction DB Lock trên Showtime & Booking
        if (st != null && st.getId() != null) {
            showtimeRepository.findByIdForUpdate(st.getId()).orElse(null);
        }
        bookingRepository.findByIdWithPessimisticLock(booking.getId()).orElse(null);

        // Nạp toàn bộ BookingSeat của đơn 1 lần (cả byId map lẫn để kiểm tra hủy-toàn-bộ sau này)
        List<BookingSeat> allBookingSeats = bookingSeatRepository.findAllByBookingIdWithSeat(booking.getId());
        Map<Integer, BookingSeat> byId = allBookingSeats.stream()
                .collect(Collectors.toMap(BookingSeat::getId, bs -> bs, (a, b) -> a));

        // Nạp 1 lần tập ghế đã xử lý sự cố → tránh N query existsActiveForBookingSeat trong vòng lặp
        java.util.Set<Integer> alreadyProcessedSeatIds = incidentRepository.findProcessedSeatIdsByBooking(booking.getId());

        List<SeatIncident> toSave = new ArrayList<>();
        List<BookingSeat> updatedSeats = new ArrayList<>();
        List<Integer> releasedSeatIds = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        for (Integer bsId : bookingSeatIds) {
            BookingSeat bs = byId.get(bsId);
            if (bs == null || !"SOLD".equalsIgnoreCase(bs.getStatus())) {
                throw new IllegalArgumentException("Ghế cần hủy không thuộc đơn hoặc đã được xử lý.");
            }
            if (alreadyProcessedSeatIds.contains(bs.getSeat().getId())) {
                throw new IllegalStateException("Ghế " + bs.getSeat().displayLabel() + " đã được xử lý sự cố trước đó.");
            }
            Seat seat = bs.getSeat();
            totalValue = totalValue.add(bs.getPriceSnapshot() != null ? bs.getPriceSnapshot() : BigDecimal.ZERO);

            bs.setStatus("CANCELLED"); // giải phóng ghế: query reserved chỉ đếm SOLD/HOLD
            updatedSeats.add(bs);
            releasedSeatIds.add(seat.getId());

            toSave.add(SeatIncident.builder()
                    .incidentType(incidentType)
                    .booking(booking).showtime(st)
                    .oldSeat(seat).oldSeatLabel(seat.displayLabel())
                    .compensationType("NONE")
                    .reason(reason)
                    .handledBy(handledBy).cinema(cinema)
                    .build());
        }
        bookingSeatRepository.saveAll(updatedSeats); // gom thành 1 batch thay vì N saves

        // BUG-08 FIX: Nếu toàn bộ BookingSeat của đơn bị hủy → chuyển Booking.status = CANCELLED
        // để khách không còn thấy đơn CONFIRMED trong lịch sử khi tất cả ghế đã bị thu hồi.
        // Điều kiện: không còn ghế SOLD hoặc HOLD nào còn hoạt động sau khi batch update hiện tại xong.
        // (Ghế đang bị hủy trong batch này đã set CANCELLED ở bước trên → tính vào byId cập nhật)
        boolean allCancelled = allBookingSeats.stream()
                .allMatch(bs -> {
                    // Ghế đang được hủy trong lần này → đã set CANCELLED ở bước trên
                    if (bookingSeatIds.contains(bs.getId())) return true;
                    // Ghế khác: không được ở trạng thái SOLD hoặc HOLD
                    String st2 = bs.getStatus() != null ? bs.getStatus().toUpperCase() : "";
                    return !st2.equals("SOLD") && !st2.equals("HOLD");
                });
        if (allCancelled) {
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            log.info("[SeatIncident] Đơn #{} hủy toàn bộ ghế → Booking.status = CANCELLED (incidentType={}).",
                    booking.getBookingCode(), incidentType);
        }

        // Hủy chỗ → đền bằng trị giá đúng giá vé đã mua; cho phép template GIFT_TICKET (đền nguyên vé)
        IncidentResultResponse.CompensationResult compResult = applyCompensation(booking, comp, totalValue, true);
        attachCompensation(toSave.get(0), compResult);

        List<SeatIncident> saved = incidentRepository.saveAll(toSave);

        // REALTIME WEBSOCKET STOMP: Giải phóng các ghế đã hủy cho mọi quầy / khách online
        if (st != null && st.getId() != null && !releasedSeatIds.isEmpty()) {
            broadcastSeatEvent(st.getId(), "SEAT_RELEASED", releasedSeatIds);
        }

        return IncidentResultResponse.builder()
                .incidentIds(saved.stream().map(SeatIncident::getId).toList())
                .swaps(List.of())
                .compensation(compResult)
                .reprint(null)          // ghế đã hủy → không in vé mới cho khách
                .emailResent(false)
                .build();
    }

    // ===================== ĐÓNG CỬA CỤM RẠP ĐỘT XUẤT =====================

    /** Loại ghi vết cho sự cố đóng cửa cụm rạp diện rộng. */
    private static final String INCIDENT_EMERGENCY = "EMERGENCY_CLOSURE";

    /**
     * Hủy + đền bù TOÀN BỘ ghế của MỘT đơn khi cụm rạp đóng cửa đột xuất. Mỗi lần gọi mở transaction
     * RIÊNG (REQUIRES_NEW) để cô lập lỗi: một đơn hỏng không kéo đổ cả batch. Được gọi từ luồng
     * {@code @Async} nên KHÔNG dựa vào SecurityContext — {@code handledByStaffId} truyền tường minh.
     *
     * <p>Idempotent: đơn không còn CONFIRMED, hoặc không còn ghế SOLD chưa xử lý → trả {@code null}
     * (bỏ qua, không lỗi). Khách VÃNG LAI (không tài khoản/không email) → chỉ ghi vết, không voucher,
     * không email (trả {@code null}).</p>
     *
     * @param bookingId           đơn cần xử lý
     * @param promotionTemplateId id template COMP_TICKET_FULL (đền nguyên vé) — null nếu chưa seed
     * @param voucherLabel        nhãn hiển thị voucher cho email
     * @param handledByStaffId    userId người kích hoạt (null = ghi vết hệ thống)
     * @param reason              lý do ghi vết
     * @return dữ liệu phẳng để gửi email hủy vé, hoặc {@code null} nếu không cần gửi
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public com.devcine.backend.dto.CancellationEmailData cancelBookingForEmergency(
            Integer bookingId, Integer promotionTemplateId, String voucherLabel,
            Integer handledByStaffId, String reason) {

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || !"CONFIRMED".equalsIgnoreCase(booking.getStatus()) || booking.getShowtime() == null) {
            return null; // đã bị xử lý bởi luồng khác / dữ liệu không hợp lệ → bỏ qua
        }

        // Chỉ hủy ghế còn SOLD và CHƯA có ghi vết sự cố (tránh ném lỗi ở performCancel nếu ghế đã
        // được đổi/hủy thủ công trước đó) → batch bền vững với dữ liệu hỗn hợp.
        // Nạp 1 lần tập ghế đã xử lý → tránh N query existsActiveForBookingSeat trong stream filter.
        java.util.Set<Integer> processedSeatIds = incidentRepository.findProcessedSeatIdsByBooking(bookingId);
        // Nạp toàn bộ booking seats 1 lần (kể cả để lấy label sau này → tránh double-query)
        List<BookingSeat> allBookingSeats = bookingSeatRepository.findAllByBookingIdWithSeat(bookingId);
        List<Integer> bookingSeatIds = allBookingSeats.stream()
                .filter(bs -> "SOLD".equalsIgnoreCase(bs.getStatus()))
                .filter(bs -> !processedSeatIds.contains(bs.getSeat().getId()))
                .map(BookingSeat::getId)
                .collect(Collectors.toList());
        if (bookingSeatIds.isEmpty()) {
            return null; // không còn gì để hủy
        }

        boolean hasCustomer = booking.getCustomer() != null && booking.getCustomer().getUser() != null;
        // Khách có tài khoản → đền nguyên vé bằng voucher COMP_TICKET_FULL (nếu đã seed);
        // khách vãng lai → NONE (applyCompensation tự bỏ qua voucher, chỉ ghi vết).
        CompensationRequest comp = (hasCustomer && promotionTemplateId != null)
                ? new CompensationRequest("GIFT_TICKET", promotionTemplateId, reason)
                : new CompensationRequest("NONE", null, reason);

        Staff handledBy = handledByStaffId != null
                ? staffRepository.findById(handledByStaffId).orElse(null) : null;

        // performCancel() đã xử lý BUG-08 — cập nhật Booking.status = CANCELLED khi toàn bộ ghế bị hủy.
        // Với EMERGENCY_CLOSURE luôn hủy toàn bộ ghế còn SOLD → Booking sẽ luôn được set CANCELLED.
        IncidentResultResponse res = performCancel(booking, bookingSeatIds, comp, reason, handledBy, INCIDENT_EMERGENCY);

        // Dựng dữ liệu email PHẲNG ngay trong transaction (mọi lazy access còn trong session).
        if (!hasCustomer) return null; // không tài khoản → không có email để gửi
        User user = booking.getCustomer().getUser();
        if (user.getEmail() == null || user.getEmail().isBlank()) return null;

        Showtime st = booking.getShowtime();
        Room room = st.getRoom();
        Cinema cinema = room != null ? room.getCinema() : null;
        // Tái dùng allBookingSeats đã nạp ở trên → tránh query thứ 2 chỉ để lấy seat label
        java.util.Set<Integer> cancelledIds = new java.util.HashSet<>(bookingSeatIds);
        List<String> seatLabels = allBookingSeats.stream()
                .filter(bs -> cancelledIds.contains(bs.getId()))
                .map(bs -> bs.getSeat().displayLabel())
                .collect(Collectors.toList());

        IncidentResultResponse.CompensationResult comp2 = res.compensation();
        return new com.devcine.backend.dto.CancellationEmailData(
                user.getEmail(),
                user.getFullName(),
                booking.getBookingCode(),
                st.getMovie() != null ? st.getMovie().getTitle() : "Phim",
                cinema != null ? cinema.getName() : "",
                room != null ? room.getName() : "",
                st.getStartTime(),
                seatLabels,
                comp2 != null && comp2.voucherIssued(),
                comp2 != null ? comp2.voucherCode() : null,
                voucherLabel,
                reason);
    }

    // ===================== LỊCH SỬ =====================

    @Transactional(readOnly = true)
    public Page<IncidentListItem> history(String type, String bookingCode,
                                          LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Integer cinemaId = resolveCinemaScope();
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now().plusYears(1);
        return incidentRepository.search(cinemaId,
                        type != null ? type : "", bookingCode != null ? bookingCode : "", f, t, pageable)
                .map(IncidentListItem::from);
    }

    @Transactional(readOnly = true)
    public IncidentListItem detail(Integer id) {
        SeatIncident si = incidentRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự cố."));
        // Cách ly cụm rạp: không xem sự cố của cơ sở khác
        SecurityUtils.assertCinemaAccess(si.getCinema() != null ? si.getCinema().getId() : null);
        return IncidentListItem.from(si);
    }

    // ===================== HELPER =====================

    /**
     * Áp dụng đền bù theo cây quyết định (client không tự quyết).
     * @param overrideValue trị giá đền quy tiền ép sẵn (dùng cho HỦY = giá vé); null → suy từ template.
     * @param allowCancelOnly cho phép dùng template GIFT_TICKET (đền nguyên vé) — chỉ true ở luồng hủy.
     */
    private IncidentResultResponse.CompensationResult applyCompensation(
            Booking booking, CompensationRequest c, BigDecimal overrideValue, boolean allowCancelOnly) {
        if (c == null || c.type() == null || "NONE".equalsIgnoreCase(c.type())) {
            return IncidentResultResponse.CompensationResult.builder()
                    .type("NONE").voucherIssued(false).counterGift(false).value(BigDecimal.ZERO).build();
        }
        boolean hasCustomer = booking.getCustomer() != null && booking.getCustomer().getUser() != null;

        // Khách vãng lai (không tài khoản) → đền trực tiếp tại quầy, KHÔNG sinh Voucher (Edge #4a)
        if (!hasCustomer) {
            return IncidentResultResponse.CompensationResult.builder()
                    .type(c.type().toUpperCase()).voucherIssued(false).counterGift(true)
                    .value(BigDecimal.ZERO).build();
        }

        if (c.promotionTemplateId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại voucher đền bù.");
        }
        Promotion promo = promotionRepository.findById(c.promotionTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Mẫu đền bù không tồn tại."));
        if (promo.getCode() == null || !promo.getCode().startsWith(PREFIX_COMP)) {
            throw new IllegalArgumentException("Chỉ được dùng mẫu voucher đền bù (COMP_*).");
        }
        String type = compTypeOf(promo);
        if ("GIFT_TICKET".equals(type) && !allowCancelOnly) {
            throw new IllegalArgumentException("Mẫu đền nguyên vé chỉ dùng cho luồng hủy chỗ.");
        }

        // BUG-03 FIX: snapshot giá trị tại thời điểm phát để tránh thay đổi khi Admin sửa Promotion sau này
        Voucher voucher = Voucher.builder()
                .promotion(promo)
                .customer(booking.getCustomer())
                .isUsed(false)
                .validUntil(LocalDateTime.now().plusDays(90))
                .build();
        voucher.snapshotFrom(promo);
        voucher = voucherRepository.save(voucher);

        BigDecimal value = overrideValue != null ? overrideValue
                : ("DISCOUNT".equals(type) && promo.getDiscountValue() != null ? promo.getDiscountValue() : BigDecimal.ZERO);
        return IncidentResultResponse.CompensationResult.builder()
                // BUG-05/BUG-01 FIX: trả voucherId chính xác để attachCompensation() không phải query lại
                .type(type).voucherIssued(true).voucherId(voucher.getId()).voucherCode(promo.getCode())
                .counterGift(false).value(value).build();
    }

    private void attachCompensation(SeatIncident si, IncidentResultResponse.CompensationResult comp) {
        si.setCompensationType(comp.type());
        si.setCompensationAmount(comp.value() != null ? comp.value() : BigDecimal.ZERO);
        // BUG-05/BUG-01 FIX: dùng voucherId (ID chính xác) thay vì query lại theo code
        // — tránh match nhầm voucher cũ cùng promotion khi khách đã từng nhận cùng loại đền bù trước đó.
        if (comp.voucherIssued() && comp.voucherId() != null) {
            voucherRepository.findById(comp.voucherId()).ifPresent(si::setVoucher);
        }
    }

    /** Suy loại đền bù từ discount_type của template. */
    private String compTypeOf(Promotion p) {
        String dt = p.getDiscountType() != null ? p.getDiscountType().toUpperCase() : "";
        return switch (dt) {
            case "GIFT_FNB" -> "GIFT_FNB";
            case "GIFT_TICKET" -> "GIFT_TICKET";
            default -> "DISCOUNT"; // FIXED_AMOUNT / PERCENTAGE
        };
    }

    private boolean isDowngrade(Seat oldSeat, Seat newSeat) {
        int o = SEAT_RANK.getOrDefault(typeName(oldSeat), 0);
        int n = SEAT_RANK.getOrDefault(typeName(newSeat), 0);
        return n < o;
    }

    private String typeName(Seat s) {
        return s.getSeatType() != null && s.getSeatType().getName() != null
                ? s.getSeatType().getName().toUpperCase() : "NORMAL";
    }

    private Booking loadConfirmedBooking(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn."));
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Chỉ xử lý được đơn đã xác nhận (CONFIRMED).");
        }
        if (booking.getShowtime() == null) {
            throw new IllegalArgumentException("Đơn không gắn suất chiếu hợp lệ.");
        }
        return booking;
    }

    private Staff currentStaffOrNull() {
        Integer uid = SecurityUtils.getCurrentUserId();
        return uid == null ? null : staffRepository.findById(uid).orElse(null);
    }

    private Cinema cinemaOf(Booking booking) {
        Showtime s = booking.getShowtime();
        return s != null && s.getRoom() != null ? s.getRoom().getCinema() : null;
    }

    private Integer cinemaIdOf(Booking booking) {
        Cinema c = cinemaOf(booking);
        return c != null ? c.getId() : null;
    }

    /**
     * Phạm vi cơ sở cho lịch sử: ADMIN = null (toàn hệ thống); STAFF/MANAGER = cinemaId của mình,
     * thiếu cơ sở thì fail-closed (403) thay vì mở toàn hệ thống.
     */
    private Integer resolveCinemaScope() {
        if (SecurityUtils.isAdmin()) return null;
        Integer cinemaId = SecurityUtils.getCurrentUserCinemaId();
        if (cinemaId == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Tài khoản chưa gắn cơ sở nên không thể xem lịch sử sự cố.");
        }
        return cinemaId;
    }

    /**
     * Chặn cứng thao tác đổi ghế/hủy chỗ nếu suất đã kết thúc quá {@link #INCIDENT_WINDOW_HOURS} giờ.
     * EMERGENCY_CLOSURE được miễn vì hệ thống tự gọi không qua guard này.
     */
    private void assertWithinIncidentWindow(Showtime st) {
        if (st == null || st.getEndTime() == null) return; // không có endTime → bỏ qua (an toàn mặc định)
        LocalDateTime deadline = st.getEndTime().plusHours(INCIDENT_WINDOW_HOURS);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new IllegalArgumentException(
                    "Suất chiếu đã kết thúc từ lúc "
                    + st.getEndTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                    + " — hết thời gian xử lý sự cố (phải xử lý trong vòng "
                    + INCIDENT_WINDOW_HOURS + " giờ sau khi chiếu xong).");
        }
    }

    // ===================== REDIS LOCK & STOMP HELPERS =====================

    /**
     * Khóa phân tán (Redis Distributed Lock) theo từng ghế đích trước khi thực hiện đổi ghế.
     * Đảm bảo chống race condition 100% giữa nhiều server/instance và giữa các quầy/khách online.
     */
    private List<String> acquireRedisLocks(Integer showtimeId, List<Integer> seatIds) {
        if (seatIds == null || seatIds.isEmpty() || redisTemplate == null) return List.of();
        List<String> acquiredKeys = new ArrayList<>();
        String lockValue = "INCIDENT:" + System.currentTimeMillis() + ":" + java.util.UUID.randomUUID();
        try {
            for (Integer seatId : seatIds) {
                String key = REDIS_LOCK_PREFIX + showtimeId + ":seat:" + seatId;
                Boolean success = redisTemplate.opsForValue().setIfAbsent(key, lockValue, REDIS_LOCK_TTL);
                if (Boolean.TRUE.equals(success)) {
                    acquiredKeys.add(key);
                } else {
                    // Không lấy được lock -> rollback các lock đã lấy trong đợt này
                    releaseRedisLockKeys(acquiredKeys);
                    throw new IllegalStateException("Ghế #" + seatId + " đang được xử lý đồng thời bởi giao dịch khác. Vui lòng thử lại sau.");
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[SeatIncident] Không thể kết nối Redis để tạo lock (fallback to DB lock): {}", e.getMessage());
        }
        return acquiredKeys;
    }

    /** Giải phóng các Redis locks đã acquire. */
    private void releaseRedisLockKeys(List<String> keys) {
        if (keys == null || keys.isEmpty() || redisTemplate == null) return;
        try {
            redisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("[SeatIncident] Lỗi giải phóng Redis lock: {}", e.getMessage());
        }
    }

    /** Broadcast sự kiện trạng thái ghế qua WebSocket STOMP tới topic /topic/showtime/{id}. */
    private void broadcastSeatEvent(Integer showtimeId, String type, List<Integer> seatIds) {
        if (showtimeId == null || seatIds == null || seatIds.isEmpty() || messagingTemplate == null) return;
        try {
            Object payload = Map.of("type", type, "seatIds", seatIds, "by", "INCIDENT_HANDLER");
            messagingTemplate.convertAndSend("/topic/showtime/" + showtimeId, payload);
            log.info("[SeatIncident] Broadcast STOMP {} cho suất #{}: ghế {}", type, showtimeId, seatIds);
        } catch (Exception e) {
            log.warn("[SeatIncident] Broadcast STOMP {} cho suất #{} thất bại: {}", type, showtimeId, e.getMessage());
        }
    }
}
