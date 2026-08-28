package com.devcine.backend.service;

import com.devcine.backend.dto.response.CinemaShowtimeDTO;
import com.devcine.backend.dto.response.ShowtimeDTO;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.entity.MovieFormat;
import com.devcine.backend.entity.Room;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.dto.request.ShowtimeRequest;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.MovieRepository;
import com.devcine.backend.repository.RoomRepository;
import com.devcine.backend.repository.MovieFormatRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import com.devcine.backend.repository.SeatRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.dto.response.MovieCardDTO;
import com.devcine.backend.dto.response.PublicShowtimeDTO;
import com.devcine.backend.dto.response.SneakPreviewDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.devcine.backend.dto.projection.ShowtimePublicProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final CinemaRepository cinemaRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final MovieFormatRepository movieFormatRepository;
    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatLayoutSnapshotService seatLayoutSnapshotService;
    private final SystemSettingService systemSettingService;

    // Giới hạn kết thúc tối đa cho ca đêm (03:30 sáng = 27 giờ 30 phút tính từ 00:00 hôm trước)
    private static final int MAX_OVERNIGHT_END_MINUTES = 27 * 60 + 30;
    // Ngưỡng trần tối đa cho 1 đợt tạo lịch hàng loạt (chống DoS / tràn bộ nhớ)
    private static final int MAX_BATCH_SLOTS_LIMIT = 500;

    public static boolean isFormatCompatibleWithRoom(MovieFormat format, Room room) {
        if (format == null || room == null) return true;
        String fmt = (format.getName() != null ? format.getName() : "").trim().toUpperCase();
        String rType = (room.getType() != null ? room.getType() : "STANDARD").trim().toUpperCase();

        if (fmt.contains("SUPERPLEX") || fmt.contains("IMAX")) {
            return rType.contains("SUPERPLEX") || rType.contains("IMAX");
        }
        if (fmt.contains("COMFORT") || fmt.contains("CINE_COMFORT")) {
            return rType.contains("COMFORT") || rType.contains("CINE_COMFORT");
        }
        return true;
    }

    public List<String> getAllCities() {
        return cinemaRepository.findAllCities();
    }

    /**
     * Lấy danh sách các phim sắp chiếu có suất chiếu sớm đang mở bán
     * kèm tóm tắt ngày, giờ, rạp để phục vụ banner/slider Sneak Preview trang chủ.
     */
    public List<SneakPreviewDTO> getSneakPreviews() {
        LocalDateTime now = LocalDateTime.now();
        List<Showtime> earlyShowtimes = showtimeRepository.findActiveEarlyShowtimes(now);
        if (earlyShowtimes.isEmpty()) {
            return Collections.emptyList();
        }

        // Gom nhóm theo Movie ID (giữ thứ tự xuất hiện)
        Map<Integer, List<Showtime>> showtimesByMovie = earlyShowtimes.stream()
                .collect(Collectors.groupingBy(s -> s.getMovie().getId(), LinkedHashMap::new, Collectors.toList()));

        List<SneakPreviewDTO> result = new ArrayList<>();
        DateTimeFormatter dotDateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter shortDateFormat = DateTimeFormatter.ofPattern("dd.MM");
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

        long totalActiveCinemas = cinemaRepository.count();

        for (Map.Entry<Integer, List<Showtime>> entry : showtimesByMovie.entrySet()) {
            List<Showtime> list = entry.getValue();
            if (list.isEmpty()) continue;

            Movie m = list.get(0).getMovie();

            // Tập hợp ngày chiếu sớm (đã sort)
            List<LocalDate> dates = list.stream()
                    .map(s -> s.getStartTime().toLocalDate())
                    .distinct()
                    .sorted()
                    .toList();

            String formattedDates = "";
            String defaultDate = "";
            if (!dates.isEmpty()) {
                defaultDate = dates.get(0).toString(); // yyyy-MM-dd
                if (dates.size() == 1) {
                    formattedDates = dates.get(0).format(dotDateFormat);
                } else {
                    LocalDate first = dates.get(0);
                    LocalDate last = dates.get(dates.size() - 1);
                    if (first.getYear() == last.getYear()) {
                        formattedDates = first.format(shortDateFormat) + " - " + last.format(dotDateFormat);
                    } else {
                        formattedDates = first.format(dotDateFormat) + " - " + last.format(dotDateFormat);
                    }
                }
            }

            // Tập hợp khung giờ mẫu (lấy các giờ bắt đầu distinct)
            List<String> distinctTimes = list.stream()
                    .map(s -> s.getStartTime().format(timeFormat))
                    .distinct()
                    .sorted()
                    .toList();

            String formattedTimes = "";
            if (!distinctTimes.isEmpty()) {
                if (distinctTimes.size() <= 3) {
                    formattedTimes = String.join(" & ", distinctTimes);
                } else {
                    formattedTimes = distinctTimes.get(0) + " - " + distinctTimes.get(distinctTimes.size() - 1) + " (" + distinctTimes.size() + " khung giờ)";
                }
            }

            // Tập hợp cụm rạp
            Set<String> cinemaNames = list.stream()
                    .map(s -> s.getRoom().getCinema().getName())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            String locationSummary = "Toàn hệ thống";
            if (cinemaNames.size() == 1) {
                locationSummary = cinemaNames.iterator().next();
            } else if (cinemaNames.size() >= 3 || (totalActiveCinemas > 0 && cinemaNames.size() >= totalActiveCinemas)) {
                locationSummary = "Toàn hệ thống (" + cinemaNames.size() + " cụm rạp)";
            } else {
                locationSummary = String.join(", ", cinemaNames);
            }

            Set<String> genreNames = m.getGenres() == null ? Collections.emptySet() :
                    m.getGenres().stream().map(com.devcine.backend.entity.Category::getName).collect(Collectors.toSet());

            result.add(SneakPreviewDTO.builder()
                    .movieId(m.getId())
                    .title(m.getTitle())
                    .titleVietnamese(m.getTitleVietnamese())
                    .posterUrl(m.getPosterUrl())
                    .bannerUrl(m.getBannerUrl())
                    .description(m.getDescription())
                    .durationMins(m.getDurationMins())
                    .ageRating(m.getAgeRating())
                    .releaseDate(m.getReleaseDate())
                    .genres(genreNames)
                    .formattedDates(formattedDates)
                    .formattedTimes(formattedTimes)
                    .locationSummary(locationSummary)
                    .defaultDate(defaultDate)
                    .totalShowtimes(list.size())
                    .build());
        }

        return result;
    }


    /** Danh sách các rạp hiện có suất chiếu sắp tới (có cache) */
    @Cacheable(value = "cinemas_showtimes", unless = "#result == null")
    public List<Map<String, Object>> getCinemasWithUpcomingShowtimes() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(systemSettingService.getBookingLateMinutes());
        List<Cinema> cinemas = showtimeRepository.findCinemasWithUpcomingShowtimes(cutoff);
        return cinemas.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("address", c.getAddress());
            m.put("city", c.getCity());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * Suất chiếu sắp tới của 1 RẠP (dùng cho /lich-chieu sau khi chọn rạp, có
     * cache)
     */
    @Cacheable(value = "showtimes_cinema", key = "#cinemaId", unless = "#result == null")
    public List<PublicShowtimeDTO> getUpcomingShowtimesByCinema(Integer cinemaId) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(systemSettingService.getBookingLateMinutes());
        List<ShowtimePublicProjection> projections = showtimeRepository.findUpcomingProjectionsByCinemaId(cinemaId,
                cutoff);
        if (projections.isEmpty())
            return Collections.emptyList();

        Map<Integer, Set<String>> genresByMovieId = getActiveMovieGenresMap();

        Set<Integer> roomIds = projections.stream().map(ShowtimePublicProjection::getRoomId)
                .collect(Collectors.toSet());
        Set<Integer> showtimeIds = projections.stream().map(ShowtimePublicProjection::getId)
                .collect(Collectors.toSet());
        Map<Integer, Integer> sellableByRoom = new HashMap<>();
        Map<Integer, Integer> reservedByShowtime = new HashMap<>();
        if (!roomIds.isEmpty()) {
            for (Object[] row : seatRepository.countSellableSeatsByRoomIds(roomIds)) {
                sellableByRoom.put((Integer) row[0], ((Number) row[1]).intValue());
            }
        }
        if (!showtimeIds.isEmpty()) {
            for (Object[] row : bookingSeatRepository.countReservedByShowtimeIds(showtimeIds)) {
                reservedByShowtime.put((Integer) row[0], ((Number) row[1]).intValue());
            }
        }

        return projections.stream().map(p -> {
            PublicShowtimeDTO dto = toPublicDTOFromProjection(p,
                    genresByMovieId.getOrDefault(p.getMovieId(), Collections.emptySet()));
            int total = sellableByRoom.getOrDefault(p.getRoomId(), 0);
            int reserved = reservedByShowtime.getOrDefault(p.getId(), 0);
            dto.setTotalSeats(total);
            dto.setAvailableSeats(Math.max(0, total - reserved));
            return dto;
        }).collect(Collectors.toList());
    }

    @Cacheable(value = "upcomingShowtimes", unless = "#result == null")
    public List<PublicShowtimeDTO> getAllUpcomingShowtimes() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(systemSettingService.getBookingLateMinutes());
        List<ShowtimePublicProjection> projections = showtimeRepository.findAllUpcomingProjections(cutoff);
        if (projections.isEmpty())
            return Collections.emptyList();

        Map<Integer, Set<String>> genresByMovieId = getActiveMovieGenresMap();

        // Tình trạng ghế tính 1 LẦN cho toàn bộ suất (2 query gộp, tránh N+1):
        // - sellable/phòng = ghế active & không bảo trì/khóa
        // - reserved/suất = ghế SOLD/HOLD
        Set<Integer> roomIds = projections.stream().map(ShowtimePublicProjection::getRoomId)
                .collect(Collectors.toSet());
        Set<Integer> showtimeIds = projections.stream().map(ShowtimePublicProjection::getId)
                .collect(Collectors.toSet());
        Map<Integer, Integer> sellableByRoom = new HashMap<>();
        Map<Integer, Integer> reservedByShowtime = new HashMap<>();
        if (!roomIds.isEmpty()) {
            for (Object[] row : seatRepository.countSellableSeatsByRoomIds(roomIds)) {
                sellableByRoom.put((Integer) row[0], ((Number) row[1]).intValue());
            }
        }
        if (!showtimeIds.isEmpty()) {
            for (Object[] row : bookingSeatRepository.countReservedByShowtimeIds(showtimeIds)) {
                reservedByShowtime.put((Integer) row[0], ((Number) row[1]).intValue());
            }
        }

        return projections.stream().map(p -> {
            PublicShowtimeDTO dto = toPublicDTOFromProjection(p,
                    genresByMovieId.getOrDefault(p.getMovieId(), Collections.emptySet()));
            int total = sellableByRoom.getOrDefault(p.getRoomId(), 0);
            int reserved = reservedByShowtime.getOrDefault(p.getId(), 0);
            dto.setTotalSeats(total);
            dto.setAvailableSeats(Math.max(0, total - reserved));
            return dto;
        }).collect(Collectors.toList());
    }

    /** Bản đồ thể loại của phim đang chiếu (chống Cartesian product trong SQL). */
    private Map<Integer, Set<String>> getActiveMovieGenresMap() {
        Map<Integer, Set<String>> map = new HashMap<>();
        for (Movie m : movieRepository.findVisibleWithGenres()) {
            if (m.getGenres() != null) {
                map.put(m.getId(), m.getGenres().stream().map(com.devcine.backend.entity.Category::getName)
                        .collect(Collectors.toSet()));
            }
        }
        return map;
    }

    /** Mapper từ Projection (không chứa layout_data) -> PublicShowtimeDTO. */
    private PublicShowtimeDTO toPublicDTOFromProjection(ShowtimePublicProjection p, Set<String> genres) {
        return PublicShowtimeDTO.builder()
                .id(p.getId())
                .startTime(p.getStartTime())
                .endTime(p.getEndTime())
                .status(p.getStatus())
                .cinemaId(p.getCinemaId())
                .cinemaName(p.getCinemaName())
                .cinemaAddress(p.getCinemaAddress())
                .movieId(p.getMovieId())
                .movieTitle(p.getMovieTitle())
                .movieTitleVietnamese(p.getMovieTitleVietnamese())
                .movieDurationMins(p.getMovieDurationMins())
                .moviePosterUrl(p.getMoviePosterUrl())
                .movieAgeRating(p.getMovieAgeRating())
                .movieCountry(p.getMovieCountry())
                .movieReleaseDate(p.getMovieReleaseDate())
                .movieDescription(p.getMovieDescription())
                .movieGenres(genres != null ? genres : Collections.emptySet())
                .movieRating(p.getMovieRating())
                .movieRatingCount(p.getMovieRatingCount())
                .movieTrailerUrl(p.getMovieTrailerUrl())
                .formatId(p.getFormatId())
                .formatName(p.getFormatName())
                .roomId(p.getRoomId())
                .roomName(p.getRoomName())
                .roomTypeName(p.getRoomType() != null ? p.getRoomType().replace("_", " ") : null)
                .build();
    }

    /**
     * Mapper dùng chung: Showtime -> PublicShowtimeDTO (DTO phẳng cho FE tự nhóm).
     */
    private PublicShowtimeDTO toPublicDTO(Showtime s) {
        return PublicShowtimeDTO.builder()
                .id(s.getId())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .status(s.getStatus())
                .cinemaId(s.getRoom().getCinema().getId())
                .cinemaName(s.getRoom().getCinema().getName())
                .cinemaAddress(s.getRoom().getCinema().getAddress())
                .movieId(s.getMovie().getId())
                .movieTitle(s.getMovie().getTitle())
                .movieTitleVietnamese(s.getMovie().getTitleVietnamese())
                .movieDurationMins(s.getMovie().getDurationMins())
                .moviePosterUrl(s.getMovie().getPosterUrl())
                .movieAgeRating(s.getMovie().getAgeRating())
                .movieCountry(s.getMovie().getCountry())
                .movieReleaseDate(s.getMovie().getReleaseDate())
                .movieDescription(s.getMovie().getDescription())
                .movieGenres(s.getMovie().getGenres() != null
                        ? s.getMovie().getGenres().stream().map(g -> g.getName()).collect(Collectors.toSet())
                        : new HashSet<>())
                .movieRating(s.getMovie().getRating())
                .movieRatingCount(s.getMovie().getRatingCount())
                .movieTrailerUrl(s.getMovie().getTrailerUrl())
                .formatId(s.getFormat().getId())
                .formatName(s.getFormat().getName())
                .roomId(s.getRoom().getId())
                .roomName(s.getRoom().getName())
                .roomTypeName(s.getRoom().getType() != null ? s.getRoom().getType().replace("_", " ") : null)
                .build();
    }

    // ===== Trang Lịch chiếu: lọc phía server + phân trang =====

    /**
     * Khoảng thời gian của một ngày; với hôm nay thì bắt đầu từ "cutoff" (now - lateMinutes) để ẩn
     * suất đã quá 10 phút sau khi bắt đầu.
     */
    private LocalDateTime[] dayRange(String date) {
        LocalDate d = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(systemSettingService.getBookingLateMinutes());
        LocalDateTime start = d.atStartOfDay();
        if (start.isBefore(cutoff))
            start = cutoff; // hôm nay: ẩn suất đã quá 10 phút sau giờ bắt đầu
        return new LocalDateTime[] { start, d.atTime(23, 59, 59) };
    }

    public List<Map<String, Object>> getCinemasByCity(String city) {
        List<Cinema> cinemas = (city != null && !city.isBlank())
                ? cinemaRepository.findByCityIgnoreCaseOrderByNameAsc(city)
                : cinemaRepository.findAllByOrderByNameAsc();
        return cinemas.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("address", c.getAddress());
            m.put("city", c.getCity());
            return m;
        }).collect(Collectors.toList());
    }

    public Page<MovieCardDTO> getMoviesWithShowtimes(String city, String date, String q, int page, int size) {
        LocalDateTime[] range = dayRange(date);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Movie> movies = showtimeRepository.findMoviesWithShowtimes(
                range[0], range[1], city != null ? city : "", q != null ? q : "", pageable);
        return movies.map(m -> MovieCardDTO.builder()
                .id(m.getId())
                .title(m.getTitle())
                .titleVietnamese(m.getTitleVietnamese())
                .posterUrl(m.getPosterUrl())
                .ageRating(m.getAgeRating())
                .durationMins(m.getDurationMins())
                .country(m.getCountry())
                .releaseDate(m.getReleaseDate())
                .genres(m.getGenres() != null ? m.getGenres().stream().map(g -> g.getName()).collect(Collectors.toSet())
                        : new HashSet<>())
                .build());
    }

    public List<PublicShowtimeDTO> getShowtimesByMovieAndDate(Integer movieId, String date, String city) {
        LocalDateTime[] range = dayRange(date);
        return showtimeRepository.findByMovieAndRange(movieId, city != null ? city : "", range[0], range[1])
                .stream().map(this::toPublicDTO).collect(Collectors.toList());
    }

    public List<PublicShowtimeDTO> getShowtimesByCinemaAndDate(Integer cinemaId, String date) {
        LocalDateTime[] range = dayRange(date);
        return showtimeRepository.findByCinemaAndRange(cinemaId, range[0], range[1])
                .stream().map(this::toPublicDTO).collect(Collectors.toList());
    }

    public List<CinemaShowtimeDTO> getShowtimesForMovie(Integer movieId, String city) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(systemSettingService.getBookingLateMinutes());
        List<Showtime> showtimes;

        if (city != null && !city.trim().isEmpty()) {
            showtimes = showtimeRepository.findUpcomingShowtimesByMovieIdAndCity(movieId, city, cutoff);
        } else {
            showtimes = showtimeRepository.findUpcomingShowtimesByMovieId(movieId, cutoff);
        }

        // Group by Cinema
        Map<Cinema, List<Showtime>> byCinema = showtimes.stream()
                .collect(Collectors.groupingBy(s -> s.getRoom().getCinema()));

        // Tính tình trạng ghế 1 LẦN cho tất cả suất (tránh N+1):
        // - sellable/phòng = ghế active & không bảo trì/khóa
        // - reserved/suất = ghế SOLD/HOLD
        Set<Integer> roomIds = showtimes.stream().map(s -> s.getRoom().getId()).collect(Collectors.toSet());
        Set<Integer> showtimeIds = showtimes.stream().map(Showtime::getId).collect(Collectors.toSet());

        Map<Integer, Integer> sellableByRoom = new HashMap<>();
        Map<Integer, Integer> reservedByShowtime = new HashMap<>();
        if (!roomIds.isEmpty()) {
            for (Object[] row : seatRepository.countSellableSeatsByRoomIds(roomIds)) {
                sellableByRoom.put((Integer) row[0], ((Number) row[1]).intValue());
            }
        }
        if (!showtimeIds.isEmpty()) {
            for (Object[] row : bookingSeatRepository.countReservedByShowtimeIds(showtimeIds)) {
                reservedByShowtime.put((Integer) row[0], ((Number) row[1]).intValue());
            }
        }

        List<CinemaShowtimeDTO> result = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Map.Entry<Cinema, List<Showtime>> entry : byCinema.entrySet()) {
            Cinema cinema = entry.getKey();
            List<Showtime> cinemaShowtimes = entry.getValue();

            // Group by Date
            Map<String, List<ShowtimeDTO>> showtimesByDate = new TreeMap<>();

            for (Showtime s : cinemaShowtimes) {
                String dateStr = s.getStartTime().format(dateFormatter);
                int total = sellableByRoom.getOrDefault(s.getRoom().getId(), 0);
                int reserved = reservedByShowtime.getOrDefault(s.getId(), 0);
                int available = Math.max(0, total - reserved);
                ShowtimeDTO dto = ShowtimeDTO.builder()
                        .id(s.getId())
                        .roomId(s.getRoom().getId())
                        .roomName(s.getRoom().getName())
                        .formatId(s.getFormat().getId())
                        .formatName(s.getFormat().getName())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .status(s.getStatus())
                        .movie(s.getMovie().getTitle())
                        .duration(s.getMovie().getDurationMins())
                        .totalSeats(total)
                        .availableSeats(available)
                        .earlyScreening("Xu\u1ea5t chi\u1ebfu s\u1edbm".equals(s.getStatus()))
                        .build();

                showtimesByDate.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(dto);
            }

            CinemaShowtimeDTO cinemaDto = CinemaShowtimeDTO.builder()
                    .cinemaId(cinema.getId())
                    .cinemaName(cinema.getName())
                    .address(cinema.getAddress())
                    .city(cinema.getCity())
                    .showtimesByDate(showtimesByDate)
                    .build();

            result.add(cinemaDto);
        }

        return result;
    }

    public List<ShowtimeDTO> getShowtimesByCinemaId(Integer cinemaId) {
        List<Showtime> showtimes = showtimeRepository.findByCinemaId(cinemaId);
        return showtimes.stream().map(s -> ShowtimeDTO.builder()
                .id(s.getId())
                .roomId(s.getRoom().getId())
                .roomName(s.getRoom().getName())
                .formatId(s.getFormat().getId())
                .formatName(s.getFormat().getName())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .status(s.getStatus())
                .movie(s.getMovie().getTitle())
                .duration(s.getMovie().getDurationMins())
                .earlyScreening("Xu\u1ea5t chi\u1ebfu s\u1edbm".equals(s.getStatus()))
                .build()).collect(Collectors.toList());
    }

    @CacheEvict(value = { "cinemas_showtimes", "showtimes_cinema", "upcomingShowtimes" }, allEntries = true)
    @org.springframework.transaction.annotation.Transactional
    public com.devcine.backend.dto.response.ShowtimeCreateResult createShowtime(ShowtimeRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy phòng chiếu với ID: " + request.getRoomId()));
        MovieFormat format = movieFormatRepository.findById(request.getFormatId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy định dạng với ID: " + request.getFormatId()));

        LocalDateTime startTime = request.getStartTime();
        // NGUỒN DUY NHẤT: thời gian dọn dẹp bốc từ chính phòng
        // (Room.turnaroundTimeMins), không nhận từ FE.
        int turnaround = turnaroundOf(room);
        int duration = movie.getDurationMins() != null ? movie.getDurationMins() : 120;
        LocalDateTime endTime = startTime.plusMinutes(duration + turnaround);

        // ===== Constraint Engine: kiểm soát theo giờ hoạt động của cụm rạp =====
        Cinema cinema = room.getCinema();
        if (cinema != null && "CLOSED".equalsIgnoreCase(cinema.getStatus())) {
            throw new IllegalArgumentException("Không thể tạo suất chiếu cho cụm rạp đã đóng cửa.");
        }
        if (!isFormatCompatibleWithRoom(format, room)) {
            throw new IllegalArgumentException("Phòng chiếu '" + room.getName() + "' (loại "
                    + (room.getType() != null ? room.getType() : "STANDARD")
                    + ") không tương thích với định dạng '" + format.getName() + "'.");
        }
        int[] win = cinemaWindow(cinema); // [openMin, closeMin] (closeMin là giờ bắt đầu suất cuối)
        int startPos = posOf(startTime.toLocalTime(), win[0]);
        int endPos = startPos + duration + turnaround;

        // RULE A — Chặn nếu suất bắt đầu trước giờ mở cửa hoặc sau giờ suất cuối
        if (startPos < win[0] || startPos > win[1]) {
            throw new IllegalArgumentException("Suất chiếu phải bắt đầu trong khung giờ từ "
                    + fmtMin(win[0]) + " đến " + fmtMin(win[1]) + " (giờ suất cuối). Vui lòng chọn giờ khác.");
        }

        // RULE B — Chặn nếu suất chiếu kéo dài quá mốc trần ca đêm (03:30 AM)
        if (endPos > MAX_OVERNIGHT_END_MINUTES) {
            throw new IllegalArgumentException("Suất chiếu kết thúc lúc " + fmtMin(endPos)
                    + ", vượt quá giới hạn ca đêm (03:30). Vui lòng chọn giờ bắt đầu sớm hơn.");
        }

        boolean hasConflict = showtimeRepository.hasConflict(room.getId(), startTime, endTime);
        if (hasConflict) {
            throw new IllegalStateException(
                    "Phòng chiếu đã có lịch trong khung giờ này (Bao gồm thời gian dọn dẹp). Vui lòng chọn giờ khác.");
        }

        // ===== Xuất chiếu sớm: suất trước ngày khởi chiếu chính thức của phim =====
        // Phát hiện tự động — không cần input từ FE; status phân biệt để query public lọc được.
        LocalDate showtimeDate = startTime.toLocalDate();
        boolean isEarlyScreening = movie.getReleaseDate() != null
                && showtimeDate.isBefore(movie.getReleaseDate());
        String showtimeStatus = isEarlyScreening ? "Xuất chiếu sớm" : "Sắp chiếu";

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .room(room)
                .format(format)
                .startTime(startTime)
                .endTime(endTime)
                .status(showtimeStatus)
                // Đông cứng sơ đồ ghế của phòng NGAY lúc tạo suất → suất này có sơ đồ riêng,
                // bất biến.
                .layoutData(seatLayoutSnapshotService.buildSnapshotJson(room.getId()))
                .build();

        Showtime saved = showtimeRepository.save(showtime);

        return com.devcine.backend.dto.response.ShowtimeCreateResult.builder()
                .requiresConfirmation(false)
                .earlyScreening(isEarlyScreening)
                .movieReleaseDate(isEarlyScreening ? movie.getReleaseDate() : null)
                .showtime(ShowtimeDTO.builder()
                        .id(saved.getId())
                        .roomId(room.getId())
                        .roomName(room.getName())
                        .formatId(format.getId())
                        .formatName(format.getName())
                        .startTime(saved.getStartTime())
                        .endTime(saved.getEndTime())
                        .status(saved.getStatus())
                        .movie(movie.getTitle())
                        .duration(movie.getDurationMins())
                        .earlyScreening(isEarlyScreening)
                        .build())
                .build();
    }

    /**
     * Tạo lịch chiếu HÀNG LOẠT cho một phim: sinh tích Descartes (phòng × ngày ×
     * khung giờ),
     * kiểm tra xung đột in-memory chống N+1, chụp snapshot sơ đồ ghế 1 lần/phòng và
     * batch insert.
     */
    @org.springframework.transaction.annotation.Transactional
    public com.devcine.backend.dto.response.BatchShowtimeResult createBatchShowtimes(
            com.devcine.backend.dto.request.BatchShowtimeRequest req) {

        Movie movie = movieRepository.findById(req.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim."));
        MovieFormat format = movieFormatRepository.findById(req.getFormatId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy định dạng phim."));
        int duration = movie.getDurationMins() != null ? movie.getDurationMins() : 120;

        List<Room> rooms = roomRepository.findAllById(req.getRoomIds());
        Map<Integer, Room> roomMap = rooms.stream()
                .collect(Collectors.toMap(Room::getId, r -> r));

        List<java.time.LocalTime> times = new ArrayList<>();
        for (String t : req.getStartTimes()) {
            try {
                times.add(java.time.LocalTime.parse(t.trim()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Khung giờ không hợp lệ: '" + t + "' (định dạng HH:mm).");
            }
        }

        Set<Integer> daysFilter = (req.getDaysOfWeek() != null && !req.getDaysOfWeek().isEmpty())
                ? new HashSet<>(req.getDaysOfWeek())
                : null;

        // KIỂM TRA NGƯỠNG TRẦN AN TOÀN CHO ĐỢT TẠO LỊCH (Batch Limit <= 500)
        int activeDaysCount = 0;
        for (LocalDate d = req.getDateFrom(); !d.isAfter(req.getDateTo()); d = d.plusDays(1)) {
            if (daysFilter == null || daysFilter.contains(d.getDayOfWeek().getValue())) {
                activeDaysCount++;
            }
        }
        long potentialSlots = (long) activeDaysCount * req.getRoomIds().size() * times.size();
        if (potentialSlots > MAX_BATCH_SLOTS_LIMIT) {
            throw new IllegalArgumentException("Yêu cầu tạo lô dự kiến (" + potentialSlots
                    + " suất) vượt quá ngưỡng trần an toàn (tối đa " + MAX_BATCH_SLOTS_LIMIT
                    + " suất/lần). Vui lòng thu hẹp khoảng ngày hoặc danh sách phòng.");
        }

        // Cửa sổ thời gian bao trùm cả lô để nạp suất hiện có 1 lần.
        // Nới đuôi thêm 1 ngày để bắt cả suất hiện có rơi sang rạng sáng hôm sau (qua nửa đêm).
        LocalDateTime windowStart = req.getDateFrom().atStartOfDay();
        LocalDateTime windowEnd = req.getDateTo().plusDays(1).atTime(23, 59, 59);

        // roomId -> danh sách khoảng [start, end) đã chiếm (DB + các suất đã nhận trong lô)
        Map<Integer, List<LocalDateTime[]>> busyByRoom = new HashMap<>();
        for (Showtime s : showtimeRepository.findByRoomsAndWindow(req.getRoomIds(), windowStart, windowEnd)) {
            busyByRoom.computeIfAbsent(s.getRoom().getId(), k -> new ArrayList<>())
                    .add(new LocalDateTime[] { s.getStartTime(), s.getEndTime() });
        }

        // Giờ hoạt động theo TỪNG phòng (mỗi phòng có thể thuộc cụm rạp khác nhau) — tính 1 lần.
        Map<Integer, int[]> windowByRoom = new HashMap<>();
        roomMap.forEach((rid, r) -> windowByRoom.put(rid, cinemaWindow(r.getCinema())));

        LocalDateTime now = LocalDateTime.now();
        List<Showtime> toSave = new ArrayList<>();
        // Snapshot sơ đồ theo TỪNG phòng, dựng 1 lần rồi tái dùng cho mọi suất cùng phòng trong lô (tránh N+1).
        Map<Integer, String> snapshotByRoom = new HashMap<>();
        List<com.devcine.backend.dto.response.BatchShowtimeResult.SkippedSlot> skipped = new ArrayList<>();
        List<com.devcine.backend.dto.response.BatchShowtimeResult.SkippedSlot> warnings = new ArrayList<>();

        for (LocalDate date = req.getDateFrom(); !date.isAfter(req.getDateTo()); date = date.plusDays(1)) {
            if (daysFilter != null && !daysFilter.contains(date.getDayOfWeek().getValue()))
                continue;

            for (java.time.LocalTime time : times) {
                LocalDateTime start = date.atTime(time);

                for (Integer roomId : req.getRoomIds()) {
                    Room room = roomMap.get(roomId);
                    // KIỂM TRA TƯƠNG THÍCH ĐỊNH DẠNG & PHÒNG CHIẾU
                    if (!isFormatCompatibleWithRoom(format, room)) {
                        skipped.add(skip(roomId, room.getName(), start,
                                "Phòng (" + (room.getType() != null ? room.getType() : "STANDARD") + ") không hỗ trợ định dạng " + format.getName()));
                        continue;
                    }

                    // endTime tính theo turnaround của CHÍNH phòng (mỗi phòng có thể khác nhau).
                    LocalDateTime end = start.plusMinutes(duration + turnaroundOf(room));
                    if (start.isBefore(now)) {
                        skipped.add(skip(roomId, room.getName(), start, "Đã qua giờ chiếu"));
                        continue;
                    }

                    // RULE A — Chặn nếu suất bắt đầu trước giờ mở cửa hoặc sau giờ suất cuối
                    int[] win = windowByRoom.get(roomId);
                    int startPos = posOf(time, win[0]);
                    int endPos = startPos + duration + turnaroundOf(room);
                    if (startPos < win[0] || startPos > win[1]) {
                        skipped.add(skip(roomId, room.getName(), start,
                                "Bắt đầu ngoài khung giờ (" + fmtMin(win[0]) + "–" + fmtMin(win[1]) + " - giờ suất cuối)"));
                        continue;
                    }

                    // RULE B — Chặn nếu suất chiếu kéo dài quá mốc trần ca đêm (03:30 AM)
                    if (endPos > MAX_OVERNIGHT_END_MINUTES) {
                        skipped.add(skip(roomId, room.getName(), start,
                                "Kết thúc quá muộn (" + fmtMin(endPos) + "), vượt giới hạn ca đêm 03:30"));
                        continue;
                    }

                    List<LocalDateTime[]> busy = busyByRoom.computeIfAbsent(roomId, k -> new ArrayList<>());
                    boolean overlap = busy.stream().anyMatch(iv -> start.isBefore(iv[1]) && end.isAfter(iv[0]));
                    if (overlap) {
                        skipped.add(skip(roomId, room.getName(), start, "Trùng lịch phòng (gồm giờ dọn dẹp)"));
                        continue;
                    }
                    // Giữ chỗ để các suất sau trong lô không đè
                    busy.add(new LocalDateTime[] { start, end });

                    // Tính status: "Xuất chiếu sớm" nếu suất nằm trước ngày khởi chiếu chính thức.
                    boolean earlyBatch = movie.getReleaseDate() != null
                            && date.isBefore(movie.getReleaseDate());
                    toSave.add(Showtime.builder()
                            .movie(movie).room(room).format(format)
                            .startTime(start).endTime(end)
                            .status(earlyBatch ? "Xuất chiếu sớm" : "Sắp chiếu")
                            .layoutData(snapshotByRoom.computeIfAbsent(roomId,
                                    rid -> seatLayoutSnapshotService.buildSnapshotJson(rid)))
                            .build());
                }
            }
        }

        int toCreate = toSave.size();
        int created = 0;
        if (!req.isDryRun() && !toSave.isEmpty()) {
            showtimeRepository.saveAll(toSave);
            created = toSave.size();
        }

        return com.devcine.backend.dto.response.BatchShowtimeResult.builder()
                .toCreate(toCreate)
                .createdCount(created)
                .skipped(skipped)
                .warnings(warnings)
                .requiresConfirmation(false)
                .build();
    }

    private com.devcine.backend.dto.response.BatchShowtimeResult.SkippedSlot skip(
            Integer roomId, String roomName, LocalDateTime start, String reason) {
        return com.devcine.backend.dto.response.BatchShowtimeResult.SkippedSlot.builder()
                .roomId(roomId).roomName(roomName)
                .startTime(start.toString()).reason(reason)
                .build();
    }

    /**
     * Backfill snapshot sơ đồ cho các suất cũ (layout_data = null) — chạy 1 lần sau
     * khi bật tính năng.
     * Mỗi phòng chỉ dựng snapshot 1 lần rồi tái dùng cho mọi suất cùng phòng (tránh
     * N+1). Trả số suất đã vá.
     */
    @CacheEvict(value = { "cinemas_showtimes", "showtimes_cinema", "upcomingShowtimes" }, allEntries = true)
    @org.springframework.transaction.annotation.Transactional
    public int backfillLayoutSnapshots() {
        List<Showtime> legacy = showtimeRepository.findWithoutLayoutSnapshot();
        if (legacy.isEmpty())
            return 0;
        Map<Integer, String> snapshotByRoom = new HashMap<>();
        for (Showtime s : legacy) {
            Integer roomId = s.getRoom().getId();
            s.setLayoutData(snapshotByRoom.computeIfAbsent(roomId,
                    rid -> seatLayoutSnapshotService.buildSnapshotJson(rid)));
        }
        showtimeRepository.saveAll(legacy);
        return legacy.size();
    }

    @CacheEvict(value = { "cinemas_showtimes", "showtimes_cinema", "upcomingShowtimes" }, allEntries = true)
    @org.springframework.transaction.annotation.Transactional
    public void updateShowtime(Integer id, java.util.Map<String, Object> updates) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));

        Integer originalRoomId = showtime.getRoom().getId();
        // Phòng đích: phòng mới (nếu đổi) hoặc phòng hiện tại.
        Room targetRoom = showtime.getRoom();
        if (updates.containsKey("roomId") && updates.get("roomId") != null) {
            Integer roomId = ((Number) updates.get("roomId")).intValue();
            targetRoom = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu."));
        }

        // Giờ bắt đầu đích: giờ mới (nếu đổi) hoặc giữ nguyên.
        LocalDateTime targetStart = showtime.getStartTime();
        if (updates.containsKey("startTime") && updates.get("startTime") != null) {
            targetStart = LocalDateTime.parse((String) updates.get("startTime"));
        }

        // endTime luôn tính lại từ thời lượng phim + turnaround của PHÒNG ĐÍCH (nguồn
        // duy nhất).
        int duration = showtime.getMovie().getDurationMins() != null ? showtime.getMovie().getDurationMins() : 120;
        LocalDateTime targetEnd = targetStart.plusMinutes(duration + turnaroundOf(targetRoom));

        // VÁ LỖ HỔNG: chặn đổi giờ/phòng gây chồng lấn (bỏ qua chính suất đang sửa).
        if (showtimeRepository.hasConflictExcluding(targetRoom.getId(), targetStart, targetEnd, id)) {
            throw new IllegalStateException(
                    "Phòng chiếu đã có lịch trong khung giờ này (gồm thời gian dọn dẹp). Vui lòng chọn giờ/phòng khác.");
        }

        int[] win = cinemaWindow(targetRoom.getCinema());
        int startPos = posOf(targetStart.toLocalTime(), win[0]);
        int endPos = startPos + duration + turnaroundOf(targetRoom);
        if (startPos < win[0] || startPos > win[1]) {
            throw new IllegalArgumentException(
                    "Suất chiếu phải bắt đầu trong khung giờ từ " + fmtMin(win[0]) + " đến " + fmtMin(win[1]) + " (giờ suất cuối). Vui lòng chọn giờ khác.");
        }
        if (endPos > MAX_OVERNIGHT_END_MINUTES) {
            throw new IllegalArgumentException("Suất chiếu kết thúc lúc " + fmtMin(endPos) + ", vượt quá giới hạn ca đêm (03:30). Vui lòng chọn giờ bắt đầu sớm hơn.");
        }

        showtime.setRoom(targetRoom);
        showtime.setStartTime(targetStart);
        showtime.setEndTime(targetEnd);
        // Đổi sang phòng khác → chụp lại sơ đồ của phòng mới (giữ đúng nguyên tắc
        // snapshot).
        // Sửa phòng gốc thì KHÔNG re-snapshot: suất giữ nguyên sơ đồ đã đông cứng.
        if (!targetRoom.getId().equals(originalRoomId) || showtime.getLayoutData() == null) {
            showtime.setLayoutData(seatLayoutSnapshotService.buildSnapshotJson(targetRoom.getId()));
        }
        showtimeRepository.save(showtime);
    }

    /**
     * Xoá một suất chiếu. Guard: nếu suất đã có vé BÁN/GIỮ (BookingSeat SOLD/HOLD)
     * thì TỪ CHỐI —
     * phải hoàn tiền/huỷ vé trước, tránh xoá suất làm mồ côi đơn hàng.
     */
    @CacheEvict(value = { "cinemas_showtimes", "showtimes_cinema", "upcomingShowtimes" }, allEntries = true)
    @org.springframework.transaction.annotation.Transactional
    public void deleteShowtime(Integer id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));
        long reserved = bookingSeatRepository.countReservedByShowtime(id);
        if (reserved > 0) {
            throw new IllegalStateException("Suất chiếu đã có " + reserved
                    + " vé được bán/giữ chỗ. Vui lòng huỷ/hoàn tiền các vé này trước khi xoá suất chiếu.");
        }
        showtimeRepository.delete(showtime);
    }

    /**
     * Chi tiết một suất chiếu kèm số liệu vé/doanh thu THỰC TẾ (cho drawer quản
     * trị).
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.devcine.backend.dto.response.ShowtimeDetailResponse getShowtimeDetail(Integer id) {
        Showtime s = showtimeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));

        Movie m = s.getMovie();
        Room room = s.getRoom();

        int totalSeats = 0;
        for (Object[] row : seatRepository.countSellableSeatsByRoomIds(java.util.List.of(room.getId()))) {
            totalSeats = ((Number) row[1]).intValue();
        }
        long sold = bookingSeatRepository.countSoldByShowtime(id);
        long held = bookingSeatRepository.countHeldByShowtime(id);
        java.math.BigDecimal revenue = bookingSeatRepository.sumSoldRevenueByShowtime(id);

        return com.devcine.backend.dto.response.ShowtimeDetailResponse.builder()
                .id(s.getId())
                .status(s.getStatus())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .movieId(m.getId())
                .movieTitle(m.getTitle())
                .movieTitleVietnamese(m.getTitleVietnamese())
                .posterUrl(m.getPosterUrl())
                .ageRating(m.getAgeRating())
                .durationMins(m.getDurationMins())
                .director(m.getDirector())
                .castMembers(m.getCastMembers())
                .description(m.getDescription())
                .versionType(m.getVersionType())
                .genres(m.getGenres() != null
                        ? m.getGenres().stream().map(g -> g.getName()).collect(Collectors.toSet())
                        : new HashSet<>())
                .formatId(s.getFormat().getId())
                .formatName(s.getFormat().getName())
                .roomId(room.getId())
                .roomName(room.getName())
                .cinemaName(room.getCinema().getName())
                .totalSeats(totalSeats)
                .soldSeats(sold)
                .heldSeats(held)
                .availableSeats(Math.max(0, totalSeats - sold - held))
                .revenue(revenue != null ? revenue : java.math.BigDecimal.ZERO)
                .build();
    }

    /**
     * Thời gian dọn dẹp (phút) của phòng — nguồn duy nhất; mặc định 15 nếu chưa cấu
     * hình.
     */
    private int turnaroundOf(Room room) {
        return room.getTurnaroundTimeMins() != null ? room.getTurnaroundTimeMins() : 15;
    }

    /**
     * Cửa sổ giờ hoạt động của cụm rạp theo phút [openMin, closeMin].
     * Nếu giờ đóng ≤ giờ mở ⇒ đóng cửa RẠNG SÁNG hôm sau → closeMin += 1440 (suất
     * khuya vắt qua nửa đêm).
     */
    private int[] cinemaWindow(Cinema cinema) {
        java.time.LocalTime open = cinema.getOpeningTime() != null ? cinema.getOpeningTime()
                : java.time.LocalTime.of(8, 0);
        java.time.LocalTime close = cinema.getClosingTime() != null ? cinema.getClosingTime()
                : java.time.LocalTime.of(23, 30);
        int openMin = open.getHour() * 60 + open.getMinute();
        int closeMin = close.getHour() * 60 + close.getMinute();
        if (closeMin <= openMin)
            closeMin += 1440;
        return new int[] { openMin, closeMin };
    }

    /**
     * Vị trí (phút) của một mốc giờ trên trục ngày vận hành: giờ < giờ mở ⇒ +1440
     * (thuộc phần khuya).
     */
    private int posOf(java.time.LocalTime t, int openMin) {
        int m = t.getHour() * 60 + t.getMinute();
        if (m < openMin)
            m += 1440;
        return m;
    }

    /**
     * Định dạng phút-trong-ngày-vận-hành thành "HH:mm" (chia dư 1440 để hiển thị
     * giờ đồng hồ).
     */
    private String fmtMin(int min) {
        int m = ((min % 1440) + 1440) % 1440;
        return String.format("%02d:%02d", m / 60, m % 60);
    }

    /**
     * Validate xem giờ mở/đóng mới có làm "tàng hình" suất chiếu chưa kết thúc nào
     * không
     */
    public void validateCinemaHoursUpdate(Cinema cinema, java.time.LocalTime newOpen, java.time.LocalTime newClose) {
        if (newOpen == null || newClose == null)
            return;

        int openMin = newOpen.getHour() * 60 + newOpen.getMinute();
        int closeMin = newClose.getHour() * 60 + newClose.getMinute();
        if (closeMin <= openMin)
            closeMin += 1440;

        List<Showtime> futureShows = showtimeRepository.findFutureShowtimesByCinema(cinema.getId(),
                LocalDateTime.now());
        int violations = 0;
        for (Showtime s : futureShows) {
            int sStart = s.getStartTime().getHour() * 60 + s.getStartTime().getMinute();
            int sEnd = s.getEndTime().getHour() * 60 + s.getEndTime().getMinute();

            if (sStart < openMin) {
                sStart += 1440;
            }
            if (sEnd < openMin || sEnd < sStart) {
                sEnd += 1440;
            }

            if (sStart < openMin || sStart > closeMin || sEnd > MAX_OVERNIGHT_END_MINUTES) {
                System.out.println("VIOLATION - Showtime ID: " + s.getId() +
                        " | Start: " + s.getStartTime() +
                        " | End: " + s.getEndTime() +
                        " | sStart: " + sStart +
                        " | sEnd: " + sEnd +
                        " | openMin: " + openMin +
                        " | closeMin: " + closeMin);
                violations++;
            }
        }

        if (violations > 0) {
            throw new IllegalArgumentException("Không thể đổi giờ hoạt động! Đang có " + violations
                    + " suất chiếu chưa kết thúc nằm ngoài khung giờ mới. Vui lòng hủy hoặc dời lịch các suất chiếu này trước!");
        }
    }
}
