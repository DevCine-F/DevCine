package com.devcine.backend.repository;

import com.devcine.backend.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    
    @Query("SELECT v FROM Voucher v JOIN FETCH v.promotion WHERE v.customer.userId = :customerId AND v.isUsed = false AND v.validUntil > CURRENT_TIMESTAMP")
    List<Voucher> findActiveVouchersByCustomerId(@Param("customerId") Integer customerId);

    @Query("SELECT v FROM Voucher v JOIN FETCH v.promotion WHERE v.customer.userId = :customerId AND v.promotion.code = :code AND v.isUsed = false AND v.validUntil > CURRENT_TIMESTAMP")
    Optional<Voucher> findActiveVoucherByCustomerAndCode(@Param("customerId") Integer customerId, @Param("code") String code);

    @Query("SELECT v FROM Voucher v JOIN FETCH v.promotion WHERE v.customer.userId = :customerId ORDER BY v.validUntil DESC")
    List<Voucher> findAllByCustomerIdWithPromotion(@Param("customerId") Integer customerId);
}
