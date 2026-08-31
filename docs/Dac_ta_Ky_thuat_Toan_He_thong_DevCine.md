# ĐẶC TẢ & PHÂN TÍCH CHI TIẾT KỸ THUẬT TOÀN HỆ THỐNG — DEVCINE

> Hệ thống Website Quản lý Cụm rạp Chiếu phim & Đặt vé Online (Đồ án tốt nghiệp)
> Tài liệu sinh từ phân tích trực tiếp mã nguồn Backend (Spring Boot) và Frontend (Vue 3).
> Phạm vi: Kiến trúc · CSDL & Entity · 7 phân hệ nghiệp vụ · API Endpoints · Ma trận phân quyền.
> Ngày lập: 01/08/2026.

---

## MỤC LỤC

1. [Tổng quan Kiến trúc & Tech Stack](#phần-1--tổng-quan-kiến-trúc--tech-stack-toàn-hệ-thống)
2. [Cấu trúc Cơ sở Dữ liệu & Entity Specs](#phần-2--cấu-trúc-cơ-sở-dữ-liệu--entity-specs)
3. [Phân tích chi tiết các phân hệ nghiệp vụ](#phần-3--phân-tích-chi-tiết-các-phân-hệ-nghiệp-vụ)
4. [Đặc tả Hệ thống API Endpoints](#phần-4--đặc-tả-hệ-thống-api-endpoints)
5. [Bảng Phân quyền Thao tác Toàn Hệ Thống](#phần-5--bảng-phân-quyền-thao-tác-toàn-hệ-thống)

---

## PHẦN 1 — TỔNG QUAN KIẾN TRÚC & TECH STACK TOÀN HỆ THỐNG

### 1.1. Mô hình kiến trúc tổng thể

DevCine là ứng dụng **fullstack tách rời (decoupled SPA + REST API)**:

```
┌─────────────────────────┐        HTTPS / JSON (REST)         ┌──────────────────────────────┐
│  FRONTEND (SPA)          │  ────────────────────────────►    │  BACKEND (REST API)          │
│  Vue 3.5 + Vite 8        │  ◄────────────────────────────    │  Spring Boot 4.0.6           │
│  Pinia · Vue Router      │                                    │  Layered Architecture        │
│  TailwindCSS 4 · Axios   │        WebSocket / STOMP           │  (Controller→Service→Repo)   │
│  cổng :5173              │  ◄──────  /ws (khóa ghế)  ──────►  │  cổng :8080                  │
└─────────────────────────┘                                    └───────────────┬──────────────┘
         │                                                                       │ JPA / Hibernate 7
         │ Cloudinary (ảnh)                                                      ▼
         │ VNPAY (thanh toán khách)                              ┌──────────────────────────────┐
         ▼                                                       │  PostgreSQL (Supabase)       │
   Trình duyệt khách / Admin                                     │  region AWS Singapore        │
                                                                 └──────────────────────────────┘
```

Backend tuân thủ **kiến trúc phân lớp nghiêm ngặt** (RULES.md):
- **Controller** — chỉ điều phối: nhận request, gọi Service, trả `ResponseEntity<ApiResponse<T>>`.
- **Service** — chứa 100% business logic, gắn `@Transactional` (ghi) / `@Transactional(readOnly=true)` (đọc).
- **Repository** — Spring Data JPA, mọi quan hệ `LAZY`, chống N+1 bằng `@EntityGraph`/`JOIN FETCH`.
- **DTO** — không bao giờ trả entity thô ra client (ẩn `passwordHash`).

Quy mô: **40 entity · 33 controller · 30 service** (BE) · **22 view khách + 21 view admin** (FE).

### 1.2. Backend Stack

| Thành phần | Công nghệ | Ghi chú kỹ thuật |
|---|---|---|
| Ngôn ngữ | **Java 21** (`<java.version>21</java.version>` trong `pom.xml`) | — |
| Framework | **Spring Boot 4.0.6** (`spring-boot-starter-parent`) | — |
| Web | `spring-boot-starter-web` (Spring MVC) | REST controllers |
| Security | `spring-boot-starter-security` + `@EnableMethodSecurity` | JWT stateless, `@PreAuthorize` |
| ORM | `spring-boot-starter-data-jpa` + **Hibernate 7** | `ddl-auto=update` |
| Validation | `spring-boot-starter-validation` (Jakarta `@Valid`) | — |
| Realtime | `spring-boot-starter-websocket` (STOMP) | Khóa ghế real-time |
| Email | `spring-boot-starter-mail` | Gửi vé QR / email chiến dịch |
| JWT | **JJWT 0.12.6** (`jjwt-api/impl/jackson`), HMAC | — |
| Mã hoá mật khẩu | `BCryptPasswordEncoder` | — |
| DB Driver | `org.postgresql:postgresql` | PostgreSQL |
| Ảnh | `cloudinary-http44:1.36.0` | Upload poster/banner |
| Cấu hình bí mật | `spring-dotenv 4.0.0` (`me.paulschwarz`) | Đọc `.env` (secret VNPAY) |
| Boilerplate | **Lombok** (`@Getter/@Setter/@Builder/@RequiredArgsConstructor`) | — |
| Build | **Maven** (`spring-boot-maven-plugin`) | — |
| JSON | **Jackson 3** — import `tools.jackson.*` (KHÔNG phải `com.fasterxml.jackson.*`) | Gotcha Spring Boot 4 |

### 1.3. Frontend Stack

| Thành phần | Công nghệ (từ `package.json`) | Ghi chú |
|---|---|---|
| Framework | **Vue 3.5.32** — 100% `<script setup>` (Composition API) | Cấm Options API |
| Build tool | **Vite 8.0.10** + `@vitejs/plugin-vue 6` | Dev server :5173 |
| State | **Pinia 3.0.4** | State chia sẻ (auth, toast, notification) |
| Routing | **Vue Router 4.6.4** | Tách `routers/customer.js` & `routers/admin.js` |
| CSS | **TailwindCSS 4.2.4** (+ `@tailwindcss/vite`, `@tailwindcss/postcss`) | Utility-first, responsive |
| HTTP | **Axios 1.16.0** | Interceptor bơm JWT + tự bóc `ApiResponse` |
| Realtime | **@stomp/stompjs 7.3.0** | Client khóa ghế WebSocket |
| Tiện ích | `fast-average-color 9.5.2` | Trích màu chủ đạo từ poster |

### 1.4. Hạ tầng chéo (Cross-cutting)

- **Auth:** JWT HMAC stateless. Token mang `userId` (principal), `username`, `role`, `cinemaId` (scoping theo cơ sở). `JwtFilter` đặt trước `UsernamePasswordAuthenticationFilter`.
- **CORS:** origin cụ thể (không dùng `*` vì `allowCredentials=true`), cấu hình qua `app.cors.allowed-origins`.
- **Database:** PostgreSQL hosted Supabase. Khóa chính chủ yếu `IDENTITY` (auto-increment); `Customer`/`Staff` dùng **`@MapsId`** chia sẻ khóa với `User`.
- **Thanh toán:** VNPAY (khách online, convention `{code,message,data}`); CASH/CARD/TRANSFER (POS tại quầy).
- **Response envelope:** `ApiResponse<T>` bọc toàn bộ controller (ngoại lệ cố ý: `PaymentController` giữ convention VNPAY). Interceptor FE tự bóc `.data`.
- **Audit:** `AuditLogInterceptor` (HandlerInterceptor) ghi mọi thao tác ghi-dữ-liệu của ADMIN/STAFF/MANAGER + LOGIN.

---

## PHẦN 2 — CẤU TRÚC CƠ SỞ DỮ LIỆU & ENTITY SPECS

> Quy ước: Entity PascalCase số ít · bảng snake_case số nhiều · cột snake_case. Mọi `@ManyToOne`/`@OneToOne` đều `FetchType.LAZY`.

### 2.1. Sơ đồ quan hệ tổng thể (ERD rút gọn)

```
Cinema 1───N Room 1───N Seat N───1 SeatType
   │             │                    
   │ manager     │ N (format_id)      
   ▼             ▼                    
 Staff        Showtime N───1 Movie N───M Category (movie_genre_mapping)
   │  ▲          │        N───1 MovieFormat
   │  │ sold_by  ▼
   │  └───────Booking ───1 Customer ───1 User ───1 Role
   │          │  │  │              │
   │          │  │  └─N BookingFnb N──1 FnbItem
   │          │  └───N BookingSeat 1──1 Ticket
   │          └─────N (voucher_id) Voucher N──1 Promotion
   │                               Customer 1──N PointTransaction
   └─N ConcessionSale (sold_by, cinema_id)   User 1──N UserPermissionOverride
PricingRule · Holiday · SystemSetting · ApprovalRequest · Notification · Review · AuditLog · SupportTicket · Banner · PromoArticle
```

### 2.2. Nhóm Rạp – Phòng – Ghế

#### `Cinema` → bảng `cinemas`
| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | Integer | PK, IDENTITY | |
| `name` | String | NOT NULL | |
| `address` | String(500) | | |
| `city`, `district` | String(100) | | Lọc theo tỉnh/thành |
| `type` | String(50) | | |
| `hotline` | String(20) | | |
| `rooms` | Integer | | Số phòng (hiển thị) |
| `image_url` | String(500) | | |
| `description` | TEXT | | |
| `latitude`, `longitude` | Double | | Bản đồ |
| `amenities` | TEXT | | Chuỗi CSV tiện ích |
| `status` | String(20) | | ACTIVE / MAINTENANCE / CLOSED |
| `opening_time` | LocalTime | | Mặc định 08:00 |
| `closing_time` | LocalTime | | Mặc định 23:30; nếu ≤ opening → đóng cửa rạng sáng hôm sau |
| `manager_id` | FK → Staff | `@ManyToOne LAZY` | Quản lý cơ sở |

#### `Room` → bảng `rooms`
| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | Integer | PK |
| `cinema_id` | FK → Cinema | `@ManyToOne LAZY`, NOT NULL |
| `name` | String(50) | NOT NULL |
| `type` | String(30) | Free-text, gom về STANDARD/SUPERPLEX/CINE_COMFORT khi tính giá |
| `status` | String(20) | |
| `turnaround_time_mins` | Integer | **Nguồn DUY NHẤT** thời gian dọn dẹp (mặc định 15) |
| `matrix_row`, `matrix_col` | Integer | Kích thước lưới sơ đồ ghế |

#### `Seat` → bảng `seats`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| `id` | Integer | PK |
| `room_id` | FK → Room | NOT NULL |
| `row_char` | String(2) | NOT NULL — hàng (A..Z) |
| `col_num` | Integer | NOT NULL — cột |
| `seat_type_id` | FK → SeatType | NOT NULL |
| `is_active` | Boolean | Xóa mềm khi lưu layout (mặc định true) |
| `label` | String(10) | Nhãn hiển thị (Admin sửa tay được) |
| `custom_label` | Boolean | true = Admin gõ tay (không bị auto-số ghi đè) |
| `seat_status` | String(20) | Trạng thái **VẬT LÝ**: AVAILABLE / MAINTENANCE / LOCKED |
| `grid_row`, `grid_col` | Integer | Toạ độ trên lưới (0-based) |

> Phương thức `displayLabel()` = ưu tiên `label`, fallback `rowChar+colNum` — nhãn chuẩn dùng cho vé/email/POS.
> **Lưu ý:** trạng thái runtime SOLD/HOLD KHÔNG lưu ở `seats` mà suy ra từ `BookingSeat`.

#### `SeatType` → bảng `seat_types`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| `id` | Integer | PK |
| `name` | String(50) | UNIQUE (NORMAL/VIP/SWEETBOX) |
| `color_code` | String(10) | Màu hiển thị |

> **Flat pricing:** loại ghế KHÔNG còn phụ thu — chỉ mang ý nghĩa hiển thị (tên + màu).

### 2.3. Nhóm Phim – Định dạng – Lịch chiếu

#### `Movie` → bảng `movies`
Trường chính: `id` (PK), `title` (NOT NULL), `slug` (UNIQUE), `duration_mins`, `age_rating`, `release_date`, `end_date`, `status`, `country`, `rating`, `rating_count`, `poster_base64`/`banner_base64` (TEXT, ánh xạ `posterUrl`/`bannerUrl`), `show_on_banner`, `trailer_url`, `format`/`supported_formats`, `title_vietnamese`, `production_year`, `language`/`original_language`, `version_type`, `description` (TEXT), `director`, `cast_members` (TEXT), `distributor`, `internal_notes`, `start_date`.
Quan hệ: `@ManyToMany` với `Category` qua bảng nối **`movie_genre_mapping`** (`movie_id`, `category_id`).

#### `MovieFormat` → bảng `movie_formats`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| `id` | Integer | PK |
| `name` | String(50) | UNIQUE (2D/3D/…) |
| `description` | String(255) | |
| `surcharge` | Decimal(15,2) | Phụ thu CÔNG NGHỆ ngày thường (2D +0, 3D +30k) |
| `weekend_surcharge` | Decimal(15,2) | Phụ thu cuối tuần/lễ (null → dùng `surcharge`) |

#### `Showtime` → bảng `showtimes`
| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | Integer | PK |
| `movie_id` | FK → Movie | NOT NULL |
| `room_id` | FK → Room | NOT NULL |
| `format_id` | FK → MovieFormat | NOT NULL |
| `start_time` | LocalDateTime | NOT NULL |
| `end_time` | LocalDateTime | NOT NULL — tính = start + duration + turnaround |
| `status` | String(20) | "Sắp chiếu" / CANCELLED… |

#### `Category` → `categories`: `id`, `name` (UNIQUE 100), `description` (500). Thể loại phim.

### 2.4. Nhóm Đặt vé – Vé

#### `Booking` → bảng `bookings`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| `id` | Integer | PK |
| `customer_id` | FK → Customer | Nullable (khách vãng lai POS) |
| `showtime_id` | FK → Showtime | NOT NULL |
| `voucher_id` | FK → Voucher | Nullable |
| `sold_by` | FK → Staff | NV bán tại quầy (POS); null với đơn ONLINE |
| `total_price` | Decimal(15,2) | Giá gốc (ghế + F&B) |
| `final_price` | Decimal(15,2) | Sau trừ voucher |
| `payment_method` | String(50) | VNPAY/CASH/CARD/TRANSFER |
| `status` | String(50) | HOLD → CONFIRMED / EXPIRED / CANCELLED |
| `booking_code` | String(50) | UNIQUE (UUID 10 ký tự) |
| `channel` | String(20) | **ONLINE** \| **POS** — nguồn tin cậy tách email |
| `created_at` | LocalDateTime | NOT NULL — mốc tính hold hết hạn |
| `printed_at` | LocalDateTime | Thời điểm in vé giấy tại quầy |
| `printed_by` | FK → Staff | NV in vé |

#### `BookingSeat` → bảng `booking_seats`
`id` (PK), `booking_id` (NOT NULL), `seat_id` (NOT NULL), `price_snapshot` Decimal(15,2) — **chốt giá tại thời điểm đặt**, `ticket_type` (ADULT/U22/CHILD/SENIOR), `status` (HOLD/SOLD/EXPIRED).

#### `BookingFnb` → bảng `booking_fnbs`
`id`, `booking_id`, `fnb_item_id`, `quantity` (NOT NULL), `price_snapshot`.

#### `Ticket` → bảng `tickets`
`id`, `booking_seat_id` (**`@OneToOne`, UNIQUE, NOT NULL**), `qr_code` (String 500, định dạng `DEVCINE-T-{bsId}-{rand8}`), `is_checked_in` (default false), `is_age_verified`, `checked_in_by` (FK Staff), `check_in_time`.

#### `FnbItem` → bảng `fnb_items`
`id`, `name` (NOT NULL), `type` (COMBO/…), `price` (NOT NULL), `image_url`, `description`, `is_active`. Tồn kho **VÔ HẠN** (kho/BOM đã gỡ hoàn toàn).

### 2.5. Nhóm Người dùng – Phân quyền

#### `User` → bảng `users`
`id`, `username` (UNIQUE NOT NULL), `password_hash` (NOT NULL, BCrypt), `full_name` (NOT NULL), `avatar_url`, `email` (UNIQUE NOT NULL), `phone`, `role_id` (FK NOT NULL), `is_active` (default true), `must_change_password` (buộc đổi lần đầu), `created_at`.

#### `Customer` → bảng `customers` — **`@MapsId` chia sẻ PK với User**
`user_id` (PK = FK → User qua `@OneToOne @MapsId`), `dob`, `id_card`, `membership_tier` (BRONZE/SILVER/GOLD/PLATINUM), `loyalty_points` (ví tiêu được), `lifetime_points` (tích luỹ trọn đời — chỉ xét hạng).

#### `Staff` → bảng `staffs` — **`@MapsId` với User**
`user_id` (PK/FK), `staff_code` (UNIQUE), `cinema_id` (FK — cơ sở làm việc, dùng cho Cinema Scoping), `manager_id` (FK self), `created_at`/`updated_at` (`@PrePersist`/`@PreUpdate`).

#### `Role` → bảng `roles`
`id`, `name` (UNIQUE: ADMIN/MANAGER/STAFF/CUSTOMER), `permissions_matrix` (TEXT — JSON `{feature:[actions]}`).

#### `UserPermissionOverride` → bảng `user_permission_overrides`
`id`, `user_id` (FK NOT NULL), `feature`, `action`, `effect` (ALLOW/DENY), `updated_at`. **UNIQUE(`user_id`,`feature`,`action`)** — override quyền theo từng người.

### 2.6. Nhóm Khuyến mãi – Loyalty

#### `Promotion` → bảng `promotions` (chương trình khuyến mãi/mã gốc)
`id`, `code` (UNIQUE), `name`, `description`, `discount_type` (**PERCENTAGE**/**FIXED_AMOUNT**), `discount_value` (NOT NULL), `start_date`/`end_date`, `is_stackable`, `points_required`, `allow_point_redemption`, `min_order_value`, `applicable_movie_id`, `customer_eligibility` (ALL/NEW_CUSTOMER/TIER_SILVER/TIER_GOLD/TIER_PLATINUM), `usage_limit`, `used_count`, `max_ticket_quantity`, `max_discount_amount`, `campaign_sent_at`, `campaign_sent_count`.

#### `Voucher` → bảng `vouchers` (voucher đã phát/cá nhân hoá)
`id`, `customer_id` (FK NOT NULL), `promotion_id` (FK NOT NULL), `valid_until`, `is_used`, `used_at`.

#### `PointTransaction` → bảng `point_transactions` (sổ điểm append-only)
`id`, `customer_id` (FK NOT NULL), `points` (±), `type` (EARN/REDEEM/VOID/ADJUST), `source` (BOOKING/FNB/PROMO_REDEEM/VOID_FNB/ADMIN), `ref_code`, `balance_after`, `note` (TEXT), `created_at` (`@PrePersist`).

### 2.7. Nhóm Định giá – Vận hành

- **`PricingRule`** → `pricing_rules`: `id`, `name`, `rule_type` (BASE_PRICE), `day_type` (WEEKDAY/WEEKEND/ALL), `room_type` (STANDARD/SUPERPLEX/CINE_COMFORT/ALL), `time_slot` (@Deprecated — luôn ALL), `audience_type` (ADULT/U22/CHILD/SENIOR/ALL), `value` (Decimal), `priority`, `active`, `start_date`/`end_date`.
- **`Holiday`** → `holidays`: `id`, `holiday_date` (UNIQUE), `name`. Ngày lễ → áp `day_type=WEEKEND`.
- **`ApprovalRequest`** → `approval_requests`: yêu cầu sửa sai do Quản lý/Quản trị viên duyệt (`type` **FNB_VOID** — hủy hóa đơn F&B, `ref_id`, `status` PENDING/APPROVED/REJECTED…). *Đổi ghế (SEAT_MOVE) đã gỡ.*
- **`SystemSetting`** → cấu hình key-value: `SEAT_HOLD_MINUTES` (giữ ghế phiên trực tiếp), `POS_ORDER_HOLD_MINUTES` (lưu đơn chờ POS), `MAX_TICKETS_PER_BOOKING` (số vé tối đa), `BOOKING_LATE_MINUTES` (mở bán trễ), `LOYALTY_POINT_RATE` (mức chi tiêu đổi 1 điểm), `PAYMENT_BANK_CODE` / `PAYMENT_BANK_NAME` / `PAYMENT_ACCOUNT_NO` / `PAYMENT_ACCOUNT_NAME` (thông tin tài khoản thụ hưởng phục vụ sinh mã VietQR Napas 247 cho kênh Online & POS, áp dụng chuẩn định dạng tự động IN HOA KHÔNG DẤU, lọc số STK 4–20 ký tự và validate inline All-or-Nothing), cờ seed…
- Các entity phụ trợ: `Notification`, `Review`, `AuditLog`, `SupportTicket`, `Banner`, `PromoArticle`, `Faq`, `AgeRating`, `PromoEmailLog` (dedup email chiến dịch).
- **`ConcessionSale`** → `concession_sales`: `id`, `sale_code` (UNIQUE), `customer_id` (nullable), `sold_by` (FK → Staff), `cinema_id` (FK → Cinema — neo cơ sở bán), `total_price`, `payment_method`, `status` (CONFIRMED/VOIDED), `created_at`. Kèm `ConcessionSaleItem` (bán F&B thuần tại quầy).

> **Đã gỡ hoàn toàn phân hệ Ca làm việc:** không còn entity `Shift`, `StaffSchedule`, `ShiftHandover`; không còn cột `staff_schedule_id`, `default_position`, setting `SHIFT_OPENING_FLOAT`.

---

## PHẦN 3 — PHÂN TÍCH CHI TIẾT CÁC PHÂN HỆ NGHIỆP VỤ

### 3.a. Phân hệ Quản lý Cụm rạp & Giờ hoạt động (Cinema & Operating Hours)

**Mục đích & Phạm vi.** Quản lý danh mục cụm rạp (CRUD), thông tin hiển thị cho khách (địa chỉ, toạ độ bản đồ, tiện ích, ảnh), và đặc biệt **cửa sổ giờ hoạt động** (`opening_time`/`closing_time`) — dữ liệu nền cho Constraint Engine của lịch chiếu.

**Luồng nghiệp vụ & Liên phân hệ.**
- Public: `GET /api/v1/cinemas` phục vụ trang "Hệ thống rạp"; `ShowtimeController` dùng danh sách city để lọc lịch chiếu.
- Admin: CRUD qua `CinemaController` (`@PreAuthorize("hasRole('ADMIN')")` — cứng, chỉ ADMIN).
- **Phụ thuộc ngược:** `ShowtimeService.cinemaWindow(cinema)` đọc `opening/closing_time` để chặn suất ngoài giờ. `Room` thuộc `Cinema`; `Staff.cinema` quyết định scoping dashboard.

**Quy tắc cốt lõi.**
- Cửa sổ giờ tính bằng phút: `[openMin, closeMin]`. Nếu `closeMin ≤ openMin` ⇒ **rạp đóng cửa rạng sáng hôm sau** → `closeMin += 1440` (hỗ trợ suất khuya vắt qua nửa đêm).
- Mặc định khi null: mở 08:00, đóng 23:30.

**Xử lý ngoại lệ & Edge cases.**
- Rạp qua nửa đêm: hàm `posOf()` cộng 1440 phút cho mốc giờ nhỏ hơn giờ mở để định vị đúng trên "trục ngày vận hành".
- Xóa cụm rạp: bảo vệ ở tầng FK (Room/Showtime tham chiếu) — chỉ ADMIN thao tác.

**Refactor & Nợ kỹ thuật đã xử lý.** Bổ sung `opening_time`/`closing_time` (nullable để `ddl-auto` thêm cột vào bảng đã có dữ liệu) làm nền cho **timeline động** thay cho khung giờ cứng trước đây.

---

### 3.b. Phân hệ Quản lý Phòng chiếu & Sơ đồ ghế (Room & Seat Map chuẩn Lotte)

**Mục đích & Phạm vi.** Quản lý phòng chiếu trong cụm rạp và **trình thiết kế sơ đồ ghế trực quan** — sinh lưới ghế, đặt loại ghế, sửa nhãn tay, đánh dấu bảo trì/khóa, lưu layout.

**Luồng nghiệp vụ & Liên phân hệ.**
- `RoomController` CRUD phòng (ADMIN-cứng). `SeatController`: `GET /room/{roomId}` (preview thiết kế), `GET /showtime/{id}` (trạng thái runtime cho đặt vé), `POST /layout/{roomId}` (lưu sơ đồ).
- `SeatService.getSeatsForShowtime()` phối hợp `PricingService` (bảng giá) + `BookingSeatRepository` (SOLD/HOLD) + `SeatLockService` (khóa tạm real-time).

**Quy tắc cốt lõi & Thuật toán.**
- **Sinh mặc định** (`generateDefaultSeats`): `rows×cols` ghế loại NORMAL, `rowChar = A..Z`, `colNum = 1..cols`, dùng **`jdbcTemplate.batchUpdate`** (chèn hàng loạt, tránh N+1 insert).
- **Lưu layout** (`saveSeatLayout`): (1) cập nhật `matrix_row/col`; (2) **xóa mềm** toàn bộ ghế cũ (`deactivateByRoomId` → `is_active=false`); (3) batch-insert ghế mới. Map loại ghế FE→BE: `STANDARD→NORMAL`, `DOUBLE→SWEETBOX`.
- **Ba trạng thái ghế tách biệt:**
  - `is_active` — xóa mềm phục vụ versioning layout.
  - `seat_status` (VẬT LÝ) — AVAILABLE/MAINTENANCE/LOCKED.
  - Runtime (SOLD/HOLD) — **suy ra** từ `BookingSeat`, không lưu ở `seats`.
- **Nhãn ghế:** `custom_label=true` khi Admin gõ tay (dblclick sửa) → không bị đánh số tự động ghi đè. `displayLabel()` là nguồn nhãn chuẩn đồng bộ Email/POS/Client.

**Xử lý ngoại lệ & Edge cases.**
- Ghi đè trạng thái: khi hiển thị cho đặt vé, ghế MAINTENANCE/LOCKED **phủ đè** lên trạng thái runtime (FE disable) — dù chưa ai đặt vẫn không bán được.
- Cột `seat_status` để nullable ở DB (rows cũ = null → coi như AVAILABLE trong code) để `ddl-auto` thêm cột không lỗi.

**Refactor & Nợ kỹ thuật đã xử lý.** Đại tu sơ đồ ghế (memory `devcine-seatmap-overhaul`): tách cột label + seat_status, công cụ bảo trì, sửa nhãn tay, **dirty-check** nút Lưu (chỉ bật khi layout thay đổi), đánh số bỏ qua lối đi. Bỏ phụ thu theo loại ghế (flat pricing) → `SeatType` chỉ còn tên + màu.

---

### 3.c. Phân hệ Quản lý Phim & Định dạng chiếu (Movie & Movie Format)

**Mục đích & Phạm vi.** CRUD phim (poster/banner Cloudinary), quản lý thể loại (`Category`), độ tuổi (`AgeRating`), và **định dạng chiếu** (`MovieFormat`) — hợp nhất từ 2 bảng `Format`+`MovieFormat` cũ.

**Luồng nghiệp vụ & Liên phân hệ.**
- Public: `GET /api/movies` (đang chiếu/sắp chiếu, tìm kiếm debounce), `GET /{id}` chi tiết + đánh giá. `Movie` liên kết `Category` (`@ManyToMany`), là gốc của `Showtime`.
- Admin: CRUD (`@perm.can('movies', ...)`), bulk status/delete, thống kê/phim (`/{id}/stats`).
- `MovieFormat.surcharge`/`weekendSurcharge` là đầu vào `PricingService`.

**Quy tắc cốt lõi.**
- `slug` UNIQUE — SEO + tra cứu. `supported_formats` (CSV) liệt kê định dạng khả dụng.
- Định dạng chiếu (2D/3D) **tách khỏi hạng phòng** (STANDARD/SUPERPLEX/CINE_COMFORT): công nghệ → `MovieFormat`; phần cứng → `Room.type`.

**Xử lý ngoại lệ & Edge cases.**
- Route conflict: dùng regex `@GetMapping("/{id:\\d+}")` để tách endpoint số khỏi `/search`, `/now-showing`, `/upcoming`.
- Ảnh lưu dạng TEXT (`poster_base64`/`banner_base64`) hoặc URL Cloudinary.

**Refactor & Nợ kỹ thuật.** Hợp nhất `Format`→`MovieFormat` (xóa entity trùng); Danh mục phim + Cấu hình giá + Suất chiếu dùng chung một danh sách định dạng. Thêm trailer modal tái dùng, synopsis thu/mở.

---

### 3.d. Phân hệ Quản lý Lịch chiếu & Constraint Engine (Showtime)

**Mục đích & Phạm vi.** Tạo suất chiếu (đơn lẻ + **hàng loạt/batch**), tự tính giờ kết thúc, và một **Constraint Engine** kiểm soát chồng lịch, giờ hoạt động, thời gian dọn dẹp và suất khuya (force overtime).

**Luồng nghiệp vụ & Liên phân hệ.**
- Admin (`@perm.can('schedules', ...)`): `POST /api/showtimes` (đơn), `POST /batch` (lô), `PATCH /{id}`, `DELETE /{id}`, `GET /{id}/detail`.
- Đọc từ `Movie` (duration), `Room` (turnaround, cinema), `MovieFormat`. Xuất cho trang khách (lịch chiếu) và POS.

**Quy tắc cốt lõi & Thuật toán.**
1. **Tính giờ kết thúc (nguồn duy nhất):** `endTime = startTime + duration(phim) + turnaround(phòng)`. `turnaround` **bốc từ `Room.turnaroundTimeMins`** (mặc định 15), KHÔNG nhận từ FE.
2. **RULE A — chặn cứng:** suất bắt đầu ngoài `[openMin, closeMin]` → ném `IllegalArgumentException`.
3. **Conflict check:** `showtimeRepository.hasConflict(roomId, start, end)` — chồng lấn (đã gồm giờ dọn dẹp) → `IllegalStateException`. Khi sửa dùng `hasConflictExcluding(..., id)` để bỏ qua chính suất đang sửa.
4. **RULE B — cảnh báo + force:** nếu `endPos > closeMin` (kết thúc quá giờ đóng cửa) và `!request.isForce()` → trả `ShowtimeCreateResult{requiresConfirmation:true}` (không ghi). FE xác nhận rồi gửi lại `force=true` để tạo **suất khuya**.
5. **Batch (tích Descartes phòng × ngày × khung giờ):**
   - Chống N+1: nạp **một lần** toàn bộ suất hiện có trong cửa sổ (`findByRoomsAndWindow`) vào `busyByRoom`, kiểm tra chồng lấn **trong bộ nhớ**.
   - Lọc `daysOfWeek`, bỏ qua suất đã qua giờ (`start.isBefore(now)`), giữ chỗ ngay trong lô để các suất sau không đè nhau.
   - **All-or-nothing:** còn suất khuya chưa xác nhận ⇒ chưa ghi (`requiresConfirmation`); hỗ trợ `dryRun` (chỉ đếm, không lưu).
   - Trả về `toCreate`, `createdCount`, danh sách `skipped` (kèm lý do) và `warnings` (suất khuya).

**Xử lý ngoại lệ & Edge cases.**
- **Xóa suất có ràng buộc:** `deleteShowtime` đếm `countReservedByShowtime` (SOLD/HOLD) > 0 → **TỪ CHỐI** ("phải hoàn tiền/hủy vé trước"), tránh đơn hàng mồ côi.
- **Sửa giờ/phòng gây chồng lấn:** vá bằng `hasConflictExcluding`.
- Suất khuya vắt nửa đêm: xử lý qua trục phút `posOf()`/`fmtMin()` (chia dư 1440).

**Refactor & Nợ kỹ thuật.** Chuẩn hoá `turnaround` về nguồn duy nhất (Room), sửa các khe hở conflict/delete, gỡ code chết; bổ sung giờ hoạt động rạp + timeline động + ràng buộc theo cửa sổ (2 commit gần nhất: `feat/refactor(showtime)`).

---

### 3.e. Phân hệ Đặt vé & Bán vé (Booking & Ticketing — POS & Client)

**Mục đích & Phạm vi.** Luồng đặt vé 2 kênh: **ONLINE** (khách tự đặt, thanh toán VNPAY) và **POS** (bán tại quầy, CASH/CARD/TRANSFER). Gồm giữ ghế real-time, giữ chỗ 2 pha (hold→confirm), chốt giá server-side, sinh vé QR, gửi email.

**Luồng nghiệp vụ (hold → pay).**
```
[Chọn ghế] → SeatLockService.trySelect (khóa tạm RAM, broadcast STOMP)
     │
     ▼
[Hold] BookingService.holdSeats → booking status=HOLD, BookingSeat status=HOLD, chốt price_snapshot
     │      (ONLINE: /api/bookings/hold · POS: /api/ticketing/hold hoặc /pay)
     ▼
[Thanh toán] completePayment → status=CONFIRMED, BookingSeat→SOLD, sinh Ticket QR,
     │      tích điểm (LoyaltyService), đánh dấu voucher used, broadcast SEAT_SOLD, gửi email
     ▼
[Check-in] TicketController /lookup + /print (quét QR tại quầy)
```

**Quy tắc cốt lõi & Thuật toán.**
- **Khóa bi quan chống race:** `findByIdForUpdate(showtimeId)` (SELECT … FOR UPDATE) **tuần tự hóa** mọi lệnh giữ ghế cùng suất → chống bán trùng POS + online.
- **Giữ ghế 2 pha:** ghế đang HOLD **quá hạn** (`createdAt < now - SEAT_HOLD_MINUTES`) mới được nhả (`status=EXPIRED`); HOLD còn sống của **bất kỳ** đơn nào (kể cả cùng tài khoản) đều bị chặn.
- **Anti-fraud theo kênh:** vé **CHILD/SENIOR** cần xác minh giấy tờ ⇒ **cấm bán ONLINE** (chặn ở service dù UI đã ẩn); ONLINE chỉ ADULT/U22.
- **Chống phe vé & Khung giờ bán vé muộn (Late Booking):** `selectedSeatIds.size() > MAX_TICKETS` (SystemSetting) → từ chối. Khung giờ khởi tạo phiên mở bán đối chiếu với `sessionStartedAt` (mốc thời gian khi nhân viên/khách hàng bấm chọn suất chiếu) $\le$ `startTime + bookingLateMinutes`.
- **Bảo vệ phiên & Tách biệt 2 cấu hình giữ đơn (Dual Hold Timers):**
  - *(1) Thời gian giữ chỗ phiên đặt vé (`SEAT_HOLD_MINUTES` — 3–30 phút, mặc định 10)*: Áp dụng khi khách đặt Online hoặc thu ngân đang chọn ghế trên POS; bảo vệ toàn bộ phiên giao dịch từ `sessionStartedAt` đến thanh toán; tích hợp **Idle Guard** tự động hủy nếu treo máy quá hạn.
  - *(2) Thời gian lưu đơn chờ tại quầy POS (`POS_ORDER_HOLD_MINUTES` — 3–60 phút, mặc định 15)*: Áp dụng khi thu ngân bấm **"Giữ đơn"** tại POS để phục vụ khách tiếp theo; tự động kẹp tối đa bằng `startTime` suất chiếu; quá hạn tự hủy đơn, nhả ghế và phạt 5 phút ghế bị bỏ rơi trên POS đó.
- **Giữ chỗ trước tại POS (QR Payment Hold):** Tại quầy POS, mở modal Chuyển khoản QR sẽ lập tức gọi `holdSeats` để khóa ghế và chốt mốc thời gian tạo đơn; khi khách quét xong chỉ cần gọi hoàn tất, nếu hủy modal thì tự động nhả ghế qua `releaseHold`.
- **Chốt giá server-side:** `PricingService.buildContext(showtime)` nạp ngữ cảnh **một lần**, mỗi ghế `priceFor(ctx, ticketType)` → lưu vào `price_snapshot` (bất biến).
- **Voucher tại đặt vé:** gọi `VoucherService.evaluate(...)` (nguồn sự thật duy nhất) → `finalPrice = totalPrice − discount` (kẹp ≥ 0). Tách `finalPrice` khỏi `totalPrice` để **sửa bug giảm giá 2 lần** ở VNPAY.
- **Idempotency:** `completePayment` đơn đã CONFIRMED → return ngay (không trừ tiền/sinh vé lần 2); đơn EXPIRED/CANCELLED → ném lỗi "đặt lại".
- **Tối ưu N+1:** gom `findByIdInWithSeatType` + `saveAll` thay vì query/save từng ghế; email fetch kèm seat (`findAllByBookingIdWithSeat`).

**Xử lý ngoại lệ & Edge cases / race conditions.**
- Ghế bảo trì/khóa lọt vào giỏ: chặn tại `holdSeats` (`seatStatus != AVAILABLE`).
- Đơn chờ POS: `PosHoldService.releaseHold` nhả ghế (HOLD→EXPIRED, booking→CANCELLED) — **CONFIRMED thì tuyệt đối không nhả** (chống mất vé đã bán).
- Email lỗi: bọc try/catch, **không rollback** giao dịch đã hoàn tất (fail-safe); broadcast STOMP lỗi cũng best-effort.
- Khách vãng lai (customer=null): bỏ qua tích điểm & email.
- Auto-tạo `Customer` BRONZE khi admin/staff đặt hộ user chưa có hồ sơ (dùng `@MapsId` — set association, **persist không merge**).

**Refactor & Nợ kỹ thuật.** Bỏ quy tắc "nhả HOLD nếu cùng member" (gây 2 phiên cùng tài khoản cướp ghế) → chỉ nhả HOLD quá hạn + khóa bi quan. Tách email theo `channel` (ONLINE có QR, POS chỉ hoá đơn) thay vì suy từ `staffSchedule`. POS rewrite dùng `ticketingApi` + tái dùng `holdSeats`/`completePayment`. Tách biệt Khung giờ mở bán (`startTime + lateMinutes`) khỏi Vòng đời giữ đơn (`expiresAt`) và bảo vệ toàn bộ phiên giao dịch từ thời điểm chọn suất (`sessionStartedAt`).

---

### 3.f. Phân hệ Voucher, Khuyến mãi & Khách hàng thân thiết (Voucher & Loyalty)

**Mục đích & Phạm vi.** Quản lý chương trình khuyến mãi (`Promotion`), phát/nhận/đổi voucher (`Voucher`), điểm thưởng & hạng thành viên (`LoyaltyService`), email chiến dịch.

**Luồng nghiệp vụ & Liên phân hệ.**
- **Nhận voucher:** (1) nhập mã `claimByCode`; (2) đổi điểm `redeemWithPoints`; (3) admin phát trực tiếp (`/issue-voucher`); (4) email chiến dịch (`/send-campaign`).
- **Áp voucher:** `getOrClaimForCheckout` tại thanh toán → `BookingService` gọi `evaluate`.
- Liên kết chặt `BookingService` (áp giảm), `LoyaltyService` (điểm), `MailService` (email).

**Quy tắc cốt lõi & Thuật toán (`VoucherService.evaluate` — nguồn sự thật duy nhất).**
Thứ tự kiểm tra **cố định** (preview và checkout khớp nhau):
1. **Đơn tối thiểu:** `orderTotal < minOrderValue` → loại.
2. **Theo phim:** `applicableMovieId != movieId` → loại.
3. **Đối tượng** (`eligibilityReason`): NEW_CUSTOMER (chưa có booking CONFIRMED — `NOT EXISTS`), TIER_* (so hạng theo `lifetimePoints`).
4. **Lượt dùng:** `usedCount >= usageLimit` → loại.
5. **Tính giảm:** base = cả đơn, hoặc nếu `maxTicketQuantity>0` → chỉ **X vé đắt nhất** (`sorted reverse().limit(X)`).
   - PERCENTAGE: `base × value/100` (HALF_UP 2 chữ số).
   - FIXED_AMOUNT: `min(value, base)`.
   - Kẹp trần `maxDiscountAmount`.

**Loyalty (`LoyaltyService`).**
- **Mô hình 2 loại điểm:** `loyaltyPoints` (ví tiêu được: +mua, −đổi/void) và `lifetimePoints` (tích luỹ trọn đời: chỉ xét hạng, **không giảm khi đổi điểm** → chống tụt hạng).
- **Tỉ lệ:** 1.000đ = 1 điểm (SystemSetting `LOYALTY_POINT_RATE`, làm tròn xuống). Tích trên `finalPrice`.
- **Hạng:** BRONZE < SILVER(≥2000) < GOLD(≥5000) < PLATINUM(≥10000).
- **API:** `award` (mua thành công), `reclaim` (void — đảo cả 2 loại, hạng có thể tụt), `redeem` (đổi điểm — chỉ trừ ví). Mọi biến động ghi `PointTransaction` (append-only, `balance_after`).

**Xử lý ngoại lệ & Edge cases.**
- Đổi điểm 1 lần/khách: `existsByCustomerAndPromotion` chặn **trước khi trừ điểm**.
- Email chiến dịch **dedup**: `PromoEmailLog` — khách đã nhận không gửi lại; mã đổi-điểm không cho gửi campaign.
- Voucher: chặn đã dùng/hết hạn/không thuộc khách tại `holdSeats`; `used_at` ghi mốc; `promotion.usedCount++` khi CONFIRMED.
- Preview: `discountAmount` không vượt `orderTotal`; mã không đủ điều kiện trả `applicable=false` + `reason` (FE làm mờ).

**Refactor & Nợ kỹ thuật.** Tách voucher công khai (đổi điểm) vs mã bí mật (tự nhập); sửa bug giảm giá 2 lần VNPAY; thống nhất `evaluate` dùng chung preview/checkout/booking; thêm `PromoEmailLog` chống spam trùng người.

---

### 3.g. Phân hệ Phân quyền & Xác thực (Auth, RBAC, Permission Matrix)

**Mục đích & Phạm vi.** Xác thực JWT stateless và phân quyền 2 tầng: **Role matrix** (tĩnh, theo vai trò) + **UserPermissionOverride** (cá nhân), cộng **Cinema Scoping** (cách ly dữ liệu theo cụm rạp). *Không còn Position gating theo ca — nhân viên có quyền `pos_ticketing` là bán/soát được ngay tại cơ sở của mình.*

**Luồng xác thực (`AuthService`).**
- **Đăng ký:** chỉ tạo CUSTOMER; `username` sinh từ SĐT; tạo `Customer` BRONZE; mật khẩu BCrypt.
- **Đăng nhập:** `findByLoginIdentifier` (SĐT/email, hỗ trợ trùng SĐT lịch sử) → lọc theo `passwordEncoder.matches`. Chặn `isActive=false`. Sinh JWT mang `userId/username/role/cinemaId`. Ghi audit LOGIN cho ADMIN/MANAGER/STAFF. Trả `mustChangePassword`, `cinemaId`, `cinemaName`.
- **Đổi mật khẩu:** kiểm mật khẩu cũ → giải phóng cờ `mustChangePassword` (kích hoạt tài khoản seed).

**Phân quyền (`PermissionService`, bean `@Service("perm")`).**
- SpEL trong `@PreAuthorize("@perm.can('feature','action')")`.
- **Đánh giá:** ADMIN → luôn true (toàn quyền). Vai trò khác: nạp ma trận JSON từ `Role.permissionsMatrix` → áp `UserPermissionOverride` (DENY xóa action, ALLOW thêm) → kiểm `actions.contains(action)`.
- **Cache 2 tầng:** `cache` (role→matrix), `userCache` (userId→effective); `invalidate`/`invalidateUser` khi cập nhật. JSON parse bằng **`tools.jackson.*`** (Jackson 3).

**Cách ly cụm rạp (Strict Cinema Scoping — `SecurityUtils.assertCinemaAccess`).**
JWT mang `cinemaId`. Mọi thao tác POS/F&B/soát vé đối chiếu cơ sở của đối tượng với `cinemaId` của nhân viên:
- **Bán vé POS** (`BookingService.holdSeats` kênh POS): suy cơ sở từ `Showtime → Room → Cinema`; nhân viên Rạp A bán suất Rạp B → **403**. Danh sách suất POS cũng lọc sẵn theo cơ sở.
- **Bán F&B thuần** (`ConcessionService`): gán cố định `cinema_id = staff.getCinema()`; đơn được neo cơ sở người bán.
- **Soát vé / Check-in** (`TicketService`): suy cơ sở từ `Booking → Showtime → Cinema`; chỉ tra cứu/in vé của cơ sở mình.
- Quy tắc `assertCinemaAccess(targetCinemaId)`: **ADMIN** bỏ qua (toàn hệ thống); **STAFF/MANAGER** thiếu `cinemaId` hoặc khác cơ sở → **403** ("Bạn không có quyền thao tác trên Cụm rạp khác").

**Scoping báo cáo theo cơ sở.**
- `resolveCinemaScope()` (Dashboard) — chỉ ADMIN được `cinemaId=null` (toàn hệ thống); MANAGER/STAFF thiếu cơ sở → **fail closed** (403), không mở toàn hệ thống.

**Cấu hình bảo mật (`SecurityConfig`).**
- Stateless, CSRF off, CORS origin cụ thể (`allowCredentials=true`). `JwtFilter` trước `UsernamePasswordAuthenticationFilter`.
- **Public (permitAll):** `/ws/**`, `/api/auth/**`, `/api/movies/**`, `/api/showtimes/**`, `/api/categories/**`, `/api/formats/**`, `/api/seats/**`, `/api/fnbs/**`, GET `/api/settings`, GET `/api/reviews`, GET `/api/v1/cinemas`, GET `/api/locations`, GET `/api/faqs`, GET `/api/marketing/promotions/active`, GET `/api/promo-articles`, GET `/api/banners/active`, `/api/payment/vnpay_return`, `/api/system/**`. Còn lại `authenticated()`.

**Xử lý ngoại lệ & Edge cases.**
- Ma trận JSON lỗi → log.error + trả `Map.of()` (fail-safe, không crash).
- "Checkbox chết": V3/V4 gỡ mọi action không có endpoint `@perm.can` tương ứng.
- WebSocket handshake `permitAll` — bảo vệ ở tầng nghiệp vụ (không qua JWT filter).

**Refactor & Nợ kỹ thuật.** Đổi `pos_inventory`→`fnb_menu` (gác THỰC ĐƠN, không phải kho); ma trận V4 chỉ giữ action backend thật sự enforce; STAFF bỏ `fnb_menu`/`support:edit`; dashboard scoping fail-closed; UserPermissionOverride cấp/thu quyền theo từng người.

---

## PHẦN 4 — ĐẶC TẢ HỆ THỐNG API ENDPOINTS

> Quyền: `hasRole(X)`/`hasAnyRole(...)` = cứng theo vai trò; `@perm.can('f','a')` = theo ma trận (ADMIN luôn qua); `permitAll` = công khai; `authenticated` = chỉ cần đăng nhập.

### 4.1. Xác thực & Người dùng

| Method | Path | Payload | Quyền |
|---|---|---|---|
| POST | `/api/auth/register` | RegisterRequest | permitAll |
| POST | `/api/auth/login` | LoginRequest | permitAll |
| GET | `/api/auth/profile/{userId}` | — | authenticated |
| PUT | `/api/auth/profile` | UpdateProfileRequest | authenticated |
| PUT | `/api/auth/change-password` | ChangePasswordRequest | authenticated |
| POST | `/api/auth/forgot-password` · `/verify-otp` · `/reset-password` | — | permitAll |
| GET | `/api/customers` | — | ADMIN/MANAGER/STAFF |
| GET/PUT | `/api/customers/{id}` · `/{id}/point-history` | — | authenticated |

### 4.2. Phim · Thể loại · Định dạng · Đánh giá

| Method | Path | Quyền |
|---|---|---|
| GET | `/api/movies`, `/search`, `/now-showing`, `/upcoming`, `/{id}` | permitAll |
| GET | `/api/movies/{id}/stats` | `@perm.can('movies','edit')` |
| POST/PUT/DELETE | `/api/movies`, `/{id}`, `/bulk`, `/bulk-status` | `@perm.can('movies', add/edit/delete)` |
| GET/POST/PUT/DELETE | `/api/categories/genres` · `/formats` · `/age-ratings` | GET permitAll; ghi `@perm.can('movies', ...)` |
| GET | `/api/formats` | permitAll |
| GET | `/api/reviews/movie/{id}`, `/eligibility` | permitAll |
| POST | `/api/reviews` | authenticated |
| GET/PUT/DELETE | `/api/reviews/admin/list`, `/{id}/visibility`, `/{id}` | `hasRole('ADMIN')` |

### 4.3. Rạp · Phòng · Ghế · Lịch chiếu

| Method | Path | Quyền |
|---|---|---|
| GET | `/api/v1/cinemas`, `/{id}` | permitAll |
| POST/PUT/DELETE | `/api/v1/cinemas`, `/{id}` | `hasRole('ADMIN')` |
| GET/POST/PUT/DELETE | `/api/rooms/cinema/{id}`, `/{id}` | GET auth; ghi `hasRole('ADMIN')` |
| GET | `/api/seats/types`, `/showtime/{id}`, `/room/{id}` | permitAll |
| POST | `/api/seats/layout/{roomId}` | authenticated (thiết kế sơ đồ) |
| GET | `/api/showtimes/*` (cities, upcoming, by-movie, by-cinema, movie/{id}, cinema/{id}) | permitAll |
| POST | `/api/showtimes` · `/batch` | `@perm.can('schedules','add')` |
| GET | `/api/showtimes/{id}/detail` | authenticated |
| PATCH/DELETE | `/api/showtimes/{id}` | `@perm.can('schedules','edit')` |

### 4.4. Đặt vé · POS · Vé · Thanh toán

| Method | Path | Quyền |
|---|---|---|
| POST | `/api/bookings/hold`, `/{id}/payment/complete`, `/{id}/release` | authenticated |
| GET | `/api/bookings/history` | authenticated |
| GET | `/api/ticketing/showtimes`, `/combos`, `/member-card/{phone}` | `@perm.can('pos_ticketing','view')` |
| POST | `/api/ticketing/pay`, `/concession`, `/hold`, `/hold/{id}/release` | `@perm.can('pos_ticketing','add')` |
| GET | `/api/admin/bookings`, `/{id}` | `@perm.can('pos_ticketing','view')` |
| POST | `/api/tickets/lookup`, `/print` | STAFF/ADMIN/MANAGER |
| GET | `/api/tickets/booking/{id}` | authenticated |
| POST | `/api/payment/create_payment` | authenticated |
| GET | `/api/payment/vnpay_return` | permitAll |

### 4.5. Voucher · Khuyến mãi · F&B

| Method | Path | Quyền |
|---|---|---|
| GET/POST | `/api/vouchers/customer/{id}`, `/preview`, `/lookup`, `/apply`, `/claim`, `/redeem`, `/validate` | authenticated |
| GET | `/api/marketing/promotions`, `/active`, `/redeemable`, `/combos` | active permitAll; còn lại auth |
| POST/PUT/DELETE | `/api/marketing/promotions`, `/{id}`, `/{id}/send-campaign`, `/{promoId}/issue-voucher` | `@perm.can('promotions', add/edit/delete)` |
| GET | `/api/fnbs` | permitAll |
| GET/POST/PUT/DELETE | `/api/fnbs/all`, `/`, `/{id}` | `@perm.can('fnb_menu', view/add/edit/delete)` |
| GET/POST/PUT/PATCH/DELETE | `/api/promo-articles/*` · `/api/banners/*` | GET công khai (/active, /); ghi `@perm.can('promotions'/'banners', ...)` |

### 4.6. Định giá · Nhân sự · Phê duyệt

| Method | Path | Quyền |
|---|---|---|
| GET | `/api/pricing/config` | authenticated |
| PUT/POST/DELETE | `/api/pricing/base-matrix`, `/seat-types`, `/formats`, `/holidays`, `/simulate` | `@perm.can('pricing','edit')` |
| GET | `/api/staff/list`, `/cinema-roster/{id}` | `@perm.can('staff_management','view')` |
| POST/PUT | `/api/staff`, `/{id}`, `/{id}/toggle` | `@perm.can('staff_management', add/edit)` |
| POST/GET/PUT | `/api/staff/approvals/fnb-void`, `/pending`, `/mine`, `/{id}/approve|reject` | Tạo/xem: ADMIN/MANAGER/STAFF; duyệt/từ chối: **ADMIN/MANAGER** |

> Đã gỡ toàn bộ endpoint Ca làm việc: `/shifts*`, `/shifts/{id}/check-in|check-out`, `/handovers*`, và `/approvals/seat-move*`.

### 4.7. Hệ thống · Dashboard · CSKH · Phân quyền

| Method | Path | Quyền |
|---|---|---|
| GET | `/api/dashboard/stats` | `@perm.can('dashboard_stats','view')` |
| GET | `/api/settings`, `/{key}` | GET permitAll |
| POST | `/api/settings` | `@perm.can('settings','edit')` |
| GET | `/api/admin/logs` | authenticated (ADMIN scope) |
| GET/POST/PUT/DELETE | `/api/support-tickets/*` | GET auth; reply/status `@perm.can('support','edit')`; delete `('support','delete')` |
| GET/POST/PUT/DELETE | `/api/faqs`, `/all`, `/{id}` | GET công khai; ghi `hasRole('ADMIN')` |
| POST | `/api/upload` | ADMIN/MANAGER/STAFF |
| GET | `/api/locations/provinces`, `/districts` | permitAll |
| GET/PUT | `/api/admin/roles`, `/staff-users`, `/{id}/permissions`, `/users/{id}/permission-overrides`, `/me/permissions` | `hasRole('ADMIN') or @perm.can('roles','manage')` (riêng `/me/permissions` authenticated) |
| GET/PUT | `/api/notifications/customer/{id}`, `/unread-count`, `/{id}/read`, `/read-all` | authenticated |

---

## PHẦN 5 — BẢNG PHÂN QUYỀN THAO TÁC TOÀN HỆ THỐNG

> Nguồn: `DataSeeder` (cờ seed `PERMISSION_MATRIX_V4`). ADMIN = toàn quyền (hardcode trong `PermissionService.can`). MANAGER = "admin thu nhỏ theo 1 cơ sở". STAFF = trần tĩnh; **có `pos_ticketing:add` là bán vé/soát vé/bán F&B được ngay tại cơ sở của mình** (không cần ca làm việc). CUSTOMER không dùng ma trận (chỉ endpoint công khai/authenticated).

### 5.1. Ma trận `feature × action` theo vai trò

| Feature (chức năng) | ADMIN | MANAGER | STAFF |
|---|---|---|---|
| `dashboard_stats` (báo cáo doanh thu) | view, export | view | — |
| `movies` (phim/thể loại/định dạng) | view, add, edit, delete | view | view |
| `schedules` (lịch chiếu) | view, add, edit, delete | view, add, edit | view |
| `banners` (banner trang chủ) | view, add, edit, delete | view, add, edit, delete | — |
| `promotions` (khuyến mãi/voucher/tin KM) | view, add, edit, delete | view, add, edit, delete | — |
| `pricing` (định giá) | view, add, edit, delete | view, edit | — |
| `cinemas` (cụm rạp) | view, add, edit, delete | view | — |
| `staff_management` (nhân sự) | view, add, edit, delete | view, add, edit | — |
| `pos_ticketing` (bán vé tại quầy) | view, add, edit, delete | view, add | view, add |
| `fnb_menu` (thực đơn F&B — sửa giá món) | view, add, edit, delete | view, add, edit, delete | — |
| `support` (CSKH) | view, edit, delete | view, edit | view |
| `settings` (cài đặt hệ thống) | view, edit | view | — |
| `roles` (phân quyền) | *(ADMIN-cứng)* | — | — |

### 5.2. Quyền "cứng" theo vai trò (không qua ma trận)

| Nhóm endpoint | Quyền enforce |
|---|---|
| Cụm rạp/Phòng CRUD (`/api/v1/cinemas`, `/api/rooms`) | `hasRole('ADMIN')` |
| Quản lý FAQ, Đánh giá phim (`/api/faqs`, `/api/reviews/admin`) | `hasRole('ADMIN')` |
| Phân quyền vai trò/override (`/api/admin/roles`) | `hasRole('ADMIN') or @perm.can('roles','manage')` |
| Kiểm vé/in vé (`/api/tickets/lookup`, `/print`) | `hasAnyRole('STAFF','ADMIN','MANAGER')` + Cinema Scoping |
| Tạo/xem yêu cầu sửa sai (`/approvals/fnb-void`, `/pending`, `/mine`) | `hasAnyRole('STAFF','ADMIN','MANAGER')` |
| Duyệt/từ chối sửa sai (`/approvals/{id}/approve|reject`) | **ADMIN/MANAGER** |
| Upload ảnh Cloudinary | `hasAnyRole('ADMIN','MANAGER','STAFF')` |

### 5.3. Ràng buộc quyền động (bổ sung lên quyền tĩnh)

1. **Cinema Scoping (STAFF/MANAGER):** mọi thao tác POS/F&B/soát vé bị ràng buộc theo `cinemaId` trong JWT (`SecurityUtils.assertCinemaAccess`); bán/soát chéo rạp → **403**. Báo cáo thiếu cơ sở → **fail closed (403)**. Chỉ ADMIN xem/thao tác toàn hệ thống.
2. **UserPermissionOverride:** cấp/thu quyền theo **từng người** (ALLOW/DENY), ưu tiên đè lên ma trận vai trò.

### 5.4. Vai trò CUSTOMER

Không dùng ma trận. Truy cập: endpoint công khai (duyệt phim/lịch chiếu/rạp/khuyến mãi) + endpoint `authenticated` gắn với chính mình (đặt vé, lịch sử, voucher, điểm, hồ sơ, đánh giá, thông báo). Không có bất kỳ quyền admin nào.

---

## PHỤ LỤC — TÓM TẮT NỢ KỸ THUẬT & PHẠM VI CỐ Ý BỎ

- **Đã gỡ hoàn toàn:** Ví điện tử (Wallet*), Kho/Định mức BOM (Inventory/BomRecipe) — tồn kho F&B vô hạn.
- **Phạm vi cố ý bỏ** (Quản lý Vận hành, không HRM): tính lương, phạt đi muộn, đơn nghỉ/đổi ca.
- **Ngoại lệ `ApiResponse`:** `PaymentController` giữ convention VNPAY `{code,message,data}`.
- **Còn lại:** 8 cảnh báo Dependabot (đụng file bảo vệ `pom.xml`/`package.json`); nhánh fail-closed dashboard chưa có tình huống thật để test.

---

*Tài liệu này được sinh từ phân tích trực tiếp mã nguồn (entity, service, controller, config). Mọi công thức, ràng buộc và quyền hạn phản ánh đúng hành vi code tại thời điểm lập.*
