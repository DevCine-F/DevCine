# Critical Paths — DevCine

> **Version:** 1.0  
> **Cập nhật:** 2026-05-26  
> **Mục đích:** Tài liệu các luồng nghiệp vụ KHÔNG ĐƯỢC PHÁ VỠ. AI Agent PHẢI đọc trước khi sửa backend.

---

## ⚠️ CẢNH BÁO

Các luồng nghiệp vụ trong file này là **CRITICAL** — nghĩa là nếu sai sẽ gây ra:
- Mất tiền (thanh toán, ví)
- Mất dữ liệu booking
- Lỗi bảo mật (auth bypass)
- Trải nghiệm khách hàng bị ảnh hưởng nghiêm trọng

**PHẢI có review từ Tech Lead** trước khi sửa bất kỳ file nào trong danh sách PROTECTED.

---

## 🔒 PROTECTED FILES

Các file sau **KHÔNG ĐƯỢC SỬA** nếu không có yêu cầu rõ ràng và review:

```
# Authentication & Security
config/SecurityConfig.java          ← CORS, JWT filter chain, endpoint security
config/JwtFilter.java               ← JWT token validation middleware
util/JwtUtil.java                    ← Token generation & parsing

# Database Schema
entity/*.java                        ← Chỉ sửa khi có DATABASE.md cập nhật
application.properties               ← Database connection, server config

# Core Business Logic
service/BookingService.java          ← Luồng đặt vé + thanh toán
service/WalletService.java           ← Luồng ví điện tử
service/TicketService.java           ← Luồng check-in
service/PricingService.java          ← Tính giá vé
```

---

## 1. 🔐 Luồng Authentication (CRITICAL)

### Mô tả
Luồng đăng nhập/đăng ký và xác thực mọi request. Nếu sai → toàn bộ hệ thống mất bảo mật.

### Files liên quan
```
config/SecurityConfig.java       ← 🔒 PROTECTED
config/JwtFilter.java            ← 🔒 PROTECTED
util/JwtUtil.java                ← 🔒 PROTECTED
controller/AuthController.java
service/AuthService.java
service/UserService.java
repository/UserRepository.java
entity/User.java                 ← 🔒 PROTECTED
entity/Role.java                 ← 🔒 PROTECTED
```

### Flow từng bước

#### 1.1 Đăng ký
```
1. Client → POST /api/auth/register { username, email, password, phone }
2. AuthController.register()
   → Validate input (username unique, email format, password strength)
   → AuthService.register()
      a. Check username/email đã tồn tại → throw DuplicateException
      b. Hash password bằng BCryptPasswordEncoder
      c. Tạo User với role = CUSTOMER
      d. Tạo Customer record (1:1 với User)
      e. Tạo Wallet cho Customer (balance = 0)
3. ← Response 201: { user info }
```

#### 1.2 Đăng nhập
```
1. Client → POST /api/auth/login { username, password }
2. AuthController.login()
   → AuthService.login()
      a. Find user by username → throw NotFoundException nếu không tồn tại
      b. Check is_active = true → throw AccountDisabledException
      c. BCrypt.matches(password, hash) → throw BadCredentialsException
      d. JwtUtil.generateToken(userId, role, expiry)
      e. Ghi AuditLog(user, LOGIN, ip_address)
3. ← Response 200: { accessToken, user }
```

#### 1.3 JWT Filter (mọi request)
```
1. Request vào → JwtFilter.doFilterInternal()
2. Lấy header Authorization: Bearer <token>
3. JwtUtil.validateToken(token)
   → Verify signature (HMAC-SHA256 với JWT_SECRET)
   → Check expiry → throw ExpiredTokenException
   → Extract userId + role
4. Set SecurityContext authentication
5. → Tiếp tục filter chain
```

---

## 2. 🎫 Luồng Đặt Vé (CRITICAL)

### Mô tả
Luồng core business: chọn phim → chọn suất → chọn ghế → chọn F&B → thanh toán → nhận vé. Sai → mất tiền, double-booking.

### Files liên quan
```
controller/BookingController.java
service/BookingService.java          ← 🔒 PROTECTED
service/PricingService.java          ← 🔒 PROTECTED
service/WalletService.java           ← 🔒 PROTECTED
service/TicketService.java           ← 🔒 PROTECTED
repository/BookingSeatRepository.java
repository/BookingFnbRepository.java
repository/TicketRepository.java
repository/SeatRepository.java
entity/BookingSeat.java
entity/BookingFnb.java
entity/Ticket.java
entity/Wallet.java
entity/WalletTransaction.java
```

### Flow từng bước
```
1. Customer → POST /api/bookings
   {
     showtimeId: 1,
     seatIds: [1, 2],
     fnbItems: [{ fnbItemId: 1, quantity: 2 }],
     voucherId: null,
     paymentMethod: "WALLET"
   }

2. BookingController.createBooking()
   → Validate: customer authenticated
   → BookingService.createBooking() (trong @Transactional)

3. Step 3a: LOCK GHẾ (Pessimistic Lock)
   → SELECT seats WHERE id IN (1,2) FOR UPDATE
   → Check: tất cả ghế AVAILABLE cho showtime này
   → Nếu đã booked → throw SeatUnavailableException (rollback)

4. Step 3b: TÍNH GIÁ
   → PricingService.calculatePrice(showtime, seats)
      a. Base price từ PricingRule (theo ngày, giờ)
      b. × seat_type.price_modifier (VIP = 1.5x)
      c. + movie_format.surcharge (3D = +30k)
      d. = Giá mỗi ghế

5. Step 3c: ÁP KHUYẾN MÃI
   → Nếu có voucherId:
      a. Validate voucher: is_used = false, valid_until > now
      b. Tính discount: PERCENTAGE hoặc FIXED_AMOUNT
      c. Check is_stackable nếu áp nhiều KM

6. Step 3d: THANH TOÁN
   → WalletService.debit(customerId, totalAmount)
      a. Lock wallet (SELECT FOR UPDATE)
      b. Check balance >= totalAmount → throw InsufficientBalanceException
      c. wallet.balance -= totalAmount
      d. Tạo WalletTransaction(type=PAYMENT, amount)

7. Step 3e: TẠO BOOKING RECORDS
   → Tạo BookingSeat cho mỗi ghế (status=CONFIRMED, price_snapshot)
   → Tạo BookingFnb cho mỗi F&B item (price_snapshot)

8. Step 3f: GENERATE TICKETS
   → Mỗi BookingSeat → tạo 1 Ticket
   → Generate QR code: "DEVCINE-T{id}-{date}"
   → Set is_checked_in = false

9. Step 3g: CẬP NHẬT
   → Mark voucher is_used = true (nếu có)
   → Cộng loyalty_points cho customer
   → Ghi AuditLog

10. ← Response 201: { booking, tickets, qrCodes }

⚠️ NẾU BẤT KỲ BƯỚC NÀO FAIL → @Transactional ROLLBACK TOÀN BỘ
```

---

## 3. ✅ Luồng Check-in (CRITICAL)

### Mô tả
Staff scan QR → xác thực vé → cho vào rạp. Sai → trùng check-in, vào sai phòng.

### Files liên quan
```
controller/TicketController.java
service/TicketService.java           ← 🔒 PROTECTED
repository/TicketRepository.java
entity/Ticket.java
entity/BookingSeat.java
```

### Flow từng bước
```
1. Staff → POST /api/tickets/check-in { qrCode: "DEVCINE-T001-20260527" }

2. TicketController.checkIn()
   → Validate: staff authenticated (role STAFF/MANAGER/ADMIN)
   → TicketService.checkIn() (trong @Transactional)

3. Step 3a: TÌM VÉ
   → ticketRepository.findByQrCode(qrCode)
   → Nếu không tìm thấy → throw TicketNotFoundException

4. Step 3b: VALIDATE
   → Check is_checked_in = false → throw AlreadyCheckedInException
   → Check showtime.start_time within today → throw WrongDateException
   → Check showtime.status = SCHEDULED/ONGOING

5. Step 3c: CHECK TUỔI (nếu phim 18+)
   → Nếu movie.age_rating = "C18" && !ticket.is_age_verified
   → Trả warning: cần xác minh tuổi trước

6. Step 3d: UPDATE
   → ticket.is_checked_in = true
   → ticket.checked_in_by = currentStaff
   → ticket.check_in_time = now()
   → Ghi AuditLog

7. ← Response 200: { ticket info, seat, movie, showtime }
```

---

## 4. 💰 Luồng Ví Điện Tử (CRITICAL)

### Mô tả
Nạp/rút/thanh toán qua ví. Sai → mất tiền, âm balance.

### Files liên quan
```
controller/WalletController.java
service/WalletService.java           ← 🔒 PROTECTED
repository/WalletRepository.java
repository/WalletTransactionRepository.java
entity/Wallet.java
entity/WalletTransaction.java
```

### Flow từng bước (Nạp tiền)
```
1. Customer → POST /api/wallets/deposit { amount: 500000 }
2. WalletService.deposit() (trong @Transactional)
   a. Validate amount > 0
   b. Lock wallet (SELECT FOR UPDATE)
   c. wallet.balance += amount
   d. Tạo WalletTransaction(type=DEPOSIT, amount)
   e. Ghi AuditLog
3. ← Response 200: { newBalance }
```

### Quy tắc bất di bất dịch
- **KHÔNG BAO GIỜ** cho balance < 0
- **LUÔN** dùng Pessimistic Lock khi thay đổi balance
- **LUÔN** tạo WalletTransaction cho mọi thay đổi balance
- **LUÔN** dùng @Transactional

---

## 5. 👷 Luồng Bàn Giao Ca (IMPORTANT)

### Mô tả
Staff kết thúc ca → khai báo tiền mặt → Manager duyệt. Sai → mất kiểm soát tài chính.

### Files liên quan
```
controller/ShiftHandoverController.java
service/ShiftHandoverService.java
repository/ShiftHandoverRepository.java
entity/ShiftHandover.java
entity/StaffSchedule.java
```

### Flow từng bước
```
1. Staff → POST /api/handovers
   { staffScheduleId: 1, declaredCash: 5000000 }

2. ShiftHandoverService.create()
   a. Validate staff owns this schedule
   b. Query system_cash từ tổng booking_seats.price_snapshot trong ca
   c. difference = declared_cash - system_cash
   d. Tạo ShiftHandover(status=PENDING)

3. Manager → PUT /api/handovers/{id}/approve
   a. Validate manager role
   b. Review difference
   c. Set status = APPROVED / REJECTED
   d. Set approved_by_manager
```

---

## 6. 📦 Luồng Quản Lý Kho (IMPORTANT)

### Mô tả
Nhập/xuất kho F&B. Sai → tồn kho âm, mất kiểm soát.

### Files liên quan
```
controller/InventoryController.java
service/InventoryService.java
repository/CinemaInventoryRepository.java
repository/InventoryLogRepository.java
entity/CinemaInventory.java
entity/InventoryLog.java
```

### Flow từng bước (Cập nhật tồn kho)
```
1. Staff → PUT /api/inventory/{id}/adjust
   { quantityChange: +50, type: "IMPORT" }

2. InventoryService.adjust() (trong @Transactional)
   a. Lock cinema_inventory (SELECT FOR UPDATE)
   b. Validate: in_stock + quantityChange >= 0
   c. cinema_inventory.in_stock += quantityChange
   d. Tạo InventoryLog(type, quantityChange, changedByStaff)
   e. Update last_updated = now()

3. ← Response 200: { newStock }
```

---

## 7. Dependency Map

```
                    ┌──────────────┐
                    │  AUTH FLOW   │ ← PHẢI HOÀN THÀNH ĐẦU TIÊN
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        ┌─────▼────┐ ┌─────▼────┐ ┌────▼──────┐
        │  MOVIE   │ │  CINEMA  │ │  USER     │
        │  MGMT    │ │  MGMT    │ │  MGMT     │
        └─────┬────┘ └─────┬────┘ └────┬──────┘
              │            │            │
              └────────────┼────────────┘
                           │
                    ┌──────▼───────┐
                    │  SHOWTIME    │
                    │  MGMT       │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        ┌─────▼────┐ ┌─────▼────┐ ┌────▼──────┐
        │ BOOKING  │ │   F&B    │ │ PROMOTION │
        │ FLOW     │ │  MGMT    │ │   MGMT    │
        └─────┬────┘ └─────┬────┘ └────┬──────┘
              │            │            │
              └────────────┼────────────┘
                           │
                    ┌──────▼───────┐
                    │  CHECK-IN    │
                    │  FLOW        │
                    └──────────────┘
```
