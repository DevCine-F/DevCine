package com.devcine.backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ngữ cảnh giỏ hàng để chấm điều kiện & tính số giảm THỰC của voucher ngay tại bước áp mã
 * (không cần đợi tới lúc đặt vé). Dùng cho endpoint {@code POST /api/vouchers/preview}.
 */
@Data
public class VoucherPreviewRequest {
    private Integer customerId;
    private Integer movieId;                 // phim của suất đang đặt — để khớp mã theo phim
    private BigDecimal fnbTotal;             // tổng tiền bắp nước
    private List<BigDecimal> seatPrices;     // giá từng ghế (đã theo đối tượng) — để tính base & đơn tối thiểu
    private Integer heldBookingId;           // ID đơn đang giữ của phiên hiện tại (để bỏ qua không tự coi là đơn khác)
    private String sessionId;                // Session ID của phiên giữ chỗ (POS hoặc Online)
    private String channel;                  // Kênh đặt vé (POS hoặc ONLINE)
}
