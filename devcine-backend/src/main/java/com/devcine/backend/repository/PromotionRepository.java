package com.devcine.backend.repository;

import com.devcine.backend.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    Optional<Promotion> findByCode(String code);
    Optional<Promotion> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id);

    /**
     * Tăng lượt dùng ATOMIC + có điều kiện: chỉ tăng khi chưa đạt giới hạn (hoặc không giới hạn).
     * Trả về số dòng affected: 1 = thành công, 0 = đã hết lượt → caller reject.
     * Giải quyết race condition khi 2 đơn thanh toán đồng thời cùng promotion.
     */
    @Modifying
    @Query("UPDATE Promotion p SET p.usedCount = p.usedCount + 1 " +
           "WHERE p.id = :id AND (p.usageLimit IS NULL OR p.usageLimit = 0 OR p.usedCount < p.usageLimit)")
    int incrementUsedCountIfAllowed(@Param("id") Integer id);

    /**
     * Tăng usedCount ngay lúc PHÁT HÀNH voucher (đổi điểm).
     * Dùng riêng cho {@code redeemWithPoints} để phân biệt với {@code incrementUsedCountIfAllowed}
     * (dùng khi khách thanh toán vé với mã công khai).
     *
     * <p>Trả về số dòng affected:
     * <ul>
     *   <li>1 = còn quota, đã giữ chỗ thành công.</li>
     *   <li>0 = hết quota → caller phải rollback toàn bộ giao dịch đổi điểm.</li>
     * </ul>
     */
    @Modifying
    @Query("UPDATE Promotion p SET p.usedCount = p.usedCount + 1 " +
           "WHERE p.id = :id AND (p.usageLimit IS NULL OR p.usageLimit = 0 OR p.usedCount < p.usageLimit)")
    int incrementIssuedCountIfQuotaAvailable(@Param("id") Integer id);
}
