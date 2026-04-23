package com.barangay.barangay.admin_management.repository;

import com.barangay.barangay.admin_management.dto.report.SeverityReportDTO;
import com.barangay.barangay.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemAdminAuditLogRepository extends JpaRepository<AuditLog, Long> {
    Long countByCreatedAtAfter(LocalDateTime date);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT new com.barangay.barangay.admin_management.dto.report.SeverityReportDTO(CAST(a.severity AS string), COUNT(a)) " +
            "FROM AuditLog a " +
            "WHERE a.createdAt BETWEEN :start AND :end " +
            "GROUP BY a.severity")
    List<SeverityReportDTO> getSeverityDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
