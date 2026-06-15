<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  cinema: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['close', 'save'])

const tempCleaningTime = ref(20)

watch(() => props.show, (newVal) => {
  if (newVal) {
    tempCleaningTime.value = props.cinema?.cleaningTime || 20
  }
})

const handleSave = () => {
  emit('save', tempCleaningTime.value)
}
</script>

<template>
  <div v-if="show" class="fixed inset-0 z-[120] flex items-center justify-center bg-black/60 backdrop-blur-sm animate-in fade-in duration-300">
    <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden flex flex-col slide-in-from-bottom-8">
      <div class="px-6 py-4 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-high/30">
        <h2 class="text-lg font-black uppercase tracking-widest text-primary flex items-center gap-2">
          <span class="material-symbols-outlined">cleaning_services</span> Cài đặt dọn dẹp
        </h2>
      </div>
      <div class="p-6 space-y-4">
        <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Thời gian dọn dẹp (phút)</label>
        <input v-model.number="tempCleaningTime" type="number" min="0" step="5" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 outline-none transition-all">
        <p class="text-[9px] text-on-surface-variant italic">Áp dụng cho tất cả các phòng chiếu của cụm rạp {{ cinema?.name }}.</p>
      </div>
      <div class="px-6 py-4 border-t border-outline-variant/10 bg-surface-container-high/10 flex justify-end gap-3">
        <button @click="$emit('close')" class="px-4 py-2 rounded-xl border border-white/10 text-on-surface-variant text-[10px] font-black uppercase tracking-widest hover:bg-white/5">Hủy</button>
        <button @click="handleSave" class="px-6 py-2 rounded-xl bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest hover:brightness-110 shadow-lg shadow-primary/20">Lưu</button>
      </div>
    </div>
  </div>
</template>
