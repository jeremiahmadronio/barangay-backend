package com.barangay.barangay.admin_management.service;

import com.barangay.barangay.admin_management.dto.dashboard.ResidentTrendDTO;
import com.barangay.barangay.admin_management.repository.SystemAdminResidentRepository;
import com.barangay.barangay.audit.repository.AuditLogRepository;
import com.barangay.barangay.admin_management.dto.ActivityOverviewDTO;
import com.barangay.barangay.admin_management.dto.DashboardStats;
import com.barangay.barangay.admin_management.dto.DeptActivityDTO;
import com.barangay.barangay.admin_management.dto.RecentSystemAction;
import com.barangay.barangay.admin_management.repository.Root_AdminRepository;
import com.barangay.barangay.employee.repository.EmployeeRepository;
import com.barangay.barangay.enumerated.ResidentStatus;
import com.barangay.barangay.enumerated.Status;
import com.barangay.barangay.person.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RootDashboardService {

    private final Root_AdminRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final SystemAdminResidentRepository residentRepository;
    private final EmployeeRepository employeeRepository;




    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDayThisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime firstDayLastMonth = firstDayThisMonth.minusMonths(1);

        List<String> excludedRoles = List.of("ROOT_ADMIN");

        Long managedUsers = userRepository.countUsersExcludingRoles(excludedRoles);

        Long activeResidents = residentRepository.countByStatus(ResidentStatus.ACTIVE);

        Long activeEmployees = employeeRepository.countByStatus(Status.ACTIVE);

        Long totalAudits = auditLogRepository.count();

        Long currentMonthAudits = auditLogRepository.countLogsThisMonth(firstDayThisMonth);
        Long lastMonthAudits = auditLogRepository.countLogsLastMonth(firstDayLastMonth, firstDayThisMonth);

        long growth = currentMonthAudits - lastMonthAudits;



        String direction = (growth > 0) ? "up" : (growth < 0) ? "down" : "neutral";

        return new DashboardStats(
                managedUsers,
                activeResidents,
                activeEmployees,
                totalAudits,
                Math.abs(growth),
                direction
        );
    }

    @Transactional(readOnly = true)
    public ActivityOverviewDTO getActivityOverview() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Object[]> rawData = auditLogRepository.countLogsByDepartment(thirtyDaysAgo);

        long totalCount = rawData.stream()
                .mapToLong(row -> (long) row[1])
                .sum();

        List<DeptActivityDTO> deptActivities = rawData.stream().map(row -> {

            String deptName = row[0] != null ? String.valueOf(row[0]) : "Unknown";

            long count = (long) row[1];

            BigDecimal percentage = totalCount > 0
                    ? BigDecimal.valueOf(count)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            return new DeptActivityDTO(deptName, count, percentage);
        }).toList();

        return new ActivityOverviewDTO(totalCount, deptActivities);
    }



    @Transactional(readOnly = true)
    public List<RecentSystemAction> getRecentActions() {
        return auditLogRepository.findTop5RecentActions();
    }


    public ResidentTrendDTO getResidentGrowthTrend() {
        List<Object[]> results = residentRepository.getRawResidentTrend();

        Map<String, Long> countMap = results.stream()
                .collect(Collectors.toMap(
                        res -> res[1].toString(), // Month Label (e.g., "Nov")
                        res -> ((Number) res[0]).longValue() // Count
                ));

        List<String> labels = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM");

        for (int i = 5; i >= 0; i--) {
            String monthLabel = LocalDateTime.now().minusMonths(i).format(formatter);
            labels.add(monthLabel);
            counts.add(countMap.getOrDefault(monthLabel, 0L));
        }

        return new ResidentTrendDTO(labels, counts);
    }
}
