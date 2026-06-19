<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { useNotificationStore } from '../../stores/notifications'
import { useTheme } from '../../composables/useTheme'
import AppModal from '../common/AppModal.vue'
import logo from '../../assets/images/Logo_DevCine_Ngang_XoaNen.png'

const router = useRouter()
const isSearchOpen = ref(false)
const { isLightMode, toggleTheme } = useTheme()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

// Tải thông báo khi đã đăng nhập (và mỗi khi trạng thái đăng nhập thay đổi)
onMounted(() => {
  if (authStore.isAuthenticated) notificationStore.fetch()
})
watch(() => authStore.isAuthenticated, (loggedIn) => {
  if (loggedIn) notificationStore.fetch()
  else notificationStore.notifications = []
})

const isNotifModalOpen = ref(false)
const selectedNotif = ref(null)

const handleLogout = () => {
  authStore.logout()
  router.push('/')
}

const viewNotification = (notif) => {
  selectedNotif.value = notif
  isNotifModalOpen.value = true
  notificationStore.markAsRead(notif.id)
}
</script>

<template>
  <header class="fixed top-0 w-full z-50 bg-neutral-950/60 backdrop-blur-xl shadow-2xl shadow-black/40">
    <nav class="flex justify-between items-center px-10 py-1 max-w-[1440px] mx-auto">
      <router-link to="/" class="flex items-center gap-2 group">
        <img :src="logo" alt="DEVCINE" class="h-12 w-auto object-contain brightness-110 group-hover:scale-105 transition-transform duration-300">
      </router-link>
      
      <div class="hidden md:flex items-center space-x-8 font-headline font-bold tracking-tight">
        <router-link to="/" class="text-neutral-400 hover:text-white transition-colors relative after:absolute after:bottom-[-6px] after:left-1/2 after:-translate-x-1/2 after:h-[2px] after:bg-[#f5c518] after:transition-all after:duration-300 after:w-0 hover:after:w-1/2" exact-active-class="!text-[#f5c518] after:!w-full">Trang chủ</router-link>
        <router-link to="/lich-chieu" class="text-neutral-400 hover:text-white transition-colors relative after:absolute after:bottom-[-6px] after:left-1/2 after:-translate-x-1/2 after:h-[2px] after:bg-[#f5c518] after:transition-all after:duration-300 after:w-0 hover:after:w-1/2" active-class="!text-[#f5c518] after:!w-full">Lịch chiếu</router-link>
        <router-link to="/he-thong-rap" class="text-neutral-400 hover:text-white transition-colors relative after:absolute after:bottom-[-6px] after:left-1/2 after:-translate-x-1/2 after:h-[2px] after:bg-[#f5c518] after:transition-all after:duration-300 after:w-0 hover:after:w-1/2" active-class="!text-[#f5c518] after:!w-full">Cụm rạp</router-link>
        <router-link to="/khuyen-mai" class="text-neutral-400 hover:text-white transition-colors relative after:absolute after:bottom-[-6px] after:left-1/2 after:-translate-x-1/2 after:h-[2px] after:bg-[#f5c518] after:transition-all after:duration-300 after:w-0 hover:after:w-1/2" active-class="!text-[#f5c518] after:!w-full">Khuyến mãi</router-link>
        <router-link to="/faq" class="text-neutral-400 hover:text-white transition-colors relative after:absolute after:bottom-[-6px] after:left-1/2 after:-translate-x-1/2 after:h-[2px] after:bg-[#f5c518] after:transition-all after:duration-300 after:w-0 hover:after:w-1/2" active-class="!text-[#f5c518] after:!w-full">Hỗ trợ</router-link>
        <router-link v-if="authStore.isAdmin" to="/admin/movies" class="text-primary font-bold border border-primary/20 px-3 py-1 rounded text-xs hover:bg-primary/10 transition-all">ADMIN</router-link>
      </div>

      <div class="flex items-center space-x-2">
        <!-- Action Icons Group -->
        <div class="flex items-center mr-2 pr-2 border-r border-white/10">
          <router-link to="/search" 
            class="w-10 h-10 flex items-center justify-center rounded-full text-neutral-400 hover:text-[#f5c518] hover:bg-white/5 transition-all duration-300">
            <span class="material-symbols-outlined text-[22px]">search</span>
          </router-link>
          
          <button @click="toggleTheme" 
            class="w-10 h-10 flex items-center justify-center rounded-full text-neutral-400 hover:text-[#f5c518] hover:bg-white/5 transition-all duration-300">
            <span class="material-symbols-outlined text-[22px]">{{ isLightMode ? 'dark_mode' : 'light_mode' }}</span>
          </button>

          <!-- Notifications Dropdown -->
          <div class="group relative">
            <button class="w-10 h-10 flex items-center justify-center rounded-full text-neutral-400 hover:text-[#f5c518] hover:bg-white/5 transition-all duration-300 relative">
              <span class="material-symbols-outlined text-[22px]">notifications</span>
              <span v-if="notificationStore.unreadCount > 0" 
                    class="absolute top-2.5 right-2.5 w-2 h-2 bg-[#f5c518] rounded-full border-2 border-neutral-900 animate-pulse"></span>
            </button>

            <!-- Dropdown Menu -->
            <div class="absolute top-full right-0 mt-3 w-80 bg-neutral-900/98 backdrop-blur-2xl border border-white/10 rounded-2xl overflow-hidden opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 translate-y-2 group-hover:translate-y-0 shadow-[0_20px_50px_rgba(0,0,0,0.5)] z-[100]">
              <div class="px-5 py-4 border-b border-white/5 flex justify-between items-center bg-white/[0.02]">
                <h3 class="text-xs font-black uppercase tracking-widest text-white font-headline">Thông báo</h3>
                <button @click="notificationStore.markAllAsRead()" 
                        class="text-[10px] font-bold text-[#f5c518] hover:underline uppercase tracking-tighter">
                  Đánh dấu đã đọc
                </button>
              </div>

              <div class="max-h-[360px] overflow-y-auto custom-scrollbar">
                <div v-if="notificationStore.notifications.length === 0" class="p-8 text-center">
                  <span class="material-symbols-outlined text-neutral-600 text-4xl mb-2">notifications_off</span>
                  <p class="text-xs text-neutral-500 font-medium">Bạn không có thông báo nào</p>
                </div>
                
                <div v-for="notif in notificationStore.sortedNotifications" :key="notif.id"
                     @click="viewNotification(notif)"
                     :class="['px-5 py-4 border-b border-white/5 cursor-pointer transition-all hover:bg-white/[0.03]', !notif.isRead ? 'bg-primary/5' : 'opacity-60']">
                  <div class="flex gap-3">
                    <div :class="['w-8 h-8 rounded-full flex items-center justify-center shrink-0', 
                                  notif.type === 'booking' ? 'bg-green-500/20 text-green-500' : 
                                  notif.type === 'promotion' ? 'bg-[#f5c518]/20 text-[#f5c518]' : 'bg-blue-500/20 text-blue-500']">
                      <span class="material-symbols-outlined text-lg">
                        {{ notif.type === 'booking' ? 'confirmation_number' : notif.type === 'promotion' ? 'sell' : 'info' }}
                      </span>
                    </div>
                    <div class="flex-grow">
                      <div class="flex justify-between items-start mb-1">
                        <span class="text-xs font-bold text-white tracking-tight leading-none font-headline">{{ notif.title }}</span>
                        <span class="text-[9px] font-medium text-neutral-500 uppercase">{{ notif.time }}</span>
                      </div>
                      <p class="text-[11px] text-neutral-400 leading-snug line-clamp-2">{{ notif.message }}</p>
                      <div v-if="!notif.isRead" class="mt-2 w-1.5 h-1.5 bg-[#f5c518] rounded-full"></div>
                    </div>
                  </div>
                </div>
              </div>

              <router-link to="/notifications" class="block w-full py-3 text-center bg-white/[0.02] hover:bg-white/[0.05] transition-all">
                <span class="text-[10px] font-black uppercase tracking-[0.2em] text-neutral-400 hover:text-white font-headline">Xem tất cả thông báo</span>
              </router-link>
            </div>
          </div>
        </div>

        <!-- Auth State -->
        <div v-if="authStore.isAuthenticated" class="flex items-center pl-2">
          <div class="group relative flex items-center gap-3 bg-white/5 hover:bg-white/10 border border-white/10 px-4 py-1.5 rounded-full transition-all duration-300 cursor-pointer">
            <div class="w-7 h-7 rounded-full bg-gradient-to-tr from-[#f5c518] to-[#ffda5c] flex items-center justify-center text-black">
              <span class="material-symbols-outlined text-sm font-bold">person</span>
            </div>
            <div class="flex flex-col">
              <span class="text-[10px] text-neutral-500 font-bold uppercase tracking-widest leading-none mb-0.5">Prestige User</span>
              <span class="text-xs font-bold text-white tracking-tight leading-none truncate max-w-[80px]">
                {{ authStore.user?.name || 'Thành viên' }}
              </span>
            </div>
            
            <!-- Hover Dropdown Menu (Simplified) -->
            <div class="absolute top-full right-0 mt-2 w-48 bg-neutral-900/95 backdrop-blur-2xl border border-white/10 rounded-xl overflow-hidden opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 translate-y-2 group-hover:translate-y-0 shadow-2xl shadow-black">
              <router-link to="/profile" class="flex items-center gap-3 px-4 py-3 hover:bg-white/5 transition-colors">
                <span class="material-symbols-outlined text-lg text-neutral-400">account_circle</span>
                <span class="text-xs font-bold">Tài khoản</span>
              </router-link>
              <div class="h-[1px] bg-white/5"></div>
              <button @click="handleLogout" class="w-full flex items-center gap-3 px-4 py-3 hover:bg-red-500/10 text-red-400 transition-colors">
                <span class="material-symbols-outlined text-lg">logout</span>
                <span class="text-xs font-bold">Đăng xuất</span>
              </button>
            </div>
          </div>
        </div>
        
        <div v-else class="flex items-center pl-2">
          <div class="group relative">
            <router-link to="/login" 
              class="w-10 h-10 flex items-center justify-center rounded-full text-neutral-400 hover:text-[#f5c518] hover:bg-white/5 transition-all duration-300">
              <span class="material-symbols-outlined text-[22px]">login</span>
            </router-link>
            
            <div class="absolute top-full left-1/2 -translate-x-1/2 mt-2 whitespace-nowrap bg-neutral-900/95 backdrop-blur-md border border-white/10 px-3 py-1.5 rounded-lg text-[10px] font-bold text-white uppercase tracking-widest opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 translate-y-1 group-hover:translate-y-0 shadow-xl pointer-events-none z-50">
              Đăng nhập
            </div>
          </div>
        </div>
      </div>
    </nav>

    <!-- Notification Detail Modal -->
    <AppModal :show="isNotifModalOpen" :title="selectedNotif?.title" @close="isNotifModalOpen = false">
      <div v-if="selectedNotif" class="flex flex-col items-center text-center py-4">
        <div :class="['w-20 h-20 rounded-full flex items-center justify-center mb-6 shadow-2xl', 
                      selectedNotif.type === 'booking' ? 'bg-green-500/10 text-green-500 shadow-green-500/20' : 
                      selectedNotif.type === 'promotion' ? 'bg-[#f5c518]/10 text-[#f5c518] shadow-[#f5c518]/20' : 'bg-blue-500/10 text-blue-500 shadow-blue-500/20']">
          <span class="material-symbols-outlined text-4xl">
            {{ selectedNotif.type === 'booking' ? 'confirmation_number' : selectedNotif.type === 'promotion' ? 'sell' : 'info' }}
          </span>
        </div>
        
        <h4 class="text-2xl font-black text-white mb-2 tracking-tight uppercase">{{ selectedNotif.title }}</h4>
        <p class="text-neutral-400 font-medium mb-8 max-w-sm leading-relaxed">{{ selectedNotif.message }}</p>
        
        <div class="w-full grid grid-cols-2 gap-4">
          <button @click="isNotifModalOpen = false" class="py-4 bg-white/5 hover:bg-white/10 rounded-xl text-xs font-black uppercase tracking-widest text-neutral-400 transition-all">
            Đóng
          </button>
          <router-link :to="selectedNotif.type === 'booking' ? '/profile' : '/khuyen-mai'" 
                       @click="isNotifModalOpen = false"
                       class="py-4 bg-[#f5c518] hover:bg-[#ffda5c] rounded-xl text-xs font-black uppercase tracking-widest text-black shadow-xl shadow-[#f5c518]/20 transition-all">
            {{ selectedNotif.type === 'booking' ? 'Xem vé của tôi' : 'Xem chi tiết' }}
          </router-link>
        </div>
        
        <p class="mt-8 text-[10px] font-bold text-neutral-600 uppercase tracking-widest">Nhận vào lúc {{ selectedNotif.time }}</p>
      </div>
    </AppModal>
  </header>
</template>

<style scoped>

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(245, 197, 24, 0.2);
  border-radius: 10px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(245, 197, 24, 0.5);
}
</style>
