package com.devcine.backend.controller;

import com.devcine.backend.service.SeatLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * Nhận lệnh khóa ghế real-time qua STOMP từ POS & khách online.
 *
 * <ul>
 *   <li>{@code /app/showtime/{id}/select}  — thử giữ ghế (ai click trước thắng).
 *       Kết quả riêng trả về người gửi qua {@code /user/queue/seat-result}.</li>
 *   <li>{@code /app/showtime/{id}/deselect} — bỏ chọn ghế (nhả khóa).</li>
 *   <li>{@code /app/showtime/{id}/sync}     — lấy danh sách ghế đang bị khóa lúc mới vào.</li>
 * </ul>
 * Việc broadcast LOCKED/RELEASED/SOLD tới cả phòng do {@link SeatLockService} đảm nhiệm.
 */
@Controller
@RequiredArgsConstructor
public class SeatLockController {

    private final SeatLockService seatLockService;

    /** Thử chọn ghế. Trả riêng về người gửi: SELECT_OK nếu giữ được, SELECT_DENIED nếu quầy khác đã giữ. */
    @MessageMapping("/showtime/{showtimeId}/select")
    @SendToUser(destinations = "/queue/seat-result", broadcast = false)
    public Map<String, Object> select(@DestinationVariable Integer showtimeId,
                                      @Payload Map<String, Object> body,
                                      SimpMessageHeaderAccessor headers) {
        Integer seatId = toInt(body.get("seatId"));
        String by = body.get("by") == null ? "quầy khác" : String.valueOf(body.get("by"));
        boolean ok = seatId != null
                && seatLockService.trySelect(showtimeId, seatId, headers.getSessionId(), by);
        return Map.of("type", ok ? "SELECT_OK" : "SELECT_DENIED", "seatId", seatId == null ? -1 : seatId);
    }

    /** Bỏ chọn ghế — nhả khóa nếu phiên này đang giữ. */
    @MessageMapping("/showtime/{showtimeId}/deselect")
    public void deselect(@DestinationVariable Integer showtimeId,
                         @Payload Map<String, Object> body,
                         SimpMessageHeaderAccessor headers) {
        Integer seatId = toInt(body.get("seatId"));
        if (seatId != null) {
            seatLockService.deselect(showtimeId, seatId, headers.getSessionId());
        }
    }

    /** Đồng bộ danh sách ghế đang bị khóa tạm — trả riêng cho người gửi lúc mới vào phòng. */
    @MessageMapping("/showtime/{showtimeId}/sync")
    @SendToUser(destinations = "/queue/seat-sync", broadcast = false)
    public Map<String, Object> sync(@DestinationVariable Integer showtimeId) {
        return Map.of("type", "SEAT_SYNC", "seatIds", seatLockService.lockedSeatIds(showtimeId));
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.valueOf(String.valueOf(v)); } catch (NumberFormatException e) { return null; }
    }
}
