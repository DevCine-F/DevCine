<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { authApi } from '../../api/customer/index'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const activeTab = ref('login')
const isLoading = ref(false)
const errorMsg = ref('')

// Form fields
const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', email: '', fullName: '', phone: '', password: '' })

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) return
  isLoading.value = true
  errorMsg.value = ''
  try {
    const res = await authApi.login(loginForm.value.username, loginForm.value.password)
    const { token, user } = res.data.data
    const role = user.role.toLowerCase()
    authStore.login({ id: user.id, username: user.username, email: user.email, fullName: user.fullName }, token, role)
    // Khách: quay lại trang trước khi bị chặn (vd /booking?step=2) nếu có
    router.push(role === 'admin' || role === 'staff' ? '/admin/dashboard' : (route.query.redirect || '/'))
  } catch (err) {
    errorMsg.value = err.response?.data?.message || 'Đăng nhập thất bại. Kiểm tra lại tài khoản và mật khẩu.'
  } finally {
    isLoading.value = false
  }
}

const handleRegister = async () => {
  const f = registerForm.value
  if (!f.username || !f.email || !f.password) return
  isLoading.value = true
  errorMsg.value = ''
  try {
    await authApi.register({ username: f.username, email: f.email, fullName: f.fullName, phone: f.phone, password: f.password })
    // Auto-login after register
    const res = await authApi.login(f.username, f.password)
    const { token, user } = res.data.data
    authStore.login({ id: user.id, username: user.username, email: user.email, fullName: user.fullName }, token, user.role.toLowerCase())
    router.push(route.query.redirect || '/')
  } catch (err) {
    errorMsg.value = err.response?.data?.message || 'Đăng ký thất bại. Vui lòng thử lại.'
  } finally {
    isLoading.value = false
  }
}

// Giữ nút demo để test nhanh khi dev
const loginDemo = async () => {
  loginForm.value = { username: 'khachhang', password: 'Khach@123' }
  await handleLogin()
}
</script>

<template>
  <main class="flex-grow flex items-stretch h-screen overflow-hidden">
    <!-- Left Side: Cinematic Imagery -->
    <div class="hidden md:flex md:w-1/2 relative overflow-hidden bg-surface-container-lowest">
      <div class="absolute inset-0 bg-gradient-to-r from-transparent to-surface z-10"></div>
      <div class="absolute inset-0 bg-gradient-to-t from-surface via-transparent to-transparent z-10"></div>
      <img class="absolute inset-0 w-full h-full object-cover brightness-50" src="/images/Hopper.webp"/>
      <div class="relative z-20 flex flex-col justify-end p-20 max-w-xl">
        <span class="text-[#f5c518] font-bold text-xs uppercase tracking-[0.3em] mb-4">Trải nghiệm đáng nhớ cùng</span>
        <h1 class="text-5xl lg:text-7xl font-extrabold tracking-tighter text-on-surface mb-6 leading-[0.9]">DEVCINE CINEMA.</h1>
        <p class="text-on-surface-variant text-lg leading-relaxed max-w-md">
          Đăng nhập ngay để đặt vé, nhận ưu đãi độc quyền và tận hưởng không gian điện ảnh đỉnh cao.
        </p>
      </div>
    </div>
    
    <!-- Right Side: Auth Card -->
    <div class="w-full md:w-1/2 flex items-center justify-center p-6 md:p-10 bg-surface">
      <div class="w-full max-w-md">
        <div class="mb-6">
          <h2 class="text-3xl font-bold tracking-tight text-on-surface mb-2">Chào mừng trở lại</h2>
          <p class="text-on-surface-variant">Hãy nhập thông tin để truy cập tài khoản DevCine của bạn.</p>
        </div>
        
        <!-- Tabs -->
        <div class="flex gap-8 border-b border-outline-variant/20 mb-6">
          <button @click="activeTab = 'login'" :class="['pb-4 font-bold text-sm uppercase tracking-widest transition-all', activeTab === 'login' ? 'text-[#f5c518] border-b-2 border-[#f5c518]' : 'text-neutral-500 hover:text-[#f5c518] border-b-2 border-transparent']">Đăng nhập</button>
          <button @click="activeTab = 'register'" :class="['pb-4 font-bold text-sm uppercase tracking-widest transition-all', activeTab === 'register' ? 'text-[#f5c518] border-b-2 border-[#f5c518]' : 'text-neutral-500 hover:text-[#f5c518] border-b-2 border-transparent']">Đăng ký</button>
        </div>
        
        <!-- Form -->
        <!-- Error Message -->
        <div v-if="errorMsg" class="mb-4 px-4 py-3 bg-red-500/10 border border-red-500/20 rounded text-red-400 text-xs font-semibold">
          {{ errorMsg }}
        </div>

        <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="space-y-4">
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Tên đăng nhập</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">person</span>
              <input v-model="loginForm.username" class="w-full bg-surface-container-lowest border-none focus:ring-1 focus:ring-[#f5c518] py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all" placeholder="Tên đăng nhập" type="text" required/>
            </div>
          </div>
          <div class="space-y-2">
            <div class="flex justify-between items-center">
              <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Mật khẩu</label>
            </div>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">lock</span>
              <input v-model="loginForm.password" class="w-full bg-surface-container-lowest border-none focus:ring-1 focus:ring-[#f5c518] py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all" placeholder="••••••••" type="password" required/>
            </div>
          </div>
          <button type="submit" :disabled="isLoading" class="w-full bg-primary-container text-on-primary py-4 font-bold uppercase tracking-widest text-sm rounded-sm hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-60">
            {{ isLoading ? 'Đang đăng nhập...' : 'Đăng nhập' }}
            <span v-if="!isLoading" class="material-symbols-outlined">chevron_right</span>
          </button>
        </form>

        <form v-else @submit.prevent="handleRegister" class="space-y-4">
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Họ và tên</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">person</span>
              <input v-model="registerForm.fullName" class="w-full bg-surface-container-lowest border-none focus:ring-1 focus:ring-[#f5c518] py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all" placeholder="Nguyễn Văn A" type="text" required/>
            </div>
          </div>
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Tên đăng nhập</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">badge</span>
              <input v-model="registerForm.username" class="w-full bg-surface-container-lowest border-none focus:ring-1 focus:ring-[#f5c518] py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all" placeholder="username" type="text" required/>
            </div>
          </div>
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Email của bạn</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">mail</span>
              <input v-model="registerForm.email" class="w-full bg-surface-container-lowest border-none focus:ring-1 focus:ring-[#f5c518] py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all" placeholder="email@example.com" type="email" required/>
            </div>
          </div>
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Mật khẩu</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">lock</span>
              <input v-model="registerForm.password" class="w-full bg-surface-container-lowest border-none focus:ring-1 focus:ring-[#f5c518] py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all" placeholder="••••••••" type="password" required/>
            </div>
          </div>
          <button type="submit" :disabled="isLoading" class="w-full bg-primary-container text-on-primary py-4 font-bold uppercase tracking-widest text-sm rounded-sm hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-2 mt-4 disabled:opacity-60">
            {{ isLoading ? 'Đang đăng ký...' : 'Đăng ký' }}
            <span v-if="!isLoading" class="material-symbols-outlined">chevron_right</span>
          </button>
        </form>
        
        <!-- Divider -->
        <div class="relative my-6 flex items-center">
          <div class="flex-grow border-t border-outline-variant/10"></div>
          <span class="px-4 text-[10px] font-bold uppercase tracking-[0.2em] text-neutral-600 bg-surface">Hoặc tiếp tục với</span>
          <div class="flex-grow border-t border-outline-variant/10"></div>
        </div>
        
        <!-- Social Login -->
        <div class="grid grid-cols-1 gap-4">
          <button class="flex items-center justify-center gap-3 py-4 bg-surface-container-high hover:bg-surface-container-highest transition-colors rounded-sm border border-outline-variant/10">
            <img alt="Google" class="w-5 h-5" src="https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg"/>
            <span class="text-xs font-bold uppercase tracking-tighter text-on-surface">Google</span>
          </button>
        </div>
        
        <!-- Quick Access for Testing -->
        <div class="mt-6 pt-6 border-t border-outline-variant/10">
          <p class="text-[9px] font-black uppercase tracking-widest text-neutral-600 mb-4 text-center">Chế độ thử nghiệm</p>
          <div class="flex gap-4">
            <button type="button" @click="loginDemo"
                    class="flex-1 py-3 bg-surface-container-high border border-outline-variant/20 rounded text-[9px] font-bold uppercase tracking-widest hover:bg-white/10 transition-colors">
              Demo Khách hàng
            </button>
            <button type="button" @click="router.push('/admin/login')"
                    class="flex-1 py-3 bg-primary/10 border border-primary/40 rounded text-[9px] font-bold uppercase tracking-widest text-primary hover:bg-primary/20 transition-colors">
              Vào trang Admin
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
