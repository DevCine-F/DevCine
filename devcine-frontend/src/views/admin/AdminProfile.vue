<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { authApi } from '@/api/customer/index'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()

const authStore = useAuthStore()

const isLoading = ref(true)
const profile = ref(null)

// Form thông tin
const infoForm = reactive({ fullName: '', email: '', phone: '' })
const isSavingInfo = ref(false)

// Form đổi mật khẩu
const pwForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const isSavingPw = ref(false)
const showOld = ref(false)
const showNew = ref(false)


const roleLabel = computed(() => {
  const r = (profile.value?.role || authStore.role || '').toUpperCase()
  if (r === 'ADMIN') return 'Quản trị cấp cao'
  if (r === 'STAFF') return 'Nhân viên'
  return r || '—'
})
const isAdmin = computed(() => (profile.value?.role || authStore.role || '').toUpperCase() === 'ADMIN')
const initials = computed(() => {
  const name = profile.value?.fullName || profile.value?.username || 'A'
  return name.trim().charAt(0).toUpperCase()
})
const createdAtLabel = computed(() => {
  if (!profile.value?.createdAt) return '—'
  const d = new Date(profile.value.createdAt)
  return isNaN(d) ? '—' : d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
})

const loadProfile = async () => {
  if (!authStore.user?.id) { isLoading.value = false; return }
  isLoading.value = true
  try {
    const { data } = await authApi.getProfile(authStore.user.id)
    profile.value = data.data ?? data
    infoForm.fullName = profile.value.fullName || ''
    infoForm.email = profile.value.email || ''
    infoForm.phone = profile.value.phone || ''
  } catch (err) {
    toast.error(friendlyError(err, 'Không tải được thông tin tài khoản.'))
  } finally {
    isLoading.value = false
  }
}

const handleSaveInfo = async () => {
  if (!infoForm.fullName.trim()) { toast.warning('Họ tên không được để trống.'); return }
  isSavingInfo.value = true
  try {
    const { data } = await authApi.updateProfile({
      userId: authStore.user.id,
      fullName: infoForm.fullName.trim(),
      email: infoForm.email.trim(),
      phone: infoForm.phone.trim()
    })
    profile.value = data.data ?? data
    // Đồng bộ tên hiển thị trên topbar
    const u = { ...authStore.user, fullName: profile.value.fullName, email: profile.value.email }
    authStore.user = u
    localStorage.setItem('user', JSON.stringify(u))
    toast.success('Cập nhật thông tin thành công!')
  } catch (err) {
    toast.error(friendlyError(err, 'Cập nhật thất bại.'))
  } finally {
    isSavingInfo.value = false
  }
}

const handleChangePassword = async () => {
  if (!pwForm.oldPassword || !pwForm.newPassword) { toast.warning('Vui lòng nhập đủ mật khẩu.'); return }
  if (pwForm.newPassword.length < 3) { toast.warning('Mật khẩu mới quá ngắn.'); return }
  if (pwForm.newPassword !== pwForm.confirmPassword) { toast.warning('Xác nhận mật khẩu không khớp.'); return }
  isSavingPw.value = true
  try {
    await authApi.changePassword(authStore.user.id, pwForm.oldPassword, pwForm.newPassword)
    pwForm.oldPassword = ''; pwForm.newPassword = ''; pwForm.confirmPassword = ''
    toast.success('Đổi mật khẩu thành công!')
  } catch (err) {
    toast.error(friendlyError(err, 'Đổi mật khẩu thất bại.'))
  } finally {
    isSavingPw.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="p-10">
    <header class="mb-12 text-on-surface">
      <h1 class="text-3xl font-extrabold tracking-tight font-headline uppercase">Tài khoản của tôi</h1>
      <p class="text-on-surface-variant text-sm mt-1">Quản lý thông tin cá nhân và bảo mật tài khoản quản trị</p>
    </header>

    <!-- Loading skeleton -->
    <div v-if="isLoading" class="max-w-4xl space-y-8">
      <div class="h-40 bg-surface-container-high rounded-lg animate-pulse"></div>
      <div class="h-64 bg-surface-container-high rounded-lg animate-pulse"></div>
    </div>

    <div v-else class="max-w-4xl space-y-8">
      <!-- Account overview -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8">
        <div class="flex flex-col md:flex-row md:items-center gap-6">
          <div class="w-20 h-20 rounded-2xl bg-primary/15 border border-primary/30 flex items-center justify-center shrink-0">
            <span class="text-3xl font-black text-primary">{{ initials }}</span>
          </div>
          <div class="flex-grow min-w-0">
            <div class="flex items-center gap-3 flex-wrap">
              <h2 class="text-2xl font-black text-on-surface">{{ profile?.fullName || profile?.username }}</h2>
              <span class="px-3 py-1 bg-primary text-on-primary text-[9px] font-black rounded-full uppercase tracking-widest">{{ roleLabel }}</span>
            </div>
            <p class="text-sm text-on-surface-variant mt-1">@{{ profile?.username }} · Tham gia {{ createdAtLabel }}</p>
            <div class="flex items-center gap-2 mt-3">
              <span class="w-2 h-2 rounded-full" :class="profile?.isActive ? 'bg-green-400' : 'bg-red-400'"></span>
              <span class="text-xs font-bold uppercase tracking-wider" :class="profile?.isActive ? 'text-green-400' : 'text-red-400'">
                {{ profile?.isActive ? 'Đang hoạt động' : 'Đã vô hiệu hoá' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Quyền hạn -->
        <div v-if="isAdmin" class="mt-6 p-5 rounded-2xl bg-primary/5 border border-primary/20 flex items-start gap-4">
          <span class="material-symbols-outlined text-primary mt-0.5">verified_user</span>
          <div>
            <p class="text-sm font-black text-primary uppercase tracking-wide">Toàn quyền hệ thống</p>
            <p class="text-xs text-on-surface-variant mt-1">Tài khoản quản trị cấp cao — truy cập & thao tác mọi phân hệ (phim, suất chiếu, POS, kho, nhân sự, khuyến mãi, định giá, cài đặt, phân quyền).</p>
          </div>
        </div>
      </section>

      <!-- Edit info -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8">
        <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-8 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">badge</span>
          Thông tin cá nhân
        </h3>
        <div class="space-y-6">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Họ và tên</label>
              <input v-model="infoForm.fullName" :disabled="isSavingInfo" type="text" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tên đăng nhập</label>
              <input :value="profile?.username" disabled type="text" class="w-full bg-surface-container-high/50 border-none text-sm rounded-lg py-3 px-4 text-on-surface-variant cursor-not-allowed">
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Email</label>
              <input v-model="infoForm.email" :disabled="isSavingInfo" type="email" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Số điện thoại</label>
              <input v-model="infoForm.phone" :disabled="isSavingInfo" type="text" inputmode="numeric" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
            </div>
          </div>
          <div class="flex justify-end">
            <button @click="handleSaveInfo" :disabled="isSavingInfo" class="px-8 py-3 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded-sm hover:brightness-110 transition-all disabled:opacity-50">
              {{ isSavingInfo ? 'Đang lưu...' : 'Lưu thông tin' }}
            </button>
          </div>
        </div>
      </section>

      <!-- Change password -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8">
        <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-8 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">lock</span>
          Đổi mật khẩu
        </h3>
        <div class="space-y-6 max-w-md">
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mật khẩu hiện tại</label>
            <div class="relative">
              <input v-model="pwForm.oldPassword" :type="showOld ? 'text' : 'password'" :disabled="isSavingPw" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 pr-11 text-on-surface">
              <button type="button" @click="showOld = !showOld" class="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-primary transition-colors">
                <span class="material-symbols-outlined text-lg">{{ showOld ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
          </div>
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mật khẩu mới</label>
            <div class="relative">
              <input v-model="pwForm.newPassword" :type="showNew ? 'text' : 'password'" :disabled="isSavingPw" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 pr-11 text-on-surface">
              <button type="button" @click="showNew = !showNew" class="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-primary transition-colors">
                <span class="material-symbols-outlined text-lg">{{ showNew ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
          </div>
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Xác nhận mật khẩu mới</label>
            <input v-model="pwForm.confirmPassword" :type="showNew ? 'text' : 'password'" :disabled="isSavingPw" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
          </div>
          <div class="flex justify-end">
            <button @click="handleChangePassword" :disabled="isSavingPw" class="px-8 py-3 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded-sm hover:bg-white/10 transition-all border border-outline-variant/20 disabled:opacity-50">
              {{ isSavingPw ? 'Đang xử lý...' : 'Đổi mật khẩu' }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
