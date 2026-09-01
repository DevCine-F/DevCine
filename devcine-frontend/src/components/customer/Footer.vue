<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import logo from '../../assets/images/Logo_DevCine_Ngang_XoaNen.png'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
const newsletterEmail = ref('')

const handleSubscribe = () => {
  const emailVal = (newsletterEmail.value || '').trim()
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if (!emailVal) {
    toast.error('Vui lòng nhập địa chỉ email của bạn.')
    return
  }
  if (!emailRegex.test(emailVal)) {
    toast.error('Email không đúng định dạng. Vui lòng kiểm tra lại.')
    return
  }

  toast.success('Đăng ký nhận bản tin thành công! Cảm ơn bạn.')
  newsletterEmail.value = ''
}
</script>

<template>
  <footer class="bg-[#0e0e0e] w-full pt-8 sm:pt-10 md:pt-12 pb-6 sm:pb-8 px-4 sm:px-6 md:px-10 border-t border-[#4e4633]/20 rounded-t-[24px] sm:rounded-t-[32px]">
    <div class="max-w-[1440px] mx-auto grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6 md:gap-8 lg:gap-10">
      <!-- Cột 1: Logo & Thông tin liên hệ -->
      <div class="col-span-1 sm:col-span-2 md:col-span-1">
        <router-link to="/" class="inline-block -mt-2.5 sm:-mt-3.5 mb-2.5">
          <img :src="logo" alt="DEVCINE" class="w-full max-w-[170px] sm:max-w-[190px] h-auto object-contain brightness-110">
        </router-link>
        <p class="text-neutral-500 font-label text-xs leading-relaxed mb-3 max-w-sm">
          Trải nghiệm điện ảnh thượng lưu. Từng khung hình là một tác phẩm nghệ thuật.
        </p>
        <div class="text-[11px] text-neutral-500 font-label space-y-0.5 mb-3.5">
          <p>Hotline: <a href="tel:19002026" class="text-neutral-400 hover:text-primary-container transition-colors">1900 2026</a> (08:00 - 23:00)</p>
          <p>Email: <a href="mailto:cskh@devcine.vn" class="text-neutral-400 hover:text-primary-container transition-colors">cskh@devcine.vn</a></p>
        </div>
        <div class="flex items-center space-x-3">
          <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" class="text-neutral-500 hover:text-primary-container transition-colors" title="Facebook">
            <span class="material-symbols-outlined text-lg">share</span>
          </a>
          <a href="https://youtube.com" target="_blank" rel="noopener noreferrer" class="text-neutral-500 hover:text-primary-container transition-colors" title="YouTube">
            <span class="material-symbols-outlined text-lg">play_circle</span>
          </a>
          <a href="mailto:cskh@devcine.vn" class="text-neutral-500 hover:text-primary-container transition-colors" title="Email">
            <span class="material-symbols-outlined text-lg">mail</span>
          </a>
        </div>
      </div>

      <!-- Cột 2: THÔNG TIN -->
      <div>
        <h4 class="font-headline text-white text-xs font-bold uppercase mb-3 sm:mb-4 tracking-wider">THÔNG TIN</h4>
        <ul class="space-y-2 sm:space-y-2.5 text-xs sm:text-[13px] font-label text-neutral-500">
          <li>
            <router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" :to="{ path: '/', hash: '#about' }">
              Về chúng tôi
            </router-link>
          </li>
          <li>
            <router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" to="/lich-chieu">
              Lịch chiếu
            </router-link>
          </li>
          <li>
            <router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" to="/he-thong-rap">
              Hệ thống rạp
            </router-link>
          </li>
          <li>
            <router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" to="/khuyen-mai">
              Khuyến mãi
            </router-link>
          </li>
          <li>
            <router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" to="/contact">
              Liên hệ
            </router-link>
          </li>
        </ul>
      </div>

      <!-- Cột 3: ĐIỀU KHOẢN -->
      <div>
        <h4 class="font-headline text-white text-xs font-bold uppercase mb-3 sm:mb-4 tracking-wider">ĐIỀU KHOẢN</h4>
        <ul class="space-y-2 sm:space-y-2.5 text-xs sm:text-[13px] font-label text-neutral-500">
          <li>
            <router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" to="/faq">
              Điều khoản sử dụng
            </router-link>
          </li>
          <li>
            <router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" to="/faq">
              Chính sách bảo mật
            </router-link>
          </li>
          <li>
            <router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" to="/faq">
              Chính sách đổi trả & hoàn vé
            </router-link>
          </li>
          <li>
            <router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" to="/faq">
              Câu hỏi thường gặp
            </router-link>
          </li>
        </ul>
      </div>

      <!-- Cột 4: BẢN TIN -->
      <div>
        <h4 class="font-headline text-white text-xs font-bold uppercase mb-3 sm:mb-4 tracking-wider">BẢN TIN</h4>
        <p class="text-xs text-neutral-500 mb-3">Đăng ký nhận thông báo về phim mới và khuyến mãi.</p>
        <form @submit.prevent="handleSubscribe" class="flex">
          <input
            v-model="newsletterEmail"
            class="bg-surface-container-high border-none text-xs text-white px-3.5 py-2 w-full rounded-l-lg focus:ring-1 focus:ring-primary-container placeholder-neutral-500"
            placeholder="Email của bạn"
            type="email"
          />
          <button
            type="submit"
            class="bg-primary-container text-on-primary px-3.5 py-2 rounded-r-lg transition-all hover:opacity-80 flex items-center justify-center cursor-pointer flex-shrink-0"
          >
            <span class="material-symbols-outlined text-sm">send</span>
          </button>
        </form>
      </div>
    </div>

    <!-- Sub-footer -->
    <div class="max-w-[1440px] mx-auto mt-8 sm:mt-10 pt-4 sm:pt-5 border-t border-neutral-800/80 flex flex-col sm:flex-row justify-between items-center gap-3 text-center sm:text-left text-[10px] text-neutral-600 uppercase tracking-wider">
      <p>DevCine &bull; SD-07 &bull; DATN</p>
      <div class="flex flex-wrap justify-center gap-4 sm:space-x-6 sm:gap-0">
        <router-link to="/style-guide" class="hover:text-primary-container transition-colors">STYLE GUIDE</router-link>
        <router-link to="/contact" class="hover:text-primary-container transition-colors">HỖ TRỢ</router-link>
        <router-link to="/faq" class="hover:text-primary-container transition-colors">CHÍNH SÁCH</router-link>
      </div>
    </div>
  </footer>
</template>



