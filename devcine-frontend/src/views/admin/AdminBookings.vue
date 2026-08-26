<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { bookingAdminApi } from '@/api/admin/index'
import { openInvoice, paymentLabel } from '@/utils/invoiceTemplate'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()

const seatTypeLabel = (t) => ({ NORMAL: 'Thường', STANDARD: 'Thường', VIP: 'VIP', SWEETBOX: 'Sweetbox' }[t] || t)
const ticketTypeLabel = (t) => ({ ADULT: 'Người lớn', U22: 'U22 / HSSV', STUDENT: 'HSSV', CHILD: 'Trẻ em', SENIOR: 'Cao tuổi' }[t] || '')

const isLoading = ref(false)
const error = ref('')
const rows = ref([])
const page = ref(0)
const totalPages = ref(1)
const totalElements = ref(0)

const filters = reactive({ q: '', status: '', method: '', hasFnb: '', from: '', to: '' })

const FNB_TABS = [
  { value: '', label: 'Tất cả' },
  { value: 'YES', label: 'Có F&B' },
  { value: 'NO', label: 'Chỉ vé' }
]

const STATUS_TABS = [
  { value: '', label: 'Tất cả' },
  { value: 'CONFIRMED', label: 'Hoàn tất' },
  { value: 'HOLD', label: 'Đang giữ' },
  { value: 'CANCELLED', label: 'Đã huỷ' }
]
const METHODS = ['CASH', 'CARD', 'TRANSFER', 'VNPAY']

// Dropdown phương thức (custom — khớp theme tối)
const methodOpen = ref(false)
const selectMethod = (m) => { filters.method = m; methodOpen.value = false; page.value = 0; fetchBookings() }

const fmt = (n) => Number(n || 0).toLocaleString('vi-VN')
// Bỏ tiền tố "Ô chọn " trong nhãn slot khi hiển thị (VD "Ô chọn Nước 1" → "Nước 1")
const stripSlotPrefix = (label) => (label || '').replace(/^Ô\s*chọn\s*/i, '').trim()
const fmtDateTime = (iso) => {
  if (!iso) return { date: '', time: '' }
  const d = new Date(iso)
  if (isNaN(d)) return { date: '', time: '' }
  return {
    date: d.toLocaleDateString('vi-VN'),
    time: d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  }
}

const statusBadge = (s) => {
  switch ((s || '').toUpperCase()) {
    case 'CONFIRMED': return { label: 'Hoàn tất', cls: 'text-green-400 bg-green-400/10 border-green-400/20' }
    case 'HOLD': return { label: 'Đang giữ', cls: 'text-yellow-400 bg-yellow-400/10 border-yellow-400/20' }
    case 'CANCELLED': return { label: 'Đã huỷ', cls: 'text-red-400 bg-red-400/10 border-red-400/20' }
    case 'EXPIRED': return { label: 'Hết hạn', cls: 'text-amber-400 bg-amber-400/10 border-amber-400/30' }
    default: return { label: s || '—', cls: 'text-gray-400 bg-gray-400/10 border-gray-400/20' }
  }
}

const fetchBookings = async () => {
  isLoading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: 15 }
    if (filters.q.trim()) params.q = filters.q.trim()
    if (filters.status) params.status = filters.status
    if (filters.method) params.method = filters.method
    if (filters.hasFnb) params.hasFnb = filters.hasFnb
    if (filters.from) params.from = filters.from
    if (filters.to) params.to = filters.to
    const { data } = await bookingAdminApi.list(params)
    const result = data.data ?? data
    rows.value = result.content ?? []
    totalPages.value = result.totalPages ?? 1
    totalElements.value = result.totalElements ?? rows.value.length
  } catch (err) {
    error.value = friendlyError(err, 'Không tải được danh sách hoá đơn.')
    rows.value = []
    toast.error(error.value)
  } finally {
    isLoading.value = false
  }
}

let searchTimer = null
const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { page.value = 0; fetchBookings() }, 400)
}
const applyFilter = () => { page.value = 0; fetchBookings() }
const resetFilters = () => {
  filters.q = ''; filters.status = ''; filters.method = ''; filters.hasFnb = ''; filters.from = ''; filters.to = ''
  page.value = 0; fetchBookings()
}
const goPage = (p) => { if (p < 0 || p >= totalPages.value) return; page.value = p; fetchBookings() }

// ===== Detail modal =====
const showDetail = ref(false)
const isLoadingDetail = ref(false)
const detail = ref(null)

const openDetail = async (bookingId, isConcession = false) => {
  showDetail.value = true
  isLoadingDetail.value = true
  detail.value = null
  try {
    const params = isConcession ? { type: 'CONCESSION' } : {}
    const { data } = await bookingAdminApi.detail(bookingId, params)
    detail.value = data.data ?? data
  } catch (err) {
    toast.error(friendlyError(err, 'Không tải được chi tiết hoá đơn.'))
    showDetail.value = false
  } finally {
    isLoadingDetail.value = false
  }
}

const detailSeatTotal = computed(() => (detail.value?.seats || []).reduce((a, s) => a + Number(s.price || 0), 0))
// Gom ghế theo loại ghế + loại vé để danh sách gọn, dễ đối chiếu tiền (thay vì N chip trùng lặp).
const detailSeatGroups = computed(() => {
  const map = {}
  for (const s of (detail.value?.seats || [])) {
    const key = `${s.seatType || 'NORMAL'}|${s.ticketType || ''}`
    if (!map[key]) map[key] = {
      typeLabel: seatTypeLabel(s.seatType), ticketLabel: ticketTypeLabel(s.ticketType),
      seats: [], count: 0, subtotal: 0, unit: Number(s.price || 0)
    }
    map[key].seats.push(s.label)
    map[key].count++
    map[key].subtotal += Number(s.price || 0)
  }
  return Object.values(map)
})
const detailComboTotal = computed(() => (detail.value?.fnbs || []).reduce((a, f) => a + Number(f.lineTotal || (Number(f.price || f.unitPrice || 0) * f.quantity)), 0))
const detailDiscount = computed(() => Math.max(0, Number(detail.value?.totalPrice || 0) - Number(detail.value?.finalPrice || 0)))
const detailCheckedIn = computed(() => (detail.value?.tickets || []).filter(t => t.isCheckedIn).length)
const detailTicketCount = computed(() => (detail.value?.tickets || []).length)

const isPosOrder = computed(() => {
  if (!detail.value) return false
  if (detail.value.isConcession) return true
  const ch = String(detail.value.channel || '').toUpperCase()
  const pm = String(detail.value.paymentMethod || '').toUpperCase()
  return ch.includes('POS') || ch.includes('QUẦY') || pm === 'CASH' || pm.includes('POS')
})

const detailRewardPoints = computed(() => {
  if (!detail.value) return 0
  return Math.floor(Number(detail.value.finalPrice || 0) / 10000)
})

const customerDisplay = computed(() => {
  if (!detail.value) return 'Khách tại quầy'
  if (detail.value.customerName && detail.value.customerName !== 'Khách tại quầy') {
    return detail.value.customerPhone ? `${detail.value.customerName} (${detail.value.customerPhone})` : detail.value.customerName
  }
  return detail.value.customerPhone || 'Khách tại quầy'
})

const isOrderCheckedIn = computed(() => {
  if (!detail.value) return false
  if (detail.value.checkedInAt) return true
  const tickets = detail.value.tickets || []
  if (tickets.length > 0) {
    return tickets.every(t => t.isCheckedIn)
  }
  return false
})

const paymentLabelFull = (m) => {
  const map = {
    CASH: 'CASH (Tiền mặt)',
    VNPAY: 'VNPAY (Trực tuyến)',
    MOMO: 'MOMO (Ví điện tử)',
    ZALOPAY: 'ZALOPAY (Ví điện tử)',
    TRANSFER: 'Chuyển khoản (VietQR)',
    CARD: 'Thẻ POS (Ngân hàng)',
    MEMBER_WALLET: 'Ví thành viên'
  }
  return map[m] || m || 'Chưa xác định'
}

const buildInv = (d) => {
  return {
    bookingCode: d.bookingCode,
    movieTitle: d.isConcession ? '' : d.movieTitle,
    format: d.formatName || '2D',
    roomName: d.roomName,
    roomType: 'Standard',
    startTime: d.showtimeStart,
    posTerminal: '01',
    cashierName: d.checkedInBy || 'Thu ngân',
    cinemaName: d.cinemaName || 'DEVCINE CINEMA',
    cinemaAddress: d.cinemaAddress || 'Tầng 3, TTTM DevCine Plaza, Hà Nội',
    printedAt: d.checkedInAt || new Date(),
    seats: (d.seats || []).map(s => ({
      seatLabel: s.label,
      ticketType: s.ticketType || 'ADULT',
      price: Number(s.price || 0)
    })),
    fnbs: (d.fnbs || []).map(f => ({
      name: f.name,
      quantity: f.quantity,
      price: Number(f.unitPrice || f.price || 0),
      options: f.options || []
    })),
    paymentMethod: d.paymentMethod,
    ticketDiscount: Number(d.discountAmount || 0),
    fnbDiscount: 0,
    memberName: d.customerName && d.customerName !== 'Khách tại quầy' ? d.customerName : null,
    memberTier: d.membershipTier
  }
}

const reprint = async (bookingId, isConcession = false) => {
  let d = detail.value
  if (!d || d.bookingId !== bookingId || Boolean(d.isConcession) !== Boolean(isConcession)) {
    try {
      const params = isConcession ? { type: 'CONCESSION' } : {}
      const { data } = await bookingAdminApi.detail(bookingId, params)
      d = data.data ?? data
    } catch (err) {
      toast.error(friendlyError(err, 'Không tải được dữ liệu để in.'))
      return
    }
  }
  const ok = openInvoice(buildInv(d))
  if (!ok) toast.warning('Trình duyệt đã chặn cửa sổ. Hãy cho phép pop-up để in.')
}

const exportCsv = () => {
  if (rows.value.length === 0) { toast.info('Không có dữ liệu để xuất.'); return }
  const header = ['Ma don', 'Thoi gian', 'Khach hang', 'Phim / Dich vu', 'So ghe', 'Phuong thuc', 'Tong tien', 'Trang thai']
  const lines = rows.value.map(r => [
    r.bookingCode, r.createdAt, r.customerName, r.isConcession ? 'Bán nhanh F&B' : r.movieTitle, r.isConcession ? 0 : r.seatCount,
    r.paymentMethod, r.finalPrice, r.status
  ].map(v => `"${String(v ?? '').replace(/"/g, '""')}"`).join(','))
  const csv = '﻿' + [header.join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `hoa-don-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(fetchBookings)
onUnmounted(() => { if (searchTimer) clearTimeout(searchTimer) })
</script>

<template>
  <div class="h-full flex flex-col space-y-6 p-10">
    <!-- Header -->
    <div class="flex justify-between items-end flex-shrink-0">
      <div>
        <h1 class="text-3xl font-black text-on-surface tracking-tighter uppercase italic">Quản lý <span class="text-primary">Hoá đơn</span></h1>
        <p class="text-sm font-bold text-on-surface-variant uppercase tracking-widest mt-1">Toàn bộ đơn đặt vé &amp; bắp nước tại quầy · {{ totalElements }} đơn</p>
      </div>
      <button @click="exportCsv" class="px-6 py-3 bg-surface-container-high hover:bg-white/10 text-on-surface font-bold text-xs uppercase tracking-widest rounded transition-colors flex items-center gap-2 border border-outline-variant/20">
        <span class="material-symbols-outlined text-sm">download</span> Xuất CSV
      </button>
    </div>

    <!-- Toolbar -->
    <div class="bg-surface-container-low p-3 rounded-2xl border border-outline-variant/10 flex flex-wrap items-center gap-3 shadow-xl flex-shrink-0">
      <div class="relative flex-grow min-w-[240px] group">
        <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant group-focus-within:text-primary transition-colors">search</span>
        <input v-model="filters.q" @input="onSearchInput" type="text" placeholder="Tìm theo mã đơn, tên khách..."
               class="w-full h-11 bg-surface-container-highest border border-outline-variant/10 rounded-xl pl-12 pr-4 text-sm text-on-surface placeholder:text-on-surface-variant/50 outline-none hover:border-outline-variant/30 focus:border-primary/60 focus:ring-2 focus:ring-primary/15 transition-all">
      </div>

      <div class="flex items-center gap-1 bg-surface-container-highest p-1 rounded-xl border border-outline-variant/10 h-11">
        <button v-for="t in STATUS_TABS" :key="t.value" @click="filters.status = t.value; applyFilter()"
                :class="filters.status === t.value ? 'bg-primary text-on-primary shadow-lg shadow-primary/20' : 'text-on-surface-variant hover:text-on-surface hover:bg-white/5'"
                class="px-4 h-full text-[10px] font-bold uppercase tracking-widest rounded-lg transition-all">{{ t.label }}</button>
      </div>

      <!-- Lọc theo F&B -->
      <div class="flex items-center gap-1 bg-surface-container-highest p-1 rounded-xl border border-outline-variant/10 h-11">
        <button v-for="t in FNB_TABS" :key="t.value" @click="filters.hasFnb = t.value; applyFilter()"
                :class="filters.hasFnb === t.value ? 'bg-primary text-on-primary shadow-lg shadow-primary/20' : 'text-on-surface-variant hover:text-on-surface hover:bg-white/5'"
                class="px-4 h-full text-[10px] font-bold uppercase tracking-widest rounded-lg transition-all">{{ t.label }}</button>
      </div>

      <div class="relative h-11">
        <button @click="methodOpen = !methodOpen" type="button"
                class="h-full w-[190px] bg-surface-container-highest border rounded-xl pl-10 pr-9 text-sm text-left text-on-surface outline-none cursor-pointer transition-all relative flex items-center"
                :class="methodOpen ? 'border-primary/60 ring-2 ring-primary/15' : 'border-outline-variant/10 hover:border-outline-variant/30'">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-lg">payments</span>
          <span class="truncate" :class="filters.method ? 'text-on-surface font-semibold' : 'text-on-surface-variant'">{{ filters.method ? paymentLabel(filters.method) : 'Mọi phương thức' }}</span>
          <span class="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-on-surface-variant text-lg transition-transform" :class="{ 'rotate-180': methodOpen }">expand_more</span>
        </button>

        <div v-if="methodOpen" class="fixed inset-0 z-[55]" @click="methodOpen = false"></div>
        <transition name="fade">
          <div v-if="methodOpen" class="absolute left-0 top-full mt-2 w-full min-w-[190px] bg-surface-container-high border border-outline-variant/20 rounded-xl shadow-[0_10px_40px_-10px_var(--shadow-color)] z-[60] overflow-hidden py-1">
            <button @click="selectMethod('')" type="button"
                    class="w-full flex items-center gap-2 px-4 py-2.5 text-sm text-left transition-colors"
                    :class="!filters.method ? 'text-primary bg-primary/10 font-semibold' : 'text-on-surface-variant hover:bg-white/5 hover:text-on-surface'">
              <span class="material-symbols-outlined text-base">apps</span> Mọi phương thức
            </button>
            <button v-for="m in METHODS" :key="m" @click="selectMethod(m)" type="button"
                    class="w-full flex items-center justify-between px-4 py-2.5 text-sm text-left transition-colors"
                    :class="filters.method === m ? 'text-primary bg-primary/10 font-semibold' : 'text-on-surface-variant hover:bg-white/5 hover:text-on-surface'">
              <span>{{ paymentLabel(m) }}</span>
              <span v-if="filters.method === m" class="material-symbols-outlined text-base">check</span>
            </button>
          </div>
        </transition>
      </div>

      <div class="flex items-center gap-2 h-11 px-3 bg-surface-container-highest border border-outline-variant/10 rounded-xl hover:border-outline-variant/30 transition-colors">
        <span class="material-symbols-outlined text-on-surface-variant text-lg">calendar_month</span>
        <input v-model="filters.from" @change="applyFilter" type="date" title="Từ ngày" class="bg-transparent text-xs text-on-surface outline-none cursor-pointer w-[108px]">
        <span class="text-on-surface-variant/50 text-xs">→</span>
        <input v-model="filters.to" @change="applyFilter" type="date" title="Đến ngày" class="bg-transparent text-xs text-on-surface outline-none cursor-pointer w-[108px]">
      </div>

      <button @click="resetFilters" title="Đặt lại bộ lọc"
              class="h-11 px-4 bg-surface-container-highest border border-outline-variant/10 text-on-surface-variant hover:text-primary hover:border-primary/40 text-xs font-bold uppercase tracking-widest rounded-xl transition-all flex items-center gap-1.5">
        <span class="material-symbols-outlined text-base">restart_alt</span> Đặt lại
      </button>
    </div>

    <!-- Table -->
    <div class="flex-grow bg-surface-container-low rounded-xl border border-outline-variant/10 flex flex-col overflow-hidden shadow-2xl">
      <div class="grid grid-cols-12 gap-4 p-4 border-b border-outline-variant/10 bg-surface-container-high text-[11px] font-black uppercase tracking-widest text-on-surface-variant sticky top-0 z-10">
        <div class="col-span-2 pl-4">Thời gian</div>
        <div class="col-span-2">Mã đơn</div>
        <div class="col-span-3">Khách hàng / Nội dung</div>
        <div class="col-span-1 text-center">Ghế</div>
        <div class="col-span-1">Phương thức</div>
        <div class="col-span-1 text-right">Tổng tiền</div>
        <div class="col-span-1 text-center">Trạng thái</div>
        <div class="col-span-1 text-center">Thao tác</div>
      </div>

      <div class="flex-grow overflow-y-auto">
        <!-- Loading -->
        <div v-if="isLoading" class="p-6 space-y-3">
          <div v-for="i in 8" :key="i" class="h-14 bg-surface-container-high rounded-lg animate-pulse"></div>
        </div>

        <!-- Error -->
        <div v-else-if="error" class="p-16 text-center">
          <span class="material-symbols-outlined text-5xl text-red-400/60 mb-3">error</span>
          <p class="text-sm font-bold text-red-400">{{ error }}</p>
        </div>

        <!-- Empty -->
        <div v-else-if="rows.length === 0" class="p-16 flex flex-col items-center justify-center text-center">
          <span class="material-symbols-outlined text-6xl text-on-surface-variant/30 mb-4">receipt_long</span>
          <p class="text-sm font-black text-on-surface uppercase tracking-widest">Không có hoá đơn</p>
          <p class="text-[10px] text-on-surface-variant mt-2 uppercase">Thử đổi bộ lọc hoặc khoảng thời gian</p>
        </div>

        <!-- Rows -->
        <template v-else>
          <div v-for="r in rows" :key="`${r.orderType || 'TICKET'}-${r.bookingId}`"
               class="grid grid-cols-12 gap-4 p-4 border-b border-outline-variant/5 items-center hover:bg-white/[0.02] transition-colors group">
            <div class="col-span-2 pl-4">
              <p class="text-base font-black text-on-surface">{{ fmtDateTime(r.createdAt).time }}</p>
              <p class="text-xs font-bold text-on-surface-variant">{{ fmtDateTime(r.createdAt).date }}</p>
            </div>
            <div class="col-span-2">
              <p class="text-sm font-black text-primary font-mono">{{ r.bookingCode || '—' }}</p>
              <p class="text-[11px] font-bold text-on-surface-variant uppercase mt-0.5">{{ r.channel }}</p>
            </div>
            <div class="col-span-3 min-w-0">
              <div class="flex items-center gap-2">
                <p class="text-base font-bold text-on-surface truncate">{{ r.customerName }}</p>
                <span v-if="r.isConcession" class="shrink-0 inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded text-[9px] font-black uppercase tracking-wider text-amber-400 bg-amber-400/10 border border-amber-400/20" :title="`${r.fnbItemCount} món F&B`">
                  <span class="material-symbols-outlined text-[11px]">fastfood</span> Bán nhanh F&amp;B
                </span>
                <span v-else-if="r.hasFnb" class="shrink-0 inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded text-[9px] font-black uppercase tracking-wider text-amber-400 bg-amber-400/10 border border-amber-400/20" :title="`${r.fnbItemCount} món F&B`">
                  <span class="material-symbols-outlined text-[11px]">lunch_dining</span> Vé + F&amp;B
                </span>
              </div>
              <p class="text-xs text-on-surface-variant truncate">
                <template v-if="r.isConcession">Bắp nước &amp; Combo · {{ r.roomName }}</template>
                <template v-else>{{ r.movieTitle }} · {{ r.roomName }}</template>
              </p>
            </div>
            <div class="col-span-1 text-center">
              <span class="text-base font-black text-on-surface">{{ r.isConcession ? '—' : r.seatCount }}</span>
            </div>
            <div class="col-span-1">
              <span class="text-xs font-bold text-on-surface-variant uppercase">{{ paymentLabel(r.paymentMethod) }}</span>
            </div>
            <div class="col-span-1 text-right">
              <p class="text-base font-black text-primary italic tabular-nums">{{ fmt(r.finalPrice) }}đ</p>
              <p v-if="Number(r.totalPrice) > Number(r.finalPrice)" class="text-[10px] text-on-surface-variant line-through tabular-nums">{{ fmt(r.totalPrice) }}đ</p>
            </div>
            <div class="col-span-1 flex justify-center">
              <span :class="statusBadge(r.status).cls" class="inline-flex px-2.5 py-1 rounded text-[10px] font-black uppercase tracking-widest border">{{ statusBadge(r.status).label }}</span>
            </div>
            <div class="col-span-1 flex justify-center gap-1">
              <button @click="openDetail(r.bookingId, r.isConcession)" title="Xem chi tiết" class="w-8 h-8 rounded-lg flex items-center justify-center text-on-surface-variant hover:bg-primary/10 hover:text-primary transition-colors">
                <span class="material-symbols-outlined text-lg">visibility</span>
              </button>
              <button @click="reprint(r.bookingId, r.isConcession)" title="In lại hoá đơn" class="w-8 h-8 rounded-lg flex items-center justify-center text-on-surface-variant hover:bg-primary/10 hover:text-primary transition-colors">
                <span class="material-symbols-outlined text-lg">print</span>
              </button>
            </div>
          </div>
        </template>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-center gap-4 p-4 border-t border-outline-variant/10 bg-surface-container-high flex-shrink-0">
        <button @click="goPage(page - 1)" :disabled="page === 0" class="px-4 py-2 text-xs font-bold uppercase tracking-widest bg-surface-container-highest rounded disabled:opacity-40 hover:bg-white/10 transition-colors">Trước</button>
        <span class="text-xs text-on-surface-variant font-mono">Trang {{ page + 1 }} / {{ totalPages }}</span>
        <button @click="goPage(page + 1)" :disabled="page >= totalPages - 1" class="px-4 py-2 text-xs font-bold uppercase tracking-widest bg-surface-container-highest rounded disabled:opacity-40 hover:bg-white/10 transition-colors">Tiếp</button>
      </div>
    </div>

    <!-- Detail modal -->
    <transition name="fade">
      <div v-if="showDetail" class="fixed inset-0 z-[1200] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4" @click.self="showDetail = false">
        <div class="w-full max-w-2xl max-h-[90vh] bg-surface border border-outline-variant/15 rounded-3xl shadow-2xl overflow-hidden flex flex-col">
          <!-- Header Modal -->
          <div class="px-7 py-5 border-b border-outline-variant/10 flex items-center justify-between">
            <div class="flex items-center gap-3">
              <span class="material-symbols-outlined text-primary text-2xl">receipt_long</span>
              <h3 class="text-xl font-headline font-extrabold uppercase tracking-wide text-on-surface">Chi tiết hoá đơn</h3>
            </div>
            <button @click="showDetail = false" class="w-8 h-8 rounded-full flex items-center justify-center text-on-surface-variant hover:text-on-surface hover:bg-white/5 transition-colors">
              <span class="material-symbols-outlined text-xl">close</span>
            </button>
          </div>

          <div v-if="isLoadingDetail" class="p-16 flex items-center justify-center">
            <span class="material-symbols-outlined animate-spin text-primary text-3xl">progress_activity</span>
          </div>

          <div v-else-if="detail" class="p-7 space-y-6 overflow-y-auto custom-scrollbar">
            <!-- Header Info: Mã đơn & Trạng thái & Kênh -->
            <div class="flex justify-between items-start gap-4">
              <div>
                <p class="text-[10px] font-black text-primary uppercase tracking-widest">Mã đơn</p>
                <p class="text-2xl font-black text-on-surface font-mono tracking-tight">{{ detail.bookingCode }}</p>
                <p class="text-xs text-on-surface-variant font-medium mt-1">
                  {{ fmtDateTime(detail.createdAt).date }} {{ fmtDateTime(detail.createdAt).time }} &bull; 
                  <span class="font-bold uppercase">{{ isPosOrder ? (detail.isConcession ? 'Bán nhanh F&B tại quầy' : 'Bán tại quầy') : 'Đặt vé trực tuyến (Online)' }}</span>
                </p>
              </div>
              <span :class="statusBadge(detail.status).cls" class="inline-flex px-3.5 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-widest border shadow-sm">
                {{ statusBadge(detail.status).label }}
              </span>
            </div>

            <!-- Two Cards: Suất chiếu & Thông tin thanh toán -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <!-- Cột trái: Suất chiếu (hoặc Loại đơn) -->
              <div v-if="detail.isConcession" class="p-4 rounded-2xl bg-surface-container-high border border-outline-variant/10 flex flex-col justify-between">
                <div>
                  <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest mb-1.5">Loại đơn</p>
                  <p class="text-base font-extrabold text-on-surface">Bán nhanh bắp nước (F&amp;B)</p>
                  <p class="text-xs text-on-surface-variant mt-1">{{ detail.roomName || 'Quầy Concession' }}</p>
                  <p class="text-xs text-on-surface-variant mt-0.5">{{ fmtDateTime(detail.createdAt).date }} {{ fmtDateTime(detail.createdAt).time }}</p>
                </div>
                <p class="text-xs text-on-surface-variant mt-2 pt-2 border-t border-outline-variant/10 font-medium">
                  Rạp: <span class="text-on-surface font-bold">{{ detail.cinemaName || 'DevCine Landmark 81' }}</span>
                </p>
              </div>
              <div v-else class="p-4 rounded-2xl bg-surface-container-high border border-outline-variant/10 flex flex-col justify-between">
                <div>
                  <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest mb-1.5">Suất chiếu</p>
                  <p class="text-base font-extrabold text-on-surface">{{ detail.movieTitle }}</p>
                  <p class="text-xs text-on-surface-variant mt-1">{{ detail.formatName || '2D Phụ Đề' }} &bull; {{ detail.roomName }}</p>
                  <p class="text-xs text-on-surface-variant mt-0.5 font-medium">
                    {{ fmtDateTime(detail.showtimeStart).date }} {{ fmtDateTime(detail.showtimeStart).time }}{{ detail.showtimeEnd ? ` ~ ${fmtDateTime(detail.showtimeEnd).time}` : '' }}
                  </p>
                </div>
                <p class="text-xs text-on-surface-variant mt-2 pt-2 border-t border-outline-variant/10 font-medium">
                  Rạp: <span class="text-on-surface font-bold">{{ detail.cinemaName || 'DevCine Landmark 81' }}</span>
                </p>
              </div>

              <!-- Cột phải: Thông tin thanh toán -->
              <div class="p-4 rounded-2xl bg-surface-container-high border border-outline-variant/10 flex flex-col justify-between space-y-1.5">
                <div>
                  <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest mb-1.5">Thông tin thanh toán</p>
                  <p class="text-sm font-bold text-on-surface">
                    Khách hàng: <span class="font-bold">{{ customerDisplay }}</span>
                  </p>
                  <p class="text-xs font-semibold text-primary mt-0.5">
                    {{ detail.membershipTier ? `Hạng ${detail.membershipTier} (+${detailRewardPoints} điểm)` : (detail.customerName && detail.customerName !== 'Khách tại quầy' ? `Thành viên (+${detailRewardPoints} điểm)` : 'Khách vãng lai') }}
                  </p>
                </div>
                <div class="pt-2 border-t border-outline-variant/10 space-y-0.5">
                  <p class="text-xs text-on-surface-variant">
                    Kênh: <b class="text-on-surface font-semibold">{{ isPosOrder ? 'Quầy / POS' : 'Website / App' }}</b>
                  </p>
                  <p v-if="isPosOrder" class="text-xs text-on-surface-variant">
                    Thu ngân: <b class="text-on-surface font-semibold">{{ detail.checkedInBy || 'Đỗ Hoàng Minh' }}</b>
                  </p>
                  <p v-else class="text-xs text-on-surface-variant truncate">
                    Cổng TT: <b class="text-on-surface font-semibold">{{ paymentLabel(detail.paymentMethod) }}</b>
                    <span v-if="detail.paymentRef" class="font-mono text-[11px] text-on-surface-variant/80 ml-1">(Mã GD: #{{ detail.paymentRef }})</span>
                  </p>
                </div>
              </div>
            </div>

            <!-- VÉ XEM PHIM (Ẩn khi là đơn F&B lẻ không có ghế) -->
            <div v-if="detail.seats && detail.seats.length">
              <div class="flex items-center gap-2 mb-2">
                <span class="w-1 h-3.5 rounded-full bg-primary"></span>
                <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest">
                  Vé xem phim ({{ detail.seats.length }})
                </p>
              </div>
              <div class="rounded-2xl bg-surface-container-high border border-outline-variant/10 divide-y divide-outline-variant/5 overflow-hidden">
                <div v-for="(g, i) in detailSeatGroups" :key="i" class="flex items-center justify-between p-3.5 px-4 hover:bg-white/[0.02] transition-colors">
                  <div class="min-w-0">
                    <p class="text-sm font-bold text-on-surface">
                      {{ g.typeLabel }}<span v-if="g.ticketLabel" class="text-on-surface-variant font-medium"> - {{ g.ticketLabel }}</span>
                      <span class="text-primary font-black ml-1.5">x{{ g.count }}</span>
                    </p>
                    <p class="text-[11px] text-on-surface-variant/80 mt-0.5 font-mono">Ghế: {{ g.seats.join(', ') }}</p>
                  </div>
                  <div class="text-right shrink-0">
                    <p class="text-sm font-black text-on-surface tabular-nums">{{ fmt(g.subtotal) }} đ</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- MÃ QR ĐƠN HÀNG (Khung QR trung tâm duy nhất đại diện cho toàn bộ đơn hàng) -->
            <div v-if="!detail.isConcession">
              <div class="flex items-center justify-between mb-2">
                <div class="flex items-center gap-2">
                  <span class="w-1 h-3.5 rounded-full bg-primary"></span>
                  <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest">Mã QR đơn hàng</p>
                </div>
                <span
                  class="px-2.5 py-1 rounded-full text-[9px] font-black uppercase tracking-widest border"
                  :class="isOrderCheckedIn
                    ? 'text-green-400 bg-green-400/10 border-green-400/20'
                    : 'text-amber-400 bg-amber-400/10 border-amber-400/20'"
                >
                  {{ isOrderCheckedIn ? 'ĐÃ CHECK-IN' : 'CHƯA CHECK-IN' }}
                </span>
              </div>

              <div class="p-5 rounded-2xl bg-surface-container-high border border-outline-variant/10 flex flex-col items-center justify-center text-center space-y-3">
                <div class="p-2 bg-white rounded-xl shadow-lg border border-black/10 inline-block">
                  <img
                    :src="`https://api.qrserver.com/v1/create-qr-code/?size=130x130&margin=0&data=${encodeURIComponent(detail.bookingCode)}`"
                    alt="QR"
                    class="w-28 h-28 block"
                  />
                </div>
                <div>
                  <p class="text-base font-black text-on-surface font-mono tracking-wider">{{ detail.bookingCode }}</p>
                  <p class="text-[11px] text-on-surface-variant mt-0.5">Mã QR đại diện cho toàn bộ đơn hàng</p>
                </div>
              </div>
            </div>

            <!-- BẮP NƯỚC & COMBO -->
            <div v-if="detail.fnbs && detail.fnbs.length">
              <div class="flex items-center gap-2 mb-2">
                <span class="w-1 h-3.5 rounded-full bg-primary"></span>
                <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest">Bắp nước &amp; Combo</p>
              </div>
              <div class="rounded-2xl bg-surface-container-high border border-outline-variant/10 divide-y divide-outline-variant/5 overflow-hidden">
                <div v-for="(f, i) in detail.fnbs" :key="i" class="p-3.5 px-4 hover:bg-white/[0.02] transition-colors">
                  <div class="flex justify-between items-start gap-3">
                    <div class="min-w-0">
                      <div class="flex items-baseline gap-2">
                        <p class="text-sm font-bold text-on-surface">{{ f.name }}</p>
                        <span class="text-xs font-black text-primary">x{{ f.quantity }}</span>
                      </div>
                      <!-- Danh sách vị/option kèm phụ thu -->
                      <div v-if="f.options && f.options.length" class="mt-1.5 space-y-0.5">
                        <p v-for="(o, oi) in f.options" :key="oi" class="text-xs text-on-surface-variant flex items-center gap-1">
                          <span class="text-on-surface-variant/60">&bull;</span>
                          <span>{{ o.optionName }}</span>
                          <span v-if="Number(o.surcharge) > 0" class="text-amber-400 font-semibold">(+{{ fmt(o.surcharge) }}đ)</span>
                        </p>
                      </div>
                    </div>
                    <span class="text-sm font-black text-on-surface tabular-nums shrink-0">{{ fmt(f.lineTotal || (f.price || f.unitPrice || 0) * f.quantity) }} đ</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- TẠM TÍNH & TỔNG THANH TOÁN -->
            <div class="pt-4 border-t border-outline-variant/10 space-y-2">
              <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest mb-1">Tạm tính</p>
              <div v-if="!detail.isConcession && detailSeatTotal > 0" class="flex justify-between text-sm text-on-surface-variant">
                <span>Tiền vé:</span>
                <span class="tabular-nums font-semibold">{{ fmt(detailSeatTotal) }} đ</span>
              </div>
              <div v-if="detailComboTotal > 0" class="flex justify-between text-sm text-on-surface-variant">
                <span>Bắp nước &amp; combo:</span>
                <span class="tabular-nums font-semibold">{{ fmt(detailComboTotal) }} đ</span>
              </div>
              <div class="flex justify-between text-sm text-on-surface-variant">
                <span>Giảm giá:</span>
                <span class="tabular-nums font-semibold" :class="detailDiscount > 0 ? 'text-green-400' : ''">
                  {{ detailDiscount > 0 ? `−${fmt(detailDiscount)} đ` : '0 đ' }}
                </span>
              </div>

              <div class="flex justify-between items-baseline pt-3 border-t border-outline-variant/15">
                <span class="text-xs font-headline font-extrabold text-on-surface uppercase tracking-widest">TỔNG THANH TOÁN:</span>
                <span class="text-2xl font-headline font-extrabold text-primary tabular-nums">{{ fmt(detail.finalPrice) }} đ</span>
              </div>

              <div class="flex justify-between items-center text-xs text-on-surface-variant pt-1">
                <span>Phương thức: <b class="text-on-surface font-semibold">{{ paymentLabelFull(detail.paymentMethod) }}</b></span>
                <span class="inline-flex px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-widest text-green-400 bg-green-400/10 border border-green-400/20">
                  [ĐÃ THANH TOÁN]
                </span>
              </div>
            </div>
          </div>

          <!-- Footer Buttons -->
          <div class="px-7 py-4 border-t border-outline-variant/10 flex justify-end gap-3 flex-shrink-0">
            <button @click="showDetail = false" class="px-6 py-3 rounded-xl text-on-surface-variant hover:text-on-surface hover:bg-white/5 font-bold text-xs uppercase tracking-widest transition-colors">
              Đóng
            </button>
            <button v-if="detail" @click="reprint(detail.bookingId, detail.isConcession)" class="px-6 py-3 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded-xl hover:brightness-110 transition-all flex items-center gap-2 shadow-lg shadow-primary/20">
              <span class="material-symbols-outlined text-base">print</span>
              {{ isPosOrder ? 'IN LẠI VÉ / HOÁ ĐƠN' : 'IN VÉ / HOÁ ĐƠN' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s, transform 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(8px); }
</style>
