<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useToastStore } from '@/stores/toast'
import StarryBackground from '@/components/common/StarryBackground.vue'
import AppToast from '@/components/common/AppToast.vue'
import { authApi } from '@/api/customer/index'
import adminRoutes from '@/routers/admin'
import { resolveFirstAccessibleAdminPath } from '@/utils/adminAccess'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const toast = useToastStore()

// Token còn hạn? (giải mã exp trong JWT)
const isTokenValid = (token) => {
  if (!token) return false
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return !payload.exp || payload.exp * 1000 > Date.now()
  } catch {
    return false
  }
}

// Đã đăng nhập (admin/staff) + phiên còn hợp lệ -> vào màn nội bộ đầu tiên được phép xem
onMounted(async () => {
  if (authStore.isAuthenticated
      && (authStore.role === 'admin' || authStore.role === 'staff')
      && isTokenValid(authStore.token)) {
    try {
      await authStore.fetchPermissions()
      router.replace(resolveFirstAccessibleAdminPath(adminRoutes, authStore))
    } catch {
      authStore.logout()
    }
    return
  }
  // Ghi nhớ: tự điền tên đăng nhập đã lưu lần trước
  const saved = localStorage.getItem(REMEMBER_KEY)
  if (saved) {
    username.value = saved
    rememberMe.value = true
  }
})

const REMEMBER_KEY = 'admin_remember_username'

const username = ref('')
const password = ref('')
const showPassword = ref(false)
const isLoading = ref(false)
const rememberMe = ref(false)

const loginAsAdmin = async (e) => {
  if (e && e.preventDefault) e.preventDefault()

  const user = username.value || 'admin'
  const pass = password.value || '123'

  isLoading.value = true
  try {
    const res = await authApi.login(user, pass)
    const { token, user: userData } = res.data.data
    const role = userData.role.toLowerCase()

    if (role !== 'admin' && role !== 'staff') {
      toast.error('Tài khoản không có quyền truy cập hệ thống quản trị.')
      return
    }

    // Ghi nhớ tên đăng nhập cho lần sau (hoặc xoá nếu bỏ chọn)
    if (rememberMe.value) localStorage.setItem(REMEMBER_KEY, user)
    else localStorage.removeItem(REMEMBER_KEY)

    themeStore.triggerWarp()
    await new Promise(resolve => setTimeout(resolve, 800))

    authStore.login({ id: userData.id, username: userData.username, email: userData.email, fullName: userData.fullName }, token, role)
    try {
      await authStore.fetchPermissions(true)
    } catch {
      authStore.logout()
      toast.error('Đăng nhập thành công nhưng chưa tải được quyền nội bộ. Vui lòng khởi động lại backend và thử lại.')
      return
    }

    toast.success('Đăng nhập thành công! Chào mừng bạn đã trở lại.')
    router.push(resolveFirstAccessibleAdminPath(adminRoutes, authStore))
  } catch (err) {
    toast.error(err.response?.data?.message || 'Số điện thoại/email hoặc mật khẩu không đúng.')
  } finally {
    isLoading.value = false
  }
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

          <!-- Username Input -->
          <div class="relative z-0 w-full group">
            <input v-model="username" type="text" id="admin_username" class="block py-3 px-0 w-full text-base text-white bg-transparent border-0 border-b border-white/30 appearance-none focus:outline-none focus:ring-0 focus:border-[#f5c518] peer transition-colors" placeholder=" " required />
            <label for="admin_username" class="peer-focus:font-bold absolute text-base text-white/50 duration-300 transform -translate-y-6 scale-75 top-3 -z-10 origin-[0] peer-focus:left-0 peer-focus:text-[#f5c518] peer-placeholder-shown:scale-100 peer-placeholder-shown:translate-y-0 peer-focus:scale-75 peer-focus:-translate-y-6">Tên đăng nhập</label>
          </div>

          <!-- Password Input -->
          <div class="relative z-0 w-full group">
            <input v-model="password" :type="showPassword ? 'text' : 'password'" id="admin_password" class="block py-3 px-0 pr-10 w-full text-base text-white bg-transparent border-0 border-b border-white/30 appearance-none focus:outline-none focus:ring-0 focus:border-[#f5c518] peer transition-colors" placeholder=" " required />
            <label for="admin_password" class="peer-focus:font-bold absolute text-base text-white/50 duration-300 transform -translate-y-6 scale-75 top-3 -z-10 origin-[0] peer-focus:left-0 peer-focus:text-[#f5c518] peer-placeholder-shown:scale-100 peer-placeholder-shown:translate-y-0 peer-focus:scale-75 peer-focus:-translate-y-6">Mật khẩu</label>
            <button type="button" @click="showPassword = !showPassword" :aria-label="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                    class="absolute right-0 top-2.5 text-white/40 hover:text-[#f5c518] transition-colors p-1">
              <span class="material-symbols-outlined text-xl">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
            </button>
          </div>

          <div class="flex items-center pt-2">
            <div class="flex items-center gap-2">
              <input v-model="rememberMe" type="checkbox" id="remember" class="w-4 h-4 rounded border-white/30 bg-white/10 text-[#f5c518] focus:ring-0 focus:ring-offset-0 cursor-pointer" />
              <label for="remember" class="text-xs text-white/60 cursor-pointer hover:text-white transition-colors">Ghi nhớ</label>
            </div>
          </div>

          <!-- Submit Button -->
          <button type="submit" :disabled="isLoading" class="w-full bg-[#f5c518] text-black py-4 mt-4 rounded-xl font-extrabold uppercase tracking-[0.2em] text-sm hover:bg-white hover:shadow-[0_0_20px_rgba(245,197,24,0.4)] transition-all duration-300 disabled:opacity-60 disabled:cursor-not-allowed">
            {{ isLoading ? 'Đang xác thực...' : 'Đăng Nhập' }}
          </button>
        </form>


        
      </div>
      
      <!-- Footer text -->
      <div class="mt-8 text-center">
        <p class="text-white/30 text-[10px] uppercase tracking-widest">| SU26 | DevCine | DATN |</p>
      </div>
    </div>

    <!-- Quick Access (Dev Only) -->
    <div class="absolute bottom-4 right-4 z-50">
      <button type="button" @click="loginAsAdmin" class="text-white/30 hover:text-[#f5c518] transition-colors p-2" title="Dev Mode: Truy cập nhanh">
        <span class="material-symbols-outlined text-sm">vpn_key</span>
      </button>
    </div>

    <!-- Toast dùng chung (trang login không nằm trong layout nên tự mount tại đây) -->
    <AppToast />
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
