package com.devcine.backend.repository;

import com.devcine.backend.entity.BookingFnb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingFnbRepository extends JpaRepository<BookingFnb, Integer> {

    // Fetch kèm options (LEFT vì món có thể không có vị) để tránh N+1/LazyInit khi build detail.
    // DISTINCT vì LEFT JOIN FETCH collection có thể nhân bản dòng cha.
    @Query("SELECT DISTINCT bf FROM BookingFnb bf " +
           "JOIN FETCH bf.fnbItem " +
           "LEFT JOIN FETCH bf.options " +
           "WHERE bf.booking.id = :bookingId")
    List<BookingFnb> findByBookingIdWithFnb(@Param("bookingId") Integer bookingId);

    // Batch đếm số dòng F&B theo từng đơn cho màn danh sách (tránh N+1 giống countSeatsByBookingIds).
    @Query("SELECT bf.booking.id, COUNT(bf) FROM BookingFnb bf WHERE bf.booking.id IN :ids GROUP BY bf.booking.id")
    List<Object[]> countFnbByBookingIds(@Param("ids") List<Integer> ids);
}
