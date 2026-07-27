package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.SupportTicket;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.SupportTicketRepository;
import com.devcine.backend.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/support-tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketRepository supportTicketRepository;
    private final CustomerRepository customerRepository;
    private final MailService mailService;

    /** Nhãn chủ đề hiển thị — đồng bộ với SUBJECTS ở ContactView.vue (FE khách). */
    private static final Map<String, String> ISSUE_TYPE_LABELS = Map.of(
            "TICKET", "Vấn đề về vé",
            "MEMBERSHIP", "Thành viên",
            "SERVICE", "Góp ý dịch vụ",
            "PARTNERSHIP", "Hợp tác quảng cáo"
    );

    private String subjectLabel(String issueType) {
        if (issueType == null) return "Yêu cầu hỗ trợ";
        return ISSUE_TYPE_LABELS.getOrDefault(issueType, issueType);
    }

    @GetMapping
    public ResponseEntity<?> getAllTickets() {
        List<SupportTicket> tickets = supportTicketRepository.findAllWithCustomer();
        List<Map<String, Object>> result = tickets.stream().map(t -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", t.getId());
            map.put("issueType", t.getIssueType() != null ? t.getIssueType() : "");
            map.put("description", t.getDescription() != null ? t.getDescription() : "");
            map.put("phone", t.getPhone() != null ? t.getPhone() : "");
            map.put("status", t.getStatus() != null ? t.getStatus() : "OPEN");
            map.put("adminReply", t.getAdminReply() != null ? t.getAdminReply() : "");
            map.put("repliedAt", t.getRepliedAt() != null ? t.getRepliedAt().toString() : null);
            map.put("createdAt", t.getCreatedAt().toString());
            map.put("customerName", t.getCustomer() != null && t.getCustomer().getUser() != null
                    ? t.getCustomer().getUser().getFullName() : "Khách hàng");
            map.put("customerEmail", t.getCustomer() != null && t.getCustomer().getUser() != null
                    ? t.getCustomer().getUser().getEmail() : "");
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestBody Map<String, Object> body) {
        try {
            Integer customerId = Integer.parseInt(body.get("customerId").toString());
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

            String phone = (String) body.getOrDefault("phone", null);
            SupportTicket ticket = SupportTicket.builder()
                    .customer(customer)
                    .issueType((String) body.getOrDefault("issueType", "OTHER"))
                    .description((String) body.getOrDefault("description", ""))
                    .phone(phone != null && !phone.isBlank() ? phone.trim() : null)
                    .status("OPEN")
                    .createdAt(LocalDateTime.now())
                    .build();
            supportTicketRepository.save(ticket);
            return ResponseEntity.status(201).body(ApiResponse.ok(Map.of("id", ticket.getId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("@perm.can('support','edit')")
    @Transactional
    public ResponseEntity<?> reply(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            String message = body.get("message") != null ? body.get("message").toString().trim() : "";
            if (message.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Nội dung phản hồi không được để trống."));
            }
            SupportTicket ticket = supportTicketRepository.findByIdWithCustomer(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ticket"));

            String email = ticket.getCustomer() != null && ticket.getCustomer().getUser() != null
                    ? ticket.getCustomer().getUser().getEmail() : null;
            String name = ticket.getCustomer() != null && ticket.getCustomer().getUser() != null
                    ? ticket.getCustomer().getUser().getFullName() : "Khách hàng";

            boolean sent = mailService.sendSupportReply(email, name, ticket.getId(),
                    subjectLabel(ticket.getIssueType()), ticket.getDescription(), message);

            ticket.setAdminReply(message);
            ticket.setRepliedAt(LocalDateTime.now());
            if ("OPEN".equals(ticket.getStatus())) {
                ticket.setStatus("IN_PROGRESS");
            }
            supportTicketRepository.save(ticket);

            String note = sent
                    ? "Đã gửi phản hồi qua email cho khách."
                    : "Đã lưu phản hồi, nhưng email chưa gửi được (khách chưa có email hoặc SMTP tắt).";
            return ResponseEntity.ok(ApiResponse.ok(Map.of("emailSent", sent, "message", note)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success("Đã cập nhật trạng thái."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('support','delete')")
    public ResponseEntity<?> deleteTicket(@PathVariable Integer id) {
        try {
            supportTicketRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Đã xoá yêu cầu hỗ trợ."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
