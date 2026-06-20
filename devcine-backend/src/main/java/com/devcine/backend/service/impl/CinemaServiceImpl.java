package com.devcine.backend.service.impl;

import com.devcine.backend.dto.request.CinemaRequest;
import com.devcine.backend.dto.response.CinemaResponse;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.RoomRepository;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.service.CinemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final StaffRepository staffRepository;
    private final RoomRepository roomRepository;

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
        Cinema cinema = Cinema.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
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
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cụm rạp với ID: " + id));

        cinema.setName(request.getName());
        cinema.setAddress(request.getAddress());
        cinema.setCity(request.getCity());
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
