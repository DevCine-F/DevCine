<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ticketingApi } from '@/api/admin/index'
import AppButton from '../../components/common/AppButton.vue'

const currentStep = ref(1) // 1: Showtime, 2: Seats, 3: Confirm, 4: F&B, 5: Payment, 6: Done

const showtimes = ref([])
const combos = ref([])
const isLoading = ref(false)

const selectedShowtime = ref(null)
const seatData = ref({ matrixRow: 9, matrixCol: 10, seats: [] })
const isLoadingSeats = ref(false)
const selectedSeats = ref([]) // seat objects from map
const selectedCombos = ref([]) // { id, name, price, quantity }

const member = ref(null)
const cardNumberInput = ref('')
const isCheckingCard = ref(false)
const cardError = ref('')

const paymentMethod = ref('CASH')
const isPaying = ref(false)
const completedBooking = ref(null)

const error = ref('')

// Toast
const toast = ref({ show: false, type: 'success', message: '' })
let toastTimer = null
const showToast = (message, type = 'success') => {
  toast.value = { show: true, type, message }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value.show = false }, 3000)
}

const seatTypeLabel = (t) => ({ NORMAL: 'Thường', STANDARD: 'Thường', VIP: 'VIP', SWEETBOX: 'Sweetbox' }[t] || t)

const fetchData = async () => {
  isLoading.value = true
  error.value = ''
  try {
    const [stRes, cbRes] = await Promise.all([
      ticketingApi.getShowtimes(),
      ticketingApi.getCombos()
    ])
    showtimes.value = stRes.data.data ?? stRes.data
    combos.value = cbRes.data.data ?? cbRes.data
  } catch (err) {
    error.value = 'Không tải được dữ liệu bán vé. Kiểm tra đăng nhập/quyền.'
  } finally {
    isLoading.value = false
  }
}

const selectShowtime = async (st) => {
  selectedShowtime.value = st
  selectedSeats.value = []
  isLoadingSeats.value = true
  currentStep.value = 2
  try {
    const { data } = await ticketingApi.getSeats(st.id)
    seatData.value = data.seats ? data : { matrixRow: 9, matrixCol: 10, seats: Array.isArray(data) ? data : [] }
  } catch (err) {
    showToast('Không tải được sơ đồ ghế.', 'error')
    seatData.value = { matrixRow: 9, matrixCol: 10, seats: [] }
  } finally {
    isLoadingSeats.value = false
  }
}

const seatAt = (row, col) => seatData.value.seats.find(s => s.gridRow === row && s.gridCol === col)
const isSelected = (seat) => selectedSeats.value.some(s => s.seatId === seat.seatId)
// Nhãn hàng (A, B, C...) suy từ ghế đầu tiên có trên hàng đó
const rowLabel = (gridRow) => {
  const s = seatData.value.seats.find(x => x.gridRow === gridRow)
  return s ? s.rowChar : ''
}

const toggleSeat = (seat) => {
  if (!seat || seat.status !== 'AVAILABLE') return
  const idx = selectedSeats.value.findIndex(s => s.seatId === seat.seatId)
  if (idx > -1) selectedSeats.value.splice(idx, 1)
  else selectedSeats.value.push(seat)
}

const seatClass = (seat) => {
  const base = 'w-8 h-8 rounded-lg flex items-center justify-center text-[9px] font-bold border transition-all leading-none'
  if (!seat) return ''
  if (seat.status === 'SOLD') return `${base} bg-surface-container-high border-white/5 text-on-surface-variant/20 cursor-not-allowed opacity-40`
  if (seat.status === 'HOLD') return `${base} bg-yellow-500/10 border-yellow-500/30 text-yellow-500/60 cursor-not-allowed`
  if (isSelected(seat)) return `${base} bg-primary border-primary text-on-primary shadow-lg shadow-primary/30 cursor-pointer scale-105`
  const byType = {
    VIP: 'bg-red-900/40 border-red-500/40 text-red-200 hover:border-red-400',
    SWEETBOX: 'bg-purple-900/40 border-purple-500/40 text-purple-200 hover:border-purple-400'
  }[seat.seatType] || 'bg-surface-container-high border-outline-variant/10 text-on-surface-variant/60 hover:border-primary/40'
  return `${base} ${byType} cursor-pointer`
}

// F&B
const addCombo = (cb) => {
  const existing = selectedCombos.value.find(c => c.id === cb.id)
  if (existing) existing.quantity++
  else selectedCombos.value.push({ id: cb.id, name: cb.name, price: Number(cb.price), quantity: 1 })
}
const changeComboQty = (item, delta) => {
  item.quantity += delta
  if (item.quantity <= 0) {
    const idx = selectedCombos.value.findIndex(c => c.id === item.id)
    if (idx > -1) selectedCombos.value.splice(idx, 1)
  }
}

const seatTotal = computed(() => selectedSeats.value.reduce((a, s) => a + Number(s.price || 0), 0))
const comboTotal = computed(() => selectedCombos.value.reduce((a, c) => a + c.price * c.quantity, 0))
const totalPrice = computed(() => seatTotal.value + comboTotal.value)

const seatTypeBreakdown = computed(() => {
  const map = {}
  for (const s of selectedSeats.value) {
    const key = s.seatType
    if (!map[key]) map[key] = { type: key, count: 0, subtotal: 0 }
    map[key].count++
    map[key].subtotal += Number(s.price || 0)
  }
  return Object.values(map)
})

const fmt = (n) => Number(n || 0).toLocaleString('vi-VN')

const checkMemberCard = async () => {
  cardError.value = ''
  if (!cardNumberInput.value.trim()) return
  isCheckingCard.value = true
  try {
    const { data } = await ticketingApi.memberCard(cardNumberInput.value.trim())
    member.value = data.data ?? data
  } catch (err) {
    cardError.value = err.response?.data?.error || 'Không tìm thấy thẻ thành viên.'
    member.value = null
  } finally {
    isCheckingCard.value = false
  }
}
const clearMember = () => { member.value = null; cardNumberInput.value = ''; cardError.value = '' }

const processPayment = async (method) => {
  if (selectedSeats.value.length === 0) {
    showToast('Chưa chọn ghế.', 'error')
    return
  }
  paymentMethod.value = method
  isPaying.value = true
  try {
    const payload = {
      showtimeId: selectedShowtime.value.id,
      seatIds: selectedSeats.value.map(s => s.seatId),
      fnbs: selectedCombos.value.map(c => ({ fnbItemId: c.id, quantity: c.quantity })),
      customerId: member.value ? member.value.customerId : null,
      paymentMethod: method
    }
    const { data } = await ticketingApi.pay(payload)
    if (data.success) {
      completedBooking.value = data
      currentStep.value = 6
    } else {
      showToast(data.message || 'Thanh toán thất bại.', 'error')
    }
  } catch (err) {
    showToast(err.response?.data?.message || 'Thanh toán thất bại (ghế có thể đã bán).', 'error')
  } finally {
    isPaying.value = false
  }
}

const resetPOS = () => {
  currentStep.value = 1
  selectedShowtime.value = null
  seatData.value = { matrixRow: 9, matrixCol: 10, seats: [] }
  selectedSeats.value = []
  selectedCombos.value = []
  member.value = null
  cardNumberInput.value = ''
  cardError.value = ''
  completedBooking.value = null
  fetchData()
}

onMounted(fetchData)
onUnmounted(() => { if (toastTimer) clearTimeout(toastTimer) })
</script>

<template>
  <div class="h-full flex flex-col p-6 space-y-5 bg-surface-container-lowest">
    <!-- Header -->
    <header class="flex justify-between items-center bg-surface px-5 py-3 rounded-2xl border border-outline-variant/10 shadow-xl">
      <div class="flex items-center gap-4">
        <div class="w-9 h-9 bg-primary rounded-xl flex items-center justify-center text-on-primary shadow-lg shadow-primary/20">
          <span class="material-symbols-outlined text-xl">point_of_sale</span>
        </div>
        <div>
          <h1 class="text-lg font-black tracking-tighter uppercase italic text-on-surface leading-none">Ticketing <span class="text-primary">POS</span></h1>
          <p class="text-[9px] font-bold text-on-surface-variant uppercase tracking-widest mt-0.5">Hệ thống bán vé & F&B v2.1</p>
        </div>
      </div>

      <div class="flex items-center gap-1.5">
        <div v-for="i in 6" :key="i" class="flex items-center gap-1.5">
          <div :class="currentStep >= i ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant/40'"
               class="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black transition-all">{{ i }}</div>
          <div v-if="i < 6" class="w-6 h-0.5 bg-outline-variant/20"></div>
        </div>
      </div>

      <AppButton variant="outline" @click="resetPOS">Hủy giao dịch</AppButton>
    </header>

    <main class="flex-grow grid grid-cols-12 gap-5 overflow-hidden">
      <div class="col-span-9 bg-surface border border-outline-variant/10 rounded-3xl shadow-2xl overflow-hidden flex flex-col">

        <!-- Step 1: Showtime -->
        <div v-if="currentStep === 1" class="p-6 space-y-8 overflow-y-auto custom-scrollbar">
          <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
            <span class="w-8 h-1 bg-primary rounded-full"></span> 1. Chọn phim & suất chiếu
          </h2>

          <div v-if="isLoading" class="grid grid-cols-2 gap-6">
            <div v-for="i in 4" :key="i" class="h-44 bg-surface-container-high rounded-3xl animate-pulse"></div>
          </div>
          <div v-else-if="error" class="p-6 bg-red-500/10 border border-red-500/20 rounded-2xl text-red-400 text-sm">{{ error }}</div>
          <div v-else-if="showtimes.length === 0" class="py-20 text-center border border-dashed border-outline-variant/20 rounded-3xl">
            <span class="material-symbols-outlined text-5xl text-on-surface-variant/40 mb-3">event_busy</span>
            <p class="text-on-surface-variant font-semibold">Không có suất chiếu nào hôm nay/sắp tới.</p>
            <p class="text-xs text-on-surface-variant/60 mt-1">Tạo suất chiếu ở "Lịch chiếu & Điều phối".</p>
          </div>

          <div v-else class="grid grid-cols-2 gap-6">
            <div v-for="st in showtimes" :key="st.id" @click="selectShowtime(st)"
                 class="p-6 bg-surface-container-high rounded-3xl border border-outline-variant/10 hover:border-primary/50 hover:bg-primary/5 transition-all cursor-pointer group">
              <div class="flex gap-6">
                <div class="w-24 h-36 bg-surface-container-highest rounded-xl overflow-hidden shadow-lg border border-outline-variant/10">
                  <img :src="st.moviePoster || '/images/Hopper.webp'" class="w-full h-full object-cover group-hover:scale-105 transition-transform" />
                </div>
                <div class="flex flex-col justify-between py-1">
                  <div>
                    <h3 class="font-black text-lg uppercase tracking-tight text-on-surface group-hover:text-primary transition-colors">{{ st.movieTitle }}</h3>
                    <p class="text-[10px] font-bold text-on-surface-variant uppercase mt-1">{{ st.formatName }} • {{ st.roomName }}</p>
                    <p class="text-[10px] font-bold text-on-surface-variant/70 mt-1">{{ new Date(st.startTime).toLocaleDateString('vi-VN') }}</p>
                  </div>
                  <span class="px-4 py-2 bg-primary/10 text-primary text-sm font-black italic rounded-xl border border-primary/20 w-fit mt-3">
                    {{ new Date(st.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 2: Seats -->
        <div v-if="currentStep === 2" class="p-6 flex flex-col h-full overflow-hidden">
          <div class="flex justify-between items-center mb-3">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 2. Chọn ghế ({{ selectedSeats.length }})
            </h2>
            <AppButton variant="ghost" @click="currentStep = 1">Quay lại</AppButton>
          </div>
          <div class="w-full flex flex-col items-center gap-1.5 mb-3 shrink-0">
            <div class="w-2/3 h-1.5 bg-gradient-to-r from-transparent via-primary/40 to-transparent rounded-full blur-[2px]"></div>
            <p class="text-[10px] font-black uppercase tracking-[0.3em] text-primary/60">Màn hình</p>
          </div>

          <div v-if="isLoadingSeats" class="flex-grow flex items-center justify-center">
            <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
          </div>
          <div v-else-if="seatData.seats.length === 0" class="flex-grow flex items-center justify-center text-on-surface-variant">
            Phòng chưa có sơ đồ ghế.
          </div>
          <div v-else class="flex-grow min-h-0 flex items-center justify-center overflow-auto custom-scrollbar">
            <div class="flex flex-col gap-1.5 mx-auto my-auto">
              <div v-for="row in seatData.matrixRow" :key="row" class="flex gap-1.5 items-center justify-center">
                <span class="w-5 text-[10px] font-bold text-on-surface-variant/50 text-center shrink-0">{{ rowLabel(row - 1) }}</span>
                <template v-for="col in seatData.matrixCol" :key="col">
                  <div v-if="seatAt(row - 1, col - 1)" :class="seatClass(seatAt(row - 1, col - 1))"
                       @click="toggleSeat(seatAt(row - 1, col - 1))" :title="seatAt(row - 1, col - 1).rowChar + seatAt(row - 1, col - 1).colNum">
                    {{ seatAt(row - 1, col - 1).rowChar }}{{ seatAt(row - 1, col - 1).colNum }}
                  </div>
                  <div v-else class="w-8 h-8"></div>
                </template>
                <span class="w-5 text-[10px] font-bold text-on-surface-variant/50 text-center shrink-0">{{ rowLabel(row - 1) }}</span>
              </div>
            </div>
          </div>

          <div class="mt-3 flex items-center justify-between shrink-0">
            <div class="flex gap-4 text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-surface-container-high border border-outline-variant/20"></span>Thường</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-red-900/40 border border-red-500/40"></span>VIP</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-purple-900/40 border border-purple-500/40"></span>Sweetbox</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-surface-container-high opacity-40"></span>Đã bán</span>
            </div>
            <AppButton @click="currentStep = 3" :disabled="selectedSeats.length === 0">3. Xác nhận vé</AppButton>
          </div>
        </div>

        <!-- Step 3: Confirm ticket types (by seat type) -->
        <div v-if="currentStep === 3" class="p-6 flex flex-col h-full overflow-hidden">
          <div class="flex justify-between items-center mb-8">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 3. Xác nhận vé & loại ghế
            </h2>
            <AppButton variant="ghost" @click="currentStep = 2">Quay lại</AppButton>
          </div>

          <div class="flex-grow overflow-y-auto custom-scrollbar space-y-4 pr-2">
            <div v-for="b in seatTypeBreakdown" :key="b.type"
                 class="p-6 bg-surface-container-high rounded-[28px] border border-outline-variant/10 flex items-center justify-between">
              <div class="flex items-center gap-4">
                <div class="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary border border-primary/20">
                  <span class="material-symbols-outlined">event_seat</span>
                </div>
                <div>
                  <p class="text-sm font-black text-on-surface uppercase">Ghế {{ seatTypeLabel(b.type) }}</p>
                  <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">{{ b.count }} ghế</p>
                </div>
              </div>
              <p class="text-lg font-black italic text-primary">{{ fmt(b.subtotal) }}đ</p>
            </div>
            <div class="px-2 pt-2 text-xs font-bold text-on-surface-variant">
              Ghế đã chọn: <span class="text-primary">{{ selectedSeats.map(s => s.rowChar + s.colNum).join(', ') }}</span>
            </div>
          </div>

          <div class="mt-6 flex justify-end">
            <AppButton @click="currentStep = 4">4. Combo / Đồ ăn</AppButton>
          </div>
        </div>

        <!-- Step 4: F&B -->
        <div v-if="currentStep === 4" class="p-6 flex flex-col h-full overflow-hidden">
          <div class="flex justify-between items-center mb-8">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 4. Combo / Đồ ăn & Nước uống
            </h2>
            <AppButton variant="ghost" @click="currentStep = 3">Quay lại</AppButton>
          </div>

          <div v-if="combos.length === 0" class="flex-grow flex items-center justify-center text-on-surface-variant text-sm">
            Chưa có combo. Thêm ở "Thực đơn F&B / Combo".
          </div>
          <div v-else class="grid grid-cols-2 gap-6 flex-grow overflow-y-auto custom-scrollbar pr-2">
            <div v-for="cb in combos" :key="cb.id" class="p-5 bg-surface-container-high rounded-3xl border border-outline-variant/10 flex gap-4 hover:border-primary/30 transition-all">
              <div class="w-16 h-16 rounded-2xl overflow-hidden bg-surface-container-highest shrink-0 flex items-center justify-center">
                <img v-if="cb.imageUrl" :src="cb.imageUrl" class="w-full h-full object-cover" />
                <span v-else class="material-symbols-outlined text-on-surface-variant/40">fastfood</span>
              </div>
              <div class="flex flex-col justify-between flex-grow min-w-0">
                <div>
                  <h3 class="text-sm font-black uppercase text-on-surface truncate">{{ cb.name }}</h3>
                  <p class="text-[11px] text-on-surface-variant line-clamp-1">{{ cb.description }}</p>
                </div>
                <div class="flex items-center justify-between mt-2">
                  <span class="text-base font-black text-primary italic">{{ fmt(cb.price) }}đ</span>
                  <button @click="addCombo(cb)" class="w-8 h-8 rounded-full bg-primary/10 text-primary border border-primary/20 flex items-center justify-center hover:bg-primary/20 transition-colors">
                    <span class="material-symbols-outlined text-sm">add</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="mt-4 rounded-2xl border border-primary/20 bg-primary/5 flex items-center gap-3 pl-4 pr-3 py-2.5">
            <!-- Label -->
            <div class="flex items-center gap-2 shrink-0">
              <span class="material-symbols-outlined text-primary text-lg">shopping_bag</span>
              <h3 class="text-[10px] font-black uppercase tracking-widest text-primary leading-none">Giỏ F&B</h3>
              <span class="text-[10px] font-bold text-on-surface-variant whitespace-nowrap">· {{ selectedCombos.length }} món</span>
            </div>

            <!-- Items (cuộn ngang nếu nhiều) -->
            <div class="flex-grow min-w-0 flex items-center gap-2 overflow-x-auto custom-scrollbar">
              <p v-if="selectedCombos.length === 0" class="text-[11px] text-on-surface-variant/60 whitespace-nowrap">Chưa chọn combo nào — không bắt buộc.</p>
              <div v-for="item in selectedCombos" :key="item.id"
                   class="shrink-0 flex items-center gap-2 bg-surface-container-high rounded-full border border-outline-variant/10 pl-3 pr-1 py-1">
                <span class="text-[11px] font-bold text-on-surface whitespace-nowrap">{{ item.name }}</span>
                <span class="text-[11px] font-black text-primary whitespace-nowrap">{{ fmt(item.price * item.quantity) }}đ</span>
                <div class="flex items-center bg-surface-container-lowest rounded-full">
                  <button @click="changeComboQty(item, -1)" class="w-6 h-6 flex items-center justify-center hover:text-primary transition-colors"><span class="material-symbols-outlined text-[15px]">remove</span></button>
                  <span class="w-5 text-center text-[11px] font-black">{{ item.quantity }}</span>
                  <button @click="changeComboQty(item, 1)" class="w-6 h-6 flex items-center justify-center hover:text-primary transition-colors"><span class="material-symbols-outlined text-[15px]">add</span></button>
                </div>
              </div>
            </div>

            <!-- Subtotal + action -->
            <div class="shrink-0 flex items-center gap-3 pl-3 border-l border-primary/15">
              <div class="text-right hidden sm:block">
                <p class="text-[9px] font-bold uppercase tracking-wider text-on-surface-variant leading-none mb-0.5">Tạm tính</p>
                <p class="text-base font-black italic text-primary tracking-tighter leading-none">{{ fmt(comboTotal) }}đ</p>
              </div>
              <AppButton @click="currentStep = 5">5. Thanh toán</AppButton>
            </div>
          </div>
        </div>

        <!-- Step 5: Payment -->
        <div v-if="currentStep === 5" class="p-6 space-y-8 overflow-y-auto custom-scrollbar h-full">
          <div class="flex justify-between items-center">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 5. Thanh toán
            </h2>
            <AppButton variant="ghost" @click="currentStep = 4">Quay lại</AppButton>
          </div>
          <div class="grid grid-cols-2 gap-8">
            <div class="bg-surface-container-high p-8 rounded-3xl border border-outline-variant/10 space-y-5">
              <p class="text-[10px] font-black text-primary uppercase tracking-widest">Chi tiết hóa đơn</p>
              <h3 class="text-xl font-black italic uppercase text-on-surface">{{ selectedShowtime?.movieTitle }}</h3>
              <div class="flex justify-between text-xs font-bold text-on-surface-variant uppercase border-b border-outline-variant/10 pb-3">
                <span>Vé (x{{ selectedSeats.length }})</span>
                <span class="text-on-surface">{{ fmt(seatTotal) }}đ</span>
              </div>
              <div v-if="comboTotal > 0" class="flex justify-between text-xs font-bold text-on-surface-variant uppercase border-b border-outline-variant/10 pb-3">
                <span>F&B / Combo</span>
                <span class="text-on-surface">{{ fmt(comboTotal) }}đ</span>
              </div>
              <div class="pt-3 flex justify-between items-end">
                <p class="text-[10px] font-black text-on-surface-variant uppercase">Tổng cộng</p>
                <p class="text-4xl font-black italic text-primary tracking-tighter">{{ fmt(totalPrice) }}đ</p>
              </div>
            </div>

            <div class="space-y-6">
              <div class="bg-primary/5 border border-primary/20 p-8 rounded-3xl space-y-4">
                <p class="text-[10px] font-black text-primary uppercase tracking-widest">Thành viên (tùy chọn — để tích điểm)</p>
                <div v-if="!member" class="space-y-3">
                  <input v-model="cardNumberInput" placeholder="Số thẻ (ID khách hàng)..." class="w-full bg-surface-container-high border border-outline-variant/10 rounded-2xl py-3 px-5 text-on-surface text-sm font-bold outline-none focus:border-primary/50" />
                  <p v-if="cardError" class="text-xs text-red-400 font-bold">{{ cardError }}</p>
                  <AppButton variant="primary" class="w-full" @click="checkMemberCard" :disabled="isCheckingCard">{{ isCheckingCard ? 'Đang kiểm tra...' : 'Kiểm tra' }}</AppButton>
                </div>
                <div v-else class="space-y-2 text-on-surface">
                  <div class="flex justify-between items-center">
                    <p class="text-sm font-black uppercase">{{ member.fullName }}</p>
                    <span class="px-2 py-1 bg-primary text-on-primary text-[8px] font-black rounded uppercase">{{ member.membershipTier }}</span>
                  </div>
                  <p class="text-xs text-on-surface-variant">Điểm tích lũy: <span class="text-primary font-bold">{{ fmt(member.loyaltyPoints) }}</span></p>
                  <button @click="clearMember" class="text-[10px] text-on-surface-variant hover:text-red-400 font-bold uppercase">Bỏ thẻ</button>
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="processPayment('CASH')" :disabled="isPaying">
                  <span class="material-symbols-outlined">payments</span> Tiền mặt
                </AppButton>
                <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="processPayment('CARD')" :disabled="isPaying">
                  <span class="material-symbols-outlined">qr_code_2</span> Thẻ / QR
                </AppButton>
              </div>
              <p v-if="isPaying" class="text-center text-xs text-on-surface-variant">Đang xử lý thanh toán...</p>
            </div>
          </div>
        </div>

        <!-- Step 6: Done -->
        <div v-if="currentStep === 6" class="p-6 flex flex-col items-center justify-center text-center h-full space-y-8">
          <div class="w-24 h-24 bg-green-500/20 text-green-500 rounded-full flex items-center justify-center shadow-2xl shadow-green-500/20">
            <span class="material-symbols-outlined text-6xl">check_circle</span>
          </div>
          <div>
            <h2 class="text-4xl font-black uppercase italic tracking-tighter text-on-surface">Thanh toán thành công</h2>
            <p class="text-on-surface-variant font-bold mt-2 uppercase tracking-widest text-xs">Xuất vé và bàn giao cho khách</p>
          </div>

          <div class="bg-surface-container-high p-8 rounded-3xl border border-outline-variant/10 w-full max-w-md space-y-6 text-left">
            <div class="flex justify-between items-start">
              <div>
                <p class="text-[10px] font-black text-primary uppercase">Mã đặt vé</p>
                <p class="text-xl font-black text-on-surface">{{ completedBooking?.bookingCode }}</p>
              </div>
              <div class="text-right">
                <p class="text-[10px] font-black text-primary uppercase">Phòng</p>
                <p class="text-xl font-black text-on-surface">{{ selectedShowtime?.roomName }}</p>
              </div>
            </div>
            <div class="border-t border-dashed border-outline-variant/20 pt-6">
              <p class="text-[10px] font-black text-on-surface-variant uppercase mb-2">Thông tin vé</p>
              <div class="flex justify-between text-sm font-bold text-on-surface">
                <span>{{ selectedSeats.length }} ghế: {{ selectedSeats.map(s => s.rowChar + s.colNum).join(', ') }}</span>
                <span class="text-primary italic">{{ fmt(totalPrice) }}đ</span>
              </div>
            </div>
          </div>

          <div class="flex gap-4">
            <AppButton variant="primary" size="lg" class="flex items-center gap-3" @click="resetPOS">
              <span class="material-symbols-outlined">print</span> Giao dịch mới
            </AppButton>
          </div>
        </div>
      </div>

      <!-- Right: Cart summary -->
      <div class="col-span-3 bg-surface-container-low border border-outline-variant/10 rounded-3xl shadow-2xl p-6 flex flex-col">
        <div class="flex items-center gap-2 pb-5 mb-5 border-b border-outline-variant/10">
          <span class="material-symbols-outlined text-primary">receipt_long</span>
          <h2 class="text-sm font-black uppercase tracking-[0.2em] text-primary">Biên lai tạm tính</h2>
        </div>
        <div v-if="selectedShowtime" class="space-y-5 flex-grow overflow-y-auto custom-scrollbar pr-1">
          <div class="pb-5 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-1.5">Phim & Suất</p>
            <h3 class="text-base font-black uppercase italic text-on-surface leading-tight">{{ selectedShowtime.movieTitle }}</h3>
            <p class="text-xs text-on-surface-variant mt-1.5">{{ selectedShowtime.roomName }} • {{ selectedShowtime.formatName }}</p>
            <p class="text-xs text-on-surface-variant">{{ new Date(selectedShowtime.startTime).toLocaleString('vi-VN', { weekday: 'short', hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }) }}</p>
          </div>
          <div v-if="selectedSeats.length" class="pb-5 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-2">Ghế ({{ selectedSeats.length }})</p>
            <p class="text-sm text-primary font-black mb-3">{{ selectedSeats.map(s => s.rowChar + s.colNum).join(', ') }}</p>
            <div v-for="b in seatTypeBreakdown" :key="b.type" class="flex justify-between text-xs font-semibold text-on-surface-variant mb-1">
              <span>Ghế {{ seatTypeLabel(b.type) }} <span class="text-on-surface-variant/60">x{{ b.count }}</span></span>
              <span class="text-on-surface">{{ fmt(b.subtotal) }}đ</span>
            </div>
          </div>
          <div v-if="selectedCombos.length" class="pb-5 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-2">F&B / Combo</p>
            <div v-for="c in selectedCombos" :key="c.id" class="flex justify-between text-xs font-semibold mb-1">
              <span class="text-on-surface-variant">{{ c.name }} <span class="text-on-surface-variant/60">x{{ c.quantity }}</span></span>
              <span class="text-on-surface">{{ fmt(c.price * c.quantity) }}đ</span>
            </div>
          </div>
        </div>
        <div v-else class="flex-grow flex flex-col items-center justify-center text-on-surface-variant/40 gap-2">
          <span class="material-symbols-outlined text-4xl">shopping_cart</span>
          <p class="text-xs font-semibold">Chưa chọn suất chiếu</p>
        </div>

        <div class="pt-5 mt-3 border-t border-outline-variant/10 flex justify-between items-center">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Tổng tiền</p>
          <p class="text-3xl font-black italic tracking-tighter text-primary">{{ fmt(totalPrice) }}đ</p>
        </div>
      </div>
    </main>

    <!-- Toast -->
    <transition name="fade">
      <div v-if="toast.show" :class="[
        'fixed bottom-6 right-6 z-[1100] px-5 py-3 rounded-xl shadow-2xl text-sm font-semibold flex items-center gap-2 border',
        toast.type === 'success' ? 'bg-green-500/15 border-green-500/30 text-green-300' : 'bg-red-500/15 border-red-500/30 text-red-300'
      ]">
        <span class="material-symbols-outlined text-base">{{ toast.type === 'success' ? 'check_circle' : 'error' }}</span>
        {{ toast.message }}
      </div>
    </transition>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; height: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(245, 197, 24, 0.2); border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: rgba(245, 197, 24, 0.4); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(10px); }
</style>
