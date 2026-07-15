package com.devcine.backend.controller;

import com.devcine.backend.dto.response.PriceBreakdown;
import com.devcine.backend.entity.Holiday;
import com.devcine.backend.entity.MovieFormat;
import com.devcine.backend.entity.PricingRule;
import com.devcine.backend.entity.SeatType;
import com.devcine.backend.entity.SpecialSeatPrice;
import com.devcine.backend.repository.HolidayRepository;
import com.devcine.backend.repository.MovieFormatRepository;
import com.devcine.backend.repository.PricingRuleRepository;
import com.devcine.backend.repository.SeatTypeRepository;
import com.devcine.backend.repository.SpecialSeatPriceRepository;
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
@CrossOrigin(origins = "*")
public class PricingController {

    private final PricingRuleRepository pricingRuleRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final MovieFormatRepository movieFormatRepository;
    private final SpecialSeatPriceRepository specialSeatPriceRepository;
    private final HolidayRepository holidayRepository;
    private final PricingService pricingService;

    private static final List<Map<String, String>> DAY_TYPES = List.of(
            Map.of("code", "WEEKDAY", "label", "T2 - T5"),
            Map.of("code", "WEEKEND", "label", "T6, T7, CN"),
            Map.of("code", "HOLIDAY", "label", "Ngày lễ"));

    // ============ Đọc toàn bộ cấu hình cho màn quản trị ============
    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("audiences", PricingService.audienceLabels());
        res.put("dayTypes", DAY_TYPES);

        List<Map<String, Object>> baseMatrix = new ArrayList<>();
        for (PricingRule r : pricingRuleRepository.findByRuleType(PricingService.RULE_BASE_PRICE)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("dayType", r.getDayType());
            m.put("timeSlot", r.getTimeSlot());
            m.put("audienceType", r.getAudienceType());
            m.put("value", r.getValue());
            baseMatrix.add(m);
        }
        res.put("baseMatrix", baseMatrix);

        List<Map<String, Object>> seatTypes = new ArrayList<>();
        for (SeatType s : seatTypeRepository.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("priceModifier", s.getPriceModifier());
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
            m.put("isFixedPrice", Boolean.TRUE.equals(f.getIsFixedPrice()));
            formats.add(m);
        }
        res.put("formats", formats);

        List<Map<String, Object>> specials = new ArrayList<>();
        for (SpecialSeatPrice sp : specialSeatPriceRepository.findAllWithRefs()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", sp.getId());
            m.put("formatId", sp.getFormat().getId());
            m.put("seatTypeId", sp.getSeatType().getId());
            m.put("price", sp.getPrice());
            specials.add(m);
        }
        res.put("specialPrices", specials);

        List<Map<String, Object>> holidays = new ArrayList<>();
        for (Holiday h : holidayRepository.findAllByOrderByHolidayDateAsc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("holidayDate", h.getHolidayDate().toString());
            m.put("name", h.getName());
            holidays.add(m);
        }
        res.put("holidays", holidays);

        return ResponseEntity.ok(res);
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
                            .timeSlot((String) r.get("timeSlot"))
                            .audienceType((String) r.get("audienceType"))
                            .value(new BigDecimal(r.get("value").toString()))
                            .priority(0)
                            .active(true)
                            .build();
                    pricingRuleRepository.save(rule);
                }
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ============ Lưu phụ thu loại ghế ============
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
                    // Phụ thu loại ghế (số tiền cộng thêm vào giá nền); chỉ ghi khi có gửi.
                    if (it.get("priceModifier") != null) {
                        s.setPriceModifier(new BigDecimal(it.get("priceModifier").toString()));
                    }
                    // Cho phép sửa tên + màu ngay tại panel cấu hình (chỉ ghi khi có gửi & không rỗng).
                    Object nm = it.get("name");
                    if (nm != null && !nm.toString().isBlank()) s.setName(nm.toString().trim());
                    Object cc = it.get("colorCode");
                    if (cc != null && !cc.toString().isBlank()) s.setColorCode(cc.toString().trim());
                    seatTypeRepository.save(s);
                }
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ============ Lưu cấu hình định dạng (phụ thu + giá cố định) ============
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
                    if (it.get("isFixedPrice") != null) f.setIsFixedPrice(Boolean.parseBoolean(it.get("isFixedPrice").toString()));
                    movieFormatRepository.save(f);
                }
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ============ Lưu giá cố định phòng đặc biệt (thay toàn bộ) ============
    @PutMapping("/special-prices")
    @PreAuthorize("@perm.can('pricing','edit')")
    @Transactional
    public ResponseEntity<?> saveSpecialPrices(@RequestBody Map<String, Object> body) {
        try {
            specialSeatPriceRepository.deleteAll();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items != null) {
                for (Map<String, Object> it : items) {
                    MovieFormat f = movieFormatRepository.findById(Integer.parseInt(it.get("formatId").toString()))
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy định dạng"));
                    SeatType s = seatTypeRepository.findById(Integer.parseInt(it.get("seatTypeId").toString()))
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy loại ghế"));
                    specialSeatPriceRepository.save(SpecialSeatPrice.builder()
                            .format(f).seatType(s)
                            .price(new BigDecimal(it.get("price").toString()))
                            .build());
                }
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ============ Ngày lễ ============
    @PostMapping("/holidays")
    @PreAuthorize("@perm.can('pricing','edit')")
    public ResponseEntity<?> addHoliday(@RequestBody Map<String, Object> body) {
        try {
            LocalDate date = LocalDate.parse(body.get("holidayDate").toString());
            if (holidayRepository.existsByHolidayDate(date)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Ngày lễ đã tồn tại"));
            }
            Holiday h = holidayRepository.save(Holiday.builder()
                    .holidayDate(date)
                    .name(body.getOrDefault("name", "Ngày lễ").toString())
                    .build());
            return ResponseEntity.status(201).body(Map.of("success", true, "id", h.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/holidays/{id}")
    @PreAuthorize("@perm.can('pricing','edit')")
    public ResponseEntity<?> deleteHoliday(@PathVariable Integer id) {
        holidayRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ============ Bộ tính thử (Simulator) ============
    @PostMapping("/simulate")
    @PreAuthorize("@perm.can('pricing','edit')")
    public ResponseEntity<?> simulate(@RequestBody Map<String, Object> body) {
        try {
            SeatType seatType = seatTypeRepository.findById(Integer.parseInt(body.get("seatTypeId").toString()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại ghế"));
            MovieFormat fmt = body.get("formatId") != null
                    ? movieFormatRepository.findById(Integer.parseInt(body.get("formatId").toString())).orElse(null)
                    : null;
            PriceBreakdown bd = pricingService.simulate(
                    (String) body.get("dayType"),
                    (String) body.get("timeSlot"),
                    (String) body.get("audienceType"),
                    seatType, fmt);
            return ResponseEntity.ok(bd);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
