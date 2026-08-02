# DevCine — Hướng dẫn cho Codex

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

**Đang dở (chưa commit, 24/06/2026):**
- Rollout Toast/FriendlyError mới áp 5/43 view (cần toàn bộ view admin + khách còn lại)
- Admin UI cho BOM (định mức) & Bàn giao ca (API đã có, chưa có route)
- Chuẩn hóa ApiResponse<T> + @ControllerAdvice toàn BE

## Commit convention

Commit 2 phần EN/VI. **TUYỆT ĐỐI không thêm Co-Authored-By.**
Remote: `DevCine-F` (frontend) / `DevCine` (backend).

## L?ch s? Refactor (02/08/2026) - QUY CHU?N M�N H�NH POS B�N V�
- **B? c?c Card Phim**: Tu�n th? "GOM NH�M 2 C?P". C?p 1: Phim. C?p 2: �?nh d?ng & Ph�ng chi?u (VD: 2D PH? �? � PH�NG 223). B?T BU?C t? d?ng chu?n h�a ti?n t? "PH�NG" n?u API tr? v? s?.
- **X? l� D? li?u**: st.movie l� String, st.duration l� s? (g�n tr?c ti?p). KH�NG G?I API /api/admin/movies trong m�n POS tr�nh 404 l�m s?p trang. T? d?ng fallback d? li?u.
- **M�i gi? & Chu?i Ng�y**: Lu�n d�ng helper parseToDate(st) (thay kho?ng tr?ng b?ng 'T'). So s�nh ng�y b?ng getLocalYmd() (chu?n m�i gi? d?a phuong), TUY?T �?I KH�NG d�ng .toISOString().
- **UI/UX Dropdown**: Gi? n?n trong su?t g-transparent, hover d?i x�m nh? hover:bg-white/10, ch? 	ext-amber-400. Kh�ng d�ng g-amber-500/10. N�t gi? chi?u d?ng Pill, tang d?n.
- **Strict Isolation**: Ch? s?a d�ng file ch? d?nh, kh�ng t? � s?a helper/API chung. �?m b?o ite build xanh 100%.
