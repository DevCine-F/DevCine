package com.devcine.backend.repository;

import com.devcine.backend.entity.BookingFnb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BookingFnbRepository extends JpaRepository<BookingFnb, Integer> {

    @Query("SELECT bf FROM BookingFnb bf JOIN FETCH bf.fnbItem WHERE bf.booking.id = :bookingId")
    List<BookingFnb> findByBookingIdWithFnb(@Param("bookingId") Integer bookingId);

    @Query("SELECT COALESCE(SUM(bf.priceSnapshot * bf.quantity), 0) FROM BookingFnb bf JOIN bf.booking b " +
           "WHERE b.status = 'CONFIRMED' AND b.staffSchedule.id = :staffScheduleId")
    BigDecimal sumFnbRevenueByStaffSchedule(@Param("staffScheduleId") Integer staffScheduleId);

    @Query("SELECT COUNT(DISTINCT b.id) FROM BookingFnb bf JOIN bf.booking b " +
           "WHERE b.status = 'CONFIRMED' AND b.staffSchedule.id = :staffScheduleId")
    long countFnbOrdersByStaffSchedule(@Param("staffScheduleId") Integer staffScheduleId);
}
