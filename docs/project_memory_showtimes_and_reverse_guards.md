# Báo Cáo Cập Nhật Bộ Nhớ Dự Án: Module Quản Lý Lịch Chiếu & Phim (Reverse Guards & Timeline Engine)

> [!NOTE]
> Tài liệu này đóng vai trò như một mốc "chụp nhanh" (Snapshot) lưu trữ toàn bộ các quy tắc, quy ước (conventions), và kiến trúc dữ liệu đã được tinh chỉnh cho Hệ thống Lịch chiếu và Phim. Tài liệu phục vụ cho mục đích bảo toàn kiến trúc (Architecture Preservation) trước khi phát triển các module tiếp theo.

---

## 1. PHẠM VI DỮ LIỆU & BẢN CẬP NHẬT TRẠNG THÁI (Update Scope)

Danh sách các tập tin và Component cốt lõi đã được nâng cấp, tối ưu và bảo vệ trong đợt cập nhật này:

### 🎬 `MovieFormModal.vue`
- **Proactive Fetching Ngầm:** Tích hợp `watch(open)` để tự động gọi API `GET /showtimes/movie/{id}` mỗi khi Modal mở (trong chế độ Sửa). Không còn phụ thuộc vào dữ liệu sơ cấp truyền từ bảng.
- **Bộ Parser API Chính Xác:** Xử lý chính xác cấu trúc trả về của Backend (`CinemaShowtimeDTO`), đọc dữ liệu từ Map `showtimesByDate` thay vì mảng trực tiếp.
- **Proactive UI Lock:** Khóa cứng ô input `startDate` bằng bộ class `opacity-50 cursor-not-allowed bg-slate-800/50 pointer-events-none`. Thuộc tính `pointer-events-none` giải quyết dứt điểm hành vi "vượt rào" bung bảng lịch mặc định của trình duyệt. Dòng cảnh báo tinh tế xuất hiện trực quan.
- **Reverse Guardrail (Vòng trong):** Hàm Submit `handleSave` được bọc logic đối chiếu ngày khởi chiếu mới với Lịch chiếu sớm nhất (`earliest`) để ngăn chặn việc sửa ngày gây mâu thuẫn hệ thống.

### 📅 `CinemaShowtimesTab.vue` (Ma trận Timeline)
- **Tái Cấu Trúc Stacking Context (Z-Index):** Đảm bảo tính nhất quán ba chiều: Sidebar Cố định (`z-30`) đè lên Thẻ Phim (`z-20`), và Thẻ Phim đè lên Vạch Giờ Hiện Tại (`z-10`).
- **Làm Phẳng Bề Mặt (Flat UI):** Xóa bỏ các bóng mờ (shadow) và viền dư thừa, thiết lập ô nền đặc 100% (`bg-[#0b111e] h-full`) để chặn tuyệt đối hiện tượng "lọt khe / trôi chữ" khi cuộn Timeline.
- **Khóa Tương Tác Kéo-Thả (Drag & Drop Lock):** Áp dụng `:draggable="!isShowtimeLocked"` vào Thẻ Phim. Các suất chiếu đã bán vé / có người giữ ghế (`reserved > 0` hoặc `soldSeats > 0`) sẽ hiển thị biểu tượng 🔒 và không cho di chuyển.

### ⚙️ `ShowtimeDrawer.vue` & `BatchShowtimeDrawer.vue`
- **Bộ Lọc Phim Chuyên Sâu:** Tự động loại bỏ các Phim đang ở trạng thái Ngừng chiếu (`archived`).
- **Validate Ranh Giới (Boundary Guard):** Ràng buộc lịch chiếu được tạo mới không được vi phạm khung ngày `[startDate, endDate]` của Bộ Phim tương ứng.
- **Chuẩn Hóa Ngày Vận Hành (Operating Day vs Calendar Date):** Tích hợp hàm `getActualDateTimeStr()` tự động cộng 1 ngày cho các suất chiếu rạng sáng sau nửa đêm (khung giờ `00:00` đến `< openMin` của rạp). Điều này đảm bảo khi Backend lưu ngày thực tế `X+1` và Frontend nạp lại, hàm `mapShow` lùi 1 ngày về đúng ngày vận hành `X` của timeline, triệt tiêu hoàn toàn lỗi tạo suất chiếu bị nhảy lùi ngày.
- **Tiếp Nhận ISO Date Chuẩn (`selectedDateIso`):** Loại bỏ việc tự parse chuỗi `DD/MM` và ghép năm thủ công, xóa bỏ nguy cơ fallback sai về ngày hôm nay (`getLocalTodayStr`).

### 🔄 Event-Driven & Reactivity Sync
- **Giao Tiếp Bất Đồng Bộ:** Sử dụng `window.dispatchEvent(new Event('showtimes-updated'))` và `movies-updated` để tự động kích hoạt tiến trình Fetch ngầm ở các tab khác mà không tạo ra sự phụ thuộc cứng (tight-coupling) giữa các Store hay Component riêng lẻ.
- **Đồng Bộ Hai Chiều Ngày Đang Chọn:** Sử dụng `v-model:selected-date` xuyên suốt giữa `CinemaManager.vue`, `CinemaShowtimesTab.vue` và `ShowtimeDrawer.vue`.

---

## 2. ĐÚC KẾT CONVENTIONS & CẤU TRÚC DỮ LIỆU CẦN LƯU BỘ NHỚ

> [!IMPORTANT]
> Các quy ước dưới đây phải được tuân thủ nghiêm ngặt trong mọi phiên bản cập nhật tương lai hoặc khi có Developer mới tham gia bảo trì hệ thống.

1. **Backend Data Mapping Alignment (Nút Thắt API Lịch Chiếu)**
   - API `GET /api/showtimes/movie/{id}` không trả về danh sách lịch chiếu dưới biến `cinema.shows`.
   - **Cấu trúc đúng:** Đối tượng `CinemaShowtimeDTO` gom nhóm lịch chiếu theo Từ điển (Map). Cần dùng `Object.values(cinema.showtimesByDate)` để bóc tách mảng suất chiếu từng ngày. Tuyệt đối không dùng `Array.isArray(cinema.shows)` vì nó luôn `undefined`.

2. **Quy Ước Đồng Bộ Ngày Vận Hành (Business Operating Day Convention)**
   - Timeline ma trận của cụm rạp hiển thị theo **Ngày vận hành** (từ giờ mở cửa `openMin`, mặc định `08:00`, kéo dài xuyên đêm tới `03:30` rạng sáng hôm sau).
   - **Khi nạp dữ liệu (`mapShow` trong `useCinemas.js`):** Suất chiếu có `startTime < openMin` (ví dụ `00:30` ngày `01/09`) sẽ bị lùi hiển thị 1 ngày thành `31/08` để nằm ở đuôi ca đêm của ngày `31/08`.
   - **Khi tạo mới dữ liệu (`getActualDateTimeStr` trong `ShowtimeDrawer.vue`):** Nếu Admin đang ở tab ngày vận hành `31/08` và chọn giờ `< openMin` (ví dụ `00:30`), Calendar Date gửi lên Backend bắt buộc phải là ngày hôm sau `2026-09-01T00:30:00`. Tuyệt đối không gửi ngày `31/08` vì sẽ bị `mapShow` lùi về `30/08`.
   - **Định dạng dữ liệu ngày:** Luôn luân chuyển ngày dạng ISO `YYYY-MM-DD` (`selectedDateIso`, `fullDate`), không sử dụng chuỗi rút gọn `DD/MM` để tính toán thời gian.

3. **UI Proactive Prevention Convention (Phòng Ngừa Chủ Động Tầng Giao Diện)**
   - Triết lý UX: "Khóa trước khi người dùng kịp sai".
   - Bất cứ hành động nào có khả năng gây lỗi mâu thuẫn dữ liệu (Data Integrity Conflict) đều phải được **Vô Hiệu Hóa Tại Nơi Nhập Liệu** (Disable Input), kèm theo giải thích nguyên nhân rõ ràng trên giao diện (Tooltips, Helper Text), thay vì để người dùng điền đầy form rồi mới báo lỗi lúc Submit.

4. **Stacking Context & Rendering (Quy Tắc Lớp Z-Index)**
   - Khung giao diện phức tạp (Grid Matrix) phải quản lý chặt chẽ theo phân tầng `z-index`:
     - **Cấp độ cao nhất (`z-30`):** Tiêu đề Cột (Header) & Tên Phòng (Sticky Sidebar) - Không bao giờ bị trôi.
     - **Cấp độ nhì (`z-20`):** Các Thẻ Dữ Liệu nổi (Movie Cards / Showtimes).
     - **Cấp độ ba (`z-10`):** Các Công cụ Chỉ báo (Current Time Indicator, Gạch đứt).
     - **Cấp độ nền (`z-0`):** Khung lưới thời gian cơ bản (Grid Background).

5. **Event-Driven Cross-Module Sync (Kiến Trúc Kích Hoạt Tín Hiệu)**
   - Không được Import vòng chéo (Circular Imports) giữa các Composable để gọi hàm tải dữ liệu của nhau. Phải sử dụng bộ kích hoạt sự kiện ngầm của Window để duy trì tính cô lập tối đa (Decoupling).

---

## 3. TƯƠNG QUAN VÀ BẢO VỆ CHO MODULE BÁN VÉ POS SẮP TỚI

> [!TIP]
> Sự cẩn trọng hiện tại chính là tấm khiên bảo vệ cho Module cốt lõi: Bán Vé Tương Tác POS.

Những màng bảo vệ vừa thiết lập ở Module Quản Lý (Admin) có ý nghĩa sống còn khi chúng ta xây dựng Module Bán Vé POS:
- **Ngăn Chặn "Bóng Ma Suất Chiếu":** Khóa ngày `startDate` ngăn chặn nguy cơ Admin sửa lùi ngày phim trong lúc Khách hàng / Thu ngân đang chọn suất chiếu, loại trừ tình trạng "Suất chiếu tồn tại nhưng Phim thì chưa ra rạp".
- **Lưới Chắn `reserved > 0` Đảm Bảo Dữ Liệu Đồng Quy:** Thuật toán Kéo thả (Drag & Drop) khóa chặt các suất chiếu đã có `reserved > 0`. Nhờ đó, tại quầy POS, Thu Ngân sẽ không bao giờ gặp lỗi "Đang bán nửa chừng thì vé đổi giờ" hoặc Tình trạng Tranh chấp ghế ngồi do Admin lỡ tay dời suất chiếu. 
- **Cờ `hasActiveShowtimes` (Proactive Fetching):** Là nền tảng tin cậy cho Frontend đánh giá tự động "Phim nào có thể bán vé, Phim nào chưa", trực tiếp định hình logic hiển thị phim trên màn hình POS.

