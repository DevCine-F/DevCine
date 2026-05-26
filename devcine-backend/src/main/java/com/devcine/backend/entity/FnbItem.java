package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "fnb_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FnbItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    private String type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;
}
