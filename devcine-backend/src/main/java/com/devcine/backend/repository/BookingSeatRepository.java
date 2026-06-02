package com.devcine.backend.repository;

import com.devcine.backend.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Integer> {
    
    @Query("SELECT bs FROM BookingSeat bs JOIN bs.booking b " +
           "WHERE b.showtime.id = :showtimeId AND (bs.status = 'SOLD' OR bs.status = 'HOLD')")
    List<BookingSeat> findReservedSeatsByShowtime(@Param("showtimeId") Integer showtimeId);

    List<BookingSeat> findAllByBookingId(Integer bookingId);
}
