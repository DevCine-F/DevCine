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
 * Riêng luật "không chứa chữ số" nằm ở CategoryService để giữ message lỗi riêng biệt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(min = 2, max = 30, message = "Tên thể loại phải từ 2 đến 30 ký tự")
    @Pattern(regexp = "^[^@#$%^&*<>/,\\[\\]{}]*$", message = "Tên thể loại chứa ký tự không hợp lệ")
    private String name;

    @Size(max = 150, message = "Mô tả tối đa 150 ký tự")
    private String description;
}
