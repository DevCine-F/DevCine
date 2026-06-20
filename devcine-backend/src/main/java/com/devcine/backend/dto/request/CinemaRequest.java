package com.devcine.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CinemaRequest {

    @NotBlank(message = "Tên rạp không được để trống")
    private String name;

    private String address;

    private String city;

    private String type;

    private String hotline;

    private Integer rooms;

    private String imageUrl;

    private String description;

    private Double latitude;

    private Double longitude;

    private String amenities;

    private String status;

    private Integer managerId;
}
