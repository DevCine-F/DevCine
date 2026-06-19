package com.devcine.backend.repository;

import com.devcine.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByCustomer_UserIdOrderByCreatedAtDesc(Integer customerId);

    long countByCustomer_UserIdAndIsReadFalse(Integer customerId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.customer.userId = :customerId AND n.isRead = false")
    int markAllAsRead(@Param("customerId") Integer customerId);
}
