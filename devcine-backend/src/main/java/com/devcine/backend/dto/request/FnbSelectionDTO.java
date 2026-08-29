package com.devcine.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FnbSelectionDTO {
    private Integer fnbItemId;
    private Integer quantity;

    /**
     * Giá snapshot từ frontend (giá tại thời điểm user bấm "+ Chọn" combo).
     * Nếu có: backend dùng giá này làm priceSnapshot thay vì fetch từ DB → tôn trọng
     * "Price Lock at Selection". Backend vẫn verify chênh lệch để chặn gian lận.
     * Nếu null: fallback về item.getPrice() từ DB (hành vi cũ, backward-compatible).
     */
    private BigDecimal clientPrice;

    @Builder.Default
    private java.util.List<FnbOptionSelectionDTO> options = new java.util.ArrayList<>();
}

