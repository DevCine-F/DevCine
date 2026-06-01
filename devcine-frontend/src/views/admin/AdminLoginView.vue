<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { useThemeStore } from '@/stores/theme'
import StarryBackground from '@/components/common/StarryBackground.vue'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const loginAsAdmin = async (e) => {
  if (e && e.preventDefault) e.preventDefault()
  
  // Trigger Warp Speed
  themeStore.triggerWarp()
  
  // Đợi hiệu ứng warp (khoảng 800ms)
  await new Promise(resolve => setTimeout(resolve, 800))
  
  const email = 'admin@devcine.com'
  const mockUser = { name: 'Admin', email }
  const mockToken = 'mock-jwt-token-' + Math.random().toString(36).substr(2)
  
  authStore.login(mockUser, mockToken, 'admin')
  router.push('/admin/dashboard')
}
</script>

<template>
  <main class="relative w-full h-screen overflow-hidden flex items-center justify-center">
    
    <!-- Starry Background -->
    <StarryBackground theme="admin" />

    <!-- Glassmorphism Login Card -->
    <div class="relative z-20 w-full max-w-md px-6">
      <div class="bg-white/5 backdrop-blur-2xl border border-white/10 rounded-3xl p-10 shadow-[0_8px_32px_0_rgba(0,0,0,0.37)]">
        
        <!-- Header -->
        <div class="text-center mb-10">
          <h1 class="text-3xl font-black tracking-tight text-white mb-2 uppercase">DevCine Admin</h1>
          <p class="text-white/60 text-sm tracking-widest uppercase">Hệ Thống Quản Trị</p>
        </div>

        <!-- Form -->
        <form @submit.prevent="loginAsAdmin" class="space-y-8">
          
          <!-- Email Input -->
          <div class="relative z-0 w-full group">
            <input type="email" name="email" id="admin_email" class="block py-3 px-0 w-full text-base text-white bg-transparent border-0 border-b border-white/30 appearance-none focus:outline-none focus:ring-0 focus:border-[#f5c518] peer transition-colors" placeholder=" " required />
            <label for="admin_email" class="peer-focus:font-bold absolute text-base text-white/50 duration-300 transform -translate-y-6 scale-75 top-3 -z-10 origin-[0] peer-focus:left-0 peer-focus:text-[#f5c518] peer-placeholder-shown:scale-100 peer-placeholder-shown:translate-y-0 peer-focus:scale-75 peer-focus:-translate-y-6">Email quản trị</label>
          </div>
          
          <!-- Password Input -->
          <div class="relative z-0 w-full group">
            <input type="password" name="password" id="admin_password" class="block py-3 px-0 w-full text-base text-white bg-transparent border-0 border-b border-white/30 appearance-none focus:outline-none focus:ring-0 focus:border-[#f5c518] peer transition-colors" placeholder=" " required />
            <label for="admin_password" class="peer-focus:font-bold absolute text-base text-white/50 duration-300 transform -translate-y-6 scale-75 top-3 -z-10 origin-[0] peer-focus:left-0 peer-focus:text-[#f5c518] peer-placeholder-shown:scale-100 peer-placeholder-shown:translate-y-0 peer-focus:scale-75 peer-focus:-translate-y-6">Mật khẩu</label>
          </div>

          <div class="flex justify-between items-center pt-2">
            <div class="flex items-center gap-2">
              <input type="checkbox" id="remember" class="w-4 h-4 rounded border-white/30 bg-white/10 text-[#f5c518] focus:ring-0 focus:ring-offset-0 cursor-pointer" />
              <label for="remember" class="text-xs text-white/60 cursor-pointer hover:text-white transition-colors">Ghi nhớ</label>
            </div>
            <a href="#" class="text-xs text-white/60 hover:text-[#f5c518] transition-colors">Quên mật khẩu?</a>
          </div>

          <!-- Submit Button -->
          <button type="submit" class="w-full bg-[#f5c518] text-black py-4 mt-4 rounded-xl font-extrabold uppercase tracking-[0.2em] text-sm hover:bg-white hover:shadow-[0_0_20px_rgba(245,197,24,0.4)] transition-all duration-300">
            Đăng Nhập
          </button>
        </form>

        <!-- Quick Access -->
        <div class="mt-8 pt-6 border-t border-white/10 text-center">
          <button type="button" @click="loginAsAdmin" class="text-[10px] text-white/40 uppercase tracking-widest hover:text-white transition-colors border border-white/10 px-4 py-2 rounded-full hover:bg-white/10">
            [Môi trường thử nghiệm] Truy cập nhanh
          </button>
        </div>
        
      </div>
      
      <!-- Footer text -->
      <div class="mt-8 text-center">
        <p class="text-white/30 text-[10px] uppercase tracking-widest">© 2026 DevCine. Protected System.</p>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* Remove default autocomplete styling in webkit */
input:-webkit-autofill,
input:-webkit-autofill:hover, 
input:-webkit-autofill:focus, 
input:-webkit-autofill:active{
    -webkit-box-shadow: 0 0 0 30px transparent inset !important;
    -webkit-text-fill-color: white !important;
    transition: background-color 5000s ease-in-out 0s;
}
</style>
