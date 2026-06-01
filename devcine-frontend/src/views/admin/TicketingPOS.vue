<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import AppButton from '../../components/common/AppButton.vue'

const API_BASE_URL = 'http://localhost:8080/api/ticketing'

const currentStep = ref(1) // 1: Movie, 2: Seats, 3: Types, 4: F&B, 5: Payment, 6: Completion
const showtimes = ref([])
const combos = ref([])
const selectedShowtime = ref(null)
const selectedSeats = ref([]) // { id: 'A1', type: 'adult' }
const selectedCombos = ref([]) // { combo, quantity, size }
const memberCard = ref(null)
const cardNumberInput = ref('')

const totalQuantity = computed(() => selectedSeats.value.length)

const fetchData = async () => {
  try {
    const [stRes, cbRes] = await Promise.all([
      axios.get(`${API_BASE_URL}/showtimes`),
      axios.get(`${API_BASE_URL}/combos`)
    ])
    showtimes.value = stRes.data
    combos.value = cbRes.data
  } catch (error) {
    console.error('Error fetching POS data:', error)
  }
}

const selectShowtime = (st) => {
  selectedShowtime.value = st
  currentStep.value = 2
}

const toggleSeat = (seatId) => {
  const idx = selectedSeats.value.findIndex(s => s.id === seatId)
  if (idx > -1) selectedSeats.value.splice(idx, 1)
  else selectedSeats.value.push({ id: seatId, type: 'adult' }) // Default type
}

const addCombo = (combo) => {
  const existing = selectedCombos.value.find(c => c.id === combo.id)
  if (existing) existing.quantity++
  else selectedCombos.value.push({ ...combo, quantity: 1, isUpsized: false })
}

const removeCombo = (id) => {
  const idx = selectedCombos.value.findIndex(c => c.id === id)
  if (idx > -1) selectedCombos.value.splice(idx, 1)
}

const upsizeCombo = (item) => {
  item.isUpsized = true
  item.price += 15000 // Upsize fee
}

const totalPrice = computed(() => {
  const ticketPrices = { adult: 95000, child: 75000, student: 80000 }
  let ticketTotal = selectedSeats.value.reduce((acc, s) => acc + (ticketPrices[s.type] || 0), 0)
  let comboTotal = selectedCombos.value.reduce((acc, c) => acc + (c.price * c.quantity), 0)
  return ticketTotal + comboTotal
})

const ticketQuantities = computed(() => {
  return selectedSeats.value.reduce((acc, s) => {
    acc[s.type]++
    return acc
  }, { adult: 0, child: 0, student: 0 })
})

const checkMemberCard = async () => {
  try {
    const res = await axios.get(`${API_BASE_URL}/member-card/${cardNumberInput.value}`)
    if (res.data) {
      memberCard.value = res.data
    } else {
      alert('Không tìm thấy thẻ thành viên!')
    }
  } catch (error) {
    console.error('Error checking card:', error)
  }
}

const processPayment = async (method) => {
  try {
    const payload = {
      showtime: { id: selectedShowtime.value.id },
      selectedSeats: selectedSeats.value,
      totalPrice: totalPrice.value,
      paymentMethod: method,
      memberCard: memberCard.value
    }
    await axios.post(`${API_BASE_URL}/pay`, payload)
    currentStep.value = 6 // Move to completion step
  } catch (error) {
    console.error('Payment error:', error)
    alert('Lỗi thanh toán!')
  }
}

const resetPOS = () => {
  currentStep.value = 1
  selectedShowtime.value = null
  selectedSeats.value = []
  selectedCombos.value = []
  memberCard.value = null
  cardNumberInput.value = ''
}

const seedData = async () => {
  try {
    await axios.post(`http://localhost:8080/api/system/seed-all`);
    alert('Đã khởi tạo dữ liệu mẫu toàn hệ thống!');
    await fetchData();
  } catch (error) {
    console.error('Seeding error:', error);
    alert('Lỗi khi khởi tạo dữ liệu!');
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="h-full flex flex-col p-8 space-y-8 bg-surface-container-lowest">
    <!-- POS Header -->
    <header class="flex justify-between items-center bg-surface p-6 rounded-3xl border border-outline-variant/10 shadow-xl">
      <div class="flex items-center gap-6">
        <div class="w-12 h-12 bg-primary rounded-2xl flex items-center justify-center text-on-primary shadow-lg shadow-primary/20">
          <span class="material-symbols-outlined text-3xl">point_of_sale</span>
        </div>
        <div>
          <h1 class="text-2xl font-black tracking-tighter uppercase italic text-on-surface">Ticketing <span class="text-primary">POS</span></h1>
          <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Hệ thống bán vé & F&B v2.1</p>
        </div>
      </div>

      <!-- Stepper Indicator -->
      <div class="flex items-center gap-4">
        <div v-for="i in 6" :key="i" class="flex items-center gap-2">
          <div :class="currentStep >= i ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant/40'" 
               class="w-8 h-8 rounded-full flex items-center justify-center text-xs font-black transition-all">
            {{ i }}
          </div>
          <div v-if="i < 6" class="w-8 h-0.5 bg-outline-variant/20"></div>
        </div>
      </div>

      <div class="flex gap-4">
        <AppButton variant="ghost" @click="seedData">Dữ liệu Mẫu</AppButton>
        <AppButton variant="outline" @click="resetPOS">Hủy giao dịch</AppButton>
      </div>
    </header>

    <main class="flex-grow grid grid-cols-12 gap-8 overflow-hidden">
      <div class="col-span-8 bg-surface border border-outline-variant/10 rounded-[40px] shadow-2xl overflow-hidden flex flex-col">
        
        <div v-if="currentStep === 1" class="p-10 space-y-8 overflow-y-auto custom-scrollbar">
          <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
             <span class="w-8 h-1 bg-primary rounded-full"></span> 1. Chọn phim & suất chiếu
          </h2>
          
          <div class="grid grid-cols-2 gap-6">
            <div v-for="st in showtimes" :key="st.id" @click="selectShowtime(st)"
                 class="p-6 bg-surface-container-high rounded-3xl border border-outline-variant/10 hover:border-primary/50 hover:bg-primary/5 transition-all cursor-pointer group relative overflow-hidden">
              <div class="relative z-10 flex gap-6">
                <div class="w-24 h-36 bg-surface-container-highest rounded-xl overflow-hidden shadow-lg border border-outline-variant/10">
                  <img :src="st.movie.posterUrl || '/images/Hopper.webp'" class="w-full h-full object-cover group-hover:scale-105 transition-transform" />
                </div>
                <div class="flex flex-col justify-between py-1">
                  <div>
                    <h3 class="font-black text-lg uppercase tracking-tight text-on-surface group-hover:text-primary transition-colors">{{ st.movie.title }}</h3>
                    <p class="text-[10px] font-bold text-on-surface-variant uppercase mt-1">{{ st.format }} • {{ st.roomNumber }}</p>
                  </div>
                  <div class="flex items-center gap-3 mt-4">
                    <span class="px-4 py-2 bg-primary/10 text-primary text-sm font-black italic rounded-xl border border-primary/20">
                      {{ new Date(st.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 2: Seat Selection -->
        <div v-if="currentStep === 2" class="p-10 flex flex-col h-full overflow-hidden">
          <div class="flex justify-between items-center mb-10">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
               <span class="w-8 h-1 bg-primary rounded-full"></span> 2. Chọn vị trí ({{ selectedSeats.length }})
            </h2>
            <AppButton variant="ghost" @click="currentStep = 1">Quay lại</AppButton>
          </div>
          <div class="w-full flex flex-col items-center gap-4 mb-12">
            <div class="w-2/3 h-1.5 bg-gradient-to-r from-transparent via-primary/40 to-transparent rounded-full blur-[2px]"></div>
            <p class="text-[10px] font-black uppercase tracking-[0.3em] text-primary/60">Màn hình</p>
          </div>
          <div class="flex-grow flex items-center justify-center overflow-auto custom-scrollbar p-8">
            <div class="grid grid-cols-10 gap-3">
              <div v-for="row in ['A','B','C','D','E']" :key="row" class="contents">
                <div v-for="col in 10" :key="col" 
                     @click="toggleSeat(row + col)"
                     :class="[
                       'w-10 h-10 rounded-xl flex items-center justify-center text-[10px] font-black border transition-all cursor-pointer',
                       selectedSeats.find(s => s.id === (row + col)) ? 'bg-primary border-primary text-on-primary shadow-lg shadow-primary/30' : 'bg-surface-container-high border-outline-variant/10 text-on-surface-variant/40 hover:border-primary/40'
                     ]">
                  {{ row }}{{ col }}
                </div>
              </div>
            </div>
          </div>
          <div class="mt-8 flex justify-end">
             <AppButton @click="currentStep = 3" :disabled="selectedSeats.length === 0">3. Phân loại vé</AppButton>
          </div>
        </div>

        <!-- Step 3: Ticket Type Assignment -->
        <div v-if="currentStep === 3" class="p-10 flex flex-col h-full overflow-hidden">
          <div class="flex justify-between items-center mb-8">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
               <span class="w-8 h-1 bg-primary rounded-full"></span> 3. Phân loại vé
            </h2>
            <AppButton variant="ghost" @click="currentStep = 2">Quay lại</AppButton>
          </div>
          
          <div class="flex-grow overflow-y-auto custom-scrollbar space-y-4 pr-4">
            <div v-for="seat in selectedSeats" :key="seat.id" 
                 class="p-6 bg-surface-container-high rounded-[32px] border border-outline-variant/10 flex items-center justify-between animate-in slide-in-from-right duration-300">
              <div class="flex items-center gap-4">
                <div class="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary font-black italic border border-primary/20">
                  {{ seat.id }}
                </div>
                <div>
                  <p class="text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Ghế đã chọn</p>
                  <p class="text-xs font-bold text-on-surface">Vị trí {{ seat.id }}</p>
                </div>
              </div>
              
              <div class="flex bg-surface-container-lowest rounded-2xl p-1 border border-outline-variant/10">
                <button v-for="(label, type) in { adult: 'Người lớn', child: 'Trẻ em', student: 'Học sinh' }" :key="type"
                        @click="seat.type = type"
                        :class="[
                          'px-6 py-2 rounded-xl text-[10px] font-black uppercase tracking-tight transition-all',
                          seat.type === type ? 'bg-primary text-on-primary shadow-lg shadow-primary/20' : 'text-on-surface-variant/40 hover:text-on-surface'
                        ]">
                  {{ label }}
                </button>
              </div>
            </div>
          </div>

          <div class="mt-8 flex justify-end">
            <AppButton @click="currentStep = 4">4. Dịch vụ đi kèm</AppButton>
          </div>
        </div>

        <!-- Step 4: F&B / Upselling -->
        <div v-if="currentStep === 4" class="p-10 flex flex-col h-full overflow-hidden">
           <div class="flex justify-between items-center mb-8">
              <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
                 <span class="w-8 h-1 bg-primary rounded-full"></span> 4. Dịch vụ đi kèm (Upselling)
              </h2>
              <AppButton variant="ghost" @click="currentStep = 3">Quay lại</AppButton>
           </div>

           <div class="grid grid-cols-2 gap-8 flex-grow overflow-y-auto custom-scrollbar pr-4">
              <div v-for="cb in combos" :key="cb.id" class="p-6 bg-surface-container-high rounded-3xl border border-outline-variant/10 flex flex-col justify-between hover:border-primary/30 transition-all">
                 <div>
                    <h3 class="text-lg font-black uppercase text-on-surface">{{ cb.name }}</h3>
                    <p class="text-xs text-on-surface-variant mt-2">{{ cb.description }}</p>
                    <p class="text-xl font-black text-primary mt-4 italic">{{ cb.price.toLocaleString() }}đ</p>
                 </div>
                 <AppButton variant="outline" class="mt-6" @click="addCombo(cb)">Thêm vào đơn</AppButton>
              </div>
           </div>

           <div class="mt-8 bg-primary/5 p-6 rounded-3xl border border-primary/20">
              <h3 class="text-[10px] font-black uppercase tracking-widest text-primary mb-4">Giỏ hàng F&B</h3>
              <div class="space-y-3">
                 <div v-for="item in selectedCombos" :key="item.id" class="flex justify-between items-center">
                    <div class="flex items-center gap-4">
                       <span class="text-xs font-bold text-on-surface">{{ item.name }} (x{{ item.quantity }})</span>
                       <button v-if="!item.isUpsized" @click="upsizeCombo(item)" class="text-[9px] font-black text-primary uppercase border border-primary/30 px-2 py-0.5 rounded hover:bg-primary/10 transition-all">+ Upsize (+15k)</button>
                       <span v-else class="text-[9px] font-black text-green-500 uppercase">Đã Upsize</span>
                    </div>
                    <button @click="removeCombo(item.id)" class="text-red-500 hover:text-red-400 transition-colors">
                       <span class="material-symbols-outlined text-sm">delete</span>
                    </button>
                 </div>
              </div>
              <div class="mt-6 flex justify-end">
                 <AppButton @click="currentStep = 5">5. Thanh toán</AppButton>
              </div>
           </div>
        </div>

        <!-- Step 5: Thanh toán -->
        <div v-if="currentStep === 5" class="p-10 space-y-10 overflow-y-auto custom-scrollbar h-full">
           <div class="flex justify-between items-center">
              <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
                 <span class="w-8 h-1 bg-primary rounded-full"></span> 5. Thanh toán
              </h2>
              <AppButton variant="ghost" @click="currentStep = 4">Quay lại</AppButton>
           </div>
           <div class="grid grid-cols-2 gap-10">
              <div class="bg-surface-container-high p-8 rounded-[40px] border border-outline-variant/10 space-y-6">
                 <p class="text-[10px] font-black text-primary uppercase tracking-widest">Chi tiết hóa đơn</p>
                 <div class="space-y-4">
                    <h3 class="text-2xl font-black italic uppercase text-on-surface">{{ selectedShowtime.movie.title }}</h3>
                    <div class="flex justify-between text-xs font-bold text-on-surface-variant uppercase tracking-wider border-b border-outline-variant/10 pb-4">
                       <span>Vé (x{{ selectedSeats.length }}):</span>
                       <span class="text-on-surface">{{ selectedSeats.map(s => s.id).join(', ') }}</span>
                    </div>
                    <div v-for="c in selectedCombos" :key="c.id" class="flex justify-between text-xs font-bold text-on-surface-variant uppercase tracking-wider border-b border-outline-variant/10 pb-4">
                       <span>{{ c.name }} (x{{ c.quantity }}):</span>
                       <span class="text-on-surface">{{ (c.price * c.quantity).toLocaleString() }}đ</span>
                    </div>
                 </div>
                 <div class="pt-6 flex justify-between items-end">
                    <p class="text-[10px] font-black text-on-surface-variant uppercase">Tổng cộng:</p>
                    <p class="text-4xl font-black italic text-primary tracking-tighter">{{ totalPrice.toLocaleString() }}đ</p>
                 </div>
              </div>
              <div class="space-y-8">
                 <div class="bg-primary/5 border border-primary/20 p-8 rounded-[40px] space-y-6">
                    <p class="text-[10px] font-black text-primary uppercase tracking-widest">Thành viên / Voucher</p>
                    <div v-if="!memberCard" class="space-y-4">
                       <input v-model="cardNumberInput" placeholder="Số thẻ thành viên..." class="w-full bg-surface-container-high border border-outline-variant/10 rounded-2xl py-4 px-6 text-on-surface text-sm font-bold outline-none focus:border-primary/50" />
                       <AppButton variant="primary" class="w-full" @click="checkMemberCard">Kiểm tra</AppButton>
                    </div>
                    <div v-else class="space-y-4 text-on-surface">
                       <div class="flex justify-between items-center">
                          <p class="text-xs font-black uppercase">{{ memberCard.ownerName }}</p>
                          <span class="px-2 py-1 bg-primary text-black text-[8px] font-black rounded">{{ memberCard.rank }}</span>
                       </div>
                       <p class="text-2xl font-black italic text-primary">{{ memberCard.balance.toLocaleString() }}đ</p>
                       <AppButton variant="primary" class="w-full" @click="processPayment('MemberCard')" :disabled="memberCard.balance < totalPrice">Thanh toán bằng thẻ</AppButton>
                    </div>
                 </div>
                 <div class="grid grid-cols-2 gap-4">
                    <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="processPayment('Cash')">
                       <span class="material-symbols-outlined">payments</span>
                       Tiền mặt
                    </AppButton>
                    <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="processPayment('CreditCard')">
                       <span class="material-symbols-outlined">qr_code_2</span>
                       Thẻ / QR
                    </AppButton>
                 </div>
              </div>
           </div>
        </div>

        <!-- Step 6: Xuất vé & Hóa đơn -->
        <div v-if="currentStep === 6" class="p-10 flex flex-col items-center justify-center text-center h-full space-y-8 animate-in zoom-in duration-500">
           <div class="w-24 h-24 bg-green-500/20 text-green-500 rounded-full flex items-center justify-center shadow-2xl shadow-green-500/20">
              <span class="material-symbols-outlined text-6xl">check_circle</span>
           </div>
           <div>
              <h2 class="text-4xl font-black uppercase italic tracking-tighter text-on-surface">Thanh toán thành công</h2>
              <p class="text-on-surface-variant font-bold mt-2 uppercase tracking-widest text-xs">Vui lòng xuất vé và bàn giao cho khách hàng</p>
           </div>
           
           <div class="bg-surface-container-high p-8 rounded-[40px] border border-outline-variant/10 w-full max-w-md space-y-6 text-left">
              <div class="flex justify-between items-start">
                 <div>
                    <p class="text-[10px] font-black text-primary uppercase">Mã hóa đơn</p>
                    <p class="text-xl font-black text-on-surface">#DC-{{ Math.floor(Math.random()*1000000) }}</p>
                 </div>
                 <div class="text-right">
                    <p class="text-[10px] font-black text-primary uppercase">Phòng chiếu</p>
                    <p class="text-xl font-black text-on-surface">{{ selectedShowtime.roomNumber }}</p>
                 </div>
              </div>
              <div class="border-t border-dashed border-outline-variant/20 pt-6">
                 <p class="text-[10px] font-black text-on-surface-variant uppercase mb-2">Thông tin vé</p>
                 <div class="flex justify-between text-sm font-bold text-on-surface">
                    <span>{{ selectedSeats.length }} Ghế: {{ selectedSeats.map(s => s.id).join(', ') }}</span>
                    <span class="text-primary italic">{{ totalPrice.toLocaleString() }}đ</span>
                 </div>
              </div>
           </div>

           <div class="flex gap-4">
              <AppButton variant="primary" size="lg" class="flex items-center gap-3" @click="resetPOS">
                 <span class="material-symbols-outlined">print</span>
                 Xuất vé & Hóa đơn
              </AppButton>
              <AppButton variant="outline" size="lg" @click="resetPOS">Giao dịch mới</AppButton>
           </div>
        </div>
      </div>

      <!-- Right Section: Cart Summary -->
      <div class="col-span-4 bg-surface-container-low border border-outline-variant/10 rounded-[40px] shadow-2xl p-10 flex flex-col justify-between">
        <div class="space-y-10">
          <div class="text-center">
            <h2 class="text-xs font-black uppercase tracking-[0.3em] text-primary">Biên lai tạm tính</h2>
          </div>
          <div v-if="selectedShowtime" class="space-y-6">
             <div class="border-b border-outline-variant/10 pb-6">
                <p class="text-[10px] font-black text-on-surface-variant uppercase mb-2">Phim & Ghế</p>
                <h3 class="text-xs font-bold text-on-surface">{{ selectedShowtime.movie.title }}</h3>
                <p class="text-xs text-primary font-black mt-1">{{ selectedSeats.map(s => s.id).join(', ') }}</p>
                
                <div class="mt-4 space-y-1">
                   <div v-if="ticketQuantities.adult" class="flex justify-between text-[10px] font-bold text-on-surface-variant">
                      <span>Người lớn x{{ ticketQuantities.adult }}</span>
                      <span>{{ (ticketQuantities.adult * 95000).toLocaleString() }}đ</span>
                   </div>
                   <div v-if="ticketQuantities.child" class="flex justify-between text-[10px] font-bold text-on-surface-variant">
                      <span>Trẻ em x{{ ticketQuantities.child }}</span>
                      <span>{{ (ticketQuantities.child * 75000).toLocaleString() }}đ</span>
                   </div>
                   <div v-if="ticketQuantities.student" class="flex justify-between text-[10px] font-bold text-on-surface-variant">
                      <span>Sinh viên x{{ ticketQuantities.student }}</span>
                      <span>{{ (ticketQuantities.student * 80000).toLocaleString() }}đ</span>
                   </div>
                </div>
             </div>
             <div v-if="selectedCombos.length > 0" class="border-b border-outline-variant/10 pb-6">
                <p class="text-[10px] font-black text-on-surface-variant uppercase mb-2">F&B / Combo</p>
                <div v-for="c in selectedCombos" :key="c.id" class="flex justify-between items-center text-xs">
                   <span class="text-on-surface-variant font-bold">{{ c.name }} x{{ c.quantity }}</span>
                   <span class="text-on-surface font-black">{{ (c.price * c.quantity).toLocaleString() }}đ</span>
                </div>
             </div>
             <div class="pt-6 flex justify-between items-end">
                <p class="text-[10px] font-black text-primary uppercase">Tổng tiền</p>
                <p class="text-3xl font-black italic tracking-tighter text-on-surface">{{ totalPrice.toLocaleString() }}đ</p>
             </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(245, 197, 24, 0.2);
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(245, 197, 24, 0.4);
}
</style>
