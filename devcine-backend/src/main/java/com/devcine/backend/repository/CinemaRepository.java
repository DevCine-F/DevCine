package com.devcine.backend.repository;

import com.devcine.backend.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Integer> {

    @Query("SELECT DISTINCT c.city FROM Cinema c WHERE c.city IS NOT NULL")
    List<String> findAllCities();
}
