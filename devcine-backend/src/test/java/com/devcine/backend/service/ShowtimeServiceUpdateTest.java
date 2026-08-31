package com.devcine.backend.service;

import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceUpdateTest {

    @Mock ShowtimeRepository showtimeRepository;
    @Mock CinemaRepository cinemaRepository;
    @Mock MovieRepository movieRepository;
    @Mock RoomRepository roomRepository;
    @Mock MovieFormatRepository movieFormatRepository;
    @Mock SeatRepository seatRepository;
    @Mock BookingSeatRepository bookingSeatRepository;
    @Mock SeatLayoutSnapshotService seatLayoutSnapshotService;
    @Mock SystemSettingService systemSettingService;

    @InjectMocks ShowtimeService showtimeService;

    private Cinema cinema;
    private Room room;
    private Movie movie;
    private MovieFormat format;
    private Showtime showtime;

    @BeforeEach
    void setUp() {
        cinema = Cinema.builder()
                .id(1)
                .name("DevCine Thủ Đức")
                .status("ACTIVE")
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(23, 30))
                .build();

        room = Room.builder()
                .id(10)
                .name("Cinema 01")
                .type("STANDARD")
                .turnaroundTimeMins(15)
                .cinema(cinema)
                .build();

        movie = Movie.builder()
                .id(100)
                .title("Phim Thử Nghiệm")
                .durationMins(120)
                .releaseDate(LocalDate.now().minusDays(5))
                .build();

        format = MovieFormat.builder()
                .id(200)
                .name("2D Phụ đề")
                .build();

        showtime = Showtime.builder()
                .id(500)
                .movie(movie)
                .room(room)
                .format(format)
                .startTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(1).withHour(12).withMinute(15))
                .status("Sắp chiếu")
                .layoutData("{\"rows\": 10, \"cols\": 10}")
                .build();
    }

    @Test
    void rejectsUpdateWhenShowtimeIsInPast() {
        showtime.setStartTime(LocalDateTime.now().minusHours(2));
        when(showtimeRepository.findById(500)).thenReturn(Optional.of(showtime));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                showtimeService.updateShowtime(500, Map.of("roomId", 10)));
        assertTrue(ex.getMessage().contains("Không thể chỉnh sửa suất chiếu đã hoặc đang diễn ra"));
    }

    @Test
    void rejectsRoomChangeWhenTicketsAreSold() {
        when(showtimeRepository.findById(500)).thenReturn(Optional.of(showtime));
        when(bookingSeatRepository.countReservedByShowtime(500)).thenReturn(2L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                showtimeService.updateShowtime(500, Map.of("roomId", 99)));
        assertTrue(ex.getMessage().contains("không thể đổi phòng chiếu"));
    }

    @Test
    void rejectsMovieChangeWhenTicketsAreSold() {
        when(showtimeRepository.findById(500)).thenReturn(Optional.of(showtime));
        when(bookingSeatRepository.countReservedByShowtime(500)).thenReturn(1L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                showtimeService.updateShowtime(500, Map.of("movieId", 999)));
        assertTrue(ex.getMessage().contains("không thể đổi phim"));
    }

    @Test
    void allowsUpdatingMovieAndFormatWhenNoTicketsSold() {
        when(showtimeRepository.findById(500)).thenReturn(Optional.of(showtime));
        when(bookingSeatRepository.countReservedByShowtime(500)).thenReturn(0L);

        Movie newMovie = Movie.builder()
                .id(101)
                .title("Phim Mới")
                .durationMins(90)
                .releaseDate(LocalDate.now().minusDays(2))
                .build();

        MovieFormat newFormat = MovieFormat.builder()
                .id(201)
                .name("3D Lồng tiếng")
                .build();

        when(movieRepository.findById(101)).thenReturn(Optional.of(newMovie));
        when(movieFormatRepository.findById(201)).thenReturn(Optional.of(newFormat));

        LocalDateTime newStart = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        when(showtimeRepository.hasConflictExcluding(eq(10), eq(newStart), any(LocalDateTime.class), eq(500)))
                .thenReturn(false);

        showtimeService.updateShowtime(500, Map.of(
                "movieId", 101,
                "formatId", 201,
                "startTime", newStart.toString()
        ));

        assertEquals(newMovie, showtime.getMovie());
        assertEquals(newFormat, showtime.getFormat());
        assertEquals(newStart, showtime.getStartTime());
        // endTime = 14:00 + 90 mins (movie) + 15 mins (turnaround) = 15:45
        assertEquals(newStart.plusMinutes(105), showtime.getEndTime());
        verify(showtimeRepository).save(showtime);
    }
}
