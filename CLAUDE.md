# DevCine — Hướng dẫn cho Claude

## Đọc ngay khi bắt đầu phiên mới

```
project_context.md   — mục tiêu, kiến trúc, toàn bộ ngữ cảnh
RULES.md             — quy ước bắt buộc (vi phạm = phá hoại dự án)
```

## Project

Website quản lý rạp chiếu phim & đặt vé online (đồ án tốt nghiệp).

- **Backend:** Java 21 + Spring Boot 4.0.6 + Spring Data JPA + Hibernate 7 · cổng `:8080`
- **Frontend:** Vue 3.5 `<script setup>` + Pinia + Vue Router + Vite + TailwindCSS 4 · cổng `:5173`
- **DB:** PostgreSQL (Supabase) · **Auth:** JWT (JJWT 0.12.6) · **Ảnh:** Cloudinary · **Thanh toán:** VNPAY (khách) / CASH,CARD,TRANSFER (POS)
- **Ví điện tử đã bị gỡ hoàn toàn** — không còn Wallet/WalletTransaction/WalletService/WalletController

## Cấu trúc thư mục chính

```
devcine-backend/src/main/java/com/devcine/
  controller/     entity/     repository/
  service/        dto/        config/
devcine-frontend/src/
  views/admin/    views/customer/
  stores/         api/        routers/
```

## Quy tắc bắt buộc (tóm tắt — đọc RULES.md đầy đủ)

- **N+1:** tất cả quan hệ `LAZY`, dùng `@EntityGraph`/JOIN FETCH, cấm query trong vòng lặp
- **Layer:** Controller điều phối, business logic 100% ở Service, dùng DTO, không expose entity/password
- **Transaction:** ghi → `@Transactional`, đọc → `@Transactional(readOnly=true)`
- **Exception:** log.error + @ControllerAdvice, cấm catch rỗng / printStackTrace
- **Bảo mật:** cấm hardcode secret, dùng application.properties/env; validate @Valid
- **Frontend:** chỉ `<script setup>`; state chia sẻ → Pinia; 4 trạng thái Loading/Empty/Error/Success; xóa/hủy phải Confirm; debounce search 300–500ms
- **Naming:** Entity PascalCase số ít · bảng/cột snake_case · Component PascalCase.vue · camelCase · UPPER_SNAKE_CASE · handler bắt đầu handle/on

## File bất khả xâm phạm (báo cáo trước khi sửa)

`SecurityConfig.java`, `JwtUtil.java`, `BookingService.java`, `PricingService.java`, `pom.xml`, `package.json`, `entity/*.java`

## Jackson / Spring Boot 4 gotcha

Dùng `tools.jackson.*` — KHÔNG phải `com.fasterxml.jackson.*`

## Gotchas thường gặp

- 403 che 500: thiếu @ControllerAdvice → Spring Security trả 403 thay vì 500
- Postgres null param: `lower(bytea)` lỗi khi truyền null vào `lower()` trong JPQL
- `@MapsId`: dùng `persist` không phải `merge` khi lưu entity có @MapsId

## Tiến độ hiện tại (~95%)

Xem memory `devcine-progress.md` để biết đã xong gì và còn lại gì.

**Đã GỠ HOÀN TOÀN — Kho / Định mức BOM (11/07/2026):** không còn `InventoryService`/`BomRecipe`/`BomController`/`/api/bom`/`InventoryController`/`/api/inventory`/màn `InventoryManagement.vue`. Tồn kho VÔ HẠN, không định mức. Chỉ giữ hiển thị + bán F&B/combo (`FnbController`, `FnbMenuManager`, `ConcessionSale`). Quyền gác màn này là **`fnb_menu`** (đổi tên từ `pos_inventory` ngày 21/07 vì nó gác THỰC ĐƠN chứ không phải kho).

**Đã GỠ HOÀN TOÀN — Phân hệ Ca làm việc & Bàn giao ca (01/08/2026):** không còn `Shift`/`StaffSchedule`/`ShiftHandover` (entity), `WorkPosition` (enum), `ShiftAccessService`/`ShiftHandoverService`/`StaffScheduleService`, các repo/DTO ca, cột `staff_schedule_id`/`Staff.default_position`, setting `SHIFT_OPENING_FLOAT`; đã gỡ mọi endpoint `/api/staff/shifts*`, `/handovers*`, `/shifts/{id}/check-in|check-out` và FE `ShiftHandover.vue`/`StaffShiftManagement.vue`/`MyShifts.vue`/`stores/shift.js`. **POS bán vé + Check-in QR nay chạy RBAC thuần** (`@perm.can('pos_ticketing',...)`) — STAFF đăng nhập là bán/soát được ngay.
- **Strict Cinema Scoping** (`SecurityUtils.assertCinemaAccess(targetCinemaId)`): ADMIN bỏ qua; STAFF/MANAGER thiếu `cinemaId` (JWT) hoặc thao tác **chéo cụm rạp** → **403**. Áp cho bán vé (`Showtime→Room→Cinema`), F&B (gán `cinema=staff.getCinema()`), soát/in vé (`Booking→Showtime→Cinema`).
- **Ghi vết người bán:** thêm cột `Booking.sold_by` + `ConcessionSale.sold_by`/`cinema_id` (thay `staff_schedule_id`). Đã verify runtime 4 kịch bản (bán/soát rạp mình OK, chéo rạp 403).
- **Phê duyệt sửa sai còn lại:** chỉ `FNB_VOID` (đổi ghế SEAT_MOVE đã gỡ); gate duyệt đổi từ ca → **role ADMIN/MANAGER** (`ApprovalService.requireApprover`).
- **File bất khả xâm phạm đã sửa (được duyệt):** `Booking.java`, `ConcessionSale.java` (thêm cột) — dùng `ddl-auto=update` tự migrate.

**Đã hoàn thiện — Chuẩn hoá lỗi & response (22/07/2026):**
- **Toast/FriendlyError:** phủ 44/49 view. KHÔNG còn toast/`notify`/`errMsg` tự chế — mọi nơi dùng `useToastStore` + `friendlyError`. CỐ Ý giữ im lặng ở dữ liệu phụ (banner trang trí, phim gợi ý, voucher đã lưu, thăm dò quyền đánh giá) vì báo lỗi chỉ gây nhiễu.
- **`ApiResponse<T>`:** mọi controller đã bọc, ngoại lệ DUY NHẤT là `PaymentController` (convention VNPAY `{code,message,data}`, không có cờ `success` nên interceptor bỏ qua). Xem memory `devcine-apiresponse-rollout.md` để biết các bẫy khi migrate.
- Route/method không khớp trả **404/405** thay vì 500.

**Đã hoàn thiện — Phân quyền & Dashboard theo cơ sở (22/07/2026):**
- Ma trận MANAGER/STAFF chỉ còn action backend THẬT SỰ enforce (hết "checkbox chết"); cờ seed `PERMISSION_MATRIX_V4`
- Feature `pos_inventory` đổi tên → **`fnb_menu`** (nó gác THỰC ĐƠN F&B, không phải kho); STAFF không còn quyền này
- Dashboard scoping theo cơ sở: `resolveCinemaScope()` — chỉ ADMIN được `cinemaId = null`; vai trò khác thiếu cơ sở thì **fail closed** (403), không mở toàn hệ thống
- "Người dùng mới" → **"Khách mới của cơ sở"** (lần đầu giao dịch tại cơ sở, dùng `NOT EXISTS`)
- Màn FAQ / Đánh giá phim chuyển sang `adminOnly` cho khớp `hasRole('ADMIN')` ở backend

**Còn lại:**
- 8 cảnh báo Dependabot (đụng `pom.xml`/`package.json` → báo cáo trước khi sửa)
- Nhánh fail-closed của dashboard chưa có tình huống thật để test (chưa có tài khoản MANAGER nào thiếu cơ sở)

## Commit convention

Commit 2 phần EN/VI. **TUYỆT ĐỐI không thêm Co-Authored-By.**
Remote: `DevCine-F` (frontend) / `DevCine` (backend).
