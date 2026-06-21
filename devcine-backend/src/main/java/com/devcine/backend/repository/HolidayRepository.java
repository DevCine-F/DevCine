package com.devcine.backend.repository;

import com.devcine.backend.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Integer> {
    boolean existsByHolidayDate(LocalDate holidayDate);
    List<Holiday> findAllByOrderByHolidayDateAsc();
}
