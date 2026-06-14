package com.devcine.backend.repository;

import com.devcine.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    @Query("SELECT COALESCE(SUM(b.finalPrice), 0) FROM Booking b WHERE b.status = 'CONFIRMED' AND b.createdAt >= :startDate AND b.createdAt <= :endDate")
    BigDecimal sumRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(bs) FROM BookingSeat bs JOIN bs.booking b WHERE b.status = 'CONFIRMED' AND b.createdAt >= :startDate AND b.createdAt <= :endDate")
    long countTicketsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT m.title, COALESCE(SUM(b.finalPrice), 0), COUNT(bs) " +
           "FROM BookingSeat bs JOIN bs.booking b JOIN b.showtime s JOIN s.movie m " +
           "WHERE b.status = 'CONFIRMED' " +
           "GROUP BY m.id, m.title " +
           "ORDER BY COALESCE(SUM(b.finalPrice), 0) DESC")
    List<Object[]> findTopMoviesByRevenue();
}
