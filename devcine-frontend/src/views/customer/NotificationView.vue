<script setup>
import { ref, computed, onMounted } from 'vue'
import { useNotificationStore } from '@/stores/notifications'
import { useAuthStore } from '@/stores/auth'

const notificationStore = useNotificationStore()
const authStore = useAuthStore()
const activeFilter = ref('all')

const filters = [
  { id: 'all', label: 'Tất cả' },
  { id: 'booking', label: 'Đặt vé' },
  { id: 'promotion', label: 'Khuyến mãi' },
  { id: 'reminder', label: 'Nhắc nhở' },
  { id: 'system', label: 'Hệ thống' }
]

const filtered = computed(() => {
  if (activeFilter.value === 'all') return notificationStore.sortedNotifications
  return notificationStore.sortedNotifications.filter(n => n.type === activeFilter.value)
})

const typeIcon = (type) => ({
  booking: 'confirmation_number',
  promotion: 'local_activity',
  reminder: 'schedule',
  system: 'info'
}[type] || 'notifications')

const typeColor = (type) => ({
  booking: 'text-green-400 bg-green-400/10',
  promotion: 'text-primary-container bg-primary-container/10',
  reminder: 'text-blue-400 bg-blue-400/10',
  system: 'text-neutral-400 bg-white/5'
}[type] || 'text-neutral-400 bg-white/5')

onMounted(() => {
  if (authStore.isAuthenticated) notificationStore.fetch()
})
</script>

<template>
  <main class="min-h-screen pt-28 sm:pt-32 pb-16 sm:pb-20 px-4 sm:px-6 md:px-10 max-w-[1000px] mx-auto">
    <!-- Header -->
    <header class="mb-6 sm:mb-10 flex flex-col sm:flex-row sm:items-end justify-between gap-4 sm:gap-6">
      <div class="space-y-1.5 sm:space-y-2">
        <span class="text-primary-container font-bold uppercase tracking-[0.2em] text-[10px] sm:text-xs">Trung tâm thông báo</span>
        <h1 class="text-3xl sm:text-4xl md:text-5xl font-headline font-extrabold tracking-tighter text-on-surface">
          Thông báo
          <span v-if="notificationStore.unreadCount > 0" class="text-xs sm:text-sm align-middle ml-2 px-2.5 sm:px-3 py-0.5 sm:py-1 rounded-full bg-primary-container text-on-primary">{{ notificationStore.unreadCount }} mới</span>
        </h1>
      </div>
      <button @click="notificationStore.markAllAsRead()" :disabled="notificationStore.unreadCount === 0"
              class="flex items-center gap-1.5 sm:gap-2 text-[11px] sm:text-xs font-bold uppercase tracking-widest text-on-surface-variant hover:text-primary transition-colors disabled:opacity-40 self-start sm:self-auto">
        <span class="material-symbols-outlined text-base sm:text-lg">done_all</span> Đánh dấu tất cả đã đọc
      </button>
    </header>

    <!-- Filters -->
    <div class="flex gap-2 mb-6 sm:mb-8 overflow-x-auto no-scrollbar pb-1 touch-pan-x">
      <button v-for="f in filters" :key="f.id" @click="activeFilter = f.id"
              :class="['px-3.5 sm:px-4 py-1.5 sm:py-2 rounded-full text-xs font-bold uppercase tracking-widest whitespace-nowrap transition-colors shrink-0',
                       activeFilter === f.id ? 'bg-primary-container text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:text-on-surface']">
        {{ f.label }}
      </button>
    </div>

    <!-- Loading -->
    <div v-if="notificationStore.loading" class="space-y-3 sm:space-y-4">
      <div v-for="i in 3" :key="i" class="h-20 sm:h-24 bg-surface-container-low rounded-xl animate-pulse border border-white/5"></div>
    </div>

    <!-- Empty -->
    <div v-else-if="filtered.length === 0" class="flex flex-col items-center justify-center py-16 sm:py-24 text-center px-4">
      <span class="material-symbols-outlined text-4xl sm:text-5xl text-outline-variant mb-4">notifications_off</span>
      <p class="text-on-surface-variant font-semibold text-sm sm:text-base">Không có thông báo nào</p>
    </div>

    <!-- List -->
    <div v-else class="space-y-3">
      <div v-for="notif in filtered" :key="notif.id"
           @click="notificationStore.markAsRead(notif.id)"
           :class="['flex gap-3 sm:gap-4 p-4 sm:p-5 rounded-xl border cursor-pointer transition-all',
                    notif.isRead ? 'bg-surface-container-low border-white/5' : 'bg-surface-container-high border-primary-container/30']">
        <div :class="['w-9 h-9 sm:w-11 sm:h-11 rounded-full flex items-center justify-center shrink-0', typeColor(notif.type)]">
          <span class="material-symbols-outlined text-lg sm:text-xl">{{ typeIcon(notif.type) }}</span>
        </div>
        <div class="flex-grow min-w-0">
          <div class="flex items-center justify-between gap-2 mb-1">
            <h3 class="font-bold text-on-surface text-xs sm:text-sm flex items-center gap-2 truncate">
              {{ notif.title }}
              <span v-if="!notif.isRead" class="w-1.5 h-1.5 sm:w-2 sm:h-2 rounded-full bg-primary-container shrink-0"></span>
            </h3>
            <span class="text-[10px] sm:text-[11px] text-on-surface-variant whitespace-nowrap shrink-0">{{ notif.time }}</span>
          </div>
          <p class="text-xs sm:text-sm text-on-surface-variant leading-relaxed">{{ notif.message }}</p>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>
