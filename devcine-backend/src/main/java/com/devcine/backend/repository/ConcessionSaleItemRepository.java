package com.devcine.backend.repository;

import com.devcine.backend.entity.ConcessionSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConcessionSaleItemRepository extends JpaRepository<ConcessionSaleItem, Integer> {

    @Query("SELECT i FROM ConcessionSaleItem i JOIN FETCH i.fnbItem WHERE i.sale.id = :saleId")
    List<ConcessionSaleItem> findBySaleIdWithItem(@Param("saleId") Integer saleId);
}
