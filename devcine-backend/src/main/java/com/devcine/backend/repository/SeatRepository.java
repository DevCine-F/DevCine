package com.devcine.backend.repository;

import com.devcine.backend.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {
    @Query("SELECT s FROM Seat s JOIN FETCH s.seatType WHERE s.room.id = :roomId AND s.isActive = true")
    List<Seat> findByRoomIdAndIsActiveTrue(@Param("roomId") Integer roomId);

    @Modifying
    @Query("DELETE FROM Seat s WHERE s.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Integer roomId);
}
