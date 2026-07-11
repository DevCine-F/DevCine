package com.devcine.backend.service.impl;

import com.devcine.backend.dto.response.DashboardStatsResponse;
import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import com.devcine.backend.repository.UserRepository;
import com.devcine.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ShowtimeRepository showtimeRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(String range) {
        Window w = resolveWindow(range);

        // ===== KPI cho khoảng đã chọn (+ trend so với kỳ liền trước) =====
        BigDecimal revenue = bookingRepository.sumRevenueByDateRange(w.start, w.end);
        BigDecimal prevRevenue = bookingRepository.sumRevenueByDateRange(w.prevStart, w.prevEnd);

        long ticketCount = bookingRepository.countTicketsByDateRange(w.start, w.end);
        long prevTicketCount = bookingRepository.countTicketsByDateRange(w.prevStart, w.prevEnd);

        long newUsers = userRepository.countNewUsersByDateRange(w.start, w.end);
        long prevNewUsers = userRepository.countNewUsersByDateRange(w.prevStart, w.prevEnd);

        long totalSeats = showtimeRepository.countTotalSeatsByDateRange(w.start, w.end);
        double occupancy = totalSeats > 0 ? (double) ticketCount / totalSeats * 100 : 0;
        long prevTotalSeats = showtimeRepository.countTotalSeatsByDateRange(w.prevStart, w.prevEnd);
        double prevOccupancy = prevTotalSeats > 0 ? (double) prevTicketCount / prevTotalSeats * 100 : 0;

        // ===== Biểu đồ doanh thu & vé (7 ngày, riêng Tháng dùng 30 ngày) =====
        List<DashboardStatsResponse.ChartData> chart = buildChart("month".equalsIgnoreCase(range) ? 30 : 7);

        return DashboardStatsResponse.builder()
                .rangeLabel(w.label)
                .revenue(new DashboardStatsResponse.StatItem(formatCurrency(revenue), calculateTrend(revenue, prevRevenue)))
                .tickets(new DashboardStatsResponse.StatItem(String.valueOf(ticketCount), calculateTrend(BigDecimal.valueOf(ticketCount), BigDecimal.valueOf(prevTicketCount))))
                .newUsers(new DashboardStatsResponse.StatItem(String.valueOf(newUsers), calculateTrend(BigDecimal.valueOf(newUsers), BigDecimal.valueOf(prevNewUsers))))
                .occupancy(new DashboardStatsResponse.StatItem(String.format("%.1f%%", occupancy), calculateTrend(BigDecimal.valueOf(occupancy), BigDecimal.valueOf(prevOccupancy))))
                .businessPerformance(chart)
                .topMovies(buildTopMovies())
                .recentBookings(buildRecentBookings())
                .todayShowtimes(buildTodayShowtimes())
                .build();
    }

    // ===== Khoảng thời gian theo range =====
    private record Window(LocalDateTime start, LocalDateTime end,
                          LocalDateTime prevStart, LocalDateTime prevEnd, String label) {}

    private Window resolveWindow(String range) {
        LocalDate today = LocalDate.now();
        LocalDateTime endToday = today.atTime(LocalTime.MAX);
        switch (range == null ? "" : range.toLowerCase()) {
            case "week":
                return new Window(
                        today.minusDays(6).atStartOfDay(), endToday,
                        today.minusDays(13).atStartOfDay(), today.minusDays(7).atTime(LocalTime.MAX),
                        "7 ngày qua");
            case "month": {
                LocalDateTime startMonth = today.withDayOfMonth(1).atStartOfDay();
                return new Window(
                        startMonth, endToday,
                        today.minusMonths(1).withDayOfMonth(1).atStartOfDay(), startMonth.minusSeconds(1),
                        "Tháng này");
            }
            default:
                return new Window(
                        today.atStartOfDay(), endToday,
                        today.minusDays(1).atStartOfDay(), today.minusDays(1).atTime(LocalTime.MAX),
                        "Hôm nay");
        }
    }

    // ===== Biểu đồ N ngày gần nhất (2 query gộp, không N+1) =====
    private List<DashboardStatsResponse.ChartData> buildChart(int days) {
        LocalDate today = LocalDate.now();
        LocalDateTime chartStart = today.minusDays(days - 1L).atStartOfDay();
        LocalDateTime chartEnd = today.atTime(LocalTime.MAX);

        Map<LocalDate, BigDecimal> revByDay = new HashMap<>();
        for (Object[] r : bookingRepository.sumRevenueGroupedByDay(chartStart, chartEnd)) {
            revByDay.put(toLocalDate(r[0]), r[1] != null ? new BigDecimal(r[1].toString()) : BigDecimal.ZERO);
        }
        Map<LocalDate, Long> ticketsByDay = new HashMap<>();
        for (Object[] r : bookingRepository.countTicketsGroupedByDay(chartStart, chartEnd)) {
            ticketsByDay.put(toLocalDate(r[0]), ((Number) r[1]).longValue());
        }

        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd/MM");
        List<DashboardStatsResponse.ChartData> list = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
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

    // ===== Top phim theo doanh thu (poster thật) =====
    private List<DashboardStatsResponse.TopMovie> buildTopMovies() {
        List<Object[]> raw = bookingRepository.findTopMoviesByRevenue();
        List<DashboardStatsResponse.TopMovie> list = new ArrayList<>();
        for (int i = 0; i < Math.min(5, raw.size()); i++) {
            Object[] row = raw.get(i);
            list.add(DashboardStatsResponse.TopMovie.builder()
                    .title((String) row[0])
                    .revenue(formatCurrency((BigDecimal) row[1]))
                    .tickets(row[2].toString())
                    .posterUrl((String) row[3])
                    .build());
        }
        return list;
    }

    // ===== Giao dịch gần đây =====
    private List<DashboardStatsResponse.RecentBooking> buildRecentBookings() {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm dd/MM");
        List<DashboardStatsResponse.RecentBooking> list = new ArrayList<>();
        for (Booking b : bookingRepository.findRecentConfirmed(PageRequest.of(0, 6))) {
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

    // ===== Suất chiếu hôm nay + tỉ lệ lấp đầy =====
    private List<DashboardStatsResponse.TodayShowtime> buildTodayShowtimes() {
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.atTime(LocalTime.MAX);

        Map<Integer, Long> soldByShowtime = new HashMap<>();
        for (Object[] r : bookingRepository.countSoldSeatsByShowtimeInRange(startToday, endToday)) {
            soldByShowtime.put(((Number) r[0]).intValue(), ((Number) r[1]).longValue());
        }

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        List<DashboardStatsResponse.TodayShowtime> list = new ArrayList<>();
        for (Showtime s : showtimeRepository.findByRangeWithDetails(startToday, endToday)) {
            Integer rows = s.getRoom().getMatrixRow();
            Integer cols = s.getRoom().getMatrixCol();
            int total = (rows != null && cols != null) ? rows * cols : 0;
            int sold = soldByShowtime.getOrDefault(s.getId(), 0L).intValue();
            double occ = total > 0 ? (double) sold / total * 100 : 0;
            list.add(DashboardStatsResponse.TodayShowtime.builder()
                    .time(s.getStartTime().format(timeFmt))
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

    @Override
    public Object debug() {
        return bookingRepository.findAll().stream()
            .map(b -> b.getId() + " - " + b.getStatus() + " - " + b.getFinalPrice() + " - " + b.getCreatedAt())
            .toList();
    }
}
