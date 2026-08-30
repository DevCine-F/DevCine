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

    @Query("""
            SELECT t FROM Ticket t
            JOIN FETCH t.bookingSeat bs
            JOIN FETCH bs.seat
            JOIN FETCH bs.booking b
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room r
            JOIN FETCH r.cinema
            WHERE t.qrCode = :qrCode
            """)
    Optional<Ticket> findByQrCodeWithDetails(@Param("qrCode") String qrCode);

    Optional<Ticket> findByBookingSeatId(Integer bookingSeatId);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.bookingSeat bs WHERE bs.id IN :bookingSeatIds")
    List<Ticket> findByBookingSeatIds(@Param("bookingSeatIds") List<Integer> bookingSeatIds);

    // JOIN FETCH bs.seat để tránh N+1 khi printByBookingCode loop qua tickets và đọc seat để check-in
    @Query("SELECT t FROM Ticket t JOIN FETCH t.bookingSeat bs JOIN FETCH bs.seat JOIN FETCH bs.booking b WHERE b.id = :bookingId")
    List<Ticket> findAllByBookingId(@Param("bookingId") Integer bookingId);

    // Như trên nhưng JOIN FETCH thêm ghế và loại ghế để lấy nhãn ghế và loại ghế khi gửi mail/in vé — tránh N+1
    @Query("SELECT t FROM Ticket t JOIN FETCH t.bookingSeat bs JOIN FETCH bs.seat s LEFT JOIN FETCH s.seatType WHERE bs.booking.id = :bookingId ORDER BY s.rowChar, s.colNum")
    List<Ticket> findAllByBookingIdWithSeat(@Param("bookingId") Integer bookingId);
}
