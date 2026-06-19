package com.devcine.backend.service;

import com.devcine.backend.entity.BomRecipe;
import com.devcine.backend.entity.CinemaInventory;
import com.devcine.backend.entity.InventoryLog;
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

/**
 * Quản lý trừ tồn kho khi bán hàng F&B.
 * - Nếu món bán có định mức nguyên liệu (BomRecipe): trừ từng nguyên liệu theo định mức.
 * - Nếu không có định mức: coi như hàng tồn trực tiếp, trừ chính món đó.
 * Việc trừ kho là "best-effort": thiếu dòng tồn kho thì ghi cảnh báo, không làm hỏng giao dịch bán vé.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final BomRecipeRepository bomRecipeRepository;
    private final CinemaInventoryRepository cinemaInventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @Transactional
    public void deductForSale(Integer cinemaId, Integer fnbItemId, int soldQuantity) {
        if (cinemaId == null || fnbItemId == null || soldQuantity <= 0) return;

        List<BomRecipe> recipes = bomRecipeRepository.findByComboIdWithIngredient(fnbItemId);
        if (recipes.isEmpty()) {
            // Không có định mức -> trừ trực tiếp chính món
            deductInventory(cinemaId, fnbItemId, soldQuantity);
        } else {
            for (BomRecipe recipe : recipes) {
                int need = recipe.getQuantity()
                        .multiply(BigDecimal.valueOf(soldQuantity))
                        .setScale(0, RoundingMode.CEILING)
                        .intValue();
                deductInventory(cinemaId, recipe.getIngredient().getId(), need);
            }
        }
    }

    private void deductInventory(Integer cinemaId, Integer fnbItemId, int amount) {
        CinemaInventory ci = cinemaInventoryRepository
                .findByCinema_IdAndFnbItem_Id(cinemaId, fnbItemId)
                .orElse(null);
        if (ci == null) {
            log.warn("Bỏ qua trừ kho: rạp {} chưa có dòng tồn kho cho mặt hàng {}", cinemaId, fnbItemId);
            return;
        }
        int newStock = Math.max(0, ci.getInStock() - amount);
        ci.setInStock(newStock);
        ci.setLastUpdated(LocalDateTime.now());
        cinemaInventoryRepository.save(ci);

        InventoryLog logEntry = InventoryLog.builder()
                .cinemaInventory(ci)
                .type("SALE")
                .quantityChange(-amount)
                .timestamp(LocalDateTime.now())
                .build();
        inventoryLogRepository.save(logEntry);
    }
}
