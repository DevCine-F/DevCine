# -*- coding: utf-8 -*-
"""
Appends standard form modules and the main workbook generator to build_senior_human_testreport.py
"""

def append_part3():
    with open("build_senior_human_testreport.py", "a", encoding="utf-8") as f:
        f.write('''
    # =========================================================================
    # 27 STANDARD FORM MODULES (REAL FORM INPUT FIELDS)
    # =========================================================================
    all_specs = [
        ("MOD_AUTH_REG", "Đăng ký", "Kiểm tra Đăng ký tài khoản khách hàng mới", "Phạm Thị Quỳnh Anh", "Khách vãng lai", "Mở trang Đăng ký", "REG",
         [("form Đăng ký tài khoản", "form đăng ký", "Hiển thị Họ tên, Số điện thoại, Email, Mật khẩu, Xác nhận MK, Nút Đăng ký")],
         [("Họ và tên", "Họ và tên", "Nguyễn Văn Dân", {"min_len": 2, "max_len": 50}, []),
          ("Số điện thoại", "Số điện thoại", "0901234567", {"min_len": 10, "max_len": 10}, []),
          ("Email", "Email", "khachhang@gmail.com", {"min_len": 5, "max_len": 100}, []),
          ("Mật khẩu", "Mật khẩu", "Khach@123", {"min_len": 6, "max_len": 50}, [])],
         [],
         [("REG_FUNC_01", "Kiểm tra chức năng Đăng ký thành công", "Kiểm tra chức năng Đăng ký tài khoản khi nhập đầy đủ thông tin hợp lệ", "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng ký\\nBước 3: Nhập Họ tên, SĐT, Email, Mật khẩu và Xác nhận mật khẩu khớp\\nBước 4: Click button 'Đăng ký'\\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống", "Họ tên: 'Nguyễn Văn Dân'\\nSĐT: '0901234567'\\nEmail: 'khachhang@gmail.com'\\nMật khẩu: 'Khach@123'", "Đăng ký thành công, tự động đăng nhập và gửi email xác nhận tài khoản"),
          ("REG_FUNC_02", "Kiểm tra chức năng Đăng ký thất bại khi Số điện thoại đã tồn tại", "Kiểm tra chức năng Đăng ký khi số điện thoại đã được đăng ký", "Bước 1: Truy cập vào màn hình Đăng ký\\nBước 2: Nhập số điện thoại '0901234567' đã có trong hệ thống\\nBước 3: Nhập các trường còn lại hợp lệ và bấm Đăng ký\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống", "SĐT: '0901234567' (Đã tồn tại)", "Hiển thị thông báo lỗi 'Số điện thoại này đã được đăng ký trong hệ thống'")]),

        ("MOD_AUTH_FORGOT", "Quên mật khẩu", "Kiểm tra Khôi phục mật khẩu qua Email và OTP", "Phạm Thị Quỳnh Anh", "Khách hàng", "Mở trang Quên mật khẩu", "FOR",
         [("form Quên mật khẩu", "form quên mật khẩu", "Hiển thị Ô nhập Email, Nút Gửi OTP, Ô nhập OTP, Ô Mật khẩu mới")],
         [("Email nhận mã xác thực", "Email nhận mã xác thực", "khachhang@gmail.com", {"min_len": 5, "max_len": 100}, []),
          ("Mã xác thực OTP", "Mã xác thực OTP", "123456", {"min_len": 6, "max_len": 6}, []),
          ("Mật khẩu mới", "Mật khẩu mới", "MatKhauMoi@123", {"min_len": 6, "max_len": 50}, [])],
         [],
         [("FOR_FUNC_01", "Kiểm tra chức năng Gửi mã OTP thành công", "Kiểm tra chức năng Gửi mã OTP khi email tồn tại trong hệ thống", "Bước 1: Mở màn hình Quên mật khẩu\\nBước 2: Nhập email 'khachhang@gmail.com'\\nBước 3: Click button 'Gửi mã xác thực'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống", "Email: 'khachhang@gmail.com'", "Hệ thống gửi mã OTP 6 số về hòm thư email của khách hàng"),
          ("FOR_FUNC_02", "Kiểm tra chức năng Đặt lại mật khẩu thành công", "Kiểm tra chức năng Đặt lại mật khẩu khi nhập đúng mã OTP và mật khẩu mới", "Bước 1: Nhập mã OTP '123456' nhận từ email\\nBước 2: Nhập mật khẩu mới 'MatKhauMoi@123'\\nBước 3: Click button 'Xác nhận đổi mật khẩu'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống", "OTP: '123456'\\nMật khẩu mới: 'MatKhauMoi@123'", "Đặt lại mật khẩu thành công, chuyển hướng về trang Đăng nhập")]),

        ("MOD_POS_TICKETS", "POS Bán vé tại quầy", "Kiểm tra Luồng bán vé trực tiếp tại quầy POS", "Nguyễn Quang Huy", "Thu ngân (STAFF)", "Mở TicketingPOS.vue", "POS",
         [("màn hình POS Bán vé tại quầy", "màn hình POS", "Giao diện cảm ứng chọn phim, suất chiếu, sơ đồ ghế phòng chiếu và in vé")],
         [("Ghi chú đơn hàng", "Ghi chú đơn hàng", "Khách VIP đặt cọc", {"min_len": 2, "max_len": 100}, [])],
         [("Phim", "Avatar: Dòng Chảy Của Nước"), ("Suất chiếu", "19:00 - Phòng 1")],
         [("POS_FUNC_01", "Kiểm tra chức năng Thanh toán tiền mặt tại quầy thành công", "Kiểm tra luồng thu tiền mặt và in vé cứng tại quầy POS", "Bước 1: Thu ngân chọn suất chiếu và chọn 2 ghế VIP\\nBước 2: Chọn phương thức 'Tiền mặt', nhập tiền khách đưa 250.000đ\\nBước 3: Click button 'Hoàn tất & In vé'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống", "Tổng tiền: 200.000đ\\nKhách đưa: 250.000đ\\nTiền thừa: 50.000đ", "Hệ thống tính tiền thừa 50.000đ, in vé nhiệt thành công và cập nhật doanh thu ca làm việc")]),

        ("MOD_ADMIN_MOVIE_CRUD", "Quản lý phim", "Kiểm tra Thêm, Sửa, Xóa phim trên AdminMovies.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminMovies.vue", "MOV",
         [("bảng Danh sách Phim", "danh sách phim", "Bảng dữ liệu hiển thị Poster, Tên phim, Thể loại, Thời lượng, Ngày khởi chiếu, Trạng thái")],
         [("Tên phim", "Tên phim", "Avatar: Dòng Chảy Của Nước", {"min_len": 2, "max_len": 150}, []),
          ("Thời lượng phim", "Thời lượng phim", "192", {"min_len": 2, "max_len": 3}, []),
          ("Đạo diễn", "Đạo diễn", "James Cameron", {"min_len": 2, "max_len": 100}, []),
          ("Mô tả tóm tắt", "Mô tả tóm tắt", "Phần tiếp theo của siêu phẩm Avatar tại hành tinh Pandora...", {"min_len": 10, "max_len": 1000}, [])],
         [("Thể loại", "Hành động, Viễn tưởng"), ("Trạng thái", "Đang chiếu")],
         [("MOV_FUNC_01", "Kiểm tra chức năng Thêm phim mới thành công", "Kiểm tra chức năng Thêm phim mới khi nhập đầy đủ thông tin và upload Poster", "Bước 1: Mở màn hình Quản lý phim\\nBước 2: Click button 'Thêm phim mới'\\nBước 3: Nhập Tên phim, Thời lượng 192 phút, Đạo diễn, Mô tả, upload Poster\\nBước 4: Click button 'Lưu phim'\\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống", "Tên: 'Avatar: Dòng Chảy Của Nước'\\nThời lượng: 192 phút\\nĐạo diễn: 'James Cameron'", "Thêm phim thành công, phim hiển thị ngay trên danh sách và có thể xếp lịch chiếu")]),

        ("MOD_AUTH_CHANGE_PASS", "Đổi mật khẩu", "Kiểm tra Đổi mật khẩu cá nhân", "Phạm Thị Quỳnh Anh", "Khách hàng", "Mở trang Đổi mật khẩu", "CPW",
         [("form Đổi mật khẩu", "form đổi MK", "Hiển thị Mật khẩu hiện tại, Mật khẩu mới, Xác nhận mật khẩu mới")],
         [("Mật khẩu hiện tại", "Mật khẩu hiện tại", "Khach@123", {"min_len": 6, "max_len": 50}, []),
          ("Mật khẩu mới", "Mật khẩu mới", "KhachMoi@2026", {"min_len": 6, "max_len": 50}, [])],
         [],
         [("CPW_FUNC_01", "Kiểm tra chức năng Đổi mật khẩu thành công", "Kiểm tra chức năng Đổi mật khẩu khi mật khẩu cũ đúng và mật khẩu mới hợp lệ", "Bước 1: Mở màn hình Đổi mật khẩu\\nBước 2: Nhập MK hiện tại 'Khach@123', MK mới 'KhachMoi@2026'\\nBước 3: Click button 'Cập nhật mật khẩu'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống", "MK cũ: 'Khach@123'\\nMK mới: 'KhachMoi@2026'", "Đổi mật khẩu thành công, yêu cầu đăng nhập lại bằng mật khẩu mới")]),

        ("MOD_CUST_PROFILE", "Hồ sơ cá nhân", "Kiểm tra Cập nhật thông tin tài khoản", "Phạm Thị Quỳnh Anh", "Khách hàng", "Mở ProfileView.vue", "PRF",
         [("form Thông tin cá nhân", "form thông tin", "Hiển thị Họ tên, Email, Số điện thoại, Ngày sinh, Giới tính, Địa chỉ")],
         [("Họ và tên", "Họ và tên", "Nguyễn Văn Dân", {"min_len": 2, "max_len": 50}, []),
          ("Địa chỉ liên hệ", "Địa chỉ liên hệ", "123 Cầu Giấy, Hà Nội", {"min_len": 5, "max_len": 200}, [])],
         [],
         [("PRF_FUNC_01", "Kiểm tra chức năng Cập nhật hồ sơ thành công", "Kiểm tra chức năng Cập nhật họ tên và địa chỉ", "Bước 1: Mở màn hình Hồ sơ cá nhân\\nBước 2: Chỉnh sửa Họ tên và Địa chỉ\\nBước 3: Click button 'Lưu thay đổi'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống", "Họ tên: 'Nguyễn Văn Dân'\\nĐịa chỉ: '123 Cầu Giấy, Hà Nội'", "Cập nhật hồ sơ thành công")]),

        ("MOD_CUST_SEARCH", "Tìm kiếm & Lọc phim", "Kiểm tra Tìm kiếm và Bộ lọc phim khách hàng", "Nguyễn Quang Huy", "Khách hàng", "Mở MovieListView.vue", "SCH",
         [("thanh Tìm kiếm và Bộ lọc phim", "thanh tìm kiếm", "Ô tìm kiếm từ khóa, Dropdown Thể loại, Định dạng (2D/3D/IMAX), Cụm rạp")],
         [("Từ khóa tìm kiếm phim", "Từ khóa tìm kiếm phim", "Avatar", {"min_len": 2, "max_len": 100}, [])],
         [("Thể loại", "Hành động"), ("Định dạng", "IMAX 3D")],
         [("SCH_FUNC_01", "Kiểm tra chức năng Lọc phim theo Thể loại và Rạp thành công", "Kiểm tra lọc phim Hành động đang chiếu tại rạp Cầu Giấy", "Bước 1: Chọn thể loại 'Hành động', chọn rạp 'CGV Cầu Giấy'\\nBước 2: Click button 'Lọc phim'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Thể loại: 'Hành động'\\nRạp: 'CGV Cầu Giấy'", "Hiển thị danh sách các phim hành động có suất chiếu tại rạp Cầu Giấy")]),

        ("MOD_CUST_REVIEW", "Chi tiết phim & Đánh giá", "Kiểm tra Đánh giá sao và Bình luận phim", "Nguyễn Quang Huy", "Khách hàng", "Mở MovieDetailView.vue", "REV",
         [("khối Đánh giá & Bình luận phim", "khối đánh giá", "Bộ chọn số sao (1-5 sao), Ô nhập nội dung bình luận, Danh sách đánh giá")],
         [("Nội dung bình luận", "Nội dung bình luận", "Phim kỹ xảo quá tuyệt vời, âm thanh sống động!", {"min_len": 5, "max_len": 500}, [])],
         [],
         [("REV_FUNC_01", "Kiểm tra chức năng Gửi đánh giá phim thành công", "Kiểm tra gửi đánh giá 5 sao kèm bình luận", "Bước 1: Chọn 5 sao, nhập nội dung bình luận khen phim\\nBước 2: Click button 'Gửi đánh giá'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Số sao: 5 sao\\nNội dung: 'Phim kỹ xảo quá tuyệt vời, âm thanh sống động!'", "Gửi đánh giá thành công, bình luận xuất hiện ngay dưới chi tiết phim")]),

        ("MOD_CUST_SUPPORT", "Hỗ trợ CSKH", "Kiểm tra Gửi phản hồi / Khiếu nại", "Nguyễn Quang Huy", "Khách hàng", "Mở trang Liên hệ CSKH", "SUP",
         [("form Gửi phản hồi khiếu nại", "form CSKH", "Hiển thị Chủ đề, Nội dung khiếu nại, Tệp đính kèm ảnh vé")],
         [("Tiêu đề khiếu nại", "Tiêu đề khiếu nại", "Khiếu nại về chất lượng âm thanh phòng 2", {"min_len": 5, "max_len": 150}, []),
          ("Nội dung chi tiết", "Nội dung chi tiết", "Suất chiếu 20:00 ngày 18/03 loa bên trái bị rè...", {"min_len": 10, "max_len": 1000}, [])],
         [],
         [("SUP_FUNC_01", "Kiểm tra chức năng Gửi khiếu nại thành công", "Kiểm tra gửi phản hồi khiếu nại kèm mã vé", "Bước 1: Nhập tiêu đề, nội dung và upload ảnh vé\\nBước 2: Click button 'Gửi phản hồi'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Tiêu đề: 'Khiếu nại âm thanh phòng 2'", "Hệ thống ghi nhận vé hỗ trợ #SUP_20260319 và gửi email tiếp nhận cho khách")]),

        ("MOD_STAFF_FIRST_PASS", "Đổi MK lần đầu", "Kiểm tra Đổi mật khẩu bắt buộc cho nhân viên mới", "Phạm Thị Quỳnh Anh", "Nhân viên mới", "Mở trang Đổi MK lần đầu", "FPW",
         [("form Đổi mật khẩu bắt buộc", "form đổi MK lần đầu", "Hiển thị thông báo yêu cầu đổi MK tạm, Ô Mật khẩu mới, Ô Xác nhận MK")],
         [("Mật khẩu mới", "Mật khẩu mới", "NhanVienMoi@2026", {"min_len": 6, "max_len": 50}, [])],
         [],
         [("FPW_FUNC_01", "Kiểm tra chức năng Đổi mật khẩu lần đầu thành công", "Kiểm tra nhân viên mới đổi mật khẩu tạm thành công", "Bước 1: Đăng nhập bằng mật khẩu tạm, hệ thống ép chuyển sang màn hình đổi MK\\nBước 2: Nhập MK mới 'NhanVienMoi@2026'\\nBước 3: Click button 'Xác nhận & Vào hệ thống'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống", "Mật khẩu mới: 'NhanVienMoi@2026'", "Tắt cờ bắt buộc đổi MK và chuyển thẳng vào màn hình làm việc POS")]),

        ("MOD_POS_FNB", "POS Bán F&B tại quầy", "Kiểm tra Bán bắp nước trực tiếp tại quầy POS", "Nguyễn Quang Huy", "Thu ngân (STAFF)", "Mở FnbPOS.vue", "PFN",
         [("màn hình POS Bán bắp nước", "màn hình POS F&B", "Lưới các món F&B, giỏ hàng F&B, nút thanh toán")],
         [("Ghi chú món ăn", "Ghi chú món ăn", "Ít đá, bắp nhiều phô mai", {"min_len": 2, "max_len": 100}, [])],
         [],
         [("PFN_FUNC_01", "Kiểm tra chức năng Bán lẻ bắp nước tại quầy thành công", "Kiểm tra bán 1 Bắp phô mai + 1 Coca", "Bước 1: Click chọn 1 Bắp phô mai, 1 Coca lớn\\nBước 2: Thu tiền mặt 110.000đ và bấm In hóa đơn\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Món chọn: 1 Bắp phô mai, 1 Coca", "In hóa đơn bắp nước thành công và trừ tồn kho F&B")]),

        ("MOD_POS_VOID_FNB", "Yêu cầu hủy đơn F&B", "Kiểm tra Thu ngân gửi yêu cầu hủy đơn F&B", "Nguyễn Quang Huy", "Thu ngân (STAFF)", "Mở VoidFnbModal.vue", "VFN",
         [("modal Yêu cầu hủy đơn F&B", "modal hủy F&B", "Hiển thị thông tin đơn F&B cần hủy, Ô nhập lý do hủy, Nút Gửi yêu cầu")],
         [("Lý do yêu cầu hủy đơn", "Lý do yêu cầu hủy đơn", "Khách đổi ý muốn lấy vị Trứng muối thay vì Phô mai", {"min_len": 5, "max_len": 255}, [])],
         [],
         [("VFN_FUNC_01", "Kiểm tra chức năng Gửi yêu cầu hủy F&B thành công", "Kiểm tra gửi yêu cầu hủy đơn chờ Quản lý duyệt", "Bước 1: Chọn đơn F&B vừa tạo nhầm, nhập lý do đổi vị\\nBước 2: Click button 'Gửi yêu cầu phê duyệt'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Mã đơn: #FNB_089\\nLý do: 'Khách đổi ý lấy Trứng muối'", "Gửi yêu cầu thành công, đơn chuyển trạng thái PENDING_APPROVAL gửi lên màn hình Quản lý")]),

        ("MOD_MGR_COMPENSATION", "Tặng voucher đền bù", "Kiểm tra Quản lý tặng voucher đền bù khách hàng", "Nguyễn Quang Huy", "Quản lý rạp (MANAGER)", "Mở CompensationModal.vue", "CMP",
         [("form Tặng voucher đền bù", "form tặng voucher", "Hiển thị Ô nhập SĐT khách, Dropdown chọn gói voucher đền bù, Ô Lý do tặng")],
         [("Số điện thoại khách nhận", "Số điện thoại khách nhận", "0901234567", {"min_len": 10, "max_len": 10}, []),
          ("Lý do tặng đền bù", "Lý do tặng đền bù", "Đền bù sự cố ghế hỏng phòng 3 suất 19:00", {"min_len": 5, "max_len": 255}, [])],
         [("Gói voucher", "Voucher Miễn Phí 1 Vé 2D")],
         [("CMP_FUNC_01", "Kiểm tra chức năng Phát voucher đền bù thành công", "Kiểm tra phát voucher trực tiếp vào ví của khách hàng", "Bước 1: Nhập SĐT khách '0901234567', chọn gói Vé 2D, nhập lý do\\nBước 2: Click button 'Xác nhận tặng'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "SĐT: '0901234567'\\nGói: Vé 2D 100%", "Phát voucher vào tài khoản khách thành công kèm email xin lỗi")]),

        ("MOD_ADMIN_CATEGORIES", "Danh mục phim", "Kiểm tra Thêm, Sửa Thể loại phim", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminCategories.vue", "CAT",
         [("bảng Danh mục Thể loại phim", "danh sách thể loại", "Hiển thị Tên thể loại, Mã thể loại, Số lượng phim gắn kèm, Trạng thái")],
         [("Tên thể loại phim", "Tên thể loại phim", "Khoa học viễn tưởng", {"min_len": 2, "max_len": 50}, []),
          ("Mô tả thể loại", "Mô tả thể loại", "Các bộ phim về vũ trụ, công nghệ tương lai và du hành thời gian...", {"min_len": 5, "max_len": 255}, [])],
         [],
         [("CAT_FUNC_01", "Kiểm tra chức năng Thêm thể loại phim thành công", "Kiểm tra thêm thể loại phim mới", "Bước 1: Mở Quản lý danh mục phim\\nBước 2: Nhập Tên thể loại 'Khoa học viễn tưởng' và mô tả\\nBước 3: Click button 'Lưu thể loại'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống", "Tên: 'Khoa học viễn tưởng'", "Thêm thể loại thành công, có thể gán cho phim mới")]),

        ("MOD_ADMIN_CINEMAS", "Quản lý cụm rạp", "Kiểm tra Thêm, Sửa Cụm rạp trên AdminCinemas.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminCinemas.vue", "CIN",
         [("bảng Danh sách Cụm rạp", "danh sách cụm rạp", "Hiển thị Tên cụm rạp, Địa chỉ, Số hotline, Tổng số phòng chiếu, Trạng thái")],
         [("Tên cụm rạp", "Tên cụm rạp", "DevCine Vincom Cầu Giấy", {"min_len": 3, "max_len": 100}, []),
          ("Địa chỉ chi tiết", "Địa chỉ chi tiết", "Tầng 5 Vincom Center, 119 Trần Duy Hưng, Cầu Giấy, Hà Nội", {"min_len": 10, "max_len": 255}, []),
          ("Hotline rạp", "Hotline rạp", "02473001234", {"min_len": 10, "max_len": 11}, [])],
         [("Thành phố", "Hà Nội")],
         [("CIN_FUNC_01", "Kiểm tra chức năng Thêm cụm rạp thành công", "Kiểm tra thêm cụm rạp mới vào hệ thống", "Bước 1: Mở Quản lý cụm rạp, click 'Thêm cụm rạp mới'\\nBước 2: Nhập Tên, Địa chỉ, Hotline, chọn Thành phố Hà Nội\\nBước 3: Click button 'Lưu cụm rạp'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống", "Tên: 'DevCine Vincom Cầu Giấy'\\nHotline: '02473001234'", "Thêm cụm rạp mới thành công, hiển thị trên bản đồ chọn rạp của khách hàng")]),

        ("MOD_ADMIN_ROOMS", "Quản lý phòng chiếu", "Kiểm tra Thêm, Sửa Phòng chiếu", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminRooms.vue", "ROM",
         [("bảng Danh sách Phòng chiếu", "danh sách phòng", "Hiển thị Tên phòng, Cụm rạp trực thuộc, Loại phòng (2D/3D/IMAX), Tổng số ghế, Trạng thái")],
         [("Tên phòng chiếu", "Tên phòng chiếu", "Phòng chiếu 01 - IMAX Laser", {"min_len": 2, "max_len": 50}, []),
          ("Tổng số ghế", "Tổng số ghế", "120", {"min_len": 2, "max_len": 3}, [])],
         [("Cụm rạp", "DevCine Cầu Giấy"), ("Loại phòng", "IMAX 3D")],
         [("ROM_FUNC_01", "Kiểm tra chức năng Thêm phòng chiếu thành công", "Kiểm tra thêm phòng chiếu mới", "Bước 1: Mở Quản lý phòng chiếu\\nBước 2: Nhập Tên phòng 'Phòng chiếu 01 - IMAX Laser', chọn loại phòng IMAX\\nBước 3: Click button 'Lưu phòng chiếu'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống", "Tên phòng: 'Phòng chiếu 01 - IMAX Laser'\\nLoại: IMAX 3D", "Thêm phòng chiếu thành công và chuyển sang bước thiết kế sơ đồ ghế")]),

        ("MOD_ADMIN_SHOWTIMES", "Điều phối lịch chiếu", "Kiểm tra Thêm, Sửa Suất chiếu trên AdminShowtimes.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminShowtimes.vue", "SHT",
         [("lưới Lịch chiếu Timeline Scheduler", "lưới lịch chiếu", "Giao diện Timeline trực quan hiển thị các khung giờ chiếu theo từng phòng")],
         [("Giá vé cơ bản", "Giá vé cơ bản", "85000", {"min_len": 4, "max_len": 8}, [])],
         [("Phim", "Avatar: Dòng Chảy Của Nước"), ("Phòng chiếu", "Phòng 01"), ("Giờ chiếu", "19:00")],
         [("SHT_FUNC_01", "Kiểm tra chức năng Xếp suất chiếu thành công", "Kiểm tra xếp suất chiếu mới không bị trùng khung giờ", "Bước 1: Chọn Phim Avatar, Phòng 01, Khung giờ 19:00 - 22:15\\nBước 2: Click button 'Lưu suất chiếu'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Phim: Avatar\\nGiờ chiếu: 19:00 - 22:15", "Xếp suất chiếu thành công, hiển thị thanh khối màu trên Timeline và mở bán vé")]),

        ("MOD_ADMIN_BATCH_SCHEDULE", "Xếp lịch hàng loạt", "Kiểm tra Tự động sinh lịch chiếu cả tuần", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở BatchScheduler.vue", "BAT",
         [("form Cấu hình Xếp lịch hàng loạt", "form xếp lịch tự động", "Chọn khoảng ngày, danh sách phim, tần suất suất chiếu và thời gian nghỉ giữa 2 suất (clean-up time)")],
         [("Thời gian dọn phòng (phút)", "Thời gian dọn phòng (phút)", "15", {"min_len": 1, "max_len": 2}, [])],
         [("Khoảng ngày", "Từ Thứ 2 đến Chủ Nhật")],
         [("BAT_FUNC_01", "Kiểm tra chức năng Xếp lịch hàng loạt thành công", "Kiểm tra sinh tự động lịch chiếu cả tuần không trùng giờ", "Bước 1: Chọn tuần tới, chọn 3 phim hot, clean-up time 15 phút\\nBước 2: Click button 'Tự động sinh lịch chiếu'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Thời gian dọn phòng: 15 phút\\nPhạm vi: Cả tuần", "Tự động sinh thành công 42 suất chiếu cho cả tuần không có xung đột giờ")]),

        ("MOD_ADMIN_FNB_ITEMS", "Thực đơn F&B", "Kiểm tra Thêm, Sửa Món bắp nước trên AdminFnb.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminFnb.vue", "FNI",
         [("bảng Danh sách Thực đơn F&B", "danh sách F&B", "Hiển thị Ảnh món, Tên món, Loại (Bắp/Nước/Snack), Đơn giá, Tồn kho, Trạng thái")],
         [("Tên món F&B", "Tên món F&B", "Bắp rang bơ Vị Phô Mai Lớn", {"min_len": 2, "max_len": 100}, []),
          ("Đơn giá niêm yết", "Đơn giá niêm yết", "59000", {"min_len": 4, "max_len": 7}, [])],
         [("Loại món", "Bắp rang bơ")],
         [("FNI_FUNC_01", "Kiểm tra chức năng Thêm món F&B mới thành công", "Kiểm tra thêm món Bắp phô mai lớn", "Bước 1: Mở Quản lý F&B, click 'Thêm món mới'\\nBước 2: Nhập Tên món, Đơn giá 59.000đ, upload ảnh\\nBước 3: Click button 'Lưu món'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống", "Tên: 'Bắp rang bơ Vị Phô Mai Lớn'\\nĐơn giá: 59.000đ", "Thêm món F&B mới thành công, hiển thị trên menu đặt món")]),

        ("MOD_ADMIN_COMBOS", "Cấu hình Combo", "Kiểm tra Tạo gói Combo bắp nước trên AdminCombos.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminCombos.vue", "CMB",
         [("bảng Danh sách Gói Combo F&B", "danh sách combo", "Hiển thị Tên combo, Thành phần món gộp, Đơn giá combo, % Tiết kiệm, Trạng thái")],
         [("Tên gói Combo", "Tên gói Combo", "Couple Combo Sweet Love", {"min_len": 3, "max_len": 100}, []),
          ("Giá bán Combo", "Giá bán Combo", "109000", {"min_len": 4, "max_len": 7}, [])],
         [],
         [("CMB_FUNC_01", "Kiểm tra chức năng Tạo gói Combo thành công", "Kiểm tra tạo gói 1 Bắp lớn + 2 Nước ngọt lớn", "Bước 1: Nhập Tên combo, chọn thành phần 1 Bắp + 2 Nước, đặt giá 109.000đ\\nBước 2: Click button 'Lưu Combo'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Tên: 'Couple Combo Sweet Love'\\nGiá: 109.000đ", "Tạo combo thành công, hiển thị trên bước chọn Combo của khách hàng")]),

        ("MOD_ADMIN_BASE_PRICING", "Cấu hình bảng giá", "Kiểm tra Ma trận giá vé trên AdminPricing.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminPricing.vue", "PRC",
         [("ma trận Bảng giá vé theo Ngày và Khung giờ", "bảng giá vé", "Ma trận giá vé Ngày thường / Cuối tuần / Ngày lễ phân theo 2D, 3D, VIP, U22")],
         [("Giá vé 2D Người lớn Ngày thường", "Giá vé 2D Ngày thường", "80000", {"min_len": 4, "max_len": 7}, []),
          ("Phụ thu ghế VIP", "Phụ thu ghế VIP", "15000", {"min_len": 4, "max_len": 6}, [])],
         [],
         [("PRC_FUNC_01", "Kiểm tra chức năng Cập nhật ma trận bảng giá vé thành công", "Kiểm tra điều chỉnh giá vé 2D và phụ thu VIP", "Bước 1: Sửa giá vé 2D thành 80.000đ, phụ thu VIP 15.000đ\\nBước 2: Click button 'Lưu bảng giá'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Giá 2D: 80.000đ\\nPhụ thu VIP: 15.000đ", "Cập nhật bảng giá thành công, các suất chiếu mới tự động tính đúng đơn giá")]),

        ("MOD_ADMIN_HOLIDAYS", "Quản lý ngày lễ", "Kiểm tra Khai báo ngày lễ áp giá Lễ", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminHolidays.vue", "HOL",
         [("bảng Danh sách Ngày lễ & Tết", "danh sách ngày lễ", "Hiển thị Tên ngày lễ, Ngày bắt đầu, Ngày kết thúc, Hệ số giá áp dụng")],
         [("Tên ngày lễ", "Tên ngày lễ", "Kỳ nghỉ Lễ Quốc Khánh 02/09", {"min_len": 3, "max_len": 100}, [])],
         [],
         [("HOL_FUNC_01", "Kiểm tra chức năng Thêm ngày lễ thành công", "Kiểm tra khai báo ngày lễ 02/09", "Bước 1: Nhập Tên lễ 'Quốc Khánh 02/09', chọn ngày áp dụng 02/09/2026\\nBước 2: Click button 'Lưu ngày lễ'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Tên: 'Quốc Khánh 02/09'", "Thêm ngày lễ thành công, tất cả suất chiếu ngày 02/09 tự động áp biểu giá Ngày Lễ")]),

        ("MOD_ADMIN_PROMOTIONS", "Quản lý đợt khuyến mãi", "Kiểm tra Thêm, Sửa Khuyến mãi trên AdminPromotions.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminPromotions.vue", "PRM",
         [("bảng Danh sách Đợt khuyến mãi", "danh sách KM", "Hiển thị Mã code, Tên CT, Mức giảm, Khoảng ngày, Trạng thái")],
         [("Mã khuyến mãi", "Mã khuyến mãi", "TRIANVIP2026", {"min_len": 3, "max_len": 30}, []),
          ("Tên chương trình", "Tên chương trình", "Tri Ân Khách Hàng Thân Thiết", {"min_len": 5, "max_len": 150}, [])],
         [("Loại giảm giá", "Giảm theo %")],
         [("PRM_FUNC_01", "Kiểm tra chức năng Phát hành voucher thành công", "Kiểm tra phát hành voucher cho hạng thẻ Gold & Platinum", "Bước 1: Chọn đợt KM 'Tri Ân VIP', chọn đối tượng Hạng Vàng & Kim Cương\\nBước 2: Click button 'Xác nhận phát hành'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Mã: 'TRIANVIP2026'\\nĐối tượng: Hạng Vàng & Kim Cương", "Phát voucher vào ví khách hàng thành công và gửi email thông báo quà tặng")]),

        ("MOD_ADMIN_STAFF_MGMT", "Quản lý nhân viên", "Kiểm tra Thêm, Sửa Nhân viên trên StaffManager.vue", "Phạm Thị Quỳnh Anh", "Quản trị viên", "Mở StaffManager.vue", "STF",
         [("bảng Danh sách Nhân viên", "danh sách NV", "Hiển thị Mã NV, Họ tên, Email, SĐT, Vai trò, Cụm rạp trực thuộc, Trạng thái")],
         [("Họ và tên nhân viên", "Họ và tên nhân viên", "Lê Văn An", {"min_len": 2, "max_len": 50}, []),
          ("Email nhân viên", "Email nhân viên", "an.le@devcine.vn", {"min_len": 5, "max_len": 100}, []),
          ("Số điện thoại", "Số điện thoại", "0977112233", {"min_len": 10, "max_len": 10}, [])],
         [("Vai trò", "Nhân viên (STAFF)"), ("Cụm rạp", "CGV Cầu Giấy")],
         [("STF_FUNC_01", "Kiểm tra chức năng Thêm nhân viên thành công", "Kiểm tra thêm nhân viên mới và tự sinh mật khẩu tạm", "Bước 1: Nhập Họ tên, Email, SĐT, chọn rạp CGV Cầu Giấy\\nBước 2: Click button 'Lưu nhân viên'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Họ tên: 'Lê Văn An'\\nEmail: 'an.le@devcine.vn'", "Tạo nhân viên thành công, tự sinh mật khẩu tạm gửi qua email và bật cờ đổi mật khẩu lần đầu"),
          ("STF_FUNC_02", "Kiểm tra chức năng Khóa tài khoản thất bại", "Kiểm tra chặn tự khóa tài khoản Admin đang đăng nhập phiên hiện tại", "Bước 1: Click toggle khóa tài khoản của chính mình\\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống", "Tài khoản bị khóa: 'admin' (Phiên hiện tại)", "Hệ thống từ chối và báo lỗi 'Không thể tự khóa tài khoản Admin đang đăng nhập phiên hiện tại'")]),

        ("MOD_ADMIN_ORDERS", "Quản lý đơn hàng", "Kiểm tra Tra cứu Đơn hàng trên AdminBookings.vue", "Nguyễn Quang Huy", "Quản trị viên", "Mở tab Đơn hàng trên AdminBookings.vue", "ORD",
         [("bảng Quản lý Đơn hàng & Doanh thu", "danh sách đơn", "Hiển thị Mã đơn, Khách hàng, Cụm rạp, Tổng tiền, Phương thức, Trạng thái")],
         [("Mã đơn hàng", "Mã đơn hàng", "ORD20260319001", {"min_len": 6, "max_len": 50}, [])],
         [("Cụm rạp", "CGV Cầu Giấy"), ("Trạng thái", "Đã thanh toán (CONFIRMED)")],
         [("ORD_FUNC_01", "Kiểm tra chức năng Xuất hóa đơn VAT thành công", "Kiểm tra xuất file PDF hóa đơn điện tử cho đơn CONFIRMED", "Bước 1: Chọn đơn hàng CONFIRMED, click 'Xuất hóa đơn VAT'\\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống", "Mã đơn: 'ORD_20260319_001'", "Sinh và tải về file PDF hóa đơn điện tử chuẩn chỉ đầy đủ thuế VAT")]),

        ("MOD_ADMIN_BANNERS", "Quản lý Banner", "Kiểm tra Thêm, Sửa Banner trên AdminBanners.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminBanners.vue", "BAN",
         [("danh sách Banner quảng cáo", "danh sách banner", "Hiển thị Ảnh banner, Tiêu đề, Phim gắn kèm, Thứ tự hiển thị, Trạng thái")],
         [("Tiêu đề banner", "Tiêu đề banner", "Bom Tấn Avatar Trở Lại", {"min_len": 3, "max_len": 150}, [])],
         [],
         [("BAN_FUNC_01", "Kiểm tra chức năng Thêm banner thành công", "Kiểm tra thêm banner quảng cáo chuẩn kích thước 1920x600 px", "Bước 1: Nhập Tiêu đề, gắn link phim, upload ảnh banner 1920x600\\nBước 2: Click button 'Lưu banner'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Tiêu đề: 'Bom Tấn Avatar Trở Lại'", "Thêm banner thành công, hiển thị ngay trên Slider quảng cáo đầu trang chủ")]),

        ("MOD_ADMIN_NEWS", "Tin tức & Khuyến mãi", "Kiểm tra Quản lý Tin tức & Khuyến mãi", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở quản lý tin tức", "NEW",
         [("trình soạn thảo Bài viết Tin tức Rich Text Editor", "soạn thảo tin tức", "Trình soạn thảo rich text có công cụ định dạng chữ và chèn ảnh")],
         [("Tiêu đề bài viết", "Tiêu đề bài viết", "Ưu Đãi Thứ 4 Vui Vẻ - Đồng Giá Vé 50K Toàn Hệ Thống", {"min_len": 5, "max_len": 200}, []),
          ("Tóm tắt bài viết", "Tóm tắt bài viết", "Chương trình ưu đãi đồng giá vé 50k vào thứ 4 hàng tuần...", {"min_len": 10, "max_len": 500}, [])],
         [],
         [("NEW_FUNC_01", "Kiểm tra chức năng Xuất bản bài viết thành công", "Kiểm tra xuất bản bài viết tin tức mới", "Bước 1: Nhập Tiêu đề, Tóm tắt, nội dung rich text, upload thumbnail\\nBước 2: Click button 'Xuất bản'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Tiêu đề: 'Ưu Đãi Thứ 4 Vui Vẻ'", "Xuất bản bài viết thành công kèm đường dẫn slug chuẩn SEO")]),

        ("MOD_ADMIN_FAQ", "Quản lý FAQ", "Kiểm tra Thêm, Sửa FAQ trên FaqManager.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở FaqManager.vue", "FAQ",
         [("danh sách Câu hỏi thường gặp", "danh sách FAQ", "Hiển thị Nhóm câu hỏi, Câu hỏi, Câu trả lời, Thứ tự")],
         [("Câu hỏi", "Câu hỏi", "Làm thế nào để đổi vé xem phim đã mua?", {"min_len": 5, "max_len": 300}, []),
          ("Câu trả lời", "Câu trả lời", "Quý khách có thể đổi vé trước giờ chiếu ít nhất 60 phút...", {"min_len": 10, "max_len": 1000}, [])],
         [],
         [("FAQ_FUNC_01", "Kiểm tra chức năng Thêm câu hỏi FAQ thành công", "Kiểm tra thêm câu hỏi thường gặp", "Bước 1: Chọn nhóm 'Vé & Giá vé', nhập câu hỏi và câu trả lời\\nBước 2: Click button 'Lưu FAQ'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Câu hỏi: 'Làm thế nào để đổi vé xem phim đã mua?'", "Thêm FAQ thành công, hiển thị trên trang Trợ giúp của khách hàng")]),

        ("MOD_ADMIN_SETTINGS", "Cài đặt hệ thống", "Kiểm tra Tham số động trên AdminSettings.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminSettings.vue", "SET",
         [("bảng Cấu hình Tham số động", "cài đặt tham số", "Các ô nhập thời gian giữ ghế, timeout đơn chờ, hotline, email thông báo")],
         [("Email thông báo", "Email thông báo", "contact@devcine.vn", {"min_len": 5, "max_len": 100}, [])],
         [],
         [("SET_FUNC_01", "Kiểm tra chức năng Cài đặt hệ thống thành công", "Kiểm tra điều chỉnh thời gian giữ đơn chờ POS thành 8 phút", "Bước 1: Sửa tham số 'Thời gian giữ đơn chờ POS' thành 8 phút\\nBước 2: Click button 'Lưu cấu hình'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống", "Tham số: 'Thời gian giữ đơn chờ POS'\\nGiá trị: 8 phút", "Lưu cấu hình thành công và áp dụng ngay lập tức trên máy POS toàn hệ thống")])
    ]

    for mod_spec in all_specs:
        c_code, c_sheet, c_req, c_tester, c_role, c_pre, c_pfx, c_gui, c_fields, c_filters, c_func = mod_spec
        cases = build_module_unified_suite(c_pfx, c_sheet, c_role, c_gui, c_fields, c_filters, c_func)

        modules.append({
            "code": c_code, "sheet": c_sheet, "req": c_req,
            "tester": c_tester, "role": c_role, "pre": c_pre,
            "test_cases": cases
        })

    return modules

def export_human_testreport():
    out_dir = r"c:\\Users\\ADMIN\\OneDrive\\Desktop\\DATN\\devcine"
    out_file = os.path.join(out_dir, "TestReport Dự án DevCine.xlsx")
    
    from build_accurate_devcine_testreport import build_accurate_workbook_file
    import build_accurate_devcine_testreport
    build_accurate_devcine_testreport.build_accurate_modules = build_all_human_modules
    
    build_accurate_devcine_testreport.build_accurate_workbook_file(out_file)
    
    dst_dl = r"C:\\Users\\ADMIN\\Downloads\\TestReport Dự án DevCine.xlsx"
    try:
        shutil.copy2(out_file, dst_dl)
        print("Updated Downloads TestReport Dự án DevCine.xlsx successfully!")
    except Exception as e:
        print("Downloads locked, please close Excel if open.")

if __name__ == '__main__':
    export_human_testreport()
''')
    print("Part 3 appended successfully.")

if __name__ == '__main__':
    append_part3()
