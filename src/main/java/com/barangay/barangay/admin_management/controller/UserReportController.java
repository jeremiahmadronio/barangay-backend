package com.barangay.barangay.admin_management.controller;

import com.barangay.barangay.admin_management.dto.report.ArchiveSummaryDTO;
import com.barangay.barangay.admin_management.dto.report.ModuleRecordsDTO;
import com.barangay.barangay.admin_management.dto.report.ReportStatsRequestDTO;
import com.barangay.barangay.admin_management.dto.report.SeverityReportDTO;
import com.barangay.barangay.admin_management.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("api/v1/system-admin")
@RequiredArgsConstructor
public class UserReportController {


    private final ReportService reportService;


    @GetMapping("/stats")
    public ResponseEntity<ReportStatsRequestDTO> getReportStats(){
        return ResponseEntity.ok(reportService.getStats());
    }

        @GetMapping("/growth-trend")
        public ResponseEntity<?> getTrend(
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

            if (ChronoUnit.YEARS.between(start, end) >= 1) {
                return ResponseEntity.badRequest().body("Maximum date range is 1 year only.");
            }
            if (start.isAfter(end)) {
                return ResponseEntity.badRequest().body("Start date cannot be after end date.");
            }

            return ResponseEntity.ok(reportService.getGrowthTrend(start, end));
        }


    @GetMapping("/module-records")
    public ResponseEntity<ModuleRecordsDTO> getModuleRecords(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        return ResponseEntity.ok(reportService.getModuleRecords(start, end));
    }


    @GetMapping("/audit-severity")
    public ResponseEntity<List<SeverityReportDTO>> getSeverityReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        return ResponseEntity.ok(reportService.getSeverityReport(start, end));
    }


    @GetMapping("/archive-summary")
    public ResponseEntity<ArchiveSummaryDTO> getArchiveSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        return ResponseEntity.ok(reportService.getArchiveSummary(start, end));
    }

}