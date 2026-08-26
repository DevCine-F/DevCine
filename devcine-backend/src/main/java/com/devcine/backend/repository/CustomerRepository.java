package com.devcine.backend.repository;

import com.devcine.backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    /** Tất cả khách hàng kèm thông tin user (JOIN FETCH tránh N+1). */
    @Query("SELECT c FROM Customer c JOIN FETCH c.user u ORDER BY u.createdAt DESC")
    List<Customer> findAllWithUser();

    /** Tra cứu khách hàng theo số điện thoại (POS tích điểm). */
    @Query("SELECT c FROM Customer c JOIN FETCH c.user u WHERE u.phone = :phone")
    java.util.Optional<Customer> findFirstByUserPhone(@Param("phone") String phone);

    /** Tra cứu danh sách khách hàng theo số điện thoại (tương thích cũ). */
    @Query("SELECT c FROM Customer c JOIN FETCH c.user u WHERE u.phone = :phone")
    List<Customer> findByUserPhone(@Param("phone") String phone);

    /** Lọc khách hàng theo từ khoá tên/email/sđt (q không null). */
    @Query("SELECT c FROM Customer c JOIN FETCH c.user u WHERE " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR u.phone LIKE CONCAT('%', :q, '%') " +
           "ORDER BY u.createdAt DESC")
    List<Customer> searchWithUser(@Param("q") String q);
}
