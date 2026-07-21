package com.devcine.backend.service;

import com.devcine.backend.dto.response.DashboardStatsResponse;

public interface DashboardService {
    DashboardStatsResponse getDashboardStats(String range, String month);
}
