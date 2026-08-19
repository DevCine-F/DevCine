# PHỤ LỤC A: ĐẶC TẢ USE CASE HỆ THỐNG DEVCINE

## A.1. ĐẶC TẢ CÁC USE CASE KHỐI KHÁCH HÀNG (CUSTOMER PORTAL)

| Mã Use case | UC-01 | Tên Use Case | Quản lý tài khoản khách hàng |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Bao gồm toàn bộ quy trình quản lý định danh người dùng: Đăng ký tài khoản thành viên mới, Đăng nhập hệ thống bằng Số điện thoại hoặc Email, Khôi phục mật khẩu qua Email bằng mã xác thực (OTP), Xem và cập nhật thông tin cá nhân, và Đổi mật khẩu tài khoản. <td colspan=3/> |
| Luồng chạy | Bước 1 (Đăng ký): Khách hàng truy cập trang đăng ký, nhập họ tên, số điện thoại, email và mật khẩu bảo mật. Hệ thống kiểm tra dữ liệu hợp lệ và duy nhất, tự động khởi tạo tài khoản hạng Đồng (Bronze) và đăng nhập vào hệ thống. <br> Bước 2 (Đăng nhập): Khách hàng nhập số điện thoại hoặc email cùng mật khẩu tại form đăng nhập. Hệ thống xác thực danh tính, lưu trạng thái phiên và tải thông tin điểm tích lũy, hạng thẻ. <br> Bước 3 (Khôi phục mật khẩu): Khi bị quên mật khẩu, khách hàng nhập email để nhận mã xác thực OTP gồm 6 chữ số. Khách hàng nhập đúng mã OTP và thiết lập mật khẩu mới. <br> Bước 4 (Cập nhật hồ sơ): Khách hàng vào mục Hồ sơ cá nhân để xem thông tin, điểm thưởng, cấp bậc và cập nhật họ tên hoặc thông tin liên lạc khi cần thiết. <br> Bước 5 (Đổi mật khẩu): Khách hàng nhập mật khẩu hiện tại cùng mật khẩu mới để thay đổi mật khẩu định kỳ nhằm nâng cao tính an toàn. <td colspan=3/> |
| Lưu ý | - Không cho phép đăng ký hoặc cập nhật số điện thoại/email trùng lặp với tài khoản khác đã có trong hệ thống. <br> - Mật khẩu phải đáp ứng tiêu chuẩn an toàn từ 8 đến 32 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt. <br> - Mã xác thực OTP qua email chỉ có hiệu lực ngắn hạn và chỉ được sử dụng một lần duy nhất. <td colspan=3/> |

Bảng A.1: Use case quản lý tài khoản khách hàng.

---

| Mã Use case | UC-02 | Tên Use Case | Tra cứu thông tin phim và cụm rạp |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Hỗ trợ khách hàng tìm kiếm, lọc và xem thông tin chi tiết của các bộ phim (phim đang chiếu, phim sắp chiếu, video trailer, tóm tắt nội dung, diễn viên, giới hạn độ tuổi) và tra cứu danh sách các cụm rạp, phòng chiếu cùng lịch chiếu theo ngày và khu vực. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng truy cập Trang chủ hoặc mục Lịch chiếu, chuyển đổi giữa danh sách "Phim đang chiếu" và "Phim sắp chiếu". <br> Bước 2: Khách hàng tìm kiếm theo tên phim hoặc lọc phim theo các tiêu chí: Thể loại (Hành động, Hài, Hoạt hình...), Cụm rạp chiếu, Định dạng (2D, 3D, IMAX). <br> Bước 3: Khách hàng chọn một bộ phim để xem trang chi tiết: Áp-phích, tên phim, thể loại, thời lượng, phân loại độ tuổi (P, K, T13, T16, T18, C), đạo diễn, diễn viên, tóm tắt nội dung và các suất chiếu theo từng rạp. <br> Bước 4: Khách hàng nhấn nút "Xem Trailer" để mở cửa sổ phát video giới thiệu chính thức của phim. <br> Bước 5: Khách hàng tra cứu lịch chiếu tổng hợp bằng cách chọn Cụm rạp (theo tỉnh/thành phố) và Ngày chiếu mong muốn để xem danh sách các suất chiếu khả dụng. <td colspan=3/> |
| Lưu ý | - Chỉ các bộ phim và cụm rạp đang ở trạng thái kích hoạt công khai mới được hiển thị trên giao diện người dùng. <br> - Các suất chiếu đã diễn ra trong quá khứ sẽ tự động được ẩn đi để tránh nhầm lẫn cho khách hàng. <br> - Hệ thống hiển thị rõ ràng nhãn cảnh báo giới hạn độ tuổi theo quy định của Cục Điện ảnh. <td colspan=3/> |

Bảng A.2: Use case tra cứu thông tin phim và cụm rạp.

---

| Mã Use case | UC-03 | Tên Use Case | Đặt vé trực tuyến và Thanh toán VNPAY (Quy trình cốt lõi) |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Quy trình khép kín cho phép khách hàng chọn suất chiếu, chọn vị trí ghế ngồi trên sơ đồ trực quan (hệ thống tự động khóa ghế tạm thời 10 phút), lựa chọn thêm bắp nước/combo có tùy biến khẩu vị, áp dụng mã giảm giá và thanh toán trực tuyến qua cổng VNPAY. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng chọn một suất chiếu cụ thể và chuyển sang giao diện Sơ đồ phòng chiếu. <br> Bước 2: Hệ thống hiển thị sơ đồ mặt bằng ghế theo thời gian thực (Ghế thường, Ghế VIP, Ghế Sweetbox). Khách hàng chọn vị trí ghế ngồi và chọn đối tượng khán giả (Người lớn, Học sinh/Sinh viên). <br> Bước 3: Khách hàng nhấn "Tiếp tục", hệ thống thực hiện khóa giữ chỗ tạm thời các ghế đã chọn trong thời gian 10 phút. <br> Bước 4: Khách hàng chọn thêm các món bắp rang, nước ngọt hoặc combo ưu đãi kèm theo, đồng thời tùy biến lựa chọn vị bắp (phô mai, caramel) và loại nước ngọt tương ứng. <br> Bước 5: Tại bước thanh toán, khách hàng nhập mã khuyến mãi hoặc chọn voucher từ ví ưu đãi cá nhân để được khấu trừ giảm giá. <br> Bước 6: Khách hàng kiểm tra tóm tắt đơn hàng, chọn phương thức VNPAY và chuyển hướng sang cổng thanh toán bảo mật VNPAY để quét mã QR hoặc nhập thông tin thẻ. <br> Bước 7: Sau khi thanh toán thành công, VNPAY phản hồi về hệ thống; hệ thống xác thực chữ ký số, chuyển đơn hàng sang trạng thái Đã xác nhận (Confirmed), hoàn tất đặt chỗ và cộng điểm thưởng thành viên. <td colspan=3/> |
| Lưu ý | - Hệ thống kiểm tra nghiêm ngặt quy tắc không để lại ghế trống đơn lẻ (orphan seat) ở giữa hoặc đầu hàng ghế. <br> - Nếu khách hàng không hoàn tất thanh toán trong vòng 10 phút giữ chỗ, hệ thống sẽ tự động hủy đơn và giải phóng ghế về trạng thái trống. <br> - Số tiền giảm giá được máy chủ tính toán độc lập chống tình trạng gian lận giá vé. <td colspan=3/> |

Bảng A.3: Use case đặt vé trực tuyến và thanh toán vnpay (quy trình cốt lõi).

---

| Mã Use case | UC-04 | Tên Use Case | Quản lý vé điện tử và Lịch sử giao dịch |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Cung cấp vé điện tử (mã đặt vé và mã QR Code) sau khi thanh toán thành công, gửi email tự động xác nhận đơn hàng và cho phép khách hàng tra cứu lại toàn bộ lịch sử các đơn vé đã mua cùng tình trạng sử dụng. <td colspan=3/> |
| Luồng chạy | Bước 1: Ngay sau khi thanh toán thành công, hệ thống hiển thị màn hình vé điện tử chứa đầy đủ thông tin: Mã đặt vé, Tên phim, Cụm rạp, Phòng chiếu, Thời gian chiếu, Vị trí ghế, Danh sách bắp nước và Mã QR Code xác thực. <br> Bước 2: Hệ thống tự động gửi thư điện tử chứa hóa đơn thanh toán và thông tin vé chi tiết đến địa chỉ email của khách hàng. <br> Bước 3: Khách hàng đăng nhập và truy cập mục "Lịch sử đặt vé" trong trang cá nhân để xem danh sách toàn bộ các đơn hàng theo thứ tự thời gian từ mới nhất đến cũ nhất. <br> Bước 4: Khách hàng nhấn chọn vào từng đơn hàng để xem lại thông tin chi tiết, tình trạng vé (Đã thanh toán, Đã in vé/Check-in, Đã hủy) và hiển thị lại mã QR để xuất trình cho nhân viên soát vé tại rạp. <td colspan=3/> |
| Lưu ý | - Dữ liệu lịch sử lưu vết snapshot toàn bộ thông tin giá vé và tên món ăn tại thời điểm giao dịch, bảo toàn tính chính xác vĩnh viễn. <br> - Khách hàng có thể lưu ảnh mã QR hoặc mở trực tiếp trên website để làm thủ tục vào phòng chiếu. <td colspan=3/> |

Bảng A.4: Use case quản lý vé điện tử và lịch sử giao dịch.

---

| Mã Use case | UC-05 | Tên Use Case | Quản lý Ví Voucher và Đánh giá phim |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Khách hàng |
| Mô tả | Cung cấp các tính năng tương tác và duy trì khách hàng thân thiết: Lưu trữ và quản lý mã khuyến mãi trong ví cá nhân, sử dụng điểm thưởng tích lũy để đổi voucher giảm giá, và gửi đánh giá chấm điểm sao cho các bộ phim đã xem. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng truy cập mục "Ưu đãi của tôi" để xem danh sách voucher khả dụng và lịch sử các voucher đã sử dụng hoặc hết hạn. <br> Bước 2: Khách hàng nhập mã ưu đãi bí mật nhận được từ các kênh quảng bá để lưu trực tiếp vào ví voucher cá nhân. <br> Bước 3: Tại tab "Đổi điểm lấy ưu đãi", khách hàng sử dụng điểm tích lũy thành viên (Loyalty Points) nhấn "Đổi ngay" tại các gói ưu đãi; hệ thống trừ điểm tương ứng và sinh voucher mới vào ví. <br> Bước 4: Đối với các bộ phim mà khách hàng đã từng mua vé xem thành công, khách hàng truy cập trang chi tiết phim để chấm điểm sao (1 đến 5 sao) và nhập nhận xét cảm nghĩ. <br> Bước 5: Hệ thống ghi nhận đánh giá, cập nhật điểm trung bình của bộ phim và hiển thị nhận xét công khai trên website. <td colspan=3/> |
| Lưu ý | - Khách hàng chưa mua vé xem bộ phim đó sẽ không được cấp quyền gửi đánh giá nhằm ngăn ngừa tình trạng đánh giá ảo hoặc tiêu cực vô căn cứ. <br> - Mỗi gói ưu đãi quy đổi điểm có thể bị giới hạn số lần đổi tối đa cho mỗi tài khoản thành viên. <td colspan=3/> |

Bảng A.5: Use case quản lý ví voucher và đánh giá phim.

---

## A.2. ĐẶC TẢ CÁC USE CASE KHỐI QUẢN TRỊ VIÊN & QUẢN LÝ (ADMIN / MANAGER PORTAL)

| Mã Use case | UC-06 | Tên Use Case | Xem Bảng điều khiển (Dashboard) thống kê |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Cung cấp bức tranh toàn cảnh về hiệu quả kinh doanh của hệ thống rạp thông qua các biểu đồ số liệu thời gian thực: Tổng doanh thu, Lượng vé bán ra, Số lượng khách hàng mới, Tỷ lệ lấp đầy ghế và bảng xếp hạng các bộ phim ăn khách nhất. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên đăng nhập trang quản trị và truy cập phân hệ "Tổng quan (Dashboard)". <br> Bước 2: Quản trị viên lựa chọn khoảng thời gian cần theo dõi: Hôm nay, Tuần này, Tháng này hoặc chọn Tháng/Năm cụ thể qua bộ chọn thời gian. <br> Bước 3: Hệ thống tổng hợp và hiển thị các thẻ chỉ số hiệu suất chính: Tổng doanh thu, Số vé bán ra, Lượng khách hàng mới, Tỷ lệ lấp đầy phòng chiếu kèm tỷ lệ tăng trưởng so với kỳ trước. <br> Bước 4: Hệ thống hiển thị Biểu đồ diễn biến doanh thu và lượng vé theo ngày, Bảng xếp hạng Top các bộ phim có doanh thu cao nhất và danh sách các giao dịch đặt vé gần nhất. <td colspan=3/> |
| Lưu ý | - Quản lý chi nhánh chỉ xem được số liệu thống kê thuộc cụm rạp mà mình được phân công phụ trách. <br> - Quản trị viên cấp cao (Admin) có quyền xem số liệu tổng hợp của toàn hệ thống hoặc lọc theo từng cụm rạp cụ thể. <td colspan=3/> |

Bảng A.6: Use case xem bảng điều khiển (dashboard) thống kê.

---

| Mã Use case | UC-07 | Tên Use Case | Quản lý Hạ tầng Rạp và Sơ đồ ghế |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Quản lý danh sách các cụm rạp chi nhánh, thiết lập quy mô phòng chiếu và sử dụng công cụ thiết kế ma trận ghế trực quan (phân bổ ghế Thường, VIP, Đôi Sweetbox, Lối đi và Khóa bảo trì ghế hỏng vật lý). <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên thêm mới hoặc chỉnh sửa thông tin Cụm rạp: Tên cụm rạp, Tỉnh/Thành phố, Địa chỉ chi tiết, Số điện thoại liên hệ và hình ảnh đại diện. <br> Bước 2: Tại tab Cơ sở vật chất của rạp, quản trị viên thêm mới Phòng chiếu và gán định dạng công nghệ hỗ trợ (2D, 3D, IMAX). <br> Bước 3: Quản trị viên mở công cụ thiết kế sơ đồ ghế trực quan, thiết lập số hàng và số cột tổng thể của phòng chiếu. <br> Bước 4: Sử dụng công cụ cọ vẽ (brush) để gán nhãn và phân loại ghế: Ghế thường, Ghế VIP, Ghế đôi Sweetbox hoặc làm Khoảng trống / Lối đi. <br> Bước 5: Quản trị viên có thể thao tác khóa bảo trì (MAINTENANCE) cho các ghế bị hư hỏng vật lý để chặn không cho bán vé. <br> Bước 6: Nhấn "Lưu sơ đồ ghế", hệ thống tự động khởi tạo danh sách ghế thực tế trong cơ sở dữ liệu. <td colspan=3/> |
| Lưu ý | - Sơ đồ ghế mới chỉ áp dụng cho các suất chiếu được tạo sau thời điểm lưu sơ đồ. <br> - Không thể xóa phòng chiếu nếu đang có các suất chiếu được lên lịch hoạt động trong phòng đó. <td colspan=3/> |

Bảng A.7: Use case quản lý hạ tầng rạp và sơ đồ ghế.

---

| Mã Use case | UC-08 | Tên Use Case | Quản lý Phim, Lập lịch và Điều phối suất chiếu |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Kiểm soát dữ liệu phim (thêm phim, cập nhật trạng thái phát hành, tải áp-phích/banner lên máy chủ đám mây, danh mục thể loại, định dạng, độ tuổi) và lập lịch chiếu phim trên dòng thời gian trực quan (tự động kiểm tra xung đột giờ chiếu và thời gian dọn phòng). <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên thêm phim mới, nhập đầy đủ thông tin: Tên phim, Thể loại, Thời lượng, Giới hạn độ tuổi, Ngày khởi chiếu, Đạo diễn, Diễn viên, Tóm tắt nội dung, Trailer và tải ảnh Poster/Banner lên dịch vụ lưu trữ đám mây. <br> Bước 2: Cập nhật trạng thái hiển thị của phim (Đang chiếu, Sắp chiếu, Ngừng chiếu) hoặc quản lý các danh mục Thể loại, Định dạng, Độ tuổi kiểm duyệt. <br> Bước 3: Truy cập tab Lịch chiếu của cụm rạp, chọn ngày chiếu và nhấn vào khung giờ trống trên dòng thời gian để tạo suất chiếu lẻ hoặc tạo suất hàng loạt. <br> Bước 4: Chọn Phim, Phòng chiếu, Định dạng và Thời gian bắt đầu. Hệ thống tự động cộng thời lượng phim cùng thời gian dọn dẹp phòng (15 phút) để tính thời gian kết thúc và kiểm tra xung đột phòng chiếu. <br> Bước 5: Nếu không có xung đột, quản trị viên nhấn "Xuất bản suất chiếu" để mở bán vé trên toàn hệ thống. <td colspan=3/> |
| Lưu ý | - Hệ thống tự động phát hiện và ngăn chặn hoàn toàn việc lưu nếu thời gian hai suất chiếu trong cùng một phòng bị chồng lấn lên nhau. <br> - Không cho phép xóa vĩnh viễn phim đã từng phát sinh giao dịch đặt vé nhằm bảo toàn tính toàn vẹn dữ liệu lịch sử. <td colspan=3/> |

Bảng A.8: Use case quản lý phim, lập lịch và điều phối suất chiếu.

---

| Mã Use case | UC-09 | Tên Use Case | Thiết lập Quy tắc Bảng giá vé |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Cấu hình động công thức tính giá vé xem phim áp dụng thống nhất cho toàn hệ thống: Giá nền theo Thứ trong tuần × Khung giờ × Đối tượng; Phụ thu loại ghế; Phụ thu định dạng; Phụ thu ngày lễ và Công cụ mô phỏng tính thử giá vé. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ Cấu hình Bảng giá. <br> Bước 2: Thiết lập ma trận Giá nền cơ bản theo các ngày trong tuần (Thứ 2 đến Thứ 5, Cuối tuần), Khung giờ (Trước/Sau 12h) và Đối tượng khán giả (Người lớn, Học sinh/Sinh viên). <br> Bước 3: Thiết lập mức phụ thu riêng cho Loại ghế (Ghế VIP, Ghế Sweetbox) và Định dạng phòng chiếu (3D, IMAX). <br> Bước 4: Quản lý danh sách các Ngày lễ quốc gia và cấu hình mức phụ thu áp dụng trong các dịp lễ. <br> Bước 5: Sử dụng tab "Tính thử giá vé (Simulator)" để nhập các điều kiện giả định và kiểm tra công thức bóc tách cấu thành giá vé: [Giá nền] + [Phụ thu ghế] + [Phụ thu định dạng] + [Phụ thu ngày lễ] = [Giá vé cuối cùng]. <br> Bước 6: Nhấn "Lưu bảng giá", hệ thống cập nhật và áp dụng công thức mới đồng bộ cho cả kênh đặt vé online và quầy POS. <td colspan=3/> |
| Lưu ý | - Công thức tính giá vé máy chủ đảm bảo tính nhất quán tuyệt đối giữa kênh bán trực tuyến (Online) và kênh bán tại quầy (POS). <br> - Công cụ mô phỏng chạy thử nghiệm độc lập không làm thay đổi hay ghi dữ liệu giao dịch vào hệ thống. <td colspan=3/> |

Bảng A.9: Use case thiết lập quy tắc bảng giá vé.

---

| Mã Use case | UC-10 | Tên Use Case | Quản lý Thực đơn F&B và Khuyến mãi |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Quản lý kho thực đơn ẩm thực bắp nước (món lẻ, combo, nhóm tùy chọn vị và phụ thu) và triển khai các chương trình khuyến mãi (khởi tạo voucher, thiết lập điều kiện áp dụng, phát mã quà tặng trực tiếp và gửi email chiến dịch hàng loạt). <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên thêm mới sản phẩm ẩm thực (Combo, Bắp rang, Nước uống, Snack), nhập đơn giá, tải ảnh minh họa và thiết lập trạng thái kinh doanh. <br> Bước 2: Khởi tạo các nhóm tùy chọn (Vị bắp, Loại nước) và gán các ô lựa chọn linh hoạt vào từng combo kèm mức giá phụ thu thêm. <br> Bước 3: Khởi tạo chương trình Khuyến mãi: Thiết lập Mã code (ví dụ: SUMMER2026), Tên chương trình, Mức giảm giá (% hoặc Tiền mặt), Giảm tối đa, Đơn hàng tối thiểu, Số vé tối đa, Ngày bắt đầu/kết thúc và cờ cho phép đổi bằng điểm thưởng. <br> Bước 4: Quản trị viên có thể phát mã voucher trực tiếp cho một khách hàng cụ thể hoặc nhấn "Gửi email chiến dịch" để hệ thống tự động gửi thư quảng bá kèm voucher đến toàn bộ khách hàng đủ điều kiện. <td colspan=3/> |
| Lưu ý | - Mã khuyến mãi đang trong thời gian chạy sẽ được khóa ngày bắt đầu để đảm bảo tính nhất quán của dữ liệu giao dịch. <br> - Hệ thống tự động loại trừ các khách hàng đã nhận email mã này trước đó khi thực hiện gửi chiến dịch hàng loạt. <td colspan=3/> |

Bảng A.10: Use case quản lý thực đơn f&b và khuyến mãi.

---

| Mã Use case | UC-11 | Tên Use Case | Quản lý Hóa đơn, Nhân sự và Phân quyền |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Quản lý luồng giao dịch hóa đơn bán vé toàn hệ thống, quản lý tài khoản nhân viên phân bổ theo cụm rạp, thiết lập ma trận phân quyền chi tiết (RBAC), kiểm duyệt đánh giá/hỗ trợ khách hàng và giám sát nhật ký kiểm toán hệ thống (Audit Logs). <td colspan=3/> |
| Luồng chạy | Bước 1 (Quản lý Hóa đơn): Quản trị viên tra cứu, tìm kiếm và lọc danh sách đơn đặt vé theo trạng thái, kênh bán, phương thức thanh toán; xem chi tiết vé, bắp nước và mã tham chiếu đối soát ngân hàng. <br> Bước 2 (Quản lý Nhân sự): Thêm tài khoản nhân viên mới, gán vai trò (ADMIN, MANAGER, STAFF), phân bổ cụm rạp làm việc và kích hoạt cờ yêu cầu đổi mật khẩu lần đầu. <br> Bước 3 (Thiết lập Phân quyền): Cấu hình bảng ma trận phân quyền (RBAC) chi tiết từng hành động Xem, Thêm, Sửa, Xóa trên từng phân hệ chức năng cho từng vai trò người dùng. <br> Bước 4 (Vận hành & Phê duyệt): Xem xét và duyệt/từ chối các yêu cầu hủy đơn hàng bắp nước từ quầy; tiếp nhận và phản hồi email phiếu hỗ trợ khách hàng; kiểm duyệt ẩn/xóa bình luận phim vi phạm. <br> Bước 5 (Nhật ký hệ thống): Xem bảng nhật ký ghi vết tự động (Audit Logs) mọi hành động đăng nhập, thêm, sửa, xóa dữ liệu của người dùng trên toàn hệ thống kèm địa chỉ IP và mốc thời gian thực. <td colspan=3/> |
| Lưu ý | - Nhân viên bị giới hạn phạm vi thao tác nghiêm ngặt theo cụm rạp trực thuộc (Cinema Scoping); thao tác chéo rạp sẽ bị từ chối truy cập. <br> - Nhật ký kiểm toán hệ thống là dữ liệu chỉ đọc (Read-only), không một tài khoản nào có quyền chỉnh sửa hoặc xóa dữ liệu nhật ký. <br> - Vai trò Quản trị viên tối cao (Admin) luôn mặc định sở hữu toàn bộ các quyền hạn và không thể bị vô hiệu hóa. <td colspan=3/> |

Bảng A.11: Use case quản lý hóa đơn, nhân sự và phân quyền.

---

