package com.devcine.backend.scheduler;

import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.BookingSeat;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.service.PendingOrderService;
import com.devcine.backend.service.PosHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCleanupTask {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PendingOrderService pendingOrderService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedDelay = 5000) // Chạy mỗi 5 giây
    @Transactional
    public void cleanupExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findExpiredHolds(now);
        
        for (Booking booking : expiredBookings) {
            try {
                // Pessimistic Lock in findByIdWithPessimisticLock
                Booking lockedBooking = bookingRepository.findByIdWithPessimisticLock(booking.getId()).orElse(null);
                if (lockedBooking != null && (lockedBooking.getStatus().equals("PENDING_PAYMENT") || lockedBooking.getStatus().equals("PAYING") || lockedBooking.getStatus().equals("HOLD")) && lockedBooking.getExpiresAt().isBefore(now)) {
                    
                    lockedBooking.setStatus("EXPIRED");
                    bookingRepository.save(lockedBooking);

                    List<BookingSeat> seats = bookingSeatRepository.findAllByBookingIdWithSeat(lockedBooking.getId());
                    for (BookingSeat bs : seats) {
                        if ("HOLD".equals(bs.getStatus())) {
                            bs.setStatus("EXPIRED");
                            // Apply penalty cache
                            if (lockedBooking.getPosTerminalId() != null) {
                                pendingOrderService.applyPenalty(lockedBooking.getPosTerminalId(), lockedBooking.getShowtime().getId(), bs.getSeat().getId());
                            }
                        }
                    }
                    bookingSeatRepository.saveAll(seats);

                    log.info("Cron job đã dọn dẹp đơn quá hạn: booking #{}, posTerminalId: {}", lockedBooking.getId(), lockedBooking.getPosTerminalId());

                    try {
                        List<Integer> seatIds = seats.stream().map(bs -> bs.getSeat().getId()).collect(Collectors.toList());
                        Object payload = java.util.Map.of("type", "SEAT_RELEASED", "seatIds", seatIds, "by", "SYSTEM_CLEANUP");
                        messagingTemplate.convertAndSend("/topic/showtime/" + lockedBooking.getShowtime().getId(), payload);
                    } catch (Exception e) {
                        log.warn("Mạng WebSocket ngắt kết nối đột ngột khi cleanup booking #{}: {}", lockedBooking.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Lỗi khi dọn dẹp booking {}", booking.getId(), e);
            }
        }
    }
}
