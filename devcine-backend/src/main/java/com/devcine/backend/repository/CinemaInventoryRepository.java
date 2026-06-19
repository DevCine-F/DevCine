package com.devcine.backend.repository;

import com.devcine.backend.entity.CinemaInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CinemaInventoryRepository extends JpaRepository<CinemaInventory, Integer> {

    @Query("SELECT ci FROM CinemaInventory ci JOIN FETCH ci.cinema JOIN FETCH ci.fnbItem ORDER BY ci.id")
    List<CinemaInventory> findAllWithDetails();

    Optional<CinemaInventory> findByCinema_IdAndFnbItem_Id(Integer cinemaId, Integer fnbItemId);
}
