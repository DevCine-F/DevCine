package com.devcine.backend.repository;

import com.devcine.backend.entity.FnbComboSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FnbComboSlotRepository extends JpaRepository<FnbComboSlot, Integer> {

    /** Có Ô chọn nào của combo đang dùng pool này không (chặn xoá pool đang được dùng). */
    boolean existsByOptionGroup_Id(Integer optionGroupId);
}
