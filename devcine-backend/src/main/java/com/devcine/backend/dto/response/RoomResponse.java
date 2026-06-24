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
