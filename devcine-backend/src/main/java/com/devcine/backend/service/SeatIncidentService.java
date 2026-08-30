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
import com.devcine.backend.event.SeatRelocatedEvent;
import com.devcine.backend.repository.*;
import com.devcine.backend.util.SecurityUtils;
import com.devcine.backend.validator.OrphanSeatValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Xử lý sự cố phòng chiếu tại quầy: đổi ghế đền bù, hủy chỗ, khóa ghế bảo trì, và ghi vết.
 *
 * <p>Chốt kiến trúc (xem CLAUDE.md / memory devcine-permission-architecture):
 * <ul>
 *   <li>Đổi ghế = REPOINT {@code BookingSeat.seat_id} TẠI CHỖ kèm cập nhật phiên bản vé/QR code mới
 *       → chống gian lận dùng vé giấy cũ và đảm bảo an ninh phòng chiếu.</li>
 *   <li>Chống trùng ghế 100%: Redis Distributed Lock + DB Pessimistic Write Lock (Showtime & Booking).</li>
 *   <li>Realtime sơ đồ ghế: WebSocket STOMP broadcast tới {@code /topic/showtime/{id}}.</li>
 *   <li>Không hoàn tiền — đền bằng Voucher (từ Promotion-template "COMP_*") hoặc đền trực tiếp tại
 *       quầy cho khách vãng lai (sinh audit_gift_code đối soát).</li>
 *   <li>Phân quyền hạn mức đền bù (RBAC): Staff tối đa 50k; Manager/Admin phê duyệt vé mời và voucher lớn.</li>
 *   <li>Mọi ghi đều qua {@link SecurityUtils#assertCinemaAccess} → chặn thao tác chéo cụm rạp (403).</li>
 * </ul>
 */
@Slf4j
@Service
@org.springframework.context.annotation.Profile("never")
@RequiredArgsConstructor
public class SeatIncidentService {

    private static final String PREFIX_COMP = "COMP_";
    private static final BigDecimal STAFF_MAX_COMPENSATION = new BigDecimal("50000");
    private static final int STAFF_MAX_COMPENSATIONS_PER_WINDOW = 5;
    private static final int COMPENSATION_WINDOW_HOURS = 8;
    private static final Set<String> ALLOWED_COMPENSATION_TYPES =
            Set.of("NONE", "DISCOUNT", "GIFT_FNB", "GIFT_TICKET");

    /** Xếp hạng loại ghế để suy "hạ hạng vật lý" (được enforce bắt buộc đền bù). */
    private static final Map<String, Integer> SEAT_RANK = Map.of("NORMAL", 0, "VIP", 1, "SWEETBOX", 2);

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final SeatIncidentRepository incidentRepository;
    private final PromotionRepository promotionRepository;
    private final VoucherRepository voucherRepository;
    private final StaffRepository staffRepository;
    private final TicketRepository ticketRepository;
    private final TicketQrHistoryRepository ticketQrHistoryRepository;
    private final TicketService ticketService;
    private final MailService mailService;
    private final SeatLockService seatLockService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisSeatLockLeaseService redisSeatLockLeaseService;
    private final OrphanSeatValidator orphanSeatValidator;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TransactionTemplate transactionTemplate;

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

        LocalDateTime now = LocalDateTime.now();
        IncidentBookingContext.ShowtimeStatus showtimeStatus = resolveShowtimeStatus(
                st != null ? st.getStartTime() : null,
                st != null ? st.getEndTime() : null,
                now);
        boolean started = showtimeStatus != IncidentBookingContext.ShowtimeStatus.UPCOMING;
        boolean ended = showtimeStatus == IncidentBookingContext.ShowtimeStatus.ENDED
                || showtimeStatus == IncidentBookingContext.ShowtimeStatus.EXPIRED;
        boolean expired = showtimeStatus == IncidentBookingContext.ShowtimeStatus.EXPIRED;

        IncidentBookingContext.ShowtimeBrief brief = IncidentBookingContext.ShowtimeBrief.builder()
                .showtimeId(st != null ? st.getId() : null)
                .movieTitle(st != null && st.getMovie() != null ? st.getMovie().getTitle() : null)
                .roomName(room != null ? room.getName() : null)
                .formatName(st != null && st.getFormat() != null ? st.getFormat().getName() : null)
                .startTime(st != null ? st.getStartTime() : null)
                .endTime(st != null ? st.getEndTime() : null)
                .cinemaId(cinema != null ? cinema.getId() : null)
                .cinemaName(cinema != null ? cinema.getName() : null)
                .status(showtimeStatus)
                .started(started)
                .ended(ended)
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
                .map(p -> {
                    String type = compTypeOf(p);
                    BigDecimal val = p.getDiscountValue() != null ? p.getDiscountValue() : BigDecimal.ZERO;
                    boolean reqMgr = "GIFT_TICKET".equals(type) || val.compareTo(STAFF_MAX_COMPENSATION) > 0;
                    return CompensationOption.builder()
                            .promotionId(p.getId())
                            .code(p.getCode())
                            .label(p.getName() != null ? p.getName() : p.getCode())
                            .type(type)
                            .discountValue(val)
                            .cancelOnly("GIFT_TICKET".equals(type))
                            .requiresManager(reqMgr)
                            .build();
                })
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
            String eventType = "AVAILABLE".equals(status) ? "SEAT_RELEASED" : "SEAT_MAINTENANCE";
            for (Showtime activeSt : activeShowtimes) {
                broadcastSeatEvent(activeSt.getId(), eventType, List.of(seat.getId()), status);
            }
        }

        return SeatPhysicalStatusResponse.builder()
                .seatId(seat.getId()).seatLabel(seat.displayLabel()).status(status).incidentId(incidentId)
                .build();
    }

    // ===================== ĐỔI GHẾ =====================

    public IncidentResultResponse relocate(RelocateRequest req) {
        List<Integer> newSeatIds = req.swaps().stream().map(RelocateRequest.SeatSwap::newSeatId).toList();
        List<Integer> oldSeatIds = req.swaps().stream().map(RelocateRequest.SeatSwap::oldSeatId).toList();
        if (newSeatIds.stream().distinct().count() != newSeatIds.size()) {
            throw new IllegalArgumentException("Không thể đổi nhiều ghế về cùng một vị trí đích.");
        }
        if (oldSeatIds.stream().distinct().count() != oldSeatIds.size()) {
            throw new IllegalArgumentException("Một ghế nguồn chỉ được xuất hiện trong một cặp đổi.");
        }

        Booking booking = bookingRepository.findDetailById(req.bookingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn."));
        assertConfirmedBooking(booking);
        Cinema cinema = cinemaOf(booking);
        SecurityUtils.assertCinemaAccess(cinema != null ? cinema.getId() : null);
        Showtime st = booking.getShowtime();
        assertWithinIncidentWindow(st);
        assertRelocationAllowed(st);

        RelocateCommitResult committed;
        try (RedisSeatLockLeaseService.LockLease lease = redisSeatLockLeaseService.acquire(st.getId(), newSeatIds)) {
            committed = transactionTemplate.execute(status -> relocateInTransaction(req, lease));
        }
        if (committed == null) throw new IllegalStateException("Không thể hoàn tất transaction đổi ghế.");

        return IncidentResultResponse.builder()
                .incidentIds(committed.incidentIds())
                .swaps(committed.swaps())
                .compensation(committed.compensation())
                .reprint(ticketService.buildPrintData(req.bookingId()))
                .emailResent(committed.emailScheduled())
                .build();
    }

    private RelocateCommitResult relocateInTransaction(
            RelocateRequest req, RedisSeatLockLeaseService.LockLease lease) {
        Booking initialBooking = bookingRepository.findDetailById(req.bookingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn."));
        assertConfirmedBooking(initialBooking);
        Integer showtimeId = initialBooking.getShowtime().getId();

        showtimeRepository.findByIdForUpdate(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));
        bookingRepository.findByIdWithPessimisticLock(req.bookingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt vé."));

        Booking booking = bookingRepository.findDetailById(req.bookingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn."));
        assertConfirmedBooking(booking);
        Showtime st = booking.getShowtime();
        Cinema cinema = cinemaOf(booking);
        SecurityUtils.assertCinemaAccess(cinema != null ? cinema.getId() : null);
        assertWithinIncidentWindow(st);
        assertRelocationAllowed(st);

        List<Integer> newSeatIds = req.swaps().stream().map(RelocateRequest.SeatSwap::newSeatId).toList();
        List<Integer> oldSeatIds = req.swaps().stream().map(RelocateRequest.SeatSwap::oldSeatId).toList();

        boolean bypassOrphan = req.allowOrphan() && (SecurityUtils.isAdmin() || SecurityUtils.isManager());
        if (!bypassOrphan) {
            List<Seat> roomSeats = seatRepository.findByRoomIdAndIsActiveTrue(st.getRoom().getId());
            Set<Integer> currentOccupied = bookingSeatRepository.findReservedSeatsByShowtime(st.getId()).stream()
                    .map(bookingSeat -> bookingSeat.getSeat().getId())
                    .collect(Collectors.toSet());
            currentOccupied.removeAll(oldSeatIds);
            currentOccupied.addAll(newSeatIds);
            if (orphanSeatValidator.hasOrphanSeats(roomSeats, currentOccupied, newSeatIds)) {
                throw new IllegalArgumentException(
                        "Vị trí ghế đổi vi phạm quy định chống để lại ghế trống đơn lẻ (Ghế mồ côi). Vui lòng chọn vị trí khác.");
            }
        }

        List<Integer> transientLocks = seatLockService.lockedSeatIds(st.getId());
        for (Integer newId : newSeatIds) {
            if (transientLocks.contains(newId)) {
                throw new IllegalStateException("Ghế đích đang được giữ tạm bởi giao dịch khác. Vui lòng chọn ghế khác.");
            }
        }

        Map<Integer, BookingSeat> soldBySeatId = bookingSeatRepository.findAllByBookingIdWithSeat(booking.getId())
                .stream().filter(bookingSeat -> "SOLD".equalsIgnoreCase(bookingSeat.getStatus()))
                .collect(Collectors.toMap(bookingSeat -> bookingSeat.getSeat().getId(), bookingSeat -> bookingSeat, (a, b) -> a));

        List<BookingSeat> sourceBookingSeats = new ArrayList<>();
        for (Integer oldSeatId : oldSeatIds) {
            BookingSeat bookingSeat = soldBySeatId.get(oldSeatId);
            if (bookingSeat == null) {
                throw new IllegalArgumentException("Ghế nguồn không thuộc đơn hoặc đã được xử lý.");
            }
            sourceBookingSeats.add(bookingSeat);
        }

        List<Integer> conflicts = bookingSeatRepository.findConflictingSeats(st.getId(), newSeatIds, LocalDateTime.now());
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Ghế đích vừa bị chiếm bởi giao dịch khác. Vui lòng chọn ghế trống khác.");
        }

        Map<Integer, Seat> newSeats = seatRepository.findByIdInWithSeatType(newSeatIds).stream()
                .collect(Collectors.toMap(Seat::getId, seat -> seat, (a, b) -> a));
        Set<Integer> alreadyProcessedSeatIds = incidentRepository.findProcessedSeatIdsByBooking(booking.getId());
        Map<Integer, Ticket> ticketsByBookingSeatId = ticketRepository.findByBookingSeatIds(
                        sourceBookingSeats.stream().map(BookingSeat::getId).toList()).stream()
                .collect(Collectors.toMap(ticket -> ticket.getBookingSeat().getId(), ticket -> ticket, (a, b) -> a));
        Staff handledBy = currentStaffOrNull();

        List<IncidentResultResponse.SeatSwapResult> swapResults = new ArrayList<>();
        List<SeatIncident> toSave = new ArrayList<>();
        List<BookingSeat> updatedSeats = new ArrayList<>();
        List<Ticket> updatedTickets = new ArrayList<>();
        List<TicketQrHistory> revokedQrCodes = new ArrayList<>();
        List<Seat> oldSeatsToLock = new ArrayList<>();

        for (RelocateRequest.SeatSwap swap : req.swaps()) {
            BookingSeat bs = soldBySeatId.get(swap.oldSeatId());
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

            // VẤN ĐỀ 3 FIX: Kiểm tra tính tương thích loại ghế (SeatTypeCompatibilityValidator)
            validateSeatTypeCompatibility(oldSeat, newSeat, req.compensation());

            String oldLabel = oldSeat.displayLabel();
            String newLabel = newSeat.displayLabel();
            boolean downgrade = isDowngrade(oldSeat, newSeat);

            bs.setSeat(newSeat); // REPOINT tại chỗ
            updatedSeats.add(bs);

            Ticket ticket = ticketsByBookingSeatId.get(bs.getId());
            if (ticket != null) {
                int currentVersion = ticket.getVersion() != null ? ticket.getVersion() : 1;
                if (ticket.getQrCode() != null && !ticket.getQrCode().isBlank()) {
                    revokedQrCodes.add(TicketQrHistory.builder()
                            .ticket(ticket)
                            .qrCode(ticket.getQrCode())
                            .ticketVersion(currentVersion)
                            .revokedReason("Đổi ghế từ " + oldLabel + " sang " + newLabel)
                            .build());
                }
                int nextVer = currentVersion + 1;
                ticket.setVersion(nextVer);
                ticket.setQrCode("DEVCINE-T-" + bs.getId() + "-V" + nextVer + "-"
                        + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                ticket.setIsRevoked(false);
                updatedTickets.add(ticket);
            }

            // VẤN ĐỀ 1 FIX: Thu thập ghế cũ để khóa bảo trì nếu cờ lockOldSeatsAsMaintenance bật
            if (req.shouldLockOldSeats()) {
                oldSeat.setSeatStatus("MAINTENANCE");
                oldSeatsToLock.add(oldSeat);

                toSave.add(SeatIncident.builder()
                        .incidentType("SEAT_MAINTENANCE")
                        .booking(booking).showtime(st)
                        .oldSeat(oldSeat)
                        .oldSeatLabel(oldLabel)
                        .compensationType("NONE")
                        .reason("Tự động khóa bảo trì khi đổi ghế: " + (req.reason() != null ? req.reason() : ""))
                        .handledBy(handledBy)
                        .cinema(cinema)
                        .build());
            }

            toSave.add(SeatIncident.builder()
                    .incidentType("RELOCATE")
                    .booking(booking).showtime(st)
                    .oldSeat(oldSeat).newSeat(newSeat)
                    .oldSeatLabel(oldLabel).newSeatLabel(newLabel)
                    .compensationType("NONE")
                    .reason(req.reason())
                    .handledBy(handledBy).cinema(cinema)
                    .build());
            swapResults.add(IncidentResultResponse.SeatSwapResult.builder()
                    .oldLabel(oldLabel).newLabel(newLabel).downgrade(downgrade).build());
        }

        bookingSeatRepository.saveAll(updatedSeats);
        if (!revokedQrCodes.isEmpty()) ticketQrHistoryRepository.saveAll(revokedQrCodes);
        if (!updatedTickets.isEmpty()) ticketRepository.saveAll(updatedTickets);
        if (req.shouldLockOldSeats()) seatRepository.saveAll(oldSeatsToLock);

        IncidentResultResponse.CompensationResult comp = applyCompensation(
                booking, req.compensation(), null, false, handledBy,
                SecurityUtils.isAdmin() || SecurityUtils.isManager());
        // Gắn đền bù vào dòng ghi vết RELOCATE đầu tiên
        SeatIncident firstRelocateIncident = toSave.stream()
                .filter(si -> "RELOCATE".equals(si.getIncidentType()))
                .findFirst()
                .orElse(toSave.get(0));
        attachCompensation(firstRelocateIncident, comp);

        List<SeatIncident> saved = incidentRepository.saveAll(toSave);
        List<Integer> incidentIds = saved.stream().map(SeatIncident::getId).toList();

        String voucherLabel = null;
        if (req.compensation() != null && req.compensation().promotionTemplateId() != null) {
            voucherLabel = promotionRepository.findById(req.compensation().promotionTemplateId())
                    .map(Promotion::getName)
                    .orElse(null);
        }
        List<com.devcine.backend.dto.IncidentRelocateEmailData.SeatSwapLine> swapLines = swapResults.stream()
                .map(s -> new com.devcine.backend.dto.IncidentRelocateEmailData.SeatSwapLine(s.oldLabel(), s.newLabel()))
                .toList();

        // VẤN ĐỀ 5 FIX: Tách gửi email ra ngoài Transaction & Redis lock thông qua Domain Event
        applicationEventPublisher.publishEvent(new SeatRelocatedEvent(
                booking.getId(), st.getId(), oldSeatIds, newSeatIds, req.shouldLockOldSeats(),
                req.reason(), swapLines, comp, voucherLabel));

        boolean hasEmail = booking.getCustomer() != null && booking.getCustomer().getUser() != null
                && booking.getCustomer().getUser().getEmail() != null
                && !booking.getCustomer().getUser().getEmail().isBlank();
        boolean emailResent = hasEmail && !"POS".equalsIgnoreCase(booking.getChannel());

        lease.assertValid();
        return new RelocateCommitResult(incidentIds, swapResults, comp, emailResent);
    }

    private record RelocateCommitResult(
            List<Integer> incidentIds,
            List<IncidentResultResponse.SeatSwapResult> swaps,
            IncidentResultResponse.CompensationResult compensation,
            boolean emailScheduled
    ) {
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
        boolean privilegedCompensation = INCIDENT_EMERGENCY.equals(incidentType)
                || SecurityUtils.isAdmin() || SecurityUtils.isManager();
        IncidentResultResponse.CompensationResult compResult = applyCompensation(
                booking, comp, totalValue, true, handledBy, privilegedCompensation);
        attachCompensation(toSave.get(0), compResult);

        List<SeatIncident> saved = incidentRepository.saveAll(toSave);

        // REALTIME WEBSOCKET STOMP: Giải phóng các ghế đã hủy cho mọi quầy / khách online
        if (st != null && st.getId() != null && !releasedSeatIds.isEmpty()) {
            broadcastSeatEvent(st.getId(), "SEAT_RELEASED", releasedSeatIds);
        }

        boolean emailSent = false;
        if (booking.getCustomer() != null && booking.getCustomer().getUser() != null) {
            User user = booking.getCustomer().getUser();
            if (user.getEmail() != null && !user.getEmail().isBlank() && !"POS".equalsIgnoreCase(booking.getChannel())) {
                String voucherLabel = null;
                if (comp != null && comp.promotionTemplateId() != null) {
                    voucherLabel = promotionRepository.findById(comp.promotionTemplateId())
                            .map(Promotion::getName).orElse(null);
                }
                Room room = st != null ? st.getRoom() : null;
                List<String> cancelledLabels = allBookingSeats.stream()
                        .filter(bs -> bookingSeatIds.contains(bs.getId()))
                        .map(bs -> bs.getSeat().displayLabel())
                        .toList();
                mailService.sendCancellationEmail(new com.devcine.backend.dto.CancellationEmailData(
                        user.getEmail(),
                        user.getFullName(),
                        booking.getBookingCode(),
                        st != null && st.getMovie() != null ? st.getMovie().getTitle() : "Phim",
                        cinema != null ? cinema.getName() : "",
                        room != null ? room.getName() : "",
                        st != null ? st.getStartTime() : null,
                        cancelledLabels,
                        compResult != null && compResult.voucherIssued(),
                        compResult != null ? compResult.voucherCode() : null,
                        voucherLabel,
                        reason
                ));
                emailSent = true;
            }
        }

        return IncidentResultResponse.builder()
                .incidentIds(saved.stream().map(SeatIncident::getId).toList())
                .swaps(List.of())
                .compensation(compResult)
                .reprint(null)          // ghế đã hủy → không in vé mới cho khách
                .emailResent(emailSent)
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
     * VẤN ĐỀ 4 FIX: Kiểm tra hạn mức và quyền phê duyệt đền bù theo vai trò (RBAC).
     * Staff: tối đa 50.000đ, không được phát vé mời. Manager/Admin: toàn quyền.
     */
    private void checkCompensationPermission(CompensationRequest comp, BigDecimal totalValue,
                                             Staff handledBy, boolean privileged) {
        if (comp == null || comp.type() == null || "NONE".equalsIgnoreCase(comp.type())) return;

        String requestedType = comp.type().toUpperCase(Locale.ROOT);
        if (!ALLOWED_COMPENSATION_TYPES.contains(requestedType)) {
            throw new IllegalArgumentException("Hình thức đền bù không hợp lệ.");
        }

        if (!privileged && handledBy == null) {
            throw new AccessDeniedException("Không xác định được nhân viên xử lý đền bù.");
        }

        // 1. Kiểm tra quyền phát vé mời đền nguyên vé
        if ("GIFT_TICKET".equals(requestedType) && !privileged) {
            throw new AccessDeniedException("Chỉ Quản lý (Manager) mới có quyền phê duyệt đền bù bằng Vé mời nguyên giá.");
        }

        // 2. Kiểm tra hạn mức giá trị tiền đền bù của Staff
        if (!privileged && comp.promotionTemplateId() != null) {
            Promotion promo = promotionRepository.findById(comp.promotionTemplateId()).orElse(null);
            if (promo != null) {
                if ("GIFT_TICKET".equalsIgnoreCase(compTypeOf(promo))) {
                    throw new AccessDeniedException("Chỉ Quản lý (Manager) mới có quyền phê duyệt đền bù bằng Vé mời nguyên giá.");
                }
                if (promo.getDiscountValue() != null && promo.getDiscountValue().compareTo(STAFF_MAX_COMPENSATION) > 0) {
                    throw new AccessDeniedException(
                            String.format("Vượt hạn mức đền bù của Nhân viên (Tối đa %sđ). Vui lòng yêu cầu Quản lý thực hiện.", "50.000")
                    );
                }
            }
        }

        if (!privileged && totalValue != null && totalValue.compareTo(STAFF_MAX_COMPENSATION) > 0) {
            throw new AccessDeniedException(
                    String.format("Vượt hạn mức đền bù của Nhân viên (Tối đa %sđ). Vui lòng yêu cầu Quản lý thực hiện.", "50.000")
            );
        }

        if (!privileged) {
            long recentCompensations = incidentRepository.countCompensationsHandledSince(
                    handledBy.getUserId(), LocalDateTime.now().minusHours(COMPENSATION_WINDOW_HOURS));
            if (recentCompensations >= STAFF_MAX_COMPENSATIONS_PER_WINDOW) {
                throw new AccessDeniedException(
                        "Nhân viên đã đạt giới hạn 5 lần đền bù trong 8 giờ. Vui lòng yêu cầu Quản lý thực hiện.");
            }
        }
    }

    /**
     * VẤN ĐỀ 3 FIX: Bộ kiểm tra tương thích loại ghế (SeatTypeCompatibilityValidator).
     * 1. Ghế đơn (NORMAL, VIP) chỉ được đổi sang ghế đơn.
     * 2. Ghế đôi (SWEETBOX) bắt buộc phải đổi sang ghế đôi.
     * 3. Bắt buộc chọn đền bù thiện chí khi hạ hạng vật lý.
     */
    private void validateSeatTypeCompatibility(Seat oldSeat, Seat newSeat, CompensationRequest comp) {
        String oldType = typeName(oldSeat);
        String newType = typeName(newSeat);

        boolean oldIsCouple = "SWEETBOX".equals(oldType);
        boolean newIsCouple = "SWEETBOX".equals(newType);

        if (oldIsCouple != newIsCouple) {
            throw new IllegalArgumentException(
                    String.format("Không thể đổi giữa ghế đơn (%s) và ghế đôi (%s). Ghế đôi Sweetbox bắt buộc phải đổi sang ghế đôi khác.", oldType, newType)
            );
        }

        if (isDowngrade(oldSeat, newSeat)) {
            if (comp == null || comp.type() == null || "NONE".equalsIgnoreCase(comp.type())) {
                throw new IllegalArgumentException(
                        String.format("Khách hàng bị hạ hạng từ %s xuống %s. Bắt buộc phải chọn hình thức đền bù thiện chí (Voucher hoặc Quà F&B).", oldType, newType)
                );
            }
        }
    }

    /**
     * Áp dụng đền bù theo cây quyết định (client không tự quyết).
     * @param overrideValue trị giá đền quy tiền ép sẵn (dùng cho HỦY = giá vé); null → suy từ template.
     * @param allowCancelOnly cho phép dùng template GIFT_TICKET (đền nguyên vé) — chỉ true ở luồng hủy.
     */
    private IncidentResultResponse.CompensationResult applyCompensation(
            Booking booking, CompensationRequest c, BigDecimal overrideValue, boolean allowCancelOnly,
            Staff handledBy, boolean privileged) {
        if (c == null || c.type() == null || "NONE".equalsIgnoreCase(c.type())) {
            return IncidentResultResponse.CompensationResult.builder()
                    .type("NONE").voucherIssued(false).counterGift(false).value(BigDecimal.ZERO).build();
        }

        // VẤN ĐỀ 4 FIX: Kiểm tra quyền & hạn mức trước khi áp dụng đền bù
        checkCompensationPermission(c, overrideValue, handledBy, privileged);

        boolean hasCustomer = booking.getCustomer() != null && booking.getCustomer().getUser() != null;

        // Khách vãng lai (không tài khoản) → đền trực tiếp tại quầy, KHÔNG sinh Voucher (sinh auditGiftCode đối soát)
        if (!hasCustomer) {
            String auditGiftCode = "GIFT-" + booking.getBookingCode() + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            return IncidentResultResponse.CompensationResult.builder()
                    .type(c.type().toUpperCase())
                    .voucherIssued(false)
                    .counterGift(true)
                    .auditGiftCode(auditGiftCode)
                    .value(BigDecimal.ZERO)
                    .build();
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
        si.setAuditGiftCode(comp.auditGiftCode());
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
        assertConfirmedBooking(booking);
        return booking;
    }

    private void assertConfirmedBooking(Booking booking) {
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Chỉ xử lý được đơn đã xác nhận (CONFIRMED).");
        }
        if (booking.getShowtime() == null) {
            throw new IllegalArgumentException("Đơn không gắn suất chiếu hợp lệ.");
        }
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
        if (!LocalDateTime.now().isBefore(deadline)) {
            throw new IllegalArgumentException(
                    "Suất chiếu đã kết thúc từ lúc "
                    + st.getEndTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                    + " — hết thời gian xử lý sự cố (phải xử lý trong vòng "
                    + INCIDENT_WINDOW_HOURS + " giờ sau khi chiếu xong).");
        }
    }

    private void assertRelocationAllowed(Showtime st) {
        assertRelocationAllowedAt(st, LocalDateTime.now());
    }

    static void assertRelocationAllowedAt(Showtime st, LocalDateTime now) {
        IncidentBookingContext.ShowtimeStatus status = resolveShowtimeStatus(
                st != null ? st.getStartTime() : null,
                st != null ? st.getEndTime() : null,
                now);
        if (status == IncidentBookingContext.ShowtimeStatus.ENDED
                || status == IncidentBookingContext.ShowtimeStatus.EXPIRED) {
            throw new IllegalArgumentException("Suất chiếu đã kết thúc — không thể đổi ghế.");
        }
    }

    static IncidentBookingContext.ShowtimeStatus resolveShowtimeStatus(
            LocalDateTime startTime, LocalDateTime endTime, LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("Thiếu thời điểm kiểm tra trạng thái suất chiếu.");
        }
        if (endTime != null && !now.isBefore(endTime.plusHours(INCIDENT_WINDOW_HOURS))) {
            return IncidentBookingContext.ShowtimeStatus.EXPIRED;
        }
        if (endTime != null && !now.isBefore(endTime)) {
            return IncidentBookingContext.ShowtimeStatus.ENDED;
        }
        if (startTime != null && !now.isBefore(startTime)) {
            return IncidentBookingContext.ShowtimeStatus.IN_PROGRESS;
        }
        return IncidentBookingContext.ShowtimeStatus.UPCOMING;
    }

    // ===================== STOMP HELPER =====================

    /** Broadcast sự kiện trạng thái ghế qua WebSocket STOMP tới topic /topic/showtime/{id}. */
    private void broadcastSeatEvent(Integer showtimeId, String type, List<Integer> seatIds) {
        broadcastSeatEvent(showtimeId, type, seatIds, null);
    }

    private void broadcastSeatEvent(Integer showtimeId, String type, List<Integer> seatIds, String status) {
        if (showtimeId == null || seatIds == null || seatIds.isEmpty() || messagingTemplate == null) return;
        try {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("type", type);
            map.put("seatIds", seatIds);
            map.put("by", "INCIDENT_HANDLER");
            if (status != null) map.put("status", status);
            messagingTemplate.convertAndSend("/topic/showtime/" + showtimeId, (Object) map);
            log.info("[SeatIncident] Broadcast STOMP {} cho suất #{}: ghế {}", type, showtimeId, seatIds);
        } catch (Exception e) {
            log.warn("[SeatIncident] Broadcast STOMP {} cho suất #{} thất bại: {}", type, showtimeId, e.getMessage());
        }
    }
}
