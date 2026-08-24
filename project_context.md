# DevCine — Project Context

> File ngữ cảnh tổng hợp để đính kèm vào đầu mỗi phiên chat mới, giúp AI nắm nhanh dự án mà không phải đọc lại toàn bộ lịch sử hội thoại.
> Cập nhật lần cuối: **24/08/2026** (một số mục 4.x đã cũ — **nguồn trạng thái chuẩn nhất là `CLAUDE.md` mục "Tiến độ hiện tại"** + memory `devcine-progress` / `devcine-permission-architecture`).

> **⚠️ Thay đổi kiến trúc lớn (cập nhật sau 24/06):**
> - **Đã GỠ HOÀN TOÀN Kho/Định mức BOM** (11/07): tồn kho VÔ HẠN, chỉ bán/hiển thị F&B.
> - **Đã GỠ HOÀN TOÀN phân hệ Ca làm việc & Bàn giao ca** (01/08): không còn `Shift`/`StaffSchedule`/`ShiftHandover`/`WorkPosition`/check-in ca/đổi ghế. POS bán vé + Check-in QR chạy **RBAC thuần** (`@perm.can('pos_ticketing',...)`) — nhân viên đăng nhập là bán/soát được ngay.
> - **Strict Cinema Scoping** (`SecurityUtils.assertCinemaAccess`): nhân viên chỉ thao tác trên cụm rạp của mình; bán/soát chéo rạp → **403**. Đơn POS lưu vết `sold_by` (Staff); đơn F&B thuần lưu thêm `cinema_id`.
> - **Xử lý sự cố / Đổi ghế đền bù** (14/08): phân hệ mới `/admin/incidents` (feature quyền `incident_handling`). Đổi ghế = repoint `BookingSeat` tại chỗ (giữ QR); đền bù bằng Voucher template `COMP_*` (KHÔNG hoàn tiền); khóa ghế hỏng = `Seat.seat_status=MAINTENANCE`. Ghi vết bảng `seat_incidents`. Chi tiết ở `CLAUDE.md` + memory `devcine-incident-feature`.
> - **Nghiệp vụ Xuất chiếu sớm (Early Screening)** (23/08): Tự động phát hiện khi `Showtime.startTime < Movie.releaseDate` $\rightarrow$ `Showtime.status = 'Xuất chiếu sớm'`. Toàn bộ 9 JPA query public (`ShowtimeRepository`) được mở rộng điều kiện `(m.status = 'active' OR (m.status = 'upcoming' AND s.status = 'Xuất chiếu sớm'))` để phim sắp chiếu vẫn mở bán vé bình thường. Giao diện đặt vé chạy tự nhiên, đồng nhất với phim thường (chuẩn Lotte Cinema).
> - **Fix Voucher — Case 1 & 2** (24/08): **(Case 1)** Ẩn hoàn toàn voucher hết lượt/sai đối tượng/sai phim khỏi màn đặt vé và profile: `VoucherService.shouldHideFromUI()` + field `hideFromUI` trong preview API; `VoucherController` gán status `EXHAUSTED` (thay vì `ACTIVE` sai) cho voucher hết lượt toàn hệ thống; `BookingView.vue` dùng computed `visibleVouchers`; `VouchersView.vue` đưa EXHAUSTED vào tab lịch sử badge amber; thêm skeleton `animate-pulse` + cờ `isVoucherEvalsReady` chống flash of content. **(Case 2)** Sửa race condition vượt `usageLimit`: `@Modifying(clearAutomatically=true)` tránh đọc `usedCount` stale từ persistence context cache; `createBooking()` load Promotion FRESH từ DB và chặn ngay khi hết lượt; `completePayment()` đảo thứ tự — atomic increment TRƯỚC, đánh dấu `isUsed=true` SAU (fail-fast, bỏ save-then-rollback).

---

## 1. Mục tiêu dự án

**DevCine** là hệ thống website **quản lý rạp chiếu phim & đặt vé xem phim trực tuyến** (fullstack, đồ án tốt nghiệp), phục vụ 2 nhóm người dùng:

- **Khách hàng:** duyệt phim → xem lịch chiếu → chọn ghế + combo F&B → áp voucher → thanh toán (VNPAY) → nhận vé QR → xem lịch sử/hồ sơ.
- **Quản trị/Nhân viên:** quản lý phim, suất chiếu, rạp/phòng/ghế, bán vé tại quầy (POS), check-in vé bằng QR, thực đơn F&B, nhân sự, khuyến mãi, định giá, dashboard, CSKH. *(POS/Check-in theo RBAC + Cinema Scoping — không còn phụ thuộc ca làm việc.)*

**Tính năng cốt lõi:** Đặt vé online, thanh toán VNPAY, sinh & quét vé QR, loyalty points + hạng thành viên, quản trị vận hành rạp toàn diện.

---

## 2. Tech Stack

| Lớp | Công nghệ |
|---|---|
| **Backend** | Java 25 (LTS) · Spring Boot 4.0.6 · Spring Security · Spring Data JPA · Hibernate 7 · Lombok · Maven |
| **Frontend** | Vue.js 3.5 (`<script setup>` / Composition API) · Vite 8 · Pinia 3 · Vue Router 5 · TailwindCSS 4 · Axios |
| **Database** | PostgreSQL 15+ (hosted trên Supabase, region AWS Singapore) |
| **Auth** | JWT (JJWT 0.12.6, HMAC) + BCryptPasswordEncoder, stateless |
| **Thanh toán** | Cổng VNPAY (khách) + CASH/CARD/TRANSFER tại quầy (POS). _Ví điện tử nội bộ đã gỡ bỏ 21/06/2026._ |
| **Lưu ảnh** | Cloudinary (poster, banner) |
| **Cổng/Port** | Backend `:8080` · Frontend `:5173` |

**Tài khoản seed sẵn (DataSeeder):** admin `admin / 123` · khách demo `khachhang / Khach@123`.

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
Quy mô hiện tại: **31 controller · 21 service · 37 entity** (BE) · **22 view khách + 21 view admin** (FE).

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
`config/SecurityConfig.java` · `util/JwtUtil.java` · `service/BookingService.java` · `service/PricingService.java` · `pom.xml`/`package.json` · `entity/*.java` (đổi schema). _(WalletService đã bị gỡ khỏi dự án.)_

---

## 4. Tiến độ hiện tại (~94% tổng thể)

### 4.1 ĐÃ hoàn thiện (kết nối FE↔BE↔DB thật)
**Khách hàng:** Đăng ký/Đăng nhập (JWT), Trang chủ & danh sách phim, Lịch chiếu, Hệ thống rạp, Chi tiết phim + **đánh giá sao/bình luận**, Đặt vé (chọn ghế + F&B + voucher, hold ghế 10', tính giá server), Thanh toán VNPAY + tích điểm/nâng hạng, Hồ sơ cá nhân (xem+sửa), Lịch sử đặt vé, Đổi mật khẩu, **Voucher của tôi** (ẩn đúng voucher hết lượt/hết hạn, skeleton loading, status EXHAUSTED), **Tìm kiếm phim (debounce)**, **Khuyến mãi (Promotion thật)**, **Thông báo (badge + đánh dấu đã đọc)**.

**Quản trị:** Đăng nhập admin (JWT + chặn role), Dashboard, Quản lý phim (CRUD + Cloudinary), Danh mục phim, Banner, Điều phối lịch chiếu (Master Scheduling), Quản lý rạp/phòng, Sơ đồ ghế, Nhân sự, POS bán vé, Check-in QR, Khuyến mãi & phát voucher, Định giá, Thực đơn F&B, CSKH, Cài đặt, **Nhật ký hệ thống (ghi thật)**, **Phân quyền chi tiết (@PreAuthorize + ma trận DB)**, **Phê duyệt hủy hóa đơn F&B (FnB Void)**.

### 4.2 Hạ tầng nghiệp vụ mới bổ sung
- **Audit Log tự động:** `AuditLogInterceptor` ghi mọi thao tác ghi-dữ-liệu của ADMIN/STAFF + LOGIN. (Dùng HandlerInterceptor thay vì AOP vì pom là file bảo vệ, không thêm lib.)
- **Phân quyền:** `@EnableMethodSecurity` + `@PreAuthorize("@perm.can('feature','action')")` (bean `PermissionService`), ma trận JSON ở `Role.permissionsMatrix`, quản lý qua `/api/admin/roles`.
- ~~Tự trừ kho theo định mức (BOM)~~ — **ĐÃ GỠ 11/07/2026** (tồn kho vô hạn).
- ~~Bàn giao ca `/api/staff/handovers`~~ — **ĐÃ GỠ 01/08/2026** cùng toàn bộ phân hệ Ca làm việc.
- **Lưu ý Spng Boot 4:** Jackson 3 → import `tools.jackson.*` (không phải `com.fasterxml.jackson.*`).
- **Bảo mật cấu hình:** secret VNPAY (`tmnCode`/`hashSecret`) tách khỏi `application.properties` sang env `${VNPAY_TMN_CODE}`/`${VNPAY_HASH_SECRET}` (`.env` gitignore). ⚠️ Secret cũ đã lộ trong git history → cần rotate trên VNPAY.

### 4.2.1 Hoàn thiện đợt 19/06/2026 (FE↔BE↔DB thật)
- **POS Bán vé (TicketingPOS):** rewrite hoàn chỉnh — dùng `ticketingApi` (axios có token), ghế thật từ `/api/seats/showtime/{id}`, combo thật từ `/api/fnbs`, tra cứu thành viên để tích điểm, CASH/CARD, tái dùng `BookingService.holdSeats`+`completePayment` (tạo booking CONFIRMED, trừ kho BOM). Đã verify 1 đơn thật.
- **Thực đơn F&B/Combo:** `FnbItem` thêm `imageUrl/description/isActive`; `FnbController` CRUD đầy đủ (`/api/fnbs` công khai active, ghi bảo vệ bằng method security `fnb_menu`, tên cũ `pos_inventory` đổi 21/07/2026). Admin UI mới `FnbMenuManager.vue` (`/admin/fnb`). Booking thêm bước chọn combo có ảnh/mô tả.
- **Voucher & Loyalty:** "Voucher của tôi" (`VouchersView`) nối API thật + tab "Đổi điểm lấy ưu đãi" (`VoucherService.redeemWithPoints`); nhập/tra cứu mã (`/api/vouchers/lookup` + `/claim`); áp voucher ở checkout (`/api/vouchers/apply`); sửa bug giảm-giá-2-lần ở VNPAY (tách `finalPrice` khỏi `totalPrice`). Phân tách voucher công khai (đổi điểm) vs mã bí mật (tự nhập).
- **Admin Khuyến mãi (`AdminPromotions`):** nối API thật tab Voucher (CRUD Promotion + phát voucher cho khách). Thêm cột `Promotion.allowPointRedemption`.
- **Quản lý khách hàng (mới):** `AdminCustomers.vue` (`/admin/customers`) + `GET /api/customers` (JOIN FETCH, hiển thị hạng/điểm).
- **Vé QR trong Lịch sử đặt vé:** modal render QR từ `qrCodes` BE trả; sửa N+1 trong `getBookingHistory`.
- **Seed dữ liệu thật:** 6 phim curated + lịch 3 ngày × 3 phòng × 5 suất = 45 suất; sửa giá ghế NORMAL 110k/VIP 150k/SWEETBOX 300k (cờ `DEMO_SCHEDULE_SEEDED` chạy 1 lần).

### 4.2.2 Đợt 21→24/06/2026 (Pricing, hợp nhất format, trailer, đặt vé nâng cấp)
- **Pricing Engine cấu hình được:** `PricingService` là nguồn giá DUY NHẤT — `giá = giá_nền(ngày × khung-giờ × đối tượng) + phụ_thu_ghế + phụ_thu_định_dạng`; phòng/định dạng đặc biệt → giá cố định ghi đè. `SeatType.priceModifier` đổi nghĩa thành PHỤ THU. Entity mới `Holiday`, `SpecialSeatPrice`, `PricingRule` mở rộng. Đối tượng rút còn ADULT/STUDENT. `AdminPricing.vue` 5 tab (giá nền, loại ghế, định dạng, ngày lễ, tính thử).
- **Hợp nhất 2 bảng định dạng:** gộp `Format`→`MovieFormat` (xoá entity `Format`); Danh mục phim + Cấu hình giá + Suất chiếu dùng chung một danh sách.
- **Trailer Modal tái dùng** (`TrailerModal.vue`): hero trang chủ + chi tiết phim, seed trailer chính thức. Chi tiết phim: synopsis thu/mở + lọc theo cụm rạp.
- **Đặt vé nâng cấp:** cấu hình thời gian giữ ghế / giới hạn số vé / thời gian mở bán; điều kiện voucher nâng cao + chống thanh toán trùng; yêu cầu đăng nhập khi rời bước chọn ghế (modal nhắc); chọn số lượng vé trước khi chọn ghế.
- **Dashboard real-data** (filter khoảng thời gian, widget thật); **Admin Movies** (bảng dày, bulk action, thống kê/phim); **Admin Bookings** (loại đối tượng vé + tiến độ check-in).
- **Tài liệu BA mới:** `docs/nghiep-vu/Tai_lieu_BA_DevCine.docx` (phân tích nghiệp vụ).

### 4.2.3 ĐANG LÀM DỞ (chưa commit, nằm trong working tree)
- **Hệ thống Toast + Friendly Error** (`AppToast.vue`, `stores/toast.js`, `utils/friendlyError.js`): hạ tầng đã có nhưng **mới áp 5/43 view** (Vouchers, KhuyenMai, Contact, BookingSuccess, Booking + CustomerLayout). Cần rollout ra toàn bộ view admin và các view khách còn lại để đạt chuẩn "4 trạng thái" của RULES.

### 4.3 CHƯA hoàn thiện (còn lại)
1. ~~Màn admin UI cho BOM & Bàn giao ca~~ — **KHÔNG CÒN áp dụng:** cả hai phân hệ (Kho/BOM và Ca làm việc/Bàn giao ca) đã bị **gỡ hoàn toàn** khỏi backend lẫn frontend.
2. **Chuẩn hóa `ApiResponse<T>` + `@ControllerAdvice`** — **xác nhận (24/06): KHÔNG tồn tại** lớp `ApiResponse` hay `@ControllerAdvice`/`@RestControllerAdvice` nào trong backend; nhiều controller vẫn trả `Map`/entity trực tiếp + `@CrossOrigin(origins="*")`. Đây là nợ kỹ thuật lớn nhất so với chuẩn RULES 1.2 (xem 3.4).
3. **Hoàn tất rollout Toast** (xem 4.2.3).
4. **Mở rộng:** đăng nhập Google (OAuth), thêm cổng thanh toán (Momo/ZaloPay).

### 4.4 Nợ kỹ thuật & rủi ro cần xử lý
- **Secret VNPAY đã lộ trong git history** → cần rotate `tmnCode`/`hashSecret` trên cổng VNPAY.
- **3 lỗ hổng Dependabot** trên repo GitHub (2 high, 1 moderate).
- File mẫu còn sót: `StyleGuideView.vue`, `components/common/HelloWorld.vue` — nên dọn.

### 4.5 Tài liệu liên quan
- `RULES.md` — quy tắc bắt buộc.
- `docs/` — `ARCHITECTURE.md`, `DATABASE.md`, `API_CONTRACTS.md`, `CRITICAL_PATHS.md`, `SECURITY.md`.
- `docs/bao-cao/Bao_cao_thong_ke_DevCine.docx` — báo cáo thống kê & phân tích tiến độ (sinh bởi `generate_report.py`).

## L?ch s? Refactor (02/08/2026) - QUY CHU?N M�N H�NH POS B�N V�
- **B? c?c Card Phim**: Tu�n th? "GOM NH�M 2 C?P". C?p 1: Phim. C?p 2: �?nh d?ng & Ph�ng chi?u (VD: 2D PH? �? � PH�NG 223). B?T BU?C t? d?ng chu?n h�a ti?n t? "PH�NG" n?u API tr? v? s?.
- **X? l� D? li?u**: st.movie l� String, st.duration l� s? (g�n tr?c ti?p). KH�NG G?I API /api/admin/movies trong m�n POS tr�nh 404 l�m s?p trang. T? d?ng fallback d? li?u.
- **M�i gi? & Chu?i Ng�y**: Lu�n d�ng helper parseToDate(st) (thay kho?ng tr?ng b?ng 'T'). So s�nh ng�y b?ng getLocalYmd() (chu?n m�i gi? d?a phuong), TUY?T �?I KH�NG d�ng .toISOString().
- **UI/UX Dropdown**: Gi? n?n trong su?t g-transparent, hover d?i x�m nh? hover:bg-white/10, ch? 	ext-amber-400. Kh�ng d�ng g-amber-500/10. N�t gi? chi?u d?ng Pill, tang d?n.
- **Strict Isolation**: Ch? s?a d�ng file ch? d?nh, kh�ng t? � s?a helper/API chung. �?m b?o ite build xanh 100%.
