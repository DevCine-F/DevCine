package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả tạo suất chiếu đơn lẻ. Nếu suất KẾT THÚC quá giờ đóng cửa và admin chưa xác nhận
 * (force=false) → {@code requiresConfirmation=true} và {@code showtime=null}; FE hiện hộp xác nhận
 * rồi gửi lại với force=true.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeCreateResult {
    private boolean requiresConfirmation;
    private String message;
    private String endTime;        // "HH:mm" — giờ kết thúc (kèm dọn dẹp) khi cảnh báo
    private ShowtimeDTO showtime;   // suất đã tạo (null khi requiresConfirmation)
}
