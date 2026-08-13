package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "row_char", nullable = false, length = 2)
    private String rowChar;

    @Column(name = "col_num", nullable = false)
    private Integer colNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_type_id", nullable = false)
    private SeatType seatType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Nhãn hiển thị của ghế (VD "A1", có thể được Admin sửa tay để override số nhảy cóc).
     * Nguồn hiển thị chuẩn ở FE; rowChar/colNum giữ lại cho tương thích (email, gom nhóm...).
     */
    @Column(name = "label", length = 10)
    private String label;

    /**
     * true = label do Admin TỰ TAY gõ đổi (chốt cứng, không bị đánh số tự động ghi đè khi tải lại).
     * false/null = label auto sinh theo thứ tự ghế. Cần lưu riêng vì label auto và custom đều
     * suy ra rowChar+colNum nên không thể phân biệt qua so sánh chuỗi.
     */
    @Column(name = "custom_label")
    @Builder.Default
    private Boolean customLabel = false;

    /**
     * Trạng thái VẬT LÝ của ghế (khác is_active — is_active phục vụ xóa mềm khi lưu layout):
     * AVAILABLE = ghế bán bình thường, MAINTENANCE = đang bảo trì, LOCKED = khóa không bán.
     * Không dùng cho trạng thái runtime SOLD/HOLD (những cái đó tính từ BookingSeat).
     */
    // nullable ở DB để ddl-auto có thể thêm cột vào bảng seats đã có dữ liệu (rows cũ = null → coi như AVAILABLE trong code)
    @Column(name = "seat_status", length = 20)
    @Builder.Default
    private String seatStatus = "AVAILABLE";

    @Column(name = "grid_row")
    private Integer gridRow;

    @Column(name = "grid_col")
    private Integer gridCol;

    /**
     * Loại ô trong lưới sơ đồ:
     * SEAT  = ghế bán được (mặc định).
     * AISLE = lối đi — KHÔNG phải ghế, chỉ lưu để FE vẽ khung trọn vẹn (không phải tự đoán)
     *         và làm rào cản (barrier) cho luật chống ghế mồ côi.
     * Dòng cũ (trước migration) có giá trị NULL → được coi như SEAT trong code (xem {@link #isSeatCell()}).
     * Ô AISLE vẫn cần seat_type/row_char/col_num vì cột NOT NULL — dùng giá trị placeholder, mọi
     * nghiệp vụ ghế đều lọc theo cell_kind nên placeholder không ảnh hưởng.
     */
    @Column(name = "cell_kind", length = 10)
    @Builder.Default
    private String cellKind = "SEAT";

    /** true nếu ô này là GHẾ bán được (SEAT hoặc dòng cũ null). false nếu là lối đi (AISLE). */
    public boolean isSeatCell() {
        return cellKind == null || "SEAT".equalsIgnoreCase(cellKind);
    }

    /**
     * Nhãn hiển thị chuẩn cho mọi nơi (vé, email, QR-liên quan, in POS):
     * ưu tiên label do Admin đặt/sửa tay, fallback về rowChar+colNum khi chưa có.
     * Dùng thay cho việc ghép rowChar+colNum rải rác để tránh lệch nhãn.
     */
    public String displayLabel() {
        return (label != null && !label.isBlank()) ? label : (rowChar + colNum);
    }
}
