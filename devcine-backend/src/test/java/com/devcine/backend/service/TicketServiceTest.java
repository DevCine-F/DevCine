package com.devcine.backend.service;

import com.devcine.backend.dto.response.BookingPrintResponse;
import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingSeatRepository bookingSeatRepository;
    @Mock BookingFnbRepository bookingFnbRepository;
    @Mock StaffRepository staffRepository;
    @Mock MailService mailService;

    @InjectMocks TicketService ticketService;

    @BeforeEach
    void authenticateAsAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "999", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void lookupByBookingCode_rejectsWhenMovieEnded() {
        // Phim 120 phút, bắt đầu từ 130 phút trước -> đã kết thúc 10 phút trước
        Booking booking = createSampleBooking(LocalDateTime.now().minusMinutes(130), 120);
        when(bookingRepository.findByBookingCodeForPrint("BK-001")).thenReturn(Optional.of(booking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ticketService.lookupByBookingCode("BK-001"));

        assertTrue(ex.getMessage().contains("Quá giờ checkin!"));
        assertTrue(ex.getMessage().contains("Phim đã kết thúc suất chiếu vào lúc"));
    }

    @Test
    void printByBookingCode_rejectsWhenMovieEnded() {
        // Phim 100 phút, bắt đầu từ 110 phút trước -> đã kết thúc
        Booking booking = createSampleBooking(LocalDateTime.now().minusMinutes(110), 100);
        when(bookingRepository.findByBookingCodeForPrint("BK-001")).thenReturn(Optional.of(booking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ticketService.printByBookingCode("BK-001"));

        assertTrue(ex.getMessage().contains("Quá giờ checkin!"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void verifyAndCheckInTicket_rejectsWhenMovieEnded() {
        // Phim 90 phút, bắt đầu từ 100 phút trước -> đã kết thúc
        Ticket ticket = ticketGraph("C7", LocalDateTime.now().minusMinutes(100), 90);
        when(ticketRepository.findByQrCodeWithDetails("ACTIVE-QR")).thenReturn(Optional.of(ticket));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ticketService.verifyAndCheckInTicket("ACTIVE-QR"));

        assertTrue(ex.getMessage().contains("Quá giờ checkin!"));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void lookupByBookingCode_rejectsDuringTurnaroundCleaningTime() {
        // Phim 100 phút, bắt đầu 105 phút trước (phim đã hết 5 phút trước), nhưng suất chiếu tổng có 15 phút dọn phòng (endTime còn 10 phút nữa)
        // Nghiệp vụ yêu cầu: Khóa ngay khi phim kết thúc, không tính thời gian dọn phòng
        LocalDateTime start = LocalDateTime.now().minusMinutes(105);
        Booking booking = createSampleBooking(start, 100);
        booking.getShowtime().setEndTime(start.plusMinutes(115)); // endTime vẫn ở tương lai 10 phút
        when(bookingRepository.findByBookingCodeForPrint("BK-001")).thenReturn(Optional.of(booking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ticketService.lookupByBookingCode("BK-001"));

        assertTrue(ex.getMessage().contains("Quá giờ checkin!"));
    }

    @Test
    void lookupByBookingCode_allowsWhenMovieStillScreening() {
        // Phim 120 phút, bắt đầu từ 30 phút trước -> đang chiếu, còn 90 phút
        Booking booking = createSampleBooking(LocalDateTime.now().minusMinutes(30), 120);
        when(bookingRepository.findByBookingCodeForPrint("BK-001")).thenReturn(Optional.of(booking));
        when(ticketRepository.findAllByBookingId(booking.getId())).thenReturn(Collections.emptyList());
        when(bookingSeatRepository.findAllByBookingIdWithSeat(booking.getId())).thenReturn(Collections.emptyList());
        when(bookingFnbRepository.findByBookingIdWithFnb(booking.getId())).thenReturn(Collections.emptyList());

        BookingPrintResponse res = ticketService.lookupByBookingCode("BK-001");

        assertNotNull(res);
        assertEquals("BK-001", res.bookingCode());
    }

    @Test
    void printByBookingCode_allowsWhenMovieStillScreening() {
        // Phim 120 phút, bắt đầu từ 30 phút trước
        Booking booking = createSampleBooking(LocalDateTime.now().minusMinutes(30), 120);
        when(bookingRepository.findByBookingCodeForPrint("BK-001")).thenReturn(Optional.of(booking));
        when(ticketRepository.findAllByBookingId(booking.getId())).thenReturn(Collections.emptyList());
        when(bookingSeatRepository.findAllByBookingIdWithSeat(booking.getId())).thenReturn(Collections.emptyList());
        when(bookingFnbRepository.findByBookingIdWithFnb(booking.getId())).thenReturn(Collections.emptyList());

        BookingPrintResponse res = ticketService.printByBookingCode("BK-001");

        assertNotNull(res);
        assertNotNull(booking.getPrintedAt());
        verify(bookingRepository).save(booking);
    }

    @Test
    void verifyAndCheckInTicket_rejectsRevokedTicket() {
        Ticket ticket = ticketGraph("C7", LocalDateTime.now().plusHours(1), 120);
        ticket.setIsRevoked(true);
        when(ticketRepository.findByQrCodeWithDetails("REVOKED-QR")).thenReturn(Optional.of(ticket));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ticketService.verifyAndCheckInTicket("REVOKED-QR"));

        assertTrue(ex.getMessage().contains("VÉ ĐÃ BỊ THU HỒI"));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void verifyAndCheckInTicket_allowsAndMarksCheckedIn() {
        Ticket ticket = ticketGraph("C7", LocalDateTime.now().plusHours(1), 120);
        when(ticketRepository.findByQrCodeWithDetails("ACTIVE-QR")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket res = ticketService.verifyAndCheckInTicket("ACTIVE-QR");

        assertNotNull(res);
        assertTrue(res.getIsCheckedIn());
        assertNotNull(res.getCheckInTime());
        verify(ticketRepository).save(ticket);
    }

    private Booking createSampleBooking(LocalDateTime startTime, int durationMins) {
        Cinema cinema = Cinema.builder().id(1).name("DevCine").build();
        Room room = Room.builder().id(2).name("Phòng 2").cinema(cinema).build();
        Movie movie = Movie.builder().id(3).title("Phim thử nghiệm").durationMins(durationMins).build();
        Showtime showtime = Showtime.builder()
                .id(4)
                .movie(movie)
                .room(room)
                .startTime(startTime)
                .endTime(startTime.plusMinutes(durationMins + 15))
                .build();
        return Booking.builder()
                .id(5)
                .bookingCode("BK-001")
                .status("CONFIRMED")
                .showtime(showtime)
                .build();
    }

    private Ticket ticketGraph(String seatLabel, LocalDateTime startTime, int durationMins) {
        Booking booking = createSampleBooking(startTime, durationMins);
        Seat seat = Seat.builder().id(6).label(seatLabel).rowChar("C").colNum(7).room(booking.getShowtime().getRoom()).build();
        BookingSeat bookingSeat = BookingSeat.builder()
                .id(7).booking(booking).seat(seat).status("SOLD").build();
        return Ticket.builder()
                .id(8).bookingSeat(bookingSeat).qrCode("ACTIVE-QR")
                .isCheckedIn(false).isRevoked(false).version(2).build();
    }
}
