package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ghi vết (audit) một lần xử lý sự cố phòng chiếu tại quầy — đổi ghế đền bù, hủy chỗ,
 * hoặc khóa ghế bảo trì. MỖI ghế xử lý = 1 dòng (đơn nhóm nhiều ghế sẽ có nhiều dòng
 * cùng {@code booking_id}). Bảng CHỈ để đối soát nội bộ; không tham gia luồng bán/soát vé.
 *
 * <p>Bối cảnh nghiệp vụ (chốt kiến trúc):
 * <ul>
 *   <li>KHÔNG có hoàn tiền — đền bù bằng Voucher (giảm giá / quà F&B / vé mời) hoặc đền trực
 *       tiếp tại quầy cho khách vãng lai (không sinh Voucher, chỉ ghi vết ở đây).</li>
 *   <li>Flat Pricing: đổi ghế trong cùng suất KHÔNG có chênh lệch tiền → đền theo "giá trị cảm
 *       nhận" (goodwill). Chênh lệch tiền &gt; 0 chỉ xảy ra khi HỦY chỗ (đền bằng giá vé đã mua).</li>
 * </ul>
 */
@Entity
@Table(name = "seat_incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** RELOCATE | CANCEL | SEAT_MAINTENANCE. */
    @Column(name = "incident_type", nullable = false, length = 20)
    private String incidentType;

    /** Vé liên quan (null khi chỉ khóa ghế bảo trì mà chưa gắn vé nào). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    /** Suất chiếu xảy ra sự cố (null với thao tác khóa ghế cấp phòng). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id")
    private Showtime showtime;

    /** Ghế nguồn (ghế hỏng / ghế cũ trước khi đổi). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_seat_id")
    private Seat oldSeat;

    /** Ghế đích sau khi đổi (null với CANCEL / SEAT_MAINTENANCE). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_seat_id")
    private Seat newSeat;

    /** Snapshot nhãn ghế tại thời điểm xử lý — label có thể bị Admin sửa về sau. */
    @Column(name = "old_seat_label", length = 10)
    private String oldSeatLabel;

    @Column(name = "new_seat_label", length = 10)
    private String newSeatLabel;

    /** NONE | DISCOUNT | GIFT_FNB | GIFT_TICKET. */
    @Column(name = "compensation_type", length = 20)
    @Builder.Default
    private String compensationType = "NONE";

    /** Trị giá đền quy tiền (0 với goodwill/quà/none; = giá vé khi CANCEL). */
    @Column(name = "compensation_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal compensationAmount = BigDecimal.ZERO;

    /** Voucher đã phát (null nếu đền trực tiếp tại quầy cho khách vãng lai, hoặc không đền). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    /** Mã phiếu quà tại quầy cho khách vãng lai, dùng để đối soát F&B. */
    @Column(name = "audit_gift_code", unique = true, length = 80)
    private String auditGiftCode;

    @Column(name = "reason", length = 255)
    private String reason;

    /** Nhân viên/Admin đang đăng nhập đã thực hiện (mirror pattern Booking.sold_by). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private Staff handledBy;

    /** Cụm rạp của sự cố — phục vụ Strict Cinema Scoping & đối soát theo cơ sở. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
