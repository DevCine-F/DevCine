package com.devcine.backend.repository;

import com.devcine.backend.entity.PromoEmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromoEmailLogRepository extends JpaRepository<PromoEmailLog, Integer> {

    /** Danh sách customerId đã nhận email của promotion này — để lọc trùng khi gửi lại. */
    @Query("SELECT l.customerId FROM PromoEmailLog l WHERE l.promotionId = :promoId")
    List<Integer> findCustomerIdsByPromotionId(@Param("promoId") Integer promoId);
}
