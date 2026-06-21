package com.devcine.backend.repository;

import com.devcine.backend.entity.CinemaInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CinemaInventoryRepository extends JpaRepository<CinemaInventory, Integer> {

    @Query("SELECT ci FROM CinemaInventory ci JOIN FETCH ci.cinema JOIN FETCH ci.fnbItem ORDER BY ci.id")
    List<CinemaInventory> findAllWithDetails();

    // Tồn kho dưới ngưỡng (cảnh báo dashboard) — JOIN FETCH tránh N+1
    @Query("SELECT ci FROM CinemaInventory ci JOIN FETCH ci.cinema JOIN FETCH ci.fnbItem " +
           "WHERE ci.inStock <= :threshold ORDER BY ci.inStock ASC")
    List<CinemaInventory> findLowStock(@Param("threshold") int threshold, org.springframework.data.domain.Pageable pageable);

    Optional<CinemaInventory> findByCinema_IdAndFnbItem_Id(Integer cinemaId, Integer fnbItemId);
}
