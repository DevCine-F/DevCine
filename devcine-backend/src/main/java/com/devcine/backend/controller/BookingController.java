package com.devcine.backend.controller;

import com.devcine.backend.dto.request.BookingRequestDTO;
import com.devcine.backend.entity.Booking;
import com.devcine.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/hold")
    public ResponseEntity<?> holdSeats(@RequestBody BookingRequestDTO request) {
        try {
            Booking booking = bookingService.holdSeats(request);
            return ResponseEntity.ok(booking);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{bookingId}/payment/complete")
    public ResponseEntity<?> completePayment(@PathVariable Integer bookingId, @RequestParam String paymentMethod) {
        try {
            bookingService.completePayment(bookingId, paymentMethod);
            return ResponseEntity.ok(Map.of("message", "Payment completed and booking confirmed"));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
