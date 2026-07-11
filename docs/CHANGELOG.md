# Changelog — DevCine

Mọi thay đổi quan trọng được ghi nhận tại đây. AI Agent cập nhật sau mỗi session.

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
