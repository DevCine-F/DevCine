package com.devcine.backend.service;

import com.devcine.backend.dto.TicketEmailData;
import com.devcine.backend.dto.response.BookingPrintResponse;
import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.BookingFnb;
import com.devcine.backend.entity.BookingSeat;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.entity.Room;
import com.devcine.backend.entity.Seat;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.entity.Ticket;
import com.devcine.backend.entity.User;
import com.devcine.backend.repository.BookingFnbRepository;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingFnbRepository bookingFnbRepository;
    private final CurrentStaffService currentStaffService;
    private final MailService mailService;

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
        Staff handledBy = currentStaffService.current();

        if (bookingCode == null || bookingCode.isBlank()) {
            throw new RuntimeException("Vui lòng cung cấp mã đặt vé.");
        }
        Booking booking = bookingRepository.findByBookingCodeForPrint(bookingCode.trim())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt vé với mã: " + bookingCode));

        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Đơn chưa thanh toán hoặc không hợp lệ để in vé.");
        }
        if (booking.getPrintedAt() != null) {
            throw new RuntimeException("Mã đặt vé này đã được in thành vé giấy trước đó vào lúc: "
                    + booking.getPrintedAt().format(TIME_FMT));
        }

        LocalDateTime now = LocalDateTime.now();
        booking.setPrintedAt(now);
        if (handledBy != null) {
            booking.setPrintedBy(handledBy);
        }
        bookingRepository.save(booking);

        // Đồng bộ trạng thái vé từng ghế (giữ báo cáo tiến độ check-in nhất quán).
        List<Ticket> tickets = ticketRepository.findAllByBookingId(booking.getId());
        for (Ticket t : tickets) {
            if (!Boolean.TRUE.equals(t.getIsCheckedIn())) {
                t.setIsCheckedIn(true);
                t.setCheckInTime(now);
                if (handledBy != null) {
                    t.setCheckedInBy(handledBy);
                }
            }
        }
        ticketRepository.saveAll(tickets);

        // Đơn đã in vé giấy → gửi email hoá đơn/cảm ơn KHÔNG kèm QR (khách đã có vé giấy).
        sendReceiptEmail(booking);

        return buildResponse(booking);
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
            Room room = showtime != null ? showtime.getRoom() : null;
            Cinema cinema = room != null ? room.getCinema() : null;

            List<TicketEmailData.FnbLine> fnbLines = new ArrayList<>();
            for (BookingFnb bf : bookingFnbRepository.findByBookingIdWithFnb(booking.getId())) {
                fnbLines.add(new TicketEmailData.FnbLine(bf.getFnbItem().getName(), bf.getQuantity()));
            }

            mailService.sendTicketEmail(new TicketEmailData(
                    user.getEmail(),
                    user.getFullName(),
                    booking.getBookingCode(),
                    movie != null ? movie.getTitle() : "Phim",
                    cinema != null ? cinema.getName() : "",
                    room != null ? room.getName() : "",
                    showtime != null ? showtime.getStartTime() : null,
                    booking.getPaymentMethod(),
                    booking.getFinalPrice(),
                    List.of(),
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
        for (BookingSeat bs : bookingSeatRepository.findAllByBookingIdWithSeat(booking.getId())) {
            Seat seat = bs.getSeat();
            String label = seat.getRowChar() + String.valueOf(seat.getColNum());
            seatLines.add(new BookingPrintResponse.SeatLine(label, bs.getTicketType(), bs.getPriceSnapshot()));
        }

        List<BookingPrintResponse.FnbLine> fnbLines = new ArrayList<>();
        for (BookingFnb bf : bookingFnbRepository.findByBookingIdWithFnb(booking.getId())) {
            fnbLines.add(new BookingPrintResponse.FnbLine(
                    bf.getFnbItem().getName(), bf.getQuantity(), bf.getPriceSnapshot()));
        }

        BigDecimal total = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal fin = booking.getFinalPrice() != null ? booking.getFinalPrice() : total;
        BigDecimal discount = total.subtract(fin).max(BigDecimal.ZERO);

        String memberName = booking.getCustomer() != null && booking.getCustomer().getUser() != null
                ? booking.getCustomer().getUser().getFullName() : null;

        return new BookingPrintResponse(
                booking.getBookingCode(),
                showtime != null && showtime.getMovie() != null ? showtime.getMovie().getTitle() : "Phim",
                cinema != null ? cinema.getName() : "",
                room != null ? room.getName() : "",
                format,
                showtime != null ? showtime.getStartTime() : null,
                booking.getPaymentMethod(),
                total,
                fin,
                discount,
                memberName,
                seatLines.size(),
                seatLines,
                fnbLines,
                booking.getPrintedAt());
    }
}
