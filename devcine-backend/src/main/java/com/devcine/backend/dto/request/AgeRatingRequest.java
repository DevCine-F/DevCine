package com.devcine.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dữ liệu tạo/sửa mục kiểm duyệt độ tuổi (P, T13, T16, C18...).
 * Ràng buộc ở đây đồng bộ với chặn cứng phía Frontend (MovieCategoryManager.vue).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgeRatingRequest {

    @NotBlank(message = "Mã kiểm duyệt không được để trống")
    @Size(min = 1, max = 10, message = "Mã kiểm duyệt không vượt quá 10 ký tự")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Mã chỉ được chứa chữ cái và số không dấu (VD: P, T13)")
    private String code;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(min = 2, max = 50, message = "Tên danh mục phải từ 2 đến 50 ký tự")
    private String name;

    @Size(max = 150, message = "Mô tả không được vượt quá 150 ký tự")
    private String description;
}
