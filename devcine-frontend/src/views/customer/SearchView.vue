<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="min-h-screen pt-32 pb-20 px-4 md:px-10 max-w-[1440px] mx-auto">
    <!-- Search Interface Section -->
    <section class="mb-20">
      <div class="max-w-3xl mx-auto text-center space-y-8">
        <h1 class="text-display-lg font-headline font-extrabold tracking-tighter text-5xl md:text-6xl text-on-surface">Khám phá điện ảnh</h1>
        <div class="relative group">
          <div class="absolute inset-y-0 left-5 flex items-center pointer-events-none text-outline">
            <span class="material-symbols-outlined">search</span>
          </div>
          <input class="w-full bg-surface-container-lowest border-0 focus:ring-1 focus:ring-primary-container py-6 pl-14 pr-6 text-xl font-body rounded-lg shadow-inner outline-none transition-all placeholder:text-neutral-600" placeholder="Tìm kiếm tiêu đề, thể loại hoặc đạo diễn..." type="text" value="The Midnight Echo"/>
        </div>
      </div>
    </section>
    
    <!-- Empty State / 404 Section -->
    <section class="flex flex-col items-center justify-center py-20 bg-surface-container-low rounded-xl border-outline-variant/5 border relative overflow-hidden">
      <!-- Cinematic Background Decor -->
      <div class="absolute inset-0 opacity-10 pointer-events-none">
        <div class="absolute -top-24 -left-24 w-96 h-96 bg-primary-container rounded-full blur-[120px]"></div>
        <div class="absolute -bottom-24 -right-24 w-96 h-96 bg-primary-container rounded-full blur-[120px]"></div>
      </div>
      <div class="relative z-10 text-center max-w-lg px-6">
        <!-- Fallback Illustration -->
        <div class="mb-8 inline-flex items-center justify-center w-32 h-32 rounded-full bg-surface-container-high border border-outline-variant/10 shadow-2xl">
          <span class="material-symbols-outlined text-6xl text-yellow-500/50" style="font-variation-settings: 'FILL' 1;">movie_filter</span>
        </div>
        <h2 class="font-headline text-3xl font-bold mb-4">Không tìm thấy phim</h2>
        <p class="text-neutral-400 body-lg mb-10 leading-relaxed">Chúng tôi không tìm thấy kết quả nào cho "The Midnight Echo". Nó có thể đã bị mất trong phòng biên tập hoặc chưa được phát hành. Thử một tiêu đề khác xem sao?</p>
        <button class="bg-surface-container-highest hover:bg-surface-bright text-primary border border-outline-variant/20 px-8 py-3 font-bold tracking-widest uppercase transition-all">Đặt lại tìm kiếm</button>
      </div>
    </section>

    <!-- Recommendations Grid (Fallback) -->
    <section class="max-w-[1440px] mx-auto px-10 pb-32 mt-32">
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
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-sm bg-surface-container-high">
            <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" src="/images/Hopper.webp"/>
            <div class="absolute top-0 left-0 bg-error-container text-on-error-container px-2 py-0.5 font-label text-[10px] font-bold">T18</div>
          </div>
          <h3 class="font-headline font-bold text-sm text-on-surface group-hover:text-primary-container transition-colors uppercase truncate">THE SILENT ECHO</h3>
          <p class="font-label text-[10px] text-on-surface-variant tracking-wider">Thriller • 112m</p>
        </router-link>
        <!-- Movie Card 2 -->
        <router-link to="/movie/1" class="group cursor-pointer">
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-sm bg-surface-container-high">
            <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" src="/images/Hopper.webp"/>
            <div class="absolute top-0 left-0 bg-tertiary-container text-on-tertiary-container px-2 py-0.5 font-label text-[10px] font-bold">P</div>
          </div>
          <h3 class="font-headline font-bold text-sm text-on-surface group-hover:text-primary-container transition-colors uppercase truncate">NEON FOREST</h3>
          <p class="font-label text-[10px] text-on-surface-variant tracking-wider">Animation • 95m</p>
        </router-link>
        <!-- Movie Card 3 -->
        <router-link to="/movie/1" class="group cursor-pointer">
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-sm bg-surface-container-high">
            <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" src="/images/Hopper.webp"/>
            <div class="absolute top-0 left-0 bg-error-container text-on-error-container px-2 py-0.5 font-label text-[10px] font-bold">T16</div>
          </div>
          <h3 class="font-headline font-bold text-sm text-on-surface group-hover:text-primary-container transition-colors uppercase truncate">VELOCITY ZERO</h3>
          <p class="font-label text-[10px] text-on-surface-variant tracking-wider">Action • 134m</p>
        </router-link>
        <!-- Movie Card 4 -->
        <router-link to="/movie/1" class="group cursor-pointer">
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-sm bg-surface-container-high">
            <img class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" src="/images/Hopper.webp"/>
            <div class="absolute top-0 left-0 bg-error-container text-on-error-container px-2 py-0.5 font-label text-[10px] font-bold">T13</div>
          </div>
          <h3 class="font-headline font-bold text-sm text-on-surface group-hover:text-primary-container transition-colors uppercase truncate">MIDNIGHT RHYTHM</h3>
          <p class="font-label text-[10px] text-on-surface-variant tracking-wider">Drama • 142m</p>
        </router-link>
        <!-- Movie Card 5 -->
        <router-link to="/movie/1" class="hidden lg:block group cursor-pointer">
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-sm bg-surface-container-high">
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
