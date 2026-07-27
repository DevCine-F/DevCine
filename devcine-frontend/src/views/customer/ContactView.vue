<script setup>
import { RouterLink } from 'vue-router'
import { onMounted, reactive, ref } from 'vue'
import { supportApi } from '@/api/customer'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const authStore = useAuthStore()
const toast = useToastStore()
const showToast = (message, type = 'success') => toast.push(message, type)

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

const handleScrollToForm = () => {
  document.getElementById('contact-form')?.scrollIntoView({ behavior: 'smooth' })
}

const handleSubmit = async () => {
  if (!authStore.isAuthenticated || !authStore.user?.id) {
    showToast('Vui lòng đăng nhập để gửi yêu cầu hỗ trợ.', 'error')
    return
  }
  if (!form.message.trim()) {
    showToast('Vui lòng nhập nội dung tin nhắn.', 'error')
    return
  }

  submitting.value = true
  try {
    // Backend lấy tên/email từ tài khoản; SĐT gửi thành field riêng cho CSKH
    await supportApi.createTicket({
      customerId: authStore.user.id,
      issueType: form.issueType,
      phone: form.phone.trim(),
      description: form.message.trim()
    })
    showToast('Đã gửi yêu cầu! Bộ phận CSKH sẽ phản hồi sớm.', 'success')
    form.message = ''
  } catch (err) {
    showToast(friendlyError(err, 'Gửi yêu cầu thất bại. Vui lòng thử lại.'), 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main>
    <!-- Hero Header -->
    <section class="relative min-h-[540px] pt-32 flex items-end px-6 md:px-10 pb-16 overflow-hidden">
      <div class="absolute inset-0 z-0">
        <img src="/images/Hopper.webp" class="w-full h-full object-cover opacity-40"/>
        <div class="absolute inset-0 hero-gradient"></div>
      </div>
      <div class="relative z-10 max-w-[1440px] mx-auto w-full">
        <h1 class="text-5xl md:text-7xl font-headline font-extrabold tracking-tighter text-on-background mb-4 uppercase">
          Liên hệ & <span class="text-primary-container">Hỗ trợ</span>
        </h1>
        <p class="max-w-2xl text-lg text-on-surface-variant font-body leading-relaxed">
          Tại DevCine, sự hài lòng của quý khách là ưu tiên hàng đầu. Chúng tôi luôn sẵn sàng lắng nghe và hỗ trợ mọi thắc mắc để mang lại trải nghiệm điện ảnh hoàn hảo nhất.
        </p>
      </div>
    </section>

    <div class="max-w-[1440px] mx-auto px-6 md:px-10">
      <!-- Support Categories -->
      <section class="py-20">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <router-link to="/faq" class="glass-card glass-shine-edge p-8 rounded-3xl hover:border-primary-container/30 transition-all duration-300 group">
            <span class="material-symbols-outlined text-4xl text-primary-container mb-6 block">help_outline</span>
            <h3 class="text-xl font-headline font-bold text-on-background mb-2">Câu hỏi thường gặp</h3>
            <p class="text-on-surface-variant text-sm font-body mb-4">Tìm câu trả lời nhanh nhất cho các thắc mắc về quy trình đặt vé và rạp.</p>
            <span class="text-primary-container font-label text-xs uppercase tracking-widest flex items-center gap-2 group-hover:gap-4 transition-all">Xem thêm <span class="material-symbols-outlined text-sm">arrow_forward</span></span>
          </router-link>
          <router-link to="/faq" class="glass-card glass-shine-edge p-8 rounded-3xl hover:border-primary-container/30 transition-all duration-300 group">
            <span class="material-symbols-outlined text-4xl text-primary-container mb-6 block">confirmation_number</span>
            <h3 class="text-xl font-headline font-bold text-on-background mb-2">Chính sách vé</h3>
            <p class="text-on-surface-variant text-sm font-body mb-4">Thông tin chi tiết về dịch vụ vé và các quy định về độ tuổi xem phim.</p>
            <span class="text-primary-container font-label text-xs uppercase tracking-widest flex items-center gap-2 group-hover:gap-4 transition-all">Xem thêm <span class="material-symbols-outlined text-sm">arrow_forward</span></span>
          </router-link>
          <router-link to="/khuyen-mai" class="glass-card glass-shine-edge p-8 rounded-3xl hover:border-primary-container/30 transition-all duration-300 group">
            <span class="material-symbols-outlined text-4xl text-primary-container mb-6 block">loyalty</span>
            <h3 class="text-xl font-headline font-bold text-on-background mb-2">Thành viên & Ưu đãi</h3>
            <p class="text-on-surface-variant text-sm font-body mb-4">Quản lý tài khoản DevCine và khám phá các đặc quyền dành riêng cho bạn.</p>
            <span class="text-primary-container font-label text-xs uppercase tracking-widest flex items-center gap-2 group-hover:gap-4 transition-all">Xem thêm <span class="material-symbols-outlined text-sm">arrow_forward</span></span>
          </router-link>
          <button type="button" @click="handleScrollToForm" class="glass-card glass-shine-edge p-8 rounded-3xl hover:border-primary-container/30 transition-all duration-300 group text-left">
            <span class="material-symbols-outlined text-4xl text-primary-container mb-6 block">rate_review</span>
            <h3 class="text-xl font-headline font-bold text-on-background mb-2">Phản hồi dịch vụ</h3>
            <p class="text-on-surface-variant text-sm font-body mb-4">Góp ý về chất lượng phục vụ tại các cụm rạp để chúng tôi ngày càng hoàn thiện.</p>
            <span class="text-primary-container font-label text-xs uppercase tracking-widest flex items-center gap-2 group-hover:gap-4 transition-all">Gửi góp ý <span class="material-symbols-outlined text-sm">arrow_forward</span></span>
          </button>
        </div>
      </section>

      <!-- Main Support Section -->
      <section class="grid grid-cols-1 lg:grid-cols-12 gap-16 pb-20">
        <!-- Contact Form -->
        <div id="contact-form" class="lg:col-span-7 scroll-mt-32">
          <h2 class="text-3xl font-headline font-bold text-on-background mb-8 uppercase tracking-tight">Gửi lời nhắn cho chúng tôi</h2>

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
            <button :disabled="submitting" class="w-full md:w-auto px-12 py-4 bg-primary-container text-on-primary font-headline font-extrabold uppercase tracking-widest rounded-sm hover:brightness-110 transition-all disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2" type="submit">
              <span v-if="submitting" class="material-symbols-outlined animate-spin text-xl">progress_activity</span>
              {{ submitting ? 'Đang gửi...' : 'Gửi yêu cầu' }}
            </button>
          </form>
        </div>

        <!-- Theater Contact Info -->
        <div class="lg:col-span-5">
          <h2 class="text-3xl font-headline font-bold text-on-background mb-8 uppercase tracking-tight">Hệ thống rạp</h2>
          <div class="space-y-8">
            <div class="flex gap-6 items-start">
              <div class="w-12 h-12 glass-card flex items-center justify-center rounded-xl flex-shrink-0">
                <span class="material-symbols-outlined text-primary-container">location_on</span>
              </div>
              <div>
                <h4 class="text-lg font-headline font-bold text-primary-container mb-1 uppercase tracking-tighter">DEVCINE Landmark 81</h4>
                <p class="text-on-surface-variant font-body text-sm mb-2">Tầng B1, Vincom Center Landmark 81, 720A Điện Biên Phủ, P. 22, Q. Bình Thạnh, TP. HCM</p>
                <p class="text-on-background font-bold text-sm tracking-widest">HOTLINE: 1900 1234</p>
              </div>
            </div>
            <div class="flex gap-6 items-start">
              <div class="w-12 h-12 glass-card flex items-center justify-center rounded-xl flex-shrink-0">
                <span class="material-symbols-outlined text-primary-container">location_on</span>
              </div>
              <div>
                <h4 class="text-lg font-headline font-bold text-primary-container mb-1 uppercase tracking-tighter">DEVCINE Bitexco</h4>
                <p class="text-on-surface-variant font-body text-sm mb-2">Tầng 3-4, Bitexco Financial Tower, 02 Hải Triều, Q. 1, TP. HCM</p>
                <p class="text-on-background font-bold text-sm tracking-widest">HOTLINE: 1900 5678</p>
              </div>
            </div>
          </div>

          <!-- FAQ Quick Links -->
          <div class="mt-16 glass-card glass-shine-edge p-8 rounded-3xl">
            <h4 class="text-xs font-label uppercase tracking-[0.2em] text-on-surface-variant mb-6 border-b border-outline-variant/20 pb-4">Liên kết nhanh</h4>
            <ul class="space-y-4">
              <li><router-link to="/faq" class="flex items-center justify-between text-on-background hover:text-primary-container transition-colors font-body font-semibold">Làm thế nào để đặt vé trực tuyến? <span class="material-symbols-outlined text-lg">chevron_right</span></router-link></li>
              <li><router-link to="/faq" class="flex items-center justify-between text-on-background hover:text-primary-container transition-colors font-body font-semibold">Quy định về thẻ thành viên DevCine <span class="material-symbols-outlined text-lg">chevron_right</span></router-link></li>
              <li><router-link to="/lich-chieu" class="flex items-center justify-between text-on-background hover:text-primary-container transition-colors font-body font-semibold">Lịch chiếu phim tuần này <span class="material-symbols-outlined text-lg">chevron_right</span></router-link></li>
            </ul>
          </div>
        </div>
      </section>
    </div>

  </main>
</template>

<style scoped>
.hero-gradient { background: linear-gradient(180deg, transparent 0%, rgba(10,10,15,0.4) 100%); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
