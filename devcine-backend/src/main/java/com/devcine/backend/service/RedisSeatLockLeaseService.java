package com.devcine.backend.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis lock lease theo từng ghế, có ownership token và tự gia hạn trong lúc transaction chạy.
 * DB pessimistic lock vẫn là lớp fallback khi Redis tạm thời không khả dụng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSeatLockLeaseService {

    private static final String LOCK_PREFIX = "lock:showtime:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final long RENEW_INTERVAL_SECONDS = 10;

    private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end", Long.class);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ScheduledExecutorService renewExecutor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "incident-seat-lock-renew");
        thread.setDaemon(true);
        return thread;
    });

    public LockLease acquire(Integer showtimeId, List<Integer> seatIds) {
        if (showtimeId == null || seatIds == null || seatIds.isEmpty()) return LockLease.noOp();

        List<Integer> orderedSeatIds = seatIds.stream().distinct().sorted(Comparator.naturalOrder()).toList();
        String ownerToken = "INCIDENT:" + UUID.randomUUID();
        List<String> acquiredKeys = new ArrayList<>();
        try {
            for (Integer seatId : orderedSeatIds) {
                String key = LOCK_PREFIX + showtimeId + ":seat:" + seatId;
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, ownerToken, LOCK_TTL);
                if (!Boolean.TRUE.equals(acquired)) {
                    releaseOwnedKeys(acquiredKeys, ownerToken);
                    throw new IllegalStateException(
                            "Ghế #" + seatId + " đang được xử lý đồng thời bởi giao dịch khác. Vui lòng thử lại sau.");
                }
                acquiredKeys.add(key);
            }
            return new RedisLockLease(List.copyOf(acquiredKeys), ownerToken);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            releaseOwnedKeys(acquiredKeys, ownerToken);
            log.warn("[SeatIncident] Redis không khả dụng, chuyển sang DB pessimistic lock: {}",
                    exception.getMessage());
            return LockLease.noOp();
        }
    }

    private void renewOwnedKeys(List<String> keys, String ownerToken, AtomicBoolean valid) {
        if (!valid.get()) return;
        try {
            for (String key : keys) {
                Long renewed = redisTemplate.execute(
                        RENEW_SCRIPT, List.of(key), ownerToken, String.valueOf(LOCK_TTL.toMillis()));
                if (!Long.valueOf(1L).equals(renewed)) {
                    valid.set(false);
                    log.error("[SeatIncident] Mất quyền sở hữu Redis lock {} trước khi transaction hoàn tất.", key);
                    return;
                }
            }
        } catch (Exception exception) {
            valid.set(false);
            log.error("[SeatIncident] Không thể gia hạn Redis seat lock: {}", exception.getMessage(), exception);
        }
    }

    private void releaseOwnedKeys(List<String> keys, String ownerToken) {
        for (String key : keys) {
            try {
                redisTemplate.execute(RELEASE_SCRIPT, List.of(key), ownerToken);
            } catch (Exception exception) {
                log.warn("[SeatIncident] Không thể giải phóng Redis lock {}: {}", key, exception.getMessage());
            }
        }
    }

    @PreDestroy
    void shutdown() {
        renewExecutor.shutdownNow();
    }

    public interface LockLease extends AutoCloseable {
        void assertValid();

        @Override
        void close();

        static LockLease noOp() {
            return NoOpLockLease.INSTANCE;
        }
    }

    private enum NoOpLockLease implements LockLease {
        INSTANCE;

        @Override
        public void assertValid() {}

        @Override
        public void close() {}
    }

    private final class RedisLockLease implements LockLease {
        private final List<String> keys;
        private final String ownerToken;
        private final AtomicBoolean valid = new AtomicBoolean(true);
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final ScheduledFuture<?> renewTask;

        private RedisLockLease(List<String> keys, String ownerToken) {
            this.keys = keys;
            this.ownerToken = ownerToken;
            this.renewTask = renewExecutor.scheduleAtFixedRate(
                    () -> renewOwnedKeys(keys, ownerToken, valid),
                    RENEW_INTERVAL_SECONDS,
                    RENEW_INTERVAL_SECONDS,
                    TimeUnit.SECONDS);
        }

        @Override
        public void assertValid() {
            if (!valid.get()) {
                throw new IllegalStateException(
                        "Khóa xử lý ghế đã mất hiệu lực. Giao dịch được hủy để tránh bán trùng ghế.");
            }
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) return;
            valid.set(false);
            renewTask.cancel(false);
            releaseOwnedKeys(keys, ownerToken);
        }
    }
}
