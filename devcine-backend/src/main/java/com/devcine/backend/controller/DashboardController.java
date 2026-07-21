package com.devcine.backend.controller;

import com.devcine.backend.dto.response.DashboardStatsResponse;
import com.devcine.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("@perm.can('dashboard_stats', 'view')")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @RequestParam(defaultValue = "today") String range,
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(dashboardService.getDashboardStats(range, month));
    }
}
