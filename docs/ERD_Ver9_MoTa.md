# DevCine — Mô tả & phân tích ERD (Ver9)

> Tài liệu từ điển dữ liệu (data dictionary) cho cơ sở dữ liệu DevCine, đồng bộ với
> **43 entity** trong `devcine-backend/src/main/java/com/devcine/backend/entity/` tại ngày **2026-08-16**.
> Sơ đồ trực quan: [`ERD_Ver9.drawio.xml`](ERD_Ver9.drawio.xml) · Sơ đồ Mermaid: [`ERD_Ver9.md`](ERD_Ver9.md)
> · Khác biệt so với Ver8: [`erd-diff-v8-v9.md`](erd-diff-v8-v9.md).
>
> Quy ước: **PK** = khóa chính · **FK** = khóa ngoại · **UK** = ràng buộc duy nhất (unique).
> Kiểu dữ liệu mô tả ở mức logic (int, string=varchar, text, decimal, double, datetime, date, time, boolean).

---

# PHẦN A — Danh sách tất cả bảng trong ERD

Tổng cộng **44 bảng** (43 entity + bảng nối `movie_genre_mapping` do `@ManyToMany` sinh ra),
chia theo 7 nhóm chức năng.

## A.1. Người dùng & phân quyền (9 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 1 | `roles` | Vai trò + ma trận quyền (ADMIN, MANAGER, STAFF, CUSTOMER) |
| 2 | `users` | Tài khoản đăng nhập chung cho mọi loại người dùng |
| 3 | `customers` | Hồ sơ khách hàng (mở rộng 1-1 từ `users`) |
| 4 | `staffs` | Hồ sơ nhân viên (mở rộng 1-1 từ `users`), gắn rạp & quản lý |
| 5 | `user_permission_overrides` | Ghi đè quyền theo từng tài khoản (ALLOW/DENY) đè lên ma trận vai trò |
| 6 | `audit_logs` | Nhật ký thao tác hệ thống (ai làm gì, bảng nào, IP) |
| 7 | `notifications` | Thông báo gửi tới khách hàng |
| 8 | `support_tickets` | Phiếu hỗ trợ/khiếu nại của khách |
| 9 | `point_transactions` | Sổ cái điểm tích lũy (cộng/trừ, số dư sau giao dịch) |

## A.2. Rạp – Phòng – Ghế (4 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 10 | `cinemas` | Cụm rạp chiếu phim |
| 11 | `rooms` | Phòng chiếu thuộc một rạp |
| 12 | `seats` | Ghế vật lý trong phòng (ô của lưới sơ đồ ghế) |
| 13 | `seat_types` | Loại ghế (thường, VIP, đôi…) + màu hiển thị |

## A.3. Phim & phân loại (6 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 14 | `movies` | Thông tin phim |
| 15 | `movie_formats` | Định dạng chiếu (2D, 3D…) và phụ thu |
| 16 | `categories` | Danh mục/thể loại phim |
| 17 | `movie_genre_mapping` | Bảng nối N-N phim ↔ thể loại (**cơ chế đang dùng**) |
| 18 | `movie_categories` | Bảng nối N-N phim ↔ thể loại (di sản, trùng vai trò) |
| 19 | `age_ratings` | Danh mục phân loại độ tuổi (P, K, T13, T16, T18…) |

## A.4. Suất chiếu, đặt vé & vé (5 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 20 | `showtimes` | Suất chiếu (phim + phòng + định dạng + giờ) |
| 21 | `bookings` | Đơn đặt vé gắn với một suất chiếu |
| 22 | `booking_seats` | Từng ghế trong một đơn đặt vé (chốt giá) |
| 23 | `tickets` | Vé điện tử phát hành cho mỗi ghế đã đặt |
| 24 | `reviews` | Đánh giá/bình luận phim của khách |

## A.5. Bắp nước — F&B (9 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 26 | `fnb_items` | Món bắp nước / combo |
| 27 | `fnb_option_groups` | **Option Pool** — nhóm lựa chọn (Nước ngọt, Size bắp…) |
| 28 | `fnb_option_items` | Từng lựa chọn trong pool + phụ thu |
| 29 | `fnb_item_slots` | **Combo Slot** — combo gồm mấy khe, mỗi khe rút từ pool nào |
| 30 | `booking_fnbs` | Combo bắp nước mua kèm đơn đặt vé |
| 31 | `booking_fnb_options` | Lựa chọn cụ thể của từng khe (snapshot) trong đơn vé |
| 32 | `concession_sales` | Đơn bán bắp nước tại quầy (không gắn suất chiếu) |
| 33 | `concession_sale_items` | Dòng món trong đơn bán bắp nước độc lập |
| 34 | `concession_sale_item_options` | Lựa chọn của từng khe (snapshot) trong đơn quầy |

## A.6. Khuyến mãi & nội dung hiển thị (5 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 35 | `promotions` | Chương trình khuyến mãi / khuôn mẫu voucher |
| 36 | `vouchers` | Voucher cụ thể phát cho từng khách |
| 37 | `promo_email_log` | Chống gửi trùng email chiến dịch khuyến mãi |
| 38 | `promo_articles` | Tin khuyến mãi biên tập (nội dung hiển thị) |
| 39 | `banners` | Banner trang chủ (ảnh hoặc dựng theo phim) |

## A.7. Cấu hình & danh mục độc lập (5 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 40 | `pricing_rules` | Ma trận giá phẳng theo loại phòng/ngày/giờ/đối tượng |
| 41 | `holidays` | Ngày lễ (áp giá nền HOLIDAY) |
| 42 | `faqs` | Câu hỏi thường gặp |
| 43 | `system_settings` | Cấu hình hệ thống dạng key-value |
| 44 | `approval_requests` | Yêu cầu phê duyệt sửa sai (hiện chỉ còn loại `FNB_VOID`) |

> **Ghi chú quan trọng — những bảng đã bị GỠ khỏi hệ thống:**
> - Ví điện tử: `wallets`, `wallet_transactions` (gỡ từ Ver8)
> - Kho & định mức BOM: `bom_recipes`, `inventory_logs`, `cinema_inventory` (gỡ 11/07/2026 — tồn kho VÔ HẠN)
> - Ca làm việc & bàn giao ca: `shifts`, `staff_schedules`, `shift_handovers` (gỡ 01/08/2026)
> - Giá cố định theo loại ghế: `special_seat_prices` (gỡ theo Flat Pricing V4)
>
> **Quan hệ "mềm" (không khai báo FK, chỉ là cột số/chuỗi thô):** `movies.age_rating` ↔ `age_ratings.code`,
> `banners.movie_id`, `promotions.applicable_movie_id`, `promo_email_log.promotion_id`/`customer_id`,
> và toàn bộ cột tham chiếu của `approval_requests`.

---

# PHẦN B — Phân tích chi tiết từng bảng

## B.1. Người dùng & phân quyền

### 1. `roles` (Role)
Định nghĩa vai trò và quyền hạn. Quyền lưu dạng JSON trong `permissions_matrix`, được service đọc
để kiểm tra phân quyền theo cặp (feature, action). Cờ phiên bản ma trận hiện tại: `PERMISSION_MATRIX_V6`.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh vai trò |
| name | string | UK | Tên vai trò (ADMIN, MANAGER, STAFF, CUSTOMER) |
| permissions_matrix | text | | Chuỗi JSON mô tả quyền cho từng chức năng |

### 2. `users` (User)
Tài khoản đăng nhập **chung** cho cả khách và nhân viên. Phân biệt loại người dùng qua `role_id`
và qua bản ghi mở rộng trong `customers`/`staffs`.

| Cột | Kiểu | Khóa | Ràng buộc | Ý nghĩa |
|---|---|---|---|---|
| id | int | PK | | Định danh người dùng |
| username | string | UK | not null | Tên đăng nhập |
| password_hash | string | | not null | Mật khẩu đã băm (không bao giờ trả ra DTO) |
| full_name | string | | not null | Họ tên hiển thị |
| avatar_url | string | | | Ảnh đại diện (Cloudinary) |
| email | string | UK | not null | Email (dùng cho OTP reset mật khẩu) |
| phone | string | | | Số điện thoại |
| role_id | int | FK→roles | not null | Vai trò của tài khoản |
| is_active | boolean | | not null, mặc định true | Tài khoản còn hoạt động hay bị khóa |
| must_change_password | boolean | | not null, mặc định false | Buộc đổi mật khẩu ở lần đăng nhập kế (tài khoản do admin tạo) |
| created_at | datetime | | not null | Thời điểm tạo |

### 3. `customers` (Customer)
Hồ sơ khách hàng, **chia sẻ khóa chính** với `users` qua `@MapsId` (quan hệ 1-1, `user_id` vừa là PK vừa là FK).

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| user_id | int | PK, FK→users | Đồng thời là id của user tương ứng |
| dob | date | | Ngày sinh (dùng kiểm tra độ tuổi xem phim) |
| id_card | string | | Số CMND/CCCD |
| membership_tier | string | | Hạng thành viên |
| loyalty_points | int | | Điểm **khả dụng** hiện tại (mặc định 0) |
| lifetime_points | int | | Điểm **tích lũy trọn đời** — dùng để xét hạng, không bị trừ khi tiêu điểm |

### 4. `staffs` (Staff)
Hồ sơ nhân viên, cũng 1-1 với `users` qua `@MapsId`. `cinema_id` là **nền tảng của Strict Cinema Scoping**:
STAFF/MANAGER thiếu cột này hoặc thao tác chéo cụm rạp sẽ bị chặn 403.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| user_id | int | PK, FK→users | Đồng thời là id user của nhân viên |
| staff_code | string | UK | Mã nhân viên |
| cinema_id | int | FK→cinemas | Rạp nhân viên trực thuộc (đưa vào JWT để scoping) |
| manager_id | int | FK→staffs | Quản lý trực tiếp (tự tham chiếu) |
| created_at | datetime | | Thời điểm tạo hồ sơ |
| updated_at | datetime | | Thời điểm cập nhật gần nhất |

### 5. `user_permission_overrides` (UserPermissionOverride)
Ghi đè quyền cho **một tài khoản cụ thể**, đè lên `roles.permissions_matrix`.
Cho phép nới hoặc siết quyền lẻ mà không phải đổi vai trò.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh |
| user_id | int | FK→users, UK¹ | Tài khoản được ghi đè |
| feature | string | UK¹ | Mã chức năng (vd `pos_ticketing`, `fnb_menu`, `incident_handling`) |
| action | string | UK¹ | Hành động (`view`, `edit`, `handle`…) |
| effect | string | | `ALLOW` hoặc `DENY` |
| updated_at | datetime | not null | Thời điểm cập nhật |

¹ Ràng buộc duy nhất theo bộ ba (`user_id`, `feature`, `action`).

### 6. `audit_logs` (AuditLog)
Nhật ký kiểm toán mọi thao tác quan trọng.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh log |
| user_id | int | FK→users | Người thực hiện thao tác |
| action | string | not null | Hành động (CREATE/UPDATE/DELETE…) |
| target_table | string | | Bảng bị tác động |
| ip_address | string | | Địa chỉ IP nguồn |
| timestamp | datetime | not null | Thời điểm xảy ra |

### 7. `notifications` (Notification)
Thông báo đẩy tới khách hàng (đặt vé thành công, nhắc suất chiếu, khuyến mãi, hệ thống).

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh thông báo |
| customer_id | int | FK→customers, not null | Khách nhận thông báo |
| title | string | not null | Tiêu đề |
| message | text | | Nội dung |
| type | string | | BOOKING / REMINDER / PROMOTION / SYSTEM |
| is_read | boolean | not null, mặc định false | Đã đọc hay chưa |
| created_at | datetime | not null | Thời điểm tạo |

### 8. `support_tickets` (SupportTicket)
Phiếu hỗ trợ khách gửi lên, có thể giao cho một nhân viên xử lý và trả lời trực tiếp trong phiếu.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh phiếu |
| customer_id | int | FK→customers, not null | Khách tạo phiếu |
| assigned_to_staff | int | FK→staffs | Nhân viên được giao xử lý |
| issue_type | string | | Loại vấn đề |
| description | text | | Mô tả chi tiết |
| phone | string | | Số điện thoại liên hệ khách để lại |
| status | string | | Trạng thái xử lý |
| admin_reply | text | | Nội dung trả lời của quản trị |
| replied_at | datetime | | Thời điểm trả lời |
| created_at | datetime | not null | Thời điểm tạo |

### 9. `point_transactions` (PointTransaction)
Sổ cái điểm tích lũy — mỗi lần cộng/trừ điểm ghi một dòng, kèm số dư sau giao dịch để đối soát.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh giao dịch điểm |
| customer_id | int | FK→customers, not null | Khách sở hữu điểm |
| points | int | not null | Số điểm biến động (dương = cộng, âm = trừ) |
| type | string | not null | EARN / REDEEM / ADJUST… |
| source | string | | Nguồn phát sinh (BOOKING, CONCESSION, PROMOTION…) |
| ref_code | string | | Mã tham chiếu (`booking_code` / `sale_code`) |
| balance_after | int | | Số dư điểm sau giao dịch |
| note | text | | Ghi chú |
| created_at | datetime | not null | Thời điểm phát sinh |

---

## B.2. Rạp – Phòng – Ghế

### 10. `cinemas` (Cinema)
Cụm rạp. Nhiều cột mở rộng phục vụ trang khách (bản đồ, tiện ích, ảnh, giờ mở cửa).

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh rạp |
| name | string | not null | Tên rạp |
| address | string | | Địa chỉ |
| city | string | | Tỉnh/thành |
| district | string | | Quận/huyện |
| type | string | | Loại rạp |
| hotline | string | | Số hotline |
| rooms | int | | Số phòng (thông tin tổng quan) |
| image_url | string | | Ảnh rạp |
| description | text | | Mô tả |
| latitude | double | | Vĩ độ (bản đồ) |
| longitude | double | | Kinh độ (bản đồ) |
| amenities | text | | Tiện ích, phân tách bởi dấu phẩy |
| status | string | | ACTIVE / MAINTENANCE / CLOSED |
| opening_time | time | | Giờ mở cửa (chặn xếp suất ngoài khung) |
| closing_time | time | | Giờ đóng cửa |
| manager_id | int | FK→staffs | Quản lý rạp |

### 11. `rooms` (Room)
Phòng chiếu thuộc một rạp. `matrix_row` × `matrix_col` là **khung chân lý** của sơ đồ ghế —
mọi ghế nằm ngoài khung này bị coi là ghế "ma" và sẽ bị dọn.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh phòng |
| cinema_id | int | FK→cinemas, not null | Rạp chứa phòng |
| name | string | UK¹, not null | Tên phòng |
| type | string | | Loại phòng: STANDARD / SUPERPLEX / CINE_COMFORT (dùng cho Flat Pricing) |
| status | string | | Trạng thái hoạt động |
| turnaround_time_mins | int | | Thời gian dọn phòng giữa 2 suất (phút) |
| matrix_row | int | | Số hàng của lưới sơ đồ ghế |
| matrix_col | int | | Số cột của lưới sơ đồ ghế |

¹ Ràng buộc duy nhất theo cặp (`cinema_id`, `name`).

### 12. `seats` (Seat)
**Một ô của lưới sơ đồ ghế** — có thể là ghế thật hoặc lối đi, phân biệt bằng `cell_kind`.
Nhãn hiển thị lấy từ `label` (suy live nên đổi ghế không cần in lại QR).

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh ghế |
| room_id | int | FK→rooms, not null | Phòng chứa ghế |
| row_char | string | not null | Ký tự hàng (A, B, C…) |
| col_num | int | not null | Số thứ tự cột |
| seat_type_id | int | FK→seat_types, not null | Loại ghế |
| is_active | boolean | not null, mặc định true | Ô còn dùng được hay không |
| label | string | | **Nhãn ghế hiển thị** (vd A1) — nguồn chân lý cho vé/email/POS |
| custom_label | boolean | mặc định false | true = nhãn do người dùng sửa tay (đánh số tự động sẽ không ghi đè) |
| seat_status | string | mặc định AVAILABLE | AVAILABLE / MAINTENANCE — `MAINTENANCE` chặn bán ở **mọi suất sau** |
| grid_row | int | | Tọa độ hàng trong lưới vẽ |
| grid_col | int | | Tọa độ cột trong lưới vẽ |
| cell_kind | string | mặc định SEAT | SEAT = ghế thật · AISLE = lối đi (không bán, bỏ qua khi đánh số) |

### 13. `seat_types` (SeatType)
Loại ghế và màu hiển thị trên sơ đồ. **Không còn cột chỉnh giá** — giá đã chuyển sang mô hình phẳng.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh loại ghế |
| name | string | UK | Tên loại (Thường, VIP, Đôi…) |
| color_code | string | | Mã màu hiển thị trên sơ đồ |

---

## B.3. Phim & phân loại

### 14. `movies` (Movie)
Bảng phim. Quan hệ thể loại đi qua `movie_genre_mapping`.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh phim |
| title | string | not null | Tên phim |
| slug | string | UK | Định danh URL thân thiện |
| duration_mins | int | | Thời lượng (phút) |
| age_rating | string | | Mã phân loại độ tuổi (chuỗi, **không** FK tới `age_ratings`) |
| release_date | date | | Ngày khởi chiếu |
| start_date | date | | Ngày bắt đầu (lịch nội bộ) |
| end_date | date | | Ngày kết thúc chiếu |
| status | string | | Sắp chiếu / đang chiếu / ngừng |
| country | string | | Quốc gia sản xuất |
| rating | string | | Điểm đánh giá hiển thị |
| rating_count | int | | Số lượt đánh giá |
| poster_base64 | text | | Ảnh poster |
| banner_base64 | text | | Ảnh banner |
| show_on_banner | boolean | | Có hiển thị ở banner trang chủ không |
| trailer_url | string | | Link trailer (dùng cho modal trailer ở màn Lịch chiếu) |
| format | string | | Định dạng (chuỗi mô tả) |
| supported_formats | string | | Các định dạng hỗ trợ |
| title_vietnamese | string | | Tên tiếng Việt |
| production_year | int | | Năm sản xuất |
| language | string | | Ngôn ngữ |
| original_language | string | | Ngôn ngữ gốc |
| version_type | string | | Loại bản chiếu (lồng tiếng/phụ đề…) |
| distributor | string | | Nhà phát hành |
| director | string | | Đạo diễn |
| cast_members | text | | Diễn viên |
| description | text | | Mô tả nội dung |
| internal_notes | text | | Ghi chú nội bộ |

### 15. `movie_formats` (MovieFormat)
Định dạng chiếu và quy tắc phụ thu. Phụ thu **cộng thẳng** vào giá phẳng của `pricing_rules`.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh định dạng |
| name | string | UK | Tên (2D, 3D…) |
| description | string | | Mô tả |
| surcharge | decimal | | Phụ thu ngày thường |
| weekend_surcharge | decimal | | Phụ thu cuối tuần & lễ |

### 16. `categories` (Category)
Danh mục/thể loại phim.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh thể loại |
| name | string | UK | Tên thể loại |
| description | string | | Mô tả |

### 17. `movie_genre_mapping`
Bảng nối **N-N** giữa phim và thể loại, sinh tự động từ `@ManyToMany` của `Movie.genres`.
Đây là cơ chế thể loại **đang thực sự được code dùng**.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| movie_id | int | FK→movies | Phim |
| category_id | int | FK→categories | Thể loại |

### 18. `movie_categories` (MovieCategory)
Bảng nối N-N phim ↔ thể loại khai báo tường minh bằng entity + `@IdClass`.
**Trùng vai trò với `movie_genre_mapping`** — là di sản còn sót lại, nên hợp nhất về một cơ chế.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| movie_id | int | PK, FK→movies | Phim |
| category_id | int | PK, FK→categories | Thể loại |

### 19. `age_ratings` (AgeRating)
Danh mục phân loại độ tuổi để màn quản lý phim chọn lựa. Hiện **đứng độc lập** —
`movies.age_rating` chỉ lưu chuỗi mã, chưa ràng buộc FK.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh |
| code | string | UK | Mã (P, K, T13, T16, T18…) |
| name | string | not null | Tên hiển thị |
| description | string | | Diễn giải |

---

## B.4. Suất chiếu, đặt vé, vé & sự cố

### 20. `showtimes` (Showtime)
Một suất chiếu = phim + phòng + định dạng + khung giờ.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh suất |
| movie_id | int | FK→movies, not null | Phim chiếu |
| room_id | int | FK→rooms, not null | Phòng chiếu |
| format_id | int | FK→movie_formats, not null | Định dạng chiếu |
| start_time | datetime | not null | Giờ bắt đầu |
| end_time | datetime | not null | Giờ kết thúc |
| status | string | | Trạng thái suất |
| layout_data | text | | **Snapshot sơ đồ ghế** tại thời điểm mở suất (chống lệch khi phòng bị sửa layout sau đó) |

### 21. `bookings` (Booking)
Đơn đặt vé gắn một suất chiếu. `total_price` là tạm tính, `final_price` là sau giảm giá/voucher.
Việc **giữ ghế tạm thời** không lưu ở đây — xử lý real-time qua WebSocket/STOMP.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh đơn |
| customer_id | int | FK→customers | Khách đặt (null nếu bán quầy không gắn thành viên) |
| showtime_id | int | FK→showtimes, not null | Suất chiếu |
| voucher_id | int | FK→vouchers | Voucher áp dụng (nếu có) |
| sold_by | int | FK→staffs | **Nhân viên bán** (ghi vết thay cho phân hệ ca đã gỡ) |
| printed_by | int | FK→staffs | Nhân viên đã in vé giấy cho đơn |
| total_price | decimal | not null | Tổng tiền trước giảm |
| final_price | decimal | not null | Tổng tiền phải trả sau giảm |
| payment_method | string | | VNPAY (khách) / CASH, CARD, TRANSFER (POS) |
| payment_gateway_ref | string | | Mã tham chiếu giao dịch cổng thanh toán (VNPAY) |
| status | string | | Trạng thái đơn (gộp thanh toán + đặt chỗ) |
| booking_code | string | UK | Mã đơn để tra cứu (nội dung mã QR — 1 QR đại diện cả đơn) |
| channel | string | | Kênh bán: ONLINE / POS (dùng cho anti-fraud vé CHILD/SENIOR) |
| pos_terminal_id | string | | Định danh máy POS đã tạo đơn |
| created_at | datetime | not null | Thời điểm tạo |
| printed_at | datetime | | Thời điểm quét QR & in vé giấy tại quầy (null = chưa in) |
| expires_at | datetime | | Hạn giữ chỗ chờ thanh toán — quá hạn thì đơn bị hủy tự động |

### 22. `booking_seats` (BookingSeat)
Mỗi ghế trong một đơn đặt vé, **chốt giá** tại thời điểm đặt.
Khi xử lý sự cố đổi ghế, hệ thống **repoint `seat_id` tại chỗ** → giữ nguyên Ticket/QR/giá,
nhãn ghế suy live nên in lại và email tự đúng.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh dòng ghế |
| booking_id | int | FK→bookings, not null | Đơn chứa ghế |
| seat_id | int | FK→seats, not null | Ghế được đặt (**bị repoint khi đổi ghế đền bù**) |
| price_snapshot | decimal | | Giá ghế chốt tại thời điểm đặt |
| ticket_type | string | | Đối tượng vé: ADULT / STUDENT / CHILD / SENIOR |
| status | string | | Trạng thái ghế trong đơn |

### 23. `tickets` (Ticket)
Vé điện tử phát hành **cho từng ghế** đã đặt (quan hệ 1-1 với `booking_seats`).

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh vé |
| booking_seat_id | int | FK→booking_seats, UK (1-1) | Ghế tương ứng của vé |
| qr_code | string | | Mã QR soát vé |
| is_checked_in | boolean | not null, mặc định false | Đã soát vé vào chưa |
| is_age_verified | boolean | mặc định false | Đã kiểm tra độ tuổi chưa |
| checked_in_by | int | FK→staffs | Nhân viên soát vé |
| check_in_time | datetime | | Thời điểm soát vé |

### 24. `reviews` (Review)
Đánh giá phim của khách; có thể gắn vé đã xem để xác thực, và cờ ẩn để kiểm duyệt.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh đánh giá |
| customer_id | int | FK→customers, not null | Khách đánh giá |
| movie_id | int | FK→movies, not null | Phim được đánh giá |
| ticket_id | int | FK→tickets | Vé xác thực đã xem (nếu có) |
| rating | int | not null | Số sao |
| comment | text | | Nội dung bình luận |
| hidden | boolean | mặc định false | true = bị admin ẩn khỏi trang công khai |
| created_at | datetime | not null | Thời điểm tạo |


---

## B.5. Bắp nước (F&B) — mô hình Option Pool + Combo Slot

> Từ 09/08/2026, F&B chuyển sang mô hình chuẩn CGV/Lotte: một **combo** (`fnb_items`) gồm
> nhiều **khe** (`fnb_item_slots`), mỗi khe rút lựa chọn từ một **pool** (`fnb_option_groups`).
> Khi bán, hệ thống **snapshot** nhãn khe + tên lựa chọn + phụ thu vào bảng `*_options`
> để hóa đơn cũ không đổi khi menu đổi. Validate số lựa chọn tối thiểu/tối đa nằm ở
> server-side (`FnbOptionValidator`).

### 26. `fnb_items` (FnbItem)
Món bắp nước hoặc combo. **Không còn vai trò "nguyên liệu"** — kho/BOM đã gỡ, tồn kho vô hạn.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh món |
| name | string | not null | Tên món |
| type | string | | Phân loại (combo / món lẻ) |
| price | decimal | not null | Giá bán |
| image_url | string | | Ảnh món |
| description | string | | Mô tả |
| is_active | boolean | not null, mặc định true | Còn bán/hiển thị cho khách không |
| is_deleted | boolean | not null, mặc định false | Xóa mềm — giữ lại để hóa đơn cũ vẫn tra được |

### 27. `fnb_option_groups` (FnbOptionGroup)
**Option Pool** — nhóm lựa chọn dùng chung (vd "Nước ngọt", "Size bắp").
Không còn cột min/max ở đây; giới hạn số lựa chọn thuộc về từng khe.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh pool |
| name | string | UK, not null | Tên pool |

### 28. `fnb_option_items` (FnbOptionItem)
Từng lựa chọn trong pool, kèm phụ thu cộng vào giá combo.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh lựa chọn |
| group_id | int | FK→fnb_option_groups, UK¹, not null | Pool chứa lựa chọn |
| name | string | UK¹, not null | Tên lựa chọn (Coca, Pepsi, Size L…) |
| surcharge_price | decimal | not null, mặc định 0 | Phụ thu khi chọn |

¹ Ràng buộc duy nhất theo cặp (`group_id`, `name`).

### 29. `fnb_item_slots` (FnbComboSlot)
**Combo Slot** — định nghĩa combo gồm mấy khe, mỗi khe rút từ pool nào và cho chọn bao nhiêu.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh khe |
| fnb_item_id | int | FK→fnb_items, not null | Combo chứa khe |
| option_group_id | int | FK→fnb_option_groups, not null | Pool mà khe này rút lựa chọn |
| default_option_item_id | int | FK→fnb_option_items | Lựa chọn mặc định của khe |
| slot_label | string | not null | Nhãn khe hiển thị (vd "Chọn nước 1") |
| display_order | int | not null, mặc định 0 | Thứ tự khe trong combo |
| min_choices | int | not null, mặc định 1 | Số lựa chọn tối thiểu |
| max_choices | int | not null, mặc định 1 | Số lựa chọn tối đa |
| is_required | boolean | not null, mặc định true | Khe bắt buộc chọn hay không |

### 30. `booking_fnbs` (BookingFnb)
Combo bắp nước mua kèm theo đơn đặt vé.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh dòng |
| booking_id | int | FK→bookings, not null | Đơn chứa combo |
| fnb_item_id | int | FK→fnb_items, not null | Món/combo |
| item_name_snapshot | string | | Tên món chốt lúc mua (hóa đơn cũ không đổi khi menu đổi tên) |
| quantity | int | not null | Số lượng |
| price_snapshot | decimal | | Giá chốt tại thời điểm mua |

### 31. `booking_fnb_options` (BookingFnbOption)
Lựa chọn cụ thể của từng khe trong combo mua kèm vé — lưu **snapshot** để bất biến.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh |
| booking_fnb_id | int | FK→booking_fnbs, not null | Dòng combo tương ứng |
| option_group_id | int | FK→fnb_option_groups | Pool của khe |
| option_item_id | int | FK→fnb_option_items | Lựa chọn đã chọn |
| slot_label_snapshot | string | | Nhãn khe chốt lúc mua |
| option_name_snapshot | string | | Tên lựa chọn chốt lúc mua |
| surcharge_snapshot | decimal | | Phụ thu chốt lúc mua |

### 32. `concession_sales` (ConcessionSale)
Đơn bán bắp nước **độc lập tại quầy** — không gắn suất chiếu/ghế, tách khỏi `bookings`
để doanh thu phòng vé không bị sai lệch bởi khách vãng lai.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh đơn |
| sale_code | string | UK | Mã đơn |
| customer_id | int | FK→customers | Thành viên (null nếu khách vãng lai) |
| sold_by | int | FK→staffs | **Nhân viên bán** (ghi vết) |
| cinema_id | int | FK→cinemas | Cụm rạp bán (gán từ `staff.getCinema()`, phục vụ Cinema Scoping) |
| total_price | decimal | not null | Tổng tiền |
| payment_method | string | | Phương thức thanh toán |
| status | string | | Trạng thái đơn |
| created_at | datetime | not null | Thời điểm tạo |

### 33. `concession_sale_items` (ConcessionSaleItem)
Từng dòng món trong một đơn bán bắp nước độc lập.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh dòng |
| sale_id | int | FK→concession_sales, not null | Đơn chứa món |
| fnb_item_id | int | FK→fnb_items, not null | Món bán |
| item_name_snapshot | string | | Tên món chốt lúc bán |
| quantity | int | not null | Số lượng |
| price_snapshot | decimal | not null | Giá chốt tại thời điểm bán |

### 34. `concession_sale_item_options` (ConcessionSaleItemOption)
Lựa chọn của từng khe trong combo bán tại quầy — cấu trúc song song với `booking_fnb_options`.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh |
| sale_item_id | int | FK→concession_sale_items, not null | Dòng món tương ứng |
| option_group_id | int | FK→fnb_option_groups, not null | Pool của khe |
| option_item_id | int | FK→fnb_option_items, not null | Lựa chọn đã chọn |
| slot_label_snapshot | string | | Nhãn khe chốt lúc bán |
| option_name_snapshot | string | | Tên lựa chọn chốt lúc bán |
| surcharge_snapshot | decimal | | Phụ thu chốt lúc bán |

---

## B.6. Khuyến mãi & nội dung hiển thị

### 35. `promotions` (Promotion)
Định nghĩa chương trình khuyến mãi / khuôn mẫu voucher, gồm điều kiện áp dụng, giới hạn lượt dùng
và số liệu chiến dịch email. Các template mã `COMP_*` (COMP_FNB_COMBO / 50K / 100K / TICKET_FULL)
dành riêng cho **voucher đền bù sự cố**, dùng `discount_type` GIFT_* trị giá 0 để không lẫn vào giảm giá.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh khuyến mãi |
| code | string | UK | Mã code |
| name | string | | Tên hiển thị |
| description | string | | Mô tả ngắn |
| discount_type | string | not null | PERCENT / FIXED / GIFT_* |
| discount_value | decimal | not null | Giá trị giảm |
| start_date | datetime | | Bắt đầu hiệu lực |
| end_date | datetime | | Kết thúc hiệu lực |
| is_stackable | boolean | not null, mặc định false | Có cộng dồn với KM khác không |
| points_required | int | mặc định 0 | Điểm cần để đổi |
| allow_point_redemption | boolean | mặc định false | Cho khách tự đổi điểm lấy voucher |
| min_order_value | decimal | mặc định 0 | Giá trị đơn tối thiểu để áp |
| applicable_movie_id | int | | Chỉ áp cho phim này (cột số, **không** FK) |
| customer_eligibility | string | mặc định ALL | ALL / NEW_CUSTOMER |
| usage_limit | int | mặc định 0 | Tổng lượt được dùng (0 = không giới hạn) |
| used_count | int | mặc định 0 | Số lượt đã dùng |
| max_ticket_quantity | int | mặc định 0 | Số vé tối đa được hưởng KM trong một đơn (0 = không giới hạn) |
| max_discount_amount | decimal | mặc định 0 | Trần số tiền giảm (chặn giảm % vô hạn) |
| campaign_sent_at | datetime | | Thời điểm gửi chiến dịch email gần nhất |
| campaign_sent_count | int | mặc định 0 | Số email đã gửi cho chiến dịch |

### 36. `vouchers` (Voucher)
Voucher cụ thể phát cho một khách, sinh ra từ một `promotion`.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh voucher |
| customer_id | int | FK→customers, not null | Khách sở hữu |
| promotion_id | int | FK→promotions, not null | Khuyến mãi gốc |
| valid_until | datetime | | Hạn dùng |
| is_used | boolean | not null, mặc định false | Đã dùng chưa |
| used_at | datetime | | Thời điểm sử dụng |

### 37. `promo_email_log` (PromoEmailLog)
Nhật ký gửi email chiến dịch — ràng buộc duy nhất (`promotion_id`, `customer_id`)
đảm bảo **một khách chỉ nhận một email cho mỗi chiến dịch**, kể cả khi gửi lại.
Hai cột tham chiếu là **Integer thô, không khai báo FK**.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh |
| promotion_id | int | UK¹, not null | Chiến dịch (soft-ref → `promotions.id`) |
| customer_id | int | UK¹, not null | Khách nhận (soft-ref → `customers.user_id`) |
| sent_at | datetime | not null | Thời điểm gửi |

¹ Ràng buộc duy nhất theo cặp (`promotion_id`, `customer_id`), tên `uk_promo_email_customer`.

### 38. `promo_articles` (PromoArticle)
Tin khuyến mãi dạng **bài biên tập** để hiển thị cho khách (khác hẳn voucher/promotion).

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh bài |
| title | string | not null | Tiêu đề |
| description | string | | Mô tả ngắn (thẻ danh sách) |
| image_url | string | | Ảnh banner/thumbnail |
| content | text | | Nội dung chi tiết |
| start_date | date | | Bắt đầu hiển thị |
| end_date | date | | Kết thúc hiển thị |
| is_active | boolean | not null, mặc định true | Bật/tắt hiển thị |
| display_order | int | not null, mặc định 0 | Thứ tự sắp xếp |
| created_at | datetime | | Thời điểm tạo |

### 39. `banners` (Banner)
Banner trang chủ. Có thể là ảnh tải lên (`mode=IMAGE`) hoặc dựng từ thông tin phim (`mode=MOVIE`).

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh banner |
| title | string | | Tiêu đề |
| image_url | string | | Ảnh banner |
| mode | string | mặc định IMAGE | IMAGE = ảnh upload; MOVIE = dựng theo phim |
| movie_id | int | | Phim được chọn khi mode=MOVIE (cột số, **không** FK) |
| placement | string | | Vị trí hiển thị |
| start_date | datetime | | Bắt đầu hiển thị |
| end_date | datetime | | Kết thúc hiển thị |
| is_active | boolean | not null, mặc định true | Bật/tắt |
| display_order | int | | Thứ tự sắp xếp |
| link | string | | Liên kết khi bấm vào |

---

## B.7. Cấu hình & danh mục độc lập

### 40. `pricing_rules` (PricingRule)
**Ma trận giá phẳng (Flat Pricing V4, chuẩn Lotte)**. Giá vé tra theo bộ
(`room_type`, `day_type`, `time_slot`, `audience_type`), không cộng dồn hệ số loại ghế.
Khi nhiều rule cùng khớp, rule có `priority` cao hơn thắng.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh rule |
| name | string | not null | Tên rule |
| rule_type | string | not null | Loại rule (BASE_PRICE…) |
| day_type | string | | WEEKDAY / WEDNESDAY / WEEKEND / HOLIDAY / ALL |
| room_type | string | | STANDARD / SUPERPLEX / CINE_COMFORT / ALL |
| time_slot | string | | EARLY / BEFORE_17H / AFTER_17H / ALL |
| audience_type | string | | ADULT / STUDENT / CHILD / SENIOR / ALL |
| value | decimal | not null | Giá áp dụng |
| priority | int | mặc định 0 | Độ ưu tiên (cao hơn thắng) |
| active | boolean | mặc định true | Rule còn hiệu lực |
| start_date | datetime | | Bắt đầu hiệu lực |
| end_date | datetime | | Kết thúc hiệu lực |

### 41. `holidays` (Holiday)
Danh sách ngày lễ; suất chiếu rơi vào ngày này áp giá `day_type=HOLIDAY`.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh |
| holiday_date | date | UK | Ngày lễ |
| name | string | not null | Tên ngày lễ |

### 42. `faqs` (Faq)
Câu hỏi thường gặp hiển thị ở trang Hỗ trợ; màn quản trị là `adminOnly`.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh |
| category | string | not null | Nhóm câu hỏi |
| question | string | not null | Câu hỏi |
| answer | text | | Câu trả lời |
| display_order | int | | Thứ tự hiển thị |
| is_active | boolean | mặc định true | Bật/tắt |

### 43. `system_settings` (SystemSetting)
Cấu hình hệ thống dạng key-value, khóa chính là chính cái key.
Cũng là nơi giữ các cờ seed (vd `PERMISSION_MATRIX_V6`).

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| setting_key | string | PK | Tên cấu hình |
| setting_value | text | | Giá trị cấu hình |

### 44. `approval_requests` (ApprovalRequest)
Yêu cầu **phê duyệt sửa sai**. Sau khi bỏ đổi ghế `SEAT_MOVE`, thực tế chỉ còn loại `FNB_VOID`;
người duyệt xác định theo vai trò ADMIN/MANAGER (`ApprovalService.requireApprover`).
Toàn bộ cột tham chiếu là **Integer thô, không khai báo FK** — `ref_id` trỏ tới bảng nào là
tùy giá trị `type`.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh yêu cầu |
| type | string | not null | Loại yêu cầu (`FNB_VOID`) |
| ref_id | int | | Id bản ghi bị tác động (bảng nào tùy `type`) |
| ref_code | string | | Mã bản ghi (vd `sale_code`) |
| payload | text | | Dữ liệu chi tiết dạng JSON |
| summary | string | | Tóm tắt hiển thị trên danh sách duyệt |
| reason | text | | Lý do người yêu cầu đưa ra |
| status | string | not null | PENDING / APPROVED / REJECTED |
| cinema_id | int | | Cụm rạp phát sinh (soft-ref → `cinemas.id`) |
| requested_by_user_id | int | | Người yêu cầu (soft-ref → `users.id`) |
| requested_by_name | string | | Tên người yêu cầu (snapshot) |
| approved_by_user_id | int | | Người duyệt (soft-ref → `users.id`) |
| approved_by_name | string | | Tên người duyệt (snapshot) |
| decision_note | text | | Ghi chú khi duyệt/từ chối |
| created_at | datetime | not null | Thời điểm tạo yêu cầu |
| decided_at | datetime | | Thời điểm ra quyết định |

> **Lưu ý vận hành:** màn "Phê duyệt sửa sai" ở frontend **đã ẩn** (tab/route/nav được comment)
> — backend và nút yêu cầu hủy ở POS vẫn giữ nguyên. Bảng này do đó còn tồn tại nhưng ít dữ liệu.

---

# PHẦN C — Tổng hợp quan hệ khóa ngoại (FK)

## C.1. FK khai báo tường minh trong entity

| Bảng nguồn | Cột | → Bảng đích |
|---|---|---|
| users | role_id | roles |
| customers | user_id | users (1-1, @MapsId) |
| staffs | user_id | users (1-1, @MapsId) |
| staffs | cinema_id | cinemas |
| staffs | manager_id | staffs (tự tham chiếu) |
| user_permission_overrides | user_id | users |
| audit_logs | user_id | users |
| notifications | customer_id | customers |
| point_transactions | customer_id | customers |
| support_tickets | customer_id | customers |
| support_tickets | assigned_to_staff | staffs |
| cinemas | manager_id | staffs |
| rooms | cinema_id | cinemas |
| seats | room_id | rooms |
| seats | seat_type_id | seat_types |
| movie_genre_mapping | movie_id | movies |
| movie_genre_mapping | category_id | categories |
| movie_categories | movie_id | movies (PK kép) |
| movie_categories | category_id | categories (PK kép) |
| showtimes | movie_id | movies |
| showtimes | room_id | rooms |
| showtimes | format_id | movie_formats |
| bookings | customer_id | customers |
| bookings | showtime_id | showtimes |
| bookings | voucher_id | vouchers |
| bookings | sold_by | staffs |
| bookings | printed_by | staffs |
| booking_seats | booking_id | bookings |
| booking_seats | seat_id | seats |
| tickets | booking_seat_id | booking_seats (1-1) |
| tickets | checked_in_by | staffs |
| reviews | customer_id | customers |
| reviews | movie_id | movies |
| reviews | ticket_id | tickets |
| seat_incidents | booking_id | bookings |
| seat_incidents | showtime_id | showtimes |
| seat_incidents | old_seat_id | seats |
| seat_incidents | new_seat_id | seats |
| seat_incidents | voucher_id | vouchers |
| seat_incidents | handled_by | staffs |
| seat_incidents | cinema_id | cinemas |
| fnb_option_items | group_id | fnb_option_groups |
| fnb_item_slots | fnb_item_id | fnb_items |
| fnb_item_slots | option_group_id | fnb_option_groups |
| fnb_item_slots | default_option_item_id | fnb_option_items |
| booking_fnbs | booking_id | bookings |
| booking_fnbs | fnb_item_id | fnb_items |
| booking_fnb_options | booking_fnb_id | booking_fnbs |
| booking_fnb_options | option_group_id | fnb_option_groups |
| booking_fnb_options | option_item_id | fnb_option_items |
| concession_sales | customer_id | customers |
| concession_sales | sold_by | staffs |
| concession_sales | cinema_id | cinemas |
| concession_sale_items | sale_id | concession_sales |
| concession_sale_items | fnb_item_id | fnb_items |
| concession_sale_item_options | sale_item_id | concession_sale_items |
| concession_sale_item_options | option_group_id | fnb_option_groups |
| concession_sale_item_options | option_item_id | fnb_option_items |
| vouchers | customer_id | customers |
| vouchers | promotion_id | promotions |

## C.2. Quan hệ "mềm" — KHÔNG khai báo FK

| Bảng nguồn | Cột | Ngầm trỏ tới |
|---|---|---|
| movies | age_rating (string) | age_ratings.code |
| banners | movie_id | movies.id |
| promotions | applicable_movie_id | movies.id |
| promo_email_log | promotion_id | promotions.id |
| promo_email_log | customer_id | customers.user_id |
| approval_requests | cinema_id | cinemas.id |
| approval_requests | ref_id | tùy `type` |
| approval_requests | requested_by_user_id | users.id |
| approval_requests | approved_by_user_id | users.id |

## C.3. Ràng buộc duy nhất nhiều cột

| Bảng | Ràng buộc |
|---|---|
| rooms | UK (`cinema_id`, `name`) |
| fnb_option_items | UK (`group_id`, `name`) |
| user_permission_overrides | UK (`user_id`, `feature`, `action`) |
| promo_email_log | UK (`promotion_id`, `customer_id`) — `uk_promo_email_customer` |
| movie_categories | PK kép (`movie_id`, `category_id`) qua `@IdClass` |
| tickets | UK (`booking_seat_id`) — quan hệ 1-1 |

---

# PHẦN D — Khuyến nghị cải thiện mô hình

1. **Hợp nhất `movie_categories` và `movie_genre_mapping`** về một cơ chế duy nhất
   (giữ `movie_genre_mapping` vì code đang dùng), tránh hai nguồn sự thật cho cùng quan hệ.
2. **Chuẩn hóa `movies.age_rating`** thành FK tới `age_ratings.id` (hoặc ít nhất thêm
   ràng buộc CHECK theo `age_ratings.code`) để tránh dữ liệu rác.
3. **Khai báo FK cho `banners.movie_id` và `promotions.applicable_movie_id`** —
   hiện xóa phim không kéo theo dọn banner/khuyến mãi trỏ tới nó.
4. **Cân nhắc bỏ hẳn `approval_requests`** nếu quyết định không phát triển tiếp phân hệ
   phê duyệt sửa sai, thay vì để bảng chết trong schema.
5. **`booking_fnb_options` và `concession_sale_item_options` có cấu trúc trùng khít** —
   có thể gộp thành một bảng đa hình nếu chấp nhận đánh đổi về ràng buộc toàn vẹn.
