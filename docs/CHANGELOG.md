# Changelog — DevCine

Mọi thay đổi quan trọng được ghi nhận tại đây. AI Agent cập nhật sau mỗi session.

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
