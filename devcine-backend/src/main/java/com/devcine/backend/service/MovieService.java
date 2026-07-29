package com.devcine.backend.service;

import com.devcine.backend.dto.request.MovieRequest;
import com.devcine.backend.entity.Category;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.CategoryRepository;
import com.devcine.backend.repository.MovieRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    @Autowired
    private BannerSyncService bannerSyncService;

    @Autowired
    private CategoryRepository categoryRepository;

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

    private static final Pattern YOUTUBE_PATTERN =
            Pattern.compile("^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.*$");

    /**
     * Lớp bảo vệ phía server (double protection) cho dữ liệu phim: chặn giá trị sai định dạng/vượt ngưỡng
     * dù client có bị bỏ qua. Chỉ kiểm tra giá trị ĐÃ CÓ (trừ tên phim luôn bắt buộc) để không chặn nhầm
     * khi sửa phim cũ thiếu dữ liệu. Ném {@link IllegalStateException} -> controller trả 400 kèm thông báo.
     */
    private void validateMoviePayload(Movie m) {
        String title = m.getTitle() == null ? "" : m.getTitle().trim();
        if (title.length() < 2 || title.length() > 150)
            throw new IllegalStateException("Tên phim phải từ 2 đến 150 ký tự.");
        if (m.getDurationMins() != null && (m.getDurationMins() < 30 || m.getDurationMins() > 300))
            throw new IllegalStateException("Thời lượng phim phải từ 30 đến 300 phút.");
        if (m.getProductionYear() != null && (m.getProductionYear() < 2020 || m.getProductionYear() > 2035))
            throw new IllegalStateException("Năm sản xuất phải từ 2020 đến 2035.");
        if (m.getTrailerUrl() != null && !m.getTrailerUrl().isBlank()
                && !YOUTUBE_PATTERN.matcher(m.getTrailerUrl().trim()).matches())
            throw new IllegalStateException("Đường dẫn Trailer phải là link Youtube hợp lệ.");
        if (m.getDescription() != null && m.getDescription().length() > 1000)
            throw new IllegalStateException("Tóm tắt nội dung tối đa 1000 ký tự.");
        if (m.getInternalNotes() != null && m.getInternalNotes().length() > 500)
            throw new IllegalStateException("Ghi chú nội bộ tối đa 500 ký tự.");
    }

    @Transactional
    public Movie createMovie(MovieRequest request) {
        Movie movie = new Movie();
        applyRequest(movie, request);
        validateMoviePayload(movie);
        if (movie.getTitle() != null && movieRepository.existsByTitleIgnoreCase(movie.getTitle().trim())) {
            throw new IllegalStateException("Thêm phim thất bại. Tên phim hoặc mã ID đã tồn tại trên hệ thống!");
        }
        Movie saved = movieRepository.save(movie);
        // Đồng bộ banner theo phim theo cờ showOnBanner (bật -> tạo banner, tắt -> không có).
        bannerSyncService.applyMovieFlag(saved.getId(), Boolean.TRUE.equals(saved.getShowOnBanner()), saved.getTitle());
        return saved;
    }

    @Transactional
    public Movie updateMovie(Integer id, MovieRequest request) {
        Movie existingMovie = movieRepository.findById(id).orElse(null);
        if (existingMovie == null) {
            return null;
        }
        applyRequest(existingMovie, request);
        validateMoviePayload(existingMovie);
        if (existingMovie.getTitle() != null
                && movieRepository.existsByTitleIgnoreCaseAndIdNot(existingMovie.getTitle().trim(), id)) {
            throw new IllegalStateException("Cập nhật thất bại. Tên phim đã tồn tại trên hệ thống!");
        }
        Movie saved = movieRepository.save(existingMovie);
        // Đồng bộ lại banner theo phim theo cờ showOnBanner vừa cập nhật.
        bannerSyncService.applyMovieFlag(saved.getId(), Boolean.TRUE.equals(saved.getShowOnBanner()), saved.getTitle());
        return saved;
    }

    /** Map dữ liệu từ request DTO sang entity (dùng chung cho tạo & cập nhật). */
    private void applyRequest(Movie movie, MovieRequest r) {
        movie.setTitle(r.getTitle());
        movie.setSlug(r.getSlug());
        movie.setDurationMins(r.getDurationMins());
        movie.setAgeRating(r.getAgeRating());
        movie.setReleaseDate(r.getReleaseDate());
        movie.setEndDate(r.getEndDate());
        movie.setStatus(r.getStatus());
        movie.setCountry(r.getCountry());
        movie.setRating(r.getRating());
        movie.setPosterUrl(r.getPosterUrl());
        movie.setBannerUrl(r.getBannerUrl());
        movie.setShowOnBanner(r.getShowOnBanner());
        movie.setTrailerUrl(r.getTrailerUrl());
        movie.setFormat(r.getFormat());
        movie.setSupportedFormats(r.getSupportedFormats());
        movie.setTitleVietnamese(r.getTitleVietnamese());
        movie.setProductionYear(r.getProductionYear());
        movie.setLanguage(r.getLanguage());
        movie.setDescription(r.getDescription());
        movie.setOriginalLanguage(r.getOriginalLanguage());
        movie.setVersionType(r.getVersionType());
        movie.setInternalNotes(r.getInternalNotes());
        movie.setStartDate(r.getStartDate());
        movie.setDirector(r.getDirector());
        movie.setCastMembers(r.getCastMembers());
        movie.setRatingCount(r.getRatingCount());
        movie.setGenres(resolveGenres(r.getGenres()));
    }

    /** Chuyển danh sách id thể loại từ FE thành các Category đang quản lý (bỏ id không tồn tại). */
    private Set<Category> resolveGenres(Set<MovieRequest.GenreRef> refs) {
        if (refs == null) {
            return null;
        }
        List<Integer> ids = refs.stream()
                .map(MovieRequest.GenreRef::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(categoryRepository.findAllById(ids));
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
        // Dọn banner theo phim để không còn banner mồ côi trỏ tới phim đã xoá.
        bannerSyncService.applyMovieFlag(id, false, null);
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
     * Tự đồng bộ trạng thái phim theo lịch phát hành + suất chiếu (gọi bởi Cron Job hằng ngày).
     * KHÔNG thay thế thao tác thủ công của admin — chỉ bù các phim admin quên chuyển.
     *
     * <p>Đọc CẢ HAI danh sách ứng viên trước rồi mới cập nhật (snapshot theo trạng thái đầu lượt):
     * nhờ vậy phim vừa được kích hoạt trong cùng lượt sẽ KHÔNG bị ngừng chiếu ngay dù chưa có
     * suất tương lai — nó được sống trọn 1 ngày, tới lượt hôm sau mới xét ngừng.</p>
     *
     * <p>Ngừng chiếu tự động còn tôn trọng guard "vé đang giữ" (xem {@code findActiveIdsToArchive}):
     * phim còn Booking HOLD sẽ KHÔNG bị archive, tránh phá luồng thanh toán dang dở của khách.</p>
     *
     * @param today thời điểm "hôm nay" (theo múi giờ VN, do scheduler truyền vào để dễ test)
     * @param now   mốc hiện tại để xác định "suất chiếu tương lai"
     * @return {@code {activated, archived}} — số phim đã lật mỗi chiều
     */
    @Transactional
    public Map<String, Integer> autoSyncStatuses(LocalDate today, LocalDateTime now) {
        List<Integer> toActivate = movieRepository.findUpcomingIdsToActivate(today);
        List<Integer> toArchive = movieRepository.findActiveIdsToArchive(today, now);

        int activated = toActivate.isEmpty() ? 0 : movieRepository.bulkUpdateStatus(toActivate, "active");
        int archived = toArchive.isEmpty() ? 0 : movieRepository.bulkUpdateStatus(toArchive, "archived");

        Map<String, Integer> result = new HashMap<>();
        result.put("activated", activated);
        result.put("archived", archived);
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
                bannerSyncService.applyMovieFlag(id, false, null); // dọn banner theo phim đã xoá
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
