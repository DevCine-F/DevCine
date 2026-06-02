package com.devcine.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaShowtimeDTO {
    private Integer cinemaId;
    private String cinemaName;
    private String address;
    private String city;
    // Map of Date (String format like YYYY-MM-DD) to list of Showtimes
    private Map<String, List<ShowtimeDTO>> showtimesByDate;
}
