package com.datapulse.repository;

import com.datapulse.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    @Query(value = "SELECT * FROM audit_logs a WHERE " +
           "(CAST(:userId AS text) IS NULL OR a.user_id = CAST(:userId AS text)) AND " +
           "(CAST(:action AS text) IS NULL OR a.action = CAST(:action AS text)) AND " +
           "(CAST(:fromDt AS timestamp) IS NULL OR a.created_at >= CAST(:fromDt AS timestamp)) AND " +
           "(CAST(:toDt AS timestamp) IS NULL OR a.created_at < CAST(:toDt AS timestamp)) " +
           "ORDER BY a.created_at DESC",
           countQuery = "SELECT count(*) FROM audit_logs a WHERE " +
           "(CAST(:userId AS text) IS NULL OR a.user_id = CAST(:userId AS text)) AND " +
           "(CAST(:action AS text) IS NULL OR a.action = CAST(:action AS text)) AND " +
           "(CAST(:fromDt AS timestamp) IS NULL OR a.created_at >= CAST(:fromDt AS timestamp)) AND " +
           "(CAST(:toDt AS timestamp) IS NULL OR a.created_at < CAST(:toDt AS timestamp))",
           nativeQuery = true)
    Page<AuditLog> findFiltered(
            @Param("userId") String userId,
            @Param("action") String action,
            @Param("fromDt") LocalDateTime from,
            @Param("toDt") LocalDateTime to,
            Pageable pageable);
}
