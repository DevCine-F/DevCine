package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Ngày lễ — suất chiếu rơi vào ngày này áp giá nền day_type=HOLIDAY (fallback WEEKEND nếu chưa cấu hình). */
@Entity
@Table(name = "holidays")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 150)
    private String name;
}
