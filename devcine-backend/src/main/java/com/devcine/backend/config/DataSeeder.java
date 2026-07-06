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
import com.devcine.backend.entity.Staff;
import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.Faq;
import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.entity.Movie;
import com.devcine.backend.entity.MovieFormat;
import com.devcine.backend.entity.PricingRule;
import com.devcine.backend.entity.PromoArticle;
import com.devcine.backend.entity.Promotion;
import com.devcine.backend.entity.Role;
import com.devcine.backend.entity.Room;
import com.devcine.backend.entity.SeatType;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.entity.SystemSetting;
import com.devcine.backend.entity.User;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.FaqRepository;
import com.devcine.backend.repository.FnbItemRepository;
import com.devcine.backend.repository.MovieFormatRepository;
import com.devcine.backend.repository.PricingRuleRepository;
import com.devcine.backend.repository.PromoArticleRepository;
import com.devcine.backend.repository.PromotionRepository;
import com.devcine.backend.repository.MovieRepository;
import com.devcine.backend.repository.RoleRepository;
import com.devcine.backend.repository.RoomRepository;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.repository.SeatTypeRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import com.devcine.backend.repository.SystemSettingRepository;
import com.devcine.backend.repository.UserRepository;

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
            PricingRuleRepository pricingRuleRepository,
            SystemSettingRepository systemSettingRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            StaffRepository staffRepository,
            CustomerRepository customerRepository,
            FnbItemRepository fnbItemRepository,
            ShowtimeRepository showtimeRepository,
            FaqRepository faqRepository,
            PromoArticleRepository promoArticleRepository,
            PromotionRepository promotionRepository) {
        return args -> {
            if (systemSettingRepository.findById("LOYALTY_POINT_RATE").isEmpty()) {
                systemSettingRepository.save(SystemSetting.builder()
                        .settingKey("LOYALTY_POINT_RATE")
                        .settingValue("1000")
                        .build());
                System.out.println("Đã cấu hình mặc định LOYALTY_POINT_RATE = 1000 VNĐ = 1 điểm.");
            }

            if (systemSettingRepository.findById("SEAT_HOLD_MINUTES").isEmpty()) {
                systemSettingRepository.save(SystemSetting.builder()
                        .settingKey("SEAT_HOLD_MINUTES")
                        .settingValue("10")
                        .build());
                System.out.println("Đã cấu hình mặc định SEAT_HOLD_MINUTES = 10 phút.");
            }

            if (systemSettingRepository.findById("MAX_TICKETS_PER_BOOKING").isEmpty()) {
                systemSettingRepository.save(SystemSetting.builder()
                        .settingKey("MAX_TICKETS_PER_BOOKING")
                        .settingValue("8")
                        .build());
                System.out.println("Đã cấu hình mặc định MAX_TICKETS_PER_BOOKING = 8 vé.");
            }

            if (systemSettingRepository.findById("BOOKING_LATE_MINUTES").isEmpty()) {
                systemSettingRepository.save(SystemSetting.builder()
                        .settingKey("BOOKING_LATE_MINUTES")
                        .settingValue("15")
                        .build());
                System.out.println("Đã cấu hình mặc định BOOKING_LATE_MINUTES = 15 phút.");
            }

            // Seed roles
            Role adminRole = roleRepository.findByName("ADMIN").orElseGet(()
                    -> roleRepository.save(Role.builder().name("ADMIN").build()));
            Role managerRole = roleRepository.findByName("MANAGER").orElseGet(()
                    -> roleRepository.save(Role.builder().name("MANAGER").build()));
            Role staffRole = roleRepository.findByName("STAFF").orElseGet(()
                    -> roleRepository.save(Role.builder().name("STAFF").build()));
            Role customerRole = roleRepository.findByName("CUSTOMER").orElseGet(()
                    -> roleRepository.save(Role.builder().name("CUSTOMER").build()));

            // Seed ma trận phân quyền mặc định.
            // Đặt lại MỘT LẦN trên DB đã có dữ liệu qua cờ PERMISSION_MATRIX_V2 (seed thường chỉ set khi blank).
            boolean permissionMatrixV2 = systemSettingRepository.findById("PERMISSION_MATRIX_V2").isPresent();
            if (!permissionMatrixV2 || adminRole.getPermissionsMatrix() == null || adminRole.getPermissionsMatrix().isBlank()) {
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
            // STAFF (trần quyền TĨNH): KHÔNG có báo cáo doanh thu (dashboard_stats) theo spec.
            // Nghiệp vụ thực tế (bán vé/F&B/kiểm vé) được kích hoạt theo Position khi mở ca — ShiftAccessService.
            if (!permissionMatrixV2 || staffRole.getPermissionsMatrix() == null || staffRole.getPermissionsMatrix().isBlank()) {
                staffRole.setPermissionsMatrix("{"
                        + "\"movies\":[\"view\"],"
                        + "\"schedules\":[\"view\"],"
                        + "\"pos_ticketing\":[\"view\",\"add\",\"edit\"],"
                        + "\"pos_inventory\":[\"view\",\"edit\"],"
                        + "\"support\":[\"view\",\"edit\"]}");
                roleRepository.save(staffRole);
                System.out.println("Đã cấu hình ma trận phân quyền mặc định cho STAFF.");
            }
            // MANAGER = "admin thu nhỏ theo 1 cơ sở": vận hành đầy đủ cụm rạp (lịch chiếu, giá vé/F&B,
            // khuyến mãi, kho, nhân sự & ca, toàn bộ báo cáo). Phạm vi cơ sở do scoping cinemaId (SecurityUtils).
            if (!permissionMatrixV2 || managerRole.getPermissionsMatrix() == null || managerRole.getPermissionsMatrix().isBlank()) {
                managerRole.setPermissionsMatrix("{"
                        + "\"dashboard_stats\":[\"view\",\"export\"],"
                        + "\"movies\":[\"view\"],"
                        + "\"schedules\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"banners\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"promotions\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"pricing\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"cinemas\":[\"view\"],"
                        + "\"staff_management\":[\"view\",\"add\",\"edit\",\"delete\",\"export\"],"
                        + "\"pos_ticketing\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"pos_inventory\":[\"view\",\"add\",\"edit\",\"delete\"],"
                        + "\"support\":[\"view\",\"edit\",\"delete\"]}");
                roleRepository.save(managerRole);
                System.out.println("Đã cấu hình ma trận phân quyền mặc định cho MANAGER.");
            }
            if (!permissionMatrixV2) {
                systemSettingRepository.save(SystemSetting.builder()
                        .settingKey("PERMISSION_MATRIX_V2").settingValue("true").build());
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

            // Pricing engine: đổi giá ghế từ TUYỆT ĐỐI sang PHỤ THU + seed ma trận giá nền (CHẠY 1 LẦN).
            // Sau đổi nghĩa: priceModifier = phụ thu cộng dồn vào giá nền (không còn là giá vé tuyệt đối).
            boolean demoPricingSeeded = systemSettingRepository.findById("DEMO_PRICING_SEEDED").isPresent();
            if (!demoPricingSeeded) {
                for (SeatType t : seatTypeRepository.findAll()) {
                    if ("NORMAL".equalsIgnoreCase(t.getName())) t.setPriceModifier(BigDecimal.ZERO);
                    else if ("VIP".equalsIgnoreCase(t.getName())) t.setPriceModifier(new BigDecimal("30000"));
                    else if ("SWEETBOX".equalsIgnoreCase(t.getName())) t.setPriceModifier(new BigDecimal("100000"));
                    seatTypeRepository.save(t);
                }

                if (pricingRuleRepository.findByRuleType("BASE_PRICE").isEmpty()) {
                    // Giá nền Người lớn theo (loại ngày, khung giờ); các đối tượng khác lệch cố định.
                    java.util.Map<String, int[]> adultBase = new java.util.LinkedHashMap<>();
                    adultBase.put("WEEKDAY", new int[]{60000, 70000, 85000});    // EARLY, BEFORE_17H, AFTER_17H
                    adultBase.put("WEDNESDAY", new int[]{75000, 75000, 75000});  // T4 đồng giá
                    adultBase.put("WEEKEND", new int[]{80000, 100000, 115000});
                    String[] slots = {"EARLY", "BEFORE_17H", "AFTER_17H"};
                    // Lệch giá theo đối tượng so với Người lớn (T4 đồng giá nên không áp lệch)
                    java.util.Map<String, Integer> audienceOffset = new java.util.LinkedHashMap<>();
                    audienceOffset.put("ADULT", 0);
                    audienceOffset.put("STUDENT", -10000);
                    audienceOffset.put("CHILD", -20000);
                    audienceOffset.put("SENIOR", -20000);

                    for (var dayEntry : adultBase.entrySet()) {
                        String dayType = dayEntry.getKey();
                        int[] base = dayEntry.getValue();
                        for (int i = 0; i < slots.length; i++) {
                            for (var audEntry : audienceOffset.entrySet()) {
                                int value = "WEDNESDAY".equals(dayType)
                                        ? base[i]
                                        : Math.max(40000, base[i] + audEntry.getValue());
                                pricingRuleRepository.save(PricingRule.builder()
                                        .name("Giá nền")
                                        .ruleType("BASE_PRICE")
                                        .dayType(dayType)
                                        .timeSlot(slots[i])
                                        .audienceType(audEntry.getKey())
                                        .value(new BigDecimal(value))
                                        .priority(0)
                                        .active(true)
                                        .build());
                            }
                        }
                    }
                    System.out.println("Đã seed ma trận giá nền (ngày × khung giờ × đối tượng).");
                }

                systemSettingRepository.save(SystemSetting.builder()
                        .settingKey("DEMO_PRICING_SEEDED").settingValue("true").build());
                System.out.println("Đã chuyển giá ghế sang phụ thu (Thường +0 / VIP +30k / Sweetbox +100k).");
            }

            // Áp dụng BIỂU GIÁ LOTTE (chạy 1 lần, cờ LOTTE_PRICING_SEEDED): giá nền theo đối tượng × ngày
            // (không theo giờ), phụ thu 3D theo ngày, KHÔNG phụ thu ghế/phòng.
            boolean lottePricingSeeded = systemSettingRepository.findById("LOTTE_PRICING_SEEDED_V2").isPresent();
            if (!lottePricingSeeded) {
                // Bỏ phụ thu loại ghế (Lotte không phân biệt giá ghế)
                for (SeatType t : seatTypeRepository.findAll()) {
                    t.setPriceModifier(BigDecimal.ZERO);
                    seatTypeRepository.save(t);
                }
                // Phụ thu định dạng theo ngày: 3D +20k (thường) / +30k (cuối tuần & lễ); 2D = 0; IMAX giữ nguyên
                for (MovieFormat f : formatRepository.findAll()) {
                    String n = f.getName() != null ? f.getName().toUpperCase() : "";
                    if (n.contains("3D")) {
                        f.setSurcharge(new BigDecimal("20000"));
                        f.setWeekendSurcharge(new BigDecimal("30000"));
                    } else if (!n.contains("IMAX")) {
                        f.setSurcharge(BigDecimal.ZERO);
                        f.setWeekendSurcharge(BigDecimal.ZERO);
                    }
                    formatRepository.save(f);
                }
                // Re-seed ma trận giá nền theo Lotte (timeSlot=ALL vì không phân biệt giờ)
                pricingRuleRepository.deleteAll(pricingRuleRepository.findByRuleType("BASE_PRICE"));
                java.util.Map<String, int[]> baseByDay = new java.util.LinkedHashMap<>(); // [ADULT, STUDENT]
                baseByDay.put("WEEKDAY", new int[]{45000, 45000});
                baseByDay.put("WEEKEND", new int[]{75000, 45000});
                baseByDay.put("HOLIDAY", new int[]{85000, 55000});
                String[] auds = {"ADULT", "STUDENT"};
                for (var e : baseByDay.entrySet()) {
                    for (int i = 0; i < auds.length; i++) {
                        pricingRuleRepository.save(PricingRule.builder()
                                .name("Giá nền").ruleType("BASE_PRICE")
                                .dayType(e.getKey()).timeSlot("ALL").audienceType(auds[i])
                                .value(new BigDecimal(e.getValue()[i])).priority(0).active(true).build());
                    }
                }
                systemSettingRepository.save(SystemSetting.builder()
                        .settingKey("LOTTE_PRICING_SEEDED_V2").settingValue("true").build());
                System.out.println("Đã áp dụng biểu giá Lotte (2 đối tượng NL/HSSV × ngày, 3D +20k/+30k, bỏ phụ thu ghế).");
            }

            // Backfill điểm tích lũy trọn đời (lifetime_points) cho khách hiện có (CHẠY 1 LẦN).
            // Cột mới mặc định null/0 → suy ra từ số dư hiện tại để hạng thành viên không bị tụt sau khi
            // tách "điểm khả dụng" khỏi "điểm tích lũy trọn đời".
            if (systemSettingRepository.findById("LOYALTY_LIFETIME_BACKFILLED").isEmpty()) {
                for (Customer c : customerRepository.findAll()) {
                    int balance = c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0;
                    int lifetime = c.getLifetimePoints() != null ? c.getLifetimePoints() : 0;
                    if (lifetime < balance) {
                        c.setLifetimePoints(balance);
                        customerRepository.save(c);
                    }
                }
                systemSettingRepository.save(SystemSetting.builder()
                        .settingKey("LOYALTY_LIFETIME_BACKFILLED").settingValue("true").build());
                System.out.println("Đã backfill điểm tích lũy trọn đời cho khách hiện có.");
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

            // Backfill trailer chính thức cho phim mẫu (idempotent — chỉ ghi khi đang trống,
            // KHÔNG đè trailer admin tự nhập). Link trailer chính thức trên YouTube.
            {
                java.util.Map<String, String> trailerBySlug = java.util.Map.of(
                        "mai-2024", "https://www.youtube.com/watch?v=EX6clvId19s",
                        "lat-mat-7-mot-dieu-uoc", "https://www.youtube.com/watch?v=d1ZHdosjNX8",
                        "dao-pho-va-piano", "https://www.youtube.com/watch?v=qn1t_biQigc",
                        "dune-part-two", "https://www.youtube.com/watch?v=WUBQdC__fC4",
                        "kung-fu-panda-4", "https://www.youtube.com/watch?v=_inKs4eeHiI",
                        "godzilla-x-kong", "https://www.youtube.com/watch?v=lV1OOlGwExM");
                List<Movie> trailerUpdates = new java.util.ArrayList<>();
                trailerBySlug.forEach((slug, url) -> movieRepository.findBySlug(slug).ifPresent(m -> {
                    if (m.getTrailerUrl() == null || m.getTrailerUrl().isBlank()) {
                        m.setTrailerUrl(url);
                        trailerUpdates.add(m);
                    }
                }));
                if (!trailerUpdates.isEmpty()) {
                    movieRepository.saveAll(trailerUpdates);
                    System.out.println("Đã cập nhật trailer cho " + trailerUpdates.size() + " phim mẫu.");
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

            // ===== Tự bù lịch chiếu cho 7 ngày tới (hôm nay .. +6) =====
            // Chạy MỖI lần khởi động: ngày nào đang TRỐNG suất thì tạo mới — idempotent
            // (bỏ qua ngày đã có lịch, tự cuốn chiếu khi lịch cũ trôi vào quá khứ).
            {
                List<String> rollSlugs = List.of(
                        "mai-2024", "lat-mat-7-mot-dieu-uoc", "dao-pho-va-piano",
                        "dune-part-two", "kung-fu-panda-4", "godzilla-x-kong");
                List<Movie> rollMovies = rollSlugs.stream()
                        .map(slug -> movieRepository.findBySlug(slug).orElse(null))
                        .filter(m -> m != null)
                        .toList();
                List<Room> rollRooms = roomRepository.findAll();
                List<MovieFormat> rollFormats = formatRepository.findAll();
                if (!rollMovies.isEmpty() && !rollRooms.isEmpty() && !rollFormats.isEmpty()) {
                    java.util.Set<LocalDate> daysWithShowtimes = showtimeRepository.findAll().stream()
                            .filter(s -> s.getStartTime() != null)
                            .map(s -> s.getStartTime().toLocalDate())
                            .collect(java.util.stream.Collectors.toSet());
                    MovieFormat imaxFmt = rollFormats.stream()
                            .filter(f -> f.getName() != null && f.getName().toUpperCase().contains("IMAX"))
                            .findFirst().orElse(rollFormats.get(0));
                    MovieFormat stdFmt = rollFormats.stream()
                            .filter(f -> f.getName() != null && f.getName().contains("2D"))
                            .findFirst().orElse(rollFormats.get(0));
                    int[][] rollSlots = {{9, 0}, {12, 30}, {16, 0}, {19, 30}, {22, 0}};
                    LocalDateTime nowTs = LocalDateTime.now();
                    List<Showtime> rollShowtimes = new java.util.ArrayList<>();
                    int rollIdx = 0;
                    for (int day = 0; day <= 6; day++) {
                        LocalDate date = LocalDate.now().plusDays(day);
                        if (daysWithShowtimes.contains(date)) continue; // ngày đã có lịch -> bỏ qua
                        for (Room room : rollRooms) {
                            MovieFormat fmt = (room.getType() != null && room.getType().toUpperCase().contains("IMAX"))
                                    ? imaxFmt : stdFmt;
                            for (int[] slot : rollSlots) {
                                LocalDateTime start = date.atTime(slot[0], slot[1]);
                                if (start.isBefore(nowTs)) continue; // bỏ suất đã qua giờ trong hôm nay
                                Movie m = rollMovies.get(rollIdx % rollMovies.size());
                                rollIdx++;
                                int dur = m.getDurationMins() != null ? m.getDurationMins() : 120;
                                rollShowtimes.add(Showtime.builder()
                                        .movie(m).room(room).format(fmt)
                                        .startTime(start).endTime(start.plusMinutes(dur + 15))
                                        .status("SCHEDULED").build());
                            }
                        }
                    }
                    if (!rollShowtimes.isEmpty()) {
                        showtimeRepository.saveAll(rollShowtimes);
                        System.out.println("Đã bổ sung " + rollShowtimes.size() + " suất chiếu cho các ngày còn trống (tới hết tuần).");
                    }
                }
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

            // Seed tin khuyến mãi mẫu (idempotent: chỉ khi bảng trống)
            if (promoArticleRepository.count() == 0) {
                List<PromoArticle> articles = List.of(
                        PromoArticle.builder()
                                .title("COMBO HÈ RỰC RỠ")
                                .description("Giảm ngay 20% khi mua kèm 2 vé xem phim.")
                                .imageUrl("https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=1000&auto=format&fit=crop")
                                .content("Mùa hè này, DevCine chiêu đãi bạn combo bắp nước siêu hời! Giảm ngay 20% tổng giá trị combo khi mua kèm tối thiểu 2 vé xem phim cho bất kỳ suất chiếu nào. Áp dụng tại toàn bộ cụm rạp DevCine trên toàn quốc.")
                                .startDate(LocalDate.of(2026, 6, 1))
                                .endDate(LocalDate.of(2026, 8, 31))
                                .isActive(true)
                                .displayOrder(1)
                                .createdAt(LocalDateTime.now())
                                .build(),
                        PromoArticle.builder()
                                .title("ƯU ĐÃI HỌC SINH - SINH VIÊN")
                                .description("Đồng giá vé chỉ 45K cho HSSV vào ngày thường.")
                                .imageUrl("https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?q=80&w=1000&auto=format&fit=crop")
                                .content("Chỉ cần xuất trình thẻ học sinh - sinh viên còn hiệu lực tại quầy, bạn được mua vé đồng giá 45.000đ cho mọi suất chiếu 2D từ Thứ Hai đến Thứ Sáu (trừ ngày lễ). Mỗi thẻ áp dụng tối đa 1 vé/suất.")
                                .startDate(LocalDate.of(2026, 5, 1))
                                .endDate(LocalDate.of(2026, 12, 31))
                                .isActive(true)
                                .displayOrder(2)
                                .createdAt(LocalDateTime.now())
                                .build(),
                        PromoArticle.builder()
                                .title("GIỜ VÀNG GIÁ VÉ")
                                .description("Mọi suất chiếu trước 12:00 từ Thứ Hai đến Thứ Năm chỉ với 50.000 VNĐ.")
                                .imageUrl("https://images.unsplash.com/photo-1440404653325-ab127d49abc1?q=80&w=1000&auto=format&fit=crop")
                                .content("Khởi đầu ngày mới cùng điện ảnh! Tất cả các suất chiếu bắt đầu trước 12:00 trưa từ Thứ Hai đến Thứ Năm hàng tuần đồng giá chỉ 50.000đ/vé (ghế thường, định dạng 2D). Số lượng có hạn theo từng suất.")
                                .startDate(LocalDate.of(2026, 1, 1))
                                .endDate(LocalDate.of(2026, 12, 31))
                                .isActive(true)
                                .displayOrder(3)
                                .createdAt(LocalDateTime.now())
                                .build()
                );
                promoArticleRepository.saveAll(articles);
                System.out.println("Đã seed " + articles.size() + " tin khuyến mãi mẫu.");
            }

            // Bổ sung tin khuyến mãi demo (idempotent theo tiêu đề) — đủ dữ liệu để test phân trang
            List<PromoArticle> demoArticles = List.of(
                    demoArticle("KHAI TRƯƠNG CỤM RẠP DEVCINE THỦ ĐỨC", "Giảm 50% toàn bộ vé trong tuần lễ khai trương.",
                            "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=1000&auto=format&fit=crop",
                            "Chào mừng cụm rạp DevCine Thủ Đức chính thức đi vào hoạt động! Trong tuần lễ khai trương, toàn bộ vé xem phim được giảm 50% (áp dụng mọi định dạng, mọi suất chiếu). Nhanh tay đặt vé để trải nghiệm phòng chiếu Laser 4K mới nhất.", 4),
                    demoArticle("THỨ TƯ VUI VẺ - VÉ ĐỒNG GIÁ 55K", "Mọi suất chiếu Thứ Tư hàng tuần đồng giá chỉ 55.000đ.",
                            "https://images.unsplash.com/photo-1485095329183-d0797f0e2c0a?q=80&w=1000&auto=format&fit=crop",
                            "Giữa tuần đừng buồn! Mỗi Thứ Tư, tất cả các suất chiếu 2D đồng giá chỉ 55.000đ/vé cho mọi khách hàng, không giới hạn số lượng. Áp dụng tại toàn hệ thống DevCine.", 5),
                    demoArticle("ĐÊM PHIM KINH DỊ HALLOWEEN", "Marathon phim kinh dị xuyên đêm, tặng bắp rang vị bí ngô.",
                            "https://images.unsplash.com/photo-1509281373149-e957c6296406?q=80&w=1000&auto=format&fit=crop",
                            "Sự kiện đặc biệt mùa Halloween: marathon 3 phim kinh dị bom tấn xuyên đêm cùng phần quà bắp rang vị bí ngô độc quyền. Vé sự kiện giới hạn — đặt sớm để giữ chỗ.", 6),
                    demoArticle("SINH NHẬT DEVCINE - QUÀ TẶNG BẤT NGỜ", "Tặng voucher 100K cho thành viên có sinh nhật trong tháng.",
                            "https://images.unsplash.com/photo-1530103862676-de8c9debad1d?q=80&w=1000&auto=format&fit=crop",
                            "Mừng sinh nhật DevCine, tất cả thành viên có ngày sinh trong tháng sẽ nhận ngay voucher 100.000đ vào mục 'Ưu đãi của tôi'. Đừng quên cập nhật ngày sinh trong hồ sơ để nhận quà nhé!", 7),
                    demoArticle("COMBO GIA ĐÌNH CUỐI TUẦN", "Mua 4 vé tặng 1 bắp lớn và 2 nước ngọt.",
                            "https://images.unsplash.com/photo-1542204165-65bf26472b9b?q=80&w=1000&auto=format&fit=crop",
                            "Cuối tuần trọn niềm vui bên gia đình: mua 4 vé bất kỳ trong cùng một hoá đơn, tặng ngay 1 bắp lớn và 2 ly nước ngọt size vừa. Áp dụng cho suất chiếu Thứ Bảy và Chủ Nhật.", 8)
            );
            int seededArticles = 0;
            for (PromoArticle a : demoArticles) {
                if (!promoArticleRepository.existsByTitleIgnoreCase(a.getTitle())) {
                    promoArticleRepository.save(a);
                    seededArticles++;
                }
            }
            if (seededArticles > 0) System.out.println("Đã bổ sung " + seededArticles + " tin khuyến mãi demo.");

            // Seed voucher/khuyến mãi demo (idempotent theo mã code) — đủ dữ liệu test phân trang
            LocalDateTime promoStart = LocalDateTime.now().minusDays(1);
            LocalDateTime promoEnd = LocalDateTime.of(2026, 12, 31, 23, 59, 59);
            List<Promotion> demoPromos = List.of(
                    demoPromo("WELCOME10", "Chào mừng thành viên mới", "PERCENTAGE", new BigDecimal("10"), promoStart, promoEnd, false, 0),
                    demoPromo("WEEKEND20", "Cuối tuần rực rỡ", "PERCENTAGE", new BigDecimal("20"), promoStart, promoEnd, false, 0),
                    demoPromo("POPCORN50", "Giảm 50K combo bắp nước", "FIXED_AMOUNT", new BigDecimal("50000"), promoStart, promoEnd, false, 0),
                    demoPromo("MIDNIGHT25", "Suất khuya giá sốc", "PERCENTAGE", new BigDecimal("25"), promoStart, promoEnd, false, 0),
                    demoPromo("BIRTHDAY30", "Quà sinh nhật DevCine", "PERCENTAGE", new BigDecimal("30"), promoStart, promoEnd, false, 0),
                    demoPromo("FAMILY15", "Ưu đãi gia đình", "PERCENTAGE", new BigDecimal("15"), promoStart, promoEnd, false, 0),
                    demoPromo("POINT200", "Đổi điểm - Giảm 25%", "PERCENTAGE", new BigDecimal("25"), promoStart, promoEnd, true, 200),
                    demoPromo("POINT500", "Đổi điểm - Giảm 100K", "FIXED_AMOUNT", new BigDecimal("100000"), promoStart, promoEnd, true, 500)
            );
            int seededPromos = 0;
            for (Promotion p : demoPromos) {
                if (!promotionRepository.existsByCodeIgnoreCase(p.getCode())) {
                    promotionRepository.save(p);
                    seededPromos++;
                }
            }
            if (seededPromos > 0) System.out.println("Đã bổ sung " + seededPromos + " voucher khuyến mãi demo.");

            // Seed nhân viên demo trải đều 2 cơ sở (idempotent: chỉ khi bảng nhân viên trống)
            if (staffRepository.count() == 0) {
                Role staffRoleSeed = roleRepository.findByName("STAFF").orElse(null);
                Cinema cs1 = cinemaRepository.findById(1).orElse(null);
                Cinema cs2 = cinemaRepository.findById(2).orElse(null);
                if (staffRoleSeed != null) {
                    // {username, fullName, email, phone, staffCode, cinema}
                    Object[][] seeds = {
                            {"nv_huy",  "Trần Quang Huy",  "huy.tran@devcine.com",  "0905111222", "NV001", cs1},
                            {"nv_lan",  "Nguyễn Thị Lan",  "lan.nguyen@devcine.com", "0905333444", "NV002", cs1},
                            {"nv_minh", "Lê Hoàng Minh",   "minh.le@devcine.com",   "0905555666", "NV003", cs1},
                            {"nv_thao", "Phạm Thu Thảo",   "thao.pham@devcine.com", "0905777888", "NV004", cs2},
                            {"nv_dat",  "Vũ Tiến Đạt",     "dat.vu@devcine.com",    "0905999000", "NV005", cs2}
                    };
                    int seededStaff = 0;
                    for (Object[] sd : seeds) {
                        String username = (String) sd[0];
                        String email = (String) sd[2];
                        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) continue;
                        User u = User.builder()
                                .username(username)
                                .email(email)
                                .fullName((String) sd[1])
                                .phone((String) sd[3])
                                .passwordHash(passwordEncoder.encode("Staff@123"))
                                .role(staffRoleSeed)
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .build();
                        userRepository.save(u);
                        Staff st = Staff.builder()
                                .user(u)
                                .staffCode((String) sd[4])
                                .cinema((Cinema) sd[5])
                                .build();
                        staffRepository.save(st);
                        seededStaff++;
                    }
                    if (seededStaff > 0)
                        System.out.println("Đã seed " + seededStaff + " nhân viên demo trải 2 cơ sở (mật khẩu Staff@123).");
                }
            }

            // Seed 1 tài khoản Quản lý (MANAGER) demo cho cơ sở 1 (idempotent theo username)
            if (!userRepository.existsByUsername("ql_minh") && !userRepository.existsByEmail("quanly.minh@devcine.com")) {
                Cinema managerCinema = cinemaRepository.findById(1).orElse(null);
                User mgrUser = User.builder()
                        .username("ql_minh")
                        .email("quanly.minh@devcine.com")
                        .fullName("Đỗ Hoàng Minh")
                        .phone("0906123456")
                        .passwordHash(passwordEncoder.encode("Manager@123"))
                        .role(managerRole)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                userRepository.save(mgrUser);
                staffRepository.save(Staff.builder()
                        .user(mgrUser)
                        .staffCode("QL001")
                        .cinema(managerCinema)
                        .build());
                System.out.println("Đã seed tài khoản Quản lý demo (ql_minh / Manager@123) cho cơ sở 1.");
            }
        };
    }

    /** Dựng nhanh một tin khuyến mãi demo (đang bật, có thời gian hiển thị trong năm). */
    private PromoArticle demoArticle(String title, String desc, String imageUrl, String content, int order) {
        return PromoArticle.builder()
                .title(title)
                .description(desc)
                .imageUrl(imageUrl)
                .content(content)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .isActive(true)
                .displayOrder(order)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /** Dựng nhanh một promotion/voucher demo công khai. */
    private Promotion demoPromo(String code, String name, String type, BigDecimal value,
                                LocalDateTime start, LocalDateTime end, boolean allowPoint, int points) {
        return Promotion.builder()
                .code(code)
                .name(name)
                .description(name)
                .discountType(type)
                .discountValue(value)
                .startDate(start)
                .endDate(end)
                .allowPointRedemption(allowPoint)
                .pointsRequired(points)
                .build();
    }
}
