package com.devcine.backend.service;

import com.devcine.backend.dto.TicketEmailData;
import com.devcine.backend.dto.request.BookingRequestDTO;
import com.devcine.backend.dto.request.FnbSelectionDTO;
import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
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
    private final InventoryService inventoryService;
    private final PricingService pricingService;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final SeatLockService seatLockService;
    private final LoyaltyService loyaltyService;

    @Transactional
    public Booking holdSeats(BookingRequestDTO request) {
        return holdSeats(request, null);
    }

    @Transactional
    public Booking holdSeatsForStaffSchedule(BookingRequestDTO request, StaffSchedule staffSchedule) {
        return holdSeats(request, staffSchedule);
    }

    private Booking holdSeats(BookingRequestDTO request, StaffSchedule staffSchedule) {
        // Khóa ghi bi quan trên suất → tuần tự hóa mọi lệnh giữ ghế cùng suất, chống bán trùng (race)
        Showtime showtime = showtimeRepository.findByIdForUpdate(request.getShowtimeId())
                .orElseThrow(() -> new RuntimeException("Showtime not found"));

        // Chuẩn hoá danh sách ghế kèm loại vé: ưu tiên seatSelections, fallback seatIds (ADULT)
        java.util.Map<Integer, String> ticketTypeBySeat = new java.util.LinkedHashMap<>();
        if (request.getSeatSelections() != null && !request.getSeatSelections().isEmpty()) {
            for (var sel : request.getSeatSelections()) {
                ticketTypeBySeat.put(sel.getSeatId(), pricingService.normalizeAudience(sel.getTicketType()));
            }
        } else if (request.getSeatIds() != null) {
            for (Integer seatId : request.getSeatIds()) {
                ticketTypeBySeat.put(seatId, "ADULT");
            }
        }
        java.util.List<Integer> selectedSeatIds = new java.util.ArrayList<>(ticketTypeBySeat.keySet());

        // Validate số lượng vé: phải có ít nhất 1 ghế và không vượt giới hạn cấu hình (chống phe vé)
        if (selectedSeatIds.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất 1 ghế.");
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

        Booking booking = Booking.builder()
                .customer(customer)
                .showtime(showtime)
                .staffSchedule(staffSchedule)
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
        // Gom 1 query đọc ghế (findAllById) + saveAll thay vì truy vấn/lưu từng ghế (giảm round-trip).
        PricingService.PricingContext priceCtx = pricingService.buildContext(showtime);
        java.util.Map<Integer, Seat> seatMap = new java.util.HashMap<>();
        seatRepository.findByIdInWithSeatType(selectedSeatIds).forEach(s -> seatMap.put(s.getId(), s));
        java.util.List<BookingSeat> bookingSeats = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Integer, String> entry : ticketTypeBySeat.entrySet()) {
            Seat seat = seatMap.get(entry.getKey());
            if (seat == null) throw new RuntimeException("Seat not found");
            String ticketType = entry.getValue();
            BigDecimal seatPrice = pricingService.priceFor(priceCtx, seat.getSeatType(), ticketType);
            bookingSeats.add(BookingSeat.builder()
                    .booking(booking)
                    .seat(seat)
                    .priceSnapshot(seatPrice)
                    .ticketType(ticketType)
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

            // Điều kiện áp dụng nâng cao (đơn tối thiểu / theo phim / đối tượng / lượt dùng)
            if (promotion.getMinOrderValue() != null
                    && totalPrice.compareTo(promotion.getMinOrderValue()) < 0) {
                throw new RuntimeException("Đơn tối thiểu " + promotion.getMinOrderValue().toBigInteger()
                        + "đ để áp dụng mã này.");
            }
            if (promotion.getApplicableMovieId() != null
                    && !promotion.getApplicableMovieId().equals(showtime.getMovie().getId())) {
                throw new RuntimeException("Mã chỉ áp dụng cho phim khác, không dùng được cho suất này.");
            }
            String eligibility = promotion.getCustomerEligibility();
            if ("NEW_CUSTOMER".equalsIgnoreCase(eligibility)
                    && bookingRepository.countConfirmedByCustomer(customer.getUserId()) > 0) {
                throw new RuntimeException("Mã chỉ dành cho khách hàng mới (chưa từng mua vé).");
            }
            // Mã tri ân khách thân thiết: yêu cầu hạng thành viên (theo điểm tích lũy) tối thiểu.
            if (eligibility != null && eligibility.startsWith("TIER_")) {
                String requiredTier = eligibility.substring(5); // SILVER | GOLD | PLATINUM
                int lifetime = customer.getLifetimePoints() != null ? customer.getLifetimePoints() : 0;
                if (loyaltyService.tierRank(loyaltyService.tierFor(lifetime)) < loyaltyService.tierRank(requiredTier)) {
                    throw new RuntimeException("Mã chỉ dành cho khách hàng hạng " + loyaltyService.tierLabelVi(requiredTier) + " trở lên.");
                }
            }
            if (promotion.getUsageLimit() != null && promotion.getUsageLimit() > 0
                    && promotion.getUsedCount() != null
                    && promotion.getUsedCount() >= promotion.getUsageLimit()) {
                throw new RuntimeException("Mã đã hết lượt sử dụng.");
            }

            // Phần tiền được tính giảm (base). Mặc định = cả đơn (ghế + bắp nước).
            // Nếu mã giới hạn số vé → chỉ tính trên tối đa X vé ĐẮT NHẤT (không gồm bắp nước).
            BigDecimal discountBase = totalPrice;
            Integer voucherMaxTickets = promotion.getMaxTicketQuantity();
            if (voucherMaxTickets != null && voucherMaxTickets > 0) {
                discountBase = bookingSeats.stream()
                        .map(BookingSeat::getPriceSnapshot)
                        .sorted(java.util.Comparator.reverseOrder())
                        .limit(voucherMaxTickets)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            BigDecimal discount = BigDecimal.ZERO;
            if ("PERCENTAGE".equalsIgnoreCase(promotion.getDiscountType())) {
                discount = discountBase.multiply(promotion.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else if ("FIXED_AMOUNT".equalsIgnoreCase(promotion.getDiscountType())) {
                // Giảm tiền cố định không vượt quá phần tiền được tính giảm
                discount = promotion.getDiscountValue().min(discountBase);
            }

            // Trần giảm tối đa (capping) — bảo vệ doanh thu với mã giảm %
            BigDecimal maxDiscount = promotion.getMaxDiscountAmount();
            if (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) > 0
                    && discount.compareTo(maxDiscount) > 0) {
                discount = maxDiscount;
            }

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
            voucherRepository.save(v);
            Promotion promo = v.getPromotion();
            if (promo != null) {
                promo.setUsedCount((promo.getUsedCount() != null ? promo.getUsedCount() : 0) + 1);
                promotionRepository.save(promo);
            }
        }

        // Tự trừ tồn kho F&B theo định mức nguyên liệu (BOM) của rạp tương ứng
        if (booking.getShowtime() != null && booking.getShowtime().getRoom() != null
                && booking.getShowtime().getRoom().getCinema() != null) {
            Integer cinemaId = booking.getShowtime().getRoom().getCinema().getId();
            for (BookingFnb bf : bookingFnbRepository.findByBookingIdWithFnb(bookingId)) {
                inventoryService.deductForSale(cinemaId, bf.getFnbItem().getId(), bf.getQuantity(), booking.getStaffSchedule());
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
                String label = seat.getRowChar() + seat.getColNum();
                seatLines.add(new TicketEmailData.SeatLine(label, bs.getTicketType(), tickets.get(i).getQrCode()));
            }

            List<TicketEmailData.FnbLine> fnbLines = new java.util.ArrayList<>();
            for (BookingFnb bf : bookingFnbRepository.findByBookingIdWithFnb(booking.getId())) {
                fnbLines.add(new TicketEmailData.FnbLine(bf.getFnbItem().getName(), bf.getQuantity()));
            }

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
                    fnbLines));
        } catch (Exception e) {
            // Không để lỗi dựng email ảnh hưởng giao dịch đặt vé đã hoàn tất
            log.error("Lỗi chuẩn bị email vé cho đơn {}: {}", booking.getBookingCode(), e.getMessage(), e);
        }
    }
}

