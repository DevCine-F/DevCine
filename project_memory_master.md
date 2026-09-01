# Project Memory Master - DevCine System

Tài liệu này lưu trữ và tổng hợp các cột mốc (milestone) đã hoàn thành, quyết định kiến trúc cốt lõi và các cập nhật quan trọng của dự án DevCine.

## Các Milestone Đã Hoàn Thành (Tháng 08/2026)

### 1. Realtime POS Showtime Filter (Lọc Suất Chiếu POS Thời Gian Thực)
- **Đồng bộ thông số:** Đồng bộ biến cấu hình lateSaleMinutes (Bán vé trễ sau giờ chiếu) từ Cài đặt hệ thống (settingsStore).
- **Cơ chế lọc:** Áp dụng thuật toán ẩn suất chiếu hết hạn tính theo từng phút cho tab "Hôm nay" (điều kiện currentMinutes > cutoffMinutes).
- **Tự động quét:** Khởi tạo tính năng rà soát ngầm thời gian thực (Auto-sweep timer), tự động dọn dẹp các suất chiếu đã quá hạn bán vé ngay trên màn hình POS mà không cần tải lại trang (F5).

### 2. Quy chuẩn hóa Loại Phòng Chiếu (Room Types Standardization)
- **Chuẩn hóa khái niệm:** Loại bỏ hoàn toàn 'Sweetbox' khỏi Enum và Dropdown loại phòng, đưa Sweetbox về đúng bản chất logic là "Loại Ghế" (Seat Type).
- **Quy chuẩn 1:1:** Đồng bộ Loại phòng chiếu khớp tuyệt đối với Ma trận Bảng Giá Vé ở Hệ thống: Chỉ còn đúng 3 loại phòng cơ bản [STANDARD, SUPERPLEX, CINE_COMFORT].

### 3. Migration Cụm Rạp & Sơ Đồ Ghế An Toàn (Safe Room & Seat Map Migration)
- **Tái chế thông minh:** Áp dụng thuật toán "Tái chế phòng tại chỗ" (Room Recycling) thay vì xóa-tạo mới. Giữ nguyên ID phòng cũ để bảo vệ tuyệt đối Khóa ngoại (Foreign Key), không làm đứt gãy liên kết của Suất chiếu & Vé đã bán trong quá khứ.
- **Tiêu chuẩn hóa phòng:** Đảm bảo tất cả các Cơ sở Rạp hiện tại đều sở hữu bộ 3 phòng chuẩn ở trạng thái ACTIVE:
  - Phòng 01: Standard (10x16)
  - Phòng 02: Superplex (10x16)
  - Phòng 03: Cine Comfort (8x10)
- **Re-seed dữ liệu:** Phân bổ, tính toán và thiết lập sơ đồ ghế tự động (Ghế Thường, VIP, Sweetbox) cho toàn bộ ~2000 ghế trên Database.

### 4. Khắc Phục Lỗi 500 Khi Lưu Sơ Đồ Ghế (UPSERT & Soft-Delete Pattern)
- **Ngăn chặn Xóa cứng:** Gỡ bỏ hoàn toàn các lệnh Hard Delete (deleteByRoomId, deleteAll) tiềm ẩn nguy cơ ở Runner Startup và các API Services (SeatService, RoomService).
- **Cơ chế An toàn:** Chuyển đổi hoàn toàn sang cơ chế UPSERT (Update-in-place) thao tác theo tọa độ (rowChar, colNum) và Soft Delete (isActive = false) cho các ghế thừa trong lưới mới. Giải quyết triệt để vấn đề rào cản Foreign Key bị PostgreSQL từ chối với bảng ooking_seats.

### 5. Tối Ưu Giao Diện Admin Sơ Đồ Ghế (Seat Map UI Refactoring)
- **Đồng bộ hóa Visual:** Chuẩn hóa Icon công cụ "Lối đi" (Icon oute) và màu sắc (Cyan/Blue #38BDF8) đồng bộ 1:1 với bộ icon của các công cụ khác (Sofa, Kim cương, Trái tim, Cờ-lê, Cục tẩy).
- **Tối ưu Layout Không gian:** Tái cấu trúc gộp chung 2 Card "Cấu hình ma trận" và "Thông số phòng chiếu" thành card **CẤU HÌNH & THỐNG KÊ** đặt trên cùng Sidebar (SeatMapBuilder.vue). Các chỉ số ghế được thiết kế dưới dạng Badges dàn ngang gọn gàng, giúp Admin bao quát toàn bộ thông số theo Real-time mà không cần cuộn trang (No-scroll UX).

### 6. Quy chuẩn tính toán Sức chứa phòng chiếu (Capacity Rule)
- **Công thức tính Sức chứa thực tế:** Ghế Đơn (NORMAL, VIP) = 1 sức chứa; Ghế Đôi (DOUBLE, SWEETBOX, COUPLE) = 2 sức chứa; Lối đi/Ô trống = 0.
- **Áp dụng Real-time UI:** UI Sidebar trên trình chỉnh sửa sơ đồ ghế (SeatMapBuilder) tự động cập nhật sức chứa phòng chiếu ngay khi thêm/sửa/xóa hoặc đổi cấu trúc ma trận hàng/cột.

### 7. Chuẩn Hóa Định Dạng Phim & Logic Phép Giao Suất Chiếu (Formats & Intersection Logic)
- **Quy chuẩn 6 Định Dạng Gốc:** Hệ thống cấu hình cố định 6 định dạng chuẩn hóa, loại bỏ hoàn toàn IMAX/ATMOS: `2D PHỤ ĐỀ`, `2D LỒNG TIẾNG`, `3D PHỤ ĐỀ`, `3D LỒNG TIẾNG`, `SUPERPLEX 2D`, `SUPERPLEX 3D`. Tính năng auto-migration tự động chuyển đổi IMAX cũ thành SUPERPLEX ở cấp độ Database (DataSeeder.java).
- **Phép giao Suất Chiếu (Intersection Filter):** Logic chọn định dạng ở Drawer Thêm/Sửa Suất Chiếu tự động lọc dựa trên hai màng lọc:
  - **Màng lọc Phim:** Chỉ các định dạng nằm trong cấu hình Phim đang chọn mới được phép hiển thị.
  - **Màng lọc Phòng:** Phụ thuộc vào Loại phòng: 
    - Phòng `STANDARD` & `CINE_COMFORT`: Chỉ lọc ra 4 chuẩn phổ thông (2D/3D Phụ đề, 2D/3D Lồng tiếng).
    - Phòng `SUPERPLEX`: Không bị giới hạn, hiển thị tất cả các chuẩn có trong màng lọc Phim.

### 8. Bảo toàn Dữ Liệu Sơ Đồ Ghế Hiện Hữu (Seat Map Preservation Rule)
- **Bảo vệ tùy biến sơ đồ:** Bổ sung cơ chế Blocking Logic ở luồng Seed/Upsert sơ đồ ghế của Backend (DataSeeder/RoomMigrationRunner).
- **Bỏ qua (SKIP):** Nếu truy vấn Database xác định phòng chiếu ĐÃ CÓ dữ liệu ghế, hệ thống sẽ tự động BỎ QUA việc tạo/upsert ghế mặc định. Tuyệt đối không reset, không xóa, không ghi đè sơ đồ ghế đã được Admin thiết lập/tùy biến.

### 9. Bộ Validate Chặt Chẽ Trong Quản Lý Phòng Chiếu (Strict Room Management Validation)
- **Kiểm soát Trạng thái Bảo trì (`MAINTENANCE` / `INACTIVE`):**
  - Chặn chuyển đổi trạng thái sang bảo trì nếu phòng đang có suất chiếu chưa kết thúc (kiểm tra bằng Query `end_time >= NOW()`). Backend Throw 400 Bad Request để khóa giao dịch.
  - Ngay cả khi tạo/sửa suất chiếu, Dropdown Chọn phòng ở Drawer cũng chỉ load những phòng đạt trạng thái `.filter(r => r.status === 'Active')`.
- **Khóa cứng (Disabled) Thuộc tính Kích thước & Loại phòng:** Hệ thống sẽ disabled và chặn API sửa Loại phòng, Số hàng và Số cột của phòng chiếu nếu phòng đó ĐÃ TỪNG có suất chiếu (Showtime > 0).
- **Smart Naming & UX Tối ưu:** 
  - Gợi ý tên phòng tự động theo định dạng: `Phòng {STT} - {Loại phòng}`. 
  - Bổ sung hệ thống Chip bấm nhanh `[ Phòng 01 ]`, `[ Phòng 02 ]` dưới input.
  - Xóa bỏ ô nhập tĩnh "Tổng số ghế" khỏi Modal vì sức chứa đã được quản lý linh hoạt, chuẩn xác bên Sơ đồ ghế.
- **Thời gian Dọn Phòng (`turnaround_time_mins`):** Ràng buộc validate giới hạn giá trị từ 10 - 60 phút (Mặc định 15 phút), được tích hợp tự động vào Backend để cộng thêm thời gian giãn cách giữa các suất chiếu.

### 10. Tạm Ẩn Giao Diện Phục Vụ Review Lượt 1 (Phase 1 Review Adjustments)
- **UI Tối giản:** Sử dụng inline-filter và comment-out để tạm ẩn các Component/Tính năng chưa cần thiết trên màn hình Chi tiết Cụm Rạp (Cinema Detail / Manager).
- Các thành phần bị ẩn: Khung thống kê 4 chỉ số (CinemaStatsBar) và 2 tab quản trị chuyên sâu (`Nhân sự`, `Phân tích`).
- **Quy tắc cô lập:** Tuyệt đối chỉ ẩn ở giao diện `<template>`, bảo tồn 100% logic bên dưới thẻ `<script>` để có thể khôi phục ngay lập tức sau đợt review.

### 11. Tối Ưu Hiệu Năng & Đồng Bộ State Quản Lý Phòng Chiếu
- **Tối ưu Batch Insert & Security Backend:**
  - Áp dụng `JdbcTemplate.batchUpdate` trong `SeatService.java` thay thế cho JPA `saveAll` để bypass giới hạn chiến lược `IDENTITY` của Hibernate. Giảm thời gian khởi tạo 160+ ghế từ ~3s xuống < 100ms. Kết hợp cấu hình `rewriteBatchedStatements=true` ở MySQL driver.
  - Bổ sung Unique Constraint vật lý tại Database qua annotation `@Table` (Entity `Room.java`) trên `cinema_id` và `name`. Bắt ngoại lệ `DataIntegrityViolationException` để trả về lỗi thân thiện HTTP 400 (Phòng chiếu này đang được tạo hoặc đã tồn tại).
- **Chuẩn Hóa Luồng Async & UX State Frontend:**
  - Khắc phục sự bất đồng bộ của sự kiện `emit` ở Vue 3 bằng cách truyền 2 callback `onSuccess` và `onError` vào Event Payload (`RoomFormModal.vue`). Nút [ ĐANG LƯU... ] giữ trạng thái loading cho đến khi `onSuccess` được kích hoạt (sau khi Toast xanh hiện ra và API hoàn thành thật sự), chống triệt để Race Condition.
  - Xử lý UX Popup Xác nhận xóa (`CinemaManager.vue`): Khai báo cờ `isDeletingRoom` để vô hiệu hóa double-click. Đảm bảo biến `roomToDelete = null` được đưa vào khối `finally` để Modal đen bắt buộc phải đóng khi hoàn thành giao dịch (dù lỗi hay không).

### 12. Thuật Toán Gợi Ý Tên Phòng Động (Dynamic Smart Chips)
- **Regex Bóc Tách STT Phòng:** Sử dụng Regex `/Phòng\s*(\d+)/i` hoặc `/\d+/` trên danh sách `room.name` hiện hữu để lọc ra số thứ tự lớn nhất.
- **Loop Until Unique:** Chạy vòng lặp `while` tịnh tiến `nextNum` và đối chiếu thực tế mảng tên phòng để ra con số tiếp theo chắc chắn không bị trùng.
- **Giao diện Dynamic Chips:** Các chip gợi ý đổi từ trạng thái hardcode tĩnh sang render động `[ Phòng 0X ]` kết hợp check trùng lặp Realtime (`isDuplicateName`) để ngăn form submit.

### 13. Chuẩn Hóa Kích Thước & Hiển Thị Ghế Đôi Sweetbox (Fixed-Track CSS Grid & Clean Layout)
- **Chuẩn hóa hiển thị CSS Grid (`SeatGridRenderer.vue`):** 
  - Thay thế `gridTemplateColumns: repeat(matrixCol, minmax(0, 1fr))` bằng kích thước track cột cố định theo size (`2.5rem` cho normal / `2rem` cho compact / `1.75rem` cho sm) kết hợp `justify-self-stretch` và `col-span-2 w-full`.
  - Đảm bảo ghế Sweetbox mở rộng chiếm trọn vẹn **đúng bằng độ dài 2 ghế thường + khoảng cách giữa 2 ghế** (`2 * trackWidth + gap`), căn thẳng hàng 100% với các cột ghế đơn từ hàng A đến hàng cuối.
  - Tinh chỉnh hàm `isOccupiedBySweetbox(r, c)` để nhận diện chính xác ô thứ 2 của ghế đôi và bỏ qua DOM element tương ứng, không gây cascade lỗi sang các ô kế tiếp.
- **Dọn dẹp Ghost Seat & Đồng bộ Composable (`useSeatLayout.js`):**
  - Tự động nhận diện ghế đôi (`SWEETBOX`, `DOUBLE`, `span === 2`) và xóa các ô bị ghế đôi đè lên ở cột liền kề (`r-(c+1)`) khi nạp từ Database/API để tránh lỗi chồng lấn/ghost seat.
  - Gửi kèm thuộc tính `span: 2` khi lưu sơ đồ phòng chiếu lên server.
- **Nghiệp vụ chọn vé & Sức chứa:** 
  - Ghế Sweetbox tính đúng sức chứa 2 chỗ, chọn 1 ghế Sweetbox tự động chọn cả cặp 2 vé và áp dụng đúng bảng giá.

### 14. Nâng Cấp Toàn Diện Quản Lý Khách Hàng (Customer Management Modernization)
- **Giao diện & Trải nghiệm bảng dữ liệu (`AdminCustomers.vue`):**
  - Phân loại rõ ràng Thành viên vs Khách vãng lai qua badges màu sắc nhận diện.
  - Tự động rút gọn email ảo của khách vãng lai thành dạng ngắn gọn, hiển thị tổng chi tiêu tích lũy và trạng thái tài khoản trực quan.
  - Bộ lọc đa chiều (Hạng thành viên, Trạng thái tài khoản, Loại khách hàng), phân trang động (10/20/50 dòng), tính năng xuất dữ liệu CSV chuẩn Excel UTF-8 BOM.
  - Thay thế toàn bộ native select bằng Custom Luxury Dropdown chuẩn hóa phong cách DevCine.
- **Hệ thống 3 Modal Thao Tác Chuyên Sâu:**
  - **Modal Chi tiết:** Thiết kế 3 tab nghiệp vụ (Lịch sử đơn vé, Kho Voucher đang sở hữu, Lịch sử biến động điểm tích lũy) kết hợp Thẻ thành viên VIP với thanh tiến trình nâng hạng trực quan.
  - **Modal Chỉnh sửa:** Cho phép cập nhật Họ tên & Ngày sinh có validate ngày hợp lệ.
  - **Modal Khóa/Mở khóa:** Khóa tài khoản kèm nhập lý do chi tiết, phục vụ kiểm soát vi phạm.
- **Tối ưu Backend & Bảo mật (`CustomerController.java`):**
  - Áp dụng Batch Aggregation Query gom nhóm tính tổng chi tiêu bằng 1 câu lệnh duy nhất, loại bỏ hoàn toàn lỗi N+1 Query.
  - Chặn sử dụng điểm tích lũy và chặn gửi email liên kết đặt lại mật khẩu cho các tài khoản đang ở trạng thái Khóa.

### 15. Phân Quyền Khách Hàng Theo Cụm Rạp (Cinema-Scoped Customer Access)
- **Kiểm soát truy cập theo phạm vi cơ sở:**
  - Với vai trò `MANAGER` và `STAFF`, hệ thống giới hạn phạm vi chỉ được xem danh sách, tìm kiếm, xem chi tiết, chỉnh sửa thông tin, khóa/mở khóa hoặc gửi email reset password cho những khách hàng đã từng phát sinh giao dịch (Booking trạng thái `CONFIRMED` hoặc ConcessionSale trạng thái `COMPLETED`) tại rạp của mình (`hasAccessToCustomer`).
  - Mọi hành vi truy cập hoặc thao tác chéo rạp đều bị chặn tức thì với mã phản hồi `HTTP 403 Forbidden`.
- **Tối ưu truy vấn JPA (`CustomerRepository.java`, `ConcessionSaleRepository.java`):**
  - Tận dụng `EXISTS` subquery trong `findByCinemaScope` và `searchByCinemaScope` để lọc chính xác và chống duplicate khi khách có nhiều đơn hàng tại rạp.
  - Các hàm kiểm tra nhanh `existsBookingByCinemaAndCustomer` và `existsCompletedByCinemaAndCustomer` đóng vai trò gác cổng hiệu năng cao cho các API thao tác chi tiết.

### 16. Tạm Ẩn Phân Hệ Chăm Sóc Khách Hàng Trên Menu Quản Trị (Customer Support Sidebar Visibility)
- **Tạm ẩn giao diện điều hướng (`AdminLayout.vue`):**
  - Tạm ẩn (comment-out) liên kết đến `/admin/customer-support` trên thanh sidebar quản trị theo yêu cầu tối giản luồng review.
  - Loại bỏ quyền `support` khỏi danh sách điều kiện hiển thị của nhóm chuyên mục *Kinh doanh & Khách hàng*.
  - Bảo tồn nguyên vẹn 100% component [CustomerSupport.vue](file:///e:/DATN/DevCine/devcine-frontend/src/views/admin/CustomerSupport.vue) và cấu hình route để có thể tái kích hoạt bất kỳ lúc nào.

### 17. Chuẩn Hóa Ma Trận Phân Quyền & Đồng Bộ Hệ Thống RBAC V8 (Permission Matrix Modernization V8)
- **Chuẩn hóa cấu trúc quyền theo vai trò (`DataSeeder.java` - PERMISSION_MATRIX_V8):**
  - **Quản trị viên (`ADMIN`):** Toàn quyền tuyệt đối trên mọi phân hệ hệ thống.
  - **Quản lý cụm rạp (`MANAGER`):** Phân quyền vận hành nghiêm ngặt trong phạm vi cơ sở được phân công quản lý:
    + Cụm rạp & Phòng chiếu (`cinemas:view,edit`): Xem và chỉnh sửa thông tin rạp, giờ mở/đóng cửa, hotline, tiện ích, cấu hình phòng chiếu và sơ đồ ghế.
    + Khách hàng (`customers:view,edit`): Xem hồ sơ, sửa thông tin, khóa tài khoản, gửi reset mật khẩu cho khách từng giao dịch tại rạp mình.
    + Lịch chiếu (`schedules:view,add,edit`): Điều phối lịch chiếu cho các phòng của rạp mình.
    + Nghiệp vụ & Vận hành (`pos_ticketing`, `bookings`, `incident_handling`, `dashboard_stats`, `staff_management`).
    + Đóng hoàn toàn các quyền cấu hình toàn cục: Phim, Banner, Thực đơn F&B, Bảng giá vé, Khuyến mãi, Cài đặt hệ thống, Nhật ký.
  - **Nhân viên quầy (`STAFF`):** Tinh gọn tối đa, CHỈ duy nhất 2 quyền nghiệp vụ quầy: `pos_ticketing:view,add` (Bán vé POS và Kiểm soát vé Check-in QR).
- **Backend & Controller Scoping:**
  - Mở quyền `@PreAuthorize("@perm.can('cinemas', 'edit')")` kết hợp `SecurityUtils.assertCinemaAccess` trong [CinemaController.java](file:///e:/DATN/DevCine/devcine-backend/src/main/java/com/devcine/backend/controller/CinemaController.java), [RoomController.java](file:///e:/DATN/DevCine/devcine-backend/src/main/java/com/devcine/backend/controller/RoomController.java), và [SeatController.java](file:///e:/DATN/DevCine/devcine-backend/src/main/java/com/devcine/backend/controller/SeatController.java).
- **Trải nghiệm Giao diện Phân quyền (`AdminPermissions.vue`):**
  - Tái cấu trúc 4 tab nghiệp vụ trực quan: *Vận hành & Quầy vé*, *Nội dung & Sản phẩm*, *Kinh doanh & Khách hàng*, *Hệ thống & Nhân sự*.
  - Tích hợp hệ thống Toast thông báo (`useToastStore` / [AppToast.vue](file:///e:/DATN/DevCine/devcine-frontend/src/components/common/AppToast.vue)) khi lưu thay đổi phân quyền cho Vai trò, Nhân viên hoặc Đặt lại quyền.

### 18. Chuẩn Hóa Nghiệp Vụ Quản Lý & Chỉnh Sửa Suất Chiếu (Showtimes Business Guard & UX Modernization)
- **Ràng buộc Nghiệp vụ Cốt lõi (Backend - `ShowtimeService.java`):**
  - **Chặn suất chiếu trong quá khứ / đang diễn ra:** Kiểm tra `showtime.startTime < NOW()`, ném `IllegalStateException` ("Không thể chỉnh sửa suất chiếu đã hoặc đang diễn ra.").
  - **Bảo vệ toàn vẹn vé đã bán/giữ chỗ (`reserved > 0`):** Nghiêm cấm thay đổi Phòng chiếu (`roomId`), Phim (`movieId`), Định dạng (`formatId`), và Giờ chiếu (`startTime`) khi suất chiếu đã phát sinh vé đặt.
  - **Cập nhật toàn diện khi chưa có vé bán (`reserved == 0`):** Cho phép cập nhật đầy đủ Phim mới, Định dạng mới, Phòng chiếu mới, Giờ chiếu mới; tự động tính lại `endTime` theo thời lượng phim mới + thời gian dọn phòng (`turnaroundOf`); tự động nhận diện xuất chiếu sớm (`isEarlyScreening`); tự động cập nhật snapshot sơ đồ ghế (`seatLayoutSnapshotService.buildSnapshotJson`) khi đổi phòng; kiểm tra tương thích định dạng phòng, chống trùng lịch và trần ca đêm (03:30).
- **Trải nghiệm Giao diện & Trực quan hóa (Frontend - `ShowtimeDrawer.vue`, `ShowtimeDetailsDrawer.vue`, `CinemaShowtimesTab.vue`):**
  - **Khóa tập trung từ màn hình Chi tiết (`ShowtimeDetailsDrawer.vue`):** Khi suất chiếu đã/đang chiếu hoặc đã có vé bán, ẩn hoàn toàn 2 nút `[ Sửa ]` và `[ Xóa ]`, thay bằng thẻ trạng thái duy nhất: `[ 🔒 KHÓA SỬA ĐỔI & XOÁ ]` kèm lý do chi tiết. Chỉ hiển thị nút Sửa/Xóa khi suất chiếu còn ở trạng thái `upcoming` và chưa phát sinh vé.
  - **Khắc phục lỗi mất dữ liệu khi Sửa suất chiếu (`ShowtimeDrawer.vue`):**
    + Sử dụng cờ `isInitializing` ngăn watchers xóa rỗng `form.formatId` khi mở form.
    + Nạp đồng bộ toàn bộ danh mục (`movies`, `formats`, `rooms`) trước khi bind dữ liệu vào form.
    + Giữ lại đầy đủ các khung giờ trong `hourOptions` và `minuteOptions` để dropdown luôn hiển thị chính xác giờ đã có của suất chiếu, đánh dấu `disabled` thay vì lọc bỏ khỏi mảng.
    + Sửa lỗi chuỗi ngày tháng bị trùng lặp năm (`selectedDateDisplay`).
  - **Tuân thủ quy chuẩn UI:** Loại bỏ hoàn toàn emoji trên nhãn giao diện, chuyển sang Material Symbols `<span class="material-symbols-outlined">lock</span>`.

### 19. Chuẩn Hóa Số Lượng Vé & Bộ Đếm Cho Ghế Đôi Sweetbox Tại Quầy POS (Sweetbox Capacity & Stepper Guard)
- **Sức chứa & Tự động gán 2 vé cho Sweetbox (`TicketingPOS.vue`):**
  - Đồng bộ logic sức chứa (`seatCapacity`): Ghế đơn (NORMAL, VIP) = 1 vé, Ghế đôi (SWEETBOX) = 2 vé.
  - Tổng số vé yêu cầu (`totalRequiredTickets`) tính theo tổng sức chứa của các ghế đã chọn.
  - Khi vào Bước 3 (Xác nhận vé & loại ghế), hệ thống tự động gán `NGƯỜI LỚN = totalRequiredTickets` (ví dụ chọn 1 ghế Sweetbox tự động là 2 vé Người lớn, trạng thái hiển thị: `Đã gán: 2/2 vé`).
# Project Memory Master - DevCine System

Tài liệu này lưu trữ và tổng hợp các cột mốc (milestone) đã hoàn thành, quyết định kiến trúc cốt lõi và các cập nhật quan trọng của dự án DevCine.

## Các Milestone Đã Hoàn Thành (Tháng 08/2026)

### 1. Realtime POS Showtime Filter (Lọc Suất Chiếu POS Thời Gian Thực)
- **Đồng bộ thông số:** Đồng bộ biến cấu hình lateSaleMinutes (Bán vé trễ sau giờ chiếu) từ Cài đặt hệ thống (settingsStore).
- **Cơ chế lọc:** Áp dụng thuật toán ẩn suất chiếu hết hạn tính theo từng phút cho tab "Hôm nay" (điều kiện currentMinutes > cutoffMinutes).
- **Tự động quét:** Khởi tạo tính năng rà soát ngầm thời gian thực (Auto-sweep timer), tự động dọn dẹp các suất chiếu đã quá hạn bán vé ngay trên màn hình POS mà không cần tải lại trang (F5).

### 2. Quy chuẩn hóa Loại Phòng Chiếu (Room Types Standardization)
- **Chuẩn hóa khái niệm:** Loại bỏ hoàn toàn 'Sweetbox' khỏi Enum và Dropdown loại phòng, đưa Sweetbox về đúng bản chất logic là "Loại Ghế" (Seat Type).
- **Quy chuẩn 1:1:** Đồng bộ Loại phòng chiếu khớp tuyệt đối với Ma trận Bảng Giá Vé ở Hệ thống: Chỉ còn đúng 3 loại phòng cơ bản [STANDARD, SUPERPLEX, CINE_COMFORT].

### 3. Migration Cụm Rạp & Sơ Đồ Ghế An Toàn (Safe Room & Seat Map Migration)
- **Tái chế thông minh:** Áp dụng thuật toán "Tái chế phòng tại chỗ" (Room Recycling) thay vì xóa-tạo mới. Giữ nguyên ID phòng cũ để bảo vệ tuyệt đối Khóa ngoại (Foreign Key), không làm đứt gãy liên kết của Suất chiếu & Vé đã bán trong quá khứ.
- **Tiêu chuẩn hóa phòng:** Đảm bảo tất cả các Cơ sở Rạp hiện tại đều sở hữu bộ 3 phòng chuẩn ở trạng thái ACTIVE:
  - Phòng 01: Standard (10x16)
  - Phòng 02: Superplex (10x16)
  - Phòng 03: Cine Comfort (8x10)
- **Re-seed dữ liệu:** Phân bổ, tính toán và thiết lập sơ đồ ghế tự động (Ghế Thường, VIP, Sweetbox) cho toàn bộ ~2000 ghế trên Database.

### 4. Khắc Phục Lỗi 500 Khi Lưu Sơ Đồ Ghế (UPSERT & Soft-Delete Pattern)
- **Ngăn chặn Xóa cứng:** Gỡ bỏ hoàn toàn các lệnh Hard Delete (deleteByRoomId, deleteAll) tiềm ẩn nguy cơ ở Runner Startup và các API Services (SeatService, RoomService).
- **Cơ chế An toàn:** Chuyển đổi hoàn toàn sang cơ chế UPSERT (Update-in-place) thao tác theo tọa độ (rowChar, colNum) và Soft Delete (isActive = false) cho các ghế thừa trong lưới mới. Giải quyết triệt để vấn đề rào cản Foreign Key bị PostgreSQL từ chối với bảng  ooking_seats.

### 5. Tối Ưu Giao Diện Admin Sơ Đồ Ghế (Seat Map UI Refactoring)
- **Đồng bộ hóa Visual:** Chuẩn hóa Icon công cụ "Lối đi" (Icon oute) và màu sắc (Cyan/Blue #38BDF8) đồng bộ 1:1 với bộ icon của các công cụ khác (Sofa, Kim cương, Trái tim, Cờ-lê, Cục tẩy).
- **Tối ưu Layout Không gian:** Tái cấu trúc gộp chung 2 Card "Cấu hình ma trận" và "Thông số phòng chiếu" thành card **CẤU HÌNH & THỐNG KÊ** đặt trên cùng Sidebar (SeatMapBuilder.vue). Các chỉ số ghế được thiết kế dưới dạng Badges dàn ngang gọn gàng, giúp Admin bao quát toàn bộ thông số theo Real-time mà không cần cuộn trang (No-scroll UX).

### 6. Quy chuẩn tính toán Sức chứa phòng chiếu (Capacity Rule)
- **Công thức tính Sức chứa thực tế:** Ghế Đơn (NORMAL, VIP) = 1 sức chứa; Ghế Đôi (DOUBLE, SWEETBOX, COUPLE) = 2 sức chứa; Lối đi/Ô trống = 0.
- **Áp dụng Real-time UI:** UI Sidebar trên trình chỉnh sửa sơ đồ ghế (SeatMapBuilder) tự động cập nhật sức chứa phòng chiếu ngay khi thêm/sửa/xóa hoặc đổi cấu trúc ma trận hàng/cột.

### 7. Chuẩn Hóa Định Dạng Phim & Logic Phép Giao Suất Chiếu (Formats & Intersection Logic)
- **Quy chuẩn 6 Định Dạng Gốc:** Hệ thống cấu hình cố định 6 định dạng chuẩn hóa, loại bỏ hoàn toàn IMAX/ATMOS: `2D PHỤ ĐỀ`, `2D LỒNG TIẾNG`, `3D PHỤ ĐỀ`, `3D LỒNG TIẾNG`, `SUPERPLEX 2D`, `SUPERPLEX 3D`. Tính năng auto-migration tự động chuyển đổi IMAX cũ thành SUPERPLEX ở cấp độ Database (DataSeeder.java).
- **Phép giao Suất Chiếu (Intersection Filter):** Logic chọn định dạng ở Drawer Thêm/Sửa Suất Chiếu tự động lọc dựa trên hai màng lọc:
  - **Màng lọc Phim:** Chỉ các định dạng nằm trong cấu hình Phim đang chọn mới được phép hiển thị.
  - **Màng lọc Phòng:** Phụ thuộc vào Loại phòng: 
    - Phòng `STANDARD` & `CINE_COMFORT`: Chỉ lọc ra 4 chuẩn phổ thông (2D/3D Phụ đề, 2D/3D Lồng tiếng).
    - Phòng `SUPERPLEX`: Không bị giới hạn, hiển thị tất cả các chuẩn có trong màng lọc Phim.

### 8. Bảo toàn Dữ Liệu Sơ Đồ Ghế Hiện Hữu (Seat Map Preservation Rule)
- **Bảo vệ tùy biến sơ đồ:** Bổ sung cơ chế Blocking Logic ở luồng Seed/Upsert sơ đồ ghế của Backend (DataSeeder/RoomMigrationRunner).
- **Bỏ qua (SKIP):** Nếu truy vấn Database xác định phòng chiếu ĐÃ CÓ dữ liệu ghế, hệ thống sẽ tự động BỎ QUA việc tạo/upsert ghế mặc định. Tuyệt đối không reset, không xóa, không ghi đè sơ đồ ghế đã được Admin thiết lập/tùy biến.

### 9. Bộ Validate Chặt Chẽ Trong Quản Lý Phòng Chiếu (Strict Room Management Validation)
- **Kiểm soát Trạng thái Bảo trì (`MAINTENANCE` / `INACTIVE`):**
  - Chặn chuyển đổi trạng thái sang bảo trì nếu phòng đang có suất chiếu chưa kết thúc (kiểm tra bằng Query `end_time >= NOW()`). Backend Throw 400 Bad Request để khóa giao dịch.
  - Ngay cả khi tạo/sửa suất chiếu, Dropdown Chọn phòng ở Drawer cũng chỉ load những phòng đạt trạng thái `.filter(r => r.status === 'Active')`.
- **Khóa cứng (Disabled) Thuộc tính Kích thước & Loại phòng:** Hệ thống sẽ disabled và chặn API sửa Loại phòng, Số hàng và Số cột của phòng chiếu nếu phòng đó ĐÃ TỪNG có suất chiếu (Showtime > 0).
- **Smart Naming & UX Tối ưu:** 
  - Gợi ý tên phòng tự động theo định dạng: `Phòng {STT} - {Loại phòng}`. 
  - Bổ sung hệ thống Chip bấm nhanh `[ Phòng 01 ]`, `[ Phòng 02 ]` dưới input.
  - Xóa bỏ ô nhập tĩnh "Tổng số ghế" khỏi Modal vì sức chứa đã được quản lý linh hoạt, chuẩn xác bên Sơ đồ ghế.
- **Thời gian Dọn Phòng (`turnaround_time_mins`):** Ràng buộc validate giới hạn giá trị từ 10 - 60 phút (Mặc định 15 phút), được tích hợp tự động vào Backend để cộng thêm thời gian giãn cách giữa các suất chiếu.

### 10. Tạm Ẩn Giao Diện Phục Vụ Review Lượt 1 (Phase 1 Review Adjustments)
- **UI Tối giản:** Sử dụng inline-filter và comment-out để tạm ẩn các Component/Tính năng chưa cần thiết trên màn hình Chi tiết Cụm Rạp (Cinema Detail / Manager).
- Các thành phần bị ẩn: Khung thống kê 4 chỉ số (CinemaStatsBar) và 2 tab quản trị chuyên sâu (`Nhân sự`, `Phân tích`).
- **Quy tắc cô lập:** Tuyệt đối chỉ ẩn ở giao diện `<template>`, bảo tồn 100% logic bên dưới thẻ `<script>` để có thể khôi phục ngay lập tức sau đợt review.

### 11. Tối Ưu Hiệu Năng & Đồng Bộ State Quản Lý Phòng Chiếu
- **Tối ưu Batch Insert & Security Backend:**
  - Áp dụng `JdbcTemplate.batchUpdate` trong `SeatService.java` thay thế cho JPA `saveAll` để bypass giới hạn chiến lược `IDENTITY` của Hibernate. Giảm thời gian khởi tạo 160+ ghế từ ~3s xuống < 100ms. Kết hợp cấu hình `rewriteBatchedStatements=true` ở MySQL driver.
  - Bổ sung Unique Constraint vật lý tại Database qua annotation `@Table` (Entity `Room.java`) trên `cinema_id` and `name`. Bắt ngoại lệ `DataIntegrityViolationException` để trả về lỗi thân thiện HTTP 400 (Phòng chiếu này đang được tạo hoặc đã tồn tại).
- **Chuẩn Hóa Luồng Async & UX State Frontend:**
  - Khắc phục sự bất đồng bộ của sự kiện `emit` ở Vue 3 bằng cách truyền 2 callback `onSuccess` và `onError` vào Event Payload (`RoomFormModal.vue`). Nút [ ĐANG LƯU... ] giữ trạng thái loading cho đến khi `onSuccess` được kích hoạt (sau khi Toast xanh hiện ra và API hoàn thành thật sự), chống triệt để Race Condition.
  - Xử lý UX Popup Xác nhận xóa (`CinemaManager.vue`): Khai báo cờ `isDeletingRoom` để vô hiệu hóa double-click. Đảm bảo biến `roomToDelete = null` được đưa vào khối `finally` để Modal đen bắt buộc phải đóng khi hoàn thành giao dịch (dù lỗi hay không).

### 12. Thuật Toán Gợi Ý Tên Phòng Động (Dynamic Smart Chips)
- **Regex Bóc Tách STT Phòng:** Sử dụng Regex `/Phòng\s*(\d+)/i` hoặc `/\d+/` trên danh sách `room.name` hiện hữu để lọc ra số thứ tự lớn nhất.
- **Loop Until Unique:** Chạy vòng lặp `while` tịnh tiến `nextNum` và đối chiếu thực tế mảng tên phòng để ra con số tiếp theo chắc chắn không bị trùng.
- **Giao diện Dynamic Chips:** Các chip gợi ý đổi từ trạng thái hardcode tĩnh sang render động `[ Phòng 0X ]` kết hợp check trùng lặp Realtime (`isDuplicateName`) để ngăn form submit.

### 13. Chuẩn Hóa Kích Thước & Hiển Thị Ghế Đôi Sweetbox (Fixed-Track CSS Grid & Clean Layout)
- **Chuẩn hóa hiển thị CSS Grid (`SeatGridRenderer.vue`):** 
  - Thay thế `gridTemplateColumns: repeat(matrixCol, minmax(0, 1fr))` bằng kích thước track cột cố định theo size (`2.5rem` cho normal / `2rem` cho compact / `1.75rem` cho sm) kết hợp `justify-self-stretch` và `col-span-2 w-full`.
  - Đảm bảo ghế Sweetbox mở rộng chiếm trọn vẹn **đúng bằng độ dài 2 ghế thường + khoảng cách giữa 2 ghế** (`2 * trackWidth + gap`), căn thẳng hàng 100% với các cột ghế đơn từ hàng A đến hàng cuối.
  - Tinh chỉnh hàm `isOccupiedBySweetbox(r, c)` để nhận diện chính xác ô thứ 2 của ghế đôi và bỏ qua DOM element tương ứng, không gây cascade lỗi sang các ô kế tiếp.
- **Dọn dẹp Ghost Seat & Đồng bộ Composable (`useSeatLayout.js`):**
  - Tự động nhận diện ghế đôi (`SWEETBOX`, `DOUBLE`, `span === 2`) và xóa các ô bị ghế đôi đè lên ở cột liền kề (`r-(c+1)`) khi nạp từ Database/API để tránh lỗi chồng lấn/ghost seat.
  - Gửi kèm thuộc tính `span: 2` khi lưu sơ đồ phòng chiếu lên server.
- **Nghiệp vụ chọn vé & Sức chứa:** 
  - Ghế Sweetbox tính đúng sức chứa 2 chỗ, chọn 1 ghế Sweetbox tự động chọn cả cặp 2 vé và áp dụng đúng bảng giá.

### 14. Nâng Cấp Toàn Diện Quản Lý Khách Hàng (Customer Management Modernization)
- **Giao diện & Trải nghiệm bảng dữ liệu (`AdminCustomers.vue`):**
  - Phân loại rõ ràng Thành viên vs Khách vãng lai qua badges màu sắc nhận diện.
  - Tự động rút gọn email ảo của khách vãng lai thành dạng ngắn gọn, hiển thị tổng chi tiêu tích lũy và trạng thái tài khoản trực quan.
  - Bộ lọc đa chiều (Hạng thành viên, Trạng thái tài khoản, Loại khách hàng), phân trang động (10/20/50 dòng), tính năng xuất dữ liệu CSV chuẩn Excel UTF-8 BOM.
  - Thay thế toàn bộ native select bằng Custom Luxury Dropdown chuẩn hóa phong cách DevCine.
- **Hệ thống 3 Modal Thao Tác Chuyên Sâu:**
  - **Modal Chi tiết:** Thiết kế 3 tab nghiệp vụ (Lịch sử đơn vé, Kho Voucher đang sở hữu, Lịch sử biến động điểm tích lũy) kết hợp Thẻ thành viên VIP với thanh tiến trình nâng hạng trực quan.
  - **Modal Chỉnh sửa:** Cho phép cập nhật Họ tên & Ngày sinh có validate ngày hợp lệ.
  - **Modal Khóa/Mở khóa:** Khóa tài khoản kèm nhập lý do chi tiết, phục vụ kiểm soát vi phạm.
- **Tối ưu Backend & Bảo mật (`CustomerController.java`):**
  - Áp dụng Batch Aggregation Query gom nhóm tính tổng chi tiêu bằng 1 câu lệnh duy nhất, loại bỏ hoàn toàn lỗi N+1 Query.
  - Chặn sử dụng điểm tích lũy và chặn gửi email liên kết đặt lại mật khẩu cho các tài khoản đang ở trạng thái Khóa.

### 15. Phân Quyền Khách Hàng Theo Cụm Rạp (Cinema-Scoped Customer Access)
- **Kiểm soát truy cập theo phạm vi cơ sở:**
  - Với vai trò `MANAGER` và `STAFF`, hệ thống giới hạn phạm vi chỉ được xem danh sách, tìm kiếm, xem chi tiết, chỉnh sửa thông tin, khóa/mở khóa hoặc gửi email reset password cho những khách hàng đã từng phát sinh giao dịch (Booking trạng thái `CONFIRMED` hoặc ConcessionSale trạng thái `COMPLETED`) tại rạp của mình (`hasAccessToCustomer`).
  - Mọi hành vi truy cập hoặc thao tác chéo rạp đều bị chặn tức thì với mã phản hồi `HTTP 403 Forbidden`.
- **Tối ưu truy vấn JPA (`CustomerRepository.java`, `ConcessionSaleRepository.java`):**
  - Tận dụng `EXISTS` subquery trong `findByCinemaScope` và `searchByCinemaScope` để lọc chính xác và chống duplicate khi khách có nhiều đơn hàng tại rạp.
  - Các hàm kiểm tra nhanh `existsBookingByCinemaAndCustomer` và `existsCompletedByCinemaAndCustomer` đóng vai trò gác cổng hiệu năng cao cho các API thao tác chi tiết.

### 16. Tạm Ẩn Phân Hệ Chăm Sóc Khách Hàng Trên Menu Quản Trị (Customer Support Sidebar Visibility)
- **Tạm ẩn giao diện điều hướng (`AdminLayout.vue`):**
  - Tạm ẩn (comment-out) liên kết đến `/admin/customer-support` trên thanh sidebar quản trị theo yêu cầu tối giản luồng review.
  - Loại bỏ quyền `support` khỏi danh sách điều kiện hiển thị của nhóm chuyên mục *Kinh doanh & Khách hàng*.
  - Bảo tồn nguyên vẹn 100% component [CustomerSupport.vue](file:///e:/DATN/DevCine/devcine-frontend/src/views/admin/CustomerSupport.vue) và cấu hình route để có thể tái kích hoạt bất kỳ lúc nào.

### 17. Chuẩn Hóa Ma Trận Phân Quyền & Đồng Bộ Hệ Thống RBAC V8 (Permission Matrix Modernization V8)
- **Chuẩn hóa cấu trúc quyền theo vai trò (`DataSeeder.java` - PERMISSION_MATRIX_V8):**
  - **Quản trị viên (`ADMIN`):** Toàn quyền tuyệt đối trên mọi phân hệ hệ thống.
  - **Quản lý cụm rạp (`MANAGER`):** Phân quyền vận hành nghiêm ngặt trong phạm vi cơ sở được phân công quản lý:
    + Cụm rạp & Phòng chiếu (`cinemas:view,edit`): Xem và chỉnh sửa thông tin rạp, giờ mở/đóng cửa, hotline, tiện ích, cấu hình phòng chiếu và sơ đồ ghế.
    + Khách hàng (`customers:view,edit`): Xem hồ sơ, sửa thông tin, khóa tài khoản, gửi reset mật khẩu cho khách từng giao dịch tại rạp mình.
    + Lịch chiếu (`schedules:view,add,edit`): Điều phối lịch chiếu cho các phòng của rạp mình.
    + Nghiệp vụ & Vận hành (`pos_ticketing`, `bookings`, `incident_handling`, `dashboard_stats`, `staff_management`).
    + Đóng hoàn toàn các quyền cấu hình toàn cục: Phim, Banner, Thực đơn F&B, Bảng giá vé, Khuyến mãi, Cài đặt hệ thống, Nhật ký.
  - **Nhân viên quầy (`STAFF`):** Tinh gọn tối đa, CHỈ duy nhất 2 quyền nghiệp vụ quầy: `pos_ticketing:view,add` (Bán vé POS và Kiểm soát vé Check-in QR).
- **Backend & Controller Scoping:**
  - Mở quyền `@PreAuthorize("@perm.can('cinemas', 'edit')")` kết hợp `SecurityUtils.assertCinemaAccess` trong [CinemaController.java](file:///e:/DATN/DevCine/devcine-backend/src/main/java/com/devcine/backend/controller/CinemaController.java), [RoomController.java](file:///e:/DATN/DevCine/devcine-backend/src/main/java/com/devcine/backend/controller/RoomController.java), và [SeatController.java](file:///e:/DATN/DevCine/devcine-backend/src/main/java/com/devcine/backend/controller/SeatController.java).
- **Trải nghiệm Giao diện Phân quyền (`AdminPermissions.vue`):**
  - Tái cấu trúc 4 tab nghiệp vụ trực quan: *Vận hành & Quầy vé*, *Nội dung & Sản phẩm*, *Kinh doanh & Khách hàng*, *Hệ thống & Nhân sự*.
  - Tích hợp hệ thống Toast thông báo (`useToastStore` / [AppToast.vue](file:///e:/DATN/DevCine/devcine-frontend/src/components/common/AppToast.vue)) khi lưu thay đổi phân quyền cho Vai trò, Nhân viên hoặc Đặt lại quyền.

### 18. Chuẩn Hóa Nghiệp Vụ Quản Lý & Chỉnh Sửa Suất Chiếu (Showtimes Business Guard & UX Modernization)
- **Ràng buộc Nghiệp vụ Cốt lõi (Backend - `ShowtimeService.java`):**
  - **Chặn suất chiếu trong quá khứ / đang diễn ra:** Kiểm tra `showtime.startTime < NOW()`, ném `IllegalStateException` ("Không thể chỉnh sửa suất chiếu đã hoặc đang diễn ra.").
  - **Bảo vệ toàn vẹn vé đã bán/giữ chỗ (`reserved > 0`):** Nghiêm cấm thay đổi Phòng chiếu (`roomId`), Phim (`movieId`), Định dạng (`formatId`), và Giờ chiếu (`startTime`) khi suất chiếu đã phát sinh vé đặt.
  - **Cập nhật toàn diện khi chưa có vé bán (`reserved == 0`):** Cho phép cập nhật đầy đủ Phim mới, Định dạng mới, Phòng chiếu mới, Giờ chiếu mới; tự động tính lại `endTime` theo thời lượng phim mới + thời gian dọn phòng (`turnaroundOf`); tự động nhận diện xuất chiếu sớm (`isEarlyScreening`); tự động cập nhật snapshot sơ đồ ghế (`seatLayoutSnapshotService.buildSnapshotJson`) khi đổi phòng; kiểm tra tương thích định dạng phòng, chống trùng lịch và trần ca đêm (03:30).
- **Trải nghiệm Giao diện & Trực quan hóa (Frontend - `ShowtimeDrawer.vue`, `ShowtimeDetailsDrawer.vue`, `CinemaShowtimesTab.vue`):**
  - **Khóa tập trung từ màn hình Chi tiết (`ShowtimeDetailsDrawer.vue`):** Khi suất chiếu đã/đang chiếu hoặc đã có vé bán, ẩn hoàn toàn 2 nút `[ Sửa ]` và `[ Xóa ]`, thay bằng thẻ trạng thái duy nhất: `[ 🔒 KHÓA SỬA ĐỔI & XOÁ ]` kèm lý do chi tiết. Chỉ hiển thị nút Sửa/Xóa khi suất chiếu còn ở trạng thái `upcoming` và chưa phát sinh vé.
  - **Khắc phục lỗi mất dữ liệu khi Sửa suất chiếu (`ShowtimeDrawer.vue`):**
    + Sử dụng cờ `isInitializing` ngăn watchers xóa rỗng `form.formatId` khi mở form.
    + Nạp đồng bộ toàn bộ danh mục (`movies`, `formats`, `rooms`) trước khi bind dữ liệu vào form.
    + Giữ lại đầy đủ các khung giờ trong `hourOptions` và `minuteOptions` để dropdown luôn hiển thị chính xác giờ đã có của suất chiếu, đánh dấu `disabled` thay vì lọc bỏ khỏi mảng.
    + Sửa lỗi chuỗi ngày tháng bị trùng lặp năm (`selectedDateDisplay`).
  - **Tuân thủ quy chuẩn UI:** Loại bỏ hoàn toàn emoji trên nhãn giao diện, chuyển sang Material Symbols `<span class="material-symbols-outlined">lock</span>`.

### 19. Chuẩn Hóa Số Lượng Vé & Bộ Đếm Cho Ghế Đôi Sweetbox Tại Quầy POS (Sweetbox Capacity & Stepper Guard)
- **Sức chứa & Tự động gán 2 vé cho Sweetbox (`TicketingPOS.vue`):**
  - Đồng bộ logic sức chứa (`seatCapacity`): Ghế đơn (NORMAL, VIP) = 1 vé, Ghế đôi (SWEETBOX) = 2 vé.
  - Tổng số vé yêu cầu (`totalRequiredTickets`) tính theo tổng sức chứa của các ghế đã chọn.
  - Khi vào Bước 3 (Xác nhận vé & loại ghế), hệ thống tự động gán `NGƯỜI LỚN = totalRequiredTickets` (ví dụ chọn 1 ghế Sweetbox tự động là 2 vé Người lớn, trạng thái hiển thị: `Đã gán: 2/2 vé`).
- **Khóa nút `[-]` khi số vé đạt trần yêu cầu (Stepper Guard):**
  - Vô hiệu hóa nút `[-]` (`disabled = true`) khi số lượng vé của loại đối tượng đó $\le 0$ HOẶC đang bằng đúng `totalRequiredTickets` (ví dụ `NGƯỜI LỚN = 2` trên tổng 2 vé của Sweetbox hoặc `NGƯỜI LỚN = 1` trên 1 vé của ghế đơn).
  - Ngăn chặn triệt để tình trạng nhân viên bấm giảm làm rơi vào trạng thái thiếu vé ($1/2$ vé hoặc $0/1$ vé).
- **Cơ chế chuyển đổi thông minh (Auto Balance / 1-Click Transfer):**
  - Chuyển đổi đối tượng bằng cách bấm `[+]` ở loại mong muốn (ví dụ `U22 / HSSV`), hệ thống tự động bớt 1 vé Người lớn và cộng 1 vé sang HSSV.
  - Khi đã phân bổ (`1 Người lớn + 1 HSSV`), bấm `[-]` ở HSSV sẽ tự động hoàn vé về lại cho Người lớn, bảo toàn tổng số vé luôn luôn bằng 100% số vé yêu cầu.
- **Tính đúng tổng tiền ghế & Đồng bộ Backend Payload:**
  - `priceOf(seat)` tính tổng giá cho cả 2 vé của ghế Sweetbox ($2 \times 105.000đ = 210.000đ$).
  - `buildSeatSelections()` sinh đủ 2 phần tử vé cho mỗi ghế Sweetbox trong payload gửi lên Backend (`BookingService.java`), giải quyết dứt điểm lỗi ngoại lệ thiếu vé khi tạo đơn và thanh toán.
  - Hóa đơn in nhiệt K80 (`printInvoice`) và Biên lai tạm tính hiển thị đầy đủ số ghế và số vé.

### 20. Tách Biệt Cấu Hình Thời Gian Giữ Ghế & Giữ Đơn Chờ POS (Dual Hold Timers)
- **Tách bạch 2 tham số cấu hình:**
  - `SEAT_HOLD_MINUTES` (3–30 phút, mặc định 10): Áp dụng cho giữ ghế phiên đặt vé trực tiếp (Online `BookingView.vue` và timer chọn ghế trên `TicketingPOS.vue`).
  - `POS_ORDER_HOLD_MINUTES` (3–60 phút, mặc định 15): Áp dụng khi thu ngân bấm "Giữ đơn" tại quầy POS (`PendingOrderService.java` và Pinia store `usePosStore.js`).
- Quản lý tập trung qua `SystemSettingService.java`, cấu hình tại `AdminSettings.vue`.

### 21. Bảo Toàn Snapshot Hóa Đơn & Đồng Bộ Hiển Thị Chi Tiết Vé (Invoice Snapshot Preservation & Synchronization)
- **Bảo toàn Snapshot Giá F&B (`AdminBookingController.java`):**
  - Loại bỏ hoàn toàn việc đọc `catalogPrice` live từ `fnbItem.getPrice()` khi xem chi tiết hoá đơn.
  - Thiết lập `finalUnitPrice = snapshot` và `basePrice = snapshot - totalSurcharge` cho cả đơn vé (`detail`) và đơn F&B lẻ (`getConcessionDetail`), bảo toàn 100% snapshot giá đã chốt tại thời điểm giao dịch.
- **Đồng bộ Tiền vé & Ghế (`BookingController.java` & `BookingHistoryView.vue`):**
  - Backend tính toán và trả về trường `seatTotal` (`SUM(BookingSeat.priceSnapshot)`).
  - Frontend `BookingHistoryView.vue` gán đúng `seatTotal` cho dòng *"Tiền vé & Ghế"* thay vì lấy nhầm `originalPrice` (tổng cả đơn).
- **Chuẩn hóa Hiển thị Loại ghế & Tùy chọn F&B (`BookingHistoryView.vue`):**
  - Chuẩn hóa nhãn loại ghế dạng `VIP - Người lớn x1` (`seatTypeLabel` + `ticketTypeLabel` + số lượng màu vàng).
  - Danh sách tùy chọn vị/nước F&B hiển thị dạng bullet point gọn gàng (`• Option (+phụ thu)`), loại bỏ tiền tố `Ô chọn...` đồng bộ 100% với giao diện Admin.

### 22. Tối Ưu Lịch Sử Đặt Vé Khách Hàng (Confirmed Bookings Filter)
- **Chỉ hiển thị đơn thành công cho khách hàng:** API `GET /api/customer/bookings/history` (`BookingController.java`) sử dụng `findConfirmedByCustomerIdWithDetails` chỉ lấy các đơn có trạng thái `CONFIRMED` hoặc `COMPLETED`. Loại bỏ hoàn toàn các đơn rác `HOLD`, `CANCELLED`, `EXPIRED` khỏi màn hình của khách (`BookingHistoryView.vue`).
- **Bảo toàn 100% dữ liệu đối soát:** Toàn bộ trạng thái đơn vẫn được lưu trữ đầy đủ trong Cơ sở dữ liệu và hiển thị trên màn hình Quản trị Admin (`AdminBookings.vue`) phục vụ đối soát tài chính, kế toán và xử lý sự cố.

### 23. Chuẩn Hóa & Nâng Cấp Giao Diện Chi Tiết Hóa Đơn Quản Trị (Admin Booking Modal Redesign & Status Decluttering)
- **Bố Cục 2 Cột Cân Bằng Thị Giác (Balanced 2-Column Master-Detail Layout):**
  - Tái cấu trúc Modal Chi tiết hoá đơn (`AdminBookings.vue`) thành 2 cột cân xứng 50/50:
    - **Cột Trái (Dịch vụ & Soát vé):** Thẻ Suất chiếu / Loại đơn $\rightarrow$ Danh sách Vé $\rightarrow$ Danh sách Bắp nước & Combo $\rightarrow$ Khung Mã QR Check-in (thiết kế dạng thanh ngang gọn gàng ở cuối cột).
    - **Cột Phải (Chủ thể & Tài chính):** Thẻ Khách hàng & Thu ngân / Cổng TT $\rightarrow$ Khối Tổng kết thanh toán (Tiền vé, Bắp nước, Giảm giá, **TỔNG TIỀN**, Phương thức, Trạng thái).
- **Tinh Gọn & Loại Bỏ Trùng Lặp Nhãn Trạng Thái (Status Deduplication):**
  - **Nguyên tắc "1 Nhiệm vụ - 1 Nhãn duy nhất":**
    - **Thanh Header:** Duy nhất 1 badge đại diện cốt lõi cho toàn bộ đơn hàng (`HOÀN TẤT`, `HẾT HẠN`, `ĐÃ HUỶ`, `ĐANG GIỮ`).
    - **Khung Mã QR:** Chỉ hiển thị badge soát vé (`ĐÃ CHECK-IN`, `QUÁ HẠN SUẤT CHIẾU`, `CHƯA CHECK-IN`) khi đơn là vé hợp lệ (`CONFIRMED`/`COMPLETED`). Đơn Hết hạn/Đã huỷ/Đang giữ không hiển thị thêm badge lặp lại trên tiêu đề QR, ảnh QR được làm mờ/grayscale đi kèm dòng chú thích tinh gọn (*"Mã QR vô hiệu hoá do đơn hàng đã hết hạn/bị huỷ"*).
    - **Khối Tổng kết thanh toán:** Rút gọn về 3 nhãn dòng tiền thuần túy: `ĐÃ THANH TOÁN` (Xanh lá), `CHỜ THANH TOÁN` (Vàng), `CHƯA THANH TOÁN` (Cam). Loại bỏ các hậu tố thừa thãi `(HẾT HẠN)`, `(ĐÃ HUỶ GIAO DỊCH)`.
- **Chuẩn Hóa Wording & Nhãn Giao Diện:**
  - Chuẩn hóa `"Tiền vé xem phim"` $\rightarrow$ `"Tiền vé"` (ngắn gọn, đối xứng hoàn hảo với *"Bắp nước & Combo"*).
  - Chuẩn hóa `"PT:"` $\rightarrow$ `"Phương thức:"` (viết đầy đủ, chuyên nghiệp, không viết tắt cụt lủn).
- **Bảo Vệ Đơn Hàng & Suất Chiếu Quá Hạn:**
  - Bổ sung tab lọc `Hết hạn` (`EXPIRED`) trên bảng quản trị hoá đơn.
  - Tự động nhận diện `isShowtimePast` để khóa/làm mờ mã QR khi suất chiếu đã kết thúc mà vé chưa được check-in.
  - Khóa nút In lại hoá đơn đối với các đơn chưa hoàn tất thanh toán (`status !== 'CONFIRMED' && status !== 'COMPLETED'`).
  - Khóa tính điểm thành viên (`detailRewardPoints = 0`) đối với đơn không thành công.

### 24. Chuẩn Hóa Thanh Lọc 1 Hàng & Quy Chuẩn Bo Góc Sắc Nét Admin (Compact Single-Row Toolbar & Rounded-SM Standard)
- **Chuẩn Hóa Bo Góc Toàn Diện (`rounded-sm` - 2px / 0.125rem):**
  - Đóng băng thông số bo góc tiêu chuẩn cho toàn bộ phân hệ Admin (khung toolbar, ô tìm kiếm, dropdowns, date picker, buttons) theo mẫu chuẩn của `MovieToolbar.vue` ("BỘ LỌC NÂNG CAO") với `rounded-sm` (2px).
  - Cấm sử dụng `rounded-xl` (12px) hoặc `rounded-2xl` (16px) cho các nút và ô nhập liệu thanh công cụ vì gây hiệu ứng bo tròn quá mức dạng viên thuốc/capsule, làm mất đi vẻ sang trọng, góc cạnh của giao diện rạp phim.
- **Tối Ưu Thanh Lọc 1 Hàng Duy Nhất Trên Laptop (`AdminBookings.vue`):**
  - Chuyển đổi toàn bộ bộ lọc thành Dropdown pills đồng bộ (`h-9` - 36px): Trạng thái (`min-w-[120px]`), Dịch vụ (`min-w-[105px]`), Phương thức (`min-w-[125px]`), Khoảng ngày compact.
  - Rút gọn ô tìm kiếm về kích thước vừa vặn `w-52` ~ `w-56` (208px - 224px).
  - Rút gọn nút Đặt lại về dạng icon vuông duy nhất `w-9 h-9` (`restart_alt`).
  - Tổng chiều ngang thanh lọc < 850px, đảm bảo luôn nằm trên 1 hàng duy nhất trên mọi độ phân giải laptop (1366px, 1440px) mà không bị rớt dòng.

### 25. Xử Lý Hiển Thị Vé Ghế Đôi Sweetbox & Đồng Bộ Hoá Đơn Toàn Diện (Sweetbox Capacity Invoice Display & Reprint Synchronization)
- **Bóc Tách Chuỗi Loại Vé Nhiều Phần Tử (`AdminBookings.vue` & `BookingHistoryView.vue`):**
  - Dưới cơ sở dữ liệu, mỗi ghế đôi Sweetbox được lưu thành 1 bản ghi `BookingSeat` với chuỗi `ticketType` ghép bởi dấu phẩy (ví dụ `"ADULT,ADULT"` hoặc `"ADULT,U22"`) và giá `priceSnapshot` là tổng của 2 vé (ví dụ 178.000đ).
  - Frontend `detailSeatGroups` (`AdminBookings.vue`) tự động phân tách chuỗi `s.ticketType.split(',')`, tính đơn giá vé lẻ `unit = price / types.length` và tăng biến đếm `count++` cho từng vé.
  - Nhãn hiển thị dòng vé Sweetbox được chuẩn hóa thành `Sweetbox - Người lớn x2: 178.000 đ` (hoặc tách riêng từng nhóm đối tượng nếu chọn nhiều loại vé khác nhau).
- **Chuẩn Hóa Tiêu Đề Tổng Số Vé:**
  - Tiêu đề nhóm vé chuyển đổi từ `detail.seats.length` (đếm ghế vật lý) sang `detailTotalTickets` (tổng số vé thực tế theo sức chứa), hiển thị chuẩn `Vé xem phim (3)` hoặc `Vé xem phim (N vé)`.
- **Đồng Bộ Dữ Liệu In Lại Vé K80 (`AdminBookings.vue` $\rightarrow$ `invoiceTemplate.js`):**
  - Hàm `buildInv` áp dụng cơ chế `flatMap` để giải nén ghế đôi Sweetbox thành các phần tử vé đơn lẻ, đảm bảo khi nhân viên bấm "In lại vé / Hoá đơn" từ Admin, phiếu in K80 sinh đúng đủ 3 vé (`3 VÉ: Người lớn 3 - 267.000 đ`) khớp 100% với mẫu in trực tiếp từ POS.
- **Nâng Cấp Xử Lý Chuỗi Loại Vé (`invoiceTemplate.js`):**
  - Hàm `ticketTypeLabel` hỗ trợ xử lý chuỗi phân tách bởi dấu phẩy, bảo toàn nhãn tiếng Việt của đối tượng vé.

### 26. Cố Định Bố Cục Bảng Quản Lý Nhân Viên & Đơn Giản Hóa Xác Thực Số Điện Thoại (Staff Table Standardization & Phone Validation Simplification)
- **Bố Cục Cố Định Bảng Nhân Viên (`StaffManager.vue`):**
  - Chuyển bảng sang `table-fixed` kết hợp phân bổ tỷ lệ độ rộng chuẩn: `Nhân viên: 26%`, `Mã NV: 11%`, `Cơ sở: 20%`, `Vai trò: 13%`, `Ngày gia nhập: 12%`, `Trạng thái: 11%`, `Thao tác: 7%`.
  - Thu ngắn khoảng cách dư thừa ở cột Nhân viên, đổi padding từ `px-8` xuống `px-6 py-4`, thêm `truncate` và `min-w-0` giúp bảng cân xứng, không bị dãn bất thường trên màn hình lớn.
- **Quy Chuẩn Xác Thực Số Điện Thoại Nhân Viên (`StaffManager.vue` & `StaffController.java`):**
  - Đơn giản hóa quy tắc kiểm tra số điện thoại: Chỉ yêu cầu đúng **10 chữ số** (`^\d{10}$`), loại bỏ kiểm tra bắt buộc đầu số nhà mạng (03/05/07/08/09) ở cả Frontend và Backend.
  - Thêm tự động lọc bỏ ký tự không phải số (`replace(/\D/g, '')`) và giới hạn độ dài `maxlength="10"` ngay khi nhập.

