package com.devcine.backend.service;

import com.devcine.backend.dto.request.BookingRequestDTO;
import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingOrderService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingService bookingService;
    private final PosHoldService posHoldService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    // Penalty Cache: key = "penalty:{posTerminalId}:{showtimeId}:{seatId}", value = expiresAt (System.currentTimeMillis + 5 mins)
    private final Map<String, Long> penaltyCache = new ConcurrentHashMap<>();

    private void cleanExpiredPenalties() {
        long now = System.currentTimeMillis();
        penaltyCache.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    public void applyPenalty(String posTerminalId, Integer showtimeId, Integer seatId) {
        cleanExpiredPenalties();
        penaltyCache.put("penalty:" + posTerminalId + ":" + showtimeId + ":" + seatId, System.currentTimeMillis() + 300_000);
        try {
            redisTemplate.opsForValue().set("penalty:" + posTerminalId + ":" + showtimeId + ":" + seatId, "1", 300, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Không thể lưu penalty vào Redis: {}", e.getMessage());
        }
    }

    private boolean isPenalized(String posTerminalId, Integer showtimeId, Integer seatId) {
        cleanExpiredPenalties();
        Long expiresAt = penaltyCache.get("penalty:" + posTerminalId + ":" + showtimeId + ":" + seatId);
        if (expiresAt != null && expiresAt > System.currentTimeMillis()) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("penalty:" + posTerminalId + ":" + showtimeId + ":" + seatId));
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public Booking holdOrder(BookingRequestDTO req, Staff soldBy, String posTerminalId) {
        if (posTerminalId == null || posTerminalId.isBlank()) {
            throw new RuntimeException("Thiếu posTerminalId");
        }
        
        long pendingCount = bookingRepository.countPendingByPosTerminalId(posTerminalId);
        if (pendingCount >= 3) {
            throw new RuntimeException("Đã đạt giới hạn 3 đơn chờ cho máy POS này. Vui lòng thanh toán hoặc hủy đơn cũ.");
        }

        Integer showtimeId = req.getShowtimeId();
        List<Integer> seatIds = req.getSeatIds();
        
        for (Integer seatId : seatIds) {
            if (isPenalized(posTerminalId, showtimeId, seatId)) {
                throw new RuntimeException("Ghế bạn chọn đang bị phạt 5 phút do đơn giữ cũ quá hạn. Vui lòng chọn ghế khác.");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<Integer> conflictingSeatIds = bookingSeatRepository.findConflictingSeats(showtimeId, seatIds, now);
        if (!conflictingSeatIds.isEmpty()) {
            throw new RuntimeException("Một số ghế đã bị đặt hoặc giữ bởi người khác.");
        }

        // Tạo đơn giữ
        Booking booking = bookingService.holdSeatsForStaff(req, soldBy);
        
        LocalDateTime expiresAt = now.plusMinutes(10);
        LocalDateTime showtimeStart = booking.getShowtime().getStartTime();
        if (showtimeStart.isBefore(expiresAt)) {
            expiresAt = showtimeStart; // Giới hạn tối đa bằng đúng giờ khởi chiếu
        }
        if (expiresAt.isBefore(now) || expiresAt.isEqual(now)) {
            throw new RuntimeException("Suất chiếu đã quá sát giờ hoặc đang diễn ra, không thể giữ đơn.");
        }

        booking.setStatus("PENDING_PAYMENT");
        booking.setPosTerminalId(posTerminalId);
        booking.setExpiresAt(expiresAt);
        bookingRepository.save(booking);

        try {
            long ttlSeconds = java.time.Duration.between(now, expiresAt).getSeconds();
            if (ttlSeconds > 0) {
                redisTemplate.opsForValue().set("booking:hold:" + booking.getId(), "HOLD", ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Không thể đăng ký TTL Redis cho POS booking #{}: {}", booking.getId(), e.getMessage());
        }

        return booking;
    }

    @Transactional
    public void payIntent(Integer bookingId, String posTerminalId) {
        Booking booking = bookingRepository.findByIdWithPessimisticLock(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!booking.getPosTerminalId().equals(posTerminalId)) {
            throw new RuntimeException("Đơn hàng không thuộc máy POS này");
        }

        LocalDateTime now = LocalDateTime.now();
        if (booking.getExpiresAt().isBefore(now.minusSeconds(30))) {
            throw new RuntimeException("Đơn hàng đã hết hạn");
        }

        booking.setStatus("PAYING");
        booking.setExpiresAt(now.plusMinutes(2)); // Gia hạn thêm 2 phút để thanh toán
        bookingRepository.save(booking);

        try {
            redisTemplate.opsForValue().set("booking:hold:" + booking.getId(), "HOLD", 120, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Không thể gia hạn TTL Redis cho POS booking #{}: {}", booking.getId(), e.getMessage());
        }
    }
    
    @Transactional(readOnly = true)
    public List<Booking> getPendingOrders(String posTerminalId) {
        return bookingRepository.findPendingByPosTerminalId(posTerminalId);
    }
    
    @Transactional
    public void cancelHold(Integer bookingId, String posTerminalId) {
        Booking booking = bookingRepository.findByIdWithPessimisticLock(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
                
        if (!booking.getPosTerminalId().equals(posTerminalId)) {
            throw new RuntimeException("Đơn hàng không thuộc máy POS này");
        }
        
        posHoldService.releaseHold(bookingId); // Reuses the logic to release seats and emit WS
        try {
            redisTemplate.delete("booking:hold:" + bookingId);
        } catch (Exception e) {
            log.warn("Không thể xóa TTL Redis cho POS booking #{}: {}", bookingId, e.getMessage());
        }
    }
    
    @Transactional
    public void finalizePayment(Integer bookingId, String posTerminalId, String paymentMethod) {
        Booking booking = bookingRepository.findByIdWithPessimisticLock(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!booking.getPosTerminalId().equals(posTerminalId)) {
            throw new RuntimeException("Đơn hàng không thuộc máy POS này");
        }
        
        // Let's assume Fnb validation is handled inside bookingService.completePayment 
        // Note: completePayment updates the booking status to CONFIRMED.
        bookingService.completePayment(booking.getId(), paymentMethod);
    }
}
