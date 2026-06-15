<script setup>
import { reactive } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  newCinema: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close', 'create'])

const handleCreate = () => {
  emit('create')
}
</script>

<template>
  <div v-if="show" class="fixed inset-0 z-[110] flex items-center justify-center bg-black/60 backdrop-blur-sm animate-in fade-in duration-300">
    <!-- Modal Content -->
    <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden flex flex-col slide-in-from-bottom-8">
      <!-- Header -->
      <div class="px-8 py-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-high/30">
        <h2 class="text-xl font-black uppercase tracking-widest text-primary flex items-center gap-3">
          <span class="material-symbols-outlined">add_business</span>
          Thiết lập Cụm Rạp Mới
        </h2>
        <button @click="$emit('close')" class="text-on-surface-variant hover:text-white transition-colors">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      
      <!-- Body -->
      <div class="p-8 space-y-6">
        <div class="grid grid-cols-2 gap-6">
          <!-- Tên Cụm Rạp -->
          <div class="space-y-2 col-span-2 sm:col-span-1">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Tên Cụm Rạp <span class="text-red-500">*</span></label>
            <input v-model="newCinema.name" type="text" placeholder="VD: DevCine Landmark 81" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all placeholder-white/20">
          </div>
          
          <!-- Hotline -->
          <div class="space-y-2 col-span-2 sm:col-span-1">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Hotline <span class="text-red-500">*</span></label>
            <input v-model="newCinema.hotline" type="text" placeholder="VD: 1900 1234" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all placeholder-white/20">
          </div>

          <!-- Địa chỉ -->
          <div class="space-y-2 col-span-2">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Địa chỉ chi tiết <span class="text-red-500">*</span></label>
            <input v-model="newCinema.address" type="text" placeholder="VD: Tầng B1, Vincom Landmark 81, TP.HCM" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all placeholder-white/20">
          </div>

          <!-- Loại rạp -->
          <div class="space-y-2 col-span-2 sm:col-span-1">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Loại Cụm Rạp</label>
            <select v-model="newCinema.type" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all appearance-none cursor-pointer">
              <option value="Standard" class="bg-surface-container-high text-white">Standard</option>
              <option value="Premium/IMAX" class="bg-surface-container-high text-white">Premium/IMAX</option>
              <option value="Sweetbox" class="bg-surface-container-high text-white">Sweetbox</option>
              <option value="Gold Class" class="bg-surface-container-high text-white">Gold Class</option>
            </select>
          </div>

          <!-- Số lượng phòng -->
          <div class="space-y-2 col-span-2 sm:col-span-1">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Số lượng phòng dự kiến</label>
            <input v-model.number="newCinema.rooms" type="number" min="1" max="20" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all">
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="px-8 py-6 border-t border-outline-variant/10 bg-surface-container-high/10 flex justify-end gap-4">
        <button @click="$emit('close')" class="px-6 py-3 rounded-xl border border-white/10 text-on-surface-variant text-[10px] font-black uppercase tracking-widest hover:bg-white/5 transition-all">
          Hủy bỏ
        </button>
        <button @click="handleCreate" class="px-8 py-3 rounded-xl bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest hover:brightness-110 transition-all shadow-lg shadow-primary/20 flex items-center gap-2">
          Tạo Cụm Rạp
        </button>
      </div>
    </div>
  </div>
</template>
