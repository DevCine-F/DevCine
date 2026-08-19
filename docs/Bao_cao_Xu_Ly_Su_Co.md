# BÁO CÁO ĐẶC TẢ NGHIỆP VỤ & KỊCH BẢN KIỂM THỬ
## PHÂN HỆ: XỬ LÝ SỰ CỐ PHÒNG CHIẾU & ĐỔI GHẾ ĐỀN BÙ (DEVCINE)

- **Mã tài liệu:** `DOC-DEVCINE-INCIDENT-2026-V1`
- **Đường dẫn giao diện:** `/admin/incidents` (URL: `localhost:5173/admin/incidents`)
- **Phân hệ quyền:** `incident_handling` (Actions: `view`, `handle`)
- **Ngày lập:** 19/08/2026
- **Trạng thái:** Đã triển khai & Kiểm tra thực tế

---

## PHẦN 1: BẢNG ĐẶC TẢ USE CASE CHUẨN (PHỤ LỤC A)

### Bảng A.37: Use case tra cứu thông tin vé và người ngồi ghế khi xảy ra sự cố phòng chiếu

| Mã Use case | UC-37 | Tên Use Case | Tra cứu thông tin vé và người ngồi ghế khi xảy ra sự cố phòng chiếu |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé / Quản lý rạp |
| Mô tả | Hỗ trợ nhân viên tra cứu tức thì thông tin đơn vé và khách hàng đang giữ ghế khi xảy ra sự cố phòng chiếu (tự động nhận diện Mã đặt vé hoặc Số điện thoại khách hàng, hoặc truy vấn ngược từ vị trí ghế trên sơ đồ). <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên truy cập phân hệ "Xử lý sự cố phòng chiếu" trên giao diện quản trị (đường dẫn /admin/incidents). <br> Bước 2: Nhân viên nhập Mã đặt vé (ví dụ: BK202608149872) hoặc Số điện thoại khách hàng (9-11 chữ số) vào ô tìm kiếm và nhấn phím Enter (hoặc nhấn nút kính lúp). <br> Bước 3: Hệ thống tự động nhận diện định dạng: Nếu là số điện thoại, truy vấn đơn vé đã xác nhận gần nhất; nếu là mã vé, truy vấn trực tiếp đơn hàng tương ứng. <br> Bước 4: Hệ thống kiểm tra quyền hạn cụm rạp (Strict Cinema Scoping). Nếu đơn vé thuộc cụm rạp khác, hệ thống từ chối truy cập. <br> Bước 5: Hệ thống tải toàn bộ thông tin chi tiết đơn vé lên cột bên trái (Mã vé, Kênh bán, Tên phim, Phòng chiếu, Suất chiếu, Thông tin khách hàng, Danh sách ghế) đồng thời nạp ma trận sơ đồ ghế của suất chiếu lên cột bên phải. <td colspan=3/> |
| Lưu ý | - Chỉ hỗ trợ tra cứu các đơn vé đã thanh toán thành công (trạng thái CONFIRMED). <br> - Áp dụng phân quyền nghiêm ngặt theo cụm rạp của tài khoản đang đăng nhập; không được phép tra cứu đơn vé của chi nhánh rạp khác. <br> - Cho phép tra cứu ngược đơn vé bằng cách gọi API chọn theo Phòng chiếu -> Suất chiếu -> Vị trí ghế đã bán. <td colspan=3/> |

---

### Bảng A.38: Use case đổi ghế đền bù cho khách hàng khi phát sinh sự cố

| Mã Use case | UC-38 | Tên Use Case | Đổi ghế đền bù cho khách hàng khi phát sinh sự cố |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé / Quản lý rạp |
| Mô tả | Thực hiện đổi vị trí chỗ ngồi cho khách hàng sang ghế trống mới tương đương trong cùng suất chiếu theo cơ chế cập nhật tại chỗ (In-place Repointing), giữ nguyên mã vé/QR code gốc, cấp voucher đền bù thiện chí (nếu có) và in lại vé mới cho khách. <td colspan=3/> |
| Luồng chạy | Bước 1: Sau khi tra cứu thông tin vé thành công, nhân viên chọn chế độ "Đổi ghế" (Relocate). <br> Bước 2: Nhân viên nhấn nút "Chọn" tại ghế nguồn gặp sự cố trong danh sách ghế của đơn; ghế nguồn đổi sang viền sáng màu xanh dương trên sơ đồ. <br> Bước 3: Nhân viên click chọn vị trí ghế đích còn trống (ô màu xám) trên sơ đồ phòng chiếu; ghế đích chuyển sang màu xanh lá và hiển thị ánh xạ (ví dụ: B5 -> C7). Hỗ trợ chọn đổi nhiều ghế cùng lúc cho nhóm khách. <br> Bước 4: Nhân viên chọn hình thức đền bù thiện chí từ danh mục (Không đền bù, Tặng Combo Bắp nước, Voucher giảm 50.000đ, Voucher giảm 100.000đ) và nhập lý do sự cố (ví dụ: Ghế lỗi tựa lưng). <br> Bước 5: Nhân viên nhấn nút "Xác nhận & in lại vé". Hộp thoại xác nhận hiển thị tóm tắt thông tin đổi ghế và gói đền bù để kiểm tra lại. <br> Bước 6: Nhân viên bấm xác nhận; hệ thống kiểm tra xung đột đồng thời, cập nhật đổi khóa ngoại ghế tại chỗ (BookingSeat.seat_id = new_seat_id), sinh voucher đền bù (cho khách có tài khoản), ghi log kiểm toán vào bảng seat_incidents, gửi lại email vé mới cho đơn Online và trả về dữ liệu in lại vé tại quầy POS. <td colspan=3/> |
| Lưu ý | - Chỉ được phép đổi ghế khi suất chiếu CHƯA BẮT ĐẦU. Nếu suất chiếu đã đến hoặc qua giờ bắt đầu, hệ thống khóa chức năng đổi ghế và yêu cầu chuyển sang hủy chỗ. <br> - Không áp dụng chênh lệch thu thêm hoặc hoàn tiền vé (nguyên tắc Flat Pricing trong cùng suất). <br> - Ghế đã từng xử lý sự cố trước đó sẽ bị chặn không cho thao tác lần thứ hai để chống gian lận. <br> - Đối với khách vãng lai (không có tài khoản), nhân viên trao quà trực tiếp tại quầy; hệ thống không sinh voucher điện tử. <td colspan=3/> |

---

### Bảng A.39: Use case hủy chỗ và đền bù bằng Voucher khi phát sinh sự cố phòng chiếu

| Mã Use case | UC-39 | Tên Use Case | Hủy chỗ và đền bù bằng Voucher khi phát sinh sự cố phòng chiếu |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé / Quản lý rạp |
| Mô tả | Xử lý tình huống bất khả kháng khi phòng chiếu không còn ghế trống phù hợp hoặc suất chiếu đã bắt đầu; hệ thống thực hiện hủy chỗ trên đơn vé, giải phóng ghế và đền bù 100% giá trị bằng Voucher vé xem phim miễn phí (Vé mời) hoặc Voucher giảm giá. <td colspan=3/> |
| Luồng chạy | Bước 1: Sau khi tra cứu đơn vé, nhân viên chuyển sang chế độ "Hủy chỗ" (Cancel). <br> Bước 2: Nhân viên tích chọn vào ô checkbox của các ghế cần hủy trong danh sách ghế của đơn hàng. <br> Bước 3: Hệ thống tự động tính toán tổng giá trị vé bị hủy dựa trên giá snapshot ban đầu (totalValue = sum(priceSnapshot)). <br> Bước 4: Nhân viên chọn mẫu voucher đền bù (cho phép chọn mẫu Đền bù: Vé mời COMP_TICKET_FULL hoặc voucher giảm giá) và nhập lý do hủy chỗ. <br> Bước 5: Nhân viên nhấn nút "Hủy chỗ & đền bù" và xác nhận thông báo chính sách không hoàn tiền mặt mà đền bù bằng voucher tương đương. <br> Bước 6: Hệ thống cập nhật trạng thái dòng ghế sang CANCELLED, lập tức giải phóng vị trí ghế trên sơ đồ, khởi tạo Voucher đền bù (hạn 90 ngày) vào tài khoản khách hàng và ghi vết lịch sử vào bảng seat_incidents. <td colspan=3/> |
| Lưu ý | - Nghiêm cấm hoàn tiền mặt hoặc chuyển khoản; toàn bộ bồi thường thực hiện qua voucher hoặc quà tặng theo quy định vận hành của rạp. <br> - Ghế bị hủy chuyển sang trạng thái CANCELLED sẽ không được in lại vé. <br> - Mẫu đền bù Vé mời (COMP_TICKET_FULL) chỉ được kích hoạt và sử dụng tại luồng Hủy chỗ. <td colspan=3/> |

---

### Bảng A.40: Use case khóa bảo trì ghế hỏng vật lý tại phòng chiếu

| Mã Use case | UC-40 | Tên Use Case | Khóa bảo trì ghế hỏng vật lý tại phòng chiếu |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé / Quản lý rạp |
| Mô tả | Cho phép nhân viên hoặc quản lý lập tức khóa trạng thái vật lý của một ghế bị hư hỏng (gãy ngả lưng, rách đệm, bẩn, lỗi thiết bị) sang chế độ bảo trì để ngăn chặn việc mở bán ghế ở toàn bộ các suất chiếu trong tương lai. <td colspan=3/> |
| Luồng chạy | Bước 1: Tại màn hình xử lý sự cố (hoặc màn hình quản trị cơ sở vật chất phòng chiếu), nhân viên xác định ghế bị hư hỏng. <br> Bước 2: Nhân viên nhấn vào biểu tượng cờ-lê (Báo hỏng) trên dòng thông tin ghế gặp sự cố. <br> Bước 3: Hộp thoại xác nhận hiển thị cảnh báo: "Đánh dấu ghế [Tên ghế] là BẢO TRÌ? Ghế sẽ ngừng được bán ở mọi suất tiếp theo cho tới khi mở lại." <br> Bước 4: Nhân viên nhập lý do báo hỏng và bấm "Khóa ghế". <br> Bước 5: Hệ thống cập nhật trạng thái vật lý của ghế trong cơ sở dữ liệu sang "MAINTENANCE" (hoặc "LOCKED"), đổi màu ghế sang đỏ trên sơ đồ và ghi nhận 1 bản ghi sự cố có incident_type = 'SEAT_MAINTENANCE'. <td colspan=3/> |
| Lưu ý | - Ghế khi đã ở trạng thái MAINTENANCE sẽ bị loại trừ hoàn toàn khỏi danh sách ghế mở bán ở mọi suất chiếu tạo mới hoặc suất chiếu sắp tới của phòng đó. <br> - Khi ghế đã hoàn tất công tác bảo dưỡng, sửa chữa vật lý, quản lý có thể thao tác mở lại trạng thái hoạt động (AVAILABLE). <td colspan=3/> |

---

## PHẦN 2: RÀNG BUỘC NGHIỆP VỤ & QUY TẮC VẬN HÀNH

### 1. Điều kiện đầu vào tra cứu
- **Nhận diện tự động:**
  - Định dạng số điện thoại Việt Nam (`^\d{9,11}$`): Tìm kiếm đơn hàng gần nhất có trạng thái `CONFIRMED` gắn với khách hàng.
  - Định dạng mã đơn vé (ví dụ: `BK...`): Tìm kiếm trực tiếp theo mã đơn.
- **Tính hợp lệ:** Đơn hàng phải tồn tại và có `status = 'CONFIRMED'`. Đơn chưa thanh toán hoặc đã hủy bị từ chối.
- **Truy vấn ngược (Seat Occupant):** Cho phép tra cứu đơn vé đang giữ ghế đã bán (`SOLD`) theo `showtimeId` và `seatId`.

### 2. Quy tắc đổi ghế, hủy chỗ & khóa ghế
- **Đổi ghế (Relocate):**
  - Chỉ thực hiện khi suất chiếu chưa bắt đầu (`startTime > NOW()`). Khi suất đã bắt đầu (`started = true`), hệ thống khóa nút đổi ghế.
  - **Repoint tại chỗ:** Cập nhật `BookingSeat.seat_id = new_seat_id`, giữ nguyên mã vé, mã QR code gốc và giá snapshot. Tự động gửi lại Email vé cho đơn Online và hỗ trợ in lại vé tại quầy.
  - Ghế đích phải cùng phòng chiếu, là ô ghế hợp lệ (`is_seat_cell = true`, `is_active = true`), đang ở trạng thái `AVAILABLE`, và không bị chiếm giữ bởi giao dịch khác.
  - Không cho phép chuyển nhiều ghế nguồn về cùng một vị trí ghế đích.
  - Ghế đã từng xử lý sự cố (`RELOCATE` / `CANCEL` trong `seat_incidents`) sẽ bị chặn xử lý lần 2 (Idempotency).
  - Nhận diện hạ hạng ghế (`SWEETBOX > VIP > NORMAL`) để gắn cờ `downgrade = true`.
- **Hủy chỗ (Cancel):**
  - Cập nhật `BookingSeat.status = 'CANCELLED'`, giải phóng vị trí ghế trên sơ đồ.
  - Không in lại vé sau khi hủy chỗ.
- **Khóa ghế hỏng (Maintenance):**
  - Cập nhật `Seat.seat_status = 'MAINTENANCE'`, ngừng bán ghế này ở mọi suất chiếu tương lai của phòng chiếu cho đến khi được mở lại (`AVAILABLE`).
  - Ghi vết sự cố `SEAT_MAINTENANCE`.

### 3. Chính sách đền bù
- **Nguyên tắc:** TUYỆT ĐỐI KHÔNG HOÀN TIỀN (No Cash/Bank Refund).
- **Mẫu Voucher đền bù chuẩn hóa (`COMP_*`):**
  - `COMP_FNB_COMBO` (`GIFT_FNB`): Tặng combo bắp nước trực tiếp.
  - `COMP_50K` (`DISCOUNT`): Voucher giảm giá 50.000đ.
  - `COMP_100K` (`DISCOUNT`): Voucher giảm giá 100.000đ.
  - `COMP_TICKET_FULL` (`GIFT_TICKET`): Vé mời xem phim (chỉ dùng cho luồng Hủy chỗ).
- **Phân loại đối tượng khách hàng:**
  - Khách hàng thành viên (`hasCustomer = true`): Sinh bản ghi `Voucher` mới trong database (hạn dùng 90 ngày).
  - Khách hàng vãng lai (`hasCustomer = false`): Không sinh voucher online; nhân viên tặng quà tại quầy, ghi log sự cố với `counterGift = true` và `voucher_id = null`.
- **Giá trị đền bù:**
  - Đổi ghế: Flat pricing, giá trị đền bù thiện chí (goodwill).
  - Hủy chỗ: Giá trị đền bù bằng đúng tổng giá snapshot của các ghế bị hủy (`totalValue = sum(priceSnapshot)`).

### 4. Ma trận phân quyền
- **Feature Permission Code:** `incident_handling` (Actions: `view`, `handle`).
- **Phân bổ theo vai trò:**
  - `ADMIN`: Toàn quyền trên toàn hệ thống (không giới hạn cụm rạp).
  - `MANAGER`: Toàn quyền trong phạm vi cụm rạp trực thuộc (Strict Cinema Scoping).
  - `STAFF`: Quyền xem và xử lý sự cố trong phạm vi cụm rạp trực thuộc (Strict Cinema Scoping).
- Thao tác chéo cụm rạp bị từ chối truy cập (HTTP 403 Forbidden).

---

## PHẦN 3: LOGIC KỸ THUẬT & TÁC ĐỘNG DATABASE

### 1. Danh mục API Endpoints

| Method | Endpoint | Quyền hạn | Chức năng |
|---|---|---|---|
| `GET` | `/api/staff/incidents/lookup` | `incident_handling:view` | Tra cứu đơn vé theo mã hoặc SĐT |
| `GET` | `/api/staff/incidents/seat-occupant` | `incident_handling:view` | Tra cứu ngược đơn vé từ vị trí ghế |
| `GET` | `/api/staff/incidents/compensation-options` | `incident_handling:view` | Lấy danh mục mẫu voucher đền bù `COMP_*` |
| `GET` | `/api/seats/showtime/{showtimeId}` | Public / POS | Lấy ma trận sơ đồ ghế live |
| `PATCH` | `/api/staff/incidents/seats/{id}/status` | `incident_handling:handle` | Khóa/mở trạng thái vật lý ghế |
| `POST` | `/api/staff/incidents/relocate` | `incident_handling:handle` | Thực hiện đổi ghế & đền bù |
| `POST` | `/api/staff/incidents/cancel` | `incident_handling:handle` | Thực hiện hủy chỗ & đền bù |
| `GET` | `/api/staff/incidents` | `incident_handling:view` | Xem lịch sử sự cố (phân trang + lọc) |
| `GET` | `/api/staff/incidents/{id}` | `incident_handling:view` | Xem chi tiết 1 sự cố |

### 2. Tác động Database
- `seat_incidents`: Bảng kiểm toán lưu vết. Insert 1 dòng cho mỗi ghế xử lý. Lưu các trường: `incident_type`, `booking_id`, `showtime_id`, `old_seat_id`, `new_seat_id`, `old_seat_label`, `new_seat_label`, `compensation_type`, `compensation_amount`, `voucher_id`, `reason`, `handled_by`, `cinema_id`, `created_at`.
- `booking_seats`:
  - Khi Đổi ghế: `UPDATE booking_seats SET seat_id = :newSeatId WHERE id = :bookingSeatId`
  - Khi Hủy chỗ: `UPDATE booking_seats SET status = 'CANCELLED' WHERE id = :bookingSeatId`
- `seats`: Khi Báo hỏng: `UPDATE seats SET seat_status = 'MAINTENANCE' WHERE id = :seatId`
- `vouchers`: Khi đền bù cho khách có tài khoản: Insert bản ghi voucher mới (hạn dùng `NOW() + 90 ngày`).

---

## PHẦN 4: MA TRẬN KỊCH BẢN KIỂM THỬ (TEST MATRIX)

### 1. Kịch bản kiểm thử Thành công (Pass Cases)

| Mã kiểm thử | Mục tiêu kiểm thử | Dữ liệu & Điều kiện đầu vào | Kết quả mong đợi (Pass Criteria) |
|---|---|---|---|
| TC-PASS-01 | Tra cứu đơn hợp lệ bằng Mã đặt vé | Nhập mã vé `BK10029`, trạng thái CONFIRMED | Hiển thị đầy đủ thông tin đơn vé và load sơ đồ ghế của suất. |
| TC-PASS-02 | Tra cứu đơn hợp lệ bằng Số điện thoại | Nhập SĐT `0901234567` có đơn vé đã xác nhận | Tự động nhận diện định dạng SĐT và hiển thị đơn vé CONFIRMED gần nhất. |
| TC-PASS-03 | Đổi 1 ghế không kèm đền bù (NONE) | Ghế A1 đổi sang A2 (trống), suất chưa chiếu, chọn NONE | Repoint BookingSeat sang A2, ghi log `seat_incidents` (RELOCATE), in lại vé mới thành công. |
| TC-PASS-04 | Đổi ghế có cấp Voucher giảm giá cho thành viên | Đơn có User, ghế B1 đổi sang B2, chọn mẫu COMP_50K | Đổi ghế thành công, sinh bản ghi vouchers mới (hạn 90 ngày), hiển thị mã voucher trên kết quả. |
| TC-PASS-05 | Đổi ghế cho khách vãng lai (tại quầy) | Đơn không có Customer, chọn COMP_FNB_COMBO | Đổi ghế thành công, counterGift = true, ghi log sự cố với voucher_id = null, thông báo đền trực tiếp tại quầy. |
| TC-PASS-06 | Đổi đồng thời nhiều ghế cho đoàn khách | Đơn có 2 ghế C1, C2 đổi sang D1, D2 | Repoint cả 2 ghế, sinh 2 dòng seat_incidents, gói đền bù chỉ gắn vào dòng đầu tiên (tránh nhân bản tiền). |
| TC-PASS-07 | Khóa bảo trì ghế hỏng trực tiếp | Nhấn nút Báo hỏng tại ghế E5, nhập lý do "Lỗi đệm ngồi" | Seat.seat_status chuyển MAINTENANCE, ghế chuyển đỏ trên sơ đồ, sinh bản ghi SEAT_MAINTENANCE. |
| TC-PASS-08 | Hủy chỗ và đền bù Vé mời | Ghế F3 hủy, chọn mẫu COMP_TICKET_FULL | BookingSeat.status chuyển CANCELLED, giải phóng vị trí ghế trên sơ đồ, cấp voucher vé mời cho khách. |
| TC-PASS-09 | Lọc và phân trang lịch sử sự cố | Tab Lịch sử, chọn lọc theo loại RELOCATE và khoảng ngày | Bảng lịch sử lọc chính xác các bản ghi thuộc cụm rạp, phân trang dữ liệu hoạt động mượt mà. |

### 2. Kịch bản kiểm thử Thất bại & Thông báo lỗi (Fail Cases)

| Mã kiểm thử | Thao tác gây lỗi | Cơ chế phát hiện & Mã HTTP | Thông báo lỗi hiển thị cho người dùng |
|---|---|---|---|
| TC-FAIL-01 | Bấm nút tìm kiếm khi ô tra cứu rỗng | Client Validation | Nhập mã vé hoặc số điện thoại khách. |
| TC-FAIL-02 | Nhập mã vé không tồn tại trong hệ thống | IllegalArgumentException (HTTP 400) | Không tìm thấy vé phù hợp. |
| TC-FAIL-03 | Nhập SĐT không có đơn đặt vé nào | IllegalArgumentException (HTTP 400) | Không tìm thấy đơn đã xác nhận cho số điện thoại này. |
| TC-FAIL-04 | Tra cứu đơn vé chưa thanh toán hoặc đã hủy | IllegalArgumentException (HTTP 400) | Đơn chưa thanh toán hoặc không hợp lệ để xử lý. |
| TC-FAIL-05 | Nhân viên rạp A cố tra cứu/xử lý vé của rạp B | AccessDeniedException (HTTP 403) | Bạn không có quyền thực hiện thao tác này. |
| TC-FAIL-06 | Đổi ghế khi suất chiếu đã bắt đầu | IllegalArgumentException (HTTP 400) | Suất đã bắt đầu — chỉ có thể hủy chỗ, không đổi ghế. |
| TC-FAIL-07 | Ghế đích vừa bị người khác đặt (Race Condition) | IllegalStateException (HTTP 409) | Ghế đích vừa bị chiếm bởi giao dịch khác. Vui lòng chọn ghế trống khác. |
| TC-FAIL-08 | Chọn nhiều ghế nguồn đổi về cùng 1 ghế đích | IllegalArgumentException (HTTP 400) | Không thể đổi nhiều ghế về cùng một vị trí đích. |
| TC-FAIL-09 | Chọn ghế đích đang bảo trì hoặc đang bị khóa | IllegalArgumentException (HTTP 400) | Ghế đích đang bảo trì/khóa, không thể chuyển tới. |
| TC-FAIL-10 | Xử lý sự cố lần 2 cho ghế đã đổi/hủy trước đó | IllegalStateException (HTTP 409) | Ghế [Tên ghế] đã được xử lý sự cố trước đó. |
| TC-FAIL-11 | Chọn mẫu vé mời COMP_TICKET_FULL khi đổi ghế | IllegalArgumentException (HTTP 400) | Mẫu đền nguyên vé chỉ dùng cho luồng hủy chỗ. |
| TC-FAIL-12 | Chọn mẫu voucher không có tiền tố COMP_* | IllegalArgumentException (HTTP 400) | Chỉ được dùng mẫu voucher đền bù (COMP_*). |
| TC-FAIL-13 | Mất kết nối mạng trong quá trình gửi yêu cầu | Network Error (ERR_NETWORK) | Mất kết nối máy chủ, vui lòng kiểm tra mạng và thử lại. |
| TC-FAIL-14 | Hết hạn phiên đăng nhập khi đang thao tác | Token Expired (HTTP 401) | Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại. |
