package com.devcine.backend.config;

import com.devcine.backend.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /** Lỗi validate @Valid trên request DTO → 400, kèm chi tiết từng field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.putIfAbsent(fe.getField(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Không hợp lệ"));
        String message = fieldErrors.values().stream().findFirst().orElse("Dữ liệu không hợp lệ.");
        return ResponseEntity.badRequest().body(ApiResponse.fail(message, fieldErrors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Bạn không có quyền thực hiện thao tác này.";
        return new ResponseEntity<>(ApiResponse.fail(message), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Dữ liệu không hợp lệ.";
        return new ResponseEntity<>(ApiResponse.fail(message), HttpStatus.BAD_REQUEST);
    }

    /** Vi phạm ràng buộc nghiệp vụ (vd: danh mục đang được sử dụng) → 409. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(IllegalStateException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Không thể thực hiện thao tác do xung đột trạng thái.";
        return new ResponseEntity<>(ApiResponse.fail(message), HttpStatus.CONFLICT);
    }

    /** Vi phạm toàn vẹn dữ liệu DB (vd: trùng số điện thoại / email do ghi đồng thời) → 409. */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        if (rootMsg != null && (rootMsg.contains("uk_users_phone") || rootMsg.contains("users_phone"))) {
            return new ResponseEntity<>(ApiResponse.fail("Số điện thoại này đã tồn tại trong hệ thống. Vui lòng kiểm tra lại."), HttpStatus.CONFLICT);
        }
        if (rootMsg != null && rootMsg.contains("users_email_key")) {
            return new ResponseEntity<>(ApiResponse.fail("Email này đã được sử dụng bởi tài khoản khác."), HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(ApiResponse.fail("Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc hệ thống."), HttpStatus.CONFLICT);
    }

    /**
     * URL không khớp handler nào → 404. Không có handler riêng thì nó rơi vào
     * {@link #handleAllExceptions} và thành 500 "Lỗi hệ thống nội bộ" — gõ nhầm địa chỉ
     * mà báo sập hệ thống là sai và gây hoang mang.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException ex) {
        return new ResponseEntity<>(ApiResponse.fail("Không tìm thấy đường dẫn yêu cầu."), HttpStatus.NOT_FOUND);
    }

    /** Đúng đường dẫn nhưng sai phương thức (vd: GET vào endpoint chỉ nhận POST) → 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return new ResponseEntity<>(ApiResponse.fail("Phương thức " + ex.getMethod() + " không được hỗ trợ cho đường dẫn này."),
                HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled RuntimeException: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(ApiResponse.fail(ex.getMessage() != null ? ex.getMessage() : "Lỗi xử lý nghiệp vụ."), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception ex) {
        log.error("Unhandled backend exception", ex);
        return new ResponseEntity<>(ApiResponse.fail("Lỗi hệ thống nội bộ: " + ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
