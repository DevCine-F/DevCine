package com.devcine.backend.repository;

import com.devcine.backend.entity.BomRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BomRecipeRepository extends JpaRepository<BomRecipe, Integer> {

    @Query("SELECT r FROM BomRecipe r JOIN FETCH r.ingredient WHERE r.combo.id = :comboId")
    List<BomRecipe> findByComboIdWithIngredient(@Param("comboId") Integer comboId);

    @Query("SELECT r FROM BomRecipe r JOIN FETCH r.combo JOIN FETCH r.ingredient ORDER BY r.combo.id")
    List<BomRecipe> findAllWithDetails();
}
