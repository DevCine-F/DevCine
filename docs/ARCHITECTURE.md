# Kiến Trúc Hệ Thống — DevCine

> **Version:** 1.0  
> **Cập nhật:** 2026-05-26  
> **Tác giả:** AI Agent + Tech Lead

---

## 1. Tổng Quan Dự Án

**DevCine** là hệ thống quản lý rạp chiếu phim toàn diện, bao gồm:
- Quản lý phim, suất chiếu, phòng chiếu, ghế ngồi
- Đặt vé online, thanh toán qua ví điện tử
- Quản lý F&B (đồ ăn/thức uống), kho hàng
- Quản lý nhân viên, ca trực, bàn giao ca
- Chương trình khuyến mãi, voucher, loyalty points
- Hỗ trợ khách hàng (support ticket)
- Quản trị nội dung (banner, thông báo)

---

## 2. Tech Stack

### 2.1 Backend

| Công nghệ | Version | Lý do chọn |
|-----------|---------|-------------|
| **Java** | 21 (LTS) | Ổn định, hiệu năng cao, hệ sinh thái lớn, phù hợp enterprise |
| **Spring Boot** | 4.0.6 | Framework Java phổ biến nhất, tích hợp sẵn security, JPA, validation |
| **Spring Data JPA** | — | ORM mạnh mẽ, giảm boilerplate code truy vấn DB |
| **Spring Security** | — | Xử lý authentication/authorization chuẩn enterprise |
| **Hibernate** | 7.x | JPA implementation, DDL auto-generation, lazy loading |
| **Lombok** | — | Giảm boilerplate (getter/setter/builder), tăng readability |
| **PostgreSQL** | 15+ | RDBMS mạnh, hỗ trợ JSON, full-text search, ACID compliance |

### 2.2 Frontend

| Công nghệ | Version | Lý do chọn |
|-----------|---------|-------------|
| **Vue.js** | 3.5 | Reactive, dễ học, Composition API mạnh mẽ |
| **Vite** | 8.x | Build tool nhanh nhất hiện tại, HMR tức thì |
| **Pinia** | 3.x | State management chính thức cho Vue 3, TypeScript-friendly |
| **Vue Router** | 5.x | SPA routing, navigation guards cho auth |
| **TailwindCSS** | 4.x | Utility-first CSS, thiết kế nhanh, responsive dễ dàng |

### 2.3 Infrastructure

| Công nghệ | Mục đích |
|-----------|----------|
| **Supabase** (PostgreSQL) | Database hosting — managed PostgreSQL trên AWS Singapore |
| **JWT** | Stateless authentication — access token + refresh token |
| **Maven** | Build tool + dependency management cho Java |
| **Git** | Version control |

---

## 3. Sơ Đồ Kiến Trúc Tổng Thể

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Vue.js SPA  │  │  Admin Panel │  │  Staff Terminal   │  │
│  │  (Customer)  │  │  (Manager)   │  │  (Check-in/POS)  │  │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
│         │                 │                    │            │
│         └─────────────────┼────────────────────┘            │
│                           │ HTTP/REST (JSON)                │
└───────────────────────────┼─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      API GATEWAY (Spring Boot :8080)        │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │ CORS Filter │→ │ JWT Filter   │→ │ Rate Limiter      │  │
│  └─────────────┘  └──────────────┘  └───────────────────┘  │
│                           │                                 │
│                           ▼                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                   CONTROLLER LAYER                   │   │
│  │  AuthController │ MovieController │ BookingController │   │
│  │  UserController │ ShowtimeCtrl    │ FnbController     │   │
│  │  CinemaCtrl     │ PromotionCtrl   │ StaffController   │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                         │                                   │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                    SERVICE LAYER                     │   │
│  │  Business Logic │ Validation │ Transaction Mgmt      │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                         │                                   │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                  REPOSITORY LAYER                    │   │
│  │  Spring Data JPA │ Custom Queries │ Specifications   │   │
│  └──────────────────────┬───────────────────────────────┘   │
└─────────────────────────┼───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    DATABASE LAYER                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │          Supabase PostgreSQL (AWS Singapore)         │   │
│  │              33 Tables │ Indexed │ ACID              │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Cấu Trúc Thư Mục

```
devcine/
├── docs/                              ← Tài liệu kỹ thuật
│   ├── ARCHITECTURE.md                ← File này
│   ├── DATABASE.md                    ← Schema + quan hệ
│   ├── API_CONTRACTS.md               ← API endpoints
│   ├── CRITICAL_PATHS.md              ← Luồng nghiệp vụ quan trọng
│   ├── SECURITY.md                    ← Quy tắc bảo mật
│   └── CHANGELOG.md                   ← Lịch sử thay đổi
├── RULES.md                           ← Quy tắc AI Agent (ĐỌC ĐẦU TIÊN)
│
├── devcine-backend/                   ← Spring Boot Backend
│   ├── src/main/java/com/devcine/backend/
│   │   ├── DevcineBackendApplication.java
│   │   ├── config/                    ← Security, CORS, JWT config
│   │   ├── controller/                ← REST API controllers
│   │   ├── dto/                       ← Request/Response DTOs
│   │   ├── entity/                    ← JPA Entities (33 files)
│   │   ├── enums/                     ← Enum types
│   │   ├── exception/                 ← Custom exceptions + handler
│   │   ├── repository/                ← Spring Data JPA repositories
│   │   ├── service/                   ← Business logic
│   │   └── util/                      ← Utility classes
│   ├── src/main/resources/
│   │   └── application.properties     ← App configuration
│   ├── .env                           ← Environment variables (KHÔNG commit)
│   └── pom.xml                        ← Maven dependencies
│
├── devcine-frontend/                  ← Vue.js Frontend
│   ├── src/
│   │   ├── components/                ← Reusable Vue components
│   │   ├── views/                     ← Page-level components
│   │   ├── layouts/                   ← Layout wrappers
│   │   ├── routers/                   ← Vue Router config
│   │   ├── stores/                    ← Pinia stores
│   │   ├── hooks/                     ← Composables
│   │   ├── utils/                     ← Helper functions
│   │   ├── i18n/                      ← Đa ngôn ngữ
│   │   ├── App.vue                    ← Root component
│   │   ├── main.js                    ← Entry point
│   │   └── index.css                  ← Global styles
│   ├── .env                           ← VITE_API_BASE_URL
│   ├── package.json
│   └── vite.config.js
│
└── .gitignore
```

---

## 5. Luồng Dữ Liệu Tổng Thể

### 5.1 Luồng Authentication

```
Client → POST /api/auth/login (username, password)
    → AuthController → AuthService
        → UserRepository.findByUsername()
        → BCrypt.verify(password, hash)
        → JwtUtil.generateToken(user)
    ← Response: { accessToken, refreshToken, user }
Client lưu token vào localStorage/cookie
    → Mọi request sau đều gửi kèm: Authorization: Bearer <token>
```

### 5.2 Luồng Đặt Vé

```
1. Customer chọn phim → GET /api/movies
2. Chọn suất chiếu   → GET /api/showtimes?movieId={id}
3. Chọn ghế           → GET /api/showtimes/{id}/seats (available seats)
4. Chọn F&B           → GET /api/fnb-items
5. Xác nhận đặt vé    → POST /api/bookings
   → BookingService:
      a. Lock ghế (optimistic/pessimistic locking)
      b. Tính giá (base + seat_type modifier + format surcharge + pricing rules)
      c. Áp khuyến mãi/voucher (nếu có)
      d. Trừ ví/thanh toán
      e. Tạo BookingSeat + BookingFnb records
      f. Generate QR code cho Ticket
6. Trả kết quả        ← Response: { booking, tickets[], qrCodes[] }
```

### 5.3 Luồng Check-in

```
1. Staff scan QR      → POST /api/tickets/check-in { qrCode }
   → TicketService:
      a. Tìm ticket bằng qr_code
      b. Verify: chưa check-in, suất chiếu đúng ngày
      c. Verify tuổi (nếu phim 18+) → is_age_verified
      d. Update: is_checked_in = true, checked_in_by, check_in_time
2. Trả kết quả        ← Response: { ticket, seatInfo, movieInfo }
```

---

## 6. Các Service Bên Ngoài

| Service | Mục đích | Ghi chú |
|---------|----------|---------|
| **Supabase** | PostgreSQL Database hosting | Region: AWS ap-southeast-1 (Singapore) |
| **JWT** | Token-based authentication | Self-issued, không cần 3rd party |
| *(Tương lai)* QR Code Generator | Generate QR cho vé | Có thể dùng thư viện Java local |
| *(Tương lai)* Email Service | Gửi email xác nhận đặt vé | SendGrid / SES |
| *(Tương lai)* Payment Gateway | Thanh toán online | VNPay / MoMo / ZaloPay |

---

## 7. Quy Ước Chung

### 7.1 Naming Convention

| Loại | Quy ước | Ví dụ |
|------|---------|-------|
| Entity class | PascalCase, số ít | `BookingSeat`, `FnbItem` |
| Table name | snake_case, số nhiều | `booking_seats`, `fnb_items` |
| Column name | snake_case | `created_at`, `price_snapshot` |
| Controller | PascalCase + Controller | `MovieController` |
| Service | PascalCase + Service | `MovieService` |
| Repository | PascalCase + Repository | `MovieRepository` |
| DTO | PascalCase + Request/Response | `CreateMovieRequest` |
| API path | kebab-case, số nhiều | `/api/fnb-items`, `/api/booking-seats` |
| Enum | UPPER_SNAKE_CASE | `TICKET_STATUS.CHECKED_IN` |

### 7.2 API Response Format

```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": { ... },
  "timestamp": "2026-05-26T22:00:00+07:00"
}
```

### 7.3 Error Response Format

```json
{
  "success": false,
  "message": "Mô tả lỗi",
  "errorCode": "RESOURCE_NOT_FOUND",
  "errors": [
    { "field": "email", "message": "Email không hợp lệ" }
  ],
  "timestamp": "2026-05-26T22:00:00+07:00"
}
```
