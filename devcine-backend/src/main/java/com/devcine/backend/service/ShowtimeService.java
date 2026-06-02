package com.devcine.backend.service;

import com.devcine.backend.dto.CinemaShowtimeDTO;
import com.devcine.backend.dto.ShowtimeDTO;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.repository.CinemaRepository;
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

    public List<String> getAllCities() {
        return cinemaRepository.findAllCities();
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
}
