package com.devcine.backend.service;

import com.devcine.backend.entity.Movie;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.MovieRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.devcine.backend.dto.response.MovieStatsResponse;
import com.devcine.backend.dto.response.MovieStatsResponse.ClassRevenue;
import com.devcine.backend.dto.response.MovieSummaryDTO;
import com.devcine.backend.dto.response.MovieSummaryDTO.CategorySummaryDTO;
@Service
public class MovieService {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

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
                .endDate(movie.getEndDate())
                .ageRating(movie.getAgeRating())
                .basePrice(movie.getBasePrice())
                .ratingCount(movie.getRatingCount())
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

    /** Cập nhật trạng thái cho nhiều phim cùng lúc (bulk action). */
    @Transactional
    public int bulkUpdateStatus(List<Integer> ids, String status) {
        if (ids == null || ids.isEmpty() || status == null || status.isBlank()) {
            return 0;
        }
        return movieRepository.bulkUpdateStatus(ids, status);
    }

    /** Xoá nhiều phim cùng lúc (bulk action). */
    @Transactional
    public void bulkDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        movieRepository.deleteAllById(ids);
    }

    /** Thống kê vận hành thật theo phim cho modal chi tiết. */
    @Transactional(readOnly = true)
    public MovieStatsResponse getMovieStats(Integer movieId) {
        LocalDateTime now = LocalDateTime.now();

        BigDecimal revenue = bookingRepository.sumTicketRevenueByMovie(movieId);
        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }
        long ticketsSold = bookingRepository.countTicketsByMovie(movieId);
        long showtimeCount = showtimeRepository.countByMovieId(movieId);

        long pastSold = bookingRepository.countPastTicketsByMovie(movieId, now);
        long pastCapacity = showtimeRepository.sumPastCapacityByMovie(movieId, now);
        double occupancyRate = pastCapacity > 0
                ? BigDecimal.valueOf(pastSold * 100.0 / pastCapacity)
                        .setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        List<Object[]> rows = bookingRepository.ticketClassDistributionByMovie(movieId);
        List<ClassRevenue> distribution = new ArrayList<>();
        for (Object[] row : rows) {
            String name = (String) row[0];
            BigDecimal classRevenue = (BigDecimal) row[1];
            long count = ((Number) row[2]).longValue();
            double percentage = revenue.signum() > 0
                    ? classRevenue.multiply(BigDecimal.valueOf(100))
                        .divide(revenue, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            distribution.add(ClassRevenue.builder()
                    .name(name)
                    .revenue(classRevenue)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        return MovieStatsResponse.builder()
                .ticketRevenue(revenue)
                .ticketsSold(ticketsSold)
                .showtimeCount(showtimeCount)
                .occupancyRate(occupancyRate)
                .classDistribution(distribution)
                .build();
    }
}
