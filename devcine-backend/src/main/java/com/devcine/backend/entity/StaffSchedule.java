package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "work_position", length = 80)
    private String workPosition;

    @Column(length = 120)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(length = 20)
    private String status;

    // Mốc giờ nhân viên bấm vào ca / kết ca thực tế (chỉ để hiển thị đối soát, không tính lương/phạt).
    @Column(name = "actual_check_in_at")
    private LocalDateTime actualCheckInAt;

    @Column(name = "actual_check_out_at")
    private LocalDateTime actualCheckOutAt;
}
