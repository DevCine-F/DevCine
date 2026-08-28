<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { incidentApi } from '@/api/admin/index'
import { useToastStore } from '@/stores/toast'
import { useConfirmStore } from '@/stores/confirm'
import { friendlyError } from '@/utils/friendlyError'
import { useSeatRealtime } from '@/composables/useSeatRealtime'
import SeatGridRenderer from '@/components/common/SeatGridRenderer.vue'

const toast = useToastStore()
const confirm = useConfirmStore()

// ===== Tabs =====
const activeTab = ref('handle') // 'handle' | 'history'

// ===== Tra cứu & ngữ cảnh vé =====
const searchQuery = ref('')
const loadingCtx = ref(false)
const ctx = ref(null)          // IncidentBookingContext (đơn đã chọn)
const phoneBookings = ref([])  // List<IncidentBookingContext> khi tìm theo SĐT có nhiều đơn

// ===== Sơ đồ ghế của suất =====
const seatMap = ref(null)      // { matrixRow, matrixCol, seats[] }
const loadingMap = ref(false)

// ===== Thao tác: chỉ đổi ghế (đã bỏ hủy chỗ) =====
const mode = ref('relocate')   // chỉ còn 'relocate'
const swaps = ref({})          // oldSeatId -> newSeatId
const activeSource = ref(null) // oldSeatId đang chờ gán ghế đích

// ===== Đền bù =====
const compOptions = ref([])
const compChoice = ref('NONE') // promotionId | 'NONE'
const compNote = ref('')
const submitting = ref(false)
const result = ref(null)       // kết quả sau khi xử lý

// ===== Lịch sử =====
const histFilters = ref({ type: '', code: '', from: '', to: '' })
const histRows = ref([])
const histPage = ref(0)
const histTotal = ref(0)
const histSize = 20
const loadingHist = ref(false)

// ===== WebSocket/STOMP real-time sơ đồ ghế =====
const { connected, connect, disconnect } = useSeatRealtime({
  by: 'Quầy sự cố',
  onSold: (ids) => {
    // Ghế vừa được bán/đổi ở nơi khác → cập nhật sơ đồ ngay lập tức
    if (!seatMap.value?.seats) return
    seatMap.value = {
      ...seatMap.value,
      seats: seatMap.value.seats.map(seat =>
        ids.includes(seat.seatId) ? { ...seat, status: 'SOLD' } : seat
      )
    }
  },
  onReleased: (ids) => {
    // Ghế vừa được giải phóng (hủy chỗ / nhả ghế) → chuyển sang AVAILABLE
    if (!seatMap.value?.seats) return
    seatMap.value = {
      ...seatMap.value,
      seats: seatMap.value.seats.map(seat =>
        ids.includes(seat.seatId) ? { ...seat, status: 'AVAILABLE' } : seat
      )
    }
  },
  onHeld: (ids) => {
    // Ghế đang bị giữ tạm ở nơi khác
    if (!seatMap.value?.seats) return
    seatMap.value = {
      ...seatMap.value,
      seats: seatMap.value.seats.map(seat =>
        ids.includes(seat.seatId) ? { ...seat, status: 'HOLD' } : seat
      )
    }
  },
})

// Kết nối STOMP khi có showtimeId, ngắt khi reset đơn
watch(() => ctx.value?.showtime?.showtimeId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    connect(newId)
  } else if (!newId) {
    disconnect()
  }
}, { immediate: false })

onUnmounted(() => {
  disconnect()
})

// ---- Chỉ mục hỗ trợ ----
const soldSeats = computed(() => (ctx.value?.seats || []).filter(s => s.status === 'SOLD'))
const bookingSeatIds = computed(() => new Set(soldSeats.value.map(s => s.seatId)))
const seatById = computed(() => {
  const m = new Map()
  for (const c of seatMap.value?.seats || []) if (c.seatId != null) m.set(c.seatId, c)
  return m
})
const chosenDest = computed(() => new Set(Object.values(swaps.value)))
const started = computed(() => ctx.value?.showtime?.started === true)
// expired: suất chiếu đã kết thúc quá 2 giờ → chặn toàn bộ thao tác xử lý sự cố
const expired = computed(() => ctx.value?.showtime?.expired === true)

const visibleCompOptions = computed(() =>
  compOptions.value.filter(o => !o.cancelOnly))

const selectedSwaps = computed(() =>
  Object.entries(swaps.value).filter(([, dest]) => dest != null)
    .map(([oldSeatId, newSeatId]) => ({ oldSeatId: Number(oldSeatId), newSeatId })))

const canSubmitRelocate = computed(() => selectedSwaps.value.length > 0 && !started.value && !expired.value)

// ================= Tra cứu =================
async function doLookup(preserveResult = false) {
  const q = searchQuery.value.trim()
  if (!q) { toast.warning('Nhập mã vé hoặc số điện thoại khách.'); return }
  loadingCtx.value = true
  phoneBookings.value = []
  resetWorkspace(preserveResult)
  try {
    if (/^\d{9,11}$/.test(q)) {
      // --- Tra theo SĐT → có thể có nhiều đơn ---
      const { data } = await incidentApi.lookupByPhone(q)
      const list = data.data ?? data
      if (list.length === 1) {
        // Chỉ 1 đơn → load thẳng vào workspace
        ctx.value = list[0]
        await loadSeatMap()
      } else {
        // Nhiều đơn → hiện picker để nhân viên chọn
        phoneBookings.value = list
      }
    } else {
      // --- Tra theo mã vé → luôn 1 đơn duy nhất ---
      const { data } = await incidentApi.lookup(q)
      ctx.value = data.data ?? data
      await loadSeatMap()
    }
  } catch (e) {
    ctx.value = null
    phoneBookings.value = []
    toast.error(friendlyError(e, 'Không tìm thấy vé phù hợp.'))
  } finally {
    loadingCtx.value = false
  }
}

// Nhân viên click chọn 1 đơn từ danh sách SĐT
async function selectPhoneBooking(booking) {
  phoneBookings.value = []
  ctx.value = booking
  await loadSeatMap()
}

async function loadSeatMap() {
  const id = ctx.value?.showtime?.showtimeId
  if (!id) return
  loadingMap.value = true
  try {
    const { data } = await incidentApi.seats(id)
    seatMap.value = data.data ?? data
  } catch (e) {
    seatMap.value = null
    toast.error(friendlyError(e, 'Không tải được sơ đồ ghế.'))
  } finally {
    loadingMap.value = false
  }
}

function resetWorkspace(preserveResult = false) {
  ctx.value = null
  seatMap.value = null
  swaps.value = {}
  activeSource.value = null
  compChoice.value = 'NONE'
  compNote.value = ''
  if (!preserveResult) {
    result.value = null
  }
}

// ================= Tương tác sơ đồ & Chặn ghế lẻ (Lotte Proactive Locking) =================
function isSeatMaintenance(cell) {
  if (!cell) return false
  return cell.status === 'MAINTENANCE' || cell.status === 'LOCKED' ||
         cell.seatStatus === 'MAINTENANCE' || cell.seatStatus === 'LOCKED'
}

function seatTypeLabel(t) {
  const type = (t || '').toUpperCase()
  if (type === 'VIP') return 'VIP'
  if (type === 'SWEETBOX' || type === 'DOUBLE' || type === 'COUPLE') return 'Sweetbox'
  return 'Thường'
}

// Ghế đôi: SWEETBOX/DOUBLE chiếm 2 CỘT lưới và ngồi được 2 người
const isCoupleSeat = (c) =>
  !!c && (c.span === 2 || ['SWEETBOX', 'DOUBLE', 'COUPLE'].includes(String(c.seatType || '').toUpperCase()))

const cellSpanCols = (c) =>
  c.kind === 'AISLE' ? Math.max(1, Number(c.span) || 1) : (isCoupleSeat(c) ? 2 : 1)

// Xác định ghế nguồn hiện tại đang cần tìm ghế đích
const currentTargetSeat = computed(() => {
  if (activeSource.value != null) {
    return soldSeats.value.find(s => s.seatId === activeSource.value) || null
  }
  // Nếu chưa chọn activeSource: lấy ghế đầu tiên trong đơn chưa được gán ghế đích
  const unassigned = soldSeats.value.filter(s => swaps.value[s.seatId] == null)
  if (unassigned.length > 0) return unassigned[0]
  return null
})

const currentTargetSize = computed(() => {
  if (!currentTargetSeat.value) return 0
  return isCoupleSeat(currentTargetSeat.value) ? 2 : 1
})

const isSeatFreeForRelocate = (s) =>
  !!s && s.status === 'AVAILABLE' && !isSeatMaintenance(s) && !chosenDest.value.has(s.seatId)

const WALL_SLOT = Object.freeze({ state: 'WALL', w: 1 })

const seatRowSlots = computed(() => {
  const byRow = new Map()
  const seats = seatMap.value?.seats || []
  for (const c of seats) {
    if (!c || c.gridRow == null || c.gridCol == null) continue
    if (!byRow.has(c.gridRow)) byRow.set(c.gridRow, [])
    byRow.get(c.gridRow).push(c)
  }

  const grid = new Map()
  for (const [row, cells] of byRow) {
    let width = 0
    for (const c of cells) width = Math.max(width, c.gridCol + cellSpanCols(c))

    const slots = new Array(width)
    for (let i = 0; i < width; i++) slots[i] = WALL_SLOT

    for (const c of cells) {
      if (c.kind === 'AISLE') continue
      slots[c.gridCol] = {
        state: isSeatFreeForRelocate(c) ? 'FREE' : 'BUSY',
        w: isCoupleSeat(c) ? 2 : 1,
        cell: c,
      }
    }
    for (const c of cells) {
      if (c.kind === 'AISLE' || !isCoupleSeat(c)) continue
      const next = c.gridCol + 1
      if (next < width && slots[next] === WALL_SLOT) slots[next] = { state: 'SPAN', w: 1, ownerCol: c.gridCol }
    }
    grid.set(row, slots)
  }
  return grid
})

const seatRowRuns = computed(() => {
  const byRow = new Map()
  for (const [row, slots] of seatRowSlots.value) {
    const runs = []
    let cur = null
    let col = 0
    while (col < slots.length) {
      const s = slots[col]
      if (s && s.state === 'FREE') {
        if (!cur) { cur = { seats: [], caps: [], capacity: 0 }; runs.push(cur) }
        cur.seats.push(s.cell)
        cur.caps.push(s.w)
        cur.capacity += s.w
        col += s.w
      } else {
        cur = null
        col += 1
      }
    }
    byRow.set(row, runs)
  }
  return byRow
})

const runContaining = (anchorCell) => {
  if (!anchorCell || anchorCell.gridRow == null || anchorCell.seatId == null) return null
  const runs = seatRowRuns.value.get(anchorCell.gridRow)
  if (!runs) return null
  for (const run of runs) {
    const idx = run.seats.findIndex(s => s.seatId === anchorCell.seatId)
    if (idx >= 0) return { run, anchorIdx: idx }
  }
  return null
}

const placementsIn = (run, size) => {
  const out = []
  const n = run.seats.length
  const odd = size % 2 === 1
  for (let j = 0; j < n; j++) {
    let cap = 0
    let e = j
    let hasCouple = false
    while (e < n && cap < size) { if (run.caps[e] === 2) hasCouple = true; cap += run.caps[e]; e++ }
    if (cap !== size) continue
    if (odd && hasCouple) continue
    let leftGap = 0
    for (let t = 0; t < j; t++) leftGap += run.caps[t]
    let rightGap = 0
    for (let t = e; t < n; t++) rightGap += run.caps[t]
    out.push({ seats: run.seats.slice(j, e), startIdx: j, endIdx: e - 1, leftGap, rightGap })
  }
  return out
}

const orphanFree = (p) => p.leftGap !== 1 && p.rightGap !== 1

const snapBlockAt = (anchorCell) => {
  const size = currentTargetSize.value
  if (!size) return null
  const found = runContaining(anchorCell)
  if (!found) return null
  const { run, anchorIdx } = found
  const cands = placementsIn(run, size)
  if (cands.length === 0) return null

  if (size === 1) {
    const p = cands.find(c => c.startIdx === anchorIdx)
    return (p && orphanFree(p)) ? p.seats : null
  }

  const safe = cands.filter(orphanFree)
  if (safe.length === 0) return null
  return safe[0].seats
}

const unselectableSeatIds = computed(() => {
  const ids = new Set()
  if (!currentTargetSize.value || started.value || expired.value) return ids

  for (const slots of seatRowSlots.value.values()) {
    for (const e of slots) {
      if (!e || e.state !== 'FREE') continue
      if (!snapBlockAt(e.cell)) ids.add(e.cell.seatId)
    }
  }
  return ids
})

const isSeatUnselectable = (seat) => !!seat && unselectableSeatIds.value.has(seat.seatId)

// customSeatClass: trả về class CSS đặc biệt cho incident mode
// Trả về null để SeatGridRenderer dùng logic mặc định (màu VIP/Sweetbox/Standard/Unselectable)
function incidentSeatClass(seat) {
  if (!seat || seat.kind === 'AISLE' || seat.seatId == null) return null

  const isCanInteract = !started.value && !expired.value
  const isSource = bookingSeatIds.value.has(seat.seatId)
  const isDest = !started.value && !expired.value && chosenDest.value.has(seat.seatId)
  const isMaint = isSeatMaintenance(seat)

  const isDouble = isCoupleSeat(seat)
  const sizeClass = isDouble
    ? 'col-span-2 w-full h-8 rounded-xl justify-self-stretch'
    : 'w-8 h-8 aspect-square rounded-lg'
  const base = `${sizeClass} flex items-center justify-center text-[9px] font-bold border transition-all leading-none shrink-0 select-none`

  // 1. Ghế của đơn đang xử lý (source - SOLD)
  if (isSource) {
    if (isMaint) {
      const active = isCanInteract && activeSource.value === seat.seatId
      const cursor = isCanInteract ? 'cursor-pointer' : 'cursor-default'
      return `${base} ${cursor} bg-red-950/80 border-2 ${active
        ? 'border-blue-400 ring-2 ring-blue-500 text-white scale-110 shadow-lg'
        : 'border-blue-500 text-blue-200 hover:border-blue-400'
      }`
    }
    const active = isCanInteract && activeSource.value === seat.seatId
    if (active) {
      return `${base} cursor-pointer bg-gradient-to-br from-blue-400 to-blue-600 border-blue-300 text-white scale-[1.08] shadow-[0_0_20px_rgba(59,130,246,0.5)] z-10`
    }
    const hover = isCanInteract ? 'hover:brightness-110 hover:-translate-y-0.5 cursor-pointer' : 'cursor-default'
    return `${base} bg-blue-900/60 border-blue-500/70 text-blue-200 ${hover}`
  }

  // 2. Ghế đích đã chọn (chỉ khi có thể đổi ghế)
  if (isDest) {
    return `${base} bg-gradient-to-br from-green-400 to-green-600 border-green-300 text-white cursor-pointer shadow-[0_0_20px_rgba(34,197,94,0.5)] scale-[1.02] z-10`
  }

  // 3. Trả về null để SeatGridRenderer xử lý theo logic mặc định
  // (VIP đỏ, Sweetbox tím, Standard xám, SOLD mờ, MAINTENANCE xám red, UNSELECTABLE có dấu X)
  return null
}

// Tạo tooltip tương thích với SeatGridRenderer.customSeatTitle
function incidentSeatTitle(seat) {
  if (!seat || seat.seatId == null) return ''
  const isMaint = isSeatMaintenance(seat)
  const typeLbl = seatTypeLabel(seat.seatType)
  if (isMaint) return `Ghế ${seat.label} (${typeLbl} · Bảo trì / khóa)${!started.value && !expired.value ? ' — Chuột phải để mở lại' : ''}`
  if (bookingSeatIds.value.has(seat.seatId)) return `Ghế ${seat.label} (${typeLbl} · Ghế của đơn đang xử lý)`
  if (!started.value && !expired.value && chosenDest.value.has(seat.seatId)) return `Ghế ${seat.label} (${typeLbl} · Ghế đích đã chọn)`
  if (isSeatUnselectable(seat)) return `Ghế ${seat.label} (${typeLbl} · Không thể chọn vì sẽ để lại ghế trống đơn lẻ)`
  if (seat.status === 'AVAILABLE') {
    if (started.value || expired.value) return `Ghế ${seat.label} (${typeLbl} · Trống)`
    return `Ghế ${seat.label} (${typeLbl} · Trống) — Chuột phải để khóa bảo trì`
  }
  return `Ghế ${seat.label} (${typeLbl} · Đã bán)`
}

// bypass: cho phép click SOLD seats của đơn, và ghế MAINTENANCE của đơn
function incidentBypassClick(seat) {
  if (!seat) return false
  return bookingSeatIds.value.has(seat.seatId)
}

function onSeatClick(cell) {
  if (!cell || cell.kind === 'AISLE' || cell.seatId == null) return
  if (started.value || expired.value) return // Khi suất đã bắt đầu hoặc hết hạn: chặn click chọn trên sơ đồ ghế

  const isSource = bookingSeatIds.value.has(cell.seatId)
  if (isSource) {
    onSourceClick(cell.seatId)
    return
  }
  const isDest = chosenDest.value.has(cell.seatId)
  if (isDest) {
    // bỏ chọn đích: tìm source đang trỏ tới ghế này
    const src = Object.keys(swaps.value).find(k => swaps.value[k] === cell.seatId)
    if (src) { delete swaps.value[src]; swaps.value = { ...swaps.value } }
    return
  }
  if (isSeatUnselectable(cell)) {
    toast.warning('Không thể chọn vị trí này vì sẽ để lại 1 ghế trống đơn lẻ.')
    return
  }
  if (cell.status === 'AVAILABLE' && !isSeatMaintenance(cell) && mode.value === 'relocate') {
    let sourceId = activeSource.value
    if (sourceId == null) {
      const unassigned = soldSeats.value.filter(s => swaps.value[s.seatId] == null)
      if (unassigned.length === 1) {
        sourceId = unassigned[0].seatId
      } else {
        toast.info('Chọn ghế nguồn (viền xanh) trước, rồi bấm ghế đích.')
        return
      }
    }
    swaps.value = { ...swaps.value, [sourceId]: cell.seatId }
    activeSource.value = null
  }
}

function onSourceClick(seatId) {
  if (started.value || expired.value) return
  activeSource.value = activeSource.value === seatId ? null : seatId
}

function clearSwap(oldSeatId) {
  delete swaps.value[oldSeatId]
  swaps.value = { ...swaps.value }
}

function destLabel(oldSeatId) {
  const dest = swaps.value[oldSeatId]
  return dest != null ? (seatById.value.get(dest)?.label || '?') : null
}

function onSeatContextMenu(cell) {
  if (!cell || cell.kind === 'AISLE' || cell.seatId == null) return
  toggleSeatMaintenance(cell)
}

// ================= Khóa / Mở ghế bảo trì =================
async function toggleSeatMaintenance(seat) {
  const seatId = seat.seatId || seat.id
  const cell = seatById.value.get(seatId)
  const isMaint = isSeatMaintenance(cell) || seat.seatStatus === 'MAINTENANCE'
  const newStatus = isMaint ? 'AVAILABLE' : 'MAINTENANCE'
  const label = seat.seatLabel || seat.label || cell?.label || `Ghế #${seatId}`

  const ok = await confirm.show({
    title: isMaint ? 'Mở khóa ghế' : 'Báo hỏng ghế',
    message: isMaint
      ? `Mở lại ghế ${label} để bán bình thường?`
      : `Đánh dấu ghế ${label} là BẢO TRÌ? Ghế sẽ ngừng được bán ở mọi suất tiếp theo cho tới khi mở lại.`,
    confirmText: isMaint ? 'Mở lại' : 'Khóa ghế',
    tone: isMaint ? 'primary' : 'danger'
  })
  if (!ok) return
  try {
    await incidentApi.setSeatStatus(seatId, {
      status: newStatus,
      reason: isMaint ? 'Mở lại sau sửa chữa' : `Báo hỏng khi xử lý đơn ${ctx.value?.bookingCode || ''}`.trim()
    })
    toast.success(isMaint ? `Đã mở lại ghế ${label}.` : `Đã khóa ghế ${label} (bảo trì).`)
    await loadSeatMap()
  } catch (e) {
    toast.error(friendlyError(e, isMaint ? 'Không thể mở lại ghế.' : 'Không thể khóa ghế.'))
  }
}

// ================= Đền bù (payload) =================
function buildCompensation() {
  if (compChoice.value === 'NONE') return { type: 'NONE', note: compNote.value || null }
  const opt = compOptions.value.find(o => String(o.promotionId) === String(compChoice.value))
  if (!opt) return { type: 'NONE', note: compNote.value || null }
  return { type: opt.type, promotionTemplateId: opt.promotionId, note: compNote.value || null }
}

// ================= Xử lý đổi ghế =================
async function submitRelocate() {
  if (!canSubmitRelocate.value) { toast.warning('Chọn ít nhất một cặp đổi ghế.'); return }

  // Kiểm tra an toàn: đảm bảo không có ghế đích nào để lại ghế lẻ
  for (const swap of selectedSwaps.value) {
    const destSeat = seatById.value.get(swap.newSeatId)
    if (destSeat && isSeatUnselectable(destSeat)) {
      toast.warning(`Ghế đích ${destSeat.label} vi phạm quy định chặn ghế lẻ. Vui lòng chọn ghế khác.`)
      return
    }
  }

  const lines = selectedSwaps.value.map(s => `${seatById.value.get(s.oldSeatId)?.label || s.oldSeatId} → ${seatById.value.get(s.newSeatId)?.label || s.newSeatId}`).join(', ')
  const ok = await confirm.show({
    title: 'Xác nhận đổi ghế',
    message: `Đổi ghế: ${lines}. ${compChoice.value !== 'NONE' ? 'Sẽ phát đền bù kèm theo.' : 'Không phát đền bù.'}`,
    confirmText: 'Đổi & in lại', tone: 'primary'
  })
  if (!ok) return
  submitting.value = true
  try {
    const { data } = await incidentApi.relocate({
      bookingId: ctx.value.bookingId,
      swaps: selectedSwaps.value,
      compensation: buildCompensation(),
      reason: compNote.value || null,
      allowOrphan: false
    })
    result.value = data.data ?? data
    toast.success('Đã đổi ghế & xử lý đền bù.')
    await doLookup(true)
  } catch (e) {
    toast.error(friendlyError(e, 'Đổi ghế thất bại.'))
  } finally {
    submitting.value = false
  }
}

// ================= Lịch sử =================
async function loadHistory(page = 0) {
  loadingHist.value = true
  try {
    const params = { page, size: histSize }
    if (histFilters.value.type) params.type = histFilters.value.type
    if (histFilters.value.code) params.code = histFilters.value.code.trim()
    if (histFilters.value.from) params.from = histFilters.value.from
    if (histFilters.value.to) params.to = histFilters.value.to
    const { data } = await incidentApi.history(params)
    const pg = data.data ?? data
    histRows.value = pg.content || []
    histTotal.value = pg.totalElements ?? 0
    histPage.value = pg.number ?? page
  } catch (e) {
    toast.error(friendlyError(e, 'Không tải được lịch sử sự cố.'))
  } finally {
    loadingHist.value = false
  }
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'history' && histRows.value.length === 0) loadHistory(0)
}

const fmtPrice = (n) => (n != null ? Number(n).toLocaleString('vi-VN') + 'đ' : '0đ')
const fmtTime = (s) => (s ? new Date(s).toLocaleString('vi-VN') : '')
// BUG-12 FIX: Thêm label cho EMERGENCY_CLOSURE
const typeLabel = (t) => ({ RELOCATE: 'Đổi ghế', CANCEL: 'Hủy chỗ', SEAT_MAINTENANCE: 'Khóa ghế', EMERGENCY_CLOSURE: 'Đóng cửa khẩn cấp' }[t] || t)
const compLabel = (t) => ({ NONE: '—', DISCOUNT: 'Voucher giảm', GIFT_FNB: 'Quà F&B', GIFT_TICKET: 'Vé mời' }[t] || t)

onMounted(async () => {
  try {
    const { data } = await incidentApi.compensationOptions()
    compOptions.value = data.data ?? data
  } catch { /* im lặng: dropdown đền bù rỗng vẫn cho thao tác NONE */ }
})
</script>

<template>
  <div class="p-6 md:p-8 flex flex-col gap-6 min-h-full">
    <!-- Header -->
    <div class="flex items-center justify-between flex-wrap gap-4">
      <div>
        <h1 class="text-2xl font-black text-on-surface tracking-tight flex items-center gap-3">
          <span class="material-symbols-outlined text-primary">event_seat</span>
          Xử lý sự cố phòng chiếu
        </h1>
        <p class="text-sm text-on-surface-variant mt-1">Đổi ghế đền bù · Khóa ghế hỏng — đền bù bằng voucher (không hoàn tiền).</p>
      </div>
      <div class="flex gap-1 bg-surface-container-high rounded-xl p-1">
        <button @click="switchTab('handle')" :class="['px-4 py-2 rounded-lg text-sm font-semibold transition-all', activeTab === 'handle' ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:bg-white/5']">Xử lý</button>
        <button @click="switchTab('history')" :class="['px-4 py-2 rounded-lg text-sm font-semibold transition-all', activeTab === 'history' ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:bg-white/5']">Lịch sử</button>
      </div>
    </div>

    <!-- ============ TAB XỬ LÝ ============ -->
    <div v-if="activeTab === 'handle'" class="grid grid-cols-1 lg:grid-cols-[380px_1fr] gap-6">
      <!-- Cột trái: tra cứu + vé -->
      <div class="flex flex-col gap-4">
        <div class="bg-surface-container-high rounded-2xl p-4 border border-outline-variant/10">
          <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tra cứu vé sự cố</label>
          <div class="flex gap-2 mt-2">
            <input v-model="searchQuery" @keyup.enter="doLookup(false)" placeholder="Mã vé hoặc SĐT khách..."
                   class="flex-1 py-2.5 px-3 rounded-lg bg-surface border border-outline-variant/20 text-on-surface text-sm outline-none focus:border-primary transition-colors" />
            <button @click="doLookup(false)" :disabled="loadingCtx"
                    class="px-4 rounded-lg bg-primary text-on-primary font-bold text-sm hover:opacity-90 disabled:opacity-50 flex items-center">
              <span class="material-symbols-outlined text-lg">{{ loadingCtx ? 'hourglass_empty' : 'search' }}</span>
            </button>
          </div>
        </div>

        <!-- Picker: nhiều đơn cùng SĐT → nhân viên chọn -->
        <div v-if="phoneBookings.length > 0" class="bg-surface-container-high rounded-2xl p-4 border border-amber-500/20 flex flex-col gap-2">
          <p class="text-[11px] font-bold text-amber-400 uppercase tracking-widest">
            📋 Tìm thấy {{ phoneBookings.length }} đơn — chọn đơn cần xử lý:
          </p>
          <button
            v-for="bk in phoneBookings" :key="bk.bookingId"
            @click="selectPhoneBooking(bk)"
            class="w-full text-left rounded-xl border border-outline-variant/20 bg-surface px-3 py-2.5 hover:border-primary/50 hover:bg-primary/5 transition-all group"
          >
            <div class="flex items-center justify-between mb-0.5">
              <span class="font-black text-on-surface text-sm group-hover:text-primary transition-colors">#{{ bk.bookingCode }}</span>
              <span :class="['text-[10px] font-bold px-2 py-0.5 rounded uppercase', bk.channel === 'POS' ? 'bg-purple-500/20 text-purple-300' : 'bg-blue-500/20 text-blue-300']">{{ bk.channel }}</span>
            </div>
            <p class="text-[11px] text-on-surface-variant">
              {{ bk.showtime?.roomName }} · {{ bk.seats?.filter(s => s.status === 'SOLD').map(s => s.seatLabel).join(', ') || '—' }}
            </p>
          </button>
        </div>

        <!-- Empty state -->
        <div v-else-if="!ctx && !loadingCtx" class="bg-surface-container rounded-2xl p-8 text-center text-on-surface-variant border border-dashed border-outline-variant/20">
          <span class="material-symbols-outlined text-4xl opacity-40">receipt_long</span>
          <p class="text-sm mt-2">Nhập mã vé hoặc SĐT để bắt đầu xử lý sự cố.</p>
        </div>

        <!-- Vé đang xử lý -->
        <div v-if="ctx && !expired" class="bg-surface-container-high rounded-2xl p-4 border border-outline-variant/10 flex flex-col gap-3">
          <div class="flex items-center justify-between">
            <span class="font-black text-on-surface">#{{ ctx.bookingCode }}</span>
            <span :class="['text-[10px] font-bold px-2 py-0.5 rounded uppercase', ctx.channel === 'POS' ? 'bg-purple-500/20 text-purple-300' : 'bg-blue-500/20 text-blue-300']">{{ ctx.channel }}</span>
          </div>
          <div class="text-sm text-on-surface-variant space-y-1">
            <p><span class="material-symbols-outlined text-xs align-middle">movie</span> {{ ctx.showtime.movieTitle }}</p>
            <p><span class="material-symbols-outlined text-xs align-middle">meeting_room</span> {{ ctx.showtime.roomName }} · {{ fmtTime(ctx.showtime.startTime) }}</p>
            <p><span class="material-symbols-outlined text-xs align-middle">person</span>
              {{ ctx.hasCustomer ? (ctx.customerName || 'Khách') : 'Khách vãng lai' }}
              <span v-if="ctx.customerPhone"> · {{ ctx.customerPhone }}</span>
            </p>
          </div>

          <div v-if="expired" class="text-[11px] font-semibold text-red-400 bg-red-500/10 rounded-lg px-3 py-2">
            ⛔ Suất chiếu đã kết thúc — đã qua cửa sổ xử lý sự cố (2 giờ sau khi chiếu xong). Không thể thực hiện thao tác.
          </div>
          <div v-else-if="started" class="text-[11px] font-semibold text-amber-400 bg-amber-500/10 rounded-lg px-3 py-2">
            ⚠ Suất đã bắt đầu — không thể đổi ghế.
          </div>
          <div v-if="!ctx.hasCustomer" class="text-[11px] font-semibold text-blue-300 bg-blue-500/10 rounded-lg px-3 py-2">
            ℹ Khách vãng lai — đền trực tiếp tại quầy, hệ thống không phát voucher điện tử (chỉ ghi vết).
          </div>

          <!-- Danh sách ghế của đơn -->
          <div class="space-y-2">
            <div v-for="s in soldSeats" :key="s.bookingSeatId" class="bg-surface rounded-lg p-2.5 border border-outline-variant/10">
              <div class="flex items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <span class="font-bold text-on-surface text-sm">{{ s.seatLabel }}</span>
                  <span class="text-[10px] px-1.5 py-0.5 rounded bg-surface-container-high text-on-surface-variant">{{ s.seatType }}</span>
                  <span class="text-[10px] text-on-surface-variant">{{ s.ticketType }}</span>
                  <span v-if="isSeatMaintenance(seatById.get(s.seatId))" class="text-[10px] font-bold px-1.5 py-0.5 rounded bg-red-500/20 text-red-400 border border-red-500/30 flex items-center gap-0.5">
                    <span class="material-symbols-outlined text-[11px]">build</span> Bảo trì
                  </span>
                </div>
                <button @click="toggleSeatMaintenance(s)" :title="isSeatMaintenance(seatById.get(s.seatId)) ? 'Mở khóa ghế này' : 'Báo hỏng ghế này (bảo trì)'"
                        :class="['flex items-center p-1 rounded transition-colors', isSeatMaintenance(seatById.get(s.seatId)) ? 'text-amber-400 hover:text-amber-300 hover:bg-amber-400/10' : 'text-red-400 hover:text-red-300 hover:bg-red-400/10']">
                  <span class="material-symbols-outlined text-lg">{{ isSeatMaintenance(seatById.get(s.seatId)) ? 'lock_open' : 'build' }}</span>
                </button>
              </div>
              <!-- Relocate: hiển thị đích (chỉ hiển thị khi suất chiếu chưa bắt đầu) -->
              <div v-if="mode === 'relocate' && !started && !expired" class="flex items-center justify-between mt-2 text-xs">
                <button @click="onSourceClick(s.seatId)"
                        :class="['px-2 py-1 rounded font-semibold', activeSource === s.seatId ? 'bg-blue-500 text-white' : 'bg-blue-900/40 text-blue-200']">
                  {{ activeSource === s.seatId ? 'Chọn ghế đích trên sơ đồ →' : 'Chọn' }}
                </button>
                <div class="flex items-center gap-1">
                  <span v-if="destLabel(s.seatId)" class="text-green-400 font-bold">→ {{ destLabel(s.seatId) }}</span>
                  <span v-else class="text-on-surface-variant/50">chưa chọn đích</span>
                  <button v-if="destLabel(s.seatId)" @click="clearSwap(s.seatId)" class="text-red-400 ml-1"><span class="material-symbols-outlined text-sm">close</span></button>
                </div>
              </div>
            </div>
          </div>

          <!-- Đền bù -->
          <div class="border-t border-outline-variant/10 pt-3 space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Đền bù</label>
            <select v-model="compChoice" class="w-full py-2 px-3 rounded-lg bg-surface border border-outline-variant/20 text-on-surface text-sm outline-none focus:border-primary">
              <option value="NONE">Không đền bù</option>
              <option v-for="o in visibleCompOptions" :key="o.promotionId" :value="o.promotionId">
                {{ o.label }}{{ o.type === 'DISCOUNT' && o.discountValue > 0 ? ` (${fmtPrice(o.discountValue)})` : '' }}
              </option>
            </select>
            <input v-model="compNote" placeholder="Lý do / ghi chú (vd: ghế lỗi tựa lưng)" maxlength="255"
                   class="w-full py-2 px-3 rounded-lg bg-surface border border-outline-variant/20 text-on-surface text-sm outline-none focus:border-primary" />

            <button @click="submitRelocate" :disabled="!canSubmitRelocate || submitting"
                    class="w-full py-3 rounded-xl bg-primary text-on-primary font-extrabold text-sm hover:opacity-90 disabled:opacity-40 flex items-center justify-center gap-2">
              <span class="material-symbols-outlined text-lg">swap_horiz</span>
              {{ submitting ? 'Đang xử lý...' : 'Xác nhận & in lại vé' }}
            </button>
          </div>

          <!-- Kết quả -->
          <div v-if="result" class="bg-green-500/10 border border-green-500/30 rounded-xl p-4 text-xs text-green-300 space-y-2">
            <div class="flex items-center justify-between">
              <p class="font-bold flex items-center gap-1.5 text-sm text-green-200">
                <span class="material-symbols-outlined text-base text-green-400">check_circle</span>
                Xử lý thành công
              </p>
              <button @click="result = null" class="text-on-surface-variant hover:text-white" title="Đóng thông báo">
                <span class="material-symbols-outlined text-sm">close</span>
              </button>
            </div>
            <div v-if="result.compensation?.voucherIssued" class="p-2.5 rounded-lg bg-green-950/60 border border-green-500/30 space-y-1">
              <p class="text-green-200">Mã voucher đền bù cho khách:</p>
              <p class="text-base font-mono font-black text-green-400 tracking-wider select-all">{{ result.compensation.voucherCode }}</p>
              <!-- BUG-13 FIX: field đúng là .value (không phải .amount) theo CompensationResult Java record -->
              <p v-if="result.compensation.value > 0" class="text-[11px] text-green-300/80">Trị giá: {{ fmtPrice(result.compensation.value) }}</p>
            </div>
            <p v-else-if="result.compensation?.counterGift" class="text-amber-300">
              ℹ Khách vãng lai: Đền trực tiếp tại quầy — hệ thống không phát voucher điện tử.
            </p>
            <p v-if="result.emailResent" class="flex items-center gap-1 text-green-300/80">
              <span class="material-symbols-outlined text-xs">mark_email_read</span> Đã gửi lại email xác nhận cho khách.
            </p>
          </div>
        </div>
      </div>

      <!-- Cột phải: sơ đồ ghế -->
      <div class="bg-surface-container-high rounded-2xl p-6 border border-outline-variant/10 flex flex-col min-h-[480px]">
        <div v-if="loadingMap" class="flex-grow flex items-center justify-center text-on-surface-variant">
          <span class="material-symbols-outlined animate-spin text-4xl text-primary">progress_activity</span>
        </div>
        <div v-else-if="!seatMap" class="flex-grow flex flex-col items-center justify-center text-on-surface-variant/50 py-16">
          <span class="material-symbols-outlined text-5xl opacity-30">grid_on</span>
          <p class="text-sm mt-2">Sơ đồ ghế của suất sẽ hiển thị sau khi tra cứu vé.</p>
        </div>
        <div v-else class="flex flex-col gap-4 flex-grow">
          <!-- Tiêu đề phòng -->
          <div class="flex items-center justify-between shrink-0">
            <div>
              <p class="text-sm font-bold text-on-surface">{{ ctx?.showtime?.roomName }}</p>
              <p class="text-[11px] text-on-surface-variant">{{ ctx?.showtime?.movieTitle }}</p>
            </div>
            <div v-if="started && !expired" class="text-[10px] font-bold px-2.5 py-1 rounded-lg bg-amber-500/10 border border-amber-500/30 text-amber-400 flex items-center gap-1.5">
              <span class="material-symbols-outlined text-[14px]">lock</span> Chỉ xem — suất đã bắt đầu
            </div>
          </div>

          <!-- SeatGridRenderer: dùng đúng component của POS/Cinema -->
          <div class="flex-grow min-h-0 overflow-auto flex items-start justify-center">
            <SeatGridRenderer
              :seats="seatMap.seats"
              :matrix-row="seatMap.matrixRow"
              :matrix-col="seatMap.matrixCol"
              mode="pos"
              size="compact"
              :readonly="false"
              show-screen
              screen-title="MÀN HÌNH CHÍNH"
              show-row-labels
              :is-seat-unselectable="isSeatUnselectable"
              :custom-seat-class="incidentSeatClass"
              :custom-seat-title="incidentSeatTitle"
              :bypass-click-filter="incidentBypassClick"
              @seat-click="onSeatClick"
              @seat-contextmenu="onSeatContextMenu"
            />
          </div>

          <!-- Chú thích (legend) - matching POS design -->
          <div class="flex flex-wrap gap-x-4 gap-y-2 text-[10px] font-bold uppercase tracking-wider text-on-surface-variant shrink-0 pt-2 border-t border-outline-variant/10">
            <span class="flex items-center gap-1.5">
              <span class="w-3.5 h-3.5 rounded-md bg-blue-900/60 border border-blue-500/70"></span>
              Ghế của đơn
            </span>
            <span v-if="!started && !expired" class="flex items-center gap-1.5">
              <span class="w-3.5 h-3.5 rounded-md bg-gradient-to-br from-green-400 to-green-600"></span>
              Ghế đích
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3.5 h-3.5 rounded-md bg-slate-800/80 border border-slate-600/50"></span>
              Thường
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3.5 h-3.5 rounded-md bg-gradient-to-b from-red-700/90 to-red-900/90 border border-red-500/50"></span>
              VIP
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3.5 h-3.5 rounded-md bg-gradient-to-b from-purple-600/90 to-purple-900/90 border border-purple-500/50"></span>
              Sweetbox
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3.5 h-3.5 rounded-md bg-surface-container-highest border border-white/10 flex items-center justify-center text-red-500">
                <span class="material-symbols-outlined text-[8px]">build</span>
              </span>
              Bảo trì
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3.5 h-3.5 rounded-md bg-surface-container-high opacity-40 border border-white/5"></span>
              Đã bán
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3.5 h-3.5 rounded-md bg-surface-container-high border border-white/5 relative seat-unselectable"></span>
              Ghế lẻ bị chặn
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ TAB LỊCH SỬ ============ -->
    <div v-else class="bg-surface-container-high rounded-2xl p-5 border border-outline-variant/10">
      <div class="flex flex-wrap gap-3 items-end mb-4">
        <div>
          <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Loại</label>
          <select v-model="histFilters.type" class="block mt-1 py-2 px-3 rounded-lg bg-surface border border-outline-variant/20 text-on-surface text-sm outline-none">
            <option value="">Tất cả</option>
            <option value="RELOCATE">Đổi ghế</option>
            <option value="SEAT_MAINTENANCE">Khóa ghế</option>
            <!-- BUG-11 FIX: Thêm EMERGENCY_CLOSURE vào filter -->
            <option value="EMERGENCY_CLOSURE">Đóng cửa khẩn cấp</option>
          </select>
        </div>
        <div>
          <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mã vé</label>
          <input v-model="histFilters.code" class="block mt-1 py-2 px-3 rounded-lg bg-surface border border-outline-variant/20 text-on-surface text-sm outline-none" placeholder="BK..." />
        </div>
        <div>
          <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Từ ngày</label>
          <input v-model="histFilters.from" type="date" class="block mt-1 py-2 px-3 rounded-lg bg-surface border border-outline-variant/20 text-on-surface text-sm outline-none" />
        </div>
        <div>
          <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Đến ngày</label>
          <input v-model="histFilters.to" type="date" class="block mt-1 py-2 px-3 rounded-lg bg-surface border border-outline-variant/20 text-on-surface text-sm outline-none" />
        </div>
        <button @click="loadHistory(0)" class="py-2 px-4 rounded-lg bg-primary text-on-primary font-bold text-sm hover:opacity-90">Lọc</button>
      </div>

      <div v-if="loadingHist" class="py-12 text-center text-on-surface-variant"><span class="material-symbols-outlined animate-spin">progress_activity</span></div>
      <div v-else-if="histRows.length === 0" class="py-12 text-center text-on-surface-variant/50">
        <span class="material-symbols-outlined text-4xl opacity-30">history</span>
        <p class="text-sm mt-2">Chưa có sự cố nào được ghi nhận.</p>
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="text-[10px] uppercase tracking-widest text-on-surface-variant border-b border-outline-variant/10">
              <th class="text-left py-2 px-2">Thời gian</th>
              <th class="text-left py-2 px-2">Loại</th>
              <th class="text-left py-2 px-2">Mã vé</th>
              <th class="text-left py-2 px-2">Ghế</th>
              <th class="text-left py-2 px-2">Đền bù</th>
              <th class="text-left py-2 px-2">Voucher</th>
              <th class="text-left py-2 px-2">Nhân viên</th>
              <th class="text-left py-2 px-2">Lý do</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in histRows" :key="row.id" class="border-b border-outline-variant/5 hover:bg-white/5">
              <td class="py-2 px-2 text-on-surface-variant whitespace-nowrap">{{ fmtTime(row.createdAt) }}</td>
              <td class="py-2 px-2"><span class="text-xs font-bold px-2 py-0.5 rounded bg-surface-container-highest text-on-surface">{{ typeLabel(row.type) }}</span></td>
              <td class="py-2 px-2 font-mono text-on-surface">{{ row.bookingCode || '—' }}</td>
              <td class="py-2 px-2 text-on-surface">{{ row.oldSeatLabel }}<span v-if="row.newSeatLabel"> → {{ row.newSeatLabel }}</span></td>
              <td class="py-2 px-2 text-on-surface-variant">{{ compLabel(row.compensationType) }}<span v-if="row.compensationAmount > 0"> · {{ fmtPrice(row.compensationAmount) }}</span></td>
              <td class="py-2 px-2 font-mono text-xs text-primary">{{ row.voucherCode || '—' }}</td>
              <td class="py-2 px-2 text-on-surface-variant">{{ row.handledByName || '—' }}</td>
              <td class="py-2 px-2 text-on-surface-variant/70 text-xs max-w-[180px] truncate" :title="row.reason">{{ row.reason || '—' }}</td>
            </tr>
          </tbody>
        </table>
        <!-- Phân trang -->
        <div class="flex items-center justify-between mt-4 text-sm text-on-surface-variant">
          <span>Tổng {{ histTotal }} sự cố</span>
          <div class="flex gap-2">
            <button :disabled="histPage === 0" @click="loadHistory(histPage - 1)" class="px-3 py-1.5 rounded-lg bg-surface-container-high disabled:opacity-40">Trước</button>
            <button :disabled="(histPage + 1) * histSize >= histTotal" @click="loadHistory(histPage + 1)" class="px-3 py-1.5 rounded-lg bg-surface-container-high disabled:opacity-40">Sau</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
