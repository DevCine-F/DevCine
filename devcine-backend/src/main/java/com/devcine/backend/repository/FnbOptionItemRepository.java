package com.devcine.backend.repository;

import com.devcine.backend.entity.FnbOptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FnbOptionItemRepository extends JpaRepository<FnbOptionItem, Integer> {
}
