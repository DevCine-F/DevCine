package com.devcine.backend.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/** Kết quả trả về sau khi đổi ghế / hủy chỗ thành công. */
@Builder
public record IncidentResultResponse(
        List<Integer> incidentIds,        // 1 dòng ghi vết / ghế
        List<SeatSwapResult> swaps,       // rỗng với luồng hủy chỗ
        CompensationResult compensation,
        BookingPrintResponse reprint,     // vé in lại (nhãn ghế mới) — tái dùng DTO có sẵn
        boolean emailResent               // true nếu đơn ONLINE đã gửi lại email vé
) {

    @Builder
    public record SeatSwapResult(
            String oldLabel,
            String newLabel,
            boolean downgrade             // true = hạ hạng vật lý (SWEETBOX/VIP → thấp hơn)
    ) {}

    @Builder
    public record CompensationResult(
            String type,                  // NONE | DISCOUNT | GIFT_FNB | GIFT_TICKET
            boolean voucherIssued,
            Integer voucherId,            // ID của Voucher vừa sinh (null nếu NONE / khách vãng lai)
            String voucherCode,           // Code của Promotion template (vd: COMP_50K) — dùng để giao tiếp với khách
            boolean counterGift,          // true = đền trực tiếp tại quầy (khách vãng lai)
            BigDecimal value
    ) {}
}
