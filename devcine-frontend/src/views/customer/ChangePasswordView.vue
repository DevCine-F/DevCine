<script setup>
import { ref } from 'vue'
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

const handleSubmit = async () => {
  if (!oldPassword.value || !newPassword.value || !confirmPassword.value) {
    toast.warning('Vui lòng điền đầy đủ thông tin.')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    toast.warning('Mật khẩu mới và xác nhận mật khẩu không khớp.')
    return
  }
  if (newPassword.value.length < 6) {
    toast.warning('Mật khẩu mới phải có ít nhất 6 ký tự.')
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
  <section class="max-w-2xl">
    <h2 class="text-2xl font-bold tracking-tight font-headline mb-8">Đổi mật khẩu</h2>

    <div class="bg-surface-container-low p-8 rounded-lg border border-white/5">
      <form @submit.prevent="handleSubmit" class="flex flex-col gap-6">
        <div>
          <label class="block text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-2">Mật khẩu hiện tại</label>
          <div class="relative">
            <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-on-surface-variant/60 text-lg pointer-events-none">lock</span>
            <input v-model="oldPassword" type="password" class="w-full bg-black/30 border border-outline-variant/25 text-sm text-white pl-11 pr-4 py-3.5 rounded-lg outline-none transition-all hover:border-outline-variant/40 focus:border-primary-container focus:ring-2 focus:ring-primary-container/30 placeholder:text-on-surface-variant/40" placeholder="Nhập mật khẩu hiện tại" />
          </div>
        </div>

        <div>
          <label class="block text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-2">Mật khẩu mới</label>
          <div class="relative">
            <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-on-surface-variant/60 text-lg pointer-events-none">lock_reset</span>
            <input v-model="newPassword" type="password" class="w-full bg-black/30 border border-outline-variant/25 text-sm text-white pl-11 pr-4 py-3.5 rounded-lg outline-none transition-all hover:border-outline-variant/40 focus:border-primary-container focus:ring-2 focus:ring-primary-container/30 placeholder:text-on-surface-variant/40" placeholder="Nhập mật khẩu mới (ít nhất 6 ký tự)" />
          </div>
        </div>

        <div>
          <label class="block text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-2">Xác nhận mật khẩu mới</label>
          <div class="relative">
            <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-on-surface-variant/60 text-lg pointer-events-none">check_circle</span>
            <input v-model="confirmPassword" type="password" class="w-full bg-black/30 border border-outline-variant/25 text-sm text-white pl-11 pr-4 py-3.5 rounded-lg outline-none transition-all hover:border-outline-variant/40 focus:border-primary-container focus:ring-2 focus:ring-primary-container/30 placeholder:text-on-surface-variant/40" placeholder="Nhập lại mật khẩu mới" />
          </div>
        </div>

        <div class="pt-4 border-t border-white/5 mt-2 flex justify-end">
          <button type="submit" :disabled="isLoading" class="bg-primary-container text-on-primary font-bold text-xs uppercase tracking-widest px-8 py-3 transition-transform active:scale-95 hover:bg-primary-fixed-dim rounded-sm disabled:opacity-60">
            {{ isLoading ? 'Đang cập nhật...' : 'Cập nhật mật khẩu' }}
          </button>
        </div>
      </form>
    </div>
  </section>
</template>
