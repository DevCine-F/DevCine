package com.devcine.backend.service;

import com.devcine.backend.repository.BookingSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Khóa ghế tạm thời (transient) khi người dùng CLICK chọn ghế — chống xung đột đồng thời
 * giữa các quầy POS và khách online, theo nguyên tắc "ai click trước, người đó giữ ghế".
 *
 * <p>Trạng thái giữ trong RAM (đủ cho 1 instance backend, không cần Redis). Đây là lớp giữ
 * <em>tạm</em> ở bước chọn ghế; khi thanh toán, ghế mới được chốt vào DB (HOLD/SOLD) qua
 * {@code BookingService.holdSeats} (đã có khóa bi quan). Khóa tạm tự nhả khi: bỏ chọn,
 * client ngắt kết nối, ghế được bán, hoặc quá hạn TTL (an toàn cho kết nối treo).
 *
 * <p>Mọi thay đổi đều được broadcast tới {@code /topic/showtime/{id}} để các màn khác cập nhật.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final SimpMessagingTemplate messaging;
    private final BookingSeatRepository bookingSeatRepository;

    /** Khóa quá hạn sau 10 phút (an toàn cho kết nối nửa-mở; bình thường nhả khi disconnect). */
    private static final long LOCK_TTL_MS = 10 * 60_000L;

    /** showtimeId → (seatId → khóa). ConcurrentHashMap lồng nhau để thao tác atomic theo từng ghế. */
    private final Map<Integer, Map<Integer, Lock>> locksByShowtime = new ConcurrentHashMap<>();

    /** Một khóa ghế: thuộc về phiên WebSocket nào, nhãn hiển thị (vd "Quầy POS"), thời điểm khóa. */
    private record Lock(String sessionId, String by, long lockedAt) {}

    private boolean isExpired(Lock l) {
        return System.currentTimeMillis() - l.lockedAt() > LOCK_TTL_MS;
    }

    private Map<Integer, Lock> showLocks(Integer showtimeId) {
        return locksByShowtime.computeIfAbsent(showtimeId, k -> new ConcurrentHashMap<>());
    }

    /**
     * Thử khóa 1 ghế (atomic). Nếu thành công (hoặc phiên này đã giữ sẵn) → broadcast LOCKED và trả true.
     * Nếu ghế đang bị PHIÊN KHÁC giữ → trả false (không broadcast), client gọi sẽ nhận DENIED.
     */
    public boolean trySelect(Integer showtimeId, Integer seatId, String sessionId, String by) {
        Map<Integer, Lock> seatLocks = showLocks(showtimeId);
        // compute giữ lock bin theo seatId → đảm bảo chỉ một phiên thắng khi click cùng lúc
        Lock winner = seatLocks.compute(seatId, (k, existing) -> {
            if (existing != null && !isExpired(existing) && !existing.sessionId().equals(sessionId)) {
                return existing; // ghế đã có chủ khác còn hiệu lực → giữ nguyên
            }
            return new Lock(sessionId, by, System.currentTimeMillis());
        });
        boolean acquired = winner.sessionId().equals(sessionId);
        if (acquired) {
            broadcast(showtimeId, "SEAT_LOCKED", List.of(seatId), by);
        }
        return acquired;
    }

    /** Bỏ chọn 1 ghế — chỉ phiên đang giữ mới nhả được. Broadcast RELEASED nếu có nhả và không có đơn DB hold. */
    public void deselect(Integer showtimeId, Integer seatId, String sessionId) {
        Map<Integer, Lock> seatLocks = locksByShowtime.get(showtimeId);
        if (seatLocks == null) return;
        Lock existing = seatLocks.get(seatId);
        if (existing != null && existing.sessionId().equals(sessionId) && seatLocks.remove(seatId, existing)) {
            List<Integer> dbHeld = bookingSeatRepository.findConflictingSeats(
                    showtimeId, List.of(seatId), LocalDateTime.now());
            if (dbHeld == null || !dbHeld.contains(seatId)) {
                broadcast(showtimeId, "SEAT_RELEASED", List.of(seatId), null);
            }
        }
    }

    /**
     * Nhả toàn bộ ghế của một phiên (khi WebSocket ngắt) — broadcast RELEASED theo từng suất.
     * DB-Aware: Nếu ghế đã được tạo đơn giữ chỗ DB (HOLD còn hạn) thì không broadcast nhả.
     */
    public void releaseSession(String sessionId) {
        locksByShowtime.forEach((showtimeId, seatLocks) -> {
            List<Integer> released = new ArrayList<>();
            seatLocks.forEach((seatId, lock) -> {
                if (lock.sessionId().equals(sessionId) && seatLocks.remove(seatId, lock)) {
                    released.add(seatId);
                }
            });
            if (!released.isEmpty()) {
                List<Integer> dbHeld = bookingSeatRepository.findConflictingSeats(
                        showtimeId, released, LocalDateTime.now());
                List<Integer> trulyReleased = released.stream()
                        .filter(id -> dbHeld == null || !dbHeld.contains(id))
                        .collect(Collectors.toList());
                if (!trulyReleased.isEmpty()) {
                    broadcast(showtimeId, "SEAT_RELEASED", trulyReleased, null);
                }
            }
        });
    }

    /** Ghế đã được BÁN (thanh toán xong) — xóa khóa tạm và broadcast SOLD để mọi màn khóa cứng. */
    public void markSold(Integer showtimeId, List<Integer> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) return;
        Map<Integer, Lock> seatLocks = locksByShowtime.get(showtimeId);
        if (seatLocks != null) seatIds.forEach(seatLocks::remove);
        broadcast(showtimeId, "SEAT_SOLD", seatIds, null);
    }

    /** Danh sách ghế đang bị khóa tạm (còn hiệu lực) của một suất — cho client đồng bộ lúc mới vào. */
    public List<Integer> lockedSeatIds(Integer showtimeId) {
        Map<Integer, Lock> seatLocks = locksByShowtime.get(showtimeId);
        if (seatLocks == null) return List.of();
        List<Integer> out = new ArrayList<>();
        seatLocks.forEach((seatId, lock) -> { if (!isExpired(lock)) out.add(seatId); });
        return out;
    }

    private void broadcast(Integer showtimeId, String type, List<Integer> seatIds, String by) {
        try {
            Object payload = Map.of("type", type, "seatIds", seatIds, "by", by == null ? "" : by);
            messaging.convertAndSend("/topic/showtime/" + showtimeId, payload);
        } catch (Exception e) {
            // Best-effort: lỗi broadcast không được phá vỡ nghiệp vụ gọi (vd thanh toán)
            log.warn("Broadcast {} cho suất #{} thất bại: {}", type, showtimeId, e.getMessage());
        }
    }

    /** Quét nhả khóa quá hạn (kết nối treo không gửi disconnect) — chạy định kỳ. */
    @Scheduled(fixedDelayString = "${devcine.seat-lock.sweep-ms:60000}")
    public void sweepExpired() {
        locksByShowtime.forEach((showtimeId, seatLocks) -> {
            List<Integer> released = new ArrayList<>();
            seatLocks.forEach((seatId, lock) -> {
                if (isExpired(lock) && seatLocks.remove(seatId, lock)) {
                    released.add(seatId);
                }
            });
            if (!released.isEmpty()) {
                log.info("Nhả {} ghế khóa quá hạn ở suất #{}.", released.size(), showtimeId);
                List<Integer> dbHeld = bookingSeatRepository.findConflictingSeats(
                        showtimeId, released, LocalDateTime.now());
                List<Integer> trulyReleased = released.stream()
                        .filter(id -> dbHeld == null || !dbHeld.contains(id))
                        .collect(Collectors.toList());
                if (!trulyReleased.isEmpty()) {
                    broadcast(showtimeId, "SEAT_RELEASED", trulyReleased, null);
                }
            }
        });
    }
}
