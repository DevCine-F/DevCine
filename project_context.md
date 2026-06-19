# DevCine — Project Context

> File ngữ cảnh tổng hợp để đính kèm vào đầu mỗi phiên chat mới, giúp AI nắm nhanh dự án mà không phải đọc lại toàn bộ lịch sử hội thoại.
> Cập nhật lần cuối: 18/06/2026

---

## 1. Mục tiêu dự án

**DevCine** là hệ thống website **quản lý rạp chiếu phim & đặt vé xem phim trực tuyến** (fullstack, đồ án tốt nghiệp), phục vụ 2 nhóm người dùng:

- **Khách hàng:** duyệt phim → xem lịch chiếu → chọn ghế + combo F&B → áp voucher → thanh toán (VNPAY/Ví điện tử) → nhận vé QR → xem lịch sử/hồ sơ.
- **Quản trị/Nhân viên:** quản lý phim, suất chiếu, rạp/phòng/ghế, bán vé tại quầy (POS), check-in vé bằng QR, kho F&B, nhân sự & ca trực, khuyến mãi, định giá, dashboard, CSKH.

**Tính năng cốt lõi:** Đặt vé online, thanh toán ví/VNPAY, sinh & quét vé QR, loyalty points + hạng thành viên, quản trị vận hành rạp toàn diện.

---

## 2. Tech Stack

| Lớp | Công nghệ |
|---|---|
| **Backend** | Java 21 (LTS) · Spring Boot 4.0.6 · Spring Security · Spring Data JPA · Hibernate 7 · Lombok · Maven |
| **Frontend** | Vue.js 3.5 (`<script setup>` / Composition API) · Vite 8 · Pinia 3 · Vue Router 5 · TailwindCSS 4 · Axios |
| **Database** | PostgreSQL 15+ (hosted trên Supabase, region AWS Singapore) |
| **Auth** | JWT (JJWT 0.12.6, HMAC) + BCryptPasswordEncoder, stateless |
| **Thanh toán** | Cổng VNPAY + Ví điện tử nội bộ (Wallet) |
| **Lưu ảnh** | Cloudinary (poster, banner) |
| **Cổng/Port** | Backend `:8080` · Frontend `:5173` |

**Tài khoản seed sẵn (DataSeeder):** admin `admin / Admin@123` · khách demo `khachhang / Khach@123` (ví 500.000đ).

---

## 3. Quy ước Code (Coding Standards) — trích từ `RULES.md` + `docs/ARCHITECTURE.md`

### 3.1 Cấu trúc thư mục
```
devcine/
├── docs/                 # Tài liệu: ARCHITECTURE, DATABASE, API_CONTRACTS, CRITICAL_PATHS, SECURITY, CHANGELOG
├── RULES.md              # Quy tắc bắt buộc — ĐỌC ĐẦU TIÊN
├── devcine-backend/src/main/java/com/devcine/backend/
│   ├── config/  controller/  dto/  entity/  enums/  exception/  repository/  service/  util/
│   └── resources/application.properties
└── devcine-frontend/src/
    ├── components/  views/  layouts/  routers/  stores/  composables(hooks)/  utils/  api/
```
Quy mô hiện tại: **26 controller · 11 service · 34 entity · 30 repository** (BE) · **19 view khách + 20 view admin** (FE).

### 3.2 Quy tắc đặt tên
| Loại | Quy ước | Ví dụ |
|---|---|---|
| Entity (Java) | PascalCase, số ít | `BookingSeat`, `FnbItem` |
| Bảng DB | snake_case, số nhiều | `booking_seats`, `fnb_items` |
| Cột DB | snake_case | `created_at`, `price_snapshot` |
| Controller/Service/Repository | PascalCase + hậu tố | `MovieController`, `MovieService` |
| DTO | PascalCase + Request/Response | `CreateMovieRequest` |
| Component Vue | PascalCase.vue | `ShowtimeDetailsDrawer.vue` |
| Biến/Hàm | camelCase, rõ nghĩa (cấm viết tắt khó hiểu) | `fetchShowtimes` |
| Hằng số | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Event handler (Vue) | bắt đầu `handle`/`on` | `handleSubmit` |
| Enum | UPPER_SNAKE_CASE | `TICKET_STATUS.CHECKED_IN` |

### 3.3 Quy tắc BẮT BUỘC (vi phạm = phá hoại dự án)
- **N+1 query:** ❌ Cấm `FetchType.EAGER` (mọi quan hệ phải `LAZY`); dùng `@EntityGraph` hoặc `@Query JOIN FETCH`. ❌ Cấm gọi query trong vòng lặp `for/while`.
- **Layer:** Controller chỉ nhận request → gọi Service → trả `ResponseEntity`. **Business logic 100% ở Service.**
- **DTO:** ❌ Không map Entity thẳng ra Response (ẩn `password`, `salt`...). Dùng DTO.
- **Transaction:** API ghi dữ liệu gắn `@Transactional`; API đọc gắn `@Transactional(readOnly = true)`.
- **Exception:** ❌ Cấm `catch(Exception e){}` rỗng, `printStackTrace`, `System.out.println`. Dùng `log.error(...)` + `@ControllerAdvice`; không ném stack trace ra client.
- **Bảo mật:** ❌ Cấm hardcode credentials/secret (dùng `application.properties`/env). ❌ Cấm raw SQL (`nativeQuery`) trừ đường cùng. Validate input bằng Jakarta `@Valid`.
- **Frontend:** chỉ `<script setup>`; state chia sẻ → Pinia (cấm prop drilling > 2 cấp); cấm mutate props (`emit`/`v-model`); dọn `setInterval`/listener trong `onUnmounted`; input search bọc **debounce 300–500ms**.
- **UI 4 trạng thái:** mọi màn hình phải xử lý **Loading (skeleton) · Empty · Error (toast) · Success**. Thao tác nguy hiểm (xóa/hủy) phải có dialog Confirm. Responsive bắt buộc (`w-full md:.. lg:..`).
- **SRP:** file không nên > 300 dòng; không tự xóa/refactor code đang chạy khi chưa được duyệt.

### 3.4 Chuẩn API response (định hướng trong RULES — lưu ý code hiện tại CHƯA wrap đồng nhất)
```json
{ "status": 200, "message": "Thành công", "data": { ... }, "timestamp": "..." }
```
Phân trang: `data: { content, page, size, totalElements, totalPages }`.
> ⚠️ **Khoảng cách thực tế:** nhiều controller hiện trả `Map`/entity trực tiếp và dùng `@CrossOrigin(origins="*")` thay vì lớp `ApiResponse<T>` thống nhất + `@ControllerAdvice`. Đây là một hạng mục nợ kỹ thuật cần chuẩn hóa dần.

### 3.5 File "bất khả xâm phạm" (cẩn trọng + báo cáo khi chạm vào)
`config/SecurityConfig.java` · `util/JwtUtil.java` · `service/WalletService.java` · `service/BookingService.java` · `service/PricingService.java` · `pom.xml`/`package.json` · `entity/*.java` (đổi schema).

---

## 4. Tiến độ hiện tại (~92% tổng thể)

### 4.1 ĐÃ hoàn thiện (kết nối FE↔BE↔DB thật)
**Khách hàng:** Đăng ký/Đăng nhập (JWT), Trang chủ & danh sách phim, Lịch chiếu, Hệ thống rạp, Chi tiết phim + **đánh giá sao/bình luận**, Đặt vé (chọn ghế + F&B + voucher, hold ghế 10', tính giá server), Thanh toán VNPAY + Ví + tích điểm/nâng hạng, Hồ sơ cá nhân (xem+sửa), Lịch sử đặt vé, Đổi mật khẩu, Ví điện tử, **Voucher của tôi**, **Tìm kiếm phim (debounce)**, **Khuyến mãi (Promotion thật)**, **Thông báo (badge + đánh dấu đã đọc)**.

**Quản trị:** Đăng nhập admin (JWT + chặn role), Dashboard, Quản lý phim (CRUD + Cloudinary), Danh mục phim, Banner, Điều phối lịch chiếu (Master Scheduling), Quản lý rạp/phòng, Sơ đồ ghế, Nhân sự & ca trực, POS bán vé, Check-in QR, Khuyến mãi & phát voucher, Định giá, Kho F&B, CSKH, Cài đặt, **Nhật ký hệ thống (ghi thật)**, **Phân quyền chi tiết (@PreAuthorize + ma trận DB)**.

### 4.2 Hạ tầng nghiệp vụ mới bổ sung
- **Audit Log tự động:** `AuditLogInterceptor` ghi mọi thao tác ghi-dữ-liệu của ADMIN/STAFF + LOGIN. (Dùng HandlerInterceptor thay vì AOP vì pom là file bảo vệ, không thêm lib.)
- **Phân quyền:** `@EnableMethodSecurity` + `@PreAuthorize("@perm.can('feature','action')")` (bean `PermissionService`), ma trận JSON ở `Role.permissionsMatrix`, quản lý qua `/api/admin/roles`.
- **Tự trừ kho theo định mức (BOM):** `InventoryService.deductForSale` chạy trong `BookingService.completePayment`; quản lý định mức qua `/api/bom`.
- **Bàn giao ca:** `/api/staff/handovers` (tạo + liệt kê, tự tính chênh lệch tiền quỹ).
- **Lưu ý Spng Boot 4:** Jackson 3 → import `tools.jackson.*` (không phải `com.fasterxml.jackson.*`).

### 4.3 CHƯA hoàn thiện (còn lại)
1. **Màn admin UI cho BOM (định mức) & Bàn giao ca** — backend API đã sẵn, chưa có giao diện riêng.
2. **Chuẩn hóa `ApiResponse<T>` + `@ControllerAdvice`** đồng nhất toàn backend (nợ kỹ thuật ở 3.4).
3. **Mở rộng:** đăng nhập Google (OAuth), thêm cổng thanh toán (Momo/ZaloPay).

### 4.3 Tài liệu liên quan
- `RULES.md` — quy tắc bắt buộc.
- `docs/` — `ARCHITECTURE.md`, `DATABASE.md`, `API_CONTRACTS.md`, `CRITICAL_PATHS.md`, `SECURITY.md`.
- `Bao_cao_thong_ke_DevCine.docx` — báo cáo thống kê & phân tích tiến độ (sinh bởi `generate_report.py`).
