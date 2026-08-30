package com.devcine.backend.dto.response;

import com.devcine.backend.entity.SeatIncident;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Dòng lịch sử sự cố cho màn đối soát. */
@Builder
public record IncidentListItem(
        Integer id,
        String type,                 // RELOCATE | CANCEL | SEAT_MAINTENANCE
        LocalDateTime createdAt,
        String bookingCode,
        String oldSeatLabel,
        String newSeatLabel,
        String compensationType,
        BigDecimal compensationAmount,
        String voucherCode,
        String auditGiftCode,
        String handledByName,
        String reason
) {

    public static IncidentListItem from(SeatIncident si) {
        String voucherCode = si.getVoucher() != null && si.getVoucher().getPromotion() != null
                ? si.getVoucher().getPromotion().getCode() : null;
        String handledBy = si.getHandledBy() != null && si.getHandledBy().getUser() != null
                ? si.getHandledBy().getUser().getFullName() : null;
        return IncidentListItem.builder()
                .id(si.getId())
                .type(si.getIncidentType())
                .createdAt(si.getCreatedAt())
                .bookingCode(si.getBooking() != null ? si.getBooking().getBookingCode() : null)
                .oldSeatLabel(si.getOldSeatLabel())
                .newSeatLabel(si.getNewSeatLabel())
                .compensationType(si.getCompensationType())
                .compensationAmount(si.getCompensationAmount())
                .voucherCode(voucherCode)
                .auditGiftCode(si.getAuditGiftCode())
                .handledByName(handledBy)
                .reason(si.getReason())
                .build();
    }
}
