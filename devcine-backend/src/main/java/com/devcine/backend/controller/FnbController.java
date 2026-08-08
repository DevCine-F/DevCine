package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.repository.FnbItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Quản trị THỰC ĐƠN F&B (món & combo) — tên, giá, ảnh, ẩn/hiện. Không phải kho:
 * tồn kho là VÔ HẠN, {@link FnbItem} không có trường số lượng và module Kho/BOM đã gỡ.
 * Quyền gác ở đây là {@code fnb_menu} (trước 21/07/2026 mang tên cũ {@code pos_inventory}).
 * Bán F&B tại quầy KHÔNG đi qua đây — xem TicketingController + Position FNB.
 */
@RestController
@RequestMapping("/api/fnbs")
@RequiredArgsConstructor
public class FnbController {

    private final FnbItemRepository fnbItemRepository;
    private final com.devcine.backend.repository.FnbOptionGroupRepository fnbOptionGroupRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FnbItem>>> getActiveFnbs() {
        return ResponseEntity.ok(ApiResponse.ok(fnbItemRepository.findByIsActiveTrueOrderByTypeAscNameAsc()));
    }
    
    @GetMapping("/groups")
    @PreAuthorize("@perm.can('fnb_menu','view')")
    public ResponseEntity<ApiResponse<List<com.devcine.backend.entity.FnbOptionGroup>>> getAllOptionGroups() {
        return ResponseEntity.ok(ApiResponse.ok(fnbOptionGroupRepository.findAll()));
    }

    /** Toàn bộ thực đơn (kể cả đang ẩn) cho khu vực quản trị. */
    @GetMapping("/all")
    @PreAuthorize("@perm.can('fnb_menu','view')")
    public ResponseEntity<ApiResponse<List<FnbItem>>> getAllFnbs() {
        return ResponseEntity.ok(ApiResponse.ok(fnbItemRepository.findAll()));
    }

    @PostMapping
    @PreAuthorize("@perm.can('fnb_menu','add')")
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
            if (body.containsKey("optionGroupIds")) {
                List<Integer> groupIds = (List<Integer>) body.get("optionGroupIds");
                item.setOptionGroups(new java.util.HashSet<>(fnbOptionGroupRepository.findAllById(groupIds)));
            }
            fnbItemRepository.save(item);
            return ResponseEntity.status(201).body(ApiResponse.ok(item));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('fnb_menu','edit')")
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
            if (body.containsKey("optionGroupIds")) {
                List<Integer> groupIds = (List<Integer>) body.get("optionGroupIds");
                item.setOptionGroups(new java.util.HashSet<>(fnbOptionGroupRepository.findAllById(groupIds)));
            }
            fnbItemRepository.save(item);
            return ResponseEntity.ok(ApiResponse.ok(item));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('fnb_menu','delete')")
    public ResponseEntity<?> deleteFnb(@PathVariable Integer id) {
        try {
            FnbItem item = fnbItemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy F&B"));
            item.setIsActive(false);
            fnbItemRepository.save(item);
            return ResponseEntity.ok(ApiResponse.success("Đã xoá món F&B."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
