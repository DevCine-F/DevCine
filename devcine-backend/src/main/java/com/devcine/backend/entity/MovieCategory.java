package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "movie_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(MovieCategory.MovieCategoryId.class)
public class MovieCategory {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovieCategoryId implements Serializable {
        private Integer movie;
        private Integer category;
    }
}
