package com.devcine.backend.event;

/**
 * Sự kiện miền: một Cụm rạp vừa chuyển sang trạng thái đóng cửa đột xuất
 * ({@code MAINTENANCE} / {@code CLOSED}) từ một trạng thái còn bán vé.
 *
 * <p>Được {@code CinemaServiceImpl.updateCinema} phát ra SAU khi đã hủy đồng bộ
 * các suất chiếu tương lai (trong cùng transaction). Một
 * {@code @TransactionalEventListener(AFTER_COMMIT)} chạy {@code @Async} sẽ tiêu thụ
 * sự kiện để hủy chỗ + đền bù + gửi email cho các đơn CONFIRMED bị ảnh hưởng —
 * KHÔNG chặn API cập nhật rạp.</p>
 *
 * <p>Cố ý chỉ mang <b>kiểu nguyên thủy (ID)</b>, không mang Entity: luồng nền tự
 * nạp lại dữ liệu trong transaction của chính nó → tránh mọi
 * {@code LazyInitializationException} do entity detached qua ranh giới async.</p>
 *
 * @param cinemaId          cơ sở vừa đóng cửa
 * @param triggeredByStaffId userId của người thực hiện (thường ADMIN). Dùng ghi vết
 *                          {@code handled_by}; null (hoặc không phải nhân sự quầy)
 *                          ⇒ ghi vết là thao tác HỆ THỐNG.
 */
public record CinemaEmergencyClosedEvent(Integer cinemaId, Integer triggeredByStaffId) {}
