package com.devcine.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dữ liệu tạo/sửa định dạng chiếu (2D, 3D, IMAX 2D...).
 * Cho phép chữ số vì tên định dạng cần "2D"/"3D" — khác với thể loại.
 * Phụ thu & giá cố định KHÔNG nằm ở đây: chỉnh tại màn "Cấu hình giá".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieFormatRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(min = 2, max = 30, message = "Tên định dạng phải từ 2 đến 30 ký tự")
    @Pattern(regexp = "^[^@#$%^&*<>/,\\[\\]{}]*$", message = "Tên định dạng chứa ký tự không hợp lệ")
    private String name;

    @Size(max = 150, message = "Mô tả tối đa 150 ký tự")
    private String description;
}
