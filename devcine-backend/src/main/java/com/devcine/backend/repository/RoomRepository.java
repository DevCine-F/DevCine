package com.devcine.backend.repository;

import com.devcine.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    @Query("SELECT r FROM Room r JOIN FETCH r.cinema c WHERE c.id = :cinemaId")
    List<Room> findByCinemaId(@Param("cinemaId") Integer cinemaId);
}
