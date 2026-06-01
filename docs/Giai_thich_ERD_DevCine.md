# Giải thích chi tiết Cơ sở dữ liệu DevCine (ERD Ver 7)

Dựa trên cấu trúc ERD, các bảng trong hệ thống được thiết kế rất chặt chẽ và chia thành 7 nhóm logic lớn. Dưới đây là phân tích chi tiết từ "cấu trúc khung" (lớn nhất) đến các "chi tiết nghiệp vụ" (nhỏ nhất) và các liên kết tương ứng.

---

## 1. Nhóm Quản trị Hệ thống & Người dùng (Core Users & Security)
*Đây là móng của hệ thống, quản lý ai có thể vào hệ thống và có quyền gì.*

* **`ROLES` (Vai trò/Phân quyền)**
  - **Tác dụng:** Định nghĩa các cấp bậc trong hệ thống (VD: Admin, Quản lý rạp, Nhân viên bán vé, Khách hàng) kèm theo ma trận quyền (`permissions_matrix`).
  - **Liên kết:** Một Role có nhiều Users.
* **`USERS` (Tài khoản người dùng)**
  - **Tác dụng:** Lưu thông tin đăng nhập gốc (username, password, email) cho MỌI người trong hệ thống.
  - **Liên kết:** Thuộc về 1 `ROLES`. Là bảng cha của `CUSTOMERS` và `STAFFS`.
* **`CUSTOMERS` (Khách hàng)**
  - **Tác dụng:** Mở rộng từ `USERS`, lưu thông tin chuyên biệt của khách (điểm thành viên, mã thẻ).
  - **Liên kết:** Rất nhiều liên kết! Tham gia vào Đơn hàng (`BOOKINGS`), Đánh giá (`REVIEWS`), Hỗ trợ (`SUPPORT_TICKETS`), Ví (`WALLETS`).
* **`STAFFS` (Nhân viên)**
  - **Tác dụng:** Mở rộng từ `USERS` dành cho nhân viên rạp, lưu mã nhân viên, thuộc rạp nào và ai là quản lý trực tiếp.
  - **Liên kết:** Làm việc tại `CINEMAS`, có lịch làm việc (`STAFF_SCHEDULES`), xử lý hỗ trợ (`SUPPORT_TICKETS`), và duyệt bàn giao (`SHIFT_HANDOVERS`).
* **`WALLETS` & `WALLET_TRANSACTIONS` (Ví điện tử)**
  - **Tác dụng:** Quản lý số dư tiền ảo/điểm của khách hàng (`WALLETS`) và ghi lại chi tiết từng lần nạp/rút/tiêu xài (`WALLET_TRANSACTIONS`).
* **`AUDIT_LOGS` (Nhật ký hệ thống)**
  - **Tác dụng:** Bảng lưu lại dấu vết (ai làm gì, thay đổi dữ liệu nào, IP nào) để truy vết lỗi hoặc gian lận. 

---

## 2. Nhóm Cơ sở vật chất Rạp (Cinema Infrastructure)
*Nơi quản lý "phần cứng" của hệ thống kinh doanh.*

* **`CINEMAS` (Rạp chiếu phim)**
  - **Tác dụng:** Khai báo các chi nhánh rạp (Tên rạp, địa chỉ...).
  - **Liên kết:** Quản lý nhiều phòng chiếu (`ROOMS`), là nơi nhân viên làm việc (`STAFFS`), và quản lý kho tại rạp (`CINEMA_INVENTORY`).
* **`ROOMS` (Phòng chiếu)**
  - **Tác dụng:** Từng phòng cụ thể trong một rạp (Rạp A có Phòng 1, Phòng 2).
  - **Liên kết:** Chứa nhiều ghế (`SEATS`), và là nơi gán Lịch chiếu (`SHOWTIMES`).
* **`SEAT_TYPES` (Loại ghế)**
  - **Tác dụng:** Định nghĩa các loại ghế (Thường, VIP, Đôi) và hệ số giá.
* **`SEATS` (Ghế vật lý)**
  - **Tác dụng:** Vị trí ghế cố định trong một phòng (Hàng A, Cột 1).
  - **Liên kết:** Phụ thuộc vào `ROOMS` và `SEAT_TYPES`. Khi khách đặt, sẽ map vào `BOOKING_SEATS`.

---

## 3. Nhóm Phim & Lịch chiếu (Movies & Showtimes)
*Quản lý "sản phẩm vô hình" của rạp.*

* **`CATEGORIES` (Thể loại phim)**
  - **Tác dụng:** Danh mục tĩnh (Hành động, Tình cảm...).
* **`MOVIES` (Phim)**
  - **Tác dụng:** Lưu thông tin cốt lõi của phim (Tên, độ dài, ngày ra mắt, phân loại tuổi).
  - **Liên kết:** Thông qua `MOVIE_CATEGORIES` (Bảng trung gian) để kết nối nhiều-nhiều với `CATEGORIES` (Một phim có nhiều thể loại).
* **`MOVIE_FORMATS` (Định dạng phim)**
  - **Tác dụng:** Định nghĩa công nghệ chiếu (2D, 3D, IMAX) và phí phụ thu.
* **`SHOWTIMES` (Lịch chiếu / Suất chiếu)**
  - **Tác dụng:** Bảng trung tâm ghép mọi thứ lại: Phim A + chiếu ở Phòng B + Định dạng 3D + Khung giờ cụ thể.
  - **Liên kết:** Bắt nguồn từ `MOVIES`, `ROOMS`, `MOVIE_FORMATS`. Và đây là đích ngắm của Đơn đặt vé (`BOOKINGS`).

---

## 4. Nhóm Kinh doanh & Giao dịch (Bookings Flow)
*Dòng chảy ra tiền của hệ thống (Nhóm quan trọng nhất).*

* **`BOOKINGS` (Đơn đặt hàng)**
  - **Tác dụng:** Hóa đơn tổng cho một lần mua của khách. Lưu tổng tiền, trạng thái thanh toán, phương thức thanh toán.
  - **Liên kết:** Cần Khách hàng (`CUSTOMERS`), Suất chiếu (`SHOWTIMES`), và Mã giảm giá nếu có (`VOUCHERS`).
* **`BOOKING_SEATS` (Ghế đã đặt)**
  - **Tác dụng:** Phá vỡ quan hệ n-n giữa Đơn hàng và Ghế. Lưu lại trong Đơn hàng X khách đã chọn những Ghế nào, với giá tại thời điểm mua là bao nhiêu.
* **`TICKETS` (Vé)**
  - **Tác dụng:** Sau khi đặt ghế thành công, sinh ra Vé vật lý/điện tử (chứa QR code, giờ check-in) cho từng ghế.
  - **Liên kết:** Trực tiếp nối vào `BOOKING_SEATS`.
* **`BOOKING_FNBS` (Đồ ăn thức uống đã đặt)**
  - **Tác dụng:** Tương tự `BOOKING_SEATS`, nhưng dành cho Bắp, Nước. Lưu số lượng và giá bắp nước của Đơn hàng X.

---

## 5. Nhóm Sản phẩm F&B & Tồn kho (F&B and Inventory)
*Quản lý hàng hóa vật lý bán kèm.*

* **`FNB_ITEMS` (Sản phẩm F&B)**
  - **Tác dụng:** Khai báo danh mục bắp, nước, combo.
* **`BOM_RECIPES` (Định mức nguyên liệu)**
  - **Tác dụng:** Dành cho việc tính kho. Ví dụ: Combo 1 (combo_id) được tạo ra từ 1 Bắp (ingredient_id) + 2 Nước (ingredient_id).
* **`CINEMA_INVENTORY` (Tồn kho của rạp)**
  - **Tác dụng:** Theo dõi số lượng tồn kho của mỗi loại F&B tại từng rạp chiếu cụ thể.
* **`INVENTORY_LOGS` (Nhật ký kho)**
  - **Tác dụng:** Ghi nhận mọi lần nhập, xuất, hao hụt kho (Ai làm, thay đổi bao nhiêu).

---

## 6. Nhóm Khuyến mãi & Marketing (Promotions & Pricing)
*Kích cầu mua sắm và hiển thị.*

* **`PRICING_RULES` (Bảng giá động)**
  - **Tác dụng:** Định nghĩa luật tính giá (Ví dụ: Thứ 3 vui vẻ giảm 20k, sau 22h giảm giá).
* **`PROMOTIONS` (Chương trình khuyến mãi)**
  - **Tác dụng:** Tổ chức các đợt chiến dịch (Tên chương trình, Ngày bắt đầu, kết thúc).
* **`VOUCHERS` (Mã giảm giá)**
  - **Tác dụng:** Từng mã phát hành cụ thể thuộc một `PROMOTIONS`.
  - **Liên kết:** Khách hàng (`CUSTOMERS`) có thể sở hữu, và áp dụng vào (`BOOKINGS`).
* **`BANNERS` (Biểu ngữ quảng cáo)**
  - **Tác dụng:** Đơn thuần lưu ảnh và vị trí hiển thị banner trên App/Web.

---

## 7. Nhóm Vận hành nội bộ (Staff Operations & CS)
*Quản lý con người và chất lượng dịch vụ.*

* **`SHIFTS` (Ca làm việc gốc)**
  - **Tác dụng:** Định nghĩa khung giờ (Ca sáng: 8h-16h).
* **`STAFF_SCHEDULES` (Lịch làm việc)**
  - **Tác dụng:** Bố trí Nhân viên A (`STAFFS`) làm Ca B (`SHIFTS`) vào ngày C.
* **`SHIFT_HANDOVERS` (Bàn giao ca)**
  - **Tác dụng:** Cuối ca, nhân viên chốt sổ (Tiền mặt thực tế, tiền mặt hệ thống) và chờ quản lý duyệt.
* **`SUPPORT_TICKETS` (Yêu cầu hỗ trợ)**
  - **Tác dụng:** Khách khiếu nại hoặc cần giúp đỡ. Sẽ được gán cho 1 nhân viên (`STAFFS`) xử lý.
* **`REVIEWS` (Đánh giá)**
  - **Tác dụng:** Khách hàng review Phim sau khi xem.

---

## 8. 🚀 Tối ưu hóa Database cho Bảng điều khiển (Dashboard) - Dành riêng cho Đồ án Tốt nghiệp
Dựa trên yêu cầu của tài liệu nghiệp vụ (hiển thị KPI Doanh thu tháng, Bảng xếp hạng phim, Tỉ lệ lấp đầy real-time trên Admin Dashboard), cấu trúc Database cần có chiến lược phân mảnh dữ liệu (Data Mart) để tránh nghẽn cổ chai:

**Vấn đề:** 
Hệ thống hiện tại đang lưu `occupiedSeats` và `totalSeats` trong bảng `SHOWTIMES` (Rất phù hợp cho luồng bán vé nhanh). Tuy nhiên, việc tính toán doanh thu và xếp hạng phim vẫn đòi hỏi phải thực hiện phép toán (SUM/COUNT) trực tiếp từ bảng `BOOKINGS`. Với dữ liệu khổng lồ của một chuỗi rạp phim, việc này sẽ làm sập Database.

**Giải pháp nâng cấp kiến trúc (OLTP vs OLAP):**
Nên thiết kế thêm các bảng thống kê riêng biệt, chạy độc lập với luồng bán vé gốc:

1. **`SHOWTIME_STATISTICS` (Thống kê theo từng suất chiếu)**
   - **Tác dụng:** Trực tiếp phục vụ tính tỉ lệ lấp đầy, công suất phòng chiếu cực nhanh.
   - **Cấu trúc:** `showtime_id` (PK), `total_seats`, `booked_seats`, `ticket_revenue` (Doanh thu vé riêng suất này), `fnb_revenue` (Doanh thu bắp nước suất này).

2. **`DAILY_MOVIE_STATS` (Thống kê Phim theo Ngày)**
   - **Tác dụng:** Dữ liệu nguồn trực tiếp để vẽ biểu đồ doanh thu 7 ngày, Top xếp hạng phim và Doanh thu tháng mà không cần query tới Hóa đơn.
   - **Cấu trúc:** `id` (PK), `movie_id` (FK), `report_date`, `daily_tickets_sold` (Tổng vé), `daily_revenue` (Tổng doanh thu).

**Cơ chế vận hành (Event-Driven):**
Áp dụng Spring Boot Events. Mỗi khi một đơn hàng (`BOOKINGS`) được thanh toán thành công ở quầy POS (hoặc Online), hệ thống bắn ra một event bất đồng bộ (Async) để tự động cập nhật cộng dồn số liệu vào 2 bảng thống kê trên. API gọi báo cáo ở Frontend chỉ việc `SELECT` và hiển thị kết quả trong vài mili-giây.

---

**Tóm tắt luồng đi chính của một khách hàng (The Core Flow):**
Khách hàng (`CUSTOMERS`) -> Chọn Phim (`MOVIES`) -> Chọn Suất chiếu (`SHOWTIMES`) tại Rạp (`CINEMAS`) -> Tạo Đơn hàng (`BOOKINGS`) -> Chọn Ghế (`BOOKING_SEATS`) + Mua bắp nước (`BOOKING_FNBS`) -> Thanh toán (qua `WALLETS` hoặc cổng ngoài) -> Nhận Vé (`TICKETS`) -> Check-in tại rạp bằng QR Code.
