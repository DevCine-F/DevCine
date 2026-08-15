package com.devcine.backend.dto.response;

import com.devcine.backend.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {

    private Integer id;
    private Integer cinemaId;
    private String name;
    private String type;
    private String status;
    private Integer turnaroundTimeMins;
    private Integer matrixRow;
    private Integer matrixCol;
    // Sức chứa VẬT LÝ thật sự (ghế đôi = 2 chỗ, bỏ lối đi/ghế xóa, giữ ghế khóa) — KHỚP editor sơ đồ.
    // KHÁC matrixRow*matrixCol: phép nhân lưới không biết ghế đôi/lối đi nên hiển thị sai (xem card cấu hình phòng).
    private Integer seatCount;

    public static RoomResponse fromEntity(Room r) {
        return RoomResponse.builder()
                .id(r.getId())
                .cinemaId(r.getCinema() != null ? r.getCinema().getId() : null)
                .name(r.getName())
                .type(r.getType())
                .status(r.getStatus())
                .turnaroundTimeMins(r.getTurnaroundTimeMins())
                .matrixRow(r.getMatrixRow())
                .matrixCol(r.getMatrixCol())
                .build();
    }
}
