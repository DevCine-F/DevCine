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
            String seatStatus = seat.getSeatStatus() != null ? seat.getSeatStatus() : "AVAILABLE";
            String status;
            if (!"AVAILABLE".equals(seatStatus)) {
                // Ghế khóa vật lý (MAINTENANCE/LOCKED) → không bán, phủ lên trạng thái runtime để FE disable
                status = seatStatus;
            } else if (soldSeatIds.contains(seat.getId())) {
                status = "SOLD";
            } else if (holdSeatIds.contains(seat.getId())) {
                status = "HOLD";
            } else {
                status = "AVAILABLE";
            }

            // Giá mặc định hiển thị trên sơ đồ = giá Người lớn (ADULT); FE đổi theo priceTable khi chọn loại vé
            return SeatDTO.builder()
                    .seatId(seat.getId())
                    .rowChar(seat.getRowChar())
                    .colNum(seat.getColNum())
                    .seatType(seat.getSeatType().getName())
                    .label(seat.displayLabel())
                    .price(pricingService.priceFor(priceCtx, "ADULT"))
                    .status(status)
                    .seatStatus(seatStatus)
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

        List<SeatDTO> seatDTOs = allSeats.stream().map(seat -> {
            String seatStatus = seat.getSeatStatus() != null ? seat.getSeatStatus() : "AVAILABLE";
            return SeatDTO.builder()
                    .seatId(seat.getId())
                    .rowChar(seat.getRowChar())
                    .colNum(seat.getColNum())
                    .seatType(seat.getSeatType().getName())
                    .label(seat.displayLabel())
                    .custom(Boolean.TRUE.equals(seat.getCustomLabel()))
                    .price(null) // preview phòng (không gắn suất) → không có giá; giá tính khi có Showtime
                    // preview phòng: giữ nguyên trạng thái vật lý (AVAILABLE/MAINTENANCE/LOCKED) để builder hiển thị đúng
                    .status(seatStatus)
                    .seatStatus(seatStatus)
                    .gridRow(seat.getGridRow())
                    .gridCol(seat.getGridCol())
                    .build();
        }).collect(Collectors.toList());

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
        Room room = roomRepository.findById(roomId).orElseThrow();
        SeatType normal = seatTypeRepository.findAll().stream()
                .filter(t -> "NORMAL".equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Chưa cấu hình loại ghế NORMAL"));
                
        List<Seat> existingList = seatRepository.findByRoomId(roomId);
        java.util.Map<String, Seat> existingMap = new java.util.HashMap<>();
        if (existingList != null) {
            for (Seat s : existingList) {
                existingMap.put(s.getRowChar() + "_" + s.getColNum(), s);
            }
        }
        
        List<Seat> seatsToInsert = new java.util.ArrayList<>();
        List<Seat> seatsToUpdate = new java.util.ArrayList<>();
        for (int r = 0; r < rows; r++) {
            String rowChar = String.valueOf((char) ('A' + r));
            for (int c = 0; c < cols; c++) {
                String key = rowChar + "_" + (c + 1);
                Seat seat = existingMap.remove(key);
                boolean isNew = false;
                if (seat == null) {
                    seat = new Seat();
                    seat.setRoom(room);
                    seat.setRowChar(rowChar);
                    seat.setColNum(c + 1);
                    isNew = true;
                }
                seat.setGridRow(r);
                seat.setGridCol(c);
                seat.setSeatType(normal);
                seat.setLabel(rowChar + (c + 1));
                seat.setSeatStatus("AVAILABLE");
                seat.setCustomLabel(false);
                seat.setIsActive(true);
                
                if (isNew) {
                    seatsToInsert.add(seat);
                } else {
                    seatsToUpdate.add(seat);
                }
            }
        }
        
        for (Seat remaining : existingMap.values()) {
            remaining.setIsActive(false);
            seatsToUpdate.add(remaining);
        }
        
        if (!seatsToInsert.isEmpty()) {
            String sqlInsert = "INSERT INTO seats (room_id, row_char, col_num, seat_type_id, is_active, label, custom_label, seat_status, grid_row, grid_col) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.batchUpdate(sqlInsert, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    Seat s = seatsToInsert.get(i);
                    ps.setInt(1, s.getRoom().getId());
                    ps.setString(2, s.getRowChar());
                    ps.setInt(3, s.getColNum());
                    ps.setInt(4, s.getSeatType().getId());
                    ps.setBoolean(5, s.getIsActive());
                    ps.setString(6, s.getLabel());
                    ps.setBoolean(7, s.getCustomLabel());
                    ps.setString(8, s.getSeatStatus());
                    if (s.getGridRow() != null) ps.setInt(9, s.getGridRow()); else ps.setNull(9, java.sql.Types.INTEGER);
                    if (s.getGridCol() != null) ps.setInt(10, s.getGridCol()); else ps.setNull(10, java.sql.Types.INTEGER);
                }
                @Override
                public int getBatchSize() { return seatsToInsert.size(); }
            });
        }
        
        if (!seatsToUpdate.isEmpty()) {
            seatRepository.saveAll(seatsToUpdate);
        }
    }

    @Transactional
    public void saveSeatLayout(Integer roomId, SeatLayoutRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        room.setMatrixRow(request.getMatrixRow());
        room.setMatrixCol(request.getMatrixCol());
        roomRepository.save(room);

        java.util.Map<String, SeatType> seatTypeMap = seatTypeRepository.findAll().stream()
                .collect(Collectors.toMap(SeatType::getName, type -> type));

        List<Seat> existingList = seatRepository.findByRoomId(roomId);
        java.util.Map<String, Seat> existingMap = new java.util.HashMap<>();
        if (existingList != null) {
            for (Seat s : existingList) {
                existingMap.put(s.getRowChar() + "_" + s.getColNum(), s);
            }
        }

        List<Seat> seatsToSave = new java.util.ArrayList<>();
        for (var def : request.getSeats()) {
            String backendType = def.getType().toUpperCase();
            if ("STANDARD".equals(backendType)) backendType = "NORMAL";
            else if ("DOUBLE".equals(backendType)) backendType = "SWEETBOX";

            SeatType seatType = seatTypeMap.get(backendType);
            if (seatType == null) {
                throw new RuntimeException("SeatType not found: " + backendType);
            }

            String label = (def.getLabel() != null && !def.getLabel().isBlank())
                    ? def.getLabel()
                    : (def.getRowChar() + def.getColNum());
            String seatStatus = (def.getStatus() != null && !def.getStatus().isBlank())
                    ? def.getStatus().toUpperCase()
                    : "AVAILABLE";

            String key = def.getRowChar() + "_" + def.getColNum();
            Seat seat = existingMap.remove(key);
            if (seat == null) {
                seat = new Seat();
                seat.setRoom(room);
                seat.setRowChar(def.getRowChar());
                seat.setColNum(def.getColNum());
            }

            seat.setGridRow(def.getGridRow());
            seat.setGridCol(def.getGridCol());
            seat.setSeatType(seatType);
            seat.setLabel(label);
            seat.setSeatStatus(seatStatus);
            seat.setCustomLabel(Boolean.TRUE.equals(def.getCustom()));
            seat.setIsActive(true);

            seatsToSave.add(seat);
        }

        for (Seat remaining : existingMap.values()) {
            remaining.setIsActive(false);
            seatsToSave.add(remaining);
        }

        seatRepository.saveAll(seatsToSave);
    }
}
