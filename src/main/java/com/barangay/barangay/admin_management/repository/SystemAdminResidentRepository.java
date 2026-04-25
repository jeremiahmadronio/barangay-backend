package com.barangay.barangay.admin_management.repository;

import com.barangay.barangay.enumerated.ResidentStatus;
import com.barangay.barangay.person.model.Resident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemAdminResidentRepository extends JpaRepository<Resident,Long> {

    long countByStatus(ResidentStatus status);


    @Query(value = """
    SELECT COUNT(*) as count, period 
    FROM (
        SELECT TO_CHAR(created_at, :format) as period, 
               date_trunc(:trunc, created_at) as trunc_date
        FROM resident 
        WHERE status = 'ACTIVE' 
        AND created_at BETWEEN :start AND :end
    ) subquery
    GROUP BY period, trunc_date 
    ORDER BY trunc_date
    """, nativeQuery = true)
    List<Object[]> getResidentTrend(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("format") String format,
            @Param("trunc") String trunc
    );



    long countByStatusAndCreatedDateBetween(ResidentStatus status, LocalDateTime start, LocalDateTime end);

    long countByStatusAndUpdatedDateBetween(ResidentStatus status, LocalDateTime start, LocalDateTime end);



    @Query(value = """
    SELECT COUNT(*) as count, 
           TO_CHAR(date_trunc('month', created_at), 'Mon') as month_label,
           date_trunc('month', created_at) as sort_date
    FROM resident
    WHERE created_at >= CURRENT_DATE - INTERVAL '5 months'
    AND status = 'ACTIVE'
    GROUP BY month_label, sort_date
    ORDER BY sort_date ASC
""", nativeQuery = true)
    List<Object[]> getRawResidentTrend();
}
