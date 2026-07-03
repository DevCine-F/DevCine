package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_handovers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_schedule_id", nullable = false)
    private StaffSchedule staffSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_manager")
    private Staff approvedByManager;

    @Column(name = "declared_cash", precision = 15, scale = 2)
    private BigDecimal declaredCash;

    @Column(name = "system_cash", precision = 15, scale = 2)
    private BigDecimal systemCash;

    @Column(name = "cash_sales", precision = 15, scale = 2)
    private BigDecimal cashSales;

    @Column(name = "card_sales", precision = 15, scale = 2)
    private BigDecimal cardSales;

    @Column(name = "transfer_sales", precision = 15, scale = 2)
    private BigDecimal transferSales;

    @Column(name = "ticket_revenue", precision = 15, scale = 2)
    private BigDecimal ticketRevenue;

    @Column(name = "concession_revenue", precision = 15, scale = 2)
    private BigDecimal concessionRevenue;

    @Column(name = "ticket_count")
    private Long ticketCount;

    @Column(name = "concession_order_count")
    private Long concessionOrderCount;

    @Column(precision = 15, scale = 2)
    private BigDecimal difference;

    @Column(length = 20)
    private String status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(columnDefinition = "TEXT")
    private String note;
}
