package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cinemas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cinema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 50)
    private String type;

    @Column(length = 20)
    private String hotline;

    @Column
    private Integer rooms;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Staff manager;
}
