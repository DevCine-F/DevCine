# TÀI LIỆU PHÂN TÍCH VÀ CẬP NHẬT BỘ NHỚ DỰ ÁN (PROJECT MEMORY & CONTEXT ANALYSIS)
**Module:** Quản lý Phim (Movies Management)
**Giai đoạn:** Hoàn thiện Nghiệp vụ lõi & UX/UI Guardrails
**Chuẩn bị cho Module:** Lịch chiếu (Showtimes) & Bán vé (Ticketing POS)

---

## 1. PHẠM VI DỮ LIỆU & BẢN CẬP NHẬT TRẠNG THÁI (Update Scope)
### Các tệp (Files) và Component đã được tinh chỉnh
- **`src/components/admin/movies/MovieFormModal.vue`**: Hoàn thiện Data-Binding 2 chiều, ép kiểu dữ liệu an toàn, xử lý logic đồng bộ Ngày Khởi Chiếu (`startDate`) và Trạng thái (`status`), thiết lập Proactive Disabling cho thẻ `<select>`.
- **`src/views/admin/AdminMovies.vue`**: Thiết lập bộ Guardrail 4 lớp chặt chẽ (Strict Guard Pattern) để giám sát và xử lý thao tác đổi trạng thái nhanh (Quick Status), cô lập luồng Popup Xác nhận Lưu trữ.
- **`src/components/admin/movies/MovieTable.vue`**: Tái cấu trúc Custom Dropdown để hỗ trợ trạng thái `:disabled` và `:title` (Tooltip) dựa trên quy tắc nghiệp vụ, chặn đứng các thao tác lỗi ngay từ UI.

### Trạng thái hệ thống (Build & Stability)
- **Độ ổn định:** Hoàn thiện 100% không phát sinh lỗi.
- **Trạng thái Build:** Quá trình `vite build` diễn ra hoàn hảo, không có cảnh báo Syntax hay Type Mismatch. Component đã được cô lập (Isolated), không gây ảnh hưởng đến các layout hay logic chung của toàn hệ thống.

---

## 2. PHÂN TÍCH NGUYÊN TẮC & MÃ LÕI CẦN LƯU BỘ NHỚ (Core Rules & Helpers to Retain)
Hệ thống hiện tại đã thiết lập các tiêu chuẩn (Convention) rất khắt khe. Các Module sau (đặc biệt là Showtimes) bắt buộc phải tái sử dụng và tuân thủ các quy tắc này:

### 2.1. Chuẩn hóa Data & Null-Safety (Data Sanitization)
- **Ngày tháng (Date):** Mọi so sánh ngày tháng luôn phải ép về **chuẩn `00:00:00` (Local Time)** của cả 2 vế (Ngày nhập vào và Ngày hiện tại) để loại bỏ sai số giờ phút khi đếm ngược hoặc xét điều kiện mở bán. Cắt chuỗi Date ISO 8601 bằng `.split("T")[0]` trước khi parse.
- **Mảng chuỗi (CSV):** Các text input như `supportedFormats`, `castMembers` phải được chuẩn hoá bằng chuỗi hàm chain: `.split(",").map(s => s.trim()).filter(Boolean)`.
- **An toàn Null (Null-Safety):** Bọc check biến tồn tại (`if (props.movieData)`) và xử lý giá trị mặc định cho Number/String trước khi render.

### 2.2. Logic Trạng thái & Đếm ngược ngày (Time-based State)
- Hàm `computeMovieStatus(startDate, endDate)`: 
  * `startDate <= Today` & `Today <= endDate` -> `"active"` (Đang chiếu).
  * `startDate > Today` -> `"upcoming"` (Sắp chiếu).
  * `endDate < Today` -> `"archived"` (Ngừng chiếu).
- Thuật toán `diffDays`: Xử lý triệt để 3 mốc (Tương lai > 0, Hiện tại = 0, Quá khứ < 0) đi kèm với Color Tone tương ứng (future, live, ended).

### 2.3. Bộ phòng thủ 4 lớp (Strict Guard Pattern)
Quy chuẩn viết hàm xử lý sự kiện mang tính phá hủy hoặc tác động diện rộng (như xoá, đổi trạng thái):
- **Lớp 0 (Khóa đúp):** Sử dụng cờ `isSubmittingStatus` để chặn thao tác spam click.
- **Lớp 1 & Lớp 2 (Early Return):** Bắt lỗi logic nghiệp vụ. Nếu vi phạm -> Bắn Toast Error -> `revertUI()` (hoàn nguyên giao diện) -> **`return;` NGẮT LUỒNG NGAY LẬP TỨC**.
- **Lớp 3 (Confirm Modal):** Dùng `await confirm.show()` chặn luồng chờ người dùng xác nhận các rủi ro cao. Nếu Cancel -> `return;`.
- **Lớp 4 (Try-Catch-Finally):** Đóng gói API call. Ủy quyền hiển thị Toast Success/Error cho Store hoặc Composable để tránh hiện tượng Dual-Toast (Toast đúp).

### 2.4. Proactive Disabling UI (Phòng ngừa lỗi chủ động)
- Các option không hợp lệ (Dựa vào `startDate`, `hasActiveShowtimes`) sẽ tự động được gán `:disabled="true"` kết hợp class Tailwind `disabled:opacity-50 disabled:cursor-not-allowed`.
- Luôn đính kèm `:title="Lý do"` để giải thích cho Admin tại sao chức năng bị khóa.

---

## 3. PHÂN TÍCH TƯƠNG QUAN VÀ LIÊN KẾT MODULE (Cross-Module Dependencies)
Bộ quy tắc của Module Phim vừa rồi tạo ra một bản lề rất chặt chẽ, ràng buộc trực tiếp đến kiến trúc của Module Lịch Chiếu (Showtimes) và Bán vé (Ticketing POS):

### A. Đối với Module Lịch Chiếu (Showtimes)
1. **Ràng buộc Tạo Lịch Chiếu:** 
   - Không được phép tạo Lịch chiếu mới cho các phim đang ở trạng thái `"archived"` (Lưu trữ).
   - Ngày chiếu của Suất chiếu (Showtime Date) bắt buộc phải nằm trong khoảng `startDate` và `endDate` của Phim.
2. **Quản lý cờ `hasActiveShowtimes`:** 
   - Khi Admin thêm suất chiếu đầu tiên cho một phim, hoặc xóa/hủy suất chiếu cuối cùng của phim đó, Module Showtimes phải cập nhật cờ `hasActiveShowtimes` một cách chuẩn xác. Sự chính xác của cờ này ảnh hưởng sống còn đến bộ Guardrail của Module Phim.

### B. Đối với Module Bán Vé POS (Ticketing POS)
1. **Hiển thị phim khả dụng:**
   - POS chỉ được hiển thị danh sách phim có trạng thái `"active"` (Đang chiếu) hoặc `"upcoming"` (Sắp chiếu - cho tính năng bán vé trước / pre-sale). Tuyệt đối ẩn phim `"archived"`.
2. **Đồng bộ Realtime:**
   - Nếu Admin đang mở POS để bán vé một bộ phim, nhưng ở cửa sổ khác, một Admin khác ép chuyển phim đó sang "LƯU TRỮ" (sau khi đã hủy sạch Lịch chiếu), Module POS cần cơ chế bắt lỗi hoặc ẩn các suất chiếu để ngăn nhân viên bán vé cho một bộ phim đã bị ngừng chiếu.
