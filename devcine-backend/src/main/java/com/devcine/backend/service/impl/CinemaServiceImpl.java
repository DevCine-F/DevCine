package com.devcine.backend.service.impl;

import com.devcine.backend.dto.request.CinemaRequest;
import com.devcine.backend.dto.response.CinemaResponse;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.event.CinemaEmergencyClosedEvent;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.RoomRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.service.CinemaService;
import com.devcine.backend.service.LocationService;
import com.devcine.backend.service.ShowtimeService;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final StaffRepository staffRepository;
    private final RoomRepository roomRepository;
    private final LocationService locationService;
    private final ShowtimeService showtimeService;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Danh mục hợp lệ — đồng bộ với dropdown phía Frontend (chống can thiệp giá trị lạ qua API)
    private static final Set<String> ALLOWED_TYPES = Set.of("STANDARD", "SUPERPLEX", "CINE_COMFORT");
    private static final Set<String> ALLOWED_STATUS = Set.of("ACTIVE", "MAINTENANCE", "CLOSED");

    @PostConstruct
    public void migrateCinemaTypes() {
        cinemaRepository.findAll().forEach(c -> {
            String raw = c.getType();
            if (raw == null) return;
            String updated = raw;
            if (raw.equalsIgnoreCase("Sweetbox") || raw.equalsIgnoreCase("Standard")) updated = "STANDARD";
            else if (raw.equalsIgnoreCase("Superplex")) updated = "SUPERPLEX";
            else if (raw.equalsIgnoreCase("Cine Comfort") || raw.equalsIgnoreCase("Cine_Comfort")) updated = "CINE_COMFORT";
            
            if (!updated.equals(raw)) {
                c.setType(updated);
                cinemaRepository.save(c);
            }
        });
    }

    /** Parse "HH:mm" -> LocalTime (null nếu rỗng). Định dạng đã được @Pattern chặn ở DTO. */
    private java.time.LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        return java.time.LocalTime.parse(s.trim());
    }

    /** Cắt khoảng trắng đầu/cuối + gộp khoảng trắng kép ở giữa. */
    private String clean(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    /**
     * Chuẩn hoá + validate nghiệp vụ cho create/update (an toàn lớp cuối, đồng bộ với FE).
     * @param id null khi tạo mới; khác null khi cập nhật (loại trừ chính nó khi check trùng tên).
     */
    private void normalizeAndValidate(CinemaRequest req, Integer id) {
        req.setName(clean(req.getName()));
        req.setAddress(clean(req.getAddress()));
        req.setCity(clean(req.getCity()));
        req.setDistrict(clean(req.getDistrict()));
        req.setDescription(req.getDescription() == null ? null : req.getDescription().trim());
        if (req.getHotline() != null) req.setHotline(req.getHotline().replaceAll("\\D", ""));

        // Loại cụm rạp & trạng thái: phải nằm trong danh mục cho phép
        if (req.getType() != null && !ALLOWED_TYPES.contains(req.getType())) {
            throw new IllegalArgumentException("Loại cụm rạp không hợp lệ");
        }
        if (req.getStatus() != null && !ALLOWED_STATUS.contains(req.getStatus())) {
            throw new IllegalArgumentException("Trạng thái hoạt động không hợp lệ");
        }

        // Tỉnh/Thành & Quận/Huyện: phải khớp danh mục hành chính (đồng bộ với dropdown FE)
        if (!locationService.isValidProvince(req.getCity())) {
            throw new IllegalArgumentException("Tỉnh/Thành phố không hợp lệ");
        }
        if (!locationService.isValidDistrict(req.getCity(), req.getDistrict())) {
            throw new IllegalArgumentException("Quận/Huyện không thuộc Tỉnh/Thành phố đã chọn");
        }

        // Trùng tên (unique, không phân biệt hoa/thường)
        boolean dup = (id == null)
                ? cinemaRepository.existsByNameIgnoreCase(req.getName())
                : cinemaRepository.existsByNameIgnoreCaseAndIdNot(req.getName(), id);
        if (dup) {
            throw new IllegalArgumentException("Tên cụm rạp đã tồn tại trong hệ thống");
        }
    }

    /** Dựng response và đồng bộ số phòng = số Room thực tế. */
    private CinemaResponse toResponse(Cinema cinema) {
        CinemaResponse res = CinemaResponse.fromEntity(cinema);
        res.setRooms((int) roomRepository.countByCinema_Id(cinema.getId()));
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CinemaResponse> getAllCinemas() {
        return getAllCinemas(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CinemaResponse> getAllActiveCinemas() {
        // Public endpoint - chỉ trả rạp ACTIVE, KHÔNG áp dụng cinema scoping cho STAFF/MANAGER.
        // Dùng cho trang client công khai (HeThongRapView, MovieDetail).
        List<Cinema> cinemas = cinemaRepository.findAllWithManager();
        cinemas = cinemas.stream()
                .filter(c -> c.getStatus() == null || "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .collect(Collectors.toList());
        return cinemas.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CinemaResponse> getAllCinemas(boolean all) {
        List<Cinema> cinemas = cinemaRepository.findAllWithManager();
        // Chỉ giới hạn theo cơ sở với STAFF/MANAGER (nhân viên gắn 1 cụm rạp).
        // ADMIN xem TẤT CẢ rạp (bao gồm rạp đã đóng) khi all=true (ví dụ ở trang quản trị rạp).
        // Khi all=false hoặc với khách/khách vãng lai/client công khai: CHỈ xem rạp đang hoạt động (ACTIVE).
        boolean isAdmin = com.devcine.backend.util.SecurityUtils.hasRole("ADMIN");
        boolean scopedToCinema = com.devcine.backend.util.SecurityUtils.isManager()
                || com.devcine.backend.util.SecurityUtils.hasRole("STAFF");

        if (scopedToCinema) {
            Integer cinemaId = com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId();
            cinemas = cinemaId == null
                    ? java.util.List.of()
                    : cinemas.stream()
                        .filter(c -> c.getId().equals(cinemaId))
                        .collect(Collectors.toList());
        } else if (!isAdmin || !all) {
            // Client / Public hoặc Admin xem giao diện Client (all=false): Loại bỏ hoàn toàn cụm rạp đã đóng
            cinemas = cinemas.stream()
                    .filter(c -> c.getStatus() == null || "ACTIVE".equalsIgnoreCase(c.getStatus()))
                    .collect(Collectors.toList());
        }

        return cinemas.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaResponse getCinemaById(Integer id) {
        Cinema cinema = cinemaRepository.findByIdWithManager(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cụm rạp với ID: " + id));

        // Khách hàng / Public / Nhân viên rạp khác: Không xem được cụm rạp đã đóng
        // Chỉ ADMIN hoặc Quản lý/Nhân viên trực thuộc chính rạp này mới xem được thông tin rạp khi đang CLOSED
        boolean isAdmin = com.devcine.backend.util.SecurityUtils.hasRole("ADMIN");
        boolean isAssignedToThisCinema = (com.devcine.backend.util.SecurityUtils.isManager()
                || com.devcine.backend.util.SecurityUtils.hasRole("STAFF"))
                && id.equals(com.devcine.backend.util.SecurityUtils.getCurrentUserCinemaId());

        if ("CLOSED".equalsIgnoreCase(cinema.getStatus()) && !isAdmin && !isAssignedToThisCinema) {
            throw new IllegalArgumentException("Không tìm thấy cụm rạp hoặc cụm rạp đã ngừng hoạt động.");
        }

        return toResponse(cinema);
    }

    @Override
    @Transactional
    public CinemaResponse createCinema(CinemaRequest request) {
        normalizeAndValidate(request, null);
        Cinema cinema = Cinema.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .district(request.getDistrict())
                .type(request.getType())
                .hotline(request.getHotline())
                .rooms(request.getRooms())
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .amenities(request.getAmenities())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                // Giờ hoạt động: dùng giá trị gửi lên, mặc định 08:00 / 23:30 khi tạo mới.
                .openingTime(request.getOpeningTime() != null ? parseTime(request.getOpeningTime()) : java.time.LocalTime.of(8, 0))
                .closingTime(request.getClosingTime() != null ? parseTime(request.getClosingTime()) : java.time.LocalTime.of(23, 30))
                .build();

        if (request.getManagerId() != null) {
            Staff manager = staffRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên quản lý với ID: " + request.getManagerId()));
            cinema.setManager(manager);
        }

        Cinema savedCinema = cinemaRepository.save(cinema);
        return toResponse(savedCinema);
    }

    @Override
    @Transactional
    public CinemaResponse updateCinema(Integer id, CinemaRequest request) {
        com.devcine.backend.util.SecurityUtils.assertCinemaAccess(id);
        normalizeAndValidate(request, id);
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cụm rạp với ID: " + id));

        // Trạng thái CŨ (trước khi ghi đè)
        String oldStatus = cinema.getStatus();

        // Nếu cố gắng chuyển sang CLOSED: kiểm tra xem còn suất chiếu chưa kết thúc không
        if (request.getStatus() != null && "CLOSED".equalsIgnoreCase(request.getStatus()) && !"CLOSED".equalsIgnoreCase(oldStatus)) {
            LocalDateTime now = LocalDateTime.now();
            long activeShowtimes = showtimeRepository.countFutureShowtimesByCinema(id, now);
            if (activeShowtimes > 0) {
                throw new IllegalArgumentException("Không thể đóng cụm rạp! Cụm rạp vẫn còn " + activeShowtimes + " suất chiếu chưa kết thúc. Vui lòng dọn dẹp hoặc hủy toàn bộ lịch chiếu trước khi đóng cụm rạp.");
            }
        }

        cinema.setName(request.getName());
        cinema.setAddress(request.getAddress());
        cinema.setCity(request.getCity());
        cinema.setDistrict(request.getDistrict());
        cinema.setType(request.getType());
        cinema.setHotline(request.getHotline());
        if (request.getRooms() != null) cinema.setRooms(request.getRooms());
        // Trường mở rộng: chỉ ghi đè khi request có gửi (tránh form cũ làm mất dữ liệu)
        if (request.getDescription() != null) cinema.setDescription(request.getDescription());
        if (request.getLatitude() != null) cinema.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) cinema.setLongitude(request.getLongitude());
        if (request.getAmenities() != null) cinema.setAmenities(request.getAmenities());
        if (request.getStatus() != null) cinema.setStatus(request.getStatus());
        
        // Giờ hoạt động: chỉ ghi đè khi request có gửi (tránh form cũ làm mất dữ liệu).
        if (request.getOpeningTime() != null || request.getClosingTime() != null) {
            java.time.LocalTime newOpen = request.getOpeningTime() != null ? parseTime(request.getOpeningTime()) : cinema.getOpeningTime();
            java.time.LocalTime newClose = request.getClosingTime() != null ? parseTime(request.getClosingTime()) : cinema.getClosingTime();
            showtimeService.validateCinemaHoursUpdate(cinema, newOpen, newClose);
            cinema.setOpeningTime(newOpen);
            cinema.setClosingTime(newClose);
        }

        if (request.getManagerId() != null) {
            Staff manager = staffRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên quản lý với ID: " + request.getManagerId()));
            cinema.setManager(manager);
        } else {
            cinema.setManager(null);
        }

        Cinema updatedCinema = cinemaRepository.save(cinema);

        return toResponse(updatedCinema);
    }

    @Override
    @Transactional
    public CinemaResponse closeCinema(Integer id) {
        com.devcine.backend.util.SecurityUtils.assertCinemaAccess(id);
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cụm rạp với ID: " + id));

        if ("CLOSED".equalsIgnoreCase(cinema.getStatus())) {
            return toResponse(cinema);
        }

        LocalDateTime now = LocalDateTime.now();
        long activeShowtimes = showtimeRepository.countFutureShowtimesByCinema(id, now);
        if (activeShowtimes > 0) {
            throw new IllegalArgumentException("Không thể đóng cụm rạp! Cụm rạp vẫn còn " + activeShowtimes + " suất chiếu chưa kết thúc. Vui lòng dọn dẹp hoặc hủy toàn bộ lịch chiếu trước khi đóng cụm rạp.");
        }

        cinema.setStatus("CLOSED");
        Cinema saved = cinemaRepository.save(cinema);
        log.info("Cụm rạp #{} '{}' đã được đóng và ẩn khỏi client thành công.", id, cinema.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CinemaResponse reopenCinema(Integer id) {
        com.devcine.backend.util.SecurityUtils.assertCinemaAccess(id);
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cụm rạp với ID: " + id));

        cinema.setStatus("ACTIVE");
        Cinema saved = cinemaRepository.save(cinema);
        log.info("Cụm rạp #{} '{}' đã được mở lại và hiển thị cho client.", id, cinema.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCinema(Integer id) {
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cụm rạp với ID: " + id));
        cinemaRepository.delete(cinema);
    }
}
