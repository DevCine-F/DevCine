<script setup>
defineProps({
  variant: {
    type: String,
    default: 'primary' // primary, secondary, outline, ghost
  },
  size: {
    type: String,
    default: 'md' // sm, md, lg
  },
  loading: Boolean,
  disabled: Boolean
})
</script>

<template>
  <button 
    :disabled="disabled || loading"
    class="relative inline-flex items-center justify-center font-bold transition-all active:scale-95 disabled:opacity-50 disabled:active:scale-100 uppercase tracking-widest text-[10px]"
    :class="[
      {
        'bg-primary text-black hover:bg-primary/90 shadow-[0_0_20px_rgba(245,197,24,0.2)]': variant === 'primary',
        'bg-surface-container-high text-on-surface hover:bg-white/5 border border-outline-variant/20': variant === 'secondary',
        'border-2 border-primary text-primary hover:bg-primary/10': variant === 'outline',
        'text-on-surface-variant hover:text-primary hover:bg-white/5': variant === 'ghost'
      },
      {
        'px-4 py-2': size === 'sm',
        'px-8 py-4': size === 'md',
        'px-12 py-6 text-xs': size === 'lg'
      }
    ]"
  >
    <span v-if="loading" class="mr-2 animate-spin">
      <span class="material-symbols-outlined text-sm">progress_activity</span>
    </span>
    <slot />
  </button>
</template>
