<script setup>
import { ref, computed, onMounted } from 'vue'
import { incidentApi } from '@/api/admin/index'
import { useToastStore } from '@/stores/toast'
import { useConfirmStore } from '@/stores/confirm'
import { friendlyError } from '@/utils/friendlyError'
import { useSeatGridRender } from '@/composables/useSeatGridRender'

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
const { cellAt } = useSeatGridRender(() => seatMap.value?.seats || [])

const rows = computed(() => Array.from({ length: seatMap.value?.matrixRow || 0 }, (_, i) => i))
const cols = computed(() => Array.from({ length: seatMap.value?.matrixCol || 0 }, (_, i) => i))

// ===== Thao tác: đổi ghế / hủy chỗ =====
const mode = ref('relocate')   // 'relocate' | 'cancel'
const swaps = ref({})          // oldSeatId -> newSeatId
const activeSource = ref(null) // oldSeatId đang chờ gán ghế đích
const cancelSel = ref({})      // bookingSeatId -> true

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
  compOptions.value.filter(o => mode.value === 'cancel' ? true : !o.cancelOnly))

const selectedSwaps = computed(() =>
  Object.entries(swaps.value).filter(([, dest]) => dest != null)
    .map(([oldSeatId, newSeatId]) => ({ oldSeatId: Number(oldSeatId), newSeatId })))

const canSubmitRelocate = computed(() => selectedSwaps.value.length > 0 && !started.value && !expired.value)
const canSubmitCancel = computed(() => Object.values(cancelSel.value).some(Boolean) && !expired.value)

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
  seatMap.value = null
  swaps.value = {}
  activeSource.value = null
  cancelSel.value = {}
  compChoice.value = 'NONE'
  compNote.value = ''
  if (!preserveResult) {
    result.value = null
  }
}

// ================= Tương tác sơ đồ =================
function isSeatMaintenance(cell) {
  if (!cell) return false
  return cell.status === 'MAINTENANCE' || cell.status === 'LOCKED' ||
         cell.seatStatus === 'MAINTENANCE' || cell.seatStatus === 'LOCKED'
}

function seatState(cell) {
  if (!cell || cell.kind === 'AISLE' || cell.seatId == null) return 'aisle'
  if (chosenDest.value.has(cell.seatId)) return 'dest'
  if (isSeatMaintenance(cell)) return 'blocked'
  if (bookingSeatIds.value.has(cell.seatId)) return 'source'
  if (cell.status === 'AVAILABLE') return 'free'
  return 'occupied'
}

function seatClass(cell) {
  const base = 'w-8 h-8 rounded-md flex items-center justify-center text-[9px] font-bold border transition-all leading-none select-none'
  const state = seatState(cell)
  const isSource = cell.seatId != null && bookingSeatIds.value.has(cell.seatId)

  switch (state) {
    case 'source': {
      if (mode.value === 'cancel') {
        const bs = soldSeats.value.find(s => s.seatId === cell.seatId)
        const isCancelled = bs && cancelSel.value[bs.bookingSeatId]
        return `${base} cursor-pointer ${isCancelled ? 'bg-red-500 border-red-300 text-white scale-105 shadow-lg shadow-red-500/30' : 'bg-blue-900/50 border-blue-500/50 text-blue-200 hover:border-blue-300'}`
      }
      const active = activeSource.value === cell.seatId
      return `${base} cursor-pointer ${active ? 'bg-blue-500 border-blue-300 text-white scale-110 shadow-lg shadow-blue-500/30' : 'bg-blue-900/50 border-blue-500/50 text-blue-200 hover:border-blue-300'}`
    }
    case 'dest':
      return `${base} bg-green-500 border-green-300 text-white cursor-pointer shadow-lg shadow-green-500/30`
    case 'blocked': {
      if (isSource) {
        if (mode.value === 'cancel') {
          const bs = soldSeats.value.find(s => s.seatId === cell.seatId)
          const isCancelled = bs && cancelSel.value[bs.bookingSeatId]
          return `${base} cursor-pointer bg-red-950/80 border-2 ${isCancelled ? 'border-red-400 ring-2 ring-red-500 text-white scale-105 shadow-lg' : 'border-red-500 text-red-300 hover:border-red-400'}`
        }
        const active = activeSource.value === cell.seatId
        return `${base} cursor-pointer bg-red-950/80 border-2 ${active ? 'border-blue-400 ring-2 ring-blue-500 text-white scale-110 shadow-lg' : 'border-red-500 text-red-300 hover:border-red-400'}`
      }
      return `${base} bg-red-950/40 border border-red-500/40 text-red-400 cursor-not-allowed opacity-80`
    }
    case 'free':
      return `${base} bg-surface-container-high border-outline-variant/20 text-on-surface-variant/60 ${mode.value === 'cancel' ? 'opacity-50' : 'cursor-pointer hover:border-primary/50'}`
    case 'occupied':
      return `${base} bg-surface-container-high border-white/5 text-on-surface-variant/20 cursor-not-allowed opacity-40`
    default:
      return 'w-8 h-8'
  }
}

function onSeatClick(cell) {
  if (!cell || cell.kind === 'AISLE' || cell.seatId == null) return
  const isSource = bookingSeatIds.value.has(cell.seatId)
  if (isSource) {
    onSourceClick(cell.seatId)
    return
  }
  const state = seatState(cell)
  if (state === 'dest') {
    // bỏ chọn đích: tìm source đang trỏ tới ghế này
    const src = Object.keys(swaps.value).find(k => swaps.value[k] === cell.seatId)
    if (src) { delete swaps.value[src]; swaps.value = { ...swaps.value } }
    return
  }
  if (state === 'free' && mode.value === 'relocate') {
    if (activeSource.value == null) { toast.info('Chọn ghế nguồn (viền xanh) trước, rồi bấm ghế đích.'); return }
    swaps.value = { ...swaps.value, [activeSource.value]: cell.seatId }
    activeSource.value = null
  }
}

function onSourceClick(seatId) {
  if (mode.value === 'relocate') {
    activeSource.value = activeSource.value === seatId ? null : seatId
  } else {
    const bsId = soldSeats.value.find(s => s.seatId === seatId)?.bookingSeatId
    if (bsId != null) {
      cancelSel.value[bsId] = !cancelSel.value[bsId]
    }
  }
}

function clearSwap(oldSeatId) {
  delete swaps.value[oldSeatId]
  swaps.value = { ...swaps.value }
}

function destLabel(oldSeatId) {
  const dest = swaps.value[oldSeatId]
  return dest != null ? (seatById.value.get(dest)?.label || '?') : null
}

function seatTooltip(cell) {
  if (!cell || cell.kind === 'AISLE' || cell.seatId == null) return ''
  const isMaint = isSeatMaintenance(cell)
  if (isMaint) return `Ghế ${cell.label} (Đang bảo trì / khóa) — Chuột phải để mở lại`
  if (bookingSeatIds.value.has(cell.seatId)) return `Ghế ${cell.label} (Ghế của đơn đang xử lý)`
  if (chosenDest.value.has(cell.seatId)) return `Ghế ${cell.label} (Ghế đích đã chọn)`
  if (cell.status === 'AVAILABLE') return `Ghế ${cell.label} (Trống) — Chuột phải để khóa bảo trì`
  return `Ghế ${cell.label} (Đã bán)`
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

// ================= Xử lý hủy chỗ =================
async function submitCancel() {
  const ids = Object.entries(cancelSel.value).filter(([, v]) => v).map(([k]) => Number(k))
  if (ids.length === 0) { toast.warning('Chọn ít nhất một ghế cần hủy.'); return }
  const ok = await confirm.show({
    title: 'Xác nhận hủy chỗ',
    message: `Hủy ${ids.length} ghế và đền bù cho khách? Thao tác này không hoàn tiền — khách nhận voucher đền bù.`,
    confirmText: 'Hủy chỗ & đền bù', tone: 'danger'
  })
  if (!ok) return
  submitting.value = true
  try {
    const { data } = await incidentApi.cancel({
      bookingId: ctx.value.bookingId,
      bookingSeatIds: ids,
      compensation: buildCompensation(),
      reason: compNote.value || null
    })
    result.value = data.data ?? data
    toast.success('Đã hủy chỗ & xử lý đền bù.')
    await doLookup(true)
  } catch (e) {
    toast.error(friendlyError(e, 'Hủy chỗ thất bại.'))
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
        <p class="text-sm text-on-surface-variant mt-1">Đổi ghế đền bù · Hủy chỗ · Khóa ghế hỏng — đền bù bằng voucher (không hoàn tiền).</p>
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
        <div v-if="ctx" class="bg-surface-container-high rounded-2xl p-4 border border-outline-variant/10 flex flex-col gap-3">
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
            ⚠ Suất đã bắt đầu — chỉ có thể HỦY CHỖ, không đổi ghế.
          </div>
          <div v-if="!ctx.hasCustomer" class="text-[11px] font-semibold text-blue-300 bg-blue-500/10 rounded-lg px-3 py-2">
            ℹ Khách vãng lai — đền trực tiếp tại quầy, hệ thống không phát voucher điện tử (chỉ ghi vết).
          </div>

          <!-- Chọn chế độ -->
          <div class="flex gap-1 bg-surface rounded-lg p-1">
            <button @click="mode = 'relocate'" :disabled="started" :class="['flex-1 py-1.5 rounded-md text-xs font-bold transition-all disabled:opacity-40', mode === 'relocate' ? 'bg-blue-500 text-white' : 'text-on-surface-variant']">Đổi ghế</button>
            <button @click="mode = 'cancel'" :class="['flex-1 py-1.5 rounded-md text-xs font-bold transition-all', mode === 'cancel' ? 'bg-red-500 text-white' : 'text-on-surface-variant']">Hủy chỗ</button>
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
              <!-- Relocate: hiển thị đích -->
              <div v-if="mode === 'relocate'" class="flex items-center justify-between mt-2 text-xs">
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
              <!-- Cancel: checkbox -->
              <label v-else class="flex items-center gap-2 mt-2 text-xs cursor-pointer select-none">
                <input type="checkbox" v-model="cancelSel[s.bookingSeatId]" class="w-4 h-4 rounded accent-red-500 cursor-pointer" />
                <span :class="['transition-colors', cancelSel[s.bookingSeatId] ? 'text-red-400 font-bold' : 'text-on-surface-variant']">
                  Hủy ghế này ({{ fmtPrice(s.priceSnapshot) }})
                </span>
              </label>
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

            <button v-if="mode === 'relocate'" @click="submitRelocate" :disabled="!canSubmitRelocate || submitting"
                    class="w-full py-3 rounded-xl bg-primary text-on-primary font-extrabold text-sm hover:opacity-90 disabled:opacity-40 flex items-center justify-center gap-2">
              <span class="material-symbols-outlined text-lg">swap_horiz</span>
              {{ submitting ? 'Đang xử lý...' : 'Xác nhận & in lại vé' }}
            </button>
            <button v-else @click="submitCancel" :disabled="!canSubmitCancel || submitting"
                    class="w-full py-3 rounded-xl bg-red-500 text-white font-extrabold text-sm hover:opacity-90 disabled:opacity-40 flex items-center justify-center gap-2">
              <span class="material-symbols-outlined text-lg">event_busy</span>
              {{ submitting ? 'Đang xử lý...' : 'Hủy chỗ & đền bù' }}
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
      <div class="bg-surface-container-high rounded-2xl p-6 border border-outline-variant/10">
        <div v-if="loadingMap" class="h-full flex items-center justify-center text-on-surface-variant">
          <span class="material-symbols-outlined animate-spin">progress_activity</span>
        </div>
        <div v-else-if="!seatMap" class="h-full flex flex-col items-center justify-center text-on-surface-variant/50 py-16">
          <span class="material-symbols-outlined text-5xl opacity-30">grid_on</span>
          <p class="text-sm mt-2">Sơ đồ ghế của suất sẽ hiển thị sau khi tra cứu vé.</p>
        </div>
        <div v-else class="flex flex-col items-center gap-5">
          <div class="w-2/3 h-1.5 bg-gradient-to-r from-transparent via-primary/60 to-transparent rounded-full"></div>
          <p class="text-[10px] uppercase tracking-[0.3em] text-on-surface-variant/60">Màn hình</p>
          <div class="inline-flex flex-col gap-1 overflow-auto max-w-full py-2">
            <div v-for="r in rows" :key="r" class="flex gap-1 justify-center">
              <template v-for="c in cols" :key="`${r}-${c}`">
                <div v-if="!cellAt(r, c)" class="w-8 h-8"></div>
                <div v-else-if="cellAt(r, c).kind === 'AISLE'" class="w-8 h-8"></div>
                <button v-else :class="seatClass(cellAt(r, c))"
                        @click="onSeatClick(cellAt(r, c))"
                        @contextmenu.prevent="onSeatContextMenu(cellAt(r, c))"
                        :title="seatTooltip(cellAt(r, c))">
                  <span v-if="isSeatMaintenance(cellAt(r, c))" class="material-symbols-outlined text-[12px] leading-none">build</span>
                  <span v-else>{{ cellAt(r, c).label }}</span>
                </button>
              </template>
            </div>
          </div>
          <!-- Chú thích -->
          <div class="flex flex-wrap gap-4 text-[11px] text-on-surface-variant pt-2">
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-blue-900/50 border border-blue-500/50"></span> Ghế của đơn</span>
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-green-500"></span> Ghế đích</span>
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-surface-container-high border border-outline-variant/20"></span> Trống</span>
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-red-950/60 border border-red-500/50 text-red-400 flex items-center justify-center"><span class="material-symbols-outlined text-[10px]">build</span></span> Bảo trì/khóa</span>
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-surface-container-high opacity-40"></span> Đã bán</span>
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
            <option value="CANCEL">Hủy chỗ</option>
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
