<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="pt-32 pb-20 max-w-[1440px] mx-auto px-10 flex flex-col lg:flex-row gap-12">
    <!-- Main Content Area -->
    <div class="flex-grow space-y-16">
      <!-- Section 1: Seat Selection -->
      <section>
        <div class="mb-12">
          <h1 class="font-headline text-3xl font-bold tracking-tight mb-2 uppercase italic text-primary-container">01. Chọn Chỗ Ngồi</h1>
          <div class="flex items-center gap-4 text-on-surface-variant">
            <span class="flex items-center gap-1"><span class="material-symbols-outlined text-sm">location_on</span> DevCine Landmark 81</span>
            <span class="w-1 h-1 rounded-full bg-outline-variant"></span>
            <span class="flex items-center gap-1"><span class="material-symbols-outlined text-sm">calendar_today</span> 24/05/2024</span>
            <span class="w-1 h-1 rounded-full bg-outline-variant"></span>
            <span class="flex items-center gap-1"><span class="material-symbols-outlined text-sm">schedule</span> 19:30</span>
          </div>
        </div>
        <div class="relative glass-card glass-shine-edge p-12 overflow-hidden rounded-3xl">
          <!-- Screen -->
          <div class="w-full h-2 bg-gradient-to-r from-transparent via-primary-container to-transparent opacity-50 mb-4 blur-sm"></div>
          <div class="w-full h-1 bg-primary-container mb-20 screen-curve rounded-[50%_/_10%]"></div>
          <p class="text-center text-label-sm font-bold uppercase tracking-[0.3em] text-outline-variant mb-16">Màn Hình / Screen</p>
          
          <!-- Seats Grid -->
          <div class="seat-grid max-w-2xl mx-auto grid grid-cols-12 gap-3 mb-16">
            <!-- Row A (Booked) -->
            <div class="col-span-1 text-label-sm font-bold text-outline-variant flex items-center justify-center">A</div>
            <div class="col-span-10 grid grid-cols-10 gap-2">
              <div v-for="n in 10" :key="n" class="aspect-square bg-surface-variant/30 cursor-not-allowed"></div>
            </div>
            <div class="col-span-1 text-label-sm font-bold text-outline-variant flex items-center justify-center">A</div>
            
            <!-- Row B (Selected) -->
            <div class="col-span-1 text-label-sm font-bold text-outline-variant flex items-center justify-center">B</div>
            <div class="col-span-10 grid grid-cols-10 gap-2">
              <div class="aspect-square border border-outline-variant/30 hover:border-primary-container transition-colors cursor-pointer"></div>
              <div class="aspect-square border border-outline-variant/30 hover:border-primary-container transition-colors cursor-pointer"></div>
              <div class="aspect-square bg-primary-container text-on-primary flex items-center justify-center text-[10px] font-bold">B3</div>
              <div class="aspect-square bg-primary-container text-on-primary flex items-center justify-center text-[10px] font-bold">B4</div>
              <div v-for="n in 6" :key="n" class="aspect-square border border-outline-variant/30 hover:border-primary-container transition-colors cursor-pointer"></div>
            </div>
            <div class="col-span-1 text-label-sm font-bold text-outline-variant flex items-center justify-center">B</div>
            
            <!-- Row D (VIP) -->
            <div class="col-span-1 text-label-sm font-bold text-outline-variant flex items-center justify-center">D</div>
            <div class="col-span-10 grid grid-cols-10 gap-2">
              <div class="aspect-square border border-outline-variant/30"></div>
              <div class="aspect-square border border-outline-variant/30"></div>
              <div v-for="n in 6" :key="n" class="aspect-square border-2 border-primary-container/60 bg-primary-container/10 hover:bg-primary-container/30 transition-colors cursor-pointer"></div>
              <div class="aspect-square border border-outline-variant/30"></div>
              <div class="aspect-square border border-outline-variant/30"></div>
            </div>
            <div class="col-span-1 text-label-sm font-bold text-outline-variant flex items-center justify-center">D</div>
            
            <!-- Row J (Sweetbox) -->
            <div class="col-span-1 text-label-sm font-bold text-outline-variant flex items-center justify-center">J</div>
            <div class="col-span-10 grid grid-cols-5 gap-2">
              <div v-for="n in 5" :key="n" class="h-8 border-2 border-pink-500/50 bg-pink-500/10 rounded-t-xl hover:bg-pink-500/20 transition-colors cursor-pointer relative group">
                <span class="absolute inset-0 flex items-center justify-center text-[10px] opacity-0 group-hover:opacity-100 transition-opacity">J{{n*2-1}}-{{n*2}}</span>
              </div>
            </div>
            <div class="col-span-1 text-label-sm font-bold text-outline-variant flex items-center justify-center">J</div>
          </div>
          
          <!-- Legend -->
          <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-6 border-t border-outline-variant/10 pt-10">
            <div class="flex items-center gap-3">
              <div class="w-5 h-5 border border-outline-variant/40"></div>
              <div class="flex flex-col">
                <span class="text-[10px] font-bold uppercase tracking-wider text-on-surface">Standard</span>
                <span class="text-[10px] text-on-surface-variant">110.000 VNĐ</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-5 h-5 border-2 border-primary-container/60 bg-primary-container/10"></div>
              <div class="flex flex-col">
                <span class="text-[10px] font-bold uppercase tracking-wider text-primary-container">VIP</span>
                <span class="text-[10px] text-on-surface-variant">150.000 VNĐ</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-10 h-5 border-2 border-pink-500/50 bg-pink-500/10 rounded-t-lg"></div>
              <div class="flex flex-col">
                <span class="text-[10px] font-bold uppercase tracking-wider text-pink-400">Sweetbox</span>
                <span class="text-[10px] text-on-surface-variant">300.000 VNĐ</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-5 h-5 bg-primary-container"></div>
              <span class="text-[10px] font-bold uppercase tracking-wider text-on-surface">Đang chọn</span>
            </div>
            <div class="flex items-center gap-3">
              <div class="w-5 h-5 bg-surface-variant/30"></div>
              <span class="text-[10px] font-bold uppercase tracking-wider text-on-surface">Đã đặt</span>
            </div>
          </div>
        </div>
      </section>
      
      <!-- Section 2: Concessions Selection -->
      <section>
        <div class="mb-8">
          <h1 class="font-headline text-3xl font-bold tracking-tight mb-2 uppercase italic text-primary-container">02. Bắp Nước & Ưu Đãi</h1>
          <p class="text-sm text-on-surface-variant">Thêm hương vị cho trải nghiệm điện ảnh của bạn</p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="glass-card p-6 flex gap-6 hover:border-primary-container/30 transition-all group rounded-2xl" v-for="n in 4" :key="n">
            <div class="w-28 h-28 flex-shrink-0 bg-black overflow-hidden relative rounded-xl">
              <img src="/images/Hopper.webp" class="w-full h-full object-cover opacity-80 group-hover:scale-110 transition-transform duration-500"/>
              <div class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
            </div>
            <div class="flex flex-col justify-between flex-grow">
              <div>
                <h3 class="font-headline font-bold text-lg mb-1">{{ ['COMBO SOLO', 'COMBO COUPLE', 'LUXURY COFFEE SET', 'KIDS PARTY'][n-1] }}</h3>
                <p class="text-xs text-on-surface-variant line-clamp-2">Mô tả sản phẩm và ưu đãi đi kèm. Tiết kiệm tối đa cho thành viên.</p>
              </div>
              <div class="flex items-center justify-between">
                <span class="font-headline font-bold text-primary-container">{{ [85, 145, 120, 95][n-1] }}.000 VNĐ</span>
                <div class="flex items-center bg-surface-container-high rounded-full px-2 py-1">
                  <button class="w-6 h-6 flex items-center justify-center hover:text-primary-container transition-colors"><span class="material-symbols-outlined text-sm">remove</span></button>
                  <span class="w-8 text-center text-xs font-bold">{{ n === 1 ? 1 : 0 }}</span>
                  <button class="w-6 h-6 flex items-center justify-center hover:text-primary-container transition-colors"><span class="material-symbols-outlined text-sm">add</span></button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Persistent Sidebar Summary -->
    <aside class="w-full lg:w-[400px]">
      <div class="glass-card glass-shine-edge sticky top-28 shadow-2xl overflow-hidden rounded-3xl">
        <!-- Movie Header -->
        <div class="p-8 pb-6 border-b border-outline-variant/10">
          <div class="flex gap-6">
            <div class="w-20 h-28 flex-shrink-0 shadow-lg">
              <img src="/images/Hopper.webp" class="w-full h-full object-cover"/>
            </div>
            <div class="flex flex-col justify-center">
              <span class="bg-error-container text-[9px] font-bold uppercase tracking-widest px-2 py-0.5 w-fit mb-2 text-white">T18</span>
              <h2 class="font-headline text-lg font-bold leading-tight uppercase tracking-tight mb-1">DUNE: PART TWO</h2>
              <p class="text-xs text-on-surface-variant font-label">DevCine Landmark 81 • Phòng 05</p>
            </div>
          </div>
        </div>
        <!-- Detailed Selections -->
        <div class="p-8 space-y-6">
          <div>
            <div class="flex justify-between items-center mb-3">
              <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ghế đã chọn</span>
              <span class="text-xs font-bold text-primary-container">B3, B4 (Standard)</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-on-surface/60">2 x 110.000 VNĐ</span>
              <span class="font-semibold">220.000 VNĐ</span>
            </div>
          </div>
          <div class="pt-6 border-t border-outline-variant/10">
            <div class="flex justify-between items-center mb-3">
              <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Bắp nước</span>
              <span class="text-xs font-bold text-primary-container">1 sản phẩm</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-on-surface/60">1 x Combo Solo</span>
              <span class="font-semibold">85.000 VNĐ</span>
            </div>
          </div>
          <!-- Total Calculation -->
          <div class="pt-6 border-t border-outline-variant/10">
            <div class="bg-black/40 border border-white/5 p-5 rounded-xl">
              <div class="flex justify-between items-center mb-1">
                <span class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Tổng thanh toán</span>
                <span class="text-2xl font-headline font-extrabold text-primary-container">305.000 VNĐ</span>
              </div>
              <p class="text-[10px] text-outline-variant text-right italic">(VAT & Phí dịch vụ đã bao gồm)</p>
            </div>
          </div>
          <!-- Action Button -->
          <router-link to="/payment" class="w-full bg-primary-container text-on-primary py-4 rounded-xl font-headline font-extrabold tracking-[0.2em] hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-2 group">
            XÁC NHẬN THANH TOÁN
            <span class="material-symbols-outlined text-lg group-hover:translate-x-1 transition-transform">arrow_forward</span>
          </router-link>
          <p class="text-[10px] text-center text-on-surface-variant/40 leading-relaxed uppercase tracking-tighter">
            Thanh toán an toàn qua Cổng liên kết quốc tế
          </p>
        </div>
      </div>
    </aside>
  </main>
</template>

<style scoped>
.seat-grid { perspective: 1000px; }
.screen-curve { box-shadow: 0 -20px 50px -10px rgba(245, 197, 24, 0.3); }
</style>
