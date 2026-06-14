package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicShowtimeDTO {
    private Integer id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    
    // Cinema info
    private Integer cinemaId;
    private String cinemaName;
    private String cinemaAddress;
    
    // Movie info
    private Integer movieId;
    private String movieTitle;
    private String movieTitleVietnamese;
    private Integer movieDurationMins;
    private String moviePosterUrl;
    private String movieAgeRating;
    private String movieCountry;
    private java.time.LocalDate movieReleaseDate;
    private Set<String> movieGenres;
    
    // Format & Room info
    private Integer formatId;
    private String formatName;
    private Integer roomId;
    private String roomName;
}
