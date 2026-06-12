package com.devcine.backend.config;

import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.entity.MovieFormat;
import com.devcine.backend.entity.Room;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.MovieRepository;
import com.devcine.backend.repository.MovieFormatRepository;
import com.devcine.backend.repository.RoomRepository;
import com.devcine.backend.repository.SeatTypeRepository;
import com.devcine.backend.entity.SeatType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    @Bean
    public CommandLineRunner initData(
            CinemaRepository cinemaRepository,
            MovieRepository movieRepository,
            MovieFormatRepository formatRepository,
            RoomRepository roomRepository,
            SeatTypeRepository seatTypeRepository) {
        return args -> {
            if (roomRepository.count() == 0) {
                // To avoid duplicating cinemas if they already exist, let's fetch them first or create if missing
                Cinema cinema1 = cinemaRepository.findById(1).orElse(null);
                if (cinema1 == null) {
                    cinema1 = Cinema.builder()
                            .name("DevCine Landmark 81")
                            .address("Tầng B1, Vincom Landmark 81, 720A Điện Biên Phủ, Phường 22, Bình Thạnh")
                            .city("Hồ Chí Minh")
                            .type("Premium/IMAX")
                            .hotline("1900 1234")
                            .rooms(8)
                            .build();
                    cinemaRepository.save(cinema1);
                }

                Cinema cinema2 = cinemaRepository.findById(2).orElse(null);
                if (cinema2 == null) {
                    cinema2 = Cinema.builder()
                            .name("DevCine Bitexco")
                            .address("Tầng 3, Bitexco Financial Tower, 2 Hải Triều, Quận 1")
                            .city("Hồ Chí Minh")
                            .type("Standard/Sweetbox")
                            .hotline("1900 5678")
                            .rooms(5)
                            .build();
                    cinemaRepository.save(cinema2);
                }

                Room room1 = Room.builder().cinema(cinema1).name("Phòng 01 - IMAX").type("IMAX").status("Hoạt động").build();
                Room room2 = Room.builder().cinema(cinema1).name("Phòng 02 - Gold").type("Gold Class").status("Hoạt động").build();
                Room room3 = Room.builder().cinema(cinema2).name("Phòng 01 - Standard").type("Standard").status("Hoạt động").build();
                
                roomRepository.save(room1);
                roomRepository.save(room2);
                roomRepository.save(room3);

                Movie movie1 = Movie.builder().title("Lật Mặt 7: Một Điều Ước").slug("lat-mat-7").durationMins(130).status("active").releaseDate(LocalDate.now().minusDays(10)).build();
                Movie movie2 = Movie.builder().title("Doraemon: Bản Giao Hưởng Địa Cầu").slug("doraemon-ban-giao-huong").durationMins(115).status("active").releaseDate(LocalDate.now()).build();
                
                movieRepository.save(movie1);
                movieRepository.save(movie2);

                MovieFormat format1 = MovieFormat.builder().name("2D Phụ Đề").surcharge(BigDecimal.ZERO).build();
                MovieFormat format2 = MovieFormat.builder().name("3D Lồng Tiếng").surcharge(new BigDecimal("30000")).build();
                MovieFormat format3 = MovieFormat.builder().name("IMAX 2D").surcharge(new BigDecimal("50000")).build();

                formatRepository.save(format1);
                formatRepository.save(format2);
                formatRepository.save(format3);

                System.out.println("Đã thêm dữ liệu giả lập cho Cụm rạp, Phòng chiếu, Phim, và Định dạng thành công!");
            }

            if (seatTypeRepository.count() == 0) {
                seatTypeRepository.save(SeatType.builder().name("NORMAL").priceModifier(new BigDecimal("0")).build());
                seatTypeRepository.save(SeatType.builder().name("VIP").priceModifier(new BigDecimal("20000")).build());
                seatTypeRepository.save(SeatType.builder().name("SWEETBOX").priceModifier(new BigDecimal("50000")).build());
                System.out.println("Đã thêm dữ liệu giả lập cho SeatType thành công!");
            }
            
            // Temporary fix for previously seeded movies with wrong status
            List<Movie> allMovies = movieRepository.findAll();
            boolean updated = false;
            for (Movie m : allMovies) {
                if ("Đang chiếu".equals(m.getStatus())) {
                    m.setStatus("active");
                    movieRepository.save(m);
                    updated = true;
                }
            }
            if (updated) {
                System.out.println("Đã đồng bộ cập nhật trạng thái phim thành công!");
            }
        };
    }
}
