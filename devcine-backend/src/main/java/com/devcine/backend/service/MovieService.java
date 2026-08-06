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

    private static final java.time.ZoneId VN_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    /** Ngày đã chạy auto-sync gần nhất — chặn read-path ghi DB lặp lại trong cùng ngày. */
    private final java.util.concurrent.atomic.AtomicReference<LocalDate> lastAutoSync =
            new java.util.concurrent.atomic.AtomicReference<>(null);

    public List<MovieSummaryDTO> getAllMovies() {
        List<MovieSummaryDTO> list = movieRepository.findAllWithGenres().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        return enrichAndSort(list);
    }

    /** Trạng thái phim bị loại khỏi mọi danh sách công khai (ngừng chiếu / huỷ / vô hiệu hoá). */
    private static final Set<String> HIDDEN_STATUSES = Set.of("archived", "cancelled", "disabled");

    private boolean isVisible(Movie m) {
        String s = m.getStatus() == null ? "" : m.getStatus().trim().toLowerCase();
        return !HIDDEN_STATUSES.contains(s);
    }

    /**
     * Phim ĐANG CHIẾU — lọc theo NGÀY (nguồn sự thật), không phụ thuộc admin có nhớ đổi status hay không:
     * <ul>
     *   <li>đã tới/qua ngày khởi chiếu: {@code releaseDate != null && releaseDate <= today}</li>
     *   <li>chưa hết hạn chiếu: {@code endDate == null || endDate >= today}</li>
     *   <li>không ở trạng thái ẩn (archived/cancelled/disabled)</li>
     * </ul>
     * Nhờ vậy phim hết hạn (endDate < today) bị loại DỨT ĐIỂM dù status còn 'active'.
     */
    @Transactional
    public List<MovieSummaryDTO> getNowShowing() {
        syncIfStale();
        LocalDate today = LocalDate.now(VN_ZONE);
        List<MovieSummaryDTO> list = movieRepository.findAllWithGenres().stream()
                .filter(this::isVisible)
                .filter(m -> m.getReleaseDate() != null && !m.getReleaseDate().isAfter(today)) // releaseDate <= today
                .filter(m -> m.getEndDate() == null || !m.getEndDate().isBefore(today))        // endDate == null || endDate >= today
                .map(this::toSummary)
                .collect(Collectors.toList());
        return enrichAndSort(list);
    }

    /**
     * Phim SẮP CHIẾU — CHỈ lấy phim NGHIÊM NGẶT chưa tới ngày khởi chiếu: {@code releaseDate > today}.
     * Loại bỏ mọi phim đã tới/qua ngày khởi chiếu (releaseDate <= today) dù status còn 'upcoming',
     * và loại phim ở trạng thái ẩn.
     */
    @Transactional
    public List<MovieSummaryDTO> getUpcoming() {
        syncIfStale();
        LocalDate today = LocalDate.now(VN_ZONE);
        List<MovieSummaryDTO> list = movieRepository.findAllWithGenres().stream()
                .filter(this::isVisible)
                .filter(m -> m.getReleaseDate() != null && m.getReleaseDate().isAfter(today)) // releaseDate > today (nghiêm ngặt)
                .map(this::toSummary)
                .collect(Collectors.toList());
        return enrichAndSort(list);
    }

    public List<MovieSummaryDTO> searchMovies(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        List<MovieSummaryDTO> list = movieRepository.searchMovies(keyword.trim()).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        return enrichAndSort(list);
    }

    private List<MovieSummaryDTO> enrichAndSort(List<MovieSummaryDTO> movies) {
        if (movies == null || movies.isEmpty()) return movies;
        
        List<Integer> ids = movies.stream().map(MovieSummaryDTO::getId).collect(Collectors.toList());
        List<Object[]> counts = bookingRepository.countTicketsByMovieIds(ids);
        
        Map<Integer, Long> salesMap = new HashMap<>();
        for (Object[] row : counts) {
            salesMap.put((Integer) row[0], ((Number) row[1]).longValue());
        }
        
        movies.forEach(m -> m.setTicketSales(salesMap.getOrDefault(m.getId(), 0L)));
        
        movies.sort(java.util.Comparator.comparing(MovieSummaryDTO::getTicketSales, java.util.Comparator.reverseOrder())
                .thenComparing(MovieSummaryDTO::getId, java.util.Comparator.reverseOrder()));
        
        return movies;
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
            throw new RuntimeException(reason);
        }
        Movie movie = movieRepository.findById(id).orElseThrow();
        movie.setStatus("archived");
        movieRepository.save(movie);
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

    /**
     * Lý do KHÔNG cho đổi sang trạng thái mới (null = hợp lệ); kèm tên phim cho thông báo.
     *
     * <p><b>ĐÃ GỠ chặn cứng "phải có lịch chiếu" khi chuyển sang 'active'/'upcoming'</b> — Admin được
     * tự do bật ĐANG CHIẾU / SẮP CHIẾU dù chưa cấu hình suất chiếu (FE chỉ nhắc bằng Toast Warning nhẹ),
     * tránh phim bị kẹt ở "Sắp chiếu". Chỉ còn giữ guard khi NGỪNG CHIẾU ('archived') để không phá
     * luồng suất chiếu tương lai / vé đang thanh toán dở.</p>
     */
    private String blockStatusReason(Integer id, String title, String status, LocalDateTime now) {
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
        // Nhắc nhẹ (KHÔNG chặn): số phim vừa bật ĐANG CHIẾU nhưng chưa có suất chiếu nào.
        long noShowtime = ("active".equalsIgnoreCase(status) && !okIds.isEmpty())
                ? movieRepository.countWithoutShowtimes(okIds) : 0L;
        result.put("noShowtime", noShowtime);
        return result;
    }

    /**
     * TỰ ĐỘNG ĐỒNG BỘ TRẠNG THÁI PHIM theo NGÀY (nguồn sự thật), 3 quy tắc mỗi quy tắc 1 bulk UPDATE:
     * <ol>
     *   <li><b>Hết hạn:</b> endDate &lt; today ⇒ 'archived'</li>
     *   <li><b>Đến ngày chiếu:</b> releaseDate &lt;= today &amp; (endDate null hoặc endDate &gt;= today) &amp; chưa archived ⇒ 'active'</li>
     *   <li><b>Chưa chiếu:</b> releaseDate &gt; today &amp; chưa archived ⇒ 'upcoming'</li>
     * </ol>
     *
     * <p>Thứ tự archive → active → upcoming để phim vừa hết hạn (archived) không bị 2 quy tắc sau
     * kéo ngược. Mọi quy tắc TÔN TRỌNG 'archived' thủ công của admin (điều kiện status &lt;&gt; 'archived').</p>
     *
     * <p>Gọi bởi: startup ({@code ApplicationReadyEvent}), cron 00:00 hằng ngày, và lười (once-per-day)
     * ngay trước khi trả {@link #getNowShowing()}/{@link #getUpcoming()}.</p>
     *
     * @return {@code {archived, activated, upcoming}} — số phim đổi trạng thái ở mỗi quy tắc
     */
    @Transactional
    public Map<String, Integer> autoSyncMovieStatuses() {
        return runAutoSync();
    }

    /** Lõi đồng bộ (không tự mở transaction — dùng lại được từ read-path đã có transaction). */
    private Map<String, Integer> runAutoSync() {
        LocalDate today = LocalDate.now(VN_ZONE);
        int archived = movieRepository.syncArchiveExpired(today);   // Quy tắc 1
        int activated = movieRepository.syncActivateReleased(today); // Quy tắc 2
        int upcoming = movieRepository.syncUpcomingFuture(today);    // Quy tắc 3
        lastAutoSync.set(today);
        Map<String, Integer> result = new HashMap<>();
        result.put("archived", archived);
        result.put("activated", activated);
        result.put("upcoming", upcoming);
        return result;
    }

    /** Chạy đồng bộ TỐI ĐA 1 LẦN/NGÀY cho read-path (tránh ghi DB mỗi request tải trang chủ). */
    private void syncIfStale() {
        if (!LocalDate.now(VN_ZONE).equals(lastAutoSync.get())) {
            runAutoSync();
        }
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
                m.setStatus("archived");
                movieRepository.save(m);
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
