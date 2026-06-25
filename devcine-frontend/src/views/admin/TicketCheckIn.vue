<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import api from '@/api/axios'

const activeTab = ref('camera') // 'camera' or 'manual'
const qrCodeInput = ref('')
const isLoading = ref(false)
const checkInResult = ref(null) // { success: boolean, data: object, message: string }
const cameraError = ref('')
const isScannerActive = ref(false)
let html5QrCode = null

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
  } catch (e) {
    console.error('Audio beep error:', e)
  }
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
  } catch (err) {
    console.error('Camera startup error:', err)
    cameraError.value = 'Không thể truy cập camera. Vui lòng cấp quyền hoặc nhập mã thủ công.'
    isScannerActive.value = false
  }
}

const stopCamera = async () => {
  if (html5QrCode && html5QrCode.isScanning) {
    try {
      await html5QrCode.stop()
      isScannerActive.value = false
    } catch (err) {
      console.error('Error stopping camera:', err)
    }
  }
}

const handleCheckIn = async (code) => {
  if (!code || !code.trim()) return
  
  isLoading.value = true
  checkInResult.value = null
  
  try {
    const response = await api.post('/tickets/check-in', null, {
      params: { qrCode: code }
    })
    
    checkInResult.value = {
      success: true,
      message: response.data.message || 'Check-in thành công!',
      data: response.data
    }
    playBeep('success')
  } catch (error) {
    console.error('Check-in error:', error)
    const errorMsg = error.response?.data?.error || 'Đã xảy ra lỗi khi xác thực vé.'
    checkInResult.value = {
      success: false,
      message: errorMsg,
      data: null
    }
    playBeep('error')
  } finally {
    isLoading.value = false
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
        message: 'Check-in thành công! (Dữ liệu giả lập)',
        data: {
          ticketCode: 'DEVCINE-T-MOCK-' + Math.random().toString(36).substring(2, 7).toUpperCase(),
          seatName: 'H12 (Couples)',
          movieTitle: 'Lật Mặt 7: Một Điều Ước',
          roomName: 'IMAX Phòng 02',
          startTime: '2026-06-15T20:30:00',
          checkInTime: new Date().toISOString()
        }
      }
      playBeep('success')
    } else {
      checkInResult.value = {
        success: false,
        message: 'Vé này đã được check-in trước đó vào lúc: 15/06/2026 19:42:01',
        data: null
      }
      playBeep('error')
    }
  }, 600)
}

onMounted(() => {
  if (activeTab.value === 'camera') {
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
          <h1 class="text-2xl font-black tracking-tighter uppercase italic text-on-surface">Kiểm soát vé</h1>
          <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Quét QR & check-in suất chiếu</p>
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
      
      <!-- LOADING STATE -->
      <div v-if="isLoading" class="text-center py-12 space-y-4">
        <div class="w-16 h-16 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
        <p class="text-sm font-bold text-on-surface-variant animate-pulse uppercase tracking-wider">Đang xác thực thông tin vé...</p>
      </div>

      <!-- RESULT STATE -->
      <div v-else-if="checkInResult" class="space-y-8 animate-fade-in">
        <!-- Success Banner -->
        <div v-if="checkInResult.success" class="flex flex-col items-center text-center space-y-4">
          <div class="w-20 h-20 bg-green-500/10 text-green-400 border border-green-500/20 rounded-full flex items-center justify-center shadow-lg shadow-green-500/10">
            <span class="material-symbols-outlined text-5xl animate-bounce">check_circle</span>
          </div>
          <div>
            <h2 class="text-2xl font-black text-green-400 uppercase italic">Check-in Thành Công!</h2>
            <p class="text-xs text-on-surface-variant mt-1">{{ checkInResult.message }}</p>
          </div>

          <!-- Ticket Info Details -->
          <div class="w-full max-w-md bg-surface-container-high rounded-2xl border border-outline-variant/20 p-6 text-left space-y-4">
            <div class="border-b border-outline-variant/10 pb-3 flex justify-between items-center">
              <span class="text-[10px] font-black uppercase text-on-surface-variant tracking-wider">Mã vé</span>
              <span class="font-mono font-bold text-sm text-primary">{{ checkInResult.data.ticketCode }}</span>
            </div>
            
            <div class="grid grid-cols-2 gap-4">
              <div>
                <span class="text-[9px] font-bold text-on-surface-variant uppercase block">Phim</span>
                <span class="text-sm font-black text-on-surface line-clamp-1">{{ checkInResult.data.movieTitle }}</span>
              </div>
              <div>
                <span class="text-[9px] font-bold text-on-surface-variant uppercase block">Phòng chiếu</span>
                <span class="text-sm font-bold text-on-surface">{{ checkInResult.data.roomName }}</span>
              </div>
              <div>
                <span class="text-[9px] font-bold text-on-surface-variant uppercase block">Ghế</span>
                <span class="text-sm font-black text-primary uppercase">{{ checkInResult.data.seatName }}</span>
              </div>
              <div>
                <span class="text-[9px] font-bold text-on-surface-variant uppercase block">Giờ chiếu</span>
                <span class="text-sm font-bold text-on-surface">
                  {{ new Date(checkInResult.data.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }}
                </span>
              </div>
            </div>

            <div class="border-t border-outline-variant/10 pt-3 flex justify-between items-center text-[10px] text-on-surface-variant/70 italic">
              <span>Kiểm soát lúc:</span>
              <span>{{ new Date(checkInResult.data.checkInTime).toLocaleString() }}</span>
            </div>
          </div>
        </div>

        <!-- Failure Banner -->
        <div v-else class="flex flex-col items-center text-center space-y-4">
          <div class="w-20 h-20 bg-red-500/10 text-red-400 border border-red-500/20 rounded-full flex items-center justify-center shadow-lg shadow-red-500/10">
            <span class="material-symbols-outlined text-5xl">cancel</span>
          </div>
          <div>
            <h2 class="text-2xl font-black text-red-400 uppercase italic">Check-in Thất Bại</h2>
            <p class="text-sm text-on-surface font-bold mt-2 max-w-md">{{ checkInResult.message }}</p>
            <p class="text-[10px] text-on-surface-variant mt-1">Vui lòng kiểm tra lại mã vé hoặc liên hệ quản lý.</p>
          </div>
        </div>

        <!-- Action Button -->
        <div class="text-center pt-4">
          <button @click="resetScanner" class="bg-primary text-on-primary font-bold px-8 py-3 rounded-xl shadow-lg hover:shadow-primary/20 hover:scale-[1.02] transition-all cursor-pointer">
            Tiếp tục quét vé
          </button>
        </div>
      </div>

      <!-- CAMERA SCANNER VIEW -->
      <div v-else-if="activeTab === 'camera'" class="space-y-6 flex flex-col items-center">
        <div class="relative w-full max-w-lg aspect-square bg-black/40 rounded-3xl overflow-hidden border-2 border-outline-variant/20 flex flex-col items-center justify-center">
          
          <div id="qr-reader" class="w-full h-full"></div>
          
          <!-- Scanner overlay borders -->
          <div v-if="isScannerActive" class="absolute inset-0 pointer-events-none border-[30px] border-black/40 flex items-center justify-center">
            <div class="w-64 h-64 border-2 border-dashed border-primary relative flex items-center justify-center">
              <!-- Laser line animation -->
              <div class="absolute w-full h-[2px] bg-primary top-0 left-0 animate-scanner-laser shadow-[0_0_8px_var(--md-sys-color-primary)]"></div>
            </div>
          </div>

          <!-- Camera error / off message -->
          <div v-if="cameraError" class="absolute inset-0 flex flex-col items-center justify-center p-8 bg-black/80 text-center space-y-4">
            <span class="material-symbols-outlined text-4xl text-yellow-400">no_photography</span>
            <p class="text-sm text-on-surface font-bold leading-relaxed">{{ cameraError }}</p>
          </div>
          
          <div v-else-if="!isScannerActive" class="absolute inset-0 flex flex-col items-center justify-center space-y-4">
            <span class="material-symbols-outlined text-5xl text-primary/40">videocam_off</span>
            <button @click="startCamera" class="bg-primary/20 text-primary border border-primary/30 font-bold px-6 py-2 rounded-xl text-xs hover:bg-primary/30 transition-all cursor-pointer">
              Bật Camera
            </button>
          </div>
        </div>
        
        <p class="text-xs text-on-surface-variant font-bold text-center uppercase tracking-wider">Hướng camera của bạn vào mã QR để tự động quét</p>
      </div>

      <!-- MANUAL CODE ENTRY VIEW -->
      <div v-else-if="activeTab === 'manual'" class="max-w-md mx-auto w-full space-y-6">
        <div class="text-center space-y-2">
          <span class="material-symbols-outlined text-4xl text-primary/60">input</span>
          <h2 class="font-black text-lg text-on-surface uppercase">Nhập mã vé thủ công</h2>
          <p class="text-xs text-on-surface-variant">Nhập mã vé in trên vé giấy hoặc vé điện tử của khách hàng</p>
        </div>

        <form @submit.prevent="submitManual" class="space-y-4">
          <div class="space-y-2">
            <label class="text-[10px] font-black uppercase text-on-surface-variant tracking-wider">Mã QR / Code vé</label>
            <input 
              v-model="qrCodeInput"
              type="text" 
              placeholder="Ví dụ: DEVCINE-T-1-F2A8C4B9"
              class="w-full bg-surface-container-high border border-outline-variant/30 focus:border-primary focus:ring-1 focus:ring-primary rounded-xl px-4 py-3.5 text-sm font-mono text-on-surface shadow-inner uppercase tracking-wider"
              required
            >
          </div>

          <button 
            type="submit"
            :disabled="!qrCodeInput.trim()"
            class="w-full bg-primary text-on-primary font-bold py-3.5 rounded-xl shadow-lg hover:shadow-primary/20 hover:scale-[1.01] transition-all disabled:opacity-50 disabled:scale-100 disabled:shadow-none cursor-pointer text-center text-sm"
          >
            Xác thực & Check-in
          </button>
        </form>
      </div>
      
    </div>
  </div>
</template>

<style scoped>
@keyframes scanner-laser {
  0% {
    top: 0%;
  }
  50% {
    top: 100%;
  }
  100% {
    top: 0%;
  }
}

.animate-scanner-laser {
  animation: scanner-laser 2.5s infinite linear;
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
