<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { supportTicketApi } from '@/api/admin/index'
import { useAdminPerm } from '@/composables/useAdminPerm'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'
import {
  issueTypeLabel, statusLabel, statusClass, parseSupportContent, formatTime
} from '@/utils/supportTicket'
import TicketDetailModal from '@/components/admin/support/TicketDetailModal.vue'

const { can } = useAdminPerm()
const toast = useToastStore()
const tickets = ref([])
const isLoading = ref(false)

// Bộ lọc + tìm kiếm
const statusFilter = ref('ALL')
const searchQuery = ref('')
const debouncedQuery = ref('')
let searchTimer = null
watch(searchQuery, (v) => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { debouncedQuery.value = v.trim().toLowerCase() }, 400)
})

const STATUS_TABS = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'OPEN', label: 'Chờ xử lý' },
  { value: 'IN_PROGRESS', label: 'Đang xử lý' },
  { value: 'CLOSED', label: 'Đã đóng' }
]

const counts = computed(() => ({
  OPEN: tickets.value.filter(t => t.status === 'OPEN').length,
  IN_PROGRESS: tickets.value.filter(t => t.status === 'IN_PROGRESS').length,
  CLOSED: tickets.value.filter(t => t.status === 'CLOSED').length
}))
const pendingCount = computed(() => counts.value.OPEN)
const resolutionRate = computed(() => {
  const total = tickets.value.length
  return total ? Math.round((counts.value.CLOSED / total) * 100) : 0
})

const filteredTickets = computed(() => {
  const q = debouncedQuery.value
  return tickets.value.filter(t => {
    if (statusFilter.value !== 'ALL' && t.status !== statusFilter.value) return false
    if (!q) return true
    const phone = parseSupportContent(t).phone
    const haystack = [
      `#${t.id}`, t.customerName, phone, issueTypeLabel(t.issueType)
    ].filter(Boolean).join(' ').toLowerCase()
    return haystack.includes(q)
  })
})

const fetchTickets = async () => {
  isLoading.value = true
  try {
    const { data } = await supportTicketApi.getAll()
    tickets.value = data.data ?? data
  } catch (e) {
    tickets.value = []
    toast.error(friendlyError(e, 'Không tải được danh sách yêu cầu hỗ trợ.'))
  } finally {
    isLoading.value = false
  }
}

const updateStatus = async (ticket, newStatus) => {
  try {
    await supportTicketApi.updateStatus(ticket.id, newStatus)
    ticket.status = newStatus
  } catch (e) {
    toast.error(friendlyError(e, 'Không cập nhật được trạng thái yêu cầu.'))
  }
}

// --- Chi tiết & phản hồi ---
const selectedTicket = ref(null)
const isReplying = ref(false)
const showDetail = computed(() => !!selectedTicket.value)

const openTicket = (ticket) => { selectedTicket.value = ticket }
const closeDetail = () => { selectedTicket.value = null }

const handleUpdateStatus = async (newStatus) => {
  if (!selectedTicket.value) return
  await updateStatus(selectedTicket.value, newStatus)
}

const handleSendReply = async (message) => {
  if (!selectedTicket.value) return
  isReplying.value = true
  try {
    const { data } = await supportTicketApi.reply(selectedTicket.value.id, { message })
    const t = selectedTicket.value
    t.adminReply = message
    t.repliedAt = new Date().toISOString()
    if (t.status === 'OPEN') t.status = 'IN_PROGRESS'
    toast.success(data?.data?.message || 'Đã gửi phản hồi.')
  } catch (e) {
    toast.error(friendlyError(e, 'Gửi phản hồi thất bại.'))
  } finally {
    isReplying.value = false
  }
}

onMounted(fetchTickets)
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
          <p class="text-xl font-black text-red-400">{{ pendingCount }}</p>
        </div>
        <div class="bg-surface-container-high px-6 py-3 rounded-lg border border-outline-variant/10 text-center">
          <p class="text-[9px] font-bold text-outline-variant uppercase tracking-widest mb-1">Tổng ticket</p>
          <p class="text-xl font-black text-primary">{{ tickets.length }}</p>
        </div>
      </div>
    </header>

    <!-- Content Grid -->
    <div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
      <!-- Ticket List -->
      <div class="xl:col-span-2 space-y-4">
        <div class="flex flex-col gap-4 mb-6">
          <div class="flex justify-between items-center gap-4">
            <h3 class="text-xs font-black uppercase tracking-widest text-on-surface flex items-center gap-2 whitespace-nowrap">
              <span class="material-symbols-outlined text-primary">confirmation_number</span>
              Yêu cầu hỗ trợ gần đây
            </h3>
            <!-- Search -->
            <div class="relative flex-1 max-w-xs">
              <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-sm text-on-surface-variant pointer-events-none">search</span>
              <input v-model="searchQuery" type="text" placeholder="Tìm mã, tên, SĐT, chủ đề..."
                     class="w-full bg-surface-container-high border border-outline-variant/10 focus:border-primary/40 text-on-surface text-xs pl-9 pr-3 py-2 rounded-lg outline-none transition-colors" />
            </div>
          </div>
          <!-- Filter tabs -->
          <div class="flex gap-2 flex-wrap">
            <button v-for="tab in STATUS_TABS" :key="tab.value" @click="statusFilter = tab.value"
                    :class="statusFilter === tab.value ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:bg-white/5'"
                    class="px-3 py-1.5 text-[10px] font-black uppercase tracking-widest rounded-md border border-outline-variant/10 transition-all">
              {{ tab.label }}
              <span v-if="tab.value !== 'ALL'" class="ml-1 opacity-70">{{ counts[tab.value] }}</span>
            </button>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="isLoading" class="flex flex-col gap-4">
          <div v-for="i in 3" :key="i" class="bg-surface-container-low h-28 rounded-xl animate-pulse"></div>
        </div>

        <!-- Empty (không có ticket nào) -->
        <div v-else-if="tickets.length === 0" class="flex flex-col items-center justify-center py-16 text-center">
          <span class="material-symbols-outlined text-5xl text-outline-variant mb-4">support_agent</span>
          <p class="text-on-surface-variant font-semibold">Không có yêu cầu hỗ trợ nào</p>
        </div>

        <!-- Empty (lọc không ra kết quả) -->
        <div v-else-if="filteredTickets.length === 0" class="flex flex-col items-center justify-center py-16 text-center">
          <span class="material-symbols-outlined text-5xl text-outline-variant mb-4">search_off</span>
          <p class="text-on-surface-variant font-semibold">Không tìm thấy ticket phù hợp</p>
        </div>

        <div v-else v-for="ticket in filteredTickets" :key="ticket.id" @click="openTicket(ticket)"
             class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-5 hover:border-primary/30 transition-all cursor-pointer group shadow-lg shadow-black/10">
          <div class="flex justify-between items-start mb-3">
            <div class="flex items-center gap-3 min-w-0">
              <div class="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-xs uppercase tracking-tighter flex-shrink-0">
                {{ (ticket.customerName || 'KH').split(' ').slice(-1)[0]?.slice(0, 2) }}
              </div>
              <div class="min-w-0">
                <p class="font-headline text-sm font-black text-on-surface tracking-tight truncate">{{ issueTypeLabel(ticket.issueType) }}</p>
                <p class="text-[10px] text-on-surface-variant uppercase font-bold italic tracking-wider truncate">
                  #{{ ticket.id }} • {{ ticket.customerName || 'Khách hàng' }}
                </p>
              </div>
            </div>
            <span :class="statusClass(ticket.status)"
                  class="text-[9px] font-black uppercase tracking-widest px-3 py-1 rounded-full border whitespace-nowrap flex-shrink-0">
              {{ statusLabel(ticket.status) }}
            </span>
          </div>

          <!-- Badge liên hệ -->
          <div class="flex flex-wrap gap-2 mb-3 min-w-0">
            <span v-if="ticket.customerEmail" class="inline-flex items-center gap-1 max-w-full min-w-0 text-[10px] text-on-surface-variant bg-surface-container-high border border-outline-variant/10 rounded px-2 py-0.5">
              <span class="material-symbols-outlined text-xs text-primary flex-shrink-0">mail</span><span class="truncate">{{ ticket.customerEmail }}</span>
            </span>
            <span v-if="parseSupportContent(ticket).phone" class="inline-flex items-center gap-1 max-w-full min-w-0 text-[10px] text-on-surface-variant bg-surface-container-high border border-outline-variant/10 rounded px-2 py-0.5">
              <span class="material-symbols-outlined text-xs text-primary flex-shrink-0">call</span><span class="truncate">{{ parseSupportContent(ticket).phone }}</span>
            </span>
          </div>

          <p class="text-xs text-on-surface-variant leading-relaxed mb-4 break-words line-clamp-2 group-hover:text-on-surface transition-colors">{{ parseSupportContent(ticket).message }}</p>

          <div class="flex justify-between items-center pt-4 border-t border-outline-variant/5">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-xs text-outline-variant">schedule</span>
              <span class="text-[9px] font-bold text-outline-variant uppercase">{{ formatTime(ticket.createdAt) }}</span>
              <span v-if="ticket.adminReply" class="inline-flex items-center gap-1 text-[9px] font-bold text-green-400 uppercase ml-2">
                <span class="material-symbols-outlined text-xs">check_circle</span>Đã phản hồi
              </span>
            </div>
            <div class="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <button v-if="ticket.status === 'OPEN' && can('support', 'edit')" @click.stop="updateStatus(ticket, 'IN_PROGRESS')" class="px-4 py-1.5 bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest rounded-md hover:brightness-110 transition-all">Xử lý</button>
              <button v-if="ticket.status !== 'CLOSED' && can('support', 'edit')" @click.stop="updateStatus(ticket, 'CLOSED')" class="px-4 py-1.5 bg-surface-container-high text-on-surface text-[10px] font-black uppercase tracking-widest rounded-md hover:bg-white/5 transition-all">Đóng</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Thống kê -->
      <div class="space-y-8">
        <div class="bg-surface-container-high border border-outline-variant/10 p-8 rounded-2xl">
          <h3 class="text-xs font-black uppercase tracking-widest text-primary mb-6">Thống kê xử lý</h3>
          <div class="flex items-end gap-2 mb-2">
            <span class="font-headline text-5xl font-black italic tracking-tighter text-white">{{ resolutionRate }}%</span>
          </div>
          <p class="text-[10px] text-on-surface-variant leading-relaxed uppercase font-bold tracking-widest mb-6">Tỉ lệ ticket đã đóng</p>

          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <span class="inline-flex items-center gap-2 text-xs font-bold text-on-surface-variant">
                <span class="w-2 h-2 rounded-full bg-red-400"></span>Chờ xử lý
              </span>
              <span class="text-sm font-black text-on-surface">{{ counts.OPEN }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="inline-flex items-center gap-2 text-xs font-bold text-on-surface-variant">
                <span class="w-2 h-2 rounded-full bg-blue-400"></span>Đang xử lý
              </span>
              <span class="text-sm font-black text-on-surface">{{ counts.IN_PROGRESS }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="inline-flex items-center gap-2 text-xs font-bold text-on-surface-variant">
                <span class="w-2 h-2 rounded-full bg-green-400"></span>Đã đóng
              </span>
              <span class="text-sm font-black text-on-surface">{{ counts.CLOSED }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <TicketDetailModal
      :show="showDetail"
      :ticket="selectedTicket"
      :can-edit="can('support', 'edit')"
      :submitting="isReplying"
      @close="closeDetail"
      @reply="handleSendReply"
      @update-status="handleUpdateStatus" />
  </div>
</template>
