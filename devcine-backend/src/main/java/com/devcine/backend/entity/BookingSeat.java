package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "price_snapshot", precision = 15, scale = 2)
    private BigDecimal priceSnapshot;

    /** Loại vé/đối tượng đã chọn cho ghế này: ADULT | STUDENT | CHILD | SENIOR. */
    @Column(name = "ticket_type", length = 20)
    private String ticketType;

    @Column(length = 20)
    private String status;
}
