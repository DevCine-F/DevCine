package com.devcine.backend.controller;

import com.devcine.backend.dto.TicketEmailData;
import com.devcine.backend.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Endpoint TEST gửi email vé bằng dữ liệu mẫu — KHÔNG cần đặt vé thật.
 * Nằm dưới /api/system/** (đã permitAll) để gọi nhanh từ trình duyệt.
 * Lưu ý: chỉ phục vụ phát triển/kiểm thử, cân nhắc gỡ trước khi production.
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TestMailController {

    private final MailService mailService;

    /** Gọi: GET /api/system/test-mail?to=devcinecinema@gmail.com */
    @GetMapping("/test-mail")
    public ResponseEntity<?> testMail(@RequestParam String to) {
        TicketEmailData sample = new TicketEmailData(
                to,
                "Nguyễn Văn An",
                "TEST-" + System.currentTimeMillis() % 100000,
                "Lật Mặt 7: Một Điều Ước",
                "DevCine Bitexco",
                "Phòng 01 - Standard",
                LocalDateTime.now().plusDays(1).withHour(9).withMinute(0),
                "TRANSFER",
                new BigDecimal("255600"),
                List.of(
                        new TicketEmailData.SeatLine("B8", "STUDENT", "DEVCINE-T-DEMO-1"),
                        new TicketEmailData.SeatLine("B9", "ADULT", "DEVCINE-T-DEMO-2")
                ),
                List.of(new TicketEmailData.FnbLine("Combo Couple", 1))
        );
        try {
            mailService.sendTicketEmailSync(sample);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã gửi mail test tới " + to));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
