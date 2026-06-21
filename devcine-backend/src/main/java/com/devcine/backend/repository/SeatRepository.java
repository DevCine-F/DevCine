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

    // Nạp ghế kèm loại ghế trong 1 truy vấn (tránh N+1 lazy khi tính giá lúc giữ ghế)
    @Query("SELECT s FROM Seat s JOIN FETCH s.seatType WHERE s.id IN :ids")
    List<Seat> findByIdInWithSeatType(@Param("ids") List<Integer> ids);

    @Modifying
    @Query("UPDATE Seat s SET s.isActive = false WHERE s.room.id = :roomId")
    void deactivateByRoomId(@Param("roomId") Integer roomId);
}
