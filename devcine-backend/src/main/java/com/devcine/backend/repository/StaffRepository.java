package com.devcine.backend.repository;

import com.devcine.backend.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Integer> {

    // JOIN FETCH user + role + cinema để tránh N+1 khi liệt kê nhân viên
    @Query("SELECT DISTINCT s FROM Staff s " +
           "JOIN FETCH s.user u " +
           "LEFT JOIN FETCH u.role " +
           "LEFT JOIN FETCH s.cinema " +
           "ORDER BY u.fullName ASC")
    List<Staff> findAllWithDetails();

    @Query("SELECT s FROM Staff s " +
           "JOIN FETCH s.user u " +
           "LEFT JOIN FETCH u.role " +
           "LEFT JOIN FETCH s.cinema " +
           "WHERE s.userId = :id")
    Optional<Staff> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT s.staffCode FROM Staff s WHERE s.staffCode IS NOT NULL")
    List<String> findAllStaffCodes();

    boolean existsByStaffCode(String staffCode);

    boolean existsByStaffCodeIgnoreCase(String staffCode);

    boolean existsByStaffCodeAndUserIdNot(String staffCode, Integer userId);

    boolean existsByStaffCodeIgnoreCaseAndUserIdNot(String staffCode, Integer userId);
}
