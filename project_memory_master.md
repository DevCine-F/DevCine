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
