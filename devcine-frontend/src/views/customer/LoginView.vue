<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const activeTab = ref('login')
const isDev = import.meta.env.DEV

const loginAs = (role) => {
  const email = role === 'admin' ? 'admin@devcine.com' : 'customer@devcine.com'
  const mockUser = { name: `Mock ${role}`, email }
  const mockToken = 'mock-jwt-token-' + Math.random().toString(36).substr(2)
  
  authStore.login(mockUser, mockToken, role)
  
  if (role === 'admin') {
    router.push('/admin/dashboard')
  } else {
    router.push('/')
  }
}

const handleLogin = (e) => {
  if (e && e.preventDefault) e.preventDefault()
  
  const formData = new FormData(e.target)
  const email = formData.get('email') || ''
  const role = email.toLowerCase().startsWith('admin') ? 'admin' : 'customer'
  
  loginAs(role)
}

const handleRegister = (e) => {
  if (e && e.preventDefault) e.preventDefault()
  // Mock successful registration by logging in as customer
  loginAs('customer')
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
        <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="space-y-4">
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Email của bạn</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">mail</span>
              <input name="email" class="w-full bg-surface-container-lowest border-none focus:ring-1 focus:ring-[#f5c518] py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all" placeholder="email@example.com" type="email" required/>
            </div>
          </div>
          <div class="space-y-2">
            <div class="flex justify-between items-center">
              <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Mật khẩu</label>
              <a class="text-[10px] font-bold uppercase tracking-[0.1em] text-[#f5c518] hover:underline" href="#">Quên mật khẩu?</a>
            </div>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">lock</span>
              <input class="w-full bg-surface-container-lowest border-none focus:ring-1 focus:ring-[#f5c518] py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all" placeholder="••••••••" type="password" required/>
            </div>
          </div>
          <div class="flex items-center gap-3 py-2">
            <input class="w-4 h-4 bg-surface-container-lowest border-outline-variant text-[#f5c518] focus:ring-0 rounded-sm" id="remember" type="checkbox"/>
            <label class="text-xs text-on-surface-variant font-medium" for="remember">Ghi nhớ đăng nhập</label>
          </div>
          <button type="submit" class="w-full bg-primary-container text-on-primary py-4 font-bold uppercase tracking-widest text-sm rounded-sm hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-2">
            Đăng nhập
            <span class="material-symbols-outlined">chevron_right</span>
          </button>
        </form>

        <form v-else @submit.prevent="handleRegister" class="space-y-4">
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Họ và tên</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">person</span>
              <input name="name" class="w-full bg-surface-container-lowest border-none focus:ring-1 focus:ring-[#f5c518] py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all" placeholder="Nguyễn Văn A" type="text" required/>
            </div>
          </div>
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Email của bạn</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">mail</span>
              <input name="email" class="w-full bg-surface-container-lowest border-none focus:ring-1 focus:ring-[#f5c518] py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all" placeholder="email@example.com" type="email" required/>
            </div>
          </div>
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Mật khẩu</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">lock</span>
              <input class="w-full bg-surface-container-lowest border-none focus:ring-1 focus:ring-[#f5c518] py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all" placeholder="••••••••" type="password" required/>
            </div>
          </div>
          <button type="submit" class="w-full bg-primary-container text-on-primary py-4 font-bold uppercase tracking-widest text-sm rounded-sm hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-2 mt-4">
            Đăng ký
            <span class="material-symbols-outlined">chevron_right</span>
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
        <div v-if="isDev" class="mt-6 pt-6 border-t border-outline-variant/10">
          <p class="text-[9px] font-black uppercase tracking-widest text-neutral-600 mb-4 text-center">Chế độ thử nghiệm</p>
          <div class="flex gap-4">
            <button @click="loginAs('customer')" 
                    class="flex-1 py-3 bg-surface-container-high border border-outline-variant/20 rounded text-[9px] font-bold uppercase tracking-widest hover:bg-white/10 transition-colors">
              Vào quyền Khách
            </button>
            <button @click="router.push('/admin/login')"
                    class="flex-1 py-3 bg-primary/10 border border-primary/40 rounded text-[9px] font-bold uppercase tracking-widest text-primary hover:bg-primary/20 transition-colors">
              Vào quyền Admin
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
