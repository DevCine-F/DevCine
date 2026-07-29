package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.response.PriceBreakdown;
import com.devcine.backend.entity.Holiday;
import com.devcine.backend.entity.MovieFormat;
import com.devcine.backend.entity.PricingRule;
import com.devcine.backend.entity.SeatType;
import com.devcine.backend.repository.HolidayRepository;
import com.devcine.backend.repository.MovieFormatRepository;
import com.devcine.backend.repository.PricingRuleRepository;
import com.devcine.backend.repository.SeatTypeRepository;
import com.devcine.backend.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingRuleRepository pricingRuleRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final MovieFormatRepository movieFormatRepository;
    private final HolidayRepository holidayRepository;
    private final PricingService pricingService;

    // Mô hình 2 bậc: T2–T5 vs T6–CN & Lễ (gộp cao điểm).
    private static final List<Map<String, String>> DAY_TYPES = List.of(
            Map.of("code", "WEEKDAY", "label", "T2 - T5 (Ngày thường)"),
            Map.of("code", "WEEKEND", "label", "T6, T7, CN & Lễ (Cao điểm)"));

    // ============ Đọc toàn bộ cấu hình cho màn quản trị ============
    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("audiences", PricingService.audienceLabels());
        res.put("dayTypes", DAY_TYPES);

        List<Map<String, String>> roomTypes = new ArrayList<>();
        PricingService.roomTypeLabels().forEach((code, label) ->
                roomTypes.add(Map.of("code", code, "label", label)));
        res.put("roomTypes", roomTypes);

        List<Map<String, Object>> baseMatrix = new ArrayList<>();
        for (PricingRule r : pricingRuleRepository.findByRuleType(PricingService.RULE_BASE_PRICE)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("dayType", r.getDayType());
            m.put("roomType", r.getRoomType());
            m.put("audienceType", r.getAudienceType());
            m.put("value", r.getValue());
            baseMatrix.add(m);
        }
        res.put("baseMatrix", baseMatrix);

        // Loại ghế chỉ còn ý nghĩa HIỂN THỊ (tên + màu) — flat pricing bỏ phụ thu theo ghế.
        List<Map<String, Object>> seatTypes = new ArrayList<>();
        for (SeatType s : seatTypeRepository.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("colorCode", s.getColorCode());
            seatTypes.add(m);
        }
        res.put("seatTypes", seatTypes);

        List<Map<String, Object>> formats = new ArrayList<>();
        for (MovieFormat f : movieFormatRepository.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("name", f.getName());
            m.put("surcharge", f.getSurcharge());
            m.put("weekendSurcharge", f.getWeekendSurcharge());
            formats.add(m);
        }
        res.put("formats", formats);

        List<Map<String, Object>> holidays = new ArrayList<>();
        for (Holiday h : holidayRepository.findAllByOrderByHolidayDateAsc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("holidayDate", h.getHolidayDate().toString());
            m.put("name", h.getName());
            holidays.add(m);
        }
        res.put("holidays", holidays);

        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    // ============ Lưu ma trận giá nền (thay toàn bộ rule BASE_PRICE) ============
    @PutMapping("/base-matrix")
    @PreAuthorize("@perm.can('pricing','edit')")
    @Transactional
    public ResponseEntity<?> saveBaseMatrix(@RequestBody Map<String, Object> body) {
        try {
            pricingRuleRepository.deleteAll(pricingRuleRepository.findByRuleType(PricingService.RULE_BASE_PRICE));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rules = (List<Map<String, Object>>) body.get("rules");
            if (rules != null) {
                for (Map<String, Object> r : rules) {
                    PricingRule rule = PricingRule.builder()
                            .name("Giá nền")
                            .ruleType(PricingService.RULE_BASE_PRICE)
                            .dayType((String) r.get("dayType"))
                            .roomType((String) r.get("roomType"))
                            .timeSlot("ALL") // flat pricing: không phân giá theo khung giờ
                            .audienceType((String) r.get("audienceType"))
                            .value(new BigDecimal(r.get("value").toString()))
                            .priority(0)
                            .active(true)
                            .build();
                    pricingRuleRepository.save(rule);
                }
            }
            return ResponseEntity.ok(ApiResponse.success("Đã lưu bảng giá nền."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    // ============ Lưu loại ghế (chỉ tên + màu — flat pricing bỏ phụ thu ghế) ============
    @PutMapping("/seat-types")
    @PreAuthorize("@perm.can('pricing','edit')")
    @Transactional
    public ResponseEntity<?> saveSeatTypes(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items != null) {
                for (Map<String, Object> it : items) {
                    Integer id = Integer.parseInt(it.get("id").toString());
                    SeatType s = seatTypeRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy loại ghế"));
                    Object nm = it.get("name");
                    if (nm != null && !nm.toString().isBlank()) s.setName(nm.toString().trim());
                    Object cc = it.get("colorCode");
                    if (cc != null && !cc.toString().isBlank()) s.setColorCode(cc.toString().trim());
                    seatTypeRepository.save(s);
                }
            }
            return ResponseEntity.ok(ApiResponse.success("Đã lưu loại ghế."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    // ============ Lưu phụ thu định dạng (2D/3D theo ngày) ============
    @PutMapping("/formats")
    @PreAuthorize("@perm.can('pricing','edit')")
    @Transactional
    public ResponseEntity<?> saveFormats(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items != null) {
                for (Map<String, Object> it : items) {
                    Integer id = Integer.parseInt(it.get("id").toString());
                    MovieFormat f = movieFormatRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy định dạng"));
                    if (it.get("surcharge") != null) f.setSurcharge(new BigDecimal(it.get("surcharge").toString()));
                    if (it.containsKey("weekendSurcharge")) {
                        f.setWeekendSurcharge(it.get("weekendSurcharge") != null
                                ? new BigDecimal(it.get("weekendSurcharge").toString()) : null);
                    }
                    movieFormatRepository.save(f);
                }
            }
            return ResponseEntity.ok(ApiResponse.success("Đã lưu cấu hình định dạng."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    // ============ Ngày lễ ============
    @PostMapping("/holidays")
    @PreAuthorize("@perm.can('pricing','edit')")
    public ResponseEntity<?> addHoliday(@RequestBody Map<String, Object> body) {
        try {
            LocalDate date = LocalDate.parse(body.get("holidayDate").toString());
            if (holidayRepository.existsByHolidayDate(date)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Ngày lễ đã tồn tại"));
            }
            Holiday h = holidayRepository.save(Holiday.builder()
                    .holidayDate(date)
                    .name(body.getOrDefault("name", "Ngày lễ").toString())
                    .build());
            return ResponseEntity.status(201).body(ApiResponse.ok(Map.of("id", h.getId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/holidays/{id}")
    @PreAuthorize("@perm.can('pricing','edit')")
    public ResponseEntity<?> deleteHoliday(@PathVariable Integer id) {
        holidayRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xoá ngày lễ."));
    }

    // ============ Bộ tính thử (Simulator) ============
    @PostMapping("/simulate")
    @PreAuthorize("@perm.can('pricing','edit')")
    public ResponseEntity<?> simulate(@RequestBody Map<String, Object> body) {
        try {
            MovieFormat fmt = body.get("formatId") != null
                    ? movieFormatRepository.findById(Integer.parseInt(body.get("formatId").toString())).orElse(null)
                    : null;
            PriceBreakdown bd = pricingService.simulate(
                    (String) body.get("dayType"),
                    (String) body.get("audienceType"),
                    (String) body.get("roomType"),
                    fmt);
            return ResponseEntity.ok(ApiResponse.ok(bd));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
