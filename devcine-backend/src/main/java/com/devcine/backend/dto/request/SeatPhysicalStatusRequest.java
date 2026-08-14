package com.devcine.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Khóa/mở trạng thái VẬT LÝ của ghế (bảo trì ghế hỏng). Set về MAINTENANCE sẽ chặn bán ghế này
 * ở MỌI suất tương lai (snapshot chỉ đông cứng khung; trạng thái vật lý overlay live theo seatId).
 */
public record SeatPhysicalStatusRequest(
        /** AVAILABLE | MAINTENANCE | LOCKED. */
        @NotBlank(message = "Thiếu trạng thái ghế.")
        String status,

        @Size(max = 255, message = "Lý do tối đa 255 ký tự.")
        String reason
) {}
