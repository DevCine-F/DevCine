package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cinema_inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CinemaInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fnb_item_id", nullable = false)
    private FnbItem fnbItem;

    @Column(name = "in_stock", nullable = false)
    @Builder.Default
    private Integer inStock = 0;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
