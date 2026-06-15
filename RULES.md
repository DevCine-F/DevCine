# 🛡️ Project Rules — DevCine (Ultimate Edition)

> **⚠️ TÀI LIỆU QUY TẮC BẮT BUỘC (CRITICAL SYSTEM INSTRUCTIONS)**
> Các AI Agent (Anti IDE, Anti 2.0, Codex, Claude, v.v.) **PHẢI ĐỌC VÀ HIỂU KỸ 100%** văn bản này. Vi phạm bất kỳ điều nào dưới đây sẽ bị coi là phá hoại dự án.

---

## 🚫 PHẦN 1: CHẶN ĐỨNG LỖI THƯỜNG GẶP (AI ANTI-PATTERNS & STRICT PREVENTIONS)
*Các AI Agent thường xuyên bị "ảo giác" (hallucination) và tạo ra code rác. Hãy dùng checklist này như hệ tư tưởng để soi chiếu mọi dòng code.*

### ⚙️ 1.1 Ám Ảnh Lỗi N+1 (JPA/Hibernate) - KHÔNG ĐƯỢC PHÉP XẢY RA
*Lỗi N+1 làm sập server database do số lượng truy vấn tăng theo cấp số nhân. Bất kỳ query nào liên quan đến bảng con (ManyToOne, OneToMany) đều phải tuân thủ nghiêm ngặt:*
- **❌ TỬ HÌNH FetchType.EAGER:** Tuyệt đối không dùng `FetchType.EAGER`. BẮT BUỘC mọi relationship (`@ManyToOne`, `@OneToMany`) đều phải là `FetchType.LAZY`.
- **✅ Giải pháp bắt buộc:** Khi cần lấy dữ liệu quan hệ để render/trả về API, PHẢI dùng `@EntityGraph(attributePaths = {"field1", "field2"})` hoặc custom `@Query` với từ khóa `JOIN FETCH`.
- **❌ Cấm vòng lặp Query:** Tuyệt đối không gọi `repository.findById()` hay bất kỳ truy vấn DB nào bên trong một vòng lặp `for`/`while`. Phải gom ID lại và dùng `repository.findAllByIdIn(List<Long> ids)`.
- **⚠️ Cẩn thận Pagination + JOIN FETCH:** Lưu ý `JOIN FETCH` kèm phân trang (`Pageable`) trên collection (`@OneToMany`) sẽ báo lỗi lưu memory và vỡ phân trang. Phải dùng `@BatchSize` (hibernate) hoặc chia thành 2 queries độc lập để giải quyết.

### 🔌 1.2 Chuẩn Hóa API Format & Call API
- **❌ Không tự chế Format Trả Về:** API Response của Backend bắt buộc phải wrap trong một chuẩn chung duy nhất (Ví dụ lớp `ApiResponse<T>`):
  ```json
  {
    "status": 200,
    "message": "Thành công",
    "data": { ... },
    "timestamp": "2026-06-15T14:00:00.000Z"
  }
  ```
- **❌ Pagination Format Standard:** API Phân trang phải trả về đầy đủ metadata chuẩn:
  ```json
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
  ```
- **❌ Đừng ném Stack Trace ra ngoài:** Bất kỳ Exception nào (`500 Internal Server Error`, `400 Bad Request`) đều phải bị catch bởi `@ControllerAdvice` và trả về format lỗi ẩn giấu chi tiết hệ thống:
  ```json
  { "status": 500, "message": "Lỗi hệ thống nội bộ", "error_code": "INTERNAL_ERROR" }
  ```
- **❌ Call API vô tội vạ (Frontend):** Input Search BẮT BUỘC phải bọc bằng `Debounce` (300ms - 500ms). Gọi API liên tục theo mỗi phím gõ là không chấp nhận được. Tránh tuyệt đối gọi API trong quá trình component re-render vòng lặp.

### 🎨 1.3 Lỗi Giao Diện & UX (UI/Frontend)
- **❌ Tự ý sáng tạo UI/CSS:** TUYỆT ĐỐI KHÔNG bịa ra class Tailwind không tồn tại. Tuân thủ strict theo Design System. Không lạm dụng `z-[9999]`, `!important`.
- **❌ Phá vỡ Responsive:** Mọi giao diện thêm vào phải bao gồm hậu tố CSS cho mobile (`w-full`), tablet (`md:w-1/2`), và desktop (`lg:w-1/3`). Tuyệt đối không hardcode px làm vỡ layout điện thoại.
- **❌ Quên trạng thái (State Handling):** Phải xử lý triệt để 4 trạng thái trên màn hình:
  1. `Loading`: Hiển thị Skeleton hoặc Spinner khi call API.
  2. `Empty`: Hiển thị UI "Không có dữ liệu" kèm Icon/Image minh hoạ khi data rỗng.
  3. `Error`: Hiển thị Toast thông báo đỏ thay vì màn hình trắng vỡ nát.
  4. `Success`: Toast xanh báo hiệu thành công.
- **❌ Bỏ bom User:** Bất kỳ thao tác nguy hiểm nào (Xóa, Hủy đơn, Ban user) PHẢI có dialog/modal bắt User xác nhận (Confirm) trước khi thực thi.

### 🌐 1.4 Lỗi Code Frontend (Vue.js 3 - Composition API)
- **❌ Không dùng Options API:** Mọi component mới BẮT BUỘC dùng `<script setup>`.
- **❌ Prop Drilling:** Cấm truyền prop xuyên qua quá 2 cấp component trung gian. Nếu state phức tạp và được chia sẻ nhiều nơi, BẮT BUỘC sử dụng **Pinia Store**.
- **❌ Mutating Props:** Trực tiếp sửa đổi giá trị prop là phá vỡ dòng chảy dữ liệu Vue. Phải sử dụng `emit('update:propName', value)` hoặc `v-model` chuẩn.
- **❌ Memory Leaks:** BẮT BUỘC dọn dẹp các sự kiện lắng nghe (`window.addEventListener`) và timers (`setInterval`, `setTimeout`) bên trong lifecycle hook `onUnmounted`.

### 🛡️ 1.5 Lỗi Backend Logic & Bảo Mật (Java Spring Boot)
- **❌ Tầng Controller làm Logic:** Controller CHỈ làm nhiệm vụ nhận Request, gọi validation sơ bộ, chuyển giao cho Service và trả về `ResponseEntity`. Business Logic nằm 100% ở Service.
- **❌ Map Entity thẳng ra Response:** BẮT BUỘC phải dùng DTO (Data Transfer Object). Không được expose các trường nhạy cảm như `password`, `salt`, `createdAt` nội bộ cho Client.
- **❌ Quên Transactional:** Mọi API thay đổi dữ liệu (Cập nhật, Thêm, Xóa) phải gắn `@Transactional`. Các hàm truy vấn đọc dữ liệu dùng `@Transactional(readOnly = true)` để tối ưu performance Hibernate.
- **❌ Nuốt lỗi (Swallowing Exceptions):** Cấm bắt `catch (Exception e) {}` để rỗng. Cấm in `e.printStackTrace()` hoặc `System.out.println()`. PHẢI dùng `log.error("...", e)` và throw Exception cụ thể để ControllerAdvice tóm lấy.

### 📁 1.6 Tư duy Quản lý File, Đặt Tên & Tổ chức (SRP & Naming)
- **Tên Component (Vue):** Dùng `PascalCase.vue` (VD: `ShowtimeDetailsDrawer.vue`).
- **Tên Class/Interface (Java):** Dùng `PascalCase.java` (VD: `BookingService.java`).
- **Tên Biến/Hàm:** Dùng `camelCase` (VD: `fetchShowtimes`). Rõ nghĩa, **CẤM** viết tắt khó hiểu như `getUsrData()`, `sz`, `idx`, `lst` nếu không phải convention toàn cầu.
- **Tên Hằng số:** Dùng `UPPER_SNAKE_CASE` (VD: `MAX_RETRY_COUNT`).
- **Tên Event Handler (Vue):** Luôn bắt đầu bằng `handle` hoặc `on` (VD: `handleSubmit`, `onClick`).
- **Single Responsibility Principle (SRP):** File code không nên dài quá 300 dòng. Thấy file phình to hãy chủ động phân tích và đề xuất tách nhỏ (VD: Tách table logic, form logic ra component/composable riêng).
- **Bảo tồn Di sản (Legacy Code):** KHÔNG tự ý xóa comments/docs giải thích nghiệp vụ của Developer con người. Không refactor/đổi tên file cấu trúc nếu chưa được thảo luận và đồng ý.

---

## 🎯 PHẦN 2: NGUYÊN TẮC KỸ THUẬT VÀ DỰ ÁN TỐI THƯỢNG

### 2.1 Các Lệnh "KHÔNG BAO GIỜ" (Never Do List)
1. **KHÔNG BAO GIỜ** xoá hoặc viết lại code đang hoạt động (kể cả nó trông không đẹp) nếu không có yêu cầu cụ thể.
2. **KHÔNG BAO GIỜ** dùng raw SQL query (dùng `@Query(nativeQuery=true)` chỉ khi đó là đường cùng và phải giải thích rõ lý do).
3. **KHÔNG BAO GIỜ** bỏ qua validation (Jakarta `@Valid`, `@NotBlank`, `@Min`, `@Max`) tại Controller. Phải rào input từ ngoài cùng.
4. **KHÔNG BAO GIỜ** hardcode credentials (mật khẩu DB, secret key JWT, API key) vào code. Phải lấy qua biến môi trường hoặc `application.properties`.

### 2.2 Các Lệnh "BẮT BUỘC" (Must Do List)
1. **PHẢI** đọc các file tài liệu thư mục `docs/` (như `CRITICAL_PATHS.md`, `DATABASE.md`) trước khi sửa logic quan trọng (Booking, Payment).
2. **PHẢI** chạy build/test để đảm bảo không bị lỗi syntax/compile error trước khi báo "hoàn thành" cho User.
3. **PHẢI** dùng `Logger` chuẩn (`log.info`, `log.error`, `log.warn`) cho các luồng quan trọng (đăng nhập, lỗi thanh toán). Không dùng `console.log` trong code Production Frontend (trừ khi debug).

### 2.3 Cấu Trúc File/Layer Bất Khả Xâm Phạm (Protected Resources)
Các file/domain sau là **Trọng Tố Hệ Thống**, cần đặc biệt thận trọng và báo cáo rõ khi phải chạm vào:
```text
config/SecurityConfig.java     (Bảo mật, Filter JWT)
util/JwtUtil.java              (Tạo/Giải mã Token)
service/WalletService.java     (Tiền bạc, Trừ tiền)
service/BookingService.java    (Giao dịch mua vé)
service/PricingService.java    (Tính toán giá vé phức tạp)
pom.xml / package.json         (Tuyệt đối không tự ý thêm thư viện)
entity/*.java                  (Làm thay đổi Database Schema)
```

---

## 🤖 PHẦN 3: ĐẠO ĐỨC & CÁCH LÀM VIỆC CỦA AI AGENT

### 3.1 Không Ngụy Biện, Không Tự Đoán (Zero Hallucination)
- **Hành động mù quáng:** Không code khi thiếu Context. Nếu User yêu cầu sửa tính năng X mà bạn chưa biết nó đang gọi API nào, UI đang xây trên framework gì -> Dừng lại và yêu cầu cung cấp file.
- **Tự đoán Data:** Không đoán mò JSON Payload. Nếu cần biết Database có trường gì, hãy yêu cầu mở file Entity (Backend) tương ứng.

### 3.2 Tư duy Độc lập (Independent Critical Thinking)
- AI Agent KHÔNG PHẢI KẺ NỊNH BỢ. Hãy **MẠNH MẼ PHẢN BIỆN** nếu yêu cầu của User:
  - Có nguy cơ tạo bug N+1 query.
  - Phá hỏng kiến trúc layer Backend hiện tại.
  - Mang lại trải nghiệm UI giật lag.
  - Tạo lỗ hổng bảo mật nghiêm trọng (Insecure Direct Object Reference, SQL Injection).
- Câu cửa miệng khi phản biện: *"Yêu cầu này có thể làm ảnh hưởng tới [vấn đề ABC], tôi đề xuất cách làm [giải pháp XYZ] sẽ tối ưu và an toàn hơn. Bạn có muốn tôi tiến hành theo giải pháp đó không?"*

### 3.3 Tiêu chuẩn Báo Cáo Hoàn Thành (Definition of Done)
Khi AI Agent thông báo "Xong" hoặc "Hoàn thành", ngầm định hệ thống đã xác minh:
- [x] Logic code chính xác, giải quyết đúng yêu cầu.
- [x] Bắt đủ mọi Exception, hiển thị UX tử tế (Skeleton, Loading, Error Toast, Empty State).
- [x] KHÔNG CÓ LỖI N+1 QUERY.
- [x] Không để sót comment rác (console.log, TODO debug).
- [x] Code tuân thủ naming convention (PascalCase, camelCase).
- [x] Ứng dụng không bị lỗi biên dịch.
