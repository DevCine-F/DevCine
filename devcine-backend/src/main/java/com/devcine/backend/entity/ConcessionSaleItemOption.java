package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "concession_sale_item_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcessionSaleItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_item_id", nullable = false)
    private ConcessionSaleItem saleItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", nullable = false)
    private FnbOptionGroup optionGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_item_id", nullable = false)
    private FnbOptionItem optionItem;

    /** Snapshot nhãn Ô chọn tại thời điểm bán (VD "Nước 1") — bảo toàn báo cáo lịch sử. */
    @Column(name = "slot_label_snapshot")
    private String slotLabelSnapshot;

    @Column(name = "option_name_snapshot")
    private String optionNameSnapshot;

    @Column(name = "surcharge_snapshot", precision = 15, scale = 2)
    private BigDecimal surchargeSnapshot;
}
