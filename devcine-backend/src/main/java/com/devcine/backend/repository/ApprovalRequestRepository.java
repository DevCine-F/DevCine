package com.devcine.backend.repository;

import com.devcine.backend.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Integer> {

    List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<ApprovalRequest> findByCinemaIdAndStatusOrderByCreatedAtDesc(Integer cinemaId, String status);

    List<ApprovalRequest> findByRequestedByUserIdOrderByCreatedAtDesc(Integer requestedByUserId);

    boolean existsByTypeAndRefIdAndStatus(String type, Integer refId, String status);
}
