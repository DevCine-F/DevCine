<script setup>
import BookingHistoryView from './BookingHistoryView.vue'
</script>

<template>
  <div>
    <header class="relative mb-16 bg-surface-container-low p-10 rounded-lg overflow-hidden flex flex-col md:flex-row items-end gap-8">
    <div class="absolute top-0 right-0 p-12 opacity-10 select-none pointer-events-none">
      <span class="material-symbols-outlined text-[12rem] text-primary-container" style="font-variation-settings: 'FILL' 1;">stars</span>
    </div>
    <div class="relative z-10 w-40 h-40 md:w-48 md:h-48 shrink-0 shadow-2xl">
      <img class="w-full h-full object-cover rounded-sm" src="/images/Hopper.webp"/>
      <div class="absolute inset-0 border border-white/10"></div>
    </div>
    <div class="relative z-10 flex-grow pb-2">
      <div class="flex items-center gap-3 mb-2">
        <span class="bg-primary-container text-on-primary text-[10px] font-bold tracking-[0.1em] uppercase px-2 py-0.5 rounded-sm">Thành viên cao cấp</span>
        <span class="text-primary-container/60 text-xs font-label uppercase tracking-widest">ID: #DC-88421</span>
      </div>
      <h2 class="text-4xl md:text-5xl font-extrabold tracking-tighter mb-4 font-headline">Trần Hoàng Nam</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-y-4 gap-x-12 mb-6">
        <div>
          <p class="text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-1">Email</p>
          <p class="text-sm font-semibold">hoangnam.tran@email.com</p>
        </div>
        <div>
          <p class="text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-1">Số điện thoại</p>
          <p class="text-sm font-semibold">090 ••• ••89</p>
        </div>
      </div>
      <div class="flex flex-wrap items-center gap-6 pt-4 border-t border-white/5">
        <div class="flex items-center gap-2">
          <span class="material-symbols-outlined text-primary-container" style="font-variation-settings: 'FILL' 1;">workspace_premium</span>
          <div>
            <p class="text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-0.5">Hạng thành viên</p>
            <span class="text-lg font-bold italic tracking-tight text-primary-container uppercase font-headline">Lumière Elite</span>
          </div>
        </div>
        <div class="h-10 w-[1px] bg-white/10 hidden md:block"></div>
        <div>
          <p class="text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-1">Điểm tích lũy</p>
          <p class="text-xl font-bold">14,250 <span class="text-xs text-primary-container">Điểm</span></p>
        </div>
        <div class="h-10 w-[1px] bg-white/10 hidden md:block"></div>
        <div>
          <p class="text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-1">Thành viên từ</p>
          <p class="text-xl font-bold">12/2022</p>
        </div>
      </div>
    </div>
    <div class="relative z-10 flex gap-4 md:ml-auto">
      <button class="bg-primary-container text-on-primary font-bold text-xs uppercase tracking-widest px-6 py-3 transition-transform active:scale-95 hover:bg-primary-fixed-dim">
        Nâng cấp hạng
      </button>
    </div>
  </header>
    <BookingHistoryView />
  </div>
</template>
