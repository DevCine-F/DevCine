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
    List<Seat> findByRoomIdAndIsActiveTrue(Integer roomId);

    @Modifying
    @Query("DELETE FROM Seat s WHERE s.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Integer roomId);
}
