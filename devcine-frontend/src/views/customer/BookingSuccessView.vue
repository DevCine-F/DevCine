<script setup>
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { onMounted, ref } from 'vue'
import { useBookingStore } from '@/stores/booking'
import { paymentApi } from '@/api/customer'

const route = useRoute()
const router = useRouter()
const store = useBookingStore()

const isLoading = ref(true)
const paymentStatus = ref('')

onMounted(async () => {
  const savedState = sessionStorage.getItem('bookingState')
  if (savedState) {
    store.$patch(JSON.parse(savedState))
  }

  if (route.query.vnp_SecureHash) {
    try {
      const queryString = window.location.search.substring(1)
      const { data } = await paymentApi.vnpayReturn(queryString)
      if (data.code === '00') {
        paymentStatus.value = 'success'
      } else {
        paymentStatus.value = 'failed'
        alert(data.message || 'Giao dịch thất bại')
      }
    } catch (err) {
      console.error(err)
      paymentStatus.value = 'failed'
      alert('Giao dịch thất bại hoặc chữ ký không hợp lệ')
    } finally {
      isLoading.value = false
    }
  } else {
    paymentStatus.value = 'success'
    isLoading.value = false
  }
})
</script>

<template>
  <main class="min-h-screen flex flex-col items-center pt-24 pb-12 relative px-4 overflow-y-auto">
    <!-- Global background from App.vue will show here -->

    <!-- Success Header -->
    <div class="relative z-10 text-center mb-6" v-if="!isLoading && paymentStatus === 'success'">
      <div class="inline-flex items-center justify-center w-12 h-12 rounded-full border border-primary-container bg-transparent mb-3">
        <span class="material-symbols-outlined text-primary-container text-2xl" style="font-variation-settings: 'FILL' 1;">check_circle</span>
      </div>
      <h1 class="font-headline font-extrabold text-2xl md:text-3xl tracking-tighter text-on-surface uppercase mb-4">ĐẶT VÉ THÀNH CÔNG</h1>
    </div>

    <div class="relative z-10 text-center mb-12" v-else-if="isLoading">
      <div class="inline-flex items-center justify-center w-20 h-20 rounded-full mb-6">
        <span class="material-symbols-outlined text-primary text-6xl animate-spin">sync</span>
      </div>
      <h1 class="font-headline font-extrabold text-4xl md:text-5xl tracking-tighter text-on-surface uppercase mb-2">ĐANG XỬ LÝ...</h1>
      <p class="font-label text-sm uppercase tracking-[0.2em] text-on-surface-variant">VUI LÒNG ĐỢI XÁC NHẬN TỪ CỔNG THANH TOÁN</p>
    </div>
    
    <div class="relative z-10 text-center mb-12" v-else>
      <div class="inline-flex items-center justify-center w-20 h-20 rounded-full bg-error-container mb-6 shadow-[0_0_40px_rgba(255,0,0,0.3)]">
        <span class="material-symbols-outlined text-on-error-container text-4xl" style="font-variation-settings: 'FILL' 1;">error</span>
      </div>
      <h1 class="font-headline font-extrabold text-4xl md:text-5xl tracking-tighter text-on-surface uppercase mb-2">GIAO DỊCH THẤT BẠI</h1>
      <p class="font-label text-sm uppercase tracking-[0.2em] text-on-surface-variant">CÓ LỖI XẢY RA TRONG QUÁ TRÌNH THANH TOÁN</p>
    </div>

    <!-- Digital Ticket -->
    <div class="relative z-10 w-full max-w-md bg-[#222] rounded-xl overflow-hidden shadow-2xl" v-if="!isLoading && paymentStatus === 'success'" id="digital-ticket">
      
      <!-- Top Section: Movie & Cinema -->
      <div class="px-6 pt-8 pb-4 text-center flex flex-col items-center">
        <h2 class="font-headline font-bold text-2xl text-white mb-2 uppercase leading-snug">{{ store.selectedMovie?.title || store.selectedShowtime?.movie || 'THỎ ƠI !!' }}</h2>
        <span class="inline-block px-2.5 py-0.5 border border-primary-container text-primary-container text-[10px] font-bold rounded-sm mb-4 uppercase">{{ store.selectedShowtime?.formatName || '2D PHỤ ĐỀ' }}</span>
        <p class="font-headline font-bold text-lg text-[#00bcd4] mb-1">{{ store.selectedShowtime?.cinema?.cinemaName || 'Beta Mỹ Đình' }}</p>
        <p class="text-on-surface-variant font-label text-[11px] leading-relaxed max-w-[90%]">{{ store.selectedShowtime?.cinema?.address || 'Tầng hầm B1, tòa nhà Golden Palace, Phường Từ Liêm, Thành phố Hà Nội.' }}</p>
      </div>

      <!-- Divider -->
      <div class="px-6 py-2">
        <div class="w-full border-t border-[#333]"></div>
      </div>

      <!-- Middle Section: Transaction Details (List format) -->
      <div class="px-6 py-4">
        <div class="flex justify-between py-2 border-b border-[#333]/50">
          <span class="text-[#888] font-label text-xs">Phòng chiếu</span>
          <span class="text-white font-headline font-bold text-sm">{{ store.selectedShowtime?.room?.name || 'Phòng 01' }}</span>
        </div>
        <div class="flex justify-between py-2 border-b border-[#333]/50">
          <span class="text-[#888] font-label text-xs">Ghế ngồi</span>
          <span class="text-white font-headline font-bold text-sm">{{ store.selectedSeats.map(s => s.rowChar + s.colNum).join(', ') || 'J1, J2' }}</span>
        </div>
        <div class="flex justify-between py-2 border-b border-[#333]/50">
          <span class="text-[#888] font-label text-xs">Phương thức thanh toán</span>
          <span class="text-white font-headline font-bold text-sm">VNPAY</span>
        </div>
        <div class="flex justify-between py-2 border-b border-[#333]/50">
          <span class="text-[#888] font-label text-xs">Combo/Dịch vụ</span>
          <span class="text-white font-headline font-bold text-sm">0 đ</span>
        </div>
        <div class="flex justify-between py-2 border-b border-[#333]/50">
          <span class="text-[#888] font-label text-xs">Giảm giá</span>
          <span class="text-white font-headline font-bold text-sm">0 đ</span>
        </div>
        <div class="flex justify-between py-3">
          <span class="text-white font-label text-xs uppercase tracking-widest font-bold">Tổng thanh toán</span>
          <span class="text-primary-container font-headline font-bold text-lg">{{ store.totalPrice ? store.totalPrice.toLocaleString('vi-VN') : '168.000' }} đ</span>
        </div>
      </div>

      <!-- Perforated Divider -->
      <div class="relative flex items-center justify-center h-4 my-2">
        <div class="absolute left-0 -translate-x-1/2 w-4 h-4 bg-[#0A0A0A] rounded-full border border-[#333]/50"></div>
        <div class="absolute right-0 translate-x-1/2 w-4 h-4 bg-[#0A0A0A] rounded-full border border-[#333]/50"></div>
        <div class="w-full border-t-[3px] border-dotted border-[#555]"></div>
      </div>

      <!-- Bottom Section: Stub with QR -->
      <div class="px-6 py-6 text-center flex flex-col items-center">
        <p class="font-label text-[10px] text-[#888] uppercase tracking-[0.1em] mb-1">MÃ VÉ (RESERVATION CODE)</p>
        <p class="font-headline font-bold text-3xl text-white mb-6 tracking-wider">{{ store.bookingCode || '6365611024328022' }}</p>

        <div class="p-3 bg-white rounded-md mb-6 inline-block shadow-[0_0_15px_rgba(255,255,255,0.1)]">
          <img :src="'https://api.qrserver.com/v1/create-qr-code/?size=150x150&margin=0&data=' + (store.bookingCode || 'DEV-CINE')" class="w-32 h-32 mix-blend-multiply" />
        </div>

        <p class="font-label text-[10px] text-[#888] uppercase tracking-[0.1em] mb-1">SUẤT CHIẾU (SESSION)</p>
        <p class="font-headline font-bold text-xl text-white">
          {{ store.selectedShowtime ? new Date(store.selectedShowtime.startTime).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'}) : '22:00' }} - {{ store.selectedShowtime ? new Date(store.selectedShowtime.startTime).toLocaleDateString('vi-VN') : '21/02/2026' }}
        </p>
      </div>

      <!-- Instructions Footer -->
      <div class="p-5 bg-[#1a1a1a] border-t border-[#333] text-left">
        <p class="text-white font-label text-xs mb-1.5 leading-relaxed">Quý khách vui lòng tới quầy dịch vụ xuất trình mã vé này để được nhận vé.</p>
        <p class="text-[#888] italic font-label text-[10px] leading-relaxed">Please go to the service counter and present your booking code to receive the physical ticket to check-in.</p>
      </div>
    </div>

    <!-- Action Buttons -->
    <div class="relative z-10 mt-6 grid grid-cols-2 gap-3 w-full max-w-md">
      <router-link to="/" class="bg-transparent border border-outline-variant text-on-surface font-headline font-bold text-xs uppercase tracking-widest py-3 rounded-sm hover:bg-primary-container/10 transition-all active:scale-[0.98] text-center flex items-center justify-center">TRANG CHỦ</router-link>
      <router-link to="/profile" class="bg-transparent border border-outline-variant text-on-surface font-headline font-bold text-xs uppercase tracking-widest py-3 rounded-sm hover:bg-primary-container/10 transition-all active:scale-[0.98] text-center flex items-center justify-center">VÉ CỦA TÔI</router-link>
      
      <!-- Save Image Button placeholder (not functional yet) -->
      <button @click="() => alert('Tính năng tải vé đang được phát triển!')" class="col-span-2 bg-primary-container text-on-primary font-headline font-bold text-sm uppercase tracking-widest py-3.5 rounded-sm hover:bg-primary-fixed-dim transition-all active:scale-[0.98] text-center flex items-center justify-center gap-2">
        <span class="material-symbols-outlined text-[20px]">download</span> LƯU ẢNH VÉ
      </button>
    </div>
  </main>
</template>

<style scoped>
.noir-gradient { background: linear-gradient(180deg, rgba(19,19,19,0) 0%, rgba(19,19,19,1) 100%); }
</style>
