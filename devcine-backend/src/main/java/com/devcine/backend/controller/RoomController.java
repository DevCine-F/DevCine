package com.devcine.backend.controller;

import com.devcine.backend.entity.Room;
import com.devcine.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // For development
public class RoomController {

    private final RoomRepository roomRepository;

    @GetMapping("/cinema/{cinemaId}")
    public ResponseEntity<List<Room>> getRoomsByCinema(@PathVariable Integer cinemaId) {
        return ResponseEntity.ok(roomRepository.findByCinemaId(cinemaId));
    }
}
