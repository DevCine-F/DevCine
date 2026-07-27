package com.devcine.backend.repository;

import com.devcine.backend.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Integer> {

    @Query("SELECT t FROM SupportTicket t LEFT JOIN FETCH t.customer c LEFT JOIN FETCH c.user ORDER BY t.createdAt DESC")
    List<SupportTicket> findAllWithCustomer();

    @Query("SELECT t FROM SupportTicket t LEFT JOIN FETCH t.customer c LEFT JOIN FETCH c.user WHERE t.id = :id")
    Optional<SupportTicket> findByIdWithCustomer(@Param("id") Integer id);
}
