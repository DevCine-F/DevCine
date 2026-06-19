package com.devcine.backend.repository;

import com.devcine.backend.entity.StaffSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StaffScheduleRepository extends JpaRepository<StaffSchedule, Integer> {

    @Query("SELECT ss FROM StaffSchedule ss JOIN FETCH ss.staff s JOIN FETCH s.user JOIN FETCH ss.shift WHERE ss.workDate = :date ORDER BY ss.id")
    List<StaffSchedule> findByWorkDateWithDetails(@Param("date") LocalDate date);
}
