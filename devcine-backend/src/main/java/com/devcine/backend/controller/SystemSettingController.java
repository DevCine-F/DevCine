package com.devcine.backend.controller;

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
    public ResponseEntity<List<SystemSettingResponseDTO>> getAllSettings() {
        return ResponseEntity.ok(systemSettingService.getAllSettings());
    }

    @GetMapping("/{key}")
    public ResponseEntity<SystemSettingResponseDTO> getSettingByKey(@PathVariable String key) {
        SystemSettingResponseDTO dto = systemSettingService.getSettingByKey(key);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("@perm.can('settings', 'edit')")
    public ResponseEntity<SystemSettingResponseDTO> saveOrUpdateSetting(@RequestBody SystemSettingRequestDTO dto) {
        return ResponseEntity.ok(systemSettingService.saveOrUpdateSetting(dto));
    }
}
