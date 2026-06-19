<script setup>
import { ref, onMounted } from 'vue'
import { settingsApi } from '@/api/admin'

const settings = ref({
  siteName: 'DevCine Editorial Cinema',
  contactEmail: 'contact@devcine.com',
  baseTicketPrice: '110.000',
  pointConversionRate: 1000,
  maintenanceMode: false
})

const isLoading = ref(false)
const notification = ref({ show: false, message: '', type: 'success' })

const showNotification = (message, type = 'success') => {
  notification.value = { show: true, message, type }
  setTimeout(() => {
    notification.value.show = false
  }, 3000)
}

const loadSettings = async () => {
  isLoading.value = true
  try {
    const { data } = await settingsApi.getAll()
    data.forEach(item => {
      if (item.settingKey === 'SITE_NAME') settings.value.siteName = item.settingValue
      else if (item.settingKey === 'CONTACT_EMAIL') settings.value.contactEmail = item.settingValue
      else if (item.settingKey === 'BASE_TICKET_PRICE') settings.value.baseTicketPrice = item.settingValue
      else if (item.settingKey === 'LOYALTY_POINT_RATE') settings.value.pointConversionRate = parseInt(item.settingValue) || 1000
      else if (item.settingKey === 'MAINTENANCE_MODE') settings.value.maintenanceMode = item.settingValue === 'true'
    })
  } catch (err) {
    console.error('Failed to load settings', err)
    showNotification('Lỗi khi tải cài đặt từ hệ thống!', 'error')
  } finally {
    isLoading.value = false
  }
}

const saveSettings = async () => {
  isLoading.value = true
  try {
    await Promise.all([
      settingsApi.save({ settingKey: 'SITE_NAME', settingValue: settings.value.siteName }),
      settingsApi.save({ settingKey: 'CONTACT_EMAIL', settingValue: settings.value.contactEmail }),
      settingsApi.save({ settingKey: 'BASE_TICKET_PRICE', settingValue: settings.value.baseTicketPrice }),
      settingsApi.save({ settingKey: 'LOYALTY_POINT_RATE', settingValue: settings.value.pointConversionRate.toString() }),
      settingsApi.save({ settingKey: 'MAINTENANCE_MODE', settingValue: settings.value.maintenanceMode.toString() })
    ])
    showNotification('Cấu hình hệ thống đã được lưu thành công!')
  } catch (err) {
    console.error('Failed to save settings', err)
    showNotification('Lỗi khi lưu cấu hình hệ thống!', 'error')
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<template>
  <div class="p-10">
    <!-- Toast Notification -->
    <div v-if="notification.show" :class="[notification.type === 'success' ? 'bg-green-950/80 border-green-500 text-green-300 shadow-green-500/10' : 'bg-red-950/80 border-red-500 text-red-300 shadow-red-500/10', 'fixed top-24 right-10 z-50 flex items-center gap-3 px-6 py-4 rounded-xl border backdrop-blur-md shadow-2xl transition-all duration-300 animate-slide-in']">
      <span class="material-symbols-outlined text-xl">{{ notification.type === 'success' ? 'check_circle' : 'error' }}</span>
      <span class="text-xs font-bold uppercase tracking-wider">{{ notification.message }}</span>
    </div>

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
              <input v-model="settings.siteName" :disabled="isLoading" type="text" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Email liên hệ</label>
              <input v-model="settings.contactEmail" :disabled="isLoading" type="email" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
            </div>
          </div>
          <div class="flex items-center justify-between p-4 bg-surface-container-high rounded-lg">
            <div>
              <p class="text-sm font-bold text-on-surface">Chế độ bảo trì</p>
              <p class="text-[10px] text-on-surface-variant uppercase tracking-widest mt-1">Tạm dừng truy cập phía người dùng</p>
            </div>
            <button @click="settings.maintenanceMode = !settings.maintenanceMode" :disabled="isLoading" :class="settings.maintenanceMode ? 'bg-primary text-on-primary' : 'bg-surface-container-highest text-on-surface-variant'" class="px-6 py-2 rounded-full text-[10px] font-bold uppercase tracking-widest transition-all">
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
            <input v-model="settings.baseTicketPrice" :disabled="isLoading" type="text" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
          </div>
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tiền tệ</label>
            <select :disabled="isLoading" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
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
                  <input v-model="settings.pointConversionRate" :disabled="isLoading" type="number" class="w-full bg-surface-container-highest border border-outline-variant/10 text-sm font-bold rounded-xl focus:border-primary focus:ring-1 focus:ring-primary py-4 px-5 pr-16 text-on-surface transition-all" placeholder="1000">
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
              <span class="text-primary font-bold">Ví dụ:</span> Nếu cấu hình {{ settings.pointConversionRate?.toLocaleString() }} VNĐ, một hoá đơn 85.000 VNĐ sẽ được cộng {{ Math.floor(85000 / (settings.pointConversionRate || 1000)) }} điểm (Hệ thống tự động làm tròn xuống phần dư).
            </p>
          </div>
        </div>
      </section>

      <div class="flex justify-end gap-4">
        <button @click="loadSettings" :disabled="isLoading" class="px-8 py-3 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded-sm hover:bg-white/10 transition-all border border-outline-variant/20 disabled:opacity-50">Hủy bỏ</button>
        <button @click="saveSettings" :disabled="isLoading" class="px-8 py-3 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded-sm hover:brightness-110 transition-all disabled:opacity-50">
          {{ isLoading ? 'Đang lưu...' : 'Lưu thay đổi' }}
        </button>
      </div>
    </div>
  </div>
</template>
