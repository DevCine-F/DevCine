<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/customer/index'

const authStore = useAuthStore()
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const isLoading = ref(false)
const successMsg = ref('')
const errorMsg = ref('')

const handleSubmit = async () => {
  errorMsg.value = ''
  successMsg.value = ''

  if (!oldPassword.value || !newPassword.value || !confirmPassword.value) {
    errorMsg.value = 'Vui lòng điền đầy đủ thông tin.'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMsg.value = 'Mật khẩu mới và xác nhận mật khẩu không khớp.'
    return
  }
  if (newPassword.value.length < 6) {
    errorMsg.value = 'Mật khẩu mới phải có ít nhất 6 ký tự.'
    return
  }

  isLoading.value = true
  try {
    await authApi.changePassword(authStore.user.id, oldPassword.value, newPassword.value)
    successMsg.value = 'Đổi mật khẩu thành công!'
    oldPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } catch (err) {
    errorMsg.value = err.response?.data?.message || 'Đổi mật khẩu thất bại. Kiểm tra lại mật khẩu hiện tại.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <section class="max-w-2xl">
    <h2 class="text-2xl font-bold tracking-tight font-headline mb-8">Đổi mật khẩu</h2>

    <div class="bg-surface-container-low p-8 rounded-lg border border-white/5">
      <!-- Success -->
      <div v-if="successMsg" class="mb-6 px-4 py-3 bg-green-500/10 border border-green-500/20 rounded text-green-400 text-sm font-semibold flex items-center gap-2">
        <span class="material-symbols-outlined text-base">check_circle</span>
        {{ successMsg }}
      </div>
      <!-- Error -->
      <div v-if="errorMsg" class="mb-6 px-4 py-3 bg-red-500/10 border border-red-500/20 rounded text-red-400 text-sm font-semibold flex items-center gap-2">
        <span class="material-symbols-outlined text-base">error</span>
        {{ errorMsg }}
      </div>

      <form @submit.prevent="handleSubmit" class="flex flex-col gap-6">
        <div>
          <label class="block text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-2">Mật khẩu hiện tại</label>
          <input v-model="oldPassword" type="password" class="w-full bg-surface-container-highest border-none text-sm text-white px-4 py-3 rounded focus:ring-1 focus:ring-primary-container" placeholder="Nhập mật khẩu hiện tại" />
        </div>

        <div>
          <label class="block text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-2">Mật khẩu mới</label>
          <input v-model="newPassword" type="password" class="w-full bg-surface-container-highest border-none text-sm text-white px-4 py-3 rounded focus:ring-1 focus:ring-primary-container" placeholder="Nhập mật khẩu mới (ít nhất 6 ký tự)" />
        </div>

        <div>
          <label class="block text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-2">Xác nhận mật khẩu mới</label>
          <input v-model="confirmPassword" type="password" class="w-full bg-surface-container-highest border-none text-sm text-white px-4 py-3 rounded focus:ring-1 focus:ring-primary-container" placeholder="Nhập lại mật khẩu mới" />
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
