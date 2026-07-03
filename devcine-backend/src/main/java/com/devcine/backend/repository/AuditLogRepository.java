package com.devcine.backend.repository;

import com.devcine.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    // Tách 2 query: tránh param null (:action IS NULL OR ...) gây lỗi type trên Postgres. JOIN FETCH user+role tránh N+1.
    @Query(value = "SELECT a FROM AuditLog a LEFT JOIN FETCH a.user u LEFT JOIN FETCH u.role " +
           "LEFT JOIN FETCH a.staffSchedule ss LEFT JOIN FETCH ss.shift LEFT JOIN FETCH ss.cinema " +
           "ORDER BY a.timestamp DESC",
           countQuery = "SELECT COUNT(a) FROM AuditLog a")
    Page<AuditLog> findAllWithUser(Pageable pageable);

    @Query(value = "SELECT a FROM AuditLog a LEFT JOIN FETCH a.user u LEFT JOIN FETCH u.role " +
           "LEFT JOIN FETCH a.staffSchedule ss LEFT JOIN FETCH ss.shift LEFT JOIN FETCH ss.cinema " +
           "WHERE a.action = :action ORDER BY a.timestamp DESC",
           countQuery = "SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action")
    Page<AuditLog> findByActionWithUser(@Param("action") String action, Pageable pageable);
}
