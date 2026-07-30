package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatDTO {
    private Integer seatId;
    private String rowChar;
    private Integer colNum;
    private String seatType; // e.g., NORMAL, VIP, SWEETBOX
    private String label;    // nhãn hiển thị (custom hoặc rowChar+colNum)
    private BigDecimal price;
    private String status;      // runtime: AVAILABLE, HOLD, SOLD — hoặc MAINTENANCE/LOCKED khi ghế khóa vật lý
    private String seatStatus;  // trạng thái vật lý gốc: AVAILABLE, MAINTENANCE, LOCKED
    private Integer gridRow;
    private Integer gridCol;
}
