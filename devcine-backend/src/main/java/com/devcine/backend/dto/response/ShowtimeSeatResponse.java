package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeSeatResponse {
    private Integer matrixRow;
    private Integer matrixCol;
    private List<SeatDTO> seats;
    /** Mã đối tượng -> nhãn (ADULT: "Người lớn"...). */
    private Map<String, String> audienceLabels;
    /** Tên loại ghế -> (mã đối tượng -> giá). Để FE đổi loại vé không cần gọi lại server. */
    private Map<String, Map<String, BigDecimal>> priceTable;
}
