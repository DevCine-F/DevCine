package com.devcine.backend.service;

import com.devcine.backend.dto.response.SeatDTO;
import com.devcine.backend.dto.response.ShowtimeSeatResponse;
import com.devcine.backend.entity.BookingSeat;
import com.devcine.backend.entity.Seat;
import com.devcine.backend.entity.Showtime;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.repository.SeatRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.devcine.backend.dto.request.SeatLayoutRequest;
import com.devcine.backend.entity.Room;
import com.devcine.backend.entity.SeatType;
import com.devcine.backend.repository.RoomRepository;
import com.devcine.backend.repository.SeatTypeRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final RoomRepository roomRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final PricingService pricingService;
    private final JdbcTemplate jdbcTemplate;

    public ShowtimeSeatResponse getSeatsForShowtime(Integer showtimeId) {
        return getSeatsForShowtime(showtimeId, "ONLINE");
    }

    public ShowtimeSeatResponse getSeatsForShowtime(Integer showtimeId, String channel) {
        boolean online = !"POS".equalsIgnoreCase(channel);
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));

        Integer roomId = showtime.getRoom().getId();
        List<Seat> allSeats = seatRepository.findByRoomIdAndIsActiveTrue(roomId);

        List<BookingSeat> reservedBookingSeats = bookingSeatRepository.findReservedSeatsByShowtime(showtimeId);

        Set<Integer> soldSeatIds = reservedBookingSeats.stream()
                .filter(bs -> "SOLD".equals(bs.getStatus()))
                .map(bs -> bs.getSeat().getId())
                .collect(Collectors.toSet());

        Set<Integer> holdSeatIds = reservedBookingSeats.stream()
                .filter(bs -> "HOLD".equals(bs.getStatus()))
                .map(bs -> bs.getSeat().getId())
                .collect(Collectors.toSet());

        // Tính giá tập trung qua PricingService (nạp ngữ cảnh suất một lần — tránh N+1)
        PricingService.PricingContext priceCtx = pricingService.buildContext(showtime);

        List<SeatDTO> seatDTOs = allSeats.stream().map(seat -> {
            String status = "AVAILABLE";
            if (soldSeatIds.contains(seat.getId())) {
                status = "SOLD";
            } else if (holdSeatIds.contains(seat.getId())) {
                status = "HOLD";
            }

            // Giá mặc định hiển thị trên sơ đồ = giá Người lớn (ADULT); FE đổi theo priceTable khi chọn loại vé
            return SeatDTO.builder()
                    .seatId(seat.getId())
                    .rowChar(seat.getRowChar())
                    .colNum(seat.getColNum())
                    .seatType(seat.getSeatType().getName())
                    .price(pricingService.priceFor(priceCtx, "ADULT"))
                    .status(status)
                    .gridRow(seat.getGridRow())
                    .gridCol(seat.getGridCol())
                    .build();
        }).collect(Collectors.toList());

        return ShowtimeSeatResponse.builder()
                .matrixRow(showtime.getRoom().getMatrixRow() != null ? showtime.getRoom().getMatrixRow() : 9)
                .matrixCol(showtime.getRoom().getMatrixCol() != null ? showtime.getRoom().getMatrixCol() : 10)
                .seats(seatDTOs)
                .audienceLabels(PricingService.audienceLabels(online))
                .priceTable(pricingService.buildPriceTable(priceCtx, seatTypeRepository.findAll(),
                        online ? PricingService.ONLINE_AUDIENCE_TYPES : PricingService.AUDIENCE_TYPES))
                .build();
    }

    public ShowtimeSeatResponse getSeatsForRoom(Integer roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        List<Seat> allSeats = seatRepository.findByRoomIdAndIsActiveTrue(roomId);

        List<SeatDTO> seatDTOs = allSeats.stream().map(seat -> SeatDTO.builder()
                .seatId(seat.getId())
                .rowChar(seat.getRowChar())
                .colNum(seat.getColNum())
                .seatType(seat.getSeatType().getName())
                .price(null) // preview phòng (không gắn suất) → không có giá; giá tính khi có Showtime
                .status("AVAILABLE")
                .gridRow(seat.getGridRow())
                .gridCol(seat.getGridCol())
                .build()).collect(Collectors.toList());

        return ShowtimeSeatResponse.builder()
                .matrixRow(room.getMatrixRow() != null ? room.getMatrixRow() : 9)
                .matrixCol(room.getMatrixCol() != null ? room.getMatrixCol() : 10)
                .seats(seatDTOs)
                .build();
    }

    /**
     * Sinh lưới ghế mặc định cho phòng mới: rows × cols ghế loại NORMAL.
     * rowChar = A..Z (rows ≤ 26), colNum = 1..cols, grid 0-based.
     */
    @Transactional
    public void generateDefaultSeats(Integer roomId, int rows, int cols) {
        SeatType normal = seatTypeRepository.findAll().stream()
                .filter(t -> "NORMAL".equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Chưa cấu hình loại ghế NORMAL trong hệ thống"));
        int seatTypeId = normal.getId();

        List<Object[]> cells = new java.util.ArrayList<>();
        for (int r = 0; r < rows; r++) {
            String rowChar = String.valueOf((char) ('A' + r));
            for (int c = 0; c < cols; c++) {
                cells.add(new Object[]{roomId, rowChar, c + 1, r, c, seatTypeId});
            }
        }

        String sql = "INSERT INTO seats (room_id, row_char, col_num, grid_row, grid_col, seat_type_id, is_active) VALUES (?, ?, ?, ?, ?, ?, true)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] d = cells.get(i);
                ps.setInt(1, (int) d[0]);
                ps.setString(2, (String) d[1]);
                ps.setInt(3, (int) d[2]);
                ps.setInt(4, (int) d[3]);
                ps.setInt(5, (int) d[4]);
                ps.setInt(6, (int) d[5]);
            }

            @Override
            public int getBatchSize() {
                return cells.size();
            }
        });
    }

    @Transactional
    public void saveSeatLayout(Integer roomId, SeatLayoutRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        room.setMatrixRow(request.getMatrixRow());
        room.setMatrixCol(request.getMatrixCol());
        roomRepository.save(room);

        seatRepository.deactivateByRoomId(roomId);

        java.util.Map<String, SeatType> seatTypeMap = seatTypeRepository.findAll().stream()
                .collect(Collectors.toMap(SeatType::getName, type -> type));

        List<com.devcine.backend.dto.request.SeatLayoutRequest.SeatDefinition> seatDefs = request.getSeats();
        String sql = "INSERT INTO seats (room_id, row_char, col_num, grid_row, grid_col, seat_type_id, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                var def = seatDefs.get(i);
                String backendType = def.getType().toUpperCase();
                if ("STANDARD".equals(backendType)) backendType = "NORMAL";
                else if ("DOUBLE".equals(backendType)) backendType = "SWEETBOX";

                String finalBackendType = backendType;
                SeatType seatType = seatTypeMap.get(finalBackendType);
                if (seatType == null) {
                    throw new RuntimeException("SeatType not found: " + finalBackendType);
                }

                ps.setInt(1, roomId);
                ps.setString(2, def.getRowChar());
                ps.setInt(3, def.getColNum());
                ps.setInt(4, def.getGridRow());
                ps.setInt(5, def.getGridCol());
                ps.setInt(6, seatType.getId());
                ps.setBoolean(7, true);
            }

            @Override
            public int getBatchSize() {
                return seatDefs.size();
            }
        });
    }
}
