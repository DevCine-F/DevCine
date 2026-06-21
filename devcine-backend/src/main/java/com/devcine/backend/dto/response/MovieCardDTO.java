package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

/** Thẻ phim cho danh sách phân trang ở trang Lịch chiếu (chế độ "Theo Phim"). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieCardDTO {
    private Integer id;
    private String title;
    private String titleVietnamese;
    private String posterUrl;
    private String ageRating;
    private Integer durationMins;
    private String country;
    private LocalDate releaseDate;
    private Set<String> genres;
}
