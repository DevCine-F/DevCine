package com.devcine.backend.repository;

import com.devcine.backend.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    
    Optional<Ticket> findByQrCode(String qrCode);

    Optional<Ticket> findByBookingSeatId(Integer bookingSeatId);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.bookingSeat bs JOIN FETCH bs.booking b WHERE b.id = :bookingId")
    List<Ticket> findAllByBookingId(@Param("bookingId") Integer bookingId);
}
