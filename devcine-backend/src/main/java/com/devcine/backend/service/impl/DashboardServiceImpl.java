package com.devcine.backend.service.impl;

import com.devcine.backend.dto.response.DashboardStatsResponse;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import com.devcine.backend.repository.UserRepository;
import com.devcine.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ShowtimeRepository showtimeRepository;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay().minusSeconds(1);

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);

        LocalDateTime startOfYesterday = startOfToday.minusDays(1);
        LocalDateTime endOfYesterday = endOfToday.minusDays(1);

        // Revenue
        BigDecimal revenueMonthly = bookingRepository.sumRevenueByDateRange(startOfMonth, endOfMonth);
        BigDecimal revenueToday = bookingRepository.sumRevenueByDateRange(startOfToday, endOfToday);
        BigDecimal revenueYesterday = bookingRepository.sumRevenueByDateRange(startOfYesterday, endOfYesterday);

        String monthlyRevenueTrend = calculateTrend(revenueMonthly, bookingRepository.sumRevenueByDateRange(startOfMonth.minusMonths(1), startOfMonth.minusSeconds(1)));
        String dailyRevenueTrend = calculateTrend(revenueToday, revenueYesterday);

        // Tickets
        long ticketsToday = bookingRepository.countTicketsByDateRange(startOfToday, endOfToday);
        long ticketsYesterday = bookingRepository.countTicketsByDateRange(startOfYesterday, endOfYesterday);
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

        for (int i = 6; i >= 0; i--) {
            LocalDateTime start = startOfToday.minusDays(i);
            LocalDateTime end = endOfToday.minusDays(i);
            BigDecimal dayRevenue = bookingRepository.sumRevenueByDateRange(start, end);
            long dayTickets = bookingRepository.countTicketsByDateRange(start, end);

            if (dayRevenue.compareTo(maxRevenue) > 0) maxRevenue = dayRevenue;
            if (dayTickets > maxTickets) maxTickets = dayTickets;
            
            DashboardStatsResponse.ChartData data = new DashboardStatsResponse.ChartData();
            data.setDay(start.format(dayFormatter));
            data.setRevenueLabel(formatCurrency(dayRevenue));
            data.setTicketLabel(dayTickets + " vé");
            // we will set percentage later
            chartDataList.add(data);
        }
        
        for (int i = 6; i >= 0; i--) {
            LocalDateTime start = startOfToday.minusDays(i);
            LocalDateTime end = endOfToday.minusDays(i);
            BigDecimal dayRevenue = bookingRepository.sumRevenueByDateRange(start, end);
            long dayTickets = bookingRepository.countTicketsByDateRange(start, end);

            DashboardStatsResponse.ChartData data = chartDataList.get(6 - i);
            data.setRevenuePercentage(dayRevenue.doubleValue() / maxRevenue.doubleValue() * 100);
            data.setTicketPercentage((double) dayTickets / maxTickets * 100);
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
