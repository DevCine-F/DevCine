package com.devcine.backend.service;

import com.devcine.backend.dto.request.RoomRequest;
import com.devcine.backend.dto.response.RoomResponse;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Room;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.RoomRepository;
import com.devcine.backend.repository.SeatRepository;
import com.devcine.backend.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final CinemaRepository cinemaRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final SeatService seatService;

    // Danh mục hợp lệ — đồng bộ với dropdown phía Frontend (chống can thiệp giá trị lạ).
    // Bắt mã hạng phòng chuẩn Lotte (khớp normalizeRoomType của PricingService).
    private static final Set<String> ALLOWED_TYPES = Set.of("STANDARD", "SUPERPLEX", "CINE_COMFORT");
    private static final Set<String> ALLOWED_STATUS = Set.of("Active", "Maintenance");

    @PostConstruct
    public void migrateRoomTypes() {
        roomRepository.findAll().forEach(r -> {
            String raw = r.getType();
            if (raw == null) return;
            String updated = raw;
            if (raw.equalsIgnoreCase("Sweetbox") || raw.equalsIgnoreCase("Standard")) updated = "STANDARD";
            else if (raw.equalsIgnoreCase("Superplex")) updated = "SUPERPLEX";
            else if (raw.equalsIgnoreCase("Cine Comfort") || raw.equalsIgnoreCase("Cine_Comfort")) updated = "CINE_COMFORT";
            
            if (!updated.equals(raw)) {
                r.setType(updated);
                roomRepository.save(r);
            }
        });
    }

    private String clean(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    /** Chuẩn hoá + validate; id null khi tạo mới, khác null khi cập nhật. */
    private void normalizeAndValidate(RoomRequest req, Integer cinemaId, Integer id) {
        req.setName(clean(req.getName()));
        if (req.getName() == null) {
            throw new IllegalArgumentException("Tên phòng không được để trống");
        }
        if (req.getType() != null && !ALLOWED_TYPES.contains(req.getType())) {
            throw new IllegalArgumentException("Loại phòng không hợp lệ");
        }
        if (req.getStatus() != null && !ALLOWED_STATUS.contains(req.getStatus())) {
            throw new IllegalArgumentException("Trạng thái phòng không hợp lệ");
        }
        boolean dup = (id == null)
                ? roomRepository.existsByCinema_IdAndNameIgnoreCase(cinemaId, req.getName())
                : roomRepository.existsByCinema_IdAndNameIgnoreCaseAndIdNot(cinemaId, req.getName(), id);
        if (dup) {
            throw new IllegalArgumentException("Tên phòng đã tồn tại trong cụm rạp này");
        }
        
        if (req.getTurnaroundTimeMins() != null) {
            if (req.getTurnaroundTimeMins() < 10 || req.getTurnaroundTimeMins() > 60) {
                throw new IllegalArgumentException("Thời gian dọn phòng phải từ 10 đến 60 phút");
            }
        }
        
        if (req.getMatrixRow() == null || req.getMatrixRow() < 5 || req.getMatrixRow() > 20) {
            throw new IllegalArgumentException("Số hàng ghế phải từ 5 đến 20");
        }
        if (req.getMatrixCol() == null || req.getMatrixCol() < 5 || req.getMatrixCol() > 30) {
            throw new IllegalArgumentException("Số cột ghế phải từ 5 đến 30");
        }
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByCinema(Integer cinemaId) {
        return roomRepository.findByCinemaId(cinemaId).stream()
                .map(RoomResponse::fromEntity)
                .toList();
    }

    @Transactional
    public RoomResponse createRoom(Integer cinemaId, RoomRequest req) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cụm rạp"));
        normalizeAndValidate(req, cinemaId, null);

        Room room = Room.builder()
                .cinema(cinema)
                .name(req.getName())
                .type(req.getType() != null ? req.getType() : "Standard")
                .status(req.getStatus() != null ? req.getStatus() : "Active")
                .turnaroundTimeMins(req.getTurnaroundTimeMins() != null ? req.getTurnaroundTimeMins() : 15)
                .matrixRow(req.getMatrixRow())
                .matrixCol(req.getMatrixCol())
                .build();
        room = roomRepository.save(room);

        // Tự sinh lưới ghế mặc định để phòng dùng được ngay
        seatService.generateDefaultSeats(room.getId(), req.getMatrixRow(), req.getMatrixCol());
        return RoomResponse.fromEntity(room);
    }

    @Transactional
    public RoomResponse updateRoom(Integer roomId, RoomRequest req) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng"));
        Integer cinemaId = room.getCinema().getId();
        normalizeAndValidate(req, cinemaId, roomId);

        boolean matrixChanged = !req.getMatrixRow().equals(room.getMatrixRow())
                || !req.getMatrixCol().equals(room.getMatrixCol());
        if (matrixChanged && showtimeRepository.existsByRoom_Id(roomId)) {
            throw new IllegalArgumentException("Phòng đã có suất chiếu, không thể đổi kích thước ma trận ghế");
        }
        
        boolean isGoingToMaintenance = req.getStatus() != null 
                && (req.getStatus().equalsIgnoreCase("MAINTENANCE") || req.getStatus().equalsIgnoreCase("INACTIVE"))
                && !req.getStatus().equalsIgnoreCase(room.getStatus());
                
        if (isGoingToMaintenance) {
            long activeShowtimes = showtimeRepository.countByRoomIdAndEndTimeAfter(roomId, java.time.LocalDateTime.now());
            if (activeShowtimes > 0) {
                throw new IllegalArgumentException("Không thể bảo trì! Phòng đang có " + activeShowtimes + " suất chiếu chưa kết thúc (bao gồm các suất chiếu hôm nay).");
            }
        }
                
        boolean typeChanged = req.getType() != null && !req.getType().equalsIgnoreCase(room.getType());
        if (typeChanged) {
            long futureShowtimes = showtimeRepository.countByRoomIdAndEndTimeAfter(roomId, java.time.LocalDateTime.now());
            if (futureShowtimes > 0) {
                throw new IllegalArgumentException("Không thể đổi loại phòng khi đang có " + futureShowtimes + " suất chiếu chưa diễn ra!");
            }
        }

        room.setName(req.getName());
        if (req.getType() != null) room.setType(req.getType());
        if (req.getStatus() != null) room.setStatus(req.getStatus());
        if (req.getTurnaroundTimeMins() != null) {
            room.setTurnaroundTimeMins(req.getTurnaroundTimeMins());
        } else {
            room.setTurnaroundTimeMins(15);
        }
        room.setMatrixRow(req.getMatrixRow());
        room.setMatrixCol(req.getMatrixCol());
        roomRepository.save(room);

        if (matrixChanged) {
            seatService.generateDefaultSeats(roomId, req.getMatrixRow(), req.getMatrixCol());
        }
        return RoomResponse.fromEntity(room);
    }

    @Transactional
    public void deleteRoom(Integer roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng"));
        if (showtimeRepository.existsByRoom_Id(roomId)) {
            throw new IllegalArgumentException("Phòng đã có suất chiếu, không thể xoá");
        }
        seatRepository.deleteByRoomId(roomId);
        roomRepository.delete(room);
    }
}
