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

// ===== CAPTCHA tự sinh bằng canvas (không gọi API) =====
// Bỏ ký tự dễ nhầm (0/O, 1/I/L) cho dễ đọc.
const CAPTCHA_CHARS = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789'
const captchaCanvas = ref(null)
const captchaCode = ref('')        // mã gốc
const userInputCaptcha = ref('')   // mã khách nhập

const drawCaptcha = () => {
  const canvas = captchaCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const { width, height } = canvas
  ctx.clearRect(0, 0, width, height)

  // Sinh mã 5 ký tự
  let code = ''
  for (let i = 0; i < 5; i++) code += CAPTCHA_CHARS[Math.floor(Math.random() * CAPTCHA_CHARS.length)]
  captchaCode.value = code

  // Noise: vài đường mảnh vàng nhạt
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(245,197,24,${0.1 + Math.random() * 0.12})`
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }
  // Noise: chấm mờ
  for (let i = 0; i < 28; i++) {
    ctx.fillStyle = `rgba(255,255,255,${Math.random() * 0.1})`
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, Math.random() * 1.1, 0, Math.PI * 2)
    ctx.fill()
  }
  // Vẽ từng ký tự: màu vàng primary-container, xoay/xê dịch nhẹ
  ctx.textBaseline = 'middle'
  const step = (width - 24) / code.length
  for (let i = 0; i < code.length; i++) {
    ctx.save()
    ctx.translate(16 + i * step, height / 2 + (Math.random() * 6 - 3))
    ctx.rotate(Math.random() * 0.5 - 0.25)
    ctx.font = `bold ${21 + Math.floor(Math.random() * 5)}px "Poppins", system-ui, sans-serif`
    ctx.fillStyle = '#f5c518'
    ctx.fillText(code[i], 0, 0)
    ctx.restore()
  }
}

const refreshCaptcha = () => {
  userInputCaptcha.value = ''
  drawCaptcha()
}

onMounted(() => {
  // Điền sẵn thông tin nếu đã đăng nhập
  if (authStore.user) {
    form.fullName = authStore.user.fullName || ''
    form.email = authStore.user.email || ''
    form.phone = authStore.user.phone || ''
  }
  drawCaptcha()
})

// ===== Chặn ký tự ngay khi gõ (@input) — kèm khoá độ dài dự phòng =====
const onFullNameInput = (e) => {
  // chỉ chữ cái (kể cả tiếng Việt có dấu) + dấu cách; tối đa 30
  form.fullName = e.target.value.replace(/[^a-zA-ZÀ-ỹ\s]/g, '').slice(0, 30)
}
const onEmailInput = (e) => {
  // bỏ khoảng trắng + dấu tiếng Việt + ký tự lạ (chỉ giữ ký tự hợp lệ của email), về chữ thường; tối đa 30
  form.email = e.target.value.replace(/[^a-zA-Z0-9@._%+\-]/g, '').toLowerCase().slice(0, 30)
}
const onPhoneInput = (e) => {
  // chỉ số 0-9; tối đa 10
  form.phone = e.target.value.replace(/\D/g, '').slice(0, 10)
}
const onCaptchaInput = (e) => {
  // chỉ chữ và số, viết hoa cho khớp mã trên ảnh; tối đa 5
  userInputCaptcha.value = e.target.value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 5)
}

// ===== Validate lúc Submit =====
const validateForm = () => {
  const name = form.fullName.trim()
  if (name.length < 2) { toast.push('Họ và tên phải có ít nhất 2 ký tự.', 'error'); return false }

  const email = form.email.trim()
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { toast.push('Email không hợp lệ.', 'error'); return false }

  const phone = form.phone.trim()
  if (!/^0\d{9}$/.test(phone)) { toast.push('Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.', 'error'); return false }

  if (!SUBJECTS.some(s => s.value === form.issueType)) { toast.push('Vui lòng chọn chủ đề.', 'error'); return false }

  const msg = form.message.trim()
  if (msg.length < 10) { toast.push('Nội dung tin nhắn phải có ít nhất 10 ký tự.', 'error'); return false }
  if (msg.length > 1000) { toast.push('Nội dung tin nhắn tối đa 1000 ký tự.', 'error'); return false }

  return true
}

const handleSubmit = async () => {
  if (!authStore.isAuthenticated || !authStore.user?.id) {
    toast.push('Vui lòng đăng nhập để gửi yêu cầu hỗ trợ.', 'error')
    return
  }
  if (!validateForm()) return
  // Kiểm tra CAPTCHA (không phân biệt hoa/thường)
  if (userInputCaptcha.value.trim().toLowerCase() !== captchaCode.value.toLowerCase()) {
    toast.push('Mã xác nhận không chính xác', 'error')
    refreshCaptcha()
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
    refreshCaptcha()
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
          <input v-model="form.fullName" @input="onFullNameInput" maxlength="30" class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all" placeholder="Nguyễn Văn A" type="text"/>
        </div>
        <div class="space-y-2">
          <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Email</label>
          <input v-model="form.email" @input="onEmailInput" maxlength="30" class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all" placeholder="email@example.com" type="email"/>
        </div>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="space-y-2">
          <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Số điện thoại</label>
          <input v-model="form.phone" @input="onPhoneInput" maxlength="10" inputmode="numeric" class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all" placeholder="0901234567" type="tel"/>
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
        <textarea v-model="form.message" maxlength="1000" class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all" placeholder="Vui lòng mô tả chi tiết yêu cầu của bạn... (tối thiểu 10 ký tự)" rows="5"></textarea>
      </div>

      <!-- CAPTCHA tự sinh: ảnh canvas + nút refresh | ô nhập -->
      <div class="space-y-2">
        <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Mã xác nhận</label>
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2 shrink-0">
            <canvas ref="captchaCanvas" width="120" height="46" class="bg-white/5 rounded-lg h-[46px] w-[120px] select-none"></canvas>
            <button type="button" @click="refreshCaptcha" title="Đổi mã khác" aria-label="Đổi mã khác"
                    class="p-2 text-on-surface-variant hover:text-primary-container transition-colors">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992V4.356M2.985 19.644v-4.992h4.992m10.032-4.665a8.25 8.25 0 00-13.803-3.7L2.985 9.348m0 0V4.356m0 4.992h4.992M2.985 14.652a8.25 8.25 0 0013.803 3.7l3.181-3.182m0 0h-4.991m4.991 0v4.992"/>
              </svg>
            </button>
          </div>
          <input v-model="userInputCaptcha" @input="onCaptchaInput" type="text" maxlength="5" autocomplete="off"
                 class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all tracking-[0.3em] uppercase"
                 placeholder="Nhập mã bên cạnh"/>
        </div>
      </div>

      <button :disabled="submitting" :class="fullWidthSubmit ? 'w-full' : 'w-full md:w-auto'" class="px-12 py-4 bg-primary-container text-on-primary font-headline font-extrabold uppercase tracking-widest rounded-sm hover:brightness-110 transition-all disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2" type="submit">
        <span v-if="submitting" class="material-symbols-outlined animate-spin text-xl">progress_activity</span>
        {{ submitting ? 'Đang gửi...' : 'Gửi yêu cầu' }}
      </button>
    </form>
  </div>
</template>
