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
    private StatItem revenueMonthly;
    private StatItem revenueDaily;
    private StatItem tickets;
    private StatItem newUsers;
    private StatItem occupancy;
    
    private List<ChartData> businessPerformance;
    private List<TopMovie> topMovies;
    
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
        private String day;
        private Double revenuePercentage;
        private String revenueLabel;
        private Double ticketPercentage;
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
        private String occupancy;
        private String trend;
        private String imageUrl;
    }
}
