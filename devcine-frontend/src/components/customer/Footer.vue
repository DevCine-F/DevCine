<script setup>
import logo from '../../assets/images/Logo_DevCine_Ngang_XoaNen.png'
</script>

<template>
  <footer class="bg-[#0e0e0e] w-full py-20 px-10 border-t border-[#4e4633]/20 rounded-t-[40px]">
    <div class="max-w-[1440px] mx-auto grid grid-cols-1 md:grid-cols-4 gap-12">
      <div class="col-span-1">
        <router-link to="/" class="block mb-6">
          <img :src="logo" alt="DEVCINE" class="w-full max-w-[260px] h-auto object-contain brightness-110">
        </router-link>
        <p class="text-neutral-500 font-label text-sm leading-relaxed mb-6">
          Trải nghiệm điện ảnh thượng lưu. Từng khung hình là một tác phẩm nghệ thuật.
        </p>
        <div class="flex space-x-4">
          <span class="material-symbols-outlined text-neutral-500 hover:text-primary-container cursor-pointer">social_leaderboard</span>
          <span class="material-symbols-outlined text-neutral-500 hover:text-primary-container cursor-pointer">camera</span>
          <span class="material-symbols-outlined text-neutral-500 hover:text-primary-container cursor-pointer">play_circle</span>
        </div>
      </div>
      <div>
        <h4 class="font-headline text-white text-xs font-bold uppercase mb-6 tracking-widest">THÔNG TIN</h4>
        <ul class="space-y-4 text-sm font-label text-neutral-500">
          <li><router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" to="/about">Về chúng tôi</router-link></li>
          <li><router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" to="/he-thong-rap">Hệ thống rạp</router-link></li>
          <li><router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" to="/contact">Liên hệ</router-link></li>
        </ul>
      </div>
      <div>
        <h4 class="font-headline text-white text-xs font-bold uppercase mb-6 tracking-widest">ĐIỀU KHOẢN</h4>
        <ul class="space-y-4 text-sm font-label text-neutral-500">
          <li><a class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" href="#">Điều khoản sử dụng</a></li>
          <li><a class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" href="#">Chính sách bảo mật</a></li>
          <li><router-link class="hover:text-[#f5c518] underline-offset-4 hover:underline transition-all" to="/faq">Câu hỏi thường gặp</router-link></li>
        </ul>
      </div>
      <div>
        <h4 class="font-headline text-white text-xs font-bold uppercase mb-6 tracking-widest">BẢN TIN</h4>
        <p class="text-xs text-neutral-500 mb-4">Đăng ký nhận thông báo về phim mới và khuyến mãi.</p>
        <div class="flex">
          <input class="bg-surface-container-high border-none text-xs text-white px-4 py-3 w-full rounded-l-lg focus:ring-1 focus:ring-primary-container" placeholder="Email của bạn" type="email"/>
          <button class="bg-primary-container text-on-primary px-4 py-3 rounded-r-lg transition-all hover:opacity-80">
            <span class="material-symbols-outlined text-sm">send</span>
          </button>
        </div>
      </div>
    </div>
    <div class="max-w-[1440px] mx-auto mt-20 pt-8 border-t border-neutral-800 flex flex-col md:flex-row justify-between items-center text-[10px] text-neutral-600 uppercase tracking-widest">
      <p>DevCine &bull; SD-07 &bull; DATN</p>
      <div class="flex space-x-6 mt-4 md:mt-0">
        <router-link to="/style-guide" class="hover:text-primary-container transition-colors">STYLE GUIDE</router-link>
        <span>DIVERSITY & INCLUSION</span>
        <span>ACCESSIBILITY</span>
      </div>
    </div>
  </footer>
</template>
