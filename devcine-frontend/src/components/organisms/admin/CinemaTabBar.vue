<script setup>
defineProps({
  tabs: {
    type: Array,
    required: true
  },
  activeTab: {
    type: String,
    required: true
  }
})

defineEmits(['update:activeTab'])
</script>

<template>
  <div
    class="flex border-b border-outline-variant/10 bg-on-surface/[0.02] p-2 overflow-x-auto no-scrollbar"
  >
    <button
      v-for="tab in tabs"
      :key="tab.id"
      @click="$emit('update:activeTab', tab.id)"
      :class="[
        activeTab === tab.id
          ? 'bg-surface-container-high text-primary shadow-sm border-outline-variant/20'
          : 'text-on-surface-variant hover:text-on-surface border-transparent',
      ]"
      class="flex items-center gap-3 px-8 py-5 text-xs font-black font-headline uppercase tracking-widest transition-all rounded-2xl border flex-shrink-0"
    >
      <span class="material-symbols-outlined text-xl">{{
        tab.icon
      }}</span>
      {{ tab.label }}
    </button>
  </div>
</template>
