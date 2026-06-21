package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    /** BASE_PRICE (giá nền theo ngày/giờ/đối tượng). Các loại khác để mở rộng sau. */
    @Column(name = "rule_type", nullable = false, length = 30)
    private String ruleType;

    /** WEEKDAY | WEDNESDAY | WEEKEND | HOLIDAY | ALL */
    @Column(name = "day_type", length = 20)
    private String dayType;

    /** EARLY (<10h) | BEFORE_17H | AFTER_17H | ALL */
    @Column(name = "time_slot", length = 20)
    private String timeSlot;

    /** ADULT | STUDENT | CHILD | SENIOR | ALL */
    @Column(name = "audience_type", length = 20)
    private String audienceType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal value;

    /** Rule cụ thể hơn / priority cao hơn được ưu tiên khi nhiều rule cùng khớp. */
    @Column(columnDefinition = "integer not null default 0")
    private Integer priority;

    @Column(columnDefinition = "boolean not null default true")
    private Boolean active;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;
}
