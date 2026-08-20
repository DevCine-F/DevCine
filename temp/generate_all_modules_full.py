# -*- coding: utf-8 -*-
"""
Script that writes out the complete build_senior_human_testreport.py with all 43 modules.
"""

def generate():
    with open("build_senior_human_testreport.py", "a", encoding="utf-8") as f:
        f.write('''
def build_all_human_modules():
    modules = []

    # =========================================================================
    # 1. ĐĂNG NHẬP (MOD_AUTH_LOGIN)
    # =========================================================================
    tc_dn = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("DN_GUI_01", "Kiểm tra chức năng hiển thị Form đăng nhập thành công",
         "Kiểm tra hiển thị đầy đủ các thành phần trên Form đăng nhập",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Quan sát và kiểm tra hiển thị Form đăng nhập\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị đầy đủ Form đăng nhập gồm: Ô nhập Số điện thoại hoặc Email, Ô nhập Mật khẩu, Nút Đăng nhập, Link Quên mật khẩu, Link Đăng ký tài khoản"),

        ("DN_GUI_02", "Kiểm tra chức năng hiển thị Nút Đăng nhập mờ (disabled) khi chưa nhập dữ liệu thành công",
         "Kiểm tra trạng thái nút Đăng nhập khi chưa nhập thông tin",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Quan sát nút Đăng nhập khi chưa điền dữ liệu\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Nút Đăng nhập ở trạng thái mờ (disabled), không thể click submit khi chưa nhập thông tin"),

        ("DN_GUI_03", "Kiểm tra chức năng hiển thị Icon ẩn/hiện mật khẩu thành công",
         "Kiểm tra hiển thị và tương tác icon con mắt trên ô Mật khẩu",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập mật khẩu và click vào icon con mắt\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Mật khẩu: 'Khach@123'", "Icon con mắt chuyển đổi trạng thái hiển thị dạng text rõ hoặc dạng dấu chấm tròn bảo mật (password)"),

        ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
        ("DN_EP_01", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị email trong khoảng từ 5 đến 100 ký tự",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email với 20 ký tự ('khachhang@gmail.com')\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'khachhang@gmail.com'\\nMật khẩu: 'Khach@123'", "Đăng nhập thành công vào hệ thống"),

        ("DN_EP_02", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị mật khẩu trong khoảng từ 6 đến 50 ký tự",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại hợp lệ ('0901234567')\\nBước 4: Nhập trường Mật khẩu trong khoảng từ 6 đến 50 ký tự ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: 'Khach@123'", "Đăng nhập thành công vào hệ thống"),

        ("DN_EP_03", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị email khoảng dưới của [5,100]",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email khoảng dưới ('a@b')\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'a@b' (3 ký tự)\\nMật khẩu: 'Khach@123'", "Hiển thị thông báo lỗi 'Email không hợp lệ (tối thiểu 5 ký tự)'"),

        ("DN_EP_04", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị email khoảng trên của [5,100]",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email khoảng trên (110 ký tự)\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: (Chuỗi email 110 ký tự)\\nMật khẩu: 'Khach@123'", "Hiển thị thông báo lỗi 'Email không được vượt quá 100 ký tự'"),

        ("DN_EP_05", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi giá trị mật khẩu là khoảng dưới của [6,50] ký tự",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại hợp lệ ('0901234567')\\nBước 4: Nhập trường Mật khẩu khoảng dưới ('12345' - 5 ký tự)\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: '12345' (5 ký tự)", "Đăng nhập không thành công, hiển thị thông báo lỗi 'Tài khoản hoặc mật khẩu không chính xác'"),

        ("DN_EP_06", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi giá trị mật khẩu là khoảng trên của [6,50] ký tự",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại hợp lệ ('0901234567')\\nBước 4: Nhập trường Mật khẩu khoảng trên (55 ký tự)\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: (Chuỗi 55 ký tự)", "Đăng nhập không thành công, hệ thống từ chối xác thực"),

        ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
        ("DN_BVA_01", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập thành công khi nhập giá trị email là giá trị biên min (5 ký tự)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email với 5 ký tự ('a@g.c')\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'a@g.c' (5 ký tự)\\nMật khẩu: 'Khach@123'", "Đăng nhập thành công vào hệ thống"),

        ("DN_BVA_02", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập thành công khi nhập giá trị email là giá trị cận biên trên min (6 ký tự)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email với 6 ký tự ('ab@g.c')\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'ab@g.c' (6 ký tự)\\nMật khẩu: 'Khach@123'", "Đăng nhập thành công vào hệ thống"),

        ("DN_BVA_03", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập thành công khi nhập giá trị email là giá trị cận biên dưới max (99 ký tự)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email với 99 ký tự\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: (Chuỗi email 99 ký tự)\\nMật khẩu: 'Khach@123'", "Đăng nhập thành công vào hệ thống"),

        ("DN_BVA_04", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập thành công khi nhập giá trị email là giá trị biên max (100 ký tự)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email với 100 ký tự\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: (Chuỗi email 100 ký tự)\\nMật khẩu: 'Khach@123'", "Đăng nhập thành công vào hệ thống"),

        ("DN_BVA_05", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập thất bại khi nhập giá trị email là giá trị cận biên dưới min (4 ký tự)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email với 4 ký tự ('a@.c')\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'a@.c' (4 ký tự)\\nMật khẩu: 'Khach@123'", "Hiển thị thông báo lỗi 'Email không hợp lệ (tối thiểu 5 ký tự)'"),

        ("DN_BVA_06", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập thất bại khi nhập giá trị email là giá trị cận biên trên max (101 ký tự)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email với 101 ký tự\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: (Chuỗi email 101 ký tự)\\nMật khẩu: 'Khach@123'", "Hiển thị thông báo lỗi 'Email không được vượt quá 100 ký tự'"),

        ("DN_BVA_07", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập thành công khi nhập giá trị mật khẩu là giá trị biên min (6 ký tự)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại hợp lệ ('0901234567')\\nBước 4: Nhập trường Mật khẩu với 6 ký tự ('Khach1')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: 'Khach1' (6 ký tự)", "Đăng nhập thành công vào hệ thống"),

        ("DN_BVA_08", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập thành công khi nhập giá trị mật khẩu là giá trị cận biên trên min (7 ký tự)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại hợp lệ ('0901234567')\\nBước 4: Nhập trường Mật khẩu với 7 ký tự ('Khach12')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: 'Khach12' (7 ký tự)", "Đăng nhập thành công vào hệ thống"),

        ("DN_BVA_09", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập thành công khi nhập giá trị mật khẩu là giá trị cận biên dưới max (49 ký tự)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại hợp lệ ('0901234567')\\nBước 4: Nhập trường Mật khẩu với 49 ký tự\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: (Chuỗi 49 ký tự)", "Đăng nhập thành công vào hệ thống"),

        ("DN_BVA_10", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập thành công khi nhập giá trị mật khẩu là giá trị biên max (50 ký tự)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại hợp lệ ('0901234567')\\nBước 4: Nhập trường Mật khẩu với 50 ký tự\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: (Chuỗi 50 ký tự)", "Đăng nhập thành công vào hệ thống"),

        ("DN_BVA_11", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị mật khẩu là giá trị cận biên dưới min (5 ký tự)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại hợp lệ ('0901234567')\\nBước 4: Nhập trường Mật khẩu với 5 ký tự ('12345')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: '12345' (5 ký tự)", "Đăng nhập không thành công, hiển thị thông báo lỗi 'Tài khoản hoặc mật khẩu không chính xác'"),

        ("DN_BVA_12", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị mật khẩu là giá trị cận biên trên max (51 ký tự)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại hợp lệ ('0901234567')\\nBước 4: Nhập trường Mật khẩu với 51 ký tự\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: (Chuỗi 51 ký tự)", "Đăng nhập không thành công, hệ thống từ chối xác thực"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("DN_ERR_01", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị email rỗng",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Để trống trường Email (Null)\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: Null\\nMật khẩu: 'Khach@123'", "Hiển thị thông báo lỗi 'Vui lòng nhập số điện thoại hoặc email'"),

        ("DN_ERR_02", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị mật khẩu là rỗng",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại hợp lệ ('0901234567')\\nBước 4: Để trống trường Mật khẩu (Null)\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: Null", "Hiển thị thông báo lỗi 'Vui lòng nhập mật khẩu'"),

        ("DN_ERR_03", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị email là khoảng trắng",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email toàn khoảng trắng\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: '          '\\nMật khẩu: 'Khach@123'", "Hiển thị thông báo lỗi 'Vui lòng nhập số điện thoại hoặc email'"),

        ("DN_ERR_04", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị mật khẩu là khoảng trắng",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại hợp lệ ('0901234567')\\nBước 4: Nhập trường Mật khẩu toàn khoảng trắng\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: '        '", "Hiển thị thông báo lỗi 'Vui lòng nhập mật khẩu'"),

        ("DN_ERR_05", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập thành công khi nhập khoảng trắng trước email",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email có khoảng trắng ở đầu ('   khachhang@gmail.com')\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: '   khachhang@gmail.com'\\nMật khẩu: 'Khach@123'", "Hệ thống tự động cắt khoảng trắng đầu và đăng nhập thành công"),

        ("DN_ERR_06", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập thành công khi nhập khoảng trắng sau email",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email có khoảng trắng ở cuối ('khachhang@gmail.com   ')\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'khachhang@gmail.com   '\\nMật khẩu: 'Khach@123'", "Hệ thống tự động cắt khoảng trắng cuối và đăng nhập thành công"),

        ("DN_ERR_07", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị email thiếu dấu @",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email thiếu dấu @ ('khachhanggmail.com')\\nBước 4: Nhập mật khẩu hợp lệ\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'khachhanggmail.com'\\nMật khẩu: 'Khach@123'", "Hiển thị thông báo lỗi 'Email không đúng định dạng'"),

        ("DN_ERR_08", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị email thiếu phần sau @",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email thiếu domain ('khachhang@')\\nBước 4: Nhập mật khẩu hợp lệ\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'khachhang@'\\nMật khẩu: 'Khach@123'", "Hiển thị thông báo lỗi 'Email không đúng định dạng'"),

        ("DN_ERR_09", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị email thiếu phần trước @",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email thiếu phần trước @ ('@gmail.com')\\nBước 4: Nhập mật khẩu hợp lệ\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: '@gmail.com'\\nMật khẩu: 'Khach@123'", "Hiển thị thông báo lỗi 'Email không đúng định dạng'"),

        ("DN_ERR_10", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị có nhiều dấu @",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email chứa 2 ký tự @ ('khachhang@@gmail.com')\\nBước 4: Nhập mật khẩu hợp lệ\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'khachhang@@gmail.com'\\nMật khẩu: 'Khach@123'", "Hiển thị thông báo lỗi 'Email không đúng định dạng'"),

        ("DN_ERR_11", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi nhập giá trị email có ký tự đặc biệt",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Email chứa ký tự đặc biệt lạ ('khach#hang$%@gmail.com')\\nBước 4: Nhập mật khẩu hợp lệ\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "Email: 'khach#hang$%@gmail.com'\\nMật khẩu: 'Khach@123'", "Hiển thị thông báo lỗi 'Email không đúng định dạng'"),

        ("DN_LOG_01", "Kiểm tra chức năng Đăng nhập thành công",
         "Kiểm tra chức năng đăng nhập thành công khi nhập số điện thoại đúng và mật khẩu đúng",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại đúng ('0901234567')\\nBước 4: Nhập trường Mật khẩu đúng ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: 'Khach@123'", "Đăng nhập thành công, hiển thị thông báo chào mừng và chuyển hướng về Trang chủ"),

        ("DN_LOG_02", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập thất bại khi nhập số điện thoại sai và mật khẩu đúng",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại sai/chưa đăng ký ('0999888777')\\nBước 4: Nhập trường Mật khẩu đúng ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0999888777'\\nMật khẩu: 'Khach@123'", "Đăng nhập không thành công, hiển thị thông báo lỗi 'Tài khoản hoặc mật khẩu không chính xác'"),

        ("DN_LOG_03", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập thất bại khi nhập số điện thoại đúng và mật khẩu sai",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại đúng ('0901234567')\\nBước 4: Nhập trường Mật khẩu sai ('SaiMatKhau@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0901234567'\\nMật khẩu: 'SaiMatKhau@123'", "Đăng nhập không thành công, hiển thị thông báo lỗi 'Tài khoản hoặc mật khẩu không chính xác'"),

        ("DN_LOG_04", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập thất bại khi nhập số điện thoại sai và mật khẩu sai",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách vãng lai\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập trường Số điện thoại sai ('0999888777')\\nBước 4: Nhập trường Mật khẩu sai ('SaiMatKhau@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0999888777'\\nMật khẩu: 'SaiMatKhau@123'", "Đăng nhập không thành công, hiển thị thông báo lỗi 'Tài khoản hoặc mật khẩu không chính xác'"),

        ("DN_LOG_05", "Kiểm tra chức năng Đăng nhập thất bại",
         "Kiểm tra chức năng đăng nhập khi tài khoản đó bị khóa",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò khách hàng có tài khoản bị khóa\\nBước 2: Truy cập vào màn hình Đăng nhập\\nBước 3: Nhập tài khoản đã bị khóa trong hệ thống ('0909999999')\\nBước 4: Nhập mật khẩu hợp lệ ('Khach@123')\\nBước 5: Click Đăng nhập\\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống",
         "SĐT: '0909999999' (Bị khóa)\\nMật khẩu: 'Khach@123'", "Hiển thị thông báo lỗi 'Tài khoản của bạn đã bị khóa. Vui lòng liên hệ CSKH để được hỗ trợ'")
    ]

    modules.append({
        "code": "MOD_AUTH_LOGIN", "sheet": "Đăng nhập",
        "req": "Kiểm tra Đăng nhập tài khoản khách hàng, nhân viên và quản trị viên",
        "tester": "Phạm Thị Quỳnh Anh", "role": "Khách hàng & Nhân viên",
        "pre": "Người dùng mở trình duyệt và truy cập vào trang Đăng nhập hệ thống DevCine",
        "test_cases": tc_dn
    })

    # =========================================================================
    # 2. CHỌN GHẾ & GIỮ CHỖ (MOD_CUST_SEAT_HOLD)
    # =========================================================================
    tc_st = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("ST_GUI_01", "Kiểm tra chức năng hiển thị Khối Loại vé thành công",
         "Kiểm tra hiển thị Bộ chọn số lượng vé Người lớn & U22 / HSSV",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào màn hình Booking Bước 1 (Chọn ghế)\\nBước 3: Quan sát khối Loại vé\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị đầy đủ 2 nhóm vé 'Người lớn' và 'U22 / HSSV' kèm nút [-]/[+] và nhãn '8 vé • đã chọn 0 ghế'"),

        ("ST_GUI_02", "Kiểm tra chức năng hiển thị Thanh chọn nhanh cụm ghế liền nhau (Block Selector) thành công",
         "Kiểm tra hiển thị các nút Block Selector",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào màn hình Booking Bước 1 (Chọn ghế)\\nBước 3: Quan sát thanh Block Selector\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị các nút: '1 ghế', '2 ghế liền', '3 ghế liền', '4 ghế liền', '5 ghế liền'"),

        ("ST_GUI_03", "Kiểm tra chức năng hiển thị Sơ đồ ma trận ghế và Màn hình chiếu thành công",
         "Kiểm tra hiển thị Sơ đồ ma trận ghế chuẩn phòng chiếu",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào màn hình Booking Bước 1 (Chọn ghế)\\nBước 3: Quan sát dải phát sáng MÀN HÌNH / SCREEN và ma trận ghế\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị dải phát sáng MÀN HÌNH, ma trận ghế phân loại rõ: Ghế Thường (xám), Ghế VIP (cam/vàng), Ghế Sweetbox đôi (hồng), Ghế đã bán (X xám), Ghế bảo trì (X gạch chéo)"),

        ("ST_GUI_04", "Kiểm tra chức năng hiển thị Cột tóm tắt thông tin đặt vé thành công",
         "Kiểm tra hiển thị Cột tóm tắt bên phải",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào màn hình Booking Bước 1 (Chọn ghế)\\nBước 3: Quan sát cột tóm tắt thông tin\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Poster phim, Tên phim, Suất chiếu, Phòng chiếu, Danh sách ghế đang chọn, Tạm tính tổng tiền và nút 'TIẾP TỤC ➔'"),

        ("ST_GUI_05", "Kiểm tra chức năng hiển thị Đồng hồ đếm ngược giữ chỗ 10 phút thành công",
         "Kiểm tra hiển thị Đồng hồ đếm ngược giữ chỗ",
         "Bước 1: Khách hàng click chọn ghế trên sơ đồ\\nBước 2: Quan sát góc trên màn hình\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị đồng hồ đếm ngược 10:00 giảm dần theo thời gian thực (10:00 -> 09:59...)"),

        ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
        ("ST_EP_01", "Kiểm tra chức năng Chọn số lượng vé thành công",
         "Kiểm tra chức năng Chọn số lượng vé khi tổng số vé trong khoảng từ 1 đến 8 vé",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Click nút [+] tại mục Người lớn 2 lần, mục U22 1 lần (Tổng 3 vé)\\nBước 3: Click chọn 3 ghế trên sơ đồ ma trận\\nBước 4: Click button 'TIẾP TỤC ➔'\\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "Loại vé: 2 Người lớn, 1 U22 (Tổng 3 vé)\\nGhế chọn: E05, E06, E07", "Hệ thống ghi nhận 3 vé hợp lệ, tính đúng tổng tiền và cho phép chuyển sang bước Combo F&B"),

        ("ST_EP_02", "Kiểm tra chức năng Chọn ghế thất bại",
         "Kiểm tra chức năng Chọn ghế khi số lượng vé là 0 vé (khoảng dưới của [1,8])",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Không tăng số lượng vé (Số vé = 0)\\nBước 3: Cố tình click trực tiếp vào ghế F05 trên sơ đồ\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng vé: 0 vé\\nThao tác: Click ghế F05", "Hiển thị thông báo hướng dẫn: 'Vui lòng chọn số lượng vé trước khi chọn vị trí ghế'"),

        ("ST_EP_03", "Kiểm tra chức năng Tăng số lượng vé thất bại",
         "Kiểm tra chức năng Tăng số lượng vé khi số lượng vượt quá 8 vé (khoảng trên của [1,8])",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Click nút [+] tăng số vé lên 8 vé\\nBước 3: Cố tình click tiếp nút [+] lần thứ 9\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số vé hiện có: 8 vé\\nThao tác: Click nút [+]", "Nút [+] bị vô hiệu hóa (disabled), hiển thị cảnh báo: 'Mỗi giao dịch chỉ được chọn tối đa 8 vé'"),

        ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
        ("ST_BVA_01", "Kiểm tra chức năng Chọn số lượng vé thành công",
         "Kiểm tra chức năng Chọn số lượng vé khi chọn giá trị biên min (1 vé)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Click nút [+] chọn 1 vé Người lớn\\nBước 3: Click chọn 1 ghế E05\\nBước 4: Click button 'TIẾP TỤC ➔'\\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng: 1 vé Người lớn\\nGhế chọn: E05", "Hệ thống chấp nhận 1 vé và cho phép chuyển tiếp"),

        ("ST_BVA_02", "Kiểm tra chức năng Chọn số lượng vé thành công",
         "Kiểm tra chức năng Chọn số lượng vé khi chọn giá trị cận biên trên min (2 vé)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Click nút [+] chọn 2 vé\\nBước 3: Click chọn 2 ghế E05, E06\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng: 2 vé\\nGhế chọn: E05, E06", "Hệ thống chấp nhận 2 vé"),

        ("ST_BVA_03", "Kiểm tra chức năng Chọn số lượng vé thành công",
         "Kiểm tra chức năng Chọn số lượng vé khi chọn giá trị cận biên dưới max (7 vé)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Tăng số lượng vé lên 7 vé\\nBước 3: Click chọn 7 ghế liền nhau\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng: 7 vé", "Hệ thống chấp nhận 7 vé"),

        ("ST_BVA_04", "Kiểm tra chức năng Chọn số lượng vé thành công",
         "Kiểm tra chức năng Chọn số lượng vé khi chọn giá trị biên max (8 vé)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Tăng số lượng vé lên 8 vé (đạt mức tối đa)\\nBước 3: Click chọn 8 ghế liền nhau\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng: 8 vé", "Hệ thống chấp nhận 8 vé tối đa"),

        ("ST_BVA_05", "Kiểm tra chức năng Giảm số lượng vé thất bại",
         "Kiểm tra chức năng Giảm số lượng vé khi số lượng là giá trị cận biên dưới min (0 vé)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Số vé đang ở mức 0\\nBước 3: Quan sát và click nút [-]\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng: 0 vé\\nThao tác: Click nút [-]", "Nút [-] bị vô hiệu hóa (disabled), không thể giảm xuống số âm"),

        ("ST_BVA_06", "Kiểm tra chức năng Tăng số lượng vé thất bại",
         "Kiểm tra chức năng Tăng số lượng vé khi số lượng là giá trị cận biên trên max (9 vé)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Đang có 8 vé trong giỏ\\nBước 3: Click nút [+] để tăng lên 9 vé\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng: 8 vé\\nThao tác: Click nút [+]", "Nút [+] bị vô hiệu hóa (disabled), không cho tăng lên 9 vé"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("ST_ERR_01", "Kiểm tra chức năng Chuyển bước thất bại",
         "Kiểm tra chức năng Chuyển bước khi chọn thiếu số lượng ghế so với số vé đã khai báo",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Chọn 3 vé nhưng chỉ click chọn 2 ghế trên sơ đồ\\nBước 3: Click button 'TIẾP TỤC ➔'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số vé khai báo: 3 vé\\nSố ghế đã chọn: 2 ghế", "Nút 'TIẾP TỤC ➔' bị vô hiệu hóa, thông báo: 'Bạn cần chọn đủ 3 ghế tương ứng với 3 vé'"),

        ("ST_ERR_02", "Kiểm tra chức năng Chọn thêm ghế thất bại",
         "Kiểm tra chức năng Chọn thêm ghế khi đã chọn đủ số lượng ghế tương ứng với số vé",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Khai báo 2 vé, đã chọn đủ 2 ghế E05, E06\\nBước 3: Cố tình click tiếp vào ghế thứ 3 E07\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số vé: 2 vé\\nGhế đã chọn: E05, E06\\nThao tác: Click tiếp E07", "Hệ thống từ chối chọn thêm và thông báo: 'Bạn đã chọn đủ 2 ghế'"),

        ("ST_ERR_03", "Kiểm tra chức năng Reset ghế tự động khi thay đổi số lượng vé",
         "Kiểm tra chức năng Tự động bỏ chọn ghế khi người dùng giảm số lượng vé đã khai báo",
         "Bước 1: Khách hàng khai báo 4 vé và đã chọn 4 ghế A01, A02, A03, A04\\nBước 2: Khách hàng click nút [-] giảm số vé xuống 2 vé\\nBước 3: Kiểm tra sơ đồ ghế",
         "Số vé cũ: 4 vé\\nSố vé mới: 2 vé", "Hệ thống tự động bỏ chọn toàn bộ ghế cũ, nhả khóa trên Redis và yêu cầu chọn lại 2 ghế mới"),

        ("ST_ERR_04", "Kiểm tra chức năng Bỏ chọn ghế (Deselect) thành công",
         "Kiểm tra chức năng Bỏ chọn ghế khi click lại vào ghế đang chọn",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Click vào ghế E05 đang được chọn (ghế đổi về màu xám trống)\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vị trí: E05 (Đang chọn)\\nThao tác: Click lại E05", "Ghế E05 chuyển về trạng thái trống (Available) và trừ tiền khỏi cột tóm tắt"),

        ("ST_ERR_05", "Kiểm tra chức năng Chọn ghế thất bại",
         "Kiểm tra chức năng Chọn ghế khi click vào ghế đã bán (Trạng thái SOLD)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Click vào ghế có ký hiệu 'X' màu xám đậm (ghế đã bán)\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vị trí: E08 (SOLD)", "Không có phản hồi chọn ghế, con trỏ chuột hiển thị 'not-allowed'"),

        ("ST_ERR_06", "Kiểm tra chức năng Chọn ghế thất bại",
         "Kiểm tra chức năng Chọn ghế khi click vào ghế đang bảo trì (Trạng thái MAINTENANCE)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Click vào ghế có ký hiệu cờ lê / gạch chéo bảo trì\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vị trí: B03 (MAINTENANCE)", "Không cho phép chọn, thông báo ghế đang trong quá trình bảo trì kỹ thuật"),

        ("ST_ERR_07", "Kiểm tra chức năng Chuyển bước thất bại",
         "Kiểm tra chức năng Bắt buộc đăng nhập khi khách vãng lai bấm Tiếp tục sau khi chọn ghế",
         "Bước 1: Khách vãng lai chưa đăng nhập tài khoản thực hiện chọn 2 ghế\\nBước 2: Click button 'TIẾP TỤC ➔'\\nBước 3: Kiểm tra phản hồi từ hệ thống",
         "Trạng thái: Chưa đăng nhập\\nThao tác: Click Tiếp tục", "Hệ thống hiển thị Modal yêu cầu Đăng nhập hoặc Đăng ký tài khoản, giữ nguyên 2 ghế đang chọn sau khi đăng nhập thành công"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("ST_FUNC_01", "Kiểm tra chức năng Chọn ghế Thường thành công",
         "Kiểm tra chức năng Chọn ghế khi click chọn ghế Thường còn trống",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Chọn 1 vé Người lớn, click chọn ghế A05\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Loại ghế: Ghế Thường\\nVị trí: A05", "Ghế đổi sang màu xanh sáng (Selected), cột tóm tắt ghi nhận 'A05 - Ghế Thường'"),

        ("ST_FUNC_02", "Kiểm tra chức năng Chọn ghế VIP thành công",
         "Kiểm tra chức năng Chọn ghế khi click chọn ghế VIP tại hàng trung tâm",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Chọn 1 vé Người lớn, click chọn ghế C05\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Loại ghế: Ghế VIP\\nVị trí: C05", "Ghế đổi sang màu xanh sáng, cột tóm tắt ghi nhận 'C05 - Ghế VIP' kèm phụ thu VIP chính xác"),

        ("ST_FUNC_03", "Kiểm tra chức năng Chọn ghế Sweetbox thành công",
         "Kiểm tra chức năng Chọn ghế khi click 1 ghế Sweetbox tự động chọn cả cặp đôi (2 chỗ)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Chọn 2 vé, click vào 1 ô của ghế Sweetbox đôi H01\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Loại ghế: SWEETBOX\\nVị trí: H01", "Hệ thống tự động chọn cả cặp đôi ghế H01-H02 (2 chỗ) đồng thời"),

        ("ST_FUNC_04", "Kiểm tra chức năng Chọn ghế theo Block thành công",
         "Kiểm tra chức năng Chọn nhanh khối ghế liền nhau bằng Block Selector",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Click nút '4 ghế liền', sau đó click vào ghế B03\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Khối chọn: 4 ghế liền nhau\\nVị trí click: B03", "Hệ thống tự động chọn liên tiếp 4 ghế B03, B04, B05, B06 còn trống trên cùng hàng"),

        ("ST_FUNC_05", "Kiểm tra chức năng Chọn ghế thất bại khi để lại 1 ghế trống ở đầu dãy (Orphan Seat Rule)",
         "Kiểm tra quy tắc Orphan Seat khi chọn ghế để lại 1 ghế trống duy nhất ở đầu dãy",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Chọn 2 vé, click chọn ghế E02, E03 (bỏ lại duy nhất ghế E01 ở đầu hàng E)\\nBước 3: Click button 'TIẾP TỤC ➔'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Ghế chọn: E02, E03\\nGhế trống cô lập: E01", "Hệ thống chặn chuyển bước và cảnh báo: 'Vui lòng không để trống 1 ghế đơn lẻ bên cạnh hoặc sát lối đi'"),

        ("ST_FUNC_06", "Kiểm tra chức năng Chọn ghế thất bại khi để lại 1 ghế trống ở cuối dãy (Orphan Seat Rule)",
         "Kiểm tra quy tắc Orphan Seat khi chọn ghế để lại 1 ghế trống duy nhất ở cuối dãy",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Dãy E có 12 ghế, chọn 2 vé và click chọn E10, E11 (bỏ lại duy nhất E12 ở cuối hàng)\\nBước 3: Click button 'TIẾP TỤC ➔'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Ghế chọn: E10, E11\\nGhế trống cô lập: E12", "Hệ thống chặn chuyển bước và cảnh báo: 'Vui lòng không để trống 1 ghế đơn lẻ bên cạnh hoặc sát lối đi'"),

        ("ST_FUNC_07", "Kiểm tra chức năng Chọn ghế thất bại khi để lại 1 ghế trống giữa 2 ghế đã đặt (Orphan Seat Rule)",
         "Kiểm tra quy tắc Orphan Seat khi chọn ghế để lại 1 ghế trống đơn lẻ giữa các ghế đã bán",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Ghế E04 và E06 đã có người mua, khách chọn 2 vé và click chọn E02, E03 (để lại duy nhất E05 trống)\\nBước 3: Click button 'TIẾP TỤC ➔'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Ghế đã bán: E04, E06\\nGhế chọn: E02, E03\\nGhế trống cô lập: E05", "Hệ thống chặn chuyển bước và cảnh báo không được để lại 1 ghế trống cô lập"),

        ("ST_FUNC_08", "Kiểm tra chức năng Chọn ghế thành công khi khoảng trống từ 2 ghế trở lên",
         "Kiểm tra quy tắc Orphan Seat khi chọn ghế để lại khoảng trống hợp lệ từ 2 ghế trở lên",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Ghế E04 đã bán, khách chọn ghế E07 (để lại 2 ghế trống E05, E06)\\nBước 3: Click button 'TIẾP TỤC ➔'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Ghế đã bán: E04\\nGhế chọn: E07\\nGhế trống còn lại: E05, E06 (2 ghế)", "Hợp lệ, hệ thống cho phép đi tiếp sang bước tiếp theo"),

        ("ST_FUNC_09", "Kiểm tra chức năng Concurrent Booking khi 2 người cùng chọn 1 ghế VIP trong cùng 1 giây",
         "Kiểm tra xử lý xung đột giữ ghế đồng thời giữa 2 quầy POS hoặc giữa Web và POS",
         "Bước 1: Người dùng A và Người dùng B cùng mở sơ đồ ghế của suất chiếu\\nBước 2: Cả 2 cùng click chọn ghế VIP F08 trong cùng 1 giây\\nBước 3: Kiểm tra phản hồi từ hệ thống",
         "Người A: Click F08 (t = 0ms)\\nNgười B: Click F08 (t = 10ms)", "Người A gửi trước được cấp Lock (ghế xanh); Người B bị Redis Lock từ chối ngay lập tức kèm thông báo: 'Ghế F08 vừa được chọn hoặc đã được bán ở nơi khác'"),

        ("ST_FUNC_10", "Kiểm tra chức năng Cập nhật trạng thái ghế real-time qua WebSocket khi giải phóng ghế",
         "Kiểm tra cơ chế WebSocket STOMP đồng bộ trạng thái ghế tức thời khi có người hủy ghế",
         "Bước 1: Khách A đang giữ ghế D05 (ghế D05 hiển thị màu đỏ/khóa trên màn hình người khác)\\nBước 2: Khách A hủy chọn ghế hoặc đóng trình duyệt\\nBước 3: Quan sát sơ đồ ghế trên màn hình người khác mà không cần F5 tải lại trang",
         "Sự kiện WebSocket: deselect(D05)", "Sơ đồ ghế tự động cập nhật D05 về trạng thái AVAILABLE (Trống) theo thời gian thực"),

        ("ST_FUNC_11", "Kiểm tra chức năng Tự động hủy giữ chỗ khi hết hạn đồng hồ đếm ngược 10 phút",
         "Kiểm tra xử lý Timeout khi hết 10 phút giữ chỗ",
         "Bước 1: Khách hàng chọn 2 ghế và giữ nguyên màn hình quá thời gian 10 phút\\nBước 2: Đồng hồ đếm ngược giảm về 00:00 (Timeout)\\nBước 3: Kiểm tra kết quả xử lý từ hệ thống",
         "Thời gian giữ chỗ: > 10 phút (Timeout 00:00)", "Hệ thống tự động nhả khóa ghế real-time, xóa sạch giỏ hàng tạm, đưa về bước 1 và thông báo: 'Đã hết thời gian giữ chỗ (10 phút). Vui lòng chọn lại ghế.'")
    ]

    modules.append({
        "code": "MOD_CUST_SEAT_HOLD", "sheet": "Chọn ghế & Giữ chỗ",
        "req": "Kiểm tra Chọn số lượng vé, Chọn ghế trên ma trận, Block Selector, Giữ chỗ 10 phút, Orphan Seat và Concurrent Booking",
        "tester": "Nguyễn Quang Huy", "role": "Khách hàng",
        "pre": "Khách hàng mở sơ đồ chọn ghế của suất chiếu trên hệ thống DevCine",
        "test_cases": tc_st
    })

    # =========================================================================
    # 3. COMBO F&B ONLINE (MOD_CUST_FNB)
    # =========================================================================
    tc_fnb = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("FNB_GUI_01", "Kiểm tra chức năng hiển thị Danh sách Combo F&B thành công",
         "Kiểm tra hiển thị Danh sách Combo F&B tại màn hình Booking (Bước 2)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Hoàn tất bước chọn ghế và chuyển sang bước 2 (Combo)\\nBước 3: Quan sát danh sách các gói Combo bắp nước\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị lưới các card món: Ảnh combo, Tên combo, Mô tả thành phần, Đơn giá và nút [-] / [+] số lượng"),

        ("FNB_GUI_02", "Kiểm tra chức năng hiển thị Modal tùy chọn FnbOptionModal thành công",
         "Kiểm tra hiển thị Popup tùy chọn vị bắp và loại nước ngọt",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào bước Combo F&B\\nBước 3: Click nút [+] tại gói 'Couple Combo'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị Modal FnbOptionModal cho phép chọn Vị bắp (Ngọt, Phô mai +15k, Trứng muối +15k) và Loại nước (Coca, Sprite, Fanta)"),

        ("FNB_GUI_03", "Kiểm tra chức năng hiển thị Nút Bỏ qua & Tiếp tục thành công",
         "Kiểm tra hiển thị nút Bỏ qua tại bước Combo",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào bước Combo F&B\\nBước 3: Quan sát thanh điều hướng phía dưới\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị nút 'TIẾP TỤC ➔' cho phép bỏ qua bước bắp nước nếu khách không có nhu cầu"),

        ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
        ("FNB_EP_01", "Kiểm tra chức năng Chọn số lượng Combo thành công",
         "Kiểm tra chức năng Chọn số lượng Combo khi chọn trong khoảng từ 1 đến 10 phần",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào bước Combo F&B\\nBước 3: Click nút [+] chọn 2 phần 'My Combo'\\nBước 4: Click button 'TIẾP TỤC ➔'\\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "Món chọn: 2 My Combo (Đơn giá 89.000đ)", "Hệ thống cộng 178.000đ vào tổng tiền và cho phép chuyển sang bước Ưu đãi / Voucher"),

        ("FNB_EP_02", "Kiểm tra chức năng Bỏ qua Combo thành công",
         "Kiểm tra chức năng Bỏ qua Combo khi số lượng F&B là 0 phần",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào bước Combo F&B\\nBước 3: Không click chọn món bắp nước nào (Số lượng = 0)\\nBước 4: Click button 'TIẾP TỤC ➔'\\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng F&B: 0 phần\\nThao tác: Click Tiếp tục", "Hệ thống cho phép bỏ qua hợp lệ và chuyển thẳng sang bước Ưu đãi"),

        ("FNB_EP_03", "Kiểm tra chức năng Tăng số lượng Combo thất bại",
         "Kiểm tra chức năng Tăng số lượng Combo khi số lượng vượt quá giới hạn cho phép (> 20 phần)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào bước Combo F&B\\nBước 3: Click nút [+] tăng liên tục đến 20 phần\\nBước 4: Cố tình click tiếp nút [+] lần thứ 21\\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng hiện có: 20 phần\\nThao tác: Click nút [+]", "Nút [+] bị vô hiệu hóa, hệ thống giới hạn tối đa 20 combo / đơn hàng online"),

        ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
        ("FNB_BVA_01", "Kiểm tra chức năng Chọn số lượng Combo thành công",
         "Kiểm tra chức năng Chọn số lượng Combo khi chọn giá trị biên min (1 phần)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào bước Combo F&B\\nBước 3: Click nút [+] chọn 1 phần Bắp ngọt\\nBước 4: Click button 'TIẾP TỤC ➔'\\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng: 1 phần", "Chọn thành công 1 phần và cập nhật tổng tiền"),

        ("FNB_BVA_02", "Kiểm tra chức năng Chọn số lượng Combo thành công",
         "Kiểm tra chức năng Chọn số lượng Combo khi chọn giá trị cận biên trên min (2 phần)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào bước Combo F&B\\nBước 3: Click nút [+] chọn 2 phần\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng: 2 phần", "Chọn thành công 2 phần"),

        ("FNB_BVA_03", "Kiểm tra chức năng Chọn số lượng Combo thành công",
         "Kiểm tra chức năng Chọn số lượng Combo khi chọn giá trị cận biên dưới max (19 phần)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào bước Combo F&B\\nBước 3: Click nút [+] chọn 19 phần\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng: 19 phần", "Chọn thành công 19 phần"),

        ("FNB_BVA_04", "Kiểm tra chức năng Chọn số lượng Combo thành công",
         "Kiểm tra chức năng Chọn số lượng Combo khi chọn giá trị biên max (20 phần)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào bước Combo F&B\\nBước 3: Click nút [+] chọn 20 phần\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng: 20 phần", "Chọn thành công 20 phần (đạt mức tối đa)"),

        ("FNB_BVA_05", "Kiểm tra chức năng Giảm số lượng Combo thất bại",
         "Kiểm tra chức năng Giảm số lượng Combo khi số lượng là giá trị cận biên dưới min (0 phần)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Món đang có số lượng là 0\\nBước 3: Quan sát và click nút [-]\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng: 0 phần\\nThao tác: Click nút [-]", "Nút [-] bị vô hiệu hóa (disabled), không thể giảm xuống số âm"),

        ("FNB_BVA_06", "Kiểm tra chức năng Tăng số lượng Combo thất bại",
         "Kiểm tra chức năng Tăng số lượng Combo khi số lượng là giá trị cận biên trên max (21 phần)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Món đã đạt 20 phần\\nBước 3: Click nút [+] để tăng lên 21\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng: 20 phần\\nThao tác: Click nút [+]", "Nút [+] bị vô hiệu hóa (disabled), không cho tăng lên 21 phần"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("FNB_ERR_01", "Kiểm tra chức năng Giảm số lượng Combo thành công",
         "Kiểm tra chức năng Giảm số lượng Combo khi click nút [-] tại món đã chọn",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Đang có 2 Bắp phô mai trong giỏ\\nBước 3: Click nút [-] 1 lần\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng cũ: 2 phần\\nThao tác: Click nút [-]", "Số lượng giảm còn 1 phần, tổng tiền tự động trừ đi 1 phần bắp"),

        ("FNB_ERR_02", "Kiểm tra chức năng Đóng Modal tùy chọn thành công",
         "Kiểm tra chức năng Hủy chọn tùy chọn khi bấm nút Đóng hoặc click ra ngoài Modal",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Mở Modal FnbOptionModal\\nBước 3: Click icon [X] hoặc click vùng tối bên ngoài\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Thao tác: Đóng Modal", "Modal đóng lại, không thêm combo vào giỏ hàng và không thay đổi tổng tiền"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("FNB_FUNC_01", "Kiểm tra chức năng Tùy chọn vị Combo có tính phụ thu thành công",
         "Kiểm tra chức năng Tùy chọn vị Combo khi chọn vị bắp Phô mai có tính phụ thu",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào màn hình Chọn Combo bắp nước\\nBước 3: Chọn đổi vị bắp sang Phô mai (+15.000đ)\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Món chọn: Bắp rang bơ\\nVị đổi: Phô mai\\nPhụ thu: +15.000đ", "Tổng tiền tạm tính cộng thêm 15.000đ chính xác"),

        ("FNB_FUNC_02", "Kiểm tra chức năng Phân trang danh sách Combo F&B thành công",
         "Kiểm tra chức năng Phân trang khi danh sách có trên 6 món F&B",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào bước Combo F&B\\nBước 3: Click chuyển sang Trang 2\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Thao tác: Chuyển trang 2", "Hiển thị 6 món tiếp theo trong thực đơn mượt mà")
    ]

    modules.append({
        "code": "MOD_CUST_FNB", "sheet": "Combo F&B online",
        "req": "Kiểm tra Chọn combo bắp nước, Modal tùy chọn vị bắp FnbOptionModal và Phân trang",
        "tester": "Nguyễn Quang Huy", "role": "Khách hàng",
        "pre": "Khách hàng hoàn tất bước chọn ghế và mở bước Combo trên BookingView",
        "test_cases": tc_fnb
    })

    # =========================================================================
    # 4. THANH TOÁN VNPAY (MOD_CUST_PAYMENT - BookingView.vue Step 4) - NO EP/BVA
    # =========================================================================
    tc_pay = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("PAY_GUI_01", "Kiểm tra chức năng hiển thị Tùy chọn Phương thức thanh toán thành công",
         "Kiểm tra hiển thị các Phương thức thanh toán tại Booking (Bước 4)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Hoàn tất các bước chọn ghế, combo, ưu đãi và chuyển sang bước 4 (Thanh toán)\\nBước 3: Quan sát các tùy chọn phương thức thanh toán\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị 2 phương thức lựa chọn: 'Cổng thanh toán VNPAY' và 'Chuyển khoản VietQR' kèm logo nhận diện chính thức"),

        ("PAY_GUI_02", "Kiểm tra chức năng hiển thị Khung mã QR chuyển khoản VietQR tự sinh thành công",
         "Kiểm tra hiển thị Mã QR chuyển khoản khi chọn phương thức VietQR",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Chọn phương thức 'Chuyển khoản VietQR'\\nBước 3: Quan sát khung hiển thị mã QR và thông tin tài khoản\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị mã QR động chuẩn VietQR chứa đúng số tiền thanh toán, số tài khoản ngân hàng rạp và nội dung chuyển khoản mã đơn hàng"),

        ("PAY_GUI_03", "Kiểm tra chức năng hiển thị Bảng tóm tắt tổng thanh toán (Sidebar Summary) thành công",
         "Kiểm tra hiển thị Bảng tóm tắt tổng thanh toán bên phải",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Khách hàng\\nBước 2: Truy cập vào bước Thanh toán\\nBước 3: Quan sát cột tóm tắt bên phải\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị chi tiết: Tiền vé, Tiền bắp nước F&B, Mức giảm giá Voucher, Tổng thanh toán cuối cùng và nút 'XÁC NHẬN THANH TOÁN'"),

        ("PAY_GUI_04", "Kiểm tra chức năng hiển thị Đồng hồ đếm ngược giữ chỗ còn lại thành công",
         "Kiểm tra hiển thị thời gian còn lại trước khi đơn hàng hết hạn giữ chỗ",
         "Bước 1: Truy cập vào bước Thanh toán\\nBước 2: Quan sát đồng hồ đếm ngược phía trên\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị đồng hồ đếm ngược thời gian giữ chỗ (ví dụ: '04:35') đồng bộ xuyên suốt từ bước 1 đến bước 4"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("PAY_ERR_01", "Kiểm tra chức năng Xử lý khi Khách hàng hủy giao dịch trên Cổng VNPAY (Mã lỗi 24) thành công",
         "Kiểm tra xử lý hủy đơn khi người dùng chủ động bấm 'Hủy giao dịch' trên giao diện VNPAY",
         "Bước 1: Chuyển sang cổng thanh toán VNPAY\\nBước 2: Click button 'Hủy giao dịch' trên màn hình VNPAY\\nBước 3: Nhận điều hướng quay trở lại website DevCine\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Mã phản hồi VNPAY: '24' (Khách hàng hủy giao dịch)", "Hệ thống hiển thị thông báo 'Giao dịch đã bị hủy bởi người dùng' và tự động giải phóng ghế đang giữ về trạng thái trống"),

        ("PAY_ERR_02", "Kiểm tra chức năng Xử lý khi Thẻ ngân hàng không đủ số dư (Mã lỗi 51) thành công",
         "Kiểm tra xử lý khi tài khoản / thẻ của khách không đủ tiền thanh toán",
         "Bước 1: Nhập thông tin thẻ test có số dư không đủ trên cổng VNPAY\\nBước 2: Xác nhận thanh toán\\nBước 3: Nhận kết quả từ VNPAY\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Mã phản hồi VNPAY: '51' (Tài khoản không đủ số dư)", "Hiển thị thông báo lỗi 'Tài khoản của quý khách không đủ số dư để thực hiện giao dịch', cho phép khách chọn thẻ khác thanh toán lại"),

        ("PAY_ERR_03", "Kiểm tra chức năng Xử lý khi Nhập sai mã OTP quá 3 lần (Mã lỗi 09) thành công",
         "Kiểm tra xử lý khi khách hàng nhập sai mã xác thực OTP nhiều lần trên VNPAY",
         "Bước 1: Nhập sai mã OTP xác thực 3 lần liên tiếp trên giao diện VNPAY\\nBước 2: Nhận kết quả từ VNPAY\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Mã phản hồi VNPAY: '09' (Thẻ chưa đăng ký dịch vụ / Sai OTP quá số lần)", "Giao dịch bị từ chối, hiển thị thông báo lỗi 'Xác thực OTP không thành công, giao dịch bị hủy'"),

        ("PAY_ERR_04", "Kiểm tra chức năng Xử lý khi Giao dịch VNPAY hết hạn chờ thanh toán (Mã lỗi 11) thành công",
         "Kiểm tra xử lý khi khách hàng để màn hình VNPAY quá thời gian quy định",
         "Bước 1: Chuyển sang cổng VNPAY và giữ nguyên màn hình quá 15 phút\\nBước 2: Nhận phản hồi hết hạn từ VNPAY\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Mã phản hồi VNPAY: '11' (Hết hạn chờ thanh toán)", "Hệ thống hủy đơn hàng, giải phóng ghế và thông báo: 'Phiên giao dịch thanh toán đã hết hạn'"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("PAY_FUNC_01", "Kiểm tra chức năng Thanh toán VNPAY thành công (Mã giao dịch 00)",
         "Kiểm tra luồng thanh toán VNPAY thành công và xử lý Webhook IPN Backend",
         "Bước 1: Khách hàng nhập thẻ test và xác thực OTP thành công trên cổng VNPAY (Mã phản hồi 00)\\nBước 2: Hệ thống Backend nhận Webhook IPN từ VNPAY và xác thực chữ ký bảo mật SHA512\\nBước 3: Kiểm tra cập nhật trạng thái đơn hàng và chuyển hướng",
         "Mã giao dịch VNPAY: '00' (Giao dịch thành công)\\nChữ ký bảo mật: Hợp lệ", "Đơn hàng chuyển trạng thái CONFIRMED, tự động sinh mã vé QR độc nhất, tích điểm hội viên và chuyển sang màn hình BookingSuccessView"),

        ("PAY_FUNC_02", "Kiểm tra chức năng Thanh toán qua Chuyển khoản VietQR thành công",
         "Kiểm tra chức năng Thanh toán khi chọn phương thức Chuyển khoản qua mã VietQR",
         "Bước 1: Chọn phương thức thanh toán 'Chuyển khoản VietQR'\\nBước 2: Khách hàng mở app ngân hàng quét mã QR hiển thị trên màn hình\\nBước 3: Click button 'Tôi đã chuyển khoản'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Phương thức: VietQR\\nTổng tiền: 190.000 VNĐ", "Hệ thống kiểm tra giao dịch và chuyển sang màn hình Đặt vé thành công khi nhận được tiền"),

        ("PAY_FUNC_03", "Kiểm tra chức năng Thanh toán đơn hàng 0 đồng (Miễn phí 100% bằng Voucher) thành công",
         "Kiểm tra chức năng Thanh toán khi đơn hàng được miễn phí hoàn toàn nhờ áp voucher 100%",
         "Bước 1: Áp dụng voucher miễn phí vé 100% tại bước 3 (Tổng thanh toán = 0 VNĐ)\\nBước 2: Chuyển sang bước 4, click button 'XÁC NHẬN ĐẶT VÉ'\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tổng thanh toán: 0 VNĐ (Voucher 100%)", "Hệ thống tự động hoàn tất đơn đặt vé mà không cần chuyển sang cổng thanh toán, sinh mã vé QR ngay lập tức"),

        ("PAY_FUNC_04", "Kiểm tra chức năng Đối soát tự động (QueryDR) và Rollback khi gặp sự cố mạng trung gian",
         "Kiểm tra cơ chế đối soát giao dịch và Rollback khi mạng giữa VNPAY và rạp bị gián đoạn",
         "Bước 1: Khách hàng đã quét mã thanh toán nhưng mạng Internet giữa VNPAY và máy chủ DevCine bị ngắt kết nối tạm thời\\nBước 2: Hệ thống kích hoạt cơ chế tự động gọi API Query Transaction Status của VNPAY để kiểm tra\\nBước 3: Kiểm tra xử lý đối soát",
         "Trạng thái QueryDR: Giao dịch chưa ghi nhận tiền về tài khoản rạp", "Hệ thống tự động Rollback giao dịch, giải phóng ghế về trạng thái trống (Available) để tránh giam ghế của rạp"),

        ("PAY_FUNC_05", "Kiểm tra chức năng Tự động hủy đơn và giải phóng ghế khi hết hạn 10 phút giữ chỗ",
         "Kiểm tra xử lý khi khách hàng đang ở bước thanh toán nhưng đồng hồ đếm ngược giảm về 00:00",
         "Bước 1: Khách hàng giữ nguyên màn hình bước 4 (Thanh toán) quá 10 phút\\nBước 2: Đồng hồ đếm ngược giảm về 00:00 (Timeout)\\nBước 3: Kiểm tra kết quả xử lý từ hệ thống",
         "Thời gian giữ chỗ: > 10 phút (Timeout 00:00)", "Hệ thống tự động nhả khóa ghế real-time trên Redis/DB, xóa đơn tạm, đưa về bước 1 và thông báo: 'Đã hết thời gian giữ chỗ (10 phút). Vui lòng chọn lại ghế.'")
    ]

    modules.append({
        "code": "MOD_CUST_PAYMENT", "sheet": "Thanh toán VNPAY",
        "req": "Kiểm tra Cổng thanh toán VNPAY, Chuyển khoản VietQR, Rollback, Mã lỗi VNPAY và Sinh vé QR",
        "tester": "Nguyễn Quang Huy", "role": "Khách hàng",
        "pre": "Khách hàng hoàn tất chọn ghế, combo, voucher và mở bước Thanh toán",
        "test_cases": tc_pay
    })

    # =========================================================================
    # 5. PHÂN QUYỀN HỆ THỐNG (MOD_ADMIN_RBAC - AdminPermissions.vue) - NO EP/BVA
    # =========================================================================
    tc_rbac = [
        ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
        ("RBC_GUI_01", "Kiểm tra chức năng hiển thị Khối chọn vai trò và Chuyển chế độ cấu hình thành công",
         "Kiểm tra hiển thị các nút chọn vai trò và switch mode trên màn hình Phân quyền",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Truy cập vào màn hình Phân quyền hệ thống (AdminPermissions.vue)\\nBước 3: Quan sát khối chọn vai trò và chế độ\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị 3 nút vai trò 'ADMIN', 'MANAGER', 'STAFF' và switch chuyển đổi 'Cấu hình theo vai trò' / 'Cấu hình theo nhân viên'"),

        ("RBC_GUI_02", "Kiểm tra chức năng hiển thị 4 Tab danh mục phân hệ quyền thành công",
         "Kiểm tra hiển thị các Tab phân hệ quyền",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Mở màn hình Phân quyền hệ thống\\nBước 3: Quan sát thanh tab phân hệ\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị đầy đủ 4 tab: 'TỔNG QUAN', 'NGHIỆP VỤ', 'NỘI DUNG', 'HỆ THỐNG'"),

        ("RBC_GUI_03", "Kiểm tra chức năng hiển thị Bảng checkbox ma trận quyền phân theo nhóm tính năng thành công",
         "Kiểm tra hiển thị các nhóm quyền chi tiết",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Chọn tab 'HỆ THỐNG'\\nBước 3: Quan sát ma trận checkbox quyền\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị các khối quyền nhóm: 'HỆ THỐNG CỤM RẠP', 'NHÂN SỰ', 'CHĂM SÓC KHÁCH HÀNG' kèm các checkbox Xem, Thêm, Sửa, Xóa"),

        ("RBC_GUI_04", "Kiểm tra chức năng hiển thị Các nút thao tác hàng loạt thành công",
         "Kiểm tra hiển thị các nút chọn nhanh / bỏ nhanh quyền",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Quan sát phía trên danh sách quyền\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị 2 nút tác vụ nhanh: '= BỎ TẤT CẢ TRONG TAB NÀY' và '🚫 BỎ TOÀN BỘ QUYỀN' (hoặc CHỌN TẤT CẢ)"),

        ("RBC_GUI_05", "Kiểm tra chức năng hiển thị Thanh tóm tắt quyền và Nút Lưu thiết lập thành công",
         "Kiểm tra hiển thị Bottom Bar tóm tắt quyền",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Quan sát thanh Bottom Bar cố định phía dưới màn hình\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "N/A", "Hiển thị tóm tắt danh sách quyền đang gán cho vai trò và nút 'LƯU THIẾT LẬP PHÂN QUYỀN'"),

        ("RBC_GUI_06", "Kiểm tra chức năng hiển thị Badge Admin Toàn quyền bị khóa thành công",
         "Kiểm tra hiển thị trạng thái bảo vệ quyền tối cao cho vai trò ADMIN",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Click chọn vai trò 'ADMIN'\\nBước 3: Quan sát trạng thái các checkbox và nút Lưu\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vai trò: ADMIN", "Hiển thị badge vàng 'ADMIN TOÀN QUYỀN 🔒', toàn bộ checkbox ở trạng thái đã chọn và bị khóa (read-only)"),

        ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
        ("RBC_ERR_01", "Kiểm tra chức năng Tước quyền Quản trị viên tối cao thất bại",
         "Kiểm tra chức năng Chặn tước quyền cốt lõi của vai trò ADMIN (Superuser Guard)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Chọn vai trò 'ADMIN'\\nBước 3: Cố tình click bỏ tích chọn các quyền hệ thống\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vai trò: ADMIN\\nThao tác: Bỏ quyền Admin", "Hệ thống khóa toàn bộ checkbox không cho chỉnh sửa, hiển thị badge 'ADMIN TOÀN QUYỀN 🔒' để ngăn chặn lỗi tự khóa tài khoản Admin"),

        ("RBC_ERR_02", "Kiểm tra chức năng Cảnh báo thay đổi chưa lưu khi chuyển vai trò thành công",
         "Kiểm tra chức năng Cảnh báo khi người dùng chuyển sang vai trò khác mà chưa bấm Lưu phân quyền",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Đang chỉnh sửa quyền cho STAFF (chưa bấm Lưu)\\nBước 3: Click chọn chuyển sang vai trò MANAGER\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Trạng thái: Có thay đổi quyền chưa lưu\\nThao tác: Chuyển vai trò", "Hiển thị Modal cảnh báo: 'Bạn có thay đổi chưa lưu. Bạn có muốn lưu trước khi chuyển vai trò không?'"),

        ("RBC_ERR_03", "Kiểm tra chức năng Chặn truy cập API trái phép từ Backend (Enforce RBAC) thành công",
         "Kiểm tra cơ chế @perm.can(...) tại Backend khi nhân viên bị tước quyền gọi trực tiếp API",
         "Bước 1: Tài khoản nhân viên STAFF bị tước quyền xóa phim (movies:delete)\\nBước 2: Dùng token của nhân viên STAFF gửi request HTTP DELETE /api/v1/admin/movies/123\\nBước 3: Kiểm tra phản hồi từ Backend",
         "Token: STAFF (Không có quyền movies:delete)\\nRequest: DELETE /api/v1/admin/movies/123", "Backend từ chối thực thi và trả về mã lỗi HTTP '403 Forbidden'"),

        ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
        ("RBC_FUNC_01", "Kiểm tra chức năng Cấu hình quyền vai trò Quản lý thành công",
         "Kiểm tra chức năng Thiết lập quyền cho vai trò Quản lý (MANAGER)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Click chọn vai trò 'MANAGER'\\nBước 3: Chọn các quyền: Xem báo cáo, Bán vé POS, Thực đơn F&B, Xử lý sự cố ghế, Quản lý phim\\nBước 4: Click button 'Lưu thiết lập phân quyền'\\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vai trò: MANAGER\\nQuyền gán: dashboard_stats(view), pos_ticketing(view,add), fnb_menu(view,add,edit), movies(view,add,edit)", "Lưu thành công, hiển thị thông báo 'Cập nhật phân quyền vai trò MANAGER thành công'"),

        ("RBC_FUNC_02", "Kiểm tra chức năng Cấu hình quyền vai trò Nhân viên thành công",
         "Kiểm tra chức năng Thiết lập quyền cho vai trò Nhân viên (STAFF)",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Click chọn vai trò 'STAFF'\\nBước 3: Chỉ chọn quyền: Vào quầy bán vé POS, Xem thực đơn F&B, Soát vé Check-in\\nBước 4: Click button 'Lưu thiết lập phân quyền'\\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vai trò: STAFF\\nQuyền gán: pos_ticketing(view,add), fnb_menu(view), ticket_checkin(view,add)", "Lưu thành công, nhân viên STAFF chỉ truy cập được màn hình POS và Check-in"),

        ("RBC_FUNC_03", "Kiểm tra chức năng Chọn tất cả quyền trong Tab thành công",
         "Kiểm tra chức năng Chọn tất cả quyền trong tab hiện tại",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Chọn vai trò MANAGER, mở tab 'NỘI DUNG'\\nBước 3: Click button 'CHỌN TẤT CẢ TRONG TAB NÀY'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tab: NỘI DUNG\\nThao tác: Click Chọn tất cả trong tab", "Toàn bộ checkbox quyền trong tab Nội dung (Phim, Lịch chiếu, Banner, Khuyến mãi, Giá vé, Khách hàng) được tích chọn đồng thời"),

        ("RBC_FUNC_04", "Kiểm tra chức năng Bỏ tất cả quyền trong Tab thành công",
         "Kiểm tra chức năng Bỏ tất cả quyền trong tab hiện tại",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Chọn vai trò STAFF, mở tab 'HỆ THỐNG'\\nBước 3: Click button 'BỎ TẤT CẢ TRONG TAB NÀY'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Tab: HỆ THỐNG\\nThao tác: Click Bỏ tất cả trong tab", "Toàn bộ checkbox quyền trong tab Hệ thống bị bỏ tích chọn sạch sẽ"),

        ("RBC_FUNC_05", "Kiểm tra chức năng Chọn toàn bộ quyền của vai trò thành công",
         "Kiểm tra chức năng Chọn toàn bộ quyền trên tất cả 4 tab",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Chọn vai trò MANAGER\\nBước 3: Click button 'CHỌN TOÀN BỘ QUYỀN'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vai trò: MANAGER\\nThao tác: Click Chọn toàn bộ quyền", "Tất cả quyền trên 4 tab (Tổng quan, Nghiệp vụ, Nội dung, Hệ thống) được tích chọn 100%"),

        ("RBC_FUNC_06", "Kiểm tra chức năng Bỏ toàn bộ quyền của vai trò thành công",
         "Kiểm tra chức năng Tước bỏ toàn bộ quyền trên tất cả 4 tab",
         "Bước 1: Truy cập vào hệ thống DevCine với vai trò Quản trị viên\\nBước 2: Chọn vai trò STAFF\\nBước 3: Click button 'BỎ TOÀN BỘ QUYỀN'\\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
         "Vai trò: STAFF\\nThao tác: Click Bỏ toàn bộ quyền", "Toàn bộ quyền của vai trò STAFF bị xóa sạch (0 quyền)"),

        ("RBC_FUNC_07", "Kiểm tra chức năng Cấp quyền cho vai trò khi số lượng quyền là 0 quyền",
         "Kiểm tra khi tước toàn bộ quyền của vai trò STAFF",
         "Bước 1: Bỏ toàn bộ quyền của vai trò STAFF và bấm Lưu\\nBước 2: Dùng tài khoản STAFF đăng nhập vào hệ thống\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng quyền gán: 0 quyền", "Nhân viên STAFF đăng nhập thành công nhưng thanh Sidebar trống trơn, không thể truy cập bất kỳ màn hình quản trị nào"),

        ("RBC_FUNC_08", "Kiểm tra chức năng Cấp quyền cho vai trò khi số lượng quyền là 1 quyền duy nhất",
         "Kiểm tra khi chỉ cấp đúng 1 quyền duy nhất cho vai trò STAFF",
         "Bước 1: Chỉ cấp quyền 'Xem lịch chiếu' cho STAFF và bấm Lưu\\nBước 2: Dùng tài khoản STAFF đăng nhập\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Số lượng quyền gán: 1 quyền", "Nhân viên STAFF chỉ nhìn thấy và chỉ truy cập được duy nhất 1 màn hình Lịch chiếu"),

        ("RBC_FUNC_09", "Kiểm tra chức năng Cập nhật quyền truy cập tức thì sau khi Lưu phân quyền thành công",
         "Kiểm tra quyền hạn của nhân viên được cập nhật real-time ngay sau khi Admin bấm Lưu",
         "Bước 1: Admin cấp thêm quyền 'Báo cáo doanh thu' cho vai trò STAFF và bấm Lưu\\nBước 2: Nhân viên STAFF đang mở trang web thực hiện tải lại trang\\nBước 3: Kiểm tra hiển thị Menu Báo cáo",
         "Vai trò: STAFF\\nQuyền mới: dashboard_stats(view)", "Menu Báo cáo doanh thu xuất hiện ngay lập tức trên thanh Sidebar của nhân viên STAFF"),

        ("RBC_FUNC_10", "Kiểm tra chức năng Phân quyền tùy biến theo từng nhân viên (User-level Override) thành công",
         "Kiểm tra cơ chế Override Allow/Deny phân quyền độc lập cho từng nhân viên cụ thể",
         "Bước 1: Chuyển sang chế độ 'Cấu hình theo nhân viên' (User mode)\\nBước 2: Chọn nhân viên 'Văn Minh Khôi' (Vai trò gốc STAFF)\\nBước 3: Cấp thêm quyền đặc thù 'Xử lý sự cố ghế (incident_handling:handle)' cho riêng nhân viên Khôi\\nBước 4: Click button 'Lưu thiết lập'\\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
         "Nhân viên: Văn Minh Khôi (STAFF)\\nQuyền Override Allow: incident_handling(handle)", "Riêng nhân viên Khôi được phép xử lý sự cố đổi ghế, các nhân viên STAFF khác vẫn bị chặn bình thường"),

        ("RBC_FUNC_11", "Kiểm tra chức năng Checkbox chọn tất cả nhóm tính năng (Toggle Feature All) thành công",
         "Kiểm tra chức năng Tích chọn master checkbox của nhóm tính năng",
         "Bước 1: Tại tab 'NỘI DUNG', click vào master checkbox '☑️ QUẢN LÝ DANH SÁCH PHIM'\\nBước 2: Quan sát các checkbox con bên trong\\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
         "Thao tác: Click Master Checkbox 'QUẢN LÝ DANH SÁCH PHIM'", "Tự động tích chọn/bỏ chọn đồng thời cả 4 action: Xem danh sách, Thêm phim, Sửa phim, Xóa phim")
    ]

    modules.append({
        "code": "MOD_ADMIN_RBAC", "sheet": "Phân quyền hệ thống",
        "req": "Kiểm tra Ma trận phân quyền RBAC, Chọn vai trò, Override theo nhân viên, Toggle All và Bảo vệ Admin tối cao",
        "tester": "Phạm Thị Quỳnh Anh", "role": "Quản trị viên",
        "pre": "Quản trị viên mở màn hình Phân quyền hệ thống (AdminPermissions.vue) trên Admin Dashboard",
        "test_cases": tc_rbac
    })
''')
    print("Part 1 regenerated without EP/BVA in VNPAY and RBAC.")

if __name__ == '__main__':
    generate()
