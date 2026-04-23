package com.barangay.barangay.admin_management.dto.report;

public record ReportStatsRequestDTO  (

         long totalAdminUsers,
         long totalResidents,
         long totalOfficers,
         long totalAuditLogsThisMonth

){
}
