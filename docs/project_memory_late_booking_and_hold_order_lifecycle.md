# Báo Cáo Cập Nhật Bộ Nhớ Dự Án: Phân Tách Khung Giờ Mở Bán & Vòng Đời Giữ Đơn (Late Booking & Hold Order Lifecycle)

> [!NOTE]
> Tài liệu này lưu trữ quy tắc nghiệp vụ cốt lõi và cơ chế kỹ thuật đã được chuẩn hóa về việc xử lý đơn hàng bán vé muộn sau giờ chiếu (Late Booking Window) và thời hạn giữ chỗ (Hold Duration) của hệ thống DevCine.

---

## 1. NGUYÊN TẮC NGHIỆP VỤ CỐT LÕI (Core Business Rules)

Hệ thống phân tách rạch ròi giữa 2 khái niệm thời gian:

1. **Khung giờ Mở Bán / Khởi tạo đơn (Booking Creation Window):**
   * Giới hạn thời điểm khách hàng hoặc nhân viên được phép **bắt đầu giao dịch / chọn suất chiếu**.
   * Suất chiếu cho phép mua vé trước giờ chiếu và trong khoảng trễ: `startTime <= now <= startTime + BOOKING_LATE_MINUTES`.
   * Khi thời gian thực tế vượt qua mốc `startTime + BOOKING_LATE_MINUTES`, hệ thống từ chối khởi tạo đơn mới (`holdSeats`).

2. **Vòng đời Giữ đơn / Chờ thanh toán (Hold Order Lifetime):**
   * Sau khi đơn hàng đã được khởi tạo (`status = HOLD`) thành công trong khung giờ hợp lệ, đơn hàng được cấp **trọn vẹn thời gian chờ thanh toán** (`expiresAt = now + SEAT_HOLD_MINUTES`, ví dụ 10 phút).
   * **Quy tắc bất biến:** Thời hạn `expiresAt` tuyệt đối **không bị cắt cụt** theo mốc đóng bán `startTime + BOOKING_LATE_MINUTES`.
   * Khách hàng / Thu ngân có toàn bộ khoảng thời gian chờ để hoàn tất thanh toán (quét QR, chuyển tiền, thanh toán tiền mặt) ngay cả khi thời điểm thanh toán thực tế đã vượt qua mốc kết thúc bán vé trễ.

---

## 2. TRIỂN KHAI KỸ THUẬT (Technical Implementation)

### 2.1. Backend (`BookingService.java`)
- **Loại bỏ ép rút ngắn `expiresAt`:**
  ```java
  LocalDateTime now = LocalDateTime.now();
  LocalDateTime expiresAt = isContinuationOfValidHold && oldBooking != null && oldBooking.getExpiresAt() != null
          ? oldBooking.getExpiresAt()
          : now.plusMinutes(holdMinutes);
  ```
- **Cơ chế `isContinuationOfValidHold`:**
  ```java
  int lateMinutes = systemSettingService.getBookingLateMinutes();
  boolean isContinuationOfValidHold = oldBooking != null
          && oldBooking.getExpiresAt() != null
          && oldBooking.getExpiresAt().isAfter(LocalDateTime.now())
          && (showtime.getStartTime() == null || oldBooking.getCreatedAt() == null
              || !oldBooking.getCreatedAt().isAfter(showtime.getStartTime().plusMinutes(lateMinutes)));

  if (!isContinuationOfValidHold
          && showtime.getStartTime() != null
          && LocalDateTime.now().isAfter(showtime.getStartTime().plusMinutes(lateMinutes))) {
      throw new RuntimeException("Suất chiếu đã quá giờ cho phép đặt vé (quá " + lateMinutes + " phút sau khi bắt đầu).");
  }
  ```
- **Hoàn tất đơn (`completePayment`):**
  * Chỉ kiểm tra trạng thái đơn (`status == HOLD`, chưa bị `EXPIRED` hoặc `CANCELLED`).
  * Không kiểm tra lại `lateMinutes` tại thời điểm hoàn tất thanh toán.

### 2.2. Frontend POS (`TicketingPOS.vue`)
- **Chủ động giữ chỗ khi mở Modal QR (`openQrModal`):**
  * Gọi `ticketingApi.hold(payload)` ngay khi thu ngân chọn "Chuyển khoản QR" để chốt thời điểm tạo đơn trong khung giờ hợp lệ và khóa ghế trên sơ đồ phòng chiếu.
  * Lưu `qrBookingId` và `restoredBookingId`.
- **Hoàn tất thanh toán (`processPayment`):**
  * Sử dụng `qrBookingId` / `restoredBookingId` đã giữ để gọi `mockWebhookSuccess` / `completePayment` mà không tạo mới lại đơn.
- **Tự động nhả ghế khi hủy modal (`closeQrModal`):**
  * Khi thu ngân bấm "Hủy" hoặc click ra ngoài modal QR, hệ thống tự động gọi `ticketingApi.releaseHold(qrBookingId)` để trả ghế về `AVAILABLE`.

---

## 3. QUY ƯỚC BẢO TRÌ (Maintenance Conventions)

> [!IMPORTANT]
> Khi phát triển hoặc nâng cấp các kênh bán vé mới (Mobile App, Kiosk tự phục vụ, Cổng đối tác OTA):
> 1. Luôn tuân thủ quy trình 2 pha: Tạo giữ chỗ (`HOLD`) trước khi chuyển khách hàng sang cổng thanh toán.
> 2. Không bao giờ tái kiểm tra `lateMinutes` ở giai đoạn Callback / Webhook / Hoàn tất thanh toán (`completePayment`).
