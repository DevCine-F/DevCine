package com.devcine.backend.service;

import com.devcine.backend.dto.request.BookingRequestDTO;
import com.devcine.backend.dto.request.FnbSelectionDTO;
import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingFnbRepository bookingFnbRepository;
    private final SeatRepository seatRepository;
    private final FnbItemRepository fnbItemRepository;
    private final ShowtimeRepository showtimeRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public Booking holdSeats(BookingRequestDTO request) {
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new RuntimeException("Showtime not found"));

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElse(null);
        }

        // Validate seats
        List<BookingSeat> existingReservedSeats = bookingSeatRepository.findReservedSeatsByShowtime(request.getShowtimeId());
        for (BookingSeat reserved : existingReservedSeats) {
            if (request.getSeatIds().contains(reserved.getSeat().getId())) {
                // If it's on HOLD but older than 10 minutes, we can override it (pretend it's free)
                if ("HOLD".equals(reserved.getStatus()) && 
                    reserved.getBooking().getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
                    continue; 
                }
                throw new RuntimeException("Seat " + reserved.getSeat().getId() + " is already taken or on hold.");
            }
        }

        Booking booking = Booking.builder()
                .customer(customer)
                .showtime(showtime)
                .bookingCode(UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                .status("HOLD") // Initial status
                .createdAt(LocalDateTime.now())
                .paymentMethod(request.getPaymentMethod())
                .totalPrice(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .build();
                
        bookingRepository.save(booking);

        BigDecimal totalPrice = BigDecimal.ZERO;

        // Process Seats
        if (request.getSeatIds() != null) {
            for (Integer seatId : request.getSeatIds()) {
                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new RuntimeException("Seat not found"));
                BigDecimal seatPrice = seat.getSeatType().getPriceModifier(); // Simplified pricing
                
                BookingSeat bookingSeat = BookingSeat.builder()
                        .booking(booking)
                        .seat(seat)
                        .priceSnapshot(seatPrice)
                        .status("HOLD")
                        .build();
                bookingSeatRepository.save(bookingSeat);
                totalPrice = totalPrice.add(seatPrice);
            }
        }

        // Process F&B
        if (request.getFnbs() != null) {
            for (FnbSelectionDTO fnbDTO : request.getFnbs()) {
                FnbItem item = fnbItemRepository.findById(fnbDTO.getFnbItemId())
                        .orElseThrow(() -> new RuntimeException("F&B Item not found"));
                BigDecimal itemTotal = item.getPrice().multiply(new BigDecimal(fnbDTO.getQuantity()));
                
                BookingFnb bookingFnb = BookingFnb.builder()
                        .booking(booking)
                        .fnbItem(item)
                        .quantity(fnbDTO.getQuantity())
                        .priceSnapshot(item.getPrice())
                        .build();
                bookingFnbRepository.save(bookingFnb);
                totalPrice = totalPrice.add(itemTotal);
            }
        }

        booking.setTotalPrice(totalPrice);
        booking.setFinalPrice(totalPrice); // Apply voucher logic later
        bookingRepository.save(booking);
        
        // TODO: Schedule a task to release the seats after X minutes if not paid
        
        return booking;
    }
    
    @Transactional
    public void completePayment(Integer bookingId, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
                
        booking.setStatus("CONFIRMED");
        booking.setPaymentMethod(paymentMethod);
        bookingRepository.save(booking);
        
        // Update seat status
        List<BookingSeat> seats = bookingSeatRepository.findAllByBookingId(bookingId);
        for (BookingSeat bs : seats) {
            bs.setStatus("SOLD");
            bookingSeatRepository.save(bs);
        }
    }
}
