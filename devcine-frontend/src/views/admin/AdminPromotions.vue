<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import CustomSelect from '@/components/common/CustomSelect.vue'

const filterStatus = ref('all')
const statusOptions = [
  { value: 'all', label: 'Tất cả trạng thái' },
  { value: 'active', label: 'Đang hiển thị' },
  { value: 'inactive', label: 'Đang ẩn' }
]
const discountTypeOptions = [
  { value: 'PERCENTAGE', label: 'Phần trăm (%)' },
  { value: 'FIXED_AMOUNT', label: 'Tiền cố định' }
]

const activeTab = ref('vouchers')
const cinemasList = ref([])
const promotions = ref([])
const combos = ref([])

const API_BASE_URL = 'http://localhost:8080/api/marketing'

const isVoucherDrawerOpen = ref(false)
const isComboDrawerOpen = ref(false)
const isArticleDrawerOpen = ref(false)

const articles = ref([
  {
    id: 1,
    title: 'COMBO HÈ RỰC RỠ',
    description: 'Giảm ngay 20% khi mua kèm 2 vé xem phim.',
    image: 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=500&auto=format&fit=crop',
    startDate: '2026-06-01',
    endDate: '2026-08-31',
    status: 'active'
  },
  {
    id: 2,
    title: 'ƯU ĐÃI HỌC SINH',
    description: 'Đồng giá vé chỉ 45K cho HSSV vào ngày thường.',
    image: 'https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?q=80&w=500&auto=format&fit=crop',
    startDate: '2026-05-01',
    endDate: '2026-12-31',
    status: 'active'
  },
  {
    id: 3,
    title: 'GIỜ VÀNG GIÁ VÉ',
    description: 'Mọi suất chiếu trước 12:00 sáng Thứ Hai đến Thứ Năm chỉ với 50.000 VNĐ.',
    image: 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?q=80&w=500&auto=format&fit=crop',
    startDate: '2026-01-01',
    endDate: '2026-12-31',
    status: 'inactive'
  }
])

const newArticle = ref({
  title: '',
  description: '',
  image: '',
  startDate: '',
  endDate: '',
  content: '',
  status: 'active'
})

const openArticleDrawer = () => {
  newArticle.value = { title: '', description: '', image: '', startDate: '', endDate: '', content: '', status: 'active' }
  isArticleDrawerOpen.value = true
}


const newVoucher = ref({
  code: '',
  type: 'PERCENTAGE',
  value: null,
  allowPointExchange: false,
  pointsRequired: null,
  title: '',
  description: '',
  expiry: '',
  cinemaMode: 'all',
  selectedCinemas: []
})

const newCombo = ref({
  name: '',
  price: null,
  description: '',
  image: '',
  status: 'active',
  items: [{ id: Date.now(), name: '', quantity: 1 }],
  cinemaMode: 'all',
  selectedCinemas: []
})

const addComboItem = () => {
  newCombo.value.items.push({ id: Date.now(), name: '', quantity: 1 })
}

const removeComboItem = (index) => {
  newCombo.value.items.splice(index, 1)
}

const openVoucherDrawer = () => {
  newVoucher.value = { code: '', type: 'PERCENTAGE', value: null, allowPointExchange: false, pointsRequired: null, title: '', description: '', expiry: '', cinemaMode: 'all', selectedCinemas: [] }
  isVoucherDrawerOpen.value = true
}

const openComboDrawer = () => {
  newCombo.value = { name: '', price: null, description: '', image: '', status: 'active', items: [{ id: Date.now(), name: '', quantity: 1 }], cinemaMode: 'all', selectedCinemas: [] }
  isComboDrawerOpen.value = true
}

const toggleStatus = (item) => {
  item.status = item.status === 'Active' || item.status === 'active' ? 'inactive' : 'active'
}

const fetchMarketingData = async () => {
  try {
    const [pRes, cRes] = await Promise.all([
      axios.get(`${API_BASE_URL}/promotions`),
      axios.get(`${API_BASE_URL}/combos`)
    ])
    
    // Map backend Promotion to frontend UI
    promotions.value = pRes.data.map(p => ({
      id: p.id,
      code: p.code,
      title: p.title, // Corrected from p.name
      description: p.description,
      type: p.type === 'VOUCHER' ? 'FIXED_AMOUNT' : 'PERCENTAGE',
      value: p.value, // Corrected from p.discountValue
      expiry: p.endDate,
      status: p.isActive ? 'active' : 'inactive',
      applicableCinemas: p.applicableScope || 'Toàn hệ thống'
    }))

    combos.value = cRes.data.map(c => ({
      id: c.id,
      name: c.name,
      price: c.price,
      description: c.description,
      items: Array.isArray(c.items) ? c.items : (c.items ? c.items.split(',') : []),
      status: c.active ? 'active' : 'inactive',
      image: '/images/Combo.webp' // Default image
    }))
  } catch (error) {
    console.error("Error fetching marketing data:", error)
  }
}

onMounted(async () => {
  fetchMarketingData()
  try {
    const response = await axios.get('http://localhost:8080/api/cinemas')
    cinemasList.value = response.data
  } catch (error) {
    console.error('Error fetching cinemas:', error)
    cinemasList.value = [
      { id: 1, name: 'DevCine Landmark 81' },
      { id: 2, name: 'DevCine Bitexco' }
    ]
  }
})
</script>

<template>
  <div class="p-10 space-y-8">
    <header class="flex justify-between items-end">
      <div>
        <h1 class="text-4xl font-extrabold tracking-tight font-headline uppercase italic text-primary">Marketing Hub</h1>
        <p class="text-on-surface-variant text-sm mt-1 uppercase tracking-widest font-bold">Quản lý chiến dịch, mã giảm giá và combo ưu đãi</p>
      </div>
      <div class="flex gap-4">
        <button v-if="activeTab === 'vouchers'" @click="openVoucherDrawer" class="bg-primary text-on-primary px-6 py-3 rounded-sm font-bold uppercase tracking-widest hover:scale-105 transition-transform flex items-center gap-2 text-xs">
          <span class="material-symbols-outlined text-sm">add_card</span>
          Tạo Voucher
        </button>
        <button v-if="activeTab === 'combos'" @click="openComboDrawer" class="bg-primary text-on-primary px-6 py-3 rounded-sm font-bold uppercase tracking-widest hover:scale-105 transition-transform flex items-center gap-2 text-xs">
          <span class="material-symbols-outlined text-sm">lunch_dining</span>
          Tạo Combo
        </button>
        <button v-if="activeTab === 'articles'" @click="openArticleDrawer" class="bg-primary text-on-primary px-6 py-3 rounded-sm font-bold uppercase tracking-widest hover:scale-105 transition-transform flex items-center gap-2 text-xs">
          <span class="material-symbols-outlined text-sm">post_add</span>
          Tạo Tin Khuyến Mãi
        </button>
      </div>
    </header>

    <!-- Tabs -->
    <div class="flex gap-8 border-b border-outline-variant/10">
      <button @click="activeTab = 'vouchers'" :class="activeTab === 'vouchers' ? 'text-primary border-primary' : 'text-on-surface-variant border-transparent'" class="pb-4 font-black text-xs uppercase tracking-[0.2em] border-b-2 transition-all">
        Mã Giảm Giá (Vouchers)
      </button>
      <button @click="activeTab = 'combos'" :class="activeTab === 'combos' ? 'text-primary border-primary' : 'text-on-surface-variant border-transparent'" class="pb-4 font-black text-xs uppercase tracking-[0.2em] border-b-2 transition-all">
        Chương trình Combo
      </button>
      <button @click="activeTab = 'articles'" :class="activeTab === 'articles' ? 'text-primary border-primary' : 'text-on-surface-variant border-transparent'" class="pb-4 font-black text-xs uppercase tracking-[0.2em] border-b-2 transition-all">
        Tin Khuyến Mãi
      </button>
    </div>

    <!-- Vouchers View -->
    <div v-if="activeTab === 'vouchers'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="promo in promotions" :key="promo.id" class="relative group">
        <div class="absolute -inset-0.5 bg-gradient-to-r from-primary/50 to-purple-500/50 rounded-2xl blur opacity-20 group-hover:opacity-40 transition duration-500"></div>
        <div class="relative bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden shadow-xl">
          <div class="p-6">
            <div class="flex justify-between items-start mb-6">
              <div class="px-4 py-2 bg-primary/10 rounded-xl border border-primary/20">
                <span class="font-black text-xl tracking-widest text-primary font-mono uppercase">{{ promo.code }}</span>
              </div>
              <button @click="toggleStatus(promo)" :class="promo.status === 'active' ? 'text-green-400' : 'text-on-surface-variant'" class="material-symbols-outlined text-3xl">
                {{ promo.status === 'active' ? 'toggle_on' : 'toggle_off' }}
              </button>
            </div>
            
            <h3 class="text-lg font-black text-on-surface mb-1 uppercase italic tracking-tight">{{ promo.title }}</h3>
            <p class="text-xs text-on-surface-variant mb-4 font-medium leading-relaxed">{{ promo.description }}</p>
            
            <div class="space-y-3 pt-4 border-t border-outline-variant/5">
              <div class="flex justify-between items-center">
                <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Mức giảm</span>
                <span class="text-sm font-black text-primary">{{ promo.type === 'PERCENTAGE' ? promo.value + '%' : promo.value.toLocaleString() + 'đ' }}</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Áp dụng</span>
                <span class="text-[10px] font-black text-on-surface uppercase">{{ promo.applicableCinemas }}</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Hết hạn</span>
                <span class="text-[10px] font-black text-red-400 uppercase italic">{{ promo.expiry }}</span>
              </div>
            </div>
          </div>
          <div class="bg-primary/5 p-4 flex justify-between items-center">
             <span class="text-[9px] font-black text-primary uppercase tracking-widest">Usage: 142/500</span>
             <button class="text-[9px] font-black text-on-surface-variant uppercase tracking-widest hover:text-white transition-colors">Edit Details</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Combos View -->
    <div v-if="activeTab === 'combos'" class="grid grid-cols-1 lg:grid-cols-2 gap-8">
       <div v-for="combo in combos" :key="combo.id" class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden flex shadow-xl hover:border-primary/30 transition-all group">
          <div class="w-48 bg-surface-container-high relative overflow-hidden">
             <img :src="combo.image" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" />
             <div class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
             <div class="absolute bottom-4 left-4">
                <span class="text-lg font-black text-primary">{{ combo.price.toLocaleString() }}đ</span>
             </div>
          </div>
          <div class="flex-1 p-6 flex flex-col justify-between">
             <div>
                <div class="flex justify-between items-start mb-2">
                   <h3 class="text-xl font-black text-on-surface uppercase italic">{{ combo.name }}</h3>
                   <span :class="combo.status === 'active' ? 'bg-green-500/10 text-green-500' : 'bg-on-surface-variant/10 text-on-surface-variant'" class="text-[9px] font-black px-2 py-1 rounded uppercase">{{ combo.status }}</span>
                </div>
                <div class="flex flex-wrap gap-2 mt-4">
                   <span v-for="item in combo.items" :key="item" class="px-3 py-1 bg-surface-container-highest border border-outline-variant/10 rounded-full text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">
                      {{ item }}
                   </span>
                </div>
             </div>
             
             <div class="flex justify-between items-center pt-6 border-t border-outline-variant/5">
                <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Áp dụng: Toàn hệ thống</span>
                <div class="flex gap-2">
                   <button class="p-2 hover:text-primary transition-colors"><span class="material-symbols-outlined text-lg">edit</span></button>
                   <button class="p-2 hover:text-red-500 transition-colors"><span class="material-symbols-outlined text-lg">delete</span></button>
                </div>
             </div>
          </div>
       </div>
    </div>

    <!-- Articles View -->
    <div v-if="activeTab === 'articles'" class="space-y-6">
      <!-- Search & Filter bar -->
      <div class="flex justify-between items-center bg-surface-container-low p-4 rounded-xl border border-outline-variant/10">
        <div class="relative w-80">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-xl">search</span>
          <input type="text" placeholder="Tìm kiếm tin khuyến mãi..." class="w-full bg-surface-container-highest border-none rounded-lg pl-10 pr-4 py-2 text-sm text-on-surface focus:ring-1 focus:ring-primary outline-none">
        </div>
        <div class="flex gap-4 w-48">
          <CustomSelect 
            v-model="filterStatus" 
            :options="statusOptions" 
            customClass="w-full px-4 py-2 rounded-lg text-sm border-none bg-surface-container-highest font-bold text-on-surface-variant" 
          />
        </div>
      </div>
      
      <!-- Table -->
      <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden shadow-xl">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-surface-container-highest/50 border-b border-outline-variant/10">
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant w-24">Hình ảnh</th>
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thông tin</th>
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thời gian áp dụng</th>
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant text-center">Trạng thái</th>
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="article in articles" :key="article.id" class="border-b border-outline-variant/5 hover:bg-white/5 transition-colors group">
              <td class="p-4">
                <div class="w-16 h-12 bg-surface-container-highest rounded overflow-hidden">
                  <img :src="article.image" class="w-full h-full object-cover" />
                </div>
              </td>
              <td class="p-4">
                <h4 class="text-sm font-black text-on-surface uppercase italic">{{ article.title }}</h4>
                <p class="text-xs text-on-surface-variant mt-1 line-clamp-1">{{ article.description }}</p>
              </td>
              <td class="p-4">
                <div class="flex flex-col gap-1">
                  <span class="text-xs text-on-surface font-mono">{{ article.startDate }}</span>
                  <span class="text-[10px] text-on-surface-variant font-mono">đến {{ article.endDate }}</span>
                </div>
              </td>
              <td class="p-4 text-center">
                <button @click="toggleStatus(article)" :class="article.status === 'active' ? 'text-green-400' : 'text-on-surface-variant'" class="material-symbols-outlined text-3xl transition-colors">
                  {{ article.status === 'active' ? 'toggle_on' : 'toggle_off' }}
                </button>
              </td>
              <td class="p-4 text-right">
                <div class="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button @click="openArticleDrawer" class="w-8 h-8 rounded-full bg-surface-container-highest hover:text-primary flex items-center justify-center transition-colors">
                    <span class="material-symbols-outlined text-sm">edit</span>
                  </button>
                  <button class="w-8 h-8 rounded-full bg-surface-container-highest hover:text-red-400 flex items-center justify-center transition-colors">
                    <span class="material-symbols-outlined text-sm">delete</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Voucher Drawer Form -->
    <div v-if="isVoucherDrawerOpen" class="fixed inset-0 z-[1000] flex justify-end">
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="isVoucherDrawerOpen = false"></div>
      
      <!-- Drawer Panel -->
      <div class="relative w-full max-w-md bg-surface-container-low h-full shadow-2xl flex flex-col border-l border-outline-variant/20 animate-in slide-in-from-right duration-300">
        <!-- Drawer Header -->
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <div>
            <h3 class="font-headline font-black uppercase italic text-primary text-xl">Tạo Voucher</h3>
            <p class="text-xs text-on-surface-variant mt-1 uppercase tracking-widest font-bold">Thêm mã giảm giá mới</p>
          </div>
          <button @click="isVoucherDrawerOpen = false" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        
        <!-- Drawer Body -->
        <div class="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-custom">
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mã Code (Tự tạo)</label>
            <input v-model="newVoucher.code" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none font-mono uppercase tracking-widest" placeholder="VD: SUMMER2026" />
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tiêu đề chiến dịch</label>
            <input v-model="newVoucher.title" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" placeholder="VD: Khuyến mãi hè rực rỡ" />
          </div>
          
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mô tả ngắn</label>
            <textarea v-model="newVoucher.description" rows="2" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-medium text-on-surface focus:border-primary outline-none resize-none" placeholder="Mô tả chi tiết voucher..."></textarea>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Loại giảm giá</label>
              <CustomSelect 
                v-model="newVoucher.type" 
                :options="discountTypeOptions" 
                customClass="w-full p-4 rounded-xl text-sm border-outline-variant/20" 
              />
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Giá trị giảm</label>
              <input v-model="newVoucher.value" type="number" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" />
            </div>
          </div>
          
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngày hết hạn</label>
            <input v-model="newVoucher.expiry" type="date" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" />
          </div>

          <div class="space-y-4 pt-4 border-t border-outline-variant/10">
            <div class="bg-surface-container-highest rounded-xl border border-outline-variant/10 overflow-hidden transition-all duration-300">
              <div class="flex items-center justify-between p-4">
                <div>
                  <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Áp dụng đổi bằng điểm</p>
                  <p class="text-[10px] text-on-surface-variant mt-1 font-bold">Nếu tắt, người dùng nhập trực tiếp mã (Code) để sử dụng</p>
                </div>
                <button @click="newVoucher.allowPointExchange = !newVoucher.allowPointExchange" :class="newVoucher.allowPointExchange ? 'bg-green-500' : 'bg-surface-container-high'" class="relative w-10 h-5 rounded-full transition-colors duration-300 focus:outline-none shrink-0">
                  <span :class="newVoucher.allowPointExchange ? 'translate-x-5 bg-white' : 'translate-x-0 bg-on-surface-variant'" class="inline-block w-4 h-4 transform rounded-full transition-transform duration-300 shadow-md absolute top-0.5 left-0.5"></span>
                </button>
              </div>
              
              <div v-if="newVoucher.allowPointExchange" class="p-4 pt-2 border-t border-outline-variant/5 animate-in fade-in slide-in-from-top-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-2 block">Số điểm cần đổi</label>
                <input v-model="newVoucher.pointsRequired" type="number" class="w-full bg-surface-container-lowest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" placeholder="VD: 50" />
              </div>
            </div>
          </div>

          <div class="space-y-4 pt-4 border-t border-outline-variant/10">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant block mb-3">Cụm rạp áp dụng</label>
            <div class="flex gap-8">
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="newVoucher.cinemaMode" value="all" class="accent-primary">
                <span class="text-xs font-bold uppercase">Toàn hệ thống</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="newVoucher.cinemaMode" value="specific" class="accent-primary">
                <span class="text-xs font-bold uppercase">Cụm rạp riêng</span>
              </label>
            </div>
            
            <div v-if="newVoucher.cinemaMode === 'specific'" class="flex flex-col gap-2 mt-2 p-4 bg-surface-container-highest rounded-xl border border-outline-variant/10">
              <label v-for="cinema in cinemasList" :key="cinema.id" class="flex items-center gap-2 cursor-pointer p-2 hover:bg-white/5 rounded transition-colors">
                <input type="checkbox" :value="cinema.id" v-model="newVoucher.selectedCinemas" class="accent-primary">
                <span class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">{{ cinema.name }}</span>
              </label>
            </div>
          </div>
        </div>

        <!-- Drawer Footer -->
        <div class="p-6 border-t border-outline-variant/10 bg-surface-container-lowest flex gap-4">
          <button @click="isVoucherDrawerOpen = false" class="flex-1 px-6 py-4 rounded-xl border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy bỏ</button>
          <button class="flex-1 px-6 py-4 rounded-xl bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform shadow-xl shadow-primary/20">Lưu Voucher</button>
        </div>
      </div>
    </div>
    <!-- Combo Drawer Form -->
    <div v-if="isComboDrawerOpen" class="fixed inset-0 z-[1000] flex justify-end">
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="isComboDrawerOpen = false"></div>
      
      <!-- Drawer Panel -->
      <div class="relative w-full max-w-md bg-surface-container-low h-full shadow-2xl flex flex-col border-l border-outline-variant/20 animate-in slide-in-from-right duration-300">
        <!-- Drawer Header -->
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <div>
            <h3 class="font-headline font-black uppercase italic text-primary text-xl">Tạo Combo</h3>
            <p class="text-xs text-on-surface-variant mt-1 uppercase tracking-widest font-bold">Thêm Combo Bắp Nước mới</p>
          </div>
          <button @click="isComboDrawerOpen = false" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        
        <!-- Drawer Body -->
        <div class="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-custom">
          <!-- Image Upload Mock -->
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ảnh Combo</label>
            <div class="w-full h-32 bg-surface-container-highest border-2 border-dashed border-outline-variant/20 rounded-2xl flex flex-col items-center justify-center text-on-surface-variant hover:border-primary/50 hover:bg-primary/5 transition-colors cursor-pointer">
              <span class="material-symbols-outlined text-3xl mb-2">cloud_upload</span>
              <span class="text-[10px] font-bold uppercase tracking-widest">Tải ảnh lên</span>
            </div>
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tên Combo</label>
            <input v-model="newCombo.name" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none uppercase" placeholder="VD: COMBO COUPLE" />
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Giá bán (VNĐ)</label>
            <input v-model="newCombo.price" type="number" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" placeholder="VD: 159000" />
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mô tả ngắn</label>
            <textarea v-model="newCombo.description" rows="2" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-medium text-on-surface focus:border-primary outline-none resize-none" placeholder="Mô tả thành phần..."></textarea>
          </div>

          <!-- Trạng thái -->
          <div class="flex items-center justify-between p-4 bg-surface-container-highest rounded-xl border border-outline-variant/10">
             <div>
                <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Trạng thái hiển thị</p>
             </div>
             <button @click="newCombo.status = newCombo.status === 'active' ? 'inactive' : 'active'" :class="newCombo.status === 'active' ? 'bg-green-500' : 'bg-surface-container-high'" class="relative w-10 h-5 rounded-full transition-colors duration-300 focus:outline-none shrink-0">
                <span :class="newCombo.status === 'active' ? 'translate-x-5 bg-white' : 'translate-x-0 bg-on-surface-variant'" class="inline-block w-4 h-4 transform rounded-full transition-transform duration-300 shadow-md absolute top-0.5 left-0.5"></span>
             </button>
          </div>

          <!-- Dynamic List: Items -->
          <div class="space-y-4 pt-4 border-t border-outline-variant/10">
            <div class="flex items-center justify-between">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Danh sách Món (Items)</label>
              <button @click="addComboItem" class="text-xs font-bold text-primary hover:text-white transition-colors flex items-center gap-1 uppercase tracking-widest">
                 <span class="material-symbols-outlined text-sm">add</span> Thêm món
              </button>
            </div>
            
            <div class="space-y-3">
              <div v-for="(item, index) in newCombo.items" :key="item.id" class="flex gap-2 items-center">
                <div class="flex-1 bg-surface-container-highest rounded-xl flex overflow-hidden border border-outline-variant/10">
                   <input v-model="item.quantity" type="number" min="1" class="w-16 bg-transparent p-3 text-sm font-bold text-center border-r border-outline-variant/10 outline-none text-primary" placeholder="SL" />
                   <input v-model="item.name" class="flex-1 bg-transparent p-3 text-sm font-bold outline-none text-on-surface" placeholder="Tên món (VD: Bắp lớn)" />
                </div>
                <button @click="removeComboItem(index)" :disabled="newCombo.items.length === 1" class="w-10 h-10 flex-shrink-0 flex items-center justify-center rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500 hover:text-white transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                  <span class="material-symbols-outlined text-sm">delete</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Drawer Footer -->
        <div class="p-6 border-t border-outline-variant/10 bg-surface-container-lowest flex gap-4">
          <button @click="isComboDrawerOpen = false" class="flex-1 px-6 py-4 rounded-xl border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy bỏ</button>
          <button class="flex-1 px-6 py-4 rounded-xl bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform shadow-xl shadow-primary/20">Lưu Combo</button>
        </div>
      </div>
    </div>

    <!-- Article Drawer Form -->
    <div v-if="isArticleDrawerOpen" class="fixed inset-0 z-[1000] flex justify-end">
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="isArticleDrawerOpen = false"></div>
      
      <!-- Drawer Panel -->
      <div class="relative w-full max-w-xl bg-surface-container-low h-full shadow-2xl flex flex-col border-l border-outline-variant/20 animate-in slide-in-from-right duration-300">
        <!-- Drawer Header -->
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <div>
            <h3 class="font-headline font-black uppercase italic text-primary text-xl">Thêm Tin Khuyến Mãi</h3>
            <p class="text-xs text-on-surface-variant mt-1 uppercase tracking-widest font-bold">Cập nhật nội dung hiển thị trên trang chủ</p>
          </div>
          <button @click="isArticleDrawerOpen = false" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        
        <!-- Drawer Body (Scrollable) -->
        <div class="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-custom">
          <!-- Image Upload Mock -->
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ảnh Banner / Thumbnail</label>
            <div class="w-full h-40 bg-surface-container-highest border-2 border-dashed border-outline-variant/20 rounded-2xl flex flex-col items-center justify-center text-on-surface-variant hover:border-primary/50 hover:bg-primary/5 transition-colors cursor-pointer">
              <span class="material-symbols-outlined text-3xl mb-2">cloud_upload</span>
              <span class="text-xs font-bold uppercase tracking-widest">Kéo thả ảnh hoặc click để tải lên</span>
            </div>
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tiêu đề Tin Khuyến Mãi</label>
            <input v-model="newArticle.title" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" placeholder="VD: Khuyến mãi Hè rực rỡ" />
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mô tả ngắn</label>
            <textarea v-model="newArticle.description" rows="2" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-medium text-on-surface focus:border-primary outline-none resize-none" placeholder="Mô tả tóm tắt hiển thị ở danh sách ngoài trang chủ..."></textarea>
          </div>

          <div class="grid grid-cols-2 gap-6">
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngày bắt đầu</label>
              <input v-model="newArticle.startDate" type="date" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" />
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngày kết thúc</label>
              <input v-model="newArticle.endDate" type="date" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" />
            </div>
          </div>

          <!-- Trạng thái -->
          <div class="flex items-center justify-between p-4 bg-surface-container-highest rounded-xl border border-outline-variant/10">
             <div>
                <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Trạng thái hiển thị</p>
                <p class="text-xs text-on-surface-variant mt-1">Cho phép hiển thị tin tức này trên hệ thống website</p>
             </div>
             <button @click="newArticle.status = newArticle.status === 'active' ? 'inactive' : 'active'" :class="newArticle.status === 'active' ? 'bg-green-500' : 'bg-surface-container-high'" class="relative w-10 h-5 rounded-full transition-colors duration-300 focus:outline-none shrink-0">
                <span :class="newArticle.status === 'active' ? 'translate-x-5 bg-white' : 'translate-x-0 bg-on-surface-variant'" class="inline-block w-4 h-4 transform rounded-full transition-transform duration-300 shadow-md absolute top-0.5 left-0.5"></span>
             </button>
          </div>

          <!-- Rich Text Mock -->
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Nội dung chi tiết</label>
            <div class="bg-surface-container-highest border border-outline-variant/20 rounded-xl overflow-hidden flex flex-col h-64">
              <!-- Toolbar -->
              <div class="bg-surface-container-lowest border-b border-outline-variant/10 p-2 flex gap-1 items-center">
                 <button class="w-8 h-8 rounded hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors"><span class="material-symbols-outlined text-sm">format_bold</span></button>
                 <button class="w-8 h-8 rounded hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors"><span class="material-symbols-outlined text-sm">format_italic</span></button>
                 <button class="w-8 h-8 rounded hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors"><span class="material-symbols-outlined text-sm">format_underlined</span></button>
                 <div class="w-px h-5 bg-outline-variant/20 mx-1"></div>
                 <button class="w-8 h-8 rounded hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors"><span class="material-symbols-outlined text-sm">format_list_bulleted</span></button>
                 <button class="w-8 h-8 rounded hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors"><span class="material-symbols-outlined text-sm">image</span></button>
                 <button class="w-8 h-8 rounded hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors"><span class="material-symbols-outlined text-sm">link</span></button>
              </div>
              <textarea v-model="newArticle.content" class="flex-1 w-full bg-transparent p-4 text-sm font-medium text-on-surface outline-none resize-none scrollbar-custom" placeholder="Soạn thảo nội dung chi tiết của chương trình khuyến mãi..."></textarea>
            </div>
          </div>
        </div>

        <!-- Drawer Footer -->
        <div class="p-6 border-t border-outline-variant/10 bg-surface-container-lowest flex gap-4">
          <button @click="isArticleDrawerOpen = false" class="flex-1 px-6 py-4 rounded-xl border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy bỏ</button>
          <button class="flex-1 px-6 py-4 rounded-xl bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform shadow-xl shadow-primary/20">Lưu Tin Tức</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(245, 197, 24, 0.2);
  border-radius: 10px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(245, 197, 24, 0.5);
}
</style>
