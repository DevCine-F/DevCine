package com.devcine.backend.service;

import com.devcine.backend.dto.response.SeatLayoutSnapshot;
import com.devcine.backend.entity.Room;
import com.devcine.backend.entity.Seat;
import com.devcine.backend.repository.RoomRepository;
import com.devcine.backend.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dựng & đọc ảnh chụp sơ đồ ghế (snapshot) cho suất chiếu.
 *
 * <p>Chốt kiến trúc: snapshot chỉ đông cứng KHUNG KHÔNG GIAN (vị trí, loại ghế, label, lối đi,
 * span, seatId, rowChar, colNum). Trạng thái SOLD/HOLD/MAINTENANCE KHÔNG nằm trong đây — luôn tính live theo seatId.
 * Nhờ vậy sửa phòng về sau không phá suất cũ, mà bảo trì/đã-bán vẫn phản ánh đúng thời gian thực.
 *
 * <p>Được tiêu thụ từ Phase 2 (gắn vào {@code showtimes.layout_data}, đọc chung cho cả hiển thị
 * lẫn luật đặt vé). Ở Phase 1 mới chỉ cung cấp hàm dựng/đọc thuần.
 */
@Service
@RequiredArgsConstructor
public class SeatLayoutSnapshotService {

    /** Phiên bản cấu trúc JSON hiện tại. */
    public static final int SCHEMA_VERSION = 1;

    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;
    private final ObjectMapper objectMapper;

    /**
     * Chụp toàn bộ khung sơ đồ (gồm cả lối đi) của một phòng thành chuỗi JSON.
     * Kích thước lưới lấy theo Room (matrixRow, matrixCol) chuẩn của Admin.
     */
    @Transactional(readOnly = true)
    public String buildSnapshotJson(Integer roomId) {
        return objectMapper.writeValueAsString(buildSnapshot(roomId));
    }

    @Transactional(readOnly = true)
    public SeatLayoutSnapshot buildSnapshot(Integer roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        List<Seat> cells = seatRepository.findLayoutByRoomId(roomId);

        int maxRow = -1;
        int maxCol = -1;
        List<SeatLayoutSnapshot.Cell> out = new ArrayList<>(cells.size());
        for (Seat s : cells) {
            if (s.getGridRow() == null || s.getGridCol() == null) continue; // ô không có toạ độ → bỏ

            boolean seatCell = s.isSeatCell();
            String type = (seatCell && s.getSeatType() != null) ? s.getSeatType().getName() : null;
            int span = (seatCell && "SWEETBOX".equalsIgnoreCase(type)) ? 2 : 1;

            maxRow = Math.max(maxRow, s.getGridRow());
            maxCol = Math.max(maxCol, s.getGridCol() + (span - 1));

            out.add(SeatLayoutSnapshot.Cell.builder()
                    .kind(seatCell ? "SEAT" : "AISLE")
                    .seatId(seatCell ? s.getId() : null)
                    .rowChar(seatCell ? s.getRowChar() : null)
                    .colNum(seatCell ? s.getColNum() : null)
                    .gridRow(s.getGridRow())
                    .gridCol(s.getGridCol())
                    .type(type)
                    .label(seatCell ? s.displayLabel() : null)
                    .span(span)
                    .build());
        }

        int matrixRow = (room != null && room.getMatrixRow() != null && room.getMatrixRow() > 0)
                ? room.getMatrixRow()
                : Math.max(maxRow + 1, 1);
        int matrixCol = (room != null && room.getMatrixCol() != null && room.getMatrixCol() > 0)
                ? room.getMatrixCol()
                : Math.max(maxCol + 1, 1);

        return SeatLayoutSnapshot.builder()
                .schema(SCHEMA_VERSION)
                .matrixRow(matrixRow)
                .matrixCol(matrixCol)
                .snapshotAt(LocalDateTime.now().toString())
                .cells(out)
                .build();
    }

    /** Parse JSON snapshot đã lưu về đối tượng để hiển thị / kiểm tra luật đặt vé (Phase 2). */
    public SeatLayoutSnapshot parse(String json) {
        return objectMapper.readValue(json, SeatLayoutSnapshot.class);
    }
}
