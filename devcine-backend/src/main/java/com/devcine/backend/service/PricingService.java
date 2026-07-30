package com.devcine.backend.service;

import com.devcine.backend.dto.response.PriceBreakdown;
import com.devcine.backend.entity.MovieFormat;
import com.devcine.backend.entity.PricingRule;
import com.devcine.backend.entity.Room;
import com.devcine.backend.entity.Seat;
import com.devcine.backend.entity.SeatType;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.repository.HolidayRepository;
import com.devcine.backend.repository.PricingRuleRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nguồn sự thật DUY NHẤT cho giá vé — mô hình FLAT PRICING.
 *   giá = giá_nền(loại_ngày × loại_phòng × đối_tượng) + phụ_thu_định_dạng(2D/3D theo ngày)
 * MỌI ghế trong cùng phòng + cùng định dạng ĐỒNG GIÁ (không phụ thu theo loại ghế).
 * Dùng chung cho seat-map (SeatService) và giữ ghế (BookingService) — không tính giá ở nơi khác.
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRuleRepository pricingRuleRepository;
    private final HolidayRepository holidayRepository;

    public static final List<String> AUDIENCE_TYPES = List.of("ADULT", "U22", "CHILD", "SENIOR");
    /** Đối tượng được phép mua ONLINE — CHILD/SENIOR phải xác minh giấy tờ nên chỉ bán tại quầy POS. */
    public static final List<String> ONLINE_AUDIENCE_TYPES = List.of("ADULT", "U22");
    public static final List<String> ROOM_TYPES = List.of("STANDARD", "SUPERPLEX", "CINE_COMFORT");
    public static final String RULE_BASE_PRICE = "BASE_PRICE";
    private static final BigDecimal DEFAULT_BASE = new BigDecimal("85000");

    /** Mã đối tượng -> nhãn hiển thị (giữ thứ tự) — đầy đủ 4 loại (dùng cho POS & màn quản trị). */
    public static Map<String, String> audienceLabels() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("ADULT", "Người lớn");
        m.put("U22", "U22 / HSSV");
        m.put("CHILD", "Trẻ em");
        m.put("SENIOR", "Người cao tuổi");
        return m;
    }

    /** Nhãn đối tượng theo kênh: ONLINE chỉ ADULT/U22, POS đủ 4 loại. */
    public static Map<String, String> audienceLabels(boolean online) {
        if (!online) return audienceLabels();
        Map<String, String> full = audienceLabels();
        Map<String, String> m = new LinkedHashMap<>();
        for (String a : ONLINE_AUDIENCE_TYPES) m.put(a, full.get(a));
        return m;
    }

    /** Mã loại phòng -> nhãn hiển thị (giữ thứ tự). Chuẩn Lotte Cinema. */
    public static Map<String, String> roomTypeLabels() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("STANDARD", "Phòng Tiêu chuẩn (2D)");
        m.put("SUPERPLEX", "Phòng Superplex (màn hình siêu lớn)");
        m.put("CINE_COMFORT", "Phòng Cine Comfort (ghế sofa ngả lưng)");
        return m;
    }

    public String normalizeAudience(String a) {
        if (a == null) return "ADULT";
        String up = a.trim().toUpperCase();
        if ("STUDENT".equals(up)) return "U22"; // tương thích dữ liệu cũ (STUDENT -> U22)
        return AUDIENCE_TYPES.contains(up) ? up : "ADULT";
    }

    /**
     * Gom Room.type (free-text lộn xộn) về 3 hạng chuẩn Lotte. Mặc định STANDARD.
     * Vẫn nhận diện chuỗi legacy (IMAX/DELUXE/GOLD) để dữ liệu cũ chưa backfill không bị tụt về STANDARD:
     * IMAX/Premium (màn lớn) -> SUPERPLEX · Deluxe/Gold/Sweetbox/Comfort (ghế cao cấp) -> CINE_COMFORT.
     */
    public String normalizeRoomType(String raw) {
        if (raw == null) return "STANDARD";
        String s = raw.trim().toUpperCase();
        if (s.contains("SUPERPLEX") || s.contains("IMAX") || s.contains("PREMIUM")) return "SUPERPLEX";
        if (s.contains("CINE_COMFORT") || s.contains("COMFORT") || s.contains("DELUXE")
                || s.contains("GOLD") || s.contains("SWEETBOX")) return "CINE_COMFORT";
        return "STANDARD";
    }

    /** 2 bậc: T2–T5 = WEEKDAY · T6,7,CN & ngày lễ = WEEKEND (gộp cao điểm). */
    public String resolveDayType(LocalDate date) {
        if (holidayRepository.existsByHolidayDate(date)) return "WEEKEND";
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

    /** Nạp toàn bộ ngữ cảnh giá của 1 suất MỘT LẦN (tránh N+1 khi tính nhiều ghế). */
    public PricingContext buildContext(Showtime st) {
        LocalDateTime start = st.getStartTime();
        String dayType = resolveDayType(start.toLocalDate());
        Room room = st.getRoom();
        String roomType = normalizeRoomType(room != null ? room.getType() : null);
        List<PricingRule> rules = pricingRuleRepository.findByRuleTypeAndActiveTrue(RULE_BASE_PRICE);

        MovieFormat fmt = st.getFormat();
        Map<String, BigDecimal> baseByAudience = new LinkedHashMap<>();
        for (String aud : AUDIENCE_TYPES) {
            baseByAudience.put(aud, resolveBase(rules, dayType, roomType, aud));
        }
        BigDecimal fmtSurcharge = resolveFormatSurcharge(fmt, dayType);
        return new PricingContext(dayType, roomType, fmt, fmtSurcharge, baseByAudience);
    }

    private BigDecimal resolveBase(List<PricingRule> rules, String dayType, String roomType, String audience) {
        PricingRule best = bestRule(rules, dayType, roomType, audience);
        return best != null ? best.getValue() : DEFAULT_BASE;
    }

    /** Chọn rule khớp nhất: ưu tiên khớp chính xác hơn ALL, rồi đến priority. */
    private PricingRule bestRule(List<PricingRule> rules, String dayType, String roomType, String audience) {
        PricingRule best = null;
        int bestScore = Integer.MIN_VALUE;
        for (PricingRule r : rules) {
            Integer sDay = matchScore(r.getDayType(), dayType);
            Integer sRoom = matchScore(r.getRoomType(), roomType);
            Integer sAud = matchScore(r.getAudienceType(), audience);
            if (sDay == null || sRoom == null || sAud == null) continue; // có chiều không khớp
            int total = (sDay + sRoom + sAud) * 100 + (r.getPriority() != null ? r.getPriority() : 0);
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

    /** Giá 1 vé theo đối tượng trong ngữ cảnh đã nạp (flat: không phụ thuộc loại ghế). */
    public BigDecimal priceFor(PricingContext ctx, String audience) {
        BigDecimal base = ctx.baseByAudience.getOrDefault(audience, DEFAULT_BASE);
        return base.add(nz(ctx.formatSurcharge)).max(BigDecimal.ZERO);
    }

    public PriceBreakdown breakdown(PricingContext ctx, Seat seat, String audience) {
        BigDecimal total = priceFor(ctx, audience);
        return PriceBreakdown.builder()
                .seatId(seat != null ? seat.getId() : null)
                .seatType(seat != null && seat.getSeatType() != null ? seat.getSeatType().getName() : null)
                .ticketType(audience)
                .basePrice(ctx.baseByAudience.getOrDefault(audience, DEFAULT_BASE))
                .seatSurcharge(BigDecimal.ZERO)
                .formatSurcharge(nz(ctx.formatSurcharge))
                .fixedPrice(false).total(total).build();
    }

    /**
     * Bảng giá cho FE: tên loại ghế -> (đối tượng -> giá). Flat pricing nên mọi loại ghế đồng giá,
     * nhưng vẫn giữ cấu trúc theo loại ghế để seat-map FE không phải đổi contract.
     */
    public Map<String, Map<String, BigDecimal>> buildPriceTable(PricingContext ctx, List<SeatType> seatTypes) {
        return buildPriceTable(ctx, seatTypes, AUDIENCE_TYPES);
    }

    public Map<String, Map<String, BigDecimal>> buildPriceTable(PricingContext ctx, List<SeatType> seatTypes,
                                                                List<String> audiences) {
        Map<String, BigDecimal> byAud = new LinkedHashMap<>();
        for (String aud : audiences) {
            byAud.put(aud, priceFor(ctx, aud));
        }
        Map<String, Map<String, BigDecimal>> table = new LinkedHashMap<>();
        for (SeatType seatType : seatTypes) {
            table.put(seatType.getName(), new LinkedHashMap<>(byAud));
        }
        return table;
    }

    /** Tính thử giá theo các chiều rời (cho bộ Simulator của admin, không cần suất thật). */
    public PriceBreakdown simulate(String dayType, String audience, String roomType, MovieFormat fmt) {
        String aud = normalizeAudience(audience);
        String room = normalizeRoomType(roomType);
        List<PricingRule> rules = pricingRuleRepository.findByRuleTypeAndActiveTrue(RULE_BASE_PRICE);
        Map<String, BigDecimal> baseByAud = new LinkedHashMap<>();
        baseByAud.put(aud, resolveBase(rules, dayType, room, aud));
        BigDecimal fmtSur = resolveFormatSurcharge(fmt, dayType);
        PricingContext ctx = new PricingContext(dayType, room, fmt, fmtSur, baseByAud);

        BigDecimal total = priceFor(ctx, aud);
        return PriceBreakdown.builder()
                .ticketType(aud)
                .basePrice(baseByAud.get(aud))
                .seatSurcharge(BigDecimal.ZERO)
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
        private final String roomType;
        private final MovieFormat format;
        private final BigDecimal formatSurcharge; // phụ thu định dạng đã resolve theo ngày
        private final Map<String, BigDecimal> baseByAudience;
    }
}
