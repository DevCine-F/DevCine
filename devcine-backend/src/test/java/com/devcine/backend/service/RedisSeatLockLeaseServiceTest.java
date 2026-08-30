package com.devcine.backend.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisSeatLockLeaseServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final RedisSeatLockLeaseService service = new RedisSeatLockLeaseService(redisTemplate);

    @AfterEach
    void shutdownExecutor() {
        service.shutdown();
    }

    @Test
    void acquire_sortsAndLocksEverySeat() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        RedisSeatLockLeaseService.LockLease lease = service.acquire(10, List.of(3, 1, 2));

        assertDoesNotThrow(lease::assertValid);
        verify(valueOperations, times(3)).setIfAbsent(anyString(), anyString(), any(Duration.class));
        lease.close();
    }

    @Test
    void acquire_rejectsSeatOwnedByAnotherRequest() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.acquire(10, List.of(1)));
    }
}
