import { computed } from 'vue'

/**
 * Chống ghế mồ côi (orphan seat) — bản MIRROR ở Frontend của
 * BookingService.validateSeatGap / validateSeatGapFromSnapshot (backend).
 *
 * ⚠️ HAI BẢN PHẢI ĐỒNG HÀNH VI: sửa thuật toán ở đây thì phải sửa cả
 *    devcine-backend/.../service/BookingService.java (và ngược lại).
 *    FE chỉ là lớp UX cảnh báo sớm; BACKEND vẫn là chốt chặn cuối khi Hold/Pay.
 *
 * Quét theo TỪNG HÀNG lưới, dựng trạng thái mỗi cột:
 *   S = ghế đang chọn · E = ghế trống còn bán được · X = rào cản
 *   (lối đi/tường/ghế đã bán-giữ-khoá-bảo trì/nửa phải SWEETBOX/vị trí không có ô).
 * Một ô 'E' bị KẸP giữa 2 rào cản (trái & phải đều không phải 'E') VÀ nằm sát ngay
 * một ghế 'S' (do người dùng vừa tạo) ⇒ ghế mồ côi. Khe trống có sẵn giữa 2 ghế đã bán
 * (không do người dùng tạo) KHÔNG bị cảnh báo — khớp điều kiện `causedByUser` của backend.
 *
 * @param {Object} p
 * @param {() => Array} p.seats getter mảng cell DTO { kind, seatId, gridRow, gridCol, seatType, span }
 * @param {() => (Array<number>|Set<number>)} p.selectedIds getter id ghế đang chọn
 * @param {(cell) => boolean} [p.isSeatBlocked] cell SEAT có bị coi là rào cản (đã bán/giữ/khoá/bảo trì/khoá real-time)
 * @returns {{ orphanKeys: import('vue').ComputedRef<Set<string>>, hasOrphan: import('vue').ComputedRef<boolean> }}
 *          orphanKeys: tập khoá "gridRow-gridCol" của các ghế mồ côi (để bôi cảnh báo đúng ô).
 */
export function useOrphanSeatCheck({ seats, selectedIds, isSeatBlocked }) {
  const S = 'S', E = 'E', X = 'X'

  const orphanKeys = computed(() => {
    const keys = new Set()
    const cells = seats() || []
    const selRaw = selectedIds() || []
    const selected = selRaw instanceof Set ? selRaw : new Set(selRaw)
    const blocked = typeof isSeatBlocked === 'function' ? isSeatBlocked : () => false

    // Gom cell theo hàng lưới (gridRow)
    const rows = new Map()
    for (const c of cells) {
      if (c == null || c.gridRow == null || c.gridCol == null) continue
      if (!rows.has(c.gridRow)) rows.set(c.gridRow, [])
      rows.get(c.gridRow).push(c)
    }

    for (const [row, rowCells] of rows) {
      // Chỉ soi hàng có ghế đang chọn (khớp hasSelectionInRow của backend)
      if (!rowCells.some(c => c.seatId != null && selected.has(c.seatId))) continue

      let maxCol = -1
      for (const c of rowCells) if (c.gridCol > maxCol) maxCol = c.gridCol
      if (maxCol < 0) continue

      const state = new Array(maxCol + 1).fill(X) // vị trí không có ô = rào cản
      for (const c of rowCells) {
        const col = c.gridCol
        if (col < 0 || col > maxCol) continue
        const isSeat = c.kind == null || c.kind === 'SEAT' // kind thiếu = ghế (tương thích payload cũ)
        if (!isSeat) { state[col] = X; continue } // lối đi (AISLE)
        // SELECTED ưu tiên trước blocked: ghế của chính giao dịch này luôn là 'S'
        // (tránh false-barrier khi khôi phục đơn giữ — ghế của mình có thể mang status HOLD).
        if (c.seatId != null && selected.has(c.seatId)) state[col] = S
        else if (blocked(c)) state[col] = X // đã bán/giữ/khoá/bảo trì/khoá real-time
        else state[col] = E
        // SWEETBOX chiếm 2 cột: ô kế bên coi như rào cản
        if ((c.span === 2 || c.seatType === 'SWEETBOX') && col + 1 <= maxCol) state[col + 1] = X
      }

      for (let c = 0; c <= maxCol; c++) {
        if (state[c] !== E) continue
        const leftBarrier = c === 0 || state[c - 1] !== E
        const rightBarrier = c === maxCol || state[c + 1] !== E
        if (!leftBarrier || !rightBarrier) continue
        const causedByUser = (c > 0 && state[c - 1] === S) || (c < maxCol && state[c + 1] === S)
        if (causedByUser) keys.add(`${row}-${c}`)
      }
    }
    return keys
  })

  const hasOrphan = computed(() => orphanKeys.value.size > 0)
  return { orphanKeys, hasOrphan }
}
