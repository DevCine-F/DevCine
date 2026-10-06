# MÔ TẢ CHI TIẾT CHỨC NĂNG PHÂN HỆ QUẢN TRỊ (ADMIN) — DEVCINE

## X.0. Giới thiệu chung về phân hệ Quản trị (Admin)
Phân hệ Quản trị (Admin Portal) của DevCine đóng vai trò là "trung tâm điều hành và kiểm soát dữ liệu" toàn diện cho chuỗi rạp chiếu phim, được xây dựng trên nền tảng kiến trúc hiện đại (Vue 3, Vite, Pinia, TailwindCSS kết hợp Spring Boot Backend). Hệ thống được thiết kế chuyên biệt nhằm phục vụ 3 nhóm đối tượng quản trị và vận hành: Quản trị viên hệ thống (Super Admin), Quản lý cụm rạp (Cinema Manager), và Nhân viên nghiệp vụ (Staff / Cashier).

Phân hệ giải quyết triệt để các bài toán kỹ thuật và nghiệp vụ cốt lõi trong source code:
1. **Đồng bộ hóa dữ liệu thời gian thực (Real-time Sync):** Tích hợp kết nối WebSocket/STOMP (`useSeatRealtime`) kết hợp cơ chế Polling và bộ đếm giữ ghế (Seat Holding Timer 5 phút) để đồng bộ trạng thái ghế tức thì giữa các quầy POS và người dùng đặt vé online, ngăn chặn xung đột bán trùng ghế.
2. **Quản trị rạp đa chi nhánh & Sơ đồ ma trận linh hoạt:** Cho phép mở rộng không giới hạn các cụm rạp (`CinemaManager`), hỗ trợ công cụ thiết kế ma trận ghế trực quan (`CinemaSeatMapView`) tương thích với kiến trúc vật lý thực tế của từng phòng chiếu.
3. **Tự động hóa xếp lịch & Thuật toán chống xung đột (Conflict Resolution):** Hệ thống timeline xếp lịch chiếu (`CinemaShowtimesTab`) tự động phát hiện trùng khung giờ chiếu, sai lệch định dạng phòng/phim, hỗ trợ cả lên lịch đơn lẻ và lên lịch hàng loạt (`BatchShowtimeDrawer`).
4. **Mô hình giá vé linh hoạt (Flat Pricing Engine):** Tính toán giá vé tự động dựa trên ma trận kết hợp: Loại phòng × Loại ngày (Ngày thường, Cuối tuần, Ngày lễ) × Đối tượng khách hàng (Người lớn, HSSV/U22, Trẻ em, Cao tuổi) + Phụ thu công nghệ định dạng (2D, 3D, IMAX...).
5. **Cơ chế phân quyền chặt chẽ (RBAC Matrix):** Quản lý quyền hạn chi tiết theo từng cặp `Feature × Action` (View, Add, Edit, Delete, Export, Manage) thông qua composable `useAdminPerm` và Router Guards, đảm bảo an toàn dữ liệu và tính chuyên môn hóa nghiệp vụ.

---

### X.1. Phân hệ Báo cáo và Thống kê (Dashboard)
**Hình X.1: Màn hình Bảng điều khiển Thống kê (Dashboard)**
**Giới thiệu chung:** 
Màn hình Dashboard (`Dashboard.vue`) là trung tâm phân tích dữ liệu kinh doanh thời gian thực của DevCine. Hệ thống tự động tổng hợp toàn bộ các giao dịch từ cả kênh Online và quầy POS, cung cấp bức tranh toàn cảnh về hiệu quả hoạt động với các bộ lọc linh hoạt: Hôm nay, 7 ngày qua (Tuần), và Theo tháng (với bộ chọn lịch Tháng/Năm tùy biến).
- **Bộ thẻ chỉ số KPI chính:** 
  - *Tổng doanh thu:* Doanh thu tổng hợp từ vé và F&B, đi kèm chỉ số % tăng/giảm so với kỳ trước.
  - *Vé đã bán:* Tổng số lượng vé đã xuất trên toàn hệ thống hoặc theo chi nhánh.
  - *Khách mới của cơ sở:* Số lượng khách hàng thực hiện giao dịch lần đầu tại rạp.
  - *Tỷ lệ lấp đầy (Occupancy Rate):* Tỷ lệ phần trăm giữa tổng số vé bán ra trên tổng sức chứa ghế của tất cả các suất chiếu đã chạy.
- **Biểu đồ Hiệu quả Kinh doanh (SVG Chart):** Biểu đồ kết hợp trực quan biểu diễn đường Doanh thu (vùng diện tích gradient) và Lượng vé bán ra (đường nét đứt), tích hợp Tooltip tương tác hiển thị chi tiết số liệu khi di chuột qua từng mốc thời gian.
- **Bảng xếp hạng Phim hàng đầu (Top Movies):** Danh sách các bộ phim đạt doanh thu và số lượng vé bán cao nhất trong khoảng thời gian được chọn, kèm ảnh Poster và doanh thu chi tiết.
- **Theo dõi Hoạt động Vận hành trong ngày:**
  - *Giao dịch gần đây (Recent Bookings):* Danh sách đơn hàng mới nhất thể hiện mã đơn, tên phim, khách hàng, kênh bán (POS/Online), thời gian và giá trị thanh toán.
  - *Tiến độ Suất chiếu (Showtime Occupancy):* Bảng theo dõi các suất chiếu trong ngày kèm thanh tiến trình đo lường tỷ lệ lấp đầy ghế theo thời gian thực (số vé đã bán / tổng ghế phòng).
- **Xuất báo cáo:** Hỗ trợ tính năng xuất dữ liệu báo cáo thống kê phục vụ công tác đối soát kế toán.

---

### X.2. Phân hệ Bán hàng & Dịch vụ (POS & Giao dịch)

**X.2.1. Chức năng Bán vé tại quầy (Ticketing POS)**
**Hình X.2: Màn hình Bán vé tại quầy (Ticketing POS)**
**Giới thiệu chung:** 
Giao diện bán hàng chuyên dụng (`TicketingPOS.vue`) được tối ưu hóa cho nhân viên thu ngân tại quầy giao dịch, hỗ trợ 2 luồng thao tác độc lập: Bán vé kèm F&B (`TICKET`) và Bán nhanh bắp nước độc lập (`FNB`). Phân hệ tích hợp các công nghệ đồng bộ hóa ghế ngồi thời gian thực, thuật toán kiểm tra ghế lẻ (Orphan Seat Check) và quản lý hàng đợi đơn hàng tạm giữ.
- **Bộ lọc và Danh sách Suất chiếu:** Lọc suất chiếu nhanh theo các tab ngày (Hôm nay, Ngày mai, và các ngày tới), tự động ẩn các suất đã quá giờ chiếu theo cấu hình thời gian trễ (`LATE_BOOKING_MINUTES`). Phim được gom nhóm theo Định dạng và Phòng chiếu trực quan.
- **Sơ đồ ghế Real-time & Bộ đếm giữ ghế:**
  - Hiển thị ma trận sơ đồ ghế phòng chiếu với phân màu trạng thái: Ghế trống, Ghế đang chọn, Ghế đang được giữ bởi quầy khác/online, Ghế đã bán, và Ghế bảo trì.
  - Bộ đếm thời gian giữ ghế 5 phút (`HOLD_SECONDS = 300s`) đếm lùi trực tiếp, tự động giải phóng ghế khi hết hạn.
  - Thuật toán `useOrphanSeatCheck` ngăn chặn việc chọn ghế để lại 1 ghế trống đơn lẻ; hỗ trợ nút gạt Override dành riêng cho Admin/Manager khi cần xử lý ngoại lệ.
- **Phân bổ Đối tượng vé (Ticket Audience Counter):** Cho phép tăng/giảm linh hoạt số lượng vé theo từng đối tượng (Người lớn, HSSV/U22, Trẻ em, Cao tuổi) khớp với tổng số ghế đã chọn và tự động áp dụng bảng giá tương ứng.
- **Bán kèm F&B và Tùy chọn món (Modal Option):** Chọn nhanh các món bắp nước/combo, hỗ trợ cửa sổ popup chọn vị bắp (Ngọt, Mặn, Phô mai, Caramel...) và kích cỡ nước (Vừa, Lớn).
- **Tích hợp Hội viên & Voucher:** Tra cứu thông tin khách hàng thành viên qua Số điện thoại/Mã thẻ để tích điểm và áp dụng mã giảm giá (Voucher) hợp lệ của khách hàng.
- **Đa dạng Phương thức Thanh toán:** Hỗ trợ thanh toán Tiền mặt (tự động tính tiền thừa trả khách), Quét mã QR chuyển khoản ngân hàng (VietQR tự động sinh mã theo đơn), và in vé điện tử/hóa đơn bán hàng chuẩn nhiệt.
- **Quản lý Đơn chờ (Pending/Hold Orders):** Tính năng lưu tạm tối đa 3 đơn hàng đang phục vụ dở dang vào danh sách đơn chờ để phục vụ khách tiếp theo, cho phép khôi phục lại đơn hàng kèm trạng thái ghế bất cứ lúc nào.
- **Yêu cầu Hủy đơn F&B (Void Request):** Gửi yêu cầu hủy hóa đơn bắp nước đã xuất nhầm để Trưởng ca/Quản lý duyệt (nhân viên thu ngân không thể tự ý xóa đơn).

**X.2.2. Chức năng Quản lý Đơn hàng (Bookings)**
**Hình X.3: Màn hình Quản lý Đơn đặt vé (Bookings)**
**Giới thiệu chung:** 
Màn hình `AdminBookings.vue` cung cấp công cụ theo dõi, tra cứu và đối soát toàn bộ các đơn hàng phát sinh trên toàn hệ thống (bao gồm đơn đặt online qua Web/App và đơn xuất tại quầy POS).
- **Danh sách Đơn hàng Toàn diện:** Bảng dữ liệu hiển thị chi tiết Mã đơn hàng (`bookingCode`), Thông tin khách hàng (Tên, SĐT, Email), Tên phim, Suất chiếu, Phòng/Rạp, Danh sách ghế, Combo F&B, Kênh đặt (Online/POS), Tổng tiền và Thời gian tạo.
- **Bộ lọc & Tìm kiếm Đa tiêu chí:** Lọc nhanh theo trạng thái giao dịch (Đã thanh toán `CONFIRMED`, Chờ thanh toán `PENDING`, Đã hủy `CANCELLED`, Đã hoàn tiền `REFUNDED`), lọc theo khoảng ngày, theo cụm rạp, hoặc tìm kiếm chính xác theo mã đơn/SĐT.
- **Chi tiết Đơn & Nghiệp vụ Xử lý:** Xem chi tiết mã QR của vé, lịch sử thanh toán; hỗ trợ quyền hủy đơn hàng hoặc xử lý hoàn tiền cọc theo chính sách của rạp.

**X.2.3. Chức năng Soát vé Khách hàng (Ticket Check-in)**
**Hình X.4: Màn hình Soát vé vào rạp (Ticket Check-in)**
**Giới thiệu chung:** 
Giao diện nghiệp vụ `TicketCheckIn.vue` được thiết kế tối ưu cho nhân viên kiểm soát tại cửa phòng chiếu, hỗ trợ quét mã vạch/QR tốc độ cao để xác thực vé vào rạp.
- **Quét mã QR đa phương thức:** Hỗ trợ quét trực tiếp qua Camera thiết bị, sử dụng đầu đọc mã vạch chuyên dụng (Barcode Scanner), hoặc nhập thủ công mã vé/số điện thoại của khách.
- **Xác thực trạng thái vé tức thời:**
  - *Vé hợp lệ (Màu xanh):* Hiển thị âm thanh thông báo thành công, thông tin số ghế, phòng chiếu, định dạng phim và giờ chiếu.
  - *Vé không hợp lệ/Cảnh báo (Màu đỏ/Vàng):* Cảnh báo rõ lý do (Vé đã check-in trước đó kèm thời gian cụ thể, Vé bị hủy, Vé chưa thanh toán, hoặc Sai suất chiếu/phòng chiếu).
- **Thống kê Tiến độ Soát vé:** Hiển thị thanh tiến trình check-in của từng suất chiếu (Số lượng khách đã vào rạp / Tổng số vé đã bán).

---

### X.3. Phân hệ Quản lý Phim & Lịch chiếu

**X.3.1. Chức năng Quản lý Phim & Danh mục (Movies & Categories)**
**Hình X.5: Màn hình Quản lý Danh sách Phim**
**Giới thiệu chung:** 
Phân hệ `AdminMovies.vue` và `MovieCategoryManager.vue` quản lý toàn bộ vòng đời nội dung phim trên hệ thống từ lúc chuẩn bị phát hành đến khi ngừng chiếu, tích hợp lưu trữ media đám mây (Cloudinary) và kiểm soát ràng buộc dữ liệu nghiêm ngặt.
- **Bộ lọc & Tìm kiếm Phim Nâng cao:** Tìm kiếm theo tên phim, đạo diễn; lọc theo Trạng thái (Đang chiếu, Sắp chiếu, Lưu trữ), Quốc gia, Độ tuổi, Thể loại, và Định dạng (2D, 3D, IMAX...).
- **Biên tập Thông tin Phim:**
  - Nhập chi tiết Tên phim (Gốc & Tiếng Việt), Đạo diễn, Diễn viên, Thời lượng (phút), Ngày khởi chiếu, Ngày kết thúc, Giới hạn độ tuổi (P, K, T13, T16, T18, C), Tóm tắt nội dung.
  - Upload ảnh Poster/Banner trực tiếp lên hệ thống Cloudinary với tính năng tự động tối ưu hóa kích thước (`optimizeCloudinaryUrl`), nhúng link Trailer Youtube.
- **Cơ chế Đổi Trạng thái Phim 4 Lớp An toàn:**
  - *Lớp 1:* Chặn chuyển thành "Sắp chiếu" nếu Ngày khởi chiếu nhỏ hơn hoặc bằng ngày hiện tại.
  - *Lớp 2:* Chặn chuyển sang "Lưu trữ" hoặc "Sắp chiếu" nếu bộ phim vẫn còn các suất chiếu đang hoạt động (`hasActiveShowtimes`).
  - *Lớp 3:* Hiển thị hộp thoại xác nhận cảnh báo khi ngừng chiếu phim (phim sẽ bị ẩn khỏi giao diện bán vé của khách).
  - *Lớp 4:* Thực thi cập nhật và đồng bộ trạng thái hiển thị.
- **Thao tác Hàng loạt (Bulk Actions):** Cho phép chọn nhiều phim để đổi trạng thái hàng loạt hoặc xóa hàng loạt.
- **Quản lý Danh mục (Category Manager):** Quản lý danh mục Thể loại phim (Genres) và Danh mục Định dạng chiếu (Formats).

**X.3.2. Chức năng Quản lý Lịch chiếu (Showtimes)**
**Hình X.6: Màn hình Quản lý Lịch chiếu Timeline**
**Giới thiệu chung:** 
Nằm trong phân hệ `CinemaManager.vue` (Tab Lịch chiếu `CinemaShowtimesTab.vue`), cung cấp công cụ xếp lịch chiếu dạng lưới thời gian (Timeline Grid) trực quan, tự động hóa việc phát hiện xung đột và hỗ trợ xếp lịch linh hoạt.
- **Timeline Lịch chiếu Trực quan:** Hiển thị toàn bộ các phòng chiếu theo trục ngang và trục thời gian từ 08:00 đến 24:00+ với các vạch chia giờ, vạch đánh dấu thời gian hiện tại (`Current Time Indicator`).
- **Thuật toán Phát hiện Xung đột (Conflict Checking):** Tự động tính toán giờ kết thúc của suất chiếu (Thời lượng phim + Thời gian dọn phòng/nghỉ giữa các suất). Cảnh báo viền đỏ nổi bật nếu có sự chồng chéo thời gian giữa hai suất chiếu trong cùng một phòng.
- **Kiểm tra Sai lệch Định dạng (Format Mismatch):** Tự động phát hiện và cảnh báo nếu xếp phim có định dạng không tương thích với phòng chiếu (Ví dụ: Xếp phim 3D vào phòng chỉ hỗ trợ 2D).
- **Tương tác Kéo - Thả (Drag & Drop):** Hỗ trợ kéo thả khối suất chiếu trên timeline để điều chỉnh phòng chiếu hoặc dời khung giờ một cách nhanh chóng.
- **Thêm Suất chiếu Đơn & Hàng loạt (Batch Showtimes):**
  - *ShowtimeDrawer:* Khởi tạo 1 suất chiếu cụ thể cho phim, chọn phòng, ngày và giờ bắt đầu.
  - *BatchShowtimeDrawer:* Công cụ sinh lịch chiếu hàng loạt theo khoảng ngày, áp dụng khung giờ lặp lại cho nhiều cụm rạp và phòng chiếu cùng lúc.
- **Xem Chi tiết & Kiểm tra Sơ đồ Ghế Suất chiếu (`ShowtimeDetailsDrawer`):** Xem thông tin doanh thu, số vé đã bán của suất; mở popup kiểm tra trực tiếp trạng thái từng ghế thực tế của suất chiếu (`SOLD`, `HOLD`, `MAINTENANCE`).

---

### X.4. Phân hệ Cơ sở vật chất & Sản phẩm

**X.4.1. Chức năng Quản lý Cụm rạp & Sơ đồ ghế (Cinema & Seat Map)**
**Hình X.7: Màn hình Quản trị Cụm rạp và Sơ đồ ghế**
**Giới thiệu chung:** 
Phân hệ `CinemaManager.vue` và `CinemaSeatMapView.vue` cho phép cấu hình và số hóa toàn bộ hệ thống cơ sở hạ tầng vật lý của chuỗi rạp chiếu phim DevCine.
- **Quản lý Danh sách Cụm rạp:** Thêm mới, chỉnh sửa thông tin chi nhánh (Tên rạp, Tỉnh/Thành phố, Quận/Huyện, Địa chỉ, Số điện thoại hotline, Hình ảnh). Tích hợp bộ lọc rạp theo Tỉnh/TP và Quận/Huyện.
- **Quản lý Phòng chiếu (Rooms / Halls):** Thiết lập danh sách các phòng chiếu trực thuộc từng cụm rạp, cấu hình tên phòng, loại phòng (Standard, 3D, IMAX, VIP, Sweetbox), trạng thái hoạt động.
- **Công cụ Thiết kế Ma trận Sơ đồ ghế (`CinemaSeatMapView`):**
  - Khai báo số hàng (`matrixRow`) và số cột (`matrixCol`) của phòng chiếu.
  - Trình vẽ lưới trực quan: Click hoặc quét chọn để định cấu hình từng ô trên lưới thành: Ghế Thường (`NORMAL`), Ghế VIP (`VIP`), Ghế Đôi (`SWEETBOX`), hoặc Lối đi lại (`AISLE`).
  - Đánh số thứ tự hàng (A, B, C...) và cột (1, 2, 3...) tự động, lưu trữ cấu trúc ma trận ghế chuẩn xác để hiển thị đồng nhất lên Web, App và POS.

**X.4.2. Chức năng Cấu hình Giá vé (Admin Pricing)**
**Hình X.8: Màn hình Cấu hình Bảng giá vé (Flat Pricing)**
**Giới thiệu chung:** 
Màn hình `AdminPricing.vue` quản lý toàn bộ chính sách giá vé tự động của hệ thống theo mô hình Flat Pricing kết hợp phụ thu công nghệ.
- **Bảng Giá nền (Base Matrix):** Ma trận thiết lập giá vé cơ sở theo 3 chiều: *Loại phòng* (Standard, IMAX, VIP...) × *Loại ngày* (Ngày thường T2–T5, Cuối tuần T6–CN) × *Đối tượng khách hàng* (Người lớn ADULT, HSSV/U22, Trẻ em CHILD, Cao tuổi SENIOR).
- **Phụ thu Định dạng công nghệ (Format Surcharge):** Cấu hình mức tiền phụ thu cộng thêm cho các định dạng đặc biệt (2D, 3D, IMAX 3D...) riêng biệt cho ngày thường và cuối tuần/lễ.
- **Quản lý Ngày lễ (Holidays):** Khai báo danh sách các ngày lễ đặc biệt trong năm (Tết, 30/4, 1/5...) để hệ thống tự động áp bậc giá "Cao điểm".
- **Bộ Mô phỏng Tính thử giá (Price Simulator):** Công cụ cho phép Admin chọn thử Loại ngày, Đối tượng, Loại phòng và Định dạng để kiểm tra kết quả giá vé tính toán trước khi áp dụng thực tế.

**X.4.3. Chức năng Quản lý Thực đơn Bắp Nước (F&B Menu Manager)**
**Hình X.9: Màn hình Quản lý Sản phẩm F&B**
**Giới thiệu chung:** 
Màn hình `FnbMenuManager.vue` quản lý danh mục sản phẩm đồ ăn, thức uống và các gói Combo bán kèm theo vé.
- **Quản lý Mặt hàng & Combo:** Thêm mới, chỉnh sửa thông tin món (Tên, Ảnh minh họa, Giá bán, Mô tả thành phần combo).
- **Quản lý Tùy chọn món (Options):** Cấu hình các nhóm tùy chọn đi kèm cho sản phẩm như Vị bắp (Ngọt, Mặn, Caramel, Phô mai) và Size nước (Vừa, Lớn) kèm giá chênh lệch.
- **Bật/Tắt Trạng thái Phục vụ:** Nút gạt chuyển đổi nhanh trạng thái "Sẵn sàng" (`is_available`) hoặc "Tạm hết hàng" để đồng bộ ngay lập tức lên giao diện đặt của khách và màn hình bán POS.

---

### X.5. Phân hệ Khách hàng & Truyền thông

**X.5.1. Chức năng Quản lý Khuyến mãi & Banner (Promotions & Banners)**
**Hình X.10: Màn hình Quản lý Khuyến mãi và Banner**
**Giới thiệu chung:** 
Phân hệ `AdminPromotions.vue` và `AdminBanners.vue` cung cấp các công cụ Marketing nhằm gia tăng tỷ lệ chuyển đổi và tương tác với khách hàng.
- **Quản lý Mã Khuyến mãi (Vouchers / Coupons):**
  - Khởi tạo mã giảm giá theo Phần trăm (`PERCENT`) hoặc Số tiền cố định (`FIXED_AMOUNT`).
  - Thiết lập các điều kiện ràng buộc: Giá trị đơn hàng tối thiểu, Mức giảm tối đa, Giới hạn tổng số lượt sử dụng, Giới hạn lượt dùng/khách hàng, Thời hạn hiệu lực (Từ ngày - Đến ngày).
  - Phân loại voucher: Voucher công khai nhập mã, Voucher tri ân gắn trực tiếp cho khách hàng, hoặc Voucher đền bù sự cố.
- **Quản lý Banner Quảng cáo (Hero Banners):** Quản lý danh sách hình ảnh trình chiếu (Slider) trên trang chủ, cài đặt đường dẫn liên kết khi khách nhấp vào banner, thiết lập thứ tự hiển thị và trạng thái kích hoạt.

**X.5.2. Chức năng Quản lý Đánh giá & Khách hàng (Reviews & Customers)**
**Hình X.11: Màn hình Quản lý Đánh giá và Khách hàng**
**Giới thiệu chung:** 
Bao gồm `AdminReviews.vue`, `AdminCustomers.vue`, `CustomerSupport.vue` và `FaqManager.vue` hỗ trợ chăm sóc khách hàng và quản lý danh tiếng thương hiệu.
- **Quản lý và Kiểm duyệt Đánh giá (Reviews):** Theo dõi danh sách bình luận, số sao đánh giá của khán giả theo từng phim; thực hiện ẩn/hiện hoặc xóa các bình luận chứa nội dung không phù hợp hoặc spoil phim.
- **Quản lý Khách hàng Thành viên (Customers):** Danh sách khách hàng đăng ký tài khoản, tra cứu lịch sử chi tiêu, cấp bậc thành viên, điểm tích lũy và các voucher khách hàng đang sở hữu.
- **Hỗ trợ Khách hàng & FAQ:** Tiếp nhận và xử lý các Ticket yêu cầu hỗ trợ từ người dùng; biên tập danh mục Câu hỏi thường gặp (FAQ) hiển thị trên website.

---

### X.6. Phân hệ Quản trị Hệ thống, Nhân sự & Sự cố

**X.6.1. Chức năng Quản lý Nhân sự & Phân quyền (Staff & Permissions)**
**Hình X.12: Màn hình Quản lý Nhân viên và Phân quyền**
**Giới thiệu chung:** 
Phân hệ `StaffManager.vue` và `AdminPermissions.vue` đảm bảo an ninh hệ thống và phân bổ trách nhiệm nghiệp vụ rõ ràng cho toàn bộ nhân viên trong chuỗi rạp.
- **Quản lý Danh sách Nhân viên:** Thêm mới tài khoản nhân viên, gán Vai trò (Role), gán Cụm rạp công tác, hỗ trợ khóa/mở khóa tài khoản và kích hoạt đổi mật khẩu trong lần đăng nhập đầu tiên (`FirstLoginPassword.vue`).
- **Ma trận Phân quyền Nâng cao (RBAC Matrix):** 
  - Định nghĩa chi tiết các vai trò trong hệ thống: Quản trị viên cấp cao (`ADMIN`), Quản lý chi nhánh (`MANAGER`), Nhân viên quầy (`STAFF`).
  - Thiết lập quyền hạn chi tiết theo từng phân hệ chức năng: `dashboard_stats`, `movies`, `cinemas`, `schedules`, `pricing`, `pos_ticketing`, `bookings`, `fnb_menu`, `promotions`, `incident_handling`, `support`, `banners`, `roles`, `audit_logs` kết hợp các hành vi cho phép (`view`, `add`, `edit`, `delete`, `export`, `manage`).
- **Nhật ký Hoạt động (Audit Logs - `AdminLogs.vue`):** Ghi lại chi tiết lịch sử thao tác của các tài khoản trên trang quản trị (Thời gian, Tên nhân viên, Hành động thực hiện, IP truy cập) phục vụ công tác giám sát và hậu kiểm.

**X.6.2. Chức năng Xử lý Sự cố Phòng chiếu (Incident Management)**
**Hình X.13: Màn hình Xử lý Sự cố Phòng chiếu (Incident Management)**
**Giới thiệu chung:** 
Màn hình nghiệp vụ đặc thù `IncidentManagement.vue` xử lý các tình huống phát sinh thực tế tại phòng chiếu (như hỏng ghế, lỗi thiết bị, sự cố kỹ thuật), cung cấp quy trình đổi ghế và đền bù chuyên nghiệp cho khách hàng mà không cần hoàn tiền mặt.
- **Tra cứu Vé Sự cố:** Tìm kiếm nhanh đơn vé đang gặp sự cố theo Mã vé (`bookingCode`) hoặc Số điện thoại khách hàng.
- **Nghiệp vụ Đổi ghế Đền bù (Seat Relocation):**
  - Hiển thị trực quan sơ đồ phòng chiếu của suất: Ghế nguồn của khách (màu xanh dương), Ghế trống khả dụng (màu xám), Ghế đích được chọn (màu xanh lá).
  - Nhân viên chọn ghế bị lỗi và click chọn vị trí ghế mới trên sơ đồ; hệ thống tự động đổi ghế và cấp lệnh in lại vé mới cho khách.
  - Tự động khóa tính năng đổi ghế nếu suất chiếu đã bắt đầu (chỉ cho phép Hủy chỗ đền bù).
- **Nghiệp vụ Hủy chỗ & Khóa ghế Hỏng:**
  - *Hủy chỗ (Cancel Seat):* Hủy vị trí ghế của khách khi phòng chiếu không còn ghế trống phù hợp.
  - *Báo hỏng ghế (Seat Maintenance):* Đánh dấu trực tiếp ghế bị hỏng sang trạng thái Bảo trì (`MAINTENANCE`), hệ thống sẽ tự động khóa và ngừng bán ghế này trên tất cả các suất chiếu tiếp theo cho đến khi được sửa chữa xong.
- **Chính sách Đền bù Tích hợp (Compensation):** Lựa chọn hình thức đền bù đi kèm theo quy định rạp: Phát Voucher giảm giá vé, Tặng Combo F&B, hoặc Cấp Vé mời miễn phí (đối với khách hàng thành viên sẽ nhận voucher điện tử; khách vãng lai nhận quà trực tiếp tại quầy).
- **Lịch sử Xử lý Sự cố:** Tab tra cứu toàn bộ nhật ký xử lý sự cố trong quá khứ theo Loại (Đổi ghế, Hủy chỗ, Khóa ghế), Mã vé, Mã voucher đền bù và Nhân viên phụ trách xử lý.
