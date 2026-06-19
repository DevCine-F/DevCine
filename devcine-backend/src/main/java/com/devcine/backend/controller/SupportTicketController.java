package com.devcine.backend.controller;

import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.SupportTicket;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/support-tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupportTicketController {

    private final SupportTicketRepository supportTicketRepository;
    private final CustomerRepository customerRepository;

    @GetMapping
    public ResponseEntity<?> getAllTickets() {
        List<SupportTicket> tickets = supportTicketRepository.findAllWithCustomer();
        List<Map<String, Object>> result = tickets.stream().map(t -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", t.getId());
            map.put("issueType", t.getIssueType() != null ? t.getIssueType() : "");
            map.put("description", t.getDescription() != null ? t.getDescription() : "");
            map.put("status", t.getStatus() != null ? t.getStatus() : "OPEN");
            map.put("createdAt", t.getCreatedAt().toString());
            map.put("customerName", t.getCustomer() != null && t.getCustomer().getUser() != null
                    ? t.getCustomer().getUser().getFullName() : "Khách hàng");
            map.put("customerEmail", t.getCustomer() != null && t.getCustomer().getUser() != null
                    ? t.getCustomer().getUser().getEmail() : "");
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestBody Map<String, Object> body) {
        try {
            Integer customerId = Integer.parseInt(body.get("customerId").toString());
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

            SupportTicket ticket = SupportTicket.builder()
                    .customer(customer)
                    .issueType((String) body.getOrDefault("issueType", "OTHER"))
                    .description((String) body.getOrDefault("description", ""))
                    .status("OPEN")
                    .createdAt(LocalDateTime.now())
                    .build();
            supportTicketRepository.save(ticket);
            return ResponseEntity.status(201).body(Map.of("success", true, "id", ticket.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@perm.can('support','edit')")
    public ResponseEntity<?> updateStatus(@PathVariable Integer id,
                                           @RequestParam String status) {
        try {
            SupportTicket ticket = supportTicketRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ticket"));
            ticket.setStatus(status);
            supportTicketRepository.save(ticket);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('support','delete')")
    public ResponseEntity<?> deleteTicket(@PathVariable Integer id) {
        try {
            supportTicketRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
