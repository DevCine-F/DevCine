<script setup>
import { ref } from 'vue'

const tickets = ref([
  { id: 'TKT-001', user: 'Hoàng Nam', type: 'Hoàn tiền', status: 'pending', priority: 'high', message: 'Tôi muốn hoàn tiền vé Oppenheimer do bận đột xuất.', time: '10 phút trước' },
  { id: 'TKT-002', user: 'Linh Chi', type: 'Lỗi ứng dụng', status: 'in-progress', priority: 'medium', message: 'Không thể thanh toán qua ví MoMo.', time: '45 phút trước' },
  { id: 'TKT-003', user: 'Minh Tuấn', type: 'Góp ý', status: 'closed', priority: 'low', message: 'Rạp nên có thêm bắp vị phô mai.', time: '2 giờ trước' }
])

const getStatusClass = (status) => {
  switch (status) {
    case 'pending': return 'bg-red-500/10 text-red-400 border-red-500/20'
    case 'in-progress': return 'bg-blue-500/10 text-blue-400 border-blue-500/20'
    case 'closed': return 'bg-green-500/10 text-green-400 border-green-500/20'
    default: return ''
  }
}
</script>

<template>
  <div class="p-10 space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
    <!-- Header -->
    <header class="flex justify-between items-end">
      <div>
        <h2 class="text-3xl font-black uppercase tracking-tighter italic font-headline">
          Chăm sóc <span class="text-primary">Khách hàng</span>
        </h2>
        <p class="text-[10px] font-black uppercase tracking-[0.3em] text-on-surface-variant mt-2">Hệ thống quản lý phản hồi & hỗ trợ người dùng</p>
      </div>
      
      <div class="flex gap-4">
        <div class="bg-surface-container-high px-6 py-3 rounded-lg border border-outline-variant/10 text-center">
          <p class="text-[9px] font-bold text-outline-variant uppercase tracking-widest mb-1">Đang chờ</p>
          <p class="text-xl font-black text-red-400">12</p>
        </div>
        <div class="bg-surface-container-high px-6 py-3 rounded-lg border border-outline-variant/10 text-center">
          <p class="text-[9px] font-bold text-outline-variant uppercase tracking-widest mb-1">Tỉ lệ giải quyết</p>
          <p class="text-xl font-black text-green-400">98%</p>
        </div>
      </div>
    </header>

    <!-- Content Grid -->
    <div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
      <!-- Ticket List -->
      <div class="xl:col-span-2 space-y-4">
        <div class="flex justify-between items-center mb-6">
          <h3 class="text-xs font-black uppercase tracking-widest text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">confirmation_number</span>
            Yêu cầu hỗ trợ gần đây
          </h3>
          <div class="flex gap-2">
            <button class="p-2 bg-surface-container-high rounded border border-outline-variant/10 hover:bg-white/5 transition-colors">
              <span class="material-symbols-outlined text-sm">filter_list</span>
            </button>
            <button class="p-2 bg-surface-container-high rounded border border-outline-variant/10 hover:bg-white/5 transition-colors">
              <span class="material-symbols-outlined text-sm">search</span>
            </button>
          </div>
        </div>

        <div v-for="ticket in tickets" :key="ticket.id" 
             class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-5 hover:border-primary/30 transition-all cursor-pointer group shadow-lg shadow-black/10">
          <div class="flex justify-between items-start mb-4">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-xs uppercase tracking-tighter">
                {{ ticket.user.split(' ').map(n => n[0]).join('') }}
              </div>
              <div>
                <p class="text-xs font-black text-on-surface tracking-tight">{{ ticket.user }}</p>
                <p class="text-[10px] text-on-surface-variant uppercase font-bold italic tracking-wider">{{ ticket.id }} • {{ ticket.type }}</p>
              </div>
            </div>
            <span :class="getStatusClass(ticket.status)" 
                  class="text-[9px] font-black uppercase tracking-widest px-3 py-1 rounded-full border">
              {{ ticket.status }}
            </span>
          </div>
          
          <p class="text-xs text-on-surface-variant leading-relaxed mb-4 group-hover:text-on-surface transition-colors">{{ ticket.message }}</p>
          
          <div class="flex justify-between items-center pt-4 border-t border-outline-variant/5">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-xs text-outline-variant">schedule</span>
              <span class="text-[9px] font-bold text-outline-variant uppercase">{{ ticket.time }}</span>
            </div>
            <div class="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <button class="px-4 py-1.5 bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest rounded-md hover:brightness-110 transition-all">Phản hồi</button>
              <button class="px-4 py-1.5 bg-surface-container-high text-on-surface text-[10px] font-black uppercase tracking-widest rounded-md hover:bg-white/5 transition-all">Chi tiết</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Stats & Feedback -->
      <div class="space-y-8">
        <!-- Satisfaction Card -->
        <div class="bg-primary/5 border border-primary/20 p-8 rounded-2xl relative overflow-hidden group">
          <div class="relative z-10">
            <h3 class="text-xs font-black uppercase tracking-widest text-primary mb-6">Độ hài lòng khách hàng</h3>
            <div class="flex items-end gap-2 mb-4">
              <span class="text-5xl font-black italic tracking-tighter text-white">4.9</span>
              <span class="text-xs font-bold text-primary mb-2">/ 5.0</span>
            </div>
            <div class="flex gap-1 mb-8">
              <span v-for="i in 5" :key="i" class="material-symbols-outlined text-primary text-sm">star</span>
            </div>
            <p class="text-[10px] text-on-surface-variant leading-relaxed uppercase font-bold tracking-widest">Dựa trên 1,240 đánh giá trong tháng này.</p>
          </div>
          <span class="material-symbols-outlined absolute -bottom-8 -right-8 text-9xl text-primary/10 group-hover:scale-110 transition-transform duration-700">sentiment_satisfied</span>
        </div>

        <!-- Quick Links -->
        <div class="bg-surface-container-high border border-outline-variant/10 p-6 rounded-2xl">
          <h3 class="text-[10px] font-black uppercase tracking-[0.2em] text-on-surface-variant mb-6">Thao tác nhanh</h3>
          <div class="grid grid-cols-1 gap-3">
             <button class="flex items-center gap-3 p-4 bg-surface-container-lowest hover:bg-white/5 rounded-xl border border-outline-variant/5 transition-all text-left group">
                <span class="material-symbols-outlined text-primary group-hover:rotate-12 transition-transform">mail</span>
                <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Gửi Email hàng loạt</span>
             </button>
             <button class="flex items-center gap-3 p-4 bg-surface-container-lowest hover:bg-white/5 rounded-xl border border-outline-variant/5 transition-all text-left group">
                <span class="material-symbols-outlined text-blue-400 group-hover:rotate-12 transition-transform">verified_user</span>
                <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Chính sách bảo mật</span>
             </button>
             <button class="flex items-center gap-3 p-4 bg-surface-container-lowest hover:bg-white/5 rounded-xl border border-outline-variant/5 transition-all text-left group">
                <span class="material-symbols-outlined text-red-400 group-hover:rotate-12 transition-transform">report_problem</span>
                <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Báo cáo vi phạm</span>
             </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
