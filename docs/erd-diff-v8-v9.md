# Khác biệt ERD: Ver8 → Ver9

> Ver8 chốt ngày **2026-06-30** (40 entity / 37 bảng liệt kê trong MoTa).
> Ver9 chốt ngày **2026-08-16**: **43 entity + 1 bảng nối = 44 bảng**.
> Đối chiếu trực tiếp với `devcine-backend/src/main/java/com/devcine/backend/entity/`.

| | Ver8 | Ver9 |
|---|---|---|
| Số entity | 40 | **43** |
| Số bảng | 37 (MoTa thiếu `movie_categories`) | **44** |
| Bảng gỡ | — | 4 |
| Bảng thêm | — | 10 |
| Bảng sửa cột | — | 16 |

---

## 1. Bảng bị GỠ (4)

| Bảng | Lý do | Mốc |
|---|---|---|
| `shifts` | Gỡ hoàn toàn phân hệ Ca làm việc; phân quyền chuyển sang RBAC thuần + Strict Cinema Scoping | 01/08/2026 |
| `staff_schedules` | nt | 01/08/2026 |
| `shift_handovers` | nt | 01/08/2026 |
| `special_seat_prices` | Flat Pricing V4 bỏ mô hình giá cố định theo (định dạng × loại ghế) | — |

Kéo theo bỏ 6 quan hệ FK: `shifts→staff_schedules`, `staffs→staff_schedules`,
`staff_schedules→shift_handovers`, `staffs→shift_handovers`,
`movie_formats→special_seat_prices`, `seat_types→special_seat_prices`.

---

## 2. Bảng THÊM MỚI (10)

### 2.1. Nhóm F&B — refactor sang Option Pool + Combo Slot (09/08/2026)

| Bảng | Entity | Vai trò |
|---|---|---|
| `fnb_option_groups` | `FnbOptionGroup` | Pool lựa chọn dùng chung (Nước ngọt, Size bắp…) |
| `fnb_option_items` | `FnbOptionItem` | Từng lựa chọn trong pool + phụ thu |
| `fnb_item_slots` | `FnbComboSlot` | Combo gồm mấy khe, mỗi khe rút từ pool nào (min/max/mặc định) |
| `booking_fnb_options` | `BookingFnbOption` | Snapshot lựa chọn của từng khe trong đơn vé |
| `concession_sale_item_options` | `ConcessionSaleItemOption` | Snapshot lựa chọn của từng khe trong đơn quầy |

> Điểm mấu chốt: min/max **rời khỏi pool**, chuyển về từng khe; mọi lựa chọn khi bán đều
> snapshot (`slot_label_snapshot`, `option_name_snapshot`, `surcharge_snapshot`) để hóa đơn
> cũ bất biến khi menu đổi.

### 2.2. Nhóm còn lại

| Bảng | Entity | Vai trò | Mốc |
|---|---|---|---|
| `seat_incidents` | `SeatIncident` | Xử lý sự cố / đổi ghế đền bù (repoint `BookingSeat.seat_id` tại chỗ, đền bằng voucher `COMP_*`) | 14/08/2026 |
| `point_transactions` | `PointTransaction` | Sổ cái điểm tích lũy (cộng/trừ + số dư sau giao dịch) | — |
| `user_permission_overrides` | `UserPermissionOverride` | Ghi đè quyền ALLOW/DENY theo từng tài khoản | — |
| `approval_requests` | `ApprovalRequest` | Yêu cầu phê duyệt sửa sai (nay chỉ còn `FNB_VOID`); **không có FK**, toàn cột Integer thô | — |
| `promo_email_log` | `PromoEmailLog` | Chống gửi trùng email chiến dịch; **không có FK** | — |

---

## 3. Cột bị GỠ khỏi ERD (Ver8 có, code không còn)

| Bảng | Cột | Lý do |
|---|---|---|
| `seat_types` | `price_modifier` | Flat Pricing V4 — loại ghế không còn tác động giá |
| `movies` | `base_price` | nt — giá lấy từ `pricing_rules` |
| `movie_formats` | `is_fixed_price` | nt — không còn nhánh giá cố định |

Kéo theo phải sửa cả phần **văn bản mô tả** trong MoTa Ver8:
- §13 `movie_formats`: bỏ câu "khi `is_fixed_price=true` thì giá lấy từ `special_seat_prices`".
- §34 `special_seat_prices`: xóa toàn bộ mục.
- §11 `seat_types`: đổi mô tả "hệ số/điều chỉnh giá" → chỉ còn tên + màu hiển thị.

---

## 4. Cột THÊM (code có, Ver8 thiếu) — 16 bảng

| Bảng | Cột thêm | Ghi chú |
|---|---|---|
| `users` | `must_change_password` | Buộc đổi mật khẩu lần đăng nhập kế |
| `customers` | `lifetime_points` | Điểm trọn đời để xét hạng, không bị trừ khi tiêu |
| `staffs` | `created_at`, `updated_at` | |
| `cinemas` | `opening_time`, `closing_time` | Chặn xếp suất ngoài giờ mở cửa |
| `seats` | `label`, `custom_label`, `seat_status`, `cell_kind` | Đại tu Seat Map: nhãn thật + sửa tay + khóa ghế MAINTENANCE + ô lối đi |
| `showtimes` | `layout_data` | Snapshot sơ đồ ghế lúc mở suất |
| `bookings` | `sold_by`, `payment_gateway_ref`, `channel`, `pos_terminal_id`, `expires_at` | `sold_by` thay vai trò ghi vết của `staff_schedule_id` đã gỡ |
| `booking_fnbs` | `item_name_snapshot` | |
| `concession_sales` | `sold_by`, `cinema_id` | Ghi vết người bán + Cinema Scoping |
| `concession_sale_items` | `item_name_snapshot` | |
| `fnb_items` | `is_deleted` | Xóa mềm để hóa đơn cũ vẫn tra được |
| `promotions` | `max_ticket_quantity`, `max_discount_amount`, `campaign_sent_at`, `campaign_sent_count` | Trần giảm giá + số liệu chiến dịch email |
| `vouchers` | `used_at` | |
| `support_tickets` | `phone`, `admin_reply`, `replied_at` | Trả lời ngay trong phiếu |
| `pricing_rules` | `room_type` | STANDARD / SUPERPLEX / CINE_COMFORT — trục mới của ma trận giá phẳng |
| `movies` | (sắp xếp lại thứ tự cột cho dễ đọc) | Không thêm/bớt ngoài `base_price` |

---

## 5. Ràng buộc UK Ver8 chưa ghi

| Bảng | UK |
|---|---|
| `rooms` | (`cinema_id`, `name`) |
| `fnb_option_items` | (`group_id`, `name`) |
| `user_permission_overrides` | (`user_id`, `feature`, `action`) |
| `promo_email_log` | (`promotion_id`, `customer_id`) |

---

## 6. Lệch nội bộ giữa 2 file Ver8

`ERD_Ver8.md` (Mermaid) **có** bảng `MOVIE_CATEGORIES`, nhưng `ERD_Ver8_MoTa.md` **không**
liệt kê ở cả PHẦN A lẫn PHẦN B — nên tổng của MoTa chỉ ra 37 thay vì 38.
Ver9 đã bổ sung đầy đủ ở cả hai file, kèm ghi chú bảng này trùng vai trò với
`movie_genre_mapping` và nên được hợp nhất.

---

## 7. Quan hệ THÊM MỚI trong Ver9 (20)

```
STAFFS                ||--o{ BOOKINGS                        (sold_by)
STAFFS                ||--o{ CONCESSION_SALES                (sold_by)
CINEMAS               ||--o{ CONCESSION_SALES                (cinema_id)
CUSTOMERS             ||--o{ POINT_TRANSACTIONS
USERS                 ||--o{ USER_PERMISSION_OVERRIDES
FNB_OPTION_GROUPS     ||--o{ FNB_OPTION_ITEMS
FNB_ITEMS             ||--o{ FNB_ITEM_SLOTS
FNB_OPTION_GROUPS     ||--o{ FNB_ITEM_SLOTS
FNB_OPTION_ITEMS      ||--o{ FNB_ITEM_SLOTS                  (default_option_item_id)
BOOKING_FNBS          ||--o{ BOOKING_FNB_OPTIONS
FNB_OPTION_GROUPS     ||--o{ BOOKING_FNB_OPTIONS
FNB_OPTION_ITEMS      ||--o{ BOOKING_FNB_OPTIONS
CONCESSION_SALE_ITEMS ||--o{ CONCESSION_SALE_ITEM_OPTIONS
FNB_OPTION_GROUPS     ||--o{ CONCESSION_SALE_ITEM_OPTIONS
FNB_OPTION_ITEMS      ||--o{ CONCESSION_SALE_ITEM_OPTIONS
BOOKINGS              ||--o{ SEAT_INCIDENTS
SHOWTIMES             ||--o{ SEAT_INCIDENTS
SEATS                 ||--o{ SEAT_INCIDENTS                  (old_seat_id, new_seat_id)
VOUCHERS              ||--o{ SEAT_INCIDENTS
STAFFS                ||--o{ SEAT_INCIDENTS                  (handled_by)
CINEMAS               ||--o{ SEAT_INCIDENTS
```

Thêm 2 quan hệ **mềm** mới (nét đứt): `promotions ⇢ promo_email_log`,
`customers ⇢ promo_email_log`.

---

## 8. Nội dung mô tả cần viết lại (không chỉ là cột)

| Vị trí trong Ver8 | Vấn đề | Ver9 đã sửa |
|---|---|---|
| Tiêu đề B.6 "Bắp nước (F&B) & **kho**" | Kho/BOM đã gỡ hoàn toàn 11/07/2026 | Đổi thành "Bắp nước (F&B) — Option Pool + Combo Slot" |
| §27 `fnb_items` — "hoặc **nguyên liệu**" | Không còn vai trò nguyên liệu, tồn kho vô hạn | Chỉ còn combo / món lẻ |
| §10 `seats` — "`row_char`+`col_num` là nhãn ghế" | Nhãn thật nằm ở cột `label` | Mô tả lại theo mô hình lưới `matrix_row × matrix_col` + `cell_kind` |
| §18 `bookings` | Thiếu ghi vết người bán | Bổ sung `sold_by` + ghi chú Strict Cinema Scoping |
| §33 `pricing_rules` | Thiếu trục `room_type` | Mô tả lại theo Flat Pricing V4 |
| Nhóm A.7 "Vận hành ca làm & cấu hình" | Phân hệ ca đã gỡ | Đổi thành "Cấu hình & danh mục độc lập" |
| Ghi chú quan hệ mềm | Thiếu `promo_email_log`, `approval_requests` | Đã bổ sung |

---

## 9. Bộ file Ver9

| File | Nội dung |
|---|---|
| [`ERD_Ver9.drawio.xml`](ERD_Ver9.drawio.xml) | Bản vẽ drawio — theo đúng bảng màu/format của `ERD_View.drawio.xml` |
| [`ERD_Ver9.md`](ERD_Ver9.md) | Sơ đồ Mermaid + ghi chú quan hệ |
| [`ERD_Ver9_MoTa.md`](ERD_Ver9_MoTa.md) | Từ điển dữ liệu đầy đủ 44 bảng + phụ lục FK |
| `erd-diff-v8-v9.md` | File này |

> Ver8 được **giữ nguyên** làm mốc lịch sử cho báo cáo đồ án, không sửa.
