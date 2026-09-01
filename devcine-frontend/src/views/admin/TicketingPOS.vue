<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { ticketingApi, settingsApi, approvalApi, bookingAdminApi, posPendingOrderApi } from '@/api/admin/index'
import { voucherApi } from '@/api/customer/index'
import AppButton from '../../components/common/AppButton.vue'
import SeatGridRenderer from '@/components/common/SeatGridRenderer.vue'
import { useSeatRealtime } from '@/composables/useSeatRealtime'
import { useSeatGridRender } from '@/composables/useSeatGridRender'
import { useOrphanSeatCheck } from '@/composables/useOrphanSeatCheck'
import { useToastStore } from '@/stores/toast'
import { useAuthStore } from '@/stores/auth'
import { friendlyError } from '@/utils/friendlyError'
import FnbOptionModal from '@/components/FnbOptionModal.vue'
import { openInvoice } from '@/utils/invoiceTemplate'

const currentStep = ref(1) // 1: Showtime, 2: Seats, 3: Confirm, 4: F&B, 5: Payment, 6: Done
const showMobileReceiptDrawer = ref(false)

const nowTs = ref(Date.now())
let nowTimer = null
const bookingError = ref('')
const showOutOfStockModal = ref(false)
const outOfStockMessage = ref('')

const restoredBookingId = ref(null)

const lateBookingMinutes = ref(15)

const getTodayYmd = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
const selectedPosDate = ref(getTodayYmd())

const showtimes = ref([])
const combos = ref([])
const isLoading = ref(false)

const selectedShowtime = ref(null)
const seatData = ref({ matrixRow: 9, matrixCol: 10, seats: [] })
const isLoadingSeats = ref(false)
const selectedSeats = ref([]) // seat objects from map
// POS override: nhân viên bật để bán khách ngoại lệ dù để trống 1 ghế lẻ (backend gate theo quyền POS)
const allowOrphan = ref(false)
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
// ── Snapshot bảng giá vé: được đóng băng khi Thu ngân vào Bước 3 (xác nhận loại vé) ──
// Từ đó trở đi, onPricingUpdate WebSocket KHÔNG override giá đang trong phiên.
// null = chưa lock (dùng priceTable live); non-null = đã lock (dùng giá tại Bước 3).
const lockedPriceTable = ref(null)
// ── Snapshot giá catalog F&B: được đóng băng khi Thu ngân vào Bước 4 (combo) hoặc chế độ Bán nhanh F&B ──
// Ngăn onFnbUpdate WebSocket thay đổi giá hiển thị trên card catalog giữa phiên giao dịch.
// null = chưa lock (dùng giá DB live); non-null = Map<id, snapshotPrice> đóng băng tại thời điểm bắt đầu chọn combo.
const lockedCombosPrices = ref(null)

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
const qrBookingId = ref(null)
const qrHoldLoading = ref(false)
const sessionStartedAt = ref(null)
const cashGiven = ref(0)

const error = ref('')
// POS không còn phụ thuộc Ca làm việc — nhân viên có quyền pos_ticketing là bán/soát được ngay.
const canUseTicketing = computed(() => true)
const canUseFnb = computed(() => true)

const toastStore = useToastStore()
// Giữ tên showToast để không phải sửa ~40 lời gọi rải khắp file
const showToast = (message, type = 'success') => toastStore.push(message, type)

const auth = useAuthStore()
// Chỉ ADMIN/MANAGER được bật "Cho phép lẻ ghế" (khớp gate vai trò ở backend). STAFF không thấy nút.
const canOverrideOrphan = computed(() => auth.isAdmin || auth.isManager)

const seatTypeLabel = (t) => ({ NORMAL: 'Thường', STANDARD: 'Thường', VIP: 'VIP', SWEETBOX: 'Sweetbox' }[t] || t)
// Sức chứa theo loại ghế: Ghế đơn (NORMAL, VIP) = 1 vé; Ghế đôi (SWEETBOX) = 2 vé
const seatCapacity = (seat) => (seat && seat.seatType === 'SWEETBOX' ? 2 : 1)

const DEFAULT_AUDIENCE_LABELS = { ADULT: 'Người lớn', U22: 'U22 / HSSV', CHILD: 'Trẻ em', SENIOR: 'Cao tuổi' }
// Lưu bảng giá + nhãn đối tượng từ response ghế để đổi loại vé không cần gọi lại server
const captureSeatMeta = (data) => {
  if (data && data.priceTable) priceTable.value = data.priceTable
  if (data && data.audienceLabels && Object.keys(data.audienceLabels).length) audienceLabels.value = data.audienceLabels
  if (!Object.keys(audienceLabels.value).length) audienceLabels.value = DEFAULT_AUDIENCE_LABELS
}
// Giá 1 ghế theo loại vé đang chọn (với SWEETBOX tính tổng 2 vé, fallback về giá ADULT sẵn có nếu thiếu bảng giá)
// ── Snapshot: dùng lockedPriceTable khi đã vào Bước 3 (giá đóng băng); dùng priceTable live khi chưa lock ──
const priceOf = (seat) => {
  if (!seat) return 0
  const cap = seatCapacity(seat)
  const types = (seat.ticketTypes && seat.ticketTypes.length > 0)
    ? seat.ticketTypes
    : [seat.ticketType || 'ADULT']
  
  // Ưu tiên lockedPriceTable (snapshot tại Bước 3) để giá vé không thay đổi khi Admin cập nhật giữa phiên
  const activePriceTable = lockedPriceTable.value ?? priceTable.value
  const byType = activePriceTable[seat.seatType]
  let total = 0
  for (let i = 0; i < cap; i++) {
    const t = types[i] || types[0] || 'ADULT'
    const p = byType ? byType[t] : null
    total += Number(p != null ? p : (seat.price || 0))
  }
  return total
}

const fmt = (n) => Number(n || 0).toLocaleString('vi-VN')

// ===== Loại vé theo SỐ LƯỢNG (counter) thay cho dropdown từng ghế =====
// Nguồn sự thật hiển thị là số lượng theo đối tượng; khi khớp đủ số vé yêu cầu (capacity) sẽ gán
// ticketTypes cho từng ghế (theo thứ tự) để giữ nguyên cách tính giá + payload.
const totalRequiredTickets = computed(() =>
  selectedSeats.value.reduce((sum, s) => sum + seatCapacity(s), 0)
)
const ticketCounts = ref({}) // mã đối tượng -> số vé
const totalTicketCount = computed(() =>
  Object.values(ticketCounts.value).reduce((a, b) => a + (Number(b) || 0), 0))
const ticketsMatchSeats = computed(() =>
  selectedSeats.value.length > 0 && totalTicketCount.value === totalRequiredTickets.value)

// Dựng lại counts: mặc định toàn bộ vé chuyển thành ADULT khi vào bước xác nhận (Sweetbox = 2 vé)
const syncTicketCountsFromSeats = () => {
  const counts = {}
  Object.keys(audienceLabels.value).forEach(k => { counts[k] = 0 })
  counts.ADULT = totalRequiredTickets.value
  ticketCounts.value = counts
  assignTicketCountsToSeats()
}

// Gán loại vé cho từng ghế theo counts (thứ tự ghế, Sweetbox nhận đủ 2 vé) → priceOf/seatTypeBreakdown tự cập nhật
const assignTicketCountsToSeats = () => {
  const order = []
  for (const [code, qty] of Object.entries(ticketCounts.value)) {
    for (let i = 0; i < (Number(qty) || 0); i++) order.push(code)
  }
  let orderIdx = 0
  selectedSeats.value.forEach((s) => {
    const cap = seatCapacity(s)
    const types = []
    for (let j = 0; j < cap; j++) {
      types.push(order[orderIdx] || 'ADULT')
      orderIdx++
    }
    s.ticketTypes = types
    s.ticketType = types[0] || 'ADULT'
  })
}

const setTicketCount = (code, delta) => {
  const maxTickets = totalRequiredTickets.value
  const cur = Number(ticketCounts.value[code] || 0)

  if (delta < 0) {
    if (cur <= 0 || cur === maxTickets) return
    // Trả vé về cho ADULT (hoặc loại khác) để tổng số vé luôn bảo toàn bằng totalRequiredTickets
    const next = Math.max(0, cur - 1)
    if (code !== 'ADULT') {
      ticketCounts.value = {
        ...ticketCounts.value,
        [code]: next,
        ADULT: Number(ticketCounts.value.ADULT || 0) + 1
      }
    } else {
      ticketCounts.value = { ...ticketCounts.value, [code]: next }
    }
  } else if (delta > 0) {
    const totalAssigned = totalTicketCount.value

    if (totalAssigned < maxTickets) {
      ticketCounts.value = { ...ticketCounts.value, [code]: cur + delta }
    } else if (totalAssigned === maxTickets && cur < maxTickets) {
      // 1-click transfer: bớt 1 vé từ loại khác (ưu tiên ADULT) sang loại được bấm
      let sourceType = 'ADULT'
      if (sourceType === code || !ticketCounts.value[sourceType]) {
        sourceType = Object.keys(ticketCounts.value).find(k => k !== code && ticketCounts.value[k] > 0)
      }
      
      if (sourceType) {
        ticketCounts.value = {
          ...ticketCounts.value,
          [sourceType]: ticketCounts.value[sourceType] - 1,
          [code]: cur + 1
        }
      }
    }
  }
  
  assignTicketCountsToSeats()
}

// Vào bước 3 (xác nhận vé) → đồng bộ counter vé
watch(currentStep, (step) => {
  if (step === 3) {
    syncTicketCountsFromSeats()
  }
  if (step === 4) {
    // ── Snapshot giá catalog F&B khi Thu ngân vào bước chọn combo ──
    // Đóng băng giá hiển thị trên card catalog trong suốt phiên.
    if (!lockedCombosPrices.value) {
      const priceMap = {}
      combos.value.forEach(c => { priceMap[c.id] = Number(c.price) })
      lockedCombosPrices.value = priceMap
    }
  }
  if (step === 5) {
    if (member.value) {
      fetchPosVoucherEvals()
    }
  }
})

// ===== Định dạng & validate suất chiếu =====
const isPastShowtime = (st) => !!(st?.startTime) && new Date(st.startTime).getTime() < (nowTs.value - lateBookingMinutes.value * 60 * 1000)
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
  if (n.includes('SUPERPLEX') || n.includes('IMAX')) return 'text-cyan-300 border-cyan-400/40 bg-cyan-500/10'
  if (n.includes('COMFORT') || n.includes('GOLD')) return 'text-amber-300 border-amber-400/40 bg-amber-500/10'
  return 'text-on-surface border-outline-variant/25 bg-surface-container-highest'
}

// ===== Giữ ghế tạm thời (Seat Holding Timer) + đồng bộ trạng thái ghế real-time =====
const seatHoldMinutes = ref(10)   // cấu hình thời gian giữ ghế khi chọn (SEAT_HOLD_MINUTES)
const posOrderHoldMinutes = ref(15) // cấu hình thời gian lưu đơn chờ POS (POS_ORDER_HOLD_MINUTES)
const holdRemaining = ref(0)      // giây còn lại
let holdTimer = null
const holdActive = computed(() => holdRemaining.value > 0 && selectedSeats.value.length > 0)
const holdMmSs = computed(() => {
  const s = Math.max(0, holdRemaining.value)
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
})
const holdUrgent = computed(() => holdRemaining.value > 0 && holdRemaining.value <= 60)

const startHoldTimer = () => {
  holdRemaining.value = (seatHoldMinutes.value || 10) * 60
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
}

// Cập nhật trạng thái ghế trực tiếp từ WebSocket event (không cần gọi lại API)
const applySeatStatusUpdate = (seatIds, newStatus) => {
  if (!seatData.value?.seats) return
  const idSet = new Set(seatIds)
  seatData.value = {
    ...seatData.value,
    seats: seatData.value.seats.map(s =>
      idSet.has(s.seatId) ? { ...s, status: newStatus } : s
    )
  }
}

const reloadPosCombos = async () => {
  try {
    const cbRes = await ticketingApi.getCombos()
    const freshCombos = unwrapData(cbRes)
    // ── Snapshot guard: nếu đang có lock, chỉ cập nhật trạng thái/metadata ──
    // KHÔNG ghi đè `price` để giữ giá đã snapshot hiển thị trên card catalog.
    // onFnbUpdate WebSocket sẽ thấy giá mới vào phiên kế tiếp, không ảnh hưởng phiên hiện tại.
    if (lockedCombosPrices.value) {
      combos.value = freshCombos.map(c => ({
        ...c,
        price: lockedCombosPrices.value[c.id] != null
          ? lockedCombosPrices.value[c.id]
          : Number(c.price)
      }))
    } else {
      combos.value = freshCombos
    }
  } catch (err) {
    console.error('Không tải được thực đơn F&B:', err)
  }
}

const reconcilePosCombos = () => {
  if (!selectedCombos.value || selectedCombos.value.length === 0) return []
  const availableMap = new Map((combos.value || []).map(f => [f.id, f]))
  const validCombos = []
  const removedNames = []

  for (const c of selectedCombos.value) {
    const currentItem = availableMap.get(c.id)
    // Snapshot F&B: Chỉ gỡ món bị xoá cứng khỏi hệ thống (isDeleted = true).
    // Món bị Admin ẩn (isActive = false) sau khi Thu ngân đã chọn vẫn được giữ nguyên trong đơn.
    // Quy tắc: snapshot đóng băng trạng thái tại thời điểm bấm chọn — phiên kế tiếp mới áp dụng thay đổi.
    if (!currentItem || currentItem.isDeleted === true) {
      removedNames.push(c.name || 'Món')
    } else {
      // ── Snapshot guard: chỉ cập nhật metadata hiển thị (tên), KHÔNG ghi đè giá đã snapshot ──
      // Thiết kế chuẩn CGV/Lotte Cinema: giá bị đóng băng tại thời điểm Thu ngân bấm chọn.
      // Admin cập nhật giá trong khi Thu ngân đang bán → giá khách đã thỏa thuận KHÔNG thay đổi.
      c.name = currentItem.name
      // KHÔNG ghi đè c.price — snapshotPrice là nguồn sự thật duy nhất cho tính tiền.
      // Backfill snapshotPrice cho combo cũ (restore từ posStore chưa có field này)
      if (c.snapshotPrice == null) c.snapshotPrice = c.price

      // Đối soát lại vị con đã chọn (nếu vị bị xóa khỏi kho)
      if (c.options && c.options.length > 0 && currentItem.slots) {
        const validOptions = []
        let newSurcharge = 0
        for (const opt of c.options) {
          const slot = currentItem.slots.find(s => s.id === opt.slotId || s.slotLabel === opt.slotLabel)
          const optItem = slot?.optionGroup?.items?.find(it => it.id === opt.optionItemId)
          if (optItem) {
            opt.surchargePrice = Number(optItem.surchargePrice || 0)
            validOptions.push(opt)
            newSurcharge += opt.surchargePrice
          }
        }
        c.options = validOptions
        c.surchargePrice = newSurcharge
      }
      validCombos.push(c)
    }
  }

  selectedCombos.value = validCombos
  return removedNames
}

const startSeatPolling = () => {
  seatRealtime.connect(selectedShowtime.value?.id || null) // kết nối WebSocket
}
const stopSeatPolling = () => {
  seatRealtime.connect(null) // nhả khóa suất cũ nhưng vẫn giữ kết nối nhận sự kiện F&B
}

import { usePosStore } from '@/stores/usePosStore'
const posStore = usePosStore()

// ===== Khóa ghế real-time & cập nhật F&B (WebSocket/STOMP) — đồng bộ với quầy POS khác & khách online =====
const seatRealtime = useSeatRealtime({
  by: 'Quầy POS',
  isMySeat: (seatId) => selectedSeats.value.some(s => Number(s.seatId) === Number(seatId)),
  // Ghế mình vừa chọn nhưng quầy khác đã giành trước → gỡ khỏi đơn + báo lỗi
  onDenied: (seatId) => {
    const numId = Number(seatId)
    const lost = selectedSeats.value.find(s => Number(s.seatId) === numId)
    selectedSeats.value = selectedSeats.value.filter(s => Number(s.seatId) !== numId)
    if (selectedSeats.value.length === 0) stopHoldTimer()
    const label = lost ? seatLabel(lost) : 'này'
    showToast(`Ghế ${label} vừa được chọn hoặc đã được bán ở quầy khác. Vui lòng chọn vị trí ghế khác!`, 'error')
  },
  // Ghế bị bán ở nơi khác → đánh dấu SOLD trực tiếp + gỡ khỏi đơn nếu đang chọn
  onSold: (seatIds) => {
    const numIds = seatIds.map(Number)
    applySeatStatusUpdate(numIds, 'SOLD')
    if (isPaying.value || currentStep.value === 6) return
    const lost = selectedSeats.value.filter(s => numIds.includes(Number(s.seatId)))
    if (lost.length) {
      selectedSeats.value = selectedSeats.value.filter(s => !numIds.includes(Number(s.seatId)))
      if (selectedSeats.value.length === 0) stopHoldTimer()
      showToast(`Ghế ${lost.map(s => seatLabel(s)).join(', ')} vừa được bán ở quầy khác — đã gỡ khỏi đơn.`, 'error')
    }
  },
  // Ghế vừa được nhả (hết hạn giữ chỗ / huỷ đơn) → cập nhật AVAILABLE ngay lập tức
  onReleased: (seatIds) => {
    applySeatStatusUpdate(seatIds, 'AVAILABLE')
  },
  // Ghế vừa bị giữ bởi quầy/khách khác → cập nhật HOLD ngay lập tức
  onHeld: (seatIds) => {
    applySeatStatusUpdate(seatIds, 'HOLD')
  },
  // Ghế chuyển sang bảo trì hoặc mở lại
  onMaintenance: (seatIds, status) => {
    const isMaint = status === 'MAINTENANCE' || status === 'LOCKED'
    const newStatus = isMaint ? status : 'AVAILABLE'
    applySeatStatusUpdate(seatIds, newStatus)
    if (isMaint) {
      const lost = selectedSeats.value.filter(s => seatIds.includes(s.seatId))
      if (lost.length) {
        selectedSeats.value = selectedSeats.value.filter(s => !seatIds.includes(s.seatId))
        if (selectedSeats.value.length === 0) stopHoldTimer()
        showToast(`Ghế ${lost.map(s => seatLabel(s)).join(', ')} vừa được chuyển sang chế độ bảo trì — đã gỡ khỏi đơn.`, 'error')
      }
    }
  },
  // Suất chiếu bị hủy khẩn cấp
  onShowtimeCancelled: () => {
    showToast('Suất chiếu này vừa bị hủy hoặc thay đổi lịch.', 'error')
    resetPOS()
  },
})
const isSeatLockedByOthers = (seat) => !!seat && seatRealtime.isLockedByOthers(seat.seatId)

// ===== Hoá đơn chờ (Hold Order / Pending List) — dùng Pinia Store =====
const showHeldPanel = ref(false)
const canHoldOrder = computed(() => {
  if (currentStep.value === 6 || fnbStep.value === 3) return false
  if (saleMode.value === 'FNB') return selectedCombos.value.length > 0
  return !!selectedShowtime.value && selectedSeats.value.length > 0
})
const isHolding = ref(false)

const holdCurrentOrder = async () => {
  if (isHolding.value || isPaying.value) return
  if (!canHoldOrder.value) { showToast('Chưa có gì để giữ đơn (giỏ hàng đang trống).', 'error'); return }

  let bookingId = null
  let bookingCode = null
  let expiresAt = null

  if (saleMode.value === 'TICKET') {
    isHolding.value = true
    try {
      const { data } = await posPendingOrderApi.hold({
        posTerminalId: posStore.getPosTerminalId(),
        showtimeId: selectedShowtime.value.id,
        seatIds: selectedSeats.value.map(s => s.seatId),
        seatSelections: buildSeatSelections(),
        customerId: member.value ? member.value.customerId : null,
        fnbs: selectedCombos.value.map(c => ({
          fnbItemId: c.id,
          quantity: c.quantity,
          clientPrice: c.snapshotPrice ?? c.price, // ── Gửi giá đã lock để Backend verify ──
          options: (c.options || []).map(o => ({
            slotId: o.slotId,
            optionGroupId: o.optionGroupId,
            optionItemId: o.optionItemId,
            clientSurcharge: o.snapshotSurcharge ?? o.surchargePrice ?? 0
          }))
        }))
      })
      // Interceptor axios đã bóc envelope ApiResponse → `data` chính là { bookingId, bookingCode, expiresAt }.
      // Giữ `?? data` để an toàn nếu envelope không bị bóc (endpoint trả payload thô).
      const held = data?.data ?? data
      bookingId = held.bookingId
      bookingCode = held.bookingCode
      expiresAt = new Date(held.expiresAt).getTime()
    } catch (err) {
      showToast(friendlyError(err, 'Không giữ được đơn (vượt quá 3 đơn, ghế bị phạt, hoặc vừa bị đặt).'), 'error')
      isHolding.value = false
      return
    } finally {
      isHolding.value = false
    }
  }

  const orderData = {
    mode: saleMode.value,
    bookingId,
    code: bookingCode,
    expiresAt,
    holdMinutes: posOrderHoldMinutes.value,
    step: saleMode.value === 'FNB' ? fnbStep.value : currentStep.value,
    showtime: selectedShowtime.value ? JSON.parse(JSON.stringify(selectedShowtime.value)) : null,
    seats: JSON.parse(JSON.stringify(selectedSeats.value)),
    combos: JSON.parse(JSON.stringify(selectedCombos.value)),
    member: member.value ? JSON.parse(JSON.stringify(member.value)) : null,
    total: totalPrice.value,
  }

  const code = await posStore.holdOrder(orderData)
  if (code) {
    showCashModal.value = false
    showQrModal.value = false
    cashGiven.value = 0
    showToast(saleMode.value === 'TICKET'
      ? `Đã giữ đơn ${code}. Ghế đã được khoá trên hệ thống.`
      : `Đã giữ đơn F&B. Gọi lại bất cứ lúc nào ở "Đơn chờ".`, 'success')
    softReset()
  }
}

const performRestore = async (o) => {
  try {
    const { data: availableFnbsResponse } = await ticketingApi.getCombos().catch(() => ({ data: { data: [] } }));
    const availableFnbs = availableFnbsResponse?.data || [];
    if (availableFnbs.length > 0) {
      const availableMap = new Map(availableFnbs.map(f => [f.id, f]));
      const validCombos = [];
      const lostCombos = [];
      for (const combo of (o.combos || [])) {
        const item = availableMap.get(combo.id || combo.fnbItemId);
        // Snapshot F&B: Chỉ gỡ món bị xóa cứng (isDeleted=true) khi restore đơn chờ.
        // Món bị ẩn (isActive=false) vẫn giữ nguyên theo snapshot đơn đã giữ.
        if (item && item.isDeleted !== true) {
          // Backfill snapshotPrice nếu đơn cũ chưa có
          if (combo.snapshotPrice == null) combo.snapshotPrice = combo.price;
          validCombos.push(combo);
        } else if (!item) {
          lostCombos.push(combo.name);
        } else {
          // item tồn tại nhưng isDeleted=true
          lostCombos.push(combo.name);
        }
      }
      if (lostCombos.length > 0) {
        showToast(`Món ${lostCombos.join(', ')} đã bị xóa khỏi hệ thống và không thể khôi phục vào đơn.`, 'error');
      }
      o.combos = validCombos;
    }
  } catch (_) {}

  if (o.mode === 'FNB') {
    saleMode.value = 'FNB'
    selectedShowtime.value = null
    selectedSeats.value = []
    selectedCombos.value = o.combos || []
    member.value = o.member || null
    concessionSale.value = null
    fnbStep.value = o.step || 1
    showHeldPanel.value = false
    posStore.removeOrder(o.code)
    return
  }
  
  if (isPastShowtime(o.showtime)) {
    if (o.bookingId) { try { await posPendingOrderApi.cancel(o.bookingId, posStore.getPosTerminalId()) } catch (_) {} }
    posStore.removeOrder(o.code)
    showToast(`Suất chiếu của đơn ${o.code} đã quá giờ — đã huỷ đơn chờ và nhả ghế.`, 'error')
    return
  }
  
  if (o.bookingId) {
    try {
      const { data } = await posPendingOrderApi.resume(o.bookingId, posStore.getPosTerminalId())
    } catch (err) {
      posStore.removeOrder(o.code)
      showHeldPanel.value = false
      showToast(`Không thể khôi phục đơn ${o.code}: ${err.response?.data?.error || err.message}`, 'error')
      return
    }
  }
  
  saleMode.value = 'TICKET'
  selectedShowtime.value = o.showtime
  selectedCombos.value = o.combos || []
  // ── Backfill snapshotPrice & snapshotSurcharge cho đơn chờ cũ ──
  selectedCombos.value = selectedCombos.value.map(c => ({
    ...c,
    snapshotPrice: c.snapshotPrice ?? c.price,
    options: (c.options || []).map(opt => ({
      ...opt,
      surchargePrice: Number(opt.surchargePrice) || 0,
      snapshotSurcharge: Number(opt.snapshotSurcharge ?? opt.surchargePrice) || 0
    }))
  }))
  member.value = o.member || null
  restoredBookingId.value = o.bookingId || null
  showHeldPanel.value = false
  currentStep.value = 2
  isLoadingSeats.value = true
  
  try {
    const { data } = await ticketingApi.getSeats(o.showtime.id)
    seatData.value = data.seats ? data : { matrixRow: 9, matrixCol: 10, seats: Array.isArray(data) ? data : [] }
    captureSeatMeta(data)
    // ── Snapshot bảng giá vé khi khôi phục đơn chờ ──
    if (priceTable.value && Object.keys(priceTable.value).length > 0) {
      lockedPriceTable.value = JSON.parse(JSON.stringify(priceTable.value))
    }
    
    const byId = new Map(seatData.value.seats.map(s => [s.seatId, s]))
    const restored = []; const lost = []
    for (const s of (o.seats || [])) {
      const cur = byId.get(s.seatId)
      // Check if it's still HOLD by us, or AVAILABLE
      if (cur && (cur.status === 'AVAILABLE' || cur.status === 'HOLD')) {
        cur.ticketType = s.ticketType || 'ADULT'
        cur.ticketTypes = s.ticketTypes || Array.from({ length: seatCapacity(cur) }, () => s.ticketType || 'ADULT')
        restored.push(cur)
      } else lost.push(s)
    }
    selectedSeats.value = restored
    if (lost.length) {
      const labels = lost.map(s => seatLabel(s)).join(', ')
      showToast(`Ghế ${labels} đã được bán cho khách hàng khác, vui lòng chọn lại ghế mới.`, 'error')
    }
    if (restored.length) startHoldTimer()
    startSeatPolling()
    currentStep.value = lost.length ? 2 : (o.step || 2)
  } catch (_) {
    showToast('Không tải được sơ đồ ghế của đơn chờ.', 'error')
  } finally {
    isLoadingSeats.value = false
  }
  posStore.removeOrder(o.code)
}

const confirmRestoreHold = ref(null)
const askRestoreHeldOrder = (o) => {
  if (canHoldOrder.value || selectedCombos.value.length > 0 || (saleMode.value === 'TICKET' && selectedShowtime.value)) {
    confirmRestoreHold.value = o
  } else {
    performRestore(o)
  }
}
const cancelRestoreHeldOrder = () => { confirmRestoreHold.value = null }
const confirmRestoreAction = () => {
  if (confirmRestoreHold.value) {
    performRestore(confirmRestoreHold.value)
    confirmRestoreHold.value = null
  }
}

const deleteHeldOrder = (o) => posStore.deleteHeldOrder(o)

const heldAgeLabel = (createdAt) => {
  if (!createdAt) return ''
  const diffSec = Math.max(0, Math.floor((Date.now() - createdAt) / 1000))
  if (diffSec < 60) return 'Vừa xong'
  const mins = Math.floor(diffSec / 60)
  return `${mins} phút trước`
}

const heldRemainingSec = (o) => {
  if (!o) return null
  if (o.holdRemaining != null && o.holdRemaining > 0) return o.holdRemaining
  if (o.expiresAt) {
    const diff = Math.floor((new Date(o.expiresAt).getTime() - Date.now()) / 1000)
    return diff > 0 ? diff : 0
  }
  return null
}

const heldCountdown = (o) => {
  const sec = heldRemainingSec(o)
  if (sec == null) return ''
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const confirmDeleteHold = ref(null)
const askDeleteHeldOrder = (o) => { confirmDeleteHold.value = o }
const cancelDeleteHeldOrder = () => { confirmDeleteHold.value = null }
const confirmDeleteHeldOrder = () => {
  if (confirmDeleteHold.value) {
    deleteHeldOrder(confirmDeleteHold.value)
    confirmDeleteHold.value = null
  }
}

// 1. TRÍCH XUẤT NGUỒN DỮ LIỆU NGÀY CÓ LỊCH
// Helper 1: Chuyển chuỗi bất kỳ sang Date Object chuẩn
const parseToDate = (st) => {
  if (!st) return null;
  const rawStr = st.startTime || st.start_time || st.showTime || st.start;
  if (!rawStr) return null;
  if (rawStr instanceof Date) return rawStr;
  
  // Xử lý thay khoảng trắng bằng 'T' để chuẩn hóa ISO (VD: "2026-08-02 09:00:00" -> "2026-08-02T09:00:00")
  const isoStr = String(rawStr).replace(' ', 'T');
  const d = new Date(isoStr);
  return isNaN(d.getTime()) ? null : d;
};

// Helper 2: Lấy chuỗi YYYY-MM-DD local
const getStYmd = (st) => {
  const d = parseToDate(st);
  if (!d) return '';
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
};

// 1. TRÍCH XUẤT NGUỒN DỮ LIỆU NGÀY CÓ LỊCH
const availableDates = computed(() => {
  if (!showtimes.value.length) return []
  
  const dates = new Set()
  const nowTsVal = nowTs.value
  const todayYmd = getTodayYmd()
  const lateMs = lateBookingMinutes.value * 60 * 1000

  showtimes.value.forEach(st => {
    const d = parseToDate(st)
    if (!d) return
    const ymd = getStYmd(st)
    
    // Bỏ qua các suất trong quá khứ đối với ngày hôm nay (tôn trọng LATE_BOOKING_MINUTES)
    if (ymd === todayYmd) {
      if (d.getTime() >= (nowTsVal - lateMs)) dates.add(ymd)
    } else if (ymd > todayYmd) {
      dates.add(ymd)
    }
  })
  
  // Trả về mảng đã sắp xếp tăng dần
  return Array.from(dates).sort()
})

// LOGIC STATE AN TOÀN
watch(availableDates, (newDates) => {
  if (newDates.length > 0 && !newDates.includes(selectedPosDate.value)) {
    selectedPosDate.value = newDates[0]
    selectedShowtime.value = null
  }
})

// 2. REFACTOR GIAO DIỆN TABS & DROPDOWN/CARD CHỌN NGÀY
const quickDateTabs = computed(() => {
  return availableDates.value.slice(0, 3).map(ymd => {
    const [y, m, dNum] = ymd.split('-')
    const d = new Date(y, m - 1, dNum)
    const isToday = ymd === getTodayYmd()
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    const tomorrowYmd = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth() + 1).padStart(2, '0')}-${String(tomorrow.getDate()).padStart(2, '0')}`
    const isTomorrow = ymd === tomorrowYmd
    
    let label = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`
    if (isToday) label = `Hôm nay (${label})`
    else if (isTomorrow) label = `Ngày mai (${label})`
    else {
      const days = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
      label = `${days[d.getDay()]} (${label})`
    }
    return { value: ymd, label }
  })
})

const otherDateOptions = computed(() => {
  return availableDates.value.slice(3).map(ymd => {
    const [y, m, dNum] = ymd.split('-')
    const d = new Date(y, m - 1, dNum)
    const days = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7']
    const label = `${days[d.getDay()]}, ${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`
    return { value: ymd, label }
  })
})

const showOtherDatesDropdown = ref(false)
const toggleOtherDates = () => { showOtherDatesDropdown.value = !showOtherDatesDropdown.value }

const selectPosDate = (val) => {
  selectedPosDate.value = val
  selectedShowtime.value = null
  showOtherDatesDropdown.value = false
}

// Lọc suất chiếu theo tab ngày và cấu hình Bán vé trễ (chỉ áp dụng cho ngày hôm nay)
const visibleShowtimes = computed(() => {
  if (!showtimes.value || !showtimes.value.length) return [];
  const nowTsVal = nowTs.value;
  const lateMs = lateBookingMinutes.value * 60 * 1000;
  const cutoffTime = nowTsVal - lateMs;
  const todayYmd = getTodayYmd();
  
  const result = showtimes.value.filter(st => {
    // 1. Kiểm tra ngày trùng với selectedPosDate
    const stYmd = getStYmd(st);
    if (stYmd !== selectedPosDate.value) return false;
    
    // 2. Logic kiểm tra giờ trễ cho TAB HÔM NAY
    const isTodayTab = selectedPosDate.value === todayYmd;
    if (isTodayTab) {
      const d = parseToDate(st);
      if (!d) return false;
      return d.getTime() >= cutoffTime; // Giữ lại suất chiếu chưa quá hạn trễ (10 phút)
    }
    
    return true; // Các ngày tương lai: Giữ lại toàn bộ
  });
  
  return result;
})

const groupedMoviesWithShowtimes = computed(() => {
  if (!visibleShowtimes.value || !visibleShowtimes.value.length) return [];
  
  const moviesMap = new Map();

  visibleShowtimes.value.forEach(st => {
    // 1. TÊN PHIM & ID
    const movieTitle = typeof st.movie === 'string' ? st.movie : (st.movie?.title || st.movieTitle || 'Phim chiếu');
    const mId = st.movieId || movieTitle;

    if (!moviesMap.has(mId)) {
      // 2. POSTER PHIM
      const posterSrc = st.moviePoster || st.posterUrl || st.moviePosterBase64 || '/images/Hopper.webp';
      
      // 3. THỜI LƯỢNG
      const durationVal = st.duration || st.durationMins || st.movieDuration || '120';
      
      moviesMap.set(mId, {
        movie: {
          id: mId,
          title: movieTitle,
          titleVietnamese: st.movieTitleVietnamese || st.titleVietnamese,
          posterUrl: posterSrc,
          posterBase64: posterSrc,
          durationMins: durationVal,
          ageRating: st.movieAgeRating || st.ageRating
        },
        roomGroupsMap: new Map()
      });
    }
    const movieData = moviesMap.get(mId);

    // 4. TÊN PHÒNG CHIẾU
    let rawRoom = st.roomName || st.room?.name || '223';
    if (!rawRoom.toUpperCase().includes('PHÒNG') && !isNaN(parseInt(rawRoom.charAt(0)))) {
      rawRoom = `PHÒNG ${rawRoom}`;
    }
    
    // 5. ĐỊNH DẠNG
    const formatName = st.formatName || st.format?.name || '2D PHỤ ĐỀ';
    const groupLabel = `${formatName} • ${rawRoom}`.toUpperCase();

    if (!movieData.roomGroupsMap.has(groupLabel)) {
      movieData.roomGroupsMap.set(groupLabel, {
        groupLabel,
        showtimes: []
      });
    }
    
    // Push suất chiếu
    movieData.roomGroupsMap.get(groupLabel).showtimes.push(st);
  });

  // Convert Map to Array
  return Array.from(moviesMap.values()).map(mData => ({
    movie: mData.movie,
    roomGroups: Array.from(mData.roomGroupsMap.values())
  }));
});

const getPoster = (movie) => {
  return movie?.posterBase64 || movie?.posterUrl || movie?.poster_base64 || '/images/Hopper.webp'
}


// Dọn khu làm việc về bước 1 mà KHÔNG tải lại danh sách suất/combo (dùng sau khi giữ đơn)
const softReset = () => {
  stopHoldTimer(); stopSeatPolling()
  currentStep.value = 1
  selectedShowtime.value = null
  seatData.value = { matrixRow: 9, matrixCol: 10, seats: [] }
  selectedSeats.value = []
  selectedCombos.value = []
  allowOrphan.value = false
  member.value = null
  restoredBookingId.value = null
  cardNumberInput.value = ''
  cardError.value = ''
  concessionSale.value = null
  fnbStep.value = 1
  showCashModal.value = false
  showQrModal.value = false
  cashGiven.value = 0
  lockedPriceTable.value = null // ── Nhả lock snapshot giá vé khi bắt đầu phiên mới ──
  lockedCombosPrices.value = null // ── Nhả lock snapshot giá catalog F&B ──
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
    showToast(friendlyError(e, 'Gửi yêu cầu hủy thất bại.'), 'error')
  } finally {
    isRequestingVoid.value = false
  }
}

const switchMode = async (mode) => {
  if (saleMode.value === mode) return
  saleMode.value = mode
  // Đổi luồng → dọn sạch khu làm việc để tránh lẫn dữ liệu giữa 2 kiểu bán
  stopHoldTimer(); stopSeatPolling()
  selectedShowtime.value = null
  seatData.value = { matrixRow: 9, matrixCol: 10, seats: [] }
  selectedSeats.value = []
  selectedCombos.value = []
  member.value = null
  restoredBookingId.value = null
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
  showMobileReceiptDrawer.value = false
  cashGiven.value = 0
  lockedPriceTable.value = null  // nhả lock giá vé khi chuyển chế độ
  lockedCombosPrices.value = null // nhả lock giá catalog trước, rồi set lại theo mode mới
  // Khi vào chế độ Bán nhanh F&B: nạp F&B mới nhất rồi snapshot giá catalog
  if (mode === 'FNB') {
    await reloadPosCombos()
    const priceMap = {}
    combos.value.forEach(c => { priceMap[c.id] = Number(c.price) })
    lockedCombosPrices.value = priceMap
  }
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
  if (isHolding.value || isPaying.value) return
  if (selectedCombos.value.length === 0) { showToast('Chưa chọn món nào.', 'error'); return }
  paymentMethod.value = method
  isPaying.value = true
  try {
    const payload = {
      fnbs: selectedCombos.value.map(c => ({
        itemId: c.id,
        fnbItemId: c.id,
        quantity: c.quantity,
        clientPrice: c.snapshotPrice ?? c.price, // ── Gửi giá đã lock để Backend verify ──
        options: c.options || []
      })),
      customerId: member.value ? member.value.customerId : null,
      paymentMethod: method,
    }
    const { data } = await ticketingApi.concession(payload)
    concessionSale.value = data
    showCashModal.value = false
    showQrModal.value = false
    fnbStep.value = 3
    printConcessionInvoice()
  } catch (err) {
    // Snapshot F&B: Không reload/reconcile khi thanh toán lỗi để bảo toàn snapshot giá.
    // Lỗi thực sự (món bị xóa cứng khỏi DB) sẽ hiển thị thông báo lỗi rõ ràng.
    if (err.response?.status === 400 && err.response?.data?.message) {
      showToast(err.response.data.message, 'error')
    } else {
      showToast(friendlyError(err, 'Thanh toán thất bại.'), 'error')
    }
  } finally {
    isPaying.value = false
  }
}

// ===== In phiếu nhận bắp nước [PICK-UP] (K80) =====
const printConcessionInvoice = () => {
  if (!concessionSale.value) return
  const inv = {
    saleCode: concessionSale.value?.saleCode,
    bookingCode: concessionSale.value?.saleCode,
    posTerminal: '01',
    cashierName: auth.user?.fullName || 'Nguyễn Quang Huy',
    cinemaName: auth.user?.cinema?.name || 'DEVCINE CINEMA',
    cinemaAddress: auth.user?.cinema?.address || 'Tầng 3, TTTM DevCine Plaza, Hà Nội',
    printedAt: new Date(),
    paymentMethod: paymentMethod.value,
    fnbs: selectedCombos.value.map(c => ({
      name: c.name,
      quantity: c.quantity,
      // ── Snapshot: in hóa đơn theo giá lock tại thời điểm chọn, không phải giá live ──
      price: c.snapshotPrice ?? c.price,
      surchargePrice: c.surchargePrice || 0,
      lineTotal: (Number(c.snapshotPrice ?? c.price) + Number(c.surchargePrice || 0)) * Number(c.quantity || 1),
      options: c.options || []
    })),
    fnbDiscount: 0,
    memberName: member.value?.fullName
  }
  const ok = openInvoice(inv)
  if (!ok) {
    showToast('Trình duyệt đã chặn cửa sổ. Hãy cho phép pop-up để in hoá đơn.', 'error')
  }
}
const newConcessionSale = async () => {
  selectedCombos.value = []
  member.value = null
  cardNumberInput.value = ''
  cardError.value = ''
  concessionSale.value = null
  fnbStep.value = 1
  lockedCombosPrices.value = null
  resetVoidState()
  await reloadPosCombos()
  const priceMap = {}
  combos.value.forEach(c => { priceMap[c.id] = Number(c.price) })
  lockedCombosPrices.value = priceMap
}

const unwrapData = (res) => {
  if (!res || !res.data) return []
  if (Array.isArray(res.data.data)) return res.data.data
  if (Array.isArray(res.data)) return res.data
  return []
}

const fetchData = async () => {
  isLoading.value = true
  error.value = ''
  
  const [stRes, cbRes] = await Promise.allSettled([
    ticketingApi.getShowtimes(),
    ticketingApi.getCombos()
  ])
  
  if (stRes.status === 'fulfilled') {
    showtimes.value = unwrapData(stRes.value)
  } else {
    const err = stRes.reason
    const status = err.response?.status
    if (status === 403) {
      error.value = 'Bạn không có quyền bán vé cho cụm rạp này.'
    } else {
      error.value = 'Không tải được dữ liệu bán vé. Vui lòng thử lại.'
    }
  }
  
  if (cbRes.status === 'fulfilled') {
    combos.value = unwrapData(cbRes.value)
  } else {
    showToast('Tạm thời không tải được danh mục Bắp nước', 'warning')
    combos.value = []
  }

  if (!error.value) {
    console.log("POS Raw Showtimes:", showtimes.value.length, "Available Dates:", availableDates.value)
    console.log("DEBUG - Grouped Movies Value:", groupedMoviesWithShowtimes.value);
    console.log("DEBUG - Sample Raw Showtime Item:", showtimes.value[0]);
  }
  
  isLoading.value = false
}

const selectShowtime = async (st) => {
  if (isPastShowtime(st)) {
    showToast('Suất chiếu đã quá giờ phát sóng — không thể bán vé.', 'error')
    return
  }
  selectedShowtime.value = st
  sessionStartedAt.value = new Date().toISOString()
  selectedSeats.value = []
  selectedCombos.value = []
  stopHoldTimer()
  isLoadingSeats.value = true
  currentStep.value = 2
  lockedCombosPrices.value = null // nhả lock catalog F&B cũ để nạp mới cho phiên này
  try {
    const [seatRes] = await Promise.all([
      ticketingApi.getSeats(st.id),
      reloadPosCombos() // nạp thực đơn F&B & kho tùy chọn mới nhất khi bắt đầu phiên bán vé mới
    ])
    const data = seatRes.data
    seatData.value = data.seats ? data : { matrixRow: 9, matrixCol: 10, seats: Array.isArray(data) ? data : [] }
    captureSeatMeta(data)
    // ── Snapshot bảng giá vé NGAY TỪ ĐẦU PHIÊN (khi nạp sơ đồ ghế) ──
    // Bảng giá này là nguồn sự thật duy nhất tính tiền trong suốt phiên.
    if (priceTable.value && Object.keys(priceTable.value).length > 0) {
      lockedPriceTable.value = JSON.parse(JSON.stringify(priceTable.value))
    }
    startSeatPolling()
  } catch (err) {
    showToast('Không tải được sơ đồ ghế.', 'error')
    seatData.value = { matrixRow: 9, matrixCol: 10, seats: [] }
  } finally {
    isLoadingSeats.value = false
  }
}

// Nội suy tọa độ dùng chung: cellAt = ô bất kỳ (ghế/lối đi); seatAt = CHỈ ghế (lối đi → null).
const { cellAt, seatAt, isAisle } = useSeatGridRender(() => seatData.value.seats)
const isSelected = (seat) => selectedSeats.value.some(s => s.seatId === seat.seatId)
// Nhãn ghế: ưu tiên label lưu ở DB (Admin có thể sửa tay), fallback rowChar+colNum
const seatLabel = (seat) => seat ? (seat.label || (seat.rowChar + seat.colNum)) : ''
const isSeatMaintenance = (seat) => !!seat && (seat.status === 'MAINTENANCE' || seat.status === 'LOCKED' || seat.seatStatus === 'MAINTENANCE' || seat.seatStatus === 'LOCKED')

// Cảnh báo ghế mồ côi real-time — tự tính lại mỗi khi đổi ghế chọn HOẶC có sự kiện khoá STOMP
// (othersLocked reassign → reactive). Rào cản = đã bán/giữ/bảo trì/khoá quầy khác.
const { orphanKeys, hasOrphan } = useOrphanSeatCheck({
  seats: () => seatData.value.seats,
  selectedIds: () => selectedSeats.value.map(s => s.seatId),
  isSeatBlocked: (cell) => cell.status === 'SOLD' || cell.status === 'HOLD'
    || isSeatMaintenance(cell) || isSeatLockedByOthers(cell)
})
const isOrphanSeat = (seat) => !!seat && orphanKeys.value.has(`${seat.gridRow}-${seat.gridCol}`)
// Nhãn hàng (A, B, C...) suy từ ghế đầu tiên có trên hàng đó
const rowLabel = (gridRow) => {
  // Ưu tiên rowChar thật (payload cũ); snapshot không mang rowChar → suy theo vị trí lưới.
  const s = seatData.value.seats.find(x => x.gridRow === gridRow && x.rowChar)
  return s ? s.rowChar : String.fromCharCode(65 + gridRow)
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
      showToast(`Ghế ${seatLabel(seat)} vừa được chọn hoặc đã được bán ở quầy khác. Vui lòng chọn vị trí ghế khác!`, 'error')
      return
    }
    // Chặn sớm khi vượt giới hạn số vé/lần đặt (chống phe vé) — tính theo sức chứa thực tế
    const nextCap = seatCapacity(seat)
    if (totalRequiredTickets.value + nextCap > maxTicketsPerBooking.value) {
      showToast(`Mỗi lần đặt tối đa ${maxTicketsPerBooking.value} vé.`, 'error')
      return
    }
    seat.ticketType = seat.ticketType || 'ADULT'
    seat.ticketTypes = Array.from({ length: nextCap }, () => 'ADULT')
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
  // Ghế khóa vật lý (bảo trì/khóa) → không bán được ở quầy
  if (isSeatMaintenance(seat)) return `${base} bg-surface-container-highest border border-white/10 text-red-500 cursor-not-allowed opacity-60`
  if (seat.status === 'SOLD') return `${base} bg-surface-container-high border-white/5 text-on-surface-variant/20 cursor-not-allowed opacity-40`
  if (seat.status === 'HOLD') return `${base} bg-yellow-500/10 border-yellow-500/30 text-yellow-500/60 cursor-not-allowed`
  // Ghế đang bị quầy khác / khách online giữ real-time → khóa xám, không cho click
  if (isSeatLockedByOthers(seat)) return `${base} bg-yellow-500/10 border-yellow-500/30 text-yellow-500/60 cursor-not-allowed`
  if (isSelected(seat)) return `${base} bg-primary border-primary text-on-primary shadow-lg shadow-primary/30 cursor-pointer scale-105`
  // Ghế trống bị "kẹt" mồ côi do lựa chọn hiện tại → nền đỏ mờ + viền nét đứt trắng (tĩnh,
  // không nhấp nháy; viền trắng để không lẫn với ghế VIP đỏ). Vẫn click được để chọn nốt.
  if (isOrphanSeat(seat)) return `${base} bg-red-500/25 border-2 border-dashed border-white/80 text-white cursor-pointer`
  const byType = {
    VIP: 'bg-red-900/40 border-red-500/40 text-red-200 hover:border-red-400',
    SWEETBOX: 'bg-purple-900/40 border-purple-500/40 text-purple-200 hover:border-purple-400'
  }[seat.seatType] || 'bg-surface-container-high border-outline-variant/10 text-on-surface-variant/60 hover:border-primary/40'
  return `${base} ${byType} cursor-pointer`
}

// F&B — giới hạn 1–99 phần/món tránh gõ nhầm làm sai hoá đơn
const MAX_FNB_QTY = 99
const isFnbModalOpen = ref(false)
const editingFnbItem = ref(null)
const editingFnbIndex = ref(-1)
const initialFnbOptions = ref([])

// Khóa gộp dòng theo BỘ VỊ: cùng combo nhưng khác bộ tùy chọn ⇒ 2 dòng riêng.
const optionsKey = (opts) => (opts || []).map(o => o.optionItemId).sort().join(',')
const isOptionsEqual = (a, b) => optionsKey(a) === optionsKey(b)
// 1 combo (id) có thể đẻ nhiều dòng (nhiều bộ vị) → gom lại để hiển thị trên card menu.
const fnbLinesOf = (cbId) => selectedCombos.value.filter(c => c.id === cbId)
const fnbQtyOf = (cbId) => fnbLinesOf(cbId).reduce((s, c) => s + c.quantity, 0)
// ── Snapshot guard: dùng snapshotPrice để tổng hiển thị trên card menu khớp giá đã lock ──
const fnbLineTotal = (cbId) => fnbLinesOf(cbId).reduce((s, c) => s + ((c.snapshotPrice ?? c.price) + (c.surchargePrice || 0)) * c.quantity, 0)

const openFnbModal = (cb) => {
  editingFnbItem.value = cb
  editingFnbIndex.value = -1
  initialFnbOptions.value = []
  isFnbModalOpen.value = true
}

const editFnbOptions = (item, index) => {
  const originalCombo = combos.value.find(c => c.id === item.id)
  if (!originalCombo) return
  editingFnbItem.value = originalCombo
  editingFnbIndex.value = index
  initialFnbOptions.value = item.options || []
  isFnbModalOpen.value = true
}

const handleFnbOptionsConfirm = ({ options, totalSurcharge }) => {
  const snapOptions = (options || []).map(o => ({
    ...o,
    surchargePrice: Number(o.surchargePrice) || 0,
    snapshotSurcharge: Number(o.snapshotSurcharge ?? o.surchargePrice) || 0
  }))
  if (editingFnbIndex.value > -1) {
    // Đổi vị của ĐÚNG dòng đang chỉnh (ghi đè tại chỗ).
    const item = selectedCombos.value[editingFnbIndex.value]
    if (item) {
      item.options = snapOptions
      item.surchargePrice = totalSurcharge
    }
  } else {
    // Thêm bộ vị mới: trùng bộ vị đã có ⇒ +1 số lượng, khác ⇒ đẻ dòng mới.
    const cb = editingFnbItem.value
    const existingIndex = selectedCombos.value.findIndex(c => c.id === cb.id && isOptionsEqual(c.options, options))
    if (existingIndex > -1) {
      if (selectedCombos.value[existingIndex].quantity >= MAX_FNB_QTY) { showToast(`Tối đa ${MAX_FNB_QTY} phần/món.`, 'error') }
      else selectedCombos.value[existingIndex].quantity++
      // snapshotPrice GIỮ NGUYÊN khi tăng số lượng
    } else {
      // ── Snapshot giá tại thời điểm Thu ngân bấm chọn (Price Lock at Selection) ──
      selectedCombos.value.push({ id: cb.id, name: cb.name, price: Number(cb.price), snapshotPrice: Number(cb.price), quantity: 1, options: snapOptions, surchargePrice: totalSurcharge })
    }
  }
  isFnbModalOpen.value = false
  editingFnbIndex.value = -1
}

const addCombo = (cb) => {
  if (cb.slots && cb.slots.length > 0) {
      openFnbModal(cb)
      return
  }
  const existingIndex = selectedCombos.value.findIndex(c => c.id === cb.id)
  if (existingIndex > -1) {
    if (selectedCombos.value[existingIndex].quantity >= MAX_FNB_QTY) { showToast(`Tối đa ${MAX_FNB_QTY} phần/món.`, 'error'); return }
    selectedCombos.value[existingIndex].quantity++
    // snapshotPrice GIỮ NGUYÊN khi tăng số lượng
  } else {
    // ── Snapshot giá tại thời điểm Thu ngân bấm chọn (Price Lock at Selection) ──
    // Giá đóng băng ngay lúc này, không bị ảnh hưởng nếu Admin cập nhật giá sau đó.
    selectedCombos.value.push({ id: cb.id, name: cb.name, price: Number(cb.price), snapshotPrice: Number(cb.price), quantity: 1, options: [], surchargePrice: 0 })
  }
}

// Click vào Card menu = TẠO MỚI (mở modal chọn vị / thêm món). Nếu combo đã có
// trong giỏ thì bỏ qua — thao tác tăng/giảm/đổi vị dùng nút ngay trên card.
const handleFnbCardClick = (cb) => {
  if (fnbQtyOf(cb.id) > 0) return
  addCombo(cb)
}

const changeComboQty = (item, delta) => {
  const next = item.quantity + delta
  if (next > MAX_FNB_QTY) { showToast(`Tối đa ${MAX_FNB_QTY} phần/món.`, 'error'); return }
  item.quantity = next
  if (item.quantity <= 0) {
    const idx = selectedCombos.value.indexOf(item)
    if (idx > -1) selectedCombos.value.splice(idx, 1)
  }
}

const seatTotal = computed(() => selectedSeats.value.reduce((a, s) => a + priceOf(s), 0))
// ── Snapshot guard: dùng snapshotPrice (giá lock lúc chọn) thay vì c.price (có thể đã bị Admin cập nhật) ──
// → tránh tổng tiền tự thay đổi khi Admin cập nhật giá F&B trong khi Thu ngân đang bán
const comboTotal = computed(() =>
  selectedCombos.value.reduce((a, c) => {
    const base = c.snapshotPrice ?? c.price
    return a + (base + (c.surchargePrice || 0)) * c.quantity
  }, 0)
)
const totalPrice = computed(() => seatTotal.value + comboTotal.value)

// Giảm giá voucher (xem trước phía client; số chính thức do BE tính lại khi thanh toán)
const discountAmount = computed(() => {
  if (!appliedVoucher.value) return 0
  const v = appliedVoucher.value
  const ev = voucherEvals.value[v.id]
  if (ev && ev.discountAmount != null && ev.applicable) {
    return Number(ev.discountAmount)
  }
  const base = totalPrice.value
  let disc = 0
  if (String(v.discountType).toUpperCase() === 'PERCENTAGE') disc = Math.round(base * Number(v.discountValue || 0) / 100)
  else disc = Math.min(Number(v.discountValue || 0), base)
  // Trần giảm tối đa (maxDiscountAmount) — khớp logic backend evaluate()
  const cap = Number(v.maxDiscountAmount || 0)
  if (cap > 0 && disc > cap) disc = cap
  return disc
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
  const seats = selectedSeats.value.map(s => seatLabel(s)).join('')
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
    const list = data?.data || data || []
    if (Array.isArray(list)) {
      list.forEach(s => { map[s.settingKey] = s.settingValue })
    }
    bankInfo.value = {
      code: map.PAYMENT_BANK_CODE || '',
      name: map.PAYMENT_BANK_NAME || '',
      accountNo: map.PAYMENT_ACCOUNT_NO || '',
      accountName: map.PAYMENT_ACCOUNT_NAME || ''
    }
    const mt = parseInt(map.MAX_TICKETS_PER_BOOKING)
    if (!isNaN(mt)) maxTicketsPerBooking.value = Math.min(20, Math.max(1, mt))
    const late = parseInt(map.BOOKING_LATE_MINUTES)
    if (!isNaN(late)) lateBookingMinutes.value = late
    const hold = parseInt(map.SEAT_HOLD_MINUTES)
    if (!isNaN(hold)) seatHoldMinutes.value = Math.min(30, Math.max(3, hold))
    const posHold = parseInt(map.POS_ORDER_HOLD_MINUTES)
    if (!isNaN(posHold)) posOrderHoldMinutes.value = Math.min(60, Math.max(3, posHold))
  } catch (err) {
    // Không chặn POS nếu lỗi — modal QR sẽ báo "chưa cấu hình"
  }
}

const openCashModal = () => {
  if (!checkoutReady()) return
  cashGiven.value = 0
  showCashModal.value = true
}
const openQrModal = async () => {
  if (!checkoutReady()) return
  if (isPaying.value || isHolding.value || qrHoldLoading.value) return
  
  if (saleMode.value === 'FNB') {
    showQrModal.value = true
    return
  }

  qrHoldLoading.value = true
  try {
    const payload = {
      showtimeId: selectedShowtime.value.id,
      seatIds: selectedSeats.value.map(s => s.seatId),
      seatSelections: buildSeatSelections(),
      fnbs: selectedCombos.value.map(c => ({
        fnbItemId: c.id,
        quantity: c.quantity,
        clientPrice: c.snapshotPrice ?? c.price, // ── Gửi giá đã lock để Backend verify ──
        options: (c.options || []).map(o => ({
          slotId: o.slotId,
          optionGroupId: o.optionGroupId,
          optionItemId: o.optionItemId,
          clientSurcharge: o.snapshotSurcharge ?? o.surchargePrice ?? 0
        }))
      })),
      customerId: member.value ? member.value.customerId : null,
      voucherId: appliedVoucher.value ? appliedVoucher.value.id : null,
      paymentMethod: 'TRANSFER',
      heldBookingId: restoredBookingId.value,
      sessionStartedAt: sessionStartedAt.value,
      allowOrphan: canOverrideOrphan.value && allowOrphan.value
    }
    const { data } = await ticketingApi.hold(payload)
    const resData = data.data ?? data
    qrBookingId.value = resData.bookingId
    restoredBookingId.value = resData.bookingId
    showQrModal.value = true
  } catch (err) {
    const errMsg = err.response?.data?.message || err.response?.data?.error || err.message || ''
    showToast(friendlyError(err, errMsg || 'Không thể tạo đơn giữ chỗ.'), 'error')
  } finally {
    qrHoldLoading.value = false
  }
}

const closeQrModal = async () => {
  showQrModal.value = false
  if (qrBookingId.value) {
    const idToRelease = qrBookingId.value
    qrBookingId.value = null
    if (restoredBookingId.value === idToRelease) {
      restoredBookingId.value = null
    }
    try {
      await ticketingApi.releaseHold(idToRelease)
    } catch (_) {}
  }
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
const voucherEvals = ref({})
const isVoucherEvalsReady = ref(false)

const clearVoucherState = () => {
  appliedVoucher.value = null
  voucherCodeInput.value = ''
  ownedVouchers.value = []
  voucherEvals.value = {}
  isVoucherEvalsReady.value = false
  voucherError.value = ''
}

const clearVoucher = () => {
  appliedVoucher.value = null
  voucherCodeInput.value = ''
  voucherError.value = ''
}

// Chấm điều kiện voucher theo giỏ hàng hiện tại qua /vouchers/preview
const fetchPosVoucherEvals = async () => {
  if (!member.value?.customerId || ownedVouchers.value.length === 0) {
    voucherEvals.value = {}
    isVoucherEvalsReady.value = true
    return
  }
  try {
    const activeTable = lockedPriceTable.value ?? priceTable.value
    const seatPrices = selectedSeats.value.flatMap(s => {
      const cap = seatCapacity(s)
      const types = (s.ticketTypes && s.ticketTypes.length > 0) ? s.ticketTypes : [s.ticketType || 'ADULT']
      const byType = activeTable[s.seatType]
      return Array.from({ length: cap }, (_, i) => {
        const t = types[i] || types[0] || 'ADULT'
        const p = byType ? byType[t] : null
        return Number(p != null ? p : (s.price || 0))
      })
    })

    const fnbTotal = selectedCombos.value.reduce((acc, c) => {
      const base = c.snapshotPrice ?? c.price
      return acc + (base + (c.surchargePrice || 0)) * c.quantity
    }, 0)

    const movieId = selectedShowtime.value?.movieId || selectedShowtime.value?.movie?.id || null

    const { data } = await voucherApi.preview({
      customerId: member.value.customerId,
      movieId: movieId,
      seatPrices: seatPrices,
      fnbTotal: fnbTotal
    })

    const resList = Array.isArray(data) ? data : (data?.data ?? [])
    const map = {}
    for (const e of resList) {
      map[e.voucherId] = e
    }
    voucherEvals.value = map

    // Nếu đang chọn voucher mà sau khi preview không còn đủ điều kiện nữa thì gỡ bỏ
    if (appliedVoucher.value && map[appliedVoucher.value.id]) {
      if (!map[appliedVoucher.value.id].applicable) {
        clearVoucher()
        voucherError.value = map[appliedVoucher.value.id].reason || 'Đơn không đủ điều kiện để áp dụng mã này.'
      }
    }
  } catch (e) {
    console.error('Lỗi preview voucher POS:', e)
  } finally {
    isVoucherEvalsReady.value = true
  }
}

const loadOwnedVouchers = async () => {
  if (!member.value) { ownedVouchers.value = []; voucherEvals.value = {}; isVoucherEvalsReady.value = true; return }
  isVoucherEvalsReady.value = false
  try {
    const { data } = await ticketingApi.customerVouchers(member.value.customerId)
    const list = Array.isArray(data) ? data : (data?.data ?? [])
    ownedVouchers.value = list.filter(v => v.status === 'ACTIVE')
    await fetchPosVoucherEvals()
  } catch (_) {
    ownedVouchers.value = []
    voucherEvals.value = {}
    isVoucherEvalsReady.value = true
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
    voucherCodeInput.value = ''
    await loadOwnedVouchers()
    const ev = voucherEvals.value[data.id]
    if (ev && !ev.applicable) {
      appliedVoucher.value = null
      voucherError.value = `Đã lưu mã vào ví! ${ev.reason || 'Đơn chưa đủ điều kiện để áp dụng ngay.'}`
    } else {
      appliedVoucher.value = {
        id: data.id,
        code: data.code,
        discountType: ev?.discountType || data.discountType,
        discountValue: ev?.discountValue != null ? Number(ev.discountValue) : Number(data.discountValue || 0),
        maxDiscountAmount: ev?.maxDiscountAmount != null ? Number(ev.maxDiscountAmount) : Number(data.maxDiscountAmount || 0),
        maxTicketQuantity: ev?.maxTicketQuantity != null ? Number(ev.maxTicketQuantity) : Number(data.maxTicketQuantity || 0)
      }
      showToast(`Đã áp mã ${data.code}.`, 'success')
      voucherError.value = ''
    }
  } catch (e) {
    appliedVoucher.value = null
    voucherError.value = friendlyError(e, 'Mã không hợp lệ hoặc không áp dụng được.')
  } finally {
    isApplyingVoucher.value = false
  }
}

const selectVoucher = (v) => {
  const ev = voucherEvals.value[v.id]
  if (ev && !ev.applicable) {
    voucherError.value = ev.reason || 'Đơn không đủ điều kiện để áp dụng mã này.'
    return
  }
  // Toggle: nếu click lại vào chính voucher đang chọn thì bỏ chọn
  if (appliedVoucher.value?.id === v.id) {
    clearVoucher()
    return
  }
  appliedVoucher.value = {
    id: v.id,
    code: v.code || ev?.code || v.promotion?.code,
    discountType: ev?.discountType || v.discountType || v.promotion?.discountType,
    discountValue: ev?.discountValue != null ? Number(ev.discountValue) : Number(v.discountValue || v.promotion?.discountValue || 0),
    maxDiscountAmount: ev?.maxDiscountAmount != null ? Number(ev.maxDiscountAmount) : Number(v.maxDiscountAmount || v.promotion?.maxDiscountAmount || 0),
    maxTicketQuantity: ev?.maxTicketQuantity != null ? Number(ev.maxTicketQuantity) : Number(v.maxTicketQuantity || v.promotion?.maxTicketQuantity || 0)
  }
  voucherCodeInput.value = v.code || ev?.code || ''
  voucherError.value = ''
  showToast(`Đã áp mã ${appliedVoucher.value.code}.`, 'success')
}

// Phân loại danh sách Voucher thành 2 nhóm: Khả dụng & Chưa đủ điều kiện
const eligibleVouchers = computed(() => {
  if (!isVoucherEvalsReady.value) return []
  return ownedVouchers.value.filter(v => voucherEvals.value[v.id]?.applicable === true)
})

const ineligibleVouchers = computed(() => {
  if (!isVoucherEvalsReady.value) return []
  return ownedVouchers.value.filter(v => {
    const ev = voucherEvals.value[v.id]
    return ev && !ev.applicable
  })
})

const isMissingOrderTotal = (v) => {
  const ev = voucherEvals.value[v.id]
  if (!ev) return false
  const minOrder = Number(ev.minOrderValue || v.promotion?.minOrderValue || 0)
  return minOrder > 0 && totalPrice.value < minOrder && (ev.reason || '').includes('Chưa đạt đơn tối thiểu')
}

const getMissingAmount = (v) => {
  const ev = voucherEvals.value[v.id]
  const minOrder = Number(ev?.minOrderValue || v.promotion?.minOrderValue || 0)
  const diff = Math.max(0, minOrder - totalPrice.value)
  return `${diff.toLocaleString('vi-VN')}đ`
}

const formatVoucherDate = (iso) => {
  if (!iso) return '—'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

// Tạo danh sách gán vé chuẩn hóa gửi backend kèm unitPrice snapshot (ghế Sweetbox sinh đủ 2 phần tử ticketType)
const buildSeatSelections = () => {
  const activeTable = lockedPriceTable.value ?? priceTable.value
  return selectedSeats.value.flatMap(s => {
    const cap = seatCapacity(s)
    const types = (s.ticketTypes && s.ticketTypes.length > 0) ? s.ticketTypes : [s.ticketType || 'ADULT']
    const byType = activeTable[s.seatType]
    return Array.from({ length: cap }, (_, i) => {
      const t = types[i] || types[0] || 'ADULT'
      const p = byType ? byType[t] : null
      const unitPrice = Number(p != null ? p : (s.price || 0))
      return {
        seatId: s.seatId,
        ticketType: t,
        unitPrice: unitPrice
      }
    })
  })
}

const processPayment = async (method) => {
  if (isHolding.value || isPaying.value || qrHoldLoading.value) return
  if (saleMode.value === 'FNB') return processConcessionPayment(method)
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
      seatSelections: buildSeatSelections(),
      fnbs: selectedCombos.value.map(c => ({
        fnbItemId: c.id,
        quantity: c.quantity,
        clientPrice: c.snapshotPrice ?? c.price, // ── Gửi giá đã lock để Backend verify ──
        options: (c.options || []).map(o => ({
          slotId: o.slotId,
          optionGroupId: o.optionGroupId,
          optionItemId: o.optionItemId,
          clientSurcharge: o.snapshotSurcharge ?? o.surchargePrice ?? 0
        }))
      })),
      customerId: member.value ? member.value.customerId : null,
      voucherId: appliedVoucher.value ? appliedVoucher.value.id : null,
      paymentMethod: method,
      heldBookingId: qrBookingId.value || restoredBookingId.value,
      sessionStartedAt: sessionStartedAt.value,
      allowOrphan: canOverrideOrphan.value && allowOrphan.value // chỉ ADMIN/MANAGER mới gửi cờ (BE cũng gate lại theo vai trò)
    }
    
    if (method === 'TRANSFER') {
      let bookingId = qrBookingId.value || restoredBookingId.value
      if (!bookingId) {
        const holdRes = await ticketingApi.hold(payload)
        const resData = holdRes.data?.data ?? holdRes.data
        bookingId = resData.bookingId
      }
      await ticketingApi.mockWebhookSuccess(bookingId)
      const bookingRes = await bookingAdminApi.detail(bookingId)
      completedBooking.value = bookingRes.data.data ?? bookingRes.data
    } else {
      const { data } = await ticketingApi.pay(payload)
      completedBooking.value = data.data ?? data
    }
    
    qrBookingId.value = null
    restoredBookingId.value = null
    stopHoldTimer()
    stopSeatPolling()
    showCashModal.value = false
    showQrModal.value = false
    currentStep.value = 6
    printInvoice()
  } catch (err) {
    // Snapshot F&B: Không reload/reconcile khi thanh toán lỗi để bảo toàn snapshot giá.
    // Lỗi thực sự (món bị xóa cứng khỏi DB) sẽ hiển thị thông báo lỗi rõ ràng.
    if (err.response?.status === 422 && err.response?.data?.message) {
      outOfStockMessage.value = err.response.data.message
      showOutOfStockModal.value = true
    } else if (err.response?.status === 400 && err.response?.data?.message) {
      showToast(err.response.data.message, 'error')
    } else if (err.response?.status === 409 && err.response?.data?.message) {
      showToast(err.response.data.message, 'error')
    } else {
      showToast(friendlyError(err, 'Thanh toán thất bại (ghế có thể đã bán).'), 'error')
    }
  } finally {
    isPaying.value = false
  }
}

// ===== In hoá đơn & vé giấy K80 (Vé xem phim + Phiếu nhận bắp nước nếu có) =====
const printInvoice = () => {
  if (!completedBooking.value) return
  const st = selectedShowtime.value
  const discount = Number(completedBooking.value?.discountAmount || discountAmount.value || 0)
  
  const inv = {
    bookingCode: completedBooking.value?.bookingCode,
    movieTitle: st?.movieTitle,
    format: st?.formatName,
    roomName: st?.roomName,
    roomType: 'Standard',
    startTime: st?.startTime,
    endTime: st?.endTime,
    posTerminal: '01',
    cashierName: auth.user?.fullName || 'Nguyễn Quang Huy',
    cinemaName: auth.user?.cinema?.name || 'DEVCINE CINEMA',
    cinemaAddress: auth.user?.cinema?.address || 'Tầng 3, TTTM DevCine Plaza, Hà Nội',
    printedAt: new Date(),
    paymentMethod: paymentMethod.value,
    seats: selectedSeats.value.flatMap(s => {
      const cap = seatCapacity(s)
      const types = (s.ticketTypes && s.ticketTypes.length > 0) ? s.ticketTypes : [s.ticketType || 'ADULT']
      // ── Snapshot: in hóa đơn theo bảng giá đã lock tại Bước 3, không phải giá live ──
      const activeTable = lockedPriceTable.value ?? priceTable.value
      const byType = activeTable[s.seatType]
      return Array.from({ length: cap }, (_, i) => {
        const t = types[i] || types[0] || 'ADULT'
        const p = byType ? byType[t] : null
        const price = Number(p != null ? p : (s.price || 0))
        return {
          seatLabel: seatLabel(s),
          ticketType: t,
          price: price
        }
      })
    }),
    fnbs: selectedCombos.value.map(c => ({
      name: c.name,
      quantity: c.quantity,
      // ── Snapshot: in hóa đơn theo giá lock tại thời điểm chọn, không phải giá live ──
      price: c.snapshotPrice ?? c.price,
      surchargePrice: c.surchargePrice || 0,
      lineTotal: (Number(c.snapshotPrice ?? c.price) + Number(c.surchargePrice || 0)) * Number(c.quantity || 1),
      options: c.options || []
    })),
    ticketDiscount: discount,
    fnbDiscount: 0,
    memberName: member.value?.fullName
  }
  const ok = openInvoice(inv)
  if (!ok) {
    showToast('Trình duyệt đã chặn cửa sổ. Hãy cho phép pop-up để in hoá đơn.', 'error')
  }
}

// ===== Hiển thị mã QR toàn màn hình (tab mới) cho khách quét — đồng bộ style DevCine =====
const buildQrPageHtml = () => {
  const st = selectedShowtime.value
  const movieTitle = st ? (typeof st.movie === 'string' ? st.movie : (st.movie?.title || st.movieTitle || 'Vé xem phim')) : (saleMode.value === 'FNB' ? 'Bán nhanh bắp nước & Combo' : '')
  const roomName = st?.roomName || ''
  const formatName = st?.formatName || '2D'
  const seatsStr = selectedSeats.value.map(s => seatLabel(s)).join(', ')
  const totalVal = payableTotal.value || comboTotal.value || 0
  const bank = bankInfo.value || {}
  const qr = cleanQrUrl.value || ''

  return `<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>Thanh toán VietQR — DevCine</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:ital,wght@0,400;0,500;0,600;0,700;0,800;1,400;1,700&family=Montserrat:ital,wght@0,700;0,800;0,900;1,700;1,800;1,900&display=swap" rel="stylesheet">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" />
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
    background-color: #0e0e0e;
    background-image: radial-gradient(circle at 50% 25%, rgba(245, 197, 24, 0.09) 0%, transparent 65%);
    color: #e5e2e1;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
  }
  .card {
    width: 100%;
    max-width: 480px;
    background: #181818;
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 28px;
    overflow: hidden;
    box-shadow: 0 25px 60px -15px rgba(0, 0, 0, 0.9), 0 0 35px -10px rgba(245, 197, 24, 0.12);
  }
  .head {
    background: #201f1f;
    padding: 18px 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }
  .brand-group {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .logo-icon {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: #f5c518;
    color: #000;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(245, 197, 24, 0.3);
  }
  .logo-icon .material-symbols-outlined {
    font-size: 22px;
  }
  .brand-text h1 {
    font-family: 'Montserrat', sans-serif;
    font-size: 18px;
    font-weight: 900;
    font-style: italic;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    line-height: 1.1;
    color: #fff;
  }
  .brand-text h1 span { color: #f5c518; }
  .brand-text p {
    font-size: 8px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.18em;
    color: #a8a29e;
    margin-top: 2px;
  }
  .status-pill {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 5px 12px;
    background: rgba(34, 197, 94, 0.12);
    border: 1px solid rgba(34, 197, 94, 0.3);
    border-radius: 20px;
    font-size: 10px;
    font-weight: 800;
    color: #4ade80;
    text-transform: uppercase;
    letter-spacing: 0.08em;
  }
  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #4ade80;
    box-shadow: 0 0 8px #4ade80;
  }
  .body {
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .order-summary {
    background: #242424;
    border: 1px solid rgba(255, 255, 255, 0.07);
    border-radius: 16px;
    padding: 12px 16px;
  }
  .order-movie {
    font-family: 'Montserrat', sans-serif;
    font-size: 14px;
    font-weight: 900;
    font-style: italic;
    text-transform: uppercase;
    color: #fff;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .order-details {
    font-size: 11px;
    font-weight: 600;
    color: #a8a29e;
    margin-top: 3px;
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
  }
  .seat-tag {
    color: #f5c518;
    font-weight: 800;
    background: rgba(245, 197, 24, 0.12);
    padding: 1px 6px;
    border-radius: 6px;
    border: 1px solid rgba(245, 197, 24, 0.25);
  }
  .qr-wrapper {
    position: relative;
    width: min(72vw, 300px);
    aspect-ratio: 1;
    margin: 4px auto;
    background: #fff;
    border-radius: 20px;
    padding: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 10px 25px rgba(0,0,0,0.5);
  }
  .qr-wrapper img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
  .scan-corner {
    position: absolute;
    width: 18px;
    height: 18px;
    border: 3px solid #f5c518;
    pointer-events: none;
  }
  .c-tl { top: -2px; left: -2px; border-right: 0; border-bottom: 0; border-top-left-radius: 8px; }
  .c-tr { top: -2px; right: -2px; border-left: 0; border-bottom: 0; border-top-right-radius: 8px; }
  .c-bl { bottom: -2px; left: -2px; border-right: 0; border-top: 0; border-bottom-left-radius: 8px; }
  .c-br { bottom: -2px; right: -2px; border-left: 0; border-top: 0; border-bottom-right-radius: 8px; }

  .qr-hint {
    font-size: 11px;
    font-weight: 600;
    color: #a8a29e;
    text-align: center;
  }
  .amount-card {
    background: #201f1f;
    border: 1px solid rgba(245, 197, 24, 0.25);
    border-radius: 18px;
    padding: 14px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .amount-label {
    font-size: 10px;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.12em;
    color: #a8a29e;
  }
  .amount-val {
    font-family: 'Montserrat', sans-serif;
    font-size: 26px;
    font-weight: 900;
    font-style: italic;
    color: #f5c518;
    letter-spacing: -0.02em;
  }
  .bank-details {
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 14px;
    padding: 10px 16px;
    font-size: 11px;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .bank-row {
    display: flex;
    justify-content: space-between;
    color: #a8a29e;
  }
  .bank-row b {
    color: #e5e2e1;
    font-weight: 700;
  }
  .footer-hint {
    font-size: 10px;
    color: #78716c;
    text-align: center;
  }
</style>
</head>
<body>
  <div class="card">
    <div class="head">
      <div class="brand-group">
        <div class="logo-icon">
          <span class="material-symbols-outlined">point_of_sale</span>
        </div>
        <div class="brand-text">
          <h1>DEV<span>CINE</span></h1>
          <p>Hệ thống thanh toán VietQR</p>
        </div>
      </div>
      <div class="status-pill">
        <span class="dot"></span> SẴN SÀNG
      </div>
    </div>
    <div class="body">
      ${movieTitle ? `
      <div class="order-summary">
        <div class="order-movie">${movieTitle}</div>
        <div class="order-details">
          ${roomName ? `<span>${roomName} • ${formatName}</span>` : ''}
          ${seatsStr ? `<span>· Ghế: <span class="seat-tag">${seatsStr}</span></span>` : ''}
        </div>
      </div>
      ` : ''}

      <div class="qr-wrapper">
        <span class="scan-corner c-tl"></span>
        <span class="scan-corner c-tr"></span>
        <span class="scan-corner c-bl"></span>
        <span class="scan-corner c-br"></span>
        <img src="${qr}" alt="VietQR" />
      </div>

      <p class="qr-hint">Mở ứng dụng Ngân hàng bất kỳ hoặc Ví điện tử để quét mã</p>

      <div class="amount-card">
        <span class="amount-label">Số tiền thanh toán</span>
        <span class="amount-val">${fmt(totalVal)}đ</span>
      </div>

      <div class="bank-details">
        <div class="bank-row"><span>Ngân hàng:</span><b>${bank.name || 'MB Bank'}</b></div>
        <div class="bank-row"><span>Số tài khoản:</span><b style="font-family:monospace;letter-spacing:0.05em">${bank.accountNo || '—'}</b></div>
        <div class="bank-row"><span>Chủ tài khoản:</span><b style="text-transform:uppercase">${bank.accountName || 'DEVCINE'}</b></div>
      </div>

      <p class="footer-hint">Hệ thống sẽ tự động xác nhận ngay sau khi nhận tiền</p>
    </div>
  </div>
</body>
</html>`
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
  allowOrphan.value = false
  member.value = null
  restoredBookingId.value = null
  cardNumberInput.value = ''
  cardError.value = ''
  completedBooking.value = null
  concessionSale.value = null
  fnbStep.value = 1
  showCashModal.value = false
  showQrModal.value = false
  qrBookingId.value = null
  qrHoldLoading.value = false
  sessionStartedAt.value = null
  showMobileReceiptDrawer.value = false
  cashGiven.value = 0
  lockedPriceTable.value = null // ── Nhả lock snapshot giá vé khi bắt đầu phiên mới ──
  lockedCombosPrices.value = null // ── Nhả lock snapshot giá catalog F&B ──
  clearVoucherState()
  fetchData()
}

const handleGlobalKeydown = (e) => {
  if (isHolding.value || isPaying.value) return
  if (currentStep.value === 6 && (e.key === 'Enter' || e.code === 'Space')) {
    e.preventDefault()
    resetPOS()
  } else if (fnbStep.value === 3 && (e.key === 'Enter' || e.code === 'Space')) {
    e.preventDefault()
    newConcessionSale()
  }
}

const loadSettings = async () => {
  await loadBankInfo()
}

// Snapshot Rule: Bỏ toàn bộ watcher reload F&B giữa phiên.
// Thực đơn được nạp lúc mở POS, giá đóng băng tại thời điểm bấm chọn.
// Phiên mới sau khi thanh toán hoặc hủy mới nạp dữ liệu mới.

onMounted(() => {
  nowTimer = setInterval(() => { nowTs.value = Date.now() }, 1000)
  fetchData(); loadBankInfo(); loadSettings();
  seatRealtime.connect(selectedShowtime.value?.id || null);
  window.addEventListener('keydown', handleGlobalKeydown)
})
onUnmounted(() => {
  if (nowTimer) clearInterval(nowTimer)
  stopHoldTimer()
  seatRealtime.disconnect()
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<template>
  <div class="h-full flex flex-col p-3 sm:p-5 lg:p-6 space-y-3 sm:space-y-5 bg-surface-container-lowest relative">
    <!-- Header -->
    <header class="flex flex-wrap lg:flex-nowrap justify-between items-center bg-surface px-3.5 py-2.5 sm:px-5 sm:py-3 rounded-2xl border border-outline-variant/10 shadow-xl gap-3">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 sm:w-9 sm:h-9 bg-primary rounded-xl flex items-center justify-center text-on-primary shadow-lg shadow-primary/20 shrink-0">
          <span class="material-symbols-outlined text-lg sm:text-xl">point_of_sale</span>
        </div>
        <div>
          <h1 class="text-base sm:text-lg font-black tracking-tighter uppercase italic text-on-surface leading-none">Ticketing <span class="text-primary">POS</span></h1>
          <p class="text-[8px] sm:text-[9px] font-bold text-on-surface-variant uppercase tracking-widest mt-0.5">Hệ thống bán vé & F&B v2.1</p>
        </div>
      </div>

      <div class="flex items-center gap-2 sm:gap-4 order-3 lg:order-2 w-full lg:w-auto justify-between lg:justify-start">
        <!-- Chọn luồng bán -->
        <div class="flex items-center gap-1 p-1 bg-surface-container-high rounded-xl border border-outline-variant/10">
          <button @click="switchMode('TICKET')"
                  :disabled="!canUseTicketing"
                  :class="saleMode === 'TICKET' ? 'bg-primary text-on-primary shadow' : 'text-on-surface-variant hover:text-on-surface'"
                  class="flex items-center gap-1 sm:gap-1.5 px-2.5 sm:px-3 py-1.5 rounded-lg text-[10px] sm:text-[11px] font-black uppercase tracking-wider transition-all">
            <span class="material-symbols-outlined text-sm sm:text-base">confirmation_number</span> Vé + F&B
          </button>
          <button @click="switchMode('FNB')"
                  :disabled="!canUseFnb"
                  :class="saleMode === 'FNB' ? 'bg-primary text-on-primary shadow' : 'text-on-surface-variant hover:text-on-surface'"
                  class="flex items-center gap-1 sm:gap-1.5 px-2.5 sm:px-3 py-1.5 rounded-lg text-[10px] sm:text-[11px] font-black uppercase tracking-wider transition-all">
            <span class="material-symbols-outlined text-sm sm:text-base">lunch_dining</span> Bán nhanh F&B
          </button>
        </div>

        <!-- Stepper bán vé (Desktop XL: Full 6 dots; Nhỏ hơn: Badge rút gọn) -->
        <div v-if="saleMode === 'TICKET'" class="flex items-center">
          <div class="hidden xl:flex items-center gap-1.5">
            <div v-for="i in 6" :key="i" class="flex items-center gap-1.5">
              <div :class="currentStep >= i ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant/40'"
                   class="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black transition-all">{{ i }}</div>
              <div v-if="i < 6" class="w-5 h-0.5 bg-outline-variant/20"></div>
            </div>
          </div>
          <div class="xl:hidden flex items-center gap-1 px-2.5 py-1 bg-surface-container-high rounded-xl border border-outline-variant/10 text-[10px] font-bold text-primary">
            <span>Bước {{ currentStep }}/6</span>
          </div>
        </div>
        <!-- Stepper bán nhanh F&B (2 bước) -->
        <div v-else class="flex items-center gap-2 text-[10px] sm:text-[11px] font-black uppercase tracking-wider">
          <span :class="fnbStep >= 1 ? 'text-primary' : 'text-on-surface-variant/40'" class="flex items-center gap-1">
            <span :class="fnbStep >= 1 ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant/40'" class="w-5 h-5 sm:w-6 sm:h-6 rounded-full flex items-center justify-center text-[10px]">1</span> <span class="hidden sm:inline">Chọn món</span>
          </span>
          <span class="w-4 h-0.5 bg-outline-variant/20"></span>
          <span :class="fnbStep >= 2 ? 'text-primary' : 'text-on-surface-variant/40'" class="flex items-center gap-1">
            <span :class="fnbStep >= 2 ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant/40'" class="w-5 h-5 sm:w-6 sm:h-6 rounded-full flex items-center justify-center text-[10px]">2</span> <span class="hidden sm:inline">Thanh toán</span>
          </span>
        </div>
      </div>

      <div class="flex items-center gap-2 sm:gap-2.5 order-2 lg:order-3">
        <!-- Đồng hồ giữ ghế -->
        <div v-if="holdActive"
             :class="holdUrgent ? 'bg-red-500/15 border-red-500/40 text-red-300 animate-pulse' : 'bg-primary/10 border-primary/30 text-primary'"
             class="flex items-center gap-1.5 px-2.5 sm:px-3.5 py-1.5 sm:py-2 rounded-xl border" title="Thời gian giữ ghế còn lại">
          <span class="material-symbols-outlined text-sm sm:text-base">timer</span>
          <span class="text-xs sm:text-sm font-black tabular-nums tracking-wider">{{ holdMmSs }}</span>
        </div>

        <!-- Giữ đơn (Hold Order) — vô hiệu hoá khi giỏ trống hoặc đã thanh toán xong -->
        <button @click="holdCurrentOrder" :disabled="!canHoldOrder || isHolding"
                :class="(canHoldOrder && !isHolding) ? 'bg-amber-500/10 border-amber-500/30 text-amber-400 hover:bg-amber-500/20' : 'bg-surface-container-high border-outline-variant/10 text-on-surface-variant/40 cursor-not-allowed opacity-40'"
                class="flex items-center gap-1 sm:gap-1.5 px-2.5 sm:px-3.5 py-1.5 sm:py-2 rounded-xl border text-[10px] sm:text-[11px] font-black uppercase tracking-wider transition-all">
          <span class="material-symbols-outlined text-base sm:text-lg">{{ isHolding ? 'progress_activity' : 'pause_circle' }}</span> <span class="hidden sm:inline">{{ isHolding ? 'ĐANG GIỮ...' : 'GIỮ ĐƠN' }}</span>
        </button>

        <!-- Danh sách đơn chờ -->
        <button type="button" @click="showHeldPanel = true"
                class="w-9 h-9 sm:w-10 sm:h-10 shrink-0 relative flex items-center justify-center rounded-xl border border-primary/40 bg-primary/10 text-primary hover:bg-primary/20 hover:border-primary transition-all cursor-pointer"
                title="Danh sách đơn chờ">
          <span class="material-symbols-outlined text-base sm:text-lg">receipt_long</span>
          <span v-if="posStore.heldOrders.length > 0" class="absolute -top-1.5 -right-1.5 w-4 h-4 sm:w-5 sm:h-5 bg-primary text-black font-black text-[9px] sm:text-xs rounded-full flex items-center justify-center border-2 border-surface shadow-sm">{{ posStore.heldOrders.length }}</span>
        </button>

        <AppButton variant="outline" size="sm" class="hidden sm:inline-flex" :disabled="currentStep === 6 || fnbStep === 3" :class="{'opacity-40 pointer-events-none': currentStep === 6 || fnbStep === 3}" @click="resetPOS">Hủy</AppButton>
        <button class="sm:hidden p-2 text-on-surface-variant hover:text-red-400 rounded-lg hover:bg-white/5" :disabled="currentStep === 6 || fnbStep === 3" :class="{'opacity-40 pointer-events-none': currentStep === 6 || fnbStep === 3}" @click="resetPOS" title="Hủy">
          <span class="material-symbols-outlined text-lg">restart_alt</span>
        </button>
      </div>
    </header>

    <main class="flex-grow grid grid-cols-12 gap-4 sm:gap-5 overflow-hidden min-h-0 pb-16 lg:pb-0">
      <div class="col-span-12 lg:col-span-8 xl:col-span-9 bg-surface border border-outline-variant/10 rounded-2xl sm:rounded-3xl shadow-2xl overflow-hidden flex flex-col min-h-0">

        <template v-if="saleMode === 'TICKET'">
        <!-- Step 1: Showtime -->
        <div v-if="currentStep === 1" class="p-6 space-y-8 overflow-y-auto custom-scrollbar">
          <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
            <span class="w-8 h-1 bg-primary rounded-full"></span> 1. Chọn phim & suất chiếu
          </h2>

          <!-- BỘ LỌC NGÀY CHO POS (DYNAMIC AVAILABLE DATES) -->
          <div class="flex flex-wrap items-center gap-2 pb-2 relative">
            <button v-for="tab in quickDateTabs" :key="tab.value" type="button"
              @click="selectPosDate(tab.value)"
              :class="[
                'shrink-0 px-4 py-2.5 rounded-2xl font-bold text-sm transition-all border',
                selectedPosDate === tab.value
                  ? 'bg-primary text-on-primary border-primary shadow-lg shadow-primary/20'
                  : 'bg-surface-container text-on-surface-variant border-outline-variant/30 hover:bg-surface-container-high hover:text-on-surface'
              ]"
            >
              {{ tab.label }}
            </button>
            
            <!-- Custom Dropdown / Popover Card -->
            <div v-if="otherDateOptions.length > 0" class="relative shrink-0" :class="{ 'z-[9999]': showOtherDatesDropdown }">
              <button type="button" @click.stop="toggleOtherDates"
                :class="[
                  'px-4 py-2.5 rounded-2xl font-bold text-sm transition-all border flex items-center gap-1',
                  !quickDateTabs.find(t => t.value === selectedPosDate)
                    ? 'bg-primary text-on-primary border-primary shadow-lg shadow-primary/20'
                    : 'bg-surface-container text-on-surface-variant border-outline-variant/30 hover:bg-surface-container-high hover:text-on-surface'
                ]"
              >
                Ngày khác ({{ otherDateOptions.length }})
                <span class="material-symbols-outlined text-lg leading-none transition-transform" :class="{ 'rotate-180': showOtherDatesDropdown }">arrow_drop_down</span>
              </button>
              
              <!-- Dropdown Menu -->
              <div v-if="showOtherDatesDropdown" 
                class="absolute top-full left-1/2 -translate-x-1/2 mt-2 w-max min-w-[160px] bg-surface border border-outline-variant/30 rounded-2xl shadow-2xl z-50 overflow-hidden py-1.5">
                <button v-for="opt in otherDateOptions" :key="opt.value" type="button"
                  @click="selectPosDate(opt.value)"
                  class="w-full text-left px-4 py-3 text-sm font-semibold transition-colors flex items-center justify-between"
                  :class="selectedPosDate === opt.value ? 'bg-primary/10 text-primary' : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'"
                >
                  {{ opt.label }}
                  <span v-if="selectedPosDate === opt.value" class="material-symbols-outlined text-base text-primary">check</span>
                </button>
              </div>
              
              <!-- Invisible Backdrop to close dropdown -->
              <div v-if="showOtherDatesDropdown" class="fixed inset-0 z-40" @click="showOtherDatesDropdown = false"></div>
            </div>
          </div>

          <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
            <div v-for="i in 4" :key="i" class="h-44 bg-surface-container-high rounded-3xl animate-pulse"></div>
          </div>
          <div v-else-if="error" class="p-6 bg-red-500/10 border border-red-500/20 rounded-2xl text-red-400 text-sm flex items-center justify-between gap-4">
            <span>{{ error }}</span>
            <button type="button" @click="fetchData"
              class="shrink-0 inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-red-500/20 hover:bg-red-500/30 text-red-300 font-semibold transition-colors">
              <span class="material-symbols-outlined text-base">refresh</span>Thử lại
            </button>
          </div>
          <div v-else-if="groupedMoviesWithShowtimes.length === 0" class="py-20 text-center border border-dashed border-outline-variant/20 rounded-3xl col-span-full">
            <span class="material-symbols-outlined text-6xl text-primary/40 mb-3">calendar_month</span>
            <p class="text-on-surface-variant text-lg font-semibold">Không có suất chiếu nào cho ngày {{ selectedPosDate ? `${selectedPosDate.split('-')[2]}/${selectedPosDate.split('-')[1]}/${selectedPosDate.split('-')[0]}` : '' }}</p>
            <p class="text-sm text-on-surface-variant/60 mt-1">Vui lòng chọn ngày khác hoặc kiểm tra lại cấu hình lịch chiếu trong Quản trị.</p>
          </div>

          <div v-else class="grid grid-cols-1 xl:grid-cols-2 gap-4 sm:gap-6">
            <div v-for="movieGroup in groupedMoviesWithShowtimes" :key="movieGroup.movie.id"
                 class="relative p-4 sm:p-6 bg-surface-container-high rounded-3xl border border-outline-variant/10 transition-all">
              <div class="flex gap-4 sm:gap-6">
                <!-- BÊN TRÁI: Ảnh Poster -->
                <div class="w-20 sm:w-24 shrink-0 flex flex-col items-center">
                  <div class="w-20 sm:w-24 h-30 sm:h-36 bg-surface-container-highest rounded-xl overflow-hidden shadow-lg border border-outline-variant/10">
                    <img :src="getPoster(movieGroup.movie)" class="w-full h-full object-cover" />
                  </div>
                </div>
                
                <!-- BÊN PHẢI: Thông tin & Suất chiếu -->
                <div class="flex flex-col min-w-0 w-full">
                  <h3 class="font-black text-base sm:text-lg uppercase tracking-tight text-on-surface truncate">{{ movieGroup.movie.title }}</h3>
                  <div class="flex items-center gap-2 mt-1 mb-4">
                    <span class="text-[10px] font-bold text-on-surface-variant uppercase">{{ movieGroup.movie.durationMins || '???' }} PHÚT</span>
                  </div>
                  
                  <div class="flex flex-col gap-4">
                    <div v-for="roomGroup in movieGroup.roomGroups" :key="roomGroup.groupLabel">
                      <p class="text-[10px] font-black text-on-surface-variant/80 uppercase tracking-widest mb-2">{{ roomGroup.groupLabel }}</p>
                      <div class="flex flex-wrap gap-2">
                        <button v-for="st in roomGroup.showtimes" :key="st.id" @click="selectShowtime(st)" type="button"
                          class="px-3.5 sm:px-4 py-2 bg-surface-container-highest hover:bg-primary/20 text-on-surface hover:text-primary transition-colors text-xs sm:text-sm font-black italic rounded-xl border border-outline-variant/20 hover:border-primary/50 tabular-nums">
                          {{ fmtTime(st.startTime) }}
                        </button>
                      </div>
                    </div>
                  </div>
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
            <SeatGridRenderer
              :seats="seatData.seats"
              :matrix-row="seatData.matrixRow"
              :matrix-col="seatData.matrixCol"
              :selected-seats="selectedSeats"
              mode="pos"
              :is-seat-locked-by-others="isSeatLockedByOthers"
              :is-seat-maintenance="isSeatMaintenance"
              :is-orphan-seat="isOrphanSeat"
              @seat-click="toggleSeat"
            />
          </div>

          <div class="mt-3 flex items-center justify-between shrink-0">
            <div class="flex gap-4 text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-surface-container-high border border-outline-variant/20"></span>Thường</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-red-900/40 border border-red-500/40"></span>VIP</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-purple-900/40 border border-purple-500/40"></span>Sweetbox</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-surface-container-high opacity-40"></span>Đã bán</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 flex items-center justify-center rounded bg-surface-container-highest border border-white/10 text-red-500 opacity-60"><span class="material-symbols-outlined text-[8px]">build</span></span>Ghế bảo trì</span>
            </div>
            <div class="flex items-center gap-3">
              <!-- Cảnh báo ghế mồ côi: hiện khi lựa chọn để trống 1 ghế lẻ và chưa bật ngoại lệ -->
              <span v-if="hasOrphan && !allowOrphan"
                    class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-red-500/10 border border-red-500/40 text-[10px] font-bold uppercase tracking-wider text-red-400">
                <span class="material-symbols-outlined text-[15px]">warning</span>
                Đang để trống ghế lẻ — chọn nốt ô đỏ hoặc bỏ bớt
              </span>
              <!-- POS override: cho phép để trống 1 ghế lẻ. Chỉ ADMIN/MANAGER thấy & bật được (backend cũng gate theo vai trò) -->
              <button v-if="canOverrideOrphan" type="button" @click="allowOrphan = !allowOrphan"
                      :title="allowOrphan ? 'Đang cho phép để trống ghế lẻ' : 'Bật để bán khách ngoại lệ dù để trống 1 ghế lẻ'"
                      class="flex items-center gap-2 px-3 py-1.5 rounded-lg border text-[10px] font-bold uppercase tracking-wider transition-all"
                      :class="allowOrphan ? 'border-primary/60 bg-primary/10 text-primary' : 'border-outline-variant/20 text-on-surface-variant/60 hover:border-outline-variant/40'">
                <span class="material-symbols-outlined text-[15px]">{{ allowOrphan ? 'toggle_on' : 'toggle_off' }}</span>
                Cho phép lẻ ghế
              </button>
              <AppButton @click="currentStep = 3" :disabled="selectedSeats.length === 0 || (hasOrphan && !allowOrphan)">3. Xác nhận vé</AppButton>
            </div>
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
              Chọn số lượng vé theo đối tượng — tổng phải bằng số vé yêu cầu ({{ totalRequiredTickets }} vé / {{ selectedSeats.length }} ghế đã chọn).
            </p>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div v-for="(label, code) in audienceLabels" :key="code"
                   class="p-5 bg-surface-container-high rounded-[24px] border border-outline-variant/10 flex items-center justify-between gap-4">
                <span class="text-sm font-black text-on-surface uppercase">{{ label }}</span>
                <div class="flex items-center gap-3 shrink-0">
                  <button @click="setTicketCount(code, -1)" :disabled="(ticketCounts[code] || 0) <= 0 || (ticketCounts[code] || 0) === totalRequiredTickets"
                          class="w-9 h-9 flex items-center justify-center rounded-full bg-surface-container border border-outline-variant/10 disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer hover:text-primary transition-colors">
                    <span class="material-symbols-outlined text-base">remove</span>
                  </button>
                  <span class="w-7 text-center text-lg font-black tabular-nums text-on-surface">{{ ticketCounts[code] || 0 }}</span>
                  <button @click="setTicketCount(code, 1)" :disabled="(ticketCounts[code] || 0) >= totalRequiredTickets"
                          class="w-9 h-9 flex items-center justify-center rounded-full bg-surface-container border border-outline-variant/10 disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer hover:text-primary transition-colors">
                    <span class="material-symbols-outlined text-base">add</span>
                  </button>
                </div>
              </div>
            </div>

            <div class="flex items-center justify-between px-2 pt-2 text-sm">
              <span class="text-on-surface-variant">Đã gán</span>
              <span class="font-black tabular-nums" :class="ticketsMatchSeats ? 'text-green-400' : 'text-primary'">
                {{ totalTicketCount }} / {{ totalRequiredTickets }} vé
              </span>
            </div>
            <div class="px-2 text-xs font-bold text-on-surface-variant">
              Ghế đã chọn: <span class="text-primary">{{ selectedSeats.map(s => seatLabel(s)).join(', ') }}</span>
            </div>
          </div>

          <div class="mt-6 flex items-center justify-end gap-4">
            <span v-if="!ticketsMatchSeats" class="text-xs font-bold text-amber-400">Cần gán đủ {{ totalRequiredTickets }} vé</span>
            <AppButton @click="currentStep = 4" :disabled="!ticketsMatchSeats">4. Combo / Đồ ăn</AppButton>
          </div>
        </div>

        <!-- Step 4: F&B -->
        <div v-if="currentStep === 4" class="p-6 flex flex-col h-full overflow-hidden">
          <div class="flex justify-between items-center mb-8">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 4. Combo / Đồ ăn & Nước uống
            </h2>
            <div class="flex items-center gap-3">
              <AppButton variant="ghost" @click="currentStep = 3">Quay lại</AppButton>
            </div>
          </div>

          <div v-if="combos.length === 0" class="flex-grow flex items-center justify-center text-on-surface-variant text-sm">
            Chưa có combo. Thêm ở "Thực đơn F&B / Combo".
          </div>
          <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3.5 flex-grow overflow-y-auto custom-scrollbar pr-2 content-start pb-6">
            <div v-for="cb in combos" :key="cb.id" 
                 @click="handleFnbCardClick(cb)"
                 :class="fnbQtyOf(cb.id) ? 'bg-primary/10 border-primary shadow-lg shadow-primary/5 cursor-pointer' : 'bg-surface-container-low/60 border-outline-variant/30 hover:border-primary/50 cursor-pointer'"
                 class="min-h-[150px] max-h-[200px] p-3.5 rounded-xl border flex flex-col justify-between transition-all duration-200">

              <template v-if="fnbQtyOf(cb.id)">
                <!-- State 2: Selected — mỗi bộ vị là 1 dòng có stepper riêng -->
                <div class="flex items-start gap-3 flex-grow min-h-0">
                  <div class="w-14 h-14 rounded-2xl overflow-hidden bg-surface-container-highest shrink-0 flex items-center justify-center">
                    <img v-if="cb.imageUrl" :src="cb.imageUrl" class="w-full h-full object-cover" />
                    <span v-else class="material-symbols-outlined text-on-surface-variant/40">fastfood</span>
                  </div>
                  <div class="flex-grow min-w-0 flex flex-col h-full">
                    <div class="flex justify-between items-start gap-2">
                        <h3 class="text-base font-bold text-on-surface truncate" :title="cb.name">{{ cb.name }}</h3>
                        <span class="text-base font-semibold text-primary shrink-0 leading-none mt-1">{{ fmt(fnbLineTotal(cb.id)) }}đ</span>
                    </div>
                    <div class="mt-1.5 flex-1 overflow-y-auto custom-scrollbar pr-1 space-y-1.5">
                      <div v-for="line in fnbLinesOf(cb.id)" :key="optionsKey(line.options)" class="flex items-center justify-between gap-2">
                        <div class="min-w-0 text-[11px] text-on-surface leading-tight truncate">
                          <template v-if="line.options && line.options.length">
                            <span v-for="(opt, oi) in line.options" :key="opt.optionItemId"><span v-if="oi">, </span>{{ opt.optionName }}<span v-if="opt.surchargePrice > 0" class="text-amber-400 font-medium"> (+{{ fmt(opt.surchargePrice) }}đ)</span></span>
                          </template>
                          <span v-else class="text-on-surface-variant/60">Mặc định</span>
                        </div>
                        <div class="flex items-center gap-1 shrink-0">
                          <button v-if="cb.slots && cb.slots.length" @click.stop="editFnbOptions(line, selectedCombos.indexOf(line))" class="w-5 h-5 rounded-full text-on-surface-variant/60 flex items-center justify-center hover:text-primary transition-colors" title="Đổi vị">
                            <span class="material-symbols-outlined text-[13px]">edit</span>
                          </button>
                          <div class="flex items-center gap-1.5 bg-surface-container-highest rounded-full p-0.5 border border-outline-variant/20">
                            <button @click.stop="changeComboQty(line, -1)" class="w-5 h-5 rounded-full bg-surface-container-lowest text-on-surface flex items-center justify-center hover:bg-primary hover:text-black transition-colors">
                              <span class="material-symbols-outlined text-[13px]">remove</span>
                            </button>
                            <span class="w-4 text-center text-[11px] font-black tabular-nums">{{ line.quantity }}</span>
                            <button @click.stop="changeComboQty(line, 1)" class="w-5 h-5 rounded-full bg-primary/20 text-primary flex items-center justify-center hover:bg-primary hover:text-black transition-colors">
                              <span class="material-symbols-outlined text-[13px]">add</span>
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="cb.slots && cb.slots.length" class="flex items-center justify-end mt-2 shrink-0">
                  <button @click.stop="openFnbModal(cb)" class="flex items-center gap-1 text-[11px] font-bold text-primary hover:text-primary/80 transition-colors px-2 py-1 rounded-full hover:bg-primary/10">
                    <span class="material-symbols-outlined text-[15px]">add</span> Thêm vị khác
                  </button>
                </div>
              </template>

              <template v-else>
                <!-- State 1: Unselected -->
                <div class="flex items-start gap-3 flex-grow min-h-0">
                  <div class="w-14 h-14 rounded-2xl overflow-hidden bg-surface-container-highest shrink-0 flex items-center justify-center">
                    <img v-if="cb.imageUrl" :src="cb.imageUrl" class="w-full h-full object-cover" />
                    <span v-else class="material-symbols-outlined text-on-surface-variant/40">fastfood</span>
                  </div>
                  <div class="flex-grow min-w-0 flex flex-col h-full">
                    <div class="flex justify-between items-start gap-2">
                        <h3 class="text-base font-bold text-on-surface truncate" :title="cb.name">{{ cb.name }}</h3>
                        <span class="text-base font-semibold text-primary shrink-0 leading-none mt-1">{{ fmt(cb.price) }}đ</span>
                    </div>
                    <p class="text-[11px] text-on-surface-variant line-clamp-2 mt-1.5 flex-1" :title="cb.description">{{ cb.description }}</p>
                  </div>
                </div>
                <div class="flex items-end justify-end mt-2 shrink-0">
                  <button @click.stop="addCombo(cb)" class="w-8 h-8 rounded-full bg-primary/10 text-primary flex items-center justify-center hover:bg-primary hover:text-black transition-colors">
                    <span class="material-symbols-outlined text-[18px]">add</span>
                  </button>
                </div>
              </template>

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
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 sm:gap-8">
            <div class="bg-surface-container-high p-8 rounded-3xl border border-outline-variant/10 space-y-5">
              <p class="text-[10px] font-black text-primary uppercase tracking-widest">Chi tiết hóa đơn</p>
              <h3 class="text-xl font-black italic uppercase text-on-surface">{{ selectedShowtime?.movieTitle }}</h3>
              <div class="flex justify-between text-xs font-bold text-on-surface-variant uppercase border-b border-outline-variant/10 pb-3">
                <span>Vé (x{{ totalRequiredTickets }})</span>
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
              <div v-if="member" class="bg-surface-container-high border border-outline-variant/10 p-6 rounded-3xl space-y-4">
                <div class="flex items-center justify-between">
                  <p class="text-[10px] font-black text-primary uppercase tracking-widest">Voucher / Khuyến mãi</p>
                  <button v-if="appliedVoucher" @click="clearVoucher" class="text-xs text-on-surface-variant hover:text-red-400 font-bold flex items-center gap-1 transition-colors px-2 py-1 rounded-lg hover:bg-white/5">
                    <span class="material-symbols-outlined text-sm">close</span> Bỏ chọn
                  </button>
                </div>

                <!-- Ô nhập mã voucher thủ công -->
                <div class="flex gap-2">
                  <input v-model="voucherCodeInput" type="text" placeholder="NHẬP MÃ VOUCHER..."
                         @keydown.enter.prevent="applyVoucher"
                         class="flex-1 bg-surface-container border border-outline-variant/10 rounded-2xl py-3 px-4 text-on-surface text-sm font-bold outline-none focus:border-primary/50 uppercase" />
                  <AppButton variant="primary" :disabled="isApplyingVoucher || !voucherCodeInput.trim()" @click="applyVoucher">
                    {{ isApplyingVoucher ? '...' : 'Áp dụng' }}
                  </AppButton>
                </div>
                <p v-if="voucherError" class="text-xs text-red-400 font-bold">{{ voucherError }}</p>

                <!-- Skeleton loading khi đang chấm điều kiện -->
                <div v-if="!isVoucherEvalsReady && ownedVouchers.length > 0" class="space-y-3 pt-2 border-t border-outline-variant/10">
                  <div class="h-4 w-36 bg-white/10 rounded animate-pulse"></div>
                  <div class="grid grid-cols-1 gap-2.5">
                    <div v-for="i in Math.min(ownedVouchers.length, 3)" :key="i"
                         class="border border-outline-variant/20 p-3 rounded-2xl flex items-center justify-between animate-pulse bg-surface-container/30">
                      <div class="space-y-1.5 flex-1">
                        <div class="h-3.5 w-28 bg-white/10 rounded"></div>
                        <div class="h-2.5 w-20 bg-white/5 rounded"></div>
                      </div>
                      <div class="h-5 w-5 bg-white/5 rounded-full ml-4"></div>
                    </div>
                  </div>
                </div>

                <!-- Danh sách Voucher phân loại -->
                <div v-else-if="isVoucherEvalsReady" class="space-y-4 pt-2 border-t border-outline-variant/10">

                  <!-- KHU VỰC 1: Voucher khả dụng -->
                  <div class="space-y-2.5">
                    <div class="flex items-center justify-between">
                      <p class="text-xs font-bold text-primary uppercase tracking-wider">
                        VOUCHER KHẢ DỤNG ({{ eligibleVouchers.length }})
                      </p>
                      <span v-if="eligibleVouchers.length > 0" class="text-[11px] text-on-surface-variant/80">Nhấn để áp dụng</span>
                    </div>

                    <!-- Danh sách thẻ khả dụng -->
                    <div v-if="eligibleVouchers.length > 0" class="grid grid-cols-1 gap-2.5 max-h-64 overflow-y-auto custom-scrollbar pr-1">
                      <div
                        v-for="v in eligibleVouchers"
                        :key="v.id"
                        @click="selectVoucher(v)"
                        :class="[
                          appliedVoucher?.id === v.id 
                            ? 'border-primary bg-primary/10 shadow-lg shadow-primary/5 ring-1 ring-primary' 
                            : 'border-outline-variant/25 bg-surface-container/60 hover:border-primary/50 hover:bg-surface-container/90'
                        ]"
                        class="border p-3 sm:p-3.5 rounded-2xl flex items-center justify-between cursor-pointer transition-all duration-200 group relative overflow-hidden text-left"
                      >
                        <!-- Accent bar khi được chọn -->
                        <div v-if="appliedVoucher?.id === v.id" class="absolute left-0 top-0 bottom-0 w-1 bg-primary"></div>
                        
                        <div class="space-y-1 pl-1 min-w-0 flex-1">
                          <div class="flex items-center gap-2 flex-wrap">
                            <span class="font-mono font-black text-xs sm:text-sm text-primary uppercase tracking-wide">
                              {{ v.code || v.promotion?.code || voucherEvals[v.id]?.code }}
                            </span>
                            <span class="text-[10px] px-2 py-0.5 rounded-md font-bold bg-primary/20 text-primary">
                              Giảm {{ (voucherEvals[v.id]?.discountType || v.promotion?.discountType || v.discountType) === 'PERCENTAGE' ? (voucherEvals[v.id]?.discountValue ?? v.promotion?.discountValue ?? v.discountValue) + '%' : fmt(voucherEvals[v.id]?.discountValue ?? v.promotion?.discountValue ?? v.discountValue) + 'đ' }}
                            </span>
                          </div>
                          <p v-if="v.promotion?.name || v.promotion?.title || voucherEvals[v.id]?.title" class="text-xs text-on-surface font-semibold line-clamp-1">
                            {{ v.promotion?.name || v.promotion?.title || voucherEvals[v.id]?.title }}
                          </p>
                          <div class="flex items-center gap-3 text-[10px] text-on-surface-variant/80 flex-wrap">
                            <span v-if="voucherEvals[v.id]?.applicableMovieTitle" class="text-amber-300 font-medium">
                              Phim: {{ voucherEvals[v.id].applicableMovieTitle }}
                            </span>
                            <span v-if="Number(voucherEvals[v.id]?.minOrderValue || v.promotion?.minOrderValue || 0) > 0">
                              Đơn từ {{ fmt(voucherEvals[v.id]?.minOrderValue || v.promotion?.minOrderValue) }}đ
                            </span>
                            <span>HSD: {{ formatVoucherDate(v.validUntil || voucherEvals[v.id]?.validUntil) }}</span>
                          </div>
                          <p v-if="voucherEvals[v.id]?.discountAmount > 0" class="text-[11px] text-green-400 font-bold pt-0.5">
                            Tiết kiệm {{ fmt(voucherEvals[v.id].discountAmount) }}đ
                          </p>
                        </div>

                        <div class="shrink-0 ml-2 sm:ml-3">
                          <div v-if="appliedVoucher?.id === v.id" class="w-5 h-5 sm:w-6 sm:h-6 rounded-full bg-primary text-on-primary flex items-center justify-center shadow-sm">
                            <span class="material-symbols-outlined text-xs sm:text-sm font-bold">check</span>
                          </div>
                          <div v-else class="w-5 h-5 sm:w-6 sm:h-6 rounded-full border border-outline-variant/40 group-hover:border-primary/60 transition-colors"></div>
                        </div>
                      </div>
                    </div>

                    <!-- Trống khả dụng -->
                    <div v-else-if="ineligibleVouchers.length > 0" class="py-3 px-4 rounded-xl border border-dashed border-outline-variant/20 bg-surface-container/30 text-center">
                      <p class="text-xs text-on-surface-variant">Chưa có voucher nào đủ điều kiện áp dụng cho đơn hàng hiện tại.</p>
                    </div>
                    <div v-else class="py-3 px-4 rounded-xl border border-dashed border-outline-variant/20 bg-surface-container/30 text-center">
                      <p class="text-xs text-on-surface-variant">Khách hàng chưa có voucher nào trong ví.</p>
                    </div>
                  </div>

                  <!-- KHU VỰC 2: Ưu đãi chưa đủ điều kiện -->
                  <div v-if="ineligibleVouchers.length > 0" class="space-y-2.5 pt-3 border-t border-outline-variant/15">
                    <p class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">
                      ƯU ĐÃI CHƯA ĐỦ ĐIỀU KIỆN ({{ ineligibleVouchers.length }})
                    </p>

                    <div class="grid grid-cols-1 gap-2.5 max-h-56 overflow-y-auto custom-scrollbar pr-1">
                      <div
                        v-for="v in ineligibleVouchers"
                        :key="v.id"
                        class="border border-dashed border-outline-variant/25 bg-surface-container/20 p-3 sm:p-3.5 rounded-2xl opacity-75 relative overflow-hidden text-left"
                      >
                        <div class="space-y-1.5">
                          <div class="flex items-center gap-2 flex-wrap">
                            <span class="font-mono font-bold text-xs sm:text-sm text-on-surface-variant uppercase">
                              {{ v.code || v.promotion?.code || voucherEvals[v.id]?.code }}
                            </span>
                            <span class="text-[10px] px-2 py-0.5 rounded-md font-semibold bg-white/5 text-on-surface-variant">
                              Giảm {{ (voucherEvals[v.id]?.discountType || v.promotion?.discountType || v.discountType) === 'PERCENTAGE' ? (voucherEvals[v.id]?.discountValue ?? v.promotion?.discountValue ?? v.discountValue) + '%' : fmt(voucherEvals[v.id]?.discountValue ?? v.promotion?.discountValue ?? v.discountValue) + 'đ' }}
                            </span>
                          </div>
                          
                          <p v-if="v.promotion?.name || v.promotion?.title || voucherEvals[v.id]?.title" class="text-xs text-on-surface-variant/90 line-clamp-1">
                            {{ v.promotion?.name || v.promotion?.title || voucherEvals[v.id]?.title }}
                          </p>

                          <!-- Lý do chưa đủ điều kiện -->
                          <div class="pt-0.5">
                            <p v-if="isMissingOrderTotal(v)" class="text-[11px] text-amber-400 font-bold bg-amber-500/10 px-2.5 py-1.5 rounded-lg border border-amber-500/20">
                              Mua thêm {{ getMissingAmount(v) }} để được giảm {{ fmt(voucherEvals[v.id]?.discountValue ?? v.promotion?.discountValue ?? v.discountValue) }}{{ (voucherEvals[v.id]?.discountType || v.promotion?.discountType || v.discountType) === 'PERCENTAGE' ? '%' : 'đ' }}
                            </p>
                            <p v-else class="text-[11px] text-red-400/90 font-medium bg-red-500/10 px-2.5 py-1.5 rounded-lg border border-red-500/20">
                              {{ voucherEvals[v.id]?.reason || 'Không đủ điều kiện áp dụng' }}
                            </p>
                          </div>

                          <div class="flex items-center gap-3 text-[10px] text-on-surface-variant/60 pt-0.5 flex-wrap">
                            <span v-if="voucherEvals[v.id]?.applicableMovieTitle" class="text-amber-300/80 font-medium">
                              Phim: {{ voucherEvals[v.id].applicableMovieTitle }}
                            </span>
                            <span v-if="Number(voucherEvals[v.id]?.minOrderValue || v.promotion?.minOrderValue || 0) > 0">
                              Đơn tối thiểu {{ fmt(voucherEvals[v.id]?.minOrderValue || v.promotion?.minOrderValue) }}đ
                            </span>
                            <span>HSD: {{ formatVoucherDate(v.validUntil || voucherEvals[v.id]?.validUntil) }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="openCashModal" :disabled="isPaying || qrHoldLoading">
                  <span class="material-symbols-outlined">payments</span> Tiền mặt
                </AppButton>
                <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="openQrModal" :disabled="isPaying || qrHoldLoading">
                  <span v-if="qrHoldLoading" class="material-symbols-outlined animate-spin">progress_activity</span>
                  <span v-else class="material-symbols-outlined">qr_code_2</span>
                  {{ qrHoldLoading ? 'Đang tạo đơn...' : 'Chuyển khoản QR' }}
                </AppButton>
              </div>
              <p v-if="isPaying" class="text-center text-xs text-on-surface-variant">Đang xử lý thanh toán...</p>
            </div>
          </div>
        </div>

        <!-- Step 6: Done -->
        <div v-if="currentStep === 6" class="p-4 sm:p-6 flex flex-col items-center justify-start text-center h-full space-y-4 sm:space-y-5 overflow-y-auto custom-scrollbar">
          <div class="w-14 h-14 sm:w-16 sm:h-16 bg-green-500/15 text-green-400 border border-green-500/30 rounded-full flex items-center justify-center shadow-lg shadow-green-500/10 shrink-0 mt-1">
            <span class="material-symbols-outlined text-3xl sm:text-4xl">check_circle</span>
          </div>
          <div class="space-y-0.5">
            <h2 class="text-2xl sm:text-3xl font-black uppercase italic tracking-tighter text-on-surface">Thanh toán thành công</h2>
            <p class="text-on-surface-variant font-bold uppercase tracking-widest text-[11px]">Xuất vé và bàn giao cho khách</p>
          </div>

          <div class="bg-surface-container-high/90 p-5 sm:p-6 rounded-2xl sm:rounded-3xl border border-outline-variant/15 shadow-xl w-full max-w-2xl space-y-4 text-left">
            <!-- Header: Mã đơn + Phòng -->
            <div class="flex justify-between items-start border-b border-outline-variant/10 pb-3">
              <div>
                <p class="text-[10px] font-black text-primary uppercase tracking-wider">Mã đặt vé</p>
                <p class="text-lg sm:text-xl font-black font-mono text-primary tracking-wide">{{ completedBooking?.bookingCode }}</p>
              </div>
              <div class="text-right">
                <p class="text-[10px] font-black text-primary uppercase tracking-wider">Phòng chiếu</p>
                <p class="text-sm sm:text-base font-black text-on-surface">{{ selectedShowtime?.roomName }}</p>
              </div>
            </div>

            <!-- Phim & Suất chiếu -->
            <div v-if="selectedShowtime" class="space-y-0.5">
              <p class="text-[10px] font-black text-on-surface-variant uppercase tracking-wider">Phim & Suất chiếu</p>
              <h3 class="text-sm sm:text-base font-black uppercase italic text-on-surface">{{ selectedShowtime.movieTitle }}</h3>
              <p class="text-xs text-on-surface-variant font-medium">
                {{ selectedShowtime.formatName }} · {{ new Date(selectedShowtime.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }} - {{ new Date(selectedShowtime.endTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }} · {{ new Date(selectedShowtime.startTime).toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' }) }}
              </p>
            </div>

            <!-- Danh sách ghế -->
            <div v-if="selectedSeats.length" class="space-y-2 border-t border-dashed border-outline-variant/20 pt-3">
              <div class="flex justify-between items-center text-xs">
                <span class="text-[10px] font-black text-on-surface-variant uppercase tracking-wider">
                  Ghế đã chọn ({{ selectedSeats.length }} ghế / {{ totalRequiredTickets }} vé)
                </span>
                <span class="font-bold text-on-surface">{{ fmt(seatTotal) }}đ</span>
              </div>
              <div class="flex flex-wrap gap-1.5">
                <span v-for="s in selectedSeats" :key="s.seatId"
                      class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-primary/10 border border-primary/25 text-primary text-xs font-black uppercase">
                  {{ seatLabel(s) }}
                  <span class="text-[10px] font-semibold text-on-surface-variant normal-case">· {{ seatTypeLabel(s.seatType) }}</span>
                </span>
              </div>
            </div>

            <!-- F&B / Combo -->
            <div v-if="selectedCombos.length" class="space-y-1.5 border-t border-dashed border-outline-variant/20 pt-3">
              <div class="flex justify-between items-center text-xs">
                <span class="text-[10px] font-black text-on-surface-variant uppercase tracking-wider">
                  Bắp nước & Combo ({{ selectedCombos.reduce((a, c) => a + c.quantity, 0) }} phần)
                </span>
                <span class="font-bold text-on-surface">{{ fmt(comboTotal) }}đ</span>
              </div>
              <div v-for="(c, ci) in selectedCombos" :key="ci" class="text-xs text-on-surface flex justify-between items-start">
                <div>
                  <span class="font-bold">{{ c.name }}</span> <span class="text-on-surface-variant">x{{ c.quantity }}</span>
                  <div v-if="c.options && c.options.length" class="text-[10px] text-on-surface-variant mt-0.5 ml-2">
                    <span v-for="(opt, oi) in c.options" :key="opt.optionItemId">
                      <span v-if="oi">, </span>{{ opt.optionName }}<span v-if="opt.surchargePrice > 0" class="text-amber-400 font-medium"> (+{{ fmt(opt.surchargePrice) }}đ)</span>
                    </span>
                  </div>
                </div>
                <span class="font-medium text-on-surface-variant">{{ fmt(((c.snapshotPrice ?? c.price) + (c.surchargePrice || 0)) * c.quantity) }}đ</span>
              </div>
            </div>

            <!-- Tóm tắt thanh toán -->
            <div class="border-t border-dashed border-outline-variant/20 pt-3 space-y-2">
              <div class="flex justify-between items-center text-xs text-on-surface-variant">
                <span>Hình thức thanh toán</span>
                <span class="font-bold text-on-surface">
                  {{ paymentMethod === 'CASH' ? 'Tiền mặt' : 'Chuyển khoản QR (VietQR)' }}
                </span>
              </div>
              <div v-if="paymentMethod === 'CASH' && cashGiven > 0" class="flex justify-between items-center text-xs text-on-surface-variant">
                <span>Tiền khách đưa / Trả lại</span>
                <span>Khách đưa: <b class="text-on-surface">{{ fmt(cashGiven) }}đ</b> · Thối lại: <b class="text-green-400">{{ fmt(changeDue) }}đ</b></span>
              </div>
              <div v-if="member" class="flex justify-between items-center text-xs text-on-surface-variant">
                <span>Thành viên</span>
                <span class="font-bold text-primary">{{ member.fullName }} ({{ member.membershipTier }})</span>
              </div>
              <div v-if="completedBooking?.discountAmount || discountAmount" class="flex justify-between items-center text-xs text-green-400 font-bold">
                <span>Giảm giá {{ appliedVoucher ? '(' + appliedVoucher.code + ')' : '' }}</span>
                <span>-{{ fmt(completedBooking?.discountAmount ?? discountAmount) }}đ</span>
              </div>
              <div class="flex justify-between items-center pt-2.5 border-t border-outline-variant/10">
                <span class="text-xs font-black text-primary uppercase tracking-wider">Tổng thanh toán</span>
                <span class="text-xl sm:text-2xl font-black text-primary italic tracking-tight">{{ fmt(completedBooking?.finalPrice ?? payableTotal) }}đ</span>
              </div>
            </div>
          </div>

          <div class="flex justify-center w-full max-w-2xl pb-2">
            <AppButton variant="primary" size="lg" class="w-full flex items-center justify-center gap-2.5 py-3.5 text-sm sm:text-base font-black uppercase tracking-wider rounded-xl shadow-lg shadow-primary/20 hover:brightness-110 active:scale-[0.99] transition-all cursor-pointer" @click="resetPOS">
              <span class="material-symbols-outlined text-lg">add_circle</span> Giao dịch mới
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
              <div class="flex items-center gap-4">
                <span class="text-[11px] font-bold text-on-surface-variant uppercase tracking-widest hidden sm:inline">Khách vãng lai · Không cần vé</span>
              </div>
            </div>

            <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3.5">
              <div v-for="i in 6" :key="i" class="h-28 bg-surface-container-high rounded-3xl animate-pulse"></div>
            </div>
            <div v-else-if="combos.length === 0" class="flex-grow flex items-center justify-center text-on-surface-variant text-sm">
              Chưa có món F&B. Thêm ở "Thực đơn F&B / Combo".
            </div>
            <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3.5 flex-grow overflow-y-auto custom-scrollbar pr-2 content-start pb-6">
              <div v-for="cb in combos" :key="cb.id" 
                   @click="handleFnbCardClick(cb)"
                   :class="fnbQtyOf(cb.id) ? 'bg-primary/10 border-primary shadow-lg shadow-primary/5 cursor-pointer' : 'bg-surface-container-low/60 border-outline-variant/30 hover:border-primary/50 cursor-pointer'"
                   class="min-h-[150px] max-h-[200px] p-3.5 rounded-xl border flex flex-col justify-between transition-all duration-200">

                <template v-if="fnbQtyOf(cb.id)">
                  <!-- State 2: Selected — mỗi bộ vị là 1 dòng có stepper riêng -->
                  <div class="flex items-start gap-3 flex-grow min-h-0">
                    <div class="w-14 h-14 rounded-2xl overflow-hidden bg-surface-container-highest shrink-0 flex items-center justify-center">
                      <img v-if="cb.imageUrl" :src="cb.imageUrl" class="w-full h-full object-cover" />
                      <span v-else class="material-symbols-outlined text-on-surface-variant/40">fastfood</span>
                    </div>
                    <div class="flex-grow min-w-0 flex flex-col h-full">
                      <div class="flex justify-between items-start gap-2">
                          <h3 class="text-base font-bold text-on-surface truncate" :title="cb.name">{{ cb.name }}</h3>
                          <span class="text-base font-semibold text-primary shrink-0 leading-none mt-1">{{ fmt(fnbLineTotal(cb.id)) }}đ</span>
                      </div>
                      <div class="mt-1.5 flex-1 overflow-y-auto custom-scrollbar pr-1 space-y-1.5">
                        <div v-for="line in fnbLinesOf(cb.id)" :key="optionsKey(line.options)" class="flex items-center justify-between gap-2">
                          <div class="min-w-0 text-[11px] text-on-surface leading-tight truncate">
                            <template v-if="line.options && line.options.length">
                              <span v-for="(opt, oi) in line.options" :key="opt.optionItemId"><span v-if="oi">, </span>{{ opt.optionName }}<span v-if="opt.surchargePrice > 0" class="text-amber-400 font-medium"> (+{{ fmt(opt.surchargePrice) }}đ)</span></span>
                            </template>
                            <span v-else class="text-on-surface-variant/60">Mặc định</span>
                          </div>
                          <div class="flex items-center gap-1 shrink-0">
                            <button v-if="cb.slots && cb.slots.length" @click.stop="editFnbOptions(line, selectedCombos.indexOf(line))" class="w-5 h-5 rounded-full text-on-surface-variant/60 flex items-center justify-center hover:text-primary transition-colors" title="Đổi vị">
                              <span class="material-symbols-outlined text-[13px]">edit</span>
                            </button>
                            <div class="flex items-center gap-1.5 bg-surface-container-highest rounded-full p-0.5 border border-outline-variant/20">
                              <button @click.stop="changeComboQty(line, -1)" class="w-5 h-5 rounded-full bg-surface-container-lowest text-on-surface flex items-center justify-center hover:bg-primary hover:text-black transition-colors">
                                <span class="material-symbols-outlined text-[13px]">remove</span>
                              </button>
                              <span class="w-4 text-center text-[11px] font-black tabular-nums">{{ line.quantity }}</span>
                              <button @click.stop="changeComboQty(line, 1)" class="w-5 h-5 rounded-full bg-primary/20 text-primary flex items-center justify-center hover:bg-primary hover:text-black transition-colors">
                                <span class="material-symbols-outlined text-[13px]">add</span>
                              </button>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div v-if="cb.slots && cb.slots.length" class="flex items-center justify-end mt-2 shrink-0">
                    <button @click.stop="openFnbModal(cb)" class="flex items-center gap-1 text-[11px] font-bold text-primary hover:text-primary/80 transition-colors px-2 py-1 rounded-full hover:bg-primary/10">
                      <span class="material-symbols-outlined text-[15px]">add</span> Thêm vị khác
                    </button>
                  </div>
                </template>

                <template v-else>
                  <!-- State 1: Unselected -->
                  <div class="flex items-start gap-3 flex-grow min-h-0">
                    <div class="w-14 h-14 rounded-2xl overflow-hidden bg-surface-container-highest shrink-0 flex items-center justify-center">
                      <img v-if="cb.imageUrl" :src="cb.imageUrl" class="w-full h-full object-cover" />
                      <span v-else class="material-symbols-outlined text-on-surface-variant/40">fastfood</span>
                    </div>
                    <div class="flex-grow min-w-0 flex flex-col h-full">
                      <div class="flex justify-between items-start gap-2">
                          <h3 class="text-base font-bold text-on-surface truncate" :title="cb.name">{{ cb.name }}</h3>
                          <span class="text-base font-semibold text-primary shrink-0 leading-none mt-1">{{ fmt(cb.price) }}đ</span>
                      </div>
                      <p class="text-[11px] text-on-surface-variant line-clamp-2 mt-1.5 flex-1" :title="cb.description">{{ cb.description }}</p>
                    </div>
                  </div>
                  <div class="flex items-end justify-end mt-2 shrink-0">
                    <button @click.stop="addCombo(cb)" class="w-8 h-8 rounded-full bg-primary/10 text-primary flex items-center justify-center hover:bg-primary hover:text-black transition-colors">
                      <span class="material-symbols-outlined text-[18px]">add</span>
                    </button>
                  </div>
                </template>

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
                <div v-for="(c, ci) in selectedCombos" :key="ci" class="border-b border-outline-variant/10 pb-3">
                  <div class="flex justify-between text-xs font-bold text-on-surface-variant uppercase">
                    <span>{{ c.name }} <span class="text-on-surface-variant/60">x{{ c.quantity }}</span></span>
                    <span class="text-on-surface">{{ fmt(((c.snapshotPrice ?? c.price) + (c.surchargePrice || 0)) * c.quantity) }}đ</span>
                  </div>
                  <div v-if="c.options && c.options.length" class="text-[10px] text-on-surface mt-1.5 space-y-1.5 leading-normal ml-2">
                    <div v-for="opt in c.options" :key="opt.optionItemId">
                      • {{ opt.optionName }} <span v-if="opt.surchargePrice > 0" class="text-amber-400 font-medium">(+{{ fmt(opt.surchargePrice) }}đ)</span>
                    </div>
                  </div>
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
                  <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="openCashModal" :disabled="isPaying || qrHoldLoading">
                    <span class="material-symbols-outlined">payments</span> Tiền mặt
                  </AppButton>
                  <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="openQrModal" :disabled="isPaying || qrHoldLoading">
                    <span v-if="qrHoldLoading" class="material-symbols-outlined animate-spin">progress_activity</span>
                    <span v-else class="material-symbols-outlined">qr_code_2</span>
                    {{ qrHoldLoading ? 'Đang tạo đơn...' : 'Chuyển khoản QR' }}
                  </AppButton>
                </div>
                <p v-if="isPaying" class="text-center text-xs text-on-surface-variant">Đang xử lý thanh toán...</p>
              </div>
            </div>
          </div>

          <!-- FNB Step 3: Done -->
          <div v-if="fnbStep === 3" class="p-4 sm:p-6 flex flex-col items-center justify-start text-center h-full space-y-4 sm:space-y-5 overflow-y-auto custom-scrollbar">
            <div class="w-14 h-14 sm:w-16 sm:h-16 bg-green-500/15 text-green-400 border border-green-500/30 rounded-full flex items-center justify-center shadow-lg shadow-green-500/10 shrink-0 mt-1">
              <span class="material-symbols-outlined text-3xl sm:text-4xl">check_circle</span>
            </div>
            <div class="space-y-0.5">
              <h2 class="text-2xl sm:text-3xl font-black uppercase italic tracking-tighter text-on-surface">Thanh toán thành công</h2>
              <p class="text-on-surface-variant font-bold uppercase tracking-widest text-[11px]">Giao bắp nước cho khách</p>
            </div>

            <div class="bg-surface-container-high/90 p-5 sm:p-6 rounded-2xl sm:rounded-3xl border border-outline-variant/15 shadow-xl w-full max-w-2xl space-y-4 text-left">
              <!-- Header: Mã đơn + Loại đơn -->
              <div class="flex justify-between items-start border-b border-outline-variant/10 pb-3">
                <div>
                  <p class="text-[10px] font-black text-primary uppercase tracking-wider">Mã hoá đơn</p>
                  <p class="text-lg sm:text-xl font-black font-mono text-primary tracking-wide">{{ concessionSale?.saleCode }}</p>
                </div>
                <div class="text-right">
                  <p class="text-[10px] font-black text-primary uppercase tracking-wider">Hình thức</p>
                  <p class="text-sm sm:text-base font-black text-on-surface">Bán nhanh F&B</p>
                </div>
              </div>

              <!-- Danh sách món F&B -->
              <div v-if="selectedCombos.length" class="space-y-1.5">
                <p class="text-[10px] font-black text-on-surface-variant uppercase tracking-wider">
                  Chi tiết món đã chọn ({{ selectedCombos.reduce((a, c) => a + c.quantity, 0) }} phần)
                </p>
                <div v-for="(c, ci) in selectedCombos" :key="ci" class="text-xs text-on-surface flex justify-between items-start border-b border-outline-variant/5 pb-1.5">
                  <div>
                    <span class="font-bold">{{ c.name }}</span> <span class="text-on-surface-variant">x{{ c.quantity }}</span>
                    <div v-if="c.options && c.options.length" class="text-[10px] text-on-surface-variant mt-0.5 ml-2">
                      <span v-for="(opt, oi) in c.options" :key="opt.optionItemId">
                        <span v-if="oi">, </span>{{ opt.optionName }}<span v-if="opt.surchargePrice > 0" class="text-amber-400 font-medium"> (+{{ fmt(opt.surchargePrice) }}đ)</span>
                      </span>
                    </div>
                  </div>
                  <span class="font-bold text-on-surface">{{ fmt(((c.snapshotPrice ?? c.price) + (c.surchargePrice || 0)) * c.quantity) }}đ</span>
                </div>
              </div>

              <!-- Tóm tắt thanh toán -->
              <div class="border-t border-dashed border-outline-variant/20 pt-3 space-y-2">
                <div class="flex justify-between items-center text-xs text-on-surface-variant">
                  <span>Hình thức thanh toán</span>
                  <span class="font-bold text-on-surface">
                    {{ paymentMethod === 'CASH' ? 'Tiền mặt' : 'Chuyển khoản QR (VietQR)' }}
                  </span>
                </div>
                <div v-if="paymentMethod === 'CASH' && cashGiven > 0" class="flex justify-between items-center text-xs text-on-surface-variant">
                  <span>Tiền khách đưa / Trả lại</span>
                  <span>Khách đưa: <b class="text-on-surface">{{ fmt(cashGiven) }}đ</b> · Thối lại: <b class="text-green-400">{{ fmt(changeDue) }}đ</b></span>
                </div>
                <div v-if="member" class="flex justify-between items-center text-xs text-on-surface-variant">
                  <span>Thành viên tích điểm</span>
                  <span class="font-bold text-primary">{{ member.fullName }} ({{ member.membershipTier }})</span>
                </div>
                <div class="flex justify-between items-center pt-2.5 border-t border-outline-variant/10">
                  <span class="text-xs font-black text-primary uppercase tracking-wider">Tổng thanh toán</span>
                  <span class="text-xl sm:text-2xl font-black text-primary italic tracking-tight">{{ fmt(comboTotal) }}đ</span>
                </div>
              </div>
            </div>

            <div class="flex justify-center w-full max-w-2xl">
              <AppButton variant="primary" size="lg" class="w-full flex items-center justify-center gap-2.5 py-3.5 text-sm sm:text-base font-black uppercase tracking-wider rounded-xl shadow-lg shadow-primary/20 hover:brightness-110 active:scale-[0.99] transition-all cursor-pointer" @click="newConcessionSale">
                <span class="material-symbols-outlined text-lg">add_circle</span> Giao dịch mới
              </AppButton>
            </div>

            <!-- Bấm nhầm? Yêu cầu Trưởng ca duyệt HỦY hóa đơn (nhân viên quầy không tự hủy được) -->
            <div class="w-full max-w-2xl">
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

      <!-- Right: Cart summary (Desktop >= lg) -->
      <div class="hidden lg:flex lg:col-span-4 xl:col-span-3 bg-surface-container-low border border-outline-variant/10 rounded-3xl shadow-2xl p-5 sm:p-6 flex-col min-h-0 overflow-hidden">
        <div class="flex items-center gap-2 pb-4 mb-4 border-b border-outline-variant/10">
          <span class="material-symbols-outlined text-primary">receipt_long</span>
          <h2 class="text-sm font-black uppercase tracking-[0.2em] text-primary">Biên lai tạm tính</h2>
        </div>
        <div v-if="(selectedShowtime || selectedCombos.length) && currentStep !== 6 && fnbStep !== 3" class="space-y-4 flex-grow overflow-y-auto custom-scrollbar pr-1">
          <div v-if="saleMode === 'FNB'" class="pb-4 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-primary uppercase tracking-wider mb-1">Bán nhanh F&B</p>
            <h3 class="text-sm sm:text-base font-black uppercase italic text-on-surface leading-tight">Bắp nước & Combo</h3>
            <p class="text-xs text-on-surface-variant mt-1">Khách vãng lai · không kèm vé</p>
          </div>
          <div v-if="selectedShowtime" class="pb-4 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-1">Phim & Suất</p>
            <h3 class="text-sm sm:text-base font-black uppercase italic text-on-surface leading-tight">{{ selectedShowtime.movieTitle }}</h3>
            <p class="text-xs text-on-surface-variant mt-1">{{ selectedShowtime.roomName }} • {{ selectedShowtime.formatName }}</p>
            <p class="text-xs text-on-surface-variant">{{ new Date(selectedShowtime.startTime).toLocaleString('vi-VN', { weekday: 'short', hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }) }}</p>
          </div>
          <div v-if="selectedSeats.length" class="pb-4 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-1.5">Ghế ({{ selectedSeats.length }} ghế / {{ totalRequiredTickets }} vé)</p>
            <p class="text-sm text-primary font-black mb-2.5">{{ selectedSeats.map(s => seatLabel(s)).join(', ') }}</p>
            <div v-for="b in seatTypeBreakdown" :key="b.type" class="flex justify-between text-xs font-semibold text-on-surface-variant mb-1">
              <span>Ghế {{ seatTypeLabel(b.type) }} <span class="text-on-surface-variant/60">x{{ b.count }}</span></span>
              <span class="text-on-surface">{{ fmt(b.subtotal) }}đ</span>
            </div>
          </div>
          <div v-if="selectedCombos.length" class="pb-4 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-1.5">F&B / Combo</p>
            <div v-for="(c, ci) in selectedCombos" :key="ci" class="mb-2">
              <div class="flex justify-between text-xs font-semibold">
                <span class="text-on-surface-variant">{{ c.name }} <span class="text-on-surface-variant/60">x{{ c.quantity }}</span></span>
                <span class="text-on-surface">{{ fmt((c.price + (c.surchargePrice || 0)) * c.quantity) }}đ</span>
              </div>
              <div v-if="c.options && c.options.length" class="text-[10px] text-on-surface mt-1 space-y-1 leading-normal ml-2">
                <div v-for="opt in c.options" :key="opt.optionItemId">
                  • {{ opt.optionName }} <span v-if="opt.surchargePrice > 0" class="text-amber-400 font-medium">(+{{ fmt(opt.surchargePrice) }}đ)</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="flex-grow flex flex-col items-center justify-center text-on-surface-variant/40 gap-2">
          <span class="material-symbols-outlined text-4xl">shopping_cart</span>
          <p class="text-xs font-semibold">{{ (currentStep === 6 || fnbStep === 3) ? 'Chưa có giao dịch mới' : (saleMode === 'FNB' ? 'Chưa chọn món nào' : 'Chưa chọn suất chiếu') }}</p>
        </div>

        <div v-if="discountAmount > 0 && currentStep !== 6 && fnbStep !== 3" class="pt-3 mt-2 flex justify-between items-center text-xs font-bold text-green-400">
          <span class="uppercase tracking-wider">Giảm giá {{ appliedVoucher ? '(' + appliedVoucher.code + ')' : '' }}</span>
          <span>-{{ fmt(discountAmount) }}đ</span>
        </div>
        <div class="pt-4 mt-2 border-t border-outline-variant/10 flex justify-between items-center">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Tổng tiền</p>
          <p class="text-2xl sm:text-3xl font-black italic tracking-tighter text-primary">{{ fmt((currentStep === 6 || fnbStep === 3) ? 0 : payableTotal) }}đ</p>
        </div>
        
        <button v-if="(saleMode === 'FNB' || currentStep === 4) && currentStep !== 6 && fnbStep !== 3"
          class="w-full py-3 mt-3 bg-primary text-on-primary font-bold text-sm sm:text-base rounded-xl shadow-lg hover:brightness-110 active:scale-[0.98] transition-all disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none"
          :disabled="totalPrice === 0"
          @click="saleMode === 'FNB' ? fnbStep = 2 : currentStep = 5">
          THANH TOÁN ({{ fmt(payableTotal) }}đ)
        </button>
      </div>
    </main>

    <!-- Mobile/Tablet Bottom Floating Bar (< lg) -->
    <div v-if="(selectedShowtime || selectedCombos.length > 0) && currentStep !== 6 && fnbStep !== 3"
         class="lg:hidden fixed bottom-0 left-0 right-0 z-30 bg-surface/95 backdrop-blur-md border-t border-outline-variant/20 px-4 py-3 shadow-[0_-4px_25px_rgba(0,0,0,0.4)] flex items-center justify-between gap-3">
      <div class="cursor-pointer" @click="showMobileReceiptDrawer = true">
        <p class="text-[10px] uppercase font-bold tracking-wider text-on-surface-variant flex items-center gap-1">
          Biên lai tạm tính <span class="material-symbols-outlined text-xs">expand_less</span>
        </p>
        <p class="text-xl font-black italic text-primary tracking-tight leading-none mt-0.5">{{ fmt(payableTotal) }}đ</p>
      </div>
      
      <div class="flex items-center gap-2">
        <button @click="showMobileReceiptDrawer = true"
                class="px-3 py-2 rounded-xl bg-surface-container-high border border-outline-variant/20 text-xs font-bold text-on-surface flex items-center gap-1 hover:bg-white/5 transition-colors">
          <span class="material-symbols-outlined text-base">receipt_long</span>
          Chi tiết
        </button>
        <button v-if="saleMode === 'FNB' || currentStep === 4"
                :disabled="totalPrice === 0"
                @click="saleMode === 'FNB' ? fnbStep = 2 : currentStep = 5"
                class="px-4 py-2 rounded-xl bg-primary text-on-primary font-black text-xs uppercase tracking-wider shadow-lg shadow-primary/20 hover:brightness-110 active:scale-95 transition-all disabled:opacity-50">
          Thanh toán
        </button>
      </div>
    </div>

    <!-- Mobile Receipt Bottom Sheet Drawer (< lg) -->
    <transition name="fade">
      <div v-if="showMobileReceiptDrawer"
           class="lg:hidden fixed inset-0 z-50 flex flex-col justify-end bg-black/70 backdrop-blur-sm"
           @click.self="showMobileReceiptDrawer = false">
        <div class="w-full bg-surface-container-low border-t border-outline-variant/20 rounded-t-3xl shadow-2xl max-h-[85vh] flex flex-col p-5 sm:p-6 overflow-hidden">
          <div class="flex justify-between items-center pb-4 border-b border-outline-variant/10">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">receipt_long</span>
              <h3 class="text-sm font-black uppercase tracking-[0.2em] text-primary">Biên lai tạm tính</h3>
            </div>
            <button @click="showMobileReceiptDrawer = false" class="p-1 rounded-full text-on-surface-variant hover:text-on-surface">
              <span class="material-symbols-outlined text-xl">close</span>
            </button>
          </div>
          
          <div v-if="selectedShowtime || selectedCombos.length" class="space-y-4 flex-grow overflow-y-auto custom-scrollbar py-4 pr-1">
            <div v-if="saleMode === 'FNB'" class="pb-4 border-b border-outline-variant/10">
              <p class="text-[10px] font-bold text-primary uppercase tracking-wider mb-1">Bán nhanh F&B</p>
              <h4 class="text-base font-black uppercase italic text-on-surface leading-tight">Bắp nước & Combo</h4>
              <p class="text-xs text-on-surface-variant mt-1">Khách vãng lai · không kèm vé</p>
            </div>
            <div v-if="selectedShowtime" class="pb-4 border-b border-outline-variant/10">
              <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-1">Phim & Suất</p>
              <h4 class="text-base font-black uppercase italic text-on-surface leading-tight">{{ selectedShowtime.movieTitle }}</h4>
              <p class="text-xs text-on-surface-variant mt-1">{{ selectedShowtime.roomName }} • {{ selectedShowtime.formatName }}</p>
              <p class="text-xs text-on-surface-variant">{{ new Date(selectedShowtime.startTime).toLocaleString('vi-VN', { weekday: 'short', hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }) }}</p>
            </div>
            <div v-if="selectedSeats.length" class="pb-4 border-b border-outline-variant/10">
              <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-1.5">Ghế ({{ selectedSeats.length }} ghế / {{ totalRequiredTickets }} vé)</p>
              <p class="text-sm text-primary font-black mb-2">{{ selectedSeats.map(s => seatLabel(s)).join(', ') }}</p>
              <div v-for="b in seatTypeBreakdown" :key="b.type" class="flex justify-between text-xs font-semibold text-on-surface-variant mb-1">
                <span>Ghế {{ seatTypeLabel(b.type) }} <span class="text-on-surface-variant/60">x{{ b.count }}</span></span>
                <span class="text-on-surface">{{ fmt(b.subtotal) }}đ</span>
              </div>
            </div>
            <div v-if="selectedCombos.length" class="pb-4 border-b border-outline-variant/10">
              <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-1.5">F&B / Combo</p>
              <div v-for="(c, ci) in selectedCombos" :key="ci" class="mb-2">
                <div class="flex justify-between text-xs font-semibold">
                  <span class="text-on-surface-variant">{{ c.name }} <span class="text-on-surface-variant/60">x{{ c.quantity }}</span></span>
                  <span class="text-on-surface">{{ fmt(((c.snapshotPrice ?? c.price) + (c.surchargePrice || 0)) * c.quantity) }}đ</span>
                </div>
                <div v-if="c.options && c.options.length" class="text-[10px] text-on-surface mt-1 space-y-1 leading-normal ml-2">
                  <div v-for="opt in c.options" :key="opt.optionItemId">
                    • {{ opt.optionName }} <span v-if="opt.surchargePrice > 0" class="text-amber-400 font-medium">(+{{ fmt(opt.surchargePrice) }}đ)</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="pt-4 border-t border-outline-variant/10 space-y-3">
            <div v-if="discountAmount > 0" class="flex justify-between items-center text-xs font-bold text-green-400">
              <span class="uppercase tracking-wider">Giảm giá {{ appliedVoucher ? '(' + appliedVoucher.code + ')' : '' }}</span>
              <span>-{{ fmt(discountAmount) }}đ</span>
            </div>
            <div class="flex justify-between items-center">
              <p class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Tổng tiền</p>
              <p class="text-2xl font-black italic tracking-tighter text-primary">{{ fmt(payableTotal) }}đ</p>
            </div>
            <button v-if="saleMode === 'FNB' || currentStep === 4"
              class="w-full py-3.5 bg-primary text-on-primary font-bold text-base rounded-xl shadow-lg hover:brightness-110 active:scale-[0.98] transition-all disabled:opacity-50"
              :disabled="totalPrice === 0"
              @click="showMobileReceiptDrawer = false; saleMode === 'FNB' ? fnbStep = 2 : currentStep = 5">
              TIẾP TỤC THANH TOÁN ({{ fmt(payableTotal) }}đ)
            </button>
          </div>
        </div>
      </div>
    </transition>

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
            <button v-if="canHoldOrder" @click.stop.prevent="holdCurrentOrder" :disabled="isPaying || isHolding"
                    class="flex items-center justify-center gap-1.5 px-4 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-[11px] font-black uppercase tracking-wider hover:bg-amber-500/20 transition-all disabled:opacity-50 disabled:cursor-not-allowed" title="Lưu đơn vào danh sách chờ (giữ số tiền gốc)">
              <span class="material-symbols-outlined text-base" :class="{'animate-spin': isHolding}">{{ isHolding ? 'progress_activity' : 'pause_circle' }}</span> {{ isHolding ? 'Đang xử lý...' : 'Giữ đơn' }}
            </button>
            <AppButton variant="primary" class="flex-1 disabled:opacity-50 disabled:cursor-not-allowed transition-all" :disabled="!canConfirmCash || isPaying || isHolding" @click.stop.prevent="processPayment('CASH')">
              <span v-if="isPaying" class="material-symbols-outlined text-base animate-spin mr-1 align-middle">progress_activity</span>
              {{ isPaying ? 'Đang xử lý...' : 'Xác nhận thanh toán' }}
            </AppButton>
          </div>
        </div>
      </div>
    </transition>

    <!-- Modal: Chuyển khoản QR (VietQR) -->
    <transition name="fade">
      <div v-if="showQrModal" class="fixed inset-0 z-[1200] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4" @click.self="closeQrModal">
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
              <div class="flex justify-between"><span class="text-on-surface-variant">Số tiền</span><span class="font-black text-primary italic">{{ fmt(payableTotal) }}đ</span></div>
              <div class="flex justify-between"><span class="text-on-surface-variant">Nội dung</span><span class="font-bold text-on-surface">{{ transferContent }}</span></div>
            </div>
          </div>

          <div class="px-7 py-5 border-t border-outline-variant/10 flex gap-3">
            <AppButton variant="ghost" class="flex-1" @click="closeQrModal" :disabled="isPaying">Hủy</AppButton>
            <AppButton variant="primary" class="flex-1 disabled:opacity-50 disabled:cursor-not-allowed transition-all" :disabled="!cleanQrUrl || isPaying || isHolding" @click.stop.prevent="processPayment('TRANSFER')">
              <span v-if="isPaying" class="material-symbols-outlined text-base animate-spin mr-1 align-middle">progress_activity</span>
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
                <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest mt-1">{{ posStore.heldOrders.length }} đơn đang giữ</p>
              </div>
            </div>
            <button @click="showHeldPanel = false" class="w-8 h-8 rounded-lg hover:bg-surface-container-high flex items-center justify-center text-on-surface-variant">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div v-if="posStore.heldOrders.length === 0" class="flex-grow flex flex-col items-center justify-center text-on-surface-variant/40 gap-3 px-8 text-center">
            <span class="material-symbols-outlined text-5xl">inbox</span>
            <p class="text-sm font-semibold">Chưa có đơn nào đang chờ.</p>
            <p class="text-xs">Bấm <b class="text-primary">Giữ đơn</b> khi cần xử lý khách tiếp theo trong hàng đợi.</p>
          </div>

          <div v-else class="flex-grow overflow-y-auto custom-scrollbar p-4 space-y-3">
            <div v-for="o in posStore.heldOrders" :key="o.code"
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
                  <span class="text-xs font-bold text-primary">{{ (o.seats || []).map(s => seatLabel(s)).join(', ') }}</span>
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
                  <button @click="askRestoreHeldOrder(o)"
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

    <!-- Modal: Xác nhận đè giỏ hàng dở dang -->
    <transition name="fade">
      <div v-if="confirmRestoreHold" class="fixed inset-0 z-[1250] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4" @click.self="cancelRestoreHeldOrder">
        <div class="w-full max-w-sm bg-surface border border-outline-variant/15 rounded-3xl shadow-2xl overflow-hidden">
          <div class="p-7 text-center space-y-4">
            <div class="w-16 h-16 mx-auto rounded-full bg-amber-500/15 border border-amber-500/30 flex items-center justify-center">
              <span class="material-symbols-outlined text-3xl text-amber-400">warning</span>
            </div>
            <div>
              <h3 class="text-lg font-black uppercase italic tracking-tighter text-on-surface">Đè giỏ hàng hiện tại?</h3>
              <p class="text-sm text-on-surface-variant mt-2">Giỏ hàng bạn đang thao tác sẽ bị xoá. Hãy <b class="text-amber-400">[Giữ đơn]</b> hiện tại trước nếu cần. Tiếp tục gọi lại đơn?</p>
            </div>
          </div>
          <div class="px-7 py-5 border-t border-outline-variant/10 flex gap-3">
            <AppButton variant="ghost" class="flex-1" @click="cancelRestoreHeldOrder">Quay lại</AppButton>
            <button @click="confirmRestoreAction" class="flex-1 px-4 py-2 rounded-xl bg-amber-500 text-black text-sm font-bold shadow hover:brightness-110 transition-all">
              Đồng ý
            </button>
          </div>
        </div>
      </div>
    </transition>

      <FnbOptionModal
        :is-open="isFnbModalOpen"
        :fnb-item="editingFnbItem"
        :initial-options="initialFnbOptions"
        @close="isFnbModalOpen = false"
        @confirm="handleFnbOptionsConfirm"
      />
  </div>

    <!-- Modal: Out of Stock -->
    <transition name="fade">
      <div v-if="showOutOfStockModal" class="fixed inset-0 z-[1250] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4" @click.self="showOutOfStockModal = false">
        <div class="w-full max-w-sm bg-surface border border-outline-variant/15 rounded-3xl shadow-2xl overflow-hidden">
          <div class="p-7 text-center space-y-4">
            <div class="w-16 h-16 mx-auto rounded-full bg-red-500/15 border border-red-500/30 flex items-center justify-center">
              <span class="material-symbols-outlined text-4xl text-red-500">inventory_2</span>
            </div>
            <div>
              <h3 class="text-lg font-black uppercase italic tracking-tighter text-on-surface">Sản phẩm không khả dụng</h3>
              <p class="text-sm text-on-surface-variant mt-2 whitespace-pre-line">{{ outOfStockMessage }}</p>
            </div>
          </div>
          <div class="px-7 py-5 border-t border-outline-variant/10 flex gap-3">
            <button @click="showOutOfStockModal = false" class="flex-1 px-4 py-3 rounded-xl bg-surface-container-highest text-on-surface text-sm font-bold shadow hover:brightness-110 transition-all">
              Đóng và Sửa giỏ hàng
            </button>
          </div>
        </div>
      </div>
    </transition>

</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; height: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(245, 197, 24, 0.2); border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: rgba(245, 197, 24, 0.4); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(10px); }
</style>
