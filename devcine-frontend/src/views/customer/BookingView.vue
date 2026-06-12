<script setup>
import { RouterLink, useRouter } from 'vue-router'
import { useBookingStore } from '@/stores/booking'
import { computed, onMounted, ref } from 'vue'

const store = useBookingStore()
const router = useRouter()
const paymentMethod = ref('VNPAY')

onMounted(async () => {
  if (!store.selectedShowtime) {
    // If accessed directly without a showtime, redirect back
    router.push('/lich-chieu')
    return
  }
  await store.fetchSeats()
  await store.fetchFnbs()
})

const handleSeatClick = (seat) => {
  if (seat.status === 'AVAILABLE') {
    store.toggleSeat(seat)
  }
}

const isSeatSelected = (seat) => {
  if (!seat) return false
  return store.selectedSeats.some(s => s.seatId === seat.seatId)
}

const getSeatAt = (row, col) => {
  return store.availableSeats.find(s => s.gridRow === row && s.gridCol === col)
}

const getRowChar = (row) => {
  const seat = store.availableSeats.find(s => s.gridRow === row)
  return seat ? seat.rowChar : ''
}

const isHiddenBecauseSweetbox = (row, col) => {
  if (col === 0) return false;
  const prevSeat = getSeatAt(row, col - 1);
  return prevSeat && prevSeat.seatType === 'SWEETBOX';
}

const proceedToPayment = async () => {
  const success = await store.holdSeatsAndProceed()
  if (success) {
    const paid = await store.confirmPayment(paymentMethod.value)
    if (paid) {
      router.push('/success')
    } else {
      alert('Payment failed')
    }
  } else {
    alert('Failed to hold seats. They might have been taken.')
  }
}

</script>

<template>
  <main class="pt-32 pb-20 max-w-[1440px] mx-auto px-10 flex flex-col lg:flex-row gap-12">
    <!-- Main Content Area -->
    <div class="flex-grow space-y-16">
      <!-- Section 1: Seat Selection -->
      <section>
        <div class="mb-12">
          <h1 class="font-headline text-3xl font-bold tracking-tight mb-2 uppercase italic text-primary-container">01. Chọn Chỗ Ngồi</h1>
          <div class="flex items-center gap-4 text-on-surface-variant" v-if="store.selectedShowtime">
            <span class="flex items-center gap-1"><span class="material-symbols-outlined text-sm">location_on</span> {{ store.selectedShowtime.cinema?.cinemaName }}</span>
            <span class="w-1 h-1 rounded-full bg-outline-variant"></span>
            <span class="flex items-center gap-1"><span class="material-symbols-outlined text-sm">calendar_today</span> {{ new Date(store.selectedShowtime.startTime).toLocaleDateString() }}</span>
            <span class="w-1 h-1 rounded-full bg-outline-variant"></span>
            <span class="flex items-center gap-1"><span class="material-symbols-outlined text-sm">schedule</span> {{ new Date(store.selectedShowtime.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }}</span>
          </div>
        </div>
        <div class="relative glass-card glass-shine-edge p-12 overflow-hidden rounded-3xl">
          <!-- Screen -->
          <div class="w-full h-2 bg-gradient-to-r from-transparent via-primary-container to-transparent opacity-50 mb-4 blur-sm"></div>
          <div class="w-full h-1 bg-primary-container mb-20 screen-curve rounded-[50%_/_10%]"></div>
          <p class="text-center text-label-sm font-bold uppercase tracking-[0.3em] text-outline-variant mb-16">Màn Hình / Screen</p>
          
          <!-- Seats Grid -->
          <div class="seat-grid w-full overflow-x-auto flex flex-col gap-3 mb-16" v-if="store.availableSeats.length">
            <div class="flex flex-col gap-3 mx-auto min-w-max pb-4">
              <div v-for="row in store.matrixRow" :key="row" class="flex items-center gap-2 justify-center">
                <div class="w-6 text-label-sm font-bold text-outline-variant text-center">{{ getRowChar(row - 1) }}</div>
                
                <template v-for="col in store.matrixCol" :key="col">
                  <template v-if="getSeatAt(row - 1, col - 1)">
                    <div @click="handleSeatClick(getSeatAt(row - 1, col - 1))"
                         :class="[
                           'aspect-square w-8 flex items-center justify-center text-[10px] font-bold cursor-pointer transition-colors',
                           getSeatAt(row - 1, col - 1).status === 'AVAILABLE' && !isSeatSelected(getSeatAt(row - 1, col - 1)) ? 
                             (getSeatAt(row - 1, col - 1).seatType === 'VIP' ? 'border-2 border-primary-container/60 bg-primary-container/10 hover:bg-primary-container/30' : 
                              getSeatAt(row - 1, col - 1).seatType === 'SWEETBOX' ? 'border-2 border-pink-500/50 bg-pink-500/10 rounded-t-xl hover:bg-pink-500/20' : 
                              'border border-outline-variant/30 hover:border-primary-container') : '',
                           getSeatAt(row - 1, col - 1).status !== 'AVAILABLE' ? 'bg-surface-variant/30 cursor-not-allowed opacity-50' : '',
                           isSeatSelected(getSeatAt(row - 1, col - 1)) ? 'bg-primary-container text-on-primary border-primary-container' : '',
                           getSeatAt(row - 1, col - 1).seatType === 'SWEETBOX' ? 'w-[4.5rem]' : ''
                         ]">
                      {{ isSeatSelected(getSeatAt(row - 1, col - 1)) ? getSeatAt(row - 1, col - 1).rowChar + getSeatAt(row - 1, col - 1).colNum : '' }}
                    </div>
                  </template>
                  <template v-else-if="!isHiddenBecauseSweetbox(row - 1, col - 1)">
                    <div class="aspect-square w-8 opacity-0"></div>
                  </template>
                </template>

                <div class="w-6 text-label-sm font-bold text-outline-variant text-center">{{ getRowChar(row - 1) }}</div>
              </div>
            </div>
          </div>
          
          <!-- Legend -->
          <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-6 border-t border-outline-variant/10 pt-10">
            <div class="flex items-center gap-3">
              <div class="w-5 h-5 border border-outline-variant/40"></div>
              <div class="flex flex-col">
                <span class="text-[10px] font-bold uppercase tracking-wider text-on-surface">Standard</span>
                <span class="text-[10px] text-on-surface-variant">110.000 VNĐ</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-5 h-5 border-2 border-primary-container/60 bg-primary-container/10"></div>
              <div class="flex flex-col">
                <span class="text-[10px] font-bold uppercase tracking-wider text-primary-container">VIP</span>
                <span class="text-[10px] text-on-surface-variant">150.000 VNĐ</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-10 h-5 border-2 border-pink-500/50 bg-pink-500/10 rounded-t-lg"></div>
              <div class="flex flex-col">
                <span class="text-[10px] font-bold uppercase tracking-wider text-pink-400">Sweetbox</span>
                <span class="text-[10px] text-on-surface-variant">300.000 VNĐ</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-5 h-5 bg-primary-container"></div>
              <span class="text-[10px] font-bold uppercase tracking-wider text-on-surface">Đang chọn</span>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-5 h-5 bg-surface-variant/30"></div>
              <span class="text-[10px] font-bold uppercase tracking-wider text-on-surface">Đã đặt</span>
            </div>
          </div>
        </div>
      </section>
      
      <!-- Section 2: Concessions Selection -->
      <section>
        <div class="mb-8">
          <h1 class="font-headline text-3xl font-bold tracking-tight mb-2 uppercase italic text-primary-container">02. Bắp Nước & Ưu Đãi</h1>
          <p class="text-sm text-on-surface-variant">Thêm hương vị cho trải nghiệm điện ảnh của bạn</p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="glass-card p-6 flex gap-6 hover:border-primary-container/30 transition-all group rounded-2xl" v-for="fnb in store.availableFnbs" :key="fnb.id">
            <div class="w-28 h-28 flex-shrink-0 bg-black overflow-hidden relative rounded-xl">
              <img :src="fnb.imageUrl || '/images/Hopper.webp'" class="w-full h-full object-cover opacity-80 group-hover:scale-110 transition-transform duration-500"/>
              <div class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
            </div>
            <div class="flex flex-col justify-between flex-grow">
              <div>
                <h3 class="font-headline font-bold text-lg mb-1">{{ fnb.name }}</h3>
                <p class="text-xs text-on-surface-variant line-clamp-2">{{ fnb.description }}</p>
              </div>
              <div class="flex items-center justify-between mt-2">
                <span class="font-headline font-bold text-primary-container">{{ fnb.price?.toLocaleString('vi-VN') }} VNĐ</span>
                <div class="flex items-center bg-surface-container-high rounded-full px-2 py-1">
                  <button @click="store.updateFnb(fnb, (store.selectedFnbs.find(f => f.fnbItem.id === fnb.id)?.quantity || 0) - 1)" class="w-6 h-6 flex items-center justify-center hover:text-primary-container transition-colors"><span class="material-symbols-outlined text-sm">remove</span></button>
                  <span class="w-8 text-center text-xs font-bold">{{ store.selectedFnbs.find(f => f.fnbItem.id === fnb.id)?.quantity || 0 }}</span>
                  <button @click="store.updateFnb(fnb, (store.selectedFnbs.find(f => f.fnbItem.id === fnb.id)?.quantity || 0) + 1)" class="w-6 h-6 flex items-center justify-center hover:text-primary-container transition-colors"><span class="material-symbols-outlined text-sm">add</span></button>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="mt-16 mb-8 border-t border-outline-variant/10 pt-16">
          <h1 class="font-headline text-3xl font-bold tracking-tight mb-2 uppercase italic text-primary-container">03. Phương Thức Thanh Toán</h1>
          <p class="text-sm text-on-surface-variant">Chọn phương thức thanh toán phù hợp nhất</p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
            <label class="glass-card p-4 rounded-xl flex items-center gap-4 cursor-pointer hover:border-primary-container transition-colors" :class="{'border-primary-container': paymentMethod === 'VNPAY'}">
                <input type="radio" value="VNPAY" v-model="paymentMethod" class="w-4 h-4 text-primary-container focus:ring-primary-container border-outline-variant/30 bg-transparent">
                <span class="font-bold">Thanh toán qua VNPAY</span>
            </label>
            <label class="glass-card p-4 rounded-xl flex items-center gap-4 cursor-pointer hover:border-primary-container transition-colors" :class="{'border-primary-container': paymentMethod === 'MOMO'}">
                <input type="radio" value="MOMO" v-model="paymentMethod" class="w-4 h-4 text-primary-container focus:ring-primary-container border-outline-variant/30 bg-transparent">
                <span class="font-bold">Thanh toán qua MoMo</span>
            </label>
            <label class="glass-card p-4 rounded-xl flex items-center gap-4 cursor-pointer hover:border-primary-container transition-colors" :class="{'border-primary-container': paymentMethod === 'TRANSFER'}">
                <input type="radio" value="TRANSFER" v-model="paymentMethod" class="w-4 h-4 text-primary-container focus:ring-primary-container border-outline-variant/30 bg-transparent">
                <span class="font-bold">Chuyển khoản thủ công</span>
            </label>
        </div>
      </section>
    </div>

    <!-- Persistent Sidebar Summary -->
    <aside class="w-full lg:w-[400px]">
      <div class="glass-card glass-shine-edge sticky top-28 shadow-2xl overflow-hidden rounded-3xl">
        <!-- Movie Header -->
        <div class="p-8 pb-6 border-b border-outline-variant/10">
          <div class="flex gap-6">
            <div class="w-20 h-28 flex-shrink-0 shadow-lg">
              <img src="/images/Hopper.webp" class="w-full h-full object-cover"/>
            </div>
            <div class="flex flex-col justify-center">
              <span class="bg-error-container text-[9px] font-bold uppercase tracking-widest px-2 py-0.5 w-fit mb-2 text-white">T18</span>
              <h2 class="font-headline text-lg font-bold leading-tight uppercase tracking-tight mb-1">DUNE: PART TWO</h2>
              <p class="text-xs text-on-surface-variant font-label">DevCine Landmark 81 • Phòng 05</p>
            </div>
          </div>
        </div>
        <!-- Detailed Selections -->
        <div class="p-8 space-y-6">
          <div>
            <div class="flex justify-between items-center mb-3">
              <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ghế đã chọn</span>
              <span class="text-xs font-bold text-primary-container">{{ store.selectedSeats.map(s => s.rowChar + s.colNum).join(', ') }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-on-surface/60">{{ store.selectedSeats.length }} x ghế</span>
              <span class="font-semibold">{{ store.selectedSeats.reduce((acc, s) => acc + s.price, 0).toLocaleString('vi-VN') }} VNĐ</span>
            </div>
          </div>
          <div class="pt-6 border-t border-outline-variant/10" v-if="store.selectedFnbs.length > 0">
            <div class="flex justify-between items-center mb-3">
              <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Bắp nước</span>
              <span class="text-xs font-bold text-primary-container">{{ store.selectedFnbs.reduce((acc, f) => acc + f.quantity, 0) }} sản phẩm</span>
            </div>
            <div class="flex justify-between text-sm" v-for="fnb in store.selectedFnbs" :key="fnb.fnbItem.id">
              <span class="text-on-surface/60">{{ fnb.quantity }} x {{ fnb.fnbItem.name }}</span>
              <span class="font-semibold">{{ (fnb.quantity * fnb.fnbItem.price).toLocaleString('vi-VN') }} VNĐ</span>
            </div>
          </div>
          <!-- Total Calculation -->
          <div class="pt-6 border-t border-outline-variant/10">
            <div class="bg-black/40 border border-white/5 p-5 rounded-xl">
              <div class="flex justify-between items-center mb-1">
                <span class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Tổng thanh toán</span>
                <span class="text-2xl font-headline font-extrabold text-primary-container">{{ store.totalPrice.toLocaleString('vi-VN') }} VNĐ</span>
              </div>
              <p class="text-[10px] text-outline-variant text-right italic">(VAT & Phí dịch vụ đã bao gồm)</p>
            </div>
          </div>
          <!-- Action Button -->
          <button @click="proceedToPayment" :disabled="store.selectedSeats.length === 0" class="w-full bg-primary-container text-on-primary py-4 rounded-xl font-headline font-extrabold tracking-[0.2em] hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-2 group disabled:opacity-50 disabled:cursor-not-allowed">
            XÁC NHẬN THANH TOÁN
            <span class="material-symbols-outlined text-lg group-hover:translate-x-1 transition-transform">arrow_forward</span>
          </button>
          <p class="text-[10px] text-center text-on-surface-variant/40 leading-relaxed uppercase tracking-tighter">
            Thanh toán an toàn qua Cổng liên kết quốc tế
          </p>
        </div>
      </div>
    </aside>
  </main>
</template>

<style scoped>
.seat-grid { perspective: 1000px; }
.screen-curve { box-shadow: 0 -20px 50px -10px rgba(245, 197, 24, 0.3); }
</style>
