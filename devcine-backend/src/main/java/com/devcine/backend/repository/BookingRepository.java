package com.devcine.backend.repository;

import com.devcine.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    @Query("SELECT COALESCE(SUM(b.finalPrice), 0) FROM Booking b WHERE b.status = 'CONFIRMED' AND b.createdAt >= :startDate AND b.createdAt <= :endDate")
    BigDecimal sumRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(bs) FROM BookingSeat bs JOIN bs.booking b WHERE b.status = 'CONFIRMED' AND b.createdAt >= :startDate AND b.createdAt <= :endDate")
    long countTicketsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Gộp doanh thu theo ngày trong 1 query (thay 7 query trong vòng lặp dashboard)
    @Query("SELECT CAST(b.createdAt AS date), COALESCE(SUM(b.finalPrice), 0) FROM Booking b " +
           "WHERE b.status = 'CONFIRMED' AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
           "GROUP BY CAST(b.createdAt AS date)")
    List<Object[]> sumRevenueGroupedByDay(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Gộp số vé theo ngày trong 1 query (thay 7 query trong vòng lặp dashboard)
    @Query("SELECT CAST(b.createdAt AS date), COUNT(bs) FROM BookingSeat bs JOIN bs.booking b " +
           "WHERE b.status = 'CONFIRMED' AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
           "GROUP BY CAST(b.createdAt AS date)")
    List<Object[]> countTicketsGroupedByDay(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT m.title, COALESCE(SUM(b.finalPrice), 0), COUNT(bs) " +
           "FROM BookingSeat bs JOIN bs.booking b JOIN b.showtime s JOIN s.movie m " +
           "WHERE b.status = 'CONFIRMED' " +
           "GROUP BY m.id, m.title " +
           "ORDER BY COALESCE(SUM(b.finalPrice), 0) DESC")
    List<Object[]> findTopMoviesByRevenue();

    @Query("SELECT b FROM Booking b JOIN FETCH b.showtime s JOIN FETCH s.movie " +
           "WHERE b.customer.userId = :customerId ORDER BY b.createdAt DESC")
    List<Booking> findByCustomerIdWithDetails(@Param("customerId") Integer customerId);

    // Danh sách hoá đơn cho admin (lọc + phân trang). Param luôn non-null để tránh bẫy null-param Postgres.
    @Query(value = "SELECT b FROM Booking b " +
           "JOIN FETCH b.showtime s JOIN FETCH s.movie m JOIN FETCH s.room r " +
           "LEFT JOIN FETCH b.customer c LEFT JOIN FETCH c.user u " +
           "WHERE b.createdAt BETWEEN :from AND :to " +
           "AND (:status = '' OR b.status = :status) " +
           "AND (:method = '' OR b.paymentMethod = :method) " +
           "AND (:q = '' OR LOWER(b.bookingCode) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.fullName) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.username) LIKE CONCAT('%', LOWER(:q), '%')) " +
           "ORDER BY b.createdAt DESC",
           countQuery = "SELECT COUNT(b) FROM Booking b LEFT JOIN b.customer c LEFT JOIN c.user u " +
           "WHERE b.createdAt BETWEEN :from AND :to " +
           "AND (:status = '' OR b.status = :status) " +
           "AND (:method = '' OR b.paymentMethod = :method) " +
           "AND (:q = '' OR LOWER(b.bookingCode) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.fullName) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.username) LIKE CONCAT('%', LOWER(:q), '%'))")
    Page<Booking> searchForAdmin(@Param("q") String q, @Param("status") String status,
                                 @Param("method") String method,
                                 @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                 Pageable pageable);

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room JOIN FETCH s.format " +
           "LEFT JOIN FETCH b.customer c LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH b.voucher v LEFT JOIN FETCH v.promotion " +
           "WHERE b.id = :id")
    Optional<Booking> findDetailById(@Param("id") Integer id);

    @Query("SELECT bs.booking.id, COUNT(bs) FROM BookingSeat bs WHERE bs.booking.id IN :ids GROUP BY bs.booking.id")
    List<Object[]> countSeatsByBookingIds(@Param("ids") List<Integer> ids);
}
