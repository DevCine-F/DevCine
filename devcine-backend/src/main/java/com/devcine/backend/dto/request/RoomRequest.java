package com.devcine.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRequest {

    @NotBlank(message = "Tên phòng không được để trống")
    @Size(min = 2, max = 50, message = "Tên phòng phải từ 2 đến 50 ký tự")
    private String name;

    private String type;    // validate enum ở service

    private String status;  // validate enum ở service

    @Min(value = 0, message = "Thời gian dọn phòng không hợp lệ")
    @Max(value = 120, message = "Thời gian dọn phòng tối đa 120 phút")
    private Integer turnaroundTimeMins;

    @NotNull(message = "Số hàng ghế không được để trống")
    @Min(value = 1, message = "Số hàng tối thiểu 1")
    @Max(value = 26, message = "Số hàng tối đa 26 (A–Z)")
    private Integer matrixRow;

    @NotNull(message = "Số cột ghế không được để trống")
    @Min(value = 1, message = "Số cột tối thiểu 1")
    @Max(value = 50, message = "Số cột tối đa 50")
    private Integer matrixCol;
}
