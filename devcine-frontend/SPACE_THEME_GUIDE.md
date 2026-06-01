# DEVCINE - SPACE THEME DESIGN GUIDE 🌌

Tài liệu này lưu trữ "Công thức" và "Ý tưởng Thiết kế" (Design System) của giao diện nền Vũ trụ Không gian (Space Theme) được xây dựng riêng cho DevCine. Mục tiêu của tài liệu này là đảm bảo tính nhất quán (Consistency) cho các cập nhật giao diện trong tương lai.

## 1. Hệ thống Màu sắc (Color Palette)

- **Màu nền Không gian (Deep Space):** `#030304` (Đen thẳm nguyên thủy). Tuyệt đối không dùng đen xám để tránh làm mất đi sự sâu thẳm.
- **Màu nhấn (Primary):** `#f5c518` (Vàng điện ảnh - chuẩn phong cách rạp chiếu phim).
- **Ánh sáng các vì sao:** 
  - Trắng tinh khiết (`#ffffff`)
  - Xanh Băng (`#dcebff`)
  - Vàng nhạt (`#fff5cc`)

## 2. Các Lớp Kiến trúc Vũ trụ (Galaxy Architecture)

- **Lớp Tinh vân (Nebula):** Sử dụng các `radial-gradient` kích thước lớn, màu sắc tĩnh mịch (Tím thẫm, Xanh thẫm), có opacity cực thấp (`~0.05`) kết hợp với hiệu ứng thu phóng siêu chậm (Pulse Animation - 25s) để tạo cảm giác vũ trụ đang "thở".
- **Hệ thống Sao (Stars) & Chiều sâu (Parallax):**
  - Gồm 3 lớp sao: Gần (Lớn, chạy nhanh), Trung bình, và Xa (Nhỏ, mờ, chạy chậm).
  - Nghiêng một góc `-8deg` so với màn hình để phá vỡ sự cân xứng nhàm chán.
  - Sử dụng `box-shadow` để sinh ra hàng ngàn ngôi sao thay vì chèn ảnh, giúp tối ưu hiệu năng tuyệt đối.

## 3. Các Tính năng Tương tác Đặc biệt (Interactive Features)

Đây là các hiệu ứng "Tầm cỡ Art Director" làm nên linh hồn của dự án:

### 3.1. Dynamic Ambient Glow (Hắt sáng theo Poster Phim)
- **Cơ chế:** Khi người dùng hover (trỏ chuột) vào một thẻ phim, hệ thống sử dụng thư viện `fast-average-color` để phân tích màu chủ đạo của bức ảnh đó.
- **Hiệu ứng:** Lớp nền không gian (phía sau kính) sẽ từ từ chuyển sắc (glow) theo màu của poster phim (Transition 1s), tạo cảm giác không gian đang sống cùng tác phẩm điện ảnh.

### 3.2. Star Wars Warp / Lightspeed Transition (Chuyển cảnh Tốc độ Ánh sáng)
- **Cơ chế:** Can thiệp vào tầng Router của Vue (`router.beforeEach`). Bắt sự kiện chuyển trang để kích hoạt trạng thái biến đổi.
- **Hiệu ứng:** Delay chuyển trang 0.5s. Trong thời gian đó, toàn bộ bầu trời sao bị kéo giãn thẳng tắp theo chiều dọc (`scaleY(40)`), sáng chói rực lên giống hệt cảnh kích hoạt Hyperspace Drive trong phim Star Wars.

### 3.3. Glass Edge Reflection (Phản chiếu Cạnh Kính)
- **Cơ chế:** Áp dụng Class `.glass-shine-edge`.
- **Hiệu ứng:** Mỗi 15s, một vệt sáng chớp nhoáng (Ice Blue) sẽ xuyệt qua dọc theo mép (border) của các thẻ chứa nội dung. Điều này giả lập việc có một vệt sao băng vô tình xẹt ngang qua và ánh sáng của nó kịp hắt lên rìa mặt kính cường lực.

## 4. Hệ thống Kính (Glassmorphism) chuẩn iOS Dark Acrylic

Tất cả các "Vật thể" (Cards, Banners, Containers) trôi nổi trên không gian này phải tuân thủ chuẩn Kính Cường lực Tối màu (Dark Acrylic) của Apple visionOS/iOS 18.

**Công thức CSS (`.glass-card`):**
```css
.glass-card {
  /* Nền tối màu đầm (Dark Acrylic) */
  background-color: rgba(10, 10, 15, 0.45);
  
  /* Làm nhòe mạnh và tăng độ bão hòa (Đặc trưng iOS Glass) */
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  
  /* Viền nguyên khối cực mờ */
  border: 1px solid rgba(255, 255, 255, 0.04);
  
  /* Viền dập nổi 3D (Beveled Edge): Ánh sáng bạc ở mép trên, bóng đen ở mép dưới */
  box-shadow: 
    0 16px 40px -10px rgba(0, 0, 0, 0.8), /* Đổ bóng sâu xuống không gian */
    inset 0 1px 1px rgba(255, 255, 255, 0.15), /* Hắt sáng mép trên */
    inset 0 -1px 1px rgba(0, 0, 0, 0.6); /* Bắt bóng mép dưới */
    
  /* Lớp phủ gradient chéo tạo độ trơn láng (Glossy) */
  background-image: linear-gradient(135deg, rgba(255, 255, 255, 0.04) 0%, transparent 100%);
}
```

## 5. Quy tắc Phát triển (Rules of Thumb)
1. **Không dùng nền đặc (Solid Backgrounds):** Tuyệt đối không dùng các khối màu nền đặc chặn ngang giữa trang. Mọi thứ phải dùng thẻ kính (Glassmorphism) để khoe được vũ trụ phía sau.
2. **Bo góc tinh tế:** Mọi thành phần từ Card, Banner đến Footer đều phải bo tròn (Border Radius từ `12px` đến `40px`) để phù hợp với độ mượt của ngôn ngữ thiết kế.
3. **Mềm mại:** Bất kỳ sự thay đổi màu sắc hay chuyển động nào cũng phải đi kèm `transition` (ít nhất `0.3s`) để không bị gắt.
