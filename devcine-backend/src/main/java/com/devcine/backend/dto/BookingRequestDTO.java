package com.devcine.backend.dto;

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
    private List<Integer> seatIds;
    private List<FnbSelectionDTO> fnbs;
    private Integer voucherId;
    private String paymentMethod; // VNPAY, MOMO, TRANSFER
}
