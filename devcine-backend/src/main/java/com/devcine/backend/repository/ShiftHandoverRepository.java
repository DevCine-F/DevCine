package com.devcine.backend.repository;

import com.devcine.backend.entity.ShiftHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftHandoverRepository extends JpaRepository<ShiftHandover, Integer> {

    @Query("SELECT h FROM ShiftHandover h JOIN FETCH h.staffSchedule ss JOIN FETCH ss.staff s JOIN FETCH s.user " +
           "JOIN FETCH ss.shift LEFT JOIN FETCH ss.cinema LEFT JOIN FETCH h.approvedByManager m LEFT JOIN FETCH m.user " +
           "ORDER BY h.id DESC")
    List<ShiftHandover> findAllWithDetails();

    @Query("SELECT h FROM ShiftHandover h JOIN FETCH h.staffSchedule ss JOIN FETCH ss.staff s JOIN FETCH s.user " +
           "JOIN FETCH ss.shift LEFT JOIN FETCH ss.cinema LEFT JOIN FETCH h.approvedByManager m LEFT JOIN FETCH m.user " +
           "WHERE h.id = :id")
    Optional<ShiftHandover> findByIdWithDetails(@Param("id") Integer id);

    Optional<ShiftHandover> findTopByStaffScheduleIdOrderByIdDesc(Integer staffScheduleId);

    boolean existsByStaffScheduleIdAndStatus(Integer staffScheduleId, String status);
}
