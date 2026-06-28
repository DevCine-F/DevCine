package com.devcine.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket (STOMP) cho cơ chế khóa ghế real-time.
 *
 * <p>Client kết nối tới {@code /ws} (raw WebSocket — không SockJS, FE dùng @stomp/stompjs).
 * <ul>
 *   <li>Gửi lệnh tới server: prefix {@code /app} (vd {@code /app/showtime/123/select}).</li>
 *   <li>Nhận broadcast: topic {@code /topic/showtime/{id}} — mọi quầy POS & khách đang xem suất đó.</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker đơn giản trong bộ nhớ — đủ cho 1 instance backend (không cần Redis/RabbitMQ)
        // /topic: broadcast cả phòng · /queue: phản hồi riêng cho người gửi (@SendToUser)
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
