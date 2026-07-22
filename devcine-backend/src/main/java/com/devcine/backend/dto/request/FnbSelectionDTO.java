package com.devcine.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FnbSelectionDTO {
    @NotNull(message = "Thiếu mã món F&B")
    private Integer fnbItemId;

    /**
     * Chặn số lượng vô lý ngay ở biên: quantity âm sẽ TRỪ vào tổng tiền đơn,
     * null gây NPE khi nhân giá. Trần 99 khớp giới hạn của màn POS.
     */
    @NotNull(message = "Thiếu số lượng món F&B")
    @Min(value = 1, message = "Số lượng mỗi món phải từ 1")
    @Max(value = 99, message = "Số lượng mỗi món tối đa 99")
    private Integer quantity;
}
