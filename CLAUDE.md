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

## Tiến độ hiện tại (~94%)

Xem memory `devcine-progress.md` để biết đã xong gì và còn lại gì.

**Đã hoàn thiện — Phân hệ Ca làm việc & Bàn giao ca (13/07/2026):**
- Chia ca theo Position + duyệt + check-in/out (lưu `actual_check_in_at/out_at` để hiển thị, KHÔNG tính lương/phạt)
- Bàn giao ca TỰ ĐỘNG (không cần Manager duyệt): nhập tiền → `difference = thực đếm − (quỹ đầu ca + DT tiền mặt)` → chốt COMPLETED + khóa POS
- Quỹ đầu ca cố định qua SystemSetting `SHIFT_OPENING_FLOAT` (mặc định 2.000.000)
- Preset ca Sáng/Chiều/Tối (cứng ở FE); màn Lịch sử bàn giao tô đỏ dòng lệch quỹ + cột Vào–Ra
- Đã gỡ: endpoint `/handovers/legacy`, `/shifts/{all,template}`, receive/confirm/reject/receivers; `Shift.status`; vị trí `PROJECTION`
- Phạm vi CỐ Ý bỏ (Quản lý Vận hành, không HRM): tính lương, phạt đi muộn, đơn nghỉ, đổi ca, tồn kho F&B

**Đang dở (chưa commit, 24/06/2026):**
- Rollout Toast/FriendlyError mới áp 5/43 view (cần toàn bộ view admin + khách còn lại)
- Admin UI cho BOM (định mức) — chưa có route
- Chuẩn hóa ApiResponse<T> + @ControllerAdvice toàn BE

## Commit convention

Commit 2 phần EN/VI. **TUYỆT ĐỐI không thêm Co-Authored-By.**
Remote: `DevCine-F` (frontend) / `DevCine` (backend).
