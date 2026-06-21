<script setup>
import { RouterLink, useRouter } from 'vue-router'
import { useBookingStore } from '@/stores/booking'
import { paymentApi, voucherApi } from '@/api/customer'
import { settingsApi } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import { computed, onMounted, ref, watch } from 'vue'

const store = useBookingStore()
const router = useRouter()
const authStore = useAuthStore()

const paymentMethod = ref('VNPAY')

// ===== Điều hướng wizard từng bước =====
const currentStep = ref(1)
const steps = [
  { id: 1, label: 'Chọn ghế', icon: 'event_seat' },
  { id: 2, label: 'Combo', icon: 'fastfood' },
  { id: 3, label: 'Ưu đãi', icon: 'local_activity' },
  { id: 4, label: 'Thanh toán', icon: 'payments' },
]

// Tiêu đề + mô tả của từng bước (render ở header chung phía trên)
const stepMeta = computed(() => ({
  1: { title: '01. Chọn Chỗ Ngồi', desc: '' },
  2: { title: '02. Combo - Đồ Ăn & Nước Uống', desc: 'Chọn combo bắp nước & đồ ăn kèm cho buổi xem phim (không bắt buộc)' },
  3: { title: '03. Ưu Đãi / Mã Giảm Giá', desc: 'Chọn voucher sẵn có hoặc nhập mã giảm giá' },
  4: { title: '04. Phương Thức Thanh Toán', desc: 'Chọn phương thức thanh toán phù hợp nhất' },
}[currentStep.value]))

// Chỉ cho rời bước 1 khi đã chọn ít nhất 1 ghế; các bước sau không bắt buộc
const canProceed = computed(() => {
  if (currentStep.value === 1) return store.selectedSeats.length > 0
  return true
})

const scrollTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

const goNext = () => {
  if (currentStep.value < steps.length && canProceed.value) {
    currentStep.value++
    scrollTop()
  }
}
const goBack = () => {
  if (currentStep.value > 1) {
    currentStep.value--
    scrollTop()
  }
}
const goToStep = (id) => {
  // Cho phép quay lại bất kỳ bước trước; chỉ chặn nhảy tới khi chưa chọn ghế
  if (id > currentStep.value && store.selectedSeats.length === 0) return
  currentStep.value = id
  scrollTop()
}

// ===== Phân trang danh sách combo / F&B (6 món = 2 cột x 3 hàng / trang) =====
const fnbPage = ref(1)
const fnbPageSize = 6
const fnbTotalPages = computed(() => Math.max(1, Math.ceil(store.availableFnbs.length / fnbPageSize)))
const pagedFnbs = computed(() => {
  const start = (fnbPage.value - 1) * fnbPageSize
  return store.availableFnbs.slice(start, start + fnbPageSize)
})
// Kẹp lại số trang nếu danh sách thay đổi (vd sau khi tải xong)
watch(fnbTotalPages, (total) => { if (fnbPage.value > total) fnbPage.value = total })
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

// ===== Thanh toán chuyển khoản (VietQR tự sinh — giống POS) =====
const bankInfo = ref({ code: '', name: '', accountNo: '', accountName: '' })

const removeDiacritics = (s) => String(s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D')

const transferContent = computed(() => {
  const seats = store.selectedSeats.map(s => s.rowChar + s.colNum).join('')
  return removeDiacritics(`DevCine ve ${seats}`).slice(0, 50)
})

const crc16 = (str) => {
  let crc = 0xFFFF
  for (let i = 0; i < str.length; i++) {
    crc ^= str.charCodeAt(i) << 8
    for (let j = 0; j < 8; j++) {
      crc = (crc & 0x8000) ? ((crc << 1) ^ 0x1021) : (crc << 1)
      crc &= 0xFFFF
    }
  }
  return crc.toString(16).toUpperCase().padStart(4, '0')
}

const buildVietQrPayload = () => {
  const b = bankInfo.value
  if (!b.code || !b.accountNo) return ''
  const tlv = (id, val) => id + String(val.length).padStart(2, '0') + val
  const acquirer = tlv('00', b.code) + tlv('01', b.accountNo)
  const f38 = tlv('38', tlv('00', 'A000000727') + tlv('01', acquirer) + tlv('02', 'QRIBFTTA'))
  const amount = Math.round(finalPaymentPrice.value || 0)
  const f54 = amount > 0 ? tlv('54', String(amount)) : ''
  const f62 = transferContent.value ? tlv('62', tlv('08', transferContent.value)) : ''
  const partial = tlv('00', '01') + tlv('01', '11') + f38 + tlv('53', '704') + f54 + tlv('58', 'VN') + f62 + '6304'
  return partial + crc16(partial)
}

const transferQrUrl = computed(() => {
  const payload = buildVietQrPayload()
  if (!payload) return ''
  return `https://api.qrserver.com/v1/create-qr-code/?size=420x420&margin=0&ecc=M&data=${encodeURIComponent(payload)}`
})

const loadBankInfo = async () => {
  try {
    const { data } = await settingsApi.getAll()
    const map = {}
    data.forEach(s => { map[s.settingKey] = s.settingValue })
    bankInfo.value = {
      code: map.PAYMENT_BANK_CODE || '',
      name: map.PAYMENT_BANK_NAME || '',
      accountNo: map.PAYMENT_ACCOUNT_NO || '',
      accountName: map.PAYMENT_ACCOUNT_NAME || ''
    }
  } catch (err) {
    // Không chặn luồng đặt vé nếu lỗi — phần QR sẽ báo chưa cấu hình
  }
}

onMounted(async () => {
  if (!store.selectedShowtime) {
    // If accessed directly without a showtime, redirect back
    router.push('/lich-chieu')
    return
  }
  await store.fetchSeats()
  await store.fetchFnbs()
  loadBankInfo()

  if (authStore.isAuthenticated && authStore.user?.id) {
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

// Giá Người lớn theo loại ghế (cho phần chú thích) — lấy động từ bảng giá của suất
const seatTypeAdultPrice = (typeName) => {
  const v = store.priceTable?.[typeName]?.ADULT
  return v != null ? Number(v).toLocaleString('vi-VN') + ' VNĐ' : '—'
}
const onChangeTicketType = (seatId, e) => store.setSeatTicketType(seatId, e.target.value)

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
    const reason = store.lastHoldError || ''
    if (/already taken|on hold/i.test(reason)) {
      alert('Một số ghế bạn chọn vừa được người khác đặt. Vui lòng tải lại sơ đồ ghế và chọn ghế khác.')
    } else {
      alert(reason || 'Giữ ghế thất bại. Vui lòng thử lại.')
    }
    // Làm mới sơ đồ ghế để cập nhật trạng thái mới nhất
    await store.fetchSeats()
  }
}

</script>

<template>
  <main class="pt-32 pb-20 max-w-[1440px] mx-auto px-10">
    <!-- Stepper / Thanh tiến trình các bước -->
    <div class="mb-12">
      <div class="flex items-center justify-between max-w-3xl mx-auto">
        <template v-for="(s, idx) in steps" :key="s.id">
          <button
            type="button"
            @click="goToStep(s.id)"
            class="flex flex-col items-center gap-2 group flex-shrink-0"
          >
            <div
              :class="currentStep === s.id
                ? 'bg-primary text-on-primary border-primary shadow-[0_0_20px_rgba(245,197,24,0.4)] scale-110'
                : currentStep > s.id
                  ? 'bg-primary/20 text-primary border-primary/40'
                  : 'bg-surface-container-high text-on-surface-variant border-outline-variant/20'"
              class="w-12 h-12 rounded-2xl border-2 flex items-center justify-center transition-all duration-300"
            >
              <span v-if="currentStep > s.id" class="material-symbols-outlined">check</span>
              <span v-else class="material-symbols-outlined">{{ s.icon }}</span>
            </div>
            <span
              :class="currentStep === s.id ? 'text-primary' : 'text-on-surface-variant'"
              class="text-[10px] font-bold uppercase tracking-widest transition-colors"
            >
              {{ s.id }}. {{ s.label }}
            </span>
          </button>
          <div
            v-if="idx < steps.length - 1"
            :class="currentStep > s.id ? 'bg-primary/50' : 'bg-outline-variant/20'"
            class="flex-grow h-0.5 mx-2 -mt-6 transition-colors duration-300"
          ></div>
        </template>
      </div>
    </div>

    <div class="flex flex-col lg:flex-row gap-12">
    <!-- Main Content Area -->
    <div class="flex-grow min-w-0">
      <!-- Header chung của bước hiện tại (trong cột trái để card tóm tắt căn ngang title/mô tả) -->
      <div class="mb-10 flex items-start justify-between gap-4">
        <div>
          <h1 class="font-headline text-3xl font-bold tracking-tight mb-2 uppercase italic text-primary-container">{{ stepMeta.title }}</h1>
          <div class="flex items-center gap-4 text-on-surface-variant" v-if="currentStep === 1 && store.selectedShowtime">
            <span class="flex items-center gap-1"><span class="material-symbols-outlined text-sm">location_on</span> {{ store.selectedShowtime.cinema?.cinemaName }}</span>
            <span class="w-1 h-1 rounded-full bg-outline-variant"></span>
            <span class="flex items-center gap-1"><span class="material-symbols-outlined text-sm">calendar_today</span> {{ new Date(store.selectedShowtime.startTime).toLocaleDateString() }}</span>
            <span class="w-1 h-1 rounded-full bg-outline-variant"></span>
            <span class="flex items-center gap-1"><span class="material-symbols-outlined text-sm">schedule</span> {{ new Date(store.selectedShowtime.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }}</span>
          </div>
          <p v-else class="text-sm text-on-surface-variant">{{ stepMeta.desc }}</p>
        </div>
        <!-- Nút Quay lại đặt ở góc phải tiêu đề -->
        <button
          v-if="currentStep > 1"
          @click="goBack"
          class="flex-shrink-0 px-5 py-2.5 rounded-xl border border-outline-variant/30 text-on-surface-variant font-bold text-xs uppercase tracking-widest hover:border-primary/40 hover:text-primary transition-all flex items-center gap-2"
        >
          <span class="material-symbols-outlined text-lg">arrow_back</span> Quay lại
        </button>
      </div>

      <!-- Section 1: Seat Selection -->
      <section v-show="currentStep === 1">
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
                <span class="text-[10px] text-on-surface-variant">{{ seatTypeAdultPrice('NORMAL') }}</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-lg bg-gradient-to-b from-red-700/90 to-red-900/90 border border-red-500/50 shadow-[0_4px_6px_rgba(0,0,0,0.3),inset_0_1px_1px_rgba(255,255,255,0.1),0_0_15px_rgba(220,38,38,0.2)]"></div>
              <div class="flex flex-col">
                <span class="text-[10px] font-bold uppercase tracking-wider text-red-400">VIP</span>
                <span class="text-[10px] text-on-surface-variant">{{ seatTypeAdultPrice('VIP') }}</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-12 h-8 rounded-t-xl rounded-b-md bg-gradient-to-b from-purple-600/90 to-purple-900/90 border border-purple-500/50 shadow-[0_4px_6px_rgba(0,0,0,0.3),inset_0_1px_1px_rgba(255,255,255,0.1),0_0_15px_rgba(147,51,234,0.2)]"></div>
              <div class="flex flex-col">
                <span class="text-[10px] font-bold uppercase tracking-wider text-purple-400">Sweetbox</span>
                <span class="text-[10px] text-on-surface-variant">{{ seatTypeAdultPrice('SWEETBOX') }}</span>
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

          <!-- Chọn loại vé / đối tượng cho từng ghế -->
          <div v-if="store.selectedSeats.length > 0" class="border-t border-outline-variant/10 pt-8 mt-10">
            <h3 class="font-headline font-bold uppercase tracking-tight text-sm mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary-container text-lg">groups</span>
              Loại vé theo ghế
            </h3>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div v-for="seat in store.selectedSeats" :key="seat.seatId"
                   class="glass-card rounded-xl p-4 flex items-center justify-between gap-3">
                <div class="flex flex-col">
                  <span class="font-bold text-primary-container">{{ seat.rowChar }}{{ seat.colNum }}</span>
                  <span class="text-[10px] uppercase tracking-wider text-on-surface-variant">{{ seat.seatType }}</span>
                </div>
                <div class="flex flex-col items-end gap-1">
                  <select :value="seat.ticketType || 'ADULT'" @change="onChangeTicketType(seat.seatId, $event)"
                          class="bg-surface-container-high border border-outline-variant/20 text-on-surface text-xs font-bold px-3 py-2 rounded-lg outline-none focus:border-primary-container">
                    <option v-for="(label, code) in store.audienceLabels" :key="code" :value="code">{{ label }}</option>
                  </select>
                  <span class="text-xs font-bold text-on-surface">{{ Number(seat.price).toLocaleString('vi-VN') }} VNĐ</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
      
      <!-- Section 2: Combo / F&B Selection -->
      <section v-show="currentStep === 2">
        <!-- Empty state khi rạp chưa có combo -->
        <div v-if="store.availableFnbs.length === 0" class="glass-card p-10 rounded-2xl text-center">
          <span class="material-symbols-outlined text-4xl text-on-surface-variant/40 mb-2">fastfood</span>
          <p class="text-sm text-on-surface-variant">Hiện chưa có combo nào. Bạn có thể tiếp tục đặt vé.</p>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="glass-card p-6 flex gap-6 hover:border-primary-container/30 transition-all group rounded-2xl" v-for="fnb in pagedFnbs" :key="fnb.id">
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

        <!-- Phân trang combo / F&B -->
        <div v-if="store.availableFnbs.length > 0 && fnbTotalPages > 1" class="flex items-center justify-center gap-2 mt-8">
          <button
            @click="fnbPage > 1 && (fnbPage--)"
            :disabled="fnbPage === 1"
            class="w-10 h-10 flex items-center justify-center rounded-xl border border-outline-variant/20 text-on-surface-variant hover:border-primary/40 hover:text-primary transition-all disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <span class="material-symbols-outlined text-lg">chevron_left</span>
          </button>
          <button
            v-for="p in fnbTotalPages"
            :key="p"
            @click="fnbPage = p"
            :class="fnbPage === p ? 'bg-primary text-on-primary border-primary' : 'border-outline-variant/20 text-on-surface-variant hover:border-primary/40'"
            class="w-10 h-10 flex items-center justify-center rounded-xl border text-sm font-bold transition-all"
          >
            {{ p }}
          </button>
          <button
            @click="fnbPage < fnbTotalPages && (fnbPage++)"
            :disabled="fnbPage === fnbTotalPages"
            class="w-10 h-10 flex items-center justify-center rounded-xl border border-outline-variant/20 text-on-surface-variant hover:border-primary/40 hover:text-primary transition-all disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <span class="material-symbols-outlined text-lg">chevron_right</span>
          </button>
        </div>
      </section>

      <!-- Section 3: Voucher / Khuyến mãi -->
      <section v-show="currentStep === 3">
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

      </section>

      <!-- Section 4: Payment -->
      <section v-show="currentStep === 4">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
            <label class="glass-card p-4 rounded-xl flex items-center gap-4 cursor-pointer hover:border-primary-container transition-colors" :class="{'border-primary-container': paymentMethod === 'VNPAY'}">
                <input type="radio" value="VNPAY" v-model="paymentMethod" class="w-4 h-4 text-primary-container focus:ring-primary-container border-outline-variant/30 bg-transparent">
                <span class="material-symbols-outlined text-primary-container">credit_card</span>
                <span class="font-bold">Thanh toán qua VNPAY</span>
            </label>
            <label class="glass-card p-4 rounded-xl flex items-center gap-4 cursor-pointer hover:border-primary-container transition-colors" :class="{'border-primary-container': paymentMethod === 'TRANSFER'}">
                <input type="radio" value="TRANSFER" v-model="paymentMethod" class="w-4 h-4 text-primary-container focus:ring-primary-container border-outline-variant/30 bg-transparent">
                <span class="material-symbols-outlined text-primary-container">qr_code_2</span>
                <span class="font-bold">Thanh toán bằng chuyển khoản</span>
            </label>
        </div>

        <!-- Khối QR chuyển khoản (VietQR tự sinh, giống POS) -->
        <div v-if="paymentMethod === 'TRANSFER'" class="glass-card p-8 rounded-2xl">
          <div v-if="transferQrUrl" class="flex flex-col md:flex-row gap-8 items-center">
            <div class="w-56 h-56 bg-white rounded-2xl p-3 flex-shrink-0">
              <img :src="transferQrUrl" alt="VietQR chuyển khoản" class="w-full h-full object-contain" />
            </div>
            <div class="flex-grow space-y-3 w-full">
              <h3 class="font-headline font-bold text-lg uppercase tracking-tight text-primary-container">Quét mã để chuyển khoản</h3>
              <p class="text-xs text-on-surface-variant">Dùng app ngân hàng/ví quét mã VietQR — số tiền & nội dung đã điền sẵn.</p>
              <div class="space-y-2 pt-2 border-t border-outline-variant/10">
                <div class="flex justify-between text-sm"><span class="text-on-surface-variant">Ngân hàng</span><span class="font-bold">{{ bankInfo.name || bankInfo.code }}</span></div>
                <div class="flex justify-between text-sm"><span class="text-on-surface-variant">Số tài khoản</span><span class="font-bold font-mono">{{ bankInfo.accountNo }}</span></div>
                <div class="flex justify-between text-sm"><span class="text-on-surface-variant">Chủ tài khoản</span><span class="font-bold uppercase">{{ bankInfo.accountName }}</span></div>
                <div class="flex justify-between text-sm"><span class="text-on-surface-variant">Số tiền</span><span class="font-bold text-primary-container">{{ finalPaymentPrice.toLocaleString('vi-VN') }} VNĐ</span></div>
                <div class="flex justify-between text-sm"><span class="text-on-surface-variant">Nội dung</span><span class="font-bold font-mono text-xs">{{ transferContent }}</span></div>
              </div>
              <p class="text-[11px] text-on-surface-variant/70 italic pt-2">Sau khi chuyển khoản, bấm "Xác nhận thanh toán" để hoàn tất đặt vé.</p>
            </div>
          </div>
          <div v-else class="text-center py-8">
            <span class="material-symbols-outlined text-4xl text-on-surface-variant/40 mb-2">account_balance</span>
            <p class="text-sm text-on-surface-variant">Rạp chưa cấu hình tài khoản nhận chuyển khoản. Vui lòng chọn VNPAY.</p>
          </div>
        </div>
      </section>
    </div>

    <!-- Persistent Sidebar Summary -->
    <aside class="w-full lg:w-[400px] lg:self-start">
      <div class="glass-card glass-shine-edge shadow-2xl rounded-3xl">
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
          <!-- Action Button (theo bước hiện tại) -->
          <button
            v-if="currentStep < steps.length"
            @click="goNext"
            :disabled="!canProceed"
            class="group w-full bg-gradient-to-r from-primary to-amber-500 text-black py-4 rounded-2xl font-headline font-extrabold text-sm tracking-[0.12em] uppercase shadow-[0_8px_24px_-6px_rgba(245,197,24,0.5)] hover:shadow-[0_10px_30px_-4px_rgba(245,197,24,0.65)] hover:brightness-105 active:scale-[0.98] transition-all flex items-center justify-center gap-2.5 disabled:grayscale disabled:opacity-50 disabled:shadow-none disabled:cursor-not-allowed"
          >
            Tiếp tục
            <span class="material-symbols-outlined text-xl group-hover:translate-x-1 transition-transform">arrow_forward</span>
          </button>
          <button
            v-else
            @click="proceedToPayment"
            :disabled="store.selectedSeats.length === 0"
            class="group w-full bg-gradient-to-r from-primary to-amber-500 text-black py-4 rounded-2xl font-headline font-extrabold text-sm tracking-[0.12em] uppercase shadow-[0_8px_24px_-6px_rgba(245,197,24,0.5)] hover:shadow-[0_10px_30px_-4px_rgba(245,197,24,0.65)] hover:brightness-105 active:scale-[0.98] transition-all flex items-center justify-center gap-2.5 disabled:grayscale disabled:opacity-50 disabled:shadow-none disabled:cursor-not-allowed"
          >
            <span class="material-symbols-outlined text-xl">lock</span>
            Xác nhận thanh toán
          </button>
        </div>
      </div>
    </aside>
    </div>
  </main>
</template>

<style scoped>
.seat-grid { perspective: 1000px; }
.screen-curve { box-shadow: 0 -20px 50px -10px rgba(245, 197, 24, 0.3); }
</style>
