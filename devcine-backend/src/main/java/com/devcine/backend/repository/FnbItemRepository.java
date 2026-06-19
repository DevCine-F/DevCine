package com.devcine.backend.repository;

import com.devcine.backend.entity.FnbItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FnbItemRepository extends JpaRepository<FnbItem, Integer> {

    /** Các món còn bán — cho bước chọn combo phía khách. */
    List<FnbItem> findByIsActiveTrueOrderByTypeAscNameAsc();
}
