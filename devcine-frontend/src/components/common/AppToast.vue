<script setup>
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const ICONS = {
  success: 'check_circle',
  error: 'error',
  warning: 'warning',
  info: 'info'
}
const STYLES = {
  success: 'bg-green-600/95 border-green-400/40',
  error: 'bg-red-600/95 border-red-400/40',
  warning: 'bg-amber-500/95 border-amber-300/40',
  info: 'bg-surface-container-highest/95 border-outline-variant/30'
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed top-6 right-6 z-[1000] flex flex-col gap-3 w-[min(92vw,360px)] pointer-events-none">
      <transition-group name="toast">
        <div
          v-for="t in toast.toasts"
          :key="t.id"
          :class="STYLES[t.type] || STYLES.info"
          class="pointer-events-auto flex items-start gap-3 px-4 py-3.5 rounded-xl border backdrop-blur-md shadow-2xl text-white"
        >
          <span class="material-symbols-outlined text-xl shrink-0 mt-0.5">{{ ICONS[t.type] || ICONS.info }}</span>
          <p class="text-sm font-semibold leading-snug flex-1 break-words">{{ t.message }}</p>
          <button
            @click="toast.remove(t.id)"
            class="shrink-0 -mr-1 text-white/70 hover:text-white transition-colors"
            aria-label="Đóng"
          >
            <span class="material-symbols-outlined text-lg">close</span>
          </button>
        </div>
      </transition-group>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(110%);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(110%);
}
.toast-move {
  transition: transform 0.3s ease;
}
</style>
