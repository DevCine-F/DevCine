package com.devcine.backend.repository;

import com.devcine.backend.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Integer> {

    @Query("SELECT l FROM InventoryLog l JOIN FETCH l.cinemaInventory ci JOIN FETCH ci.fnbItem ORDER BY l.timestamp DESC")
    List<InventoryLog> findAllWithDetails();
}
