package com.devcine.backend.repository;

import com.devcine.backend.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    // Dùng :now (LocalDateTime.now() từ JVM) thay CURRENT_TIMESTAMP để KHỚP múi giờ với phần còn lại
    // của ứng dụng (BookingService, getAllVouchers). CURRENT_TIMESTAMP lấy giờ DB (UTC) gây lệch +7h.
    @Query("SELECT v FROM Voucher v JOIN FETCH v.promotion WHERE v.customer.userId = :customerId AND v.isUsed = false AND v.validUntil > :now")
    List<Voucher> findActiveVouchersByCustomerId(@Param("customerId") Integer customerId, @Param("now") LocalDateTime now);

    @Query("SELECT v FROM Voucher v JOIN FETCH v.promotion WHERE v.customer.userId = :customerId AND v.promotion.code = :code AND v.isUsed = false AND v.validUntil > :now")
    Optional<Voucher> findActiveVoucherByCustomerAndCode(@Param("customerId") Integer customerId, @Param("code") String code, @Param("now") LocalDateTime now);

    @Query("SELECT v FROM Voucher v JOIN FETCH v.promotion WHERE v.customer.userId = :customerId ORDER BY v.usedAt DESC NULLS LAST, v.validUntil DESC, v.id DESC")
    List<Voucher> findAllByCustomerIdWithPromotion(@Param("customerId") Integer customerId);

    // Khách đã từng sở hữu voucher của promotion này chưa (bất kể đã dùng/hết hạn) — chặn đổi điểm trùng
    @Query("SELECT COUNT(v) > 0 FROM Voucher v WHERE v.customer.userId = :customerId AND v.promotion.id = :promotionId")
    boolean existsByCustomerAndPromotion(@Param("customerId") Integer customerId, @Param("promotionId") Integer promotionId);
}
