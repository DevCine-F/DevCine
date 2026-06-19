package com.devcine.backend.service;

import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.Notification;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getForCustomer(Integer customerId) {
        return notificationRepository.findByCustomer_UserIdOrderByCreatedAtDesc(customerId).stream()
                .map(n -> Map.<String, Object>of(
                        "id", n.getId(),
                        "title", n.getTitle() != null ? n.getTitle() : "",
                        "message", n.getMessage() != null ? n.getMessage() : "",
                        "type", n.getType() != null ? n.getType() : "SYSTEM",
                        "isRead", Boolean.TRUE.equals(n.getIsRead()),
                        "createdAt", n.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Integer customerId) {
        return notificationRepository.countByCustomer_UserIdAndIsReadFalse(customerId);
    }

    @Transactional
    public void markAsRead(Integer notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Integer customerId) {
        notificationRepository.markAllAsRead(customerId);
    }

    /**
     * Tạo thông báo cho khách hàng. Không ném lỗi ra ngoài để không ảnh hưởng
     * nghiệp vụ chính (vd: đặt vé) khi tạo thông báo gặp sự cố.
     */
    @Transactional
    public void notifyCustomer(Integer customerId, String title, String message, String type) {
        try {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) return;
            Notification notification = Notification.builder()
                    .customer(customer)
                    .title(title)
                    .message(message)
                    .type(type)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Không thể tạo thông báo cho khách {}: {}", customerId, title, e);
        }
    }
}
