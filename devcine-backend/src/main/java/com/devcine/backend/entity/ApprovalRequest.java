package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Yêu cầu "sửa sai" nội bộ cần Quản lý/Quản trị viên phê duyệt.
 * Mô hình: nhân viên tạo yêu cầu (PENDING) → Quản lý/Admin duyệt (APPROVED) hoặc từ chối (REJECTED).
 *
 * <p>Loại ({@code type}):
 * <ul>
 *   <li>{@code FNB_VOID} — hủy hóa đơn bắp nước bấm nhầm; {@code refId} = ConcessionSale.id.</li>
 * </ul>
 */
@Entity
@Table(name = "approval_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** FNB_VOID */
    @Column(nullable = false, length = 30)
    private String type;

    /** Khoá tham chiếu đối tượng bị tác động (ConcessionSale.id hoặc Booking.id). */
    @Column(name = "ref_id")
    private Integer refId;

    /** Mã hiển thị của đối tượng (saleCode / bookingCode) để duyệt nhanh không cần join. */
    @Column(name = "ref_code", length = 50)
    private String refCode;

    /** Dữ liệu bổ sung dạng JSON (dự phòng cho các loại yêu cầu sau này). */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /** Mô tả ngắn để người duyệt đọc nhanh (VD "Hủy hóa đơn CCS-XXXX (95.000đ)"). */
    @Column(length = 255)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String reason;

    /** PENDING | APPROVED | REJECTED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "cinema_id")
    private Integer cinemaId;

    @Column(name = "requested_by_user_id")
    private Integer requestedByUserId;

    @Column(name = "requested_by_name", length = 120)
    private String requestedByName;

    @Column(name = "approved_by_user_id")
    private Integer approvedByUserId;

    @Column(name = "approved_by_name", length = 120)
    private String approvedByName;

    @Column(name = "decision_note", columnDefinition = "TEXT")
    private String decisionNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}
