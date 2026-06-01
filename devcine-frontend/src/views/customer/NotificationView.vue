<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="min-h-screen pt-32 pb-20 px-6 md:px-10 max-w-[1440px] mx-auto">
    <!-- Header Section -->
    <header class="mb-12 flex flex-col md:flex-row md:items-end justify-between gap-6">
      <div class="space-y-2">
        <span class="label-sm text-primary-container font-bold uppercase tracking-[0.2em]">Trung tâm thông báo</span>
        <h1 class="text-5xl md:text-6xl font-headline font-extrabold tracking-tighter text-on-surface">Thông báo</h1>
      </div>
      <div class="flex items-center gap-4">
        <button class="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-on-surface-variant hover:text-primary transition-colors group">
          <span class="material-symbols-outlined text-lg">done_all</span> Đánh dấu đã đọc
        </button>
        <button class="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-on-surface-variant hover:text-primary transition-colors group">
          <span class="material-symbols-outlined text-lg">settings</span> Cài đặt
        </button>
      </div>
    </header>
    
    <!-- Notification Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
      <!-- Left Column: Categories & Filter (Sticky) -->
      <aside class="lg:col-span-3 space-y-8">
        <div class="flex flex-col gap-2">
          <button class="flex items-center justify-between px-4 py-3 bg-primary-container text-on-primary rounded-sm font-bold text-sm uppercase tracking-wider">
            <span>Tất cả thông báo</span>
            <span class="bg-on-primary/10 px-2 py-0.5 text-[10px]">12</span>
          </button>
          <button class="flex items-center justify-between px-4 py-3 hover:bg-surface-container-high transition-colors rounded-sm text-on-surface-variant font-bold text-sm uppercase tracking-wider">
            <span>Đặt vé</span>
            <span class="bg-surface-container-highest px-2 py-0.5 text-[10px] text-on-surface">4</span>
          </button>
          <button class="flex items-center justify-between px-4 py-3 hover:bg-surface-container-high transition-colors rounded-sm text-on-surface-variant font-bold text-sm uppercase tracking-wider">
            <span>Nhắc nhở</span>
            <span class="bg-surface-container-highest px-2 py-0.5 text-[10px] text-on-surface">3</span>
          </button>
          <button class="flex items-center justify-between px-4 py-3 hover:bg-surface-container-high transition-colors rounded-sm text-on-surface-variant font-bold text-sm uppercase tracking-wider">
            <span>Khuyến mãi</span>
            <span class="bg-surface-container-highest px-2 py-0.5 text-[10px] text-on-surface">5</span>
          </button>
        </div>
        
        <div class="p-6 bg-surface-container-low rounded-sm space-y-4">
          <h3 class="label-sm text-on-surface-variant font-bold uppercase tracking-widest">Suất chiếu tiếp theo</h3>
          <div class="relative aspect-[3/4] w-full overflow-hidden group">
            <img class="w-full h-full object-cover transition-all duration-700" src="/images/Hopper.webp"/>
            <div class="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent"></div>
            <div class="absolute bottom-4 left-4 right-4">
              <p class="title-md font-headline font-bold leading-tight">Dune: Part Two</p>
              <p class="text-[10px] uppercase text-primary-container tracking-widest mt-1">Bắt đầu sau 45 phút</p>
            </div>
          </div>
        </div>
      </aside>

      <!-- Right Column: Notification Feed -->
      <section class="lg:col-span-9 space-y-4">
        <!-- Ticket Ready - Priority Glass Card -->
        <div class="glass-card p-6 flex gap-6 relative overflow-hidden group hover:bg-white/5 transition-colors">
          <div class="absolute left-0 top-0 bottom-0 w-1 bg-primary-container"></div>
          <div class="flex-shrink-0 w-12 h-12 bg-primary-container/10 flex items-center justify-center">
            <span class="material-symbols-outlined text-primary-container text-3xl">confirmation_number</span>
          </div>
          <div class="flex-grow space-y-1">
            <div class="flex justify-between items-start">
              <h4 class="title-md font-headline font-bold text-on-surface">Vé đã sẵn sàng: Dune Part Two</h4>
              <span class="text-[10px] text-on-surface-variant uppercase font-bold tracking-widest">Vừa xong</span>
            </div>
            <p class="body-lg text-on-surface-variant text-sm leading-relaxed max-w-2xl">Vé xem phim kỹ thuật số của bạn tại Phòng chiếu IMAX 4 đã có sẵn. Vui lòng xuất trình mã QR tại lối vào để vào rạp.</p>
            <div class="pt-4 flex gap-4">
              <router-link to="/profile" class="bg-primary-container text-on-primary px-5 py-2 text-[10px] font-bold uppercase tracking-widest hover:brightness-110 transition-all text-center inline-block">Xem vé</router-link>
              <button class="border border-outline-variant/30 text-on-surface px-5 py-2 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-all">Thêm vào ví</button>
            </div>
          </div>
        </div>
        
        <!-- Movie Reminder -->
        <div class="glass-card p-6 flex gap-6 group hover:bg-white/5 transition-colors">
          <div class="flex-shrink-0 w-12 h-12 bg-surface-container-high flex items-center justify-center">
            <span class="material-symbols-outlined text-primary-container text-3xl" style="font-variation-settings: 'FILL' 1;">schedule</span>
          </div>
          <div class="flex-grow space-y-1">
            <div class="flex justify-between items-start">
              <h4 class="title-md font-headline font-bold text-on-surface">Bắt đầu sau 30 phút</h4>
              <span class="text-[10px] text-on-surface-variant uppercase font-bold tracking-widest">12 phút trước</span>
            </div>
            <p class="body-lg text-on-surface-variant text-sm leading-relaxed">Nhắc nhở nhanh rằng phim <span class="text-on-surface font-semibold">The Batman</span> sẽ bắt đầu lúc 8:45 CH. Đừng bỏ lỡ các đoạn trailer trước phim nhé.</p>
          </div>
        </div>
        
        <!-- Promotion -->
        <div class="glass-card p-0 flex flex-col md:flex-row group hover:bg-white/5 transition-colors overflow-hidden">
          <div class="md:w-48 h-40 md:h-auto flex-shrink-0 relative">
            <img class="w-full h-full object-cover" src="/images/Hopper.webp"/>
            <div class="absolute inset-0 bg-primary-container/10 mix-blend-overlay"></div>
          </div>
          <div class="p-6 flex-grow space-y-2">
            <div class="flex justify-between items-start">
              <div class="flex items-center gap-2">
                <span class="bg-tertiary-container/20 text-tertiary-container text-[9px] font-black uppercase px-2 py-0.5 tracking-tighter">Ưu đãi có hạn</span>
                <h4 class="title-md font-headline font-bold text-on-surface">Combo Noir mới đã sẵn sàng</h4>
              </div>
              <span class="text-[10px] text-on-surface-variant uppercase font-bold tracking-widest">2 giờ trước</span>
            </div>
            <p class="body-lg text-on-surface-variant text-sm leading-relaxed">Nâng tầm trải nghiệm của bạn với Bắp Truffle thủ công và Soda Anh Đào Đậm. Ưu đãi giảm giá 20% dành riêng cho Thành viên Vàng.</p>
            <div class="pt-2">
              <button class="text-primary-container text-[10px] font-bold uppercase tracking-widest flex items-center gap-1 group">
                Nhận ưu đãi ngay <span class="material-symbols-outlined text-sm group-hover:translate-x-1 transition-transform">arrow_forward</span>
              </button>
            </div>
          </div>
        </div>
        
        <!-- Past History Separator -->
        <div class="py-8 flex items-center gap-4">
          <span class="text-[10px] font-bold uppercase tracking-[0.3em] text-neutral-600">Trước đó trong tuần</span>
          <div class="h-[1px] flex-grow bg-neutral-800/40"></div>
        </div>
        
        <!-- Secondary Booking -->
        <div class="glass-card p-6 flex gap-6 opacity-60 hover:opacity-100 transition-opacity group">
          <div class="flex-shrink-0 w-12 h-12 bg-surface-container-high flex items-center justify-center">
            <span class="material-symbols-outlined text-on-surface-variant text-3xl">check_circle</span>
          </div>
          <div class="flex-grow space-y-1">
            <div class="flex justify-between items-start">
              <h4 class="title-md font-headline font-bold text-on-surface">Đặt vé thành công</h4>
              <span class="text-[10px] text-on-surface-variant uppercase font-bold tracking-widest">2 ngày trước</span>
            </div>
            <p class="body-lg text-on-surface-variant text-sm leading-relaxed">Thanh toán cho phim 'Oppenheimer' (2 Ghế) đã thành công. Chúng tôi đã gửi hóa đơn vào email của bạn.</p>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.glass-card {
  background: rgba(42, 42, 42, 0.4);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(78, 70, 51, 0.1);
}
</style>
