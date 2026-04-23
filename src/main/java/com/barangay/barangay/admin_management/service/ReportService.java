package com.barangay.barangay.admin_management.service;

import com.barangay.barangay.admin_management.dto.report.*;
import com.barangay.barangay.admin_management.repository.Root_AdminRepository;
import com.barangay.barangay.admin_management.repository.SystemAdminAuditLogRepository;
import com.barangay.barangay.admin_management.repository.SystemAdminEmployeeRepository;
import com.barangay.barangay.admin_management.repository.SystemAdminResidentRepository;
import com.barangay.barangay.audit.repository.AuditLogRepository;
import com.barangay.barangay.enumerated.ResidentStatus;
import com.barangay.barangay.enumerated.Status;
import com.barangay.barangay.person.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final Root_AdminRepository userRepository;
    private final SystemAdminResidentRepository residentRepository;
    private final SystemAdminEmployeeRepository employeeRepository;
    private final SystemAdminAuditLogRepository auditLogRepository;




    @Transactional(readOnly = true)
    public ReportStatsRequestDTO getStats() {
        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .with(LocalTime.MIN);

        long activeAdmins = userRepository.countByRole_RoleNameAndStatus("ADMIN", Status.ACTIVE);
        long activeOfficers = employeeRepository.countByStatus( Status.ACTIVE);
        long activeResidents = residentRepository.countByStatus(ResidentStatus.ACTIVE);

        long logsThisMonth = auditLogRepository.countByCreatedAtAfter(startOfMonth);

        return new ReportStatsRequestDTO(
                activeAdmins,
                activeResidents,
                activeOfficers,
                logsThisMonth
        );
    }



    @Transactional(readOnly = true)
        public GrowthTrendResponse getGrowthTrend(LocalDateTime start, LocalDateTime end) {
            long daysBetween = ChronoUnit.DAYS.between(start, end);

            boolean isDaily = daysBetween <= 30;
            String format = isDaily ? "DD" : "Mon";
            String trunc = isDaily ? "day" : "month";

            List<String> labels = generateLabels(start, end, isDaily);
        List<String> adminRoles = List.of("ROOT_ADMIN", "ADMIN");

            Map<String, Long> adminData = convertToMap(userRepository.getTrendByRoleAndStatus("ADMIN", start, end, format, trunc));
            Map<String, Long> residentData = convertToMap(residentRepository.getResidentTrend(start, end, format, trunc));
            Map<String, Long> officerData = convertToMap(employeeRepository.getEmployeeTrend(start, end, format, trunc));
        Map<String, Long> regularUserData = convertToMap(userRepository.getTrendExcludingRoles(
                adminRoles, start, end, format, trunc));
            List<Long> adminCounts = labels.stream().map(l -> adminData.getOrDefault(l, 0L)).toList();
            List<Long> residentCounts = labels.stream().map(l -> residentData.getOrDefault(l, 0L)).toList();
            List<Long> officerCounts = labels.stream().map(l -> officerData.getOrDefault(l, 0L)).toList();
            List<Long> userCounts = labels.stream().map(l -> regularUserData.getOrDefault(l, 0L)).toList();
            return new GrowthTrendResponse(labels, adminCounts, residentCounts, officerCounts, userCounts);
        }

        private List<String> generateLabels(LocalDateTime start, LocalDateTime end, boolean isDaily) {
            List<String> labels = new ArrayList<>();
            LocalDateTime current = start;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(isDaily ? "dd" : "MMM");

            while (!current.isAfter(end)) {
                labels.add(current.format(formatter));
                current = isDaily ? current.plusDays(1) : current.plusMonths(1);
            }
            return labels;
        }

        private Map<String, Long> convertToMap(List<Object[]> results) {
            if (results == null) return Collections.emptyMap();
            return results.stream().collect(Collectors.toMap(
                    res -> res[1].toString().trim(),
                    res -> ((Number) res[0]).longValue(),
                    (existing, replacement) -> existing
            ));
        }





        @Transactional(readOnly = true)
    public ModuleRecordsDTO getModuleRecords(LocalDateTime start, LocalDateTime end) {

        // 1. Active Admins within date range
        long adminCount = userRepository.countByRole_RoleNameAndStatusAndCreatedAtBetween(
                "ADMIN", Status.ACTIVE, start, end);

        // 2. Active Residents within date range
        long residentCount = residentRepository.countByStatusAndCreatedDateBetween(
                ResidentStatus.ACTIVE, start, end);

        // 3. Active Officers (Employees) within date range
        long officerCount = employeeRepository.countByStatusAndCreatedAtBetween(
                Status.ACTIVE, start, end);

        // 4. Audit Logs within date range (Total activities recorded)
        long auditCount = auditLogRepository.countByCreatedAtBetween(start, end);

        return new ModuleRecordsDTO(adminCount, residentCount, officerCount, auditCount);
    }




    @Transactional(readOnly = true)
    public List<SeverityReportDTO> getSeverityReport(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.getSeverityDistribution(start, end);
    }




    public ArchiveSummaryDTO getArchiveSummary(LocalDateTime start, LocalDateTime end) {

        long resArchive = residentRepository.countByStatusAndUpdatedDateBetween(
                ResidentStatus.ARCHIVED, start, end);

        long adminArchive = userRepository.countByRole_RoleNameAndStatusAndUpdatedAtBetween(
                "ADMIN", Status.ARCHIVED, start, end);

        long officerArchive = employeeRepository.countByStatusAndUpdatedAtBetween(
                Status.ARCHIVED, start, end);

        long userArchive = userRepository.countByStatusAndUpdatedAtBetween(
                Status.ARCHIVED, start, end);

        return new ArchiveSummaryDTO(resArchive, userArchive, officerArchive, adminArchive);
    }








}
