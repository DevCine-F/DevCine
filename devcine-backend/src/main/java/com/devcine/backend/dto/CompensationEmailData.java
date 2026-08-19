package com.devcine.backend.dto;

import java.time.LocalDateTime;

/**
 * Dữ liệu phẳng (primitives) phục vụ gửi email ĐỀN BÙ Đợt 2 sau khi Admin duyệt phát voucher, hoặc
 * email đổi ghế (Mẫu 3) từ luồng relocate. Cố tình KHÔNG mang entity LAZY để luồng gửi mail chạy
 * ngoài transaction không bị LazyInitializationException.
 *
 * <p>Định tuyến mẫu qua {@link #templateType}:
 * <ul>
 *   <li>{@code MONEY_VOUCHER} — Mẫu 1: voucher mệnh giá tiền (đơn thuần vé, giá trị cao)</li>
 *   <li>{@code TICKET_VOUCHER} — Mẫu 2: voucher đổi vé miễn phí (đơn thuần vé)</li>
 *   <li>{@code SEAT_CHANGE} — Mẫu 3: cáo lỗi đổi ghế (khách vẫn xem phim)</li>
 *   <li>{@code DUAL} — Mẫu 4: đền bù kép (voucher tiền/vé + voucher F&B) cho khách VIP / đơn có F&B</li>
 * </ul>
 *
 * @param voucherBenefitLabel nhãn quyền lợi voucher chính (vd "150.000 VNĐ", "100% vé", "20.000đ")
 * @param fnbVoucherCode      mã voucher F&B (Mẫu 4) — null nếu không áp dụng
 * @param counterGift         Mẫu 3: đền bằng hiện vật tại quầy (true) hay bằng voucher (false)
 * @param counterGiftLabel    Mẫu 3: mô tả quà tại quầy (vd "01 phần bắp/nước")
 * @param incidentReason      Mẫu 4: tên sự cố hiển thị (vd "bảo trì đột xuất")
 */
public record CompensationEmailData(
        String templateType,
        String toEmail,
        String customerName,
        String movieTitle,
        LocalDateTime showDate,
        // Voucher chính (tiền / vé)
        String voucherCode,
        String voucherBenefitLabel,
        LocalDateTime voucherExpiry,
        // Voucher F&B (Mẫu 4)
        String fnbVoucherCode,
        String fnbBenefitLabel,
        LocalDateTime fnbExpiry,
        // Mẫu 3 — quà tại quầy
        boolean counterGift,
        String counterGiftLabel,
        // Mẫu 4 — tên sự cố
        String incidentReason
) {
    public static final String MONEY_VOUCHER = "MONEY_VOUCHER";
    public static final String TICKET_VOUCHER = "TICKET_VOUCHER";
    public static final String SEAT_CHANGE = "SEAT_CHANGE";
    public static final String DUAL = "DUAL";
}
