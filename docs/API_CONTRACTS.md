# API Contracts — DevCine

> **Version:** 1.0  
> **Base URL:** `http://localhost:8080/api`  
> **Auth:** JWT Bearer Token  
> **Content-Type:** `application/json`

---

## Quy Ước Chung

### Response Format (Success)
```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": { },
  "timestamp": "2026-05-26T22:00:00+07:00"
}
```

### Response Format (Error)
```json
{
  "success": false,
  "message": "Mô tả lỗi",
  "errorCode": "VALIDATION_ERROR",
  "errors": [{ "field": "email", "message": "Email không hợp lệ" }],
  "timestamp": "2026-05-26T22:00:00+07:00"
}
```

### Pagination Format
```json
{
  "success": true,
  "data": {
    "content": [ ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

### Auth Levels
- 🔓 **PUBLIC** — Không cần token
- 🔐 **AUTH** — Cần JWT token (mọi role)
- 👤 **CUSTOMER** — Chỉ khách hàng
- 👷 **STAFF** — Nhân viên + Manager + Admin
- 🏢 **MANAGER** — Manager + Admin
- 🛡️ **ADMIN** — Chỉ Admin

---

## 1. Authentication (`/api/auth`)

### POST `/api/auth/register` 🔓
Đăng ký tài khoản khách hàng mới.

**Request:**
```json
{
  "username": "nguyenvana",
  "email": "vana@gmail.com",
  "password": "P@ssw0rd123",
  "phone": "0901234567"
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "nguyenvana",
    "email": "vana@gmail.com",
    "role": "CUSTOMER"
  }
}
```

---

### POST `/api/auth/login` 🔓
Đăng nhập lấy JWT token.

**Request:**
```json
{
  "username": "nguyenvana",
  "password": "P@ssw0rd123"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 604800000,
    "user": {
      "id": 1,
      "username": "nguyenvana",
      "email": "vana@gmail.com",
      "role": "CUSTOMER"
    }
  }
}
```

---

### POST `/api/auth/refresh` 🔐
Làm mới access token.

---

### GET `/api/auth/me` 🔐
Lấy thông tin user hiện tại.

---

## 2. Users (`/api/users`)

### GET `/api/users` 🛡️
Danh sách tất cả users (pagination + search).

**Query params:** `?page=0&size=20&search=nguyen&role=CUSTOMER&isActive=true`

---

### GET `/api/users/{id}` 🛡️
Chi tiết một user.

---

### PUT `/api/users/{id}` 🛡️
Cập nhật user (admin có thể đổi role, toggle active).

**Request:**
```json
{
  "email": "newemail@gmail.com",
  "phone": "0912345678",
  "isActive": true,
  "roleId": 2
}
```

---

### DELETE `/api/users/{id}` 🛡️
Vô hiệu hóa user (soft delete: set is_active = false).

---

## 3. Movies (`/api/movies`)

### GET `/api/movies` 🔓
Danh sách phim (filter theo status, category).

**Query params:** `?page=0&size=20&status=NOW_SHOWING&categoryId=1&search=avengers`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Avengers: Endgame",
        "slug": "avengers-endgame",
        "durationMins": 181,
        "ageRating": "C13",
        "releaseDate": "2026-05-01",
        "status": "NOW_SHOWING",
        "categories": [
          { "id": 1, "name": "Hành động" },
          { "id": 3, "name": "Khoa học viễn tưởng" }
        ]
      }
    ],
    "totalElements": 25
  }
}
```

---

### GET `/api/movies/{id}` 🔓
Chi tiết phim + suất chiếu sắp tới.

---

### POST `/api/movies` 🏢
Thêm phim mới.

**Request:**
```json
{
  "title": "Avengers: Endgame",
  "slug": "avengers-endgame",
  "durationMins": 181,
  "ageRating": "C13",
  "releaseDate": "2026-05-01",
  "endDate": "2026-07-01",
  "status": "COMING_SOON",
  "categoryIds": [1, 3]
}
```

---

### PUT `/api/movies/{id}` 🏢
Cập nhật thông tin phim.

---

### DELETE `/api/movies/{id}` 🛡️
Xóa phim (chỉ khi chưa có suất chiếu).

---

## 4. Categories (`/api/categories`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/categories` | 🔓 | Danh sách thể loại |
| POST | `/api/categories` | 🏢 | Thêm thể loại |
| PUT | `/api/categories/{id}` | 🏢 | Sửa thể loại |
| DELETE | `/api/categories/{id}` | 🛡️ | Xóa thể loại |

---

## 5. Cinemas (`/api/cinemas`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/cinemas` | 🔓 | Danh sách rạp |
| GET | `/api/cinemas/{id}` | 🔓 | Chi tiết rạp + phòng chiếu |
| POST | `/api/cinemas` | 🛡️ | Thêm rạp |
| PUT | `/api/cinemas/{id}` | 🛡️ | Sửa rạp |
| DELETE | `/api/cinemas/{id}` | 🛡️ | Xóa rạp |

---

## 6. Rooms (`/api/rooms`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/cinemas/{cinemaId}/rooms` | 🔓 | Phòng chiếu theo rạp |
| GET | `/api/rooms/{id}` | 🔓 | Chi tiết phòng + sơ đồ ghế |
| POST | `/api/rooms` | 🏢 | Thêm phòng |
| PUT | `/api/rooms/{id}` | 🏢 | Sửa phòng |

---

## 7. Seats (`/api/seats`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/rooms/{roomId}/seats` | 🔓 | Sơ đồ ghế của phòng |
| POST | `/api/rooms/{roomId}/seats/batch` | 🏢 | Tạo hàng loạt ghế |
| PUT | `/api/seats/{id}` | 🏢 | Sửa ghế (đổi type, toggle active) |

---

## 8. Seat Types (`/api/seat-types`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/seat-types` | 🔓 | Danh sách loại ghế |
| POST | `/api/seat-types` | 🛡️ | Thêm loại ghế |
| PUT | `/api/seat-types/{id}` | 🛡️ | Sửa loại ghế |

---

## 9. Movie Formats (`/api/movie-formats`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/movie-formats` | 🔓 | Danh sách định dạng chiếu |
| POST | `/api/movie-formats` | 🛡️ | Thêm định dạng |
| PUT | `/api/movie-formats/{id}` | 🛡️ | Sửa định dạng |

---

## 10. Showtimes (`/api/showtimes`)

### GET `/api/showtimes` 🔓
Danh sách suất chiếu (filter theo phim, rạp, ngày).

**Query params:** `?movieId=1&cinemaId=1&date=2026-05-26`

---

### GET `/api/showtimes/{id}/seats` 🔓
Sơ đồ ghế với trạng thái (available / booked / selected).

**Response 200:**
```json
{
  "success": true,
  "data": {
    "showtimeId": 1,
    "room": { "id": 1, "name": "Room 1" },
    "seats": [
      {
        "id": 1,
        "row": "A",
        "col": 1,
        "type": { "name": "Standard", "colorCode": "#4CAF50", "priceModifier": 1.0 },
        "status": "AVAILABLE",
        "price": 90000
      },
      {
        "id": 2,
        "row": "A",
        "col": 2,
        "type": { "name": "VIP", "colorCode": "#FF9800", "priceModifier": 1.5 },
        "status": "BOOKED",
        "price": 135000
      }
    ]
  }
}
```

---

### POST `/api/showtimes` 🏢
Tạo suất chiếu mới.

**Request:**
```json
{
  "movieId": 1,
  "roomId": 1,
  "formatId": 1,
  "startTime": "2026-05-27T19:00:00",
  "status": "SCHEDULED"
}
```

---

### PUT `/api/showtimes/{id}` 🏢
Sửa suất chiếu.

---

### DELETE `/api/showtimes/{id}` 🏢
Hủy suất chiếu (chỉ khi chưa có booking).

---

## 11. Bookings (`/api/bookings`)

### POST `/api/bookings` 👤
Đặt vé (seat + F&B).

**Request:**
```json
{
  "showtimeId": 1,
  "seatIds": [1, 2, 3],
  "fnbItems": [
    { "fnbItemId": 1, "quantity": 2 },
    { "fnbItemId": 5, "quantity": 1 }
  ],
  "voucherId": null,
  "paymentMethod": "WALLET"
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "bookingId": 100,
    "totalAmount": 450000,
    "seats": [
      { "id": 1, "row": "A", "col": 1, "price": 90000 },
      { "id": 2, "row": "A", "col": 2, "price": 135000 },
      { "id": 3, "row": "A", "col": 3, "price": 90000 }
    ],
    "fnbItems": [
      { "name": "Bắp rang bơ", "quantity": 2, "price": 45000 },
      { "name": "Combo Couple", "quantity": 1, "price": 89000 }
    ],
    "tickets": [
      { "id": 1, "qrCode": "DEVCINE-T001-20260527", "seat": "A1" },
      { "id": 2, "qrCode": "DEVCINE-T002-20260527", "seat": "A2" },
      { "id": 3, "qrCode": "DEVCINE-T003-20260527", "seat": "A3" }
    ]
  }
}
```

---

### GET `/api/bookings/my` 👤
Lịch sử đặt vé của customer hiện tại.

---

## 12. Tickets (`/api/tickets`)

### POST `/api/tickets/check-in` 👷
Check-in vé bằng QR code.

**Request:**
```json
{
  "qrCode": "DEVCINE-T001-20260527"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "ticketId": 1,
    "seat": "A1",
    "movie": "Avengers: Endgame",
    "showtime": "19:00 - 22:01",
    "room": "Room 1",
    "isAgeVerified": false,
    "checkedInAt": "2026-05-27T18:45:00"
  }
}
```

---

### PUT `/api/tickets/{id}/verify-age` 👷
Xác minh tuổi cho phim 18+.

---

## 13. Reviews (`/api/reviews`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/movies/{movieId}/reviews` | 🔓 | Đánh giá của phim |
| POST | `/api/reviews` | 👤 | Thêm đánh giá (cần có ticket) |
| PUT | `/api/reviews/{id}` | 👤 | Sửa đánh giá (chỉ owner) |
| DELETE | `/api/reviews/{id}` | 👤/🛡️ | Xóa đánh giá |

---

## 14. F&B Items (`/api/fnb-items`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/fnb-items` | 🔓 | Danh sách F&B (filter type) |
| POST | `/api/fnb-items` | 🏢 | Thêm sản phẩm |
| PUT | `/api/fnb-items/{id}` | 🏢 | Sửa sản phẩm |
| DELETE | `/api/fnb-items/{id}` | 🛡️ | Xóa sản phẩm |

---

## 15. Inventory (`/api/inventory`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/cinemas/{cinemaId}/inventory` | 👷 | Tồn kho theo rạp |
| PUT | `/api/inventory/{id}/adjust` | 👷 | Cập nhật tồn kho (+/-) |
| GET | `/api/inventory/{id}/logs` | 🏢 | Lịch sử thay đổi tồn kho |

---

## 16. Wallets (`/api/wallets`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/wallets/my` | 👤 | Thông tin ví cá nhân |
| POST | `/api/wallets/deposit` | 👤 | Nạp tiền |
| GET | `/api/wallets/my/transactions` | 👤 | Lịch sử giao dịch |

---

## 17. Promotions (`/api/promotions`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/promotions` | 🔓 | KM đang hoạt động |
| GET | `/api/promotions/{id}` | 🔓 | Chi tiết KM |
| POST | `/api/promotions` | 🏢 | Tạo KM |
| PUT | `/api/promotions/{id}` | 🏢 | Sửa KM |
| DELETE | `/api/promotions/{id}` | 🛡️ | Xóa KM |
| POST | `/api/promotions/validate` | 👤 | Kiểm tra mã KM hợp lệ |

---

## 18. Vouchers (`/api/vouchers`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/vouchers/my` | 👤 | Voucher của tôi |
| POST | `/api/vouchers/issue` | 🏢 | Phát voucher cho customer |

---

## 19. Staffs (`/api/staffs`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/staffs` | 🏢 | Danh sách nhân viên |
| GET | `/api/staffs/{id}` | 🏢 | Chi tiết nhân viên |
| POST | `/api/staffs` | 🛡️ | Thêm nhân viên |
| PUT | `/api/staffs/{id}` | 🏢 | Sửa nhân viên |

---

## 20. Shifts & Schedules (`/api/shifts`, `/api/schedules`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/shifts` | 👷 | Danh sách ca |
| POST | `/api/shifts` | 🏢 | Tạo ca |
| GET | `/api/schedules` | 👷 | Lịch trực (filter date, staff) |
| POST | `/api/schedules` | 🏢 | Xếp lịch trực |
| PUT | `/api/schedules/{id}` | 🏢 | Sửa lịch |

---

## 21. Shift Handovers (`/api/handovers`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| POST | `/api/handovers` | 👷 | Tạo bàn giao ca |
| PUT | `/api/handovers/{id}/approve` | 🏢 | Duyệt bàn giao |
| GET | `/api/handovers` | 🏢 | Danh sách bàn giao |

---

## 22. Support Tickets (`/api/support-tickets`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/support-tickets` | 👷 | Danh sách ticket (staff) |
| GET | `/api/support-tickets/my` | 👤 | Ticket của tôi |
| POST | `/api/support-tickets` | 👤 | Tạo ticket |
| PUT | `/api/support-tickets/{id}/assign` | 🏢 | Gán NV phụ trách |
| PUT | `/api/support-tickets/{id}/resolve` | 👷 | Đánh dấu đã xử lý |

---

## 23. Banners (`/api/banners`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/banners` | 🔓 | Banner đang hiển thị |
| GET | `/api/banners/all` | 🏢 | Tất cả banner (kể cả hết hạn) |
| POST | `/api/banners` | 🏢 | Thêm banner |
| PUT | `/api/banners/{id}` | 🏢 | Sửa banner |
| DELETE | `/api/banners/{id}` | 🏢 | Xóa banner |

---

## 24. Pricing Rules (`/api/pricing-rules`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/pricing-rules` | 🏢 | Danh sách quy tắc giá |
| POST | `/api/pricing-rules` | 🛡️ | Thêm quy tắc |
| PUT | `/api/pricing-rules/{id}` | 🛡️ | Sửa quy tắc |
| DELETE | `/api/pricing-rules/{id}` | 🛡️ | Xóa quy tắc |

---

## 25. Audit Logs (`/api/audit-logs`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/audit-logs` | 🛡️ | Lịch sử thao tác hệ thống |

**Query params:** `?userId=1&action=DELETE&targetTable=movies&from=2026-05-01&to=2026-05-26`

---

## 26. Lost & Found (`/api/lost-and-founds`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/lost-and-founds` | 👷 | Danh sách đồ thất lạc |
| POST | `/api/lost-and-founds` | 👷 | Ghi nhận đồ tìm thấy |
| PUT | `/api/lost-and-founds/{id}/claim` | 👷 | Đánh dấu đã trả |

---

## 27. BOM Recipes (`/api/bom-recipes`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/fnb-items/{comboId}/recipes` | 🏢 | Công thức combo |
| POST | `/api/bom-recipes` | 🏢 | Thêm nguyên liệu vào combo |
| DELETE | `/api/bom-recipes/{id}` | 🏢 | Xóa nguyên liệu khỏi combo |

---

## 28. Customers (Admin) (`/api/customers`)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/customers` | 🏢 | Danh sách khách hàng |
| GET | `/api/customers/{id}` | 🏢 | Chi tiết KH + loyalty info |
| PUT | `/api/customers/{id}/tier` | 🛡️ | Cập nhật hạng thành viên |
| PUT | `/api/customers/{id}/points` | 🏢 | Điều chỉnh điểm tích lũy |
