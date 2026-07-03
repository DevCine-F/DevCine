package com.devcine.backend.repository;

import com.devcine.backend.entity.ConcessionSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ConcessionSaleRepository extends JpaRepository<ConcessionSale, Integer> {

    @Query("SELECT COALESCE(SUM(s.totalPrice), 0) FROM ConcessionSale s " +
           "WHERE s.status = 'CONFIRMED' AND s.staffSchedule.id = :staffScheduleId AND s.paymentMethod = :paymentMethod")
    BigDecimal sumConfirmedRevenueByStaffScheduleAndPaymentMethod(@Param("staffScheduleId") Integer staffScheduleId,
                                                                  @Param("paymentMethod") String paymentMethod);

    @Query("SELECT COALESCE(SUM(s.totalPrice), 0) FROM ConcessionSale s " +
           "WHERE s.status = 'CONFIRMED' AND s.staffSchedule.id = :staffScheduleId")
    BigDecimal sumRevenueByStaffSchedule(@Param("staffScheduleId") Integer staffScheduleId);

    @Query("SELECT COUNT(s) FROM ConcessionSale s " +
           "WHERE s.status = 'CONFIRMED' AND s.staffSchedule.id = :staffScheduleId")
    long countConfirmedByStaffSchedule(@Param("staffScheduleId") Integer staffScheduleId);
}
