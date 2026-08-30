# Database Schema — DevCine

> **Version:** 1.0  
> **Cập nhật:** 2026-05-26  
> **Database:** PostgreSQL 15+ (Supabase)  
> **ORM:** Hibernate 7.x / Spring Data JPA  
> **Tổng bảng:** 30

---

## Mục Lục

1. [User & Auth Domain](#1-user--auth-domain-5-bảng)
2. [Wallet Domain](#2-wallet-domain-2-bảng)
3. [Movie Domain](#3-movie-domain-4-bảng)
4. [Cinema & Room Domain](#4-cinema--room-domain-4-bảng)
5. [Showtime & Booking Domain](#5-showtime--booking-domain-5-bảng)
6. [F&B Domain](#6-fb-domain-5-bảng)
7. [Promotion Domain](#7-promotion-domain-2-bảng)
8. [Staff Management Domain](#8-staff-management-domain-3-bảng)
9. [CMS Domain](#9-cms-domain-3-bảng)
10. [Sơ đồ quan hệ](#10-sơ-đồ-quan-hệ-tổng-thể)
11. [Quy tắc Migration](#11-quy-tắc-migration)

---

## 1. User & Auth Domain (5 bảng)

### 1.1 `roles`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID vai trò |
| `name` | `VARCHAR(255)` | NOT NULL, UNIQUE | Tên vai trò (ADMIN, MANAGER, STAFF, CUSTOMER) |
| `permissions_matrix` | `TEXT` | — | JSON string chứa ma trận phân quyền |

### 1.2 `users`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID người dùng |
| `username` | `VARCHAR(255)` | NOT NULL, UNIQUE | Tên đăng nhập |
| `password_hash` | `VARCHAR(255)` | NOT NULL | Mật khẩu đã hash (BCrypt) |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE | Email |
| `phone` | `VARCHAR(20)` | — | Số điện thoại |
| `role_id` | `INTEGER` | NOT NULL, FK → `roles.id` | Vai trò |
| `is_active` | `BOOLEAN` | NOT NULL, DEFAULT true | Trạng thái tài khoản |
| `created_at` | `TIMESTAMP` | NOT NULL | Thời điểm tạo |

### 1.3 `customers`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `user_id` | `INTEGER` | PK, FK → `users.id` | ID khách hàng = ID user (1:1) |
| `dob` | `DATE` | — | Ngày sinh |
| `id_card` | `VARCHAR(20)` | — | Số CCCD/CMND |
| `membership_tier` | `VARCHAR(50)` | — | Hạng thành viên (BRONZE, SILVER, GOLD, PLATINUM) |
| `loyalty_points` | `INTEGER` | DEFAULT 0 | Điểm tích lũy |

### 1.4 `staffs`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `user_id` | `INTEGER` | PK, FK → `users.id` | ID nhân viên = ID user (1:1) |
| `staff_code` | `VARCHAR(20)` | UNIQUE | Mã nhân viên (NV001, NV002...) |
| `cinema_id` | `INTEGER` | FK → `cinemas.id` | Rạp đang làm việc |
| `manager_id` | `INTEGER` | FK → `staffs.user_id` | Quản lý trực tiếp (self-reference) |

### 1.5 `audit_logs`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID log |
| `user_id` | `INTEGER` | FK → `users.id` | Người thực hiện |
| `action` | `VARCHAR(50)` | NOT NULL | Hành động (CREATE, UPDATE, DELETE, LOGIN) |
| `target_table` | `VARCHAR(100)` | — | Bảng bị tác động |
| `ip_address` | `VARCHAR(45)` | — | Địa chỉ IP (hỗ trợ IPv6) |
| `timestamp` | `TIMESTAMP` | NOT NULL | Thời điểm |

---

## 2. Wallet Domain (2 bảng)

### 2.1 `wallets`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID ví |
| `customer_id` | `INTEGER` | NOT NULL, UNIQUE, FK → `customers.user_id` | Chủ ví (1:1) |
| `balance` | `DECIMAL(15,2)` | NOT NULL, DEFAULT 0 | Số dư (VND) |
| `status` | `VARCHAR(20)` | — | Trạng thái (ACTIVE, FROZEN, CLOSED) |

### 2.2 `wallet_transactions`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID giao dịch |
| `wallet_id` | `INTEGER` | NOT NULL, FK → `wallets.id` | Ví liên quan |
| `type` | `VARCHAR(20)` | NOT NULL | Loại (DEPOSIT, WITHDRAW, PAYMENT, REFUND) |
| `amount` | `DECIMAL(15,2)` | NOT NULL | Số tiền |
| `description` | `VARCHAR(500)` | — | Mô tả giao dịch |
| `created_at` | `TIMESTAMP` | NOT NULL | Thời điểm |

---

## 3. Movie Domain (4 bảng)

### 3.1 `movies`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID phim |
| `title` | `VARCHAR(255)` | NOT NULL | Tên phim |
| `slug` | `VARCHAR(255)` | UNIQUE | URL-friendly name |
| `duration_mins` | `INTEGER` | — | Thời lượng (phút) |
| `age_rating` | `VARCHAR(10)` | — | Phân loại tuổi (P, C13, C16, C18) |
| `release_date` | `DATE` | — | Ngày khởi chiếu |
| `end_date` | `DATE` | — | Ngày kết thúc |
| `status` | `VARCHAR(20)` | — | Trạng thái (COMING_SOON, NOW_SHOWING, ENDED) |

### 3.2 `categories`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID thể loại |
| `name` | `VARCHAR(100)` | NOT NULL, UNIQUE | Tên (Hành động, Kinh dị, Tình cảm...) |
| `description` | `VARCHAR(500)` | — | Mô tả |

### 3.3 `movie_categories` (Junction table)

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `movie_id` | `INTEGER` | PK, FK → `movies.id` | ID phim |
| `category_id` | `INTEGER` | PK, FK → `categories.id` | ID thể loại |

### 3.4 `movie_formats`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID định dạng |
| `name` | `VARCHAR(50)` | NOT NULL, UNIQUE | Tên (2D, 3D, IMAX, 4DX) |
| `surcharge` | `DECIMAL(15,2)` | — | Phụ thu so với giá gốc |

---

## 4. Cinema & Room Domain (4 bảng)

### 4.1 `cinemas`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID rạp |
| `name` | `VARCHAR(255)` | NOT NULL | Tên rạp |
| `address` | `VARCHAR(500)` | — | Địa chỉ |
| `manager_id` | `INTEGER` | FK → `staffs.user_id` | Quản lý rạp |

### 4.2 `rooms`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID phòng |
| `cinema_id` | `INTEGER` | NOT NULL, FK → `cinemas.id` | Thuộc rạp nào |
| `name` | `VARCHAR(50)` | NOT NULL | Tên phòng (Room 1, Room 2...) |
| `type` | `VARCHAR(30)` | — | Loại phòng (STANDARD, VIP, PREMIUM) |
| `status` | `VARCHAR(20)` | — | Trạng thái (ACTIVE, MAINTENANCE, CLOSED) |
| `turnaround_time_mins` | `INTEGER` | — | Thời gian dọn dẹp giữa 2 suất (phút) |

### 4.3 `seat_types`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID loại ghế |
| `name` | `VARCHAR(50)` | NOT NULL, UNIQUE | Tên (Standard, VIP, Couple, Sweetbox) |
| `color_code` | `VARCHAR(10)` | — | Mã màu hiển thị (#FF5733) |
| `price_modifier` | `DECIMAL(15,2)` | — | Phụ thu loại ghế — số tiền **cộng thêm** vào giá nền (0 = không phụ thu, 15000 = +15.000đ). Xem `PricingService`. |

### 4.4 `seats`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID ghế |
| `room_id` | `INTEGER` | NOT NULL, FK → `rooms.id` | Thuộc phòng nào |
| `row_char` | `VARCHAR(2)` | NOT NULL | Hàng ghế (A, B, C...) |
| `col_num` | `INTEGER` | NOT NULL | Số cột (1, 2, 3...) |
| `seat_type_id` | `INTEGER` | NOT NULL, FK → `seat_types.id` | Loại ghế |
| `is_active` | `BOOLEAN` | NOT NULL, DEFAULT true | Còn hoạt động không |

---

## 5. Showtime & Booking Domain (7 bảng)

### 5.1 `showtimes`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID suất chiếu |
| `movie_id` | `INTEGER` | NOT NULL, FK → `movies.id` | Phim chiếu |
| `room_id` | `INTEGER` | NOT NULL, FK → `rooms.id` | Phòng chiếu |
| `format_id` | `INTEGER` | NOT NULL, FK → `movie_formats.id` | Định dạng chiếu |
| `start_time` | `TIMESTAMP` | NOT NULL | Giờ bắt đầu |
| `end_time` | `TIMESTAMP` | NOT NULL | Giờ kết thúc |
| `status` | `VARCHAR(20)` | — | Trạng thái (SCHEDULED, ONGOING, COMPLETED, CANCELLED) |

### 5.2 `pricing_rules`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID quy tắc giá |
| `name` | `VARCHAR(100)` | NOT NULL | Tên quy tắc |
| `rule_type` | `VARCHAR(30)` | NOT NULL | Loại (WEEKDAY, WEEKEND, HOLIDAY, EARLY_BIRD) |
| `value` | `DECIMAL(15,2)` | NOT NULL | Giá trị (VND hoặc %) |
| `start_date` | `TIMESTAMP` | — | Hiệu lực từ |
| `end_date` | `TIMESTAMP` | — | Hiệu lực đến |

### 5.3 `booking_seats`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID đặt ghế |
| `booking_id` | `INTEGER` | NOT NULL | ID booking (foreign logic) |
| `seat_id` | `INTEGER` | NOT NULL, FK → `seats.id` | Ghế đã đặt |
| `price_snapshot` | `DECIMAL(15,2)` | — | Giá tại thời điểm đặt |
| `status` | `VARCHAR(20)` | — | Trạng thái (RESERVED, CONFIRMED, CANCELLED) |

### 5.4 `tickets`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID vé |
| `booking_seat_id` | `INTEGER` | NOT NULL, UNIQUE, FK → `booking_seats.id` | Ghế đã đặt (1:1) |
| `qr_code` | `VARCHAR(500)` | — | Mã QR để check-in |
| `is_checked_in` | `BOOLEAN` | NOT NULL, DEFAULT false | Đã check-in chưa |
| `is_age_verified` | `BOOLEAN` | DEFAULT false | Đã xác minh tuổi chưa |
| `checked_in_by` | `INTEGER` | FK → `staffs.user_id` | Nhân viên check-in |
| `check_in_time` | `TIMESTAMP` | — | Thời điểm check-in |
| `version` | `INTEGER` | DEFAULT 1 | Phiên bản QR hiện hành của vé |
| `is_revoked` | `BOOLEAN` | DEFAULT false | Cờ tương thích cho vé bị thu hồi |

### 5.5 `ticket_qr_histories`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID lịch sử QR |
| `ticket_id` | `INTEGER` | NOT NULL, FK → `tickets.id` | Vé hiện hành liên quan |
| `qr_code` | `VARCHAR(500)` | NOT NULL, UNIQUE | QR cũ đã bị thu hồi |
| `ticket_version` | `INTEGER` | NOT NULL | Phiên bản của QR cũ |
| `revoked_at` | `TIMESTAMP` | NOT NULL | Thời điểm thu hồi |
| `revoked_reason` | `VARCHAR(255)` | — | Lý do thu hồi, gồm nhãn ghế cũ/mới |

### 5.6 `seat_incidents`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID sự cố |
| `incident_type` | `VARCHAR(20)` | NOT NULL | RELOCATE, CANCEL, SEAT_MAINTENANCE, EMERGENCY_CLOSURE |
| `booking_id` | `INTEGER` | FK → `bookings.id` | Đơn liên quan |
| `showtime_id` | `INTEGER` | FK → `showtimes.id` | Suất chiếu liên quan |
| `old_seat_id` | `INTEGER` | FK → `seats.id` | Ghế nguồn |
| `new_seat_id` | `INTEGER` | FK → `seats.id` | Ghế đích sau khi đổi |
| `old_seat_label` | `VARCHAR(10)` | — | Snapshot nhãn ghế nguồn |
| `new_seat_label` | `VARCHAR(10)` | — | Snapshot nhãn ghế đích |
| `compensation_type` | `VARCHAR(20)` | — | NONE, DISCOUNT, GIFT_FNB, GIFT_TICKET |
| `compensation_amount` | `DECIMAL(15,2)` | — | Giá trị đền bù quy tiền |
| `voucher_id` | `INTEGER` | FK → `vouchers.id` | Voucher đền bù đã phát |
| `audit_gift_code` | `VARCHAR(80)` | UNIQUE | Mã phiếu quà quầy cho khách vãng lai |
| `handled_by` | `INTEGER` | FK → `staffs.user_id` | Nhân viên xử lý |
| `cinema_id` | `INTEGER` | FK → `cinemas.id` | Cụm rạp xử lý |
| `reason` | `VARCHAR(255)` | — | Lý do/ghi chú |
| `created_at` | `TIMESTAMP` | NOT NULL | Thời điểm xử lý |

### 5.7 `reviews`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID đánh giá |
| `customer_id` | `INTEGER` | NOT NULL, FK → `customers.user_id` | Người đánh giá |
| `movie_id` | `INTEGER` | NOT NULL, FK → `movies.id` | Phim được đánh giá |
| `ticket_id` | `INTEGER` | FK → `tickets.id` | Vé liên quan (xác thực đã xem) |
| `rating` | `INTEGER` | NOT NULL, CHECK (1-5) | Điểm (1-5 sao) |
| `comment` | `TEXT` | — | Nội dung đánh giá |
| `created_at` | `TIMESTAMP` | NOT NULL | Thời điểm |

---

## 6. F&B Domain (2 bảng)

### 6.1 `fnb_items`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID sản phẩm |
| `name` | `VARCHAR(255)` | NOT NULL | Tên (Bắp rang bơ, Coca Cola...) |
| `type` | `VARCHAR(30)` | — | Loại (FOOD, DRINK, COMBO) |
| `price` | `DECIMAL(15,2)` | NOT NULL | Giá bán |

### 6.2 `booking_fnbs`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID đặt F&B |
| `booking_id` | `INTEGER` | NOT NULL | ID booking |
| `fnb_item_id` | `INTEGER` | NOT NULL, FK → `fnb_items.id` | Sản phẩm |
| `quantity` | `INTEGER` | NOT NULL | Số lượng |
| `price_snapshot` | `DECIMAL(15,2)` | — | Giá tại thời điểm đặt |

---

## 7. Promotion Domain (2 bảng)

### 7.1 `promotions`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID khuyến mãi |
| `code` | `VARCHAR(50)` | UNIQUE | Mã khuyến mãi (SUMMER2026) |
| `discount_type` | `VARCHAR(20)` | NOT NULL | Loại giảm (PERCENTAGE, FIXED_AMOUNT) |
| `discount_value` | `DECIMAL(15,2)` | NOT NULL | Giá trị giảm |
| `start_date` | `TIMESTAMP` | — | Bắt đầu |
| `end_date` | `TIMESTAMP` | — | Kết thúc |
| `is_stackable` | `BOOLEAN` | NOT NULL, DEFAULT false | Có thể cộng dồn không |

### 7.2 `vouchers`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID voucher |
| `customer_id` | `INTEGER` | NOT NULL, FK → `customers.user_id` | Khách được cấp |
| `promotion_id` | `INTEGER` | NOT NULL, FK → `promotions.id` | Khuyến mãi gốc |
| `valid_until` | `TIMESTAMP` | — | Hạn sử dụng |
| `is_used` | `BOOLEAN` | NOT NULL, DEFAULT false | Đã dùng chưa |

---

## 8. Staff Management Domain (3 bảng)

### 8.1 `shifts`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID ca |
| `start_time` | `TIMESTAMP` | NOT NULL | Giờ bắt đầu |
| `end_time` | `TIMESTAMP` | NOT NULL | Giờ kết thúc |
| `status` | `VARCHAR(20)` | — | Trạng thái (ACTIVE, INACTIVE) |

### 8.2 `staff_schedules`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID lịch |
| `staff_id` | `INTEGER` | NOT NULL, FK → `staffs.user_id` | Nhân viên |
| `shift_id` | `INTEGER` | NOT NULL, FK → `shifts.id` | Ca trực |
| `work_date` | `DATE` | NOT NULL | Ngày làm |
| `status` | `VARCHAR(20)` | — | Trạng thái (SCHEDULED, CHECKED_IN, ABSENT) |

### 8.3 `shift_handovers`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID bàn giao |
| `staff_schedule_id` | `INTEGER` | NOT NULL, FK → `staff_schedules.id` | Lịch trực liên quan |
| `approved_by_manager` | `INTEGER` | FK → `staffs.user_id` | Quản lý duyệt |
| `declared_cash` | `DECIMAL(15,2)` | — | Tiền mặt khai báo |
| `system_cash` | `DECIMAL(15,2)` | — | Tiền hệ thống ghi nhận |
| `difference` | `DECIMAL(15,2)` | — | Chênh lệch |
| `status` | `VARCHAR(20)` | — | Trạng thái (PENDING, APPROVED, REJECTED) |

---

## 9. CMS Domain (3 bảng)

### 9.1 `support_tickets`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID ticket |
| `customer_id` | `INTEGER` | NOT NULL, FK → `customers.user_id` | Khách phản ánh |
| `assigned_to_staff` | `INTEGER` | FK → `staffs.user_id` | NV phụ trách |
| `issue_type` | `VARCHAR(50)` | — | Loại (REFUND, COMPLAINT, QUESTION, BUG_REPORT) |
| `status` | `VARCHAR(20)` | — | Trạng thái (OPEN, IN_PROGRESS, RESOLVED, CLOSED) |
| `created_at` | `TIMESTAMP` | NOT NULL | Thời điểm tạo |

### 9.2 `banners`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID banner |
| `title` | `VARCHAR(255)` | — | Tiêu đề |
| `image_url` | `VARCHAR(500)` | — | URL hình ảnh |
| `placement` | `VARCHAR(50)` | — | Vị trí (HOME_TOP, HOME_SIDE, BOOKING_PAGE) |
| `start_date` | `TIMESTAMP` | — | Hiển thị từ |
| `end_date` | `TIMESTAMP` | — | Hiển thị đến |

### 9.3 `lost_and_founds`

| Cột | Type | Constraint | Mô tả |
|-----|------|-----------|-------|
| `id` | `INTEGER` | PK, AUTO_INCREMENT | ID |
| `cinema_id` | `INTEGER` | FK → `cinemas.id` | Rạp tìm thấy |
| `found_by_staff` | `INTEGER` | FK → `staffs.user_id` | NV tìm thấy |
| `item_description` | `TEXT` | — | Mô tả vật phẩm |
| `found_location` | `VARCHAR(255)` | — | Vị trí tìm thấy |
| `status` | `VARCHAR(20)` | — | Trạng thái (FOUND, CLAIMED, DISCARDED) |
| `found_at` | `TIMESTAMP` | — | Thời điểm tìm |
| `claimed_at` | `TIMESTAMP` | — | Thời điểm trả |

---

## 10. Sơ Đồ Quan Hệ Tổng Thể

### Quan hệ 1:1

```
users       ←──── 1:1 ────→ customers     (shared PK: user_id)
users       ←──── 1:1 ────→ staffs        (shared PK: user_id)
customers   ←──── 1:1 ────→ wallets       (customer_id UNIQUE)
booking_seats ←── 1:1 ────→ tickets       (booking_seat_id UNIQUE)
tickets       ←── 1:N ────→ ticket_qr_histories
bookings      ←── 1:N ────→ seat_incidents
```

### Quan hệ 1:N

```
roles           1 ──→ N users
users           1 ──→ N audit_logs
cinemas         1 ──→ N rooms
rooms           1 ──→ N seats
seat_types      1 ──→ N seats
movies          1 ──→ N showtimes
rooms           1 ──→ N showtimes
movie_formats   1 ──→ N showtimes
wallets         1 ──→ N wallet_transactions
customers       1 ──→ N reviews
customers       1 ──→ N vouchers
promotions      1 ──→ N vouchers
staffs          1 ──→ N staff_schedules
shifts          1 ──→ N staff_schedules
staff_schedules 1 ──→ N shift_handovers
staffs (self)   1 ──→ N staffs (manager_id)
cinemas         1 ──→ N lost_and_founds
```

### Quan hệ N:N

```
movies ←──── N:N ────→ categories   (qua movie_categories)
```

---

## 11. Quy Tắc Migration

### 11.1 Nguyên tắc chung

1. **DDL Auto = `update`**: Hibernate chỉ thêm mới, không xóa cột/bảng cũ
2. **KHÔNG BAO GIỜ** dùng `ddl-auto=create` trên production
3. Mọi thay đổi schema phải được document trong `CHANGELOG.md`
4. Backup database trước khi migration

### 11.2 Quy trình thêm bảng/cột mới

```
1. Cập nhật DATABASE.md với bảng/cột mới
2. Tạo/sửa Entity class tương ứng
3. Chạy app với ddl-auto=update → Hibernate tạo bảng
4. Verify schema trên Supabase Dashboard
5. Commit + ghi CHANGELOG.md
```

### 11.3 Quy trình xóa/sửa cột

```
1. KHÔNG dùng ddl-auto để xóa — phải dùng SQL migration thủ công
2. Tạo file migration: docs/migrations/V{version}__{description}.sql
3. Review SQL → chạy trên staging trước
4. Chạy trên production sau khi staging OK
5. Cập nhật Entity + DATABASE.md
```

### 11.4 Naming convention cho migration files

```
V001__initial_schema.sql
V002__add_lost_and_founds_table.sql
V003__add_avatar_column_to_users.sql
```

---

## 12. Addendum 2026-07-03: Shift Operations

Các cột nullable được thêm để gắn nghiệp vụ vận hành theo ca, tương thích `ddl-auto=update`:

- `bookings.staff_schedule_id` FK -> `staff_schedules.id`: ca POS tạo/hoàn tất đơn.
- `concession_sales.staff_schedule_id` FK -> `staff_schedules.id`: ca bán F&B độc lập.
- `audit_logs.staff_schedule_id` FK -> `staff_schedules.id`: ca hiện tại khi STAFF thực hiện thao tác.
- `shift_handovers.cash_sales`, `card_sales`, `transfer_sales`, `ticket_revenue`, `concession_revenue`, `ticket_count`, `concession_order_count`, `submitted_at`, `confirmed_at`, `note`: dữ liệu đối soát cuối ca.
