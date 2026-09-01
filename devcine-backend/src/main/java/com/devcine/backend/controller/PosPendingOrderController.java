package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.service.PendingOrderService;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/pos/bookings")
@RequiredArgsConstructor
public class PosPendingOrderController {

    private final PendingOrderService pendingOrderService;
    private final StaffRepository staffRepository;

    private Staff currentStaffOrNull() {
        Integer uid = SecurityUtils.getCurrentUserId();
        return uid == null ? null : staffRepository.findById(uid).orElse(null);
    }

    @PostMapping("/hold")
    @PreAuthorize("@perm.can('pos_ticketing', 'add')")
    public ResponseEntity<?> holdOrder(@RequestBody Map<String, Object> body) {
        try {
            Staff soldBy = currentStaffOrNull();
            String posTerminalId = (String) body.get("posTerminalId");
            
            // Loại vé/đối tượng theo từng ghế kèm đơn giá snapshot
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> selRaw = (List<Map<String, Object>>) body.get("seatSelections");
            List<com.devcine.backend.dto.request.SeatSelectionDTO> seatSelections = selRaw == null ? null : selRaw.stream()
                    .map(m -> com.devcine.backend.dto.request.SeatSelectionDTO.builder()
                            .seatId(Integer.parseInt(m.get("seatId").toString()))
                            .ticketType(m.get("ticketType") != null ? m.get("ticketType").toString() : "ADULT")
                            .unitPrice(m.get("unitPrice") != null ? new java.math.BigDecimal(m.get("unitPrice").toString()) : null)
                            .build())
                    .toList();

            com.devcine.backend.dto.request.BookingRequestDTO req = com.devcine.backend.dto.request.BookingRequestDTO.builder()
                    .showtimeId(Integer.parseInt(body.get("showtimeId").toString()))
                    .seatIds((List<Integer>) body.get("seatIds"))
                    .seatSelections(seatSelections)
                    .customerId(body.get("customerId") != null ? Integer.parseInt(body.get("customerId").toString()) : null)
                    .paymentMethod("POS_HOLD") // Initially POS_HOLD
                    .build();
            
            // F&B
            List<Map<String, Object>> fnbsRaw = (List<Map<String, Object>>) body.get("fnbs");
            if (fnbsRaw != null) {
                List<com.devcine.backend.dto.request.FnbSelectionDTO> fnbs = fnbsRaw.stream().map(m -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> optionsRaw = (List<Map<String, Object>>) m.get("options");
                    List<com.devcine.backend.dto.request.FnbOptionSelectionDTO> options = optionsRaw == null ? List.of() : optionsRaw.stream()
                            .map(opt -> com.devcine.backend.dto.request.FnbOptionSelectionDTO.builder()
                                    .slotId(opt.get("slotId") != null ? Integer.parseInt(opt.get("slotId").toString()) : null)
                                    .optionGroupId(opt.get("optionGroupId") != null ? Integer.parseInt(opt.get("optionGroupId").toString()) : null)
                                    .optionItemId(opt.get("optionItemId") != null ? Integer.parseInt(opt.get("optionItemId").toString()) : null)
                                    .clientSurcharge(opt.get("clientSurcharge") != null ? new java.math.BigDecimal(opt.get("clientSurcharge").toString()) : null)
                                    .build())
                            .toList();
                    return com.devcine.backend.dto.request.FnbSelectionDTO.builder()
                            .fnbItemId(Integer.parseInt(m.get("fnbItemId").toString()))
                            .quantity(Integer.parseInt(m.get("quantity").toString()))
                            .clientPrice(m.get("clientPrice") != null ? new java.math.BigDecimal(m.get("clientPrice").toString()) : null)
                            .options(options)
                            .build();
                }).toList();
                req.setFnbs(fnbs);
            }

            Booking booking = pendingOrderService.holdOrder(req, soldBy, posTerminalId);

            return ResponseEntity.status(201).body(ApiResponse.ok(Map.of(
                    "bookingId", booking.getId(),
                    "bookingCode", booking.getBookingCode(),
                    "expiresAt", booking.getExpiresAt().toString()
            )));
        } catch (Exception e) {
            // Bọc null-safe: exception có thể có message == null (VD: NPE) → tránh NPE thứ cấp khi gọi .contains()
            String msg = e.getMessage() != null ? e.getMessage() : "Không giữ được đơn. Vui lòng thử lại.";
            if (msg.contains("xung đột") || msg.contains("bị phạt") || msg.contains("đã bị đặt")) {
                return ResponseEntity.status(409).body(ApiResponse.fail(msg));
            }
            if (msg.contains("giới hạn")) {
                return ResponseEntity.status(400).body(ApiResponse.fail(msg));
            }
            return ResponseEntity.badRequest().body(ApiResponse.fail(msg));
        }
    }

    @GetMapping
    @PreAuthorize("@perm.can('pos_ticketing', 'view')")
    public ResponseEntity<?> getPendingOrders(@RequestParam String posTerminalId) {
        List<Booking> orders = pendingOrderService.getPendingOrders(posTerminalId);
        List<Map<String, Object>> result = orders.stream().map(b -> Map.<String, Object>of(
                "id", b.getId(),
                "bookingCode", b.getBookingCode(),
                "expiresAt", b.getExpiresAt().toString(),
                "totalPrice", b.getTotalPrice()
        )).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/{id}/pay-intent")
    @PreAuthorize("@perm.can('pos_ticketing', 'add')")
    public ResponseEntity<?> payIntent(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            String posTerminalId = (String) body.get("posTerminalId");
            pendingOrderService.payIntent(id, posTerminalId);
            return ResponseEntity.ok(ApiResponse.ok("OK"));
        } catch (Exception e) {
            return ResponseEntity.status(410).body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("@perm.can('pos_ticketing', 'add')")
    public ResponseEntity<?> payOrder(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            String posTerminalId = (String) body.get("posTerminalId");
            String paymentMethod = (String) body.getOrDefault("paymentMethod", "CASH");
            pendingOrderService.finalizePayment(id, posTerminalId, paymentMethod);
            return ResponseEntity.ok(ApiResponse.ok("Thanh toán thành công"));
        } catch (Exception e) {
            if (e.getMessage().contains("Out-of-Stock")) { // Giả định BookingService văng lỗi này
                return ResponseEntity.status(422).body(ApiResponse.fail(e.getMessage()));
            }
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/cancel")
    @PreAuthorize("@perm.can('pos_ticketing', 'add')")
    public ResponseEntity<?> cancelOrder(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            String posTerminalId = (String) body.get("posTerminalId");
            pendingOrderService.cancelHold(id, posTerminalId);
            return ResponseEntity.ok(ApiResponse.ok("Hủy thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/resume")
    @PreAuthorize("@perm.can('pos_ticketing', 'view')")
    public ResponseEntity<?> resumeOrder(@PathVariable Integer id, @RequestParam String posTerminalId) {
        try {
            // Check status and return detailed info
            List<Booking> orders = pendingOrderService.getPendingOrders(posTerminalId);
            Booking booking = orders.stream().filter(b -> b.getId().equals(id)).findFirst()
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại hoặc đã quá hạn"));
                    
            // TODO: Trả về chi tiết ghế, F&B... để POS build lại giỏ hàng (TicketingPOS.vue cần)
            Map<String, Object> result = Map.of(
                "bookingId", booking.getId(),
                "showtimeId", booking.getShowtime().getId(),
                "expiresAt", booking.getExpiresAt().toString()
            );
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.status(410).body(ApiResponse.fail(e.getMessage()));
        }
    }
}
