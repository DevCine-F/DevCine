package com.devcine.backend.dto;

/**
 * Định dạng phản hồi API thống nhất toàn hệ thống.
 * Giữ {@code message} ở top-level để tương thích với FE hiện tại.
 *
 * @param success trạng thái xử lý
 * @param message thông điệp cho người dùng (null nếu không có)
 * @param data    dữ liệu trả về (null khi lỗi hoặc không có body)
 * @param errors  chi tiết lỗi field khi validate (null nếu không phải lỗi validate)
 */
public record ApiResponse<T>(boolean success, String message, T data, Object errors) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(false, message, null, null);
    }

    public static ApiResponse<Void> fail(String message, Object errors) {
        return new ApiResponse<>(false, message, null, errors);
    }
}
