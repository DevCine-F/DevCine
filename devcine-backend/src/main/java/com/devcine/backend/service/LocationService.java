package com.devcine.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Cung cấp danh mục Tỉnh/Thành & Quận/Huyện Việt Nam (đọc 1 lần từ resource vn-locations.json).
 * Dùng cho dropdown động phía Frontend + validate đồng bộ ở Backend.
 */
@Service
@RequiredArgsConstructor
public class LocationService {

    private final ObjectMapper objectMapper;
    private List<ProvinceEntry> data = List.of();

    /** Bản ghi 1 tỉnh và danh sách quận/huyện. */
    public static class ProvinceEntry {
        public String province;
        public List<String> districts;
    }

    @PostConstruct
    void load() {
        try (InputStream is = new ClassPathResource("vn-locations.json").getInputStream()) {
            data = Arrays.asList(objectMapper.readValue(is, ProvinceEntry[].class));
        } catch (Exception ex) {
            throw new IllegalStateException("Không nạp được dữ liệu Tỉnh/Quận (vn-locations.json)", ex);
        }
    }

    public List<String> getProvinces() {
        return data.stream().map(e -> e.province).toList();
    }

    public List<String> getDistricts(String province) {
        if (province == null) return List.of();
        String p = province.trim();
        return data.stream()
                .filter(e -> e.province.equalsIgnoreCase(p))
                .findFirst()
                .map(e -> e.districts)
                .orElse(List.of());
    }

    public boolean isValidProvince(String province) {
        return province != null && getProvinces().stream().anyMatch(p -> p.equalsIgnoreCase(province.trim()));
    }

    public boolean isValidDistrict(String province, String district) {
        return district != null && getDistricts(province).stream().anyMatch(d -> d.equalsIgnoreCase(district.trim()));
    }
}
