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
  { value: 'EXPIRED', label: 'Hết hạn' },
  { value: 'CANCELLED', label: 'Đã huỷ' }
]
const METHODS = ['CASH', 'CARD', 'TRANSFER', 'VNPAY']

// Dropdown phương thức (custom — khớp theme tối)
const methodOpen = ref(false)
const selectMethod = (m) => { filters.method = m; methodOpen.value = false; page.value = 0; fetchBookings() }

const fmt = (n) => Number(n || 0).toLocaleString('vi-VN')
const fnbTotalSurcharge = (f) => (f?.options || []).reduce((sum, o) => sum + Number(o.surcharge || 0), 0)
const fnbBasePrice = (f) => {
  if (f?.basePrice != null && Number(f.basePrice) > 0) return Number(f.basePrice)
  const sur = fnbTotalSurcharge(f)
  const unit = Number(f?.unitPrice || f?.price || 0)
  return unit > sur ? unit - sur : unit
}
const fnbLineTotal = (f) => {
  if (f?.lineTotal != null && Number(f.lineTotal) > 0) return Number(f.lineTotal)
  const qty = Number(f?.quantity || 1)
  const unit = Number(f?.unitPrice || f?.price || 0)
  if (unit > 0) return unit * qty
  const base = fnbBasePrice(f)
  const sur = fnbTotalSurcharge(f)
  return (base + sur) * qty
}
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
    case 'CONFIRMED':
    case 'COMPLETED': return { label: 'Hoàn tất', cls: 'text-green-400 bg-green-400/10 border-green-400/20' }
    case 'HOLD':
    case 'PENDING_PAYMENT':
    case 'PAYING': return { label: 'Đang giữ', cls: 'text-yellow-400 bg-yellow-400/10 border-yellow-400/20' }
    case 'CANCELLED': return { label: 'Đã huỷ', cls: 'text-red-400 bg-red-400/10 border-red-400/20' }
    case 'EXPIRED': return { label: 'Hết hạn', cls: 'text-amber-400 bg-amber-400/10 border-amber-400/30' }
    default: return { label: s || '—', cls: 'text-gray-400 bg-gray-400/10 border-gray-400/20' }
  }
}

const paymentStatusBadge = (s) => {
  switch ((s || '').toUpperCase()) {
    case 'CONFIRMED':
    case 'COMPLETED':
      return { label: 'ĐÃ THANH TOÁN', cls: 'text-green-400 bg-green-400/10 border-green-400/20' }
    case 'HOLD':
    case 'PENDING_PAYMENT':
    case 'PAYING':
      return { label: 'CHỜ THANH TOÁN', cls: 'text-yellow-400 bg-yellow-400/10 border-yellow-400/20' }
    case 'CANCELLED':
    case 'EXPIRED':
    default:
      return { label: 'CHƯA THANH TOÁN', cls: 'text-amber-400/90 bg-amber-400/10 border-amber-400/20' }
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
const detailComboTotal = computed(() => {
  if (!detail.value?.fnbs?.length) return 0
  return detail.value.fnbs.reduce((a, f) => a + fnbLineTotal(f), 0)
})
const detailDiscount = computed(() => {
  if (!detail.value) return 0
  if (detail.value.discountAmount != null && Number(detail.value.discountAmount) > 0) {
    return Number(detail.value.discountAmount)
  }
  return Math.max(0, Number(detail.value?.totalPrice || 0) - Number(detail.value?.finalPrice || 0))
})
const detailFinalPrice = computed(() => {
  if (!detail.value) return 0
  if (detail.value.finalPrice != null && Number(detail.value.finalPrice) >= 0) {
    return Number(detail.value.finalPrice)
  }
  if (detail.value.isConcession) {
    return Math.max(0, detailComboTotal.value - detailDiscount.value)
  }
  return Math.max(0, detailSeatTotal.value + detailComboTotal.value - detailDiscount.value)
})
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
  const status = String(detail.value.status || '').toUpperCase()
  if (status !== 'CONFIRMED' && status !== 'COMPLETED') return 0
  return Math.floor(Number(detailFinalPrice.value || detail.value.finalPrice || 0) / 10000)
})

const tierStyle = (tier) => {
  const t = (tier || 'BRONZE').toUpperCase()
  return {
    PLATINUM: 'bg-sky-500/15 text-sky-300 border-sky-500/30',
    GOLD: 'bg-amber-500/15 text-amber-400 border-amber-500/30',
    SILVER: 'bg-slate-400/15 text-slate-300 border-slate-400/30',
    BRONZE: 'bg-amber-800/15 text-amber-600 border-amber-800/30'
  }[t] || 'bg-white/10 text-on-surface-variant border-white/10'
}

const fmtPhone = (phone) => {
  if (!phone) return ''
  const str = String(phone).trim()
  const cleaned = str.replace(/\D/g, '')
  if (cleaned.length === 10) {
    return `${cleaned.slice(0, 4)} ${cleaned.slice(4, 7)} ${cleaned.slice(7)}`
  }
  if (cleaned.length === 11) {
    return `${cleaned.slice(0, 4)} ${cleaned.slice(4, 7)} ${cleaned.slice(7)}`
  }
  return str
}

const customerNameDisplay = computed(() => {
  if (!detail.value) return 'Khách tại quầy'
  if (detail.value.customerName && detail.value.customerName.trim()) {
    return detail.value.customerName
  }
  return detail.value.customerPhone ? 'Khách hàng' : 'Khách tại quầy'
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

const isShowtimePast = computed(() => {
  if (!detail.value) return false
  const timeStr = detail.value.showtimeEnd || detail.value.showtimeStart
  if (!timeStr) return false
  const t = new Date(timeStr)
  return !isNaN(t.getTime()) && t < new Date()
})

const isQrDisabled = computed(() => {
  const status = String(detail.value?.status || '').toUpperCase()
  if (status !== 'CONFIRMED' && status !== 'COMPLETED') return true
  if (!isOrderCheckedIn.value && isShowtimePast.value) return true
  return false
})

const paymentLabelFull = (m) => {
  const map = {
    CASH: 'Tiền mặt',
    VNPAY: 'VNPAY (Trực tuyến)',
    MOMO: 'Ví MoMo',
    ZALOPAY: 'Ví ZaloPay',
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
      price: Number(fnbBasePrice(f) || f.unitPrice || f.price || 0),
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
  const status = (d?.status || '').toUpperCase()
  if (status !== 'CONFIRMED' && status !== 'COMPLETED') {
    toast.warning('Không thể in vé/hoá đơn cho đơn hàng chưa hoàn tất thanh toán.')
    return
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
        <p class="text-sm font-bold text-on-surface-variant uppercase tracking-widest mt-1">Toàn bộ đơn đặt vé &amp; bắp nước · {{ totalElements }} đơn</p>
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
              <button
                @click="reprint(r.bookingId, r.isConcession)"
                :disabled="r.status !== 'CONFIRMED' && r.status !== 'COMPLETED'"
                :title="r.status === 'CONFIRMED' || r.status === 'COMPLETED' ? 'In lại hoá đơn' : 'Không thể in cho đơn chưa hoàn tất thanh toán'"
                class="w-8 h-8 rounded-lg flex items-center justify-center transition-colors"
                :class="r.status === 'CONFIRMED' || r.status === 'COMPLETED'
                  ? 'text-on-surface-variant hover:bg-primary/10 hover:text-primary cursor-pointer'
                  : 'text-on-surface-variant/20 cursor-not-allowed opacity-30'"
              >
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

    <!-- Detail modal (Bố cục 2 Cột Dashboard Master-Detail) -->
    <transition name="fade">
      <div v-if="showDetail" class="fixed inset-0 z-[1200] flex items-center justify-center bg-black/75 backdrop-blur-md p-4 sm:p-6" @click.self="showDetail = false">
        <div class="w-full max-w-4xl max-h-[90vh] bg-surface border border-outline-variant/20 rounded-3xl shadow-2xl overflow-hidden flex flex-col animate-in fade-in zoom-in-95 duration-200">
          <!-- Header Modal -->
          <div class="px-6 py-4 border-b border-outline-variant/10 flex items-center justify-between flex-shrink-0 bg-surface-container-high/50">
            <div class="flex items-center gap-2.5">
              <span class="material-symbols-outlined text-primary text-2xl">receipt_long</span>
              <h3 class="text-lg font-headline font-extrabold uppercase tracking-wide text-on-surface">Chi tiết hoá đơn</h3>
            </div>
            <button @click="showDetail = false" class="w-8 h-8 rounded-full flex items-center justify-center text-on-surface-variant hover:text-on-surface hover:bg-white/10 transition-colors">
              <span class="material-symbols-outlined text-xl">close</span>
            </button>
          </div>

          <!-- Loading State -->
          <div v-if="isLoadingDetail" class="p-20 flex flex-col items-center justify-center gap-3">
            <span class="material-symbols-outlined animate-spin text-primary text-4xl">progress_activity</span>
            <p class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Đang tải thông tin...</p>
          </div>

          <!-- Detail Body -->
          <div v-else-if="detail" class="p-6 space-y-5 overflow-y-auto custom-scrollbar flex-grow">
            <!-- Top Summary Bar: Mã đơn, Thời gian, Kênh, Trạng thái -->
            <div class="p-4 rounded-2xl bg-surface-container-high/60 border border-outline-variant/10 flex flex-wrap items-center justify-between gap-3">
              <div class="flex items-center gap-3 min-w-0">
                <div class="w-10 h-10 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary shrink-0">
                  <span class="material-symbols-outlined text-xl">confirmation_number</span>
                </div>
                <div class="min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="text-base font-black text-on-surface font-mono tracking-tight">{{ detail.bookingCode }}</span>
                    <span class="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded bg-white/5 text-on-surface-variant border border-outline-variant/10">
                      {{ isPosOrder ? (detail.isConcession ? 'Bán nhanh F&B' : 'Quầy (POS)') : 'Đặt Online' }}
                    </span>
                  </div>
                  <p class="text-xs text-on-surface-variant mt-0.5">
                    Thời gian tạo: <span class="text-on-surface font-semibold">{{ fmtDateTime(detail.createdAt).date }} {{ fmtDateTime(detail.createdAt).time }}</span>
                  </p>
                </div>
              </div>

              <div class="flex items-center gap-2 shrink-0">
                <span :class="statusBadge(detail.status).cls" class="inline-flex px-3.5 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-widest border shadow-sm">
                  {{ statusBadge(detail.status).label }}
                </span>
              </div>
            </div>

            <!-- BỐ CỤC 2 CỘT CÂN BẰNG (Master-Detail Grid) -->
            <div class="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
              <!-- CỘT TRÁI: SUẤT CHIẾU, VÉ, BẮP NƯỚC & MÃ QR SOÁT VÉ (col-span-7) -->
              <div class="lg:col-span-7 space-y-4">
                <!-- 1. Thẻ Suất chiếu / Loại đơn -->
                <div v-if="detail.isConcession" class="p-4 rounded-2xl bg-surface-container-high border border-outline-variant/10 flex flex-col justify-between shadow-sm">
                  <div>
                    <div class="flex items-center gap-2 mb-1.5">
                      <span class="w-1 h-3 rounded-full bg-primary"></span>
                      <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest">Loại dịch vụ</p>
                    </div>
                    <p class="text-base font-extrabold text-on-surface">Bán nhanh bắp nước (F&amp;B)</p>
                    <p class="text-xs text-on-surface-variant mt-0.5">{{ detail.roomName || 'Quầy Concession' }}</p>
                  </div>
                  <p class="text-xs text-on-surface-variant mt-3 pt-2.5 border-t border-outline-variant/10 font-medium">
                    Rạp: <span class="text-on-surface font-bold">{{ detail.cinemaName || 'DevCine Landmark 81' }}</span>
                  </p>
                </div>
                <div v-else class="p-4 rounded-2xl bg-surface-container-high border border-outline-variant/10 flex flex-col justify-between shadow-sm">
                  <div>
                    <div class="flex items-center justify-between gap-2 mb-1.5">
                      <div class="flex items-center gap-2">
                        <span class="w-1 h-3 rounded-full bg-primary"></span>
                        <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest">Suất chiếu</p>
                      </div>
                      <span v-if="detail.formatName" class="text-[9px] font-black uppercase tracking-widest px-2 py-0.5 rounded bg-primary/10 text-primary border border-primary/20">
                        {{ detail.formatName }}
                      </span>
                    </div>
                    <p class="text-base font-extrabold text-on-surface" :title="detail.movieTitle">{{ detail.movieTitle }}</p>
                    <p class="text-xs text-on-surface-variant mt-1 font-medium">
                      {{ detail.roomName }} &bull; <span class="text-on-surface font-semibold">{{ fmtDateTime(detail.showtimeStart).date }} {{ fmtDateTime(detail.showtimeStart).time }}{{ detail.showtimeEnd ? ` ~ ${fmtDateTime(detail.showtimeEnd).time}` : '' }}</span>
                    </p>
                  </div>
                  <p class="text-xs text-on-surface-variant mt-3 pt-2.5 border-t border-outline-variant/10 font-medium">
                    Rạp: <span class="text-on-surface font-bold">{{ detail.cinemaName || 'DevCine Landmark 81' }}</span>
                  </p>
                </div>

                <!-- 2. Danh sách Vé xem phim -->
                <div v-if="detail.seats && detail.seats.length">
                  <div class="flex items-center gap-2 mb-2">
                    <span class="w-1 h-3 rounded-full bg-primary"></span>
                    <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest">
                      Vé xem phim ({{ detail.seats.length }})
                    </p>
                  </div>
                  <div class="rounded-2xl bg-surface-container-high border border-outline-variant/10 divide-y divide-outline-variant/5 overflow-hidden shadow-sm">
                    <div v-for="(g, i) in detailSeatGroups" :key="i" class="flex items-center justify-between p-3.5 px-4 hover:bg-white/[0.02] transition-colors">
                      <div class="min-w-0">
                        <p class="text-xs font-bold text-on-surface">
                          {{ g.typeLabel }}<span v-if="g.ticketLabel" class="text-on-surface-variant font-medium"> - {{ g.ticketLabel }}</span>
                          <span class="text-primary font-black ml-1.5">x{{ g.count }}</span>
                        </p>
                        <p class="text-[11px] text-on-surface-variant/80 mt-0.5 font-mono">Ghế: {{ g.seats.join(', ') }}</p>
                      </div>
                      <div class="text-right shrink-0">
                        <p class="text-xs font-black text-on-surface tabular-nums font-mono">{{ fmt(g.subtotal) }} đ</p>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 3. Danh sách Bắp nước & Combo -->
                <div v-if="detail.fnbs && detail.fnbs.length">
                  <div class="flex items-center gap-2 mb-2">
                    <span class="w-1 h-3 rounded-full bg-primary"></span>
                    <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest">Bắp nước &amp; Combo</p>
                  </div>
                  <div class="space-y-2">
                    <div
                      v-for="(f, i) in detail.fnbs"
                      :key="i"
                      class="p-3.5 px-4 rounded-2xl bg-surface-container-high border border-outline-variant/10 hover:bg-white/[0.02] transition-colors shadow-sm"
                    >
                      <div class="flex justify-between items-start gap-3">
                        <div class="min-w-0">
                          <div class="flex items-baseline gap-1.5 flex-wrap">
                            <span class="text-xs font-bold text-on-surface">{{ f.name }}</span>
                            <span v-if="fnbTotalSurcharge(f) > 0" class="text-[11px] text-on-surface-variant font-normal">
                              (Gốc: {{ fmt(fnbBasePrice(f)) }}đ)
                            </span>
                            <span class="text-xs font-black text-primary ml-1">x{{ f.quantity }}</span>
                          </div>
                          <!-- Danh sách vị/option kèm phụ thu -->
                          <div v-if="f.options && f.options.length" class="mt-1 space-y-0.5">
                            <p v-for="(o, oi) in f.options" :key="oi" class="text-[11px] text-on-surface-variant flex items-center gap-1.5 pl-1">
                              <span class="text-on-surface-variant/60">&bull;</span>
                              <span>{{ o.optionName }}</span>
                              <span v-if="Number(o.surcharge) > 0" class="text-amber-400 font-semibold">(+{{ fmt(o.surcharge) }}đ)</span>
                            </p>
                          </div>
                        </div>
                        <span class="text-xs font-black text-on-surface tabular-nums font-mono shrink-0">{{ fmt(fnbLineTotal(f)) }} đ</span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 4. Mã QR Check-in (Thanh ngang gọn gàng ở cuối cột trái) -->
                <div v-if="!detail.isConcession">
                  <div class="flex items-center justify-between mb-2">
                    <div class="flex items-center gap-2">
                      <span class="w-1 h-3 rounded-full bg-primary"></span>
                      <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest">Mã QR Check-in</p>
                    </div>
                    <!-- Chỉ hiển thị badge soát vé khi đơn thành công (CONFIRMED / COMPLETED) -->
                    <span
                      v-if="((detail.status || '').toUpperCase() === 'CONFIRMED' || (detail.status || '').toUpperCase() === 'COMPLETED') && isOrderCheckedIn"
                      class="px-2.5 py-0.5 rounded-full text-[9px] font-black uppercase tracking-widest border text-green-400 bg-green-400/10 border-green-400/20"
                    >
                      ĐÃ CHECK-IN
                    </span>
                    <span
                      v-else-if="((detail.status || '').toUpperCase() === 'CONFIRMED' || (detail.status || '').toUpperCase() === 'COMPLETED') && isShowtimePast"
                      class="px-2.5 py-0.5 rounded-full text-[9px] font-black uppercase tracking-widest border text-amber-400 bg-amber-400/10 border-amber-400/30"
                    >
                      QUÁ HẠN SUẤT CHIẾU
                    </span>
                    <span
                      v-else-if="(detail.status || '').toUpperCase() === 'CONFIRMED' || (detail.status || '').toUpperCase() === 'COMPLETED'"
                      class="px-2.5 py-0.5 rounded-full text-[9px] font-black uppercase tracking-widest border text-amber-400 bg-amber-400/10 border-amber-400/20"
                    >
                      CHƯA CHECK-IN
                    </span>
                  </div>

                  <div class="p-4 rounded-2xl bg-surface-container-high border border-outline-variant/10 flex items-center gap-4 shadow-sm relative overflow-hidden">
                    <div
                      class="p-2 bg-white rounded-xl shadow-md border border-black/10 shrink-0 transition-all"
                      :class="{ 'opacity-25 grayscale blur-[0.5px]': isQrDisabled }"
                    >
                      <img
                        :src="`https://api.qrserver.com/v1/create-qr-code/?size=95x95&margin=0&data=${encodeURIComponent(detail.bookingCode)}`"
                        alt="QR"
                        class="w-20 h-20 block"
                      />
                    </div>
                    <div class="min-w-0 space-y-1">
                      <div class="flex items-center gap-2">
                        <span class="text-xs text-on-surface-variant font-mono">Mã soát vé:</span>
                        <span class="text-sm font-black text-on-surface font-mono tracking-wider">{{ detail.bookingCode }}</span>
                      </div>
                      <p v-if="((detail.status || '').toUpperCase() === 'CONFIRMED' || (detail.status || '').toUpperCase() === 'COMPLETED') && isOrderCheckedIn" class="text-xs text-green-400 font-medium leading-relaxed">
                        Mã QR đã check-in vào phòng chiếu thành công.
                      </p>
                      <p v-else-if="((detail.status || '').toUpperCase() === 'CONFIRMED' || (detail.status || '').toUpperCase() === 'COMPLETED') && isShowtimePast" class="text-xs text-amber-400/90 font-semibold leading-relaxed">
                        Suất chiếu đã kết thúc &bull; Vé không còn hiệu lực sử dụng.
                      </p>
                      <p v-else-if="(detail.status || '').toUpperCase() === 'CONFIRMED' || (detail.status || '').toUpperCase() === 'COMPLETED'" class="text-xs text-on-surface-variant leading-relaxed">
                        Quét mã QR tại cổng soát vé để vào phòng chiếu.
                      </p>
                      <p v-else-if="(detail.status || '').toUpperCase() === 'EXPIRED'" class="text-xs text-amber-400/90 font-medium leading-relaxed">
                        Mã QR vô hiệu hoá do đơn hàng đã hết hạn.
                      </p>
                      <p v-else-if="(detail.status || '').toUpperCase() === 'CANCELLED'" class="text-xs text-red-400/90 font-medium leading-relaxed">
                        Mã QR vô hiệu hoá do đơn hàng đã bị huỷ.
                      </p>
                      <p v-else class="text-xs text-yellow-400/90 font-medium leading-relaxed">
                        Đơn chờ thanh toán &bull; Chưa phát hành vé.
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              <!-- CỘT PHẢI: KHÁCH HÀNG & TỔNG KẾT THANH TOÁN (col-span-5) -->
              <div class="lg:col-span-5 space-y-4">
                <!-- 1. Card Khách hàng & Người phục vụ -->
                <div>
                  <div class="flex items-center gap-2 mb-2">
                    <span class="w-1 h-3 rounded-full bg-primary"></span>
                    <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest">
                      Thông tin khách hàng
                    </p>
                  </div>
                  <div class="p-4 rounded-2xl bg-surface-container-high border border-outline-variant/10 flex flex-col justify-between shadow-sm space-y-3">
                    <div>
                      <div class="flex items-center justify-between gap-1 mb-1.5">
                        <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider">Khách hàng</span>
                        <span
                          v-if="detail.membershipTier"
                          :class="tierStyle(detail.membershipTier)"
                          class="text-[9px] font-black uppercase tracking-widest px-2 py-0.5 rounded border inline-block"
                        >
                          Hạng {{ detail.membershipTier }}
                        </span>
                        <span
                          v-else-if="detail.customerName && detail.customerName !== 'Khách tại quầy'"
                          class="text-[9px] font-black uppercase tracking-widest px-2 py-0.5 rounded bg-primary/10 text-primary border border-primary/20 inline-block"
                        >
                          Thành viên
                        </span>
                        <span
                          v-else
                          class="text-[9px] font-bold uppercase tracking-widest px-2 py-0.5 rounded bg-white/5 text-on-surface-variant/80 border border-outline-variant/10 inline-block"
                        >
                          Khách vãng lai
                        </span>
                      </div>

                      <p class="text-base font-extrabold text-on-surface truncate">
                        {{ customerNameDisplay }}
                      </p>

                      <div class="flex items-center justify-between text-xs mt-1 gap-1">
                        <p v-if="detail.customerPhone" class="text-on-surface font-semibold font-mono text-xs truncate">
                          {{ fmtPhone(detail.customerPhone) }}
                        </p>
                        <p v-else class="text-xs text-on-surface-variant/60 italic">
                          Chưa có SĐT
                        </p>
                        <span
                          v-if="detailRewardPoints > 0 && detail.customerName && detail.customerName !== 'Khách tại quầy'"
                          class="text-[10px] font-bold text-primary shrink-0 bg-primary/10 px-1.5 py-0.5 rounded border border-primary/20"
                        >
                          +{{ detailRewardPoints }} điểm
                        </span>
                      </div>
                    </div>

                    <div class="pt-2.5 border-t border-outline-variant/10 text-xs text-on-surface-variant space-y-1">
                      <p v-if="isPosOrder">
                        Thu ngân: <b class="text-on-surface font-semibold">{{ detail.checkedInBy || 'Đỗ Hoàng Minh' }}</b>
                      </p>
                      <p v-else class="truncate">
                        Cổng TT: <b class="text-on-surface font-semibold">{{ paymentLabel(detail.paymentMethod) }}</b>
                        <span v-if="detail.paymentRef" class="font-mono text-[10px] text-on-surface-variant/70 ml-1">(#{{ detail.paymentRef }})</span>
                      </p>
                    </div>
                  </div>
                </div>

                <!-- 2. Khối Tổng kết thanh toán (Biên lai tóm tắt) -->
                <div>
                  <div class="flex items-center gap-2 mb-2">
                    <span class="w-1 h-3 rounded-full bg-primary"></span>
                    <p class="text-[10px] font-headline font-bold text-on-surface-variant uppercase tracking-widest">
                      Tổng kết thanh toán
                    </p>
                  </div>

                  <div class="p-4 sm:p-5 rounded-2xl bg-surface-container-high border border-outline-variant/10 shadow-sm space-y-3.5">
                    <!-- Danh sách các khoản tiền -->
                    <div class="space-y-2.5 text-xs">
                      <div v-if="!detail.isConcession && detailSeatTotal > 0" class="flex justify-between items-center text-on-surface-variant">
                        <span class="font-medium">Tiền vé</span>
                        <span class="tabular-nums font-semibold font-mono text-on-surface">{{ fmt(detailSeatTotal) }} đ</span>
                      </div>

                      <div v-if="detailComboTotal > 0" class="flex justify-between items-center text-on-surface-variant">
                        <span class="font-medium">Bắp nước &amp; Combo</span>
                        <span class="tabular-nums font-semibold font-mono text-on-surface">{{ fmt(detailComboTotal) }} đ</span>
                      </div>

                      <div class="flex justify-between items-center text-on-surface-variant">
                        <span class="font-medium">Khuyến mãi / Giảm giá</span>
                        <span
                          class="tabular-nums font-semibold font-mono"
                          :class="detailDiscount > 0 ? 'text-green-400 font-bold' : 'text-on-surface-variant'"
                        >
                          {{ detailDiscount > 0 ? `−${fmt(detailDiscount)} đ` : '0 đ' }}
                        </span>
                      </div>
                    </div>

                    <!-- Divider nét đứt phân tách -->
                    <div class="border-t border-dashed border-outline-variant/20 pt-3">
                      <div class="flex justify-between items-baseline">
                        <span class="text-xs font-headline font-extrabold text-on-surface uppercase tracking-widest">
                          TỔNG TIỀN:
                        </span>
                        <span class="text-2xl font-headline font-black text-primary tabular-nums tracking-tight">
                          {{ fmt(detailFinalPrice) }} đ
                        </span>
                      </div>
                    </div>

                    <!-- Phương thức thanh toán & Trạng thái -->
                    <div class="pt-3 border-t border-outline-variant/10 flex flex-wrap items-center justify-between gap-2 text-xs">
                      <div class="flex items-center gap-1.5 text-on-surface-variant min-w-0">
                        <span>Phương thức:</span>
                        <b class="text-on-surface font-bold truncate text-xs">{{ paymentLabelFull(detail.paymentMethod) }}</b>
                      </div>
                      <span
                        :class="paymentStatusBadge(detail.status).cls"
                        class="inline-flex px-2.5 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest border shadow-sm shrink-0"
                      >
                        {{ paymentStatusBadge(detail.status).label }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer Buttons -->
          <div class="px-6 py-4 border-t border-outline-variant/10 flex justify-end gap-3 flex-shrink-0 bg-surface-container-high/30">
            <button @click="showDetail = false" class="px-6 py-3 rounded-xl text-on-surface-variant hover:text-on-surface hover:bg-white/5 font-bold text-xs uppercase tracking-widest transition-colors">
              Đóng
            </button>
            <button
              v-if="detail"
              :disabled="(detail.status || '').toUpperCase() !== 'CONFIRMED' && (detail.status || '').toUpperCase() !== 'COMPLETED'"
              @click="reprint(detail.bookingId, detail.isConcession)"
              class="px-6 py-3 font-bold text-xs uppercase tracking-widest rounded-xl transition-all flex items-center gap-2"
              :class="(detail.status || '').toUpperCase() === 'CONFIRMED' || (detail.status || '').toUpperCase() === 'COMPLETED'
                ? 'bg-primary text-on-primary hover:brightness-110 shadow-lg shadow-primary/20 cursor-pointer'
                : 'bg-white/5 text-on-surface-variant/40 border border-white/5 cursor-not-allowed'"
            >
              <span class="material-symbols-outlined text-base">print</span>
              {{ ((detail.status || '').toUpperCase() === 'CONFIRMED' || (detail.status || '').toUpperCase() === 'COMPLETED') ? (isPosOrder ? 'IN LẠI VÉ / HOÁ ĐƠN' : 'IN VÉ / HOÁ ĐƠN') : 'KHÔNG THỂ IN (CHƯA HOÀN TẤT)' }}
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
