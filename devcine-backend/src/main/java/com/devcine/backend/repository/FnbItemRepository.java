package com.devcine.backend.repository;

import com.devcine.backend.entity.FnbItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FnbItemRepository extends JpaRepository<FnbItem, Integer> {

    // Chỉ 1 bag (slots là List) trong đồ thị fetch → an toàn MultipleBagFetchException.
    @EntityGraph(attributePaths = {"slots", "slots.optionGroup", "slots.optionGroup.items"})
    List<FnbItem> findByIsActiveTrueOrderByTypeAscNameAsc();

    @EntityGraph(attributePaths = {"slots", "slots.optionGroup", "slots.optionGroup.items"})
    List<FnbItem> findAll();

    @EntityGraph(attributePaths = {"slots", "slots.optionGroup", "slots.optionGroup.items"})
    List<FnbItem> findByIdIn(List<Integer> ids);
}
