package com.devcine.backend.service;

import com.devcine.backend.dto.request.SystemSettingRequestDTO;
import com.devcine.backend.dto.response.SystemSettingResponseDTO;
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

    /** Key cấu hình thời gian giữ ghế (phút) trong SystemSetting. */
    public static final String KEY_SEAT_HOLD_MINUTES = "SEAT_HOLD_MINUTES";
    public static final int SEAT_HOLD_MIN = 3;
    public static final int SEAT_HOLD_MAX = 30;
    public static final int SEAT_HOLD_DEFAULT = 10;

    /** Số vé tối đa cho một lần đặt (chống phe vé). */
    public static final String KEY_MAX_TICKETS = "MAX_TICKETS_PER_BOOKING";
    public static final int MAX_TICKETS_MIN = 1;
    public static final int MAX_TICKETS_MAX = 20;
    public static final int MAX_TICKETS_DEFAULT = 8;

    /** Số phút sau khi suất bắt đầu vẫn còn cho phép mua vé. */
    public static final String KEY_BOOKING_LATE_MINUTES = "BOOKING_LATE_MINUTES";
    public static final int BOOKING_LATE_MIN = 0;
    public static final int BOOKING_LATE_MAX = 60;
    public static final int BOOKING_LATE_DEFAULT = 15;

    /** Quỹ đầu ca (tiền mặt sẵn trong két đầu ca) dùng cho đối soát cuối ca. */
    public static final String KEY_SHIFT_OPENING_FLOAT = "SHIFT_OPENING_FLOAT";
    public static final long SHIFT_OPENING_FLOAT_DEFAULT = 2_000_000L;

    /** Đọc một setting số nguyên, kẹp trong [min, max]; thiếu/sai → defaultValue. */
    private int getIntSetting(String key, int min, int max, int defaultValue) {
        return systemSettingRepository.findById(key)
                .map(s -> {
                    try {
                        int v = Integer.parseInt(s.getSettingValue().trim());
                        return Math.min(max, Math.max(min, v));
                    } catch (NumberFormatException | NullPointerException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /** Số vé tối đa/lần đặt (đã kẹp [1, 20], mặc định 8). */
    public int getMaxTicketsPerBooking() {
        return getIntSetting(KEY_MAX_TICKETS, MAX_TICKETS_MIN, MAX_TICKETS_MAX, MAX_TICKETS_DEFAULT);
    }

    /** Số phút còn bán vé sau giờ chiếu (đã kẹp [0, 60], mặc định 15). */
    public int getBookingLateMinutes() {
        return getIntSetting(KEY_BOOKING_LATE_MINUTES, BOOKING_LATE_MIN, BOOKING_LATE_MAX, BOOKING_LATE_DEFAULT);
    }

    /**
     * Thời gian giữ ghế (phút) admin cấu hình, đã kẹp trong [3, 30]; thiếu/sai → mặc định 10.
     * Là nguồn DUY NHẤT cho cả luồng giữ ghế (BookingService) lẫn job dọn ghế quá hạn.
     */
    public int getSeatHoldMinutes() {
        return getIntSetting(KEY_SEAT_HOLD_MINUTES, SEAT_HOLD_MIN, SEAT_HOLD_MAX, SEAT_HOLD_DEFAULT);
    }

    /** Quỹ đầu ca (VNĐ) admin cấu hình; thiếu/sai/âm → mặc định 2.000.000. Không kẹp trần. */
    public java.math.BigDecimal getShiftOpeningFloat() {
        return systemSettingRepository.findById(KEY_SHIFT_OPENING_FLOAT)
                .map(s -> {
                    try {
                        java.math.BigDecimal v = new java.math.BigDecimal(s.getSettingValue().trim());
                        return v.signum() < 0 ? java.math.BigDecimal.ZERO : v;
                    } catch (NumberFormatException | NullPointerException e) {
                        return java.math.BigDecimal.valueOf(SHIFT_OPENING_FLOAT_DEFAULT);
                    }
                })
                .orElse(java.math.BigDecimal.valueOf(SHIFT_OPENING_FLOAT_DEFAULT));
    }

    public List<SystemSettingResponseDTO> getAllSettings() {
        return systemSettingRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public SystemSettingResponseDTO getSettingByKey(String key) {
        return systemSettingRepository.findById(key)
                .map(this::mapToDTO)
                .orElse(null);
    }

    public SystemSettingResponseDTO saveOrUpdateSetting(SystemSettingRequestDTO dto) {
        SystemSetting setting = systemSettingRepository.findById(dto.getSettingKey())
                .orElse(SystemSetting.builder().settingKey(dto.getSettingKey()).build());
        setting.setSettingValue(dto.getSettingValue());
        
        SystemSetting saved = systemSettingRepository.save(setting);
        return mapToDTO(saved);
    }

    private SystemSettingResponseDTO mapToDTO(SystemSetting setting) {
        return SystemSettingResponseDTO.builder()
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .build();
    }
}
