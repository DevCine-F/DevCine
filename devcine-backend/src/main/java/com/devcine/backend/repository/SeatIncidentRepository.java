package com.devcine.backend.repository;

import com.devcine.backend.entity.SeatIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SeatIncidentRepository extends JpaRepository<SeatIncident, Integer> {

    /**
     * Idempotency (Edge #8): đã tồn tại lần xử lý RELOCATE/CANCEL cho đúng cặp vé + ghế nguồn chưa.
     * Chặn phát trùng voucher khi nhân viên bấm xử lý 2 lần cho cùng một ghế hỏng.
     */
    @Query("SELECT COUNT(si) > 0 FROM SeatIncident si " +
           "WHERE si.booking.id = :bookingId AND si.oldSeat.id = :oldSeatId " +
           "AND si.incidentType IN ('RELOCATE', 'CANCEL')")
    boolean existsActiveForBookingSeat(@Param("bookingId") Integer bookingId,
                                       @Param("oldSeatId") Integer oldSeatId);

    /**
     * Batch version: lấy TẬP hợp oldSeat.id đã được xử lý (RELOCATE/CANCEL) của một đơn —
     * dùng thay vì gọi existsActiveForBookingSeat() N lần trong vòng lặp (tránh N+1).
     */
    @Query("SELECT si.oldSeat.id FROM SeatIncident si " +
           "WHERE si.booking.id = :bookingId " +
           "AND si.incidentType IN ('RELOCATE', 'CANCEL')")
    Set<Integer> findProcessedSeatIdsByBooking(@Param("bookingId") Integer bookingId);

    /**
     * Lịch sử sự cố (cinema-scoped + filter). Param luôn non-null để tránh bẫy null-param Postgres
     * (:cinemaId = null nghĩa là ADMIN xem toàn hệ thống — điều kiện đó được canh ở Service).
     */
    @Query(value = "SELECT si FROM SeatIncident si " +
           "LEFT JOIN FETCH si.booking b " +
           "LEFT JOIN FETCH si.handledBy h LEFT JOIN FETCH h.user " +
           "LEFT JOIN FETCH si.voucher v LEFT JOIN FETCH v.promotion " +
           "WHERE (:cinemaId IS NULL OR si.cinema.id = :cinemaId) " +
           "AND (:type = '' OR si.incidentType = :type) " +
           "AND si.createdAt BETWEEN :from AND :to " +
           "AND (:code = '' OR LOWER(b.bookingCode) LIKE CONCAT('%', LOWER(:code), '%')) " +
           "ORDER BY si.createdAt DESC",
           countQuery = "SELECT COUNT(si) FROM SeatIncident si LEFT JOIN si.booking b " +
           "WHERE (:cinemaId IS NULL OR si.cinema.id = :cinemaId) " +
           "AND (:type = '' OR si.incidentType = :type) " +
           "AND si.createdAt BETWEEN :from AND :to " +
           "AND (:code = '' OR LOWER(b.bookingCode) LIKE CONCAT('%', LOWER(:code), '%'))")
    Page<SeatIncident> search(@Param("cinemaId") Integer cinemaId,
                              @Param("type") String type,
                              @Param("code") String code,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              Pageable pageable);

    @Query("SELECT si FROM SeatIncident si " +
           "LEFT JOIN FETCH si.booking b " +
           "LEFT JOIN FETCH si.handledBy h LEFT JOIN FETCH h.user " +
           "LEFT JOIN FETCH si.voucher v LEFT JOIN FETCH v.promotion " +
           "WHERE si.id = :id")
    Optional<SeatIncident> findDetailById(@Param("id") Integer id);
}
