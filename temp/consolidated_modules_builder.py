# -*- coding: utf-8 -*-
"""
Self-contained Consolidated 14 Functional Modules Builder for DevCine
- Uses __FEATURE__ for major feature titles (Blue, Accent 1, Lighter 60% - FFBDD7EE)
- Uses __SECTION__ for testing technique titles (Yellow - FFFFFF00)
- 100% DOM-aligned and verified against frontend Vue components & backend APIs
"""

import sys
from detailed_operational_suites import (
    tc_pos_pending, tc_checkin, tc_void, tc_incident, tc_maint
)
from pos_fnb_suite import tc_pos_fnb
from movie_management_suite import full_movie_suite as tc_movies_suite
from sidebar_modules_builder import get_all_raw_modules

sys.stdout.reconfigure(encoding='utf-8')

def build_consolidated_14_modules():
    raw = get_all_raw_modules()
    modules = []

    # =========================================================================
    # NHÓM 1: PHÂN HỆ KHÁCH HÀNG (CLIENT PORTAL) - 4 SHEETS
    # =========================================================================

    # 1. XÁC THỰC & TÀI KHOẢN (Đăng nhập, Quên MK OTP, Đăng ký, Hồ sơ cá nhân & Đổi MK)
    tc_dn = [
        ("__FEATURE__", "ĐĂNG NHẬP & QUÊN MẬT KHẨU"),
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("DN_GUI_01", "Kiểm tra chức năng hiển thị Form đăng nhập thành công",
         "Kiểm tra hiển thị đầy đủ các thành phần trên Form đăng nhập tại LoginView.vue",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\nBước 2: Truy cập vào màn hình Đăng nhập (/login)\nBước 3: Quan sát và kiểm tra hiển thị Form đăng nhập\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị đầy đủ Form đăng nhập gồm: Tab Đăng nhập / Đăng ký, Ô Số điện thoại hoặc Email, Ô Mật khẩu có icon ổ khóa và icon con mắt, Link Quên mật khẩu và Nút Đăng nhập"),

        ("DN_GUI_02", "Kiểm tra chức năng hiển thị và chuyển đổi Icon ẩn/hiện mật khẩu thành công",
         "Kiểm tra hiển thị và tương tác icon con mắt trên ô Mật khẩu",
         "Bước 1: Truy cập vào màn hình Đăng nhập\nBước 2: Nhập mật khẩu 'Khach@123' vào ô Mật khẩu\nBước 3: Click vào icon con mắt bên phải ô mật khẩu\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Mật khẩu: 'Khach@123'", "Icon con mắt chuyển đổi trạng thái hiển thị dạng text rõ hoặc dạng dấu chấm tròn bảo mật (password) kèm icon visibility / visibility_off"),

        ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
        ("DN_EP_01", "Kiểm tra chức năng Đăng nhập bằng Email hợp lệ thành công",
         "Kiểm tra chức năng đăng nhập khi nhập Email đúng định dạng và đúng mật khẩu",
         "Bước 1: Truy cập vào màn hình Đăng nhập\nBước 2: Nhập trường Số điện thoại hoặc Email: 'khachhang@gmail.com'\nBước 3: Nhập trường Mật khẩu: 'Khach@123'\nBước 4: Click button 'Đăng nhập'\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'khachhang@gmail.com'\nMật khẩu: 'Khach@123'", "Đăng nhập thành công, hiển thị Toast 'Đăng nhập thành công! Chào mừng bạn đã trở lại.' và chuyển hướng về Trang chủ"),

        ("DN_EP_02", "Kiểm tra chức năng Đăng nhập bằng Số điện thoại hợp lệ thành công",
         "Kiểm tra chức năng đăng nhập khi nhập Số điện thoại 10 số chuẩn VN và đúng mật khẩu",
         "Bước 1: Truy cập vào màn hình Đăng nhập\nBước 2: Nhập trường Số điện thoại hoặc Email: '0901234567'\nBước 3: Nhập trường Mật khẩu: 'Khach@123'\nBước 4: Click button 'Đăng nhập'\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\nMật khẩu: 'Khach@123'", "Đăng nhập thành công, hiển thị Toast chào mừng và chuyển hướng về Trang chủ"),

        ("DN_EP_03", "Kiểm tra chức năng Đăng nhập bằng Số điện thoại có định dạng quốc tế (+84) thành công",
         "Kiểm tra hàm chuẩn hóa số điện thoại tự động chuyển +84 thành đầu số 0",
         "Bước 1: Nhập trường Số điện thoại hoặc Email: '+84901234567'\nBước 2: Nhập Mật khẩu: 'Khach@123'\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra kết quả",
         "SĐT: '+84901234567'\nMật khẩu: 'Khach@123'", "Hệ thống tự động chuẩn hóa về '0901234567' và đăng nhập thành công"),

        ("DN_EP_04", "Kiểm tra chức năng Đăng nhập thất bại khi Email không đúng định dạng",
         "Kiểm tra hiển thị thông báo lỗi inline khi nhập email sai cú pháp (thiếu @ hoặc domain)",
         "Bước 1: Nhập Email: 'khachhanggmail.com' (thiếu dấu @)\nBước 2: Nhập Mật khẩu: 'Khach@123'\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'khachhanggmail.com'\nMật khẩu: 'Khach@123'", "Viền đỏ ô nhập, hiển thị thông báo lỗi inline: 'Email không đúng định dạng (vd: ten@domain.com).'"),

        ("DN_EP_05", "Kiểm tra chức năng Đăng nhập thất bại khi Số điện thoại không hợp lệ (không đủ 10 số)",
         "Kiểm tra hiển thị thông báo lỗi inline khi số điện thoại không thỏa mãn đầu số VN đủ 10 số",
         "Bước 1: Nhập Số điện thoại: '0901234' (7 chữ số)\nBước 2: Nhập Mật khẩu: 'Khach@123'\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234' (7 chữ số)\nMật khẩu: 'Khach@123'", "Viền đỏ ô nhập, hiển thị thông báo lỗi inline: 'Số điện thoại không hợp lệ (đầu số VN, đủ 10 số).'"),

        ("DN_EP_06", "Kiểm tra chức năng Đăng nhập thất bại khi Số điện thoại chứa ký tự chữ cái",
         "Kiểm tra hiển thị thông báo lỗi inline khi số điện thoại chứa ký tự chữ",
         "Bước 1: Nhập Số điện thoại: '090123abcd'\nBước 2: Nhập Mật khẩu: 'Khach@123'\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '090123abcd'\nMật khẩu: 'Khach@123'", "Viền đỏ ô nhập, hiển thị thông báo lỗi inline: 'Số điện thoại chỉ gồm chữ số.'"),

        ("DN_EP_07", "Kiểm tra chức năng Đăng nhập với tài khoản Quản trị / Nhân sự thành công",
         "Kiểm tra luồng đăng nhập tài khoản có vai trò Quản trị viên / Quản lý / Nhân viên",
         "Bước 1: Nhập Email: 'admin@devcine.vn'\nBước 2: Nhập Mật khẩu: 'Admin@123'\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'admin@devcine.vn'\nMật khẩu: 'Admin@123'", "Đăng nhập thành công, tải quyền RBAC và tự động điều hướng sang khu vực quản trị nội bộ"),

        ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
        ("DN_BVA_01", "Kiểm tra chức năng Đăng nhập khi Số điện thoại có đúng 10 chữ số thành công",
         "Kiểm tra giá trị biên chuẩn của số điện thoại Việt Nam (10 chữ số)",
         "Bước 1: Nhập SĐT đúng 10 chữ số ('0987654321')\nBước 2: Nhập mật khẩu đúng và bấm Đăng nhập",
         "SĐT: '0987654321' (10 chữ số)", "Hệ thống xác thực số điện thoại hợp lệ và gửi request đăng nhập"),

        ("DN_BVA_02", "Kiểm tra chức năng Đăng nhập thất bại khi Số điện thoại có 9 chữ số (cận biên dưới)",
         "Kiểm tra chặn số điện thoại thiếu 1 chữ số",
         "Bước 1: Nhập SĐT có 9 chữ số ('098765432')\nBước 2: Click button 'Đăng nhập'",
         "SĐT: '098765432' (9 chữ số)", "Hiển thị thông báo lỗi inline: 'Số điện thoại không hợp lệ (đầu số VN, đủ 10 số).'"),

        ("DN_BVA_03", "Kiểm tra chức năng Đăng nhập thất bại khi Số điện thoại có 11 chữ số (cận biên trên)",
         "Kiểm tra chặn số điện thoại thừa 1 chữ số",
         "Bước 1: Nhập SĐT có 11 chữ số ('09876543210')\nBước 2: Click button 'Đăng nhập'",
         "SĐT: '09876543210' (11 chữ số)", "Hiển thị thông báo lỗi inline: 'Số điện thoại không hợp lệ (đầu số VN, đủ 10 số).'"),

        ("DN_BVA_04", "Kiểm tra chức năng Đăng nhập bằng Email có độ dài tối thiểu hợp lệ thành công",
         "Kiểm tra địa chỉ email có độ dài ngắn nhất thỏa mãn định dạng email",
         "Bước 1: Nhập Email ngắn nhất hợp lệ: 'a@b.co'\nBước 2: Nhập mật khẩu hợp lệ và bấm Đăng nhập",
         "Email: 'a@b.co'", "Hệ thống chấp nhận định dạng email hợp lệ và gửi xác thực"),

        ("DN_BVA_05", "Kiểm tra chức năng Đăng nhập thất bại khi Email thiếu phần mở rộng domain",
         "Kiểm tra chặn email không có domain đuôi",
         "Bước 1: Nhập Email: 'khachhang@domain'\nBước 2: Click button 'Đăng nhập'",
         "Email: 'khachhang@domain'", "Hiển thị thông báo lỗi inline: 'Email không đúng định dạng (vd: ten@domain.com).'"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("DN_ERR_01", "Kiểm tra chức năng Để trống cả 2 trường SĐT/Email và Mật khẩu (Null) thất bại",
         "Kiểm tra hiển thị cảnh báo lỗi khi bấm Đăng nhập mà không điền thông tin",
         "Bước 1: Mở màn hình Đăng nhập\nBước 2: Để trống cả 2 ô SĐT/Email và Mật khẩu\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT/Email: Null\nMật khẩu: Null", "Viền đỏ cả 2 ô nhập, hiển thị thông báo lỗi inline: 'Vui lòng nhập số điện thoại hoặc email.' và 'Vui lòng nhập mật khẩu.'"),

        ("DN_ERR_02", "Kiểm tra chức năng Để trống trường Mật khẩu thất bại",
         "Kiểm tra hiển thị cảnh báo lỗi khi chỉ điền SĐT/Email mà quên nhập Mật khẩu",
         "Bước 1: Nhập SĐT: '0901234567'\nBước 2: Để trống trường Mật khẩu (Null)\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra kết quả",
         "SĐT: '0901234567'\nMật khẩu: Null", "Viền đỏ ô Mật khẩu, hiển thị thông báo lỗi inline: 'Vui lòng nhập mật khẩu.'"),

        ("DN_ERR_03", "Kiểm tra chức năng Để trống trường SĐT/Email thất bại",
         "Kiểm tra hiển thị cảnh báo lỗi khi chỉ điền Mật khẩu mà để trống SĐT/Email",
         "Bước 1: Để trống ô SĐT/Email (Null)\nBước 2: Nhập Mật khẩu: 'Khach@123'\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra kết quả",
         "SĐT/Email: Null\nMật khẩu: 'Khach@123'", "Viền đỏ ô SĐT/Email, hiển thị thông báo lỗi inline: 'Vui lòng nhập số điện thoại hoặc email.'"),

        ("DN_ERR_04", "Kiểm tra chức năng Nhập trường SĐT/Email toàn khoảng trắng ('   ') thất bại",
         "Kiểm tra hàm trim() phát hiện chuỗi khoảng trắng",
         "Bước 1: Nhập 5 dấu cách vào ô SĐT/Email\nBước 2: Click button 'Đăng nhập'\nBước 3: Kiểm tra kết quả",
         "SĐT/Email: '     '", "Hệ thống cắt tỉa nhận diện rỗng và hiển thị lỗi: 'Vui lòng nhập số điện thoại hoặc email.'"),

        ("DN_ERR_05", "Kiểm tra chức năng Tự động cắt khoảng trắng thừa đầu/cuối của SĐT/Email thành công",
         "Kiểm tra hàm chuẩn hóa cắt khoảng trắng thừa khi người dùng copy-paste",
         "Bước 1: Nhập SĐT có khoảng trắng đầu cuối: '   0901234567   '\nBước 2: Nhập mật khẩu đúng và bấm Đăng nhập",
         "SĐT: '   0901234567   '\nMật khẩu: 'Khach@123'", "Hệ thống tự động cắt khoảng trắng đầu cuối và đăng nhập thành công"),

        ("DN_ERR_06", "Kiểm tra chức năng Tự động loại bỏ khoảng trắng xen giữa các chữ số của SĐT thành công",
         "Kiểm tra chuẩn hóa số điện thoại khi có dấu cách ở giữa",
         "Bước 1: Nhập SĐT có dấu cách: '090 123 4567'\nBước 2: Nhập mật khẩu đúng và bấm Đăng nhập",
         "SĐT: '090 123 4567'", "Hệ thống tự động gộp thành '0901234567' và đăng nhập thành công"),

        ("DN_ERR_07", "Kiểm tra chức năng Nhập Email chứa 2 ký tự @ thất bại",
         "Kiểm tra chặn email lỗi cú pháp 2 dấu @",
         "Bước 1: Nhập Email: 'khachhang@@gmail.com'\nBước 2: Click button 'Đăng nhập'",
         "Email: 'khachhang@@gmail.com'", "Hiển thị thông báo lỗi inline: 'Email không đúng định dạng (vd: ten@domain.com).'"),

        ("DN_ERR_08", "Kiểm tra chức năng Nhập Email chứa ký tự đặc biệt lạ thất bại",
         "Kiểm tra chặn email chứa ký tự không hợp lệ",
         "Bước 1: Nhập Email: 'khach#hang$%@gmail.com'\nBước 2: Click button 'Đăng nhập'",
         "Email: 'khach#hang$%@gmail.com'", "Hiển thị thông báo lỗi inline: 'Email không đúng định dạng (vd: ten@domain.com).'"),

        ("__SECTION__", "KIỂM TRA ĐĂNG NHẬP VỚI CÁC TÀI KHOẢN"),
        ("DN_LOG_01", "Kiểm tra chức năng Đăng nhập thành công khi nhập đúng SĐT và đúng Mật khẩu",
         "Kiểm tra xác thực thành công tài khoản hội viên",
         "Bước 1: Nhập SĐT đúng: '0901234567'\nBước 2: Nhập Mật khẩu đúng: 'Khach@123'\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\nMật khẩu: 'Khach@123'", "Đăng nhập thành công, hiển thị Toast 'Đăng nhập thành công! Chào mừng bạn đã trở lại.' và chuyển hướng về Trang chủ"),

        ("DN_LOG_02", "Kiểm tra chức năng Đăng nhập thành công khi nhập đúng Email và đúng Mật khẩu",
         "Kiểm tra xác thực thành công bằng email",
         "Bước 1: Nhập Email đúng: 'khachhang@gmail.com'\nBước 2: Nhập Mật khẩu đúng: 'Khach@123'\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra kết quả",
         "Email: 'khachhang@gmail.com'\nMật khẩu: 'Khach@123'", "Đăng nhập thành công, chuyển hướng về Trang chủ"),

        ("DN_LOG_03", "Kiểm tra chức năng Đăng nhập thất bại khi nhập đúng SĐT nhưng Sai Mật khẩu",
         "Kiểm tra hệ thống từ chối xác thực khi mật khẩu không trùng khớp",
         "Bước 1: Nhập SĐT đúng: '0901234567'\nBước 2: Nhập Mật khẩu sai: 'SaiMatKhau@123'\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra phản hồi từ hệ thống",
         "SĐT: '0901234567'\nMật khẩu: 'SaiMatKhau@123' (Sai)", "Đăng nhập không thành công, Toast thông báo lỗi: 'Số điện thoại/email hoặc mật khẩu không chính xác.'"),

        ("DN_LOG_04", "Kiểm tra chức năng Đăng nhập thất bại khi nhập SĐT chưa từng đăng ký",
         "Kiểm tra hệ thống từ chối tài khoản không tồn tại (chống dò quét username enumeration)",
         "Bước 1: Nhập SĐT chưa đăng ký: '0999888777'\nBước 2: Nhập Mật khẩu: 'Khach@123'\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra kết quả",
         "SĐT: '0999888777' (Chưa đăng ký)\nMật khẩu: 'Khach@123'", "Đăng nhập không thành công, Toast thông báo lỗi chung: 'Số điện thoại/email hoặc mật khẩu không chính xác.'"),

        ("DN_LOG_05", "Kiểm tra chức năng Đăng nhập thất bại khi nhập Email chưa từng đăng ký",
         "Kiểm tra hệ thống từ chối email lạ",
         "Bước 1: Nhập Email chưa đăng ký: 'chuatungdangky@gmail.com'\nBước 2: Nhập Mật khẩu: 'Khach@123'\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra kết quả",
         "Email: 'chuatungdangky@gmail.com'\nMật khẩu: 'Khach@123'", "Đăng nhập không thành công, Toast thông báo lỗi: 'Số điện thoại/email hoặc mật khẩu không chính xác.'"),

        ("DN_LOG_06", "Kiểm tra chức năng Đăng nhập thất bại khi Tài khoản bị khóa (Trạng thái LOCKED)",
         "Kiểm tra chặn đăng nhập đối với tài khoản hội viên bị Ban quản trị khóa",
         "Bước 1: Nhập SĐT của tài khoản đã bị khóa trong hệ thống: '0909999999'\nBước 2: Nhập Mật khẩu đúng: 'Khach@123'\nBước 3: Click button 'Đăng nhập'\nBước 4: Kiểm tra phản hồi từ hệ thống",
         "SĐT: '0909999999' (LOCKED)\nMật khẩu: 'Khach@123'", "Hệ thống từ chối đăng nhập và thông báo: 'Tài khoản của bạn đã bị khóa. Vui lòng liên hệ CSKH để được hỗ trợ.'"),

        ("DN_LOG_07", "Kiểm tra chức năng Tự động chuyển hướng về Trang trước đó (Redirect URL) sau khi đăng nhập",
         "Kiểm tra lưu giữ tham số redirect khi khách hàng đăng nhập từ trang đặt vé",
         "Bước 1: Khách hàng chọn suất chiếu và được chuyển sang /login?redirect=/booking?scheduleId=123\nBước 2: Nhập tài khoản và bấm Đăng nhập\nBước 3: Kiểm tra trang đích sau đăng nhập",
         "Tham số URL: redirect=/booking?scheduleId=123", "Đăng nhập thành công và tự động chuyển hướng ngay về màn hình /booking?scheduleId=123"),

        ("__SECTION__", "CHỨC NĂNG QUÊN MẬT KHẨU - GIAO DIỆN (GUI)"),
        ("FOR_GUI_01", "Kiểm tra chức năng Mở Wizard Quên mật khẩu 3 bước thành công",
         "Kiểm tra hiển thị giao diện Quên mật khẩu khi click link 'Quên mật khẩu?'",
         "Bước 1: Tại màn hình Đăng nhập, click vào link 'Quên mật khẩu?'\nBước 2: Quan sát giao diện hiển thị\nBước 3: Kiểm tra kết quả",
         "N/A", "Hiển thị wizard Quên mật khẩu 3 bước kèm nút 'arrow_back Quay lại đăng nhập' và thanh chỉ báo tiến trình 3 vạch"),

        ("FOR_GUI_02", "Kiểm tra chức năng hiển thị Bước 1: Nhập email tài khoản thành công",
         "Kiểm tra giao diện Bước 1 của Quên mật khẩu",
         "Bước 1: Quan sát giao diện Bước 1\nBước 2: Kiểm tra các thành phần",
         "N/A", "Hiển thị: Tiêu đề 'Quên mật khẩu', Ô nhập Email tài khoản (icon mail) và Nút 'Gửi mã xác minh'"),

        ("FOR_GUI_03", "Kiểm tra chức năng hiển thị Bước 2: Nhập mã xác minh (OTP) thành công",
         "Kiểm tra giao diện Bước 2 sau khi gửi email",
         "Bước 1: Hoàn tất Bước 1, chuyển sang Bước 2\nBước 2: Quan sát giao diện",
         "N/A", "Hiển thị: Tiêu đề 'Nhập mã xác minh', Ô nhập OTP 6 số căn giữa to rõ, Nút 'Xác minh', Nút 'Đổi email' và Nút 'Gửi lại mã'"),

        ("FOR_GUI_04", "Kiểm tra chức năng hiển thị Bước 3: Đặt mật khẩu mới thành công",
         "Kiểm tra giao diện Bước 3 sau khi xác minh OTP thành công",
         "Bước 1: Xác minh OTP thành công, chuyển sang Bước 3\nBước 2: Quan sát giao diện",
         "N/A", "Hiển thị: Tiêu đề 'Đặt mật khẩu mới', Ô Mật khẩu mới kèm icon con mắt, Ô Xác nhận mật khẩu, Ghi chú '8–32 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt' và Nút 'Đặt lại mật khẩu'"),

        ("__SECTION__", "CHỨC NĂNG QUÊN MẬT KHẨU - PHÂN VÙNG TƯƠNG ĐƯƠNG"),
        ("FOR_EP_01", "Kiểm tra chức năng Gửi mã xác minh OTP qua Email hợp lệ thành công",
         "Kiểm tra gửi OTP khi nhập email có trong hệ thống",
         "Bước 1: Nhập Email: 'khachhang@gmail.com'\nBước 2: Click button 'Gửi mã xác minh'\nBước 3: Kiểm tra kết quả",
         "Email: 'khachhang@gmail.com'", "Hệ thống gửi mã OTP 6 số về hòm thư, hiển thị Toast 'Đã gửi yêu cầu cấp mã xác minh...', bắt đầu đếm ngược 30s và chuyển sang Bước 2"),

        ("FOR_EP_02", "Kiểm tra chức năng Gửi mã xác minh thất bại khi Email sai định dạng",
         "Kiểm tra chặn gửi OTP khi email không đúng định dạng",
         "Bước 1: Nhập Email: 'khachhanggmail.com'\nBước 2: Click button 'Gửi mã xác minh'\nBước 3: Kiểm tra kết quả",
         "Email: 'khachhanggmail.com'", "Toast thông báo lỗi: 'Email không đúng định dạng (vd: ten@domain.com).'"),

        ("FOR_EP_03", "Kiểm tra chức năng Xác minh Mã OTP đúng 6 chữ số thành công",
         "Kiểm tra xác thực khi nhập đúng mã OTP nhận từ email",
         "Bước 1: Nhập mã OTP '123456' vào ô mã xác minh\nBước 2: Click button 'Xác minh'\nBước 3: Kiểm tra kết quả",
         "OTP: '123456'", "Mã xác minh hợp lệ, hệ thống tự động chuyển sang Bước 3 (Đặt mật khẩu mới)"),

        ("FOR_EP_04", "Kiểm tra chức năng Đặt lại Mật khẩu mới thỏa mãn độ phức tạp [8, 32] ký tự thành công",
         "Kiểm tra đặt mật khẩu mới gồm chữ hoa, chữ thường, số và ký tự đặc biệt",
         "Bước 1: Nhập Mật khẩu mới: 'MatKhauMoi@123'\nBước 2: Nhập Xác nhận mật khẩu: 'MatKhauMoi@123'\nBước 3: Click button 'Đặt lại mật khẩu'\nBước 4: Kiểm tra kết quả",
         "MK mới: 'MatKhauMoi@123'\nXác nhận: 'MatKhauMoi@123'", "Đặt lại mật khẩu thành công, hiển thị Toast 'Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.', quay về form đăng nhập với email đã điền sẵn"),

        ("FOR_EP_05", "Kiểm tra chức năng Đặt lại Mật khẩu thất bại khi Mật khẩu mới không đạt độ phức tạp",
         "Kiểm tra chặn khi mật khẩu mới chỉ có chữ hoặc chỉ có số mà thiếu ký tự đặc biệt",
         "Bước 1: Nhập Mật khẩu mới: '12345678' (thiếu chữ hoa, chữ thường, ký tự đặc biệt)\nBước 2: Click button 'Đặt lại mật khẩu'\nBước 3: Kiểm tra kết quả",
         "MK mới: '12345678'", "Toast thông báo lỗi: 'Mật khẩu 8–32 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.'"),

        ("FOR_EP_06", "Kiểm tra chức năng Đặt lại Mật khẩu thất bại khi Mật khẩu xác nhận không khớp",
         "Kiểm tra chặn khi 2 ô mật khẩu mới và xác nhận khác nhau",
         "Bước 1: Nhập MK mới: 'MatKhau@123', Xác nhận MK: 'KhacNhau@123'\nBước 2: Click button 'Đặt lại mật khẩu'\nBước 3: Kiểm tra kết quả",
         "MK mới: 'MatKhau@123'\nXác nhận: 'KhacNhau@123'", "Toast thông báo lỗi: 'Mật khẩu xác nhận không khớp.'"),

        ("__SECTION__", "CHỨC NĂNG QUÊN MẬT KHẨU - PHÂN TÍCH GIÁ TRỊ BIÊN"),
        ("FOR_BVA_01", "Kiểm tra chức năng Đặt Mật khẩu mới ở giá trị biên min 8 ký tự thành công",
         "Kiểm tra đặt mật khẩu mới với độ dài ngắn nhất hợp lệ (8 ký tự)",
         "Bước 1: Nhập Mật khẩu mới đúng 8 ký tự đủ 4 thành phần: 'Dev@2026'\nBước 2: Xác nhận và bấm Đặt lại mật khẩu",
         "MK mới: 'Dev@2026' (8 ký tự)", "Hệ thống chấp nhận mật khẩu 8 ký tự hợp lệ"),

        ("FOR_BVA_02", "Kiểm tra chức năng Đặt Mật khẩu mới ở cận biên dưới 7 ký tự thất bại",
         "Kiểm tra chặn mật khẩu mới có 7 ký tự",
         "Bước 1: Nhập Mật khẩu mới 7 ký tự: 'Dev@202'\nBước 2: Click button 'Đặt lại mật khẩu'",
         "MK mới: 'Dev@202' (7 ký tự)", "Toast thông báo lỗi: 'Mật khẩu 8–32 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.'"),

        ("FOR_BVA_03", "Kiểm tra chức năng Đặt Mật khẩu mới ở giá trị biên max 32 ký tự thành công",
         "Kiểm tra đặt mật khẩu mới với độ dài dài nhất hợp lệ (32 ký tự)",
         "Bước 1: Nhập Mật khẩu mới đúng 32 ký tự đủ thành phần\nBước 2: Xác nhận và bấm Đặt lại mật khẩu",
         "MK mới: (Chuỗi 32 ký tự)", "Hệ thống chấp nhận mật khẩu 32 ký tự hợp lệ"),

        ("FOR_BVA_04", "Kiểm tra chức năng Nhập Mã OTP thiếu số (< 6 số) thất bại",
         "Kiểm tra chặn khi nhập OTP chỉ có 5 số",
         "Bước 1: Nhập OTP '12345' (5 số)\nBước 2: Click button 'Xác minh'",
         "OTP: '12345' (5 chữ số)", "Toast thông báo lỗi: 'Mã xác minh gồm 6 chữ số.'"),

        ("__SECTION__", "CHỨC NĂNG QUÊN MẬT KHẨU - ĐOÁN LỖI"),
        ("FOR_ERR_01", "Kiểm tra chức năng Để trống Email nhận OTP (Null) thất bại",
         "Kiểm tra nút Gửi mã khi chưa điền email",
         "Bước 1: Để trống trường Email (Null)\nBước 2: Click button 'Gửi mã xác minh'\nBước 3: Kiểm tra kết quả",
         "Email: Null", "Toast thông báo lỗi: 'Email không đúng định dạng (vd: ten@domain.com).'"),

        ("FOR_ERR_02", "Kiểm tra chức năng Nhập sai mã OTP thất bại",
         "Kiểm tra hệ thống từ chối khi nhập mã OTP không trùng khớp hoặc đã hết hạn",
         "Bước 1: Nhập mã OTP sai '999999'\nBước 2: Click button 'Xác minh'\nBước 3: Kiểm tra kết quả",
         "OTP: '999999' (Sai)", "Toast thông báo lỗi: 'Mã xác minh không đúng hoặc đã hết hạn.'"),

        ("FOR_ERR_03", "Kiểm tra chức năng Tự động lọc bỏ ký tự chữ khi nhập ô Mã OTP thành công",
         "Kiểm tra hàm replace(/\D/g, '') chỉ cho phép nhập chữ số vào ô OTP",
         "Bước 1: Gõ các chữ cái 'abc' vào ô OTP\nBước 2: Quan sát giá trị hiển thị trong ô",
         "Ký tự gõ: 'abc123xyz'", "Hệ thống tự động loại bỏ các chữ cái, chỉ giữ lại '123'"),

        ("FOR_ERR_04", "Kiểm tra chức năng Cơ chế Cooldown 30s chống Spam Gửi lại OTP thành công",
         "Kiểm tra nút Gửi lại mã bị khóa đếm ngược 30 giây",
         "Bước 1: Sau khi gửi OTP ở Bước 1, quan sát nút 'Gửi lại sau (30s)' ở Bước 2\nBước 2: Cố tình click vào nút khi đang đếm ngược",
         "Trạng thái: Đang trong 30s Cooldown", "Nút bị vô hiệu hóa (disabled), không thể spam gửi lại OTP liên tục"),

        ("FOR_ERR_05", "Kiểm tra chức năng Nút 'Quay lại đăng nhập' đóng wizard thành công",
         "Kiểm tra người dùng hủy quy trình quên mật khẩu",
         "Bước 1: Đang ở Bước 2 nhập OTP\nBước 2: Click button 'arrow_back Quay lại đăng nhập'\nBước 3: Kiểm tra kết quả",
         "Thao tác: Click Quay lại", "Wizard đóng lại, quay về Form đăng nhập ban đầu"),

        ("__SECTION__", "CHỨC NĂNG QUÊN MẬT KHẨU - LUỒNG NGHIỆP VỤ"),
        ("FOR_FUNC_01", "Kiểm tra chức năng Hoàn tất Đặt lại mật khẩu và Đăng nhập bằng mật khẩu mới thành công",
         "Kiểm tra luồng hoàn tất đổi mật khẩu và đăng nhập lại ngay lập tức",
         "Bước 1: Hoàn thành cả 3 bước Quên mật khẩu với mật khẩu mới 'DevCine@2026'\nBước 2: Form đăng nhập tự động mở với email đã điền sẵn\nBước 3: Nhập mật khẩu mới 'DevCine@2026' và bấm Đăng nhập\nBước 4: Kiểm tra kết quả",
         "Mật khẩu mới: 'DevCine@2026'", "Đăng nhập thành công bằng mật khẩu mới hoàn toàn trơn tru")
    ]

    # 2. ĐĂNG KÝ TÀI KHOẢN
    tc_reg = [
        ("__FEATURE__", "ĐĂNG KÝ TÀI KHOẢN"),
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("REG_GUI_01", "Kiểm tra chức năng hiển thị Form Đăng ký tài khoản thành công",
         "Kiểm tra hiển thị đầy đủ các trường nhập trên Form đăng ký tại LoginView.vue",
         "Bước 1: Truy cập vào màn hình /login\nBước 2: Click vào Tab 'ĐĂNG KÝ'\nBước 3: Quan sát và kiểm tra hiển thị Form đăng ký\nBước 4: Kiểm tra kết quả",
         "N/A", "Hiển thị đầy đủ Form đăng ký gồm: Họ và tên (icon person), Email của bạn (icon mail), Số điện thoại (icon call), Mật khẩu (icon lock & con mắt), Ghi chú '8–32 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt' và Nút 'ĐĂNG KÝ >'"),

        ("REG_GUI_02", "Kiểm tra chức năng hiển thị Nút Đăng ký ở trạng thái mờ (disabled) khi form chưa hợp lệ",
         "Kiểm tra ràng buộc nút Đăng ký bị vô hiệu hóa khi chưa điền đủ hoặc dữ liệu chưa hợp lệ",
         "Bước 1: Mở Tab Đăng ký\nBước 2: Quan sát trạng thái nút 'ĐĂNG KÝ >'\nBước 3: Kiểm tra kết quả",
         "Trạng thái: Form trống", "Nút 'ĐĂNG KÝ >' ở trạng thái mờ (disabled: opacity-50, cursor-not-allowed), không thể click submit"),

        ("REG_GUI_03", "Kiểm tra chức năng Tự động kích hoạt Nút Đăng ký (enabled) khi nhập đầy đủ thông tin hợp lệ",
         "Kiểm tra tính năng computed isRegisterValid kích hoạt nút đăng ký theo thời gian thực",
         "Bước 1: Nhập Họ tên 'Nguyễn Văn Dân', Email 'khachhang@gmail.com', SĐT '0901234567', Mật khẩu 'Khach@123'\nBước 2: Quan sát nút Đăng ký",
         "Dữ liệu: 4 trường đều hợp lệ", "Nút 'ĐĂNG KÝ >' sáng màu vàng (enabled, cursor-pointer), sẵn sàng thực hiện đăng ký"),

        ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
        ("REG_EP_01", "Kiểm tra chức năng Nhập Họ và tên hợp lệ [2, 50] ký tự thành công",
         "Kiểm tra trường Họ tên khi nhập chuỗi chữ cái có dấu và khoảng trắng",
         "Bước 1: Nhập Họ và tên: 'Nguyễn Văn Dân' (13 ký tự)\nBước 2: Kiểm tra phản hồi",
         "Họ tên: 'Nguyễn Văn Dân'", "Hệ thống ghi nhận hợp lệ, không hiển thị lỗi"),

        ("REG_EP_02", "Kiểm tra chức năng Nhập Họ và tên thất bại khi chứa chữ số hoặc ký tự đặc biệt",
         "Kiểm tra chặn họ tên chứa ký tự không phải chữ cái",
         "Bước 1: Nhập Họ tên: 'Nguyen Van 123@'\nBước 2: Click ra ngoài ô nhập (blur)\nBước 3: Kiểm tra kết quả",
         "Họ tên: 'Nguyen Van 123@'", "Viền đỏ ô nhập, hiển thị thông báo lỗi inline: 'Họ tên chỉ gồm chữ cái và khoảng trắng.'"),

        ("REG_EP_03", "Kiểm tra chức năng Nhập Email hợp lệ thành công",
         "Kiểm tra trường Email khi nhập đúng định dạng",
         "Bước 1: Nhập Email: 'khachhang@gmail.com'\nBước 2: Kiểm tra phản hồi",
         "Email: 'khachhang@gmail.com'", "Hệ thống ghi nhận hợp lệ, không hiển thị lỗi"),

        ("REG_EP_04", "Kiểm tra chức năng Nhập Email thất bại khi sai định dạng",
         "Kiểm tra chặn email thiếu domain",
         "Bước 1: Nhập Email: 'khachhang@'\nBước 2: Click ra ngoài (blur)\nBước 3: Kiểm tra kết quả",
         "Email: 'khachhang@'", "Viền đỏ, hiển thị thông báo lỗi inline: 'Email không đúng định dạng (vd: ten@domain.com).'"),

        ("REG_EP_05", "Kiểm tra chức năng Nhập Số điện thoại hợp lệ chuẩn VN 10 chữ số thành công",
         "Kiểm tra trường Số điện thoại với đầu số hợp lệ (03, 05, 07, 08, 09)",
         "Bước 1: Nhập SĐT: '0901234567'\nBước 2: Kiểm tra phản hồi",
         "SĐT: '0901234567'", "Hệ thống ghi nhận SĐT hợp lệ"),

        ("REG_EP_06", "Kiểm tra chức năng Nhập Số điện thoại thất bại khi sai đầu số hoặc không đủ 10 số",
         "Kiểm tra chặn số điện thoại không hợp lệ",
         "Bước 1: Nhập SĐT: '0123456789' (đầu số 01 không thuộc VN)\nBước 2: Click ra ngoài (blur)\nBước 3: Kiểm tra kết quả",
         "SĐT: '0123456789'", "Viền đỏ, hiển thị thông báo lỗi inline: 'Số điện thoại không hợp lệ (đầu số VN, đủ 10 số).'"),

        ("REG_EP_07", "Kiểm tra chức năng Nhập Mật khẩu mạnh [8, 32] ký tự thành công",
         "Kiểm tra trường Mật khẩu khi thỏa mãn đủ 4 thành phần (chữ hoa, chữ thường, số, ký tự đặc biệt)",
         "Bước 1: Nhập Mật khẩu: 'Khach@123'\nBước 2: Kiểm tra phản hồi",
         "Mật khẩu: 'Khach@123'", "Hệ thống ghi nhận mật khẩu hợp lệ"),

        ("REG_EP_08", "Kiểm tra chức năng Nhập Mật khẩu thất bại khi thiếu ký tự đặc biệt hoặc chữ hoa",
         "Kiểm tra chặn mật khẩu yếu khi đăng ký",
         "Bước 1: Nhập Mật khẩu: 'khachhang123' (thiếu chữ hoa và ký tự đặc biệt)\nBước 2: Click ra ngoài (blur)\nBước 3: Kiểm tra kết quả",
         "Mật khẩu: 'khachhang123'", "Viền đỏ, hiển thị thông báo lỗi inline: 'Mật khẩu 8–32 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.'"),

        ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
        ("REG_BVA_01", "Kiểm tra chức năng Nhập Họ và tên ở giá trị biên min (2 ký tự) thành công",
         "Kiểm tra họ tên ngắn nhất hợp lệ",
         "Bước 1: Nhập Họ và tên: 'Lê' (2 ký tự)\nBước 2: Kiểm tra kết quả",
         "Họ tên: 'Lê' (2 ký tự)", "Hệ thống chấp nhận họ tên 2 ký tự"),

        ("REG_BVA_02", "Kiểm tra chức năng Nhập Họ và tên ở cận biên dưới (1 ký tự) thất bại",
         "Kiểm tra chặn họ tên chỉ có 1 ký tự",
         "Bước 1: Nhập Họ và tên: 'A' (1 ký tự)\nBước 2: Click ra ngoài (blur)",
         "Họ tên: 'A' (1 ký tự)", "Hiển thị thông báo lỗi inline: 'Họ tên từ 2 đến 50 ký tự.'"),

        ("REG_BVA_03", "Kiểm tra chức năng Nhập Họ và tên ở giá trị biên max (50 ký tự) thành công",
         "Kiểm tra họ tên dài nhất hợp lệ",
         "Bước 1: Nhập Họ và tên đúng 50 ký tự chữ cái\nBước 2: Kiểm tra kết quả",
         "Họ tên: (Chuỗi 50 ký tự)", "Hệ thống chấp nhận họ tên 50 ký tự"),

        ("REG_BVA_04", "Kiểm tra chức năng Nhập Họ và tên ở cận biên trên (51 ký tự) thất bại",
         "Kiểm tra chặn họ tên vượt quá 50 ký tự",
         "Bước 1: Nhập Họ và tên dài 51 ký tự\nBước 2: Click ra ngoài (blur)",
         "Họ tên: (Chuỗi 51 ký tự)", "Hiển thị thông báo lỗi inline: 'Họ tên từ 2 đến 50 ký tự.'"),

        ("REG_BVA_05", "Kiểm tra chức năng Nhập Mật khẩu ở giá trị biên min (8 ký tự) thành công",
         "Kiểm tra mật khẩu đăng ký ngắn nhất hợp lệ",
         "Bước 1: Nhập Mật khẩu đúng 8 ký tự đủ 4 thành phần: 'Dev@2026'\nBước 2: Kiểm tra kết quả",
         "Mật khẩu: 'Dev@2026' (8 ký tự)", "Hệ thống chấp nhận mật khẩu 8 ký tự"),

        ("REG_BVA_06", "Kiểm tra chức năng Nhập Mật khẩu ở cận biên dưới (7 ký tự) thất bại",
         "Kiểm tra chặn mật khẩu đăng ký 7 ký tự",
         "Bước 1: Nhập Mật khẩu 7 ký tự: 'Dev@202'\nBước 2: Click ra ngoài (blur)",
         "Mật khẩu: 'Dev@202' (7 ký tự)", "Hiển thị thông báo lỗi inline: 'Mật khẩu 8–32 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.'"),

        ("REG_BVA_07", "Kiểm tra chức năng Nhập Mật khẩu ở giá trị biên max (32 ký tự) thành công",
         "Kiểm tra mật khẩu đăng ký dài nhất hợp lệ",
         "Bước 1: Nhập Mật khẩu đúng 32 ký tự đủ thành phần\nBước 2: Kiểm tra kết quả",
         "Mật khẩu: (Chuỗi 32 ký tự)", "Hệ thống chấp nhận mật khẩu 32 ký tự"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("REG_ERR_01", "Kiểm tra chức năng Click vào ô rồi rời đi mà không nhập (Touched blur) thất bại",
         "Kiểm tra hiển thị thông báo bắt buộc nhập khi người dùng chạm vào ô rồi bỏ trống",
         "Bước 1: Click vào từng ô Họ tên, Email, SĐT, Mật khẩu rồi click ra ngoài\nBước 2: Kiểm tra hiển thị lỗi inline",
         "Trạng thái: Touched = true, Value = ''", "Hiển thị đồng thời 4 thông báo lỗi inline: 'Vui lòng nhập họ và tên.', 'Vui lòng nhập email.', 'Vui lòng nhập số điện thoại.', 'Vui lòng nhập mật khẩu.'"),

        ("REG_ERR_02", "Kiểm tra chức năng Tự động chặn nhập ký tự chữ vào ô Số điện thoại thành công",
         "Kiểm tra hàm onPhoneInput lọc bỏ ký tự không phải số",
         "Bước 1: Gõ các chữ cái 'abc' vào ô Số điện thoại\nBước 2: Kiểm tra giá trị xuất hiện trong ô",
         "Ký tự gõ: '090abc123'", "Hệ thống tự động loại bỏ chữ 'abc', chỉ hiển thị '090123'"),

        ("REG_ERR_03", "Kiểm tra chức năng Đăng ký thất bại khi Số điện thoại đã tồn tại trong hệ thống",
         "Kiểm tra xử lý trùng số điện thoại từ Backend API",
         "Bước 1: Nhập Số điện thoại '0901234567' đã có tài khoản trong hệ thống\nBước 2: Nhập các trường còn lại hợp lệ và bấm Đăng ký\nBước 3: Kiểm tra phản hồi từ hệ thống",
         "SĐT: '0901234567' (Đã tồn tại)", "Đăng ký thất bại, hiển thị Toast thông báo lỗi: 'Số điện thoại này đã được đăng ký trong hệ thống.'"),

        ("REG_ERR_04", "Kiểm tra chức năng Đăng ký thất bại khi Email đã tồn tại trong hệ thống",
         "Kiểm tra xử lý trùng email từ Backend API",
         "Bước 1: Nhập Email 'khachhang@gmail.com' đã có tài khoản trong hệ thống\nBước 2: Nhập các trường còn lại hợp lệ và bấm Đăng ký\nBước 3: Kiểm tra phản hồi từ hệ thống",
         "Email: 'khachhang@gmail.com' (Đã tồn tại)", "Đăng ký thất bại, hiển thị Toast thông báo lỗi: 'Email này đã được sử dụng.'"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("REG_FUNC_01", "Kiểm tra chức năng Đăng ký tài khoản mới và Tự động đăng nhập thành công",
         "Kiểm tra luồng đăng ký tài khoản mới hoàn chỉnh từ Client lên Server",
         "Bước 1: Nhập Họ tên 'Trần Thị Thu Trang', Email 'trang.tran@gmail.com', SĐT '0988112233', Mật khẩu 'Trang@2026'\nBước 2: Click button 'Đăng ký'\nBước 3: Kiểm tra kết quả xử lý từ hệ thống",
         "Họ tên: 'Trần Thị Thu Trang'\nEmail: 'trang.tran@gmail.com'\nSĐT: '0988112233'\nMK: 'Trang@2026'", "Tạo tài khoản thành công, tự động đăng nhập, hiển thị Toast 'Đăng ký thành công! Chào mừng bạn đến với DevCine.' và chuyển hướng về Trang chủ")
    ]

    # 3. HỒ SƠ CÁ NHÂN & ĐỔI MẬT KHẨU
    tc_profile = [("__FEATURE__", "HỒ SƠ & ĐỔI MẬT KHẨU")] + raw["MOD_CUST_PROFILE"]["test_cases"] + raw["MOD_CUST_CHANGE_PASS"]["test_cases"]

    modules.append({
        "code": "MOD_AUTH_ACCOUNT", "sheet": "Xác thực & Tài khoản",
        "req": "Kiểm tra Đăng nhập, Khôi phục mật khẩu OTP, Đăng ký tài khoản mới, Cập nhật thông tin cá nhân và Đổi mật khẩu",
        "tester": "Phạm Thị Quỳnh Anh", "role": "Khách hàng & Khách vãng lai",
        "pre": "Người dùng mở trang Đăng nhập / Đăng ký / Hồ sơ cá nhân trên hệ thống DevCine",
        "test_cases": tc_dn + tc_reg + tc_profile
    })

    # =========================================================================
    # CÁC PHÂN HỆ CÒN LẠI (2 ĐẾN 14)
    # =========================================================================

    # 2. ĐẶT VÉ TRỰC TUYẾN (ONLINE BOOKING)
    tc_seat_cases = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("ST_GUI_01", "Kiểm tra chức năng hiển thị Khối Loại vé thành công",
         "Kiểm tra hiển thị Bộ chọn số lượng vé Người lớn & U22 / HSSV",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\nBước 2: Truy cập vào màn hình Booking Bước 1 (Chọn ghế)\nBước 3: Quan sát khối Loại vé\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị đầy đủ 2 nhóm vé 'Người lớn' và 'U22 / HSSV' kèm nút [-]/[+] và nhãn '8 vé • đã chọn 0 ghế'"),

        ("ST_GUI_02", "Kiểm tra chức năng hiển thị Thanh chọn nhanh cụm ghế liền nhau (Block Selector) thành công",
         "Kiểm tra hiển thị các nút Block Selector",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\nBước 2: Truy cập vào màn hình Booking Bước 1 (Chọn ghế)\nBước 3: Quan sát thanh Block Selector\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị các nút: '1 ghế', '2 ghế liền', '3 ghế liền', '4 ghế liền', '5 ghế liền'"),

        ("ST_GUI_03", "Kiểm tra chức năng hiển thị Sơ đồ ma trận ghế và Màn hình chiếu thành công",
         "Kiểm tra hiển thị Sơ đồ ma trận ghế chuẩn phòng chiếu",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\nBước 2: Truy cập vào màn hình Booking Bước 1 (Chọn ghế)\nBước 3: Quan sát dải phát sáng MÀN HÌNH / SCREEN và ma trận ghế\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị dải phát sáng MÀN HÌNH, ma trận ghế phân loại rõ: Ghế Thường (xám), Ghế VIP (cam/vàng), Ghế Sweetbox đôi (hồng), Ghế đã bán (X xám), Ghế bảo trì (X gạch chéo)"),

        ("ST_GUI_04", "Kiểm tra chức năng hiển thị Cột tóm tắt thông tin đặt vé thành công",
         "Kiểm tra hiển thị Cột tóm tắt bên phải",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\nBước 2: Truy cập vào màn hình Booking Bước 1 (Chọn ghế)\nBước 3: Quan sát cột tóm tắt thông tin\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Poster phim, Tên phim, Suất chiếu, Phòng chiếu, Danh sách ghế đang chọn, Tạm tính tổng tiền và nút 'TIẾP TỤC ➔'"),

        ("ST_GUI_05", "Kiểm tra chức năng hiển thị Đồng hồ đếm ngược giữ chỗ 10 phút thành công",
         "Kiểm tra hiển thị Đồng hồ đếm ngược giữ chỗ",
         "Bước 1: Khách hàng click chọn ghế trên sơ đồ\nBước 2: Quan sát góc trên màn hình\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị đồng hồ đếm ngược 10:00 giảm dần theo thời gian thực (10:00 -> 09:59...)"),

        ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
        ("ST_EP_01", "Kiểm tra chức năng Chọn số lượng vé thành công",
         "Kiểm tra chức năng Chọn số lượng vé khi tổng số vé trong khoảng từ 1 đến 8 vé",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\nBước 2: Click nút [+] tại mục Người lớn 2 lần, mục U22 1 lần (Tổng 3 vé)\nBước 3: Click chọn 3 ghế trên sơ đồ ma trận\nBước 4: Click button 'TIẾP TỤC ➔'\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "Loại vé: 2 Người lớn, 1 U22 (Tổng 3 vé)\nGhế chọn: E05, E06, E07", "Hệ thống ghi nhận 3 vé hợp lệ, tính đúng tổng tiền và cho phép chuyển sang bước Combo F&B"),

        ("ST_EP_02", "Kiểm tra chức năng Chọn ghế thất bại khi số lượng vé là 0 vé",
         "Kiểm tra chức năng Chọn ghế khi chưa chọn số lượng vé",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\nBước 2: Không tăng số lượng vé (Số vé = 0)\nBước 3: Cố tình click trực tiếp vào ghế F05 trên sơ đồ\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng vé: 0 vé\nThao tác: Click ghế F05", "Hiển thị thông báo hướng dẫn: 'Vui lòng chọn số lượng vé trước khi chọn vị trí ghế'"),

        ("ST_EP_03", "Kiểm tra chức năng Tăng số lượng vé thất bại khi vượt quá 8 vé",
         "Kiểm tra giới hạn tối đa 8 vé cho một giao dịch",
         "Bước 1: Click nút [+] tăng số vé lên 8 vé\nBước 2: Cố tình click tiếp nút [+] lần thứ 9\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số vé hiện có: 8 vé\nThao tác: Click nút [+]", "Nút [+] bị vô hiệu hóa (disabled), hiển thị cảnh báo: 'Mỗi giao dịch chỉ được chọn tối đa 8 vé'"),

        ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
        ("ST_BVA_01", "Kiểm tra chức năng Chọn số lượng vé ở giá trị biên min (1 vé) thành công",
         "Kiểm tra chọn 1 vé Người lớn",
         "Bước 1: Click nút [+] chọn 1 vé Người lớn\nBước 2: Click chọn 1 ghế E05\nBước 3: Click button 'TIẾP TỤC ➔'",
         "Số lượng: 1 vé Người lớn\nGhế chọn: E05", "Hệ thống chấp nhận 1 vé và cho phép chuyển tiếp"),

        ("ST_BVA_02", "Kiểm tra chức năng Chọn số lượng vé ở giá trị biên max (8 vé) thành công",
         "Kiểm tra chọn 8 vé đạt mức tối đa",
         "Bước 1: Tăng số lượng vé lên 8 vé\nBước 2: Click chọn 8 ghế liền nhau\nBước 3: Kiểm tra kết quả",
         "Số lượng: 8 vé", "Hệ thống chấp nhận 8 vé tối đa"),

        ("ST_BVA_03", "Kiểm tra chức năng Giảm số lượng vé ở cận biên dưới (0 vé) thất bại",
         "Kiểm tra nút [-] bị khóa khi số lượng vé là 0",
         "Bước 1: Số vé đang ở mức 0\nBước 2: Quan sát và click nút [-]",
         "Số lượng: 0 vé\nThao tác: Click nút [-]", "Nút [-] bị vô hiệu hóa (disabled), không thể giảm xuống số âm"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("ST_ERR_01", "Kiểm tra chức năng Chuyển bước thất bại khi chọn thiếu số lượng ghế",
         "Kiểm tra nút Tiếp tục bị khóa khi số ghế đã chọn ít hơn số vé đã khai báo",
         "Bước 1: Chọn 3 vé nhưng chỉ click chọn 2 ghế trên sơ đồ\nBước 2: Click button 'TIẾP TỤC ➔'",
         "Số vé khai báo: 3 vé\nSố ghế đã chọn: 2 ghế", "Nút 'TIẾP TỤC ➔' bị vô hiệu hóa, thông báo: 'Bạn cần chọn đủ 3 ghế tương ứng với 3 vé'"),

        ("ST_ERR_02", "Kiểm tra chức năng Bỏ chọn ghế (Deselect) thành công",
         "Kiểm tra click lại vào ghế đang chọn để trả về trạng thái trống",
         "Bước 1: Click vào ghế E05 đang được chọn\nBước 2: Kiểm tra trạng thái ghế",
         "Vị trí: E05 (Đang chọn)\nThao tác: Click lại E05", "Ghế E05 chuyển về trạng thái trống (Available) và trừ tiền khỏi cột tóm tắt"),

        ("ST_ERR_03", "Kiểm tra chức năng Chọn ghế thất bại khi click vào ghế đã bán (SOLD)",
         "Kiểm tra chặn chọn ghế đã có người mua",
         "Bước 1: Click vào ghế có ký hiệu 'X' màu xám đậm\nBước 2: Kiểm tra kết quả",
         "Vị trí: E08 (SOLD)", "Không có phản hồi chọn ghế, con trỏ chuột hiển thị 'not-allowed'"),

        ("ST_ERR_04", "Kiểm tra chức năng Chọn ghế thất bại khi click vào ghế bảo trì (MAINTENANCE)",
         "Kiểm tra chặn chọn ghế đang bảo trì vật lý",
         "Bước 1: Click vào ghế có ký hiệu cờ lê / bảo trì\nBước 2: Kiểm tra kết quả",
         "Vị trí: B03 (MAINTENANCE)", "Không cho phép chọn, thông báo ghế đang trong quá trình bảo trì kỹ thuật"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("ST_FUNC_01", "Kiểm tra chức năng Chọn ghế VIP có phụ thu giá vé thành công",
         "Kiểm tra chọn ghế VIP tại hàng trung tâm và tính phụ thu",
         "Bước 1: Chọn 1 vé Người lớn, click chọn ghế C05 (VIP)\nBước 2: Kiểm tra cột tóm tắt",
         "Loại ghế: Ghế VIP\nVị trí: C05", "Ghế đổi sang màu xanh sáng, cột tóm tắt ghi nhận 'C05 - Ghế VIP' kèm phụ thu VIP chính xác"),

        ("ST_FUNC_02", "Kiểm tra chức năng Chọn ghế Sweetbox đôi tự động chọn cả cặp (2 chỗ) thành công",
         "Kiểm tra click 1 ghế Sweetbox tự động gộp 2 chỗ",
         "Bước 1: Chọn 2 vé, click vào 1 ô của ghế Sweetbox đôi H01\nBước 2: Kiểm tra sơ đồ",
         "Loại ghế: SWEETBOX\nVị trí: H01", "Hệ thống tự động chọn cả cặp đôi ghế H01-H02 (2 chỗ) đồng thời"),

        ("ST_FUNC_03", "Kiểm tra quy tắc Orphan Seat Rule chặn để lại 1 ghế trống cô lập thành công",
         "Kiểm tra quy tắc không được để lại 1 ghế trống đơn lẻ ở đầu dãy hoặc giữa các ghế",
         "Bước 1: Chọn 2 vé, click chọn ghế E02, E03 (để lại duy nhất E01 ở đầu hàng)\nBước 2: Click button 'TIẾP TỤC ➔'",
         "Ghế chọn: E02, E03\nGhế trống cô lập: E01", "Hệ thống chặn chuyển bước và cảnh báo: 'Vui lòng không để trống 1 ghế đơn lẻ bên cạnh hoặc sát lối đi'"),

        ("ST_FUNC_04", "Kiểm tra cơ chế Concurrent Booking xử lý xung đột 2 người cùng chọn 1 ghế",
         "Kiểm tra khóa ghế Redis khi 2 người cùng click 1 ghế trong cùng 1 giây",
         "Bước 1: Người A và Người B cùng click chọn ghế F08 tại cùng thời điểm\nBước 2: Kiểm tra phản hồi",
         "Người A: Click trước (t=0ms)\nNgười B: Click sau (t=10ms)", "Người A được cấp Lock giữ ghế; Người B bị Redis Lock từ chối ngay lập tức kèm thông báo: 'Ghế F08 vừa được chọn hoặc đã được bán ở nơi khác'"),

        ("ST_FUNC_05", "Kiểm tra chức năng Tự động hủy giữ chỗ khi hết hạn đồng hồ đếm ngược 10 phút",
         "Kiểm tra xử lý Timeout khi hết 10 phút giữ chỗ",
         "Bước 1: Chọn ghế và giữ nguyên màn hình quá 10 phút (00:00)\nBước 2: Kiểm tra kết quả xử lý từ hệ thống",
         "Thời gian giữ chỗ: > 10 phút (Timeout 00:00)", "Hệ thống tự động nhả khóa ghế real-time, xóa sạch giỏ hàng tạm, đưa về bước 1 và thông báo: 'Đã hết thời gian giữ chỗ (10 phút). Vui lòng chọn lại ghế.'")
    ]

    tc_combo_cases = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("FNB_GUI_01", "Kiểm tra chức năng hiển thị Danh sách Combo F&B thành công",
         "Kiểm tra hiển thị Danh sách Combo F&B tại Booking Bước 2",
         "Bước 1: Hoàn tất bước chọn ghế và chuyển sang bước 2 (Combo)\nBước 2: Quan sát danh sách bắp nước\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị lưới các card món: Ảnh combo, Tên combo, Mô tả thành phần, Đơn giá và nút [-] / [+] số lượng"),

        ("FNB_GUI_02", "Kiểm tra chức năng hiển thị Modal tùy chọn FnbOptionModal thành công",
         "Kiểm tra hiển thị Popup tùy chọn vị bắp và loại nước ngọt",
         "Bước 1: Click nút [+] tại gói 'Couple Combo'\nBước 2: Kiểm tra popup hiển thị",
         "N/A", "Hiển thị Modal FnbOptionModal cho phép chọn Vị bắp (Ngọt, Phô mai +15k, Trứng muối +15k) và Loại nước (Coca, Sprite, Fanta)"),

        ("FNB_GUI_03", "Kiểm tra chức năng hiển thị Nút Bỏ qua & Tiếp tục thành công",
         "Kiểm tra hiển thị nút Bỏ qua khi khách không có nhu cầu mua F&B",
         "Bước 1: Quan sát thanh điều hướng phía dưới\nBước 2: Kiểm tra nút tác vụ",
         "N/A", "Hiển thị nút 'TIẾP TỤC ➔' cho phép bỏ qua bước bắp nước nếu khách không có nhu cầu"),

        ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
        ("FNB_EP_01", "Kiểm tra chức năng Chọn số lượng Combo trong khoảng [1, 10] phần thành công",
         "Kiểm tra chọn 2 phần My Combo",
         "Bước 1: Click nút [+] chọn 2 phần 'My Combo'\nBước 2: Click button 'TIẾP TỤC ➔'",
         "Món chọn: 2 My Combo (Đơn giá 89.000đ)", "Hệ thống cộng 178.000đ vào tổng tiền và cho phép chuyển sang bước tiếp theo"),

        ("FNB_EP_02", "Kiểm tra chức năng Bỏ qua Combo (0 phần) thành công",
         "Kiểm tra không chọn món bắp nước nào và chuyển tiếp",
         "Bước 1: Không click chọn món bắp nước nào (Số lượng = 0)\nBước 2: Click button 'TIẾP TỤC ➔'",
         "Số lượng F&B: 0 phần", "Hệ thống cho phép bỏ qua hợp lệ và chuyển thẳng sang bước Ưu đãi"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("FNB_FUNC_01", "Kiểm tra chức năng Tùy chọn vị Combo có tính phụ thu thành công",
         "Kiểm tra chọn vị bắp Phô mai có tính phụ thu +15.000đ",
         "Bước 1: Chọn đổi vị bắp sang Phô mai (+15.000đ)\nBước 2: Kiểm tra tổng tiền",
         "Vị đổi: Phô mai\nPhụ thu: +15.000đ", "Tổng tiền tạm tính cộng thêm 15.000đ chính xác")
    ]

    tc_payment_cases = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("PAY_GUI_01", "Kiểm tra chức năng hiển thị Tùy chọn Phương thức thanh toán thành công",
         "Kiểm tra hiển thị các Phương thức thanh toán tại Booking Bước 4",
         "Bước 1: Chuyển sang bước 4 (Thanh toán)\nBước 2: Quan sát các tùy chọn phương thức thanh toán\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị 2 phương thức lựa chọn: 'Cổng thanh toán VNPAY' và 'Chuyển khoản VietQR' kèm logo nhận diện chính thức"),

        ("PAY_GUI_02", "Kiểm tra chức năng hiển thị Khung mã QR chuyển khoản VietQR tự sinh thành công",
         "Kiểm tra hiển thị Mã QR chuyển khoản khi chọn phương thức VietQR",
         "Bước 1: Chọn phương thức 'Chuyển khoản VietQR'\nBước 2: Quan sát khung hiển thị mã QR",
         "N/A", "Hiển thị mã QR động chuẩn VietQR chứa đúng số tiền thanh toán, số tài khoản ngân hàng rạp và nội dung chuyển khoản mã đơn hàng"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("PAY_ERR_01", "Kiểm tra chức năng Xử lý khi Khách hàng hủy giao dịch trên Cổng VNPAY (Mã lỗi 24) thành công",
         "Kiểm tra xử lý hủy đơn khi người dùng bấm 'Hủy giao dịch' trên VNPAY",
         "Bước 1: Click button 'Hủy giao dịch' trên màn hình VNPAY\nBước 2: Nhận điều hướng quay trở lại website DevCine\nBước 3: Kiểm tra kết quả",
         "Mã phản hồi VNPAY: '24' (Hủy giao dịch)", "Hệ thống hiển thị thông báo 'Giao dịch đã bị hủy bởi người dùng' và tự động giải phóng ghế đang giữ về trạng thái trống"),

        ("PAY_ERR_02", "Kiểm tra chức năng Xử lý khi Thẻ ngân hàng không đủ số dư (Mã lỗi 51) thành công",
         "Kiểm tra xử lý khi tài khoản / thẻ của khách không đủ tiền thanh toán",
         "Bước 1: Nhập thông tin thẻ test có số dư không đủ trên cổng VNPAY\nBước 2: Xác nhận thanh toán\nBước 3: Kiểm tra kết quả",
         "Mã phản hồi VNPAY: '51' (Không đủ số dư)", "Hiển thị thông báo lỗi 'Tài khoản của quý khách không đủ số dư để thực hiện giao dịch', cho phép khách chọn thẻ khác"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("PAY_FUNC_01", "Kiểm tra chức năng Thanh toán VNPAY thành công (Mã giao dịch 00)",
         "Kiểm tra luồng thanh toán VNPAY thành công và xử lý Webhook IPN Backend",
         "Bước 1: Xác thực OTP thành công trên cổng VNPAY (Mã phản hồi 00)\nBước 2: Backend nhận Webhook IPN từ VNPAY và xác thực chữ ký SHA512\nBước 3: Kiểm tra cập nhật trạng thái đơn hàng",
         "Mã giao dịch VNPAY: '00' (Thành công)", "Đơn hàng chuyển trạng thái CONFIRMED, tự động sinh mã vé QR độc nhất, tích điểm hội viên và chuyển sang BookingSuccessView"),

        ("PAY_FUNC_02", "Kiểm tra chức năng Thanh toán qua Chuyển khoản VietQR thành công",
         "Kiểm tra chức năng Thanh toán khi chọn phương thức Chuyển khoản qua mã VietQR",
         "Bước 1: Quét mã QR hiển thị trên màn hình qua app ngân hàng\nBước 2: Click button 'Tôi đã chuyển khoản'\nBước 3: Kiểm tra kết quả",
         "Phương thức: VietQR\nTổng tiền: 190.000 VNĐ", "Hệ thống kiểm tra giao dịch và chuyển sang màn hình Đặt vé thành công khi nhận được tiền")
    ]

    tc_seat = [("__FEATURE__", "CHỌN GHẾ & GIỮ CHỖ")] + tc_seat_cases
    tc_combo = [("__FEATURE__", "CHỌN BẮP NƯỚC (F&B)")] + tc_combo_cases
    tc_payment = [("__FEATURE__", "THANH TOÁN & NHẬN VÉ")] + tc_payment_cases

    modules.append({
        "code": "MOD_ONLINE_BOOKING", "sheet": "Đặt vé trực tuyến",
        "req": "Kiểm tra Luồng đặt vé trực tuyến 4 bước: Chọn ghế & Giữ chỗ 10 phút, Combo F&B online, Thanh toán VNPAY, Chuyển khoản VietQR và Sinh vé điện tử QR Code",
        "tester": "Nguyễn Quang Huy", "role": "Khách hàng",
        "pre": "Khách hàng thực hiện quy trình đặt vé xem phim trên BookingView.vue",
        "test_cases": tc_seat + tc_combo + tc_payment
    })

    # 3. LỊCH SỬ VÉ & VÍ VOUCHER
    tc_his = [("__FEATURE__", "LỊCH SỬ ĐẶT VÉ")] + raw["MOD_CUST_BOOKING_HISTORY"]["test_cases"]
    tc_myv = [("__FEATURE__", "VÍ VOUCHER CÁ NHÂN")] + raw["MOD_CUST_MY_VOUCHERS"]["test_cases"]
    modules.append({
        "code": "MOD_HISTORY_VOUCHERS", "sheet": "Lịch sử vé & Voucher",
        "req": "Kiểm tra Danh sách vé đã mua, Xem chi tiết mã vé QR và Quản lý Ví Voucher cá nhân",
        "tester": "Nguyễn Quang Huy", "role": "Khách hàng",
        "pre": "Khách hàng mở trang Lịch sử đặt vé / Ví voucher của tôi trên Web",
        "test_cases": tc_his + tc_myv
    })

    # 4. TƯƠNG TÁC & ĐÁNH GIÁ (Reviews, Comments, Feedback)
    tc_rev = [("__FEATURE__", "ĐÁNH GIÁ PHIM")] + raw["MOD_CUST_REVIEWS"]["test_cases"]
    tc_cmt = [("__FEATURE__", "BÌNH LUẬN PHIM")] + raw["MOD_CUST_COMMENTS"]["test_cases"]
    tc_fdb = [("__FEATURE__", "LIÊN HỆ & GÓP Ý")] + raw["MOD_CUST_FEEDBACK"]["test_cases"]
    modules.append({
        "code": "MOD_INTERACTION_REVIEWS", "sheet": "Tương tác & Đánh giá",
        "req": "Kiểm tra Gửi đánh giá sao 1-5, Viết nhận xét, Bình luận thảo luận phim trên MovieDetailView.vue và Gửi liên hệ góp ý CSKH",
        "tester": "Nguyễn Quang Huy", "role": "Khách hàng",
        "pre": "Khách hàng mở trang Chi tiết phim / Liên hệ trên hệ thống DevCine",
        "test_cases": tc_rev + tc_cmt + tc_fdb
    })

    # =========================================================================
    # NHÓM 2: ADMIN: TỔNG QUAN & VẬN HÀNH (OPERATIONS & POS) - 4 SHEETS
    # =========================================================================

    # 5. TỔNG QUAN (DASHBOARD)
    tc_dash_cases = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("STA_GUI_01", "Kiểm tra chức năng hiển thị 4 Thẻ KPI Tổng quan thành công",
         "Kiểm tra hiển thị các thẻ KPI doanh thu, số vé, khách hàng, F&B",
         "Bước 1: Mở màn hình Thống kê & Báo cáo (Dashboard.vue)\nBước 2: Quan sát hàng thẻ chỉ số KPI phía trên\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị 4 thẻ KPI: 'DOANH THU TỔNG', 'TỔNG SỐ VÉ ĐÃ BÁN', 'KHÁCH HÀNG MỚI', 'DOANH THU F&B' kèm % tăng trưởng so với kỳ trước"),

        ("STA_GUI_02", "Kiểm tra chức năng hiển thị Bộ lọc Thời gian và Custom Month Picker thành công",
         "Kiểm tra hiển thị Bộ chọn khoảng thời gian và chọn tháng",
         "Bước 1: Quan sát thanh điều khiển bộ lọc thời gian\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị các nút: 'Hôm nay', 'Tuần này', 'Tháng này', 'Năm nay' và Custom Month Picker ('Tháng MM/YYYY')"),

        ("STA_GUI_03", "Kiểm tra chức năng hiển thị Biểu đồ Doanh thu và Top Phim thành công",
         "Kiểm tra hiển thị Biểu đồ cột doanh thu và bảng xếp hạng Top 5 phim",
         "Bước 1: Quan sát khu vực biểu đồ\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Biểu đồ cột Doanh thu theo ngày/tháng có hover tooltip và Bảng Top 5 phim ăn khách nhất"),

        ("STA_GUI_04", "Kiểm tra chức năng hiển thị Bảng Doanh thu theo Cụm rạp thành công",
         "Kiểm tra hiển thị Bảng phân bổ doanh thu theo từng chi nhánh rạp",
         "Bước 1: Quan sát bảng dữ liệu cụm rạp\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Bảng: Tên cụm rạp, Số vé bán, Doanh thu vé, Doanh thu F&B, Tổng doanh thu và Tỷ trọng %"),

        ("STA_GUI_05", "Kiểm tra chức năng hiển thị Nút Xuất báo cáo Excel thành công",
         "Kiểm tra hiển thị nút Export Excel",
         "Bước 1: Quan sát góc phải thanh công cụ Dashboard\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị nút màu xanh '📥 XUẤT BÁO CÁO EXCEL'"),

        ("__SECTION__", "KIỂM TRA TÌM KIẾM & BỘ LỌC DỮ LIỆU"),
        ("STA_LOC_01", "Kiểm tra chức năng Lọc thống kê theo Hôm nay thành công",
         "Kiểm tra chức năng Lọc dữ liệu doanh thu phát sinh trong ngày hôm nay",
         "Bước 1: Click nút 'Hôm nay'\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Khoảng thời gian: Hôm nay", "4 thẻ KPI và biểu đồ cập nhật số liệu các đơn hàng hoàn tất trong ngày hôm nay"),

        ("STA_LOC_02", "Kiểm tra chức năng Lọc thống kê theo Tuần này thành công",
         "Kiểm tra chức năng Lọc dữ liệu doanh thu trong tuần hiện tại (Thứ 2 đến CN)",
         "Bước 1: Click nút 'Tuần này'\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Khoảng thời gian: Tuần này (7 ngày)", "Biểu đồ cột hiển thị doanh thu chi tiết 7 ngày trong tuần"),

        ("STA_LOC_03", "Kiểm tra chức năng Lọc thống kê theo Tháng này thành công",
         "Kiểm tra chức năng Lọc dữ liệu doanh thu trong tháng hiện tại",
         "Bước 1: Click nút 'Tháng này'\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Khoảng thời gian: Tháng hiện tại", "Hiển thị tổng doanh thu tháng và xu hướng doanh thu từng tuần"),

        ("STA_LOC_04", "Kiểm tra chức năng Lọc thống kê theo Năm nay thành công",
         "Kiểm tra chức năng Lọc dữ liệu doanh thu cả năm",
         "Bước 1: Click nút 'Năm nay'\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Khoảng thời gian: Năm hiện tại (12 tháng)", "Biểu đồ hiển thị doanh thu theo 12 tháng trong năm"),

        ("STA_LOC_05", "Kiểm tra chức năng Lọc thống kê theo Cụm rạp cụ thể thành công",
         "Kiểm tra chức năng Lọc số liệu riêng cho 1 chi nhánh rạp",
         "Bước 1: Chọn dropdown Cụm rạp: 'CGV Vincom Cầu Giấy'\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Cụm rạp: 'CGV Vincom Cầu Giấy'", "Chỉ hiển thị số vé và doanh thu thuộc riêng rạp Cầu Giấy"),

        ("STA_LOC_06", "Kiểm tra chức năng Chọn tháng quá khứ qua Month Picker thành công",
         "Kiểm tra chức năng Chọn tháng lịch sử trên Custom Month Picker",
         "Bước 1: Mở Custom Month Picker và chọn tháng '01/2024'\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tháng chọn: '01/2024'", "Dashboard tải và hiển thị số liệu lịch sử của tháng 01/2024 chuẩn xác"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("STA_ERR_01", "Kiểm tra chức năng Hiển thị trạng thái rỗng (Empty State) thành công",
         "Kiểm tra hiển thị Dashboard khi chọn khoảng thời gian rạp chưa có phát sinh doanh thu (0đ)",
         "Bước 1: Lọc ngày rạp đóng cửa bảo dưỡng (0 đơn hàng)\nBước 2: Quan sát các biểu đồ\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Doanh thu: 0 VNĐ\nSố vé: 0 vé", "Hiển thị 0đ rõ ràng, biểu đồ phẳng mượt mà, không xảy ra lỗi crash hoặc vỡ layout"),

        ("STA_ERR_02", "Kiểm tra chức năng Chặn chọn tháng trong tương lai thành công",
         "Kiểm tra chức năng Vô hiệu hóa các tháng tương lai trên Month Picker",
         "Bước 1: Mở Custom Month Picker, quan sát các tháng trong tương lai (Tháng 12/2026)\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tháng tương lai: '12/2026'", "Các tháng tương lai bị làm mờ (disabled), không thể click chọn"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("STA_FUNC_01", "Kiểm tra chức năng Xuất báo cáo Excel thành công",
         "Kiểm tra chức năng Tải file Excel báo cáo doanh thu tổng hợp",
         "Bước 1: Chọn khoảng thời gian tháng hiện tại\nBước 2: Click button '📥 XUẤT BÁO CÁO EXCEL'\nBước 3: Kiểm tra file tải về",
         "Định dạng xuất: Excel (.xlsx)\nPhạm vi: Toàn hệ thống", "Tải về file Excel chứa đầy đủ các sheet: Doanh thu theo rạp, Doanh thu theo phim, Doanh thu Combo F&B"),

        ("STA_FUNC_02", "Kiểm tra chức năng Tự động làm mới số liệu Dashboard real-time",
         "Kiểm tra cơ chế cập nhật số liệu khi có đơn đặt vé mới thành công từ khách hàng",
         "Bước 1: Admin đang mở Dashboard (Doanh thu: 10.000.000đ)\nBước 2: Có khách hàng đặt thành công đơn vé 200.000đ trên Web\nBước 3: Quan sát thẻ KPI Doanh thu trên màn hình Admin",
         "Đơn hàng mới: +200.000đ", "Thẻ KPI tự động cập nhật số liệu lên 10.200.000đ ngay tức thì mà không cần F5 tải lại trang")
    ]
    modules.append({
        "code": "MOD_ADMIN_DASHBOARD", "sheet": "Tổng quan (Dashboard)",
        "req": "Kiểm tra 4 Thẻ KPI, Biểu đồ doanh thu, Bộ lọc thời gian, Month Picker, Phân bổ theo rạp và Xuất Excel",
        "tester": "Nguyễn Ngọc Hà Linh", "role": "Quản trị viên",
        "pre": "Quản trị viên mở màn hình Dashboard.vue trên Admin Portal",
        "test_cases": [("__FEATURE__", "BÁO CÁO DOANH THU")] + tc_dash_cases
    })

    # 6. BÁN HÀNG TẠI QUẦY (POS) (Gộp POS Bán vé, Đơn chờ, Bán F&B quầy)
    tc_pos_ticket = [("__FEATURE__", "BÁN VÉ TẠI QUẦY (POS)")] + raw["MOD_POS_TICKETS"]["test_cases"]
    tc_pos_pending_sec = [("__FEATURE__", "QUẢN LÝ ĐƠN CHỜ")] + tc_pos_pending
    tc_pos_fnb_sec = [("__FEATURE__", "BÁN BẮP NƯỚC TẠI QUẦY")] + tc_pos_fnb
    modules.append({
        "code": "MOD_ADMIN_POS", "sheet": "Bán hàng tại quầy (POS)",
        "req": "Kiểm tra Bán vé xem phim tại quầy, Quản lý 3 Tab Đơn chờ, Bán bắp nước F&B, Tùy chọn vị Topping, Tra cứu hội viên, Áp voucher và In hóa đơn/phiếu nhận món",
        "tester": "Văn Minh Khôi", "role": "Nhân viên",
        "pre": "Nhân viên mở giao diện TicketingPOS.vue tại quầy vé và quầy bắp nước",
        "test_cases": tc_pos_ticket + tc_pos_pending_sec + tc_pos_fnb_sec
    })

    # 7. KIỂM SOÁT VÉ (CHECK-IN)
    modules.append({
        "code": "MOD_ADMIN_CHECKIN", "sheet": "Kiểm soát vé (Check-in)",
        "req": "Kiểm tra Quét mã QR qua Camera, Nhập mã thủ công, Chặn vé đã dùng/giả mạo/sai suất, Âm thanh BEEP và In vé nhiệt",
        "tester": "Văn Minh Khôi", "role": "Nhân viên",
        "pre": "Nhân viên mở màn hình StaffTicketCheckin.vue tại cửa phòng chiếu",
        "test_cases": [("__FEATURE__", "SOÁT VÉ (CHECK-IN)")] + tc_checkin
    })

    # 8. SỰ CỐ & HÓA ĐƠN (Gộp Hóa đơn Bookings, Approve Void, Xử lý sự cố ghế, Khóa bảo trì ghế)
    tc_bookings_sec = [("__FEATURE__", "HÓA ĐƠN & HỦY ĐƠN")] + tc_void
    tc_incident_sec = [("__FEATURE__", "XỬ LÝ SỰ CỐ GHẾ")] + tc_incident
    tc_maint_sec = [("__FEATURE__", "KHÓA BẢO TRÌ GHẾ")] + tc_maint
    modules.append({
        "code": "MOD_ADMIN_INCIDENTS_BOOKINGS", "sheet": "Sự cố & Hóa đơn",
        "req": "Kiểm tra Danh sách hóa đơn đơn hàng, Phê duyệt hủy đơn F&B hoàn tiền, Xử lý sự cố chỗ ngồi đổi ngang VIP và Khóa bảo trì ghế vật lý",
        "tester": "Văn Minh Khôi", "role": "Nhân viên & Quản lý",
        "pre": "Nhân viên / Quản lý mở AdminBookings.vue / IncidentManagement.vue",
        "test_cases": tc_bookings_sec + tc_incident_sec + tc_maint_sec
    })

    # =========================================================================
    # NHÓM 3: ADMIN: PHIM & HẠ TẦNG (MOVIES & INFRASTRUCTURE) - 3 SHEETS
    # =========================================================================

    # 9. QUẢN LÝ PHIM & DANH MỤC (Phim 106 TCs + Categories 108 TCs + Banners 44 TCs)
    tc_cat_part = [("__FEATURE__", "DANH MỤC PHIM")] + raw["MOD_ADMIN_GENRES"]["test_cases"] + raw["MOD_ADMIN_DIRECTORS"]["test_cases"] + raw["MOD_ADMIN_ACTORS"]["test_cases"] + raw["MOD_ADMIN_FORMATS"]["test_cases"]
    tc_ban_part = [("__FEATURE__", "QUẢN LÝ BANNER")] + raw["MOD_ADMIN_BANNERS"]["test_cases"]
    modules.append({
        "code": "MOD_ADMIN_MOVIES_CAT", "sheet": "Quản lý Phim & Danh mục",
        "req": "Kiểm tra Modal Thêm phim mới & Chỉnh sửa thông tin phim, Danh mục Thể loại, Đạo diễn, Diễn viên, Định dạng phòng chiếu và Quản lý Banner quảng cáo",
        "tester": "Nguyễn Ngọc Hà Linh", "role": "Quản trị viên",
        "pre": "Quản trị viên mở AdminMovies.vue / MovieCategoryManager.vue / AdminBanners.vue",
        "test_cases": tc_movies_suite + tc_cat_part + tc_ban_part
    })

    # 10. CỤM RẠP & LỊCH CHIẾU (Cụm rạp, Phòng chiếu, Sơ đồ ghế SeatMapBuilder, Lịch chiếu)
    tc_smap_cases = [
        ("__FEATURE__", "THIẾT KẾ SƠ ĐỒ GHẾ"),
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("MAP_GUI_01", "Kiểm tra chức năng hiển thị Lưới ma trận cấu hình ghế thành công",
         "Kiểm tra hiển thị Trình dựng sơ đồ ghế trực quan trên AdminSeatMap.vue",
         "Bước 1: Quản trị viên mở thiết kế sơ đồ ghế của phòng chiếu\nBước 2: Quan sát lưới ma trận hàng x cột\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị lưới ma trận cấu hình trực quan: Hàng A-Z, Cột 1-20, Bảng màu phân loại loại ghế (Thường, VIP, Sweetbox, Lối đi)"),

        ("MAP_GUI_02", "Kiểm tra chức năng hiển thị Thanh công cụ chọn loại ghế thành công",
         "Kiểm tra hiển thị Toolbar chọn loại ghế để quét vẽ",
         "Bước 1: Quan sát thanh công cụ phía trên ma trận ghế\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị các công cụ: 'Ghế Thường (Single)', 'Ghế VIP', 'Ghế Sweetbox (Couple)', 'Lối đi (Aisle/Space)', 'Vô hiệu hóa (Disabled)'"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("MAP_FUNC_01", "Kiểm tra chức năng Chuyển đổi loại ghế bằng cách Click ô thành công",
         "Kiểm tra chức năng Đổi loại ghế từ Thường sang VIP khi click",
         "Bước 1: Chọn công cụ 'Ghế VIP' trên Toolbar\nBước 2: Click vào ghế D05 trên lưới ma trận\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vị trí: D05\nLoại mới: VIP", "Ghế D05 đổi sang màu vàng cam của Ghế VIP"),

        ("MAP_FUNC_02", "Kiểm tra chức năng Tạo lối đi (Aisle) giữa 2 dãy ghế thành công",
         "Kiểm tra chức năng Chuyển ô ghế thành lối đi trống",
         "Bước 1: Chọn công cụ 'Lối đi (Space)'\nBước 2: Click liên tiếp vào các ô cột 6 từ hàng A đến H\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Cột: 6 (Hàng A-H)\nLoại: Lối đi", "Toàn bộ cột 6 chuyển thành khoảng trống lối đi ngăn cách hai dãy ghế"),

        ("MAP_FUNC_03", "Kiểm tra chức năng Đặt ghế Sweetbox đôi (chiếm 2 ô liền nhau) thành công",
         "Kiểm tra chức năng Ghế Sweetbox tự động gộp 2 ô ngang",
         "Bước 1: Chọn công cụ 'Sweetbox'\nBước 2: Click vào ô H01 tại hàng cuối\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vị trí: H01\nLoại: Sweetbox đôi", "Hệ thống tự động gộp ô H01 và H02 thành 1 khối ghế đôi Sweetbox màu hồng"),

        ("MAP_FUNC_04", "Kiểm tra chức năng Tự động đánh số lại nhãn ghế (Auto Re-index) thành công",
         "Kiểm tra hệ thống tự động bỏ qua lối đi khi đánh số thứ tự cột",
         "Bước 1: Cột 5 là lối đi trống, ô trước đó là A04\nBước 2: Quan sát nhãn của ô ghế tiếp theo sau lối đi\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Cột 4: Ghế\nCột 5: Lối đi\nCột 6: Ghế", "Ô tại cột 6 tự động được gắn nhãn là 'A05' liền mạch, không bị đứt đoạn số ghế"),

        ("MAP_FUNC_05", "Kiểm tra chức năng Lưu sơ đồ ghế thành công",
         "Kiểm tra lưu thiết kế ma trận ghế vào cơ sở dữ liệu",
         "Bước 1: Hoàn tất thiết kế sơ đồ phòng chiếu 1 (120 ghế)\nBước 2: Click button 'Lưu sơ đồ ghế'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tổng số ghế: 120 ghế\nPhòng: Phòng 1", "Lưu thành công, hiển thị thông báo 'Cập nhật sơ đồ ghế phòng chiếu thành công'"),

        ("MAP_FUNC_06", "Kiểm tra chức năng Chặn sửa sơ đồ ghế khi phòng chiếu đang có suất chiếu đã bán vé",
         "Kiểm tra ràng buộc toàn vẹn dữ liệu khi sửa ma trận phòng chiếu đang hoạt động",
         "Bước 1: Mở sơ đồ phòng chiếu 1 đang có suất chiếu lúc 19:00 đã bán được 10 vé\nBước 2: Cố tình thay đổi vị trí ghế và bấm Lưu\nBước 3: Kiểm tra phản hồi từ hệ thống",
         "Trạng thái phòng: Đang có suất chiếu mở bán", "Hệ thống từ chối lưu và cảnh báo: 'Không thể sửa sơ đồ ghế của phòng chiếu đang có suất chiếu kích hoạt'")
    ]
    tc_cinema_infra = [("__FEATURE__", "CỤM RẠP & PHÒNG CHIẾU")] + raw["MOD_ADMIN_CINEMAS"]["test_cases"] + raw["MOD_ADMIN_ROOMS"]["test_cases"] + tc_smap_cases
    tc_sched_part = [("__FEATURE__", "QUẢN LÝ LỊCH CHIẾU")] + raw["MOD_ADMIN_SCHEDULES"]["test_cases"]
    modules.append({
        "code": "MOD_ADMIN_CINEMAS_SCHED", "sheet": "Cụm rạp & Lịch chiếu",
        "req": "Kiểm tra Quản lý Chi nhánh Cụm rạp, Danh sách Phòng chiếu, Trình dựng thiết kế Sơ đồ ghế (SeatMapBuilder) và Xếp lịch chiếu suất phim",
        "tester": "Nguyễn Ngọc Hà Linh", "role": "Quản trị viên",
        "pre": "Quản trị viên mở CinemaManager.vue / AdminSchedules.vue",
        "test_cases": tc_cinema_infra + tc_sched_part
    })

    # 11. THỰC ĐƠN F&B / COMBO (Món F&B + Toppings)
    tc_fnb_items_sec = [("__FEATURE__", "MÓN & COMBO BẮP NƯỚC")] + raw["MOD_ADMIN_FNB_ITEMS"]["test_cases"]
    tc_toppings_sec = [("__FEATURE__", "TÙY CHỌN VỊ & TOPPING")] + raw["MOD_ADMIN_TOPPINGS"]["test_cases"]
    modules.append({
        "code": "MOD_ADMIN_FNB", "sheet": "Thực đơn F&B",
        "req": "Kiểm tra Quản lý Danh mục Món F&B, Combo bắp nước và Bảng tùy chọn vị Topping trên FnbMenuManager.vue",
        "tester": "Nguyễn Quang Huy", "role": "Quản lý",
        "pre": "Quản lý mở màn hình Thực đơn F&B (FnbMenuManager.vue) trên Admin Portal",
        "test_cases": tc_fnb_items_sec + tc_toppings_sec
    })

    # =========================================================================
    # NHÓM 4: ADMIN: KINH DOANH & HỆ THỐNG (BUSINESS & SYSTEM) - 3 SHEETS
    # =========================================================================

    # 12. GIÁ VÉ & KHUYẾN MÃI (Giá vé + Voucher + Loyalty)
    tc_pri_sec = [("__FEATURE__", "BẢNG GIÁ VÉ")] + raw["MOD_ADMIN_PRICING"]["test_cases"]
    tc_vou_sec = [("__FEATURE__", "KHUYẾN MÃI & VOUCHER")] + raw["MOD_ADMIN_VOUCHERS"]["test_cases"]
    tc_loy_sec = [("__FEATURE__", "ĐIỂM THƯỞNG & HẠNG THẺ")] + raw["MOD_ADMIN_LOYALTY"]["test_cases"]
    modules.append({
        "code": "MOD_ADMIN_PRICING_PROMO", "sheet": "Giá vé & Khuyến mãi",
        "req": "Kiểm tra Cấu hình Bảng giá vé theo khung giờ/ghế, Tạo mã giảm giá Voucher, Chương trình khuyến mãi và Cấu hình tích điểm Loyalty",
        "tester": "Nguyễn Ngọc Hà Linh", "role": "Quản trị viên",
        "pre": "Quản trị viên mở AdminPricing.vue / AdminPromotions.vue",
        "test_cases": tc_pri_sec + tc_vou_sec + tc_loy_sec
    })

    # 13. KHÁCH HÀNG & CSKH (Khách hàng + Phản hồi CSKH & FAQ)
    tc_cus_cases = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("CUS_GUI_01", "Kiểm tra chức năng hiển thị Bảng Danh sách Khách hàng thành công",
         "Kiểm tra hiển thị Danh sách hội viên trên AdminCustomers.vue",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\nBước 2: Mở màn hình Quản lý khách hàng\nBước 3: Quan sát bảng dữ liệu\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị các cột: Mã KH, Họ tên, SĐT, Email, Hạng thành viên (Standard/VIP/Diamond), Điểm Loyalty, Trạng thái (Hoạt động/Bị khóa)"),

        ("CUS_GUI_02", "Kiểm tra chức năng hiển thị Modal Chi tiết Lịch sử Đặt vé thành công",
         "Kiểm tra hiển thị Popup xem các đơn vé đã mua của khách hàng",
         "Bước 1: Click nút 'Xem lịch sử' tại khách hàng 'Nguyễn Văn Dân'\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Modal danh sách các vé đã đặt, tổng tiền chi tiêu tích lũy và số điểm thưởng"),

        ("__SECTION__", "KIỂM TRA TÌM KIẾM & BỘ LỌC DỮ LIỆU"),
        ("CUS_LOC_01", "Kiểm tra chức năng Tìm kiếm khách hàng theo Số điện thoại thành công",
         "Kiểm tra tìm kiếm khách hàng theo SĐT '0901234567'",
         "Bước 1: Nhập '0901234567' vào ô Tìm kiếm\nBước 2: Click Tìm kiếm\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tìm kiếm: '0901234567'", "Bảng dữ liệu chỉ hiển thị duy nhất khách hàng có số điện thoại 0901234567"),

        ("CUS_LOC_02", "Kiểm tra chức năng Lọc khách hàng theo Hạng thành viên thành công",
         "Kiểm tra chức năng lọc hội viên hạng 'VIP'",
         "Bước 1: Chọn dropdown Hạng: 'VIP'\nBước 2: Click Lọc\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Hạng thành viên: 'VIP'", "Bảng dữ liệu chỉ hiển thị các khách hàng đạt thứ hạng VIP"),

        ("CUS_LOC_03", "Kiểm tra chức năng Lọc khách hàng theo Trạng thái tài khoản thành công",
         "Kiểm tra chức năng lọc danh sách tài khoản 'Bị khóa'",
         "Bước 1: Chọn dropdown Trạng thái: 'Bị khóa'\nBước 2: Click Lọc\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Trạng thái: 'LOCKED'", "Bảng dữ liệu chỉ hiển thị các tài khoản đang trong trạng thái bị khóa"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("CUS_ERR_01", "Kiểm tra chức năng Xác nhận trước khi Khóa tài khoản thành công",
         "Kiểm tra hiển thị Modal cảnh báo xác nhận khi Admin bấm Khóa tài khoản",
         "Bước 1: Click nút 'Khóa tài khoản' tại 1 khách hàng\nBước 2: Quan sát thông báo xác nhận\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Thao tác: Bấm Khóa", "Hiển thị Modal cảnh báo: 'Bạn có chắc chắn muốn khóa tài khoản khách hàng này không? Khách hàng sẽ không thể đăng nhập.'"),

        ("CUS_ERR_02", "Kiểm tra chức năng Hủy thao tác khóa tài khoản thành công",
         "Kiểm tra khi Admin bấm Hủy bỏ trên Modal xác nhận khóa",
         "Bước 1: Mở Modal xác nhận khóa tài khoản\nBước 2: Click button 'Hủy bỏ'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Thao tác: Click Hủy", "Modal đóng lại, trạng thái tài khoản khách hàng giữ nguyên Hoạt động"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("CUS_FUNC_01", "Kiểm tra chức năng Khóa tài khoản khách hàng vi phạm thành công",
         "Kiểm tra chức năng Khóa tài khoản và chặn đăng nhập",
         "Bước 1: Admin xác nhận khóa tài khoản '0901234567'\nBước 2: Khách hàng dùng số '0901234567' đăng nhập trên Web\nBước 3: Kiểm tra phản hồi từ hệ thống",
         "Tài khoản: '0901234567'\nTrạng thái mới: LOCKED", "Tài khoản chuyển sang trạng thái 'Bị khóa' (Badge đỏ), trên Web báo lỗi: 'Tài khoản của bạn đã bị khóa'"),

        ("CUS_FUNC_02", "Kiểm tra chức năng Mở khóa tài khoản khách hàng thành công",
         "Kiểm tra chức năng Mở khóa và khôi phục quyền đăng nhập",
         "Bước 1: Admin click button 'Mở khóa' tại tài khoản '0901234567'\nBước 2: Khách hàng thực hiện đăng nhập lại trên Web\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tài khoản: '0901234567'\nTrạng thái mới: ACTIVE", "Tài khoản chuyển về trạng thái 'Hoạt động' (Badge xanh), khách hàng đăng nhập lại bình thường"),

        ("CUS_FUNC_03", "Kiểm tra chức năng Tự động nâng hạng thành viên khi đạt mốc chi tiêu",
         "Kiểm tra hệ thống tự động thăng hạng từ Standard lên VIP khi chi tiêu đạt 2.000.000đ",
         "Bước 1: Khách hàng tích lũy đủ 2.000.000đ chi tiêu mua vé\nBước 2: Admin mở màn hình Quản lý khách hàng quan sát\nBước 3: Kiểm tra thứ hạng",
         "Tổng chi tiêu: 2.150.000 VNĐ", "Hệ thống tự động nâng hạng khách hàng lên 'VIP' kèm tỷ lệ tích điểm cao hơn")
    ]

    tc_cus_sec = [("__FEATURE__", "QUẢN LÝ KHÁCH HÀNG")] + tc_cus_cases
    tc_sup_sec = [("__FEATURE__", "PHẢN HỒI CSKH & FAQ")] + raw["MOD_CUST_FEEDBACK"]["test_cases"]
    modules.append({
        "code": "MOD_ADMIN_CUSTOMERS_SUPPORT", "sheet": "Khách hàng & CSKH",
        "req": "Kiểm tra Danh sách hội viên, Hạng thẻ, Khóa/Mở khóa tài khoản, Tiếp nhận phản hồi đóng góp ý kiến của khách hàng và Quản lý FAQ",
        "tester": "Phạm Thị Quỳnh Anh", "role": "Quản trị viên",
        "pre": "Quản trị viên mở AdminCustomers.vue / CustomerSupport.vue",
        "test_cases": tc_cus_sec + tc_sup_sec
    })

    # 14. QUẢN TRỊ HỆ THỐNG (RBAC) (Nhân viên + RBAC + Audit Logs + Settings)
    tc_rbac_cases = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("RBC_GUI_01", "Kiểm tra chức năng hiển thị Khối chọn vai trò và Chuyển chế độ cấu hình thành công",
         "Kiểm tra hiển thị các nút chọn vai trò và switch mode trên màn hình Phân quyền",
         "Bước 1: Mở màn hình Phân quyền hệ thống (AdminPermissions.vue)\nBước 2: Quan sát khối chọn vai trò và chế độ\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị 3 nút vai trò 'ADMIN', 'MANAGER', 'STAFF' và switch chuyển đổi 'Cấu hình theo vai trò' / 'Cấu hình theo nhân viên'"),

        ("RBC_GUI_02", "Kiểm tra chức năng hiển thị 4 Tab danh mục phân hệ quyền thành công",
         "Kiểm tra hiển thị các Tab phân hệ quyền",
         "Bước 1: Mở màn hình Phân quyền hệ thống\nBước 2: Quan sát thanh tab phân hệ\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị đầy đủ 4 tab: 'TỔNG QUAN', 'NGHIỆP VỤ', 'NỘI DUNG', 'HỆ THỐNG'"),

        ("RBC_GUI_03", "Kiểm tra chức năng hiển thị Bảng checkbox ma trận quyền phân theo nhóm tính năng thành công",
         "Kiểm tra hiển thị các nhóm quyền chi tiết",
         "Bước 1: Chọn tab 'HỆ THỐNG'\nBước 2: Quan sát ma trận checkbox quyền\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị các khối quyền nhóm: 'HỆ THỐNG CỤM RẠP', 'NHÂN SỰ', 'CHĂM SÓC KHÁCH HÀNG' kèm các checkbox Xem, Thêm, Sửa, Xóa"),

        ("RBC_GUI_04", "Kiểm tra chức năng hiển thị Các nút thao tác hàng loạt thành công",
         "Kiểm tra hiển thị các nút chọn nhanh / bỏ nhanh quyền",
         "Bước 1: Quan sát phía trên danh sách quyền\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị 2 nút tác vụ nhanh: '= BỎ TẤT CẢ TRONG TAB NÀY' và '🚫 BỎ TOÀN BỘ QUYỀN' (hoặc CHỌN TẤT CẢ)"),

        ("RBC_GUI_05", "Kiểm tra chức năng hiển thị Thanh tóm tắt quyền và Nút Lưu thiết lập thành công",
         "Kiểm tra hiển thị Bottom Bar tóm tắt quyền",
         "Bước 1: Quan sát thanh Bottom Bar cố định phía dưới màn hình\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị tóm tắt danh sách quyền đang gán cho vai trò và nút 'LƯU THIẾT LẬP PHÂN QUYỀN'"),

        ("RBC_GUI_06", "Kiểm tra chức năng hiển thị Badge Admin Toàn quyền bị khóa thành công",
         "Kiểm tra hiển thị trạng thái bảo vệ quyền tối cao cho vai trò ADMIN",
         "Bước 1: Click chọn vai trò 'ADMIN'\nBước 2: Quan sát trạng thái các checkbox và nút Lưu\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vai trò: ADMIN", "Hiển thị badge vàng 'ADMIN TOÀN QUYỀN 🔒', toàn bộ checkbox ở trạng thái đã chọn và bị khóa (read-only)"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("RBC_ERR_01", "Kiểm tra chức năng Tước quyền Quản trị viên tối cao thất bại",
         "Kiểm tra chức năng Chặn tước quyền cốt lõi của vai trò ADMIN (Superuser Guard)",
         "Bước 1: Chọn vai trò 'ADMIN'\nBước 2: Cố tình click bỏ tích chọn các quyền hệ thống\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vai trò: ADMIN\nThao tác: Bỏ quyền Admin", "Hệ thống khóa toàn bộ checkbox không cho chỉnh sửa, hiển thị badge 'ADMIN TOÀN QUYỀN 🔒' để ngăn chặn lỗi tự khóa tài khoản Admin"),

        ("RBC_ERR_02", "Kiểm tra chức năng Cảnh báo thay đổi chưa lưu khi chuyển vai trò thành công",
         "Kiểm tra chức năng Cảnh báo khi người dùng chuyển sang vai trò khác mà chưa bấm Lưu phân quyền",
         "Bước 1: Đang chỉnh sửa quyền cho STAFF (chưa bấm Lưu)\nBước 2: Click chọn chuyển sang vai trò MANAGER\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Trạng thái: Có thay đổi quyền chưa lưu\nThao tác: Chuyển vai trò", "Hiển thị Modal cảnh báo: 'Bạn có thay đổi chưa lưu. Bạn có muốn lưu trước khi chuyển vai trò không?'"),

        ("RBC_ERR_03", "Kiểm tra chức năng Chặn truy cập API trái phép từ Backend (Enforce RBAC) thành công",
         "Kiểm tra cơ chế @perm.can(...) tại Backend khi nhân viên bị tước quyền gọi trực tiếp API",
         "Bước 1: Tài khoản nhân viên STAFF bị tước quyền xóa phim (movies:delete)\nBước 2: Dùng token của nhân viên STAFF gửi request HTTP DELETE /api/v1/admin/movies/123\nBước 3: Kiểm tra phản hồi từ Backend",
         "Token: STAFF (Không có quyền movies:delete)\nRequest: DELETE /api/v1/admin/movies/123", "Backend từ chối thực thi và trả về mã lỗi HTTP '403 Forbidden'"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("RBC_FUNC_01", "Kiểm tra chức năng Cấu hình quyền vai trò Quản lý thành công",
         "Kiểm tra chức năng Thiết lập quyền cho vai trò Quản lý (MANAGER)",
         "Bước 1: Click chọn vai trò 'MANAGER'\nBước 2: Chọn các quyền: Xem báo cáo, Bán vé POS, Thực đơn F&B, Xử lý sự cố ghế, Quản lý phim\nBước 3: Click button 'Lưu thiết lập phân quyền'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vai trò: MANAGER\nQuyền gán: dashboard_stats(view), pos_ticketing(view,add), fnb_menu(view,add,edit), movies(view,add,edit)", "Lưu thành công, hiển thị thông báo 'Cập nhật phân quyền vai trò MANAGER thành công'"),

        ("RBC_FUNC_02", "Kiểm tra chức năng Cấu hình quyền vai trò Nhân viên thành công",
         "Kiểm tra chức năng Thiết lập quyền cho vai trò Nhân viên (STAFF)",
         "Bước 1: Click chọn vai trò 'STAFF'\nBước 2: Chỉ chọn quyền: Vào quầy bán vé POS, Xem thực đơn F&B, Soát vé Check-in\nBước 3: Click button 'Lưu thiết lập phân quyền'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vai trò: STAFF\nQuyền gán: pos_ticketing(view,add), fnb_menu(view), ticket_checkin(view,add)", "Lưu thành công, nhân viên STAFF chỉ truy cập được màn hình POS và Check-in"),

        ("RBC_FUNC_03", "Kiểm tra chức năng Chọn tất cả quyền trong Tab thành công",
         "Kiểm tra chức năng Chọn tất cả quyền trong tab hiện tại",
         "Bước 1: Chọn vai trò MANAGER, mở tab 'NỘI DUNG'\nBước 2: Click button 'CHỌN TẤT CẢ TRONG TAB NÀY'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tab: NỘI DUNG\nThao tác: Click Chọn tất cả trong tab", "Toàn bộ checkbox quyền trong tab Nội dung (Phim, Lịch chiếu, Banner, Khuyến mãi, Giá vé, Khách hàng) được tích chọn đồng thời"),

        ("RBC_FUNC_04", "Kiểm tra chức năng Bỏ tất cả quyền trong Tab thành công",
         "Kiểm tra chức năng Bỏ tất cả quyền trong tab hiện tại",
         "Bước 1: Chọn vai trò STAFF, mở tab 'HỆ THỐNG'\nBước 2: Click button 'BỎ TẤT CẢ TRONG TAB NÀY'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tab: HỆ THỐNG\nThao tác: Click Bỏ tất cả trong tab", "Toàn bộ checkbox quyền trong tab Hệ thống bị bỏ tích chọn sạch sẽ"),

        ("RBC_FUNC_05", "Kiểm tra chức năng Chọn toàn bộ quyền của vai trò thành công",
         "Kiểm tra chức năng Chọn toàn bộ quyền trên tất cả 4 tab",
         "Bước 1: Chọn vai trò MANAGER\nBước 2: Click button 'CHỌN TOÀN BỘ QUYỀN'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vai trò: MANAGER\nThao tác: Click Chọn toàn bộ quyền", "Tất cả quyền trên 4 tab (Tổng quan, Nghiệp vụ, Nội dung, Hệ thống) được tích chọn 100%"),

        ("RBC_FUNC_06", "Kiểm tra chức năng Bỏ toàn bộ quyền của vai trò thành công",
         "Kiểm tra chức năng Tước bỏ toàn bộ quyền trên tất cả 4 tab",
         "Bước 1: Chọn vai trò STAFF\nBước 2: Click button 'BỎ TOÀN BỘ QUYỀN'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vai trò: STAFF\nThao tác: Click Bỏ toàn bộ quyền", "Toàn bộ quyền của vai trò STAFF bị xóa sạch (0 quyền)"),

        ("RBC_FUNC_07", "Kiểm tra chức năng Cấp quyền cho vai trò khi số lượng quyền là 0 quyền",
         "Kiểm tra khi tước toàn bộ quyền của vai trò STAFF",
         "Bước 1: Bỏ toàn bộ quyền của vai trò STAFF và bấm Lưu\nBước 2: Dùng tài khoản STAFF đăng nhập vào hệ thống\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng quyền gán: 0 quyền", "Nhân viên STAFF đăng nhập thành công nhưng thanh Sidebar trống trơn, không thể truy cập bất kỳ màn hình quản trị nào"),

        ("RBC_FUNC_08", "Kiểm tra chức năng Cấp quyền cho vai trò khi số lượng quyền là 1 quyền duy nhất",
         "Kiểm tra khi chỉ cấp đúng 1 quyền duy nhất cho vai trò STAFF",
         "Bước 1: Chỉ cấp quyền 'Xem lịch chiếu' cho STAFF và bấm Lưu\nBước 2: Dùng tài khoản STAFF đăng nhập\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng quyền gán: 1 quyền", "Nhân viên STAFF chỉ nhìn thấy và chỉ truy cập được duy nhất 1 màn hình Lịch chiếu"),

        ("RBC_FUNC_09", "Kiểm tra chức năng Cập nhật quyền truy cập tức thì sau khi Lưu phân quyền thành công",
         "Kiểm tra quyền hạn của nhân viên được cập nhật real-time ngay sau khi Admin bấm Lưu",
         "Bước 1: Admin cấp thêm quyền 'Báo cáo doanh thu' cho vai trò STAFF và bấm Lưu\nBước 2: Nhân viên STAFF đang mở trang web thực hiện tải lại trang\nBước 3: Kiểm tra hiển thị Menu Báo cáo",
         "Vai trò: STAFF\nQuyền mới: dashboard_stats(view)", "Menu Báo cáo doanh thu xuất hiện ngay lập tức trên thanh Sidebar của nhân viên STAFF"),

        ("RBC_FUNC_10", "Kiểm tra chức năng Phân quyền tùy biến theo từng nhân viên (User-level Override) thành công",
         "Kiểm tra cơ chế Override Allow/Deny phân quyền độc lập cho từng nhân viên cụ thể",
         "Bước 1: Chuyển sang chế độ 'Cấu hình theo nhân viên' (User mode)\nBước 2: Chọn nhân viên 'Văn Minh Khôi' (Vai trò gốc STAFF)\nBước 3: Cấp thêm quyền đặc thù 'Xử lý sự cố ghế (incident_handling:handle)' cho riêng nhân viên Khôi\nBước 4: Click button 'Lưu thiết lập'\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "Nhân viên: Văn Minh Khôi (STAFF)\nQuyền Override Allow: incident_handling(handle)", "Riêng nhân viên Khôi được phép xử lý sự cố đổi ghế, các nhân viên STAFF khác vẫn bị chặn bình thường"),

        ("RBC_FUNC_11", "Kiểm tra chức năng Checkbox chọn tất cả nhóm tính năng (Toggle Feature All) thành công",
         "Kiểm tra chức năng Tích chọn master checkbox của nhóm tính năng",
         "Bước 1: Tại tab 'NỘI DUNG', click vào master checkbox '☑️ QUẢN LÝ DANH SÁCH PHIM'\nBước 2: Quan sát các checkbox con bên trong\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Thao tác: Click Master Checkbox 'QUẢN LÝ DANH SÁCH PHIM'", "Tự động tích chọn/bỏ chọn đồng thời cả 4 action: Xem danh sách, Thêm phim, Sửa phim, Xóa phim")
    ]

    tc_aud_cases = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("AUD_GUI_01", "Kiểm tra chức năng hiển thị Bảng Nhật ký hoạt động thành công",
         "Kiểm tra hiển thị Bảng Audit Logs trên AdminAuditLogs.vue",
         "Bước 1: Mở màn hình Nhật ký hệ thống\nBước 2: Quan sát bảng dữ liệu\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị các cột: Thời gian, Người thực hiện, Vai trò, Hành động (CREATE/UPDATE/DELETE), Phân hệ, IP Address, Chi tiết thay đổi"),

        ("AUD_GUI_02", "Kiểm tra chức năng hiển thị Bộ lọc Phân hệ và Hành động thành công",
         "Kiểm tra hiển thị các dropdown lọc log",
         "Bước 1: Quan sát thanh lọc log\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị dropdown 'Tất cả phân hệ' (Movies, Schedules, RBAC, Users...) và dropdown 'Hành động' (CREATE, UPDATE, DELETE, LOGIN)"),

        ("AUD_GUI_03", "Kiểm tra chức năng hiển thị Modal Chi tiết JSON Log thành công",
         "Kiểm tra hiển thị Popup xem diff thay đổi trước/sau (Old State vs New State)",
         "Bước 1: Click vào icon con mắt tại 1 dòng log\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Modal JSON format rõ ràng hai cột: Dữ liệu cũ (Old Data) và Dữ liệu mới (New Data)"),

        ("__SECTION__", "KIỂM TRA TÌM KIẾM & BỘ LỌC DỮ LIỆU"),
        ("AUD_LOC_01", "Kiểm tra chức năng Lọc log theo Phân hệ thành công",
         "Kiểm tra chức năng lọc log thuộc phân hệ Phân quyền (RBAC)",
         "Bước 1: Chọn dropdown Phân hệ: 'Phân quyền (RBAC)'\nBước 2: Click button Lọc\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Phân hệ: 'RBAC'", "Bảng dữ liệu chỉ hiển thị các thao tác gán quyền, tước quyền quản trị"),

        ("AUD_LOC_02", "Kiểm tra chức năng Lọc log theo Hành động thành công",
         "Kiểm tra chức năng lọc log theo hành động 'DELETE'",
         "Bước 1: Chọn dropdown Hành động: 'DELETE'\nBước 2: Click button Lọc\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Hành động: 'DELETE'", "Bảng dữ liệu chỉ hiển thị các thao tác xóa bản ghi trong hệ thống"),

        ("AUD_LOC_03", "Kiểm tra chức năng Tìm kiếm log theo Email người thực hiện thành công",
         "Kiểm tra chức năng tìm kiếm log theo email tài khoản",
         "Bước 1: Nhập email 'admin@devcine.vn' vào ô tìm kiếm\nBước 2: Click Tìm kiếm\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tìm kiếm: 'admin@devcine.vn'", "Hiển thị toàn bộ lịch sử thao tác của tài khoản admin@devcine.vn"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("AUD_ERR_01", "Kiểm tra chức năng Không thể chỉnh sửa hoặc xóa Nhật ký hệ thống thành công",
         "Kiểm tra tính toàn vẹn (Immutability) của bảng Audit Logs (Read-only Guard)",
         "Bước 1: Kiểm tra giao diện màn hình Audit Logs\nBước 2: Quan sát xem có nút Xóa log hoặc Sửa log không\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Thao tác: Sửa/Xóa Log", "Không có nút Sửa/Xóa trên giao diện, Backend chặn mọi API DELETE/PUT đối với bảng audit_logs để đảm bảo tính minh bạch"),

        ("AUD_ERR_02", "Kiểm tra chức năng Tìm kiếm không có kết quả thành công",
         "Kiểm tra tìm kiếm với IP không tồn tại",
         "Bước 1: Nhập IP '192.168.999.999'\nBước 2: Click Tìm kiếm\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tìm kiếm: '192.168.999.999'", "Hiển thị thông báo 'Không tìm thấy bản ghi nhật ký nào'"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("AUD_FUNC_01", "Kiểm tra chức năng Tự động ghi Log khi Admin tạo mới Phim thành công",
         "Kiểm tra hệ thống tự động ghi lại vết audit log khi có thao tác CREATE phim",
         "Bước 1: Admin thực hiện thêm mới 1 bộ phim 'Dune: Part Two'\nBước 2: Mở màn hình Nhật ký hệ thống\nBước 3: Quan sát dòng log đầu tiên",
         "Hành động: CREATE Movie\nTên phim: 'Dune: Part Two'", "Ghi nhận log mới: Action=CREATE, Module=MOVIES, Actor=admin@devcine.vn kèm IP và thời gian chính xác"),

        ("AUD_FUNC_02", "Kiểm tra chức năng Phân trang danh sách Nhật ký hệ thống thành công",
         "Kiểm tra chuyển trang khi số lượng log lớn (> 20 bản ghi / trang)",
         "Bước 1: Click nút chuyển sang Trang 2\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
         "Thao tác: Chuyển trang 2", "Tải và hiển thị 20 dòng log tiếp theo mượt mà")
    ]

    tc_stf_sec = [("__FEATURE__", "QUẢN LÝ NHÂN SỰ")] + raw["MOD_ADMIN_STAFF"]["test_cases"]
    tc_rbc_sec = [("__FEATURE__", "PHÂN QUYỀN HỆ THỐNG (RBAC)")] + tc_rbac_cases
    tc_aud_sec = [("__FEATURE__", "NHẬT KÝ HỆ THỐNG (AUDIT LOGS)")] + tc_aud_cases
    tc_set_sec = [("__FEATURE__", "CÀI ĐẶT HỆ THỐNG")] + raw["MOD_ADMIN_SETTINGS"]["test_cases"]
    modules.append({
        "code": "MOD_ADMIN_SYSTEM_RBAC", "sheet": "Quản trị Hệ thống (RBAC)",
        "req": "Kiểm tra Quản lý Nhân sự, Ma trận phân quyền RBAC, Nhật ký hệ thống Audit Logs và Cấu hình Cài đặt hệ thống",
        "tester": "Phạm Thị Quỳnh Anh", "role": "Quản trị viên",
        "pre": "Quản trị viên mở StaffManager.vue / AdminPermissions.vue / AdminLogs.vue / AdminSettings.vue",
        "test_cases": tc_stf_sec + tc_rbc_sec + tc_aud_sec + tc_set_sec
    })

    return modules
