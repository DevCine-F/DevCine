package com.devcine.backend.repository;

import com.devcine.backend.entity.SpecialSeatPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialSeatPriceRepository extends JpaRepository<SpecialSeatPrice, Integer> {

    @Query("SELECT s FROM SpecialSeatPrice s JOIN FETCH s.format JOIN FETCH s.seatType")
    List<SpecialSeatPrice> findAllWithRefs();
}
