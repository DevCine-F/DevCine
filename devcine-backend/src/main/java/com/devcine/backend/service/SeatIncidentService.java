package com.devcine.backend.service;

import com.devcine.backend.dto.request.CancelSeatRequest;
import com.devcine.backend.dto.request.CompensationRequest;
import com.devcine.backend.dto.request.RelocateRequest;
import com.devcine.backend.dto.request.SeatPhysicalStatusRequest;
import com.devcine.backend.dto.response.*;
import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 *   <li>Không hoàn tiền — đền bằng Voucher (từ Promotion-template "COMP_*") lưu trực tiếp vào SĐT khách
 *       (tự tạo hồ sơ Customer nếu khách vãng lai) hoặc đền quà trực tiếp tại quầy.</li>
 *   <li>Mọi ghi đều qua {@link SecurityUtils#assertCinemaAccess} → chặn thao tác chéo cụm rạp (403).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatIncidentService {

    private static final String PREFIX_COMP = "COMP_";
    /** Xếp hạng loại ghế để suy "hạ hạng vật lý" (chỉ gợi ý đền bù, không enforce). */
    private static final Map<String, Integer> SEAT_RANK = Map.of("NORMAL", 0, "VIP", 1, "SWEETBOX", 2);

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final SeatIncidentRepository incidentRepository;
    private final PromotionRepository promotionRepository;
    private final VoucherRepository voucherRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SeatLockService seatLockService;
    private final TicketService ticketService;

    // ===================== TRA CỨU =====================

    /** Tra vé theo Mã đặt vé hoặc SĐT khách (auto-detect). */
    @Transactional(readOnly = true)
    public IncidentBookingContext lookup(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã vé hoặc số điện thoại.");
        }
        String q = query.trim();
        Booking booking;
        if (q.matches("\\d{9,11}")) { // SĐT Việt Nam 9–11 chữ số
            List<Booking> found = bookingRepository.findConfirmedByCustomerPhone(q, PageRequest.of(0, 1));
            if (found.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy đơn đã xác nhận cho số điện thoại này.");
            }
            booking = found.get(0);
        } else {
            booking = bookingRepository.findByBookingCodeForPrint(q)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn với mã: " + q));
            if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
                throw new IllegalArgumentException("Đơn chưa thanh toán hoặc không hợp lệ để xử lý.");
            }
        }
        SecurityUtils.assertCinemaAccess(cinemaIdOf(booking));
        return buildContext(booking);
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

        IncidentBookingContext.ShowtimeBrief brief = IncidentBookingContext.ShowtimeBrief.builder()
                .showtimeId(st != null ? st.getId() : null)
                .movieTitle(st != null && st.getMovie() != null ? st.getMovie().getTitle() : null)
                .roomName(room != null ? room.getName() : null)
                .formatName(st != null && st.getFormat() != null ? st.getFormat().getName() : null)
                .startTime(st != null ? st.getStartTime() : null)
                .cinemaId(cinema != null ? cinema.getId() : null)
                .cinemaName(cinema != null ? cinema.getName() : null)
                .started(st != null && st.getStartTime() != null && st.getStartTime().isBefore(LocalDateTime.now()))
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

    // ===================== KHÓA GHẾ VẬT LÝ & CẢNH BÁO XUNG ĐỘT =====================

    /**
     * Tìm danh sách các đơn vé ở suất chiếu tương lai bị ảnh hưởng khi một vị trí ghế bị khóa bảo trì (Chain Lock).
     */
    @Transactional(readOnly = true)
    public List<FutureSeatConflictDTO> findConflictingFutureBookings(Integer seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế."));
        Cinema cinema = seat.getRoom() != null ? seat.getRoom().getCinema() : null;
        SecurityUtils.assertCinemaAccess(cinema != null ? cinema.getId() : null);

        List<BookingSeat> conflicts = bookingSeatRepository.findFutureBookingsBySeat(seatId, LocalDateTime.now());
        return conflicts.stream().map(bs -> {
            Booking b = bs.getBooking();
            Showtime st = b.getShowtime();
            User u = (b.getCustomer() != null && b.getCustomer().getUser() != null) ? b.getCustomer().getUser() : null;
            return FutureSeatConflictDTO.builder()
                    .bookingId(b.getId())
                    .bookingCode(b.getBookingCode())
                    .showtimeId(st != null ? st.getId() : null)
                    .movieTitle(st != null && st.getMovie() != null ? st.getMovie().getTitle() : "Phim")
                    .roomName(st != null && st.getRoom() != null ? st.getRoom().getName() : "")
                    .startTime(st != null ? st.getStartTime() : null)
                    .seatLabel(bs.getSeat().displayLabel())
                    .customerName(u != null ? u.getFullName() : "Khách vãng lai")
                    .customerPhone(u != null ? u.getPhone() : "")
                    .build();
        }).collect(Collectors.toList());
    }

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
        return SeatPhysicalStatusResponse.builder()
                .seatId(seat.getId()).seatLabel(seat.displayLabel()).status(status).incidentId(incidentId)
                .build();
    }

    // ===================== ĐỔI GHẾ =====================

    @Transactional
    public IncidentResultResponse relocate(RelocateRequest req) {
        Booking booking = loadConfirmedBooking(req.bookingId());
        Cinema cinema = cinemaOf(booking);
        SecurityUtils.assertCinemaAccess(cinema != null ? cinema.getId() : null);
        Showtime st = booking.getShowtime();

        // Ghế nguồn phải thuộc đơn & đang SOLD
        Map<Integer, BookingSeat> soldBySeatId = bookingSeatRepository.findAllByBookingIdWithSeat(booking.getId())
                .stream().filter(bs -> "SOLD".equalsIgnoreCase(bs.getStatus()))
                .collect(Collectors.toMap(bs -> bs.getSeat().getId(), bs -> bs, (a, b) -> a));

        List<Integer> newSeatIds = req.swaps().stream().map(RelocateRequest.SeatSwap::newSeatId).toList();
        List<Integer> oldSeatIds = req.swaps().stream().map(RelocateRequest.SeatSwap::oldSeatId).toList();
        if (newSeatIds.stream().distinct().count() != newSeatIds.size()) {
            throw new IllegalArgumentException("Không thể đổi nhiều ghế về cùng một vị trí đích.");
        }

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

        // Broadcast real-time qua WebSocket: ghế đích SOLD, ghế nguồn RELEASED
        seatLockService.markSold(st.getId(), newSeatIds);
        seatLockService.broadcastReleased(st.getId(), oldSeatIds);

        // Đền bù ÁP DỤNG MỘT LẦN cho cả lần xử lý → gắn vào dòng ghi vết đầu tiên (tránh cộng trùng trị giá)
        IncidentResultResponse.CompensationResult comp = applyCompensation(booking, req.compensation(), null, false);
        attachCompensation(toSave.get(0), comp);

        List<SeatIncident> saved = incidentRepository.saveAll(toSave);
        List<Integer> incidentIds = saved.stream().map(SeatIncident::getId).toList();

        boolean emailResent = ticketService.resendTicketEmailIfOnline(booking.getId());
        return IncidentResultResponse.builder()
                .incidentIds(incidentIds).swaps(swapResults).compensation(comp)
                .reprint(ticketService.buildPrintData(booking.getId()))
                .emailResent(emailResent)
                .build();
    }

    // ===================== HỦY CHỖ =====================

    @Transactional
    public IncidentResultResponse cancel(CancelSeatRequest req) {
        Booking booking = loadConfirmedBooking(req.bookingId());
        Cinema cinema = cinemaOf(booking);
        SecurityUtils.assertCinemaAccess(cinema != null ? cinema.getId() : null);
        // Thao tác quầy tương tác: người xử lý = nhân viên/quản lý đang đăng nhập; ghi vết loại "CANCEL".
        return performCancel(booking, req.bookingSeatIds(), req.compensation(), req.reason(),
                currentStaffOrNull(), "CANCEL");
    }

    /**
     * LÕI HỦY CHỖ dùng chung — KHÔNG chạm SecurityContext (an toàn khi gọi từ thread @Async không
     * có ngữ cảnh bảo mật). Cả luồng quầy tương tác {@link #cancel} lẫn luồng đóng cửa đột xuất
     * {@link #cancelBookingForEmergency} đều tái sử dụng để tránh nhân đôi logic đền bù/ghi vết.
     */
    private IncidentResultResponse performCancel(Booking booking, List<Integer> bookingSeatIds,
                                                 CompensationRequest comp, String reason,
                                                 Staff handledBy, String incidentType) {
        Cinema cinema = cinemaOf(booking);
        Showtime st = booking.getShowtime();

        Map<Integer, BookingSeat> byId = bookingSeatRepository.findAllByBookingIdWithSeat(booking.getId())
                .stream().collect(Collectors.toMap(BookingSeat::getId, bs -> bs, (a, b) -> a));

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

        // Broadcast real-time qua WebSocket: ghế hủy RELEASED
        if (st != null) {
            seatLockService.broadcastReleased(st.getId(), releasedSeatIds);
        }

        // Hủy chỗ → đền bằng trị giá đúng giá vé đã mua; cho phép template GIFT_TICKET (đền nguyên vé)
        IncidentResultResponse.CompensationResult compResult = applyCompensation(booking, comp, totalValue, true);
        attachCompensation(toSave.get(0), compResult);

        List<SeatIncident> saved = incidentRepository.saveAll(toSave);
        return IncidentResultResponse.builder()
                .incidentIds(saved.stream().map(SeatIncident::getId).toList())
                .swaps(List.of())
                .compensation(compResult)
                .reprint(null)          // ghế đã hủy → không in vé mới cho khách
                .emailResent(false)
                .build();
    }

    // ===================== ĐÓNG CỬA CỤM RẠP ĐỘT XUẤT =====================

    private static final String INCIDENT_EMERGENCY = "EMERGENCY_CLOSURE";

    /**
     * Hủy + đền bù TOÀN BỘ ghế của MỘT đơn khi cụm rạp đóng cửa đột xuất.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public com.devcine.backend.dto.CancellationEmailData cancelBookingForEmergency(
            Integer bookingId, Integer promotionTemplateId, String voucherLabel,
            Integer handledByStaffId, String reason) {

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || !"CONFIRMED".equalsIgnoreCase(booking.getStatus()) || booking.getShowtime() == null) {
            return null; // đã bị xử lý bởi luồng khác / dữ liệu không hợp lệ → bỏ qua
        }

        java.util.Set<Integer> processedSeatIds = incidentRepository.findProcessedSeatIdsByBooking(bookingId);
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
        CompensationRequest comp = (hasCustomer && promotionTemplateId != null)
                ? new CompensationRequest("GIFT_TICKET", promotionTemplateId, reason)
                : new CompensationRequest("NONE", null, reason);

        Staff handledBy = handledByStaffId != null
                ? staffRepository.findById(handledByStaffId).orElse(null) : null;

        IncidentResultResponse res = performCancel(booking, bookingSeatIds, comp, reason, handledBy, INCIDENT_EMERGENCY);

        if (!hasCustomer) return null; // không tài khoản → không có email để gửi
        User user = booking.getCustomer().getUser();
        if (user.getEmail() == null || user.getEmail().isBlank()) return null;

        Showtime st = booking.getShowtime();
        Room room = st.getRoom();
        Cinema cinema = room != null ? room.getCinema() : null;
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

    // ===================== LỊCH SỬ & XUẤT DỮ LIỆU =====================

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

    /**
     * Xuất danh sách sự cố ra file CSV chuẩn UTF-8 phục vụ đối soát Kế toán & Quản lý rạp.
     */
    @Transactional(readOnly = true)
    public byte[] exportHistoryCsv(String type, String bookingCode, LocalDateTime from, LocalDateTime to) {
        Integer cinemaId = resolveCinemaScope();
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now().plusYears(1);
        List<SeatIncident> list = incidentRepository.search(cinemaId,
                        type != null ? type : "", bookingCode != null ? bookingCode : "", f, t, PageRequest.of(0, 10000))
                .getContent();

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM để Excel hiển thị đúng tiếng Việt có dấu
        sb.append('\ufeff');
        sb.append("Mã sự cố,Thời gian,Loại sự cố,Mã đặt vé,Ghế nguồn,Ghế đích,Hình thức đền bù,Trị giá đền bù,Mã Voucher,Người xử lý,Cơ sở,Lý do\n");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        for (SeatIncident si : list) {
            String incidentCode = "INC-" + (si.getCreatedAt() != null ? si.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : "2026")
                    + "-" + String.format("%04d", si.getId());
            String time = si.getCreatedAt() != null ? si.getCreatedAt().format(dtf) : "";
            String incType = typeNameLabel(si.getIncidentType());
            String bCode = si.getBooking() != null ? si.getBooking().getBookingCode() : "";
            String oldSeat = si.getOldSeatLabel() != null ? si.getOldSeatLabel() : "";
            String newSeat = si.getNewSeatLabel() != null ? si.getNewSeatLabel() : "";
            String compType = compLabel(si.getCompensationType());
            String compAmt = si.getCompensationAmount() != null ? si.getCompensationAmount().toPlainString() : "0";
            String vCode = (si.getVoucher() != null && si.getVoucher().getPromotion() != null) ? si.getVoucher().getPromotion().getCode() : "";
            String staff = (si.getHandledBy() != null && si.getHandledBy().getUser() != null) ? si.getHandledBy().getUser().getFullName() : "";
            String cinema = si.getCinema() != null ? si.getCinema().getName() : "";
            String reason = si.getReason() != null ? si.getReason().replace("\"", "\"\"") : "";

            sb.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    incidentCode, time, incType, bCode, oldSeat, newSeat, compType, compAmt, vCode, staff, cinema, reason));
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String typeNameLabel(String t) {
        if (t == null) return "";
        return switch (t.toUpperCase()) {
            case "RELOCATE" -> "Đổi ghế";
            case "CANCEL" -> "Hủy chỗ";
            case "SEAT_MAINTENANCE" -> "Khóa bảo trì";
            case "EMERGENCY_CLOSURE" -> "Đóng cửa khẩn cấp";
            default -> t;
        };
    }

    private String compLabel(String c) {
        if (c == null) return "Không đền bù";
        return switch (c.toUpperCase()) {
            case "DISCOUNT" -> "Voucher giảm giá";
            case "GIFT_FNB" -> "Quà bắp nước F&B";
            case "GIFT_TICKET" -> "Vé mời xem phim";
            default -> "Không đền bù";
        };
    }

    // ===================== HELPER =====================

    /**
     * Áp dụng đền bù theo cây quyết định (client không tự quyết).
     * Hỗ trợ lưu Voucher theo SĐT cho khách vãng lai nếu nhập SĐT.
     */
    private IncidentResultResponse.CompensationResult applyCompensation(
            Booking booking, CompensationRequest c, BigDecimal overrideValue, boolean allowCancelOnly) {
        if (c == null || c.type() == null || "NONE".equalsIgnoreCase(c.type())) {
            return IncidentResultResponse.CompensationResult.builder()
                    .type("NONE").voucherIssued(false).counterGift(false).value(BigDecimal.ZERO).build();
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

        // Tìm hoặc khởi tạo Customer nhận voucher (theo tài khoản đơn hoặc SĐT cung cấp)
        Customer targetCustomer = null;
        if (booking.getCustomer() != null && booking.getCustomer().getUser() != null) {
            targetCustomer = booking.getCustomer();
        }

        String phone = (c.customerPhone() != null && !c.customerPhone().isBlank())
                ? c.customerPhone().trim()
                : (targetCustomer != null && targetCustomer.getUser() != null ? targetCustomer.getUser().getPhone() : null);

        if (targetCustomer == null && phone != null && phone.matches("\\d{9,11}")) {
            List<Customer> byPhone = customerRepository.findByUserPhone(phone);
            if (!byPhone.isEmpty()) {
                targetCustomer = byPhone.get(0);
            } else {
                User existingUser = userRepository.findByLoginIdentifier(phone).stream().findFirst().orElse(null);
                if (existingUser != null) {
                    targetCustomer = customerRepository.save(Customer.builder()
                            .user(existingUser)
                            .membershipTier("BRONZE")
                            .loyaltyPoints(0)
                            .build());
                } else {
                    Role customerRole = roleRepository.findByName("CUSTOMER")
                            .orElseGet(() -> roleRepository.save(Role.builder().name("CUSTOMER").build()));
                    User newUser = userRepository.save(User.builder()
                            .username(phone)
                            .phone(phone)
                            .fullName("Khách " + phone)
                            .role(customerRole)
                            .isActive(true)
                            .createdAt(LocalDateTime.now())
                            .build());
                    targetCustomer = customerRepository.save(Customer.builder()
                            .user(newUser)
                            .membershipTier("BRONZE")
                            .loyaltyPoints(0)
                            .build());
                }
            }
        }

        if (targetCustomer == null) {
            // Khách vãng lai không để lại SĐT -> đền quà trực tiếp tại quầy
            return IncidentResultResponse.CompensationResult.builder()
                    .type(type).voucherIssued(false).counterGift(true)
                    .value(BigDecimal.ZERO).build();
        }

        Voucher voucher = voucherRepository.save(Voucher.builder()
                .promotion(promo)
                .customer(targetCustomer)
                .isUsed(false)
                .validUntil(LocalDateTime.now().plusDays(90))
                .build());

        BigDecimal value = overrideValue != null ? overrideValue
                : ("DISCOUNT".equals(type) && promo.getDiscountValue() != null ? promo.getDiscountValue() : BigDecimal.ZERO);
        return IncidentResultResponse.CompensationResult.builder()
                .type(type).voucherIssued(true).voucherCode(promo.getCode())
                .counterGift(false).value(value).build();
    }

    private void attachCompensation(SeatIncident si, IncidentResultResponse.CompensationResult comp) {
        si.setCompensationType(comp.type());
        si.setCompensationAmount(comp.value() != null ? comp.value() : BigDecimal.ZERO);
        if (comp.voucherIssued() && comp.voucherCode() != null) {
            // Gán voucher mới nhất vừa tạo
            voucherRepository.findAll().stream()
                    .filter(v -> v.getPromotion() != null && comp.voucherCode().equals(v.getPromotion().getCode()) && !Boolean.TRUE.equals(v.getIsUsed()))
                    .max(java.util.Comparator.comparing(Voucher::getId))
                    .ifPresent(si::setVoucher);
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
}
