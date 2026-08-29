package com.devcine.backend.repository;

import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;


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

    // ===== Đóng cửa cụm rạp đột xuất (Emergency Closure) =====

    /**
     * ID các đơn ĐÃ XÁC NHẬN (CONFIRMED) gắn suất chiếu TƯƠNG LAI (startTime >= now) của một cụm rạp.
     * Chỉ SELECT id (nhẹ) — luồng nền tự nạp lại từng đơn trong transaction riêng để hủy/đền bù.
     */
    @Query("SELECT b.id FROM Booking b WHERE b.status = 'CONFIRMED' "
           + "AND b.showtime.startTime >= :now "
           + "AND b.showtime.room.cinema.id = :cinemaId")
    List<Integer> findConfirmedIdsByCinemaAndFutureShowtime(@Param("cinemaId") Integer cinemaId,
                                                            @Param("now") LocalDateTime now);

    /**
     * Dọn rác: đưa các đơn còn ĐANG GIỮ CHỖ / CHỜ THANH TOÁN của suất tương lai thuộc cụm rạp về
     * EXPIRED để nhả ghế (query đếm ghế reserved chỉ tính SOLD/HOLD → đơn EXPIRED không còn khóa chỗ).
     * Chạy ĐỒNG BỘ cùng transaction hủy suất chiếu.
     *
     * @return số đơn đã hết hiệu lực
     */
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("UPDATE Booking b SET b.status = 'EXPIRED' "
           + "WHERE b.status IN ('HOLD', 'PENDING_PAYMENT', 'PAYING') "
           + "AND b.showtime.id IN (SELECT s.id FROM Showtime s WHERE s.startTime >= :now "
           + "AND s.room.id IN (SELECT r.id FROM Room r WHERE r.cinema.id = :cinemaId))")
    int expireActiveHoldsByCinema(@Param("cinemaId") Integer cinemaId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(bs) FROM BookingSeat bs JOIN bs.booking b WHERE b.status = 'CONFIRMED' "
           + "AND b.createdAt >= :startDate AND b.createdAt <= :endDate "
           + "AND (:cinemaId IS NULL OR b.showtime.room.cinema.id = :cinemaId)")
    long countTicketsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
                                 @Param("cinemaId") Integer cinemaId);

    /**
     * Khách LẦN ĐẦU giao dịch tại cơ sở trong khoảng đã chọn.
     *
     * <p>Không phải "số khách khác nhau có đơn trong khoảng" — điều kiện NOT EXISTS loại bỏ những
     * khách đã từng mua ở chính cơ sở đó TRƯỚC mốc bắt đầu. Đơn khách vãng lai tại quầy không có
     * customer nên bị loại: không định danh được thì không đếm là khách mới.</p>
     */
    @Query("SELECT COUNT(DISTINCT b.customer.userId) FROM Booking b "
           + "WHERE b.status = 'CONFIRMED' AND b.customer IS NOT NULL "
           + "AND b.createdAt >= :startDate AND b.createdAt <= :endDate "
           + "AND (:cinemaId IS NULL OR b.showtime.room.cinema.id = :cinemaId) "
           + "AND NOT EXISTS (SELECT 1 FROM Booking pb "
           + "                WHERE pb.status = 'CONFIRMED' "
           + "                  AND pb.customer.userId = b.customer.userId "
           + "                  AND pb.createdAt < :startDate "
           + "                  AND (:cinemaId IS NULL OR pb.showtime.room.cinema.id = :cinemaId))")
    long countNewCustomersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
                                      @Param("cinemaId") Integer cinemaId);

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

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.showtime s JOIN FETCH s.movie m " +
           "LEFT JOIN FETCH s.room r LEFT JOIN FETCH r.cinema c " +
           "LEFT JOIN FETCH s.format f " +
           "WHERE b.customer.userId = :customerId ORDER BY b.createdAt DESC")
    List<Booking> findByCustomerIdWithDetails(@Param("customerId") Integer customerId);

    @Query("SELECT bs FROM BookingSeat bs JOIN FETCH bs.seat s LEFT JOIN FETCH s.seatType WHERE bs.booking.id IN :bookingIds")
    List<BookingSeat> findAllSeatsByBookingIds(@Param("bookingIds") List<Integer> bookingIds);

    @Query("SELECT t FROM Ticket t WHERE t.bookingSeat.booking.id IN :bookingIds")
    List<com.devcine.backend.entity.Ticket> findAllTicketsByBookingIds(@Param("bookingIds") List<Integer> bookingIds);

    @Query("SELECT bf FROM BookingFnb bf JOIN FETCH bf.fnbItem WHERE bf.booking.id IN :bookingIds")
    List<com.devcine.backend.entity.BookingFnb> findAllFnbsByBookingIds(@Param("bookingIds") List<Integer> bookingIds);

    /** Số đơn đã CONFIRMED của khách — dùng xác định "khách mới" cho voucher giới hạn đối tượng. */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.customer.userId = :customerId AND b.status = 'CONFIRMED'")
    long countConfirmedByCustomer(@Param("customerId") Integer customerId);

    /** Tổng chi tiêu và số đơn vé CONFIRMED theo danh sách customerIds (O(1) batch query). */
    @Query("SELECT b.customer.userId, COALESCE(SUM(b.finalPrice), 0), COUNT(b) " +
           "FROM Booking b WHERE b.status = 'CONFIRMED' AND b.customer.userId IN :customerIds " +
           "GROUP BY b.customer.userId")
    List<Object[]> aggregateSpentAndOrderCountByCustomerIds(@Param("customerIds") List<Integer> customerIds);

    /**
     * Batch version: trả tập hợp customerId ĐÃ có ít nhất 1 đơn CONFIRMED —
     * dùng thay vì gọi countConfirmedByCustomer() N lần trong sendCampaignEmails() (tránh N+1).
     */
    @Query("SELECT DISTINCT b.customer.userId FROM Booking b WHERE b.status = 'CONFIRMED' AND b.customer IS NOT NULL")
    Set<Integer> findCustomerIdsWithConfirmedBookings();

    /** Khách đã từng mua vé (đơn CONFIRMED) cho 1 phim hay chưa — điều kiện được phép đánh giá. */
    @Query("SELECT COUNT(b) > 0 FROM Booking b JOIN b.showtime s " +
           "WHERE b.customer.userId = :customerId AND s.movie.id = :movieId AND b.status = 'CONFIRMED'")
    boolean hasConfirmedBookingForMovie(@Param("customerId") Integer customerId,
                                        @Param("movieId") Integer movieId);

    // Danh sách hoá đơn cho admin (lọc + phân trang theo Cụm rạp). Param luôn non-null để tránh bẫy null-param Postgres.
    @Query(value = "SELECT b FROM Booking b " +
           "JOIN FETCH b.showtime s JOIN FETCH s.movie m JOIN FETCH s.room r " +
           "LEFT JOIN FETCH b.customer c LEFT JOIN FETCH c.user u " +
           "LEFT JOIN b.soldBy st " +
           "WHERE b.createdAt BETWEEN :from AND :to " +
           "AND (:status = '' OR b.status = :status) " +
           "AND (:method = '' OR b.paymentMethod = :method) " +
           "AND (:cinemaId IS NULL OR r.cinema.id = :cinemaId) " +
           "AND (:q = '' OR LOWER(b.bookingCode) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.fullName) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.username) LIKE CONCAT('%', LOWER(:q), '%')) " +
           "AND (:hasFnb = '' " +
           "     OR (:hasFnb = 'YES' AND EXISTS (SELECT 1 FROM BookingFnb bf WHERE bf.booking = b)) " +
           "     OR (:hasFnb = 'NO' AND NOT EXISTS (SELECT 1 FROM BookingFnb bf WHERE bf.booking = b))) " +
           "ORDER BY b.createdAt DESC",
           countQuery = "SELECT COUNT(b) FROM Booking b JOIN b.showtime s JOIN s.room r " +
           "LEFT JOIN b.customer c LEFT JOIN c.user u " +
           "WHERE b.createdAt BETWEEN :from AND :to " +
           "AND (:status = '' OR b.status = :status) " +
           "AND (:method = '' OR b.paymentMethod = :method) " +
           "AND (:cinemaId IS NULL OR r.cinema.id = :cinemaId) " +
           "AND (:q = '' OR LOWER(b.bookingCode) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.fullName) LIKE CONCAT('%', LOWER(:q), '%') " +
           "     OR LOWER(u.username) LIKE CONCAT('%', LOWER(:q), '%')) " +
           "AND (:hasFnb = '' " +
           "     OR (:hasFnb = 'YES' AND EXISTS (SELECT 1 FROM BookingFnb bf WHERE bf.booking = b)) " +
           "     OR (:hasFnb = 'NO' AND NOT EXISTS (SELECT 1 FROM BookingFnb bf WHERE bf.booking = b)))")
    Page<Booking> searchForAdmin(@Param("q") String q, @Param("status") String status,
                                 @Param("method") String method,
                                 @Param("cinemaId") Integer cinemaId,
                                 @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                 @Param("hasFnb") String hasFnb,
                                 Pageable pageable);

    @Query("SELECT b FROM Booking b " +
           "LEFT JOIN FETCH b.showtime s " +
           "LEFT JOIN FETCH s.movie " +
           "LEFT JOIN FETCH s.room r " +
           "LEFT JOIN FETCH r.cinema " +
           "LEFT JOIN FETCH s.format " +
           "LEFT JOIN FETCH b.customer c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH b.voucher v " +
           "LEFT JOIN FETCH v.promotion " +
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

    /** Lấy map số vé đã bán của danh sách phim (giúp tránh N+1). */
    @Query("SELECT s.movie.id, COUNT(bs) FROM BookingSeat bs JOIN bs.booking b JOIN b.showtime s " +
           "WHERE s.movie.id IN :movieIds AND b.status = 'CONFIRMED' GROUP BY s.movie.id")
    List<Object[]> countTicketsByMovieIds(@Param("movieIds") List<Integer> movieIds);

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

    // ===== Hold Order & Pending Orders Management =====

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.posTerminalId = :posTerminalId AND (b.status = 'PENDING_PAYMENT' OR b.status = 'PAYING')")
    long countPendingByPosTerminalId(@Param("posTerminalId") String posTerminalId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room LEFT JOIN FETCH b.customer c LEFT JOIN FETCH c.user WHERE b.posTerminalId = :posTerminalId AND (b.status = 'PENDING_PAYMENT' OR b.status = 'PAYING') ORDER BY b.createdAt DESC")
    List<Booking> findPendingByPosTerminalId(@Param("posTerminalId") String posTerminalId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithPessimisticLock(@Param("id") Integer id);

    @Query("SELECT b FROM Booking b WHERE (b.status = 'PENDING_PAYMENT' OR b.status = 'PAYING' OR b.status = 'HOLD') AND b.expiresAt < :now")
    List<Booking> findExpiredHolds(@Param("now") LocalDateTime now);

    /**
     * Tra cứu đơn theo SĐT khách (xử lý sự cố tại quầy). Chỉ đơn CONFIRMED, mới nhất trước.
     * Fetch suất/phim/phòng/rạp + khách để dựng ngữ cảnh sự cố không N+1.
     *
     * <p><b>Nghiệp vụ:</b> Chỉ tra cứu suất còn trong cửa sổ xử lý sự cố ({@code startTime >= cutoff}
     * — thường = now - 2h). Suất chiếu kết thúc > 2 giờ không thể đổi ghế/hủy chỗ nữa.</p>
     */
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room r LEFT JOIN FETCH r.cinema " +
           "LEFT JOIN FETCH s.format " +
           "JOIN FETCH b.customer c JOIN FETCH c.user u " +
           "WHERE u.phone = :phone AND b.status = 'CONFIRMED' " +
           "AND s.startTime >= :cutoff " +
           "AND s.endTime > :now " +
           "ORDER BY s.startTime DESC, b.createdAt DESC, b.id DESC")
    List<Booking> findConfirmedByCustomerPhone(@Param("phone") String phone,
                                               @Param("cutoff") LocalDateTime cutoff,
                                               @Param("now") LocalDateTime now,
                                               Pageable pageable);
}
