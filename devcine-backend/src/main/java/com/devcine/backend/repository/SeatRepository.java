package com.devcine.backend.repository;

import com.devcine.backend.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {
    // Chỉ GHẾ bán được (loại bỏ ô lối đi AISLE). Dùng cho giữ ghế, tính giá, sơ đồ suất chiếu.
    // Dòng cũ cell_kind = NULL vẫn coi là ghế để tương thích ngược.
    @Query("SELECT s FROM Seat s JOIN FETCH s.seatType WHERE s.room.id = :roomId AND s.isActive = true " +
           "AND (s.cellKind IS NULL OR s.cellKind = 'SEAT')")
    List<Seat> findByRoomIdAndIsActiveTrue(@Param("roomId") Integer roomId);

    // TOÀN BỘ ô của phòng gồm cả lối đi (AISLE) — dùng cho trình thiết kế sơ đồ & dựng snapshot,
    // để FE có khung trọn vẹn, không phải tự đoán lối đi.
    @Query("SELECT s FROM Seat s JOIN FETCH s.seatType WHERE s.room.id = :roomId AND s.isActive = true")
    List<Seat> findLayoutByRoomId(@Param("roomId") Integer roomId);

    List<Seat> findByRoomId(Integer roomId);

    // Đếm ghế BÁN ĐƯỢC theo phòng (active + không bảo trì/khóa + không phải lối đi) cho nhiều phòng
    // một lần → tránh N+1. Trả [roomId, count].
    @Query("SELECT s.room.id, COUNT(s) FROM Seat s WHERE s.room.id IN :roomIds AND s.isActive = true " +
           "AND (s.cellKind IS NULL OR s.cellKind = 'SEAT') " +
           "AND (s.seatStatus IS NULL OR s.seatStatus = 'AVAILABLE') GROUP BY s.room.id")
    List<Object[]> countSellableSeatsByRoomIds(@Param("roomIds") Collection<Integer> roomIds);

    // Nạp ghế kèm loại ghế trong 1 truy vấn (tránh N+1 lazy khi tính giá lúc giữ ghế)
    @Query("SELECT s FROM Seat s JOIN FETCH s.seatType WHERE s.id IN :ids")
    List<Seat> findByIdInWithSeatType(@Param("ids") List<Integer> ids);

    @Modifying
    @Query("UPDATE Seat s SET s.isActive = false WHERE s.room.id = :roomId")
    void deactivateByRoomId(@Param("roomId") Integer roomId);

    // Xoá cứng toàn bộ ghế của một phòng (dùng khi xoá phòng)
    @Modifying
    @Query("UPDATE Seat s SET s.isActive = false WHERE s.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Integer roomId);
}
