package com.devcine.backend.config;

import com.devcine.backend.service.SeatLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Khi một client WebSocket ngắt kết nối (đóng tab, mất mạng, chuyển bước) → nhả toàn bộ ghế
 * mà phiên đó đang khóa tạm, để các quầy khác chọn được ngay (không phải chờ TTL).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SeatLockService seatLockService;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        if (sessionId != null) {
            seatLockService.releaseSession(sessionId);
        }
    }
}
