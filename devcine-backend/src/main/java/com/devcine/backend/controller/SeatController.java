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

    // channel=ONLINE (mặc định) → chỉ ADULT/U22; channel=POS → đủ 4 loại vé (bán tại quầy).
    @GetMapping("/showtime/{showtimeId}")
    public ResponseEntity<ApiResponse<ShowtimeSeatResponse>> getSeatsForShowtime(
            @PathVariable Integer showtimeId,
            @RequestParam(name = "channel", defaultValue = "ONLINE") String channel) {
        return ResponseEntity.ok(ApiResponse.ok(seatService.getSeatsForShowtime(showtimeId, channel)));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<ShowtimeSeatResponse>> getSeatsForRoom(@PathVariable Integer roomId) {
        return ResponseEntity.ok(ApiResponse.ok(seatService.getSeatsForRoom(roomId)));
    }

    @PostMapping("/layout/{roomId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @perm.can('cinemas', 'edit')")
    public ResponseEntity<ApiResponse<Void>> saveSeatLayout(@PathVariable Integer roomId, @RequestBody SeatLayoutRequest request) {
        seatService.saveSeatLayout(roomId, request);
        return ResponseEntity.ok(ApiResponse.success("Đã lưu sơ đồ ghế."));
    }

    /**
     * Dọn hàng loạt toàn hệ thống (chạy 1 lần, chỉ ADMIN): cắt ghế "ma" ngoài khung của mọi phòng,
     * chuẩn hóa status rác, và dựng lại snapshot cho suất chưa bán. Sửa triệt để lỗi lệch sơ đồ.
     */
    @PostMapping("/cleanup-all")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Integer>>> cleanupAll() {
        return ResponseEntity.ok(ApiResponse.ok(seatService.cleanupAllRooms()));
    }
}
