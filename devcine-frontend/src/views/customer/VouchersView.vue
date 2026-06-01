<script setup>
import { ref } from 'vue'

const activeTab = ref('active') // 'active' | 'history'

// Mock data for Active Vouchers
const activeVouchers = [
  {
    id: 'VC-WELCOME24',
    title: 'Giảm 50K vé 2D',
    description: 'Áp dụng cho mọi cụm rạp DevCine toàn quốc. Không áp dụng Lễ/Tết.',
    type: 'Giảm giá vé',
    expiry: '31/12/2024',
    discount: '50K'
  },
  {
    id: 'CB-POPCORN',
    title: 'Tặng 1 Bắp Nước Nhỏ',
    description: 'Áp dụng khi mua kèm 2 vé 2D/3D. Dành riêng cho hạng thành viên.',
    type: 'Quà tặng',
    expiry: '15/11/2024',
    discount: 'FREE'
  }
]

// Mock data for Voucher History
const historyVouchers = [
  {
    date: '25/10/2024 19:15',
    code: 'VC-HALLOWEEN',
    description: 'Giảm 20% tổng hóa đơn vé xem phim kinh dị',
    status: 'Đã sử dụng'
  },
  {
    date: '10/09/2024 14:30',
    code: 'VC-SUMMER',
    description: 'Tặng vé xem phim miễn phí thứ 3',
    status: 'Đã hết hạn'
  }
]
</script>

<template>
  <section class="w-full">
    <div class="flex flex-col md:flex-row justify-between items-baseline mb-8 gap-4">
      <h2 class="text-2xl font-bold tracking-tight font-headline">Ưu đãi của tôi</h2>
      <div class="flex gap-4">
        <button 
          @click="activeTab = 'active'"
          :class="[
            'text-xs font-bold uppercase tracking-widest pb-1 border-b-2 transition-colors',
            activeTab === 'active' ? 'border-primary-container text-primary-container' : 'border-transparent text-neutral-500 hover:text-on-surface'
          ]"
        >
          Voucher của tôi
        </button>
        <button 
          @click="activeTab = 'history'"
          :class="[
            'text-xs font-bold uppercase tracking-widest pb-1 border-b-2 transition-colors',
            activeTab === 'history' ? 'border-primary-container text-primary-container' : 'border-transparent text-neutral-500 hover:text-on-surface'
          ]"
        >
          Lịch sử voucher
        </button>
      </div>
    </div>
    
    <!-- Tab 1: Active Vouchers -->
    <div v-if="activeTab === 'active'" class="grid grid-cols-1 xl:grid-cols-2 gap-6">
      <div v-for="voucher in activeVouchers" :key="voucher.id" class="relative bg-surface-container-low rounded-xl flex overflow-hidden border border-white/5 hover:border-primary-container/30 transition-all shadow-xl">
        <!-- Left side / Ticket stub -->
        <div class="w-24 md:w-32 bg-primary-container/10 flex flex-col items-center justify-center border-r border-dashed border-white/20 p-4 shrink-0">
          <span class="material-symbols-outlined text-4xl text-primary-container mb-2">loyalty</span>
          <span class="text-[9px] md:text-[10px] font-bold uppercase tracking-widest text-primary-container text-center">{{ voucher.type }}</span>
        </div>
        <!-- Right side / Details -->
        <div class="p-6 flex-grow flex flex-col">
          <div class="flex flex-col md:flex-row md:justify-between md:items-start mb-2 gap-2">
            <h3 class="text-lg md:text-xl font-bold font-headline text-white">{{ voucher.title }}</h3>
            <span class="bg-surface-container-high text-[10px] font-bold px-2 py-1 rounded border border-white/10 tracking-widest w-fit">{{ voucher.id }}</span>
          </div>
          <p class="text-sm text-on-surface-variant mb-6 flex-grow">{{ voucher.description }}</p>
          <div class="flex justify-between items-center mt-auto pt-4 border-t border-white/5">
            <div>
              <p class="text-[9px] uppercase tracking-widest text-neutral-500 mb-0.5">Ngày hết hạn</p>
              <p class="text-xs font-bold text-error">{{ voucher.expiry }}</p>
            </div>
            <button class="bg-primary-container text-on-primary text-[10px] font-bold uppercase tracking-widest px-4 py-2 hover:bg-primary-fixed-dim transition-colors rounded-sm">
              Dùng ngay
            </button>
          </div>
        </div>
      </div>
      
      <div v-if="activeVouchers.length === 0" class="col-span-full py-20 text-center border border-dashed border-white/10 rounded-xl">
        <span class="material-symbols-outlined text-4xl text-neutral-600 mb-4">sentiment_dissatisfied</span>
        <p class="text-neutral-400">Bạn chưa có voucher nào trong ví.</p>
      </div>
    </div>
    
    <!-- Tab 2: Voucher History -->
    <div v-if="activeTab === 'history'" class="bg-surface-container-low rounded-xl border border-white/5 overflow-hidden shadow-xl">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-surface-container-high border-b border-white/5">
              <th class="py-4 px-6 text-[10px] uppercase font-bold tracking-widest text-on-surface-variant whitespace-nowrap">Thời gian</th>
              <th class="py-4 px-6 text-[10px] uppercase font-bold tracking-widest text-on-surface-variant whitespace-nowrap">Mã voucher</th>
              <th class="py-4 px-6 text-[10px] uppercase font-bold tracking-widest text-on-surface-variant">Nội dung voucher</th>
              <th class="py-4 px-6 text-[10px] uppercase font-bold tracking-widest text-on-surface-variant whitespace-nowrap text-right">Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in historyVouchers" :key="index" class="border-b border-white/5 last:border-0 hover:bg-white/5 transition-colors">
              <td class="py-4 px-6 text-sm whitespace-nowrap">{{ item.date }}</td>
              <td class="py-4 px-6">
                <span class="bg-surface-container-highest px-2 py-1 rounded text-[10px] font-bold tracking-widest border border-white/5 inline-block">{{ item.code }}</span>
              </td>
              <td class="py-4 px-6 text-sm text-neutral-300">{{ item.description }}</td>
              <td class="py-4 px-6 whitespace-nowrap text-right">
                <span :class="[
                  'text-[9px] font-bold uppercase tracking-widest px-2 py-1 rounded-sm inline-block',
                  item.status === 'Đã sử dụng' ? 'bg-primary-container/20 text-primary-container' : 'bg-error/20 text-error'
                ]">
                  {{ item.status }}
                </span>
              </td>
            </tr>
            <tr v-if="historyVouchers.length === 0">
              <td colspan="4" class="py-12 text-center text-neutral-500">Chưa có lịch sử sử dụng voucher.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>
