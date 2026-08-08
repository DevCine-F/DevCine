package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "booking_fnb_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingFnbOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_fnb_id", nullable = false)
    @JsonIgnore
    private BookingFnb bookingFnb;

    @Column(name = "option_name_snapshot", nullable = false)
    private String optionNameSnapshot;

    @Column(name = "surcharge_snapshot", nullable = false, precision = 15, scale = 2)
    private BigDecimal surchargeSnapshot;
}
