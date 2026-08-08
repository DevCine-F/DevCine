package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "fnb_option_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FnbOptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @JsonIgnore
    private FnbOptionGroup group;

    @Column(nullable = false)
    private String name;

    @Column(name = "surcharge_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal surchargePrice = BigDecimal.ZERO;
}
