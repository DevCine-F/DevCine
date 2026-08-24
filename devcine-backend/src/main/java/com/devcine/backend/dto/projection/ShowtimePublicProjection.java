package com.devcine.backend.dto.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Spring Data JPA Projection cho danh sách suất chiếu công khai.
 * Chỉ SELECT các trường cần thiết, HOÀN TOÀN BỎ QUA cột TEXT JSON lớn layout_data và tránh Cartesian product genres.
 */
public interface ShowtimePublicProjection {
    Integer getId();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    String getStatus();

    Integer getCinemaId();
    String getCinemaName();
    String getCinemaAddress();
    String getCinemaCity();

    Integer getMovieId();
    String getMovieTitle();
    String getMovieTitleVietnamese();
    Integer getMovieDurationMins();
    String getMoviePosterUrl();
    String getMovieAgeRating();
    String getMovieCountry();
    LocalDate getMovieReleaseDate();
    String getMovieDescription();
    String getMovieRating();
    Integer getMovieRatingCount();
    String getMovieTrailerUrl();

    Integer getFormatId();
    String getFormatName();

    Integer getRoomId();
    String getRoomName();
    String getRoomType();
}
