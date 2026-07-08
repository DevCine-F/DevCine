package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Kết quả tạo lịch hàng loạt: số suất sẽ/đã tạo + danh sách bị bỏ qua kèm lý do. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchShowtimeResult {

    /** Số suất hợp lệ (sẽ tạo nếu dryRun, hoặc đã tạo nếu không). */
    private int toCreate;

    /** Số suất thực sự đã ghi vào DB (0 khi dryRun). */
    private int createdCount;

    private List<SkippedSlot> skipped;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkippedSlot {
        private Integer roomId;
        private String roomName;
        private String startTime; // ISO-8601 local
        private String reason;
    }
}
