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

    public List<Ticket> getTicketsByBooking(Integer bookingId) {
        return ticketRepository.findAllByBookingId(bookingId);
    }

    @Transactional
    public Ticket checkIn(String qrCode) {
        Ticket ticket = ticketRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new RuntimeException("Vé không tồn tại trên hệ thống"));

        if (ticket.getIsCheckedIn()) {
            throw new RuntimeException("Vé này đã được check-in trước đó vào lúc: " + ticket.getCheckInTime());
        }

        // Validate showtime date (nếu suất chiếu không phải hôm nay, ném cảnh báo nhưng cho phép check-in nếu admin vẫn duyệt - ở đây ta chỉ validate cơ bản)
        LocalDateTime showtimeStart = ticket.getBookingSeat().getBooking().getShowtime().getStartTime();
        if (showtimeStart.toLocalDate().isBefore(LocalDateTime.now().toLocalDate())) {
            throw new RuntimeException("Suất chiếu của vé này đã diễn ra trong quá khứ (" + showtimeStart + ")");
        }

        ticket.setIsCheckedIn(true);
        ticket.setCheckInTime(LocalDateTime.now());
        
        return ticketRepository.save(ticket);
    }
}
