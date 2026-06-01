<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="min-h-screen">
    <!-- Hero Section -->
    <section class="relative min-h-[540px] pt-32 pb-16 flex flex-col items-center justify-center text-center overflow-hidden">
      <div class="absolute inset-0 z-0">
        <img class="w-full h-full object-cover opacity-30" src="/images/Hopper.webp"/>
        <div class="absolute inset-0 bg-gradient-to-t from-surface via-surface/60 to-transparent"></div>
      </div>
      <div class="relative z-10 px-4 max-w-4xl w-full">
        <h1 class="font-headline font-extrabold text-5xl md:text-7xl text-on-surface tracking-tighter mb-8">
          CÂU HỎI <span class="text-primary-container italic">THƯỜNG GẶP</span>
        </h1>
        <div class="relative group max-w-2xl mx-auto">
          <div class="absolute inset-y-0 left-6 flex items-center pointer-events-none">
            <span class="material-symbols-outlined text-primary-container">search</span>
          </div>
          <input class="w-full bg-black/30 border border-white/10 focus:border-primary-container focus:ring-1 focus:ring-primary-container/30 text-white py-5 pl-16 pr-8 rounded-2xl font-body text-lg shadow-2xl placeholder:text-neutral-500 transition-all" placeholder="Tìm kiếm câu trả lời của bạn..." type="text"/>
        </div>
      </div>
    </section>

    <!-- Content Layout -->
    <section class="max-w-[1440px] mx-auto px-6 md:px-10 py-12 flex flex-col md:flex-row gap-12">
      <!-- Sidebar Navigation -->
      <aside class="md:w-1/4 space-y-2">
        <div class="sticky top-32">
          <h3 class="font-headline font-bold text-neutral-500 text-xs uppercase tracking-[0.2em] mb-6 px-4">Danh mục</h3>
          <nav class="flex flex-col gap-1">
            <button class="flex items-center gap-4 px-4 py-4 rounded-lg bg-primary-container text-on-primary font-semibold text-left transition-all">
              <span class="material-symbols-outlined">payments</span>
              <span>Đặt vé & Thanh toán</span>
            </button>
            <button class="flex items-center gap-4 px-4 py-4 rounded-xl text-neutral-400 hover:bg-white/10 hover:text-white text-left transition-all group">
              <span class="material-symbols-outlined group-hover:text-primary-container transition-colors">stars</span>
              <span>Thành viên Prestige</span>
            </button>
            <button class="flex items-center gap-4 px-4 py-4 rounded-xl text-neutral-400 hover:bg-white/10 hover:text-white text-left transition-all group">
              <span class="material-symbols-outlined group-hover:text-primary-container transition-colors">gavel</span>
              <span>Quy định rạp</span>
            </button>
            <button class="flex items-center gap-4 px-4 py-4 rounded-xl text-neutral-400 hover:bg-white/10 hover:text-white text-left transition-all group">
              <span class="material-symbols-outlined group-hover:text-primary-container transition-colors">local_offer</span>
              <span>Ưu đãi & Khuyến mãi</span>
            </button>
          </nav>
          <div class="mt-12 p-6 rounded-2xl glass-card glass-shine-edge">
            <p class="text-sm text-neutral-400 mb-4 font-body leading-relaxed">Không tìm thấy điều bạn cần?</p>
            <router-link to="/contact" class="text-primary-container font-headline font-bold text-sm flex items-center gap-2 group">
              LIÊN HỆ CHÚNG TÔI 
              <span class="material-symbols-outlined text-sm group-hover:translate-x-1 transition-transform">arrow_forward</span>
            </router-link>
          </div>
        </div>
      </aside>

      <!-- FAQ Accordions -->
      <div class="md:w-3/4 space-y-8">
        <div class="mb-10">
          <h2 class="font-headline font-bold text-3xl text-on-surface mb-2">Đặt vé & Thanh toán</h2>
          <div class="h-1 w-20 bg-primary-container"></div>
        </div>
        
        <div class="space-y-4">
          <!-- Question 1 -->
          <details class="group glass-card rounded-2xl overflow-hidden mb-4" open>
            <summary class="flex items-center justify-between p-6 cursor-pointer list-none">
              <h4 class="font-headline font-semibold text-xl text-on-surface group-hover:text-primary-container transition-colors">Làm thế nào để đặt vé trực tuyến?</h4>
              <span class="material-symbols-outlined text-neutral-500 group-open:rotate-180 transition-transform">expand_more</span>
            </summary>
            <div class="px-6 pb-8 text-on-surface-variant font-body leading-relaxed space-y-4">
              <p>Bạn có thể đặt vé dễ dàng thông qua website chính thức hoặc ứng dụng di động của DevCine theo các bước sau:</p>
              <ol class="list-decimal list-inside space-y-2 ml-4">
                <li>Chọn phim và suất chiếu mong muốn.</li>
                <li>Chọn vị trí ghế ngồi (Ghế Standard, VIP hoặc Sweetbox).</li>
                <li>Lựa chọn bắp nước kèm theo (tùy chọn).</li>
                <li>Thanh toán qua các phương thức: Ví điện tử (Momo, ShopeePay), Thẻ ATM/Visa/Mastercard.</li>
              </ol>
              <p>Sau khi đặt vé thành công, một mã QR Code sẽ được gửi về email và mục "Vé của tôi" trong ứng dụng.</p>
            </div>
          </details>
          
          <!-- Question 2 -->
          <details class="group glass-card rounded-2xl overflow-hidden">
            <summary class="flex items-center justify-between p-6 cursor-pointer list-none">
              <h4 class="font-headline font-semibold text-xl text-on-surface group-hover:text-primary-container transition-colors">Hủy vé đã đặt có được hoàn tiền không?</h4>
              <span class="material-symbols-outlined text-neutral-500 group-open:rotate-180 transition-transform">expand_more</span>
            </summary>
            <div class="px-6 pb-8 text-on-surface-variant font-body leading-relaxed">
              <p>Theo quy định của DevCine, vé đã thanh toán thành công <strong>không được hoàn trả hoặc đổi lại</strong> dưới bất kỳ hình thức nào. Quý khách vui lòng kiểm tra kỹ thông tin phim, rạp chiếu, ngày giờ và số ghế trước khi tiến hành thanh toán.</p>
              <div class="mt-4 p-4 bg-error-container/10 border-l-4 border-error-container rounded-r-md">
                <p class="text-error text-sm">Lưu ý: Trong các trường hợp sự cố kỹ thuật từ phía rạp, chúng tôi sẽ hỗ trợ đổi suất chiếu hoặc hoàn tiền tùy theo tình huống cụ thể.</p>
              </div>
            </div>
          </details>
        </div>
        
        <!-- CTA Section -->
        <div class="mt-20 p-12 glass-card glass-shine-edge rounded-3xl relative overflow-hidden text-center">
          <div class="absolute top-0 right-0 w-64 h-64 bg-primary-container/5 rounded-full -translate-y-1/2 translate-x-1/2 blur-3xl"></div>
          <div class="relative z-10">
            <h3 class="font-headline font-bold text-3xl mb-4">Vẫn còn thắc mắc?</h3>
            <p class="text-on-surface-variant mb-8 max-w-lg mx-auto">Đội ngũ hỗ trợ của chúng tôi luôn sẵn sàng giải đáp mọi câu hỏi của bạn 24/7 qua các kênh hotline hoặc chat trực tuyến.</p>
            <div class="flex flex-wrap justify-center gap-4">
              <router-link to="/contact" class="bg-primary-container text-on-primary font-headline font-bold uppercase py-4 px-10 rounded-sm hover:shadow-[0_0_30px_rgba(245,197,24,0.3)] transition-all">
                Liên hệ hỗ trợ
              </router-link>
              <button class="border border-outline-variant/40 text-on-surface font-headline font-bold uppercase py-4 px-10 rounded-sm hover:bg-white/5 transition-all">
                Chat với chúng tôi
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
details > summary::-webkit-details-marker {
  display: none;
}
</style>
