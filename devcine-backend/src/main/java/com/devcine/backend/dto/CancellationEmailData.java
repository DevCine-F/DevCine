package com.devcine.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu phẳng (primitives) phục vụ gửi email HỦY VÉ khi cụm rạp đóng cửa đột xuất.
 * Cố tình KHÔNG mang entity LAZY để luồng {@code @Async} gửi mail không bị
 * LazyInitializationException khi chạy ngoài transaction.
 *
 * @param voucherIssued true nếu đã phát voucher đền bù (khách có tài khoản)
 * @param voucherCode   mã voucher đền bù (vd COMP_TICKET_FULL) — null nếu không phát
 * @param voucherLabel  nhãn hiển thị voucher cho khách đọc
 * @param reason        lý do hủy (hiển thị lời xin lỗi)
 */
public record CancellationEmailData(
        String toEmail,
        String customerName,
        String bookingCode,
        String movieTitle,
        String cinemaName,
        String roomName,
        LocalDateTime startTime,
        List<String> seatLabels,
        boolean voucherIssued,
        String voucherCode,
        String voucherLabel,
        String reason
) {}
