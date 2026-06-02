package com.devcine.backend.repository;

import com.devcine.backend.entity.FnbItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FnbItemRepository extends JpaRepository<FnbItem, Integer> {
}
