package com.devcine.backend.repository;

import com.devcine.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // ===== Truy vấn phục vụ Dashboard =====
    // Mọi query dưới đây nhận :cinemaId để lọc theo cơ sở (Booking -> showtime -> room -> cinema).
    // cinemaId = null nghĩa là KHÔNG lọc (toàn hệ thống) và chỉ ADMIN được phép truyền null —
    // ràng buộc đó được canh ở DashboardServiceImpl.resolveCinemaScope(), không phải ở đây.

    @Query("SELECT COALESCE(SUM(b.finalPrice), 0) FROM Booking b WHERE b.status = 'CONFIRMED' "
           + "AND b.createdAt >= :startDate AND b.createdAt <= :endDate "
           + "AND (:cinemaId IS NULL OR b.showtime.room.cinema.id = :cinemaId)")
    BigDecimal sumRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
                                     @Param("cinemaId") Integer cinemaId);

    @Query("SELECT COUNT(bs) FROM BookingSeat bs JOIN bs.booking b WHERE b.status = 'CONFIRMED' "
           + "AND b.createdAt >= :startDate AND b.createdAt <= :endDate "
           + "AND (:cinemaId IS NULL OR b.showtime.room.cinema.id = :cinemaId)")
    long countTicketsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
                                 @Param("cinemaId") Integer cinemaId);

    @Query("SELECT COALESCE(SUM(b.finalPrice), 0) FROM Booking b " +
           "WHERE b.status = 'CONFIRMED' AND b.staffSchedule.id = :staffScheduleId AND b.paymentMethod = :paymentMethod")
    BigDecimal sumConfirmedRevenueByStaffScheduleAndPaymentMethod(@Param("staffScheduleId") Integer staffScheduleId,
                                                                  @Param("paymentMethod") String paymentMethod);

    @Query("SELECT COALESCE(SUM(bs.priceSnapshot), 0) FROM BookingSeat bs JOIN bs.booking b " +
           "WHERE b.status = 'CONFIRMED' AND b.staffSchedule.id = :staffScheduleId")
    BigDecimal sumTicketRevenueByStaffSchedule(@Param("staffScheduleId") Integer staffScheduleId);

    @Query("SELECT COUNT(bs) FROM BookingSeat bs JOIN bs.booking b " +
           "WHERE b.status = 'CONFIRMED' AND b.staffSchedule.id = :staffScheduleId")
    long countTicketsByStaffSchedule(@Param("staffScheduleId") Integer staffScheduleId);

    // Gộp doanh thu theo ngày trong 1 query (thay 7 query trong vòng lặp dashboard)
    @Query("SELECT CAST(b.createdAt AS date), COALESCE(SUM(b.finalPrice), 0) FROM Booking b " +
           "WHERE b.status = 'CONFIRMED' AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
           "AND (:cinemaId IS NULL OR b.showtime.room.cinema.id = :cinemaId) " +
           "GROUP BY CAST(b.createdAt AS date)")
    List<Object[]> sumRevenueGroupedByDay(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
                                          @Param("cinemaId") Integer cinemaId);

    // Gộp số vé theo ngày trong 1 query (thay 7 query trong vòng lặp dashboard)
    @Query("SELECT CAST(b.createdAt AS date), COUNT(bs) FROM BookingSeat bs JOIN bs.booking b " +
           "WHERE b.status = 'CONFIRMED' AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
           "AND (:cinemaId IS NULL OR b.showtime.room.cinema.id = :cinemaId) " +
           "GROUP BY CAST(b.createdAt AS date)")
    List<Object[]> countTicketsGroupedByDay(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
                                            @Param("cinemaId") Integer cinemaId);

    // Top phim theo doanh thu trong khoảng thời gian đã chọn (dashboard).
    // Gom ở cấp Booking và KHÔNG join BookingSeat: join ghế làm mỗi đơn xuất hiện một lần trên mỗi
    // ghế, khiến SUM(finalPrice) bị nhân lên đúng bằng số ghế của đơn. Số vé đếm riêng ở query dưới.
    // (Đừng "sửa" bằng SUM(DISTINCT finalPrice) — hai đơn khác nhau cùng mệnh giá sẽ chỉ được cộng một lần.)
    @Query("SELECT m.id, m.title, COALESCE(SUM(b.finalPrice), 0), m.posterUrl " +
           "FROM Booking b JOIN b.showtime s JOIN s.movie m " +
           "WHERE b.status = 'CONFIRMED' AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
           "AND (:cinemaId IS NULL OR s.room.cinema.id = :cinemaId) " +
           "GROUP BY m.id, m.title, m.posterUrl " +
           "ORDER BY COALESCE(SUM(b.finalPrice), 0) DESC")
    List<Object[]> findTopMoviesByRevenue(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("cinemaId") Integer cinemaId);

    // Số vé bán ra theo từng phim (1 ghế = 1 vé) — ghép với query trên theo movieId ở tầng service
    @Query("SELECT m.id, COUNT(bs) " +
           "FROM BookingSeat bs JOIN bs.booking b JOIN b.showtime s JOIN s.movie m " +
           "WHERE b.status = 'CONFIRMED' AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
           "AND (:cinemaId IS NULL OR s.room.cinema.id = :cinemaId) " +
           "GROUP BY m.id")
    List<Object[]> countTicketsGroupedByMovie(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              @Param("cinemaId") Integer cinemaId);

    // Đơn đặt vé gần nhất trong khoảng đã chọn (JOIN FETCH tránh N+1, phân trang lấy top N)
    @Query("SELECT b FROM Booking b JOIN FETCH b.showtime s JOIN FETCH s.movie m " +
           "LEFT JOIN FETCH b.customer c LEFT JOIN FETCH c.user u " +
           "WHERE b.status = 'CONFIRMED' AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
           "AND (:cinemaId IS NULL OR s.room.cinema.id = :cinemaId) " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findRecentConfirmed(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      @Param("cinemaId") Integer cinemaId,
                                      Pageable pageable);

    // Số vé đã bán theo từng suất trong khoảng (1 query thay vì N truy vấn)
    @Query("SELECT b.showtime.id, COUNT(bs) FROM BookingSeat bs JOIN bs.booking b " +
           "WHERE b.status = 'CONFIRMED' AND b.showtime.startTime >= :start AND b.showtime.startTime <= :end " +
           "AND (:cinemaId IS NULL OR b.showtime.room.cinema.id = :cinemaId) " +
           "GROUP BY b.showtime.id")
    List<Object[]> countSoldSeatsByShowtimeInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                                   @Param("cinemaId") Integer cinemaId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.showtime s JOIN FETCH s.movie " +
           "WHERE b.customer.userId = :customerId ORDER BY b.createdAt DESC")
    List<Booking> findByCustomerIdWithDetails(@Param("customerId") Integer customerId);

    /** Số đơn đã CONFIRMED của khách — dùng xác định "khách mới" cho voucher giới hạn đối tượng. */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.customer.userId = :customerId AND b.status = 'CONFIRMED'")
    long countConfirmedByCustomer(@Param("customerId") Integer customerId);

    /** Khách đã từng mua vé (đơn CONFIRMED) cho 1 phim hay chưa — điều kiện được phép đánh giá. */
    @Query("SELECT COUNT(b) > 0 FROM Booking b JOIN b.showtime s " +
           "WHERE b.customer.userId = :customerId AND s.movie.id = :movieId AND b.status = 'CONFIRMED'")
    boolean hasConfirmedBookingForMovie(@Param("customerId") Integer customerId,
                                        @Param("movieId") Integer movieId);

    // Danh sách hoá đơn cho admin (lọc + phân trang). Param luôn non-null để tránh bẫy null-param Postgres.
    @Query(value = "SELECT b FROM Booking b " +
           "JOIN FETCH b.showtime s JOIN FETCH s.movie m JOIN FETCH s.room r " +
           "LEFT JOIN FETCH b.customer c LEFT JOIN FETCH c.user u " +
           "LEFT JOIN b.staffSchedule ss LEFT JOIN ss.staff st " +
           "WHERE b.createdAt BETWEEN :from AND :to " +
           "AND (:status = '' OR b.status = :status) " +
           "AND (:method = '' OR b.paymentMethod = :method) " +
           "AND (:staffUserId IS NULL OR st.userId = :staffUserId) " +
           "AND (:q = '' OR LOWER(b.bookingCode) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.fullName) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.username) LIKE CONCAT('%', LOWER(:q), '%')) " +
           "ORDER BY b.createdAt DESC",
           countQuery = "SELECT COUNT(b) FROM Booking b LEFT JOIN b.customer c LEFT JOIN c.user u " +
           "LEFT JOIN b.staffSchedule ss LEFT JOIN ss.staff st " +
           "WHERE b.createdAt BETWEEN :from AND :to " +
           "AND (:status = '' OR b.status = :status) " +
           "AND (:method = '' OR b.paymentMethod = :method) " +
           "AND (:staffUserId IS NULL OR st.userId = :staffUserId) " +
           "AND (:q = '' OR LOWER(b.bookingCode) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.fullName) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.username) LIKE CONCAT('%', LOWER(:q), '%'))")
    Page<Booking> searchForAdmin(@Param("q") String q, @Param("status") String status,
                                 @Param("method") String method,
                                 @Param("staffUserId") Integer staffUserId,
                                 @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                 Pageable pageable);

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room JOIN FETCH s.format " +
           "LEFT JOIN FETCH b.customer c LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH b.voucher v LEFT JOIN FETCH v.promotion " +
           "WHERE b.id = :id")
    Optional<Booking> findDetailById(@Param("id") Integer id);

    /** Nạp đơn theo mã (quét QR in vé): fetch suất/phim/phòng/rạp tránh N+1. */
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room r LEFT JOIN FETCH r.cinema " +
           "LEFT JOIN FETCH s.format " +
           "LEFT JOIN FETCH b.customer c LEFT JOIN FETCH c.user " +
           "WHERE b.bookingCode = :code")
    Optional<Booking> findByBookingCodeForPrint(@Param("code") String code);

    @Query("SELECT bs.booking.id, COUNT(bs) FROM BookingSeat bs WHERE bs.booking.id IN :ids GROUP BY bs.booking.id")
    List<Object[]> countSeatsByBookingIds(@Param("ids") List<Integer> ids);

    // ===== Thống kê theo phim (modal chi tiết Quản lý Phim) =====

    /** Doanh thu vé (tổng price_snapshot ghế đã CONFIRMED) của 1 phim. */
    @Query("SELECT COALESCE(SUM(bs.priceSnapshot), 0) FROM BookingSeat bs JOIN bs.booking b JOIN b.showtime s " +
           "WHERE s.movie.id = :movieId AND b.status = 'CONFIRMED'")
    BigDecimal sumTicketRevenueByMovie(@Param("movieId") Integer movieId);

    /** Tổng số vé đã bán (ghế CONFIRMED) của 1 phim. */
    @Query("SELECT COUNT(bs) FROM BookingSeat bs JOIN bs.booking b JOIN b.showtime s " +
           "WHERE s.movie.id = :movieId AND b.status = 'CONFIRMED'")
    long countTicketsByMovie(@Param("movieId") Integer movieId);

    /** Số vé đang GIỮ CHỖ (HOLD, chưa thanh toán) của 1 phim — chặn ngừng chiếu/ẩn khi còn giao dịch dở. */
    @Query("SELECT COUNT(b) FROM Booking b JOIN b.showtime s " +
           "WHERE s.movie.id = :movieId AND b.status = 'HOLD'")
    long countActiveHoldsByMovie(@Param("movieId") Integer movieId);

    /** Số vé bán ra ở các suất ĐÃ diễn ra — tử số cho tỷ lệ lấp đầy. */
    @Query("SELECT COUNT(bs) FROM BookingSeat bs JOIN bs.booking b JOIN b.showtime s " +
           "WHERE s.movie.id = :movieId AND b.status = 'CONFIRMED' AND s.startTime <= :now")
    long countPastTicketsByMovie(@Param("movieId") Integer movieId, @Param("now") LocalDateTime now);

    /** Phân bổ doanh thu/số vé theo hạng ghế (Standard/VIP/Sweetbox...) của 1 phim. */
    @Query("SELECT t.name, COALESCE(SUM(bs.priceSnapshot), 0), COUNT(bs) " +
           "FROM BookingSeat bs JOIN bs.seat se JOIN se.seatType t JOIN bs.booking b JOIN b.showtime s " +
           "WHERE s.movie.id = :movieId AND b.status = 'CONFIRMED' " +
           "GROUP BY t.id, t.name ORDER BY SUM(bs.priceSnapshot) DESC")
    List<Object[]> ticketClassDistributionByMovie(@Param("movieId") Integer movieId);
}
