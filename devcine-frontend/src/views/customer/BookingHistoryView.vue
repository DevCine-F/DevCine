<template>
  <section>
    <div class="flex flex-col md:flex-row justify-between items-baseline mb-8 gap-4">
      <h2 class="text-2xl font-bold tracking-tight font-headline">Lịch sử đặt vé</h2>
      <div class="flex gap-4">
        <button class="text-xs font-bold uppercase tracking-widest border-b-2 border-primary-container pb-1 text-primary-container">Tất cả</button>
        <button class="text-xs font-bold uppercase tracking-widest text-neutral-500 hover:text-on-surface transition-colors pb-1 border-b-2 border-transparent">Sắp diễn ra</button>
        <button class="text-xs font-bold uppercase tracking-widest text-neutral-500 hover:text-on-surface transition-colors pb-1 border-b-2 border-transparent">Đã xem</button>
      </div>
    </div>
    
    <div class="grid grid-cols-1 gap-4">
      <!-- Record 1 (Upcoming) -->
      <div class="group relative bg-surface-container-high hover:bg-surface-bright transition-all duration-300 p-1 flex flex-col md:flex-row gap-6 items-stretch">
        <div class="w-full md:w-24 h-36 md:h-auto overflow-hidden shrink-0">
          <img class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" src="/images/Hopper.webp"/>
        </div>
        <div class="flex-grow flex flex-col justify-center py-4">
          <div class="flex items-center gap-2 mb-1">
            <span class="bg-error-container text-on-error-container text-[9px] font-bold px-1.5 py-0.5 rounded-sm">T16</span>
            <span class="text-primary-container text-[9px] font-bold uppercase tracking-widest">Sắp diễn ra</span>
          </div>
          <h3 class="text-xl font-bold mb-3 group-hover:text-primary-container transition-colors uppercase font-headline">Dune: Hành tinh cát - Phần 2</h3>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <p class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant mb-0.5">Phim / Rạp</p>
              <p class="text-xs font-semibold truncate">DevCine Vincom Center</p>
            </div>
            <div>
              <p class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant mb-0.5">Suất chiếu</p>
              <p class="text-xs font-semibold">19:30 • 25/10/2024</p>
            </div>
            <div>
              <p class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant mb-0.5">Số ghế</p>
              <p class="text-xs font-semibold text-primary-container">V.I.P J-12, J-13</p>
            </div>
            <div>
              <p class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant mb-0.5">Tổng tiền</p>
              <p class="text-xs font-semibold">240.000đ</p>
            </div>
          </div>
        </div>
        <div class="flex flex-col justify-center gap-2 py-4 px-4 md:pr-6">
          <button class="bg-white/5 hover:bg-white/10 text-[10px] font-bold uppercase tracking-widest px-4 py-2 transition-colors border border-white/5">
            Chi tiết vé
          </button>
          <button class="text-[9px] font-bold uppercase tracking-widest text-primary-container/80 hover:text-primary-container underline underline-offset-4 decoration-1">
            Hủy vé
          </button>
        </div>
      </div>
    </div>
    
    <div class="mt-8 flex justify-center">
      <router-link to="/profile/history" class="group flex items-center gap-2 text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant hover:text-primary-container transition-colors cursor-pointer">
        Xem thêm lịch sử
        <span class="material-symbols-outlined text-sm transition-transform group-hover:translate-y-1">expand_more</span>
      </router-link>
    </div>
  </section>
</template>
