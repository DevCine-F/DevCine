package com.devcine.backend.service;

import com.devcine.backend.entity.Movie;
import com.devcine.backend.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import com.devcine.backend.dto.response.MovieSummaryDTO;
import com.devcine.backend.dto.response.MovieSummaryDTO.CategorySummaryDTO;
@Service
public class MovieService {

    @Autowired
    private MovieRepository movieRepository;

    public List<MovieSummaryDTO> getAllMovies() {
        return movieRepository.findAllWithGenres().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /** Phim đang chiếu (status = active). */
    public List<MovieSummaryDTO> getNowShowing() {
        return movieRepository.findAllWithGenres().stream()
                .filter(m -> "active".equalsIgnoreCase(m.getStatus()))
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /** Phim sắp chiếu (status = upcoming). */
    public List<MovieSummaryDTO> getUpcoming() {
        return movieRepository.findAllWithGenres().stream()
                .filter(m -> "upcoming".equalsIgnoreCase(m.getStatus()))
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    public List<MovieSummaryDTO> searchMovies(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return movieRepository.searchMovies(keyword.trim()).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private MovieSummaryDTO toSummary(Movie movie) {
        return MovieSummaryDTO.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .titleVietnamese(movie.getTitleVietnamese())
                .durationMins(movie.getDurationMins())
                .format(movie.getFormat())
                .supportedFormats(movie.getSupportedFormats())
                .rating(movie.getRating())
                .country(movie.getCountry())
                .status(movie.getStatus())
                .posterUrl(movie.getPosterUrl())
                .releaseDate(movie.getReleaseDate())
                .ageRating(movie.getAgeRating())
                .genres(movie.getGenres() == null ? null : movie.getGenres().stream()
                        .map(g -> CategorySummaryDTO.builder()
                                .id(g.getId())
                                .name(g.getName())
                                .build())
                        .collect(Collectors.toSet()))
                .build();
    }

    public Movie getMovieById(Integer id) {
        return movieRepository.findByIdWithGenres(id).orElse(null);
    }

    public Movie createMovie(Movie movie) {
        return movieRepository.save(movie);
    }

    public Movie updateMovie(Integer id, Movie movieDetails) {
        Movie existingMovie = movieRepository.findById(id).orElse(null);
        if (existingMovie != null) {
            existingMovie.setTitle(movieDetails.getTitle());
            existingMovie.setSlug(movieDetails.getSlug());
            existingMovie.setDurationMins(movieDetails.getDurationMins());
            existingMovie.setAgeRating(movieDetails.getAgeRating());
            existingMovie.setReleaseDate(movieDetails.getReleaseDate());
            existingMovie.setEndDate(movieDetails.getEndDate());
            existingMovie.setStatus(movieDetails.getStatus());
            existingMovie.setCountry(movieDetails.getCountry());
            existingMovie.setRating(movieDetails.getRating());
            existingMovie.setPosterUrl(movieDetails.getPosterUrl());
            existingMovie.setBannerUrl(movieDetails.getBannerUrl());
            existingMovie.setShowOnBanner(movieDetails.getShowOnBanner());
            existingMovie.setTrailerUrl(movieDetails.getTrailerUrl());
            existingMovie.setFormat(movieDetails.getFormat());
            existingMovie.setSupportedFormats(movieDetails.getSupportedFormats());
            existingMovie.setTitleVietnamese(movieDetails.getTitleVietnamese());
            existingMovie.setProductionYear(movieDetails.getProductionYear());
            existingMovie.setLanguage(movieDetails.getLanguage());
            existingMovie.setBasePrice(movieDetails.getBasePrice());
            existingMovie.setDescription(movieDetails.getDescription());
            existingMovie.setOriginalLanguage(movieDetails.getOriginalLanguage());
            existingMovie.setVersionType(movieDetails.getVersionType());
            existingMovie.setInternalNotes(movieDetails.getInternalNotes());
            existingMovie.setStartDate(movieDetails.getStartDate());
            existingMovie.setGenres(movieDetails.getGenres());
            existingMovie.setDirector(movieDetails.getDirector());
            existingMovie.setCastMembers(movieDetails.getCastMembers());
            existingMovie.setDistributor(movieDetails.getDistributor());
            existingMovie.setRatingCount(movieDetails.getRatingCount());
            return movieRepository.save(existingMovie);
        }
        return null;
    }

    public void deleteMovie(Integer id) {
        movieRepository.deleteById(id);
    }
}
