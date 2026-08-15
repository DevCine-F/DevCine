package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ảnh chụp (snapshot) BẤT BIẾN của khung sơ đồ ghế tại thời điểm tạo suất chiếu.
 * Chỉ chứa KHÔNG GIAN tĩnh (vị trí, loại ghế, label, lối đi, span) + seatId để map trạng thái.
 * KHÔNG chứa trạng thái runtime (SOLD/HOLD/MAINTENANCE) — những cái đó luôn overlay live theo seatId.
 *
 * <p>Được nén thành JSON lưu ở cột {@code showtimes.layout_data} (dùng từ Phase 2) để mỗi suất
 * có sơ đồ riêng, không bị lệch khi Admin sửa lại phòng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLayoutSnapshot {
    /** Phiên bản cấu trúc JSON — để nâng cấp schema sau này không làm vỡ suất cũ. */
    private int schema;
    private int matrixRow; // = max(gridRow)+1 tại thời điểm snapshot (không dùng fallback)
    private int matrixCol; // = max(gridCol)+1
    private String snapshotAt;
    private List<Cell> cells;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cell {
        /** SEAT (ghế bán được) | AISLE (lối đi, làm rào cản, không bán). */
        private String kind;
        /** Định danh ghế bất biến — chỉ có ở kind=SEAT; dùng map trạng thái live & tham chiếu booking. */
        private Integer seatId;
        private int gridRow;
        private int gridCol;
        private String rowChar;
        private Integer colNum;
        /** Loại ghế (NORMAL/VIP/SWEETBOX) — chỉ ý nghĩa với SEAT. */
        private String type;
        /** Nhãn hiển thị — chỉ có ở SEAT. */
        private String label;
        /** Số chỗ ô này chiếm theo chiều ngang: SWEETBOX=2, còn lại=1. */
        private int span;
    }
}
