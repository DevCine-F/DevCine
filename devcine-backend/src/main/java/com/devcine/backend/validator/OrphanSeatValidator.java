package com.devcine.backend.validator;

import com.devcine.backend.entity.Seat;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Thuật toán kiểm tra chống ghế mồ côi (Orphan Seat Prevention) cấp server.
 * Đảm bảo các luồng Đặt vé / Đổi sự cố không tạo ra khoảng trống 1 ghế đơn lẻ kẹp giữa hai rào cản.
 */
@Component
public class OrphanSeatValidator {

    /**
     * Kiểm tra xem việc gán tập ghế {@code selectedSeatIds} vào sơ đồ phòng (với trạng thái chiếm chỗ {@code occupiedSeatIds})
     * có tạo ra ghế mồ côi (1 ghế trống đơn lẻ kẹp giữa 2 rào cản / ghế đã bán) do chính thao tác này gây ra hay không.
     *
     * @param allSeats danh sách tất cả ghế trong phòng
     * @param occupiedSeatIds danh sách ID các ghế đã được bán/giữ (sau khi đã trừ ghế cũ và cộng ghế mới)
     * @param selectedSeatIds danh sách ID các ghế đích mới được chọn trong đợt này
     * @return true nếu phát hiện có ghế mồ côi do thao tác này gây ra, false nếu an toàn
     */
    public boolean hasOrphanSeats(List<Seat> allSeats, Set<Integer> occupiedSeatIds, List<Integer> selectedSeatIds) {
        if (selectedSeatIds == null || selectedSeatIds.isEmpty() || allSeats == null || allSeats.isEmpty()) {
            return false;
        }

        Set<Integer> selectedSet = new HashSet<>(selectedSeatIds);
        Set<Integer> occupiedSet = occupiedSeatIds != null ? occupiedSeatIds : Collections.emptySet();

        Map<Integer, List<Seat>> rows = allSeats.stream()
                .filter(s -> s.getGridRow() != null && s.getGridCol() != null)
                .collect(Collectors.groupingBy(Seat::getGridRow));

        for (Map.Entry<Integer, List<Seat>> rowEntry : rows.entrySet()) {
            List<Seat> seatsInRow = rowEntry.getValue();
            boolean hasSelectionInRow = seatsInRow.stream().anyMatch(s -> selectedSet.contains(s.getId()));
            if (!hasSelectionInRow) continue;

            int maxCol = seatsInRow.stream().mapToInt(Seat::getGridCol).max().orElse(-1);
            if (maxCol < 0) continue;

            // Xây dựng bản đồ trạng thái:
            // 'X' = Barrier (Lối đi AISLE / Biên tường / Ghế bảo trì / Ghế khóa / Ô span thứ 2 của Sweetbox)
            // 'S' = Selected (Ghế đích vừa chọn)
            // 'O' = Occupied (Ghế đã bán/giữ từ trước)
            // 'E' = Empty (Ghế trống còn bán được)
            char[] state = new char[maxCol + 1];
            Arrays.fill(state, 'X');

            for (Seat s : seatsInRow) {
                int col = s.getGridCol();
                if (col < 0 || col > maxCol) continue;

                if ("AISLE".equalsIgnoreCase(s.getCellKind())) {
                    state[col] = 'X';
                    continue;
                }

                String seatStatus = s.getSeatStatus();
                if (seatStatus != null && !"AVAILABLE".equalsIgnoreCase(seatStatus)) {
                    state[col] = 'X'; // Ghế bảo trì / khóa
                } else if (selectedSet.contains(s.getId())) {
                    state[col] = 'S';
                } else if (occupiedSet.contains(s.getId())) {
                    state[col] = 'O';
                } else {
                    state[col] = 'E';
                }

                // Nếu là SWEETBOX (span 2), ô kế tiếp là rào cản
                boolean isSweetbox = s.getSeatType() != null && "SWEETBOX".equalsIgnoreCase(s.getSeatType().getName());
                if (isSweetbox && col + 1 <= maxCol) {
                    state[col + 1] = 'X';
                }
            }

            // Quét tìm khe trống đơn lẻ (1 ô 'E' giữa 2 rào cản/ghế chiếm)
            for (int c = 0; c <= maxCol; c++) {
                if (state[c] == 'E') {
                    boolean leftBarrier = (c == 0) || state[c - 1] != 'E';
                    boolean rightBarrier = (c == maxCol) || state[c + 1] != 'E';
                    if (leftBarrier && rightBarrier) {
                        // Kiểm tra xem người dùng có TẠO RA khe hở này không (kế bên có 'S')
                        boolean causedByUser = (c > 0 && state[c - 1] == 'S') || (c < maxCol && state[c + 1] == 'S');
                        if (causedByUser) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
