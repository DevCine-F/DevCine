package com.devcine.backend.repository;

import com.devcine.backend.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Integer> {

    @Query("SELECT bs FROM BookingSeat bs JOIN bs.booking b JOIN FETCH bs.seat " +
           "WHERE b.showtime.id = :showtimeId AND (bs.status = 'SOLD' OR bs.status = 'HOLD')")
    List<BookingSeat> findReservedSeatsByShowtime(@Param("showtimeId") Integer showtimeId);

    // Đếm ghế đã giữ/bán (SOLD/HOLD) cho nhiều suất một lần → tránh N+1 khi dựng card suất chiếu.
    // Trả [showtimeId, count].
    @Query("SELECT b.showtime.id, COUNT(bs) FROM BookingSeat bs JOIN bs.booking b " +
           "WHERE b.showtime.id IN :showtimeIds AND (bs.status = 'SOLD' OR bs.status = 'HOLD') " +
           "GROUP BY b.showtime.id")
    List<Object[]> countReservedByShowtimeIds(@Param("showtimeIds") Collection<Integer> showtimeIds);

    // Ghế đang HOLD nhưng đơn đã tạo trước mốc cutoff → quá hạn giữ, cần giải phóng
    @Query("SELECT bs FROM BookingSeat bs JOIN FETCH bs.booking b " +
           "WHERE bs.status = 'HOLD' AND b.createdAt < :cutoff")
    List<BookingSeat> findStaleHolds(@Param("cutoff") LocalDateTime cutoff);

    // Số ghế đã GIỮ/BÁN (SOLD/HOLD) của MỘT suất — guard trước khi xoá suất chiếu.
    @Query("SELECT COUNT(bs) FROM BookingSeat bs " +
           "WHERE bs.booking.showtime.id = :showtimeId AND (bs.status = 'SOLD' OR bs.status = 'HOLD')")
    long countReservedByShowtime(@Param("showtimeId") Integer showtimeId);

    // Số ghế đã BÁN (SOLD) của MỘT suất — vé thực bán cho drawer chi tiết.
    @Query("SELECT COUNT(bs) FROM BookingSeat bs " +
           "WHERE bs.booking.showtime.id = :showtimeId AND bs.status = 'SOLD'")
    long countSoldByShowtime(@Param("showtimeId") Integer showtimeId);

    // Số ghế đang GIỮ (HOLD) của MỘT suất.
    @Query("SELECT COUNT(bs) FROM BookingSeat bs " +
           "WHERE bs.booking.showtime.id = :showtimeId AND bs.status = 'HOLD'")
    long countHeldByShowtime(@Param("showtimeId") Integer showtimeId);

    // Doanh thu vé thực tế của MỘT suất = tổng giá chốt (priceSnapshot) các ghế đã BÁN.
    @Query("SELECT COALESCE(SUM(bs.priceSnapshot), 0) FROM BookingSeat bs " +
           "WHERE bs.booking.showtime.id = :showtimeId AND bs.status = 'SOLD'")
    java.math.BigDecimal sumSoldRevenueByShowtime(@Param("showtimeId") Integer showtimeId);

    List<BookingSeat> findAllByBookingId(Integer bookingId);

    @Query("SELECT bs FROM BookingSeat bs JOIN FETCH bs.seat s LEFT JOIN FETCH s.seatType " +
           "WHERE bs.booking.id = :bookingId ORDER BY s.rowChar, s.colNum")
    List<BookingSeat> findAllByBookingIdWithSeat(@Param("bookingId") Integer bookingId);

    @Query("SELECT bs.seat.id FROM BookingSeat bs JOIN bs.booking b " +
           "WHERE bs.seat.id IN :seatIds AND b.showtime.id = :showtimeId " +
           "AND (bs.status = 'SOLD' OR (bs.status = 'HOLD' AND b.expiresAt >= :now AND b.status IN ('PENDING_PAYMENT', 'PAYING', 'HOLD')))")
    List<Integer> findConflictingSeats(@Param("showtimeId") Integer showtimeId, @Param("seatIds") List<Integer> seatIds, @Param("now") LocalDateTime now);
}
