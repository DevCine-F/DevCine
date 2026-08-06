package com.devcine.backend.service;

import com.devcine.backend.entity.Booking;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Collections;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PosHoldServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private PosHoldService posHoldService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testReleaseHold_WhenStatusIsExpired_ShouldReturnReleasedAndNotExecuteUpdate() {
        // Arrange
        Integer bookingId = 1;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus("EXPIRED");

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // Act
        String result = posHoldService.releaseHold(bookingId);

        // Assert
        assertEquals("RELEASED", result);
        
        // Verify repository methods are NOT called (Idempotent)
        verify(bookingSeatRepository, never()).findAllByBookingIdWithSeat(any());
        verify(bookingSeatRepository, never()).saveAll(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void testReleaseHold_WhenStatusIsHold_ShouldUpdateToExpiredAndReturnReleased() {
        // Arrange
        Integer bookingId = 2;
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus("HOLD");

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findAllByBookingIdWithSeat(bookingId)).thenReturn(Collections.emptyList());

        // Act
        String result = posHoldService.releaseHold(bookingId);

        // Assert
        assertEquals("RELEASED", result);
        assertEquals("CANCELLED", booking.getStatus());
        
        // Verify repository methods ARE called
        verify(bookingSeatRepository, times(1)).findAllByBookingIdWithSeat(bookingId);
        verify(bookingSeatRepository, times(1)).saveAll(any());
        verify(bookingRepository, times(1)).save(booking);
    }
}
