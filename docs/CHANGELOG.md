# Changelog — DevCine

Mọi thay đổi quan trọng được ghi nhận tại đây. AI Agent cập nhật sau mỗi session.

---

## [1.7.1] — 2026-09-02

### Enhanced & Standardized — Cơ Chế Giữ Chỗ CSDL Sớm & Cập Nhật Liền Mạch (Early DB Hold & Mutation Flow)
- **Kích Hoạt Giữ Chỗ Database Sớm (`BookingView.vue`):**
  - Kích hoạt `ensureHeld()` tạo đơn hàng `HOLD` trong Database ngay khi khách hàng bấm *"Tiếp tục"* rời Bước 1 Chọn ghế, bảo vệ độc quyền 100% vị trí ghế trong CSDL xuyên suốt quá trình chọn bắp nước (Bước 2) và chọn voucher (Bước 3).
  - Ngay cả khi người dùng dừng lại lâu hoặc bị ngắt kết nối WebSocket/chuyển tab, trạng thái ghế vẫn được bảo vệ vững chắc trong CSDL theo cấu hình `SEAT_HOLD_MINUTES`.
- **Cập Nhật Đơn Liền Mạch Qua `heldBookingId` (`stores/booking.js` & `BookingService.java`):**
  - Truyền `heldBookingId: this.bookingId || null` trong payload của `holdSeatsAndProceed`.
  - Backend `BookingService.java` giải phóng an toàn đơn giữ cũ và tạo/cập nhật đơn mới trong cùng một transaction, bảo toàn mốc thời gian đếm ngược `expiresAt` ban đầu.
  - Triệt tiêu hoàn toàn lỗi Toast đỏ *"Một số ghế vừa được người khác đặt"* khi người dùng thao tác chậm hoặc dừng lâu giữa các bước.
- **Tối Ưu Trải Nghiệm & Đồng Bộ Đếm Ngược (`BookingView.vue`):**
  - Đồng bộ mốc đếm ngược `holdStartTs` với thời gian tạo đơn `heldAt` nhận từ Server.
  - Thêm hiệu ứng xoay spinner và nhãn *"Đang giữ chỗ..."* trên nút bấm Tiếp tục khi đang xử lý, ngăn chặn việc bấm liên tiếp gây race-condition.

---

## [1.7.0] — 2026-09-01

### Fixed & Standardized — Chuẩn hoá Luồng In Vé POS & Phân Tách Nghiệp Vụ Soát Vé Vào Cổng (POS Ticket Printing & Gate Check-in Differentiation)
- **POS Checkout Synchronization (`TicketingController.java`):**
  - Tự động gán `booking.setPrintedAt(now)` và `booking.setPrintedBy(soldBy)` vào CSDL ngay khi thu ngân thanh toán đơn hàng thành công tại quầy POS, ghi nhận chính xác việc vé giấy K80 đã được in trao tay khách tại quầy.
- **Backend Gate Check-in Endpoint (`TicketService.java`, `TicketController.java`, `BookingPrintResponse.java`):**
  - Bổ sung trường `isCheckedIn` và `alreadyPrinted` trong `BookingPrintResponse`.
  - Hỗ trợ tra cứu linh hoạt theo cả mã đơn `bookingCode` và mã QR vé đơn lẻ (`DEVCINE-T-...`).
  - Thêm endpoint `POST /api/tickets/checkin`: Thực hiện soát vé vào cổng cho các đơn đã in vé giấy (`isCheckedIn = true`, `checkInTime = now`), tuyệt đối không kích hoạt hay can thiệp vào lệnh in lại vé giấy K80.
  - Chống gian lận: Tự động chặn và trả lỗi cảnh báo khi quét lại mã đơn hàng đã check-in toàn bộ trước đó.
- **Frontend Kiểm Soát Vé Thông Minh (`TicketCheckIn.vue`):**
  - Tự động phân tách 2 kịch bản khi quét mã QR:
    - **Vé đã có vé giấy (Đơn POS hoặc Online đã lấy giấy):** Chuyển sang chế độ `GATE_CHECKIN`, hiển thị banner xanh *"Check-in Thành Công! Quý khách vào phòng chiếu"*, hoàn toàn không đếm ngược in vé và không bật popup in K80.
    - **Vé Online chưa in giấy:** Chuyển sang chế độ `PRINT_REQUIRED`, đếm ngược 3s tự động mở cửa sổ in vé giấy K80.
  - Tinh gọn giao diện: Loại bỏ nút in lại vé thừa thãi, chỉ giữ lại nút bấm "Quay lại màn hình quét" để thao tác nhanh.

---

## [1.6.9] — 2026-09-01

### Enhanced & Redesigned — Nâng cấp Giao diện Chi tiết Hoá đơn Quản trị & Tinh gọn Nhãn Trạng thái (Admin Booking Modal & Status Decluttering)
- **Bố cục 2 Cột Cân Bằng (`AdminBookings.vue`):**
  - Tái cấu trúc Modal Chi tiết hoá đơn thành bố cục 2 cột Master-Detail cân xứng 50/50:
    - **Cột Trái (Dịch vụ & Soát vé):** Thẻ Suất chiếu / Loại đơn $\rightarrow$ Danh sách Vé $\rightarrow$ Danh sách Bắp nước & Combo $\rightarrow$ Khung Mã QR Check-in dạng thanh ngang gọn gàng.
    - **Cột Phải (Chủ thể & Tài chính):** Thẻ Khách hàng & Thu ngân / Cổng TT $\rightarrow$ Khối Tổng kết thanh toán (Tiền vé, Bắp nước, Giảm giá, **TỔNG TIỀN**, Phương thức, Trạng thái).
- **Tinh Gọn & Loại Bỏ Trùng Lặp Nhãn Trạng Thái (Status Deduplication):**
  - Áp dụng nguyên tắc *"1 Nhiệm vụ - 1 Nhãn duy nhất"*:
    - **Header:** Duy nhất 1 badge đại diện cốt lõi cho toàn bộ đơn hàng (`HOÀN TẤT`, `HẾT HẠN`, `ĐÃ HUỶ`, `ĐANG GIỮ`).
    - **Tiêu đề QR:** Chỉ hiển thị badge soát vé (`ĐÃ CHECK-IN`, `QUÁ HẠN SUẤT CHIẾU`, `CHƯA CHECK-IN`) khi đơn là vé hợp lệ. Đơn hết hạn/huỷ chỉ làm mờ ảnh QR kèm chú thích nhẹ (*"Mã QR vô hiệu hoá do đơn hàng đã hết hạn/bị huỷ"*), loại bỏ badge lặp lại.
    - **Khối Tổng kết thanh toán:** Rút gọn về 3 nhãn dòng tiền thuần túy: `ĐÃ THANH TOÁN` (Xanh lá), `CHỜ THANH TOÁN` (Vàng), `CHƯA THANH TOÁN` (Cam). Loại bỏ các hậu tố thừa thãi `(HẾT HẠN)`, `(ĐÃ HUỶ GIAO DỊCH)`.
- **Chuẩn Hóa Nhãn Text:**
  - Đổi `"Tiền vé xem phim"` $\rightarrow$ `"Tiền vé"` (ngắn gọn, đối xứng hoàn hảo với *"Bắp nước & Combo"*).
  - Đổi `"PT:"` $\rightarrow$ `"Phương thức:"` (viết đầy đủ, chuyên nghiệp, không viết tắt cụt lủn).
- **Tối Ưu Thanh Lọc 1 Hàng & Chuẩn Hóa Bo Góc Sắc Nét (`AdminBookings.vue`):**
  - Chuyển đổi toàn bộ thanh lọc sang hệ thống Dropdown pills đồng bộ (`h-9` - 36px), rút gọn ô tìm kiếm `w-52` ~ `w-56` và nút Đặt lại chỉ còn icon vuông `w-9 h-9`, đảm bảo thanh lọc luôn nằm trọn trên 1 hàng duy nhất (< 850px) trên mọi màn hình Laptop.
  - Chuẩn hóa toàn bộ bo góc thanh lọc, ô nhập liệu, dropdown, date picker và nút bấm sang **`rounded-sm` (2px)** chuẩn 1:1 theo mẫu `MovieToolbar.vue`, loại bỏ tình trạng bo tròn quá mức dạng viên thuốc.
- **Bảo Vệ Đơn Hàng & Suất Chiếu Quá Hạn:**
  - Bổ sung tab lọc `Hết hạn` (`EXPIRED`) trên bảng quản trị hoá đơn.
  - Tự động nhận diện `isShowtimePast` để khóa/làm mờ mã QR khi suất chiếu đã kết thúc mà vé chưa check-in.
  - Khóa nút In lại hoá đơn đối với các đơn chưa hoàn tất thanh toán (`status !== 'CONFIRMED' && status !== 'COMPLETED'`).
  - Khóa tính điểm thành viên (`detailRewardPoints = 0`) đối với đơn không thành công.

---

## [1.6.8] — 2026-09-01

### Fixed & Standardized — Bảo toàn Snapshot Đơn giá F&B & Đồng bộ Chi tiết Vé Khách hàng (Invoice Snapshot & Ticket Details Synchronization)
- **Backend (`AdminBookingController.java`, `BookingController.java`, `BookingRepository.java`):**
  - Loại bỏ việc đọc `catalogPrice` từ entity live `fnbItem.getPrice()` khi xem chi tiết hoá đơn Admin; tính `finalUnitPrice = snapshot` và `basePrice = snapshot - totalSurcharge` cho cả đơn vé (`detail`) và đơn F&B lẻ (`getConcessionDetail`).
  - `BookingController.java`: Bổ sung tính toán `seatTotal` (`SUM(BookingSeat.priceSnapshot)`) trả về cho API lịch sử đặt vé khách hàng.
  - `BookingRepository.java`: Bổ sung `findConfirmedByCustomerIdWithDetails` chỉ trả về đơn `CONFIRMED`/`COMPLETED` cho lịch sử khách hàng, dọn sạch các đơn rác `HOLD`/`CANCELLED`/`EXPIRED` khỏi màn hình người dùng.
- **Frontend (`AdminBookings.vue` & `BookingHistoryView.vue`):**
  - `AdminBookings.vue`: Tối ưu `fnbLineTotal` và ưu tiên lấy trực tiếp `finalPrice` từ snapshot DB.
  - `BookingHistoryView.vue`: Gán đúng `selectedBooking.seatTotal` cho dòng *"Tiền vé & Ghế"*; chuẩn hóa nhãn loại ghế dạng `VIP - Người lớn x1`; định dạng danh sách tùy chọn F&B dạng bullet point sạch sẽ (`• Option (+phụ thu)`), loại bỏ tiền tố `Ô chọn...` đồng bộ 100% với Admin.

---

## [1.6.7] — 2026-09-01

### Enhanced & Standardized — Đồng bộ Kích thước Giao diện & Cơ chế Validate Inline Tài khoản nhận tiền VietQR (Admin Settings)
- **Đồng bộ Kích thước Form (`AdminSettings.vue`):** Quy chuẩn toàn bộ các ô nhập liệu số, chữ và dropdown select ngân hàng về chiều cao chuẩn 56px (`py-4 px-5 rounded-xl border border-outline-variant/10`), căn lề chuẩn suffix badge (Vé, Phút, VNĐ) tại `right-5 pr-20`.
- **Chuẩn hóa & Tự động định dạng Thông tin VietQR:**
  - **Số tài khoản (STK):** Tự động lọc chỉ nhận ký tự số `0–9`, chặn chữ và ký tự đặc biệt, giới hạn độ dài `4–20` chữ số theo chuẩn ngân hàng.
  - **Tên chủ tài khoản:** Tự động chuyển đổi sang chữ **IN HOA KHÔNG DẤU** (`A–Z` và khoảng trắng) theo thời gian thực khi người dùng gõ/dán, giới hạn tối đa 50 ký tự, tối thiểu 2 ký tự.
  - **Mô tả bao quát:** Cập nhật mô tả hỗ trợ cả kênh đặt vé trực tuyến (Online) và tại quầy (POS).
- **Cơ chế Báo lỗi trực tiếp (Real-time Inline Error):**
  - Kiểm tra và hiển thị viền đỏ sáng kèm thông báo lỗi và biểu tượng cảnh báo trực tiếp dưới từng ô nhập liệu (`text-red-500`, `border-red-500`) theo thời gian thực ngay khi người dùng thao tác.
  - Áp dụng nguyên tắc *All-or-Nothing*: Bắt buộc hoàn tất hợp lệ cả 3 trường (Ngân hàng, STK, Tên chủ TK) khi có cấu hình, hoặc cho phép để trống cả 3 nếu chưa áp dụng.
  - Loại bỏ hoàn toàn toast popup khi validate dữ liệu form, giữ giao diện sạch sẽ và trực quan.
- **Tối ưu Trải nghiệm Tải trang (Skeleton Loading State):** Bổ sung hiệu ứng skeleton animation (`isInitialLoading`) khi vừa vào trang Cài đặt, loại bỏ triệt để hiện tượng nhấp nháy/nhảy số mặc định trong thời gian chờ dữ liệu từ Database.

---

## [1.6.6] — 2026-09-01

### Enhanced — Tách biệt & Đồng bộ Cấu hình Thời gian Giữ chỗ Phiên đặt vé & Lưu đơn chờ POS (Dual Hold Timers)
- **Tách biệt 2 cấu hình thời hạn:**
  - **`SEAT_HOLD_MINUTES` (3–30 phút, mặc định 10):** Thời gian giữ chỗ trực tiếp trong phiên giao dịch (khách đặt Online hoặc thu ngân đang chọn ghế trên POS).
  - **`POS_ORDER_HOLD_MINUTES` (3–60 phút, mặc định 15):** Thời gian duy trì đơn hàng chờ khi thu ngân bấm "Giữ đơn" tại quầy POS để phục vụ khách khác trước.
- **Backend (`SystemSettingService.java` & `PendingOrderService.java`):** Quản lý tập trung các key setting, kẹp cận an toàn, tính `expiresAt` tự động kẹp theo giờ suất chiếu và đồng bộ TTL Redis cho các đơn giữ POS.
- **Admin Settings (`AdminSettings.vue`):** Loại bỏ khối Cấu hình chung không cần thiết; nâng cấp khối *Cấu hình thời gian giữ ghế & giữ đơn* thành bố cục 2 cột cân xứng, hỗ trợ tải và lưu đồng bộ cả 2 giá trị.
- **Màn hình Bán vé POS (`TicketingPOS.vue` & `usePosStore.js`):** Nạp động cấu hình, đếm lùi timer chọn ghế trực tiếp theo `seatHoldMinutes` và quản lý hạn từng đơn chờ theo `posOrderHoldMinutes` hoặc `expiresAt` từ Backend; cập nhật bộ kiểm thử `usePosStore.spec.js` đạt 100% pass.

---

## [1.6.5] — 2026-08-31

### Enhanced & Fixed — Chuẩn hóa số lượng vé & bộ đếm ghế đôi Sweetbox tại Quầy vé (Ticketing POS)
- **Chuẩn hóa sức chứa Sweetbox (`seatCapacity = 2`):** Chọn 1 ghế Sweetbox tự động yêu cầu đủ 2 vé (2 người xem), khởi tạo `NGƯỜI LỚN = totalRequiredTickets` ở Bước 3 và tính đúng tổng tiền 2 vé ($2 \times 105.000đ = 210.000đ$).
- **Khóa nút `[-]` chống thiếu vé (Stepper Guard):** Vô hiệu hóa nút `[-]` khi số lượng vé đang bằng đúng `totalRequiredTickets` (ví dụ `NGƯỜI LỚN = 2` cho 1 ghế Sweetbox hoặc `1` cho ghế đơn), ngăn chặn việc bấm giảm làm hỏng tỷ lệ vé yêu cầu ($1/2$ hoặc $0/1$).
- **Cơ chế 1-click transfer & Auto Balance:** Bấm `[+]` ở loại vé mong muốn sẽ tự động bớt 1 vé Người lớn và cộng sang loại đó; bấm `[-]` ở loại vé phụ (HSSV, Trẻ em) sẽ tự động hoàn vé về cho Người lớn, đảm bảo tổng vé luôn bằng 100% sức chứa ghế.
- **Đồng bộ Payload Backend (`BookingService.java`):** `buildSeatSelections()` sinh đủ 2 phần tử vé cho mỗi ghế Sweetbox, giải quyết triệt để lỗi ngoại lệ thiếu vé khi tạo đơn và thanh toán.
- **Hóa đơn in nhiệt K80 (`printInvoice`) & Biên lai:** Cập nhật hiển thị chi tiết số ghế và số vé tương ứng.

---

## [1.6.4] — 2026-08-31

### Enhanced & Fixed — Bảo vệ phiên giao dịch từ khi chọn suất chiếu (Session-Based Late Booking Protection)
- Bổ sung `sessionStartedAt` trong `BookingRequestDTO`, `TicketingController.java`, `TicketingPOS.vue` và `stores/booking.js`: Ghi nhận mốc thời gian người dùng bắt đầu chọn suất chiếu tại Bước 1.
- Kiểm tra khung giờ mở bán (`startTime + bookingLateMinutes`) đối chiếu với `sessionStartedAt` thay vì mốc bấm thanh toán ở bước cuối, bảo vệ toàn bộ quy trình chọn ghế, chọn bắp nước, áp voucher và quét mã QR.
- Tích hợp **Idle Guard**: Tự động hủy phiên nếu người dùng treo máy quá thời hạn giữ đơn (`SEAT_HOLD_MINUTES`) tính từ `sessionStartedAt`.
- Cấp trọn vẹn thời gian giữ đơn (`expiresAt`) tính từ mốc bắt đầu phiên, ngăn chặn dứt điểm lỗi `400 Bad Request` khi quét QR hoặc thu tiền sau giờ kết thúc mở bán.

---

## [1.6.3] — 2026-08-31

### Fixed — Chuẩn hóa Ngày vận hành (Operating Day) & Khắc phục lỗi chọn ngày tạo suất chiếu
- Khắc phục lỗi tạo suất chiếu ngày tương lai bị lùi về ngày hôm trước: Đồng bộ logic giữa ngày vận hành (Business Operating Day) và lịch dương thực tế (Calendar Date) trong `ShowtimeDrawer.vue` (`getActualDateTimeStr`).
- Tự động nhận diện suất ca đêm sau nửa đêm (`00:00` đến `< openMin` của rạp) để tăng 1 ngày lịch thực tế (`dateObj + 1`), giúp hàm `mapShow` khi tải lại lùi 1 ngày về đúng tab ngày vận hành mà Admin đang chọn.
- Luân chuyển dữ liệu ngày theo chuẩn ISO `YYYY-MM-DD` (`selectedDateIso`, `fullDate` trong `useShowtimes.js`), loại bỏ việc parse chuỗi `DD/MM` thủ công và xóa bỏ nguy cơ fallback sai về ngày hôm nay (`getLocalTodayStr`).
- Ràng buộc hai chiều `v-model:selected-date` giữa `CinemaManager.vue`, `CinemaShowtimesTab.vue` và `ShowtimeDrawer.vue`.

---

## [1.6.2] — 2026-08-31

### Fixed & Enhanced — Tách bạch Khung giờ mở bán và Vòng đời giữ đơn (Late Booking & Hold Window)
- Bỏ logic ép rút ngắn thời gian giữ đơn (`expiresAt`) về mốc `startTime + bookingLateMinutes` trong `BookingService.java`. Đơn hàng phát sinh hợp lệ được hưởng trọn vẹn thời gian chờ thanh toán (`now + SEAT_HOLD_MINUTES`).
- Bổ sung cơ chế `isContinuationOfValidHold`: Cho phép hoàn tất đơn hàng (`completePayment`) hoặc chuyển tiếp đơn giữ chỗ hợp lệ đã tạo trong khung giờ mở bán kể cả khi thời điểm thanh toán thực tế đã vượt qua mốc kết thúc bán vé trễ.
- Nâng cấp luồng POS Chuyển khoản QR (`TicketingPOS.vue`): Khởi tạo giữ đơn (`HOLD`) ngay khi thu ngân mở modal Chuyển khoản QR để chốt thời điểm tạo đơn và khóa ghế, hoàn tất trực tiếp trên đơn đã giữ khi xác nhận, và tự động gọi `releaseHold` giải phóng ghế nếu hủy/đóng modal.

---

## [1.6.1] — 2026-08-30

### Fixed — Chuyển đổi Validation tại nguồn cho luồng Đặt vé (Fail-Early)
- Khắc phục lỗi báo vi phạm ghế mồ côi muộn ở Bước 4 (Thanh toán) bằng cách chuyển toàn bộ kiểm tra nghiệp vụ về chặn ngay tại Bước 1 (Chọn vé & ghế).
- Nút "Tiếp tục" luôn ở trạng thái tương tác (bỏ :disabled), kích hoạt kiểm tra và hiển thị toast thông báo cụ thể khi người dùng bấm nút thay vì bị im lặng.
- Khóa chặt lỗ hổng nhảy cóc trên thanh tiến trình Stepper (goToStep): Chặn hoàn toàn việc người dùng click trực tiếp vào Step 2, 3, 4 khi Bước 1 chưa hợp lệ.
- Đồng bộ 100% thuật toán quét ghế mồ côi (validateSeatGap) giữa Frontend và Backend snapshot (tính đến lối đi AISLE, ghế bảo trì MAINTENANCE, và ghế đôi Sweetbox span=2).
- Bổ sung thông báo chi tiết khi ghế đôi Sweetbox chưa đủ 2 vé: "Ghế đôi [Tên ghế] cần có 2 vé để đặt. Vui lòng kiểm tra lại loại ghế hoặc bổ sung thêm số lượng vé."

---

## [1.6.0] — 2026-08-30

### 🔒 Fixed — An toàn xử lý sự cố ghế
- Đổi ghế mặc định khóa ghế nguồn sang `MAINTENANCE` trong cùng transaction và chỉ broadcast trạng thái sau commit.
- Chặn đổi chéo ghế đơn/Sweetbox, bắt buộc đền bù khi hạ hạng và kiểm tra ghế mồ côi ở backend sau khi đã khóa bi quan suất chiếu.
- Redis seat lock dùng owner token, tự gia hạn TTL và compare-and-delete; lock chỉ được nhả sau khi transaction DB commit.

### 🎫 Fixed — Thu hồi QR vé cũ
- Thêm bảng `ticket_qr_histories`; mỗi lần đổi ghế lưu QR/version cũ rồi sinh QR mới bằng UUID.
- `POST /api/tickets/verify-ticket` trả DTO an toàn, nhận diện QR cũ và cảnh báo ghế hiện hành.
- Màn Quét & In vé tự phân biệt booking code với QR vé lẻ; booking code giữ luồng xác minh/in, QR vé lẻ chạy check-in.

### 🛡️ Fixed — Kiểm soát đền bù
- Staff tối đa 50.000đ và tối đa 5 lần đền bù trong cửa sổ 8 giờ; vé mời/voucher lớn yêu cầu Manager hoặc Admin.
- Mã quà tại quầy của khách vãng lai được lưu vào `seat_incidents.audit_gift_code` và hiển thị trong lịch sử đối soát.
- Batch-load Ticket và Staff ngoài vòng lặp đổi ghế để loại N+1 query.

---

## [1.5.0] — 2026-07-11

### ✨ Changed — Tách bước Quét & In vé tại quầy
- Quét mã QR đơn giờ **chỉ XÁC MINH** đơn (endpoint mới `POST /api/tickets/lookup`, read-only, không đổi trạng thái) → màn **"Quét thành công"** + chi tiết đơn + nút **"In vé"**. Chỉ khi bấm nút mới gọi `POST /api/tickets/print` để **đánh dấu đã in** + mở cửa sổ in vé giấy, rồi chuyển sang **"Đã in vé"**.
- Vẫn chống in trùng ở cả 2 bước: đơn đã in → 400 *"Mã đặt vé này đã được in thành vé giấy trước đó..."*.
- BE: `TicketService.lookupByBookingCode` (`@Transactional(readOnly=true)`); FE `TicketCheckIn.vue` thêm trạng thái `printed` + hành động `doPrint`.

### ✨ Changed — Email vé (gọn lại + tách Online/POS)
- Khối "Vé & mã QR" chỉ còn **ảnh QR đơn** + dòng "quét 1 lần cho cả đơn (N ghế)"; bỏ danh sách ghế/loại vé và dòng "Vị trí ghế: ...".
- **Tách nội dung mail theo nguồn đơn** (dùng chung 1 template + cờ `TicketEmailData.showQr`):
  - **Đơn Online** (`staffSchedule == null`): hiện QR, tiêu đề *"Vé điện tử đơn ..."* (khách ra rạp quét in vé).
  - **Đơn POS / đã in vé giấy**: **ẩn QR**, tiêu đề *"Hoá đơn thanh toán đơn ..."* + dòng *"Cảm ơn bạn đã sử dụng dịch vụ tại DevCine. Chúc bạn xem phim vui vẻ!"*.
  - `completePayment` đặt `showQr` theo `staffSchedule`. Khi **in vé tại quầy** (`/tickets/print`) gửi thêm mail hoá đơn/cảm ơn (ẩn QR) **chỉ cho đơn Online gốc**; đơn POS đã nhận hoá đơn lúc thanh toán → bỏ qua để tránh làm phiền hộp thư.

### 🐛 Fixed — POS thanh toán
- Bỏ `@Transactional` khỏi controller `/api/ticketing/pay` & `/concession` → lỗi nghiệp vụ (vd **"Mỗi lần đặt tối đa N vé"**) không còn bị `UnexpectedRollbackException` đè thành 500 "Lỗi hệ thống nội bộ"; giữ làm tròn tiền mặt bằng lưu tường minh. POS chặn sớm khi chọn vượt `MAX_TICKETS_PER_BOOKING`.

### 💄 UI
- POS: chọn loại vé bằng **counter theo đối tượng** (tổng = số ghế) thay dropdown từng ghế; dropdown chọn **voucher của khách** làm lại thành custom dropdown khớp theme.

---

## [1.4.0] — 2026-07-11

### ✨ Changed — Mã QR theo ĐƠN HÀNG + In vé tại quầy
- **Mô hình QR:** 1 mã QR = **mã đặt vé** (đại diện cả đơn), thay cho QR từng ghế (`DEVCINE-T-...`). Email vé gộp ghế 1 dòng + 1 ô QR lớn; modal lịch sử đặt vé của khách & tem vé giấy (POS/in lại) cũng dùng chung 1 QR đơn.
- **Luồng quét & in vé:** `POST /api/tickets/print?code={bookingCode}` — quét QR đơn → validate đơn `CONFIRMED` & chưa in → in toàn bộ vé giấy + đánh dấu đã in. Quét lại đơn đã in → 400 chống trùng. Thay endpoint cũ `POST /api/tickets/check-in`.
- **Trạng thái cấp Đơn hàng:** thêm cột `bookings.printed_at`, `bookings.printed_by` (FK→staffs). Đồng bộ `tickets.is_checked_in` khi in để giữ báo cáo tiến độ nhất quán.
- **Backend:** `TicketService.printByBookingCode`, `BookingRepository.findByBookingCodeForPrint` (JOIN FETCH), DTO `BookingPrintResponse`. Bỏ khung giờ check-in cũ (in được khi đã thanh toán & chưa in).
- **Frontend:** màn "Quét & In vé" (`TicketCheckIn.vue`) hiện chi tiết đơn + tự mở cửa sổ in; `BookingHistoryView`, `invoiceTemplate.js`, POS đồng bộ 1 QR đơn.

---

## [1.3.0] — 2026-07-11

### 🗑️ Removed — Gỡ lớp Quản lý kho / Định mức (ngoài phạm vi đồ án)
- **Entity/Bảng:** xóa `CinemaInventory`, `InventoryLog`, `BomRecipe` (+ repository tương ứng). Bảng DB cũ (`cinema_inventory`, `inventory_logs`, `bom_recipes`) để lại rỗng, vô hại (`ddl-auto=update` không drop).
- **Service/Controller:** xóa `InventoryService`, `InventoryController` (`/api/inventory`), `BomController` (`/api/bom`).
- **Frontend:** xóa màn `InventoryManagement.vue`, route `/admin/inventory`, link sidebar "Quản lý kho (F&B)", và widget "Cảnh báo tồn kho" ở Dashboard (+ `DashboardStatsResponse.LowStockItem`).
- **Logic:** gỡ trừ/hoàn kho trong `BookingService.completePayment`, `ConcessionService.createSale`, `ApprovalService.executeFnbVoid` → **tồn kho vô hạn**, luôn cho thanh toán. Void hóa đơn F&B vẫn hoàn điểm + set VOIDED.

### ✅ Kept — Bán bắp nước & doanh thu
- POS bán F&B lẻ (`ConcessionSale`), Thực đơn F&B (`FnbMenuManager.vue`, `FnbController` `/api/fnbs`), bước chọn combo khi đặt vé; bảng `concession_sales`/`booking_fnbs` giữ để tổng hợp doanh thu.

---

## [1.2.0] — 2026-06-19

### 🔒 Security
- Tách secret VNPAY (`vnpay.tmnCode`/`vnpay.hashSecret`) khỏi `application.properties` sang biến môi trường `${VNPAY_TMN_CODE}`/`${VNPAY_HASH_SECRET}` (`.env` đã gitignore, thêm placeholder vào `.env.example`). ⚠️ Secret cũ đã lộ trong git history → cần rotate trên VNPAY.

### ✨ Features
- **POS Bán vé (TicketingPOS):** rewrite hoàn chỉnh — token auth, ghế/combo/giá thật, tra cứu thành viên tích điểm, CASH/CARD, tái dùng `holdSeats`+`completePayment` (trừ kho BOM). Verify 1 đơn thật.
- **Thực đơn F&B/Combo:** `FnbItem` thêm `imageUrl/description/isActive`; `FnbController` CRUD; admin UI `FnbMenuManager.vue` (`/admin/fnb`); booking chọn combo có ảnh/mô tả.
- **Voucher & Loyalty:** "Voucher của tôi" nối API + đổi điểm lấy ưu đãi, nhập/tra cứu/claim mã, áp voucher ở checkout. Phân tách voucher công khai vs mã bí mật. Thêm `Promotion.allowPointRedemption`.
- **Admin Khuyến mãi:** CRUD Promotion + phát voucher cho khách (API thật).
- **Quản lý khách hàng (mới):** `AdminCustomers.vue` (`/admin/customers`) + `GET /api/customers`.
- **Vé QR:** modal QR trong Lịch sử đặt vé.
- **Seed dữ liệu thật:** 6 phim curated + 45 suất chiếu; giá ghế NORMAL 110k / VIP 150k / SWEETBOX 300k (cờ `DEMO_SCHEDULE_SEEDED`).

### 🐛 Fixes
- Sửa bug giảm-giá-2-lần ở VNPAY (tách `finalPrice` khỏi `totalPrice`).
- Sửa N+1 trong `BookingController.getBookingHistory`.

---

## [1.1.0] — 2026-06-18

### ✨ Features (đợt hoàn thiện ưu tiên 1→8)
- **Audit Log tự động:** `AuditLogInterceptor` ghi mọi thao tác ghi-dữ-liệu ADMIN/STAFF + LOGIN (HandlerInterceptor, không AOP).
- **Phân quyền chi tiết:** `@EnableMethodSecurity` + `@PreAuthorize("@perm.can(...)")`, ma trận JSON ở `Role.permissionsMatrix`, quản lý qua `/api/admin/roles`, UI `AdminPermissions.vue`.
- **Đánh giá phim:** Review module (1 đánh giá/khách/phim) + UI sao trong `MovieDetail.vue`.
- **Tìm kiếm phim** (debounce 400ms) + **Khuyến mãi** (Promotion thật) + **Thông báo** (entity `Notification` + badge).
- **BOM (định mức):** `InventoryService.deductForSale` tự trừ kho khi `completePayment`; `BomController` (`/api/bom`).
- **Bàn giao ca:** endpoint `/api/staff/handovers`.

### 🧹 Refactor
- Atomic design cho cinema manager; sửa nhiều N+1 backend queries; dọn trùng lặp (xóa `AdminSchedule.vue`, `PaymentView.vue`).
- Showtime: UI + backend flow mới, statistics UI, configuration feature.

---

## [1.0.0] — 2026-05-26

### 🏗️ Foundation
- **Entity Layer:** Tạo 33 JPA Entity classes từ ERD
  - User & Auth: `Role`, `User`, `Customer`, `Staff`, `AuditLog`
  - Wallet: `Wallet`, `WalletTransaction`
  - Movie: `Movie`, `Category`, `MovieCategory`, `MovieFormat`
  - Cinema: `Cinema`, `Room`, `SeatType`, `Seat`
  - Booking: `Showtime`, `PricingRule`, `BookingSeat`, `Ticket`, `Review`
  - F&B: `FnbItem`, `BomRecipe`, `BookingFnb`, `CinemaInventory`, `InventoryLog`
  - Promotion: `Promotion`, `Voucher`
  - Staff: `Shift`, `StaffSchedule`, `ShiftHandover`
  - CMS: `SupportTicket`, `Banner`, `LostAndFound`

### 📚 Documentation
- Tạo Technical Design docs theo chuẩn LPT:
  - `docs/ARCHITECTURE.md` — Kiến trúc + tech stack + sơ đồ
  - `docs/DATABASE.md` — 33 bảng + quan hệ + migration rules
  - `docs/API_CONTRACTS.md` — 28 nhóm endpoint + request/response mẫu
  - `docs/CRITICAL_PATHS.md` — 7 luồng nghiệp vụ + protected files
  - `docs/SECURITY.md` — Quy tắc bảo mật bắt buộc
  - `RULES.md` — Quy tắc AI Agent

### ⚙️ Configuration
- Thêm `spring-dotenv` dependency vào pom.xml
- Đổi `ddl-auto` từ `create` sang `update`
- Thêm `dotenv-cli` + npm scripts: `dev:all`, `dev:backend`
