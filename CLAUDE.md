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

**Đã hoàn thiện — Xử lý sự cố / Đổi ghế đền bù (14/08/2026):** phân hệ mới ở khu quản trị (`IncidentManagement.vue`, route `/admin/incidents`, feature quyền `incident_handling` view/handle — STAFF được handle, chịu Cinema Scoping). BE: entity `SeatIncident` (bảng `seat_incidents`) + `SeatIncidentService`/`SeatIncidentController` (`/api/staff/incidents/*`). Xem memory `devcine-incident-feature.md`.
- **Đổi ghế = REPOINT `BookingSeat.seat_id` TẠI CHỖ** → giữ nguyên Ticket/QR/giá; nhãn ghế suy live nên reprint & email tự đúng (KHÔNG sinh Ticket/QR mới).
- **Flat Pricing ⇒ đổi ghế cùng suất chênh lệch = 0đ** → đền theo **goodwill** (voucher quà/giảm), KHÔNG theo phép trừ. Chênh lệch > 0 chỉ khi HỦY chỗ (đền = giá vé). Không hoàn tiền.
- **Đền bù = Voucher từ Promotion-template `COMP_*`** (seed: COMP_FNB_COMBO/50K/100K/TICKET_FULL). `discountType` GIFT_* trị giá 0 để không lẫn vào giảm giá. Khách vãng lai (không Customer) → đền trực tiếp tại quầy, không sinh Voucher, chỉ ghi vết.
- **Khóa ghế hỏng** = set `Seat.seat_status=MAINTENANCE` (chặn bán mọi suất sau). Ghi vết `handled_by`+`cinema_id`. Idempotency & race-check → 409.
- **Ma trận quyền bump `PERMISSION_MATRIX_V4` → `V6`** (thực tế đã qua V5; nay V6 thêm `incident_handling` cho ADMIN/MANAGER/STAFF).
- **File bất khả xâm phạm:** tạo MỚI `entity/SeatIncident.java` (thêm bảng, `ddl-auto` tự tạo); sửa `TicketService.java` (+2 helper in lại/gửi email). Không đụng `BookingService`/`PricingService`.

**Đã hoàn thiện — Nâng cấp Quản lý khách hàng & Phân quyền theo cụm rạp (28/08/2026):**
- **Admin Customers UI (`AdminCustomers.vue`):** Bảng dữ liệu chuẩn Luxury Dark Mode, tag phân loại Thành viên / Vãng lai, rút gọn email ảo, hiển thị tổng chi tiêu & trạng thái, bộ lọc đa tiêu chí, phân trang 10/20/50 dòng, xuất file CSV Excel UTF-8 BOM, thay native select bằng Custom Luxury Dropdown.
- **3 Modal quản trị:** Chi tiết (3 tab Đơn hàng / Voucher / Biến động điểm + Thẻ VIP tiến trình), Chỉnh sửa (Họ tên, Ngày sinh), Khóa/Mở khóa tài khoản kèm lý do.
- **Cinema Scoping (`CustomerController`, `CustomerRepository`, `ConcessionSaleRepository`):** Với MANAGER/STAFF, chỉ cho phép xem/thao tác các khách hàng đã từng giao dịch (mua vé CONFIRMED hoặc mua F&B COMPLETED) tại rạp của mình (`hasAccessToCustomer`), chéo rạp trả về 403 Forbidden.
- **Tối ưu Backend:** Batch Aggregation Query O(1) tính tổng chi tiêu chống N+1; chặn tích/tiêu điểm và chặn gửi email reset password cho tài khoản bị khóa.
- **Tạm ẩn menu CSKH (`AdminLayout.vue`):** Tạm ẩn tab "Chăm sóc khách hàng" trên sidebar quản trị phục vụ review, bảo tồn toàn bộ route & code bên dưới.
- **Chuẩn hóa Ma trận Phân quyền V8 (`DataSeeder`, `AdminPermissions.vue`, `CinemaController`, `RoomController`, `SeatController`):**
  + MANAGER: Được phân quyền quản lý cụm rạp (`cinemas:view,edit` scoped), khách hàng (`customers:view,edit` scoped), lịch chiếu, nhân sự, hóa đơn, sự cố ghế và thống kê rạp mình. Đóng toàn bộ các quyền cấu hình toàn cục.
  + STAFF: Tinh gọn tuyệt đối, CHỈ có đúng 2 quyền: Bán vé (POS) và Kiểm soát vé (Check-in QR).
  + ADMIN: Toàn quyền toàn hệ thống.
  + UI AdminPermissions: Tái cấu trúc 4 tab trực quan, tích hợp Toast thông báo (`useToastStore` / `AppToast.vue`).

**Còn lại:**
- 8 cảnh báo Dependabot (đụng `pom.xml`/`package.json` → báo cáo trước khi sửa)
- Nhánh fail-closed của dashboard chưa có tình huống thật để test (chưa có tài khoản MANAGER nào thiếu cơ sở)
- Incident: chưa enforce orphan-check khi đổi cụm ghế (`allowOrphan` giữ sẵn); chưa verify end-to-end runtime (mới compile/build sạch)

## Commit convention

Commit 2 phần EN/VI. **TUYỆT ĐỐI không thêm Co-Authored-By.**
Remote: `DevCine-F` (frontend) / `DevCine` (backend).

