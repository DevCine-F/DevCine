package com.devcine.backend.service;

import com.devcine.backend.dto.response.PriceBreakdown;
import com.devcine.backend.entity.MovieFormat;
import com.devcine.backend.entity.PricingRule;
import com.devcine.backend.entity.Seat;
import com.devcine.backend.entity.SeatType;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.entity.SpecialSeatPrice;
import com.devcine.backend.repository.HolidayRepository;
import com.devcine.backend.repository.PricingRuleRepository;
import com.devcine.backend.repository.SpecialSeatPriceRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nguồn sự thật DUY NHẤT cho giá vé. Công thức cộng dồn:
 *   giá = giá_nền(ngày × khung giờ × đối tượng) + phụ_thu_loại_ghế + phụ_thu_định_dạng
 * Định dạng/phòng đặc biệt (isFixedPrice) → giá cố định (SpecialSeatPrice) ghi đè công thức.
 * Dùng chung cho seat-map (SeatService) và giữ ghế (BookingService) — không tính giá ở nơi khác.
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRuleRepository pricingRuleRepository;
    private final HolidayRepository holidayRepository;
    private final SpecialSeatPriceRepository specialSeatPriceRepository;

    public static final List<String> AUDIENCE_TYPES = List.of("ADULT", "STUDENT", "CHILD", "SENIOR");
    public static final String RULE_BASE_PRICE = "BASE_PRICE";
    private static final BigDecimal DEFAULT_BASE = new BigDecimal("75000");
    private static final LocalTime SLOT_EARLY_END = LocalTime.of(10, 0);
    private static final LocalTime SLOT_DAY_END = LocalTime.of(17, 0);

    /** Mã đối tượng -> nhãn hiển thị (giữ thứ tự). */
    public static Map<String, String> audienceLabels() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("ADULT", "Người lớn");
        m.put("STUDENT", "HSSV / U22");
        m.put("CHILD", "Trẻ em");
        m.put("SENIOR", "Người cao tuổi");
        return m;
    }

    public String normalizeAudience(String a) {
        if (a == null) return "ADULT";
        String up = a.trim().toUpperCase();
        return AUDIENCE_TYPES.contains(up) ? up : "ADULT";
    }

    /** Mô hình Lotte: T2–T5 = WEEKDAY · T6,7,CN = WEEKEND · ngày lễ = HOLIDAY. */
    public String resolveDayType(LocalDate date) {
        if (holidayRepository.existsByHolidayDate(date)) return "HOLIDAY";
        DayOfWeek d = date.getDayOfWeek();
        if (d == DayOfWeek.FRIDAY || d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) return "WEEKEND";
        return "WEEKDAY";
    }

    /** Phụ thu định dạng phụ thuộc ngày: T2–T5 dùng surcharge; cuối tuần/lễ dùng weekendSurcharge (fallback surcharge). */
    public BigDecimal resolveFormatSurcharge(MovieFormat fmt, String dayType) {
        if (fmt == null) return BigDecimal.ZERO;
        if ("WEEKDAY".equals(dayType)) return nz(fmt.getSurcharge());
        return fmt.getWeekendSurcharge() != null ? fmt.getWeekendSurcharge() : nz(fmt.getSurcharge());
    }

    public String resolveTimeSlot(LocalTime t) {
        if (t.isBefore(SLOT_EARLY_END)) return "EARLY";
        if (t.isBefore(SLOT_DAY_END)) return "BEFORE_17H";
        return "AFTER_17H";
    }

    /** Nạp toàn bộ ngữ cảnh giá của 1 suất MỘT LẦN (tránh N+1 khi tính nhiều ghế). */
    public PricingContext buildContext(Showtime st) {
        LocalDateTime start = st.getStartTime();
        String dayType = resolveDayType(start.toLocalDate());
        String timeSlot = resolveTimeSlot(start.toLocalTime());
        List<PricingRule> rules = pricingRuleRepository.findByRuleTypeAndActiveTrue(RULE_BASE_PRICE);

        MovieFormat fmt = st.getFormat();
        boolean fixed = fmt != null && Boolean.TRUE.equals(fmt.getIsFixedPrice());
        Map<Integer, BigDecimal> specialBySeatType = new HashMap<>();
        if (fixed) {
            for (SpecialSeatPrice sp : specialSeatPriceRepository.findAllWithRefs()) {
                if (sp.getFormat().getId().equals(fmt.getId())) {
                    specialBySeatType.put(sp.getSeatType().getId(), sp.getPrice());
                }
            }
        }

        Map<String, BigDecimal> baseByAudience = new LinkedHashMap<>();
        for (String aud : AUDIENCE_TYPES) {
            baseByAudience.put(aud, resolveBase(rules, dayType, timeSlot, aud));
        }
        BigDecimal fmtSurcharge = resolveFormatSurcharge(fmt, dayType);
        return new PricingContext(dayType, timeSlot, fmt, fixed, fmtSurcharge, baseByAudience, specialBySeatType);
    }

    private BigDecimal resolveBase(List<PricingRule> rules, String dayType, String timeSlot, String audience) {
        PricingRule best = bestRule(rules, dayType, timeSlot, audience);
        if (best == null && "HOLIDAY".equals(dayType)) {
            best = bestRule(rules, "WEEKEND", timeSlot, audience); // lễ chưa cấu hình → dùng cuối tuần
        }
        return best != null ? best.getValue() : DEFAULT_BASE;
    }

    /** Chọn rule khớp nhất: ưu tiên khớp chính xác hơn ALL, rồi đến priority. */
    private PricingRule bestRule(List<PricingRule> rules, String dayType, String timeSlot, String audience) {
        PricingRule best = null;
        int bestScore = Integer.MIN_VALUE;
        for (PricingRule r : rules) {
            Integer sDay = matchScore(r.getDayType(), dayType);
            Integer sSlot = matchScore(r.getTimeSlot(), timeSlot);
            Integer sAud = matchScore(r.getAudienceType(), audience);
            if (sDay == null || sSlot == null || sAud == null) continue; // có chiều không khớp
            int total = (sDay + sSlot + sAud) * 100 + (r.getPriority() != null ? r.getPriority() : 0);
            if (total > bestScore) {
                bestScore = total;
                best = r;
            }
        }
        return best;
    }

    /** 2 = khớp chính xác · 1 = wildcard (null/ALL) · null = lệch. */
    private Integer matchScore(String ruleVal, String actual) {
        if (ruleVal == null || ruleVal.isBlank() || "ALL".equalsIgnoreCase(ruleVal)) return 1;
        return ruleVal.equalsIgnoreCase(actual) ? 2 : null;
    }

    /** Giá 1 ghế theo loại ghế + đối tượng trong ngữ cảnh đã nạp. */
    public BigDecimal priceFor(PricingContext ctx, SeatType seatType, String audience) {
        if (ctx.fixed) {
            BigDecimal sp = ctx.specialBySeatType.get(seatType.getId());
            if (sp != null) return sp;
            return ctx.format != null && ctx.format.getSurcharge() != null ? ctx.format.getSurcharge() : DEFAULT_BASE;
        }
        BigDecimal base = ctx.baseByAudience.getOrDefault(audience, DEFAULT_BASE);
        BigDecimal seatSur = nz(seatType.getPriceModifier());
        return base.add(seatSur).add(nz(ctx.formatSurcharge)).max(BigDecimal.ZERO);
    }

    public PriceBreakdown breakdown(PricingContext ctx, Seat seat, String audience) {
        SeatType seatType = seat.getSeatType();
        BigDecimal total = priceFor(ctx, seatType, audience);
        if (ctx.fixed) {
            return PriceBreakdown.builder()
                    .seatId(seat.getId()).seatType(seatType.getName()).ticketType(audience)
                    .basePrice(total).seatSurcharge(BigDecimal.ZERO).formatSurcharge(BigDecimal.ZERO)
                    .fixedPrice(true).total(total).build();
        }
        return PriceBreakdown.builder()
                .seatId(seat.getId()).seatType(seatType.getName()).ticketType(audience)
                .basePrice(ctx.baseByAudience.getOrDefault(audience, DEFAULT_BASE))
                .seatSurcharge(nz(seatType.getPriceModifier()))
                .formatSurcharge(nz(ctx.formatSurcharge))
                .fixedPrice(false).total(total).build();
    }

    /** Bảng giá cho FE: tên loại ghế -> (đối tượng -> giá). FE đổi loại vé không cần gọi lại server. */
    public Map<String, Map<String, BigDecimal>> buildPriceTable(PricingContext ctx, List<SeatType> seatTypes) {
        Map<String, Map<String, BigDecimal>> table = new LinkedHashMap<>();
        for (SeatType seatType : seatTypes) {
            Map<String, BigDecimal> byAud = new LinkedHashMap<>();
            for (String aud : AUDIENCE_TYPES) {
                byAud.put(aud, priceFor(ctx, seatType, aud));
            }
            table.put(seatType.getName(), byAud);
        }
        return table;
    }

    /** Tính thử giá theo các chiều rời (cho bộ Simulator của admin, không cần suất thật). */
    public PriceBreakdown simulate(String dayType, String timeSlot, String audience,
                                   SeatType seatType, MovieFormat fmt) {
        String aud = normalizeAudience(audience);
        List<PricingRule> rules = pricingRuleRepository.findByRuleTypeAndActiveTrue(RULE_BASE_PRICE);
        boolean fixed = fmt != null && Boolean.TRUE.equals(fmt.getIsFixedPrice());
        Map<Integer, BigDecimal> special = new HashMap<>();
        if (fixed) {
            for (SpecialSeatPrice sp : specialSeatPriceRepository.findAllWithRefs()) {
                if (sp.getFormat().getId().equals(fmt.getId())) {
                    special.put(sp.getSeatType().getId(), sp.getPrice());
                }
            }
        }
        Map<String, BigDecimal> baseByAud = new LinkedHashMap<>();
        baseByAud.put(aud, resolveBase(rules, dayType, timeSlot, aud));
        BigDecimal fmtSur = resolveFormatSurcharge(fmt, dayType);
        PricingContext ctx = new PricingContext(dayType, timeSlot, fmt, fixed, fmtSur, baseByAud, special);

        BigDecimal total = priceFor(ctx, seatType, aud);
        if (fixed) {
            return PriceBreakdown.builder()
                    .seatType(seatType.getName()).ticketType(aud)
                    .basePrice(total).seatSurcharge(BigDecimal.ZERO).formatSurcharge(BigDecimal.ZERO)
                    .fixedPrice(true).total(total).build();
        }
        return PriceBreakdown.builder()
                .seatType(seatType.getName()).ticketType(aud)
                .basePrice(baseByAud.get(aud))
                .seatSurcharge(nz(seatType.getPriceModifier()))
                .formatSurcharge(nz(fmtSur))
                .fixedPrice(false).total(total).build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** Ngữ cảnh giá của một suất, nạp một lần và tái dùng cho mọi ghế. */
    @Getter
    @AllArgsConstructor
    public static class PricingContext {
        private final String dayType;
        private final String timeSlot;
        private final MovieFormat format;
        private final boolean fixed;
        private final BigDecimal formatSurcharge; // phụ thu định dạng đã resolve theo ngày
        private final Map<String, BigDecimal> baseByAudience;
        private final Map<Integer, BigDecimal> specialBySeatType;
    }
}
