package com.devcine.backend.controller;

import com.devcine.backend.entity.PricingRule;
import com.devcine.backend.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PricingController {

    private final PricingRuleRepository pricingRuleRepository;

    @GetMapping
    public ResponseEntity<?> getAllRules() {
        return ResponseEntity.ok(pricingRuleRepository.findAllByOrderByStartDateDesc());
    }

    @PostMapping
    @PreAuthorize("@perm.can('pricing','add')")
    public ResponseEntity<?> createRule(@RequestBody Map<String, Object> body) {
        try {
            PricingRule rule = PricingRule.builder()
                    .name((String) body.get("name"))
                    .ruleType((String) body.get("ruleType"))
                    .value(new BigDecimal(body.get("value").toString()))
                    .startDate(body.get("startDate") != null ? LocalDateTime.parse((String) body.get("startDate")) : null)
                    .endDate(body.get("endDate") != null ? LocalDateTime.parse((String) body.get("endDate")) : null)
                    .build();
            pricingRuleRepository.save(rule);
            return ResponseEntity.status(201).body(Map.of("success", true, "data", rule));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('pricing','edit')")
    public ResponseEntity<?> updateRule(@PathVariable Integer id,
                                         @RequestBody Map<String, Object> body) {
        try {
            PricingRule rule = pricingRuleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy quy tắc giá"));
            if (body.containsKey("name")) rule.setName((String) body.get("name"));
            if (body.containsKey("ruleType")) rule.setRuleType((String) body.get("ruleType"));
            if (body.containsKey("value")) rule.setValue(new BigDecimal(body.get("value").toString()));
            if (body.get("startDate") != null) rule.setStartDate(LocalDateTime.parse((String) body.get("startDate")));
            if (body.get("endDate") != null) rule.setEndDate(LocalDateTime.parse((String) body.get("endDate")));
            pricingRuleRepository.save(rule);
            return ResponseEntity.ok(Map.of("success", true, "data", rule));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('pricing','delete')")
    public ResponseEntity<?> deleteRule(@PathVariable Integer id) {
        try {
            pricingRuleRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
