package com.devcine.backend.controller;

import com.devcine.backend.entity.CinemaInventory;
import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.entity.InventoryLog;
import com.devcine.backend.entity.StaffSchedule;
import com.devcine.backend.repository.CinemaInventoryRepository;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.FnbItemRepository;
import com.devcine.backend.repository.InventoryLogRepository;
import com.devcine.backend.service.ShiftAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InventoryController {

    private final CinemaInventoryRepository cinemaInventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final CinemaRepository cinemaRepository;
    private final FnbItemRepository fnbItemRepository;
    private final ShiftAccessService shiftAccessService;

    @GetMapping("/items")
    public ResponseEntity<?> getAllItems() {
        List<CinemaInventory> items = cinemaInventoryRepository.findAllWithDetails();
        List<Map<String, Object>> result = items.stream().map(ci -> Map.<String, Object>of(
                "id", ci.getId(),
                "inStock", ci.getInStock(),
                "lastUpdated", ci.getLastUpdated() != null ? ci.getLastUpdated().toString() : "",
                "cinemaId", ci.getCinema().getId(),
                "cinemaName", ci.getCinema().getName(),
                "fnbItemId", ci.getFnbItem().getId(),
                "fnbItemName", ci.getFnbItem().getName(),
                "fnbItemType", ci.getFnbItem().getType() != null ? ci.getFnbItem().getType() : ""
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions() {
        List<InventoryLog> logs = inventoryLogRepository.findAllWithDetails();
        List<Map<String, Object>> result = logs.stream().map(log -> Map.<String, Object>of(
                "id", log.getId(),
                "type", log.getType() != null ? log.getType() : "",
                "quantityChange", log.getQuantityChange(),
                "timestamp", log.getTimestamp().toString(),
                "fnbItemName", log.getCinemaInventory().getFnbItem().getName(),
                "changedBy", log.getChangedByStaff() != null && log.getChangedByStaff().getUser() != null
                        ? log.getChangedByStaff().getUser().getFullName() : "Hệ thống"
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/transactions")
    @Transactional
    @PreAuthorize("@perm.can('pos_inventory','edit')")
    public ResponseEntity<?> adjustInventory(@RequestBody Map<String, Object> body) {
        try {
            StaffSchedule schedule = shiftAccessService.requireCurrentShiftForStaff(List.of("FNB", "SHIFT_LEAD"), "quan ly kho F&B");
            Integer inventoryId = Integer.parseInt(body.get("inventoryId").toString());
            Integer quantityChange = Integer.parseInt(body.get("quantityChange").toString());
            String type = (String) body.getOrDefault("type", "IMPORT");

            CinemaInventory ci = cinemaInventoryRepository.findById(inventoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy mục kho"));

            int newStock = ci.getInStock() + quantityChange;
            if (newStock < 0) {
                throw new RuntimeException("Tồn kho không thể âm");
            }
            ci.setInStock(newStock);
            ci.setLastUpdated(LocalDateTime.now());
            cinemaInventoryRepository.save(ci);

            InventoryLog log = InventoryLog.builder()
                    .cinemaInventory(ci)
                    .changedByStaff(schedule != null ? schedule.getStaff() : null)
                    .type(type)
                    .quantityChange(quantityChange)
                    .timestamp(LocalDateTime.now())
                    .build();
            inventoryLogRepository.save(log);

            return ResponseEntity.ok(Map.of("success", true, "newStock", newStock));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/items")
    @Transactional
    @PreAuthorize("@perm.can('pos_inventory','add')")
    public ResponseEntity<?> addInventoryItem(@RequestBody Map<String, Object> body) {
        try {
            shiftAccessService.requireCurrentShiftForStaff(List.of("FNB", "SHIFT_LEAD"), "quan ly kho F&B");
            Integer cinemaId = Integer.parseInt(body.get("cinemaId").toString());
            Integer fnbItemId = Integer.parseInt(body.get("fnbItemId").toString());
            Integer initialStock = Integer.parseInt(body.getOrDefault("initialStock", 0).toString());

            var cinema = cinemaRepository.findById(cinemaId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy rạp"));
            FnbItem fnbItem = fnbItemRepository.findById(fnbItemId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy mặt hàng F&B"));

            CinemaInventory ci = CinemaInventory.builder()
                    .cinema(cinema)
                    .fnbItem(fnbItem)
                    .inStock(initialStock)
                    .lastUpdated(LocalDateTime.now())
                    .build();
            cinemaInventoryRepository.save(ci);
            return ResponseEntity.status(201).body(Map.of("success", true, "id", ci.getId()));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
