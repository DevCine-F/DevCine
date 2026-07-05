package com.devcine.backend.service;

import com.devcine.backend.entity.BomRecipe;
import com.devcine.backend.entity.CinemaInventory;
import com.devcine.backend.entity.InventoryLog;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.entity.StaffSchedule;
import com.devcine.backend.repository.BomRecipeRepository;
import com.devcine.backend.repository.CinemaInventoryRepository;
import com.devcine.backend.repository.InventoryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final BomRecipeRepository bomRecipeRepository;
    private final CinemaInventoryRepository cinemaInventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @Transactional
    public void deductForSale(Integer cinemaId, Integer fnbItemId, int soldQuantity) {
        deductForSale(cinemaId, fnbItemId, soldQuantity, null);
    }

    @Transactional
    public void deductForSale(Integer cinemaId, Integer fnbItemId, int soldQuantity, StaffSchedule staffSchedule) {
        if (cinemaId == null || fnbItemId == null || soldQuantity <= 0) return;

        Staff changedByStaff = staffSchedule != null ? staffSchedule.getStaff() : null;
        List<BomRecipe> recipes = bomRecipeRepository.findByComboIdWithIngredient(fnbItemId);
        if (recipes.isEmpty()) {
            deductInventory(cinemaId, fnbItemId, soldQuantity, changedByStaff);
            return;
        }

        for (BomRecipe recipe : recipes) {
            int need = recipe.getQuantity()
                    .multiply(BigDecimal.valueOf(soldQuantity))
                    .setScale(0, RoundingMode.CEILING)
                    .intValue();
            deductInventory(cinemaId, recipe.getIngredient().getId(), need, changedByStaff);
        }
    }

    /**
     * Hoàn kho khi Trưởng ca duyệt HỦY (void) một hóa đơn F&B lỗi — đảo ngược {@link #deductForSale}.
     * Cộng lại đúng lượng đã trừ (theo BOM nếu là combo), ghi InventoryLog loại VOID.
     */
    @Transactional
    public void restockForVoid(Integer cinemaId, Integer fnbItemId, int quantity, StaffSchedule staffSchedule) {
        if (cinemaId == null || fnbItemId == null || quantity <= 0) return;

        Staff changedByStaff = staffSchedule != null ? staffSchedule.getStaff() : null;
        List<BomRecipe> recipes = bomRecipeRepository.findByComboIdWithIngredient(fnbItemId);
        if (recipes.isEmpty()) {
            addInventory(cinemaId, fnbItemId, quantity, changedByStaff);
            return;
        }

        for (BomRecipe recipe : recipes) {
            int back = recipe.getQuantity()
                    .multiply(BigDecimal.valueOf(quantity))
                    .setScale(0, RoundingMode.CEILING)
                    .intValue();
            addInventory(cinemaId, recipe.getIngredient().getId(), back, changedByStaff);
        }
    }

    private void addInventory(Integer cinemaId, Integer fnbItemId, int amount, Staff changedByStaff) {
        if (amount <= 0) return;
        CinemaInventory ci = cinemaInventoryRepository
                .findByCinema_IdAndFnbItem_Id(cinemaId, fnbItemId)
                .orElse(null);
        if (ci == null) {
            log.warn("Skip inventory restock: cinema {} has no inventory row for item {}", cinemaId, fnbItemId);
            return;
        }

        ci.setInStock(ci.getInStock() + amount);
        ci.setLastUpdated(LocalDateTime.now());
        cinemaInventoryRepository.save(ci);

        InventoryLog logEntry = InventoryLog.builder()
                .cinemaInventory(ci)
                .changedByStaff(changedByStaff)
                .type("VOID")
                .quantityChange(amount)
                .timestamp(LocalDateTime.now())
                .build();
        inventoryLogRepository.save(logEntry);
    }

    private void deductInventory(Integer cinemaId, Integer fnbItemId, int amount, Staff changedByStaff) {
        CinemaInventory ci = cinemaInventoryRepository
                .findByCinema_IdAndFnbItem_Id(cinemaId, fnbItemId)
                .orElse(null);
        if (ci == null) {
            log.warn("Skip inventory deduction: cinema {} has no inventory row for item {}", cinemaId, fnbItemId);
            return;
        }

        int newStock = Math.max(0, ci.getInStock() - amount);
        ci.setInStock(newStock);
        ci.setLastUpdated(LocalDateTime.now());
        cinemaInventoryRepository.save(ci);

        InventoryLog logEntry = InventoryLog.builder()
                .cinemaInventory(ci)
                .changedByStaff(changedByStaff)
                .type("SALE")
                .quantityChange(-amount)
                .timestamp(LocalDateTime.now())
                .build();
        inventoryLogRepository.save(logEntry);
    }
}
