package com.barangay.barangay.admin_management.repository;

import com.barangay.barangay.employee.model.Employee;
import com.barangay.barangay.enumerated.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemAdminEmployeeRepository extends JpaRepository<Employee, Long> {

    long countByStatus(Status status);


    @Query(value = """
    SELECT COUNT(*) as count, period 
    FROM (
        SELECT TO_CHAR(created_at, :format) as period, 
               date_trunc(:trunc, created_at) as trunc_date
        FROM employees 
        WHERE status = 'ACTIVE' 
        AND created_at BETWEEN :start AND :end
    ) subquery
    GROUP BY period, trunc_date 
    ORDER BY trunc_date
    """, nativeQuery = true)
    List<Object[]> getEmployeeTrend(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("format") String format,
            @Param("trunc") String trunc
    );



    long countByStatusAndCreatedAtBetween(Status status, LocalDateTime start, LocalDateTime end);

    long countByStatusAndUpdatedAtBetween(Status status, LocalDateTime start, LocalDateTime end);
}
