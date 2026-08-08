package com.devcine.backend.repository;

import com.devcine.backend.entity.FnbItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FnbItemRepository extends JpaRepository<FnbItem, Integer> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"optionGroups", "optionGroups.items"})
    List<FnbItem> findByIsActiveTrueOrderByTypeAscNameAsc();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"optionGroups", "optionGroups.items"})
    List<FnbItem> findAll();
}
