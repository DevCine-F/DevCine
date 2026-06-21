package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private String rangeLabel;

    private StatItem revenue;
    private StatItem tickets;
    private StatItem newUsers;
    private StatItem occupancy;

    private List<ChartData> businessPerformance;
    private List<TopMovie> topMovies;
    private List<RecentBooking> recentBookings;
    private List<TodayShowtime> todayShowtimes;
    private List<LowStockItem> lowStock;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatItem {
        private String value;
        private String trend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartData {
        private String label;
        private Double revenue;
        private Long tickets;
        private String revenueLabel;
        private String ticketLabel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopMovie {
        private String title;
        private String revenue;
        private String tickets;
        private String posterUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentBooking {
        private String code;
        private String movieTitle;
        private String customerName;
        private String amount;
        private String channel;
        private String time;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayShowtime {
        private String time;
        private String movieTitle;
        private String cinemaName;
        private String roomName;
        private Integer sold;
        private Integer total;
        private Double occupancy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockItem {
        private String name;
        private String cinemaName;
        private Integer inStock;
    }
}
