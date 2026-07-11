# ERD_Ver7 ↔ Code — Bảng đối chiếu sai lệch (field-by-field)

> Sinh ngày 2026-06-30 bằng cách đối chiếu `docs/ERD_Ver7.png` với 40 entity trong
> `devcine-backend/src/main/java/com/devcine/backend/entity/`.
>
> Ký hiệu cột "Tình trạng": ✅ khớp · ✳️ khác (đổi tên / kiểu / nullable) · ➕ code có, ERD không · ➖ ERD có, code không.

---

## 0. Tổng quan

| Nhóm | Số lượng | Danh sách |
|---|---|---|
| Bảng **khớp tên** giữa ERD và code | 21 | roles, users, customers, staffs, audit_logs, support_tickets, cinemas, rooms, seats, seat_types, showtimes, movies, movie_formats, movie_categories, categories, bookings, booking_seats, booking_fnbs, tickets, reviews, promotions, vouchers, fnb_items, shifts, staff_schedules, shift_handovers, pricing_rules, banners |
| Bảng **chỉ có trong ERD** (đã gỡ khỏi code) | 5 | `wallets`, `wallet_transactions`, `bom_recipes`, `cinema_inventory`, `inventory_logs` |
| Bảng **chỉ có trong code** (thêm mới sau ERD) | 9 | `system_settings`, `notifications`, `age_ratings`, `faqs`, `holidays`, `special_seat_prices`, `promo_articles`, `concession_sales`, `concession_sale_items` |
| Bảng nối **không có trong ERD** | 1 | `movie_genre_mapping` (Movie `@ManyToMany`) — trùng vai trò với `movie_categories` |

**Kết luận nhanh:** ERD = 33 bảng → Code = 37 bảng (+9 mới, −2 ví, −3 kho/định mức, và 1 bảng nối ẩn). Lớp Quản lý kho (`bom_recipes`, `cinema_inventory`, `inventory_logs`) đã gỡ khỏi code (ngoài phạm vi đồ án); bảng cũ để lại rỗng trong DB. Nhiều bảng giữ nguyên tên nhưng số cột đã phình to so với ERD.

---

## 1. Bảng ERD đã bị GỠ khỏi code ➖

### `wallets` — ĐÃ GỠ
ERD: `id (PK)`, `customer_id (FK)`, `balance (decimal)`, `status (string)`
→ Không còn entity nào. Ví điện tử bị gỡ hoàn toàn (xem CLAUDE.md).

### `wallet_transactions` — ĐÃ GỠ
ERD: `id (PK)`, `wallet_id (FK)`, `wallet_id (FK)` *(ERD bị lặp cột này)*, `amount (decimal)`, `type (string)`, `description (string)`, `created_at (datetime)`
→ Không còn entity nào.

---

## 2. Bảng code thêm MỚI (ERD chưa có) ➕

### `system_settings` (SystemSetting)
| Kiểu | Cột | Khóa |
|---|---|---|
| string | setting_key | PK |
| text | setting_value | |

### `notifications` (Notification)
| Kiểu | Cột | Khóa |
|---|---|---|
| int | id | PK |
| int | customer_id | FK → customers |
| string | title | |
| text | message | |
| string | type | (BOOKING\|REMINDER\|PROMOTION\|SYSTEM) |
| boolean | is_read | |
| datetime | created_at | |

### `age_ratings` (AgeRating) — danh mục độ tuổi
| Kiểu | Cột | Khóa |
|---|---|---|
| int | id | PK |
| string | code | unique (P, K, T13, T16, T18…) |
| string | name | |
| string | description | |

> Lưu ý: ERD để `age_rating` là **cột string** trong `movies`. Code vừa giữ cột `movies.age_rating (string)` vừa thêm bảng danh mục `age_ratings` riêng — **nhưng `movies` KHÔNG có FK trỏ tới `age_ratings`** (chưa liên kết).

### `faqs` (Faq)
| Kiểu | Cột | Khóa |
|---|---|---|
| int | id | PK |
| string | category | |
| string | question | |
| text | answer | |
| int | display_order | |
| boolean | is_active | |

### `holidays` (Holiday)
| Kiểu | Cột | Khóa |
|---|---|---|
| int | id | PK |
| date | holiday_date | unique |
| string | name | |

### `special_seat_prices` (SpecialSeatPrice) — giá cố định cho IMAX/4DX/Gold
| Kiểu | Cột | Khóa |
|---|---|---|
| int | id | PK |
| int | format_id | FK → movie_formats |
| int | seat_type_id | FK → seat_types |
| decimal | price | |
| | *(unique: format_id + seat_type_id)* | |

### `promo_articles` (PromoArticle) — tin khuyến mãi biên tập (khác Promotion)
| Kiểu | Cột | Khóa |
|---|---|---|
| int | id | PK |
| string | title | |
| string | description | |
| string | image_url | |
| text | content | |
| date | start_date | |
| date | end_date | |
| boolean | is_active | |
| int | display_order | |
| datetime | created_at | |

### `concession_sales` (ConcessionSale) — đơn bán bắp nước tại quầy, không gắn suất chiếu
| Kiểu | Cột | Khóa |
|---|---|---|
| int | id | PK |
| string | sale_code | unique |
| int | customer_id | FK → customers (nullable, khách vãng lai) |
| decimal | total_price | |
| string | payment_method | |
| string | status | |
| datetime | created_at | |

### `concession_sale_items` (ConcessionSaleItem)
| Kiểu | Cột | Khóa |
|---|---|---|
| int | id | PK |
| int | sale_id | FK → concession_sales |
| int | fnb_item_id | FK → fnb_items |
| int | quantity | |
| decimal | price_snapshot | |

### `movie_genre_mapping` (bảng nối ẩn, sinh từ Movie `@ManyToMany`)
| Cột | Khóa |
|---|---|
| movie_id | FK → movies |
| category_id | FK → categories |

> ⚠️ Trùng vai trò với `movie_categories`. Dự án đang có **2 cơ chế phim–thể loại song song**. Nên hợp nhất.

---

## 3. Bảng giữ tên nhưng SAI LỆCH cột

### `users`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id (PK) | id |
| ✅ | username | username |
| ✅ | password_hash | password_hash |
| ✅ | email | email |
| ✅ | phone | phone |
| ✅ | role_id (FK) | role (FK) |
| ✅ | is_active | is_active |
| ✅ | created_at | created_at |
| ➕ | — | **full_name** (not null) |
| ➕ | — | **avatar_url** |

### `customers` ✅ khớp hoàn toàn
id↔user_id (PK, @MapsId), dob, id_card, membership_tier, loyalty_points.

### `staffs` ✅ khớp
user_id (PK, @MapsId), staff_code, cinema_id (FK), manager_id (FK self).

### `roles` ✅ khớp
id, name, permissions_matrix.

### `audit_logs` ✅ khớp
id, user_id (FK), action, target_table, ip_address, timestamp.

### `support_tickets`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, customer_id, assigned_to_staff, issue_type, status, created_at | (giống) |
| ➕ | — | **description** (text) |

### `cinemas`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, name, address, manager_id (FK) | (giống) |
| ➕ | — | **city, district, type, hotline, rooms, image_url, description, latitude, longitude, amenities, status** |

### `rooms`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, cinema_id (FK), name, type, turnaround_time_mins, status | (giống) |
| ➕ | — | **matrix_row, matrix_col** (sơ đồ ghế) |

### `seats`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, room_id, row_char, col_num, seat_type_id, is_active | (giống) |
| ➕ | — | **grid_row, grid_col** |

### `seat_types` ✅ khớp
id, name, color_code, price_modifier.

### `showtimes`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, movie_id, room_id, start_time, end_time, status, format_id (FK) | (giống) |

> Khớp tốt; lưu ý `format_id` trong code là **not null** (ERD vẽ FK thường).

### `movies` — sai lệch LỚN
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, title, slug, duration_mins, age_rating (string), release_date, end_date, status | (giống) |
| ➕ | — | **country, rating, poster_base64, banner_base64, show_on_banner, trailer_url, format, supported_formats, title_vietnamese, production_year, language, base_price, description, original_language, version_type, internal_notes, start_date, distributor, director, cast_members, rating_count** |
| ✳️ | quan hệ qua `movie_categories` | thêm `@ManyToMany` qua **movie_genre_mapping** (song song) |

### `movie_formats`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, name, surcharge | (giống) |
| ➕ | — | **description, weekend_surcharge, is_fixed_price** |

### `movie_categories` ✅ khớp
PK kép (movie_id, category_id).

### `categories`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, name, description | (giống) |
| ✳️ | ERD vẽ `description` là **FK** | Code: `description` chỉ là string thường (không FK) |

### `bookings` — sai lệch về trạng thái & giá
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, customer_id (FK), showtime_id (FK), payment_method, created_at, voucher_id (FK) | (giống) |
| ✳️ | total_amount | **total_price** (đổi tên) |
| ✳️ | payment_status + booking_status (2 cột) | gộp còn **status** (1 cột) |
| ➖ | **hold_expired_at** | (bỏ — thay bằng khóa ghế real-time STOMP) |
| ➕ | — | **final_price, booking_code** (unique) |

### `booking_seats`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, booking_id (FK), seat_id (FK), price_snapshot, status | (giống) |
| ➕ | — | **ticket_type** (ADULT\|STUDENT\|CHILD\|SENIOR) |

### `booking_fnbs` ✅ khớp
id, booking_id (FK), fnb_item_id (FK), quantity, price_snapshot.

### `tickets`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, booking_seat_id (FK), qr_code, is_checked_in, check_in_time, checked_in_by (FK), is_age_verified | (giống) |
| ✳️ | booking_seat_id (FK thường) | code: **OneToOne unique** |

### `reviews`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, customer_id (FK), movie_id (FK), ticket_id (FK), rating, comment, created_at | (giống) |
| ➕ | — | **hidden** (cờ kiểm duyệt) |

### `promotions` — sai lệch trung bình
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, code, discount_type, discount_value, is_stackable, start_date, end_date | (giống) |
| ➕ | — | **name, description, points_required, allow_point_redemption, min_order_value, applicable_movie_id, customer_eligibility, usage_limit, used_count** |

### `vouchers` ✅ khớp
id, customer_id (FK), promotion_id (FK), valid_until, is_used.

### `fnb_items`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, name, type, price | (giống) |
| ➕ | — | **image_url, description, is_active** |

### `shifts` ✅ khớp
id, start_time, end_time, status.

### `staff_schedules` ✅ khớp
id, staff_id (FK), shift_id (FK), work_date, status.

### `shift_handovers` ✅ khớp
id, staff_schedule_id (FK), declared_cash, system_cash, difference, status, approved_by_manager (FK).

### `pricing_rules` — sai lệch trung bình
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, name, rule_type, value, start_date, end_date | (giống) |
| ➕ | — | **day_type, time_slot, audience_type, priority, active** |

### `banners`
| Tình trạng | ERD | Code |
|---|---|---|
| ✅ | id, title, image_url, placement, start_date, end_date | (giống) |
| ➕ | — | **mode, movie_id, is_active, display_order, link** |

---

## 4. Việc nên làm để đồng bộ ERD → Ver8

1. **Xóa** `wallets`, `wallet_transactions` khỏi ERD.
2. **Thêm** 9 bảng mới + bảng nối `movie_genre_mapping`.
3. **Sửa `bookings`**: bỏ `hold_expired_at`, gộp `payment_status`/`booking_status` → `status`, thêm `final_price`, `booking_code`; `total_amount` → `total_price`.
4. **Mở rộng `movies`** (+21 cột) và cân nhắc bỏ `movie_genre_mapping` hoặc `movie_categories` (đang trùng).
5. Bổ sung cột mới cho `promotions`, `pricing_rules`, `banners`, `cinemas`, `movie_formats`, `users`…
6. Quyết định liên kết `movies.age_rating` (string) với bảng `age_ratings` mới (hiện chưa có FK).
