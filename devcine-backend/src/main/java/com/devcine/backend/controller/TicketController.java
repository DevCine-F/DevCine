package com.devcine.backend.controller;

import com.devcine.backend.entity.Ticket;
import com.devcine.backend.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Quét QR/nhập mã đặt vé tại quầy → in toàn bộ vé giấy cho đơn & đánh dấu đã in.
     * Nhận mã đặt vé (booking_code) — 1 mã QR đại diện cả đơn, không phải từng ghế.
     */
    @PostMapping("/print")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','MANAGER')")
    public ResponseEntity<?> printTickets(@RequestParam("code") String code) {
        try {
            return ResponseEntity.ok(ticketService.printByBookingCode(code));
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
