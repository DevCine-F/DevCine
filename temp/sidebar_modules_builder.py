# -*- coding: utf-8 -*-
"""
Sidebar Modules Builder for DevCine
Contains all 28 modules aligned with:
1. Customer Flow (Đăng nhập, Đăng ký, Booking Bước 1-4, Hồ sơ, Lịch sử, Đánh giá)
2. Admin Sidebar:
   - Tổng quan & Vận hành: Tổng quan (Dashboard), Bán vé tại quầy (POS), Bán F&B tại quầy (POS), Kiểm soát vé (Check-in), Hóa đơn & Hủy đơn, Sự cố & Bảo trì ghế
   - Phim & Nội dung: Quản lý phim, Danh mục phim, Quản lý Banner
   - Rạp & Hạ tầng: Cụm rạp & Phòng chiếu (gồm Sơ đồ ghế), Lịch chiếu suất phim, Thực đơn F&B
   - Kinh doanh & Khách hàng: Quản lý giá, Khuyến mãi & Voucher, Khách hàng, Chăm sóc khách hàng
   - Nhân sự & Hệ thống: Nhân viên, Phân quyền, Nhật ký, Cài đặt
"""

from clean_qa_suite import build_module_unified_suite
from detailed_operational_suites import (
    tc_pos_pending, tc_checkin, tc_void, tc_incident, tc_maint
)
from pos_fnb_suite import tc_pos_fnb
from movie_management_suite import full_movie_suite as tc_movies_suite

def get_all_raw_modules():
    # Import the original raw test suites
    raw_specs = [
        ("MOD_ADMIN_CINEMAS", "Cụm rạp", "Kiểm tra Quản lý chi nhánh cụm rạp trên AdminCinemas.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminCinemas.vue", "CIN",
         [("bảng Danh sách Cụm rạp", "danh sách cụm rạp", "Hiển thị Tên rạp, Địa chỉ, Số điện thoại hotline, Thành phố, Trạng thái")],
         [("Tên cụm rạp", "Tên cụm rạp", "DevCine Cầu Giấy", {"min_len": 2, "max_len": 100}, []),
          ("Địa chỉ chi tiết", "Địa chỉ chi tiết", "Tầng 5 Vincom Center, 122 Cầu Giấy, Hà Nội", {"min_len": 5, "max_len": 200}, []),
          ("Hotline liên hệ", "Hotline liên hệ", "02438889999", {"min_len": 10, "max_len": 11}, [])],
         [("Thành phố", "Hà Nội")], []),

        ("MOD_ADMIN_ROOMS", "Phòng chiếu", "Kiểm tra Quản lý phòng chiếu trên AdminRooms.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminRooms.vue", "ROM",
         [("bảng Danh sách Phòng chiếu", "danh sách phòng chiếu", "Hiển thị Tên phòng, Cụm rạp trực thuộc, Định dạng (2D/3D/IMAX), Tổng số ghế")],
         [("Tên phòng chiếu", "Tên phòng chiếu", "Phòng chiếu 01 - IMAX", {"min_len": 2, "max_len": 50}, []),
          ("Tổng số ghế", "Tổng số ghế", "120", {"min_len": 2, "max_len": 3}, [])],
         [("Cụm rạp", "DevCine Cầu Giấy"), ("Định dạng", "IMAX")], []),

        ("MOD_ADMIN_SCHEDULES", "Lịch chiếu", "Kiểm tra Xếp lịch chiếu suất phim trên AdminSchedules.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminSchedules.vue", "SCH",
         [("bảng Lịch chiếu suất phim", "lịch chiếu", "Hiển thị Suất chiếu theo ngày, Phim, Phòng chiếu, Giờ bắt đầu, Giờ kết thúc, Trạng thái")],
         [("Ghi chú suất chiếu", "Ghi chú suất chiếu", "Suất chiếu Sneak Show đặc biệt", {"min_len": 2, "max_len": 100}, [])],
         [("Phim", "Avatar 2"), ("Phòng chiếu", "Phòng 1"), ("Ngày chiếu", "2026-03-20")],
         [("SCH_FUNC_01", "Kiểm tra chức năng Chặn trùng lịch chiếu (Schedule Overlap Guard) thành công", "Kiểm tra hệ thống chặn xếp lịch khi thời gian suất chiếu bị đè lên suất chiếu khác", "Bước 1: Phòng 1 đã có suất chiếu 19:00 - 21:00\nBước 2: Xếp thêm suất chiếu 20:00 - 22:00 cùng phòng 1\nBước 3: Click 'Lưu lịch chiếu'\nBước 4: Kiểm tra phản hồi từ hệ thống", "Suất mới: 20:00 - 22:00 (Trùng phòng 1)", "Hệ thống từ chối lưu và báo lỗi: 'Khung giờ này đã bị trùng với suất chiếu khác trong cùng phòng chiếu'")]),

        ("MOD_ADMIN_PRICING", "Bảng giá vé", "Kiểm tra Cấu hình giá vé theo khung giờ và loại ghế trên AdminPricing.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminPricing.vue", "PRI",
         [("bảng Cấu hình Giá vé", "bảng giá vé", "Hiển thị Khung giờ, Ngày trong tuần (Thường/Cuối tuần), Loại ghế (Thường/VIP/Sweetbox), Đơn giá")],
         [("Tên chính sách giá", "Tên chính sách giá", "Bảng giá Ngày Thường (T2 - T5)", {"min_len": 2, "max_len": 100}, []),
          ("Giá vé cơ bản (VNĐ)", "Giá vé cơ bản (VNĐ)", "85000", {"min_len": 4, "max_len": 7}, []),
          ("Phụ thu ghế VIP (VNĐ)", "Phụ thu ghế VIP (VNĐ)", "15000", {"min_len": 4, "max_len": 6}, [])],
         [("Áp dụng cho", "Ngày thường")], []),

        ("MOD_ADMIN_VOUCHERS", "Khuyến mãi & Voucher", "Kiểm tra Tạo mã giảm giá trên AdminPromotions.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminPromotions.vue", "VOU",
         [("bảng Danh sách Khuyến mãi", "danh sách voucher", "Hiển thị Mã voucher, Tên CTKM, Mức giảm (% hoặc số tiền), Số lượng, Hạn dùng, Trạng thái")],
         [("Mã Voucher", "Mã Voucher", "DEVCINE2026", {"min_len": 3, "max_len": 20}, []),
          ("Tên chương trình", "Tên chương trình", "Giảm 50K cho thành viên mới", {"min_len": 3, "max_len": 100}, []),
          ("Mức giảm giá", "Mức giảm giá", "50000", {"min_len": 2, "max_len": 7}, []),
          ("Giá trị đơn tối thiểu", "Giá trị đơn tối thiểu", "100000", {"min_len": 4, "max_len": 7}, [])],
         [("Loại giảm giá", "Số tiền cố định"), ("Trạng thái", "Đang diễn ra")], []),

        ("MOD_ADMIN_BANNERS", "Banner quảng cáo", "Kiểm tra Quản lý Banner Trang chủ trên AdminBanners.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminBanners.vue", "BAN",
         [("bảng Danh sách Banner", "danh sách banner", "Hiển thị Hình ảnh Banner, Tiêu đề, Link liên kết, Thứ tự hiển thị, Trạng thái")],
         [("Tiêu đề Banner", "Tiêu đề Banner", "Bom tấn Mùa Hè 2026", {"min_len": 2, "max_len": 100}, []),
          ("Link liên kết (URL)", "Link liên kết (URL)", "https://devcine.vn/movies/avatar-2", {"min_len": 5, "max_len": 255}, []),
          ("Thứ tự hiển thị", "Thứ tự hiển thị", "1", {"min_len": 1, "max_len": 2}, [])],
         [("Trạng thái", "Hiển thị")], []),

        ("MOD_ADMIN_GENRES", "Thể loại phim", "Kiểm tra Danh mục Thể loại trên AdminGenres.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminGenres.vue", "GEN",
         [("bảng Danh sách Thể loại", "danh sách thể loại", "Hiển thị Tên thể loại, Mô tả, Số lượng phim thuộc thể loại")],
         [("Tên thể loại", "Tên thể loại", "Hành động (Action)", {"min_len": 2, "max_len": 50}, []),
          ("Mô tả thể loại", "Mô tả thể loại", "Phim hành động kịch tính với các pha rượt đuổi", {"min_len": 2, "max_len": 200}, [])],
         [], []),

        ("MOD_ADMIN_DIRECTORS", "Đạo diễn", "Kiểm tra Danh mục Đạo diễn trên AdminDirectors.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminDirectors.vue", "DIR",
         [("bảng Danh sách Đạo diễn", "danh sách đạo diễn", "Hiển thị Ảnh đại diện, Tên đạo diễn, Quốc tịch, Tiểu sử tóm tắt")],
         [("Tên đạo diễn", "Tên đạo diễn", "James Cameron", {"min_len": 2, "max_len": 100}, []),
          ("Quốc tịch", "Quốc tịch", "Canada", {"min_len": 2, "max_len": 50}, [])],
         [], []),

        ("MOD_ADMIN_ACTORS", "Diễn viên", "Kiểm tra Danh mục Diễn viên trên AdminActors.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminActors.vue", "ACT",
         [("bảng Danh sách Diễn viên", "danh sách diễn viên", "Hiển thị Ảnh đại diện, Tên diễn viên, Quốc tịch, Năm sinh")],
         [("Tên diễn viên", "Tên diễn viên", "Sam Worthington", {"min_len": 2, "max_len": 100}, []),
          ("Quốc tịch", "Quốc tịch", "Úc", {"min_len": 2, "max_len": 50}, [])],
         [], []),

        ("MOD_ADMIN_FORMATS", "Định dạng chiếu", "Kiểm tra Danh mục Định dạng phòng chiếu trên AdminFormats.vue", "Nguyễn Ngọc Hà Linh", "Quản trị viên", "Mở AdminFormats.vue", "FMT",
         [("bảng Danh sách Định dạng", "danh sách định dạng", "Hiển thị Tên định dạng (2D, 3D, IMAX 3D, 4DX), Phụ thu chuẩn")],
         [("Tên định dạng", "Tên định dạng", "IMAX 3D Laser", {"min_len": 2, "max_len": 50}, []),
          ("Phụ thu mặc định (VNĐ)", "Phụ thu mặc định (VNĐ)", "50000", {"min_len": 4, "max_len": 6}, [])],
         [], []),

        ("MOD_ADMIN_FNB_ITEMS", "Món F&B", "Kiểm tra Danh mục Món Bắp nước trên AdminFnbItems.vue", "Nguyễn Quang Huy", "Quản lý", "Mở AdminFnbItems.vue", "ITM",
         [("bảng Danh mục Món F&B", "danh mục món f&b", "Hiển thị Ảnh món, Tên món, Nhóm (Bắp/Nước/Combo), Đơn giá bán, Trạng thái")],
         [("Tên món F&B", "Tên món F&B", "Bắp rang bơ Vị Phô Mai", {"min_len": 2, "max_len": 100}, []),
          ("Đơn giá bán (VNĐ)", "Đơn giá bán (VNĐ)", "65000", {"min_len": 4, "max_len": 7}, []),
          ("Mô tả thành phần", "Mô tả thành phần", "Bắp ngô Mỹ rang bơ tươi phủ bột phô mai béo ngậy", {"min_len": 5, "max_len": 200}, [])],
         [("Nhóm sản phẩm", "Bắp rang"), ("Trạng thái", "Đang bán")], []),

        ("MOD_ADMIN_TOPPINGS", "Tùy chọn vị F&B", "Kiểm tra Tùy chọn vị bắp và nước trên AdminFnbOptions.vue", "Nguyễn Quang Huy", "Quản lý", "Mở AdminFnbOptions.vue", "TOP",
         [("bảng Tùy chọn Vị F&B", "tùy chọn vị f&b", "Hiển thị Tên nhóm tùy chọn, Tên vị, Phụ thu đổi vị, Trạng thái")],
         [("Tên vị / Tùy chọn", "Tên vị / Tùy chọn", "Vị Trứng Muối Hoàng Kim", {"min_len": 2, "max_len": 50}, []),
          ("Phụ thu (VNĐ)", "Phụ thu (VNĐ)", "15000", {"min_len": 4, "max_len": 6}, [])],
         [], []),

        ("MOD_ADMIN_STAFF", "Nhân viên", "Kiểm tra Quản lý Tài khoản Nhân sự trên AdminStaff.vue", "Phạm Thị Quỳnh Anh", "Quản trị viên", "Mở AdminStaff.vue", "STF",
         [("bảng Danh sách Nhân viên", "danh sách nhân viên", "Hiển thị Mã NV, Họ tên, SĐT, Email, Vai trò (Nhân viên/Quản lý), Chi nhánh rạp, Trạng thái")],
         [("Họ và tên nhân viên", "Họ và tên nhân viên", "Văn Minh Khôi", {"min_len": 2, "max_len": 50}, []),
          ("Số điện thoại", "Số điện thoại", "0912345678", {"min_len": 10, "max_len": 10}, []),
          ("Email nội bộ", "Email nội bộ", "khoivm@devcine.vn", {"min_len": 5, "max_len": 100}, [])],
         [("Vai trò", "Nhân viên"), ("Cụm rạp", "DevCine Cầu Giấy")], []),

        ("MOD_ADMIN_SETTINGS", "Cài đặt hệ thống", "Kiểm tra Cấu hình Rạp và Tham số hệ thống trên AdminSettings.vue", "Phạm Thị Quỳnh Anh", "Quản trị viên", "Mở AdminSettings.vue", "SET",
         [("form Cài đặt hệ thống", "form cài đặt", "Hiển thị Thời gian giữ chỗ (phút), Tên thương hiệu, Hotline CSKH, Email nhận thông báo")],
         [("Tên thương hiệu Rạp", "Tên thương hiệu Rạp", "DevCine Cinema Complex", {"min_len": 2, "max_len": 100}, []),
          ("Thời gian giữ chỗ (phút)", "Thời gian giữ chỗ (phút)", "10", {"min_len": 1, "max_len": 2}, []),
          ("Hotline CSKH", "Hotline CSKH", "1900123456", {"min_len": 10, "max_len": 11}, [])],
         [], []),

        ("MOD_ADMIN_LOYALTY", "Điểm thưởng Loyalty", "Kiểm tra Cấu hình Tỷ lệ tích điểm trên AdminLoyalty.vue", "Phạm Thị Quỳnh Anh", "Quản trị viên", "Mở AdminLoyalty.vue", "LOY",
         [("bảng Cấu hình Hạng thẻ Loyalty", "cấu hình loyalty", "Hiển thị Hạng thẻ, Chi tiêu yêu cầu, Tỷ lệ tích điểm %, Quyền lợi đặc quyền")],
         [("Tên hạng thẻ", "Tên hạng thẻ", "Diamond Member", {"min_len": 2, "max_len": 50}, []),
          ("Chi tiêu tối thiểu (VNĐ)", "Chi tiêu tối thiểu (VNĐ)", "5000000", {"min_len": 4, "max_len": 8}, []),
          ("Tỷ lệ tích điểm (%)", "Tỷ lệ tích điểm (%)", "10", {"min_len": 1, "max_len": 2}, [])],
         [], []),

        ("MOD_CUST_CHANGE_PASS", "Đổi mật khẩu", "Kiểm tra Đổi mật khẩu tài khoản trên CustomerChangePassword.vue", "Phạm Thị Quỳnh Anh", "Khách hàng", "Mở trang Đổi mật khẩu", "CPW",
         [("form Đổi mật khẩu", "form đổi mật khẩu", "Hiển thị Mật khẩu hiện tại, Mật khẩu mới, Xác nhận mật khẩu mới, Nút Cập nhật")],
         [("Mật khẩu hiện tại", "Mật khẩu hiện tại", "KhachCu@123", {"min_len": 6, "max_len": 50}, []),
          ("Mật khẩu mới", "Mật khẩu mới", "KhachMoi@123", {"min_len": 6, "max_len": 50}, [])],
         [], []),

        ("MOD_CUST_PROFILE", "Thông tin cá nhân", "Kiểm tra Cập nhật hồ sơ trên CustomerProfile.vue", "Phạm Thị Quỳnh Anh", "Khách hàng", "Mở trang Thông tin cá nhân", "PRO",
         [("form Hồ sơ cá nhân", "form hồ sơ cá nhân", "Hiển thị Avatar, Họ tên, SĐT (Read-only), Email, Ngày sinh, Giới tính")],
         [("Họ và tên", "Họ và tên", "Nguyễn Văn Dân", {"min_len": 2, "max_len": 50}, []),
          ("Email liên hệ", "Email liên hệ", "dan.nguyen@gmail.com", {"min_len": 5, "max_len": 100}, [])],
         [("Giới tính", "Nam")], []),

        ("MOD_CUST_REVIEWS", "Đánh giá phim", "Kiểm tra Gửi đánh giá sao trên MovieDetailView.vue", "Nguyễn Quang Huy", "Khách hàng", "Mở trang Chi tiết phim", "REV",
         [("form Đánh giá Phim", "form đánh giá phim", "Hiển thị Bộ chọn số sao 1-5, Ô nhập nhận xét, Nút Gửi đánh giá")],
         [("Nội dung nhận xét", "Nội dung nhận xét", "Phim rất xuất sắc, kỹ xảo đỉnh cao và âm thanh sống động!", {"min_len": 5, "max_len": 500}, [])],
         [("Số sao đánh giá", "5 sao")], []),

        ("MOD_CUST_COMMENTS", "Bình luận", "Kiểm tra Bình luận phim trên MovieDetailView.vue", "Nguyễn Quang Huy", "Khách hàng", "Mở trang Chi tiết phim", "CMT",
         [("khung Bình luận", "khung bình luận", "Hiển thị Ô gõ bình luận, Nút Gửi bình luận và Danh sách bình luận cộng đồng")],
         [("Nội dung bình luận", "Nội dung bình luận", "Ai đi xem suất 19h tối nay với mình không?", {"min_len": 2, "max_len": 300}, [])],
         [], []),

        ("MOD_CUST_FEEDBACK", "Liên hệ góp ý", "Kiểm tra Gửi phản hồi CSKH trên ContactView.vue", "Phạm Thị Quỳnh Anh", "Khách hàng", "Mở trang Liên hệ", "FDB",
         [("form Liên hệ góp ý", "form liên hệ", "Hiển thị Họ tên, Email, SĐT, Tiêu đề, Nội dung góp ý, Nút Gửi phản hồi")],
         [("Họ và tên", "Họ và tên", "Trần Thị Mai", {"min_len": 2, "max_len": 50}, []),
          ("Email", "Email", "mai.tran@gmail.com", {"min_len": 5, "max_len": 100}, []),
          ("Tiêu đề góp ý", "Tiêu đề góp ý", "Góp ý về dịch vụ bắp nước tại rạp Cầu Giấy", {"min_len": 5, "max_len": 150}, []),
          ("Nội dung chi tiết", "Nội dung chi tiết", "Nhân viên phục vụ rất nhiệt tình và chu đáo, bắp phô mai giòn ngon!", {"min_len": 10, "max_len": 1000}, [])],
         [], []),

        ("MOD_CUST_MY_VOUCHERS", "Voucher của tôi", "Kiểm tra Ví voucher trên CustomerVouchers.vue", "Nguyễn Quang Huy", "Khách hàng", "Mở trang Ví Voucher", "MYV",
         [("danh sách Ví Voucher", "danh sách ví voucher", "Hiển thị các mã khuyến mãi cá nhân, Hạn dùng, Nút 'Dùng ngay', Ô nhập mã kích hoạt")],
         [("Mã Voucher kích hoạt", "Mã Voucher kích hoạt", "SINHNHAT2026", {"min_len": 3, "max_len": 20}, [])],
         [("Trạng thái Voucher", "Chưa sử dụng")], []),

        ("MOD_CUST_BOOKING_HISTORY", "Lịch sử đặt vé", "Kiểm tra Danh sách vé đã mua trên BookingHistoryView.vue", "Nguyễn Quang Huy", "Khách hàng", "Mở trang Lịch sử đặt vé", "HIS",
         [("danh sách Lịch sử đặt vé", "danh sách lịch sử", "Hiển thị Mã đơn, Tên phim, Suất chiếu, Ghế, Tổng tiền, Trạng thái (Đã thanh toán/Đã hủy), Nút 'Xem vé QR'")],
         [("Từ khóa tìm đơn", "Từ khóa tìm đơn", "Avatar", {"min_len": 2, "max_len": 50}, [])],
         [("Trạng thái đơn", "Đã thanh toán")], []),

        ("MOD_POS_TICKETS", "POS Bán vé tại quầy", "Kiểm tra Luồng bán vé trực tiếp tại quầy POS", "Nguyễn Quang Huy", "Nhân viên", "Mở TicketingPOS.vue", "POS",
         [("màn hình POS Bán vé tại quầy", "màn hình POS", "Giao diện cảm ứng chọn phim, suất chiếu, sơ đồ ghế phòng chiếu và in vé")],
         [("Ghi chú đơn hàng", "Ghi chú đơn hàng", "Khách VIP đặt cọc", {"min_len": 2, "max_len": 100}, [])],
         [("Phim", "Avatar: Dòng Chảy Của Nước"), ("Suất chiếu", "19:00 - Phòng 1")],
         [("POS_FUNC_01", "Kiểm tra chức năng Thanh toán tiền mặt tại quầy thành công", "Kiểm tra luồng thu tiền mặt và in vé cứng tại quầy POS", "Bước 1: Nhân viên chọn suất chiếu và chọn 2 ghế VIP\nBước 2: Chọn phương thức 'Tiền mặt', nhập tiền khách đưa 250.000đ\nBước 3: Click button 'Hoàn tất & In vé'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống", "Tổng tiền: 200.000đ\nKhách đưa: 250.000đ\nTiền thừa: 50.000đ", "Hệ thống tính tiền thừa 50.000đ, in vé nhiệt thành công và cập nhật doanh thu bán vé")])
    ]

    raw_dict = {}
    for code, sname, req, tester, role, pre, pfx, guis, flds, flts, funcs in raw_specs:
        tc = build_module_unified_suite(pfx, sname, role, guis, flds, flts, funcs)
        raw_dict[code] = {
            "code": code, "sheet": sname, "req": req,
            "tester": tester, "role": role, "pre": pre,
            "test_cases": tc
        }
    return raw_dict
