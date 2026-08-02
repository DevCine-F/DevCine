package com.devcine.backend.config;

import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.Room;
import com.devcine.backend.entity.Seat;
import com.devcine.backend.entity.SeatType;
import com.devcine.backend.repository.CinemaRepository;
import com.devcine.backend.repository.RoomRepository;
import com.devcine.backend.repository.SeatRepository;
import com.devcine.backend.repository.SeatTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RoomMigrationRunner implements CommandLineRunner {

    private final CinemaRepository cinemaRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("====== BẮT ĐẦU CHẠY MIGRATION PHÒNG CHIẾU & SƠ ĐỒ GHẾ (UPSERT) ======");

        SeatType normalType = getOrCreateSeatType("NORMAL");
        SeatType vipType = getOrCreateSeatType("VIP");
        SeatType sweetboxType = getOrCreateSeatType("SWEETBOX");

        List<Cinema> cinemas = cinemaRepository.findAll();
        for (Cinema cinema : cinemas) {
            List<Room> rooms = roomRepository.findByCinemaId(cinema.getId());
            // Tìm hoặc tạo các phòng theo TÊN để tránh vi phạm Unique Constraint
            Room room1 = getOrCreateRoomByName(rooms, "Phòng 01 - Standard", cinema);
            updateRoom(room1, "Phòng 01 - Standard", "STANDARD", 10, 16);
            upsertSeats(room1, 10, 16, 4, 9, normalType, vipType, sweetboxType);

            Room room2 = getOrCreateRoomByName(rooms, "Phòng 02 - Superplex", cinema);
            updateRoom(room2, "Phòng 02 - Superplex", "SUPERPLEX", 10, 16);
            upsertSeats(room2, 10, 16, 3, 9, normalType, vipType, sweetboxType);

            Room room3 = getOrCreateRoomByName(rooms, "Phòng 03 - Cine Comfort", cinema);
            updateRoom(room3, "Phòng 03 - Cine Comfort", "CINE_COMFORT", 8, 10);
            upsertSeats(room3, 8, 10, 2, 8, normalType, vipType, sweetboxType);
            
            // Xóa hoặc set Maintenance cho các phòng dư thừa
            for (Room r : rooms) {
                String n = r.getName();
                if (n != null && !n.equals("Phòng 01 - Standard") 
                              && !n.equals("Phòng 02 - Superplex") 
                              && !n.equals("Phòng 03 - Cine Comfort")) {
                    r.setStatus("Maintenance");
                    try {
                        roomRepository.save(r);
                    } catch (Exception e) {
                        System.err.println("Không thể cập nhật trạng thái phòng dư thừa: " + e.getMessage());
                    }
                }
            }
        }
        System.out.println("====== HOÀN TẤT MIGRATION PHÒNG CHIẾU & SƠ ĐỒ GHẾ ======");
    }

    private SeatType getOrCreateSeatType(String name) {
        return seatTypeRepository.findAll().stream()
                .filter(t -> name.equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElseGet(() -> seatTypeRepository.save(SeatType.builder().name(name).build()));
    }

    private Room getOrCreateRoomByName(List<Room> rooms, String targetName, Cinema cinema) {
        // 1. Tìm đúng phòng theo tên
        for (Room r : rooms) {
            if (targetName.equals(r.getName())) {
                return r;
            }
        }
        
        // 2. Nếu không tìm thấy, tái chế một phòng rác (không phải 3 phòng chuẩn)
        for (Room r : rooms) {
            String n = r.getName();
            if (n != null && !n.equals("Phòng 01 - Standard") 
                          && !n.equals("Phòng 02 - Superplex") 
                          && !n.equals("Phòng 03 - Cine Comfort")) {
                r.setName(targetName);
                return r;
            }
        }
        
        // 3. Nếu không còn phòng rác nào, tạo mới
        Room newRoom = new Room();
        newRoom.setCinema(cinema);
        newRoom.setTurnaroundTimeMins(15);
        newRoom.setName(targetName);
        rooms.add(newRoom);
        return roomRepository.save(newRoom);
    }

    private void updateRoom(Room room, String name, String type, int rows, int cols) {
        room.setName(name);
        room.setType(type);
        room.setStatus("Active");
        room.setMatrixRow(rows);
        room.setMatrixCol(cols);
        roomRepository.save(room);
    }

    private void upsertSeats(Room room, int rows, int cols, int normalRows, int vipRows, SeatType normal, SeatType vip, SeatType sweetbox) {
        List<Seat> existingSeatsList = seatRepository.findByRoomId(room.getId());
        if (existingSeatsList != null && !existingSeatsList.isEmpty()) {
            System.out.println("Bỏ qua việc tạo ghế cho phòng " + room.getName() + " vì đã có sơ đồ ghế.");
            return;
        }
        
        List<Seat> seatsToSave = new ArrayList<>();
        
        for (int r = 1; r <= rows; r++) {
            String rowChar = String.valueOf((char) ('A' + r - 1));
            for (int c = 1; c <= cols; c++) {
                Seat seat = new Seat();
                seat.setRoom(room);
                seat.setRowChar(rowChar);
                seat.setColNum(c);
                seat.setLabel(rowChar + String.format("%02d", c));
                seat.setIsActive(true);
                
                if (r <= normalRows) seat.setSeatType(normal);
                else if (r <= vipRows) seat.setSeatType(vip);
                else seat.setSeatType(sweetbox);
                
                seatsToSave.add(seat);
            }
        }
        
        seatRepository.saveAll(seatsToSave);
    }
}
