package com.devcine.backend.repository;

import com.devcine.backend.entity.ConcessionSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConcessionSaleRepository extends JpaRepository<ConcessionSale, Integer> {

    @Query("SELECT s FROM ConcessionSale s " +
           "LEFT JOIN FETCH s.customer c LEFT JOIN FETCH c.user u " +
           "LEFT JOIN s.soldBy st " +
           "LEFT JOIN FETCH s.cinema cin " +
           "WHERE s.createdAt BETWEEN :from AND :to " +
           "AND (:status = '' OR s.status = :status) " +
           "AND (:method = '' OR s.paymentMethod = :method) " +
           "AND (:staffUserId IS NULL OR st.userId = :staffUserId) " +
           "AND (:q = '' OR LOWER(s.saleCode) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.fullName) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.username) LIKE CONCAT('%', LOWER(:q), '%')) " +
           "ORDER BY s.createdAt DESC")
    List<ConcessionSale> searchForAdmin(
            @Param("q") String q, @Param("status") String status,
            @Param("method") String method,
            @Param("staffUserId") Integer staffUserId,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT s FROM ConcessionSale s " +
           "LEFT JOIN FETCH s.customer c LEFT JOIN FETCH c.user u " +
           "LEFT JOIN FETCH s.soldBy st LEFT JOIN FETCH st.user stu " +
           "LEFT JOIN FETCH s.cinema cin " +
           "WHERE s.id = :id")
    Optional<ConcessionSale> findDetailById(@Param("id") Integer id);

    @Query("SELECT s FROM ConcessionSale s " +
           "LEFT JOIN FETCH s.customer c LEFT JOIN FETCH c.user u " +
           "LEFT JOIN FETCH s.soldBy st LEFT JOIN FETCH st.user stu " +
           "LEFT JOIN FETCH s.cinema cin " +
           "WHERE s.saleCode = :code")
    Optional<ConcessionSale> findDetailBySaleCode(@Param("code") String code);

    /** Tổng chi tiêu và số đơn F&B COMPLETED theo danh sách customerIds (O(1) batch query). */
    @Query("SELECT s.customer.userId, COALESCE(SUM(s.totalPrice), 0), COUNT(s) " +
           "FROM ConcessionSale s WHERE s.status = 'COMPLETED' AND s.customer.userId IN :customerIds " +
           "GROUP BY s.customer.userId")
    List<Object[]> aggregateConcessionSpentAndCountByCustomerIds(@Param("customerIds") List<Integer> customerIds);

    /** Toàn bộ đơn bán nhanh F&B của 1 khách hàng (mới nhất trước). */
    @Query("SELECT s FROM ConcessionSale s " +
           "LEFT JOIN FETCH s.cinema cin " +
           "WHERE s.customer.userId = :customerId ORDER BY s.createdAt DESC")
    List<ConcessionSale> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") Integer customerId);

    /**
     * Kiểm tra nhanh khách có ConcessionSale COMPLETED tại cinema hay không
     * (dùng để guard endpoint detail khi staff không phải ADMIN).
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM ConcessionSale s WHERE s.customer.userId = :customerId AND s.status = 'COMPLETED' " +
           "AND s.cinema.id = :cinemaId")
    boolean existsCompletedByCinemaAndCustomer(@Param("cinemaId") Integer cinemaId,
                                               @Param("customerId") Integer customerId);
}

