<script setup>
import { ref } from 'vue'

const settings = ref({
  siteName: 'DevCine Editorial Cinema',
  contactEmail: 'contact@devcine.com',
  baseTicketPrice: '110.000',
  pointConversionRate: '100000',
  maintenanceMode: false
})
</script>

<template>
  <div class="p-10">
    <header class="mb-12 text-on-surface">
      <h1 class="text-3xl font-extrabold tracking-tight font-headline uppercase">Cài đặt Hệ thống</h1>
      <p class="text-on-surface-variant text-sm mt-1">Cấu hình các tham số vận hành của toàn bộ nền tảng DevCine</p>
    </header>

    <div class="max-w-4xl space-y-8">
      <!-- General Settings -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8">
        <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-8 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">settings_applications</span>
          Cấu hình chung
        </h3>
        <div class="space-y-6">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tên Website</label>
              <input v-model="settings.siteName" type="text" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Email liên hệ</label>
              <input v-model="settings.contactEmail" type="email" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
            </div>
          </div>
          <div class="flex items-center justify-between p-4 bg-surface-container-high rounded-lg">
            <div>
              <p class="text-sm font-bold text-on-surface">Chế độ bảo trì</p>
              <p class="text-[10px] text-on-surface-variant uppercase tracking-widest mt-1">Tạm dừng truy cập phía người dùng</p>
            </div>
            <button @click="settings.maintenanceMode = !settings.maintenanceMode" :class="settings.maintenanceMode ? 'bg-primary text-on-primary' : 'bg-surface-container-highest text-on-surface-variant'" class="px-6 py-2 rounded-full text-[10px] font-bold uppercase tracking-widest transition-all">
              {{ settings.maintenanceMode ? 'BẬT' : 'TẮT' }}
            </button>
          </div>
        </div>
      </section>

      <!-- Business Settings -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8">
        <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-8 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">monetization_on</span>
          Cấu hình nghiệp vụ
        </h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Giá vé cơ bản (VNĐ)</label>
            <input v-model="settings.baseTicketPrice" type="text" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
          </div>
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tiền tệ</label>
            <select class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
              <option>Việt Nam Đồng (VNĐ)</option>
              <option>US Dollar ($)</option>
            </select>
          </div>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
          <div class="space-y-4 md:col-span-2 p-6 rounded-2xl bg-surface-container-low border border-outline-variant/10 relative overflow-hidden">
            <!-- Decorative background element for space theme -->
            <div class="absolute top-0 right-0 w-64 h-64 bg-primary/5 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2"></div>
            
            <label class="text-[10px] font-bold uppercase tracking-widest text-primary flex items-center gap-2 relative z-10">
              <span class="material-symbols-outlined text-sm">stars</span>
              Cơ chế Tích luỹ Điểm Thành Viên
            </label>
            
            <div class="flex flex-col md:flex-row items-center gap-4 md:gap-6 relative z-10 mt-2">
              <div class="w-full md:w-1/2 space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mức chi tiêu yêu cầu</label>
                <div class="relative">
                  <input v-model="settings.pointConversionRate" type="number" class="w-full bg-surface-container-highest border border-outline-variant/10 text-sm font-bold rounded-xl focus:border-primary focus:ring-1 focus:ring-primary py-4 px-5 pr-16 text-on-surface transition-all" placeholder="10000">
                  <span class="absolute right-5 top-1/2 -translate-y-1/2 text-xs font-bold text-on-surface-variant pointer-events-none uppercase tracking-widest">VNĐ</span>
                </div>
              </div>
              
              <div class="flex items-center justify-center shrink-0 w-10 h-10 rounded-full bg-surface-container-highest border border-outline-variant/10 hidden md:flex mt-6 shadow-lg shadow-black/20">
                <span class="material-symbols-outlined text-on-surface-variant text-sm">arrow_forward</span>
              </div>
              <div class="flex items-center justify-center shrink-0 h-8 w-full md:hidden mt-2 mb-2">
                <span class="material-symbols-outlined text-on-surface-variant text-sm rotate-90 md:rotate-0">arrow_forward</span>
              </div>

              <div class="w-full md:w-1/2 space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Điểm thưởng nhận được</label>
                <div class="w-full bg-primary/10 border border-primary/20 text-sm font-bold rounded-xl py-4 px-5 text-primary flex items-center justify-between shadow-inner shadow-primary/5">
                  <span class="text-lg">1</span>
                  <span class="text-[10px] font-bold uppercase tracking-widest opacity-80">Điểm (Point)</span>
                </div>
              </div>
            </div>
            
            <p class="text-xs text-on-surface-variant mt-4 italic opacity-75 relative z-10 border-t border-outline-variant/10 pt-4">
              <span class="text-primary font-bold">Ví dụ:</span> Nếu cấu hình 10.000 VNĐ, một hoá đơn 85.000 VNĐ sẽ được cộng 8 điểm (Hệ thống tự động làm tròn xuống phần dư).
            </p>
          </div>
        </div>
      </section>

      <div class="flex justify-end gap-4">
        <button class="px-8 py-3 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded-sm hover:bg-white/10 transition-all border border-outline-variant/20">Hủy bỏ</button>
        <button class="px-8 py-3 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded-sm hover:brightness-110 transition-all">Lưu thay đổi</button>
      </div>
    </div>
  </div>
</template>
