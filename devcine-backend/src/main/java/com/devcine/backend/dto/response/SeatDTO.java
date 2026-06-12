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
    private BigDecimal price;
    private String status; // AVAILABLE, HOLD, SOLD
    private Integer gridRow;
    private Integer gridCol;
}
