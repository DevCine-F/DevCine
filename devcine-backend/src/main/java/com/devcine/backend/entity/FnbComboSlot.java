package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Một "ô chọn món" (Slot) khi cấu hình Combo — mô hình CGV/Lotte.
 *
 * <p>Slot trỏ tới một {@link FnbOptionGroup} (Option Pool thuần túy) và mang
 * ràng buộc chọn RIÊNG của slot đó ({@code minChoices}/{@code maxChoices}/{@code isRequired}).
 * Nhờ tách ràng buộc xuống Slot, một món có thể tham chiếu cùng một pool NHIỀU LẦN
 * (VD: Combo Đôi = 1 Slot Bắp + 2 Slot Nước riêng biệt "Nước 1"/"Nước 2"), và
 * cấu hình "Mix 2 vị" chỉ đơn giản là một Slot Bắp có {@code maxChoices = 2} —
 * không còn logic dò tên chuỗi trên UI.
 */
@Entity
@Table(name = "fnb_item_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FnbComboSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fnb_item_id", nullable = false)
    @JsonIgnore
    private FnbItem fnbItem;

    /** Kho tùy chọn (pool) mà slot này lấy món ra cho khách chọn. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", nullable = false)
    private FnbOptionGroup optionGroup;

    /** Nhãn hiển thị của ô chọn: "Bắp", "Nước 1", "Nước 2"... */
    @Column(name = "slot_label", nullable = false)
    private String slotLabel;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "min_choices", nullable = false)
    @Builder.Default
    private Integer minChoices = 1;

    @Column(name = "max_choices", nullable = false)
    @Builder.Default
    private Integer maxChoices = 1;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = true;

    /** Tùy chọn mặc định được chọn sẵn khi mở modal cho Slot này */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_option_item_id")
    private FnbOptionItem defaultOptionItem;
}
