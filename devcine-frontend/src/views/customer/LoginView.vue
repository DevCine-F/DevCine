<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { authApi } from '../../api/customer/index'
import { useToastStore } from '../../stores/toast'
import { friendlyError } from '../../utils/friendlyError'
import adminRoutes from '@/routers/admin'
import { resolveFirstAccessibleAdminPath } from '@/utils/adminAccess'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const toast = useToastStore()
const activeTab = ref('login')
const isLoading = ref(false)

// Hiện/ẩn mật khẩu
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)

// ===== Regex & helper validate =====
const EMAIL_RE = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
const PHONE_RE = /^0[35789]\d{8}$/            // đầu số VN, đủ 10 số
const NAME_RE = /^[\p{L}\s]+$/u               // chỉ chữ cái (có dấu) + khoảng trắng
const STRONG_RE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,32}$/
// +84xxxxxxxxx -> 0xxxxxxxxx; bỏ khoảng trắng
const normalizePhone = (v) => (v || '').trim().replace(/\s+/g, '').replace(/^\+84/, '0')

// Form fields
const loginForm = ref({ identifier: '', password: '' }) // identifier = số điện thoại HOẶC email
const registerForm = ref({ fullName: '', email: '', phone: '', password: '' })

// ===== Validate ĐĂNG NHẬP =====
const loginErrors = ref({ identifier: '', password: '' })

const validateIdentifier = (raw) => {
  const v = (raw || '').trim()
  if (!v) return 'Vui lòng nhập số điện thoại hoặc email.'
  if (v.includes('@')) {
    if (!EMAIL_RE.test(v)) return 'Email không đúng định dạng (vd: ten@domain.com).'
  } else {
    const p = normalizePhone(v)
    if (!/^[0-9]+$/.test(p)) return 'Số điện thoại chỉ gồm chữ số.'
    if (!PHONE_RE.test(p)) return 'Số điện thoại không hợp lệ (đầu số VN, đủ 10 số).'
  }
  return ''
}

const validateLogin = () => {
  loginErrors.value.identifier = validateIdentifier(loginForm.value.identifier)
  loginErrors.value.password = loginForm.value.password ? '' : 'Vui lòng nhập mật khẩu.'
  return !loginErrors.value.identifier && !loginErrors.value.password
}

const handleLogin = async () => {
  if (!validateLogin()) return
  const raw = loginForm.value.identifier.trim()
  // Tự nhận diện: có '@' -> email; ngược lại -> chuẩn hoá số điện thoại
  const identifier = raw.includes('@') ? raw : normalizePhone(raw)
  isLoading.value = true
  try {
    const res = await authApi.login(identifier, loginForm.value.password)
    const { token, user } = res.data.data
    const role = user.role.toLowerCase()
    authStore.login({ id: user.id, username: user.username, email: user.email, fullName: user.fullName }, token, role)
    if (role === 'admin' || role === 'staff') {
      try {
        await authStore.fetchPermissions(true)
      } catch {
        authStore.logout()
        toast.error('Đăng nhập thành công nhưng chưa tải được quyền nội bộ. Vui lòng khởi động lại backend và thử lại.')
        return
      }

      toast.success('Đăng nhập thành công! Đang chuyển sang khu nội bộ.')
      router.push(resolveFirstAccessibleAdminPath(adminRoutes, authStore))
      return
    }

    toast.success('Đăng nhập thành công! Chào mừng bạn đã trở lại.')
    router.push(route.query.redirect || '/')
  } catch (err) {
    // Lưu ý bảo mật: báo lỗi chung, tránh username enumeration
    toast.error(friendlyError(err, 'Số điện thoại/email hoặc mật khẩu không chính xác.'))
  } finally {
    isLoading.value = false
  }
}

// ===== Validate ĐĂNG KÝ (kiểm tra trực tiếp để bật/tắt nút) =====
const touched = ref({ fullName: false, email: false, phone: false, password: false })
const triedRegister = ref(false)

const registerErrors = computed(() => {
  const f = registerForm.value
  const e = { fullName: '', email: '', phone: '', password: '' }

  const name = f.fullName.trim()
  if (!name) e.fullName = 'Vui lòng nhập họ và tên.'
  else if (name.length < 2 || name.length > 50) e.fullName = 'Họ tên từ 2 đến 50 ký tự.'
  else if (!NAME_RE.test(name)) e.fullName = 'Họ tên chỉ gồm chữ cái và khoảng trắng.'

  const email = f.email.trim()
  if (!email) e.email = 'Vui lòng nhập email.'
  else if (!EMAIL_RE.test(email)) e.email = 'Email không đúng định dạng (vd: ten@domain.com).'

  const phoneRaw = f.phone.trim()
  const phone = normalizePhone(phoneRaw)
  if (!phoneRaw) e.phone = 'Vui lòng nhập số điện thoại.'
  else if (!/^[0-9]+$/.test(phone)) e.phone = 'Số điện thoại chỉ gồm chữ số.'
  else if (!PHONE_RE.test(phone)) e.phone = 'Số điện thoại không hợp lệ (đầu số VN, đủ 10 số).'

  if (!f.password) e.password = 'Vui lòng nhập mật khẩu.'
  else if (!STRONG_RE.test(f.password)) e.password = 'Mật khẩu 8–32 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.'

  return e
})
const isRegisterValid = computed(() => Object.values(registerErrors.value).every((v) => !v))
// Chỉ hiển thị lỗi khi field đã chạm hoặc đã bấm Đăng ký
const showRegErr = (field) => (touched.value[field] || triedRegister.value) && registerErrors.value[field]

// Chặn ký tự không phải số (cho phép dấu + đầu) khi gõ số điện thoại
const onPhoneInput = () => { registerForm.value.phone = registerForm.value.phone.replace(/[^\d+]/g, '') }

const handleRegister = async () => {
  triedRegister.value = true
  if (!isRegisterValid.value) return
  const f = registerForm.value
  const payload = {
    fullName: f.fullName.trim(),
    email: f.email.trim(),
    phone: normalizePhone(f.phone),
    password: f.password,
  }
  isLoading.value = true
  try {
    await authApi.register(payload)
    const res = await authApi.login(payload.phone, payload.password) // tự đăng nhập bằng SĐT
    const { token, user } = res.data.data
    authStore.login({ id: user.id, username: user.username, email: user.email, fullName: user.fullName }, token, user.role.toLowerCase())
    toast.success('Đăng ký thành công! Chào mừng bạn đến với DevCine.')
    router.push(route.query.redirect || '/')
  } catch (err) {
    // Lỗi trùng email/SĐT do backend trả (vd "Email này đã được sử dụng")
    toast.error(friendlyError(err, 'Đăng ký thất bại. Vui lòng thử lại.'))
  } finally {
    isLoading.value = false
  }
}

// Nút demo: điền sẵn tài khoản test vào form đăng nhập, người dùng tự bấm "Đăng nhập"
const loginDemo = () => {
  activeTab.value = 'login'
  loginForm.value = { identifier: '0901234567', password: 'Khach@123' }
  loginErrors.value = { identifier: '', password: '' }
  showLoginPassword.value = true
  toast.info('Đã điền tài khoản demo. Bấm "Đăng nhập" để tiếp tục.')
}

// ===== Quên mật khẩu: nhập email → nếu email có trong hệ thống thì gửi OTP → xác minh → đặt lại =====
const forgotMode = ref(false)
const forgotStep = ref(1) // 1: nhập email, 2: nhập mã OTP, 3: đặt mật khẩu mới
const forgotLoading = ref(false)
const showNewPassword = ref(false)
const forgotForm = ref({ email: '', otp: '', newPassword: '', confirmPassword: '' })

const openForgot = () => {
  forgotMode.value = true
  forgotStep.value = 1
  forgotForm.value = { email: '', otp: '', newPassword: '', confirmPassword: '' }
  // Tiện: nếu ô đăng nhập đang là email thì điền sẵn
  const id = loginForm.value.identifier.trim()
  if (id.includes('@')) forgotForm.value.email = id
}
const closeForgot = () => { forgotMode.value = false }

const sendOtp = async () => {
  const email = forgotForm.value.email.trim()
  if (!EMAIL_RE.test(email)) { toast.error('Email không đúng định dạng (vd: ten@domain.com).'); return }
  forgotLoading.value = true
  try {
    await authApi.forgotPassword(email)
    toast.success('Đã gửi mã xác minh tới email của bạn. Vui lòng kiểm tra hộp thư (cả mục Spam).')
    forgotStep.value = 2
  } catch (err) {
    toast.error(friendlyError(err, 'Không gửi được mã xác minh. Vui lòng thử lại.'))
  } finally {
    forgotLoading.value = false
  }
}

const verifyOtpStep = async () => {
  const otp = forgotForm.value.otp.trim()
  if (!/^\d{6}$/.test(otp)) { toast.error('Mã xác minh gồm 6 chữ số.'); return }
  forgotLoading.value = true
  try {
    await authApi.verifyOtp(forgotForm.value.email.trim(), otp)
    forgotStep.value = 3
  } catch (err) {
    toast.error(friendlyError(err, 'Mã xác minh không đúng hoặc đã hết hạn.'))
  } finally {
    forgotLoading.value = false
  }
}

const submitNewPassword = async () => {
  const { newPassword, confirmPassword } = forgotForm.value
  if (!STRONG_RE.test(newPassword)) { toast.error('Mật khẩu 8–32 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.'); return }
  if (newPassword !== confirmPassword) { toast.error('Mật khẩu xác nhận không khớp.'); return }
  forgotLoading.value = true
  try {
    await authApi.resetPassword(forgotForm.value.email.trim(), forgotForm.value.otp.trim(), newPassword)
    toast.success('Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.')
    loginForm.value.identifier = forgotForm.value.email.trim()
    loginForm.value.password = ''
    activeTab.value = 'login'
    forgotMode.value = false
  } catch (err) {
    toast.error(friendlyError(err, 'Đặt lại mật khẩu thất bại. Vui lòng thử lại.'))
  } finally {
    forgotLoading.value = false
  }
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
        
        <!-- ===== Đăng nhập / Đăng ký (ẩn khi đang quên mật khẩu) ===== -->
        <template v-if="!forgotMode">
        <!-- Tabs -->
        <div class="flex gap-8 border-b border-outline-variant/20 mb-6">
          <button @click="activeTab = 'login'" :class="['pb-4 font-bold text-sm uppercase tracking-widest transition-all', activeTab === 'login' ? 'text-[#f5c518] border-b-2 border-[#f5c518]' : 'text-neutral-500 hover:text-[#f5c518] border-b-2 border-transparent']">Đăng nhập</button>
          <button @click="activeTab = 'register'" :class="['pb-4 font-bold text-sm uppercase tracking-widest transition-all', activeTab === 'register' ? 'text-[#f5c518] border-b-2 border-[#f5c518]' : 'text-neutral-500 hover:text-[#f5c518] border-b-2 border-transparent']">Đăng ký</button>
        </div>
        
        <!-- Form -->
        <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="space-y-4">
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Số điện thoại hoặc Email</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">account_circle</span>
              <input v-model="loginForm.identifier" @input="loginErrors.identifier = ''" type="text" placeholder="Nhập số điện thoại hoặc email"
                     :class="loginErrors.identifier ? 'ring-1 ring-red-500' : 'focus:ring-1 focus:ring-[#f5c518]'"
                     class="w-full bg-surface-container-lowest border-none py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all"/>
            </div>
            <p v-if="loginErrors.identifier" class="text-red-400 text-xs flex items-center gap-1">
              <span class="material-symbols-outlined text-sm">error</span>{{ loginErrors.identifier }}
            </p>
          </div>
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Mật khẩu</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">lock</span>
              <input v-model="loginForm.password" @input="loginErrors.password = ''" :type="showLoginPassword ? 'text' : 'password'" placeholder="••••••••"
                     :class="loginErrors.password ? 'ring-1 ring-red-500' : 'focus:ring-1 focus:ring-[#f5c518]'"
                     class="w-full bg-surface-container-lowest border-none py-4 pl-12 pr-12 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all"/>
              <button type="button" @click="showLoginPassword = !showLoginPassword" :title="showLoginPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                      class="absolute right-2 inset-y-0 flex items-center px-2 text-outline-variant hover:text-[#f5c518] transition-colors">
                <span class="material-symbols-outlined text-lg leading-none">{{ showLoginPassword ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
            <p v-if="loginErrors.password" class="text-red-400 text-xs flex items-center gap-1">
              <span class="material-symbols-outlined text-sm">error</span>{{ loginErrors.password }}
            </p>
          </div>
          <div class="flex justify-end">
            <button type="button" @click="openForgot" class="text-xs font-semibold text-neutral-400 hover:text-[#f5c518] transition-colors">Quên mật khẩu?</button>
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
              <input v-model="registerForm.fullName" @blur="touched.fullName = true" type="text" placeholder="Nguyễn Văn A"
                     :class="showRegErr('fullName') ? 'ring-1 ring-red-500' : 'focus:ring-1 focus:ring-[#f5c518]'"
                     class="w-full bg-surface-container-lowest border-none py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all"/>
            </div>
            <p v-if="showRegErr('fullName')" class="text-red-400 text-xs flex items-center gap-1">
              <span class="material-symbols-outlined text-sm">error</span>{{ registerErrors.fullName }}
            </p>
          </div>
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Email của bạn</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">mail</span>
              <input v-model="registerForm.email" @blur="touched.email = true" type="text" placeholder="email@example.com"
                     :class="showRegErr('email') ? 'ring-1 ring-red-500' : 'focus:ring-1 focus:ring-[#f5c518]'"
                     class="w-full bg-surface-container-lowest border-none py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all"/>
            </div>
            <p v-if="showRegErr('email')" class="text-red-400 text-xs flex items-center gap-1">
              <span class="material-symbols-outlined text-sm">error</span>{{ registerErrors.email }}
            </p>
          </div>
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Số điện thoại</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">call</span>
              <input v-model="registerForm.phone" @input="onPhoneInput" @blur="touched.phone = true" type="tel" inputmode="numeric" placeholder="VD: 0901234567"
                     :class="showRegErr('phone') ? 'ring-1 ring-red-500' : 'focus:ring-1 focus:ring-[#f5c518]'"
                     class="w-full bg-surface-container-lowest border-none py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all"/>
            </div>
            <p v-if="showRegErr('phone')" class="text-red-400 text-xs flex items-center gap-1">
              <span class="material-symbols-outlined text-sm">error</span>{{ registerErrors.phone }}
            </p>
          </div>
          <div class="space-y-2">
            <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Mật khẩu</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">lock</span>
              <input v-model="registerForm.password" @blur="touched.password = true" :type="showRegisterPassword ? 'text' : 'password'" placeholder="••••••••"
                     :class="showRegErr('password') ? 'ring-1 ring-red-500' : 'focus:ring-1 focus:ring-[#f5c518]'"
                     class="w-full bg-surface-container-lowest border-none py-4 pl-12 pr-12 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all"/>
              <button type="button" @click="showRegisterPassword = !showRegisterPassword" :title="showRegisterPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                      class="absolute right-2 inset-y-0 flex items-center px-2 text-outline-variant hover:text-[#f5c518] transition-colors">
                <span class="material-symbols-outlined text-lg leading-none">{{ showRegisterPassword ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
            <p v-if="showRegErr('password')" class="text-red-400 text-xs flex items-center gap-1">
              <span class="material-symbols-outlined text-sm">error</span>{{ registerErrors.password }}
            </p>
            <p v-else class="text-neutral-500 text-[11px]">8–32 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.</p>
          </div>
          <button type="submit" :disabled="isLoading || !isRegisterValid"
                  class="w-full bg-primary-container text-on-primary py-4 font-bold uppercase tracking-widest text-sm rounded-sm hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-2 mt-4 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:brightness-100">
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
        </template>

        <!-- ===== Quên mật khẩu (wizard 3 bước) ===== -->
        <div v-else class="space-y-5">
          <button type="button" @click="closeForgot" class="flex items-center gap-1 text-xs font-semibold text-neutral-400 hover:text-[#f5c518] transition-colors">
            <span class="material-symbols-outlined text-base">arrow_back</span> Quay lại đăng nhập
          </button>

          <!-- Chỉ báo tiến trình -->
          <div class="flex items-center gap-2">
            <div v-for="s in 3" :key="s" class="flex-1 h-1 rounded-full transition-colors" :class="forgotStep >= s ? 'bg-[#f5c518]' : 'bg-outline-variant/20'"></div>
          </div>

          <!-- Bước 1: Nhập email -->
          <form v-if="forgotStep === 1" @submit.prevent="sendOtp" class="space-y-4">
            <div>
              <h3 class="text-lg font-bold text-on-surface">Quên mật khẩu</h3>
              <p class="text-sm text-on-surface-variant mt-1">Nhập email tài khoản. Nếu email có trong hệ thống, chúng tôi sẽ gửi mã xác minh 6 số tới hộp thư của bạn.</p>
            </div>
            <div class="space-y-2">
              <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Email tài khoản</label>
              <div class="relative">
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">mail</span>
                <input v-model="forgotForm.email" type="email" placeholder="email@example.com"
                       class="w-full bg-surface-container-lowest border-none py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all focus:ring-1 focus:ring-[#f5c518]"/>
              </div>
            </div>
            <button type="submit" :disabled="forgotLoading" class="w-full bg-primary-container text-on-primary py-4 font-bold uppercase tracking-widest text-sm rounded-sm hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-60">
              {{ forgotLoading ? 'Đang gửi...' : 'Gửi mã xác minh' }}
            </button>
          </form>

          <!-- Bước 2: Nhập mã OTP -->
          <form v-else-if="forgotStep === 2" @submit.prevent="verifyOtpStep" class="space-y-4">
            <div>
              <h3 class="text-lg font-bold text-on-surface">Nhập mã xác minh</h3>
              <p class="text-sm text-on-surface-variant mt-1">Mã 6 số đã được gửi tới <b class="text-on-surface">{{ forgotForm.email }}</b>. Mã có hiệu lực trong 10 phút.</p>
            </div>
            <div class="space-y-2">
              <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Mã xác minh (OTP)</label>
              <input v-model="forgotForm.otp" type="text" inputmode="numeric" maxlength="6" placeholder="------"
                     @input="forgotForm.otp = forgotForm.otp.replace(/\D/g, '')"
                     class="w-full bg-surface-container-lowest border-none py-4 px-4 text-on-surface text-center text-2xl font-bold tracking-[0.5em] placeholder:text-neutral-700 placeholder:tracking-[0.3em] rounded-sm transition-all focus:ring-1 focus:ring-[#f5c518]"/>
            </div>
            <button type="submit" :disabled="forgotLoading" class="w-full bg-primary-container text-on-primary py-4 font-bold uppercase tracking-widest text-sm rounded-sm hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-60">
              {{ forgotLoading ? 'Đang xác minh...' : 'Xác minh' }}
            </button>
            <div class="flex items-center justify-between text-xs">
              <button type="button" @click="forgotStep = 1" class="font-semibold text-neutral-400 hover:text-[#f5c518] transition-colors">Đổi email</button>
              <button type="button" @click="sendOtp" :disabled="forgotLoading" class="font-semibold text-neutral-400 hover:text-[#f5c518] transition-colors disabled:opacity-50">Gửi lại mã</button>
            </div>
          </form>

          <!-- Bước 3: Đặt mật khẩu mới -->
          <form v-else @submit.prevent="submitNewPassword" class="space-y-4">
            <div>
              <h3 class="text-lg font-bold text-on-surface">Đặt mật khẩu mới</h3>
              <p class="text-sm text-on-surface-variant mt-1">Tạo mật khẩu mới cho tài khoản của bạn.</p>
            </div>
            <div class="space-y-2">
              <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Mật khẩu mới</label>
              <div class="relative">
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">lock</span>
                <input v-model="forgotForm.newPassword" :type="showNewPassword ? 'text' : 'password'" placeholder="••••••••"
                       class="w-full bg-surface-container-lowest border-none py-4 pl-12 pr-12 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all focus:ring-1 focus:ring-[#f5c518]"/>
                <button type="button" @click="showNewPassword = !showNewPassword"
                        class="absolute right-2 inset-y-0 flex items-center px-2 text-outline-variant hover:text-[#f5c518] transition-colors">
                  <span class="material-symbols-outlined text-lg leading-none">{{ showNewPassword ? 'visibility_off' : 'visibility' }}</span>
                </button>
              </div>
              <p class="text-neutral-500 text-[11px]">8–32 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.</p>
            </div>
            <div class="space-y-2">
              <label class="block text-[10px] font-bold uppercase tracking-[0.1em] text-on-surface-variant">Xác nhận mật khẩu</label>
              <div class="relative">
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline-variant text-lg">lock</span>
                <input v-model="forgotForm.confirmPassword" :type="showNewPassword ? 'text' : 'password'" placeholder="••••••••"
                       class="w-full bg-surface-container-lowest border-none py-4 pl-12 pr-4 text-on-surface placeholder:text-neutral-700 rounded-sm transition-all focus:ring-1 focus:ring-[#f5c518]"/>
              </div>
            </div>
            <button type="submit" :disabled="forgotLoading" class="w-full bg-primary-container text-on-primary py-4 font-bold uppercase tracking-widest text-sm rounded-sm hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-60">
              {{ forgotLoading ? 'Đang lưu...' : 'Đặt lại mật khẩu' }}
            </button>
          </form>
        </div>
      </div>
    </div>
  </main>
</template>
