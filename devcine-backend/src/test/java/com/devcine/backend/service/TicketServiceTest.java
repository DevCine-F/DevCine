package com.devcine.backend.service;

import com.devcine.backend.dto.response.TicketVerificationResponse;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.Disabled("Tạm ẩn phân hệ sự cố & QR history")
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock TicketQrHistoryRepository ticketQrHistoryRepository;
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
    void verifyAndCheckInTicket_rejectsRevokedQrWithCurrentSeat() {
        Ticket currentTicket = ticketGraph("C7");
        TicketQrHistory history = TicketQrHistory.builder()
                .ticket(currentTicket)
                .qrCode("OLD-QR")
                .ticketVersion(1)
                .build();
        when(ticketRepository.findByQrCodeWithDetails("OLD-QR")).thenReturn(Optional.empty());
        when(ticketQrHistoryRepository.findByQrCodeWithDetails("OLD-QR")).thenReturn(Optional.of(history));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ticketService.verifyAndCheckInTicket("OLD-QR"));

        assertTrue(exception.getMessage().contains("VÉ ĐÃ BỊ THU HỒI"));
        assertTrue(exception.getMessage().contains("C7"));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void verifyAndCheckInTicket_returnsDtoAndMarksActiveTicketCheckedIn() {
        Ticket ticket = ticketGraph("C7");
        when(ticketRepository.findByQrCodeWithDetails("ACTIVE-QR")).thenReturn(Optional.of(ticket));
        when(staffRepository.findById(999)).thenReturn(Optional.empty());
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket response = ticketService.verifyAndCheckInTicket("ACTIVE-QR");

        assertEquals("BK-001", response.getBookingSeat().getBooking().getBookingCode());
        assertEquals("C7", response.getBookingSeat().getSeat().getLabel());
        assertTrue(ticket.getIsCheckedIn());
        assertNotNull(response.getCheckInTime());
    }

    private Ticket ticketGraph(String seatLabel) {
        Cinema cinema = Cinema.builder().id(1).name("DevCine").build();
        Room room = Room.builder().id(2).name("Phòng 2").cinema(cinema).build();
        Movie movie = Movie.builder().id(3).title("Phim thử nghiệm").build();
        Showtime showtime = Showtime.builder()
                .id(4).movie(movie).room(room).startTime(LocalDateTime.now().plusHours(1)).build();
        Booking booking = Booking.builder()
                .id(5).bookingCode("BK-001").status("CONFIRMED").showtime(showtime).build();
        Seat seat = Seat.builder().id(6).label(seatLabel).rowChar("C").colNum(7).room(room).build();
        BookingSeat bookingSeat = BookingSeat.builder()
                .id(7).booking(booking).seat(seat).status("SOLD").build();
        return Ticket.builder()
                .id(8).bookingSeat(bookingSeat).qrCode("ACTIVE-QR")
                .isCheckedIn(false).isRevoked(false).version(2).build();
    }
}
