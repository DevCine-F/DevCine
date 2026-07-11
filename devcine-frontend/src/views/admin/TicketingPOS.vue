<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { ticketingApi, settingsApi, approvalApi } from '@/api/admin/index'
import AppButton from '../../components/common/AppButton.vue'
import { useSeatRealtime } from '@/composables/useSeatRealtime'
import { useShiftStore } from '@/stores/shift'

const shiftStore = useShiftStore()
const currentStep = ref(1) // 1: Showtime, 2: Seats, 3: Confirm, 4: F&B, 5: Payment, 6: Done

const showtimes = ref([])
const combos = ref([])
const isLoading = ref(false)

const selectedShowtime = ref(null)
const seatData = ref({ matrixRow: 9, matrixCol: 10, seats: [] })
const isLoadingSeats = ref(false)
const selectedSeats = ref([]) // seat objects from map
const selectedCombos = ref([]) // { id, name, price, quantity }

const member = ref(null)
const cardNumberInput = ref('')
const isCheckingCard = ref(false)
const cardError = ref('')

const paymentMethod = ref('CASH')
const isPaying = ref(false)
const completedBooking = ref(null)

// Bảng giá theo đối tượng của suất đang chọn (tên loại ghế -> { mã đối tượng -> giá }) + nhãn đối tượng
const priceTable = ref({})
const audienceLabels = ref({})

// Voucher tại quầy — chỉ dùng sau khi tra cứu thành viên (voucher gắn với khách hàng)
const voucherCodeInput = ref('')
const appliedVoucher = ref(null) // { id, code, discountType, discountValue }
const ownedVouchers = ref([])
const isApplyingVoucher = ref(false)
const voucherError = ref('')

// Thanh toán: tài khoản nhận tiền (QR) + modal tiền mặt / QR
const bankInfo = ref({ code: '', name: '', accountNo: '', accountName: '' })
// Giới hạn số vé/lần đặt (cấu hình admin MAX_TICKETS_PER_BOOKING) — chặn sớm ngay khi chọn ghế
const maxTicketsPerBooking = ref(8)
const showCashModal = ref(false)
const showQrModal = ref(false)
const cashGiven = ref(0)

const error = ref('')
const canUseTicketing = computed(() => shiftStore.canUse(['POS_TICKETING']))
const canUseFnb = computed(() => shiftStore.canUse(['FNB']))
const isLocked = computed(() => !canUseTicketing.value && !canUseFnb.value)
const lockedMessage = computed(() => shiftStore.lockedMessage('bán vé POS hoặc quầy F&B'))

// Toast
const toast = ref({ show: false, type: 'success', message: '' })
let toastTimer = null
const showToast = (message, type = 'success') => {
  toast.value = { show: true, type, message }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value.show = false }, 3000)
}

const seatTypeLabel = (t) => ({ NORMAL: 'Thường', STANDARD: 'Thường', VIP: 'VIP', SWEETBOX: 'Sweetbox' }[t] || t)

const DEFAULT_AUDIENCE_LABELS = { ADULT: 'Người lớn', STUDENT: 'HSSV', CHILD: 'Trẻ em', SENIOR: 'Cao tuổi' }
// Lưu bảng giá + nhãn đối tượng từ response ghế để đổi loại vé không cần gọi lại server
const captureSeatMeta = (data) => {
  if (data && data.priceTable) priceTable.value = data.priceTable
  if (data && data.audienceLabels && Object.keys(data.audienceLabels).length) audienceLabels.value = data.audienceLabels
  if (!Object.keys(audienceLabels.value).length) audienceLabels.value = DEFAULT_AUDIENCE_LABELS
}
// Giá 1 ghế theo loại vé đang chọn (fallback về giá ADULT sẵn có nếu thiếu bảng giá)
const priceOf = (seat) => {
  const t = seat.ticketType || 'ADULT'
  const byType = priceTable.value[seat.seatType]
  const p = byType ? byType[t] : null
  return Number(p != null ? p : (seat.price || 0))
}

const fmt = (n) => Number(n || 0).toLocaleString('vi-VN')

// ===== Loại vé theo SỐ LƯỢNG (counter) thay cho dropdown từng ghế =====
// Nguồn sự thật hiển thị là số lượng theo đối tượng; khi khớp đủ số ghế sẽ gán
// ticketType cho từng ghế (theo thứ tự) để giữ nguyên cách tính giá + payload.
const ticketCounts = ref({}) // mã đối tượng -> số vé
const totalTicketCount = computed(() =>
  Object.values(ticketCounts.value).reduce((a, b) => a + (Number(b) || 0), 0))
const ticketsMatchSeats = computed(() =>
  selectedSeats.value.length > 0 && totalTicketCount.value === selectedSeats.value.length)

// Dựng lại counts từ ticketType hiện có của ghế (khi vào bước xác nhận / đổi ghế)
const syncTicketCountsFromSeats = () => {
  const counts = {}
  Object.keys(audienceLabels.value).forEach(k => { counts[k] = 0 })
  if (counts.ADULT == null) counts.ADULT = 0
  for (const s of selectedSeats.value) {
    const t = s.ticketType || 'ADULT'
    counts[t] = (counts[t] || 0) + 1
  }
  ticketCounts.value = counts
}

// Gán loại vé cho từng ghế theo counts (thứ tự ghế) → priceOf/seatTypeBreakdown tự cập nhật
const assignTicketCountsToSeats = () => {
  const order = []
  for (const [code, qty] of Object.entries(ticketCounts.value)) {
    for (let i = 0; i < (Number(qty) || 0); i++) order.push(code)
  }
  selectedSeats.value.forEach((s, i) => { s.ticketType = order[i] || 'ADULT' })
}

const setTicketCount = (code, delta) => {
  const cur = Number(ticketCounts.value[code] || 0)
  if (delta > 0 && totalTicketCount.value >= selectedSeats.value.length) return // không vượt số ghế
  const next = Math.max(0, cur + delta)
  ticketCounts.value = { ...ticketCounts.value, [code]: next }
  assignTicketCountsToSeats()
}

// Vào bước 3 (xác nhận vé) → đồng bộ counter với các ghế đang chọn
watch(currentStep, (step) => { if (step === 3) syncTicketCountsFromSeats() })

// ===== Định dạng & validate suất chiếu =====
const isPastShowtime = (st) => !!(st?.startTime) && new Date(st.startTime).getTime() < Date.now()
// Giờ 24h (HH:mm)
const fmtTime = (iso) => new Date(iso).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false })
// Mã suất chiếu theo chuẩn nội bộ: SC-YYYYMMDD-<id>
const showtimeCode = (st) => {
  if (!st?.startTime) return ''
  const d = new Date(st.startTime)
  const ymd = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`
  return `SC-${ymd}-${String(st.id).padStart(2, '0')}`
}
// Nhãn định dạng phòng in đậm, tô màu để tránh nhân viên chọn nhầm
const formatTone = (name) => {
  const n = String(name || '').toUpperCase()
  if (n.includes('IMAX')) return 'text-cyan-300 border-cyan-400/40 bg-cyan-500/10'
  if (n.includes('GOLD')) return 'text-amber-300 border-amber-400/40 bg-amber-500/10'
  return 'text-on-surface border-outline-variant/25 bg-surface-container-highest'
}

// ===== Giữ ghế tạm thời (Seat Holding Timer) + đồng bộ trạng thái ghế real-time =====
const HOLD_SECONDS = 5 * 60       // 5 phút giữ ghế
const SEAT_POLL_MS = 12000        // 12s/lần kiểm tra ghế bị kênh khác đặt
const holdRemaining = ref(0)      // giây còn lại
let holdTimer = null
let seatPollTimer = null
const holdActive = computed(() => holdRemaining.value > 0 && selectedSeats.value.length > 0)
const holdMmSs = computed(() => {
  const s = Math.max(0, holdRemaining.value)
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
})
const holdUrgent = computed(() => holdRemaining.value > 0 && holdRemaining.value <= 60)

const startHoldTimer = () => {
  holdRemaining.value = HOLD_SECONDS
  if (holdTimer) clearInterval(holdTimer)
  holdTimer = setInterval(() => {
    holdRemaining.value--
    if (holdRemaining.value <= 0) expireHold()
  }, 1000)
}
const stopHoldTimer = () => {
  if (holdTimer) { clearInterval(holdTimer); holdTimer = null }
  holdRemaining.value = 0
}
const expireHold = () => {
  stopHoldTimer()
  selectedSeats.value = []
  showToast('Hết thời gian giữ ghế — ghế đã được giải phóng cho khách khác. Vui lòng chọn lại.', 'error')
  currentStep.value = 2
  refreshSeats()
}

// Tải lại trạng thái ghế; gỡ khỏi giỏ ghế vừa bị App/Web/quầy khác mua
const refreshSeats = async () => {
  if (!selectedShowtime.value) return
  try {
    const { data } = await ticketingApi.getSeats(selectedShowtime.value.id)
    const fresh = data.seats
      ? data
      : { matrixRow: seatData.value.matrixRow, matrixCol: seatData.value.matrixCol, seats: Array.isArray(data) ? data : [] }
    seatData.value = fresh
    const freeIds = new Set(fresh.seats.filter(s => s.status === 'AVAILABLE').map(s => s.seatId))
    const lost = selectedSeats.value.filter(s => !freeIds.has(s.seatId))
    if (lost.length) {
      selectedSeats.value = selectedSeats.value.filter(s => freeIds.has(s.seatId))
      showToast(`Ghế ${lost.map(s => s.rowChar + s.colNum).join(', ')} vừa bị khách khác đặt — đã gỡ khỏi đơn.`, 'error')
      if (selectedSeats.value.length === 0) stopHoldTimer()
    }
  } catch (_) { /* im lặng — lần poll sau thử lại */ }
}
const startSeatPolling = () => {
  if (selectedShowtime.value) seatRealtime.connect(selectedShowtime.value.id) // khóa ghế real-time
  if (seatPollTimer) clearInterval(seatPollTimer)
  seatPollTimer = setInterval(() => {
    if (selectedShowtime.value && currentStep.value >= 2 && currentStep.value <= 5) refreshSeats()
  }, SEAT_POLL_MS)
}
const stopSeatPolling = () => {
  if (seatPollTimer) { clearInterval(seatPollTimer); seatPollTimer = null }
  seatRealtime.disconnect() // rời bước chọn ghế → nhả khóa của mình + ngắt WebSocket
}

// ===== Khóa ghế real-time (WebSocket/STOMP) — đồng bộ với quầy POS khác & khách online =====
const seatRealtime = useSeatRealtime({
  by: 'Quầy POS',
  // Ghế mình vừa chọn nhưng quầy khác đã giành trước → gỡ khỏi đơn + báo lỗi
  onDenied: (seatId) => {
    const lost = selectedSeats.value.find(s => s.seatId === seatId)
    selectedSeats.value = selectedSeats.value.filter(s => s.seatId !== seatId)
    if (selectedSeats.value.length === 0) stopHoldTimer()
    const label = lost ? lost.rowChar + lost.colNum : 'này'
    showToast(`Ghế ${label} vừa được chọn hoặc đã được bán ở quầy khác. Vui lòng chọn vị trí ghế khác!`, 'error')
  },
  // Ghế bị bán ở nơi khác trong lúc đang chọn → gỡ khỏi đơn nếu có
  onSold: (seatIds) => {
    const lost = selectedSeats.value.filter(s => seatIds.includes(s.seatId))
    if (lost.length) {
      selectedSeats.value = selectedSeats.value.filter(s => !seatIds.includes(s.seatId))
      if (selectedSeats.value.length === 0) stopHoldTimer()
      showToast(`Ghế ${lost.map(s => s.rowChar + s.colNum).join(', ')} vừa được bán ở quầy khác — đã gỡ khỏi đơn.`, 'error')
    }
  },
})
const isSeatLockedByOthers = (seat) => !!seat && seatRealtime.isLockedByOthers(seat.seatId)

// ===== Hoá đơn chờ (Hold Order / Pending List) — lưu localStorage để bền qua refresh =====
const HELD_KEY = 'devcine_pos_held_orders'
const HELD_SEQ_KEY = 'devcine_pos_hold_seq'
const heldOrders = ref([])
const showHeldPanel = ref(false)
const loadHeldOrders = () => {
  try { heldOrders.value = JSON.parse(localStorage.getItem(HELD_KEY) || '[]') } catch { heldOrders.value = [] }
}
const persistHeld = () => { try { localStorage.setItem(HELD_KEY, JSON.stringify(heldOrders.value)) } catch (_) {} }
const nextHoldCode = () => {
  const seq = parseInt(localStorage.getItem(HELD_SEQ_KEY) || '0', 10) + 1
  localStorage.setItem(HELD_SEQ_KEY, String(seq))
  return 'HOLD-' + String(seq).padStart(3, '0')
}
const canHoldOrder = computed(() => {
  if (saleMode.value === 'FNB') return selectedCombos.value.length > 0
  return !!selectedShowtime.value && selectedSeats.value.length > 0
})
const isHolding = ref(false)
const holdCurrentOrder = async () => {
  if (saleMode.value === 'TICKET' && !canUseTicketing.value) { showToast(shiftStore.lockedMessage('bán vé POS'), 'error'); return }
  if (saleMode.value === 'FNB' && !canUseFnb.value) { showToast(shiftStore.lockedMessage('quầy F&B'), 'error'); return }
  if (!canHoldOrder.value) { showToast('Chưa có gì để giữ đơn (giỏ hàng đang trống).', 'error'); return }
  // Giới hạn số đơn chờ cùng lúc để tránh treo rác bộ nhớ tạm
  if (heldOrders.value.length >= HELD_MAX) {
    showToast(`Tối đa ${HELD_MAX} đơn chờ cùng lúc. Hãy xử lý bớt đơn đang treo trước.`, 'error')
    return
  }
  // Đơn CÓ VÉ → tạo HOLD trong DB để khoá ghế toàn hệ thống (online/quầy khác không chọn được)
  let bookingId = null
  if (saleMode.value === 'TICKET') {
    isHolding.value = true
    try {
      const { data } = await ticketingApi.hold({
        showtimeId: selectedShowtime.value.id,
        seatIds: selectedSeats.value.map(s => s.seatId),
        customerId: member.value ? member.value.customerId : null,
      })
      if (!data.success) { showToast(data.message || 'Không giữ được ghế.', 'error'); return }
      bookingId = data.bookingId
    } catch (err) {
      showToast(err.response?.data?.message || 'Không giữ được ghế (có thể vừa bị đặt).', 'error')
      return
    } finally {
      isHolding.value = false
    }
  }
  const code = nextHoldCode()
  heldOrders.value.unshift({
    code,
    createdAt: Date.now(),
    mode: saleMode.value,
    bookingId,
    // Lưu lại bước hiện tại để khi "Gọi lại" đưa thẳng về đúng màn hình (vd thanh toán)
    step: saleMode.value === 'FNB' ? fnbStep.value : currentStep.value,
    showtime: selectedShowtime.value ? JSON.parse(JSON.stringify(selectedShowtime.value)) : null,
    seats: JSON.parse(JSON.stringify(selectedSeats.value)),
    combos: JSON.parse(JSON.stringify(selectedCombos.value)),
    member: member.value ? JSON.parse(JSON.stringify(member.value)) : null,
    // Số tiền GỐC trên biên lai tạm tính — KHÔNG lấy số tiền khách đưa đang gõ dở
    total: totalPrice.value,
  })
  // Huỷ/đóng luồng thanh toán đang mở (pop-up tiền mặt / QR) khi chuyển đơn sang chờ
  showCashModal.value = false
  showQrModal.value = false
  cashGiven.value = 0
  persistHeld()
  showToast(saleMode.value === 'TICKET'
    ? `Đã giữ đơn ${code}. Ghế đã được khoá trên toàn hệ thống.`
    : `Đã giữ đơn ${code}. Gọi lại bất cứ lúc nào ở "Đơn chờ".`, 'success')
  softReset()
}
const restoreHeldOrder = async (o) => {
  // Đơn chờ F&B độc lập: khôi phục thẳng giỏ món, không cần suất/ghế
  if (o.mode === 'FNB') {
    saleMode.value = 'FNB'
    selectedShowtime.value = null
    selectedSeats.value = []
    selectedCombos.value = o.combos || []
    member.value = o.member || null
    concessionSale.value = null
    // Đưa về đúng bước đã giữ (vd đang ở màn thanh toán) thay vì bắt làm lại
    fnbStep.value = o.step || 1
    showHeldPanel.value = false
    heldOrders.value = heldOrders.value.filter(x => x.code !== o.code)
    persistHeld()
    return
  }
  // Vé phim: suất đã quá giờ → nhả ghế + bỏ đơn chờ (đơn đã vô dụng)
  if (isPastShowtime(o.showtime)) {
    if (o.bookingId) { try { await ticketingApi.releaseHold(o.bookingId) } catch (_) {} }
    heldOrders.value = heldOrders.value.filter(x => x.code !== o.code)
    persistHeld()
    showToast(`Suất chiếu của đơn ${o.code} đã quá giờ — đã huỷ đơn chờ và nhả ghế.`, 'error')
    return
  }
  // Nhả HOLD của đơn này (ghế về AVAILABLE) trước khi tái dựng + kiểm tra ghế còn không.
  // Đồng thời double-check: nếu đơn đã CONFIRMED (đã thanh toán) thì không cho gọi lại.
  if (o.bookingId) {
    try {
      const { data } = await ticketingApi.releaseHold(o.bookingId)
      if (data.status === 'CONFIRMED') {
        heldOrders.value = heldOrders.value.filter(x => x.code !== o.code)
        persistHeld()
        showHeldPanel.value = false
        showToast(`Đơn ${o.code} đã được thanh toán trước đó — không thể gọi lại.`, 'error')
        return
      }
    } catch (_) { /* nhả lỗi: scheduler sẽ dọn; vẫn tiếp tục kiểm tra ghế thực tế */ }
  }
  saleMode.value = 'TICKET'
  selectedShowtime.value = o.showtime
  selectedCombos.value = o.combos || []
  member.value = o.member || null
  showHeldPanel.value = false
  currentStep.value = 2
  isLoadingSeats.value = true
  try {
    const { data } = await ticketingApi.getSeats(o.showtime.id)
    seatData.value = data.seats ? data : { matrixRow: 9, matrixCol: 10, seats: Array.isArray(data) ? data : [] }
    captureSeatMeta(data)
    // Quét lại tính khả dụng theo thời gian thực: ghế đã bị bán trong lúc chờ sẽ bị loại,
    // GIỮ LẠI phần bắp nước hợp lệ; chỉ phần vé trùng bị gỡ khỏi biên lai tạm tính.
    const byId = new Map(seatData.value.seats.map(s => [s.seatId, s]))
    const restored = []; const lost = []
    for (const s of (o.seats || [])) {
      const cur = byId.get(s.seatId)
      if (cur && cur.status === 'AVAILABLE') { cur.ticketType = s.ticketType || 'ADULT'; restored.push(cur) } else lost.push(s)
    }
    selectedSeats.value = restored
    if (lost.length) {
      const labels = lost.map(s => s.rowChar + s.colNum).join(', ')
      showToast(`Ghế ${labels} đã được bán cho khách hàng khác, vui lòng chọn lại ghế mới.`, 'error')
    }
    if (restored.length) startHoldTimer()
    startSeatPolling()
    // Mất ghế → buộc về bước chọn ghế để chọn lại; còn nguyên → về đúng bước đã giữ (vd thanh toán)
    currentStep.value = lost.length ? 2 : (o.step || 2)
  } catch (_) {
    showToast('Không tải được sơ đồ ghế của đơn chờ.', 'error')
  } finally {
    isLoadingSeats.value = false
  }
  heldOrders.value = heldOrders.value.filter(x => x.code !== o.code)
  persistHeld()
}
const deleteHeldOrder = (o) => {
  // Đơn có vé → nhả ghế HOLD về AVAILABLE ngay theo thời gian thực
  if (o.bookingId) { ticketingApi.releaseHold(o.bookingId).catch(() => {}) }
  heldOrders.value = heldOrders.value.filter(x => x.code !== o.code)
  persistHeld()
  showToast(`Đã huỷ đơn chờ ${o.code}${o.bookingId ? ' — ghế được giải phóng' : ''}.`, 'success')
}

// Cấu hình đơn chờ
const HELD_MAX = 10                  // tối đa số đơn chờ cùng lúc / quầy
const HELD_TICKET_TTL = HOLD_SECONDS // đơn chờ CÓ VÉ hết hạn sau 5 phút (giải phóng ghế)
const nowTs = ref(Date.now())        // nhịp đồng hồ 1s để countdown/tuổi đơn cập nhật reactive
let nowTimer = null

// Chỉ hiện suất chưa tới giờ chiếu; suất quá giờ tự ẩn theo nhịp đồng hồ
const visibleShowtimes = computed(() =>
  showtimes.value.filter(st => !(st?.startTime && new Date(st.startTime).getTime() < nowTs.value))
)

// Số giây còn lại của đơn chờ có vé (null = đơn F&B, không hết hạn)
const heldRemainingSec = (o) => {
  if (o.mode === 'FNB' || !(o.seats && o.seats.length)) return null
  return Math.max(0, HELD_TICKET_TTL - Math.floor((nowTs.value - o.createdAt) / 1000))
}
const heldCountdown = (o) => {
  const s = heldRemainingSec(o)
  if (s == null) return ''
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}
// Tự xoá đơn chờ có vé khi hết giờ giữ → giải phóng ghế cho khách khác
const sweepExpiredHolds = () => {
  const expired = heldOrders.value.filter(o => heldRemainingSec(o) === 0)
  if (!expired.length) return
  // Nhả ghế HOLD của các đơn vừa hết giờ về AVAILABLE
  expired.forEach(o => { if (o.bookingId) ticketingApi.releaseHold(o.bookingId).catch(() => {}) })
  heldOrders.value = heldOrders.value.filter(o => heldRemainingSec(o) !== 0)
  persistHeld()
  showToast(`Đơn chờ ${expired.map(o => o.code).join(', ')} đã hết giờ giữ — ghế được giải phóng.`, 'error')
}
const heldAgeLabel = (ts) => {
  const mins = Math.floor((nowTs.value - ts) / 60000)
  if (mins < 1) return 'vừa xong'
  if (mins < 60) return `${mins} phút trước`
  return `${Math.floor(mins / 60)} giờ trước`
}

// Xác nhận trước khi xoá đơn chờ (chống bấm nhầm)
const confirmDeleteHold = ref(null)
const askDeleteHeldOrder = (o) => { confirmDeleteHold.value = o }
const cancelDeleteHeldOrder = () => { confirmDeleteHold.value = null }
const confirmDeleteHeldOrder = () => {
  if (confirmDeleteHold.value) deleteHeldOrder(confirmDeleteHold.value)
  confirmDeleteHold.value = null
}

// Dọn khu làm việc về bước 1 mà KHÔNG tải lại danh sách suất/combo (dùng sau khi giữ đơn)
const softReset = () => {
  stopHoldTimer(); stopSeatPolling()
  currentStep.value = 1
  selectedShowtime.value = null
  seatData.value = { matrixRow: 9, matrixCol: 10, seats: [] }
  selectedSeats.value = []
  selectedCombos.value = []
  member.value = null
  cardNumberInput.value = ''
  cardError.value = ''
  concessionSale.value = null
  fnbStep.value = 1
  showCashModal.value = false
  showQrModal.value = false
  cashGiven.value = 0
  clearVoucherState()
}

// ===== Hai luồng bán: TICKET (vé + F&B) và FNB (bán nhanh bắp nước độc lập) =====
const saleMode = ref('TICKET')     // 'TICKET' | 'FNB'
const fnbStep = ref(1)             // luồng F&B: 1 = chọn món, 2 = thanh toán, 3 = hoàn tất
const concessionSale = ref(null)   // kết quả đơn F&B đã thanh toán

// Yêu cầu Trưởng ca duyệt HỦY hóa đơn F&B bấm nhầm (nhân viên quầy KHÔNG tự hủy được)
const showVoidForm = ref(false)
const voidReason = ref('')
const voidRequested = ref(false)
const isRequestingVoid = ref(false)

const resetVoidState = () => {
  showVoidForm.value = false
  voidReason.value = ''
  voidRequested.value = false
  isRequestingVoid.value = false
}

const handleRequestVoid = async () => {
  const saleId = concessionSale.value?.saleId
  if (!saleId) { showToast('Không xác định được hóa đơn.', 'error'); return }
  isRequestingVoid.value = true
  try {
    await approvalApi.requestFnbVoid(saleId, voidReason.value?.trim() || null)
    voidRequested.value = true
    showVoidForm.value = false
    showToast('Đã gửi yêu cầu hủy — chờ Trưởng ca duyệt.', 'success')
  } catch (e) {
    showToast(e?.response?.data?.message || 'Gửi yêu cầu hủy thất bại.', 'error')
  } finally {
    isRequestingVoid.value = false
  }
}

const switchMode = (mode) => {
  if (saleMode.value === mode) return
  if (mode === 'TICKET' && !canUseTicketing.value) { showToast(shiftStore.lockedMessage('bán vé POS'), 'error'); return }
  if (mode === 'FNB' && !canUseFnb.value) { showToast(shiftStore.lockedMessage('quầy F&B'), 'error'); return }
  saleMode.value = mode
  // Đổi luồng → dọn sạch khu làm việc để tránh lẫn dữ liệu giữa 2 kiểu bán
  stopHoldTimer(); stopSeatPolling()
  selectedShowtime.value = null
  seatData.value = { matrixRow: 9, matrixCol: 10, seats: [] }
  selectedSeats.value = []
  selectedCombos.value = []
  member.value = null
  cardNumberInput.value = ''
  cardError.value = ''
  completedBooking.value = null
  concessionSale.value = null
  resetVoidState()
  clearVoucherState()
  currentStep.value = 1
  fnbStep.value = 1
  showCashModal.value = false
  showQrModal.value = false
  cashGiven.value = 0
}

// Sẵn sàng thanh toán: TICKET cần ghế, FNB cần ít nhất 1 món
const checkoutReady = () => {
  if (saleMode.value === 'FNB') {
    if (selectedCombos.value.length === 0) { showToast('Chưa chọn món nào.', 'error'); return false }
    return true
  }
  if (selectedSeats.value.length === 0) { showToast('Chưa chọn ghế.', 'error'); return false }
  return true
}

const processConcessionPayment = async (method) => {
  if (!canUseFnb.value) { showToast(shiftStore.lockedMessage('quầy F&B'), 'error'); return }
  if (selectedCombos.value.length === 0) { showToast('Chưa chọn món nào.', 'error'); return }
  paymentMethod.value = method
  isPaying.value = true
  try {
    const payload = {
      fnbs: selectedCombos.value.map(c => ({ fnbItemId: c.id, quantity: c.quantity })),
      customerId: member.value ? member.value.customerId : null,
      paymentMethod: method,
    }
    const { data } = await ticketingApi.concession(payload)
    if (data.success) {
      concessionSale.value = data
      showCashModal.value = false
      showQrModal.value = false
      fnbStep.value = 3
    } else {
      showToast(data.message || 'Thanh toán thất bại.', 'error')
    }
  } catch (err) {
    showToast(err.response?.data?.message || 'Thanh toán thất bại.', 'error')
  } finally {
    isPaying.value = false
  }
}

// ===== Hoá đơn bắp nước độc lập (không vé/QR) =====
const buildConcessionInvoiceHtml = () => {
  const saleCode = esc(concessionSale.value?.saleCode)
  const printedAt = new Date().toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
  const itemRows = selectedCombos.value
    .map(c => `<tr><td>${esc(c.name)}</td><td class="c">${c.quantity}</td><td class="r">${fmt(c.price)}đ</td><td class="r b">${fmt(c.price * c.quantity)}đ</td></tr>`)
    .join('')
  return `<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8" />
<title>Hoá đơn ${saleCode} — DevCine</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@600;700;800;900&family=Inter:wght@400;500;600;700;800&display=swap');
  *{box-sizing:border-box;margin:0;padding:0}
  body{font-family:'Inter',system-ui,Arial,sans-serif;background:#efe8da;color:#26221b;padding:34px 20px;font-size:14px;-webkit-print-color-adjust:exact;print-color-adjust:exact}
  .serif{font-family:'Playfair Display',Georgia,serif}
  .g{background:linear-gradient(135deg,#e6c878,#c4992f);-webkit-background-clip:text;background-clip:text;color:transparent}
  .bar{display:flex;justify-content:center;gap:12px;max-width:620px;margin:0 auto 26px}
  .bar button{border:0;border-radius:999px;padding:13px 30px;font-weight:700;font-size:11px;letter-spacing:.14em;text-transform:uppercase;cursor:pointer}
  .btn-print{background:linear-gradient(135deg,#dcb869,#b8902f);color:#1c1a17;box-shadow:0 8px 22px rgba(184,144,47,.4)}
  .btn-close{background:#fff;color:#6b6456;border:1px solid #ddd4c1}
  .bill{max-width:620px;margin:0 auto;background:#fffdf8;border-radius:20px;overflow:hidden;box-shadow:0 30px 70px rgba(40,34,22,.18);border:1px solid #ece3d0}
  .bill-head{display:flex;justify-content:space-between;align-items:center;padding:32px 40px 26px;background:linear-gradient(160deg,#211d16,#14110c);color:#f3ecdc;position:relative}
  .bill-head::after{content:'';position:absolute;left:0;right:0;bottom:0;height:3px;background:linear-gradient(90deg,#b8902f,#e6c878,#b8902f)}
  .brand{display:flex;align-items:center;gap:14px}
  .mono{width:50px;height:50px;border-radius:14px;background:linear-gradient(135deg,#e9cd80,#b8902f);display:flex;align-items:center;justify-content:center;font-size:30px;font-weight:900;color:#1c1a17;flex:none}
  .brand-name{font-size:24px;font-weight:800;letter-spacing:.2em;line-height:1}
  .brand-tag{font-size:9px;letter-spacing:.3em;text-transform:uppercase;color:#a89c81;margin-top:6px}
  .doc{text-align:right}
  .doc-t{font-size:17px;letter-spacing:.3em;text-transform:uppercase;color:#e6c878}
  .doc-meta{margin-top:11px;font-size:11px;line-height:1.9;color:#a89c81}
  .doc-meta b{color:#f3ecdc;font-weight:600}
  .feature{display:flex;align-items:center;gap:14px;padding:22px 40px;border-bottom:1px solid #efe6d3}
  .feature .ico{width:44px;height:44px;border-radius:12px;background:#f7efe0;display:flex;align-items:center;justify-content:center;font-size:22px;flex:none}
  .feature h2{font-size:21px;font-weight:700;color:#211d16}
  .feature p{color:#8c836d;font-size:12px;margin-top:4px}
  table{width:100%;border-collapse:collapse}
  thead th{font-size:9.5px;letter-spacing:.18em;text-transform:uppercase;color:#a89c81;text-align:left;padding:18px 40px 8px;font-weight:700}
  thead th.c{text-align:center}thead th.r{text-align:right}
  tbody td{padding:13px 40px;font-size:13.5px;border-top:1px solid #f3ecdd;color:#3a342a}
  td.c{text-align:center}td.r{text-align:right;font-variant-numeric:tabular-nums}td.b{font-weight:700;color:#211d16}
  .summary{padding:16px 40px 8px;display:flex;flex-direction:column;align-items:flex-end}
  .s-grand{display:flex;justify-content:space-between;align-items:baseline;width:min(320px,100%);padding-top:16px;border-top:2px solid #211d16}
  .s-grand span{font-size:11px;letter-spacing:.18em;text-transform:uppercase;color:#8c836d;font-weight:700}
  .s-grand b{font-size:30px;font-weight:800;color:#211d16;font-variant-numeric:tabular-nums;line-height:1}
  .s-grand b .u{font-size:16px;color:#b8902f;margin-left:3px}
  .foot{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-top:18px;padding:20px 40px;background:#faf4e9;border-top:1px solid #efe6d3}
  .foot .pm{font-size:12px;color:#6f6755}.foot .pm b{color:#26221b;font-weight:600}
  .stamp{font-size:10px;font-weight:800;letter-spacing:.16em;text-transform:uppercase;color:#b8902f;border:1.5px solid #dcb869;border-radius:9px;padding:9px 15px;transform:rotate(-4deg);white-space:nowrap;flex:none}
  .thanks{text-align:center;font-style:italic;font-size:14px;color:#9a8f76;padding:18px}
  @media print{body{background:#fff;padding:0}.bar{display:none}.bill{box-shadow:none;border-radius:0;max-width:100%;border:0;margin:0}}
</style></head>
<body>
  <div class="bar">
    <button class="btn-print" onclick="window.print()">🖨 In hoá đơn</button>
    <button class="btn-close" onclick="window.close()">Đóng</button>
  </div>
  <section class="bill">
    <div class="bill-head">
      <div class="brand">
        <div class="mono serif">D</div>
        <div><div class="brand-name">DEV<span class="g">CINE</span></div><div class="brand-tag">Cinema · Quầy bắp nước</div></div>
      </div>
      <div class="doc">
        <div class="doc-t serif">Hoá Đơn</div>
        <div class="doc-meta">Số: <b>${saleCode}</b><br/>Ngày in: <b>${esc(printedAt)}</b><br/>Quầy: <b>POS · Concession</b></div>
      </div>
    </div>
    <div class="feature">
      <div class="ico">🍿</div>
      <div><h2 class="serif">Bắp nước &amp; Combo</h2><p>Bán nhanh tại quầy — không kèm vé xem phim</p></div>
    </div>
    <table>
      <thead><tr><th>Nội dung</th><th class="c">SL</th><th class="r">Đơn giá</th><th class="r">Thành tiền</th></tr></thead>
      <tbody>${itemRows}</tbody>
    </table>
    <div class="summary">
      <div class="s-grand"><span>Tổng thanh toán</span><b class="serif">${fmt(comboTotal.value)}<span class="u">đ</span></b></div>
    </div>
    <div class="foot">
      <div class="pm">Phương thức: <b>${esc(paymentLabel(paymentMethod.value))}</b>${member.value ? `<br/>Thành viên: <b>${esc(member.value.fullName)}</b> · ${esc(member.value.membershipTier)}` : ''}</div>
      <div class="stamp">Đã thanh toán</div>
    </div>
    <div class="thanks serif">Cảm ơn quý khách &amp; hẹn gặp lại tại DevCine</div>
  </section>
</body></html>`
}
const printConcessionInvoice = () => {
  if (!concessionSale.value) return
  const win = window.open('', '_blank')
  if (!win) { showToast('Trình duyệt đã chặn cửa sổ. Hãy cho phép pop-up để in hoá đơn.', 'error'); return }
  win.document.open(); win.document.write(buildConcessionInvoiceHtml()); win.document.close()
}
const newConcessionSale = () => {
  selectedCombos.value = []
  member.value = null
  cardNumberInput.value = ''
  cardError.value = ''
  concessionSale.value = null
  fnbStep.value = 1
  resetVoidState()
}

const fetchData = async () => {
  isLoading.value = true
  error.value = ''
  try {
    await shiftStore.fetchCurrent(true)
    if (isLocked.value) {
      showtimes.value = []
      combos.value = []
      error.value = lockedMessage.value
      return
    }
    if (!canUseTicketing.value && canUseFnb.value) saleMode.value = 'FNB'
    if (canUseTicketing.value && !canUseFnb.value) saleMode.value = 'TICKET'
    const [stRes, cbRes] = await Promise.all([
      canUseTicketing.value ? ticketingApi.getShowtimes() : Promise.resolve({ data: [] }),
      ticketingApi.getCombos()
    ])
    showtimes.value = canUseTicketing.value ? (stRes.data.data ?? stRes.data) : []
    combos.value = cbRes.data.data ?? cbRes.data
  } catch (err) {
    error.value = 'Không tải được dữ liệu bán vé. Kiểm tra đăng nhập/quyền.'
  } finally {
    isLoading.value = false
  }
}

const selectShowtime = async (st) => {
  if (isPastShowtime(st)) {
    showToast('Suất chiếu đã quá giờ phát sóng — không thể bán vé.', 'error')
    return
  }
  selectedShowtime.value = st
  selectedSeats.value = []
  stopHoldTimer()
  isLoadingSeats.value = true
  currentStep.value = 2
  try {
    const { data } = await ticketingApi.getSeats(st.id)
    seatData.value = data.seats ? data : { matrixRow: 9, matrixCol: 10, seats: Array.isArray(data) ? data : [] }
    captureSeatMeta(data)
    startSeatPolling()
  } catch (err) {
    showToast('Không tải được sơ đồ ghế.', 'error')
    seatData.value = { matrixRow: 9, matrixCol: 10, seats: [] }
  } finally {
    isLoadingSeats.value = false
  }
}

const seatAt = (row, col) => seatData.value.seats.find(s => s.gridRow === row && s.gridCol === col)
const isSelected = (seat) => selectedSeats.value.some(s => s.seatId === seat.seatId)
// Nhãn hàng (A, B, C...) suy từ ghế đầu tiên có trên hàng đó
const rowLabel = (gridRow) => {
  const s = seatData.value.seats.find(x => x.gridRow === gridRow)
  return s ? s.rowChar : ''
}

const toggleSeat = (seat) => {
  if (!seat || seat.status !== 'AVAILABLE') return
  const idx = selectedSeats.value.findIndex(s => s.seatId === seat.seatId)
  if (idx > -1) {
    selectedSeats.value.splice(idx, 1)
    seatRealtime.deselect(seat.seatId) // nhả khóa real-time cho quầy khác chọn được
  } else {
    // Ghế đang bị quầy khác giữ (real-time) → chặn ngay, không cho chọn
    if (isSeatLockedByOthers(seat)) {
      showToast(`Ghế ${seat.rowChar + seat.colNum} vừa được chọn hoặc đã được bán ở quầy khác. Vui lòng chọn vị trí ghế khác!`, 'error')
      return
    }
    // Chặn sớm khi vượt giới hạn số vé/lần đặt (chống phe vé) — khớp với ràng buộc backend
    if (selectedSeats.value.length >= maxTicketsPerBooking.value) {
      showToast(`Mỗi lần đặt tối đa ${maxTicketsPerBooking.value} vé.`, 'error')
      return
    }
    seat.ticketType = seat.ticketType || 'ADULT'
    selectedSeats.value.push(seat)
    seatRealtime.select(seat.seatId) // giữ ghế trên server (ai click trước thắng)
  }
  // Bắt đầu/đặt lại bộ đếm giữ ghế khi có ghế; dừng khi bỏ hết ghế
  if (selectedSeats.value.length > 0) { if (!holdTimer) startHoldTimer() }
  else stopHoldTimer()
}

const seatClass = (seat) => {
  const base = 'w-8 h-8 rounded-lg flex items-center justify-center text-[9px] font-bold border transition-all leading-none'
  if (!seat) return ''
  if (seat.status === 'SOLD') return `${base} bg-surface-container-high border-white/5 text-on-surface-variant/20 cursor-not-allowed opacity-40`
  if (seat.status === 'HOLD') return `${base} bg-yellow-500/10 border-yellow-500/30 text-yellow-500/60 cursor-not-allowed`
  // Ghế đang bị quầy khác / khách online giữ real-time → khóa xám, không cho click
  if (isSeatLockedByOthers(seat)) return `${base} bg-yellow-500/10 border-yellow-500/30 text-yellow-500/60 cursor-not-allowed`
  if (isSelected(seat)) return `${base} bg-primary border-primary text-on-primary shadow-lg shadow-primary/30 cursor-pointer scale-105`
  const byType = {
    VIP: 'bg-red-900/40 border-red-500/40 text-red-200 hover:border-red-400',
    SWEETBOX: 'bg-purple-900/40 border-purple-500/40 text-purple-200 hover:border-purple-400'
  }[seat.seatType] || 'bg-surface-container-high border-outline-variant/10 text-on-surface-variant/60 hover:border-primary/40'
  return `${base} ${byType} cursor-pointer`
}

// F&B — giới hạn 1–99 phần/món tránh gõ nhầm làm sai hoá đơn
const MAX_FNB_QTY = 99
const addCombo = (cb) => {
  const existing = selectedCombos.value.find(c => c.id === cb.id)
  if (existing) {
    if (existing.quantity >= MAX_FNB_QTY) { showToast(`Tối đa ${MAX_FNB_QTY} phần/món.`, 'error'); return }
    existing.quantity++
  } else {
    selectedCombos.value.push({ id: cb.id, name: cb.name, price: Number(cb.price), quantity: 1 })
  }
}
const changeComboQty = (item, delta) => {
  const next = item.quantity + delta
  if (next > MAX_FNB_QTY) { showToast(`Tối đa ${MAX_FNB_QTY} phần/món.`, 'error'); return }
  item.quantity = next
  if (item.quantity <= 0) {
    const idx = selectedCombos.value.findIndex(c => c.id === item.id)
    if (idx > -1) selectedCombos.value.splice(idx, 1)
  }
}

const seatTotal = computed(() => selectedSeats.value.reduce((a, s) => a + priceOf(s), 0))
const comboTotal = computed(() => selectedCombos.value.reduce((a, c) => a + c.price * c.quantity, 0))
const totalPrice = computed(() => seatTotal.value + comboTotal.value)

// Giảm giá voucher (xem trước phía client; số chính thức do BE tính lại khi thanh toán)
const discountAmount = computed(() => {
  if (!appliedVoucher.value) return 0
  const v = appliedVoucher.value
  const base = totalPrice.value
  if (String(v.discountType).toUpperCase() === 'PERCENTAGE') return Math.round(base * Number(v.discountValue || 0) / 100)
  return Math.min(Number(v.discountValue || 0), base)
})
// Số tiền khách thực trả sau giảm giá — dùng cho QR/tiền thối
const payableTotal = computed(() => Math.max(0, totalPrice.value - discountAmount.value))

// Cash Rounding: khách vãng lai (KHÔNG có thẻ thành viên) trả tiền mặt -> làm tròn gần nhất 1.000đ
const CASH_ROUND_UNIT = 1000
const roundCash = (v) => Math.round(Number(v || 0) / CASH_ROUND_UNIT) * CASH_ROUND_UNIT
const cashRoundingApplies = computed(() => !member.value)
const cashPayable = computed(() => cashRoundingApplies.value ? roundCash(payableTotal.value) : payableTotal.value)
const cashRoundingDelta = computed(() => cashPayable.value - payableTotal.value)
const cashRoundingLabel = computed(() => (cashRoundingDelta.value > 0 ? '+' : '−') + fmt(Math.abs(cashRoundingDelta.value)) + 'đ')

const seatTypeBreakdown = computed(() => {
  const map = {}
  for (const s of selectedSeats.value) {
    const key = s.seatType
    if (!map[key]) map[key] = { type: key, count: 0, subtotal: 0 }
    map[key].count++
    map[key].subtotal += priceOf(s)
  }
  return Object.values(map)
})

// ---- Thanh toán tiền mặt ----
const MAX_CASH = 100_000_000 // giới hạn 100 triệu tránh tràn số / gõ nhầm thừa số 0

// Ô nhập có format dấu chấm nghìn; nguồn dữ liệu thật vẫn là cashGiven (number)
const cashGivenDisplay = computed({
  get: () => (cashGiven.value ? cashGiven.value.toLocaleString('vi-VN') : ''),
  set: (val) => {
    const digits = String(val).replace(/\D/g, '') // chỉ giữ chữ số
    let n = digits ? parseInt(digits, 10) : 0
    if (n > MAX_CASH) n = MAX_CASH // chặn vượt trần
    cashGiven.value = n
  },
})

const changeDue = computed(() => Math.max(0, Number(cashGiven.value || 0) - cashPayable.value))
const canConfirmCash = computed(() => Number(cashGiven.value || 0) >= cashPayable.value && cashPayable.value > 0)
// Thông điệp validate dưới ô nhập
const cashError = computed(() => {
  const given = Number(cashGiven.value || 0)
  if (given === 0) return 'Vui lòng nhập số tiền khách đưa.'
  if (given < cashPayable.value) return `Tiền khách đưa chưa đủ (còn thiếu ${fmt(cashPayable.value - given)}đ).`
  return ''
})
const cashSuggestions = computed(() => {
  const t = cashPayable.value
  if (t <= 0) return []
  const set = new Set([t])
  for (const note of [50000, 100000, 200000, 500000]) set.add(Math.ceil(t / note) * note)
  return Array.from(set).filter(v => v >= t).sort((a, b) => a - b).slice(0, 5)
})

// ---- Thanh toán QR (VietQR) ----
const removeDiacritics = (s) => String(s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D')
const transferContent = computed(() => {
  const seats = selectedSeats.value.map(s => s.rowChar + s.colNum).join('')
  return removeDiacritics(`DevCine ve ${seats}`).slice(0, 50)
})

// Tự dựng payload VietQR/napas247 (EMVCo) để render QR THUẦN, không logo ở giữa
const crc16 = (str) => {
  let crc = 0xFFFF
  for (let i = 0; i < str.length; i++) {
    crc ^= str.charCodeAt(i) << 8
    for (let j = 0; j < 8; j++) {
      crc = (crc & 0x8000) ? ((crc << 1) ^ 0x1021) : (crc << 1)
      crc &= 0xFFFF
    }
  }
  return crc.toString(16).toUpperCase().padStart(4, '0')
}
const buildVietQrPayload = () => {
  const b = bankInfo.value
  if (!b.code || !b.accountNo) return ''
  const tlv = (id, val) => id + String(val.length).padStart(2, '0') + val
  const acquirer = tlv('00', b.code) + tlv('01', b.accountNo)
  const f38 = tlv('38', tlv('00', 'A000000727') + tlv('01', acquirer) + tlv('02', 'QRIBFTTA'))
  const amount = Math.round(payableTotal.value || 0)
  const f54 = amount > 0 ? tlv('54', String(amount)) : ''
  const f62 = transferContent.value ? tlv('62', tlv('08', transferContent.value)) : ''
  const partial = tlv('00', '01') + tlv('01', '11') + f38 + tlv('53', '704') + f54 + tlv('58', 'VN') + f62 + '6304'
  return partial + crc16(partial)
}
const cleanQrUrl = computed(() => {
  const payload = buildVietQrPayload()
  if (!payload) return ''
  return `https://api.qrserver.com/v1/create-qr-code/?size=420x420&margin=0&ecc=M&data=${encodeURIComponent(payload)}`
})

const loadBankInfo = async () => {
  try {
    const { data } = await settingsApi.getAll()
    const map = {}
    data.forEach(s => { map[s.settingKey] = s.settingValue })
    bankInfo.value = {
      code: map.PAYMENT_BANK_CODE || '',
      name: map.PAYMENT_BANK_NAME || '',
      accountNo: map.PAYMENT_ACCOUNT_NO || '',
      accountName: map.PAYMENT_ACCOUNT_NAME || ''
    }
    const mt = parseInt(map.MAX_TICKETS_PER_BOOKING)
    if (!isNaN(mt)) maxTicketsPerBooking.value = Math.min(20, Math.max(1, mt))
  } catch (err) {
    // Không chặn POS nếu lỗi — modal QR sẽ báo "chưa cấu hình"
  }
}

const openCashModal = () => {
  if (!checkoutReady()) return
  cashGiven.value = 0
  showCashModal.value = true
}
const openQrModal = () => {
  if (!checkoutReady()) return
  showQrModal.value = true
}

const checkMemberCard = async () => {
  cardError.value = ''
  if (!cardNumberInput.value.trim()) return
  isCheckingCard.value = true
  try {
    const { data } = await ticketingApi.memberCard(cardNumberInput.value.trim())
    member.value = data.data ?? data
    loadOwnedVouchers() // bật danh sách voucher của khách sau khi tra cứu thành công
  } catch (err) {
    cardError.value = err.response?.data?.error || 'Không tìm thấy thẻ thành viên.'
    member.value = null
  } finally {
    isCheckingCard.value = false
  }
}
const clearMember = () => { member.value = null; cardNumberInput.value = ''; cardError.value = ''; clearVoucherState() }

// ===== Voucher / khuyến mãi tại quầy =====
const clearVoucherState = () => {
  appliedVoucher.value = null
  voucherCodeInput.value = ''
  ownedVouchers.value = []
  voucherError.value = ''
}
const loadOwnedVouchers = async () => {
  if (!member.value) { ownedVouchers.value = []; return }
  try {
    const { data } = await ticketingApi.customerVouchers(member.value.customerId)
    const list = Array.isArray(data) ? data : (data?.data ?? [])
    ownedVouchers.value = list.filter(v => v.status === 'ACTIVE')
  } catch (_) {
    ownedVouchers.value = []
  }
}
const applyVoucher = async () => {
  if (!member.value) { showToast('Vui lòng tra cứu thành viên trước khi áp voucher.', 'error'); return }
  const code = (voucherCodeInput.value || '').trim()
  if (!code) { voucherError.value = 'Vui lòng nhập hoặc chọn mã voucher.'; return }
  isApplyingVoucher.value = true
  voucherError.value = ''
  try {
    const { data } = await ticketingApi.applyVoucher(member.value.customerId, code)
    appliedVoucher.value = { id: data.id, code: data.code, discountType: data.discountType, discountValue: Number(data.discountValue || 0) }
    voucherCodeInput.value = data.code
    showToast(`Đã áp mã ${data.code}.`, 'success')
    loadOwnedVouchers()
  } catch (e) {
    appliedVoucher.value = null
    voucherError.value = e.response?.data?.message || 'Mã không hợp lệ hoặc không áp dụng được.'
  } finally {
    isApplyingVoucher.value = false
  }
}
const clearVoucher = () => { appliedVoucher.value = null; voucherCodeInput.value = ''; voucherError.value = '' }

const processPayment = async (method) => {
  if (saleMode.value === 'FNB') return processConcessionPayment(method)
  if (!canUseTicketing.value) { showToast(shiftStore.lockedMessage('bán vé POS'), 'error'); return }
  if (selectedSeats.value.length === 0) {
    showToast('Chưa chọn ghế.', 'error')
    return
  }
  paymentMethod.value = method
  isPaying.value = true
  try {
    const payload = {
      showtimeId: selectedShowtime.value.id,
      seatIds: selectedSeats.value.map(s => s.seatId),
      seatSelections: selectedSeats.value.map(s => ({ seatId: s.seatId, ticketType: s.ticketType || 'ADULT' })),
      fnbs: selectedCombos.value.map(c => ({ fnbItemId: c.id, quantity: c.quantity })),
      customerId: member.value ? member.value.customerId : null,
      voucherId: appliedVoucher.value ? appliedVoucher.value.id : null,
      paymentMethod: method
    }
    const { data } = await ticketingApi.pay(payload)
    if (data.success) {
      completedBooking.value = data
      stopHoldTimer()
      stopSeatPolling()
      showCashModal.value = false
      showQrModal.value = false
      currentStep.value = 6
    } else {
      showToast(data.message || 'Thanh toán thất bại.', 'error')
    }
  } catch (err) {
    showToast(err.response?.data?.message || 'Thanh toán thất bại (ghế có thể đã bán).', 'error')
  } finally {
    isPaying.value = false
  }
}

// ===== In hoá đơn (mở tab mới: hoá đơn + vé QR soát cổng) =====
const esc = (v) => String(v ?? '').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]))
const qrUrl = (code) => `https://api.qrserver.com/v1/create-qr-code/?size=200x200&margin=0&data=${encodeURIComponent(code)}`
const paymentLabel = (m) => ({ CASH: 'Tiền mặt', CARD: 'Thẻ / QR', TRANSFER: 'Chuyển khoản QR' }[m] || m)

const buildInvoiceHtml = () => {
  const st = selectedShowtime.value
  const movie = esc(st?.movieTitle)
  const room = esc(st?.roomName)
  const format = esc(st?.formatName)
  const bookingCode = esc(completedBooking.value?.bookingCode)
  const tickets = completedBooking.value?.tickets || []
  const dateStr = st?.startTime
    ? new Date(st.startTime).toLocaleString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
    : ''
  const printedAt = new Date().toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })

  const itemRow = (name, qty, unit, total) =>
    `<tr><td>${name}</td><td class="c">${qty}</td><td class="r">${fmt(unit)}đ</td><td class="r b">${fmt(total)}đ</td></tr>`

  const seatRows = seatTypeBreakdown.value.map(b => {
    const labels = selectedSeats.value.filter(s => s.seatType === b.type).map(s => s.rowChar + s.colNum).join(', ')
    const unit = b.count ? b.subtotal / b.count : 0
    return itemRow(`Ghế ${esc(seatTypeLabel(b.type))} <span class="muted">${esc(labels)}</span>`, b.count, unit, b.subtotal)
  }).join('')

  const comboRows = selectedCombos.value
    .map(c => itemRow(esc(c.name), c.quantity, c.price, c.price * c.quantity)).join('')

  // Mỗi ghế = 1 tờ vé riêng (không gộp), tách trang khi in
  const ticketSlips = tickets.length
    ? tickets.map((t, i) => `
        <article class="ticket">
          <div class="tk-main">
            <div class="tk-head">
              <div class="mono serif">D</div>
              <div>
                <div class="tk-brand">DEV<span class="g">CINE</span></div>
                <div class="tk-sub">Vé xem phim · Admit One</div>
              </div>
              <div class="tk-no serif">${String(i + 1).padStart(2, '0')}<span>/${String(tickets.length).padStart(2, '0')}</span></div>
            </div>
            <div class="tk-seat-row">
              <div>
                <div class="tk-k">Ghế ngồi</div>
                <div class="tk-seat serif">${esc(t.seatLabel)}</div>
              </div>
              <div class="tk-movie">${movie}</div>
            </div>
            <dl class="tk-meta">
              <div><dt>Phòng chiếu</dt><dd>${room} · ${format}</dd></div>
              <div><dt>Suất chiếu</dt><dd>${esc(dateStr)}</dd></div>
              <div><dt>Mã đơn</dt><dd>${bookingCode}</dd></div>
              <div><dt>Loại vé</dt><dd>Người lớn</dd></div>
            </dl>
          </div>
          <div class="tk-stub">
            <div class="tk-stub-t">Mã đặt vé — quét tại quầy</div>
            <img src="${qrUrl(bookingCode)}" alt="QR ${bookingCode}" />
            <div class="tk-code">${bookingCode}</div>
            <div class="tk-note">1 mã QR dùng chung cho cả đơn</div>
          </div>
        </article>`).join('')
    : '<div class="ticket"><p class="muted" style="padding:28px">Không có dữ liệu vé QR.</p></div>'

  const seatCount = selectedSeats.value.length
  const seatSection = seatRows ? `<tr class="grp"><td colspan="4">Vé xem phim</td></tr>${seatRows}` : ''
  const comboSection = comboRows ? `<tr class="grp"><td colspan="4">Bắp nước &amp; Combo</td></tr>${comboRows}` : ''

  // Số tiền giảm khi áp mã/voucher (0 nếu không có) + làm tròn tiền mặt (âm nếu tròn xuống). Ưu tiên số BE trả.
  const discount = Number(completedBooking.value?.discountAmount || 0)
  const rounding = Number(completedBooking.value?.roundingAmount || 0)
  const grandTotal = completedBooking.value?.finalAmount != null
    ? Number(completedBooking.value.finalAmount)
    : Math.max(0, seatTotal.value + comboTotal.value - discount + rounding)

  return `<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8" />
<title>Hoá đơn ${bookingCode} — DevCine</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,600;0,700;0,800;0,900;1,500&family=Inter:wght@400;500;600;700;800&display=swap');
  *{box-sizing:border-box;margin:0;padding:0}
  body{font-family:'Inter',system-ui,Arial,sans-serif;background:#efe8da;color:#26221b;padding:34px 20px;font-size:14px;-webkit-print-color-adjust:exact;print-color-adjust:exact}
  .serif{font-family:'Playfair Display',Georgia,serif}
  .mono-num{font-variant-numeric:tabular-nums}
  .g{background:linear-gradient(135deg,#e6c878,#c4992f);-webkit-background-clip:text;background-clip:text;color:transparent}

  .bar{display:flex;justify-content:center;gap:12px;max-width:880px;margin:0 auto 26px}
  .bar button{border:0;border-radius:999px;padding:13px 30px;font-weight:700;font-size:11px;letter-spacing:.14em;text-transform:uppercase;cursor:pointer}
  .btn-print{background:linear-gradient(135deg,#dcb869,#b8902f);color:#1c1a17;box-shadow:0 8px 22px rgba(184,144,47,.4)}
  .btn-close{background:#fff;color:#6b6456;border:1px solid #ddd4c1}

  /* ===== BILL ===== */
  .bill{max-width:880px;margin:0 auto;background:#fffdf8;border-radius:20px;overflow:hidden;box-shadow:0 30px 70px rgba(40,34,22,.18);border:1px solid #ece3d0}
  .bill-head{display:flex;justify-content:space-between;align-items:center;padding:36px 44px 30px;background:linear-gradient(160deg,#211d16,#14110c);color:#f3ecdc;position:relative}
  .bill-head::after{content:'';position:absolute;left:0;right:0;bottom:0;height:3px;background:linear-gradient(90deg,#b8902f,#e6c878,#b8902f)}
  .brand{display:flex;align-items:center;gap:16px}
  .mono{width:54px;height:54px;border-radius:15px;background:linear-gradient(135deg,#e9cd80,#b8902f);display:flex;align-items:center;justify-content:center;font-size:32px;font-weight:900;color:#1c1a17;box-shadow:0 8px 18px rgba(0,0,0,.35);flex:none}
  .brand-name{font-size:27px;font-weight:800;letter-spacing:.2em;line-height:1}
  .brand-tag{font-size:9px;letter-spacing:.32em;text-transform:uppercase;color:#a89c81;margin-top:7px}
  .doc{text-align:right}
  .doc-t{font-size:19px;letter-spacing:.34em;text-transform:uppercase;color:#e6c878}
  .doc-meta{margin-top:13px;font-size:11px;line-height:1.95;color:#a89c81}
  .doc-meta b{color:#f3ecdc;font-weight:600}

  .feature{display:flex;align-items:center;gap:16px;padding:24px 44px;border-bottom:1px solid #efe6d3}
  .feature .ico{width:46px;height:46px;border-radius:13px;background:#f7efe0;display:flex;align-items:center;justify-content:center;font-size:22px;flex:none}
  .feature h2{font-size:23px;font-weight:700;line-height:1.15;color:#211d16}
  .feature p{color:#8c836d;font-size:12.5px;margin-top:5px;letter-spacing:.01em}

  table{width:100%;border-collapse:collapse}
  thead th{font-size:9.5px;letter-spacing:.18em;text-transform:uppercase;color:#a89c81;text-align:left;padding:20px 44px 8px;font-weight:700}
  thead th.c{text-align:center}thead th.r{text-align:right}
  tbody td{padding:13px 44px;font-size:13.5px;border-top:1px solid #f3ecdd;color:#3a342a}
  td.c{text-align:center}td.r{text-align:right;font-variant-numeric:tabular-nums}td.b{font-weight:700;color:#211d16}
  tr.grp td{padding:18px 44px 6px;font-size:10px;letter-spacing:.2em;text-transform:uppercase;color:#b8902f;font-weight:800;border-top:0}
  tr.grp+tr td{border-top:0}
  .muted{color:#b3a994;font-weight:500;font-size:12px}

  .summary{padding:14px 44px 6px;display:flex;flex-direction:column;align-items:flex-end}
  .s-row{display:flex;justify-content:space-between;width:min(340px,100%);padding:10px 0;font-size:13px;color:#6f6755;border-top:1px solid #f3ecdd}
  .s-row b{color:#26221b;font-weight:600;font-variant-numeric:tabular-nums}
  .s-row b.cut{color:#3b6d11}
  .s-grand{display:flex;justify-content:space-between;align-items:baseline;width:min(340px,100%);margin-top:12px;padding-top:18px;border-top:2px solid #211d16}
  .s-grand span{font-size:11px;letter-spacing:.18em;text-transform:uppercase;color:#8c836d;font-weight:700}
  .s-grand b{font-size:33px;font-weight:800;color:#211d16;font-variant-numeric:tabular-nums;line-height:1}
  .s-grand b .u{font-size:17px;color:#b8902f;margin-left:3px}

  .foot{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-top:20px;padding:22px 44px;background:#faf4e9;border-top:1px solid #efe6d3}
  .foot .pm{font-size:12px;color:#6f6755;line-height:1.7}
  .foot .pm b{color:#26221b;font-weight:600}
  .stamp{font-size:10px;font-weight:800;letter-spacing:.16em;text-transform:uppercase;color:#b8902f;border:1.5px solid #dcb869;border-radius:9px;padding:9px 15px;transform:rotate(-4deg);white-space:nowrap;flex:none}
  .thanks{text-align:center;font-style:italic;font-size:15px;color:#9a8f76;padding:20px}

  .divider{max-width:880px;margin:34px auto 20px;display:flex;align-items:center;gap:18px;color:#9a8f76}
  .divider::before,.divider::after{content:'';flex:1;height:1px}
  .divider::before{background:linear-gradient(90deg,transparent,#c9bda1)}
  .divider::after{background:linear-gradient(90deg,#c9bda1,transparent)}
  .divider span{font-size:10px;letter-spacing:.28em;text-transform:uppercase;font-weight:700;white-space:nowrap}

  /* ===== TICKET ===== */
  .ticket{max-width:880px;margin:0 auto 22px;display:flex;background:#fffdf8;border-radius:18px;box-shadow:0 22px 54px rgba(40,34,22,.15);overflow:hidden;border:1px solid #ece3d0;position:relative}
  .ticket::before{content:'';position:absolute;left:0;top:0;bottom:0;width:7px;background:linear-gradient(180deg,#e9cd80,#b8902f)}
  .tk-main{flex:1;padding:28px 34px;min-width:0}
  .tk-head{display:flex;align-items:center;gap:13px}
  .tk-head .mono{width:40px;height:40px;border-radius:11px;font-size:22px}
  .tk-brand{font-size:16px;font-weight:800;letter-spacing:.16em;line-height:1}
  .tk-sub{font-size:9px;letter-spacing:.2em;text-transform:uppercase;color:#a89c81;margin-top:4px}
  .tk-no{margin-left:auto;font-size:28px;font-weight:800;color:#211d16}
  .tk-no span{font-size:15px;color:#b3a994}
  .tk-seat-row{display:flex;align-items:flex-end;justify-content:space-between;gap:18px;margin:22px 0 20px;padding-bottom:20px;border-bottom:1px dashed #ddd2bb}
  .tk-k{font-size:9px;letter-spacing:.2em;text-transform:uppercase;color:#a89c81;font-weight:700}
  .tk-seat{font-size:50px;font-weight:900;line-height:.95;color:#211d16;margin-top:4px}
  .tk-movie{font-size:15px;font-weight:700;text-transform:uppercase;text-align:right;color:#3a342a;letter-spacing:.01em;max-width:55%}
  .tk-meta{display:grid;grid-template-columns:1fr 1fr;gap:14px 24px}
  .tk-meta dt{font-size:9px;letter-spacing:.16em;text-transform:uppercase;color:#a89c81;font-weight:700}
  .tk-meta dd{font-size:13px;font-weight:600;color:#26221b;margin-top:4px}

  .tk-stub{width:218px;flex:none;padding:24px 22px;text-align:center;background:#faf4e9;display:flex;flex-direction:column;align-items:center;justify-content:center;border-left:2px dashed #d7ccb3;position:relative}
  .tk-stub::before,.tk-stub::after{content:'';position:absolute;left:-12px;width:22px;height:22px;border-radius:50%;background:#efe8da}
  .tk-stub::before{top:-11px}
  .tk-stub::after{bottom:-11px}
  .tk-stub-t{font-size:9px;letter-spacing:.18em;text-transform:uppercase;color:#a89c81;font-weight:700;margin-bottom:12px}
  .tk-stub img{width:152px;height:152px;display:block}
  .tk-code{font-family:'Courier New',monospace;font-size:9.5px;color:#8c836d;margin-top:11px;word-break:break-all;letter-spacing:.02em}
  .tk-note{font-size:9px;letter-spacing:.1em;text-transform:uppercase;color:#b8902f;font-weight:700;margin-top:9px}

  @media print{
    body{background:#fff;padding:0}
    .bar,.divider{display:none}
    .bill,.ticket{box-shadow:none;border-radius:0;max-width:100%;border:0;margin:0}
    .ticket{break-before:page}
    .ticket::before{display:none}
    .tk-stub::before,.tk-stub::after{background:#fff}
  }
</style></head>
<body>
  <div class="bar">
    <button class="btn-print" onclick="window.print()">🖨 In hoá đơn</button>
    <button class="btn-close" onclick="window.close()">Đóng</button>
  </div>

  <section class="bill">
    <div class="bill-head">
      <div class="brand">
        <div class="mono serif">D</div>
        <div>
          <div class="brand-name">DEV<span class="g">CINE</span></div>
          <div class="brand-tag">Cinema · Hệ thống rạp chiếu phim</div>
        </div>
      </div>
      <div class="doc">
        <div class="doc-t serif">Hoá Đơn</div>
        <div class="doc-meta">
          Số: <b>${bookingCode}</b><br/>
          Ngày in: <b>${esc(printedAt)}</b><br/>
          Quầy: <b>POS · Lễ tân</b>
        </div>
      </div>
    </div>

    <div class="feature">
      <div class="ico">🎬</div>
      <div>
        <h2 class="serif">${movie}</h2>
        <p>${format} · ${room} · Suất ${esc(dateStr)}</p>
      </div>
    </div>

    <table>
      <thead><tr><th>Nội dung</th><th class="c">SL</th><th class="r">Đơn giá</th><th class="r">Thành tiền</th></tr></thead>
      <tbody>${seatSection}${comboSection}</tbody>
    </table>

    <div class="summary">
      <div class="s-row"><span>Tạm tính vé · ${seatCount} ghế</span><b>${fmt(seatTotal.value)}đ</b></div>
      ${comboTotal.value > 0 ? `<div class="s-row"><span>Bắp nước &amp; combo</span><b>${fmt(comboTotal.value)}đ</b></div>` : ''}
      <div class="s-row"><span>Số tiền được giảm</span><b class="${discount > 0 ? 'cut' : ''}">${discount > 0 ? '−' + fmt(discount) : '0'}đ</b></div>
      ${rounding !== 0 ? `<div class="s-row"><span>Làm tròn tiền mặt</span><b>${rounding > 0 ? '+' : '−'}${fmt(Math.abs(rounding))}đ</b></div>` : ''}
      <div class="s-grand"><span>Tổng thanh toán</span><b class="serif">${fmt(grandTotal)}<span class="u">đ</span></b></div>
    </div>

    <div class="foot">
      <div class="pm">Phương thức: <b>${esc(paymentLabel(paymentMethod.value))}</b>${member.value ? `<br/>Thành viên: <b>${esc(member.value.fullName)}</b> · ${esc(member.value.membershipTier)}` : ''}</div>
      <div class="stamp">Đã thanh toán</div>
    </div>
    <div class="thanks serif">Cảm ơn quý khách & hẹn gặp lại tại DevCine</div>
  </section>

  <div class="divider"><span>Vé xem phim · ${tickets.length} vé</span></div>

  ${ticketSlips}
</body></html>`
}

const printInvoice = () => {
  if (!completedBooking.value) return
  const win = window.open('', '_blank')
  if (!win) {
    showToast('Trình duyệt đã chặn cửa sổ. Hãy cho phép pop-up để in hoá đơn.', 'error')
    return
  }
  win.document.open()
  win.document.write(buildInvoiceHtml())
  win.document.close()
}

// ===== Hiển thị mã QR toàn màn hình (tab mới) cho khách quét — chỉ mã, không thông tin =====
const buildQrPageHtml = () => {
  return `<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>Quét mã chuyển khoản — DevCine</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700;800;900&family=Inter:wght@400;500;600;700;800&display=swap');
  *{box-sizing:border-box;margin:0;padding:0}
  body{font-family:'Inter',system-ui,Arial,sans-serif;background:#efe8da;color:#26221b;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px}
  .card{width:100%;max-width:560px;background:#fffdf8;border-radius:24px;overflow:hidden;box-shadow:0 30px 70px rgba(40,34,22,.2);border:1px solid #ece3d0}
  .head{background:linear-gradient(160deg,#211d16,#14110c);color:#f3ecdc;padding:24px 30px;display:flex;align-items:center;gap:14px;position:relative}
  .head::after{content:'';position:absolute;left:0;right:0;bottom:0;height:3px;background:linear-gradient(90deg,#b8902f,#e6c878,#b8902f)}
  .mono{width:46px;height:46px;border-radius:13px;background:linear-gradient(135deg,#e9cd80,#b8902f);display:flex;align-items:center;justify-content:center;font-size:27px;font-weight:900;color:#1c1a17;font-family:'Playfair Display',serif;flex:none}
  .brand{font-size:22px;font-weight:800;letter-spacing:.18em;line-height:1}
  .brand .g{color:#e6c878}
  .brand small{display:block;font-size:9px;letter-spacing:.28em;color:#a89c81;margin-top:6px;font-weight:600;text-transform:uppercase}
  .body{padding:28px 30px 32px;text-align:center}
  .qr{width:min(80vmin,440px);aspect-ratio:1;margin:0 auto;background:#fff;border-radius:18px;padding:12px;border:1px solid #eee}
  .qr img{width:100%;height:100%;object-fit:contain}
  .hint{font-size:13px;color:#8c836d;margin:16px 0 0}
  .amount{margin-top:22px;background:#211d16;border-radius:14px;padding:16px 22px;display:flex;justify-content:space-between;align-items:center}
  .amount span{font-size:11px;letter-spacing:.16em;text-transform:uppercase;color:#a89c81;font-weight:700}
  .amount b{font-family:'Playfair Display',serif;font-size:32px;font-weight:800;color:#e6c878}
</style></head>
<body>
  <div class="card">
    <div class="head">
      <div class="mono">D</div>
      <div class="brand">DEV<span class="g">CINE</span><small>Quét mã để chuyển khoản</small></div>
    </div>
    <div class="body">
      <div class="qr"><img src="${cleanQrUrl.value}" alt="VietQR" /></div>
      <p class="hint">Mở app ngân hàng → quét mã. Số tiền &amp; nội dung tự điền.</p>
      <div class="amount"><span>Số tiền</span><b>${fmt(payableTotal.value)}đ</b></div>
    </div>
  </div>
</body></html>`
}

const openQrFullscreen = () => {
  if (!cleanQrUrl.value) return
  const win = window.open('', '_blank')
  if (!win) {
    showToast('Trình duyệt đã chặn cửa sổ. Hãy cho phép pop-up để hiển thị mã.', 'error')
    return
  }
  win.document.open()
  win.document.write(buildQrPageHtml())
  win.document.close()
}

const resetPOS = () => {
  stopHoldTimer()
  stopSeatPolling()
  currentStep.value = 1
  selectedShowtime.value = null
  seatData.value = { matrixRow: 9, matrixCol: 10, seats: [] }
  selectedSeats.value = []
  selectedCombos.value = []
  member.value = null
  cardNumberInput.value = ''
  cardError.value = ''
  completedBooking.value = null
  concessionSale.value = null
  fnbStep.value = 1
  showCashModal.value = false
  showQrModal.value = false
  cashGiven.value = 0
  clearVoucherState()
  fetchData()
}

onMounted(() => {
  fetchData(); loadBankInfo(); loadHeldOrders()
  // Nhịp 1s: cập nhật countdown/tuổi đơn chờ + tự dọn đơn hết giờ
  nowTimer = setInterval(() => { nowTs.value = Date.now(); sweepExpiredHolds() }, 1000)
})
onUnmounted(() => {
  if (toastTimer) clearTimeout(toastTimer)
  stopHoldTimer()
  stopSeatPolling()
  if (nowTimer) clearInterval(nowTimer)
})
</script>

<template>
  <div class="h-full flex flex-col p-6 space-y-5 bg-surface-container-lowest">
    <!-- Header -->
    <header class="flex justify-between items-center bg-surface px-5 py-3 rounded-2xl border border-outline-variant/10 shadow-xl">
      <div class="flex items-center gap-4">
        <div class="w-9 h-9 bg-primary rounded-xl flex items-center justify-center text-on-primary shadow-lg shadow-primary/20">
          <span class="material-symbols-outlined text-xl">point_of_sale</span>
        </div>
        <div>
          <h1 class="text-lg font-black tracking-tighter uppercase italic text-on-surface leading-none">Ticketing <span class="text-primary">POS</span></h1>
          <p class="text-[9px] font-bold text-on-surface-variant uppercase tracking-widest mt-0.5">Hệ thống bán vé & F&B v2.1</p>
        </div>
      </div>

      <div class="flex items-center gap-4">
        <!-- Chọn luồng bán -->
        <div class="flex items-center gap-1 p-1 bg-surface-container-high rounded-xl border border-outline-variant/10">
          <button @click="switchMode('TICKET')"
                  :disabled="!canUseTicketing"
                  :class="saleMode === 'TICKET' ? 'bg-primary text-on-primary shadow' : 'text-on-surface-variant hover:text-on-surface'"
                  class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[11px] font-black uppercase tracking-wider transition-all">
            <span class="material-symbols-outlined text-base">confirmation_number</span> Vé + F&B
          </button>
          <button @click="switchMode('FNB')"
                  :disabled="!canUseFnb"
                  :class="saleMode === 'FNB' ? 'bg-primary text-on-primary shadow' : 'text-on-surface-variant hover:text-on-surface'"
                  class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[11px] font-black uppercase tracking-wider transition-all">
            <span class="material-symbols-outlined text-base">lunch_dining</span> Bán nhanh F&B
          </button>
        </div>

        <!-- Stepper bán vé -->
        <div v-if="saleMode === 'TICKET'" class="flex items-center gap-1.5">
          <div v-for="i in 6" :key="i" class="flex items-center gap-1.5">
            <div :class="currentStep >= i ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant/40'"
                 class="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black transition-all">{{ i }}</div>
            <div v-if="i < 6" class="w-6 h-0.5 bg-outline-variant/20"></div>
          </div>
        </div>
        <!-- Stepper bán nhanh F&B (2 bước) -->
        <div v-else class="flex items-center gap-2 text-[11px] font-black uppercase tracking-wider">
          <span :class="fnbStep >= 1 ? 'text-primary' : 'text-on-surface-variant/40'" class="flex items-center gap-1.5">
            <span :class="fnbStep >= 1 ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant/40'" class="w-6 h-6 rounded-full flex items-center justify-center">1</span> Chọn món
          </span>
          <span class="w-6 h-0.5 bg-outline-variant/20"></span>
          <span :class="fnbStep >= 2 ? 'text-primary' : 'text-on-surface-variant/40'" class="flex items-center gap-1.5">
            <span :class="fnbStep >= 2 ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant/40'" class="w-6 h-6 rounded-full flex items-center justify-center">2</span> Thanh toán
          </span>
        </div>
      </div>

      <div class="flex items-center gap-2.5">
        <!-- Đồng hồ giữ ghế -->
        <div v-if="holdActive"
             :class="holdUrgent ? 'bg-red-500/15 border-red-500/40 text-red-300 animate-pulse' : 'bg-primary/10 border-primary/30 text-primary'"
             class="flex items-center gap-2 px-3.5 py-2 rounded-xl border" title="Thời gian giữ ghế còn lại">
          <span class="material-symbols-outlined text-base">timer</span>
          <span class="text-sm font-black tabular-nums tracking-wider">{{ holdMmSs }}</span>
        </div>

        <!-- Giữ đơn (Hold Order) — vô hiệu hoá khi giỏ trống -->
        <button @click="holdCurrentOrder" :disabled="!canHoldOrder || isHolding"
                :class="(canHoldOrder && !isHolding) ? 'bg-amber-500/10 border-amber-500/30 text-amber-300 hover:bg-amber-500/20' : 'bg-surface-container-high border-outline-variant/10 text-on-surface-variant/40 cursor-not-allowed'"
                class="flex items-center gap-1.5 px-3.5 py-2 rounded-xl border text-[11px] font-black uppercase tracking-wider transition-all">
          <span class="material-symbols-outlined text-base">{{ isHolding ? 'progress_activity' : 'pause_circle' }}</span> {{ isHolding ? 'Đang giữ...' : 'Giữ đơn' }}
        </button>

        <!-- Danh sách đơn chờ -->
        <button @click="showHeldPanel = true"
                class="relative flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-surface-container-high border border-outline-variant/15 text-on-surface text-[11px] font-black uppercase tracking-wider hover:border-primary/40 transition-all">
          <span class="material-symbols-outlined text-base">receipt_long</span> Đơn chờ
          <span v-if="heldOrders.length" class="min-w-[18px] h-[18px] px-1 rounded-full bg-primary text-on-primary text-[10px] font-black flex items-center justify-center">{{ heldOrders.length }}</span>
        </button>

        <AppButton variant="outline" @click="resetPOS">Hủy giao dịch</AppButton>
      </div>
    </header>

    <main class="flex-grow grid grid-cols-12 gap-5 overflow-hidden">
      <div class="col-span-9 bg-surface border border-outline-variant/10 rounded-3xl shadow-2xl overflow-hidden flex flex-col">

        <template v-if="saleMode === 'TICKET'">
        <!-- Step 1: Showtime -->
        <div v-if="currentStep === 1" class="p-6 space-y-8 overflow-y-auto custom-scrollbar">
          <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
            <span class="w-8 h-1 bg-primary rounded-full"></span> 1. Chọn phim & suất chiếu
          </h2>

          <div v-if="isLoading" class="grid grid-cols-2 gap-6">
            <div v-for="i in 4" :key="i" class="h-44 bg-surface-container-high rounded-3xl animate-pulse"></div>
          </div>
          <div v-else-if="error" class="p-6 bg-red-500/10 border border-red-500/20 rounded-2xl text-red-400 text-sm">{{ error }}</div>
          <div v-else-if="visibleShowtimes.length === 0" class="py-20 text-center border border-dashed border-outline-variant/20 rounded-3xl">
            <span class="material-symbols-outlined text-5xl text-on-surface-variant/40 mb-3">event_busy</span>
            <p class="text-on-surface-variant font-semibold">Không có suất chiếu nào hôm nay/sắp tới.</p>
            <p class="text-xs text-on-surface-variant/60 mt-1">Tạo suất chiếu ở "Lịch chiếu & Điều phối".</p>
          </div>

          <div v-else class="grid grid-cols-2 gap-6">
            <div v-for="st in visibleShowtimes" :key="st.id" @click="selectShowtime(st)"
                 class="relative p-6 bg-surface-container-high rounded-3xl border border-outline-variant/10 transition-all group hover:border-primary/50 hover:bg-primary/5 cursor-pointer">
              <div class="flex gap-6">
                <div class="w-24 h-36 bg-surface-container-highest rounded-xl overflow-hidden shadow-lg border border-outline-variant/10">
                  <img :src="st.moviePoster || '/images/Hopper.webp'" class="w-full h-full object-cover group-hover:scale-105 transition-transform" />
                </div>
                <div class="flex flex-col justify-between py-1 min-w-0">
                  <div>
                    <p class="text-[9px] font-black text-on-surface-variant/60 uppercase tracking-[0.15em] mb-1 font-mono">{{ showtimeCode(st) }}</p>
                    <h3 class="font-black text-lg uppercase tracking-tight text-on-surface group-hover:text-primary transition-colors truncate">{{ st.movieTitle }}</h3>
                    <div class="flex items-center gap-2 mt-2">
                      <span :class="formatTone(st.formatName)" class="px-2 py-0.5 rounded-md border text-[10px] font-black uppercase tracking-wider">{{ String(st.formatName).toUpperCase() }}</span>
                      <span class="text-[10px] font-bold text-on-surface-variant uppercase">{{ st.roomName }}</span>
                    </div>
                    <p class="text-[10px] font-bold text-on-surface-variant/70 mt-1.5">{{ new Date(st.startTime).toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric' }) }}</p>
                  </div>
                  <span class="px-4 py-2 bg-primary/10 text-primary text-sm font-black italic rounded-xl border border-primary/20 w-fit mt-3 tabular-nums">
                    {{ fmtTime(st.startTime) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 2: Seats -->
        <div v-if="currentStep === 2" class="p-6 flex flex-col h-full overflow-hidden">
          <div class="flex justify-between items-center mb-3">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 2. Chọn ghế ({{ selectedSeats.length }})
            </h2>
            <AppButton variant="ghost" @click="currentStep = 1">Quay lại</AppButton>
          </div>
          <div class="w-full flex flex-col items-center gap-1.5 mb-3 shrink-0">
            <div class="w-2/3 h-1.5 bg-gradient-to-r from-transparent via-primary/40 to-transparent rounded-full blur-[2px]"></div>
            <p class="text-[10px] font-black uppercase tracking-[0.3em] text-primary/60">Màn hình</p>
          </div>

          <div v-if="isLoadingSeats" class="flex-grow flex items-center justify-center">
            <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
          </div>
          <div v-else-if="seatData.seats.length === 0" class="flex-grow flex items-center justify-center text-on-surface-variant">
            Phòng chưa có sơ đồ ghế.
          </div>
          <div v-else class="flex-grow min-h-0 flex items-center justify-center overflow-auto custom-scrollbar">
            <div class="flex flex-col gap-1.5 mx-auto my-auto">
              <div v-for="row in seatData.matrixRow" :key="row" class="flex gap-1.5 items-center justify-center">
                <span class="w-5 text-[10px] font-bold text-on-surface-variant/50 text-center shrink-0">{{ rowLabel(row - 1) }}</span>
                <template v-for="col in seatData.matrixCol" :key="col">
                  <div v-if="seatAt(row - 1, col - 1)" :class="seatClass(seatAt(row - 1, col - 1))"
                       @click="toggleSeat(seatAt(row - 1, col - 1))" :title="seatAt(row - 1, col - 1).rowChar + seatAt(row - 1, col - 1).colNum">
                    {{ seatAt(row - 1, col - 1).rowChar }}{{ seatAt(row - 1, col - 1).colNum }}
                  </div>
                  <div v-else class="w-8 h-8"></div>
                </template>
                <span class="w-5 text-[10px] font-bold text-on-surface-variant/50 text-center shrink-0">{{ rowLabel(row - 1) }}</span>
              </div>
            </div>
          </div>

          <div class="mt-3 flex items-center justify-between shrink-0">
            <div class="flex gap-4 text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-surface-container-high border border-outline-variant/20"></span>Thường</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-red-900/40 border border-red-500/40"></span>VIP</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-purple-900/40 border border-purple-500/40"></span>Sweetbox</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-surface-container-high opacity-40"></span>Đã bán</span>
            </div>
            <AppButton @click="currentStep = 3" :disabled="selectedSeats.length === 0">3. Xác nhận vé</AppButton>
          </div>
        </div>

        <!-- Step 3: Confirm ticket types (by seat type) -->
        <div v-if="currentStep === 3" class="p-6 flex flex-col h-full overflow-hidden">
          <div class="flex justify-between items-center mb-8">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 3. Xác nhận vé & loại ghế
            </h2>
            <AppButton variant="ghost" @click="currentStep = 2">Quay lại</AppButton>
          </div>

          <div class="flex-grow overflow-y-auto custom-scrollbar space-y-4 pr-2">
            <p class="px-1 text-xs text-on-surface-variant">
              Chọn số lượng vé theo đối tượng — tổng phải bằng số ghế đã chọn ({{ selectedSeats.length }} ghế).
            </p>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div v-for="(label, code) in audienceLabels" :key="code"
                   class="p-5 bg-surface-container-high rounded-[24px] border border-outline-variant/10 flex items-center justify-between gap-4">
                <span class="text-sm font-black text-on-surface uppercase">{{ label }}</span>
                <div class="flex items-center gap-3 shrink-0">
                  <button @click="setTicketCount(code, -1)" :disabled="(ticketCounts[code] || 0) <= 0"
                          class="w-9 h-9 flex items-center justify-center rounded-full bg-surface-container border border-outline-variant/10 disabled:opacity-30 hover:text-primary transition-colors">
                    <span class="material-symbols-outlined text-base">remove</span>
                  </button>
                  <span class="w-7 text-center text-lg font-black tabular-nums text-on-surface">{{ ticketCounts[code] || 0 }}</span>
                  <button @click="setTicketCount(code, 1)" :disabled="totalTicketCount >= selectedSeats.length"
                          class="w-9 h-9 flex items-center justify-center rounded-full bg-surface-container border border-outline-variant/10 disabled:opacity-30 hover:text-primary transition-colors">
                    <span class="material-symbols-outlined text-base">add</span>
                  </button>
                </div>
              </div>
            </div>

            <div class="flex items-center justify-between px-2 pt-2 text-sm">
              <span class="text-on-surface-variant">Đã gán</span>
              <span class="font-black tabular-nums" :class="ticketsMatchSeats ? 'text-green-400' : 'text-primary'">
                {{ totalTicketCount }} / {{ selectedSeats.length }} vé
              </span>
            </div>
            <div class="px-2 text-xs font-bold text-on-surface-variant">
              Ghế đã chọn: <span class="text-primary">{{ selectedSeats.map(s => s.rowChar + s.colNum).join(', ') }}</span>
            </div>
          </div>

          <div class="mt-6 flex items-center justify-end gap-4">
            <span v-if="!ticketsMatchSeats" class="text-xs font-bold text-amber-400">Cần gán đủ {{ selectedSeats.length }} vé</span>
            <AppButton @click="currentStep = 4" :disabled="!ticketsMatchSeats">4. Combo / Đồ ăn</AppButton>
          </div>
        </div>

        <!-- Step 4: F&B -->
        <div v-if="currentStep === 4" class="p-6 flex flex-col h-full overflow-hidden">
          <div class="flex justify-between items-center mb-8">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 4. Combo / Đồ ăn & Nước uống
            </h2>
            <AppButton variant="ghost" @click="currentStep = 3">Quay lại</AppButton>
          </div>

          <div v-if="combos.length === 0" class="flex-grow flex items-center justify-center text-on-surface-variant text-sm">
            Chưa có combo. Thêm ở "Thực đơn F&B / Combo".
          </div>
          <div v-else class="grid grid-cols-2 gap-6 flex-grow overflow-y-auto custom-scrollbar pr-2">
            <div v-for="cb in combos" :key="cb.id" class="p-5 bg-surface-container-high rounded-3xl border border-outline-variant/10 flex gap-4 hover:border-primary/30 transition-all">
              <div class="w-16 h-16 rounded-2xl overflow-hidden bg-surface-container-highest shrink-0 flex items-center justify-center">
                <img v-if="cb.imageUrl" :src="cb.imageUrl" class="w-full h-full object-cover" />
                <span v-else class="material-symbols-outlined text-on-surface-variant/40">fastfood</span>
              </div>
              <div class="flex flex-col justify-between flex-grow min-w-0">
                <div>
                  <h3 class="text-sm font-black uppercase text-on-surface truncate">{{ cb.name }}</h3>
                  <p class="text-[11px] text-on-surface-variant line-clamp-1">{{ cb.description }}</p>
                </div>
                <div class="flex items-center justify-between mt-2">
                  <span class="text-base font-black text-primary italic">{{ fmt(cb.price) }}đ</span>
                  <button @click="addCombo(cb)" class="w-8 h-8 rounded-full bg-primary/10 text-primary border border-primary/20 flex items-center justify-center hover:bg-primary/20 transition-colors">
                    <span class="material-symbols-outlined text-sm">add</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="mt-4 rounded-2xl border border-primary/20 bg-primary/5 flex items-center gap-3 pl-4 pr-3 py-2.5">
            <!-- Label -->
            <div class="flex items-center gap-2 shrink-0">
              <span class="material-symbols-outlined text-primary text-lg">shopping_bag</span>
              <h3 class="text-[10px] font-black uppercase tracking-widest text-primary leading-none">Giỏ F&B</h3>
              <span class="text-[10px] font-bold text-on-surface-variant whitespace-nowrap">· {{ selectedCombos.length }} món</span>
            </div>

            <!-- Items (cuộn ngang nếu nhiều) -->
            <div class="flex-grow min-w-0 flex items-center gap-2 overflow-x-auto custom-scrollbar">
              <p v-if="selectedCombos.length === 0" class="text-[11px] text-on-surface-variant/60 whitespace-nowrap">Chưa chọn combo nào — không bắt buộc.</p>
              <div v-for="item in selectedCombos" :key="item.id"
                   class="shrink-0 flex items-stretch gap-3 bg-surface-container-high rounded-2xl border border-outline-variant/10 hover:border-primary/30 transition-colors pl-4 pr-2.5 py-2">
                <div class="flex flex-col justify-center leading-tight">
                  <span class="text-[11px] font-bold text-on-surface whitespace-nowrap">{{ item.name }}</span>
                  <span class="text-[12px] font-black italic text-primary whitespace-nowrap tabular-nums">{{ fmt(item.price * item.quantity) }}đ</span>
                </div>
                <div class="flex items-center gap-1.5 pl-3 border-l border-outline-variant/10">
                  <button @click="changeComboQty(item, -1)"
                          class="w-6 h-6 rounded-lg bg-surface-container-lowest border border-outline-variant/15 text-on-surface-variant flex items-center justify-center hover:bg-primary hover:text-black hover:border-primary active:scale-90 transition-all">
                    <span class="material-symbols-outlined text-[16px] leading-none">remove</span>
                  </button>
                  <span class="w-5 text-center text-[13px] font-black tabular-nums text-on-surface">{{ item.quantity }}</span>
                  <button @click="changeComboQty(item, 1)"
                          class="w-6 h-6 rounded-lg bg-primary/15 border border-primary/30 text-primary flex items-center justify-center hover:bg-primary hover:text-black active:scale-90 transition-all">
                    <span class="material-symbols-outlined text-[16px] leading-none">add</span>
                  </button>
                </div>
              </div>
            </div>

            <!-- Subtotal + action -->
            <div class="shrink-0 flex items-center gap-3 pl-3 border-l border-primary/15">
              <div class="text-right hidden sm:block">
                <p class="text-[9px] font-bold uppercase tracking-wider text-on-surface-variant leading-none mb-0.5">Tạm tính</p>
                <p class="text-base font-black italic text-primary tracking-tighter leading-none">{{ fmt(comboTotal) }}đ</p>
              </div>
              <AppButton @click="currentStep = 5">5. Thanh toán</AppButton>
            </div>
          </div>
        </div>

        <!-- Step 5: Payment -->
        <div v-if="currentStep === 5" class="p-6 space-y-8 overflow-y-auto custom-scrollbar h-full">
          <div class="flex justify-between items-center">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 5. Thanh toán
            </h2>
            <AppButton variant="ghost" @click="currentStep = 4">Quay lại</AppButton>
          </div>
          <div class="grid grid-cols-2 gap-8">
            <div class="bg-surface-container-high p-8 rounded-3xl border border-outline-variant/10 space-y-5">
              <p class="text-[10px] font-black text-primary uppercase tracking-widest">Chi tiết hóa đơn</p>
              <h3 class="text-xl font-black italic uppercase text-on-surface">{{ selectedShowtime?.movieTitle }}</h3>
              <div class="flex justify-between text-xs font-bold text-on-surface-variant uppercase border-b border-outline-variant/10 pb-3">
                <span>Vé (x{{ selectedSeats.length }})</span>
                <span class="text-on-surface">{{ fmt(seatTotal) }}đ</span>
              </div>
              <div v-if="comboTotal > 0" class="flex justify-between text-xs font-bold text-on-surface-variant uppercase border-b border-outline-variant/10 pb-3">
                <span>F&B / Combo</span>
                <span class="text-on-surface">{{ fmt(comboTotal) }}đ</span>
              </div>
              <div v-if="discountAmount > 0" class="flex justify-between text-xs font-bold text-green-400 uppercase border-b border-outline-variant/10 pb-3">
                <span>Giảm giá <span v-if="appliedVoucher" class="normal-case">({{ appliedVoucher.code }})</span></span>
                <span>-{{ fmt(discountAmount) }}đ</span>
              </div>
              <div class="pt-3 flex justify-between items-end">
                <p class="text-[10px] font-black text-on-surface-variant uppercase">Tổng cộng</p>
                <p class="text-4xl font-black italic text-primary tracking-tighter">{{ fmt(payableTotal) }}đ</p>
              </div>
            </div>

            <div class="space-y-6">
              <div class="bg-primary/5 border border-primary/20 p-8 rounded-3xl space-y-4">
                <p class="text-[10px] font-black text-primary uppercase tracking-widest">Thành viên (tùy chọn — để tích điểm)</p>
                <div v-if="!member" class="space-y-3">
                  <input v-model="cardNumberInput" type="tel" inputmode="numeric" placeholder="Số điện thoại khách hàng..." class="w-full bg-surface-container-high border border-outline-variant/10 rounded-2xl py-3 px-5 text-on-surface text-sm font-bold outline-none focus:border-primary/50" />
                  <p v-if="cardError" class="text-xs text-red-400 font-bold">{{ cardError }}</p>
                  <AppButton variant="primary" class="w-full" @click="checkMemberCard" :disabled="isCheckingCard">{{ isCheckingCard ? 'Đang kiểm tra...' : 'Kiểm tra' }}</AppButton>
                </div>
                <div v-else class="space-y-2 text-on-surface">
                  <div class="flex justify-between items-center">
                    <p class="text-sm font-black uppercase">{{ member.fullName }}</p>
                    <span class="px-2 py-1 bg-primary text-on-primary text-[8px] font-black rounded uppercase">{{ member.membershipTier }}</span>
                  </div>
                  <p v-if="member.phone" class="text-xs text-on-surface-variant">SĐT: <span class="font-bold">{{ member.phone }}</span></p>
                  <p class="text-xs text-on-surface-variant">Điểm tích lũy: <span class="text-primary font-bold">{{ fmt(member.loyaltyPoints) }}</span></p>
                  <button @click="clearMember" class="text-[10px] text-on-surface-variant hover:text-red-400 font-bold uppercase">Bỏ thẻ</button>
                </div>
              </div>

              <!-- Voucher / khuyến mãi: chỉ bật sau khi đã tra cứu thành viên -->
              <div v-if="member" class="bg-surface-container-high border border-outline-variant/10 p-6 rounded-3xl space-y-3">
                <p class="text-[10px] font-black text-primary uppercase tracking-widest">Voucher / Khuyến mãi</p>
                <template v-if="!appliedVoucher">
                  <select v-if="ownedVouchers.length" v-model="voucherCodeInput"
                          class="w-full bg-surface-container border border-outline-variant/10 rounded-2xl py-3 px-4 text-on-surface text-sm font-bold outline-none focus:border-primary/50">
                    <option value="">— Voucher của khách ({{ ownedVouchers.length }}) —</option>
                    <option v-for="v in ownedVouchers" :key="v.id" :value="v.code">
                      {{ v.code }} · giảm {{ v.discountType === 'PERCENTAGE' ? v.discountValue + '%' : fmt(v.discountValue) + 'đ' }}
                    </option>
                  </select>
                  <div class="flex gap-2">
                    <input v-model="voucherCodeInput" type="text" placeholder="Nhập mã voucher..."
                           class="flex-1 bg-surface-container border border-outline-variant/10 rounded-2xl py-3 px-4 text-on-surface text-sm font-bold outline-none focus:border-primary/50 uppercase" />
                    <AppButton variant="primary" :disabled="isApplyingVoucher || !voucherCodeInput" @click="applyVoucher">
                      {{ isApplyingVoucher ? '...' : 'Áp dụng' }}
                    </AppButton>
                  </div>
                  <p v-if="voucherError" class="text-xs text-red-400 font-bold">{{ voucherError }}</p>
                </template>
                <div v-else class="flex items-center justify-between">
                  <div>
                    <p class="text-sm font-black text-green-400 uppercase">{{ appliedVoucher.code }}</p>
                    <p class="text-xs text-on-surface-variant">
                      Giảm {{ appliedVoucher.discountType === 'PERCENTAGE' ? appliedVoucher.discountValue + '%' : fmt(appliedVoucher.discountValue) + 'đ' }}
                      · -{{ fmt(discountAmount) }}đ
                    </p>
                  </div>
                  <button @click="clearVoucher" class="text-[10px] text-on-surface-variant hover:text-red-400 font-bold uppercase">Bỏ mã</button>
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="openCashModal" :disabled="isPaying">
                  <span class="material-symbols-outlined">payments</span> Tiền mặt
                </AppButton>
                <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="openQrModal" :disabled="isPaying">
                  <span class="material-symbols-outlined">qr_code_2</span> Chuyển khoản QR
                </AppButton>
              </div>
              <p v-if="isPaying" class="text-center text-xs text-on-surface-variant">Đang xử lý thanh toán...</p>
            </div>
          </div>
        </div>

        <!-- Step 6: Done -->
        <div v-if="currentStep === 6" class="p-6 flex flex-col items-center justify-center text-center h-full space-y-8">
          <div class="w-24 h-24 bg-green-500/20 text-green-500 rounded-full flex items-center justify-center shadow-2xl shadow-green-500/20">
            <span class="material-symbols-outlined text-6xl">check_circle</span>
          </div>
          <div>
            <h2 class="text-4xl font-black uppercase italic tracking-tighter text-on-surface">Thanh toán thành công</h2>
            <p class="text-on-surface-variant font-bold mt-2 uppercase tracking-widest text-xs">Xuất vé và bàn giao cho khách</p>
          </div>

          <div class="bg-surface-container-high p-8 rounded-3xl border border-outline-variant/10 w-full max-w-md space-y-6 text-left">
            <div class="flex justify-between items-start">
              <div>
                <p class="text-[10px] font-black text-primary uppercase">Mã đặt vé</p>
                <p class="text-xl font-black text-on-surface">{{ completedBooking?.bookingCode }}</p>
              </div>
              <div class="text-right">
                <p class="text-[10px] font-black text-primary uppercase">Phòng</p>
                <p class="text-xl font-black text-on-surface">{{ selectedShowtime?.roomName }}</p>
              </div>
            </div>
            <div class="border-t border-dashed border-outline-variant/20 pt-6">
              <p class="text-[10px] font-black text-on-surface-variant uppercase mb-2">Thông tin vé</p>
              <div class="flex justify-between text-sm font-bold text-on-surface">
                <span>{{ selectedSeats.length }} ghế: {{ selectedSeats.map(s => s.rowChar + s.colNum).join(', ') }}</span>
                <span class="text-primary italic">{{ fmt(totalPrice) }}đ</span>
              </div>
            </div>
          </div>

          <div class="flex gap-4">
            <AppButton variant="primary" size="lg" class="flex items-center gap-3" @click="printInvoice">
              <span class="material-symbols-outlined">print</span> In hoá đơn
            </AppButton>
            <AppButton variant="outline" size="lg" class="flex items-center gap-3" @click="resetPOS">
              <span class="material-symbols-outlined">add_circle</span> Giao dịch mới
            </AppButton>
          </div>
        </div>
        </template>

        <!-- ===== Luồng bán nhanh F&B (Concession Only) ===== -->
        <template v-else>
          <!-- FNB Step 1: Chọn món -->
          <div v-if="fnbStep === 1" class="p-6 flex flex-col h-full overflow-hidden">
            <div class="flex justify-between items-center mb-6">
              <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
                <span class="w-8 h-1 bg-primary rounded-full"></span> Bán nhanh bắp nước & combo
              </h2>
              <span class="text-[11px] font-bold text-on-surface-variant uppercase tracking-widest">Khách vãng lai · Không cần vé</span>
            </div>

            <div v-if="isLoading" class="grid grid-cols-3 gap-5">
              <div v-for="i in 6" :key="i" class="h-28 bg-surface-container-high rounded-3xl animate-pulse"></div>
            </div>
            <div v-else-if="combos.length === 0" class="flex-grow flex items-center justify-center text-on-surface-variant text-sm">
              Chưa có món F&B. Thêm ở "Thực đơn F&B / Combo".
            </div>
            <div v-else class="grid grid-cols-3 gap-5 flex-grow overflow-y-auto custom-scrollbar pr-2 content-start">
              <div v-for="cb in combos" :key="cb.id" class="p-5 bg-surface-container-high rounded-3xl border border-outline-variant/10 flex gap-4 hover:border-primary/30 transition-all">
                <div class="w-16 h-16 rounded-2xl overflow-hidden bg-surface-container-highest shrink-0 flex items-center justify-center">
                  <img v-if="cb.imageUrl" :src="cb.imageUrl" class="w-full h-full object-cover" />
                  <span v-else class="material-symbols-outlined text-on-surface-variant/40">fastfood</span>
                </div>
                <div class="flex flex-col justify-between flex-grow min-w-0">
                  <div>
                    <h3 class="text-sm font-black uppercase text-on-surface truncate">{{ cb.name }}</h3>
                    <p class="text-[11px] text-on-surface-variant line-clamp-1">{{ cb.description }}</p>
                  </div>
                  <div class="flex items-center justify-between mt-2">
                    <span class="text-base font-black text-primary italic">{{ fmt(cb.price) }}đ</span>
                    <button @click="addCombo(cb)" class="w-8 h-8 rounded-full bg-primary/10 text-primary border border-primary/20 flex items-center justify-center hover:bg-primary/20 transition-colors">
                      <span class="material-symbols-outlined text-sm">add</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Giỏ F&B + nút Thanh toán -->
            <div class="mt-4 rounded-2xl border border-primary/20 bg-primary/5 flex items-center gap-3 pl-4 pr-3 py-2.5">
              <div class="flex items-center gap-2 shrink-0">
                <span class="material-symbols-outlined text-primary text-lg">shopping_bag</span>
                <h3 class="text-[10px] font-black uppercase tracking-widest text-primary leading-none">Giỏ F&B</h3>
                <span class="text-[10px] font-bold text-on-surface-variant whitespace-nowrap">· {{ selectedCombos.length }} món</span>
              </div>
              <div class="flex-grow min-w-0 flex items-center gap-2 overflow-x-auto custom-scrollbar">
                <p v-if="selectedCombos.length === 0" class="text-[11px] text-on-surface-variant/60 whitespace-nowrap">Chọn món để thêm vào giỏ.</p>
                <div v-for="item in selectedCombos" :key="item.id" class="shrink-0 flex items-stretch gap-3 bg-surface-container-high rounded-2xl border border-outline-variant/10 pl-4 pr-2.5 py-2">
                  <div class="flex flex-col justify-center leading-tight">
                    <span class="text-[11px] font-bold text-on-surface whitespace-nowrap">{{ item.name }}</span>
                    <span class="text-[12px] font-black italic text-primary whitespace-nowrap tabular-nums">{{ fmt(item.price * item.quantity) }}đ</span>
                  </div>
                  <div class="flex items-center gap-1.5 pl-3 border-l border-outline-variant/10">
                    <button @click="changeComboQty(item, -1)" class="w-6 h-6 rounded-lg bg-surface-container-lowest border border-outline-variant/15 text-on-surface-variant flex items-center justify-center hover:bg-primary hover:text-black hover:border-primary active:scale-90 transition-all">
                      <span class="material-symbols-outlined text-[16px] leading-none">remove</span>
                    </button>
                    <span class="w-5 text-center text-[13px] font-black tabular-nums text-on-surface">{{ item.quantity }}</span>
                    <button @click="changeComboQty(item, 1)" class="w-6 h-6 rounded-lg bg-primary/15 border border-primary/30 text-primary flex items-center justify-center hover:bg-primary hover:text-black active:scale-90 transition-all">
                      <span class="material-symbols-outlined text-[16px] leading-none">add</span>
                    </button>
                  </div>
                </div>
              </div>
              <div class="shrink-0 flex items-center gap-3 pl-3 border-l border-primary/15">
                <div class="text-right hidden sm:block">
                  <p class="text-[9px] font-bold uppercase tracking-wider text-on-surface-variant leading-none mb-0.5">Tạm tính</p>
                  <p class="text-base font-black italic text-primary tracking-tighter leading-none">{{ fmt(comboTotal) }}đ</p>
                </div>
                <AppButton :disabled="selectedCombos.length === 0" @click="fnbStep = 2">Thanh toán</AppButton>
              </div>
            </div>
          </div>

          <!-- FNB Step 2: Thanh toán -->
          <div v-if="fnbStep === 2" class="p-6 space-y-8 overflow-y-auto custom-scrollbar h-full">
            <div class="flex justify-between items-center">
              <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
                <span class="w-8 h-1 bg-primary rounded-full"></span> Thanh toán bắp nước
              </h2>
              <AppButton variant="ghost" @click="fnbStep = 1">Quay lại</AppButton>
            </div>
            <div class="grid grid-cols-2 gap-8">
              <div class="bg-surface-container-high p-8 rounded-3xl border border-outline-variant/10 space-y-4">
                <p class="text-[10px] font-black text-primary uppercase tracking-widest">Chi tiết hóa đơn</p>
                <div v-for="c in selectedCombos" :key="c.id" class="flex justify-between text-xs font-bold text-on-surface-variant uppercase border-b border-outline-variant/10 pb-3">
                  <span>{{ c.name }} <span class="text-on-surface-variant/60">x{{ c.quantity }}</span></span>
                  <span class="text-on-surface">{{ fmt(c.price * c.quantity) }}đ</span>
                </div>
                <div class="pt-3 flex justify-between items-end">
                  <p class="text-[10px] font-black text-on-surface-variant uppercase">Tổng cộng</p>
                  <p class="text-4xl font-black italic text-primary tracking-tighter">{{ fmt(totalPrice) }}đ</p>
                </div>
              </div>
              <div class="space-y-6">
                <div class="bg-primary/5 border border-primary/20 p-8 rounded-3xl space-y-4">
                  <p class="text-[10px] font-black text-primary uppercase tracking-widest">Thành viên (tùy chọn — để tích điểm)</p>
                  <div v-if="!member" class="space-y-3">
                    <input v-model="cardNumberInput" type="tel" inputmode="numeric" placeholder="Số điện thoại khách hàng..." class="w-full bg-surface-container-high border border-outline-variant/10 rounded-2xl py-3 px-5 text-on-surface text-sm font-bold outline-none focus:border-primary/50" />
                    <p v-if="cardError" class="text-xs text-red-400 font-bold">{{ cardError }}</p>
                    <AppButton variant="primary" class="w-full" @click="checkMemberCard" :disabled="isCheckingCard">{{ isCheckingCard ? 'Đang kiểm tra...' : 'Kiểm tra' }}</AppButton>
                  </div>
                  <div v-else class="space-y-2 text-on-surface">
                    <div class="flex justify-between items-center">
                      <p class="text-sm font-black uppercase">{{ member.fullName }}</p>
                      <span class="px-2 py-1 bg-primary text-on-primary text-[8px] font-black rounded uppercase">{{ member.membershipTier }}</span>
                    </div>
                    <p v-if="member.phone" class="text-xs text-on-surface-variant">SĐT: <span class="font-bold">{{ member.phone }}</span></p>
                    <p class="text-xs text-on-surface-variant">Điểm tích lũy: <span class="text-primary font-bold">{{ fmt(member.loyaltyPoints) }}</span></p>
                    <button @click="clearMember" class="text-[10px] text-on-surface-variant hover:text-red-400 font-bold uppercase">Bỏ thẻ</button>
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-4">
                  <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="openCashModal" :disabled="isPaying">
                    <span class="material-symbols-outlined">payments</span> Tiền mặt
                  </AppButton>
                  <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="openQrModal" :disabled="isPaying">
                    <span class="material-symbols-outlined">qr_code_2</span> Chuyển khoản QR
                  </AppButton>
                </div>
                <p v-if="isPaying" class="text-center text-xs text-on-surface-variant">Đang xử lý thanh toán...</p>
              </div>
            </div>
          </div>

          <!-- FNB Step 3: Done -->
          <div v-if="fnbStep === 3" class="p-6 flex flex-col items-center justify-center text-center h-full space-y-8">
            <div class="w-24 h-24 bg-green-500/20 text-green-500 rounded-full flex items-center justify-center shadow-2xl shadow-green-500/20">
              <span class="material-symbols-outlined text-6xl">check_circle</span>
            </div>
            <div>
              <h2 class="text-4xl font-black uppercase italic tracking-tighter text-on-surface">Thanh toán thành công</h2>
              <p class="text-on-surface-variant font-bold mt-2 uppercase tracking-widest text-xs">Giao bắp nước cho khách</p>
            </div>
            <div class="bg-surface-container-high p-8 rounded-3xl border border-outline-variant/10 w-full max-w-md space-y-6 text-left">
              <div class="flex justify-between items-center">
                <div>
                  <p class="text-[10px] font-black text-primary uppercase">Mã hoá đơn</p>
                  <p class="text-xl font-black text-on-surface">{{ concessionSale?.saleCode }}</p>
                </div>
                <div class="text-right">
                  <p class="text-[10px] font-black text-primary uppercase">Tổng tiền</p>
                  <p class="text-xl font-black text-primary italic">{{ fmt(comboTotal) }}đ</p>
                </div>
              </div>
            </div>
            <div class="flex gap-4">
              <AppButton variant="primary" size="lg" class="flex items-center gap-3" @click="printConcessionInvoice">
                <span class="material-symbols-outlined">print</span> In hoá đơn
              </AppButton>
              <AppButton variant="outline" size="lg" class="flex items-center gap-3" @click="newConcessionSale">
                <span class="material-symbols-outlined">add_circle</span> Giao dịch mới
              </AppButton>
            </div>

            <!-- Bấm nhầm? Yêu cầu Trưởng ca duyệt HỦY hóa đơn (nhân viên quầy không tự hủy được) -->
            <div class="w-full max-w-md">
              <div v-if="voidRequested" class="flex items-center justify-center gap-2 text-amber-400 text-sm font-bold">
                <span class="material-symbols-outlined text-base">hourglass_top</span>
                Đã gửi yêu cầu hủy — chờ Trưởng ca duyệt
              </div>
              <template v-else>
                <button v-if="!showVoidForm" class="text-xs text-on-surface-variant hover:text-red-400 underline transition-colors" @click="showVoidForm = true">
                  Bấm nhầm? Yêu cầu hủy hóa đơn
                </button>
                <div v-else class="bg-surface-container-high p-4 rounded-2xl border border-red-500/20 space-y-3 text-left">
                  <p class="text-xs font-black text-red-400 uppercase tracking-wider">Yêu cầu hủy hóa đơn {{ concessionSale?.saleCode }}</p>
                  <textarea v-model="voidReason" rows="2" placeholder="Lý do (VD: bấm nhầm số lượng, khách chưa trả tiền...)" class="w-full bg-surface-container rounded-xl px-3 py-2 text-sm outline-none"></textarea>
                  <div class="flex gap-2 justify-end">
                    <AppButton variant="ghost" size="sm" @click="showVoidForm = false">Đóng</AppButton>
                    <AppButton variant="primary" size="sm" :loading="isRequestingVoid" @click="handleRequestVoid">Gửi yêu cầu</AppButton>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </template>
      </div>

      <!-- Right: Cart summary -->
      <div class="col-span-3 bg-surface-container-low border border-outline-variant/10 rounded-3xl shadow-2xl p-6 flex flex-col">
        <div class="flex items-center gap-2 pb-5 mb-5 border-b border-outline-variant/10">
          <span class="material-symbols-outlined text-primary">receipt_long</span>
          <h2 class="text-sm font-black uppercase tracking-[0.2em] text-primary">Biên lai tạm tính</h2>
        </div>
        <div v-if="selectedShowtime || selectedCombos.length" class="space-y-5 flex-grow overflow-y-auto custom-scrollbar pr-1">
          <div v-if="saleMode === 'FNB'" class="pb-5 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-primary uppercase tracking-wider mb-1.5">Bán nhanh F&B</p>
            <h3 class="text-base font-black uppercase italic text-on-surface leading-tight">Bắp nước & Combo</h3>
            <p class="text-xs text-on-surface-variant mt-1.5">Khách vãng lai · không kèm vé</p>
          </div>
          <div v-if="selectedShowtime" class="pb-5 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-1.5">Phim & Suất</p>
            <h3 class="text-base font-black uppercase italic text-on-surface leading-tight">{{ selectedShowtime.movieTitle }}</h3>
            <p class="text-xs text-on-surface-variant mt-1.5">{{ selectedShowtime.roomName }} • {{ selectedShowtime.formatName }}</p>
            <p class="text-xs text-on-surface-variant">{{ new Date(selectedShowtime.startTime).toLocaleString('vi-VN', { weekday: 'short', hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }) }}</p>
          </div>
          <div v-if="selectedSeats.length" class="pb-5 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-2">Ghế ({{ selectedSeats.length }})</p>
            <p class="text-sm text-primary font-black mb-3">{{ selectedSeats.map(s => s.rowChar + s.colNum).join(', ') }}</p>
            <div v-for="b in seatTypeBreakdown" :key="b.type" class="flex justify-between text-xs font-semibold text-on-surface-variant mb-1">
              <span>Ghế {{ seatTypeLabel(b.type) }} <span class="text-on-surface-variant/60">x{{ b.count }}</span></span>
              <span class="text-on-surface">{{ fmt(b.subtotal) }}đ</span>
            </div>
          </div>
          <div v-if="selectedCombos.length" class="pb-5 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-2">F&B / Combo</p>
            <div v-for="c in selectedCombos" :key="c.id" class="flex justify-between text-xs font-semibold mb-1">
              <span class="text-on-surface-variant">{{ c.name }} <span class="text-on-surface-variant/60">x{{ c.quantity }}</span></span>
              <span class="text-on-surface">{{ fmt(c.price * c.quantity) }}đ</span>
            </div>
          </div>
        </div>
        <div v-else class="flex-grow flex flex-col items-center justify-center text-on-surface-variant/40 gap-2">
          <span class="material-symbols-outlined text-4xl">shopping_cart</span>
          <p class="text-xs font-semibold">{{ saleMode === 'FNB' ? 'Chưa chọn món nào' : 'Chưa chọn suất chiếu' }}</p>
        </div>

        <div v-if="discountAmount > 0" class="pt-4 mt-2 flex justify-between items-center text-xs font-bold text-green-400">
          <span class="uppercase tracking-wider">Giảm giá {{ appliedVoucher ? '(' + appliedVoucher.code + ')' : '' }}</span>
          <span>-{{ fmt(discountAmount) }}đ</span>
        </div>
        <div class="pt-5 mt-3 border-t border-outline-variant/10 flex justify-between items-center">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Tổng tiền</p>
          <p class="text-3xl font-black italic tracking-tighter text-primary">{{ fmt(payableTotal) }}đ</p>
        </div>
      </div>
    </main>

    <!-- Modal: Thanh toán tiền mặt -->
    <transition name="fade">
      <div v-if="showCashModal" class="fixed inset-0 z-[1200] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4" @click.self="showCashModal = false">
        <div class="w-full max-w-md bg-surface border border-outline-variant/15 rounded-3xl shadow-2xl overflow-hidden">
          <div class="px-7 py-5 border-b border-outline-variant/10 flex items-center gap-3">
            <span class="material-symbols-outlined text-primary">payments</span>
            <h3 class="text-lg font-black uppercase italic tracking-tighter text-on-surface">Thanh toán tiền mặt</h3>
          </div>
          <div class="p-7 space-y-6">
            <div v-if="cashRoundingDelta !== 0" class="space-y-1.5">
              <div class="flex justify-between text-xs font-bold text-on-surface-variant">
                <span class="uppercase tracking-widest">Tổng hóa đơn</span>
                <span>{{ fmt(payableTotal) }}đ</span>
              </div>
              <div class="flex justify-between text-xs font-bold text-amber-300">
                <span class="uppercase tracking-widest">Làm tròn tiền mặt</span>
                <span>{{ cashRoundingLabel }}</span>
              </div>
            </div>
            <div class="flex justify-between items-end">
              <span class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tổng phải trả</span>
              <span class="text-3xl font-black italic text-primary tracking-tighter">{{ fmt(cashPayable) }}đ</span>
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tiền khách đưa</label>
              <div class="relative">
                <input v-model="cashGivenDisplay" type="text" inputmode="numeric" maxlength="13" placeholder="0"
                       :class="cashError ? 'border-red-500/60' : 'border-outline-variant/10 focus:border-primary/50'"
                       class="w-full bg-surface-container-high border rounded-2xl py-3.5 pl-5 pr-9 text-on-surface text-xl font-black outline-none tabular-nums" />
                <span class="absolute right-5 top-1/2 -translate-y-1/2 text-on-surface-variant font-black pointer-events-none">đ</span>
              </div>
              <p v-if="cashError" class="text-xs text-red-400 font-bold flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">error</span>{{ cashError }}
              </p>
              <div class="flex flex-wrap gap-2 pt-1">
                <button v-for="s in cashSuggestions" :key="s" @click="cashGiven = s"
                        class="px-3 py-1.5 rounded-lg bg-surface-container-high border border-outline-variant/10 text-[11px] font-bold text-on-surface hover:border-primary/40 hover:text-primary transition-all tabular-nums">{{ fmt(s) }}đ</button>
              </div>
            </div>
            <div class="flex justify-between items-center p-4 rounded-2xl bg-surface-container-high border border-outline-variant/10">
              <span class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tiền trả lại</span>
              <span class="text-2xl font-black italic tracking-tighter" :class="changeDue > 0 ? 'text-green-400' : 'text-on-surface'">{{ fmt(changeDue) }}đ</span>
            </div>
          </div>
          <div class="px-7 py-5 border-t border-outline-variant/10 flex gap-3">
            <AppButton variant="ghost" class="flex-1" @click="showCashModal = false">Hủy</AppButton>
            <button v-if="canHoldOrder" @click="holdCurrentOrder" :disabled="isPaying || isHolding"
                    class="flex items-center justify-center gap-1.5 px-4 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-[11px] font-black uppercase tracking-wider hover:bg-amber-500/20 transition-all disabled:opacity-40" title="Lưu đơn vào danh sách chờ (giữ số tiền gốc)">
              <span class="material-symbols-outlined text-base">{{ isHolding ? 'progress_activity' : 'pause_circle' }}</span> {{ isHolding ? 'Đang giữ...' : 'Giữ đơn' }}
            </button>
            <AppButton variant="primary" class="flex-1" :disabled="!canConfirmCash || isPaying" @click="processPayment('CASH')">
              {{ isPaying ? 'Đang xử lý...' : 'Xác nhận thanh toán' }}
            </AppButton>
          </div>
        </div>
      </div>
    </transition>

    <!-- Modal: Chuyển khoản QR (VietQR) -->
    <transition name="fade">
      <div v-if="showQrModal" class="fixed inset-0 z-[1200] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4" @click.self="showQrModal = false">
        <div class="w-full max-w-lg bg-surface border border-outline-variant/15 rounded-3xl shadow-2xl overflow-hidden">
          <div class="px-7 py-5 border-b border-outline-variant/10 flex items-center gap-3">
            <span class="material-symbols-outlined text-primary">qr_code_2</span>
            <h3 class="text-lg font-black uppercase italic tracking-tighter text-on-surface">Chuyển khoản QR</h3>
          </div>

          <div v-if="!cleanQrUrl" class="p-10 text-center space-y-3">
            <span class="material-symbols-outlined text-5xl text-on-surface-variant/40">account_balance</span>
            <p class="text-sm font-bold text-on-surface">Chưa cấu hình tài khoản nhận tiền.</p>
            <p class="text-xs text-on-surface-variant">Vào <b class="text-on-surface">Cài đặt → Tài khoản nhận tiền</b> để thêm Ngân hàng + STK.</p>
          </div>

          <div v-else class="p-7 space-y-5">
            <div class="flex flex-col items-center gap-3">
              <div class="w-80 h-80 max-w-full rounded-2xl bg-white p-3 flex items-center justify-center shadow-lg">
                <img :src="cleanQrUrl" alt="VietQR" class="w-full h-full object-contain" />
              </div>
              <p class="text-xs text-on-surface-variant text-center">Khách dùng app ngân hàng quét mã — số tiền &amp; nội dung tự điền.</p>
              <button @click="openQrFullscreen"
                      class="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-primary/10 border border-primary/30 text-primary text-[11px] font-black uppercase tracking-widest hover:bg-primary hover:text-black transition-all">
                <span class="material-symbols-outlined text-[18px]">open_in_full</span> Hiển thị mã cho khách
              </button>
            </div>
            <div class="space-y-2.5 text-sm">
              <div class="flex justify-between"><span class="text-on-surface-variant">Ngân hàng</span><span class="font-bold text-on-surface">{{ bankInfo.name }}</span></div>
              <div class="flex justify-between"><span class="text-on-surface-variant">Số tài khoản</span><span class="font-bold text-on-surface font-mono">{{ bankInfo.accountNo }}</span></div>
              <div class="flex justify-between"><span class="text-on-surface-variant">Chủ tài khoản</span><span class="font-bold text-on-surface uppercase">{{ bankInfo.accountName }}</span></div>
              <div class="flex justify-between"><span class="text-on-surface-variant">Số tiền</span><span class="font-black text-primary italic">{{ fmt(totalPrice) }}đ</span></div>
              <div class="flex justify-between"><span class="text-on-surface-variant">Nội dung</span><span class="font-bold text-on-surface">{{ transferContent }}</span></div>
            </div>
          </div>

          <div class="px-7 py-5 border-t border-outline-variant/10 flex gap-3">
            <AppButton variant="ghost" class="flex-1" @click="showQrModal = false">Hủy</AppButton>
            <AppButton variant="primary" class="flex-1" :disabled="!cleanQrUrl || isPaying" @click="processPayment('TRANSFER')">
              {{ isPaying ? 'Đang xử lý...' : 'Xác nhận đã chuyển khoản' }}
            </AppButton>
          </div>
        </div>
      </div>
    </transition>

    <!-- Panel: Danh sách hoá đơn chờ (Hold Orders) -->
    <transition name="fade">
      <div v-if="showHeldPanel" class="fixed inset-0 z-[1200] flex justify-end bg-black/60 backdrop-blur-sm" @click.self="showHeldPanel = false">
        <aside class="w-full max-w-md h-full bg-surface border-l border-outline-variant/15 shadow-2xl flex flex-col">
          <div class="px-6 py-5 border-b border-outline-variant/10 flex items-center justify-between">
            <div class="flex items-center gap-3">
              <span class="material-symbols-outlined text-primary">receipt_long</span>
              <div>
                <h3 class="text-base font-black uppercase italic tracking-tighter text-on-surface leading-none">Hoá đơn chờ</h3>
                <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest mt-1">{{ heldOrders.length }} đơn đang giữ</p>
              </div>
            </div>
            <button @click="showHeldPanel = false" class="w-8 h-8 rounded-lg hover:bg-surface-container-high flex items-center justify-center text-on-surface-variant">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div v-if="heldOrders.length === 0" class="flex-grow flex flex-col items-center justify-center text-on-surface-variant/40 gap-3 px-8 text-center">
            <span class="material-symbols-outlined text-5xl">inbox</span>
            <p class="text-sm font-semibold">Chưa có đơn nào đang chờ.</p>
            <p class="text-xs">Bấm <b class="text-primary">Giữ đơn</b> khi cần xử lý khách tiếp theo trong hàng đợi.</p>
          </div>

          <div v-else class="flex-grow overflow-y-auto custom-scrollbar p-4 space-y-3">
            <div v-for="o in heldOrders" :key="o.code"
                 class="p-4 bg-surface-container-high rounded-2xl border border-outline-variant/10 hover:border-primary/30 transition-all">
              <div class="flex items-center justify-between mb-2">
                <span class="px-2.5 py-1 rounded-lg bg-primary/15 border border-primary/30 text-primary text-[11px] font-black tracking-wider font-mono">{{ o.code }}</span>
                <div class="flex items-center gap-2">
                  <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider">{{ heldAgeLabel(o.createdAt) }}</span>
                  <!-- Đếm ngược giữ ghế (chỉ đơn có vé) -->
                  <span v-if="heldRemainingSec(o) != null"
                        :class="heldRemainingSec(o) <= 60 ? 'bg-red-500/15 border-red-500/40 text-red-300 animate-pulse' : 'bg-primary/10 border-primary/30 text-primary'"
                        class="flex items-center gap-1 px-2 py-0.5 rounded-lg border text-[10px] font-black tabular-nums" title="Thời gian giữ ghế còn lại">
                    <span class="material-symbols-outlined text-[13px]">timer</span>{{ heldCountdown(o) }}
                  </span>
                </div>
              </div>
              <h4 class="text-sm font-black uppercase text-on-surface truncate">{{ o.mode === 'FNB' ? 'Bán nhanh F&B' : o.showtime?.movieTitle }}</h4>
              <p v-if="o.mode === 'FNB'" class="text-[11px] text-on-surface-variant mt-0.5">Khách vãng lai · không kèm vé</p>
              <p v-else class="text-[11px] text-on-surface-variant mt-0.5">{{ String(o.showtime?.formatName).toUpperCase() }} • {{ o.showtime?.roomName }} • {{ fmtTime(o.showtime?.startTime) }}</p>
              <div class="flex items-center gap-2 mt-2 flex-wrap">
                <template v-if="o.mode === 'FNB'">
                  <span class="material-symbols-outlined text-sm text-on-surface-variant">lunch_dining</span>
                  <span class="text-xs font-bold text-primary">{{ (o.combos || []).length }} món · {{ (o.combos || []).reduce((a, c) => a + c.quantity, 0) }} phần</span>
                </template>
                <template v-else>
                  <span class="material-symbols-outlined text-sm text-on-surface-variant">event_seat</span>
                  <span class="text-xs font-bold text-primary">{{ (o.seats || []).map(s => s.rowChar + s.colNum).join(', ') }}</span>
                  <span v-if="o.combos?.length" class="text-[10px] text-on-surface-variant">· {{ o.combos.length }} món F&B</span>
                </template>
              </div>
              <div class="flex items-center justify-between mt-3 pt-3 border-t border-outline-variant/10">
                <span class="text-base font-black italic text-primary tabular-nums">{{ fmt(o.total) }}đ</span>
                <div class="flex items-center gap-2">
                  <button @click="askDeleteHeldOrder(o)"
                          class="w-9 h-9 rounded-lg bg-surface-container-lowest border border-outline-variant/15 text-on-surface-variant hover:text-red-400 hover:border-red-500/30 flex items-center justify-center transition-all" title="Huỷ đơn chờ">
                    <span class="material-symbols-outlined text-lg">delete</span>
                  </button>
                  <button @click="restoreHeldOrder(o)"
                          class="flex items-center gap-1.5 px-4 py-2 rounded-lg bg-primary text-on-primary text-[11px] font-black uppercase tracking-wider hover:brightness-110 transition-all" title="Khôi phục đơn">
                    <span class="material-symbols-outlined text-base">restore</span> Gọi lại
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="px-6 py-4 border-t border-outline-variant/10 flex items-center gap-2 text-[10px] text-on-surface-variant">
            <span class="material-symbols-outlined text-sm">info</span>
            Ghế đã bị bán trong lúc chờ sẽ tự loại khi gọi lại đơn.
          </div>
        </aside>
      </div>
    </transition>

    <!-- Modal: Xác nhận huỷ đơn chờ -->
    <transition name="fade">
      <div v-if="confirmDeleteHold" class="fixed inset-0 z-[1250] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4" @click.self="cancelDeleteHeldOrder">
        <div class="w-full max-w-sm bg-surface border border-outline-variant/15 rounded-3xl shadow-2xl overflow-hidden">
          <div class="p-7 text-center space-y-4">
            <div class="w-16 h-16 mx-auto rounded-full bg-red-500/15 border border-red-500/30 flex items-center justify-center">
              <span class="material-symbols-outlined text-3xl text-red-400">delete_forever</span>
            </div>
            <div>
              <h3 class="text-lg font-black uppercase italic tracking-tighter text-on-surface">Huỷ hoá đơn chờ?</h3>
              <p class="text-sm text-on-surface-variant mt-2">Bạn có chắc chắn muốn huỷ đơn chờ
                <b class="text-primary font-mono">{{ confirmDeleteHold.code }}</b>? Ghế đang giữ (nếu có) sẽ được giải phóng.</p>
            </div>
          </div>
          <div class="px-7 py-5 border-t border-outline-variant/10 flex gap-3">
            <AppButton variant="ghost" class="flex-1" @click="cancelDeleteHeldOrder">Không</AppButton>
            <AppButton variant="primary" class="flex-1 !bg-red-500 !border-red-500" @click="confirmDeleteHeldOrder">Huỷ đơn</AppButton>
          </div>
        </div>
      </div>
    </transition>

    <!-- Toast -->
    <transition name="fade">
      <div v-if="toast.show" :class="[
        'fixed top-20 right-6 z-[1300] px-5 py-3 rounded-xl shadow-2xl text-sm font-bold flex items-center gap-2 border',
        toast.type === 'success' ? 'bg-green-600 border-green-400 text-white' : 'bg-red-600 border-red-400 text-white'
      ]">
        <span class="material-symbols-outlined text-base">{{ toast.type === 'success' ? 'check_circle' : 'error' }}</span>
        {{ toast.message }}
      </div>
    </transition>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; height: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(245, 197, 24, 0.2); border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: rgba(245, 197, 24, 0.4); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(10px); }
</style>
