# -*- coding: utf-8 -*-
"""
Detailed Movie Management Test Suite for AdminMovies.vue & MovieFormModal.vue
Includes completely separated sections for:
1. THÊM PHIM MỚI (ADD MOVIE MODAL) - 67 TCs
2. CHỈNH SỬA THÔNG TIN PHIM (EDIT MOVIE MODAL) - 39 TCs
Total: 106 Test Cases
"""

tc_movies = [
    # =========================================================================
    # CHỨC NĂNG THÊM MỚI PHIM (ADD MOVIE MODAL)
    # =========================================================================
    ("__FEATURE__", "THÊM PHIM MỚI"),
    ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
    ("MOV_ADD_GUI_01", "Kiểm tra chức năng hiển thị Modal Thêm phim mới thành công",
     "Kiểm tra hiển thị Modal 'THÊM PHIM MỚI' gồm 2 cột (Form nhập liệu 2/3 và Xem trước Poster 1/3)",
     "Bước 1: Quản trị viên truy cập màn hình Quản lý phim (AdminMovies.vue)\nBước 2: Click button '+ THÊM PHIM MỚI'\nBước 3: Quan sát Modal hiển thị trên màn hình\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị Modal Thêm phim mới với tiêu đề 'THÊM PHIM MỚI', phụ đề 'Thông tin chi tiết nội dung kỹ thuật số', bố cục 2 cột rõ ràng"),

    ("MOV_ADD_GUI_02", "Kiểm tra chức năng hiển thị Nhóm 01. Định danh & Nội dung thành công",
     "Kiểm tra hiển thị các trường thông tin cơ bản của phim",
     "Bước 1: Mở Modal Thêm phim mới\nBước 2: Quan sát khu vực '01. Định danh & Nội dung'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị: Tên phim (*), Thời lượng (Phút) (*), Trailer URL (YouTube) (*), Đạo diễn, Diễn viên chính"),

    ("MOV_ADD_GUI_03", "Kiểm tra chức năng hiển thị Nhóm 02. Kỹ thuật & Sản xuất thành công",
     "Kiểm tra hiển thị thông tin sản xuất và bộ chọn thể loại/định dạng",
     "Bước 1: Mở Modal Thêm phim mới\nBước 2: Quan sát khu vực '02. Kỹ thuật & Sản xuất'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị: Dropdown Quốc gia, Ô Năm sản xuất (*), Dropdown Ngôn ngữ gốc, Dropdown Loại hình hiển thị, Lưới chọn Thể loại phim (*), Badge chọn Định dạng hỗ trợ (*)"),

    ("MOV_ADD_GUI_04", "Kiểm tra chức năng hiển thị Nhóm 03. Vận hành & Kiểm soát thành công",
     "Kiểm tra hiển thị cấu hình ngày chiếu và độ tuổi",
     "Bước 1: Mở Modal Thêm phim mới\nBước 2: Quan sát khu vực '03. Vận hành & Kiểm soát'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị: Dropdown Phân loại độ tuổi, Dropdown Trạng thái, Ô Ngày khởi chiếu (*), Ô Ngày kết thúc dự kiến (*), Textarea Ghi chú nội bộ cho Admin"),

    ("MOV_ADD_GUI_05", "Kiểm tra chức năng hiển thị Nhóm 04. Media & Mô tả thành công",
     "Kiểm tra hiển thị khung tải ảnh Banner và Tóm tắt nội dung",
     "Bước 1: Mở Modal Thêm phim mới\nBước 2: Quan sát khu vực '04. Media & Mô tả'\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị: Khung upload Ảnh Banner (tối đa 8MB), Switch 'Hiển thị trên Banner trang chủ', Textarea Tóm tắt nội dung (*) (50-1000 ký tự) kèm bộ đếm ký tự"),

    ("MOV_ADD_GUI_06", "Kiểm tra chức năng hiển thị Cột Xem trước Poster và Nút Xuất bản thành công",
     "Kiểm tra hiển thị khung xem trước poster bên phải và các nút tác vụ",
     "Bước 1: Mở Modal Thêm phim mới\nBước 2: Quan sát cột bên phải (1/3)\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị Khung tỷ lệ 2/3 'Xem trước hiển thị (Click để tải Poster)' (tối đa 5MB), Nút 'Hủy bỏ' và Nút 'Xuất bản' kèm icon tên lửa"),

    ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
    ("MOV_ADD_EP_01", "Kiểm tra chức năng Nhập Tên phim hợp lệ [2, 150] ký tự thành công",
     "Kiểm tra trường Tên phim khi nhập độ dài hợp lệ",
     "Bước 1: Mở Modal Thêm phim mới\nBước 2: Nhập Tên phim: 'Oppenheimer'\nBước 3: Kiểm tra hiển thị",
     "Tên phim: 'Oppenheimer' (11 ký tự)", "Hệ thống chấp nhận tên phim hợp lệ, không báo lỗi"),

    ("MOV_ADD_EP_02", "Kiểm tra chức năng Nhập Tên phim thất bại khi < 2 ký tự",
     "Kiểm tra ràng buộc độ dài tối thiểu của Tên phim (khoảng dưới của [2,150])",
     "Bước 1: Nhập Tên phim 'A' (1 ký tự)\nBước 2: Click ra ngoài hoặc bấm Xuất bản\nBước 3: Kiểm tra phản hồi từ hệ thống",
     "Tên phim: 'A' (1 ký tự)", "Viền đỏ ô Tên phim, hiển thị thông báo lỗi 'Tên phim phải từ 2 ký tự' và khóa nút Xuất bản"),

    ("MOV_ADD_EP_03", "Kiểm tra chức năng Nhập Tên phim thất bại khi > 150 ký tự",
     "Kiểm tra giới hạn ký tự tối đa của Tên phim (khoảng trên của [2,150])",
     "Bước 1: Nhập chuỗi tên phim dài 160 ký tự\nBước 2: Kiểm tra độ dài giá trị trong ô",
     "Tên phim: (Chuỗi 160 ký tự)", "Hệ thống tự động chặn gõ tại 150 ký tự (maxlength=150) hoặc báo lỗi 'Tên phim tối đa 150 ký tự'"),

    ("MOV_ADD_EP_04", "Kiểm tra chức năng Nhập Thời lượng phim hợp lệ [30, 300] phút thành công",
     "Kiểm tra trường Thời lượng khi nhập số phút hợp lệ",
     "Bước 1: Nhập Thời lượng '120' phút\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Thời lượng: '120' (phút)", "Hệ thống ghi nhận 120 phút hợp lệ"),

    ("MOV_ADD_EP_05", "Kiểm tra chức năng Nhập Thời lượng phim thất bại khi < 30 phút",
     "Kiểm tra chặn thời lượng phim quá ngắn (khoảng dưới của [30,300])",
     "Bước 1: Nhập Thời lượng '15' phút\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Thời lượng: '15' (phút)", "Viền đỏ, hiển thị thông báo lỗi 'Thời lượng phim phải là số nguyên từ 30 đến 300 phút'"),

    ("MOV_ADD_EP_06", "Kiểm tra chức năng Nhập Thời lượng phim thất bại khi > 300 phút",
     "Kiểm tra chặn thời lượng phim quá dài (khoảng trên của [30,300])",
     "Bước 1: Nhập Thời lượng '350' phút\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Thời lượng: '350' (phút)", "Viền đỏ, hiển thị thông báo lỗi 'Thời lượng phim phải là số nguyên từ 30 đến 300 phút'"),

    ("MOV_ADD_EP_07", "Kiểm tra chức năng Nhập Trailer URL YouTube hợp lệ thành công",
     "Kiểm tra đường dẫn Trailer khi nhập link từ YouTube",
     "Bước 1: Nhập Trailer URL 'https://youtube.com/watch?v=uYPbbksJxIg'\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Trailer URL: 'https://youtube.com/watch?v=uYPbbksJxIg'", "Hệ thống chấp nhận link YouTube hợp lệ"),

    ("MOV_ADD_EP_08", "Kiểm tra chức năng Nhập Trailer URL thất bại khi không thuộc YouTube",
     "Kiểm tra chặn đường dẫn trailer từ các domain không được hỗ trợ",
     "Bước 1: Nhập Trailer URL 'https://facebook.com/video/123456'\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Trailer URL: 'https://facebook.com/video/123456'", "Viền đỏ, hiển thị thông báo lỗi 'Đường dẫn Trailer không hợp lệ. Vui lòng nhập link từ Youtube'"),

    ("MOV_ADD_EP_09", "Kiểm tra chức năng Nhập Đạo diễn hợp lệ [2, 100] ký tự thành công",
     "Kiểm tra trường Đạo diễn khi nhập tên chữ cái có dấu và khoảng trắng",
     "Bước 1: Nhập Đạo diễn: 'Christopher Nolan'\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Đạo diễn: 'Christopher Nolan'", "Hệ thống ghi nhận tên đạo diễn hợp lệ"),

    ("MOV_ADD_EP_10", "Kiểm tra chức năng Nhập Diễn viên chính hợp lệ [2, 255] ký tự thành công",
     "Kiểm tra trường Diễn viên khi nhập danh sách diễn viên",
     "Bước 1: Nhập Diễn viên: 'Cillian Murphy, Emily Blunt, Matt Damon'\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Diễn viên: 'Cillian Murphy, Emily Blunt, Matt Damon'", "Hệ thống ghi nhận danh sách diễn viên hợp lệ"),

    ("MOV_ADD_EP_11", "Kiểm tra chức năng Nhập Năm sản xuất hợp lệ [1900, 2030] thành công",
     "Kiểm tra trường Năm sản xuất với năm hợp lệ",
     "Bước 1: Nhập Năm sản xuất: '2024'\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Năm sản xuất: '2024'", "Hệ thống ghi nhận năm sản xuất 2024"),

    ("MOV_ADD_EP_12", "Kiểm tra chức năng Nhập Năm sản xuất thất bại khi < 1900",
     "Kiểm tra chặn năm sản xuất quá cũ (khoảng dưới của [1900,2030])",
     "Bước 1: Nhập Năm sản xuất: '1850'\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Năm sản xuất: '1850'", "Viền đỏ, hiển thị thông báo lỗi 'Năm sản xuất không hợp lệ (1900 - 2030)'"),

    ("MOV_ADD_EP_13", "Kiểm tra chức năng Nhập Năm sản xuất thất bại khi > 2030",
     "Kiểm tra chặn năm sản xuất quá xa trong tương lai (khoảng trên của [1900,2030])",
     "Bước 1: Nhập Năm sản xuất: '2050'\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Năm sản xuất: '2050'", "Viền đỏ, hiển thị thông báo lỗi 'Năm sản xuất không hợp lệ (1900 - 2030)'"),

    ("MOV_ADD_EP_14", "Kiểm tra chức năng Nhập Tóm tắt nội dung hợp lệ [50, 1000] ký tự thành công",
     "Kiểm tra trường Tóm tắt nội dung khi nhập đoạn văn đủ độ dài",
     "Bước 1: Nhập đoạn tóm tắt 120 ký tự\nBước 2: Quan sát bộ đếm ký tự bên góc phải\nBước 3: Kiểm tra kết quả",
     "Tóm tắt: (Đoạn văn 120 ký tự)", "Bộ đếm hiển thị '120/1000 ký tự', không báo lỗi"),

    ("MOV_ADD_EP_15", "Kiểm tra chức năng Nhập Tóm tắt nội dung thất bại khi < 50 ký tự",
     "Kiểm tra ràng buộc độ dài tối thiểu của Tóm tắt nội dung (khoảng dưới của [50,1000])",
     "Bước 1: Nhập Tóm tắt: 'Phim rất hay' (12 ký tự)\nBước 2: Click ra ngoài ô nhập\nBước 3: Kiểm tra phản hồi",
     "Tóm tắt: 'Phim rất hay' (12 ký tự)", "Viền đỏ, hiển thị thông báo lỗi 'Tóm tắt nội dung phim tối thiểu 50 ký tự'"),

    ("MOV_ADD_EP_16", "Kiểm tra chức năng Nhập Tóm tắt nội dung thất bại khi > 1000 ký tự",
     "Kiểm tra chặn khi vượt quá 1000 ký tự (khoảng trên của [50,1000])",
     "Bước 1: Nhập đoạn văn dài 1050 ký tự\nBước 2: Quan sát ô nhập liệu",
     "Tóm tắt: (Chuỗi 1050 ký tự)", "Hệ thống tự động cắt chuỗi tại đúng 1000 ký tự (maxlength=1000)"),

    ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
    ("MOV_ADD_BVA_01", "Kiểm tra chức năng Nhập Tên phim ở giá trị biên min (2 ký tự) thành công",
     "Kiểm tra Tên phim với độ dài 2 ký tự",
     "Bước 1: Nhập Tên phim: 'IT' (2 ký tự)\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Tên phim: 'IT' (2 ký tự)", "Hệ thống chấp nhận tên phim 2 ký tự hợp lệ"),

    ("MOV_ADD_BVA_02", "Kiểm tra chức năng Nhập Tên phim ở giá trị cận biên trên min (3 ký tự) thành công",
     "Kiểm tra Tên phim với độ dài 3 ký tự",
     "Bước 1: Nhập Tên phim: 'Her' (3 ký tự)\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Tên phim: 'Her' (3 ký tự)", "Hệ thống chấp nhận tên phim 3 ký tự hợp lệ"),

    ("MOV_ADD_BVA_03", "Kiểm tra chức năng Nhập Tên phim ở giá trị cận biên dưới max (149 ký tự) thành công",
     "Kiểm tra Tên phim với độ dài 149 ký tự",
     "Bước 1: Nhập Tên phim 149 ký tự\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Tên phim: (Chuỗi 149 ký tự)", "Hệ thống chấp nhận tên phim 149 ký tự hợp lệ"),

    ("MOV_ADD_BVA_04", "Kiểm tra chức năng Nhập Tên phim ở giá trị biên max (150 ký tự) thành công",
     "Kiểm tra Tên phim với độ dài 150 ký tự",
     "Bước 1: Nhập Tên phim 150 ký tự\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Tên phim: (Chuỗi 150 ký tự)", "Hệ thống chấp nhận tên phim 150 ký tự hợp lệ"),

    ("MOV_ADD_BVA_05", "Kiểm tra chức năng Nhập Tên phim ở giá trị cận biên dưới min (1 ký tự) thất bại",
     "Kiểm tra Tên phim với độ dài 1 ký tự",
     "Bước 1: Nhập Tên phim 'A' (1 ký tự)\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Tên phim: 'A' (1 ký tự)", "Báo lỗi 'Tên phim phải từ 2 ký tự'"),

    ("MOV_ADD_BVA_06", "Kiểm tra chức năng Nhập Tên phim ở giá trị cận biên trên max (151 ký tự) thất bại",
     "Kiểm tra Tên phim với độ dài 151 ký tự",
     "Bước 1: Nhập Tên phim 151 ký tự\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Tên phim: (Chuỗi 151 ký tự)", "Hệ thống tự động chặn gõ tại 150 ký tự hoặc báo lỗi 'Tên phim tối đa 150 ký tự'"),

    ("MOV_ADD_BVA_07", "Kiểm tra chức năng Nhập Thời lượng ở giá trị biên min (30 phút) thành công",
     "Kiểm tra thời lượng phim hoạt hình ngắn 30 phút",
     "Bước 1: Nhập Thời lượng '30' phút\nBước 2: Kiểm tra kết quả",
     "Thời lượng: '30' (phút)", "Hệ thống chấp nhận 30 phút"),

    ("MOV_ADD_BVA_08", "Kiểm tra chức năng Nhập Thời lượng ở giá trị cận biên trên min (31 phút) thành công",
     "Kiểm tra thời lượng phim 31 phút",
     "Bước 1: Nhập Thời lượng '31' phút\nBước 2: Kiểm tra kết quả",
     "Thời lượng: '31' (phút)", "Hệ thống chấp nhận 31 phút"),

    ("MOV_ADD_BVA_09", "Kiểm tra chức năng Nhập Thời lượng ở giá trị cận biên dưới max (299 phút) thành công",
     "Kiểm tra thời lượng phim 299 phút",
     "Bước 1: Nhập Thời lượng '299' phút\nBước 2: Kiểm tra kết quả",
     "Thời lượng: '299' (phút)", "Hệ thống chấp nhận 299 phút"),

    ("MOV_ADD_BVA_10", "Kiểm tra chức năng Nhập Thời lượng ở giá trị biên max (300 phút) thành công",
     "Kiểm tra thời lượng phim bom tấn dài 300 phút (5 tiếng)",
     "Bước 1: Nhập Thời lượng '300' phút\nBước 2: Kiểm tra kết quả",
     "Thời lượng: '300' (phút)", "Hệ thống chấp nhận 300 phút"),

    ("MOV_ADD_BVA_11", "Kiểm tra chức năng Nhập Thời lượng ở giá trị cận biên dưới min (29 phút) thất bại",
     "Kiểm tra chặn thời lượng 29 phút",
     "Bước 1: Nhập Thời lượng '29' phút\nBước 2: Kiểm tra kết quả",
     "Thời lượng: '29' (phút)", "Báo lỗi 'Thời lượng phim phải là số nguyên từ 30 đến 300 phút'"),

    ("MOV_ADD_BVA_12", "Kiểm tra chức năng Nhập Thời lượng ở giá trị cận biên trên max (301 phút) thất bại",
     "Kiểm tra chặn thời lượng 301 phút",
     "Bước 1: Nhập Thời lượng '301' phút\nBước 2: Kiểm tra kết quả",
     "Thời lượng: '301' (phút)", "Báo lỗi 'Thời lượng phim phải là số nguyên từ 30 đến 300 phút'"),

    ("MOV_ADD_BVA_13", "Kiểm tra chức năng Nhập Năm sản xuất ở giá trị biên min (1900) thành công",
     "Kiểm tra phim cổ điển sản xuất năm 1900",
     "Bước 1: Nhập Năm sản xuất '1900'\nBước 2: Kiểm tra kết quả",
     "Năm sản xuất: '1900'", "Hệ thống chấp nhận năm 1900"),

    ("MOV_ADD_BVA_14", "Kiểm tra chức năng Nhập Năm sản xuất ở giá trị cận biên trên min (1901) thành công",
     "Kiểm tra phim sản xuất năm 1901",
     "Bước 1: Nhập Năm sản xuất '1901'\nBước 2: Kiểm tra kết quả",
     "Năm sản xuất: '1901'", "Hệ thống chấp nhận năm 1901"),

    ("MOV_ADD_BVA_15", "Kiểm tra chức năng Nhập Năm sản xuất ở giá trị cận biên dưới max (2029) thành công",
     "Kiểm tra phim dự kiến sản xuất năm 2029",
     "Bước 1: Nhập Năm sản xuất '2029'\nBước 2: Kiểm tra kết quả",
     "Năm sản xuất: '2029'", "Hệ thống chấp nhận năm 2029"),

    ("MOV_ADD_BVA_16", "Kiểm tra chức năng Nhập Năm sản xuất ở giá trị biên max (2030) thành công",
     "Kiểm tra phim sản xuất năm 2030",
     "Bước 1: Nhập Năm sản xuất '2030'\nBước 2: Kiểm tra kết quả",
     "Năm sản xuất: '2030'", "Hệ thống chấp nhận năm 2030"),

    ("MOV_ADD_BVA_17", "Kiểm tra chức năng Nhập Năm sản xuất ở giá trị cận biên dưới min (1899) thất bại",
     "Kiểm tra chặn năm sản xuất 1899",
     "Bước 1: Nhập Năm sản xuất '1899'\nBước 2: Kiểm tra kết quả",
     "Năm sản xuất: '1899'", "Báo lỗi 'Năm sản xuất không hợp lệ (1900 - 2030)'"),

    ("MOV_ADD_BVA_18", "Kiểm tra chức năng Nhập Năm sản xuất ở giá trị cận biên trên max (2031) thất bại",
     "Kiểm tra chặn năm sản xuất 2031",
     "Bước 1: Nhập Năm sản xuất '2031'\nBước 2: Kiểm tra kết quả",
     "Năm sản xuất: '2031'", "Báo lỗi 'Năm sản xuất không hợp lệ (1900 - 2030)'"),

    ("MOV_ADD_BVA_19", "Kiểm tra chức năng Nhập Tóm tắt nội dung ở giá trị biên min (50 ký tự) thành công",
     "Kiểm tra đoạn tóm tắt đúng 50 ký tự",
     "Bước 1: Nhập đoạn tóm tắt đúng 50 ký tự\nBước 2: Quan sát bộ đếm",
     "Tóm tắt: (Chuỗi 50 ký tự)", "Bộ đếm hiển thị '50/1000 ký tự', không báo lỗi"),

    ("MOV_ADD_BVA_20", "Kiểm tra chức năng Nhập Tóm tắt nội dung ở giá trị cận biên trên min (51 ký tự) thành công",
     "Kiểm tra đoạn tóm tắt 51 ký tự",
     "Bước 1: Nhập đoạn tóm tắt 51 ký tự\nBước 2: Kiểm tra kết quả",
     "Tóm tắt: (Chuỗi 51 ký tự)", "Chấp nhận 51 ký tự hợp lệ"),

    ("MOV_ADD_BVA_21", "Kiểm tra chức năng Nhập Tóm tắt nội dung ở giá trị cận biên dưới max (999 ký tự) thành công",
     "Kiểm tra đoạn tóm tắt 999 ký tự",
     "Bước 1: Nhập đoạn tóm tắt 999 ký tự\nBước 2: Kiểm tra kết quả",
     "Tóm tắt: (Chuỗi 999 ký tự)", "Chấp nhận 999 ký tự hợp lệ"),

    ("MOV_ADD_BVA_22", "Kiểm tra chức năng Nhập Tóm tắt nội dung ở giá trị biên max (1000 ký tự) thành công",
     "Kiểm tra đoạn tóm tắt đạt mức tối đa 1000 ký tự",
     "Bước 1: Nhập đoạn tóm tắt đúng 1000 ký tự\nBước 2: Kiểm tra kết quả",
     "Tóm tắt: (Chuỗi 1000 ký tự)", "Bộ đếm hiển thị '1000/1000 ký tự', không báo lỗi"),

    ("MOV_ADD_BVA_23", "Kiểm tra chức năng Nhập Tóm tắt nội dung ở giá trị cận biên dưới min (49 ký tự) thất bại",
     "Kiểm tra chặn tóm tắt chỉ có 49 ký tự",
     "Bước 1: Nhập đoạn tóm tắt 49 ký tự\nBước 2: Kiểm tra kết quả",
     "Tóm tắt: (Chuỗi 49 ký tự)", "Báo lỗi 'Tóm tắt nội dung phim tối thiểu 50 ký tự'"),

    ("MOV_ADD_BVA_24", "Kiểm tra chức năng Nhập Tóm tắt nội dung ở giá trị cận biên trên max (1001 ký tự) thất bại",
     "Kiểm tra chặn tóm tắt 1001 ký tự",
     "Bước 1: Nhập đoạn tóm tắt 1001 ký tự\nBước 2: Kiểm tra kết quả",
     "Tóm tắt: (Chuỗi 1001 ký tự)", "Hệ thống tự động cắt chuỗi tại 1000 ký tự"),

    ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
    ("MOV_ADD_ERR_01", "Kiểm tra chức năng Để trống Tên phim (Null) thất bại",
     "Kiểm tra bắt buộc nhập trường Tên phim",
     "Bước 1: Để trống ô Tên phim (Null)\nBước 2: Bấm Xuất bản\nBước 3: Kiểm tra phản hồi",
     "Tên phim: Null", "Viền đỏ, hiển thị thông báo lỗi 'Vui lòng nhập tên phim'"),

    ("MOV_ADD_ERR_02", "Kiểm tra chức năng Nhập Tên phim toàn khoảng trắng ('   ') thất bại",
     "Kiểm tra phát hiện chuỗi khoảng trắng",
     "Bước 1: Nhập 5 dấu cách vào ô Tên phim\nBước 2: Bấm Xuất bản\nBước 3: Kiểm tra phản hồi",
     "Tên phim: '     '", "Hệ thống cắt tỉa trim() nhận diện rỗng và báo lỗi 'Vui lòng nhập tên phim'"),

    ("MOV_ADD_ERR_03", "Kiểm tra chức năng Tự động cắt khoảng trắng thừa đầu/cuối của Tên phim thành công",
     "Kiểm tra hàm trim() chuẩn hóa chuỗi Tên phim",
     "Bước 1: Nhập Tên phim: '   Dune: Part Two   '\nBước 2: Bấm Xuất bản\nBước 3: Kiểm tra tên phim lưu trong CSDL",
     "Tên phim: '   Dune: Part Two   '", "Hệ thống tự động lưu tên phim chuẩn: 'Dune: Part Two'"),

    ("MOV_ADD_ERR_04", "Kiểm tra chức năng Nhập Ký tự đặc biệt hợp lệ trong Tên phim thành công",
     "Kiểm tra tên phim chứa dấu hai chấm, gạch nối, dấu ngoặc, số La Mã",
     "Bước 1: Nhập Tên phim: 'Mission: Impossible - Dead Reckoning (Part 1)'\nBước 2: Bấm Xuất bản\nBước 3: Kiểm tra kết quả",
     "Tên phim: 'Mission: Impossible - Dead Reckoning (Part 1)'", "Hệ thống lưu thành công tên phim có ký tự đặc biệt hợp lệ"),

    ("MOV_ADD_ERR_05", "Kiểm tra chức năng Nhập ký tự chữ vào ô Thời lượng thất bại",
     "Kiểm tra chặn kiểu dữ liệu chuỗi chữ vào ô số nguyên Thời lượng",
     "Bước 1: Gõ chữ 'HaiTieng' vào ô Thời lượng\nBước 2: Kiểm tra giá trị trong ô",
     "Thời lượng: 'HaiTieng'", "Ô input type=number không nhận ký tự chữ, giá trị giữ nguyên trống"),

    ("MOV_ADD_ERR_06", "Kiểm tra chức năng Nhập số âm hoặc số thập phân vào Thời lượng thất bại",
     "Kiểm tra chặn số âm và số thực",
     "Bước 1: Nhập Thời lượng '-120' hoặc '120.5'\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Thời lượng: '-120' / '120.5'", "Viền đỏ, hiển thị thông báo lỗi 'Thời lượng phải là số nguyên dương'"),

    ("MOV_ADD_ERR_07", "Kiểm tra chức năng Chưa chọn Thể loại phim (Thể loại rỗng) thất bại",
     "Kiểm tra bắt buộc chọn ít nhất 1 thể loại phim",
     "Bước 1: Không tích chọn bất kỳ thể loại nào trong lưới\nBước 2: Bấm Xuất bản\nBước 3: Kiểm tra phản hồi",
     "Thể loại: [] (Rỗng)", "Hiển thị thông báo lỗi 'Vui lòng chọn ít nhất 1 thể loại'"),

    ("MOV_ADD_ERR_08", "Kiểm tra chức năng Chưa chọn Định dạng phòng chiếu (Định dạng rỗng) thất bại",
     "Kiểm tra bắt buộc chọn ít nhất 1 định dạng (2D, 3D, IMAX...)",
     "Bước 1: Không chọn định dạng nào\nBước 2: Bấm Xuất bản\nBước 3: Kiểm tra phản hồi",
     "Định dạng: [] (Rỗng)", "Hiển thị thông báo lỗi 'Vui lòng chọn ít nhất 1 định dạng'"),

    ("MOV_ADD_ERR_09", "Kiểm tra chức năng Chọn Ngày khởi chiếu trong quá khứ thất bại",
     "Kiểm tra chặn ngày khởi chiếu nhỏ hơn ngày hiện tại",
     "Bước 1: Chọn Ngày khởi chiếu là ngày hôm qua (ví dụ '2026-03-18')\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Ngày khởi chiếu: '2026-03-18' (< Hôm nay)", "Viền đỏ, hiển thị thông báo lỗi 'Ngày khởi chiếu phải từ hôm nay trở đi'"),

    ("MOV_ADD_ERR_10", "Kiểm tra chức năng Chọn Ngày kết thúc trước hoặc bằng Ngày khởi chiếu thất bại",
     "Kiểm tra logic thời gian giữa Ngày khởi chiếu và Ngày kết thúc",
     "Bước 1: Chọn Ngày khởi chiếu '2026-03-25', chọn Ngày kết thúc '2026-03-20'\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Khởi chiếu: 2026-03-25\nKết thúc: 2026-03-20", "Viền đỏ, hiển thị thông báo lỗi 'Ngày kết thúc phải sau ngày khởi chiếu ít nhất 1 ngày'"),

    ("MOV_ADD_ERR_11", "Kiểm tra chức năng Chưa tải ảnh Poster (Poster rỗng) thất bại",
     "Kiểm tra bắt buộc tải ảnh Poster đại diện phim",
     "Bước 1: Điền đủ thông tin các trường nhưng chưa click tải ảnh Poster\nBước 2: Quan sát nút Xuất bản",
     "Ảnh Poster: Chưa tải", "Nút Xuất bản bị vô hiệu hóa (disabled), hiển thị 'Vui lòng tải lên ảnh Poster'"),

    ("MOV_ADD_ERR_12", "Kiểm tra chức năng Tải file ảnh Poster định dạng GIF thất bại",
     "Kiểm tra chặn tải ảnh động GIF làm xấu banner trang chủ",
     "Bước 1: Chọn file ảnh động 'poster_animation.gif'\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "File upload: 'poster_animation.gif'", "Hệ thống từ chối tải lên và báo lỗi: 'Form phim không nhận ảnh GIF (ảnh động). Chỉ dùng JPG, PNG, WEBP.'"),

    ("MOV_ADD_ERR_13", "Kiểm tra chức năng Tải file ảnh Poster dung lượng vượt quá 5MB thất bại",
     "Kiểm tra giới hạn dung lượng ảnh Poster tối đa 5MB",
     "Bước 1: Chọn file ảnh poster 'heavy_poster.png' dung lượng 7.2MB\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Dung lượng file: 7.2MB (> 5MB)", "Hệ thống từ chối tải lên và thông báo file vượt quá dung lượng tối đa 5MB"),

    ("MOV_ADD_ERR_14", "Kiểm tra chức năng Tải file ảnh Banner dung lượng vượt quá 8MB thất bại",
     "Kiểm tra giới hạn dung lượng ảnh Banner tối đa 8MB",
     "Bước 1: Chọn file banner 'huge_banner.jpg' dung lượng 10.5MB\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Dung lượng file: 10.5MB (> 8MB)", "Hệ thống từ chối tải lên và thông báo file vượt quá dung lượng tối đa 8MB"),

    ("MOV_ADD_ERR_15", "Kiểm tra chức năng Bật hiển thị Banner nhưng chưa tải ảnh Banner thất bại",
     "Kiểm tra bắt buộc tải Banner khi bật switch Hiển thị trên Banner trang chủ",
     "Bước 1: Bật switch 'Hiển thị trên Banner trang chủ' nhưng để trống ô tải Banner\nBước 2: Bấm Xuất bản\nBước 3: Kiểm tra kết quả",
     "Switch Banner: Bật\nẢnh Banner: Trống", "Hiển thị thông báo lỗi 'Đang bật hiển thị Banner trang chủ — vui lòng tải ảnh Banner'"),

    ("MOV_ADD_ERR_16", "Kiểm tra chức năng Hủy bỏ Thêm phim mới (Đóng Modal) thành công",
     "Kiểm tra khi Quản trị viên bấm nút 'Hủy bỏ' hoặc click ra ngoài vùng tối",
     "Bước 1: Đang nhập dở thông tin phim\nBước 2: Click button 'Hủy bỏ'\nBước 3: Kiểm tra trạng thái",
     "Thao tác: Click Hủy bỏ", "Modal đóng lại ngay lập tức, không lưu dữ liệu và form được reset về trạng thái trắng"),

    ("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"),
    ("MOV_ADD_FUNC_01", "Kiểm tra chức năng Thêm mới phim Sắp chiếu thành công",
     "Kiểm tra luồng tạo mới phim với Ngày khởi chiếu tương lai (Auto status = upcoming)",
     "Bước 1: Nhập Tên phim: 'Avatar 3', Thời lượng: 195 phút, Trailer Youtube, Ngày khởi chiếu: '2026-12-15', Ngày kết thúc: '2027-01-30'\nBước 2: Chọn Thể loại Hành động + Viễn tưởng, Định dạng IMAX 3D Laser\nBước 3: Tải ảnh Poster hợp lệ và nhập tóm tắt 100 ký tự\nBước 4: Click button 'Xuất bản'\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống",
     "Phim: 'Avatar 3'\nKhởi chiếu: 2026-12-15", "Hệ thống lưu phim thành công, tự động gán trạng thái 'upcoming' (Sắp chiếu) và hiển thị ngay trên bảng danh sách phim"),

    ("MOV_ADD_FUNC_02", "Kiểm tra chức năng Thêm mới phim Đang chiếu có hiển thị Banner trang chủ thành công",
     "Kiểm tra luồng tạo mới phim với Ngày khởi chiếu là hôm nay và tải ảnh Banner",
     "Bước 1: Nhập Tên phim: 'Dune: Part Two', Ngày khởi chiếu: hôm nay, Ngày kết thúc: 1 tháng sau\nBước 2: Tải ảnh Poster và tải ảnh Banner 1920x1080, bật switch 'Hiển thị trên Banner trang chủ'\nBước 3: Click button 'Xuất bản'\nBước 4: Kiểm tra kết quả",
     "Phim: 'Dune 2'\nBanner: Đã tải", "Lưu phim thành công với trạng thái 'active' (Đang chiếu), ảnh Banner xuất hiện trên Slider Trang chủ khách hàng"),

    ("MOV_ADD_FUNC_03", "Kiểm tra chức năng Chọn nhiều Thể loại và nhiều Định dạng đồng thời thành công",
     "Kiểm tra chọn đa thể loại và đa định dạng",
     "Bước 1: Tích chọn 3 thể loại: 'Hành động', 'Kinh dị', 'Tâm lý'\nBước 2: Chọn 2 định dạng: '2D', 'IMAX 3D Laser'\nBước 3: Bấm Xuất bản\nBước 4: Kiểm tra kết quả",
     "Thể loại: 3 loại\nĐịnh dạng: 2 định dạng", "Phim được gắn đủ 3 thể loại và 2 định dạng chiếu trong cơ sở dữ liệu"),

    ("MOV_ADD_FUNC_04", "Kiểm tra chức năng Nhúng và Tải trước Video Trailer YouTube thành công",
     "Kiểm tra tính năng phát thử trailer trực tiếp trong form",
     "Bước 1: Dán link YouTube vào ô Trailer URL\nBước 2: Click nút 'Xem trước Trailer'\nBước 3: Kiểm tra iframe phát video",
     "Trailer: 'https://youtube.com/watch?v=uYPbbksJxIg'", "Trình phát YouTube tải và phát trailer mượt mà ngay trong modal"),

    ("MOV_ADD_FUNC_05", "Kiểm tra chức năng Xem trước Poster tỷ lệ 2/3 ngay sau khi chọn file",
     "Kiểm tra hiển thị ảnh preview tức thì",
     "Bước 1: Chọn file ảnh poster 'poster_test.jpg' từ máy tính\nBước 2: Quan sát khung xem trước bên phải",
     "File: 'poster_test.jpg'", "Khung bên phải lập tức hiển thị hình ảnh poster vừa chọn sắc nét"),

    # =========================================================================
    # CHỨC NĂNG CHỈNH SỬA THÔNG TIN PHIM (EDIT MOVIE MODAL)
    # =========================================================================
    ("__FEATURE__", "CHỈNH SỬA PHIM"),
    ("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)"),
    ("MOV_EDIT_GUI_01", "Kiểm tra chức năng hiển thị Modal Chỉnh sửa với dữ liệu có sẵn thành công",
     "Kiểm tra nạp toàn bộ thông tin cũ của phim vào Form",
     "Bước 1: Quản trị viên click vào icon Chỉnh sửa (Cây bút) tại dòng phim 'Oppenheimer'\nBước 2: Quan sát Modal hiển thị trên màn hình\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Hiển thị Modal với tiêu đề 'CHỈNH SỬA THÔNG TIN PHIM', toàn bộ dữ liệu Tên, Thời lượng, Đạo diễn, Ngày chiếu, Thể loại, Poster được load sẵn 100%"),

    ("MOV_EDIT_GUI_02", "Kiểm tra chức năng hiển thị Khóa ô Ngày khởi chiếu khi phim đang có suất chiếu",
     "Kiểm tra hiển thị trạng thái disabled của ô Ngày khởi chiếu khi phim có lịch chiếu",
     "Bước 1: Mở Modal Chỉnh sửa của bộ phim đang có lịch chiếu hoạt động trong hệ thống\nBước 2: Quan sát ô Ngày khởi chiếu\nBước 3: Kiểm tra kết quả hiển thị từ hệ thống",
     "N/A", "Ô Ngày khởi chiếu bị làm mờ, không thể click chỉnh sửa kèm dòng chữ cảnh báo màu vàng: 'Phim đang có lịch chiếu hoạt động, không thể thay đổi ngày khởi chiếu.'"),

    ("MOV_EDIT_GUI_03", "Kiểm tra chức năng hiển thị Tooltip giải thích khi Trạng thái bị vô hiệu hóa",
     "Kiểm tra tooltip khi hover vào các option trạng thái bị khóa",
     "Bước 1: Mở dropdown Trạng thái của phim đang chiếu có lịch chiếu\nBước 2: Rê chuột vào tùy chọn 'Sắp chiếu' hoặc 'Ngừng chiếu'\nBước 3: Kiểm tra tooltip",
     "N/A", "Hiển thị tooltip: 'Không thể Lưu trữ do phim đang có suất chiếu chưa hoàn tất' hoặc 'Không thể chọn do Ngày khởi chiếu đã/đang diễn ra'"),

    ("MOV_EDIT_GUI_04", "Kiểm tra chức năng hiển thị Nút Cập nhật thành công",
     "Kiểm tra hiển thị nút Cập nhật ở góc dưới bên phải",
     "Bước 1: Quan sát cột bên phải của Modal Chỉnh sửa\nBước 2: Kiểm tra nút tác vụ",
     "N/A", "Hiển thị nút 'Cập nhật' nổi bật thay cho nút 'Xuất bản'"),

    ("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG"),
    ("MOV_EDIT_EP_01", "Kiểm tra chức năng Chỉnh sửa Tên phim hợp lệ [2, 150] ký tự thành công",
     "Kiểm tra cập nhật tên phim mới",
     "Bước 1: Mở Modal Sửa phim 'Oppenheimer'\nBước 2: Sửa tên phim thành: 'Oppenheimer (Bản Mở Rộng)'\nBước 3: Click button 'Cập nhật'\nBước 4: Kiểm tra kết quả",
     "Tên phim mới: 'Oppenheimer (Bản Mở Rộng)'", "Cập nhật thành công, tên phim mới hiển thị ngay ngoài danh sách"),

    ("MOV_EDIT_EP_02", "Kiểm tra chức năng Chỉnh sửa Tên phim thất bại khi xóa trắng",
     "Kiểm tra chặn cập nhật khi xóa rỗng tên phim (khoảng dưới của [2,150])",
     "Bước 1: Xóa trắng toàn bộ ô Tên phim (Null)\nBước 2: Quan sát phản hồi\nBước 3: Kiểm tra kết quả",
     "Tên phim: Null", "Viền đỏ ô Tên phim, báo lỗi 'Vui lòng nhập tên phim', khóa nút Cập nhật"),

    ("MOV_EDIT_EP_03", "Kiểm tra chức năng Chỉnh sửa Thời lượng phim hợp lệ [30, 300] phút thành công",
     "Kiểm tra cập nhật lại thời lượng phim",
     "Bước 1: Đổi thời lượng từ 120 phút thành 180 phút\nBước 2: Click 'Cập nhật'\nBước 3: Kiểm tra kết quả",
     "Thời lượng mới: 180 phút", "Cập nhật thành công thời lượng 180 phút"),

    ("MOV_EDIT_EP_04", "Kiểm tra chức năng Chỉnh sửa Đạo diễn và Diễn viên chính thành công",
     "Kiểm tra cập nhật thông tin ekip làm phim",
     "Bước 1: Bổ sung thêm diễn viên 'Robert Downey Jr.' vào danh sách diễn viên\nBước 2: Click 'Cập nhật'\nBước 3: Kiểm tra kết quả",
     "Diễn viên bổ sung: 'Robert Downey Jr.'", "Cập nhật thành công danh sách diễn viên mới"),

    ("MOV_EDIT_EP_05", "Kiểm tra chức năng Bổ sung thêm Thể loại và Định dạng khi chỉnh sửa thành công",
     "Kiểm tra thêm thể loại cho phim đã tạo",
     "Bước 1: Click chọn thêm thể loại 'Tiểu sử' và định dạng 'IMAX'\nBước 2: Click 'Cập nhật'",
     "Thể loại thêm: 'Tiểu sử'\nĐịnh dạng thêm: 'IMAX'", "Cập nhật thành công thể loại và định dạng mới cho phim"),

    ("MOV_EDIT_EP_06", "Kiểm tra chức năng Gia hạn Ngày kết thúc phim thành công",
     "Kiểm tra kéo dài thời gian chiếu phim thêm 15 ngày",
     "Bước 1: Đổi Ngày kết thúc từ '2026-04-15' thành '2026-04-30'\nBước 2: Click 'Cập nhật'",
     "Ngày kết thúc mới: 2026-04-30", "Cập nhật ngày kết thúc thành công, cho phép xếp lịch chiếu đến hết tháng 4"),

    ("MOV_EDIT_EP_07", "Kiểm tra chức năng Chỉnh sửa Tóm tắt nội dung hợp lệ [50, 1000] ký tự thành công",
     "Kiểm tra cập nhật lại cốt truyện",
     "Bước 1: Sửa đoạn tóm tắt nội dung dài 150 ký tự\nBước 2: Click 'Cập nhật'",
     "Tóm tắt mới: (Đoạn văn 150 ký tự)", "Cập nhật thành công tóm tắt mới"),

    ("MOV_EDIT_EP_08", "Kiểm tra chức năng Chuyển Trạng thái phim sang 'Ngừng chiếu' (archived) thành công",
     "Kiểm tra lưu trữ phim khi hết đợt chiếu",
     "Bước 1: Phim không còn suất chiếu nào trong tương lai\nBước 2: Chọn Trạng thái: 'Ngừng chiếu' (archived)\nBước 3: Click 'Cập nhật'",
     "Trạng thái: 'archived'", "Chuyển trạng thái sang Ngừng chiếu thành công, phim ẩn khỏi danh sách đặt vé"),

    ("MOV_EDIT_EP_09", "Kiểm tra chức năng Thay đổi link Trailer YouTube mới thành công",
     "Kiểm tra cập nhật video trailer mới",
     "Bước 1: Dán link Trailer mới 'https://youtube.com/watch?v=new_trailer_id'\nBước 2: Click 'Cập nhật'",
     "Trailer URL: 'https://youtube.com/watch?v=new_trailer_id'", "Cập nhật thành công link trailer mới"),

    ("MOV_EDIT_EP_10", "Kiểm tra chức năng Bật/Tắt switch Hiển thị trên Banner trang chủ thành công",
     "Kiểm tra gỡ phim khỏi banner trang chủ",
     "Bước 1: Tắt switch 'Hiển thị trên Banner trang chủ'\nBước 2: Click 'Cập nhật'",
     "Switch Banner: Tắt (False)", "Phim được gỡ khỏi banner trang chủ thành công"),

    ("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN"),
    ("MOV_EDIT_BVA_01", "Kiểm tra chức năng Sửa Tên phim về giá trị biên min (2 ký tự) thành công",
     "Kiểm tra đổi tên phim thành 2 ký tự",
     "Bước 1: Đổi Tên phim thành 'UP' (2 ký tự)\nBước 2: Click 'Cập nhật'",
     "Tên phim mới: 'UP' (2 ký tự)", "Cập nhật thành công tên phim 2 ký tự"),

    ("MOV_EDIT_BVA_02", "Kiểm tra chức năng Sửa Tên phim về giá trị biên max (150 ký tự) thành công",
     "Kiểm tra đổi tên phim thành 150 ký tự",
     "Bước 1: Đổi Tên phim thành chuỗi 150 ký tự\nBước 2: Click 'Cập nhật'",
     "Tên phim mới: (Chuỗi 150 ký tự)", "Cập nhật thành công tên phim 150 ký tự"),

    ("MOV_EDIT_BVA_03", "Kiểm tra chức năng Sửa Thời lượng về giá trị biên min (30 phút) thành công",
     "Kiểm tra cập nhật thời lượng 30 phút",
     "Bước 1: Đổi Thời lượng thành '30' phút\nBước 2: Click 'Cập nhật'",
     "Thời lượng mới: '30' (phút)", "Cập nhật thành công 30 phút"),

    ("MOV_EDIT_BVA_04", "Kiểm tra chức năng Sửa Thời lượng về giá trị biên max (300 phút) thành công",
     "Kiểm tra cập nhật thời lượng 300 phút",
     "Bước 1: Đổi Thời lượng thành '300' phút\nBước 2: Click 'Cập nhật'",
     "Thời lượng mới: '300' (phút)", "Cập nhật thành công 300 phút"),

    ("MOV_EDIT_BVA_05", "Kiểm tra chức năng Sửa Tóm tắt nội dung về giá trị biên min (50 ký tự) thành công",
     "Kiểm tra cập nhật tóm tắt đúng 50 ký tự",
     "Bước 1: Sửa Tóm tắt thành đoạn văn đúng 50 ký tự\nBước 2: Click 'Cập nhật'",
     "Tóm tắt mới: (Chuỗi 50 ký tự)", "Cập nhật thành công"),

    ("MOV_EDIT_BVA_06", "Kiểm tra chức năng Sửa Tóm tắt nội dung về giá trị biên max (1000 ký tự) thành công",
     "Kiểm tra cập nhật tóm tắt đúng 1000 ký tự",
     "Bước 1: Sửa Tóm tắt thành đoạn văn đúng 1000 ký tự\nBước 2: Click 'Cập nhật'",
     "Tóm tắt mới: (Chuỗi 1000 ký tự)", "Cập nhật thành công"),

    ("MOV_EDIT_BVA_07", "Kiểm tra chức năng Sửa Tên phim xuống 1 ký tự thất bại",
     "Kiểm tra chặn sửa tên phim thành 1 ký tự",
     "Bước 1: Sửa Tên phim thành 'X' (1 ký tự)\nBước 2: Click 'Cập nhật'",
     "Tên phim mới: 'X' (1 ký tự)", "Viền đỏ, báo lỗi 'Tên phim phải từ 2 ký tự'"),

    ("MOV_EDIT_BVA_08", "Kiểm tra chức năng Sửa Thời lượng xuống 29 phút thất bại",
     "Kiểm tra chặn sửa thời lượng dưới 30 phút",
     "Bước 1: Sửa Thời lượng thành '29' phút\nBước 2: Click 'Cập nhật'",
     "Thời lượng mới: '29' (phút)", "Báo lỗi thời lượng tối thiểu 30 phút"),

    ("MOV_EDIT_BVA_09", "Kiểm tra chức năng Sửa Thời lượng lên 301 phút thất bại",
     "Kiểm tra chặn sửa thời lượng trên 300 phút",
     "Bước 1: Sửa Thời lượng thành '301' phút\nBước 2: Click 'Cập nhật'",
     "Thời lượng mới: '301' (phút)", "Báo lỗi thời lượng tối đa 300 phút"),

    ("MOV_EDIT_BVA_10", "Kiểm tra chức năng Sửa Tóm tắt nội dung xuống 49 ký tự thất bại",
     "Kiểm tra chặn sửa tóm tắt dưới 50 ký tự",
     "Bước 1: Sửa Tóm tắt thành đoạn văn 49 ký tự\nBước 2: Click 'Cập nhật'",
     "Tóm tắt mới: (Chuỗi 49 ký tự)", "Báo lỗi tóm tắt tối thiểu 50 ký tự"),

    ("__SECTION__", "KỸ THUẬT ĐOÁN LỖI"),
    ("MOV_EDIT_ERR_01", "Kiểm tra chức năng Giữ nguyên thông tin cũ khi không thay đổi gì và bấm Cập nhật",
     "Kiểm tra không bị lỗi khi mở modal ra rồi bấm Cập nhật ngay",
     "Bước 1: Mở Modal Chỉnh sửa phim 'Oppenheimer'\nBước 2: Không sửa bất kỳ trường nào, bấm ngay nút 'Cập nhật'\nBước 3: Kiểm tra kết quả",
     "Dữ liệu: Giữ nguyên", "Hệ thống xác nhận thành công, dữ liệu phim giữ nguyên vẹn 100%"),

    ("MOV_EDIT_ERR_02", "Kiểm tra chức năng Thay đổi ảnh Poster mới thành công",
     "Kiểm tra tải đè ảnh poster mới thay thế ảnh cũ",
     "Bước 1: Click vào khung Poster, chọn file ảnh mới 'oppenheimer_new_poster.jpg'\nBước 2: Click 'Cập nhật'",
     "Poster mới: 'oppenheimer_new_poster.jpg'", "Ảnh poster mới được cập nhật và thay thế ảnh cũ ngay lập tức"),

    ("MOV_EDIT_ERR_03", "Kiểm tra chức năng Thay đổi ảnh Banner mới thành công",
     "Kiểm tra tải đè ảnh banner mới thay thế banner cũ",
     "Bước 1: Click vào khung Banner, chọn file ảnh mới 'oppenheimer_wide_banner.jpg'\nBước 2: Click 'Cập nhật'",
     "Banner mới: 'oppenheimer_wide_banner.jpg'", "Ảnh banner mới được cập nhật thành công"),

    ("MOV_EDIT_ERR_04", "Kiểm tra chức năng Bỏ chọn toàn bộ thể loại (Rỗng thể loại) thất bại",
     "Kiểm tra chặn khi người dùng bỏ hết các tick thể loại khi sửa",
     "Bước 1: Bỏ tích tất cả các thể loại đang chọn\nBước 2: Click 'Cập nhật'",
     "Thể loại: [] (Rỗng)", "Hiển thị thông báo lỗi 'Vui lòng chọn ít nhất 1 thể loại'"),

    ("MOV_EDIT_ERR_05", "Kiểm tra chức năng Bỏ chọn toàn bộ định dạng (Rỗng định dạng) thất bại",
     "Kiểm tra chặn khi người dùng bỏ hết các định dạng đang chọn khi sửa",
     "Bước 1: Bỏ chọn tất cả các định dạng đang chọn\nBước 2: Click 'Cập nhật'",
     "Định dạng: [] (Rỗng)", "Hiển thị thông báo lỗi 'Vui lòng chọn ít nhất 1 định dạng'"),

    ("MOV_EDIT_ERR_06", "Kiểm tra chức năng Sửa Ngày kết thúc về trước ngày có suất chiếu đã xếp thất bại",
     "Kiểm tra logic ngày kết thúc không được cắt ngắn trước các suất chiếu đã tạo",
     "Bước 1: Phim đã có suất chiếu vào ngày '2026-04-10'\nBước 2: Cố tình sửa Ngày kết thúc thành '2026-04-05'\nBước 3: Click 'Cập nhật'",
     "Ngày kết thúc sửa: 2026-04-05\nSuất chiếu tồn tại: 2026-04-10", "Viền đỏ, hiển thị cảnh báo lỗi: 'Ngày kết thúc không được trước ngày của các suất chiếu đã lên lịch (2026-04-10)'"),

    ("MOV_EDIT_ERR_07", "Kiểm tra chức năng Tải ảnh Poster định dạng không hợp lệ (.pdf, .exe) thất bại",
     "Kiểm tra chặn file không phải ảnh",
     "Bước 1: Chọn file 'document.pdf' khi tải poster\nBước 2: Kiểm tra phản hồi",
     "File upload: 'document.pdf'", "Hệ thống từ chối tải lên và báo lỗi định dạng file"),

    ("MOV_EDIT_ERR_08", "Kiểm tra chức năng Đóng Modal chỉnh sửa mà không lưu thay đổi thành công",
     "Kiểm tra khi bấm Hủy bỏ trên Modal Sửa",
     "Bước 1: Sửa tên phim thành 'Tên Tạm'\nBước 2: Bấm nút 'Hủy bỏ'\nBước 3: Kiểm tra ngoài danh sách",
     "Thao tác: Click Hủy bỏ", "Modal đóng lại, tên phim ngoài danh sách vẫn giữ nguyên tên gốc ban đầu"),

    ("__SECTION__", "KIỂM TRA RÀNG BUỘC SUẤT CHIẾU & NGHIỆP VỤ (SHOWTIME GUARDRAILS)"),
    ("MOV_EDIT_GRD_01", "Kiểm tra chức năng Chặn sửa Ngày khởi chiếu khi phim đã có suất chiếu trong hệ thống",
     "Kiểm tra guardrail bảo vệ tính toàn vẹn của lịch chiếu đã mở bán",
     "Bước 1: Phim 'Dune 2' đang có suất chiếu ngày '2026-03-20'\nBước 2: Mở Modal Chỉnh sửa phim\nBước 3: Cố tình click vào ô Ngày khởi chiếu\nBước 4: Kiểm tra phản hồi từ hệ thống",
     "Trạng thái phim: Đang có suất chiếu", "Ô Ngày khởi chiếu bị khóa hoàn toàn (disabled), tooltip hiển thị 'Phim đang có lịch chiếu hoạt động, không thể thay đổi ngày khởi chiếu.'"),

    ("MOV_EDIT_GRD_02", "Kiểm tra chức năng Chặn chuyển trạng thái sang 'Sắp chiếu' khi Ngày khởi chiếu đã tới",
     "Kiểm tra guardrail trạng thái khi ngày khởi chiếu <= hôm nay",
     "Bước 1: Phim có Ngày khởi chiếu là ngày hôm nay ('2026-03-19')\nBước 2: Mở dropdown Trạng thái, chọn 'Sắp chiếu' (upcoming)\nBước 3: Click 'Cập nhật'",
     "Khởi chiếu: 2026-03-19 (Hôm nay)\nTrạng thái chọn: 'upcoming'", "Hệ thống từ chối và cảnh báo: 'Không thể đặt trạng thái Sắp chiếu do Ngày khởi chiếu đã bắt đầu'"),

    ("MOV_EDIT_GRD_03", "Kiểm tra chức năng Chặn chuyển trạng thái sang 'Ngừng chiếu' khi còn suất chiếu chưa chiếu",
     "Kiểm tra guardrail bảo vệ khách hàng đã mua vé xem phim",
     "Bước 1: Phim đang có 3 suất chiếu vào ngày mai\nBước 2: Mở dropdown Trạng thái, chọn 'Ngừng chiếu' (archived)\nBước 3: Click 'Cập nhật'",
     "Suất chiếu chưa chiếu: 3 suất\nTrạng thái chọn: 'archived'", "Hệ thống từ chối và cảnh báo: 'Không thể Lưu trữ/Ngừng chiếu do phim đang có suất chiếu chưa hoàn tất'"),

    ("MOV_EDIT_GRD_04", "Kiểm tra chức năng Cho phép sửa Ngày khởi chiếu khi phim CHƯA có bất kỳ suất chiếu nào",
     "Kiểm tra linh hoạt đổi lịch ra mắt phim khi chưa xếp lịch chiếu",
     "Bước 1: Phim mới tạo, chưa có suất chiếu nào\nBước 2: Mở Modal Sửa, đổi Ngày khởi chiếu từ '2026-04-01' sang '2026-04-15'\nBước 3: Click 'Cập nhật'",
     "Suất chiếu: 0 suất\nKhởi chiếu mới: 2026-04-15", "Ô ngày khởi chiếu cho phép sửa bình thường và cập nhật thành công"),

    ("MOV_EDIT_GRD_05", "Kiểm tra chức năng Xóa phim thành công khi phim chưa có suất chiếu và chưa bán vé",
     "Kiểm tra luồng xóa phim an toàn",
     "Bước 1: Quản trị viên click icon Xóa (Thùng rác) tại dòng phim chưa có lịch chiếu\nBước 2: Xác nhận 'Đồng ý xóa' trên Modal xác nhận\nBước 3: Kiểm tra danh sách phim",
     "Phim: Chưa có suất chiếu, chưa bán vé", "Hệ thống xóa phim thành công và biến mất khỏi bảng danh sách phim"),

    ("MOV_EDIT_GRD_06", "Kiểm tra chức năng Chặn Xóa phim khi phim đang có suất chiếu hoặc đã bán vé",
     "Kiểm tra bảo vệ ràng buộc toàn vẹn dữ liệu đơn hàng và vé",
     "Bước 1: Quản trị viên click icon Xóa tại phim 'Oppenheimer' đã có dữ liệu vé bán\nBước 2: Kiểm tra phản hồi từ hệ thống",
     "Phim: Đã có lịch chiếu và vé bán", "Hệ thống từ chối xóa và thông báo: 'Không thể xóa phim này do đã phát sinh suất chiếu và dữ liệu giao dịch vé. Vui lòng chuyển sang trạng thái Ngừng chiếu.'"),

    ("MOV_EDIT_GRD_07", "Kiểm tra chức năng Cập nhật đồng bộ thông tin phim ra Trang chủ khách hàng ngay tức thì",
     "Kiểm tra đồng bộ dữ liệu sau khi sửa phim thành công",
     "Bước 1: Sửa tên phim 'Dune: Part Two' thành 'Dune: Part Two (IMAX Edition)' và bấm Cập nhật\nBước 2: Mở trang chủ khách hàng trên tab trình duyệt khác\nBước 3: Quan sát thẻ phim ngoài trang chủ",
     "Tên phim mới: 'Dune: Part Two (IMAX Edition)'", "Trang chủ khách hàng cập nhật ngay tên phim mới và poster mới mà không gặp độ trễ")
]

full_movie_suite = tc_movies
