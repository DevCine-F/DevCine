<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { bookingApi } from '@/api/customer/index'

// preview: chế độ nhúng (vd. trang Thông tin cá nhân) — chỉ hiện 3 lượt gần nhất,
// ẩn tab lọc và hiển thị CTA chuyển sang trang Lịch sử đặt vé đầy đủ.
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

// Phân trang (chỉ áp dụng ở trang đầy đủ, áp cho cả 3 tab).
const currentPage = ref(1)
const totalPages = computed(() => Math.max(1, Math.ceil(filteredBookings.value.length / PAGE_SIZE)))

// Danh sách thực sự hiển thị:
//  - preview: 3 suất gần nhất theo thời gian chiếu.
//  - đầy đủ: lát cắt theo trang hiện tại của tab đang chọn.
const displayBookings = computed(() => {
  if (props.preview) {
    return [...bookings.value]
      .sort((a, b) => new Date(b.showtime?.startTime || 0) - new Date(a.showtime?.startTime || 0))
      .slice(0, PREVIEW_LIMIT)
  }
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredBookings.value.slice(start, start + PAGE_SIZE)
})

// Dãy số trang hiển thị, rút gọn bằng "…" khi quá nhiều trang.
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

// Đổi tab -> quay về trang 1; dữ liệu thay đổi làm giảm số trang -> kẹp lại.
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
    error.value = 'Không thể tải lịch sử đặt vé. Vui lòng thử lại.'
  } finally {
    isLoading.value = false
  }
}

const formatDate = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })
}

const formatPrice = (n) => n != null ? Number(n).toLocaleString('vi-VN') + 'đ' : ''

const statusLabel = (s) => ({ CONFIRMED: 'Đã xác nhận', HOLD: 'Chờ thanh toán', CANCELLED: 'Đã huỷ' }[s] || s)
const statusClass = (s) => ({
  CONFIRMED: 'bg-green-500/10 text-green-400',
  HOLD: 'bg-yellow-500/10 text-yellow-400',
  CANCELLED: 'bg-red-500/10 text-red-400'
}[s] || 'bg-white/10 text-on-surface-variant')

onMounted(fetchHistory)
</script>

<template>
  <section>
    <div class="flex flex-col md:flex-row justify-between items-baseline mb-8 gap-4">
      <h2 class="text-2xl font-bold tracking-tight font-headline">Lịch sử đặt vé</h2>
      <!-- Tab lọc: chỉ ở trang đầy đủ -->
      <div v-if="!preview" class="flex gap-4">
        <button @click="activeFilter = 'all'" :class="['text-xs font-bold uppercase tracking-widest pb-1 border-b-2 transition-colors', activeFilter === 'all' ? 'border-primary-container text-primary-container' : 'text-neutral-500 hover:text-on-surface border-transparent']">Tất cả</button>
        <button @click="activeFilter = 'upcoming'" :class="['text-xs font-bold uppercase tracking-widest pb-1 border-b-2 transition-colors', activeFilter === 'upcoming' ? 'border-primary-container text-primary-container' : 'text-neutral-500 hover:text-on-surface border-transparent']">Sắp diễn ra</button>
        <button @click="activeFilter = 'past'" :class="['text-xs font-bold uppercase tracking-widest pb-1 border-b-2 transition-colors', activeFilter === 'past' ? 'border-primary-container text-primary-container' : 'text-neutral-500 hover:text-on-surface border-transparent']">Đã xem</button>
      </div>
      <!-- Preview: gợi ý xem tất cả -->
      <router-link v-else to="/profile/history" class="text-xs font-bold uppercase tracking-widest pb-1 text-primary-container hover:brightness-110 transition-colors flex items-center gap-1">
        Xem tất cả
        <span class="material-symbols-outlined text-base">arrow_forward</span>
      </router-link>
    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="flex flex-col gap-4">
      <div v-for="i in 3" :key="i" class="bg-surface-container-high animate-pulse h-32 rounded"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="p-6 bg-red-500/10 border border-red-500/20 rounded text-red-400 text-sm">{{ error }}</div>

    <!-- Empty -->
    <div v-else-if="displayBookings.length === 0" class="flex flex-col items-center justify-center py-24 text-center">
      <span class="material-symbols-outlined text-5xl text-outline-variant mb-4">confirmation_number</span>
      <p class="text-on-surface-variant font-semibold">Chưa có lịch sử đặt vé</p>
      <p class="text-sm text-outline-variant mt-1">Các vé bạn đã đặt sẽ xuất hiện ở đây</p>
    </div>

    <!-- List -->
    <div v-else class="grid grid-cols-1 gap-4">
      <div v-for="b in displayBookings" :key="b.bookingId"
           class="group relative bg-surface-container-high hover:bg-surface-bright transition-all duration-300 p-1 flex flex-col md:flex-row gap-6 items-stretch">
        <div class="w-full md:w-24 h-36 md:h-auto overflow-hidden shrink-0">
          <img v-if="b.showtime?.moviePosterUrl"
               :src="b.showtime.moviePosterUrl"
               class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"/>
          <div v-else class="w-full h-full bg-surface-container-highest flex items-center justify-center">
            <span class="material-symbols-outlined text-3xl text-outline-variant">movie</span>
          </div>
        </div>
        <div class="flex-grow flex flex-col justify-center py-4">
          <div class="flex items-center gap-2 mb-1">
            <span :class="['text-[9px] font-bold px-1.5 py-0.5 rounded-sm', statusClass(b.status)]">{{ statusLabel(b.status) }}</span>
          </div>
          <h3 class="text-xl font-bold mb-3 group-hover:text-primary-container transition-colors uppercase font-headline">
            {{ b.showtime?.movieTitle }}
          </h3>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <p class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant mb-0.5">Mã đặt vé</p>
              <p class="text-xs font-semibold font-mono">{{ b.bookingCode }}</p>
            </div>
            <div>
              <p class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant mb-0.5">Suất chiếu</p>
              <p class="text-xs font-semibold">{{ formatDate(b.showtime?.startTime) }}</p>
            </div>
            <div>
              <p class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant mb-0.5">Ghế</p>
              <p class="text-xs font-semibold text-primary-container">{{ b.seats || '—' }}</p>
            </div>
            <div>
              <p class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant mb-0.5">Tổng tiền</p>
              <p class="text-xs font-semibold">{{ formatPrice(b.finalPrice) }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Preview: gợi ý chuyển sang trang Lịch sử đặt vé đầy đủ -->
      <router-link v-if="preview && bookings.length > PREVIEW_LIMIT" to="/profile/history"
                   class="mt-2 flex items-center justify-center gap-2 py-4 bg-surface-container-high/60 hover:bg-surface-container-highest border border-white/5 rounded text-sm font-bold uppercase tracking-widest text-primary-container hover:brightness-110 transition-colors">
        Xem tất cả lịch sử đặt vé
        <span class="material-symbols-outlined text-lg">arrow_forward</span>
      </router-link>
    </div>

    <!-- Pagination (trang đầy đủ, áp cho cả 3 tab) -->
    <div v-if="!preview && !isLoading && !error && totalPages > 1" class="mt-8 flex items-center justify-center gap-2">
      <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 1"
              class="w-9 h-9 flex items-center justify-center rounded border border-white/10 text-on-surface-variant hover:bg-surface-container-highest hover:text-white transition-colors disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent">
        <span class="material-symbols-outlined text-lg">chevron_left</span>
      </button>
      <template v-for="(p, i) in pageNumbers" :key="i">
        <span v-if="p === '...'" class="w-9 h-9 flex items-center justify-center text-on-surface-variant">…</span>
        <button v-else @click="goToPage(p)"
                :class="['w-9 h-9 flex items-center justify-center rounded text-sm font-bold transition-colors border', p === currentPage ? 'bg-primary-container text-on-primary border-primary-container' : 'border-white/10 text-on-surface-variant hover:bg-surface-container-highest hover:text-white']">
          {{ p }}
        </button>
      </template>
      <button @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages"
              class="w-9 h-9 flex items-center justify-center rounded border border-white/10 text-on-surface-variant hover:bg-surface-container-highest hover:text-white transition-colors disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent">
        <span class="material-symbols-outlined text-lg">chevron_right</span>
      </button>
    </div>

  </section>
</template>
