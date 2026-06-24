<script setup>
import { computed, watch, onUnmounted } from 'vue'

const props = defineProps({
  show: Boolean,
  url: { type: String, default: '' }
})
const emit = defineEmits(['close'])

// Chuẩn hoá link YouTube (watch?v= / youtu.be / embed) sang dạng nhúng + autoplay
const embedUrl = computed(() => {
  const url = props.url
  if (!url) return ''
  let m = url.match(/youtu\.be\/([\w-]+)/)
  if (m) return `https://www.youtube.com/embed/${m[1]}?autoplay=1`
  m = url.match(/[?&]v=([\w-]+)/)
  if (m) return `https://www.youtube.com/embed/${m[1]}?autoplay=1`
  return url // đã là embed hoặc link trực tiếp
})

const close = () => emit('close')
const handleKeydown = (e) => { if (e.key === 'Escape') close() }

// Chỉ lắng nghe Esc khi modal mở; dọn listener khi đóng/huỷ (tránh memory leak)
watch(() => props.show, (open) => {
  if (open) window.addEventListener('keydown', handleKeydown)
  else window.removeEventListener('keydown', handleKeydown)
})
onUnmounted(() => window.removeEventListener('keydown', handleKeydown))
</script>

<template>
  <Teleport to="body">
    <Transition name="trailer">
      <div v-if="show" @click.self="close" class="fixed inset-0 z-[999] bg-black/85 backdrop-blur-sm flex items-center justify-center p-4">
        <div class="relative w-full max-w-[960px] aspect-video bg-black rounded-xl overflow-hidden shadow-2xl">
          <button @click="close" class="absolute -top-11 right-0 md:top-3 md:right-3 z-10 w-9 h-9 flex items-center justify-center rounded-full bg-black/60 text-white hover:bg-[#f5c518] hover:text-black transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
          <iframe v-if="embedUrl" :src="embedUrl" class="w-full h-full" frameborder="0" allow="autoplay; encrypted-media; fullscreen" allowfullscreen></iframe>
          <div v-else class="w-full h-full flex items-center justify-center text-gray-400 text-sm">Chưa có trailer cho phim này.</div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.trailer-enter-active, .trailer-leave-active { transition: opacity 0.25s ease; }
.trailer-enter-from, .trailer-leave-to { opacity: 0; }
</style>
