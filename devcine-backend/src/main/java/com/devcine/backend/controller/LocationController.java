package com.devcine.backend.controller;

import com.devcine.backend.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API danh mục hành chính (công khai, chỉ đọc) phục vụ dropdown Tỉnh/Thành & Quận/Huyện.
 */
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/provinces")
    public ResponseEntity<List<String>> getProvinces() {
        return ResponseEntity.ok(locationService.getProvinces());
    }

    @GetMapping("/districts")
    public ResponseEntity<List<String>> getDistricts(@RequestParam String province) {
        return ResponseEntity.ok(locationService.getDistricts(province));
    }
}
