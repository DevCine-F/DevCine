package com.devcine.backend.repository;

import com.devcine.backend.entity.BookingFnb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingFnbRepository extends JpaRepository<BookingFnb, Integer> {

    @Query("SELECT bf FROM BookingFnb bf JOIN FETCH bf.fnbItem WHERE bf.booking.id = :bookingId")
    List<BookingFnb> findByBookingIdWithFnb(@Param("bookingId") Integer bookingId);
}
