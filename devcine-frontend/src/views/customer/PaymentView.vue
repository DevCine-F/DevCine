<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="pt-32 pb-24 px-10 max-w-[1440px] mx-auto min-h-screen">
    <!-- Countdown Timer Banner -->
    <div class="mb-10 bg-error-container/20 border border-error-container/30 p-4 flex items-center justify-center gap-3">
      <span class="material-symbols-outlined text-error animate-pulse-soft">schedule</span>
      <p class="text-error font-bold uppercase tracking-widest text-sm">Ghế được giữ trong <span id="timer">09:55</span></p>
    </div>
    
    <div class="flex flex-col lg:flex-row gap-12 items-start">
      <!-- Left Side: Payment Method Selection -->
      <div class="w-full lg:w-3/5 space-y-12">
        <div>
          <h1 class="text-4xl md:text-5xl font-bold tracking-tighter mb-4 uppercase">PHƯƠNG THỨC THANH TOÁN</h1>
          <p class="text-on-surface-variant font-body leading-relaxed max-w-xl">Hoàn tất đặt vé của bạn một cách an toàn. Vui lòng chọn phương thức thanh toán nội địa hoặc quốc tế bên dưới.</p>
        </div>
        
        <div class="space-y-4">
          <!-- Credit Card Option -->
          <div class="group relative bg-surface-container-high p-6 transition-all duration-300 hover:bg-surface-bright cursor-pointer ring-1 ring-outline-variant/20 hover:ring-primary-container/40">
            <input checked="" class="hidden" id="cc" name="payment" type="radio"/>
            <label class="flex items-center justify-between cursor-pointer" for="cc">
              <div class="flex items-center gap-4">
                <span class="material-symbols-outlined text-primary-container text-3xl">credit_card</span>
                <div>
                  <h3 class="font-headline font-bold text-lg">Thẻ Tín dụng / Ghi nợ</h3>
                  <p class="text-on-surface-variant text-sm font-body">Visa, Mastercard, JCB, American Express</p>
                </div>
              </div>
              <div class="w-6 h-6 rounded-full border-2 border-outline-variant group-hover:border-primary-container flex items-center justify-center">
                <div class="w-3 h-3 bg-primary-container rounded-full opacity-100"></div>
              </div>
            </label>
            <div class="mt-6 grid grid-cols-2 gap-4">
              <div class="col-span-2">
                <label class="block text-[0.6875rem] font-bold uppercase tracking-widest text-on-surface-variant mb-2">Số thẻ</label>
                <input class="w-full bg-surface-container-lowest border border-outline-variant/20 focus:border-primary-container/50 focus:ring-0 text-on-surface p-4 text-sm tracking-[0.2em]" placeholder="•••• •••• •••• ••••" type="text"/>
              </div>
              <div>
                <label class="block text-[0.6875rem] font-bold uppercase tracking-widest text-on-surface-variant mb-2">Ngày hết hạn</label>
                <input class="w-full bg-surface-container-lowest border border-outline-variant/20 focus:border-primary-container/50 focus:ring-0 text-on-surface p-4 text-sm" placeholder="MM / YY" type="text"/>
              </div>
              <div>
                <label class="block text-[0.6875rem] font-bold uppercase tracking-widest text-on-surface-variant mb-2">Mã CVV</label>
                <input class="w-full bg-surface-container-lowest border border-outline-variant/20 focus:border-primary-container/50 focus:ring-0 text-on-surface p-4 text-sm" placeholder="•••" type="password"/>
              </div>
            </div>
          </div>

          <!-- Momo E-Wallet -->
          <div class="group bg-surface-container-low p-6 transition-all duration-300 hover:bg-surface-bright cursor-pointer ring-1 ring-outline-variant/20">
            <input class="hidden" id="momo" name="payment" type="radio"/>
            <label class="flex items-center justify-between cursor-pointer" for="momo">
              <div class="flex items-center gap-4">
                <div class="w-8 h-8 rounded-lg bg-[#A50064] flex items-center justify-center text-white font-bold text-[10px]">MoMo</div>
                <div>
                  <h3 class="font-headline font-bold text-lg">Ví MoMo</h3>
                  <p class="text-on-surface-variant text-sm font-body">Thanh toán qua ứng dụng hoặc mã QR MoMo</p>
                </div>
              </div>
              <div class="w-6 h-6 rounded-full border-2 border-outline-variant group-hover:border-primary-container flex items-center justify-center">
                <div class="w-3 h-3 bg-primary-container rounded-full opacity-0 group-active:opacity-100"></div>
              </div>
            </label>
          </div>

          <!-- VNPAY Option -->
          <div class="group bg-surface-container-low p-6 transition-all duration-300 hover:bg-surface-bright cursor-pointer ring-1 ring-outline-variant/20">
            <input class="hidden" id="vnpay" name="payment" type="radio"/>
            <label class="flex items-center justify-between cursor-pointer" for="vnpay">
              <div class="flex items-center gap-4">
                <div class="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center text-white font-bold text-[10px]">VNPAY</div>
                <div>
                  <h3 class="font-headline font-bold text-lg">VNPAY</h3>
                  <p class="text-on-surface-variant text-sm font-body">Hỗ trợ thanh toán QR từ tất cả ngân hàng nội địa</p>
                </div>
              </div>
              <div class="w-6 h-6 rounded-full border-2 border-outline-variant group-hover:border-primary-container flex items-center justify-center">
                <div class="w-3 h-3 bg-primary-container rounded-full opacity-0 group-active:opacity-100"></div>
              </div>
            </label>
          </div>

          <!-- ATM Option -->
          <div class="group bg-surface-container-low p-6 transition-all duration-300 hover:bg-surface-bright cursor-pointer ring-1 ring-outline-variant/20">
            <input class="hidden" id="atm" name="payment" type="radio"/>
            <label class="flex items-center justify-between cursor-pointer" for="atm">
              <div class="flex items-center gap-4">
                <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary-container text-3xl">account_balance</span>
                <div>
                  <h3 class="font-headline font-bold text-lg">Thẻ ATM Nội địa</h3>
                  <p class="text-on-surface-variant text-sm font-body">Kết nối qua cổng NAPAS (hơn 32 ngân hàng)</p>
                </div>
              </div>
              <div class="w-6 h-6 rounded-full border-2 border-outline-variant group-hover:border-primary-container flex items-center justify-center">
                <div class="w-3 h-3 bg-primary-container rounded-full opacity-0 group-active:opacity-100"></div>
              </div>
            </label>
          </div>
        </div>
        
        <div class="flex items-center gap-4 p-4 bg-surface-container/50 text-on-surface-variant text-xs font-body italic border-l-2 border-primary-container">
          <span class="material-symbols-outlined text-sm">lock</span> 
          Giao dịch của bạn được mã hóa bằng giao thức SSL 256-bit. DevCine Noir Editorial không lưu trữ dữ liệu thẻ tín dụng thô.
        </div>
      </div>

      <!-- Right Side: Sticky Order Summary -->
      <aside class="w-full lg:w-2/5 lg:sticky lg:top-32">
        <div class="bg-surface-container-high p-8 ring-1 ring-outline-variant/20 relative overflow-hidden">
          <div class="absolute inset-0 bg-gradient-to-br from-primary-container/5 to-transparent pointer-events-none"></div>
          <div class="relative z-10">
            <h2 class="text-[0.6875rem] font-bold uppercase tracking-[0.3em] text-primary-container mb-8">CHI TIẾT ĐẶT VÉ</h2>
            
            <div class="flex gap-6 mb-8">
              <img src="/images/Hopper.webp" class="w-24 h-36 object-cover ring-1 ring-outline-variant/20" />
              <div class="flex flex-col justify-center">
                <h3 class="text-2xl font-bold tracking-tight leading-tight mb-2">NOIR: THE LAST BREATH</h3>
                <div class="flex flex-wrap gap-2">
                  <span class="bg-error-container text-[10px] font-bold px-2 py-0.5 tracking-tighter">T18</span>
                  <span class="text-on-surface-variant text-[10px] font-bold uppercase tracking-widest">IMAX 4K</span>
                  <span class="text-primary-container text-[10px] font-bold uppercase tracking-widest">English Sub</span>
                </div>
              </div>
            </div>

            <div class="space-y-6 border-y border-outline-variant/10 py-8">
              <div class="flex justify-between items-start">
                <div class="space-y-1">
                  <p class="text-[0.6875rem] font-bold uppercase tracking-widest text-on-surface-variant">RẠP & THỜI GIAN</p>
                  <p class="text-sm font-semibold">DevCine Grand Premiere, Hall 07</p>
                  <p class="text-sm text-on-surface-variant">Thứ Tư, 20.12.2024 | 19:00</p>
                </div>
                <div class="text-right">
                  <p class="text-[0.6875rem] font-bold uppercase tracking-widest text-on-surface-variant">GHẾ</p>
                  <p class="text-sm font-bold text-primary-container">J12, J13 (VIP)</p>
                </div>
              </div>

              <div class="space-y-4">
                <p class="text-[0.6875rem] font-bold uppercase tracking-widest text-on-surface-variant">COMBO ĐÃ CHỌN</p>
                <div class="flex justify-between items-center text-sm">
                  <span>Midnight Black Popcorn (L)</span>
                  <span class="font-semibold">$18.00</span>
                </div>
                <div class="flex justify-between items-center text-sm">
                  <span>Gold-Label Whiskey Soda</span>
                  <span class="font-semibold">$12.00</span>
                </div>
              </div>
            </div>

            <div class="pt-8 space-y-4">
              <div class="flex justify-between items-center text-sm text-on-surface-variant">
                <span>Giá vé (2x $12.50)</span>
                <span>$25.00</span>
              </div>
              <div class="flex justify-between items-center text-sm text-on-surface-variant">
                <span>Tổng cộng bắp nước</span>
                <span>$39.00</span>
              </div>
              <div class="flex justify-between items-center text-sm text-on-surface-variant">
                <span>Phí đặt vé</span>
                <span>$2.50</span>
              </div>
              <div class="flex justify-between items-center pt-4 border-t border-dashed border-outline-variant/30">
                <span class="text-lg font-bold tracking-tight">Tổng số tiền</span>
                <span class="text-3xl font-bold text-primary-container tracking-tighter">$66.50</span>
              </div>
            </div>
            
            <router-link to="/success" class="w-full mt-10 bg-primary-container text-on-primary py-5 font-bold uppercase tracking-[0.2em] text-sm hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-3">
              XÁC NHẬN VÀ THANH TOÁN 
              <span class="material-symbols-outlined text-lg">arrow_forward</span>
            </router-link>
          </div>
        </div>
      </aside>
    </div>
  </main>
</template>

<style scoped>
@keyframes pulse-soft {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}
.animate-pulse-soft {
  animation: pulse-soft 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
</style>
