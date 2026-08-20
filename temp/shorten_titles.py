# -*- coding: utf-8 -*-
import sys
sys.stdout.reconfigure(encoding='utf-8')

file_path = r'c:\Users\ADMIN\OneDrive\Desktop\DATN\devcine\consolidated_modules_builder.py'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

replacements = {
    '"ĐĂNG NHẬP HỆ THỐNG & QUÊN MẬT KHẨU"': '"ĐĂNG NHẬP & QUÊN MẬT KHẨU"',
    '"HỒ SƠ CÁ NHÂN & ĐỔI MẬT KHẨU"': '"HỒ SƠ & ĐỔI MẬT KHẨU"',
    '"CHỌN SUẤT CHIẾU, SƠ ĐỒ GHẾ & GIỮ CHỖ"': '"CHỌN GHẾ & GIỮ CHỖ"',
    '"CHỌN COMBO BẮP NƯỚC & TÙY CHỌN VỊ F&B"': '"CHỌN BẮP NƯỚC (F&B)"',
    '"THANH TOÁN VNPAY, VIETQR & SINH VÉ QR"': '"THANH TOÁN & NHẬN VÉ"',
    '"LỊCH SỬ ĐẶT VÉ & XEM CHI TIẾT MÃ VÉ QR"': '"LỊCH SỬ ĐẶT VÉ"',
    '"VÍ VOUCHER CÁ NHÂN & KÍCH HOẠT MÃ ƯU ĐÃI"': '"VÍ VOUCHER CÁ NHÂN"',
    '"ĐÁNH GIÁ SAO VÀ NHẬN XÉT PHIM"': '"ĐÁNH GIÁ PHIM"',
    '"BÌNH LUẬN THẢO LUẬN PHIM"': '"BÌNH LUẬN PHIM"',
    '"LIÊN HỆ ĐÓNG GÓP Ý KIẾN CSKH"': '"LIÊN HỆ & GÓP Ý"',
    '"TỔNG QUAN THỐNG KÊ & BÁO CÁO DOANH THU (DASHBOARD)"': '"BÁO CÁO DOANH THU"',
    '"BÁN VÉ XEM PHIM TRỰC TIẾP TẠI QUẦY POS"': '"BÁN VÉ TẠI QUẦY (POS)"',
    '"QUẢN LÝ 3 TAB ĐƠN CHỜ TẠI QUẦY POS"': '"QUẢN LÝ ĐƠN CHỜ"',
    '"BÁN BẮP NƯỚC F&B TẠI QUẦY CONCESSION POS"': '"BÁN BẮP NƯỚC TẠI QUẦY"',
    '"KIỂM SOÁT VÉ (CHECK-IN) QUA MÃ QR & CAMERA"': '"SOÁT VÉ (CHECK-IN)"',
    '"QUẢN LÝ HÓA ĐƠN & PHÊ DUYỆT HỦY ĐƠN F&B HOÀN TIỀN"': '"HÓA ĐƠN & HỦY ĐƠN"',
    '"XỬ LÝ SỰ CỐ CHỖ NGỒI & ĐỔI GHẾ VIP MIỄN PHÍ"': '"XỬ LÝ SỰ CỐ GHẾ"',
    '"KHÓA BẢO TRÌ GHẾ VẬT LÝ TRÁNH BÁN NHẦM"': '"KHÓA BẢO TRÌ GHẾ"',
    '"DANH MỤC PHIM (THỂ LOẠI, ĐẠO DIỄN, DIỄN VIÊN, ĐỊNH DẠNG CHIẾU)"': '"DANH MỤC PHIM"',
    '"QUẢN LÝ BANNER QUẢNG CÁO TRANG CHỦ"': '"QUẢN LÝ BANNER"',
    '"TRÌNH DỰNG THIẾT KẾ SƠ ĐỒ GHẾ (SEATMAPBUILDER)"': '"THIẾT KẾ SƠ ĐỒ GHẾ"',
    '"QUẢN LÝ CỤM RẠP & PHÒNG CHIẾU"': '"CỤM RẠP & PHÒNG CHIẾU"',
    '"LỊCH CHIẾU SUẤT PHIM & CHẶN TRÙNG GIỜ CHIẾU (OVERLAP GUARD)"': '"QUẢN LÝ LỊCH CHIẾU"',
    '"DANH MỤC MÓN BẮP NƯỚC & COMBO F&B"': '"MÓN & COMBO BẮP NƯỚC"',
    '"BẢNG TÙY CHỌN VỊ BẮP NƯỚC & TOPPING"': '"TÙY CHỌN VỊ & TOPPING"',
    '"QUẢN LÝ BẢNG GIÁ VÉ THEO KHUNG GIỜ & PHỤ THU"': '"BẢNG GIÁ VÉ"',
    '"CHƯƠNG TRÌNH KHUYẾN MÃI & TẠO MÃ VOUCHER GIẢM GIÁ"': '"KHUYẾN MÃI & VOUCHER"',
    '"CẤU HÌNH TÍCH ĐIỂM HẠNG THẺ LOYALTY (STANDARD/VIP/DIAMOND)"': '"ĐIỂM THƯỞNG & HẠNG THẺ"',
    '"QUẢN LÝ DANH SÁCH HỘI VIÊN, HẠNG THẺ & KHÓA TÀI KHOẢN"': '"QUẢN LÝ KHÁCH HÀNG"',
    '"TIẾP NHẬN PHẢN HỒI GÓP Ý KHÁCH HÀNG & QUẢN LÝ FAQ"': '"PHẢN HỒI CSKH & FAQ"',
    '"QUẢN LÝ TÀI KHOẢN NHÂN SỰ CHI NHÁNH RẠP"': '"QUẢN LÝ NHÂN SỰ"',
    '"MA TRẬN PHÂN QUYỀN RBAC & OVERRIDE THEO NHÂN VIÊN"': '"PHÂN QUYỀN HỆ THỐNG (RBAC)"',
    '"NHẬT KÝ HỆ THỐNG AUDIT LOGS & DIFF JSON (READ-ONLY)"': '"NHẬT KÝ HỆ THỐNG (AUDIT LOGS)"',
    '"CÀI ĐẶT THÔNG TIN RẠP, HOTLINE & THỜI GIAN GIỮ CHỖ"': '"CÀI ĐẶT HỆ THỐNG"'
}

count = 0
for old_t, new_t in replacements.items():
    if old_t in content:
        content = content.replace(old_t, new_t)
        count += 1
        print(f'Replaced: {old_t} -> {new_t}')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print(f'Total replacements in consolidated_modules_builder.py: {count}')
