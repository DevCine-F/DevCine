package com.devcine.backend.service;

import com.devcine.backend.entity.Ticket;
import com.devcine.backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private static final int CHECK_IN_EARLY_MINUTES = 30;
    private static final int CHECK_IN_LATE_MINUTES = 10;

    private final TicketRepository ticketRepository;
    private final ShiftAccessService shiftAccessService;

    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByBooking(Integer bookingId) {
        return ticketRepository.findAllByBookingId(bookingId);
    }

    @Transactional
    public Ticket checkIn(String qrCode) {
        var schedule = shiftAccessService.requireCurrentShiftForStaff(List.of("CHECK_IN", "SHIFT_LEAD"), "kiem soat ve");
        Ticket ticket = ticketRepository.findByQrCodeWithDetails(qrCode)
                .orElseThrow(() -> new RuntimeException("Ve khong ton tai tren he thong"));

        if (Boolean.TRUE.equals(ticket.getIsCheckedIn())) {
            throw new RuntimeException("Ve nay da duoc check-in truoc do vao luc: " + ticket.getCheckInTime());
        }

        var showtime = ticket.getBookingSeat().getBooking().getShowtime();
        LocalDateTime showtimeStart = showtime.getStartTime();
        LocalDateTime showtimeEnd = showtime.getEndTime();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInOpenAt = showtimeStart.minusMinutes(CHECK_IN_EARLY_MINUTES);
        LocalDateTime checkInCloseAt = showtimeEnd.plusMinutes(CHECK_IN_LATE_MINUTES);
        if (now.isBefore(checkInOpenAt)) {
            throw new RuntimeException("Chua den gio check-in. Cong check-in mo luc: " + checkInOpenAt);
        }
        if (now.isAfter(checkInCloseAt)) {
            throw new RuntimeException("Da qua thoi gian check-in. Cong check-in dong luc: " + checkInCloseAt);
        }

        ticket.setIsCheckedIn(true);
        ticket.setCheckInTime(now);
        if (schedule != null) {
            ticket.setCheckedInBy(schedule.getStaff());
        }

        return ticketRepository.save(ticket);
    }
}
