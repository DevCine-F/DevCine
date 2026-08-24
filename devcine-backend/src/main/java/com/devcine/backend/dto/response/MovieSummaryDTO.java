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
    @com.fasterxml.jackson.annotation.JsonProperty("posterUrl")
    private String posterUrl;
    private String trailerUrl;
    private java.time.LocalDate releaseDate;
    private java.time.LocalDate endDate;
    private String ageRating;
    private Integer ratingCount;
    private Long ticketSales;
    private Set<CategorySummaryDTO> genres;

    /**
     * true nếu phim sắp chiếu (releaseDate > today) đã có ít nhất 1 suất chiếu sớm
     * còn mở bán (status = "Xuất chiếu sớm" và startTime >= now).
     * FE dùng để hiện nút "Đặt vé sớm" và badge trên card phim sắp chiếu.
     */
    private boolean hasEarlyScreening;

    @Data
    @Builder
    public static class CategorySummaryDTO {
        private Integer id;
        private String name;
    }
}
