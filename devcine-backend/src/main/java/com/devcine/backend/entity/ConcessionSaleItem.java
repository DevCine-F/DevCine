package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

/** Một dòng món trong đơn bán bắp nước độc lập ({@link ConcessionSale}). */
@Entity
@Table(name = "concession_sale_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcessionSaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private ConcessionSale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fnb_item_id", nullable = false)
    private FnbItem fnbItem;

    /** Tên món tại thời điểm bán — bất biến, đọc từ đây khi xem lịch sử (không qua FK). */
    @Column(name = "item_name_snapshot", length = 255)
    private String itemNameSnapshot;

    @Column(nullable = false)
    private Integer quantity;

    /** Giá tại thời điểm bán (chốt giá, không phụ thuộc giá món sau này thay đổi). */
    @Column(name = "price_snapshot", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceSnapshot;

    @OneToMany(mappedBy = "saleItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConcessionSaleItemOption> options = new ArrayList<>();
}
