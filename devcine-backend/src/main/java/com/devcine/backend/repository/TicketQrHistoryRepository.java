package com.devcine.backend.repository;

import com.devcine.backend.entity.TicketQrHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketQrHistoryRepository extends JpaRepository<TicketQrHistory, Integer> {

    @Query("""
            SELECT h FROM TicketQrHistory h
            JOIN FETCH h.ticket t
            JOIN FETCH t.bookingSeat bs
            JOIN FETCH bs.seat
            JOIN FETCH bs.booking b
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room r
            JOIN FETCH r.cinema
            WHERE h.qrCode = :qrCode
            """)
    Optional<TicketQrHistory> findByQrCodeWithDetails(@Param("qrCode") String qrCode);
}
