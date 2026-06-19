package com.devcine.backend.controller;

import com.devcine.backend.entity.BomRecipe;
import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.repository.BomRecipeRepository;
import com.devcine.backend.repository.FnbItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Quản lý định mức nguyên liệu (Bill of Materials) cho từng combo F&B.
 * Dùng để tự động trừ tồn kho nguyên liệu khi bán combo.
 */
@RestController
@RequestMapping("/api/bom")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BomController {

    private final BomRecipeRepository bomRecipeRepository;
    private final FnbItemRepository fnbItemRepository;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(toDtoList(bomRecipeRepository.findAllWithDetails()));
    }

    @GetMapping("/combo/{comboId}")
    public ResponseEntity<?> getByCombo(@PathVariable Integer comboId) {
        return ResponseEntity.ok(toDtoList(bomRecipeRepository.findByComboIdWithIngredient(comboId)));
    }

    @PostMapping
    @PreAuthorize("@perm.can('pos_inventory','edit')")
    public ResponseEntity<?> addRecipe(@RequestBody Map<String, Object> body) {
        try {
            Integer comboId = Integer.parseInt(body.get("comboId").toString());
            Integer ingredientId = Integer.parseInt(body.get("ingredientId").toString());
            BigDecimal quantity = new BigDecimal(body.get("quantity").toString());

            FnbItem combo = fnbItemRepository.findById(comboId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy combo"));
            FnbItem ingredient = fnbItemRepository.findById(ingredientId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu"));

            BomRecipe recipe = BomRecipe.builder()
                    .combo(combo)
                    .ingredient(ingredient)
                    .quantity(quantity)
                    .build();
            bomRecipeRepository.save(recipe);
            return ResponseEntity.status(201).body(Map.of("success", true, "id", recipe.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('pos_inventory','edit')")
    public ResponseEntity<?> deleteRecipe(@PathVariable Integer id) {
        try {
            bomRecipeRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private List<Map<String, Object>> toDtoList(List<BomRecipe> recipes) {
        return recipes.stream().map(r -> Map.<String, Object>of(
                "id", r.getId(),
                "comboId", r.getCombo().getId(),
                "comboName", r.getCombo().getName(),
                "ingredientId", r.getIngredient().getId(),
                "ingredientName", r.getIngredient().getName(),
                "quantity", r.getQuantity()
        )).collect(Collectors.toList());
    }
}
