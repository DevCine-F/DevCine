<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/customer/index'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const authStore = useAuthStore()
const toast = useToastStore()
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const isLoading = ref(false)

// Tiêu chí validate mật khẩu mới (bộ "Mạnh") — hiển thị checklist live + chặn submit
const rules = computed(() => {
  const v = newPassword.value
  return [
    { label: 'Ít nhất 8 ký tự', ok: v.length >= 8 },
    { label: 'Có chữ hoa (A-Z)', ok: /[A-Z]/.test(v) },
    { label: 'Có chữ thường (a-z)', ok: /[a-z]/.test(v) },
    { label: 'Có chữ số (0-9)', ok: /\d/.test(v) },
    { label: 'Có ký tự đặc biệt (!@#$…)', ok: /[^A-Za-z0-9]/.test(v) },
    { label: 'Khác mật khẩu hiện tại', ok: !!v && v !== oldPassword.value },
    { label: 'Khớp với xác nhận mật khẩu', ok: !!v && v === confirmPassword.value },
  ]
})
const allRulesPassed = computed(() => rules.value.every(r => r.ok))

const handleSubmit = async () => {
  if (!oldPassword.value || !newPassword.value || !confirmPassword.value) {
    toast.warning('Vui lòng điền đầy đủ thông tin.')
    return
  }
  if (!allRulesPassed.value) {
    toast.warning('Mật khẩu mới chưa đạt đủ tiêu chí. Vui lòng kiểm tra danh sách bên dưới.')
    return
  }

  isLoading.value = true
  try {
    await authApi.changePassword(authStore.user.id, oldPassword.value, newPassword.value)
    toast.success('Đổi mật khẩu thành công!')
    oldPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } catch (err) {
    toast.error(friendlyError(err, 'Đổi mật khẩu thất bại. Kiểm tra lại mật khẩu hiện tại.'))
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <section class="max-w-5xl">
    <h2 class="text-xl sm:text-2xl font-bold tracking-tight font-headline mb-6 sm:mb-8">Đổi mật khẩu</h2>

    <div class="grid lg:grid-cols-[1fr_20rem] gap-6 items-start">
    <div class="bg-surface-container-low p-4 sm:p-8 rounded-2xl border border-white/5">
      <form @submit.prevent="handleSubmit" class="flex flex-col gap-4 sm:gap-6">
        <div>
          <label class="block text-[9px] sm:text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-2">Mật khẩu hiện tại</label>
          <div class="relative">
            <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-on-surface-variant/60 text-base sm:text-lg pointer-events-none">lock</span>
            <input v-model="oldPassword" type="password" class="w-full bg-black/30 border border-outline-variant/25 text-xs sm:text-sm text-white pl-10 sm:pl-11 pr-4 py-3 sm:py-3.5 rounded-xl outline-none transition-all hover:border-outline-variant/40 focus:border-primary-container focus:ring-2 focus:ring-primary-container/30 placeholder:text-on-surface-variant/40" placeholder="Nhập mật khẩu hiện tại" />
          </div>
        </div>

        <div>
          <label class="block text-[9px] sm:text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-2">Mật khẩu mới</label>
          <div class="relative">
            <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-on-surface-variant/60 text-base sm:text-lg pointer-events-none">lock_reset</span>
            <input v-model="newPassword" type="password" class="w-full bg-black/30 border border-outline-variant/25 text-xs sm:text-sm text-white pl-10 sm:pl-11 pr-4 py-3 sm:py-3.5 rounded-xl outline-none transition-all hover:border-outline-variant/40 focus:border-primary-container focus:ring-2 focus:ring-primary-container/30 placeholder:text-on-surface-variant/40" placeholder="Nhập mật khẩu mới" />
          </div>
        </div>

        <div>
          <label class="block text-[9px] sm:text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-2">Xác nhận mật khẩu mới</label>
          <div class="relative">
            <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-on-surface-variant/60 text-base sm:text-lg pointer-events-none">check_circle</span>
            <input v-model="confirmPassword" type="password" class="w-full bg-black/30 border border-outline-variant/25 text-xs sm:text-sm text-white pl-10 sm:pl-11 pr-4 py-3 sm:py-3.5 rounded-xl outline-none transition-all hover:border-outline-variant/40 focus:border-primary-container focus:ring-2 focus:ring-primary-container/30 placeholder:text-on-surface-variant/40" placeholder="Nhập lại mật khẩu mới" />
          </div>
        </div>

        <div class="pt-4 border-t border-white/5 mt-2 flex justify-end">
          <button type="submit" :disabled="isLoading || !allRulesPassed" class="w-full sm:w-auto bg-primary-container text-on-primary font-bold text-xs uppercase tracking-widest px-8 py-3 transition-transform active:scale-95 hover:bg-primary-fixed-dim rounded-xl disabled:opacity-60 disabled:cursor-not-allowed">
            {{ isLoading ? 'Đang cập nhật...' : 'Cập nhật mật khẩu' }}
          </button>
        </div>
      </form>
    </div>

    <!-- Card tiêu chí mật khẩu mới -->
    <aside class="bg-surface-container-low p-4 sm:p-6 rounded-2xl border border-white/5 lg:sticky lg:top-24">
      <h3 class="text-[9px] sm:text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-4 flex items-center gap-2">
        <span class="material-symbols-outlined text-base text-primary-container">shield</span> Tiêu chí mật khẩu
      </h3>
      <ul class="flex flex-col gap-2.5">
        <li v-for="r in rules" :key="r.label"
            class="flex items-center gap-2 text-xs transition-colors"
            :class="r.ok ? 'text-green-400' : 'text-on-surface-variant/60'">
          <span class="material-symbols-outlined text-base shrink-0">{{ r.ok ? 'check_circle' : 'radio_button_unchecked' }}</span>
          {{ r.label }}
        </li>
      </ul>
    </aside>
    </div>
  </section>
</template>
