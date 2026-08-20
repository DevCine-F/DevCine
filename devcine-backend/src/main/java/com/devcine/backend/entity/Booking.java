package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    /** Nhân viên đã tạo đơn tại quầy (POS). Null với đơn ONLINE khách tự đặt. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_by")
    private Staff soldBy;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "final_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalPrice;

    /** Số tiền giảm từ voucher (snapshot tại thời điểm tạo đơn). 0 nếu không dùng voucher. */
    @Column(name = "discount_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    /** Mã giao dịch cổng thanh toán (VNPAY vnp_TransactionNo…) để đối soát. Null với tiền mặt. */
    @Column(name = "payment_gateway_ref", length = 100)
    private String paymentRef;

    @Column(length = 50)
    private String status;

    @Column(name = "booking_code", length = 50, unique = true)
    private String bookingCode;

    /** Kênh tạo đơn: "ONLINE" (khách tự đặt) | "POS" (bán tại quầy). Nguồn tin cậy để tách
     *  email (đơn Online kèm QR, đơn POS chỉ hoá đơn). */
    @Column(length = 20)
    private String channel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Thời điểm đơn được quét QR & in vé giấy tại quầy. Null = chưa in. */
    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    /** Nhân viên đã thực hiện in vé cho đơn (kiểm soát tại quầy). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "printed_by")
    private Staff printedBy;

    @Column(name = "pos_terminal_id", length = 100)
    private String posTerminalId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
