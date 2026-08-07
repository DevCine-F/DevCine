<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { bookingApi } from '@/api/customer/index'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()

const props = defineProps({
  preview: { type: Boolean, default: false }
})

const PREVIEW_LIMIT = 3
const PAGE_SIZE = 5

const authStore = useAuthStore()
const bookings = ref([])
const isLoading = ref(false)
const error = ref('')
const activeFilter = ref('all')

const filteredBookings = computed(() => {
  if (activeFilter.value === 'upcoming') {
    return bookings.value.filter(b => new Date(b.showtime?.startTime) > new Date())
  }
  if (activeFilter.value === 'past') {
    return bookings.value.filter(b => new Date(b.showtime?.startTime) <= new Date())
  }
  return bookings.value
})

const currentPage = ref(1)
const totalPages = computed(() => Math.max(1, Math.ceil(filteredBookings.value.length / PAGE_SIZE)))

const displayBookings = computed(() => {
  if (props.preview) {
    return [...bookings.value]
      .sort((a, b) => new Date(b.showtime?.startTime || 0) - new Date(a.showtime?.startTime || 0))
      .slice(0, PREVIEW_LIMIT)
  }
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredBookings.value.slice(start, start + PAGE_SIZE)
})

const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = [1]
  if (cur > 3) pages.push('...')
  for (let i = Math.max(2, cur - 1); i <= Math.min(total - 1, cur + 1); i++) pages.push(i)
  if (cur < total - 2) pages.push('...')
  pages.push(total)
  return pages
})

const goToPage = (p) => {
  if (typeof p !== 'number' || p < 1 || p > totalPages.value) return
  currentPage.value = p
}

watch(activeFilter, () => { currentPage.value = 1 })
watch(totalPages, (n) => { if (currentPage.value > n) currentPage.value = n })

const fetchHistory = async () => {
  if (!authStore.isAuthenticated || !authStore.user?.id) return
  isLoading.value = true
  error.value = ''
  try {
    const { data } = await bookingApi.getHistory(authStore.user.id)
    bookings.value = data
  } catch (err) {
    error.value = friendlyError(err, 'Không thể tải lịch sử đặt vé. Vui lòng thử lại.')
    toast.error(error.value)
  } finally {
    isLoading.value = false
  }
}

const extractTime = (iso) => {
  if (!iso) return ''
  return new Date(iso).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

const extractDate = (iso) => {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

const formatDateTimeFormat = (iso, format) => {
  if (!iso) return ''
  const time = extractTime(iso)
  const date = extractDate(iso)
  return `${time} - ${date}${format ? ` - ${format}` : ''}`
}

const formatPrice = (n) => n != null ? Number(n).toLocaleString('vi-VN') + 'đ' : ''

const statusLabel = (s) => ({ 
  CONFIRMED: 'Đã xác nhận', 
  HOLD: 'Chờ thanh toán', 
  CANCELLED: 'Đã huỷ',
  EXPIRED: 'Đã huỷ',
  COMPLETED: 'Đã sử dụng'
}[s] || s)

const statusClass = (s) => ({
  CONFIRMED: 'bg-green-500/10 text-green-400 border border-green-500/20',
  HOLD: 'bg-yellow-500/10 text-yellow-400 border border-yellow-500/20',
  CANCELLED: 'bg-red-500/10 text-red-400 border border-red-500/20',
  EXPIRED: 'bg-red-500/10 text-red-400 border border-red-500/20',
  COMPLETED: 'bg-neutral-500/10 text-neutral-400 border border-neutral-500/20'
}[s] || 'bg-white/10 text-on-surface-variant border border-white/20')

const ageRatingColor = (rating) => {
  if (!rating) return 'bg-white/10 text-white'
  if (rating.includes('18')) return 'bg-red-600 text-white'
  if (rating.includes('16')) return 'bg-orange-500 text-white'
  if (rating.includes('13')) return 'bg-yellow-500 text-black'
  if (rating.includes('K')) return 'bg-blue-500 text-white'
  if (rating.includes('P')) return 'bg-green-500 text-white'
  return 'bg-white/20 text-white'
}

// Ticket Detail Modal
const selectedBooking = ref(null)
const showTicketModal = ref(false)
const showPriceDetails = ref(false)

const groupedSeats = computed(() => {
  if (!selectedBooking.value?.seatsDetail) return []
  const counts = {}
  selectedBooking.value.seatsDetail.forEach(s => {
    const type = s.seatType || 'Thường'
    const target = s.targetType ? ` (${s.targetType})` : ''
    const key = `${type}${target}`
    counts[key] = (counts[key] || 0) + 1
  })
  return Object.entries(counts).map(([k, v]) => `${v}× ${k}`)
})

const openTicketDetail = (booking) => {
  selectedBooking.value = booking
  showTicketModal.value = true
  showPriceDetails.value = false
}

const closeTicketDetail = () => {
  showTicketModal.value = false
  selectedBooking.value = null
  showPriceDetails.value = false
}

const printTicket = () => {
  window.print()
}

onMounted(fetchHistory)
</script>

<template>
  <section class="max-w-6xl mx-auto">
    <div class="flex flex-col md:flex-row justify-between items-baseline mb-8 gap-4 print:hidden">
      <h2 class="text-2xl font-bold tracking-tight font-headline">Lịch sử đặt vé</h2>
      <div v-if="!preview" class="flex gap-4">
        <button @click="activeFilter = 'all'" :class="['text-xs font-bold uppercase tracking-widest pb-1 border-b-2 transition-colors', activeFilter === 'all' ? 'border-[#EAB308] text-[#EAB308]' : 'text-neutral-500 hover:text-white border-transparent']">Tất cả</button>
        <button @click="activeFilter = 'upcoming'" :class="['text-xs font-bold uppercase tracking-widest pb-1 border-b-2 transition-colors', activeFilter === 'upcoming' ? 'border-[#EAB308] text-[#EAB308]' : 'text-neutral-500 hover:text-white border-transparent']">Sắp diễn ra</button>
        <button @click="activeFilter = 'past'" :class="['text-xs font-bold uppercase tracking-widest pb-1 border-b-2 transition-colors', activeFilter === 'past' ? 'border-[#EAB308] text-[#EAB308]' : 'text-neutral-500 hover:text-white border-transparent']">Đã xem</button>
      </div>
      <router-link v-else to="/profile/history" class="text-xs font-bold uppercase tracking-widest pb-1 text-[#EAB308] hover:text-[#FACC15] transition-colors flex items-center gap-1">
        Xem tất cả
        <span class="material-symbols-outlined text-base">arrow_forward</span>
      </router-link>
    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="flex flex-col gap-6 print:hidden">
      <div v-for="i in 3" :key="i" class="bg-[#18181B] border border-white/5 animate-pulse h-40 rounded-xl"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="p-6 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm print:hidden">{{ error }}</div>

    <!-- Empty -->
    <div v-else-if="displayBookings.length === 0" class="flex flex-col items-center justify-center py-24 text-center print:hidden">
      <span class="material-symbols-outlined text-5xl text-outline-variant mb-4">confirmation_number</span>
      <p class="text-on-surface-variant font-semibold">Chưa có lịch sử đặt vé</p>
      <p class="text-sm text-outline-variant mt-1">Các vé bạn đã đặt sẽ xuất hiện ở đây</p>
    </div>

    <!-- List -->
    <div v-else class="grid grid-cols-1 gap-6 print:hidden">
      <div v-for="b in displayBookings" :key="b.bookingId" @click="openTicketDetail(b)"
           class="group relative bg-[#18181B] hover:bg-[#27272A] border border-white/5 cursor-pointer transition-all duration-300 rounded-xl p-3 flex flex-col md:flex-row gap-6 items-stretch shadow-lg">
        
        <!-- Khối 1: Poster -->
        <div class="w-full md:w-32 aspect-[2/3] md:h-auto overflow-hidden shrink-0 rounded-lg shadow-md bg-[#27272A]">
          <img v-if="b.showtime?.moviePosterUrl"
               :src="b.showtime.moviePosterUrl"
               class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"/>
          <div v-else class="w-full h-full flex items-center justify-center">
            <span class="material-symbols-outlined text-3xl text-white/20">movie</span>
          </div>
        </div>
        
        <div class="flex-grow flex flex-col justify-center py-2">
          <div class="flex flex-col md:flex-row gap-6 items-stretch h-full">
            
            <!-- Khối 2: Thông tin chính -->
            <div class="flex-[1.5] flex flex-col justify-center py-2">
              <div class="flex items-center gap-3 mb-2">
                <span class="text-sm font-bold font-mono text-[#EAB308]">#{{ b.bookingCode }}</span>
                <span :class="['text-[10px] font-bold px-2 py-0.5 rounded-sm uppercase tracking-wider flex items-center gap-1', statusClass(b.status)]">
                  <span v-if="b.status === 'CONFIRMED'" class="w-1.5 h-1.5 rounded-full bg-green-400"></span>
                  {{ statusLabel(b.status) }}
                </span>
              </div>
              <div class="flex items-center gap-2 mb-3">
                 <h3 class="text-xl md:text-2xl font-bold group-hover:text-[#EAB308] transition-colors uppercase font-headline">
                   {{ b.showtime?.movieTitle }}
                 </h3>
                 <span v-if="b.showtime?.ageRating" :class="['px-1.5 py-0.5 text-[10px] rounded font-bold uppercase shrink-0', ageRatingColor(b.showtime.ageRating)]">
                   {{ b.showtime.ageRating }}
                 </span>
              </div>
              <div class="flex flex-col gap-1 text-sm text-on-surface-variant">
                <p class="flex items-center gap-2"><span class="material-symbols-outlined text-base opacity-70">schedule</span> {{ formatDateTimeFormat(b.showtime?.startTime, '') }}</p>
                <p class="flex items-center gap-2"><span class="material-symbols-outlined text-base opacity-70">movie</span> {{ b.showtime?.format || '2D' }}</p>
              </div>
            </div>

            <!-- Khối 3: Địa điểm & Ghế -->
            <div class="flex-1 flex flex-col justify-center md:border-l border-white/10 md:pl-6 py-2">
               <div class="mb-3">
                 <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Rạp chiếu</p>
                 <p class="text-sm font-semibold truncate">{{ b.showtime?.cinemaName }}</p>
                 <p class="text-sm opacity-80">{{ b.showtime?.roomName }}</p>
               </div>
               <div>
                 <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Ghế ngồi</p>
                 <p class="text-sm font-bold text-[#EAB308] truncate">{{ b.seats || '—' }}</p>
               </div>
            </div>

            <!-- Khối 4: Giá tiền -->
            <div class="flex-1 flex flex-row items-center justify-between md:border-l border-white/10 md:pl-6 pr-4 py-2 w-full h-full mt-3 md:mt-0 pt-3 md:pt-0 border-t md:border-t-0">
              <div class="flex flex-col items-start gap-1.5">
                <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant m-0 leading-none">Tổng tiền</p>
                <p class="text-2xl font-bold text-white leading-none">{{ formatPrice(b.finalPrice) }}</p>
              </div>
              <div class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center group-hover:bg-[#EAB308] group-hover:text-black transition-colors hidden md:flex shrink-0">
                <span class="material-symbols-outlined text-sm">arrow_forward</span>
              </div>
            </div>

          </div>
        </div>
      </div>
      
      <router-link v-if="preview && bookings.length > PREVIEW_LIMIT" to="/profile/history"
                   class="mt-2 flex items-center justify-center gap-2 py-4 bg-[#18181B] hover:bg-[#27272A] border border-white/5 rounded-xl text-sm font-bold uppercase tracking-widest text-[#EAB308] transition-colors">
        Xem tất cả lịch sử đặt vé
        <span class="material-symbols-outlined text-lg">arrow_forward</span>
      </router-link>
    </div>

    <!-- Pagination -->
    <div v-if="!preview && !isLoading && !error && totalPages > 1" class="mt-10 flex items-center justify-center gap-2 print:hidden">
      <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 1"
              class="w-10 h-10 flex items-center justify-center rounded-lg bg-[#18181B] border border-white/5 text-on-surface-variant hover:bg-[#27272A] hover:text-white transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
        <span class="material-symbols-outlined text-lg">chevron_left</span>
      </button>
      <template v-for="(p, i) in pageNumbers" :key="i">
        <span v-if="p === '...'" class="w-10 h-10 flex items-center justify-center text-on-surface-variant">…</span>
        <button v-else @click="goToPage(p)"
                :class="['w-10 h-10 flex items-center justify-center rounded-lg text-sm font-bold transition-colors border', p === currentPage ? 'bg-[#EAB308] text-black border-[#EAB308]' : 'bg-[#18181B] border-white/5 text-on-surface-variant hover:bg-[#27272A] hover:text-white']">
          {{ p }}
        </button>
      </template>
      <button @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages"
              class="w-10 h-10 flex items-center justify-center rounded-lg bg-[#18181B] border border-white/5 text-on-surface-variant hover:bg-[#27272A] hover:text-white transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
        <span class="material-symbols-outlined text-lg">chevron_right</span>
      </button>
    </div>

    <!-- Modal Chi Tiết Vé (Ticket Stub Design) -->
    <div v-if="showTicketModal && selectedBooking" class="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-lg p-4 print:p-0 print:bg-white print:static print:h-auto print:w-full print:block overflow-y-auto" @click.self="closeTicketDetail">

      <!-- Ticket Container -->
      <div class="ticket-stub relative w-full max-w-md mx-auto my-auto flex flex-col rounded-[28px] shadow-2xl print:shadow-none print:w-full print:max-w-none print:h-auto bg-[#0e0e10] ring-1 ring-white/10 print:bg-white print:rounded-none print:ring-0">

        <!-- Top Section: Details -->
        <div class="relative overflow-hidden rounded-t-[28px] print:rounded-none">

          <!-- Poster backdrop wash -->
          <div v-if="selectedBooking.showtime?.moviePosterUrl" class="absolute inset-0 print:hidden pointer-events-none">
            <img :src="selectedBooking.showtime.moviePosterUrl" class="w-full h-full object-cover opacity-25 blur-2xl scale-125" />
            <div class="absolute inset-0 bg-gradient-to-b from-[#0e0e10]/50 via-[#0e0e10]/85 to-[#0e0e10]"></div>
          </div>

          <div class="relative z-10 p-6 pb-5 print:p-4 flex flex-col gap-5">

            <!-- Header Badges & Close -->
            <div class="flex justify-between items-start print:hidden">
              <div class="flex items-center gap-2">
                <span v-if="selectedBooking.showtime?.format" class="px-2.5 py-1 rounded-md bg-white/10 backdrop-blur-sm border border-white/15 text-[10px] font-bold text-white">{{ selectedBooking.showtime.format }}</span>
                <span v-if="selectedBooking.showtime?.ageRating" :class="['px-2 py-1 rounded-md text-[10px] font-bold uppercase', ageRatingColor(selectedBooking.showtime.ageRating)]">{{ selectedBooking.showtime.ageRating }}</span>
              </div>
              <div class="flex items-center gap-3">
                <span :class="['text-[9px] font-bold px-2 py-1 rounded-md uppercase tracking-wider flex items-center gap-1.5', statusClass(selectedBooking.status)]">
                  <span v-if="selectedBooking.status === 'CONFIRMED'" class="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse"></span>
                  {{ statusLabel(selectedBooking.status) }}
                </span>
                <button @click="closeTicketDetail" aria-label="Đóng" class="group/close relative w-8 h-8 flex items-center justify-center rounded-full text-zinc-500 hover:text-white transition-colors shrink-0">
                  <span class="absolute inset-0 rounded-full bg-white/0 group-hover/close:bg-white/[0.08] transition-colors"></span>
                  <span class="material-symbols-outlined text-[20px] leading-none relative transition-transform duration-300 group-hover/close:rotate-90" style="font-variation-settings: 'wght' 300;">close</span>
                </button>
              </div>
            </div>

            <!-- Movie Title -->
            <div>
              <p class="text-[10px] font-bold uppercase tracking-[0.25em] text-[#EAB308]/80 mb-1.5 print:hidden">Vé xem phim</p>
              <h3 class="text-[28px] font-bold uppercase font-headline text-white print:text-black tracking-tight leading-[1.2]">
                {{ selectedBooking.showtime?.movieTitle }}
              </h3>
            </div>

            <!-- Rạp & Phòng chiếu -->
            <div class="flex items-start gap-2.5">
              <span class="material-symbols-outlined text-[18px] text-[#EAB308]/70 mt-0.5 print:hidden">location_on</span>
              <div>
                <p class="text-[10px] font-medium uppercase tracking-wider text-zinc-400 mb-0.5">Rạp chiếu</p>
                <p class="font-semibold text-sm text-white print:text-black leading-snug">{{ selectedBooking.showtime?.cinemaName }} <span class="opacity-55 font-normal">— {{ selectedBooking.showtime?.roomName }}</span></p>
              </div>
            </div>

            <!-- Info grid: giờ / ngày / loại ghế / số ghế -->
            <div class="grid grid-cols-2 gap-3">
              <div class="rounded-xl bg-white/[0.04] border border-white/10 px-3.5 py-2.5 print:border-black/20">
                <p class="text-[10px] font-medium uppercase tracking-wider text-zinc-400 mb-0.5">Suất chiếu</p>
                <p class="font-bold text-base text-white print:text-black">{{ extractTime(selectedBooking.showtime?.startTime) }}</p>
              </div>
              <div class="rounded-xl bg-white/[0.04] border border-white/10 px-3.5 py-2.5 print:border-black/20">
                <p class="text-[10px] font-medium uppercase tracking-wider text-zinc-400 mb-0.5">Ngày chiếu</p>
                <p class="font-bold text-base text-white print:text-black">{{ extractDate(selectedBooking.showtime?.startTime) }}</p>
              </div>
              <div class="rounded-xl bg-white/[0.04] border border-white/10 px-3.5 py-2.5 print:border-black/20">
                <p class="text-[10px] font-medium uppercase tracking-wider text-zinc-400 mb-0.5">Loại ghế</p>
                <div v-if="groupedSeats.length > 0" class="flex flex-wrap gap-1 mt-1">
                  <span v-for="(group, idx) in groupedSeats" :key="idx" class="font-semibold text-[11px] text-white print:text-black bg-white/10 print:bg-black/5 px-2 py-0.5 rounded leading-tight">
                    {{ group }}
                  </span>
                </div>
                <p v-else class="font-semibold text-[13px] text-white print:text-black">—</p>
              </div>
              <div class="rounded-xl bg-[#EAB308]/10 border border-[#EAB308]/25 px-3.5 py-2.5 print:border-black/20 print:bg-transparent">
                <p class="text-[10px] font-medium uppercase tracking-wider text-[#EAB308]/80 mb-0.5 print:text-black">Số ghế</p>
                <p class="font-bold text-lg text-[#EAB308] print:text-black leading-tight break-words max-h-20 overflow-y-auto">
                  {{ selectedBooking.seatsDetail?.map(s => s.seatNumber).join(', ') || selectedBooking.seats || '—' }}
                </p>
              </div>
            </div>

            <!-- Combo F&B -->
            <div v-if="selectedBooking.fnbs && selectedBooking.fnbs.length > 0">
              <p class="text-[10px] font-medium uppercase tracking-wider text-zinc-400 mb-2">Combo / Bắp nước</p>
              <div class="flex flex-wrap gap-2 max-h-24 overflow-y-auto custom-scrollbar pr-1">
                <span v-for="(fnb, idx) in selectedBooking.fnbs" :key="idx" class="text-xs text-zinc-100 bg-white/[0.06] border border-white/10 px-2.5 py-1.5 rounded-lg shrink-0">
                  <span class="font-bold text-[#EAB308] mr-1">{{ fnb.quantity }}×</span>{{ fnb.itemName }}
                </span>
              </div>
            </div>

          </div>
        </div>

        <!-- Divider -->
        <div class="mx-6 h-px bg-gradient-to-r from-transparent via-white/15 to-transparent print:hidden"></div>

        <!-- Bottom Section: QR & Price -->
        <div class="px-6 pb-6 pt-5 print:px-4 print:pt-4 flex flex-col relative">

          <div class="flex justify-between items-center mb-5">
            <span class="text-[10px] font-medium uppercase tracking-wider text-zinc-400">Mã đặt vé / Bắp nước</span>
            <span class="text-lg font-mono font-bold text-[#EAB308] print:text-black tracking-[0.2em]">#{{ selectedBooking.bookingCode }}</span>
          </div>

          <!-- QR with scanner corner brackets -->
          <div class="flex items-center justify-center mb-5">
            <div class="relative">
              <div class="absolute -inset-2.5 print:hidden">
                <span class="absolute top-0 left-0 w-5 h-5 border-t-2 border-l-2 border-[#EAB308]/70"></span>
                <span class="absolute top-0 right-0 w-5 h-5 border-t-2 border-r-2 border-[#EAB308]/70"></span>
                <span class="absolute bottom-0 left-0 w-5 h-5 border-b-2 border-l-2 border-[#EAB308]/70"></span>
                <span class="absolute bottom-0 right-0 w-5 h-5 border-b-2 border-r-2 border-[#EAB308]/70"></span>
              </div>
              <div v-if="selectedBooking.status === 'CONFIRMED' || selectedBooking.status === 'COMPLETED'" class="bg-white p-3 shadow-[0_0_30px_rgba(234,179,8,0.12)] print:shadow-none print:p-0">
                <img :src="`https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${selectedBooking.bookingCode}`" alt="QR Code" class="w-32 h-32 object-contain print:w-32 print:h-32" />
              </div>
              <div v-else class="bg-[#18181B] p-3 border border-red-500/30 shadow-[0_0_30px_rgba(239,68,68,0.12)] w-32 h-32 flex items-center justify-center relative overflow-hidden">
                <div class="absolute inset-0 opacity-30 bg-repeat bg-center" style="background-image: url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI4IiBoZWlnaHQ9IjgiPgo8cmVjdCB3aWR0aD0iOCIgaGVpZ2h0PSI4IiBmaWxsPSIjZmZmIiBmaWxsLW9wYWNpdHk9IjAuMSI+PC9yZWN0Pgo8cGF0aCBkPSJNMCAwTDggOFpNOCAwTDAgOFoiIHN0cm9rZT0iI2VmNDQ0NCIgc3Ryb2tlLW9wYWNpdHk9IjAuMyIgc3Ryb2tlLXdpZHRoPSIxIj48L3BhdGg+Cjwvc3ZnPg==')"></div>
                <span class="text-red-500 font-bold uppercase tracking-widest text-xs text-center z-10 rotate-[-15deg] px-2 py-1 bg-red-500/10 border border-red-500/30 rounded backdrop-blur-sm shadow-xl leading-snug">Vé đã hủy<br/>VOID</span>
              </div>
            </div>
          </div>

          <div class="pt-5 border-t border-white/10 print:border-black/20">

            <!-- Accordion Toggle & Total -->
            <div class="flex justify-between items-center print:hidden">
              <button @click="showPriceDetails = !showPriceDetails" class="bg-white/[0.06] hover:bg-white/10 text-xs text-zinc-300 px-3 py-1.5 rounded-lg border border-white/10 transition font-bold uppercase tracking-widest flex items-center gap-1.5">
                Chi tiết giá
                <span class="material-symbols-outlined text-sm transition-transform duration-300" :class="{ 'rotate-180': showPriceDetails }">expand_more</span>
              </button>
              <div class="text-right">
                <p class="text-[9px] font-bold uppercase tracking-widest text-zinc-400 mb-0.5">Tổng thanh toán</p>
                <p class="text-2xl font-bold text-[#EAB308] font-mono leading-none">{{ formatPrice(selectedBooking.finalPrice) }}</p>
              </div>
            </div>

            <!-- In vé tĩnh -->
            <div class="hidden print:flex justify-between items-center">
              <p class="text-sm font-bold uppercase tracking-widest">Tổng tiền</p>
              <p class="text-xl font-bold text-black">{{ formatPrice(selectedBooking.finalPrice) }}</p>
            </div>

            <!-- Slide Down Price Details -->
            <div v-show="showPriceDetails" class="bg-white/[0.03] rounded-xl p-4 mt-4 text-sm space-y-3 print:hidden border border-white/10">
              <div class="flex justify-between text-zinc-300">
                <span>Tiền vé &amp; Ghế:</span>
                <span class="font-mono">{{ formatPrice(selectedBooking.originalPrice) }}</span>
              </div>
              <div v-if="selectedBooking.fnbTotal > 0" class="flex justify-between text-zinc-300">
                <span>Tiền Bắp / Nước:</span>
                <span class="font-mono">{{ formatPrice(selectedBooking.fnbTotal) }}</span>
              </div>
              <div v-if="selectedBooking.discountAmount > 0" class="flex justify-between text-green-400">
                <span>Khuyến mãi / Giảm giá:</span>
                <span class="font-mono">-{{ formatPrice(selectedBooking.discountAmount) }}</span>
              </div>
              <div class="pt-3 mt-1 border-t border-white/10 flex justify-between font-bold text-white text-base">
                <span>Thành tiền:</span>
                <span class="font-mono text-[#EAB308]">{{ formatPrice(selectedBooking.finalPrice) }}</span>
              </div>
            </div>

          </div>
        </div>

      </div>
    </div>

  </section>
</template>

<style scoped>
.ticket-stub {
  animation: ticketIn 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}
@keyframes ticketIn {
  from { opacity: 0; transform: translateY(14px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}
@media print {
  .ticket-stub { animation: none; }
}
</style>
