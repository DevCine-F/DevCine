package com.devcine.backend.repository;

import com.devcine.backend.entity.FnbItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FnbItemRepository extends JpaRepository<FnbItem, Integer> {

    // Kênh bán (Khách + POS): đang bán VÀ chưa xoá.
    @EntityGraph(attributePaths = {"slots", "slots.optionGroup", "slots.optionGroup.items"})
    List<FnbItem> findByIsActiveTrueAndIsDeletedFalseOrderByTypeAscNameAsc();

    // Màn quản trị: cả đang bán lẫn tạm ngưng, nhưng LỌC BỎ món đã xoá.
    @EntityGraph(attributePaths = {"slots", "slots.optionGroup", "slots.optionGroup.items"})
    List<FnbItem> findByIsDeletedFalseOrderByTypeAscNameAsc();

    @EntityGraph(attributePaths = {"slots", "slots.optionGroup", "slots.optionGroup.items"})
    List<FnbItem> findByIdIn(List<Integer> ids);
}
