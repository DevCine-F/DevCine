<script setup>
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import logo from '../../assets/images/Logo_DevCine_Ngang_XoaNen.png'

const router = useRouter()
const route = useRoute()
const isMobileMenuOpen = ref(false)
const authStore = useAuthStore()

// Tự động đóng mobile menu khi chuyển trang
watch(() => route.fullPath, () => {
  isMobileMenuOpen.value = false
})

const handleLogout = () => {
  authStore.logout()
  isMobileMenuOpen.value = false
  router.push('/')
}
</script>

<template>
  <header class="fixed top-0 w-full z-50 bg-neutral-950/70 backdrop-blur-xl shadow-2xl shadow-black/40 border-b border-white/5">
    <nav class="flex justify-between items-center px-4 sm:px-6 md:px-10 py-1.5 max-w-[1440px] mx-auto">
      <div class="flex items-center gap-3">
        <!-- Mobile Menu Toggle Button -->
        <button
          @click="isMobileMenuOpen = !isMobileMenuOpen"
          type="button"
          aria-label="Menu"
          class="md:hidden w-10 h-10 flex items-center justify-center rounded-xl text-neutral-300 hover:text-[#f5c518] hover:bg-white/5 transition-colors"
        >
          <span class="material-symbols-outlined text-[26px]">
            menu
          </span>
        </button>

        <router-link to="/" class="flex items-center gap-2 group">
          <img :src="logo" alt="DEVCINE" class="h-10 sm:h-12 w-auto object-contain brightness-110 group-hover:scale-105 transition-transform duration-300">
        </router-link>
      </div>
      
      <!-- Desktop Navigation Links -->
      <div class="hidden md:flex items-center space-x-6 lg:space-x-8 font-headline font-bold tracking-tight text-sm lg:text-base">
        <router-link to="/" class="text-neutral-400 hover:text-white transition-colors relative after:absolute after:bottom-[-6px] after:left-1/2 after:-translate-x-1/2 after:h-[2px] after:bg-[#f5c518] after:transition-all after:duration-300 after:w-0 hover:after:w-1/2" exact-active-class="!text-[#f5c518] after:!w-full">Trang chủ</router-link>
        <router-link to="/lich-chieu" class="text-neutral-400 hover:text-white transition-colors relative after:absolute after:bottom-[-6px] after:left-1/2 after:-translate-x-1/2 after:h-[2px] after:bg-[#f5c518] after:transition-all after:duration-300 after:w-0 hover:after:w-1/2" active-class="!text-[#f5c518] after:!w-full">Lịch chiếu</router-link>
        <router-link to="/he-thong-rap" class="text-neutral-400 hover:text-white transition-colors relative after:absolute after:bottom-[-6px] after:left-1/2 after:-translate-x-1/2 after:h-[2px] after:bg-[#f5c518] after:transition-all after:duration-300 after:w-0 hover:after:w-1/2" active-class="!text-[#f5c518] after:!w-full">Cụm rạp</router-link>
        <router-link to="/khuyen-mai" class="text-neutral-400 hover:text-white transition-colors relative after:absolute after:bottom-[-6px] after:left-1/2 after:-translate-x-1/2 after:h-[2px] after:bg-[#f5c518] after:transition-all after:duration-300 after:w-0 hover:after:w-1/2" active-class="!text-[#f5c518] after:!w-full">Khuyến mãi</router-link>
        <router-link to="/faq" class="text-neutral-400 hover:text-white transition-colors relative after:absolute after:bottom-[-6px] after:left-1/2 after:-translate-x-1/2 after:h-[2px] after:bg-[#f5c518] after:transition-all after:duration-300 after:w-0 hover:after:w-1/2" active-class="!text-[#f5c518] after:!w-full">Hỗ trợ</router-link>
        <router-link v-if="authStore.isAdmin" to="/admin/movies" class="text-primary font-bold border border-primary/20 px-3 py-1 rounded text-xs hover:bg-primary/10 transition-all">ADMIN</router-link>
      </div>

      <div class="flex items-center space-x-1 sm:space-x-2">
        <!-- Action Icons Group -->
        <div class="flex items-center mr-1 sm:mr-2 pr-1 sm:pr-2 border-r border-white/10">
          <router-link to="/search" 
            class="w-9 h-9 sm:w-10 sm:h-10 flex items-center justify-center rounded-full text-neutral-400 hover:text-[#f5c518] hover:bg-white/5 transition-all duration-300"
            title="Tìm kiếm">
            <span class="material-symbols-outlined text-[20px] sm:text-[22px]">search</span>
          </router-link>
        </div>

        <!-- Auth State -->
        <div v-if="authStore.isAuthenticated" class="flex items-center pl-1 sm:pl-2">
          <div class="group relative flex items-center gap-2 sm:gap-3 bg-white/5 hover:bg-white/10 border border-white/10 px-2.5 sm:px-4 py-1.5 rounded-full transition-all duration-300 cursor-pointer">
            <div class="w-7 h-7 rounded-full bg-gradient-to-tr from-[#f5c518] to-[#ffda5c] flex items-center justify-center text-black shrink-0">
              <span class="material-symbols-outlined text-sm font-bold">person</span>
            </div>
            <div class="hidden sm:flex flex-col">
              <span class="text-[10px] text-neutral-500 font-bold uppercase tracking-widest leading-none mb-0.5">DevCine User</span>
              <span class="text-xs font-bold text-white tracking-tight leading-none truncate max-w-[120px]">
                {{ authStore.user?.fullName || authStore.user?.username || 'Thành viên' }}
              </span>
            </div>
            
            <!-- Hover Dropdown Menu -->
            <div class="absolute top-full right-0 mt-2 w-48 bg-neutral-900/95 backdrop-blur-2xl border border-white/10 rounded-xl overflow-hidden opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 translate-y-2 group-hover:translate-y-0 shadow-2xl shadow-black z-[100]">
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
        
        <div v-else class="flex items-center pl-1 sm:pl-2">
          <div class="group relative">
            <router-link to="/login" 
              class="w-9 h-9 sm:w-10 sm:h-10 flex items-center justify-center rounded-full text-neutral-400 hover:text-[#f5c518] hover:bg-white/5 transition-all duration-300">
              <span class="material-symbols-outlined text-[20px] sm:text-[22px]">login</span>
            </router-link>
            
            <div class="absolute top-full left-1/2 -translate-x-1/2 mt-2 whitespace-nowrap bg-neutral-900/95 backdrop-blur-md border border-white/10 px-3 py-1.5 rounded-lg text-[10px] font-bold text-white uppercase tracking-widest opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 translate-y-1 group-hover:translate-y-0 shadow-xl pointer-events-none z-50">
              Đăng nhập
            </div>
          </div>
        </div>
      </div>
    </nav>

    <!-- Mobile Drawer: Teleported to body for true full-screen overlay -->
    <Teleport to="body">
      <!-- Backdrop -->
      <transition name="fade">
        <div v-if="isMobileMenuOpen"
             class="fixed inset-0 bg-black/75 backdrop-blur-sm z-[998] md:hidden"
             @click="isMobileMenuOpen = false"></div>
      </transition>

      <!-- Off-canvas Sidebar sliding from the left -->
      <aside :class="[
        'fixed inset-y-0 left-0 w-72 sm:w-80 max-w-[85vw] bg-[#121212] text-white border-r border-white/10 z-[999] flex flex-col md:hidden transition-transform duration-300 ease-in-out shadow-2xl shadow-black',
        isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full'
      ]">
        <!-- Header with Logo and Close button -->
        <div class="p-5 flex justify-between items-center border-b border-white/10">
          <router-link to="/" @click="isMobileMenuOpen = false" class="flex items-center gap-2">
            <img :src="logo" alt="DEVCINE" class="h-9 w-auto object-contain brightness-110">
          </router-link>
          <button class="p-2 text-neutral-400 hover:text-white rounded-lg hover:bg-white/5 transition-colors"
                  @click="isMobileMenuOpen = false"
                  title="Đóng menu">
            <span class="material-symbols-outlined text-2xl">close</span>
          </button>
        </div>

        <!-- Navigation Links List with Icons -->
        <nav class="flex-grow px-3 py-4 space-y-1.5 overflow-y-auto">
          <div class="text-[10px] font-bold text-neutral-500 uppercase tracking-[0.2em] px-3 mb-2">Menu Khách hàng</div>

          <router-link to="/" @click="isMobileMenuOpen = false"
            class="flex items-center gap-3 px-3.5 py-3 rounded-xl text-neutral-300 hover:bg-white/5 hover:text-white transition-all group"
            exact-active-class="bg-[#f5c518]/10 text-[#f5c518] border-l-4 border-[#f5c518] font-bold">
            <span class="material-symbols-outlined text-[22px] group-hover:text-[#f5c518] transition-colors">home</span>
            <span class="font-semibold text-sm">Trang chủ</span>
          </router-link>

          <router-link to="/lich-chieu" @click="isMobileMenuOpen = false"
            class="flex items-center gap-3 px-3.5 py-3 rounded-xl text-neutral-300 hover:bg-white/5 hover:text-white transition-all group"
            active-class="bg-[#f5c518]/10 text-[#f5c518] border-l-4 border-[#f5c518] font-bold">
            <span class="material-symbols-outlined text-[22px] group-hover:text-[#f5c518] transition-colors">calendar_month</span>
            <span class="font-semibold text-sm">Lịch chiếu</span>
          </router-link>

          <router-link to="/he-thong-rap" @click="isMobileMenuOpen = false"
            class="flex items-center gap-3 px-3.5 py-3 rounded-xl text-neutral-300 hover:bg-white/5 hover:text-white transition-all group"
            active-class="bg-[#f5c518]/10 text-[#f5c518] border-l-4 border-[#f5c518] font-bold">
            <span class="material-symbols-outlined text-[22px] group-hover:text-[#f5c518] transition-colors">theater_comedy</span>
            <span class="font-semibold text-sm">Cụm rạp</span>
          </router-link>

          <router-link to="/khuyen-mai" @click="isMobileMenuOpen = false"
            class="flex items-center gap-3 px-3.5 py-3 rounded-xl text-neutral-300 hover:bg-white/5 hover:text-white transition-all group"
            active-class="bg-[#f5c518]/10 text-[#f5c518] border-l-4 border-[#f5c518] font-bold">
            <span class="material-symbols-outlined text-[22px] group-hover:text-[#f5c518] transition-colors">local_activity</span>
            <span class="font-semibold text-sm">Khuyến mãi</span>
          </router-link>

          <router-link to="/faq" @click="isMobileMenuOpen = false"
            class="flex items-center gap-3 px-3.5 py-3 rounded-xl text-neutral-300 hover:bg-white/5 hover:text-white transition-all group"
            active-class="bg-[#f5c518]/10 text-[#f5c518] border-l-4 border-[#f5c518] font-bold">
            <span class="material-symbols-outlined text-[22px] group-hover:text-[#f5c518] transition-colors">help_outline</span>
            <span class="font-semibold text-sm">Hỗ trợ</span>
          </router-link>

          <div v-if="authStore.isAdmin" class="pt-3 border-t border-white/10 mt-3">
            <div class="text-[10px] font-bold text-neutral-500 uppercase tracking-[0.2em] px-3 mb-2">Hệ thống</div>
            <router-link to="/admin/dashboard" @click="isMobileMenuOpen = false"
              class="flex items-center justify-between px-3.5 py-3 rounded-xl text-[#f5c518] hover:bg-[#f5c518]/10 transition-all font-semibold text-sm border border-[#f5c518]/20">
              <span class="flex items-center gap-3">
                <span class="material-symbols-outlined text-[22px]">admin_panel_settings</span>
                <span>Trang Quản trị</span>
              </span>
              <span class="material-symbols-outlined text-sm">arrow_forward</span>
            </router-link>
          </div>
        </nav>

        <!-- Bottom User / Auth Section -->
        <div class="p-4 border-t border-white/10 bg-black/40">
          <div v-if="authStore.isAuthenticated" class="space-y-3">
            <router-link to="/profile" @click="isMobileMenuOpen = false" class="flex items-center gap-3 p-2 rounded-xl hover:bg-white/5 transition-colors">
              <div class="w-10 h-10 rounded-full bg-gradient-to-tr from-[#f5c518] to-[#ffda5c] flex items-center justify-center text-black shrink-0">
                <span class="material-symbols-outlined text-lg font-bold">person</span>
              </div>
              <div class="flex-grow min-w-0">
                <p class="text-xs font-bold text-white truncate">{{ authStore.user?.fullName || authStore.user?.username || 'Thành viên' }}</p>
                <p class="text-[10px] text-[#f5c518] uppercase tracking-wider font-semibold">Tài khoản của tôi</p>
              </div>
            </router-link>
            <button @click="handleLogout" class="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl border border-red-500/30 text-red-400 hover:bg-red-500/10 text-xs font-bold uppercase tracking-wider transition-all">
              <span class="material-symbols-outlined text-base">logout</span>
              <span>Đăng xuất</span>
            </button>
          </div>

          <div v-else class="space-y-2">
            <router-link to="/login" @click="isMobileMenuOpen = false" class="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-primary-container text-on-primary font-bold text-xs uppercase tracking-widest hover:brightness-110 transition-all">
              <span class="material-symbols-outlined text-base">login</span>
              <span>Đăng nhập / Đăng ký</span>
            </router-link>
          </div>
        </div>
      </aside>
    </Teleport>
  </header>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
