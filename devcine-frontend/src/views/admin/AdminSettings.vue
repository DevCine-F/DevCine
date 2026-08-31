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
  pointConversionRate: 1000,
  seatHoldMinutes: 10,
  posOrderHoldMinutes: 15,
  maxTicketsPerBooking: 8,
  bookingLateMinutes: 15,
  bankCode: '',
  bankName: '',
  accountNo: '',
  accountName: ''
})

const bankErrors = ref({
  bankCode: '',
  accountNo: '',
  accountName: ''
})

const onBankChange = () => {
  const bank = BANKS.find(b => b.code === settings.value.bankCode)
  settings.value.bankName = bank ? bank.name : ''
  validateBankSettings(true)
}

const validateBankSettings = (showErrors = true) => {
  const hasBank = !!settings.value.bankCode
  const hasAccountNo = !!settings.value.accountNo?.trim()
  const hasAccountName = !!settings.value.accountName?.trim()
  const hasAny = hasBank || hasAccountNo || hasAccountName

  let isValid = true
  const errors = {
    bankCode: '',
    accountNo: '',
    accountName: ''
  }

  if (hasAny) {
    if (!hasBank) {
      errors.bankCode = 'Vui lòng chọn ngân hàng nhận tiền'
      isValid = false
    }
    const accNo = settings.value.accountNo?.trim() || ''
    if (!accNo) {
      errors.accountNo = 'Vui lòng nhập số tài khoản'
      isValid = false
    } else if (accNo.length < 4 || accNo.length > 20) {
      errors.accountNo = 'Số tài khoản phải từ 4 đến 20 chữ số'
      isValid = false
    }

    const accName = settings.value.accountName?.trim() || ''
    if (!accName) {
      errors.accountName = 'Vui lòng nhập tên chủ tài khoản'
      isValid = false
    } else if (accName.length < 2) {
      errors.accountName = 'Tên chủ tài khoản phải từ 2 ký tự trở lên'
      isValid = false
    }
  }

  if (showErrors) {
    bankErrors.value = errors
  }
  return isValid
}

// Xem trước mã VietQR của tài khoản nhận tiền (số tiền để trống → khách tự nhập / sẽ điền ở POS)
const qrPreviewUrl = computed(() => {
  const { bankCode, accountNo, accountName } = settings.value
  if (!bankCode || !accountNo || accountNo.trim().length < 4) return ''
  return `https://img.vietqr.io/image/${bankCode}-${accountNo.trim()}-compact2.png?accountName=${encodeURIComponent(accountName?.trim() || '')}`
})

const isInitialLoading = ref(true)
const isLoading = ref(false)

const loadSettings = async () => {
  isLoading.value = true
  try {
    const { data } = await settingsApi.getAll()
    data.forEach(item => {
      if (item.settingKey === 'LOYALTY_POINT_RATE') settings.value.pointConversionRate = parseInt(item.settingValue) || 1000
      else if (item.settingKey === 'SEAT_HOLD_MINUTES') settings.value.seatHoldMinutes = parseInt(item.settingValue) || 10
      else if (item.settingKey === 'POS_ORDER_HOLD_MINUTES') settings.value.posOrderHoldMinutes = parseInt(item.settingValue) || 15
      else if (item.settingKey === 'MAX_TICKETS_PER_BOOKING') settings.value.maxTicketsPerBooking = parseInt(item.settingValue) || 8
      else if (item.settingKey === 'BOOKING_LATE_MINUTES') settings.value.bookingLateMinutes = parseInt(item.settingValue) || 15
      else if (item.settingKey === 'PAYMENT_BANK_CODE') settings.value.bankCode = item.settingValue || ''
      else if (item.settingKey === 'PAYMENT_BANK_NAME') settings.value.bankName = item.settingValue || ''
      else if (item.settingKey === 'PAYMENT_ACCOUNT_NO') settings.value.accountNo = item.settingValue || ''
      else if (item.settingKey === 'PAYMENT_ACCOUNT_NAME') settings.value.accountName = item.settingValue || ''
    })
    bankErrors.value = { bankCode: '', accountNo: '', accountName: '' }
  } catch (err) {
    toast.error(friendlyError(err, 'Không tải được cài đặt hệ thống.'))
  } finally {
    isLoading.value = false
    isInitialLoading.value = false
  }
}

const saveSettings = async () => {
  // Validate ràng buộc tài khoản nhận tiền VietQR và hiển thị lỗi inline
  if (!validateBankSettings(true)) {
    return
  }

  // Kẹp các tham số nghiệp vụ về khoảng cho phép trước khi lưu
  settings.value.seatHoldMinutes = Math.min(30, Math.max(3, parseInt(settings.value.seatHoldMinutes) || 10))
  settings.value.posOrderHoldMinutes = Math.min(60, Math.max(3, parseInt(settings.value.posOrderHoldMinutes) || 15))
  settings.value.maxTicketsPerBooking = Math.min(20, Math.max(1, parseInt(settings.value.maxTicketsPerBooking) || 8))
  settings.value.bookingLateMinutes = Math.min(60, Math.max(0, parseInt(settings.value.bookingLateMinutes) || 15))
  isLoading.value = true
  try {
    await Promise.all([
      settingsApi.save({ settingKey: 'LOYALTY_POINT_RATE', settingValue: settings.value.pointConversionRate.toString() }),
      settingsApi.save({ settingKey: 'SEAT_HOLD_MINUTES', settingValue: settings.value.seatHoldMinutes.toString() }),
      settingsApi.save({ settingKey: 'POS_ORDER_HOLD_MINUTES', settingValue: settings.value.posOrderHoldMinutes.toString() }),
      settingsApi.save({ settingKey: 'MAX_TICKETS_PER_BOOKING', settingValue: settings.value.maxTicketsPerBooking.toString() }),
      settingsApi.save({ settingKey: 'BOOKING_LATE_MINUTES', settingValue: settings.value.bookingLateMinutes.toString() }),
      settingsApi.save({ settingKey: 'PAYMENT_BANK_CODE', settingValue: settings.value.bankCode }),
      settingsApi.save({ settingKey: 'PAYMENT_BANK_NAME', settingValue: settings.value.bankName }),
      settingsApi.save({ settingKey: 'PAYMENT_ACCOUNT_NO', settingValue: settings.value.accountNo?.trim() || '' }),
      settingsApi.save({ settingKey: 'PAYMENT_ACCOUNT_NAME', settingValue: settings.value.accountName?.trim() || '' })
    ])
    toast.success('Đã lưu cấu hình hệ thống.')
  } catch (err) {
    toast.error(friendlyError(err, 'Lưu cấu hình hệ thống thất bại.'))
  } finally {
    isLoading.value = false
  }
}

const fmtThousand = (n) => (n === null || n === undefined || n === '' ? '' : Number(n).toLocaleString('vi-VN'))

const handlePointRateInput = (event) => {
  const input = event.target
  const rawOldVal = input.value || ''
  const caretPos = input.selectionStart || 0

  const digitsBefore = rawOldVal.slice(0, caretPos).replace(/\D/g, '').length
  let cleanDigits = rawOldVal.replace(/\D/g, '').replace(/^0+(?=\d)/, '')
  if (cleanDigits.length > 9) cleanDigits = cleanDigits.slice(0, 9)

  const numVal = cleanDigits ? Math.min(Number(cleanDigits), 100000000) : 0
  settings.value.pointConversionRate = numVal

  const formattedVal = cleanDigits ? numVal.toLocaleString('vi-VN') : ''
  input.value = formattedVal

  let newCaretPos = 0
  let digitsCount = 0
  for (let i = 0; i < formattedVal.length; i++) {
    if (/\d/.test(formattedVal[i])) digitsCount++
    if (digitsCount === digitsBefore) {
      newCaretPos = i + 1
      break
    }
  }
  if (digitsBefore === 0) newCaretPos = 0
  input.setSelectionRange(newCaretPos, newCaretPos)
}

const handleAccountNoInput = (event) => {
  const input = event.target
  let clean = (input.value || '').replace(/\D/g, '').slice(0, 20)
  settings.value.accountNo = clean
  input.value = clean
  validateBankSettings(true)
}

const removeDiacritics = (s) => String(s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D')

const handleAccountNameInput = (event) => {
  const input = event.target
  let clean = removeDiacritics(input.value).toUpperCase()
  clean = clean.replace(/[^A-Z\s]/g, '').replace(/\s+/g, ' ').slice(0, 50)
  settings.value.accountName = clean
  input.value = clean
  validateBankSettings(true)
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

    <!-- Skeleton Loading State (Tránh nhảy số mặc định khi đang tải từ DB) -->
    <div v-if="isInitialLoading" class="max-w-4xl space-y-8 animate-pulse">
      <!-- Skeleton 1: Cấu hình nghiệp vụ -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8 space-y-6">
        <div class="h-6 w-48 bg-surface-container-high rounded-md"></div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-2">
            <div class="h-3 w-32 bg-surface-container-high rounded"></div>
            <div class="h-14 bg-surface-container-high rounded-xl"></div>
            <div class="h-3 w-40 bg-surface-container-high/60 rounded"></div>
          </div>
          <div class="space-y-2">
            <div class="h-3 w-44 bg-surface-container-high rounded"></div>
            <div class="h-14 bg-surface-container-high rounded-xl"></div>
            <div class="h-3 w-48 bg-surface-container-high/60 rounded"></div>
          </div>
        </div>
        <div class="p-6 rounded-2xl bg-surface-container-high/40 border border-outline-variant/10 space-y-4">
          <div class="h-4 w-52 bg-surface-container-high rounded"></div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="h-14 bg-surface-container-high rounded-xl"></div>
            <div class="h-14 bg-surface-container-high rounded-xl"></div>
          </div>
        </div>
      </section>

      <!-- Skeleton 2: Cấu hình thời gian giữ ghế & giữ đơn -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8 space-y-6">
        <div class="space-y-2">
          <div class="h-6 w-64 bg-surface-container-high rounded-md"></div>
          <div class="h-3 w-96 bg-surface-container-high/60 rounded"></div>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-2">
            <div class="h-3 w-36 bg-surface-container-high rounded"></div>
            <div class="h-14 bg-surface-container-high rounded-xl"></div>
            <div class="h-3 w-44 bg-surface-container-high/60 rounded"></div>
          </div>
          <div class="space-y-2">
            <div class="h-3 w-40 bg-surface-container-high rounded"></div>
            <div class="h-14 bg-surface-container-high rounded-xl"></div>
            <div class="h-3 w-48 bg-surface-container-high/60 rounded"></div>
          </div>
        </div>
      </section>

      <!-- Skeleton 3: Tài khoản nhận tiền VietQR -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8 space-y-6">
        <div class="space-y-2">
          <div class="h-6 w-60 bg-surface-container-high rounded-md"></div>
          <div class="h-3 w-80 bg-surface-container-high/60 rounded"></div>
        </div>
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div class="lg:col-span-2 space-y-6">
            <div class="space-y-2">
              <div class="h-3 w-20 bg-surface-container-high rounded"></div>
              <div class="h-14 bg-surface-container-high rounded-xl"></div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <div class="h-3 w-28 bg-surface-container-high rounded"></div>
                <div class="h-14 bg-surface-container-high rounded-xl"></div>
              </div>
              <div class="space-y-2">
                <div class="h-3 w-24 bg-surface-container-high rounded"></div>
                <div class="h-14 bg-surface-container-high rounded-xl"></div>
              </div>
            </div>
          </div>
          <div class="h-64 bg-surface-container-high rounded-2xl"></div>
        </div>
      </section>
    </div>

    <!-- Actual Settings Form -->
    <div v-else class="max-w-4xl space-y-8">

      <!-- Business Settings -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8">
        <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-8 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">monetization_on</span>
          Cấu hình nghiệp vụ
        </h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Số lượng vé tối đa mỗi giao dịch</label>
            <div class="relative">
              <input v-model.number="settings.maxTicketsPerBooking" :disabled="isLoading" type="number" min="1" max="20"
                     class="w-full bg-surface-container-high border border-outline-variant/10 text-sm font-bold rounded-xl focus:border-primary focus:ring-1 focus:ring-primary py-4 px-5 pr-20 text-on-surface transition-all">
              <span class="absolute right-5 top-1/2 -translate-y-1/2 text-xs font-bold text-on-surface-variant pointer-events-none uppercase tracking-widest">Vé</span>
            </div>
            <p class="text-[10px] text-on-surface-variant/70">Giới hạn số vé tối đa cho mỗi lượt đặt.<br>Khoảng từ 1–20 vé.</p>
          </div>
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thời gian mở bán sau giờ khởi chiếu tại quầy (POS)</label>
            <div class="relative">
              <input v-model.number="settings.bookingLateMinutes" :disabled="isLoading" type="number" min="0" max="60"
                     class="w-full bg-surface-container-high border border-outline-variant/10 text-sm font-bold rounded-xl focus:border-primary focus:ring-1 focus:ring-primary py-4 px-5 pr-20 text-on-surface transition-all">
              <span class="absolute right-5 top-1/2 -translate-y-1/2 text-xs font-bold text-on-surface-variant pointer-events-none uppercase tracking-widest">Phút</span>
            </div>
            <p class="text-[10px] text-on-surface-variant/70">Khoảng thời gian vẫn cho phép tiếp tục bán vé sau khi suất chiếu đã bắt đầu.<br>Khoảng từ 0–60 phút.</p>
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
                  <input :value="fmtThousand(settings.pointConversionRate)" @input="handlePointRateInput" :disabled="isLoading" type="text" inputmode="numeric" class="w-full bg-surface-container-highest border border-outline-variant/10 text-sm font-bold rounded-xl focus:border-primary focus:ring-1 focus:ring-primary py-4 px-5 pr-20 text-on-surface transition-all tabular-nums" placeholder="1.000">
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

      <!-- Seat & POS Order Hold Time Settings -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8">
        <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-2 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">timer</span>
          Cấu hình thời gian giữ ghế &amp; giữ đơn
        </h3>
        <p class="text-xs text-on-surface-variant mb-8">Quy định thời hạn khóa ghế trong phiên giao dịch và thời gian lưu trữ hóa đơn chờ tại quầy. Quá hạn, hệ thống sẽ tự động hủy đơn và giải phóng ghế.</p>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thời gian giữ chỗ phiên đặt vé</label>
            <div class="relative">
              <input v-model.number="settings.seatHoldMinutes" :disabled="isLoading" type="number" min="3" max="30"
                     class="w-full bg-surface-container-high border border-outline-variant/10 text-sm font-bold rounded-xl focus:border-primary focus:ring-1 focus:ring-primary py-4 px-5 pr-20 text-on-surface transition-all">
              <span class="absolute right-5 top-1/2 -translate-y-1/2 text-xs font-bold text-on-surface-variant pointer-events-none uppercase tracking-widest">Phút</span>
            </div>
            <p class="text-[10px] text-on-surface-variant/70">Thời gian khóa ghế tạm thời cho mỗi phiên đặt vé (Online/POS).<br>Khoảng từ 3–30 phút.</p>
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thời gian lưu đơn chờ tại quầy (POS)</label>
            <div class="relative">
              <input v-model.number="settings.posOrderHoldMinutes" :disabled="isLoading" type="number" min="3" max="60"
                     class="w-full bg-surface-container-high border border-outline-variant/10 text-sm font-bold rounded-xl focus:border-primary focus:ring-1 focus:ring-primary py-4 px-5 pr-20 text-on-surface transition-all">
              <span class="absolute right-5 top-1/2 -translate-y-1/2 text-xs font-bold text-on-surface-variant pointer-events-none uppercase tracking-widest">Phút</span>
            </div>
            <p class="text-[10px] text-on-surface-variant/70">Thời gian duy trì đơn khi thu ngân bấm "Giữ đơn" để phục vụ khách tiếp theo.<br>Khoảng từ 3–60 phút.</p>
          </div>
        </div>
      </section>

      <!-- Payment / Bank Account Settings -->
      <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8">
        <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-2 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">qr_code_2</span>
          Tài khoản nhận tiền (QR chuyển khoản)
        </h3>
        <p class="text-xs text-on-surface-variant mb-8">Thông tin này dùng để sinh mã VietQR ở bước thanh toán trực tuyến (Online) và tại quầy (POS). Khách quét mã sẽ tự điền số tiền &amp; nội dung đơn hàng.</p>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div class="lg:col-span-2 space-y-6">
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngân hàng</label>
              <select v-model="settings.bankCode" @change="onBankChange" :disabled="isLoading"
                      :class="bankErrors.bankCode ? '!border-red-500 !ring-1 !ring-red-500/50' : 'border-outline-variant/10 focus:border-primary focus:ring-primary'"
                      class="w-full bg-surface-container-high border text-sm font-medium rounded-xl focus:ring-1 py-4 px-5 text-on-surface transition-all">
                <option value="">— Chọn ngân hàng —</option>
                <option v-for="b in BANKS" :key="b.code" :value="b.code">{{ b.name }}</option>
              </select>
              <p v-if="bankErrors.bankCode" class="text-[11px] text-red-500 flex items-center gap-1 font-medium mt-1">
                <span class="material-symbols-outlined text-sm text-red-500">error</span>
                {{ bankErrors.bankCode }}
              </p>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Số tài khoản (STK)</label>
                <input :value="settings.accountNo" @input="handleAccountNoInput" :disabled="isLoading" type="text" inputmode="numeric" maxlength="20" placeholder="VD: 0123456789"
                       :class="bankErrors.accountNo ? '!border-red-500 !ring-1 !ring-red-500/50' : 'border-outline-variant/10 focus:border-primary focus:ring-primary'"
                       class="w-full bg-surface-container-high border text-sm font-bold rounded-xl focus:ring-1 py-4 px-5 text-on-surface transition-all tabular-nums">
                <p v-if="bankErrors.accountNo" class="text-[11px] text-red-500 flex items-center gap-1 font-medium mt-1">
                  <span class="material-symbols-outlined text-sm text-red-500">error</span>
                  {{ bankErrors.accountNo }}
                </p>
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Chủ tài khoản</label>
                <input :value="settings.accountName" @input="handleAccountNameInput" :disabled="isLoading" type="text" maxlength="50" placeholder="VD: NGUYEN VAN A"
                       :class="bankErrors.accountName ? '!border-red-500 !ring-1 !ring-red-500/50' : 'border-outline-variant/10 focus:border-primary focus:ring-primary'"
                       class="w-full bg-surface-container-high border text-sm font-bold rounded-xl focus:ring-1 py-4 px-5 text-on-surface uppercase transition-all">
                <p v-if="bankErrors.accountName" class="text-[11px] text-red-500 flex items-center gap-1 font-medium mt-1">
                  <span class="material-symbols-outlined text-sm text-red-500">error</span>
                  {{ bankErrors.accountName }}
                </p>
              </div>
            </div>
            <p class="text-xs text-on-surface-variant italic opacity-75 border-t border-outline-variant/10 pt-4">
              <span class="text-primary font-bold">Mẹo:</span> Tên chủ tài khoản sẽ tự động chuyển thành IN HOA KHÔNG DẤU để khớp chuẩn liên ngân hàng.
            </p>
          </div>

          <!-- QR Preview -->
          <div class="flex flex-col items-center justify-center gap-3 p-5 rounded-2xl bg-surface-container-high border border-outline-variant/10">
            <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Xem trước mã QR</p>
            <div class="w-44 h-44 rounded-xl bg-white flex items-center justify-center overflow-hidden">
              <img v-if="qrPreviewUrl" :src="qrPreviewUrl" alt="VietQR preview" class="w-full h-full object-contain" />
              <div v-else class="text-center text-surface-container-highest px-4">
                <span class="material-symbols-outlined text-5xl text-gray-300">qr_code_2</span>
                <p class="text-[10px] font-bold text-gray-400 mt-1">Nhập đủ NH &amp; STK để xem QR</p>
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
