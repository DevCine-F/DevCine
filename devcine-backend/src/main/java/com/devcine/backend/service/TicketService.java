package com.devcine.backend.service;

import com.devcine.backend.dto.response.BookingPrintResponse;
import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.BookingFnb;
import com.devcine.backend.entity.BookingSeat;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Room;
import com.devcine.backend.entity.Seat;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.entity.Ticket;
import com.devcine.backend.repository.BookingFnbRepository;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingFnbRepository bookingFnbRepository;
    private final ShiftAccessService shiftAccessService;

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
        shiftAccessService.requireCurrentShiftForStaff(List.of("CHECK_IN", "SHIFT_LEAD"), "kiem tra ve");

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
        var schedule = shiftAccessService.requireCurrentShiftForStaff(List.of("CHECK_IN", "SHIFT_LEAD"), "in ve tai quay");

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
        if (schedule != null) {
            booking.setPrintedBy(schedule.getStaff());
        }
        bookingRepository.save(booking);

        // Đồng bộ trạng thái vé từng ghế (giữ báo cáo tiến độ check-in nhất quán).
        List<Ticket> tickets = ticketRepository.findAllByBookingId(booking.getId());
        for (Ticket t : tickets) {
            if (!Boolean.TRUE.equals(t.getIsCheckedIn())) {
                t.setIsCheckedIn(true);
                t.setCheckInTime(now);
                if (schedule != null) {
                    t.setCheckedInBy(schedule.getStaff());
                }
            }
        }
        ticketRepository.saveAll(tickets);

        return buildResponse(booking);
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
