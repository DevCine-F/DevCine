<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="pt-32 pb-20 max-w-[1440px] mx-auto px-6 md:px-10">
    <!-- Hero Section / Header -->
    <header class="mb-16">
      <div class="inline-block bg-primary-container/10 px-4 py-1 border-l-2 border-primary-container mb-4">
        <span class="text-primary-container text-xs font-bold tracking-widest uppercase font-label">Đặc quyền hội viên</span>
      </div>
      <h1 class="text-5xl md:text-7xl font-headline font-extrabold tracking-tighter mb-6 text-on-surface leading-none">
        KHUYẾN MÃI <br/><span class="text-primary-container">& ƯU ĐÃI.</span>
      </h1>
      <p class="max-w-2xl text-on-surface-variant text-lg leading-relaxed">
        Nâng tầm trải nghiệm điện ảnh của bạn với những gói ưu đãi độc quyền. Từ những combo bắp nước chủ đề đến các chương trình dành riêng cho thành viên DevCine.
      </p>
    </header>
    
    <!-- Promotions Grid -->
    <section class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
      <!-- Featured Card -->
      <div class="md:col-span-2 lg:col-span-2 group relative bg-surface-container-high rounded-xl overflow-hidden min-h-[400px] flex flex-col justify-end transition-all duration-500 hover:shadow-2xl hover:shadow-primary-container/5">
        <img class="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-700" src="/images/Hopper.webp"/>
        <div class="absolute inset-0 editorial-gradient"></div>
        <div class="relative p-10 z-10">
          <span class="bg-error-container text-on-error-container px-3 py-1 text-[10px] font-bold uppercase tracking-widest mb-4 inline-block">Hot Trend</span>
          <h3 class="text-4xl font-headline font-bold text-white mb-2">Combo Siêu Anh Hùng</h3>
          <p class="text-neutral-300 max-w-md mb-6 leading-relaxed">
            Sở hữu trọn bộ ly sưu tập giới hạn cùng bắp rang bơ vị Caramel đặc biệt khi đặt vé các phim bom tấn Marvel & DC.
          </p>
          <a class="inline-flex items-center gap-2 text-primary-container font-bold hover:gap-4 transition-all uppercase tracking-widest text-xs" href="#">
            Learn More <span class="material-symbols-outlined">arrow_forward</span>
          </a>
        </div>
      </div>
      
      <!-- Standard Grid Cards -->
      <div class="group bg-surface-container-low rounded-xl overflow-hidden flex flex-col border border-outline-variant/10 hover:border-primary-container/30 transition-all duration-300">
        <div class="h-64 overflow-hidden relative">
          <img class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" src="/images/Hopper.webp"/>
          <div class="absolute top-0 left-0 bg-primary-container text-on-primary px-3 py-1 font-bold text-[10px] uppercase">Thứ 3 Hàng Tuần</div>
        </div>
        <div class="p-8 flex flex-col flex-grow">
          <h3 class="text-xl font-headline font-bold mb-3 text-on-surface">Ngày Hội Gia Đình</h3>
          <p class="text-on-surface-variant text-sm leading-relaxed mb-6 flex-grow">
            Giảm ngay 30% giá vé cho nhóm gia đình từ 4 người và tặng kèm 01 phần quà bí mật cho các bé dưới 12 tuổi.
          </p>
          <a class="text-primary-container font-bold text-xs uppercase tracking-widest flex items-center gap-2 hover:opacity-80 transition-opacity" href="#">
            Learn More <span class="material-symbols-outlined text-sm">open_in_new</span>
          </a>
        </div>
      </div>
      <div class="group bg-surface-container-low rounded-xl overflow-hidden flex flex-col border border-outline-variant/10 hover:border-primary-container/30 transition-all duration-300">
        <div class="h-64 overflow-hidden relative">
          <img class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" src="/images/Hopper.webp"/>
          <div class="absolute top-0 left-0 bg-tertiary-container text-on-tertiary-container px-3 py-1 font-bold text-[10px] uppercase">Lumière Elite</div>
        </div>
        <div class="p-8 flex flex-col flex-grow">
          <h3 class="text-xl font-headline font-bold mb-3 text-on-surface">Đặc Quyền Thành Viên</h3>
          <p class="text-on-surface-variant text-sm leading-relaxed mb-6 flex-grow">
            Nhân đôi điểm thưởng cho mọi giao dịch đặt vé qua ứng dụng. Đổi điểm lấy vé xem phim miễn phí hoặc bỏng nước.
          </p>
          <a class="text-primary-container font-bold text-xs uppercase tracking-widest flex items-center gap-2 hover:opacity-80 transition-opacity" href="#">
            Learn More <span class="material-symbols-outlined text-sm">open_in_new</span>
          </a>
        </div>
      </div>
      <div class="group bg-surface-container-low rounded-xl overflow-hidden flex flex-col border border-outline-variant/10 hover:border-primary-container/30 transition-all duration-300">
        <div class="h-64 overflow-hidden relative">
          <img class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" src="/images/Hopper.webp"/>
          <div class="absolute top-0 left-0 bg-error-container text-on-error-container px-3 py-1 font-bold text-[10px] uppercase">Cặp Đôi</div>
        </div>
        <div class="p-8 flex flex-col flex-grow">
          <h3 class="text-xl font-headline font-bold mb-3 text-on-surface">Sweetbox Night</h3>
          <p class="text-on-surface-variant text-sm leading-relaxed mb-6 flex-grow">
            Tận hưởng không gian riêng tư tại hàng ghế Sweetbox với gói combo đặc biệt bao gồm 2 nước, 1 bắp lớn và 1 phần snack.
          </p>
          <a class="text-primary-container font-bold text-xs uppercase tracking-widest flex items-center gap-2 hover:opacity-80 transition-opacity" href="#">
            Learn More <span class="material-symbols-outlined text-sm">open_in_new</span>
          </a>
        </div>
      </div>
    </section>

    <!-- Newsletter / CTA -->
    <section class="mt-24 bg-surface-container rounded-sm p-12 border border-outline-variant/20 relative overflow-hidden">
      <div class="absolute top-0 right-0 w-64 h-64 bg-primary-container/5 rounded-full blur-3xl -mr-32 -mt-32"></div>
      <div class="relative z-10 flex flex-col md:flex-row items-center justify-between gap-8">
        <div class="max-w-xl">
          <h2 class="text-3xl font-headline font-bold text-on-surface mb-2">Đừng bỏ lỡ bất kỳ ưu đãi nào.</h2>
          <p class="text-on-surface-variant">Đăng ký nhận tin để được thông báo về các suất chiếu sớm và mã giảm giá độc quyền dành riêng cho bạn.</p>
        </div>
        <div class="flex w-full md:w-auto gap-2">
          <input class="bg-surface-container-lowest border-outline-variant/30 focus:border-primary-container focus:ring-0 text-on-surface px-6 py-3 rounded-sm w-full md:min-w-[300px]" placeholder="Email của bạn" type="email"/>
          <button class="bg-primary-container text-on-primary px-8 py-3 rounded-sm font-bold uppercase tracking-widest text-xs hover:opacity-90 transition-opacity shrink-0">Đăng ký</button>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.editorial-gradient {
  background: linear-gradient(to top, rgba(14, 14, 14, 1) 0%, rgba(14, 14, 14, 0.4) 50%, rgba(14, 14, 14, 0) 100%);
}
</style>
