package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.request.SystemSettingRequestDTO;
import com.devcine.backend.dto.response.SystemSettingResponseDTO;
import com.devcine.backend.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemSettingResponseDTO>>> getAllSettings() {
        return ResponseEntity.ok(ApiResponse.ok(systemSettingService.getAllSettings()));
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> getSettingByKey(@PathVariable String key) {
        SystemSettingResponseDTO dto = systemSettingService.getSettingByKey(key);
        if (dto == null) {
            return ResponseEntity.status(404).body(ApiResponse.fail("Không tìm thấy cài đặt."));
        }
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping
    @PreAuthorize("@perm.can('settings', 'edit')")
    public ResponseEntity<ApiResponse<SystemSettingResponseDTO>> saveOrUpdateSetting(@RequestBody SystemSettingRequestDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(systemSettingService.saveOrUpdateSetting(dto)));
    }
}
