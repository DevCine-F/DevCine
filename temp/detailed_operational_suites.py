# -*- coding: utf-8 -*-
"""
Expanded Senior QA Test Suites for Operational Modules:
1. POS Đơn chờ (MOD_POS_PENDING) - 20 TCs
2. Soát vé & Check-in (MOD_STAFF_CHECKIN) - 20 TCs
3. Phê duyệt hủy đơn F&B (MOD_MGR_APPROVE_VOID) - 18 TCs
4. Xử lý sự cố & Đổi ghế (MOD_STAFF_INCIDENT_RELOCATE) - 20 TCs
5. Khóa bảo trì ghế vật lý (MOD_MGR_SEAT_MAINTENANCE) - 18 TCs
"""

# 1. POS ĐƠN CHỜ
tc_pos_pending = [
    ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
    ("PEN_GUI_01", "Kiểm tra chức năng hiển thị Thanh quản lý Tab Đơn chờ POS thành công",
     "Kiểm tra hiển thị các Tab đơn hàng đang giữ trên TicketingPOS.vue",
     "Bước 1: Nhân viên truy cập màn hình POS Bán vé\nBước 2: Quan sát thanh tab đơn hàng phía trên giỏ hàng\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị các Tab đơn hàng: 'Đơn 1 (Hiện tại)', nút '+ THÊM ĐƠN CHỜ' và đồng hồ đếm ngược của từng đơn"),

    ("PEN_GUI_02", "Kiểm tra chức năng hiển thị Nút Thêm đơn chờ và Badge số thứ tự đơn thành công",
     "Kiểm tra hiển thị nhãn nhận diện đơn 1, đơn 2, đơn 3",
     "Bước 1: Nhân viên mở màn hình POS\nBước 2: Quan sát thanh tab đơn hàng\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị nút '+ THÊM ĐƠN CHỜ' nổi bật, các tab được đánh số thứ tự rõ ràng (Đơn 1, Đơn 2, Đơn 3)"),

    ("PEN_GUI_03", "Kiểm tra chức năng hiển thị Đồng hồ đếm ngược giữ chỗ riêng từng đơn thành công",
     "Kiểm tra hiển thị countdown timer 10 phút trên từng tab",
     "Bước 1: Chọn ghế tại Đơn 1, sau đó mở Đơn 2 và chọn ghế\nBước 2: Quan sát đồng hồ đếm ngược của 2 tab\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Mỗi tab hiển thị đồng hồ đếm ngược độc lập giảm dần theo thời gian thực (ví dụ Đơn 1: 08:45, Đơn 2: 09:30)"),

    ("PEN_GUI_04", "Kiểm tra chức năng hiển thị Icon đóng tab [x] để hủy đơn chờ thành công",
     "Kiểm tra hiển thị nút xóa nhanh tab đơn hàng",
     "Bước 1: Mở Tab Đơn 2 trong hàng chờ\nBước 2: Quan sát góc phải của tab Đơn 2\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị icon [x] cho phép nhân viên chủ động hủy và đóng đơn chờ"),

    ("PEN_GUI_05", "Kiểm tra chức năng hiển thị Trạng thái Tab đang chọn (Active) thành công",
     "Kiểm tra làm nổi bật tab đơn hàng đang thao tác",
     "Bước 1: Click chọn Tab Đơn 2\nBước 2: Quan sát hiệu ứng màu sắc của các tab\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Tab Đơn 2 sáng màu nổi bật (Active), các tab khác mờ hơn ở trạng thái chờ (Inactive)"),

    ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
    ("PEN_EP_01", "Kiểm tra chức năng Tạo đơn chờ hợp lệ trong khoảng từ 1 đến 3 đơn thành công",
     "Kiểm tra chức năng Giữ nguyên giỏ hàng đơn 1 và mở đơn 2 phục vụ khách tiếp theo",
     "Bước 1: Khách A đang chọn 2 ghế VIP nhưng chưa thanh toán ngay\nBước 2: Nhân viên click button '+ THÊM ĐƠN CHỜ'\nBước 3: Kiểm tra màn hình POS",
     "Thao tác: Tạo Đơn 2", "Hệ thống lưu Đơn 1 vào hàng chờ (giữ ghế cho khách A), mở Tab Đơn 2 trống để nhân viên bán vé cho khách B ngay lập tức"),

    ("PEN_EP_02", "Kiểm tra chức năng Khởi tạo tab đơn hàng mới trống (0 ghế, 0 combo)",
     "Kiểm tra giỏ hàng của tab đơn mới tạo",
     "Bước 1: Click '+ THÊM ĐƠN CHỜ' mở Đơn 2\nBước 2: Kiểm tra giỏ hàng Đơn 2\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Trạng thái Đơn 2: Mới tạo", "Giỏ hàng Đơn 2 trống hoàn toàn (0 VNĐ), sơ đồ ghế sẵn sàng cho khách mới chọn"),

    ("PEN_EP_03", "Kiểm tra chức năng Tạo thêm đơn chờ thất bại khi vượt quá 3 đơn",
     "Kiểm tra chặn tạo quá 3 đơn chờ đồng thời tại 1 quầy POS (khoảng trên của [1,3])",
     "Bước 1: Quầy POS đã có sẵn 3 tab đơn chờ (Đơn 1, Đơn 2, Đơn 3)\nBước 2: Cố tình click nút '+ THÊM ĐƠN CHỜ' lần thứ 4\nBước 3: Kiểm tra phản hồi từ hệ thống",
     "Số đơn hiện có: 3 đơn chờ\nThao tác: Click thêm đơn thứ 4", "Nút thêm đơn chờ bị vô hiệu hóa, thông báo: 'Mỗi quầy chỉ được giữ tối đa 3 đơn chờ đồng thời'"),

    ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
    ("PEN_BVA_01", "Kiểm tra chức năng Quản lý đơn chờ ở mức biên min (1 đơn)",
     "Kiểm tra vận hành POS khi chỉ có duy nhất 1 đơn hàng hiện tại",
     "Bước 1: Nhân viên mở POS tại quầy vé\nBước 2: Kiểm tra thanh tab đơn hàng",
     "Số lượng đơn: 1 đơn", "Hiển thị duy nhất 'Đơn 1', hoạt động bán vé bình thường"),

    ("PEN_BVA_02", "Kiểm tra chức năng Quản lý đơn chờ ở mức cận biên trên min (2 đơn)",
     "Kiểm tra chuyển đổi qua lại mượt mà giữa 2 đơn chờ",
     "Bước 1: Có 2 tab Đơn 1 và Đơn 2\nBước 2: Click chuyển đổi qua lại nhiều lần giữa 2 tab\nBước 3: Kiểm tra giỏ hàng",
     "Số lượng đơn: 2 đơn", "Dữ liệu ghế và bắp nước của từng đơn được khôi phục nguyên vẹn 100% không bị lẫn lộn"),

    ("PEN_BVA_03", "Kiểm tra chức năng Quản lý đơn chờ ở mức biên max (3 đơn)",
     "Kiểm tra khi quầy POS đạt tải tối đa 3 đơn chờ đồng thời",
     "Bước 1: Tạo đủ 3 tab Đơn 1, Đơn 2, Đơn 3 đều đang giữ chỗ\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
     "Số lượng đơn: 3 đơn (Tối đa)", "Hệ thống quản lý tốt cả 3 đơn, đồng hồ đếm ngược từng đơn chạy chính xác"),

    ("PEN_BVA_04", "Kiểm tra chức năng Chặn mở đơn thứ 4 (cận biên trên max 4 đơn)",
     "Kiểm tra hệ thống từ chối tạo đơn thứ 4 khi đã có 3 đơn",
     "Bước 1: Đang có 3 tab đơn chờ\nBước 2: Quan sát nút '+ THÊM ĐƠN CHỜ'",
     "Số lượng đơn: 3 đơn", "Nút bị làm mờ (disabled), con trỏ chuột not-allowed"),

    ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
    ("PEN_ERR_01", "Kiểm tra chức năng Xác nhận trước khi đóng Tab Đơn chờ thành công",
     "Kiểm tra hiển thị popup cảnh báo khi nhân viên bấm icon [x] tại tab đang giữ ghế",
     "Bước 1: Đơn 2 đang chọn 3 ghế VIP\nBước 2: Nhân viên click icon [x] tại tab Đơn 2\nBước 3: Kiểm tra popup cảnh báo",
     "Thao tác: Click [x] đóng Đơn 2", "Hiển thị Modal cảnh báo: 'Bạn có chắc chắn muốn hủy và giải phóng ghế của đơn này không?'"),

    ("PEN_ERR_02", "Kiểm tra chức năng Hủy thao tác đóng tab đơn chờ thành công",
     "Kiểm tra khi nhân viên bấm 'Hủy bỏ' trên popup xác nhận đóng tab",
     "Bước 1: Mở popup xác nhận đóng tab Đơn 2\nBước 2: Click button 'Hủy bỏ'\nBước 3: Kiểm tra trạng thái Đơn 2",
     "Thao tác: Click Hủy bỏ", "Modal đóng lại, giữ nguyên vẹn Tab Đơn 2 và 3 ghế VIP đang chọn"),

    ("PEN_ERR_03", "Kiểm tra chức năng Tự động giải phóng ghế khi mất kết nối mạng bất ngờ",
     "Kiểm tra cơ chế dọn dẹp ghế giữ chỗ (Redis TTL) khi máy POS bị mất nguồn hoặc tắt trình duyệt",
     "Bước 1: POS đang giữ 3 đơn chờ, đột ngột tắt trình duyệt\nBước 2: Chờ hết 10 phút timeout\nBước 3: Kiểm tra trạng thái ghế trên hệ thống",
     "Trạng thái: Mất kết nối > 10 phút", "Hệ thống Redis tự động hết hạn TTL và nhả toàn bộ ghế của 3 đơn về trạng thái trống (Available)"),

    ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
    ("PEN_FUNC_01", "Kiểm tra chức năng Chuyển đổi qua lại giữa các Đơn chờ thành công",
     "Kiểm tra khôi phục nguyên vẹn giỏ hàng khi click chọn lại Tab đơn cũ",
     "Bước 1: Nhân viên click vào Tab 'Đơn 1'\nBước 2: Kiểm tra sơ đồ ghế và giỏ hàng\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Tab chọn: 'Đơn 1'", "Khôi phục đầy đủ 2 ghế VIP và combo bắp nước của khách A"),

    ("PEN_FUNC_02", "Kiểm tra chức năng Tự động đóng tab đơn khi hoàn tất thanh toán thành công",
     "Kiểm tra khi Đơn 2 thanh toán xong thì tab Đơn 2 tự động đóng",
     "Bước 1: Nhân viên thanh toán thành công cho Đơn 2\nBước 2: Quan sát thanh tab đơn hàng",
     "Trạng thái Đơn 2: Đã thanh toán", "Tab Đơn 2 tự động đóng lại, tiêu điểm chuyển về Đơn 1 để tiếp tục phục vụ"),

    ("PEN_FUNC_03", "Kiểm tra chức năng Tự động hủy đơn chờ khi hết thời gian giữ chỗ (Timeout 10 phút)",
     "Kiểm tra xử lý khi đơn chờ để quá 10 phút không thanh toán",
     "Bước 1: Đơn 2 để trong hàng chờ quá 10 phút\nBước 2: Đồng hồ đếm ngược của Đơn 2 về 00:00\nBước 3: Kiểm tra kết quả xử lý từ hệ thống",
     "Thời gian chờ: > 10 phút (Timeout 00:00)", "Hệ thống tự động đóng Tab Đơn 2, nhả toàn bộ ghế của Đơn 2 về trạng thái trống (Available)"),

    ("PEN_FUNC_04", "Kiểm tra chức năng Giữ các Suất chiếu khác nhau ở từng tab đơn độc lập",
     "Kiểm tra khả năng bán song song nhiều phim/suất chiếu khác nhau tại cùng 1 quầy POS",
     "Bước 1: Đơn 1 chọn suất Avatar lúc 19:00 (Phòng 1)\nBước 2: Đơn 2 chọn suất Dune lúc 20:30 (Phòng 3)\nBước 3: Chuyển qua lại giữa 2 tab",
     "Đơn 1: Avatar (P1)\nĐơn 2: Dune (P3)", "Mỗi tab tải đúng sơ đồ ghế và thông tin suất chiếu riêng biệt, không bị xung đột dữ liệu"),

    ("PEN_FUNC_05", "Kiểm tra chức năng Áp dụng Hội viên và Voucher độc lập cho từng tab đơn",
     "Kiểm tra thông tin khách hàng và mã giảm giá không bị ghi đè chéo",
     "Bước 1: Đơn 1 tra cứu khách VIP A và áp mã voucher 50k\nBước 2: Chuyển sang Đơn 2 tra cứu khách Standard B\nBước 3: Quay lại Đơn 1 kiểm tra",
     "Đơn 1: Khách A (Voucher 50k)\nĐơn 2: Khách B (Không voucher)", "Đơn 1 giữ nguyên thông tin khách A và voucher 50k, Đơn 2 giữ nguyên thông tin khách B")
]

# 2. SOÁT VÉ & CHECK-IN
tc_checkin = [
    ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
    ("CHK_GUI_01", "Kiểm tra chức năng hiển thị Màn hình Soát vé và Camera Scanner thành công",
     "Kiểm tra hiển thị giao diện Check-in trên StaffTicketCheckin.vue",
     "Bước 1: Nhân viên truy cập vào màn hình Soát vé\nBước 2: Quan sát khung Camera quét QR và ô nhập mã thủ công\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị khung Camera quét QR hoạt động mượt mà, Ô nhập Mã vé thủ công, Nút 'XÁC NHẬN CHECK-IN' và Lịch sử soát vé gần đây"),

    ("CHK_GUI_02", "Kiểm tra chức năng hiển thị 2 Tab 'Quét Camera' và 'Nhập mã thủ công' thành công",
     "Kiểm tra hiển thị các tab phương thức soát vé",
     "Bước 1: Nhân viên mở màn hình Soát vé\nBước 2: Quan sát thanh tab chuyển đổi\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị 2 tab: '📷 QUÉT MÃ QR CAMERA' và '⌨️ NHẬP MÃ THỦ CÔNG'"),

    ("CHK_GUI_03", "Kiểm tra chức năng hiển thị Khung kết quả tra cứu chi tiết vé thành công",
     "Kiểm tra hiển thị thông tin vé sau khi quét hợp lệ",
     "Bước 1: Quét mã vé thành công\nBước 2: Quan sát khung kết quả hiển thị trên màn hình\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị đầy đủ: Poster phim, Tên phim, Định dạng (2D/3D/IMAX), Phòng chiếu, Suất chiếu, Danh sách ghế, Combo F&B kèm theo"),

    ("CHK_GUI_04", "Kiểm tra chức năng hiển thị Nút In vé giấy và Badge trạng thái vé thành công",
     "Kiểm tra hiển thị nút in vé nhiệt",
     "Bước 1: Xác thực đơn vé thành công\nBước 2: Quan sát các nút tác vụ\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị nút '🖨️ IN VÉ GIAO KHÁCH' và badge trạng thái 'ĐÃ THANH TOÁN (PAID)' hoặc 'ĐÃ CHECK-IN'"),

    ("CHK_GUI_05", "Kiểm tra chức năng hiển thị Lịch sử soát vé gần đây thành công",
     "Kiểm tra hiển thị bảng nhật ký soát vé trong ngày tại rạp",
     "Bước 1: Quan sát khu vực phía dưới màn hình soát vé\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị danh sách 10 vé vừa soát gần nhất: Mã vé, Tên phim, Số ghế, Giờ check-in và Trạng thái"),

    ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
    ("CHK_EP_01", "Kiểm tra chức năng Nhập mã vé hợp lệ chuẩn 8 ký tự thành công",
     "Kiểm tra chức năng Check-in khi nhập mã vé có độ dài hợp lệ",
     "Bước 1: Chuyển sang tab 'Nhập mã thủ công'\nBước 2: Nhập mã vé 'DC889922' (8 ký tự)\nBước 3: Click button 'Tra cứu vé'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
     "Mã vé: 'DC889922'", "Hệ thống tra cứu thành công và hiển thị chi tiết đơn vé hợp lệ"),

    ("CHK_EP_02", "Kiểm tra chức năng Nhập mã vé thất bại khi độ dài quá ngắn",
     "Kiểm tra chức năng Check-in khi nhập mã vé < 4 ký tự (khoảng dưới của [4,30])",
     "Bước 1: Nhập mã vé 'DC1'\nBước 2: Click button 'Tra cứu vé'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Mã vé: 'DC1' (3 ký tự)", "Hiển thị thông báo lỗi: 'Mã vé không hợp lệ (tối thiểu 4 ký tự)'"),

    ("CHK_EP_03", "Kiểm tra chức năng Nhập mã vé thất bại khi độ dài quá dài",
     "Kiểm tra chức năng Check-in khi nhập mã vé > 30 ký tự (khoảng trên của [4,30])",
     "Bước 1: Nhập chuỗi mã vé dài 35 ký tự\nBước 2: Click button 'Tra cứu vé'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Mã vé: (Chuỗi 35 ký tự)", "Hiển thị thông báo lỗi: 'Mã vé không đúng định dạng'"),

    ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
    ("CHK_BVA_01", "Kiểm tra chức năng Nhập mã vé ở giá trị biên min (4 ký tự)",
     "Kiểm tra tra cứu mã vé ngắn nhất hợp lệ trong hệ thống",
     "Bước 1: Nhập mã vé 'DC01'\nBước 2: Click button 'Tra cứu vé'",
     "Mã vé: 'DC01' (4 ký tự)", "Hệ thống chấp nhận và thực hiện tra cứu"),

    ("CHK_BVA_02", "Kiểm tra chức năng Nhập mã vé ở giá trị biên max (30 ký tự)",
     "Kiểm tra tra cứu mã vé dài nhất hợp lệ (mã UUID/Hash)",
     "Bước 1: Nhập mã vé chuẩn 30 ký tự\nBước 2: Click button 'Tra cứu vé'",
     "Mã vé: (Chuỗi 30 ký tự)", "Hệ thống chấp nhận và thực hiện tra cứu"),

    ("CHK_BVA_03", "Kiểm tra chức năng Tra cứu khi để trống ô mã vé (0 ký tự)",
     "Kiểm tra nút Tra cứu khi chưa nhập dữ liệu",
     "Bước 1: Để trống ô mã vé (Null)\nBước 2: Click button 'Tra cứu vé'",
     "Mã vé: Null (0 ký tự)", "Hiển thị cảnh báo: 'Vui lòng nhập mã vé hoặc số điện thoại khách hàng'"),

    ("CHK_BVA_04", "Kiểm tra chức năng Soát vé sát giờ chiếu (Đúng giờ bắt đầu suất chiếu)",
     "Kiểm tra soát vé tại thời điểm suất chiếu vừa bắt đầu (t = 0 phút)",
     "Bước 1: Suất chiếu lúc 19:00, khách đưa vé quét lúc 19:00:15\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
     "Giờ suất chiếu: 19:00\nGiờ quét: 19:00", "Hệ thống xác thực hợp lệ, cho phép khách vào phòng chiếu bình thường"),

    ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
    ("CHK_ERR_01", "Kiểm tra chức năng Soát vé thất bại khi Vé đã qua sử dụng (Đã Check-in trước đó)",
     "Kiểm tra chức năng Chặn quét trùng vé (Prevent Double Check-in)",
     "Bước 1: Nhân viên quét mã QR của vé đã được check-in lúc 18:30\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
     "Mã vé: 'TK-ALREADY-USED'\nTrạng thái: CHECKED_IN", "Phát âm thanh cảnh báo BEEP lỗi (Sawtooth 220Hz), màn hình đổi màu đỏ cảnh báo: 'VÉ ĐÃ QUA SỬ DỤNG! Đã check-in lúc 18:30:15 bởi NV Khôi'"),

    ("CHK_ERR_02", "Kiểm tra chức năng Soát vé thất bại khi Mã vé không tồn tại trong hệ thống",
     "Kiểm tra chức năng Chặn mã QR giả mạo",
     "Bước 1: Quét mã QR lạ không do hệ thống DevCine phát hành\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
     "Mã vé: 'FAKE-QR-CODE-999'", "Phát âm thanh lỗi, màn hình cảnh báo đỏ: 'MÃ VÉ KHÔNG HỢP LỆ HOẶC KHÔNG TỒN TẠI!'"),

    ("CHK_ERR_03", "Kiểm tra chức năng Soát vé thất bại khi Quét vé sai Suất chiếu / Sai Rạp",
     "Kiểm tra chức năng Cảnh báo khi khách đến nhầm suất chiếu hoặc nhầm rạp",
     "Bước 1: Khách mua vé suất 21:00 nhưng đưa vé quét vào lúc 18:00\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Suất vé: 21:00\nGiờ hiện tại: 18:00", "Màn hình cảnh báo vàng: 'VÉ KHÔNG THUỘC SUẤT CHIẾU HIỆN TẠI! Suất chiếu của vé: 21:00'"),

    ("CHK_ERR_04", "Kiểm tra chức năng Xử lý khi trình duyệt bị từ chối quyền truy cập Camera",
     "Kiểm tra thông báo hướng dẫn khi người dùng chặn quyền Camera",
     "Bước 1: Mở màn hình Soát vé trên trình duyệt chưa cấp quyền Camera\nBước 2: Kiểm tra thông báo",
     "Trạng thái: Permission Denied", "Hiển thị thông báo: 'Không thể truy cập camera. Vui lòng cấp quyền hoặc nhập mã thủ công.'"),

    ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
    ("CHK_FUNC_01", "Kiểm tra chức năng Soát vé thành công (Quét mã QR hợp lệ)",
     "Kiểm tra luồng check-in thành công cho vé hợp lệ",
     "Bước 1: Khách hàng đưa mã QR trên điện thoại vào khung Camera\nBước 2: Camera nhận diện mã vé hợp lệ\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Mã vé: 'TK-20260319-VALID'\nTrạng thái cũ: PAID", "Phát âm thanh BEEP thành công (Sine 880Hz), màn hình sáng xanh, hiển thị: Tên phim, Phòng chiếu, Vị trí ghế (VIP F08) và chuyển trạng thái vé sang 'CHECKED_IN'"),

    ("CHK_FUNC_02", "Kiểm tra chức năng Soát vé bằng Mã nhập tay thành công khi màn hình khách bị vỡ",
     "Kiểm tra chức năng Check-in thủ công bằng chuỗi 8 ký tự",
     "Bước 1: Khách hàng đọc mã vé 'DC889922'\nBước 2: Nhân viên gõ 'DC889922' vào ô nhập và bấm 'Tra cứu vé'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Mã nhập tay: 'DC889922'", "Hệ thống xác thực thành công và hiển thị thông tin vé cho khách vào rạp"),

    ("CHK_FUNC_03", "Kiểm tra chức năng In vé giấy tại quầy Soát vé thành công",
     "Kiểm tra bấm nút In vé mở cửa sổ in vé nhiệt cho khách",
     "Bước 1: Quét mã đơn hợp lệ thành công\nBước 2: Nhân viên click button '🖨️ IN VÉ GIAO KHÁCH'\nBước 3: Kiểm tra cửa sổ in",
     "Mã đơn: 'DC889922'\nSố vé: 2 vé", "Hệ thống đánh dấu đơn đã in vé và mở popup in vé nhiệt chứa đầy đủ thông tin từng ghế"),

    ("CHK_FUNC_04", "Kiểm tra chức năng Hiển thị nhắc nhở Combo F&B đi kèm vé",
     "Kiểm tra thông báo nhắc nhân viên giao bắp nước cho khách khi vé có kèm combo",
     "Bước 1: Quét vé có đặt kèm '1 My Combo (Bắp phô mai + Coca)'\nBước 2: Quan sát màn hình check-in",
     "Combo kèm: 1 My Combo", "Hiển thị khung vàng nổi bật: 'ĐƠN HÀNG CÓ KÈM COMBO F&B: 1 Bắp phô mai + 1 Coca-Cola Lớn'"),

    ("CHK_FUNC_05", "Kiểm tra chức năng Tự động kích hoạt lại Camera để quét khách tiếp theo",
     "Kiểm tra camera tự động mở lại sau 3 giây để soát liên tục cho hàng dài khách",
     "Bước 1: Soát xong cho khách A\nBước 2: Chờ 3 giây hoặc click 'Quét tiếp'\nBước 3: Khách B đưa vé vào camera",
     "Thao tác: Quét liên tục", "Camera tự động kích hoạt nhận diện vé của khách B mượt mà không cần thao tác lại từ đầu")
]

# 3. PHÊ DUYỆT HỦY ĐƠN F&B
tc_void = [
    ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
    ("VOI_GUI_01", "Kiểm tra chức năng hiển thị 2 Tab 'Hàng chờ duyệt' và 'Yêu cầu của tôi' thành công",
     "Kiểm tra hiển thị giao diện Phê duyệt trên ApprovalQueue.vue",
     "Bước 1: Quản lý đăng nhập vào hệ thống DevCine\nBước 2: Mở màn hình Phê duyệt yêu cầu hủy (ApprovalQueue.vue)\nBước 3: Quan sát thanh tab\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị 2 tab: '⏳ HÀNG CHỜ DUYỆT (Pending)' và '📋 YÊU CẦU CỦA TÔI (Mine)' kèm số lượng badge"),

    ("VOI_GUI_02", "Kiểm tra chức năng hiển thị Bảng Danh sách Yêu cầu hủy đơn F&B thành công",
     "Kiểm tra hiển thị các cột thông tin trong bảng yêu cầu",
     "Bước 1: Quản lý mở tab 'Hàng chờ duyệt'\nBước 2: Quan sát bảng dữ liệu\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị đầy đủ các cột: Mã yêu cầu, Loại (Hủy hóa đơn F&B), Nhân viên gửi, Lý do hủy, Tổng tiền hoàn, Thời gian gửi và Nút thao tác"),

    ("VOI_GUI_03", "Kiểm tra chức năng hiển thị Badge Trạng thái yêu cầu nổi bật thành công",
     "Kiểm tra hiển thị màu sắc trạng thái PENDING / APPROVED / REJECTED",
     "Bước 1: Mở danh sách yêu cầu hủy\nBước 2: Quan sát cột Trạng thái\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị: 'PENDING' (Badge vàng/cam), 'APPROVED' (Badge xanh lá), 'REJECTED' (Badge đỏ)"),

    ("VOI_GUI_04", "Kiểm tra chức năng hiển thị 2 nút tác vụ '✓ PHÊ DUYỆT' và '✕ TỪ CHỐI' thành công",
     "Kiểm tra hiển thị các nút thao tác tại từng dòng yêu cầu",
     "Bước 1: Quan sát cột thao tác tại yêu cầu đang chờ duyệt\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị nút '✓ PHÊ DUYỆT' màu xanh và nút '✕ TỪ CHỐI' màu đỏ"),

    ("VOI_GUI_05", "Kiểm tra chức năng hiển thị Modal Nhập lý do từ chối thành công",
     "Kiểm tra popup nhập lý do khi Quản lý bấm Từ chối yêu cầu",
     "Bước 1: Quản lý click button '✕ TỪ CHỐI'\nBước 2: Quan sát popup hiển thị trên màn hình",
     "N/A", "Hiển thị Modal: Tiêu đề 'Từ chối yêu cầu hủy đơn', Ô nhập 'Lý do từ chối', Nút 'Xác nhận từ chối' và Nút 'Đóng'"),

    ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
    ("VOI_EP_01", "Kiểm tra chức năng Nhập lý do từ chối hợp lệ trong khoảng từ 5 đến 200 ký tự",
     "Kiểm tra chức năng từ chối khi nhập lý do đầy đủ",
     "Bước 1: Mở Modal từ chối yêu cầu FNB-001\nBước 2: Nhập lý do: 'Bắp nước đã làm xong và giao cho khách'\nBước 3: Click 'Xác nhận từ chối'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
     "Lý do: 'Bắp nước đã làm xong và giao cho khách' (39 ký tự)", "Hệ thống từ chối yêu cầu thành công, lưu lại lý do và cập nhật trạng thái REJECTED"),

    ("VOI_EP_02", "Kiểm tra chức năng Từ chối thất bại khi để trống lý do",
     "Kiểm tra bắt buộc nhập lý do khi từ chối yêu cầu (khoảng dưới của [5,200])",
     "Bước 1: Mở Modal từ chối\nBước 2: Để trống ô lý do (Null)\nBước 3: Click 'Xác nhận từ chối'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
     "Lý do: Null (0 ký tự)", "Nút xác nhận bị vô hiệu hóa hoặc báo lỗi: 'Vui lòng nhập lý do từ chối yêu cầu'"),

    ("VOI_EP_03", "Kiểm tra chức năng Nhập lý do từ chối vượt quá 200 ký tự thất bại",
     "Kiểm tra giới hạn ký tự tối đa ô lý do từ chối (khoảng trên của [5,200])",
     "Bước 1: Nhập chuỗi lý do dài 220 ký tự\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Lý do: (Chuỗi 220 ký tự)", "Hệ thống chặn không cho gõ quá 200 ký tự hoặc báo lỗi vượt quá độ dài"),

    ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
    ("VOI_BVA_01", "Kiểm tra chức năng Nhập lý do từ chối ở giá trị biên min (5 ký tự)",
     "Kiểm tra nhập lý do ngắn nhất hợp lệ",
     "Bước 1: Nhập lý do 'Da lam' (5 ký tự)\nBước 2: Click 'Xác nhận từ chối'",
     "Lý do: 'Da lam' (5 ký tự)", "Hệ thống chấp nhận lý do và thực hiện từ chối thành công"),

    ("VOI_BVA_02", "Kiểm tra chức năng Nhập lý do từ chối ở giá trị biên max (200 ký tự)",
     "Kiểm tra nhập lý do dài nhất hợp lệ",
     "Bước 1: Nhập chuỗi lý do đúng 200 ký tự\nBước 2: Click 'Xác nhận từ chối'",
     "Lý do: (Chuỗi 200 ký tự)", "Hệ thống chấp nhận lý do 200 ký tự hợp lệ"),

    ("VOI_BVA_03", "Kiểm tra chức năng Nhập lý do từ chối ở cận biên dưới min (4 ký tự) thất bại",
     "Kiểm tra chặn nhập lý do quá ngắn",
     "Bước 1: Nhập lý do 'Huy' (3 ký tự)\nBước 2: Click 'Xác nhận từ chối'",
     "Lý do: 'Huy' (3 ký tự)", "Hiển thị thông báo yêu cầu lý do tối thiểu 5 ký tự"),

    ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
    ("VOI_ERR_01", "Kiểm tra chức năng Chặn nhân viên STAFF truy cập màn hình Phê duyệt",
     "Kiểm tra phân quyền bảo vệ màn hình ApprovalQueue.vue (RBAC Guard)",
     "Bước 1: Đăng nhập bằng tài khoản nhân viên STAFF\nBước 2: Cố tình truy cập URL /admin/approvals\nBước 3: Kiểm tra phản hồi từ hệ thống",
     "Tài khoản: STAFF (Không có quyền duyệt)", "Hệ thống chặn truy cập và chuyển hướng về trang Thông báo lỗi 403 Forbidden"),

    ("VOI_ERR_02", "Kiểm tra chức năng Xử lý khi yêu cầu đã được Quản lý khác xử lý trước đó",
     "Kiểm tra xử lý xung đột (Race condition) khi 2 quản lý cùng mở và duyệt 1 yêu cầu",
     "Bước 1: Quản lý A và Quản lý B cùng thấy yêu cầu FNB-001\nBước 2: Quản lý A bấm duyệt thành công\nBước 3: Quản lý B bấm duyệt sau đó vài giây",
     "Trạng thái: Đã được Quản lý A duyệt", "Hệ thống thông báo: 'Yêu cầu này đã được xử lý trước đó', tự động cập nhật lại danh sách của Quản lý B"),

    ("VOI_ERR_03", "Kiểm tra chức năng Đóng Modal từ chối mà không xác nhận",
     "Kiểm tra khi Quản lý mở popup từ chối nhưng bấm nút Đóng [X]",
     "Bước 1: Mở Modal từ chối yêu cầu FNB-001\nBước 2: Click icon [X] hoặc click ra ngoài vùng tối\nBước 3: Kiểm tra danh sách",
     "Thao tác: Đóng Modal", "Modal đóng lại, yêu cầu FNB-001 giữ nguyên trạng thái PENDING trong hàng chờ"),

    ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
    ("VOI_FUNC_01", "Kiểm tra chức năng Quản lý Phê duyệt Hủy đơn F&B thành công",
     "Kiểm tra luồng phê duyệt hủy đơn và hoàn trả tiền cho khách hàng",
     "Bước 1: Quản lý click button '✓ PHÊ DUYỆT' tại yêu cầu hủy đơn FNB-001 (Lý do: Khách đổi ý)\nBước 2: Xác nhận trên hộp thoại confirm\nBước 3: Kiểm tra kết quả xử lý từ hệ thống",
     "Mã đơn: 'FNB-001'\nTổng tiền hoàn: 130.000 VNĐ", "Đơn hàng chuyển trạng thái 'VOID_APPROVED', hoàn tiền mặt tại két cho khách, ghi nhận log duyệt"),

    ("VOI_FUNC_02", "Kiểm tra chức năng Quản lý Từ chối Hủy đơn F&B kèm lý do thành công",
     "Kiểm tra chức năng Từ chối yêu cầu hủy khi món bắp nước đã được chế biến",
     "Bước 1: Quản lý click button '✕ TỪ CHỐI' tại yêu cầu FNB-002\nBước 2: Nhập lý do: 'Bắp phô mai đã chế biến và giao cho khách'\nBước 3: Click 'Xác nhận từ chối'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
     "Lý do: 'Bắp phô mai đã chế biến và giao cho khách'", "Yêu cầu chuyển trạng thái 'VOID_REJECTED', giữ nguyên doanh thu đơn hàng và thông báo cho nhân viên"),

    ("VOI_FUNC_03", "Kiểm tra chức năng Tự động xóa bản ghi khỏi Hàng chờ duyệt ngay tức thì (Optimistic UI Update)",
     "Kiểm tra bản ghi biến mất khỏi danh sách ngay sau khi Quản lý bấm Duyệt/Từ chối",
     "Bước 1: Hàng chờ đang có 3 yêu cầu\nBước 2: Quản lý duyệt 1 yêu cầu\nBước 3: Quan sát bảng hàng chờ",
     "Thao tác: Duyệt yêu cầu", "Bản ghi vừa duyệt biến mất ngay lập tức, số lượng badge giảm từ 3 xuống 2 mà không bị giật trang"),

    ("VOI_FUNC_04", "Kiểm tra chức năng Xem lịch sử các yêu cầu tại Tab 'Yêu cầu của tôi'",
     "Kiểm tra tra cứu các yêu cầu đã xử lý trong quá khứ",
     "Bước 1: Quản lý click chuyển sang tab '📋 YÊU CẦU CỦA TÔI'\nBước 2: Kiểm tra danh sách hiển thị",
     "Tab: Yêu cầu của tôi", "Hiển thị toàn bộ lịch sử các đơn F&B đã được duyệt hoặc từ chối kèm thời gian và ghi chú chi tiết")
]

# 4. XỬ LÝ SỰ CỐ & ĐỔI GHẾ
tc_incident = [
    ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
    ("INC_GUI_01", "Kiểm tra chức năng hiển thị 2 Tab 'Xử lý sự cố' và 'Lịch sử sự cố' thành công",
     "Kiểm tra hiển thị giao diện trên IncidentManagement.vue",
     "Bước 1: Nhân viên truy cập vào màn hình Xử lý sự cố chỗ ngồi (IncidentManagement.vue)\nBước 2: Quan sát thanh tab\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị 2 tab: '⚡ XỬ LÝ SỰ CỐ (Handle)' và '📜 LỊCH SỬ SỰ CỐ (History)'"),

    ("INC_GUI_02", "Kiểm tra chức năng hiển thị Khung Tra cứu vé sự cố thành công",
     "Kiểm tra hiển thị ô tìm kiếm mã đơn / SĐT khách hàng",
     "Bước 1: Nhân viên mở tab Xử lý sự cố\nBước 2: Quan sát ô tra cứu\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị Ô nhập 'Mã vé hoặc Số điện thoại khách hàng' kèm nút 'TRA CỨU VÉ'"),

    ("INC_GUI_03", "Kiểm tra chức năng hiển thị Khung Thông tin vé và Ghế khách đang sở hữu thành công",
     "Kiểm tra hiển thị thông tin suất chiếu và ghế sau khi tra cứu",
     "Bước 1: Nhập mã vé hợp lệ và bấm Tra cứu\nBước 2: Quan sát khung thông tin bên trái\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị: Tên phim, Suất chiếu, Phòng chiếu, Kênh đặt (POS/ONLINE), Danh sách ghế khách đang giữ kèm checkbox chọn ghế bị sự cố"),

    ("INC_GUI_04", "Kiểm tra chức năng hiển thị Sơ đồ ghế thực tế của suất chiếu thành công",
     "Kiểm tra hiển thị ma trận ghế trực quan của phòng chiếu",
     "Bước 1: Tra cứu đơn vé thành công\nBước 2: Quan sát sơ đồ ma trận ghế bên phải\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị sơ đồ ghế: Ghế của khách (màu xanh viền sáng), Ghế trống khả dụng (màu xám/vàng), Ghế đã bán khác (màu xám đậm)"),

    ("INC_GUI_05", "Kiểm tra chức năng hiển thị 2 Chế độ 'ĐỔI GHẾ' / 'HỦY CHỖ HOÀN TIỀN' và Chọn Đền bù thành công",
     "Kiểm tra hiển thị các công cụ xử lý sự cố",
     "Bước 1: Quan sát thanh công cụ phía dưới màn hình xử lý\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị 2 nút chuyển chế độ: '🔄 ĐỔI GHẾ KHÁC' và '🚫 HỦY CHỖ HOÀN TIỀN', Dropdown chọn 'Chính sách đền bù' (Voucher CSKH) và Nút 'XÁC NHẬN XỬ LÝ'"),

    ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
    ("INC_EP_01", "Kiểm tra chức năng Tra cứu vé sự cố bằng Số điện thoại thành công",
     "Kiểm tra chức năng tìm kiếm vé khi khách đọc số điện thoại",
     "Bước 1: Nhập SĐT '0901234567' vào ô tra cứu\nBước 2: Click button 'Tra cứu vé'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Tìm kiếm: '0901234567'", "Hệ thống tìm thấy đơn vé hợp lệ của khách và hiển thị sơ đồ phòng chiếu ngay lập tức"),

    ("INC_EP_02", "Kiểm tra chức năng Tra cứu vé sự cố thất bại khi Mã vé không tồn tại",
     "Kiểm tra xử lý khi nhập mã vé không có trong hệ thống",
     "Bước 1: Nhập mã vé 'NOT_FOUND_999'\nBước 2: Click button 'Tra cứu vé'\nBước 3: Kiểm tra phản hồi từ hệ thống",
     "Tìm kiếm: 'NOT_FOUND_999'", "Hiển thị thông báo lỗi: 'Không tìm thấy vé phù hợp'"),

    ("INC_EP_03", "Kiểm tra chức năng Nhập Ghi chú đền bù hợp lệ trong khoảng từ 5 đến 200 ký tự",
     "Kiểm tra nhập ghi chú giải trình lý do đền bù CSKH",
     "Bước 1: Chọn chính sách đền bù Voucher 50k\nBước 2: Nhập ghi chú: 'Khách bị đổ nước ngọt vào ghế do khách bên cạnh'\nBước 3: Click 'Xác nhận xử lý'",
     "Ghi chú: 'Khách bị đổ nước ngọt vào ghế do khách bên cạnh' (49 ký tự)", "Lưu thành công ghi chú sự cố và thực hiện đền bù cho khách"),

    ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
    ("INC_BVA_01", "Kiểm tra chức năng Đổi 1 ghế bị sự cố (Biên min 1 ghế)",
     "Kiểm tra luồng đổi 1 vị trí ghế đơn lẻ",
     "Bước 1: Chọn 1 ghế C05 bị gãy tay vịn\nBước 2: Click chọn ghế trống C08 trên sơ đồ\nBước 3: Click 'Xác nhận xử lý'",
     "Số lượng ghế đổi: 1 ghế (C05 -> C08)", "Đổi thành công 1 ghế và cập nhật vị trí mới cho khách"),

    ("INC_BVA_02", "Kiểm tra chức năng Đổi toàn bộ các ghế trong đơn (Đổi tối đa 8 ghế)",
     "Kiểm tra đổi toàn bộ cụm ghế khi phòng chiếu bị sự cố khu vực",
     "Bước 1: Đơn hàng có 4 ghế, chọn cả 4 ghế\nBước 2: Chọn 4 ghế trống mới trên hàng khác\nBước 3: Click 'Xác nhận xử lý'",
     "Số lượng ghế đổi: 4 ghế", "Đổi thành công toàn bộ 4 ghế sang vị trí mới liền nhau"),

    ("INC_BVA_03", "Kiểm tra chức năng Chưa chọn ghế đích mới mà bấm Xác nhận thất bại",
     "Kiểm tra nút xác nhận khi chưa gán ghế thay thế",
     "Bước 1: Tích chọn ghế sự cố C05 nhưng chưa click chọn ghế đích trên sơ đồ\nBước 2: Quan sát nút 'Xác nhận xử lý'",
     "Ghế đích: Chưa chọn", "Nút 'Xác nhận xử lý' bị vô hiệu hóa (disabled), không thể submit"),

    ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
    ("INC_ERR_01", "Kiểm tra chức năng Chặn chọn ghế đích trùng với ghế đã bán của người khác",
     "Kiểm tra không cho phép gán ghế sự cố vào vị trí đã có người ngồi",
     "Bước 1: Chọn ghế sự cố C05\nBước 2: Cố tình click vào ghế F08 (màu xám đậm - Đã bán)\nBước 3: Kiểm tra phản hồi từ hệ thống",
     "Ghế click: F08 (Đã bán)", "Không có phản hồi gán ghế, con trỏ chuột hiển thị 'not-allowed'"),

    ("INC_ERR_02", "Kiểm tra chức năng Chặn chọn ghế đích trùng với ghế đang bảo trì",
     "Kiểm tra không cho phép gán ghế sự cố vào vị trí đang bảo dưỡng",
     "Bước 1: Chọn ghế sự cố C05\nBước 2: Click vào ghế B03 (icon cờ lê - Bảo trì)\nBước 3: Kiểm tra phản hồi từ hệ thống",
     "Ghế click: B03 (MAINTENANCE)", "Hệ thống từ chối chọn và thông báo ghế đang trong quá trình bảo trì"),

    ("INC_ERR_03", "Kiểm tra chức năng Chặn đổi ghế khi suất chiếu đã bắt đầu quá thời gian quy định",
     "Kiểm tra ràng buộc thời gian khi suất chiếu đã chiếu xong",
     "Bước 1: Tra cứu đơn vé của suất chiếu đã chiếu xong 2 tiếng trước\nBước 2: Quan sát trạng thái xử lý",
     "Trạng thái suất: Đã kết thúc", "Hệ thống chặn đổi ghế và thông báo: 'Suất chiếu này đã kết thúc, không thể thực hiện đổi ghế'"),

    ("INC_ERR_04", "Kiểm tra chức năng Hủy bỏ thao tác chọn ghế đích (Reset gán ghế)",
     "Kiểm tra khi nhân viên click hủy gán ghế để chọn lại vị trí khác",
     "Bước 1: Đã gán C05 sang C08\nBước 2: Click nút 'Hủy gán' tại C08\nBước 3: Kiểm tra sơ đồ",
     "Thao tác: Hủy gán", "Ghế C08 trở về trạng thái trống, cho phép nhân viên chọn ghế đích khác"),

    ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
    ("INC_FUNC_01", "Kiểm tra chức năng Đổi ghế ngang hạng thành công (VIP sang VIP)",
     "Kiểm tra chức năng chuyển khách từ ghế VIP bị hỏng sang ghế VIP trống khác",
     "Bước 1: Ghế VIP C05 bị hỏng tay vịn trong phòng chiếu 1\nBước 2: Nhân viên chọn khách tại C05 và click chọn ghế VIP trống C08\nBước 3: Chọn lý do: 'Ghế hỏng cơ học'\nBước 4: Click button 'Xác nhận xử lý'\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
     "Ghế cũ: C05 (VIP)\nGhế mới: C08 (VIP)\nPhụ thu: 0đ", "Hệ thống giải phóng C05 chuyển sang MAINTENANCE, gán C08 cho khách, cập nhật lại mã vé và in lại phiếu đổi chỗ"),

    ("INC_FUNC_02", "Kiểm tra chức năng Nâng cấp ghế miễn phí (Upgrade Thường lên VIP) khi phòng hết ghế Thường",
     "Kiểm tra chính sách bù đắp dịch vụ CSKH: cho phép nâng hạng ghế miễn phí khi xảy ra sự cố",
     "Bước 1: Ghế Thường B03 bị ướt nước ngọt, hàng ghế Thường đã kín chỗ\nBước 2: Nhân viên chọn nâng khách lên ghế VIP D06 (Miễn phí phụ thu do sự cố)\nBước 3: Click 'Xác nhận xử lý'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
     "Ghế cũ: B03 (Thường)\nGhế mới: D06 (VIP)\nPhụ thu sự cố: 0 VNĐ", "Hệ thống cho phép nâng ghế VIP miễn phí, ghi nhận log sự cố và in phiếu chỗ ngồi mới cho khách"),

    ("INC_FUNC_03", "Kiểm tra chức năng Hủy chỗ và Hoàn tiền vé do sự cố bất khả kháng",
     "Kiểm tra luồng hủy vé và hoàn tiền khi phòng chiếu gặp sự cố nghiêm trọng (mất điện, hỏng máy chiếu)",
     "Bước 1: Chuyển sang chế độ '🚫 HỦY CHỖ HOÀN TIỀN'\nBước 2: Chọn toàn bộ ghế của đơn và nhập lý do: 'Phòng chiếu hỏng máy chiếu'\nBước 3: Click 'Xác nhận hủy chỗ'\nBước 4: Kiểm tra kết quả",
     "Chế độ: Hủy chỗ hoàn tiền\nTổng tiền hoàn: 100% giá vé", "Đơn vé chuyển trạng thái CANCELLED_INCIDENT, hoàn 100% tiền vé cho khách hàng và giải phóng ghế"),

    ("INC_FUNC_04", "Kiểm tra chức năng Tặng Voucher đền bù dịch vụ CSKH cho khách hàng",
     "Kiểm tra hệ thống tự động sinh và gửi Voucher đền bù vào tài khoản hội viên của khách",
     "Bước 1: Thực hiện đổi ghế do sự cố rạp\nBước 2: Tại mục Đền bù, chọn 'Voucher giảm 50K cho lần xem sau'\nBước 3: Xác nhận xử lý\nBước 4: Kiểm tra ví voucher của khách",
     "Chính sách đền bù: Voucher 50K", "Hệ thống tự động cộng mã voucher 50K vào ví tài khoản hội viên của khách hàng"),

    ("INC_FUNC_05", "Kiểm tra chức năng Tra cứu Lịch sử sự cố tại Tab History thành công",
     "Kiểm tra lọc lịch sử sự cố theo mã vé, loại sự cố và khoảng ngày",
     "Bước 1: Click chuyển sang tab '📜 LỊCH SỬ SỰ CỐ'\nBước 2: Lọc theo khoảng ngày hôm nay\nBước 3: Kiểm tra bảng dữ liệu",
     "Tab: Lịch sử sự cố", "Hiển thị đầy đủ nhật ký: Mã vé, Ghế cũ, Ghế mới, Nhân viên xử lý, Lý do và Voucher đền bù đã cấp")
]

# 5. KHÓA BẢO TRÌ GHẾ VẬT LÝ
tc_maint = [
    ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
    ("MNT_GUI_01", "Kiểm tra chức năng hiển thị Sơ đồ ma trận ghế kèm Icon bảo trì thành công",
     "Kiểm tra hiển thị trạng thái bảo dưỡng trên sơ đồ phòng chiếu",
     "Bước 1: Quản lý truy cập vào sơ đồ ghế phòng chiếu trên hệ thống\nBước 2: Quan sát các ghế đang trong trạng thái bảo trì\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị icon cờ lê / gạch chéo xám nổi bật trên các ghế đang bảo dưỡng kỹ thuật"),

    ("MNT_GUI_02", "Kiểm tra chức năng hiển thị Menu ngữ cảnh 'Khóa bảo trì' / 'Mở khóa' thành công",
     "Kiểm tra menu thao tác nhanh khi click chuột vào ghế",
     "Bước 1: Click chuột phải hoặc click chọn ghế E04\nBước 2: Quan sát menu ngữ cảnh hiển thị\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị menu chọn: '🔧 KHÓA BẢO TRÌ GHẾ' hoặc '✅ MỞ KHÓA HOẠT ĐỘNG'"),

    ("MNT_GUI_03", "Kiểm tra chức năng hiển thị Modal Khóa bảo trì ghế thành công",
     "Kiểm tra popup thiết lập bảo dưỡng ghế",
     "Bước 1: Click chọn 'Khóa bảo trì ghế'\nBước 2: Quan sát modal hiển thị trên màn hình\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị Modal: Vị trí ghế (E04 - Hàng E Cột 4), Dropdown 'Lý do bảo trì', Ô 'Ghi chú kỹ thuật', Nút 'Xác nhận khóa'"),

    ("MNT_GUI_04", "Kiểm tra chức năng hiển thị Dropdown Danh mục Lý do bảo trì thành công",
     "Kiểm tra hiển thị các nguyên nhân hỏng hóc kỹ thuật",
     "Bước 1: Mở dropdown Lý do bảo trì trên Modal\nBước 2: Kiểm tra danh sách lựa chọn",
     "N/A", "Hiển thị: 'Hỏng cơ học (Gãy chân/tay vịn)', 'Rách đệm/Bẩn bề mặt', 'Lỗi cụm điện tử/Massage', 'Bảo dưỡng định kỳ'"),

    ("MNT_GUI_05", "Kiểm tra chức năng hiển thị Chú thích màu sắc (Legend) trạng thái Bảo trì thành công",
     "Kiểm tra hiển thị mục chú giải màu ghế",
     "Bước 1: Quan sát thanh chú thích phía dưới sơ đồ ghế\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị rõ ràng ô chú thích: '🔧 GHẾ BẢO TRÌ (Không thể đặt)' kèm màu xám đặc trưng"),

    ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
    ("MNT_EP_01", "Kiểm tra chức năng Khóa bảo trì khi chọn lý do và nhập ghi chú hợp lệ [5, 200] ký tự",
     "Kiểm tra thiết lập bảo trì ghế đầy đủ thông tin",
     "Bước 1: Chọn ghế E04 phòng 2\nBước 2: Chọn lý do: 'Hỏng cơ học (Gãy chân/tay vịn)'\nBước 3: Nhập ghi chú: 'Gãy khớp ngả lưng bên trái, chờ thợ sửa'\nBước 4: Click 'Xác nhận khóa'\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
     "Vị trí: E04\nLý do: Hỏng cơ học\nGhi chú: 'Gãy khớp ngả lưng bên trái'", "Ghế E04 chuyển sang trạng thái MAINTENANCE thành công, lưu vết lý do bảo dưỡng"),

    ("MNT_EP_02", "Kiểm tra chức năng Khóa bảo trì nhanh khi để trống ghi chú kỹ thuật",
     "Kiểm tra cho phép khóa nhanh chỉ bằng việc chọn lý do trong dropdown",
     "Bước 1: Chọn ghế E05, chọn lý do 'Rách đệm/Bẩn bề mặt'\nBước 2: Để trống ô ghi chú kỹ thuật\nBước 3: Click 'Xác nhận khóa'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
     "Vị trí: E05\nGhi chú: Null", "Ghế E05 chuyển sang MAINTENANCE hợp lệ với lý do mặc định 'Rách đệm/Bẩn bề mặt'"),

    ("MNT_EP_03", "Kiểm tra chức năng Nhập ghi chú kỹ thuật vượt quá 200 ký tự thất bại",
     "Kiểm tra giới hạn ký tự tối đa ô ghi chú bảo trì",
     "Bước 1: Nhập chuỗi ghi chú dài 220 ký tự\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Ghi chú: (Chuỗi 220 ký tự)", "Hệ thống chặn không cho nhập quá 200 ký tự hoặc cảnh báo vượt quá độ dài"),

    ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
    ("MNT_BVA_01", "Kiểm tra chức năng Khóa bảo trì 1 ghế đơn lẻ (Biên min 1 ghế)",
     "Kiểm tra khóa 1 ghế Thường hoặc 1 ghế VIP",
     "Bước 1: Click chọn duy nhất ghế E04\nBước 2: Xác nhận khóa bảo trì",
     "Số lượng: 1 ghế", "Ghế E04 chuyển sang MAINTENANCE"),

    ("MNT_BVA_02", "Kiểm tra chức năng Khóa bảo trì cụm ghế đôi Sweetbox (2 ghế liền nhau)",
     "Kiểm tra khi khóa 1 vị trí Sweetbox thì tự động khóa cả cặp ghế đôi",
     "Bước 1: Click vào ghế Sweetbox H01\nBước 2: Xác nhận khóa bảo trì\nBước 3: Quan sát cụm ghế",
     "Loại ghế: SWEETBOX (H01-H02)", "Hệ thống tự động khóa đồng thời cả 2 vị trí H01 và H02 của cụm ghế đôi Sweetbox"),

    ("MNT_BVA_03", "Kiểm tra chức năng Khóa bảo trì toàn bộ hàng ghế (Khóa hàng loạt)",
     "Kiểm tra tính năng khóa cả hàng ghế khi sửa chữa dãy",
     "Bước 1: Chọn công cụ 'Khóa cả hàng'\nBước 2: Click vào hàng D\nBước 3: Xác nhận khóa",
     "Phạm vi: Toàn bộ hàng D (12 ghế)", "Tất cả 12 ghế từ D01 đến D12 chuyển sang trạng thái MAINTENANCE đồng loạt"),

    ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
    ("MNT_ERR_01", "Kiểm tra chức năng Chặn khóa bảo trì ghế đã được bán cho khách trong suất sắp chiếu",
     "Kiểm tra ràng buộc toàn vẹn khi ghế đã phát hành vé",
     "Bước 1: Ghế VIP C05 đã có khách mua vé trong suất 19:00 hôm nay\nBước 2: Quản lý cố tình click khóa bảo trì ghế C05\nBước 3: Kiểm tra phản hồi từ hệ thống",
     "Trạng thái ghế: Đã bán (SOLD)", "Hệ thống từ chối khóa và cảnh báo: 'Ghế này đã có khách mua vé trong suất chiếu 19:00. Vui lòng đổi ghế cho khách trước khi khóa bảo trì'"),

    ("MNT_ERR_02", "Kiểm tra chức năng Chặn nhân viên STAFF không có quyền thực hiện khóa ghế",
     "Kiểm tra phân quyền bảo vệ chức năng bảo trì (Chỉ Quản lý/Admin)",
     "Bước 1: Đăng nhập bằng tài khoản nhân viên STAFF\nBước 2: Cố tình gọi API khóa bảo trì ghế\nBước 3: Kiểm tra phản hồi",
     "Vai trò: STAFF", "Backend từ chối thực thi và trả về mã lỗi 403 Forbidden"),

    ("MNT_ERR_03", "Kiểm tra chức năng Hủy bỏ thao tác khóa trên Modal xác nhận",
     "Kiểm tra khi người dùng bấm 'Hủy bỏ'",
     "Bước 1: Mở Modal khóa bảo trì ghế E04\nBước 2: Click button 'Hủy bỏ'\nBước 3: Kiểm tra trạng thái ghế",
     "Thao tác: Click Hủy bỏ", "Modal đóng lại, ghế E04 giữ nguyên trạng thái TRỐNG (AVAILABLE) bình thường"),

    ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
    ("MNT_FUNC_01", "Kiểm tra chức năng Khóa bảo trì ghế thành công (Chặn chọn ghế trên Web & POS)",
     "Kiểm tra khi ghế bị khóa bảo trì, toàn bộ hệ thống Web và POS không cho phép chọn ghế đó",
     "Bước 1: Quản lý xác nhận khóa bảo trì ghế E04 phòng 2\nBước 2: Mở giao diện Booking trên Web và POS của các suất chiếu tương lai tại phòng 2\nBước 3: Quan sát vị trí E04",
     "Vị trí: E04\nTrạng thái mới: MAINTENANCE", "Ghế E04 chuyển sang màu xám có icon gạch chéo bảo trì, người dùng và nhân viên không thể click chọn"),

    ("MNT_FUNC_02", "Kiểm tra chức năng Mở khóa bảo trì ghế thành công khi sửa chữa xong",
     "Kiểm tra khôi phục trạng thái ghế về AVAILABLE sau khi kỹ thuật sửa xong",
     "Bước 1: Quản lý mở lại sơ đồ ghế, click vào ghế E04 đang bảo trì\nBước 2: Click button '✅ MỞ KHÓA HOẠT ĐỘNG'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Vị trí: E04\nTrạng thái mới: AVAILABLE", "Ghế E04 khôi phục về trạng thái TRỐNG bình thường và mở bán trở lại cho khách hàng"),

    ("MNT_FUNC_03", "Kiểm tra chức năng Đồng bộ trạng thái ghế bảo trì real-time qua WebSocket",
     "Kiểm tra cập nhật tức thì trên màn hình của tất cả các quầy POS và khách online",
     "Bước 1: Quản lý vừa bấm Khóa bảo trì ghế E04\nBước 2: Quan sát màn hình Booking của khách online và quầy POS khác mà không cần F5",
     "Sự kiện: seat_maintenance(E04)", "Ghế E04 tự động chuyển sang màu xám bảo trì ngay lập tức theo thời gian thực"),

    ("MNT_FUNC_04", "Kiểm tra chức năng Ghi nhận nhật ký bảo trì vào Nhật ký hệ thống (Audit Logs)",
     "Kiểm tra lưu vết lịch sử thao tác bảo trì phục vụ đối soát quản lý",
     "Bước 1: Quản lý mở khóa hoạt động cho ghế E04\nBước 2: Truy cập vào màn hình Nhật ký hệ thống (AdminAuditLogs.vue)\nBước 3: Kiểm tra dòng log mới nhất",
     "Module: SEAT_MAINTENANCE\nAction: UNLOCK_SEAT", "Ghi nhận đầy đủ: Người thực hiện, Vị trí ghế E04, Phòng 2, Thời gian mở khóa chính xác")
]
