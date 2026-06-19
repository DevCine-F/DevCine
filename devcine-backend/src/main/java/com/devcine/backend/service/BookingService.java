package com.devcine.backend.service;

import com.devcine.backend.dto.request.BookingRequestDTO;
import com.devcine.backend.dto.request.FnbSelectionDTO;
import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
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
    private final TicketRepository ticketRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final NotificationService notificationService;
    private final InventoryService inventoryService;

    @Transactional
    public Booking holdSeats(BookingRequestDTO request) {
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new RuntimeException("Showtime not found"));

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElse(null);
        }

        // Validate seats
        List<BookingSeat> existingReservedSeats = bookingSeatRepository.findReservedSeatsByShowtime(request.getShowtimeId());
        for (BookingSeat reserved : existingReservedSeats) {
            if (request.getSeatIds().contains(reserved.getSeat().getId())) {
                // If it's on HOLD but older than 10 minutes, we can override it (pretend it's free)
                if ("HOLD".equals(reserved.getStatus()) && 
                    reserved.getBooking().getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
                    continue; 
                }
                throw new RuntimeException("Seat " + reserved.getSeat().getId() + " is already taken or on hold.");
            }
        }

        Booking booking = Booking.builder()
                .customer(customer)
                .showtime(showtime)
                .bookingCode(UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                .status("HOLD") // Initial status
                .createdAt(LocalDateTime.now())
                .paymentMethod(request.getPaymentMethod())
                .totalPrice(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .build();
                
        bookingRepository.save(booking);

        BigDecimal totalPrice = BigDecimal.ZERO;

        // Process Seats
        if (request.getSeatIds() != null) {
            for (Integer seatId : request.getSeatIds()) {
                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new RuntimeException("Seat not found"));
                BigDecimal seatPrice = seat.getSeatType().getPriceModifier(); // Simplified pricing
                
                BookingSeat bookingSeat = BookingSeat.builder()
                        .booking(booking)
                        .seat(seat)
                        .priceSnapshot(seatPrice)
                        .status("HOLD")
                        .build();
                bookingSeatRepository.save(bookingSeat);
                totalPrice = totalPrice.add(seatPrice);
            }
        }

        // Process F&B
        if (request.getFnbs() != null) {
            for (FnbSelectionDTO fnbDTO : request.getFnbs()) {
                FnbItem item = fnbItemRepository.findById(fnbDTO.getFnbItemId())
                        .orElseThrow(() -> new RuntimeException("F&B Item not found"));
                BigDecimal itemTotal = item.getPrice().multiply(new BigDecimal(fnbDTO.getQuantity()));
                
                BookingFnb bookingFnb = BookingFnb.builder()
                        .booking(booking)
                        .fnbItem(item)
                        .quantity(fnbDTO.getQuantity())
                        .priceSnapshot(item.getPrice())
                        .build();
                bookingFnbRepository.save(bookingFnb);
                totalPrice = totalPrice.add(itemTotal);
            }
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
            BigDecimal discount = BigDecimal.ZERO;
            if ("PERCENTAGE".equalsIgnoreCase(promotion.getDiscountType())) {
                discount = totalPrice.multiply(promotion.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else if ("FIXED_AMOUNT".equalsIgnoreCase(promotion.getDiscountType())) {
                discount = promotion.getDiscountValue();
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
            return; // Already processed
        }
        
        // If paid with wallet, deduct balance
        if ("WALLET".equalsIgnoreCase(paymentMethod)) {
            if (booking.getCustomer() == null) {
                throw new RuntimeException("Guest booking cannot pay with Wallet");
            }
            Wallet wallet = walletRepository.findByCustomerUserId(booking.getCustomer().getUserId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found for customer"));
            
            if (wallet.getBalance().compareTo(booking.getFinalPrice()) < 0) {
                throw new RuntimeException("Insufficient wallet balance");
            }
            
            // Deduct balance
            wallet.setBalance(wallet.getBalance().subtract(booking.getFinalPrice()));
            walletRepository.save(wallet);
            
            // Create Transaction Log
            WalletTransaction wt = WalletTransaction.builder()
                    .wallet(wallet)
                    .type("PAYMENT")
                    .amount(booking.getFinalPrice().negate())
                    .description("Thanh toán đơn vé: " + booking.getBookingCode())
                    .createdAt(LocalDateTime.now())
                    .build();
            walletTransactionRepository.save(wt);
        }

        // Loyalty points and membership tiers update
        if (booking.getCustomer() != null) {
            Customer customer = booking.getCustomer();
            
            BigDecimal pointRate = BigDecimal.valueOf(1000); // Mặc định 1000 VNĐ = 1 điểm
            try {
                SystemSetting setting = systemSettingRepository.findById("LOYALTY_POINT_RATE").orElse(null);
                if (setting != null && setting.getSettingValue() != null) {
                    BigDecimal parsed = new BigDecimal(setting.getSettingValue());
                    if (parsed.compareTo(BigDecimal.ZERO) > 0) {
                        pointRate = parsed;
                    }
                }
            } catch (Exception ex) {
                // Bỏ qua, dùng mặc định
            }

            int earnedPoints = booking.getFinalPrice().divide(pointRate, 0, RoundingMode.DOWN).intValue();
            if (earnedPoints > 0) {
                customer.setLoyaltyPoints(customer.getLoyaltyPoints() + earnedPoints);
                
                // Update membership tier based on points
                int points = customer.getLoyaltyPoints();
                String newTier = "BRONZE";
                if (points >= 10000) {
                    newTier = "PLATINUM";
                } else if (points >= 5000) {
                    newTier = "GOLD";
                } else if (points >= 2000) {
                    newTier = "SILVER";
                }
                
                customer.setMembershipTier(newTier);
                customerRepository.save(customer);
            }
        }
        
        booking.setStatus("CONFIRMED");
        booking.setPaymentMethod(paymentMethod);
        bookingRepository.save(booking);
        
        // Update seat status
        List<BookingSeat> seats = bookingSeatRepository.findAllByBookingId(bookingId);
        for (BookingSeat bs : seats) {
            bs.setStatus("SOLD");
            bookingSeatRepository.save(bs);
            
            // Generate Ticket and QR code
            Ticket ticket = Ticket.builder()
                    .bookingSeat(bs)
                    .qrCode("DEVCINE-T-" + bs.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .isCheckedIn(false)
                    .isAgeVerified(false)
                    .build();
            ticketRepository.save(ticket);
        }
        
        // Mark voucher as used
        if (booking.getVoucher() != null) {
            Voucher v = booking.getVoucher();
            v.setIsUsed(true);
            voucherRepository.save(v);
        }

        // Tự trừ tồn kho F&B theo định mức nguyên liệu (BOM) của rạp tương ứng
        if (booking.getShowtime() != null && booking.getShowtime().getRoom() != null
                && booking.getShowtime().getRoom().getCinema() != null) {
            Integer cinemaId = booking.getShowtime().getRoom().getCinema().getId();
            for (BookingFnb bf : bookingFnbRepository.findByBookingIdWithFnb(bookingId)) {
                inventoryService.deductForSale(cinemaId, bf.getFnbItem().getId(), bf.getQuantity());
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
    }
}

