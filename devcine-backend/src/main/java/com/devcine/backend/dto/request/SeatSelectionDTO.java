package com.devcine.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một ghế được chọn kèm loại vé/đối tượng (ADULT | STUDENT | CHILD | SENIOR). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatSelectionDTO {
    private Integer seatId;
    private String ticketType;
}
