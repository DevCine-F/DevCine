package com.devcine.backend.service;

import com.devcine.backend.dto.SystemSettingDTO;
import com.devcine.backend.entity.SystemSetting;
import com.devcine.backend.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public List<SystemSettingDTO> getAllSettings() {
        return systemSettingRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public SystemSettingDTO getSettingByKey(String key) {
        return systemSettingRepository.findById(key)
                .map(this::mapToDTO)
                .orElse(null);
    }

    public SystemSettingDTO saveOrUpdateSetting(SystemSettingDTO dto) {
        SystemSetting setting = systemSettingRepository.findById(dto.getSettingKey())
                .orElse(SystemSetting.builder().settingKey(dto.getSettingKey()).build());
        setting.setSettingValue(dto.getSettingValue());
        
        SystemSetting saved = systemSettingRepository.save(setting);
        return mapToDTO(saved);
    }

    private SystemSettingDTO mapToDTO(SystemSetting setting) {
        return SystemSettingDTO.builder()
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .build();
    }
}
