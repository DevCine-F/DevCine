package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.response.SeatDTO;
import com.devcine.backend.dto.response.ShowtimeSeatResponse;
import com.devcine.backend.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.devcine.backend.dto.request.SeatLayoutRequest;
import com.devcine.backend.repository.SeatTypeRepository;
import com.devcine.backend.entity.SeatType;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;
    private final SeatTypeRepository seatTypeRepository;

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<SeatType>>> getAllSeatTypes() {
        return ResponseEntity.ok(ApiResponse.ok(seatTypeRepository.findAll()));
    }

    @GetMapping("/showtime/{showtimeId}")
    public ResponseEntity<ApiResponse<ShowtimeSeatResponse>> getSeatsForShowtime(@PathVariable Integer showtimeId) {
        return ResponseEntity.ok(ApiResponse.ok(seatService.getSeatsForShowtime(showtimeId)));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<ShowtimeSeatResponse>> getSeatsForRoom(@PathVariable Integer roomId) {
        return ResponseEntity.ok(ApiResponse.ok(seatService.getSeatsForRoom(roomId)));
    }

    @PostMapping("/layout/{roomId}")
    public ResponseEntity<ApiResponse<Void>> saveSeatLayout(@PathVariable Integer roomId, @RequestBody SeatLayoutRequest request) {
        seatService.saveSeatLayout(roomId, request);
        return ResponseEntity.ok(ApiResponse.success("Đã lưu sơ đồ ghế."));
    }
}
