package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lưu các phiên bản QR vé đã bị thu hồi khi đổi ghế.
 *
 * <p>QR hiện hành vẫn nằm ở {@link Ticket#qrCode}; bảng này chỉ giữ lịch sử để cổng soát vé
 * nhận diện chính xác vé giấy cũ thay vì trả lỗi "không tồn tại" chung chung.</p>
 */
@Entity
@Table(name = "ticket_qr_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketQrHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "qr_code", nullable = false, unique = true, length = 500)
    private String qrCode;

    @Column(name = "ticket_version", nullable = false)
    private Integer ticketVersion;

    @Column(name = "revoked_at", nullable = false)
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    @PrePersist
    void onCreate() {
        if (revokedAt == null) revokedAt = LocalDateTime.now();
    }
}
