package com.devcine.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một ghế được chọn kèm loại vé/đối tượng (ADULT | STUDENT | CHILD | SENIOR). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatSelectionDTO {
    private Integer seatId;
    private String ticketType;
    /**
     * Giá snapshot từ bảng giá phía client tại thời điểm người dùng chọn ghế/tải sơ đồ.
     * Dùng để chốt giá (Price Snapshot) chuẩn CGV/LotteCinema, tránh trường hợp admin đổi giá nền
     * giữa phiên đặt vé làm nhảy giá lúc thanh toán. Null nếu là client cũ hoặc POS không gửi giá.
     */
    private java.math.BigDecimal unitPrice;
}
