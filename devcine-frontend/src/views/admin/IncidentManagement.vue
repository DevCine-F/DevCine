<script setup>
import { ref, computed, watch, onMounted } from 'vue'
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
const ctx = ref(null)          // IncidentBookingContext

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

// ===== Đền bù & Khách vãng lai =====
const compOptions = ref([])
const compChoice = ref('NONE') // promotionId | 'NONE'
const compNote = ref('')
const walkInPhone = ref('')
const submitting = ref(false)
const result = ref(null)       // kết quả sau khi xử lý

// ===== Modal Cảnh báo Xung đột Suất sau (Chain Lock) =====
const conflictModalOpen = ref(false)
const conflictList = ref([])
const pendingMaintenanceSeat = ref(null)

// ===== Lịch sử & Xuất báo cáo =====
const histFilters = ref({ type: '', code: '', from: '', to: '' })
const histRows = ref([])
const histPage = ref(0)
const histTotal = ref(0)
const histSize = 20
const loadingHist = ref(false)
const exporting = ref(false)

// ---- Chỉ mục hỗ trợ ----
const soldSeats = computed(() => (ctx.value?.seats || []).filter(s => s.status === 'SOLD'))
const bookingSeatIds = computed(() => new Set(soldSeats.value.map(s => s.seatId)))
const seatById = computed(() => {
  const m = new Map()
  for (const c of seatMap.value?.seats || []) if (c.seatId != null) m.set(c.seatId, c)
  return m
})
const chosenDest = computed(() => new Set(Object.values(swaps.value)))

const visibleCompOptions = computed(() =>
  compOptions.value.filter(o => mode.value === 'cancel' ? true : !o.cancelOnly))

const selectedSwaps = computed(() =>
  Object.entries(swaps.value).filter(([, dest]) => dest != null)
    .map(([oldSeatId, newSeatId]) => ({ oldSeatId: Number(oldSeatId), newSeatId })))

const canSubmitRelocate = computed(() => selectedSwaps.value.length > 0)
const canSubmitCancel = computed(() => Object.values(cancelSel.value).some(Boolean))

// Xếp hạng loại ghế kiểm tra hạ hạng: NORMAL (0) < VIP (1) < SWEETBOX (2)
const SEAT_RANK = { NORMAL: 0, VIP: 1, SWEETBOX: 2 }
const hasDowngrade = computed(() => {
  return selectedSwaps.value.some(s => {
    const oldSeat = soldSeats.value.find(seat => seat.seatId === s.oldSeatId)
    const newSeat = seatById.value.get(s.newSeatId)
    if (!oldSeat || !newSeat) return false
    const oldRank = SEAT_RANK[(oldSeat.seatType || 'NORMAL').toUpperCase()] ?? 0
    const newRank = SEAT_RANK[(newSeat.seatType || 'NORMAL').toUpperCase()] ?? 0
    return newRank < oldRank
  })
})

// Tự động gợi ý voucher bắp nước khi hạ hạng ghế
watch(hasDowngrade, (downgrade) => {
  if (downgrade && compChoice.value === 'NONE') {
    const fnbOpt = compOptions.value.find(o => o.code === 'COMP_FNB_COMBO' || o.type === 'GIFT_FNB')
    if (fnbOpt) {
      compChoice.value = fnbOpt.promotionId
      if (!compNote.value) {
        compNote.value = 'Tặng kèm Voucher Bắp Nước thiện chí do chuyển xuống hạng ghế thấp hơn'
      }
    }
  }
})

// ================= Tra cứu =================
async function doLookup(preserveResult = false) {
  const q = searchQuery.value.trim()
  if (!q) { toast.warning('Nhập mã vé hoặc số điện thoại khách.'); return }
  loadingCtx.value = true
  resetWorkspace(preserveResult)
  try {
    const { data } = await incidentApi.lookup(q)
    ctx.value = data.data ?? data
    if (ctx.value?.customerPhone) {
      walkInPhone.value = ctx.value.customerPhone
    }
    await loadSeatMap()
  } catch (e) {
    ctx.value = null
    toast.error(friendlyError(e, 'Không tìm thấy vé phù hợp.'))
  } finally {
    loadingCtx.value = false
  }
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
  walkInPhone.value = ''
  if (!preserveResult) {
    result.value = null
  }
}

// ================= Tương tác sơ đồ tĩnh =================
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
        return `${base} cursor-pointer ${isCancelled ? 'bg-red-600 border-red-300 text-white shadow-md' : 'bg-blue-950/90 border-2 border-amber-400 ring-2 ring-amber-400/50 text-amber-200 hover:border-amber-300'}`
      }
      const active = activeSource.value === cell.seatId
      return `${base} cursor-pointer ${active ? 'bg-blue-600 border-2 border-amber-300 ring-4 ring-amber-400/60 text-white scale-110 shadow-lg' : 'bg-blue-950/90 border-2 border-amber-400 ring-2 ring-amber-400/50 text-amber-200 hover:border-amber-300'}`
    }
    case 'dest':
      return `${base} bg-green-600 border-green-300 text-white cursor-pointer shadow-lg shadow-green-600/30 font-bold`
    case 'blocked': {
      if (isSource) {
        if (mode.value === 'cancel') {
          const bs = soldSeats.value.find(s => s.seatId === cell.seatId)
          const isCancelled = bs && cancelSel.value[bs.bookingSeatId]
          return `${base} cursor-pointer bg-red-950/90 border-2 ${isCancelled ? 'border-red-400 ring-2 ring-red-500 text-white' : 'border-amber-400 text-amber-200'}`
        }
        const active = activeSource.value === cell.seatId
        return `${base} cursor-pointer bg-red-950/90 border-2 ${active ? 'border-amber-300 ring-4 ring-amber-400 text-white scale-110' : 'border-amber-400 text-amber-200'}`
      }
      return `${base} bg-red-950/70 border border-red-500/50 text-red-400 cursor-not-allowed`
    }
    case 'free':
      return `${base} bg-surface-container-high border border-outline-variant/30 text-on-surface ${mode.value === 'cancel' ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer hover:border-primary/60'}`
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
    const src = Object.keys(swaps.value).find(k => swaps.value[k] === cell.seatId)
    if (src) { delete swaps.value[src]; swaps.value = { ...swaps.value } }
    return
  }
  if (state === 'free' && mode.value === 'relocate') {
    if (activeSource.value == null) { toast.info('Chọn ghế sự cố (viền vàng) trước, rồi chọn vị trí ghế đích.'); return }
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
  if (bookingSeatIds.value.has(cell.seatId)) return `Ghế ${cell.label} (Ghế của khách gặp sự cố)`
  if (chosenDest.value.has(cell.seatId)) return `Ghế ${cell.label} (Ghế đích đã chọn)`
  if (cell.status === 'AVAILABLE') return `Ghế ${cell.label} (Trống) — Chuột phải để khóa bảo trì`
  return `Ghế ${cell.label} (Đã bán)`
}

function onSeatContextMenu(cell) {
  if (!cell || cell.kind === 'AISLE' || cell.seatId == null) return
  toggleSeatMaintenance(cell)
}

// ================= Khóa / Mở ghế bảo trì (Chain Lock) =================
async function toggleSeatMaintenance(seat) {
  const seatId = seat.seatId || seat.id
  const cell = seatById.value.get(seatId)
  const isMaint = isSeatMaintenance(cell) || seat.seatStatus === 'MAINTENANCE'
  const label = seat.seatLabel || seat.label || cell?.label || `Ghế #${seatId}`

  if (isMaint) {
    const ok = await confirm.show({
      title: 'Mở khóa ghế',
      message: `Mở lại ghế ${label} để bán bình thường ở tất cả các suất chiếu tương lai?`,
      confirmText: 'Mở lại',
      tone: 'primary'
    })
    if (!ok) return
    try {
      await incidentApi.setSeatStatus(seatId, {
        status: 'AVAILABLE',
        reason: 'Mở lại sau sửa chữa'
      })
      toast.success(`Đã mở lại ghế ${label}.`)
      await loadSeatMap()
    } catch (e) {
      toast.error(friendlyError(e, 'Không thể mở lại ghế.'))
    }
  } else {
    // Quét cảnh báo xung đột các suất tương lai trước khi khóa
    try {
      const { data } = await incidentApi.futureConflicts(seatId)
      const conflicts = data.data ?? data ?? []
      if (conflicts.length > 0) {
        conflictList.value = conflicts
        pendingMaintenanceSeat.value = { seatId, label }
        conflictModalOpen.value = true
        return
      }
    } catch (e) {
      console.warn('Không thể quét xung đột suất sau:', e)
    }

    proceedLockSeat(seatId, label)
  }
}

async function proceedLockSeat(seatId, label) {
  const ok = await confirm.show({
    title: 'Khóa bảo trì ghế',
    message: `Đánh dấu ghế ${label} là BẢO TRÌ? Ghế sẽ tự động ngừng bán ở suất hiện tại và TẤT CẢ các suất chiếu tương lai (Chain Lock).`,
    confirmText: 'Khóa ghế',
    tone: 'danger'
  })
  if (!ok) return
  try {
    await incidentApi.setSeatStatus(seatId, {
      status: 'MAINTENANCE',
      reason: `Báo hỏng khi xử lý đơn ${ctx.value?.bookingCode || ''}`.trim()
    })
    toast.success(`Đã khóa ghế ${label} (bảo trì).`)
    conflictModalOpen.value = false
    await loadSeatMap()
  } catch (e) {
    toast.error(friendlyError(e, 'Không thể khóa ghế.'))
  }
}

// ================= Đền bù (payload) =================
function buildCompensation() {
  const phone = walkInPhone.value.trim() || ctx.value?.customerPhone || null
  if (compChoice.value === 'NONE') return { type: 'NONE', note: compNote.value || null, customerPhone: phone }
  const opt = compOptions.value.find(o => String(o.promotionId) === String(compChoice.value))
  if (!opt) return { type: 'NONE', note: compNote.value || null, customerPhone: phone }
  return { type: opt.type, promotionTemplateId: opt.promotionId, note: compNote.value || null, customerPhone: phone }
}

// ================= Xử lý đổi ghế =================
async function submitRelocate() {
  if (!canSubmitRelocate.value) { toast.warning('Chọn ít nhất một cặp đổi ghế.'); return }
  const lines = selectedSwaps.value.map(s => `${seatById.value.get(s.oldSeatId)?.label || s.oldSeatId} → ${seatById.value.get(s.newSeatId)?.label || s.newSeatId}`).join(', ')
  const ok = await confirm.show({
    title: 'Xác nhận đổi ghế',
    message: `Đổi ghế: ${lines} (Phụ thu 0đ). ${compChoice.value !== 'NONE' ? 'Sẽ cấp voucher đền bù kèm theo.' : 'Không phát voucher đền bù.'}`,
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
    toast.success('Đã đổi ghế & xử lý đền bù thành công.')
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
    title: 'Xác nhận hủy chỗ & Đền bù',
    message: `Hủy ${ids.length} ghế và cấp Voucher đền bù cho khách? Hệ thống không hoàn tiền mặt mà quy đổi thành voucher đền bù.`,
    confirmText: 'Hủy chỗ & Đền bù', tone: 'danger'
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
    toast.success('Đã hủy chỗ & xử lý đền bù thành công.')
    await doLookup(true)
  } catch (e) {
    toast.error(friendlyError(e, 'Hủy chỗ thất bại.'))
  } finally {
    submitting.value = false
  }
}

// ================= In lại vé =================
function printReceipt() {
  window.print()
}

// ================= Lịch sử & Xuất báo cáo =================
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

async function exportCsv() {
  exporting.value = true
  try {
    const params = {}
    if (histFilters.value.type) params.type = histFilters.value.type
    if (histFilters.value.code) params.code = histFilters.value.code.trim()
    if (histFilters.value.from) params.from = histFilters.value.from
    if (histFilters.value.to) params.to = histFilters.value.to
    const res = await incidentApi.exportHistory(params)
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `incidents_${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    toast.success('Đã xuất file báo cáo đối soát CSV.')
  } catch (e) {
    toast.error(friendlyError(e, 'Xuất báo cáo thất bại.'))
  } finally {
    exporting.value = false
  }
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'history' && histRows.value.length === 0) loadHistory(0)
}

const fmtPrice = (n) => (n != null ? Number(n).toLocaleString('vi-VN') + 'đ' : '0đ')
const fmtTime = (s) => (s ? new Date(s).toLocaleString('vi-VN') : '')
const typeLabel = (t) => ({ RELOCATE: 'Đổi ghế', CANCEL: 'Hủy chỗ', SEAT_MAINTENANCE: 'Khóa bảo trì', EMERGENCY_CLOSURE: 'Đóng cửa rạp' }[t] || t)
const compLabel = (t) => ({ NONE: '—', DISCOUNT: 'Voucher giảm giá', GIFT_FNB: 'Quà Bắp Nước', GIFT_TICKET: 'Vé mời 2D FOC' }[t] || t)
const incidentCodeDisplay = (row) => 'INC-' + (row.createdAt ? new Date(row.createdAt).toISOString().slice(0, 10).replace(/-/g, '') : '2026') + '-' + String(row.id).padStart(4, '0')

onMounted(async () => {
  try {
    const { data } = await incidentApi.compensationOptions()
    compOptions.value = data.data ?? data
  } catch { /* im lặng */ }
})
</script>

<template>
  <div class="p-6 md:p-8 flex flex-col gap-6 min-h-full">
    <!-- Header -->
    <div class="flex items-center justify-between flex-wrap gap-4">
      <div>
        <h1 class="text-3xl font-black tracking-tighter text-on-surface uppercase italic">
          Xử lý sự cố <span class="text-primary">Phòng chiếu & Ghế ngồi</span>
        </h1>
        <p class="text-on-surface-variant text-xs mt-1 font-bold uppercase tracking-widest">
          Hỗ trợ và xử lý sự cố khách hàng
        </p>
      </div>
      <div class="flex gap-1 bg-surface-container-high rounded-xl p-1">
        <button @click="switchTab('handle')" :class="['px-4 py-2 rounded-lg text-sm font-semibold transition-all', activeTab === 'handle' ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:bg-white/5']">Xử lý sự cố</button>
        <button @click="switchTab('history')" :class="['px-4 py-2 rounded-lg text-sm font-semibold transition-all', activeTab === 'history' ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:bg-white/5']">Lịch sử & Đối soát</button>
      </div>
    </div>

    <!-- ============ TAB XỬ LÝ SỰ CỐ ============ -->
    <div v-if="activeTab === 'handle'" class="grid grid-cols-1 lg:grid-cols-[400px_1fr] gap-6">
      <!-- Cột trái: Tra cứu & Chi tiết vé -->
      <div class="flex flex-col gap-4">
        <!-- Ô tra cứu -->
        <div class="bg-surface-container-high rounded-2xl p-4 border border-outline-variant/10">
          <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tra cứu đơn vé sự cố</label>
          <div class="flex gap-2 mt-2">
            <input v-model="searchQuery" @keyup.enter="doLookup(false)" placeholder="Nhập Mã vé hoặc SĐT khách..."
                   class="flex-1 py-2.5 px-3 rounded-lg bg-surface border border-outline-variant/20 text-on-surface text-sm outline-none focus:border-primary transition-colors" />
            <button @click="doLookup(false)" :disabled="loadingCtx"
                    class="px-4 rounded-lg bg-primary text-on-primary font-bold text-sm hover:opacity-90 disabled:opacity-50 flex items-center">
              <span class="material-symbols-outlined text-lg">{{ loadingCtx ? 'hourglass_empty' : 'search' }}</span>
            </button>
          </div>
        </div>

        <!-- Empty state -->
        <div v-if="!ctx && !loadingCtx" class="bg-surface-container rounded-2xl p-8 text-center text-on-surface-variant border border-dashed border-outline-variant/20">
          <span class="material-symbols-outlined text-4xl opacity-40">receipt_long</span>
          <p class="text-sm mt-2">Nhập mã vé hoặc số điện thoại để bắt đầu xử lý sự cố.</p>
        </div>

        <!-- Chi tiết đơn vé -->
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

          <!-- Lựa chọn chế độ: Đổi ghế / Hủy chỗ -->
          <div class="flex gap-1 bg-surface rounded-lg p-1 mt-1">
            <button @click="mode = 'relocate'" :class="['flex-1 py-1.5 rounded-md text-xs font-bold transition-all', mode === 'relocate' ? 'bg-blue-600 text-white' : 'text-on-surface-variant']">Hướng 1: Đổi ghế (0đ)</button>
            <button @click="mode = 'cancel'" :class="['flex-1 py-1.5 rounded-md text-xs font-bold transition-all', mode === 'cancel' ? 'bg-red-600 text-white' : 'text-on-surface-variant']">Hướng 2: Hủy chỗ & Đền bù</button>
          </div>

          <!-- Danh sách ghế của đơn -->
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Danh sách ghế trong vé</label>
            <div v-for="s in soldSeats" :key="s.bookingSeatId" class="bg-surface rounded-lg p-2.5 border border-outline-variant/10">
              <div class="flex items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <span class="font-bold text-amber-300 text-sm px-1.5 py-0.5 rounded bg-blue-950/80 border border-amber-400">{{ s.seatLabel }}</span>
                  <span class="text-[10px] px-1.5 py-0.5 rounded bg-surface-container-high text-on-surface-variant font-semibold">{{ s.seatType }}</span>
                  <span class="text-[10px] text-on-surface-variant">{{ s.ticketType }}</span>
                  <span v-if="isSeatMaintenance(seatById.get(s.seatId))" class="text-[10px] font-bold px-1.5 py-0.5 rounded bg-red-500/20 text-red-400 border border-red-500/30 flex items-center gap-0.5">
                    <span class="material-symbols-outlined text-[11px]">build</span> Bảo trì
                  </span>
                </div>
                <button @click="toggleSeatMaintenance(s)" :title="isSeatMaintenance(seatById.get(s.seatId)) ? 'Mở lại ghế này' : 'Báo hỏng ghế này (bảo trì)'"
                        :class="['flex items-center p-1 rounded transition-colors', isSeatMaintenance(seatById.get(s.seatId)) ? 'text-amber-400 hover:text-amber-300 hover:bg-amber-400/10' : 'text-red-400 hover:text-red-300 hover:bg-red-400/10']">
                  <span class="material-symbols-outlined text-lg">{{ isSeatMaintenance(seatById.get(s.seatId)) ? 'lock_open' : 'build' }}</span>
                </button>
              </div>

              <!-- Mode Relocate: hiển thị đích -->
              <div v-if="mode === 'relocate'" class="flex items-center justify-between mt-2 text-xs">
                <button @click="onSourceClick(s.seatId)"
                        :class="['px-2.5 py-1 rounded font-bold transition-all', activeSource === s.seatId ? 'bg-amber-400 text-black shadow-md' : 'bg-blue-950/80 text-amber-200 border border-amber-400/40']">
                  {{ activeSource === s.seatId ? 'Chọn ghế trống trên sơ đồ →' : 'Chọn ghế này để đổi' }}
                </button>
                <div class="flex items-center gap-1">
                  <span v-if="destLabel(s.seatId)" class="text-green-400 font-bold">→ {{ destLabel(s.seatId) }}</span>
                  <span v-else class="text-on-surface-variant/50">chưa chọn đích</span>
                  <button v-if="destLabel(s.seatId)" @click="clearSwap(s.seatId)" class="text-red-400 ml-1"><span class="material-symbols-outlined text-sm">close</span></button>
                </div>
              </div>

              <!-- Mode Cancel: checkbox -->
              <label v-else class="flex items-center gap-2 mt-2 text-xs cursor-pointer select-none">
                <input type="checkbox" v-model="cancelSel[s.bookingSeatId]" class="w-4 h-4 rounded accent-red-500 cursor-pointer" />
                <span :class="['transition-colors', cancelSel[s.bookingSeatId] ? 'text-red-400 font-bold' : 'text-on-surface-variant']">
                  Hủy vị trí ghế này ({{ fmtPrice(s.priceSnapshot) }})
                </span>
              </label>
            </div>
          </div>

          <!-- Cảnh báo gợi ý khi hạ hạng ghế -->
          <div v-if="hasDowngrade && mode === 'relocate'" class="p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs flex items-start gap-2">
            <span class="material-symbols-outlined text-base mt-0.5 text-amber-400">card_giftcard</span>
            <div>
              <p class="font-bold text-amber-200">Gợi ý đền bù thiện chí</p>
              <p class="mt-0.5 text-[11px]">Khách chuyển từ hạng ghế cao xuống hạng ghế thấp hơn. Hệ thống đề xuất tặng kèm Voucher Bắp Nước thiện chí.</p>
            </div>
          </div>

          <!-- Khu vực Đền bù Voucher -->
          <div class="border-t border-outline-variant/10 pt-3 space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Gói đền bù (Không hoàn tiền)</label>
            <select v-model="compChoice" class="w-full py-2 px-3 rounded-lg bg-surface border border-outline-variant/20 text-on-surface text-sm outline-none focus:border-primary">
              <option value="NONE">Không đền bù</option>
              <option v-for="o in visibleCompOptions" :key="o.promotionId" :value="o.promotionId">
                {{ o.label }}{{ o.type === 'DISCOUNT' && o.discountValue > 0 ? ` (${fmtPrice(o.discountValue)})` : '' }}
              </option>
            </select>

            <!-- Ô nhập SĐT nhận voucher nếu khách vãng lai -->
            <div v-if="compChoice !== 'NONE'" class="space-y-1">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">SĐT nhận Voucher đền bù (Hạn 90 ngày)</label>
              <input v-model="walkInPhone" placeholder="Nhập số điện thoại khách hàng..." maxlength="11"
                     class="w-full py-2 px-3 rounded-lg bg-surface border border-outline-variant/20 text-on-surface text-sm outline-none focus:border-primary font-mono" />
              <p class="text-[10px] text-on-surface-variant/70">Voucher sẽ được lưu vào hồ sơ theo SĐT này. Khách có thể đọc SĐT tại quầy hoặc đăng nhập để sử dụng.</p>
            </div>

            <input v-model="compNote" placeholder="Lý do sự cố (vd: kẹt cơ cấu, ghế dính bẩn...)" maxlength="255"
                   class="w-full py-2 px-3 rounded-lg bg-surface border border-outline-variant/20 text-on-surface text-sm outline-none focus:border-primary" />

            <button v-if="mode === 'relocate'" @click="submitRelocate" :disabled="!canSubmitRelocate || submitting"
                    class="w-full py-3 rounded-xl bg-primary text-on-primary font-extrabold text-sm hover:opacity-90 disabled:opacity-40 flex items-center justify-center gap-2 shadow-lg">
              <span class="material-symbols-outlined text-lg">swap_horiz</span>
              {{ submitting ? 'Đang xử lý...' : 'Xác nhận đổi ghế (0đ phụ thu)' }}
            </button>
            <button v-else @click="submitCancel" :disabled="!canSubmitCancel || submitting"
                    class="w-full py-3 rounded-xl bg-red-600 text-white font-extrabold text-sm hover:opacity-90 disabled:opacity-40 flex items-center justify-center gap-2 shadow-lg">
              <span class="material-symbols-outlined text-lg">event_busy</span>
              {{ submitting ? 'Đang xử lý...' : 'Hủy chỗ & Cấp Voucher đền bù' }}
            </button>
          </div>

          <!-- Kết quả sau khi xử lý -->
          <div v-if="result" class="bg-green-500/10 border border-green-500/30 rounded-xl p-4 text-xs text-green-300 space-y-2">
            <div class="flex items-center justify-between">
              <p class="font-bold flex items-center gap-1.5 text-sm text-green-200">
                <span class="material-symbols-outlined text-base text-green-400">check_circle</span>
                Xử lý sự cố thành công
              </p>
              <button @click="result = null" class="text-on-surface-variant hover:text-white" title="Đóng">
                <span class="material-symbols-outlined text-sm">close</span>
              </button>
            </div>
            <div v-if="result.compensation?.voucherIssued" class="p-2.5 rounded-lg bg-green-950/60 border border-green-500/30 space-y-1">
              <p class="text-green-200">Mã voucher đã lưu vào SĐT khách:</p>
              <p class="text-base font-mono font-black text-green-400 tracking-wider select-all">{{ result.compensation.voucherCode }}</p>
              <p class="text-[11px] text-green-300/80">Hạn sử dụng: 90 ngày kể từ hôm nay.</p>
            </div>
            <p v-else-if="result.compensation?.counterGift" class="text-amber-300">
              Khách vãng lai: Tặng quà/phiếu giấy trực tiếp tại quầy theo quy định.
            </p>
            <div v-if="result.reprint" class="pt-1">
              <button @click="printReceipt" class="w-full py-2 rounded-lg bg-green-600 text-white font-bold text-xs hover:bg-green-500 flex items-center justify-center gap-1.5 shadow">
                <span class="material-symbols-outlined text-base">print</span>
                In cuống vé giấy mới tại quầy
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Cột phải: Sơ đồ phòng chiếu tĩnh -->
      <div class="bg-surface-container-high rounded-2xl p-6 border border-outline-variant/10">
        <div v-if="loadingMap" class="h-full flex items-center justify-center text-on-surface-variant">
          <span class="material-symbols-outlined animate-spin">progress_activity</span>
        </div>
        <div v-else-if="!seatMap" class="h-full flex flex-col items-center justify-center text-on-surface-variant/50 py-16">
          <span class="material-symbols-outlined text-5xl opacity-30">grid_on</span>
          <p class="text-sm mt-2">Sơ đồ phòng chiếu sẽ hiển thị sau khi tra cứu đơn vé.</p>
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
          <!-- Chú thích quy chuẩn màu tĩnh -->
          <div class="flex flex-wrap gap-4 text-[11px] text-on-surface-variant pt-3 border-t border-outline-variant/10 w-full justify-center">
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-blue-950/90 border-2 border-amber-400 ring-1 ring-amber-400"></span> Ghế của khách gặp sự cố</span>
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-surface-container-high border border-outline-variant/30"></span> Ghế trống (sẵn sàng đổi)</span>
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-green-600 border border-green-300"></span> Ghế đích vừa chọn</span>
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-red-950/70 border border-red-500/50 text-red-400 flex items-center justify-center"><span class="material-symbols-outlined text-[10px]">build</span></span> Đang bảo trì</span>
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-surface-container-high opacity-40"></span> Đã bán cho khách khác</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ TAB LỊCH SỬ & ĐỐI SOÁT ============ -->
    <div v-else class="bg-surface-container-high rounded-2xl p-5 border border-outline-variant/10">
      <div class="flex flex-wrap gap-3 items-end mb-4 justify-between">
        <div class="flex flex-wrap gap-3 items-end">
          <div>
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Loại sự cố</label>
            <select v-model="histFilters.type" class="block mt-1 py-2 px-3 rounded-lg bg-surface border border-outline-variant/20 text-on-surface text-sm outline-none">
              <option value="">Tất cả</option>
              <option value="RELOCATE">Đổi ghế</option>
              <option value="CANCEL">Hủy chỗ</option>
              <option value="SEAT_MAINTENANCE">Khóa bảo trì</option>
              <option value="EMERGENCY_CLOSURE">Đóng cửa rạp</option>
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

        <button @click="exportCsv" :disabled="exporting" class="py-2 px-4 rounded-lg bg-surface border border-outline-variant/30 text-on-surface font-bold text-sm hover:bg-white/5 flex items-center gap-2">
          <span class="material-symbols-outlined text-base">download</span>
          {{ exporting ? 'Đang xuất...' : 'Xuất CSV / Excel' }}
        </button>
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
              <th class="text-left py-2 px-2">Mã sự cố</th>
              <th class="text-left py-2 px-2">Thời gian</th>
              <th class="text-left py-2 px-2">Loại</th>
              <th class="text-left py-2 px-2">Mã vé</th>
              <th class="text-left py-2 px-2">Vị trí ghế</th>
              <th class="text-left py-2 px-2">Đền bù</th>
              <th class="text-left py-2 px-2">Mã Voucher</th>
              <th class="text-left py-2 px-2">Quản lý thực hiện</th>
              <th class="text-left py-2 px-2">Lý do sự cố</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in histRows" :key="row.id" class="border-b border-outline-variant/5 hover:bg-white/5">
              <td class="py-2 px-2 font-mono text-xs text-primary font-bold">{{ incidentCodeDisplay(row) }}</td>
              <td class="py-2 px-2 text-on-surface-variant whitespace-nowrap text-xs">{{ fmtTime(row.createdAt) }}</td>
              <td class="py-2 px-2"><span class="text-xs font-bold px-2 py-0.5 rounded bg-surface-container-highest text-on-surface">{{ typeLabel(row.type) }}</span></td>
              <td class="py-2 px-2 font-mono text-on-surface">{{ row.bookingCode || '—' }}</td>
              <td class="py-2 px-2 text-on-surface font-semibold">{{ row.oldSeatLabel }}<span v-if="row.newSeatLabel" class="text-green-400"> → {{ row.newSeatLabel }}</span></td>
              <td class="py-2 px-2 text-on-surface-variant">{{ compLabel(row.compensationType) }}<span v-if="row.compensationAmount > 0"> · {{ fmtPrice(row.compensationAmount) }}</span></td>
              <td class="py-2 px-2 font-mono text-xs text-amber-300 font-bold">{{ row.voucherCode || '—' }}</td>
              <td class="py-2 px-2 text-on-surface-variant">{{ row.handledByName || '—' }}</td>
              <td class="py-2 px-2 text-on-surface-variant/70 text-xs max-w-[200px] truncate" :title="row.reason">{{ row.reason || '—' }}</td>
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

    <!-- ============ MODAL CẢNH BÁO XUNG ĐỘT SUẤT SAU (CHAIN LOCK) ============ -->
    <div v-if="conflictModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
      <div class="bg-surface-container-high border border-amber-500/40 rounded-2xl p-6 max-w-2xl w-full shadow-2xl space-y-4">
        <div class="flex items-center justify-between border-b border-outline-variant/10 pb-3">
          <h2 class="text-lg font-black text-amber-300 flex items-center gap-2">
            <span class="material-symbols-outlined text-amber-400">warning</span>
            Cảnh báo Xung đột Khóa bảo trì (Chain Lock)
          </h2>
          <button @click="conflictModalOpen = false" class="text-on-surface-variant hover:text-white"><span class="material-symbols-outlined">close</span></button>
        </div>

        <div class="p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 text-xs text-amber-200 space-y-1">
          <p class="font-bold">Ghế {{ pendingMaintenanceSeat?.label }} đã có khách đặt trước ở các suất chiếu tiếp theo!</p>
          <p>Nếu xác nhận khóa bảo trì, vị trí ghế này sẽ ngừng bán trên toàn hệ thống. Quản lý vui lòng chủ động liên hệ danh sách khách hàng dưới đây để sắp xếp đổi chỗ trước giờ chiếu:</p>
        </div>

        <div class="max-h-60 overflow-y-auto space-y-2 pr-1">
          <div v-for="c in conflictList" :key="c.bookingId" class="p-3 rounded-xl bg-surface border border-outline-variant/15 text-xs flex items-center justify-between gap-3">
            <div>
              <p class="font-bold text-on-surface text-sm">{{ c.movieTitle }} · <span class="text-primary font-mono">#{{ c.bookingCode }}</span></p>
              <p class="text-on-surface-variant mt-0.5">{{ c.roomName }} · Suất: {{ fmtTime(c.startTime) }} · Ghế: <span class="font-bold text-amber-300">{{ c.seatLabel }}</span></p>
            </div>
            <div class="text-right">
              <p class="font-bold text-on-surface">{{ c.customerName }}</p>
              <p class="font-mono text-primary font-bold mt-0.5"><a :href="`tel:${c.customerPhone}`" class="hover:underline">{{ c.customerPhone || 'Không có SĐT' }}</a></p>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3 pt-2 border-t border-outline-variant/10">
          <button @click="conflictModalOpen = false" class="px-4 py-2 rounded-xl bg-surface text-on-surface text-sm font-semibold hover:bg-white/5">Đóng / Để sau</button>
          <button @click="proceedLockSeat(pendingMaintenanceSeat?.seatId, pendingMaintenanceSeat?.label)" class="px-5 py-2 rounded-xl bg-red-600 text-white text-sm font-extrabold hover:bg-red-500 shadow-lg">Vẫn xác nhận Khóa ghế</button>
        </div>
      </div>
    </div>
  </div>
</template>
