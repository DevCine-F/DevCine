package com.devcine.backend.repository;

import com.devcine.backend.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Integer> {

    /** Lịch sử điểm của một khách, mới nhất trước — phục vụ màn "Lịch sử điểm". */
    List<PointTransaction> findByCustomer_UserIdOrderByCreatedAtDescIdDesc(Integer customerId);
}
