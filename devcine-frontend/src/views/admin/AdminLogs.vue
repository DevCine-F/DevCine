<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { auditLogApi } from '@/api/admin/index'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()
const searchQuery = ref('')
const filterType = ref('all')
const logs = ref([])
const isLoading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const totalElements = ref(0)
const pageSize = ref(10)
const pageSizeDropdownOpen = ref(false)
const PAGE_SIZE_OPTIONS = [10, 20, 50]

const fetchLogs = async () => {
  isLoading.value = true
  try {
    const params = {
      page: Math.max(0, currentPage.value - 1),
      size: pageSize.value
    }
    if (filterType.value !== 'all') params.action = filterType.value
    const { data } = await auditLogApi.getLogs(params)
    const result = data.data ?? data
    if (result.content) {
      logs.value = result.content
      totalPages.value = Math.max(1, result.totalPages ?? 1)
      totalElements.value = result.totalElements ?? result.content.length
      if (currentPage.value > totalPages.value) {
        currentPage.value = totalPages.value
      }
    } else {
      logs.value = Array.isArray(result) ? result : []
      totalPages.value = 1
      totalElements.value = logs.value.length
    }
  } catch (e) {
    logs.value = []
    totalElements.value = 0
    totalPages.value = 1
    toast.error(friendlyError(e, 'Không tải được nhật ký hoạt động.'))
  } finally {
    isLoading.value = false
  }
}

const changePageSize = (size) => {
  pageSize.value = size
  currentPage.value = 1
  pageSizeDropdownOpen.value = false
  fetchLogs()
}

const goToPage = (page) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  currentPage.value = page
  fetchLogs()
}

watch(filterType, () => {
  currentPage.value = 1
  fetchLogs()
})

const filteredLogs = computed(() => {
  if (!searchQuery.value) return logs.value
  const q = searchQuery.value.toLowerCase()
  return logs.value.filter(l =>
    (l.performedBy || l.actor || '').toLowerCase().includes(q) ||
    (l.description || l.detail || '').toLowerCase().includes(q) ||
    (l.entityType || l.target || '').toLowerCase().includes(q)
  )
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
    default: return action || 'Khác'
  }
}

const formatTimestamp = (iso) => {
  if (!iso) return { date: '', time: '' }
  const d = new Date(iso)
  return {
    date: d.toLocaleDateString('vi-VN'),
    time: d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  }
}

const exportCsv = () => {
  if (filteredLogs.value.length === 0) {
    toast.info('Không có dữ liệu phù hợp để xuất.')
    return
  }

  const header = [
    'Mã nhật ký',
    'Thời gian',
    'Tác nhân',
    'Vai trò',
    'Hành động',
    'Bảng / Đối tượng',
    'Chi tiết',
    'IP'
  ]

  const lines = filteredLogs.value.map(l => {
    const ts = formatTimestamp(l.createdAt ?? l.timestamp)
    return [
      `#${l.logId ?? l.id ?? ''}`,
      `${ts.time} ${ts.date}`,
      l.performedBy ?? l.actor ?? 'system',
      l.userRole ?? l.role ?? 'SYSTEM',
      getActionLabel((l.action ?? '').toUpperCase()),
      l.entityType ?? l.target ?? '',
      l.description ?? l.detail ?? '',
      l.ipAddress ?? ''
    ].map(v => `"${String(v ?? '').replace(/"/g, '""')}"`).join(',')
  })

  const csv = '\uFEFF' + [header.join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `nhat-ky-he-thong-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  toast.success('Xuất nhật ký hệ thống thành công!')
}

onMounted(fetchLogs)
</script>

<template>
  <div class="h-full flex flex-col space-y-6 p-10">
    <!-- Header -->
    <div class="flex justify-between items-end flex-shrink-0">
      <div>
        <h1 class="text-3xl font-black text-on-surface tracking-tighter uppercase italic">Nhật ký <span class="text-primary">Hệ thống</span></h1>
        <p class="text-sm font-bold text-on-surface-variant uppercase tracking-widest mt-1">Truy vết thao tác & Cảnh báo an ninh</p>
      </div>
      <button @click="exportCsv" class="px-6 py-3 bg-surface-container-high hover:bg-white/10 text-on-surface font-bold text-xs uppercase tracking-widest rounded transition-colors flex items-center gap-2 border border-outline-variant/20">
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
         <!-- Loading -->
         <div v-if="isLoading" class="flex items-center justify-center py-16">
            <span class="material-symbols-outlined animate-spin text-primary text-3xl">autorenew</span>
         </div>

         <template v-else>
         <div v-for="log in filteredLogs" :key="log.id ?? log.logId" class="grid grid-cols-12 gap-4 p-4 border-b border-outline-variant/5 items-center hover:bg-white/[0.02] transition-colors group">

            <!-- Time -->
            <div class="col-span-2 pl-4">
               <p class="text-sm font-black text-on-surface group-hover:text-primary transition-colors">{{ formatTimestamp(log.createdAt ?? log.timestamp).time }}</p>
               <p class="text-[10px] font-bold text-on-surface-variant">{{ formatTimestamp(log.createdAt ?? log.timestamp).date }}</p>
            </div>

            <!-- Actor -->
            <div class="col-span-3 flex items-center gap-3">
               <div class="w-10 h-10 rounded-full bg-surface-container-highest border border-outline-variant/20 flex items-center justify-center flex-shrink-0">
                  <span v-if="(log.action ?? '').toUpperCase() === 'SYSTEM'" class="material-symbols-outlined text-primary text-sm">smart_toy</span>
                  <span v-else class="material-symbols-outlined text-on-surface-variant text-sm">person</span>
               </div>
               <div>
                  <p class="text-xs font-black text-on-surface uppercase">{{ log.performedBy ?? log.actor }}</p>
                  <p class="text-[9px] font-bold text-primary italic uppercase tracking-widest mt-0.5">{{ log.userRole ?? log.role }}</p>
               </div>
            </div>

            <!-- Action Tag -->
            <div class="col-span-2">
               <div :class="getActionColor((log.action ?? '').toUpperCase())" class="inline-flex px-2.5 py-1 rounded text-[9px] font-black uppercase tracking-widest border">
                  {{ getActionLabel((log.action ?? '').toUpperCase()) }}
               </div>
               <p class="text-[10px] text-on-surface-variant mt-1.5 uppercase font-bold">{{ log.entityType ?? log.target }}</p>
            </div>

            <!-- Detail -->
            <div class="col-span-4 pr-4">
               <p class="text-xs text-on-surface/90 leading-relaxed">{{ log.description ?? log.detail }}</p>
               <p class="text-[9px] text-on-surface-variant mt-1 font-mono opacity-50">#{{ log.logId ?? log.id }}</p>
            </div>

            <!-- Status -->
            <div class="col-span-1 flex flex-col items-center justify-center">
               <span class="material-symbols-outlined text-green-400">check_circle</span>
            </div>

         </div>

         <!-- Empty State -->
         <div v-if="filteredLogs.length === 0" class="p-16 flex flex-col items-center justify-center text-center">
            <span class="material-symbols-outlined text-6xl text-on-surface-variant/30 mb-4">search_off</span>
            <p class="text-sm font-black text-on-surface uppercase tracking-widest">Không tìm thấy dữ liệu</p>
            <p class="text-[10px] text-on-surface-variant mt-2 uppercase">Vui lòng thử lại với từ khóa khác</p>
         </div>
         </template>
      </div>

      <!-- Pagination & Footer với Custom Page Size Dropdown -->
      <div class="p-4 bg-surface-container-highest/30 text-[11px] font-bold uppercase tracking-widest text-on-surface-variant flex flex-col sm:flex-row justify-between items-center gap-4 border-t border-outline-variant/10 flex-shrink-0">
        <!-- Page size selector & Summary text -->
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2">
            <span>Hiển thị:</span>

            <!-- Custom Page Size Dropdown -->
            <div class="relative">
              <button
                type="button"
                @click="pageSizeDropdownOpen = !pageSizeDropdownOpen"
                class="h-8 bg-surface-container-highest border rounded-lg px-2.5 text-xs font-bold font-mono text-on-surface outline-none cursor-pointer flex items-center gap-1.5 transition-all shadow-sm"
                :class="pageSizeDropdownOpen ? 'border-primary/60 ring-2 ring-primary/15' : 'border-outline-variant/10 hover:border-outline-variant/30'"
              >
                <span>{{ pageSize }}</span>
                <span class="material-symbols-outlined text-sm text-on-surface-variant transition-transform duration-200" :class="{ 'rotate-180': pageSizeDropdownOpen }">expand_more</span>
              </button>

              <div v-if="pageSizeDropdownOpen" class="fixed inset-0 z-[55]" @click="pageSizeDropdownOpen = false"></div>

              <transition name="fade">
                <div v-if="pageSizeDropdownOpen" class="absolute bottom-full left-0 mb-1.5 w-24 bg-surface-container-high border border-outline-variant/20 rounded-xl shadow-[0_12px_40px_-8px_rgba(0,0,0,0.7)] z-[60] overflow-hidden py-1 backdrop-blur-xl">
                  <button
                    v-for="size in PAGE_SIZE_OPTIONS"
                    :key="size"
                    type="button"
                    @click="changePageSize(size)"
                    class="w-full flex items-center justify-between px-3 py-2 text-xs font-mono transition-colors"
                    :class="pageSize === size ? 'text-primary bg-primary/10 font-bold' : 'text-on-surface-variant hover:bg-white/5 hover:text-on-surface'"
                  >
                    <span>{{ size }}</span>
                    <span v-if="pageSize === size" class="material-symbols-outlined text-sm text-primary">check</span>
                  </button>
                </div>
              </transition>
            </div>

            <span>dòng/trang</span>
          </div>
          <span class="hidden md:inline text-on-surface-variant/40">|</span>
          <span>
            Tổng: <strong class="text-primary">{{ totalElements.toLocaleString('vi-VN') }}</strong> nhật ký
          </span>
        </div>

        <!-- Navigation Buttons -->
        <div class="flex items-center gap-1">
          <button
            @click="goToPage(1)"
            :disabled="currentPage === 1 || isLoading"
            class="p-1.5 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
            title="Trang đầu"
          >
            <span class="material-symbols-outlined text-base">first_page</span>
          </button>
          <button
            @click="goToPage(currentPage - 1)"
            :disabled="currentPage === 1 || isLoading"
            class="p-1.5 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
            title="Trang trước"
          >
            <span class="material-symbols-outlined text-base">chevron_left</span>
          </button>

          <span class="px-3 py-1 bg-surface-container-highest rounded-lg font-mono font-bold text-primary text-xs">
            {{ currentPage }} / {{ totalPages }}
          </span>

          <button
            @click="goToPage(currentPage + 1)"
            :disabled="currentPage === totalPages || isLoading"
            class="p-1.5 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
            title="Trang sau"
          >
            <span class="material-symbols-outlined text-base">chevron_right</span>
          </button>
          <button
            @click="goToPage(totalPages)"
            :disabled="currentPage === totalPages || isLoading"
            class="p-1.5 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
            title="Trang cuối"
          >
            <span class="material-symbols-outlined text-base">last_page</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}
</style>
