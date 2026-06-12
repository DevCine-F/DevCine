package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 30)
    private String type;

    @Column(length = 20)
    private String status;

    @Column(name = "turnaround_time_mins")
    private Integer turnaroundTimeMins;

    @Column(name = "matrix_row")
    private Integer matrixRow;

    @Column(name = "matrix_col")
    private Integer matrixCol;
}
