package com.devcine.backend.repository;

import com.devcine.backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    /** Tất cả khách hàng kèm thông tin user (JOIN FETCH tránh N+1). ADMIN only. */
    @Query("SELECT c FROM Customer c JOIN FETCH c.user u ORDER BY u.createdAt DESC")
    List<Customer> findAllWithUser();

    /** Tra cứu khách hàng theo số điện thoại (POS tích điểm). */
    @Query("SELECT c FROM Customer c JOIN FETCH c.user u WHERE u.phone = :phone")
    java.util.Optional<Customer> findFirstByUserPhone(@Param("phone") String phone);

    /** Tra cứu danh sách khách hàng theo số điện thoại (tương thích cũ). */
    @Query("SELECT c FROM Customer c JOIN FETCH c.user u WHERE u.phone = :phone")
    List<Customer> findByUserPhone(@Param("phone") String phone);

    /** Lọc khách hàng theo từ khoá tên/email/sđt/id (q không null). ADMIN only. */
    @Query("SELECT c FROM Customer c JOIN FETCH c.user u WHERE " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR u.phone LIKE CONCAT('%', :q, '%') " +
           "OR CAST(c.userId AS string) LIKE CONCAT('%', :q, '%') " +
           "ORDER BY u.createdAt DESC")
    List<Customer> searchWithUser(@Param("q") String q);

    // ===== Cinema-scoped queries (Manager / Staff) =====

    /**
     * Khách hàng ĐÃ TỪNG giao dịch tại cinema chỉ định.
     * "Giao dịch" = Booking CONFIRMED tại rạp đó, hoặc ConcessionSale COMPLETED tại rạp đó.
     * Dùng EXISTS subquery để tránh duplicate khi khách có nhiều đơn.
     */
    @Query("SELECT c FROM Customer c JOIN FETCH c.user u " +
           "WHERE EXISTS (" +
           "  SELECT 1 FROM Booking b WHERE b.customer = c AND b.status = 'CONFIRMED' " +
           "  AND b.showtime.room.cinema.id = :cinemaId" +
           ") OR EXISTS (" +
           "  SELECT 1 FROM ConcessionSale s WHERE s.customer = c AND s.status = 'COMPLETED' " +
           "  AND s.cinema.id = :cinemaId" +
           ") ORDER BY u.createdAt DESC")
    List<Customer> findByCinemaScope(@Param("cinemaId") Integer cinemaId);

    /**
     * Tìm kiếm khách hàng theo từ khoá, giới hạn trong phạm vi cinema chỉ định.
     */
    @Query("SELECT c FROM Customer c JOIN FETCH c.user u " +
           "WHERE (EXISTS (" +
           "  SELECT 1 FROM Booking b WHERE b.customer = c AND b.status = 'CONFIRMED' " +
           "  AND b.showtime.room.cinema.id = :cinemaId" +
           ") OR EXISTS (" +
           "  SELECT 1 FROM ConcessionSale s WHERE s.customer = c AND s.status = 'COMPLETED' " +
           "  AND s.cinema.id = :cinemaId" +
           ")) AND (" +
           "  LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "  OR u.phone LIKE CONCAT('%', :q, '%') " +
           "  OR CAST(c.userId AS string) LIKE CONCAT('%', :q, '%')" +
           ") ORDER BY u.createdAt DESC")
    List<Customer> searchByCinemaScope(@Param("q") String q, @Param("cinemaId") Integer cinemaId);

    /**
     * Kiểm tra nhanh khách có giao dịch tại cinema hay không (dùng để guard endpoint detail).
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
           "FROM Booking b WHERE b.customer.userId = :customerId AND b.status = 'CONFIRMED' " +
           "AND b.showtime.room.cinema.id = :cinemaId")
    boolean existsBookingByCinemaAndCustomer(@Param("cinemaId") Integer cinemaId,
                                             @Param("customerId") Integer customerId);
}
