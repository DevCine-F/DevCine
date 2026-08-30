package com.devcine.backend.service;

import com.devcine.backend.dto.IncidentRelocateEmailData;
import com.devcine.backend.dto.TicketEmailData;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@org.junit.jupiter.api.Disabled("Tạm ẩn phân hệ sự cố")
class MailServiceTest {

    @Test
    void incidentRelocateEmailUsesCurrentTicketQrInsteadOfStableBookingCode() {
        MailService mailService = new MailService(mock(JavaMailSender.class));
        IncidentRelocateEmailData data = new IncidentRelocateEmailData(
                "customer@example.com",
                "Khách hàng",
                "BOOK-001",
                "Phim thử nghiệm",
                "DevCine",
                "Phòng 01",
                LocalDateTime.of(2026, 8, 30, 18, 0),
                "Ghế cần bảo trì",
                List.of(new IncidentRelocateEmailData.SeatSwapLine("B10", "C10")),
                false,
                null,
                null,
                BigDecimal.ZERO,
                "NONE",
                false,
                List.of(new TicketEmailData.SeatLine(
                        "C10", "NORMAL", "ADULT", "DEVCINE-T-17-V2-NEWQR")),
                List.of());

        String html = ReflectionTestUtils.invokeMethod(mailService, "buildIncidentRelocateHtml", data);

        assertNotNull(html);
        assertTrue(html.contains("data=DEVCINE-T-17-V2-NEWQR"));
        assertTrue(html.contains("QR vé mới vào phòng chiếu"));
        assertTrue(html.contains("Ghế C10"));
        assertFalse(html.contains("data=BOOK-001"));
    }
}
