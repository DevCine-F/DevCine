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

// ===== Sửa label thủ công =====
const editingKey = ref(null)
const editingLabel = ref('')

const totalSeats = computed(() => {
  return Object.values(seatMap).filter(s => s && s.type !== 'aisle' && s.type !== 'remove').length
})

const rowLetter = (r) => String.fromCharCode(65 + r)

const isSeatCell = (cell) => !!cell && cell.type && cell.type !== 'aisle' && cell.type !== 'hidden'

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

/**
 * Đánh số lại 1 hàng theo thứ tự GHẾ THỰC (bỏ qua lối đi / ô đã xóa) → không nhảy cóc.
 * Ghế có label do Admin sửa tay (custom=true) được coi là "chốt cứng": giữ nguyên label
 * và KHÔNG chiếm số tự động, để Admin chủ động tạo khoảng nhảy số khi cần.
 */
const relabelRow = (r) => {
  let counter = 0
  for (let c = 0; c < cols.value; c++) {
    const cell = seatMap[`${r}-${c}`]
    if (!isSeatCell(cell)) continue        // lối đi / ô xóa → không chiếm số
    if (cell.custom) continue              // label thủ công → giữ nguyên, không đếm
    counter++
    cell.label = `${rowLetter(r)}${counter}`
  }
}

const initializeMap = () => {
  for (let r = 0; r < rows.value; r++) {
    for (let c = 0; c < cols.value; c++) {
      const key = `${r}-${c}`
      if (!seatMap[key]) {
        seatMap[key] = { type: 'standard', label: `${rowLetter(r)}${c + 1}`, status: 'AVAILABLE' }
      }
    }
    relabelRow(r)
  }
  emitUpdate()
}

const isOccupiedByDouble = (r, c) => {
  if (c === 0) return false
  return seatMap[`${r}-${c - 1}`]?.type === 'double'
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

  // Công cụ Bảo trì/Khóa: KHÔNG xóa ghế, chỉ đảo trạng thái vật lý (giữ nguyên type + label + số)
  if (selectedTool.value === 'maintenance') {
    const cell = seatMap[key]
    if (!isSeatCell(cell)) return // chỉ áp lên ghế thật
    cell.status = cell.status === 'MAINTENANCE' ? 'AVAILABLE' : 'MAINTENANCE'
    emitUpdate()
    return
  }

  // Không cho phép đặt ghế đôi ở cột cuối cùng vì nó cần 2 ô
  if (selectedTool.value === 'double' && c === cols.value - 1) {
    return
  }

  if (selectedTool.value === 'remove') {
    delete seatMap[key]
  } else if (selectedTool.value === 'aisle') {
    seatMap[key] = { type: 'aisle' }
  } else {
    seatMap[key] = {
      type: selectedTool.value,
      // label sẽ được relabelRow gán tự động ngay bên dưới; giữ status hiện có nếu có
      label: seatMap[key]?.custom ? seatMap[key].label : '',
      status: seatMap[key]?.status || 'AVAILABLE',
      custom: seatMap[key]?.custom || false
    }
    // Nếu là ghế đôi, xóa ghế bên phải nếu có để tránh xung đột layout
    if (selectedTool.value === 'double' && c < cols.value - 1) {
      delete seatMap[`${r}-${c + 1}`]
    }
  }
  relabelRow(r)
  emitUpdate()
}

// ===== Sửa label thủ công =====
const openLabelEditor = (r, c) => {
  if (props.readonly) return
  const key = `${r}-${c}`
  const cell = seatMap[key]
  if (!isSeatCell(cell)) return // chỉ ghế thật mới sửa được tên
  editingKey.value = key
  editingLabel.value = cell.label || ''
}

const closeLabelEditor = () => {
  editingKey.value = null
  editingLabel.value = ''
}

const saveLabel = () => {
  if (!editingKey.value) return
  const cell = seatMap[editingKey.value]
  const val = (editingLabel.value || '').trim().toUpperCase()
  if (cell && val) {
    cell.label = val
    cell.custom = true // chốt cứng, không bị đánh số tự động ghi đè
    emitUpdate()
  }
  closeLabelEditor()
}

// Trả label về chế độ tự động (bỏ chốt cứng) và đánh số lại cả hàng
const resetLabelToAuto = () => {
  if (!editingKey.value) return
  const cell = seatMap[editingKey.value]
  if (cell) {
    cell.custom = false
    const r = parseInt(editingKey.value.split('-')[0])
    relabelRow(r)
    emitUpdate()
  }
  closeLabelEditor()
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

const getSeatClass = (cell, r, c) => {
  const type = cell?.type
  const baseClasses = 'shadow-[0_4px_6px_rgba(0,0,0,0.3),inset_0_1px_1px_rgba(255,255,255,0.1)] transition-all duration-200'

  if (!type) return 'bg-white/[0.02] border border-white/5 text-transparent hover:bg-white/[0.05] hover:border-primary/30 transition-colors rounded-lg'

  // Ghế đang bảo trì/khóa: viền đỏ nét đứt, mờ, icon cờ lê (không phụ thuộc readonly)
  if (isSeatCell(cell) && cell.status && cell.status !== 'AVAILABLE') {
    const doubleClass = type === 'double' ? 'col-span-2 rounded-xl' : 'rounded-lg'
    return `${doubleClass} bg-red-950/40 border-2 border-dashed border-red-500/60 text-red-300/70 opacity-70`
  }

  // Mock logic: nếu đang ở mode readonly thì giả lập một số ghế đã bị đặt (dựa trên tọa độ cố định để khỏi bị giật)
  const isSold = props.readonly && type !== 'aisle' && type !== 'remove' && ((r * 7 + c * 3) % 5 === 0)

  if (isSold) {
    const doubleClass = type === 'double' ? 'col-span-2 rounded-xl' : 'rounded-lg'
    return `${doubleClass} bg-surface-container-high border border-white/5 text-white/20 cursor-not-allowed pointer-events-none opacity-50`
  }

  // Ghế có label thủ công (custom) → thêm viền hổ phách báo hiệu (không dùng icon che chữ)
  const customRing = isSeatCell(cell) && cell.custom ? ' ring-2 ring-inset ring-primary/70' : ''

  switch (type) {
    case 'standard': return `${baseClasses}${customRing} rounded-lg bg-slate-800/80 border border-slate-600/50 text-slate-300 hover:brightness-125 hover:-translate-y-0.5 hover:shadow-lg`
    case 'vip': return `${baseClasses}${customRing} rounded-lg bg-gradient-to-b from-red-700/90 to-red-900/90 border border-red-500/50 text-red-100 shadow-[0_0_15px_rgba(220,38,38,0.2)] hover:shadow-[0_0_20px_rgba(220,38,38,0.4)] hover:-translate-y-0.5 hover:brightness-110`
    case 'double': return `${baseClasses}${customRing} col-span-2 rounded-t-2xl rounded-b-lg bg-gradient-to-b from-purple-600/90 to-purple-900/90 border border-purple-500/50 text-purple-100 shadow-[0_0_15px_rgba(147,51,234,0.2)] hover:shadow-[0_0_20px_rgba(147,51,234,0.4)] hover:-translate-y-0.5 hover:brightness-110`
    case 'aisle': return 'bg-transparent border border-dashed border-white/10 text-transparent opacity-20 rounded-lg hover:opacity-50 hover:border-primary/50'
    default: return 'rounded-lg'
  }
}

const getToolClass = (tool) => {
  return selectedTool.value === tool
    ? 'bg-gradient-to-br from-primary to-amber-600 text-on-primary shadow-[0_0_20px_rgba(245,197,24,0.3)] scale-[1.02] border-none'
    : 'bg-surface-container-high/40 text-on-surface-variant border border-outline-variant/10 hover:bg-white/10 hover:border-white/20 hover:scale-[1.01]'
}

const maintenanceCount = computed(() =>
  Object.values(seatMap).filter(s => s && s.status && s.status !== 'AVAILABLE' && s.type !== 'aisle').length
)

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
    <div class="flex-grow bg-surface-container-low/80 backdrop-blur-md border border-outline-variant/10 rounded-3xl flex flex-col overflow-hidden relative shadow-2xl items-center justify-center p-6">
      <!-- Screen -->
      <div class="w-full flex flex-col items-center flex-shrink-0 relative py-8">
        <div class="absolute top-0 w-full h-[100px] bg-gradient-to-b from-primary/5 to-transparent pointer-events-none"></div>
        <div class="w-2/3 h-1.5 bg-primary/70 rounded-full shadow-[0_2px_15px_rgba(245,197,24,0.2)] mb-4 border border-primary/20"></div>
        <p class="text-[9px] font-bold uppercase tracking-[0.6em] text-primary/50 relative z-10">MÀN HÌNH CHÍNH</p>
      </div>

      <!-- Grid with Labels -->
      <div class="w-full flex-grow overflow-auto p-4 flex items-center justify-center relative scrollbar-hide">
        <div class="absolute inset-0 opacity-[0.15] pointer-events-none" style="background-image: radial-gradient(rgba(255, 255, 255, 0.4) 1px, transparent 1px); background-size: 24px 24px;"></div>
        <div class="relative z-10 flex flex-col gap-2.5 bg-black/40 backdrop-blur-sm p-6 rounded-3xl border border-white/5 shadow-2xl">
           <!-- Column Labels -->
           <div class="flex gap-2.5 pl-8 mb-1">
              <div v-for="c in cols" :key="c" class="w-9 text-center text-[9px] font-black text-outline-variant/40 shrink-0">{{ c }}</div>
           </div>

           <div class="flex gap-3">
              <!-- Row Labels -->
              <div class="flex flex-col gap-2.5 pr-1">
                 <div v-for="r in rows" :key="r" class="h-9 flex items-center justify-end text-[9px] font-black text-outline-variant/40 w-5">
                    {{ String.fromCharCode(64 + r) }}
                 </div>
              </div>

              <!-- Matrix -->
              <div class="flex flex-col gap-2.5">
                <div v-for="r in rows" :key="r" class="flex gap-2.5">
                  <template v-for="c in cols" :key="c">
                    <div v-if="!isOccupiedByDouble(r-1, c-1)"
                      :class="[getSeatClass(seatMap[`${r-1}-${c-1}`], r-1, c-1), seatMap[`${r-1}-${c-1}`]?.type === 'double' ? 'w-[82px]' : 'w-9', 'h-9 flex items-center justify-center text-[8px] font-black transition-all duration-75 group relative cursor-default shrink-0']">
                    {{ seatMap[`${r-1}-${c-1}`]?.label }}
                    <div v-if="!seatMap[`${r-1}-${c-1}`]?.type || seatMap[`${r-1}-${c-1}`]?.type === 'aisle'" class="absolute inset-0 flex items-center justify-center pointer-events-none">
                       <div class="w-1 h-1 bg-white/10 rounded-full"></div>
                    </div>
                    </div>
                  </template>
                </div>
              </div>
           </div>
        </div>
      </div>

      <!-- Legend -->
      <div class="w-full flex justify-center mt-6 relative z-10">
        <div class="flex items-center gap-6 px-6 py-3 bg-surface-container-high/80 backdrop-blur-md rounded-full border border-white/5 shadow-xl flex-wrap justify-center">
          <div class="flex items-center gap-1.5">
            <div class="w-3.5 h-3.5 rounded bg-slate-800/80 border border-slate-600/50"></div>
            <span class="text-[9px] font-bold text-white/70 uppercase tracking-widest">Ghế trống</span>
          </div>
          <div class="flex items-center gap-1.5">
            <div class="w-3.5 h-3.5 rounded bg-gradient-to-b from-red-700/90 to-red-900/90 border border-red-500/50"></div>
            <span class="text-[9px] font-bold text-white/70 uppercase tracking-widest">Ghế VIP</span>
          </div>
          <div class="flex items-center gap-1.5">
            <div class="w-3.5 h-3.5 rounded-t-lg rounded-b-sm bg-gradient-to-b from-purple-600/90 to-purple-900/90 border border-purple-500/50"></div>
            <span class="text-[9px] font-bold text-white/70 uppercase tracking-widest">Ghế Đôi</span>
          </div>
          <div class="h-3.5 w-px bg-white/10 mx-1 hidden sm:block"></div>
          <div class="flex items-center gap-1.5">
            <div class="w-3.5 h-3.5 rounded bg-surface-container-high border border-white/5 flex items-center justify-center text-[7px] text-white/20">X</div>
            <span class="text-[9px] font-bold text-white/40 uppercase tracking-widest">Đã đặt</span>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- IF EDITABLE (Horizontal builder view) -->
  <div v-else class="flex gap-8 flex-grow overflow-hidden h-full">
    <!-- Toolbar / Sidebar -->
    <aside class="w-80 space-y-6 flex-shrink-0 overflow-y-auto pr-2 pb-10 no-scrollbar relative z-20">
      <div class="bg-surface-container-low/60 backdrop-blur-md border border-white/5 p-6 rounded-2xl shadow-xl">
        <h3 class="text-[10px] font-bold uppercase tracking-[0.2em] text-primary mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-sm">grid_on</span> Cấu hình Ma trận
        </h3>
        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-2">
            <label class="text-[9px] font-bold uppercase text-outline-variant">Hàng (Rows)</label>
            <input v-model.number="rows" @change="initializeMap" type="number" min="1" max="26" class="w-full bg-black/40 border border-white/5 text-sm rounded-xl py-2 px-3 text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all">
          </div>
          <div class="space-y-2">
            <label class="text-[9px] font-bold uppercase text-outline-variant">Cột (Cols)</label>
            <input v-model.number="cols" @change="initializeMap" type="number" min="1" max="25" class="w-full bg-black/40 border border-white/5 text-sm rounded-xl py-2 px-3 text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all">
          </div>
        </div>
      </div>

      <div class="bg-surface-container-low/60 backdrop-blur-md border border-white/5 p-6 rounded-2xl shadow-xl">
        <h3 class="text-[10px] font-bold uppercase tracking-[0.2em] text-primary mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-sm">design_services</span> Công cụ thiết kế
        </h3>
        <div class="space-y-3">
          <button v-for="tool in [
            { id: 'standard', label: 'Ghế Thường', icon: 'chair', color: 'text-slate-400' },
            { id: 'vip', label: 'Ghế VIP', icon: 'diamond', color: 'text-red-500' },
            { id: 'double', label: 'Ghế Đôi', icon: 'favorite', color: 'text-purple-400' },
            { id: 'aisle', label: 'Lối đi', icon: 'pave_list', color: 'text-blue-400' },
            { id: 'maintenance', label: 'Bảo trì / Khóa', icon: 'build', color: 'text-red-400' },
            { id: 'remove', label: 'Xóa ô', icon: 'ink_eraser', color: 'text-orange-500' }
          ]" :key="tool.id" @click="toggleTool(tool.id)" :class="getToolClass(tool.id)" class="w-full flex items-center gap-4 p-4 rounded-xl transition-all text-left group relative overflow-hidden">
            <div v-if="selectedTool === tool.id" class="absolute inset-0 bg-white/10"></div>
            <span class="material-symbols-outlined relative z-10" :class="selectedTool === tool.id ? 'text-white' : tool.color">{{ tool.icon }}</span>
            <p class="text-[10px] font-bold uppercase tracking-widest relative z-10" :class="selectedTool === tool.id ? 'text-white' : ''">{{ tool.label }}</p>
          </button>
        </div>
        <p class="text-[9px] text-on-surface-variant/60 mt-4 leading-relaxed flex items-start gap-1.5">
          <span class="material-symbols-outlined text-[13px] text-primary/70">lightbulb</span>
          Nhấp đúp (hoặc chuột phải) vào một ghế để sửa tên/số ghế thủ công.
        </p>
      </div>

      <div class="bg-surface-container-low/60 backdrop-blur-md border border-white/5 p-6 rounded-2xl shadow-xl">
        <h3 class="text-[10px] font-bold uppercase tracking-[0.2em] text-primary mb-6 flex items-center gap-2">
          <span class="material-symbols-outlined text-sm">bar_chart</span>
          Thông số Phòng chiếu
        </h3>
        <div class="space-y-3">
          <div class="flex justify-between items-center p-4 bg-black/40 rounded-xl border border-white/5">
            <span class="text-[9px] font-bold opacity-50 uppercase tracking-widest">Sức chứa</span>
            <span class="text-base font-black text-primary">{{ totalSeats }} ghế</span>
          </div>
          <div class="grid grid-cols-3 gap-3">
            <div class="p-4 bg-black/40 rounded-xl border border-white/5 flex flex-col items-center justify-center">
              <p class="text-[8px] font-bold text-primary opacity-70 uppercase tracking-widest mb-1">VIP</p>
              <p class="text-sm font-black">{{ Object.values(seatMap).filter(s => s?.type === 'vip').length }}</p>
            </div>
            <div class="p-4 bg-black/40 rounded-xl border border-white/5 flex flex-col items-center justify-center">
              <p class="text-[8px] font-bold text-pink-400 opacity-70 uppercase tracking-widest mb-1">Double</p>
              <p class="text-sm font-black">{{ Object.values(seatMap).filter(s => s?.type === 'double').length }}</p>
            </div>
            <div class="p-4 bg-black/40 rounded-xl border border-white/5 flex flex-col items-center justify-center">
              <p class="text-[8px] font-bold text-red-400 opacity-70 uppercase tracking-widest mb-1">Bảo trì</p>
              <p class="text-sm font-black">{{ maintenanceCount }}</p>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Canvas Area -->
    <section class="flex-grow bg-surface-container-low/80 backdrop-blur-md border border-outline-variant/10 rounded-3xl flex flex-col overflow-hidden relative shadow-2xl">
      <!-- Screen -->
      <div class="flex flex-col items-center flex-shrink-0 relative py-8 w-full">
        <div class="absolute top-0 w-full h-[100px] bg-gradient-to-b from-primary/5 to-transparent pointer-events-none"></div>
        <div class="w-3/4 h-1.5 bg-primary/70 rounded-full shadow-[0_2px_15px_rgba(245,197,24,0.2)] mb-4 border border-primary/20"></div>
        <p class="text-[9px] font-bold uppercase tracking-[0.6em] text-primary/50 relative z-10">MÀN HÌNH CHÍNH</p>
      </div>

      <!-- Grid with Labels -->
      <div class="flex-grow overflow-auto p-12 flex items-start justify-center relative scrollbar-hide">
        <!-- Dot matrix background overlay -->
        <div class="absolute inset-0 opacity-[0.15] pointer-events-none" style="background-image: radial-gradient(rgba(255, 255, 255, 0.4) 1px, transparent 1px); background-size: 24px 24px;"></div>

        <div class="relative z-10 flex flex-col gap-3 bg-black/40 backdrop-blur-sm p-8 rounded-[2rem] border border-white/5 shadow-2xl">
           <!-- Column Labels -->
           <div class="flex gap-3 pl-10 mb-2">
              <div v-for="c in cols" :key="c" class="w-10 text-center text-[10px] font-black text-outline-variant/40 shrink-0">{{ c }}</div>
           </div>

           <div class="flex gap-4">
              <!-- Row Labels -->
              <div class="flex flex-col gap-3 pr-2">
                 <div v-for="r in rows" :key="r" class="h-10 flex items-center justify-end text-[10px] font-black text-outline-variant/40 w-6">
                    {{ String.fromCharCode(64 + r) }}
                 </div>
              </div>

              <!-- Matrix -->
              <div class="flex flex-col gap-3">
                <div v-for="r in rows" :key="r" class="flex gap-3">
                  <template v-for="c in cols" :key="c">
                    <div v-if="!isOccupiedByDouble(r-1, c-1)"
                      @mousedown="handleMouseDown($event, r-1, c-1)"
                      @mouseenter="handleMouseEnter(r-1, c-1)"
                      @dblclick="openLabelEditor(r-1, c-1)"
                      @contextmenu.prevent="openLabelEditor(r-1, c-1)"
                      :class="[getSeatClass(seatMap[`${r-1}-${c-1}`], r-1, c-1), seatMap[`${r-1}-${c-1}`]?.type === 'double' ? 'w-[92px]' : 'w-10', 'h-10 flex items-center justify-center text-[10px] font-black leading-none transition-all duration-75 group relative select-none shrink-0']"
                      style="cursor: pointer">
                    <!-- Tên/số ghế: luôn ưu tiên hiển thị to, rõ, ở giữa ô -->
                    <span class="relative z-[1] pointer-events-none">{{ seatMap[`${r-1}-${c-1}`]?.type !== 'aisle' ? seatMap[`${r-1}-${c-1}`]?.label : '' }}</span>
                    <!-- Marker bảo trì: chấm đỏ nhỏ góc trên phải (không che text) -->
                    <span v-if="seatMap[`${r-1}-${c-1}`]?.status && seatMap[`${r-1}-${c-1}`]?.status !== 'AVAILABLE'"
                          class="material-symbols-outlined absolute z-[2] text-red-300 bg-red-950 rounded-full leading-none pointer-events-none"
                          style="top:2px;right:2px;font-size:11px;width:12px;height:12px;">build</span>
                    <!-- Marker label thủ công: chấm hổ phách nhỏ góc trên phải -->
                    <span v-else-if="seatMap[`${r-1}-${c-1}`]?.custom"
                          class="absolute z-[2] rounded-full bg-primary ring-1 ring-black/40 pointer-events-none"
                          style="top:2px;right:2px;width:6px;height:6px;" title="Tên ghế đặt thủ công"></span>
                    <!-- Placeholder dot for empty or aisle spaces -->
                    <div v-if="!seatMap[`${r-1}-${c-1}`]?.type || seatMap[`${r-1}-${c-1}`]?.type === 'aisle'" class="absolute inset-0 flex items-center justify-center pointer-events-none">
                       <div class="w-1.5 h-1.5 bg-white/10 rounded-full group-hover:bg-primary/50 transition-colors"></div>
                    </div>
                    </div>
                  </template>
                </div>
              </div>
           </div>
        </div>
      </div>
    </section>

    <!-- Modal sửa label thủ công -->
    <div v-if="editingKey" class="fixed inset-0 z-[140] flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="closeLabelEditor">
      <div class="bg-surface-container-low border border-white/10 rounded-2xl p-6 w-80 shadow-2xl">
        <h3 class="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-sm">edit</span> Sửa tên ghế
        </h3>
        <input v-model="editingLabel" @keyup.enter="saveLabel" type="text" maxlength="10" autofocus
               class="w-full bg-black/40 border border-white/10 text-lg font-black text-center tracking-widest rounded-xl py-3 px-3 text-white uppercase focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all">
        <p class="text-[9px] text-on-surface-variant/60 mt-2 text-center">VD: A5, B12... Nhãn thủ công sẽ được giữ nguyên khi đánh số tự động.</p>
        <div class="flex gap-2 mt-5">
          <button @click="resetLabelToAuto" class="flex-1 px-3 py-2.5 rounded-lg border border-white/10 text-on-surface-variant text-[9px] font-black uppercase tracking-widest hover:bg-white/5 transition-all">Tự động</button>
          <button @click="closeLabelEditor" class="flex-1 px-3 py-2.5 rounded-lg border border-white/10 text-on-surface-variant text-[9px] font-black uppercase tracking-widest hover:bg-white/5 transition-all">Hủy</button>
          <button @click="saveLabel" class="flex-1 px-3 py-2.5 rounded-lg bg-primary text-on-primary text-[9px] font-black uppercase tracking-widest hover:brightness-110 transition-all">Lưu</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.scrollbar-hide::-webkit-scrollbar { display: none; }
.no-scrollbar::-webkit-scrollbar { display: none; }
</style>
