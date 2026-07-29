<script setup>
import { computed, ref, onMounted, onUnmounted, nextTick } from 'vue'
import api from '@/api/axios'
import { useShiftStore } from '@/stores/shift'
import { openInvoice, paymentLabel } from '@/utils/invoiceTemplate'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()

// ===== Helpers hiển thị & dựng bản in vé giấy =====
const seatTypeLabel = (t) =>
  ({ ADULT: 'Người lớn', U22: 'U22 / HSSV', STUDENT: 'Học sinh/Sinh viên', CHILD: 'Trẻ em', SENIOR: 'Người cao tuổi' }[t] || 'Người lớn')

const formatDateTime = (iso) => {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

// Dựng dữ liệu hoá đơn từ phản hồi in vé (1 mã QR chung = mã đặt vé cho mọi vé giấy).
const buildInv = (b) => ({
  bookingCode: b.bookingCode,
  movie: b.movieTitle,
  room: b.roomName,
  format: b.format || '',
  dateStr: formatDateTime(b.startTime),
  counter: 'Quầy in vé',
  seatRows: (b.seats || []).map(s => ({ label: s.seatLabel, seats: seatTypeLabel(s.ticketType), count: 1, unit: Number(s.price || 0), subtotal: Number(s.price || 0) })),
  combos: (b.fnbs || []).map(f => ({ name: f.name, quantity: f.quantity, price: Number(f.price || 0) })),
  seatTotal: (b.seats || []).reduce((a, s) => a + Number(s.price || 0), 0),
  comboTotal: (b.fnbs || []).reduce((a, f) => a + Number(f.price || 0) * Number(f.quantity || 0), 0),
  discount: Number(b.discount || 0),
  grandTotal: Number(b.finalPrice || 0),
  seatCount: b.seatCount,
  paymentLabel: paymentLabel(b.paymentMethod),
  memberName: b.memberName,
  tickets: (b.seats || []).map(s => ({ seatLabel: s.seatLabel, qrCode: b.bookingCode })),
})

const printBooking = (b) => { try { openInvoice(buildInv(b)) } catch (_) {} }

const shiftStore = useShiftStore()
const activeTab = ref('camera') // 'camera' or 'manual'
const qrCodeInput = ref('')
const isLoading = ref(false)
const checkInResult = ref(null) // { success: boolean, data: object, message: string }
const cameraError = ref('')
const isScannerActive = ref(false)
let html5QrCode = null
const canCheckIn = computed(() => shiftStore.canUse(['CHECK_IN']))
const isShiftLocked = computed(() => !canCheckIn.value)
const lockedMessage = computed(() => shiftStore.lockedMessage('kiểm soát vé'))

// Web Audio Beep generator (no external audio files needed)
const playBeep = (type = 'success') => {
  try {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)()
    const oscillator = audioCtx.createOscillator()
    const gainNode = audioCtx.createGain()
    
    oscillator.connect(gainNode)
    gainNode.connect(audioCtx.destination)
    
    if (type === 'success') {
      oscillator.type = 'sine'
      oscillator.frequency.setValueAtTime(880, audioCtx.currentTime) // A5 note
      gainNode.gain.setValueAtTime(0.1, audioCtx.currentTime)
      oscillator.start()
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.15)
      oscillator.stop(audioCtx.currentTime + 0.15)
    } else {
      oscillator.type = 'sawtooth'
      oscillator.frequency.setValueAtTime(220, audioCtx.currentTime) // A3 note
      gainNode.gain.setValueAtTime(0.1, audioCtx.currentTime)
      oscillator.start()
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.3)
      oscillator.stop(audioCtx.currentTime + 0.3)
    }
  } catch (_) {}
}

// Load html5-qrcode script dynamically
const loadScannerScript = () => {
  return new Promise((resolve, reject) => {
    if (window.Html5Qrcode) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = 'https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js'
    script.type = 'text/javascript'
    script.onload = () => resolve()
    script.onerror = (err) => reject(err)
    document.head.appendChild(script)
  })
}

const startCamera = async () => {
  if (isShiftLocked.value) return
  cameraError.value = ''
  try {
    await loadScannerScript()
    if (!window.Html5Qrcode) {
      throw new Error('Không thể tải thư viện quét mã QR')
    }

    // Chờ Vue render lại khung quét trước khi khởi tạo (sau màn kết quả #qr-reader bị gỡ khỏi DOM)
    await nextTick()
    if (!document.getElementById('qr-reader')) {
      // Khung chưa sẵn sàng (vd vừa rời màn kết quả/đổi tab) — sẽ tự khởi động lại khi quay về tab camera
      return
    }

    html5QrCode = new window.Html5Qrcode('qr-reader')
    isScannerActive.value = true
    
    const config = { fps: 10, qrbox: { width: 250, height: 250 } }
    
    await html5QrCode.start(
      { facingMode: 'environment' },
      config,
      (decodedText) => {
        // Stop scanning upon detection
        stopCamera()
        handleCheckIn(decodedText)
      },
      (errorMessage) => {
        // Verbose scanning log (silent)
      }
    )
  } catch (_) {
    cameraError.value = 'Không thể truy cập camera. Vui lòng cấp quyền hoặc nhập mã thủ công.'
    isScannerActive.value = false
  }
}

const stopCamera = async () => {
  if (html5QrCode && html5QrCode.isScanning) {
    try {
      await html5QrCode.stop()
      isScannerActive.value = false
    } catch (_) {}
  }
}

const handleCheckIn = async (code) => {
  if (!code || !code.trim()) return
  if (isShiftLocked.value) {
    checkInResult.value = { success: false, message: lockedMessage.value, data: null }
    playBeep('error')
    return
  }
  
  isLoading.value = true
  checkInResult.value = null

  try {
    // Bước 1: chỉ XÁC MINH đơn (chưa in) → hiển thị "Quét thành công" + chi tiết
    const response = await api.post('/tickets/lookup', null, {
      params: { code: code.trim() }
    })

    checkInResult.value = {
      success: true,
      message: 'Đơn hợp lệ — sẵn sàng in vé.',
      data: response.data,
      printed: false
    }
    playBeep('success')
  } catch (error) {
    const errorMsg = friendlyError(error, 'Đã xảy ra lỗi khi quét vé.')
    checkInResult.value = {
      success: false,
      message: errorMsg,
      data: null,
      printed: false
    }
    playBeep('error')
  } finally {
    isLoading.value = false
  }
}

// Bước 2: bấm "In vé" → đánh dấu đã in + mở cửa sổ in vé giấy
const isPrinting = ref(false)
const doPrint = async () => {
  const booking = checkInResult.value?.data
  if (!booking || isPrinting.value) return
  isPrinting.value = true
  try {
    const response = await api.post('/tickets/print', null, {
      params: { code: booking.bookingCode }
    })
    checkInResult.value = {
      success: true,
      message: 'Đã in vé cho toàn bộ đơn!',
      data: response.data,
      printed: true
    }
    playBeep('success')
    printBooking(response.data) // mở cửa sổ in vé giấy
  } catch (error) {
    checkInResult.value = {
      success: false,
      message: friendlyError(error, 'Không in được vé.'),
      data: null,
      printed: false
    }
    playBeep('error')
  } finally {
    isPrinting.value = false
  }
}

const submitManual = () => {
  if (!qrCodeInput.value.trim()) return
  handleCheckIn(qrCodeInput.value.trim())
}

const resetScanner = () => {
  checkInResult.value = null
  qrCodeInput.value = ''
  if (activeTab.value === 'camera') {
    startCamera()
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'camera') {
    startCamera()
  } else {
    stopCamera()
  }
}

// Quick Mock Tool for Development/Testing
const triggerMockCheckIn = (status = 'success') => {
  stopCamera()
  isLoading.value = true
  setTimeout(() => {
    isLoading.value = false
    if (status === 'success') {
      checkInResult.value = {
        success: true,
        message: 'Đơn hợp lệ — sẵn sàng in vé. (Dữ liệu giả lập)',
        printed: false,
        data: {
          bookingCode: '0AA550BA-0',
          movieTitle: 'Godzilla x Kong: Đế Chế Mới',
          cinemaName: 'Test HN',
          roomName: 'Phòng 1',
          format: '2D',
          startTime: '2026-07-10T16:00:00',
          paymentMethod: 'TRANSFER',
          seatCount: 2,
          seats: [
            { seatLabel: 'A5', ticketType: 'ADULT', price: 110000 },
            { seatLabel: 'A6', ticketType: 'STUDENT', price: 90000 }
          ],
          fnbs: [{ name: 'Combo Bắp Phô Mai', quantity: 1, price: 89000 }],
          totalPrice: 289000,
          finalPrice: 289000,
          discount: 0,
          printedAt: null
        }
      }
      playBeep('success')
    } else {
      checkInResult.value = {
        success: false,
        message: 'Mã đặt vé này đã được in thành vé giấy trước đó vào lúc: 15:30 10/07/2026',
        data: null
      }
      playBeep('error')
    }
  }, 600)
}

onMounted(async () => {
  await shiftStore.fetchCurrent(true)
  if (activeTab.value === 'camera' && !isShiftLocked.value) {
    startCamera()
  }
})

onUnmounted(() => {
  stopCamera()
})
</script>

<template>
  <div class="p-8 max-w-4xl mx-auto space-y-8 flex-grow">
    <!-- Header -->
    <div class="flex justify-between items-center bg-surface p-6 rounded-3xl border border-outline-variant/10 shadow-xl">
      <div class="flex items-center gap-6">
        <div class="w-12 h-12 bg-primary rounded-2xl flex items-center justify-center text-on-primary shadow-lg shadow-primary/20">
          <span class="material-symbols-outlined text-3xl">qr_code_scanner</span>
        </div>
        <div>
          <h1 class="text-2xl font-black tracking-tighter uppercase italic text-on-surface">Quét & In vé</h1>
          <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Quét mã đơn → in toàn bộ vé giấy</p>
        </div>
      </div>
      
      <!-- Quick test controls -->
      <div class="flex gap-2">
        <button @click="triggerMockCheckIn('success')" class="text-[10px] bg-green-500/10 text-green-400 hover:bg-green-500/20 px-3 py-1.5 rounded-full border border-green-500/20 font-bold transition-all cursor-pointer">
          Giả lập Vé Hợp lệ
        </button>
        <button @click="triggerMockCheckIn('fail')" class="text-[10px] bg-red-500/10 text-red-400 hover:bg-red-500/20 px-3 py-1.5 rounded-full border border-red-500/20 font-bold transition-all cursor-pointer">
          Giả lập Vé Trùng
        </button>
      </div>
    </div>

    <!-- Navigation Tabs -->
    <div class="flex bg-surface-container-high p-1.5 rounded-2xl border border-outline-variant/10">
      <button 
        @click="switchTab('camera')"
        :class="activeTab === 'camera' ? 'bg-primary text-on-primary shadow-lg' : 'text-on-surface-variant hover:text-on-surface'"
        class="flex-1 py-3 rounded-xl font-bold text-sm transition-all flex items-center justify-center gap-2 cursor-pointer"
      >
        <span class="material-symbols-outlined text-lg">photo_camera</span>
        Quét Camera QR
      </button>
      <button 
        @click="switchTab('manual')"
        :class="activeTab === 'manual' ? 'bg-primary text-on-primary shadow-lg' : 'text-on-surface-variant hover:text-on-surface'"
        class="flex-1 py-3 rounded-xl font-bold text-sm transition-all flex items-center justify-center gap-2 cursor-pointer"
      >
        <span class="material-symbols-outlined text-lg">keyboard</span>
        Nhập Mã Vé Thủ Công
      </button>
    </div>

    <!-- Main Working Card -->
    <div class="glass-card bg-surface rounded-3xl border border-outline-variant/10 shadow-2xl p-8 overflow-hidden min-h-[400px] flex flex-col justify-center">
      
      <div v-if="isShiftLocked" class="text-center py-14 space-y-4">
        <span class="material-symbols-outlined text-6xl text-on-surface-variant/40">lock_clock</span>
        <h2 class="text-xl font-black text-on-surface">Chưa mở chức năng kiểm soát vé</h2>
        <p class="text-sm text-on-surface-variant">{{ lockedMessage }}</p>
      </div>

      <!-- LOADING STATE -->
      <div v-else-if="isLoading" class="text-center py-12 space-y-4">
        <div class="w-16 h-16 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
        <p class="text-sm font-bold text-on-surface-variant animate-pulse uppercase tracking-wider">Đang xử lý & in vé...</p>
      </div>

      <!-- RESULT STATE -->
      <div v-else-if="checkInResult" class="space-y-8 animate-fade-in">
        <!-- Success Banner -->
        <div v-if="checkInResult.success" class="flex flex-col items-center text-center space-y-4">
          <div class="w-20 h-20 rounded-full flex items-center justify-center shadow-lg"
               :class="checkInResult.printed ? 'bg-green-500/10 text-green-400 border border-green-500/20 shadow-green-500/10' : 'bg-primary/10 text-primary border border-primary/20 shadow-primary/10'">
            <span class="material-symbols-outlined text-5xl animate-bounce">{{ checkInResult.printed ? 'print' : 'check_circle' }}</span>
          </div>
          <div>
            <h2 class="text-2xl font-black uppercase italic" :class="checkInResult.printed ? 'text-green-400' : 'text-primary'">
              {{ checkInResult.printed ? 'Đã In Vé!' : 'Quét Thành Công' }}
            </h2>
            <p class="text-xs text-on-surface-variant mt-1">{{ checkInResult.message }}</p>
          </div>

          <!-- Booking Info Details -->
          <div class="w-full max-w-md bg-surface-container-high rounded-2xl border border-outline-variant/20 p-6 text-left space-y-4">
            <div class="border-b border-outline-variant/10 pb-3 flex justify-between items-center">
              <span class="text-[10px] font-black uppercase text-on-surface-variant tracking-wider">Mã đặt vé</span>
              <span class="font-mono font-bold text-sm text-primary">{{ checkInResult.data.bookingCode }}</span>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="col-span-2">
                <span class="text-[9px] font-bold text-on-surface-variant uppercase block">Phim</span>
                <span class="text-sm font-black text-on-surface line-clamp-1">{{ checkInResult.data.movieTitle }}</span>
              </div>
              <div>
                <span class="text-[9px] font-bold text-on-surface-variant uppercase block">Phòng chiếu</span>
                <span class="text-sm font-bold text-on-surface">{{ checkInResult.data.roomName }}</span>
              </div>
              <div>
                <span class="text-[9px] font-bold text-on-surface-variant uppercase block">Giờ chiếu</span>
                <span class="text-sm font-bold text-on-surface">
                  {{ new Date(checkInResult.data.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }}
                </span>
              </div>
            </div>

            <div>
              <span class="text-[9px] font-bold text-on-surface-variant uppercase block mb-1.5">Ghế ({{ checkInResult.data.seatCount }})</span>
              <div class="flex flex-wrap gap-1.5">
                <span v-for="s in checkInResult.data.seats" :key="s.seatLabel"
                  class="text-xs font-black text-primary bg-primary/10 border border-primary/20 rounded-lg px-2.5 py-1 uppercase">
                  {{ s.seatLabel }}
                  <span class="text-[9px] font-semibold text-on-surface-variant normal-case ml-0.5">· {{ seatTypeLabel(s.ticketType) }}</span>
                </span>
              </div>
            </div>

            <div v-if="checkInResult.data.fnbs && checkInResult.data.fnbs.length">
              <span class="text-[9px] font-bold text-on-surface-variant uppercase block mb-1.5">Combo / Đồ ăn kèm</span>
              <div v-for="f in checkInResult.data.fnbs" :key="f.name" class="text-xs text-on-surface">
                • {{ f.name }} × {{ f.quantity }}
              </div>
            </div>

            <div v-if="checkInResult.printed" class="border-t border-outline-variant/10 pt-3 flex justify-between items-center text-[10px] text-on-surface-variant/70 italic">
              <span>Đã in lúc:</span>
              <span>{{ formatDateTime(checkInResult.data.printedAt) }}</span>
            </div>
          </div>

          <!-- Chưa in: nút In vé (thực sự in + đánh dấu đã in) -->
          <button v-if="!checkInResult.printed" @click="doPrint" :disabled="isPrinting"
            class="flex items-center gap-2 text-sm font-bold text-on-primary bg-primary hover:bg-primary/90 rounded-xl px-8 py-3 shadow-lg shadow-primary/20 hover:scale-[1.02] transition-all disabled:opacity-50 disabled:scale-100 cursor-pointer">
            <span class="material-symbols-outlined text-base">print</span> {{ isPrinting ? 'Đang in...' : 'In vé giấy' }}
          </button>
          <!-- Đã in: cho phép in lại (không đánh dấu lại) -->
          <button v-else @click="printBooking(checkInResult.data)"
            class="flex items-center gap-2 text-xs font-bold text-primary bg-primary/10 hover:bg-primary/20 border border-primary/20 rounded-xl px-5 py-2.5 transition-all cursor-pointer">
            <span class="material-symbols-outlined text-base">print</span> In lại vé giấy
          </button>
        </div>

        <!-- Failure Banner -->
        <div v-else class="flex flex-col items-center text-center space-y-4">
          <div class="w-20 h-20 bg-red-500/10 text-red-400 border border-red-500/20 rounded-full flex items-center justify-center shadow-lg shadow-red-500/10">
            <span class="material-symbols-outlined text-5xl">cancel</span>
          </div>
          <div>
            <h2 class="text-2xl font-black text-red-400 uppercase italic">In Vé Thất Bại</h2>
            <p class="text-sm text-on-surface font-bold mt-2 max-w-md">{{ checkInResult.message }}</p>
            <p class="text-[10px] text-on-surface-variant mt-1">Vui lòng kiểm tra lại mã đặt vé hoặc liên hệ quản lý.</p>
          </div>
        </div>

        <!-- Action Button -->
        <div class="text-center pt-4">
          <button @click="resetScanner" class="bg-primary text-on-primary font-bold px-8 py-3 rounded-xl shadow-lg hover:shadow-primary/20 hover:scale-[1.02] transition-all cursor-pointer">
            Quét đơn tiếp theo
          </button>
        </div>
      </div>

      <!-- CAMERA SCANNER VIEW -->
      <div v-else-if="activeTab === 'camera'" class="flex flex-col items-center gap-6">
        <div class="scanner-frame relative w-full max-w-sm aspect-square rounded-[2rem] overflow-hidden bg-black">

          <div id="qr-reader" class="w-full h-full"></div>

          <!-- Custom scanner overlay -->
          <div v-if="isScannerActive" class="absolute inset-0 pointer-events-none">
            <div class="scan-window">
              <span class="corner corner-tl"></span>
              <span class="corner corner-tr"></span>
              <span class="corner corner-bl"></span>
              <span class="corner corner-br"></span>
              <div class="laser"></div>
            </div>
          </div>

          <!-- Camera error message -->
          <div v-if="cameraError" class="absolute inset-0 flex flex-col items-center justify-center p-8 bg-black/85 backdrop-blur-sm text-center space-y-4">
            <div class="w-16 h-16 rounded-2xl bg-yellow-400/10 border border-yellow-400/20 flex items-center justify-center">
              <span class="material-symbols-outlined text-3xl text-yellow-400">no_photography</span>
            </div>
            <p class="text-sm text-on-surface font-bold leading-relaxed max-w-xs">{{ cameraError }}</p>
            <button @click="startCamera" class="bg-primary text-on-primary font-bold px-6 py-2.5 rounded-xl text-xs shadow-lg hover:scale-[1.03] transition-all cursor-pointer">
              Thử lại
            </button>
          </div>

          <!-- Camera off / idle -->
          <div v-else-if="!isScannerActive" class="absolute inset-0 flex flex-col items-center justify-center gap-5 bg-black/60">
            <div class="w-20 h-20 rounded-3xl bg-primary/10 border border-primary/20 flex items-center justify-center">
              <span class="material-symbols-outlined text-4xl text-primary/70">photo_camera</span>
            </div>
            <button @click="startCamera" class="bg-primary text-on-primary font-bold px-7 py-2.5 rounded-xl text-sm shadow-lg shadow-primary/20 hover:scale-[1.03] transition-all cursor-pointer flex items-center gap-2">
              <span class="material-symbols-outlined text-lg">play_arrow</span>
              Bật Camera
            </button>
          </div>
        </div>

        <!-- Status hint pill -->
        <div class="flex items-center gap-2.5 bg-surface-container-high border border-outline-variant/15 rounded-full pl-3 pr-4 py-2">
          <span
            :class="isScannerActive ? 'bg-green-400 animate-live-dot' : 'bg-on-surface-variant/50'"
            class="w-2 h-2 rounded-full shrink-0"
          ></span>
          <span class="text-[11px] font-bold text-on-surface-variant uppercase tracking-wider">
            {{ isScannerActive ? 'Đang quét — hướng camera vào mã QR' : 'Camera đang tắt' }}
          </span>
        </div>
      </div>

      <!-- MANUAL CODE ENTRY VIEW -->
      <div v-else-if="activeTab === 'manual'" class="max-w-md mx-auto w-full space-y-6">
        <div class="text-center space-y-2">
          <span class="material-symbols-outlined text-4xl text-primary/60">input</span>
          <h2 class="font-black text-lg text-on-surface uppercase">Nhập mã đặt vé thủ công</h2>
          <p class="text-xs text-on-surface-variant">Nhập mã đặt vé (booking code) in trên vé điện tử của khách hàng</p>
        </div>

        <form @submit.prevent="submitManual" class="space-y-4">
          <div class="space-y-2">
            <label class="text-[10px] font-black uppercase text-on-surface-variant tracking-wider">Mã đặt vé</label>
            <input
              v-model="qrCodeInput"
              type="text"
              placeholder="Ví dụ: 0AA550BA-0"
              class="w-full bg-surface-container-high border border-outline-variant/30 focus:border-primary focus:ring-1 focus:ring-primary rounded-xl px-4 py-3.5 text-sm font-mono text-on-surface shadow-inner uppercase tracking-wider"
              required
            >
          </div>

          <button
            type="submit"
            :disabled="!qrCodeInput.trim()"
            class="w-full bg-primary text-on-primary font-bold py-3.5 rounded-xl shadow-lg hover:shadow-primary/20 hover:scale-[1.01] transition-all disabled:opacity-50 disabled:scale-100 disabled:shadow-none cursor-pointer text-center text-sm"
          >
            Xác nhận & In vé
          </button>
        </form>
      </div>
      
    </div>
  </div>
</template>

<style scoped>
/* ===== Strip html5-qrcode default chrome, keep only the video ===== */
:deep(#qr-reader) {
  border: 0 !important;
  background: transparent !important;
  width: 100% !important;
  height: 100% !important;
}
:deep(#qr-reader video) {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
  display: block;
}
:deep(#qr-reader__dashboard) { display: none !important; }
:deep(#qr-reader__scan_region img) { display: none !important; }
:deep(#qr-shaded-region) { display: none !important; }

/* ===== Scanner frame + custom overlay ===== */
.scanner-frame {
  border: 1px solid color-mix(in srgb, var(--md-sys-color-primary) 25%, transparent);
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.4),
    0 20px 45px -15px rgba(0, 0, 0, 0.6),
    0 0 40px -8px color-mix(in srgb, var(--md-sys-color-primary) 30%, transparent);
}

.scan-window {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 64%;
  aspect-ratio: 1;
  border-radius: 1.25rem;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5);
}

.corner {
  position: absolute;
  width: 26px;
  height: 26px;
  border: 3px solid var(--md-sys-color-primary);
}
.corner-tl { top: -2px; left: -2px; border-right: 0; border-bottom: 0; border-top-left-radius: 14px; }
.corner-tr { top: -2px; right: -2px; border-left: 0; border-bottom: 0; border-top-right-radius: 14px; }
.corner-bl { bottom: -2px; left: -2px; border-right: 0; border-top: 0; border-bottom-left-radius: 14px; }
.corner-br { bottom: -2px; right: -2px; border-left: 0; border-top: 0; border-bottom-right-radius: 14px; }

.laser {
  position: absolute;
  left: 7%;
  right: 7%;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, transparent, var(--md-sys-color-primary), transparent);
  box-shadow: 0 0 12px 2px color-mix(in srgb, var(--md-sys-color-primary) 70%, transparent);
  animation: scanner-laser 2.4s ease-in-out infinite;
}

@keyframes scanner-laser {
  0%   { top: 6%; opacity: 0.4; }
  50%  { top: 94%; opacity: 1; }
  100% { top: 6%; opacity: 0.4; }
}

.animate-live-dot {
  animation: liveDot 1.4s ease-in-out infinite;
}
@keyframes liveDot {
  0%, 100% { box-shadow: 0 0 0 0 color-mix(in srgb, var(--md-sys-color-primary) 60%, transparent); opacity: 1; }
  50%      { box-shadow: 0 0 0 5px transparent; opacity: 0.6; }
}

.animate-fade-in {
  animation: fadeIn 0.4s ease-out forwards;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
