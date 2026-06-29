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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                .trailerUrl(movie.getTrailerUrl())
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
        if (movie.getTitle() != null && movieRepository.existsByTitleIgnoreCase(movie.getTitle().trim())) {
            throw new IllegalStateException("Thêm phim thất bại. Tên phim hoặc mã ID đã tồn tại trên hệ thống!");
        }
        return movieRepository.save(movie);
    }

    public Movie updateMovie(Integer id, Movie movieDetails) {
        Movie existingMovie = movieRepository.findById(id).orElse(null);
        if (existingMovie != null) {
            if (movieDetails.getTitle() != null
                    && movieRepository.existsByTitleIgnoreCaseAndIdNot(movieDetails.getTitle().trim(), id)) {
                throw new IllegalStateException("Cập nhật thất bại. Tên phim đã tồn tại trên hệ thống!");
            }
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

    @Transactional
    public void deleteMovie(Integer id) {
        Movie m = movieRepository.findById(id).orElse(null);
        if (m == null) return;
        String reason = blockDeleteReason(id);
        if (reason != null) {
            throw new IllegalStateException(reason);
        }
        movieRepository.deleteById(id);
    }

    /** Lý do KHÔNG cho xoá cứng phim (null = được phép xoá). Bảo vệ dữ liệu hoá đơn/lịch chiếu. */
    private String blockDeleteReason(Integer id) {
        if (bookingRepository.countTicketsByMovie(id) > 0) {
            return "Không thể xóa phim này! Phim đã phát sinh lịch sử hóa đơn/giao dịch bán vé. "
                    + "Bạn chỉ có thể chuyển sang trạng thái 'Ngừng chiếu'.";
        }
        if (showtimeRepository.countByMovieId(id) > 0) {
            return "Không thể xóa! Phim đang có lịch chiếu được lên lịch sẵn. Vui lòng xóa lịch chiếu trước.";
        }
        return null;
    }

    /** Lý do KHÔNG cho đổi sang trạng thái mới (null = hợp lệ); kèm tên phim cho thông báo. */
    private String blockStatusReason(Integer id, String title, String status, LocalDateTime now) {
        if ("active".equalsIgnoreCase(status) && showtimeRepository.countByMovieId(id) == 0) {
            return "Không thể chuyển '" + title + "' sang 'Đang chiếu'. Phim chưa được cấu hình lịch chiếu hợp lệ!";
        }
        if ("archived".equalsIgnoreCase(status)) {
            if (showtimeRepository.countFutureByMovieId(id, now) > 0) {
                return "Không thể ngừng chiếu '" + title + "'. Hiện vẫn còn suất chiếu chưa hoàn tất!";
            }
            if (bookingRepository.countActiveHoldsByMovie(id) > 0) {
                return "Không thể ngừng chiếu '" + title + "'. Có vé đang chờ thanh toán!";
            }
        }
        return null;
    }

    /**
     * Đổi trạng thái nhiều phim (cũng dùng cho đổi nhanh 1 phim). Validate từng phim;
     * trả về {@code {updated, blocked:[lý do...]}} để FE hiện toast chi tiết / một phần.
     */
    @Transactional
    public Map<String, Object> bulkUpdateStatus(List<Integer> ids, String status) {
        List<String> blocked = new ArrayList<>();
        List<Integer> okIds = new ArrayList<>();
        if (ids != null && status != null && !status.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            for (Integer id : ids) {
                Movie m = movieRepository.findById(id).orElse(null);
                if (m == null) continue;
                String reason = blockStatusReason(id, m.getTitle(), status, now);
                if (reason != null) {
                    blocked.add(reason);
                    continue;
                }
                okIds.add(id);
            }
        }
        int updated = okIds.isEmpty() ? 0 : movieRepository.bulkUpdateStatus(okIds, status);
        Map<String, Object> result = new HashMap<>();
        result.put("updated", updated);
        result.put("blocked", blocked);
        return result;
    }

    /**
     * Xoá nhiều phim. Validate từng phim (hoá đơn / lịch chiếu); trả
     * {@code {deleted, blocked:[tên phim...]}} để FE hiện toast thành công một phần.
     */
    @Transactional
    public Map<String, Object> bulkDelete(List<Integer> ids) {
        List<String> blocked = new ArrayList<>();
        int deleted = 0;
        if (ids != null) {
            for (Integer id : ids) {
                Movie m = movieRepository.findById(id).orElse(null);
                if (m == null) continue;
                if (blockDeleteReason(id) != null) {
                    blocked.add(m.getTitle());
                    continue;
                }
                movieRepository.deleteById(id);
                deleted++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted);
        result.put("blocked", blocked);
        return result;
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
