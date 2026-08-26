<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { ticketingApi, settingsApi, approvalApi, bookingAdminApi, posPendingOrderApi } from '@/api/admin/index'
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

const DEFAULT_AUDIENCE_LABELS = { ADULT: 'Người lớn', U22: 'U22 / HSSV', CHILD: 'Trẻ em', SENIOR: 'Cao tuổi' }
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

// Dựng lại counts: mặc định toàn bộ ghế chuyển thành ADULT khi vào bước xác nhận
const syncTicketCountsFromSeats = () => {
  const counts = {}
  Object.keys(audienceLabels.value).forEach(k => { counts[k] = 0 })
  counts.ADULT = selectedSeats.value.length
  ticketCounts.value = counts
  assignTicketCountsToSeats()
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
  const maxSeats = selectedSeats.value.length
  const cur = Number(ticketCounts.value[code] || 0)

  if (delta < 0) {
    const next = Math.max(0, cur + delta)
    ticketCounts.value = { ...ticketCounts.value, [code]: next }
  } else if (delta > 0) {
    const totalAssigned = totalTicketCount.value

    if (totalAssigned < maxSeats) {
      ticketCounts.value = { ...ticketCounts.value, [code]: cur + delta }
    } else if (totalAssigned === maxSeats && cur < maxSeats) {
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

// Vào bước 3 (xác nhận vé) → đồng bộ counter với các ghế đang chọn
watch(currentStep, (step) => { if (step === 3) syncTicketCountsFromSeats() })

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
const HOLD_SECONDS = 5 * 60       // 5 phút giữ ghế
const holdRemaining = ref(0)      // giây còn lại
let holdTimer = null
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

const startSeatPolling = () => {
  if (selectedShowtime.value) seatRealtime.connect(selectedShowtime.value.id) // kết nối WebSocket
}
const stopSeatPolling = () => {
  seatRealtime.disconnect() // rời bước chọn ghế → nhả khóa của mình + ngắt WebSocket
}

import { usePosStore } from '@/stores/usePosStore'
const posStore = usePosStore()

// ===== Khóa ghế real-time (WebSocket/STOMP) — đồng bộ với quầy POS khác & khách online =====
const seatRealtime = useSeatRealtime({
  by: 'Quầy POS',
  // Ghế mình vừa chọn nhưng quầy khác đã giành trước → gỡ khỏi đơn + báo lỗi
  onDenied: (seatId) => {
    const lost = selectedSeats.value.find(s => s.seatId === seatId)
    selectedSeats.value = selectedSeats.value.filter(s => s.seatId !== seatId)
    if (selectedSeats.value.length === 0) stopHoldTimer()
    const label = lost ? seatLabel(lost) : 'này'
    showToast(`Ghế ${label} vừa được chọn hoặc đã được bán ở quầy khác. Vui lòng chọn vị trí ghế khác!`, 'error')
  },
  // Ghế bị bán ở nơi khác → đánh dấu SOLD trực tiếp + gỡ khỏi đơn nếu đang chọn
  onSold: (seatIds) => {
    applySeatStatusUpdate(seatIds, 'SOLD')
    const lost = selectedSeats.value.filter(s => seatIds.includes(s.seatId))
    if (lost.length) {
      selectedSeats.value = selectedSeats.value.filter(s => !seatIds.includes(s.seatId))
      if (selectedSeats.value.length === 0) stopHoldTimer()
      showToast(`Ghế ${lost.map(s => seatLabel(s)).join(', ')} vừa được bán ở quầy khác — đã gỡ khỏi đơn.`, 'error')
    }
  },
  // Ghế vừa được nhả (hết hạn giữ chỗ / huỷ đơn) → cập nhật AVAILABLE ngay lập tức
  onReleased: (seatIds) => {
    applySeatStatusUpdate(seatIds, 'AVAILABLE')
    // Gỡ khỏi danh sách bị chặn (nếu có)
    const recovered = seatIds.filter(id => selectedSeats.value.some(s => s.seatId === id))
    if (recovered.length > 0) {
      showToast(`${recovered.length} ghế vừa được nhả lại — có thể chọn thêm.`, 'info')
    }
  },
  // Ghế vừa bị giữ bởi quầy/khách khác → cập nhật HOLD ngay lập tức
  onHeld: (seatIds) => {
    applySeatStatusUpdate(seatIds, 'HOLD')
  },
})
const isSeatLockedByOthers = (seat) => !!seat && seatRealtime.isLockedByOthers(seat.seatId)

// ===== Hoá đơn chờ (Hold Order / Pending List) — dùng Pinia Store =====
const showHeldPanel = ref(false)
const canHoldOrder = computed(() => {
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
        customerId: member.value ? member.value.customerId : null,
        fnbs: selectedCombos.value.map(c => ({ fnbItemId: c.id, quantity: c.quantity, options: c.options || [] }))
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
        if (item && (item.is_available || item.isAvailable)) {
          validCombos.push(combo);
        } else {
          lostCombos.push(combo.name);
        }
      }
      if (lostCombos.length > 0) {
        showToast(`Món ${lostCombos.join(', ')} đã tạm hết và được xóa khỏi đơn giữ.`, 'error');
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
  member.value = o.member || null
  restoredBookingId.value = o.bookingId || null
  showHeldPanel.value = false
  currentStep.value = 2
  isLoadingSeats.value = true
  
  try {
    const { data } = await ticketingApi.getSeats(o.showtime.id)
    seatData.value = data.seats ? data : { matrixRow: 9, matrixCol: 10, seats: Array.isArray(data) ? data : [] }
    captureSeatMeta(data)
    
    const byId = new Map(seatData.value.seats.map(s => [s.seatId, s]))
    const restored = []; const lost = []
    for (const s of (o.seats || [])) {
      const cur = byId.get(s.seatId)
      // Check if it's still HOLD by us, or AVAILABLE
      if (cur && (cur.status === 'AVAILABLE' || cur.status === 'HOLD')) { cur.ticketType = s.ticketType || 'ADULT'; restored.push(cur) } else lost.push(s)
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

// Số giây còn lại của đơn chờ có vé (null = đơn F&B, không hết hạn)
const heldRemainingSec = (o) => {
  if (o.mode === 'FNB' || !(o.seats && o.seats.length)) return null
  return Math.max(0, HOLD_SECONDS - Math.floor((nowTs.value - o.createdAt) / 1000))
}
const heldCountdown = (o) => {
  const s = heldRemainingSec(o)
  if (s == null) return ''
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}
// Tự xoá đơn chờ hết giờ + nhả ghế được xử lý tập trung trong posStore.updateTimers()
// (chạy mỗi giây khi còn đơn giữ). Không lặp lại ở component để tránh lệch trạng thái.
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

const switchMode = (mode) => {
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
  } catch (err) {
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
      price: c.price,
      surchargePrice: c.surchargePrice || 0,
      lineTotal: (Number(c.price || 0) + Number(c.surchargePrice || 0)) * Number(c.quantity || 1),
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
const newConcessionSale = () => {
  selectedCombos.value = []
  member.value = null
  cardNumberInput.value = ''
  cardError.value = ''
  concessionSale.value = null
  fnbStep.value = 1
  resetVoidState()
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
const fnbLineTotal = (cbId) => fnbLinesOf(cbId).reduce((s, c) => s + (c.price + (c.surchargePrice || 0)) * c.quantity, 0)

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
  if (editingFnbIndex.value > -1) {
    // Đổi vị của ĐÚNG dòng đang chỉnh (ghi đè tại chỗ).
    const item = selectedCombos.value[editingFnbIndex.value]
    if (item) {
      item.options = options
      item.surchargePrice = totalSurcharge
    }
  } else {
    // Thêm bộ vị mới: trùng bộ vị đã có ⇒ +1 số lượng, khác ⇒ đẻ dòng mới.
    const cb = editingFnbItem.value
    const existingIndex = selectedCombos.value.findIndex(c => c.id === cb.id && isOptionsEqual(c.options, options))
    if (existingIndex > -1) {
      if (selectedCombos.value[existingIndex].quantity >= MAX_FNB_QTY) { showToast(`Tối đa ${MAX_FNB_QTY} phần/món.`, 'error') }
      else selectedCombos.value[existingIndex].quantity++
    } else {
      selectedCombos.value.push({ id: cb.id, name: cb.name, price: Number(cb.price), quantity: 1, options, surchargePrice: totalSurcharge })
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
  } else {
    selectedCombos.value.push({ id: cb.id, name: cb.name, price: Number(cb.price), quantity: 1, options: [], surchargePrice: 0 })
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
const comboTotal = computed(() => selectedCombos.value.reduce((a, c) => a + (c.price + (c.surchargePrice || 0)) * c.quantity, 0))
const totalPrice = computed(() => seatTotal.value + comboTotal.value)

// Giảm giá voucher (xem trước phía client; số chính thức do BE tính lại khi thanh toán)
const discountAmount = computed(() => {
  if (!appliedVoucher.value) return 0
  const v = appliedVoucher.value
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
    appliedVoucher.value = { id: data.id, code: data.code, discountType: data.discountType, discountValue: Number(data.discountValue || 0), maxDiscountAmount: Number(data.maxDiscountAmount || 0) }
    voucherCodeInput.value = data.code
    showToast(`Đã áp mã ${data.code}.`, 'success')
    loadOwnedVouchers()
  } catch (e) {
    appliedVoucher.value = null
    voucherError.value = friendlyError(e, 'Mã không hợp lệ hoặc không áp dụng được.')
  } finally {
    isApplyingVoucher.value = false
  }
}
const clearVoucher = () => { appliedVoucher.value = null; voucherCodeInput.value = ''; voucherError.value = '' }

// ===== Dropdown chọn voucher đã lưu của khách (custom UI thay <select> mặc định) =====
const showVoucherDropdown = ref(false)
const voucherDiscountLabel = (v) =>
  v.discountType === 'PERCENTAGE' ? `Giảm ${v.discountValue}%` : `Giảm ${fmt(v.discountValue)}đ`
const selectedOwnedVoucher = computed(() =>
  ownedVouchers.value.find(v => v.code === voucherCodeInput.value) || null)
const selectVoucher = (v) => { voucherCodeInput.value = v.code; showVoucherDropdown.value = false }

const processPayment = async (method) => {
  if (isHolding.value || isPaying.value) return
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
      seatSelections: selectedSeats.value.map(s => ({ seatId: s.seatId, ticketType: s.ticketType || 'ADULT' })),
      fnbs: selectedCombos.value.map(c => ({ fnbItemId: c.id, quantity: c.quantity, options: c.options || [] })),
      customerId: member.value ? member.value.customerId : null,
      voucherId: appliedVoucher.value ? appliedVoucher.value.id : null,
      paymentMethod: method,
      heldBookingId: restoredBookingId.value,
      allowOrphan: canOverrideOrphan.value && allowOrphan.value // chỉ ADMIN/MANAGER mới gửi cờ (BE cũng gate lại theo vai trò)
    }
    
    if (method === 'TRANSFER') {
      const holdRes = await ticketingApi.hold(payload)
      const bookingId = holdRes.data.bookingId
      await ticketingApi.mockWebhookSuccess(bookingId)
      const bookingRes = await bookingAdminApi.detail(bookingId)
      completedBooking.value = bookingRes.data.data ?? bookingRes.data
    } else {
      const { data } = await ticketingApi.pay(payload)
      completedBooking.value = data
    }
    
    stopHoldTimer()
    stopSeatPolling()
    showCashModal.value = false
    showQrModal.value = false
    currentStep.value = 6
  } catch (err) {
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
    seats: selectedSeats.value.map(s => ({
      seatLabel: seatLabel(s),
      ticketType: s.ticketType || 'ADULT',
      price: s.price || 0
    })),
    fnbs: selectedCombos.value.map(c => ({
      name: c.name,
      quantity: c.quantity,
      price: c.price,
      surchargePrice: c.surchargePrice || 0,
      lineTotal: (Number(c.price || 0) + Number(c.surchargePrice || 0)) * Number(c.quantity || 1),
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
  cashGiven.value = 0
  clearVoucherState()
  fetchData()
}

const handleGlobalKeydown = (e) => {
  if (isHolding.value || isPaying.value) return
  // Các phím tắt thu ngân có thể xử lý ở đây (ví dụ: F2, F4...)
}

const loadSettings = async () => {
  try {
    const { data } = await settingsApi.getAll()
    const list = data?.data || data || []
    const lateSetting = list.find(s => s.settingKey === 'BOOKING_LATE_MINUTES')
    if (lateSetting && !isNaN(Number(lateSetting.settingValue))) {
      lateBookingMinutes.value = Number(lateSetting.settingValue)
    }
  } catch (e) {
    // fallback 10 minutes
  }
}

onMounted(() => {
  nowTimer = setInterval(() => { nowTs.value = Date.now() }, 1000)
  fetchData(); loadBankInfo(); loadSettings();
  window.addEventListener('keydown', handleGlobalKeydown)
})
onUnmounted(() => {
  if (nowTimer) clearInterval(nowTimer)
  stopHoldTimer()
  stopSeatPolling()
  window.removeEventListener('keydown', handleGlobalKeydown)
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
                :class="(canHoldOrder && !isHolding) ? 'bg-amber-500/10 border-amber-500/30 text-amber-400 hover:bg-amber-500/20' : 'bg-surface-container-high border-outline-variant/10 text-on-surface-variant/40 cursor-not-allowed'"
                class="flex items-center gap-1.5 px-3.5 py-2 rounded-xl border text-[11px] font-black uppercase tracking-wider transition-all">
          <span class="material-symbols-outlined text-lg">{{ isHolding ? 'progress_activity' : 'pause_circle' }}</span> {{ isHolding ? 'ĐANG GIỮ...' : 'GIỮ ĐƠN' }}
        </button>

        <!-- Danh sách đơn chờ -->
        <AppButton variant="outline" class="w-12 h-12 !p-0 shrink-0 relative" @click="showHeldPanel = true" title="Danh sách đơn chờ">
          <span class="material-symbols-outlined">receipt_long</span>
          <span v-if="posStore.heldOrders.length > 0" class="absolute -top-2 -right-2 w-5 h-5 bg-primary text-on-primary font-bold text-xs rounded-full flex items-center justify-center border-2 border-surface shadow-sm">{{ posStore.heldOrders.length }}</span>
        </AppButton>

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

          <div v-if="isLoading" class="grid grid-cols-2 gap-6">
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

          <div v-else class="grid grid-cols-2 gap-6">
            <div v-for="movieGroup in groupedMoviesWithShowtimes" :key="movieGroup.movie.id"
                 class="relative p-6 bg-surface-container-high rounded-3xl border border-outline-variant/10 transition-all">
              <div class="flex gap-6">
                <!-- BÊN TRÁI: Ảnh Poster -->
                <div class="w-24 shrink-0 flex flex-col items-center">
                  <div class="w-24 h-36 bg-surface-container-highest rounded-xl overflow-hidden shadow-lg border border-outline-variant/10">
                    <img :src="getPoster(movieGroup.movie)" class="w-full h-full object-cover" />
                  </div>
                </div>
                
                <!-- BÊN PHẢI: Thông tin & Suất chiếu -->
                <div class="flex flex-col min-w-0 w-full">
                  <h3 class="font-black text-lg uppercase tracking-tight text-on-surface truncate">{{ movieGroup.movie.title }}</h3>
                  <div class="flex items-center gap-2 mt-1 mb-4">
                    <span class="text-[10px] font-bold text-on-surface-variant uppercase">{{ movieGroup.movie.durationMins || '???' }} PHÚT</span>
                  </div>
                  
                  <div class="flex flex-col gap-4">
                    <div v-for="roomGroup in movieGroup.roomGroups" :key="roomGroup.groupLabel">
                      <p class="text-[10px] font-black text-on-surface-variant/80 uppercase tracking-widest mb-2">{{ roomGroup.groupLabel }}</p>
                      <div class="flex flex-wrap gap-2">
                        <button v-for="st in roomGroup.showtimes" :key="st.id" @click="selectShowtime(st)" type="button"
                          class="px-4 py-2 bg-surface-container-highest hover:bg-primary/20 text-on-surface hover:text-primary transition-colors text-sm font-black italic rounded-xl border border-outline-variant/20 hover:border-primary/50 tabular-nums">
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
              Chọn số lượng vé theo đối tượng — tổng phải bằng số ghế đã chọn ({{ selectedSeats.length }} ghế).
            </p>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div v-for="(label, code) in audienceLabels" :key="code"
                   class="p-5 bg-surface-container-high rounded-[24px] border border-outline-variant/10 flex items-center justify-between gap-4">
                <span class="text-sm font-black text-on-surface uppercase">{{ label }}</span>
                <div class="flex items-center gap-3 shrink-0">
                  <button @click="setTicketCount(code, -1)" :disabled="(ticketCounts[code] || 0) <= 0"
                          class="w-9 h-9 flex items-center justify-center rounded-full bg-surface-container border border-outline-variant/10 disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer hover:text-primary transition-colors">
                    <span class="material-symbols-outlined text-base">remove</span>
                  </button>
                  <span class="w-7 text-center text-lg font-black tabular-nums text-on-surface">{{ ticketCounts[code] || 0 }}</span>
                  <button @click="setTicketCount(code, 1)" :disabled="(ticketCounts[code] || 0) >= selectedSeats.length"
                          class="w-9 h-9 flex items-center justify-center rounded-full bg-surface-container border border-outline-variant/10 disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer hover:text-primary transition-colors">
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
              Ghế đã chọn: <span class="text-primary">{{ selectedSeats.map(s => seatLabel(s)).join(', ') }}</span>
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
            <div class="flex items-center gap-3">
              <AppButton variant="ghost" @click="currentStep = 3">Quay lại</AppButton>
            </div>
          </div>

          <div v-if="combos.length === 0" class="flex-grow flex items-center justify-center text-on-surface-variant text-sm">
            Chưa có combo. Thêm ở "Thực đơn F&B / Combo".
          </div>
          <div v-else class="grid grid-cols-2 xl:grid-cols-3 gap-3.5 flex-grow overflow-y-auto custom-scrollbar pr-2 content-start pb-6">
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
                  <div v-if="ownedVouchers.length" class="relative">
                    <!-- Trigger -->
                    <button type="button" @click="showVoucherDropdown = !showVoucherDropdown"
                            class="w-full flex items-center justify-between gap-2 bg-surface-container border rounded-2xl py-3 px-4 text-sm font-bold text-on-surface outline-none transition-colors"
                            :class="showVoucherDropdown ? 'border-primary/50' : 'border-outline-variant/10 hover:border-primary/30'">
                      <span class="flex items-center gap-2.5 min-w-0">
                        <span class="material-symbols-outlined text-primary text-xl shrink-0">confirmation_number</span>
                        <span v-if="selectedOwnedVoucher" class="truncate">
                          {{ selectedOwnedVoucher.code }}
                          <span class="text-on-surface-variant font-semibold">· {{ voucherDiscountLabel(selectedOwnedVoucher) }}</span>
                        </span>
                        <span v-else class="text-on-surface-variant">Voucher của khách ({{ ownedVouchers.length }})</span>
                      </span>
                      <span class="material-symbols-outlined text-on-surface-variant transition-transform duration-300 shrink-0"
                            :class="{ 'rotate-180': showVoucherDropdown }">expand_more</span>
                    </button>

                    <!-- Backdrop đóng khi click ra ngoài -->
                    <div v-if="showVoucherDropdown" class="fixed inset-0 z-40" @click="showVoucherDropdown = false"></div>

                    <!-- Panel -->
                    <transition
                      enter-active-class="transition duration-200 ease-out" enter-from-class="transform scale-95 opacity-0 -translate-y-1" enter-to-class="transform scale-100 opacity-100 translate-y-0"
                      leave-active-class="transition duration-100 ease-in" leave-from-class="transform scale-100 opacity-100" leave-to-class="transform scale-95 opacity-0 -translate-y-1">
                      <div v-if="showVoucherDropdown"
                           class="absolute z-50 left-0 right-0 mt-2 rounded-2xl border border-primary/15 bg-surface-container-high/95 backdrop-blur-xl shadow-2xl shadow-black/40 overflow-hidden">
                        <p class="px-4 pt-3 pb-1.5 text-[10px] font-black uppercase tracking-widest text-on-surface-variant border-b border-outline-variant/10">
                          Voucher của khách ({{ ownedVouchers.length }})
                        </p>
                        <div class="max-h-56 overflow-y-auto custom-scrollbar py-1">
                          <button v-for="v in ownedVouchers" :key="v.id" type="button" @click="selectVoucher(v)"
                                  class="w-full text-left px-3 py-2.5 flex items-center gap-3 hover:bg-white/5 transition-colors"
                                  :class="{ 'bg-primary/10': voucherCodeInput === v.code }">
                            <span class="w-9 h-9 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary shrink-0">
                              <span class="material-symbols-outlined text-lg">local_activity</span>
                            </span>
                            <span class="min-w-0 flex-grow">
                              <span class="block text-sm font-black text-on-surface truncate uppercase">{{ v.code }}</span>
                              <span class="block text-[11px] font-bold text-primary">{{ voucherDiscountLabel(v) }}</span>
                            </span>
                            <span v-if="voucherCodeInput === v.code" class="material-symbols-outlined text-primary text-xl shrink-0">check_circle</span>
                          </button>
                        </div>
                      </div>
                    </transition>
                  </div>
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
            <div class="border-t border-dashed border-outline-variant/20 pt-6 space-y-2">
              <div class="flex justify-between text-sm font-bold text-on-surface">
                <span>{{ selectedSeats.length }} ghế: {{ selectedSeats.map(s => seatLabel(s)).join(', ') }}</span>
                <span>{{ fmt(seatTotal) }}đ</span>
              </div>
              <div v-if="selectedCombos.length" class="flex justify-between text-xs text-on-surface-variant font-medium">
                <span>F&B / Combo ({{ selectedCombos.reduce((a, c) => a + c.quantity, 0) }} phần)</span>
                <span>{{ fmt(comboTotal) }}đ</span>
              </div>
              <div v-if="completedBooking?.discountAmount || discountAmount" class="flex justify-between text-xs text-green-400 font-bold">
                <span>Giảm giá {{ appliedVoucher ? '(' + appliedVoucher.code + ')' : '' }}</span>
                <span>-{{ fmt(completedBooking?.discountAmount ?? discountAmount) }}đ</span>
              </div>
              <div class="flex justify-between items-center pt-2 border-t border-outline-variant/10">
                <span class="text-[10px] font-black text-primary uppercase">Tổng thanh toán</span>
                <span class="text-xl font-black text-primary italic">{{ fmt(completedBooking?.finalPrice ?? payableTotal) }}đ</span>
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
              <div class="flex items-center gap-4">
                <span class="text-[11px] font-bold text-on-surface-variant uppercase tracking-widest hidden sm:inline">Khách vãng lai · Không cần vé</span>
              </div>
            </div>

            <div v-if="isLoading" class="grid grid-cols-3 gap-5">
              <div v-for="i in 6" :key="i" class="h-28 bg-surface-container-high rounded-3xl animate-pulse"></div>
            </div>
            <div v-else-if="combos.length === 0" class="flex-grow flex items-center justify-center text-on-surface-variant text-sm">
              Chưa có món F&B. Thêm ở "Thực đơn F&B / Combo".
            </div>
            <div v-else class="grid grid-cols-2 xl:grid-cols-3 gap-3.5 flex-grow overflow-y-auto custom-scrollbar pr-2 content-start pb-6">
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
                    <span class="text-on-surface">{{ fmt((c.price + (c.surchargePrice || 0)) * c.quantity) }}đ</span>
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
            <p class="text-sm text-primary font-black mb-3">{{ selectedSeats.map(s => seatLabel(s)).join(', ') }}</p>
            <div v-for="b in seatTypeBreakdown" :key="b.type" class="flex justify-between text-xs font-semibold text-on-surface-variant mb-1">
              <span>Ghế {{ seatTypeLabel(b.type) }} <span class="text-on-surface-variant/60">x{{ b.count }}</span></span>
              <span class="text-on-surface">{{ fmt(b.subtotal) }}đ</span>
            </div>
          </div>
          <div v-if="selectedCombos.length" class="pb-5 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-2">F&B / Combo</p>
            <div v-for="(c, ci) in selectedCombos" :key="ci" class="mb-2">
              <div class="flex justify-between text-xs font-semibold">
                <span class="text-on-surface-variant">{{ c.name }} <span class="text-on-surface-variant/60">x{{ c.quantity }}</span></span>
                <span class="text-on-surface">{{ fmt((c.price + (c.surchargePrice || 0)) * c.quantity) }}đ</span>
              </div>
              <div v-if="c.options && c.options.length" class="text-[10px] text-on-surface mt-1.5 space-y-1.5 leading-normal ml-2">
                <div v-for="opt in c.options" :key="opt.optionItemId">
                  • {{ opt.optionName }} <span v-if="opt.surchargePrice > 0" class="text-amber-400 font-medium">(+{{ fmt(opt.surchargePrice) }}đ)</span>
                </div>
              </div>
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
        
        <button v-if="saleMode === 'FNB' || currentStep === 4"
          class="w-full py-3.5 mt-4 bg-primary text-on-primary font-bold text-base rounded-xl shadow-lg hover:brightness-110 active:scale-[0.98] transition-all disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none"
          :disabled="totalPrice === 0"
          @click="saleMode === 'FNB' ? fnbStep = 2 : currentStep = 5">
          THANH TOÁN ({{ fmt(payableTotal) }}đ)
        </button>
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
              <div class="flex justify-between"><span class="text-on-surface-variant">Số tiền</span><span class="font-black text-primary italic">{{ fmt(payableTotal) }}đ</span></div>
              <div class="flex justify-between"><span class="text-on-surface-variant">Nội dung</span><span class="font-bold text-on-surface">{{ transferContent }}</span></div>
            </div>
          </div>

          <div class="px-7 py-5 border-t border-outline-variant/10 flex gap-3">
            <AppButton variant="ghost" class="flex-1" @click="showQrModal = false">Hủy</AppButton>
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
