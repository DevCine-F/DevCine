package com.devcine.backend.repository;

import com.devcine.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    List<Room> findByCinemaId(Integer cinemaId);
}
