package com.devcine.backend.dto.response;

import com.devcine.backend.entity.Cinema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CinemaResponse {

    private Integer id;
    private String name;
    private String address;
    private String city;
    private String type;
    private String hotline;
    private Integer rooms;
    private Integer managerId;
    private String managerName;

    public static CinemaResponse fromEntity(Cinema cinema) {
        return CinemaResponse.builder()
                .id(cinema.getId())
                .name(cinema.getName())
                .address(cinema.getAddress())
                .city(cinema.getCity())
                .type(cinema.getType())
                .hotline(cinema.getHotline())
                .rooms(cinema.getRooms())
                .managerId(cinema.getManager() != null ? cinema.getManager().getUserId() : null)
                .managerName((cinema.getManager() != null && cinema.getManager().getUser() != null) ? cinema.getManager().getUser().getFullName() : null)
                .build();
    }
}
