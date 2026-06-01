<script setup>
defineProps({
  show: Boolean,
  title: String
})
defineEmits(['close'])
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="show" class="fixed inset-0 z-[999] flex items-center justify-center p-4">
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" @click="$emit('close')"></div>
        
        <!-- Content -->
        <div class="relative w-full max-w-2xl bg-surface border border-outline-variant/20 shadow-2xl rounded-2xl overflow-hidden animate-in fade-in zoom-in duration-300">
          <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center">
            <h3 class="text-lg font-black uppercase tracking-widest text-primary">{{ title }}</h3>
            <button @click="$emit('close')" class="material-symbols-outlined text-on-surface-variant hover:text-white transition-colors">close</button>
          </div>
          
          <div class="p-6">
            <slot />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-enter-active, .modal-leave-active {
  transition: opacity 0.3s ease;
}
.modal-enter-from, .modal-leave-to {
  opacity: 0;
}
</style>
