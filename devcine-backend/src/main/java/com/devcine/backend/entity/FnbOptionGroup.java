package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;
import java.util.HashSet;

/**
 * KHO TÙY CHỌN GỐC (Option Pool) — thuần túy chỉ là một danh mục các lựa chọn
 * dùng chung (VD: "Tùy Chọn Bắp", "Tùy Chọn Nước").
 *
 * <p>Ràng buộc chọn (min/max/required) KHÔNG còn nằm ở đây — chúng đã được
 * chuyển xuống {@link FnbComboSlot} để mỗi Slot của từng Combo tự định nghĩa
 * số lượng chọn riêng. Pool chỉ trả lời câu hỏi "được chọn CÁI GÌ", còn Slot
 * trả lời "chọn BAO NHIÊU, ở Ô nào".
 */
@Entity
@Table(name = "fnb_option_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FnbOptionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<FnbOptionItem> items = new HashSet<>();
}
