package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
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
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getTicketsByBooking(@PathVariable Integer bookingId) {
        try {
            List<Ticket> tickets = ticketService.getTicketsByBooking(bookingId);
            return ResponseEntity.ok(ApiResponse.ok(tickets));
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        }
    }

    /**
     * Quét/tra cứu mã đặt vé để XÁC MINH đơn (chưa in). Trả chi tiết để hiển thị "Quét thành công".
     * Không đánh dấu đã in — việc in do endpoint /print thực hiện khi nhân viên bấm nút.
     */
    @PostMapping("/lookup")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','MANAGER')")
    public ResponseEntity<?> lookupBooking(@RequestParam("code") String code) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(ticketService.lookupByBookingCode(code)));
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.ok(ticketService.printByBookingCode(code)));
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        }
    }
}
