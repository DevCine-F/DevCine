package com.devcine.backend.controller;

import com.devcine.backend.dto.request.RoomRequest;
import com.devcine.backend.dto.response.RoomResponse;
import com.devcine.backend.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/cinema/{cinemaId}")
    public ResponseEntity<List<RoomResponse>> getRoomsByCinema(@PathVariable Integer cinemaId) {
        return ResponseEntity.ok(roomService.getRoomsByCinema(cinemaId));
    }

    @PostMapping("/cinema/{cinemaId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> createRoom(@PathVariable Integer cinemaId,
                                                   @Valid @RequestBody RoomRequest request) {
        return new ResponseEntity<>(roomService.createRoom(cinemaId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Integer id,
                                                   @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable Integer id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    // Lỗi nghiệp vụ (trùng tên, có suất chiếu, enum sai) -> 400 kèm message tiếng Việt
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
