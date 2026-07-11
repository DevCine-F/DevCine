# DevCine — Mô tả & phân tích ERD (Ver8)

> Tài liệu từ điển dữ liệu (data dictionary) cho cơ sở dữ liệu DevCine, đồng bộ với
> 40 entity trong `devcine-backend/src/main/java/com/devcine/backend/entity/` tại ngày 2026-06-30.
> Sơ đồ trực quan: [`ERD_Ver8_grid.drawio.xml`](ERD_Ver8_grid.drawio.xml) · Khác biệt so với bản cũ: [`erd-diff.md`](erd-diff.md).
>
> Quy ước: **PK** = khóa chính · **FK** = khóa ngoại · **UK** = ràng buộc duy nhất (unique).
> Kiểu dữ liệu mô tả ở mức logic (int, string=varchar/text, decimal, datetime, date, boolean).

---

# PHẦN A — Danh sách tất cả bảng trong ERD

Tổng cộng **37 bảng**, chia theo 7 nhóm chức năng.

## A.1. Người dùng & phân quyền (7 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 1 | `roles` | Vai trò + ma trận quyền (admin, quản lý rạp, nhân viên, khách…) |
| 2 | `users` | Tài khoản đăng nhập chung cho mọi loại người dùng |
| 3 | `customers` | Hồ sơ khách hàng (mở rộng 1-1 từ `users`) |
| 4 | `staffs` | Hồ sơ nhân viên (mở rộng 1-1 từ `users`), gắn rạp & quản lý |
| 5 | `audit_logs` | Nhật ký thao tác hệ thống (ai làm gì, bảng nào, IP) |
| 6 | `notifications` | Thông báo gửi tới khách hàng |
| 7 | `support_tickets` | Phiếu hỗ trợ/khiếu nại của khách |

## A.2. Rạp – Phòng – Ghế (4 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 8 | `cinemas` | Cụm rạp chiếu phim |
| 9 | `rooms` | Phòng chiếu thuộc một rạp |
| 10 | `seats` | Ghế vật lý trong phòng (sơ đồ ghế) |
| 11 | `seat_types` | Loại ghế và hệ số/điều chỉnh giá (thường, VIP, đôi…) |

## A.3. Phim & phân loại (5 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 12 | `movies` | Thông tin phim |
| 13 | `movie_formats` | Định dạng chiếu (2D, 3D, IMAX, 4DX…) và phụ thu |
| 14 | `categories` | Danh mục/thể loại phim |
| 15 | `movie_genre_mapping` | Bảng nối N-N giữa phim và thể loại |
| 16 | `age_ratings` | Danh mục phân loại độ tuổi (P, K, T13, T16, T18…) |

## A.4. Suất chiếu, đặt vé & vé (6 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 17 | `showtimes` | Suất chiếu (phim + phòng + định dạng + giờ) |
| 18 | `bookings` | Đơn đặt vé gắn với một suất chiếu |
| 19 | `booking_seats` | Từng ghế trong một đơn đặt vé (chốt giá) |
| 20 | `booking_fnbs` | Combo bắp nước kèm theo đơn đặt vé |
| 21 | `tickets` | Vé điện tử phát hành cho mỗi ghế đã đặt |
| 22 | `reviews` | Đánh giá/bình luận phim của khách |

## A.5. Khuyến mãi & nội dung hiển thị (4 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 23 | `promotions` | Chương trình khuyến mãi / định nghĩa voucher |
| 24 | `vouchers` | Voucher cụ thể phát cho từng khách |
| 25 | `promo_articles` | Tin khuyến mãi biên tập (nội dung hiển thị) |
| 26 | `banners` | Banner trang chủ (ảnh hoặc dựng theo phim) |

## A.6. Bắp nước (F&B) (3 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 27 | `fnb_items` | Món bắp nước / combo |
| 28 | `concession_sales` | Đơn bán bắp nước tại quầy (không gắn suất chiếu) |
| 29 | `concession_sale_items` | Dòng món trong đơn bán bắp nước độc lập |

## A.7. Vận hành ca làm & cấu hình (8 bảng)
| # | Bảng | Mục đích ngắn gọn |
|---|---|---|
| 30 | `shifts` | Định nghĩa ca làm việc (khung giờ) |
| 31 | `staff_schedules` | Phân ca cho nhân viên theo ngày |
| 32 | `shift_handovers` | Bàn giao ca: đối chiếu tiền mặt cuối ca |
| 33 | `pricing_rules` | Quy tắc giá nền theo ngày/giờ/đối tượng |
| 34 | `special_seat_prices` | Giá cố định cho định dạng/phòng đặc biệt theo loại ghế |
| 35 | `holidays` | Ngày lễ (áp giá nền HOLIDAY) |
| 36 | `faqs` | Câu hỏi thường gặp |
| 37 | `system_settings` | Cấu hình hệ thống dạng key-value |

> **Ghi chú quan trọng:** Bảng ví điện tử (`wallets`, `wallet_transactions`) trong các bản ERD cũ **đã bị gỡ hoàn toàn**.
> Quan hệ "mềm" sau **không khai báo FK** trong code (chỉ là cột số): `movies.age_rating` ↔ `age_ratings`,
> `banners.movie_id`, `promotions.applicable_movie_id`.

---

# PHẦN B — Phân tích chi tiết từng bảng

## B.1. Người dùng & phân quyền

### 1. `roles` (Role)
Định nghĩa vai trò và quyền hạn. Quyền lưu dạng JSON trong `permissions_matrix` (ma trận quyền), được service đọc để kiểm tra phân quyền.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh vai trò |
| name | string | UK | Tên vai trò (ADMIN, MANAGER, STAFF, CUSTOMER…) |
| permissions_matrix | string (TEXT) | | Chuỗi JSON mô tả quyền cho từng chức năng |

### 2. `users` (User)
Tài khoản đăng nhập **chung** cho cả khách và nhân viên. Phân biệt loại người dùng qua `role_id` và qua bản ghi mở rộng trong `customers`/`staffs`.

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
| is_active | boolean | | mặc định true | Tài khoản còn hoạt động hay bị khóa |
| created_at | datetime | | not null | Thời điểm tạo |

### 3. `customers` (Customer)
Hồ sơ khách hàng, **chia sẻ khóa chính** với `users` qua `@MapsId` (quan hệ 1-1, `user_id` vừa là PK vừa là FK).

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| user_id | int | PK, FK→users | Đồng thời là id của user tương ứng |
| dob | date | | Ngày sinh (dùng kiểm tra độ tuổi xem phim) |
| id_card | string | | Số CMND/CCCD |
| membership_tier | string | | Hạng thành viên |
| loyalty_points | int | | Điểm tích lũy (mặc định 0) |

### 4. `staffs` (Staff)
Hồ sơ nhân viên, cũng 1-1 với `users` qua `@MapsId`. Có quan hệ tự tham chiếu để biểu diễn cấp quản lý.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| user_id | int | PK, FK→users | Đồng thời là id user của nhân viên |
| staff_code | string | UK | Mã nhân viên |
| cinema_id | int | FK→cinemas | Rạp nhân viên trực thuộc |
| manager_id | int | FK→staffs | Nhân viên quản lý trực tiếp (tự tham chiếu) |

### 5. `audit_logs` (AuditLog)
Nhật ký kiểm toán mọi thao tác quan trọng, ghi tự động qua interceptor.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh log |
| user_id | int | FK→users | Người thực hiện thao tác |
| action | string | not null | Hành động (CREATE/UPDATE/DELETE…) |
| target_table | string | | Bảng bị tác động |
| ip_address | string | | Địa chỉ IP nguồn |
| timestamp | datetime | not null | Thời điểm xảy ra |

### 6. `notifications` (Notification)
Thông báo đẩy tới khách hàng (đặt vé thành công, nhắc suất chiếu, khuyến mãi, hệ thống).

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh thông báo |
| customer_id | int | FK→customers, not null | Khách nhận thông báo |
| title | string | not null | Tiêu đề |
| message | string (TEXT) | | Nội dung |
| type | string | | Phân loại: BOOKING / REMINDER / PROMOTION / SYSTEM |
| is_read | boolean | mặc định false | Đã đọc hay chưa |
| created_at | datetime | not null | Thời điểm tạo |

### 7. `support_tickets` (SupportTicket)
Phiếu hỗ trợ khách gửi lên, có thể giao cho một nhân viên xử lý.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh phiếu |
| customer_id | int | FK→customers, not null | Khách tạo phiếu |
| assigned_to_staff | int | FK→staffs | Nhân viên được giao xử lý |
| issue_type | string | | Loại vấn đề |
| description | string (TEXT) | | Mô tả chi tiết |
| status | string | | Trạng thái xử lý |
| created_at | datetime | not null | Thời điểm tạo |

---

## B.2. Rạp – Phòng – Ghế

### 8. `cinemas` (Cinema)
Cụm rạp. Nhiều cột mở rộng phục vụ trang khách (bản đồ, tiện ích, ảnh).

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
| description | string (TEXT) | | Mô tả |
| latitude | decimal | | Vĩ độ (bản đồ) |
| longitude | decimal | | Kinh độ (bản đồ) |
| amenities | string (TEXT) | | Tiện ích, phân tách bởi dấu phẩy |
| status | string | | ACTIVE / MAINTENANCE / CLOSED |
| manager_id | int | FK→staffs | Quản lý rạp |

### 9. `rooms` (Room)
Phòng chiếu thuộc một rạp, có cấu hình lưới ghế và thời gian dọn phòng giữa các suất.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh phòng |
| cinema_id | int | FK→cinemas, not null | Rạp chứa phòng |
| name | string | not null | Tên phòng |
| type | string | | Loại phòng |
| status | string | | Trạng thái hoạt động |
| turnaround_time_mins | int | | Thời gian dọn phòng giữa 2 suất (phút) |
| matrix_row | int | | Số hàng của lưới sơ đồ ghế |
| matrix_col | int | | Số cột của lưới sơ đồ ghế |

### 10. `seats` (Seat)
Ghế vật lý trong phòng. `row_char` + `col_num` là nhãn ghế (vd A1); `grid_row`/`grid_col` là tọa độ trong lưới vẽ.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh ghế |
| room_id | int | FK→rooms, not null | Phòng chứa ghế |
| row_char | string | not null | Ký tự hàng (A, B, C…) |
| col_num | int | not null | Số thứ tự cột |
| seat_type_id | int | FK→seat_types, not null | Loại ghế |
| is_active | boolean | mặc định true | Ghế còn dùng được hay không |
| grid_row | int | | Tọa độ hàng trong lưới vẽ |
| grid_col | int | | Tọa độ cột trong lưới vẽ |

### 11. `seat_types` (SeatType)
Loại ghế và phần điều chỉnh giá so với giá nền.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh loại ghế |
| name | string | UK | Tên loại (Thường, VIP, Đôi…) |
| color_code | string | | Mã màu hiển thị trên sơ đồ |
| price_modifier | decimal | | Phần cộng/trừ vào giá nền |

---

## B.3. Phim & phân loại

### 12. `movies` (Movie)
Bảng phim, nhiều thuộc tính phục vụ cả quản trị lẫn hiển thị. Quan hệ thể loại đi qua `movie_genre_mapping`.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh phim |
| title | string | not null | Tên phim |
| slug | string | UK | Định danh URL thân thiện |
| duration_mins | int | | Thời lượng (phút) |
| age_rating | string | | Mã phân loại độ tuổi (cột chuỗi, **không** FK tới `age_ratings`) |
| release_date | date | | Ngày khởi chiếu |
| end_date | date | | Ngày kết thúc chiếu |
| status | string | | Trạng thái (sắp chiếu/đang chiếu/ngừng) |
| country | string | | Quốc gia sản xuất |
| rating | string | | Điểm đánh giá hiển thị |
| poster_base64 | string (TEXT) | | Ảnh poster |
| banner_base64 | string (TEXT) | | Ảnh banner |
| show_on_banner | boolean | | Có hiển thị ở banner trang chủ không |
| trailer_url | string | | Link trailer |
| format | string | | Định dạng (chuỗi mô tả) |
| supported_formats | string | | Các định dạng hỗ trợ |
| title_vietnamese | string | | Tên tiếng Việt |
| production_year | int | | Năm sản xuất |
| language | string | | Ngôn ngữ |
| base_price | decimal | | Giá nền tham khảo của phim |
| description | string (TEXT) | | Mô tả nội dung |
| original_language | string | | Ngôn ngữ gốc |
| version_type | string | | Loại bản chiếu (lồng tiếng/phụ đề…) |
| internal_notes | string (TEXT) | | Ghi chú nội bộ |
| start_date | date | | Ngày bắt đầu (lịch nội bộ) |
| distributor | string | | Nhà phát hành |
| director | string | | Đạo diễn |
| cast_members | string (TEXT) | | Diễn viên |
| rating_count | int | | Số lượt đánh giá |

### 13. `movie_formats` (MovieFormat)
Định dạng chiếu và quy tắc phụ thu. Khi `is_fixed_price=true` thì giá lấy từ `special_seat_prices` thay vì cộng dồn.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh định dạng |
| name | string | UK | Tên (2D, 3D, IMAX, 4DX…) |
| description | string | | Mô tả |
| surcharge | decimal | | Phụ thu ngày thường |
| weekend_surcharge | decimal | | Phụ thu cuối tuần & lễ |
| is_fixed_price | boolean | | true → dùng bảng giá cố định (`special_seat_prices`) |

### 14. `categories` (Category)
Danh mục/thể loại phim.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh thể loại |
| name | string | UK | Tên thể loại |
| description | string | | Mô tả |

### 15. `movie_genre_mapping`
Bảng nối **N-N** giữa phim và thể loại (sinh từ `@ManyToMany` của entity `Movie.genres`). Đây là cơ chế thể loại **đang thực sự được code dùng**.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| movie_id | int | FK→movies | Phim |
| category_id | int | FK→categories | Thể loại |

### 16. `age_ratings` (AgeRating)
Danh mục phân loại độ tuổi để màn quản lý phim chọn lựa. Hiện **đứng độc lập** — `movies.age_rating` chỉ lưu chuỗi, chưa ràng buộc FK.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh |
| code | string | UK | Mã (P, K, T13, T16, T18…) |
| name | string | not null | Tên hiển thị |
| description | string | | Diễn giải |

---

## B.4. Suất chiếu, đặt vé & vé

### 17. `showtimes` (Showtime)
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

### 18. `bookings` (Booking)
Đơn đặt vé gắn một suất chiếu. `total_price` là tạm tính, `final_price` là sau giảm giá/voucher.
Lưu ý: việc giữ ghế tạm thời (chống đặt trùng) **không** lưu ở đây mà xử lý real-time qua WebSocket/STOMP.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh đơn |
| customer_id | int | FK→customers | Khách đặt (null nếu bán tại quầy không gắn thành viên) |
| showtime_id | int | FK→showtimes, not null | Suất chiếu |
| voucher_id | int | FK→vouchers | Voucher áp dụng (nếu có) |
| total_price | decimal | not null | Tổng tiền trước giảm |
| final_price | decimal | not null | Tổng tiền phải trả sau giảm |
| payment_method | string | | Phương thức thanh toán (VNPAY/CASH/CARD/TRANSFER) |
| status | string | | Trạng thái đơn (gộp thanh toán + đặt chỗ) |
| booking_code | string | UK | Mã đơn để tra cứu (nội dung mã QR — 1 QR đại diện cả đơn) |
| created_at | datetime | not null | Thời điểm tạo |
| printed_at | datetime | | Thời điểm quét QR & in vé giấy tại quầy (null = chưa in) |
| printed_by | int | FK→staffs | Nhân viên đã in vé cho đơn |

### 19. `booking_seats` (BookingSeat)
Mỗi ghế trong một đơn đặt vé, **chốt giá** tại thời điểm đặt.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh dòng ghế |
| booking_id | int | FK→bookings, not null | Đơn chứa ghế |
| seat_id | int | FK→seats, not null | Ghế được đặt |
| price_snapshot | decimal | | Giá ghế chốt tại thời điểm đặt |
| ticket_type | string | | Đối tượng vé: ADULT / STUDENT / CHILD / SENIOR |
| status | string | | Trạng thái ghế trong đơn |

### 20. `booking_fnbs` (BookingFnb)
Combo bắp nước mua kèm theo đơn đặt vé.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh dòng |
| booking_id | int | FK→bookings, not null | Đơn chứa combo |
| fnb_item_id | int | FK→fnb_items, not null | Món/combo |
| quantity | int | not null | Số lượng |
| price_snapshot | decimal | | Giá chốt tại thời điểm mua |

### 21. `tickets` (Ticket)
Vé điện tử phát hành **cho từng ghế** đã đặt (quan hệ 1-1 với `booking_seats`). Phục vụ soát vé bằng QR.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh vé |
| booking_seat_id | int | FK→booking_seats, UK (1-1) | Ghế tương ứng của vé |
| qr_code | string | | Mã QR soát vé |
| is_checked_in | boolean | mặc định false | Đã soát vé vào chưa |
| is_age_verified | boolean | mặc định false | Đã kiểm tra độ tuổi chưa |
| checked_in_by | int | FK→staffs | Nhân viên soát vé |
| check_in_time | datetime | | Thời điểm soát vé |

### 22. `reviews` (Review)
Đánh giá phim của khách; có thể gắn vé đã xem để xác thực, và cờ ẩn để kiểm duyệt.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh đánh giá |
| customer_id | int | FK→customers, not null | Khách đánh giá |
| movie_id | int | FK→movies, not null | Phim được đánh giá |
| ticket_id | int | FK→tickets | Vé xác thực đã xem (nếu có) |
| rating | int | not null | Số sao |
| comment | string (TEXT) | | Nội dung bình luận |
| hidden | boolean | mặc định false | true = bị admin ẩn khỏi trang công khai |
| created_at | datetime | not null | Thời điểm tạo |

---

## B.5. Khuyến mãi & nội dung hiển thị

### 23. `promotions` (Promotion)
Định nghĩa chương trình khuyến mãi / khuôn mẫu voucher, gồm điều kiện áp dụng và giới hạn lượt dùng.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh khuyến mãi |
| code | string | UK | Mã code |
| name | string | | Tên hiển thị |
| description | string | | Mô tả ngắn |
| discount_type | string | not null | Loại giảm (PERCENT/FIXED…) |
| discount_value | decimal | not null | Giá trị giảm |
| start_date | datetime | | Bắt đầu hiệu lực |
| end_date | datetime | | Kết thúc hiệu lực |
| is_stackable | boolean | mặc định false | Có cộng dồn với KM khác không |
| points_required | int | | Điểm cần để đổi (mặc định 0) |
| allow_point_redemption | boolean | mặc định false | Cho khách tự đổi điểm lấy voucher |
| min_order_value | decimal | | Giá trị đơn tối thiểu để áp |
| applicable_movie_id | int | | Chỉ áp cho phim này (cột số, **không** FK) |
| customer_eligibility | string | | Đối tượng: ALL / NEW_CUSTOMER |
| usage_limit | int | | Tổng lượt được dùng (0 = không giới hạn) |
| used_count | int | | Số lượt đã dùng |

### 24. `vouchers` (Voucher)
Voucher cụ thể phát cho một khách, sinh ra từ một `promotion`.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh voucher |
| customer_id | int | FK→customers, not null | Khách sở hữu |
| promotion_id | int | FK→promotions, not null | Khuyến mãi gốc |
| valid_until | datetime | | Hạn dùng |
| is_used | boolean | mặc định false | Đã dùng chưa |

### 25. `promo_articles` (PromoArticle)
Tin khuyến mãi dạng **bài biên tập** để hiển thị cho khách (khác hẳn voucher/promotion).

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh bài |
| title | string | not null | Tiêu đề |
| description | string | | Mô tả ngắn (thẻ danh sách) |
| image_url | string | | Ảnh banner/thumbnail |
| content | string (TEXT) | | Nội dung chi tiết |
| start_date | date | | Bắt đầu hiển thị |
| end_date | date | | Kết thúc hiển thị |
| is_active | boolean | mặc định true | Bật/tắt hiển thị |
| display_order | int | | Thứ tự sắp xếp |
| created_at | datetime | | Thời điểm tạo |

### 26. `banners` (Banner)
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
| is_active | boolean | mặc định true | Bật/tắt |
| display_order | int | | Thứ tự sắp xếp |
| link | string | | Liên kết khi bấm vào |

---

## B.6. Bắp nước (F&B) & kho

### 27. `fnb_items` (FnbItem)
Món bắp nước, combo, hoặc nguyên liệu. Một dòng đóng nhiều vai trò tùy `type`.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh món |
| name | string | not null | Tên món |
| type | string | | Phân loại (combo / nguyên liệu / món lẻ) |
| price | decimal | not null | Giá bán |
| image_url | string | | Ảnh món |
| description | string | | Mô tả |
| is_active | boolean | mặc định true | Còn bán/hiển thị cho khách không |

### 28. `concession_sales` (ConcessionSale)
Đơn bán bắp nước **độc lập tại quầy** — không gắn suất chiếu/ghế, tách khỏi `bookings` để doanh thu phòng vé không bị sai lệch bởi khách vãng lai.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh đơn |
| sale_code | string | UK | Mã đơn |
| customer_id | int | FK→customers | Thành viên (null nếu khách vãng lai) |
| total_price | decimal | not null | Tổng tiền |
| payment_method | string | | Phương thức thanh toán |
| status | string | | Trạng thái đơn |
| created_at | datetime | not null | Thời điểm tạo |

### 29. `concession_sale_items` (ConcessionSaleItem)
Từng dòng món trong một đơn bán bắp nước độc lập.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh dòng |
| sale_id | int | FK→concession_sales, not null | Đơn chứa món |
| fnb_item_id | int | FK→fnb_items, not null | Món bán |
| quantity | int | not null | Số lượng |
| price_snapshot | decimal | not null | Giá chốt tại thời điểm bán |

---

## B.7. Vận hành ca làm & cấu hình

### 30. `shifts` (Shift)
Định nghĩa khung giờ các ca làm việc.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh ca |
| start_time | datetime | not null | Giờ bắt đầu ca |
| end_time | datetime | not null | Giờ kết thúc ca |
| status | string | | Trạng thái |

### 31. `staff_schedules` (StaffSchedule)
Phân ca cho nhân viên theo từng ngày làm việc.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh lịch |
| staff_id | int | FK→staffs, not null | Nhân viên |
| shift_id | int | FK→shifts, not null | Ca làm |
| work_date | date | not null | Ngày làm |
| status | string | | Trạng thái (đã nhận/đã làm…) |

### 32. `shift_handovers` (ShiftHandover)
Bàn giao ca cuối ngày: nhân viên khai tiền mặt, hệ thống đối chiếu, quản lý duyệt.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh phiếu bàn giao |
| staff_schedule_id | int | FK→staff_schedules, not null | Lịch ca tương ứng |
| approved_by_manager | int | FK→staffs | Quản lý duyệt |
| declared_cash | decimal | | Tiền mặt nhân viên khai |
| system_cash | decimal | | Tiền mặt theo hệ thống |
| difference | decimal | | Chênh lệch (khai − hệ thống) |
| status | string | | Trạng thái duyệt |

### 33. `pricing_rules` (PricingRule)
Quy tắc giá nền theo ngày/khung giờ/đối tượng. Khi nhiều rule cùng khớp, rule có `priority` cao hơn được ưu tiên.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh rule |
| name | string | not null | Tên rule |
| rule_type | string | not null | Loại rule (BASE_PRICE…) |
| day_type | string | | WEEKDAY / WEDNESDAY / WEEKEND / HOLIDAY / ALL |
| time_slot | string | | EARLY / BEFORE_17H / AFTER_17H / ALL |
| audience_type | string | | ADULT / STUDENT / CHILD / SENIOR / ALL |
| value | decimal | not null | Giá nền áp dụng |
| priority | int | | Độ ưu tiên (cao hơn thắng) |
| active | boolean | | Rule còn hiệu lực |
| start_date | datetime | | Bắt đầu hiệu lực |
| end_date | datetime | | Kết thúc hiệu lực |

### 34. `special_seat_prices` (SpecialSeatPrice)
Bảng giá **cố định** cho định dạng/phòng đặc biệt (IMAX/4DX/Gold) theo từng loại ghế; ghi đè công thức cộng dồn khi `movie_formats.is_fixed_price=true`. Ràng buộc duy nhất theo cặp (format_id, seat_type_id).

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh dòng giá |
| format_id | int | FK→movie_formats, not null | Định dạng đặc biệt |
| seat_type_id | int | FK→seat_types, not null | Loại ghế |
| price | decimal | not null | Giá cố định |

### 35. `holidays` (Holiday)
Danh sách ngày lễ; suất chiếu rơi vào ngày này áp giá nền `day_type=HOLIDAY`.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh |
| holiday_date | date | UK | Ngày lễ |
| name | string | not null | Tên ngày lễ |

### 36. `faqs` (Faq)
Câu hỏi thường gặp hiển thị ở trang Hỗ trợ, admin CRUD được.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| id | int | PK | Định danh |
| category | string | not null | Nhóm câu hỏi |
| question | string | not null | Câu hỏi |
| answer | string (TEXT) | | Câu trả lời |
| display_order | int | | Thứ tự hiển thị |
| is_active | boolean | | Bật/tắt |

### 37. `system_settings` (SystemSetting)
Cấu hình hệ thống dạng key-value, khóa chính là chính cái key.

| Cột | Kiểu | Khóa | Ý nghĩa |
|---|---|---|---|
| setting_key | string | PK | Tên cấu hình |
| setting_value | string (TEXT) | | Giá trị cấu hình |

---

## Phụ lục — Tổng hợp quan hệ khóa ngoại (FK)

| Bảng nguồn | Cột | → Bảng đích |
|---|---|---|
| users | role_id | roles |
| customers | user_id | users (1-1, @MapsId) |
| staffs | user_id | users (1-1, @MapsId) |
| staffs | cinema_id | cinemas |
| staffs | manager_id | staffs (tự tham chiếu) |
| audit_logs | user_id | users |
| notifications | customer_id | customers |
| support_tickets | customer_id | customers |
| support_tickets | assigned_to_staff | staffs |
| cinemas | manager_id | staffs |
| rooms | cinema_id | cinemas |
| seats | room_id | rooms |
| seats | seat_type_id | seat_types |
| movie_genre_mapping | movie_id | movies |
| movie_genre_mapping | category_id | categories |
| showtimes | movie_id | movies |
| showtimes | room_id | rooms |
| showtimes | format_id | movie_formats |
| reviews | customer_id | customers |
| reviews | movie_id | movies |
| reviews | ticket_id | tickets |
| bookings | customer_id | customers |
| bookings | showtime_id | showtimes |
| bookings | voucher_id | vouchers |
| bookings | printed_by | staffs |
| booking_seats | booking_id | bookings |
| booking_seats | seat_id | seats |
| booking_fnbs | booking_id | bookings |
| booking_fnbs | fnb_item_id | fnb_items |
| tickets | booking_seat_id | booking_seats (1-1) |
| tickets | checked_in_by | staffs |
| vouchers | customer_id | customers |
| vouchers | promotion_id | promotions |
| concession_sales | customer_id | customers |
| concession_sale_items | sale_id | concession_sales |
| concession_sale_items | fnb_item_id | fnb_items |
| staff_schedules | staff_id | staffs |
| staff_schedules | shift_id | shifts |
| shift_handovers | staff_schedule_id | staff_schedules |
| shift_handovers | approved_by_manager | staffs |
| special_seat_prices | format_id | movie_formats |
| special_seat_prices | seat_type_id | seat_types |

> Các cột mang tính tham chiếu nhưng **không** khai báo FK (quan hệ mềm): `movies.age_rating`,
> `banners.movie_id`, `promotions.applicable_movie_id`.
