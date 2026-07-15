package com.devcine.backend.repository;

import com.devcine.backend.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Integer> {

    @Query("SELECT DISTINCT c.city FROM Cinema c WHERE c.city IS NOT NULL")
    List<String> findAllCities();

    @Query("SELECT c FROM Cinema c LEFT JOIN FETCH c.manager m LEFT JOIN FETCH m.user")
    List<Cinema> findAllWithManager();

    // Nạp kèm manager + user (LEFT JOIN FETCH) để tránh lỗi lazy/khoá ngoại manager_id trỏ tới staff đã xoá.
    @Query("SELECT c FROM Cinema c LEFT JOIN FETCH c.manager m LEFT JOIN FETCH m.user WHERE c.id = :id")
    Optional<Cinema> findByIdWithManager(@Param("id") Integer id);

    List<Cinema> findAllByOrderByNameAsc();
    List<Cinema> findByCityIgnoreCaseOrderByNameAsc(String city);

    // Kiểm tra trùng tên cụm rạp (không phân biệt hoa/thường)
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}
