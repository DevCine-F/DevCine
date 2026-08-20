package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.dto.request.CancelSeatRequest;
import com.devcine.backend.dto.request.RelocateRequest;
import com.devcine.backend.dto.request.SeatPhysicalStatusRequest;
import com.devcine.backend.dto.response.CompensationOption;
import com.devcine.backend.dto.response.IncidentBookingContext;
import com.devcine.backend.dto.response.IncidentListItem;
import com.devcine.backend.dto.response.IncidentResultResponse;
import com.devcine.backend.dto.response.SeatPhysicalStatusResponse;
import com.devcine.backend.service.SeatIncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Xử lý sự cố phòng chiếu / đổi ghế đền bù (khu quản trị). Chịu Strict Cinema Scoping ở Service
 * (STAFF/MANAGER chỉ thao tác trong cụm rạp của mình). Feature quyền: {@code incident_handling}.
 */
@RestController
@RequestMapping("/api/staff/incidents")
@RequiredArgsConstructor
public class SeatIncidentController {

    private final SeatIncidentService incidentService;

    /** Tra vé theo Mã đặt vé hoặc SĐT khách. */
    @GetMapping("/lookup")
    @PreAuthorize("@perm.can('incident_handling','view')")
    public ResponseEntity<ApiResponse<IncidentBookingContext>> lookup(@RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.lookup(query)));
    }

    /** Chọn theo Phòng→Suất→Ghế: truy ngược đơn đang giữ ghế đã bán. */
    @GetMapping("/seat-occupant")
    @PreAuthorize("@perm.can('incident_handling','view')")
    public ResponseEntity<ApiResponse<IncidentBookingContext>> seatOccupant(
            @RequestParam Integer showtimeId, @RequestParam Integer seatId) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.findSeatOccupant(showtimeId, seatId)));
    }

    /** Danh sách mẫu voucher đền bù cho dropdown. */
    @GetMapping("/compensation-options")
    @PreAuthorize("@perm.can('incident_handling','view')")
    public ResponseEntity<ApiResponse<List<CompensationOption>>> compensationOptions() {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.listCompensationTemplates()));
    }

    /** Khóa/mở trạng thái vật lý của ghế (bảo trì ghế hỏng). */
    @PatchMapping("/seats/{seatId}/status")
    @PreAuthorize("@perm.can('incident_handling','handle')")
    public ResponseEntity<ApiResponse<SeatPhysicalStatusResponse>> setSeatStatus(
            @PathVariable Integer seatId, @Valid @RequestBody SeatPhysicalStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                incidentService.setSeatPhysicalStatus(seatId, req), "Đã cập nhật trạng thái ghế."));
    }

    /** Đổi ghế đền bù (1..n ghế). */
    @PostMapping("/relocate")
    @PreAuthorize("@perm.can('incident_handling','handle')")
    public ResponseEntity<ApiResponse<IncidentResultResponse>> relocate(@Valid @RequestBody RelocateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.relocate(req), "Đã đổi ghế & xử lý đền bù."));
    }

    /** Hủy chỗ (hết ghế thay thế) + đền theo giá vé. */
    @PostMapping("/cancel")
    @PreAuthorize("@perm.can('incident_handling','handle')")
    public ResponseEntity<ApiResponse<IncidentResultResponse>> cancel(@Valid @RequestBody CancelSeatRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.cancel(req), "Đã hủy chỗ & xử lý đền bù."));
    }

    /** Lịch sử sự cố (cinema-scoped, filter + phân trang). */
    @GetMapping
    @PreAuthorize("@perm.can('incident_handling','view')")
    public ResponseEntity<ApiResponse<Page<IncidentListItem>>> history(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDateTime fromTs = from != null ? from.atStartOfDay() : null;
        LocalDateTime toTs = to != null ? to.atTime(23, 59, 59) : null;
        Page<IncidentListItem> result = incidentService.history(type, code, fromTs, toTs, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** Chi tiết một sự cố. */
    @GetMapping("/{id}")
    @PreAuthorize("@perm.can('incident_handling','view')")
    public ResponseEntity<ApiResponse<IncidentListItem>> detail(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.detail(id)));
    }

    /** ĐỢT 2: duyệt phát voucher hàng loạt cho các sự cố đóng cửa đột xuất còn chờ đền bù. */
    @PostMapping("/emergency/approve")
    @PreAuthorize("@perm.can('incident_handling','handle')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Integer>>> approveEmergencyBatch() {
        return ResponseEntity.ok(ApiResponse.ok(
                incidentService.approveEmergencyBatch(), "Đã duyệt & phát voucher đền bù đợt sự cố."));
    }
}
