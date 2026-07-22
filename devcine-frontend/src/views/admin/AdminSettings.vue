<script setup>
import { ref, computed, onMounted } from 'vue'
import { settingsApi } from '@/api/admin'
import { useAdminPerm } from '@/composables/useAdminPerm'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'


const { can } = useAdminPerm()
const toast = useToastStore()

// Danh sách ngân hàng hỗ trợ VietQR (mã = BIN napas247)
const BANKS = [
  { code: '970436', name: 'Vietcombank' },
  { code: '970407', name: 'Techcombank' },
  { code: '970418', name: 'BIDV' },
  { code: '970415', name: 'VietinBank' },
  { code: '970422', name: 'MB Bank' },
  { code: '970416', name: 'ACB' },
  { code: '970432', name: 'VPBank' },
  { code: '970423', name: 'TPBank' },
  { code: '970403', name: 'Sacombank' },
  { code: '970405', name: 'Agribank' },
  { code: '970441', name: 'VIB' },
  { code: '970443', name: 'SHB' },
  { code: '970437', name: 'HDBank' },
  { code: '970448', name: 'OCB' },
  { code: '970426', name: 'MSB' },
  { code: '970431', name: 'Eximbank' },
  { code: '970449', name: 'LPBank' },
  { code: '970440', name: 'SeABank' },
  { code: '970412', name: 'PVcomBank' },
  { code: '970419', name: 'NCB' }
]

const settings = ref({
  siteName: 'DevCine Editorial Cinema',
  contactEmail: 'contact@devcine.com',
  baseTicketPrice: '110.000',
  pointConversionRate: 1000,
  seatHoldMinutes: 10,
  maxTicketsPerBooking: 8,
  bookingLateMinutes: 15,
  maintenanceMode: false,
  bankCode: '',
  bankName: '',
  accountNo: '',
  accountName: ''
})

const onBankChange = () => {
  const bank = BANKS.find(b => b.code === settings.value.bankCode)
  settings.value.bankName = bank ? bank.name : ''
}

// Xem trước mã VietQR của tài khoản nhận tiền (số tiền để trống → khách tự nhập / sẽ điền ở POS)
const qrPreviewUrl = computed(() => {
  const { bankCode, accountNo, accountName } = settings.value
  if (!bankCode || !accountNo) return ''
  return `https://img.vietqr.io/image/${bankCode}-${accountNo}-compact2.png?accountName=${encodeURIComponent(accountName || '')}`
})

const isLoading = ref(false)

const loadSettings = async () => {
  isLoading.value = true
  try {
    const { data } = await settingsApi.getAll()
    data.forEach(item => {
      if (item.settingKey === 'SITE_NAME') settings.value.siteName = item.settingValue
      else if (item.settingKey === 'CONTACT_EMAIL') settings.value.contactEmail = item.settingValue
      else if (item.settingKey === 'BASE_TICKET_PRICE') settings.value.baseTicketPrice = item.settingValue
      else if (item.settingKey === 'LOYALTY_POINT_RATE') settings.value.pointConversionRate = parseInt(item.settingValue) || 1000
      else if (item.settingKey === 'SEAT_HOLD_MINUTES') settings.value.seatHoldMinutes = parseInt(item.settingValue) || 10
      else if (item.settingKey === 'MAX_TICKETS_PER_BOOKING') settings.value.maxTicketsPerBooking = parseInt(item.settingValue) || 8
      else if (item.settingKey === 'BOOKING_LATE_MINUTES') settings.value.bookingLateMinutes = parseInt(item.settingValue) || 15
      else if (item.settingKey === 'MAINTENANCE_MODE') settings.value.maintenanceMode = item.settingValue === 'true'
      else if (item.settingKey === 'PAYMENT_BANK_CODE') settings.value.bankCode = item.settingValue || ''
      else if (item.settingKey === 'PAYMENT_BANK_NAME') settings.value.bankName = item.settingValue || ''
      else if (item.settingKey === 'PAYMENT_ACCOUNT_NO') settings.value.accountNo = item.settingValue || ''
      else if (item.settingKey === 'PAYMENT_ACCOUNT_NAME') settings.value.accountName = item.settingValue || ''
    })
  } catch (err) {
    toast.error(friendlyError(err, 'Không tải được cài đặt hệ thống.'))
  } finally {
    isLoading.value = false
  }
}

const saveSettings = async () => {
  // Kẹp các tham số nghiệp vụ về khoảng cho phép trước khi lưu
  settings.value.seatHoldMinutes = Math.min(30, Math.max(3, parseInt(settings.value.seatHoldMinutes) || 10))
  settings.value.maxTicketsPerBooking = Math.min(20, Math.max(1, parseInt(settings.value.maxTicketsPerBooking) || 8))
  settings.value.bookingLateMinutes = Math.min(60, Math.max(0, parseInt(settings.value.bookingLateMinutes) || 15))
  isLoading.value = true
  try {
    await Promise.all([
      settingsApi.save({ settingKey: 'SITE_NAME', settingValue: settings.value.siteName }),
      settingsApi.save({ settingKey: 'CONTACT_EMAIL', settingValue: settings.value.contactEmail }),
      settingsApi.save({ settingKey: 'BASE_TICKET_PRICE', settingValue: settings.value.baseTicketPrice }),
      settingsApi.save({ settingKey: 'LOYALTY_POINT_RATE', settingValue: settings.value.pointConversionRate.toString() }),
      settingsApi.save({ settingKey: 'SEAT_HOLD_MINUTES', settingValue: settings.value.seatHoldMinutes.toString() }),
      settingsApi.save({ settingKey: 'MAX_TICKETS_PER_BOOKING', settingValue: settings.value.maxTicketsPerBooking.toString() }),
      settingsApi.save({ settingKey: 'BOOKING_LATE_MINUTES', settingValue: settings.value.bookingLateMinutes.toString() }),
      settingsApi.save({ settingKey: 'MAINTENANCE_MODE', settingValue: settings.value.maintenanceMode.toString() }),
      settingsApi.save({ settingKey: 'PAYMENT_BANK_CODE', settingValue: settings.value.bankCode }),
      settingsApi.save({ settingKey: 'PAYMENT_BANK_NAME', settingValue: settings.value.bankName }),
      settingsApi.save({ settingKey: 'PAYMENT_ACCOUNT_NO', settingValue: settings.value.accountNo }),
      settingsApi.save({ settingKey: 'PAYMENT_ACCOUNT_NAME', settingValue: settings.value.accountName })
    ])
    toast.success('Đã lưu cấu hình hệ thống.')
  } catch (err) {
    toast.error(friendlyError(err, 'Lưu cấu hình hệ thống thất bại.'))
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
            <button @click="settings.maintenanceMode = !settings.maintenanceMode" :disabled="isLoading || !can('settings', 'edit')" :class="settings.maintenanceMode ? 'bg-primary text-on-primary' : 'bg-surface-container-highest text-on-surface-variant'" class="px-6 py-2 rounded-full text-[10px] font-bold uppercase tracking-widest transition-all disabled:opacity-50 disabled:cursor-not-allowed">
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
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Số vé tối đa / lần đặt</label>
            <div class="relative">
              <input v-model.number="settings.maxTicketsPerBooking" :disabled="isLoading" type="number" min="1" max="20"
                     class="w-full bg-surface-container-high border-none text-sm font-bold rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 pr-16 text-on-surface">
              <span class="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-bold text-on-surface-variant pointer-events-none uppercase tracking-widest">Vé</span>
            </div>
            <p class="text-[10px] text-on-surface-variant/70">Chống đầu cơ/phe vé. Khoảng 1–20, mặc định 8.</p>
          </div>
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Bán vé trễ sau giờ chiếu</label>
            <div class="relative">
              <input v-model.number="settings.bookingLateMinutes" :disabled="isLoading" type="number" min="0" max="60"
                     class="w-full bg-surface-container-high border-none text-sm font-bold rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 pr-16 text-on-surface">
              <span class="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-bold text-on-surface-variant pointer-events-none uppercase tracking-widest">Phút</span>
            </div>
            <p class="text-[10px] text-on-surface-variant/70">Sau giờ chiếu vẫn cho mua trong khoảng này. Vd 15 → phim 19:30 bán đến 19:45.</p>
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

      <!-- Seat Hold Time Settings -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8">
        <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-2 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">timer</span>
          Cấu hình thời gian giữ ghế
        </h3>
        <p class="text-xs text-on-surface-variant mb-8">Khi khách chọn ghế và sang bước thanh toán, ghế sẽ được giữ trong khoảng thời gian này. Quá hạn, hệ thống tự động nhả ghế để khách khác đặt.</p>

        <div class="space-y-2 w-full md:w-auto">
          <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thời gian giữ ghế</label>
          <div class="relative w-full md:w-56">
            <input v-model.number="settings.seatHoldMinutes" :disabled="isLoading" type="number" min="3" max="30"
                   class="w-full bg-surface-container-high border border-outline-variant/10 text-sm font-bold rounded-xl focus:border-primary focus:ring-1 focus:ring-primary py-4 px-5 pr-20 text-on-surface transition-all">
            <span class="absolute right-5 top-1/2 -translate-y-1/2 text-xs font-bold text-on-surface-variant pointer-events-none uppercase tracking-widest">Phút</span>
          </div>
        </div>
      </section>

      <!-- Payment / Bank Account Settings -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8">
        <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-2 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">qr_code_2</span>
          Tài khoản nhận tiền (QR chuyển khoản)
        </h3>
        <p class="text-xs text-on-surface-variant mb-8">Thông tin này dùng để sinh mã VietQR ở bước thanh toán tại quầy (POS). Khách quét mã sẽ tự điền số tiền &amp; nội dung đơn hàng.</p>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div class="lg:col-span-2 space-y-6">
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngân hàng</label>
              <select v-model="settings.bankCode" @change="onBankChange" :disabled="isLoading" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
                <option value="">— Chọn ngân hàng —</option>
                <option v-for="b in BANKS" :key="b.code" :value="b.code">{{ b.name }}</option>
              </select>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Số tài khoản (STK)</label>
                <input v-model.trim="settings.accountNo" :disabled="isLoading" type="text" inputmode="numeric" placeholder="VD: 0123456789" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Chủ tài khoản</label>
                <input v-model.trim="settings.accountName" :disabled="isLoading" type="text" placeholder="VD: NGUYEN VAN A" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface uppercase">
              </div>
            </div>
            <p class="text-xs text-on-surface-variant italic opacity-75 border-t border-outline-variant/10 pt-4">
              <span class="text-primary font-bold">Mẹo:</span> Tên chủ tài khoản nên viết IN HOA không dấu để khớp hiển thị trên app ngân hàng.
            </p>
          </div>

          <!-- QR Preview -->
          <div class="flex flex-col items-center justify-center gap-3 p-5 rounded-2xl bg-surface-container-high border border-outline-variant/10">
            <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Xem trước mã QR</p>
            <div class="w-44 h-44 rounded-xl bg-white flex items-center justify-center overflow-hidden">
              <img v-if="qrPreviewUrl" :src="qrPreviewUrl" alt="VietQR preview" class="w-full h-full object-contain" />
              <div v-else class="text-center text-surface-container-highest px-4">
                <span class="material-symbols-outlined text-5xl text-gray-300">qr_code_2</span>
                <p class="text-[10px] font-bold text-gray-400 mt-1">Nhập NH &amp; STK để xem QR</p>
              </div>
            </div>
            <p v-if="settings.bankName" class="text-xs font-bold text-on-surface text-center">{{ settings.bankName }}<br><span class="text-on-surface-variant font-mono">{{ settings.accountNo }}</span></p>
          </div>
        </div>
      </section>

      <div v-if="!can('settings', 'edit')" class="flex justify-end">
        <p class="text-xs text-on-surface-variant italic flex items-center gap-2">
          <span class="material-symbols-outlined text-base">lock</span>
          Chỉ xem — bạn không có quyền thay đổi cài đặt hệ thống.
        </p>
      </div>
      <div v-else class="flex justify-end gap-4">
        <button @click="loadSettings" :disabled="isLoading" class="px-8 py-3 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded-sm hover:bg-white/10 transition-all border border-outline-variant/20 disabled:opacity-50">Hủy bỏ</button>
        <button @click="saveSettings" :disabled="isLoading" class="px-8 py-3 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded-sm hover:brightness-110 transition-all disabled:opacity-50">
          {{ isLoading ? 'Đang lưu...' : 'Lưu thay đổi' }}
        </button>
      </div>
    </div>
  </div>
</template>
