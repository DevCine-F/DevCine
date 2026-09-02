package com.devcine.backend.scheduler;

import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.BookingSeat;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tự động giải phóng các ghế đang giữ (HOLD) đã quá thời gian cấu hình.
 * Chạy định kỳ mỗi phút; mốc hết hạn lấy theo SystemSetting (SEAT_HOLD_MINUTES).
 * Nhờ job này, ghế bị bỏ dở sẽ nhả ĐÚNG GIỜ kể cả khi không có khách khác chọn lại.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldCleanupScheduler {

    private final BookingSeatRepository bookingSeatRepository;
    private final BookingRepository bookingRepository;
    private final SystemSettingService systemSettingService;

    @Scheduled(fixedDelayString = "${devcine.seat-hold.cleanup-ms:60000}")
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        int holdMinutes = systemSettingService.getSeatHoldMinutes();
        LocalDateTime cutoff = now.minusMinutes(holdMinutes);

        List<BookingSeat> staleSeats = bookingSeatRepository.findStaleHolds(now, cutoff);
        if (staleSeats.isEmpty()) {
            return;
        }

        Set<Booking> staleBookings = new LinkedHashSet<>();
        for (BookingSeat seat : staleSeats) {
            seat.setStatus("EXPIRED");
            if (seat.getBooking() != null) {
                staleBookings.add(seat.getBooking());
            }
        }
        bookingSeatRepository.saveAll(staleSeats);

        // Đơn còn ở trạng thái HOLD nhưng đã quá hạn → đánh dấu EXPIRED để không treo rác
        staleBookings.forEach(b -> {
            if ("HOLD".equals(b.getStatus())) {
                b.setStatus("EXPIRED");
            }
        });
        bookingRepository.saveAll(staleBookings);

        log.info("Đã giải phóng {} ghế giữ quá hạn (> {} phút), {} đơn chuyển EXPIRED.",
                staleSeats.size(), holdMinutes, staleBookings.size());
    }
}
