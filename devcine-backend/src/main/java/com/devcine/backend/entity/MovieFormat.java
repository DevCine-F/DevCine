package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "movie_formats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(precision = 15, scale = 2)
    private BigDecimal surcharge;
}
