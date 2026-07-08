package com.devcine.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Cấu hình tạo lịch chiếu HÀNG LOẠT: sinh tích Descartes
 * (phòng × ngày × khung giờ) cho MỘT phim. Xem {@code dryRun} để chỉ xem trước.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchShowtimeRequest {

    @NotNull(message = "Movie ID is required")
    private Integer movieId;

    @NotNull(message = "Format ID is required")
    private Integer formatId;

    /** Thời gian dọn dẹp (phút); mặc định 15 nếu bỏ trống. */
    private Integer cleaningTime;

    /** Danh sách phòng áp dụng (có thể thuộc nhiều cơ sở). */
    @NotEmpty(message = "Phải chọn ít nhất một phòng chiếu")
    private List<Integer> roomIds;

    @NotNull(message = "Ngày bắt đầu là bắt buộc")
    private LocalDate dateFrom;

    @NotNull(message = "Ngày kết thúc là bắt buộc")
    private LocalDate dateTo;

    /** Lọc theo thứ trong tuần (ISO: 1=Thứ Hai … 7=Chủ Nhật). Rỗng/null = mọi ngày. */
    private List<Integer> daysOfWeek;

    /** Danh sách mốc giờ bắt đầu dạng "HH:mm" (vd "09:00","18:30"). */
    @NotEmpty(message = "Phải nhập ít nhất một khung giờ")
    private List<String> startTimes;

    /** true = chỉ xem trước (không ghi DB). */
    private boolean dryRun;
}
