<script setup>
import SeatMapBuilder from '@/components/admin/SeatMapBuilder.vue'

defineProps({
  viewingHall: {
    type: Object,
    required: true
  },
  currentSeatMap: {
    type: Object,
    required: true
  },
  tempRows: {
    type: Number,
    required: true
  },
  tempCols: {
    type: Number,
    required: true
  },
  isSavingLayout: {
    type: Boolean,
    default: false
  }
})

defineEmits(['back', 'reset', 'save', 'update:layout'])
</script>

<template>
  <div
    class="animate-in fade-in slide-in-from-right-8 duration-700 flex flex-col h-[calc(100vh-120px)]"
  >
    <header
      class="flex justify-between items-center mb-8 px-4 flex-shrink-0"
    >
      <div class="flex items-center gap-6">
        <button
          @click="$emit('back')"
          class="w-10 h-10 flex items-center justify-center rounded-full bg-on-surface/5 border border-white/10 text-on-surface hover:text-primary transition-all"
        >
          <span class="material-symbols-outlined text-lg">arrow_back</span>
        </button>
        <div>
          <h1
            class="text-2xl font-black tracking-tight font-headline uppercase text-on-surface flex items-center gap-3"
          >
            {{ viewingHall.name }}
            <span class="text-primary/30 text-lg">/</span>
            <span class="text-primary text-lg">{{ viewingHall.type }}</span>
          </h1>
        </div>
      </div>
      <div class="flex gap-3">
        <button
          @click="$emit('reset')"
          class="px-6 py-2.5 rounded-lg border border-white/10 text-on-surface-variant text-[9px] font-black uppercase tracking-widest hover:bg-white/5 transition-all"
        >
          Đặt lại
        </button>
        <button
          @click="$emit('save')"
          :disabled="isSavingLayout"
          class="px-8 py-2.5 rounded-lg bg-primary text-on-primary text-[9px] font-black uppercase tracking-widest hover:brightness-110 transition-all shadow-xl shadow-primary/20 flex items-center gap-2 disabled:opacity-50"
        >
          <span class="material-symbols-outlined text-sm">
            {{ isSavingLayout ? 'hourglass_empty' : 'save' }}
          </span>
          {{ isSavingLayout ? 'Đang lưu...' : 'Lưu Cấu Trúc' }}
        </button>
      </div>
    </header>

    <div class="flex-grow overflow-hidden min-h-0">
      <SeatMapBuilder 
        :initial-rows="tempRows" 
        :initial-cols="tempCols" 
        :initial-seat-map="currentSeatMap" 
        @update:layout="(data) => $emit('update:layout', data)" 
      />
    </div>
  </div>
</template>
