package com.devcine.backend.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MovieDTO {
    private Integer id;
    private String title;
    private String slug;
    private Integer durationMins;
    private String ageRating;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private String status;
    private String country;
    private String rating;
    private String posterUrl;
    private String bannerUrl;
    private Boolean showOnBanner;
    private String trailerUrl;
    private String format;
    private String supportedFormats;
    private String titleVietnamese;
    private Integer productionYear;
    private String language;
    private String description;
    private String originalLanguage;
    private String versionType;
    private String internalNotes;
    private LocalDate startDate;
    private java.util.Set<com.devcine.backend.entity.Category> genres;
}
