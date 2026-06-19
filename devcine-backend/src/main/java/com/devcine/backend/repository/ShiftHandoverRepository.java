package com.devcine.backend.repository;

import com.devcine.backend.entity.ShiftHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftHandoverRepository extends JpaRepository<ShiftHandover, Integer> {

    @Query("SELECT h FROM ShiftHandover h JOIN FETCH h.staffSchedule ss JOIN FETCH ss.staff s JOIN FETCH s.user ORDER BY h.id DESC")
    List<ShiftHandover> findAllWithDetails();
}
