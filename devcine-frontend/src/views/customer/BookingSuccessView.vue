<script setup>
import { RouterLink, useRoute } from 'vue-router'
import { computed, onMounted, ref } from 'vue'
import { useBookingStore } from '@/stores/booking'
import { paymentApi } from '@/api/customer'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'
import { formatComboTitle } from '@/utils/format'

const route = useRoute()
const store = useBookingStore()
const toast = useToastStore()

const isLoading = ref(true)
const paymentStatus = ref('')

// Làm sạch state khôi phục từ sessionStorage: loại bỏ dòng F&B/ghế hỏng (thiếu trường bắt buộc)
// có thể tồn từ phiên/phiên bản cũ khác shape — chặn tại NGUỒN để không entry hỏng nào vào store
// (store dùng chung với BookingView), tránh render ném lỗi làm trắng màn.
const sanitizeBookingState = (s) => {
  if (s && Array.isArray(s.selectedFnbs)) {
    s.selectedFnbs = s.selectedFnbs.filter(f => f && f.fnbItem && f.fnbItem.id != null)
  }
  if (s && Array.isArray(s.selectedSeats)) {
    s.selectedSeats = s.selectedSeats.filter(seat => seat && seat.seatId != null)
  }
  return s
}

onMounted(async () => {
  if (route.query.vnp_SecureHash) {
    // Quay về từ VNPAY: khôi phục state đã lưu trước khi chuyển hướng rồi xác thực chữ ký
    const savedState = sessionStorage.getItem('bookingState')
    if (savedState) store.$patch(sanitizeBookingState(JSON.parse(savedState)))
    try {
      const queryString = window.location.search.substring(1)
      const { data } = await paymentApi.vnpayReturn(queryString)
      if (data.code === '00') {
        paymentStatus.value = 'success'
        store.paymentMethod = 'VNPAY'
        if (!store.paidAt) store.paidAt = new Date().toISOString()
        sessionStorage.removeItem('bookingState')
      } else {
        paymentStatus.value = 'failed'
        toast.error('Giao dịch chưa thành công. Vui lòng thử lại hoặc liên hệ hỗ trợ.')
      }
    } catch (err) {
      paymentStatus.value = 'failed'
      toast.error(friendlyError(err, 'Giao dịch chưa thành công. Vui lòng thử lại hoặc liên hệ hỗ trợ.'))
    } finally {
      isLoading.value = false
    }
  } else {
    // Thanh toán nội bộ (chuyển khoản / ví): dùng trực tiếp state trong store
    // Dọn state VNPAY còn sót (nếu có phiên VNPAY dở dang trước đó) để không rò rỉ sang lần sau
    sessionStorage.removeItem('bookingState')
    paymentStatus.value = 'success'
    isLoading.value = false
  }
})

const paymentLabel = (m) => ({
  VNPAY: 'VNPAY',
  TRANSFER: 'Chuyển khoản (VietQR)',
  CASH: 'Tiền mặt',
  CARD: 'Thẻ ngân hàng'
}[m] || m || '—')

const showtimeText = computed(() => {
  const st = store.selectedShowtime
  if (!st?.startTime) return '—'
  const d = new Date(st.startTime)
  return `${d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} • ${d.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })}`
})

const paidAtText = computed(() =>
  store.paidAt ? new Date(store.paidAt).toLocaleString('vi-VN') : '—'
)

const seatsText = computed(() =>
  store.selectedSeats.map(s => s.label || (s.rowChar + s.colNum)).join(', ') || '—'
)

const totalAmount = computed(() => store.finalPrice || store.totalPrice || 0)
const discount = computed(() => {
  if (!store.selectedVoucher) return 0
  const d = (store.totalPrice || 0) - (store.finalPrice || store.totalPrice || 0)
  return d > 0 ? d : 0
})

// Chỉ hiện thông tin giảm giá khi đơn THỰC SỰ có voucher và được giảm
const hasVoucher = computed(() => !!store.selectedVoucher && discount.value > 0)

// Lọc bỏ dòng F&B hỏng (thiếu fnbItem) — có thể tồn từ state phiên cũ / sessionStorage lỗi thời.
// Một dòng hỏng KHÔNG được phép làm sập cả trang xác nhận đặt vé thành công.
const fnbLines = computed(() => (store.selectedFnbs || []).filter(f => f && f.fnbItem))
</script>

<template>
  <main class="min-h-screen flex flex-col items-center pt-24 sm:pt-28 pb-12 relative px-4 overflow-y-auto">
    <!-- Global background from App.vue will show here -->

    <!-- Success Header -->
    <div class="relative z-10 text-center mb-6" v-if="!isLoading && paymentStatus === 'success'">
      <div class="inline-flex items-center justify-center w-10 h-10 sm:w-12 sm:h-12 rounded-full border border-primary-container bg-transparent mb-3">
        <span class="material-symbols-outlined text-primary-container text-xl sm:text-2xl" style="font-variation-settings: 'FILL' 1;">check_circle</span>
      </div>
      <h1 class="font-headline font-extrabold text-xl sm:text-2xl md:text-3xl tracking-tighter text-on-surface uppercase mb-2 sm:mb-4">ĐẶT VÉ THÀNH CÔNG</h1>
    </div>

    <div class="relative z-10 text-center mb-8 sm:mb-12" v-else-if="isLoading">
      <div class="inline-flex items-center justify-center w-16 h-16 sm:w-20 sm:h-20 rounded-full mb-4 sm:mb-6">
        <span class="material-symbols-outlined text-primary text-4xl sm:text-6xl animate-spin">sync</span>
      </div>
      <h1 class="font-headline font-extrabold text-2xl sm:text-4xl md:text-5xl tracking-tighter text-on-surface uppercase mb-2">ĐANG XỬ LÝ...</h1>
      <p class="font-label text-xs sm:text-sm uppercase tracking-[0.2em] text-on-surface-variant">VUI LÒNG ĐỢI XÁC NHẬN TỪ CỔNG THANH TOÁN</p>
    </div>
    
    <div class="relative z-10 text-center mb-8 sm:mb-12" v-else>
      <div class="inline-flex items-center justify-center w-16 h-16 sm:w-20 sm:h-20 rounded-full bg-error-container mb-4 sm:mb-6 shadow-[0_0_40px_rgba(255,0,0,0.3)]">
        <span class="material-symbols-outlined text-on-error-container text-3xl sm:text-4xl" style="font-variation-settings: 'FILL' 1;">error</span>
      </div>
      <h1 class="font-headline font-extrabold text-2xl sm:text-4xl md:text-5xl tracking-tighter text-on-surface uppercase mb-2">GIAO DỊCH THẤT BẠI</h1>
      <p class="font-label text-xs sm:text-sm uppercase tracking-[0.2em] text-on-surface-variant">CÓ LỖI XẢY RA TRONG QUÁ TRÌNH THANH TOÁN</p>
    </div>

    <!-- Thông tin đơn hàng vừa đặt (dữ liệu thật) -->
    <div class="relative z-10 w-full max-w-md bg-[#1c1c1c] rounded-2xl overflow-hidden shadow-2xl border border-white/5" v-if="!isLoading && paymentStatus === 'success'">

      <!-- Movie & Cinema -->
      <div class="px-4 sm:px-6 pt-5 sm:pt-7 pb-4 sm:pb-5 text-center flex flex-col items-center border-b border-[#333]">
        <h2 class="font-headline font-bold text-lg sm:text-2xl text-white mb-2 uppercase leading-snug">{{ store.selectedMovie?.title || store.selectedShowtime?.movieTitle || '—' }}</h2>
        <span v-if="store.selectedShowtime?.formatName" class="inline-block px-2.5 py-0.5 border border-primary-container text-primary-container text-[10px] font-bold rounded-sm mb-2 sm:mb-3 uppercase">{{ store.selectedShowtime.formatName }}</span>
        <p class="font-headline font-bold text-base sm:text-lg text-[#00bcd4] mb-1">{{ store.selectedShowtime?.cinema?.cinemaName || store.selectedShowtime?.cinema?.name || '—' }}</p>
        <p v-if="store.selectedShowtime?.cinema?.address" class="text-on-surface-variant font-label text-[10px] sm:text-[11px] leading-relaxed max-w-[90%]">{{ store.selectedShowtime.cinema.address }}</p>
      </div>

      <!-- Chi tiết đơn -->
      <div class="px-6 py-4">
        <div class="flex justify-between gap-4 py-2.5 border-b border-[#333]/50">
          <span class="text-[#888] font-label text-xs flex-shrink-0">Suất chiếu</span>
          <span class="text-white font-headline font-bold text-sm text-right">{{ showtimeText }}</span>
        </div>
        <div class="flex justify-between gap-4 py-2.5 border-b border-[#333]/50">
          <span class="text-[#888] font-label text-xs flex-shrink-0">Ghế ngồi</span>
          <span class="text-primary-container font-headline font-bold text-sm text-right">{{ seatsText }}</span>
        </div>

        <!-- Combo / đồ ăn -->
        <div class="py-2.5 border-b border-[#333]/50">
          <div class="flex justify-between gap-4">
            <span class="text-[#888] font-label text-xs flex-shrink-0">Combo / Đồ ăn</span>
            <span v-if="fnbLines.length === 0" class="text-white font-headline font-bold text-sm">Không có</span>
          </div>
          <div v-if="fnbLines.length > 0" class="mt-3 space-y-3">
            <div v-for="(f, idx) in fnbLines" :key="f.fnbItem.id ?? idx" class="flex justify-between items-start gap-4 text-sm">
              <div>
                <div class="font-medium text-white/90">{{ f.quantity }} × {{ formatComboTitle(f.fnbItem.name).title }}</div>
                <div v-if="formatComboTitle(f.fnbItem.name).desc" class="text-xs text-gray-400 mt-0.5">{{ formatComboTitle(f.fnbItem.name).desc }}</div>
              </div>
              <div class="text-white/70 font-mono text-right whitespace-nowrap pt-0.5">{{ (f.quantity * ((f.snapshotPrice ?? f.fnbItem.price) || 0)).toLocaleString('vi-VN') }} đ</div>
            </div>
          </div>
        </div>

        <div class="flex justify-between gap-4 py-2.5 border-b border-[#333]/50">
          <span class="text-[#888] font-label text-xs flex-shrink-0">Phương thức thanh toán</span>
          <span class="text-white font-headline font-bold text-sm text-right">{{ paymentLabel(store.paymentMethod) }}</span>
        </div>
        <div class="flex justify-between gap-4 py-2.5 border-b border-[#333]/50">
          <span class="text-[#888] font-label text-xs flex-shrink-0">Thời gian thanh toán</span>
          <span class="text-white font-headline font-bold text-sm text-right">{{ paidAtText }}</span>
        </div>

        <!-- Tạm tính trước giảm (chỉ hiện khi có giảm) -->
        <div v-if="discount > 0" class="flex justify-between gap-4 py-2.5 border-b border-[#333]/50">
          <span class="text-[#888] font-label text-xs flex-shrink-0">Tạm tính</span>
          <span class="text-on-surface-variant text-sm line-through">{{ (store.totalPrice || 0).toLocaleString('vi-VN') }} đ</span>
        </div>
        <!-- Số tiền được giảm -->
        <div v-if="hasVoucher" class="flex justify-between gap-4 py-2.5 border-b border-[#333]/50">
          <span class="text-[#888] font-label text-xs flex-shrink-0">Số tiền được giảm</span>
          <span class="text-green-400 font-headline font-bold text-sm">-{{ discount.toLocaleString('vi-VN') }} đ</span>
        </div>

        <div class="flex justify-between items-center gap-4 pt-4 pb-1">
          <span class="text-white font-label text-xs uppercase tracking-widest font-bold">Tổng giá trị đơn</span>
          <span class="text-primary-container font-headline font-extrabold text-2xl">{{ totalAmount.toLocaleString('vi-VN') }} đ</span>
        </div>
      </div>

      <!-- Ghi chú gửi vé -->
      <div class="p-5 bg-[#161616] border-t border-[#333] text-left flex items-start gap-3">
        <span class="material-symbols-outlined text-primary-container text-lg flex-shrink-0">mark_email_read</span>
        <p class="text-on-surface-variant font-label text-[11px] leading-relaxed">Mã vé điện tử sẽ được gửi về email của bạn. Bạn cũng có thể xem lại vé trong mục "Vé của tôi".</p>
      </div>
    </div>

    <!-- Action Buttons -->
    <div class="relative z-10 mt-6 grid grid-cols-2 gap-3 w-full max-w-md" v-if="!isLoading && paymentStatus === 'success'">
      <router-link to="/" class="bg-transparent border border-outline-variant text-on-surface font-headline font-bold text-xs uppercase tracking-widest py-3.5 rounded-sm hover:bg-primary-container/10 transition-all active:scale-[0.98] text-center flex items-center justify-center">TRANG CHỦ</router-link>
      <router-link :to="{ path: '/profile/history', query: { code: store.bookingCode || store.bookingId } }" class="bg-primary-container text-on-primary font-headline font-bold text-xs uppercase tracking-widest py-3.5 rounded-sm hover:brightness-110 transition-all active:scale-[0.98] text-center flex items-center justify-center">VÉ CỦA TÔI</router-link>
    </div>

    <!-- Nút khi thất bại -->
    <div class="relative z-10 mt-6 w-full max-w-md" v-else-if="!isLoading">
      <router-link to="/" class="block bg-transparent border border-outline-variant text-on-surface font-headline font-bold text-xs uppercase tracking-widest py-3.5 rounded-sm hover:bg-primary-container/10 transition-all text-center">VỀ TRANG CHỦ</router-link>
    </div>
  </main>
</template>

<style scoped>
.noir-gradient { background: linear-gradient(180deg, rgba(19,19,19,0) 0%, rgba(19,19,19,1) 100%); }
</style>
