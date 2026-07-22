<script setup>
// Sinh QR ngay trên máy (thư viện qrcode) — KHÔNG gọi dịch vụ ngoài.
// Trước đây dùng api.qrserver.com: mỗi vé một request, mất mạng là hỏng
// và mã vé của khách bị gửi sang bên thứ ba.
import { ref, watch } from 'vue'
import QRCode from 'qrcode'

const props = defineProps({
  value: { type: String, required: true },
  size: { type: Number, default: 120 }
})

const dataUrl = ref('')
const failed = ref(false)

const render = async () => {
  failed.value = false
  if (!props.value) { dataUrl.value = ''; return }
  try {
    dataUrl.value = await QRCode.toDataURL(props.value, {
      width: props.size,
      margin: 0,
      errorCorrectionLevel: 'M',
      color: { dark: '#000000', light: '#ffffff' }
    })
  } catch {
    // Mã hỏng/quá dài — hiện ô trống thay vì để ảnh vỡ
    dataUrl.value = ''
    failed.value = true
  }
}

watch(() => [props.value, props.size], render, { immediate: true })
</script>

<template>
  <img v-if="dataUrl" :src="dataUrl" alt="Mã QR vé" class="bg-white rounded p-0.5 object-contain" />
  <div v-else-if="failed" class="bg-surface-container-highest rounded flex items-center justify-center text-on-surface-variant">
    <span class="material-symbols-outlined text-base">broken_image</span>
  </div>
  <div v-else class="bg-surface-container-highest rounded animate-pulse" />
</template>
