package com.devcine.backend.service;

import com.devcine.backend.dto.TicketEmailData;
import com.devcine.backend.dto.request.BookingRequestDTO;
import com.devcine.backend.dto.request.FnbSelectionDTO;
import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingFnbRepository bookingFnbRepository;
    private final SeatRepository seatRepository;
    private final FnbItemRepository fnbItemRepository;
    private final ShowtimeRepository showtimeRepository;
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;
    private final PromotionRepository promotionRepository;
    private final TicketRepository ticketRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final SystemSettingService systemSettingService;
    private final NotificationService notificationService;
    private final PricingService pricingService;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final SeatLockService seatLockService;
    private final LoyaltyService loyaltyService;
    private final VoucherService voucherService;

    @Transactional
    public Booking holdSeats(BookingRequestDTO request) {
        return holdSeats(request, null, "ONLINE");
    }

    /** POS: giữ ghế do nhân viên {@code soldBy} tạo tại quầy (kênh POS). */
    @Transactional
    public Booking holdSeatsForStaff(BookingRequestDTO request, Staff soldBy) {
        return holdSeats(request, soldBy, "POS");
    }

    private Booking holdSeats(BookingRequestDTO request, Staff soldBy, String channel) {
        // Khóa ghi bi quan trên suất → tuần tự hóa mọi lệnh giữ ghế cùng suất, chống bán trùng (race)
        Showtime showtime = showtimeRepository.findByIdForUpdate(request.getShowtimeId())
                .orElseThrow(() -> new RuntimeException("Showtime not found"));

        // Cách ly cụm rạp cho đơn POS: nhân viên/quản lý chỉ bán suất thuộc cơ sở mình (ADMIN bỏ qua).
        if ("POS".equalsIgnoreCase(channel)) {
            Integer cinemaId = showtime.getRoom() != null && showtime.getRoom().getCinema() != null
                    ? showtime.getRoom().getCinema().getId() : null;
            SecurityUtils.assertCinemaAccess(cinemaId);
        }

        // Chuẩn hoá danh sách ghế kèm loại vé: 1 ghế có thể có nhiều loại vé (VD: Sweetbox)
        java.util.Map<Integer, java.util.List<String>> ticketTypesBySeat = new java.util.LinkedHashMap<>();
        if (request.getSeatSelections() != null && !request.getSeatSelections().isEmpty()) {
            for (var sel : request.getSeatSelections()) {
                ticketTypesBySeat.computeIfAbsent(sel.getSeatId(), k -> new java.util.ArrayList<>())
                        .add(pricingService.normalizeAudience(sel.getTicketType()));
            }
        } else if (request.getSeatIds() != null) {
            for (Integer seatId : request.getSeatIds()) {
                ticketTypesBySeat.computeIfAbsent(seatId, k -> new java.util.ArrayList<>()).add("ADULT");
            }
        }
        java.util.List<Integer> selectedSeatIds = new java.util.ArrayList<>(ticketTypesBySeat.keySet());

        // Validate số lượng vé: phải có ít nhất 1 ghế và không vượt giới hạn cấu hình (chống phe vé)
        if (selectedSeatIds.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất 1 ghế.");
        }

        java.util.List<Seat> allSeats = seatRepository.findByRoomIdAndIsActiveTrue(showtime.getRoom().getId());
        java.util.Map<Integer, Seat> seatMap = new java.util.HashMap<>();
        allSeats.forEach(s -> seatMap.put(s.getId(), s));
        
        int requiredTickets = 0;
        for (Integer seatId : selectedSeatIds) {
            Seat seat = seatMap.get(seatId);
            if (seat == null) throw new RuntimeException("Seat not found");
            int capacity = "SWEETBOX".equals(seat.getSeatType().getName()) ? 2 : 1;
            requiredTickets += capacity;
            if (ticketTypesBySeat.get(seatId).size() != capacity) {
                throw new RuntimeException("Ghế " + seat.displayLabel() + " yêu cầu đúng " + capacity + " loại vé.");
            }
        }
        
        int providedTickets = request.getTotalTickets() != null ? request.getTotalTickets() : 
                (request.getSeatSelections() != null ? request.getSeatSelections().size() : selectedSeatIds.size());
        if (providedTickets < requiredTickets) {
            throw new RuntimeException("Số lượng vé bạn chọn không đủ cho sức chứa của ghế (Sweetbox cần 2 vé).");
        }

        // Anti-fraud theo KÊNH: vé CHILD/SENIOR bắt buộc xác minh giấy tờ/chiều cao tại quầy →
        // cấm bán online (kẻ gian có thể gọi thẳng API dù UI đã ẩn). U22/ADULT cho qua bình thường.
        if ("ONLINE".equalsIgnoreCase(channel)) {
            boolean hasRestricted = ticketTypesBySeat.values().stream()
                    .flatMap(java.util.List::stream)
                    .anyMatch(t -> "CHILD".equals(t) || "SENIOR".equals(t));
            if (hasRestricted) {
                throw new IllegalArgumentException(
                        "Vé Trẻ em / Người cao tuổi chỉ bán tại quầy (cần xác minh giấy tờ). Vui lòng đến rạp để mua.");
            }
        }
        int maxTickets = systemSettingService.getMaxTicketsPerBooking();
        if (selectedSeatIds.size() > maxTickets) {
            throw new RuntimeException("Mỗi lần đặt tối đa " + maxTickets + " vé.");
        }

        // Validate thời gian bán: chỉ cho mua trước giờ chiếu + khoảng trễ cho phép (vd 15 phút sau giờ chiếu)
        int lateMinutes = systemSettingService.getBookingLateMinutes();
        if (showtime.getStartTime() != null
                && LocalDateTime.now().isAfter(showtime.getStartTime().plusMinutes(lateMinutes))) {
            throw new RuntimeException("Suất chiếu đã đóng bán vé.");
        }
        if (showtime.getStatus() != null && "CANCELLED".equalsIgnoreCase(showtime.getStatus())) {
            throw new RuntimeException("Suất chiếu đã bị huỷ.");
        }

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId()).orElse(null);
            // Tự tạo hồ sơ khách cho user chưa có Customer (vd admin/staff đặt vé) → đơn gắn customer + hiện ở lịch sử
            if (customer == null) {
                User u = userRepository.findById(request.getCustomerId()).orElse(null);
                if (u != null) {
                    customer = customerRepository.save(Customer.builder()
                            .user(u) // @MapsId: chỉ set association, KHÔNG set userId (tránh merge)
                            .membershipTier("BRONZE")
                            .loyaltyPoints(0)
                            .build());
                }
            }
        }

        // Validate seats
        int holdMinutes = systemSettingService.getSeatHoldMinutes(); // thời gian giữ ghế admin cấu hình
        List<BookingSeat> existingReservedSeats = bookingSeatRepository.findReservedSeatsByShowtime(request.getShowtimeId());
        for (BookingSeat reserved : existingReservedSeats) {
            if (selectedSeatIds.contains(reserved.getSeat().getId())) {
                boolean isHold = "HOLD".equals(reserved.getStatus());
                // Chỗ giữ quá hạn (quá thời gian cấu hình) coi như đã được giải phóng
                boolean isStale = reserved.getBooking().getCreatedAt() != null
                        && reserved.getBooking().getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(holdMinutes));

                // CHỈ nhả chỗ giữ đã quá hạn. Trước đây còn nhả khi "cùng member" → cho phép
                // 2 phiên cùng tài khoản cướp ghế của nhau (bán trùng). Nay bỏ, kết hợp khóa
                // bi quan ở trên để mỗi ghế chỉ một đơn còn sống giữ tại một thời điểm.
                if (isHold && isStale) {
                    // Giải phóng chỗ giữ cũ để tránh khoá ghế trùng và rác HOLD
                    reserved.setStatus("EXPIRED");
                    bookingSeatRepository.save(reserved);
                    continue;
                }
                throw new RuntimeException("Seat " + reserved.getSeat().getId() + " is already taken or on hold.");
            }
        }

        validateSeatGap(selectedSeatIds, existingReservedSeats, allSeats);

        Booking booking = Booking.builder()
                .customer(customer)
                .showtime(showtime)
                .soldBy(soldBy)
                .channel(channel) // ONLINE (khách đặt) | POS (bán quầy) — nguồn tin cậy tách email
                .bookingCode(UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                .status("HOLD") // Initial status
                .createdAt(LocalDateTime.now())
                .paymentMethod(request.getPaymentMethod())
                .totalPrice(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .build();
                
        bookingRepository.save(booking);

        BigDecimal totalPrice = BigDecimal.ZERO;

        // Process Seats — giá tính tập trung qua PricingService (nạp ngữ cảnh suất một lần).
        PricingService.PricingContext priceCtx = pricingService.buildContext(showtime);
        java.util.List<BookingSeat> bookingSeats = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Integer, java.util.List<String>> entry : ticketTypesBySeat.entrySet()) {
            Seat seat = seatMap.get(entry.getKey());
            // Chặn đặt ghế đang khóa vật lý (bảo trì/khóa) — không phụ thuộc trạng thái runtime
            if (seat.getSeatStatus() != null && !"AVAILABLE".equals(seat.getSeatStatus())) {
                throw new RuntimeException("Ghế " + (seat.getLabel() != null ? seat.getLabel() : seat.getId())
                        + " đang bảo trì/khóa, không thể đặt.");
            }
            java.util.List<String> types = entry.getValue();
            BigDecimal seatPrice = BigDecimal.ZERO;
            for (String t : types) {
                seatPrice = seatPrice.add(pricingService.priceFor(priceCtx, t));
            }
            bookingSeats.add(BookingSeat.builder()
                    .booking(booking)
                    .seat(seat)
                    .priceSnapshot(seatPrice)
                    .ticketType(String.join(",", types))
                    .status("HOLD")
                    .build());
            totalPrice = totalPrice.add(seatPrice);
        }
        bookingSeatRepository.saveAll(bookingSeats);

        // Process F&B — gom 1 query đọc món + saveAll
        if (request.getFnbs() != null && !request.getFnbs().isEmpty()) {
            java.util.List<Integer> fnbIds = request.getFnbs().stream()
                    .map(FnbSelectionDTO::getFnbItemId).toList();
            java.util.Map<Integer, FnbItem> fnbMap = new java.util.HashMap<>();
            fnbItemRepository.findAllById(fnbIds).forEach(i -> fnbMap.put(i.getId(), i));
            java.util.List<BookingFnb> bookingFnbs = new java.util.ArrayList<>();
            for (FnbSelectionDTO fnbDTO : request.getFnbs()) {
                FnbItem item = fnbMap.get(fnbDTO.getFnbItemId());
                if (item == null) throw new RuntimeException("F&B Item not found");
                bookingFnbs.add(BookingFnb.builder()
                        .booking(booking)
                        .fnbItem(item)
                        .quantity(fnbDTO.getQuantity())
                        .priceSnapshot(item.getPrice())
                        .build());
                totalPrice = totalPrice.add(item.getPrice().multiply(new BigDecimal(fnbDTO.getQuantity())));
            }
            bookingFnbRepository.saveAll(bookingFnbs);
        }

        booking.setTotalPrice(totalPrice);
        
        // Process Voucher
        BigDecimal finalPrice = totalPrice;
        if (request.getVoucherId() != null) {
            Voucher voucher = voucherRepository.findById(request.getVoucherId())
                    .orElseThrow(() -> new RuntimeException("Voucher not found"));
            
            if (voucher.getIsUsed()) {
                throw new RuntimeException("Voucher has already been used");
            }
            if (voucher.getValidUntil().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Voucher has expired");
            }
            if (customer == null || !voucher.getCustomer().getUserId().equals(customer.getUserId())) {
                throw new RuntimeException("Voucher does not belong to this customer");
            }

            Promotion promotion = voucher.getPromotion();

            // Chấm điều kiện (đơn tối thiểu / theo phim / đối tượng / lượt dùng) + tính giảm qua
            // NGUỒN SỰ THẬT DUY NHẤT — dùng chung với bước preview để hai bên không lệch nhau.
            java.util.List<BigDecimal> seatPrices = bookingSeats.stream()
                    .map(BookingSeat::getPriceSnapshot)
                    .collect(java.util.stream.Collectors.toList());
            VoucherService.VoucherEval eval = voucherService.evaluate(
                    customer.getUserId(), customer, promotion, totalPrice, showtime.getMovie().getId(), seatPrices);
            if (!eval.applicable()) {
                throw new RuntimeException(eval.reason());
            }
            BigDecimal discount = eval.discountAmount();

            finalPrice = totalPrice.subtract(discount);
            if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                finalPrice = BigDecimal.ZERO;
            }
            booking.setVoucher(voucher);
        }
        
        booking.setFinalPrice(finalPrice);
        bookingRepository.save(booking);
        return booking;
    }

    private void validateSeatGap(List<Integer> selectedSeatIds, List<BookingSeat> reservedSeats, List<Seat> allSeats) {
        if (selectedSeatIds.isEmpty()) return;

        java.util.Set<Integer> reservedIds = reservedSeats.stream()
                .filter(bs -> !"EXPIRED".equals(bs.getStatus()))
                .map(bs -> bs.getSeat().getId())
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<Integer, java.util.List<Seat>> rows = allSeats.stream()
                .filter(s -> s.getGridRow() != null && s.getGridCol() != null)
                .collect(java.util.stream.Collectors.groupingBy(Seat::getGridRow));

        for (java.util.Map.Entry<Integer, java.util.List<Seat>> rowEntry : rows.entrySet()) {
            java.util.List<Seat> seatsInRow = rowEntry.getValue();
            // Check if user selected any seat in this row
            boolean hasSelectionInRow = seatsInRow.stream().anyMatch(s -> selectedSeatIds.contains(s.getId()));
            if (!hasSelectionInRow) continue;

            int maxCol = seatsInRow.stream().mapToInt(Seat::getGridCol).max().orElse(-1);
            if (maxCol < 0) continue;

            // Xây dựng state map: E (Empty), S (Selected), O (Occupied), X (Barrier/Aisle)
            char[] state = new char[maxCol + 1];
            java.util.Arrays.fill(state, 'X');
            for (Seat s : seatsInRow) {
                int col = s.getGridCol();
                if (s.getSeatStatus() != null && !"AVAILABLE".equals(s.getSeatStatus())) {
                    state[col] = 'X';
                } else if (selectedSeatIds.contains(s.getId())) {
                    state[col] = 'S';
                } else if (reservedIds.contains(s.getId())) {
                    state[col] = 'O';
                } else {
                    state[col] = 'E';
                }
                
                // Nếu là SWEETBOX, ô bên phải bị ẩn coi như Barrier (X)
                if ("SWEETBOX".equals(s.getSeatType().getName()) && col + 1 <= maxCol) {
                    state[col + 1] = 'X';
                }
            }

            for (int c = 0; c <= maxCol; c++) {
                if (state[c] == 'E') {
                    boolean leftBarrier = (c == 0) || state[c - 1] != 'E';
                    boolean rightBarrier = (c == maxCol) || state[c + 1] != 'E';

                    if (leftBarrier && rightBarrier) {
                        // Khe hở 1 ghế nằm giữa 2 rào cản. Kiểm tra xem người dùng có TẠO RA khe hở này không.
                        boolean causedByUser = (c > 0 && state[c - 1] == 'S') || (c < maxCol && state[c + 1] == 'S');
                        if (causedByUser) {
                            throw new RuntimeException("Vui lòng không để trống 1 ghế đơn lẻ bên cạnh hoặc sát lối đi.");
                        }
                    }
                }
            }
        }
    }
    
    @Transactional
    public void completePayment(Integer bookingId, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
                
        if ("CONFIRMED".equals(booking.getStatus())) {
            return; // Idempotent: đơn đã xác nhận → không xử lý/trừ tiền/sinh vé lần 2
        }
        if ("EXPIRED".equals(booking.getStatus()) || "CANCELLED".equals(booking.getStatus())) {
            // Đơn đã hết hạn giữ chỗ (ghế đã nhả) hoặc bị huỷ → không thể hoàn tất
            throw new RuntimeException("Đơn đã hết hạn giữ chỗ, vui lòng đặt lại.");
        }
        // Chỉ hoàn tất đơn còn đang giữ ghế (HOLD); ghế phải vẫn thuộc đơn này
        if (!"HOLD".equals(booking.getStatus())) {
            throw new RuntimeException("Trạng thái đơn không hợp lệ để thanh toán.");
        }
        
        // Tích điểm — dùng chung LoyaltyService cho CẢ vé online lẫn vé POS; tính trên số tiền
        // thực trả (finalPrice, đã trừ voucher + làm tròn tiền mặt). Khách null (vãng lai) -> bỏ qua.
        loyaltyService.award(booking.getCustomer(), booking.getFinalPrice(), "BOOKING", booking.getBookingCode());
        
        booking.setStatus("CONFIRMED");
        booking.setPaymentMethod(paymentMethod);
        bookingRepository.save(booking);
        
        // Update seat status + sinh vé QR — gom saveAll thay vì lưu từng bản ghi (giảm round-trip).
        // Fetch kèm seat (+seatType) trong 1 query để dựng nhãn ghế cho email không bị N+1.
        List<BookingSeat> seats = bookingSeatRepository.findAllByBookingIdWithSeat(bookingId);
        List<Ticket> tickets = new java.util.ArrayList<>();
        for (BookingSeat bs : seats) {
            bs.setStatus("SOLD");
            tickets.add(Ticket.builder()
                    .bookingSeat(bs)
                    .qrCode("DEVCINE-T-" + bs.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .isCheckedIn(false)
                    .isAgeVerified(false)
                    .build());
        }
        bookingSeatRepository.saveAll(seats);
        ticketRepository.saveAll(tickets);

        // Ghế đã bán → broadcast real-time cho mọi quầy POS & khách online khóa cứng ghế này
        // (best-effort: lỗi messaging không được làm hỏng giao dịch thanh toán đã hoàn tất)
        if (booking.getShowtime() != null) {
            List<Integer> soldSeatIds = seats.stream()
                    .filter(bs -> bs.getSeat() != null)
                    .map(bs -> bs.getSeat().getId())
                    .toList();
            seatLockService.markSold(booking.getShowtime().getId(), soldSeatIds);
        }

        // Mark voucher as used + tăng lượt dùng của chương trình khuyến mãi
        if (booking.getVoucher() != null) {
            Voucher v = booking.getVoucher();
            v.setIsUsed(true);
            v.setUsedAt(LocalDateTime.now()); // ghi mốc thời điểm sử dụng voucher
            voucherRepository.save(v);
            Promotion promo = v.getPromotion();
            if (promo != null) {
                promo.setUsedCount((promo.getUsedCount() != null ? promo.getUsedCount() : 0) + 1);
                promotionRepository.save(promo);
            }
        }

        // Tạo thông báo "đặt vé thành công" cho khách hàng
        if (booking.getCustomer() != null) {
            String movieTitle = booking.getShowtime() != null && booking.getShowtime().getMovie() != null
                    ? booking.getShowtime().getMovie().getTitle() : "phim";
            notificationService.notifyCustomer(
                    booking.getCustomer().getUserId(),
                    "Đặt vé thành công",
                    "Bạn đã đặt vé xem phim \"" + movieTitle + "\" thành công. Mã đặt vé: " + booking.getBookingCode(),
                    "BOOKING");
        }

        // Gửi vé điện tử (mã QR) qua email — bất đồng bộ, fail-safe (không rollback nếu mail lỗi)
        sendTicketEmail(booking, seats, tickets);
    }

    /**
     * Dựng dữ liệu phẳng từ đơn vừa xác nhận và đẩy sang {@link MailService} gửi vé qua email.
     * Chỉ gửi khi đơn có khách hàng kèm email (đơn POS khách vãng lai bỏ qua).
     */
    private void sendTicketEmail(Booking booking, List<BookingSeat> seats, List<Ticket> tickets) {
        try {
            if (booking.getCustomer() == null || booking.getCustomer().getUser() == null) {
                return;
            }
            User user = booking.getCustomer().getUser();
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                return;
            }

            Showtime showtime = booking.getShowtime();
            Movie movie = showtime != null ? showtime.getMovie() : null;
            Room room = showtime != null ? showtime.getRoom() : null;
            Cinema cinema = room != null ? room.getCinema() : null;

            List<TicketEmailData.SeatLine> seatLines = new java.util.ArrayList<>();
            for (int i = 0; i < seats.size(); i++) {
                BookingSeat bs = seats.get(i);
                Seat seat = bs.getSeat();
                String label = seat.displayLabel();
                seatLines.add(new TicketEmailData.SeatLine(label, bs.getTicketType(), tickets.get(i).getQrCode()));
            }

            List<TicketEmailData.FnbLine> fnbLines = new java.util.ArrayList<>();
            for (BookingFnb bf : bookingFnbRepository.findByBookingIdWithFnb(booking.getId())) {
                fnbLines.add(new TicketEmailData.FnbLine(bf.getFnbItem().getName(), bf.getQuantity()));
            }

            // Tách email theo KÊNH đơn (tin cậy): đơn Online → hiện QR để khách ra rạp quét in vé;
            // đơn POS (kể cả admin/manager bán không-ca) → ẩn QR, chỉ hoá đơn + lời cảm ơn.
            boolean showQr = !"POS".equalsIgnoreCase(booking.getChannel());
            mailService.sendTicketEmail(new TicketEmailData(
                    user.getEmail(),
                    user.getFullName(),
                    booking.getBookingCode(),
                    movie != null ? movie.getTitle() : "Phim",
                    cinema != null ? cinema.getName() : "",
                    room != null ? room.getName() : "",
                    showtime != null ? showtime.getStartTime() : null,
                    booking.getPaymentMethod(),
                    booking.getFinalPrice(),
                    seatLines,
                    fnbLines,
                    showQr));
        } catch (Exception e) {
            // Không để lỗi dựng email ảnh hưởng giao dịch đặt vé đã hoàn tất
            log.error("Lỗi chuẩn bị email vé cho đơn {}: {}", booking.getBookingCode(), e.getMessage(), e);
        }
    }
}

