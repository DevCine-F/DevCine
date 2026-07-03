package com.devcine.backend.repository;

import com.devcine.backend.entity.StaffSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffScheduleRepository extends JpaRepository<StaffSchedule, Integer> {

    @Query("""
            SELECT ss FROM StaffSchedule ss
            JOIN FETCH ss.staff s
            JOIN FETCH s.user u
            LEFT JOIN FETCH u.role
            JOIN FETCH ss.shift sh
            LEFT JOIN FETCH ss.cinema c
            WHERE ss.workDate = :date
              AND (:cinemaId IS NULL OR c.id = :cinemaId)
              AND (:status IS NULL OR ss.status = :status)
            ORDER BY sh.startTime ASC, ss.id ASC
            """)
    List<StaffSchedule> findByWorkDateWithDetails(
            @Param("date") LocalDate date,
            @Param("cinemaId") Integer cinemaId,
            @Param("status") String status);

    @Query("""
            SELECT ss FROM StaffSchedule ss
            JOIN FETCH ss.staff s
            JOIN FETCH s.user u
            LEFT JOIN FETCH u.role
            JOIN FETCH ss.shift sh
            LEFT JOIN FETCH ss.cinema c
            WHERE s.userId = :staffId
              AND ss.workDate >= :fromDate
              AND ss.workDate <= :toDate
            ORDER BY ss.workDate ASC, sh.startTime ASC, ss.id ASC
            """)
    List<StaffSchedule> findMyShiftsWithDetails(
            @Param("staffId") Integer staffId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT CASE WHEN COUNT(ss) > 0 THEN true ELSE false END FROM StaffSchedule ss
            JOIN ss.shift sh
            WHERE ss.staff.userId = :staffId
              AND ss.workDate = :workDate
              AND ss.status <> 'REJECTED'
              AND sh.startTime < :endAt
              AND sh.endTime > :startAt
            """)
    boolean existsOverlappingSchedule(
            @Param("staffId") Integer staffId,
            @Param("workDate") LocalDate workDate,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    @Query("""
            SELECT ss FROM StaffSchedule ss
            JOIN FETCH ss.staff s
            JOIN FETCH s.user u
            LEFT JOIN FETCH u.role
            JOIN FETCH ss.shift sh
            LEFT JOIN FETCH ss.cinema c
            WHERE s.userId = :staffId
              AND ss.status = 'APPROVED'
              AND sh.startTime <= :now
              AND sh.endTime >= :now
            ORDER BY sh.startTime DESC
            """)
    List<StaffSchedule> findCurrentApprovedSchedules(
            @Param("staffId") Integer staffId,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT ss FROM StaffSchedule ss
            JOIN FETCH ss.staff s
            JOIN FETCH s.user u
            LEFT JOIN FETCH u.role
            JOIN FETCH ss.shift sh
            LEFT JOIN FETCH ss.cinema c
            WHERE ss.id = :id
            """)
    Optional<StaffSchedule> findByIdWithDetails(@Param("id") Integer id);
}
