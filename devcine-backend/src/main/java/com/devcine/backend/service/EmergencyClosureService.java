package com.devcine.backend.service;

import com.devcine.backend.dto.CancellationEmailData;
import com.devcine.backend.entity.Promotion;
import com.devcine.backend.event.CinemaEmergencyClosedEvent;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Điều phối luồng NỀN khi cụm rạp đóng cửa đột xuất: duyệt toàn bộ đơn CONFIRMED của các suất
 * tương lai vừa bị hủy, hủy chỗ + đền bù (voucher đền nguyên vé), và gửi email thông báo.
 *
 * <p>Chốt kiến trúc (xem kế hoạch):
 * <ul>
 *   <li><b>{@code @TransactionalEventListener(AFTER_COMMIT)}</b>: chỉ chạy SAU khi transaction cập
 *       nhật rạp + hủy suất đã COMMIT → luồng nền đọc được dữ liệu nhất quán, không "async chạy
 *       trước commit".</li>
 *   <li><b>{@code @Async}</b>: chạy trên executor riêng → KHÔNG block API cập nhật rạp.</li>
 *   <li><b>Cô lập lỗi từng đơn</b>: mỗi đơn xử lý trong transaction REQUIRES_NEW riêng (ở
 *       {@link SeatIncidentService#cancelBookingForEmergency}); một đơn hỏng chỉ log + bỏ qua,
 *       không kéo đổ cả batch.</li>
 *   <li><b>Không dựa SecurityContext</b>: thread async không có ngữ cảnh bảo mật — phạm vi cơ sở
 *       và người xử lý đều truyền tường minh qua sự kiện.</li>
 * </ul>
 */
@Slf4j
@Service
@org.springframework.context.annotation.Profile("never")
@RequiredArgsConstructor
public class EmergencyClosureService {

    private static final String CLOSURE_REASON = "Rạp đóng cửa đột xuất / Sự cố hệ thống";
    /** Template đền bù: vé mời đền nguyên vé khi hủy chỗ (seed sẵn ở DataSeeder). */
    private static final String COMP_TEMPLATE_CODE = "COMP_TICKET_FULL";

    private final BookingRepository bookingRepository;
    private final PromotionRepository promotionRepository;
    private final SeatIncidentService seatIncidentService;
    private final MailService mailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCinemaClosed(CinemaEmergencyClosedEvent event) {
        try {
            List<Integer> bookingIds = bookingRepository.findConfirmedIdsByCinemaAndFutureShowtime(
                    event.cinemaId(), LocalDateTime.now());
            if (bookingIds.isEmpty()) {
                log.info("Đóng cửa cụm rạp #{}: không có đơn CONFIRMED nào cần đền bù.", event.cinemaId());
                return;
            }

            // Nạp MỘT LẦN template đền bù (dùng lại cho mọi đơn).
            Promotion template = promotionRepository.findByCode(COMP_TEMPLATE_CODE).orElse(null);
            Integer templateId = template != null ? template.getId() : null;
            String templateLabel = template != null && template.getName() != null
                    ? template.getName() : "Vé mời đền bù";
            if (templateId == null) {
                log.warn("Đóng cửa cụm rạp #{}: chưa seed template {} → sẽ chỉ hủy chỗ + ghi vết, không phát voucher.",
                        event.cinemaId(), COMP_TEMPLATE_CODE);
            }

            int cancelled = 0, mailed = 0, failed = 0;
            for (Integer bookingId : bookingIds) {
                try {
                    CancellationEmailData emailData = seatIncidentService.cancelBookingForEmergency(
                            bookingId, templateId, templateLabel, event.triggeredByStaffId(), CLOSURE_REASON);
                    cancelled++;
                    if (emailData != null) {
                        mailService.sendCancellationEmail(emailData);
                        mailed++;
                    }
                } catch (Exception ex) {
                    failed++;
                    log.error("Đền bù đóng cửa cụm rạp #{}: lỗi xử lý đơn #{}: {}",
                            event.cinemaId(), bookingId, ex.getMessage(), ex);
                }
            }
            log.info("Đóng cửa cụm rạp #{}: xử lý {} đơn (gửi {} email, {} lỗi).",
                    event.cinemaId(), cancelled, mailed, failed);
        } catch (Exception e) {
            // Bọc ngoài: sự cố toàn cục (vd query lỗi) không được ném ra khỏi luồng async
            log.error("Batch đền bù đóng cửa cụm rạp #{} thất bại: {}", event.cinemaId(), e.getMessage(), e);
        }
    }
}
