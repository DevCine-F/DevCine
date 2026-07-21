package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<List<String>>> getProvinces() {
        return ResponseEntity.ok(ApiResponse.ok(locationService.getProvinces()));
    }

    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<String>>> getDistricts(@RequestParam String province) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.getDistricts(province)));
    }
}
