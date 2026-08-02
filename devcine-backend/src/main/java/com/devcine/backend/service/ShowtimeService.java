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

        // Tính tình trạng ghế 1 LẦN cho tất cả suất (tránh N+1):
        //  - sellable/phòng = ghế active & không bảo trì/khóa
        //  - reserved/suất  = ghế SOLD/HOLD
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

    @org.springframework.transaction.annotation.Transactional
    public com.devcine.backend.dto.response.ShowtimeCreateResult createShowtime(ShowtimeRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu với ID: " + request.getRoomId()));
        MovieFormat format = movieFormatRepository.findById(request.getFormatId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy định dạng với ID: " + request.getFormatId()));

        LocalDateTime startTime = request.getStartTime();
        // NGUỒN DUY NHẤT: thời gian dọn dẹp bốc từ chính phòng (Room.turnaroundTimeMins), không nhận từ FE.
        int turnaround = turnaroundOf(room);
        int duration = movie.getDurationMins() != null ? movie.getDurationMins() : 120;
        LocalDateTime endTime = startTime.plusMinutes(duration + turnaround);

        // ===== Constraint Engine: kiểm soát theo giờ hoạt động của cụm rạp =====
        Cinema cinema = room.getCinema();
        int[] win = cinemaWindow(cinema);       // [openMin, closeMin] (closeMin đã +1440 nếu qua nửa đêm)
        int startPos = posOf(startTime.toLocalTime(), win[0]);
        int endPos = startPos + duration + turnaround;
        // RULE A — chặn cứng: suất bắt đầu ngoài giờ hoạt động.
        if (startPos < win[0] || startPos >= win[1]) {
            throw new IllegalArgumentException("Suất chiếu bắt đầu ngoài giờ hoạt động của rạp ("
                    + fmtMin(win[0]) + "–" + fmtMin(win[1]) + "). Vui lòng chọn giờ khác.");
        }

        boolean hasConflict = showtimeRepository.hasConflict(room.getId(), startTime, endTime);
        if (hasConflict) {
            throw new IllegalStateException("Phòng chiếu đã có lịch trong khung giờ này (Bao gồm thời gian dọn dẹp). Vui lòng chọn giờ khác.");
        }

        // RULE B — chặn cứng: suất kết thúc quá giờ đóng cửa.
        if (endPos > win[1]) {
            throw new IllegalArgumentException("Suất chiếu kết thúc lúc " + fmtMin(endPos) + ", vượt quá giờ đóng cửa ("
                            + fmtMin(win[1]) + "). Vui lòng chọn giờ khác.");
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

        return com.devcine.backend.dto.response.ShowtimeCreateResult.builder()
                .requiresConfirmation(false)
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
                        .build())
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

        int duration = movie.getDurationMins() != null ? movie.getDurationMins() : 120;

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

        // Giờ hoạt động theo TỪNG phòng (mỗi phòng có thể thuộc cụm rạp khác nhau) — tính 1 lần.
        Map<Integer, int[]> windowByRoom = new HashMap<>();
        roomMap.forEach((rid, r) -> windowByRoom.put(rid, cinemaWindow(r.getCinema())));

        LocalDateTime now = LocalDateTime.now();
        List<Showtime> toSave = new ArrayList<>();
        List<com.devcine.backend.dto.response.BatchShowtimeResult.SkippedSlot> skipped = new ArrayList<>();
        // Suất hợp lệ nhưng KẾT THÚC quá giờ đóng cửa — chỉ tạo khi force.
        List<com.devcine.backend.dto.response.BatchShowtimeResult.SkippedSlot> warnings = new ArrayList<>();

        for (LocalDate date = req.getDateFrom(); !date.isAfter(req.getDateTo()); date = date.plusDays(1)) {
            if (daysFilter != null && !daysFilter.contains(date.getDayOfWeek().getValue())) continue;

            for (java.time.LocalTime time : times) {
                LocalDateTime start = date.atTime(time);

                for (Integer roomId : req.getRoomIds()) {
                    Room room = roomMap.get(roomId);
                    // endTime tính theo turnaround của CHÍNH phòng (mỗi phòng có thể khác nhau).
                    LocalDateTime end = start.plusMinutes(duration + turnaroundOf(room));
                    if (start.isBefore(now)) {
                        skipped.add(skip(roomId, room.getName(), start, "Đã qua giờ chiếu"));
                        continue;
                    }
                    // RULE A — chặn cứng: suất bắt đầu ngoài giờ hoạt động của cụm rạp.
                    int[] win = windowByRoom.get(roomId);
                    int startPos = posOf(time, win[0]);
                    int endPos = startPos + duration + turnaroundOf(room);
                    if (startPos < win[0] || startPos >= win[1]) {
                        skipped.add(skip(roomId, room.getName(), start,
                                "Ngoài giờ hoạt động (" + fmtMin(win[0]) + "–" + fmtMin(win[1]) + ")"));
                        continue;
                    }
                    List<LocalDateTime[]> busy = busyByRoom.computeIfAbsent(roomId, k -> new ArrayList<>());
                    boolean overlap = busy.stream().anyMatch(iv -> start.isBefore(iv[1]) && end.isAfter(iv[0]));
                    if (overlap) {
                        skipped.add(skip(roomId, room.getName(), start, "Trùng lịch phòng (gồm giờ dọn dẹp)"));
                        continue;
                    }
                    // Giữ chỗ để các suất sau trong lô không đè (kể cả suất khuya cảnh báo).
                    busy.add(new LocalDateTime[]{ start, end });
                    boolean afterClosing = endPos > win[1];
                    if (afterClosing) {
                        warnings.add(skip(roomId, room.getName(), start,
                                "Kết thúc " + fmtMin(endPos) + " quá giờ đóng cửa (" + fmtMin(win[1]) + ")"));
                        // Chỉ đưa vào danh sách ghi khi admin đã xác nhận (force).
                        if (!req.isForce()) continue;
                    }
                    toSave.add(Showtime.builder()
                            .movie(movie).room(room).format(format)
                            .startTime(start).endTime(end)
                            .status("Sắp chiếu")
                            .build());
                }
            }
        }

        // All-or-nothing: còn suất khuya chưa xác nhận ⇒ chưa ghi, yêu cầu FE xác nhận rồi gửi lại force.
        boolean requiresConfirmation = !warnings.isEmpty() && !req.isForce();
        int toCreate = toSave.size() + (req.isForce() ? 0 : warnings.size());

        int created = 0;
        // Chỉ ghi khi KHÔNG dryRun VÀ không còn suất khuya chờ xác nhận (all-or-nothing).
        if (!req.isDryRun() && !requiresConfirmation && !toSave.isEmpty()) {
            showtimeRepository.saveAll(toSave);
            created = toSave.size();
        }

        return com.devcine.backend.dto.response.BatchShowtimeResult.builder()
                .toCreate(toCreate)
                .createdCount(created)
                .skipped(skipped)
                .warnings(warnings)
                .requiresConfirmation(requiresConfirmation)
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));

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

        // endTime luôn tính lại từ thời lượng phim + turnaround của PHÒNG ĐÍCH (nguồn duy nhất).
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
        if (startPos < win[0] || startPos >= win[1]) {
            throw new IllegalArgumentException("Suất chiếu bắt đầu ngoài giờ hoạt động của rạp. Vui lòng chọn giờ khác.");
        }
        if (endPos > win[1]) {
            throw new IllegalArgumentException("Suất chiếu kết thúc lúc " + fmtMin(endPos) + ", vượt quá giờ đóng cửa ("
                            + fmtMin(win[1]) + "). Vui lòng chọn giờ khác.");
        }

        showtime.setRoom(targetRoom);
        showtime.setStartTime(targetStart);
        showtime.setEndTime(targetEnd);
        showtimeRepository.save(showtime);
    }

    /**
     * Xoá một suất chiếu. Guard: nếu suất đã có vé BÁN/GIỮ (BookingSeat SOLD/HOLD) thì TỪ CHỐI —
     * phải hoàn tiền/huỷ vé trước, tránh xoá suất làm mồ côi đơn hàng.
     */
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

    /** Chi tiết một suất chiếu kèm số liệu vé/doanh thu THỰC TẾ (cho drawer quản trị). */
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

    /** Thời gian dọn dẹp (phút) của phòng — nguồn duy nhất; mặc định 15 nếu chưa cấu hình. */
    private int turnaroundOf(Room room) {
        return room.getTurnaroundTimeMins() != null ? room.getTurnaroundTimeMins() : 15;
    }

    /**
     * Cửa sổ giờ hoạt động của cụm rạp theo phút [openMin, closeMin].
     * Nếu giờ đóng ≤ giờ mở ⇒ đóng cửa RẠNG SÁNG hôm sau → closeMin += 1440 (suất khuya vắt qua nửa đêm).
     */
    private int[] cinemaWindow(Cinema cinema) {
        java.time.LocalTime open = cinema.getOpeningTime() != null ? cinema.getOpeningTime() : java.time.LocalTime.of(8, 0);
        java.time.LocalTime close = cinema.getClosingTime() != null ? cinema.getClosingTime() : java.time.LocalTime.of(23, 30);
        int openMin = open.getHour() * 60 + open.getMinute();
        int closeMin = close.getHour() * 60 + close.getMinute();
        if (closeMin <= openMin) closeMin += 1440;
        return new int[]{ openMin, closeMin };
    }

    /** Vị trí (phút) của một mốc giờ trên trục ngày vận hành: giờ < giờ mở ⇒ +1440 (thuộc phần khuya). */
    private int posOf(java.time.LocalTime t, int openMin) {
        int m = t.getHour() * 60 + t.getMinute();
        if (m < openMin) m += 1440;
        return m;
    }

    /** Định dạng phút-trong-ngày-vận-hành thành "HH:mm" (chia dư 1440 để hiển thị giờ đồng hồ). */
    private String fmtMin(int min) {
        int m = ((min % 1440) + 1440) % 1440;
        return String.format("%02d:%02d", m / 60, m % 60);
    }

    /** Validate xem giờ mở/đóng mới có làm "tàng hình" suất chiếu chưa kết thúc nào không */
    public void validateCinemaHoursUpdate(Cinema cinema, java.time.LocalTime newOpen, java.time.LocalTime newClose) {
        if (newOpen == null || newClose == null) return;
        
        int openMin = newOpen.getHour() * 60 + newOpen.getMinute();
        int closeMin = newClose.getHour() * 60 + newClose.getMinute();
        if (closeMin <= openMin) closeMin += 1440;
        
        List<Showtime> futureShows = showtimeRepository.findFutureShowtimesByCinema(cinema.getId(), LocalDateTime.now());
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

            if (sStart < openMin || sEnd > closeMin) {
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
            throw new IllegalArgumentException("Không thể đổi giờ hoạt động! Đang có " + violations + " suất chiếu chưa kết thúc nằm ngoài khung giờ mới. Vui lòng hủy hoặc dời lịch các suất chiếu này trước!");
        }
    }
}
