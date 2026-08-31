<script setup>
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const handleLogout = () => {
  authStore.logout()
  router.push('/')
}
</script>

<template>
  <main class="pt-24 sm:pt-28 pb-16 sm:pb-20 max-w-[1440px] mx-auto px-4 sm:px-6 md:px-10">
    <!-- Header Title -->
    <div class="mb-6 sm:mb-10">
      <h1 class="text-2xl sm:text-3xl font-extrabold tracking-tight uppercase font-headline">Tài khoản của tôi</h1>
    </div>
    
    <!-- Layout Container with Sidebar -->
    <div class="flex flex-col lg:flex-row gap-6 sm:gap-10">
      <!-- Sidebar Navigation -->
      <aside class="w-full lg:w-64 shrink-0">
        <nav class="flex flex-row lg:flex-col gap-1.5 sm:gap-2 overflow-x-auto no-scrollbar pb-2 lg:pb-0 touch-pan-x">
          <router-link to="/profile" exact-active-class="bg-primary-container/10 !text-primary-container border-primary-container font-bold" class="flex items-center gap-2 sm:gap-3 px-3.5 sm:px-4 py-2.5 sm:py-3 text-neutral-400 hover:bg-white/5 hover:text-on-surface transition-colors text-xs sm:text-sm rounded-xl lg:rounded-none lg:border-l-4 lg:border-transparent shrink-0">
            <span class="material-symbols-outlined text-lg sm:text-xl">person</span>
            <span class="whitespace-nowrap">Thông tin cá nhân</span>
          </router-link>
          <router-link to="/profile/history" active-class="bg-primary-container/10 !text-primary-container border-primary-container font-bold" class="flex items-center gap-2 sm:gap-3 px-3.5 sm:px-4 py-2.5 sm:py-3 text-neutral-400 hover:bg-white/5 hover:text-on-surface transition-colors text-xs sm:text-sm rounded-xl lg:rounded-none lg:border-l-4 lg:border-transparent shrink-0">
            <span class="material-symbols-outlined text-lg sm:text-xl">confirmation_number</span>
            <span class="whitespace-nowrap">Lịch sử đặt vé</span>
          </router-link>
          <router-link to="/profile/vouchers" active-class="bg-primary-container/10 !text-primary-container border-primary-container font-bold" class="flex items-center gap-2 sm:gap-3 px-3.5 sm:px-4 py-2.5 sm:py-3 text-neutral-400 hover:bg-white/5 hover:text-on-surface transition-colors text-xs sm:text-sm rounded-xl lg:rounded-none lg:border-l-4 lg:border-transparent shrink-0">
            <span class="material-symbols-outlined text-lg sm:text-xl">card_giftcard</span>
            <span class="whitespace-nowrap">Ưu đãi của tôi</span>
          </router-link>
          <router-link to="/profile/password" active-class="bg-primary-container/10 !text-primary-container border-primary-container font-bold" class="flex items-center gap-2 sm:gap-3 px-3.5 sm:px-4 py-2.5 sm:py-3 text-neutral-400 hover:bg-white/5 hover:text-on-surface transition-colors text-xs sm:text-sm rounded-xl lg:rounded-none lg:border-l-4 lg:border-transparent shrink-0">
            <span class="material-symbols-outlined text-lg sm:text-xl">lock_reset</span>
            <span class="whitespace-nowrap">Đổi mật khẩu</span>
          </router-link>
          <hr class="hidden lg:block my-2 border-white/5"/>
          <button type="button" @click="handleLogout" class="flex items-center gap-2 sm:gap-3 px-3.5 sm:px-4 py-2.5 sm:py-3 text-error hover:bg-error/5 transition-colors text-xs sm:text-sm rounded-xl lg:rounded-none lg:border-l-4 lg:border-transparent text-left shrink-0 lg:w-full">
            <span class="material-symbols-outlined text-lg sm:text-xl">logout</span>
            <span class="whitespace-nowrap">Đăng xuất</span>
          </button>
        </nav>
      </aside>
      
      <!-- Main Content Area -->
      <div class="flex-grow min-w-0">
        <router-view></router-view>
      </div>
    </div>
  </main>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>
