package com.devcine.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dữ liệu tạo/sửa FAQ trang Hỗ trợ.
 * Ràng buộc đồng bộ với cột entity Faq (category ≤100, question ≤500).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqRequest {

    @NotBlank(message = "Danh mục không được để trống")
    @Size(max = 100, message = "Danh mục tối đa 100 ký tự")
    private String category;

    @NotBlank(message = "Câu hỏi không được để trống")
    @Size(max = 500, message = "Câu hỏi tối đa 500 ký tự")
    private String question;

    private String answer;

    private Integer displayOrder;

    private Boolean isActive;
}
