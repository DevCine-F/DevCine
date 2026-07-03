package com.devcine.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShiftHandoverRequest {

    private Integer staffScheduleId;

    @NotNull(message = "Vui lòng nhập số tiền mặt thực tế")
    @DecimalMin(value = "0.0", message = "Số tiền mặt thực tế không được âm")
    private BigDecimal declaredCash;

    private String note;
}
