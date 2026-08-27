package com.devcine.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dữ liệu tạo/sửa thể loại phim (Hành động, Kinh dị...).
 * Ràng buộc đồng bộ với chặn cứng phía Frontend (MovieCategoryManager.vue, tab Thể loại).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(min = 2, max = 50, message = "Tên danh mục phải từ 2 đến 50 ký tự")
    @Pattern(regexp = "^[^@#$%^&*<>/,\\[\\]{}]*$", message = "Tên danh mục chứa ký tự không hợp lệ")
    private String name;

    @Size(max = 150, message = "Mô tả không được vượt quá 150 ký tự")
    private String description;
}

