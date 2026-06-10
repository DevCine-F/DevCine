package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(unique = true)
    private String slug;

    @Column(name = "duration_mins")
    private Integer durationMins;

    @Column(name = "age_rating", length = 10)
    private String ageRating;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 20)
    private String status;

    @Column(length = 100)
    private String country;

    @Column(length = 10)
    private String rating;

    @Column(name = "poster_base64", columnDefinition = "TEXT")
    private String posterUrl;

    @Column(name = "banner_base64", columnDefinition = "TEXT")
    private String bannerUrl;

    @Column(name = "show_on_banner")
    private Boolean showOnBanner;

    @Column(name = "trailer_url", length = 1000)
    private String trailerUrl;

    @Column(length = 255)
    private String format;

    @Column(name = "title_vietnamese")
    private String titleVietnamese;

    @Column(name = "production_year")
    private Integer productionYear;

    @Column(length = 100)
    private String language;

    @Column(name = "base_price")
    private Double basePrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "original_language", length = 100)
    private String originalLanguage;

    @Column(name = "version_type", length = 100)
    private String versionType;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(length = 255)
    private String distributor;

    @Column(length = 255)
    private String director;

    @Column(name = "cast_members", columnDefinition = "TEXT")
    private String castMembers;

    @Column(name = "rating_count")
    private Integer ratingCount;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "movie_genre_mapping",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private java.util.Set<Category> genres;
}
