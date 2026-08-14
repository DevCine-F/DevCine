package com.devcine.backend.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Một lựa chọn đền bù (nguồn từ Promotion-template đã seed, mã bắt đầu "COMP_").
 * FE dựng dropdown từ danh sách này — KHÔNG hardcode.
 */
@Builder
public record CompensationOption(
        Integer promotionId,
        String code,               // COMP_FNB_COMBO | COMP_50K | COMP_100K | COMP_TICKET_FULL
        String label,              // "Tặng Combo Bắp nước", "Voucher giảm 50.000đ"...
        String type,               // DISCOUNT | GIFT_FNB | GIFT_TICKET
        BigDecimal discountValue,  // 50000 / 100000 / 0(quà)
        boolean cancelOnly         // true → chỉ hiện ở luồng Hủy chỗ (đền nguyên vé)
) {}
