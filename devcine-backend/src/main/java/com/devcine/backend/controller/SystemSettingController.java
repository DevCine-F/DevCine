package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.request.SystemSettingRequestDTO;
import com.devcine.backend.dto.response.SystemSettingResponseDTO;
import com.devcine.backend.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;
    private final SimpMessagingTemplate messagingTemplate;

    private void notifySettingsUpdate(String key, String value) {
        try {
            if (messagingTemplate != null) {
                Object payload = Map.of(
                        "action", "SETTINGS_UPDATED",
                        "key", key != null ? key : "",
                        "value", value != null ? value : "",
                        "timestamp", System.currentTimeMillis()
                );
                messagingTemplate.convertAndSend("/topic/settings-updates", payload);
            }
        } catch (Exception e) {
            // best-effort notification
        }
    }

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
        SystemSettingResponseDTO res = systemSettingService.saveOrUpdateSetting(dto);
        notifySettingsUpdate(dto.getSettingKey(), dto.getSettingValue());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}
