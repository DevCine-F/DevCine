<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import StarryBackground from '@/components/common/StarryBackground.vue'
import AppToast from '@/components/common/AppToast.vue'
import { authApi } from '@/api/customer/index'
import adminRoutes from '@/routers/admin'
import { resolveFirstAccessibleAdminPath } from '@/utils/adminAccess'
import { friendlyError } from '@/utils/friendlyError'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToastStore()

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showPw = ref(false)
const isSaving = ref(false)

// Validate mật khẩu mới
const pwError = computed(() => {
  if (!newPassword.value) return ''
  if (newPassword.value.length < 8) return 'Mật khẩu mới cần tối thiểu 8 ký tự.'
  if (newPassword.value === currentPassword.value) return 'Mật khẩu mới phải khác mật khẩu hiện tại.'
  return ''
})
const confirmError = computed(() => {
  if (!confirmPassword.value) return ''
  if (confirmPassword.value !== newPassword.value) return 'Xác nhận mật khẩu không khớp.'
  return ''
})
const canSubmit = computed(() =>
  currentPassword.value && newPassword.value && confirmPassword.value &&
  !pwError.value && !confirmError.value && !isSaving.value
)

const handleSubmit = async () => {
  if (!canSubmit.value) return
  const userId = authStore.user?.id
  if (!userId) { toast.error('Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.'); return }
  isSaving.value = true
  try {
    const { data } = await authApi.changePassword(userId, currentPassword.value, newPassword.value)
    if (data?.success === false) throw new Error(data.message || 'Đổi mật khẩu thất bại.')
    authStore.clearMustChangePassword()
    toast.success('Đổi mật khẩu thành công! Tài khoản đã được kích hoạt.')
    try { await authStore.fetchPermissions(true) } catch { /* vẫn cho vào, guard sẽ xử lý */ }
    router.replace(resolveFirstAccessibleAdminPath(adminRoutes, authStore))
  } catch (e) {
    toast.error(friendlyError(e, e?.message || 'Đổi mật khẩu thất bại. Kiểm tra lại mật khẩu hiện tại.'))
  } finally {
    isSaving.value = false
  }
}

const handleLogout = () => {
  authStore.logout()
  router.replace('/admin/login')
}
</script>

<template>
  <main class="relative w-full h-screen overflow-hidden flex items-center justify-center">
    <StarryBackground theme="admin" />

    <div class="relative z-20 w-full max-w-md px-6">
      <div class="bg-white/5 backdrop-blur-2xl border border-white/10 rounded-3xl p-10 shadow-[0_8px_32px_0_rgba(0,0,0,0.37)]">
        <div class="text-center mb-8">
          <span class="material-symbols-outlined text-[#f5c518] text-5xl">lock_reset</span>
          <h1 class="text-2xl font-black tracking-tight text-white mt-3 uppercase">Đổi mật khẩu lần đầu</h1>
          <p class="text-white/60 text-xs tracking-wide mt-2 leading-relaxed">
            Tài khoản của bạn đang dùng mật khẩu mặc định. Vui lòng đặt mật khẩu mới để kích hoạt và bắt đầu ca làm việc.
          </p>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-5">
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-white/50">Mật khẩu hiện tại (mặc định)</label>
            <input v-model="currentPassword" :type="showPw ? 'text' : 'password'" placeholder="VD: DevCine@2026"
                   class="w-full py-3 px-4 rounded-xl bg-white/10 border border-white/20 text-white text-sm outline-none focus:border-[#f5c518] transition-colors" />
          </div>

          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-white/50">Mật khẩu mới</label>
            <input v-model="newPassword" :type="showPw ? 'text' : 'password'" placeholder="Tối thiểu 8 ký tự"
                   class="w-full py-3 px-4 rounded-xl bg-white/10 border text-white text-sm outline-none transition-colors"
                   :class="pwError ? 'border-red-400 focus:border-red-400' : 'border-white/20 focus:border-[#f5c518]'" />
            <p v-if="pwError" class="text-[11px] text-red-400 font-medium">{{ pwError }}</p>
          </div>

          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-white/50">Xác nhận mật khẩu mới</label>
            <input v-model="confirmPassword" :type="showPw ? 'text' : 'password'" placeholder="Nhập lại mật khẩu mới"
                   class="w-full py-3 px-4 rounded-xl bg-white/10 border text-white text-sm outline-none transition-colors"
                   :class="confirmError ? 'border-red-400 focus:border-red-400' : 'border-white/20 focus:border-[#f5c518]'" />
            <p v-if="confirmError" class="text-[11px] text-red-400 font-medium">{{ confirmError }}</p>
          </div>

          <label class="flex items-center gap-2 text-xs text-white/60 cursor-pointer">
            <input type="checkbox" v-model="showPw" class="w-4 h-4 rounded border-white/30 bg-white/10 text-[#f5c518]" />
            Hiện mật khẩu
          </label>

          <button type="submit" :disabled="!canSubmit"
                  class="w-full bg-[#f5c518] text-black py-4 rounded-xl font-extrabold uppercase tracking-[0.2em] text-sm hover:bg-white transition-all disabled:opacity-50 disabled:cursor-not-allowed">
            {{ isSaving ? 'Đang lưu...' : 'Kích hoạt tài khoản' }}
          </button>
        </form>

        <button @click="handleLogout" class="w-full mt-4 text-white/40 hover:text-white/70 text-xs uppercase tracking-widest transition-colors">
          Đăng xuất
        </button>
      </div>
    </div>

    <AppToast />
  </main>
</template>
