# Project Rules — DevCine
# Văn bản này là QUY TẮC BẮT BUỘC. AI Agent PHẢI đọc trước khi code.

---

## PHẦN 1: NGUYÊN TẮC KỸ THUẬT VÀ DỰ ÁN

### 1.1 Nguyên tắc Tuyệt đối
1. **KHÔNG BAO GIỜ** xoá hoặc viết lại code đang hoạt động mà không hỏi trước.
2. **KHÔNG BAO GIỜ** dùng raw SQL — chỉ dùng Spring Data JPA / JPQL.
3. **KHÔNG BAO GIỜ** bỏ qua validation đầu vào ở bất kỳ API nào.
4. **KHÔNG BAO GIỜ** hardcode credentials, tokens, secrets vào source code.
5. **PHẢI** đọc `docs/CRITICAL_PATHS.md` trước khi sửa bất kỳ file backend nào.
6. **PHẢI** đọc `docs/DATABASE.md` trước khi thêm/sửa bảng hoặc quan hệ.
7. **PHẢI** chạy build test trước khi báo "hoàn thành".
8. **PHẢI** dùng `@Transactional` cho mọi thao tác write liên quan đến tiền (Wallet, Booking).

### 1.2 Tech Stack — KHÔNG ĐƯỢC THAY ĐỔI
| Layer | Công nghệ | Ghi chú |
|-------|-----------|---------|
| Backend | Java 21 + Spring Boot 4.0.6 | Maven build |
| ORM | Spring Data JPA + Hibernate 7 | KHÔNG raw SQL |
| Database | PostgreSQL (Supabase) | Region: ap-southeast-1 |
| Auth | JWT (Spring Security) | Access token 7 ngày |
| Frontend | Vue.js 3 + Vite 8 + Pinia 3 | TailwindCSS 4 |
| API Format | REST JSON | Chuẩn response format trong API_CONTRACTS.md |

### 1.3 Protected Files — KHÔNG ĐƯỢC SỬA
Các file dưới đây chỉ được sửa khi có **yêu cầu rõ ràng** và **review từ Tech Lead**:
```text
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

### 1.4 Quy tắc khi Code
**Backend (Java/Spring Boot)**
1. Mọi Controller phải dùng DTO — không expose Entity ra response.
2. Mọi Service method ghi dữ liệu phải có `@Transactional`.
3. Mọi API phải có input validation (`@Valid` + Jakarta Validation).
4. Error handling qua `@ControllerAdvice` — không try/catch rồi swallow.
5. Naming convention:
   - Entity: PascalCase, số ít (`BookingSeat`)
   - Table: snake_case, số nhiều (`booking_seats`)
   - API path: kebab-case (`/api/fnb-items`)
   - DTO: `Create{Entity}Request`, `{Entity}Response`
6. API Prefix Structure:
   - API Khách hàng (Public): prefix bắt buộc là `/api/v1/public/...`
   - API Nội bộ (Admin/Staff): prefix bắt buộc là `/api/v1/admin/...`

**Frontend (Vue.js)**
1. Component name: PascalCase (`MovieCard.vue`)
2. API calls qua file riêng trong `utils/` hoặc `stores/`
3. State management qua Pinia store — không truyền prop quá nhiều tầng (prop drilling)
4. Routing guard cho các trang cần auth

### 1.5 Module Development Order (Thứ tự phát triển)
```text
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

---

## PHẦN 2: QUY TẮC HÀNH VI CỦA AI AGENT

### 2.1 Tư duy cốt lõi (Core Mindset)
- Viết code đơn giản, dễ đọc, dễ bảo trì.
- Tuân thủ cấu trúc dự án và phong cách code hiện tại.
- KHÔNG làm phức tạp hóa vấn đề (over-engineer).
- KHÔNG thêm tính năng ngoài phạm vi yêu cầu.
- KHÔNG mù quáng làm theo các yêu cầu rủi ro hoặc không rõ ràng.

### 2.2 Hiểu bối cảnh trước khi Code
Trước khi viết code, phải hiểu rõ: Dự án làm gì, cấu trúc thư mục, luồng dữ liệu, hợp đồng API, quy ước đặt tên và các pattern UI có sẵn.
- KHÔNG code nếu thiếu bối cảnh. Nếu thiếu, hãy yêu cầu người dùng cung cấp file trước.

### 2.3 Không tự suy đoán (No Assumption)
- KHÔNG ĐOÁN: Cấu trúc response API, tên trường, schema database, props của component, cấu trúc route, logic nghiệp vụ. Nếu không rõ, hãy dừng lại và hỏi.

### 2.4 Kiểm soát phạm vi (Scope Control)
- Chỉ code đúng những gì được yêu cầu. Không tự ý viết lại logic không liên quan, không đổi file khác, không thêm thư viện mới nếu chưa được cho phép.

### 2.5 Code Sạch và Tái cấu trúc (Clean Code & Refactor)
- Tái cấu trúc chỉ khi: Có ít nhất 2 đoạn logic bị lặp, việc trừu tượng hóa giúp dễ đọc hơn, và không làm ẩn đi logic nghiệp vụ.
- Tên biến phải rõ ràng. Tốt: `selectedMovieId`. Xấu: `catId`, `prdSz`.

### 2.6 Định dạng Code xuất ra
- Luôn hiển thị đường dẫn file trước khối code.
- Luôn trả về toàn bộ code của hàm hoặc file đã thay đổi. KHÔNG viết tắt kiểu "giữ nguyên code cũ" hay "...". Code phải có thể copy-paste và chạy được luôn.

### 2.7 Ghi chú (Comments)
- Ghi chú phải cụ thể và hữu ích. Bỏ qua các ghi chú chung chung kiểu "Optimize performance".
- Chỉ ghi chú khi giải thích: Quy tắc nghiệp vụ, Hợp đồng bên ngoài, Các trường hợp ngoại lệ (Edge case) phức tạp.

### 2.8 Các trường hợp ngoại lệ và Lỗi (Edge Cases & Errors)
- Luôn xử lý lỗi: Lỗi API, Dữ liệu đầu vào không hợp lệ, Mảng rỗng, Null/Undefined.
- Hãy tự hỏi: Dữ liệu có thể trống không? Người dùng có bấm nút 2 lần không?

### 2.9 Nhất quán Giao diện (UI Consistency)
- Tuyệt đối KHÔNG thay đổi UI trừ khi được yêu cầu. Không tự ý đổi layout, khoảng cách, màu sắc, font chữ.
- Nếu phải refactor UI component, giao diện hiển thị ra phải giống hệt như cũ.
- Tham khảo `DESIGN.md` (nếu có) cho các màu sắc, font chữ, nút bấm.

### 2.10 Tư duy Độc lập (Independent Thinking)
- KHÔNG nhắm mắt làm theo yêu cầu nếu nó sai về mặt kỹ thuật.
- Hãy phản biện nếu yêu cầu gây ra bug, phá hỏng UI, khó bảo trì, tạo rủi ro bảo mật. Nêu rõ vấn đề và đề xuất giải pháp tốt hơn.

### 2.11 Quy tắc Git & Commit
- **KHÔNG** tự động sinh ra commit hoặc PR.
- Chỉ nhắc nhở commit khi một đơn vị logic đã hoàn thành và đạt Tiêu chuẩn Hoàn thành (Definition of Done).
- Mẫu nhắc nhở: *"Là Senior, tôi thấy làm như này ổn để commit rồi, bạn có muốn commit không?"*
- Chỉ thực thi thao tác Commit/PR khi user chat chữ `DONE` hoặc `COMMIT` hoặc `ACCEPT`.
- Khi user gọi lệnh commit, cung cấp 3 lựa chọn Tiêu đề Commit (Best, Short, Detailed) để user chọn trước khi push.

### 2.12 Tiêu chuẩn Hoàn thành (Definition of Done)
Một task chỉ được coi là XONG khi:
- Code đáp ứng đúng logic yêu cầu.
- Không phát sinh lỗi cú pháp/runtime mới.
- Xử lý được các edge case cơ bản.
- Hành vi cũ và UI cũ không bị phá hỏng.
- Tuân thủ phong cách code của dự án.
- **Verification:** Đã chạy thử lệnh build (`mvnw compile` hoặc `npm run build`) và PASS.

---
*Nếu có bất kỳ điều gì không an toàn hoặc không chắc chắn, HÃY DỪNG LẠI VÀ HỎI TRƯỚC.*
