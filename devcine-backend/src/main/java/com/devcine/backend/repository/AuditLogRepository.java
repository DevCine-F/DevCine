package com.devcine.backend.repository;

import com.devcine.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    @Query(value = "SELECT a FROM AuditLog a LEFT JOIN FETCH a.user u LEFT JOIN FETCH u.role WHERE " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:from IS NULL OR a.timestamp >= :from) AND " +
           "(:to IS NULL OR a.timestamp <= :to) " +
           "ORDER BY a.timestamp DESC",
           countQuery = "SELECT COUNT(a) FROM AuditLog a WHERE " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:from IS NULL OR a.timestamp >= :from) AND " +
           "(:to IS NULL OR a.timestamp <= :to)")
    Page<AuditLog> findWithFilters(@Param("action") String action,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   Pageable pageable);
}
