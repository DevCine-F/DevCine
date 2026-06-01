<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main>
    <!-- Hero Section -->
    <section class="relative h-[921px] w-full flex items-end overflow-hidden">
      <div class="absolute inset-0 z-0">
        <img class="w-full h-full object-cover" src="/images/Hopper.webp"/>
        <div class="absolute inset-0 hero-gradient"></div>
      </div>
      <div class="relative z-10 max-w-[1440px] mx-auto px-10 pb-20 w-full grid grid-cols-1 md:grid-cols-12 gap-12 items-end">
        <div class="md:col-span-3 hidden md:block">
          <div class="relative aspect-[2/3] shadow-2xl overflow-hidden rounded-sm group">
            <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105" src="/images/Hopper.webp"/>
            <div class="absolute top-0 left-0 bg-error-container text-on-error-container px-3 py-1 font-label text-xs font-bold">T16</div>
          </div>
        </div>
        <div class="md:col-span-9">
          <div class="flex items-center gap-4 mb-4">
            <span class="bg-surface-container-high px-3 py-1 rounded-sm text-[10px] font-bold tracking-widest text-primary-container uppercase">Science Fiction</span>
            <span class="text-on-surface-variant font-label text-xs tracking-widest uppercase">128 Phút</span>
          </div>
          <h1 class="text-[5rem] md:text-[8rem] font-headline font-extrabold leading-none tracking-tighter mb-6 editorial-shadow text-on-surface uppercase">Hoppers</h1>
          <div class="flex flex-wrap gap-4 items-center">
            <router-link to="/booking" class="bg-primary-container text-on-primary px-10 py-4 font-headline font-bold tracking-wider hover:opacity-90 transition-all flex items-center gap-3 scale-100 active:scale-95">
              <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
              ĐẶT VÉ NGAY
            </router-link>
            <button class="border border-outline-variant/30 text-on-surface px-10 py-4 font-headline font-bold tracking-wider hover:bg-primary/10 transition-all flex items-center gap-3">
              <span class="material-symbols-outlined">add</span>
              DANH SÁCH CỦA TÔI
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- Content Details -->
    <section class="max-w-[1440px] mx-auto px-10 py-24 grid grid-cols-1 lg:grid-cols-3 gap-20">
      <div class="lg:col-span-2 space-y-12">
        <div>
          <h2 class="text-2xl font-headline font-bold mb-6 text-primary-container tracking-tight uppercase">Nội dung phim</h2>
          <p class="text-on-surface/80 text-lg leading-relaxed font-body">
            Trong một tương lai gần nơi ý thức có thể được chuyển đổi giữa các cơ thể sinh học khác nhau, một nhóm thợ săn tiền thưởng nghiệp dư phát hiện ra một âm mưu đen tối đe dọa sự tồn vong của nhân loại. Hoppers không chỉ là một cuộc rượt đuổi nghẹt thở mà còn là bài toán về đạo đức và bản sắc cá nhân trong kỷ nguyên công nghệ số.
          </p>
        </div>
        <div>
          <h2 class="text-2xl font-headline font-bold mb-8 text-primary-container tracking-tight uppercase">Trailer & Video</h2>
          <div class="relative aspect-video bg-surface-container-lowest overflow-hidden group cursor-pointer">
            <img class="w-full h-full object-cover opacity-50 group-hover:opacity-60 transition-opacity" src="/images/Hopper.webp"/>
            <div class="absolute inset-0 flex items-center justify-center">
              <div class="w-20 h-20 bg-primary-container/20 backdrop-blur-md rounded-full flex items-center justify-center border border-primary-container/30 group-hover:scale-110 transition-transform">
                <span class="material-symbols-outlined text-primary-container text-4xl" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
              </div>
            </div>
            <div class="absolute bottom-6 left-6 text-on-surface font-headline font-bold tracking-tight">Trailer chính thức - Hoppers (2024)</div>
          </div>
        </div>
      </div>
      <div class="space-y-10">
        <div class="glass-card p-8 rounded-2xl glass-shine-edge">
          <h3 class="font-headline font-bold text-lg mb-6 border-b border-outline-variant/10 pb-4 text-primary-container tracking-tight">Chi tiết sản xuất</h3>
          <div class="space-y-6">
            <div>
              <span class="font-label text-[10px] text-on-surface-variant block mb-1 tracking-widest uppercase">Đạo diễn</span>
              <span class="text-on-surface font-semibold text-lg">Denis Villeneuve Jr.</span>
            </div>
            <div>
              <span class="font-label text-[10px] text-on-surface-variant block mb-1 tracking-widest uppercase">Diễn viên chính</span>
              <ul class="text-on-surface font-semibold space-y-1">
                <li>Florence Pugh</li>
                <li>Timothée Chalamet</li>
                <li>Oscar Isaac</li>
              </ul>
            </div>
            <div>
              <span class="font-label text-[10px] text-on-surface-variant block mb-1 tracking-widest uppercase">Ngày khởi chiếu</span>
              <span class="text-on-surface font-semibold">15 Tháng 11, 2024</span>
            </div>
            <div>
              <span class="font-label text-[10px] text-on-surface-variant block mb-1 tracking-widest uppercase">Ngôn ngữ</span>
              <span class="text-on-surface font-semibold">Tiếng Anh (Phụ đề Việt)</span>
            </div>
          </div>
        </div>
        <div class="glass-card p-8 rounded-2xl flex flex-col items-center text-center">
          <span class="material-symbols-outlined text-primary-container text-4xl mb-4">confirmation_number</span>
          <h4 class="font-headline font-bold mb-2">Trải nghiệm Lumière Noir</h4>
          <p class="text-sm text-on-surface-variant mb-6">Thưởng thức điện ảnh với tiêu chuẩn âm thanh Dolby Atmos và ghế ngồi hạng thương gia.</p>
          <button class="w-full py-3 bg-on-surface text-background font-headline font-bold text-sm tracking-widest hover:bg-primary-container hover:text-on-primary transition-colors">TÌM RẠP GẦN BẠN</button>
        </div>
      </div>
    </section>

    <!-- Recommended Grid -->
    <section class="max-w-[1440px] mx-auto px-10 pb-32">
      <div class="flex justify-between items-end mb-12">
        <div>
          <h2 class="text-3xl font-headline font-extrabold tracking-tight text-on-surface uppercase mb-2">Phim gợi ý</h2>
          <div class="h-1 w-20 bg-primary-container"></div>
        </div>
        <a class="text-primary-container font-label text-xs font-bold tracking-widest uppercase hover:underline" href="#">Xem tất cả</a>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
        <!-- Movie Card 1 -->
        <router-link to="/movie/1" class="group cursor-pointer">
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-xl glass-card glass-shine-edge">
            <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" src="/images/Hopper.webp"/>
            <div class="absolute top-0 left-0 bg-error-container text-on-error-container px-2 py-0.5 font-label text-[10px] font-bold">T18</div>
          </div>
          <h3 class="font-headline font-bold text-sm text-on-surface group-hover:text-primary-container transition-colors uppercase truncate">THE SILENT ECHO</h3>
          <p class="font-label text-[10px] text-on-surface-variant tracking-wider">Thriller • 112m</p>
        </router-link>
        <!-- Movie Card 2 -->
        <router-link to="/movie/1" class="group cursor-pointer">
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-xl glass-card glass-shine-edge">
            <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" src="/images/Hopper.webp"/>
            <div class="absolute top-0 left-0 bg-tertiary-container text-on-tertiary-container px-2 py-0.5 font-label text-[10px] font-bold">P</div>
          </div>
          <h3 class="font-headline font-bold text-sm text-on-surface group-hover:text-primary-container transition-colors uppercase truncate">NEON FOREST</h3>
          <p class="font-label text-[10px] text-on-surface-variant tracking-wider">Animation • 95m</p>
        </router-link>
        <!-- Movie Card 3 -->
        <router-link to="/movie/1" class="group cursor-pointer">
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-xl glass-card glass-shine-edge">
            <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" src="/images/Hopper.webp"/>
            <div class="absolute top-0 left-0 bg-error-container text-on-error-container px-2 py-0.5 font-label text-[10px] font-bold">T16</div>
          </div>
          <h3 class="font-headline font-bold text-sm text-on-surface group-hover:text-primary-container transition-colors uppercase truncate">VELOCITY ZERO</h3>
          <p class="font-label text-[10px] text-on-surface-variant tracking-wider">Action • 134m</p>
        </router-link>
        <!-- Movie Card 4 -->
        <router-link to="/movie/1" class="group cursor-pointer">
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-xl glass-card glass-shine-edge">
            <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" src="/images/Hopper.webp"/>
            <div class="absolute top-0 left-0 bg-error-container text-on-error-container px-2 py-0.5 font-label text-[10px] font-bold">T13</div>
          </div>
          <h3 class="font-headline font-bold text-sm text-on-surface group-hover:text-primary-container transition-colors uppercase truncate">MIDNIGHT RHYTHM</h3>
          <p class="font-label text-[10px] text-on-surface-variant tracking-wider">Drama • 142m</p>
        </router-link>
        <!-- Movie Card 5 -->
        <router-link to="/movie/1" class="hidden lg:block group cursor-pointer">
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-xl glass-card glass-shine-edge">
            <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" src="/images/Hopper.webp"/>
            <div class="absolute top-0 left-0 bg-error-container text-on-error-container px-2 py-0.5 font-label text-[10px] font-bold">T18</div>
          </div>
          <h3 class="font-headline font-bold text-sm text-on-surface group-hover:text-primary-container transition-colors uppercase truncate">GHOSTS OF ELM</h3>
          <p class="font-label text-[10px] text-on-surface-variant tracking-wider">Horror • 108m</p>
        </router-link>
      </div>
    </section>
  </main>
</template>

<style scoped>
.hero-gradient {
  background: linear-gradient(0deg, transparent 0%, rgba(10, 10, 15, 0.4) 50%, rgba(10, 10, 15, 0.1) 100%);
}
.editorial-shadow {
  text-shadow: 0 4px 12px rgba(0,0,0,0.5);
}
</style>
