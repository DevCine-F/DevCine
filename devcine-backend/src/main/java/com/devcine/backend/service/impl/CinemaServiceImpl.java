package com.devcine.backend.service.impl;

import com.devcine.backend.dto.request.CinemaRequest;
import com.devcine.backend.dto.response.CinemaResponse;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.repository.CinemaRepository;
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

    @Override
    @Transactional(readOnly = true)
    public List<CinemaResponse> getAllCinemas() {
        return cinemaRepository.findAll().stream()
                .map(CinemaResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaResponse getCinemaById(Integer id) {
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cụm rạp với ID: " + id));
        return CinemaResponse.fromEntity(cinema);
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
                .build();

        if (request.getManagerId() != null) {
            Staff manager = staffRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên quản lý với ID: " + request.getManagerId()));
            cinema.setManager(manager);
        }

        Cinema savedCinema = cinemaRepository.save(cinema);
        return CinemaResponse.fromEntity(savedCinema);
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
        cinema.setRooms(request.getRooms());

        if (request.getManagerId() != null) {
            Staff manager = staffRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên quản lý với ID: " + request.getManagerId()));
            cinema.setManager(manager);
        } else {
            cinema.setManager(null);
        }

        Cinema updatedCinema = cinemaRepository.save(cinema);
        return CinemaResponse.fromEntity(updatedCinema);
    }

    @Override
    @Transactional
    public void deleteCinema(Integer id) {
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cụm rạp với ID: " + id));
        cinemaRepository.delete(cinema);
    }
}
