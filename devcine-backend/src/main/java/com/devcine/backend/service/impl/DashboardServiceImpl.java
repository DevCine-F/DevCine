package com.devcine.backend.service.impl;

import com.devcine.backend.dto.response.DashboardStatsResponse;
import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import com.devcine.backend.service.DashboardService;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(String range, String month) {
        return getDashboardStats(range, month, null);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(String range, String month, Integer requestedCinemaId) {
        Window w = resolveWindow(range, month);
        Integer cinemaId = resolveCinemaScope(requestedCinemaId);

        // ===== KPI cho khoảng đã chọn (+ trend so với kỳ liền trước) =====
        BigDecimal revenue = bookingRepository.sumRevenueByDateRange(w.start, w.end, cinemaId);
        BigDecimal prevRevenue = bookingRepository.sumRevenueByDateRange(w.prevStart, w.prevEnd, cinemaId);

        long ticketCount = bookingRepository.countTicketsByDateRange(w.start, w.end, cinemaId);
        long prevTicketCount = bookingRepository.countTicketsByDateRange(w.prevStart, w.prevEnd, cinemaId);

        // "Khách mới của cơ sở" = lần đầu giao dịch tại đây, KHÔNG phải số tài khoản mới đăng ký:
        // khách đăng ký tài khoản cho cả hệ thống nên con số đó vô nghĩa với một quản lý cơ sở.
        long newUsers = bookingRepository.countNewCustomersByDateRange(w.start, w.end, cinemaId);
        long prevNewUsers = bookingRepository.countNewCustomersByDateRange(w.prevStart, w.prevEnd, cinemaId);

        long totalSeats = showtimeRepository.countTotalSeatsByDateRange(w.start, w.end, cinemaId);
        double occupancy = totalSeats > 0 ? (double) ticketCount / totalSeats * 100 : 0;
        long prevTotalSeats = showtimeRepository.countTotalSeatsByDateRange(w.prevStart, w.prevEnd, cinemaId);
        double prevOccupancy = prevTotalSeats > 0 ? (double) prevTicketCount / prevTotalSeats * 100 : 0;

        // ===== Biểu đồ doanh thu & vé (Tháng: trọn tháng đã chọn, còn lại: 7 ngày gần nhất) =====
        List<DashboardStatsResponse.ChartData> chart = buildChart(w.chartFrom, w.chartTo, cinemaId);

        return DashboardStatsResponse.builder()
                .rangeLabel(w.label)
                .revenue(new DashboardStatsResponse.StatItem(formatCurrency(revenue), calculateTrend(revenue, prevRevenue)))
                .tickets(new DashboardStatsResponse.StatItem(String.valueOf(ticketCount), calculateTrend(BigDecimal.valueOf(ticketCount), BigDecimal.valueOf(prevTicketCount))))
                .newUsers(new DashboardStatsResponse.StatItem(String.valueOf(newUsers), calculateTrend(BigDecimal.valueOf(newUsers), BigDecimal.valueOf(prevNewUsers))))
                .occupancy(new DashboardStatsResponse.StatItem(String.format("%.1f%%", occupancy), calculateTrend(BigDecimal.valueOf(occupancy), BigDecimal.valueOf(prevOccupancy))))
                .businessPerformance(chart)
                .topMovies(buildTopMovies(w, cinemaId))
                .recentBookings(buildRecentBookings(w, cinemaId))
                .showtimes(buildShowtimes(w, cinemaId))
                .build();
    }

    /**
     * Phạm vi cơ sở của người đang xem dashboard.
     *
     * <p>ADMIN: được phép xem toàn hệ thống (null) hoặc chọn 1 cơ sở cụ thể.
     * Quản lý/Nhân viên: bị khoá cứng theo cơ sở được phân công, không thể xem rạp khác.</p>
     */
    private Integer resolveCinemaScope(Integer requestedCinemaId) {
        if (SecurityUtils.hasRole("ADMIN")) return requestedCinemaId;

        Integer cinemaId = SecurityUtils.getCurrentUserCinemaId();
        if (cinemaId == null) {
            throw new AccessDeniedException(
                    "Tài khoản chưa được gán cơ sở nên không xem được báo cáo. Vui lòng liên hệ quản trị viên.");
        }
        return cinemaId;
    }

    // ===== Khoảng thời gian theo range =====
    private record Window(LocalDateTime start, LocalDateTime end,
                          LocalDateTime prevStart, LocalDateTime prevEnd, String label,
                          LocalDate chartFrom, LocalDate chartTo) {}

    private Window resolveWindow(String range, String month) {
        LocalDate today = LocalDate.now();
        LocalDateTime endToday = today.atTime(LocalTime.MAX);
        switch (range == null ? "" : range.toLowerCase()) {
            case "week":
                return new Window(
                        today.minusDays(6).atStartOfDay(), endToday,
                        today.minusDays(13).atStartOfDay(), today.minusDays(7).atTime(LocalTime.MAX),
                        "7 ngày qua", today.minusDays(6), today);
            case "month": {
                YearMonth ym = parseMonth(month, today);
                YearMonth prev = ym.minusMonths(1);
                LocalDate first = ym.atDay(1);
                // Tháng hiện tại chỉ tính tới hôm nay; tháng quá khứ tính trọn tháng
                LocalDate last = ym.equals(YearMonth.from(today)) ? today : ym.atEndOfMonth();
                return new Window(
                        first.atStartOfDay(), last.atTime(LocalTime.MAX),
                        prev.atDay(1).atStartOfDay(), prev.atEndOfMonth().atTime(LocalTime.MAX),
                        String.format("Tháng %02d/%d", ym.getMonthValue(), ym.getYear()),
                        first, last);
            }
            default:
                return new Window(
                        today.atStartOfDay(), endToday,
                        today.minusDays(1).atStartOfDay(), today.minusDays(1).atTime(LocalTime.MAX),
                        "Hôm nay", today.minusDays(6), today);
        }
    }

    // month dạng "yyyy-MM"; sai định dạng hoặc null → tháng hiện tại
    private YearMonth parseMonth(String month, LocalDate today) {
        if (month == null || month.isBlank()) return YearMonth.from(today);
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            return YearMonth.from(today);
        }
    }

    // ===== Biểu đồ theo khoảng ngày (2 query gộp, không N+1) =====
    private List<DashboardStatsResponse.ChartData> buildChart(LocalDate from, LocalDate to, Integer cinemaId) {
        LocalDateTime chartStart = from.atStartOfDay();
        LocalDateTime chartEnd = to.atTime(LocalTime.MAX);

        Map<LocalDate, BigDecimal> revByDay = new HashMap<>();
        for (Object[] r : bookingRepository.sumRevenueGroupedByDay(chartStart, chartEnd, cinemaId)) {
            revByDay.put(toLocalDate(r[0]), r[1] != null ? new BigDecimal(r[1].toString()) : BigDecimal.ZERO);
        }
        Map<LocalDate, Long> ticketsByDay = new HashMap<>();
        for (Object[] r : bookingRepository.countTicketsGroupedByDay(chartStart, chartEnd, cinemaId)) {
            ticketsByDay.put(toLocalDate(r[0]), ((Number) r[1]).longValue());
        }

        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd/MM");
        List<DashboardStatsResponse.ChartData> list = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            BigDecimal rev = revByDay.getOrDefault(day, BigDecimal.ZERO);
            long tk = ticketsByDay.getOrDefault(day, 0L);
            list.add(DashboardStatsResponse.ChartData.builder()
                    .label(day.format(labelFmt))
                    .revenue(rev.doubleValue())
                    .tickets(tk)
                    .revenueLabel(formatCurrency(rev))
                    .ticketLabel(tk + " vé")
                    .build());
        }
        return list;
    }

    // ===== Top phim theo doanh thu trong khoảng đã chọn (poster thật) =====
    // Doanh thu và số vé lấy từ hai query riêng (xem ghi chú ở BookingRepository) rồi ghép theo movieId.
    private List<DashboardStatsResponse.TopMovie> buildTopMovies(Window w, Integer cinemaId) {
        Map<Integer, Long> ticketsByMovie = new HashMap<>();
        for (Object[] row : bookingRepository.countTicketsGroupedByMovie(w.start, w.end, cinemaId)) {
            ticketsByMovie.put((Integer) row[0], ((Number) row[1]).longValue());
        }

        List<Object[]> raw = bookingRepository.findTopMoviesByRevenue(w.start, w.end, cinemaId);
        List<DashboardStatsResponse.TopMovie> list = new ArrayList<>();
        for (int i = 0; i < Math.min(5, raw.size()); i++) {
            Object[] row = raw.get(i);
            Integer movieId = (Integer) row[0];
            list.add(DashboardStatsResponse.TopMovie.builder()
                    .title((String) row[1])
                    .revenue(formatCurrency((BigDecimal) row[2]))
                    .tickets(String.valueOf(ticketsByMovie.getOrDefault(movieId, 0L)))
                    .posterUrl((String) row[3])
                    .build());
        }
        return list;
    }

    // ===== Giao dịch gần đây trong khoảng đã chọn =====
    private List<DashboardStatsResponse.RecentBooking> buildRecentBookings(Window w, Integer cinemaId) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm dd/MM");
        List<DashboardStatsResponse.RecentBooking> list = new ArrayList<>();
        for (Booking b : bookingRepository.findRecentConfirmed(w.start, w.end, cinemaId, PageRequest.of(0, 6))) {
            String customerName = "Khách vãng lai";
            if (b.getCustomer() != null && b.getCustomer().getUser() != null && b.getCustomer().getUser().getFullName() != null) {
                customerName = b.getCustomer().getUser().getFullName();
            }
            list.add(DashboardStatsResponse.RecentBooking.builder()
                    .code(b.getBookingCode())
                    .movieTitle(b.getShowtime().getMovie().getTitle())
                    .customerName(customerName)
                    .amount(formatCurrency(b.getFinalPrice()))
                    .channel(channelLabel(b.getPaymentMethod()))
                    .time(b.getCreatedAt() != null ? b.getCreatedAt().format(timeFmt) : "")
                    .build());
        }
        return list;
    }

    // ===== Suất chiếu trong khoảng đã chọn + tỉ lệ lấp đầy =====
    // Khoảng dài (Tháng) có thể hàng trăm suất → chỉ lấy SHOWTIME_LIMIT suất gần hiện tại nhất
    private static final int SHOWTIME_LIMIT = 50;

    private List<DashboardStatsResponse.ShowtimeItem> buildShowtimes(Window w, Integer cinemaId) {
        Map<Integer, Long> soldByShowtime = new HashMap<>();
        for (Object[] r : bookingRepository.countSoldSeatsByShowtimeInRange(w.start, w.end, cinemaId)) {
            soldByShowtime.put(((Number) r[0]).intValue(), ((Number) r[1]).longValue());
        }

        // Query trả DESC (mới nhất trước) → đảo lại để hiển thị theo thứ tự thời gian tăng dần
        List<Showtime> showtimes = new ArrayList<>(
                showtimeRepository.findLatestByRangeWithDetails(w.start, w.end, cinemaId, PageRequest.of(0, SHOWTIME_LIMIT)));
        Collections.reverse(showtimes);

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM");
        List<DashboardStatsResponse.ShowtimeItem> list = new ArrayList<>();
        for (Showtime s : showtimes) {
            Integer rows = s.getRoom().getMatrixRow();
            Integer cols = s.getRoom().getMatrixCol();
            int total = (rows != null && cols != null) ? rows * cols : 0;
            int sold = soldByShowtime.getOrDefault(s.getId(), 0L).intValue();
            double occ = total > 0 ? (double) sold / total * 100 : 0;
            list.add(DashboardStatsResponse.ShowtimeItem.builder()
                    .time(s.getStartTime().format(timeFmt))
                    .date(s.getStartTime().format(dateFmt))
                    .movieTitle(s.getMovie().getTitle())
                    .cinemaName(s.getRoom().getCinema().getName())
                    .roomName(s.getRoom().getName())
                    .sold(sold)
                    .total(total)
                    .occupancy(occ)
                    .build());
        }
        return list;
    }

    private String channelLabel(String method) {
        if (method == null) return "—";
        switch (method.toUpperCase()) {
            case "VNPAY": return "VNPAY";
            case "CASH": return "Tiền mặt";
            case "CARD": return "Thẻ";
            case "TRANSFER": return "Chuyển khoản";
            default: return method;
        }
    }

    // CAST(... AS date) có thể trả java.sql.Date, LocalDate hoặc LocalDateTime tuỳ driver — chuẩn hoá về LocalDate
    private LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate ld) return ld;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof LocalDateTime dt) return dt.toLocalDate();
        return LocalDate.parse(o.toString().substring(0, 10));
    }

    private String calculateTrend(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0%";
        }
        double change = (current.doubleValue() - previous.doubleValue()) / previous.doubleValue() * 100;
        return String.format("%s%.1f%%", change > 0 ? "+" : "", change);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0đ";
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(amount) + "đ";
    }
}
