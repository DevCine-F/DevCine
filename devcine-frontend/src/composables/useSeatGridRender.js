import { computed } from "vue";

/**
 * Logic NỘI SUY TỌA ĐỘ dùng chung cho mọi màn render sơ đồ ghế từ payload suất chiếu
 * (Client BookingView, POS TicketingPOS...). KHÔNG chứa markup/style — mỗi UI vẫn tự vẽ.
 *
 * Nguồn dữ liệu là mảng cell DTO trả từ backend (ShowtimeSeatResponse.seats), mỗi cell có:
 *   { kind: 'SEAT'|'AISLE', seatId, gridRow, gridCol, seatType, span, label, status, seatStatus }
 * Lối đi (AISLE) nay được backend trả tường minh → FE không còn tự đoán "ô trống = lối đi".
 *
 * @param {() => Array|import('vue').Ref} seatsSource getter hoặc ref trả mảng cell.
 */
export function useSeatGridRender(seatsSource) {
  const getSeats = typeof seatsSource === "function"
    ? seatsSource
    : () => (seatsSource?.value ?? []);

  const isAisle = (cell) => !!cell && cell.kind === "AISLE";
  // Ghế bán được: có cell và KHÔNG phải lối đi (kind null/thiếu = ghế, cho tương thích payload cũ).
  const isSeat = (cell) => !!cell && cell.kind !== "AISLE";

  // Chỉ mục O(1) theo "gridRow-gridCol" — một cách dựng lưới DUY NHẤT cho mọi màn (chống lệch).
  const cellIndex = computed(() => {
    const map = new Map();
    for (const s of getSeats() || []) {
      if (s == null || s.gridRow == null || s.gridCol == null) continue;
      map.set(`${s.gridRow}-${s.gridCol}`, s);
    }
    return map;
  });

  /** Ô bất kỳ tại vị trí lưới (ghế HOẶC lối đi), hoặc null nếu trống. Dùng cho template quyết định cách vẽ. */
  const cellAt = (row, col) => cellIndex.value.get(`${row}-${col}`) || null;

  /**
   * CHỈ ghế bán được tại vị trí lưới (lối đi → null). Thay thế trực tiếp getSeatAt/seatAt cũ:
   * giữ nguyên mọi logic khối/chống-mồ-côi vốn coi "null = rào cản" (lối đi vẫn là rào cản).
   */
  const seatAt = (row, col) => {
    const c = cellIndex.value.get(`${row}-${col}`);
    return isSeat(c) ? c : null;
  };

  return { cellAt, seatAt, isAisle, isSeat };
}
