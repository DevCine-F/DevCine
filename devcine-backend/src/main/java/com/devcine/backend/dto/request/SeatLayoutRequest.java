package com.devcine.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLayoutRequest {
    private Integer matrixRow;
    private Integer matrixCol;
    private List<SeatDefinition> seats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatDefinition {
        private String rowChar;
        private Integer colNum;
        private Integer gridRow;
        private Integer gridCol;
        private String type; // e.g., "standard", "vip", "double"
        private String label; // e.g., "A1" (có thể được Admin sửa tay)
        private String status; // trạng thái vật lý: AVAILABLE, MAINTENANCE, LOCKED
        private Boolean custom; // true = label do Admin tự tay gõ đổi
    }
}
