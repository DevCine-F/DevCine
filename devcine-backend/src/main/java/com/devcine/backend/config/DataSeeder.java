package com.devcine.backend.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.Faq;
import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.entity.MovieFormat;
import com.devcine.backend.entity.Role;
import com.devcine.backend.entity.Room;
import com.devcine.backend.entity.SeatType;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.entity.SystemSetting;
import com.devcine.backend.entity.User;
import com.devcine.backend.entity.Wallet;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.FaqRepository;
import com.devcine.backend.repository.FnbItemRepository;
import com.devcine.backend.repository.MovieFormatRepository;
import com.devcine.backend.repository.MovieRepository;
import com.devcine.backend.repository.RoleRepository;
import com.devcine.backend.repository.RoomRepository;
import com.devcine.backend.repository.SeatTypeRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import com.devcine.backend.repository.SystemSettingRepository;
import com.devcine.backend.repository.UserRepository;
import com.devcine.backend.repository.WalletRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData(
            CinemaRepository cinemaRepository,
            MovieRepository movieRepository,
            MovieFormatRepository formatRepository,
            RoomRepository roomRepository,
            SeatTypeRepository seatTypeRepository,
            SystemSettingRepository systemSettingRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            WalletRepository walletRepository,
            FnbItemRepository fnbItemRepository,
            ShowtimeRepository showtimeRepository,
            FaqRepository faqRepository) {
        return args -> {
            if (systemSettingRepository.findById("LOYALTY_POINT_RATE").isEmpty()) {
                systemSettingRepository.save(SystemSetting.builder()
                        .settingKey("LOYALTY_POINT_RATE")
                        .settingValue("1000")
                        .build());
                System.out.println("Đã cấu hình mặc định LOYALTY_POINT_RATE = 1000 VNĐ = 1 điểm.");
            }

            // Seed roles
            Role adminRole = roleRepository.findByName("ADMIN").orElseGet(()
                    -> roleRepository.save(Role.builder().name("ADMIN").build()));
            Role staffRole = roleRepository.findByName("STAFF").orElseGet(()
                    -> roleRepository.save(Role.builder().name("STAFF").build()));
            Role customerRole = roleRepository.findByName("CUSTOMER").orElseGet(()
                    -> roleRepository.save(Role.builder().name("CUSTOMER").build()));

            // Seed ma trận phân quyền mặc định (chỉ set khi chưa có cấu hình)
            if (adminRole.getPermissionsMatrix() == null || adminRole.getPermissionsMatrix().isBlank()) {
                adminRole.setPermissionsMatrix("{"
                        + "\"dashboard_stats\":[\"view\",\"export\"],"
                        + "\"movies\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"schedules\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"banners\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"promotions\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"pricing\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"cinemas\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"staff_management\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"pos_ticketing\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"pos_inventory\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"support\":[\"view\",\"edit\",\"delete\"],"
                        + "\"settings\":[\"view\",\"edit\"]}");
                roleRepository.save(adminRole);
            }
            if (staffRole.getPermissionsMatrix() == null || staffRole.getPermissionsMatrix().isBlank()) {
                staffRole.setPermissionsMatrix("{"
                        + "\"dashboard_stats\":[\"view\"],"
                        + "\"movies\":[\"view\"],"
                        + "\"schedules\":[\"view\"],"
                        + "\"pos_ticketing\":[\"view\",\"add\",\"edit\"],"
                        + "\"pos_inventory\":[\"view\",\"edit\"],"
                        + "\"support\":[\"view\",\"edit\"]}");
                roleRepository.save(staffRole);
                System.out.println("Đã cấu hình ma trận phân quyền mặc định cho STAFF.");
            }

            // Seed / đảm bảo tài khoản admin (admin / 123) — tạo mới nếu chưa có, reset mật khẩu nếu đã tồn tại
            User adminUser = userRepository.findByUsername("admin").orElse(null);
            if (adminUser == null) {
                adminUser = User.builder()
                        .username("admin")
                        .email("admin@devcine.com")
                        .passwordHash(passwordEncoder.encode("123"))
                        .fullName("Quản trị viên")
                        .role(adminRole)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                userRepository.save(adminUser);
                System.out.println("Đã tạo tài khoản admin mặc định (admin / 123)");
            } else if (!passwordEncoder.matches("123", adminUser.getPasswordHash())) {
                adminUser.setPasswordHash(passwordEncoder.encode("123"));
                adminUser.setRole(adminRole);
                adminUser.setIsActive(true);
                userRepository.save(adminUser);
                System.out.println("Đã đặt lại mật khẩu tài khoản admin về (admin / 123)");
            }

            // Seed customer demo
            if (!userRepository.existsByUsername("khachhang")) {
                User demoUser = User.builder()
                        .username("khachhang")
                        .email("khachhang@devcine.com")
                        .passwordHash(passwordEncoder.encode("Khach@123"))
                        .fullName("Nguyễn Văn An")
                        .phone("0901234567")
                        .role(customerRole)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                userRepository.save(demoUser);
                Customer demoCustomer = Customer.builder()
                        .userId(demoUser.getId())
                        .user(demoUser)
                        .membershipTier("SILVER")
                        .loyaltyPoints(500)
                        .build();
                customerRepository.save(demoCustomer);
                walletRepository.save(Wallet.builder()
                        .customer(demoCustomer)
                        .balance(new BigDecimal("500000"))
                        .status("ACTIVE")
                        .build());
                System.out.println("Đã tạo tài khoản demo khách hàng (khachhang / Khach@123)");
            }

            // Backfill: đảm bảo mọi user vai trò CUSTOMER đều có bản ghi Customer
            // (dữ liệu cũ tạo trước khi có logic seed Customer có thể bị thiếu -> /api/customers/{id} 404).
            java.util.Set<Integer> existingCustomerIds = customerRepository.findAll().stream()
                    .map(Customer::getUserId)
                    .collect(java.util.stream.Collectors.toSet());
            List<User> missingCustomerUsers = userRepository.findAllByRoleName("CUSTOMER").stream()
                    .filter(u -> !existingCustomerIds.contains(u.getId()))
                    .toList();
            for (User u : missingCustomerUsers) {
                // KHÔNG set userId thủ công: @MapsId tự lấy id từ user; để @Id null giúp Spring Data
                // dùng persist (entity mới) thay vì merge (gây AssertionFailure với @MapsId).
                customerRepository.save(Customer.builder()
                        .user(u)
                        .membershipTier("BRONZE")
                        .loyaltyPoints(0)
                        .build());
            }
            if (!missingCustomerUsers.isEmpty()) {
                System.out.println("Đã bổ sung " + missingCustomerUsers.size() + " bản ghi Customer còn thiếu.");
            }

            // Seed thực đơn F&B / Combo mẫu (combo bắp nước, đồ uống, đồ ăn vặt thường gặp ở rạp)
            if (fnbItemRepository.count() == 0) {
                List<FnbItem> menu = List.of(
                        FnbItem.builder().name("Combo Solo").type("COMBO").price(new BigDecimal("89000"))
                                .description("1 bắp ngọt lớn + 1 nước ngọt lớn").isActive(true).build(),
                        FnbItem.builder().name("Combo Couple").type("COMBO").price(new BigDecimal("129000"))
                                .description("1 bắp ngọt lớn + 2 nước ngọt lớn").isActive(true).build(),
                        FnbItem.builder().name("Combo Nhóm (Party)").type("COMBO").price(new BigDecimal("219000"))
                                .description("2 bắp ngọt lớn + 4 nước ngọt").isActive(true).build(),
                        FnbItem.builder().name("Combo Bắp Phô Mai").type("COMBO").price(new BigDecimal("109000"))
                                .description("1 bắp phô mai lớn + 1 nước ngọt lớn").isActive(true).build(),
                        FnbItem.builder().name("Bắp Ngọt (Lớn)").type("POPCORN").price(new BigDecimal("55000"))
                                .description("Bắp rang bơ vị ngọt size lớn").isActive(true).build(),
                        FnbItem.builder().name("Bắp Phô Mai (Lớn)").type("POPCORN").price(new BigDecimal("65000"))
                                .description("Bắp rang phủ phô mai size lớn").isActive(true).build(),
                        FnbItem.builder().name("Bắp Caramel (Lớn)").type("POPCORN").price(new BigDecimal("65000"))
                                .description("Bắp rang caramel giòn ngọt size lớn").isActive(true).build(),
                        FnbItem.builder().name("Coca-Cola (Lớn)").type("DRINK").price(new BigDecimal("35000"))
                                .description("Nước ngọt có ga Coca-Cola ly lớn").isActive(true).build(),
                        FnbItem.builder().name("Pepsi (Lớn)").type("DRINK").price(new BigDecimal("35000"))
                                .description("Nước ngọt có ga Pepsi ly lớn").isActive(true).build(),
                        FnbItem.builder().name("Sprite (Lớn)").type("DRINK").price(new BigDecimal("35000"))
                                .description("Nước ngọt có ga Sprite ly lớn").isActive(true).build(),
                        FnbItem.builder().name("Nước Suối Aquafina").type("DRINK").price(new BigDecimal("20000"))
                                .description("Nước tinh khiết Aquafina 500ml").isActive(true).build(),
                        FnbItem.builder().name("Khoai Tây Chiên").type("SNACK").price(new BigDecimal("45000"))
                                .description("Khoai tây chiên giòn kèm sốt").isActive(true).build(),
                        FnbItem.builder().name("Xúc Xích (Hotdog)").type("SNACK").price(new BigDecimal("49000"))
                                .description("Bánh mì kẹp xúc xích nóng").isActive(true).build(),
                        FnbItem.builder().name("Snack Khoai Tây").type("SNACK").price(new BigDecimal("25000"))
                                .description("Gói snack ăn vặt giòn rụm").isActive(true).build()
                );
                fnbItemRepository.saveAll(menu);
                System.out.println("Đã tạo " + menu.size() + " món F&B/combo mẫu.");
            }

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

            // Cập nhật giá ghế thực tế (seed cũ để NORMAL = 0đ). Khớp với chú thích giá ở giao diện khách.
            SeatType normalType = seatTypeRepository.findAll().stream()
                    .filter(t -> "NORMAL".equalsIgnoreCase(t.getName())).findFirst().orElse(null);
            if (normalType != null && (normalType.getPriceModifier() == null
                    || normalType.getPriceModifier().compareTo(BigDecimal.ZERO) == 0)) {
                for (SeatType t : seatTypeRepository.findAll()) {
                    if ("NORMAL".equalsIgnoreCase(t.getName())) {
                        t.setPriceModifier(new BigDecimal("110000")); 
                    }else if ("VIP".equalsIgnoreCase(t.getName())) {
                        t.setPriceModifier(new BigDecimal("150000")); 
                    }else if ("SWEETBOX".equalsIgnoreCase(t.getName())) {
                        t.setPriceModifier(new BigDecimal("300000"));
                    }
                    seatTypeRepository.save(t);
                }
                System.out.println("Đã cập nhật giá ghế thực tế (Thường 110k / VIP 150k / Sweetbox 300k).");
            }

            // Seed phim thật (thêm theo slug nếu chưa có — không trùng phim cũ)
            {
                List<Movie> seedMovies = List.of(
                        Movie.builder().title("Mai").slug("mai-2024").durationMins(131).ageRating("T18")
                                .status("active").country("Việt Nam").language("Tiếng Việt").director("Trấn Thành")
                                .releaseDate(LocalDate.now().minusDays(20)).productionYear(2024)
                                .description("Chuyện đời của Mai — một người phụ nữ với quá khứ nhiều biến cố, đi tìm hạnh phúc và sự bình yên cho riêng mình.")
                                .build(),
                        Movie.builder().title("Lật Mặt 7: Một Điều Ước").slug("lat-mat-7-mot-dieu-uoc").durationMins(132).ageRating("T16")
                                .status("active").country("Việt Nam").language("Tiếng Việt").director("Lý Hải")
                                .releaseDate(LocalDate.now().minusDays(10)).productionYear(2024)
                                .description("Câu chuyện cảm động về tình mẫu tử và hành trình gắn kết của một gia đình nhiều thế hệ.")
                                .build(),
                        Movie.builder().title("Đào, Phở Và Piano").slug("dao-pho-va-piano").durationMins(100).ageRating("T13")
                                .status("active").country("Việt Nam").language("Tiếng Việt").director("Phi Tiến Sơn")
                                .releaseDate(LocalDate.now().minusDays(30)).productionYear(2024)
                                .description("Bức tranh lãng mạn và bi tráng về Hà Nội mùa đông năm 1946 qua những con người bình dị.")
                                .build(),
                        Movie.builder().title("Dune: Hành Tinh Cát - Phần Hai").slug("dune-part-two").durationMins(166).ageRating("T13")
                                .status("active").country("Mỹ").language("Tiếng Anh").director("Denis Villeneuve")
                                .releaseDate(LocalDate.now().minusDays(15)).productionYear(2024)
                                .description("Paul Atreides liên kết cùng người Fremen để báo thù và ngăn chặn tương lai khủng khiếp mà chỉ mình anh thấy trước.")
                                .build(),
                        Movie.builder().title("Kung Fu Panda 4").slug("kung-fu-panda-4").durationMins(94).ageRating("P")
                                .status("active").country("Mỹ").language("Lồng tiếng").director("Mike Mitchell")
                                .releaseDate(LocalDate.now().minusDays(5)).productionYear(2024)
                                .description("Po phải tìm và huấn luyện Thần Long Đại Hiệp kế nhiệm trong khi đối đầu với một phù thủy quyền năng.")
                                .build(),
                        Movie.builder().title("Godzilla x Kong: Đế Chế Mới").slug("godzilla-x-kong").durationMins(115).ageRating("T13")
                                .status("active").country("Mỹ").language("Tiếng Anh").director("Adam Wingard")
                                .releaseDate(LocalDate.now().minusDays(3)).productionYear(2024)
                                .description("Godzilla và Kong buộc phải bắt tay chống lại một mối đe dọa khổng lồ ẩn sâu trong lòng Trái Đất.")
                                .build()
                );
                List<Movie> toAdd = seedMovies.stream()
                        .filter(m -> movieRepository.findBySlug(m.getSlug()).isEmpty())
                        .toList();
                if (!toAdd.isEmpty()) {
                    movieRepository.saveAll(toAdd);
                    System.out.println("Đã tạo " + toAdd.size() + " phim mẫu.");
                }
            }

            // Seed lịch chiếu mẫu MỘT LẦN (cờ DEMO_SCHEDULE_SEEDED) — dùng riêng các phim mẫu,
            // dọn các suất sắp tới cũ (dữ liệu test lẫn lộn) rồi tạo lịch 3 ngày sạch sẽ.
            boolean demoScheduleSeeded = systemSettingRepository.findById("DEMO_SCHEDULE_SEEDED").isPresent();
            if (!demoScheduleSeeded) {
                List<String> curatedSlugs = List.of(
                        "mai-2024", "lat-mat-7-mot-dieu-uoc", "dao-pho-va-piano",
                        "dune-part-two", "kung-fu-panda-4", "godzilla-x-kong");
                List<Movie> movies = curatedSlugs.stream()
                        .map(slug -> movieRepository.findBySlug(slug).orElse(null))
                        .filter(m -> m != null)
                        .toList();
                List<Room> rooms = roomRepository.findAll();
                List<MovieFormat> formats = formatRepository.findAll();

                // Xoá các suất chiếu từ hôm nay trở đi (lịch demo cũ/lẫn lộn) để tạo lại sạch
                List<Showtime> upcomingOld = showtimeRepository.findAll().stream()
                        .filter(s -> s.getStartTime() != null
                        && !s.getStartTime().isBefore(LocalDate.now().atStartOfDay()))
                        .toList();
                if (!upcomingOld.isEmpty()) {
                    showtimeRepository.deleteAll(upcomingOld);
                }

                if (!movies.isEmpty() && !rooms.isEmpty() && !formats.isEmpty()) {
                    MovieFormat imaxFmt = formats.stream()
                            .filter(f -> f.getName() != null && f.getName().toUpperCase().contains("IMAX"))
                            .findFirst().orElse(formats.get(0));
                    MovieFormat stdFmt = formats.stream()
                            .filter(f -> f.getName() != null && f.getName().contains("2D"))
                            .findFirst().orElse(formats.get(0));

                    int[][] slots = {{9, 0}, {12, 30}, {16, 0}, {19, 30}, {22, 0}};
                    List<Showtime> showtimes = new java.util.ArrayList<>();
                    int movieIdx = 0;
                    for (int day = 0; day < 3; day++) {
                        LocalDate date = LocalDate.now().plusDays(day);
                        for (Room room : rooms) {
                            MovieFormat fmt = (room.getType() != null && room.getType().toUpperCase().contains("IMAX"))
                                    ? imaxFmt : stdFmt;
                            for (int[] slot : slots) {
                                Movie m = movies.get(movieIdx % movies.size());
                                movieIdx++;
                                LocalDateTime start = date.atTime(slot[0], slot[1]);
                                int dur = m.getDurationMins() != null ? m.getDurationMins() : 120;
                                LocalDateTime end = start.plusMinutes(dur + 15);
                                showtimes.add(Showtime.builder()
                                        .movie(m).room(room).format(fmt)
                                        .startTime(start).endTime(end)
                                        .status("SCHEDULED").build());
                            }
                        }
                    }
                    showtimeRepository.saveAll(showtimes);
                    System.out.println("Đã tạo " + showtimes.size() + " suất chiếu mẫu cho 3 ngày.");
                }
                systemSettingRepository.save(SystemSetting.builder()
                        .settingKey("DEMO_SCHEDULE_SEEDED").settingValue("true").build());
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

            // Seed nội dung FAQ trang Hỗ trợ (chạy một lần khi bảng trống)
            if (faqRepository.count() == 0) {
                List<Faq> faqs = List.of(
                        Faq.builder().category("Đặt vé & Thanh toán").displayOrder(1).isActive(true)
                                .question("Làm thế nào để đặt vé trực tuyến?")
                                .answer("Bạn có thể đặt vé dễ dàng qua website DevCine theo các bước sau:\n1. Chọn phim và suất chiếu mong muốn.\n2. Chọn vị trí ghế ngồi (Standard, VIP hoặc Sweetbox).\n3. Lựa chọn bắp nước/combo kèm theo (tùy chọn).\n4. Thanh toán qua VNPAY hoặc chuyển khoản (mã VietQR).\nSau khi đặt vé thành công, thông tin vé sẽ được gửi về email và lưu trong mục \"Vé của tôi\".").build(),
                        Faq.builder().category("Đặt vé & Thanh toán").displayOrder(2).isActive(true)
                                .question("Hủy vé đã đặt có được hoàn tiền không?")
                                .answer("Theo quy định của DevCine, vé đã thanh toán thành công không được hoàn trả hoặc đổi lại dưới bất kỳ hình thức nào. Quý khách vui lòng kiểm tra kỹ thông tin phim, rạp chiếu, ngày giờ và số ghế trước khi thanh toán. Trong trường hợp sự cố kỹ thuật từ phía rạp, chúng tôi sẽ hỗ trợ đổi suất chiếu hoặc hoàn tiền tùy tình huống.").build(),
                        Faq.builder().category("Đặt vé & Thanh toán").displayOrder(3).isActive(true)
                                .question("DevCine hỗ trợ những phương thức thanh toán nào?")
                                .answer("Hiện DevCine hỗ trợ thanh toán qua cổng VNPAY và chuyển khoản ngân hàng bằng mã VietQR được sinh tự động. Số tiền và nội dung chuyển khoản đã được điền sẵn để bạn thanh toán nhanh chóng.").build(),
                        Faq.builder().category("Thành viên DevCine").displayOrder(1).isActive(true)
                                .question("Làm sao để tích điểm thành viên?")
                                .answer("Mỗi giao dịch đặt vé thành công sẽ được tích điểm tự động vào tài khoản của bạn theo tỉ lệ quy đổi của hệ thống. Điểm tích lũy có thể dùng để đổi voucher ưu đãi trong mục \"Ưu đãi của tôi\".").build(),
                        Faq.builder().category("Thành viên DevCine").displayOrder(2).isActive(true)
                                .question("Đổi điểm lấy ưu đãi như thế nào?")
                                .answer("Vào mục \"Ưu đãi của tôi\" → tab \"Đổi điểm lấy ưu đãi\", chọn voucher bạn muốn và xác nhận đổi. Hệ thống sẽ trừ điểm tương ứng và thêm voucher vào tài khoản để dùng khi thanh toán.").build(),
                        Faq.builder().category("Quy định rạp").displayOrder(1).isActive(true)
                                .question("Tôi cần đến rạp trước giờ chiếu bao lâu?")
                                .answer("Quý khách nên có mặt tại rạp trước giờ chiếu khoảng 15–20 phút để check-in vé và ổn định chỗ ngồi. Vui lòng xuất trình mã vé tại quầy hoặc cổng soát vé.").build(),
                        Faq.builder().category("Quy định rạp").displayOrder(2).isActive(true)
                                .question("Phân loại độ tuổi phim có ý nghĩa gì?")
                                .answer("DevCine áp dụng phân loại độ tuổi theo quy định: P (mọi đối tượng), K (dưới 13 có người lớn đi kèm), T13/T16/T18 (từ 13/16/18 tuổi). Vui lòng mang theo giấy tờ tùy thân khi cần xác minh độ tuổi.").build(),
                        Faq.builder().category("Ưu đãi & Khuyến mãi").displayOrder(1).isActive(true)
                                .question("Làm sao để nhận mã giảm giá?")
                                .answer("Bạn có thể xem các chương trình đang chạy ở trang Khuyến mãi, lưu mã công khai hoặc nhập mã bí mật trong mục \"Ưu đãi của tôi\". Mã hợp lệ sẽ dùng được ngay ở bước thanh toán.").build(),
                        Faq.builder().category("Ưu đãi & Khuyến mãi").displayOrder(2).isActive(true)
                                .question("Có thể dùng nhiều voucher cho một đơn không?")
                                .answer("Mỗi đơn đặt vé chỉ áp dụng được một voucher giảm giá. Hệ thống sẽ tính toán số tiền được giảm và hiển thị tổng thanh toán cuối cùng trước khi bạn xác nhận.").build()
                );
                faqRepository.saveAll(faqs);
                System.out.println("Đã tạo " + faqs.size() + " câu hỏi FAQ mẫu.");
            }

            // Đổi tên danh mục FAQ "Thành viên Prestige" -> "Thành viên DevCine" (idempotent)
            List<Faq> prestigeFaqs = faqRepository.findByCategory("Thành viên Prestige");
            if (!prestigeFaqs.isEmpty()) {
                prestigeFaqs.forEach(f -> f.setCategory("Thành viên DevCine"));
                faqRepository.saveAll(prestigeFaqs);
                System.out.println("Đã đổi tên danh mục FAQ sang 'Thành viên DevCine'.");
            }

            // Backfill thông tin mở rộng cho cụm rạp (idempotent: chỉ ghi khi imageUrl còn trống)
            Cinema c1 = cinemaRepository.findById(1).orElse(null);
            if (c1 != null && (c1.getImageUrl() == null || c1.getImageUrl().isBlank())) {
                c1.setImageUrl("https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1200&q=80");
                c1.setDescription("Cụm rạp cao cấp tại tòa nhà cao nhất Việt Nam — Vincom Landmark 81. Trang bị phòng chiếu IMAX màn ảnh khổng lồ cùng hệ thống âm thanh Dolby Atmos sống động, mang đến trải nghiệm điện ảnh đẳng cấp bậc nhất.");
                c1.setLatitude(10.794903);
                c1.setLongitude(106.721866);
                c1.setAmenities("IMAX,Dolby Atmos,Phòng Sweetbox,Bãi đỗ xe,Khu vực F&B,Wifi miễn phí");
                c1.setStatus("ACTIVE");
                cinemaRepository.save(c1);
                System.out.println("Đã bổ sung thông tin mở rộng cho cụm rạp DevCine Landmark 81.");
            }
            Cinema c2 = cinemaRepository.findById(2).orElse(null);
            if (c2 != null && (c2.getImageUrl() == null || c2.getImageUrl().isBlank())) {
                c2.setImageUrl("https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=1200&q=80");
                c2.setDescription("Tọa lạc tại tháp tài chính Bitexco biểu tượng của Quận 1, DevCine Bitexco mang phong cách hiện đại với phòng chiếu Sweetbox dành cho các cặp đôi và hệ thống âm thanh Dolby Atmos.");
                c2.setLatitude(10.771706);
                c2.setLongitude(106.704309);
                c2.setAmenities("Phòng Sweetbox,Dolby Atmos,Bãi đỗ xe,Khu vực F&B,Wifi miễn phí");
                c2.setStatus("ACTIVE");
                cinemaRepository.save(c2);
                System.out.println("Đã bổ sung thông tin mở rộng cho cụm rạp DevCine Bitexco.");
            }
        };
    }
}
