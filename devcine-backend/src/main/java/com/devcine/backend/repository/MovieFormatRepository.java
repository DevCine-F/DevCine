package com.devcine.backend.repository;

import com.devcine.backend.entity.MovieFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieFormatRepository extends JpaRepository<MovieFormat, Integer> {
    boolean existsByNameIgnoreCase(String name);
}
