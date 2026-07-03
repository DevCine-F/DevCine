<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const goBack = () => {
  if (window.history.length > 2) {
    router.back()
  } else {
    router.push('/admin')
  }
}

const goHome = () => {
  router.push('/admin')
}
</script>

<template>
  <div class="h-full w-full flex items-center justify-center bg-surface p-8">
    <div class="max-w-md w-full text-center space-y-6">
      <div class="relative w-48 h-48 mx-auto mb-8">
        <div class="absolute inset-0 bg-red-500/20 rounded-full animate-ping"></div>
        <div class="absolute inset-4 bg-surface rounded-full flex items-center justify-center border border-red-500/30 backdrop-blur-sm z-10 shadow-[0_0_40px_rgba(239,68,68,0.2)]">
          <span class="material-symbols-outlined text-red-500 text-7xl animate-pulse">gpp_maybe</span>
        </div>
      </div>
      
      <div>
        <h1 class="text-4xl font-black text-on-surface tracking-tight mb-2">403</h1>
        <h2 class="text-xl font-bold text-red-400 mb-4 uppercase tracking-widest">Truy cập bị từ chối</h2>
        <p class="text-on-surface-variant text-sm leading-relaxed">
          Tài khoản của bạn (<span class="font-bold text-primary">{{ authStore.user?.username }}</span>) không có quyền truy cập vào chức năng này. Vui lòng liên hệ quản trị viên nếu bạn nghĩ đây là một sự nhầm lẫn.
        </p>
      </div>

      <div class="flex items-center justify-center gap-4 pt-4">
        <button @click="goBack" class="px-6 py-2.5 rounded-lg border border-outline-variant/30 text-on-surface hover:bg-white/5 font-semibold text-sm transition-all flex items-center gap-2">
          <span class="material-symbols-outlined text-sm">arrow_back</span>
          Quay lại
        </button>
        <button @click="goHome" class="px-6 py-2.5 rounded-lg bg-primary text-on-primary hover:bg-primary/90 font-bold text-sm shadow-[0_0_20px_rgba(245,197,24,0.3)] hover:shadow-[0_0_25px_rgba(245,197,24,0.4)] transition-all flex items-center gap-2">
          <span class="material-symbols-outlined text-sm">home</span>
          Về trang chủ
        </button>
      </div>
    </div>
  </div>
</template>
