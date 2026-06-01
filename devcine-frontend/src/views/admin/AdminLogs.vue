<script setup>
import { ref, computed } from 'vue'

const searchQuery = ref('')
const filterType = ref('all')

const logs = ref([
  {
    id: 'LOG-001',
    timestamp: '2026-04-28 11:45:22',
    actor: 'Nguyen Admin',
    role: 'Super Admin',
    action: 'CREATE',
    target: 'Suất chiếu',
    detail: 'Tạo mới suất chiếu Oppenheimer lúc 19:00 tại Phòng IMAX 01',
    ip: '192.168.1.45',
    status: 'success'
  },
  {
    id: 'LOG-002',
    timestamp: '2026-04-28 11:30:15',
    actor: 'Tran Staff',
    role: 'Nhân viên Quầy',
    action: 'UPDATE',
    target: 'Vé xem phim',
    detail: 'Hoàn vé #TK-99234 cho khách hàng Nguyễn Văn A',
    ip: '192.168.1.12',
    status: 'warning'
  },
  {
    id: 'LOG-003',
    timestamp: '2026-04-28 10:15:00',
    actor: 'Lumière AI',
    role: 'Hệ thống',
    action: 'SYSTEM',
    target: 'Lịch chiếu',
    detail: 'Chạy thuật toán Auto-Scheduling tối ưu hóa doanh thu',
    ip: 'localhost',
    status: 'success'
  },
  {
    id: 'LOG-004',
    timestamp: '2026-04-28 09:05:11',
    actor: 'Le Manager',
    role: 'Quản lý Rạp',
    action: 'DELETE',
    target: 'Voucher',
    detail: 'Xóa mã khuyến mãi hết hạn KHAI_TRUONG_2026',
    ip: '192.168.1.88',
    status: 'danger'
  },
  {
    id: 'LOG-005',
    timestamp: '2026-04-27 22:10:45',
    actor: 'Nguyen Admin',
    role: 'Super Admin',
    action: 'LOGIN',
    target: 'Hệ thống',
    detail: 'Đăng nhập thành công vào Admin Portal',
    ip: '115.79.22.10',
    status: 'success'
  }
])

const filteredLogs = computed(() => {
  let result = logs.value

  if (filterType.value !== 'all') {
    result = result.filter(log => log.action === filterType.value)
  }

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(log => 
      log.actor.toLowerCase().includes(query) || 
      log.detail.toLowerCase().includes(query) ||
      log.target.toLowerCase().includes(query)
    )
  }

  return result
})

const getActionColor = (action) => {
  switch (action) {
    case 'CREATE': return 'text-green-400 bg-green-400/10 border-green-400/20'
    case 'UPDATE': return 'text-yellow-400 bg-yellow-400/10 border-yellow-400/20'
    case 'DELETE': return 'text-red-400 bg-red-400/10 border-red-400/20'
    case 'SYSTEM': return 'text-primary bg-primary/10 border-primary/20'
    case 'LOGIN': return 'text-blue-400 bg-blue-400/10 border-blue-400/20'
    default: return 'text-gray-400 bg-gray-400/10 border-gray-400/20'
  }
}

const getActionLabel = (action) => {
  switch (action) {
    case 'CREATE': return 'Thêm mới'
    case 'UPDATE': return 'Cập nhật'
    case 'DELETE': return 'Xóa'
    case 'SYSTEM': return 'Hệ thống'
    case 'LOGIN': return 'Đăng nhập'
    default: return 'Khác'
  }
}
</script>

<template>
  <div class="h-full flex flex-col space-y-6 p-10">
    <!-- Header -->
    <div class="flex justify-between items-end flex-shrink-0">
      <div>
        <h1 class="text-3xl font-black text-on-surface tracking-tighter uppercase italic">Nhật ký <span class="text-primary">Hệ thống</span></h1>
        <p class="text-sm font-bold text-on-surface-variant uppercase tracking-widest mt-1">Truy vết thao tác & Cảnh báo an ninh</p>
      </div>
      <button class="px-6 py-3 bg-surface-container-high hover:bg-white/10 text-on-surface font-bold text-xs uppercase tracking-widest rounded transition-colors flex items-center gap-2 border border-outline-variant/20">
         <span class="material-symbols-outlined text-sm">download</span> Xuất báo cáo CSV
      </button>
    </div>

    <!-- Toolbar -->
    <div class="bg-surface-container-low p-4 rounded-xl border border-outline-variant/10 flex items-center justify-between shadow-xl flex-shrink-0">
       <div class="flex items-center gap-4 w-1/2">
          <div class="relative flex-grow">
             <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant">search</span>
             <input v-model="searchQuery" type="text" placeholder="Tìm kiếm theo Tác nhân, Hành động..." 
                    class="w-full bg-surface-container-highest border border-outline-variant/10 rounded-lg pl-12 pr-4 py-3 text-sm text-on-surface placeholder:text-on-surface-variant/50 focus:outline-none focus:border-primary/50 transition-colors">
          </div>
       </div>

       <div class="flex items-center gap-2 bg-surface-container-highest p-1 rounded-lg border border-outline-variant/10">
          <button @click="filterType = 'all'" :class="filterType === 'all' ? 'bg-surface-container-low text-on-surface shadow' : 'text-on-surface-variant hover:text-on-surface'" class="px-4 py-2 text-[10px] font-bold uppercase tracking-widest rounded transition-all">Tất cả</button>
          <button @click="filterType = 'CREATE'" :class="filterType === 'CREATE' ? 'bg-surface-container-low text-on-surface shadow' : 'text-on-surface-variant hover:text-on-surface'" class="px-4 py-2 text-[10px] font-bold uppercase tracking-widest rounded transition-all">Tạo mới</button>
          <button @click="filterType = 'UPDATE'" :class="filterType === 'UPDATE' ? 'bg-surface-container-low text-on-surface shadow' : 'text-on-surface-variant hover:text-on-surface'" class="px-4 py-2 text-[10px] font-bold uppercase tracking-widest rounded transition-all">Cập nhật</button>
          <button @click="filterType = 'DELETE'" :class="filterType === 'DELETE' ? 'bg-surface-container-low text-on-surface shadow' : 'text-on-surface-variant hover:text-on-surface'" class="px-4 py-2 text-[10px] font-bold uppercase tracking-widest rounded transition-all">Xóa</button>
          <button @click="filterType = 'SYSTEM'" :class="filterType === 'SYSTEM' ? 'bg-surface-container-low text-on-surface shadow' : 'text-on-surface-variant hover:text-on-surface'" class="px-4 py-2 text-[10px] font-bold uppercase tracking-widest rounded transition-all">Hệ thống</button>
       </div>
    </div>

    <!-- Data Table -->
    <div class="flex-grow bg-surface-container-low rounded-xl border border-outline-variant/10 flex flex-col overflow-hidden shadow-2xl relative">
      <!-- Table Header -->
      <div class="grid grid-cols-12 gap-4 p-4 border-b border-outline-variant/10 bg-surface-container-high text-[10px] font-black uppercase tracking-widest text-on-surface-variant sticky top-0 z-10 shadow-md">
         <div class="col-span-2 pl-4">Thời gian</div>
         <div class="col-span-3">Tác nhân</div>
         <div class="col-span-2">Phân loại</div>
         <div class="col-span-4">Chi tiết hành động</div>
         <div class="col-span-1 text-center">IP / Trạng thái</div>
      </div>

      <!-- Table Body -->
      <div class="flex-grow overflow-y-auto">
         <div v-for="log in filteredLogs" :key="log.id" class="grid grid-cols-12 gap-4 p-4 border-b border-outline-variant/5 items-center hover:bg-white/[0.02] transition-colors group">
            
            <!-- Time -->
            <div class="col-span-2 pl-4">
               <p class="text-sm font-black text-on-surface group-hover:text-primary transition-colors">{{ log.timestamp.split(' ')[1] }}</p>
               <p class="text-[10px] font-bold text-on-surface-variant">{{ log.timestamp.split(' ')[0] }}</p>
            </div>

            <!-- Actor -->
            <div class="col-span-3 flex items-center gap-3">
               <div class="w-10 h-10 rounded-full bg-surface-container-highest border border-outline-variant/20 flex items-center justify-center flex-shrink-0">
                  <span v-if="log.action === 'SYSTEM'" class="material-symbols-outlined text-primary text-sm">smart_toy</span>
                  <span v-else class="material-symbols-outlined text-on-surface-variant text-sm">person</span>
               </div>
               <div>
                  <p class="text-xs font-black text-on-surface uppercase">{{ log.actor }}</p>
                  <p class="text-[9px] font-bold text-primary italic uppercase tracking-widest mt-0.5">{{ log.role }}</p>
               </div>
            </div>

            <!-- Action Tag -->
            <div class="col-span-2">
               <div :class="getActionColor(log.action)" class="inline-flex px-2.5 py-1 rounded text-[9px] font-black uppercase tracking-widest border">
                  {{ getActionLabel(log.action) }}
               </div>
               <p class="text-[10px] text-on-surface-variant mt-1.5 uppercase font-bold">{{ log.target }}</p>
            </div>

            <!-- Detail -->
            <div class="col-span-4 pr-4">
               <p class="text-xs text-on-surface/90 leading-relaxed">{{ log.detail }}</p>
               <p class="text-[9px] text-on-surface-variant mt-1 font-mono opacity-50">{{ log.id }}</p>
            </div>

            <!-- Status / IP -->
            <div class="col-span-1 flex flex-col items-center justify-center">
               <span v-if="log.status === 'success'" class="material-symbols-outlined text-green-400">check_circle</span>
               <span v-else-if="log.status === 'warning'" class="material-symbols-outlined text-yellow-400">warning</span>
               <span v-else-if="log.status === 'danger'" class="material-symbols-outlined text-red-400">error</span>
               <p class="text-[9px] font-mono text-on-surface-variant mt-1">{{ log.ip }}</p>
            </div>

         </div>

         <!-- Empty State -->
         <div v-if="filteredLogs.length === 0" class="p-16 flex flex-col items-center justify-center text-center">
            <span class="material-symbols-outlined text-6xl text-on-surface-variant/30 mb-4">search_off</span>
            <p class="text-sm font-black text-on-surface uppercase tracking-widest">Không tìm thấy dữ liệu</p>
            <p class="text-[10px] text-on-surface-variant mt-2 uppercase">Vui lòng thử lại với từ khóa khác</p>
         </div>
      </div>
    </div>
  </div>
</template>
