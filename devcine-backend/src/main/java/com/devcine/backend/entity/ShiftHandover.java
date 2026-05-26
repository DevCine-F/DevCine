package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

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

    @Column(precision = 15, scale = 2)
    private BigDecimal difference;

    private String status;
}
