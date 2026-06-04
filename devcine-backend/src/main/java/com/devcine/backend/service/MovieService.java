package com.devcine.backend.service;

import com.devcine.backend.entity.Movie;
import com.devcine.backend.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

    @Autowired
    private MovieRepository movieRepository;

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Movie getMovieById(Integer id) {
        return movieRepository.findById(id).orElse(null);
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
            return movieRepository.save(existingMovie);
        }
        return null;
    }

    public void deleteMovie(Integer id) {
        movieRepository.deleteById(id);
    }
}
