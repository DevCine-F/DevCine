<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main>
    <!-- Hero Header -->
    <section class="relative min-h-[540px] pt-32 flex items-end px-6 md:px-10 pb-16 overflow-hidden">
      <div class="absolute inset-0 z-0">
        <img src="/images/Hopper.webp" class="w-full h-full object-cover opacity-40"/>
        <div class="absolute inset-0 hero-gradient"></div>
      </div>
      <div class="relative z-10 max-w-[1440px] mx-auto w-full">
        <h1 class="text-5xl md:text-7xl font-headline font-extrabold tracking-tighter text-on-background mb-4 uppercase">
          Liên hệ & <span class="text-primary-container">Hỗ trợ</span>
        </h1>
        <p class="max-w-2xl text-lg text-on-surface-variant font-body leading-relaxed">
          Tại DevCine, sự hài lòng của quý khách là ưu tiên hàng đầu. Chúng tôi luôn sẵn sàng lắng nghe và hỗ trợ mọi thắc mắc để mang lại trải nghiệm điện ảnh hoàn hảo nhất.
        </p>
      </div>
    </section>

    <div class="max-w-[1440px] mx-auto px-6 md:px-10">
      <!-- Support Categories -->
      <section class="py-20">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <router-link to="/faq" class="glass-card glass-shine-edge p-8 rounded-3xl hover:border-primary-container/30 transition-all duration-300 group">
            <span class="material-symbols-outlined text-4xl text-primary-container mb-6 block">help_outline</span>
            <h3 class="text-xl font-headline font-bold text-on-background mb-2">Câu hỏi thường gặp</h3>
            <p class="text-on-surface-variant text-sm font-body mb-4">Tìm câu trả lời nhanh nhất cho các thắc mắc về quy trình đặt vé và rạp.</p>
            <span class="text-primary-container font-label text-xs uppercase tracking-widest flex items-center gap-2 group-hover:gap-4 transition-all">Xem thêm <span class="material-symbols-outlined text-sm">arrow_forward</span></span>
          </router-link>
          <div class="glass-card glass-shine-edge p-8 rounded-3xl hover:border-primary-container/30 transition-all duration-300 group">
            <span class="material-symbols-outlined text-4xl text-primary-container mb-6 block">confirmation_number</span>
            <h3 class="text-xl font-headline font-bold text-on-background mb-2">Chính sách vé</h3>
            <p class="text-on-surface-variant text-sm font-body mb-4">Thông tin chi tiết về hoàn tiền, đổi trả và các quy định về độ tuổi xem phim.</p>
            <a class="text-primary-container font-label text-xs uppercase tracking-widest flex items-center gap-2 group-hover:gap-4 transition-all" href="#">Xem thêm <span class="material-symbols-outlined text-sm">arrow_forward</span></a>
          </div>
          <div class="glass-card glass-shine-edge p-8 rounded-3xl hover:border-primary-container/30 transition-all duration-300 group">
            <span class="material-symbols-outlined text-4xl text-primary-container mb-6 block">loyalty</span>
            <h3 class="text-xl font-headline font-bold text-on-background mb-2">Thành viên & Ưu đãi</h3>
            <p class="text-on-surface-variant text-sm font-body mb-4">Quản lý tài khoản DevCine Prestige và khám phá các đặc quyền dành riêng cho bạn.</p>
            <a class="text-primary-container font-label text-xs uppercase tracking-widest flex items-center gap-2 group-hover:gap-4 transition-all" href="#">Xem thêm <span class="material-symbols-outlined text-sm">arrow_forward</span></a>
          </div>
          <div class="glass-card glass-shine-edge p-8 rounded-3xl hover:border-primary-container/30 transition-all duration-300 group">
            <span class="material-symbols-outlined text-4xl text-primary-container mb-6 block">rate_review</span>
            <h3 class="text-xl font-headline font-bold text-on-background mb-2">Phản hồi dịch vụ</h3>
            <p class="text-on-surface-variant text-sm font-body mb-4">Góp ý về chất lượng phục vụ tại các cụm rạp để chúng tôi ngày càng hoàn thiện.</p>
            <a class="text-primary-container font-label text-xs uppercase tracking-widest flex items-center gap-2 group-hover:gap-4 transition-all" href="#">Xem thêm <span class="material-symbols-outlined text-sm">arrow_forward</span></a>
          </div>
        </div>
      </section>

      <!-- Main Support Section -->
      <section class="grid grid-cols-1 lg:grid-cols-12 gap-16 pb-20">
        <!-- Contact Form -->
        <div class="lg:col-span-7">
          <h2 class="text-3xl font-headline font-bold text-on-background mb-8 uppercase tracking-tight">Gửi lời nhắn cho chúng tôi</h2>
          <form class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Họ và tên</label>
                <input class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all" placeholder="Nguyễn Văn A" type="text"/>
              </div>
              <div class="space-y-2">
                <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Email</label>
                <input class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all" placeholder="email@example.com" type="email"/>
              </div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Số điện thoại</label>
                <input class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all" placeholder="090 123 4567" type="tel"/>
              </div>
              <div class="space-y-2">
                <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Chủ đề</label>
                <select class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all">
                  <option>Vấn đề về vé</option>
                  <option>Thành viên</option>
                  <option>Góp ý dịch vụ</option>
                  <option>Hợp tác quảng cáo</option>
                </select>
              </div>
            </div>
            <div class="space-y-2">
              <label class="text-xs font-label uppercase tracking-widest text-on-surface-variant">Nội dung tin nhắn</label>
              <textarea class="w-full bg-black/40 border border-white/10 focus:border-primary-container text-white px-4 py-3 rounded-xl outline-none transition-all" placeholder="Vui lòng mô tả chi tiết yêu cầu của bạn..." rows="5"></textarea>
            </div>
            <button class="w-full md:w-auto px-12 py-4 bg-primary-container text-on-primary font-headline font-extrabold uppercase tracking-widest rounded-sm hover:brightness-110 transition-all" type="submit">Gửi yêu cầu</button>
          </form>
        </div>

        <!-- Theater Contact Info -->
        <div class="lg:col-span-5">
          <h2 class="text-3xl font-headline font-bold text-on-background mb-8 uppercase tracking-tight">Hệ thống rạp</h2>
          <div class="space-y-8">
            <div class="flex gap-6 items-start">
              <div class="w-12 h-12 glass-card flex items-center justify-center rounded-xl flex-shrink-0">
                <span class="material-symbols-outlined text-primary-container">location_on</span>
              </div>
              <div>
                <h4 class="text-lg font-headline font-bold text-primary-container mb-1 uppercase tracking-tighter">DEVCINE Landmark 81</h4>
                <p class="text-on-surface-variant font-body text-sm mb-2">Tầng B1, Vincom Center Landmark 81, 720A Điện Biên Phủ, P. 22, Q. Bình Thạnh, TP. HCM</p>
                <p class="text-on-background font-bold text-sm tracking-widest">HOTLINE: 1900 1234</p>
              </div>
            </div>
            <div class="flex gap-6 items-start">
              <div class="w-12 h-12 glass-card flex items-center justify-center rounded-xl flex-shrink-0">
                <span class="material-symbols-outlined text-primary-container">location_on</span>
              </div>
              <div>
                <h4 class="text-lg font-headline font-bold text-primary-container mb-1 uppercase tracking-tighter">DEVCINE Bitexco</h4>
                <p class="text-on-surface-variant font-body text-sm mb-2">Tầng 3-4, Bitexco Financial Tower, 02 Hải Triều, Q. 1, TP. HCM</p>
                <p class="text-on-background font-bold text-sm tracking-widest">HOTLINE: 1900 5678</p>
              </div>
            </div>
          </div>

          <!-- FAQ Quick Links -->
          <div class="mt-16 glass-card glass-shine-edge p-8 rounded-3xl">
            <h4 class="text-xs font-label uppercase tracking-[0.2em] text-on-surface-variant mb-6 border-b border-outline-variant/20 pb-4">Liên kết nhanh</h4>
            <ul class="space-y-4">
              <li><router-link to="/faq" class="flex items-center justify-between text-on-background hover:text-primary-container transition-colors font-body font-semibold">Làm thế nào để đặt vé trực tuyến? <span class="material-symbols-outlined text-lg">chevron_right</span></router-link></li>
              <li><a class="flex items-center justify-between text-on-background hover:text-primary-container transition-colors font-body font-semibold" href="#">Quy định về thẻ thành viên Prestige <span class="material-symbols-outlined text-lg">chevron_right</span></a></li>
              <li><router-link to="/lich-chieu" class="flex items-center justify-between text-on-background hover:text-primary-container transition-colors font-body font-semibold">Lịch chiếu phim tuần này <span class="material-symbols-outlined text-lg">chevron_right</span></router-link></li>
            </ul>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.hero-gradient { background: linear-gradient(180deg, transparent 0%, rgba(10,10,15,0.4) 100%); }
</style>
