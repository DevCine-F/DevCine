# PHỤ LỤC A: ĐẶC TẢ USE CASE HỆ THỐNG DEVCINE

## A.1. ĐẶC TẢ CÁC USE CASE KHỐI KHÁCH HÀNG (CUSTOMER PORTAL)

| Mã Use case | UC-01 | Tên Use Case | Đăng ký tài khoản khách hàng mới |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Cho phép người dùng mới tạo tài khoản thành viên trong hệ thống để tham gia đặt vé trực tuyến, tích lũy điểm thưởng và nhận các ưu đãi thành viên. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng truy cập trang đăng ký tài khoản từ giao diện trang chủ hoặc màn hình đăng nhập. <br> Bước 2: Khách hàng nhập các thông tin bắt buộc gồm họ tên, số điện thoại, địa chỉ email và mật khẩu bảo mật. <br> Bước 3: Khách hàng nhấn nút "Đăng ký". <br> Bước 4: Hệ thống kiểm tra tính hợp lệ của dữ liệu (định dạng email, định dạng số điện thoại, độ mạnh mật khẩu và đảm bảo số điện thoại/email chưa từng được đăng ký trước đó). <br> Bước 5: Hệ thống khởi tạo tài khoản thành viên mới với hạng mặc định là Đồng (Bronze), tự động đăng nhập và chuyển hướng khách hàng về trang chủ. <td colspan=3/> |
| Lưu ý | - Nếu số điện thoại hoặc email đã tồn tại trong hệ thống, hệ thống hiển thị thông báo lỗi và yêu cầu kiểm tra lại. <br> - Mật khẩu phải đáp ứng tiêu chuẩn an toàn từ 8 đến 32 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt. <td colspan=3/> |

Bảng A.1: Use case đăng ký tài khoản khách hàng mới.

---

| Mã Use case | UC-02 | Tên Use Case | Đăng nhập hệ thống |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Xác thực danh tính khách hàng để cấp quyền truy cập các tính năng cá nhân hóa như đặt vé, xem lịch sử mua hàng và quản lý ưu đãi. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng chọn chức năng "Đăng nhập" trên thanh điều hướng. <br> Bước 2: Khách hàng nhập định danh tài khoản (Số điện thoại hoặc Email) và mật khẩu. <br> Bước 3: Khách hàng nhấn nút "Đăng nhập". <br> Bước 4: Hệ thống kiểm tra thông tin tài khoản và xác thực mật khẩu. <br> Bước 5: Hệ thống lưu trạng thái phiên đăng nhập, tải thông tin thành viên (điểm thưởng, hạng thẻ) và chuyển về trang đang thao tác trước đó. <td colspan=3/> |
| Lưu ý | - Nếu thông tin định danh hoặc mật khẩu không chính xác, hệ thống thông báo lỗi và không tiết lộ cụ thể trường thông tin nào sai để đảm bảo an toàn bảo mật. <td colspan=3/> |

Bảng A.2: Use case đăng nhập hệ thống.

---

| Mã Use case | UC-03 | Tên Use Case | Khôi phục mật khẩu qua Email |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Khách hàng |
| Mô tả | Cho phép khách hàng tự khôi phục quyền truy cập vào tài khoản khi bị quên mật khẩu thông qua mã xác minh (OTP) gửi về hộp thư điện tử. <td colspan=3/> |
| Luồng chạy | Bước 1: Tại màn hình đăng nhập, khách hàng chọn "Quên mật khẩu?". <br> Bước 2: Khách hàng nhập địa chỉ email đã đăng ký tài khoản và nhấn "Gửi mã xác nhận". <br> Bước 3: Hệ thống kiểm tra email trong hệ thống, tạo mã xác thực ngẫu nhiên gồm 6 chữ số có hiệu lực giới hạn và gửi email đến khách hàng. <br> Bước 4: Khách hàng kiểm tra hộp thư, nhập mã xác thực vào giao diện. <br> Bước 5: Hệ thống xác thực mã thành công và cho phép khách hàng nhập mật khẩu mới cùng xác nhận mật khẩu. <br> Bước 6: Khách hàng nhấn "Đặt lại mật khẩu", hệ thống lưu mật khẩu mới và thông báo thành công. <td colspan=3/> |
| Lưu ý | - Mã xác thực chỉ có hiệu lực trong thời gian ngắn và chỉ được sử dụng một lần duy nhất. <br> - Giao diện có cơ chế đếm ngược thời gian chờ gửi lại mã để chống tình trạng gửi thư liên tục. <td colspan=3/> |

Bảng A.3: Use case khôi phục mật khẩu qua email.

---

| Mã Use case | UC-04 | Tên Use Case | Xem và cập nhật thông tin cá nhân |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Khách hàng |
| Mô tả | Cho phép khách hàng xem hồ sơ cá nhân, cấp bậc thành viên, tổng điểm tích lũy và cập nhật thông tin liên lạc khi có nhu cầu thay đổi. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng đăng nhập và chọn mục "Hồ sơ cá nhân" từ menu tài khoản. <br> Bước 2: Hệ thống hiển thị thông tin chi tiết: Họ tên, Email, Số điện thoại, Hạng thành viên hiện tại và Điểm thưởng tích lũy. <br> Bước 3: Khách hàng chỉnh sửa các trường thông tin cần cập nhật (Họ tên, Số điện thoại, Email). <br> Bước 4: Khách hàng nhấn nút "Lưu thay đổi". <br> Bước 5: Hệ thống kiểm tra tính hợp lệ và duy nhất của thông tin mới, lưu vào cơ sở dữ liệu và hiển thị thông báo cập nhật thành công. <td colspan=3/> |
| Lưu ý | - Không cho phép sửa đổi số điện thoại hoặc email trùng lặp với tài khoản của khách hàng khác đang hoạt động. <td colspan=3/> |

Bảng A.4: Use case xem và cập nhật thông tin cá nhân.

---

| Mã Use case | UC-05 | Tên Use Case | Đổi mật khẩu tài khoản |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Khách hàng |
| Mô tả | Cho phép khách hàng chủ động thay đổi mật khẩu đăng nhập định kỳ để nâng cao tính an toàn cho tài khoản cá nhân. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng truy cập trang "Hồ sơ cá nhân" và chọn tab "Đổi mật khẩu". <br> Bước 2: Khách hàng nhập Mật khẩu hiện tại, Mật khẩu mới và Nhập lại mật khẩu mới. <br> Bước 3: Khách hàng nhấn nút "Cập nhật mật khẩu". <br> Bước 4: Hệ thống kiểm tra tính chính xác của mật khẩu hiện tại, độ mạnh của mật khẩu mới và sự trùng khớp của ô xác nhận mật khẩu. <br> Bước 5: Hệ thống mã hóa và lưu mật khẩu mới, sau đó thông báo đổi mật khẩu thành công. <td colspan=3/> |
| Lưu ý | - Nếu mật khẩu cũ không đúng hoặc mật khẩu mới trùng khớp hoàn toàn với mật khẩu cũ, hệ thống sẽ đưa ra thông báo cảnh báo. <td colspan=3/> |

Bảng A.5: Use case đổi mật khẩu tài khoản.

---

| Mã Use case | UC-06 | Tên Use Case | Xem danh sách phim đang chiếu và sắp chiếu |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Cung cấp danh sách các bộ phim đang được công chiếu tại các rạp và những bộ phim chuẩn bị ra mắt giúp khách hàng dễ dàng nắm bắt thông tin giải trí. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng truy cập vào Trang chủ hoặc mục "Lịch chiếu" trên thanh điều hướng. <br> Bước 2: Khách hàng chuyển đổi giữa các tab "Phim đang chiếu" và "Phim sắp chiếu". <br> Bước 3: Hệ thống hiển thị danh sách phim kèm áp-phích (poster), tên phim, thể loại, thời lượng, phân loại độ tuổi và điểm đánh giá trung bình. <td colspan=3/> |
| Lưu ý | - Chỉ các bộ phim đang ở trạng thái kích hoạt công khai mới được hiển thị trên giao diện người dùng. <td colspan=3/> |

Bảng A.6: Use case xem danh sách phim đang chiếu và sắp chiếu.

---

| Mã Use case | UC-07 | Tên Use Case | Xem thông tin chi tiết phim và xem Video Trailer |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Hiển thị toàn bộ thông tin chi tiết của một bộ phim (nội dung tóm tắt, đạo diễn, diễn viên, ngày khởi chiếu) và cho phép xem video giới thiệu (trailer). <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng nhấn chọn vào một bộ phim từ danh sách phim hoặc trang chủ. <br> Bước 2: Hệ thống hiển thị trang chi tiết phim với đầy đủ thông tin: Áp-phích, Tên phim, Tên gốc, Thể loại, Thời lượng, Giới hạn độ tuổi, Đạo diễn, Diễn viên, Tóm tắt nội dung và các suất chiếu theo từng rạp. <br> Bước 3: Khách hàng nhấn vào nút "Xem Trailer". <br> Bước 4: Hệ thống mở cửa sổ phát video trailer chính thức của phim để khách hàng thưởng thức. <td colspan=3/> |
| Lưu ý | - Hệ thống cảnh báo rõ ràng nhãn kiểm duyệt độ tuổi (như P, K, T13, T16, T18, C) để khách hàng cân nhắc trước khi quyết định đặt vé. <td colspan=3/> |

Bảng A.7: Use case xem thông tin chi tiết phim và xem video trailer.

---

| Mã Use case | UC-08 | Tên Use Case | Tìm kiếm và lọc phim |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Hỗ trợ khách hàng tìm kiếm nhanh bộ phim mong muốn theo từ khóa tên phim hoặc lọc phim theo thể loại, cụm rạp và định dạng chiếu. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng nhập từ khóa tên phim vào thanh tìm kiếm hoặc truy cập trang "Tìm kiếm". <br> Bước 2: Khách hàng có thể chọn thêm các tiêu chí lọc: Thể loại phim (Hành động, Hài, Hoạt hình...), Cụm rạp chiếu, Định dạng (2D, 3D, IMAX). <br> Bước 3: Hệ thống tự động xử lý trì hoãn tìm kiếm (debounce) và trả về danh sách các bộ phim phù hợp với tiêu chí đã chọn. <td colspan=3/> |
| Lưu ý | - Nếu không tìm thấy kết quả phù hợp, hệ thống hiển thị trạng thái danh sách rỗng kèm gợi ý các phim đang được quan tâm nhiều nhất. <td colspan=3/> |

Bảng A.8: Use case tìm kiếm và lọc phim.

---

| Mã Use case | UC-09 | Tên Use Case | Tra cứu Lịch chiếu và chọn Suất chiếu theo Cụm rạp |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Cho phép khách hàng xem lịch chiếu tổng hợp của tất cả các rạp, chọn ngày xem, chọn cụm rạp yêu thích và chọn suất chiếu thuận tiện nhất. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng truy cập trang "Lịch chiếu" hoặc xem phần lịch chiếu tại trang Chi tiết phim. <br> Bước 2: Khách hàng chọn Cụm rạp (theo khu vực/tỉnh thành) và chọn Ngày chiếu mong muốn. <br> Bước 3: Hệ thống lọc và hiển thị danh sách các phim kèm các khung giờ chiếu, phòng chiếu và định dạng tương ứng. <br> Bước 4: Khách hàng chọn một suất chiếu cụ thể để bắt đầu quá trình đặt vé. <td colspan=3/> |
| Lưu ý | - Các suất chiếu đã diễn ra trong quá khứ sẽ tự động được ẩn đi hoặc khóa không cho chọn đặt vé. <td colspan=3/> |

Bảng A.9: Use case tra cứu lịch chiếu và chọn suất chiếu theo cụm rạp.

---

| Mã Use case | UC-10 | Tên Use Case | Chọn vị trí ghế ngồi trên Sơ đồ phòng chiếu |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Hiển thị sơ đồ mặt bằng ghế của phòng chiếu theo thời gian thực, cho phép khách hàng chọn các vị trí ghế ngồi ưng ý và tạm giữ chỗ trong 10 phút. <td colspan=3/> |
| Luồng chạy | Bước 1: Sau khi chọn suất chiếu, hệ thống điều hướng khách hàng đến màn hình Sơ đồ ghế. <br> Bước 2: Hệ thống hiển thị trực quan cấu trúc phòng chiếu gồm Màn hình, Lối đi và Ma trận các loại ghế (Ghế thường, Ghế VIP, Ghế đôi Sweetbox) kèm trạng thái (Còn trống, Đang giữ, Đã bán, Đang bảo trì). <br> Bước 3: Khách hàng nhấn chọn các vị trí ghế mong muốn và chọn loại đối tượng áp dụng cho từng ghế (Người lớn, Học sinh/Sinh viên). <br> Bước 4: Khách hàng nhấn "Tiếp tục". Hệ thống thực hiện khóa giữ chỗ tạm thời các ghế đã chọn trong thời gian 10 phút để khách tiến hành thanh toán. <td colspan=3/> |
| Lưu ý | - Hệ thống kiểm tra quy tắc không để lại ghế trống đơn lẻ (orphan seat) ở giữa hoặc đầu hàng. <br> - Nếu có ghế đã bị người khác chọn trước trong tích tắc, hệ thống sẽ thông báo xung đột và yêu cầu khách hàng chọn vị trí khác. <td colspan=3/> |

Bảng A.10: Use case chọn vị trí ghế ngồi trên sơ đồ phòng chiếu.

---

| Mã Use case | UC-11 | Tên Use Case | Lựa chọn Bắp nước / Combo và tùy biến lựa chọn |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Cho phép khách hàng chọn thêm các món bắp rang, nước ngọt, thức ăn nhẹ hoặc combo ưu đãi kèm theo vé, đồng thời hỗ trợ đổi vị và chọn kích cỡ. <td colspan=3/> |
| Luồng chạy | Bước 1: Tại bước chọn bắp nước của quy trình đặt vé, hệ thống hiển thị danh mục các combo và món ăn đang mở bán kèm hình ảnh, mô tả và giá tiền. <br> Bước 2: Khách hàng tăng/giảm số lượng món hoặc combo muốn mua. <br> Bước 3: Đối với các combo có tùy chọn vị (như Vị bắp: Phô mai, Caramel; Vị nước: Coca, Sprite), hệ thống mở cửa sổ tùy biến để khách hàng lựa chọn khẩu vị yêu thích và tính phụ thu tương ứng (nếu có). <br> Bước 4: Hệ thống tự động cập nhật tổng tiền giỏ hàng bao gồm tiền vé và tiền bắp nước. <td colspan=3/> |
| Lưu ý | - Bước chọn bắp nước là không bắt buộc; khách hàng có thể bấm "Bỏ qua" hoặc "Tiếp tục" để sang bước thanh toán. <td colspan=3/> |

Bảng A.11: Use case lựa chọn bắp nước / combo và tùy biến lựa chọn.

---

| Mã Use case | UC-12 | Tên Use Case | Áp dụng Mã giảm giá / Voucher ưu đãi vào đơn đặt vé |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Hỗ trợ khách hàng nhập mã khuyến mãi hoặc chọn các voucher sẵn có trong ví ưu đãi để được khấu trừ giảm giá trực tiếp trên tổng hóa đơn. <td colspan=3/> |
| Luồng chạy | Bước 1: Tại bước xác nhận thanh toán, khách hàng nhấn chọn danh sách voucher khả dụng từ ví cá nhân hoặc nhập mã code khuyến mãi vào ô nhập liệu. <br> Bước 2: Khách hàng nhấn nút "Áp dụng". <br> Bước 3: Hệ thống kiểm tra điều kiện áp dụng của mã: Thời hạn sử dụng, giá trị đơn hàng tối thiểu, giới hạn số vé, phim áp dụng và đối tượng khách hàng. <br> Bước 4: Nếu thỏa mãn điều kiện, hệ thống tính toán số tiền được giảm trừ và cập nhật lại số tiền thanh toán thực tế của đơn hàng. <td colspan=3/> |
| Lưu ý | - Mỗi đơn hàng chỉ được áp dụng một mã giảm giá trừ khi chương trình có quy định cho phép cộng dồn ưu đãi. <br> - Nếu mã giảm giá không đủ điều kiện áp dụng, hệ thống hiển thị lý do cụ thể (chưa đủ tiền tối thiểu, hết hạn...). <td colspan=3/> |

Bảng A.12: Use case áp dụng mã giảm giá / voucher ưu đãi vào đơn đặt vé.

---

| Mã Use case | UC-13 | Tên Use Case | Thanh toán vé trực tuyến qua Cổng thanh toán VNPAY |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Kết nối cổng thanh toán trực tuyến an toàn VNPAY cho phép khách hàng thanh toán qua ứng dụng ngân hàng, ví điện tử hoặc thẻ nội địa/quốc tế. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng kiểm tra lại tóm tắt đơn đặt vé (Phim, Suất chiếu, Ghế ngồi, Combo, Tổng tiền) và chọn phương thức thanh toán VNPAY. <br> Bước 2: Khách hàng nhấn nút "Thanh toán". <br> Bước 3: Hệ thống tạo giao dịch và chuyển hướng khách hàng sang cổng thanh toán bảo mật VNPAY. <br> Bước 4: Khách hàng thực hiện quét mã QR qua ứng dụng ngân hàng hoặc nhập thông tin thẻ để xác nhận thanh toán. <br> Bước 5: Sau khi thanh toán thành công, VNPAY chuyển hướng phản hồi về hệ thống DevCine. <br> Bước 6: Hệ thống xác thực chữ ký bảo mật giao dịch, chuyển trạng thái đơn hàng sang Đã xác nhận (Confirmed), hoàn tất đặt chỗ và cộng điểm thưởng thành viên. <td colspan=3/> |
| Lưu ý | - Nếu khách hàng hủy thanh toán hoặc giao dịch thất bại quá thời gian giữ chỗ 10 phút, hệ thống sẽ tự động giải phóng các ghế đã chọn về trạng thái trống. <td colspan=3/> |

Bảng A.13: Use case thanh toán vé trực tuyến qua cổng thanh toán vnpay.

---

| Mã Use case | UC-14 | Tên Use Case | Xem Vé điện tử (Mã đặt vé & Mã QR Code) sau khi thanh toán |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Khách hàng |
| Mô tả | Cung cấp vé điện tử trực quan chứa thông tin suất chiếu và mã vạch QR Code giúp khách hàng sử dụng để check-in hoặc in vé giấy khi đến rạp. <td colspan=3/> |
| Luồng chạy | Bước 1: Khi thanh toán thành công, hệ thống điều hướng khách hàng đến màn hình "Đặt vé thành công". <br> Bước 2: Hệ thống hiển thị toàn bộ thông tin vé điện tử: Mã đặt vé, Tên phim, Rạp chiếu, Phòng chiếu, Thời gian chiếu, Vị trí ghế, Danh sách bắp nước và Mã QR Code xác thực. <br> Bước 3: Hệ thống đồng thời gửi email thông báo xác nhận kèm hóa đơn chi tiết đến địa chỉ email của khách hàng. <td colspan=3/> |
| Lưu ý | - Khách hàng có thể chụp màn hình hoặc lưu mã QR để xuất trình trực tiếp cho nhân viên soát vé tại rạp chiếu phim. <td colspan=3/> |

Bảng A.14: Use case xem vé điện tử (mã đặt vé & mã qr code) sau khi thanh toán.

---

| Mã Use case | UC-15 | Tên Use Case | Tra cứu Lịch sử đặt vé và trạng thái vé |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Khách hàng |
| Mô tả | Cho phép khách hàng xem lại toàn bộ danh sách các đơn hàng và vé xem phim đã mua trong quá khứ hoặc sắp tới kèm thông tin chi tiết từng vé. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng đăng nhập và chọn mục "Lịch sử đặt vé" trong hồ sơ cá nhân. <br> Bước 2: Hệ thống truy vấn và hiển thị danh sách các đơn đặt vé theo thứ tự thời gian từ mới nhất đến cũ nhất. <br> Bước 3: Khách hàng nhấn chọn vào một đơn đặt vé bất kỳ để xem chi tiết: Mã đặt vé, Tên phim, Suất chiếu, Ghế ngồi, Bắp nước, Tổng tiền thanh toán, Trạng thái (Đã thanh toán, Đã in vé/Check-in, Đã hủy) và Mã QR vé. <td colspan=3/> |
| Lưu ý | - Dữ liệu lịch sử được tối ưu hóa để tải nhanh chóng và bảo toàn đầy đủ thông tin kể cả khi giá vé hoặc thực đơn có sự thay đổi sau này. <td colspan=3/> |

Bảng A.15: Use case tra cứu lịch sử đặt vé và trạng thái vé.

---

| Mã Use case | UC-16 | Tên Use Case | Đánh giá và chấm điểm phim |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Khách hàng |
| Mô tả | Cho phép những khách hàng đã thực sự mua vé và xem phim gửi đánh giá chấm điểm sao cùng cảm nhận bình luận về bộ phim để chia sẻ với cộng đồng. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng truy cập trang chi tiết của bộ phim đã từng xem. <br> Bước 2: Hệ thống kiểm tra điều kiện (khách hàng đã đăng nhập và có đơn đặt vé thành công bộ phim này). <br> Bước 3: Khách hàng chọn số sao đánh giá (từ 1 đến 5 sao) và nhập nội dung nhận xét cảm nghĩ. <br> Bước 4: Khách hàng nhấn nút "Gửi đánh giá". <br> Bước 5: Hệ thống ghi nhận đánh giá, tính toán lại điểm đánh giá trung bình của phim và hiển thị nhận xét trên trang phim. <td colspan=3/> |
| Lưu ý | - Khách hàng chưa mua vé bộ phim này sẽ không được cấp quyền gửi đánh giá nhằm ngăn ngừa tình trạng đánh giá ảo hoặc tiêu cực vô căn cứ. <td colspan=3/> |

Bảng A.16: Use case đánh giá và chấm điểm phim.

---

| Mã Use case | UC-17 | Tên Use Case | Quản lý Ví Voucher cá nhân |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Khách hàng |
| Mô tả | Cung cấp giao diện ví voucher cá nhân để khách hàng tra cứu mã khuyến mãi, lưu mã ưu đãi vào tài khoản và theo dõi hạn sử dụng của các voucher đang sở hữu. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng truy cập mục "Ưu đãi của tôi" trong trang cá nhân. <br> Bước 2: Hệ thống hiển thị các tab phân loại: "Voucher khả dụng", "Đổi điểm lấy ưu đãi" và "Lịch sử voucher (Đã dùng / Hết hạn)". <br> Bước 3: Khách hàng có thể nhập mã code bí mật được nhận từ các chiến dịch quảng cáo vào ô tra cứu và nhấn "Lưu mã". <br> Bước 4: Hệ thống kiểm tra tính hợp lệ của mã và thêm voucher vào ví cá nhân của khách hàng. <td colspan=3/> |
| Lưu ý | - Nếu mã ưu đãi đã hết lượt sử dụng, hết hạn hoặc khách hàng đã lưu trước đó, hệ thống sẽ đưa ra thông báo phù hợp. <td colspan=3/> |

Bảng A.17: Use case quản lý ví voucher cá nhân.

---

| Mã Use case | UC-18 | Tên Use Case | Quy đổi Điểm tích lũy thành viên lấy Voucher giảm giá |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Khách hàng |
| Mô tả | Cho phép khách hàng sử dụng điểm thưởng tích lũy (Loyalty Points) có được từ các lần mua vé trước đó để đổi lấy các phiếu giảm giá có giá trị cao. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng truy cập tab "Đổi điểm lấy ưu đãi" tại trang Ví Voucher. <br> Bước 2: Hệ thống hiển thị danh sách các chương trình khuyến mãi cho phép đổi điểm kèm số điểm yêu cầu tương ứng (ví dụ: 100 điểm, 200 điểm) và số điểm hiện có của khách. <br> Bước 3: Khách hàng nhấn nút "Đổi ngay" tại gói ưu đãi mong muốn. <br> Bước 4: Hệ thống kiểm tra số điểm tích lũy hiện tại của khách hàng có đủ điều kiện không. <br> Bước 5: Hệ thống trừ số điểm tương ứng trong tài khoản thành viên, sinh voucher mới vào ví của khách hàng và thông báo đổi điểm thành công. <td colspan=3/> |
| Lưu ý | - Mỗi gói ưu đãi quy đổi chỉ có thể đổi một lần cho mỗi tài khoản thành viên theo chính sách của từng chương trình. <td colspan=3/> |

Bảng A.18: Use case quy đổi điểm tích lũy thành viên lấy voucher giảm giá.

---

| Mã Use case | UC-19 | Tên Use Case | Gửi Yêu cầu hỗ trợ / Liên hệ trực tuyến |
|---|---|---|---|
| Độ ưu tiên | Thấp | Tác nhân | Khách hàng |
| Mô tả | Cung cấp biểu mẫu liên hệ trực tuyến để khách hàng gửi phản ánh, thắc mắc về dịch vụ, sự cố vé hoặc góp ý đến ban quản trị cụm rạp. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng truy cập trang "Liên hệ / Hỗ trợ". <br> Bước 2: Khách hàng chọn nhóm chủ đề liên hệ (Vấn đề về vé, Thẻ thành viên, Góp ý dịch vụ, Hợp tác quảng cáo). <br> Bước 3: Khách hàng nhập số điện thoại và nội dung chi tiết cần hỗ trợ. <br> Bước 4: Khách hàng nhấn nút "Gửi yêu cầu hỗ trợ". <br> Bước 5: Hệ thống tiếp nhận, tạo phiếu yêu cầu hỗ trợ (Support Ticket) ở trạng thái chờ xử lý và thông báo gửi yêu cầu thành công đến khách hàng. <td colspan=3/> |
| Lưu ý | - Khách hàng cần đăng nhập tài khoản để hệ thống tự động liên kết yêu cầu hỗ trợ với hồ sơ khách hàng nhằm tiện cho việc phản hồi. <td colspan=3/> |

Bảng A.19: Use case gửi yêu cầu hỗ trợ / liên hệ trực tuyến.

---

| Mã Use case | UC-20 | Tên Use Case | Xem và tra cứu Danh mục Câu hỏi thường gặp (FAQ) |
|---|---|---|---|
| Độ ưu tiên | Thấp | Tác nhân | Khách hàng |
| Mô tả | Cung cấp danh sách các câu hỏi thường gặp và câu trả lời chuẩn xác được sắp xếp theo từng chủ đề giúp khách hàng tự tra cứu thông tin nhanh chóng. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng truy cập trang "Hỏi đáp (FAQ)". <br> Bước 2: Hệ thống hiển thị các câu hỏi được phân nhóm theo danh mục (Đặt vé online, Quy định độ tuổi, Thẻ thành viên, Giá vé & Ưu đãi). <br> Bước 3: Khách hàng có thể nhập từ khóa vào ô tìm kiếm nhanh. <br> Bước 4: Khách hàng nhấn vào tiêu đề câu hỏi để mở rộng nội dung câu trả lời chi tiết. <td colspan=3/> |
| Lưu ý | - Giao diện hỗ trợ mở/đóng mượt mà giúp người dùng dễ dàng theo dõi nhiều nội dung mà không bị rối mắt. <td colspan=3/> |

Bảng A.20: Use case xem và tra cứu danh mục câu hỏi thường gặp (faq).

---

| Mã Use case | UC-21 | Tên Use Case | Xem Tin tức, Bài viết sự kiện và Chương trình khuyến mãi |
|---|---|---|---|
| Độ ưu tiên | Thấp | Tác nhân | Khách hàng |
| Mô tả | Giúp khách hàng theo dõi các bài viết tin tức điện ảnh, sự kiện ra mắt phim và các bài viết giới thiệu chương trình ưu đãi hấp dẫn đang diễn ra. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng truy cập trang "Khuyến mãi" hoặc xem mục tin tức trên trang chủ. <br> Bước 2: Hệ thống hiển thị danh sách các bài viết tin tức sự kiện kèm hình ảnh đại diện, tiêu đề và tóm tắt ngắn. <br> Bước 3: Khách hàng nhấn chọn một bài viết cụ thể. <br> Bước 4: Hệ thống hiển thị trang nội dung bài viết chi tiết cùng thời gian áp dụng của sự kiện. <td colspan=3/> |
| Lưu ý | - Chỉ các bài viết đang ở trạng thái kích hoạt và còn trong thời gian hiệu lực mới được hiển thị công khai. <td colspan=3/> |

Bảng A.21: Use case xem tin tức, bài viết sự kiện và chương trình khuyến mãi.

---

| Mã Use case | UC-22 | Tên Use Case | Nhận và quản lý Thông báo cá nhân trên hệ thống |
|---|---|---|---|
| Độ ưu tiên | Thấp | Tác nhân | Khách hàng |
| Mô tả | Hộp thư thông báo nội bộ giúp khách hàng nhận các cập nhật quan trọng như: xác nhận đặt vé thành công, nhận voucher quà tặng, thăng hạng thành viên. <td colspan=3/> |
| Luồng chạy | Bước 1: Khách hàng đăng nhập và nhấn vào biểu tượng Chuông thông báo trên thanh điều hướng. <br> Bước 2: Hệ thống hiển thị số lượng thông báo chưa đọc cùng danh sách các thông báo mới nhất. <br> Bước 3: Khách hàng nhấn vào một thông báo để xem nội dung chi tiết; hệ thống tự động chuyển trạng thái thông báo đó sang "Đã đọc". <br> Bước 4: Khách hàng có thể nhấn nút "Đánh dấu tất cả đã đọc" để xóa huy hiệu số tin chưa đọc. <td colspan=3/> |
| Lưu ý | - Hệ thống đảm bảo tính bảo mật, người dùng chỉ có thể xem và thao tác trên thông báo thuộc về chính tài khoản của mình. <td colspan=3/> |

Bảng A.22: Use case nhận và quản lý thông báo cá nhân trên hệ thống.

---

## A.2. ĐẶC TẢ CÁC USE CASE KHỐI NHÂN VIÊN QUẦY & SOÁT VÉ (STAFF / POS / CHECK-IN)

| Mã Use case | UC-23 | Tên Use Case | Đăng nhập hệ thống Bán vé & Vận hành tại quầy (POS) |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Xác thực danh tính và quyền hạn của nhân viên để truy cập vào màn hình bán vé tại quầy (POS) và màn hình soát vé theo cụm rạp được phân công. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên truy cập cổng đăng nhập nội bộ dành cho nhân sự. <br> Bước 2: Nhân viên nhập tên đăng nhập và mật khẩu được cấp. <br> Bước 3: Nhân viên nhấn nút "Đăng nhập". <br> Bước 4: Hệ thống xác thực thông tin, kiểm tra vai trò và quyền hạn thao tác tại quầy, đồng thời tải phạm vi cụm rạp (Cinema Scoping) mà nhân viên trực thuộc. <br> Bước 5: Hệ thống điều hướng nhân viên vào màn hình Bán vé POS hoặc màn hình Soát vé tương ứng. <td colspan=3/> |
| Lưu ý | - Nhân viên chỉ có quyền thao tác trên dữ liệu và suất chiếu thuộc cụm rạp mà mình được phân bổ công tác; mọi hành vi thao tác chéo rạp đều bị chặn. <td colspan=3/> |

Bảng A.23: Use case đăng nhập hệ thống bán vé & vận hành tại quầy (pos).

---

| Mã Use case | UC-24 | Tên Use Case | Đổi mật khẩu bắt buộc trong lần đầu tiên đăng nhập |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Cơ chế bảo mật bắt buộc nhân viên mới phải thay đổi mật khẩu mặc định do quản trị viên cấp ngay trong lần đăng nhập đầu tiên trước khi sử dụng hệ thống. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên đăng nhập thành công với tài khoản được quản trị viên khởi tạo lần đầu. <br> Bước 2: Hệ thống nhận diện cờ yêu cầu đổi mật khẩu lần đầu và tự động chuyển hướng khóa vào màn hình "Đổi mật khẩu lần đầu". <br> Bước 3: Nhân viên nhập Mật khẩu mới và Nhập lại mật khẩu mới đáp ứng tiêu chuẩn an toàn. <br> Bước 4: Nhân viên nhấn nút "Xác nhận đổi mật khẩu". <br> Bước 5: Hệ thống cập nhật mật khẩu mới, gỡ bỏ cờ bắt buộc và mở khóa toàn bộ quyền hạn để nhân viên bắt đầu làm việc. <td colspan=3/> |
| Lưu ý | - Nhân viên không thể truy cập bất kỳ tính năng bán vé hay quản trị nào khác nếu chưa hoàn tất bước đổi mật khẩu lần đầu này. <td colspan=3/> |

Bảng A.24: Use case đổi mật khẩu bắt buộc trong lần đầu tiên đăng nhập.

---

| Mã Use case | UC-25 | Tên Use Case | Tra cứu lịch chiếu và tình trạng suất chiếu nhanh tại quầy |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Cung cấp giao diện trực quan hiển thị danh sách các phim, phòng chiếu và suất chiếu trong ngày tại rạp giúp nhân viên tư vấn nhanh cho khách hàng. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên mở màn hình Bán vé POS. <br> Bước 2: Hệ thống tự động tải và hiển thị danh sách tất cả các suất chiếu từ ngày hôm nay trở đi của cụm rạp hiện tại. <br> Bước 3: Nhân viên chọn nhanh ngày chiếu qua các tab (Hôm nay, Ngày mai...) hoặc chọn phim cụ thể. <br> Bước 4: Hệ thống gom nhóm hiển thị theo từng bộ phim, kèm định dạng chiếu (2D, 3D), phòng chiếu và thời gian bắt đầu chiếu. <td colspan=3/> |
| Lưu ý | - Các suất chiếu đã bắt đầu quá thời gian cho phép bán vé trễ (mặc định 30 phút sau giờ chiếu) sẽ tự động được ẩn khỏi màn hình bán vé. <td colspan=3/> |

Bảng A.25: Use case tra cứu lịch chiếu và tình trạng suất chiếu nhanh tại quầy.

---

| Mã Use case | UC-26 | Tên Use Case | Bán vé xem phim cho khách vãng lai |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Hỗ trợ nhân viên chọn suất chiếu, chọn vị trí ghế ngồi trên sơ đồ, chọn loại vé phù hợp cho khách mua trực tiếp tại quầy không cần tài khoản. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên chọn một suất chiếu theo yêu cầu của khách hàng. <br> Bước 2: Hệ thống hiển thị sơ đồ ghế của phòng chiếu theo thời gian thực. <br> Bước 3: Nhân viên nhấn chọn các vị trí ghế ngồi mà khách hàng yêu cầu. <br> Bước 4: Nhân viên chỉ định loại đối tượng cho từng ghế (Người lớn, Học sinh/Sinh viên, Trẻ em, Người cao tuổi) để hệ thống áp dụng mức giá tương ứng. <br> Bước 5: Nhân viên xác nhận thông tin và chuyển sang bước tiếp theo. <td colspan=3/> |
| Lưu ý | - Nhân viên có quyền đặc biệt cho phép bỏ qua quy tắc kiểm tra ghế trống đơn lẻ nếu khách hàng có yêu cầu đặc thù và được quản lý chấp thuận. <td colspan=3/> |

Bảng A.26: Use case bán vé xem phim cho khách vãng lai.

---

| Mã Use case | UC-27 | Tên Use Case | Bán kèm hoặc Bán nhanh Bắp nước độc lập tại quầy |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Hỗ trợ nhân viên bán bắp rang, nước uống kèm theo đơn mua vé hoặc bán nhanh các sản phẩm bắp nước cho khách vãng lai chỉ có nhu cầu mua ẩm thực. <td colspan=3/> |
| Luồng chạy | Bước 1: Tại màn hình POS, nhân viên có thể chọn thêm combo bắp nước vào đơn vé hoặc chọn chế độ "Bán nhanh Bắp nước (Concession Only)". <br> Bước 2: Hệ thống hiển thị danh mục thực đơn F&B phân loại theo Combo, Bắp rang, Nước uống, Đồ ăn vặt. <br> Bước 3: Nhân viên chọn món, số lượng và tùy chọn vị bắp/loại nước theo yêu cầu của khách hàng. <br> Bước 4: Hệ thống tự động tính toán tổng tiền hàng và đưa vào giỏ thanh toán. <td colspan=3/> |
| Lưu ý | - Đơn bán bắp nước độc lập không gắn liền với suất chiếu hay ghế ngồi, cho phép thanh toán và xuất hóa đơn ngay tức thì. <td colspan=3/> |

Bảng A.27: Use case bán kèm hoặc bán nhanh bắp nước độc lập tại quầy.

---

| Mã Use case | UC-28 | Tên Use Case | Tra cứu và định danh Khách hàng thành viên qua Số điện thoại để tích điểm |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Cho phép nhân viên tra cứu nhanh thông tin thành viên bằng số điện thoại để áp dụng ưu đãi theo hạng thẻ và tích lũy điểm thưởng khi mua hàng tại quầy. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên hỏi số điện thoại của khách hàng và nhập vào ô "Tra cứu thành viên" tại màn hình POS. <br> Bước 2: Nhân viên nhấn nút "Tra cứu" hoặc phím Enter. <br> Bước 3: Hệ thống tìm kiếm và hiển thị thông tin thành viên: Họ tên khách hàng, Hạng thẻ (Đồng, Bạc, Vàng, Bạch Kim) và Điểm thưởng hiện có. <br> Bước 4: Hệ thống liên kết tài khoản thành viên vào đơn hàng hiện tại để tích điểm sau khi giao dịch hoàn tất. <td colspan=3/> |
| Lưu ý | - Nếu số điện thoại chưa được đăng ký trong hệ thống, nhân viên có thể tiến hành bán vé dưới dạng khách vãng lai thông thường. <td colspan=3/> |

Bảng A.28: Use case tra cứu và định danh khách hàng thành viên qua số điện thoại để tích điểm.

---

| Mã Use case | UC-29 | Tên Use Case | Áp dụng Mã giảm giá / Voucher tại quầy cho khách thành viên |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Hỗ trợ áp dụng các phiếu giảm giá, voucher quà tặng hoặc mã ưu đãi từ chương trình khuyến mãi cho khách hàng khi thanh toán tại quầy vé. <td colspan=3/> |
| Luồng chạy | Bước 1: Sau khi đã định danh khách hàng thành viên, nhân viên mở danh sách voucher mà khách đang sở hữu hoặc nhập mã ưu đãi trực tiếp. <br> Bước 2: Nhân viên chọn voucher phù hợp và nhấn "Áp dụng". <br> Bước 3: Hệ thống kiểm tra điều kiện áp dụng và tự động khấu trừ số tiền giảm giá vào tổng giá trị đơn hàng. <td colspan=3/> |
| Lưu ý | - Chỉ các voucher hợp lệ, chưa sử dụng và còn trong thời hạn hiệu lực mới được chấp nhận áp dụng. <td colspan=3/> |

Bảng A.29: Use case áp dụng mã giảm giá / voucher tại quầy cho khách thành viên.

---

| Mã Use case | UC-30 | Tên Use Case | Thanh toán bằng Tiền mặt và tính tiền thừa trả khách |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Xử lý giao dịch thanh toán bằng tiền mặt tại quầy, tự động tính số tiền thừa cần trả lại khách và hỗ trợ quy tắc làm tròn tiền lẻ. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên chọn phương thức thanh toán là "Tiền mặt (CASH)". <br> Bước 2: Hệ thống hiển thị tổng số tiền cần thu. Đối với khách vãng lai, hệ thống tự động làm tròn số tiền về bội số 1.000 đồng gần nhất. <br> Bước 3: Nhân viên nhập số tiền mặt khách hàng đưa. <br> Bước 4: Hệ thống tự động tính toán và hiển thị số tiền thừa (tiền thối) cần trả lại cho khách. <br> Bước 5: Nhân viên thu tiền, trả lại tiền thừa cho khách và nhấn nút "Hoàn tất thanh toán". <td colspan=3/> |
| Lưu ý | - Số tiền làm tròn được lưu vết chuẩn xác để đảm bảo quá trình đối soát doanh thu cuối ca làm việc khớp hoàn toàn với số tiền thực tế trong két. <td colspan=3/> |

Bảng A.30: Use case thanh toán bằng tiền mặt và tính tiền thừa trả khách.

---

| Mã Use case | UC-31 | Tên Use Case | Thanh toán Chuyển khoản ngân hàng qua Mã QR tự động (VietQR) |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Sinh mã phản hồi nhanh VietQR động chứa chính xác số tài khoản, số tiền và nội dung đơn hàng giúp khách quét mã chuyển khoản nhanh tại quầy. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên chọn phương thức thanh toán là "Chuyển khoản (TRANSFER)". <br> Bước 2: Hệ thống tự động tạo mã VietQR động hiển thị trên màn hình phụ hướng về phía khách hàng. <br> Bước 3: Khách hàng sử dụng ứng dụng ngân hàng trên điện thoại quét mã QR để thực hiện chuyển khoản. <br> Bước 4: Sau khi nhân viên xác nhận tài khoản rạp đã nhận được tiền, nhân viên nhấn nút "Xác nhận đã nhận tiền". <br> Bước 5: Hệ thống chuyển trạng thái đơn hàng sang Đã xác nhận và tiến hành xuất vé. <td colspan=3/> |
| Lưu ý | - Mã QR động chứa sẵn nội dung chuyển khoản là mã đơn hàng để hỗ trợ đối soát giao dịch ngân hàng chính xác. <td colspan=3/> |

Bảng A.31: Use case thanh toán chuyển khoản ngân hàng qua mã qr tự động (vietqr).

---

| Mã Use case | UC-32 | Tên Use Case | In vé giấy và Biên lai hóa đơn thanh toán tại quầy |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Tự động xuất lệnh in vé xem phim dạng giấy nhiệt và biên lai hóa đơn bán hàng cho khách hàng ngay sau khi thanh toán thành công tại quầy. <td colspan=3/> |
| Luồng chạy | Bước 1: Khi đơn hàng được thanh toán thành công, hệ thống mở cửa sổ mẫu in vé chuẩn. <br> Bước 2: Mẫu in bao gồm đầy đủ thông tin: Tên cụm rạp, Tên phim, Phòng chiếu, Định dạng, Ngày giờ chiếu, Vị trí ghế, Chi tiết bắp nước, Tổng tiền thanh toán và Mã QR xác thực của đơn vé. <br> Bước 3: Máy in nhiệt tại quầy tự động in vé giấy và biên lai để nhân viên trao cho khách hàng. <td colspan=3/> |
| Lưu ý | - Hệ thống cho phép cấu hình in tự động hoặc nhấn nút in thủ công theo thiết lập của từng quầy bán vé. <td colspan=3/> |

Bảng A.32: Use case in vé giấy và biên lai hóa đơn thanh toán tại quầy.

---

| Mã Use case | UC-33 | Tên Use Case | Lưu tạm đơn hàng và phục hồi giỏ hàng chờ tại quầy |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Cho phép nhân viên lưu tạm một đơn hàng đang chọn dở dang khi khách hàng cần thêm thời gian lựa chọn, để chuyển sang phục vụ khách tiếp theo mà không làm mất trạng thái ghế. <td colspan=3/> |
| Luồng chạy | Bước 1: Khi khách hàng cần chờ bạn hoặc chưa quyết định xong, nhân viên nhấn nút "Lưu tạm đơn hàng". <br> Bước 2: Hệ thống tạo mã đơn hàng tạm, giữ ghế trên hệ thống và chuyển giao diện POS về trạng thái sẵn sàng đón khách mới. <br> Bước 3: Khi khách hàng quay lại, nhân viên mở danh sách "Đơn hàng chờ". <br> Bước 4: Nhân viên chọn đơn hàng tương ứng và nhấn "Mở lại đơn". <br> Bước 5: Hệ thống tải lại toàn bộ ghế ngồi và bắp nước đã chọn trước đó để tiếp tục thực hiện thanh toán. <td colspan=3/> |
| Lưu ý | - Đơn hàng lưu tạm có thời gian hết hạn tự động; nếu quá hạn mà chưa thanh toán, hệ thống sẽ tự động hủy đơn và giải phóng ghế. <td colspan=3/> |

Bảng A.33: Use case lưu tạm đơn hàng và phục hồi giỏ hàng chờ tại quầy.

---

| Mã Use case | UC-34 | Tên Use Case | Soát vé tự động bằng Camera quét mã QR |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Sử dụng camera hoặc đầu đọc mã vạch để quét mã QR trên vé điện tử của khách hàng nhằm kiểm tra tính hợp lệ và check-in vào phòng chiếu. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên soát vé mở màn hình "Soát vé (Check-in)" và kích hoạt Camera quét mã. <br> Bước 2: Khách hàng xuất trình mã QR trên điện thoại hoặc vé giấy trước ống kính camera. <br> Bước 3: Hệ thống nhận diện mã QR, phát âm thanh báo hiệu và hiển thị thông tin vé: Tên phim, Phòng chiếu, Suất chiếu, Vị trí ghế ngồi và Cảnh báo kiểm tra thẻ sinh viên/độ tuổi (nếu có). <br> Bước 4: Nhân viên kiểm tra khách hàng thực tế và nhấn "Xác nhận vào phòng / In vé". <br> Bước 5: Hệ thống cập nhật trạng thái vé thành "Đã soát vé / Đã in vé" và ghi nhận thời gian cùng nhân viên thực hiện. <td colspan=3/> |
| Lưu ý | - Nếu mã QR không hợp lệ, đã bị hủy hoặc vé đã được quét check-in trước đó, hệ thống sẽ lập tức phát âm thanh cảnh báo và hiển thị thông báo lỗi chống quét trùng lặp. <td colspan=3/> |

Bảng A.34: Use case soát vé tự động bằng camera quét mã qr.

---

| Mã Use case | UC-35 | Tên Use Case | Soát vé và xác minh đơn thủ công bằng Mã đặt vé |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Hỗ trợ nhân viên nhập mã ký tự đặt vé bằng tay trong trường hợp điện thoại của khách hàng bị hỏng camera, mờ mã QR hoặc lỗi thiết bị quét. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên chuyển sang tab "Nhập mã thủ công" tại màn hình Soát vé. <br> Bước 2: Nhân viên hỏi mã đặt vé (ví dụ: BK123456) của khách và nhập vào ô tìm kiếm. <br> Bước 3: Nhân viên nhấn nút "Tra cứu & Kiểm tra". <br> Bước 4: Hệ thống tìm kiếm và hiển thị chi tiết thông tin đơn vé. <br> Bước 5: Nhân viên đối chiếu và xác nhận check-in đơn vé cho khách vào phòng chiếu. <td colspan=3/> |
| Lưu ý | - Chỉ các đơn vé thuộc cụm rạp mà nhân viên đang phụ trách mới có thể thực hiện kiểm tra và check-in thành công. <td colspan=3/> |

Bảng A.35: Use case soát vé và xác minh đơn thủ công bằng mã đặt vé.

---

| Mã Use case | UC-36 | Tên Use Case | Tra cứu hóa đơn và in vé giấy từ mã đặt vé trực tuyến |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Cho phép khách hàng đã mua vé online đến quầy xuất trình mã đặt vé để nhân viên in ra thành vé giấy truyền thống trước khi vào rạp xem phim. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên nhập mã đặt vé online của khách hàng vào hệ thống tại quầy soát vé/in vé. <br> Bước 2: Hệ thống kiểm tra đơn hàng: Đã thanh toán (Confirmed) và Chưa từng in vé giấy. <br> Bước 3: Nhân viên nhấn nút "In vé giấy". <br> Bước 4: Máy in xuất vé giấy, hệ thống cập nhật thời gian in vé và ghi nhận người thực hiện in. <br> Bước 5: Hệ thống đồng thời gửi email thông báo cảm ơn đến khách hàng. <td colspan=3/> |
| Lưu ý | - Hệ thống kiểm soát nghiêm ngặt: Mỗi đơn đặt vé chỉ được in vé giấy một lần duy nhất để chống gian lận in lậu vé. <td colspan=3/> |

Bảng A.36: Use case tra cứu hóa đơn và in vé giấy từ mã đặt vé trực tuyến.

---

| Mã Use case | UC-37 | Tên Use Case | Tra cứu thông tin vé và người ngồi ghế khi xảy ra sự cố phòng chiếu |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Hỗ trợ nhân viên và quản trị viên tra cứu tức thì thông tin đơn vé và khách hàng đang ngồi tại một vị trí ghế cụ thể trong phòng chiếu khi có sự cố phát sinh. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên truy cập phân hệ "Xử lý sự cố" trên giao diện quản trị/nội bộ. <br> Bước 2: Nhân viên nhập Mã đặt vé / Số điện thoại của khách hàng hoặc chọn trực tiếp theo Phòng chiếu → Suất chiếu → Vị trí ghế ngồi gặp sự cố. <br> Bước 3: Hệ thống truy vấn ngược và hiển thị chi tiết đơn đặt vé liên quan: Họ tên khách hàng, Số điện thoại, Mã vé, Loại vé, Giá vé và Tình trạng thanh toán. <td colspan=3/> |
| Lưu ý | - Tính năng này áp dụng phân quyền nghiêm ngặt theo cụm rạp để đảm bảo tính riêng tư của dữ liệu khách hàng. <td colspan=3/> |

Bảng A.37: Use case tra cứu thông tin vé và người ngồi ghế khi xảy ra sự cố phòng chiếu.

---

| Mã Use case | UC-38 | Tên Use Case | Xử lý đổi chỗ ngồi cho khách do lỗi kỹ thuật |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Xử lý nghiệp vụ đổi chỗ ngồi cho khách sang vị trí ghế mới tương đương khi ghế cũ bị hỏng, ướt hoặc gặp lỗi kỹ thuật, đồng thời giữ nguyên mã vé ban đầu. <td colspan=3/> |
| Luồng chạy | Bước 1: Tại màn hình xử lý sự cố, nhân viên chọn đơn vé và vị trí ghế cũ gặp sự cố của khách hàng. <br> Bước 2: Hệ thống hiển thị sơ đồ phòng chiếu với các vị trí ghế còn trống trong cùng suất chiếu. <br> Bước 3: Nhân viên chọn vị trí ghế mới phù hợp cho khách hàng. <br> Bước 4: Nhân viên có thể chọn thêm phương án đền bù thiện chí bằng Voucher quà tặng (Combo bắp nước, Voucher giảm giá) theo quy định chăm sóc khách hàng. <br> Bước 5: Nhân viên nhấn "Xác nhận chuyển ghế". <br> Bước 6: Hệ thống cập nhật điều chuyển vị trí ghế tại chỗ, giữ nguyên mã vé/mã QR cũ, in lại vé giấy mới với vị trí ghế đã đổi và phát voucher đền bù cho khách (nếu có). <td colspan=3/> |
| Lưu ý | - Việc đổi ghế trong cùng suất chiếu không phát sinh chênh lệch tiền vé; hệ thống ghi vết đầy đủ nhân viên thao tác và lý do đổi ghế vào lịch sử sự cố. <td colspan=3/> |

Bảng A.38: Use case xử lý đổi chỗ ngồi cho khách do lỗi kỹ thuật.

---

| Mã Use case | UC-39 | Tên Use Case | Xử lý hủy chỗ và đền bù bằng Voucher quà tặng |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Xử lý tình huống bất khả kháng khi phòng chiếu hết ghế trống để đổi cho khách; hệ thống tiến hành hủy chỗ và đền bù toàn bộ bằng Voucher vé xem phim miễn phí. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên tra cứu đơn vé và chọn chức năng "Hủy chỗ & Đền bù sự cố". <br> Bước 2: Nhân viên chọn vị trí ghế cần hủy và nhập lý do sự cố (ghế hỏng toàn diện, sự cố kỹ thuật phòng chiếu). <br> Bước 3: Hệ thống tự động tính toán giá trị đền bù tương ứng với 100% giá trị tiền vé của ghế bị hủy. <br> Bước 4: Nhân viên nhấn nút "Xác nhận hủy chỗ và đền bù". <br> Bước 5: Hệ thống hủy vị trí ghế trong đơn hàng, giải phóng trạng thái ghế và tự động phát một Voucher đền bù 100% giá vé vào ví tài khoản của khách hàng (hoặc gửi quà đền bù tại quầy cho khách vãng lai). <td colspan=3/> |
| Lưu ý | - Hệ thống không thực hiện hoàn tiền mặt mà giải quyết bồi thường thông qua chính sách phát hành Voucher quà tặng theo quy định vận hành của rạp. <td colspan=3/> |

Bảng A.39: Use case xử lý hủy chỗ và đền bù bằng voucher quà tặng.

---

| Mã Use case | UC-40 | Tên Use Case | Khóa bảo trì ghế hỏng vật lý tại phòng chiếu |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Cho phép nhân viên hoặc quản lý lập tức khóa trạng thái của một chiếc ghế bị gãy, rách hoặc hỏng hóc kỹ thuật sang chế độ bảo trì để ngăn chặn việc bán vé ở mọi suất chiếu tiếp theo. <td colspan=3/> |
| Luồng chạy | Bước 1: Tại màn hình xử lý sự cố hoặc quản lý sơ đồ ghế, nhân viên chọn chiếc ghế bị hỏng. <br> Bước 2: Nhân viên chọn thao tác "Khóa bảo trì ghế". <br> Bước 3: Nhân viên nhập mô tả tình trạng hư hỏng (ví dụ: gãy tay vịn, hỏng đệm ngồi). <br> Bước 4: Nhân viên nhấn "Xác nhận khóa bảo trì". <br> Bước 5: Hệ thống chuyển trạng thái vật lý của ghế sang "Bảo trì (MAINTENANCE)"; ghế này sẽ lập tức biến mất khỏi danh sách ghế trống của toàn bộ các suất chiếu trong tương lai. <td colspan=3/> |
| Lưu ý | - Khi ghế đã được sửa chữa xong, quản lý có thể thao tác mở lại trạng thái hoạt động bình thường cho ghế. <td colspan=3/> |

Bảng A.40: Use case khóa bảo trì ghế hỏng vật lý tại phòng chiếu.

---

| Mã Use case | UC-41 | Tên Use Case | Tạo yêu cầu phê duyệt Hủy hóa đơn bắp nước đã bán |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Nhân viên bán vé & soát vé |
| Mô tả | Cho phép nhân viên quầy gửi yêu cầu hủy hóa đơn bắp nước lên Quản lý rạp khi khách trả lại đồ hoặc nhân viên nhập nhầm món cần sửa sai. <td colspan=3/> |
| Luồng chạy | Bước 1: Nhân viên tra cứu hóa đơn bán bắp nước cần hủy trong ngày. <br> Bước 2: Nhân viên nhấn chọn chức năng "Yêu cầu hủy hóa đơn (Void)". <br> Bước 3: Nhân viên nhập lý do yêu cầu hủy (khách đổi ý, pha nhầm món...). <br> Bước 4: Nhân viên nhấn "Gửi yêu cầu phê duyệt". <br> Bước 5: Hệ thống đưa yêu cầu vào Hàng đợi phê duyệt (Approval Queue) để chờ Quản lý rạp hoặc Quản trị viên xem xét xử lý. <td colspan=3/> |
| Lưu ý | - Nhân viên không thể tự ý hủy hóa đơn đã thanh toán nếu chưa có sự phê duyệt chính thức từ Quản trị viên hoặc Quản lý cơ sở. <td colspan=3/> |

Bảng A.41: Use case tạo yêu cầu phê duyệt hủy hóa đơn bắp nước đã bán.

---

## A.3. ĐẶC TẢ CÁC USE CASE KHỐI QUẢN TRỊ VIÊN & QUẢN LÝ (ADMIN / MANAGER PORTAL)

| Mã Use case | UC-42 | Tên Use Case | Đăng nhập Trang quản trị hệ thống |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Xác thực tài khoản của Quản trị viên cấp cao hoặc Quản lý cụm rạp để cấp quyền điều hành toàn bộ các phân hệ quản trị của DevCine. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập đường dẫn cổng quản trị hệ thống. <br> Bước 2: Nhập tên đăng nhập và mật khẩu tài khoản quản trị. <br> Bước 3: Nhấn nút "Đăng nhập". <br> Bước 4: Hệ thống xác thực danh tính, kiểm tra quyền hạn chi tiết và tải giao diện Bảng điều khiển quản trị. <td colspan=3/> |
| Lưu ý | - Tài khoản người dùng thông thường nếu cố tình truy cập vào trang quản trị sẽ bị từ chối và tự động chuyển hướng về trang lỗi phân quyền. <td colspan=3/> |

Bảng A.42: Use case đăng nhập trang quản trị hệ thống.

---

| Mã Use case | UC-43 | Tên Use Case | Xem Bảng điều khiển (Dashboard) thống kê Tổng quan Doanh thu, Số vé, Khách mới và Tỷ lệ lấp đầy |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Cung cấp bức tranh toàn cảnh về hiệu quả kinh doanh của rạp thông qua các biểu đồ số liệu thời gian thực: Doanh thu, Lượng vé, Khách hàng mới và Tỷ lệ lấp đầy ghế. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập mục "Tổng quan (Dashboard)". <br> Bước 2: Chọn khoảng thời gian cần theo dõi: Hôm nay, Tuần này, Tháng này hoặc chọn Tháng/Năm cụ thể. <br> Bước 3: Hệ thống tổng hợp và hiển thị các thẻ số liệu chính: Tổng doanh thu, Tổng số vé bán ra, Số lượng khách mới, Tỷ lệ lấp đầy phòng chiếu kèm tỷ lệ tăng trưởng so với kỳ trước. <br> Bước 4: Hệ thống hiển thị Biểu đồ diễn biến doanh thu/số vé theo ngày và Bảng xếp hạng Top các bộ phim ăn khách nhất. <td colspan=3/> |
| Lưu ý | - Quản lý cụm rạp chỉ nhìn thấy số liệu thống kê thuộc cụm rạp mình phụ trách; Quản trị viên cấp cao xem được số liệu toàn hệ thống. <td colspan=3/> |

Bảng A.43: Use case xem bảng điều khiển (dashboard) thống kê tổng quan doanh thu, số vé, khách mới và tỷ lệ lấp đầy.

---

| Mã Use case | UC-44 | Tên Use Case | Quản lý Danh sách Phim |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Cho phép quản trị viên thêm mới phim, chỉnh sửa thông tin phim, tải lên hình ảnh áp-phích (poster) và hình nền (banner) qua dịch vụ lưu trữ đám mây. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ "Quản lý Phim". <br> Bước 2: Hệ thống hiển thị danh sách toàn bộ các bộ phim kèm bộ lọc tìm kiếm. <br> Bước 3: Quản trị viên nhấn "Thêm phim mới" hoặc chọn "Chỉnh sửa" một bộ phim. <br> Bước 4: Nhập các trường thông tin: Tên phim, Tên tiếng Anh, Thể loại, Thời lượng, Ngày phát hành, Đạo diễn, Diễn viên, Giới hạn độ tuổi, Mô tả nội dung và Đường dẫn Trailer. <br> Bước 5: Tải hình ảnh Poster và Banner từ máy tính; hệ thống tự động tải lên dịch vụ đám mây và nhận đường dẫn ảnh. <br> Bước 6: Nhấn "Lưu phim". Hệ thống kiểm tra dữ liệu và lưu thông tin vào cơ sở dữ liệu. <td colspan=3/> |
| Lưu ý | - Thời lượng phim phải là số nguyên dương và các trường thông tin quan trọng không được để trống. <td colspan=3/> |

Bảng A.44: Use case quản lý danh sách phim.

---

| Mã Use case | UC-45 | Tên Use Case | Quản lý Trạng thái Phim |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Điều chỉnh trạng thái phát hành của bộ phim (Đang chiếu, Sắp chiếu, Ngừng chiếu) hoặc thực hiện xóa mềm bộ phim khỏi hệ thống. <td colspan=3/> |
| Luồng chạy | Bước 1: Tại danh mục quản lý phim, quản trị viên chọn bộ phim cần cập nhật trạng thái. <br> Bước 2: Thay đổi trạng thái hiển thị của phim (Đang chiếu / Sắp chiếu / Ngừng chiếu) hoặc nhấn biểu tượng Xóa phim. <br> Bước 3: Hệ thống hiển thị hộp thoại xác nhận hành động. <br> Bước 4: Quản trị viên xác nhận; hệ thống cập nhật trạng thái mới của phim trong cơ sở dữ liệu. <td colspan=3/> |
| Lưu ý | - Hệ thống không cho phép xóa vĩnh viễn phim đã từng có phát sinh giao dịch đặt vé nhằm bảo toàn tính toàn vẹn dữ liệu lịch sử. <td colspan=3/> |

Bảng A.45: Use case quản lý trạng thái phim.

---

| Mã Use case | UC-46 | Tên Use Case | Quản lý Danh mục Thể loại phim |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Quản lý danh sách các thể loại phim trong hệ thống (Hành động, Tình cảm, Kinh dị, Hoạt hình, Viễn tưởng...) phục vụ việc phân loại và tìm kiếm phim. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập mục "Danh mục phim" và chọn tab "Thể loại". <br> Bước 2: Hệ thống hiển thị danh sách các thể loại hiện có. <br> Bước 3: Quản trị viên có thể Thêm mới, Chỉnh sửa tên thể loại hoặc Xóa thể loại. <br> Bước 4: Nhấn "Lưu", hệ thống kiểm tra tính hợp lệ và cập nhật dữ liệu. <td colspan=3/> |
| Lưu ý | - Tên thể loại không được chứa ký tự đặc biệt nguy hiểm và không được phép trùng lặp với thể loại đã tồn tại. <td colspan=3/> |

Bảng A.46: Use case quản lý danh mục thể loại phim.

---

| Mã Use case | UC-47 | Tên Use Case | Quản lý Danh mục Định dạng chiếu |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Quản lý các định dạng công nghệ trình chiếu phim (2D, 3D, IMAX, 4DX, ScreenX) để gán cho phòng chiếu, suất chiếu và cấu hình phụ thu giá vé. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên chọn tab "Định dạng chiếu" trong phần Danh mục phim. <br> Bước 2: Hệ thống hiển thị danh sách các định dạng phim đang hỗ trợ. <br> Bước 3: Quản trị viên thực hiện Thêm mới, Chỉnh sửa mã định dạng/tên định dạng hoặc Xóa định dạng. <br> Bước 4: Hệ thống lưu và đồng bộ danh mục định dạng trên toàn hệ thống. <td colspan=3/> |
| Lưu ý | - Không thể xóa định dạng nếu đang có phòng chiếu hoặc suất chiếu sử dụng định dạng này. <td colspan=3/> |

Bảng A.47: Use case quản lý danh mục định dạng chiếu.

---

| Mã Use case | UC-48 | Tên Use Case | Quản lý Danh mục Phân loại độ tuổi kiểm duyệt |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Quản lý các nhãn kiểm duyệt độ tuổi khán giả theo quy định của Cục Điện ảnh (P - Phổ biến, K - Dưới 13 tuổi có phụ huynh, T13, T16, T18, C - Cấm phổ biến). <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên chọn tab "Kiểm duyệt độ tuổi" trong Danh mục phim. <br> Bước 2: Hệ thống hiển thị danh sách các mã phân loại độ tuổi và mô tả quy định tương ứng. <br> Bước 3: Quản trị viên có thể thêm mã mới, chỉnh sửa giải thích độ tuổi hoặc xóa nhãn không còn áp dụng. <br> Bước 4: Nhấn "Lưu thay đổi" để cập nhật vào hệ thống. <td colspan=3/> |
| Lưu ý | - Mã kiểm duyệt được chuẩn hóa tự động viết hoa (ví dụ: T13, T18) để bảo đảm tính thống nhất. <td colspan=3/> |

Bảng A.48: Use case quản lý danh mục phân loại độ tuổi kiểm duyệt.

---

| Mã Use case | UC-49 | Tên Use Case | Quản lý Cụm rạp chi nhánh |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Cho phép thêm mới cụm rạp chi nhánh, cập nhật thông tin tên rạp, địa chỉ chi tiết, khu vực tỉnh/thành phố và thông tin liên hệ của từng rạp. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ "Quản lý Rạp chiếu". <br> Bước 2: Hệ thống hiển thị danh sách các cụm rạp hiện có trên toàn quốc. <br> Bước 3: Quản trị viên nhấn "Thêm cụm rạp mới" hoặc chọn rạp để "Chỉnh sửa". <br> Bước 4: Nhập thông tin: Tên cụm rạp, Tỉnh/Thành phố, Địa chỉ chi tiết, Số điện thoại liên hệ và hình ảnh đại diện. <br> Bước 5: Nhấn "Lưu cụm rạp", hệ thống cập nhật vào cơ sở dữ liệu. <td colspan=3/> |
| Lưu ý | - Mỗi cụm rạp là một không gian vận hành độc lập phục vụ cho việc phân quyền nhân sự và cách ly số liệu doanh thu. <td colspan=3/> |

Bảng A.49: Use case quản lý cụm rạp chi nhánh.

---

| Mã Use case | UC-50 | Tên Use Case | Quản lý Phòng chiếu |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Quản lý danh sách các phòng chiếu (halls) thuộc từng cụm rạp, thiết lập định dạng hỗ trợ của phòng và theo dõi tổng sức chứa ghế ngồi. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên chọn một cụm rạp cụ thể và truy cập tab "Cơ sở vật chất". <br> Bước 2: Hệ thống hiển thị danh sách các phòng chiếu kèm định dạng phòng (2D, 3D, IMAX) và tổng số lượng ghế. <br> Bước 3: Quản trị viên nhấn "Thêm phòng chiếu", nhập tên phòng (ví dụ: Phòng 01, Cinema 2) và chọn định dạng chiếu. <br> Bước 4: Nhấn "Xác nhận tạo phòng". Hệ thống lưu phòng chiếu mới vào cụm rạp. <td colspan=3/> |
| Lưu ý | - Hệ thống yêu cầu xác nhận kỹ lưỡng khi xóa phòng chiếu để tránh ảnh hưởng đến các suất chiếu đang được xếp lịch. <td colspan=3/> |

Bảng A.50: Use case quản lý phòng chiếu.

---

| Mã Use case | UC-51 | Tên Use Case | Thiết kế và Lưu trữ Sơ đồ ma trận ghế phòng chiếu |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Cung cấp công cụ vẽ và chỉnh sửa ma trận ghế trực quan: Tạo kích thước hàng/cột, gắn nhãn ghế (A1, B2...), thiết lập loại ghế (Thường, VIP, Đôi, Lối đi). <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên chọn một phòng chiếu và nhấn chức năng "Sơ đồ ghế". <br> Bước 2: Hệ thống mở giao diện công cụ thiết kế ma trận ghế trực quan. <br> Bước 3: Quản trị viên cấu hình số hàng và số cột tổng thể của phòng. <br> Bước 4: Sử dụng công cụ cọ vẽ (brush) để phân bổ các vị trí: Ghế thường, Ghế VIP, Ghế Sweetbox hoặc làm Khoảng trống / Lối đi. <br> Bước 5: Kiểm tra bảng xem trước sơ đồ ghế và nhấn nút "Lưu sơ đồ ghế". <br> Bước 6: Hệ thống tự động khởi tạo danh sách ghế thực tế trong cơ sở dữ liệu theo đúng sơ đồ vừa vẽ. <td colspan=3/> |
| Lưu ý | - Việc sửa đổi sơ đồ ghế chỉ được áp dụng cho các suất chiếu được tạo mới sau thời điểm lưu sơ đồ. <td colspan=3/> |

Bảng A.51: Use case thiết kế và lưu trữ sơ đồ ma trận ghế phòng chiếu.

---

| Mã Use case | UC-52 | Tên Use Case | Lập lịch và Điều phối suất chiếu |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Hỗ trợ xếp lịch chiếu phim trên dòng thời gian trực quan, hỗ trợ tạo suất lẻ hoặc tạo hàng loạt (batch scheduling), tự động kiểm tra xung đột khung giờ chiếu. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập tab "Lịch chiếu" của cụm rạp và chọn Ngày cần xếp lịch. <br> Bước 2: Hệ thống hiển thị dòng thời gian trực quan của tất cả các phòng chiếu trong ngày. <br> Bước 3: Quản trị viên nhấn vào khung giờ trống hoặc nhấn "Tạo suất chiếu". <br> Bước 4: Chọn Phim, Phòng chiếu, Định dạng, Thời gian bắt đầu và Giá vé áp dụng. <br> Bước 5: Hệ thống tự động cộng thời lượng phim cùng thời gian dọn dẹp vệ sinh phòng (mặc định 15 phút) để tính thời gian kết thúc và kiểm tra xung đột với các suất chiếu khác trong cùng phòng. <br> Bước 6: Nếu không có xung đột, quản trị viên nhấn "Xuất bản suất chiếu" để mở bán. <td colspan=3/> |
| Lưu ý | - Hệ thống lập tức cảnh báo màu đỏ và ngăn chặn lưu nếu phát hiện thời gian hai suất chiếu bị chồng lấn lên nhau. <td colspan=3/> |

Bảng A.52: Use case lập lịch và điều phối suất chiếu.

---

| Mã Use case | UC-53 | Tên Use Case | Điều chỉnh hoặc Hủy bỏ Suất chiếu |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Cho phép thay đổi giờ chiếu, chuyển phòng chiếu hoặc hủy bỏ một suất chiếu đã lên lịch khi có sự cố kỹ thuật hoặc thay đổi kế hoạch chiếu. <td colspan=3/> |
| Luồng chạy | Bước 1: Trên dòng thời gian lịch chiếu, quản trị viên chọn một suất chiếu cụ thể. <br> Bước 2: Xem chi tiết tình trạng suất: Số vé đã bán, doanh thu tạm tính và danh sách ghế đã được đặt. <br> Bước 3: Quản trị viên có thể chỉnh sửa khung giờ bắt đầu hoặc nhấn nút "Hủy suất chiếu". <br> Bước 4: Hệ thống kiểm tra: Nếu suất chiếu đã có khách đặt vé, hệ thống yêu cầu xử lý đền bù/chuyển vé trước khi hủy. <br> Bước 5: Xác nhận thao tác, hệ thống cập nhật trạng thái suất chiếu thành Đã hủy (Cancelled). <td colspan=3/> |
| Lưu ý | - Hủy suất chiếu đã có vé bán ra là thao tác nghiêm trọng và được ghi vết chi tiết vào Nhật ký kiểm toán hệ thống. <td colspan=3/> |

Bảng A.53: Use case điều chỉnh hoặc hủy bỏ suất chiếu.

---

| Mã Use case | UC-54 | Tên Use Case | Quản lý Thực đơn Bắp nước |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Quản lý danh sách các món ăn, nước ngọt và các gói combo ẩm thực: Thêm món, chỉnh sửa giá bán, tải ảnh minh họa và bật/tắt trạng thái mở bán. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ "Thực đơn F&B". <br> Bước 2: Hệ thống hiển thị danh sách toàn bộ các sản phẩm ẩm thực đang có. <br> Bước 3: Nhấn "Thêm món mới" hoặc chọn món để "Chỉnh sửa". <br> Bước 4: Nhập Tên sản phẩm, Loại món (Combo, Bắp rang, Nước uống, Snack), Đơn giá niêm yết, Hình ảnh sản phẩm và Mô tả thành phần. <br> Bước 5: Thiết lập trạng thái Bật/Tắt kinh doanh. <br> Bước 6: Nhấn "Lưu thông tin", hệ thống cập nhật thực đơn tức thì trên toàn hệ thống đặt vé online và POS. <td colspan=3/> |
| Lưu ý | - Hệ thống quản lý tồn kho vô hạn, tập trung hoàn toàn vào việc hiển thị thực đơn và định giá bán chính xác. <td colspan=3/> |

Bảng A.54: Use case quản lý thực đơn bắp nước.

---

| Mã Use case | UC-55 | Tên Use Case | Quản lý Nhóm tùy chọn Bắp nước |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Quản lý kho các tùy chọn khẩu vị (Vị phô mai, Vị caramel, Nước suối, Nước ngọt các loại) và gán các ô chọn vị linh hoạt vào từng combo. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên chọn tab "Kho tùy chọn" trong phân hệ Thực đơn F&B. <br> Bước 2: Quản trị viên khởi tạo các nhóm tùy chọn (ví dụ: Nhóm Vị Bắp, Nhóm Loại Nước) và danh sách các vị kèm mức giá phụ thu thêm (nếu có). <br> Bước 3: Khi cấu hình Combo, quản trị viên thêm các ô lựa chọn (Slots) và gán nhóm tùy chọn tương ứng (ví dụ: Ô 1 chọn 1 vị bắp, Ô 2 chọn 2 loại nước). <br> Bước 4: Nhấn "Lưu cấu hình combo". Hệ thống tự động sinh giao diện chọn vị thông minh cho khách hàng và thu ngân. <td colspan=3/> |
| Lưu ý | - Hỗ trợ thiết lập số lượng lựa chọn tối thiểu và tối đa trên từng ô để đảm bảo khách chọn đúng số lượng món trong combo. <td colspan=3/> |

Bảng A.55: Use case quản lý nhóm tùy chọn bắp nước.

---

| Mã Use case | UC-56 | Tên Use Case | Quản lý Chương trình Khuyến mãi |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Khởi tạo và quản lý các chương trình ưu đãi, voucher giảm giá: Thiết lập loại giảm giá (phần trăm hoặc tiền cố định), điều kiện đơn hàng, thời gian áp dụng. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ "Khuyến mãi & Ưu đãi". <br> Bước 2: Hệ thống hiển thị danh sách các chương trình khuyến mãi hiện có. <br> Bước 3: Quản trị viên nhấn "Tạo chương trình mới". <br> Bước 4: Thiết lập các thông số: Mã code khuyến mãi (ví dụ: SUMMER2026), Tên chương trình, Loại giảm giá (% hoặc Tiền mặt), Giá trị giảm, Giảm tối đa, Đơn hàng tối thiểu, Số vé tối đa áp dụng, Ngày bắt đầu, Ngày kết thúc và Đối tượng khách hàng áp dụng. <br> Bước 5: Cấu hình tùy chọn cho phép khách hàng đổi bằng điểm thưởng tích lũy (nếu áp dụng). <br> Bước 6: Nhấn "Lưu khuyến mãi". Hệ thống kích hoạt chương trình theo đúng lịch trình. <td colspan=3/> |
| Lưu ý | - Mã khuyến mãi đang trong thời gian chạy sẽ được khóa ngày bắt đầu để đảm bảo tính nhất quán của dữ liệu giao dịch. <td colspan=3/> |

Bảng A.56: Use case quản lý chương trình khuyến mãi.

---

| Mã Use case | UC-57 | Tên Use Case | Phát Mã giảm giá trực tiếp cho Khách hàng cụ thể |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Cho phép quản trị viên cấp phát trực tiếp một mã giảm giá hoặc phiếu ưu đãi đặc biệt vào ví tài khoản của một khách hàng cụ thể để tri ân hoặc hỗ trợ. <td colspan=3/> |
| Luồng chạy | Bước 1: Tại danh sách chương trình khuyến mãi, quản trị viên chọn thao tác "Phát voucher cho khách". <br> Bước 2: Tìm kiếm và chọn tài khoản khách hàng cần phát mã qua Số điện thoại hoặc Email. <br> Bước 3: Thiết lập hạn sử dụng riêng cho voucher của khách (nếu cần). <br> Bước 4: Nhấn "Xác nhận phát voucher". <br> Bước 5: Hệ thống tạo một voucher mới gắn liền với tài khoản của khách hàng và gửi thông báo vào ứng dụng của khách. <td colspan=3/> |
| Lưu ý | - Voucher được phát trực tiếp sẽ hiển thị ngay trong mục "Ưu đãi của tôi" trên tài khoản của khách hàng. <td colspan=3/> |

Bảng A.57: Use case phát mã giảm giá trực tiếp cho khách hàng cụ thể.

---

| Mã Use case | UC-58 | Tên Use Case | Gửi Chiến dịch Email Thông báo Khuyến mãi cho tập khách hàng đủ điều kiện |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Tự động quét tập khách hàng thỏa mãn tiêu chí của chương trình ưu đãi và gửi thư điện tử quảng bá chiến dịch kèm mã voucher đến hộp thư của khách. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên chọn một chương trình khuyến mãi đang hoạt động. <br> Bước 2: Nhấn chọn chức năng "Gửi email chiến dịch". <br> Bước 3: Hệ thống hiển thị số lượng khách hàng thuộc đối tượng áp dụng (Tất cả khách hàng, Khách hàng mới, Khách VIP...) chưa nhận thông báo. <br> Bước 4: Quản trị viên xác nhận gửi. <br> Bước 5: Hệ thống kích hoạt tiến trình gửi email hàng loạt kèm mẫu thư thiết kế đẹp mắt và thông báo tổng số thư đã gửi thành công. <td colspan=3/> |
| Lưu ý | - Hệ thống tự động loại trừ các khách hàng đã nhận email mã này trước đó để tránh gửi thư trùng lặp gây phiền hà. <td colspan=3/> |

Bảng A.58: Use case gửi chiến dịch email thông báo khuyến mãi cho tập khách hàng đủ điều kiện.

---

| Mã Use case | UC-59 | Tên Use Case | Thiết lập Quy tắc Bảng giá vé |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Cấu hình động công thức tính giá vé xem phim duy nhất cho toàn hệ thống: Giá nền theo Thứ trong tuần × Khung giờ × Đối tượng; Phụ thu loại ghế; Phụ thu định dạng và Phụ thu ngày lễ. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ "Cấu hình Bảng giá". <br> Bước 2: Tại tab "Giá nền", thiết lập ma trận mức giá cơ bản theo các ngày trong tuần (Thứ 2 đến Thứ 5, Cuối tuần), các khung giờ (Trước 12h, Sau 12h) và Đối tượng khán giả (Người lớn, Học sinh/Sinh viên). <br> Bước 3: Tại tab "Phụ thu loại ghế", thiết lập mức phụ thu thêm cho Ghế VIP và Ghế đôi Sweetbox. <br> Bước 4: Tại tab "Phụ thu định dạng", thiết lập mức phụ thu thêm cho các định dạng đặc biệt (3D, IMAX). <br> Bước 5: Tại tab "Ngày lễ", quản lý danh sách ngày nghỉ lễ quốc gia và cấu hình mức phụ thu ngày lễ. <br> Bước 6: Nhấn "Lưu bảng giá". Toàn bộ hệ thống đặt vé sẽ áp dụng ngay công thức tính giá mới. <td colspan=3/> |
| Lưu ý | - Công thức tính giá vé máy chủ đảm bảo tính nhất quán tuyệt đối giữa kênh bán trực tuyến (Online) và kênh bán tại quầy (POS). <td colspan=3/> |

Bảng A.59: Use case thiết lập quy tắc bảng giá vé.

---

| Mã Use case | UC-60 | Tên Use Case | Sử dụng Công cụ Mô phỏng & Tính thử Giá vé trực quan |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Cung cấp công cụ mô phỏng trực tiếp giúp quản trị viên kiểm thử nhanh công thức tính giá vé xem phim với các tham số đầu vào giả định trước khi ban hành. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập tab "Tính thử giá vé (Simulator)" trong phân hệ Bảng giá. <br> Bước 2: Chọn các tham số thử nghiệm: Ngày chiếu, Giờ chiếu, Loại ghế (Thường/VIP/Đôi), Định dạng phòng chiếu và Đối tượng khán giả. <br> Bước 3: Hệ thống lập tức tính toán và bóc tách chi tiết cấu thành giá: [Giá nền] + [Phụ thu ghế] + [Phụ thu định dạng] + [Phụ thu ngày lễ] = [Giá vé cuối cùng]. <br> Bước 4: Quản trị viên đối chiếu kết quả để đảm bảo chính sách giá được thiết lập đúng như mong muốn. <td colspan=3/> |
| Lưu ý | - Công cụ chạy thử nghiệm độc lập không làm thay đổi hay ghi dữ liệu giao dịch vào hệ thống. <td colspan=3/> |

Bảng A.60: Use case sử dụng công cụ mô phỏng & tính thử giá vé trực quan.

---

| Mã Use case | UC-61 | Tên Use Case | Quản lý Danh sách Hóa đơn đặt vé và Xem chi tiết giao dịch toàn hệ thống |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Quản lý toàn bộ danh sách các đơn đặt vé trong hệ thống, tìm kiếm lọc theo trạng thái/kênh bán, xem chi tiết vé, bắp nước và thông tin thanh toán đối soát. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ "Quản lý Hóa đơn (Bookings)". <br> Bước 2: Sử dụng bộ lọc tìm kiếm theo: Mã đơn hàng, Trạng thái (Hoàn tất, Đang giữ, Đã hủy), Kênh bán (Online / Quầy POS), Phương thức thanh toán và Khoảng ngày giao dịch. <br> Bước 3: Nhấn chọn một đơn hàng để mở cửa sổ chi tiết giao dịch. <br> Bước 4: Hệ thống hiển thị chi tiết: Thông tin khách hàng, Tên phim, Suất chiếu, Chi tiết ghế và giá vé, Chi tiết bắp nước và các tùy chọn vị, Mã giảm giá đã áp dụng, Mã tham chiếu ngân hàng và Trạng thái in vé/check-in. <td colspan=3/> |
| Lưu ý | - Hỗ trợ xem lại bản in biên lai hóa đơn để phục vụ công tác đối chiếu hoặc hỗ trợ giải quyết khiếu nại của khách hàng. <td colspan=3/> |

Bảng A.61: Use case quản lý danh sách hóa đơn đặt vé và xem chi tiết giao dịch toàn hệ thống.

---

| Mã Use case | UC-62 | Tên Use Case | Quản lý Danh sách Khách hàng và Lịch sử giao dịch |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Theo dõi danh sách người dùng thành viên, cấp bậc thành viên, tổng điểm tích lũy và tổng chi tiêu của từng khách hàng trong toàn hệ thống. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ "Khách hàng". <br> Bước 2: Hệ thống hiển thị danh sách khách hàng kèm Họ tên, Số điện thoại, Email, Hạng thành viên (Đồng, Bạc, Vàng, Bạch Kim) và Điểm thưởng. <br> Bước 3: Quản trị viên có thể tìm kiếm khách hàng theo tên hoặc số điện thoại. <br> Bước 4: Nhấn vào một khách hàng để xem lịch sử tất cả các đơn đặt vé mà khách hàng đó đã thực hiện. <td colspan=3/> |
| Lưu ý | - Hệ thống hỗ trợ bảo mật thông tin nhạy cảm của khách hàng theo các tiêu chuẩn an toàn dữ liệu. <td colspan=3/> |

Bảng A.62: Use case quản lý danh sách khách hàng và lịch sử giao dịch.

---

| Mã Use case | UC-63 | Tên Use Case | Quản lý Danh sách Nhân viên và Thông tin phân bổ theo Cụm rạp |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Quản lý tài khoản nhân viên nội bộ: Khởi tạo tài khoản nhân sự mới, gán vai trò quyền hạn (Quản trị viên, Quản lý, Nhân viên), phân bổ rạp làm việc và khóa tài khoản khi cần. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ "Quản lý Nhân viên". <br> Bước 2: Hệ thống hiển thị danh sách nhân sự kèm vai trò và cụm rạp công tác tương ứng. <br> Bước 3: Quản trị viên nhấn "Thêm nhân viên mới", nhập Họ tên, Tên đăng nhập, Email, Số điện thoại, Vai trò (ADMIN, MANAGER, STAFF) và chọn Cụm rạp phân công. <br> Bước 4: Nhấn "Lưu nhân viên". Hệ thống tạo tài khoản với mật khẩu tạm thời và bật cờ yêu cầu đổi mật khẩu lần đầu. <br> Bước 5: Quản trị viên có thể thực hiện Khóa / Kích hoạt lại tài khoản hoặc Đặt lại mật khẩu cho nhân viên khi có yêu cầu. <td colspan=3/> |
| Lưu ý | - Nhân viên được gán cụm rạp nào sẽ chỉ có quyền thực hiện bán vé và soát vé trong phạm vi của cụm rạp đó. <td colspan=3/> |

Bảng A.63: Use case quản lý danh sách nhân viên và thông tin phân bổ theo cụm rạp.

---

| Mã Use case | UC-64 | Tên Use Case | Thiết lập Ma trận Phân quyền người dùng |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Cung cấp bảng ma trận phân quyền chi tiết cho phép bật/tắt các quyền hạn Xem, Thêm, Sửa, Xóa trên từng phân hệ chức năng cho từng vai trò người dùng trong hệ thống. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên cấp cao truy cập mục "Phân quyền (Roles & Permissions)". <br> Bước 2: Hệ thống hiển thị ma trận quyền gồm danh sách các Tính năng (Phim, Rạp, Lịch chiếu, Bán vé POS, Bắp nước, Khuyến mãi, Báo cáo, Nhật ký...) theo các Cột quyền hạn (Xem, Thêm, Sửa, Xóa, Xử lý). <br> Bước 3: Quản trị viên tích chọn hoặc bỏ chọn các quyền hạn tương ứng cho từng vai trò (Quản lý, Nhân viên). <br> Bước 4: Nhấn "Lưu ma trận phân quyền". <br> Bước 5: Hệ thống cập nhật và áp dụng quy tắc kiểm tra quyền hạn tức thì trên toàn bộ các yêu cầu thao tác. <td colspan=3/> |
| Lưu ý | - Vai trò Quản trị viên tối cao (Admin) luôn mặc định sở hữu toàn bộ các quyền hạn và không thể bị vô hiệu hóa. <td colspan=3/> |

Bảng A.64: Use case thiết lập ma trận phân quyền người dùng.

---

| Mã Use case | UC-65 | Tên Use Case | Quản lý và Phê duyệt Yêu cầu Hủy đơn hàng Bắp nước |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Xem xét danh sách các yêu cầu hủy hóa đơn bắp nước do nhân viên quầy gửi lên và đưa ra quyết định Phê duyệt hoặc Từ chối kèm ghi chú lý do. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản lý hoặc Quản trị viên truy cập mục "Hàng đợi phê duyệt (Approval Queue)". <br> Bước 2: Hệ thống hiển thị danh sách các yêu cầu hủy đơn hàng F&B đang ở trạng thái Chờ duyệt kèm thông tin: Nhân viên tạo yêu cầu, Mã đơn, Giá trị đơn, Thời gian và Lý do hủy. <br> Bước 3: Quản lý kiểm tra thông tin thực tế và nhấn nút "Phê duyệt" hoặc "Từ chối". <br> Bước 4: Nếu từ chối, quản lý nhập ghi chú lý do từ chối. <br> Bước 5: Hệ thống cập nhật trạng thái đơn hàng, hủy ghi nhận doanh thu nếu được duyệt và thông báo kết quả cho nhân viên quầy. <td colspan=3/> |
| Lưu ý | - Chỉ người dùng có vai trò Quản trị viên hoặc Quản lý cơ sở mới có thẩm quyền thực hiện phê duyệt các yêu cầu này. <td colspan=3/> |

Bảng A.65: Use case quản lý và phê duyệt yêu cầu hủy đơn hàng bắp nước.

---

| Mã Use case | UC-66 | Tên Use Case | Kiểm duyệt và Quản lý Đánh giá / Bình luận phim của khách hàng |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Giúp quản trị viên kiểm soát nội dung đánh giá của khách hàng: Theo dõi danh sách nhận xét, ẩn các bình luận có nội dung không phù hợp hoặc xóa đánh giá vi phạm. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ "Quản lý Đánh giá (Reviews)". <br> Bước 2: Hệ thống hiển thị danh sách tất cả các đánh giá của người dùng kèm Tên phim, Người đánh giá, Số sao, Nội dung nhận xét, Thời gian và Trạng thái ẩn/hiện. <br> Bước 3: Quản trị viên có thể nhấn nút "Ẩn/Hiện" để tạm thời ẩn bình luận khỏi trang chi tiết phim. <br> Bước 4: Quản trị viên có thể nhấn "Xóa" đối với các đánh giá chứa ngôn từ vi phạm tiêu chuẩn cộng đồng. <br> Bước 5: Hệ thống cập nhật trạng thái hiển thị và tự động tính toán lại điểm số trung bình của bộ phim. <td colspan=3/> |
| Lưu ý | - Tính năng này bảo vệ hình ảnh thương hiệu và giữ môi trường thảo luận điện ảnh văn minh, lành mạnh. <td colspan=3/> |

Bảng A.66: Use case kiểm duyệt và quản lý đánh giá / bình luận phim của khách hàng.

---

| Mã Use case | UC-67 | Tên Use Case | Tiếp nhận, Quản lý trạng thái và Phản hồi Yêu cầu Hỗ trợ khách hàng qua Email |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Tiếp nhận các phiếu yêu cầu hỗ trợ từ khách hàng, phân loại, cập nhật tiến độ xử lý và soạn thư phản hồi gửi trực tiếp đến email của khách hàng. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ "Chăm sóc khách hàng / Hỗ trợ (Customer Support)". <br> Bước 2: Hệ thống hiển thị danh sách các phiếu yêu cầu hỗ trợ (Support Tickets) phân loại theo trạng thái (Mới tiếp nhận, Đang xử lý, Đã giải quyết). <br> Bước 3: Quản trị viên chọn một phiếu hỗ trợ để xem thông tin khách hàng, số điện thoại, chủ đề và nội dung phản ánh. <br> Bước 4: Quản trị viên nhập nội dung giải đáp/xử lý vào ô "Phản hồi khách hàng" và nhấn "Gửi phản hồi qua Email". <br> Bước 5: Hệ thống tự động gửi thư điện tử chứa nội dung trả lời đến email của khách hàng, đồng thời chuyển trạng thái phiếu sang "Đang xử lý / Đã giải quyết". <td colspan=3/> |
| Lưu ý | - Toàn bộ lịch sử trao đổi phản hồi được lưu trữ trong phiếu yêu cầu để tiện theo dõi quá trình hỗ trợ khách hàng. <td colspan=3/> |

Bảng A.67: Use case tiếp nhận, quản lý trạng thái và phản hồi yêu cầu hỗ trợ khách hàng qua email.

---

| Mã Use case | UC-68 | Tên Use Case | Quản lý Danh mục Câu hỏi thường gặp |
|---|---|---|---|
| Độ ưu tiên | Thấp | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Cho phép quản trị viên thêm mới, chỉnh sửa nội dung câu hỏi/câu trả lời, sắp xếp thứ tự hiển thị và bật/tắt các mục giải đáp thắc mắc (FAQ) trên website. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập mục "Quản lý FAQ". <br> Bước 2: Hệ thống hiển thị danh sách các câu hỏi thường gặp hiện có kèm nhóm danh mục. <br> Bước 3: Quản trị viên nhấn "Thêm câu hỏi", nhập Nhóm danh mục, Tiêu đề câu hỏi, Nội dung giải đáp chi tiết và Thứ tự hiển thị. <br> Bước 4: Quản trị viên có thể chỉnh sửa nội dung hoặc Bật/Tắt trạng thái hiển thị của từng câu hỏi. <br> Bước 5: Nhấn "Lưu", hệ thống cập nhật tức thì lên trang Hỏi đáp phía khách hàng. <td colspan=3/> |
| Lưu ý | - Các câu hỏi được thiết lập trạng thái ẩn sẽ chỉ hiển thị ở giao diện quản trị và không xuất hiện trên giao diện người dùng. <td colspan=3/> |

Bảng A.68: Use case quản lý danh mục câu hỏi thường gặp.

---

| Mã Use case | UC-69 | Tên Use Case | Quản lý Đăng tải, Ghim và Điều hướng Banner quảng cáo trang chủ |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Quản lý các biểu ngữ hình ảnh (banner/slider) trên trang chủ: Tải ảnh chất lượng cao, ghim vị trí hiển thị ưu tiên và gắn liên kết điều hướng trực tiếp đến phim hoặc sự kiện. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ "Quản lý Banner". <br> Bước 2: Hệ thống hiển thị danh sách các banner quảng cáo đang hoạt động và banner đã lưu trữ. <br> Bước 3: Quản trị viên nhấn "Thêm Banner mới", tải hình ảnh biểu ngữ chất lượng cao lên hệ thống. <br> Bước 4: Thiết lập Tiêu đề, Phụ đề, Đường dẫn liên kết khi khách bấm vào banner (chuyển đến trang phim, trang khuyến mãi...) và Thứ tự ưu tiên hiển thị. <br> Bước 5: Thiết lập trạng thái Bật/Tắt hiển thị hoặc Ghim đầu trang. <br> Bước 6: Nhấn "Lưu banner", hệ thống cập nhật biểu ngữ lên màn hình chính của trang chủ. <td colspan=3/> |
| Lưu ý | - Hình ảnh banner được tự động tối ưu hóa kích thước hiển thị để đảm bảo tốc độ tải trang nhanh và đẹp mắt trên mọi thiết bị. <td colspan=3/> |

Bảng A.69: Use case quản lý đăng tải, ghim và điều hướng banner quảng cáo trang chủ.

---

| Mã Use case | UC-70 | Tên Use Case | Quản lý Tin tức và Bài viết sự kiện khuyến mãi |
|---|---|---|---|
| Độ ưu tiên | Trung bình | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Soạn thảo và xuất bản các bài viết tin tức điện ảnh, bài giới thiệu phim hot và bài viết chi tiết về các sự kiện khuyến mãi trên trang web. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên chọn tab "Tin bài sự kiện" trong phân hệ Khuyến mãi. <br> Bước 2: Hệ thống hiển thị danh sách các bài viết đã xuất bản. <br> Bước 3: Nhấn "Tạo bài viết mới", nhập Tiêu đề bài viết, Mô tả tóm tắt, Ngày bắt đầu sự kiện, Ngày kết thúc sự kiện và tải Ảnh đại diện. <br> Bước 4: Nhập nội dung bài viết chi tiết và chọn trạng thái Xuất bản (Active) hoặc Lưu nháp (Inactive). <br> Bước 5: Nhấn "Lưu bài viết", hệ thống đăng tải bài viết lên trang tin tức của website. <td colspan=3/> |
| Lưu ý | - Quản trị viên có thể tìm kiếm, chỉnh sửa nội dung hoặc xóa bài viết đã hết hạn sự kiện bất cứ lúc nào. <td colspan=3/> |

Bảng A.70: Use case quản lý tin tức và bài viết sự kiện khuyến mãi.

---

| Mã Use case | UC-71 | Tên Use Case | Xem Nhật ký hoạt động và Ghi vết thao tác của người dùng trên hệ thống |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Theo dõi và giám sát toàn bộ lịch sử thao tác của nhân viên và quản trị viên (Audit Logs): Đăng nhập, Thêm/Sửa/Xóa dữ liệu, Bán vé, Duyệt hủy đơn nhằm phục vụ công tác an ninh và kiểm toán. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên cấp cao truy cập phân hệ "Nhật ký hệ thống (Audit Logs)". <br> Bước 2: Hệ thống hiển thị bảng nhật ký ghi vết tự động theo thời gian thực: Thời gian thực hiện, Tài khoản người thao tác, Vai trò, Địa chỉ IP, Hành động (ĐĂNG NHẬP, TẠO MỚI, CHỈNH SỬA, XÓA), Phân hệ bị tác động và Chi tiết thay đổi. <br> Bước 3: Quản trị viên có thể lọc nhật ký theo Khoảng ngày, Loại hành động hoặc Tên nhân viên cụ thể. <br> Bước 4: Nhấn vào một dòng nhật ký để xem chi tiết thông tin dữ liệu trước và sau khi thay đổi. <td colspan=3/> |
| Lưu ý | - Nhật ký kiểm toán hệ thống là dữ liệu chỉ đọc (Read-only), không một tài khoản nào có quyền chỉnh sửa hoặc xóa dữ liệu nhật ký để đảm bảo tính minh bạch tuyệt đối. <td colspan=3/> |

Bảng A.71: Use case xem nhật ký hoạt động và ghi vết thao tác của người dùng trên hệ thống.

---

| Mã Use case | UC-72 | Tên Use Case | Quản lý Cấu hình tham số hệ thống chung |
|---|---|---|---|
| Độ ưu tiên | Cao | Tác nhân | Quản trị viên / Quản lý rạp |
| Mô tả | Thiết lập các tham số vận hành toàn cục của hệ thống: Thời gian giữ ghế tạm thời, thời gian cho phép bán vé trễ, giới hạn số vé tối đa trên một đơn hàng và thông tin tài khoản VietQR nhận tiền. <td colspan=3/> |
| Luồng chạy | Bước 1: Quản trị viên truy cập phân hệ "Cài đặt hệ thống (Settings)". <br> Bước 2: Hệ thống hiển thị các nhóm tham số cấu hình chung. <br> Bước 3: Quản trị viên điều chỉnh các giá trị vận hành: Thời gian giữ ghế trực tuyến (phút), Thời gian cho phép bán vé trễ sau khi phim chiếu (phút), Số lượng vé tối đa được phép đặt trên một lần giao dịch (vé). <br> Bước 4: Cấu hình thông tin tài khoản nhận thanh toán chuyển khoản tại quầy (Tên ngân hàng, Số tài khoản, Tên chủ tài khoản thụ hưởng phục vụ sinh mã VietQR). <br> Bước 5: Nhấn nút "Lưu cấu hình". <br> Bước 6: Hệ thống lưu trữ các tham số và áp dụng ngay lập tức cho toàn bộ các giao dịch trên toàn hệ thống. <td colspan=3/> |
| Lưu ý | - Các tham số cấu hình này tác động trực tiếp đến logic vận hành thời gian thực của cả cổng khách hàng và quầy bán vé. <td colspan=3/> |

Bảng A.72: Use case quản lý cấu hình tham số hệ thống chung.

---

