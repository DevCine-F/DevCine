package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "booking_fnbs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingFnb {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fnb_item_id", nullable = false)
    private FnbItem fnbItem;

    /** Tên món tại thời điểm mua — bất biến, đọc từ đây khi xem lịch sử (không qua FK). */
    @Column(name = "item_name_snapshot", length = 255)
    private String itemNameSnapshot;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_snapshot", precision = 15, scale = 2)
    private BigDecimal priceSnapshot;

    @OneToMany(mappedBy = "bookingFnb", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingFnbOption> options = new ArrayList<>();
}
