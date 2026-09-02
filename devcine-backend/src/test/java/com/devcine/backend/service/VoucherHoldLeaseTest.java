package com.devcine.backend.service;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VoucherHoldLeaseTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock SimpMessagingTemplate messagingTemplate;

    ObjectMapper objectMapper = new ObjectMapper();
    VoucherHoldLeaseService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new VoucherHoldLeaseService(redisTemplate, messagingTemplate, objectMapper);
    }

    @Test
    @DisplayName("POS giữ mã thành công khi Redis key còn trống")
    void testPosAcquireSuccess() {
        when(valueOperations.setIfAbsent(eq("voucher:lease:100"), anyString(), any(Duration.class)))
                .thenReturn(true);

        boolean ok = service.acquire(100, "POS", "POS_SESSION_1", 55, 600);
        assertTrue(ok);
        verify(valueOperations).setIfAbsent(eq("voucher:lease:100"), anyString(), eq(Duration.ofSeconds(600)));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/customer/55/vouchers"), any(Object.class));
    }

    @Test
    @DisplayName("Online bị từ chối khi POS đang giữ mã đó")
    void testOnlineAcquireFailsWhenPosHolds() throws Exception {
        when(valueOperations.setIfAbsent(eq("voucher:lease:100"), anyString(), any(Duration.class)))
                .thenReturn(false);

        VoucherHoldLeaseService.LeaseInfo posHolder = new VoucherHoldLeaseService.LeaseInfo(
                100, "POS", "POS_SESSION_1", 55, System.currentTimeMillis(), 600
        );
        when(valueOperations.get("voucher:lease:100")).thenReturn(objectMapper.writeValueAsString(posHolder));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.acquire(100, "ONLINE", "BOOKING_99", 55, 600)
        );
        assertEquals("Mã ưu đãi đang được áp dụng tại quầy thu ngân.", ex.getMessage());
    }

    @Test
    @DisplayName("POS bị từ chối khi Online đang giữ mã đó")
    void testPosAcquireFailsWhenOnlineHolds() throws Exception {
        when(valueOperations.setIfAbsent(eq("voucher:lease:100"), anyString(), any(Duration.class)))
                .thenReturn(false);

        VoucherHoldLeaseService.LeaseInfo onlineHolder = new VoucherHoldLeaseService.LeaseInfo(
                100, "ONLINE", "BOOKING_99", 55, System.currentTimeMillis(), 600
        );
        when(valueOperations.get("voucher:lease:100")).thenReturn(objectMapper.writeValueAsString(onlineHolder));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.acquire(100, "POS", "POS_SESSION_2", 55, 600)
        );
        assertEquals("Mã ưu đãi đang được giữ trong một phiên đặt vé Online của bạn.", ex.getMessage());
    }

    @Test
    @DisplayName("Cùng session re-acquire thì gia hạn TTL thành công")
    void testReacquireSameSessionSucceeds() throws Exception {
        when(valueOperations.setIfAbsent(eq("voucher:lease:100"), anyString(), any(Duration.class)))
                .thenReturn(false);

        VoucherHoldLeaseService.LeaseInfo existing = new VoucherHoldLeaseService.LeaseInfo(
                100, "POS", "POS_SESSION_1", 55, System.currentTimeMillis(), 600
        );
        when(valueOperations.get("voucher:lease:100")).thenReturn(objectMapper.writeValueAsString(existing));

        boolean ok = service.acquire(100, "POS", "POS_SESSION_1", 55, 600);
        assertTrue(ok);
        verify(redisTemplate).expire(eq("voucher:lease:100"), eq(Duration.ofSeconds(600)));
    }

    @Test
    @DisplayName("Release thành công đúng session, session khác không được release")
    void testReleaseAuthorization() throws Exception {
        VoucherHoldLeaseService.LeaseInfo info = new VoucherHoldLeaseService.LeaseInfo(
                100, "POS", "POS_SESSION_1", 55, System.currentTimeMillis(), 600
        );
        when(valueOperations.get("voucher:lease:100")).thenReturn(objectMapper.writeValueAsString(info));

        // Session khác cố release -> false, không xóa key
        boolean wrong = service.release(100, "WRONG_SESSION");
        assertFalse(wrong);
        verify(redisTemplate, never()).delete("voucher:lease:100");

        // Đúng session -> xóa key
        boolean correct = service.release(100, "POS_SESSION_1");
        assertTrue(correct);
        verify(redisTemplate).delete("voucher:lease:100");
    }
}
