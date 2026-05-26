# Changelog — DevCine

Mọi thay đổi quan trọng được ghi nhận tại đây. AI Agent cập nhật sau mỗi session.

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
