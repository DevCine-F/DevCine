# Quy Tắc Bảo Mật — DevCine

> **Version:** 1.0  
> **Cập nhật:** 2026-05-26  
> **Áp dụng:** Bắt buộc cho mọi thành viên dev team

---

## 1. Input Validation

- **MỌI** API endpoint phải có Jakarta Validation (`@Valid`, `@NotNull`, `@Size`, `@Email`)
- Validate **TRƯỚC KHI** xử lý bất kỳ logic nào
- Sanitize HTML input: loại bỏ `<script>`, SQL keywords
- Giới hạn độ dài input:
  - `VARCHAR(255)` cho text ngắn (tên, email)
  - `VARCHAR(500)` cho text dài (mô tả, URL)
  - `TEXT` cho nội dung dài (comment, review) — max 10,000 ký tự
- Validate số nguyên: `@Min(0)`, `@Max(...)` cho các trường quantity, rating
- Validate decimal: precision(15,2) cho tiền — không dùng `float`/`double`

### Ví dụ validation
```java
public class CreateMovieRequest {
    @NotBlank(message = "Tên phim không được trống")
    @Size(max = 255, message = "Tên phim tối đa 255 ký tự")
    private String title;

    @Min(value = 1, message = "Thời lượng phải > 0")
    @Max(value = 600, message = "Thời lượng tối đa 600 phút")
    private Integer durationMins;

    @Pattern(regexp = "^(P|C13|C16|C18)$", message = "Phân loại tuổi không hợp lệ")
    private String ageRating;
}
```

---

## 2. Authentication & Authorization

### JWT Configuration
- **Algorithm:** HMAC-SHA256
- **Access Token Expiry:** 7 ngày (604800000ms) — *cân nhắc giảm xuống 15 phút cho production*
- **Secret Key:** Từ biến môi trường `JWT_SECRET` — min 64 ký tự
- Token chứa: `userId`, `role`, `issuedAt`, `expiration`

### Quy tắc
- Mọi API private **PHẢI** qua `JwtFilter` middleware
- Phân quyền theo role: `ADMIN > MANAGER > STAFF > CUSTOMER`
- **Password:** BCrypt hash, minimum 8 ký tự, phải có chữ hoa + số + ký tự đặc biệt
- Rate limit login: 5 lần sai / 15 phút → lock tạm thời
- Không lưu JWT vào database — stateless authentication

### Endpoint Security Matrix
```
PUBLIC (no auth):     GET /api/movies, /api/cinemas, /api/banners, /auth/**
CUSTOMER:             POST /api/bookings, GET /api/wallets/my, POST /api/reviews
STAFF:                POST /api/tickets/print, POST /api/ticketing/pay
MANAGER:              POST /api/showtimes, PUT /api/staffs, /api/handovers/approve
ADMIN:                POST /api/users, DELETE /api/**, GET /api/audit-logs
```

---

## 3. Database Security

### Quy tắc tuyệt đối
- **CHỈ** dùng Spring Data JPA — **TUYỆT ĐỐI** không raw SQL
- Nếu cần query phức tạp → dùng `@Query` với JPQL, **KHÔNG** nativeQuery
- Parameterized queries cho mọi trường hợp (JPA tự xử lý)
- **Không lưu** sensitive data dạng plaintext:
  - Password → BCrypt hash
  - JWT Secret → Environment variable
  - DB credentials → `.env` file (KHÔNG commit git)

### Backup
- Supabase tự động backup hàng ngày
- Trước migration → manual backup bằng `pg_dump`

### Quy tắc truy cập
- Mỗi môi trường (dev/staging/prod) có DB credentials riêng
- Không share credentials qua chat/email — chỉ qua `.env`
- Database password min 16 ký tự, có special characters

---

## 4. API Security

### Rate Limiting
```
Chung:     100 requests / phút / IP
Login:     5 requests / phút / IP (tránh brute-force)
Booking:   10 requests / phút / user (tránh spam booking)
```

### CORS Configuration
```java
// Chỉ cho phép domain frontend
allowedOrigins: ["http://localhost:5173", "https://devcine.vn"]
allowedMethods: ["GET", "POST", "PUT", "DELETE"]
allowedHeaders: ["Authorization", "Content-Type"]
allowCredentials: true
```

### Security Headers
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains (HTTPS only)
Content-Security-Policy: default-src 'self'
```

### Error Response
- **KHÔNG** expose stack trace trong error response
- **KHÔNG** expose internal server details (DB name, table name, IP)
- Log chi tiết ở server-side (AuditLog), trả message generic cho client
- HTTP status codes chuẩn:
  ```
  400 Bad Request       → Validation error
  401 Unauthorized      → Missing/invalid token
  403 Forbidden         → Insufficient permissions
  404 Not Found         → Resource không tồn tại
  409 Conflict          → Duplicate resource
  429 Too Many Requests → Rate limited
  500 Internal Error    → Server error (log + alert)
  ```

---

## 5. Sensitive Data Handling

### Dữ liệu nhạy cảm
| Loại | Cách xử lý |
|------|------------|
| Password | BCrypt hash (cost factor 12) |
| JWT Secret | Environment variable (`JWT_SECRET`) |
| DB credentials | `.env` file, không commit git |
| CCCD/CMND (`id_card`) | Chỉ hiển thị 4 số cuối cho STAFF |
| Email | Full access cho ADMIN, mask cho STAFF |
| Phone | Full access cho ADMIN, mask cho STAFF |
| Wallet balance | Chỉ owner và ADMIN xem được |

### .gitignore BẮT BUỘC
```
# Secrets — KHÔNG BAO GIỜ commit
.env
*.env.local
application-prod.properties

# IDE
.idea/
.vscode/
*.iml

# Build
target/
node_modules/
dist/
```

---

## 6. Infrastructure Security

### Khi deploy production
- **SSL/HTTPS** bắt buộc — redirect HTTP → HTTPS
- **Firewall:** chỉ mở port 80, 443, 22
- **SSH:** key-only authentication, disable password auth
- **Database:** không expose port 5432 ra internet
- **Logs:** không ghi sensitive data (password, token) vào log file

### Monitoring
- Setup health check endpoint: `GET /api/health`
- Alert khi:
  - Error rate > 5% trong 5 phút
  - Response time > 3 giây
  - Database connection pool exhausted
  - Disk space < 10%

---

## 7. Checklist Bảo Mật Trước Deploy

```
[ ] Tất cả API private có auth middleware
[ ] Tất cả input có validation
[ ] Không có raw SQL
[ ] Không có hardcoded secrets trong source code
[ ] .env đã trong .gitignore
[ ] CORS chỉ cho phép domain production
[ ] Error response không expose stack trace
[ ] Password hash bằng BCrypt
[ ] JWT secret đủ mạnh (min 64 ký tự)
[ ] Rate limiting đã cấu hình
[ ] HTTPS đã bật
[ ] Database backup đã lên lịch
[ ] Audit log hoạt động
```
