package com.devcine.backend.controller;

import com.devcine.backend.dto.SystemSettingDTO;
import com.devcine.backend.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<List<SystemSettingDTO>> getAllSettings() {
        return ResponseEntity.ok(systemSettingService.getAllSettings());
    }

    @GetMapping("/{key}")
    public ResponseEntity<SystemSettingDTO> getSettingByKey(@PathVariable String key) {
        SystemSettingDTO dto = systemSettingService.getSettingByKey(key);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<SystemSettingDTO> saveOrUpdateSetting(@RequestBody SystemSettingDTO dto) {
        return ResponseEntity.ok(systemSettingService.saveOrUpdateSetting(dto));
    }
}
