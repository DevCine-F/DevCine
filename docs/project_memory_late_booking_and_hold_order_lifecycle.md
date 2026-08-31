# Báo Cáo Cập Nhật Bộ Nhớ Dự Án: Phân Tách Khung Giờ Mở Bán & Vòng Đời Giữ Đơn (Late Booking & Hold Order Lifecycle)

> [!NOTE]
> Tài liệu này lưu trữ quy tắc nghiệp vụ cốt lõi và cơ chế kỹ thuật đã được chuẩn hóa về việc xử lý đơn hàng bán vé muộn sau giờ chiếu (Late Booking Window) và thời hạn giữ chỗ (Hold Duration) của hệ thống DevCine.

---

## 1. NGUYÊN TẮC NGHIỆP VỤ CỐT LÕI (Core Business Rules)

Hệ thống phân tách rạch ròi giữa 3 khái niệm thời gian:

1. **Khung giờ Mở Bán / Khởi tạo phiên (Booking Window & Session Start):**
   * Mốc bắt đầu giao dịch (`sessionStartedAt`) được xác lập **ngay khi nhân viên/khách hàng bấm chọn suất chiếu** ở Bước 1.
   * Suất chiếu cho phép chọn khi: `sessionStartedAt <= startTime + BOOKING_LATE_MINUTES`.
   * **Bảo vệ phiên:** Khi người dùng đã bắt đầu chọn suất chiếu trong khung giờ hợp lệ, toàn bộ quá trình chọn ghế, chọn bắp nước, áp voucher và quét mã QR đều được bảo vệ trong suốt thời hạn giữ đơn (`SEAT_HOLD_MINUTES`).
   * **Chống treo máy (Idle Guard):** Nếu khoảng cách từ `sessionStartedAt` đến thời điểm hiện tại vượt quá `SEAT_HOLD_MINUTES` (cấu hình động), hệ thống coi phiên đã hết hạn và từ chối tạo đơn.

2. **Thời gian Giữ chỗ Phiên đặt vé trực tiếp (`SEAT_HOLD_MINUTES` — 3–30 phút, mặc định 10):**
   * Áp dụng khi khách đặt Online (`BookingView.vue`) hoặc thu ngân đang chọn ghế trên màn hình POS (`TicketingPOS.vue`).
   * Đơn hàng được cấp **trọn vẹn thời gian chờ thanh toán** (`expiresAt = now + SEAT_HOLD_MINUTES`, cấu hình động từ Admin).
   * **Quy tắc bất biến:** Thời hạn `expiresAt` tuyệt đối **không bị cắt cụt** theo mốc đóng bán `startTime + BOOKING_LATE_MINUTES`.
   * Khách hàng / Thu ngân có toàn bộ khoảng thời gian chờ để hoàn tất thanh toán (quét QR, chuyển tiền, thanh toán tiền mặt) ngay cả khi thời điểm thanh toán thực tế đã vượt qua mốc kết thúc bán vé trễ.

3. **Thời gian Lưu đơn chờ tại quầy POS (`POS_ORDER_HOLD_MINUTES` — 3–60 phút, mặc định 15):**
   * Áp dụng khi thu ngân tại quầy bấm nút **"Giữ đơn"** để tạm treo hóa đơn và phục vụ khách hàng tiếp theo.
   * Thời hạn lưu giữ `expiresAt = now + POS_ORDER_HOLD_MINUTES` (tự động kẹp tối đa bằng giờ suất chiếu bắt đầu `startTime`).
   * Quá hạn, đơn chuyển sang `EXPIRED`, hệ thống tự động giải phóng ghế trên Redis/DB, gửi WebSocket thông báo và áp dụng phạt 5 phút với ghế bị bỏ rơi trên máy POS đó.

---

## 2. TRIỂN KHAI KỸ THUẬT (Technical Implementation)

### 2.1. Backend (`BookingService.java`, `TicketingController.java`, `BookingRequestDTO.java`)
- **Truyền `sessionStartedAt` trong DTO & Controller:**
  * `BookingRequestDTO` mang trường `private LocalDateTime sessionStartedAt;`.
  * `TicketingController` parse chuỗi ISO 8601 từ request body (`/ticketing/pay`, `/ticketing/hold`) sang `LocalDateTime`.
- **Kiểm tra hợp lệ theo mốc bắt đầu phiên kết hợp Idle Guard (`BookingService.java`):**
  ```java
  int lateMinutes = systemSettingService.getBookingLateMinutes();
  int holdMinutes = systemSettingService.getSeatHoldMinutes();

  LocalDateTime effectiveStartTime = request.getSessionStartedAt() != null
          ? request.getSessionStartedAt()
          : LocalDateTime.now();

  // Chống treo màn hình: nếu sessionStartedAt quá cũ (> holdMinutes) -> dùng giờ thực tế
  if (request.getSessionStartedAt() != null
          && request.getSessionStartedAt().isBefore(LocalDateTime.now().minusMinutes(holdMinutes))) {
      effectiveStartTime = LocalDateTime.now();
  }

  boolean isStartedWithinAllowedWindow = showtime.getStartTime() == null
          || !effectiveStartTime.isAfter(showtime.getStartTime().plusMinutes(lateMinutes));

  boolean isContinuationOfValidHold = oldBooking != null
          && oldBooking.getExpiresAt() != null
          && oldBooking.getExpiresAt().isAfter(LocalDateTime.now())
          && (showtime.getStartTime() == null || oldBooking.getCreatedAt() == null
              || !oldBooking.getCreatedAt().isAfter(showtime.getStartTime().plusMinutes(lateMinutes)));

  if (!isContinuationOfValidHold && !isStartedWithinAllowedWindow) {
      throw new RuntimeException("Suất chiếu đã quá giờ cho phép đặt vé (quá " + lateMinutes + " phút sau khi bắt đầu).");
  }
  ```
- **Hoàn tất đơn (`completePayment`):**
  * Chỉ kiểm tra trạng thái đơn (`status == HOLD`, chưa bị `EXPIRED` hoặc `CANCELLED`).
  * Không kiểm tra lại `lateMinutes` tại thời điểm hoàn tất thanh toán.

### 2.2. Frontend POS & Online (`TicketingPOS.vue`, `booking.js`)
- **POS (`TicketingPOS.vue`):**
  * `sessionStartedAt.value = new Date().toISOString()` ngay khi gọi `selectShowtime(st)`.
  * Tự động reset về `null` khi `resetPOS()`.
  * Gửi `sessionStartedAt` trong payload của `openQrModal`, `processPayment` và `holdCurrentOrder`.
- **Online (`stores/booking.js`):**
  * Ghi nhận `sessionStartedAt` khi gọi `setShowtime()` và truyền trong payload của `holdSeatsAndProceed`.

---

## 3. QUY ƯỚC BẢO TRÌ (Maintenance Conventions)

> [!IMPORTANT]
> Khi phát triển hoặc nâng cấp các kênh bán vé mới (Mobile App, Kiosk tự phục vụ, Cổng đối tác OTA):
> 1. Luôn ghi nhận mốc thời gian bắt đầu chọn suất chiếu của người dùng (`sessionStartedAt`) và gửi lên Backend trong yêu cầu giữ chỗ / tạo đơn.
> 2. Luôn áp dụng cơ chế 2 pha: Tạo giữ chỗ (`HOLD`) trước khi chuyển khách hàng sang cổng thanh toán.
> 3. Không bao giờ tái kiểm tra `lateMinutes` ở giai đoạn Callback / Webhook / Hoàn tất thanh toán (`completePayment`).
