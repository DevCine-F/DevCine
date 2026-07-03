package com.devcine.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffShiftRequest {

    @NotNull(message = "Vui lòng chọn nhân viên")
    private Integer staffId;

    @NotNull(message = "Vui lòng chọn ngày làm việc")
    private LocalDate workDate;

    @NotNull(message = "Vui lòng nhập giờ bắt đầu")
    private LocalTime startTime;

    @NotNull(message = "Vui lòng nhập giờ kết thúc")
    private LocalTime endTime;

    @NotNull(message = "Vui lòng chọn cơ sở")
    private Integer cinemaId;

    @NotBlank(message = "Vui lòng chọn vị trí làm việc")
    @Size(max = 80, message = "Vị trí làm việc tối đa 80 ký tự")
    private String workPosition;

    @Size(max = 120, message = "Điểm làm việc tối đa 120 ký tự")
    private String location;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;
}
