package com.devcine.backend.repository;

import com.devcine.backend.entity.FnbOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FnbOptionGroupRepository extends JpaRepository<FnbOptionGroup, Integer> {
}
