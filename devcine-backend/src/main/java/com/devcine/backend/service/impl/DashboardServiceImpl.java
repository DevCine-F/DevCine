package com.devcine.backend.service.impl;

import com.devcine.backend.dto.response.DashboardStatsResponse;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import com.devcine.backend.repository.UserRepository;
import com.devcine.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
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
    public DashboardStatsResponse getDashboardStats() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay().minusSeconds(1);

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);

        LocalDateTime startOfYesterday = startOfToday.minusDays(1);
        LocalDateTime endOfYesterday = endOfToday.minusDays(1);

        // Gộp doanh thu + vé theo ngày cho 7 ngày gần nhất (2 query thay vì 14 query trong vòng lặp)
        LocalDateTime chartStart = startOfToday.minusDays(6);
        Map<LocalDate, BigDecimal> revByDay = new HashMap<>();
        for (Object[] r : bookingRepository.sumRevenueGroupedByDay(chartStart, endOfToday)) {
            revByDay.put(toLocalDate(r[0]), r[1] != null ? new BigDecimal(r[1].toString()) : BigDecimal.ZERO);
        }
        Map<LocalDate, Long> ticketsByDay = new HashMap<>();
        for (Object[] r : bookingRepository.countTicketsGroupedByDay(chartStart, endOfToday)) {
            ticketsByDay.put(toLocalDate(r[0]), ((Number) r[1]).longValue());
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Revenue (hôm nay/hôm qua lấy lại từ map đã gộp — không query thêm)
        BigDecimal revenueMonthly = bookingRepository.sumRevenueByDateRange(startOfMonth, endOfMonth);
        BigDecimal revenueToday = revByDay.getOrDefault(today, BigDecimal.ZERO);
        BigDecimal revenueYesterday = revByDay.getOrDefault(yesterday, BigDecimal.ZERO);

        String monthlyRevenueTrend = calculateTrend(revenueMonthly, bookingRepository.sumRevenueByDateRange(startOfMonth.minusMonths(1), startOfMonth.minusSeconds(1)));
        String dailyRevenueTrend = calculateTrend(revenueToday, revenueYesterday);

        // Tickets (lấy lại từ map đã gộp)
        long ticketsToday = ticketsByDay.getOrDefault(today, 0L);
        long ticketsYesterday = ticketsByDay.getOrDefault(yesterday, 0L);
        String ticketsTrend = calculateTrend(BigDecimal.valueOf(ticketsToday), BigDecimal.valueOf(ticketsYesterday));

        // New Users
        long newUsersToday = userRepository.countNewUsersByDateRange(startOfToday, endOfToday);
        long newUsersYesterday = userRepository.countNewUsersByDateRange(startOfYesterday, endOfYesterday);
        String newUsersTrend = calculateTrend(BigDecimal.valueOf(newUsersToday), BigDecimal.valueOf(newUsersYesterday));

        // Occupancy Rate (Simplified for today)
        long totalSeatsToday = showtimeRepository.countTotalSeatsByDateRange(startOfToday, endOfToday);
        double occupancyRate = totalSeatsToday > 0 ? (double) ticketsToday / totalSeatsToday * 100 : 0;
        
        long totalSeatsYesterday = showtimeRepository.countTotalSeatsByDateRange(startOfYesterday, endOfYesterday);
        double occupancyRateYesterday = totalSeatsYesterday > 0 ? (double) ticketsYesterday / totalSeatsYesterday * 100 : 0;
        String occupancyTrend = calculateTrend(BigDecimal.valueOf(occupancyRate), BigDecimal.valueOf(occupancyRateYesterday));

        // Business Performance Chart
        List<DashboardStatsResponse.ChartData> chartDataList = new ArrayList<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE"); // Mon, Tue
        BigDecimal maxRevenue = BigDecimal.ONE;
        long maxTickets = 1;

        BigDecimal[] dayRevenues = new BigDecimal[7];
        long[] dayTicketsArr = new long[7];

        for (int i = 6; i >= 0; i--) {
            LocalDateTime start = startOfToday.minusDays(i);
            LocalDate day = today.minusDays(i);
            BigDecimal dayRevenue = revByDay.getOrDefault(day, BigDecimal.ZERO);
            long dayTickets = ticketsByDay.getOrDefault(day, 0L);

            dayRevenues[6 - i] = dayRevenue;
            dayTicketsArr[6 - i] = dayTickets;

            if (dayRevenue.compareTo(maxRevenue) > 0) maxRevenue = dayRevenue;
            if (dayTickets > maxTickets) maxTickets = dayTickets;

            DashboardStatsResponse.ChartData data = new DashboardStatsResponse.ChartData();
            data.setDay(start.format(dayFormatter));
            data.setRevenueLabel(formatCurrency(dayRevenue));
            data.setTicketLabel(dayTickets + " vé");
            // we will set percentage later
            chartDataList.add(data);
        }
        
        for (int i = 0; i < 7; i++) {
            DashboardStatsResponse.ChartData data = chartDataList.get(i);
            data.setRevenuePercentage(dayRevenues[i].doubleValue() / maxRevenue.doubleValue() * 100);
            data.setTicketPercentage((double) dayTicketsArr[i] / maxTickets * 100);
        }

        // Top Movies
        List<Object[]> topMoviesRaw = bookingRepository.findTopMoviesByRevenue();
        List<DashboardStatsResponse.TopMovie> topMovies = new ArrayList<>();
        for (int i = 0; i < Math.min(4, topMoviesRaw.size()); i++) {
            Object[] row = topMoviesRaw.get(i);
            DashboardStatsResponse.TopMovie movie = new DashboardStatsResponse.TopMovie();
            movie.setTitle((String) row[0]);
            movie.setRevenue(formatCurrency((BigDecimal) row[1]));
            movie.setTickets(row[2].toString());
            movie.setOccupancy("N/A"); // simplified
            movie.setTrend("up");
            movie.setImageUrl("/images/Hopper.webp"); // placeholder
            topMovies.add(movie);
        }

        return DashboardStatsResponse.builder()
                .revenueMonthly(new DashboardStatsResponse.StatItem(formatCurrency(revenueMonthly), monthlyRevenueTrend))
                .revenueDaily(new DashboardStatsResponse.StatItem(formatCurrency(revenueToday), dailyRevenueTrend))
                .tickets(new DashboardStatsResponse.StatItem(String.valueOf(ticketsToday), ticketsTrend))
                .newUsers(new DashboardStatsResponse.StatItem(String.valueOf(newUsersToday), newUsersTrend))
                .occupancy(new DashboardStatsResponse.StatItem(String.format("%.1f%%", occupancyRate), occupancyTrend))
                .businessPerformance(chartDataList)
                .topMovies(topMovies)
                .build();
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
