package com.devcine.backend.service;

import com.devcine.backend.dto.TicketEmailData;
import com.devcine.backend.dto.response.BookingPrintResponse;
import com.devcine.backend.dto.response.TicketVerificationResponse;
import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.BookingFnb;
import com.devcine.backend.entity.BookingSeat;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.entity.Room;
import com.devcine.backend.entity.Seat;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.entity.Ticket;
import com.devcine.backend.entity.TicketQrHistory;
import com.devcine.backend.entity.User;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.repository.BookingFnbRepository;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.repository.TicketRepository;
import com.devcine.backend.repository.TicketQrHistoryRepository;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final TicketRepository ticketRepository;
    private final TicketQrHistoryRepository ticketQrHistoryRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingFnbRepository bookingFnbRepository;
    private final StaffRepository staffRepository;
    private final MailService mailService;

    /** Nhân viên (Staff) đang đăng nhập, hoặc null nếu là ADMIN không phải nhân sự quầy. */
    private Staff currentStaffOrNull() {
        Integer uid = SecurityUtils.getCurrentUserId();
        return uid == null ? null : staffRepository.findById(uid).orElse(null);
    }

    /** Cơ sở (rạp) của đơn: Booking → Showtime → Room → Cinema. */
    private Integer cinemaIdOf(Booking booking) {
        Showtime s = booking.getShowtime();
        return s != null && s.getRoom() != null && s.getRoom().getCinema() != null
                ? s.getRoom().getCinema().getId() : null;
    }

    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByBooking(Integer bookingId) {
        return ticketRepository.findAllByBookingId(bookingId);
    }

    /**
     * Quét/tra cứu mã đặt vé để XÁC MINH đơn (chưa in). Trả chi tiết đơn để hiển thị
     * "Quét thành công"; KHÔNG đánh dấu đã in. Đơn đã in trước đó → báo lỗi chống trùng.
     */
    @Transactional(readOnly = true)
    public BookingPrintResponse lookupByBookingCode(String bookingCode) {
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new RuntimeException("Vui lòng cung cấp mã đặt vé.");
        }
        Booking booking = bookingRepository.findByBookingCodeForPrint(bookingCode.trim())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt vé với mã: " + bookingCode));

        // Cách ly cụm rạp: chỉ soát/tra cứu vé của cơ sở mình.
        SecurityUtils.assertCinemaAccess(cinemaIdOf(booking));

        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Đơn chưa thanh toán hoặc không hợp lệ để in vé.");
        }
        if (booking.getPrintedAt() != null) {
            throw new RuntimeException("Mã đặt vé này đã được in thành vé giấy trước đó vào lúc: "
                    + booking.getPrintedAt().format(TIME_FMT));
        }
        return buildResponse(booking);
    }

    /**
     * Quét QR/nhập mã đặt vé tại quầy → in toàn bộ vé giấy cho đơn.
     * <p>Chỉ đơn đã thanh toán (CONFIRMED) & CHƯA in mới được in. Quét lại đơn
     * đã in → báo lỗi (chống in trùng). Quản lý trạng thái ở cấp Đơn hàng
     * ({@link Booking#getPrintedAt()}); đồng thời đánh dấu các vé đã check-in
     * để đồng bộ với báo cáo tiến độ.
     */
    @Transactional
    public BookingPrintResponse printByBookingCode(String bookingCode) {
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new RuntimeException("Vui lòng cung cấp mã đặt vé.");
        }
        Booking booking = bookingRepository.findByBookingCodeForPrint(bookingCode.trim())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt vé với mã: " + bookingCode));

        // Cách ly cụm rạp: chỉ in/soát vé của cơ sở mình.
        SecurityUtils.assertCinemaAccess(cinemaIdOf(booking));
        Staff staff = currentStaffOrNull();

        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Đơn chưa thanh toán hoặc không hợp lệ để in vé.");
        }
        if (booking.getPrintedAt() != null) {
            throw new RuntimeException("Mã đặt vé này đã được in thành vé giấy trước đó vào lúc: "
                    + booking.getPrintedAt().format(TIME_FMT));
        }

        LocalDateTime now = LocalDateTime.now();
        booking.setPrintedAt(now);
        if (staff != null) {
            booking.setPrintedBy(staff);
        }
        bookingRepository.save(booking);

        // Đồng bộ trạng thái vé từng ghế (giữ báo cáo tiến độ check-in nhất quán).
        List<Ticket> tickets = ticketRepository.findAllByBookingId(booking.getId());
        for (Ticket t : tickets) {
            if (Boolean.TRUE.equals(t.getIsRevoked())) continue; // Bỏ qua vé đã bị thu hồi do đổi chỗ
            if (!Boolean.TRUE.equals(t.getIsCheckedIn())) {
                t.setIsCheckedIn(true);
                t.setCheckInTime(now);
                if (staff != null) {
                    t.setCheckedInBy(staff);
                }
            }
        }
        ticketRepository.saveAll(tickets);

        // Đơn đã in vé giấy → gửi email hoá đơn/cảm ơn KHÔNG kèm QR (khách đã có vé giấy).
        sendReceiptEmail(booking);

        return buildResponse(booking);
    }

    /**
     * VẤN ĐỀ 2 FIX: Xác thực và check-in vé lẻ qua mã QR của từng vé.
     * Chặn quét vé cũ đã bị thu hồi sau khi đổi chỗ.
     */
    @Transactional
    public TicketVerificationResponse verifyAndCheckInTicket(String qrCode) {
        if (qrCode == null || qrCode.isBlank()) {
            throw new IllegalArgumentException("Vui lòng cung cấp mã QR vé.");
        }

        String normalizedQrCode = qrCode.trim();
        Ticket ticket = ticketRepository.findByQrCodeWithDetails(normalizedQrCode).orElse(null);
        if (ticket == null) {
            TicketQrHistory revokedQr = ticketQrHistoryRepository.findByQrCodeWithDetails(normalizedQrCode)
                    .orElseThrow(() -> new IllegalArgumentException("Mã vé không tồn tại hoặc không hợp lệ."));
            Ticket currentTicket = revokedQr.getTicket();
            Booking currentBooking = currentTicket.getBookingSeat().getBooking();
            SecurityUtils.assertCinemaAccess(cinemaIdOf(currentBooking));
            String currentSeatLabel = currentTicket.getBookingSeat().getSeat().displayLabel();
            throw new IllegalArgumentException(
                    "VÉ ĐÃ BỊ THU HỒI: Chỗ ngồi đã được chuyển sang " + currentSeatLabel
                    + ". Vui lòng sử dụng vé in mới nhất.");
        }

        if (Boolean.TRUE.equals(ticket.getIsRevoked())) {
            String seatLabel = ticket.getBookingSeat() != null && ticket.getBookingSeat().getSeat() != null
                    ? ticket.getBookingSeat().getSeat().displayLabel() : "ghế mới";
            throw new IllegalArgumentException(
                    "VÉ ĐÃ BỊ THU HỒI: Chỗ ngồi đã được chuyển sang " + seatLabel
                    + ". Vui lòng sử dụng vé in mới nhất.");
        }

        Booking booking = ticket.getBookingSeat() != null ? ticket.getBookingSeat().getBooking() : null;
        if (booking != null) {
            SecurityUtils.assertCinemaAccess(cinemaIdOf(booking));
        }

        if (Boolean.TRUE.equals(ticket.getIsCheckedIn())) {
            throw new IllegalArgumentException("Vé này đã được check-in vào lúc: "
                    + (ticket.getCheckInTime() != null ? ticket.getCheckInTime().format(TIME_FMT) : "trước đó"));
        }

        ticket.setIsCheckedIn(true);
        ticket.setCheckInTime(LocalDateTime.now());
        ticket.setCheckedInBy(currentStaffOrNull());
        Ticket savedTicket = ticketRepository.save(ticket);
        BookingSeat bookingSeat = savedTicket.getBookingSeat();
        Booking savedBooking = bookingSeat.getBooking();
        Showtime showtime = savedBooking.getShowtime();
        return new TicketVerificationResponse(
                savedTicket.getId(),
                savedBooking.getBookingCode(),
                bookingSeat.getSeat().displayLabel(),
                showtime.getMovie() != null ? showtime.getMovie().getTitle() : "Phim",
                showtime.getRoom() != null ? showtime.getRoom().getName() : "",
                showtime.getStartTime(),
                savedTicket.getCheckInTime());
    }

    /**
     * Gửi email hoá đơn (ẩn QR, chỉ lời cảm ơn) khi đơn đã được in vé giấy tại quầy.
     * Best-effort: bỏ qua nếu không có khách/email; lỗi mail không ảnh hưởng việc in vé.
     */
    private void sendReceiptEmail(Booking booking) {
        try {
            // Chỉ gửi cho ĐƠN ONLINE GỐC (channel = ONLINE). Đơn POS đã nhận mail hoá đơn
            // lúc thanh toán → khi quét in lại KHÔNG gửi thêm, tránh làm phiền hộp thư khách.
            // Dùng channel (tin cậy) thay staffSchedule vì admin/manager bán POS có schedule = null.
            if (!"ONLINE".equalsIgnoreCase(booking.getChannel())) {
                return;
            }
            if (booking.getCustomer() == null || booking.getCustomer().getUser() == null) {
                return;
            }
            User user = booking.getCustomer().getUser();
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                return;
            }
            Showtime showtime = booking.getShowtime();
            Movie movie = showtime != null ? showtime.getMovie() : null;
            String formatName = (showtime != null && showtime.getFormat() != null) ? showtime.getFormat().getName() : "";
            Room room = showtime != null ? showtime.getRoom() : null;
            Cinema cinema = room != null ? room.getCinema() : null;

            List<TicketEmailData.SeatLine> seatLines = new ArrayList<>();
            for (BookingSeat bs : bookingSeatRepository.findAllByBookingIdWithSeat(booking.getId())) {
                if (!"SOLD".equalsIgnoreCase(bs.getStatus())) continue;
                Seat seat = bs.getSeat();
                String label = seat != null ? seat.displayLabel() : "";
                String seatType = (seat != null && seat.getSeatType() != null) ? seat.getSeatType().getName() : null;
                seatLines.add(new TicketEmailData.SeatLine(label, seatType, bs.getTicketType(), null));
            }

            List<TicketEmailData.FnbLine> fnbLines = new ArrayList<>();
            for (BookingFnb bf : bookingFnbRepository.findByBookingIdWithFnb(booking.getId())) {
                String name = bf.getItemNameSnapshot() != null ? bf.getItemNameSnapshot() : bf.getFnbItem().getName();
                fnbLines.add(new TicketEmailData.FnbLine(name, bf.getQuantity()));
            }

            mailService.sendTicketEmail(new TicketEmailData(
                    user.getEmail(),
                    user.getFullName(),
                    booking.getBookingCode(),
                    movie != null ? movie.getTitle() : "Phim",
                    formatName,
                    cinema != null ? cinema.getName() : "",
                    room != null ? room.getName() : "",
                    showtime != null ? showtime.getStartTime() : null,
                    booking.getPaymentMethod(),
                    booking.getFinalPrice(),
                    seatLines,
                    fnbLines,
                    false)); // showQr = false → ẩn QR, hiển thị lời cảm ơn
        } catch (Exception e) {
            log.error("Lỗi gửi email hoá đơn khi in vé đơn {}: {}", booking.getBookingCode(), e.getMessage());
        }
    }

    private BookingPrintResponse buildResponse(Booking booking) {
        Showtime showtime = booking.getShowtime();
        Room room = showtime != null ? showtime.getRoom() : null;
        Cinema cinema = room != null ? room.getCinema() : null;
        String format = showtime != null && showtime.getFormat() != null ? showtime.getFormat().getName() : "";

        List<BookingPrintResponse.SeatLine> seatLines = new ArrayList<>();
        Map<Integer, Ticket> activeTicketsByBookingSeatId = ticketRepository.findAllByBookingIdWithSeat(booking.getId())
                .stream()
                .filter(ticket -> !Boolean.TRUE.equals(ticket.getIsRevoked()))
                .collect(Collectors.toMap(
                        ticket -> ticket.getBookingSeat().getId(),
                        Function.identity(),
                        (first, ignored) -> first));
        boolean requiresStudentVerification = false;
        for (BookingSeat bs : bookingSeatRepository.findAllByBookingIdWithSeat(booking.getId())) {
            Seat seat = bs.getSeat();
            String label = seat.displayLabel();
            String type = bs.getTicketType();
            Ticket activeTicket = activeTicketsByBookingSeatId.get(bs.getId());
            seatLines.add(new BookingPrintResponse.SeatLine(
                    label,
                    type,
                    bs.getPriceSnapshot(),
                    activeTicket != null ? activeTicket.getQrCode() : null));
            
            if (seat.getSeatType() != null && "SWEETBOX".equals(seat.getSeatType().getName()) && type != null && List.of("U22", "CHILD", "SENIOR").contains(type.toUpperCase())) {
                requiresStudentVerification = true;
            }
        }

        List<BookingPrintResponse.FnbLine> fnbLines = new ArrayList<>();
        for (BookingFnb bf : bookingFnbRepository.findByBookingIdWithFnb(booking.getId())) {
            String name = bf.getItemNameSnapshot() != null ? bf.getItemNameSnapshot() : bf.getFnbItem().getName();
            List<BookingPrintResponse.FnbLine.FnbOptionLine> optLines = new ArrayList<>();
            BigDecimal totalSurcharge = BigDecimal.ZERO;
            if (bf.getOptions() != null) {
                for (com.devcine.backend.entity.BookingFnbOption opt : bf.getOptions()) {
                    BigDecimal sc = opt.getSurchargeSnapshot() != null ? opt.getSurchargeSnapshot() : BigDecimal.ZERO;
                    totalSurcharge = totalSurcharge.add(sc);
                    optLines.add(new BookingPrintResponse.FnbLine.FnbOptionLine(
                            opt.getSlotLabelSnapshot(), opt.getOptionNameSnapshot(), sc));
                }
            }
            fnbLines.add(new BookingPrintResponse.FnbLine(name, bf.getQuantity(), bf.getPriceSnapshot(), totalSurcharge, optLines));
        }

        BigDecimal total = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal fin = booking.getFinalPrice() != null ? booking.getFinalPrice() : total;
        BigDecimal discount = total.subtract(fin).max(BigDecimal.ZERO);

        String memberName = booking.getCustomer() != null && booking.getCustomer().getUser() != null
                ? booking.getCustomer().getUser().getFullName() : null;

        Staff staff = currentStaffOrNull();
        String cashierName = (booking.getPrintedBy() != null && booking.getPrintedBy().getUser() != null)
                ? booking.getPrintedBy().getUser().getFullName()
                : (staff != null && staff.getUser() != null ? staff.getUser().getFullName() : "Nguyễn Quang Huy");

        return new BookingPrintResponse(
                booking.getBookingCode(),
                showtime != null && showtime.getMovie() != null ? showtime.getMovie().getTitle() : "Phim",
                cinema != null && cinema.getName() != null ? cinema.getName() : "DEVCINE CINEMA",
                cinema != null && cinema.getAddress() != null ? cinema.getAddress() : "Tầng 3, TTTM DevCine Plaza, Hà Nội",
                room != null ? room.getName() : "",
                room != null && room.getType() != null ? room.getType() : "Standard",
                format,
                showtime != null ? showtime.getStartTime() : null,
                showtime != null ? showtime.getEndTime() : null,
                booking.getPaymentMethod(),
                total,
                fin,
                discount,
                memberName,
                seatLines.size(),
                seatLines,
                fnbLines,
                booking.getPrintedAt(),
                cashierName,
                requiresStudentVerification);
    }

    /**
     * Dựng dữ liệu in vé cho một đơn (dùng cho IN LẠI sau khi xử lý sự cố đổi ghế).
     * Khác {@link #lookupByBookingCode}: KHÔNG chặn đơn đã in — đổi ghế xong luôn cần in lại nhãn mới.
     */
    @Transactional(readOnly = true)
    public BookingPrintResponse buildPrintData(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn để in lại vé."));
        return buildResponse(booking);
    }

    /**
     * Gửi lại email xác nhận đơn ONLINE. Mã đơn giữ nguyên để tra cứu; QR soát vé phiên bản mới
     * được gửi bởi luồng email sự cố chuyên biệt sau khi đổi ghế.
     */
    @Transactional
    public boolean resendTicketEmailIfOnline(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return false;
        if ("POS".equalsIgnoreCase(booking.getChannel())) return false; // đơn POS không kèm QR
        if (booking.getCustomer() == null || booking.getCustomer().getUser() == null) return false;
        User user = booking.getCustomer().getUser();
        if (user.getEmail() == null || user.getEmail().isBlank()) return false;

        Showtime showtime = booking.getShowtime();
        Movie movie = showtime != null ? showtime.getMovie() : null;
        String formatName = (showtime != null && showtime.getFormat() != null) ? showtime.getFormat().getName() : "";
        Room room = showtime != null ? showtime.getRoom() : null;
        Cinema cinema = room != null ? room.getCinema() : null;

        List<TicketEmailData.SeatLine> seatLines = new ArrayList<>();
        for (Ticket t : ticketRepository.findAllByBookingIdWithSeat(bookingId)) {
            BookingSeat bs = t.getBookingSeat();
            if (bs == null || !"SOLD".equalsIgnoreCase(bs.getStatus())) continue; // bỏ ghế đã hủy
            Seat seat = bs.getSeat();
            String label = seat != null ? seat.displayLabel() : "";
            String seatType = (seat != null && seat.getSeatType() != null) ? seat.getSeatType().getName() : null;
            seatLines.add(new TicketEmailData.SeatLine(label, seatType, bs.getTicketType(), t.getQrCode()));
        }

        List<TicketEmailData.FnbLine> fnbLines = new ArrayList<>();
        for (BookingFnb bf : bookingFnbRepository.findByBookingIdWithFnb(bookingId)) {
            String name = bf.getItemNameSnapshot() != null ? bf.getItemNameSnapshot() : bf.getFnbItem().getName();
            fnbLines.add(new TicketEmailData.FnbLine(name, bf.getQuantity()));
        }

        try {
            mailService.sendTicketEmail(new TicketEmailData(
                    user.getEmail(), user.getFullName(), booking.getBookingCode(),
                    movie != null ? movie.getTitle() : "Phim",
                    formatName,
                    cinema != null ? cinema.getName() : "",
                    room != null ? room.getName() : "",
                    showtime != null ? showtime.getStartTime() : null,
                    booking.getPaymentMethod(), booking.getFinalPrice(),
                    seatLines, fnbLines, true));
            return true;
        } catch (Exception e) {
            log.error("Lỗi gửi lại email vé sau đổi ghế cho đơn {}: {}", booking.getBookingCode(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gửi email thông báo đổi ghế sự cố & phát voucher đền bù (kèm mã QR voucher và mã QR vé mới)
     * cho đơn ONLINE. Trả false (im lặng) nếu đơn POS / không có email.
     */
    @Transactional(readOnly = true)
    public boolean sendIncidentRelocateEmailIfOnline(
            Integer bookingId,
            String reason,
            List<com.devcine.backend.dto.IncidentRelocateEmailData.SeatSwapLine> swaps,
            com.devcine.backend.dto.response.IncidentResultResponse.CompensationResult comp,
            String voucherLabel) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return false;
        if ("POS".equalsIgnoreCase(booking.getChannel())) return false; // đơn POS trao quà tại quầy
        if (booking.getCustomer() == null || booking.getCustomer().getUser() == null) return false;
        User user = booking.getCustomer().getUser();
        if (user.getEmail() == null || user.getEmail().isBlank()) return false;

        Showtime showtime = booking.getShowtime();
        Movie movie = showtime != null ? showtime.getMovie() : null;
        Room room = showtime != null ? showtime.getRoom() : null;
        Cinema cinema = room != null ? room.getCinema() : null;

        List<TicketEmailData.SeatLine> seatLines = new ArrayList<>();
        for (Ticket t : ticketRepository.findAllByBookingIdWithSeat(bookingId)) {
            BookingSeat bs = t.getBookingSeat();
            if (bs == null || !"SOLD".equalsIgnoreCase(bs.getStatus())) continue; // bỏ ghế đã hủy
            seatLines.add(new TicketEmailData.SeatLine(bs.getSeat().displayLabel(), bs.getTicketType(), t.getQrCode()));
        }

        List<TicketEmailData.FnbLine> fnbLines = new ArrayList<>();
        for (BookingFnb bf : bookingFnbRepository.findByBookingIdWithFnb(bookingId)) {
            String name = bf.getItemNameSnapshot() != null ? bf.getItemNameSnapshot() : bf.getFnbItem().getName();
            fnbLines.add(new TicketEmailData.FnbLine(name, bf.getQuantity()));
        }

        boolean voucherIssued = comp != null && comp.voucherIssued();
        String voucherCode = comp != null ? comp.voucherCode() : null;
        BigDecimal voucherValue = comp != null ? comp.value() : BigDecimal.ZERO;
        String voucherType = comp != null ? comp.type() : "NONE";
        boolean counterGift = comp != null && comp.counterGift();

        try {
            mailService.sendIncidentRelocateEmail(new com.devcine.backend.dto.IncidentRelocateEmailData(
                    user.getEmail(),
                    user.getFullName(),
                    booking.getBookingCode(),
                    movie != null ? movie.getTitle() : "Phim",
                    cinema != null ? cinema.getName() : "",
                    room != null ? room.getName() : "",
                    showtime != null ? showtime.getStartTime() : null,
                    reason,
                    swaps,
                    voucherIssued,
                    voucherCode,
                    voucherLabel,
                    voucherValue,
                    voucherType,
                    counterGift,
                    seatLines,
                    fnbLines
            ));
            return true;
        } catch (Exception e) {
            log.error("Lỗi gửi email sự cố sau đổi ghế cho đơn {}: {}", booking.getBookingCode(), e.getMessage(), e);
            return false;
        }
    }
}
