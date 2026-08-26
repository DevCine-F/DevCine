package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SneakPreviewDTO {
    private Integer movieId;
    private String title;
    private String titleVietnamese;
    private String posterUrl;
    private String bannerUrl;
    private String description;
    private Integer durationMins;
    private String ageRating;
    private LocalDate releaseDate;
    private Set<String> genres;
    private String formattedDates;
    private String formattedTimes;
    private String locationSummary;
    private String defaultDate;
    private Integer totalShowtimes;
}
