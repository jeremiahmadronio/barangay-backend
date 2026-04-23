package com.barangay.barangay.audit.repository;

import com.barangay.barangay.audit.model.AuditLog;
import com.barangay.barangay.admin_management.dto.RecentSystemAction;
import com.barangay.barangay.enumerated.Departments;
import com.barangay.barangay.user_management.dto.AdminDashboardActivityByDepartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog,Long>  {





    Long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT COUNT(a) FROM AuditLog a")
    Long countAllLogs();

    @Query("SELECT DISTINCT a.module FROM AuditLog a WHERE a.module IS NOT NULL ORDER BY a.module")
    List<String> findDistinctModules();

    @Query("SELECT DISTINCT a.actionTaken FROM AuditLog a WHERE a.actionTaken IS NOT NULL ORDER BY a.actionTaken")
    List<String> findDistinctActions();

    @Query("SELECT DISTINCT CAST(a.severity AS string) FROM AuditLog a WHERE a.severity IS NOT NULL ORDER BY CAST(a.severity AS string)")
    List<String> findDistinctSeverities();

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt >= :startOfDay")
    Long countLogsToday(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.severity = com.barangay.barangay.enumerated.Severity.WARNING")
    Long countWarningAlerts();

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.severity = com.barangay.barangay.enumerated.Severity.CRITICAL")
    Long countCriticalAlerts();

    // Para sa growth computation
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt >= :startOfMonth")
    Long countLogsThisMonth(@Param("startOfMonth") LocalDateTime startOfMonth);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt >= :startOfLastMonth AND a.createdAt < :startOfMonth")
    Long countLogsLastMonth(@Param("startOfLastMonth") LocalDateTime lastMonth, @Param("startOfMonth") LocalDateTime thisMonth);


    @Query("SELECT l.department, COUNT(l) FROM AuditLog l WHERE l.createdAt >= :startDate AND l.department IS NOT NULL GROUP BY l.department")
    List<Object[]> countLogsByDepartment(@Param("startDate") LocalDateTime startDate);

    @Query("""
            SELECT a FROM AuditLog a
            JOIN FETCH a.user u
            LEFT JOIN FETCH u.role
            WHERE a.id = :id
            """)
    Optional<AuditLog> findByIdWithDetails(@Param("id") Long id);



    List<AuditLog> findTop5ByDepartmentAndCreatedAtAfterOrderByCreatedAtDesc(
            Departments department,
            LocalDateTime date);



    @Query("""
        SELECT new com.barangay.barangay.admin_management.dto.RecentSystemAction(
            p.firstName,
            p.lastName,
            CAST(a.severity AS string),
            a.actionTaken,
            a.module,
            a.createdAt
        )
        FROM AuditLog a
            JOIN a.user u
            JOIN u.person p
        ORDER BY a.createdAt DESC
        LIMIT 7
    """)
    List<RecentSystemAction> findTop5RecentActions();


    @Query(value = """
    SELECT al.* FROM audit_logs al
    LEFT JOIN users u ON u.id = al.user_id
    LEFT JOIN person p ON p.id = u.person_id
    WHERE (CAST(:search AS text) IS NULL OR (
          p.first_name ILIKE CONCAT('%', CAST(:search AS text), '%') OR 
          p.last_name ILIKE CONCAT('%', CAST(:search AS text), '%') OR 
          al.reason ILIKE CONCAT('%', CAST(:search AS text), '%') OR
          al.ip_address ILIKE CONCAT('%', CAST(:search AS text), '%')
    ))
    AND (CAST(:severity AS text) IS NULL OR al.severity = :severity)
    AND (CAST(:module AS text) IS NULL OR al.module = :module)
    AND (CAST(:action AS text) IS NULL OR al.action_taken = :action)
    AND (CAST(:startDate AS timestamp) IS NULL OR al.created_at >= :startDate)
    AND (CAST(:endDate AS timestamp) IS NULL OR al.created_at <= :endDate)
""",
            countQuery = """
    SELECT COUNT(*) FROM audit_logs al
    LEFT JOIN users u ON u.id = al.user_id
    LEFT JOIN person p ON p.id = u.person_id
    WHERE (CAST(:search AS text) IS NULL OR (
          p.first_name ILIKE CONCAT('%', CAST(:search AS text), '%') OR 
          p.last_name ILIKE CONCAT('%', CAST(:search AS text), '%') OR 
          al.reason ILIKE CONCAT('%', CAST(:search AS text), '%')
    ))
    AND (CAST(:severity AS text) IS NULL OR al.severity = :severity)
""", nativeQuery = true)
    Page<AuditLog> findAllFiltered(
            @Param("search")    String search,
            @Param("severity")  String severity,
            @Param("module")    String module,
            @Param("action")    String action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate,
            Pageable pageable
    );


    @Query("""
        SELECT a.department, COUNT(a)
        FROM AuditLog a
        WHERE a.department IN :depts 
        AND a.createdAt >= :startDate
        GROUP BY a.department
    """)
    List<Object[]> getActivityCountsByDept(
            @Param("depts") Set<Departments> depts,
            @Param("startDate") LocalDateTime startDate
    );
}



