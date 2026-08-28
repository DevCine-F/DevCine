package com.devcine.backend.repository;

import com.devcine.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    @Query("SELECT r FROM Room r JOIN FETCH r.cinema c WHERE c.id = :cinemaId ORDER BY r.name ASC, r.id ASC")
    List<Room> findByCinemaId(@Param("cinemaId") Integer cinemaId);

    /** Đếm số phòng thực tế của một cụm rạp (đồng bộ số phòng hiển thị). */
    long countByCinema_Id(Integer cinemaId);

    // Trùng tên phòng trong cùng 1 cụm rạp (không phân biệt hoa/thường)
    boolean existsByCinema_IdAndNameIgnoreCase(Integer cinemaId, String name);
    boolean existsByCinema_IdAndNameIgnoreCaseAndIdNot(Integer cinemaId, String name, Integer id);
}
