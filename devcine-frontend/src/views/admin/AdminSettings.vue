<script setup>
import { ref } from 'vue'

const settings = ref({
  siteName: 'DevCine Editorial Cinema',
  contactEmail: 'contact@devcine.com',
  baseTicketPrice: '110.000',
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
      </section>

      <div class="flex justify-end gap-4">
        <button class="px-8 py-3 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded-sm hover:bg-white/10 transition-all border border-outline-variant/20">Hủy bỏ</button>
        <button class="px-8 py-3 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded-sm hover:brightness-110 transition-all">Lưu thay đổi</button>
      </div>
    </div>
  </div>
</template>
