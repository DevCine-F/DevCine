<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { customerApi } from '@/api/admin/index'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()
const customers = ref([])
const isLoading = ref(false)
const error = ref('')
const searchQuery = ref('')
let searchTimer = null

const tierStyle = (tier) => {
  const t = (tier || 'BRONZE').toUpperCase()
  return {
    PLATINUM: 'bg-sky-500/15 text-sky-300 border-sky-500/30',
    GOLD: 'bg-amber-500/15 text-amber-400 border-amber-500/30',
    SILVER: 'bg-slate-400/15 text-slate-300 border-slate-400/30',
    BRONZE: 'bg-amber-800/15 text-amber-600 border-amber-800/30'
  }[t] || 'bg-white/10 text-on-surface-variant border-white/10'
}

const formatDate = (iso) => {
  if (!iso) return '—'
  const d = new Date(iso)
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

const fetchCustomers = async () => {
  isLoading.value = true
  error.value = ''
  try {
    const { data } = await customerApi.list(searchQuery.value)
    customers.value = data.data ?? data
  } catch (err) {
    error.value = friendlyError(err, 'Không thể tải danh sách khách hàng.')
    customers.value = []
    toast.error(error.value)
  } finally {
    isLoading.value = false
  }
}

const handleSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(fetchCustomers, 400)
}

onMounted(fetchCustomers)
onUnmounted(() => { if (searchTimer) clearTimeout(searchTimer) })
</script>

<template>
  <div class="p-10 space-y-8">
    <header class="flex justify-between items-end flex-wrap gap-4">
      <div>
        <h1 class="text-4xl font-extrabold tracking-tight font-headline uppercase italic text-primary">Khách hàng</h1>
        <p class="text-on-surface-variant text-sm mt-1 uppercase tracking-widest font-bold">Danh sách thành viên, hạng & điểm tích luỹ</p>
      </div>
      <div class="relative w-full md:w-80">
        <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-xl">search</span>
        <input
          v-model="searchQuery"
          @input="handleSearchInput"
          type="text"
          placeholder="Tìm theo tên, email, SĐT..."
          class="w-full bg-surface-container-highest border-none rounded-lg pl-10 pr-4 py-2.5 text-sm text-on-surface focus:ring-1 focus:ring-primary outline-none"
        />
      </div>
    </header>

    <!-- Loading -->
    <div v-if="isLoading" class="space-y-3">
      <div v-for="i in 6" :key="i" class="h-16 bg-surface-container-low rounded-xl animate-pulse border border-white/5"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="p-6 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm flex items-center gap-2">
      <span class="material-symbols-outlined">error</span> {{ error }}
    </div>

    <!-- Empty -->
    <div v-else-if="customers.length === 0" class="py-24 text-center border border-dashed border-white/10 rounded-xl">
      <span class="material-symbols-outlined text-5xl text-neutral-600 mb-4">group_off</span>
      <p class="text-neutral-400 font-semibold">Không tìm thấy khách hàng nào.</p>
    </div>

    <!-- Table -->
    <div v-else class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden shadow-xl">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-surface-container-highest/50 border-b border-outline-variant/10">
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Khách hàng</th>
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Liên hệ</th>
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant text-center">Hạng</th>
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant text-right">Điểm</th>
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant text-right">Tham gia</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in customers" :key="c.userId" class="border-b border-outline-variant/5 last:border-0 hover:bg-white/5 transition-colors">
              <td class="p-4">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-full bg-primary/20 border border-primary/30 flex items-center justify-center shrink-0">
                    <span class="material-symbols-outlined text-primary text-lg">person</span>
                  </div>
                  <div>
                    <p class="text-sm font-bold text-on-surface">{{ c.fullName || 'Khách hàng' }}</p>
                    <p class="text-[10px] text-on-surface-variant font-mono">ID #DC-{{ c.userId }}</p>
                  </div>
                </div>
              </td>
              <td class="p-4">
                <p class="text-xs text-on-surface">{{ c.email || '—' }}</p>
                <p class="text-[11px] text-on-surface-variant">{{ c.phone || 'Chưa cập nhật' }}</p>
              </td>
              <td class="p-4 text-center">
                <span :class="tierStyle(c.membershipTier)" class="text-[9px] font-bold uppercase tracking-widest px-2.5 py-1 rounded-md border inline-block">{{ c.membershipTier || 'BRONZE' }}</span>
              </td>
              <td class="p-4 text-right">
                <span class="text-sm font-mono font-bold text-primary-container">{{ (c.loyaltyPoints || 0).toLocaleString('vi-VN') }}</span>
              </td>
              <td class="p-4 text-right text-xs text-on-surface-variant">{{ formatDate(c.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="p-4 bg-surface-container-highest/30 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
        Tổng: {{ customers.length }} khách hàng
      </div>
    </div>
  </div>
</template>
