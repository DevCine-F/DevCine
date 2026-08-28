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



