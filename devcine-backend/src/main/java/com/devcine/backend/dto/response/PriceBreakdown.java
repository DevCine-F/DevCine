package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Tách khoản giá 1 ghế để hiển thị/preview cho admin & khách. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceBreakdown {
    private Integer seatId;
    private String seatType;
    private String ticketType;
    private BigDecimal basePrice;
    private BigDecimal seatSurcharge;
    private BigDecimal formatSurcharge;
    private boolean fixedPrice;
    private BigDecimal total;
}
