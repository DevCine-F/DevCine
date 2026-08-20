# -*- coding: utf-8 -*-
"""
Appends the remaining modules to build_senior_human_testreport.py
All action / matrix / view-only screens DO NOT have fake EP / BVA.
"""

def append_part2():
    with open("build_senior_human_testreport.py", "a", encoding="utf-8") as f:
        f.write('''
    # =========================================================================
    # 6. THỐNG KÊ & BÁO CÁO (MOD_ADMIN_DASHBOARD - Dashboard.vue) - NO EP/BVA
    # =========================================================================
    tc_dash = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("STA_GUI_01", "Kiểm tra chức năng hiển thị 4 Thẻ KPI Tổng quan thành công",
         "Kiểm tra hiển thị các thẻ KPI doanh thu, số vé, khách hàng, F&B",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Mở màn hình Thống kê & Báo cáo (Dashboard.vue)\nBước 3: Quan sát hàng thẻ chỉ số KPI phía trên\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị 4 thẻ KPI: 'DOANH THU TỔNG', 'TỔNG SỐ VÉ ĐÃ BÁN', 'KHÁCH HÀNG MỚI', 'DOANH THU F&B' kèm % tăng trưởng so với kỳ trước"),

        ("STA_GUI_02", "Kiểm tra chức năng hiển thị Bộ lọc Thời gian và Custom Month Picker thành công",
         "Kiểm tra hiển thị Bộ chọn khoảng thời gian và chọn tháng",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Quan sát thanh điều khiển bộ lọc thời gian\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị các nút: 'Hôm nay', 'Tuần này', 'Tháng này', 'Năm nay' và Custom Month Picker ('Tháng MM/YYYY')"),

        ("STA_GUI_03", "Kiểm tra chức năng hiển thị Biểu đồ Doanh thu và Top Phim thành công",
         "Kiểm tra hiển thị Biểu đồ cột doanh thu và bảng xếp hạng Top 5 phim",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Quan sát khu vực biểu đồ\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Biểu đồ cột Doanh thu theo ngày/tháng có hover tooltip và Bảng Top 5 phim ăn khách nhất"),

        ("STA_GUI_04", "Kiểm tra chức năng hiển thị Bảng Doanh thu theo Cụm rạp thành công",
         "Kiểm tra hiển thị Bảng phân bổ doanh thu theo từng chi nhánh rạp",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Quan sát bảng dữ liệu cụm rạp\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Bảng: Tên cụm rạp, Số vé bán, Doanh thu vé, Doanh thu F&B, Tổng doanh thu và Tỷ trọng %"),

        ("STA_GUI_05", "Kiểm tra chức năng hiển thị Nút Xuất báo cáo Excel thành công",
         "Kiểm tra hiển thị nút Export Excel",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Quan sát góc phải thanh công cụ Dashboard\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị nút màu xanh '📥 XUẤT BÁO CÁO EXCEL'"),

        ("__SECTION__", "KIỂM TRA TÌM KIẾM & BỘ LỌC DỮ LIỆU"),
        ("STA_LOC_01", "Kiểm tra chức năng Lọc thống kê theo Hôm nay thành công",
         "Kiểm tra chức năng Lọc dữ liệu doanh thu phát sinh trong ngày hôm nay",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Click nút 'Hôm nay'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Khoảng thời gian: Hôm nay", "4 thẻ KPI và biểu đồ cập nhật số liệu các đơn hàng hoàn tất trong ngày hôm nay"),

        ("STA_LOC_02", "Kiểm tra chức năng Lọc thống kê theo Tuần này thành công",
         "Kiểm tra chức năng Lọc dữ liệu doanh thu trong tuần hiện tại (Thứ 2 đến CN)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Click nút 'Tuần này'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Khoảng thời gian: Tuần này (7 ngày)", "Biểu đồ cột hiển thị doanh thu chi tiết 7 ngày trong tuần"),

        ("STA_LOC_03", "Kiểm tra chức năng Lọc thống kê theo Tháng này thành công",
         "Kiểm tra chức năng Lọc dữ liệu doanh thu trong tháng hiện tại",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Click nút 'Tháng này'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Khoảng thời gian: Tháng hiện tại", "Hiển thị tổng doanh thu tháng và xu hướng doanh thu từng tuần"),

        ("STA_LOC_04", "Kiểm tra chức năng Lọc thống kê theo Năm nay thành công",
         "Kiểm tra chức năng Lọc dữ liệu doanh thu cả năm",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Click nút 'Năm nay'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Khoảng thời gian: Năm hiện tại (12 tháng)", "Biểu đồ hiển thị doanh thu theo 12 tháng trong năm"),

        ("STA_LOC_05", "Kiểm tra chức năng Lọc thống kê theo Cụm rạp cụ thể thành công",
         "Kiểm tra chức năng Lọc số liệu riêng cho 1 chi nhánh rạp",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Chọn dropdown Cụm rạp: 'CGV Vincom Cầu Giấy'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Cụm rạp: 'CGV Vincom Cầu Giấy'", "Chỉ hiển thị số vé và doanh thu thuộc riêng rạp Cầu Giấy"),

        ("STA_LOC_06", "Kiểm tra chức năng Chọn tháng quá khứ qua Month Picker thành công",
         "Kiểm tra chức năng Chọn tháng lịch sử trên Custom Month Picker",
         "Bước 1: Mở Custom Month Picker và chọn tháng '01/2024'\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tháng chọn: '01/2024'", "Dashboard tải và hiển thị số liệu lịch sử của tháng 01/2024 chuẩn xác"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("STA_ERR_01", "Kiểm tra chức năng Hiển thị trạng thái rỗng (Empty State) thành công",
         "Kiểm tra hiển thị Dashboard khi chọn khoảng thời gian rạp chưa có phát sinh doanh thu (0đ)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Lọc ngày rạp đóng cửa bảo dưỡng (0 đơn hàng)\nBước 3: Quan sát các biểu đồ\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Doanh thu: 0 VNĐ\nSố vé: 0 vé", "Hiển thị 0đ rõ ràng, biểu đồ phẳng mượt mà, không xảy ra lỗi crash hoặc vỡ layout"),

        ("STA_ERR_02", "Kiểm tra chức năng Chặn chọn tháng trong tương lai thành công",
         "Kiểm tra chức năng Vô hiệu hóa các tháng tương lai trên Month Picker",
         "Bước 1: Mở Custom Month Picker, quan sát các tháng trong tương lai (Tháng 12/2026)\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tháng tương lai: '12/2026'", "Các tháng tương lai bị làm mờ (disabled), không thể click chọn"),

        ("STA_ERR_03", "Kiểm tra chức năng Mất kết nối mạng khi tải Dashboard thất bại",
         "Kiểm tra xử lý lỗi khi mạng bị gián đoạn trong lúc tải dữ liệu thống kê",
         "Bước 1: Ngắt kết nối mạng và click làm mới Dashboard\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Trạng thái: Offline", "Hiển thị nút 'Thử lại' kèm thông báo 'Không thể tải dữ liệu thống kê, vui lòng kiểm tra kết nối mạng'"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("STA_FUNC_01", "Kiểm tra chức năng Xuất báo cáo Excel thành công",
         "Kiểm tra chức năng Tải file Excel báo cáo doanh thu tổng hợp",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Chọn khoảng thời gian tháng hiện tại\nBước 3: Click button '📥 XUẤT BÁO CÁO EXCEL'\nBước 4: Kiểm tra file tải về",
         "Định dạng xuất: Excel (.xlsx)\nPhạm vi: Toàn hệ thống", "Tải về file Excel chứa đầy đủ các sheet: Doanh thu theo rạp, Doanh thu theo phim, Doanh thu Combo F&B"),

        ("STA_FUNC_02", "Kiểm tra chức năng Tự động làm mới số liệu Dashboard real-time",
         "Kiểm tra cơ chế cập nhật số liệu khi có đơn đặt vé mới thành công từ khách hàng",
         "Bước 1: Admin đang mở Dashboard (Doanh thu: 10.000.000đ)\nBước 2: Có khách hàng đặt thành công đơn vé 200.000đ trên Web\nBước 3: Quan sát thẻ KPI Doanh thu trên màn hình Admin",
         "Đơn hàng mới: +200.000đ", "Thẻ KPI tự động cập nhật số liệu lên 10.200.000đ ngay tức thì mà không cần F5 tải lại trang")
    ]

    modules.append({
        "code": "MOD_ADMIN_DASHBOARD", "sheet": "Thống kê & Báo cáo",
        "req": "Kiểm tra Dashboard thống kê doanh thu, KPI cards, Month picker, Biểu đồ và Xuất Excel",
        "tester": "Nguyễn Ngọc Hà Linh", "role": "Quản trị viên",
        "pre": "Quản trị viên mở màn hình Dashboard.vue trên trang quản trị DevCine",
        "test_cases": tc_dash
    })

    # =========================================================================
    # 7. NHẬT KÝ HỆ THỐNG (MOD_ADMIN_AUDIT_LOGS - AdminLogs.vue) - NO EP/BVA
    # =========================================================================
    tc_logs = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("LOG_GUI_01", "Kiểm tra chức năng hiển thị Ô tìm kiếm và Dropdown Lọc hành động thành công",
         "Kiểm tra hiển thị các công cụ lọc nhật ký trên AdminLogs.vue",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Truy cập vào màn hình Nhật ký hệ thống (AdminLogs.vue)\nBước 3: Quan sát thanh tìm kiếm và bộ lọc\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Ô tìm kiếm 'Tìm theo người thực hiện, đối tượng...', Dropdown lọc 'Tất cả hành động' (CREATE, UPDATE, DELETE, SYSTEM, LOGIN)"),

        ("LOG_GUI_02", "Kiểm tra chức năng hiển thị Bảng danh sách Audit Log thành công",
         "Kiểm tra hiển thị Bảng dữ liệu nhật ký thao tác",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Quan sát bảng danh sách log\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị các cột: Thời gian, Người thực hiện (Actor), Vai trò, Hành động (Action Badge), Đối tượng tác động (Target), Địa chỉ IP, Chi tiết"),

        ("LOG_GUI_03", "Kiểm tra chức năng hiển thị Thanh phân trang 20 log/trang thành công",
         "Kiểm tra hiển thị thanh phân trang nhật ký",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Cuộn xuống cuối bảng danh sách\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị tổng số log và các nút điều hướng chuyển trang [<] [1] [2] [3]... [>]"),

        ("__SECTION__", "KIỂM TRA TÌM KIẾM & BỘ LỌC DỮ LIỆU"),
        ("LOG_LOC_01", "Kiểm tra chức năng Lọc theo loại hành động CREATE thành công",
         "Kiểm tra chức năng Lọc các nhật ký thêm mới dữ liệu",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Chọn bộ lọc Hành động: 'Tạo mới (CREATE)'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Hành động: CREATE", "Bảng chỉ hiển thị các log tạo phim, tạo suất chiếu, tạo khuyến mãi có badge màu xanh lá"),

        ("LOG_LOC_02", "Kiểm tra chức năng Lọc theo loại hành động UPDATE thành công",
         "Kiểm tra chức năng Lọc các nhật ký chỉnh sửa dữ liệu",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Chọn bộ lọc Hành động: 'Cập nhật (UPDATE)'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Hành động: UPDATE", "Bảng chỉ hiển thị các log sửa giá vé, sửa phân quyền có badge màu xanh dương"),

        ("LOG_LOC_03", "Kiểm tra chức năng Lọc theo loại hành động DELETE thành công",
         "Kiểm tra chức năng Lọc các nhật ký xóa dữ liệu",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Chọn bộ lọc Hành động: 'Xóa bỏ (DELETE)'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Hành động: DELETE", "Bảng chỉ hiển thị các log xóa suất chiếu, xóa banner có badge màu đỏ"),

        ("LOG_LOC_04", "Kiểm tra chức năng Lọc theo loại hành động SYSTEM thành công",
         "Kiểm tra chức năng Lọc các sự cố hoặc tác vụ tự động của hệ thống",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Chọn bộ lọc Hành động: 'Hệ thống (SYSTEM)'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Hành động: SYSTEM", "Bảng chỉ hiển thị các log nhả ghế timeout, đồng bộ trạng thái tự động"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("LOG_ERR_01", "Kiểm tra chức năng Tìm kiếm nhật ký với từ khóa không khớp thành công",
         "Kiểm tra hiển thị khi tìm kiếm từ khóa không có trong nhật ký",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Nhập từ khóa 'USER_UNKNOWN_9999' vào ô tìm kiếm\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Từ khóa: 'USER_UNKNOWN_9999'", "Hiển thị thông báo 'Không tìm thấy bản ghi nhật ký nào'"),

        ("LOG_ERR_02", "Kiểm tra chức năng Xem chi tiết log JSON hợp lệ thành công",
         "Kiểm tra mở Popup xem Payload JSON chi tiết của hành động",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Click vào icon [Con mắt / Chi tiết] tại 1 dòng log\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Thao tác: Xem chi tiết payload", "Hiển thị Modal JSON format đẹp mắt, hiển thị dữ liệu Before / After của bản ghi"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("LOG_FUNC_01", "Kiểm tra chức năng Tự động ghi Audit Log khi Quản trị viên thao tác thành công",
         "Kiểm tra hệ thống tự động sinh log chuẩn sau mỗi hành động của Admin",
         "Bước 1: Admin thực hiện tạo 1 suất chiếu mới trong AdminShowtimes.vue\nBước 2: Admin mở màn hình Nhật ký hệ thống\nBước 3: Kiểm tra dòng log đầu tiên",
         "Hành động vừa làm: Tạo suất chiếu phim Avatar", "Xuất hiện ngay 1 dòng log mới: Actor 'admin', Action 'CREATE', Target 'Showtime #102', IP '192.168.1.10'"),

        ("LOG_FUNC_02", "Kiểm tra chức năng Phân quyền truy cập Audit Log chỉ dành riêng cho ADMIN",
         "Kiểm tra nhân viên STAFF hoặc MANAGER không thể xem màn hình Nhật ký",
         "Bước 1: Đăng nhập bằng tài khoản STAFF hoặc MANAGER\nBước 2: Cố tình truy cập URL /admin/logs\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tài khoản: STAFF", "Hệ thống từ chối truy cập và chuyển hướng về trang cá nhân kèm cảnh báo 403")
    ]

    modules.append({
        "code": "MOD_ADMIN_AUDIT_LOGS", "sheet": "Nhật ký hệ thống",
        "req": "Kiểm tra Audit Log, Tìm kiếm, Lọc theo hành động (CREATE/UPDATE/DELETE/SYSTEM), Phân trang và Bảo mật",
        "tester": "Phạm Thị Quỳnh Anh", "role": "Quản trị viên",
        "pre": "Quản trị viên mở màn hình Nhật ký hệ thống (AdminLogs.vue)",
        "test_cases": tc_logs
    })

    # =========================================================================
    # 8. QUẢN LÝ KHÁCH HÀNG (MOD_ADMIN_CUSTOMERS - AdminCustomers.vue) - NO EP/BVA
    # =========================================================================
    tc_cust = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("CUS_GUI_01", "Kiểm tra chức năng hiển thị Ô tìm kiếm khách hàng và Bảng danh sách thành công",
         "Kiểm tra hiển thị danh sách thành viên trên AdminCustomers.vue",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Mở màn hình Quản lý khách hàng\nBước 3: Quan sát bảng danh sách khách hàng\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị các cột: Avatar, Họ tên, Số điện thoại, Email, Hạng hội viên (Badge màu), Điểm Loyalty, Tổng chi tiêu, Trạng thái (Active/Locked)"),

        ("CUS_GUI_02", "Kiểm tra chức năng hiển thị Huy hiệu Hạng thẻ Hội viên thành công",
         "Kiểm tra hiển thị màu sắc và tên hạng thẻ",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Quan sát cột Hạng thẻ\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị đúng 4 cấp bậc: BRONZE (Đồng - Nâu), SILVER (Bạc - Xám), GOLD (Vàng - Cam), PLATINUM (Bạch kim - Tím ánh kim)"),

        ("__SECTION__", "KIỂM TRA TÌM KIẾM & BỘ LỌC DỮ LIỆU"),
        ("CUS_LOC_01", "Kiểm tra chức năng Tìm kiếm khách hàng theo Số điện thoại thành công",
         "Kiểm tra chức năng Tìm kiếm thành viên bằng Số điện thoại",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Nhập số điện thoại '0901234567' vào ô tìm kiếm\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Từ khóa: '0901234567'", "Bảng lọc và hiển thị chính xác khách hàng có SĐT '0901234567'"),

        ("CUS_LOC_02", "Kiểm tra chức năng Tìm kiếm khách hàng theo Email thành công",
         "Kiểm tra chức năng Tìm kiếm thành viên bằng Email",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Nhập email 'khachhang@gmail.com' vào ô tìm kiếm\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Từ khóa: 'khachhang@gmail.com'", "Bảng lọc và hiển thị chính xác khách hàng khớp email"),

        ("CUS_LOC_03", "Kiểm tra chức năng Tìm kiếm khách hàng theo Họ tên thành công",
         "Kiểm tra chức năng Tìm kiếm thành viên bằng Họ và tên",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Nhập tên 'Nguyễn Văn Dân' vào ô tìm kiếm\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Từ khóa: 'Nguyễn Văn Dân'", "Bảng lọc và hiển thị danh sách khách hàng có tên chứa 'Nguyễn Văn Dân'"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("CUS_ERR_01", "Kiểm tra chức năng Tìm kiếm không tồn tại thành công",
         "Kiểm tra chức năng Tìm kiếm khi nhập thông tin khách không có trong hệ thống",
         "Bước 1: Nhập SĐT '0999999999' vào ô tìm kiếm\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Từ khóa: '0999999999'", "Hiển thị thông báo 'Không tìm thấy khách hàng phù hợp'"),

        ("CUS_ERR_02", "Kiểm tra chức năng Khóa tài khoản khách hàng có cảnh báo vé chưa xem",
         "Kiểm tra chức năng Khóa tài khoản khi khách hàng đang có vé xem phim chưa sử dụng",
         "Bước 1: Chọn khách hàng đang có 2 vé xem phim tối nay\nBước 2: Click nút Khóa tài khoản (Lock)\nBước 3: Kiểm tra cảnh báo từ hệ thống",
         "Khách hàng: Đang có 2 vé chưa dùng tối nay", "Hiển thị Modal cảnh báo màu vàng: 'Khách hàng này hiện đang có 2 vé xem phim chưa sử dụng tối nay. Bạn có chắc muốn khóa?'"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("CUS_FUNC_01", "Kiểm tra chức năng Tự động tích điểm và nâng hạng thẻ khi khách mua vé",
         "Kiểm tra cơ chế tự động cộng điểm sau mỗi giao dịch mua vé hoàn tất",
         "Bước 1: Khách hàng mua đơn vé 500.000đ\nBước 2: Hệ thống tự động tích 50 điểm loyalty (10k = 1 điểm)\nBước 3: Admin mở màn hình khách hàng kiểm tra",
         "Giao dịch: +500.000đ", "Điểm Loyalty tăng 50 điểm và tự động nâng hạng thẻ nếu vượt ngưỡng"),

        ("CUS_FUNC_02", "Kiểm tra chức năng Debounce 400ms khi tìm kiếm khách hàng",
         "Kiểm tra tính năng Debounce giảm tải request khi Admin gõ tìm kiếm",
         "Bước 1: Admin gõ nhanh '0901234567'\nBước 2: Quan sát Network request trên DevTools\nBước 3: Kiểm tra số lần gọi API",
         "Thao tác: Gõ 10 ký tự liên tiếp trong 300ms", "Chỉ gửi đúng 1 request API sau khi Admin dừng gõ 400ms")
    ]

    modules.append({
        "code": "MOD_ADMIN_CUSTOMERS", "sheet": "Quản lý khách hàng",
        "req": "Kiểm tra Danh sách khách hàng, Tìm kiếm SĐT/Email/Tên, Hạng thẻ Loyalty, Điểm tích lũy và Khóa tài khoản",
        "tester": "Phạm Thị Quỳnh Anh", "role": "Quản trị viên",
        "pre": "Quản trị viên mở màn hình Quản lý khách hàng (AdminCustomers.vue)",
        "test_cases": tc_cust
    })

    # =========================================================================
    # 9. SƠ ĐỒ GHẾ (MOD_ADMIN_SEATMAP - CinemaSeatMapView.vue) - NO EP/BVA
    # =========================================================================
    tc_smp = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("SMP_GUI_01", "Kiểm tra chức năng hiển thị Sơ đồ ma trận ghế và Bảng màu loại ghế thành công",
         "Kiểm tra hiển thị Sơ đồ ghế tại CinemaSeatMapView.vue",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Mở tab Sơ đồ ghế trong CinemaManager.vue\nBước 3: Quan sát lưới ma trận ghế và thanh công cụ\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị ma trận hàng A..J, cột 1..14, thanh chọn loại ghế: 'Ghế Thường', 'Ghế VIP', 'Sweetbox', 'Lối đi / Trống'"),

        ("SMP_GUI_02", "Kiểm tra chức năng hiển thị Nút Lưu cấu hình và Nút Đặt lại thành công",
         "Kiểm tra hiển thị các nút thao tác sơ đồ",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Quan sát góc trên sơ đồ ghế\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị nút 'LƯU SƠ ĐỒ GHẾ' và nút 'ĐẶT LẠI BAN ĐẦU'"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("SMP_ERR_01", "Kiểm tra chức năng Lưu sơ đồ phòng không có ghế thất bại",
         "Kiểm tra chức năng Chặn lưu sơ đồ khi toàn bộ ô đều là lối đi (0 ghế)",
         "Bước 1: Xóa toàn bộ ghế thành lối đi\nBước 2: Click button 'Lưu sơ đồ ghế'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng ghế: 0 ghế", "Hệ thống từ chối và báo lỗi 'Phòng chiếu phải có ít nhất 1 ghế'"),

        ("SMP_ERR_02", "Kiểm tra chức năng Chặn sửa sơ đồ ghế khi phòng đang có suất chiếu đã bán vé",
         "Kiểm tra cơ chế bảo vệ tính toàn vẹn khi phòng đang có vé đã thanh toán",
         "Bước 1: Phòng 1 đang có 5 vé đã bán cho suất chiếu tối nay\nBước 2: Admin cố tình sửa sơ đồ xóa ghế đã bán\nBước 3: Bấm Lưu\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Trạng thái phòng: Có vé đã bán", "Hệ thống chặn lưu và thông báo: 'Không thể sửa đổi sơ đồ ghế vì phòng đang có suất chiếu đã bán vé'"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("SMP_FUNC_01", "Kiểm tra chức năng Đổi loại ghế thành Ghế VIP thành công",
         "Kiểm tra chức năng Click chuyển đổi loại ghế sang Ghế VIP",
         "Bước 1: Click chọn cọ vẽ 'Ghế VIP'\nBước 2: Click vào hàng ghế D05..D10\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Loại chọn: VIP\nVị trí: D05..D10", "Các ghế chuyển sang màu cam/vàng của ghế VIP"),

        ("SMP_FUNC_02", "Kiểm tra chức năng Đổi loại ghế thành Sweetbox thành công",
         "Kiểm tra chức năng Gộp 2 ô liền nhau thành ghế Sweetbox đôi",
         "Bước 1: Click chọn cọ vẽ 'Sweetbox'\nBước 2: Click vào ô J01\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Loại chọn: Sweetbox\nVị trí: J01", "Hệ thống tự động gộp ô J01 và J02 thành 1 khối ghế đôi màu hồng"),

        ("SMP_FUNC_03", "Kiểm tra chức năng Chuyển ô thành Lối đi thành công",
         "Kiểm tra chức năng Biến ô ghế thành khoảng trống lối đi",
         "Bước 1: Click chọn cọ vẽ 'Lối đi / Khoảng trống'\nBước 2: Click vào ô C04\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Loại chọn: Lối đi\nVị trí: C04", "Ô C04 biến mất trở thành lối đi trống"),

        ("SMP_FUNC_04", "Kiểm tra chức năng Tự động đánh số nhãn ghế chuẩn xác",
         "Kiểm tra hệ thống tự động sinh mã ghế (A01, A02... J14) bỏ qua các ô lối đi",
         "Bước 1: Đặt ô B04 làm lối đi\nBước 2: Quan sát nhãn các ghế tiếp theo trên hàng B\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Hàng B: B01, B02, B03, [Lối đi], B04...", "Các ghế sau lối đi tự động đánh số tiếp B04, B05 chuẩn xác không bị ngắt quãng"),

        ("SMP_FUNC_05", "Kiểm tra chức năng Đồng bộ sơ đồ ghế sang màn hình Đặt vé tức thì",
         "Kiểm tra sau khi Admin lưu sơ đồ mới, khách hàng mở đặt vé nhìn thấy đúng sơ đồ vừa lưu",
         "Bước 1: Admin đổi ghế C05 thành VIP và bấm Lưu\nBước 2: Mở màn hình BookingView của suất chiếu phòng đó\nBước 3: Kiểm tra ghế C05",
         "Vị trí: C05", "Ghế C05 hiển thị màu vàng VIP đồng bộ hoàn hảo")
    ]

    modules.append({
        "code": "MOD_ADMIN_SEATMAP", "sheet": "Sơ đồ ghế",
        "req": "Kiểm tra Thiết kế sơ đồ ghế ma trận, Loại ghế (Thường/VIP/Sweetbox/Lối đi), Tự động đánh số và Bảo toàn vé đã bán",
        "tester": "Nguyễn Quang Huy", "role": "Quản trị viên",
        "pre": "Quản trị viên mở tab Sơ đồ ghế trên CinemaManager.vue",
        "test_cases": tc_smp
    })

    # =========================================================================
    # 10. POS ĐƠN CHỜ (MOD_POS_PENDING - TicketingPOS.vue) - NO EP/BVA
    # =========================================================================
    tc_pnd = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("PND_GUI_01", "Kiểm tra chức năng hiển thị Thanh Tab đơn chờ và Nút Lưu đơn chờ thành công",
         "Kiểm tra hiển thị các Tab đơn chờ trên TicketingPOS.vue",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Thu ngân (STAFF)\nBước 2: Mở màn hình Bán vé POS (TicketingPOS.vue)\nBước 3: Quan sát thanh tab đơn chờ\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị nút 'LƯU ĐƠN CHỜ' và các tab đơn đang giữ: 'Đơn #1', 'Đơn #2', 'Đơn #3'"),

        ("PND_GUI_02", "Kiểm tra chức năng hiển thị Đồng hồ đếm ngược 10 phút trên từng Tab thành công",
         "Kiểm tra hiển thị thời gian giữ đơn chờ",
         "Bước 1: Lưu 1 đơn chờ tại quầy POS\nBước 2: Quan sát tab đơn chờ vừa tạo\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị đồng hồ đếm ngược 10:00 giảm dần kèm tên/ghi chú của đơn"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("PND_ERR_01", "Kiểm tra chức năng Lưu đơn chờ khi giỏ hàng rỗng thất bại",
         "Kiểm tra chức năng Chặn lưu đơn chờ khi chưa chọn ghế",
         "Bước 1: Chưa chọn vé hoặc món nào trên POS\nBước 2: Click button 'Lưu đơn chờ'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Giỏ hàng: Rỗng (0đ)", "Hiển thị thông báo: 'Giỏ hàng đang trống, không thể lưu đơn chờ'"),

        ("PND_ERR_02", "Kiểm tra chức năng Lưu đơn chờ thứ 4 thất bại",
         "Kiểm tra chức năng Chặn lưu quá 3 đơn chờ (Vượt giới hạn quầy POS)",
         "Bước 1: Đã có 3 đơn chờ đang mở\nBước 2: Cố tình click 'Lưu đơn chờ' lần thứ 4\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số tab hiện có: 3 đơn\nThao tác: Lưu đơn thứ 4", "Hệ thống từ chối và thông báo lỗi: 'Chỉ được phép lưu tối đa 3 đơn chờ tại cùng một quầy'"),

        ("PND_ERR_03", "Kiểm tra chức năng Hủy/Xóa đơn chờ thành công",
         "Kiểm tra chức năng Click nút [X] đóng tab đơn chờ",
         "Bước 1: Click icon [X] tại tab Đơn #2\nBước 2: Xác nhận xóa đơn chờ\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Thao tác: Xóa đơn #2", "Tab Đơn #2 bị đóng, ghế của đơn #2 được giải phóng ngay lập tức"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("PND_FUNC_01", "Kiểm tra chức năng Lưu đơn chờ thành công",
         "Kiểm tra chức năng Lưu đơn chờ khi giỏ hàng hợp lệ",
         "Bước 1: Chọn 2 vé ghế F05, F06, nhập ghi chú 'Khách rút tiền ATM'\nBước 2: Click button 'Lưu đơn chờ'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số đơn lưu: 1 đơn\nGhi chú: 'Khách rút tiền ATM'", "Tạo tab 'Đơn #1' thành công, giỏ hàng POS được làm trống để phục vụ khách tiếp theo"),

        ("PND_FUNC_02", "Kiểm tra chức năng Khôi phục đơn chờ thành công",
         "Kiểm tra chức năng Mở lại đơn chờ đã lưu để thanh toán",
         "Bước 1: Click vào tab 'Đơn #1'\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Thao tác: Click Tab Đơn #1", "Toàn bộ ghế F05, F06 và combo bắp nước của đơn #1 được nạp lại đầy đủ vào giỏ hàng POS"),

        ("PND_FUNC_03", "Kiểm tra chức năng Hết hạn đơn chờ và Phạt khóa ghế 5 phút (Penalty Lock)",
         "Kiểm tra xử lý khi đơn chờ quá thời gian 10 phút mà thu ngân chưa bấm Hoàn tất",
         "Bước 1: Thu ngân lưu đơn chờ giữ ghế VIP G05\nBước 2: Quá thời gian 10 phút đơn chờ hết hạn (00:00)\nBước 3: Kiểm tra trạng thái ghế G05",
         "Thời gian chờ: > 10 phút (Timeout)", "Hệ thống tự động xóa tab đơn chờ, kích hoạt trạng thái Penalty Lock khóa ghế G05 trong 5 phút để tránh đầu cơ vé trước khi nhả về trạng thái trống")
    ]

    modules.append({
        "code": "MOD_POS_PENDING", "sheet": "POS Đơn chờ",
        "req": "Kiểm tra Lưu đơn chờ, Giới hạn 3 tab, Đếm ngược 10 phút, Khôi phục đơn và Phạt khóa ghế khi quá hạn",
        "tester": "Nguyễn Quang Huy", "role": "Thu ngân (STAFF)",
        "pre": "Thu ngân mở màn hình POS Bán vé (TicketingPOS.vue)",
        "test_cases": tc_pnd
    })

    # =========================================================================
    # 11. SOÁT VÉ & CHECK-IN (MOD_STAFF_CHECKIN - TicketCheckIn.vue) - NO EP/BVA
    # =========================================================================
    tc_chk = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("CHK_GUI_01", "Kiểm tra chức năng hiển thị Khung quét Camera QR và Ô nhập mã thủ công thành công",
         "Kiểm tra giao diện soát vé tại TicketCheckIn.vue",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Nhân viên Soát vé\nBước 2: Mở màn hình Soát vé (TicketCheckIn.vue)\nBước 3: Quan sát khung quét và thanh nhập mã\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Khung camera quét mã QR có tia laser đỏ quét liên tục, Ô nhập 'Mã vé thủ công (Ticket Code)' và nút 'CHECK-IN'"),

        ("CHK_GUI_02", "Kiểm tra chức năng hiển thị Card Kết quả Check-in thành công",
         "Kiểm tra hiển thị chi tiết vé sau khi quét",
         "Bước 1: Quét 1 vé xem phim hợp lệ\nBước 2: Quan sát Card kết quả\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Tên phim, Suất chiếu, Phòng chiếu, Số ghế, Danh sách bắp nước đi kèm và nút 'IN LẠI VÉ CỨNG'"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("CHK_ERR_01", "Kiểm tra chức năng Soát vé thất bại khi vé đã qua sử dụng",
         "Kiểm tra chức năng Chặn quét vé trùng lặp (Đã check-in trước đó)",
         "Bước 1: Đưa mã QR vé đã check-in cách đây 10 phút trước camera\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Mã vé: 'TIC20260319001' (Đã dùng)", "Hiển thị màn hình đỏ cảnh báo: 'VÉ ĐÃ ĐƯỢC SỬ DỤNG LÚC 19:45 BỞI SOÁT VÉ 1', phát âm thanh cảnh báo trầm"),

        ("CHK_ERR_02", "Kiểm tra chức năng Soát vé thất bại khi sai Cụm rạp",
         "Kiểm tra chức năng Chặn vé thuộc cụm rạp khác",
         "Bước 1: Quét vé được mua tại chi nhánh 'CGV Cầu Giấy' tại quầy của 'CGV Hà Đông'\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Mã vé: Rạp CGV Cầu Giấy\nRạp quét: CGV Hà Đông", "Hiển thị thông báo lỗi 'Vé này thuộc cụm rạp CGV Cầu Giấy, không hợp lệ tại cụm rạp này'"),

        ("CHK_ERR_03", "Kiểm tra chức năng Quét mã QR không hợp lệ thất bại",
         "Kiểm tra chức năng Xử lý khi quét mã QR lạ (không phải vé DevCine)",
         "Bước 1: Đưa mã QR URL website khác hoặc mã vạch sản phẩm trước camera\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Mã quét: 'https://google.com'", "Hiển thị thông báo: 'Mã QR không đúng định dạng vé DevCine'"),

        ("CHK_ERR_04", "Kiểm tra chức năng Xử lý khi Camera mất kết nối thất bại",
         "Kiểm tra xử lý khi ngắt webcam hoặc camera bị lỗi phần cứng",
         "Bước 1: Ngắt kết nối camera quét\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Thiết bị: Camera Disconnected", "Hiển thị thông báo 'Không tìm thấy thiết bị Camera, vui lòng chuyển sang nhập mã vé thủ công'"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("CHK_FUNC_01", "Kiểm tra chức năng Soát vé thành công",
         "Kiểm tra chức năng Check-in khi quét mã QR vé hợp lệ đúng suất chiếu",
         "Bước 1: Đưa mã QR vé hợp lệ trước camera quét\nBước 2: Hệ thống quét mã thành công\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Mã vé: 'TIC20260319001' (Hợp lệ)", "Hiển thị màn hình xanh lá: 'CHECK-IN THÀNH CÔNG', phát âm thanh Beep cao, chuyển trạng thái vé sang CHECKED_IN"),

        ("CHK_FUNC_02", "Kiểm tra chức năng Phát âm thanh phản hồi Audio Beep khi Check-in",
         "Kiểm tra âm thanh Beep phân biệt rõ ràng giữa vé hợp lệ và vé lỗi",
         "Bước 1: Quét vé hợp lệ -> Kiểm tra âm thanh\nBước 2: Quét vé lỗi -> Kiểm tra âm thanh",
         "Âm thanh: Audio Web API", "Vé hợp lệ phát tiếng 'Beep' cao vui tươi; vé lỗi phát tiếng 'Buzzer' trầm cảnh báo"),

        ("CHK_FUNC_03", "Kiểm tra chức năng In lại vé cứng (Reprint Ticket) tại quầy soát vé",
         "Kiểm tra chức năng Gửi lệnh in lại vé giấy cho khách có nhu cầu giữ vé làm kỷ niệm",
         "Bước 1: Check-in vé thành công\nBước 2: Click button 'IN LẠI VÉ CỨNG'\nBước 3: Kiểm tra máy in vé nhiệt",
         "Thao tác: In lại vé", "Máy in xuất vé giấy đầy đủ thông tin kèm dấu mộc 'ĐÃ CHECK-IN'")
    ]

    modules.append({
        "code": "MOD_STAFF_CHECKIN", "sheet": "Soát vé & Check-in",
        "req": "Kiểm tra Quét mã QR camera, Nhập mã thủ công, Chặn vé trùng lặp, Audio Beep và In lại vé",
        "tester": "Phạm Thị Quỳnh Anh", "role": "Soát vé (STAFF)",
        "pre": "Nhân viên soát vé mở màn hình Soát vé (TicketCheckIn.vue)",
        "test_cases": tc_chk
    })

    # =========================================================================
    # 12. PHÊ DUYỆT HỦY ĐƠN F&B (MOD_MGR_APPROVE_VOID - ApprovalQueue.vue) - NO EP/BVA
    # =========================================================================
    tc_void = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("VOD_GUI_01", "Kiểm tra chức năng hiển thị Danh sách yêu cầu Chờ duyệt trên ApprovalQueue.vue thành công",
         "Kiểm tra hiển thị Hàng đợi yêu cầu hủy đơn F&B",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản lý rạp (MANAGER)\nBước 2: Mở màn hình Phê duyệt yêu cầu (ApprovalQueue.vue)\nBước 3: Quan sát tab 'Chờ duyệt'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Bảng: Mã yêu cầu, Loại yêu cầu (Hủy F&B), Thu ngân gửi yêu cầu, Lý do hủy, Số tiền hủy, Thời gian gửi, Nút 'Phê duyệt' và 'Từ chối'"),

        ("VOD_GUI_02", "Kiểm tra chức năng hiển thị Modal Nhập lý do từ chối thành công",
         "Kiểm tra hiển thị Popup lý do từ chối hủy đơn",
         "Bước 1: Click button 'Từ chối' tại 1 yêu cầu hủy F&B\nBước 2: Quan sát Modal hiển thị\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Modal yêu cầu Quản lý nhập 'Lý do từ chối' kèm nút 'Xác nhận từ chối' và 'Hủy'"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("VOD_ERR_01", "Kiểm tra chức năng Để trống lý do từ chối thất bại",
         "Kiểm tra chức năng Bắt buộc nhập lý do khi từ chối yêu cầu",
         "Bước 1: Để trống ô lý do từ chối\nBước 2: Click button 'Xác nhận từ chối'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Lý do: Null", "Hiển thị thông báo lỗi 'Vui lòng nhập lý do từ chối để thông báo cho thu ngân'"),

        ("VOD_ERR_02", "Kiểm tra chức năng Xử lý yêu cầu đã được duyệt trước đó thất bại",
         "Kiểm tra khi 2 Quản lý cùng mở danh sách và duyệt cùng 1 yêu cầu",
         "Bước 1: Quản lý A đã duyệt yêu cầu #089\nBước 2: Quản lý B click Phê duyệt yêu cầu #089 trên màn hình chưa F5\nBước 3: Kiểm tra phản hồi từ hệ thống",
         "Yêu cầu: Đã duyệt bởi Quản lý A", "Hệ thống thông báo: 'Yêu cầu này đã được xử lý bởi Quản lý khác' và tự động ẩn khỏi danh sách chờ"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("VOD_FUNC_01", "Kiểm tra chức năng Phê duyệt yêu cầu hủy F&B thành công",
         "Kiểm tra chức năng Phê duyệt hủy đơn F&B của thu ngân",
         "Bước 1: Quản lý chọn yêu cầu hủy đơn F&B #FNB_089 do 'Khách đổi vị bắp'\nBước 2: Click button 'Phê duyệt'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Mã yêu cầu: FNB_089\nThao tác: Phê duyệt", "Phê duyệt thành công, đơn F&B chuyển trạng thái VOIDED, tự động trừ doanh thu ca thu ngân"),

        ("VOD_FUNC_02", "Kiểm tra chức năng Từ chối yêu cầu hủy F&B thành công",
         "Kiểm tra chức năng Từ chối yêu cầu hủy đơn khi lý do không chính đáng",
         "Bước 1: Quản lý click 'Từ chối', nhập lý do: 'Bắp nước đã làm xong và khách đã mang vào rạp'\nBước 2: Click button 'Xác nhận từ chối'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Mã yêu cầu: FNB_089\nLý do: 'Bắp nước đã làm xong và khách đã mang vào rạp'", "Yêu cầu chuyển trạng thái REJECTED, đơn hàng giữ nguyên trạng thái hoàn tất"),

        ("VOD_FUNC_03", "Kiểm tra chức năng Ghi log Audit và Thông báo real-time cho Thu ngân khi duyệt hủy đơn",
         "Kiểm tra hệ thống gửi thông báo WebSocket về máy POS của thu ngân khi được duyệt",
         "Bước 1: Quản lý bấm Phê duyệt yêu cầu hủy F&B\nBước 2: Kiểm tra màn hình POS của thu ngân\nBước 3: Kiểm tra Audit Log",
         "Sự kiện: Manager Approved Void", "Màn hình POS thu ngân nhận thông báo 'Yêu cầu hủy đơn đã được Quản lý phê duyệt' và ghi Audit Log đầy đủ")
    ]

    modules.append({
        "code": "MOD_MGR_APPROVE_VOID", "sheet": "Phê duyệt hủy đơn F&B",
        "req": "Kiểm tra Hàng đợi duyệt hủy F&B, Phê duyệt, Từ chối kèm lý do, Đồng bộ doanh thu và Ghi log",
        "tester": "Phạm Thị Quỳnh Anh", "role": "Quản lý rạp (MANAGER)",
        "pre": "Quản lý rạp mở màn hình Phê duyệt yêu cầu (ApprovalQueue.vue)",
        "test_cases": tc_void
    })

    # =========================================================================
    # 13. XỬ LÝ SỰ CỐ & ĐỔI GHẾ (MOD_STAFF_INCIDENT_RELOCATE - IncidentManagement.vue) - NO EP/BVA
    # =========================================================================
    tc_rel = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("REL_GUI_01", "Kiểm tra chức năng hiển thị Khối tra cứu vé sự cố và Sơ đồ đổi ghế thành công",
         "Kiểm tra giao diện xử lý sự cố tại IncidentManagement.vue",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Nhân viên / Quản lý\nBước 2: Mở màn hình Xử lý sự cố (IncidentManagement.vue)\nBước 3: Quan sát giao diện đổi ghế\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Ô tra cứu vé, Sơ đồ ghế phòng chiếu hiện tại, Khung chọn ghế chuyển đến (Target Seat) và Nút 'XÁC NHẬN ĐỔI GHẾ'"),

        ("REL_GUI_02", "Kiểm tra chức năng hiển thị Khối Tặng Voucher đền bù thành công",
         "Kiểm tra hiển thị tùy chọn phát voucher đền bù khách hàng",
         "Bước 1: Truy cập vào IncidentManagement.vue\nBước 2: Quan sát khối đền bù\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Checkbox 'Tặng voucher đền bù', Dropdown chọn gói voucher (Voucher Bắp nước 50k, Voucher Vé miễn phí 100%)"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("REL_ERR_01", "Kiểm tra chức năng Đổi sang ghế đã bán thất bại",
         "Kiểm tra chức năng Chặn chọn ghế đích là ghế đã có người ngồi",
         "Bước 1: Ghế F08 đã có người mua (SOLD)\nBước 2: Cố tình click chọn F08 làm ghế chuyển đến\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Ghế đích: F08 (Đã bán)", "Không cho phép chọn, thông báo: 'Vị trí này đã có người ngồi, vui lòng chọn ghế trống'"),

        ("REL_ERR_02", "Kiểm tra chức năng Đổi sang ghế đang bảo trì thất bại",
         "Kiểm tra chức năng Chặn chọn ghế đích là ghế đang bảo trì",
         "Bước 1: Ghế C01 đang bảo trì (MAINTENANCE)\nBước 2: Cố tình chọn C01 làm ghế đích\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Ghế đích: C01 (Bảo trì)", "Không cho phép chọn, thông báo: 'Ghế đang bảo trì kỹ thuật'"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("REL_FUNC_01", "Kiểm tra chức năng Đổi ghế sang ghế cùng hạng thành công",
         "Kiểm tra chức năng Đổi ghế sự cố sang ghế thường còn trống",
         "Bước 1: Ghế B03 của khách bị gãy tựa lưng (sự cố)\nBước 2: Chọn đổi sang ghế trống B06 cùng hạng Thường\nBước 3: Click button 'Xác nhận đổi ghế'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Ghế cũ: B03 (Thường)\nGhế mới: B06 (Thường)", "Đổi ghế thành công, sinh mã vé mới cho ghế B06 và giải phóng B03"),

        ("REL_FUNC_02", "Kiểm tra chức năng Nâng hạng ghế VIP đền bù miễn phí thành công",
         "Kiểm tra chức năng Nâng khách lên ghế VIP khi hết ghế thường mà không thu thêm phụ phí",
         "Bước 1: Ghế thường B03 bị sự cố nước đổ, phòng hết ghế thường trống\nBước 2: Nhân viên chọn nâng lên ghế VIP D05 (Miễn phí đền bù)\nBước 3: Bấm Xác nhận\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Ghế cũ: B03 (Thường)\nGhế mới: D05 (VIP - Miễn phí)", "Hệ thống cho phép chuyển ghế VIP với giá 0đ và ghi nhận sự cố đền bù dịch vụ"),

        ("REL_FUNC_03", "Kiểm tra chức năng Tự động phát Voucher đền bù vào tài khoản khách hàng",
         "Kiểm tra hệ thống tự động cộng voucher vào ví điện tử của khách sau khi xử lý sự cố",
         "Bước 1: Tích chọn 'Tặng voucher Vé miễn phí 100%' cho khách\nBước 2: Bấm Xác nhận đổi ghế\nBước 3: Kiểm tra ví voucher của khách hàng",
         "Gói đền bù: Voucher Vé 100%", "Ví voucher của khách xuất hiện mã giảm giá mới kèm thông báo xin lỗi từ ban quản lý rạp"),

        ("REL_FUNC_04", "Kiểm tra chức năng Ghi log sự cố phòng chiếu",
         "Kiểm tra hệ thống ghi nhận sự cố ghế vào báo cáo kỹ thuật phòng chiếu",
         "Bước 1: Hoàn tất đổi ghế sự cố B03\nBước 2: Mở báo cáo kỹ thuật phòng chiếu\nBước 3: Kiểm tra kết quả",
         "Sự cố ghi nhận: Ghế B03 gãy lưng", "Ghế B03 tự động chuyển cờ 'Cần sửa chữa' trong danh sách bảo trì của quản lý rạp")
    ]

    modules.append({
        "code": "MOD_STAFF_INCIDENT_RELOCATE", "sheet": "Xử lý sự cố & Đổi ghế",
        "req": "Kiểm tra Đổi ghế sự cố, Nâng hạng ghế đền bù, Tặng voucher xin lỗi và Ghi nhận báo cáo kỹ thuật",
        "tester": "Nguyễn Quang Huy", "role": "Nhân viên & Quản lý",
        "pre": "Nhân viên mở màn hình Xử lý sự cố (IncidentManagement.vue)",
        "test_cases": tc_rel
    })

    # =========================================================================
    # 14. KHÓA BẢO TRÌ GHẾ VẬT LÝ (MOD_MGR_SEAT_MAINTENANCE - CinemaManager.vue) - NO EP/BVA
    # =========================================================================
    tc_mnt = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("MNT_GUI_01", "Kiểm tra chức năng hiển thị Trạng thái ghế Bảo trì trên Sơ đồ phòng thành công",
         "Kiểm tra hiển thị icon và màu sắc ghế đang bảo trì",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản lý rạp (MANAGER)\nBước 2: Mở tab Quản lý ghế trong CinemaManager.vue\nBước 3: Quan sát sơ đồ ghế phòng chiếu\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị ghế bảo trì với màu xám sọc chéo kèm icon cờ lê vàng (MAINTENANCE)"),

        ("MNT_GUI_02", "Kiểm tra chức năng hiển thị Modal Khóa bảo trì ghế thành công",
         "Kiểm tra hiển thị Popup nhập lý do bảo trì",
         "Bước 1: Click chọn 1 ghế trống và chọn 'Khóa bảo trì'\nBước 2: Quan sát Modal hiển thị\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Modal nhập 'Lý do hỏng hóc / bảo dưỡng' và nút 'XÁC NHẬN KHÓA GHẾ'"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("MNT_ERR_01", "Kiểm tra chức năng Khóa bảo trì ghế đang có vé đã bán trong tương lai có cảnh báo",
         "Kiểm tra cảnh báo khi ghế hỏng đã có khách mua vé ở suất chiếu sắp tới",
         "Bước 1: Ghế F05 có khách mua suất tối mai\nBước 2: Quản lý chọn Khóa bảo trì ghế F05\nBước 3: Kiểm tra cảnh báo từ hệ thống",
         "Ghế: F05 (Có vé tương lai)", "Hiển thị Modal cảnh báo: 'Ghế F05 đã có vé đặt trước cho suất chiếu tối mai. Bạn có muốn khóa và chuyển khách sang ghế khác không?'"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("MNT_FUNC_01", "Kiểm tra chức năng Chuyển ghế sang trạng thái MAINTENANCE thành công",
         "Kiểm tra chức năng Khóa bảo trì ghế khi phát hiện hỏng hóc",
         "Bước 1: Chọn ghế D08, nhập lý do 'Rách đệm mút cần thay bọc mới'\nBước 2: Click button 'Xác nhận khóa ghế'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Ghế: D08\nLý do: 'Rách đệm mút cần thay bọc mới'", "Ghế D08 chuyển sang trạng thái MAINTENANCE, tự động ẩn khỏi sơ đồ bán vé toàn hệ thống"),

        ("MNT_FUNC_02", "Kiểm tra chức năng Mở khóa ghế bảo trì về AVAILABLE thành công",
         "Kiểm tra chức năng Mở lại ghế sau khi kỹ thuật đã sửa chữa xong",
         "Bước 1: Click vào ghế D08 đang bảo trì\nBước 2: Click button 'Hoàn tất bảo trì & Mở lại ghế'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Ghế: D08 (MAINTENANCE)\nThao tác: Mở khóa", "Ghế D08 chuyển về trạng thái AVAILABLE (Trống) và có thể bán vé bình thường"),

        ("MNT_FUNC_03", "Kiểm tra chức năng Tự động khóa ghế trên toàn bộ các Suất chiếu tương lai",
         "Kiểm tra sau khi khóa bảo trì, tất cả suất chiếu tương lai của phòng đó đều tự động khóa ghế đó",
         "Bước 1: Khóa bảo trì ghế D08\nBước 2: Mở sơ đồ đặt vé của 5 suất chiếu khác nhau trong tuần của phòng đó\nBước 3: Kiểm tra ghế D08",
         "Ghế: D08 (MAINTENANCE)", "Ghế D08 hiển thị trạng thái bị khóa / bảo trì trên tất cả các suất chiếu, khách hàng và POS không thể chọn mua")
    ]

    modules.append({
        "code": "MOD_MGR_SEAT_MAINTENANCE", "sheet": "Khóa bảo trì ghế vật lý",
        "req": "Kiểm tra Khóa bảo trì ghế, Mở khóa sau khi sửa, Cảnh báo vé tương lai và Đồng bộ toàn hệ thống",
        "tester": "Nguyễn Quang Huy", "role": "Quản lý rạp (MANAGER)",
        "pre": "Quản lý rạp mở tab Quản lý ghế trên CinemaManager.vue",
        "test_cases": tc_mnt
    })
''')
    print("Part 2 updated cleanly.")

if __name__ == '__main__':
    append_part2()
