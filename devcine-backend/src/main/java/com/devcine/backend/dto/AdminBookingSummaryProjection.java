package com.devcine.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AdminBookingSummaryProjection {
    Integer getBookingId();
    String getBookingCode();
    Boolean getIsConcession();
    String getOrderType();
    String getStatus();
    String getPaymentMethod();
    BigDecimal getTotalPrice();
    BigDecimal getFinalPrice();
    LocalDateTime getCreatedAt();
    String getCustomerName();
    String getChannel();
    String getMovieTitle();
    String getRoomName();
    String getShowtimeStart();
    Long getSeatCount();
    Long getFnbItemCount();
    Boolean getHasFnb();
}
