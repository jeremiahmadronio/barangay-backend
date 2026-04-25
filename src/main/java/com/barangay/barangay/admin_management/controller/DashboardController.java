package com.barangay.barangay.admin_management.controller;

import com.barangay.barangay.admin_management.dto.ActivityOverviewDTO;
import com.barangay.barangay.admin_management.dto.DashboardStats;
import com.barangay.barangay.admin_management.dto.RecentSystemAction;
import com.barangay.barangay.admin_management.dto.dashboard.ResidentTrendDTO;
import com.barangay.barangay.admin_management.repository.Root_AdminRepository;
import com.barangay.barangay.admin_management.service.RootDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Controller
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final Root_AdminRepository userRepository;
    private final RootDashboardService rootDashboardService;


    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getRootStats() {
        return ResponseEntity.ok(rootDashboardService.getDashboardStats());
    }

    @GetMapping("/activity-overview")
    public ResponseEntity<ActivityOverviewDTO> getActivityOverview() {
        return ResponseEntity.ok(rootDashboardService.getActivityOverview());
    }

    @GetMapping("/recent-actions")
    public ResponseEntity<List<RecentSystemAction>> getRecentActions() {
        return ResponseEntity.ok(rootDashboardService.getRecentActions());
    }


    @GetMapping("/last-six-months")
    public ResponseEntity<ResidentTrendDTO> getResidentTrend() {
        return ResponseEntity.ok(rootDashboardService.getResidentGrowthTrend());
    }
}
