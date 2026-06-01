<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="min-h-screen flex flex-col items-center justify-center relative overflow-hidden px-4 py-20">
    <!-- Background Atmospheric Element -->
    <div class="absolute inset-0 z-0">
      <img src="/images/Hopper.webp" class="w-full h-full object-cover opacity-20"/>
      <div class="absolute inset-0 noir-gradient"></div>
    </div>

    <!-- Success Header -->
    <div class="relative z-10 text-center mb-12">
      <div class="inline-flex items-center justify-center w-20 h-20 rounded-full bg-primary-container mb-6 shadow-[0_0_40px_rgba(245,197,24,0.3)]">
        <span class="material-symbols-outlined text-on-primary-container text-4xl" style="font-variation-settings: 'FILL' 1;">check_circle</span>
      </div>
      <h1 class="font-headline font-extrabold text-4xl md:text-5xl tracking-tighter text-on-surface uppercase mb-2">ĐẶT VÉ THÀNH CÔNG</h1>
      <p class="font-label text-sm uppercase tracking-[0.2em] text-on-surface-variant">HÀNH TRÌNH ĐIỆN ẢNH CỦA BẠN BẮT ĐẦU NGAY BÂY GIỜ</p>
    </div>

    <!-- Digital Ticket -->
    <div class="relative z-10 w-full max-w-md bg-surface-container-high rounded-sm shadow-2xl overflow-hidden border border-outline-variant/20">
      <!-- Movie Banner -->
      <div class="relative h-48 w-full overflow-hidden">
        <img src="/images/Hopper.webp" class="w-full h-full object-cover"/>
        <div class="absolute inset-0 bg-gradient-to-t from-surface-container-high via-transparent to-transparent"></div>
        <div class="absolute top-0 left-0 p-4">
          <span class="bg-error-container text-on-error-container font-label text-[10px] px-3 py-1 tracking-widest">T16</span>
        </div>
      </div>
      
      <!-- Ticket Body -->
      <div class="p-8">
        <div class="flex justify-between items-start mb-8">
          <div>
            <h2 class="font-headline font-bold text-2xl text-primary-container leading-tight mb-1">THE SILENT WITNESS</h2>
            <p class="text-on-surface-variant font-label text-xs uppercase tracking-widest">DevCine Grand Hall | Screen 04</p>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-y-6 gap-x-8 mb-10">
          <div>
            <p class="font-label text-[10px] text-on-surface-variant uppercase tracking-[0.15em] mb-1">NGÀY</p>
            <p class="font-headline font-bold text-lg">DEC 14, 2024</p>
          </div>
          <div>
            <p class="font-label text-[10px] text-on-surface-variant uppercase tracking-[0.15em] mb-1">GIỜ CHIẾU</p>
            <p class="font-headline font-bold text-lg">20:45 PM</p>
          </div>
          <div>
            <p class="font-label text-[10px] text-on-surface-variant uppercase tracking-[0.15em] mb-1">GHẾ</p>
            <p class="font-headline font-bold text-lg">H12, H13</p>
          </div>
          <div>
            <p class="font-label text-[10px] text-on-surface-variant uppercase tracking-[0.15em] mb-1">MÃ ĐẶT VÉ</p>
            <p class="font-headline font-bold text-lg">#DC-99210</p>
          </div>
        </div>
        
        <!-- Perforation Divider -->
        <div class="relative flex items-center justify-center my-8">
          <div class="absolute left-0 -translate-x-1/2 w-6 h-6 bg-surface rounded-full border border-outline-variant/20"></div>
          <div class="absolute right-0 translate-x-1/2 w-6 h-6 bg-surface rounded-full border border-outline-variant/20"></div>
          <div class="w-full border-t border-dashed border-outline-variant/40"></div>
        </div>
        
        <!-- QR Section -->
        <div class="flex flex-col items-center">
          <div class="p-4 bg-white rounded-sm mb-4 shadow-xl">
            <img src="/images/Hopper.webp" class="w-40 h-40"/>
          </div>
          <p class="font-label text-[10px] text-on-surface-variant uppercase tracking-widest text-center">QUÉT MÃ QR TẠI CỬA RẠP ĐỂ VÀO CỬA NHANH CHÓNG</p>
        </div>
      </div>
    </div>

    <!-- Action Buttons -->
    <div class="relative z-10 mt-12 flex flex-col md:flex-row gap-4 w-full max-w-md">
      <router-link to="/profile" class="flex-1 bg-primary-container text-on-primary font-headline font-bold text-sm uppercase tracking-widest py-5 rounded-sm hover:bg-primary-fixed-dim transition-all active:scale-[0.98] text-center inline-block">XEM VÉ CỦA TÔI</router-link>
      <router-link to="/" class="flex-1 bg-transparent border border-outline-variant text-on-surface font-headline font-bold text-sm uppercase tracking-widest py-5 rounded-sm hover:bg-primary-container/10 transition-all active:scale-[0.98] text-center inline-block">QUAY LẠI TRANG CHỦ</router-link>
    </div>
  </main>
</template>

<style scoped>
.noir-gradient { background: linear-gradient(180deg, rgba(19,19,19,0) 0%, rgba(19,19,19,1) 100%); }
</style>
