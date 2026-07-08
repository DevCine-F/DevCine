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
import com.devcine.backend.dto.response.MovieCardDTO;
import com.devcine.backend.dto.response.PublicShowtimeDTO;
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

    public List<String> getAllCities() {
        return cinemaRepository.findAllCities();
    }

    public List<PublicShowtimeDTO> getAllUpcomingShowtimes() {
        LocalDateTime now = LocalDateTime.now();
        return showtimeRepository.findUpcomingShowtimes(now).stream()
                .map(this::toPublicDTO).collect(Collectors.toList());
    }

    /** Mapper dùng chung: Showtime -> PublicShowtimeDTO (DTO phẳng cho FE tự nhóm). */
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
                .movieGenres(s.getMovie().getGenres() != null ?
                    s.getMovie().getGenres().stream().map(g -> g.getName()).collect(Collectors.toSet()) : new HashSet<>())
                .formatId(s.getFormat().getId())
                .formatName(s.getFormat().getName())
                .roomId(s.getRoom().getId())
                .roomName(s.getRoom().getName())
                .build();
    }

    // ===== Trang Lịch chiếu: lọc phía server + phân trang =====

    /** Khoảng thời gian của một ngày; với hôm nay thì bắt đầu từ "bây giờ" để ẩn suất đã qua. */
    private LocalDateTime[] dayRange(String date) {
        LocalDate d = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = d.atStartOfDay();
        if (start.isBefore(now)) start = now; // hôm nay: ẩn suất đã chiếu
        return new LocalDateTime[]{ start, d.atTime(23, 59, 59) };
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
                .genres(m.getGenres() != null ? m.getGenres().stream().map(g -> g.getName()).collect(Collectors.toSet()) : new HashSet<>())
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
        LocalDateTime now = LocalDateTime.now();
        List<Showtime> showtimes;
        
        if (city != null && !city.trim().isEmpty()) {
            showtimes = showtimeRepository.findUpcomingShowtimesByMovieIdAndCity(movieId, city, now);
        } else {
            showtimes = showtimeRepository.findUpcomingShowtimesByMovieId(movieId, now);
        }

        // Group by Cinema
        Map<Cinema, List<Showtime>> byCinema = showtimes.stream()
                .collect(Collectors.groupingBy(s -> s.getRoom().getCinema()));

        List<CinemaShowtimeDTO> result = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Map.Entry<Cinema, List<Showtime>> entry : byCinema.entrySet()) {
            Cinema cinema = entry.getKey();
            List<Showtime> cinemaShowtimes = entry.getValue();

            // Group by Date
            Map<String, List<ShowtimeDTO>> showtimesByDate = new TreeMap<>();
            
            for (Showtime s : cinemaShowtimes) {
                String dateStr = s.getStartTime().format(dateFormatter);
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
                .build()).collect(Collectors.toList());
    }

    public ShowtimeDTO createShowtime(ShowtimeRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu với ID: " + request.getRoomId()));
        MovieFormat format = movieFormatRepository.findById(request.getFormatId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy định dạng với ID: " + request.getFormatId()));

        LocalDateTime startTime = request.getStartTime();
        int cleaningTime = request.getCleaningTime() != null ? request.getCleaningTime() : 15;
        // Assume duration is in minutes
        int duration = movie.getDurationMins() != null ? movie.getDurationMins() : 120; 
        LocalDateTime endTime = startTime.plusMinutes(duration + cleaningTime);

        boolean hasConflict = showtimeRepository.hasConflict(room.getId(), startTime, endTime);
        if (hasConflict) {
            throw new IllegalStateException("Phòng chiếu đã có lịch trong khung giờ này (Bao gồm thời gian dọn dẹp). Vui lòng chọn giờ khác.");
        }

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .room(room)
                .format(format)
                .startTime(startTime)
                .endTime(endTime)
                .status("Sắp chiếu")
                .build();

        Showtime saved = showtimeRepository.save(showtime);

        return ShowtimeDTO.builder()
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
                .build();
    }

    /**
     * Tạo lịch chiếu HÀNG LOẠT cho một phim: sinh tích Descartes (phòng × ngày × khung giờ),
     * bỏ qua suất trùng lịch (với DB lẫn giữa các suất trong lô) và suất đã qua giờ.
     * Chống N+1: nạp một lần toàn bộ suất hiện có trong cửa sổ rồi kiểm tra trong bộ nhớ.
     */
    @org.springframework.transaction.annotation.Transactional
    public com.devcine.backend.dto.response.BatchShowtimeResult createBatchShowtimes(
            com.devcine.backend.dto.request.BatchShowtimeRequest req) {

        Movie movie = movieRepository.findById(req.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + req.getMovieId()));
        MovieFormat format = movieFormatRepository.findById(req.getFormatId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy định dạng với ID: " + req.getFormatId()));

        if (req.getDateFrom().isAfter(req.getDateTo())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }

        int cleaningTime = req.getCleaningTime() != null ? req.getCleaningTime() : 15;
        int duration = movie.getDurationMins() != null ? movie.getDurationMins() : 120;
        int blockMins = duration + cleaningTime;

        // Nạp phòng theo id, giữ thứ tự để đặt tên trong báo cáo
        Map<Integer, Room> roomMap = new LinkedHashMap<>();
        roomRepository.findAllById(req.getRoomIds()).forEach(r -> roomMap.put(r.getId(), r));
        List<Integer> missing = req.getRoomIds().stream().filter(id -> !roomMap.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy phòng chiếu với ID: " + missing);
        }

        // Parse các mốc giờ "HH:mm"
        List<java.time.LocalTime> times = new ArrayList<>();
        for (String t : req.getStartTimes()) {
            try {
                times.add(java.time.LocalTime.parse(t.trim()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Khung giờ không hợp lệ: '" + t + "' (định dạng HH:mm).");
            }
        }

        Set<Integer> daysFilter = (req.getDaysOfWeek() != null && !req.getDaysOfWeek().isEmpty())
                ? new HashSet<>(req.getDaysOfWeek()) : null;

        // Cửa sổ thời gian bao trùm cả lô để nạp suất hiện có 1 lần.
        // Nới đuôi thêm 1 ngày để bắt cả suất hiện có rơi sang rạng sáng hôm sau (qua nửa đêm).
        LocalDateTime windowStart = req.getDateFrom().atStartOfDay();
        LocalDateTime windowEnd = req.getDateTo().plusDays(1).atTime(23, 59, 59);

        // roomId -> danh sách khoảng [start, end) đã chiếm (DB + các suất đã nhận trong lô)
        Map<Integer, List<LocalDateTime[]>> busyByRoom = new HashMap<>();
        for (Showtime s : showtimeRepository.findByRoomsAndWindow(req.getRoomIds(), windowStart, windowEnd)) {
            busyByRoom.computeIfAbsent(s.getRoom().getId(), k -> new ArrayList<>())
                    .add(new LocalDateTime[]{ s.getStartTime(), s.getEndTime() });
        }

        LocalDateTime now = LocalDateTime.now();
        List<Showtime> toSave = new ArrayList<>();
        List<com.devcine.backend.dto.response.BatchShowtimeResult.SkippedSlot> skipped = new ArrayList<>();

        for (LocalDate date = req.getDateFrom(); !date.isAfter(req.getDateTo()); date = date.plusDays(1)) {
            if (daysFilter != null && !daysFilter.contains(date.getDayOfWeek().getValue())) continue;

            for (java.time.LocalTime time : times) {
                LocalDateTime start = date.atTime(time);
                LocalDateTime end = start.plusMinutes(blockMins);

                for (Integer roomId : req.getRoomIds()) {
                    Room room = roomMap.get(roomId);
                    if (start.isBefore(now)) {
                        skipped.add(skip(roomId, room.getName(), start, "Đã qua giờ chiếu"));
                        continue;
                    }
                    List<LocalDateTime[]> busy = busyByRoom.computeIfAbsent(roomId, k -> new ArrayList<>());
                    boolean overlap = busy.stream().anyMatch(iv -> start.isBefore(iv[1]) && end.isAfter(iv[0]));
                    if (overlap) {
                        skipped.add(skip(roomId, room.getName(), start, "Trùng lịch phòng (gồm giờ dọn dẹp)"));
                        continue;
                    }
                    // Nhận suất: giữ chỗ để các suất sau trong lô không đè
                    busy.add(new LocalDateTime[]{ start, end });
                    toSave.add(Showtime.builder()
                            .movie(movie).room(room).format(format)
                            .startTime(start).endTime(end)
                            .status("Sắp chiếu")
                            .build());
                }
            }
        }

        int created = 0;
        if (!req.isDryRun() && !toSave.isEmpty()) {
            showtimeRepository.saveAll(toSave);
            created = toSave.size();
        }

        return com.devcine.backend.dto.response.BatchShowtimeResult.builder()
                .toCreate(toSave.size())
                .createdCount(created)
                .skipped(skipped)
                .build();
    }

    private com.devcine.backend.dto.response.BatchShowtimeResult.SkippedSlot skip(
            Integer roomId, String roomName, LocalDateTime start, String reason) {
        return com.devcine.backend.dto.response.BatchShowtimeResult.SkippedSlot.builder()
                .roomId(roomId).roomName(roomName)
                .startTime(start.toString()).reason(reason)
                .build();
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateShowtime(Integer id, java.util.Map<String, Object> updates) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Showtime not found"));

        if (updates.containsKey("roomId")) {
            Integer roomId = (Integer) updates.get("roomId");
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Room not found"));
            showtime.setRoom(room);
        }

        if (updates.containsKey("startTime")) {
            LocalDateTime startTime = LocalDateTime.parse((String) updates.get("startTime"));
            int duration = showtime.getMovie().getDurationMins();
            int cleaningTime = updates.containsKey("cleaningTime") ? (Integer) updates.get("cleaningTime") : 15;
            LocalDateTime endTime = startTime.plusMinutes(duration).plusMinutes(cleaningTime);

            showtime.setStartTime(startTime);
            showtime.setEndTime(endTime);
        }

        showtimeRepository.save(showtime);
    }
}
