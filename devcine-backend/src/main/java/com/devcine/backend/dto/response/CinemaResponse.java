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
    private String district;
    private String type;
    private String hotline;
    private Integer rooms;
    private String description;
    private Double latitude;
    private Double longitude;
    private String amenities;
    private String status;
    private Integer managerId;
    private String managerName;
    private String openingTime;  // "HH:mm" (default 08:00 nếu null)
    private String closingTime;  // "HH:mm" (default 23:30 nếu null)

    private static final java.time.format.DateTimeFormatter HM =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm");

    public static CinemaResponse fromEntity(Cinema cinema) {
        return CinemaResponse.builder()
                .id(cinema.getId())
                .name(cinema.getName())
                .address(cinema.getAddress())
                .city(cinema.getCity())
                .district(cinema.getDistrict())
                .type(cinema.getType())
                .hotline(cinema.getHotline())
                .rooms(cinema.getRooms())
                .description(cinema.getDescription())
                .latitude(cinema.getLatitude())
                .longitude(cinema.getLongitude())
                .amenities(cinema.getAmenities())
                .status(cinema.getStatus())
                .managerId(cinema.getManager() != null ? cinema.getManager().getUserId() : null)
                .managerName((cinema.getManager() != null && cinema.getManager().getUser() != null) ? cinema.getManager().getUser().getFullName() : null)
                .openingTime(cinema.getOpeningTime() != null ? cinema.getOpeningTime().format(HM) : "08:00")
                .closingTime(cinema.getClosingTime() != null ? cinema.getClosingTime().format(HM) : "23:30")
                .build();
    }
}
