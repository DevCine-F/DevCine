import sys
import io
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

with open('scratch/full_audit_data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

print(f"Loaded {len(data)} sheets.")

# Group sheets by domain
domains = {
    "Xác thực & Tài khoản": ["Đăng nhập", "Đăng ký", "Quên mật khẩu", "Đổi mật khẩu", "Thông tin cá nhân"],
    "Khách hàng & Đặt vé Online": ["Chọn ghế & Giữ chỗ", "Combo F&B online", "Thanh toán VNPAY", "Voucher của tôi", "Lịch sử đặt vé", "Đánh giá phim", "Bình luận", "Liên hệ góp ý"],
    "Quầy POS & Vận hành rạp": ["POS Bán vé tại quầy", "POS Đơn chờ", "Soát vé & Check-in", "Xử lý sự cố & Đổi ghế", "Khóa bảo trì ghế vật lý", "POS Bán F&B tại quầy", "POS Kết ca & Bàn giao", "Phê duyệt hủy đơn F&B"],
    "Quản trị Hệ thống & Nghiệp vụ Rạp (Admin)": ["Quản lý phim", "Cụm rạp", "Phòng chiếu", "Sơ đồ ghế", "Lịch chiếu", "Bảng giá vé", "Khuyến mãi & Voucher", "Banner quảng cáo", "Món F&B", "Tùy chọn vị F&B", "Nhân viên", "Phân quyền hệ thống", "Thống kê & Báo cáo", "Nhật ký hệ thống", "Quản lý khách hàng", "Cài đặt hệ thống", "Điểm thưởng Loyalty"],
    "Danh mục phụ trợ / Master Data": ["Thể loại phim", "Đạo diễn", "Diễn viên", "Định dạng chiếu"],
    "Phân hệ ngoài phạm vi (Out-of-scope)": ["Ca làm việc"]
}

for dom, sheets in domains.items():
    print(f"\n==================================================")
    print(f"DOM: {dom} ({len(sheets)} sheets)")
    print(f"==================================================")
    for s in sheets:
        if s in data:
            sh_data = data[s]
            print(f"- [{s}] | Total TCs: {sh_data['total_tcs']} | Code: {sh_data['module_code']} | Req: {sh_data['test_requirement']}")
