package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả tạo suất chiếu đơn lẻ. Nếu suất KẾT THÚC quá giờ đóng cửa và admin chưa xác nhận
 * (force=false) → {@code requiresConfirmation=true} và {@code showtime=null}; FE hiện hộp xác nhận
 * rồi gửi lại với force=true.
 *
 * <p>{@code earlyScreening=true} khi startTime nằm trước ngày khởi chiếu của phim
 * ({@code movie.releaseDate}) — FE dùng để hiện badge "Chiếu sớm" và thông báo cho admin.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeCreateResult {
    private boolean requiresConfirmation;
    private String message;
    private String endTime;            // "HH:mm" — giờ kết thúc (kèm dọn dẹp) khi cảnh báo
    private ShowtimeDTO showtime;      // suất đã tạo (null khi requiresConfirmation)

    /** true nếu suất vừa tạo là xuất chiếu sớm (startTime < movie.releaseDate). */
    private boolean earlyScreening;

    /** Ngày khởi chiếu chính thức của phim (ISO yyyy-MM-dd) — chỉ có khi earlyScreening=true. */
    private java.time.LocalDate movieReleaseDate;
}
