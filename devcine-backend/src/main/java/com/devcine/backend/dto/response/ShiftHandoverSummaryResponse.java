package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftHandoverSummaryResponse {
    private Integer staffScheduleId;
    private Integer staffId;
    private String staffName;
    private String workPosition;
    private Integer cinemaId;
    private String cinemaName;
    private LocalDate workDate;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BigDecimal cashSales;
    private BigDecimal cardSales;
    private BigDecimal transferSales;
    private BigDecimal systemCash;
    private BigDecimal openingFloat;   // quỹ đầu ca (từ cấu hình)
    private BigDecimal expectedCash;    // tiền két kỳ vọng = quỹ đầu ca + doanh thu tiền mặt
    private BigDecimal ticketRevenue;
    private BigDecimal concessionRevenue;
    private Long ticketCount;
    private Long concessionOrderCount;
}
