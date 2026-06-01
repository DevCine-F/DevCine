package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_inventory_id", nullable = false)
    private CinemaInventory cinemaInventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_staff")
    private Staff changedByStaff;

    @Column(length = 20)
    private String type;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
