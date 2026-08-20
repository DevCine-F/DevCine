package com.devcine.backend.scheduler;

import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.BookingSeat;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.service.BookingService;
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
    private final BookingService bookingService;

    /**
     * Fallback Safety Net: Chạy dự phòng 1 giờ/lần để dọn các đơn sót nếu Redis bị gián đoạn.
     * 99.99% sự kiện nhả đơn thời gian thực đã được xử lý ngay lập tức bởi BookingExpirationListener (Redis Event).
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findExpiredHolds(now);
        if (!expiredBookings.isEmpty()) {
            log.info("Safety-net scheduler tìm thấy {} đơn hết hạn sót lại -> Tiến hành dọn dẹp", expiredBookings.size());
            for (Booking booking : expiredBookings) {
                try {
                    bookingService.expireBooking(booking.getId());
                } catch (Exception e) {
                    log.error("Lỗi khi safety-net dọn dẹp booking {}", booking.getId(), e);
                }
            }
        }
    }
}
