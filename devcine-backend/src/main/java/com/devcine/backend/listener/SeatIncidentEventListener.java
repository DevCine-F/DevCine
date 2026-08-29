package com.devcine.backend.listener;

import com.devcine.backend.event.SeatRelocatedEvent;
import com.devcine.backend.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSeatRelocated(SeatRelocatedEvent event) {
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
}
