package com.devcine.backend.controller;

import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.repository.FnbItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fnbs")
@RequiredArgsConstructor
public class FnbController {

    private final FnbItemRepository fnbItemRepository;

    /** Công khai — chỉ trả món còn bán, phục vụ bước chọn combo khi đặt vé. */
    @GetMapping
    public ResponseEntity<List<FnbItem>> getActiveFnbs() {
        return ResponseEntity.ok(fnbItemRepository.findByIsActiveTrueOrderByTypeAscNameAsc());
    }

    /** Toàn bộ thực đơn (kể cả đang ẩn) cho khu vực quản trị. */
    @GetMapping("/all")
    @PreAuthorize("@perm.can('pos_inventory','view')")
    public ResponseEntity<List<FnbItem>> getAllFnbs() {
        return ResponseEntity.ok(fnbItemRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("@perm.can('pos_inventory','add')")
    public ResponseEntity<?> createFnb(@RequestBody Map<String, Object> body) {
        try {
            FnbItem item = FnbItem.builder()
                    .name((String) body.get("name"))
                    .type((String) body.getOrDefault("type", "COMBO"))
                    .price(new BigDecimal(body.get("price").toString()))
                    .imageUrl((String) body.get("imageUrl"))
                    .description((String) body.get("description"))
                    .isActive(body.get("isActive") == null || Boolean.parseBoolean(body.get("isActive").toString()))
                    .build();
            fnbItemRepository.save(item);
            return ResponseEntity.status(201).body(Map.of("success", true, "data", item));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('pos_inventory','edit')")
    public ResponseEntity<?> updateFnb(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            FnbItem item = fnbItemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món F&B"));
            if (body.containsKey("name")) item.setName((String) body.get("name"));
            if (body.containsKey("type")) item.setType((String) body.get("type"));
            if (body.get("price") != null) item.setPrice(new BigDecimal(body.get("price").toString()));
            if (body.containsKey("imageUrl")) item.setImageUrl((String) body.get("imageUrl"));
            if (body.containsKey("description")) item.setDescription((String) body.get("description"));
            if (body.containsKey("isActive")) item.setIsActive(Boolean.parseBoolean(body.get("isActive").toString()));
            fnbItemRepository.save(item);
            return ResponseEntity.ok(Map.of("success", true, "data", item));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('pos_inventory','delete')")
    public ResponseEntity<?> deleteFnb(@PathVariable Integer id) {
        try {
            fnbItemRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
