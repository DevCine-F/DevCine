package com.devcine.backend.service;

import com.devcine.backend.entity.AuditLog;
import com.devcine.backend.entity.User;
import com.devcine.backend.repository.AuditLogRepository;
import com.devcine.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Ghi nhật ký hệ thống (Audit Log) cho các thao tác quản trị quan trọng.
 * Dùng REQUIRES_NEW để việc ghi log luôn độc lập với transaction nghiệp vụ chính
 * (không làm rollback nghiệp vụ nếu ghi log lỗi, và chạy được cả trong context readOnly).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Integer userId, String action, String targetTable, String ipAddress) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .targetTable(truncate(targetTable, 100))
                    .ipAddress(truncate(ipAddress, 45))
                    .timestamp(LocalDateTime.now())
                    .build();
            if (userId != null) {
                User userRef = userRepository.getReferenceById(userId);
                entry.setUser(userRef);
            }
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Ghi audit không bao giờ được phép làm hỏng nghiệp vụ chính
            log.error("Không thể ghi audit log (action={}, target={})", action, targetTable, e);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
