<script setup>
import { RouterLink, useRouter } from 'vue-router'
import { useBookingStore } from '@/stores/booking'
import { paymentApi, walletApi, voucherApi } from '@/api/customer'
import { useAuthStore } from '@/stores/auth'
import { computed, onMounted, ref, watch } from 'vue'

const store = useBookingStore()
const router = useRouter()
const authStore = useAuthStore()

const paymentMethod = ref('VNPAY')
const walletBalance = ref(0)
const vouchers = ref([])
const voucherCode = ref('')
const voucherError = ref('')
const voucherSuccess = ref('')
const discountAmount = ref(0)

const finalPaymentPrice = computed(() => {
  const total = store.totalPrice
  const final = total - discountAmount.value
  return final < 0 ? 0 : final
})

onMounted(async () => {
  if (!store.selectedShowtime) {
    // If accessed directly without a showtime, redirect back
    router.push('/lich-chieu')
    return
  }
  await store.fetchSeats()
  await store.fetchFnbs()
  
  if (authStore.isAuthenticated && authStore.user?.id) {
    // Fetch wallet info
    try {
      const walletRes = await walletApi.getWallet(authStore.user.id)
      walletBalance.value = walletRes.data.balance
    } catch (err) {
      console.error('Failed to fetch wallet info', err)
    }
    // Fetch active vouchers
    try {
      const voucherRes = await voucherApi.getActiveVouchers(authStore.user.id)
      vouchers.value = voucherRes.data
    } catch (err) {
      console.error('Failed to fetch vouchers', err)
    }
  }
})

const isApplyingVoucher = ref(false)

const refreshVouchers = async () => {
  if (!authStore.user?.id) return
  try {
    const voucherRes = await voucherApi.getActiveVouchers(authStore.user.id)
    vouchers.value = voucherRes.data
  } catch (err) {
    // giữ danh sách cũ nếu refresh lỗi
  }
}

const successText = (discountType, discountValue) =>
  `Áp dụng thành công! Được giảm ${discountType === 'PERCENTAGE' ? Number(discountValue) + '%' : Number(discountValue).toLocaleString('vi-VN') + ' VNĐ'}`

const applyVoucherCode = async () => {
  voucherError.value = ''
  voucherSuccess.value = ''
  if (!voucherCode.value.trim()) return

  if (!authStore.isAuthenticated || !authStore.user?.id) {
    voucherError.value = 'Vui lòng đăng nhập để sử dụng mã giảm giá!'
    return
  }

  isApplyingVoucher.value = true
  try {
    // /apply: dùng voucher đã lưu, hoặc tự lưu mã hợp lệ rồi áp dụng (chặn mã đổi-điểm / hết hạn / không tồn tại)
    const { data } = await voucherApi.applyCode(authStore.user.id, voucherCode.value.trim())
    store.selectedVoucher = data
    voucherSuccess.value = successText(data.discountType, data.discountValue)
    voucherCode.value = ''
    calculateDiscount()
    await refreshVouchers()
  } catch (err) {
    voucherError.value = err.response?.data?.message || err.response?.data?.error || 'Mã giảm giá không hợp lệ!'
    store.selectedVoucher = null
    discountAmount.value = 0
  } finally {
    isApplyingVoucher.value = false
  }
}

const selectVoucher = (v) => {
  store.selectedVoucher = {
    id: v.id,
    code: v.promotion.code,
    discountType: v.promotion.discountType,
    discountValue: v.promotion.discountValue
  }
  voucherSuccess.value = successText(v.promotion.discountType, v.promotion.discountValue)
  voucherError.value = ''
  calculateDiscount()
}

const removeVoucher = () => {
  store.selectedVoucher = null
  discountAmount.value = 0
  voucherSuccess.value = ''
  voucherError.value = ''
}

const calculateDiscount = () => {
  if (!store.selectedVoucher) {
    discountAmount.value = 0
    return
  }
  const total = store.selectedSeats.reduce((acc, s) => acc + s.price, 0) + store.selectedFnbs.reduce((acc, f) => acc + f.fnbItem.price * f.quantity, 0)
  if (store.selectedVoucher.discountType === 'PERCENTAGE') {
    discountAmount.value = total * store.selectedVoucher.discountValue / 100
  } else {
    discountAmount.value = store.selectedVoucher.discountValue
  }
}

// Recalculate discount if seat or fnb selections change
watch(() => [store.selectedSeats.length, store.selectedFnbs.length], () => {
  calculateDiscount()
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

const getBookingSeatClass = (seat) => {
  const isSelected = isSeatSelected(seat);
  const isAvailable = seat.status === 'AVAILABLE';
  const type = seat.seatType; 
  
  const baseClasses = 'flex items-center justify-center text-[10px] font-bold transition-all duration-200';
  const shadowClasses = 'shadow-[0_4px_6px_rgba(0,0,0,0.3),inset_0_1px_1px_rgba(255,255,255,0.1)]';
  const standardSize = 'aspect-square w-10';
  const doubleSize = 'h-10 w-[5.5rem]'; // w-10 (40px) * 2 + gap-2 (8px) = 88px = 5.5rem
  
  if (!isAvailable) {
    const sizeClass = type === 'SWEETBOX' ? `${doubleSize} rounded-xl` : `${standardSize} rounded-lg`;
    return `${baseClasses} ${sizeClass} bg-surface-container-high border border-white/5 text-white/20 cursor-not-allowed pointer-events-none opacity-50`;
  }
  
  if (isSelected) {
     const sizeClass = type === 'SWEETBOX' ? `${doubleSize} rounded-t-2xl rounded-b-lg` : `${standardSize} rounded-lg`;
     return `${baseClasses} ${sizeClass} bg-gradient-to-br from-primary to-amber-600 text-on-primary shadow-[0_0_20px_rgba(245,197,24,0.3)] scale-[1.02] border-none cursor-pointer`;
  }
  
  // Available and Not Selected
  switch (type) {
    case 'VIP': 
      return `${baseClasses} ${standardSize} ${shadowClasses} rounded-lg bg-gradient-to-b from-red-700/90 to-red-900/90 border border-red-500/50 text-red-100 shadow-[0_0_15px_rgba(220,38,38,0.2)] hover:shadow-[0_0_20px_rgba(220,38,38,0.4)] hover:-translate-y-0.5 hover:brightness-110 cursor-pointer`;
    case 'SWEETBOX': 
      return `${baseClasses} ${doubleSize} ${shadowClasses} rounded-t-2xl rounded-b-lg bg-gradient-to-b from-purple-600/90 to-purple-900/90 border border-purple-500/50 text-purple-100 shadow-[0_0_15px_rgba(147,51,234,0.2)] hover:shadow-[0_0_20px_rgba(147,51,234,0.4)] hover:-translate-y-0.5 hover:brightness-110 cursor-pointer`;
    default: // STANDARD
      return `${baseClasses} ${standardSize} ${shadowClasses} rounded-lg bg-slate-800/80 border border-slate-600/50 text-slate-300 hover:brightness-125 hover:-translate-y-0.5 hover:shadow-lg cursor-pointer`;
  }
}

const proceedToPayment = async () => {
  
  const success = await store.holdSeatsAndProceed(paymentMethod.value)
  if (success) {
    if (paymentMethod.value === 'VNPAY') {
      try {
        // Dùng giá cuối do backend tính ở bước giữ ghế (đã trừ voucher) để tránh lệch/giảm 2 lần
        const { data } = await paymentApi.createPayment(store.finalPrice, store.bookingId);
        if (data.code === '00') {
          sessionStorage.setItem('bookingState', JSON.stringify(store.$state));
          window.location.href = data.data; // Redirect to VNPAY Sandbox
        } else {
          alert('Không thể tạo liên kết thanh toán VNPay');
        }
      } catch (err) {
        console.error(err);
        alert('Lỗi tạo cổng thanh toán');
      }
    } else {
      const paid = await store.confirmPayment(paymentMethod.value)
      if (paid) {
        router.push('/success')
      } else {
        alert('Thanh toán thất bại! Vui lòng thử lại.')
      }
    }
  } else {
    alert('Giữ ghế thất bại. Ghế có thể đã được đặt hoặc hết hạn giữ.')
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
          <div class="w-full flex flex-col items-center flex-shrink-0 relative py-8 mb-12">
            <div class="absolute top-0 w-full h-[100px] bg-gradient-to-b from-primary/5 to-transparent pointer-events-none"></div>
            <div class="w-2/3 h-1.5 bg-primary/70 rounded-full shadow-[0_2px_15px_rgba(245,197,24,0.2)] mb-4 border border-primary/20"></div>
            <p class="text-[9px] font-bold uppercase tracking-[0.6em] text-primary/50 relative z-10">MÀN HÌNH / SCREEN</p>
          </div>
          
          <!-- Seats Grid -->
          <div class="seat-grid w-full overflow-x-auto flex flex-col gap-3 mb-16 relative" v-if="store.availableSeats.length">
            <div class="absolute inset-0 opacity-[0.15] pointer-events-none" style="background-image: radial-gradient(rgba(255, 255, 255, 0.4) 1px, transparent 1px); background-size: 24px 24px;"></div>
            <div class="relative z-10 flex flex-col gap-3 mx-auto min-w-max pb-4 bg-black/40 backdrop-blur-sm p-8 rounded-3xl border border-white/5 shadow-2xl">
              <div v-for="row in store.matrixRow" :key="row" class="flex items-center gap-2 justify-center">
                <div class="w-6 text-label-sm font-bold text-outline-variant text-center">{{ getRowChar(row - 1) }}</div>
                
                <template v-for="col in store.matrixCol" :key="col">
                  <template v-if="getSeatAt(row - 1, col - 1)">
                    <div @click="handleSeatClick(getSeatAt(row - 1, col - 1))"
                         :class="getBookingSeatClass(getSeatAt(row - 1, col - 1))">
                      {{ getSeatAt(row - 1, col - 1).rowChar + getSeatAt(row - 1, col - 1).colNum }}
                    </div>
                  </template>
                  <template v-else-if="!isHiddenBecauseSweetbox(row - 1, col - 1)">
                    <div class="aspect-square w-10 opacity-0"></div>
                  </template>
                </template>

                <div class="w-6 text-label-sm font-bold text-outline-variant text-center">{{ getRowChar(row - 1) }}</div>
              </div>
            </div>
          </div>
          
          <!-- Legend -->
          <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-6 border-t border-outline-variant/10 pt-10">
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-lg bg-slate-800/80 border border-slate-600/50 shadow-[0_4px_6px_rgba(0,0,0,0.3),inset_0_1px_1px_rgba(255,255,255,0.1)]"></div>
              <div class="flex flex-col">
                <span class="text-[10px] font-bold uppercase tracking-wider text-slate-300">Standard</span>
                <span class="text-[10px] text-on-surface-variant">110.000 VNĐ</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-lg bg-gradient-to-b from-red-700/90 to-red-900/90 border border-red-500/50 shadow-[0_4px_6px_rgba(0,0,0,0.3),inset_0_1px_1px_rgba(255,255,255,0.1),0_0_15px_rgba(220,38,38,0.2)]"></div>
              <div class="flex flex-col">
                <span class="text-[10px] font-bold uppercase tracking-wider text-red-400">VIP</span>
                <span class="text-[10px] text-on-surface-variant">150.000 VNĐ</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-12 h-8 rounded-t-xl rounded-b-md bg-gradient-to-b from-purple-600/90 to-purple-900/90 border border-purple-500/50 shadow-[0_4px_6px_rgba(0,0,0,0.3),inset_0_1px_1px_rgba(255,255,255,0.1),0_0_15px_rgba(147,51,234,0.2)]"></div>
              <div class="flex flex-col">
                <span class="text-[10px] font-bold uppercase tracking-wider text-purple-400">Sweetbox</span>
                <span class="text-[10px] text-on-surface-variant">300.000 VNĐ</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-amber-600 shadow-[0_0_20px_rgba(245,197,24,0.3)]"></div>
              <span class="text-[10px] font-bold uppercase tracking-wider text-primary">Đang chọn</span>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-lg bg-surface-container-high border border-white/5 opacity-50"></div>
              <span class="text-[10px] font-bold uppercase tracking-wider text-on-surface">Đã đặt</span>
            </div>
          </div>
        </div>
      </section>
      
      <!-- Section 2: Combo / F&B Selection -->
      <section>
        <div class="mb-8">
          <h1 class="font-headline text-3xl font-bold tracking-tight mb-2 uppercase italic text-primary-container">02. Combo - Đồ Ăn & Nước Uống</h1>
          <p class="text-sm text-on-surface-variant">Chọn combo bắp nước & đồ ăn kèm cho buổi xem phim (không bắt buộc)</p>
        </div>

        <!-- Empty state khi rạp chưa có combo -->
        <div v-if="store.availableFnbs.length === 0" class="glass-card p-10 rounded-2xl text-center">
          <span class="material-symbols-outlined text-4xl text-on-surface-variant/40 mb-2">fastfood</span>
          <p class="text-sm text-on-surface-variant">Hiện chưa có combo nào. Bạn có thể tiếp tục đặt vé.</p>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-6">
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
        
        <!-- Add Voucher Input & List -->
        <div class="mt-16 mb-8 border-t border-outline-variant/10 pt-16">
          <h1 class="font-headline text-3xl font-bold tracking-tight mb-2 uppercase italic text-primary-container">03. Ưu Đãi / Mã Giảm Giá</h1>
          <p class="text-sm text-on-surface-variant">Chọn voucher sẵn có hoặc nhập mã giảm giá</p>
        </div>
        
        <div class="glass-card p-6 rounded-2xl space-y-6">
          <!-- Code input -->
          <div class="flex flex-col sm:flex-row gap-4">
            <input 
              v-model="voucherCode" 
              type="text" 
              placeholder="Nhập mã giảm giá..."
              class="flex-grow bg-surface-container-high border border-outline-variant/30 focus:border-primary focus:ring-1 focus:ring-primary rounded-xl px-4 py-3 text-sm text-on-surface font-mono uppercase tracking-wider"
            >
            <button @click="applyVoucherCode" :disabled="isApplyingVoucher" class="bg-primary text-on-primary font-bold px-6 py-3 rounded-xl hover:brightness-115 active:scale-[0.98] transition-all cursor-pointer disabled:opacity-60">
              {{ isApplyingVoucher ? 'Đang áp dụng...' : 'Áp dụng' }}
            </button>
          </div>
          <p v-if="voucherError" class="text-xs text-error font-bold px-2">{{ voucherError }}</p>
          <div v-if="voucherSuccess" class="flex items-center justify-between px-2">
            <p class="text-xs text-green-400 font-bold">{{ voucherSuccess }}</p>
            <button v-if="store.selectedVoucher" @click="removeVoucher" class="text-xs text-on-surface-variant hover:text-error font-bold flex items-center gap-1 transition-colors">
              <span class="material-symbols-outlined text-sm">close</span> Bỏ chọn
            </button>
          </div>

          <!-- Active Vouchers list -->
          <div v-if="vouchers.length > 0" class="space-y-3 pt-4 border-t border-outline-variant/10">
            <p class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Voucher của bạn:</p>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div 
                v-for="v in vouchers" 
                :key="v.id"
                @click="selectVoucher(v)"
                :class="store.selectedVoucher?.id === v.id ? 'border-primary bg-primary/5' : 'border-outline-variant/20 bg-surface-container-high/40'"
                class="border p-4 rounded-xl flex items-center justify-between cursor-pointer hover:border-primary/50 transition-colors"
              >
                <div>
                  <p class="font-mono font-bold text-sm text-primary uppercase">{{ v.promotion.code }}</p>
                  <p class="text-[10px] text-on-surface-variant mt-1">Giảm {{ v.promotion.discountType === 'PERCENTAGE' ? v.promotion.discountValue + '%' : v.promotion.discountValue.toLocaleString() + 'đ' }}</p>
                </div>
                <span v-if="store.selectedVoucher?.id === v.id" class="material-symbols-outlined text-primary">check_circle</span>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-16 mb-8 border-t border-outline-variant/10 pt-16">
          <h1 class="font-headline text-3xl font-bold tracking-tight mb-2 uppercase italic text-primary-container">04. Phương Thức Thanh Toán</h1>
          <p class="text-sm text-on-surface-variant">Chọn phương thức thanh toán phù hợp nhất</p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
            <label class="glass-card p-4 rounded-xl flex items-center gap-4 cursor-pointer hover:border-primary-container transition-colors" :class="{'border-primary-container': paymentMethod === 'VNPAY'}">
                <input type="radio" value="VNPAY" v-model="paymentMethod" class="w-4 h-4 text-primary-container focus:ring-primary-container border-outline-variant/30 bg-transparent">
                <span class="font-bold">Thanh toán qua VNPAY</span>
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
              <img :src="store.selectedMovie?.posterUrl || '/images/Hopper.webp'" class="w-full h-full object-cover rounded-lg"/>
            </div>
            <div class="flex flex-col justify-center">
              <span class="bg-error-container text-[9px] font-bold uppercase tracking-widest px-2 py-0.5 w-fit mb-2 text-white">
                {{ store.selectedMovie?.ageRating || 'T18' }}
              </span>
              <h2 class="font-headline text-lg font-bold leading-tight uppercase tracking-tight mb-1">
                {{ store.selectedMovie?.title || 'Phim đã chọn' }}
              </h2>
              <p class="text-xs text-on-surface-variant font-label">
                {{ store.selectedShowtime?.cinema?.name }} • {{ store.selectedShowtime?.room?.name }}
              </p>
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
            <div class="bg-black/40 border border-white/5 p-5 rounded-xl space-y-2">
              <div class="flex justify-between items-center text-xs text-on-surface-variant">
                <span>Tạm tính (suất + bắp):</span>
                <span>{{ store.totalPrice.toLocaleString('vi-VN') }}đ</span>
              </div>
              <div v-if="discountAmount > 0" class="flex justify-between items-center text-xs text-green-400">
                <span>Khuyến mãi (voucher):</span>
                <span>-{{ discountAmount.toLocaleString('vi-VN') }}đ</span>
              </div>
              <div class="flex justify-between items-center border-t border-outline-variant/10 pt-2 mb-1">
                <span class="text-xs font-bold uppercase tracking-widest text-on-surface">Tổng tiền</span>
                <span class="text-2xl font-headline font-extrabold text-primary-container">{{ finalPaymentPrice.toLocaleString('vi-VN') }} VNĐ</span>
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
