package com.devcine.backend.listener;

import com.devcine.backend.service.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookingExpirationListener extends KeyExpirationEventMessageListener {

    public static final String BOOKING_HOLD_PREFIX = "booking:hold:";

    private final BookingService bookingService;

    public BookingExpirationListener(RedisMessageListenerContainer listenerContainer,
                                     BookingService bookingService) {
        super(listenerContainer);
        this.bookingService = bookingService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.debug("Nhận sự kiện Redis key hết hạn: {}", expiredKey);

        if (expiredKey != null && expiredKey.startsWith(BOOKING_HOLD_PREFIX)) {
            try {
                String idStr = expiredKey.substring(BOOKING_HOLD_PREFIX.length());
                Integer bookingId = Integer.parseInt(idStr);
                log.info("Redis key '{}' đã hết hạn TTL -> Tự động nhả đơn giữ chỗ #{}", expiredKey, bookingId);
                bookingService.expireBooking(bookingId);
            } catch (NumberFormatException e) {
                log.warn("Không thể parse bookingId từ key hết hạn '{}': {}", expiredKey, e.getMessage());
            } catch (Exception e) {
                log.error("Lỗi khi xử lý nhả đơn giữ chỗ #{} từ Redis event: {}", expiredKey, e.getMessage(), e);
            }
        }
    }
}
