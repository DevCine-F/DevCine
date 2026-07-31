package com.devcine.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Chi tiết một suất chiếu cho drawer quản trị: thông tin phim + phòng/rạp + khung giờ
 * và số liệu THỰC TẾ (vé đã bán/giữ, doanh thu vé) — thay dữ liệu hardcode ở FE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeDetailResponse {
    private Integer id;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Phim
    private Integer movieId;
    private String movieTitle;
    private String movieTitleVietnamese;
    private String posterUrl;
    private String ageRating;
    private Integer durationMins;
    private String director;
    private String castMembers;
    private String description;
    private String versionType;
    private Set<String> genres;

    // Định dạng + phòng/rạp
    private Integer formatId;
    private String formatName;
    private Integer roomId;
    private String roomName;
    private String cinemaName;

    // Số liệu thực tế
    private int totalSeats;     // ghế bán được của phòng (loại trừ bảo trì/khóa)
    private long soldSeats;     // ghế đã bán (SOLD)
    private long heldSeats;     // ghế đang giữ (HOLD)
    private long availableSeats;
    private BigDecimal revenue; // doanh thu vé thực tế (SUM priceSnapshot của ghế SOLD)
}
