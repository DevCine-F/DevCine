package com.devcine.backend.listener;

import com.devcine.backend.event.SeatRelocatedEvent;
import com.devcine.backend.service.SeatLockService;
import com.devcine.backend.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener xử lý các tác vụ nền sau khi sự cố đổi ghế được commit thành công vào cơ sở dữ liệu.
 * Đảm bảo tách biệt 100% thời gian gọi mạng SMTP gửi Email ra ngoài Transaction và Redis Lock.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatIncidentEventListener {

    private final TicketService ticketService;
    private final SeatLockService seatLockService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSeatRelocated(SeatRelocatedEvent event) {
        broadcastCommittedSeatChanges(event);
        log.info("[SeatIncident] Bắt đầu gửi email sự cố sau commit cho đơn #{}", event.bookingId());
        try {
            ticketService.sendIncidentRelocateEmailIfOnline(
                    event.bookingId(),
                    event.reason(),
                    event.swaps(),
                    event.comp(),
                    event.voucherLabel()
            );
        } catch (Exception e) {
            log.error("[SeatIncident] Lỗi gửi email nền cho đơn #{}: {}", event.bookingId(), e.getMessage(), e);
        }
    }

    private void broadcastCommittedSeatChanges(SeatRelocatedEvent event) {
        if (event.showtimeId() == null) return;
        seatLockService.markSold(event.showtimeId(), event.newSeatIds());
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", event.oldSeatsLockedForMaintenance() ? "SEAT_MAINTENANCE" : "SEAT_RELEASED");
            payload.put("seatIds", event.oldSeatIds());
            payload.put("by", "INCIDENT_HANDLER");
            if (event.oldSeatsLockedForMaintenance()) payload.put("status", "MAINTENANCE");
            messagingTemplate.convertAndSend("/topic/showtime/" + event.showtimeId(), (Object) payload);
        } catch (Exception exception) {
            log.warn("[SeatIncident] Broadcast trạng thái ghế sau commit cho suất #{} thất bại: {}",
                    event.showtimeId(), exception.getMessage());
        }
    }
}
