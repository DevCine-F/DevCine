package com.devcine.backend.service;

import com.devcine.backend.dto.request.BookingRequestDTO;
import com.devcine.backend.entity.*;
import com.devcine.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingVoucherConcurrencyTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingSeatRepository bookingSeatRepository;
    @Mock BookingFnbRepository bookingFnbRepository;
    @Mock SeatRepository seatRepository;
    @Mock FnbItemRepository fnbItemRepository;
    @Mock FnbOptionValidator fnbOptionValidator;
    @Mock ShowtimeRepository showtimeRepository;
    @Mock CustomerRepository customerRepository;
    @Mock VoucherRepository voucherRepository;
    @Mock PromotionRepository promotionRepository;
    @Mock TicketRepository ticketRepository;
    @Mock SystemSettingRepository systemSettingRepository;
    @Mock SystemSettingService systemSettingService;
    @Mock NotificationService notificationService;
    @Mock PricingService pricingService;
    @Mock UserRepository userRepository;
    @Mock MailService mailService;
    @Mock SeatLockService seatLockService;
    @Mock LoyaltyService loyaltyService;
    @Mock VoucherService voucherService;
    @Mock PosHoldService posHoldService;
    @Mock SeatLayoutSnapshotService seatLayoutSnapshotService;
    @Mock StringRedisTemplate redisTemplate;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock VoucherHoldLeaseService voucherHoldLeaseService;

    @InjectMocks
    BookingService bookingService;

    private Customer customer;
    private User user;
    private Staff staff;
    private Showtime showtime;
    private Movie movie;
    private Room room;
    private Cinema cinema;
    private Seat seatA1;
    private Seat seatA2;
    private SeatType seatTypeStandard;
    private Promotion promo;
    private Voucher voucher;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "999", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        user = User.builder().id(1).fullName("Nguyen Van A").email("a@example.com").build();
        customer = Customer.builder().user(user).membershipTier("BRONZE").loyaltyPoints(100).build();
        customer.setUserId(1);

        staff = Staff.builder().userId(10).staffCode("ST001").build();

        cinema = Cinema.builder().id(1).name("DevCine Hanoi").build();
        room = Room.builder().id(1).cinema(cinema).name("Room 1").build();
        movie = Movie.builder().id(1).title("Doraemon").durationMins(100).build();

        showtime = Showtime.builder()
                .id(1)
                .movie(movie)
                .room(room)
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusHours(4))
                .status("SCHEDULED")
                .build();

        seatTypeStandard = SeatType.builder().id(1).name("STANDARD").build();
        seatA1 = Seat.builder().id(101).rowChar("A").colNum(1).seatType(seatTypeStandard).room(room).build();
        seatA2 = Seat.builder().id(102).rowChar("A").colNum(2).seatType(seatTypeStandard).room(room).build();

        promo = Promotion.builder()
                .id(1)
                .code("GIAM50K")
                .name("Giảm 50.000đ")
                .discountType("FIXED_AMOUNT")
                .discountValue(new BigDecimal("50000"))
                .minOrderValue(new BigDecimal("100000"))
                .isActive(true)
                .usedCount(0)
                .usageLimit(100)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .build();

        voucher = Voucher.builder()
                .id(10)
                .customer(customer)
                .promotion(promo)
                .isUsed(false)
                .validUntil(LocalDateTime.now().plusDays(5))
                .discountTypeSnapshot("FIXED_AMOUNT")
                .discountValueSnapshot(new BigDecimal("50000"))
                .minOrderValueSnapshot(new BigDecimal("100000"))
                .titleSnapshot("Giảm 50.000đ")
                .build();

        when(systemSettingService.getMaxTicketsPerBooking()).thenReturn(8);
        when(systemSettingService.getBookingLateMinutes()).thenReturn(15);
        when(systemSettingService.getSeatHoldMinutes()).thenReturn(10);
        when(showtimeRepository.findByIdForUpdate(1)).thenReturn(Optional.of(showtime));
        when(seatRepository.findByRoomIdAndIsActiveTrue(1)).thenReturn(List.of(seatA1, seatA2));
        when(pricingService.normalizeAudience(any())).thenReturn("ADULT");
        when(pricingService.priceFor(any(), eq("ADULT"))).thenReturn(new BigDecimal("100000"));
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(voucherRepository.findById(10)).thenReturn(Optional.of(voucher));
        when(promotionRepository.findById(1)).thenReturn(Optional.of(promo));
        when(promotionRepository.incrementUsedCountIfAllowed(1)).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("LỚP 1: Chặn giữ voucher khi voucher đang được giữ bởi một đơn HOLD khác chưa hết hạn")
    void testHoldSeatsRejectsWhenVoucherHeldByOtherBooking() {
        // Giả lập Đơn 1 đang giữ voucher #10
        when(bookingRepository.isVoucherHeldByOtherBooking(eq(10), any(), any(), any()))
                .thenReturn(true);

        BookingRequestDTO posReq = BookingRequestDTO.builder()
                .showtimeId(1)
                .seatIds(List.of(102))
                .customerId(1)
                .voucherId(10)
                .paymentMethod("CASH")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> bookingService.holdSeatsForStaff(posReq, staff));
        assertTrue(ex.getMessage().contains("Mã ưu đãi đang được giữ trong một phiên giao dịch khác"));
    }

    @Test
    @DisplayName("LỚP 1: Cho phép re-hold cập nhật đơn trên chính booking đang giữ (heldBookingId)")
    void testHoldSeatsAllowsReholdForSameBooking() {
        when(voucherService.evaluate(any(), any(), eq(voucher), any(), any(), any()))
                .thenReturn(new VoucherService.VoucherEval(true, null, new BigDecimal("50000")));

        // Khi excludeBookingId = 1001 -> trả về false (không bị đơn KHÁC giữ)
        when(bookingRepository.isVoucherHeldByOtherBooking(eq(10), eq(1001), any(), any()))
                .thenReturn(false);

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingSeatRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        BookingRequestDTO reholdReq = BookingRequestDTO.builder()
                .showtimeId(1)
                .seatIds(List.of(101))
                .customerId(1)
                .voucherId(10)
                .heldBookingId(1001)
                .paymentMethod("VNPAY")
                .build();

        Booking reheld = bookingService.holdSeats(reholdReq);
        assertNotNull(reheld);
        assertEquals(new BigDecimal("50000"), reheld.getFinalPrice());
    }

    @Test
    @DisplayName("LỚP 2: Chặn thanh toán hoàn tất nếu voucher đã bị đơn khác sử dụng trước đó (Atomic Fail-safe)")
    void testCompletePaymentRejectsWhenVoucherAlreadyUsedByOtherBooking() {
        Booking booking = Booking.builder()
                .id(3001)
                .status("HOLD")
                .totalPrice(new BigDecimal("100000"))
                .finalPrice(new BigDecimal("50000"))
                .voucher(voucher)
                .customer(customer)
                .showtime(showtime)
                .bookingCode("BK-3001")
                .build();

        when(bookingRepository.findDetailById(3001)).thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findAllByBookingIdWithSeat(3001))
                .thenReturn(List.of(BookingSeat.builder().id(1).seat(seatA1).booking(booking).build()));

        // Giả lập atomic update thất bại (trả về 0) do voucher đã bị đơn khác đánh dấu isUsed = true
        when(voucherRepository.markVoucherAsUsedIfUnused(eq(10), any()))
                .thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.completePayment(3001, "CASH", null));

        assertTrue(ex.getMessage().contains("Mã ưu đãi đã được sử dụng trong một đơn hàng khác"));
        // Đơn hàng KHÔNG được chuyển thành CONFIRMED
        assertNotEquals("CONFIRMED", booking.getStatus());
    }

    @Test
    @DisplayName("LỚP 2: Thanh toán thành công khi voucher chưa bị ai sử dụng")
    void testCompletePaymentSucceedsWhenVoucherUnused() {
        Booking booking = Booking.builder()
                .id(3002)
                .status("HOLD")
                .totalPrice(new BigDecimal("100000"))
                .finalPrice(new BigDecimal("50000"))
                .voucher(voucher)
                .customer(customer)
                .showtime(showtime)
                .bookingCode("BK-3002")
                .build();

        when(bookingRepository.findDetailById(3002)).thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findAllByBookingIdWithSeat(3002))
                .thenReturn(List.of(BookingSeat.builder().id(1).seat(seatA1).booking(booking).build()));

        // Atomic update thành công (trả về 1)
        when(voucherRepository.markVoucherAsUsedIfUnused(eq(10), any()))
                .thenReturn(1);

        bookingService.completePayment(3002, "VNPAY", "VNP_SUCCESS");

        assertEquals("CONFIRMED", booking.getStatus());
        assertTrue(voucher.getIsUsed());
        assertNotNull(voucher.getUsedAt());
    }

    @Test
    @DisplayName("RACE CONDITION: 2 luồng đồng thời thanh toán -> Đúng 1 luồng thành công, 1 luồng bị chặn hoàn toàn")
    void testConcurrentPaymentsWithAtomicVoucherLock() throws Exception {
        Booking booking1 = Booking.builder()
                .id(4001)
                .status("HOLD")
                .totalPrice(new BigDecimal("100000"))
                .finalPrice(new BigDecimal("50000"))
                .voucher(voucher)
                .customer(customer)
                .showtime(showtime)
                .bookingCode("BK-CONCURRENT-1")
                .build();

        Booking booking2 = Booking.builder()
                .id(4002)
                .status("HOLD")
                .totalPrice(new BigDecimal("100000"))
                .finalPrice(new BigDecimal("50000"))
                .voucher(voucher)
                .customer(customer)
                .showtime(showtime)
                .bookingCode("BK-CONCURRENT-2")
                .build();

        when(bookingRepository.findDetailById(4001)).thenReturn(Optional.of(booking1));
        when(bookingRepository.findDetailById(4002)).thenReturn(Optional.of(booking2));
        when(bookingSeatRepository.findAllByBookingIdWithSeat(4001)).thenReturn(List.of(BookingSeat.builder().id(11).seat(seatA1).booking(booking1).build()));
        when(bookingSeatRepository.findAllByBookingIdWithSeat(4002)).thenReturn(List.of(BookingSeat.builder().id(12).seat(seatA2).booking(booking2).build()));

        // Giả lập Atomic database lock: chỉ luồng đầu tiên đến DB đổi được từ is_used=false -> true
        AtomicBoolean voucherAlreadyUsedInDb = new AtomicBoolean(false);
        when(voucherRepository.markVoucherAsUsedIfUnused(eq(10), any())).thenAnswer(invocation -> {
            boolean acquired = voucherAlreadyUsedInDb.compareAndSet(false, true);
            return acquired ? 1 : 0;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startGate.await();
                bookingService.completePayment(4001, "VNPAY", "REF1");
                successCount.incrementAndGet();
            } catch (Exception e) {
                rejectedCount.incrementAndGet();
            } finally {
                endGate.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startGate.await();
                bookingService.completePayment(4002, "CASH", null);
                successCount.incrementAndGet();
            } catch (Exception e) {
                rejectedCount.incrementAndGet();
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown(); // Kích hoạt 2 luồng cùng lúc
        boolean finished = endGate.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished);
        // ĐẢM BẢO CHỈ CÓ ĐÚNG 1 ĐƠN THÀNH CÔNG VÀ 1 ĐƠN BỊ CHẶN LỖI!
        assertEquals(1, successCount.get(), "Chỉ đúng 1 đơn hàng được hoàn tất thanh toán thành công!");
        assertEquals(1, rejectedCount.get(), "Đơn hàng thứ hai phải bị từ chối do voucher đã bị dùng!");
    }
}
