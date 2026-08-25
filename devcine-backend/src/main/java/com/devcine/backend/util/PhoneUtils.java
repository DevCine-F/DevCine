package com.devcine.backend.util;

import java.util.regex.Pattern;

/**
 * Tiện ích chuẩn hóa (sanitization) và kiểm tra định dạng (validation) số điện thoại Việt Nam.
 */
public final class PhoneUtils {

    private PhoneUtils() {}

    /**
     * Regex chuẩn 10 chữ số Việt Nam (đầu số 03x, 05x, 07x, 08x, 09x).
     */
    public static final Pattern VIETNAM_PHONE_PATTERN = Pattern.compile("^(0)(3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$");

    /**
     * Làm sạch chuỗi số điện thoại:
     * - Cắt khoảng trắng 2 đầu và loại bỏ toàn bộ khoảng trắng bên trong, dấu chấm (.), dấu gạch ngang (-), ngoặc đơn.
     * - Chuyển đổi tiền tố quốc gia (+84 hoặc 84 ở đầu) thành số 0.
     * - Trả về null nếu chuỗi rỗng sau khi làm sạch.
     */
    public static String sanitize(Object rawPhone) {
        if (rawPhone == null) return null;
        String s = rawPhone.toString().trim();
        if (s.isEmpty()) return null;

        // Loại bỏ khoảng trắng, dấu chấm, gạch ngang, ngoặc đơn
        s = s.replaceAll("[\\s.\\-()]", "");
        if (s.isEmpty()) return null;

        // Chuyển đổi +84 hoặc 84 ở đầu chuỗi thành 0
        if (s.startsWith("+84")) {
            s = "0" + s.substring(3);
        } else if (s.startsWith("84") && s.length() == 11) {
            s = "0" + s.substring(2);
        }

        return s.isEmpty() ? null : s;
    }

    /**
     * Kiểm tra số điện thoại đã được sanitize có đúng định dạng mạng di động Việt Nam (10 số).
     */
    public static boolean isValidVietnamPhone(String cleanPhone) {
        if (cleanPhone == null) return false;
        return VIETNAM_PHONE_PATTERN.matcher(cleanPhone).matches();
    }

    /**
     * Chuẩn hóa và kiểm tra định dạng.
     *
     * @param rawPhone Chuỗi SĐT thô
     * @param required true nếu bắt buộc phải có SĐT (không được null/rỗng)
     * @return Chuỗi SĐT đã chuẩn hóa hoặc null (nếu không required và không nhập)
     * @throws IllegalArgumentException Nếu dữ liệu không hợp lệ
     */
    public static String validateAndSanitize(Object rawPhone, boolean required) {
        String clean = sanitize(rawPhone);
        if (clean == null) {
            if (required) {
                throw new IllegalArgumentException("Vui lòng nhập số điện thoại.");
            }
            return null;
        }

        if (!isValidVietnamPhone(clean)) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (yêu cầu 10 chữ số thuộc các nhà mạng Việt Nam).");
        }

        return clean;
    }
}
