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

        if (ticket.getIsCheckedIn()) {
            throw new RuntimeException("Ve nay da duoc check-in truoc do vao luc: " + ticket.getCheckInTime());
        }

        LocalDateTime showtimeStart = ticket.getBookingSeat().getBooking().getShowtime().getStartTime();
        if (showtimeStart.toLocalDate().isBefore(LocalDateTime.now().toLocalDate())) {
            throw new RuntimeException("Suat chieu cua ve nay da dien ra trong qua khu (" + showtimeStart + ")");
        }

        ticket.setIsCheckedIn(true);
        ticket.setCheckInTime(LocalDateTime.now());
        if (schedule != null) {
            ticket.setCheckedInBy(schedule.getStaff());
        }

        return ticketRepository.save(ticket);
    }
}
