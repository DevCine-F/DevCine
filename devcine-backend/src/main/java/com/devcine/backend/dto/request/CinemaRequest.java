package com.devcine.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CinemaRequest {

    @NotBlank(message = "Tên cụm rạp không được để trống")
    @Size(min = 5, max = 100, message = "Tên cụm rạp phải từ 5 đến 100 ký tự")
    // Chỉ cho phép chữ (Unicode), số, khoảng trắng và các dấu cơ bản - & _
    @Pattern(regexp = "^[\\p{L}\\p{N} \\-&_]+$", message = "Tên cụm rạp chứa ký tự không hợp lệ")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(min = 10, max = 255, message = "Địa chỉ phải từ 10 đến 255 ký tự")
    private String address;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    @Size(max = 100, message = "Tỉnh/Thành phố quá dài")
    private String city;

    @NotBlank(message = "Quận/Huyện không được để trống")
    @Size(max = 100, message = "Quận/Huyện quá dài")
    private String district;

    private String type;

    @NotBlank(message = "Hotline không được để trống")
    // Hotline gửi lên dưới dạng RAW (chỉ chữ số) — FE đã loại bỏ ký tự định dạng
    @Pattern(regexp = "^\\d{8,11}$", message = "Hotline phải gồm 8 đến 11 chữ số")
    private String hotline;

    private Integer rooms;

    // Nếu có nhập: phải là URL http/https kết thúc bằng đuôi ảnh hợp lệ
    @Pattern(regexp = "^(https?://).+\\.(?i)(jpg|jpeg|png|webp)(\\?.*)?$",
            message = "Ảnh phải là URL http(s) hợp lệ kết thúc bằng .jpg/.jpeg/.png/.webp")
    private String imageUrl;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description;

    private Double latitude;

    private Double longitude;

    private String amenities;

    private String status;

    private Integer managerId;
}
