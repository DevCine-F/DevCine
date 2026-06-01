<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="pt-32 pb-20 max-w-[1440px] mx-auto px-10">
    <!-- Page Title -->
    <header class="mb-12">
      <h1 class="font-headline text-5xl font-extrabold tracking-tight text-on-surface mb-2">Lịch Chiếu</h1>
      <p class="text-on-surface-variant font-medium">Khám phá các suất chiếu đặc sắc tại hệ thống DevCine hôm nay.</p>
    </header>
    
    <!-- Date Selector -->
    <section class="mb-16">
      <div class="flex items-center justify-between mb-6">
        <h2 class="font-headline text-sm font-bold uppercase tracking-[0.2em] text-primary-container">Chọn ngày chiếu</h2>
      </div>
      <div class="flex space-x-4 overflow-x-auto no-scrollbar pb-4">
        <!-- Active Date -->
        <button class="flex-shrink-0 w-24 h-24 flex flex-col items-center justify-center bg-primary-container text-on-primary rounded-sm border-none shadow-lg shadow-primary-container/20">
          <span class="text-xs font-bold uppercase tracking-widest opacity-80">THỨ 2</span>
          <span class="text-3xl font-extrabold font-headline">15</span>
          <span class="text-xs font-bold uppercase tracking-tighter">TH 05</span>
        </button>
        <!-- Inactive Dates -->
        <button class="flex-shrink-0 w-24 h-24 flex flex-col items-center justify-center bg-surface-container-high text-on-surface-variant rounded-sm hover:bg-surface-container-highest transition-colors" v-for="n in 6" :key="n">
          <span class="text-xs font-bold uppercase tracking-widest">{{ ['THỨ 3', 'THỨ 4', 'THỨ 5', 'THỨ 6', 'THỨ 7', 'CN'][n-1] }}</span>
          <span class="text-3xl font-extrabold font-headline">{{ 15 + n }}</span>
          <span class="text-xs font-bold uppercase tracking-tighter">{{ n === 5 ? 'FULL' : 'TH 05' }}</span>
        </button>
      </div>
    </section>

    <!-- Main Showtimes Section -->
    <div class="space-y-16">
      <!-- Branch 1: Bitexco -->
      <section class="bg-surface-container-low p-8 rounded-sm">
        <div class="flex items-center justify-between mb-10 border-l-4 border-primary-container pl-6">
          <div>
            <h2 class="font-headline text-3xl font-bold text-on-surface">DevCine Bitexco Financial</h2>
            <p class="text-on-surface-variant text-sm mt-1">Lầu 3 & 4, Bitexco Financial Tower, Quận 1, TP. HCM</p>
          </div>
          <button class="flex items-center space-x-2 text-primary-container font-bold text-sm tracking-widest uppercase hover:opacity-80 transition-opacity">
            <span>Chi tiết rạp</span>
            <span class="material-symbols-outlined">arrow_forward</span>
          </button>
        </div>
        
        <div class="space-y-10">
          <!-- Movie Entry 1 -->
          <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
            <div class="lg:col-span-3">
              <div class="relative group overflow-hidden rounded-sm aspect-[2/3] shadow-xl">
                <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" src="/images/Hopper.webp"/>
                <span class="absolute top-0 left-0 bg-error-container text-white text-[10px] font-bold px-2 py-1 uppercase tracking-tighter">T16</span>
              </div>
            </div>
            <div class="lg:col-span-9 flex flex-col justify-center">
              <div class="mb-6">
                <h3 class="font-headline text-2xl font-bold text-on-surface mb-2">HÀNH TINH CỦA CÁC VỊ THẦN</h3>
                <div class="flex space-x-4 text-xs font-bold uppercase tracking-widest text-on-surface-variant">
                  <span>Hành Động, Viễn Tưởng</span>
                  <span>•</span>
                  <span>124 Phút</span>
                </div>
              </div>
              <div class="space-y-6">
                <div>
                  <span class="text-xs font-bold text-primary-container tracking-[0.15em] uppercase mb-3 block">Định dạng 2D Phụ đề</span>
                  <div class="flex flex-wrap gap-3">
                    <router-link to="/booking" class="px-6 py-2 border border-outline-variant/30 text-on-surface font-bold hover:bg-primary-container hover:text-on-primary transition-all duration-200">09:15</router-link>
                    <router-link to="/booking" class="px-6 py-2 border border-outline-variant/30 text-on-surface font-bold hover:bg-primary-container hover:text-on-primary transition-all duration-200">11:45</router-link>
                    <router-link to="/booking" class="px-6 py-2 border border-outline-variant/30 text-on-surface font-bold hover:bg-primary-container hover:text-on-primary transition-all duration-200">14:20</router-link>
                    <router-link to="/booking" class="px-6 py-2 border border-outline-variant/30 text-on-surface font-bold hover:bg-primary-container hover:text-on-primary transition-all duration-200">17:00</router-link>
                    <button class="px-6 py-2 border border-outline-variant/30 text-on-surface font-bold hover:bg-primary-container hover:text-on-primary transition-all duration-200 opacity-40 cursor-not-allowed">20:30</button>
                  </div>
                </div>
                <div>
                  <span class="text-xs font-bold text-tertiary-container tracking-[0.15em] uppercase mb-3 block">Định dạng IMAX LASER</span>
                  <div class="flex flex-wrap gap-3">
                    <router-link to="/booking" class="px-6 py-2 bg-tertiary-container/10 border border-tertiary-container/30 text-tertiary-container font-bold hover:bg-tertiary-container hover:text-on-tertiary transition-all duration-200">13:00</router-link>
                    <router-link to="/booking" class="px-6 py-2 bg-tertiary-container/10 border border-tertiary-container/30 text-tertiary-container font-bold hover:bg-tertiary-container hover:text-on-tertiary transition-all duration-200">19:45</router-link>
                    <router-link to="/booking" class="px-6 py-2 bg-tertiary-container/10 border border-tertiary-container/30 text-tertiary-container font-bold hover:bg-tertiary-container hover:text-on-tertiary transition-all duration-200">22:15</router-link>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Branch 2: Landmark 81 -->
      <section class="bg-surface-container-low p-8 rounded-sm">
        <div class="flex items-center justify-between mb-10 border-l-4 border-primary-container pl-6">
          <div>
            <h2 class="font-headline text-3xl font-bold text-on-surface">DevCine Landmark 81</h2>
            <p class="text-on-surface-variant text-sm mt-1">Tầng B1, Vincom Center Landmark 81, Quận Bình Thạnh, TP. HCM</p>
          </div>
          <button class="flex items-center space-x-2 text-primary-container font-bold text-sm tracking-widest uppercase hover:opacity-80 transition-opacity">
            <span>Chi tiết rạp</span>
            <span class="material-symbols-outlined">arrow_forward</span>
          </button>
        </div>
        
        <div class="space-y-10">
          <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
            <div class="lg:col-span-3">
              <div class="relative group overflow-hidden rounded-sm aspect-[2/3] shadow-xl">
                <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" src="/images/Hopper.webp"/>
                <span class="absolute top-0 left-0 bg-error-container text-white text-[10px] font-bold px-2 py-1 uppercase tracking-tighter">T18</span>
              </div>
            </div>
            <div class="lg:col-span-9 flex flex-col justify-center">
              <div class="mb-6">
                <h3 class="font-headline text-2xl font-bold text-on-surface mb-2">ĐÊM TRẮNG TẠI SAIGON</h3>
                <div class="flex space-x-4 text-xs font-bold uppercase tracking-widest text-on-surface-variant">
                  <span>Hành Động, Hình Sự</span>
                  <span>•</span>
                  <span>110 Phút</span>
                </div>
              </div>
              <div class="space-y-6">
                <div>
                  <span class="text-xs font-bold text-primary-container tracking-[0.15em] uppercase mb-3 block">Định dạng 2D Phụ đề</span>
                  <div class="flex flex-wrap gap-3">
                    <router-link to="/booking" class="px-6 py-2 border border-outline-variant/30 text-on-surface font-bold hover:bg-primary-container hover:text-on-primary transition-all duration-200">18:45</router-link>
                    <router-link to="/booking" class="px-6 py-2 border border-outline-variant/30 text-on-surface font-bold hover:bg-primary-container hover:text-on-primary transition-all duration-200">20:15</router-link>
                    <router-link to="/booking" class="px-6 py-2 border border-outline-variant/30 text-on-surface font-bold hover:bg-primary-container hover:text-on-primary transition-all duration-200">22:30</router-link>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Legend / Info -->
    <section class="mt-16 grid grid-cols-1 md:grid-cols-3 gap-8 py-10 border-t border-outline-variant/20">
      <div class="flex items-start space-x-4">
        <span class="material-symbols-outlined text-primary-container text-3xl">confirmation_number</span>
        <div>
          <h4 class="font-bold text-on-surface mb-1">Đặt vé trực tuyến</h4>
          <p class="text-xs text-on-surface-variant leading-relaxed">Tiết kiệm thời gian, chọn chỗ ngồi đẹp nhất và nhận vé ngay qua ứng dụng.</p>
        </div>
      </div>
      <div class="flex items-start space-x-4">
        <span class="material-symbols-outlined text-primary-container text-3xl">info</span>
        <div>
          <h4 class="font-bold text-on-surface mb-1">Quy định phân loại</h4>
          <p class="text-xs text-on-surface-variant leading-relaxed">Vui lòng kiểm tra độ tuổi yêu cầu của phim trước khi đặt vé để đảm bảo trải nghiệm tốt nhất.</p>
        </div>
      </div>
      <div class="flex items-start space-x-4">
        <span class="material-symbols-outlined text-primary-container text-3xl">loyalty</span>
        <div>
          <h4 class="font-bold text-on-surface mb-1">Ưu đãi thành viên</h4>
          <p class="text-xs text-on-surface-variant leading-relaxed">Tích lũy điểm DevPoints cho mỗi giao dịch và đổi nhiều phần quà hấp dẫn.</p>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>
