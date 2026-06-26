<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useConfirmStore } from '@/stores/confirm'

const confirm = useConfirmStore()

// Esc = huỷ, Enter = đồng ý (chỉ khi đang mở)
const onKey = (e) => {
  if (!confirm.open) return
  if (e.key === 'Escape') confirm.cancel()
  else if (e.key === 'Enter') confirm.confirm()
}
onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <Transition name="confirm">
      <div v-if="confirm.open" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="confirm.cancel()"></div>

        <!-- Hộp thoại -->
        <div class="relative w-full max-w-md bg-surface border border-outline-variant/20 shadow-2xl rounded-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
          <div class="p-6">
            <div class="flex items-start gap-4">
              <div :class="confirm.tone === 'danger' ? 'bg-red-500/15 text-red-500' : 'bg-primary/15 text-primary'"
                   class="w-11 h-11 rounded-full flex items-center justify-center shrink-0">
                <span class="material-symbols-outlined">{{ confirm.tone === 'danger' ? 'warning' : 'help' }}</span>
              </div>
              <div class="flex-1 min-w-0">
                <h3 class="text-base font-black uppercase tracking-wide text-on-surface">{{ confirm.title }}</h3>
                <p class="text-sm text-on-surface-variant mt-1.5 leading-relaxed">{{ confirm.message }}</p>
              </div>
            </div>
          </div>

          <div class="px-6 py-4 bg-surface-container-lowest border-t border-outline-variant/10 flex justify-end gap-3">
            <button @click="confirm.cancel()"
                    class="px-5 py-2.5 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded hover:bg-white/10 transition-all">
              {{ confirm.cancelText }}
            </button>
            <button @click="confirm.confirm()"
                    :class="confirm.tone === 'danger' ? 'bg-red-500 text-white hover:bg-red-600' : 'bg-primary text-on-primary hover:brightness-110'"
                    class="px-5 py-2.5 font-bold text-xs uppercase tracking-widest rounded transition-all">
              {{ confirm.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.confirm-enter-active, .confirm-leave-active { transition: opacity 0.2s ease; }
.confirm-enter-from, .confirm-leave-to { opacity: 0; }
</style>
