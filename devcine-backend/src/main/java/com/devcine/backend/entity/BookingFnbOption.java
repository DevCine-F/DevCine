package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "booking_fnb_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingFnbOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_fnb_id", nullable = false)
    @JsonIgnore
    private BookingFnb bookingFnb;

    /**
     * FK tới Vị đã chọn (truy vết ID cho đơn Online — parity với ConcessionSaleItemOption).
     * Nullable: đơn cũ trước khi có cột này để null; snapshot tên/giá vẫn đủ để hiển thị.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_item_id")
    private FnbOptionItem optionItem;

    /** FK tới Kho (Pool) của vị — truy vết ID. Nullable vì lý do tương thích ngược. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id")
    private FnbOptionGroup optionGroup;

    /** Snapshot nhãn Ô chọn tại thời điểm đặt (VD "Nước 1") — bảo toàn báo cáo lịch sử. */
    @Column(name = "slot_label_snapshot")
    private String slotLabelSnapshot;

    @Column(name = "option_name_snapshot")
    private String optionNameSnapshot;

    @Column(name = "surcharge_snapshot", precision = 15, scale = 2)
    private BigDecimal surchargeSnapshot;
}
