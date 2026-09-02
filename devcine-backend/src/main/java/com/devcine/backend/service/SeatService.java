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
    private final SeatLayoutSnapshotService seatLayoutSnapshotService;
    private final JdbcTemplate jdbcTemplate;

    public ShowtimeSeatResponse getSeatsForShowtime(Integer showtimeId) {
        return getSeatsForShowtime(showtimeId, "ONLINE");
    }

    public ShowtimeSeatResponse getSeatsForShowtime(Integer showtimeId, String channel) {
        boolean online = !"POS".equalsIgnoreCase(channel);
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));

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
        List<SeatType> allSeatTypes = seatTypeRepository.findAll();
        java.util.Map<String, SeatType> seatTypeByName = allSeatTypes.stream()
                .collect(Collectors.toMap(SeatType::getName, s -> s, (a, b) -> a));

        String layoutJson = showtime.getLayoutData();
        List<SeatDTO> seatDTOs;
        int matrixRow;
        int matrixCol;

        if (layoutJson != null && !layoutJson.isBlank()) {
            // ===== NGUỒN CHUẨN: đọc KHUNG từ snapshot đông cứng của suất, overlay TRẠNG THÁI live =====
            com.devcine.backend.dto.response.SeatLayoutSnapshot snap = seatLayoutSnapshotService.parse(layoutJson);
            matrixRow = snap.getMatrixRow();
            matrixCol = snap.getMatrixCol();

            // Trạng thái vật lý (MAINTENANCE/LOCKED) LUÔN lấy live theo seatId → bảo trì sau khi tạo suất vẫn phản ánh.
            List<Integer> seatIds = snap.getCells().stream()
                    .filter(c -> "SEAT".equalsIgnoreCase(c.getKind()) && c.getSeatId() != null)
                    .map(com.devcine.backend.dto.response.SeatLayoutSnapshot.Cell::getSeatId)
                    .collect(Collectors.toList());
            java.util.Map<Integer, String> physStatusById = new java.util.HashMap<>();
            if (!seatIds.isEmpty()) {
                for (Seat s : seatRepository.findByIdInWithSeatType(seatIds)) {
                    physStatusById.put(s.getId(), s.getSeatStatus() != null ? s.getSeatStatus() : "AVAILABLE");
                }
            }

            seatDTOs = new java.util.ArrayList<>();
            for (var cell : snap.getCells()) {
                // Lối đi (AISLE): trả tường minh để FE vẽ khung trọn vẹn (không tự đoán), không bán/không giá.
                if (!"SEAT".equalsIgnoreCase(cell.getKind())) {
                    seatDTOs.add(SeatDTO.builder()
                            .kind("AISLE")
                            .span(cell.getSpan() > 0 ? cell.getSpan() : 1)
                            .gridRow(cell.getGridRow())
                            .gridCol(cell.getGridCol())
                            .build());
                    continue;
                }

                String seatStatus = physStatusById.getOrDefault(cell.getSeatId(), "AVAILABLE");
                String status = seatStatusToRuntime(seatStatus, cell.getSeatId(), soldSeatIds, holdSeatIds);

                String rowChar = cell.getRowChar();
                Integer colNum = cell.getColNum();
                if ((rowChar == null || rowChar.isBlank()) && cell.getLabel() != null) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("^([A-Za-z]+)(\\d+)$").matcher(cell.getLabel());
                    if (m.find()) {
                        rowChar = m.group(1).toUpperCase();
                        try { colNum = Integer.parseInt(m.group(2)); } catch (Exception ignored) {}
                    }
                }
                if (rowChar == null || rowChar.isBlank()) {
                    rowChar = String.valueOf((char) ('A' + cell.getGridRow()));
                }
                if (colNum == null) {
                    colNum = cell.getGridCol() + 1;
                }

                SeatType cellSeatType = seatTypeByName.get(cell.getType());
                seatDTOs.add(SeatDTO.builder()
                        .seatId(cell.getSeatId())
                        .rowChar(rowChar)
                        .colNum(colNum)
                        .seatType(cell.getType())
                        .kind("SEAT")
                        .span(cell.getSpan() > 0 ? cell.getSpan() : 1)
                        .label(cell.getLabel())
                        .price(pricingService.priceFor(priceCtx, cellSeatType, "ADULT"))
                        .status(status)
                        .seatStatus(seatStatus)
                        .gridRow(cell.getGridRow())
                        .gridCol(cell.getGridCol())
                        .build());
            }
        } else {
            // ===== FALLBACK: suất cũ chưa có snapshot → đọc toàn bộ ô (kể cả lối đi) từ DB =====
            Integer roomId = showtime.getRoom().getId();
            List<Seat> allSeats = seatRepository.findLayoutByRoomId(roomId);
            matrixRow = showtime.getRoom().getMatrixRow() != null ? showtime.getRoom().getMatrixRow() : 9;
            matrixCol = showtime.getRoom().getMatrixCol() != null ? showtime.getRoom().getMatrixCol() : 10;

            seatDTOs = allSeats.stream().map(seat -> {
                if (!seat.isSeatCell()) {
                    return SeatDTO.builder()
                            .kind("AISLE")
                            .span(1)
                            .gridRow(seat.getGridRow())
                            .gridCol(seat.getGridCol())
                            .build();
                }

                String seatStatus = seat.getSeatStatus() != null ? seat.getSeatStatus() : "AVAILABLE";
                String status = seatStatusToRuntime(seatStatus, seat.getId(), soldSeatIds, holdSeatIds);
                String typeName = seat.getSeatType() != null ? seat.getSeatType().getName() : "NORMAL";
                int span = "SWEETBOX".equalsIgnoreCase(typeName) ? 2 : 1;

                return SeatDTO.builder()
                        .seatId(seat.getId())
                        .rowChar(seat.getRowChar())
                        .colNum(seat.getColNum())
                        .seatType(typeName)
                        .kind("SEAT")
                        .span(span)
                        .label(seat.displayLabel())
                        .price(pricingService.priceFor(priceCtx, seat.getSeatType(), "ADULT"))
                        .status(status)
                        .seatStatus(seatStatus)
                        .gridRow(seat.getGridRow())
                        .gridCol(seat.getGridCol())
                        .build();
            }).collect(Collectors.toList());
        }

        return ShowtimeSeatResponse.builder()
                .matrixRow(matrixRow)
                .matrixCol(matrixCol)
                .seats(seatDTOs)
                .audienceLabels(PricingService.audienceLabels(online))
                .priceTable(pricingService.buildPriceTable(priceCtx, allSeatTypes,
                        online ? PricingService.ONLINE_AUDIENCE_TYPES : PricingService.AUDIENCE_TYPES))
                .build();

    }

    /**
     * Suy trạng thái runtime hiển thị: ưu tiên khóa vật lý (MAINTENANCE/LOCKED) → SOLD → HOLD → AVAILABLE.
     * seatStatus là trạng thái vật lý LIVE (đã lấy theo seatId), sold/hold từ booking_seats.
     */
    private String seatStatusToRuntime(String seatStatus, Integer seatId,
                                       Set<Integer> soldSeatIds, Set<Integer> holdSeatIds) {
        if (seatStatus != null && !"AVAILABLE".equals(seatStatus)) return seatStatus;
        if (soldSeatIds.contains(seatId)) return "SOLD";
        if (holdSeatIds.contains(seatId)) return "HOLD";
        return "AVAILABLE";
    }

    public ShowtimeSeatResponse getSeatsForRoom(Integer roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // Đọc TOÀN BỘ ô gồm cả lối đi (AISLE) để builder có khung trọn vẹn, không phải tự đoán.
        List<Seat> allSeats = seatRepository.findLayoutByRoomId(roomId);

        List<SeatDTO> seatDTOs = allSeats.stream().map(seat -> {
            String seatStatus = seat.getSeatStatus() != null ? seat.getSeatStatus() : "AVAILABLE";
            String kind = seat.getCellKind() != null ? seat.getCellKind() : "SEAT";
            return SeatDTO.builder()
                    .seatId(seat.getId())
                    .rowChar(seat.getRowChar())
                    .colNum(seat.getColNum())
                    .seatType(seat.getSeatType().getName())
                    .kind(kind)
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

        // Kiểm tra phòng đã phát sinh vé đặt ở bất kỳ suất nào chưa → trả FE để khóa Trình thiết kế.
        boolean hasBookings = !bookingSeatRepository.findShowtimeIdsWithReservedSeatsByRoom(roomId).isEmpty();

        return ShowtimeSeatResponse.builder()
                .matrixRow(room.getMatrixRow() != null ? room.getMatrixRow() : 9)
                .matrixCol(room.getMatrixCol() != null ? room.getMatrixCol() : 10)
                .seats(seatDTOs)
                .hasBookings(hasBookings)
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
        if (room.getCinema() != null) {
            com.devcine.backend.util.SecurityUtils.assertCinemaAccess(room.getCinema().getId());
        }

        // ===== GUARD: Chặn sửa sơ đồ nếu phòng đã phát sinh vé đặt (SOLD/HOLD) =====
        // Mục tiêu: bảo vệ tính nhất quán dữ liệu – không được dịch ghế/đổi vị trí khi
        // khách đã cầm vé. Trường hợp ghế hỏng → dùng luồng Xử lý sự cố ghế (/admin/incidents).
        boolean hasBookings = !bookingSeatRepository.findShowtimeIdsWithReservedSeatsByRoom(roomId).isEmpty();
        if (hasBookings) {
            throw new IllegalArgumentException(
                "Phòng chiếu đã phát sinh vé đặt, không thể chỉnh sửa sơ đồ ghế. " +
                "Nếu có sự cố ghế bị hỏng, vui lòng sử dụng tính năng Xử lý sự cố ghế."
            );
        }

        room.setMatrixRow(request.getMatrixRow());
        room.setMatrixCol(request.getMatrixCol());
        roomRepository.save(room);

        // Khung do SERVER ép: ghế ngoài matrixRow×matrixCol là "ma" → bị deactivate, không lưu active.
        Integer matrixRow = request.getMatrixRow();
        Integer matrixCol = request.getMatrixCol();

        java.util.Map<String, SeatType> seatTypeMap = seatTypeRepository.findAll().stream()
                .collect(Collectors.toMap(SeatType::getName, type -> type));
        // Loại ghế placeholder cho ô lối đi (cột seat_type_id NOT NULL; cell_kind='AISLE' nên bị bỏ qua khắp nơi)
        SeatType aislePlaceholder = seatTypeMap.get("NORMAL");
        if (aislePlaceholder == null) {
            throw new RuntimeException("Chưa cấu hình loại ghế NORMAL (bắt buộc để lưu sơ đồ).");
        }

        List<Seat> existingList = seatRepository.findByRoomId(roomId);
        // Định danh theo VỊ TRÍ LƯỚI (gridRow_gridCol) thay vì rowChar_colNum: lối đi không có
        // rowChar/colNum thật, và label thủ công có thể suy ra cùng rowChar/colNum gây trùng khóa.
        //
        // ⚠ PHẢI gom thành DANH SÁCH, không phải Map<String, Seat>. Bản Map cũ ghi đè khi trùng
        // khoá nên các bản sinh đôi bị rơi khỏi map, không bao giờ lọt vào nhánh deactivate cuối
        // hàm → chúng ở lại is_active=true vĩnh viễn và ĐẺ THÊM mỗi lần lưu sơ đồ. Đó chính là
        // nguồn gốc ghế "ma" trùng toạ độ. Nay giữ mọi bản: 1 bản sống sót, phần còn lại bị tắt.
        java.util.Map<String, List<Seat>> existingMap = new java.util.HashMap<>();
        if (existingList != null) {
            for (Seat s : existingList) {
                if (s.getGridRow() != null && s.getGridCol() != null) {
                    existingMap.computeIfAbsent(s.getGridRow() + "_" + s.getGridCol(), k -> new java.util.ArrayList<>())
                            .add(s);
                }
            }
        }

        // Toạ độ có ghế THẬT trong sơ đồ mới — dùng để loại bản SWEETBOX (rộng 2 cột) sẽ đè lên
        // ghế ở cột kế bên. Ghế đôi bị đè khiến renderer nuốt mất ô cột+1 → hàng thiếu ghế.
        java.util.Set<String> seatDefCoords = new java.util.HashSet<>();
        for (var def : request.getSeats()) {
            boolean aisleDef = "AISLE".equalsIgnoreCase(def.getKind()) || "aisle".equalsIgnoreCase(def.getType());
            if (!aisleDef && def.getGridRow() != null && def.getGridCol() != null) {
                seatDefCoords.add(def.getGridRow() + "_" + def.getGridCol());
            }
        }

        List<Seat> seatsToSave = new java.util.ArrayList<>();
        for (var def : request.getSeats()) {
            // Chốt chặn khung: ô ngoài matrixRow×matrixCol không được lưu active. KHÔNG remove khỏi
            // existingMap → nó rơi vào nhánh deactivate ở cuối (dọn sạch ghế "ma" của layout cũ).
            if (def.getGridRow() != null && def.getGridCol() != null && matrixRow != null && matrixCol != null
                    && (def.getGridRow() >= matrixRow || def.getGridCol() >= matrixCol)) {
                continue;
            }

            boolean isAisle = "AISLE".equalsIgnoreCase(def.getKind())
                    || "aisle".equalsIgnoreCase(def.getType());

            String key = def.getGridRow() + "_" + def.getGridCol();
            List<Seat> bucket = existingMap.remove(key);
            Seat seat = null;
            if (bucket != null && !bucket.isEmpty()) {
                boolean nextColTaken = seatDefCoords.contains(def.getGridRow() + "_" + (def.getGridCol() + 1));
                seat = pickSurvivor(bucket, nextColTaken);
                // Mọi bản sinh đôi còn lại tại đúng ô này → tắt ngay (không xoá cứng, vé cũ vẫn tra được).
                for (Seat twin : bucket) {
                    if (twin != seat) {
                        twin.setIsActive(false);
                        seatsToSave.add(twin);
                    }
                }
            }
            if (seat == null) {
                seat = new Seat();
                seat.setRoom(room);
            }
            seat.setGridRow(def.getGridRow());
            seat.setGridCol(def.getGridCol());
            seat.setIsActive(true);

            if (isAisle) {
                // Ô lối đi: không phải ghế. Điền placeholder cho các cột NOT NULL; cell_kind='AISLE'.
                seat.setCellKind("AISLE");
                seat.setSeatType(aislePlaceholder);
                seat.setSeatStatus("AVAILABLE");
                seat.setCustomLabel(false);
                seat.setLabel(null);
                // row_char/col_num NOT NULL → suy từ vị trí lưới (chỉ để thỏa ràng buộc, không dùng)
                seat.setRowChar(def.getRowChar() != null && !def.getRowChar().isBlank()
                        ? def.getRowChar() : String.valueOf((char) ('A' + (def.getGridRow() != null ? def.getGridRow() : 0))));
                seat.setColNum(def.getColNum() != null ? def.getColNum()
                        : (def.getGridCol() != null ? def.getGridCol() + 1 : 1));
                seatsToSave.add(seat);
                continue;
            }

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
            // Chuẩn hóa trạng thái vật lý: chỉ chấp nhận MAINTENANCE/LOCKED; giá trị rác (vd "ACTIVE",
            // "ONLINE"...) hoặc rỗng → AVAILABLE, để không bị vẽ nhầm thành ghế khóa.
            String rawStatus = (def.getStatus() != null && !def.getStatus().isBlank())
                    ? def.getStatus().toUpperCase() : "AVAILABLE";
            String seatStatus = ("MAINTENANCE".equals(rawStatus) || "LOCKED".equals(rawStatus))
                    ? rawStatus : "AVAILABLE";

            seat.setCellKind("SEAT");
            seat.setRowChar(def.getRowChar());
            seat.setColNum(def.getColNum());
            seat.setSeatType(seatType);
            seat.setLabel(label);
            seat.setSeatStatus(seatStatus);
            seat.setCustomLabel(Boolean.TRUE.equals(def.getCustom()));

            seatsToSave.add(seat);
        }

        // Ô không còn trong sơ đồ mới (kể cả ghế ngoài khung): tắt TOÀN BỘ bản ghi tại ô đó.
        for (List<Seat> remaining : existingMap.values()) {
            for (Seat s : remaining) {
                s.setIsActive(false);
                seatsToSave.add(s);
            }
        }

        seatRepository.saveAll(seatsToSave);

        // Sửa phòng phải LAN sang các suất đang mở bán → dựng lại snapshot khung sơ đồ cho chúng.
        resyncActiveShowtimeSnapshots(roomId);
    }

    /**
     * Re-đồng bộ snapshot sơ đồ (layout_data) cho các suất CHƯA KẾT THÚC của phòng sau khi admin
     * sửa sơ đồ ghế. Giữ triết lý "đông cứng theo suất" cho suất đã phát sinh giao dịch:
     * BỎ QUA mọi suất đang có ghế GIỮ/BÁN (SOLD/HOLD) để không dịch ghế khách đang giữ/đã mua;
     * suất đã qua giờ hoặc đã huỷ cũng không đụng tới. Chỉ suất đang mở bán và CHƯA có giao dịch
     * mới nhận khung sơ đồ mới → khớp với trình thiết kế phòng.
     */
    private int resyncActiveShowtimeSnapshots(Integer roomId) {
        List<Showtime> active = showtimeRepository.findActiveByRoomId(roomId, java.time.LocalDateTime.now());
        if (active.isEmpty()) return 0;

        Set<Integer> reservedShowtimeIds = new java.util.HashSet<>(
                bookingSeatRepository.findShowtimeIdsWithReservedSeatsByRoom(roomId));

        String freshSnapshot = seatLayoutSnapshotService.buildSnapshotJson(roomId);
        List<Showtime> toUpdate = new java.util.ArrayList<>();
        for (Showtime st : active) {
            if (reservedShowtimeIds.contains(st.getId())) continue; // có ghế GIỮ/BÁN → giữ snapshot cũ
            st.setLayoutData(freshSnapshot);
            toUpdate.add(st);
        }
        if (!toUpdate.isEmpty()) showtimeRepository.saveAll(toUpdate);
        return toUpdate.size();
    }

    /**
     * Chọn bản ghi ĐƯỢC GIỮ LẠI trong một ô có nhiều ghế trùng toạ độ. Thứ tự ưu tiên:
     * (1) có label thật — bản rác sinh ra do lỗi trùng thường để label = null;
     * (2) hình học hợp lệ — loại bản SWEETBOX (rộng 2 cột) khi cột kế bên đã có ghế thật,
     *     vì renderer bỏ qua ô bị ghế đôi che nên hàng sẽ mất hẳn một ghế;
     * (3) id nhỏ nhất — chốt tất định để hai lần chạy cho cùng kết quả.
     *
     * CỐ Ý KHÔNG xét số vé đã bán: ghế bị loại chỉ bị đặt is_active=false chứ không xoá, dòng
     * seats vẫn còn nên booking_seats cũ vẫn tra ngược được. Ưu tiên theo vé từng khiến bản rác
     * (label null, SWEETBOX chồng lấn) thắng bản chuẩn và làm hỏng khung sơ đồ.
     */
    private static Seat pickSurvivor(List<Seat> bucket, boolean nextColTaken) {
        return bucket.stream()
                .min(java.util.Comparator
                        .comparingInt((Seat s) -> (s.getLabel() != null && !s.getLabel().isBlank()) ? 0 : 1)
                        .thenComparingInt(s -> (nextColTaken && isSweetbox(s)) ? 1 : 0)
                        .thenComparingInt(s -> s.getId() == null ? Integer.MAX_VALUE : s.getId()))
                .orElse(null);
    }

    private static boolean isSweetbox(Seat s) {
        return s.getSeatType() != null && "SWEETBOX".equalsIgnoreCase(s.getSeatType().getName());
    }

    /** Trạng thái ghế vật lý hợp lệ; mọi giá trị khác bị coi là rác → AVAILABLE. */
    private static final Set<String> VALID_SEAT_STATUS = Set.of("AVAILABLE", "MAINTENANCE", "LOCKED");

    /**
     * DỌN HÀNG LOẠT toàn hệ thống (chạy 1 lần, chỉ ADMIN). Sửa triệt để lỗi lệch sơ đồ do dữ liệu
     * cũ: (1) deactivate ghế "ma" ngoài khung matrixRow×matrixCol của MỌI phòng; (2) chuẩn hóa
     * status rác (vd "ACTIVE") → AVAILABLE; (3) dựng lại snapshot cho mọi suất chưa kết thúc &
     * chưa có ghế GIỮ/BÁN (suất đã bán giữ nguyên để không dịch ghế khách). Trả thống kê thay đổi.
     */
    @Transactional
    public java.util.Map<String, Integer> cleanupAllRooms() {
        int roomsTouched = 0, seatsDeactivated = 0, statusFixed = 0, showtimesResynced = 0;
        List<Room> rooms = roomRepository.findAll();

        for (Room room : rooms) {
            Integer R = room.getMatrixRow();
            Integer C = room.getMatrixCol();
            if (R == null || C == null) continue;

            List<Seat> seats = seatRepository.findByRoomId(room.getId());
            List<Seat> dirty = new java.util.ArrayList<>();
            for (Seat s : seats) {
                if (!Boolean.TRUE.equals(s.getIsActive())) continue;
                boolean off = s.getGridRow() != null && s.getGridCol() != null
                        && (s.getGridRow() >= R || s.getGridCol() >= C);
                if (off) {
                    s.setIsActive(false);
                    seatsDeactivated++;
                    dirty.add(s);
                } else {
                    String st = s.getSeatStatus();
                    if (st != null && !VALID_SEAT_STATUS.contains(st)) {
                        s.setSeatStatus("AVAILABLE");
                        statusFixed++;
                        dirty.add(s);
                    }
                }
            }
            if (!dirty.isEmpty()) {
                seatRepository.saveAll(dirty);
                roomsTouched++;
            }
        }

        // Dựng lại snapshot cho MỌI phòng (kể cả phòng không đổi ghế — để chữa snapshot cũ đã lệch).
        for (Room room : rooms) {
            showtimesResynced += resyncActiveShowtimeSnapshots(room.getId());
        }

        return java.util.Map.of(
                "roomsTouched", roomsTouched,
                "seatsDeactivated", seatsDeactivated,
                "statusFixed", statusFixed,
                "showtimesResynced", showtimesResynced);
    }
}
