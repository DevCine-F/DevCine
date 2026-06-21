package com.devcine.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {
    private Integer customerId;
    private Integer showtimeId;
    /** Cách cũ: danh sách ghế (mặc định loại vé ADULT). Giữ để tương thích ngược. */
    private List<Integer> seatIds;
    /** Cách mới: ghế kèm loại vé/đối tượng. Nếu có sẽ ưu tiên dùng thay cho seatIds. */
    private List<SeatSelectionDTO> seatSelections;
    private List<FnbSelectionDTO> fnbs;
    private Integer voucherId;
    private String paymentMethod; // VNPAY, MOMO, TRANSFER
}
