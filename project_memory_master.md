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
- **Đồng bộ hóa Visual:** Chuẩn hóa Icon công cụ "Lối đi" (Icon oute) và màu sắc (Cyan/Blue #38BDF8) đồng bộ 1:1 với bộ icon của các công cụ khác (Sofa, Kim cương, Trái tim, Cờ-lê, Cục tẩy).
- **Tối ưu Layout Không gian:** Tái cấu trúc gộp chung 2 Card "Cấu hình ma trận" và "Thông số phòng chiếu" thành card **CẤU HÌNH & THỐNG KÊ** đặt trên cùng Sidebar (SeatMapBuilder.vue). Các chỉ số ghế được thiết kế dưới dạng Badges dàn ngang gọn gàng, giúp Admin bao quát toàn bộ thông số theo Real-time mà không cần cuộn trang (No-scroll UX).
