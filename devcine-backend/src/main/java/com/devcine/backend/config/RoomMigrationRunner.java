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
            
            // Tìm hoặc tạo Room 1
            Room room1 = getRoomByIndex(rooms, 0, cinema);
            updateRoom(room1, "Phòng 01 - Standard", "STANDARD", 10, 16);
            upsertSeats(room1, 10, 16, 4, 9, normalType, vipType, sweetboxType);

            // Tìm hoặc tạo Room 2
            Room room2 = getRoomByIndex(rooms, 1, cinema);
            updateRoom(room2, "Phòng 02 - Superplex", "SUPERPLEX", 10, 16);
            upsertSeats(room2, 10, 16, 3, 9, normalType, vipType, sweetboxType);

            // Tìm hoặc tạo Room 3
            Room room3 = getRoomByIndex(rooms, 2, cinema);
            updateRoom(room3, "Phòng 03 - Cine Comfort", "CINE_COMFORT", 8, 10);
            upsertSeats(room3, 8, 10, 2, 8, normalType, vipType, sweetboxType);
            
            // Xóa hoặc set Maintenance cho các phòng dư thừa (> 3)
            if (rooms.size() > 3) {
                for (int i = 3; i < rooms.size(); i++) {
                    Room extra = rooms.get(i);
                    extra.setStatus("Maintenance");
                    roomRepository.save(extra);
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

    private Room getRoomByIndex(List<Room> rooms, int index, Cinema cinema) {
        if (index < rooms.size()) {
            return rooms.get(index);
        }
        Room newRoom = new Room();
        newRoom.setCinema(cinema);
        newRoom.setTurnaroundTimeMins(15);
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
        java.util.Map<String, Seat> existingSeats = new java.util.HashMap<>();
        if (existingSeatsList != null) {
            for (Seat s : existingSeatsList) {
                existingSeats.put(s.getRowChar() + "-" + s.getColNum(), s);
            }
        }
        
        List<Seat> seatsToSave = new ArrayList<>();
        
        for (int r = 1; r <= rows; r++) {
            String rowChar = String.valueOf((char) ('A' + r - 1));
            for (int c = 1; c <= cols; c++) {
                String key = rowChar + "-" + c;
                Seat seat = existingSeats.remove(key);
                
                if (seat == null) {
                    seat = new Seat();
                    seat.setRoom(room);
                    seat.setRowChar(rowChar);
                    seat.setColNum(c);
                }
                
                seat.setLabel(rowChar + String.format("%02d", c));
                seat.setIsActive(true);
                
                if (r <= normalRows) seat.setSeatType(normal);
                else if (r <= vipRows) seat.setSeatType(vip);
                else seat.setSeatType(sweetbox);
                
                seatsToSave.add(seat);
            }
        }
        
        // Deactivate remaining seats
        for (Seat remaining : existingSeats.values()) {
            if (remaining.getIsActive() != null && remaining.getIsActive()) {
                remaining.setIsActive(false);
                seatsToSave.add(remaining);
            }
        }
        
        seatRepository.saveAll(seatsToSave);
    }
}
