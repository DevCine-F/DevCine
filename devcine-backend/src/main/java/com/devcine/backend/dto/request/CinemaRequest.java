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

    @NotNull(message = "Số phòng không được để trống")
    private Integer rooms;

    private Integer managerId;
}
