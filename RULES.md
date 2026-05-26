# Project Rules — DevCine
# Văn bản này là QUY TẮC BẮT BUỘC. AI Agent PHẢI đọc trước khi code.

---

## NGUYÊN TẮC TUYỆT ĐỐI

1. **KHÔNG BAO GIỜ** xoá hoặc viết lại code đang hoạt động mà không hỏi trước
2. **KHÔNG BAO GIỜ** dùng raw SQL — chỉ dùng Spring Data JPA / JPQL
3. **KHÔNG BAO GIỜ** skip validation đầu vào ở bất kỳ API nào
4. **KHÔNG BAO GIỜ** hardcode credentials, tokens, secrets vào source code
5. **PHẢI** đọc `docs/CRITICAL_PATHS.md` trước khi sửa bất kỳ file backend nào
6. **PHẢI** đọc `docs/DATABASE.md` trước khi thêm/sửa bảng hoặc quan hệ
7. **PHẢI** chạy build test trước khi báo "hoàn thành"
8. **PHẢI** dùng `@Transactional` cho mọi thao tác write liên quan đến tiền (Wallet, Booking)

---

## TECH STACK — KHÔNG ĐƯỢC THAY ĐỔI

| Layer | Công nghệ | Ghi chú |
|-------|-----------|---------|
| Backend | Java 21 + Spring Boot 4.0.6 | Maven build |
| ORM | Spring Data JPA + Hibernate 7 | KHÔNG raw SQL |
| Database | PostgreSQL (Supabase) | Region: ap-southeast-1 |
| Auth | JWT (Spring Security) | Access token 7 ngày |
| Frontend | Vue.js 3 + Vite 8 + Pinia 3 | TailwindCSS 4 |
| API Format | REST JSON | Chuẩn response format trong API_CONTRACTS.md |

---

## PROTECTED FILES — KHÔNG ĐƯỢC SỬA

Các file dưới đây chỉ được sửa khi có **yêu cầu rõ ràng** và **review từ Tech Lead**:

```
# Authentication & Security
config/SecurityConfig.java
config/JwtFilter.java
util/JwtUtil.java

# Core Business Logic
service/BookingService.java
service/WalletService.java
service/TicketService.java
service/PricingService.java

# Database Schema (sửa = migration)
entity/*.java → chỉ sửa khi DATABASE.md đã cập nhật trước
application.properties

# Infrastructure
.env
pom.xml → chỉ thêm dependency khi thật sự cần
```

> Cập nhật danh sách này khi có thêm module pass QC.

---

## QUY TẮC KHI CODE

### Nguyên tắc chung
1. Chỉ sửa đúng file và đúng function được yêu cầu
2. **KHÔNG** refactor, rename, hoặc "cải thiện" code khác
3. **KHÔNG** xoá comment hoặc code "không dùng"
4. Nếu thấy code "có vấn đề" → **BÁO cho user**, KHÔNG tự sửa
5. Trước khi code: liệt kê file sẽ sửa → chờ xác nhận

### Backend (Java/Spring Boot)
1. Mọi Controller phải dùng DTO — không expose Entity ra response
2. Mọi Service method ghi dữ liệu phải có `@Transactional`
3. Mọi API phải có input validation (`@Valid` + Jakarta Validation)
4. Error handling qua `@ControllerAdvice` — không try/catch rồi swallow
5. Naming convention:
   - Entity: PascalCase, số ít (`BookingSeat`)
   - Table: snake_case, số nhiều (`booking_seats`)
   - API path: kebab-case (`/api/fnb-items`)
   - DTO: `Create{Entity}Request`, `{Entity}Response`

### Frontend (Vue.js)
1. Component name: PascalCase (`MovieCard.vue`)
2. API calls qua file riêng trong `utils/` hoặc `stores/`
3. State management qua Pinia store — không prop drilling
4. Routing guard cho các trang cần auth

---

## TRƯỚC KHI BẮT ĐẦU SESSION

```
1. Đọc RULES.md (file này)
2. Đọc docs/ARCHITECTURE.md
3. Đọc docs/CRITICAL_PATHS.md nếu sửa backend
4. Đọc docs/DATABASE.md nếu liên quan database
5. Đọc docs/API_CONTRACTS.md nếu tạo/sửa API
```

---

## VERIFICATION — SAU MỖI TASK

```
1. BUILD CHECK:
   - Chạy: .\mvnw.cmd compile (backend)
   - Chạy: npm run build (frontend)
   - 0 error = PASS

2. FUNCTIONAL TEST:
   - Test happy path
   - Test error path (invalid input, unauthorized, not found)

3. SECURITY CHECK:
   - Grep tìm raw SQL: $queryRaw, createNativeQuery
   - Mọi API private có auth check
   - Không hardcoded secrets

4. Chỉ báo "xong" khi TẤT CẢ bước PASS.
```

---

## LOCK MODULE — SAU KHI QC PASS

Khi module pass QC → thêm vào PROTECTED FILES ở trên:

```markdown
### Module [Tên] (QC Pass: YYYY-MM-DD)
- path/to/file1.java
- path/to/file2.java
```

---

## MODULE DEVELOPMENT ORDER

```
Layer 1 (Foundation):  Auth + Security + Database Setup + Global Exception Handler
    ↓ PHẢI xong trước Layer 2
Layer 2 (Core Data):   Roles, Users, Customers, Staffs, Cinemas, Rooms, Seats
    ↓ có thể code song song
Layer 3 (Content):     Movies, Categories, MovieFormats, SeatTypes
    ↓
Layer 4 (Scheduling):  Showtimes, PricingRules, Shifts, StaffSchedules
    ↓
Layer 5 (Transaction): Booking Flow, Wallets, Tickets, Payments
    ↓ CRITICAL — cần review kỹ
Layer 6 (F&B):         FnbItems, BomRecipes, BookingFnbs, Inventory
    ↓
Layer 7 (Engagement):  Promotions, Vouchers, Reviews, LoyaltyPoints
    ↓
Layer 8 (Operations):  ShiftHandovers, SupportTickets, LostAndFound
    ↓
Layer 9 (CMS):         Banners, AuditLogs
    ↓
Layer 10 (Polish):     Dashboard analytics, Export, Performance optimization
```
