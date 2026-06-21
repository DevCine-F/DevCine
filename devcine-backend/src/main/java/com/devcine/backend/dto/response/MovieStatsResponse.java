package com.devcine.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Thống kê vận hành thật theo từng phim — hiển thị ở modal chi tiết Quản lý Phim.
 * Mọi số liệu lấy từ booking_seats/bookings/showtimes (status CONFIRMED), không hardcode.
 */
@Data
@Builder
public class MovieStatsResponse {

    /** Doanh thu vé (tổng price_snapshot của ghế đã CONFIRMED). */
    private BigDecimal ticketRevenue;

    /** Tổng số vé đã bán. */
    private long ticketsSold;

    /** Tổng số suất chiếu đã lên lịch. */
    private long showtimeCount;

    /** Tỷ lệ lấp đầy của các suất đã diễn ra (0–100). */
    private double occupancyRate;

    /** Phân bổ doanh thu theo hạng ghế. */
    private List<ClassRevenue> classDistribution;

    @Data
    @Builder
    public static class ClassRevenue {
        private String name;
        private BigDecimal revenue;
        private long count;
        /** % doanh thu so với tổng (0–100). */
        private double percentage;
    }
}
