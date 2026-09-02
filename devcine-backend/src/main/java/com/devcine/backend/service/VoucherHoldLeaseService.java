package com.devcine.backend.service;

import tools.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * Quản lý vòng đời giữ/khóa tạm thời Voucher trên Redis (Redis Voucher Lease)
 * Đảm bảo đồng bộ 2 chiều tức thì giữa quầy POS và đặt vé Online.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherHoldLeaseService {

    private static final String LEASE_PREFIX = "voucher:lease:";
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaseInfo {
        private Integer voucherId;
        private String channel;    // "POS" hoặc "ONLINE"
        private String sessionId;  // Booking ID hoặc POS Session ID
        private Integer customerId;
        private long lockedAt;
        private long ttlSeconds;
    }

    /**
     * Khóa tạm thời voucher cho một phiên (POS hoặc Online)
     */
    public boolean acquire(Integer voucherId, String channel, String sessionId, Integer customerId, long ttlSeconds) {
        if (voucherId == null || sessionId == null) return false;
        String key = LEASE_PREFIX + voucherId;
        long ttl = ttlSeconds > 0 ? ttlSeconds : 600; // Mặc định 10 phút

        LeaseInfo info = new LeaseInfo(voucherId, channel, sessionId, customerId, System.currentTimeMillis(), ttl);
        String json;
        try {
            json = objectMapper.writeValueAsString(info);
        } catch (Exception e) {
            log.error("Lỗi serialize LeaseInfo voucher #{}: {}", voucherId, e.getMessage());
            return false;
        }

        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, json, Duration.ofSeconds(ttl));
            if (Boolean.TRUE.equals(acquired)) {
                log.info("[VoucherLease] Đã cấp khóa voucher #{} cho channel={}, session={}, customer={}",
                        voucherId, channel, sessionId, customerId);
                broadcastVoucherEvent(customerId, "VOUCHER_LEASE_ACQUIRED", voucherId, channel, sessionId);
                return true;
            }

            // Key đã tồn tại -> kiểm tra xem có phải chính session này đang giữ hay không
            String existingJson = redisTemplate.opsForValue().get(key);
            if (existingJson != null) {
                try {
                    LeaseInfo existing = objectMapper.readValue(existingJson, LeaseInfo.class);
                    // 1. Cùng session -> gia hạn TTL
                    if (sessionId.equals(existing.getSessionId())) {
                        redisTemplate.expire(key, Duration.ofSeconds(ttl));
                        return true;
                    }
                    // 2. Cùng khách hàng Online (khách F5 hoặc mở lại tab mới trên web) -> cho phép kế thừa lease
                    if ("ONLINE".equalsIgnoreCase(channel)
                            && "ONLINE".equalsIgnoreCase(existing.getChannel())
                            && customerId != null && customerId.equals(existing.getCustomerId())) {
                        redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttl));
                        log.info("[VoucherLease] Khách hàng #{} Online kế thừa lại khóa voucher #{} từ session cũ {} sang {}",
                                customerId, voucherId, existing.getSessionId(), sessionId);
                        broadcastVoucherEvent(customerId, "VOUCHER_LEASE_ACQUIRED", voucherId, channel, sessionId);
                        return true;
                    }
                    // 3. Khác session / khác kênh -> báo lỗi xung đột
                    String conflictReason = "POS".equalsIgnoreCase(existing.getChannel())
                            ? "Mã ưu đãi đang được áp dụng tại quầy thu ngân."
                            : "Mã ưu đãi đang được giữ trong một phiên đặt vé Online của bạn.";
                    throw new IllegalStateException(conflictReason);
                } catch (IllegalStateException ise) {
                    throw ise;
                } catch (Exception e) {
                    log.warn("[VoucherLease] Không thể đọc lease hiện tại của voucher #{}: {}", voucherId, e.getMessage());
                }
            }
            throw new IllegalStateException("Mã ưu đãi đang được giữ trong một phiên giao dịch khác.");
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            log.warn("[VoucherLease] Redis không khả dụng khi acquire voucher #{}: {}", voucherId, e.getMessage());
            return true; // Fallback an toàn nếu Redis gặp sự cố
        }
    }

    /**
     * Giải phóng khóa voucher khi bấm bỏ chọn hoặc hủy đơn
     */
    public boolean release(Integer voucherId, String sessionId) {
        if (voucherId == null) return false;
        String key = LEASE_PREFIX + voucherId;
        try {
            String existingJson = redisTemplate.opsForValue().get(key);
            if (existingJson != null) {
                LeaseInfo info = objectMapper.readValue(existingJson, LeaseInfo.class);
                if (sessionId == null || sessionId.equals(info.getSessionId())) {
                    redisTemplate.delete(key);
                    log.info("[VoucherLease] Đã giải phóng khóa voucher #{} từ session={}", voucherId, sessionId);
                    broadcastVoucherEvent(info.getCustomerId(), "VOUCHER_LEASE_RELEASED", voucherId, info.getChannel(), sessionId);
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("[VoucherLease] Lỗi khi giải phóng khóa voucher #{}: {}", voucherId, e.getMessage());
        }
        return false;
    }

    /**
     * Kiểm tra xem voucher có đang bị một phiên khác giữ không
     */
    public boolean isHeldByOther(Integer voucherId, String channel, String sessionId, Integer customerId) {
        if (voucherId == null) return false;
        String key = LEASE_PREFIX + voucherId;
        try {
            String existingJson = redisTemplate.opsForValue().get(key);
            if (existingJson == null) return false;
            LeaseInfo info = objectMapper.readValue(existingJson, LeaseInfo.class);
            // Cùng session -> không bị coi là người khác giữ
            if (sessionId != null && sessionId.equals(info.getSessionId())) {
                return false;
            }
            // Cùng khách hàng Online -> không bị coi là người khác giữ
            if ("ONLINE".equalsIgnoreCase(channel)
                    && "ONLINE".equalsIgnoreCase(info.getChannel())
                    && customerId != null && customerId.equals(info.getCustomerId())) {
                return false;
            }
            return true; // Bị phiên khác / kênh khác giữ
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isHeldByOther(Integer voucherId, String sessionId) {
        return isHeldByOther(voucherId, null, sessionId, null);
    }

    /**
     * Lấy lý do chi tiết nếu voucher đang bị giữ
     */
    public String getHoldReason(Integer voucherId, String channel, String sessionId, Integer customerId) {
        if (voucherId == null) return null;
        String key = LEASE_PREFIX + voucherId;
        try {
            String existingJson = redisTemplate.opsForValue().get(key);
            if (existingJson == null) return null;
            LeaseInfo info = objectMapper.readValue(existingJson, LeaseInfo.class);
            if (sessionId != null && sessionId.equals(info.getSessionId())) {
                return null;
            }
            if ("ONLINE".equalsIgnoreCase(channel)
                    && "ONLINE".equalsIgnoreCase(info.getChannel())
                    && customerId != null && customerId.equals(info.getCustomerId())) {
                return null;
            }
            return "POS".equalsIgnoreCase(info.getChannel())
                    ? "Mã ưu đãi đang được áp dụng tại quầy thu ngân."
                    : "Mã ưu đãi đang được giữ trong một phiên đặt vé Online của bạn.";
        } catch (Exception e) {
            return null;
        }
    }

    public String getHoldReason(Integer voucherId, String sessionId) {
        return getHoldReason(voucherId, null, sessionId, null);
    }

    private void broadcastVoucherEvent(Integer customerId, String action, Integer voucherId, String channel, String sessionId) {
        if (customerId == null || messagingTemplate == null) return;
        try {
            String reason = "POS".equalsIgnoreCase(channel)
                    ? "Mã ưu đãi đang được áp dụng tại quầy thu ngân."
                    : "Mã ưu đãi đang được giữ trong một phiên đặt vé Online của bạn.";
            Map<String, Object> payload = Map.of(
                    "action", action,
                    "voucherId", voucherId,
                    "channel", channel != null ? channel : "",
                    "sessionId", sessionId != null ? sessionId : "",
                    "customerId", customerId,
                    "reason", reason,
                    "timestamp", System.currentTimeMillis()
            );
            messagingTemplate.convertAndSend("/topic/customer/" + customerId + "/vouchers", (Object) payload);
            messagingTemplate.convertAndSend("/topic/voucher-updates", (Object) payload);
        } catch (Exception e) {
            log.warn("[VoucherLease] Không thể gửi WebSocket event cho customer #{}: {}", customerId, e.getMessage());
        }
    }
}
