package com.devcine.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class MovieSummaryDTO {
    private Integer id;
    private String title;
    private String titleVietnamese;
    private Integer durationMins;
    private String format;
    private String supportedFormats;
    private String rating;
    private String country;
    private String status;
    private String posterUrl;
    private String trailerUrl;
    private java.time.LocalDate releaseDate;
    private java.time.LocalDate endDate;
    private String ageRating;
    private Integer ratingCount;
    private Set<CategorySummaryDTO> genres;

    @Data
    @Builder
    public static class CategorySummaryDTO {
        private Integer id;
        private String name;
    }
}
