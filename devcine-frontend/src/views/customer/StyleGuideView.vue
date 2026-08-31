<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="pt-28 sm:pt-32 pb-16 sm:pb-20 max-w-[1440px] mx-auto px-4 sm:px-6 md:px-10">
    <!-- Hero Title -->
    <header class="mb-12 sm:mb-24">
      <p class="text-primary-container font-label text-xs sm:text-sm uppercase tracking-widest mb-3 sm:mb-4">Hệ thống Thiết kế</p>
      <h1 class="font-headline font-extrabold text-3xl sm:text-5xl md:text-[3.5rem] leading-[1.1] tracking-tighter max-w-4xl">DevCine<br>
        <span class="text-neutral-600">EDITORIAL UI KIT</span>
      </h1>
    </header>

    <!-- 1. Brand Identity -->
    <section class="mb-32">
      <div class="flex items-center gap-4 mb-12">
        <div class="h-px bg-outline-variant/30 flex-grow"></div>
        <h2 class="font-headline text-2xl font-bold uppercase tracking-tight">01. Nhận diện Thương hiệu</h2>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-10">
        <div class="bg-surface-container-lowest p-16 flex flex-col items-center justify-center border border-outline-variant/10">
          <span class="text-[#f5c518] font-headline text-6xl font-extrabold tracking-tighter mb-4">DevCine</span>
          <p class="text-on-surface-variant font-label text-xs uppercase tracking-widest">Logo Chính (Gold on Dark)</p>
        </div>
        <div class="bg-primary-container p-16 flex flex-col items-center justify-center">
          <span class="text-on-primary font-headline text-6xl font-extrabold tracking-tighter mb-4">DevCine</span>
          <p class="text-on-primary/60 font-label text-xs uppercase tracking-widest">Logo Đảo (Dark on Gold)</p>
        </div>
      </div>
    </section>

    <!-- 2. Color Palette -->
    <section class="mb-32">
      <div class="flex items-center gap-4 mb-12">
        <div class="h-px bg-outline-variant/30 flex-grow"></div>
        <h2 class="font-headline text-2xl font-bold uppercase tracking-tight">02. Bảng Màu Cinematic</h2>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
        <div class="space-y-4">
          <div class="aspect-square bg-surface border border-outline-variant/20 rounded-sm"></div>
          <div>
            <p class="font-headline font-bold">Surface</p>
            <p class="text-xs text-on-surface-variant">#131313</p>
          </div>
        </div>
        <div class="space-y-4">
          <div class="aspect-square bg-surface-container-lowest rounded-sm"></div>
          <div>
            <p class="font-headline font-bold">Deep Black</p>
            <p class="text-xs text-on-surface-variant">#0E0E0E</p>
          </div>
        </div>
        <div class="space-y-4">
          <div class="aspect-square bg-primary-container rounded-sm"></div>
          <div>
            <p class="font-headline font-bold">Golden Yellow</p>
            <p class="text-xs text-on-surface-variant">#F5C518</p>
          </div>
        </div>
        <div class="space-y-4">
          <div class="aspect-square bg-surface-container-high rounded-sm"></div>
          <div>
            <p class="font-headline font-bold">Surface High</p>
            <p class="text-xs text-on-surface-variant">#2A2A2A</p>
          </div>
        </div>
        <div class="space-y-4">
          <div class="aspect-square bg-error-container rounded-sm"></div>
          <div>
            <p class="font-headline font-bold">Error (T16/T13)</p>
            <p class="text-xs text-on-surface-variant">#93000A</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 3. Typography -->
    <section class="mb-32">
      <div class="flex items-center gap-4 mb-12">
        <div class="h-px bg-outline-variant/30 flex-grow"></div>
        <h2 class="font-headline text-2xl font-bold uppercase tracking-tight">03. Hệ thống Chữ</h2>
      </div>
      <div class="space-y-16">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8 border-b border-outline-variant/10 pb-8">
          <p class="text-on-surface-variant font-label uppercase text-xs">Display Large</p>
          <div class="md:col-span-2">
            <h3 class="font-headline font-extrabold text-[3.5rem] leading-none tracking-tighter">Oppenheimer</h3>
            <p class="mt-4 text-on-surface-variant font-body">Plus Jakarta Sans Bold - Used for hero movie titles.</p>
          </div>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8 border-b border-outline-variant/10 pb-8">
          <p class="text-on-surface-variant font-label uppercase text-xs">Headline Large</p>
          <div class="md:col-span-2">
            <h3 class="font-headline font-bold text-3xl tracking-tight">Phim Đang Chiếu</h3>
            <p class="mt-4 text-on-surface-variant font-body">Plus Jakarta Sans Bold - Used for section headers.</p>
          </div>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8 border-b border-outline-variant/10 pb-8">
          <p class="text-on-surface-variant font-label uppercase text-xs">Body Text</p>
          <div class="md:col-span-2">
            <p class="font-body text-lg leading-relaxed text-on-surface max-w-2xl">Câu chuyện về J. Robert Oppenheimer, nhà vật lý lý thuyết người Mỹ được mệnh danh là "cha đẻ của bom nguyên tử".</p>
            <p class="mt-4 text-on-surface-variant font-body">Manrope Regular - 1.6 line height for dark mode legibility.</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 4. Button Library -->
    <section class="mb-32">
      <div class="flex items-center gap-4 mb-12">
        <div class="h-px bg-outline-variant/30 flex-grow"></div>
        <h2 class="font-headline text-2xl font-bold uppercase tracking-tight">04. Thư viện Nút</h2>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12 items-end">
        <div class="space-y-4">
          <p class="text-xs font-label uppercase text-neutral-500">Primary</p>
          <button class="w-full bg-primary-container text-on-primary py-4 px-8 font-headline font-bold uppercase text-xs tracking-widest hover:opacity-90 active:scale-95 transition-all">Đặt Vé Ngay</button>
        </div>
        <div class="space-y-4">
          <p class="text-xs font-label uppercase text-neutral-500">Secondary</p>
          <button class="w-full bg-transparent border border-outline-variant text-on-surface py-4 px-8 font-headline font-bold uppercase text-xs tracking-widest hover:bg-primary-container/10 active:scale-95 transition-all">Xem Trailer</button>
        </div>
        <div class="space-y-4">
          <p class="text-xs font-label uppercase text-neutral-500">Ghost</p>
          <button class="w-full text-primary-container py-4 px-8 font-headline font-bold uppercase text-xs tracking-widest hover:bg-surface-container-high transition-all">Tất cả phim</button>
        </div>
        <div class="space-y-4">
          <p class="text-xs font-label uppercase text-neutral-500">Icon Button</p>
          <div class="flex gap-4">
            <button class="w-12 h-12 flex items-center justify-center bg-surface-container-high border border-outline-variant/20 hover:border-primary-container transition-all">
              <span class="material-symbols-outlined text-[#f5c518]">favorite</span>
            </button>
            <button class="w-12 h-12 flex items-center justify-center bg-surface-container-high border border-outline-variant/20 hover:border-primary-container transition-all">
              <span class="material-symbols-outlined text-[#f5c518]">share</span>
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- 5. Movie Cards & UI Elements -->
    <section class="mb-32">
      <div class="flex items-center gap-4 mb-12">
        <div class="h-px bg-outline-variant/30 flex-grow"></div>
        <h2 class="font-headline text-2xl font-bold uppercase tracking-tight">05. Linh kiện Movie Card</h2>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-10">
        <!-- Movie Card 1 -->
        <div class="group relative bg-surface-container-low transition-all duration-500 hover:-translate-y-2">
          <div class="absolute top-0 left-0 bg-error-container px-3 py-1 text-[10px] font-bold z-10 font-label">T18</div>
          <div class="aspect-[2/3] overflow-hidden relative">
            <img class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" src="/images/Hopper.webp">
            <div class="absolute inset-0 bg-gradient-to-t from-surface to-transparent opacity-60"></div>
          </div>
          <div class="p-6">
            <h4 class="font-headline font-bold text-xl mb-1 group-hover:text-primary-container transition-colors">Bóng Đè</h4>
            <p class="text-neutral-500 font-label text-[10px] uppercase tracking-widest">Kinh Dị • 105 Phút</p>
          </div>
        </div>
        <!-- Mini UI snippet -->
        <div class="flex flex-col gap-6">
          <div class="bg-surface-container-high p-6 border border-outline-variant/10">
            <p class="font-label text-[10px] text-neutral-500 uppercase tracking-widest mb-4">Lọc Phân Loại</p>
            <div class="flex flex-wrap gap-2">
              <span class="px-3 py-1 bg-primary-container text-on-primary text-[10px] font-bold">Hành Động</span>
              <span class="px-3 py-1 bg-surface-container text-neutral-400 text-[10px] font-bold hover:text-on-surface transition-colors">Tâm Lý</span>
              <span class="px-3 py-1 bg-surface-container text-neutral-400 text-[10px] font-bold hover:text-on-surface transition-colors">Kịch Tính</span>
            </div>
          </div>
          <div class="bg-surface-container-high p-6 border border-outline-variant/10">
            <p class="font-label text-[10px] text-neutral-500 uppercase tracking-widest mb-4">Chọn Suất Chiếu</p>
            <div class="grid grid-cols-3 gap-2">
              <button class="bg-surface-container border border-outline-variant/20 py-2 text-xs font-bold hover:border-primary-container transition-all">19:30</button>
              <button class="bg-primary-container text-on-primary py-2 text-xs font-bold">20:45</button>
              <button class="bg-surface-container border border-outline-variant/20 py-2 text-xs font-bold hover:border-primary-container transition-all">21:15</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 6. Effects & Glassmorphism -->
    <section class="mb-32">
      <div class="flex items-center gap-4 mb-12">
        <div class="h-px bg-outline-variant/30 flex-grow"></div>
        <h2 class="font-headline text-2xl font-bold uppercase tracking-tight">06. Hiệu ứng & Glassmorphism</h2>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-10">
        <div class="relative h-64 overflow-hidden group">
          <img class="w-full h-full object-cover" src="/images/Hopper.webp">
          <div class="absolute inset-0 flex items-center justify-center">
            <div class="bg-surface/60 backdrop-blur-xl p-8 max-w-xs text-center border border-on-surface/5">
              <p class="font-headline font-bold text-lg mb-2">Backdrop Blur</p>
              <p class="text-xs text-on-surface-variant font-body">Sử dụng blur 20px kết hợp với 60% opacity cho thanh điều hướng và overlay.</p>
            </div>
          </div>
        </div>
        <div class="bg-surface-container-lowest p-12 flex flex-col items-center justify-center border border-outline-variant/10">
          <div class="w-48 h-48 bg-surface-container-high shadow-2xl shadow-black/80 flex items-center justify-center">
            <span class="font-label text-xs uppercase tracking-widest text-neutral-500">Elevated Shadow</span>
          </div>
          <p class="mt-8 text-xs text-neutral-400 max-w-xs text-center font-body">Bóng đổ chỉ được sử dụng cho các thành phần nổi (Modals, Popovers) với màu Shadow được lấy từ đen tuyệt đối để tạo độ sâu cinematic.</p>
        </div>
      </div>
    </section>
  </main>
</template>
