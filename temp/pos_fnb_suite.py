# -*- coding: utf-8 -*-
"""
POS Bán F&B tại quầy - Full 28 Test Cases Suite
"""

tc_pos_fnb = [
    ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
    ("PFN_GUI_01", "Kiểm tra chức năng hiển thị Màn hình POS Bán F&B tại quầy thành công",
     "Kiểm tra hiển thị Thực đơn F&B dạng lưới cảm ứng trên POS",
     "Bước 1: Nhân viên mở màn hình Bán F&B tại quầy\nBước 2: Quan sát danh mục món\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị danh mục món F&B dạng lưới card cảm ứng trực quan: Ảnh món, Tên món, Đơn giá và nút Thêm nhanh"),

    ("PFN_GUI_02", "Kiểm tra chức năng hiển thị Bộ lọc nhóm danh mục F&B thành công",
     "Kiểm tra hiển thị thanh tab nhóm sản phẩm",
     "Bước 1: Nhân viên mở màn hình Bán F&B\nBước 2: Quan sát các tab danh mục phía trên thực đơn\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị đầy đủ các tab: 'TẤT CẢ', 'BẮP RANG', 'NƯỚC NGỌT', 'COMBO ƯU ĐÃI', 'SNACK'"),

    ("PFN_GUI_03", "Kiểm tra chức năng hiển thị Cột Giỏ hàng F&B bên phải thành công",
     "Kiểm tra hiển thị Chi tiết giỏ hàng bắp nước",
     "Bước 1: Nhân viên chọn món 'My Combo'\nBước 2: Quan sát cột giỏ hàng bên phải màn hình\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị: Tên món, Vị bắp đã chọn, Loại nước, Đơn giá, Nút [-] / [+] số lượng, Nút xóa món và Tạm tính tổng tiền"),

    ("PFN_GUI_04", "Kiểm tra chức năng hiển thị Modal tùy chọn vị bắp và nước FnbOptionModal thành công",
     "Kiểm tra hiển thị Popup tùy chọn topping / đổi vị khi chọn combo",
     "Bước 1: Nhân viên click vào card 'Couple Combo'\nBước 2: Kiểm tra popup hiển thị trên màn hình POS",
     "N/A", "Hiển thị Modal FnbOptionModal: Chọn vị bắp (Ngọt 0đ, Phô mai +15k, Trứng muối +15k, Caramel +15k) và Chọn 2 loại nước (Coca, Sprite, Fanta)"),

    ("PFN_GUI_05", "Kiểm tra chức năng hiển thị Khung Tra cứu Hội viên & Áp Voucher tại quầy thành công",
     "Kiểm tra hiển thị Ô tìm kiếm khách hàng bằng SĐT và ô nhập mã khuyến mãi",
     "Bước 1: Quan sát phía dưới giỏ hàng F&B\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị: Ô nhập SĐT khách hàng kèm nút 'Tra cứu', Thông tin hạng thẻ & Điểm tích lũy, Ô nhập Mã Voucher"),

    ("PFN_GUI_06", "Kiểm tra chức năng hiển thị Bảng Tổng thanh toán và Nút Chọn Phương thức thành công",
     "Kiểm tra hiển thị Tổng tiền, Ô tiền khách đưa và 2 nút thanh toán",
     "Bước 1: Giỏ hàng có món bắp nước\nBước 2: Quan sát khu vực thanh toán phía dưới cùng\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị: Tổng tiền cần thu, Nút '💵 TIỀN MẶT', Nút '📱 CHUYỂN KHOẢN VIETQR', Ô nhập Tiền khách đưa và Tiền thừa trả lại"),

    ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
    ("PFN_EP_01", "Kiểm tra chức năng Chọn số lượng món F&B thành công",
     "Kiểm tra chức năng Chọn số lượng món F&B trong khoảng từ 1 đến 50 phần",
     "Bước 1: Nhân viên click chọn 3 phần 'Bắp phô mai' (Tổng 195.000đ)\nBước 2: Kiểm tra giỏ hàng và tổng tiền\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Số lượng: 3 phần\nMón: Bắp phô mai (65.000đ/phần)", "Hệ thống ghi nhận 3 phần vào giỏ hàng, tính đúng tổng tiền 195.000đ và sẵn sàng thanh toán"),

    ("PFN_EP_02", "Kiểm tra chức năng Thanh toán khi Giỏ hàng F&B trống thất bại",
     "Kiểm tra chức năng Thanh toán khi số lượng món F&B trong giỏ là 0 phần (khoảng dưới của [1,50])",
     "Bước 1: Mở màn hình Bán F&B, không chọn bất kỳ món nào (Giỏ hàng = 0đ)\nBước 2: Cố tình click button 'TIỀN MẶT' hoặc 'VIETQR'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Số lượng F&B: 0 phần\nThao tác: Bấm Thanh toán", "Các nút thanh toán bị vô hiệu hóa (disabled), hiển thị thông báo: 'Vui lòng chọn ít nhất 1 món F&B'"),

    ("PFN_EP_03", "Kiểm tra chức năng Tăng số lượng món F&B thất bại khi vượt quá giới hạn",
     "Kiểm tra chức năng Tăng số lượng món F&B vượt quá 50 phần/đơn (khoảng trên của [1,50])",
     "Bước 1: Tăng số lượng Bắp rang lên 50 phần trong giỏ\nBước 2: Click tiếp nút [+] lần thứ 51\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Số lượng hiện có: 50 phần\nThao tác: Click nút [+]", "Nút [+] bị vô hiệu hóa, hiển thị cảnh báo: 'Số lượng món vượt quá giới hạn tối đa cho 1 đơn hàng tại quầy (50 phần)'"),

    ("PFN_EP_04", "Kiểm tra chức năng Nhập Tiền khách đưa hợp lệ thành công",
     "Kiểm tra chức năng Tính tiền thừa khi khách đưa số tiền lớn hơn hoặc bằng tổng đơn hàng",
     "Bước 1: Đơn F&B có tổng tiền 130.000đ\nBước 2: Nhân viên nhập tiền khách đưa 200.000đ vào ô 'Tiền khách đưa'\nBước 3: Kiểm tra hiển thị tiền thừa",
     "Tổng đơn: 130.000 VNĐ\nKhách đưa: 200.000 VNĐ", "Hệ thống tự động tính toán và hiển thị rõ ràng: 'Tiền thừa trả khách: 70.000 VNĐ'"),

    ("PFN_EP_05", "Kiểm tra chức năng Thanh toán tiền mặt thất bại khi Tiền khách đưa chưa đủ",
     "Kiểm tra chặn hoàn tất đơn khi số tiền khách đưa nhỏ hơn tổng tiền phải thanh toán",
     "Bước 1: Đơn F&B tổng tiền 150.000đ\nBước 2: Nhân viên nhập tiền khách đưa 100.000đ\nBước 3: Click button 'Hoàn tất & In phiếu'\nBước 4: Kiểm tra phản hồi từ hệ thống",
     "Tổng đơn: 150.000 VNĐ\nKhách đưa: 100.000 VNĐ (Thiếu 50.000đ)", "Hệ thống từ chối thanh toán và cảnh báo: 'Số tiền khách đưa chưa đủ, còn thiếu 50.000 VNĐ'"),

    ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
    ("PFN_BVA_01", "Kiểm tra chức năng Chọn số lượng món F&B thành công",
     "Kiểm tra chức năng Chọn số lượng món F&B là giá trị biên min (1 phần)",
     "Bước 1: Nhân viên click chọn 1 ly 'Coca-Cola Lớn'\nBước 2: Kiểm tra giỏ hàng\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Số lượng: 1 ly", "Thêm 1 ly vào giỏ hàng thành công"),

    ("PFN_BVA_02", "Kiểm tra chức năng Chọn số lượng món F&B thành công",
     "Kiểm tra chức năng Chọn số lượng món F&B là giá trị cận biên trên min (2 phần)",
     "Bước 1: Click nút [+] tăng lên 2 ly Coca-Cola\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
     "Số lượng: 2 ly", "Ghi nhận 2 ly và nhân đôi thành tiền"),

    ("PFN_BVA_03", "Kiểm tra chức năng Chọn số lượng món F&B thành công",
     "Kiểm tra chức năng Chọn số lượng món F&B là giá trị cận biên dưới max (49 phần)",
     "Bước 1: Nhập số lượng 49 phần Bắp ngọt\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
     "Số lượng: 49 phần", "Hệ thống chấp nhận 49 phần hợp lệ"),

    ("PFN_BVA_04", "Kiểm tra chức năng Chọn số lượng món F&B thành công",
     "Kiểm tra chức năng Chọn số lượng món F&B là giá trị biên max (50 phần)",
     "Bước 1: Nhập số lượng 50 phần Bắp ngọt (đạt mức tối đa)\nBước 2: Kiểm tra kết quả hiển thị từ hệ thống",
     "Số lượng: 50 phần", "Hệ thống chấp nhận 50 phần tối đa"),

    ("PFN_BVA_05", "Kiểm tra chức năng Giảm số lượng món về 0 phần thành công",
     "Kiểm tra chức năng Tự động xóa món khỏi giỏ hàng khi giảm số lượng về cận biên dưới min (0 phần)",
     "Bước 1: Món đang có số lượng 1 trong giỏ\nBước 2: Nhân viên click nút [-]\nBước 3: Kiểm tra giỏ hàng",
     "Số lượng cũ: 1 phần\nThao tác: Click nút [-]", "Món tự động được xóa hoàn toàn khỏi giỏ hàng, tổng tiền về 0đ"),

    ("PFN_BVA_06", "Kiểm tra chức năng Tăng số lượng món F&B thất bại khi vượt cận biên trên max (51 phần)",
     "Kiểm tra chức năng Chặn tăng số lượng khi đã đạt mức tối đa 50 phần",
     "Bước 1: Món đã có 50 phần trong giỏ\nBước 2: Cố tình click nút [+] để tăng lên 51\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Số lượng: 50 phần\nThao tác: Click nút [+]", "Nút [+] bị vô hiệu hóa (disabled), không cho phép tăng lên 51 phần"),

    ("PFN_BVA_07", "Kiểm tra chức năng Thanh toán tiền mặt khi Tiền khách đưa đúng bằng Tổng tiền (Biên min tiền)",
     "Kiểm tra luồng thanh toán khi khách trả tiền chẵn, tiền thừa bằng đúng 0 VNĐ",
     "Bước 1: Đơn F&B tổng tiền 89.000đ\nBước 2: Khách đưa đúng 89.000đ\nBước 3: Click 'Hoàn tất & In phiếu'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
     "Tổng đơn: 89.000 VNĐ\nKhách đưa: 89.000 VNĐ\nTiền thừa: 0 VNĐ", "Thanh toán thành công ngay lập tức, tiền thừa 0 VNĐ, in phiếu nhận món"),

    ("PFN_BVA_08", "Kiểm tra chức năng Thanh toán tiền mặt thất bại khi Tiền khách đưa thiếu 1.000 VNĐ",
     "Kiểm tra chức năng Chặn thanh toán khi khách đưa thiếu mức cận biên dưới (thiếu 1.000đ)",
     "Bước 1: Đơn F&B tổng tiền 100.000đ\nBước 2: Nhập khách đưa 99.000đ (thiếu 1.000đ)\nBước 3: Click 'Hoàn tất & In phiếu'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
     "Tổng đơn: 100.000 VNĐ\nKhách đưa: 99.000 VNĐ", "Hệ thống từ chối thanh toán và báo thiếu 1.000 VNĐ"),

    ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
    ("PFN_ERR_01", "Kiểm tra chức năng Nhập tiền khách đưa để trống (Null) thất bại",
     "Kiểm tra khi nhân viên không nhập số tiền khách đưa mà bấm hoàn tất",
     "Bước 1: Chọn món vào giỏ (Tổng tiền: 120.000đ)\nBước 2: Để trống ô Tiền khách đưa (Null)\nBước 3: Click button 'Hoàn tất & In phiếu'\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
     "Tiền khách đưa: Null", "Hiển thị thông báo yêu cầu: 'Vui lòng nhập số tiền khách đưa'"),

    ("PFN_ERR_02", "Kiểm tra chức năng Nhập tiền khách đưa chứa ký tự đặc biệt hoặc chữ cái",
     "Kiểm tra chức năng Tự động làm sạch dữ liệu đầu vào ô tiền mặt",
     "Bước 1: Nhập chuỗi '200abc@#$' vào ô Tiền khách đưa\nBước 2: Kiểm tra giá trị hiển thị trong ô",
     "Dữ liệu nhập: '200abc@#$'", "Hệ thống tự động lọc bỏ ký tự lạ, chỉ giữ lại số nguyên hợp lệ '200' (hoặc 200.000 VNĐ)"),

    ("PFN_ERR_03", "Kiểm tra chức năng Hủy giỏ hàng F&B (Clear Cart) thành công",
     "Kiểm tra chức năng Xóa toàn bộ món đang chọn để phục vụ khách mới",
     "Bước 1: Giỏ hàng đang có 4 món bắp nước\nBước 2: Click button '🗑️ XÓA TẤT CẢ GIỎ HÀNG'\nBước 3: Xác nhận trên popup\nBước 4: Kiểm tra giỏ hàng",
     "Thao tác: Click Xóa tất cả giỏ hàng", "Giỏ hàng được xóa sạch hoàn toàn về 0 món, tổng tiền về 0 VNĐ"),

    ("PFN_ERR_04", "Kiểm tra chức năng Đóng Modal FnbOptionModal mà không bấm Xác nhận",
     "Kiểm tra khi nhân viên mở popup đổi vị nhưng bấm [X] hủy",
     "Bước 1: Mở Modal tùy chọn vị bắp\nBước 2: Click icon [X] đóng modal hoặc click vùng tối bên ngoài\nBước 3: Kiểm tra giỏ hàng",
     "Thao tác: Đóng Modal không lưu", "Modal đóng lại, không thêm combo vào giỏ hàng và không làm sai lệch tổng tiền"),

    ("__SECTION__", "KIỂM TRA TÌM KIẾM & BỘ LỌC DỮ LIỆU"),
    ("PFN_LOC_01", "Kiểm tra chức năng Lọc thực đơn theo nhóm Bắp rang thành công",
     "Kiểm tra chức năng lọc danh mục chỉ hiển thị các món bắp",
     "Bước 1: Nhân viên click vào tab 'BẮP RANG'\nBước 2: Quan sát lưới món\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Tab chọn: 'BẮP RANG'", "Lưới thực đơn chỉ hiển thị các món: Bắp ngọt, Bắp phô mai, Bắp trứng muối, Bắp caramel"),

    ("PFN_LOC_02", "Kiểm tra chức năng Lọc thực đơn theo nhóm Nước ngọt thành công",
     "Kiểm tra chức năng lọc danh mục chỉ hiển thị các loại nước uống",
     "Bước 1: Nhân viên click vào tab 'NƯỚC NGỌT'\nBước 2: Quan sát lưới món\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Tab chọn: 'NƯỚC NGỌT'", "Lưới thực đơn chỉ hiển thị các món: Coca-Cola, Sprite, Fanta, Nước suối Dasani"),

    ("PFN_LOC_03", "Kiểm tra chức năng Lọc thực đơn theo nhóm Combo ưu đãi thành công",
     "Kiểm tra chức năng lọc danh mục chỉ hiển thị các gói combo",
     "Bước 1: Nhân viên click vào tab 'COMBO ƯU ĐÃI'\nBước 2: Quan sát lưới món\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "Tab chọn: 'COMBO ƯU ĐÃI'", "Lưới thực đơn chỉ hiển thị: Solo Combo, Couple Combo, Family Party Combo"),

    ("PFN_LOC_04", "Kiểm tra chức năng Tìm kiếm món F&B theo từ khóa thành công",
     "Kiểm tra chức năng tìm kiếm nhanh món bắp nước tại quầy POS",
     "Bước 1: Gõ từ khóa 'Phô mai' vào ô Tìm kiếm món trên POS\nBước 2: Kiểm tra danh sách hiển thị",
     "Từ khóa: 'Phô mai'", "Lưới món tự động lọc hiển thị: 'Bắp rang bơ Vị Phô Mai' và 'Xúc xích lắc phô mai'"),

    ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
    ("PFN_FUNC_01", "Kiểm tra chức năng Bán Combo F&B có tính phụ thu đổi vị thành công",
     "Kiểm tra luồng bán 'Couple Combo' đổi vị bắp sang Phô mai (+15.000đ) và 2 nước Sprite",
     "Bước 1: Nhân viên click chọn 'Couple Combo' (Giá gốc 119.000đ)\nBước 2: Trên Modal chọn Vị Phô mai (+15.000đ), Nước 1: Sprite, Nước 2: Fanta\nBước 3: Click 'Xác nhận thêm vào giỏ'\nBước 4: Kiểm tra tổng tiền",
     "Combo: Couple Combo (119.000đ)\nPhụ thu vị: +15.000đ\nTổng: 134.000đ", "Hệ thống ghi nhận chính xác 134.000đ, hiển thị rõ thành phần và vị bắp trong giỏ hàng"),

    ("PFN_FUNC_02", "Kiểm tra chức năng Tra cứu Hội viên và Tích điểm F&B tại quầy thành công",
     "Kiểm tra luồng nhập SĐT khách hàng để tích điểm Loyalty khi mua bắp nước lẻ",
     "Bước 1: Nhập SĐT '0901234567' vào ô tra cứu khách hàng\nBước 2: Click button 'Tra cứu'\nBước 3: Nhân viên hoàn tất thanh toán đơn F&B 200.000đ\nBước 4: Kiểm tra điểm tích lũy",
     "SĐT khách: '0901234567'\nHạng thẻ: VIP (Tích 10%)\nTổng đơn: 200.000đ", "Hệ thống hiển thị tên 'Nguyễn Văn Dân', tích thành công 20 điểm Loyalty vào tài khoản khách"),

    ("PFN_FUNC_03", "Kiểm tra chức năng Áp dụng mã Voucher giảm giá F&B tại quầy thành công",
     "Kiểm tra chức năng giảm trừ tiền trực tiếp khi khách đưa mã khuyến mãi bắp nước",
     "Bước 1: Nhập mã voucher 'FNB30K' (Giảm 30.000đ cho đơn từ 100k)\nBước 2: Click button 'Áp dụng'\nBước 3: Kiểm tra tổng thanh toán",
     "Mã Voucher: 'FNB30K'\nTổng đơn cũ: 150.000đ\nGiảm giá: -30.000đ", "Tổng tiền thanh toán tự động giảm còn 120.000đ, hiển thị badge voucher màu xanh"),

    ("PFN_FUNC_04", "Kiểm tra chức năng Thanh toán Chuyển khoản VietQR động tại quầy F&B thành công",
     "Kiểm tra luồng thanh toán không dùng tiền mặt bằng mã QR VietQR tự sinh tại quầy",
     "Bước 1: Đơn F&B tổng 180.000đ, nhân viên click button '📱 CHUYỂN KHOẢN VIETQR'\nBước 2: Modal hiển thị mã VietQR động chứa số tiền 180.000đ\nBước 3: Khách quét mã chuyển khoản thành công\nBước 4: Nhân viên click 'Xác nhận đã nhận tiền'\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
     "Phương thức: VietQR\nTổng tiền: 180.000 VNĐ", "Hệ thống ghi nhận đơn thanh toán qua ngân hàng, in phiếu nhận món và lưu vết giao dịch POS"),

    ("PFN_FUNC_05", "Kiểm tra chức năng In Phiếu nhận món F&B (Receipt) tại máy in nhiệt quầy Concession",
     "Kiểm tra định dạng và nội dung phiếu nhận món in ra cho khách hàng",
     "Bước 1: Hoàn tất thanh toán đơn F&B tại quầy\nBước 2: Máy in hóa đơn nhiệt tự động in phiếu\nBước 3: Kiểm tra nội dung in trên giấy",
     "Thiết bị: Máy in nhiệt 80mm\nMã đơn: 'POS-FNB-20260319-01'", "Phiên in thành công, phiếu hiển thị đầy đủ: Tên rạp DevCine, Số thứ tự nhận món (Số 15), Chi tiết món & Vị bắp, Tổng tiền và Thời gian mua")
]
