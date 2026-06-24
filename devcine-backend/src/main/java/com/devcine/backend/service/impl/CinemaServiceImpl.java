package com.devcine.backend.service.impl;

import com.devcine.backend.dto.request.CinemaRequest;
import com.devcine.backend.dto.response.CinemaResponse;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.RoomRepository;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.service.CinemaService;
import com.devcine.backend.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final StaffRepository staffRepository;
    private final RoomRepository roomRepository;
    private final LocationService locationService;

    // Danh mục hợp lệ — đồng bộ với dropdown phía Frontend (chống can thiệp giá trị lạ qua API)
    private static final Set<String> ALLOWED_TYPES = Set.of("Standard", "Premium/IMAX", "Sweetbox", "Gold Class");
    private static final Set<String> ALLOWED_STATUS = Set.of("ACTIVE", "MAINTENANCE", "CLOSED");

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
        return cinemaRepository.findAllWithManager().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaResponse getCinemaById(Integer id) {
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cụm rạp với ID: " + id));
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
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .amenities(request.getAmenities())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
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
        normalizeAndValidate(request, id);
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cụm rạp với ID: " + id));

        cinema.setName(request.getName());
        cinema.setAddress(request.getAddress());
        cinema.setCity(request.getCity());
        cinema.setDistrict(request.getDistrict());
        cinema.setType(request.getType());
        cinema.setHotline(request.getHotline());
        if (request.getRooms() != null) cinema.setRooms(request.getRooms());
        // Trường mở rộng: chỉ ghi đè khi request có gửi (tránh form cũ làm mất dữ liệu)
        if (request.getImageUrl() != null) cinema.setImageUrl(request.getImageUrl());
        if (request.getDescription() != null) cinema.setDescription(request.getDescription());
        if (request.getLatitude() != null) cinema.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) cinema.setLongitude(request.getLongitude());
        if (request.getAmenities() != null) cinema.setAmenities(request.getAmenities());
        if (request.getStatus() != null) cinema.setStatus(request.getStatus());

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
    public void deleteCinema(Integer id) {
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cụm rạp với ID: " + id));
        cinemaRepository.delete(cinema);
    }
}
