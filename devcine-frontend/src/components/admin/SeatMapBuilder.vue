<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  initialRows: { type: Number, default: 10 },
  initialCols: { type: Number, default: 12 },
  initialSeatMap: { type: Object, default: () => ({}) },
  readonly: { type: Boolean, default: false },
  soldTickets: { type: Number, default: 0 },
  canceledTickets: { type: Number, default: 0 },
  revenue: { type: String, default: '0đ' }
})

const emit = defineEmits(['update:layout'])

const rows = ref(props.initialRows)
const cols = ref(props.initialCols)
const selectedTool = ref('standard')
const seatMap = reactive({ ...props.initialSeatMap })
const isMouseDown = ref(false)

const totalSeats = computed(() => {
  return Object.values(seatMap).filter(s => s.type !== 'aisle' && s.type !== 'remove' && s).length;
})

// Cập nhật lại khi prop thay đổi
watch(() => props.initialRows, (val) => { rows.value = val })
watch(() => props.initialCols, (val) => { cols.value = val })
watch(() => props.initialSeatMap, (val) => {
  for (const key in seatMap) {
    delete seatMap[key]
  }
  Object.assign(seatMap, val)
}, { deep: true })

const toggleTool = (id) => {
  if (selectedTool.value === id) {
    selectedTool.value = null // Hủy chọn nếu nhấn lại
  } else {
    selectedTool.value = id
  }
}

const initializeMap = () => {
  for (let r = 0; r < rows.value; r++) {
    for (let c = 0; c < cols.value; c++) {
      const key = `${r}-${c}`
      if (!seatMap[key]) seatMap[key] = { type: 'standard', label: `${String.fromCharCode(65 + r)}${c + 1}` }
    }
  }
  emitUpdate()
}

const isOccupiedByDouble = (r, c) => {
  if (c === 0) return false
  return seatMap[`${r}-${c-1}`]?.type === 'double'
}

const emitUpdate = () => {
  emit('update:layout', {
    rows: rows.value,
    cols: cols.value,
    seats: { ...seatMap }
  })
}

const applyTool = (r, c) => {
  if (!selectedTool.value) return
  const key = `${r}-${c}`
  if (selectedTool.value === 'remove') {
    delete seatMap[key]
  } else {
    seatMap[key] = { 
      type: selectedTool.value, 
      label: `${String.fromCharCode(65 + r)}${c + 1}` 
    }
    // Nếu là ghế đôi, tự động dọn dẹp ô bên phải để tránh xung đột layout
    if (selectedTool.value === 'double' && c < cols.value - 1) {
      delete seatMap[`${r}-${c+1}`]
    }
  }
  emitUpdate()
}

const handleMouseDown = (event, r, c) => {
  event.preventDefault() // Ngăn chặn bôi đen văn bản để thao tác kéo mượt hơn
  isMouseDown.value = true
  applyTool(r, c)
}

const handleMouseEnter = (r, c) => {
  if (isMouseDown.value) {
    applyTool(r, c)
  }
}

const handleMouseUp = () => {
  isMouseDown.value = false
}

const getSeatClass = (type, r, c) => {
  if (!type) return 'bg-white/[0.02] border-white/5 text-transparent hover:bg-white/[0.05] hover:border-primary/30 transition-colors'
  
  // Mock logic: nếu đang ở mode readonly thì giả lập một số ghế đã bị đặt (dựa trên tọa độ cố định để khỏi bị giật)
  const isSold = props.readonly && type !== 'aisle' && type !== 'remove' && ((r * 7 + c * 3) % 5 === 0);
  
  if (isSold) {
    return type === 'double' ? 'col-span-2 bg-white/5 border-white/10 text-white/20 cursor-not-allowed pointer-events-none' : 'bg-white/5 border-white/10 text-white/20 cursor-not-allowed pointer-events-none';
  }

  switch (type) {
    case 'standard': return 'bg-surface-container-highest border-outline-variant/30 text-on-surface-variant shadow-inner'
    case 'vip': return 'bg-primary/20 border-primary/50 text-primary shadow-[0_0_15px_rgba(245,197,24,0.1)]'
    case 'double': return 'bg-red-500/20 border-red-500/50 text-red-400 col-span-2 shadow-[0_0_20px_rgba(239,68,68,0.1)]'
    case 'aisle': return 'bg-transparent border-dashed border-outline-variant/10 text-transparent opacity-20 pointer-events-none'
    default: return ''
  }
}

const getToolClass = (tool) => {
  return selectedTool.value === tool ? 'bg-primary text-on-primary ring-4 ring-primary/10 shadow-lg scale-105' : 'bg-surface-container-high text-on-surface-variant border border-outline-variant/10 hover:bg-white/5'
}

onMounted(() => {
  window.addEventListener('mouseup', handleMouseUp)
  if (Object.keys(seatMap).length === 0) {
    initializeMap()
  }
})

onUnmounted(() => {
  window.removeEventListener('mouseup', handleMouseUp)
})
</script>

<template>
  <!-- IF READONLY (View mode with stats on top, seat map on bottom) -->
  <div v-if="readonly" class="flex flex-col gap-6 flex-grow overflow-y-auto h-full p-6">
    <!-- Top: Stats Row -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 flex-shrink-0">
      <!-- Card 1: Fill rate/percentage -->
      <div class="bg-surface-container-low border border-outline-variant/10 p-5 rounded-2xl flex items-center gap-6 shadow-sm">
        <div class="flex items-center justify-center relative flex-shrink-0">
          <svg class="w-20 h-20 transform -rotate-90">
            <circle cx="40" cy="40" r="34" class="stroke-white/5" stroke-width="8" fill="none" />
            <circle cx="40" cy="40" r="34" class="stroke-primary transition-all duration-1000" stroke-width="8" fill="none" stroke-dasharray="213.6" :stroke-dashoffset="213.6 - (213.6 * soldTickets / (totalSeats || 1))" stroke-linecap="round" />
          </svg>
          <div class="absolute inset-0 flex items-center justify-center">
            <span class="text-base font-black text-white">{{ Math.round((soldTickets / (totalSeats || 1)) * 100) }}%</span>
          </div>
        </div>
        <div class="space-y-1">
          <h4 class="text-[10px] font-bold uppercase tracking-[0.2em] text-primary">Tỷ lệ lấp đầy</h4>
          <p class="text-xs font-semibold text-white/60">Đã bán: <span class="font-bold text-white text-sm">{{ soldTickets }}</span> / {{ totalSeats }} ghế</p>
        </div>
      </div>

      <!-- Card 2: Seat categories / counts -->
      <div class="bg-surface-container-low border border-outline-variant/10 p-5 rounded-2xl shadow-sm flex flex-col justify-center">
        <h4 class="text-[10px] font-bold uppercase tracking-[0.2em] text-white/70 mb-3">Chi tiết ghế</h4>
        <div class="grid grid-cols-3 gap-4">
          <div class="text-center">
            <p class="text-[8px] font-bold text-white/40 uppercase mb-1">Ghế trống</p>
            <p class="text-lg font-black text-white">{{ totalSeats - soldTickets }}</p>
          </div>
          <div class="text-center border-x border-white/5">
            <p class="text-[8px] font-bold text-primary uppercase mb-1">Đã đặt</p>
            <p class="text-lg font-black text-primary">{{ soldTickets }}</p>
          </div>
          <div class="text-center">
            <p class="text-[8px] font-bold text-red-400 uppercase mb-1">Vé hủy</p>
            <p class="text-lg font-black text-red-400">{{ canceledTickets }}</p>
          </div>
        </div>
      </div>

      <!-- Card 3: Revenue / Doanh thu tạm tính -->
      <div class="bg-surface-container-low border border-outline-variant/10 p-5 rounded-2xl shadow-sm flex flex-col justify-center">
        <h4 class="text-[10px] font-bold uppercase tracking-[0.2em] text-green-400 mb-1.5 flex items-center gap-1.5">
          <span class="material-symbols-outlined text-sm">payments</span>
          Doanh thu tạm tính
        </h4>
        <div class="text-2xl font-black text-green-400 tracking-tight">{{ revenue }}</div>
        <p class="text-[8px] font-medium text-white/30 mt-1 leading-relaxed">* Đã trừ vé hủy và bao gồm thuế dịch vụ.</p>
      </div>
    </div>

    <!-- Bottom: Seat Map Canvas -->
    <div class="flex-grow bg-surface-container-low border border-outline-variant/10 rounded-2xl flex flex-col overflow-hidden relative shadow-2xl items-center justify-center p-6">
      <!-- Screen -->
      <div class="w-full flex flex-col items-center flex-shrink-0 bg-surface-container-high/20 py-4 rounded-t-xl mb-4">
        <div class="w-2/3 h-1.5 bg-primary rounded-full mb-3 shadow-[0_0_50px_rgba(245,197,24,0.6)]"></div>
        <p class="text-[8px] font-black uppercase tracking-[0.6em] text-primary/40 animate-pulse">MÀN HÌNH CHÍNH</p>
      </div>

      <!-- Grid with Labels -->
      <div class="w-full flex-grow overflow-auto p-4 flex items-center justify-center bg-[radial-gradient(circle_at_center,_rgba(245,197,24,0.03)_0%,_transparent_70%)] scrollbar-hide">
        <div class="flex flex-col gap-2.5">
           <!-- Column Labels -->
           <div class="flex gap-2.5 pl-8 mb-1">
              <div v-for="c in cols" :key="c" class="w-9 text-center text-[9px] font-black text-outline-variant/40">{{ c }}</div>
           </div>

           <div class="flex gap-3">
              <!-- Row Labels -->
              <div class="flex flex-col gap-2.5 pr-1">
                 <div v-for="r in rows" :key="r" class="h-9 flex items-center justify-end text-[9px] font-black text-outline-variant/40 w-5">
                    {{ String.fromCharCode(64 + r) }}
                 </div>
              </div>

              <!-- Matrix -->
              <div class="grid gap-2.5" :style="{ gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))` }">
                <div v-for="r in rows" :key="r" class="contents">
                  <div v-for="c in cols" :key="c">
                    <div v-if="!isOccupiedByDouble(r-1, c-1)"
                      :class="getSeatClass(seatMap[`${r-1}-${c-1}`]?.type, r-1, c-1)" 
                      class="w-9 h-9 border rounded-sm flex items-center justify-center text-[8px] font-black transition-all duration-75 group relative cursor-default">
                    {{ seatMap[`${r-1}-${c-1}`]?.label }}
                    <div v-if="!seatMap[`${r-1}-${c-1}`]?.type || seatMap[`${r-1}-${c-1}`]?.type === 'aisle'" class="absolute inset-0 flex items-center justify-center pointer-events-none">
                       <div class="w-1 h-1 bg-white/10 rounded-full"></div>
                    </div>
                    </div>
                  </div>
                </div>
              </div>
           </div>
        </div>
      </div>

      <!-- Legend -->
      <div class="w-full flex justify-center mt-6">
        <div class="flex items-center gap-6 px-6 py-3 bg-surface-container-high rounded-full border border-white/5 shadow-xl flex-wrap justify-center">
          <div class="flex items-center gap-1.5">
            <div class="w-3.5 h-3.5 rounded bg-surface-container-highest border-outline-variant/30 border"></div>
            <span class="text-[9px] font-bold text-white/70 uppercase tracking-widest">Ghế trống</span>
          </div>
          <div class="flex items-center gap-1.5">
            <div class="w-3.5 h-3.5 rounded bg-primary/20 border border-primary/50"></div>
            <span class="text-[9px] font-bold text-white/70 uppercase tracking-widest">Ghế VIP</span>
          </div>
          <div class="flex items-center gap-1.5">
            <div class="w-3.5 h-3.5 rounded bg-red-500/20 border border-red-500/50"></div>
            <span class="text-[9px] font-bold text-white/70 uppercase tracking-widest">Ghế Đôi</span>
          </div>
          <div class="h-3.5 w-px bg-white/10 mx-1 hidden sm:block"></div>
          <div class="flex items-center gap-1.5">
            <div class="w-3.5 h-3.5 rounded bg-white/5 border border-white/10 flex items-center justify-center text-[7px] text-white/20">X</div>
            <span class="text-[9px] font-bold text-white/40 uppercase tracking-widest">Đã đặt</span>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- IF EDITABLE (Horizontal builder view) -->
  <div v-else class="flex gap-8 flex-grow overflow-hidden h-full">
    <!-- Toolbar / Sidebar -->
    <aside class="w-80 space-y-6 flex-shrink-0 overflow-y-auto pr-2 pb-10 no-scrollbar">
      <div class="bg-surface-container-low border border-outline-variant/10 p-6 rounded-lg shadow-sm">
        <h3 class="text-[10px] font-bold uppercase tracking-[0.2em] text-primary mb-6">Cấu hình Ma trận</h3>
        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-2">
            <label class="text-[9px] font-bold uppercase text-outline-variant">Hàng (Rows)</label>
            <input v-model.number="rows" @change="initializeMap" type="number" min="1" max="26" class="w-full bg-surface-container-high border-none text-sm rounded py-2 px-3 text-on-surface focus:ring-1 focus:ring-primary outline-none transition-all">
          </div>
          <div class="space-y-2">
            <label class="text-[9px] font-bold uppercase text-outline-variant">Cột (Cols)</label>
            <input v-model.number="cols" @change="initializeMap" type="number" min="1" max="25" class="w-full bg-surface-container-high border-none text-sm rounded py-2 px-3 text-on-surface focus:ring-1 focus:ring-primary outline-none transition-all">
          </div>
        </div>
      </div>

      <div class="bg-surface-container-low border border-outline-variant/10 p-6 rounded-lg shadow-sm">
        <h3 class="text-[10px] font-bold uppercase tracking-[0.2em] text-primary mb-6">Công cụ thiết kế</h3>
        <div class="space-y-3">
          <button v-for="tool in [
            { id: 'standard', label: 'Ghế Thường', icon: 'chair', color: 'text-on-surface' },
            { id: 'vip', label: 'Ghế VIP', icon: 'diamond', color: 'text-primary' },
            { id: 'double', label: 'Ghế Đôi', icon: 'favorite', color: 'text-pink-400' },
            { id: 'aisle', label: 'Lối đi', icon: 'pave_list', color: 'text-blue-400' },
            { id: 'remove', label: 'Xóa ô', icon: 'ink_eraser', color: 'text-red-500' }
          ]" :key="tool.id" @click="toggleTool(tool.id)" :class="getToolClass(tool.id)" class="w-full flex items-center gap-4 p-4 rounded-lg transition-all text-left">
            <span class="material-symbols-outlined" :class="tool.color">{{ tool.icon }}</span>
            <p class="text-[10px] font-bold uppercase tracking-widest">{{ tool.label }}</p>
          </button>
        </div>
      </div>

      <div class="bg-surface-container-low border border-outline-variant/10 p-6 rounded-lg shadow-sm">
        <h3 class="text-[10px] font-bold uppercase tracking-[0.2em] text-primary mb-6 flex items-center gap-2">
          <span class="material-symbols-outlined text-sm">bar_chart</span>
          Thông số Phòng chiếu
        </h3>
        <div class="space-y-3">
          <div class="flex justify-between items-center p-3 bg-surface-container-high rounded-xl border border-white/5">
            <span class="text-[8px] font-bold opacity-40 uppercase">Sức chứa</span>
            <span class="text-sm font-black text-primary">{{ totalSeats }} ghế</span>
          </div>
          <div class="grid grid-cols-2 gap-2">
            <div class="p-3 bg-surface-container-high rounded-xl border border-white/5">
              <p class="text-[7px] font-bold opacity-30 uppercase mb-0.5">VIP</p>
              <p class="text-xs font-black">{{ Object.values(seatMap).filter(s => s?.type === 'vip').length }}</p>
            </div>
            <div class="p-3 bg-surface-container-high rounded-xl border border-white/5">
              <p class="text-[7px] font-bold opacity-30 uppercase mb-0.5">Double</p>
              <p class="text-xs font-black">{{ Object.values(seatMap).filter(s => s?.type === 'double').length }}</p>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Canvas Area -->
    <section class="flex-grow bg-surface-container-low border border-outline-variant/10 rounded-lg flex flex-col overflow-hidden relative shadow-2xl">
      <!-- Screen -->
      <div class="p-8 flex flex-col items-center flex-shrink-0 bg-surface-container-high/20">
        <div class="w-3/4 h-1.5 bg-primary rounded-full mb-4 shadow-[0_0_50px_rgba(245,197,24,0.6)]"></div>
        <p class="text-[9px] font-black uppercase tracking-[0.6em] text-primary/40 animate-pulse">MÀN HÌNH CHÍNH</p>
      </div>

      <!-- Grid with Labels -->
      <div class="flex-grow overflow-auto p-12 flex items-start justify-center bg-[radial-gradient(circle_at_center,_rgba(245,197,24,0.03)_0%,_transparent_70%)] scrollbar-hide">
        <div class="flex flex-col gap-3">
           <!-- Column Labels -->
           <div class="flex gap-3 pl-10 mb-2">
              <div v-for="c in cols" :key="c" class="w-10 text-center text-[10px] font-black text-outline-variant/40">{{ c }}</div>
           </div>

           <div class="flex gap-4">
              <!-- Row Labels -->
              <div class="flex flex-col gap-3 pr-2">
                 <div v-for="r in rows" :key="r" class="h-10 flex items-center justify-end text-[10px] font-black text-outline-variant/40 w-6">
                    {{ String.fromCharCode(64 + r) }}
                 </div>
              </div>

              <!-- Matrix -->
              <div class="grid gap-3" :style="{ gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))` }">
                <div v-for="r in rows" :key="r" class="contents">
                  <div v-for="c in cols" :key="c">
                    <div v-if="!isOccupiedByDouble(r-1, c-1)"
                      @mousedown="handleMouseDown($event, r-1, c-1)"
                      @mouseenter="handleMouseEnter(r-1, c-1)"
                      :class="getSeatClass(seatMap[`${r-1}-${c-1}`]?.type, r-1, c-1)" 
                      class="w-10 h-10 border rounded-sm flex items-center justify-center text-[8px] font-black transition-all duration-75 group relative"
                      style="cursor: pointer">
                    {{ seatMap[`${r-1}-${c-1}`]?.label }}
                    <!-- Placeholder dot for empty or aisle spaces -->
                    <div v-if="!seatMap[`${r-1}-${c-1}`]?.type || seatMap[`${r-1}-${c-1}`]?.type === 'aisle'" class="absolute inset-0 flex items-center justify-center pointer-events-none">
                       <div class="w-1 h-1 bg-white/10 rounded-full group-hover:bg-primary/50 transition-colors"></div>
                    </div>
                    </div>
                  </div>
                </div>
              </div>
           </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.scrollbar-hide::-webkit-scrollbar { display: none; }
.no-scrollbar::-webkit-scrollbar { display: none; }
</style>
