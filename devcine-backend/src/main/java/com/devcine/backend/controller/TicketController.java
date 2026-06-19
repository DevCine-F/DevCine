package com.devcine.backend.controller;

import com.devcine.backend.entity.Ticket;
import com.devcine.backend.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getTicketsByBooking(@PathVariable Integer bookingId) {
        try {
            List<Ticket> tickets = ticketService.getTicketsByBooking(bookingId);
            return ResponseEntity.ok(tickets);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(@RequestParam String qrCode) {
        try {
            Ticket ticket = ticketService.checkIn(qrCode);
            return ResponseEntity.ok(Map.of(
                "message", "Check-in thành công!",
                "ticketCode", ticket.getQrCode(),
                "seatName", ticket.getBookingSeat().getSeat().getRowChar() + ticket.getBookingSeat().getSeat().getColNum(),
                "movieTitle", ticket.getBookingSeat().getBooking().getShowtime().getMovie().getTitle(),
                "roomName", ticket.getBookingSeat().getBooking().getShowtime().getRoom().getName(),
                "startTime", ticket.getBookingSeat().getBooking().getShowtime().getStartTime().toString(),
                "checkInTime", ticket.getCheckInTime().toString()
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
