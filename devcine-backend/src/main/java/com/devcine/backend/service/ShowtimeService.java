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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public List<com.devcine.backend.dto.response.PublicShowtimeDTO> getAllUpcomingShowtimes() {
        LocalDateTime now = LocalDateTime.now();
        List<Showtime> showtimes = showtimeRepository.findUpcomingShowtimes(now);
        return showtimes.stream().map(s -> com.devcine.backend.dto.response.PublicShowtimeDTO.builder()
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
                    s.getMovie().getGenres().stream().map(g -> g.getName()).collect(Collectors.toSet()) : new java.util.HashSet<>())
                .formatId(s.getFormat().getId())
                .formatName(s.getFormat().getName())
                .roomId(s.getRoom().getId())
                .roomName(s.getRoom().getName())
                .build()).collect(Collectors.toList());
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
