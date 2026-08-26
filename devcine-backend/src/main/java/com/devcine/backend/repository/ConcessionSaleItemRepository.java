package com.devcine.backend.repository;

import com.devcine.backend.entity.ConcessionSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConcessionSaleItemRepository extends JpaRepository<ConcessionSaleItem, Integer> {

    @Query("SELECT i FROM ConcessionSaleItem i JOIN FETCH i.fnbItem WHERE i.sale.id = :saleId")
    List<ConcessionSaleItem> findBySaleIdWithItem(@Param("saleId") Integer saleId);

    @Query("SELECT i.sale.id, COALESCE(SUM(i.quantity), 0) FROM ConcessionSaleItem i WHERE i.sale.id IN :saleIds GROUP BY i.sale.id")
    List<Object[]> countItemsBySaleIds(@Param("saleIds") List<Integer> saleIds);

    @Query("SELECT DISTINCT i FROM ConcessionSaleItem i LEFT JOIN FETCH i.fnbItem LEFT JOIN FETCH i.options WHERE i.sale.id = :saleId")
    List<ConcessionSaleItem> findBySaleIdWithOptions(@Param("saleId") Integer saleId);
}

