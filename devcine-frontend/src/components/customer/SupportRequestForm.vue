<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { supportApi } from '@/api/customer'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const emit = defineEmits(['submitted'])

// Nút submit trải full chiều rộng (dùng khi form nằm trong cột hẹp / căn giữa, vd FAQ inline).
defineProps({
  fullWidthSubmit: { type: Boolean, default: false }
})

const authStore = useAuthStore()
const toast = useToastStore()

// Ánh xạ chủ đề hiển thị -> mã issueType lưu ở backend
const SUBJECTS = [
  { value: 'TICKET', label: 'Vấn đề về vé' },
  { value: 'MEMBERSHIP', label: 'Thành viên' },
  { value: 'SERVICE', label: 'Góp ý dịch vụ' },
  { value: 'PARTNERSHIP', label: 'Hợp tác quảng cáo' }
]

const form = reactive({
  fullName: '',
  email: '',
  phone: '',
  issueType: SUBJECTS[0].value,
  message: ''
})

const submitting = ref(false)

onMounted(() => {
  // Điền sẵn thông tin nếu đã đăng nhập
  if (authStore.user) {
    form.fullName = authStore.user.fullName || ''
    form.email = authStore.user.email || ''
    form.phone = authStore.user.phone || ''
  }
})

const handleSubmit = async () => {
  if (!authStore.isAuthenticated || !authStore.user?.id) {
    toast.push('Vui lòng đăng nhập để gửi yêu cầu hỗ trợ.', 'error')
    return
  }
  if (!form.message.trim()) {
    toast.push('Vui lòng nhập nội dung tin nhắn.', 'error')
    return
  }

  submitting.value = true
  try {
    await supportApi.createTicket({
      customerId: authStore.user.id,
      issueType: form.issueType,
      phone: form.phone.trim(),
      description: form.message.trim()
    })
    toast.push('Đã gửi yêu cầu! Bộ phận CSKH sẽ phản hồi sớm.', 'success')
    form.message = ''
    emit('submitted')
  } catch (err) {
    toast.push(friendlyError(err, 'Gửi yêu cầu thất bại. Vui lòng thử lại.'), 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div>
    <div v-if="!authStore.isAuthenticated" class="glass-card glass-shine-edge p-5 rounded-2xl mb-6 flex items-center gap-3 text-sm text-on-surface-variant">
      <span class="material-symbols-outlined text-primary-container">info</span>
      <span>Vui lòng <router-link to="/login" class="text-primary-container font-semibold hover:underline">đăng nhập</router-link> để gửi yêu cầu hỗ trợ tới bộ phận CSKH.</span>
    </div>

    <form class="space-y-6" @submit.prevent="handleSubmit">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="space-y-2">
          <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Họ và tên</label>
          <input v-model="form.fullName" class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all" placeholder="Nguyễn Văn A" type="text"/>
        </div>
        <div class="space-y-2">
          <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Email</label>
          <input v-model="form.email" class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all" placeholder="email@example.com" type="email"/>
        </div>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="space-y-2">
          <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Số điện thoại</label>
          <input v-model="form.phone" class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all" placeholder="090 123 4567" type="tel"/>
        </div>
        <div class="space-y-2">
          <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Chủ đề</label>
          <select v-model="form.issueType" class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all">
            <option v-for="s in SUBJECTS" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>
      </div>
      <div class="space-y-2">
        <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Nội dung tin nhắn</label>
        <textarea v-model="form.message" class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all" placeholder="Vui lòng mô tả chi tiết yêu cầu của bạn..." rows="5"></textarea>
      </div>
      <button :disabled="submitting" :class="fullWidthSubmit ? 'w-full' : 'w-full md:w-auto'" class="px-12 py-4 bg-primary-container text-on-primary font-headline font-extrabold uppercase tracking-widest rounded-sm hover:brightness-110 transition-all disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2" type="submit">
        <span v-if="submitting" class="material-symbols-outlined animate-spin text-xl">progress_activity</span>
        {{ submitting ? 'Đang gửi...' : 'Gửi yêu cầu' }}
      </button>
    </form>
  </div>
</template>
