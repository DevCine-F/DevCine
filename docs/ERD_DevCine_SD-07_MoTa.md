# TÀI LIỆU ĐẶC TẢ CƠ SỞ DỮ LIỆU (ERD SPECIFICATION) — DEVCINE

**Phiên bản:** Ver 10.0 (Đã tối ưu & tinh gọn)
**Quy mô:** 39 Bảng / 39 Thực thể (Entities)
**Kiến trúc:** Phân tách rõ ràng thành 7 Phân hệ nghiệp vụ (A → G)

---

## MỤC LỤC CÁC PHÂN HỆ

- [PHÂN HỆ A: NGƯỜI DÙNG &amp; PHÂN QUYỀN (7 BẢNG)](#phân-hệ-a-người-dùng--phân-quyền)
- [PHÂN HỆ B: CỤM RẠP – PHÒNG CHIẾU – GHẾ NGỒI (4 BẢNG)](#phân-hệ-b-cụm-rạp--phòng-chiếu--ghế-ngồi)
- [PHÂN HỆ C: PHIM &amp; PHÂN LOẠI (6 BẢNG)](#phân-hệ-c-phim--phân-loại)
- [PHÂN HỆ D: SUẤT CHIẾU – ĐẶT VÉ – VÉ XEM PHIM (7 BẢNG)](#phân-hệ-d-suất-chiếu--đặt-vé--vé-xem-phim)
- [PHÂN HỆ E: BẮP NƯỚC (F&amp;B) &amp; BÁN LẺ POS (7 BẢNG)](#phân-hệ-e-bắp-nước-fb--bán-lẻ-pos)
- [PHÂN HỆ F: CẤU HÌNH &amp; DANH MỤC HỆ THỐNG (3 BẢNG)](#phân-hệ-f-cấu-hình--danh-mục-hệ-thống)
- [PHÂN HỆ G: KHUYẾN MÃI, MARKETING &amp; NỘI DUNG (5 BẢNG)](#phân-hệ-g-khuyến-mãi-marketing--nội-dung)

---

## PHÂN HỆ A: NGƯỜI DÙNG & PHÂN QUYỀN

### 1. `ROLES` — Danh mục vai trò / Chức vụ

*Mô tả:* Quản lý các nhóm quyền hệ thống (VD: `ADMIN`, `MANAGER`, `TICKET_STAFF`, `FNB_STAFF`, `CUSTOMER`).

| Tên cột              | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                    |
| ---------------------- | --------------- | :----------: | ------------------------ | ---------------------------------------------------------- |
| `id`                 | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh vai trò                                   |
| `name`               | `string`      | **UK** | NOT NULL, UNIQUE         | Tên vai trò (ADMIN, MANAGER, STAFF...)                   |
| `permissions_matrix` | `text`        |              | NULL                     | Ma trận quyền JSON cấu hình chi tiết theo chức năng |

---

### 2. `USERS` — Tài khoản người dùng hệ thống

*Mô tả:* Chứa thông tin đăng nhập, xác thực tập trung cho cả khách hàng và nhân viên.

| Tên cột                | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                              |
| ------------------------ | --------------- | :----------: | ------------------------ | -------------------------------------------------------------------- |
| `id`                   | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh người dùng                                        |
| `username`             | `string`      | **UK** | NOT NULL, UNIQUE         | Tên đăng nhập                                                    |
| `password_hash`        | `string`      |              | NOT NULL                 | Mật khẩu đã mã hóa (BCrypt)                                    |
| `full_name`            | `string`      |              | NOT NULL                 | Họ và tên đầy đủ                                              |
| `avatar_url`           | `string`      |              | NULL                     | Đường dẫn ảnh đại diện                                       |
| `email`                | `string`      | **UK** | NOT NULL, UNIQUE         | Địa chỉ email liên hệ                                           |
| `phone`                | `string`      |              | NULL                     | Số điện thoại liên hệ                                          |
| `role_id`              | `int`         | **FK** | NOT NULL                 | Tham chiếu tới`ROLES(id)`                                        |
| `is_active`            | `boolean`     |              | DEFAULT true             | Trạng thái tài khoản (Đang hoạt động / Bị khóa)            |
| `must_change_password` | `boolean`     |              | DEFAULT false            | Yêu cầu đổi mật khẩu lần đầu (khi được cấp tài khoản) |
| `created_at`           | `datetime`    |              | NOT NULL                 | Thời gian tạo tài khoản                                          |

---

### 3. `CUSTOMERS` — Hồ sơ khách hàng / Thành viên

*Mô tả:* Thông tin mở rộng dành riêng cho tài khoản Khách hàng (Thành viên rạp).

| Tên cột           | Kiểu dữ liệu |      Khóa      | Ràng buộc        | Mô tả                                                           |
| ------------------- | --------------- | :--------------: | ------------------ | ----------------------------------------------------------------- |
| `user_id`         | `int`         | **PK, FK** | NOT NULL           | Mã người dùng, tham chiếu`USERS(id)` (1-1)                 |
| `dob`             | `date`        |                  | NULL               | Ngày sinh                                                        |
| `id_card`         | `string`      |                  | NULL               | Số CMND / CCCD                                                   |
| `membership_tier` | `string`      |                  | DEFAULT 'STANDARD' | Hạng thành viên (STANDARD, VIP, VVIP)                          |
| `loyalty_points`  | `int`         |                  | DEFAULT 0          | Điểm tích lũy hiện tại có thể dùng                       |
| `lifetime_points` | `int`         |                  | DEFAULT 0          | Tổng điểm tích lũy trọn đời (dùng để xét nâng hạng) |

---

### 4. `STAFFS` — Hồ sơ nhân viên rạp

*Mô tả:* Thông tin mở rộng dành riêng cho tài khoản Nhân viên / Quản lý làm việc tại cụm rạp.

| Tên cột      | Kiểu dữ liệu |      Khóa      | Ràng buộc      | Mô tả                                                       |
| -------------- | --------------- | :--------------: | ---------------- | ------------------------------------------------------------- |
| `user_id`    | `int`         | **PK, FK** | NOT NULL         | Mã người dùng, tham chiếu`USERS(id)` (1-1)             |
| `staff_code` | `string`      |   **UK**   | NOT NULL, UNIQUE | Mã số nhân viên (VD:`NV001`, `QL002`)                 |
| `cinema_id`  | `int`         |   **FK**   | NULL             | Thuộc cụm rạp làm việc, tham chiếu`CINEMAS(id)`       |
| `manager_id` | `int`         |   **FK**   | NULL             | Người quản lý trực tiếp, tham chiếu`STAFFS(user_id)` |
| `created_at` | `datetime`    |                  | NOT NULL         | Thời gian tạo hồ sơ                                       |
| `updated_at` | `datetime`    |                  | NULL             | Thời gian cập nhật hồ sơ                                 |

---

### 5. `USER_PERMISSION_OVERRIDES` — Ghi đè quyền người dùng

*Mô tả:* Cấp thêm hoặc tước quyền cụ thể cho một cá nhân mà không cần đổi Role chung.

| Tên cột      | Kiểu dữ liệu |      Khóa      | Ràng buộc              | Mô tả                                                      |
| -------------- | --------------- | :--------------: | ------------------------ | ------------------------------------------------------------ |
| `id`         | `int`         |   **PK**   | NOT NULL, AUTO_INCREMENT | Mã định danh bản ghi                                     |
| `user_id`    | `int`         | **FK, UK** | NOT NULL                 | Tham chiếu tới`USERS(id)`                                |
| `feature`    | `string`      |   **UK**   | NOT NULL                 | Tên module/tính năng (VD:`movie_management`)            |
| `action`     | `string`      |   **UK**   | NOT NULL                 | Hành động thao tác (VD:`create`, `edit`, `delete`) |
| `effect`     | `string`      |                  | NOT NULL                 | Hiệu lực (`ALLOW` hoặc `DENY`)                        |
| `updated_at` | `datetime`    |                  | NOT NULL                 | Thời gian cập nhật                                        |

---

### 6. `AUDIT_LOGS` — Nhật ký kiểm toán hệ thống

*Mô tả:* Ghi vết toàn bộ thao tác quan trọng của nhân viên và quản trị viên.

| Tên cột        | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                 |
| ---------------- | --------------- | :----------: | ------------------------ | ------------------------------------------------------- |
| `id`           | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh bản ghi log                            |
| `user_id`      | `int`         | **FK** | NULL                     | Người thực hiện thao tác, tham chiếu`USERS(id)` |
| `action`       | `string`      |              | NOT NULL                 | Hành động (CREATE, UPDATE, DELETE, LOGIN...)         |
| `target_table` | `string`      |              | NOT NULL                 | Bảng dữ liệu bị tác động                         |
| `ip_address`   | `string`      |              | NULL                     | Địa chỉ IP của máy thao tác                       |
| `timestamp`    | `datetime`    |              | NOT NULL                 | Mốc thời gian ghi nhận                               |

---

### 7. `POINT_TRANSACTIONS` — Lịch sử biến động điểm thưởng

*Mô tả:* Ghi nhận chi tiết từng lần cộng/trừ điểm thành viên (Đặt vé, Đổi voucher, Thu hồi hoàn vé).

| Tên cột         | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                            |
| ----------------- | --------------- | :----------: | ------------------------ | ------------------------------------------------------------------ |
| `id`            | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã giao dịch điểm                                              |
| `customer_id`   | `int`         | **FK** | NOT NULL                 | Khách hàng nhận biến động, tham chiếu`CUSTOMERS(user_id)` |
| `points`        | `int`         |              | NOT NULL                 | Số điểm thay đổi (+ cộng, - trừ)                            |
| `type`          | `string`      |              | NOT NULL                 | Loại biến động (`EARN`, `REDEEM`, `VOID`, `EXPIRE`)    |
| `source`        | `string`      |              | NULL                     | Nguồn phát sinh (BOOKING, CONCESSION...)                         |
| `ref_code`      | `string`      |              | NULL                     | Mã đơn hàng hoặc đối tượng tham chiếu                    |
| `balance_after` | `int`         |              | NOT NULL                 | Số dư điểm sau giao dịch                                      |
| `note`          | `text`        |              | NULL                     | Ghi chú chi tiết                                                 |
| `created_at`    | `datetime`    |              | NOT NULL                 | Thời gian ghi nhận                                               |

---

## PHÂN HỆ B: CỤM RẠP – PHÒNG CHIẾU – GHẾ NGỒI

### 8. `CINEMAS` — Cụm rạp chiếu phim

*Mô tả:* Quản lý các cơ sở rạp trực thuộc chuỗi rạp DevCine.

| Tên cột        | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                              |
| ---------------- | --------------- | :----------: | ------------------------ | -------------------------------------------------------------------- |
| `id`           | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh rạp                                                 |
| `name`         | `string`      |              | NOT NULL                 | Tên cụm rạp (VD: DevCine Nguyễn Du)                              |
| `address`      | `string`      |              | NOT NULL                 | Địa chỉ chi tiết                                                 |
| `city`         | `string`      |              | NOT NULL                 | Tỉnh / Thành phố                                                  |
| `district`     | `string`      |              | NOT NULL                 | Quận / Huyện                                                       |
| `type`         | `string`      |              | NOT NULL                 | Loại rạp                                                           |
| `hotline`      | `string`      |              | NULL                     | Số hotline hỗ trợ                                                 |
| `rooms`        | `int`         |              | DEFAULT 0                | Tổng số phòng chiếu                                              |
| `image_url`    | `string`      |              | NULL                     | Hình ảnh đại diện rạp                                          |
| `description`  | `text`        |              | NULL                     | Mô tả giới thiệu rạp                                            |
| `latitude`     | `double`      |              | NULL                     | Tọa độ vĩ độ (phục vụ bản đồ)                             |
| `longitude`    | `double`      |              | NULL                     | Tọa độ kinh độ (phục vụ bản đồ)                            |
| `amenities`    | `text`        |              | NULL                     | Tiện ích rạp (JSON hoặc chuỗi CSV: Bãi đỗ xe, Thang máy...) |
| `status`       | `string`      |              | DEFAULT 'ACTIVE'         | Trạng thái rạp (`ACTIVE`, `MAINTENANCE`, `INACTIVE`)        |
| `opening_time` | `time`        |              | NULL                     | Giờ mở cửa                                                        |
| `closing_time` | `time`        |              | NULL                     | Giờ đóng cửa                                                     |
| `manager_id`   | `int`         | **FK** | NULL                     | Giám đốc / Quản lý rạp, tham chiếu`STAFFS(user_id)`         |

---

### 9. `ROOMS` — Phòng chiếu phim

*Mô tả:* Danh sách phòng chiếu tại từng cụm rạp.

| Tên cột                | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                       |
| ------------------------ | --------------- | :----------: | ------------------------ | ------------------------------------------------------------- |
| `id`                   | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh phòng chiếu                                 |
| `cinema_id`            | `int`         | **FK** | NOT NULL                 | Thuộc cụm rạp nào, tham chiếu`CINEMAS(id)`             |
| `name`                 | `string`      | **UK** | NOT NULL                 | Tên phòng chiếu (Phòng 1, Cinema 2, IMAX...)              |
| `type`                 | `string`      |              | NOT NULL                 | Định dạng phòng chiếu (2D, 3D, IMAX, 4DX, GOLD_CLASS)    |
| `status`               | `string`      |              | DEFAULT 'ACTIVE'         | Trạng thái phòng (`ACTIVE`, `MAINTENANCE`, `CLOSED`) |
| `turnaround_time_mins` | `int`         |              | DEFAULT 15               | Thời gian dọn dẹp phòng giữa 2 suất chiếu (phút)      |
| `matrix_row`           | `int`         |              | NOT NULL                 | Số hàng ghế trong ma trận layout                          |
| `matrix_col`           | `int`         |              | NOT NULL                 | Số cột ghế trong ma trận layout                           |

---

### 10. `SEAT_TYPES` — Danh mục loại ghế

*Mô tả:* Quy định thuộc tính và sức chứa của các loại ghế (Thường, VIP, Sweetbox đôi).

| Tên cột       | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                               |
| --------------- | --------------- | :----------: | ------------------------ | ----------------------------------------------------- |
| `id`          | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh loại ghế                            |
| `name`        | `string`      | **UK** | NOT NULL, UNIQUE         | Tên loại ghế (`STANDARD`, `VIP`, `SWEETBOX`) |
| `color`       | `string`      |              | NULL                     | Mã màu hiển thị trên sơ đồ ghế (HEX code)    |
| `surcharge`   | `decimal`     |              | DEFAULT 0                | Phụ thu cơ bản của loại ghế (VNĐ)              |
| `capacity`    | `int`         |              | DEFAULT 1                | Sức chứa (Ghế đơn = 1 vé, Sweetbox = 2 vé)     |
| `description` | `text`        |              | NULL                     | Mô tả loại ghế                                    |
| `is_active`   | `boolean`     |              | DEFAULT true             | Trạng thái kích hoạt                              |

---

### 11. `SEATS` — Ghế ngồi trong phòng chiếu

*Mô tả:* Từng ghế vật lý cụ thể được định vị trên lưới ma trận phòng chiếu.

| Tên cột        | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                          |
| ---------------- | --------------- | :----------: | ------------------------ | ------------------------------------------------ |
| `id`           | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh ghế                             |
| `room_id`      | `int`         | **FK** | NOT NULL                 | Thuộc phòng chiếu, tham chiếu`ROOMS(id)`   |
| `row_char`     | `string`      |              | NOT NULL                 | Ký tự hàng (A, B, C... J)                     |
| `col_num`      | `int`         |              | NOT NULL                 | Số thứ tự cột (1, 2, 3...)                   |
| `seat_type_id` | `int`         | **FK** | NOT NULL                 | Thuộc loại ghế, tham chiếu`SEAT_TYPES(id)` |
| `is_active`    | `boolean`     |              | DEFAULT true             | Trạng thái ghế khả dụng                     |
| `position_x`   | `int`         |              | NULL                     | Tọa độ X trên lưới ma trận                |
| `position_y`   | `int`         |              | NULL                     | Tọa độ Y trên lưới ma trận                |
| `created_at`   | `datetime`    |              | NOT NULL                 | Thời gian tạo ghế                             |
| `updated_at`   | `datetime`    |              | NULL                     | Thời gian cập nhật                            |

---

## PHÂN HỆ C: PHIM & PHÂN LOẠI

### 12. `AGE_RATINGS` — Phân loại độ tuổi khán giả

*Mô tả:* Chuẩn kiểm duyệt độ tuổi điện ảnh Việt Nam (P, K, T13, T16, T18, C).

| Tên cột       | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                       |
| --------------- | --------------- | :----------: | ------------------------ | --------------------------------------------- |
| `id`          | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã phân loại độ tuổi                    |
| `code`        | `string`      | **UK** | NOT NULL, UNIQUE         | Mã nhãn độ tuổi (P, K, T13, T16, T18, C) |
| `name`        | `string`      |              | NOT NULL                 | Tên hiển thị đầy đủ                    |
| `description` | `text`        |              | NULL                     | Mô tả chi tiết quy định độ tuổi       |
| `min_age`     | `int`         |              | NOT NULL                 | Độ tuổi tối thiểu được phép xem      |

---

### 13. `CATEGORIES` — Thể loại phim

*Mô tả:* Danh mục các thể loại phim (Hành động, Hài, Kinh dị, Hoạt hình...).

| Tên cột       | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                    |
| --------------- | --------------- | :----------: | ------------------------ | -------------------------- |
| `id`          | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh thể loại |
| `name`        | `string`      | **UK** | NOT NULL, UNIQUE         | Tên thể loại phim       |
| `description` | `text`        |              | NULL                     | Mô tả thể loại         |
| `is_active`   | `boolean`     |              | DEFAULT true             | Trạng thái hoạt động  |

---

### 14. `MOVIE_FORMATS` — Định dạng chiếu phim

*Mô tả:* Danh mục định dạng công nghệ trình chiếu (2D Lồng tiếng, 2D Phụ đề, 3D, IMAX 2D...).

| Tên cột       | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                         |
| --------------- | --------------- | :----------: | ------------------------ | ----------------------------------------------- |
| `id`          | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định dạng phim                           |
| `name`        | `string`      | **UK** | NOT NULL, UNIQUE         | Tên định dạng chiếu                        |
| `code`        | `string`      | **UK** | NOT NULL, UNIQUE         | Mã định dạng ngắn (2D, 3D, IMAX)           |
| `surcharge`   | `decimal`     |              | DEFAULT 0                | Phụ thu công nghệ định dạng chiếu (VNĐ) |
| `description` | `text`        |              | NULL                     | Mô tả định dạng                            |
| `is_active`   | `boolean`     |              | DEFAULT true             | Trạng thái hoạt động                       |

---

### 15. `MOVIES` — Thông tin phim

*Mô tả:* Kho dữ liệu phim chiếu rạp.

| Tên cột          | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                      |
| ------------------ | --------------- | :----------: | ------------------------ | ------------------------------------------------------------ |
| `id`             | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh phim                                         |
| `title`          | `string`      |              | NOT NULL                 | Tên phim                                                    |
| `director`       | `string`      |              | NULL                     | Đạo diễn                                                  |
| `cast`           | `string`      |              | NULL                     | Dàn diễn viên chính                                      |
| `release_date`   | `date`        |              | NOT NULL                 | Ngày khởi chiếu chính thức                              |
| `end_date`       | `date`        |              | NULL                     | Ngày dự kiến ngừng chiếu                                |
| `duration`       | `int`         |              | NOT NULL                 | Thời lượng phim (phút)                                   |
| `trailer_url`    | `string`      |              | NULL                     | Link trailer YouTube                                         |
| `poster_url`     | `string`      |              | NULL                     | Ảnh poster phim                                             |
| `banner_url`     | `string`      |              | NULL                     | Ảnh banner khổ ngang                                       |
| `description`    | `text`        |              | NULL                     | Tóm tắt nội dung phim                                     |
| `status`         | `string`      |              | NOT NULL                 | Trạng thái (`COMING_SOON`, `NOW_SHOWING`, `STOPPED`) |
| `age_rating_id`  | `int`         | **FK** | NOT NULL                 | Phân loại độ tuổi, tham chiếu`AGE_RATINGS(id)`       |
| `show_on_banner` | `boolean`     |              | DEFAULT false            | Cờ đồng bộ hiển thị trên Banner trang chủ            |
| `created_at`     | `datetime`    |              | NOT NULL                 | Thời gian thêm phim                                        |
| `updated_at`     | `datetime`    |              | NULL                     | Thời gian cập nhật                                        |

---

### 16. `MOVIE_CATEGORIES` — Bảng nối Phim & Thể loại (N-N)

*Mô tả:* Thiết lập quan hệ nhiều-nhiều giữa Phim và Thể loại.

| Tên cột       | Kiểu dữ liệu |      Khóa      | Ràng buộc | Mô tả                            |
| --------------- | --------------- | :--------------: | ----------- | ---------------------------------- |
| `movie_id`    | `int`         | **PK, FK** | NOT NULL    | Tham chiếu tới`MOVIES(id)`     |
| `category_id` | `int`         | **PK, FK** | NOT NULL    | Tham chiếu tới`CATEGORIES(id)` |

---

### 17. `REVIEWS` — Đánh giá & Bình luận phim

*Mô tả:* Phản hồi, xếp hạng sao và nhận xét từ khách hàng đã xem phim.

| Tên cột       | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                     |
| --------------- | --------------- | :----------: | ------------------------ | ----------------------------------------------------------- |
| `id`          | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã đánh giá                                             |
| `movie_id`    | `int`         | **FK** | NOT NULL                 | Phim được đánh giá, tham chiếu`MOVIES(id)`         |
| `customer_id` | `int`         | **FK** | NOT NULL                 | Khách hàng đánh giá, tham chiếu`CUSTOMERS(user_id)` |
| `rating`      | `int`         |              | NOT NULL                 | Điểm số đánh giá (1 đến 5 sao)                      |
| `comment`     | `text`        |              | NULL                     | Nội dung bình luận chi tiết                             |
| `status`      | `string`      |              | DEFAULT 'APPROVED'       | Trạng thái duyệt (`APPROVED`, `HIDDEN`)              |
| `created_at`  | `datetime`    |              | NOT NULL                 | Thời gian gửi đánh giá                                 |

---

## PHÂN HỆ D: SUẤT CHIẾU – ĐẶT VÉ – VÉ XEM PHIM

### 18. `SHOWTIMES` — Suất chiếu phim

*Mô tả:* Lịch chiếu cụ thể của một bộ phim tại phòng chiếu và khung giờ xác định.

| Tên cột      | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                               |
| -------------- | --------------- | :----------: | ------------------------ | ----------------------------------------------------- |
| `id`         | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh suất chiếu                          |
| `movie_id`   | `int`         | **FK** | NOT NULL                 | Phim chiếu, tham chiếu`MOVIES(id)`                |
| `room_id`    | `int`         | **FK** | NOT NULL                 | Phòng chiếu, tham chiếu`ROOMS(id)`               |
| `format_id`  | `int`         | **FK** | NOT NULL                 | Định dạng chiếu, tham chiếu`MOVIE_FORMATS(id)` |
| `start_time` | `datetime`    |              | NOT NULL                 | Thời gian bắt đầu chiếu                          |
| `end_time`   | `datetime`    |              | NOT NULL                 | Thời gian kết thúc suất chiếu                    |
| `status`     | `string`      |              | DEFAULT 'OPEN'           | Trạng thái (`OPEN`, `CLOSED`, `CANCELLED`)    |
| `base_price` | `decimal`     |              | NOT NULL                 | Giá vé cơ sở của suất chiếu (VNĐ)             |
| `created_at` | `datetime`    |              | NOT NULL                 | Thời gian tạo suất                                 |

---

### 19. `PRICING_RULES` — Quy tắc cấu hình giá vé linh hoạt

*Mô tả:* Quy tắc điều chỉnh giá vé theo đối tượng, ngày lễ, khung giờ chiếu và thứ trong tuần.

| Tên cột            | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                                             |
| -------------------- | --------------- | :----------: | ------------------------ | ----------------------------------------------------------------------------------- |
| `id`               | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã quy tắc giá                                                                   |
| `name`             | `string`      |              | NOT NULL                 | Tên quy tắc (Giá HSSV, Suất khuya, Cuối tuần, Ngày lễ...)                   |
| `day_type`         | `string`      |              | NOT NULL                 | Loại ngày (`WEEKDAY`, `WEEKEND`, `HOLIDAY`, `ALL`)                        |
| `customer_type`    | `string`      |              | NOT NULL                 | Đối tượng khách (`ADULT`, `STUDENT`, `CHILD`, `SENIOR`, `ALL`)       |
| `time_from`        | `time`        |              | NULL                     | Áp dụng từ khung giờ                                                            |
| `time_to`          | `time`        |              | NULL                     | Đến khung giờ                                                                    |
| `adjustment_type`  | `string`      |              | NOT NULL                 | Cách điều chỉnh (`FIXED_PRICE`, `PERCENTAGE_SURCHARGE`, `FLAT_SURCHARGE`) |
| `adjustment_value` | `decimal`     |              | NOT NULL                 | Giá trị điều chỉnh                                                             |
| `priority`         | `int`         |              | DEFAULT 0                | Độ ưu tiên áp dụng quy tắc                                                   |
| `is_active`        | `boolean`     |              | DEFAULT true             | Trạng thái kích hoạt                                                            |

---

### 20. `BOOKINGS` — Hóa đơn / Đơn đặt vé

*Mô tả:* Giao dịch đặt vé tổng hợp bao gồm cả vé xem phim, bắp nước F&B và voucher giảm giá.

| Tên cột              | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                                                                      |
| ---------------------- | --------------- | :----------: | ------------------------ | ------------------------------------------------------------------------------------------------------------ |
| `id`                 | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh đơn hàng                                                                                  |
| `booking_code`       | `string`      | **UK** | NOT NULL, UNIQUE         | Mã tra cứu đơn hàng (VD:`BK260902-8X9Y`)                                                              |
| `customer_id`        | `int`         | **FK** | NULL                     | Khách hàng đặt (NULL nếu là khách vãng lai tại quầy)                                               |
| `showtime_id`        | `int`         | **FK** | NOT NULL                 | Suất chiếu, tham chiếu`SHOWTIMES(id)`                                                                   |
| `promotion_id`       | `int`         | **FK** | NULL                     | Mã ưu đãi áp dụng, tham chiếu`PROMOTIONS(id)`                                                       |
| `voucher_id`         | `int`         | **FK** | NULL                     | Voucher đã áp dụng, tham chiếu`VOUCHERS(id)`                                                          |
| `total_price`        | `decimal`     |              | NOT NULL                 | Tổng tiền gốc trước giảm trừ                                                                          |
| `discount_amount`    | `decimal`     |              | DEFAULT 0                | Số tiền được giảm giá                                                                                 |
| `final_price`        | `decimal`     |              | NOT NULL                 | Tổng tiền thực tế cần thanh toán                                                                       |
| `status`             | `string`      |              | NOT NULL                 | Trạng thái đơn (`HOLD`, `PENDING_PAYMENT`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `EXPIRED`) |
| `payment_method`     | `string`      |              | NULL                     | Phương thức thanh toán (CASH, VNPAY, MOMO, VIETQR, POINTS)                                               |
| `channel`            | `string`      |              | NOT NULL                 | Kênh đặt hàng (`ONLINE` hoặc `POS`)                                                                 |
| `expires_at`         | `datetime`    |              | NULL                     | Hạn chót thanh toán (phục vụ bộ đếm ngược giữ ghế)                                               |
| `created_at`         | `datetime`    |              | NOT NULL                 | Thời gian tạo đơn                                                                                        |
| `paid_at`            | `datetime`    |              | NULL                     | Thời gian thanh toán thành công                                                                          |
| `sold_by_user_id`    | `int`         | **FK** | NULL                     | Thu ngân thực hiện bán vé tại quầy                                                                    |
| `printed_at`         | `datetime`    |              | NULL                     | Mốc thời gian in vé giấy K80 đầu tiên                                                                 |
| `printed_by_user_id` | `int`         | **FK** | NULL                     | Nhân viên thực hiện in vé                                                                               |

---

### 21. `BOOKING_SEATS` — Chi tiết ghế & Snapshot giá vé

*Mô tả:* Lưu thông tin ghế trong đơn hàng kèm đóng băng giá vé (Snapshot bất biến lúc mua).

| Tên cột          | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                              |
| ------------------ | --------------- | :----------: | ------------------------ | -------------------------------------------------------------------- |
| `id`             | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh dòng ghế                                           |
| `booking_id`     | `int`         | **FK** | NOT NULL                 | Thuộc đơn đặt vé, tham chiếu`BOOKINGS(id)`                  |
| `seat_id`        | `int`         | **FK** | NOT NULL                 | Ghế được chọn, tham chiếu`SEATS(id)`                         |
| `ticket_type`    | `string`      |              | NOT NULL                 | Loại vé đối tượng (VD:`ADULT`, `STUDENT`, `ADULT,ADULT`) |
| `price_snapshot` | `decimal`     |              | NOT NULL                 | Đơn giá vé đóng băng tại thời điểm đặt                  |

---

### 22. `BOOKING_FNBS` — Chi tiết bắp nước mua kèm theo đơn vé

*Mô tả:* Lưu món F&B đặt cùng đơn vé kèm đóng băng tên món và giá tiền lúc mua.

| Tên cột              | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                             |
| ---------------------- | --------------- | :----------: | ------------------------ | --------------------------------------------------- |
| `id`                 | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh dòng F&B                           |
| `booking_id`         | `int`         | **FK** | NOT NULL                 | Thuộc đơn đặt vé, tham chiếu`BOOKINGS(id)` |
| `fnb_item_id`        | `int`         | **FK** | NOT NULL                 | Món F&B gốc, tham chiếu`FNB_ITEMS(id)`         |
| `item_name_snapshot` | `string`      |              | NOT NULL                 | Tên món đóng băng tại thời điểm mua        |
| `quantity`           | `int`         |              | NOT NULL                 | Số lượng mua                                     |
| `price_snapshot`     | `decimal`     |              | NOT NULL                 | Đơn giá gốc đóng băng                        |
| `surcharge_snapshot` | `decimal`     |              | DEFAULT 0                | Tổng phụ thu tùy chọn vị/nước đóng băng   |
| `total_price`        | `decimal`     |              | NOT NULL                 | Thành tiền của dòng món F&B                    |

---

### 23. `BOOKING_FNB_OPTIONS` — Tùy chọn vị / nước của món Combo mua kèm

*Mô tả:* Lưu chi tiết từng vị bắp hoặc loại nước được khách chọn cho món Combo trong đơn vé.

| Tên cột                | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                    |
| ------------------------ | --------------- | :----------: | ------------------------ | ---------------------------------------------------------- |
| `id`                   | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã tùy chọn                                             |
| `booking_fnb_id`       | `int`         | **FK** | NOT NULL                 | Thuộc dòng món F&B, tham chiếu`BOOKING_FNBS(id)`     |
| `option_item_id`       | `int`         | **FK** | NOT NULL                 | Tham chiếu tới`FNB_OPTION_ITEMS(id)`                   |
| `slot_label_snapshot`  | `string`      |              | NULL                     | Tên ô chọn đóng băng (VD: "Bắp", "Nước 1")        |
| `option_name_snapshot` | `string`      |              | NOT NULL                 | Tên tùy chọn đóng băng (VD: "Phô mai", "Coke Zero") |
| `surcharge_snapshot`   | `decimal`     |              | DEFAULT 0                | Phụ thu tùy chọn đóng băng                           |

---

### 24. `TICKETS` — Vé điện tử / Vé xem phim vào cổng

*Mô tả:* Đại diện cho từng vé xem phim cụ thể, chứa mã QR riêng biệt phục vụ quét mã tại cổng soát vé.

| Tên cột           | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                            |
| ------------------- | --------------- | :----------: | ------------------------ | -------------------------------------------------- |
| `id`              | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh vé                                |
| `booking_seat_id` | `int`         | **FK** | NOT NULL                 | Thuộc ghế nào, tham chiếu`BOOKING_SEATS(id)` |
| `qr_code`         | `string`      | **UK** | NOT NULL, UNIQUE         | Mã QR định danh soát vé độc lập            |
| `is_checked_in`   | `boolean`     |              | DEFAULT false            | Trạng thái đã qua cổng soát vé              |
| `check_in_time`   | `datetime`    |              | NULL                     | Mốc thời gian quét mã vào phòng chiếu       |
| `checked_in_by`   | `int`         | **FK** | NULL                     | Nhân viên soát vé thực hiện check-in         |
| `is_revoked`      | `boolean`     |              | DEFAULT false            | Trạng thái vé bị thu hồi                      |
| `created_at`      | `datetime`    |              | NOT NULL                 | Thời gian phát hành vé                         |

---

## PHÂN HỆ E: BẮP NƯỚC (F&B) & BÁN LẺ POS

### 25. `FNB_ITEMS` — Danh mục món bắp nước / Combo

*Mô tả:* Danh mục sản phẩm F&B (Món lẻ hoặc Combo nhiều ô chọn).

| Tên cột         | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                      |
| ----------------- | --------------- | :----------: | ------------------------ | ------------------------------------------------------------ |
| `id`            | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh món F&B                                     |
| `name`          | `string`      |              | NOT NULL                 | Tên món (Bắp rang bơ, Combo Solo, Combo Couple...)       |
| `type`          | `string`      |              | NOT NULL                 | Phân loại (`SINGLE` món lẻ hoặc `COMBO` gói combo) |
| `price`         | `decimal`     |              | NOT NULL                 | Giá bán niêm yết cơ sở (VNĐ)                          |
| `image_url`     | `string`      |              | NULL                     | Hình ảnh minh họa sản phẩm                              |
| `description`   | `text`        |              | NULL                     | Mô tả chi tiết thành phần món                          |
| `is_active`     | `boolean`     |              | DEFAULT true             | Trạng thái đang kinh doanh                                |
| `display_order` | `int`         |              | DEFAULT 0                | Thứ tự hiển thị trên Menu                               |

---

### 26. `FNB_OPTION_GROUPS` — Kho nhóm tùy chọn (Option Pool)

*Mô tả:* Quản lý các nhóm tùy chọn chung (VD: Nhóm Vị Bắp, Nhóm Loại Nước Ngọt, Nhóm Nước Đóng Chai).

| Tên cột       | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                  |
| --------------- | --------------- | :----------: | ------------------------ | ------------------------ |
| `id`          | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã nhóm tùy chọn     |
| `name`        | `string`      |              | NOT NULL                 | Tên nhóm tùy chọn    |
| `description` | `text`        |              | NULL                     | Mô tả nhóm            |
| `is_active`   | `boolean`     |              | DEFAULT true             | Trạng thái kích hoạt |

---

### 27. `FNB_OPTION_ITEMS` — Chi tiết từng tùy chọn trong nhóm

*Mô tả:* Từng lựa chọn cụ thể và mức phụ thu tương ứng (Vị Ngọt: +0đ, Phô Mai: +10.000đ, Caramel: +10.000đ).

| Tên cột         | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                           |
| ----------------- | --------------- | :----------: | ------------------------ | ----------------------------------------------------------------- |
| `id`            | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã tùy chọn                                                    |
| `group_id`      | `int`         | **FK** | NOT NULL                 | Thuộc nhóm tùy chọn, tham chiếu`FNB_OPTION_GROUPS(id)`     |
| `name`          | `string`      |              | NOT NULL                 | Tên tùy chọn (Ngọt, Mặn, Phô mai, Caramel, Coke, Sprite...) |
| `surcharge`     | `decimal`     |              | DEFAULT 0                | Phụ thu nâng cấp vị/món (VNĐ)                               |
| `is_default`    | `boolean`     |              | DEFAULT false            | Là tùy chọn mặc định                                        |
| `is_active`     | `boolean`     |              | DEFAULT true             | Trạng thái khả dụng                                           |
| `display_order` | `int`         |              | DEFAULT 0                | Thứ tự hiển thị                                               |

---

### 28. `FNB_ITEM_SLOTS` — Cấu hình các ô chọn cho món Combo

*Mô tả:* Cấu hình động chuẩn CGV/Lotte cho món Combo (VD: Combo Couple có 1 Slot Bắp max 2 vị + 2 Slot Nước riêng biệt).

| Tên cột                  | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                            |
| -------------------------- | --------------- | :----------: | ------------------------ | ------------------------------------------------------------------ |
| `id`                     | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã ô chọn Combo                                                 |
| `fnb_item_id`            | `int`         | **FK** | NOT NULL                 | Thuộc món Combo nào, tham chiếu`FNB_ITEMS(id)`               |
| `option_group_id`        | `int`         | **FK** | NOT NULL                 | Nguồn pool tùy chọn, tham chiếu`FNB_OPTION_GROUPS(id)`       |
| `default_option_item_id` | `int`         | **FK** | NULL                     | Tùy chọn chọn sẵn khi mở modal                                |
| `slot_label`             | `string`      |              | NOT NULL                 | Nhãn ô chọn (VD: "Bắp rang", "Nước 1", "Nước 2")           |
| `display_order`          | `int`         |              | DEFAULT 0                | Thứ tự hiển thị ô chọn                                       |
| `min_choices`            | `int`         |              | DEFAULT 1                | Số lượng tùy chọn tối thiểu                                 |
| `max_choices`            | `int`         |              | DEFAULT 1                | Số lượng tùy chọn tối đa (VD: = 2 cho phép mix 2 vị bắp) |
| `is_required`            | `boolean`     |              | DEFAULT true             | Bắt buộc phải chọn trước khi thêm vào giỏ                 |

---

### 29. `CONCESSION_SALES` — Hóa đơn bán lẻ F&B độc lập tại quầy POS

*Mô tả:* Giao dịch khi khách chỉ mua bắp nước tại quầy thu ngân F&B mà không mua vé xem phim.

| Tên cột          | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                        |
| ------------------ | --------------- | :----------: | ------------------------ | -------------------------------------------------------------- |
| `id`             | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh hóa đơn F&B                                 |
| `sale_code`      | `string`      | **UK** | NOT NULL, UNIQUE         | Mã tra cứu hóa đơn (VD:`CS260902-1234`)                 |
| `cinema_id`      | `int`         | **FK** | NOT NULL                 | Cơ sở rạp phát sinh giao dịch, tham chiếu`CINEMAS(id)` |
| `customer_id`    | `int`         | **FK** | NULL                     | Khách hàng thành viên tích điểm (nếu có)              |
| `seller_user_id` | `int`         | **FK** | NOT NULL                 | Thu ngân F&B bán hàng, tham chiếu`USERS(id)`             |
| `total_price`    | `decimal`     |              | NOT NULL                 | Tổng giá trị thanh toán (VNĐ)                             |
| `status`         | `string`      |              | NOT NULL                 | Trạng thái (`CONFIRMED`, `VOIDED`)                       |
| `payment_method` | `string`      |              | NOT NULL                 | Phương thức thanh toán (CASH, VIETQR, CARD...)             |
| `created_at`     | `datetime`    |              | NOT NULL                 | Thời gian lập hóa đơn                                     |

---

### 30. `CONCESSION_SALE_ITEMS` — Chi tiết món trên hóa đơn bán lẻ F&B

*Mô tả:* Lưu các món bắp nước kèm đóng băng giá trong hóa đơn bán lẻ POS.

| Tên cột               | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                    |
| ----------------------- | --------------- | :----------: | ------------------------ | ---------------------------------------------------------- |
| `id`                  | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh dòng món                                 |
| `concession_sale_id`  | `int`         | **FK** | NOT NULL                 | Thuộc hóa đơn F&B, tham chiếu`CONCESSION_SALES(id)` |
| `fnb_item_id`         | `int`         | **FK** | NOT NULL                 | Tham chiếu tới`FNB_ITEMS(id)`                          |
| `item_name_snapshot`  | `string`      |              | NOT NULL                 | Tên món đóng băng                                     |
| `quantity`            | `int`         |              | NOT NULL                 | Số lượng bán                                           |
| `unit_price_snapshot` | `decimal`     |              | NOT NULL                 | Đơn giá gốc đóng băng                               |
| `surcharge_snapshot`  | `decimal`     |              | DEFAULT 0                | Tổng phụ thu tùy chọn vị đóng băng                 |
| `total_price`         | `decimal`     |              | NOT NULL                 | Thành tiền dòng món                                    |

---

### 31. `CONCESSION_SALE_ITEM_OPTIONS` — Tùy chọn vị / nước của hóa đơn bán lẻ F&B

*Mô tả:* Lưu chi tiết các vị bắp nước của món Combo bán lẻ tại quầy POS.

| Tên cột                   | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                       |
| --------------------------- | --------------- | :----------: | ------------------------ | --------------------------------------------- |
| `id`                      | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã tùy chọn bán lẻ                       |
| `concession_sale_item_id` | `int`         | **FK** | NOT NULL                 | Tham chiếu tới`CONCESSION_SALE_ITEMS(id)` |
| `option_item_id`          | `int`         | **FK** | NOT NULL                 | Tham chiếu tới`FNB_OPTION_ITEMS(id)`      |
| `slot_label_snapshot`     | `string`      |              | NULL                     | Nhãn ô chọn đóng băng                   |
| `option_name_snapshot`    | `string`      |              | NOT NULL                 | Tên tùy chọn đóng băng                  |
| `surcharge_snapshot`      | `decimal`     |              | DEFAULT 0                | Mức phụ thu đóng băng                    |

---

## PHÂN HỆ F: CẤU HÌNH & DANH MỤC HỆ THỐNG

### 32. `SYSTEM_SETTINGS` — Cấu hình thông số toàn hệ thống

*Mô tả:* Bảng cặp Key-Value lưu trữ các cấu hình vận hành rạp phim động.

| Tên cột       | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                       |
| --------------- | --------------- | :----------: | ------------------------ | ------------------------------------------------------------- |
| `id`          | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã cấu hình                                                |
| `key_name`    | `string`      | **UK** | NOT NULL, UNIQUE         | Khóa cấu hình (`SEAT_HOLD_MINUTES`, `BANK_ACCOUNT`...) |
| `key_value`   | `text`        |              | NOT NULL                 | Giá trị cấu hình                                          |
| `description` | `string`      |              | NULL                     | Giải thích ý nghĩa thông số cấu hình                  |
| `updated_at`  | `datetime`    |              | NOT NULL                 | Thời gian cập nhật                                         |

---

### 33. `HOLIDAYS` — Danh mục ngày lễ / Tết

*Mô tả:* Danh sách các ngày lễ phục vụ tự động áp dụng biểu giá vé ngày lễ (`PRICING_RULES`).

| Tên cột        | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                                    |
| ---------------- | --------------- | :----------: | ------------------------ | -------------------------------------------------------------------------- |
| `id`           | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã ngày lễ                                                              |
| `name`         | `string`      |              | NOT NULL                 | Tên ngày lễ (Tết Dương Lịch, Giỗ Tổ Hùng Vương, 30/4 - 1/5...) |
| `date`         | `date`        | **UK** | NOT NULL, UNIQUE         | Ngày dương lịch diễn ra lễ                                           |
| `is_recurring` | `boolean`     |              | DEFAULT false            | Lặp lại hàng năm                                                       |

---

### 34. `FAQS` — Câu hỏi thường gặp & Trợ giúp

*Mô tả:* Nội dung câu hỏi trợ giúp hiển thị cho khách hàng tại trang FAQ.

| Tên cột         | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                                |
| ----------------- | --------------- | :----------: | ------------------------ | ---------------------------------------------------------------------- |
| `id`            | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã câu hỏi                                                          |
| `question`      | `text`        |              | NOT NULL                 | Nội dung câu hỏi                                                    |
| `answer`        | `text`        |              | NOT NULL                 | Nội dung câu trả lời                                               |
| `category`      | `string`      |              | NOT NULL                 | Phân mục câu hỏi (Vé, Thanh toán, Thành viên, Quy định rạp) |
| `display_order` | `int`         |              | DEFAULT 0                | Thứ tự hiển thị                                                    |
| `is_active`     | `boolean`     |              | DEFAULT true             | Trạng thái hiển thị                                                |

---

## PHÂN HỆ G: KHUYẾN MÃI, MARKETING & NỘI DUNG

### 35. `PROMOTIONS` — Chương trình khuyến mãi & Mã giảm giá

*Mô tả:* Quản lý các chương trình ưu đãi, voucher giảm giá %, giảm tiền cố định và voucher riêng tư.

| Tên cột                | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                                     |
| ------------------------ | --------------- | :----------: | ------------------------ | --------------------------------------------------------------------------- |
| `id`                   | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã khuyến mãi                                                            |
| `code`                 | `string`      | **UK** | NOT NULL, UNIQUE         | Mã khuyến mãi nhập lúc đặt vé (VD:`CHAOBANMOI`, `DEVCINE50K`)   |
| `name`                 | `string`      |              | NOT NULL                 | Tên chương trình khuyến mãi                                           |
| `description`          | `text`        |              | NULL                     | Mô tả chi tiết thể lệ chương trình                                  |
| `discount_type`        | `string`      |              | NOT NULL                 | Loại giảm giá (`PERCENTAGE` % hoặc `FIXED_AMOUNT` tiền cố định) |
| `discount_value`       | `decimal`     |              | NOT NULL                 | Giá trị giảm (% hoặc số tiền VNĐ)                                    |
| `max_discount_amount`  | `decimal`     |              | NULL                     | Mức giảm tối đa (áp dụng khi giảm theo %)                            |
| `min_order_value`      | `decimal`     |              | DEFAULT 0                | Giá trị đơn hàng tối thiểu để được áp dụng                    |
| `max_ticket_quantity`  | `int`         |              | DEFAULT 0                | Số vé tối đa được giảm / đơn (0 = toàn bộ đơn)                |
| `applicable_movie_ids` | `text`        |              | NULL                     | Danh sách ID các phim áp dụng (chuỗi CSV)                              |
| `usage_limit`          | `int`         |              | DEFAULT 0                | Tổng ngân sách lượt dùng toàn hệ thống (0 = không giới hạn)     |
| `used_count`           | `int`         |              | DEFAULT 0                | Số lượt đã sử dụng thực tế                                         |
| `is_hidden`            | `boolean`     |              | DEFAULT false            | Cờ Voucher riêng tư (Private / Ẩn khỏi trang công khai)               |
| `start_date`           | `datetime`    |              | NOT NULL                 | Ngày bắt đầu áp dụng                                                  |
| `end_date`             | `datetime`    |              | NOT NULL                 | Ngày kết thúc khuyến mãi                                               |
| `is_active`            | `boolean`     |              | DEFAULT true             | Trạng thái kích hoạt                                                    |
| `created_at`           | `datetime`    |              | NOT NULL                 | Thời gian tạo khuyến mãi                                                |

---

### 36. `VOUCHERS` — Ví voucher cá nhân của thành viên

*Mô tả:* Từng mã voucher được phát riêng vào ví của khách hàng (Quà tặng, Đổi điểm, Bù sự cố).

| Tên cột        | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                         |
| ---------------- | --------------- | :----------: | ------------------------ | --------------------------------------------------------------- |
| `id`           | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh voucher trong ví                               |
| `customer_id`  | `int`         | **FK** | NOT NULL                 | Chủ sở hữu voucher, tham chiếu`CUSTOMERS(user_id)`        |
| `promotion_id` | `int`         | **FK** | NOT NULL                 | Thuộc chương trình ưu đãi, tham chiếu`PROMOTIONS(id)` |
| `code`         | `string`      | **UK** | NOT NULL, UNIQUE         | Mã voucher duy nhất của khách                               |
| `is_used`      | `boolean`     |              | DEFAULT false            | Trạng thái đã sử dụng                                     |
| `used_at`      | `datetime`    |              | NULL                     | Mốc thời gian sử dụng                                       |
| `valid_from`   | `datetime`    |              | NOT NULL                 | Hiệu lực từ ngày                                            |
| `valid_until`  | `datetime`    |              | NOT NULL                 | Hạn sử dụng của voucher                                     |
| `created_at`   | `datetime`    |              | NOT NULL                 | Thời gian cấp voucher                                         |

---

### 37. `PROMO_ARTICLES` — Tin tức & Bài viết khuyến mãi

*Mô tả:* Các bài viết truyền thông, sự kiện ưu đãi hiển thị trên trang Tin tức/Khuyến mãi.

| Tên cột         | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                |
| ----------------- | --------------- | :----------: | ------------------------ | -------------------------------------- |
| `id`            | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã bài viết                         |
| `title`         | `string`      |              | NOT NULL                 | Tiêu đề bài viết                  |
| `slug`          | `string`      | **UK** | NOT NULL, UNIQUE         | Đường dẫn thân thiện SEO         |
| `thumbnail_url` | `string`      |              | NULL                     | Ảnh thumbnail đại diện             |
| `content`       | `text`        |              | NOT NULL                 | Nội dung bài viết (HTML / Markdown) |
| `is_active`     | `boolean`     |              | DEFAULT true             | Trạng thái công khai                |
| `display_order` | `int`         |              | DEFAULT 0                | Thứ tự hiển thị                    |
| `created_at`    | `datetime`    |              | NOT NULL                 | Thời gian đăng bài                 |
| `updated_at`    | `datetime`    |              | NULL                     | Thời gian chỉnh sửa                 |

---

### 38. `PROMO_EMAIL_LOG` — Nhật ký gửi email chiến dịch Marketing

*Mô tả:* Ghi nhận lịch sử gửi email chiến dịch tới khách hàng, đảm bảo chống gửi trùng lặp (Dedup).

| Tên cột        | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                             |
| ---------------- | --------------- | :----------: | ------------------------ | ----------------------------------- |
| `id`           | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã bản ghi log gửi email         |
| `promotion_id` | `int`         | **UK** | NOT NULL                 | Chiến dịch khuyến mãi đã gửi |
| `customer_id`  | `int`         | **UK** | NOT NULL                 | Khách hàng nhận email            |
| `sent_at`      | `datetime`    |              | NOT NULL                 | Mốc thời gian gửi thành công   |

---

### 39. `BANNERS` — Banner quảng cáo & Phim nổi bật trang chủ

*Mô tả:* Quản lý Banner Carousel trên trang chủ (Chế độ Banner ảnh tĩnh hoặc Banner gắn kèm Phim đang chiếu).

| Tên cột         | Kiểu dữ liệu |    Khóa    | Ràng buộc              | Mô tả                                                       |
| ----------------- | --------------- | :----------: | ------------------------ | ------------------------------------------------------------- |
| `id`            | `int`         | **PK** | NOT NULL, AUTO_INCREMENT | Mã định danh banner                                        |
| `title`         | `string`      |              | NOT NULL                 | Tiêu đề banner                                             |
| `image_url`     | `string`      |              | NOT NULL                 | Đường dẫn ảnh banner khổ lớn                           |
| `link_url`      | `string`      |              | NULL                     | Đường dẫn chuyển hướng khi click                       |
| `mode`          | `string`      |              | NOT NULL                 | Chế độ banner (`IMAGE` hoặc `MOVIE`)                  |
| `movie_id`      | `int`         | **FK** | NULL                     | Phim liên kết (khi mode = MOVIE), tham chiếu`MOVIES(id)` |
| `display_order` | `int`         |              | NOT NULL                 | Thứ tự hiển thị liên tục (chuỗi 1..N)                  |
| `is_active`     | `boolean`     |              | DEFAULT true             | Trạng thái đang hiển thị                                 |
| `created_at`    | `datetime`    |              | NOT NULL                 | Thời gian tạo banner                                        |
| `updated_at`    | `datetime`    |              | NULL                     | Thời gian cập nhật                                         |

---

## TỔNG HỢP CÁC BẢNG ĐÃ LOẠI BỎ (5 BẢNG)

Để giữ kiến trúc sạch sẽ và phản ánh chính xác hệ thống vận hành thực tế, 5 bảng sau đã được loại bỏ hoàn toàn:

1. `SUPPORT_TICKETS` *(Tính năng CSKH gửi ticket không phát triển phía Frontend)*
2. `NOTIFICATIONS` *(Thông báo In-App chưa hoàn thiện chuông trên Navbar, tạm ẩn)*
3. `SEAT_INCIDENTS` *(Xử lý sự cố đổi ghế đã bị tắt ở runtime bằng `@Profile("never")`)*
4. `TICKET_QR_HISTORIES` *(Lưu QR cũ khi đổi ghế sự cố, phụ thuộc vào `SEAT_INCIDENTS`)*
5. `APPROVAL_REQUESTS` *(Phê duyệt sửa sai hủy hóa đơn F&B đã được ghi chú bỏ và comment router)*
